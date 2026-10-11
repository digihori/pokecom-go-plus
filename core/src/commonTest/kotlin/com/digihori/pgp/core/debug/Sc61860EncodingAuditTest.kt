package com.digihori.pgp.core.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Independent golden table for PGP's supported dialect, not generated from production metadata or ROM. */
class Sc61860EncodingAuditTest {
    private val rows = listOf(
        "LII LIJ LIA LIB IX DX IY DY MVW EXW MVB EXB ADN SBN ADW SBW",
        "LIDP LIDL LIP LIQ ADB SBB - - MVWD EXWD MVBD EXBD SRW SLW FILM FILD",
        "LDP LDQ LDR CLRA IXL DXL IYS DYS JRNZP JRNZM JRNCP JRNCM JRP JRM - LOOP",
        "STP STQ STR NOPT PUSH MVWP - RTN JRZP JRZM JRCP JRCM - - - -",
        "INCI DECI INCA DECA ADM SBM ANMA ORMA INCK DECK INCM DECM INA NOPW WAIT CUP",
        "INCP DECP STD MVDM MVMP MVMD LDPC LDD SWP LDM SL POP - OUTA - OUTF",
        "ANIM ORIM TSIM CPIM ANIA ORIA TSIA CPIA NOPT CASE2 NOPT TEST - - - CDN",
        "ADIM SBIM UNDOC UNDOC ADIA SBIA UNDOC UNDOC CALL JP CASE1 - JPNZ JPNC JPZ JPC",
        List(16) { "LP" }.joinToString(" "), List(16) { "LP" }.joinToString(" "),
        List(16) { "LP" }.joinToString(" "), List(16) { "LP" }.joinToString(" "),
        "INCJ DECJ INCB DECB ADCM SBCM TSMA CPMA INCL DECL INCN DECN INB NOPW NOPT -",
        "SC RC SR NOPW ANID ORID TSID UNDOC LEAVE NOPW EXAB EXAM - OUTB - OUTC",
        List(16) { "CAL" }.joinToString(" "), List(16) { "CAL" }.joinToString(" "),
    )
    private val mnemonics = rows.flatMap { it.split(' ') }
    private val immediate = setOf(0x00, 0x01, 0x02, 0x03, 0x11, 0x12, 0x13, 0x4e,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x6b, 0x70, 0x71,
        0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0xd4, 0xd5, 0xd6, 0xd7)
    private val absolute = setOf(0x10, 0x78, 0x79, 0x7c, 0x7d, 0x7e, 0x7f)
    private val forward = setOf(0x28, 0x2a, 0x2c, 0x38, 0x3a)
    private val backward = setOf(0x29, 0x2b, 0x2d, 0x2f, 0x39, 0x3b)
    private fun length(opcode: Int): Int = when {
        opcode == 0x7a -> 4
        opcode in absolute -> 3
        opcode in immediate || opcode in forward || opcode in backward || opcode >= 0xe0 -> 2
        else -> 1
    }

    @Test fun checksEveryOpcodeNameLengthAndCanonicalEncodingAgainstGoldenValues() {
        for (opcode in 0..255) {
            val mnemonic = mnemonics[opcode]
            val metadata = Sc61860InstructionSet.find(opcode)
            if (mnemonic == "-") { assertNull(metadata, "opcode $opcode"); continue }
            assertEquals(mnemonic, metadata?.mnemonic, "opcode $opcode")
            assertEquals(length(opcode), metadata?.length, "opcode $opcode")
            assertEquals(mnemonic != "UNDOC", metadata?.encodable, "opcode $opcode")
            if (mnemonic == "UNDOC" || (mnemonics.indexOf(mnemonic) != opcode && mnemonic !in listOf("LP", "CAL"))) continue
            val operand = when {
                opcode in immediate -> " 165"
                opcode in absolute -> " 0xABCD"
                opcode in forward -> " 0x10A6"
                opcode in backward -> " 0x0F5C"
                opcode == 0x7a -> " 0xA5ABCD"
                mnemonic == "LP" -> " ${opcode and 0x3f}"
                mnemonic == "CAL" -> " ${((opcode and 0x1f) shl 8) or 0xa5}"
                else -> ""
            }
            val bytes = when (length(opcode)) {
                1 -> listOf(opcode)
                2 -> listOf(opcode, 0xa5)
                3 -> listOf(opcode, 0xab, 0xcd)
                else -> listOf(opcode, 0xa5, 0xab, 0xcd)
            }
            val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble("ORG 0x1000\n$mnemonic$operand"))
            assertEquals(bytes, assembled.listing.single().bytes, mnemonic)
        }
    }

    @Test fun preservesAllOpcodesAndOperandBytesOnExportAndReassembly() {
        for (opcode in 0..255) for (value in listOf(0, 1, 0x7f, 0xff)) {
            val bytes = List(length(opcode)) { if (it == 0) opcode else value }
            val source = Sc61860Disassembler.renderAssembly(0x1000, 0x1000 + bytes.lastIndex) { bytes[it - 0x1000] }
            val result = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source), source)
            assertEquals(bytes, result.listing.flatMap { it.bytes }, source)
        }
    }

    @Test fun neverReadsOutsideATruncatedExportRangeIncludingFFFF() {
        for (opcode in 0..255) for (size in 1..length(opcode)) {
            val bytes = List(size) { if (it == 0) opcode else 0xa5 }
            val start = 0x10000 - size
            val source = Sc61860Disassembler.renderAssembly(start, 0xffff) { bytes[it - start] }
            val result = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source), source)
            assertEquals(bytes, result.listing.flatMap { it.bytes })
        }
    }

    @Test fun validatesValuesAndBranchBoundaries() {
        fun success(code: String) = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(code))
        fun failure(code: String) = assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble(code))
        for (mnemonic in listOf("LII", "WAIT", "TEST")) {
            for (value in listOf(0, 255)) success("ORG 0\n$mnemonic $value")
            for (value in listOf(-1, 256)) failure("ORG 0\n$mnemonic $value")
            failure("ORG 0\n$mnemonic")
        }
        for ((name, max) in listOf("LIDP" to 65535, "CASE1" to 0xffffff, "LP" to 63, "CAL" to 8191)) {
            for (value in listOf(0, max)) success("ORG 0\n$name $value")
            for (value in listOf(-1, max + 1)) failure("ORG 0\n$name $value")
        }
        for (name in listOf("JRP", "JRNZP", "JRNCP", "JRZP", "JRCP")) {
            for (offset in listOf(0, 255)) success("ORG 0x1000\n$name ${0x1001 + offset}")
            failure("ORG 0x1000\n$name ${0x1001 + 256}")
        }
        for (name in listOf("JRM", "JRNZM", "JRNCM", "JRZM", "JRCM", "LOOP")) {
            for (offset in listOf(0, 255)) success("ORG 0x1000\n$name ${0x1001 - offset}")
            failure("ORG 0x1000\n$name ${0x1001 - 256}")
        }
        assertEquals(listOf(0x2c, 1), success("ORG 0xFFFE\nJRP 0").listing.single().bytes)
        assertEquals(listOf(0x2d, 2), success("ORG 0\nJRM 0xFFFF").listing.single().bytes)
        failure("ORG 0\nJRM -1")
        failure("ORG 0\nJRP 65536")
    }

    @Test fun reportsMalformedDataSymbolsAndOutputBoundaries() {
        for (data in listOf("", ",1", "1,", "1,,2")) {
            assertEquals(2, assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0\nDB $data")).line)
        }
        assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0\nX: DB 0\nx: RTN"))
        assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0\nCALL MISSING"))
        assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0xFFFF\nLII 1"))
        assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0xFFFF\nDB 1\nAFTER:"))
        val overlap = assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0x1000\nDB 1\nORG 0x1000\nDB 2"))
        assertEquals(3, overlap.line)
        val valid = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble("\uFEFFORG $1000\r\nA: DB 1 ; comment\rORG &2000\rDB 2"))
        assertEquals(listOf(0x1000, 0x2000), valid.image.segments.map { it.startAddress })
    }

    @Test fun assembledTestConsumesItsImmediateBeforeTheNextCpuInstruction() {
        val result = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble("ORG 0x1000\nTEST 1\nLIA 0xA5"))
        val bytes = result.image.segments.single().copyBytes()
        val cpu = com.digihori.pgp.core.emulator.cpu.Sc61860Cpu(object : com.digihori.pgp.core.emulator.cpu.Sc61860Bus {
            override fun read(address: Int): Int = bytes[address - 0x1000].toInt() and 0xff
            override fun write(address: Int, value: Int) = Unit
        })
        cpu.state.programCounter = 0x1000
        assertNull(cpu.step().stopReason)
        assertEquals(0x1002, cpu.state.programCounter)
        assertNull(cpu.step().stopReason)
        assertEquals(0xa5, cpu.state.internalRam[2])
        assertEquals(0x1004, cpu.state.programCounter)
    }
}
