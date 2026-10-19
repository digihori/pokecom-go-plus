package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import kotlin.test.*

class Pc1251PlayerFoundationTest {
    private fun session(program: List<Int> = listOf(0x79, 0, 0)): PlayerSession {
        val internal = ByteArray(0x2000) { 0x4d }
        program.forEachIndexed { i, byte -> internal[i] = byte.toByte() }
        val rom = assertIs<RomImportResult.Success>(Pc1251ComponentRomImporter.importImages(
            internal, ByteArray(0x4000),
        )).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }

    @Test fun keyboardLegendsAndAllKeysMatchTheOldFamilyMatrix() {
        val player = session()
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        val caps = assertNotNull(player.keyboard()).rows.flatten()
        assertEquals(52, caps.size)
        assertEquals(52, caps.map { it.id }.toSet().size)
        caps.forEach {
            assertNull(it.basicLabel)
            assertIs<InputResult.Accepted>(player.pressKey(it.key))
            assertIs<InputResult.Accepted>(player.releaseKey(it.key))
        }
        assertEquals("(", caps.single { it.key == PocketKey.DOWN }.shiftedLabel)
        assertEquals(")", caps.single { it.key == PocketKey.UP }.shiftedLabel)
        assertNull(caps.single { it.key == PocketKey.NUM_1 }.shiftedLabel)
        assertNull(caps.single { it.key == PocketKey.NUM_2 }.shiftedLabel)
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.MODE))
        assertNull(PlayerKeyboardCatalog.forMachine(MachineId("pc-1450")))
    }

    @Test fun hardwareRamRejectsExpandedAddressesAndSessionsStayIndependent() {
        val first = session()
        fun dump(text: String) = assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(
            text.encodeToByteArray(), PlayerProgramFormat.MEMORY_DUMP, first.machineId,
        )).input
        assertIs<PlayerProgramApplyResult.Success>(first.loadProgram(dump("B800 12\nC7FF 34")))
        assertIs<PlayerProgramApplyResult.Failure>(first.loadProgram(dump("B800 56\nA000 78")))
        val saved = assertIs<PlayerProgramExportResult.Success>(first.exportMemoryDump(0xb800, 0xb800))
        assertTrue(saved.copyBytes().decodeToString().contains("12"))
        assertIs<PlayerProgramExportResult.Failure>(first.exportMemoryDump(0xa000, 0xa000))
        val other = assertIs<PlayerProgramExportResult.Success>(session().exportMemoryDump(0xb800, 0xb800))
        assertFalse(saved.copyBytes().contentEquals(other.copyBytes()))
    }

    @Test fun lcdEndpointsSymbolsModeAndSnapshotIndependence() {
        val code = buildList {
            fun write(address: Int, value: Int) { addAll(listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52)) }
            write(0xf800, 1); write(0xf840, 0x40)
            write(0xf83c, 0x0f); write(0xf83d, 7)
            val loop = size
            addAll(listOf(0x79, loop shr 8, loop and 255))
        }
        val player = session(code)
        val before = player.displayFrame()
        player.runner.run(0)
        assertEquals(1920, player.runner.tick(10_000_000).budget.cycles.toInt())
        player.runner.pause()
        val frame = player.displayFrame()
        assertEquals(24, frame.characterColumns)
        assertEquals(120, frame.dotColumns)
        assertEquals(7, frame.dotRows)
        assertEquals(143, frame.logicalColumns)
        assertTrue(frame.isDotOn(0, 0))
        assertTrue(frame.isDotOn(119, 6))
        assertFalse(before.isDotOn(0, 0))
        assertEquals(setOf(DisplaySymbol.BUSY, DisplaySymbol.P, DisplaySymbol.DEF,
            DisplaySymbol.DE, DisplaySymbol.G, DisplaySymbol.RAD, DisplaySymbol.SHIFT,
            DisplaySymbol.RUN), frame.symbols.toSet())
        listOf(OperatingMode.PROGRAM, OperatingMode.RESERVE, OperatingMode.RUN).forEach {
            assertTrue(player.setOperatingMode(it))
            assertEquals(it, player.operatingMode)
        }
        assertFalse(player.setOperatingMode(OperatingMode.CALCULATOR))
        player.reset()
        assertEquals(OperatingMode.RUN, player.operatingMode)
        assertFalse(player.displayFrame().isDotOn(0, 0))
        assertTrue(frame.isDotOn(0, 0))
    }

    @Test fun skinMatchesPhysicalKeysAndTwentyFourCellAspect() {
        val skin = assertNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1251")))
        val caps = assertNotNull(session().keyboard()).rows.flatten()
        assertEquals(caps.map { it.id }.toSet(), skin.keyRegions.map { it.id }.toSet())
        assertEquals(1205f, skin.designSize.width)
        assertEquals(600f, skin.designSize.height)
        assertEquals(143f / 7f, skin.lcdContentRegion.width / skin.lcdContentRegion.height, 0.001f)
        skin.keyRegions.forEach { assertTrue(it.bounds.y > skin.lcdPanelRegion.bottom) }
        skin.statusRegions.forEach { assertTrue(it.bounds.bottom <= skin.lcdContentRegion.y || it.bounds.y >= skin.lcdContentRegion.bottom) }
        assertTrue(PlayerMachineCatalog.definitions.any { it.id == MachineId("pc-1251") })
    }
}
