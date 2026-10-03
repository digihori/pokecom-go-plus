package com.digihori.pgp.core.emulator.machine.pc1245

/** Logical PC-1245 LCD state, independent of drawing coordinates and UI toolkits. */
internal class Pc1245Display {
    private val dotColumns: ByteArray = ByteArray(DOT_COLUMN_COUNT)
    private var symbolState0: Int = 0
    private var symbolState1: Int = 0

    var enabled: Boolean = true
        private set

    var revision: Long = 0
        private set

    fun writeMemory(address: Int, value: Int) {
        val byteValue = value and 0xff
        when (address) {
            in 0xf800..0xf83b -> setDotColumn(address - 0xf800, byteValue)
            in 0xf868..0xf87b -> setDotColumn(79 - (address - 0xf868), byteValue)
            0xf83c -> {
                if (symbolState0 != byteValue) {
                    symbolState0 = byteValue
                    revision++
                }
            }
            0xf83d -> {
                if (symbolState1 != byteValue) {
                    symbolState1 = byteValue
                    revision++
                }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        if (enabled != value) {
            enabled = value
            revision++
        }
    }

    fun reset() {
        val changed = dotColumns.any { it.toInt() != 0 } ||
            symbolState0 != 0 ||
            symbolState1 != 0 ||
            !enabled

        dotColumns.fill(0)
        symbolState0 = 0
        symbolState1 = 0
        enabled = true
        if (changed) revision++
    }

    fun copyDotColumns(): ByteArray = dotColumns.copyOf()

    fun symbolState0(): Int = symbolState0

    fun symbolState1(): Int = symbolState1

    private fun setDotColumn(index: Int, value: Int) {
        val byteValue = value.toByte()
        if (dotColumns[index] != byteValue) {
            dotColumns[index] = byteValue
            revision++
        }
    }

    companion object {
        const val CHARACTER_COLUMNS: Int = 16
        const val CHARACTER_WIDTH: Int = 5
        const val DOT_ROWS: Int = 7
        const val DOT_COLUMN_COUNT: Int = CHARACTER_COLUMNS * CHARACTER_WIDTH
    }
}
