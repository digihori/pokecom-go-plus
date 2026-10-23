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

/** Resolves only RAM aliases; peripheral and ROM decoding belongs to the bus. */
internal fun pc1251RamAddress(address: Int, model: Pc1251FamilyModel, mode: Pc1251FamilyMemoryMode): Int {
    if (address in 0xd000..0xd7ff) return address - 0x1000
    // Expanded mode uses the full PC-1255 RAM configuration and its aliases.
    val effective = if (mode == Pc1251FamilyMemoryMode.EXPANDED) Pc1251FamilyModel.PC_1255 else model
    return when (effective) {
        Pc1251FamilyModel.PC_1250 -> address
        Pc1251FamilyModel.PC_1251 -> when (address) {
            in 0xb000..0xb7ff -> address + 0x0800
            in 0x9000..0x9fff -> 0xb800 or (address and 0x07ff)
            else -> address
        }
        Pc1251FamilyModel.PC_1255 -> if (address in 0x8000..0x9fff) address + 0x2000 else address
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
