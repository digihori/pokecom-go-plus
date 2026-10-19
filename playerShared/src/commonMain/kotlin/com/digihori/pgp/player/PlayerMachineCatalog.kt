package com.digihori.pgp.player

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.skin.PlayerSkinCatalog

/** Explicit Player integration targets; a skin alone does not enable a machine. */
public object PlayerMachineCatalog {
    public val definitions: List<MachineDefinition>
        get() = enabledMachineIds.map { MachineCatalog.require(MachineId(it)) }
            .filter { PlayerSkinCatalog.forMachine(it.id) != null }
            .sortedBy { it.displayName }

    // Integration targets are explicit; presentation order follows the machine name.
    private val enabledMachineIds = listOf("pc-1245", "pc-1401", "pc-1350", "pc-1251", "pc-1261", "pc-1360", "pc-1360k")
}

/** Portable library listing; file paths and persistence stay in each host. */
public data class PlayerRomEntry(
    public val machineId: com.digihori.pgp.core.rom.MachineId,
    public val installed: Boolean,
    public val sourceName: String?,
)
