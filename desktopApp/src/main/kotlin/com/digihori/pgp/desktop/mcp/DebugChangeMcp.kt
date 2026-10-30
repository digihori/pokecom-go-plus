package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.application.development.DevelopmentReadError
import kotlinx.serialization.json.*

internal object DebugChangeMcp {
    const val PROPOSE = "pgp_propose_debug_action"
    const val OPERATION = "pgp_get_debug_operation"
    fun tools(): List<JsonObject> {
        fun string(max: Int, empty: Boolean = false) = buildJsonObject { put("type", "string"); put("maxLength", max); put("minLength", if (empty) 0 else 1) }
        fun number(max: Long) = buildJsonObject { put("type", "integer"); put("minimum", 0); put("maximum", max) }
        fun bool() = buildJsonObject { put("type", "boolean") }
        fun schema(properties: JsonObject, required: List<String>) = buildJsonObject {
            put("type", "object"); put("additionalProperties", false); put("properties", properties)
            put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        }
        val variants = listOf("save_breakpoint", "remove_breakpoint", "save_watch", "remove_watch", "trace", "pause", "step").map { action ->
            val fields = buildJsonObject {
                if (action in setOf("save_breakpoint", "remove_breakpoint", "save_watch", "remove_watch")) put("id", number(9_007_199_254_740_991))
                if (action == "save_breakpoint") { put("address", number(65535)); put("condition", string(128, true)); put("hitCondition", string(32, true)); put("bank", buildJsonObject { put("type", buildJsonArray { add("integer"); add("null") }); put("minimum", 0); put("maximum", 255); put("description", "Optional ROM bank in the machine bank window; null clears bank scope. Omission preserves existing scope on edit, or any bank on create.") }) }
                if (action == "save_watch") {
                    put("startAddress", number(65535)); put("endAddress", number(65535))
                    put("kind", buildJsonObject { put("type", "string"); put("enum", buildJsonArray { listOf("change", "read", "write", "readWrite").forEach { add(JsonPrimitive(it)) } }) })
                }
                if (action in setOf("save_breakpoint", "save_watch", "trace")) put("enabled", bool())
                if (action == "trace") put("capacity", buildJsonObject { put("type", "integer"); put("minimum", 16); put("maximum", 65536) })
            }
            val required = when (action) {
                "save_breakpoint" -> listOf("address", "enabled")
                "remove_breakpoint", "remove_watch" -> listOf("id")
                "save_watch" -> listOf("startAddress", "endAddress", "kind", "enabled")
                "trace" -> listOf("enabled", "capacity")
                else -> emptyList()
            }
            schema(buildJsonObject {
                put("sessionId", string(128))
                if (action != "pause") put("sessionRevision", number(9_007_199_254_740_991))
                put("action", buildJsonObject { put("type", "string"); put("const", action) })
                put("parameters", schema(fields, required))
            }, listOf("sessionId", "action", "parameters") + if (action == "pause") emptyList() else listOf("sessionRevision"))
        }
        fun tool(name: String, description: String, input: JsonObject, readOnly: Boolean) = buildJsonObject {
            put("name", name); put("description", description); put("inputSchema", input)
            put("outputSchema", buildJsonObject { put("type", "object") })
            put("annotations", buildJsonObject { put("readOnlyHint", readOnly); put("destructiveHint", !readOnly); put("idempotentHint", readOnly); put("openWorldHint", false) })
        }
        return listOf(
            tool(PROPOSE, "Queue a debug action for local Studio UI approval. Stop-only breakpoint/watch/trace edits and exactly one Step require current session ID/revision; pause targets the same live Session without a revision. No Continue, Load, Reset or direct memory writes. Step may change RAM/I/O. Read operation then fresh context after approval.", buildJsonObject { put("type", "object"); put("oneOf", JsonArray(variants)) }, false),
            tool(OPERATION, "Read a debug action's approval/result status and before/after revision. Approval is available only in Studio UI.", schema(buildJsonObject { put("operationId", string(128)) }, listOf("operationId")), true),
        )
    }
    fun call(service: DebugChangeService, name: String, args: JsonObject): JsonObject {
        fun string(key: String): String {
            val value = args[key] as? JsonPrimitive ?: fail()
            if (!value.isString || value.content.isBlank() || value.content.length > 128) fail()
            return value.content
        }
        if (name == OPERATION) {
            if (args.keys != setOf("operationId")) fail()
            return service.operation(string("operationId"))
        }
        if (args.keys.any { it !in setOf("sessionId", "sessionRevision", "action", "parameters") } || !args.keys.containsAll(setOf("sessionId", "action", "parameters"))) fail()
        val revision = args["sessionRevision"]?.let {
            val value = it as? JsonPrimitive ?: fail()
            if (value.isString) fail()
            value.longOrNull?.takeIf { it in 0..9_007_199_254_740_991 } ?: fail()
        }
        return service.propose(string("sessionId"), revision, DebugAction(string("action"), args["parameters"] as? JsonObject ?: fail()))
    }
    private fun fail(): Nothing = throw DevelopmentReadError("INVALID_ARGUMENTS")
}
