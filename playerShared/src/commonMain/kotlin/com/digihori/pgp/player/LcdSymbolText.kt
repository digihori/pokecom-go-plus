package com.digihori.pgp.player

import com.digihori.pgp.core.api.DisplaySymbol

/** DE, G and RAD are adjacent LCD segments, independent of register bit order. */
public fun lcdSymbolText(
    symbols: List<DisplaySymbol>,
    label: (DisplaySymbol) -> String = { it.name },
): String {
    val angleSegments = listOf(DisplaySymbol.DE, DisplaySymbol.G, DisplaySymbol.RAD)
    val angleText = angleSegments.filter { it in symbols }.joinToString("") { it.name }
    var angleEmitted = false
    return buildList {
        for (symbol in symbols) {
            if (symbol in angleSegments) {
                if (!angleEmitted) add(angleText)
                angleEmitted = true
            } else {
                add(label(symbol))
            }
        }
    }.joinToString("  ")
}
