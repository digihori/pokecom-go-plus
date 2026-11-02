package com.digihori.pgp.desktop

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class DebuggerMemoryMapTest {
    @Test fun configurableRamMapMatchesEveryBusAddressInBothModes() {
        for (id in listOf("pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261", "pc-1401", "pc-1402")) {
            for (mode in RamMode.entries) {
                val layout = MachineMemoryLayout(MachineId(id), mode)
                val regions = layout.debuggerMemoryRegions().filter {
                    it.kind == MachineMemoryRegionKind.RAM || it.mirrorsRegionId == "ram"
                }
                for (address in 0..0xffff) {
                    val region = regions.singleOrNull { address in it.startAddress..it.endAddressInclusive }
                    val location = layout.locate(address)
                    assertEquals(location.kind == MachineMemoryKind.RAM, region != null, "$id $mode $address")
                    if (region != null) assertEquals(location.physicalAddress != address,
                        region.kind == MachineMemoryRegionKind.MIRROR, "$id $mode $address")
                }
            }
        }
    }

    @Test fun unrestricted1251KeepsOnlyD000RamMirror() {
        val regions = MachineMemoryLayout(MachineId("pc-1251"), RamMode.UNRESTRICTED).debuggerMemoryRegions()
        val ram = regions.single { it.kind == MachineMemoryRegionKind.RAM }
        assertEquals(0x8000..0xcfff, ram.startAddress..ram.endAddressInclusive)
        val mirror = regions.single { it.mirrorsRegionId == "ram" }
        assertEquals(0xd000..0xd7ff, mirror.startAddress..mirror.endAddressInclusive)
    }
}
