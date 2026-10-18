package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import java.io.File
import kotlin.test.*

class RealPc1360kRomSmokeTest {
    @Test
    fun localFirmwareBootsWithoutKanjiRom() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1360k") }
            .firstOrNull { File(it, "pc1360kmem.bin").isFile && File(it, "pc1360kbank.bin").isFile } ?: return
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        val imported = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(), id,
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, imported.romSet)).session
        val result = session.runCycles(1_000_000L)
        assertIs<ExecutionStatus.Ready>(result.status)
        assertEquals(id, session.machineId)
        assertTrue(result.executedInstructions > 0)
        assertTrue(session.displaySnapshot().copyDots().any { it.toInt() != 0 })
        assertTrue(session.drainBankSwitchEvents().isNotEmpty())
    }
}
