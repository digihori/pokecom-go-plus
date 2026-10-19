package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.player.*
import kotlin.test.*

class FixedModeSymbolsTest {
    private val modes = mapOf(OperatingMode.RUN to DisplaySymbol.RUN,
        OperatingMode.PROGRAM to DisplaySymbol.PRO, OperatingMode.RESERVE to DisplaySymbol.RESERVE)

    @Test fun fixedLabelsMatchEachMachinesModesAndOnlyTheCurrentOneIsActive() {
        for (name in listOf("pc-1245", "pc-1251", "pc-1261")) {
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

    @Test fun referenceLayoutEnlargesAndGroupsThe1261StatusBands() {
        val skin = assertNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1261")))
        fun bounds(symbol: DisplaySymbol) = skin.statusRegions.single { it.symbol == symbol }.bounds
        assertTrue(skin.statusRegions.all { it.bounds.height == 24f })
        assertEquals(bounds(DisplaySymbol.DEG).right, bounds(DisplaySymbol.RAD).x)
        assertEquals(bounds(DisplaySymbol.SMALL).x, bounds(DisplaySymbol.PRINT).x)
        assertTrue(bounds(DisplaySymbol.DEF).x < bounds(DisplaySymbol.SHIFT).x)
        assertTrue(bounds(DisplaySymbol.SHIFT).right < bounds(DisplaySymbol.SMALL).x)
        assertTrue(bounds(DisplaySymbol.RAD).right < bounds(DisplaySymbol.ERROR).x)
    }
}
