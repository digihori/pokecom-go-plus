package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.rom.MachineId

/** One registration row per shared ROM; machine selection still lists every RAM model. */
internal object DesktopRomRegistrationCatalog {
    val definitions: List<MachineDefinition> get() = MachineCatalog.definitions.filter { definition ->
        when (definition.family) {
            MachineFamily.PC_1251 -> definition.id.value == "pc-1251"
            MachineFamily.PC_1261 -> definition.id.value == "pc-1261"
            MachineFamily.PC_1401 -> definition.id.value == "pc-1401"
            else -> true
        }
    }

    fun label(id: MachineId): String = when (MachineCatalog.require(id).family) {
        MachineFamily.PC_1251 -> "PC-1250 / PC-1251 / PC-1255 family"
        MachineFamily.PC_1261 -> "PC-1261 family"
        MachineFamily.PC_1401 -> "PC-1401 family"
        else -> MachineCatalog.require(id).displayName
    }
}
