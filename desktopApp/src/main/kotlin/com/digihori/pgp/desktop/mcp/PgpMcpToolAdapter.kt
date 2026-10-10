package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.desktop.application.debug.DebugContextError
import com.digihori.pgp.desktop.application.debug.DebugContextJsonWriter
import com.digihori.pgp.desktop.application.debug.DebugContextRequest
import com.digihori.pgp.desktop.application.debug.DebugContextResult
import com.digihori.pgp.desktop.application.debug.DebugContextService
import com.digihori.pgp.desktop.application.debug.DebugMemoryRangeRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal fun interface McpApplicationDispatcher {
    fun dispatch(block: () -> JsonObject): JsonObject
}

internal class PgpMcpToolAdapter(
    private val serviceProvider: () -> DebugContextService?,
    private val applicationDispatcher: McpApplicationDispatcher = McpApplicationDispatcher { it() },
) {
    fun tools(): JsonArray = buildJsonArray {
        add(buildJsonObject {
            put("name", GET_CAPABILITIES)
            put("title", "Get Pokecom GO capabilities")
            put("description", "Returns the active machine and read-only Debug Context limits.")
            put("inputSchema", objectSchema())
            put("outputSchema", buildJsonObject { put("type", "object") })
            put("annotations", readOnlyAnnotations())
        })
        add(buildJsonObject {
            put("name", GET_DEBUG_CONTEXT)
            put("title", "Get Pokecom GO Debug Context")
            put(
                "description",
                "Returns versioned CPU, stop, selected memory, disassembly, ROM location, access, bank, and trace data. " +
                    "The emulator must already be paused or faulted.",
            )
            put("inputSchema", debugContextInputSchema())
            put("outputSchema", buildJsonObject { put("type", "object") })
            put("annotations", readOnlyAnnotations())
        })
    }

    fun call(name: String, arguments: JsonObject): JsonObject = applicationDispatcher.dispatch {
        val service = serviceProvider() ?: return@dispatch toolError("NO_ACTIVE_SESSION", "No active emulator session")
        when (name) {
            GET_CAPABILITIES -> capabilities(service)
            GET_DEBUG_CONTEXT -> debugContext(service, arguments)
            else -> toolError("UNKNOWN_TOOL", "Unknown tool: $name")
        }
    }

    private fun capabilities(service: DebugContextService): JsonObject {
        val value = service.capabilities()
        val structured = buildJsonObject {
            put("format", value.format)
            put("schemaVersion", value.schemaVersion)
            put("machineId", value.machineId)
            put("maxMemoryRanges", value.maxMemoryRanges)
            put("maxMemoryBytes", value.maxMemoryBytes)
            put("maxDisassemblyInstructions", value.maxDisassemblyInstructions)
            put("maxHistoryEntries", value.maxHistoryEntries)
            put("memoryAccessHistorySupported", value.memoryAccessHistorySupported)
            put("bankHistorySupported", value.bankHistorySupported)
            put("romBytesAllowed", value.romBytesAllowed)
        }
        return toolSuccess(structured, "Read-only Pokecom GO capabilities")
    }

    private fun debugContext(service: DebugContextService, arguments: JsonObject): JsonObject {
        val request = runCatching { arguments.toDebugContextRequest() }.getOrElse {
            return toolError("INVALID_ARGUMENTS", it.message ?: "Invalid arguments")
        }
        return when (val result = service.capture(request)) {
            is DebugContextResult.Success -> {
                val structured = Json.parseToJsonElement(
                    DebugContextJsonWriter.write(result.context).decodeToString(),
                ).jsonObject
                toolSuccess(structured, "Captured ${result.context.session.machineId} Debug Context")
            }
            is DebugContextResult.Failure -> when (val error = result.error) {
                DebugContextError.SessionRunning -> toolError(
                    "SESSION_RUNNING",
                    "Pause the emulator before requesting a Debug Context",
                )
                is DebugContextError.InvalidRequest -> toolError("INVALID_ARGUMENTS", error.message)
                is DebugContextError.RomBytesNotAllowed -> toolError(
                    "ROM_BYTES_NOT_ALLOWED",
                    "ROM bytes are not exposed through memory ranges; request bounded disassembly instead",
                    buildJsonObject {
                        put("startAddress", error.startAddress)
                        put("length", error.length)
                    },
                )
            }
        }
    }

    private fun JsonObject.toDebugContextRequest(): DebugContextRequest {
        rejectUnknownKeys(
            setOf(
                "memoryRanges", "disassemblyStartAddress", "disassemblyInstructionCount",
                "memoryAccessHistoryLimit", "bankHistoryLimit", "traceLimit",
            ),
        )
        val ranges = get("memoryRanges")?.let { element ->
            element.asArray("memoryRanges").mapIndexed { index, item ->
                val range = item.asObject("memoryRanges[$index]")
                range.rejectUnknownKeys(setOf("startAddress", "length"))
                DebugMemoryRangeRequest(
                    startAddress = range.requiredInt("startAddress"),
                    length = range.requiredInt("length"),
                )
            }
        }.orEmpty()
        return DebugContextRequest(
            memoryRanges = ranges,
            disassemblyStartAddress = optionalInt("disassemblyStartAddress"),
            disassemblyInstructionCount = optionalInt("disassemblyInstructionCount") ?: 0,
            memoryAccessHistoryLimit = optionalInt("memoryAccessHistoryLimit") ?: 256,
            bankHistoryLimit = optionalInt("bankHistoryLimit") ?: 256,
            traceLimit = optionalInt("traceLimit") ?: 256,
        )
    }

    private fun JsonObject.rejectUnknownKeys(allowed: Set<String>) {
        val unknown = keys - allowed
        require(unknown.isEmpty()) { "Unknown arguments: ${unknown.sorted().joinToString()}" }
    }

    private fun JsonObject.requiredInt(name: String): Int =
        optionalInt(name) ?: error("$name must be an integer")

    private fun JsonObject.optionalInt(name: String): Int? {
        val value = get(name) ?: return null
        require(value !is JsonNull) { "$name must be an integer" }
        return value.jsonPrimitive.intOrNull ?: error("$name must be an integer")
    }

    private fun JsonElement.asArray(name: String): JsonArray =
        runCatching { jsonArray }.getOrElse { error("$name must be an array") }

    private fun JsonElement.asObject(name: String): JsonObject =
        runCatching { jsonObject }.getOrElse { error("$name must be an object") }

    private fun toolSuccess(structured: JsonObject, summary: String): JsonObject = buildJsonObject {
        put("structuredContent", structured)
        put("content", buildJsonArray {
            add(buildJsonObject { put("type", "text"); put("text", summary) })
        })
        put("isError", false)
    }

    private fun toolError(code: String, message: String, details: JsonObject? = null): JsonObject = buildJsonObject {
        put("structuredContent", buildJsonObject {
            put("error", buildJsonObject {
                put("code", code)
                put("message", message)
                details?.let { put("details", it) }
            })
        })
        put("content", buildJsonArray {
            add(buildJsonObject { put("type", "text"); put("text", "$code: $message") })
        })
        put("isError", true)
    }

    private fun objectSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
        put("additionalProperties", false)
    }

    private fun debugContextInputSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("memoryRanges", buildJsonObject {
                put("type", "array"); put("maxItems", 8)
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("startAddress", integerSchema(0, 0xffff))
                        put("length", integerSchema(1, 4_096))
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("startAddress")); add(JsonPrimitive("length")) })
                    put("additionalProperties", false)
                })
            })
            put("disassemblyStartAddress", integerSchema(0, 0xffff))
            put("disassemblyInstructionCount", integerSchema(0, 256))
            put("memoryAccessHistoryLimit", integerSchema(0, 4_096))
            put("bankHistoryLimit", integerSchema(0, 4_096))
            put("traceLimit", integerSchema(0, 4_096))
        })
        put("additionalProperties", false)
    }

    private fun integerSchema(minimum: Int, maximum: Int): JsonObject = buildJsonObject {
        put("type", "integer"); put("minimum", minimum); put("maximum", maximum)
    }

    private fun readOnlyAnnotations(): JsonObject = buildJsonObject {
        put("readOnlyHint", true)
        put("destructiveHint", false)
        put("openWorldHint", false)
    }

    companion object {
        const val GET_CAPABILITIES: String = "pgp_get_capabilities"
        const val GET_DEBUG_CONTEXT: String = "pgp_get_debug_context"
    }
}
