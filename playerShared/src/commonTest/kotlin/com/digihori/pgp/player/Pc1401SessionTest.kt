package com.digihori.pgp.player

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import kotlin.test.*

class Pc1401SessionTest {
    private fun session(): PlayerSession {
        // Original NOPW bytes, not ROM-derived firmware.
        val rom = assertIs<RomImportResult.Success>(Pc1401ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x4d }, ByteArray(0x8000) { 0x4d },
        )).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }

    @Test fun freshAndResetSessionsUseCalculatorAndDoNotShareRam() {
        val first = session()
        assertEquals(PlayerRunState.PAUSED, first.runner.state)
        assertEquals(OperatingMode.CALCULATOR, first.operatingMode)
        val marker = assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(
            "4000 12 34".encodeToByteArray(), PlayerProgramFormat.MEMORY_DUMP, first.machineId,
        )).input
        assertIs<PlayerProgramApplyResult.Success>(first.loadProgram(marker))
        first.runner.run(0)
        first.runner.tick(20_000_000)
        first.runner.pause()
        assertContentEquals(byteArrayOf(0x12, 0x34), bytes(first))
        val second = session()
        assertContentEquals(byteArrayOf(0, 0), bytes(second))
        assertContentEquals(byteArrayOf(0x12, 0x34), bytes(first))
        first.setOperatingMode(OperatingMode.PROGRAM)
        first.reset()
        assertEquals(OperatingMode.CALCULATOR, first.operatingMode)
        assertEquals(PlayerRunState.PAUSED, first.runner.state)
        assertContentEquals(byteArrayOf(0, 0), bytes(first))
        assertFalse(first.setOperatingMode(OperatingMode.RESERVE))
    }

    private fun bytes(session: PlayerSession): ByteArray {
        val exported = assertIs<PlayerProgramExportResult.Success>(session.exportMemoryDump(0x4000, 0x4001))
        return assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(
            exported.copyBytes().decodeToString(),
        )).image.segments.single().copyBytes()
    }
}
