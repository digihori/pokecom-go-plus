package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.*

public object Pc14xxRomDefinition {
    public val INTERNAL_ID: RomComponentId = RomComponentId("internal")
    public val EXTERNAL_ID: RomComponentId = RomComponentId("external")
    public const val INTERNAL_SIZE: Int = 0x2000
    public const val EXTERNAL_SIZE: Int = 0x8000
    public const val BANK_SIZE: Int = 0x4000
    public fun bankId(bank: Int): RomComponentId = RomComponentId("bank-$bank")
}

/** external is one 32 KiB image for 1450, or sequential 16 KiB banks for banked models. */
public object Pc14xxComponentRomImporter {
    public fun importImages(model: Pc14xxModel, internal: ByteArray, external: ByteArray): RomImportResult {
        if (internal.size != 0x2000) return RomImportResult.Failure(
            RomImportError.InvalidComponentSize(Pc14xxRomDefinition.INTERNAL_ID, 0x2000, internal.size))
        val size = if (model.bankCount == 0) 0x8000 else model.bankCount * 0x4000
        if (external.size != size) return RomImportResult.Failure(
            RomImportError.InvalidImageSize(listOf(size), external.size))
        val components = listOf(RomComponent(Pc14xxRomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal)) +
            if (model.bankCount == 0) listOf(RomComponent(Pc14xxRomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external))
            else List(model.bankCount) { bank -> RomComponent(Pc14xxRomDefinition.bankId(bank), RomRole.EXTERNAL,
                external.copyOfRange(bank * 0x4000, (bank + 1) * 0x4000)) }
        return RomImportResult.Success(RomSet(model.machineId, components))
    }
}

/** Historical memory file plus (for banked machines) a separate sequential bank file. */
public object Pc14xxFlatRomImporter {
    public fun importImage(model: Pc14xxModel, image: ByteArray, bankImage: ByteArray? = null): RomImportResult {
        val sizes = if (model.bankCount == 0) listOf(0x10000) else listOf(0x2000, 0x10000)
        if (image.size !in sizes) return RomImportResult.Failure(RomImportError.InvalidImageSize(sizes, image.size))
        if (model.bankCount != 0 && bankImage == null) return RomImportResult.Failure(
            RomImportError.InvalidImageSize(listOf(model.bankCount * 0x4000), 0))
        return Pc14xxComponentRomImporter.importImages(model, image.copyOfRange(0, 0x2000),
            if (model.bankCount == 0) image.copyOfRange(0x8000, 0x10000) else checkNotNull(bankImage))
    }
}
