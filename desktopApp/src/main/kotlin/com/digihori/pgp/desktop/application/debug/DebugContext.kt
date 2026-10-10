package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.core.api.MemoryAccessKind

internal data class DebugContextRequest(
    val memoryRanges: List<DebugMemoryRangeRequest> = emptyList(),
    val disassemblyStartAddress: Int? = null,
    val disassemblyInstructionCount: Int = 0,
    val memoryAccessHistoryLimit: Int = 256,
    val bankHistoryLimit: Int = 256,
    val traceLimit: Int = 256,
)

internal data class DebugContextCapabilities(
    val format: String,
    val schemaVersion: Int,
    val machineId: String,
    val maxMemoryRanges: Int,
    val maxMemoryBytes: Int,
    val maxDisassemblyInstructions: Int,
    val maxHistoryEntries: Int,
    val memoryAccessHistorySupported: Boolean,
    val bankHistorySupported: Boolean,
    val romBytesAllowed: Boolean,
)

internal data class DebugMemoryRangeRequest(val startAddress: Int, val length: Int)

internal sealed interface DebugContextResult {
    data class Success(val context: DebugContextV1) : DebugContextResult
    data class Failure(val error: DebugContextError) : DebugContextResult
}

internal sealed interface DebugContextError {
    data object SessionRunning : DebugContextError
    data class InvalidRequest(val message: String) : DebugContextError
    data class RomBytesNotAllowed(val startAddress: Int, val length: Int) : DebugContextError
}

internal data class DebugContextV1(
    val capturedAt: String,
    val session: DebugSessionContext,
    val stop: DebugStopContext?,
    val cpu: DebugCpuContext,
    val rom: DebugRomContext,
    val memory: List<DebugMemoryContext>,
    val disassembly: List<DebugDisassemblyContext>,
    val memoryAccessHistory: DebugMemoryAccessHistoryContext,
    val bankHistory: DebugBankHistoryContext,
    val trace: DebugTraceContext,
    val warnings: List<String> = emptyList(),
) {
    companion object {
        const val FORMAT: String = "pgp-debug-context"
        const val SCHEMA_VERSION: Int = 1
    }
}

internal data class DebugSessionContext(
    val id: String,
    val revision: Long,
    val machineId: String,
    val runState: String,
    val executedCycles: Long?,
)

internal sealed interface DebugStopContext {
    val kind: String

    data object Reset : DebugStopContext { override val kind = "reset" }
    data object UserPause : DebugStopContext { override val kind = "userPause" }
    data object StepComplete : DebugStopContext { override val kind = "stepComplete" }
    data class Breakpoint(val address: Int) : DebugStopContext { override val kind = "breakpoint" }
    data class RunToAddress(val address: Int) : DebugStopContext { override val kind = "runToAddress" }
    data class MemoryChanged(
        val instructionAddress: Int,
        val changes: List<DebugMemoryChangeContext>,
    ) : DebugStopContext { override val kind = "memoryChanged" }
    data class MemoryAccessed(
        val instructionAddress: Int,
        val accesses: List<DebugMemoryAccessContext>,
    ) : DebugStopContext { override val kind = "memoryAccessed" }
    data class Fault(val faultKind: String, val address: Int?, val opcode: Int?) : DebugStopContext {
        override val kind = "fault"
    }
}

internal data class DebugMemoryChangeContext(val address: Int, val before: Int, val after: Int)

internal data class DebugCpuContext(
    val programCounter: Int,
    val currentProgramCounter: Int,
    val opcode: Int,
    val dataPointer: Int,
    val p: Int,
    val q: Int,
    val r: Int,
    val d: Int,
    val alu: Int,
    val carry: Boolean,
    val zero: Boolean,
    val xInput: Int,
    val powerOn: Boolean,
    val ia: Int,
    val ib: Int,
    val fo: Int,
    val control: Int,
    val testPort: Int,
    val internalRamHex: String,
    val internalRamByteLength: Int,
)

internal data class DebugRomLocationContext(val component: String, val bank: Int?, val offset: Int)
internal data class DebugRomContext(val selectedBank: Int?, val currentLocation: DebugRomLocationContext?)

internal data class DebugMemoryContext(
    val startAddress: Int,
    val byteLength: Int,
    val regionId: String?,
    val encoding: String = "hex",
    val data: String,
)

internal data class DebugDisassemblyContext(
    val address: Int,
    val bytes: List<Int>,
    val mnemonic: String?,
    val operandValue: Int?,
    val targetAddress: Int?,
    val text: String,
    val romLocation: DebugRomLocationContext?,
)

internal data class DebugMemoryAccessContext(
    val sequence: Long?,
    val instructionAddress: Int?,
    val kind: MemoryAccessKind,
    val address: Int,
    val value: Int,
)

internal data class DebugMemoryAccessHistoryContext(
    val enabled: Boolean,
    val truncated: Boolean,
    val entries: List<DebugMemoryAccessContext>,
)

internal data class DebugBankHistoryEntryContext(
    val sequence: Long,
    val previousBank: Int,
    val selectedBank: Int,
    val selectorValue: Int,
)

internal data class DebugBankHistoryContext(
    val supported: Boolean,
    val truncated: Boolean,
    val entries: List<DebugBankHistoryEntryContext>,
)

internal data class DebugTraceEntryContext(
    val sequence: Long,
    val address: Int,
    val bytes: List<Int>,
    val text: String,
    val romLocation: DebugRomLocationContext?,
    val dataPointer: Int,
    val p: Int,
    val q: Int,
    val r: Int,
    val d: Int,
    val carry: Boolean,
    val zero: Boolean,
)

internal data class DebugTraceContext(
    val enabled: Boolean,
    val capacity: Int,
    val truncated: Boolean,
    val entries: List<DebugTraceEntryContext>,
)
