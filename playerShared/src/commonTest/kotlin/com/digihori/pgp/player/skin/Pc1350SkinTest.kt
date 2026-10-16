package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.*
import kotlin.test.*

class Pc1350SkinTest {
    private val skin = requireNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1350")))

    @Test fun physicalRegionsMatchKeycapsAndBothShiftPositionsAreDistinct() {
        val caps = Pc1350KeyboardLayout.keyboard.rows.flatten()
        assertEquals(SkinSize(1429f, 603f), skin.designSize)
        assertEquals(62, skin.keyRegions.size)
        assertEquals(caps.associate { it.id to it.key }, skin.keyRegions.associate { it.id to it.key })
        val shifts = skin.keyRegions.filter { it.key == PocketKey.SHIFT }
        assertEquals(2, shifts.size)
        assertEquals(SkinRect(11f, 369f, 68f, 49f), shifts.single { it.id == "SHIFT_LEFT" }.bounds)
        assertEquals(SkinRect(1339f, 191f, 68f, 51f), shifts.single { it.id == "SHIFT_RIGHT" }.bounds)
        assertEquals(SkinRect(798f, 545f, 159f, 49f), skin.keyRegions.single { it.key == PocketKey.ENTER }.bounds)
        skin.keyRegions.forEachIndexed { index, key ->
            assertFalse(overlap(key.bounds, skin.lcdPanelRegion), key.id)
            assertFalse(overlap(key.bounds, skin.controllerDisplayRegion), key.id)
            skin.keyRegions.drop(index + 1).forEach { other ->
                assertFalse(overlap(key.bounds, other.bounds), "${key.id} overlaps ${other.id}")
            }
        }
        assertNull(skin.operatingModeRegion)
        assertTrue(PlayerMachineCatalog.definitions.none { it.id == MachineId("pc-1360") })
    }

    @Test fun lcdAndExportedSymbolsFitThePanelWithoutOverlap() {
        assertEquals(150f / 32f, skin.lcdContentRegion.width / skin.lcdContentRegion.height, 0.0001f)
        assertEquals(setOf(DisplaySymbol.SHIFT, DisplaySymbol.DEF, DisplaySymbol.RUN,
            DisplaySymbol.PRO, DisplaySymbol.KANA, DisplaySymbol.SMALL), skin.statusRegions.map { it.symbol }.toSet())
        val regions = skin.statusRegions.map { it.bounds } + skin.lcdContentRegion
        regions.forEachIndexed { index, region ->
            assertTrue(contains(skin.lcdPanelRegion, region))
            assertTrue(contains(skin.controllerDisplayRegion, region))
            regions.drop(index + 1).forEach { assertFalse(overlap(region, it)) }
        }
    }

    @Test fun verticalSymbolColumnMatchesCenteredLcdHeight() {
        val lcd = skin.lcdContentRegion
        val panel = skin.lcdPanelRegion
        assertEquals(panel.y + panel.height / 2, lcd.y + lcd.height / 2, 0.001f)
        assertTrue(lcd.x + lcd.width / 2 > panel.x + panel.width / 2)
        assertEquals(listOf(DisplaySymbol.RUN, DisplaySymbol.PRO, DisplaySymbol.KANA,
            DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF), skin.statusRegions.map { it.symbol })
        assertEquals(lcd.y, skin.statusRegions.first().bounds.y, 0.001f)
        assertEquals(lcd.bottom, skin.statusRegions.last().bounds.bottom, 0.001f)
        skin.statusRegions.forEach { assertTrue(it.bounds.right < lcd.x) }
    }

    @Test fun lcdAndTouchCoordinatesShareSafeAreaTransformsInBothLandscapeDirections() {
        for (mode in PresentationMode.entries) {
            for (size in listOf(SkinSize(960f, 432f), SkinSize(800f, 360f))) {
                for (insets in listOf(SkinInsets(28f, 0f, 0f, 24f), SkinInsets(0f, 0f, 28f, 24f))) {
                    val p = SkinLayoutEngine.calculate(skin, SkinLayoutRequest(size, insets, mode))
                    assertTrue(p.viewport.x >= insets.left - 0.001f)
                    assertTrue(p.viewport.y >= insets.top - 0.001f)
                    assertTrue(p.viewport.right <= size.width - insets.right + 0.001f)
                    assertTrue(p.viewport.bottom <= size.height - insets.bottom + 0.001f)
                    val regions = listOf(skin.lcdContentRegion) +
                        if (mode == PresentationMode.CONTROLLER_DISPLAY) emptyList() else skin.keyRegions.map { it.bounds }
                    regions.forEach { bounds ->
                        val x = bounds.x + bounds.width / 2
                        val y = bounds.y + bounds.height / 2
                        val sx = p.screenX(x)
                        val sy = p.screenY(y)
                        assertTrue(sx in p.viewport.x..p.viewport.right)
                        assertTrue(sy in p.viewport.y..p.viewport.bottom)
                        assertEquals(x, p.skinX(sx), 0.001f)
                        assertEquals(y, p.skinY(sy), 0.001f)
                    }
                }
            }
        }
    }

    @Test fun duplicateRegionAndKeycapIdsAreRejectedWhileLogicalShiftMayRepeat() {
        val shift = skin.keyRegions.single { it.id == "SHIFT_LEFT" }
        assertFailsWith<IllegalArgumentException> { skin.copy(keyRegions = skin.keyRegions + shift) }
        assertFailsWith<IllegalArgumentException> { skin.copy(keyRegions = listOf(shift.copy(id = ""))) }
        assertFailsWith<IllegalArgumentException> {
            PlayerKeyboard(2, listOf(listOf(PlayerKeyCap(PocketKey.SHIFT, "SHIFT", column = 0),
                PlayerKeyCap(PocketKey.SHIFT, "SHIFT", column = 1))))
        }
        // Existing machines retain their old IDs and one region per logical key.
        for (id in listOf("pc-1245", "pc-1401")) {
            val existing = PlayerSkinCatalog.forMachine(MachineId(id))!!
            assertTrue(existing.keyRegions.all { it.id == it.key.name })
        }
    }

    private fun overlap(a: SkinRect, b: SkinRect) =
        a.x < b.right && b.x < a.right && a.y < b.bottom && b.y < a.bottom
    private fun contains(a: SkinRect, b: SkinRect) =
        b.x >= a.x && b.y >= a.y && b.right <= a.right && b.bottom <= a.bottom
}
