package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Studio's 1470U scan identities with supplied skin legends; two physical SHIFT owners. */
public object Pc1470uKeyboardLayout {
    public val keyboard: PlayerKeyboard = Pc1460KeyboardLayout.keyboard.let { base -> base.copy(
        rows = base.rows.map { row -> row.map { cap -> when (cap.key) {
            PocketKey.MEMORY_STORE -> cap.copy(key = PocketKey.DELETE, primaryLabel = "DEL", shiftedLabel = "X↔M")
            PocketKey.MEMORY_RECALL -> cap.copy(key = PocketKey.BACKSPACE, primaryLabel = "BS", shiftedLabel = "RM")
            PocketKey.MEMORY_ADD -> cap.copy(key = PocketKey.INSERT, primaryLabel = "INS", shiftedLabel = "M+")
            PocketKey.CAL -> cap.copy(primaryLabel = "AER", shiftedLabel = "CAL")
            PocketKey.DIVIDE -> cap.copy(shiftedLabel = "DRG")
            PocketKey.DOT -> cap.copy(shiftedLabel = null)
            PocketKey.EQUALS -> cap.copy(shiftedLabel = "STAT")
            PocketKey.LOCK -> cap.copy(key = PocketKey.SHIFT, primaryLabel = "SHIFT", shiftedLabel = null)
            PocketKey.LEFT -> cap.copy(shiftedLabel = "|←")
            PocketKey.RIGHT -> cap.copy(shiftedLabel = "→|")
            else -> cap
        } } },
        functionPanel = base.functionPanel!!.let { panel -> panel.copy(rows = panel.rows.map { row -> row.map { cap ->
            if (cap.key == PocketKey.STAT) cap.copy(key = PocketKey.TITLE, primaryLabel = "↕︎", shiftedLabel = "TITLE") else cap
        } }) },
    ) }
}
