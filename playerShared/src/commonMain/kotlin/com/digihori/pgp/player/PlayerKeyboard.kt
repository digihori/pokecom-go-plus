package com.digihori.pgp.player

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

public data class PlayerKeyCap(
    public val key: PocketKey,
    public val primaryLabel: String,
    public val shiftedLabel: String? = null,
    public val basicLabel: String? = null,
    public val column: Int,
    public val columnSpan: Int = 1,
)

public data class PlayerKeyboard(
    public val columnCount: Int,
    public val rows: List<List<PlayerKeyCap>>,
) {
    init {
        require(columnCount > 0)
        require(rows.isNotEmpty())
        rows.flatten().forEach { cap ->
            require(cap.primaryLabel.isNotBlank())
            require(cap.column >= 0 && cap.columnSpan > 0)
            require(cap.column + cap.columnSpan <= columnCount)
        }
        rows.forEach { row ->
            row.sortedBy(PlayerKeyCap::column).zipWithNext().forEach { (left, right) ->
                require(left.column + left.columnSpan <= right.column) { "Keyboard keys overlap" }
            }
        }
    }
}

public object PlayerKeyboardCatalog {
    public fun forMachine(machineId: MachineId): PlayerKeyboard? =
        when (MachineCatalog.require(machineId).keyboardLayout) {
            MachineKeyboardLayout.PC_1245 -> pc1245
            MachineKeyboardLayout.PC_1251,
            MachineKeyboardLayout.PC_1350,
            MachineKeyboardLayout.PC_1360,
            -> null
        }

    private val pc1245 = PlayerKeyboard(
        columnCount = 14,
        rows = listOf(
            listOf(
                cap(PocketKey.DEF, "DEF", 3), cap(PocketKey.SHIFT, "SHIFT", 4),
                cap(PocketKey.DOWN, "↓", 5), cap(PocketKey.UP, "↑", 6),
                cap(PocketKey.LEFT, "←", 7, shifted = "DEL"),
                cap(PocketKey.RIGHT, "→", 8, shifted = "INS"),
                cap(PocketKey.BREAK, "BRK", 9, shifted = "ON"),
                cap(PocketKey.NUM_7, "7", 10), cap(PocketKey.NUM_8, "8", 11),
                cap(PocketKey.NUM_9, "9", 12), cap(PocketKey.CLEAR, "CL", 13, shifted = "CA"),
            ),
            listOf(
                cap(PocketKey.Q, "Q", 0, shifted = "!"), cap(PocketKey.W, "W", 1, shifted = "\""),
                cap(PocketKey.E, "E", 2, shifted = "#"), cap(PocketKey.R, "R", 3, shifted = "\$"),
                cap(PocketKey.T, "T", 4, shifted = "%"), cap(PocketKey.Y, "Y", 5, shifted = "&"),
                cap(PocketKey.U, "U", 6, shifted = "?"), cap(PocketKey.I, "I", 7, shifted = ":"),
                cap(PocketKey.O, "O", 8, shifted = ","), cap(PocketKey.P, "P", 9, shifted = ";"),
                cap(PocketKey.NUM_4, "4", 10), cap(PocketKey.NUM_5, "5", 11),
                cap(PocketKey.NUM_6, "6", 12, shifted = "¥"),
                cap(PocketKey.DIVIDE, "÷", 13, shifted = "^"),
            ),
            listOf(
                cap(PocketKey.A, "A", 0, basic = "INPUT"), cap(PocketKey.S, "S", 1, basic = "IF"),
                cap(PocketKey.D, "D", 2, basic = "THEN"), cap(PocketKey.F, "F", 3, basic = "GOTO"),
                cap(PocketKey.G, "G", 4, basic = "FOR"), cap(PocketKey.H, "H", 5, basic = "TO"),
                cap(PocketKey.J, "J", 6, basic = "STEP"), cap(PocketKey.K, "K", 7, basic = "NEXT"),
                cap(PocketKey.L, "L", 8, basic = "LIST"),
                cap(PocketKey.EQUALS, "=", 9, basic = "RUN"),
                cap(PocketKey.NUM_1, "1", 10, shifted = "("),
                cap(PocketKey.NUM_2, "2", 11, shifted = ")"),
                cap(PocketKey.NUM_3, "3", 12, shifted = "@"),
                cap(PocketKey.MULTIPLY, "×", 13, shifted = "<"),
            ),
            listOf(
                cap(PocketKey.Z, "Z", 0, basic = "PRINT"),
                cap(PocketKey.X, "X", 1, basic = "USING"),
                cap(PocketKey.C, "C", 2, basic = "GOSUB"),
                cap(PocketKey.V, "V", 3, basic = "RETURN"),
                cap(PocketKey.B, "B", 4, basic = "DIM"), cap(PocketKey.N, "N", 5, basic = "END"),
                cap(PocketKey.M, "M", 6, basic = "CSAVE"),
                cap(PocketKey.SPACE, "SPACE", 7, basic = "CLOAD"),
                cap(PocketKey.ENTER, "ENTER", 8, span = 2, basic = "N↔NP"),
                cap(PocketKey.NUM_0, "0", 10, shifted = "π"),
                cap(PocketKey.DOT, ".", 11, shifted = "√"),
                cap(PocketKey.PLUS, "+", 12, shifted = "EXP"),
                cap(PocketKey.MINUS, "−", 13, shifted = ">"),
            ),
        ),
    )

    private fun cap(
        key: PocketKey,
        label: String,
        column: Int,
        span: Int = 1,
        shifted: String? = null,
        basic: String? = null,
    ): PlayerKeyCap = PlayerKeyCap(key, label, shifted, basic, column, span)
}
