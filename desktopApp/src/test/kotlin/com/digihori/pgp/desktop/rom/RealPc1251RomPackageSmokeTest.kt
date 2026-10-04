package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1251RomPackageSmokeTest {
    @Test
    fun convertsReloadsAndBootsTheLocallySuppliedRomComponents() {
        val internalRom = findLocalFile(INTERNAL_ROM_ENV, DEFAULT_INTERNAL_ROM_PATH) ?: return
        val externalRom = findLocalFile(EXTERNAL_ROM_ENV, DEFAULT_EXTERNAL_ROM_PATH) ?: return
        val conversion = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.createPc1251Package(
                internalRom.readBytes(),
                externalRom.readBytes(),
            ),
        )
        val packageRead = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(conversion.packageBytes),
        )

        assertEquals(Pc1251RomDefinition.MACHINE_ID, packageRead.romSet.machineId)
        assertEquals("PC-1251 ROM", packageRead.title)
        assertEquals(emptyList(), packageRead.warnings)
        assertEquals(
            listOf(Pc1251RomDefinition.INTERNAL_SIZE, Pc1251RomDefinition.EXTERNAL_SIZE),
            packageRead.romSet.components.map { it.size },
        )

        val loaded = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadPackage(conversion.packageBytes),
        )
        val run = loaded.session.runCycles(BOOT_CYCLE_BUDGET)

        assertEquals(Pc1251RomDefinition.MACHINE_ID, loaded.session.machineId)
        assertEquals(24, loaded.session.displaySnapshot().characterColumns)
        assertTrue(run.executedCycles > 0)
        assertTrue(run.status !is ExecutionStatus.Faulted, "Packaged PC-1251 ROM failed to boot: ${run.status}")

        val audio = loaded.session.drainAudioSamples()
        assertEquals(22_050, audio.sampleRate)
        assertEquals(0, audio.sampleCount, "Silent PC-1251 boot unexpectedly produced audible PCM")

        loaded.session.pressKey(PocketKey.A)
        val keyHeld = loaded.session.runCycles(KEY_HOLD_CYCLES)
        loaded.session.releaseKey(PocketKey.A)
        val keyReleased = loaded.session.runCycles(KEY_SETTLE_CYCLES)

        assertIs<ExecutionStatus.Ready>(keyHeld.status)
        assertIs<ExecutionStatus.Ready>(keyReleased.status)

        val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compilePc1245Program("10 PRINT \"PC-1251\"\n20 END\n".encodeToByteArray()),
        )
        val basicLoaded = assertIs<BasicProgramLoadResult.Success>(
            loaded.session.loadBasicProgram(compiled.bytes),
        )
        val basicSnapshot = assertIs<BasicProgramSnapshotResult.Success>(loaded.session.basicProgramSnapshot())

        assertTrue(basicLoaded.startAddress in 0xb800 until 0xc6e1)
        assertTrue(compiled.bytes.contentEquals(basicSnapshot.copyBytes()))
    }

    private fun findLocalFile(environmentVariable: String, defaultPath: String): File? {
        val configured = System.getenv(environmentVariable)
        if (!configured.isNullOrBlank()) return File(configured).takeIf(File::isFile)

        var directory: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(MAX_PARENT_SEARCH_DEPTH + 1) {
            val current = directory ?: return null
            val candidate = File(current, defaultPath)
            if (candidate.isFile) return candidate
            directory = current.parentFile
        }
        return null
    }

    private companion object {
        const val INTERNAL_ROM_ENV = "PGP_PC1251_INTERNAL_ROM"
        const val EXTERNAL_ROM_ENV = "PGP_PC1251_EXTERNAL_ROM"
        const val DEFAULT_INTERNAL_ROM_PATH = "../pc1251-emulator/rom/cpu-1251.rom"
        const val DEFAULT_EXTERNAL_ROM_PATH = "../pc1251-emulator/rom/bas-1251.rom"
        const val MAX_PARENT_SEARCH_DEPTH = 4
        const val BOOT_CYCLE_BUDGET = 1_000_000L
        const val KEY_HOLD_CYCLES = 17_280L
        const val KEY_SETTLE_CYCLES = 57_600L
    }
}
