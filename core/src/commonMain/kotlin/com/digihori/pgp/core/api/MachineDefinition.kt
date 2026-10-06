package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.rom.MachineId

public enum class MachineFamily {
    PC_1245,
    PC_1251,
    PC_1350,
}

public enum class MachineGeneration {
    OLD,
    S1,
    S2,
}

public sealed interface MachineMemoryBanking {
    public data object None : MachineMemoryBanking

    public data class Banked(
        public val windowStart: Int,
        public val windowEndInclusive: Int,
        public val bankCount: Int,
    ) : MachineMemoryBanking {
        init {
            require(windowStart in 0..0xffff)
            require(windowEndInclusive in windowStart..0xffff)
            require(bankCount > 1)
        }
    }
}

public enum class MachineRomLayout {
    SC61860_INTERNAL_8K_EXTERNAL_16K,
    SC61860_INTERNAL_8K_EXTERNAL_32K,
}

public enum class MachineKeyboardLayout {
    PC_1245,
    PC_1251,
    PC_1350,
}

public enum class MachineBasicDialect {
    OLD,
    S1,
}

public enum class MachineMemoryRegionKind {
    ROM,
    RAM,
    DISPLAY,
    MIRROR,
}

/** A debugger-facing description of a meaningful range in the machine address space. */
public data class MachineMemoryRegion(
    public val id: String,
    public val displayName: String,
    public val startAddress: Int,
    public val endAddressInclusive: Int,
    public val kind: MachineMemoryRegionKind,
    public val mirrorsRegionId: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(startAddress in 0..0xffff)
        require(endAddressInclusive in startAddress..0xffff)
        require(kind == MachineMemoryRegionKind.MIRROR || mirrorsRegionId == null)
        require(kind != MachineMemoryRegionKind.MIRROR || !mirrorsRegionId.isNullOrBlank())
    }
}

public data class MachineDefinition(
    public val id: MachineId,
    public val displayName: String,
    public val family: MachineFamily,
    public val generation: MachineGeneration,
    public val memoryBanking: MachineMemoryBanking,
    public val romLayout: MachineRomLayout,
    public val keyboardLayout: MachineKeyboardLayout,
    public val basicDialect: MachineBasicDialect,
    public val characterColumns: Int,
    public val characterRows: Int,
    public val supportedOperatingModes: Set<OperatingMode>,
    public val cyclesPerSecond: Long,
    public val automaticKeyHoldCycles: Long,
    public val automaticKeyGapCycles: Long,
    public val supportsConfigurableRam: Boolean,
    public val memoryRegions: List<MachineMemoryRegion>,
) {
    init {
        require(displayName.isNotBlank())
        require(characterColumns > 0)
        require(characterRows > 0)
        require(supportedOperatingModes.isNotEmpty())
        require(cyclesPerSecond > 0)
        require(automaticKeyHoldCycles > 0)
        require(automaticKeyGapCycles >= 0)
        require(memoryRegions.isNotEmpty())
        require(memoryRegions.map(MachineMemoryRegion::id).distinct().size == memoryRegions.size)
        val regionIds = memoryRegions.map(MachineMemoryRegion::id).toSet()
        require(memoryRegions.all { it.mirrorsRegionId == null || it.mirrorsRegionId in regionIds })
    }
}

public object MachineCatalog {
    private val pc1245 = MachineDefinition(
        id = Pc1245RomDefinition.MACHINE_ID,
        displayName = "PC-1245",
        family = MachineFamily.PC_1245,
        generation = MachineGeneration.OLD,
        memoryBanking = MachineMemoryBanking.None,
        romLayout = MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_16K,
        keyboardLayout = MachineKeyboardLayout.PC_1245,
        basicDialect = MachineBasicDialect.OLD,
        characterColumns = 16,
        characterRows = 1,
        supportedOperatingModes = setOf(OperatingMode.RUN, OperatingMode.PROGRAM),
        cyclesPerSecond = 288_000L,
        automaticKeyHoldCycles = 17_280L,
        automaticKeyGapCycles = 5_760L,
        supportsConfigurableRam = false,
        memoryRegions = oldMachineRegions(
            ramStart = 0xc000,
            // The B000-BFFF behavior is intentionally not described as a mirror until it is verified.
            ramAliases = listOf(0xd000..0xd7ff),
        ),
    )

    private val pc1251Family: List<MachineDefinition> = Pc1251FamilyModel.entries.map { model ->
        MachineDefinition(
            id = model.machineId,
            displayName = model.machineId.value.uppercase(),
            family = MachineFamily.PC_1251,
            generation = MachineGeneration.OLD,
            memoryBanking = MachineMemoryBanking.None,
            romLayout = MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_16K,
            keyboardLayout = MachineKeyboardLayout.PC_1251,
            basicDialect = MachineBasicDialect.OLD,
            characterColumns = 24,
            characterRows = 1,
            supportedOperatingModes = setOf(
                OperatingMode.RUN,
                OperatingMode.PROGRAM,
                OperatingMode.RESERVE,
            ),
            cyclesPerSecond = 192_000L,
            automaticKeyHoldCycles = 38_400L,
            automaticKeyGapCycles = 19_200L,
            supportsConfigurableRam = true,
            memoryRegions = oldMachineRegions(
                ramStart = model.physicalRamStart,
                ramAliases = when (model) {
                    Pc1251FamilyModel.PC_1250 -> listOf(0xb000..0xbfff, 0xd000..0xd7ff)
                    Pc1251FamilyModel.PC_1251 -> listOf(0xb000..0xb7ff, 0xd000..0xd7ff)
                    Pc1251FamilyModel.PC_1255 -> listOf(0x8000..0x9fff, 0xd000..0xd7ff)
                },
            ),
        )
    }

    private val pc1350 = MachineDefinition(
        id = Pc1350RomDefinition.MACHINE_ID,
        displayName = "PC-1350",
        family = MachineFamily.PC_1350,
        generation = MachineGeneration.S1,
        memoryBanking = MachineMemoryBanking.None,
        romLayout = MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_32K,
        keyboardLayout = MachineKeyboardLayout.PC_1350,
        basicDialect = MachineBasicDialect.S1,
        characterColumns = 25,
        characterRows = 4,
        supportedOperatingModes = setOf(OperatingMode.RUN, OperatingMode.PROGRAM),
        // The reference implementation uses this value but marks it as unverified.
        cyclesPerSecond = 768_000L,
        automaticKeyHoldCycles = 46_080L,
        automaticKeyGapCycles = 15_360L,
        supportsConfigurableRam = false,
        memoryRegions = listOf(
            MachineMemoryRegion("internal-rom", "Internal ROM", 0x0000, 0x1fff, MachineMemoryRegionKind.ROM),
            MachineMemoryRegion("ram", "RAM", 0x2000, 0x7fff, MachineMemoryRegionKind.RAM),
            MachineMemoryRegion("display", "Display memory", 0x7000, 0x787b, MachineMemoryRegionKind.DISPLAY),
            MachineMemoryRegion("external-rom", "External ROM", 0x8000, 0xffff, MachineMemoryRegionKind.ROM),
        ),
    )

    public val definitions: List<MachineDefinition> = listOf(pc1245) + pc1251Family + pc1350
    public val defaultDefinition: MachineDefinition = pc1245

    private val byId: Map<MachineId, MachineDefinition> = definitions.associateBy(MachineDefinition::id)

    public fun find(machineId: MachineId): MachineDefinition? = byId[machineId]

    public fun require(machineId: MachineId): MachineDefinition =
        requireNotNull(find(machineId)) { "Unsupported machine: ${machineId.value}" }

    private fun oldMachineRegions(
        ramStart: Int,
        ramAliases: List<IntRange>,
    ): List<MachineMemoryRegion> = buildList {
        add(MachineMemoryRegion("internal-rom", "Internal ROM", 0x0000, 0x1fff, MachineMemoryRegionKind.ROM))
        add(MachineMemoryRegion("external-rom", "External ROM", 0x4000, 0x7fff, MachineMemoryRegionKind.ROM))
        add(MachineMemoryRegion("rom-mirror", "ROM read mirror", 0x2000, 0x3fff, MachineMemoryRegionKind.MIRROR, "external-rom"))
        add(MachineMemoryRegion("ram", "RAM", ramStart, 0xc7ff, MachineMemoryRegionKind.RAM))
        add(MachineMemoryRegion("display", "Display memory", 0xf800, 0xf8ff, MachineMemoryRegionKind.DISPLAY))
        ramAliases.forEachIndexed { index, range ->
            add(
                MachineMemoryRegion(
                    id = "ram-mirror-$index",
                    displayName = "RAM mirror",
                    startAddress = range.first,
                    endAddressInclusive = range.last,
                    kind = MachineMemoryRegionKind.MIRROR,
                    mirrorsRegionId = "ram",
                ),
            )
        }
        add(MachineMemoryRegion("display-mirror-e", "Display mirror", 0xe800, 0xefff, MachineMemoryRegionKind.MIRROR, "display"))
        add(MachineMemoryRegion("display-mirror-f", "Display mirror", 0xf900, 0xffff, MachineMemoryRegionKind.MIRROR, "display"))
    }
}
