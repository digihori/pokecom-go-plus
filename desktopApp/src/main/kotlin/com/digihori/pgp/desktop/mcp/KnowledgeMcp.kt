package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.desktop.application.development.DevelopmentReadError
import com.digihori.pgp.desktop.application.development.PocketKnowledgeService
import kotlinx.serialization.json.*

internal object KnowledgeMcp {
    const val SEARCH = "pgp_search_knowledge"
    const val GET = "pgp_get_knowledge"
    const val EXAMPLE = "pgp_get_example"
    val names = setOf(SEARCH, GET, EXAMPLE)

    fun tools(): List<JsonObject> = names.map { name -> buildJsonObject {
        put("name", name)
        put("description", when (name) {
            SEARCH -> "Search the bundled knowledge for supported machines by keyword/topic. Start here for generation guidance, BASIC, memory, LCD, buzzer and melodies, assembler, instruction metadata, examples and known gaps. No session, ROM or project required. Use includeContent=true to fetch matching articles, provenance and structured data in this one call instead of search followed by individual reads. Batch known IDs with ids=[...] to get their full content in one call; omit query and topic for that mode. Page with offset/limit. All query words must match."
            GET -> "Read a single knowledge ID when only one item is needed. For several IDs use pgp_search_knowledge with ids in one call. Returns provenance, verification limits, article and Core-derived data. Selected instruction items also provide Core-derived register effects, flags and cycles in data.coreBehavior. No file paths accepted."
            else -> "Read a bundled example by exampleId from knowledge search. Returns manifest and BASIC/assembly text with applicability and verification guidance; does not create files, build, load or run."
        })
        putJsonObject("inputSchema") {
            put("type", "object"); put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("machineId") { put("type", "string"); putJsonArray("enum") { PocketKnowledgeService.MACHINES.forEach { add(it) } } }
                when (name) {
                    SEARCH -> {
                        putJsonObject("includeContent") { put("type", "boolean"); put("description", "Return article bodies and structured data with search matches to reduce tool calls. Defaults to false for query/topic searches and true for exact IDs.") }
                        putJsonObject("ids") {
                            put("type", "array"); put("minItems", 1); put("maxItems", 16); put("uniqueItems", true)
                            put("description", "Batch exact knowledge IDs for the selected machine. Omit query and topic. Includes full content by default.")
                            putJsonObject("items") { put("type", "string"); put("minLength", 1); put("maxLength", 128) }
                        }
                        putJsonObject("query") { put("type", "string"); put("maxLength", 256) }
                        putJsonObject("topic") { put("type", "string"); putJsonArray("enum") { PocketKnowledgeService.TOPICS.forEach { add(it) } } }
                        putJsonObject("offset") { put("type", "integer"); put("minimum", 0); put("maximum", 1_000_000) }
                        putJsonObject("limit") { put("type", "integer"); put("minimum", 1); put("maximum", 16) }
                    }
                    else -> putJsonObject(if (name == GET) "id" else "exampleId") {
                        put("type", "string"); put("minLength", 1); put("maxLength", 128)
                    }
                }
            }
            putJsonArray("required") { add("machineId"); if (name != SEARCH) add(if (name == GET) "id" else "exampleId") }
        }
        putJsonObject("outputSchema") { put("type", "object") }
        putJsonObject("annotations") {
            put("readOnlyHint", true); put("destructiveHint", false); put("idempotentHint", true); put("openWorldHint", false)
        }
    } }

    fun call(service: PocketKnowledgeService, name: String, args: JsonObject): JsonObject {
        val allowed = if (name == SEARCH) setOf("machineId", "query", "topic", "offset", "limit", "includeContent", "ids")
            else setOf("machineId", if (name == GET) "id" else "exampleId")
        if (args.keys.any { it !in allowed }) invalid()
        fun string(key: String, required: Boolean = true, max: Int = 128): String? {
            val value = args[key] ?: return if (required) invalid() else null
            val primitive = value as? JsonPrimitive ?: invalid()
            if (!primitive.isString || primitive.content.length > max || (key != "query" && primitive.content.isBlank())) invalid()
            return primitive.content
        }
        fun integer(key: String, default: Int): Int {
            val value = args[key] ?: return default
            val primitive = value as? JsonPrimitive ?: invalid()
            if (primitive.isString) invalid()
            return primitive.intOrNull ?: invalid()
        }
        val machine = string("machineId")!!
        val ids = args["ids"]?.let {
            val values = it as? JsonArray ?: invalid()
            if (values.size !in 1..16 || "query" in args || "topic" in args) invalid()
            values.map { value ->
                val id = value as? JsonPrimitive ?: invalid()
                if (!id.isString) invalid()
                id.content
            }
        }
        val includeContent = args["includeContent"]?.let {
            val primitive = it as? JsonPrimitive ?: invalid()
            if (primitive.isString) invalid()
            primitive.booleanOrNull ?: invalid()
        } ?: (ids != null)
        return when (name) {
            SEARCH -> service.search(
                machine, string("query", false, 256) ?: "", string("topic", false),
                integer("offset", 0), integer("limit", ids?.size ?: 8), includeContent, ids,
            )
            GET -> service.get(machine, string("id")!!)
            EXAMPLE -> service.example(machine, string("exampleId")!!)
            else -> throw DevelopmentReadError("UNKNOWN_TOOL")
        }
    }
    private fun invalid(): Nothing = throw DevelopmentReadError("INVALID_ARGUMENTS")
}
