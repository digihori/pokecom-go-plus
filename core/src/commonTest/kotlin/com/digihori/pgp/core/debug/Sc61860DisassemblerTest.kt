package com.digihori.pgp.core.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class Sc61860DisassemblerTest {
    @Test
    fun rendersAssemblerOrientedSourceWithAddressAndByteComments() {
        val memory = byteArrayOf(0x00, 0x12, 0x10, 0xc0.toByte(), 0x30)

        val source = Sc61860Disassembler.renderAssembly(0xc000, 0xc004) { address ->
            memory[address - 0xc000].toInt()
        }

        assertEquals(
            """ORG 0xC000
                |LII 0x12             ; C000: 00 12
                |LIDP 0xC030          ; C002: 10 C0 30
                |
            """.trimMargin(),
            source,
        )
    }

    @Test
    fun emitsDataByteWhenFinalInstructionWouldCrossTheSelectedRange() {
        val memory = byteArrayOf(0x10, 0xc0.toByte(), 0x30)

        val lines = Sc61860Disassembler.disassemble(0x9000, 0x9001) { address ->
            memory[address - 0x9000].toInt()
        }

        assertEquals(listOf("DB 0x10", "DB 0xC0"), lines.map { it.sourceText })
    }
}

