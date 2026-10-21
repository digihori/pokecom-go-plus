package com.digihori.pgp.desktop.mcp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.*

/** Presentation only; the caller owns the server, clipboard and credentials. */
@Composable
internal fun McpServerPanel(
    port: String,
    onPortChange: (String) -> Unit,
    info: DesktopMcpServerInfo?,
    error: String?,
    lastRequest: DesktopMcpRequestSummary?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyToken: () -> Unit,
    onCopyConfiguration: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.text("mcp.heading"), style = MaterialTheme.typography.headlineSmall)
        Text(
            strings.text("mcp.scope"),
        )
        Text(strings.text("mcp.how"), style = MaterialTheme.typography.titleMedium)
        Text(
            strings.text("mcp.howHelp"),
        )
        Text(strings.text("mcp.setup"), style = MaterialTheme.typography.titleMedium)
        Text(
            strings.text("mcp.setupHelp"),
        )
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            enabled = info == null,
            label = { Text(strings.text("mcp.port")) },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (info == null) {
                Button(onClick = onStart) { Text(strings.text("mcp.start")) }
            } else {
                Button(onClick = onStop) { Text(strings.text("mcp.stop")) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { info ->
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.text("mcp.listening"))
                    Text(strings.text("mcp.url", info.url), fontFamily = FontFamily.Monospace)
                    Text(strings.text("mcp.token", info.token), fontFamily = FontFamily.Monospace)
                    Text(strings.text("mcp.credential", info.credentialPath), fontFamily = FontFamily.Monospace)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopyUrl) { Text(strings.text("mcp.copyUrl")) }
                TextButton(onClick = onCopyToken) { Text(strings.text("mcp.copyToken")) }
                TextButton(onClick = onCopyConfiguration) { Text(strings.text("mcp.copyConfig")) }
            }
            Text(
                strings.text("mcp.credentialHelp"),
            )
        }
        Text(strings.text("mcp.tools"), style = MaterialTheme.typography.titleMedium)
        Text("• pgp_get_capabilities\n• pgp_get_debug_context\n• pgp_get_debug_registrations\n• pgp_get_machine_spec\n• pgp_get_development_context\n• pgp_get_source\n• pgp_propose_source_patch\n• pgp_build_project\n• pgp_get_operation\n• pgp_propose_debug_action\n• pgp_get_debug_operation\n• pgp_get_build_artifact\n• pgp_propose_load\n• pgp_propose_continue\n• pgp_get_execution_operation", fontFamily = FontFamily.Monospace)
        Text(strings.text("mcp.lastRequest"), style = MaterialTheme.typography.titleMedium)
        Text(strings.mcpRequestDescription(lastRequest))
    }
}
