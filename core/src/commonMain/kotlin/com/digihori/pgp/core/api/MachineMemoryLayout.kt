package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350Display
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360Display
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
    init { require(machineId in MachineCatalog.definitions.map { it.id }) }

    /** A starter location away from the initial BASIC text and its pointer words. */
    public val assemblyStartAddress: Int get() = when (machineId.value) {
        "pc-1261", "pc-1350" -> 0x6000
        "pc-1360" -> 0xc000
        "pc-1401" -> 0x4400
        else -> 0xc500
    }

    public val defaultBasicStartAddress: Int get() = when (machineId.value) {
        "pc-1245" -> 0xc000
        "pc-1250", "pc-1251", "pc-1255" -> checkNotNull(Pc1251FamilyModel.fromMachineId(machineId)).physicalRamStart
        "pc-1261" -> 0x4080
        "pc-1350" -> 0x2030
        "pc-1360" -> 0x8030
        else -> 0x3800
    }

    public val basicStartPointerAddress: Int get() = when (machineId.value) {
        "pc-1261" -> 0x66e1
        "pc-1350" -> 0x6f01
        "pc-1360" -> 0xffd7
        "pc-1401" -> 0x46e1
        else -> 0xc6e1
    }

    public fun locate(address: Int): MachineMemoryLocation {
        require(address in 0..0xffff)
        if (address < 0x2000) return location(MachineMemoryKind.ROM, false, address)
        return when (machineId.value) {
            "pc-1245" -> {
                var mapped = address
                if (mapped in 0xb000..0xb7ff) mapped += 0x0800
                if (mapped in 0x8000..0x9fff) mapped += 0x2000
                if (mapped in 0xd000..0xd7ff) mapped -= 0x1000
                if (mapped in 0xe800..0xefff) mapped += 0x1000
                if (mapped >= 0xf900) mapped = mapped and 0xf8ff
                if (mapped in 0xb800..0xbfff) mapped += 0x0800
                when {
                    address in 0x2000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                    mapped in 0xf800..0xf8ff -> location(MachineMemoryKind.LCD, true, mapped)
                    else -> location(MachineMemoryKind.RAM, true, mapped)
                }
            }
            "pc-1250", "pc-1251", "pc-1255" -> {
                val model = checkNotNull(Pc1251FamilyModel.fromMachineId(machineId))
                val start = if (pc1251MemoryMode == Pc1251FamilyMemoryMode.EXPANDED) 0xa000 else model.physicalRamStart
                val mapped = when (address) {
                    in 0x8000..0x9fff -> address + 0x2000
                    in 0xb000..0xb7ff -> when (model) {
                        Pc1251FamilyModel.PC_1250 -> address + 0x1000
                        Pc1251FamilyModel.PC_1251 -> address + 0x0800
                        Pc1251FamilyModel.PC_1255 -> address
                    }
                    in 0xb800..0xbfff -> if (model == Pc1251FamilyModel.PC_1250) address + 0x0800 else address
                    in 0xd000..0xd7ff -> address - 0x1000
                    in 0xe800..0xefff -> 0xf800 or (address and 0xff)
                    in 0xf900..0xffff -> address and 0xf8ff
                    else -> address
                }
                when {
                    address in 0x2000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                    mapped in 0xf800..0xf8ff -> location(MachineMemoryKind.LCD, true, mapped)
                    mapped in start..0xc7ff -> location(MachineMemoryKind.RAM, true, mapped)
                    else -> location(MachineMemoryKind.UNMAPPED, false, mapped)
                }
            }
            "pc-1261" -> when (address) {
                in 0x2000..0x3fff -> location(MachineMemoryKind.LCD, true, address and 0x28ff)
                in 0x4000..0x67ff -> location(MachineMemoryKind.RAM, true, address)
                in 0x8000..0xffff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.UNMAPPED, false, address)
            }
            "pc-1350" -> when {
                isDisplayAddress(address, Pc1350Display.VRAM_BLOCK_STARTS, Pc1350Display.SYMBOL_ADDRESS) -> location(MachineMemoryKind.LCD, true, address)
                address in 0x2000..0x7fff -> location(MachineMemoryKind.RAM, true, address)
                else -> location(MachineMemoryKind.ROM, false, address)
            }
            "pc-1360" -> when {
                isDisplayAddress(address, Pc1360Display.VRAM_BLOCK_STARTS, Pc1360Display.SYMBOL_ADDRESS) -> location(MachineMemoryKind.LCD, true, address)
                address in 0x2000..0x3fff -> location(MachineMemoryKind.CONTROL, true, address)
                address in 0x4000..0x7fff -> location(MachineMemoryKind.ROM, false, address)
                else -> location(MachineMemoryKind.RAM, true, address)
            }
            else -> when (address) {
                in 0x3800..0x47ff -> location(MachineMemoryKind.RAM, true, address)
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
