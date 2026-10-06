package com.digihori.pgp.core.debug

public data class Sc61860DisassemblyLine(
    public val instruction: Sc61860DecodedInstruction,
    public val sourceText: String,
)

public object Sc61860Disassembler {
    public fun disassemble(
        startAddress: Int,
        endAddressInclusive: Int,
        readByte: (Int) -> Int,
    ): List<Sc61860DisassemblyLine> {
        require(startAddress in 0..0xffff)
        require(endAddressInclusive in startAddress..0xffff)
        val lines = mutableListOf<Sc61860DisassemblyLine>()
        var address = startAddress
        while (address <= endAddressInclusive) {
            val decoded = Sc61860InstructionDecoder.decode(address, readByte)
            val remaining = endAddressInclusive - address + 1
            if (decoded.length > remaining) {
                repeat(remaining) { offset ->
                    val dataAddress = address + offset
                    val instruction = dataByte(dataAddress, readByte(dataAddress))
                    lines += Sc61860DisassemblyLine(
                        instruction = instruction,
                        sourceText = Sc61860InstructionFormatter.format(instruction),
                    )
                }
                break
            }
            lines += Sc61860DisassemblyLine(
                instruction = decoded,
                sourceText = Sc61860InstructionFormatter.format(decoded),
            )
            address += decoded.length
        }
        return lines
    }

    public fun renderAssembly(
        startAddress: Int,
        endAddressInclusive: Int,
        readByte: (Int) -> Int,
    ): String = buildString {
        append("ORG 0x")
        append(startAddress.hex(4))
        append('\n')
        disassemble(startAddress, endAddressInclusive, readByte).forEach { line ->
            val instruction = line.instruction
            val bytes = instruction.bytes.joinToString(" ") { it.hex(2) }
            append(line.sourceText.padEnd(SOURCE_COLUMN_WIDTH))
            append(" ; ")
            append(instruction.address.hex(4))
            append(": ")
            append(bytes)
            append('\n')
        }
    }

    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')

    private fun dataByte(address: Int, value: Int): Sc61860DecodedInstruction =
        Sc61860DecodedInstruction(
            address = address,
            definition = null,
            bytes = listOf(value and 0xff),
            operandValue = null,
            targetAddress = null,
        )

    private const val SOURCE_COLUMN_WIDTH: Int = 20
}
