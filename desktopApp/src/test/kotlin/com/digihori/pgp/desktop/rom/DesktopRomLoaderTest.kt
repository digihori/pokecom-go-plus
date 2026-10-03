package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
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
