package com.digihori.pgp.core.integration

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PokecomGoStateImporterTest {
    @Test
    fun convertsExistingPc1245SavedStateToGoldenExpectation() {
        val content = savedState(id = 1245)

        val expectation = PokecomGoStateImporter.importPc1245Expectation(content)

        assertEquals("1234", expectation.cpu?.programCounter)
        assertEquals("5a", expectation.cpu?.opcode)
        assertEquals("60", expectation.cpu?.r)
        assertEquals(true, expectation.cpu?.carry)
        assertEquals(false, expectation.cpu?.zero)
        assertEquals("aa", expectation.cpu?.internalRamHex?.substring(4, 6))
        assertEquals("7f", expectation.memory?.first()?.bytesHex?.substring(0, 2))
        assertEquals(true, expectation.display?.enabled)
        assertEquals(listOf("BUSY", "DEF", "PRO"), expectation.display?.symbols)
        assertEquals('1', expectation.display?.rows?.first()?.first())
    }

    @Test
    fun rejectsStateForAnotherMachine() {
        assertFailsWith<IllegalArgumentException> {
            PokecomGoStateImporter.importPc1245Expectation(savedState(id = 1251))
        }
    }

    private fun savedState(id: Int): String = buildJsonObject {
        put("id", id)
        put("prog_mode_1245", true)
        put("pc", 0x1234)
        put("current_pc", 0x1233)
        put("opcode", 0x5a)
        put("dp", 0xabcd)
        put("preg", 0x20)
        put("qreg", 0x21)
        put("rreg", 0x60)
        put("dreg", 0x22)
        put("alu", 0x0100)
        put("cflag", 1)
        put("zflag", 0)
        put("xin", 0)
        put("power_on", 1)
        put("disp_on", 1)
        put("iaval", 1)
        put("ibval", 2)
        put("foval", 3)
        put("ctrlval", 4)
        put("testport", 5)
        put("iram", buildJsonArray { repeat(256) { add(JsonPrimitive(if (it == 2) 0xaa else 0)) } })
        put("mainram", buildJsonArray {
            repeat(0x10000) { add(JsonPrimitive(if (it == 0x8000) 0x7f else 0)) }
        })
        put("digi", buildJsonArray { repeat(600) { add(JsonPrimitive(if (it == 0) 1 else 0)) } })
        put("state", buildJsonArray {
            add(JsonPrimitive(1)); add(JsonPrimitive(1)); add(JsonPrimitive(0)); add(JsonPrimitive(0))
        })
    }.toString()
}
