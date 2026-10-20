package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc14xx.*
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.input.*
import kotlin.test.*

class Pc1470uPlayerFoundationTest {
    private val id = MachineId("pc-1470u")
    private fun session(code: List<Int> = listOf(0x79, 0, 0)): PlayerSession {
        val internal = ByteArray(0x2000) { 0x4d }
        code.forEachIndexed { i, v -> internal[i] = v.toByte() }
        val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(
            Pc14xxModel.PC_1470U, internal, ByteArray(0x20000))).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }
    private fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC) =
        assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, id)).input

    @Test fun approvedSkinCoversAllPhysicalKeysAndTwentyTwoSymbolsWithoutOverlap() {
        val skin = assertNotNull(com.digihori.pgp.player.skin.PlayerSkinCatalog.forMachine(id))
        val keyboard = assertNotNull(session().keyboard())
        val caps = keyboard.rows.flatten() + assertNotNull(keyboard.functionPanel).rows.flatten()
        assertEquals(caps.map { it.id }.toSet(), skin.keyRegions.map { it.id }.toSet())
        assertEquals(22, skin.statusRegions.size)
        assertEquals(143f / 15f, skin.lcdContentRegion.width / skin.lcdContentRegion.height, 0.001f)
        skin.statusRegions.forEach { r ->
            assertTrue(r.bounds.y >= skin.lcdPanelRegion.y && r.bounds.bottom <= skin.lcdPanelRegion.bottom)
            assertTrue(r.bounds.bottom <= skin.lcdContentRegion.y || r.bounds.y >= skin.lcdContentRegion.bottom)
        }
        skin.statusRegions.groupBy { it.bounds.y }.values.forEach { row ->
            row.sortedBy { it.bounds.x }.zipWithNext().forEach { (a, b) -> assertTrue(a.bounds.right <= b.bounds.x) }
        }
        assertEquals(listOf(DisplaySymbol.CAL, DisplaySymbol.AER, DisplaySymbol.RUN, DisplaySymbol.PRO, DisplaySymbol.MATRIX, DisplaySymbol.STAT, DisplaySymbol.PRINT),
            skin.statusRegions.filter { it.bounds.y > skin.lcdContentRegion.bottom }.map { it.symbol })
        assertEquals(OperatingMode.RUN, session().operatingMode)
    }

    @Test fun keyboardAndGamepadExposeExactlyTheSupportedScanKeys() {
        val player = session()
        val keyboard = assertNotNull(player.keyboard())
        val caps = keyboard.rows.flatten() + assertNotNull(keyboard.functionPanel).rows.flatten()
        assertEquals(81, caps.size)
        assertEquals(Pc14xxKeyCatalog.supportedKeys(Pc14xxModel.PC_1470U), caps.map { it.key }.toSet())
        assertEquals(2, caps.count { it.key == PocketKey.SHIFT })
        assertEquals("AER", caps.single { it.key == PocketKey.CAL }.primaryLabel)
        assertNotNull(caps.single { it.key == PocketKey.TITLE })
        assertEquals(80, caps.map { it.key }.toSet().size)
        assertEquals("|←", caps.single { it.key == PocketKey.LEFT }.shiftedLabel)
        assertEquals("→|", caps.single { it.key == PocketKey.RIGHT }.shiftedLabel)
        assertEquals(18, keyboard.functionPanel.rows.flatten().size)
        val events = mutableListOf<String>()
        val owners = HostKeyState({ key -> assertIs<InputResult.Accepted>(player.pressKey(key)); events += "down:$key"; InputResult.Accepted },
            { key -> assertIs<InputResult.Accepted>(player.releaseKey(key)); events += "up:$key"; InputResult.Accepted })
        val pad = GamepadInput({ button, key -> owners.press("pad:$button", key) }, { button, _ -> owners.release("pad:$button") })
        val keys = GamepadKeys.forMachine(id)
        assertEquals(caps.map { it.key }.toSet(), keys)
        keys.forEach { key ->
            events.clear()
            pad.configure(GamepadSettings(true, mapOf(GamepadButton.SOUTH to key)), "pad", keys, true)
            pad.update("pad", emptySet()); pad.update("pad", setOf(GamepadButton.SOUTH))
            pad.configure(GamepadSettings(true, mapOf(GamepadButton.SOUTH to key)), "pad", keys, false)
            assertEquals(listOf("down:$key", "up:$key"), events)
            assertFalse(owners.isHeld(key))
        }
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.MODE))
    }

    @Test fun lcdUses1470AddressesAndExportsAllTwentyTwoFlags() {
        val code = buildList {
            fun write(a: Int, v: Int) { addAll(listOf(0x10, a shr 8, a and 255, 0x02, v, 0x52)) }
            write(0x2800, 1); write(0x2a3b, 2); write(0x2840, 4); write(0x2a7b, 64)
            write(0x283c, 127); write(0x283d, 15); write(0x287c, 127); write(0x287d, 15)
            val loop = size; addAll(listOf(0x79, loop shr 8, loop and 255))
        }
        val player = session(code)
        val before = player.displayFrame()
        player.runner.run(0); player.runner.tick(10_000_000); player.runner.pause()
        val frame = player.displayFrame()
        assertEquals(24, frame.characterColumns); assertEquals(2, frame.characterRows)
        assertEquals(143, frame.logicalColumns); assertEquals(15, frame.logicalRows)
        assertTrue(frame.isDotOn(0, 0)); assertTrue(frame.isDotOn(119, 1))
        assertTrue(frame.isDotOn(0, 9)); assertTrue(frame.isDotOn(119, 13))
        assertFalse(before.isDotOn(0, 0))
        assertEquals(setOf(DisplaySymbol.KANA, DisplaySymbol.KANA_SMALL, DisplaySymbol.SMALL, DisplaySymbol.STAT,
            DisplaySymbol.BUSY, DisplaySymbol.DEF, DisplaySymbol.SHIFT, DisplaySymbol.HYP, DisplaySymbol.PRO,
            DisplaySymbol.RUN, DisplaySymbol.CAL, DisplaySymbol.ERROR, DisplaySymbol.MEMORY, DisplaySymbol.PARENTHESES,
            DisplaySymbol.RAD, DisplaySymbol.G, DisplaySymbol.DE, DisplaySymbol.PRINT, DisplaySymbol.MATRIX, DisplaySymbol.AER, DisplaySymbol.BATTERY, DisplaySymbol.DBL), frame.symbols.toSet())
        player.reset(); assertFalse(player.displayFrame().isDotOn(0, 0)); assertTrue(frame.isDotOn(0, 0))
    }

    @Test fun basicVocabularyAndKanaRoundTripWhileDumpStaysInFixedRam() {
        val player = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 A=FACT(5):PRINT\"ｶπ√\"\n20 DECI 1\n30 END")))
        val saved = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()
        val other = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(saved.decodeToString())))
        assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(other.exportBasic()).copyBytes())
        for (text in listOf("8000 12", "FFFF 34")) assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
        for (text in listOf("1FFF 12", "3FFF 12", "7FFF 12 34", "3000 12", "3C00 12", "4000 12")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
            assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
        }
        val huge = input((1..500).joinToString("\n") { "$it PRINT\"${"A".repeat(60)}\"" })
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(huge))
        assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
        player.runner.run(0)
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input("10 END")))
    }

    @Test fun twoPhysicalShiftOwnersReleaseIndependently() {
        val player = session()
        val events = mutableListOf<String>()
        val owners = HostKeyState({ key -> events += "down:$key"; player.pressKey(key) },
            { key -> events += "up:$key"; player.releaseKey(key) })
        owners.press("SHIFT", PocketKey.SHIFT); owners.press("LOCK", PocketKey.SHIFT)
        owners.release("SHIFT"); assertTrue(owners.isHeld(PocketKey.SHIFT))
        owners.release("LOCK"); assertFalse(owners.isHeld(PocketKey.SHIFT))
        assertEquals(listOf("down:SHIFT", "up:SHIFT"), events)
    }

    @Test fun speedsAudioAndPauseResumeUseTheExisting256KhzRunner() {
        for (speed in PlayerExecutionSpeed.entries) {
            // LIP 5f, LIA 20, EXAM, OUTC: independently authored oscillator setup.
            val player = session(listOf(0x12, 0x5f, 0x02, 0x20, 0xdb, 0xdf, 0x79, 0, 6))
            player.runner.setSpeed(speed, 0); player.runner.run(0)
            val tick = player.runner.tick(10_000_000)
            assertEquals(2560L * speed.numerator / speed.denominator, tick.budget.cycles)
            val samples = assertNotNull(tick.audio).copySamples()
            assertTrue(samples.any { it > 0 }); assertTrue(samples.any { it < 0 })
            player.runner.pause(); assertNull(player.runner.tick(9_000_000_000).runResult)
            player.runner.run(9_000_000_000)
            assertEquals(tick.budget.cycles, player.runner.tick(9_010_000_000).budget.cycles)
            player.reset(); assertEquals(PlayerRunState.PAUSED, player.runner.state)
        }
    }
}
