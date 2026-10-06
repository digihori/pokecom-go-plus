package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Pc1350KeyboardTest {
    @Test
    fun scansFirstSevenGroupsThroughMemorySelector() {
        val keyboard = Pc1350Keyboard()
        assertTrue(keyboard.press(PocketKey.A))
        assertEquals(0x20, keyboard.readInputA(ia = 0, memorySelector = 0x02))
        assertEquals(0, keyboard.readInputA(ia = 0, memorySelector = 0x01))
        assertTrue(keyboard.release(PocketKey.A))
        assertEquals(0, keyboard.readInputA(ia = 0, memorySelector = 0x02))
    }

    @Test
    fun scansLastFiveGroupsThroughIa() {
        val keyboard = Pc1350Keyboard()
        keyboard.press(PocketKey.ENTER)
        assertEquals(0x10, keyboard.readInputA(ia = 0x08, memorySelector = 0))
        assertEquals(0, keyboard.readInputA(ia = 0x04, memorySelector = 0))
    }

    @Test
    fun scansPc1350SpecificPunctuationAndControlKeys() {
        val keyboard = Pc1350Keyboard()
        keyboard.press(PocketKey.COLON)
        keyboard.press(PocketKey.MODE)
        assertEquals(0x02, keyboard.readInputA(ia = 0, memorySelector = 0x01))
        assertEquals(0x08, keyboard.readInputA(ia = 0x02, memorySelector = 0))
    }

    @Test
    fun breakProducesFiniteKeyOnPulse() {
        val keyboard = Pc1350Keyboard()
        keyboard.press(PocketKey.BREAK)
        repeat(10) { assertTrue(keyboard.consumeKeyOnSignal()) }
        assertFalse(keyboard.consumeKeyOnSignal())
    }
}
