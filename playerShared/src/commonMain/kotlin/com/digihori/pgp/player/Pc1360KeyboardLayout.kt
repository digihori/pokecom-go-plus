package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Studio's domestic PC-1360 legends; the Player illustration has only a left SHIFT. */
public object Pc1360KeyboardLayout {
    public val rows: List<List<PlayerKeyCap>> = listOf(
        listOf(
            PlayerKeyCap(PocketKey.MODE, "MODE", column = 0),
            PlayerKeyCap(PocketKey.BREAK, "BRK", shiftedLabel = "ON", column = 1),
            PlayerKeyCap(PocketKey.DOWN, "↓", column = 2), PlayerKeyCap(PocketKey.UP, "↑", column = 3),
            PlayerKeyCap(PocketKey.LEFT, "←", column = 4), PlayerKeyCap(PocketKey.RIGHT, "→", column = 5),
            PlayerKeyCap(PocketKey.KANA, "カナ", column = 6),
            PlayerKeyCap(PocketKey.DELETE, "DEL", column = 7), PlayerKeyCap(PocketKey.INSERT, "INS", column = 8),
            PlayerKeyCap(PocketKey.CLEAR, "CLS", shiftedLabel = "CA", column = 10),
            PlayerKeyCap(PocketKey.NUM_7, "7", column = 11), PlayerKeyCap(PocketKey.NUM_8, "8", column = 12),
            PlayerKeyCap(PocketKey.NUM_9, "9", column = 13),
            PlayerKeyCap(PocketKey.COMMA, ",", shiftedLabel = "<", column = 14),
            PlayerKeyCap(PocketKey.COLON, ":", shiftedLabel = ">", column = 15),
        ),
        Pc1350KeyboardLayout.rows[1].map { cap -> when (cap.key) {
            PocketKey.DIVIDE -> cap.copy(shiftedLabel = "(")
            PocketKey.COLON -> cap.copy(key = PocketKey.SEMICOLON, id = PocketKey.SEMICOLON.name,
                primaryLabel = ";", shiftedLabel = ")")
            else -> cap
        } },
        Pc1350KeyboardLayout.rows[2].filterNot { it.key == PocketKey.SEMICOLON }.map { cap ->
            if (cap.key == PocketKey.MULTIPLY) cap.copy(shiftedLabel = "¥") else cap
        },
        Pc1350KeyboardLayout.rows[3].filterNot { it.key == PocketKey.COMMA }.map { cap ->
            if (cap.key == PocketKey.KANA) cap.copy(key = PocketKey.SMALL, id = PocketKey.SMALL.name,
                primaryLabel = "SML", shiftedLabel = null) else cap
        },
    )
    public val keyboard: PlayerKeyboard = PlayerKeyboard(16, rows)
}
