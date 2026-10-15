package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Pc1245BuzzerTest {
    @Test
    fun controlModesExposeOnlyTheBuiltInOscillatorFrequency() {
        val buzzer = Pc1245Buzzer()

        buzzer.writeControl(0x20)
        assertEquals(2_000, buzzer.frequencyHz)
        buzzer.writeControl(0x30)
        assertEquals(4_000, buzzer.frequencyHz)
        buzzer.writeControl(0x50)
        assertEquals(0, buzzer.frequencyHz)
        buzzer.writeControl(0x40)
        assertEquals(0, buzzer.frequencyHz)
    }

    @Test
    fun rendersTheBuiltInOscillatorAsPcm() {
        val buzzer = Pc1245Buzzer()
        buzzer.writeControl(0x20)
        buzzer.advanceCycles(1_920)

        val pcm = buzzer.drainPcm()

        assertTrue(pcm.size in 219..221)
        assertTrue(pcm.any { it > 0 })
        assertTrue(pcm.any { it < 0 })
        assertEquals(0, buzzer.drainPcm().size)
    }

    @Test
    fun directHighLowTimingProducesPcmWithoutADeclaredFrequency() {
        val buzzer = Pc1245Buzzer()
        repeat(10) {
            buzzer.writeControl(0x50)
            buzzer.advanceCycles(96)
            buzzer.writeControl(0x40)
            buzzer.advanceCycles(96)
        }

        val pcm = buzzer.drainPcm()

        assertEquals(0, buzzer.frequencyHz)
        assertTrue(pcm.any { it > 0 })
        assertTrue(pcm.any { it < 0 })
    }

    @Test
    fun silentIntervalsRetainTheirExactFrameCount() {
        val buzzer = Pc1245Buzzer()
        buzzer.writeControl(0x20)
        buzzer.advanceCycles(1920)
        assertEquals(220, buzzer.drainPcm().size)
        assertEquals(220, buzzer.lastDrainFrameCount)
        buzzer.writeControl(0)
        buzzer.advanceCycles(19200)
        buzzer.drainPcm() // Allow the high-pass tail to settle.
        buzzer.advanceCycles(1920)
        assertEquals(0, buzzer.drainPcm().size)
        assertTrue(buzzer.lastDrainFrameCount in 220..221)
        assertEquals(0, buzzer.drainPcm().size)
        assertEquals(0, buzzer.lastDrainFrameCount)
        buzzer.writeControl(0x30)
        buzzer.advanceCycles(1920)
        assertTrue(buzzer.drainPcm().size in 220..221)
        buzzer.reset()
        assertEquals(0, buzzer.lastDrainFrameCount)
    }

    @Test
    fun resetClearsPendingAudioAndStopsAnOscillator() {
        val buzzer = Pc1245Buzzer()
        buzzer.writeControl(0x30)
        buzzer.advanceCycles(1_000)

        buzzer.reset()

        assertEquals(0, buzzer.frequencyHz)
        assertEquals(0, buzzer.drainPcm().size)
    }
}
