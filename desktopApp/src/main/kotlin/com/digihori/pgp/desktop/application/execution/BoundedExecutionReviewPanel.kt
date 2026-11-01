package com.digihori.pgp.desktop.application.execution

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

@Composable
internal fun BoundedExecutionReviewPanel(
    review: BoundedExecutionReview?, history: List<BoundedOperation>, onApprove: () -> Unit, onReject: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    if (review == null && history.isEmpty()) return
    Text(strings.text("mcp.bounded.heading"), style = MaterialTheme.typography.titleLarge)
    review?.let {
        Text("${it.projectName} · ${it.action}")
        Text("${it.machineId} · Session ${it.sessionId.take(8)} · revision ${it.revision}", fontFamily = FontFamily.Monospace)
        Text(strings.text("mcp.bounded.scope"))
        Text(strings.text("mcp.bounded.keys", it.keys.joinToString(" → ") { key -> key.name }.ifEmpty { "—" }), fontFamily = FontFamily.Monospace)
        Text(strings.text("mcp.bounded.limits", it.maxInstructions.toString(), it.maxCycles.toString()))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove) { Text(strings.text("mcp.review.approve")) }
            OutlinedButton(onClick = onReject) { Text(strings.text("mcp.review.reject")) }
        }
    }
    history.take(32).forEach { operation ->
        Text("${operation.review.action} · ${operation.status} · ${operation.result?.reason ?: operation.code ?: "—"} · ${operation.review.id.take(8)}", fontFamily = FontFamily.Monospace)
        operation.result?.let { Text(strings.text("mcp.bounded.result", it.instructions.toString(), it.cycles.toString(), it.inputComplete.toString())) }
    }
}
