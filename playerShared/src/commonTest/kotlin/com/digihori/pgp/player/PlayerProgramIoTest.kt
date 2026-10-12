package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class PlayerProgramIoTest {
    private val id = Pc1245RomDefinition.MACHINE_ID
    private fun session(): Pair<PlayerSession, EmulatorSession> {
        val rom = RomSet(id, listOf(
            RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d }),
            RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d }),
        ))
        val core = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
        val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse("C6E1 00 C0 FF BF")).image
        assertIs<MemoryImageLoadResult.Success>(core.loadMemoryImage(image))
        val empty = assertIs<com.digihori.pgp.core.source.basic.BasicProgramCompileResult.Success>(
            com.digihori.pgp.core.source.basic.BasicProgramCodec.compileProgram(byteArrayOf(), id),
        )
        assertIs<BasicProgramLoadResult.Success>(core.loadBasicProgram(empty.bytes))
        return PlayerSession(core) to core
    }
    private fun input(text: String, format: PlayerProgramFormat = PlayerProgramFormat.BASIC): PlayerProgramInput =
        assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, id)).input

    @Test fun basicRoundTripAndCheckDoesNotMutate() {
        val (player, core) = session()
        val before = core.memorySnapshot(0, 65536).copyBytes()
        val source = "\uFEFF# Studio description\r\n10 PRINT \"HELLO\"\r\n20 END\r"
        val parsed = input(source)
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(parsed))
        assertContentEquals(before, core.memorySnapshot(0, 65536).copyBytes())
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(parsed))
        val text = assertIs<PlayerProgramExportResult.Success>(player.exportBasic()).copyBytes().decodeToString()
        assertEquals("10 PRINT \"HELLO\"\n20 END\n", text)
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
    }

    @Test fun disjointDumpRoundTripAndAtomicFailure() {
        val (player, core) = session()
        val dump = input("C500 01 02\nC510 FF", PlayerProgramFormat.MEMORY_DUMP)
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(dump))
        val exported = assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0xc500, 0xc501)).copyBytes()
        val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(exported.decodeToString())).image
        assertContentEquals(byteArrayOf(1,2), image.segments.single().copyBytes())
        val before = core.memorySnapshot(0,65536).copyBytes()
        val invalid = input("C500 99\n0000 FF", PlayerProgramFormat.MEMORY_DUMP)
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(invalid))
        assertContentEquals(before, core.memorySnapshot(0,65536).copyBytes())
    }

    @Test fun protectsLcdRomAndAliases() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        val before = core.memorySnapshot(0,65536).copyBytes()
        for (text in listOf("0000 01", "F800 01", "C500 01\nD500 02")) {
            assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(input(text, PlayerProgramFormat.MEMORY_DUMP)))
            assertContentEquals(before, core.memorySnapshot(0,65536).copyBytes())
        }
    }

    @Test fun capacityFailureAndRevalidationLeaveMemoryUnchanged() {
        val (player, core) = session()
        val large = input((1..80).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" })
        val before = core.memorySnapshot(0,65536).copyBytes()
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(large))
        assertContentEquals(before, core.memorySnapshot(0,65536).copyBytes())
        val dump = input("C010 99", PlayerProgramFormat.MEMORY_DUMP)
        assertIs<PlayerProgramCheckResult.Success>(player.checkProgram(dump))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 PRINT \"ABCDEFGHIJKLMNOPQRSTUVWXYZ\"")))
        player.runner.run(0)
        val afterBasic = core.memorySnapshot(0,65536).copyBytes()
        assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(dump))
        assertContentEquals(afterBasic, core.memorySnapshot(0,65536).copyBytes())
        player.runner.pause()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(dump))
        assertEquals(0x99, core.memorySnapshot(0xc010, 1).copyBytes()[0].toInt() and 0xff)
    }

    @Test fun futureTargetDialectsAndSpecialEscapesRoundTrip() {
        for (machine in listOf(id, MachineId("pc-1401"), MachineId("pc-1350"))) {
            val source = if (machine.value == "pc-1350") "10 PRINT \"HELLO\"" else "10 PRINT \"\\PI\\SQR\""
            val compiled = assertIs<com.digihori.pgp.core.source.basic.BasicProgramCompileResult.Success>(
                com.digihori.pgp.core.source.basic.BasicProgramCodec.compileProgram(source.encodeToByteArray(), machine),
            )
            val decoded = assertIs<com.digihori.pgp.core.source.basic.BasicProgramDecodeResult.Success>(
                com.digihori.pgp.core.source.basic.BasicProgramCodec.detokenizeProgram(compiled.bytes, machine),
            )
            val recompiled = assertIs<com.digihori.pgp.core.source.basic.BasicProgramCompileResult.Success>(
                com.digihori.pgp.core.source.basic.BasicProgramCodec.compileProgram(decoded.utf8Bytes, machine),
            )
            assertContentEquals(compiled.bytes, recompiled.bytes)
        }
    }

    @Test fun machineCodeCanReplaceBasicAtC000() {
        val (player, core) = session()
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("10 END")))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("C000 01 02 03", PlayerProgramFormat.MEMORY_DUMP)))
        assertContentEquals(byteArrayOf(1,2,3), core.memorySnapshot(0xc000,3).copyBytes())
        // Machine-code-only use must not depend on valid BASIC pointers.
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("C6E1 00 00 00 00", PlayerProgramFormat.MEMORY_DUMP)))
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input("C000 04", PlayerProgramFormat.MEMORY_DUMP)))
    }

    @Test fun rejectsRunningWrongMachineAndInvalidInput() {
        val (player, core) = session()
        val parsed = input("10 END")
        player.runner.run(0)
        assertEquals(PlayerProgramError.NotPaused, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(parsed)).error)
        assertIs<PlayerProgramExportResult.Failure>(player.exportBasic())
        player.runner.pause()
        val other = assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse("10 END".encodeToByteArray(), PlayerProgramFormat.BASIC, MachineId("pc-1350"))).input
        assertEquals(PlayerProgramError.WrongMachine, assertIs<PlayerProgramApplyResult.Failure>(player.loadProgram(other)).error)
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(byteArrayOf(0xc0.toByte()), PlayerProgramFormat.BASIC, id))
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(byteArrayOf(0xc0.toByte()), PlayerProgramFormat.MEMORY_DUMP, id))
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse("C500 GG".encodeToByteArray(), PlayerProgramFormat.MEMORY_DUMP, id))
        assertIs<PlayerProgramParseResult.Failure>(PlayerProgramParser.parse(byteArrayOf(), PlayerProgramFormat.MEMORY_DUMP, id))
        assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(0xffff, 0x10000))
        assertIs<PlayerProgramExportResult.Failure>(player.exportMemoryDump(0, 10))
        assertEquals(0, core.memorySnapshot(0xc500,1).copyBytes()[0].toInt())
    }
}
