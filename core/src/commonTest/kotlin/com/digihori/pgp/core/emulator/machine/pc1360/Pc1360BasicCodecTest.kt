package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1360BasicCodecTest {
    @Test
    fun encodesS2TokensLineReferencesAndHalfWidthKana() {
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse("10 PRINT \"ｱ\"\n20 GOTO 10"),
        ).document
        val bytes = assertIs<Pc1360BasicTokenizeResult.Success>(Pc1360BasicCodec.tokenize(document)).bytes
        assertContentEquals(
            byteArrayOf(
                0xff.toByte(), 0x00, 0x0a, 0x06, 0xfe.toByte(), 0x60, 0x22, 0xb1.toByte(), 0x22, 0x0d,
                0x00, 0x14, 0x06, 0xfe.toByte(), 0x2b, 0x1f, 0x00, 0x0a, 0x0d, 0xff.toByte(),
            ),
            bytes,
        )
    }

    @Test
    fun roundTripsCanonicalS2Source() {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse("10 PRINT \"HELLO\"\n20 GOTO 10")).document
        val bytes = assertIs<Pc1360BasicTokenizeResult.Success>(Pc1360BasicCodec.tokenize(document)).bytes
        val source = assertIs<Pc1360BasicDetokenizeResult.Success>(Pc1360BasicCodec.detokenize(bytes)).source
        assertEquals("10:PRINT\"HELLO\"\n20:GOTO 10", source)
    }

    @Test
    fun loadsAndExtractsProgramAtPc1360Pointers() {
        val memory = ByteArray(0x10000)
        val program = byteArrayOf(0xff.toByte(), 0xff.toByte())
        val loaded = assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.Success>(
            Pc1360BasicProgramMemory.load(program, { memory[it].toInt() and 0xff }, { a, v -> memory[a] = v.toByte() }),
        )
        assertEquals(0x8030, loaded.start)
        assertEquals(0x30, memory[0xffd7].toInt() and 0xff)
        assertEquals(0x80, memory[0xffd8].toInt() and 0xff)
        assertContentEquals(program, assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.Success>(
            Pc1360BasicProgramMemory.extract { memory[it].toInt() and 0xff },
        ).bytes)
    }
}
