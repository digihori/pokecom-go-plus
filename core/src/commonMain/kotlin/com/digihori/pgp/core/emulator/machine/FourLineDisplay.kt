package com.digihori.pgp.core.emulator.machine

internal class FourLineDisplay(
    private val vramBlockStarts: List<Int>,
    private val symbolAddress: Int,
) {
    private val vramColumns = ByteArray(VRAM_COLUMN_COUNT)
    private var symbols: Int = 0

    var revision: Long = 0
        private set

    fun writeMemory(address: Int, value: Int) {
        val block = vramBlockStarts.indexOfFirst { address in it until it + VRAM_BLOCK_SIZE }
        if (block >= 0) {
            val index = block * VRAM_BLOCK_SIZE + address - vramBlockStarts[block]
            val byte = (value and 0xff).toByte()
            if (vramColumns[index] != byte) {
                vramColumns[index] = byte
                revision++
            }
        } else if (address == symbolAddress && symbols != (value and 0xff)) {
            symbols = value and 0xff
            revision++
        }
    }

    fun reset() {
        val changed = vramColumns.any { it.toInt() != 0 } || symbols != 0
        vramColumns.fill(0)
        symbols = 0
        if (changed) revision++
    }

    fun copyVramColumns(): ByteArray = vramColumns.copyOf()

    fun copyDots(): ByteArray {
        val dots = ByteArray(DOT_COLUMNS * DOT_ROWS)
        for (characterRow in 0 until CHARACTER_ROWS) {
            val sourceRowStart = characterRow * DOT_COLUMNS
            val destinationRowStart = characterRow * CHARACTER_HEIGHT
            for (column in 0 until DOT_COLUMNS) {
                val bits = vramColumns[sourceRowStart + column].toInt() and 0xff
                for (row in 0 until CHARACTER_HEIGHT) {
                    if (bits and (1 shl row) != 0) {
                        dots[(destinationRowStart + row) * DOT_COLUMNS + column] = 1
                    }
                }
            }
        }
        return dots
    }

    fun symbolState(): Int = symbols

    companion object {
        const val CHARACTER_COLUMNS = 25
        const val CHARACTER_ROWS = 4
        const val CHARACTER_WIDTH = 6
        const val CHARACTER_HEIGHT = 8
        const val DOT_COLUMNS = CHARACTER_COLUMNS * CHARACTER_WIDTH
        const val DOT_ROWS = CHARACTER_ROWS * CHARACTER_HEIGHT
        const val VRAM_BLOCK_SIZE = 30
        const val VRAM_COLUMN_COUNT = DOT_COLUMNS * CHARACTER_ROWS
    }
}
