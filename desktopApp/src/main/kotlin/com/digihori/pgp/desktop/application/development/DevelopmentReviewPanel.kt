package com.digihori.pgp.desktop.application.development

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

@Composable
internal fun DevelopmentReviewPanel(
    review: DevelopmentReview?, history: List<DevelopmentOperation>, onApprove: () -> Unit, onReject: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    if (review == null && history.isEmpty()) return
    review?.let {
        Text(strings.text("mcp.review.heading"), style = MaterialTheme.typography.titleLarge)
        Text(strings.text(if (it.kind == "PATCH") "mcp.review.patch" else "mcp.review.build", it.projectName))
        it.sourcePath?.let { path -> Text(path, fontFamily = FontFamily.Monospace) }
        Text(strings.text("mcp.review.untrusted"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove) { Text(strings.text("mcp.review.approve")) }
            OutlinedButton(onClick = onReject) { Text(strings.text("mcp.review.reject")) }
        }
        SelectionContainer {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                it.replacements.forEach { edit ->
                    Text(strings.text("mcp.review.before"), color = MaterialTheme.colorScheme.error)
                    Text(edit.oldText, fontFamily = FontFamily.Monospace)
                    Text(strings.text("mcp.review.after"))
                    Text(edit.newText, fontFamily = FontFamily.Monospace)
                }
            }
        }

    }
    if (history.isNotEmpty()) Text(strings.text("mcp.review.history"), style = MaterialTheme.typography.titleMedium)
    history.take(32).forEach {
        Text("${it.kind} · ${it.status}${it.code?.let { code -> " · $code" }.orEmpty()} · ${it.id.take(8)}", fontFamily = FontFamily.Monospace)
    }
}
