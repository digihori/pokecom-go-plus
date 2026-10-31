package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1360BasicCodecTest {
    @Test
    fun successfulReplacementInvalidatesOnlyTheSavedRunContext() {
        for (id in listOf(Pc1360RomDefinition.MACHINE_ID, Pc1360RomDefinition.PC1360K_MACHINE_ID)) {
            val rom = assertIs<com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult.Success>(
                Pc1360ComponentRomImporter.importImages(ByteArray(0x2000), List(8) { ByteArray(0x4000) }, id),
            ).romSet
            val machine = Pc1360Machine(rom)
            machine.cpuState.internalRam[0x35] = 0x19
            machine.cpuState.internalRam[0x34] = 0xa5
            assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.InvalidProgram>(
                machine.loadBasicProgram(byteArrayOf(0)),
            )
            assertEquals(0x19, machine.cpuState.internalRam[0x35])
            assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.Success>(
                machine.loadBasicProgram(byteArrayOf(-1, -1)),
            )
            assertEquals(0x09, machine.cpuState.internalRam[0x35])
            assertEquals(0xa5, machine.cpuState.internalRam[0x34])
            assertEquals(0, machine.cpuState.programCounter)
        }
    }

    @Test
    fun encodesReferenceListsJapaneseAndRemarkQuotesWithoutChangingBytes() {
        val source = "10 ON A GOSUB 0,65535:IF A=1 THEN 100\n100 PRINT \"漢字ｱ\"\n200 REM THEN 100 \""
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        val image = assertIs<Pc1360BasicTokenizeResult.Success>(Pc1360BasicCodec.tokenize(document)).bytes
        val raw = image.map { it.toInt() and 255 }
        val refs = raw.windowed(3).filter { it[0] == 0x1f }
        assertEquals(listOf(listOf(31, 0, 0), listOf(31, 255, 255), listOf(31, 0, 100)), refs)
        // CP932 漢=8ABF, 字=8E9A; half-width ｱ=B1.
        kotlin.test.assertTrue(raw.windowed(5).contains(listOf(0x8a, 0xbf, 0x8e, 0x9a, 0xb1)))
        val text = assertIs<Pc1360BasicDetokenizeResult.Success>(Pc1360BasicCodec.detokenize(image)).source
        val regenerated = assertIs<Pc1360BasicTokenizeResult.Success>(Pc1360BasicCodec.tokenize(
            assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(text)).document)).bytes
        assertContentEquals(image, regenerated)
    }

    @Test
    fun rejectsOutOfRangeReferencesUnsupportedUnicodeAndTruncatedMarkers() {
        for (source in listOf("10 GOTO 65536", "10 GOTO 999999999999999999999999", "10 PRINT \"😀\"")) {
            val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
            assertIs<Pc1360BasicTokenizeResult.Failure>(Pc1360BasicCodec.tokenize(document))
        }
        for (statement in listOf(byteArrayOf(0xfe.toByte()), byteArrayOf(0x1f, 0), byteArrayOf(0xfe.toByte(), 0xff.toByte()))) {
            val image = byteArrayOf(-1, 0, 10, (statement.size + 1).toByte()) + statement + byteArrayOf(13, -1)
            assertIs<Pc1360BasicDetokenizeResult.Failure>(Pc1360BasicCodec.detokenize(image))
        }
    }

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
