package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopBasicLoaderTest {
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
