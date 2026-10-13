package com.digihori.pgp.desktop.display

import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.rom.MachineId

/** Physical presentation is independent of Core's logical 5x7 dot cells. */
internal object DesktopLcdGeometry {
    fun panelAspectRatio(machineId: MachineId, snapshot: DisplaySnapshot): Float {
        val columns = CharacterCellGeometry.visualColumnCount(snapshot.characterColumns,
            snapshot.characterWidth, snapshot.interCharacterColumnGap)
        val rows = CharacterCellGeometry.visualRowCount(snapshot.characterRows,
            snapshot.characterHeight, snapshot.interCharacterRowGap)
        return when (machineId.value) {
            // MainLoop1460 uses horizontal step 3 and vertical step 4.
            "pc-1460" -> columns * 0.75f / rows
            else -> columns.toFloat() / rows
        }
    }
}
