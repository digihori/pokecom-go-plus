package com.digihori.pgp.core.emulator.machine.pc1261

import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicDetokenizeResult
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertEquals

class Pc1261BasicTokenizerTest {
    @Test
    fun usesPc1261S1TokenValues() {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse("10 PRINT 1")).document
        val result = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1261BasicTokenizer.tokenize(document))
        assertContentEquals(
            byteArrayOf(0xff.toByte(), 0, 10, 3, 0xde.toByte(), '1'.code.toByte(), 0x0d, 0xff.toByte()),
            result.bytes,
        )
    }

    @Test
    fun doesNotEmitPc1350OnlyChainToken() {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse("10 CHAIN")).document
        val result = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1261BasicTokenizer.tokenize(document))
        assertContentEquals("CHAIN".encodeToByteArray(), result.bytes.copyOfRange(4, 9))
    }

    @Test
    fun detokenizesPc1261KeywordsStringsAndKana() {
        val document = assertIs<BasicTextParseResult.Success>(
            BasicTextParser.parse("10 PRINT A\n20 PRINT \"ｱｲ\"\n30 REM PC1261"),
        ).document
        val encoded = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1261BasicTokenizer.tokenize(document))
        val decoded = assertIs<Pc1350BasicDetokenizeResult.Success>(Pc1261BasicTokenizer.detokenize(encoded.bytes))

        assertEquals("10 PRINT A\n20 PRINT\"ｱｲ\"\n30 REM PC1261", decoded.source)
    }

    @Test
    fun detokenizerRejectsUnknownS1Token() {
        val result = Pc1261BasicTokenizer.detokenize(
            byteArrayOf(0xff.toByte(), 0, 10, 2, 0xe5.toByte(), 0x0d, 0xff.toByte()),
        )
        // Undefined PC-1261 codes remain representable in Studio source instead of being mislabelled.
        assertEquals(
            "10 \\xE5",
            assertIs<Pc1350BasicDetokenizeResult.Success>(result).source,
        )
    }
}
