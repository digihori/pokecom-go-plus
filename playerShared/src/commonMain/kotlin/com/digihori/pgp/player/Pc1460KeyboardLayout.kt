package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey

/** Studio's 1460 legends, retaining the 1450 shared matrix. */
public object Pc1460KeyboardLayout {
    public val keyboard: PlayerKeyboard = Pc1450KeyboardLayout.keyboard.let { base ->
        base.copy(rows = base.rows.map { row -> row.map { cap -> when (cap.key) {
            PocketKey.LOCK -> cap.copy(primaryLabel = "LOCK")
            PocketKey.UP, PocketKey.DOWN -> cap.copy(shiftedLabel = "MATRIX")
            else -> cap
        } } }, functionPanel = base.functionPanel!!.let { panel -> panel.copy(rows = panel.rows.map { row ->
            row.map { cap -> if (cap.key == PocketKey.FORMAT) cap.copy(primaryLabel = "FSE") else cap }
        }) })
    }
}
