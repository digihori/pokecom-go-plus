package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.Pc1401KeyboardLayout
import com.digihori.pgp.player.PlayerMachineCatalog
import com.digihori.pgp.player.PresentationMode
import kotlin.test.*

class Pc1401SkinTest {
    private val skin = requireNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1401")))

    @Test fun illustrationHasEveryPhysicalKeyOnceWithoutOverlaps() {
        val keyboard = Pc1401KeyboardLayout.keyboard
        val expected = (keyboard.rows.flatten() + keyboard.functionPanel!!.rows.flatten()).map { it.key }
        assertEquals(SkinSize(1280f, 548f), skin.designSize)
        assertEquals(76, skin.keyRegions.size)
        assertEquals(expected.toSet(), skin.keyRegions.map { it.key }.toSet())
        skin.keyRegions.forEachIndexed { index, key ->
            assertTrue(key.bounds.x >= 0 && key.bounds.y >= 0)
            assertTrue(key.bounds.right <= 1280 && key.bounds.bottom <= 548)
            skin.keyRegions.drop(index + 1).forEach { other ->
                assertFalse(overlap(key.bounds, other.bounds), "${key.key} overlaps ${other.key}")
            }
            assertFalse(overlap(key.bounds, skin.lcdPanelRegion), key.key.name)
        }
        assertEquals(SkinRect(689f, 484f, 154f, 52f), skin.keyRegions.single { it.key == PocketKey.ENTER }.bounds)
        assertEquals(SkinRect(773f, 218f, 70f, 52f), skin.keyRegions.single { it.key == PocketKey.SHIFT }.bounds)
        assertEquals(SkinRect(910f, 25f, 47f, 33f), skin.keyRegions.single { it.key == PocketKey.HYP }.bounds)
    }

    @Test fun modesAreLcdSymbolsAndOnlyExplicitIntegrationTargetsAreEnabled() {
        assertNull(skin.operatingModeRegion)
        assertTrue(setOf(DisplaySymbol.CAL, DisplaySymbol.RUN, DisplaySymbol.PRO,
            DisplaySymbol.HYP, DisplaySymbol.STAT).all { symbol -> skin.statusRegions.any { it.symbol == symbol } })
        val regions = skin.statusRegions.map { it.bounds } + skin.lcdContentRegion
        regions.forEachIndexed { index, region ->
            assertTrue(region.x >= skin.lcdPanelRegion.x && region.y >= skin.lcdPanelRegion.y)
            assertTrue(region.right <= skin.lcdPanelRegion.right && region.bottom <= skin.lcdPanelRegion.bottom)
            regions.drop(index + 1).forEach { assertFalse(overlap(region, it)) }
        }
        assertEquals(listOf("pc-1245", "pc-1401"), PlayerMachineCatalog.definitions.map { it.id.value })
    }

    @Test fun referenceSymbolsSurroundDotsInTheRequestedGroups() {
        val dots = skin.lcdContentRegion
        assertEquals(skin.lcdPanelRegion.y + skin.lcdPanelRegion.height / 2, dots.y + dots.height / 2)
        val top = skin.statusRegions.filter { it.bounds.bottom <= dots.y }.sortedBy { it.bounds.x }
        val bottom = skin.statusRegions.filter { it.bounds.y >= dots.bottom }.sortedBy { it.bounds.x }
        assertEquals(listOf(DisplaySymbol.BUSY, DisplaySymbol.DEF, DisplaySymbol.SHIFT,
            DisplaySymbol.HYP, DisplaySymbol.DE, DisplaySymbol.G, DisplaySymbol.RAD,
            DisplaySymbol.PARENTHESES, DisplaySymbol.MEMORY, DisplaySymbol.ERROR), top.map { it.symbol })
        assertEquals(listOf(DisplaySymbol.CAL, DisplaySymbol.RUN, DisplaySymbol.PRO,
            DisplaySymbol.STAT, DisplaySymbol.PRINT), bottom.map { it.symbol })
        assertEquals(15, skin.statusRegions.map { it.symbol }.distinct().size)
        assertEquals(dots.x, top.first().bounds.x)
        assertEquals(dots.x, bottom.first().bounds.x)
        assertTrue(bottom.take(3).all { it.bounds.right < dots.x + dots.width * 0.2f })
        assertTrue(bottom.drop(3).all { it.bounds.x > dots.x + dots.width * 0.75f })
        val units = top.filter { it.symbol in setOf(DisplaySymbol.DE, DisplaySymbol.G, DisplaySymbol.RAD) }
        units.zipWithNext().forEach { (left, right) -> assertEquals(left.bounds.right, right.bounds.x) }
        assertEquals(dots.right, top.last().bounds.right)
    }

    @Test fun imageLcdAndTouchCoordinatesUseSameTransformWithSafeInsets() {
        for (mode in PresentationMode.entries) {
            for (size in listOf(SkinSize(960f, 432f), SkinSize(800f, 360f), SkinSize(432f, 960f))) {
                val inset = SkinInsets(24f, 8f, 16f, 24f)
                val placement = SkinLayoutEngine.calculate(skin, SkinLayoutRequest(size, inset, mode))
                assertTrue(placement.viewport.x >= inset.left - 0.001f)
                assertTrue(placement.viewport.y >= inset.top - 0.001f)
                assertTrue(placement.viewport.right <= size.width - inset.right + 0.001f)
                assertTrue(placement.viewport.bottom <= size.height - inset.bottom + 0.001f)
                skin.keyRegions.forEach { key ->
                    val x = key.bounds.x + key.bounds.width / 2
                    val y = key.bounds.y + key.bounds.height / 2
                    assertEquals(x, placement.skinX(placement.screenX(x)), 0.001f)
                    assertEquals(y, placement.skinY(placement.screenY(y)), 0.001f)
                }
            }
        }
    }

    private fun overlap(a: SkinRect, b: SkinRect) =
        a.x < b.right && b.x < a.right && a.y < b.bottom && b.y < a.bottom
}
