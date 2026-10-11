package com.digihori.pgp.core.debug

import kotlin.test.*

class Sc61860AssemblyExpressionsTest {
    @Test fun acceptsHashCommentsAnywhereAndPreservesQuotedHashes() {
        val result = success("""
            # leading comment
            ORG 0x1000 # origin
            # body comment
            VALUE EQU 7 # constant
            DB "#;", '#', VALUE # data
            LIA VALUE # instruction
            ; semicolon comment
            RTN
        """.trimIndent())
        assertEquals(listOf(35, 59, 35, 7, 2, 7, 0x37), result.listing.flatMap { it.bytes })
        assertEquals(listOf(5, 6, 8), result.listing.map { it.line })
        failure("ORG 0\n# comment\nDB MISSING # comment", "Undefined symbol", 3)
    }

    private fun success(source: String) = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source), source)
    private fun bytes(source: String): List<Int> = success(source).listing.flatMap { it.bytes }
    private fun failure(source: String, message: String, line: Int? = null) {
        val result = assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble(source), source)
        assertTrue(result.message.contains(message, ignoreCase = true), result.message)
        line?.let { assertEquals(it, result.line) }
    }

    @Test fun evaluatesPrecedenceUnaryBitOperatorsAndCharacterValues() {
        assertEquals(listOf(14, 20, 4, 1, 5, 3, 0, 255, 65, 59, 8, 1, 10, 2), bytes("""
            ORG 0x1000
            DB 2+3*4, (2+3)*4, 16 >> 1+1, 7&3^2, 1|4, -7/-2, -7%7, ~0 & 255
            DB 'A', ';', 1 << 3, +1, LOW(0x120A), HIGH(0x0201)
        """.trimIndent()))
        val result = success("A EQU -2147483648\nB EQU 2147483647\nORG 0\nDB (A>>31)&255, B&255")
        assertEquals(Int.MIN_VALUE, result.symbols[0].value)
        assertEquals(Int.MAX_VALUE, result.symbols[1].value)
        assertEquals(listOf(255,255), result.listing.single().bytes)
        assertEquals(listOf(10, 11, 12, 13), bytes("ORG $1000\nDB $0A, &0B, 0x0C, 13"))
    }

    @Test fun resolvesForwardEquChainsLabelsAndCaseInsensitiveReferences() {
        val result = success("""
            BASE EQU LATER - 2
            COUNT: EQU MORE + 1
            MORE EQU 2
            ORG 0xC500
            LIA count
            LIDP base
            DW later
            LATER: RTN
        """.trimIndent())
        assertEquals(listOf(2, 3, 0x10, 0xc5, 5, 0xc5, 7, 0x37), result.listing.flatMap { it.bytes })
        assertEquals(0xc505, result.symbols.first().value)
        assertEquals(Sc61860AssemblySymbolKind.CONSTANT, result.symbols.first().kind)
        assertEquals(Sc61860AssemblySymbolKind.LABEL, result.symbols.last().kind)
    }

    @Test fun usesStatementPositionAndTheEquDefinitionPosition() {
        assertEquals(listOf(0x10, 0, 0, 0, 4, 0x10, 0, 0x10, 4), bytes("""
            ORG 0x1000
            HERE EQU .
            DW ., LOW(.)
            DB LOW(.), HIGH(HERE), LOW(HERE), HIGH(.), LOW(.)
        """.trimIndent()))
    }

    @Test fun emitsStringsWithLiteralPunctuationAndEscapes() {
        assertEquals(listOf(65, 59, 44, 58, 34, 92, 10, 13, 9, 0, 255, 66), bytes("""
            ORG 0
            DB "A;,:\"\\\n\r\t\0\xFF", 'B' ; ignored
        """.trimIndent()))
        assertEquals(listOf(1, 2), bytes("ORG 0\nDB 1, \"\", 2"))
        failure("ORG 0\nDB \"日本\"", "one byte", 2)
        failure("ORG 0\nDB '\\xGG'", "hex escape", 2)
        failure("ORG 0\nDB '\\x+1'", "hex escape", 2)
        failure("ORG 0\nDB 'AB'", "exactly one", 2)
        failure("ORG 0\nDB \"unfinished; comment", "Unterminated", 2)
        failure("ORG 0\nDB \"a\"+1", "literal", 2)
        failure("ORG 0\nDB \"\\q\"", "Unknown escape", 2)
    }

    @Test fun emitsBigEndianWordsAndFilledReservationAlignment() {
        val result = success("""
            N EQU 2
            ORG 0x1001
            DB 1
            DS N, 0xAA
            ALIGN 8, 0xBB
            START: DW 0x1234, 0xFFFF
            DS 0
            ALIGN 4
            DB 2
        """.trimIndent())
        assertEquals(listOf(1, 0xaa, 0xaa, 0xbb, 0xbb, 0xbb, 0xbb, 0x12, 0x34, 255, 255, 2), result.listing.flatMap { it.bytes })
        assertEquals(0x1008, result.symbols.last().address)
        assertEquals(listOf(3, 4, 5, 6, 9), result.listing.map { it.line })
        assertEquals(listOf(0, 0, 1), bytes("ORG 0\nDS 2\nDB 1"))
        failure("ORG 0\nDW -1", "range", 2)
        failure("ORG 0\nDW 65536", "range", 2)
        failure("ORG 0\nALIGN 3", "power of two", 2)
        failure("ORG 0\nDS -1", "size", 2)
        failure("ORG 0\nDS 1,256", "fill byte", 2)
        failure("ORG 0xFFFF\nDW 0", "exceeds", 2)
        failure("ORG 0xFFFF\nDS 2", "exceeds", 2)
        assertEquals(65536, success("ORG 0\nDS 65536").image.byteCount)
    }

    @Test fun layoutMayUseForwardNumericConstantsButNotUnresolvedLabels() {
        val result = success("""
            ORG BASE+1
            DS COUNT
            ALIGN BOUNDARY
            END: RTN
            BASE EQU 0x1000
            COUNT EQU 2
            BOUNDARY EQU 8
        """.trimIndent())
        assertEquals(0x1008, result.symbols.first().address)
        failure("ORG END\nEND: RTN", "already resolved", 1)
        failure("ORG 0\nDS END-.\nEND: RTN", "already resolved", 2)
        failure("ORG 0\nALIGN BOUNDARY\nBOUNDARY EQU END\nEND: RTN", "already resolved", 3)
        failure("WHERE EQU .\nORG 0\nDW WHERE", "Current position", 1)
        assertEquals(listOf(1), bytes("ORG 0x1000\nBASE: ORG BASE+0x100\nDB 1"))
    }

    @Test fun rejectsUndefinedDuplicateCyclicAndOverflowingExpressions() {
        failure("ORG 0\nDB MISSING+1", "Undefined symbol", 2)
        failure("A EQU B\nB EQU A\nORG 0\nDB 1", "Circular EQU")
        failure("A EQU 1\na: EQU 2\nORG 0\nDB 1", "Duplicate symbol", 2)
        failure("ORG 0\nA: DB 1\nA EQU 2", "Duplicate symbol", 3)
        failure("EQU 1\nORG 0\nDB 1", "requires a name", 1)
        for (expression in listOf("2147483648", "2147483647+1", "-2147483648/-1", "65536*65536", "1<<31"))
            failure("ORG 0\nDB $expression", "overflow", 2)
        for (expression in listOf("1/0", "1%0")) failure("ORG 0\nDB $expression", "Division by zero", 2)
        failure("ORG 0\nDB 1<<32", "Shift count", 2)
        failure("ORG 0\nDB HIGH(65536)", "16-bit", 2)
        for (expression in listOf("1+", "(1+2", "1 2", "LOW()", "1,,2"))
            assertIs<Sc61860AssemblyResult.Failure>(Sc61860Assembler.assemble("ORG 0\nDB $expression"))
        failure("ORG 0\nDB " + "(".repeat(1000) + "1" + ")".repeat(1000), "too deep", 2)
        failure("ORG 0\nDB " + List(1000) { "1" }.joinToString("+"), "too complex", 2)
    }

    @Test fun extendedDataStillRoundTripsAndUsesExpressionsInInstructions() {
        val result = success("""
            TARGET EQU END
            ORG 0xC500
            LIA LOW(TARGET)
            LIDP TARGET+1
            JRP TARGET
            DB "ABC"
            DW TARGET
            DS 2
            ALIGN 16
            END: RTN
        """.trimIndent())
        val segment = result.image.segments.single()
        val content = segment.copyBytes()
        val source = Sc61860Disassembler.renderAssembly(segment.startAddress, segment.endAddress) { content[it-segment.startAddress].toInt() }
        assertContentEquals(content, success(source).image.segments.single().copyBytes())
    }

    @Test fun evaluatesSharedConstantDependenciesWithoutRepeatedExpansion() {
        val source = buildString {
            append("C0 EQU 1\n")
            for (i in 1..30) append("C$i EQU C${i-1}+C${i-1}\n")
            append("ORG 0\nDB C30 & 255\n")
        }
        assertEquals(listOf(0), bytes(source))
    }
}
