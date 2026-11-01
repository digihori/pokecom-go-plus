package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.desktop.application.development.DevelopmentReadError
import com.digihori.pgp.desktop.application.execution.BoundedExecutionService
import kotlinx.serialization.json.*

internal object BoundedExecutionMcp {
    const val PROPOSE = "pgp_propose_bounded_execution"
    const val OPERATION = "pgp_get_bounded_execution"
    const val DISPLAY = "pgp_get_display"
    val names = setOf(PROPOSE, OPERATION, DISPLAY)
    val allowedKeys = PocketKey.entries.filter { it.name.length == 1 || it.name.startsWith("NUM_") ||
        it in setOf(PocketKey.ENTER, PocketKey.SPACE, PocketKey.CLEAR, PocketKey.BREAK, PocketKey.PLUS, PocketKey.MINUS, PocketKey.DOT, PocketKey.EQUALS, PocketKey.LEFT, PocketKey.RIGHT) }
    private fun string(max: Int = 128) = buildJsonObject { put("type", "string"); put("minLength", 1); put("maxLength", max) }
    private fun number(max: Long, min: Long = 0) = buildJsonObject { put("type", "integer"); put("minimum", min); put("maximum", max) }
    fun tools() = listOf(
        tool(PROPOSE, "Propose PC-1245/PC-1251 execution for local Studio approval with mandatory instruction and cycle limits. Requires paused RUN-mode Session and current PC-1245/PC-1251 text-project revision. Use run/call at a verified BASIC command prompt; firmware readiness is not inferred. action run types CLEAR,R,U,N,ENTER; call types CLEAR,C,A,L,L,SPACE,decimal RAM address,ENTER; keys types explicit key taps; continue resumes current PC without keys. Does not load or force PC. Stops at the first limit, breakpoint/watch, fault or interruption and releases/discards queued keys. Poll pgp_get_bounded_execution, then inspect stopped LCD. A limit or completed key sequence is not proof of program completion, successful input handling or output.", buildJsonObject {
            put("sessionId", string()); put("sessionRevision", number(9_007_199_254_740_991)); put("projectRevision", string())
            put("action", buildJsonObject { put("type", "string"); putJsonArray("enum") { listOf("run", "call", "keys", "continue").forEach { add(it) } } })
            put("callAddress", number(0xc7ff, 0xa000))
            put("keys", buildJsonObject { put("type", "array"); put("minItems", 1); put("maxItems", 64); putJsonObject("items") {
                put("type", "string"); putJsonArray("enum") { allowedKeys.forEach { add(it.name) } }
            } })
            put("maxInstructions", number(1_000_000, 1)); put("maxCycles", number(5_000_000, 1))
        }, listOf("sessionId", "sessionRevision", "projectRevision", "action", "maxInstructions", "maxCycles"), false),
        tool(OPERATION, "Read approval/progress and immutable terminal evidence for a bounded execution request. SUCCEEDED means this request stopped, not that the program ended. inputComplete only means all scheduled key holds/gaps were emitted. A partial sequence is discarded, never silently resumed. Approval only in Studio.", buildJsonObject { put("operationId", string()) }, listOf("operationId"), true),
        tool(DISPLAY, "Read a stopped PC-1245/PC-1251 LCD as pixels, symbols and PNG, with exact Session ID/revision. Never pauses or executes. No ROM bytes or OCR text; enabled=false means a blank visible display. Snapshot is observation, not proof of program completion or input readiness.", buildJsonObject {
            put("sessionId", string()); put("sessionRevision", number(9_007_199_254_740_991))
        }, listOf("sessionId", "sessionRevision"), true),
    )
    private fun tool(name: String, description: String, fields: JsonObject, required: List<String>, readOnly: Boolean) = buildJsonObject {
        put("name", name); put("description", description)
        putJsonObject("inputSchema") { put("type", "object"); put("additionalProperties", false); put("properties", fields); putJsonArray("required") { required.forEach { add(it) } } }
        putJsonObject("outputSchema") { put("type", "object") }
        putJsonObject("annotations") { put("readOnlyHint", readOnly); put("destructiveHint", !readOnly); put("openWorldHint", false) }
    }
    fun string(args: JsonObject, key: String): String {
        val value = args[key] as? JsonPrimitive ?: fail()
        if (!value.isString || value.content.isBlank() || value.content.length > 128) fail()
        return value.content
    }
    fun number(args: JsonObject, key: String, min: Long = 0, max: Long = 9_007_199_254_740_991): Long {
        val value = args[key] as? JsonPrimitive ?: fail()
        if (value.isString) fail()
        return value.longOrNull?.takeIf { it in min..max } ?: fail()
    }
    fun call(service: BoundedExecutionService, name: String, args: JsonObject): JsonObject {
        if (name == OPERATION) {
            if (args.keys != setOf("operationId")) fail()
            return service.operation(string(args, "operationId"))
        }
        val action = string(args, "action")
        val extra = when (action) { "run", "continue" -> emptySet(); "call" -> setOf("callAddress"); "keys" -> setOf("keys"); else -> fail() }
        if (args.keys != setOf("sessionId", "sessionRevision", "projectRevision", "action", "maxInstructions", "maxCycles") + extra) fail()
        val keys = when (action) {
            "run" -> listOf(PocketKey.CLEAR, PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER)
            "call" -> listOf(PocketKey.CLEAR, PocketKey.C, PocketKey.A, PocketKey.L, PocketKey.L, PocketKey.SPACE) +
                number(args, "callAddress", 0xa000, 0xc7ff).toString().map { PocketKey.valueOf("NUM_$it") } + PocketKey.ENTER
            "keys" -> {
                val values = args["keys"] as? JsonArray ?: fail()
                if (values.size !in 1..64) fail()
                values.map { raw ->
                    val value = raw as? JsonPrimitive ?: fail()
                    if (!value.isString) fail()
                    allowedKeys.find { it.name == value.content } ?: fail()
                }
            }
            else -> emptyList()
        }
        return service.propose(string(args, "sessionId"), number(args, "sessionRevision"), string(args, "projectRevision"), action, keys,
            number(args, "maxInstructions", 1, 1_000_000), number(args, "maxCycles", 1, 5_000_000),
            if (action == "call") number(args, "callAddress", 0xa000, 0xc7ff) else null)
    }
    private fun fail(): Nothing = throw DevelopmentReadError("INVALID_ARGUMENTS")
}
