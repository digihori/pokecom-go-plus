package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860InstructionSet
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDialect
import com.digihori.pgp.desktop.application.development.PocketKnowledgeService
import com.digihori.pgp.desktop.codexMcpConfiguration
import java.io.File
import kotlinx.serialization.json.*
import kotlin.test.*

class PocketKnowledgeTest {
    private val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
    private fun call(name: String, args: String): JsonObject = adapter.call(name, Json.parseToJsonElement(args).jsonObject)
    private fun ok(name: String, args: String): JsonObject {
        val result = call(name, args)
        assertEquals(JsonPrimitive(false), result["isError"], result.toString())
        val value = result.getValue("structuredContent").jsonObject
        assertEquals(value, Json.parseToJsonElement(result.getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content))
        assertTrue(value.toString().toByteArray().size <= PocketKnowledgeService.MAX_BYTES)
        return value
    }

    @Test fun discoversKnowledgeWithoutSessionAndPaginatesWithoutGaps() {
        val all = mutableListOf<String>()
        var offset = 0
        while (true) {
            val page = ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1245","offset":$offset,"limit":3}""")
            all += page.getValue("items").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }
            val next = page.getValue("nextOffset")
            if (next == JsonNull) { assertEquals(page.getValue("total").jsonPrimitive.int, all.size); break }
            assertTrue(next.jsonPrimitive.int > offset)
            offset = next.jsonPrimitive.int
        }
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.contains("pc1245.basic"))
        val japanese = ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1245","query":"表示","topic":"display"}""")
        assertEquals("pc1245.display", japanese.getValue("items").jsonArray.single().jsonObject.getValue("id").jsonPrimitive.content)
        val instruction = ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1245","query":"lia","topic":"instruction"}""")
        assertEquals("sc61860.instruction.lia", instruction.getValue("items").jsonArray.first().jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals(0, ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1245","query":"no-such-knowledge"}""").getValue("total").jsonPrimitive.int)
    }

    @Test fun instructionVariantsAndBasicTablesComeFromCoreDefinitions() {
        for (mnemonic in listOf("LIA", "LP", "CAL", "NOPT", "UNDOC")) {
            val item = ok(KnowledgeMcp.GET, """{"machineId":"pc-1245","id":"sc61860.instruction.${mnemonic.lowercase()}"}""")
            val data = item.getValue("data").jsonObject
            val actual = data.getValue("variants").jsonArray
            val expected = Sc61860InstructionSet.allDefined().filter { it.mnemonic == mnemonic }
            assertEquals(expected.map { it.opcode }, actual.map { it.jsonObject.getValue("opcode").jsonPrimitive.int })
            assertEquals(expected.map { it.encodable }, actual.map { it.jsonObject.getValue("encodable").jsonPrimitive.boolean })
            assertTrue(data.getValue("notProvided").jsonArray.contains(JsonPrimitive("flagEffects")))
        }
        val basic = ok(KnowledgeMcp.GET, """{"machineId":"pc-1245","id":"pc1245.basic"}""").getValue("data").jsonObject
        assertEquals(Pc1245BasicDialect.definition.programFormat.maximumLineNumber, basic.getValue("maximumLineNumber").jsonPrimitive.int)
        val table = basic.getValue("codeTable").jsonArray.map { it.jsonObject }
        assertEquals(Pc1245BasicDialect.definition.codeOfKeyword("CALL"), table.single { it["kind"] == JsonPrimitive("keyword") && it["text"] == JsonPrimitive("CALL") }.getValue("code").jsonPrimitive.int)
        assertFalse(table.single { it["symbol"] == JsonPrimitive("PI") }.getValue("code") == table.single { it["kind"] == JsonPrimitive("keyword") && it["text"] == JsonPrimitive("PI") }.getValue("code"))
    }

    @Test fun bundledArticlesHaveResolvableProvenanceAndExplicitLimits() {
        val service = PocketKnowledgeService()
        val items = service.search("pc-1245", "", null, 0, 16).getValue("items").jsonArray
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
        for (summary in items) {
            val id = summary.jsonObject.getValue("id").jsonPrimitive.content
            val item = service.get("pc-1245", id)
            assertTrue(item.getValue("body").jsonPrimitive.content.isNotBlank())
            val metadata = item.getValue("item").jsonObject
            assertEquals("notVerified", metadata.getValue("verification").jsonObject.getValue("realHardware").jsonPrimitive.content)
            for (source in metadata.getValue("sources").jsonArray) {
                val path = source.jsonObject.getValue("path").jsonPrimitive.content
                assertFalse(File(path).isAbsolute)
                assertTrue(File(root, path).isFile, "Missing provenance: $path")
            }
        }
        val display = service.get("pc-1245", "pc1245.display").getValue("data").jsonObject.getValue("display").jsonObject
        assertEquals(80, display.getValue("directDrawing").jsonObject.getValue("dotColumns").jsonPrimitive.int)
        assertEquals(0xc000, service.get("pc-1245", "pc1245.memory").getValue("data").jsonObject.getValue("basicTextStart").jsonPrimitive.int)
    }

    @Test fun bundledExampleMatchesBuildInputsAndAssembles() {
        val result = ok(KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1245","exampleId":"pc1245-hello-world"}""")
        assertEquals(JsonPrimitive(false), result["createsFiles"])
        val files = result.getValue("files").jsonArray.associate {
            it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
        }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/hello.asm"), files.keys)
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }.first { File(it, "test-data").isDirectory }
        for ((path, text) in files) assertEquals(File(root, "test-data/assembly/pc1245-hello/$path").readText(), text)
        assertIs<com.digihori.pgp.core.debug.Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/hello.asm")))
    }

    @Test fun numericLabelsExampleIsBundledAsBasicOnlyWithExplicitObservationLimits() {
        val result = ok(KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1245","exampleId":"pc1245-numeric-labels"}""")
        assertEquals("pc1245-v2", result.getValue("catalogVersion").jsonPrimitive.content)
        val files = result.getValue("files").jsonArray.associate { it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content }
        assertEquals(setOf("pgp-project.json", "src/main.bas"), files.keys)
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }.first { File(it, "test-data").isDirectory }
        for ((path, text) in files) assertEquals(File(root, "test-data/basic/ai/pc1245-numeric-labels/$path").readText(), text)
        assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
            com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), com.digihori.pgp.core.rom.MachineId("pc-1245")))
        assertEquals("emulatorObservedExample", result.getValue("item").jsonObject.getValue("verification").jsonObject.getValue("basis").jsonPrimitive.content)
        assertTrue(result.getValue("guidance").jsonPrimitive.content.contains("inputComplete"))
    }

    @Test fun rejectsUnknownKeysTypesRangesAndPathsWithoutReadingFiles() {
        val invalidSearches = listOf("{}", """{"machineId":null}""", """{"machineId":"pc-1245","query":0}""",
            """{"machineId":"pc-1245","topic":null}""", """{"machineId":"pc-1245","topic":"unknown"}""",
            """{"machineId":"pc-1245","limit":"8"}""", """{"machineId":"pc-1245","offset":-1}""",
            """{"machineId":"pc-1245","limit":17}""", """{"machineId":"pc-1245","path":"/etc/passwd"}""")
        for (args in invalidSearches) assertError("INVALID_ARGUMENTS", KnowledgeMcp.SEARCH, args)
        assertError("UNSUPPORTED_KNOWLEDGE_MACHINE", KnowledgeMcp.SEARCH, """{"machineId":"pc-1350"}""")
        assertError("KNOWLEDGE_NOT_FOUND", KnowledgeMcp.GET, """{"machineId":"pc-1245","id":"../../etc/passwd"}""")
        assertError("EXAMPLE_NOT_FOUND", KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1245","exampleId":"/etc/passwd"}""")
    }

    @Test fun publicationAndCopiedCodexConfigIncludeAllKnowledgeTools() {
        val published = adapter.tools().map { it.jsonObject.getValue("name").jsonPrimitive.content }
        assertEquals(published, PgpMcpToolAdapter.publicToolNames)
        val config = codexMcpConfiguration(DesktopMcpServerInfo("http://127.0.0.1:8765/mcp", "not-a-real-token", 8765, "helper", "credential"))
        for (name in KnowledgeMcp.names) {
            assertTrue(config.contains("\"$name\""))
            val tool = adapter.tools().single { it.jsonObject["name"] == JsonPrimitive(name) }.jsonObject
            assertEquals(JsonPrimitive(true), tool.getValue("annotations").jsonObject["readOnlyHint"])
        }
    }

    @Test fun pc1251KnowledgeIsIsolatedAndUsesItsOwnCatalogLayout() {
        val page = ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1251","topic":"memory"}""")
        assertEquals("pc1251-v3", page.getValue("catalogVersion").jsonPrimitive.content)
        assertEquals("pc1251.memory", page.getValue("items").jsonArray.single().jsonObject.getValue("id").jsonPrimitive.content)
        val memory = ok(KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"pc1251.memory"}""")
        val layout = memory.getValue("data").jsonObject.getValue("memoryLayout").jsonObject
        assertEquals(0xb800, layout.getValue("defaultBasicStartAddress").jsonPrimitive.int)
        assertEquals("catalogDefaults", layout.getValue("scope").jsonPrimitive.content)
        val display = ok(KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"pc1251.display"}""")
            .getValue("data").jsonObject.getValue("display").jsonObject
        assertEquals(24, display.getValue("characterColumns").jsonPrimitive.int)
        assertEquals(JsonNull, display.getValue("directDrawing"))
        val basic = ok(KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"pc1251.basic"}""")
        assertEquals(Pc1245BasicDialect.definition.id.value, basic.getValue("data").jsonObject.getValue("dialectId").jsonPrimitive.content)
        assertEquals(JsonArray(listOf(JsonPrimitive("pc-1251"))), basic.getValue("item").jsonObject.getValue("machineIds"))
        assertError("KNOWLEDGE_NOT_FOUND", KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"pc1245.memory"}""")
        assertError("KNOWLEDGE_NOT_FOUND", KnowledgeMcp.GET, """{"machineId":"pc-1245","id":"pc1251.basic"}""")
        assertError("EXAMPLE_NOT_FOUND", KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1251","exampleId":"pc1245-numeric-labels"}""")
        assertEquals(1, ok(KnowledgeMcp.SEARCH, """{"machineId":"pc-1251","topic":"example"}""").getValue("total").jsonPrimitive.int)
        for (id in listOf("sc61860.assembler", "sc61860.instruction.lia", "pc1251.execution")) {
            val item = ok(KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"$id"}""")
            assertTrue(item.getValue("item").jsonObject.getValue("machineIds").jsonArray.contains(JsonPrimitive("pc-1251")))
        }
        val scope = ok(KnowledgeMcp.GET, """{"machineId":"pc-1251","id":"pc1251.execution"}""").getValue("data").jsonObject
        assertEquals(JsonPrimitive(true), scope["boundedExecutionSupported"])
        assertEquals(JsonPrimitive(true), scope["displayCaptureSupported"])
        val schemas = KnowledgeMcp.tools().map { it.getValue("inputSchema").jsonObject.getValue("properties").jsonObject.getValue("machineId").jsonObject.getValue("enum").jsonArray }
        assertTrue(schemas.all { it == JsonArray(PocketKnowledgeService.MACHINES.map(::JsonPrimitive)) })
    }

    @Test fun pc1251ExampleIsBundledAndCannotBeReadAsPc1245() {
        val result = ok(KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1251","exampleId":"pc1251-numeric-labels"}""")
        val files = result.getValue("files").jsonArray.associate { it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content }
        assertEquals(setOf("pgp-project.json", "src/main.bas"), files.keys)
        assertEquals("pc-1251", Json.parseToJsonElement(files.getValue("pgp-project.json")).jsonObject.getValue("machineId").jsonPrimitive.content)
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }.first { File(it, "test-data").isDirectory }
        for ((path, text) in files) assertEquals(File(root, "test-data/basic/ai/pc1251-numeric-labels/$path").readText(), text)
        assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
            com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), com.digihori.pgp.core.rom.MachineId("pc-1251")))
        assertEquals("emulatorObservedExample", result.getValue("item").jsonObject.getValue("verification").jsonObject.getValue("basis").jsonPrimitive.content)
        assertError("EXAMPLE_NOT_FOUND", KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1245","exampleId":"pc1251-numeric-labels"}""")
    }

    private fun assertError(code: String, name: String, args: String) {
        val result = call(name, args)
        assertEquals(JsonPrimitive(true), result["isError"])
        assertEquals(code, result.getValue("structuredContent").jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content)
    }
}
