package com.digihori.pgp.desktop.mcp

import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlinx.serialization.json.*

/** One bounded JSON-RPC line per message. No UI, emulator, retries, or credential output. */
internal class StdioMcpBridge(
    private val port: Int,
    private val authorization: () -> String,
) {
    init { require(port in 1..65535) }

    fun run(input: InputStream, output: OutputStream) {
        val source = input.buffered()
        while (true) {
            val line = readLine(source) ?: return
            val reply = if (line.size > MAX_REQUEST_BYTES) {
                error(JsonNull, -32600, "MCP request exceeds 64 KiB")
            } else handle(line)
            if (reply != null) {
                output.write(reply.toString().toByteArray(Charsets.UTF_8))
                output.write('\n'.code)
                output.flush()
            }
        }
    }

    private fun handle(line: ByteArray): JsonObject? {
        val request = runCatching { Json.parseToJsonElement(line.decodeToString(throwOnInvalidSequence = true)).jsonObject }
            .getOrElse { return error(JsonNull, -32700, "Invalid JSON-RPC JSON") }
        val id = request["id"] ?: JsonNull
        val notification = "id" !in request
        if (request["jsonrpc"] != JsonPrimitive("2.0") || request["method"] !is JsonPrimitive ||
            request["method"]?.jsonPrimitive?.isString != true ||
            (id != JsonNull && (id !is JsonPrimitive || (!id.isString && id.doubleOrNull?.isFinite() != true)))) {
            return error(JsonNull, -32600, "Invalid JSON-RPC request")
        }
        return try {
            val connection = URI("http://127.0.0.1:$port/mcp").toURL()
                .openConnection(Proxy.NO_PROXY) as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5_000
                connection.readTimeout = 30_000
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json, text/event-stream")
                // Read anew for EVERY request, including after Studio restarts.
                connection.setRequestProperty("Authorization", authorization())
                connection.setFixedLengthStreamingMode(line.size)
                connection.outputStream.use { it.write(line) }
                val status = connection.responseCode
                if (notification) return null
                if (status != 200) return error(id, -32050, when (status) {
                    401 -> "Studio MCP authentication expired; start the server and retry explicitly"
                    403 -> "Studio MCP rejected this connection"
                    else -> "Studio MCP HTTP request failed"
                })
                val body = connection.inputStream.use { it.readNBytes(MAX_RESPONSE_BYTES + 1) }
                if (body.size > MAX_RESPONSE_BYTES) return error(id, -32050, "Studio MCP response exceeds limit")
                val response = Json.parseToJsonElement(body.decodeToString(throwOnInvalidSequence = true)).jsonObject
                if (response["jsonrpc"] != JsonPrimitive("2.0") || response["id"] != id ||
                    (("result" in response) == ("error" in response))) {
                    error(id, -32050, "Invalid Studio MCP response")
                } else response
            } finally { connection.disconnect() }
        } catch (_: Exception) {
            // Exception messages may contain local paths or headers. Never forward them.
            if (notification) null else error(id, -32050,
                "Studio MCP unavailable or credentials unreadable; start the Studio MCP server")
        }
    }

    /** Drain an oversized line without allocating an unbounded buffer; keep the next line usable. */
    private fun readLine(input: InputStream): ByteArray? {
        val bytes = java.io.ByteArrayOutputStream()
        var seen = false
        while (true) {
            val next = input.read()
            if (next == -1) return if (seen) bytes.toByteArray() else null
            seen = true
            if (next == '\n'.code) return bytes.toByteArray()
            if (bytes.size() <= MAX_REQUEST_BYTES) bytes.write(next)
        }
    }

    companion object {
        internal const val MAX_REQUEST_BYTES = 64 * 1024
        internal const val MAX_RESPONSE_BYTES = 1024 * 1024

        private fun error(id: JsonElement, code: Int, message: String) = buildJsonObject {
            put("jsonrpc", "2.0"); put("id", id)
            putJsonObject("error") { put("code", code); put("message", message) }
        }
    }
}

internal fun readMcpAuthorization(path: Path): String {
    check(Files.isRegularFile(path, NOFOLLOW_LINKS))
    val posix = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView::class.java, NOFOLLOW_LINKS)
    if (posix != null) {
        check(posix.readAttributes().permissions() == setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
    } else {
        val acl = requireNotNull(Files.getFileAttributeView(path, java.nio.file.attribute.AclFileAttributeView::class.java, NOFOLLOW_LINKS))
        check(acl.acl.isNotEmpty() && acl.acl.all {
            it.principal() == acl.owner && it.type() == java.nio.file.attribute.AclEntryType.ALLOW
        })
    }
    val bytes = Files.newInputStream(path).use { it.readNBytes(4097) }
    check(bytes.size <= 4096)
    val headers = Json.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence = true)).jsonObject
    check(headers.keys == setOf("Authorization"))
    val value = headers.getValue("Authorization").jsonPrimitive.content
    check(value.matches(Regex("Bearer [A-Za-z0-9_-]{43}")))
    return value
}

/** Invoked before ANY Studio preferences, localization, windows or hardware are initialized. */
internal fun runMcpStdio(args: Array<String>, input: InputStream = System.`in`, output: OutputStream = System.out) {
    val port = when {
        args.size == 1 -> 8765
        args.size == 3 && args[1] == "--port" -> args[2].toIntOrNull()
        else -> null
    }
    if (port == null || port !in 1..65535) {
        System.err.println("Usage: PokecomGOStudio --mcp-stdio [--port 1..65535]")
        return
    }
    val properties = System.getProperties().stringPropertyNames().associateWith(System::getProperty)
    val credential = PlatformDesktopMcpCredentialStore.resolveCredentialPath(System.getenv(), properties)
    StdioMcpBridge(port) { readMcpAuthorization(credential) }.run(input, output)
}
