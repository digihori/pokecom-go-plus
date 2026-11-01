package com.digihori.pgp.desktop.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.*

internal enum class StdioMcpClient(val label: String) {
    VSCODE("VS Code"), COPILOT("Copilot"), CLAUDE("Claude Desktop"), GEMINI("Gemini CLI")
}

internal fun packagedMcpLauncher(): String? = System.getProperty("jpackage.app-path")
    ?.takeIf { it.isNotBlank() && Files.isRegularFile(Path.of(it)) }

/** Paths are JSON strings, never shell commands. No credentials or environment snapshots. */
internal fun stdioMcpConfiguration(client: StdioMcpClient, launcher: String, port: Int): String {
    require(port in 1..65535)
    require(Path.of(launcher).isAbsolute)
    val value = buildJsonObject {
        putJsonObject(if (client == StdioMcpClient.VSCODE) "servers" else "mcpServers") {
            putJsonObject("pokecom_go_studio") {
                if (client == StdioMcpClient.VSCODE) put("type", "stdio")
                put("command", launcher)
                putJsonArray("args") { add("--mcp-stdio"); add("--port"); add(port.toString()) }
                if (client == StdioMcpClient.COPILOT) putJsonArray("tools") { add("*") }
            }
        }
    }
    return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), value)
}
