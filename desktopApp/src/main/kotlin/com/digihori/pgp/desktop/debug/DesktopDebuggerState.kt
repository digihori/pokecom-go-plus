package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.BankSwitchEvent
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.CpuSnapshot

internal sealed interface DebuggerStopReason {
    data object Reset : DebuggerStopReason
    data object UserPause : DebuggerStopReason
    data object StepComplete : DebuggerStopReason
    data class Breakpoint(val address: Int) : DebuggerStopReason
    data class RunToAddress(val address: Int) : DebuggerStopReason
    data class MemoryChanged(
        val instructionAddress: Int,
        val changes: List<MemoryValueChange>,
    ) : DebuggerStopReason
    data class MemoryAccessed(
        val instructionAddress: Int,
        val accesses: List<MemoryAccess>,
    ) : DebuggerStopReason
    data class Fault(val fault: CoreFault) : DebuggerStopReason
}

internal data class MemoryValueChange(
    val address: Int,
    val before: Int,
    val after: Int,
)

internal data class DesktopMemoryAccessHistoryEntry(
    val sequence: Long,
    val instructionAddress: Int,
    val access: MemoryAccess,
)

internal data class DesktopBankHistoryEntry(
    val sequence: Long,
    val event: BankSwitchEvent,
)

internal enum class CpuField {
    PC,
    CURRENT_PC,
    OPCODE,
    DP,
    P,
    Q,
    R,
    D,
    ALU,
    CARRY,
    ZERO,
    IA,
    IB,
    FO,
    CONTROL,
    TEST,
    INTERNAL_RAM,
}

internal object CpuSnapshotDifference {
    fun changed(previous: CpuSnapshot?, current: CpuSnapshot?): Set<CpuField> {
        if (previous == null || current == null) return emptySet()
        return buildSet {
            if (previous.programCounter != current.programCounter) add(CpuField.PC)
            if (previous.currentProgramCounter != current.currentProgramCounter) add(CpuField.CURRENT_PC)
            if (previous.opcode != current.opcode) add(CpuField.OPCODE)
            if (previous.dataPointer != current.dataPointer) add(CpuField.DP)
            if (previous.p != current.p) add(CpuField.P)
            if (previous.q != current.q) add(CpuField.Q)
            if (previous.r != current.r) add(CpuField.R)
            if (previous.d != current.d) add(CpuField.D)
            if (previous.alu != current.alu) add(CpuField.ALU)
            if (previous.carry != current.carry) add(CpuField.CARRY)
            if (previous.zero != current.zero) add(CpuField.ZERO)
            if (previous.ia != current.ia) add(CpuField.IA)
            if (previous.ib != current.ib) add(CpuField.IB)
            if (previous.fo != current.fo) add(CpuField.FO)
            if (previous.control != current.control) add(CpuField.CONTROL)
            if (previous.testPort != current.testPort) add(CpuField.TEST)
            if (!previous.copyInternalRam().contentEquals(current.copyInternalRam())) add(CpuField.INTERNAL_RAM)
        }
    }
}
