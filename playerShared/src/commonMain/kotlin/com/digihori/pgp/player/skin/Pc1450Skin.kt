package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.Pc1450KeyboardLayout

/** Supplied 1361×575 skin; symbols follow the user's PC-1450 LCD guide. */
internal object Pc1450Skin {
    val definition = SkinDefinition(
        id = PlayerSkinCatalog.PC1450_SKIN_ID,
        machineIds = setOf(MachineId("pc-1450")),
        designSize = SkinSize(1361f, 575f),
        fullDeviceRegion = SkinRect(0f, 0f, 1361f, 575f),
        controllerDisplayRegion = SkinRect(0f, 0f, 932f, 216f),
        lcdPanelRegion = SkinRect(22f, 44f, 893f, 140f),
        lcdContentRegion = SkinRect(46f, 85f, 836f, 61.6f),
        statusRegions = listOf(
            top(DisplaySymbol.BUSY, 46f, 80f), top(DisplaySymbol.DEF, 187f, 52f),
            top(DisplaySymbol.SHIFT, 250f, 85f), top(DisplaySymbol.KANA, 376f, 52f),
            top(DisplaySymbol.KANA_SMALL, 437f, 26f), top(DisplaySymbol.SMALL, 473f, 58f),
            top(DisplaySymbol.DE, 570f, 34f), top(DisplaySymbol.G, 610f, 18f),
            top(DisplaySymbol.RAD, 630f, 58f), top(DisplaySymbol.HYP, 726f, 52f),
            top(DisplaySymbol.PARENTHESES, 797f, 30f), top(DisplaySymbol.MEMORY, 833f, 24f),
            top(DisplaySymbol.ERROR, 868f, 18f),
            bottom(DisplaySymbol.CAL, 46f, 50f), bottom(DisplaySymbol.RUN, 108f, 54f),
            bottom(DisplaySymbol.PRO, 176f, 54f), bottom(DisplaySymbol.STAT, 283f, 68f),
            bottom(DisplaySymbol.PRINT, 373f, 82f),
        ),
        keyRegions = buildList {
            val keyboard = Pc1450KeyboardLayout.keyboard
            keyboard.rows.forEachIndexed { row, caps -> caps.forEach { cap ->
                val numeric = cap.column >= 11
                val x = if (numeric) listOf(936f, 1020f, 1106f, 1191f, 1276f)[cap.column - 11]
                    else listOf(6f, 90f, 175f, 260f, 345f, 430f, 515f, 600f, 685f, 770f, 855f)[cap.column]
                val y = if (numeric) listOf(237f, 330f, 418f, 506f)[row]
                    else listOf(246f, 333f, 424f, 512f)[row]
                add(SkinKeyRegion(cap.key, SkinRect(x, y,
                    if (cap.columnSpan == 2) 155f else if (numeric) 78f else 69f,
                    if (numeric) 61f else 50f), cap.id))
            } }
            keyboard.functionPanel!!.rows.forEachIndexed { row, caps -> caps.forEach { cap ->
                add(SkinKeyRegion(cap.key, SkinRect(listOf(941f, 1013f, 1084f, 1155f, 1226f, 1296f)[cap.column],
                    listOf(28f, 98f, 167f)[row], 53f, 37f), cap.id))
            } }
        },
    )
    private fun top(symbol: DisplaySymbol, x: Float, width: Float) = SkinStatusRegion(symbol, SkinRect(x, 51f, width, 24f))
    private fun bottom(symbol: DisplaySymbol, x: Float, width: Float) = SkinStatusRegion(symbol, SkinRect(x, 153f, width, 24f))
}
