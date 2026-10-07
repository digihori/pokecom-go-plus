package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.machine.FourLineDisplay

internal class Pc1350Display {
    private val delegate = FourLineDisplay(VRAM_BLOCK_STARTS, SYMBOL_ADDRESS)
    val revision: Long get() = delegate.revision
    fun writeMemory(address: Int, value: Int) = delegate.writeMemory(address, value)
    fun reset() = delegate.reset()
    fun copyVramColumns(): ByteArray = delegate.copyVramColumns()
    fun copyDots(): ByteArray = delegate.copyDots()
    fun symbolState(): Int = delegate.symbolState()

    companion object {
        const val CHARACTER_COLUMNS: Int = FourLineDisplay.CHARACTER_COLUMNS
        const val CHARACTER_ROWS: Int = FourLineDisplay.CHARACTER_ROWS
        const val CHARACTER_WIDTH: Int = FourLineDisplay.CHARACTER_WIDTH
        const val CHARACTER_HEIGHT: Int = FourLineDisplay.CHARACTER_HEIGHT
        const val DOT_COLUMNS: Int = FourLineDisplay.DOT_COLUMNS
        const val DOT_ROWS: Int = FourLineDisplay.DOT_ROWS
        const val VRAM_BLOCK_SIZE: Int = FourLineDisplay.VRAM_BLOCK_SIZE
        const val VRAM_COLUMN_COUNT: Int = FourLineDisplay.VRAM_COLUMN_COUNT
        const val SYMBOL_ADDRESS: Int = 0x783c

        val VRAM_BLOCK_STARTS: List<Int> = listOf(
            0x7000, 0x7200, 0x7400, 0x7600, 0x7800,
            0x7040, 0x7240, 0x7440, 0x7640, 0x7840,
            0x701e, 0x721e, 0x741e, 0x761e, 0x781e,
            0x705e, 0x725e, 0x745e, 0x765e, 0x785e,
        )
    }
}
