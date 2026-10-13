package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.player.PlayerKeyCap
import com.digihori.pgp.player.PlayerKeyboard
import com.digihori.pgp.player.Pc1401KeyboardLayout

internal object Pc14xxKeyboardLayout {
    fun forLayout(layout: com.digihori.pgp.core.api.MachineKeyboardLayout): PlayerKeyboard? = when (layout) {
        com.digihori.pgp.core.api.MachineKeyboardLayout.PC_1450 -> pc1450
        com.digihori.pgp.core.api.MachineKeyboardLayout.PC_1460 -> pc1460
        com.digihori.pgp.core.api.MachineKeyboardLayout.PC_1470U -> pc1470u
        else -> null
    }

    val pc1450: PlayerKeyboard = Pc1401KeyboardLayout.keyboard.let { base ->
        base.copy(columnCount = 16, rows = listOf(
            listOf(PocketKey.CAL, PocketKey.BASIC, PocketKey.BREAK, PocketKey.KANA, PocketKey.DAKUTEN,
                PocketKey.LONG_VOWEL, PocketKey.DOWN, PocketKey.UP, PocketKey.LEFT, PocketKey.RIGHT,
                PocketKey.SHIFT, PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9, PocketKey.DIVIDE, PocketKey.MEMORY_STORE)
                .mapIndexed { column, key -> PlayerKeyCap(key, label(key), column = column) },
        ) + base.rows.drop(1).mapIndexed { row, caps ->
            listOf(PlayerKeyCap(listOf(PocketKey.LOCK, PocketKey.DEF, PocketKey.SMALL)[row],
                label(listOf(PocketKey.LOCK, PocketKey.DEF, PocketKey.SMALL)[row]), column = 0)) +
                caps.map { it.copy(column = it.column + 1, shiftedLabel = if (it.key == PocketKey.MEMORY_ADD) null else it.shiftedLabel) }
        })
    }
    val pc1460: PlayerKeyboard = pc1450.copy(rows = pc1450.rows.map { row -> row.map { cap ->
        if (cap.key in setOf(PocketKey.UP, PocketKey.DOWN)) cap.copy(shiftedLabel = "MATRIX") else cap
    } }, functionPanel = Pc1401KeyboardLayout.functionPanel.copy(
        rows = Pc1401KeyboardLayout.functionPanel.rows.map { row -> row.map { cap ->
            if (cap.key == PocketKey.FORMAT) cap.copy(primaryLabel = "FSE") else cap
        } }
    ))
    val pc1470u: PlayerKeyboard = pc1460.copy(rows = pc1460.rows.map { row -> row.map { cap ->
        when (cap.key) {
            PocketKey.MEMORY_STORE -> cap.copy(key = PocketKey.DELETE, primaryLabel = "DEL", shiftedLabel = "X↔M")
            PocketKey.MEMORY_RECALL -> cap.copy(key = PocketKey.BACKSPACE, primaryLabel = "BS", shiftedLabel = "RM")
            PocketKey.MEMORY_ADD -> cap.copy(key = PocketKey.INSERT, primaryLabel = "INS", shiftedLabel = "M+")
            PocketKey.CAL -> cap.copy(primaryLabel = "AER", shiftedLabel = "CAL")
            PocketKey.DIVIDE -> cap.copy(shiftedLabel = "DRG")
            PocketKey.EQUALS -> cap.copy(shiftedLabel = "STAT")
            PocketKey.LOCK -> cap.copy(key = PocketKey.SHIFT, primaryLabel = "SHIFT", shiftedLabel = null)
            else -> cap
        }
    } }, functionPanel = checkNotNull(pc1460.functionPanel).let { panel ->
        panel.copy(rows = panel.rows.map { row -> row.map { cap ->
            if (cap.key == PocketKey.STAT) cap.copy(key = PocketKey.TITLE, primaryLabel = "↕︎", shiftedLabel = "TITLE") else cap
        } })
    })
    private fun label(key: PocketKey): String = when (key) {
        PocketKey.BREAK -> "BRK"; PocketKey.KANA -> "カナ"; PocketKey.DAKUTEN -> "゛"
        PocketKey.LONG_VOWEL -> "ー"; PocketKey.SMALL -> "SML"; PocketKey.DOWN -> "↓"; PocketKey.UP -> "↑"
        PocketKey.LEFT -> "←"; PocketKey.RIGHT -> "→"; PocketKey.MEMORY_STORE -> "X↔M"
        PocketKey.DIVIDE -> "÷"; PocketKey.NUM_7 -> "7"; PocketKey.NUM_8 -> "8"; PocketKey.NUM_9 -> "9"
        else -> key.name
    }
}
