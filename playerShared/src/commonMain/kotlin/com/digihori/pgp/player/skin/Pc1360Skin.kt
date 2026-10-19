package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.Pc1360KeyboardLayout

/** Coordinates from the bundled illustration, independently checked against PC-1350. */
internal object Pc1360Skin {
    val definition = SkinDefinition(
        id = PlayerSkinCatalog.PC1360_SKIN_ID,
        machineIds = setOf(MachineId("pc-1360")),
        designSize = SkinSize(1426f, 615f),
        fullDeviceRegion = SkinRect(0f, 0f, 1426f, 615f),
        controllerDisplayRegion = SkinRect(0f, 0f, 963f, 339f),
        lcdPanelRegion = SkinRect(34f, 44f, 897f, 252f),
        lcdContentRegion = FourLinePlayerLcdLayout.content,
        statusRegions = FourLinePlayerLcdLayout.symbols(),
        keyRegions = Pc1360KeyboardLayout.rows.flatMapIndexed { row, caps -> caps.map { cap ->
            val controls = mapOf(
                PocketKey.MODE to SkinRect(1250f, 20f, 71f, 53f),
                PocketKey.CLEAR to SkinRect(1343f, 20f, 71f, 53f),
                PocketKey.KANA to SkinRect(970f, 113f, 71f, 52f),
                PocketKey.DELETE to SkinRect(1249f, 112f, 71f, 53f),
                PocketKey.INSERT to SkinRect(1342f, 111f, 71f, 53f),
                PocketKey.DOWN to SkinRect(969f, 201f, 71f, 52f),
                PocketKey.UP to SkinRect(1064f, 200f, 71f, 53f),
                PocketKey.LEFT to SkinRect(1157f, 200f, 71f, 53f),
                PocketKey.RIGHT to SkinRect(1246f, 200f, 71f, 53f),
                PocketKey.BREAK to SkinRect(1342f, 200f, 71f, 53f),
            )
            val bounds = when {
                row == 0 && cap.column <= 10 -> controls.getValue(cap.key)
                cap.column >= 11 -> SkinRect(
                    listOf(968f, 1061f, 1154f, 1247f, 1340f)[cap.column - 11],
                    listOf(286f, 378f, 467f, 554f)[row], 78f, 61f)
                else -> SkinRect(
                    listOf(8f, 96f, 183f, 269f, 357f, 445f, 533f, 621f, 709f, 798f, 886f)[cap.column],
                    listOf(380f, 469f, 558f)[row - 1],
                    if (cap.key == PocketKey.ENTER) 158f else 70f, 52f)
            }
            SkinKeyRegion(cap.key, bounds, cap.id)
        } },
    )
}
