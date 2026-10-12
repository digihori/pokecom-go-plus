package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.debug.Sc61860AssemblySymbolKind
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DesktopSc61ListingParserTest {
    private fun row(line: Int, address: Int, code: String, cycles: String, text: String) =
        "%05d:%04X  ".format(line, address) + code.padEnd(8) + cycles.padEnd(7) + text

    @Test fun parsesDataContinuationCyclesAndCaseSensitiveSymbolKinds() {
        val root = Files.createTempDirectory("pgp-sc61-list-").toFile()
        try {
            val source = File(root, "main.asm").apply { writeText("ORG $4000\nCount EQU $1234\nSTART: LIA Count.L\nDATA: DB \"12345\"\n") }
            val raw = listOf(
                row(1, 0x4000, "", "", "ORG $4000"),
                row(2, 0x4000, "", "", "Count: EQU $1234"),
                row(3, 0x4000, "0234", "4", "START: LIA Count.L"),
                row(4, 0x4002, "31323334", "", "DATA: DB \"12345\""),
                "            35      ",
                "SC61860 ASSEMBLER Rev 0.04a - MAP LIST OF [main.asm]",
                " Count : 1234h     START : 4000h",
                " DATA : 4002h",
            ).joinToString("\n")
            val parsed = DesktopSc61ListingParser.parse(raw, source)
            assertEquals(raw, parsed.raw)
            assertEquals("4", parsed.rows[2].cycles)
            assertEquals(listOf(0x31, 0x32, 0x33, 0x34, 0x35), parsed.rows[3].bytes)
            assertEquals(3, parsed.listing[2].line)
            assertEquals(Sc61860AssemblySymbolKind.CONSTANT, parsed.symbols.first().kind)
            assertEquals(0x1234, parsed.symbols.first().value)
            assertEquals(Sc61860AssemblySymbolKind.LABEL, parsed.symbols[1].kind)
            assertEquals(source.canonicalFile, parsed.symbolLocations.getValue("START").file)
        } finally { root.deleteRecursively() }
    }

    @Test fun handlesTabListingAndShiftJisComments() {
        val root = Files.createTempDirectory("pgp-sc61-tab-").toFile()
        try {
            val source = File(root, "main.asm").apply { writeText("START: LIA $34 ; 日本語\n") }
            val text = "00001:4000  0234\t4\tSTART:\tLIA\t$34 ; 日本語\n"
            val decoded = DesktopSc61ListingParser.decode(text.toByteArray(java.nio.charset.Charset.forName("windows-31j")))
            val parsed = DesktopSc61ListingParser.parse(decoded, source)
            assertEquals("4", parsed.rows.single().cycles)
            assertEquals(listOf(2, 0x34), parsed.rows.single().bytes)
            assertEquals(1, parsed.rows.single().location?.line)
            assertTrue(parsed.raw.contains("日本語"))
        } finally { root.deleteRecursively() }
    }

    @Test fun resolvesIncludeErrorsOnlyWhenUnambiguous() {
        val root = Files.createTempDirectory("pgp-sc61-include-").toFile()
        try {
            val source = File(root, "main.asm").apply { writeText("ORG $4000\nINCLUDE (inc.s)\n") }
            val include = File(root, "inc.s").apply { writeText("BOGUS\n") }
            val log = row(1, 0x4000, "", "", "BOGUS") + "\nLINE:1 ERROR CODE:017 Operation Type Mismatch\n"
            val location = assertNotNull(DesktopSc61ListingParser.diagnostic(log, source))
            assertEquals(include.canonicalFile, location.file)
            assertEquals(1, location.line)
            source.writeText("BOGUS\nINCLUDE (inc.s)\n")
            assertNull(DesktopSc61ListingParser.diagnostic(log, source))
        } finally { root.deleteRecursively() }
    }
}
