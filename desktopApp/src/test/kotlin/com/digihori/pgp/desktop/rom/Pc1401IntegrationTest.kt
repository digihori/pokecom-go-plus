package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.basic.*
import kotlin.test.*

class Pc1401IntegrationTest {
    @Test fun componentAndFlatPackagesPreserveMachineIdentity() {
        val id = Pc1401RomDefinition.MACHINE_ID
        val components = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.createPackage(id, ByteArray(0x2000), ByteArray(0x8000)))
        val loaded = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(components.packageBytes))
        assertEquals(id, loaded.session.machineId)
        assertIs<DesktopRomLoadResult.Failure>(DesktopRomLoader.loadPackageForMachine(components.packageBytes, MachineId("pc-1261")))
        val flat = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.convertLegacyImage(id, ByteArray(0x10000)))
        assertEquals(id, assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(flat.packageBytes)).session.machineId)
    }

    @Test fun localRomEditorWaitsForProgramModeBeforeGeneratedInput() {
        val rom = DesktopRomLocator.findPc1401Rom() ?: return
        val core = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadLegacyImage(Pc1401RomDefinition.MACHINE_ID, rom.readBytes())).session
        core.runCycles(1_000_000)
        val runner = com.digihori.pgp.desktop.runner.DesktopEmulatorRunner(core)
        runner.setOperatingMode(OperatingMode.PROGRAM)
        val source = "10 POKE 16384,77\n"
        val keys = assertIs<DesktopBasicLoadResult.Success>(DesktopBasicLoader.compileRomInput(source.encodeToByteArray(), Pc1401RomDefinition.MACHINE_ID)).keys
        assertIs<ExecutionStatus.Ready>(runner.runKeySequenceImmediately(keys).status)
        assertEquals(OperatingMode.PROGRAM, core.currentOperatingMode())
        val actual = assertIs<BasicProgramSnapshotResult.Success>(core.basicProgramSnapshot()).copyBytes()
        val expected = assertIs<DesktopBasicProgramCompileResult.Success>(DesktopBasicLoader.compileProgram(source.encodeToByteArray(), Pc1401RomDefinition.MACHINE_ID)).bytes
        assertContentEquals(expected, actual)
    }

    @Test fun studioCompilesAndExportsPc1401ScientificVocabulary() {
        val id = Pc1401RomDefinition.MACHINE_ID
        val compiled = assertIs<DesktopBasicProgramCompileResult.Success>(DesktopBasicLoader.compileProgram("10 PRINT FAC(5),HSN(0)\n".encodeToByteArray(), id))
        val decoded = assertIs<DesktopBasicProgramDecodeResult.Success>(DesktopBasicLoader.detokenizeProgram(compiled.bytes, id))
        assertTrue("FAC(5)" in decoded.utf8Bytes.decodeToString())
        assertIs<DesktopBasicLoadResult.Success>(DesktopBasicLoader.compileRomInput("10 PRINT 1\n".encodeToByteArray(), id))
    }
}
