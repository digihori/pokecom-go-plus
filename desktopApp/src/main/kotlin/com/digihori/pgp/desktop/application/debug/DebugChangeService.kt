package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.debug.*
import com.digihori.pgp.desktop.runner.*
import java.util.UUID
import kotlinx.serialization.json.*

internal data class DebugAction(val name: String, val parameters: JsonObject)
internal data class DebugReview(val id: String, val action: DebugAction, val sessionId: String,
    val revision: Long, val machineId: String, val before: String, val after: String)
internal data class DebugOperation(val id: String, val action: String, val status: String,
    val sessionId: String, val beforeRevision: Long, val afterRevision: Long? = null,
    val registrationId: Long? = null, val code: String? = null, val beforeSummary: String = "", val afterSummary: String = "")

/** One explicit UI approval per debug operation. Never calls run/load or writes memory directly. */
internal class DebugChangeService(
    private val stateProvider: () -> DevelopmentReadState,
    private val onChange: () -> Unit = {},
    private val onPause: (DesktopEmulatorRunner) -> Unit = {},
    private val onStep: (DesktopEmulatorRunner) -> Unit = {},
    private val onSettingsChanged: (DesktopEmulatorRunner) -> Unit = {},
) {
    private var pending: DebugReview? = null
    private var target: DesktopEmulatorRunner? = null
    private var project: Any? = null
    private var generation = 0L
    private val operations = linkedMapOf<String, DebugOperation>()
    fun review() = pending
    fun history() = operations.values.toList().asReversed()

    fun propose(sessionId: String, revision: Long?, action: DebugAction): JsonObject {
        if (pending != null) fail("BUSY")
        val state = stateProvider()
        val runner = state.runner ?: fail("NO_ACTIVE_SESSION")
        if (state.busy) fail("BUSY")
        val identity = state.debugService?.sessionIdentity() ?: fail("NOT_AVAILABLE")
        if (identity.id != sessionId) fail("STALE_SESSION")
        if (action.name == "pause") {
            if (revision != null) fail("INVALID_ARGUMENTS")
            if (runner.state != RunnerState.RUNNING) fail("SESSION_NOT_RUNNING")
        } else {
            if (revision == null || revision != runner.sessionRevision) fail("STALE_REVISION")
            if (runner.state == RunnerState.RUNNING) fail("SESSION_RUNNING")
            if (action.name == "step" && runner.state != RunnerState.PAUSED) fail("SESSION_NOT_PAUSED")
        }
        validate(runner, action)
        val (before, after) = describe(runner, action)
        val review = DebugReview(UUID.randomUUID().toString(), action, sessionId, runner.sessionRevision, runner.machineId.value, before, after)
        pending = review; target = runner; project = state.project; generation = runner.debugTargetGeneration
        operations[review.id] = DebugOperation(review.id, action.name, "PENDING_APPROVAL", sessionId, review.revision, beforeSummary = before, afterSummary = after)
        while (operations.size > 32) operations.remove(operations.keys.first())
        onChange()
        return operation(review.id)
    }

    fun approve() {
        val review = pending ?: return
        val old = operations.getValue(review.id)
        try {
            val state = stateProvider()
            val runner = state.runner ?: fail("STALE_SESSION")
            if (state.busy) fail("BUSY")
            if (runner !== target || state.project !== project || state.debugService?.sessionIdentity()?.id != review.sessionId || runner.debugTargetGeneration != generation) fail("STALE_SESSION")
            if (review.action.name == "pause") {
                if (runner.state != RunnerState.RUNNING) fail("SESSION_NOT_RUNNING")
            } else {
                if (runner.sessionRevision != review.revision) fail("STALE_REVISION")
                if (runner.state == RunnerState.RUNNING) fail("SESSION_RUNNING")
                if (review.action.name == "step" && runner.state != RunnerState.PAUSED) fail("SESSION_NOT_PAUSED")
            }
            validate(runner, review.action)
            val p = review.action.parameters
            fun int(key: String) = p.getValue(key).jsonPrimitive.int
            fun bool(key: String) = p.getValue(key).jsonPrimitive.boolean
            fun id() = p["id"]?.jsonPrimitive?.long
            val result = when (review.action.name) {
                "save_breakpoint" -> runner.saveBreakpoint(id(), int("address"), bool("enabled"), p["condition"]?.jsonPrimitive?.content, p["hitCondition"]?.jsonPrimitive?.content,
                    if ("bank" in p) p["bank"]?.takeUnless { it == JsonNull }?.jsonPrimitive?.int else runner.breakpointRegistrations().firstOrNull { it.id == id() }?.bank)
                "remove_breakpoint" -> runner.removeBreakpoint(id()!!)
                "save_watch" -> runner.saveWatch(id(), int("startAddress"), int("endAddress"), watchKind(p), bool("enabled"))
                "remove_watch" -> runner.removeWatch(id()!!)
                "trace" -> { runner.setInstructionTraceCapacity(int("capacity")); runner.setInstructionTraceEnabled(bool("enabled")); null }
                "pause" -> { onPause(runner); if (runner.state == RunnerState.RUNNING) fail("PAUSE_FAILED"); null }
                "step" -> { onStep(runner); if (runner.sessionRevision == review.revision) fail("STEP_FAILED"); null }
                else -> fail("INVALID_ARGUMENTS")
            }
            if (result is DebuggerRegistrationResult.Failure) fail(result.error.name)
            if (review.action.name !in setOf("pause", "step")) { runner.signalDebugSettingsChange(); onSettingsChanged(runner) }
            operations[review.id] = old.copy(status = "SUCCEEDED", afterRevision = runner.sessionRevision,
                registrationId = (result as? DebuggerRegistrationResult.Success)?.id)
        } catch (error: Exception) {
            operations[review.id] = old.copy(status = "FAILED", code = (error as? DevelopmentReadError)?.code ?: "DEBUG_OPERATION_FAILED")
        } finally { pending = null; target = null; project = null; onChange() }
    }
    fun reject() {
        val review = pending ?: return
        operations[review.id] = operations.getValue(review.id).copy(status = "REJECTED")
        pending = null; target = null; project = null; onChange()
    }
    fun operation(id: String): JsonObject {
        val op = operations[id] ?: fail("OPERATION_NOT_FOUND")
        return buildJsonObject {
            put("format", "pgp-debug-operation"); put("schemaVersion", 1); put("operationId", op.id)
            put("action", op.action); put("status", op.status); put("sessionId", op.sessionId)
            put("beforeRevision", op.beforeRevision); put("afterRevision", op.afterRevision?.let(::JsonPrimitive) ?: JsonNull)
            put("registrationId", op.registrationId?.let(::JsonPrimitive) ?: JsonNull)
            put("beforeSummary", op.beforeSummary); put("afterSummary", op.afterSummary)
            put("code", op.code?.let(::JsonPrimitive) ?: JsonNull); put("approval", "Studio UI only")
            put("stepExecutesOneInstruction", op.action == "step")
        }
    }

    private fun validate(runner: DesktopEmulatorRunner, action: DebugAction) {
        val p = action.parameters
        val optional = when (action.name) { "save_breakpoint" -> setOf("id", "condition", "hitCondition", "bank"); "save_watch" -> setOf("id"); else -> emptySet() }
        val required = when (action.name) {
            "save_breakpoint" -> setOf("address", "enabled")
            "save_watch" -> setOf("startAddress", "endAddress", "kind", "enabled")
            "remove_breakpoint", "remove_watch" -> setOf("id")
            "trace" -> setOf("enabled", "capacity")
            "pause", "step" -> emptySet()
            else -> fail("INVALID_ARGUMENTS")
        }
        if (!p.keys.containsAll(required) || p.keys.any { it !in required + optional }) fail("INVALID_ARGUMENTS")
        fun number(key: String, low: Long, high: Long): Long {
            val value = p[key] as? JsonPrimitive ?: fail("INVALID_ARGUMENTS")
            if (value.isString) fail("INVALID_ARGUMENTS")
            return value.longOrNull?.takeIf { it in low..high } ?: fail("INVALID_ARGUMENTS")
        }
        p["id"]?.let { number("id", 0, 9_007_199_254_740_991) }
        for (key in listOf("address", "startAddress", "endAddress")) if (key in p) number(key, 0, 65535)
        if ("enabled" in p) {
            val value = p["enabled"] as? JsonPrimitive ?: fail("INVALID_ARGUMENTS")
            if (value.isString || value.booleanOrNull == null) fail("INVALID_ARGUMENTS")
        }
        if ("capacity" in p) number("capacity", DesktopEmulatorRunner.MIN_INSTRUCTION_TRACE_CAPACITY.toLong(), DesktopEmulatorRunner.MAX_INSTRUCTION_TRACE_CAPACITY.toLong())
        if ("kind" in p) watchKind(p)
        val id = p["id"]?.jsonPrimitive?.long
        val bp = runner.breakpointRegistrations().firstOrNull { it.id == id }
        val watch = runner.watchRegistrations().firstOrNull { it.id == id }
        if (id != null && ((action.name.endsWith("breakpoint") && bp == null) || (action.name.endsWith("watch") && watch == null))) fail("NOT_FOUND")
        if (action.name == "save_breakpoint") {
            val address = p.getValue("address").jsonPrimitive.int
            val bank = if ("bank" in p) {
                if (p["bank"] == JsonNull) null else number("bank", 0, 255).toInt()
            } else bp?.bank
            if (!validBreakpointBank(runner.machineId, address, bank)) fail("BANK")
            if (runner.breakpointRegistrations().any { it.id != id && it.address == address && (it.bank == null || bank == null || it.bank == bank) }) fail("DUPLICATE_ADDRESS")
            if (id == null && runner.breakpointRegistrations().size >= DebuggerRegistrationStore.MAX_BREAKPOINTS) fail("BREAKPOINT_LIMIT")
            for (key in listOf("condition", "hitCondition")) p[key]?.let {
                val value = it as? JsonPrimitive ?: fail("INVALID_ARGUMENTS")
                if (!value.isString || value.content.length > if (key == "condition") 128 else 32) fail("INVALID_ARGUMENTS")
                if (value.content.isNotBlank() && (if (key == "condition") DebuggerBreakpointCondition.parse(value.content) else DebuggerBreakpointHitCondition.parse(value.content)) == null) fail(if (key == "condition") "CONDITION" else "HIT_CONDITION")
            }
        }
        if (action.name == "save_watch") {
            val start = p.getValue("startAddress").jsonPrimitive.int
            val end = p.getValue("endAddress").jsonPrimitive.int
            if (start > end) fail("RANGE")
            if (id == null && runner.watchRegistrations().size >= DebuggerRegistrationStore.MAX_WATCHES) fail("WATCH_LIMIT")
            if (watchKind(p) == DebuggerWatchKind.CHANGE) {
                val length = end - start + 1
                val existing = runner.watchRegistrations().filter { it.id != id && it.enabled && it.kind == DebuggerWatchKind.CHANGE }.sumOf { it.range.count() }
                if (length > DebuggerRegistrationStore.MAX_CHANGE_BYTES || (p.getValue("enabled").jsonPrimitive.boolean && existing + length > DebuggerRegistrationStore.MAX_CHANGE_BYTES)) fail("WATCH_BYTES_LIMIT")
            }
        }
    }
    private fun watchKind(p: JsonObject): DebuggerWatchKind {
        val value = p["kind"] as? JsonPrimitive ?: fail("INVALID_ARGUMENTS")
        if (!value.isString) fail("INVALID_ARGUMENTS")
        return when (value.content) { "change" -> DebuggerWatchKind.CHANGE; "read" -> DebuggerWatchKind.READ; "write" -> DebuggerWatchKind.WRITE; "readWrite" -> DebuggerWatchKind.READ_WRITE; else -> fail("INVALID_ARGUMENTS") }
    }
    private fun describe(runner: DesktopEmulatorRunner, action: DebugAction): Pair<String, String> {
        val p = action.parameters
        val id = p["id"]?.jsonPrimitive?.long
        val before = when {
            action.name.endsWith("breakpoint") -> runner.breakpointRegistrations().firstOrNull { it.id == id }?.let { "id=${it.id}, address=0x${it.address.hex()}, enabled=${it.enabled}, bank=${it.bank ?: "any"}\ncondition=${it.condition}, hitCondition=${it.hitCondition}, hitCount=${it.hitCount}" } ?: "—"
            action.name.endsWith("watch") -> runner.watchRegistrations().firstOrNull { it.id == id }?.let { "id=${it.id}, 0x${it.range.first.hex()}..0x${it.range.last.hex()}, ${it.kind}, enabled=${it.enabled}" } ?: "—"
            action.name == "trace" -> "enabled=${runner.isInstructionTraceEnabled()}, capacity=${runner.instructionTraceCapacity()}"
            else -> "${runner.state}, PC=0x${runner.cpuSnapshot().programCounter.hex()}"
        }
        val after = when (action.name) {
            "pause" -> "PAUSED (same live Session; PC may advance until approval)"
            "step" -> "1 instruction at PC=0x${runner.cpuSnapshot().programCounter.hex()} (may write RAM / I/O)"
            "remove_breakpoint", "remove_watch" -> "—"
            else -> p.entries.joinToString("\n") { (key, value) ->
                val text = if (key in setOf("address", "startAddress", "endAddress")) "0x${value.jsonPrimitive.int.hex()}"
                    else if (value.jsonPrimitive.isString) value.jsonPrimitive.content else value.toString()
                "$key = $text"
            } + if (action.name == "save_breakpoint" && "bank" !in p) "\nbank = " + (runner.breakpointRegistrations().firstOrNull { it.id == id }?.bank?.toString() ?: "any") else ""
        }
        return before to after
    }
    private fun Int.hex() = toString(16).uppercase().padStart(4, '0')
    private fun fail(code: String): Nothing = throw DevelopmentReadError(code)
}
