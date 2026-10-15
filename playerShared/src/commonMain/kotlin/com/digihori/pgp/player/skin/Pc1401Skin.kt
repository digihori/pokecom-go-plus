package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.Pc1401KeyboardLayout

/** Coordinates measured from the existing 1280 × 548 PC-1401 illustration. */
internal object Pc1401Skin {
    val definition = SkinDefinition(
        id = PlayerSkinCatalog.PC1401_SKIN_ID,
        machineIds = setOf(MachineId("pc-1401")),
        designSize = SkinSize(1280f, 548f),
        fullDeviceRegion = SkinRect(0f, 0f, 1280f, 548f),
        controllerDisplayRegion = SkinRect(0f, 0f, 900f, 192f),
        lcdPanelRegion = SkinRect(36f, 30f, 824f, 132f),
        lcdContentRegion = SkinRect(60f, 68f, 760f, 56f),
        // CAL/RUN/PRO come from the ROM's LCD symbols, never a host-generated label.
        // Relative positions match the supplied LCD guide: two symbol rows around the dots.
        statusRegions = listOf(
            SkinStatusRegion(DisplaySymbol.BUSY, SkinRect(60f, 36f, 48f, 20f)),
            SkinStatusRegion(DisplaySymbol.DEF, SkinRect(114f, 36f, 38f, 20f)),
            SkinStatusRegion(DisplaySymbol.SHIFT, SkinRect(296f, 36f, 52f, 20f)),
            SkinStatusRegion(DisplaySymbol.HYP, SkinRect(350f, 36f, 34f, 20f)),
            SkinStatusRegion(DisplaySymbol.DE, SkinRect(560f, 36f, 22f, 20f)),
            SkinStatusRegion(DisplaySymbol.G, SkinRect(582f, 36f, 12f, 20f)),
            SkinStatusRegion(DisplaySymbol.RAD, SkinRect(594f, 36f, 36f, 20f)),
            SkinStatusRegion(DisplaySymbol.PARENTHESES, SkinRect(769f, 36f, 22f, 20f)),
            SkinStatusRegion(DisplaySymbol.MEMORY, SkinRect(792f, 36f, 14f, 20f)),
            SkinStatusRegion(DisplaySymbol.ERROR, SkinRect(809f, 36f, 11f, 20f)),
            SkinStatusRegion(DisplaySymbol.CAL, SkinRect(60f, 132f, 34f, 20f)),
            SkinStatusRegion(DisplaySymbol.RUN, SkinRect(100f, 132f, 34f, 20f)),
            SkinStatusRegion(DisplaySymbol.PRO, SkinRect(140f, 132f, 34f, 20f)),
            SkinStatusRegion(DisplaySymbol.STAT, SkinRect(664f, 132f, 46f, 20f)),
            SkinStatusRegion(DisplaySymbol.PRINT, SkinRect(714f, 132f, 54f, 20f)),
        ),
        keyRegions = buildList {
            val letterX = listOf(8f, 92f, 177f, 263f, 347f, 432f, 517f, 602f, 689f, 773f)
            val numberX = listOf(853f, 938f, 1023f, 1108f, 1193f)
            val rowsY = listOf(218f, 305f, 397f, 484f)
            Pc1401KeyboardLayout.keyboard.rows.forEachIndexed { row, caps ->
                caps.forEach { cap ->
                    val numeric = cap.column >= 10
                    val x = when {
                        cap.key == PocketKey.SHIFT -> 773f
                        numeric -> numberX[cap.column - 10]
                        else -> letterX[cap.column]
                    }
                    val y = if (numeric) listOf(210f, 302f, 391f, 478f)[row] else rowsY[row]
                    val width = when {
                        numeric -> 79f
                        cap.key == PocketKey.ENTER -> 154f
                        else -> 70f
                    }
                    add(SkinKeyRegion(cap.key, SkinRect(x, y, width, if (numeric) 62f else 52f)))
                }
            }
            Pc1401KeyboardLayout.functionPanel.rows.forEachIndexed { row, caps ->
                caps.forEach { cap ->
                    add(SkinKeyRegion(cap.key, SkinRect(
                        listOf(910f, 973f, 1035f, 1099f, 1161f, 1223f)[cap.column],
                        listOf(25f, 87f, 147f)[row], 47f, 33f,
                    )))
                }
            }
        },
    )
}
