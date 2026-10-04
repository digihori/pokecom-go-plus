package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.source.machine.PgpMemoryDumpError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopMemoryDumpLoaderTest {
    @Test
    fun decodesAndParsesUtf8Dump() {
        val result = assertIs<DesktopMemoryDumpLoadResult.Success>(
            DesktopMemoryDumpLoader.parse("9000:00010203".encodeToByteArray()),
        )

        assertEquals(4, result.image.byteCount)
        assertContentEquals(byteArrayOf(0, 1, 2, 3), result.image.segments.single().copyBytes())
    }

    @Test
    fun reportsEncodingAndFormatErrors() {
        assertIs<DesktopMemoryDumpLoadError.InvalidUtf8>(
            assertIs<DesktopMemoryDumpLoadResult.Failure>(
                DesktopMemoryDumpLoader.parse(byteArrayOf(0xc3.toByte(), 0x28)),
            ).error,
        )
        val format = assertIs<DesktopMemoryDumpLoadError.Parse>(
            assertIs<DesktopMemoryDumpLoadResult.Failure>(
                DesktopMemoryDumpLoader.parse("9000 001".encodeToByteArray()),
            ).error,
        )
        assertIs<PgpMemoryDumpError.Syntax>(format.error)
    }
}
