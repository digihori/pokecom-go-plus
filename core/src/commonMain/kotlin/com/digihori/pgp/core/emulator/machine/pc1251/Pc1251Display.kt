package com.digihori.pgp.core.emulator.machine.pc1251

internal class Pc1251Display {
    private val dotColumns = ByteArray(DOT_COLUMN_COUNT)
    private var symbol0: Int = 0
    private var symbol1: Int = 0

    var enabled: Boolean = true
        private set
    var revision: Long = 0
        private set

    fun writeMemory(address: Int, value: Int) {
        val byte = (value and 0xff).toByte()
        val column = when (address) {
            in 0xf800..0xf83b -> address - 0xf800
            in 0xf840..0xf87b -> 119 - (address - 0xf840)
            else -> null
        }
        if (column != null && dotColumns[column] != byte) {
            dotColumns[column] = byte
            revision++
        }
        when (address) {
            0xf83c -> if (symbol0 != (value and 0xff)) { symbol0 = value and 0xff; revision++ }
            0xf83d -> if (symbol1 != (value and 0xff)) { symbol1 = value and 0xff; revision++ }
        }
    }

    fun setEnabled(value: Boolean) {
        if (enabled != value) { enabled = value; revision++ }
    }

    fun reset() {
        val changed = dotColumns.any { it.toInt() != 0 } || symbol0 != 0 || symbol1 != 0 || !enabled
        dotColumns.fill(0)
        symbol0 = 0
        symbol1 = 0
        enabled = true
        if (changed) revision++
    }

    fun copyDotColumns(): ByteArray = dotColumns.copyOf()
    fun symbolState0(): Int = symbol0
    fun symbolState1(): Int = symbol1

    companion object {
        const val CHARACTER_COLUMNS: Int = 24
        const val CHARACTER_WIDTH: Int = 5
        const val DOT_ROWS: Int = 7
        const val DOT_COLUMN_COUNT: Int = CHARACTER_COLUMNS * CHARACTER_WIDTH
    }
}
