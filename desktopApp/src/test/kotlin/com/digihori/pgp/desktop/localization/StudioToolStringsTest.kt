package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.character.CharacterEditorFormats
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.*
import com.digihori.pgp.desktop.character.DesktopCharacterEditorModel
import com.digihori.pgp.desktop.wav.*
import kotlin.test.*

class StudioToolStringsTest {
    private val en = StudioStrings(StudioLanguage.ENGLISH)
    private val ja = StudioStrings(StudioLanguage.JAPANESE)

    @Test fun displayTranslationDoesNotChangeWavProgramRawOrReport() {
        val machine = MachineId("pc-1245")
        for ((source, binary) in listOf("10 PRINT 1.25\n" to false, "C000: 01 02 FF\n" to true)) {
            val encoded = DesktopWavOperation.encode(source.encodeToByteArray(), binary, machine, "TEST", false)
            val wav = assertNotNull(encoded.wav)
            val decoded = DesktopWavOperation.decode(wav, machine)
            val report = decoded.report()
            val summary = decoded.summary
            val raw = decoded.raw!!.copyBytes()
            val program = decoded.program!!.copyOf()
            val mappings = decoded.mappings.toList()
            for (strings in listOf(en, ja)) {
                assertTrue(strings.wavSummary(summary).contains("Hz"))
                decoded.diagnostics.forEach { it.uiDescription(strings) }
                decoded.uiError(strings)
                assertEquals(report, decoded.report())
                assertEquals(summary, decoded.summary)
                assertEquals(mappings, decoded.mappings)
                assertContentEquals(raw, decoded.raw.copyBytes())
                assertContentEquals(program, decoded.program)
                val repeated = DesktopWavOperation.encode(source.encodeToByteArray(), binary, machine, "TEST", false)
                assertContentEquals(wav.copyBytes(), repeated.wav!!.copyBytes())
                assertEquals(encoded.report(), repeated.report())
            }
            assertNotEquals(en.wavSummary(summary), ja.wavSummary(summary))
            if (binary) assertTrue(ja.wavSummary(summary).contains("C000..C002"))
            else assertTrue(decoded.content.contains("1.25"))
        }
        val path = "/日本語/{file}/output.wav"
        assertTrue(ja.text("wav.saved", path).contains(path))
        assertEquals("Raw", en.text("wav.tab.Raw"))
        assertEquals("生バイト列", ja.text("wav.tab.Raw"))
    }

    @Test fun codecDiagnosticDisplayPreservesCodeDetailsAndCoordinates() {
        val diagnostic = CodecDiagnostic(CodecDiagnosticSeverity.ERROR, CodecStage.TRANSFER_DECODE,
            CodecDiagnosticCode.CHECKSUM_MISMATCH, "OLD filename checksum mismatch",
            CodecDataRange(CodecDataKind.RAW_BYTES, 8, 2), mapOf("expected" to "FF", "actual" to "12"))
        val original = diagnostic.describe()
        assertEquals(original, diagnostic.uiDescription(en))
        val translated = diagnostic.uiDescription(ja)
        for (value in listOf("CHECKSUM_MISMATCH", "8..10", "FF", "12", "チェックサム")) assertTrue(translated.contains(value))
        assertEquals(original, diagnostic.describe())
        for (stage in CodecStage.entries) for (severity in CodecDiagnosticSeverity.entries) for (kind in CodecDataKind.entries) {
            assertTrue(CodecDiagnostic(severity, stage, CodecDiagnosticCode.INVALID_ARGUMENT,
                "future diagnostic {raw}", CodecDataRange(kind, 0, 1)).uiDescription(ja).contains("future diagnostic {raw}"))
        }
        val failure = DesktopWavOperation.decode(WavByteSequence(byteArrayOf(0)), MachineId("pc-1245"))
        assertTrue(ja.wavSummary(failure.summary).contains("失敗"))
        assertTrue(failure.diagnostics.single().uiDescription(ja).contains("RIFF"))
    }

    @Test fun typedWavInputErrorsKeepOriginalReport() {
        for (result in listOf(
            DesktopWavOperation.encode(byteArrayOf(0xFF.toByte()), false, MachineId("pc-1245"), "TEST", false),
            DesktopWavOperation.encode("invalid".encodeToByteArray(), true, MachineId("pc-1245"), "TEST", false),
            DesktopWavOperation.encode("C000: 01\nC002: FF\n".encodeToByteArray(), true, MachineId("pc-1245"), "TEST", false))) {
            val before = result.report()
            assertTrue(result.uiError(ja).isNotBlank())
            assertNotEquals(result.error, result.uiError(ja))
            assertEquals(before, result.report())
        }
    }

    @Test fun characterErrorsTranslateWithoutChangingBytesOrSource() {
        val format = CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP
        val valid = DesktopCharacterEditorModel(format).withByteText("0x01, &02, 3, 4, 127").applyByteText()
        assertNull(valid.inputError)
        val outputs = listOf(valid.hexOutput, valid.basicOutput, valid.assemblerOutput, valid.ampersandHexOutput,
            valid.decimalOutput, valid.dollarHexOutput, valid.prefixedHexOutput)
        for (input in listOf("", "1,2", "1,2,3,4,invalid", "1,2,3,4,128")) {
            val failed = valid.withByteText(input).applyByteText()
            val original = assertNotNull(failed.inputError)
            assertEquals(original, en.characterInputError(original))
            assertNotEquals(original, ja.characterInputError(original))
            assertEquals(original, failed.inputError)
            assertEquals(valid.pattern, failed.pattern)
        }
        ja.text("character.format")
        assertEquals("5x7-column-lsb-top", format.id)
        assertEquals(outputs, listOf(valid.hexOutput, valid.basicOutput, valid.assemblerOutput, valid.ampersandHexOutput,
            valid.decimalOutput, valid.dollarHexOutput, valid.prefixedHexOutput))
        assertTrue(valid.basicOutput.startsWith("DATA "))
        assertTrue(valid.assemblerOutput.startsWith("DB "))
    }
}
