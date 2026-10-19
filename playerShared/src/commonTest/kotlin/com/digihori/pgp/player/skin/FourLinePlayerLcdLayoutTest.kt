package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class FourLinePlayerLcdLayoutTest {
    @Test fun allThreeLcdsAreCenteredAndSymbolsFormACompactLowerLeftRow() {
        for (id in listOf("pc-1350", "pc-1360", "pc-1360k")) {
            val skin = assertNotNull(PlayerSkinCatalog.forMachine(MachineId(id)))
            val lcd = skin.lcdContentRegion
            val panel = skin.lcdPanelRegion
            assertEquals(857f, lcd.width)
            assertEquals(150f / 32f, lcd.width / lcd.height, 0.001f)
            assertEquals(panel.x + panel.width / 2, lcd.x + lcd.width / 2, 0.001f)
            assertEquals(panel.y + panel.height / 2, lcd.y + lcd.height / 2, 0.001f)
            val symbols = skin.statusRegions
            assertEquals(lcd.x, symbols.first().bounds.x)
            symbols.forEach {
                assertTrue(it.bounds.y > lcd.bottom)
                assertTrue(it.bounds.bottom <= panel.bottom)
                assertEquals(symbols.first().bounds.y, it.bounds.y)
            }
            symbols.zipWithNext().forEach { (left, right) ->
                assertEquals(16f, right.bounds.x - left.bounds.right)
            }
            assertTrue(symbols.last().bounds.right < lcd.x + lcd.width * 0.7f)
            val expected = listOf(DisplaySymbol.RUN, DisplaySymbol.PRO) +
                (if (id == "pc-1360k") listOf(DisplaySymbol.HIRAGANA, DisplaySymbol.KATAKANA, DisplaySymbol.S)
                else listOf(DisplaySymbol.KANA)) + listOf(DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF)
            assertEquals(expected, symbols.map { it.symbol })
        }
    }
}
