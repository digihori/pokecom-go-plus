package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.EmulatorConfiguration
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopRomLoaderTest {
    @Test
    fun createsAPc1245SessionFromALegacyImage() {
        val result = DesktopRomLoader.loadPc1245LegacyImage(
            ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE),
        )

        val success = assertIs<DesktopRomLoadResult.Success>(result)
        assertEquals(Pc1245RomDefinition.MACHINE_ID, success.session.machineId)
    }

    @Test
    fun reportsTheActualSizeOfAnInvalidImage() {
        val result = DesktopRomLoader.loadPc1245LegacyImage(ByteArray(42))

        val failure = assertIs<DesktopRomLoadResult.Failure>(result)
        val invalid = assertIs<DesktopRomLoadError.InvalidRom>(failure.error)
        val size = assertIs<RomImportError.InvalidImageSize>(invalid.error)
        assertEquals(Pc1245RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES, size.expected)
        assertEquals(42, size.actual)
    }

    @Test
    fun createsAPc1251SessionFromALegacyImage() {
        val result = DesktopRomLoader.loadLegacyImage(
            Pc1251RomDefinition.MACHINE_ID,
            ByteArray(Pc1251RomDefinition.LEGACY_IMAGE_SIZE),
        )

        assertEquals(
            Pc1251RomDefinition.MACHINE_ID,
            assertIs<DesktopRomLoadResult.Success>(result).session.machineId,
        )
    }

    @Test
    fun createsAPc1350SessionFromALegacyImage() {
        val result = DesktopRomLoader.loadLegacyImage(
            Pc1350RomDefinition.MACHINE_ID,
            ByteArray(Pc1350RomDefinition.LEGACY_IMAGE_SIZE),
        )

        val session = assertIs<DesktopRomLoadResult.Success>(result).session
        assertEquals(Pc1350RomDefinition.MACHINE_ID, session.machineId)
        assertEquals(4, session.displaySnapshot().characterRows)
    }

    @Test
    fun passesTheSelectedFamilyMemoryModeToTheSession() {
        val image = ByteArray(Pc1251RomDefinition.LEGACY_IMAGE_SIZE)
        val hardwareSession = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadLegacyImage(
                Pc1251FamilyModel.PC_1250.machineId,
                image,
                EmulatorConfiguration(Pc1251FamilyMemoryMode.HARDWARE),
            ),
        ).session
        val expandedSession = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadLegacyImage(
                Pc1251FamilyModel.PC_1250.machineId,
                image,
                EmulatorConfiguration(Pc1251FamilyMemoryMode.EXPANDED),
            ),
        ).session
        val memoryImage = assertIs<PgpMemoryDumpParseResult.Success>(
            PgpMemoryDumpParser.parse("A000 55"),
        ).image

        assertIs<MemoryImageLoadResult.Failure>(hardwareSession.loadMemoryImage(memoryImage))
        assertIs<MemoryImageLoadResult.Success>(expandedSession.loadMemoryImage(memoryImage))
    }

    @Test
    fun createsASessionFromAPgpromPackage() {
        val image = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE)
        val romSet = assertIs<com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult.Success>(
            com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter.importImage(image),
        ).romSet

        val success = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadPackage(DesktopRomPackage.write(romSet)),
        )

        assertEquals(Pc1245RomDefinition.MACHINE_ID, success.session.machineId)
    }
}
