package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.OperatingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1251KeyboardTest {
    @Test
    fun exposesProgramModeThroughPc1251Contacts() {
        val keyboard = Pc1251Keyboard()
        keyboard.setOperatingMode(OperatingMode.PROGRAM)

        assertEquals(0x02, keyboard.readInputB(0x08))
        assertEquals(0x08, keyboard.readInputB(0x02))
        assertEquals(0x00, keyboard.readInputB(0x01))
    }

    @Test
    fun exposesReserveModeThroughPc1251Contacts() {
        val keyboard = Pc1251Keyboard()
        keyboard.setOperatingMode(OperatingMode.RESERVE)

        assertEquals(0x01, keyboard.readInputB(0x08))
        assertEquals(0x08, keyboard.readInputB(0x01))
        assertEquals(0x00, keyboard.readInputB(0x02))
    }
}
