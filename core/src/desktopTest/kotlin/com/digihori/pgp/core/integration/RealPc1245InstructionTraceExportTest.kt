package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs

class RealPc1245InstructionTraceExportTest {
    @Test
    fun exportsTheRealRomBootTraceWhenExplicitlyEnabled() {
        if (System.getenv(ENABLE_ENV) != "1") return

        val rom = findRequiredFile(ROM_PATH)
        val romSet = assertIs<RomImportResult.Success>(
            Pc1245FlatRomImporter.importImage(rom.readBytes()),
        ).romSet
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, romSet),
        ).session
        val trace = InstructionTraceRecorder.recordUntil(
            session = session,
            cycleBudget = BOOT_CYCLES,
            tailCapacity = traceCapacity(),
        )

        val output = outputFile(outputPath())
        output.parentFile.mkdirs()
        output.writeText(trace.toTsv())
        check(output.readText() == trace.toTsv()) { "Instruction trace round-trip failed" }
    }

    private fun findRequiredFile(path: String): File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull(File::isFile)
            ?: error("Required local integration file was not found: $path")

    private fun outputFile(path: String): File {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repository root")
        return File(root, path)
    }

    private fun traceCapacity(): Int =
        if (System.getenv(FULL_TRACE_ENV) == "1") FULL_TRACE_SIZE else TRACE_TAIL_SIZE

    private fun outputPath(): String =
        if (System.getenv(FULL_TRACE_ENV) == "1") FULL_OUTPUT_PATH else OUTPUT_PATH

    private companion object {
        const val ENABLE_ENV = "PGP_EXPORT_INSTRUCTION_TRACE"
        const val FULL_TRACE_ENV = "PGP_EXPORT_FULL_INSTRUCTION_TRACE"
        const val ROM_PATH = "local-data/roms/pc-1245/pc1245mem.bin"
        const val OUTPUT_PATH = "local-data/golden/pc1245-pgp-trace-1000000-tail.tsv"
        const val FULL_OUTPUT_PATH = "local-data/golden/pc1245-pgp-trace-1000000-full.tsv"
        const val BOOT_CYCLES = 1_000_000L
        const val TRACE_TAIL_SIZE = 256
        const val FULL_TRACE_SIZE = 300_000
    }
}
