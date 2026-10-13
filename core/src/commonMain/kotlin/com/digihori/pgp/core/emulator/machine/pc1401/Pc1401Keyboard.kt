package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.PocketKey

internal class Pc1401Keyboard {
    private val pressed = mutableSetOf<PocketKey>()
    private val masks = IntArray(14)
    private var keyOnCountdown = 0
    fun press(key: PocketKey): Boolean {
        if (key == PocketKey.BREAK) {
            if (pressed.add(key)) keyOnCountdown = 10
            return true
        }
        val (group, bit) = positions[key] ?: return false
        if (pressed.add(key)) masks[group] = masks[group] or (1 shl bit)
        return true
    }
    fun release(key: PocketKey): Boolean {
        if (key == PocketKey.BREAK) { pressed.remove(key); return true }
        val (group, bit) = positions[key] ?: return false
        if (pressed.remove(key)) masks[group] = masks[group] and (1 shl bit).inv()
        return true
    }
    // Retain the established Pokecom GO scan semantics; simultaneous select lines are tested separately.
    fun readInputA(ia: Int, ib: Int): Int {
        val select = if (ia != 0) ia else ib
        if (select == 0) return 0
        val bit = (0..7).first { select and (1 shl it) != 0 }
        return if (bit < 7) masks[bit + if (ia != 0) 7 else 0] else 0
    }
    fun readInputB(ib: Int): Int = 0
    fun consumeKeyOnSignal(): Boolean {
        val latched = keyOnCountdown > 0
        if (latched) keyOnCountdown--
        // Keep KEY ON asserted while held, retaining the legacy pulse for short taps.
        return PocketKey.BREAK in pressed || latched
    }

    fun reset() { pressed.clear(); masks.fill(0); keyOnCountdown = 0 }
    private companion object {
        val positions = mapOf(
            PocketKey.SIGN to (0 to 0),
            PocketKey.NUM_8 to (0 to 1),
            PocketKey.NUM_2 to (0 to 2),
            PocketKey.NUM_5 to (0 to 3),
            PocketKey.CAL to (0 to 4),
            PocketKey.Q to (0 to 5),
            PocketKey.A to (0 to 6),
            PocketKey.Z to (0 to 7),
            PocketKey.DOT to (1 to 0),
            PocketKey.NUM_9 to (1 to 1),
            PocketKey.NUM_3 to (1 to 2),
            PocketKey.NUM_6 to (1 to 3),
            PocketKey.BASIC to (1 to 4),
            PocketKey.W to (1 to 5),
            PocketKey.S to (1 to 6),
            PocketKey.X to (1 to 7),
            PocketKey.PLUS to (2 to 0),
            PocketKey.DIVIDE to (2 to 1),
            PocketKey.MINUS to (2 to 2),
            PocketKey.MULTIPLY to (2 to 3),
            PocketKey.DEF to (2 to 4),
            PocketKey.E to (2 to 5),
            PocketKey.D to (2 to 6),
            PocketKey.C to (2 to 7),
            PocketKey.RIGHT_PAREN to (3 to 0),
            PocketKey.LEFT_PAREN to (3 to 1),
            PocketKey.SQUARE to (3 to 2),
            PocketKey.ROOT to (3 to 3),
            PocketKey.POWER to (3 to 4),
            PocketKey.EXPONENT to (3 to 5),
            PocketKey.MEMORY_STORE to (3 to 6),
            PocketKey.EQUALS to (3 to 7),
            PocketKey.STAT to (4 to 0),
            PocketKey.RECIPROCAL to (4 to 1),
            PocketKey.LOG to (4 to 2),
            PocketKey.LN to (4 to 3),
            PocketKey.DEGREES to (4 to 4),
            PocketKey.HEX to (4 to 5),
            PocketKey.MEMORY_ADD to (4 to 7),
            PocketKey.CLEAR to (5 to 0),
            PocketKey.FORMAT to (5 to 1),
            PocketKey.TAN to (5 to 2),
            PocketKey.COS to (5 to 3),
            PocketKey.SIN to (5 to 4),
            PocketKey.HYP to (5 to 5),
            PocketKey.SHIFT to (5 to 6),
            PocketKey.MEMORY_RECALL to (5 to 7),
            PocketKey.NUM_7 to (7 to 1),
            PocketKey.NUM_1 to (7 to 2),
            PocketKey.NUM_4 to (7 to 3),
            PocketKey.DOWN to (7 to 4),
            PocketKey.R to (7 to 5),
            PocketKey.F to (7 to 6),
            PocketKey.V to (7 to 7),
            PocketKey.COMMA to (8 to 2),
            PocketKey.P to (8 to 3),
            PocketKey.UP to (8 to 4),
            PocketKey.T to (8 to 5),
            PocketKey.G to (8 to 6),
            PocketKey.B to (8 to 7),
            PocketKey.O to (9 to 3),
            PocketKey.LEFT to (9 to 4),
            PocketKey.Y to (9 to 5),
            PocketKey.H to (9 to 6),
            PocketKey.N to (9 to 7),
            PocketKey.RIGHT to (10 to 4),
            PocketKey.U to (10 to 5),
            PocketKey.J to (10 to 6),
            PocketKey.M to (10 to 7),
            PocketKey.I to (11 to 5),
            PocketKey.K to (11 to 6),
            PocketKey.SPACE to (11 to 7),
            PocketKey.L to (12 to 6),
            PocketKey.ENTER to (12 to 7),
            PocketKey.NUM_0 to (13 to 7),
        )
    }
}
