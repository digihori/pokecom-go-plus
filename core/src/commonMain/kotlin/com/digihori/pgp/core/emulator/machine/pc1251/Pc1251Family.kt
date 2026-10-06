package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.rom.MachineId

/** Hardware variants which share the PC-1251 family ROM and peripherals. */
public enum class Pc1251FamilyModel(
    public val machineId: MachineId,
    public val physicalRamStart: Int,
) {
    PC_1250(MachineId("pc-1250"), 0xc000),
    PC_1251(MachineId("pc-1251"), 0xb800),
    PC_1255(MachineId("pc-1255"), 0xa000),
    ;

    public companion object {
        public fun fromMachineId(machineId: MachineId): Pc1251FamilyModel? =
            entries.firstOrNull { it.machineId == machineId }
    }
}

/**
 * Controls whether absent RAM follows the selected hardware or is made available
 * as the largest PC-1255 configuration for emulator convenience.
 */
public enum class Pc1251FamilyMemoryMode {
    HARDWARE,
    EXPANDED,
}
