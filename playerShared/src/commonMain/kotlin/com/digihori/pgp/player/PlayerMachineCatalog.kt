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

    /** One stable home/library row per shared ROM, retaining the pre-series storage IDs. */
    public val homeDefinitions: List<MachineDefinition>
        get() = definitions.filter { romId(it.id) == it.id }

    public fun romId(id: MachineId): MachineId = MachineId(when (id.value) {
        "pc-1250", "pc-1255" -> "pc-1251"
        "pc-1260" -> "pc-1261"
        "pc-1402" -> "pc-1401"
        else -> id.value
    })

    public fun models(id: MachineId): List<MachineDefinition> =
        definitions.filter { romId(it.id) == romId(id) }

    public fun selectedModel(id: MachineId, selections: Map<MachineId, MachineId>): MachineId =
        selections[romId(id)]?.takeIf { candidate -> models(id).any { it.id == candidate } } ?: romId(id)

    public fun romLabel(id: MachineId): String = models(id).joinToString(" / ") { it.displayName }

    /** Retarget identity only within an explicitly supported shared-ROM group. */
    public fun forModel(rom: com.digihori.pgp.core.rom.RomSet, id: MachineId): com.digihori.pgp.core.rom.RomSet? {
        if (definitions.none { it.id == id } || romId(rom.machineId) != romId(id)) return null
        return com.digihori.pgp.core.rom.RomSet(id, rom.components)
    }

    // Integration targets are explicit; presentation order follows the machine name.
    private val enabledMachineIds = listOf("pc-1245", "pc-1401", "pc-1402", "pc-1350", "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261", "pc-1360", "pc-1360k", "pc-1450", "pc-1460", "pc-1470u")
}

/** Portable library listing; file paths and persistence stay in each host. */
public data class PlayerRomEntry(
    public val machineId: com.digihori.pgp.core.rom.MachineId,
    public val installed: Boolean,
    public val sourceName: String?,
)
