package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.rom.MachineId

/** Two-line LCD layout follows the supplied PC-1261 panel illustration. */
internal object Pc1261Skin {
    val definition = Pc1251Skin.definition.copy(
        id = PlayerSkinCatalog.PC1261_SKIN_ID,
        machineIds = listOf("pc-1260", "pc-1261").map(::MachineId).toSet(),
        designSize = SkinSize(1206f, 600f),
        fullDeviceRegion = SkinRect(0f, 0f, 1206f, 600f),
        playableRegion = SkinRect(0f, 0f, 1206f, 600f),
        landscapeRegion = SkinRect(0f, 0f, 1206f, 600f),
        controllerDisplayRegion = SkinRect(30f, 26f, 1146f, 204f),
        lcdPanelRegion = SkinRect(49f, 41f, 1106f, 163f),
        // 143 columns and 15 rows include one dot of spacing between cells/lines.
        // Equal gaps above the dot area and below it, up to the segment top (185).
        lcdContentRegion = SkinRect(125f, 59.75f, 1015.3f, 106.5f),
        statusRegions = listOf(
            SkinStatusRegion(DisplaySymbol.KANA, SkinRect(57f, 48f, 60f, 22f)),
            SkinStatusRegion(DisplaySymbol.SMALL, SkinRect(57f, 82f, 60f, 22f)),
            SkinStatusRegion(DisplaySymbol.SHIFT, SkinRect(57f, 116f, 60f, 22f)),
            SkinStatusRegion(DisplaySymbol.DEF, SkinRect(57f, 150f, 60f, 22f)),
        ) + listOf(
            DisplaySymbol.BUSY to 135f,
            DisplaySymbol.PRINT to 248f,
            DisplaySymbol.DEG to 338f,
            DisplaySymbol.RAD to 425f,
            DisplaySymbol.ERROR to 642f,
            DisplaySymbol.UNLABELLED to 725f,
        ).map { (symbol, x) ->
            SkinStatusRegion(symbol, SkinRect(x, 185f, 28f, 10f), SkinStatusStyle.RECTANGLE)
        },
        operatingModeRegion = null,

    )
}
