package com.digihori.pgp.desktop.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

/** Bridge the host's observable refresh signals to panels that otherwise only read plain Runner fields. */
@Composable
internal fun rememberDebuggerSessionRevision(
    runner: DesktopEmulatorRunner?, snapshot: CpuSnapshot?, state: RunnerState,
): Long = remember(runner, snapshot, state) { runner?.sessionRevision ?: 0L }
