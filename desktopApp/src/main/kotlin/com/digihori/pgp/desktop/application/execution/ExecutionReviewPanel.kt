package com.digihori.pgp.desktop.application.execution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

@Composable
internal fun ExecutionReviewPanel(
    review: ExecutionReview?, history: List<ExecutionOperation>, onApprove: () -> Unit, onReject: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    if (review == null && history.isEmpty()) return
    Text(strings.text("mcp.execution.heading"), style = MaterialTheme.typography.titleLarge)
    review?.let {
        Text(strings.text("mcp.execution.${it.action}"), style = MaterialTheme.typography.titleMedium)
        Text(it.projectName)
        Text("${it.machineId} · Session ${it.sessionId.take(8)} · revision ${it.revision}", fontFamily = FontFamily.Monospace)
        Text(strings.text("mcp.execution.${it.action}Scope"))
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PC=0x${it.pc.hex()}", fontFamily = FontFamily.Monospace)
                if (it.action == "load") {
                    Text("Build ${it.artifactId?.take(8)}", fontFamily = FontFamily.Monospace)
                    Text(strings.text("mcp.execution.bytes", it.basicByteCount.toString(), it.writeByteCount.toString()))
                    it.writes.forEach { range -> Text("0x${range.startAddress.hex()}..0x${(range.startAddress + range.length - 1).hex()} (${range.length})", fontFamily = FontFamily.Monospace) }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove) { Text(strings.text("mcp.review.approve")) }
            OutlinedButton(onClick = onReject) { Text(strings.text("mcp.review.reject")) }
        }
    }
    Text(strings.text("mcp.execution.history"), style = MaterialTheme.typography.titleMedium)
    history.take(32).forEach { operation ->
        val target = operation.review
        Text("${target.action} · ${operation.status}${operation.code?.let { " · $it" }.orEmpty()} · ${target.id.take(8)}", fontFamily = FontFamily.Monospace)
        Text("${target.projectName}\nSession ${target.sessionId.take(8)} · ${target.revision} → ${operation.afterRevision ?: "—"}", fontFamily = FontFamily.Monospace)
        if (target.action == "load") Text("Build ${target.artifactId?.take(8)} · ${target.writeByteCount} bytes", fontFamily = FontFamily.Monospace)
    }
}
private fun Int.hex() = toString(16).uppercase().padStart(4, '0')
