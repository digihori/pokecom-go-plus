package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PokecomGoDeviceStateIntegrationTest {
    @Test
    fun importsCapturedPc1245Preferences() {
        if (System.getenv(ENABLE_ENV) != "1") return

        val preferences = sequenceOf(File(INPUT_PATH), File("../$INPUT_PATH"))
            .firstOrNull(File::isFile)
            ?: File(INPUT_PATH)
        require(preferences.isFile) {
            "Capture Pokecom GO preferences at '$INPUT_PATH' before running this test"
        }

        val expectation = PokecomGoPreferencesReader.readPc1245Expectation(
            preferences.readText(),
        )

        val cpu = assertNotNull(expectation.cpu)
        assertEquals(4, cpu.programCounter?.length)
        assertEquals(512, cpu.internalRamHex?.length)
        assertEquals(listOf("8000", "f800"), expectation.memory?.map { it.start })
        assertEquals(7, expectation.display?.rows?.size)
        assertEquals(true, expectation.display?.rows?.all { it.length == 80 })
    }

    @Test
    fun comparesPgpWithTheCapturedPokecomGoBootState() {
        if (System.getenv(ENABLE_ENV) != "1") return

        val preferences = findRequiredFile(CYCLE_CAPTURE_PATH)
        val rom = findRequiredFile(ROM_PATH)
        val expectation = PokecomGoPreferencesReader.readPc1245Expectation(preferences.readText())
        val romSet = assertIs<RomImportResult.Success>(
            Pc1245FlatRomImporter.importImage(rom.readBytes()),
        ).romSet
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, romSet),
        ).session

        session.runCycles(BOOT_CYCLES)
        val report = PokecomGoExpectationComparator.compare(session, expectation)

        assertEquals(
            listOf("programCounter", "currentProgramCounter", "opcode", "q", "ib", "testPort"),
            report.cpuFields,
        )
        assertEquals(8, report.internalRamByteDifferences)
        assertEquals(mapOf("8000" to 0, "f800" to 0), report.memoryByteDifferences)
        assertEquals(0, report.displayDotDifferences)
        assertEquals(true, report.displayMetadataMatches)
    }

    private fun findRequiredFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull(File::isFile)
            ?: error("Required local integration file was not found: $path")

    private companion object {
        const val ENABLE_ENV: String = "PGP_VERIFY_POKECOM_GO_STATE"
        const val INPUT_PATH: String = "local-data/golden/pokecom-go-preferences.xml"
        const val CYCLE_CAPTURE_PATH: String =
            "local-data/golden/pokecom-go-boot-1000000-cycle-timers-preferences.xml"
        const val ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
        const val BOOT_CYCLES: Long = 1_000_000
    }
}
