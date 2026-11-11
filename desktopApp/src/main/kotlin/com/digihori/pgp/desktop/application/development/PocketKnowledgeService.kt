package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.debug.Sc61860InstructionSet
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDialect
import com.digihori.pgp.core.source.basic.BasicCodeMeaning
import java.util.Locale
import kotlinx.serialization.json.*

/** Installed, read-only knowledge. Never reads a project, ROM, user file, or network. */
internal class PocketKnowledgeService {
    private data class Entry(
        val id: String, val topic: String, val title: String, val summary: String,
        val keywords: List<String>, val article: String?, val sources: List<String>,
        val basis: String = "implementationDerived", val exampleId: String? = null,
        val machineIds: List<String> = listOf("pc-1245"),
    )

    private val articles = listOf(
        Entry("pc1245.basic", "basic", "PC-1245 BASIC and character codes / BASIC・文字コード",
            "OLD dialect, line numbers, tokens, characters, text escapes and CALL syntax.",
            listOf("BASIC", "PRINT", "INPUT", "IF", "文字", "構文", "トークン", "PI"), "PC1245_BASIC.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicTokenizer.kt",
                "docs/reference/BASIC_TEXT_FORMAT.md")),
        Entry("pc1245.memory", "memory", "PC-1245 memory / メモリ配置と共存",
            "Catalog memory regions, BASIC pointers, endianness and example placement constraints.",
            listOf("RAM", "ROM", "メモリ", "C500", "C600", "ポインター", "共存"), "PC1245_MEMORY.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245MemoryBus.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "docs/reference/machines/RAM_1245_1261.md")),
        Entry("pc1245.display", "display", "PC-1245 LCD direct drawing / LCD直接描画",
            "80x7 pixels, column addressing, LCD enable and firmware redraw limits.",
            listOf("LCD", "表示", "画面", "描画", "F800", "OUTC"), "PC1245_DISPLAY.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245Display.kt",
                "desktopApp/src/main/kotlin/com/digihori/pgp/desktop/application/development/DevelopmentReadService.kt",
                "core/src/commonTest/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245DisplayTest.kt")),
        Entry("sc61860.assembler", "assembler", "Built-in assembler / 内蔵assembler構文",
            "ORG, DB, DW, EQU, expressions, relative branches and unsupported directives.",
            listOf("アセンブラ", "構文", "ORG", "DB", "DW", "EQU", "LP", "CAL", "TEST"), "SC61860_ASSEMBLER.md",
            listOf("docs/guides/SC61860_ASSEMBLY.md",
                "core/src/commonTest/kotlin/com/digihori/pgp/core/debug/Sc61860AssemblerTest.kt"), machineIds = MACHINES),
        Entry("pc1245.execution", "execution", "PC-1245 launch and observation / 起動・観測と未提供情報",
            "Load versus Continue versus RUN/CALL, manual keys and unknown ROM routine ABI.",
            listOf("起動", "実行", "キー", "入力", "RUN", "CALL", "ABI", "ROMルーチン", "フラグ"), "PC1245_EXECUTION.md",
            listOf("docs/reference/AI_INTEGRATION.md", "test-data/assembly/pc1245-hello/README.md")),
        Entry("pc1245.example.hello-world", "example", "PC-1245 HELLO WORLD example / 作例",
            "Retrieve the existing BASIC and assembly project with pgp_get_example.",
            listOf("サンプル", "作例", "HELLO", "表示", "CALL", "RTN"), "PC1245_HELLO_EXAMPLE.md",
            listOf("test-data/assembly/pc1245-hello/README.md",
                "desktopApp/src/test/kotlin/com/digihori/pgp/desktop/project/Pc1245HelloWorldTest.kt"),
            basis = "documentedExample", exampleId = "pc1245-hello-world"),
        Entry("pc1245.example.numeric-labels", "example", "PC-1245 numeric labels / 数値入力と分岐の作例",
            "G01: 1→ONE, 2→TWO, other→OTHER, 0→END. Observed with bounded input and LCD on one local ROM.",
            listOf("G01", "INPUT", "IF", "THEN", "数値", "入力", "ONE", "TWO", "OTHER", "生成"), "PC1245_NUMERIC_LABELS.md",
            listOf("test-data/basic/ai/pc1245-numeric-labels/README.md", "docs/development/AI_PROGRAM_GENERATION_VERIFICATION.md"),
            basis = "emulatorObservedExample", exampleId = "pc1245-numeric-labels"),
        Entry("pc1251.basic", "basic", "PC-1251 BASIC / BASIC・文字コード",
            "PGP OLD dialect code table and limits; ROM editor input uses PC-1251 key mapping.",
            listOf("BASIC", "文字", "トークン", "INPUT", "CALL"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/source/basic/BasicProgramCodec.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251RomBasicInput.kt",
                "desktopApp/src/main/kotlin/com/digihori/pgp/desktop/basic/DesktopBasicLoader.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.memory", "memory", "PC-1251 memory / RAM構成とメモリ配置",
            "Catalog defaults and HARDWARE/UNRESTRICTED distinction. Ask machine spec for active session layout.",
            listOf("RAM", "メモリ", "HARDWARE", "UNRESTRICTED", "B800", "A000"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/api/MachineMemoryLayout.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251Family.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.display", "display", "PC-1251 LCD / 表示の対応範囲",
            "24 character columns; direct drawing and MCP display capture are not exposed for this model.",
            listOf("LCD", "表示", "画面"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251Display.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.execution", "execution", "PC-1251 AI workflow scope / 起動・生成の対応範囲",
            "Project generation, bounded input and stopped LCD capture are supported; real-ROM workflow remains unverified.",
            listOf("RUN", "CALL", "起動", "実行", "生成", "未対応"), "PC1251_SCOPE.md",
            listOf("docs/guides/AI_PROGRAM_GENERATION.md"), machineIds = listOf("pc-1251")),
        Entry("pc1251.example.numeric-labels", "example", "PC-1251 numeric labels / 数値入力と分岐の作例",
            "1→ONE, 2→TWO, other→OTHER, 0→END. Observed with MCP input and LCD on one local ROM.",
            listOf("G01", "INPUT", "IF", "数値", "入力", "ONE", "TWO", "OTHER", "作例"), "PC1251_NUMERIC_LABELS.md",
            listOf("test-data/basic/ai/pc1251-numeric-labels/README.md", "docs/development/AI_MACHINE_ROLLOUT_VERIFICATION.md"),
            basis = "emulatorObservedExample", exampleId = "pc1251-numeric-labels", machineIds = listOf("pc-1251")),
    )
    private val instructions = Sc61860InstructionSet.allDefined().groupBy { it.mnemonic }.toSortedMap()
    private val entries = articles + instructions.map { (mnemonic, _) ->
        Entry("sc61860.instruction.${mnemonic.lowercase(Locale.ROOT)}", "instruction", "SC61860 $mnemonic",
            "Core opcode/operand/control-flow metadata only; flag effects and full semantics are not provided.",
            listOf(mnemonic, "命令", "opcode"), null,
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/debug/Sc61860InstructionSet.kt"), machineIds = MACHINES)
    }
    private val bodies by lazy { articles.associate { it.id to resource("articles/${it.article}") } }

    fun search(machineId: String, query: String, topic: String?, offset: Int, limit: Int): JsonObject {
        machine(machineId)
        if (query.length > 256 || topic != null && topic !in TOPICS || offset !in 0..1_000_000 || limit !in 1..16)
            throw DevelopmentReadError("INVALID_ARGUMENTS")
        val words = query.lowercase(Locale.ROOT).trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val found = entries.filter { entry ->
            machineId in entry.machineIds && (topic == null || entry.topic == topic) && words.all { word ->
                (listOf(entry.id, entry.title, entry.summary) + entry.keywords + bodies[entry.id].orEmpty())
                    .any { it.lowercase(Locale.ROOT).contains(word) }
            }
        }.sortedWith(compareByDescending<Entry> { entry ->
            words.count { it == entry.id || entry.keywords.any { keyword -> keyword.equals(it, ignoreCase = true) } }
        }.thenBy { it.id })
        return response(machineId, "pgp-knowledge-search") {
            put("machineId", machineId); put("query", query); put("topic", topic?.let(::JsonPrimitive) ?: JsonNull)
            put("offset", offset); put("limit", limit); put("total", found.size)
            put("nextOffset", if (offset + limit < found.size) JsonPrimitive(offset + limit) else JsonNull)
            put("items", buildJsonArray { found.drop(offset).take(limit).forEach { add(metadata(it)) } })
        }
    }

    fun get(machineId: String, id: String): JsonObject {
        machine(machineId)
        val entry = entries.find { it.id == id && machineId in it.machineIds } ?: throw DevelopmentReadError("KNOWLEDGE_NOT_FOUND")
        return response(machineId, "pgp-knowledge-item") {
            put("machineId", machineId); put("item", metadata(entry))
            put("body", bodies[id] ?: "Core instruction metadata. See sc61860.assembler for syntax. Full semantics, flags and cycle counts are not provided.")
            put("data", data(entry, machineId))
        }
    }

    fun example(machineId: String, exampleId: String): JsonObject {
        machine(machineId)
        val entry = articles.find { it.exampleId == exampleId && machineId in it.machineIds } ?: throw DevelopmentReadError("EXAMPLE_NOT_FOUND")
        return response(machineId, "pgp-knowledge-example") {
            put("machineId", machineId); put("exampleId", exampleId); put("item", metadata(entry))
            put("guidance", bodies.getValue(entry.id)); put("createsFiles", false)
            put("files", buildJsonArray {
                (if (exampleId in setOf("pc1245-numeric-labels", "pc1251-numeric-labels")) listOf("pgp-project.json", "src/main.bas")
                    else listOf("pgp-project.json", "src/main.bas", "src/hello.asm")).forEach { path ->
                    add(buildJsonObject { put("path", path); put("text", resource("examples/$exampleId/$path")); put("encoding", "UTF-8") })
                }
            })
        }
    }

    private fun data(entry: Entry, machineId: String): JsonObject = buildJsonObject {
        when (entry.id) {
            "pc1245.basic", "pc1251.basic" -> {
                val dialect = Pc1245BasicDialect.definition
                put("dialectId", dialect.id.value); put("maximumLineNumber", dialect.programFormat.maximumLineNumber)
                put("startMarker", dialect.programFormat.startMarker); put("endMarker", dialect.programFormat.endMarker)
                put("lineTerminator", dialect.programFormat.lineTerminator); put("lineNumberEncoding", dialect.programFormat.lineNumberEncoding.name)
                put("codeTable", buildJsonArray { (0..255).forEach { code -> dialect.meaningOf(code)?.let { meaning -> add(buildJsonObject {
                    put("code", code)
                    when (meaning) {
                        is BasicCodeMeaning.Keyword -> { put("kind", "keyword"); put("text", meaning.text) }
                        is BasicCodeMeaning.Character -> { put("kind", "character"); put("text", meaning.value.toString()) }
                        is BasicCodeMeaning.Special -> { put("kind", "special"); put("symbol", meaning.symbol.name) }
                    }
                }) } } })
            }
            "pc1245.memory" -> {
                val spec = catalogSpec()
                put("memoryRegions", spec.getValue("memoryRegions")); put("memoryLayout", spec.getValue("memoryLayout"))
                put("basicTextStart", Pc1245BasicDialect.PROGRAM_TEXT_START)
                put("basicStartPointerLow", Pc1245BasicDialect.PROGRAM_START_POINTER_LOW)
                put("basicEndPointerLow", Pc1245BasicDialect.PROGRAM_END_POINTER_LOW)
                put("basicStorageEndExclusive", Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE)
            }
            "pc1245.display", "pc1251.display" -> put("display", catalogSpec(machineId).getValue("display"))
            "pc1251.memory" -> {
                val spec = catalogSpec(machineId)
                put("memoryRegions", spec.getValue("memoryRegions")); put("memoryLayout", spec.getValue("memoryLayout"))
            }
            "pc1251.execution" -> {
                put("keyboard", catalogSpec(machineId).getValue("keyboard"))
                put("boundedExecutionSupported", true); put("displayCaptureSupported", true); put("projectCreationSupported", true)
            }
            "pc1245.execution" -> put("keyboard", catalogSpec().getValue("keyboard"))
            else -> if (entry.topic == "instruction") {
                put("variants", buildJsonArray { instructions.getValue(entry.title.removePrefix("SC61860 ")).forEach { instruction -> add(buildJsonObject {
                    put("opcode", instruction.opcode); put("mnemonic", instruction.mnemonic); put("length", instruction.length)
                    put("operandEncoding", instruction.operandEncoding.name); put("flow", instruction.flow.name); put("encodable", instruction.encodable)
                }) } })
                put("notProvided", buildJsonArray { listOf("fullSemantics", "flagEffects", "cycleCounts").forEach { add(it) } })
            }
        }
    }

    private fun metadata(entry: Entry) = buildJsonObject {
        put("id", entry.id); put("topic", entry.topic); put("title", entry.title); put("summary", entry.summary)
        put("machineIds", buildJsonArray { entry.machineIds.forEach { add(it) } }); put("keywords", buildJsonArray { entry.keywords.forEach { add(it) } })
        entry.exampleId?.let { put("exampleId", it) }
        put("sources", buildJsonArray {
            (entry.sources + listOfNotNull(entry.article?.let { "docs/reference/knowledge/$it" })).forEach { path ->
                add(buildJsonObject { put("path", path); put("scope", "PGP repository reference; bundled data does not require a checkout") })
            }
        })
        putJsonObject("verification") {
            put("basis", entry.basis); put("realHardware", "notVerified")
            put("romApplicability", "PGP implementation for the listed machineIds; arbitrary ROM revisions and ROM routine ABI are not verified")
            put("evidencePolicy", "Source/test references are provenance, not proof that real-ROM tests ran in this session")
        }
    }

    private fun catalogSpec(machineId: String = "pc-1245") = DesktopDevelopmentReadService { DevelopmentReadState() }.machineSpec(machineId)
    private fun machine(machineId: String) {
        if (machineId !in MACHINES) throw DevelopmentReadError("UNSUPPORTED_KNOWLEDGE_MACHINE")
    }
    private fun resource(path: String): String = checkNotNull(javaClass.getResourceAsStream("/knowledge/$path"))
        .use { stream ->
            val bytes = stream.readNBytes(MAX_BYTES + 1)
            check(bytes.size <= MAX_BYTES)
            bytes.decodeToString(throwOnInvalidSequence = true)
        }
    private fun response(machineId: String, format: String, body: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put("format", format); put("schemaVersion", 1); put("catalogVersion", if (machineId == "pc-1245") "pc1245-v2" else "pc1251-v3"); body()
    }.also { if (it.toString().toByteArray(Charsets.UTF_8).size > MAX_BYTES) throw DevelopmentReadError("RESPONSE_TOO_LARGE") }

    companion object {
        val MACHINES = listOf("pc-1245", "pc-1251")
        val TOPICS = listOf("basic", "memory", "display", "assembler", "execution", "example", "instruction")
        const val MAX_BYTES = 64 * 1024
    }
}
