package com.digihori.pgp.desktop

import com.digihori.pgp.core.api.*

/** Preserve catalog ROM/display descriptions while deriving configurable RAM from the active bus. */
internal fun MachineMemoryLayout.debuggerMemoryRegions(): List<MachineMemoryRegion> {
    val definition = MachineCatalog.require(machineId)
    if (!definition.supportsConfigurableRam) return definition.memoryRegions
    val regions = definition.memoryRegions.filterNot {
        it.kind == MachineMemoryRegionKind.RAM || it.mirrorsRegionId == "ram"
    }.toMutableList()
    var address = 0
    while (address <= 0xffff) {
        val first = locate(address)
        if (first.kind != MachineMemoryKind.RAM) {
            address++
            continue
        }
        val start = address
        val offset = first.physicalAddress - start
        while (address < 0xffff) {
            val next = locate(address + 1)
            if (next.kind != MachineMemoryKind.RAM || next.physicalAddress - (address + 1) != offset) break
            address++
        }
        val mirror = offset != 0
        regions += MachineMemoryRegion(
            id = if (mirror) "ram-mirror-$start" else "ram",
            displayName = if (mirror) "RAM mirror" else "RAM",
            startAddress = start,
            endAddressInclusive = address,
            kind = if (mirror) MachineMemoryRegionKind.MIRROR else MachineMemoryRegionKind.RAM,
            mirrorsRegionId = if (mirror) "ram" else null,
        )
        address++
    }
    return regions.sortedBy { it.startAddress }
}
