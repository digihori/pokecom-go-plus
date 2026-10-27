package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350Display
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360Display
import com.digihori.pgp.core.emulator.machine.pc1251.pc1251RamAddress
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.machine.AddressedMemoryImage

public enum class MachineMemoryKind { RAM, ROM, LCD, CONTROL, UNMAPPED }

public data class MachineMemoryLocation(
    public val kind: MachineMemoryKind,
    public val writable: Boolean,
    /** The bus write destination; aliases share this address. */
    public val physicalAddress: Int,
)

/** Pure description of PGP's current bus mapping, including compatibility write windows. */
public class MachineMemoryLayout(
    public val machineId: MachineId,
    public val pc1251MemoryMode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED,
) {
    init { require(MachineCatalog.find(machineId) != null) }

    /** A starter location away from the initial BASIC text and its pointer words. */
    public val assemblyStartAddress: Int get() = when (machineId.value) {
        "pc-1260", "pc-1261", "pc-1350" -> 0x6000
        "pc-1470u", "pc-1460", "pc-1360", "pc-1360k" -> 0xc000
        "pc-1450" -> 0x5000
        "pc-1401", "pc-1402" -> 0x4400
        else -> 0xc500
    }

    public val defaultBasicStartAddress: Int get() = when (machineId.value) {
        "pc-1245" -> 0xc000
        "pc-1250", "pc-1251", "pc-1255" -> checkNotNull(Pc1251FamilyModel.fromMachineId(machineId)).physicalRamStart
        "pc-1260", "pc-1261" -> checkNotNull(Pc1261FamilyModel.fromMachineId(machineId)).basicStart
        "pc-1401", "pc-1402" -> checkNotNull(Pc1401FamilyModel.fromMachineId(machineId)).basicStart
        "pc-1450" -> 0x2030
        "pc-1350" -> 0x2030
        "pc-1470u", "pc-1460", "pc-1360", "pc-1360k" -> 0x8030
        else -> 0x3800
    }

    public val basicStartPointerAddress: Int get() = when (machineId.value) {
        "pc-1260", "pc-1261" -> 0x66e1
        "pc-1350" -> 0x6f01
        "pc-1470u", "pc-1360", "pc-1360k" -> 0xffd7
        "pc-1460" -> 0xff01
        "pc-1450" -> 0x5f01
        "pc-1401", "pc-1402" -> 0x46e1
        else -> 0xc6e1
    }

    public fun locate(address: Int): MachineMemoryLocation {
        require(address in 0..0xffff)
        if (address < 0x2000) return location(MachineMemoryKind.ROM, false, address)
        return when (machineId.value) {
            "pc-1245" -> {
                var mapped = address
                if (mapped in 0xd000..0xd7ff) mapped -= 0x1000
                if (mapped in 0xe800..0xefff) mapped += 0x1000
                if (mapped >= 0xf900) mapped = mapped and 0xf8ff
                when {
                    address in 0x2000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                    mapped in 0xf800..0xf8ff -> location(MachineMemoryKind.LCD, true, mapped)
                    mapped in 0xc000..0xc7ff -> location(MachineMemoryKind.RAM, true, mapped)
                    else -> location(MachineMemoryKind.UNMAPPED, false, mapped)
                }
            }
            "pc-1250", "pc-1251", "pc-1255" -> {
                val model = checkNotNull(Pc1251FamilyModel.fromMachineId(machineId))
                val start = if (pc1251MemoryMode == Pc1251FamilyMemoryMode.EXPANDED) 0xa000 else model.physicalRamStart
                val mapped = when (address) {
                    in 0xe800..0xefff -> 0xf800 or (address and 0xff)
                    in 0xf900..0xffff -> address and 0xf8ff
                    else -> pc1251RamAddress(address, model, pc1251MemoryMode)
                }
                when {
                    address in 0x2000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                    mapped in 0xf800..0xf8ff -> location(MachineMemoryKind.LCD, true, mapped)
                    mapped in start..0xc7ff -> location(MachineMemoryKind.RAM, true, mapped)
                    else -> location(MachineMemoryKind.UNMAPPED, false, mapped)
                }
            }
            "pc-1260", "pc-1261" -> when (address) {
                in 0x2000..0x3fff -> location(MachineMemoryKind.LCD, true, address and 0x28ff)
                in checkNotNull(Pc1261FamilyModel.fromMachineId(machineId)).physicalRamStart..0x67ff -> location(MachineMemoryKind.RAM, true, address)
                in 0x8000..0xffff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.UNMAPPED, false, address)
            }
            "pc-1350" -> when {
                isDisplayAddress(address, Pc1350Display.VRAM_BLOCK_STARTS, Pc1350Display.SYMBOL_ADDRESS) -> location(MachineMemoryKind.LCD, true, address)
                address in 0x2000..0x7fff -> location(MachineMemoryKind.RAM, true, address)
                else -> location(MachineMemoryKind.ROM, false, address)
            }
            "pc-1360", "pc-1360k" -> when {
                machineId.value == "pc-1360k" && address in 0x2000..0x27ff -> location(MachineMemoryKind.RAM, true, address)
                isDisplayAddress(address, Pc1360Display.VRAM_BLOCK_STARTS, Pc1360Display.SYMBOL_ADDRESS) -> location(MachineMemoryKind.LCD, true, address)
                address in 0x2000..0x3fff -> location(MachineMemoryKind.CONTROL, true, address)
                address in 0x4000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.RAM, true, address)
            }
            "pc-1470u" -> when (address) {
                in 0x2800..0x287d, in 0x2a00..0x2a7b -> location(MachineMemoryKind.LCD, true, address)
                in 0x2000..0x3fff -> location(MachineMemoryKind.CONTROL, true, address)
                in 0x4000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.RAM, true, address)
            }
            "pc-1460" -> when (address) {
                in 0x3000..0x307c -> location(MachineMemoryKind.LCD, true, address)
                in 0x2000..0x3fff -> location(MachineMemoryKind.CONTROL, true, address)
                in 0x4000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.RAM, true, address)
            }
            "pc-1450" -> when (address) {
                in 0x2000..0x5fff -> location(MachineMemoryKind.RAM, true, address)
                in 0x7000..0x707c -> location(MachineMemoryKind.LCD, true, address)
                in 0x6000..0x7fff -> location(MachineMemoryKind.CONTROL, true, address)
                else -> location(MachineMemoryKind.ROM, false, address)
            }
            else -> when (address) {
                in checkNotNull(Pc1401FamilyModel.fromMachineId(machineId)).physicalRamStart..0x47ff -> location(MachineMemoryKind.RAM, true, address)
                in 0x6000..0x67ff, in 0x7000..0x77ff -> location(MachineMemoryKind.LCD, true, 0x6000 or (address and 0xff))
                in 0x8000..0xffff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.UNMAPPED, false, address)
            }
        }
    }

    private fun isDisplayAddress(address: Int, starts: List<Int>, symbol: Int): Boolean =
        address == symbol || starts.any { address in it until it + 30 }

    private fun location(kind: MachineMemoryKind, writable: Boolean, physical: Int): MachineMemoryLocation =
        MachineMemoryLocation(kind, writable, physical)
}

public data class MemoryPlacementIssue(public val segmentIndex: Int, public val error: MemoryImageLoadError)

public object MemoryPlacementValidator {
    /** Validates before writing anything, including aliases within a single segment. */
    public fun validate(image: AddressedMemoryImage, layout: MachineMemoryLayout): MemoryPlacementIssue? {
        val owners = IntArray(0x10000) { -1 }
        image.segments.forEachIndexed { index, segment ->
            for (address in segment.startAddress..segment.endAddress) {
                val location = layout.locate(address)
                if (!location.writable) return MemoryPlacementIssue(index, MemoryImageLoadError.ReadOnlyAddress(address, segment.sourceLine))
                val previous = owners[location.physicalAddress]
                if (previous >= 0) return MemoryPlacementIssue(index, MemoryImageLoadError.MirrorOverlap(
                    address, previous, location.physicalAddress, segment.sourceLine,
                ))
                owners[location.physicalAddress] = address
            }
        }
        return null
    }
}
