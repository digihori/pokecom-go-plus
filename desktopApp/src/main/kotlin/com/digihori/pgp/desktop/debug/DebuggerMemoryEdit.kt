package com.digihori.pgp.desktop.debug

internal enum class DebuggerMemoryEditError { PAUSED_ONLY, ADDRESS, VALUE, RAM_ONLY, STALE, UNCHANGED, READ, WRITE }

internal data class DebuggerMemoryEditPreview(
    val address: Int, val physicalAddress: Int, val before: Int, val after: Int, val revision: Long, val sessionId: String,
)

internal sealed interface DebuggerMemoryEditResult {
    data class Success(val edit: DebuggerMemoryEditPreview) : DebuggerMemoryEditResult
    data class Failure(val error: DebuggerMemoryEditError) : DebuggerMemoryEditResult
}
