package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Legends and scan identities ported from Studio Pc14xxKeyboardLayout. */
public object Pc1450KeyboardLayout {
    public val keyboard: PlayerKeyboard = Pc1401KeyboardLayout.keyboard.let { base ->
        base.copy(columnCount = 16, rows = listOf(
            listOf(PocketKey.CAL, PocketKey.BASIC, PocketKey.BREAK, PocketKey.KANA, PocketKey.DAKUTEN,
                PocketKey.LONG_VOWEL, PocketKey.DOWN, PocketKey.UP, PocketKey.LEFT, PocketKey.RIGHT,
                PocketKey.SHIFT, PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9, PocketKey.DIVIDE, PocketKey.MEMORY_STORE)
                .mapIndexed { column, key -> PlayerKeyCap(key, label(key), column = column, shiftedLabel = when (key) {
                    PocketKey.BREAK -> "ON"; PocketKey.LEFT -> "DEL"; PocketKey.RIGHT -> "INS"; else -> null }) },
        ) + base.rows.drop(1).mapIndexed { row, caps ->
            listOf(PlayerKeyCap(listOf(PocketKey.LOCK, PocketKey.DEF, PocketKey.SMALL)[row],
                label(listOf(PocketKey.LOCK, PocketKey.DEF, PocketKey.SMALL)[row]), column = 0)) +
                caps.map { it.copy(column = it.column + 1, shiftedLabel = if (it.key == PocketKey.MEMORY_ADD) null else it.shiftedLabel) }
        })
    }
    private fun label(key: PocketKey): String = when (key) {
        PocketKey.LOCK -> "SHIFT"; PocketKey.BREAK -> "BRK"; PocketKey.KANA -> "カナ"; PocketKey.DAKUTEN -> "゛"
        PocketKey.LONG_VOWEL -> "ー"; PocketKey.SMALL -> "SML"; PocketKey.DOWN -> "↓"; PocketKey.UP -> "↑"
        PocketKey.LEFT -> "←"; PocketKey.RIGHT -> "→"; PocketKey.MEMORY_STORE -> "X↔M"
        PocketKey.DIVIDE -> "÷"; PocketKey.NUM_7 -> "7"; PocketKey.NUM_8 -> "8"; PocketKey.NUM_9 -> "9"
        else -> key.name
    }
}
