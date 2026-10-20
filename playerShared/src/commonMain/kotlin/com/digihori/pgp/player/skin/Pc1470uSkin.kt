package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

/** Two LCD rows and 22 indicators follow the user's PC-1470U guide. */
internal object Pc1470uSkin {
    val definition = Pc1450Skin.definition.let { base -> base.copy(
        id = PlayerSkinCatalog.PC1470U_SKIN_ID, machineIds = setOf(MachineId("pc-1470u")),
        lcdPanelRegion = SkinRect(22f, 34f, 893f, 160f),
        lcdContentRegion = SkinRect(46f, 72f, 836f, 836f * 15f / 143f),
        keyRegions = base.keyRegions.map { region -> region.copy(key = when (region.key) {
            PocketKey.LOCK -> PocketKey.SHIFT
            PocketKey.MEMORY_STORE -> PocketKey.DELETE
            PocketKey.MEMORY_RECALL -> PocketKey.BACKSPACE
            PocketKey.MEMORY_ADD -> PocketKey.INSERT
            PocketKey.STAT -> PocketKey.TITLE
            else -> region.key
        }) },
        statusRegions = listOf(
            top(DisplaySymbol.BUSY, 46f, 64f), top(DisplaySymbol.DBL, 116f, 44f),
            top(DisplaySymbol.DEF, 170f, 44f), top(DisplaySymbol.SHIFT, 225f, 66f),
            top(DisplaySymbol.KANA, 337f, 44f), top(DisplaySymbol.KANA_SMALL, 388f, 23f),
            top(DisplaySymbol.SMALL, 418f, 48f), top(DisplaySymbol.DE, 501f, 31f),
            top(DisplaySymbol.G, 537f, 16f), top(DisplaySymbol.RAD, 558f, 50f),
            top(DisplaySymbol.HYP, 642f, 48f), top(DisplaySymbol.PARENTHESES, 706f, 28f),
            top(DisplaySymbol.MEMORY, 737f, 22f), top(DisplaySymbol.ERROR, 770f, 17f),
            top(DisplaySymbol.BATTERY, 803f, 68f),
            bottom(DisplaySymbol.CAL, 46f, 45f), bottom(DisplaySymbol.AER, 103f, 45f),
            bottom(DisplaySymbol.RUN, 158f, 48f), bottom(DisplaySymbol.PRO, 219f, 48f),
            bottom(DisplaySymbol.MATRIX, 610f, 90f), bottom(DisplaySymbol.STAT, 714f, 58f),
            bottom(DisplaySymbol.PRINT, 783f, 76f),
        ),
    ) }
    private fun top(s: DisplaySymbol, x: Float, w: Float) = SkinStatusRegion(s, SkinRect(x, 39f, w, 24f))
    private fun bottom(s: DisplaySymbol, x: Float, w: Float) = SkinStatusRegion(s, SkinRect(x, 165f, w, 24f))
}
