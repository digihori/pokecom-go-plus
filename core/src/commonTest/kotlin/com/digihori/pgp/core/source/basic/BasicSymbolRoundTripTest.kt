package com.digihori.pgp.core.source.basic

import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class BasicSymbolRoundTripTest {
    private fun compile(source: String, id: String): ByteArray =
        assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(source.encodeToByteArray(), MachineId(id)), "$id: $source").bytes

    @Test fun exactSymbolCodesAndCanonicalTextRoundTripForEveryModel() {
        val groups = listOf(
            listOf("1245", "1250", "1251", "1255") to listOf(0x19, 0x1a, 0x4b, 0x4c),
            listOf("1260", "1261", "1401", "1402") to listOf(0xfb, 0xfc, 0xfd, 0xfe),
            listOf("1350", "1360", "1360k", "1450", "1460", "1470u") to listOf(0xfb, 0xfc, 0xfa, 0xf9),
        )
        for ((models, codes) in groups) for (model in models) {
            val id = "pc-$model"
            val names = if (model in groups.first().first) listOf("PI", "SQR", "EX", "BX") else listOf("PI", "SQR", "INS", "BX")
            // Strings/REM must preserve symbols even adjacent to ordinary letters.
            val text = "10 PRINT \"" + names.joinToString("") { "\\$it" } + "A¥\\\\SQR\"\n20 REM " + names.joinToString("") { "\\$it" } + "Z"
            val bytes = compile(text, id)
            val unsigned = bytes.map { it.toInt() and 255 }
            assertTrue(unsigned.windowed(codes.size).any { it == codes }, id)
            val decoded = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, MachineId(id))).utf8Bytes.decodeToString()
            names.forEach { assertTrue(decoded.contains("\\$it"), "$id: $decoded") }
            assertTrue(decoded.contains("¥"), decoded)
            assertContentEquals(bytes, compile(decoded, id), "$id: $decoded")
        }
    }

    @Test fun lc3SyntaxKeepsYenAndRootDistinct() {
        val text = "40 A=A-1: IF A¥255 GOTO #;\n55 IF INKEY$ \\\\\"0\" GOTO $=\n130 IF \\SQR S:S=S+3: IF S<40 GOTO ^: GOTO #B\n135 \\SQR S=V,X+V+V+2,Y+2"
        val bytes = compile(text, "pc-1261")
        val decoded = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, MachineId("pc-1261"))).utf8Bytes.decodeToString()
        assertTrue(decoded.contains("¥255"), decoded)
        assertTrue(decoded.contains("\\SQR"), decoded)
        assertContentEquals(bytes, compile(decoded, "pc-1261"))
    }

    @Test fun unsupportedExponentIsNotSilentlyConvertedToE() {
        for (id in listOf("pc-1261", "pc-1350", "pc-1360", "pc-1470u")) {
            assertIs<BasicProgramCompileResult.Failure>(BasicProgramCodec.compileProgram("10 PRINT \"\\EX\"".encodeToByteArray(), MachineId(id)))
            compile("10 PRINT \"E\"", id)
        }
        assertIs<BasicProgramCompileResult.Failure>(BasicProgramCodec.compileProgram("10 PRINT \"\\INS\"".encodeToByteArray(), MachineId("pc-1251")))
    }

    @Test fun yenAliasesAndEscapedBackslashHaveIdenticalBytes() {
        for (id in listOf("pc-1251", "pc-1261", "pc-1360")) {
            val expected = compile("10 PRINT \"¥\"", id)
            assertContentEquals(expected, compile("10 PRINT \"￥\"", id))
            assertContentEquals(expected, compile("10 PRINT \"\\\\\"", id))
        }
    }
}
