package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorConfiguration
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomBasicInput
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1251RomSmokeTest {
    @Test
    fun locallySuppliedRomBootsAndExposesThePc1251Display() {
        val session = createLocalSession() ?: return

        val result = session.runCycles(BOOT_CYCLE_BUDGET)
        val display = session.displaySnapshot()

        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertTrue(result.executedCycles >= BOOT_CYCLE_BUDGET)
        assertEquals(Pc1251RomDefinition.MACHINE_ID, session.machineId)
        assertEquals(24, display.characterColumns)
        assertEquals(120, display.dotColumns)
        assertTrue(DisplaySymbol.RUN in display.symbols)
    }

    @Test
    fun locallySuppliedRomAcceptsAllThreeModeSwitchPositions() {
        val session = createLocalSession() ?: return
        session.runCycles(BOOT_CYCLE_BUDGET)

        session.setOperatingMode(OperatingMode.PROGRAM)
        assertTrue(DisplaySymbol.PRO in session.displaySnapshot().symbols)

        session.setOperatingMode(OperatingMode.RESERVE)
        assertTrue(DisplaySymbol.RESERVE in session.displaySnapshot().symbols)

        session.setOperatingMode(OperatingMode.RUN)
        assertTrue(DisplaySymbol.RUN in session.displaySnapshot().symbols)
    }

    @Test
    fun locallySuppliedRomAcceptsAKeyAfterBoot() {
        val session = createLocalSession() ?: return
        session.runCycles(BOOT_CYCLE_BUDGET)
        val before = session.displaySnapshot().copyDots()

        session.pressKey(PocketKey.A)
        session.runCycles(KEY_HOLD_CYCLES)
        session.releaseKey(PocketKey.A)
        session.runCycles(KEY_SETTLE_CYCLES)

        assertTrue(
            !before.contentEquals(session.displaySnapshot().copyDots()),
            "PC-1251 ROM did not update the LCD after the A key was pressed",
        )
    }

    @Test
    fun locallySuppliedRomStoresParenthesesTypedThroughPc1251Keys() {
        val session = createLocalSession() ?: return
        session.runCycles(BOOT_CYCLE_BUDGET)
        session.setOperatingMode(OperatingMode.PROGRAM)
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse(BASIC_SOURCE),
        ).document
        val keys = assertIs<Pc1245RomInputResult.Success>(
            Pc1251RomBasicInput.compile(document),
        ).keys

        keys.forEach { key -> session.tap(key) }
        session.runCycles(BASIC_LINE_SETTLE_CYCLES)

        val program = assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot())
        val decoded = assertIs<Pc1245BasicDetokenizeResult.Success>(
            Pc1245BasicDetokenizer.detokenize(program.copyBytes()),
        )
        assertEquals(BASIC_SOURCE, decoded.source)
    }

    @Test
    fun pc1251RomBootsWithEveryFamilyHardwareRamRange() {
        if (findLocalRom() == null) return
        val expectedProgramStarts = mapOf(
            // PC-1250 uses the B800 mirror; B830 resolves to physical C030 RAM.
            Pc1251FamilyModel.PC_1250 to 0xb830,
            Pc1251FamilyModel.PC_1251 to 0xb830,
            Pc1251FamilyModel.PC_1255 to 0xa030,
        )

        expectedProgramStarts.forEach { (model, expectedStart) ->
            val session = requireNotNull(createLocalSession(model, Pc1251FamilyMemoryMode.HARDWARE))
            val result = session.runCycles(BOOT_CYCLE_BUDGET)
            val pointers = session.memorySnapshot(0xc6e1, 4).copyBytes()
            val actualStart = pointers[0].toInt() and 0xff or ((pointers[1].toInt() and 0xff) shl 8)

            assertIs<ExecutionStatus.Ready>(result.status)
            assertEquals(model.machineId, session.machineId)
            assertEquals(expectedStart, actualStart, model.name)
        }
    }

    private fun createLocalSession(
        model: Pc1251FamilyModel = Pc1251FamilyModel.PC_1251,
        memoryMode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED,
    ) = findLocalRom()?.let { romFile ->
        val imported = assertIs<RomImportResult.Success>(
            Pc1251FlatRomImporter.importImage(romFile.readBytes(), model.machineId),
        )
        assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(
                model.machineId,
                imported.romSet,
                EmulatorConfiguration(memoryMode),
            ),
        ).session
    }

    private fun findLocalRom(): File? {
        val configuredPath = System.getenv(ROM_PATH_ENV)
        if (!configuredPath.isNullOrBlank()) {
            val configuredFile = File(configuredPath)
            if (configuredFile.isAbsolute) return configuredFile.takeIf(File::isFile)
            return findFromWorkingDirectoryAncestors(configuredPath)
        }
        return findFromWorkingDirectoryAncestors(DEFAULT_ROM_PATH)
    }

    private fun findFromWorkingDirectoryAncestors(path: String): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull(File::isFile)

    private fun EmulatorSession.tap(key: PocketKey) {
        pressKey(key)
        assertIs<ExecutionStatus.Ready>(runCycles(KEY_HOLD_CYCLES).status)
        releaseKey(key)
        assertIs<ExecutionStatus.Ready>(runCycles(KEY_GAP_CYCLES).status)
    }

    private companion object {
        const val ROM_PATH_ENV: String = "PGP_PC1251_ROM"
        const val DEFAULT_ROM_PATH: String = "local-data/roms/pc-1251/pc1251mem.bin"
        const val BOOT_CYCLE_BUDGET: Long = 1_000_000
        const val KEY_HOLD_CYCLES: Long = 38_400
        const val KEY_GAP_CYCLES: Long = 19_200
        const val KEY_SETTLE_CYCLES: Long = 57_600
        const val BASIC_LINE_SETTLE_CYCLES: Long = 57_600
        const val BASIC_SOURCE: String = "10 PRINT (1)"
    }
}
