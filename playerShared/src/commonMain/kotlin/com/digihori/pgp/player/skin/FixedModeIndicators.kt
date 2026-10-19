package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol

/** Optional host presentation: fixed mode positions, retaining the original skin for other hosts. */
public fun SkinDefinition.withFixedModeIndicators(): SkinDefinition {
    val band = operatingModeRegion ?: return this
    val modes = when (id) {
        PlayerSkinCatalog.PC1245_SKIN_ID -> listOf(DisplaySymbol.RUN, DisplaySymbol.PRO)
        PlayerSkinCatalog.PC1251_SKIN_ID -> listOf(DisplaySymbol.RUN, DisplaySymbol.PRO, DisplaySymbol.RESERVE)
        else -> return this
    }
    return copy(
        statusRegions = statusRegions + modes.mapIndexed { index, symbol ->
            SkinStatusRegion(symbol, SkinRect(band.x + index * 65f, band.y, 60f, band.height))
        },
        operatingModeRegion = null,
    )
}
