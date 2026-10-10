package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopBasicLoaderTest {
    @Test
    fun compilesAndDecodesPc1360S2ProgramImages() {
        val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram("10 PRINT \"HELLO\"\n20 GOTO 10".encodeToByteArray(), Pc1360RomDefinition.MACHINE_ID),
        )
        val decoded = assertIs<DesktopBasicProgramDecodeResult.Success>(
            DesktopBasicLoader.detokenizeProgram(compiled.bytes, Pc1360RomDefinition.MACHINE_ID),
        )
        assertEquals("10:PRINT\"HELLO\"\n20:GOTO 10\n", decoded.utf8Bytes.decodeToString())
    }

    @Test
    fun compilesAndDecodesPc1245ProgramImages() {
        val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compilePc1245Program("10 PRINT \"HELLO\"".encodeToByteArray()),
        )
        val decoded = assertIs<DesktopBasicProgramDecodeResult.Success>(
            DesktopBasicLoader.detokenizePc1245Program(compiled.bytes),
        )

        assertEquals("10 PRINT \"HELLO\"\n", decoded.utf8Bytes.decodeToString())
    }

    @Test
    fun ignoresStudioSourceHeaderWhenCompilingBasic() {
        val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compilePc1245Program(
                "# Demonstration\n# Start with RUN\n10 PRINT \"HELLO\"".encodeToByteArray(),
            ),
        )
        val decoded = assertIs<DesktopBasicProgramDecodeResult.Success>(
            DesktopBasicLoader.detokenizePc1245Program(compiled.bytes),
        )

        assertEquals("10 PRINT \"HELLO\"\n", decoded.utf8Bytes.decodeToString())
    }

    @Test
    fun reportsDirectProgramCompileErrors() {
        assertIs<DesktopBasicProgramCompileError.InvalidUtf8>(
            assertIs<DesktopBasicProgramCompileResult.Failure>(
                DesktopBasicLoader.compilePc1245Program(byteArrayOf(0xc3.toByte(), 0x28)),
            ).error,
        )
        assertIs<DesktopBasicProgramCompileError.Tokenize>(
            assertIs<DesktopBasicProgramCompileResult.Failure>(
                DesktopBasicLoader.compilePc1245Program("10 PRINT _".encodeToByteArray()),
            ).error,
        )
    }

    @Test
    fun compilesUtf8BasicTextForPc1245RomInput() {
        val result = DesktopBasicLoader.compilePc1245RomInput(
            "10 PRINT \\SQR\n".encodeToByteArray(),
        )
        val success = assertIs<DesktopBasicLoadResult.Success>(result)

        assertEquals(PocketKey.ENTER, success.keys.last())
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.DOT),
            success.keys.subList(success.keys.size - 3, success.keys.size - 1),
        )
    }

    @Test
    fun rejectsMalformedUtf8() {
        val result = DesktopBasicLoader.compilePc1245RomInput(byteArrayOf(0xc3.toByte(), 0x28))

        assertEquals(
            DesktopBasicLoadResult.Failure(DesktopBasicLoadError.InvalidUtf8),
            result,
        )
    }

    @Test
    fun preservesParserAndRomInputErrors() {
        val parseFailure = assertIs<DesktopBasicLoadResult.Failure>(
            DesktopBasicLoader.compilePc1245RomInput("10 \\UNKNOWN".encodeToByteArray()),
        )
        assertIs<DesktopBasicLoadError.Parse>(parseFailure.error)

        val inputFailure = assertIs<DesktopBasicLoadResult.Failure>(
            DesktopBasicLoader.compilePc1245RomInput("10 \\xFC".encodeToByteArray()),
        )
        val unsupported = assertIs<DesktopBasicLoadError.UnsupportedRomInput>(inputFailure.error)
        assertEquals(Pc1245RomInputUnsupported.RawByte(0xfc), unsupported.error.unsupported)
    }
}
