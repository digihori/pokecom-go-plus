package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.desktop.project.*
import java.util.UUID
import kotlinx.serialization.json.*

internal data class ProjectBuildStamp(val workspace: DesktopProjectWorkspace, val revision: String)
internal data class RegisteredProjectBuild(val id: String, val stamp: ProjectBuildStamp, val artifact: DesktopProjectArtifact)

/** Retains the successful in-memory Build, never reloads edited dump/listing files from disk. */
internal class ProjectBuildRegistry(
    private val stateProvider: () -> DevelopmentReadState,
    private val revision: () -> String,
) {
    private var current: RegisteredProjectBuild? = null
    fun begin(workspace: DesktopProjectWorkspace): ProjectBuildStamp? {
        current = null // A failed or unsupported new Build must not leave an older artifact loadable.
        return runCatching {
            check(stateProvider().project === workspace)
            ProjectBuildStamp(workspace, revision())
        }.getOrNull()
    }
    fun publish(stamp: ProjectBuildStamp?, artifact: DesktopProjectArtifact): Boolean {
        current = null
        if (stamp == null || stateProvider().project !== stamp.workspace) return false
        if (runCatching { revision() != stamp.revision }.getOrDefault(true)) return false
        current = RegisteredProjectBuild(UUID.randomUUID().toString(), stamp, artifact)
        return true
    }
    fun requireCurrent(id: String? = null): RegisteredProjectBuild {
        val build = current ?: throw DevelopmentReadError("NO_BUILD")
        if (id != null && build.id != id) throw DevelopmentReadError("STALE_BUILD")
        if (stateProvider().project !== build.stamp.workspace || revision() != build.stamp.revision) throw DevelopmentReadError("STALE_BUILD")
        return build
    }
    fun metadata(): JsonObject = try {
        val build = requireCurrent()
        buildJsonObject {
            put("format", "pgp-build-artifact"); put("schemaVersion", 1); put("available", true)
            put("artifactId", build.id); put("projectRevision", build.stamp.revision)
            put("projectName", build.stamp.workspace.definition.name.take(256)); put("machineId", (build.artifact.targetMachineId ?: build.stamp.workspace.definition.machineId).value)
            put("basicByteCount", build.artifact.copyBasicProgram()?.size ?: 0)
            put("memoryByteCount", build.artifact.memoryImage?.byteCount ?: 0)
            put("memorySegmentCount", build.artifact.memoryImage?.segments?.size ?: 0)
            put("basis", "successfulInMemoryBuild; sourceContentChecked; notProofOfLoadedState")
        }
    } catch (error: Exception) {
        buildJsonObject {
            put("format", "pgp-build-artifact"); put("schemaVersion", 1); put("available", false)
            put("code", (error as? DevelopmentReadError)?.code ?: "BUILD_NOT_AVAILABLE")
        }
    }
}
