package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.api.PocketKey

internal class Pc1350Keyboard {
    private val groupMasks = IntArray(GROUP_COUNT)
    private val pressedKeys = mutableSetOf<PocketKey>()
    private var keyOnCountdown: Int = 0

    fun press(key: PocketKey): Boolean {
        if (key == PocketKey.BREAK) {
            if (pressedKeys.add(key)) keyOnCountdown = KEY_ON_PULSE_COUNT
            return true
        }
        val position = KEY_POSITIONS[key] ?: return false
        if (pressedKeys.add(key)) {
            groupMasks[position.group] = groupMasks[position.group] or (1 shl position.row)
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
            groupMasks[position.group] = groupMasks[position.group] and (1 shl position.row).inv()
        }
        return true
    }

    fun readInputA(ia: Int, memorySelector: Int): Int {
        if (ia == 0 && memorySelector != 0) {
            val selected = lowestSetBit(memorySelector)
            return if (selected < MEMORY_SELECTED_GROUPS) groupMasks[selected] else 0
        }
        if (ia != 0) {
            val selected = lowestSetBit(ia)
            return if (selected < IA_SELECTED_GROUPS) groupMasks[selected + MEMORY_SELECTED_GROUPS] else 0
        }
        return 0
    }

    fun consumeKeyOnSignal(): Boolean {
        if (keyOnCountdown <= 0) return false
        keyOnCountdown--
        return true
    }

    fun reset() {
        groupMasks.fill(0)
        pressedKeys.clear()
        keyOnCountdown = 0
    }

    internal fun groupMask(group: Int): Int = groupMasks[group]

    private fun lowestSetBit(value: Int): Int {
        for (bit in 0 until 8) if (value and (1 shl bit) != 0) return bit
        return 8
    }

    private data class Position(val group: Int, val row: Int)

    private companion object {
        const val GROUP_COUNT = 12
        const val MEMORY_SELECTED_GROUPS = 7
        const val IA_SELECTED_GROUPS = 5
        const val KEY_ON_PULSE_COUNT = 10

        val KEY_POSITIONS: Map<PocketKey, Position> = mapOf(
            PocketKey.RIGHT_PAREN to Position(0, 0), PocketKey.COLON to Position(0, 1),
            PocketKey.SEMICOLON to Position(0, 2), PocketKey.COMMA to Position(0, 3),
            PocketKey.KANA to Position(0, 4), PocketKey.DEF to Position(0, 5), PocketKey.SHIFT to Position(0, 6),
            PocketKey.LEFT_PAREN to Position(1, 0), PocketKey.DIVIDE to Position(1, 1), PocketKey.MULTIPLY to Position(1, 2),
            PocketKey.MINUS to Position(1, 3), PocketKey.Z to Position(1, 4),
            PocketKey.A to Position(1, 5), PocketKey.Q to Position(1, 6),
            PocketKey.NUM_9 to Position(2, 0), PocketKey.NUM_6 to Position(2, 1),
            PocketKey.NUM_3 to Position(2, 2), PocketKey.PLUS to Position(2, 3),
            PocketKey.X to Position(2, 4), PocketKey.S to Position(2, 5), PocketKey.W to Position(2, 6),
            PocketKey.NUM_8 to Position(3, 0), PocketKey.NUM_5 to Position(3, 1),
            PocketKey.NUM_2 to Position(3, 2), PocketKey.DOT to Position(3, 3),
            PocketKey.C to Position(3, 4), PocketKey.D to Position(3, 5), PocketKey.E to Position(3, 6),
            PocketKey.NUM_7 to Position(4, 0), PocketKey.NUM_4 to Position(4, 1),
            PocketKey.NUM_1 to Position(4, 2), PocketKey.NUM_0 to Position(4, 3),
            PocketKey.V to Position(4, 4), PocketKey.F to Position(4, 5), PocketKey.R to Position(4, 6),
            PocketKey.UP to Position(5, 0), PocketKey.DOWN to Position(5, 1),
            PocketKey.LEFT to Position(5, 2), PocketKey.RIGHT to Position(5, 3),
            PocketKey.B to Position(5, 4), PocketKey.G to Position(5, 5), PocketKey.T to Position(5, 6),
            PocketKey.INSERT to Position(7, 2), PocketKey.DELETE to Position(7, 3),
            PocketKey.N to Position(7, 4), PocketKey.H to Position(7, 5), PocketKey.Y to Position(7, 6),
            PocketKey.MODE to Position(8, 3), PocketKey.M to Position(8, 4), PocketKey.J to Position(8, 5), PocketKey.U to Position(8, 6),
            PocketKey.CLEAR to Position(9, 3), PocketKey.SPACE to Position(9, 4),
            PocketKey.K to Position(9, 5), PocketKey.I to Position(9, 6),
            PocketKey.ENTER to Position(10, 4), PocketKey.L to Position(10, 5), PocketKey.O to Position(10, 6),
            PocketKey.EQUALS to Position(11, 5), PocketKey.P to Position(11, 6),
        )
    }
}
