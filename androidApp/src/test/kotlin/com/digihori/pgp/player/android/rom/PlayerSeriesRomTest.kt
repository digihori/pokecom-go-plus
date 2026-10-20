package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.player.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class PlayerSeriesRomTest {
    private val groups = listOf(listOf("1250", "1251", "1255"), listOf("1260", "1261"), listOf("1401", "1402"))
    private fun id(name: String) = MachineId("pc-$name")
    private fun rom(name: String, revision: Byte = 0): RomSet {
        val internal = ByteArray(0x2000) { 0x4d }
        byteArrayOf(0x79, 0, 0).copyInto(internal)
        val imported = when (PlayerMachineCatalog.romId(id(name)).value) {
            "pc-1251" -> Pc1251ComponentRomImporter.importImages(internal, ByteArray(0x4000) { revision })
            "pc-1261" -> Pc1261ComponentRomImporter.importImages(internal, ByteArray(0x8000) { revision })
            else -> Pc1401ComponentRomImporter.importImages(internal, ByteArray(0x8000) { revision })
        }
        return RomSet(id(name), assertIs<RomImportResult.Success>(imported).romSet.components)
    }

    @Test fun everyPackageIdentityAndFlatImportCanSelectOnlyItsOwnSeries() {
        for (group in groups) for (source in group) {
            val bytes = AndroidRomPackage.write(rom(source))
            for (target in group) {
                val parsed = assertNotNull(PlayerRomDecoder.decode(bytes, id(target)))
                assertEquals(id(target), parsed.machineId)
                assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(parsed))
                assertEquals(id(target), assertNotNull(PlayerRomDecoder.decode(ByteArray(65536), id(target))).machineId)
                assertNull(PlayerRomDecoder.decode(bytes.copyOf(bytes.size / 2), id(target)))
            }
            for (other in listOf("1245", "1350", "1360", "1360k") + groups.flatten().filter { it !in group }) {
                assertNull(PlayerRomDecoder.decode(bytes, id(other)))
            }
        }
    }

    @Test fun oldPathsReplacementAllAliasesRestoreAndFamilyDeletion() = withLibrary { root ->
        val library = AndroidRomLibrary(root)
        val originalPackages = groups.associate { group ->
            val representative = PlayerMachineCatalog.romId(id(group.first()))
            val original = rom(representative.value.removePrefix("pc-"))
            val bytes = AndroidRomPackage.write(original)
            library.install(bytes, original, "original.pgrom")
            representative to bytes
        }
        for (group in groups) {
            val representative = PlayerMachineCatalog.romId(id(group.first()))
            assertTrue(File(root, "rom-library/${representative.value}/active.pgrom").isFile)
            for (alias in group) {
                assertContentEquals(originalPackages.getValue(representative), library.load(id(alias)))
                library.select(id(alias))
                val restored = AndroidRomLibrary(root)
                assertEquals(id(alias), restored.activeMachineId())
                val parsed = assertNotNull(PlayerRomDecoder.decode(assertNotNull(restored.loadActive()), id(alias)))
                assertEquals(id(alias), assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(parsed)).session.machineId)
            }
            val replacement = rom(group.last(), 1)
            val bytes = AndroidRomPackage.write(replacement)
            library.install(bytes, replacement, "replacement.pgrom")
            assertEquals(id(group.last()), library.activeMachineId())
            for (alias in group) assertContentEquals(bytes, library.load(id(alias)))
            for ((other, original) in originalPackages) if (other != representative) assertContentEquals(original, library.load(other))
            library.delete(id(group.first())) // deleting any alias removes the shared ROM and active runtime model
            assertNull(library.activeMachineId())
            for (alias in group) assertNull(library.load(id(alias)))
            library.install(originalPackages.getValue(representative), rom(representative.value.removePrefix("pc-")), "original.pgrom")
        }
        library.select(id("1402"))
        library.delete(id("1250")) // inactive shared-ROM deletion must retain another series' active alias
        assertEquals(id("1402"), library.activeMachineId())
        assertContentEquals(originalPackages.getValue(id("1401")), library.loadActive())
        library.install(originalPackages.getValue(id("1251")), rom("1251"), "original.pgrom")
        val rows = library.entries(PlayerMachineCatalog.homeDefinitions.map { it.id })
        assertEquals(10, rows.size)
        assertEquals(groups.size, rows.count { it.installed })
    }

    @Test fun familyReplacementFailurePreservesPackageAndExactActiveAlias() = withLibrary { root ->
        val library = AndroidRomLibrary(root)
        for (group in groups) {
            val original = rom(group.first())
            val bytes = AndroidRomPackage.write(original)
            library.install(bytes, original, "original.pgrom")
            library.select(id(group.last()))
            val representative = PlayerMachineCatalog.romId(id(group.first()))
            val metadata = File(root, "rom-library/${representative.value}/metadata.properties")
            assertTrue(metadata.delete());assertTrue(metadata.mkdir())
            File(metadata, "blocker").writeText("block")
            val changed = rom(group.last(), 1)
            assertFails { library.install(AndroidRomPackage.write(changed), changed, "replacement.pgrom") }
            assertEquals(id(group.last()), library.activeMachineId())
            group.forEach { assertContentEquals(bytes, library.load(id(it))) }
        }
    }

    private fun withLibrary(action: (File) -> Unit) {
        val root = Files.createTempDirectory("player-series-test-").toFile()
        try { action(root) } finally { root.deleteRecursively() }
    }
}
