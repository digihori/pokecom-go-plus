package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidRomPackageTest {
    @Test
    fun `round trip creates a canonical valid package`() {
        val legacy = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE) { it.toByte() }
        val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(legacy))

        val encoded = AndroidRomPackage.write(imported.romSet)
        val decoded = assertIs<RomPackageReadResult.Success>(AndroidRomPackage.read(encoded))

        assertEquals(Pc1245RomDefinition.MACHINE_ID, decoded.romSet.machineId)
        assertEquals(imported.romSet.components.map { it.id }, decoded.romSet.components.map { it.id })
        imported.romSet.components.zip(decoded.romSet.components).forEach { (expected, actual) ->
            assertContentEquals(expected.copyBytes(), actual.copyBytes())
        }
    }

    @Test
    fun `archive traversal path is rejected`() {
        val encoded = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("../manifest.json"))
                zip.write(byteArrayOf(1))
                zip.closeEntry()
            }
            output.toByteArray()
        }

        assertEquals(RomPackageReadResult.Failure, AndroidRomPackage.read(encoded))
    }
}
