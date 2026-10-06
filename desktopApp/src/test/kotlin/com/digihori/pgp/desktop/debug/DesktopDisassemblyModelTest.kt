package com.digihori.pgp.desktop.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDisassemblyModelTest {
    @Test
    fun advancesByDecodedInstructionLength() {
        val memory = mapOf(
            0xc000 to 0x10,
            0xc001 to 0xc1,
            0xc002 to 0x00,
            0xc003 to 0xf1,
            0xc004 to 0xe0,
            0xc005 to 0x37,
        )

        val lines = DesktopDisassemblyModel.build(0xc000, 3) { memory[it] ?: 0xff }

        assertEquals(listOf(0xc000, 0xc003, 0xc005), lines.map { it.instruction.address })
        assertEquals(listOf("10 C1 00", "F1 E0", "37"), lines.map { it.byteText })
        assertEquals(listOf("LIDP 0xC100", "CAL 0x11E0", "RTN"), lines.map { it.instructionText })
    }

    @Test
    fun wrapsAtTheEndOfTheAddressSpace() {
        val lines = DesktopDisassemblyModel.build(0xffff, 2) { address ->
            when (address) {
                0xffff -> 0x00
                0x0000 -> 0x12
                0x0001 -> 0x37
                else -> 0xff
            }
        }

        assertEquals(listOf(0xffff, 0x0001), lines.map { it.instruction.address })
    }
}
