package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.Pc1350KeyboardLayout

/** Geometry in the existing 1429 × 603 illustration, with symbols beside the centered LCD. */
internal object Pc1350Skin {
    val definition = SkinDefinition(
        id = PlayerSkinCatalog.PC1350_SKIN_ID,
        machineIds = setOf(MachineId("pc-1350")),
        designSize = SkinSize(1429f, 603f),
        fullDeviceRegion = SkinRect(0f, 0f, 1429f, 603f),
        controllerDisplayRegion = SkinRect(0f, 0f, 968f, 337f),
        lcdPanelRegion = SkinRect(34f, 44f, 897f, 252f),
        // Core supplies all 150 × 32 dots, including each character's sixth column/eighth row.
        lcdContentRegion = SkinRect(132f, 86.8f, 780f, 166.4f),
        statusRegions = listOf(
            DisplaySymbol.RUN, DisplaySymbol.PRO, DisplaySymbol.KANA,
            DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF,
        ).mapIndexed { index, symbol ->
            SkinStatusRegion(symbol, SkinRect(48f, 86.8f + index * ((166.4f - 26f) / 5f), 72f, 26f))
        },
        keyRegions = buildList {
            val controls = mapOf(
                PocketKey.DOWN to SkinRect(982f, 105f, 68f, 49f),
                PocketKey.UP to SkinRect(1067f, 105f, 68f, 49f),
                PocketKey.MODE to SkinRect(1163f, 105f, 68f, 49f),
                PocketKey.BREAK to SkinRect(1252f, 105f, 68f, 49f),
                PocketKey.CLEAR to SkinRect(1339f, 105f, 68f, 49f),
                PocketKey.LEFT to SkinRect(982f, 191f, 68f, 51f),
                PocketKey.RIGHT to SkinRect(1067f, 191f, 68f, 51f),
                PocketKey.DELETE to SkinRect(1163f, 191f, 68f, 51f),
                PocketKey.INSERT to SkinRect(1252f, 191f, 68f, 51f),
                PocketKey.SHIFT to SkinRect(1339f, 191f, 68f, 51f),
            )
            Pc1350KeyboardLayout.rows.forEachIndexed { row, caps ->
                caps.forEach { cap ->
                    val bounds = when {
                        row == 0 && cap.column <= 10 -> controls.getValue(cap.key)
                        cap.column >= 11 -> SkinRect(
                            listOf(976f, 1066f, 1156f, 1246f, 1336f)[cap.column - 11],
                            listOf(275f, 364f, 453f, 541f)[row], 77f, 60f,
                        )
                        else -> SkinRect(
                            listOf(11f, 99f, 186f, 273f, 361f, 447f, 535f, 623f, 710f, 798f, 885f)[cap.column],
                            listOf(369f, 457f, 545f)[row - 1],
                            if (cap.key == PocketKey.ENTER) 159f else 68f, 49f,
                        )
                    }
                    add(SkinKeyRegion(cap.key, bounds, cap.id))
                }
            }
        },
    )
}
