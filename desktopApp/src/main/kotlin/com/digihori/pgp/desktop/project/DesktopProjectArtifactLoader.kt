package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.BasicProgramMemoryError
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.MemoryImageLoadError
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

internal sealed interface DesktopProjectApplyResult {
    data class Success(
        val basicByteCount: Int,
        val memorySegmentCount: Int,
        val memoryByteCount: Int,
    ) : DesktopProjectApplyResult
    data class BasicFailure(val error: BasicProgramMemoryError) : DesktopProjectApplyResult
    data class MemoryFailure(val error: MemoryImageLoadError) : DesktopProjectApplyResult
}

/** Applies a fully built artifact while preserving the runner's previous run/pause state. */
internal object DesktopProjectArtifactLoader {
    fun load(
        runner: DesktopEmulatorRunner,
        artifact: DesktopProjectArtifact,
    ): DesktopProjectApplyResult {
        val resumeAfterLoad = runner.state == RunnerState.RUNNING
        runner.pause()

        var basicByteCount = 0
        artifact.copyBasicProgram()?.let { program ->
            when (val loaded = runner.loadBasicProgram(program)) {
                is BasicProgramLoadResult.Failure -> {
                    if (resumeAfterLoad) runner.run()
                    return DesktopProjectApplyResult.BasicFailure(loaded.error)
                }
                is BasicProgramLoadResult.Success -> basicByteCount = loaded.size
            }
        }

        var memorySegmentCount = 0
        var memoryByteCount = 0
        artifact.memoryImage?.let { image ->
            when (val loaded = runner.loadMemoryImage(image)) {
                is MemoryImageLoadResult.Failure -> {
                    if (resumeAfterLoad) runner.run()
                    return DesktopProjectApplyResult.MemoryFailure(loaded.error)
                }
                is MemoryImageLoadResult.Success -> {
                    memorySegmentCount = loaded.segmentCount
                    memoryByteCount = loaded.byteCount
                }
            }
        }

        if (resumeAfterLoad) runner.run()
        return DesktopProjectApplyResult.Success(basicByteCount, memorySegmentCount, memoryByteCount)
    }
}
