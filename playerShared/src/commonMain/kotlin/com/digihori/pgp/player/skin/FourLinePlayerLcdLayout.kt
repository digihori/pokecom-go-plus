package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol

/** Approved Player layout: center the LCD itself, then group indicators at the lower left. */
internal object FourLinePlayerLcdLayout {
    val content = SkinRect(54f, 44f + (252f - 857f * 32f / 150f) / 2f, 857f, 857f * 32f / 150f)

    fun symbols(kanji: Boolean = false): List<SkinStatusRegion> {
        val labels = listOf(DisplaySymbol.RUN to 58f, DisplaySymbol.PRO to 58f) +
            (if (kanji) listOf(DisplaySymbol.HIRAGANA to 30f, DisplaySymbol.KATAKANA to 30f,
                DisplaySymbol.S to 24f) else listOf(DisplaySymbol.KANA to 50f)) +
            listOf(DisplaySymbol.SMALL to 60f, DisplaySymbol.SHIFT to 80f, DisplaySymbol.DEF to 52f)
        var x = content.x
        return labels.map { (symbol, width) ->
            SkinStatusRegion(symbol, SkinRect(x, 270f, width, 24f)).also { x += width + 16f }
        }
    }
}
