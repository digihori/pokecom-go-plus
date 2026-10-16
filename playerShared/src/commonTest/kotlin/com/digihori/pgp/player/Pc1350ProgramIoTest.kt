package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import kotlin.test.*

class Pc1350ProgramIoTest {
    private val id = MachineId("pc-1350")
    private fun session(): Pair<PlayerSession, EmulatorSession> {
        // Original NOPW instructions; no firmware or ROM-derived fixtures.
        val rom = assertIs<RomImportResult.Success>(Pc1350ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x4d }, ByteArray(0x8000) { 0x4d },
        )).romSet
        val core = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
        return PlayerSession(core) to core
    }
    private fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC,
                      machine: MachineId = id): PlayerProgramInput =
        assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, machine)).input
    private fun ram(core: EmulatorSession) = core.memorySnapshot(0x2000, 0x6000).copyBytes()
    private fun basic(player: PlayerSession) = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes()

    @Test fun s1BasicRoundTripUses1350PointersAndLeavesCheckReadOnly() {
        val (player, core) = session()
        val parsed = input("\uFEFF# original test\r\n10 PRINT \"HELLO\"\r\n20 END\r\n")
        val before = ram(core)
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(parsed))
        assertContentEquals(before, ram(core))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(parsed))
        val bytes = basic(player)
        assertEquals("10 PRINT\"HELLO\"\n20 END\n", bytes.decodeToString())
        val pointer = core.memorySnapshot(0x6f01, 4).copyBytes()
        assertEquals(0x2030, (pointer[0].toInt() and 255) or ((pointer[1].toInt() and 255) shl 8))
        assertEquals(0xff, core.memorySnapshot(0x2030, 1).copyBytes()[0].toInt() and 255)
        val (other, _) = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(bytes.decodeToString())))
        assertContentEquals(bytes, basic(other))
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
    }

    @Test fun capacityAndWrongMachineFailuresDoNotChangeExistingBasic() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        val before = ram(core)
        val tooLarge = input((1..500).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" })
        val error = assertIs<PlayerProgramError.BasicMemory>(assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(tooLarge)).error)
        val capacity = assertIs<BasicProgramMemoryError.ProgramTooLarge>(error.error)
        assertEquals(0x6f00 - 0x2030, capacity.capacity)
        assertContentEquals(before, ram(core))
        val wrong = input("10 END", machine = MachineId("pc-1245"))
        assertEquals(PlayerProgramError.WrongMachine, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(wrong)).error)
        assertContentEquals(before, ram(core))
    }

    @Test fun dumpBoundariesAndDisjointSegmentsRoundTripWithAtomicRejection() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("2000 CD\n2030 12 34\n6000 56\n6FFF EF\n7FFF AB", PlayerProgramFormat.MEMORY_DUMP)))
        for ((address, expected) in listOf(0x2000 to 0xcd, 0x6fff to 0xef, 0x2030 to 0x12, 0x6000 to 0x56, 0x7fff to 0xab)) {
            val exported = assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(address, address)).copyBytes()
            val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(exported.decodeToString())).image
            assertEquals(address, image.segments.single().startAddress)
            assertEquals(expected, image.segments.single().copyBytes().single().toInt() and 255)
        }
        val before = ram(core)
        for (text in listOf("6000 99\n0000 AA", "1FFF AA", "7FFF 12 34", "7000 12", "8000 12")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
            assertContentEquals(before, ram(core))
        }
        for ((start, end) in listOf(0x1fff to 0x2000, 0x7fff to 0x8000, 0x7000 to 0x7000, 0x8000 to 0x8000, 0x6001 to 0x6000)) {
            assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(start, end))
        }
        // DMP is a direct RAM load and may replace BASIC bytes/pointers deliberately.
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("2030 4D\n6F01 00 00 00 00", PlayerProgramFormat.MEMORY_DUMP)))
        assertIs<PlayerProgramExportResult.Failure>(player.exportBasic())
        assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0x2030, 0x2030))
    }

    @Test fun dumpExportReloadsIntoANewSessionWithInclusiveEnd() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input(
            "6000 12 34 56\n6010 AB CD", PlayerProgramFormat.MEMORY_DUMP)))
        val dump = assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0x6000, 0x6011)).copyBytes()
        val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(dump.decodeToString())).image
        assertEquals(18, image.byteCount)
        val (other, otherCore) = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(dump.decodeToString(), PlayerProgramFormat.MEMORY_DUMP)))
        assertContentEquals(core.memorySnapshot(0x6000, 18).copyBytes(), otherCore.memorySnapshot(0x6000, 18).copyBytes())
        assertContentEquals(dump, assertIs<PlayerProgramExportResult.Success>(other.exportMemoryDump(0x6000, 0x6011)).copyBytes())
    }

    @Test fun runningOperationsAreRejectedAndMalformedInputCannotBeApplied() {
        val (player, core) = session()
        val parsed = input("10 END")
        val before = ram(core)
        player.runner.run(0)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(parsed)).error)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramExportResult.Failure>(player.exportBasic()).error)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(0x6000, 0x6001)).error)
        assertContentEquals(before, ram(core))
        for ((text, format) in listOf("PRINT 1" to PlayerProgramFormat.BASIC, "6000 GG" to PlayerProgramFormat.MEMORY_DUMP)) {
            assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(text.encodeToByteArray(), format, id))
        }
    }
    @Test fun kanaAndDescriptionHeadersRoundTripThroughSharedCodec() {
        val (player, _) = session()
        val source = "\uFEFF# original UTF-8 sample\r\n10 PRINT \"ｱｲｳ ﾊﾟ\"\r\n20 END\r\n"
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input(source)))
        val exported = basic(player)
        assertEquals("10 PRINT\"ｱｲｳ ﾊﾟ\"\n20 END\n", exported.decodeToString())
        val (other, _) = session()
        assertIs<PlayerProgramApplyResult.Success>(other.loadProgram(input(exported.decodeToString())))
        assertContentEquals(exported, basic(other))
        // Full-width Japanese and arbitrary Unicode are not silently replaced.
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(
            "10 PRINT \"漢字\"".encodeToByteArray(), PlayerProgramFormat.BASIC, id))
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(
            byteArrayOf(0xc3.toByte(), 0x28), PlayerProgramFormat.BASIC, id))
    }

    @Test fun configuredBasicStartAndCapacityStayBelowManagementArea() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input(
            "6F01 F0 6E", PlayerProgramFormat.MEMORY_DUMP)))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        val pointers = core.memorySnapshot(0x6f01, 4).copyBytes()
        assertEquals(0x6ef0, (pointers[0].toInt() and 255) or ((pointers[1].toInt() and 255) shl 8))
        val before = ram(core)
        val failed = assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(
            "10 PRINT \"${"A".repeat(40)}\"")))
        val tooLarge = assertIs<BasicProgramMemoryError.ProgramTooLarge>(
            assertIs<PlayerProgramError.BasicMemory>(failed.error).error)
        assertEquals(16, tooLarge.capacity)
        assertContentEquals(before, ram(core))
    }

}
