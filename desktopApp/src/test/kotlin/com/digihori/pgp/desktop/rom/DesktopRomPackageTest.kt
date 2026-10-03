package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomPackageValidationError
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopRomPackageTest {
    @Test
    fun writesAndReadsAPc1245Package() {
        val image = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE) { it.toByte() }
        val source = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(image)).romSet

        val packageBytes = DesktopRomPackage.write(source, "PC-1245 ROM")
        val result = assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(packageBytes))

        assertEquals(source.machineId, result.romSet.machineId)
        assertEquals("PC-1245 ROM", result.title)
        assertEquals(emptyList(), result.warnings)
        source.components.zip(result.romSet.components).forEach { (expected, actual) ->
            assertEquals(expected.id, actual.id)
            assertContentEquals(expected.copyBytes(), actual.copyBytes())
        }
    }

    @Test
    fun rejectsAComponentWhoseDigestDoesNotMatch() {
        val manifest = """{
          "format":"pgp-rom-package","formatVersion":1,"machineId":"pc-1245",
          "components":[{"id":"internal","role":"internal","file":"rom/internal.bin",
          "size":1,"sha256":"${"0".repeat(64)}"}]
        }""".trimIndent()
        val packageBytes = zipOf("manifest.json" to manifest.encodeToByteArray(), "rom/internal.bin" to byteArrayOf(1))

        val error = assertIs<DesktopRomPackageReadResult.Failure>(DesktopRomPackage.read(packageBytes)).error
        val validation = assertIs<DesktopRomPackageError.Validation>(error)
        assertIs<RomPackageValidationError.Sha256Mismatch>(validation.errors.single())
    }

    @Test
    fun rejectsPathTraversalBeforeReadingContent() {
        val result = DesktopRomPackage.read(zipOf("../manifest.json" to byteArrayOf()))

        assertEquals(
            DesktopRomPackageError.UnsafePath("../manifest.json"),
            assertIs<DesktopRomPackageReadResult.Failure>(result).error,
        )
    }

    private fun zipOf(vararg files: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                files.forEach { (path, bytes) ->
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
}
