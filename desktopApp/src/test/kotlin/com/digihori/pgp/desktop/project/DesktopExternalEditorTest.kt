package com.digihori.pgp.desktop.project

import kotlin.test.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class DesktopExternalEditorTest {
    @Test
    fun findsBundledVsCodeWithoutTerminalPathAndPreservesArguments() {
        val root = Files.createTempDirectory("pgp-editor-resolve").toFile()
        try {
            val executable = File(root, "Visual Studio Code.app/Contents/Resources/app/bin/code")
            executable.parentFile.mkdirs()
            executable.writeText("#!/bin/sh\n")
            executable.setExecutable(true)
            val arguments = listOf("code", "--goto", "/tmp/source with spaces.bas:42")
            assertEquals(listOf(executable.absolutePath) + arguments.drop(1),
                DesktopExternalEditor.resolveCommand(arguments, "Mac OS X", "", listOf(root)))
            val pathExecutable = File(root, "code")
            pathExecutable.writeText("#!/bin/sh\n")
            pathExecutable.setExecutable(true)
            assertEquals(listOf(pathExecutable.absolutePath) + arguments.drop(1),
                DesktopExternalEditor.resolveCommand(arguments, "Mac OS X", root.absolutePath, listOf(root)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun preservesExplicitPathsAndOtherPlatformsAndReportsMissingVsCode() {
        val explicit = listOf("/custom/code", "file.bas")
        assertEquals(explicit, DesktopExternalEditor.resolveCommand(explicit, "Mac OS X", "", emptyList()))
        val command = listOf("code", "file.bas")
        assertEquals(command, DesktopExternalEditor.resolveCommand(command, "Linux", "", emptyList()))
        assertFailsWith<IllegalStateException> {
            DesktopExternalEditor.resolveCommand(command, "Mac OS X", "", emptyList())
        }
    }

    @Test
    fun substitutesFileWithSpacesAsOneArgumentAndDefaultsToFirstLine() {
        val file = File("/tmp/source with spaces.asm")
        assertEquals(listOf("code", "--goto", "${file.absolutePath}:1"),
            DesktopExternalEditor.commandFor("code --goto \"{file}:{line}\"", file, null))
    }

    @Test
    fun appendsFileWhenNoPlaceholderAndKeepsShellCharactersLiteral() {
        val file = File("/tmp/source;literal.asm")
        assertEquals(listOf("editor", "$(literal)", file.absolutePath),
            DesktopExternalEditor.commandFor("editor '$(literal)'", file, 12))
    }

    @Test
    fun rejectsUnclosedCommandQuote() {
        assertFailsWith<IllegalArgumentException> { DesktopExternalEditor.splitCommand("editor '\"unfinished") }
    }

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
