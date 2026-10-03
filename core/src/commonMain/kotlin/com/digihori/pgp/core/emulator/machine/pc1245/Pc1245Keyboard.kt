package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey

internal class Pc1245Keyboard {
    private val columnMasks: IntArray = IntArray(COLUMN_COUNT)
    private val pressedKeys: MutableSet<PocketKey> = mutableSetOf()
    private var keyOnCountdown: Int = 0

    var operatingMode: OperatingMode = OperatingMode.RUN
        private set

    var modeRevision: Long = 0
        private set

    fun press(key: PocketKey): Boolean {
        if (key == PocketKey.BREAK) {
            if (pressedKeys.add(key)) keyOnCountdown = KEY_ON_PULSE_COUNT
            return true
        }

        val position = KEY_POSITIONS[key] ?: return false
        if (pressedKeys.add(key)) {
            columnMasks[position.column] = columnMasks[position.column] or (1 shl position.row)
        }
        return true
    }

    fun release(key: PocketKey): Boolean {
        if (key == PocketKey.BREAK) {
            pressedKeys.remove(key)
            return true
        }

        val position = KEY_POSITIONS[key] ?: return false
        if (pressedKeys.remove(key)) {
            columnMasks[position.column] = columnMasks[position.column] and (1 shl position.row).inv()
        }
        return true
    }

    fun setOperatingMode(mode: OperatingMode) {
        if (operatingMode != mode) {
            operatingMode = mode
            modeRevision++
        }
    }

    fun readInputA(ia: Int, ib: Int): Int {
        if (ia == 0 && ib != 0) {
            val selected = lowestSetBit(ib)
            return if (selected < 3) columnMasks[selected] else 0
        }
        if (ia != 0) {
            val selected = lowestSetBit(ia)
            return if (selected < 7) columnMasks[selected + 3] else 0
        }
        return 0
    }

    fun readInputB(ib: Int): Int {
        var result = 0
        if (ib and 0x08 != 0) {
            if (operatingMode == OperatingMode.PROGRAM) result = result or 0x02
        } else if (ib and 0x04 != 0) {
            // No PC-1245 mode contact is connected for this selection.
        } else if (ib and 0x02 != 0) {
            if (operatingMode == OperatingMode.PROGRAM) result = result or 0x08
        }
        return result and ib.inv() and 0xff
    }

    /** Models the ten TEST-instruction observations triggered by BREAK in Pokecom GO. */
    fun consumeKeyOnSignal(): Boolean {
        if (keyOnCountdown <= 0) return false
        keyOnCountdown--
        return true
    }

    fun reset() {
        columnMasks.fill(0)
        pressedKeys.clear()
        keyOnCountdown = 0
        if (operatingMode != OperatingMode.RUN) {
            operatingMode = OperatingMode.RUN
            modeRevision++
        }
    }

    internal fun columnMask(column: Int): Int = columnMasks[column]

    private fun lowestSetBit(value: Int): Int {
        for (bit in 0 until 8) {
            if (value and (1 shl bit) != 0) return bit
        }
        return 8
    }

    private data class Position(val column: Int, val row: Int)

    private companion object {
        const val COLUMN_COUNT: Int = 10
        const val KEY_ON_PULSE_COUNT: Int = 10

        val KEY_POSITIONS: Map<PocketKey, Position> = mapOf(
            PocketKey.MINUS to Position(0, 0),
            PocketKey.CLEAR to Position(0, 1),
            PocketKey.MULTIPLY to Position(0, 2),
            PocketKey.DIVIDE to Position(0, 3),
            PocketKey.DOWN to Position(0, 4),
            PocketKey.E to Position(0, 5),
            PocketKey.D to Position(0, 6),
            PocketKey.C to Position(0, 7),
            PocketKey.PLUS to Position(1, 0),
            PocketKey.NUM_9 to Position(1, 1),
            PocketKey.NUM_3 to Position(1, 2),
            PocketKey.NUM_6 to Position(1, 3),
            PocketKey.SHIFT to Position(1, 4),
            PocketKey.W to Position(1, 5),
            PocketKey.S to Position(1, 6),
            PocketKey.X to Position(1, 7),
            PocketKey.DOT to Position(2, 0),
            PocketKey.NUM_8 to Position(2, 1),
            PocketKey.NUM_2 to Position(2, 2),
            PocketKey.NUM_5 to Position(2, 3),
            PocketKey.DEF to Position(2, 4),
            PocketKey.Q to Position(2, 5),
            PocketKey.A to Position(2, 6),
            PocketKey.Z to Position(2, 7),
            PocketKey.NUM_7 to Position(3, 1),
            PocketKey.NUM_1 to Position(3, 2),
            PocketKey.NUM_4 to Position(3, 3),
            PocketKey.UP to Position(3, 4),
            PocketKey.R to Position(3, 5),
            PocketKey.F to Position(3, 6),
            PocketKey.V to Position(3, 7),
            PocketKey.EQUALS to Position(4, 2),
            PocketKey.P to Position(4, 3),
            PocketKey.LEFT to Position(4, 4),
            PocketKey.T to Position(4, 5),
            PocketKey.G to Position(4, 6),
            PocketKey.B to Position(4, 7),
            PocketKey.O to Position(5, 3),
            PocketKey.RIGHT to Position(5, 4),
            PocketKey.Y to Position(5, 5),
            PocketKey.H to Position(5, 6),
            PocketKey.N to Position(5, 7),
            PocketKey.U to Position(6, 5),
            PocketKey.J to Position(6, 6),
            PocketKey.M to Position(6, 7),
            PocketKey.I to Position(7, 5),
            PocketKey.K to Position(7, 6),
            PocketKey.SPACE to Position(7, 7),
            PocketKey.L to Position(8, 6),
            PocketKey.ENTER to Position(8, 7),
            PocketKey.NUM_0 to Position(9, 7),
        )
    }
}
