package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey

internal data class Pc1245KeyCap(
    val key: PocketKey,
    val primaryLabel: String,
    val shiftedLabel: String? = null,
    val basicLabel: String? = null,
    val column: Int,
    val columnSpan: Int = 1,
)

internal object Pc1245KeyboardLayout {
    const val COLUMN_COUNT: Int = 14

    val rows: List<List<Pc1245KeyCap>> = listOf(
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
    ): Pc1245KeyCap = Pc1245KeyCap(key, primary, shifted, basic, column, span)
}
