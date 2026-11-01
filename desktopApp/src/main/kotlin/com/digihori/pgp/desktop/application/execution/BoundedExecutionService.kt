package com.digihori.pgp.desktop.application.execution

import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.project.DesktopProjectWorkspace
import com.digihori.pgp.desktop.runner.*
import java.util.UUID
import kotlinx.serialization.json.*

internal data class BoundedExecutionReview(
    val id: String, val sessionId: String, val revision: Long, val projectRevision: String,
    val projectName: String, val action: String, val keys: List<PocketKey>,
    val maxInstructions: Long, val maxCycles: Long,
    val machineId: String = "pc-1245", val callAddress: Long? = null,
)
internal data class BoundedOperation(
    val review: BoundedExecutionReview, val status: String, val result: BoundedExecutionResult? = null,
    val code: String? = null, val projectStillCurrent: Boolean? = null,
)

/** UI-thread queue. Input and execution only start from the local approval button. */
internal class BoundedExecutionService(
    private val stateProvider: () -> DevelopmentReadState,
    private val contentRevision: () -> String,
    private val onChange: () -> Unit = {},
) {
    private var pending: BoundedExecutionReview? = null
    private var target: DesktopEmulatorRunner? = null
    private var project: DesktopProjectWorkspace? = null
    private var generation = 0L
    private var runningId: String? = null
    private val operations = linkedMapOf<String, BoundedOperation>()
    fun review() = pending
    fun history() = operations.values.toList().asReversed()

    private fun targetState(sessionId: String, revision: Long, projectRevision: String, callAddress: Long? = null): DevelopmentReadState {
        val state = stateProvider()
        val runner = state.runner ?: fail("NO_ACTIVE_SESSION")
        if (state.debugService?.sessionIdentity()?.id != sessionId) fail("STALE_SESSION")
        if (runner.sessionRevision != revision) fail("STALE_REVISION")
        if (runner.state != RunnerState.PAUSED) fail("SESSION_NOT_PAUSED")
        if (state.busy) fail("BUSY")
        if (runner.machineId.value !in GeneratedProjectFiles.MACHINES || state.project?.definition?.machineId != runner.machineId) fail("MATCHING_SUPPORTED_PROJECT_REQUIRED")
        if (callAddress != null) {
            val location = runner.memoryLayout.locate(callAddress.toInt())
            if (location.kind != MachineMemoryKind.RAM || !location.writable || location.physicalAddress != callAddress.toInt()) fail("CALL_ADDRESS_NOT_RAM")
        }
        if (contentRevision() != projectRevision) fail("STALE_PROJECT")
        if (runner.currentOperatingMode() != OperatingMode.RUN) fail("RUN_MODE_REQUIRED")
        if (!runner.canStartBoundedInput()) fail("INPUT_BUSY")
        return state
    }

    fun propose(sessionId: String, revision: Long, projectRevision: String, action: String,
        keys: List<PocketKey>, maxInstructions: Long, maxCycles: Long, callAddress: Long? = null): JsonObject {
        observe()
        if (pending != null || runningId != null) fail("BUSY")
        if (maxInstructions !in 1..1_000_000 || maxCycles !in 1..5_000_000 || keys.size > 64) fail("INVALID_ARGUMENTS")
        val state = targetState(sessionId, revision, projectRevision, callAddress)
        val review = BoundedExecutionReview(UUID.randomUUID().toString(), sessionId, revision, projectRevision,
            state.project!!.definition.name.take(256), action, keys.toList(), maxInstructions, maxCycles, state.runner!!.machineId.value, callAddress)
        pending = review; target = state.runner; project = state.project; generation = target!!.debugTargetGeneration
        operations[review.id] = BoundedOperation(review, "PENDING_APPROVAL")
        while (operations.size > 32) operations.remove(operations.keys.first())
        onChange()
        return operation(review.id)
    }

    fun approve() {
        val review = pending ?: return
        try {
            val state = targetState(review.sessionId, review.revision, review.projectRevision, review.callAddress)
            if (state.runner !== target || state.project !== project || target!!.debugTargetGeneration != generation) fail("STALE_SESSION")
            target!!.startBoundedExecution(review.id, review.keys, review.maxInstructions, review.maxCycles)
            runningId = review.id
            operations[review.id] = BoundedOperation(review, "RUNNING")
        } catch (error: Exception) {
            operations[review.id] = BoundedOperation(review, "FAILED", code = (error as? DevelopmentReadError)?.code ?: "EXECUTION_FAILED")
            target = null; project = null
        } finally { pending = null; onChange() }
    }

    fun reject() {
        val review = pending ?: return
        operations[review.id] = BoundedOperation(review, "REJECTED")
        pending = null; target = null; project = null; onChange()
    }

    /** Also called after every runner tick, so terminal evidence is retained before another UI action. */
    fun observe() {
        val id = runningId ?: return
        val runner = target ?: return
        val state = stateProvider()
        if (runner.boundedExecutionActive && (state.runner !== runner || state.project !== project || runner.debugTargetGeneration != generation)) runner.pause()
        val result = runner.boundedExecution(id) ?: return
        if (result.reason == null) return
        val operation = operations.getValue(id)
        operations[id] = operation.copy(
            status = when (result.reason) { "interrupted" -> "INTERRUPTED"; "fault" -> "FAILED"; else -> "SUCCEEDED" },
            result = result, projectStillCurrent = runCatching { state.project === project && contentRevision() == operation.review.projectRevision }.getOrDefault(false),
        )
        runningId = null; target = null; project = null; onChange()
    }

    fun cancel() {
        reject()
        if (runningId != null) { target?.pause(); observe() }
    }

    fun operation(id: String): JsonObject {
        observe()
        val operation = operations[id] ?: fail("OPERATION_NOT_FOUND")
        val review = operation.review
        val result = operation.result ?: target?.takeIf { runningId == id }?.boundedExecution(id)
        return buildJsonObject {
            put("format", "pgp-bounded-execution"); put("schemaVersion", 1)
            put("operationId", id); put("status", operation.status); put("action", review.action)
            put("sessionId", review.sessionId); put("beforeRevision", review.revision)
            put("projectRevision", review.projectRevision); put("machineId", review.machineId)
            putJsonArray("keys") { review.keys.forEach { add(it.name) } }
            put("maxInstructions", review.maxInstructions); put("maxCycles", review.maxCycles)
            put("executedInstructions", result?.instructions?.let(::JsonPrimitive) ?: JsonNull)
            put("executedCycles", result?.cycles?.let(::JsonPrimitive) ?: JsonNull)
            put("inputComplete", result?.inputComplete?.let(::JsonPrimitive) ?: JsonNull)
            put("stopReason", result?.reason?.let(::JsonPrimitive) ?: JsonNull)
            put("observedRevision", result?.revision?.let(::JsonPrimitive) ?: JsonNull)
            put("observedPc", result?.pc?.let(::JsonPrimitive) ?: JsonNull)
            put("projectStillCurrent", operation.projectStillCurrent?.let(::JsonPrimitive) ?: JsonNull)
            put("code", operation.code?.let(::JsonPrimitive) ?: JsonNull)
            put("approval", "Studio UI only"); put("programFinished", "notDetermined")
            put("successMeaning", "boundedRequestStoppedNotProgramFinished; verify LCD and behavior separately")
            put("cycleLimitPrecision", "checked after each instruction; may exceed by the final instruction's cycles")
            put("inputPolicy", "stopping discards unsent keys and releases held automatic keys; do not automatically retry an incomplete sequence")
        }
    }
    private fun fail(code: String): Nothing = throw DevelopmentReadError(code)
}
