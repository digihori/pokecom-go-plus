package com.digihori.pgp.core.debug

public enum class Sc61860OperandEncoding {
    NONE,
    IMMEDIATE_8,
    ADDRESS_16,
    RELATIVE_FORWARD,
    RELATIVE_BACKWARD,
    PAGE_ADDRESS,
    EMBEDDED_6,
    CASE1_DATA,
}

public enum class Sc61860Flow {
    NEXT,
    CONDITIONAL_BRANCH,
    JUMP,
    CALL,
    RETURN,
    TABLE_BRANCH,
}

public data class Sc61860InstructionDefinition(
    public val opcode: Int,
    public val mnemonic: String,
    public val length: Int,
    public val operandEncoding: Sc61860OperandEncoding = Sc61860OperandEncoding.NONE,
    public val flow: Sc61860Flow = Sc61860Flow.NEXT,
    public val encodable: Boolean = true,
)

/** Shared SC61860 metadata for the debugger, disassembler, and future assembler. */
public object Sc61860InstructionSet {
    private val definitions: Array<Sc61860InstructionDefinition?> = arrayOfNulls(256)

    init {
        defineRow(0x00, "LII LIJ LIA LIB IX DX IY DY MVW EXW MVB EXB ADN SBN ADW SBW")
        defineRow(0x10, "LIDP LIDL LIP LIQ ADB SBB - - MVWD EXWD MVBD EXBD SRW SLW FILM FILD")
        defineRow(0x20, "LDP LDQ LDR CLRA IXL DXL IYS DYS JRNZP JRNZM JRNCP JRNCM JRP JRM - LOOP")
        defineRow(0x30, "STP STQ STR NOPT PUSH MVWP - RTN JRZP JRZM JRCP JRCM - - - -")
        defineRow(0x40, "INCI DECI INCA DECA ADM SBM ANMA ORMA INCK DECK INCM DECM INA NOPW WAIT CUP")
        defineRow(0x50, "INCP DECP STD MVDM MVMP MVMD LDPC LDD SWP LDM SL POP - OUTA - OUTF")
        defineRow(0x60, "ANIM ORIM TSIM CPIM ANIA ORIA TSIA CPIA NOPT CASE2 NOPT TEST - - - CDN")
        defineRow(0x70, "ADIM SBIM - - ADIA SBIA - - CALL JP CASE1 - JPNZ JPNC JPZ JPC")
        defineRow(0xc0, "INCJ DECJ INCB DECB ADCM SBCM TSMA CPMA INCL DECL INCN DECN INB NOPW NOPT -")
        defineRow(0xd0, "SC RC SR NOPW ANID ORID TSID - LEAVE NOPW EXAB EXAM - OUTB - OUTC")

        for (opcode in 0x80..0xbf) define(opcode, "LP", Sc61860OperandEncoding.EMBEDDED_6)
        for (opcode in 0xe0..0xff) {
            define(
                opcode,
                "CAL",
                Sc61860OperandEncoding.PAGE_ADDRESS,
                Sc61860Flow.CALL,
            )
        }

        setOperand(0x00..0x03, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x11..0x13, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x4e..0x4e, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x60..0x67, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x70..0x71, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x74..0x75, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0xd4..0xd6, Sc61860OperandEncoding.IMMEDIATE_8)
        setOperand(0x10..0x10, Sc61860OperandEncoding.ADDRESS_16)
        setOperand(0x78..0x79, Sc61860OperandEncoding.ADDRESS_16)
        setOperand(0x7c..0x7f, Sc61860OperandEncoding.ADDRESS_16)
        setRelative(intArrayOf(0x28, 0x2a, 0x2c, 0x38, 0x3a), forward = true)
        setRelative(intArrayOf(0x29, 0x2b, 0x2d, 0x2f, 0x39, 0x3b), forward = false)
        replace(0x7a, Sc61860OperandEncoding.CASE1_DATA, Sc61860Flow.NEXT)

        setFlow(intArrayOf(0x28, 0x29, 0x2a, 0x2b, 0x38, 0x39, 0x3a, 0x3b), Sc61860Flow.CONDITIONAL_BRANCH)
        setFlow(intArrayOf(0x2c, 0x2d, 0x79), Sc61860Flow.JUMP)
        setFlow(intArrayOf(0x78), Sc61860Flow.CALL)
        setFlow(intArrayOf(0x37), Sc61860Flow.RETURN)
        setFlow(intArrayOf(0x69), Sc61860Flow.TABLE_BRANCH)
        setFlow(intArrayOf(0x7c, 0x7d, 0x7e, 0x7f), Sc61860Flow.CONDITIONAL_BRANCH)

        // These opcodes are executed by the compatibility core but are not documented as
        // assembler mnemonics in the reference table. Preserve their consumed length only.
        intArrayOf(0x72, 0x73, 0x76, 0x77, 0xd7).forEach { opcode ->
            definitions[opcode] = Sc61860InstructionDefinition(
                opcode,
                "UNDOC",
                2,
                Sc61860OperandEncoding.IMMEDIATE_8,
                encodable = false,
            )
        }
    }

    public fun find(opcode: Int): Sc61860InstructionDefinition? =
        definitions[opcode and 0xff]

    public fun allDefined(): List<Sc61860InstructionDefinition> = definitions.filterNotNull()

    private fun defineRow(start: Int, mnemonics: String) {
        mnemonics.split(' ').forEachIndexed { index, mnemonic ->
            if (mnemonic != "-") define(start + index, mnemonic)
        }
    }

    private fun define(
        opcode: Int,
        mnemonic: String,
        operand: Sc61860OperandEncoding = Sc61860OperandEncoding.NONE,
        flow: Sc61860Flow = Sc61860Flow.NEXT,
    ) {
        definitions[opcode] = Sc61860InstructionDefinition(
            opcode,
            mnemonic,
            operand.length,
            operand,
            flow,
        )
    }

    private fun setOperand(opcodes: IntRange, operand: Sc61860OperandEncoding) {
        opcodes.forEach { replace(it, operand, definitions[it]?.flow ?: Sc61860Flow.NEXT) }
    }

    private fun setRelative(opcodes: IntArray, forward: Boolean) {
        val operand = if (forward) {
            Sc61860OperandEncoding.RELATIVE_FORWARD
        } else {
            Sc61860OperandEncoding.RELATIVE_BACKWARD
        }
        opcodes.forEach { replace(it, operand, definitions[it]?.flow ?: Sc61860Flow.JUMP) }
    }

    private fun setFlow(opcodes: IntArray, flow: Sc61860Flow) {
        opcodes.forEach { opcode ->
            val current = checkNotNull(definitions[opcode])
            definitions[opcode] = current.copy(flow = flow)
        }
    }

    private fun replace(opcode: Int, operand: Sc61860OperandEncoding, flow: Sc61860Flow) {
        val current = checkNotNull(definitions[opcode])
        definitions[opcode] = current.copy(length = operand.length, operandEncoding = operand, flow = flow)
    }

    private val Sc61860OperandEncoding.length: Int
        get() = when (this) {
            Sc61860OperandEncoding.NONE -> 1
            Sc61860OperandEncoding.EMBEDDED_6 -> 1
            Sc61860OperandEncoding.IMMEDIATE_8,
            Sc61860OperandEncoding.RELATIVE_FORWARD,
            Sc61860OperandEncoding.RELATIVE_BACKWARD,
            Sc61860OperandEncoding.PAGE_ADDRESS -> 2
            Sc61860OperandEncoding.ADDRESS_16 -> 3
            Sc61860OperandEncoding.CASE1_DATA -> 4
        }
}
