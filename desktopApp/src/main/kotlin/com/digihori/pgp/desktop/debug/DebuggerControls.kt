package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.desktop.displayName
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.runner.RunnerState

internal data class DebuggerStopLocation(val instructionAddress: Int, val memoryAddress: Int? = null)

internal fun DebuggerStopReason.location(): DebuggerStopLocation? = when (this) {
    is DebuggerStopReason.Breakpoint -> DebuggerStopLocation(address)
    is DebuggerStopReason.RunToAddress -> DebuggerStopLocation(address)
    is DebuggerStopReason.MemoryChanged -> DebuggerStopLocation(instructionAddress, changes.firstOrNull()?.address)
    is DebuggerStopReason.MemoryAccessed -> DebuggerStopLocation(instructionAddress, accesses.firstOrNull()?.address)
    is DebuggerStopReason.Fault -> when (val value = fault) {
        is CoreFault.UnsupportedOpcode -> DebuggerStopLocation(value.address)
    }
    else -> null
}

/** Display only: execution continues to use the application's existing callbacks. */
@Composable
internal fun DebuggerControls(
    hasSession: Boolean,
    state: RunnerState,
    snapshot: CpuSnapshot?,
    stopReason: DebuggerStopReason?,
    onRun: () -> Unit,
    onPause: () -> Unit,
    onStep: () -> Unit,
    onFollowPc: () -> Unit,
    onStopInstruction: (Int) -> Unit,
    onStopMemory: (Int) -> Unit,
    strings: StudioStrings = StudioStrings.system,
    onStepOver: (() -> Unit)? = null,
    onStepOut: (() -> Unit)? = null,
    canStepOut: Boolean = false,
    stepStatus: DebuggerStepStatus = DebuggerStepStatus.IDLE,
) {
    val location = stopReason?.location()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onRun, enabled = hasSession && state == RunnerState.PAUSED) { Text(strings.text("emulator.run")) }
            Button(onPause, enabled = hasSession && state == RunnerState.RUNNING) { Text(strings.text("emulator.pause")) }
            Button(onStep, enabled = hasSession && state == RunnerState.PAUSED) { Text(strings.text("emulator.step")) }
            onStepOver?.let { action ->
                Button(action, enabled = hasSession && state == RunnerState.PAUSED) { Text(strings.text("debug.step.over")) }
            }
            onStepOut?.let { action ->
                Button(action, enabled = hasSession && state == RunnerState.PAUSED && canStepOut) { Text(strings.text("debug.step.out")) }
            }
            Text(strings.text(if (hasSession) "runner.${state.name}" else "debug.noCpu"), modifier = Modifier.align(Alignment.CenterVertically))
        }
        if (stepStatus != DebuggerStepStatus.IDLE) Text(strings.text("debug.step.status.${stepStatus.name}"))
        if (onStepOut != null && hasSession && state == RunnerState.PAUSED && !canStepOut)
            Text(strings.text("debug.step.noFrameHint"), style = MaterialTheme.typography.bodySmall)
        SelectionContainer {
            Column {
                Text(strings.text("debug.nextPc", snapshot?.programCounter?.debugHex() ?: "----"), fontFamily = FontFamily.Monospace)
                Text(strings.text("debug.lastPc", snapshot?.currentProgramCounter?.debugHex() ?: "----"), fontFamily = FontFamily.Monospace)
                stopReason?.let { Text(strings.text("debug.stopReason", it.displayName(strings))) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onFollowPc, enabled = hasSession && snapshot != null) { Text(strings.text("debug.followPc")) }
            location?.let { target ->
                TextButton({ onStopInstruction(target.instructionAddress) }, enabled = hasSession) {
                    Text(strings.text("debug.showStopInstruction", target.instructionAddress.debugHex()))
                }
                target.memoryAddress?.let { address ->
                    TextButton({ onStopMemory(address) }, enabled = hasSession) {
                        Text(strings.text("debug.showStopMemory", address.debugHex()))
                    }
                }
            }
        }
    }
}

private fun Int.debugHex(): String = (this and 0xffff).toString(16).uppercase().padStart(4, '0')

/** Keep controls visible; wide windows give each pane its own scroll position. */
@Composable
internal fun DebuggerLayout(
    navigationRevision: Int,
    navigateMemory: Boolean,
    header: @Composable () -> Unit,
    execution: @Composable ColumnScope.() -> Unit,
    memory: @Composable ColumnScope.() -> Unit,
    strings: StudioStrings = StudioStrings.system,
    history: (@Composable ColumnScope.() -> Unit)? = null,
    registrations: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var registrationsSelected by remember { mutableStateOf(false) }
    val registrationsScroll = rememberScrollState()
    var historySelected by remember { mutableStateOf(false) }
    var memorySelected by remember { mutableStateOf(false) }
    val savedPanes = rememberSaveableStateHolder()
    val executionScroll = rememberScrollState()
    val memoryScroll = rememberScrollState()
    val historyScroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(navigationRevision) {
        if (navigationRevision > 0) {
            focusManager.clearFocus()
            historySelected = false
            registrationsSelected = false
            memorySelected = navigateMemory
            if (navigateMemory) memoryScroll.scrollTo(0) else executionScroll.scrollTo(0)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        val headerMaxHeight = maxHeight * 0.45f
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DebuggerScrollPane(Modifier.fillMaxWidth().heightIn(max = headerMaxHeight)) { header() }
            if (wide && (history != null || registrations != null)) {
                PrimaryTabRow(if (registrationsSelected) (if (history != null) 2 else 1) else if (historySelected) 1 else 0) {
                    Tab(!historySelected && !registrationsSelected, { historySelected = false; registrationsSelected = false }, text = { Text(strings.text("debug.history.liveTab")) })
                    if (history != null) Tab(historySelected && !registrationsSelected, { historySelected = true; registrationsSelected = false }, text = { Text(strings.text("debug.history.title")) })
                    if (registrations != null) Tab(registrationsSelected, { registrationsSelected = true; historySelected = false }, text = { Text(strings.text("debug.registration.title")) })
                }
            } else if (!wide) {
                PrimaryTabRow(selectedTabIndex = if (registrationsSelected) (if (history != null) 3 else 2) else if (historySelected) 2 else if (memorySelected) 1 else 0) {
                    Tab(selected = !historySelected && !registrationsSelected && !memorySelected, onClick = { historySelected = false; registrationsSelected = false; memorySelected = false },
                        text = { Text(strings.text(if (registrations == null) "debug.executionTab" else "debug.registration.executionTab")) })
                    Tab(selected = !historySelected && !registrationsSelected && memorySelected, onClick = { historySelected = false; registrationsSelected = false; memorySelected = true },
                        text = { Text(strings.text(if (registrations == null) "debug.memoryTab" else "debug.registration.memoryTab")) })
                    if (history != null) Tab(historySelected && !registrationsSelected, { historySelected = true; registrationsSelected = false }, text = { Text(strings.text("debug.history.title")) })
                    if (registrations != null) Tab(registrationsSelected, { registrationsSelected = true; historySelected = false }, text = { Text(strings.text("debug.registration.title")) })
                }
            }
            if (registrationsSelected && registrations != null) {
                DebuggerScrollPane(Modifier.weight(1f).fillMaxWidth(), registrationsScroll) {
                    savedPanes.SaveableStateProvider("registrations") { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = registrations) }
                }
            } else if (historySelected && history != null) {
                DebuggerScrollPane(Modifier.weight(1f).fillMaxWidth(), historyScroll) {
                    savedPanes.SaveableStateProvider("history") { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = history) }
                }
            } else if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DebuggerScrollPane(Modifier.weight(1f).fillMaxHeight(), executionScroll) {
                        savedPanes.SaveableStateProvider("execution") { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = execution) }
                    }
                    DebuggerScrollPane(Modifier.weight(1f).fillMaxHeight(), memoryScroll) {
                        savedPanes.SaveableStateProvider("memory") { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = memory) }
                    }
                }
            } else {
                DebuggerScrollPane(Modifier.weight(1f).fillMaxWidth(), if (memorySelected) memoryScroll else executionScroll) {
                    savedPanes.SaveableStateProvider(if (memorySelected) "memory" else "execution") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = if (memorySelected) memory else execution)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebuggerScrollPane(
    modifier: Modifier,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        // The overlay must not enlarge a header that should wrap its contents.
        Box(Modifier.matchParentSize()) {
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}
