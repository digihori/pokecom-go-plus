package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.debug.Sc61860InstructionFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object DesktopDebuggerCheckpointWriter {
    private val json = Json { prettyPrint = true }

    fun write(
        machineId: String,
        capturedAt: String,
        executedCycles: Long,
        cpu: CpuSnapshot,
        memory: MemorySnapshot,
        trace: List<DesktopInstructionTraceEntry>,
        stopReason: String?,
    ): ByteArray {
        val root = buildJsonObject {
            put("format", "pgp-debug-checkpoint")
            put("formatVersion", 1)
            put("machineId", machineId)
            put("capturedAt", capturedAt)
            put("executedCycles", executedCycles)
            stopReason?.let { put("stopReason", it) }
            put("cpu", buildJsonObject {
                put("pc", cpu.programCounter.hex(4))
                put("currentPc", cpu.currentProgramCounter.hex(4))
                put("opcode", cpu.opcode.hex(2))
                put("dp", cpu.dataPointer.hex(4))
                put("p", cpu.p.hex(2)); put("q", cpu.q.hex(2)); put("r", cpu.r.hex(2)); put("d", cpu.d.hex(2))
                put("alu", cpu.alu.hex(4)); put("carry", cpu.carry); put("zero", cpu.zero)
                put("ia", cpu.ia.hex(2)); put("ib", cpu.ib.hex(2)); put("fo", cpu.fo.hex(2))
                put("control", cpu.control.hex(2)); put("test", cpu.testPort.hex(2))
                put("internalRam", cpu.copyInternalRam().hex())
            })
            put("memory", buildJsonObject {
                put("startAddress", memory.startAddress.hex(4))
                put("endAddress", (memory.startAddress + memory.size - 1).hex(4))
                put("bytes", memory.copyBytes().hex())
            })
            put("trace", buildJsonArray {
                trace.forEach { entry ->
                    add(buildJsonObject {
                        put("sequence", entry.sequence)
                        put("address", entry.instruction.address.hex(4))
                        put("bytes", entry.instruction.bytes.joinToString("") { it.hex(2) })
                        put("instruction", Sc61860InstructionFormatter.format(entry.instruction))
                        put("dp", entry.dataPointer.hex(4)); put("p", entry.p.hex(2))
                        put("carry", entry.carry); put("zero", entry.zero)
                    })
                }
            })
        }
        return (json.encodeToString(root) + "\n").encodeToByteArray()
    }

    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).hex(2) }
    private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
        .toString(16).uppercase().padStart(width, '0')
}
