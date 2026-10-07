package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey

internal data class PocketKeyCap(
    val key: PocketKey,
    val primaryLabel: String,
    val shiftedLabel: String? = null,
    val basicLabel: String? = null,
    val column: Int,
    val columnSpan: Int = 1,
)

internal object Pc1245KeyboardLayout {
    const val COLUMN_COUNT: Int = 14

    val rows: List<List<PocketKeyCap>> = listOf(
        listOf(
            cap(PocketKey.DEF, "DEF", column = 3),
            cap(PocketKey.SHIFT, "SHIFT", column = 4),
            cap(PocketKey.DOWN, "↓", column = 5),
            cap(PocketKey.UP, "↑", column = 6),
            cap(PocketKey.LEFT, "←", shifted = "DEL", column = 7),
            cap(PocketKey.RIGHT, "→", shifted = "INS", column = 8),
            cap(PocketKey.BREAK, "BRK", shifted = "ON", column = 9),
            cap(PocketKey.NUM_7, "7", column = 10),
            cap(PocketKey.NUM_8, "8", column = 11),
            cap(PocketKey.NUM_9, "9", column = 12),
            cap(PocketKey.CLEAR, "CL", shifted = "CA", column = 13),
        ),
        listOf(
            cap(PocketKey.Q, "Q", shifted = "!", column = 0),
            cap(PocketKey.W, "W", shifted = "\"", column = 1),
            cap(PocketKey.E, "E", shifted = "#", column = 2),
            cap(PocketKey.R, "R", shifted = "$", column = 3),
            cap(PocketKey.T, "T", shifted = "%", column = 4),
            cap(PocketKey.Y, "Y", shifted = "&", column = 5),
            cap(PocketKey.U, "U", shifted = "?", column = 6),
            cap(PocketKey.I, "I", shifted = ":", column = 7),
            cap(PocketKey.O, "O", shifted = ",", column = 8),
            cap(PocketKey.P, "P", shifted = ";", column = 9),
            cap(PocketKey.NUM_4, "4", column = 10),
            cap(PocketKey.NUM_5, "5", column = 11),
            cap(PocketKey.NUM_6, "6", shifted = "¥", column = 12),
            cap(PocketKey.DIVIDE, "÷", shifted = "^", column = 13),
        ),
        listOf(
            cap(PocketKey.A, "A", basic = "INPUT", column = 0),
            cap(PocketKey.S, "S", basic = "IF", column = 1),
            cap(PocketKey.D, "D", basic = "THEN", column = 2),
            cap(PocketKey.F, "F", basic = "GOTO", column = 3),
            cap(PocketKey.G, "G", basic = "FOR", column = 4),
            cap(PocketKey.H, "H", basic = "TO", column = 5),
            cap(PocketKey.J, "J", basic = "STEP", column = 6),
            cap(PocketKey.K, "K", basic = "NEXT", column = 7),
            cap(PocketKey.L, "L", basic = "LIST", column = 8),
            cap(PocketKey.EQUALS, "=", basic = "RUN", column = 9),
            cap(PocketKey.NUM_1, "1", shifted = "(", column = 10),
            cap(PocketKey.NUM_2, "2", shifted = ")", column = 11),
            cap(PocketKey.NUM_3, "3", shifted = "@", column = 12),
            cap(PocketKey.MULTIPLY, "×", shifted = "<", column = 13),
        ),
        listOf(
            cap(PocketKey.Z, "Z", basic = "PRINT", column = 0),
            cap(PocketKey.X, "X", basic = "USING", column = 1),
            cap(PocketKey.C, "C", basic = "GOSUB", column = 2),
            cap(PocketKey.V, "V", basic = "RETURN", column = 3),
            cap(PocketKey.B, "B", basic = "DIM", column = 4),
            cap(PocketKey.N, "N", basic = "END", column = 5),
            cap(PocketKey.M, "M", basic = "CSAVE", column = 6),
            cap(PocketKey.SPACE, "SPACE", basic = "CLOAD", column = 7),
            cap(PocketKey.ENTER, "ENTER", basic = "N↔NP", column = 8, span = 2),
            cap(PocketKey.NUM_0, "0", shifted = "π", column = 10),
            cap(PocketKey.DOT, ".", shifted = "√", column = 11),
            cap(PocketKey.PLUS, "+", shifted = "EXP", column = 12),
            cap(PocketKey.MINUS, "−", shifted = ">", column = 13),
        ),
    )

    private fun cap(
        key: PocketKey,
        primary: String,
        shifted: String? = null,
        basic: String? = null,
        column: Int,
        span: Int = 1,
    ): PocketKeyCap = PocketKeyCap(key, primary, shifted, basic, column, span)
}

internal object Pc1251KeyboardLayout {
    const val COLUMN_COUNT: Int = Pc1245KeyboardLayout.COLUMN_COUNT

    val rows: List<List<PocketKeyCap>> = Pc1245KeyboardLayout.rows.map { row ->
        row.map { cap ->
            when (cap.key) {
                PocketKey.DOWN -> cap.copy(shiftedLabel = "(", basicLabel = null)
                PocketKey.UP -> cap.copy(shiftedLabel = ")", basicLabel = null)
                PocketKey.NUM_1, PocketKey.NUM_2 -> cap.copy(shiftedLabel = null, basicLabel = null)
                else -> cap.copy(basicLabel = null)
            }
        }
    }
}

internal object Pc1350KeyboardLayout {
    const val COLUMN_COUNT: Int = 16

    val rows: List<List<PocketKeyCap>> = listOf(
        listOf(
            cap(PocketKey.MODE, "MODE", 0), cap(PocketKey.BREAK, "BRK", 1),
            cap(PocketKey.DOWN, "↓", 2), cap(PocketKey.UP, "↑", 3),
            cap(PocketKey.LEFT, "←", 4), cap(PocketKey.RIGHT, "→", 5),
            cap(PocketKey.DELETE, "DEL", 7), cap(PocketKey.INSERT, "INS", 8),
            cap(PocketKey.SHIFT, "SHIFT", 9), cap(PocketKey.CLEAR, "CE", 10),
            cap(PocketKey.NUM_7, "7", 11), cap(PocketKey.NUM_8, "8", 12), cap(PocketKey.NUM_9, "9", 13),
            cap(PocketKey.LEFT_PAREN, "(", 14, shifted = "<"), cap(PocketKey.RIGHT_PAREN, ")", 15, shifted = ">"),
        ),
        listOf(
            cap(PocketKey.SHIFT, "SHIFT", 0),
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
            cap(PocketKey.KANA, "KANA", 0, shifted = "SML"),
            cap(PocketKey.Z, "Z", 1), cap(PocketKey.X, "X", 2), cap(PocketKey.C, "C", 3),
            cap(PocketKey.V, "V", 4), cap(PocketKey.B, "B", 5), cap(PocketKey.N, "N", 6), cap(PocketKey.M, "M", 7),
            cap(PocketKey.SPACE, "SPACE", 8), cap(PocketKey.ENTER, "ENTER", 9, span = 2),
            cap(PocketKey.NUM_0, "0", 11), cap(PocketKey.DOT, ".", 12), cap(PocketKey.PLUS, "+", 13),
            cap(PocketKey.MINUS, "−", 14, shifted = "^"), cap(PocketKey.COMMA, ",", 15),
        ),
    )

    private fun cap(
        key: PocketKey,
        label: String,
        column: Int,
        span: Int = 1,
        shifted: String? = null,
    ): PocketKeyCap = PocketKeyCap(
        key = key,
        primaryLabel = label,
        shiftedLabel = shifted,
        column = column,
        columnSpan = span,
    )
}

internal object Pc1360KeyboardLayout {
    const val COLUMN_COUNT: Int = 16

    val rows: List<List<PocketKeyCap>> = listOf(
        listOf(
            cap(PocketKey.MODE, "MODE", 0), cap(PocketKey.BREAK, "BRK", 1),
            cap(PocketKey.DOWN, "↓", 2), cap(PocketKey.UP, "↑", 3),
            cap(PocketKey.LEFT, "←", 4), cap(PocketKey.RIGHT, "→", 5),
            cap(PocketKey.KANA, "KANA", 6), cap(PocketKey.DELETE, "DEL", 7),
            cap(PocketKey.INSERT, "INS", 8), cap(PocketKey.SHIFT, "SHIFT", 9), cap(PocketKey.CLEAR, "CE", 10),
            cap(PocketKey.NUM_7, "7", 11), cap(PocketKey.NUM_8, "8", 12), cap(PocketKey.NUM_9, "9", 13),
            cap(PocketKey.COMMA, ",", 14, shifted = "<"), cap(PocketKey.COLON, ":", 15, shifted = ">"),
        ),
        Pc1350KeyboardLayout.rows[1].map { cap ->
            when (cap.key) {
                PocketKey.DIVIDE -> cap.copy(shiftedLabel = "(")
                PocketKey.COLON -> cap.copy(key = PocketKey.SEMICOLON, primaryLabel = ";", shiftedLabel = ")")
                else -> cap
            }
        },
        Pc1350KeyboardLayout.rows[2].filterNot { it.key == PocketKey.SEMICOLON }.map { cap ->
            if (cap.key == PocketKey.MULTIPLY) cap.copy(shiftedLabel = "¥") else cap
        },
        Pc1350KeyboardLayout.rows[3].filterNot { it.key == PocketKey.COMMA }.map { cap ->
            if (cap.key == PocketKey.KANA) {
                cap.copy(key = PocketKey.SMALL, primaryLabel = "SML", shiftedLabel = null)
            } else {
                cap
            }
        },
    )

    private fun cap(
        key: PocketKey,
        label: String,
        column: Int,
        span: Int = 1,
        shifted: String? = null,
    ): PocketKeyCap = PocketKeyCap(key, label, shiftedLabel = shifted, column = column, columnSpan = span)
}
