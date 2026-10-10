package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.ProjectInfo
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal data class DesktopMcpServerInfo(
    val url: String,
    val token: String,
    val port: Int,
    val headerHelperCommand: String,
    val credentialPath: String,
)

internal data class DesktopMcpRequestSummary(
    val capturedAt: String,
    val method: String,
    val toolName: String?,
    val succeeded: Boolean,
)

internal sealed interface DesktopMcpStartResult {
    data class Success(val info: DesktopMcpServerInfo) : DesktopMcpStartResult
    data class Failure(val message: String) : DesktopMcpStartResult
}

internal class DesktopMcpServer(
    private val adapter: PgpMcpToolAdapter,
    private val requestLimitBytes: Int = DEFAULT_REQUEST_LIMIT_BYTES,
    private val token: String = generateToken(),
    private val credentialStore: DesktopMcpCredentialStore = PlatformDesktopMcpCredentialStore(),
    private val onRequest: (DesktopMcpRequestSummary) -> Unit = {},
) : AutoCloseable {
    @Volatile
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null
    private var runningInfo: DesktopMcpServerInfo? = null

    val isRunning: Boolean get() = server != null

    fun start(port: Int = DEFAULT_PORT): DesktopMcpStartResult {
        require(port in 0..65535) { "MCP port must be between 0 and 65535" }
        runningInfo?.let { return DesktopMcpStartResult.Success(it) }
        return runCatching {
            val created = HttpServer.create(InetSocketAddress(LOOPBACK, port), 0)
            val createdExecutor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "pgp-mcp-http").apply { isDaemon = true }
            }
            created.executor = createdExecutor
            created.createContext("/mcp", ::handleMcp)
            created.createContext("/", ::handleRoot)
            created.start()
            executor = createdExecutor
            server = created
            val actualPort = created.address.port
            val credential = credentialStore.publish(token)
            val info = DesktopMcpServerInfo(
                endpoint(actualPort),
                token,
                actualPort,
                credential.headerHelperCommand,
                credential.path.toString(),
            )
            runningInfo = info
            DesktopMcpStartResult.Success(info)
        }.getOrElse { error ->
            stop()
            DesktopMcpStartResult.Failure(error.message ?: error::class.simpleName ?: "Could not start MCP server")
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        runningInfo = null
        executor?.shutdownNow()
        executor = null
        runCatching { credentialStore.removeIfOwned(token) }
    }

    override fun close() = stop()

    private fun handleRoot(exchange: HttpExchange) {
        exchange.use {
            if (!exchange.remoteAddress.address.isLoopbackAddress) {
                sendText(exchange, 403, "Forbidden")
            } else if (exchange.requestMethod == "GET" && exchange.requestURI.path == "/") {
                sendText(exchange, 200, "Pokecom GO Studio read-only MCP server")
            } else {
                sendText(exchange, 404, "Not Found")
            }
        }
    }

    private fun handleMcp(exchange: HttpExchange) {
        exchange.use {
            addCorsHeaders(exchange)
            if (exchange.requestMethod == "OPTIONS") {
                exchange.sendResponseHeaders(204, -1)
                return
            }
            if (!exchange.remoteAddress.address.isLoopbackAddress) {
                sendJson(exchange, 403, rpcError(JsonNull, -32001, "Loopback clients only"))
                return
            }
            if (!isAuthorized(exchange)) {
                exchange.responseHeaders.set("WWW-Authenticate", "Bearer")
                sendJson(exchange, 401, rpcError(JsonNull, -32002, "Unauthorized"))
                return
            }
            if (exchange.requestMethod != "POST") {
                exchange.responseHeaders.set("Allow", "POST, OPTIONS")
                sendJson(exchange, 405, rpcError(JsonNull, -32600, "Only POST is supported in stateless mode"))
                return
            }
            val body = exchange.requestBody.readNBytes(requestLimitBytes + 1)
            if (body.size > requestLimitBytes) {
                sendJson(exchange, 413, rpcError(JsonNull, -32600, "Request exceeds $requestLimitBytes bytes"))
                return
            }
            val request = runCatching { Json.parseToJsonElement(body.decodeToString()).jsonObject }.getOrElse {
                sendJson(exchange, 400, rpcError(JsonNull, -32700, "Parse error"))
                return
            }
            val id = request["id"] ?: JsonNull
            val method = request["method"]?.jsonPrimitive?.contentOrNull
            if (request["jsonrpc"]?.jsonPrimitive?.contentOrNull != "2.0" || method == null) {
                sendJson(exchange, 400, rpcError(id, -32600, "Invalid JSON-RPC request"))
                return
            }
            if (request["id"] == null) {
                onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, true))
                exchange.sendResponseHeaders(202, -1)
                return
            }
            val response = handleRequest(id, method, request["params"])
            sendJson(exchange, 200, response)
        }
    }

    private fun handleRequest(id: JsonElement, method: String, params: JsonElement?): JsonObject = when (method) {
        "initialize" -> rpcResult(id, buildJsonObject {
            val requested = runCatching { params?.jsonObject?.get("protocolVersion")?.jsonPrimitive?.content }.getOrNull()
            put("protocolVersion", if (requested in SUPPORTED_PROTOCOL_VERSIONS) requested!! else MCP_PROTOCOL_VERSION)
            put("capabilities", buildJsonObject {
                put("tools", buildJsonObject { put("listChanged", false) })
            })
            put("serverInfo", buildJsonObject {
                put("name", "pokecom-go-studio")
                put("title", "Pokecom GO Studio")
                put("version", ProjectInfo.VERSION)
            })
            put(
                "instructions",
                "Read-only access to the active paused Pokecom GO Studio emulator session. " +
                    "ROM bytes are not exposed through memory ranges; use bounded disassembly and trace data.",
            )
        }).also { onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, true)) }

        "ping" -> rpcResult(id, buildJsonObject {}).also {
            onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, true))
        }
        "tools/list" -> rpcResult(id, buildJsonObject { put("tools", adapter.tools()) }).also {
            onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, true))
        }
        "tools/call" -> {
            val value = runCatching {
                val objectValue = params?.jsonObject ?: error("params must be an object")
                val name = objectValue["name"]?.jsonPrimitive?.contentOrNull ?: error("name is required")
                val arguments = objectValue["arguments"]?.jsonObject ?: buildJsonObject {}
                name to adapter.call(name, arguments)
            }
            value.fold(
                onSuccess = { (name, result) ->
                    val succeeded = result["isError"]?.jsonPrimitive?.contentOrNull != "true"
                    onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, name, succeeded))
                    rpcResult(id, result)
                },
                onFailure = { error ->
                    onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, false))
                    rpcError(id, -32602, error.message ?: "Invalid tool arguments")
                },
            )
        }
        else -> rpcError(id, -32601, "Method not found: $method").also {
            onRequest(DesktopMcpRequestSummary(Instant.now().toString(), method, null, false))
        }
    }

    private fun isAuthorized(exchange: HttpExchange): Boolean {
        val expected = "Bearer $token".toByteArray(StandardCharsets.UTF_8)
        val actual = exchange.requestHeaders.getFirst("Authorization")
            ?.toByteArray(StandardCharsets.UTF_8) ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    private fun addCorsHeaders(exchange: HttpExchange) {
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
        exchange.responseHeaders.set(
            "Access-Control-Allow-Headers",
            "authorization, content-type, mcp-protocol-version",
        )
        exchange.responseHeaders.set("Access-Control-Expose-Headers", "MCP-Protocol-Version")
    }

    private fun sendJson(exchange: HttpExchange, status: Int, body: JsonObject) {
        val bytes = Json.encodeToString(body).encodeToByteArray()
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun sendText(exchange: HttpExchange, status: Int, text: String) {
        val bytes = text.encodeToByteArray()
        exchange.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun rpcResult(id: JsonElement, result: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0"); put("id", id); put("result", result)
    }

    private fun rpcError(id: JsonElement, code: Int, message: String): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0"); put("id", id)
        put("error", buildJsonObject { put("code", code); put("message", message) })
    }

    private fun endpoint(port: Int): String = "http://${LOOPBACK.hostAddress}:$port/mcp"

    companion object {
        const val DEFAULT_PORT: Int = 8765
        const val DEFAULT_REQUEST_LIMIT_BYTES: Int = 64 * 1024
        const val MCP_PROTOCOL_VERSION: String = "2025-06-18"
        private val SUPPORTED_PROTOCOL_VERSIONS = setOf(MCP_PROTOCOL_VERSION, "2025-03-26")
        private val LOOPBACK: InetAddress = InetAddress.getByName("127.0.0.1")

        private fun generateToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
