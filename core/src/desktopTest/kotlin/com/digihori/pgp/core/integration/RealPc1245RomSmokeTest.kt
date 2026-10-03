package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1245RomSmokeTest {
    @Test
    fun locallySuppliedRomExecutesWithoutCoreFault() {
        val romFile = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(
            Pc1245FlatRomImporter.importImage(romFile.readBytes()),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        val result = session.runCycles(BOOT_CYCLE_BUDGET)

        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertTrue(result.executedCycles >= BOOT_CYCLE_BUDGET)
        assertEquals(Pc1245RomDefinition.MACHINE_ID, session.machineId)
    }

    private fun findLocalRom(): File? {
        val configuredPath = System.getenv(ROM_PATH_ENV)
        if (!configuredPath.isNullOrBlank()) {
            val configuredFile = File(configuredPath)
            if (configuredFile.isAbsolute) return configuredFile
            return findFromWorkingDirectoryAncestors(configuredPath)
        }

        return findFromWorkingDirectoryAncestors(DEFAULT_ROM_PATH)
    }

    private fun findFromWorkingDirectoryAncestors(path: String): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull(File::isFile)

    private companion object {
        const val ROM_PATH_ENV: String = "PGP_PC1245_ROM"
        const val DEFAULT_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
        const val BOOT_CYCLE_BUDGET: Long = 1_000_000
    }
}
