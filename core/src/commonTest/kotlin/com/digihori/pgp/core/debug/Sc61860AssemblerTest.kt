package com.digihori.pgp.core.debug

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Sc61860AssemblerTest {
    @Test
    fun reassemblesDisassemblerOutputIncludingEmbeddedOpcodes() {
        val original = byteArrayOf(0x00, 0x12, 0x10, 0xc0.toByte(), 0x30, 0x9f.toByte(), 0xf1.toByte(), 0xe0.toByte())
        val source = Sc61860Disassembler.renderAssembly(0xc000, 0xc007) { address ->
            original[address - 0xc000].toInt()
        }

        val result = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source))

        assertEquals(0xc000, result.image.segments.single().startAddress)
        assertContentEquals(original, result.image.segments.single().copyBytes())
    }

    @Test
    fun resolvesLabelsForAbsoluteAndRelativeInstructions() {
        val result = assertIs<Sc61860AssemblyResult.Success>(
            Sc61860Assembler.assemble(
                """ORG 0xC000
                    |START: LII 1
                    |       JRP DONE
                    |       DB 0xAA, &BB, 204
                    |DONE:  CALL START
                """.trimMargin(),
            ),
        )

        assertContentEquals(
            byteArrayOf(0x00, 0x01, 0x2c, 0x04, 0xaa.toByte(), 0xbb.toByte(), 0xcc.toByte(), 0x78, 0xc0.toByte(), 0x00),
            result.image.segments.single().copyBytes(),
        )
        assertEquals(listOf(Sc61860AssemblySymbol("START", 0xc000, 2), Sc61860AssemblySymbol("DONE", 0xc007, 5)), result.symbols)
        assertEquals(listOf(2, 3, 4, 5), result.listing.map { it.line })
        assertEquals(listOf(0xc000, 0xc002, 0xc004, 0xc007), result.listing.map { it.address })
    }

    @Test
    fun reportsOutOfRangeRelativeTargetWithSourceLine() {
        val result = assertIs<Sc61860AssemblyResult.Failure>(
            Sc61860Assembler.assemble("ORG 0x1000\nJRP 0x2000\n"),
        )

        assertEquals(2, result.line)
        assertEquals("Forward target is out of range", result.message)
    }
}
