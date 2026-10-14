package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.api.MachineMemoryRegion
import com.digihori.pgp.desktop.debug.CpuField

internal fun CpuField.uiLabel(strings: StudioStrings): String = when (this) {
    CpuField.CURRENT_PC -> "CURRENT_PC"
    CpuField.OPCODE -> "OP"
    CpuField.CARRY -> "C"
    CpuField.ZERO -> "Z"
    CpuField.CONTROL -> "CTRL"
    CpuField.INTERNAL_RAM -> strings.text("debug.internalRam")
    else -> name
}

internal fun StudioStrings.memoryRegionLabel(region: MachineMemoryRegion): String = when (region.displayName) {
    "RAM mirror" -> text("debug.region.ramMirror")
    "Display mirror" -> text("debug.region.displayAlias")
    "Internal ROM" -> text("debug.region.internal")
    "External ROM" -> text("debug.region.external")
    "Display memory" -> text("debug.region.display")
    "Display memory mirror" -> text("debug.region.displayMirror")
    "Control and display memory" -> text("debug.region.controlDisplay")
    "Banked ROM window" -> text("debug.region.banked")
    "RAM (fixed 16 KiB card)" -> text("debug.region.fixed16")
    "Compatibility control/RAM window" -> text("debug.region.compatControl")
    "Control / LCD" -> text("debug.region.controlLcd")
    "RAM (fixed 32 KiB card)" -> text("debug.region.fixed32")
    "RAM (32 KiB compatibility window)" -> text("debug.region.compat32")
    "ROM read mirror" -> text("debug.region.romMirror")
    else -> region.displayName
}

internal fun StudioStrings.debugContextRequestError(message: String): String {
    val known = when (message) {
        "Memory ranges must be non-empty and remain inside the 16-bit address space" -> text("debug.request.memoryRanges")
        "Disassembly start address must be a 16-bit address" -> text("debug.request.disassemblyAddress")
        "Disassembly instruction count is out of range" -> text("debug.request.instructionRange")
        "Disassembly start address is required when instructions are requested" -> text("debug.request.addressRequired")
        else -> null
    }
    if (known != null) return known
    Regex("At most (\\d+) memory ranges may be requested").matchEntire(message)?.let { return text("debug.request.rangeCount", it.groupValues[1]) }
    Regex("At most (\\d+) memory bytes may be requested").matchEntire(message)?.let { return text("debug.request.byteCount", it.groupValues[1]) }
    Regex("History and trace limits must be between 0 and (\\d+)").matchEntire(message)?.let { return text("debug.request.history", it.groupValues[1]) }
    return text("debug.request.unknown", message)
}
