package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.assertIs

internal object GoldenSnapshotExporter {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        explicitNulls = false
        encodeDefaults = false
    }

    fun exportPc1245Boot(
        romImage: ByteArray,
        producerRevision: String,
        cycleBudget: Long = BOOT_CYCLE_BUDGET,
    ): String {
        require(producerRevision.isNotBlank()) { "Producer revision must not be blank" }
        val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(romImage))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        val afterReset = capture(session, ExecutionStatus.Ready)
        val runResult = session.runCycles(cycleBudget)
        val afterBoot = capture(session, runResult.status)

        val goldenCase = GoldenCase(
            schemaVersion = "pgp-golden-1",
            caseId = "pc1245-pgp-boot-$cycleBudget",
            description = "PGP PC-1245 cold reset and $cycleBudget-cycle headless boot",
            machineId = Pc1245RomDefinition.MACHINE_ID.value,
            producer = GoldenProducer("pgp", producerRevision),
            rom = GoldenRom("pokecom-go-flat-64k", romImage.sha256()),
            actions = listOf(
                buildJsonObject { put("type", "reset") },
                buildJsonObject {
                    put("type", "checkpoint")
                    put("expectation", "after-reset")
                },
                buildJsonObject {
                    put("type", "runCycles")
                    put("cycles", cycleBudget)
                },
                buildJsonObject {
                    put("type", "checkpoint")
                    put("expectation", "after-boot")
                },
            ),
            expectations = linkedMapOf(
                "after-reset" to afterReset,
                "after-boot" to afterBoot,
            ),
        )
        return json.encodeToString(goldenCase) + "\n"
    }

    private fun capture(session: EmulatorSession, status: ExecutionStatus): GoldenExpectation {
        val cpu = session.cpuSnapshot()
        val display = session.displaySnapshot()
        return GoldenExpectation(
            cpu = GoldenCpu(
                programCounter = cpu.programCounter.hex(4),
                currentProgramCounter = cpu.currentProgramCounter.hex(4),
                opcode = cpu.opcode.hex(2),
                dataPointer = cpu.dataPointer.hex(4),
                p = cpu.p.hex(2),
                q = cpu.q.hex(2),
                r = cpu.r.hex(2),
                d = cpu.d.hex(2),
                alu = cpu.alu.hex(4),
                carry = cpu.carry,
                zero = cpu.zero,
                xInput = cpu.xInput.hex(2),
                powerOn = cpu.powerOn,
                ia = cpu.ia.hex(2),
                ib = cpu.ib.hex(2),
                fo = cpu.fo.hex(2),
                control = cpu.control.hex(2),
                testPort = cpu.testPort.hex(2),
                internalRamHex = cpu.copyInternalRam().hex(),
            ),
            memory = listOf(
                GoldenMemoryRange("8000", session.memorySnapshot(0x8000, 0x0800).copyBytes().hex()),
                GoldenMemoryRange("f800", session.memorySnapshot(0xf800, 0x0100).copyBytes().hex()),
            ),
            display = GoldenDisplay(
                enabled = display.enabled,
                rows = List(display.dotRows) { row ->
                    buildString(display.dotColumns) {
                        repeat(display.dotColumns) { column ->
                            append(if (display.isDotOn(column, row)) '1' else '0')
                        }
                    }
                },
                symbols = display.symbols.map { it.name }.sorted(),
            ),
            audio = GoldenAudio(session.audioSnapshot().frequencyHz),
            execution = status.toGoldenExecution(),
        )
    }

    private fun ExecutionStatus.toGoldenExecution(): GoldenExecution = when (this) {
        ExecutionStatus.Ready -> GoldenExecution("READY")
        is ExecutionStatus.Faulted -> when (val coreFault = fault) {
            is CoreFault.UnsupportedOpcode -> GoldenExecution(
                status = "UNSUPPORTED_OPCODE",
                address = coreFault.address.hex(4),
                opcode = coreFault.opcode.hex(2),
            )
        }
    }

    private fun Int.hex(width: Int): String = toString(16).padStart(width, '0')
    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).hex(2) }
    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(this).hex()

    private const val BOOT_CYCLE_BUDGET: Long = 1_000_000
}
