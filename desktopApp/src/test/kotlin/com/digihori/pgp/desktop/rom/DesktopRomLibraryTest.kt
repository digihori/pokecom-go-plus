package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopRomLibraryTest {
    @Test
    fun installsValidatedPackageAndRemovesIt() {
        val root = Files.createTempDirectory("pgp-rom-library").toFile()
        val library = DesktopRomLibrary(root)
        val machineId = Pc1245RomDefinition.MACHINE_ID
        val bytes = DesktopRomPackage.write(
            RomSet(machineId, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
            )),
        )

        assertIs<DesktopRomLibraryResult.Success>(library.install(bytes, "original.bin"))
        assertTrue(library.isInstalled(machineId))
        assertEquals(setOf(machineId), library.installedMachineIds())
        assertTrue(library.packageFile(machineId).isFile)
        assertTrue(library.remove(machineId))
        assertFalse(library.isInstalled(machineId))
    }

    @Test
    fun rejectsInvalidPackageWithoutReplacingInstalledRom() {
        val root = Files.createTempDirectory("pgp-rom-library").toFile()
        val library = DesktopRomLibrary(root)

        assertIs<DesktopRomLibraryResult.Failure>(library.install("not a package".encodeToByteArray(), "bad.bin"))
        assertTrue(library.installedMachineIds().isEmpty())
    }

    @Test
    fun sharesOneRomPackageAcrossThePc1251FamilyModels() {
        val root = Files.createTempDirectory("pgp-rom-library-family").toFile()
        val library = DesktopRomLibrary(root)
        val bytes = DesktopRomPackage.write(
            RomSet(Pc1251RomDefinition.MACHINE_ID, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
            )),
        )

        assertIs<DesktopRomLibraryResult.Success>(library.install(bytes, "pc1251.bin"))
        val familyIds = Pc1251FamilyModel.entries.map { it.machineId }.toSet()
        assertTrue(library.installedMachineIds().containsAll(familyIds))
        assertEquals(1, familyIds.map { library.packageFile(it).absolutePath }.distinct().size)

        val pc1250 = Pc1251FamilyModel.PC_1250.machineId
        val loaded = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadPackageForMachine(bytes, pc1250),
        )
        assertEquals(pc1250, loaded.session.machineId)

        assertTrue(library.remove(Pc1251FamilyModel.PC_1255.machineId))
        assertTrue(familyIds.none(library::isInstalled))
    }
}
