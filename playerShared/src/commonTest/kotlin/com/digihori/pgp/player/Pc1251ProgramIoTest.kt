package com.digihori.pgp.player

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class Pc1251ProgramIoTest {
    private val id = MachineId("pc-1251")
    private fun session(): PlayerSession {
        val rom = assertIs<RomImportResult.Success>(Pc1251ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x4d }, ByteArray(0x4000),
        )).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session.also {
            // Synthetic code has no ROM boot: seed an empty BASIC range as NEW would do.
            assertIs<PlayerProgramApplyResult.Success>(it.loadProgram(input(
                "C6E1 00 B8 FF B7", PlayerProgramFormat.MEMORY_DUMP)))
        }
    }
    private fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC,
                      machine: MachineId = id) = assertIs<PlayerProgramParseResult.Success>(
        PlayerProgramParser.parse(text.encodeToByteArray(), format, machine)).input
    private fun dump(player: PlayerSession, start: Int = 0xb800, end: Int = 0xc7ff) =
        assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(start, end)).copyBytes()
    private fun basic(player: PlayerSession) = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()

    @Test fun oldBasicRoundTripAndPointersUseHardwareCapacity() {
        val player = session()
        val source = input("\uFEFF# original test\r\n10 PRINT \"HELLO\"\r\n20 END\r\n")
        val before = dump(player)
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(source))
        assertContentEquals(before, dump(player))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(source))
        val text = basic(player)
        assertEquals("10 PRINT \"HELLO\"\n20 END\n", text.decodeToString())
        assertTrue(dump(player, 0xc6e1, 0xc6e2).decodeToString().contains("00 B8"))
        val other = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(text.decodeToString())))
        assertContentEquals(text, basic(other))
    }

    @Test fun capacityAndMachineMismatchRejectBeforeWriting() {
        val player = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        val before = dump(player)
        val large = input((1..200).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" })
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(large))
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input("10 END", machine = MachineId("pc-1245"))))
        assertContentEquals(before, dump(player))
    }

    @Test fun dumpAliasesBoundsAndManagementCorruptionAreExplicit() {
        val player = session()
        fun load(text: String) = player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP))
        assertIs<PlayerProgramApplyResult.Success>(load("B800 12 34\nC7FF 56"))
        assertEquals(dump(player, 0xb800, 0xb801).decodeToString().substringAfter(' '),
            dump(player, 0xb000, 0xb001).decodeToString().substringAfter(' '))
        val before = dump(player)
        for (text in listOf("B800 99\nA000 12", "C7FF 12 34", "F800 01", "0000 01", "B000 12\nB800 34")) {
            assertIs<PlayerProgramApplyResult.Failure>(load(text))
            assertContentEquals(before, dump(player))
        }
        assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(0xb000, 0xbfff))
        val exported = dump(player, 0xc500, 0xc50f)
        assertIs<PlayerProgramApplyResult.Success>(session().loadProgram(input(exported.decodeToString(), PlayerProgramFormat.MEMORY_DUMP)))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        assertIs<PlayerProgramApplyResult.Success>(load("C6E1 00 00 00 00"))
        assertIs<PlayerProgramExportResult.Failure>(player.exportBasic())
        assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0xc6e1, 0xc6e4))
    }

    @Test fun runningOperationsRejectAndPauseRestoresAccess() {
        val player = session()
        val source = input("10 END")
        player.runner.run(0)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(source)).error)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramExportResult.Failure>(player.exportBasic()).error)
        player.runner.pause()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(source))
        assertIs<PlayerProgramExportResult.Success>(player.exportBasic())
    }
}
