package com.digihori.pgp.core.emulator.machine.pc1261

internal class Pc1261Display {
    private val columns = ByteArray(DOT_COLUMN_COUNT)
    private var state0 = 0
    private var state1 = 0
    var revision: Long = 0
        private set

    fun writeMemory(address: Int, value: Int) {
        val column = when (address) {
            in 0x2000..0x203b -> address - 0x2000
            in 0x2800..0x283b -> 60 + address - 0x2800
            in 0x2040..0x207b -> 120 + address - 0x2040
            in 0x2840..0x287b -> 180 + address - 0x2840
            else -> null
        }
        val byte = (value and 0xff).toByte()
        if (column != null && columns[column] != byte) {
            columns[column] = byte
            revision++
        }
        when (address) {
            0x203d -> if (state0 != value and 0xff) { state0 = value and 0xff; revision++ }
            0x207c -> if (state1 != value and 0xff) { state1 = value and 0xff; revision++ }
        }
    }

    fun reset() {
        val changed = columns.any { it.toInt() != 0 } || state0 != 0 || state1 != 0
        columns.fill(0)
        state0 = 0
        state1 = 0
        if (changed) revision++
    }

    fun copyColumns(): ByteArray = columns.copyOf()
    fun symbolState0(): Int = state0
    fun symbolState1(): Int = state1

    companion object {
        const val CHARACTER_COLUMNS = 24
        const val CHARACTER_ROWS = 2
        const val CHARACTER_WIDTH = 5
        const val CHARACTER_HEIGHT = 7
        const val DOT_ROWS = CHARACTER_ROWS * CHARACTER_HEIGHT
        const val DOT_COLUMN_COUNT = CHARACTER_COLUMNS * CHARACTER_WIDTH * CHARACTER_ROWS
    }
}
