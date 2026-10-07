package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1360KeyboardTest {
    @Test
    fun memorySelectorUsesOneBasedGroupNumber() {
        val keyboard = Pc1360Keyboard()
        keyboard.press(PocketKey.Q)
        assertEquals(0, keyboard.readInputA(0, 1))
        assertEquals(0x40, keyboard.readInputA(0, 2))
    }

    @Test
    fun iaSelectsUpperFiveGroups() {
        val keyboard = Pc1360Keyboard()
        keyboard.press(PocketKey.ENTER)
        assertEquals(0x10, keyboard.readInputA(0x04, 0))
    }

    @Test
    fun smallAndKanaAreIndependentKeys() {
        val keyboard = Pc1360Keyboard()

        keyboard.press(PocketKey.SMALL)
        assertEquals(0x10, keyboard.readInputA(0, 1))
        assertEquals(0, keyboard.readInputA(0, 6))
        keyboard.release(PocketKey.SMALL)

        keyboard.press(PocketKey.KANA)
        assertEquals(0, keyboard.readInputA(0, 1))
        assertEquals(0x02, keyboard.readInputA(0, 6))
    }
}
