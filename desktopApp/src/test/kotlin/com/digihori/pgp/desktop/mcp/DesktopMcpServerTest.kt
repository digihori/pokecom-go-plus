package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.desktop.application.debug.DebugContextCapabilities
import com.digihori.pgp.desktop.application.debug.DebugContextError
import com.digihori.pgp.desktop.application.debug.DebugContextRequest
import com.digihori.pgp.desktop.application.debug.DebugContextResult
import com.digihori.pgp.desktop.application.debug.DebugContextService
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopMcpServerTest {
    @Test
    fun adapterMarksOnlyProposalToolsAsMutationsAndValidatesArguments() {
        val adapter = PgpMcpToolAdapter({ FakeDebugContextService() })
        val tools = adapter.tools()

        assertEquals(setOf(PgpMcpToolAdapter.GET_CAPABILITIES, PgpMcpToolAdapter.GET_DEBUG_CONTEXT, PgpMcpToolAdapter.GET_DEBUG_REGISTRATIONS, PgpMcpToolAdapter.GET_MACHINE_SPEC, PgpMcpToolAdapter.GET_DEVELOPMENT_CONTEXT, "pgp_get_source", "pgp_propose_source_patch", "pgp_build_project", "pgp_get_operation", DebugChangeMcp.PROPOSE, DebugChangeMcp.OPERATION, ExecutionChangeMcp.ARTIFACT, ExecutionChangeMcp.LOAD, ExecutionChangeMcp.CONTINUE, ExecutionChangeMcp.OPERATION), tools.map {
            it.jsonObject.getValue("name").jsonPrimitive.content
        }.toSet())
        assertTrue(tools.filter { it.jsonObject.getValue("name").jsonPrimitive.content !in setOf("pgp_propose_source_patch", "pgp_build_project", DebugChangeMcp.PROPOSE, ExecutionChangeMcp.LOAD, ExecutionChangeMcp.CONTINUE) }.all {
            it.jsonObject.getValue("annotations").jsonObject.getValue("readOnlyHint").jsonPrimitive.content == "true"
        })

        val invalid = adapter.call(
            PgpMcpToolAdapter.GET_DEBUG_CONTEXT,
            buildJsonObject { put("unexpected", 1) },
        )
        assertEquals("true", invalid.getValue("isError").jsonPrimitive.content)
        assertEquals(
            "INVALID_ARGUMENTS",
            invalid.getValue("structuredContent").jsonObject.getValue("error").jsonObject
                .getValue("code").jsonPrimitive.content,
        )

        val running = adapter.call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, buildJsonObject {})
        assertEquals("true", running.getValue("isError").jsonPrimitive.content)
        assertEquals(
            "SESSION_RUNNING",
            running.getValue("structuredContent").jsonObject.getValue("error").jsonObject
                .getValue("code").jsonPrimitive.content,
        )
    }

    @Test
    fun streamableHttpRequiresBearerTokenAndServesMcpMethods() {
        val summaries = mutableListOf<DesktopMcpRequestSummary>()
        val server = DesktopMcpServer(
            adapter = PgpMcpToolAdapter({ FakeDebugContextService() }, developmentProvider = {
                com.digihori.pgp.desktop.application.development.DesktopDevelopmentReadService {
                    com.digihori.pgp.desktop.application.development.DevelopmentReadState()
                }
            }),
            token = "test-token",
            credentialStore = TemporaryCredentialStore(),
            onRequest = summaries::add,
        )
        val info = assertIs<DesktopMcpStartResult.Success>(server.start(0)).info
        assertTrue(info.url.startsWith("http://127.0.0.1:"))
        try {
            val unauthorized = post(info.url, null, initializeRequest())
            assertEquals(401, unauthorized.statusCode())

            val initialize = post(info.url, info.token, initializeRequest())
            assertEquals(200, initialize.statusCode())
            val initializeResult = Json.parseToJsonElement(initialize.body()).jsonObject.getValue("result").jsonObject
            assertEquals("2025-06-18", initializeResult.getValue("protocolVersion").jsonPrimitive.content)

            val toolsResponse = post(
                info.url,
                info.token,
                """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""",
            )
            val tools = Json.parseToJsonElement(toolsResponse.body()).jsonObject
                .getValue("result").jsonObject.getValue("tools").jsonArray
            assertEquals(15, tools.size)

            val callResponse = post(
                info.url,
                info.token,
                """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"pgp_get_capabilities","arguments":{}}}""",
            )
            val callResult = Json.parseToJsonElement(callResponse.body()).jsonObject.getValue("result").jsonObject
            assertEquals("false", callResult.getValue("isError").jsonPrimitive.content)
            assertEquals(
                "pc-1360",
                callResult.getValue("structuredContent").jsonObject.getValue("machineId").jsonPrimitive.content,
            )
            assertTrue(summaries.any { it.toolName == PgpMcpToolAdapter.GET_CAPABILITIES && it.succeeded })
            val registrationsResponse = post(info.url, info.token,
                """{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"pgp_get_debug_registrations","arguments":{}}}""")
            val registrations = Json.parseToJsonElement(registrationsResponse.body()).jsonObject.getValue("result").jsonObject
            assertEquals("false", registrations.getValue("isError").jsonPrimitive.content)
            assertEquals("pgp-debug-registrations", registrations.getValue("structuredContent").jsonObject.getValue("format").jsonPrimitive.content)
            assertEquals("http-session", registrations.getValue("structuredContent").jsonObject.getValue("session").jsonObject.getValue("id").jsonPrimitive.content)
            assertTrue(summaries.any { it.toolName == PgpMcpToolAdapter.GET_DEBUG_REGISTRATIONS && it.succeeded })
            val spec = result(post(info.url, info.token, """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"pgp_get_machine_spec","arguments":{"machineId":"pc-1245"}}}"""))
            assertEquals("pgp-machine-spec", spec.getValue("structuredContent").jsonObject.getValue("format").jsonPrimitive.content)
            val development = result(post(info.url, info.token, """{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"pgp_get_development_context","arguments":{}}}"""))
            assertEquals("pgp-development-context", development.getValue("structuredContent").jsonObject.getValue("format").jsonPrimitive.content)
            assertTrue(summaries.any { it.toolName == PgpMcpToolAdapter.GET_MACHINE_SPEC && it.succeeded })
            assertTrue(summaries.any { it.toolName == PgpMcpToolAdapter.GET_DEVELOPMENT_CONTEXT && it.succeeded })
        } finally {
            server.stop()
        }
        assertFalse(server.isRunning)
    }

    private fun result(response: HttpResponse<String>) = Json.parseToJsonElement(response.body()).jsonObject.getValue("result").jsonObject

    private fun post(url: String, token: String?, body: String): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(body))
        token?.let { builder.header("Authorization", "Bearer $it") }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun initializeRequest(): String =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}"""

    private class TemporaryCredentialStore : DesktopMcpCredentialStore {
        private val path = Files.createTempDirectory("pgp-mcp-test").resolve("headers.json")

        override fun publish(token: String): DesktopMcpCredentialLocation {
            Files.writeString(path, "test")
            return DesktopMcpCredentialLocation(path, "/bin/cat '$path'")
        }

        override fun removeIfOwned(token: String) {
            Files.deleteIfExists(path)
        }
    }

    private class FakeDebugContextService : DebugContextService {
        override fun registrations(): com.digihori.pgp.desktop.application.debug.DebugRegistrationsResult =
            com.digihori.pgp.desktop.application.debug.DebugRegistrationsResult.Success(
                com.digihori.pgp.desktop.application.debug.DebugRegistrationsSnapshot(
                    com.digihori.pgp.desktop.application.debug.DebugSessionContext("http-session", 7L, "pc-1360", "paused", null),
                    emptyList(), emptyList(),
                ),
            )

        override fun capabilities(): DebugContextCapabilities = DebugContextCapabilities(
            format = "pgp-debug-context",
            schemaVersion = 1,
            machineId = "pc-1360",
            maxMemoryRanges = 8,
            maxMemoryBytes = 4_096,
            maxDisassemblyInstructions = 256,
            maxHistoryEntries = 4_096,
            memoryAccessHistorySupported = true,
            bankHistorySupported = true,
            romBytesAllowed = false,
        )

        override fun capture(request: DebugContextRequest): DebugContextResult =
            DebugContextResult.Failure(DebugContextError.SessionRunning)
    }
}
