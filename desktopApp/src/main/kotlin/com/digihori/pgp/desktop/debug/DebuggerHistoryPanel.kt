package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

/** Filtering and navigation are display operations; the caller owns refresh and address selection. */
@Composable
internal fun DebuggerHistoryPanel(
    snapshot: DebuggerHistorySnapshot,
    keyboardInput: DesktopKeyboardInput,
    onInstruction: (Int) -> Unit,
    onMemory: (Int) -> Unit,
    onRefresh: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    var banksSelected by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf(DebuggerAccessFilter.ALL) }
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var bankText by rememberSaveable { mutableStateOf("") }
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    var helpVisible by rememberSaveable { mutableStateOf(false) }
    val canNavigate = snapshot.state != null && snapshot.state != RunnerState.RUNNING
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryTabRow(if (banksSelected) 1 else 0) {
            Tab(!banksSelected, { banksSelected = false }, text = { Text(strings.text("debug.history.access")) })
            Tab(banksSelected, { banksSelected = true }, text = { Text(strings.text("debug.history.bank")) })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onRefresh, enabled = snapshot.state != null) { Text(strings.text("debug.refresh")) }
            TextButton({ filtersVisible = !filtersVisible }) { Text(strings.text("debug.history.filters")) }
            TextButton({ helpVisible = !helpVisible }) { Text(strings.text("debug.history.help")) }
        }
        Text(strings.text("debug.history.currentOnly"), style = MaterialTheme.typography.bodySmall)
        if (snapshot.state == null) Text(strings.text("debug.noCpu"))
        if (helpVisible) {
            Text(strings.text("debug.history.navigationHelp"), style = MaterialTheme.typography.bodySmall)
            Text(strings.text(if (banksSelected) "debug.history.bankHelp" else "debug.history.accessHelp"), style = MaterialTheme.typography.bodySmall)
            Text(strings.text("debug.history.resetHelp"), style = MaterialTheme.typography.bodySmall)
        }
        if (banksSelected) {
            val banking = snapshot.banking
            if (banking == null) {
                Text(strings.text("debug.history.bankUnsupported"))
            } else {
                Text(strings.text("debug.history.bankStatus", (snapshot.selectedBank ?: "--").toString()))
                val bank = bankText.trim().toIntOrNull()?.takeIf { it in 0 until banking.bankCount }
                val invalid = bankText.isNotBlank() && bank == null
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filtersVisible) OutlinedTextField(bankText, { bankText = it }, label = { Text(strings.text("debug.history.bankFilter")) },
                        placeholder = { Text(strings.text("debug.history.any")) }, singleLine = true, isError = invalid,
                        modifier = Modifier.widthIn(max = 200.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                    TextButton({ onMemory(banking.windowStart) }, enabled = canNavigate) {
                        Text(strings.text("debug.history.currentWindow", banking.windowStart.historyHex(4)))
                    }
                }
                if (!filtersVisible && bank != null) Text(strings.text("debug.history.bankSelection", bank.toString()))
                if (invalid) Text(strings.text("debug.history.error.BANK", (banking.bankCount - 1).toString()), color = MaterialTheme.colorScheme.error)
                val entries = remember(snapshot.banks, bank, invalid) { if (invalid) emptyList() else DebuggerHistoryModel.banks(snapshot.banks, bank) }
                HistoryCount(entries.size, snapshot.banks.map { it.sequence }, strings)
                if (entries.isEmpty()) Text(strings.text(if (invalid) "debug.history.invalid" else "debug.history.empty"))
                else HistoryRows(entries, { it.sequence }) { entry ->
                    SelectionContainer {
                        Column {
                            Text(entry.historyText(), fontFamily = FontFamily.Monospace)
                            Text(strings.text("debug.history.selectorValue", entry.event.selectorValue.historyHex(2)), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        } else {
            Text(strings.text(if (snapshot.observationEnabled) "debug.history.recording" else "debug.history.notRecording"))
            if (filtersVisible) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DebuggerAccessFilter.entries.forEach { value ->
                    val label = strings.text("debug.history.filter.${value.name}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(kind == value, { kind = value }, modifier = Modifier.semantics { contentDescription = label })
                        Text(label)
                    }
                }
            }
            val range = historyRange(start, end)
            if (filtersVisible) Text(strings.text("debug.history.addressRange"), style = MaterialTheme.typography.labelSmall)
            if (filtersVisible) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(start, { start = it }, label = { Text(strings.text("debug.history.start")) },
                    placeholder = { Text("0000") }, singleLine = true,
                    modifier = Modifier.widthIn(max = 150.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
                OutlinedTextField(end, { end = it }, label = { Text(strings.text("debug.history.end")) },
                    placeholder = { Text("FFFF") }, singleLine = true,
                    modifier = Modifier.widthIn(max = 150.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) })
            }
            if (!filtersVisible && range is DebuggerHistoryRange.Valid && (kind != DebuggerAccessFilter.ALL || start.isNotBlank() || end.isNotBlank())) {
                Text(strings.text("debug.history.selection", strings.text("debug.history.filter.${kind.name}"),
                    range.range.first.historyHex(4), range.range.last.historyHex(4)))
            }
            if (range is DebuggerHistoryRange.Invalid) Text(strings.text("debug.history.error.${range.error.name}"), color = MaterialTheme.colorScheme.error)
            val entries = remember(snapshot.accesses, kind, range) {
                if (range is DebuggerHistoryRange.Valid) DebuggerHistoryModel.accesses(snapshot.accesses, kind, range.range) else emptyList()
            }
            HistoryCount(entries.size, snapshot.accesses.map { it.sequence }, strings)
            if (entries.isEmpty()) Text(strings.text(if (range is DebuggerHistoryRange.Invalid) "debug.history.invalid" else "debug.history.empty"))
            else HistoryRows(entries, { it.sequence }) { entry ->
                Column {
                    SelectionContainer { Text(entry.historyText(), fontFamily = FontFamily.Monospace) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton({ onInstruction(entry.instructionAddress) }, enabled = canNavigate) {
                            Text(strings.text("debug.history.instruction", entry.instructionAddress.historyHex(4)))
                        }
                        TextButton({ onMemory(entry.access.address) }, enabled = canNavigate) {
                            Text(strings.text("debug.history.memory", entry.access.address.historyHex(4)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCount(shown: Int, sequences: List<Long>, strings: StudioStrings) {
    Text(strings.text("debug.history.count", shown.toString(), sequences.size.toString(), DesktopEmulatorRunner.MAX_DEBUG_HISTORY_ENTRIES.toString()))
    if (sequences.isNotEmpty()) {
        Text(strings.text("debug.history.sequenceRange", sequences.first().toString(), sequences.last().toString()))
        if (sequences.first() > 0) Text(strings.text("debug.history.dropped"))
    }
}

@Composable
private fun <T> HistoryRows(entries: List<T>, sequence: (T) -> Long, row: @Composable (T) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(entries.firstOrNull()?.let(sequence)) { state.scrollToItem(0) }
    Box(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        LazyColumn(Modifier.fillMaxWidth().padding(end = 12.dp), state,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = sequence) { row(it) }
        }
        Box(Modifier.matchParentSize()) {
            VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}
