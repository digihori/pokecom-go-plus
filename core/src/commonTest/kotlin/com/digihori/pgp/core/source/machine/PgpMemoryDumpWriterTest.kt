package com.digihori.pgp.core.source.machine

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PgpMemoryDumpWriterTest {
    @Test
    fun writesSixteenBytesPerLineAndRoundTrips() {
        val bytes = ByteArray(18) { it.toByte() }
        val text = PgpMemoryDumpWriter.write(0x9000, bytes)

        assertEquals(
            "9000 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n9010 10 11\n",
            text,
        )
        val parsed = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(text)).image
        assertContentEquals(bytes, parsed.segments.flatMap { it.copyBytes().asIterable() }.toByteArray())
    }
}
