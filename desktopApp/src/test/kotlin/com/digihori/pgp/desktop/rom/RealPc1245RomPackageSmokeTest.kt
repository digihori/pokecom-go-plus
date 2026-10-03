package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1245RomPackageSmokeTest {
    @Test
    fun convertsReloadsAndBootsTheLocallySuppliedRom() {
        val romFile = findLocalRom() ?: return
        val legacy = assertIs<RomImportResult.Success>(
            Pc1245FlatRomImporter.importImage(romFile.readBytes()),
        ).romSet
        val conversion = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.createPc1245Package(
                requireNotNull(legacy.component(Pc1245RomDefinition.INTERNAL_ID)).copyBytes(),
                requireNotNull(legacy.component(Pc1245RomDefinition.EXTERNAL_ID)).copyBytes(),
            ),
        )
        val packageRead = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(conversion.packageBytes),
        )

        assertEquals(Pc1245RomDefinition.MACHINE_ID, packageRead.romSet.machineId)
        assertEquals("PC-1245 ROM", packageRead.title)
        assertEquals(emptyList(), packageRead.warnings)
        assertEquals(
            listOf(Pc1245RomDefinition.INTERNAL_SIZE, Pc1245RomDefinition.EXTERNAL_SIZE),
            packageRead.romSet.components.map { it.size },
        )

        val loaded = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadPackage(conversion.packageBytes),
        )
        val run = loaded.session.runCycles(BOOT_CYCLE_BUDGET)

        assertTrue(run.executedCycles > 0)
        assertTrue(run.status !is ExecutionStatus.Faulted, "Packaged ROM failed to boot: ${run.status}")
    }

    private fun findLocalRom(): File? {
        val configured = System.getenv(ROM_PATH_ENV)
        if (!configured.isNullOrBlank()) return File(configured).takeIf(File::isFile)

        var directory: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(MAX_PARENT_SEARCH_DEPTH + 1) {
            val current = directory ?: return null
            val candidate = File(current, DEFAULT_ROM_PATH)
            if (candidate.isFile) return candidate
            directory = current.parentFile
        }
        return null
    }

    private companion object {
        const val ROM_PATH_ENV = "PGP_PC1245_ROM"
        const val DEFAULT_ROM_PATH = "local-data/roms/pc-1245/pc1245mem.bin"
        const val MAX_PARENT_SEARCH_DEPTH = 4
        const val BOOT_CYCLE_BUDGET = 1_000_000L
    }
}
