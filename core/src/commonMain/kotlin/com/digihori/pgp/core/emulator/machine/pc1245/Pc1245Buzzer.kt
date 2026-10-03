package com.digihori.pgp.core.emulator.machine.pc1245

/** Logical tone requested by the PC-1245 control port; it does not play host audio. */
internal class Pc1245Buzzer {
    var frequencyHz: Int = SILENT_FREQUENCY_HZ
        private set

    var revision: Long = 0
        private set

    fun writeControl(value: Int) {
        val requestedFrequency = when (value and TONE_SELECT_MASK) {
            0x00 -> SILENT_FREQUENCY_HZ
            0x20 -> TONE_2_KHZ
            0x30 -> TONE_4_KHZ
            else -> return
        }
        if (frequencyHz != requestedFrequency) {
            frequencyHz = requestedFrequency
            revision++
        }
    }

    fun reset() {
        if (frequencyHz != SILENT_FREQUENCY_HZ) {
            frequencyHz = SILENT_FREQUENCY_HZ
            revision++
        }
    }

    private companion object {
        const val TONE_SELECT_MASK: Int = 0x30
        const val SILENT_FREQUENCY_HZ: Int = 0
        const val TONE_2_KHZ: Int = 2_000
        const val TONE_4_KHZ: Int = 4_000
    }
}
