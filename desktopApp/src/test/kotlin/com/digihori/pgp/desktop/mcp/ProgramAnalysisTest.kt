package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpWriter
import kotlinx.serialization.json.*
import kotlin.test.*

class ProgramAnalysisTest {
    private val service = ProgramAnalysisService()
    private val adapter = PgpMcpToolAdapter({ error("Analysis must not read an emulator session") }, analysisProvider = { service })
    private fun request(section: String, program: AnalysisProgram? = null, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject = buildJsonObject {
        put("section", section)
        program?.let { put("targetId", it.id); put("revision", it.revision) }
        extra()
    }
    private fun call(args: JsonObject): JsonObject = adapter.call(ProgramAnalysisMcp.GET, args)
    private fun ok(args: JsonObject): JsonObject {
        val result = call(args)
        assertEquals(JsonPrimitive(false), result["isError"], result.toString())
        val data = result.getValue("structuredContent").jsonObject
        assertEquals(data, Json.parseToJsonElement(result.getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content))
        return data
    }
    private fun error(code: String, args: JsonObject) {
        val result = call(args)
        assertEquals(JsonPrimitive(true), result["isError"])
        assertEquals(code, result.getValue("structuredContent").jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content)
    }
    private fun prepare(source: String) = service.prepare("/private/user/main.bas", source.encodeToByteArray(), AnalysisProgramKind.BASIC,
        "pc-1245", null, null, "Calls external code; purpose unknown")

    @Test fun selectionIsPrivateUntilPublishedAndSnapshotDoesNotRereadOriginal() {
        val bytes = "\uFEFF10 CALL 50432\r\n20 END\r\n".encodeToByteArray()
        val program = service.prepare("/private/user/main.bas", bytes, AnalysisProgramKind.BASIC, "pc-1245", null, null, "")
        assertEquals(JsonPrimitive(false), ok(request("metadata"))["available"])
        error("NO_ANALYSIS_TARGET", request("source", program))
        bytes.fill(0)
        service.publish(program)
        val target = ok(request("metadata")).getValue("target").jsonObject
        assertEquals("main.bas", target.getValue("name").jsonPrimitive.content)
        assertEquals(JsonPrimitive(false), target["importLoadsEmulator"])
        assertEquals(JsonPrimitive("notTracked"), target["runtimeCorrespondence"])
        assertFalse(target.containsKey("loadedIntoEmulator"))
        assertFalse(target.toString().contains("/private/"))
        val page = ok(request("source", program) { put("limit", 1) })
        assertEquals("10 CALL 50432", page.getValue("lines").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals(1, page.getValue("nextOffset").jsonPrimitive.int)
        val next = ok(request("source", program) { put("offset", 1) })
        assertEquals("20 END", next.getValue("lines").jsonArray.first().jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals(2, next.getValue("lines").jsonArray.first().jsonObject.getValue("line").jsonPrimitive.int)
        service.clear()
        error("NO_ANALYSIS_TARGET", request("source", program))
    }

    @Test fun targetReplacementRevokesOldReadsEvenIfFileContentsMatch() {
        val first = prepare("10 END")
        service.publish(first)
        val second = prepare("10 END")
        assertEquals(first.revision, second.revision)
        assertNotEquals(first.id, second.id)
        service.publish(second)
        error("STALE_ANALYSIS_TARGET", request("source", first))
        error("STALE_ANALYSIS_TARGET", request("source", second) { put("revision", "obsolete") })
        ok(request("source", second))
    }

    @Test fun dmpAnalysisUsesImportedBytesAcrossGapsAndPreservesUnknownEntry() {
        val result = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble("""
            ORG 0xC000
            START: LIDP 0xC100
                   LIA 0x41
                   STD
                   CALL SUB
                   JP START
            ORG 0xC020
            SUB:   LIA 0x42
                   STD
                   RTN
        """.trimIndent()))
        val program = service.prepare("fixture.dmp", DesktopMemoryDumpWriter.write(result.image), AnalysisProgramKind.DMP,
            "pc-1245", null, null, "")
        service.publish(program)
        val data = ok(request("disassembly", program) { put("instructionCount", 128) })
        assertEquals("firstSegment; entryUnknown", data.getValue("entryBasis").jsonPrimitive.content)
        val rows = data.getValue("instructions").jsonArray.map { it.jsonObject }
        assertEquals(8, rows.size)
        assertEquals("LIDP 0xC100", rows.first().getValue("text").jsonPrimitive.content)
        assertEquals(JsonNull, rows.first()["controlFlowTarget"])
        assertEquals(0xc020, rows.single { it.getValue("text").jsonPrimitive.content == "CALL 0xC020" }.getValue("controlFlowTarget").jsonPrimitive.int)
        assertTrue(rows.any { it.getValue("address").jsonPrimitive.int == 0xc020 })
        assertEquals(JsonNull, data["nextAddress"])
        error("ADDRESS_OUTSIDE_IMAGE", request("disassembly", program) { put("startAddress", 0xc010) })
        error("ANALYSIS_HAS_NO_SOURCE", request("source", program))
    }

    @Test fun rawBinaryIsCopiedAndTruncatedInstructionDoesNotReadOutsideImage() {
        val bytes = byteArrayOf(0x02, 0x41, 0x78) // LIA 41 followed by incomplete CALL
        val program = service.prepare("code.bin", bytes, AnalysisProgramKind.RAW, "pc-1245", 0xc000, 0xc000, "")
        bytes.fill(0)
        service.publish(program)
        val first = ok(request("disassembly", program) { put("instructionCount", 1) })
        assertEquals(0xc002, first.getValue("nextAddress").jsonPrimitive.int)
        assertEquals("LIA 0x41", first.getValue("instructions").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
        val last = ok(request("disassembly", program) { put("startAddress", 0xc002) })
        val row = last.getValue("instructions").jsonArray.single().jsonObject
        assertEquals("DB 0x78", row.getValue("text").jsonPrimitive.content)
        assertEquals(JsonPrimitive(false), row["completeInstruction"])
        assertEquals(JsonNull, last["nextAddress"])
    }

    @Test fun invalidInputCannotReplacePublishedSnapshotOrExposeRom() {
        val original = prepare("10 END"); service.publish(original)
        fun invalid(code: String, block: () -> Unit) {
            assertEquals(code, assertFailsWith<DevelopmentReadError> { block() }.code)
            assertEquals(original.id, ok(request("metadata")).getValue("target").jsonObject.getValue("targetId").jsonPrimitive.content)
        }
        invalid("INVALID_UTF8") { service.prepare("bad.bas", byteArrayOf(0xff.toByte()), AnalysisProgramKind.BASIC, "pc-1245", null, null, "") }
        invalid("INVALID_DMP") { service.prepare("bad.dmp", "garbage".encodeToByteArray(), AnalysisProgramKind.DMP, "pc-1245", null, null, "") }
        invalid("ANALYSIS_INPUT_SIZE") { service.prepare("huge.bas", ByteArray(65537), AnalysisProgramKind.BASIC, "pc-1245", null, null, "") }
        for (address in listOf(0x0000, 0x4000, 0xf800, 0xc7ff, Int.MAX_VALUE)) {
            invalid("ANALYSIS_RAM_ONLY") { service.prepare("rom.bin", byteArrayOf(2, 3), AnalysisProgramKind.RAW, "pc-1245", address, null, "") }
        }
        invalid("LOAD_ADDRESS_REQUIRED") { service.prepare("raw.bin", byteArrayOf(1), AnalysisProgramKind.RAW, "pc-1245", null, null, "") }
        invalid("ENTRY_OUTSIDE_IMAGE") { service.prepare("raw.bin", byteArrayOf(1), AnalysisProgramKind.RAW, "pc-1245", 0xc000, 0xc100, "") }
        for (kind in listOf(AnalysisProgramKind.RAW, AnalysisProgramKind.DMP, AnalysisProgramKind.ASSEMBLY)) {
            invalid("ANALYSIS_PC1251_BASIC_ONLY") {
                service.prepare("other.bas", byteArrayOf(1), kind, "pc-1251", null, null, "")
            }
        }
        invalid("ANALYSIS_PC1251_BASIC_ONLY") {
            service.prepare("other.txt", "10 END".encodeToByteArray(), AnalysisProgramKind.BASIC, "pc-1251", null, null, "")
        }
        invalid("UNSUPPORTED_ANALYSIS_MACHINE") { service.prepare("other.bas", "10 END".encodeToByteArray(), AnalysisProgramKind.BASIC, "pc-1350", null, null, "") }
    }

    @Test fun strictReadSchemaAndBoundedPages() {
        val original = prepare((1..100).joinToString("\n") { "$it REM " + "X".repeat(1000) }.take(60000))
        service.publish(original)
        val page = ok(request("source", original) { put("limit", 128) })
        assertTrue(page.getValue("lines").jsonArray.size in 1..20)
        assertNotEquals(JsonNull, page["nextOffset"])
        error("INVALID_ARGUMENTS", request("metadata") { put("path", "/etc/passwd") })
        error("INVALID_ARGUMENTS", request("source", original) { put("offset", "0") })
        error("INVALID_ARGUMENTS", request("source", original) { put("limit", 129) })
        error("INVALID_ARGUMENTS", request("source", original) { put("limit", JsonNull) })
        error("INVALID_ARGUMENTS", request("disassembly", original) { put("startAddress", "0xC000") })
        error("ANALYSIS_HAS_NO_IMAGE", request("disassembly", original))
        error("INVALID_ARGUMENTS", request("source"))
    }
}
