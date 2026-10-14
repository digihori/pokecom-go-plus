package com.digihori.pgp.core.emulator.machine.pc1261

import com.digihori.pgp.core.rom.MachineId

/** Model identity selects RAM capacity; firmware revisions do not select hardware models. */
public enum class Pc1261FamilyModel(public val machineId: MachineId, public val physicalRamStart: Int, public val basicStart: Int) {
    PC_1260(MachineId("pc-1260"), 0x5800, 0x5880),
    PC_1261(MachineId("pc-1261"), 0x4000, 0x4080);

    public companion object {
        public fun fromMachineId(id: MachineId): Pc1261FamilyModel? = entries.firstOrNull { it.machineId == id }
    }
}
