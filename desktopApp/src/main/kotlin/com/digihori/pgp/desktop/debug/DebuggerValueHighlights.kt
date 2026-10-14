package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.localization.uiLabel

@Composable
internal fun DebuggerComparisonLegend(basis: DebuggerComparisonBasis, strings: StudioStrings) {
    Text(strings.text("debug.diff.basis.${basis.name}"), style = MaterialTheme.typography.labelMedium)
    if (basis == DebuggerComparisonBasis.STEP || basis == DebuggerComparisonBasis.STOP) {
        Text(strings.text("debug.diff.legend"), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun changedValueStyle() = SpanStyle(color = MaterialTheme.colorScheme.onPrimaryContainer,
    background = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold,
    textDecoration = TextDecoration.Underline)

@Composable
internal fun DebuggerCpuValues(
    snapshot: CpuSnapshot?, previous: CpuSnapshot?, changedFields: Set<CpuField>,
    basis: DebuggerComparisonBasis, strings: StudioStrings,
) {
    val rows = DebuggerRegisterModel.rows(snapshot, if (basis == DebuggerComparisonBasis.RUNNING) null else previous)
    val style = changedValueStyle()
    Column {
        Text("CPU", style = MaterialTheme.typography.labelLarge)
        DebuggerComparisonLegend(basis, strings)
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            rows.forEach { row ->
                val text = buildAnnotatedString {
                    row.forEachIndexed { index, cell ->
                        if (index != 0) append("  ")
                        append("${cell.name}=")
                        if (cell.changed) withStyle(style) { append(cell.value) } else append(cell.value)
                    }
                }
                Text(text, fontFamily = FontFamily.Monospace, softWrap = false,
                    modifier = Modifier.semantics {
                        contentDescription = row.joinToString { cell -> if (cell.changed)
                            strings.text("debug.diff.value", cell.name, cell.previous!!, cell.value) else "${cell.name}=${cell.value}" }
                    })
            }
        }
        val changes = rows.flatten().filter { it.changed }
        val represented = changes.map { it.name }.toSet()
        val remaining = changedFields.filter { field ->
            (if (field == CpuField.CONTROL) "OUTC" else field.uiLabel(strings)) !in represented
        }
        if (basis != DebuggerComparisonBasis.RUNNING && (changes.isNotEmpty() || remaining.isNotEmpty())) {
            val descriptions = changes.map {
                strings.text("debug.diff.value", it.name, it.previous!!, it.value)
            } + remaining.map { it.uiLabel(strings) }
            Text(strings.text("debug.cpuChanged", descriptions.joinToString()), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun DebuggerMemoryLine(line: DesktopMemoryViewLine, changes: Map<Int, MemoryValueChange>, strings: StudioStrings) {
    val style = changedValueStyle()
    fun change(index: Int): MemoryValueChange? = changes[(line.address + index) and 0xffff]?.takeIf { it.after == line.bytes[index] }
    val text = buildAnnotatedString {
        append("${line.address.highlightHex(4)}: ")
        line.bytes.forEachIndexed { index, value ->
            if (index != 0) append(" ")
            if (change(index) != null) withStyle(style) { append(value.highlightHex(2)) } else append(value.highlightHex(2))
        }
        append("  |")
        line.asciiText.forEachIndexed { index, value ->
            if (change(index) != null) withStyle(style) { append(value) } else append(value)
        }
        append("|")
    }
    Text(text, fontFamily = FontFamily.Monospace, softWrap = false, modifier = Modifier.semantics {
        contentDescription = text.text + line.bytes.indices.mapNotNull(::change).joinToString(prefix = " ") {
            strings.text("debug.diff.value", it.address.highlightHex(4), it.before.highlightHex(2), it.after.highlightHex(2))
        }
    })
}

private fun Int.highlightHex(width: Int) = toString(16).uppercase().padStart(width, '0')
