package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet

public object Pc1245RomDefinition {
    public val MACHINE_ID: MachineId = MachineId("pc-1245")
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public val EXTERNAL_ID: RomComponentId = RomComponentId("external")

    public const val INTERNAL_SIZE: Int = 0x2000
    public const val EXTERNAL_SIZE: Int = 0x4000
    public const val LEGACY_IMAGE_SIZE: Int = 0x10000

    internal const val INTERNAL_START: Int = 0x0000
    internal const val EXTERNAL_START: Int = 0x4000
}

public object Pc1245FlatRomImporter {
    public fun importImage(image: ByteArray): RomImportResult {
        if (image.size != Pc1245RomDefinition.LEGACY_IMAGE_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(
                    expected = Pc1245RomDefinition.LEGACY_IMAGE_SIZE,
                    actual = image.size,
                ),
            )
        }

        val internal = image.copyOfRange(
            Pc1245RomDefinition.INTERNAL_START,
            Pc1245RomDefinition.INTERNAL_START + Pc1245RomDefinition.INTERNAL_SIZE,
        )
        val external = image.copyOfRange(
            Pc1245RomDefinition.EXTERNAL_START,
            Pc1245RomDefinition.EXTERNAL_START + Pc1245RomDefinition.EXTERNAL_SIZE,
        )

        return RomImportResult.Success(
            RomSet(
                machineId = Pc1245RomDefinition.MACHINE_ID,
                components = listOf(
                    RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                    RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
                ),
            ),
        )
    }
}

public sealed interface RomImportResult {
    public data class Success(public val romSet: RomSet) : RomImportResult
    public data class Failure(public val error: RomImportError) : RomImportResult
}

public sealed interface RomImportError {
    public data class InvalidImageSize(
        public val expected: Int,
        public val actual: Int,
    ) : RomImportError
}
