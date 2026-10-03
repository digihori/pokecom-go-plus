package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal object GoldenScenarioRunner {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    fun decode(content: String): GoldenCase = json.decodeFromString(content)

    fun run(goldenCase: GoldenCase, romImage: ByteArray) {
        require(goldenCase.schemaVersion == "pgp-golden-1") { "Unsupported Golden schema version" }
        require(goldenCase.machineId == Pc1245RomDefinition.MACHINE_ID.value) { "Unsupported machine" }
        assertEquals(goldenCase.rom.sha256, romImage.sha256(), "ROM SHA-256")

        val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(romImage))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet),
        ).session
        var executionStatus: ExecutionStatus = ExecutionStatus.Ready

        goldenCase.actions.forEachIndexed { index, action ->
            val type = action.requiredString("type")
            when (type) {
                "reset" -> {
                    session.reset()
                    executionStatus = ExecutionStatus.Ready
                }
                "step" -> {
                    for (ignored in 0 until action.requiredInt("count")) {
                        executionStatus = session.step().status
                        if (executionStatus is ExecutionStatus.Faulted) break
                    }
                }
                "runCycles" -> {
                    executionStatus = session.runCycles(action.requiredLong("cycles")).status
                }
                "pressKey" -> session.pressKey(PocketKey.valueOf(action.requiredString("key")))
                "releaseKey" -> session.releaseKey(PocketKey.valueOf(action.requiredString("key")))
                "setOperatingMode" -> session.setOperatingMode(
                    OperatingMode.valueOf(action.requiredString("mode")),
                )
                "checkpoint" -> {
                    val name = action.requiredString("expectation")
                    val expectation = requireNotNull(goldenCase.expectations[name]) {
                        "Action $index refers to missing expectation '$name'"
                    }
                    compare(session, executionStatus, expectation, name)
                }
                else -> error("Unsupported Golden action '$type' at index $index")
            }
        }
    }

    private fun compare(
        session: EmulatorSession,
        executionStatus: ExecutionStatus,
        expected: GoldenExpectation,
        checkpoint: String,
    ) {
        expected.cpu?.let { compareCpu(session, it, checkpoint) }
        expected.memory?.forEach { range ->
            val bytes = range.bytesHex.hexBytes()
            assertContentEquals(
                bytes,
                session.memorySnapshot(range.start.hexInt(), bytes.size).copyBytes(),
                "$checkpoint memory ${range.start}",
            )
        }
        expected.display?.let { display ->
            val actual = session.displaySnapshot()
            display.enabled?.let { assertEquals(it, actual.enabled, "$checkpoint display enabled") }
            display.symbols?.let { names ->
                assertEquals(names, actual.symbols.map { it.name }.sorted(), "$checkpoint display symbols")
            }
            display.rows?.let { rows ->
                val actualRows = List(actual.dotRows) { row ->
                    buildString(actual.dotColumns) {
                        repeat(actual.dotColumns) { column ->
                            append(if (actual.isDotOn(column, row)) '1' else '0')
                        }
                    }
                }
                assertEquals(rows, actualRows, "$checkpoint display rows")
            }
        }
        expected.audio?.let {
            assertEquals(it.frequencyHz, session.audioSnapshot().frequencyHz, "$checkpoint audio")
        }
        expected.execution?.let { compareExecution(executionStatus, it, checkpoint) }
    }

    private fun compareCpu(session: EmulatorSession, expected: GoldenCpu, checkpoint: String) {
        val actual = session.cpuSnapshot()
        expected.programCounter?.let { assertEquals(it.hexInt(), actual.programCounter, "$checkpoint PC") }
        expected.currentProgramCounter?.let {
            assertEquals(it.hexInt(), actual.currentProgramCounter, "$checkpoint current PC")
        }
        expected.opcode?.let { assertEquals(it.hexInt(), actual.opcode, "$checkpoint opcode") }
        expected.dataPointer?.let { assertEquals(it.hexInt(), actual.dataPointer, "$checkpoint DP") }
        expected.p?.let { assertEquals(it.hexInt(), actual.p, "$checkpoint P") }
        expected.q?.let { assertEquals(it.hexInt(), actual.q, "$checkpoint Q") }
        expected.r?.let { assertEquals(it.hexInt(), actual.r, "$checkpoint R") }
        expected.d?.let { assertEquals(it.hexInt(), actual.d, "$checkpoint D") }
        expected.alu?.let { assertEquals(it.hexInt(), actual.alu, "$checkpoint ALU") }
        expected.carry?.let { assertEquals(it, actual.carry, "$checkpoint carry") }
        expected.zero?.let { assertEquals(it, actual.zero, "$checkpoint zero") }
        expected.xInput?.let { assertEquals(it.hexInt(), actual.xInput, "$checkpoint Xin") }
        expected.powerOn?.let { assertEquals(it, actual.powerOn, "$checkpoint power") }
        expected.ia?.let { assertEquals(it.hexInt(), actual.ia, "$checkpoint IA") }
        expected.ib?.let { assertEquals(it.hexInt(), actual.ib, "$checkpoint IB") }
        expected.fo?.let { assertEquals(it.hexInt(), actual.fo, "$checkpoint FO") }
        expected.control?.let { assertEquals(it.hexInt(), actual.control, "$checkpoint control") }
        expected.testPort?.let { assertEquals(it.hexInt(), actual.testPort, "$checkpoint TEST") }
        expected.internalRamHex?.let {
            assertContentEquals(it.hexBytes(), actual.copyInternalRam(), "$checkpoint internal RAM")
        }
    }

    private fun compareExecution(
        status: ExecutionStatus,
        expected: GoldenExecution,
        checkpoint: String,
    ) {
        when (expected.status) {
            "READY" -> assertIs<ExecutionStatus.Ready>(status, "$checkpoint execution")
            "UNSUPPORTED_OPCODE" -> {
                val fault = assertIs<CoreFault.UnsupportedOpcode>(
                    assertIs<ExecutionStatus.Faulted>(status).fault,
                )
                assertEquals(expected.address?.hexInt(), fault.address, "$checkpoint fault address")
                assertEquals(expected.opcode?.hexInt(), fault.opcode, "$checkpoint fault opcode")
            }
            else -> error("Unsupported execution expectation '${expected.status}'")
        }
    }

    private fun JsonObject.requiredString(name: String): String =
        requireNotNull(this[name]?.jsonPrimitive?.contentOrNull) { "Missing '$name'" }

    private fun JsonObject.requiredInt(name: String): Int =
        requireNotNull(this[name]) { "Missing '$name'" }.jsonPrimitive.int

    private fun JsonObject.requiredLong(name: String): Long =
        requireNotNull(this[name]) { "Missing '$name'" }.jsonPrimitive.long

    private fun String.hexInt(): Int = toInt(16)

    private fun String.hexBytes(): ByteArray {
        require(length % 2 == 0) { "Hex byte string must have an even length" }
        return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }
}

@Serializable
internal data class GoldenCase(
    val schemaVersion: String,
    val caseId: String,
    val description: String? = null,
    val machineId: String,
    val producer: GoldenProducer,
    val rom: GoldenRom,
    val actions: List<JsonObject>,
    val expectations: Map<String, GoldenExpectation>,
)

@Serializable internal data class GoldenProducer(val implementation: String, val revision: String)
@Serializable internal data class GoldenRom(val format: String, val sha256: String)

@Serializable
internal data class GoldenExpectation(
    val cpu: GoldenCpu? = null,
    val memory: List<GoldenMemoryRange>? = null,
    val display: GoldenDisplay? = null,
    val audio: GoldenAudio? = null,
    val execution: GoldenExecution? = null,
)

@Serializable
internal data class GoldenCpu(
    val programCounter: String? = null,
    val currentProgramCounter: String? = null,
    val opcode: String? = null,
    val dataPointer: String? = null,
    val p: String? = null,
    val q: String? = null,
    val r: String? = null,
    val d: String? = null,
    val alu: String? = null,
    val carry: Boolean? = null,
    val zero: Boolean? = null,
    val xInput: String? = null,
    val powerOn: Boolean? = null,
    val ia: String? = null,
    val ib: String? = null,
    val fo: String? = null,
    val control: String? = null,
    val testPort: String? = null,
    val internalRamHex: String? = null,
)

@Serializable internal data class GoldenMemoryRange(val start: String, val bytesHex: String)
@Serializable internal data class GoldenDisplay(val enabled: Boolean? = null, val rows: List<String>? = null, val symbols: List<String>? = null)
@Serializable internal data class GoldenAudio(val frequencyHz: Int)
@Serializable internal data class GoldenExecution(val status: String, val address: String? = null, val opcode: String? = null)
