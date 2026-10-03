package com.digihori.pgp.core.integration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenSnapshotExporterTest {
    @Test
    fun exportedSyntheticBootCanBeDecodedAndReplayed() {
        val romImage = ByteArray(0x10000) { 0x33 }
        val content = GoldenSnapshotExporter.exportPc1245Boot(
            romImage = romImage,
            producerRevision = "synthetic-export-test",
            cycleBudget = 6,
        )
        val goldenCase = GoldenScenarioRunner.decode(content)

        assertEquals("pc1245-pgp-boot-6", goldenCase.caseId)
        GoldenScenarioRunner.run(goldenCase, romImage)
    }

    @Test
    fun exportsLocallySuppliedRomOnlyWhenExplicitlyRequested() {
        if (System.getenv(EXPORT_ENV) != "1") return

        val romFile = requireNotNull(findFile(System.getenv(ROM_PATH_ENV) ?: DEFAULT_ROM_PATH)) {
            "Cannot find PC-1245 ROM; set $ROM_PATH_ENV"
        }
        val revision = requireNotNull(System.getenv(REVISION_ENV)?.takeIf(String::isNotBlank)) {
            "$REVISION_ENV is required when exporting Golden Data"
        }
        val content = GoldenSnapshotExporter.exportPc1245Boot(romFile.readBytes(), revision)
        val outputFile = findRepositoryRoot().resolve(DEFAULT_OUTPUT_PATH)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)

        GoldenScenarioRunner.run(GoldenScenarioRunner.decode(outputFile.readText()), romFile.readBytes())
    }

    private fun findFile(path: String): File? {
        val file = File(path)
        if (file.isAbsolute) return file.takeIf(File::isFile)
        return ancestors().map { File(it, path) }.firstOrNull(File::isFile)
    }

    private fun findRepositoryRoot(): File = requireNotNull(
        ancestors().firstOrNull { File(it, "settings.gradle.kts").isFile },
    ) { "Cannot find repository root" }

    private fun ancestors(): Sequence<File> =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }

    private companion object {
        const val EXPORT_ENV: String = "PGP_EXPORT_GOLDEN"
        const val ROM_PATH_ENV: String = "PGP_PC1245_ROM"
        const val REVISION_ENV: String = "PGP_GOLDEN_PRODUCER_REVISION"
        const val DEFAULT_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
        const val DEFAULT_OUTPUT_PATH: String = "local-data/golden/pc1245-pgp-boot-1000000.json"
    }
}
