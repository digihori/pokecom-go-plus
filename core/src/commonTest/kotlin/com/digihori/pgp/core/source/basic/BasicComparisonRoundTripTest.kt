package com.digihori.pgp.core.source.basic

import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class BasicComparisonRoundTripTest {
    @Test fun s1AndS2ComparisonsKeepEveryOperatorByteAndRoundTrip() {
        for (model in listOf("1260", "1261", "1350", "1401", "1402", "1450", "1460", "1360", "1360k", "1470u")) {
            val id = MachineId("pc-$model")
            fun compile(text: String) = assertIs<BasicProgramCompileResult.Success>(
                BasicProgramCodec.compileProgram(text.encodeToByteArray(), id), "$id: $text").bytes
            for (operator in listOf(">=", "<=", "<>", ">", "<", "=")) {
                val source = "10 IF A${operator}B THEN PRINT \"A${operator}B\"\n20 REM A${operator}B"
                val bytes = compile(source)
                val pattern = "A${operator}B".map(Char::code)
                assertEquals(3, bytes.map { it.toInt() and 255 }.windowed(pattern.size).count { it == pattern }, "$id: $operator")
                val saved = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, id)).utf8Bytes.decodeToString()
                assertEquals(3, Regex(Regex.escape("A${operator}B")).findAll(saved).count(), "$id: $saved")
                assertContentEquals(bytes, compile(saved), "$id: $saved")
            }
        }
    }
}
