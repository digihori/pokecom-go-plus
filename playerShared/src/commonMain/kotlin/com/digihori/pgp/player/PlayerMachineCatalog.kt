package com.digihori.pgp.player

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.player.skin.PlayerSkinCatalog

/** Only machines with a Player skin are selectable; Core support alone does not enable a Player. */
public object PlayerMachineCatalog {
    public val definitions: List<MachineDefinition>
        get() = MachineCatalog.definitions.filter { PlayerSkinCatalog.forMachine(it.id) != null }
}

/** Portable library listing; file paths and persistence stay in each host. */
public data class PlayerRomEntry(
    public val machineId: com.digihori.pgp.core.rom.MachineId,
    public val installed: Boolean,
    public val sourceName: String?,
)
