package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import kotlin.test.*

class Pc1261PlayerFoundationTest {
    private fun session(code: List<Int> = listOf(0x79, 0, 0)): PlayerSession {
        val internal = ByteArray(0x2000) { 0x4d }
        code.forEachIndexed { i, v -> internal[i] = v.toByte() }
        val rom = assertIs<RomImportResult.Success>(Pc1261ComponentRomImporter.importImages(internal, ByteArray(0x8000))).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }

    @Test fun allKeysAndShiftLegendsFollowThe1261Matrix() {
        val player = session()
        val caps = assertNotNull(player.keyboard()).rows.flatten()
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        assertEquals(52, caps.size)
        caps.forEach {
            assertNull(it.basicLabel)
            assertIs<InputResult.Accepted>(player.pressKey(it.key))
            assertIs<InputResult.Accepted>(player.releaseKey(it.key))
        }
        assertEquals("HELP", caps.single { it.key == PocketKey.NUM_7 }.shiftedLabel)
        assertEquals("カナ", caps.single { it.key == PocketKey.NUM_8 }.shiftedLabel)
        assertEquals("(", caps.single { it.key == PocketKey.NUM_1 }.shiftedLabel)
        assertEquals(")", caps.single { it.key == PocketKey.NUM_2 }.shiftedLabel)
        assertNull(caps.single { it.key == PocketKey.DOWN }.shiftedLabel)
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.MODE))
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.KANA))
    }

    @Test fun twoLcdRowsAndNineFlagsReachImmutableSnapshots() {
        val code = buildList {
            fun write(address: Int, value: Int) { addAll(listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52)) }
            write(0x2000, 1); write(0x283b, 64)
            write(0x2040, 1); write(0x287b, 64)
            write(0x203d, 0x7b); write(0x207c, 0x23)
            val loop = size
            addAll(listOf(0x79, loop shr 8, loop and 255))
        }
        val player = session(code)
        val before = player.displayFrame()
        player.runner.run(0)
        assertEquals(2560L, player.runner.tick(10_000_000).budget.cycles)
        player.runner.pause()
        val frame = player.displayFrame()
        assertEquals(24, frame.characterColumns)
        assertEquals(2, frame.characterRows)
        assertEquals(120, frame.dotColumns)
        assertEquals(14, frame.dotRows)
        assertEquals(143, frame.logicalColumns)
        assertEquals(15, frame.logicalRows)
        assertTrue(frame.isDotOn(0, 0)); assertTrue(frame.isDotOn(119, 6))
        assertTrue(frame.isDotOn(0, 7)); assertTrue(frame.isDotOn(119, 13))
        assertFalse(before.isDotOn(0, 0))
        assertEquals(setOf(DisplaySymbol.BUSY, DisplaySymbol.PRINT, DisplaySymbol.KANA,
            DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF, DisplaySymbol.DEG,
            DisplaySymbol.RAD, DisplaySymbol.ERROR, DisplaySymbol.RUN), frame.symbols.toSet())
        for (mode in listOf(OperatingMode.PROGRAM, OperatingMode.RESERVE, OperatingMode.RUN)) {
            assertTrue(player.setOperatingMode(mode)); assertEquals(mode, player.operatingMode)
        }
        player.reset()
        assertEquals(OperatingMode.RUN, player.operatingMode)
        assertFalse(player.displayFrame().isDotOn(0, 0))
        assertTrue(frame.isDotOn(0, 0))
    }

    @Test fun s1BasicAndDumpLoadUse1261MemoryWhileBasicRemainsExportable() {
        fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC) =
            assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, MachineId("pc-1261"))).input
        val player = session()
        val source = input("\uFEFF# original test\r\n10 PRINT \"PC1261\"\r\n20 END\r\n")
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(source))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(source))
        val exported = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()
        assertEquals("10 PRINT\"PC1261\"\n20 END\n", exported.decodeToString())
        val other = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(exported.decodeToString())))
        assertContentEquals(exported, assertIs<PlayerProgramExportResult.Success>(other.exportBasic()).copyBytes())
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("4000 12\n67FF 34", PlayerProgramFormat.MEMORY_DUMP)))
        for (text in listOf("3FFF 12", "67FF 12 34", "2000 01", "8000 01")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
            assertContentEquals(exported, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
        }
        val tooLarge = input((1..400).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" })
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(tooLarge))
        assertContentEquals(exported, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
    }

    @Test fun skinMatchesKeysAndBothRowsFitBetweenStatusBands() {
        val skin = assertNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1261")))
        assertEquals(1206f, skin.designSize.width)
        assertEquals(session().keyboard()!!.rows.flatten().map { it.id }.toSet(), skin.keyRegions.map { it.id }.toSet())
        assertEquals(143f / 15f, skin.lcdContentRegion.width / skin.lcdContentRegion.height, 0.001f)
        skin.statusRegions.forEach {
            assertTrue(it.bounds.bottom <= skin.lcdContentRegion.y || it.bounds.y >= skin.lcdContentRegion.bottom)
        }
        skin.keyRegions.forEach { assertTrue(it.bounds.y > skin.lcdPanelRegion.bottom) }
    }
}
