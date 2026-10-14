package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.rom.MachineId

/** Model identity selects RAM capacity; firmware revisions do not select hardware models. */
public enum class Pc1401FamilyModel(public val machineId: MachineId, public val physicalRamStart: Int, public val basicStart: Int) {
    PC_1401(MachineId("pc-1401"), 0x3800, 0x3800),
    PC_1402(MachineId("pc-1402"), 0x2000, 0x2000);

    public companion object {
        public fun fromMachineId(id: MachineId): Pc1401FamilyModel? = entries.firstOrNull { it.machineId == id }
    }
}
