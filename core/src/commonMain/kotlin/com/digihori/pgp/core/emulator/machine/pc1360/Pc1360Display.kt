package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.emulator.machine.FourLineDisplay

internal class Pc1360Display {
    private val delegate = FourLineDisplay(VRAM_BLOCK_STARTS, SYMBOL_ADDRESS)
    val revision: Long get() = delegate.revision
    fun writeMemory(address: Int, value: Int) = delegate.writeMemory(address, value)
    fun reset() = delegate.reset()
    fun copyVramColumns(): ByteArray = delegate.copyVramColumns()
    fun copyDots(): ByteArray = delegate.copyDots()
    fun symbolState(): Int = delegate.symbolState()

    companion object {
        const val CHARACTER_COLUMNS = FourLineDisplay.CHARACTER_COLUMNS
        const val CHARACTER_ROWS = FourLineDisplay.CHARACTER_ROWS
        const val CHARACTER_WIDTH = FourLineDisplay.CHARACTER_WIDTH
        const val DOT_ROWS = FourLineDisplay.DOT_ROWS
        const val VRAM_BLOCK_SIZE = FourLineDisplay.VRAM_BLOCK_SIZE
        const val SYMBOL_ADDRESS = 0x303c
        val VRAM_BLOCK_STARTS: List<Int> = listOf(
            0x2800, 0x2a00, 0x2c00, 0x2e00, 0x3000,
            0x2840, 0x2a40, 0x2c40, 0x2e40, 0x3040,
            0x281e, 0x2a1e, 0x2c1e, 0x2e1e, 0x301e,
            0x285e, 0x2a5e, 0x2c5e, 0x2e5e, 0x305e,
        )
    }
}
