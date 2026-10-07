package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.debug.Sc61860DecodedInstruction
import com.digihori.pgp.core.api.PhysicalRomLocation

internal data class DesktopInstructionTraceEntry(
    val sequence: Long,
    val dataPointer: Int,
    val p: Int,
    val q: Int,
    val r: Int,
    val d: Int,
    val carry: Boolean,
    val zero: Boolean,
    val instruction: Sc61860DecodedInstruction,
    val romLocation: PhysicalRomLocation? = null,
)
