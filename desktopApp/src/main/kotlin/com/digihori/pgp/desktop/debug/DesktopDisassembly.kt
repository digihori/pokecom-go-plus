package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.debug.Sc61860DecodedInstruction
import com.digihori.pgp.core.debug.Sc61860InstructionDecoder
import com.digihori.pgp.core.debug.Sc61860InstructionFormatter

internal data class DesktopDisassemblyLine(
    val instruction: Sc61860DecodedInstruction,
    val byteText: String,
    val instructionText: String,
)

internal object DesktopDisassemblyModel {
    fun build(
        startAddress: Int,
        instructionCount: Int,
        readByte: (Int) -> Int,
    ): List<DesktopDisassemblyLine> {
        require(startAddress in 0..0xffff)
        require(instructionCount >= 0)
        var address = startAddress
        return List(instructionCount) {
            val instruction = Sc61860InstructionDecoder.decode(address, readByte)
            val line = DesktopDisassemblyLine(
                instruction = instruction,
                byteText = instruction.bytes.joinToString(" ") { it.hex(2) },
                instructionText = Sc61860InstructionFormatter.format(instruction),
            )
            address = instruction.nextAddress
            line
        }
    }

    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
}
