package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import kotlin.test.*

class Pc1401ProgramIoTest {
    private val id = MachineId("pc-1401")
    private fun session(): Pair<PlayerSession, EmulatorSession> {
        // Original NOPW instructions; no firmware or ROM-derived fixtures.
        val rom = assertIs<RomImportResult.Success>(Pc1401ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x4d }, ByteArray(0x8000) { 0x4d },
        )).romSet
        val core = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
        return PlayerSession(core) to core
    }
    private fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC,
                      machine: MachineId = id): PlayerProgramInput =
        assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, machine)).input
    private fun ram(core: EmulatorSession) = core.memorySnapshot(0x3800, 0x1000).copyBytes()
    private fun basic(player: PlayerSession) = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()

    @Test fun s1BasicRoundTripUses1401PointersAndLeavesCheckReadOnly() {
        val (player, core) = session()
        val parsed = input("\uFEFF# original test\r\n10 PRINT \"HELLO\"\r\n20 END\r\n")
        val before = ram(core)
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(parsed))
        assertContentEquals(before, ram(core))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(parsed))
        val bytes = basic(player)
        assertEquals("10 PRINT\"HELLO\"\n20 END\n", bytes.decodeToString())
        val pointer = core.memorySnapshot(0x46e1, 4).copyBytes()
        assertEquals(0x3800, (pointer[0].toInt() and 255) or ((pointer[1].toInt() and 255) shl 8))
        assertEquals(0xff, core.memorySnapshot(0x3800, 1).copyBytes()[0].toInt() and 255)
        val (other, _) = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(bytes.decodeToString())))
        assertContentEquals(bytes, basic(other))
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
    }

    @Test fun capacityAndWrongMachineFailuresDoNotChangeExistingBasic() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        val before = ram(core)
        val tooLarge = input((1..100).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" })
        val error = assertIs<PlayerProgramError.BasicMemory>(assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(tooLarge)).error)
        val capacity = assertIs<BasicProgramMemoryError.ProgramTooLarge>(error.error)
        assertEquals(0x46e0 - 0x3800, capacity.capacity)
        assertContentEquals(before, ram(core))
        val wrong = input("10 END", machine = MachineId("pc-1245"))
        assertEquals(PlayerProgramError.WrongMachine, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(wrong)).error)
        assertContentEquals(before, ram(core))
    }

    @Test fun dumpBoundariesAndDisjointSegmentsRoundTripWithAtomicRejection() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("3800 12 34\n4400 56\n47FF AB", PlayerProgramFormat.MEMORY_DUMP)))
        for ((address, expected) in listOf(0x3800 to 0x12, 0x4400 to 0x56, 0x47ff to 0xab)) {
            val exported = assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(address, address)).copyBytes()
            val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(exported.decodeToString())).image
            assertEquals(address, image.segments.single().startAddress)
            assertEquals(expected, image.segments.single().copyBytes().single().toInt() and 255)
        }
        val before = ram(core)
        for (text in listOf("4400 99\n0000 AA", "37FF AA", "47FF 12 34", "6000 12", "8000 12")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
            assertContentEquals(before, ram(core))
        }
        for ((start, end) in listOf(0x37ff to 0x3800, 0x47ff to 0x4800, 0x6000 to 0x6000, 0x8000 to 0x8000, 0x4401 to 0x4400)) {
            assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(start, end))
        }
        // DMP is a direct RAM load and may replace BASIC bytes/pointers deliberately.
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("3800 4D\n46E1 00 00 00 00", PlayerProgramFormat.MEMORY_DUMP)))
        assertIs<PlayerProgramExportResult.Failure>(player.exportBasic())
        assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0x3800, 0x3800))
    }

    @Test fun runningOperationsAreRejectedAndMalformedInputCannotBeApplied() {
        val (player, core) = session()
        val parsed = input("10 END")
        val before = ram(core)
        player.runner.run(0)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(parsed)).error)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramExportResult.Failure>(player.exportBasic()).error)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(0x4400, 0x4401)).error)
        assertContentEquals(before, ram(core))
        for ((text, format) in listOf("PRINT 1" to PlayerProgramFormat.BASIC, "4400 GG" to PlayerProgramFormat.MEMORY_DUMP)) {
            assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(text.encodeToByteArray(), format, id))
        }
    }
}
