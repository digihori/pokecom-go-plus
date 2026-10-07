package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet

public object Pc1360RomDefinition {
    public val MACHINE_ID: MachineId = MachineId("pc-1360")
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public fun bankId(bank: Int): RomComponentId {
        require(bank in 0 until BANK_COUNT)
        return RomComponentId("bank-$bank")
    }

    public const val INTERNAL_SIZE: Int = 0x2000
    public const val BANK_SIZE: Int = 0x4000
    public const val BANK_COUNT: Int = 8
    public const val LEGACY_BANK_IMAGE_SIZE: Int = BANK_SIZE * BANK_COUNT
}

public object Pc1360ComponentRomImporter {
    public fun importImages(internal: ByteArray, banks: List<ByteArray>): RomImportResult {
        if (internal.size != Pc1360RomDefinition.INTERNAL_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidComponentSize(
                    Pc1360RomDefinition.INTERNAL_ID,
                    Pc1360RomDefinition.INTERNAL_SIZE,
                    internal.size,
                ),
            )
        }
        if (banks.size != Pc1360RomDefinition.BANK_COUNT) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(listOf(Pc1360RomDefinition.BANK_COUNT), banks.size),
            )
        }
        banks.forEachIndexed { index, bank ->
            if (bank.size != Pc1360RomDefinition.BANK_SIZE) {
                return RomImportResult.Failure(
                    RomImportError.InvalidComponentSize(
                        Pc1360RomDefinition.bankId(index),
                        Pc1360RomDefinition.BANK_SIZE,
                        bank.size,
                    ),
                )
            }
        }
        return RomImportResult.Success(
            RomSet(
                Pc1360RomDefinition.MACHINE_ID,
                listOf(RomComponent(Pc1360RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal)) +
                    banks.mapIndexed { index, bank ->
                        RomComponent(Pc1360RomDefinition.bankId(index), RomRole.EXTERNAL, bank)
                    },
            ),
        )
    }
}

/** Imports the two-file Pokecom GO representation. */
public object Pc1360LegacyRomImporter {
    public fun importImages(internal: ByteArray, bankImage: ByteArray): RomImportResult {
        if (bankImage.size != Pc1360RomDefinition.LEGACY_BANK_IMAGE_SIZE) {
            return RomImportResult.Failure(
                RomImportError.InvalidImageSize(
                    listOf(Pc1360RomDefinition.LEGACY_BANK_IMAGE_SIZE),
                    bankImage.size,
                ),
            )
        }
        return Pc1360ComponentRomImporter.importImages(
            internal,
            List(Pc1360RomDefinition.BANK_COUNT) { bank ->
                val start = bank * Pc1360RomDefinition.BANK_SIZE
                bankImage.copyOfRange(start, start + Pc1360RomDefinition.BANK_SIZE)
            },
        )
    }
}
