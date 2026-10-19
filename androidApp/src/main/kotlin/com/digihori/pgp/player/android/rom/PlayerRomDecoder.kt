package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350FlatRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.player.PlayerMachineCatalog

/** Legacy size is ambiguous: the host must supply the selected machine. */
internal object PlayerRomDecoder {
    fun decode(bytes: ByteArray, machineId: MachineId): RomSet? {
        if (PlayerMachineCatalog.definitions.none { it.id == machineId }) return null
        val rom = if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) {
            (AndroidRomPackage.read(bytes) as? RomPackageReadResult.Success)?.romSet
        } else {
            // PC-1360 uses internal + eight ROM banks; import its validated .pgrom package.
            val imported = when (machineId.value) {
                "pc-1261" -> Pc1261FlatRomImporter.importImage(bytes)
                "pc-1251" -> Pc1251FlatRomImporter.importImage(bytes)
                "pc-1245" -> Pc1245FlatRomImporter.importImage(bytes)
                "pc-1401" -> Pc1401FlatRomImporter.importImage(bytes)
                "pc-1350" -> Pc1350FlatRomImporter.importImage(bytes)
                else -> return null
            }
            (imported as? RomImportResult.Success)?.romSet
        }
        return rom?.takeIf { it.machineId == machineId }
    }
}
