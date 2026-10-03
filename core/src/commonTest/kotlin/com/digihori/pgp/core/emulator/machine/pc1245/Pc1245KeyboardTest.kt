package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Pc1245KeyboardTest {
    @Test
    fun mapsKeysToColumnsAndSupportsSimultaneousPresses() {
        val keyboard = Pc1245Keyboard()

        keyboard.press(PocketKey.MINUS)
        keyboard.press(PocketKey.CLEAR)
        keyboard.press(PocketKey.NUM_7)

        assertEquals(0x03, keyboard.columnMask(0))
        assertEquals(0x02, keyboard.columnMask(3))
        assertEquals(0x03, keyboard.readInputA(ia = 0, ib = 0x01))
        assertEquals(0x02, keyboard.readInputA(ia = 0x01, ib = 0))

        keyboard.release(PocketKey.MINUS)
        assertEquals(0x02, keyboard.columnMask(0))
    }

    @Test
    fun duplicatePressAndReleaseAreIdempotent() {
        val keyboard = Pc1245Keyboard()

        keyboard.press(PocketKey.A)
        keyboard.press(PocketKey.A)
        keyboard.release(PocketKey.A)
        keyboard.release(PocketKey.A)

        assertEquals(0, keyboard.columnMask(2))
    }

    @Test
    fun exposesProgramModeThroughTheIbContacts() {
        val keyboard = Pc1245Keyboard()
        assertEquals(0, keyboard.readInputB(0x08))
        assertEquals(0, keyboard.readInputB(0x02))

        keyboard.setOperatingMode(OperatingMode.PROGRAM)

        assertEquals(0x02, keyboard.readInputB(0x08))
        assertEquals(0x08, keyboard.readInputB(0x02))
        assertEquals(1, keyboard.modeRevision)
    }

    @Test
    fun breakProducesTenKeyOnObservations() {
        val keyboard = Pc1245Keyboard()

        keyboard.press(PocketKey.BREAK)
        repeat(10) { assertTrue(keyboard.consumeKeyOnSignal()) }
        assertFalse(keyboard.consumeKeyOnSignal())

        keyboard.release(PocketKey.BREAK)
        keyboard.press(PocketKey.BREAK)
        assertTrue(keyboard.consumeKeyOnSignal())
    }

    @Test
    fun resetClearsKeysBreakAndProgramMode() {
        val keyboard = Pc1245Keyboard()
        keyboard.press(PocketKey.A)
        keyboard.press(PocketKey.BREAK)
        keyboard.setOperatingMode(OperatingMode.PROGRAM)

        keyboard.reset()

        assertEquals(0, keyboard.columnMask(2))
        assertFalse(keyboard.consumeKeyOnSignal())
        assertEquals(OperatingMode.RUN, keyboard.operatingMode)
        assertEquals(2, keyboard.modeRevision)
    }
}
