package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet

public object Pc1401RomDefinition {
    public val MACHINE_ID: MachineId = MachineId("pc-1401")
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public val EXTERNAL_ID: RomComponentId = RomComponentId("external")
    public const val INTERNAL_SIZE: Int = 0x2000
    public const val EXTERNAL_SIZE: Int = 0x8000
    public const val LEGACY_IMAGE_SIZE: Int = 0x10000
}

public object Pc1401ComponentRomImporter {
    public fun importImages(internal: ByteArray, external: ByteArray): RomImportResult {
        if (internal.size != Pc1401RomDefinition.INTERNAL_SIZE) return RomImportResult.Failure(
            RomImportError.InvalidComponentSize(
                Pc1401RomDefinition.INTERNAL_ID, Pc1401RomDefinition.INTERNAL_SIZE, internal.size,
            ),
        )
        if (external.size != Pc1401RomDefinition.EXTERNAL_SIZE) return RomImportResult.Failure(
            RomImportError.InvalidComponentSize(
                Pc1401RomDefinition.EXTERNAL_ID, Pc1401RomDefinition.EXTERNAL_SIZE, external.size,
            ),
        )
        return RomImportResult.Success(
            RomSet(
                Pc1401RomDefinition.MACHINE_ID,
                listOf(
                    RomComponent(Pc1401RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                    RomComponent(Pc1401RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
                ),
            ),
        )
    }
}

/** Imports the historical 64 KiB address-space image; its 0x2000-0x7fff contents are not ROM. */
public object Pc1401FlatRomImporter {
    public fun importImage(image: ByteArray): RomImportResult {
        if (image.size != Pc1401RomDefinition.LEGACY_IMAGE_SIZE) return RomImportResult.Failure(
            RomImportError.InvalidImageSize(listOf(Pc1401RomDefinition.LEGACY_IMAGE_SIZE), image.size),
        )
        return Pc1401ComponentRomImporter.importImages(
            image.copyOfRange(0x0000, 0x2000),
            image.copyOfRange(0x8000, 0x10000),
        )
    }
}
