package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult

internal object DesktopRomPackageConverter {
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
