package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360LegacyRomImporter
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1360RomSmokeTest {
    @Test
    fun locallySuppliedRomBootsUpdatesDisplayAndSwitchesBanks() {
        val internal = findLocal("pc1360mem.bin") ?: return
        val banks = findLocal("pc1360bank.bin") ?: return
        val imported = assertIs<RomImportResult.Success>(
            Pc1360LegacyRomImporter.importImages(internal.readBytes(), banks.readBytes()),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1360RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        val result = session.runCycles(1_000_000L)
        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertEquals(25, session.displaySnapshot().characterColumns)
        assertEquals(4, session.displaySnapshot().characterRows)
        assertTrue(session.displaySnapshot().copyDots().any { it.toInt() != 0 })
        assertTrue(session.drainBankSwitchEvents().isNotEmpty(), "PC-1360 ROM did not switch ROM banks")
    }

    @Test fun locallySuppliedRomInitializesThenRunsLoadedS2Basic() {
        val internal = findLocal("pc1360mem.bin") ?: return
        val banks = findLocal("pc1360bank.bin") ?: return
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(internal.readBytes(), banks.readBytes())).romSet
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(rom.machineId, rom)).session
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(rom.machineId)
        fun advance(cycles: Long) { session.runCycles(cycles); session.drainAudioSamples() }
        fun tap(key: com.digihori.pgp.core.api.PocketKey) {
            session.pressKey(key); advance(definition.automaticKeyHoldCycles); session.releaseKey(key); advance(definition.automaticKeyGapCycles)
        }
        advance(1_000_000)
        tap(com.digihori.pgp.core.api.PocketKey.Y)
        tap(com.digihori.pgp.core.api.PocketKey.ENTER)
        tap(com.digihori.pgp.core.api.PocketKey.CLEAR)
        advance(500_000)
        val compiled = assertIs<com.digihori.pgp.core.source.basic.BasicProgramCompileResult.Success>(
            com.digihori.pgp.core.source.basic.BasicProgramCodec.compileProgram(
                "10 POKE &C100,90\n20 END".encodeToByteArray(), rom.machineId)).bytes
        assertIs<com.digihori.pgp.core.api.BasicProgramLoadResult.Success>(session.loadBasicProgram(compiled))
        for (key in listOf(com.digihori.pgp.core.api.PocketKey.R, com.digihori.pgp.core.api.PocketKey.U,
            com.digihori.pgp.core.api.PocketKey.N, com.digihori.pgp.core.api.PocketKey.ENTER)) tap(key)
        advance(1_000_000)
        assertEquals(90, session.memorySnapshot(0xc100, 1).copyBytes().single().toInt() and 255)
    }

    private fun findLocal(name: String): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1360/$name") }
            .firstOrNull(File::isFile)
}
