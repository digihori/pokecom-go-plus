package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Key identities/shift legends follow Studio; labels match the bundled PC-1350 illustration. */
public object Pc1350KeyboardLayout {
    public const val COLUMN_COUNT: Int = 16

    public val rows: List<List<PlayerKeyCap>> = listOf(
        listOf(
            cap(PocketKey.MODE, "MODE", 0), cap(PocketKey.BREAK, "BRK", 1, shifted = "ON"),
            cap(PocketKey.DOWN, "↓", 2), cap(PocketKey.UP, "↑", 3),
            cap(PocketKey.LEFT, "←", 4), cap(PocketKey.RIGHT, "→", 5),
            cap(PocketKey.DELETE, "DEL", 7), cap(PocketKey.INSERT, "INS", 8),
            cap(PocketKey.SHIFT, "SHIFT", 9, id = "SHIFT_RIGHT"), cap(PocketKey.CLEAR, "CLS", 10, shifted = "CA"),
            cap(PocketKey.NUM_7, "7", 11), cap(PocketKey.NUM_8, "8", 12), cap(PocketKey.NUM_9, "9", 13),
            cap(PocketKey.LEFT_PAREN, "(", 14, shifted = "<"), cap(PocketKey.RIGHT_PAREN, ")", 15, shifted = ">"),
        ),
        listOf(
            cap(PocketKey.SHIFT, "SHIFT", 0, id = "SHIFT_LEFT"),
            cap(PocketKey.Q, "Q", 1, shifted = "!"), cap(PocketKey.W, "W", 2, shifted = "\""), cap(PocketKey.E, "E", 3, shifted = "#"),
            cap(PocketKey.R, "R", 4, shifted = "$"), cap(PocketKey.T, "T", 5, shifted = "%"), cap(PocketKey.Y, "Y", 6, shifted = "&"),
            cap(PocketKey.U, "U", 7, shifted = "?"), cap(PocketKey.I, "I", 8, shifted = "π"), cap(PocketKey.O, "O", 9, shifted = "√"), cap(PocketKey.P, "P", 10, shifted = "@"),
            cap(PocketKey.NUM_4, "4", 11), cap(PocketKey.NUM_5, "5", 12), cap(PocketKey.NUM_6, "6", 13),
            cap(PocketKey.DIVIDE, "÷", 14, shifted = "¥"), cap(PocketKey.COLON, ":", 15),
        ),
        listOf(
            cap(PocketKey.DEF, "DEF", 0),
            cap(PocketKey.A, "A", 1), cap(PocketKey.S, "S", 2), cap(PocketKey.D, "D", 3),
            cap(PocketKey.F, "F", 4), cap(PocketKey.G, "G", 5), cap(PocketKey.H, "H", 6),
            cap(PocketKey.J, "J", 7), cap(PocketKey.K, "K", 8), cap(PocketKey.L, "L", 9), cap(PocketKey.EQUALS, "=", 10),
            cap(PocketKey.NUM_1, "1", 11), cap(PocketKey.NUM_2, "2", 12), cap(PocketKey.NUM_3, "3", 13),
            cap(PocketKey.MULTIPLY, "×", 14), cap(PocketKey.SEMICOLON, ";", 15),
        ),
        listOf(
            cap(PocketKey.KANA, "カナ", 0, shifted = "SML"),
            cap(PocketKey.Z, "Z", 1), cap(PocketKey.X, "X", 2), cap(PocketKey.C, "C", 3),
            cap(PocketKey.V, "V", 4), cap(PocketKey.B, "B", 5), cap(PocketKey.N, "N", 6), cap(PocketKey.M, "M", 7),
            cap(PocketKey.SPACE, "SPC", 8), cap(PocketKey.ENTER, "ENTER", 9, span = 2, shifted = "N↔NP"),
            cap(PocketKey.NUM_0, "0", 11), cap(PocketKey.DOT, ".", 12), cap(PocketKey.PLUS, "+", 13),
            cap(PocketKey.MINUS, "−", 14, shifted = "^"), cap(PocketKey.COMMA, ",", 15),
        ),
    )

    public val keyboard: PlayerKeyboard = PlayerKeyboard(COLUMN_COUNT, rows)

    private fun cap(
        key: PocketKey,
        label: String,
        column: Int,
        span: Int = 1,
        shifted: String? = null,
        id: String = key.name,
    ): PlayerKeyCap = PlayerKeyCap(
        key = key,
        id = id,
        primaryLabel = label,
        shiftedLabel = shifted,
        column = column,
        columnSpan = span,
    )
}
