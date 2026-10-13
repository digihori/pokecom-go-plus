package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.rom.MachineId

/** Fixed PGP configurations; card sizing/expansion is a separate feature. */
public enum class Pc14xxModel(
    public val machineId: MachineId,
    public val bankCount: Int,
    public val characterColumns: Int,
    public val characterRows: Int,
    public val keyboardSelector: Int,
    public val basicStart: Int,
    public val basicPointer: Int,
    public val basicLimit: Int,
) {
    PC_1450(MachineId("pc-1450"), 0, 16, 1, 0x7e00, 0x2030, 0x5f01, 0x5c30),
    PC_1460(MachineId("pc-1460"), 4, 24, 1, 0x3e00, 0x8030, 0xff01, 0xff01),
    PC_1470U(MachineId("pc-1470u"), 8, 24, 2, 0x3e00, 0x8030, 0xffd7, 0xffd7);

    public companion object {
        public fun fromMachineId(id: MachineId): Pc14xxModel? = entries.firstOrNull { it.machineId == id }
    }
}
