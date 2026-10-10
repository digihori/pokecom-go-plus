package com.digihori.pgp.desktop.source

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSourceHeaderParserTest {
    @Test
    fun extractsLeadingDescriptionAndPreservesLineNumbers() {
        val parsed = DesktopSourceHeaderParser.parse("# Demo\n# CALL &C000\n10 PRINT \"OK\"\n")

        assertEquals("Demo\nCALL &C000", parsed.description)
        assertEquals("\n\n10 PRINT \"OK\"\n", parsed.sourceText)
    }

    @Test
    fun stopsAtFirstNonHeaderLine() {
        val parsed = DesktopSourceHeaderParser.parse("# Header\nORG 0xC000\n# not metadata")

        assertEquals("Header", parsed.description)
        assertEquals("\nORG 0xC000\n# not metadata", parsed.sourceText)
    }
}
