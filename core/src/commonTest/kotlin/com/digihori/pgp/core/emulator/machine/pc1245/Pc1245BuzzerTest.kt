package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1245BuzzerTest {
    @Test
    fun controlBitsSelectTwoAndFourKilohertzOrSilence() {
        val buzzer = Pc1245Buzzer()

        buzzer.writeControl(0x20)
        assertEquals(2_000, buzzer.frequencyHz)
        assertEquals(1, buzzer.revision)

        buzzer.writeControl(0x30)
        assertEquals(4_000, buzzer.frequencyHz)
        assertEquals(2, buzzer.revision)

        buzzer.writeControl(0x00)
        assertEquals(0, buzzer.frequencyHz)
        assertEquals(3, buzzer.revision)
    }

    @Test
    fun controlValueWithOnlyBitFourKeepsTheCurrentTone() {
        val buzzer = Pc1245Buzzer()
        buzzer.writeControl(0x20)

        buzzer.writeControl(0x10)

        assertEquals(2_000, buzzer.frequencyHz)
        assertEquals(1, buzzer.revision)
    }

    @Test
    fun repeatedValuesDoNotReviseAndResetStopsAnActiveTone() {
        val buzzer = Pc1245Buzzer()
        buzzer.writeControl(0x30)
        buzzer.writeControl(0x30)
        assertEquals(1, buzzer.revision)

        buzzer.reset()
        assertEquals(0, buzzer.frequencyHz)
        assertEquals(2, buzzer.revision)

        buzzer.reset()
        assertEquals(2, buzzer.revision)
    }
}
