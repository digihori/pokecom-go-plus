package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251CharacterInput
import com.digihori.pgp.core.emulator.machine.pc1261.*
import com.digihori.pgp.core.emulator.machine.pc1401.*
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

/** Optional user firmware, never copied to test resources or used as a binary fixture. */
class RealCapacityFamilyRomTest {
    @Test fun locallySupplied1261RevisionRunsWithAll126xCapacities() {
        for (model in Pc1261FamilyModel.entries) {
            val directory = directory("pc-1261") ?: return
            val rom = assertIs<RomImportResult.Success>(Pc1261ComponentRomImporter.importImages(
                File(directory, "pc1261-int.bin").readBytes(), File(directory, "pc1261-ext.bin").readBytes(), model)).romSet
            val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
            exercise(s, model.basicStart, 0x6000, Pc1251CharacterInput::keySequence)
        }
    }
    @Test fun locallySupplied140xRomsRunWithTheirSelectedCapacities() {
        for (model in Pc1401FamilyModel.entries) {
            val directory = directory(model.machineId.value) ?: continue
            val rom = assertIs<RomImportResult.Success>(Pc1401ComponentRomImporter.importImages(
                File(directory, "${model.machineId.value.replace("-", "")}-int.bin").readBytes(), File(directory, "${model.machineId.value.replace("-", "")}-ext.bin").readBytes(), model)).romSet
            val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
            exercise(s, model.basicStart, 0x4400, Pc1401CharacterInput::keySequence)
        }
    }
    private fun exercise(s: EmulatorSession, expectedStart: Int, output: Int, keys: (Char) -> List<PocketKey>?) {
        assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        s.setOperatingMode(OperatingMode.PROGRAM); s.runCycles(1_000_000)
        fun type(text: String) {
            for (key in text.flatMap { checkNotNull(keys(it)) } + PocketKey.ENTER) {
                s.pressKey(key); s.runCycles(80_000); s.releaseKey(key); s.runCycles(40_000)
            }
            assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        }
        type("NEW")
        val pointer = s.memorySnapshot(s.memoryLayout.basicStartPointerAddress, 2).copyBytes()
        assertEquals(expectedStart, (pointer[0].toInt() and 255) or ((pointer[1].toInt() and 255) shl 8), s.machineId.value)
        val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram("10 POKE $output,123\n20 END".encodeToByteArray(), s.machineId)).bytes
        assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(program))
        s.setOperatingMode(OperatingMode.RUN); s.runCycles(1_000_000); type("RUN")
        assertEquals(123, s.memorySnapshot(output, 1).copyBytes()[0].toInt() and 255, s.machineId.value)
    }
    private fun directory(id: String): File? = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, "local-data/roms/$id") }.firstOrNull { File(it, "${id.replace("-", "")}-int.bin").isFile && File(it, "${id.replace("-", "")}-ext.bin").isFile }
}
