package com.digihori.pgp.desktop.project

import com.digihori.pgp.desktop.window.DesktopUiPreferences
import java.awt.Desktop
import java.io.File

internal sealed interface DesktopExternalEditorResult {
    data object Opened : DesktopExternalEditorResult
    data class Failed(val message: String) : DesktopExternalEditorResult
}

/** Opens a diagnostic using PGP_EDITOR="code --goto {file}:{line}" or the OS file handler. */
internal object DesktopExternalEditor {
    fun open(file: File, line: Int?): DesktopExternalEditorResult = runCatching {
        val configured = DesktopUiPreferences.externalEditorCommand()
            ?: System.getenv("PGP_EDITOR")?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            val command = resolveCommand(commandFor(configured, file, line))
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
        check(Desktop.isDesktopSupported()) { "Configure an external editor in Settings" }
        val desktop = Desktop.getDesktop()
        check(desktop.isSupported(Desktop.Action.EDIT)) { "Configure an external editor in Settings" }
        desktop.edit(file)
    }

    internal fun commandFor(template: String, file: File, line: Int?): List<String> {
        val arguments = splitCommand(template)
        val command = arguments.map {
            it.replace("{file}", file.absolutePath).replace("{line}", (line ?: 1).toString())
        }
        return if (arguments.any { "{file}" in it }) command else command + file.absolutePath
    }

    internal fun resolveCommand(
        command: List<String>,
        osName: String = System.getProperty("os.name"),
        path: String = System.getenv("PATH").orEmpty(),
        applicationDirectories: List<File> = listOf(
            File("/Applications"), File(System.getProperty("user.home"), "Applications"),
        ),
    ): List<String> {
        if (!osName.startsWith("Mac", ignoreCase = true) || command.first() != "code") return command
        // Finder-launched apps do not inherit the terminal's PATH. Prefer an existing
        // CLI, then use VS Code's bundled launcher without requiring a shell setup.
        val executable = path.split(File.pathSeparatorChar).filter { it.isNotBlank() }
            .map { File(it, "code") }
            .firstOrNull { it.isFile && it.canExecute() }
            ?: applicationDirectories.asSequence()
                .map { File(it, "Visual Studio Code.app/Contents/Resources/app/bin/code") }
                .firstOrNull { it.isFile && it.canExecute() }
        check(executable != null) {
            "VS Code was not found. Install it in Applications, or specify its full launcher path in Settings."
        }
        return listOf(executable.absolutePath) + command.drop(1)
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
        require(quote == null) { "Editor command has an unclosed quote" }
        if (token.isNotEmpty()) result += token.toString()
        require(result.isNotEmpty()) { "Editor command is empty" }
        return result
    }
}
