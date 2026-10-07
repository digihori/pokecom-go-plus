package com.digihori.pgp.desktop.project

import java.awt.Desktop
import java.io.File

internal sealed interface DesktopExternalEditorResult {
    data object Opened : DesktopExternalEditorResult
    data class Failed(val message: String) : DesktopExternalEditorResult
}

/** Opens a diagnostic using PGP_EDITOR="code --goto {file}:{line}" or the OS file handler. */
internal object DesktopExternalEditor {
    fun open(file: File, line: Int?): DesktopExternalEditorResult = runCatching {
        val configured = System.getenv("PGP_EDITOR")?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            val command = splitCommand(configured).map { argument ->
                argument.replace("{file}", file.absolutePath).replace("{line}", (line ?: 1).toString())
            }.toMutableList()
            if (command.none { it.contains(file.absolutePath) }) command += file.absolutePath
            ProcessBuilder(command).start()
        } else {
            openWithSystemTextEditor(file)
        }
        DesktopExternalEditorResult.Opened
    }.getOrElse { DesktopExternalEditorResult.Failed(it.message ?: it::class.simpleName.orEmpty()) }

    private fun openWithSystemTextEditor(file: File) {
        if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
            // `open file` follows the .asm association, which may point back to Studio and be mistaken for a ROM.
            ProcessBuilder("open", "-t", file.absolutePath).start()
            return
        }
        check(Desktop.isDesktopSupported()) { "Set PGP_EDITOR to configure an external editor" }
        val desktop = Desktop.getDesktop()
        check(desktop.isSupported(Desktop.Action.EDIT)) { "Set PGP_EDITOR to configure an external editor" }
        desktop.edit(file)
    }

    internal fun splitCommand(value: String): List<String> {
        val result = mutableListOf<String>()
        val token = StringBuilder()
        var quote: Char? = null
        value.forEach { character ->
            when {
                quote != null && character == quote -> quote = null
                quote != null -> token.append(character)
                character == '\'' || character == '"' -> quote = character
                character.isWhitespace() && token.isNotEmpty() -> {
                    result += token.toString()
                    token.clear()
                }
                !character.isWhitespace() -> token.append(character)
            }
        }
        if (token.isNotEmpty()) result += token.toString()
        require(result.isNotEmpty()) { "Editor command is empty" }
        return result
    }
}
