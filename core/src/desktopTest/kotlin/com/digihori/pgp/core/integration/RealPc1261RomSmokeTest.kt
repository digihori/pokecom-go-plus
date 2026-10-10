package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261RomDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1261RomSmokeTest {
    @Test
    fun locallySuppliedRomBootsAndUpdatesTwoLineDisplay() {
        val rom = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(Pc1261FlatRomImporter.importImage(rom.readBytes()))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1261RomDefinition.MACHINE_ID, imported.romSet),
        ).session
        val result = session.runCycles(1_000_000L)
        val display = session.displaySnapshot()

        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertEquals(24, display.characterColumns)
        assertEquals(2, display.characterRows)
        assertTrue(display.copyDots().any { it.toInt() != 0 }, "PC-1261 ROM did not update the LCD")
    }

    private fun findLocalRom(): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1261/pc1261mem.bin") }
            .firstOrNull(File::isFile)
}
