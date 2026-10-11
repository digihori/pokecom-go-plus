package com.digihori.pgp.player

import com.digihori.pgp.core.api.DisplaySymbol.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LcdSymbolTextTest {
    @Test fun angleSegmentsFollowPhysicalOrderWithoutSpaces() {
        assertEquals("CAL  DEG", lcdSymbolText(listOf(CAL, G, DE)))
        assertEquals("GRAD  RUN", lcdSymbolText(listOf(RAD, G, RUN)))
        assertEquals("RAD", lcdSymbolText(listOf(RAD)))
        assertEquals("DEGRAD", lcdSymbolText(listOf(RAD, G, DE)))
        assertEquals("", lcdSymbolText(emptyList()))
    }

    @Test fun otherSymbolsAndDedicatedDegreeIndicatorKeepTheirLabels() {
        assertEquals("SHIFT  DEG  RUN", lcdSymbolText(listOf(SHIFT, DE, G, RUN)))
        assertEquals("DEG  RAD", lcdSymbolText(listOf(DEG, RAD)))
        assertEquals("M  DEG", lcdSymbolText(listOf(MEMORY, G, DE)) { if (it == MEMORY) "M" else it.name })
    }
}
