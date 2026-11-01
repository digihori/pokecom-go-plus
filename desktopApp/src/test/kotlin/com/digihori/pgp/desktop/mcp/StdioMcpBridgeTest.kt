package com.digihori.pgp.desktop.mcp

import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.serialization.json.*
import kotlin.test.*

class StdioMcpBridgeTest {
    @Test fun actualStudioServerSupportsInitializeToolsAndRotationThroughSameBridge() {
        val path = Files.createTempFile("pgp-bridge-server", ".json")
        var token = "a".repeat(43)
        val store = object : DesktopMcpCredentialStore {
            override fun publish(token: String): DesktopMcpCredentialLocation {
                Files.writeString(path, """{"Authorization":"Bearer $token"}""")
                return DesktopMcpCredentialLocation(path, "unused")
            }
            override fun removeIfOwned(token: String) { Files.deleteIfExists(path) }
        }
        fun server() = DesktopMcpServer(PgpMcpToolAdapter({ null }), token = token, credentialStore = store)
        var server = server()
        try {
            val info = assertIs<DesktopMcpStartResult.Success>(server.start(0)).info
            val bridge = StdioMcpBridge(info.port) {
                Json.parseToJsonElement(Files.readString(path)).jsonObject.getValue("Authorization").jsonPrimitive.content
            }
            fun exchange(request: String): JsonObject {
                val output = ByteArrayOutputStream()
                bridge.run(ByteArrayInputStream(request.toByteArray()), output)
                return Json.parseToJsonElement(output.toString(Charsets.UTF_8)).jsonObject
            }
            val initialized = exchange("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}""")
            assertEquals("2025-06-18", initialized.getValue("result").jsonObject.getValue("protocolVersion").jsonPrimitive.content)
            server.stop()
            token = "b".repeat(43)
            server = server()
            assertIs<DesktopMcpStartResult.Success>(server.start(info.port))
            val listed = exchange("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""")
            assertEquals(24, listed.getValue("result").jsonObject.getValue("tools").jsonArray.size)
        } finally { server.stop(); Files.deleteIfExists(path) }
    }

    @Test fun forwardsRequestsAndNotificationsAndRefreshesCredentialsWithoutRetry() {
        val headers = mutableListOf<String>()
        val methods = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/mcp") { exchange ->
            exchange.use {
                headers += exchange.requestHeaders.getFirst("Authorization")
                val request = Json.parseToJsonElement(exchange.requestBody.readBytes().decodeToString()).jsonObject
                methods += request.getValue("method").jsonPrimitive.content
                if ("id" !in request) exchange.sendResponseHeaders(202, -1) else {
                    val body = buildJsonObject {
                        put("jsonrpc", "2.0"); put("id", request.getValue("id"))
                        putJsonObject("result") { put("ok", true) }
                    }.toString().toByteArray()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.write(body)
                }
            }
        }
        server.start()
        try {
            var reads = 0
            val bridge = StdioMcpBridge(server.address.port) { "Bearer token-${++reads}" }
            val output = ByteArrayOutputStream()
            bridge.run(ByteArrayInputStream((
                """{"jsonrpc":"2.0","id":1,"method":"initialize"}""" + "\n" +
                """{"jsonrpc":"2.0","method":"notifications/initialized"}""" + "\n" +
                """{"jsonrpc":"2.0","id":"true","method":"tools/list"}""" + "\n" +
                """{"jsonrpc":"2.0","id":1.25,"method":"ping"}""" + "\n"
            ).toByteArray()), output)
            assertEquals(listOf("Bearer token-1", "Bearer token-2", "Bearer token-3", "Bearer token-4"), headers)
            assertEquals(listOf("initialize", "notifications/initialized", "tools/list", "ping"), methods)
            val replies = output.toString(Charsets.UTF_8).trim().lines().map { Json.parseToJsonElement(it).jsonObject }
            assertEquals(listOf(JsonPrimitive(1), JsonPrimitive("true"), JsonPrimitive(1.25)), replies.map { it["id"] })
            assertFalse(output.toString().contains("Bearer"))
        } finally { server.stop(0) }
    }

    @Test fun boundedInputRecoversAndCredentialFailureNeverLeaksDetails() {
        var reads = 0
        val bridge = StdioMcpBridge(1) { reads++; error("secret-token /private/credential/path") }
        val output = ByteArrayOutputStream()
        val input = "x".repeat(StdioMcpBridge.MAX_REQUEST_BYTES + 10) + "\n" +
            "not-json\n" + """{"jsonrpc":"2.0","id":9,"method":"tools/list"}""" + "\n" +
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""" + "\n"
        bridge.run(ByteArrayInputStream(input.toByteArray()), output)
        val text = output.toString(Charsets.UTF_8)
        val replies = text.trim().lines().map { Json.parseToJsonElement(it).jsonObject }
        assertEquals(3, replies.size)
        assertEquals(listOf(-32600, -32700, -32050), replies.map { it.getValue("error").jsonObject.getValue("code").jsonPrimitive.int })
        assertEquals(JsonPrimitive(9), replies.last()["id"])
        assertEquals(2, reads)
        assertFalse(text.contains("secret-token")); assertFalse(text.contains("/private/"))
    }

    @Test fun refusesRedirectWithoutSendingCredentialsToAnotherEndpoint() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var redirected = false
        server.createContext("/mcp") { it.use { exchange ->
            exchange.responseHeaders.set("Location", "http://127.0.0.1:${server.address.port}/elsewhere")
            exchange.sendResponseHeaders(307, -1)
        } }
        server.createContext("/elsewhere") { redirected = true; it.close() }
        server.start()
        try {
            val output = ByteArrayOutputStream()
            StdioMcpBridge(server.address.port) { "Bearer private" }.run(
                ByteArrayInputStream("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""".toByteArray()), output)
            assertFalse(redirected)
            assertTrue(output.toString().contains("HTTP request failed"))
            assertFalse(output.toString().contains("private"))
        } finally { server.stop(0) }
    }

    @Test fun readsRotatedCredentialAndRejectsExposedOrSymlinkFiles() {
        val directory = Files.createTempDirectory("pgp-bridge-test")
        val path = directory.resolve("headers.json")
        try {
            if (Files.getFileAttributeView(directory, java.nio.file.attribute.PosixFileAttributeView::class.java) == null) return
            Files.writeString(path, """{"Authorization":"Bearer ${"a".repeat(43)}"}""")
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            assertEquals("Bearer ${"a".repeat(43)}", readMcpAuthorization(path))
            Files.writeString(path, """{"Authorization":"Bearer ${"b".repeat(43)}"}""")
            assertEquals("Bearer ${"b".repeat(43)}", readMcpAuthorization(path))
            val link = Files.createSymbolicLink(directory.resolve("link"), path)
            assertFails { readMcpAuthorization(link) }
            Files.delete(link)
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"))
            assertFails { readMcpAuthorization(path) }
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(directory) }
    }

    @Test fun clientConfigurationsPreservePathsAndNeverContainTokens() {
        val launcher = "/Applications/日本語 Studio.app/Contents/MacOS/PokecomGOStudio"
        for (client in StdioMcpClient.entries) {
            val text = stdioMcpConfiguration(client, launcher, 9123)
            val root = Json.parseToJsonElement(text).jsonObject
            val server = root.getValue(if (client == StdioMcpClient.VSCODE) "servers" else "mcpServers")
                .jsonObject.getValue("pokecom_go_studio").jsonObject
            assertEquals(launcher, server.getValue("command").jsonPrimitive.content)
            assertEquals(listOf("--mcp-stdio", "--port", "9123"), server.getValue("args").jsonArray.map { it.jsonPrimitive.content })
            assertFalse(text.contains("Authorization")); assertFalse(text.contains("mcp-headers"))
        }
    }
}
