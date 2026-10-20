package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.input.HostKeyState
import kotlin.test.*

class Pc1360PlayerFoundationTest {
    @Test fun factoryProvidesPausedSessionAndEveryPhysicalKeyReachesCore() {
        val player = session()
        assertEquals(Pc1360RomDefinition.MACHINE_ID, player.machineId)
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        val caps = assertNotNull(player.keyboard()).rows.flatten()
        assertEquals(60, caps.size)
        assertEquals(60, caps.map { it.key }.toSet().size)
        assertEquals(60, caps.map { it.id }.toSet().size)
        caps.forEach {
            assertIs<InputResult.Accepted>(player.pressKey(it.key), it.id)
            assertIs<InputResult.Accepted>(player.releaseKey(it.key), it.id)
        }
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.CAL))
        assertTrue(PlayerMachineCatalog.definitions.any { it.id == player.machineId })
    }

    @Test fun onlyLeftShiftAndDomesticLegendsMatchTheIllustration() {
        val caps = session().keyboard()!!.rows.flatten()
        assertEquals("SHIFT_LEFT", caps.single { it.key == PocketKey.SHIFT }.id)
        for ((key, legend) in listOf(PocketKey.COMMA to "<", PocketKey.COLON to ">",
            PocketKey.DIVIDE to "(", PocketKey.SEMICOLON to ")", PocketKey.MULTIPLY to "¥")) {
            assertEquals(legend, caps.single { it.key == key }.shiftedLabel)
        }
        assertNotNull(caps.single { it.key == PocketKey.SMALL })
        assertNotNull(caps.single { it.key == PocketKey.KANA })
    }

    @Test fun originalInstructionsExerciseFourRowsAndSnapshotSymbolsWithoutRom() {
        // LIDP address; LIA value; STD. Each physical VRAM row has five disjoint blocks.
        val program = buildList<Int> {
            fun write(address: Int, value: Int) { addAll(listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52)) }
            listOf(0x2800, 0x2840, 0x281e, 0x285e).forEach { write(it, 0x01) }
            listOf(0x301d, 0x305d, 0x303b, 0x307b).forEach { write(it, 0x80) }
            write(0x303c, 0x5f) // Exercise all six exported flags, not a real operating mode.
            val loop = size
            addAll(listOf(0x4d, 0x79, loop shr 8, loop and 255))
        }.map(Int::toByte).toByteArray()
        val player = session(program)
        val before = player.displayFrame()
        player.runner.run(0)
        val tick = player.runner.tick(10_000_000)
        assertEquals(2560L, tick.budget.cycles)
        assertIs<ExecutionStatus.Ready>(tick.runResult?.status)
        val frame = player.displayFrame()
        assertEquals(25, frame.characterColumns)
        assertEquals(4, frame.characterRows)
        assertEquals(6, frame.characterWidth)
        assertEquals(150, frame.dotColumns)
        assertEquals(32, frame.dotRows)
        assertEquals(150, frame.logicalColumns)
        assertEquals(32, frame.logicalRows)
        assertEquals(150f / 32f, frame.logicalAspectRatio)
        repeat(4) { row ->
            assertTrue(frame.isDotOn(0, row * 8))
            assertTrue(frame.isDotOn(149, row * 8 + 7))
            assertFalse(frame.isDotOn(1, row * 8))
            assertFalse(before.isDotOn(0, row * 8))
        }
        assertEquals(149, frame.logicalX(149))
        assertEquals(31, frame.logicalY(31))
        assertEquals(setOf(DisplaySymbol.SHIFT, DisplaySymbol.DEF, DisplaySymbol.RUN,
            DisplaySymbol.PRO, DisplaySymbol.KANA, DisplaySymbol.SMALL), frame.symbols.toSet())
        player.runner.pause()
        assertNull(player.runner.tick(1_000_000_000).runResult)
        player.reset()
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        assertTrue(player.displayFrame().symbols.isEmpty())
        val reset = player.displayFrame()
        repeat(32) { row -> repeat(150) { column -> assertFalse(reset.isDotOn(column, row)) } }
        assertTrue(frame.isDotOn(0, 0)) // Earlier snapshots remain independent after reset.
    }

    @Test fun s2BasicRoundTripAndRamOnlyDumpBoundaries() {
        val player = session()
        fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC) =
            assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(),
                format, MachineId("pc-1360"))).input
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 PRINT \"PC1360\"\n20 END")))
        val saved = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()
        val other = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(saved.decodeToString())))
        assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(other.exportBasic()).copyBytes())
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("8000 12\nFFFF 34", PlayerProgramFormat.MEMORY_DUMP)))
        for (address in listOf("1FFF", "2000", "2800", "303C", "3400", "3E00", "4000", "7FFF")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input("$address 5A", PlayerProgramFormat.MEMORY_DUMP)))
        }
        assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
        val tooLarge = input((1..800).joinToString("\n") { "$it PRINT \"${"A".repeat(48)}\"" })
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(tooLarge))
        assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes())
    }

    @Test fun skinPhysicalIdsAndLcdAspectMatchFourLineFrame() {
        val skin = assertNotNull(com.digihori.pgp.player.skin.PlayerSkinCatalog.forMachine(MachineId("pc-1360")))
        assertEquals(1426f, skin.designSize.width)
        assertEquals(session().keyboard()!!.rows.flatten().map { it.id }.toSet(), skin.keyRegions.map { it.id }.toSet())
        assertEquals(150f / 32f, skin.lcdContentRegion.width / skin.lcdContentRegion.height, 0.001f)
        skin.keyRegions.forEach { assertTrue(it.bounds.x >= skin.lcdPanelRegion.right || it.bounds.y >= skin.lcdPanelRegion.bottom) }
    }

    private fun session(program: ByteArray = byteArrayOf(0x79, 0, 0)): PlayerSession {
        val internal = ByteArray(Pc1360RomDefinition.INTERNAL_SIZE) { 0x4d }
        program.copyInto(internal)
        val rom = RomSet(Pc1360RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1360RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
        ) + List(8) { bank -> RomComponent(Pc1360RomDefinition.bankId(bank), RomRole.EXTERNAL,
            ByteArray(Pc1360RomDefinition.BANK_SIZE) { 0x4d }) })
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }
}
