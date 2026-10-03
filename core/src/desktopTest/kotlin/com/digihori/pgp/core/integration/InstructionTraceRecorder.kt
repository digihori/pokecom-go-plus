package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus

internal data class InstructionTraceEntry(
    val instruction: Long,
    val cycleBefore: Long,
    val cycles: Int,
    val pc: Int,
    val opcode: Int,
    val programCounterAfter: Int,
    val q: Int,
    val ib: Int,
    val testPort: Int,
)

internal data class InstructionTrace(
    val executedInstructions: Long,
    val executedCycles: Long,
    val status: ExecutionStatus,
    val tail: List<InstructionTraceEntry>,
) {
    fun toTsv(): String = buildString {
        appendLine("instruction\tcycleBefore\tcycles\tpc\topcode\tpcAfter\tq\tib\ttestPort")
        tail.forEach { entry ->
            append(entry.instruction).append('\t')
            append(entry.cycleBefore).append('\t')
            append(entry.cycles).append('\t')
            append(entry.pc.hex(4)).append('\t')
            append(entry.opcode.hex(2)).append('\t')
            append(entry.programCounterAfter.hex(4)).append('\t')
            append(entry.q.hex(2)).append('\t')
            append(entry.ib.hex(2)).append('\t')
            append(entry.testPort.hex(2)).appendLine()
        }
    }
}

internal object InstructionTraceRecorder {
    fun recordUntil(
        session: EmulatorSession,
        cycleBudget: Long,
        tailCapacity: Int = 256,
    ): InstructionTrace {
        require(cycleBudget > 0) { "cycleBudget must be positive" }
        require(tailCapacity > 0) { "tailCapacity must be positive" }
        val tail = ArrayDeque<InstructionTraceEntry>(tailCapacity)
        var instructions = 0L
        var cycles = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready

        while (cycles < cycleBudget && status !is ExecutionStatus.Faulted) {
            val before = session.cpuSnapshot()
            val result = session.step()
            val after = session.cpuSnapshot()
            instructions++
            if (tail.size == tailCapacity) tail.removeFirst()
            tail.addLast(
                InstructionTraceEntry(
                    instruction = instructions,
                    cycleBefore = cycles,
                    cycles = result.cycles,
                    pc = after.currentProgramCounter,
                    opcode = after.opcode,
                    programCounterAfter = after.programCounter,
                    q = after.q,
                    ib = after.ib,
                    testPort = after.testPort,
                ),
            )
            check(after.currentProgramCounter == before.programCounter) {
                "Session trace boundary does not identify the instruction that was stepped"
            }
            cycles += result.cycles
            status = result.status
        }

        return InstructionTrace(instructions, cycles, status, tail.toList())
    }
}

private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
    .toString(16).padStart(width, '0')
