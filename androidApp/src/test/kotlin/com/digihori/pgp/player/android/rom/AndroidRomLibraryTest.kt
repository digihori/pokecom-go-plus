package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

class AndroidRomLibraryTest {
    @Test
    fun `installed package is loaded from app private root`() {
        val root = Files.createTempDirectory("player-rom-library-test")
        try {
            val imported = Pc1245FlatRomImporter.importImage(
                ByteArray(Pc1245RomDefinition.COMPACT_LEGACY_IMAGE_SIZE),
            ) as RomImportResult.Success
            val packageBytes = AndroidRomPackage.write(imported.romSet)
            val library = AndroidRomLibrary(root.toFile())

            library.install(packageBytes, imported.romSet, "owned-rom.bin")

            assertContentEquals(
                packageBytes,
                assertNotNull(library.load(Pc1245RomDefinition.MACHINE_ID)),
            )
            assertContentEquals(packageBytes, assertNotNull(library.loadActive()))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

class AndroidRomLibraryManagementTest {
    private fun rom(fill: Int = 0) = (Pc1245FlatRomImporter.importImage(
        ByteArray(Pc1245RomDefinition.COMPACT_LEGACY_IMAGE_SIZE) { fill.toByte() },
    ) as RomImportResult.Success).romSet

    @Test fun multipleMachinesSelectionAndDeleteAreIndependent() {
        val root = Files.createTempDirectory("player-rom-management-test").toFile()
        try {
            val library = AndroidRomLibrary(root)
            val first = rom()
            val second = (com.digihori.pgp.core.emulator.machine.pc1350.Pc1350FlatRomImporter.importImage(ByteArray(65536)) as RomImportResult.Success).romSet
            val a = AndroidRomPackage.write(first)
            val b = AndroidRomPackage.write(second)
            library.install(a, first, "1245.bin")
            library.install(b, second, "1350.pgrom")
            kotlin.test.assertEquals(second.machineId, library.activeMachineId())
            library.select(first.machineId)
            assertContentEquals(a, assertNotNull(library.loadActive()))
            val entries = library.entries(listOf(first.machineId, second.machineId))
            kotlin.test.assertTrue(entries.all { it.installed })
            kotlin.test.assertEquals("1245.bin", entries.first().sourceName)
            library.delete(second.machineId)
            assertContentEquals(a, assertNotNull(library.loadActive()))
            kotlin.test.assertNull(library.load(second.machineId))
            library.delete(first.machineId)
            kotlin.test.assertNull(library.loadActive())
            kotlin.test.assertNull(library.activeMachineId())
        } finally { root.deleteRecursively() }
    }

    @Test fun failedReplacementRestoresPreviousPackageAndActiveSelection() {
        val root = Files.createTempDirectory("player-rom-rollback-test").toFile()
        try {
            val library = AndroidRomLibrary(root)
            val original = rom()
            val bytes = AndroidRomPackage.write(original)
            library.install(bytes, original, "original.bin")
            val active = java.io.File(root, "active-rom-machine-id").readBytes()
            val metadata = java.io.File(root, "rom-library/${original.machineId.value}/metadata.properties")
            kotlin.test.assertTrue(metadata.delete())
            kotlin.test.assertTrue(metadata.mkdir())
            java.io.File(metadata, "blocker").writeText("not an empty directory")
            val replacement = rom(1)
            kotlin.test.assertFails { library.install(AndroidRomPackage.write(replacement), replacement, "replacement.bin") }
            assertContentEquals(bytes, assertNotNull(library.load(original.machineId)))
            assertContentEquals(active, java.io.File(root, "active-rom-machine-id").readBytes())
        } finally { root.deleteRecursively() }
    }

    @Test fun libraryRejectsPathTraversalMachineId() {
        val root = Files.createTempDirectory("player-rom-path-test").toFile()
        try {
            val library = AndroidRomLibrary(root)
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                library.load(com.digihori.pgp.core.rom.MachineId("../outside"))
            }
        } finally { root.deleteRecursively() }
    }
}
