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
    creationDestination: String? = null,
    onChooseCreationParent: () -> Unit = {},
) {
    if (review == null && history.isEmpty()) return
    review?.let {
        Text(strings.text("mcp.review.heading"), style = MaterialTheme.typography.titleLarge)
        Text(strings.text(when (it.kind) {
            "PATCH" -> "mcp.review.patch"
            "CREATE_PROJECT" -> "mcp.review.createProject"
            "ADD_SOURCE" -> "mcp.review.addSource"
            else -> "mcp.review.build"
        }, it.projectName))
        it.sourcePath?.let { path -> Text(path, fontFamily = FontFamily.Monospace) }
        Text(strings.text("mcp.review.untrusted"))
        if (it.kind == "CREATE_PROJECT") {
            Text(strings.text("mcp.review.creationMachine", it.machineId))
            Text(strings.text("mcp.review.destination", creationDestination ?: strings.text("mcp.review.destinationMissing")))
            OutlinedButton(onClick = onChooseCreationParent) { Text(strings.text("mcp.review.chooseParent")) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove, enabled = it.kind != "CREATE_PROJECT" || creationDestination != null) { Text(strings.text("mcp.review.approve")) }
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
                if (it.generatedSources.isNotEmpty()) Text(it.machineId)
                it.generatedSources.forEach { source ->
                    Text("${source.definition.path} · ${source.type}", fontFamily = FontFamily.Monospace)
                    Text(source.text, fontFamily = FontFamily.Monospace)
                }
            }
        }

    }
    if (history.isNotEmpty()) Text(strings.text("mcp.review.history"), style = MaterialTheme.typography.titleMedium)
    history.take(32).forEach {
        Text("${it.kind} · ${it.status}${it.code?.let { code -> " · $code" }.orEmpty()} · ${it.id.take(8)}", fontFamily = FontFamily.Monospace)
    }
}
