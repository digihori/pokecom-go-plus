package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineRomLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopRomPackageConverterTest {
    @Test
    fun createsAPackageForEveryCatalogMachine() {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x11 }

        MachineCatalog.definitions.forEach { definition ->
            val externalSize = when (definition.romLayout) {
                MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_16K -> 0x4000
                MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_32K -> 0x8000
                MachineRomLayout.SC61860_INTERNAL_8K_BANKED_16K_X8 -> 0x20000
            }
            val external = ByteArray(externalSize) { 0x22 }
            val converted = assertIs<DesktopRomPackageConversionResult.Success>(
                DesktopRomPackageConverter.createPackage(definition.id, internal, external),
                definition.displayName,
            )
            val read = assertIs<DesktopRomPackageReadResult.Success>(
                DesktopRomPackage.read(converted.packageBytes),
                definition.displayName,
            )

            assertEquals(definition.id, read.romSet.machineId)
        }
    }

    @Test
    fun createsAPackageFromSeparatePhysicalRomDumps() {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x11 }
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x22 }

        val converted = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.createPc1245Package(internal, external),
        )
        val romSet = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(converted.packageBytes),
        ).romSet

        kotlin.test.assertContentEquals(internal, romSet.components[0].copyBytes())
        kotlin.test.assertContentEquals(external, romSet.components[1].copyBytes())
    }

    @Test
    fun convertsALegacyImageToAReadablePackage() {
        val image = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE) { it.toByte() }

        val converted = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.convertPc1245LegacyImage(image),
        )
        val read = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(converted.packageBytes),
        )

        assertEquals(Pc1245RomDefinition.MACHINE_ID, read.romSet.machineId)
        assertEquals(Pc1245RomDefinition.INTERNAL_SIZE, read.romSet.components[0].size)
        assertEquals(Pc1245RomDefinition.EXTERNAL_SIZE, read.romSet.components[1].size)
    }

    @Test
    fun rejectsAnInvalidLegacyImageBeforeWritingAPackage() {
        val failure = assertIs<DesktopRomPackageConversionResult.Failure>(
            DesktopRomPackageConverter.convertPc1245LegacyImage(ByteArray(42)),
        )

        assertEquals(
            RomImportError.InvalidImageSize(Pc1245RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES, 42),
            failure.error,
        )
    }

    @Test
    fun compactAndFullImagesProduceTheSamePackageContents() {
        val compact = ByteArray(Pc1245RomDefinition.COMPACT_LEGACY_IMAGE_SIZE) { it.toByte() }
        val full = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE) { index ->
            if (index < compact.size) compact[index] else 0x55
        }

        val compactPackage = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.convertPc1245LegacyImage(compact),
        )
        val fullPackage = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.convertPc1245LegacyImage(full),
        )
        val compactSet = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(compactPackage.packageBytes),
        ).romSet
        val fullSet = assertIs<DesktopRomPackageReadResult.Success>(
            DesktopRomPackage.read(fullPackage.packageBytes),
        ).romSet

        compactSet.components.zip(fullSet.components).forEach { (first, second) ->
            kotlin.test.assertContentEquals(first.copyBytes(), second.copyBytes())
        }
    }
}
