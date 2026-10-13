package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.PocketKey

internal class Pc14xxKeyboard(private val model: Pc14xxModel) {
    private val pressed = mutableSetOf<PocketKey>()
    private val masks = IntArray(13)
    private var keyOnCountdown = 0
    fun supportedKeys(): Set<PocketKey> = positions.keys.toSet() + PocketKey.BREAK
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
    fun readInputA(ia: Int, selector: Int): Int {
        val selected = if (ia == 0) {
            if (model == Pc14xxModel.PC_1470U) selector - 1 else lowestSetBit(selector)
        } else lowestSetBit(ia) + 7
        return if (selected in 0..12 && (ia != 0 || selected < 7)) masks[selected] else 0
    }
    private fun lowestSetBit(value: Int): Int = (0..7).firstOrNull { value and (1 shl it) != 0 } ?: 8
    fun consumeKeyOnSignal(): Boolean {
        val latched = keyOnCountdown > 0
        if (latched) keyOnCountdown--
        // Keep KEY ON asserted while held, retaining the legacy pulse for short taps.
        return PocketKey.BREAK in pressed || latched
    }

    fun reset() { pressed.clear(); masks.fill(0); keyOnCountdown = 0 }
    private val positions: Map<PocketKey, Pair<Int, Int>> = buildMap {
        val rows = listOf(
            if (model == Pc14xxModel.PC_1470U) "NUM_7 NUM_8 NUM_9 DIVIDE DELETE _ _ _" else "NUM_7 NUM_8 NUM_9 DIVIDE MEMORY_STORE _ _ _",
            if (model == Pc14xxModel.PC_1470U) "NUM_4 NUM_5 NUM_6 MULTIPLY BACKSPACE SHIFT DEF SMALL" else "NUM_4 NUM_5 NUM_6 MULTIPLY MEMORY_RECALL SHIFT DEF SMALL",
            if (model == Pc14xxModel.PC_1470U) "NUM_1 NUM_2 NUM_3 MINUS INSERT Q A Z" else "NUM_1 NUM_2 NUM_3 MINUS MEMORY_ADD Q A Z",
            "NUM_0 SIGN DOT PLUS EQUALS W S X",
            "HYP SIN COS TAN KANA E D C",
            "HEX DEGREES LN LOG DAKUTEN R F V",
            "EXPONENT POWER ROOT SQUARE LONG_VOWEL T G B",
            if (model == Pc14xxModel.PC_1470U) "_ CLEAR _ FORMAT DOWN Y H N" else "_ CLEAR STAT FORMAT DOWN Y H N",
            "_ _ RIGHT_PAREN RECIPROCAL UP U J M",
            "_ _ _ LEFT_PAREN LEFT I K SPACE",
            "_ _ _ _ RIGHT O L ENTER",
            "_ _ _ _ _ P COMMA BASIC",
            if (model == Pc14xxModel.PC_1470U) "_ _ _ _ _ _ _ CAL" else "_ _ _ _ _ _ LOCK CAL",
        )
        rows.forEachIndexed { group, row -> row.split(' ').forEachIndexed { bit, name ->
            if (name != "_") put(PocketKey.valueOf(name), group to bit)
        } }
    }
}
