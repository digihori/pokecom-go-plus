package com.digihori.pgp.core.integration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Converts Pokecom GO's existing Gson `Sc61860params` state into a Golden expectation. */
internal object PokecomGoStateImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun importPc1245Expectation(content: String): GoldenExpectation {
        val root = json.parseToJsonElement(content).jsonObject
        require(root.int("id") == 1245) { "Pokecom GO state is not for PC-1245" }

        val internalRam = root.array("iram", minimumSize = 256)
        val mainRam = root.array("mainram", minimumSize = 0x10000)
        val digits = root.array("digi", minimumSize = 80)
        val displayState = root.array("state", minimumSize = 2)
        val programMode = root.boolean("prog_mode_1245")

        return GoldenExpectation(
            cpu = GoldenCpu(
                programCounter = root.int("pc").hex(4),
                currentProgramCounter = root.int("current_pc").hex(4),
                opcode = root.int("opcode").hex(2),
                dataPointer = root.int("dp").hex(4),
                p = root.int("preg").hex(2),
                q = root.int("qreg").hex(2),
                r = root.int("rreg").hex(2),
                d = root.int("dreg").hex(2),
                alu = root.int("alu").hex(4),
                carry = root.int("cflag") != 0,
                zero = root.int("zflag") != 0,
                xInput = root.int("xin").hex(2),
                powerOn = root.int("power_on") != 0,
                ia = root.int("iaval").hex(2),
                ib = root.int("ibval").hex(2),
                fo = root.int("foval").hex(2),
                control = root.int("ctrlval").hex(2),
                testPort = root.int("testport").hex(2),
                internalRamHex = internalRam.hexRange(0, 256),
            ),
            memory = listOf(
                GoldenMemoryRange("8000", mainRam.hexRange(0x8000, 0x0800)),
                GoldenMemoryRange("f800", mainRam.hexRange(0xf800, 0x0100)),
            ),
            display = GoldenDisplay(
                enabled = root.int("disp_on") != 0,
                rows = List(7) { row ->
                    buildString(80) {
                        repeat(80) { column ->
                            append(if (digits.byte(column) and (1 shl row) != 0) '1' else '0')
                        }
                    }
                },
                symbols = displaySymbols(
                    state0 = displayState.byte(0),
                    state1 = displayState.byte(1),
                    programMode = programMode,
                ),
            ),
        )
    }

    private fun displaySymbols(state0: Int, state1: Int, programMode: Boolean): List<String> =
        buildList {
            if (state1 and 0x01 != 0) add("BUSY")
            if (state0 and 0x02 != 0) add("P")
            if (state0 and 0x01 != 0) add("DEF")
            if (state0 and 0x08 != 0) add("DE")
            if (state0 and 0x04 != 0) add("G")
            if (state1 and 0x04 != 0) add("RAD")
            if (state1 and 0x02 != 0) add("SHIFT")
            add(if (programMode) "PRO" else "RUN")
        }.sorted()

    private fun JsonObject.int(name: String): Int =
        requireNotNull(this[name]) { "Missing Pokecom GO field '$name'" }.jsonPrimitive.int

    private fun JsonObject.boolean(name: String): Boolean =
        requireNotNull(this[name]) { "Missing Pokecom GO field '$name'" }.jsonPrimitive.boolean

    private fun JsonObject.array(name: String, minimumSize: Int): JsonArray {
        val result = requireNotNull(this[name]) { "Missing Pokecom GO field '$name'" }.jsonArray
        require(result.size >= minimumSize) { "Pokecom GO field '$name' is too short" }
        return result
    }

    private fun JsonArray.byte(index: Int): Int = this[index].jsonPrimitive.int and 0xff

    private fun JsonArray.hexRange(start: Int, length: Int): String =
        buildString(length * 2) {
            repeat(length) { offset -> append(byte(start + offset).hex(2)) }
        }

    private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
        .toString(16)
        .padStart(width, '0')
}
