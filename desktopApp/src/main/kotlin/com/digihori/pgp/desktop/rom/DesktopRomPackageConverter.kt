package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360LegacyRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineFamily

internal object DesktopRomPackageConverter {
    fun convertLegacyImage(
        machineId: MachineId,
        image: ByteArray,
        bankImage: ByteArray? = null,
    ): DesktopRomPackageConversionResult {
        val imported = when (MachineCatalog.find(machineId)?.family) {
            MachineFamily.PC_1245 -> Pc1245FlatRomImporter.importImage(image)
            MachineFamily.PC_1251 -> com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FlatRomImporter
                .importImage(image, machineId)
            MachineFamily.PC_1350 -> com.digihori.pgp.core.emulator.machine.pc1350.Pc1350FlatRomImporter
                .importImage(image)
            MachineFamily.PC_1360 -> if (bankImage == null) {
                RomImportResult.Failure(RomImportError.InvalidImageSize(listOf(0x20000), 0))
            } else {
                Pc1360LegacyRomImporter.importImages(image, bankImage)
            }
            null -> RomImportResult.Failure(RomImportError.UnsupportedMachine(machineId))
        }
        return when (imported) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"),
            )
        }
    }

    fun createPackage(
        machineId: MachineId,
        internal: ByteArray,
        external: ByteArray,
    ): DesktopRomPackageConversionResult = when (MachineCatalog.find(machineId)?.family) {
        MachineFamily.PC_1245 -> createPc1245Package(internal, external)
        MachineFamily.PC_1251 -> createPc1251Package(internal, external, machineId)
        MachineFamily.PC_1350 -> createPc1350Package(internal, external)
        MachineFamily.PC_1360 -> createPc1360Package(internal, external)
        null -> DesktopRomPackageConversionResult.Failure(RomImportError.UnsupportedMachine(machineId))
    }

    fun createPc1245Package(
        internal: ByteArray,
        external: ByteArray,
    ): DesktopRomPackageConversionResult =
        when (val imported = Pc1245ComponentRomImporter.importImages(internal, external)) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "PC-1245 ROM"),
            )
        }

    fun createPc1251Package(
        internal: ByteArray,
        external: ByteArray,
        machineId: MachineId = com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition.MACHINE_ID,
    ): DesktopRomPackageConversionResult =
        when (val imported = Pc1251ComponentRomImporter.importImages(internal, external, machineId)) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"),
            )
        }

    fun createPc1350Package(
        internal: ByteArray,
        external: ByteArray,
    ): DesktopRomPackageConversionResult =
        when (val imported = Pc1350ComponentRomImporter.importImages(internal, external)) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "PC-1350 ROM"),
            )
        }

    fun createPc1360Package(internal: ByteArray, bankImage: ByteArray): DesktopRomPackageConversionResult =
        when (val imported = Pc1360LegacyRomImporter.importImages(internal, bankImage)) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "PC-1360 ROM"),
            )
        }

    /** Compatibility path for existing Pokecom GO address-space images. */
    fun convertPc1245LegacyImage(image: ByteArray): DesktopRomPackageConversionResult =
        when (val imported = Pc1245FlatRomImporter.importImage(image)) {
            is RomImportResult.Failure -> DesktopRomPackageConversionResult.Failure(imported.error)
            is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(
                DesktopRomPackage.write(imported.romSet, title = "PC-1245 ROM"),
            )
        }
}

internal sealed interface DesktopRomPackageConversionResult {
    data class Success(val packageBytes: ByteArray) : DesktopRomPackageConversionResult
    data class Failure(val error: RomImportError) : DesktopRomPackageConversionResult
}
