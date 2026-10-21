package com.digihori.pgp.desktop.application.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

@Composable
internal fun DebugReviewPanel(
    review: DebugReview?, history: List<DebugOperation>, onApprove: () -> Unit, onReject: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    if (review == null && history.isEmpty()) return
    Text(strings.text("mcp.debug.heading"), style = MaterialTheme.typography.titleLarge)
    review?.let {
        Text(strings.text("mcp.debug.action.${it.action.name}"), style = MaterialTheme.typography.titleMedium)
        Text("${it.machineId} · Session ${it.sessionId.take(8)} · revision ${it.revision}", fontFamily = FontFamily.Monospace)
        Text(strings.text(when (it.action.name) { "pause" -> "mcp.debug.pauseScope"; "step" -> "mcp.debug.stepScope"; "trace" -> "mcp.debug.traceScope"; else -> "mcp.debug.scope" }))
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.text("mcp.review.before")); Text(it.before, fontFamily = FontFamily.Monospace)
                Text(strings.text("mcp.review.after")); Text(it.after, fontFamily = FontFamily.Monospace)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove) { Text(strings.text("mcp.review.approve")) }
            OutlinedButton(onClick = onReject) { Text(strings.text("mcp.review.reject")) }
        }
    }
    Text(strings.text("mcp.debug.history"), style = MaterialTheme.typography.titleMedium)
    history.take(32).forEach {
        Text("${it.action} · ${it.status}${it.code?.let { code -> " · $code" }.orEmpty()} · ${it.id.take(8)}", fontFamily = FontFamily.Monospace)
        Text("Session ${it.sessionId.take(8)} · ${it.beforeRevision} → ${it.afterRevision ?: "—"}", fontFamily = FontFamily.Monospace)
        if (it.afterSummary.isNotEmpty()) Text("${it.beforeSummary}\n→ ${it.afterSummary}", fontFamily = FontFamily.Monospace)
    }
}
