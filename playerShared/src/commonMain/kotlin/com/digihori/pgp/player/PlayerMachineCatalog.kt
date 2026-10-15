package com.digihori.pgp.player

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.player.skin.PlayerSkinCatalog

/** Explicit Player integration targets; a skin alone does not enable a machine. */
public object PlayerMachineCatalog {
    public val definitions: List<MachineDefinition>
        get() = MachineCatalog.definitions.filter {
            it.id.value in enabledMachineIds && PlayerSkinCatalog.forMachine(it.id) != null
        }

    private val enabledMachineIds = setOf("pc-1245", "pc-1401")
}

/** Portable library listing; file paths and persistence stay in each host. */
public data class PlayerRomEntry(
    public val machineId: com.digihori.pgp.core.rom.MachineId,
    public val installed: Boolean,
    public val sourceName: String?,
)
