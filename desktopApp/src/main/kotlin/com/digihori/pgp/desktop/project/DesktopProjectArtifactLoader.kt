package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.BasicProgramMemoryError
import com.digihori.pgp.core.api.MemoryImageLoadError
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner

internal sealed interface DesktopProjectApplyResult {
    data class Success(
        val basicByteCount: Int,
        val memorySegmentCount: Int,
        val memoryByteCount: Int,
    ) : DesktopProjectApplyResult
    data class BasicFailure(val error: BasicProgramMemoryError) : DesktopProjectApplyResult
    data class MemoryFailure(val error: MemoryImageLoadError) : DesktopProjectApplyResult
}

/** Preflights and applies a fully built artifact while preserving the runner's previous run/pause state. */
internal object DesktopProjectArtifactLoader {
    fun load(
        runner: DesktopEmulatorRunner,
        artifact: DesktopProjectArtifact,
    ): DesktopProjectApplyResult {
        return when (val loaded = runner.loadProgramImage(artifact.copyBasicProgram(), artifact.memoryImage)) {
            is com.digihori.pgp.core.api.ProgramImageLoadResult.BasicFailure -> DesktopProjectApplyResult.BasicFailure(loaded.error)
            is com.digihori.pgp.core.api.ProgramImageLoadResult.MemoryFailure -> DesktopProjectApplyResult.MemoryFailure(loaded.error)
            is com.digihori.pgp.core.api.ProgramImageLoadResult.Success -> DesktopProjectApplyResult.Success(
                loaded.basicByteCount, loaded.memorySegmentCount, loaded.memoryByteCount,
            )
        }
    }
}
