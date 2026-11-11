package com.digihori.pgp.player.android

import kotlin.test.*

class SaveAddressTest {
    @Test fun acceptsOnlySixteenBitHexAddresses() {
        assertEquals(0, parseSaveAddress("0000"))
        assertEquals(65535, parseSaveAddress(" fFfF "))
        assertEquals(0xc500, parseSaveAddress("C500"))
        for (text in listOf("", "10000", "-1", "0xFF", "GG", "1 2")) assertNull(parseSaveAddress(text))
    }
}
