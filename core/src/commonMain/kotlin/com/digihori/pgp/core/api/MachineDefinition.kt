package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.rom.MachineId

public enum class MachineFamily {
    PC_1245,
    PC_1251,
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
}

public enum class MachineKeyboardLayout {
    PC_1245,
    PC_1251,
}

public enum class MachineBasicDialect {
    OLD,
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
    public val supportedOperatingModes: Set<OperatingMode>,
    public val cyclesPerSecond: Long,
    public val automaticKeyHoldCycles: Long,
    public val automaticKeyGapCycles: Long,
    public val supportsConfigurableRam: Boolean,
) {
    init {
        require(displayName.isNotBlank())
        require(characterColumns > 0)
        require(supportedOperatingModes.isNotEmpty())
        require(cyclesPerSecond > 0)
        require(automaticKeyHoldCycles > 0)
        require(automaticKeyGapCycles >= 0)
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
        supportedOperatingModes = setOf(OperatingMode.RUN, OperatingMode.PROGRAM),
        cyclesPerSecond = 288_000L,
        automaticKeyHoldCycles = 17_280L,
        automaticKeyGapCycles = 5_760L,
        supportsConfigurableRam = false,
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
            supportedOperatingModes = setOf(
                OperatingMode.RUN,
                OperatingMode.PROGRAM,
                OperatingMode.RESERVE,
            ),
            cyclesPerSecond = 192_000L,
            automaticKeyHoldCycles = 38_400L,
            automaticKeyGapCycles = 19_200L,
            supportsConfigurableRam = true,
        )
    }

    public val definitions: List<MachineDefinition> = listOf(pc1245) + pc1251Family
    public val defaultDefinition: MachineDefinition = pc1245

    private val byId: Map<MachineId, MachineDefinition> = definitions.associateBy(MachineDefinition::id)

    public fun find(machineId: MachineId): MachineDefinition? = byId[machineId]

    public fun require(machineId: MachineId): MachineDefinition =
        requireNotNull(find(machineId)) { "Unsupported machine: ${machineId.value}" }
}
