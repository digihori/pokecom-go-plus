package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.basic.*
import com.digihori.pgp.desktop.machine.*
import com.digihori.pgp.desktop.message
import com.digihori.pgp.desktop.parseHexAddress
import kotlin.test.*

class StudioTransferStringsTest {
    @Test fun basicDumpAndAssemblyOutputsStayIdenticalAcrossUiLanguages() {
        val basicSource = "10 LET A=1.25\n20 PRINT A\n".encodeToByteArray()
        val assemblySource = "ORG 0xC100\nLII 0x12\nRTN\n"
        data class Outputs(val basic: ByteArray, val decoded: ByteArray, val dump: ByteArray, val assembled: ByteArray)
        val results = StudioLanguage.entries.map { language ->
            val strings = StudioStrings(language)
            strings.text("basic.loadTitle")
            val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(DesktopBasicLoader.compileProgram(basicSource, MachineId("pc-1245")))
            val decoded = assertIs<DesktopBasicProgramDecodeResult.Success>(DesktopBasicLoader.detokenizeProgram(compiled.bytes, MachineId("pc-1245")))
            val dump = assertIs<DesktopMemoryDumpLoadResult.Success>(DesktopMemoryDumpLoader.parse("C000: 00 12 37\n".encodeToByteArray()))
            val assembly = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(assemblySource))
            strings.text("quick.saved", assembly.image.byteCount.toString(), "日本語 output.dmp")
            Outputs(compiled.bytes, decoded.utf8Bytes, DesktopMemoryDumpWriter.write(dump.image), DesktopMemoryDumpWriter.write(assembly.image))
        }
        assertContentEquals(results[0].basic, results[1].basic)
        assertContentEquals(results[0].decoded, results[1].decoded)
        assertContentEquals(results[0].dump, results[1].dump)
        assertContentEquals(results[0].assembled, results[1].assembled)
        assertTrue(results[0].decoded.decodeToString().contains("1.25"))
        assertTrue(results[0].assembled.decodeToString().contains("C100"))
    }

    @Test fun rangeParsingKeepsExistingSixteenBitHexRules() {
        for ((input, expected) in listOf("0000" to 0, "ffff" to 65535, " 0xC000 " to 49152, "0XC0FF" to 49407)) {
            assertEquals(expected, parseHexAddress(input))
        }
        for (input in listOf("", "10000", "GGGG", "-1", "C0 00", "1.25")) assertNull(parseHexAddress(input))
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            assertTrue(strings.text("memory.invalidStart").contains("0000..FFFF"))
            assertTrue(strings.text("memory.saved", "256", "C000", "C0FF", "日本語 file.dmp").contains("0xC000..0xC0FF"))
        }
    }

    @Test fun inputDiagnosticsTranslateAndKeepOffendingCharactersAndPositions() {
        val japanese = StudioStrings(StudioLanguage.JAPANESE)
        val error = DesktopBasicLoadError.UnsupportedRomInput(Pc1245RomInputError(12, 34, Pc1245RomInputUnsupported.RawByte(0xAB)))
        val text = error.message("PC-1245", japanese)
        for (value in listOf("PC-1245", "12", "34", "0xAB")) assertTrue(text.contains(value), text)
        val parsed = assertIs<DesktopBasicProgramCompileResult.Failure>(DesktopBasicLoader.compileProgram("PRINT 1".encodeToByteArray(), MachineId("pc-1245")))
        assertTrue(parsed.error.message("PC-1245", japanese).contains("行番号"))
        val dump = assertIs<DesktopMemoryDumpLoadResult.Failure>(DesktopMemoryDumpLoader.parse("XYZ: 00".encodeToByteArray()))
        assertTrue(dump.error.message(japanese).contains("16進数"))
        assertEquals("未定義のシンボルです: USER_SYMBOL", japanese.transferDetail("Undefined symbol: USER_SYMBOL"))
        assertTrue(japanese.transferDetail("unknown future diagnostic {source}").endsWith("unknown future diagnostic {source}"))
    }

    @Test fun externalLogsAndUserFileNamesRemainUnmodified() {
        val log = "LINE:42 ERROR CODE:7\nLIDP 0xC000\n{source}\n/folder with spaces/input.asm"
        val filename = "日本語 {file} output.dmp"
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val text = strings.text("quick.externalSaved", "3", filename, log)
            assertTrue(text.contains(filename))
            assertTrue(text.endsWith(log))
            assertTrue(strings.text("transfer.standaloneAdditional", filename).contains(filename))
        }
    }
}
