package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.DisplaySymbol

internal class Pc1401Display {
    private val registers = ByteArray(0x100)
    var enabled: Boolean = true
        private set
    var revision: Long = 0
        private set
    fun writeMemory(address: Int, value: Int) {
        val index = address and 0xff
        if (registers[index] != value.toByte()) { registers[index] = value.toByte(); revision++ }
    }
    fun setEnabled(value: Boolean) {
        if (enabled != value) { enabled = value; revision++ }
    }
    fun reset() {
        if (registers.any { it != 0.toByte() } || !enabled) revision++
        registers.fill(0); enabled = true
    }
    fun snapshot(): DisplaySnapshot {
        val dots = ByteArray(80 * 7)
        repeat(80) { column ->
            val index = if (column < 40) column else 0x67 - (column - 40)
            repeat(7) { row -> if (registers[index].toInt() and (1 shl row) != 0) dots[row * 80 + column] = 1 }
        }
        val symbols = buildList {
            if (registers[0x3c].toInt() and 8 != 0) add(DisplaySymbol.STAT)
            listOf(DisplaySymbol.BUSY, DisplaySymbol.DEF, DisplaySymbol.SHIFT, DisplaySymbol.HYP,
                DisplaySymbol.PRO, DisplaySymbol.RUN, DisplaySymbol.CAL).forEachIndexed { bit, symbol ->
                if (registers[0x3d].toInt() and (1 shl bit) != 0) add(symbol)
            }
            listOf(DisplaySymbol.ERROR, DisplaySymbol.MEMORY, DisplaySymbol.PARENTHESES,
                DisplaySymbol.RAD, DisplaySymbol.G, DisplaySymbol.DE, DisplaySymbol.PRINT).forEachIndexed { bit, symbol ->
                if (registers[0x7c].toInt() and (1 shl bit) != 0) add(symbol)
            }
        }
        return DisplaySnapshot(16, 5, 7, 1, symbols, enabled, revision, dots)
    }
}
