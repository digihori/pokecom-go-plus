package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.emulator.cpu.Sc61860Io

/** Connects SC61860 port signals to the PC-1245 peripherals. */
internal class Pc1245Io(
    private val keyboard: Pc1245Keyboard,
    private val display: Pc1245Display,
    private val buzzer: Pc1245Buzzer,
) : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int): Int = keyboard.readInputA(ia, ib)

    override fun readInputB(ib: Int): Int = keyboard.readInputB(ib)

    override fun consumeKeyOnSignal(): Boolean = keyboard.consumeKeyOnSignal()

    override fun writeOutputControl(value: Int) {
        display.setEnabled(value and DISPLAY_ENABLE_BIT != 0)
        buzzer.writeControl(value)
    }

    private companion object {
        const val DISPLAY_ENABLE_BIT: Int = 0x01
    }
}
