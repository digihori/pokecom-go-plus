package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.parseHexAddress
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

internal fun DebuggerRegistrationError.display(strings: StudioStrings): String = when (this) {
    DebuggerRegistrationError.BREAKPOINT_LIMIT -> strings.text("debug.registration.error.$name", DebuggerRegistrationStore.MAX_BREAKPOINTS.toString())
    DebuggerRegistrationError.WATCH_LIMIT -> strings.text("debug.registration.error.$name", DebuggerRegistrationStore.MAX_WATCHES.toString())
    DebuggerRegistrationError.WATCH_BYTES_LIMIT -> strings.text("debug.registration.error.$name", DebuggerRegistrationStore.MAX_CHANGE_BYTES.toString())
    else -> strings.text("debug.registration.error.$name")
}

@Composable
internal fun DebuggerRegistrationPanel(
    runner: DesktopEmulatorRunner?, keyboardInput: DesktopKeyboardInput,
    executionRevision: Any? = null, strings: StudioStrings = StudioStrings.system,
    initialWatch: Boolean = false,
) {
    var watchSelected by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf(initialWatch) }
    var bpId by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf<Long?>(null) }
    var bpAddress by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf("") }
    var showConditionHelp by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf(false) }
    var bpCondition by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf("") }
    var bpHitCondition by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf("") }
    var bpEnabled by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf(true) }
    var watchId by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf<Long?>(null) }
    var start by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf("C000") }
    var end by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf("C0FF") }
    var kind by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf(DebuggerWatchKind.CHANGE) }
    var watchEnabled by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf(true) }
    var error by rememberSaveable(runner, runner?.debugSettingsRevision) { mutableStateOf<DebuggerRegistrationError?>(null) }
    var localRevision by remember { mutableStateOf(0) }
    val canEdit = runner != null && runner.state != RunnerState.RUNNING
    val breakpoints = remember(runner, executionRevision, runner?.registrationRevision, localRevision) { runner?.breakpointRegistrations().orEmpty() }
    val watches = remember(runner, executionRevision, runner?.registrationRevision, localRevision) { runner?.watchRegistrations().orEmpty() }
    fun result(value: DebuggerRegistrationResult?): Boolean {
        error = (value as? DebuggerRegistrationResult.Failure)?.error
        if (value is DebuggerRegistrationResult.Success) { localRevision++; return true }
        return false
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryTabRow(if (watchSelected) 1 else 0) {
            Tab(!watchSelected, { watchSelected = false; error = null }, text = { Text(strings.text("debug.registration.breakpoints")) })
            Tab(watchSelected, { watchSelected = true; error = null }, text = { Text(strings.text("debug.registration.watches")) })
        }
        Text(strings.text("debug.registration.scope"), style = MaterialTheme.typography.bodySmall)
        if (!canEdit) Text(strings.text(if (runner == null) "debug.noCpu" else "debug.registration.pause"))
        if (!watchSelected) {
            Text(if (bpId == null) strings.text("debug.registration.new") else strings.text("debug.registration.editing", bpId.toString()))
            Text(strings.text("debug.breakpoint.examples"), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { showConditionHelp = !showConditionHelp }) {
                Text(strings.text(if (showConditionHelp) "debug.breakpoint.hideHelp" else "debug.breakpoint.showHelp"))
            }
            if (showConditionHelp) Text(strings.text("debug.breakpoint.conditionHelp"), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bpAddress, { bpAddress = it; error = null }, label = { Text(strings.text("debug.addressHex")) },
                    readOnly = !canEdit, singleLine = true, modifier = Modifier.widthIn(max = 180.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                OutlinedTextField(bpCondition, { bpCondition = it; error = null }, label = { Text(strings.text("debug.breakpoint.condition")) },
                    placeholder = { Text("A == 0x41") }, readOnly = !canEdit, singleLine = true,
                    modifier = Modifier.widthIn(max = 240.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                OutlinedTextField(bpHitCondition, { bpHitCondition = it; error = null }, label = { Text(strings.text("debug.breakpoint.hitCondition")) },
                    placeholder = { Text(">= 3") }, readOnly = !canEdit, singleLine = true,
                    modifier = Modifier.widthIn(max = 260.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(bpEnabled, { bpEnabled = it }, enabled = canEdit,
                        modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.formEnabled") })
                    Text(strings.text("debug.registration.enabled"))
                }
                Button(enabled = canEdit, onClick = {
                    val address = parseHexAddress(bpAddress)
                    if (address == null) error = DebuggerRegistrationError.ADDRESS
                    else if (result(runner?.saveBreakpoint(bpId, address, bpEnabled, bpCondition, bpHitCondition))) bpId = null
                }) { Text(strings.text(if (bpId == null) "debug.registration.add" else "common.apply")) }
                if (bpId != null) TextButton({ bpId = null; error = null }) { Text(strings.text("common.cancel")) }
            }
            error?.let { Text(it.display(strings), color = MaterialTheme.colorScheme.error) }
            Text(strings.text("debug.registration.count", breakpoints.count { it.enabled }.toString(), breakpoints.size.toString()))
            if (breakpoints.isEmpty()) Text(strings.text("debug.registration.empty"))
            else RegistrationRows(breakpoints, { it.id }) { entry ->
                Text(strings.text("debug.breakpoint.ruleSummary", entry.condition.ifBlank { "—" },
                    entry.hitCondition.ifBlank { "—" }, entry.hitCount.toString()), fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(entry.enabled, { enabled ->
                        if (result(runner?.setBreakpointEnabled(entry.id, enabled)) && bpId == entry.id) bpEnabled = enabled
                    }, enabled = canEdit, modifier = Modifier.semantics {
                        contentDescription = strings.text("debug.registration.toggleBreakpoint", entry.address.historyHex(4)) })
                    Text("#${entry.id} ${entry.address.historyHex(4)}", fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.text(if (entry.enabled) "debug.registration.enabled" else "debug.registration.disabled"))
                }
                FlowRow {
                    TextButton(enabled = canEdit, onClick = {
                        bpId = entry.id; bpAddress = entry.address.historyHex(4); bpEnabled = entry.enabled; bpCondition = entry.condition; bpHitCondition = entry.hitCondition; error = null
                    }, modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.editBreakpoint", entry.address.historyHex(4)) }) { Text(strings.text("debug.registration.edit")) }
                    TextButton(enabled = canEdit, onClick = {
                        if (result(runner?.removeBreakpoint(entry.id)) && bpId == entry.id) bpId = null
                    }, modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.deleteBreakpoint", entry.address.historyHex(4)) }) { Text(strings.text("debug.registration.delete")) }
                }
            }
        } else {
            Text(if (watchId == null) strings.text("debug.registration.new") else strings.text("debug.registration.editing", watchId.toString()))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(start, { start = it; error = null }, label = { Text(strings.text("debug.history.start")) }, readOnly = !canEdit,
                    singleLine = true, modifier = Modifier.widthIn(max = 150.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                OutlinedTextField(end, { end = it; error = null }, label = { Text(strings.text("debug.history.end")) }, readOnly = !canEdit,
                    singleLine = true, modifier = Modifier.widthIn(max = 150.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
            }
            FlowRow {
                DebuggerWatchKind.entries.forEach { value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val label = strings.text("debug.registration.kind.${value.name}")
                        RadioButton(kind == value, { kind = value; error = null }, enabled = canEdit,
                            modifier = Modifier.semantics { contentDescription = label })
                        Text(label)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(watchEnabled, { watchEnabled = it }, enabled = canEdit,
                        modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.formEnabled") })
                    Text(strings.text("debug.registration.enabled"))
                }
                Button(enabled = canEdit, onClick = {
                    val first = parseHexAddress(start)
                    val last = parseHexAddress(end)
                    if (first == null || last == null) error = DebuggerRegistrationError.ADDRESS
                    else if (result(runner?.saveWatch(watchId, first, last, kind, watchEnabled))) watchId = null
                }) { Text(strings.text(if (watchId == null) "debug.registration.add" else "common.apply")) }
                if (watchId != null) TextButton({ watchId = null; error = null }) { Text(strings.text("common.cancel")) }
            }
            error?.let { Text(it.display(strings), color = MaterialTheme.colorScheme.error) }
            Text(strings.text("debug.registration.watchBudget", watches.filter { it.enabled && it.kind == DebuggerWatchKind.CHANGE }
                .sumOf { it.range.last - it.range.first + 1 }.toString(), DebuggerRegistrationStore.MAX_CHANGE_BYTES.toString()), style = MaterialTheme.typography.labelSmall)
            Text(strings.text("debug.registration.count", watches.count { it.enabled }.toString(), watches.size.toString()))
            if (watches.isEmpty()) Text(strings.text("debug.registration.empty"))
            else RegistrationRows(watches, { it.id }) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(entry.enabled, { enabled ->
                        if (result(runner?.setWatchEnabled(entry.id, enabled)) && watchId == entry.id) watchEnabled = enabled
                    }, enabled = canEdit, modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.toggleWatch", entry.id.toString()) })
                    Text("#${entry.id} ${entry.range.first.historyHex(4)}–${entry.range.last.historyHex(4)}", fontFamily = FontFamily.Monospace)
                }
                Text(strings.text("debug.registration.watchDescription", strings.text("debug.registration.kind.${entry.kind.name}"),
                    strings.text(if (entry.enabled) "debug.registration.enabled" else "debug.registration.disabled")))
                FlowRow {
                    TextButton(enabled = canEdit, onClick = {
                        watchId = entry.id; start = entry.range.first.historyHex(4); end = entry.range.last.historyHex(4)
                        kind = entry.kind; watchEnabled = entry.enabled; error = null
                    }, modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.editWatch", entry.id.toString()) }) { Text(strings.text("debug.registration.edit")) }
                    TextButton(enabled = canEdit, onClick = {
                        if (result(runner?.removeWatch(entry.id)) && watchId == entry.id) watchId = null
                    }, modifier = Modifier.semantics { contentDescription = strings.text("debug.registration.deleteWatch", entry.id.toString()) }) { Text(strings.text("debug.registration.delete")) }
                }
            }
        }
    }
}

@Composable
private fun <T> RegistrationRows(entries: List<T>, id: (T) -> Long, row: @Composable ColumnScope.(T) -> Unit) {
    val scroll = rememberLazyListState()
    Box(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        LazyColumn(Modifier.fillMaxWidth().padding(end = 12.dp), scroll, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = id) { entry -> Column { row(entry) } }
        }
        Box(Modifier.matchParentSize()) { VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight()) }
    }
}
