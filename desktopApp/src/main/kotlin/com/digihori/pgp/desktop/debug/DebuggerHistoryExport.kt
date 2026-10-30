package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.core.debug.Sc61860InstructionFormatter
import com.digihori.pgp.desktop.checkpointDescription
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.*

internal data class DebuggerExportLimits(val trace: Int = 256, val accesses: Int = 256, val banks: Int = 256, val logs: Int = 256) {
    fun valid() = trace in 0..4096 && accesses in 0..4096 && banks in 0..4096 && logs in 0..256
}
internal enum class DebuggerExportError { LIMIT, STATE, CHANGED, FORMAT, SIZE, IO }
internal class DebuggerExportException(val code: DebuggerExportError) : Exception(code.name)

/** Existing observations only: never reads arbitrary memory, drains events, pauses or steps. */
internal object DebuggerHistoryExport {
    const val MAX_BYTES = 8 * 1024 * 1024
    private fun fail(code: DebuggerExportError): Nothing = throw DebuggerExportException(code)
    private fun hex(value: Int, width: Int = 4) = value.toString(16).uppercase().padStart(width, '0')
    private fun JsonObjectBuilder.location(location: PhysicalRomLocation?) {
        location?.let {
            put("romComponent", it.componentId.value)
            it.bank?.let { bank -> put("romBank", bank) }
            put("romOffset", hex(it.offset))
        }
    }
    private fun stop(reason: DebuggerStopReason) = buildJsonObject {
        put("code", when (reason) {
            DebuggerStopReason.Reset -> "RESET"
            DebuggerStopReason.UserPause -> "USER_PAUSE"
            DebuggerStopReason.StepComplete -> "STEP_COMPLETE"
            is DebuggerStopReason.Breakpoint -> "BREAKPOINT"
            is DebuggerStopReason.RunToAddress -> "RUN_TO_ADDRESS"
            is DebuggerStopReason.MemoryChanged -> "MEMORY_CHANGED"
            is DebuggerStopReason.MemoryAccessed -> "MEMORY_ACCESSED"
            is DebuggerStopReason.Fault -> "FAULT"
        })
        val address = when (reason) {
            is DebuggerStopReason.Breakpoint -> reason.address
            is DebuggerStopReason.RunToAddress -> reason.address
            is DebuggerStopReason.MemoryChanged -> reason.instructionAddress
            is DebuggerStopReason.MemoryAccessed -> reason.instructionAddress
            else -> null
        }
        address?.let { put("instructionAddress", hex(it)) }
        put("description", reason.checkpointDescription())
    }
    private fun <T> section(entries: List<T>, limit: Int, encode: (T) -> JsonObject) = buildJsonObject {
        put("retainedBeforeExport", entries.size)
        put("omittedByLimit", (entries.size - limit).coerceAtLeast(0))
        put("entries", buildJsonArray { entries.takeLast(limit).forEach { add(encode(it)) } })
    }

    fun capture(runner: DesktopEmulatorRunner, limits: DebuggerExportLimits, capturedAt: String): ByteArray {
        if (!limits.valid()) fail(DebuggerExportError.LIMIT)
        if (runner.state == RunnerState.RUNNING) fail(DebuggerExportError.STATE)
        val revision = runner.sessionRevision
        val root = buildJsonObject {
            put("format", "pgp-debug-history"); put("formatVersion", 1)
            put("machineId", runner.machineId.value); put("capturedAt", capturedAt)
            put("sessionRevision", revision); put("targetGeneration", runner.debugTargetGeneration)
            put("state", runner.state.name); put("pc", hex(runner.cpuSnapshot().programCounter))
            runner.stopReason?.let { put("stop", stop(it)) }
            runner.selectedRomBank()?.let { put("selectedRomBank", it) }
            put("traceEnabled", runner.isInstructionTraceEnabled())
            put("accessObservationEnabled", runner.isDebugObservationEnabled())
            put("bankEventsLinkedToInstructions", false)
            put("trace", section(runner.instructionTrace(), limits.trace) { entry -> buildJsonObject {
                put("sequence", entry.sequence); put("address", hex(entry.instruction.address))
                put("bytes", entry.instruction.bytes.joinToString("") { hex(it, 2) })
                put("instruction", Sc61860InstructionFormatter.format(entry.instruction))
                location(entry.romLocation)
                put("dp", hex(entry.dataPointer)); put("p", hex(entry.p, 2)); put("q", hex(entry.q, 2))
                put("r", hex(entry.r, 2)); put("d", hex(entry.d, 2)); put("carry", entry.carry); put("zero", entry.zero)
            } })
            put("accesses", section(runner.memoryAccessHistory(), limits.accesses) { entry -> buildJsonObject {
                put("sequence", entry.sequence); put("instructionAddress", hex(entry.instructionAddress))
                put("kind", entry.access.kind.name); put("address", hex(entry.access.address)); put("value", hex(entry.access.value, 2))
            } })
            put("banks", section(runner.bankHistory(), limits.banks) { entry -> buildJsonObject {
                put("sequence", entry.sequence); put("previousBank", entry.event.previousBank)
                put("selectedBank", entry.event.selectedBank); put("selectorValue", hex(entry.event.selectorValue, 2))
            } })
            put("logs", section(runner.logpointEntries(), limits.logs) { entry -> buildJsonObject {
                put("sequence", entry.sequence); put("logpointId", entry.id); put("address", hex(entry.address))
                put("operand", entry.operand); put("value", entry.value?.let { JsonPrimitive(hex(it)) } ?: JsonNull)
                put("readFailed", entry.failed); location(entry.rom)
            } })
            put("logEntriesDiscarded", runner.logpointDropped())
        }
        if (runner.sessionRevision != revision || runner.state == RunnerState.RUNNING) fail(DebuggerExportError.CHANGED)
        val bytes = (Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), root) + "\n").encodeToByteArray()
        if (bytes.size > MAX_BYTES) fail(DebuggerExportError.SIZE)
        return bytes
    }

    /** Freeze before the native chooser, then reject a replaced or resumed session before writing. */
    fun save(current: () -> DesktopEmulatorRunner?, limits: DebuggerExportLimits, capturedAt: String, choose: () -> File?): Boolean {
        val runner = current() ?: fail(DebuggerExportError.STATE)
        val revision = runner.sessionRevision
        val bytes = capture(runner, limits, capturedAt)
        val file = choose() ?: return false
        if (current() !== runner || runner.sessionRevision != revision || runner.state == RunnerState.RUNNING) fail(DebuggerExportError.CHANGED)
        write(file, bytes)
        return true
    }

    fun write(file: File, bytes: ByteArray, move: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }) {
        if (!file.name.endsWith(".pgtrace.json", ignoreCase = true)) fail(DebuggerExportError.FORMAT)
        if (bytes.size > MAX_BYTES) fail(DebuggerExportError.SIZE)
        try {
            val target = file.absoluteFile.toPath()
            val temporary = Files.createTempFile(target.parent, ".pgp-trace-", ".tmp")
            try { Files.write(temporary, bytes); move(temporary, target) }
            finally { Files.deleteIfExists(temporary) }
        } catch (_: Exception) { fail(DebuggerExportError.IO) }
    }
}
