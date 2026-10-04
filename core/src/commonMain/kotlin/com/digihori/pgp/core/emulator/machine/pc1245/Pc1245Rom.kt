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
    public const val COMPACT_LEGACY_IMAGE_SIZE: Int = 0x8000
    public const val LEGACY_IMAGE_SIZE: Int = 0x10000
    public val SUPPORTED_LEGACY_IMAGE_SIZES: List<Int> = listOf(
        COMPACT_LEGACY_IMAGE_SIZE,
        LEGACY_IMAGE_SIZE,
    )

    internal const val INTERNAL_START: Int = 0x0000
    internal const val EXTERNAL_START: Int = 0x4000
}

public object Pc1245FlatRomImporter {
    public fun importImage(image: ByteArray): RomImportResult {
        if (image.size !in Pc1245RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(
                    expected = Pc1245RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES,
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

/** Imports dumps made per physical ROM component. This is the preferred PGP import path. */
public object Pc1245ComponentRomImporter {
    public fun importImages(internal: ByteArray, external: ByteArray): RomImportResult {
        val sizeErrors = buildList {
            if (internal.size != Pc1245RomDefinition.INTERNAL_SIZE) {
                add(
                    RomImportError.InvalidComponentSize(
                        Pc1245RomDefinition.INTERNAL_ID,
                        Pc1245RomDefinition.INTERNAL_SIZE,
                        internal.size,
                    ),
                )
            }
            if (external.size != Pc1245RomDefinition.EXTERNAL_SIZE) {
                add(
                    RomImportError.InvalidComponentSize(
                        Pc1245RomDefinition.EXTERNAL_ID,
                        Pc1245RomDefinition.EXTERNAL_SIZE,
                        external.size,
                    ),
                )
            }
        }
        if (sizeErrors.isNotEmpty()) return RomImportResult.Failure(sizeErrors.first())

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
    public data class UnsupportedMachine(public val machineId: MachineId) : RomImportError

    public data class InvalidImageSize(
        public val expected: List<Int>,
        public val actual: Int,
    ) : RomImportError

    public data class InvalidComponentSize(
        public val componentId: RomComponentId,
        public val expected: Int,
        public val actual: Int,
    ) : RomImportError
}
