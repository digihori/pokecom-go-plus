package com.digihori.pgp.desktop.localization

import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState

internal fun DesktopExecutionSpeed.uiLabel(strings: StudioStrings = StudioStrings.system): String = strings.text("speed.$name")
internal fun RunnerState.uiLabel(strings: StudioStrings = StudioStrings.system): String = strings.text("runner.$name")

/** Known application validation is translated; OS/tool details stay in their original form. */
internal fun StudioStrings.editorError(message: String): String = when (message) {
    "Editor command has an unclosed quote" -> text("editor.error.unclosedQuote")
    "Editor command is empty" -> text("editor.error.empty")
    "Configure an external editor in Settings" -> text("editor.error.configure")
    "VS Code was not found. Install it in Applications, or specify its full launcher path in Settings." -> text("editor.error.missingCode")
    else -> message
}
