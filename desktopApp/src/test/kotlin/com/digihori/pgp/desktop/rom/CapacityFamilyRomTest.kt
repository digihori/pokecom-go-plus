package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.PlayerKeyboardCatalog
import com.digihori.pgp.player.PlayerMachineCatalog
import kotlin.test.*

class CapacityFamilyRomTest {
    @Test fun registrationHasOneFamilyRowWhileMachineSelectionKeepsAllModels() {
        val rows = DesktopRomRegistrationCatalog.definitions.map { it.id.value }
        assertTrue("pc-1261" in rows); assertFalse("pc-1260" in rows)
        assertTrue("pc-1401" in rows); assertFalse("pc-1402" in rows)
        assertTrue("pc-1251" in rows); assertFalse("pc-1250" in rows); assertFalse("pc-1255" in rows)
        assertEquals("PC-1261 family", DesktopRomRegistrationCatalog.label(MachineId("pc-1260")))
        assertEquals("PC-1401 family", DesktopRomRegistrationCatalog.label(MachineId("pc-1402")))
        assertTrue(MachineCatalog.find(MachineId("pc-1260")) != null)
        assertTrue(MachineCatalog.find(MachineId("pc-1402")) != null)
    }
    @Test fun librarySharesOneFirmwareRegistrationWithinEachCapacityFamily() {
        val directory = java.nio.file.Files.createTempDirectory("pgp-capacity-library").toFile()
        try {
            val library = DesktopRomLibrary(directory)
            for (family in listOf(listOf("pc-1261", "pc-1260"), listOf("pc-1401", "pc-1402"))) {
                val base = MachineId(family.first())
                val bytes = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.createPackage(base, ByteArray(0x2000), ByteArray(0x8000))).packageBytes
                assertIs<DesktopRomLibraryResult.Success>(library.install(bytes, "user ROM revision"))
                assertEquals(java.io.File(directory, "${base.value}/active.pgrom"), library.packageFile(base))
                for (id in family.map(::MachineId)) {
                    assertEquals(library.packageFile(base), library.packageFile(id))
                    assertTrue(library.isInstalled(id))
                    assertEquals(id, assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackageForMachine(library.packageFile(id).readBytes(), id)).session.machineId)
                }
            }
        } finally { directory.deleteRecursively() }
    }
    @Test fun selectedModelSurvivesLegacyImportPackagingAndLibraryLoadingForAnyRevision() {
        val ids = listOf("1260", "1261", "1401", "1402").map { MachineId("pc-$it") }
        for (id in ids) for (revision in listOf(0, 17)) {
            val image = ByteArray(0x10000) { revision.toByte() }
            val converted = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.convertLegacyImage(id, image))
            val componentPackage = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.createPackage(id, ByteArray(0x2000) { revision.toByte() }, ByteArray(0x8000) { revision.toByte() }))
            for (bytes in listOf(converted.packageBytes, componentPackage.packageBytes)) {
                val read = assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(bytes))
                assertEquals(id, read.romSet.machineId)
                val s = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackageForMachine(bytes, id)).session
                assertEquals(id, s.machineId)
                for (other in ids.filter { it != id }) {
                    val loaded = DesktopRomLoader.loadPackageForMachine(bytes, other)
                    if (MachineCatalog.require(other).family == MachineCatalog.require(id).family) {
                        assertEquals(other, assertIs<DesktopRomLoadResult.Success>(loaded).session.machineId)
                    } else assertIs<DesktopRomLoadResult.Failure>(loaded)
                }
                val keyboard = PlayerKeyboardCatalog.forMachine(id)
                val keys = keyboard?.let { (it.rows + it.functionPanel?.rows.orEmpty()).flatten().map { cap -> cap.key } }
                    ?: listOf(PocketKey.A, PocketKey.SHIFT, PocketKey.ENTER, PocketKey.BREAK)
                for (key in keys) {
                    assertIs<InputResult.Accepted>(s.pressKey(key), "${id.value} $key"); s.releaseKey(key)
                }
            }
            assertEquals(id, assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadLegacyImage(id, image)).session.machineId)
            assertEquals(32_768L, DesktopRomSetCreator.externalSize(id))
        }
        for (id in listOf("pc-1260", "pc-1402")) {
            assertTrue(PlayerMachineCatalog.definitions.any { it.id.value == id }, "Player now supports the capacity models")
            assertFalse(PlayerMachineCatalog.homeDefinitions.any { it.id.value == id }, "Shared-ROM models keep one home row per series")
        }
    }
}
