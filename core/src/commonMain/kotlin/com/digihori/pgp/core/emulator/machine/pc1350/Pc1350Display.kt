package com.digihori.pgp.core.emulator.machine.pc1350

internal class Pc1350Display {
    private val vramColumns = ByteArray(VRAM_COLUMN_COUNT)
    private var symbols: Int = 0

    var revision: Long = 0
        private set

    fun writeMemory(address: Int, value: Int) {
        val block = VRAM_BLOCK_STARTS.indexOfFirst { address in it until it + VRAM_BLOCK_SIZE }
        if (block >= 0) {
            val index = block * VRAM_BLOCK_SIZE + address - VRAM_BLOCK_STARTS[block]
            val byte = (value and 0xff).toByte()
            if (vramColumns[index] != byte) {
                vramColumns[index] = byte
                revision++
            }
        } else if (address == SYMBOL_ADDRESS && symbols != (value and 0xff)) {
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

    /** Converts the line-major VRAM bytes into the row-major dot plane used by DisplaySnapshot. */
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
        const val CHARACTER_COLUMNS: Int = 25
        const val CHARACTER_ROWS: Int = 4
        const val CHARACTER_WIDTH: Int = 6
        const val CHARACTER_HEIGHT: Int = 8
        const val DOT_COLUMNS: Int = CHARACTER_COLUMNS * CHARACTER_WIDTH
        const val DOT_ROWS: Int = CHARACTER_ROWS * CHARACTER_HEIGHT
        const val VRAM_BLOCK_SIZE: Int = 30
        const val VRAM_COLUMN_COUNT: Int = DOT_COLUMNS * CHARACTER_ROWS
        const val SYMBOL_ADDRESS: Int = 0x783c

        val VRAM_BLOCK_STARTS: List<Int> = listOf(
            0x7000, 0x7200, 0x7400, 0x7600, 0x7800,
            0x7040, 0x7240, 0x7440, 0x7640, 0x7840,
            0x701e, 0x721e, 0x741e, 0x761e, 0x781e,
            0x705e, 0x725e, 0x745e, 0x765e, 0x785e,
        )
    }
}
