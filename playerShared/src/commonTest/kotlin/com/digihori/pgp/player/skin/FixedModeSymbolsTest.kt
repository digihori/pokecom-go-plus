package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.player.*
import kotlin.test.*

class FixedModeSymbolsTest {
    private val modes = mapOf(OperatingMode.RUN to DisplaySymbol.RUN,
        OperatingMode.PROGRAM to DisplaySymbol.PRO, OperatingMode.RESERVE to DisplaySymbol.RESERVE)

    @Test fun fixedLabelsMatchEachMachinesModesAndOnlyTheCurrentOneIsActive() {
        for (name in listOf("pc-1245", "pc-1251")) {
            val id = MachineId(name)
            val skin = assertNotNull(PlayerSkinCatalog.forMachine(id)).withFixedModeIndicators()
            val supported = MachineCatalog.require(id).supportedOperatingModes
            val regions = skin.statusRegions.filter { it.symbol in modes.values }
            assertNull(skin.operatingModeRegion)
            assertEquals(supported.map { modes.getValue(it) }.toSet(), regions.map { it.symbol }.toSet())
            assertEquals(listOf(70f, 135f, 200f).take(regions.size), regions.map { it.bounds.x })
            assertEquals(1, regions.map { it.bounds.y }.distinct().size)
            val internal = ByteArray(0x2000).also { byteArrayOf(0x79, 0, 0).copyInto(it) }
            val rom = RomSet(id, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL,
                    ByteArray(if (name == "pc-1261") 0x8000 else 0x4000)),
            ))
            val player = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
            for ((mode, symbol) in modes) {
                assertEquals(mode in supported, player.setOperatingMode(mode))
                if (mode in supported) assertEquals(listOf(symbol), player.displayFrame().symbols.filter { it in modes.values })
            }
        }
    }

    @Test fun referenceLayoutSeparatesTextLabelsAndPrintedPanelSegments() {
        val skin = assertNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1261")))
        val labels = skin.statusRegions.filter { it.style == SkinStatusStyle.LABEL }
        val segments = skin.statusRegions.filter { it.style == SkinStatusStyle.RECTANGLE }
        assertEquals(listOf(DisplaySymbol.KANA, DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF), labels.map { it.symbol })
        assertTrue(labels.all { it.bounds.right <= skin.lcdContentRegion.x })
        assertEquals(labels.map { it.bounds.y }.sorted(), labels.map { it.bounds.y })
        assertEquals(listOf(DisplaySymbol.BUSY, DisplaySymbol.PRINT, DisplaySymbol.DEG, DisplaySymbol.RAD,
            DisplaySymbol.ERROR, DisplaySymbol.UNLABELLED), segments.map { it.symbol })
        assertTrue(segments.all { it.bounds.y > skin.lcdContentRegion.bottom })
        assertEquals(segments.map { it.bounds.x }.sorted(), segments.map { it.bounds.x })
        assertTrue(skin.statusRegions.none { it.symbol in modes.values })
    }
}
