package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1350RomSmokeTest {
    @Test
    fun locallySuppliedRomBootsAndUpdatesFourLineDisplay() {
        val rom = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(Pc1350FlatRomImporter.importImage(rom.readBytes()))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1350RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        val result = session.runCycles(BOOT_CYCLE_BUDGET)
        val display = session.displaySnapshot()

        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertEquals(25, display.characterColumns)
        assertEquals(4, display.characterRows)
        assertTrue(display.copyDots().any { it.toInt() != 0 }, "PC-1350 ROM did not update the LCD")
    }

    private fun findLocalRom(): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, DEFAULT_ROM_PATH) }
            .firstOrNull(File::isFile)

    private companion object {
        const val DEFAULT_ROM_PATH = "local-data/roms/pc-1350/pc1350mem.bin"
        const val BOOT_CYCLE_BUDGET = 1_000_000L
    }
}
