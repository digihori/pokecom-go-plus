package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineMemoryBanking
import com.digihori.pgp.core.api.MemoryAccessKind
import com.digihori.pgp.desktop.parseHexAddress
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

internal data class DebuggerHistorySnapshot(
    val state: RunnerState? = null,
    val observationEnabled: Boolean = false,
    val accesses: List<DesktopMemoryAccessHistoryEntry> = emptyList(),
    val banks: List<DesktopBankHistoryEntry> = emptyList(),
    val banking: MachineMemoryBanking.Banked? = null,
    val selectedBank: Int? = null,
)

/** Copies the Runner's existing observations. Does not drain Core events or mutate the session. */
internal fun captureDebuggerHistory(runner: DesktopEmulatorRunner?): DebuggerHistorySnapshot =
    if (runner == null) DebuggerHistorySnapshot() else DebuggerHistorySnapshot(
        runner.state, runner.isDebugObservationEnabled(), runner.memoryAccessHistory(), runner.bankHistory(),
        MachineCatalog.find(runner.machineId)?.memoryBanking as? MachineMemoryBanking.Banked, runner.selectedRomBank(),
    )

internal enum class DebuggerAccessFilter { ALL, READ, WRITE }
internal enum class DebuggerHistoryFilterError { START, END, RANGE, BANK }
internal sealed interface DebuggerHistoryRange {
    data class Valid(val range: IntRange) : DebuggerHistoryRange
    data class Invalid(val error: DebuggerHistoryFilterError) : DebuggerHistoryRange
}

internal fun historyRange(start: String, end: String): DebuggerHistoryRange {
    val first = if (start.isBlank()) 0 else parseHexAddress(start) ?: return DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.START)
    val last = if (end.isBlank()) 0xffff else parseHexAddress(end) ?: return DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.END)
    return if (last < first) DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.RANGE) else DebuggerHistoryRange.Valid(first..last)
}

internal object DebuggerHistoryModel {
    fun accesses(entries: List<DesktopMemoryAccessHistoryEntry>, kind: DebuggerAccessFilter, range: IntRange): List<DesktopMemoryAccessHistoryEntry> =
        entries.asReversed().filter { it.access.address in range && when (kind) {
            DebuggerAccessFilter.ALL -> true
            DebuggerAccessFilter.READ -> it.access.kind == MemoryAccessKind.READ
            DebuggerAccessFilter.WRITE -> it.access.kind == MemoryAccessKind.WRITE
        } }

    fun banks(entries: List<DesktopBankHistoryEntry>, bank: Int?): List<DesktopBankHistoryEntry> =
        entries.asReversed().filter { bank == null || it.event.previousBank == bank || it.event.selectedBank == bank }
}

internal fun DesktopMemoryAccessHistoryEntry.historyText(): String =
    "#$sequence PC=${instructionAddress.historyHex(4)} ${if (access.kind == MemoryAccessKind.READ) "R" else "W"}:${access.address.historyHex(4)}=${access.value.historyHex(2)}"

internal fun DesktopBankHistoryEntry.historyText(): String =
    "#$sequence bank-${event.previousBank} → bank-${event.selectedBank}"

internal fun Int.historyHex(width: Int): String = toString(16).uppercase().padStart(width, '0')
