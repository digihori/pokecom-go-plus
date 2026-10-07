package com.digihori.pgp.desktop.project

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopExternalEditorTest {
    @Test
    fun splitsQuotedEditorTemplateWithoutUsingAShell() {
        assertEquals(
            listOf("/Applications/Visual Studio Code.app/bin/code", "--goto", "{file}:{line}"),
            DesktopExternalEditor.splitCommand(
                "\"/Applications/Visual Studio Code.app/bin/code\" --goto \"{file}:{line}\"",
            ),
        )
    }
}
