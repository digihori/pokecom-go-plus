package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.rom.MachineId

/** 24×1 cells, physical pitch 3:4; supplied 1362×571 illustration. */
internal object Pc1460Skin {
    val definition = Pc1450Skin.definition.let { base -> base.copy(
        id = PlayerSkinCatalog.PC1460_SKIN_ID, machineIds = setOf(MachineId("pc-1460")),
        designSize = SkinSize(1362f, 571f), fullDeviceRegion = SkinRect(0f, 0f, 1362f, 571f),
        playableRegion = SkinRect(0f, 0f, 1362f, 571f), landscapeRegion = SkinRect(0f, 0f, 1362f, 571f),
        lcdPanelRegion = SkinRect(20f, 42f, 893f, 140f),
        lcdContentRegion = SkinRect(44f, 84f, 836f, 836f * 7f * 4f / (143f * 3f)),
        keyRegions = base.keyRegions.map { region -> region.copy(bounds = region.bounds.copy(
            y = region.bounds.y - if (region.bounds.y > 215f) 4f else 0f)) },
        statusRegions = listOf(
            top(DisplaySymbol.BUSY, 44f, 70f), top(DisplaySymbol.DEF, 161f, 44f),
            top(DisplaySymbol.SHIFT, 215f, 76f), top(DisplaySymbol.KANA, 325f, 50f),
            top(DisplaySymbol.KANA_SMALL, 381f, 25f), top(DisplaySymbol.SMALL, 411f, 52f),
            top(DisplaySymbol.DE, 497f, 31f), top(DisplaySymbol.G, 531f, 16f),
            top(DisplaySymbol.RAD, 551f, 50f), top(DisplaySymbol.HYP, 637f, 48f),
            top(DisplaySymbol.PARENTHESES, 700f, 28f), top(DisplaySymbol.MEMORY, 732f, 22f),
            top(DisplaySymbol.ERROR, 762f, 17f),
            bottom(DisplaySymbol.CAL, 44f, 46f), bottom(DisplaySymbol.RUN, 98f, 48f),
            bottom(DisplaySymbol.PRO, 158f, 48f), bottom(DisplaySymbol.MATRIX, 600f, 90f),
            bottom(DisplaySymbol.STAT, 700f, 60f), bottom(DisplaySymbol.PRINT, 772f, 76f),
        ),
    ) }
    private fun top(s: DisplaySymbol, x: Float, w: Float) = SkinStatusRegion(s, SkinRect(x, 49f, w, 24f))
    private fun bottom(s: DisplaySymbol, x: Float, w: Float) = SkinStatusRegion(s, SkinRect(x, 151f, w, 24f))
}
