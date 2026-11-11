package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectSourceType
import java.io.File
import kotlin.test.*

class ProjectFileTreeTest {
    @Test fun groupsByFoldersInsteadOfRegistrationAndSortsFoldersFirst() {
        val entries = listOf(
            ProjectFileEntry("src/z.bas", File("z.bas"), ProjectSourceType.BASIC, true),
            ProjectFileEntry("src/a.pgmacro", File("a.pgmacro"), null, false),
            ProjectFileEntry("src/lib/x.asm", File("x.asm"), null, false),
        )
        val root = projectFileNodes(entries).single()
        assertEquals("src", root.name)
        assertEquals(listOf("lib", "a.pgmacro", "z.bas"), root.children.map { it.name })
        assertEquals("src/lib/x.asm", root.children.first().children.single().entry!!.path)
        assertFalse(entries[1].needsUpdate)
        assertTrue(entries[2].needsUpdate)
    }

    @Test fun fileActionsFollowTypeAndRegistration() {
        val basic = ProjectFileEntry("src/a.bas", File("a.bas"), ProjectSourceType.BASIC, true)
        assertEquals(listOf(ProjectFileAction.EDIT, ProjectFileAction.LOAD, ProjectFileAction.WAV,
            ProjectFileAction.RENAME, ProjectFileAction.TRASH), basic.actions())
        val macro = ProjectFileEntry("src/a.pgmacro", File("a.pgmacro"), null, false)
        assertEquals(listOf(ProjectFileAction.EDIT, ProjectFileAction.MACRO, ProjectFileAction.RENAME, ProjectFileAction.TRASH), macro.actions())
        val assembly = ProjectFileEntry("src/a.asm", File("a.asm"), ProjectSourceType.ASSEMBLY, true)
        assertTrue(ProjectFileAction.ASSEMBLY in assembly.actions())
        assertFalse(ProjectFileAction.WAV in assembly.actions())
        assertFalse(ProjectFileAction.LOAD in assembly.copy(type = null, registered = false).actions())
        assertFalse(ProjectFileAction.ASSEMBLY in assembly.copy(type = null, registered = false).actions())
    }
}
