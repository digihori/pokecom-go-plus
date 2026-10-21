package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.desktop.application.development.DevelopmentReadError
import com.digihori.pgp.desktop.application.execution.ExecutionChangeService
import kotlinx.serialization.json.*

internal object ExecutionChangeMcp {
    const val ARTIFACT = "pgp_get_build_artifact"
    const val LOAD = "pgp_propose_load"
    const val CONTINUE = "pgp_propose_continue"
    const val OPERATION = "pgp_get_execution_operation"
    val names = setOf(ARTIFACT, LOAD, CONTINUE, OPERATION)
    fun tools(): List<JsonObject> = listOf(
        tool(ARTIFACT, "Read the current successful in-memory built-in text-project artifact ID, content revision and size. No byte content. Rebuild after restarting Studio; edited dump files are never imported.", emptyList(), true),
        tool(LOAD, "Queue Load of the exact successful artifact for local Studio approval. Requires paused, matching Session/revision and unchanged project contents. Preflights all writes including BASIC pointers. Load stays paused; no RUN/CALL input or PC jump.", listOf("sessionId", "sessionRevision", "projectRevision", "artifactId"), false),
        tool(CONTINUE, "Queue resume from current PC for local Studio approval. Requires paused matching Session/revision and project contents. Does not type RUN/CALL or jump to an entry point. Runs until breakpoint/watch, fault or manual Pause; success means started, not program finished.", listOf("sessionId", "sessionRevision", "projectRevision"), false),
        tool(OPERATION, "Read Load/Continue approval and outcome, target Session, revisions, artifact ID and bounded write ranges. Approval is possible only in Studio UI.", listOf("operationId"), true),
    )
    private fun tool(name: String, description: String, keys: List<String>, readOnly: Boolean) = buildJsonObject {
        put("name", name); put("description", description)
        put("inputSchema", buildJsonObject {
            put("type", "object"); put("additionalProperties", false)
            put("properties", buildJsonObject { keys.forEach { key -> put(key, buildJsonObject {
                if (key == "sessionRevision") { put("type", "integer"); put("minimum", 0); put("maximum", 9_007_199_254_740_991L) }
                else { put("type", "string"); put("minLength", 1); put("maxLength", 128) }
            }) } })
            put("required", buildJsonArray { keys.forEach { add(JsonPrimitive(it)) } })
        })
        put("outputSchema", buildJsonObject { put("type", "object") })
        put("annotations", buildJsonObject { put("readOnlyHint", readOnly); put("destructiveHint", !readOnly); put("idempotentHint", readOnly); put("openWorldHint", false) })
    }
    fun call(service: ExecutionChangeService, name: String, args: JsonObject): JsonObject {
        val expected = when (name) {
            ARTIFACT -> emptySet()
            LOAD -> setOf("sessionId", "sessionRevision", "projectRevision", "artifactId")
            CONTINUE -> setOf("sessionId", "sessionRevision", "projectRevision")
            OPERATION -> setOf("operationId")
            else -> fail()
        }
        if (args.keys != expected) fail()
        fun string(key: String): String {
            val value = args[key] as? JsonPrimitive ?: fail()
            if (!value.isString || value.content.isBlank() || value.content.length > 128) fail()
            return value.content
        }
        if (name == ARTIFACT) return service.artifact()
        if (name == OPERATION) return service.operation(string("operationId"))
        val rawRevision = args["sessionRevision"] as? JsonPrimitive ?: fail()
        if (rawRevision.isString) fail()
        val revision = rawRevision.longOrNull?.takeIf { it in 0..9_007_199_254_740_991 } ?: fail()
        return if (name == LOAD) service.proposeLoad(string("sessionId"), revision, string("projectRevision"), string("artifactId"))
            else service.proposeContinue(string("sessionId"), revision, string("projectRevision"))
    }
    private fun fail(): Nothing = throw DevelopmentReadError("INVALID_ARGUMENTS")
}
