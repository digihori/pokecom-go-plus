package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.DisplaySymbol

internal class Pc14xxDisplay(private val model: Pc14xxModel) {
    private val registers = ByteArray(0x300)
    var enabled: Boolean = true
        private set
    var revision: Long = 0
        private set
    fun writeMemory(address: Int, value: Int) {
        val base = when (model) { Pc14xxModel.PC_1450 -> 0x7000; Pc14xxModel.PC_1460 -> 0x3000; Pc14xxModel.PC_1470U -> 0x2800 }
        if (address !in base until base + registers.size) return
        val index = address - base
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
        val width = model.characterColumns * 5
        val dots = ByteArray(width * 7 * model.characterRows)
        repeat(model.characterRows) { characterRow ->
            repeat(width) { column ->
                val index = when (model) {
                    Pc14xxModel.PC_1450 -> if (column < 60) column else 0x7b - (column - 60)
                    Pc14xxModel.PC_1460 -> when (column) {
                        in 0..29 -> column
                        in 30..44 -> 0x2d + column - 30
                        in 45..59 -> 0x1e + column - 45
                        in 60..74 -> 0x6c - (column - 60)
                        in 75..89 -> 0x7b - (column - 75)
                        else -> 0x5d - (column - 90)
                    }
                    Pc14xxModel.PC_1470U -> characterRow * 0x40 +
                        if (column < 60) column else 0x200 + column - 60
                }
                repeat(7) { row ->
                    if (registers[index].toInt() and (1 shl row) != 0)
                        dots[(characterRow * 7 + row) * width + column] = 1
                }
            }
        }
        val symbols = buildList {
            fun addBits(index: Int, vararg symbols: DisplaySymbol) {
                symbols.forEachIndexed { bit, symbol -> if (registers[index].toInt() and (1 shl bit) != 0) add(symbol) }
            }
            if (model == Pc14xxModel.PC_1470U) {
                addBits(0x3c, DisplaySymbol.BATTERY, DisplaySymbol.PARENTHESES, DisplaySymbol.HYP,
                    DisplaySymbol.AER, DisplaySymbol.PRO, DisplaySymbol.RUN, DisplaySymbol.CAL)
                addBits(0x3d, DisplaySymbol.BUSY, DisplaySymbol.DEF, DisplaySymbol.SHIFT, DisplaySymbol.DBL)
                addBits(0x7c, DisplaySymbol.ERROR, DisplaySymbol.MEMORY, DisplaySymbol.RAD, DisplaySymbol.G,
                    DisplaySymbol.MATRIX, DisplaySymbol.STAT, DisplaySymbol.PRINT)
                addBits(0x7d, DisplaySymbol.KANA, DisplaySymbol.KANA_SMALL, DisplaySymbol.SMALL, DisplaySymbol.DE)
            } else {
                addBits(0x3c, DisplaySymbol.KANA, DisplaySymbol.KANA_SMALL, DisplaySymbol.SMALL, DisplaySymbol.STAT)
                if (model == Pc14xxModel.PC_1460 && registers[0x3c].toInt() and 16 != 0) add(DisplaySymbol.MATRIX)
                addBits(0x3d, DisplaySymbol.BUSY, DisplaySymbol.DEF, DisplaySymbol.SHIFT, DisplaySymbol.HYP,
                    DisplaySymbol.PRO, DisplaySymbol.RUN, DisplaySymbol.CAL)
                addBits(0x7c, DisplaySymbol.ERROR, DisplaySymbol.MEMORY, DisplaySymbol.PARENTHESES,
                    DisplaySymbol.RAD, DisplaySymbol.G, DisplaySymbol.DE, DisplaySymbol.PRINT)
            }
        }
        return DisplaySnapshot(model.characterColumns, 5, 7 * model.characterRows, model.characterRows, symbols, enabled, revision, dots)
    }
}
