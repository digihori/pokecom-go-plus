package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class Pc1350BasicTokenizerTest {
    @Test
    fun encodesS1LineNumberLengthKeywordsAndMarkers() {
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse("10 PRINT \"HELLO\"\n20 END"),
        ).document
        val result = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1350BasicTokenizer.tokenize(document))
        assertContentEquals(
            byteArrayOf(
                0xff.toByte(), 0x00, 0x0a, 0x09, 0xde.toByte(), 0x22, 0x48, 0x45, 0x4c, 0x4c, 0x4f, 0x22, 0x0d,
                0x00, 0x14, 0x02, 0xd8.toByte(), 0x0d, 0xff.toByte(),
            ),
            result.bytes,
        )
    }

    @Test
    fun usesTheVerifiedS1CodesForPrintAndInput() {
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse("10 PRINT A\n20 INPUT B"),
        ).document
        val bytes = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1350BasicTokenizer.tokenize(document)).bytes
        kotlin.test.assertEquals(0xde, bytes[4].toInt() and 0xff)
        val secondLine = 1 + 3 + (bytes[3].toInt() and 0xff)
        kotlin.test.assertEquals(0xdf, bytes[secondLine + 3].toInt() and 0xff)
    }

    @Test
    fun encodesUnicodeHalfWidthKanaUsingPcwavS1Codes() {
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse("10 PRINT \"ｱｲｳ ﾊﾟ\""),
        ).document
        val bytes = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1350BasicTokenizer.tokenize(document)).bytes

        assertContentEquals(
            byteArrayOf(
                0xff.toByte(), 0x00, 0x0a, 0x0f,
                0xde.toByte(), 0x22,
                0xfe.toByte(), 0xb1.toByte(),
                0xfe.toByte(), 0xb2.toByte(),
                0xfe.toByte(), 0xb3.toByte(),
                0x20,
                0xfe.toByte(), 0xca.toByte(),
                0xfe.toByte(), 0xdf.toByte(),
                0x22, 0x0d, 0xff.toByte(),
            ),
            bytes,
        )
    }
}
