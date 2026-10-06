package com.digihori.pgp.core.debug

public data class Sc61860DecodedInstruction(
    public val address: Int,
    public val definition: Sc61860InstructionDefinition?,
    public val bytes: List<Int>,
    public val operandValue: Int?,
    public val targetAddress: Int?,
) {
    public val length: Int get() = bytes.size
    public val nextAddress: Int get() = (address + length) and 0xffff
}

public object Sc61860InstructionDecoder {
    public fun decode(address: Int, readByte: (Int) -> Int): Sc61860DecodedInstruction {
        require(address in 0..0xffff)
        val opcode = readByte(address) and 0xff
        val definition = Sc61860InstructionSet.find(opcode)
        val length = definition?.length ?: 1
        val bytes = List(length) { offset -> readByte((address + offset) and 0xffff) and 0xff }
        val operand = when (definition?.operandEncoding) {
            Sc61860OperandEncoding.IMMEDIATE_8,
            Sc61860OperandEncoding.RELATIVE_FORWARD,
            Sc61860OperandEncoding.RELATIVE_BACKWARD,
            Sc61860OperandEncoding.PAGE_ADDRESS -> bytes[1]
            Sc61860OperandEncoding.EMBEDDED_6 -> opcode and 0x3f
            Sc61860OperandEncoding.ADDRESS_16 -> (bytes[1] shl 8) or bytes[2]
            Sc61860OperandEncoding.CASE1_DATA ->
                (bytes[1] shl 16) or (bytes[2] shl 8) or bytes[3]
            Sc61860OperandEncoding.NONE, null -> null
        }
        val target = when (definition?.operandEncoding) {
            Sc61860OperandEncoding.ADDRESS_16 -> operand
            Sc61860OperandEncoding.RELATIVE_FORWARD -> ((address + 1) + checkNotNull(operand)) and 0xffff
            Sc61860OperandEncoding.RELATIVE_BACKWARD -> ((address + 1) - checkNotNull(operand)) and 0xffff
            Sc61860OperandEncoding.PAGE_ADDRESS -> ((opcode and 0x1f) shl 8) or checkNotNull(operand)
            else -> null
        }
        return Sc61860DecodedInstruction(address, definition, bytes, operand, target)
    }
}

public object Sc61860InstructionFormatter {
    public fun format(instruction: Sc61860DecodedInstruction): String {
        val definition = instruction.definition
            ?: return "DB 0x${instruction.bytes.first().hex(2)}"
        val operand = when (definition.operandEncoding) {
            Sc61860OperandEncoding.NONE -> ""
            Sc61860OperandEncoding.IMMEDIATE_8 -> "0x${checkNotNull(instruction.operandValue).hex(2)}"
            Sc61860OperandEncoding.ADDRESS_16,
            Sc61860OperandEncoding.RELATIVE_FORWARD,
            Sc61860OperandEncoding.RELATIVE_BACKWARD,
            Sc61860OperandEncoding.PAGE_ADDRESS -> "0x${checkNotNull(instruction.targetAddress).hex(4)}"
            Sc61860OperandEncoding.CASE1_DATA -> "0x${checkNotNull(instruction.operandValue).hex(6)}"
            Sc61860OperandEncoding.EMBEDDED_6 -> "0x${checkNotNull(instruction.operandValue).hex(2)}"
        }
        return if (operand.isEmpty()) definition.mnemonic else "${definition.mnemonic} $operand"
    }

    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
}
