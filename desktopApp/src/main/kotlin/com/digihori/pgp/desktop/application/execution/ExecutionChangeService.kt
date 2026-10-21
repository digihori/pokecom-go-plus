package com.digihori.pgp.desktop.application.execution

import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.runner.*
import java.util.UUID
import kotlinx.serialization.json.*

internal data class ExecutionWriteRange(val startAddress: Int, val length: Int)
internal data class ExecutionReview(val id: String, val action: String, val sessionId: String, val revision: Long,
    val machineId: String, val projectName: String, val projectRevision: String, val artifactId: String?,
    val pc: Int, val basicByteCount: Int, val writes: List<ExecutionWriteRange>, val writeByteCount: Int)
internal data class ExecutionOperation(val review: ExecutionReview, val status: String, val afterRevision: Long? = null, val code: String? = null)

internal class ExecutionChangeService(
    private val stateProvider: () -> DevelopmentReadState,
    private val registry: ProjectBuildRegistry,
    private val contentRevision: () -> String,
    private val onChange: () -> Unit = {},
    private val onLoad: (DesktopProjectWorkspace, DesktopEmulatorRunner, DesktopProjectArtifact) -> DesktopProjectApplyResult,
    private val onContinue: (DesktopProjectWorkspace, DesktopEmulatorRunner) -> Unit,
) {
    private var pending: ExecutionReview? = null
    private var target: DesktopEmulatorRunner? = null
    private var workspace: DesktopProjectWorkspace? = null
    private var build: RegisteredProjectBuild? = null
    private var generation = 0L
    private val operations = linkedMapOf<String, ExecutionOperation>()
    fun review() = pending
    fun history() = operations.values.toList().asReversed()
    fun artifact() = registry.metadata()
    private fun state(sessionId: String, revision: Long, projectRevision: String): DevelopmentReadState {
        val state = stateProvider()
        if (state.busy) fail("BUSY")
        val active = state.runner ?: fail("NO_ACTIVE_SESSION")
        if (state.debugService?.sessionIdentity()?.id != sessionId) fail("STALE_SESSION")
        if (active.sessionRevision != revision) fail("STALE_REVISION")
        if (active.state != RunnerState.PAUSED) fail("SESSION_NOT_PAUSED")
        val project = state.project ?: fail("NO_PROJECT")
        if (project.definition.machineId != active.machineId) fail("MACHINE_MISMATCH")
        if (contentRevision() != projectRevision) fail("STALE_PROJECT")
        return state
    }
    fun proposeLoad(sessionId: String, revision: Long, projectRevision: String, artifactId: String): JsonObject =
        propose("load", sessionId, revision, projectRevision, artifactId)
    fun proposeContinue(sessionId: String, revision: Long, projectRevision: String): JsonObject =
        propose("continue", sessionId, revision, projectRevision, null)
    private fun propose(action: String, sessionId: String, revision: Long, projectRevision: String, artifactId: String?): JsonObject {
        if (pending != null) fail("BUSY")
        val state = state(sessionId, revision, projectRevision)
        val active = state.runner!!
        val built = if (action == "load") registry.requireCurrent(artifactId) else null
        val writes = built?.let { plannedWrites(active, it.artifact) }.orEmpty()
        val review = ExecutionReview(UUID.randomUUID().toString(), action, sessionId, revision, active.machineId.value,
            state.project!!.definition.name.take(256), projectRevision, artifactId, active.cpuSnapshot().programCounter,
            built?.artifact?.copyBasicProgram()?.size ?: 0, writes, writes.sumOf { it.length })
        pending = review; target = active; workspace = state.project; build = built; generation = active.debugTargetGeneration
        operations[review.id] = ExecutionOperation(review, "PENDING_APPROVAL")
        while (operations.size > 32) operations.remove(operations.keys.first())
        onChange()
        return operation(review.id)
    }
    fun approve() {
        val review = pending ?: return
        val operation = operations.getValue(review.id)
        try {
            val current = state(review.sessionId, review.revision, review.projectRevision)
            val active = current.runner!!
            if (active !== target || current.project !== workspace || active.debugTargetGeneration != generation) fail("STALE_SESSION")
            if (review.action == "load") {
                val built = registry.requireCurrent(review.artifactId)
                if (built !== build) fail("STALE_BUILD")
                if (plannedWrites(active, built.artifact) != review.writes) fail("STALE_LOAD_PLAN")
                when (onLoad(current.project!!, active, built.artifact)) {
                    is DesktopProjectApplyResult.Success -> Unit
                    is DesktopProjectApplyResult.BasicFailure -> fail("BASIC_LOAD_FAILED")
                    is DesktopProjectApplyResult.MemoryFailure -> fail("MEMORY_LOAD_FAILED")
                }
                if (active.state != RunnerState.PAUSED) fail("LOAD_DID_NOT_STAY_PAUSED")
            } else {
                onContinue(current.project!!, active)
                if (active.state != RunnerState.RUNNING) fail("CONTINUE_FAILED")
            }
            operations[review.id] = operation.copy(status = "SUCCEEDED", afterRevision = active.sessionRevision)
        } catch (error: Exception) {
            operations[review.id] = operation.copy(status = "FAILED", code = (error as? DevelopmentReadError)?.code ?: "EXECUTION_OPERATION_FAILED")
        } finally { pending = null; target = null; workspace = null; build = null; onChange() }
    }
    fun reject() {
        val review = pending ?: return
        operations[review.id] = operations.getValue(review.id).copy(status = "REJECTED")
        pending = null; target = null; workspace = null; build = null; onChange()
    }
    private fun plannedWrites(runner: DesktopEmulatorRunner, artifact: DesktopProjectArtifact): List<ExecutionWriteRange> {
        val plan = ProgramImageLoader.plan(runner.memoryLayout, artifact.copyBasicProgram(), artifact.memoryImage, runner::memoryByte)
        val image = when (plan) {
            is ProgramImagePlanResult.Success -> plan.image ?: fail("EMPTY_BUILD")
            is ProgramImagePlanResult.BasicFailure -> fail("BASIC_LOAD_FAILED")
            is ProgramImagePlanResult.MemoryFailure -> fail("MEMORY_LOAD_FAILED")
        }
        val ranges = mutableListOf<ExecutionWriteRange>()
        for (segment in image.segments.sortedBy { it.startAddress }) {
            val last = ranges.lastOrNull()
            if (last != null && last.startAddress + last.length == segment.startAddress)
                ranges[ranges.lastIndex] = last.copy(length = last.length + segment.size)
            else ranges += ExecutionWriteRange(segment.startAddress, segment.size)
        }
        if (ranges.size > 128) fail("LOAD_RANGE_LIMIT")
        return ranges
    }
    fun operation(id: String): JsonObject {
        val operation = operations[id] ?: fail("OPERATION_NOT_FOUND")
        val review = operation.review
        return buildJsonObject {
            put("format", "pgp-execution-operation"); put("schemaVersion", 1); put("operationId", id)
            put("action", review.action); put("status", operation.status); put("sessionId", review.sessionId)
            put("beforeRevision", review.revision); put("afterRevision", operation.afterRevision?.let(::JsonPrimitive) ?: JsonNull)
            put("artifactId", review.artifactId?.let(::JsonPrimitive) ?: JsonNull); put("projectRevision", review.projectRevision)
            put("machineId", review.machineId); put("basicByteCount", review.basicByteCount); put("writeByteCount", review.writeByteCount)
            put("writes", buildJsonArray { review.writes.forEach { add(buildJsonObject { put("startAddress", it.startAddress); put("length", it.length) }) } })
            put("code", operation.code?.let(::JsonPrimitive) ?: JsonNull); put("approval", "Studio UI only")
            put("successMeaning", if (review.action == "continue") "executionStartedNotProgramFinished" else "loadedWhilePaused")
        }
    }
    private fun fail(code: String): Nothing = throw DevelopmentReadError(code)
}
