package com.digihori.pgp.core.rom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RomPackageValidatorTest {
    @Test
    fun createsARomSetFromAValidManifest() {
        val bytes = byteArrayOf(1, 2, 3)
        val result = RomPackageValidator.validate(
            manifest(component(size = 3, sha256 = SHA)),
            mapOf("rom/internal.bin" to (RomPackageEntryInfo("rom/internal.bin", 3, SHA) to bytes)),
        )

        val success = assertIs<RomPackageValidationResult.Success>(result)
        assertEquals(MachineId("pc-1245"), success.romSet.machineId)
        assertEquals(3, success.romSet.components.single().size)
    }

    @Test
    fun reportsStructuralAndContentErrorsTogether() {
        val result = RomPackageValidator.validate(
            manifest(
                component(file = "../internal.bin", size = 4, sha256 = "BAD"),
                component(file = "../internal.bin", size = 4, sha256 = "BAD"),
            ),
            emptyMap(),
        )

        val errors = assertIs<RomPackageValidationResult.Failure>(result).errors
        assertTrue(errors.any { it is RomPackageValidationError.DuplicateComponentId })
        assertTrue(errors.any { it is RomPackageValidationError.DuplicateComponentPath })
        assertTrue(errors.any { it is RomPackageValidationError.InvalidPath })
        assertTrue(errors.any { it is RomPackageValidationError.InvalidSha256 })
        assertTrue(errors.any { it is RomPackageValidationError.MissingComponentFile })
    }

    @Test
    fun warnsAboutUnlistedFiles() {
        val entries = mapOf(
            "rom/internal.bin" to (RomPackageEntryInfo("rom/internal.bin", 3, SHA) to byteArrayOf(1, 2, 3)),
            "notes.txt" to (RomPackageEntryInfo("notes.txt", 0, SHA) to byteArrayOf()),
        )
        val success = assertIs<RomPackageValidationResult.Success>(
            RomPackageValidator.validate(manifest(component(size = 3, sha256 = SHA)), entries),
        )

        assertEquals(listOf(RomPackageWarning.UnlistedEntry("notes.txt")), success.warnings)
    }

    private fun manifest(vararg components: RomPackageComponentManifest) = RomPackageManifest(
        format = RomPackageValidator.FORMAT,
        formatVersion = RomPackageValidator.VERSION,
        machineId = "pc-1245",
        components = components.toList(),
    )

    private fun component(
        file: String = "rom/internal.bin",
        size: Int = 0,
        sha256: String = SHA,
    ) = RomPackageComponentManifest("internal", "internal", file, size, sha256)

    private companion object {
        const val SHA = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
