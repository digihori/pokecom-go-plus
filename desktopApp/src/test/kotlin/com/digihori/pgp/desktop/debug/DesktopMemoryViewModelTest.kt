package com.digihori.pgp.desktop.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopMemoryViewModelTest {
    @Test
    fun formatsSixteenBytesAsHexAndAscii() {
        val line = DesktopMemoryViewModel.build(0xc000, 1) { address ->
            when (address) {
                0xc000 -> 'A'.code
                0xc001 -> ' '.code
                0xc002 -> 0x7e
                else -> 0
            }
        }.single()

        assertEquals(0xc000, line.address)
        assertEquals("41 20 7E 00", line.hexText.take(11))
        assertEquals("A ~.............", line.asciiText)
    }

    @Test
    fun wrapsReadsAcrossEndOfAddressSpace() {
        val line = DesktopMemoryViewModel.build(0xfff8, 1) { it and 0xff }.single()

        assertEquals(
            listOf(0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff, 0x00, 0x01),
            line.bytes.take(10),
        )
    }
}
