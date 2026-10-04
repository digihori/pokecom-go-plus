package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet

public object Pc1251RomDefinition {
    public val MACHINE_ID: MachineId = Pc1251FamilyModel.PC_1251.machineId
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public val EXTERNAL_ID: RomComponentId = RomComponentId("external")
    public const val INTERNAL_SIZE: Int = 0x2000
    public const val EXTERNAL_SIZE: Int = 0x4000
    public const val COMPACT_LEGACY_IMAGE_SIZE: Int = 0x8000
    public const val LEGACY_IMAGE_SIZE: Int = 0x10000
    public val SUPPORTED_LEGACY_IMAGE_SIZES: List<Int> = listOf(COMPACT_LEGACY_IMAGE_SIZE, LEGACY_IMAGE_SIZE)
    internal const val INTERNAL_START: Int = 0x0000
    internal const val EXTERNAL_START: Int = 0x4000
}

public object Pc1251ComponentRomImporter {
    public fun importImages(
        internal: ByteArray,
        external: ByteArray,
        machineId: MachineId = Pc1251RomDefinition.MACHINE_ID,
    ): RomImportResult {
        if (Pc1251FamilyModel.fromMachineId(machineId) == null) {
            return RomImportResult.Failure(RomImportError.UnsupportedMachine(machineId))
        }
        if (internal.size != Pc1251RomDefinition.INTERNAL_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidComponentSize(
                    Pc1251RomDefinition.INTERNAL_ID,
                    Pc1251RomDefinition.INTERNAL_SIZE,
                    internal.size,
                ),
            )
        }
        if (external.size != Pc1251RomDefinition.EXTERNAL_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidComponentSize(
                    Pc1251RomDefinition.EXTERNAL_ID,
                    Pc1251RomDefinition.EXTERNAL_SIZE,
                    external.size,
                ),
            )
        }
        return RomImportResult.Success(romSet(machineId, internal, external))
    }
}

public object Pc1251FlatRomImporter {
    public fun importImage(
        image: ByteArray,
        machineId: MachineId = Pc1251RomDefinition.MACHINE_ID,
    ): RomImportResult {
        if (Pc1251FamilyModel.fromMachineId(machineId) == null) {
            return RomImportResult.Failure(RomImportError.UnsupportedMachine(machineId))
        }
        if (image.size !in Pc1251RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(Pc1251RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES, image.size),
            )
        }
        return RomImportResult.Success(
            romSet(
                machineId,
                image.copyOfRange(0, Pc1251RomDefinition.INTERNAL_SIZE),
                image.copyOfRange(0x4000, 0x4000 + Pc1251RomDefinition.EXTERNAL_SIZE),
            ),
        )
    }
}

private fun romSet(machineId: MachineId, internal: ByteArray, external: ByteArray): RomSet = RomSet(
    machineId,
    listOf(
        RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
        RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
    ),
)
