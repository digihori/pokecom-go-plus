package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.rom.MachineId

/** Bundled image key geometry matches PC-1251; the two-line LCD/status placement awaits review. */
internal object Pc1261Skin {
    val definition = Pc1251Skin.definition.copy(
        id = PlayerSkinCatalog.PC1261_SKIN_ID,
        machineIds = setOf(MachineId("pc-1261")),
        designSize = SkinSize(1206f, 600f),
        fullDeviceRegion = SkinRect(0f, 0f, 1206f, 600f),
        playableRegion = SkinRect(0f, 0f, 1206f, 600f),
        landscapeRegion = SkinRect(0f, 0f, 1206f, 600f),
        lcdPanelRegion = SkinRect(49f, 46f, 1106f, 164f),
        // 143 columns and 15 rows include one dot of spacing between cells/lines.
        lcdContentRegion = SkinRect(70f, 73.5f, 1072.5f, 112.5f),
        statusRegions = listOf(
            DisplaySymbol.BUSY to SkinRect(70f, 49f, 70f, 24f),
            DisplaySymbol.DEF to SkinRect(147f, 49f, 56f, 24f),
            DisplaySymbol.SHIFT to SkinRect(209f, 49f, 90f, 24f),
            DisplaySymbol.SMALL to SkinRect(352f, 49f, 56f, 24f),
            DisplaySymbol.KANA to SkinRect(414f, 49f, 66f, 24f),
            DisplaySymbol.RUN to SkinRect(70f, 186f, 60f, 24f),
            DisplaySymbol.PRO to SkinRect(135f, 186f, 60f, 24f),
            DisplaySymbol.RESERVE to SkinRect(200f, 186f, 60f, 24f),
            DisplaySymbol.PRINT to SkinRect(352f, 186f, 90f, 24f),
            DisplaySymbol.DEG to SkinRect(453f, 186f, 48f, 24f),
            DisplaySymbol.RAD to SkinRect(501f, 186f, 48f, 24f),
            DisplaySymbol.ERROR to SkinRect(577f, 186f, 105f, 24f),
        ).map { (symbol, bounds) -> SkinStatusRegion(symbol, bounds) },
        operatingModeRegion = null,

    )
}
