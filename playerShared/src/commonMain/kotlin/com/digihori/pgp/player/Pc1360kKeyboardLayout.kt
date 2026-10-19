package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Existing Studio kanji controls, placed on the Player's single-SHIFT illustration. */
public object Pc1360kKeyboardLayout {
    public val rows: List<List<PlayerKeyCap>> = Pc1360KeyboardLayout.rows.mapIndexed { index, row ->
        row + when (index) {
            0 -> listOf(PlayerKeyCap(PocketKey.KANJI, "漢字", column = 16))
            1 -> listOf(PlayerKeyCap(PocketKey.HIRA_KATA, "平/片", column = 16))
            2 -> listOf(PlayerKeyCap(PocketKey.KANJI_FIX, "採用", column = 15),
                PlayerKeyCap(PocketKey.YJ, "▲▼", column = 16))
            else -> listOf(PlayerKeyCap(PocketKey.KANJI_CONVERT, "変換", column = 15))
        }
    }
    public val keyboard: PlayerKeyboard = PlayerKeyboard(17, rows)
}
