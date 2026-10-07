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

    private fun findLocal(name: String): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1360/$name") }
            .firstOrNull(File::isFile)
}
