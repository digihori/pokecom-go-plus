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
    public val PC1360K_MACHINE_ID: MachineId = MachineId("pc-1360k")
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public fun bankId(bank: Int): RomComponentId {
        require(bank in 0 until BANK_COUNT)
        return RomComponentId("bank-$bank")
    }

    public fun kanjiId(selector: Int): RomComponentId {
        require(selector in 0 until KANJI_SELECTOR_COUNT)
        return RomComponentId("kanji-selector-$selector")
    }
    public const val KANJI_SELECTOR_COUNT: Int = 4
    public const val KANJI_SIZE: Int = 0x10000

    public const val INTERNAL_SIZE: Int = 0x2000
    public const val BANK_SIZE: Int = 0x4000
    public const val BANK_COUNT: Int = 8
    public const val LEGACY_BANK_IMAGE_SIZE: Int = BANK_SIZE * BANK_COUNT
}

public object Pc1360ComponentRomImporter {
    public fun importImages(internal: ByteArray, banks: List<ByteArray>, machineId: MachineId = Pc1360RomDefinition.MACHINE_ID, kanjiImages: List<ByteArray> = emptyList()): RomImportResult {
        require(machineId == Pc1360RomDefinition.MACHINE_ID || machineId == Pc1360RomDefinition.PC1360K_MACHINE_ID)
        if (kanjiImages.isNotEmpty()) {
            if (machineId != Pc1360RomDefinition.PC1360K_MACHINE_ID) return RomImportResult.Failure(RomImportError.UnsupportedMachine(machineId))
            if (kanjiImages.size !in listOf(2, Pc1360RomDefinition.KANJI_SELECTOR_COUNT)) return RomImportResult.Failure(RomImportError.InvalidImageSize(listOf(2, Pc1360RomDefinition.KANJI_SELECTOR_COUNT), kanjiImages.size))
            kanjiImages.forEachIndexed { selector, image ->
                if (image.size != Pc1360RomDefinition.KANJI_SIZE) return RomImportResult.Failure(
                    RomImportError.InvalidComponentSize(Pc1360RomDefinition.kanjiId(selector), Pc1360RomDefinition.KANJI_SIZE, image.size),
                )
            }
        }
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
                machineId,
                listOf(RomComponent(Pc1360RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal)) +
                    banks.mapIndexed { index, bank ->
                        RomComponent(Pc1360RomDefinition.bankId(index), RomRole.EXTERNAL, bank)
                    } + kanjiImages.mapIndexed { selector, image ->
                        RomComponent(Pc1360RomDefinition.kanjiId(selector), RomRole.KANJI, image)
                    },
            ),
        )
    }
}

/** Imports the two-file Pokecom GO representation. */
public object Pc1360LegacyRomImporter {
    public fun importImages(internal: ByteArray, bankImage: ByteArray, machineId: MachineId = Pc1360RomDefinition.MACHINE_ID, kanjiImages: List<ByteArray> = emptyList()): RomImportResult {
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
            machineId,
            kanjiImages,
        )
    }
}
