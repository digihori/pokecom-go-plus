package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Keyboard

/** PC-1251 mode contacts around the shared OLD-family key matrix. */
internal class Pc1251Keyboard {
    private val matrix = Pc1245Keyboard()

    val operatingMode: OperatingMode get() = matrix.operatingMode
    val modeRevision: Long get() = matrix.modeRevision

    fun press(key: PocketKey): Boolean = matrix.press(key)
    fun release(key: PocketKey): Boolean = matrix.release(key)
    fun setOperatingMode(mode: OperatingMode) = matrix.setOperatingMode(mode)
    fun readInputA(ia: Int, ib: Int): Int = matrix.readInputA(ia, ib)
    fun consumeKeyOnSignal(): Boolean = matrix.consumeKeyOnSignal()
    fun reset() = matrix.reset()

    fun readInputB(ib: Int): Int {
        var result = 0
        when {
            ib and 0x08 != 0 -> when (operatingMode) {
                OperatingMode.PROGRAM -> result = result or 0x02
                OperatingMode.RESERVE -> result = result or 0x01
                OperatingMode.RUN -> Unit
            }
            ib and 0x01 != 0 -> if (operatingMode == OperatingMode.RESERVE) result = result or 0x08
            ib and 0x02 != 0 -> if (operatingMode == OperatingMode.PROGRAM) result = result or 0x08
        }
        return result and ib.inv() and 0xff
    }
}
