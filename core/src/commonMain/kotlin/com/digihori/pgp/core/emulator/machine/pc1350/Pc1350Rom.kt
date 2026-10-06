package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet

public object Pc1350RomDefinition {
    public val MACHINE_ID: MachineId = MachineId("pc-1350")
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public val EXTERNAL_ID: RomComponentId = RomComponentId("external")

    public const val INTERNAL_SIZE: Int = 0x2000
    public const val EXTERNAL_SIZE: Int = 0x8000
    public const val LEGACY_IMAGE_SIZE: Int = 0x10000

    internal const val INTERNAL_START: Int = 0x0000
    internal const val EXTERNAL_START: Int = 0x8000
}

/** Imports dumps made per physical ROM component. This is the preferred PGP import path. */
public object Pc1350ComponentRomImporter {
    public fun importImages(internal: ByteArray, external: ByteArray): RomImportResult {
        if (internal.size != Pc1350RomDefinition.INTERNAL_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidComponentSize(
                    Pc1350RomDefinition.INTERNAL_ID,
                    Pc1350RomDefinition.INTERNAL_SIZE,
                    internal.size,
                ),
            )
        }
        if (external.size != Pc1350RomDefinition.EXTERNAL_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidComponentSize(
                    Pc1350RomDefinition.EXTERNAL_ID,
                    Pc1350RomDefinition.EXTERNAL_SIZE,
                    external.size,
                ),
            )
        }
        return RomImportResult.Success(
            RomSet(
                machineId = Pc1350RomDefinition.MACHINE_ID,
                components = listOf(
                    RomComponent(Pc1350RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                    RomComponent(Pc1350RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
                ),
            ),
        )
    }
}

/** Imports the historical 64 KiB memory-image form and discards its RAM area. */
public object Pc1350FlatRomImporter {
    public fun importImage(image: ByteArray): RomImportResult {
        if (image.size != Pc1350RomDefinition.LEGACY_IMAGE_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(
                    expected = listOf(Pc1350RomDefinition.LEGACY_IMAGE_SIZE),
                    actual = image.size,
                ),
            )
        }
        return Pc1350ComponentRomImporter.importImages(
            internal = image.copyOfRange(0x0000, 0x2000),
            external = image.copyOfRange(0x8000, 0x10000),
        )
    }
}
