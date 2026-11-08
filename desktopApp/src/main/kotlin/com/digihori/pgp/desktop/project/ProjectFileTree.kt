package com.digihori.pgp.desktop.project

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.localization.StudioStrings
import java.io.File

internal enum class ProjectFileAction(val label: String) {
    EDIT("project.edit"), LOAD("project.contextLoad"), MACRO("project.contextMacro"), WAV("project.wav"),
    ASSEMBLY("assembly.open"), RENAME("project.contextRename"), TRASH("project.contextTrash"),
}
internal data class ProjectFileEntry(val path: String, val file: File, val type: ProjectSourceType?, val registered: Boolean) {
    val macro get() = file.extension.equals("pgmacro", true)
    val needsUpdate get() = !registered && file.extension.lowercase() in setOf("bas", "asm", "dmp")
    fun actions(): List<ProjectFileAction> = buildList {
        if (file.extension.lowercase() in setOf("bas", "asm", "dmp", "pgmacro")) add(ProjectFileAction.EDIT)
        if (registered && type != null) add(ProjectFileAction.LOAD)
        if (macro) add(ProjectFileAction.MACRO)
        if (file.extension.lowercase() in setOf("bas", "dmp", "hex", "ihx")) add(ProjectFileAction.WAV)
        if (registered && type == ProjectSourceType.ASSEMBLY) add(ProjectFileAction.ASSEMBLY)
        add(ProjectFileAction.RENAME); add(ProjectFileAction.TRASH)
    }
}
internal data class ProjectFileNode(val name: String, val path: String, val entry: ProjectFileEntry?, val children: List<ProjectFileNode>)

internal fun projectFileEntries(tree: DesktopProjectTreeSnapshot): List<ProjectFileEntry> =
    (tree.tracked.map { ProjectFileEntry(it.definition.path, it.file, it.definition.type, true) } +
        tree.untracked.map { ProjectFileEntry(it.relativePath, it.file, null, false) }).distinctBy { it.path }

internal fun projectFileNodes(entries: List<ProjectFileEntry>, prefix: String = ""): List<ProjectFileNode> =
    entries.groupBy { it.path.removePrefix(prefix).substringBefore('/') }.map { (name, group) ->
        val path = prefix + name
        val file = group.firstOrNull { it.path == path }
        ProjectFileNode(name, path, file, if (file == null) projectFileNodes(group, "$path/") else emptyList())
    }.sortedWith(compareBy<ProjectFileNode> { it.entry != null }.thenBy { it.name.lowercase() }.thenBy { it.name })

@Composable
internal fun ProjectFileTree(
    nodes: List<ProjectFileNode>, selectedPath: String?, enabled: Boolean,
    canLoad: Boolean, onSelected: (String) -> Unit, onAction: (ProjectFileAction, ProjectFileEntry) -> Unit,
    strings: StudioStrings = StudioStrings.system, depth: Int = 0,
) {
    for (node in nodes) key(node.path) {
        val entry = node.entry
        if (entry == null) {
            var expanded by remember { mutableStateOf(true) }
            Text("${if (expanded) "▾" else "▸"} ${node.name}", Modifier.padding(start = (depth * 16).dp)
                .clickable { expanded = !expanded }.padding(vertical = 3.dp), fontFamily = FontFamily.Monospace)
            if (expanded) ProjectFileTree(node.children, selectedPath, enabled, canLoad, onSelected, onAction, strings, depth + 1)
        } else {
            val menu = remember { ContextMenuState() }
            LaunchedEffect(menu.status) {
                if (menu.status is ContextMenuState.Status.Open) onSelected(entry.path)
            }
            ContextMenuArea(state = menu, items = {
                if (!enabled) emptyList() else entry.actions().filter { it != ProjectFileAction.LOAD || canLoad }.map { action ->
                    ContextMenuItem(strings.text(action.label)) {
                        onSelected(entry.path)
                        onAction(action, entry)
                    }
                }
            }) {
                val kind = if (entry.macro) strings.text("macro.menu") else entry.type?.let {
                    when (it) {
                        ProjectSourceType.BASIC -> "BASIC"
                        ProjectSourceType.ASSEMBLY -> "ASM"
                        ProjectSourceType.MEMORY_DUMP -> "DMP"
                        ProjectSourceType.RAW_BINARY -> "BIN"
                    }
                } ?: entry.file.extension.uppercase()
                Text("${node.name}  [$kind]" + if (entry.needsUpdate) "  ${strings.text("project.needsUpdate")}" else "",
                    Modifier.fillMaxWidth().background(if (entry.path == selectedPath) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .semantics { contentDescription = strings.text("project.selectPath", entry.path); selected = entry.path == selectedPath }
                        .clickable { onSelected(entry.path) }.padding(start = (depth * 16 + 4).dp, top = 3.dp, bottom = 3.dp, end = 8.dp),
                    fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
            }
        }
    }
}
