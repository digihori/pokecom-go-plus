package com.digihori.pgp.desktop.localization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import com.digihori.pgp.desktop.machine.MemoryDumpRangeFields
import com.digihori.pgp.desktop.*
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.input.GamepadDevice
import com.digihori.pgp.input.GamepadButton
import com.digihori.pgp.desktop.wav.*
import com.digihori.pgp.desktop.character.*
import com.digihori.pgp.core.character.CharacterEditorFormats
import com.digihori.pgp.desktop.debug.DebugContextFieldPair
import com.digihori.pgp.desktop.project.AssemblerSelectionOptions
import com.digihori.pgp.desktop.debug.CpuField
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.desktop.project.ProjectStarterOptions
import com.digihori.pgp.desktop.project.DesktopProjectStarter
import com.digihori.pgp.desktop.rom.RomLibraryRow
import com.digihori.pgp.desktop.rom.RomFileField
import com.digihori.pgp.desktop.rom.DesktopRomRegistrationCatalog
import com.digihori.pgp.desktop.rom.RomSetInputIssue
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.HostKeyInputModeSelector
import com.digihori.pgp.desktop.input.HostKeyInputMode
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import java.io.File
import javax.swing.SwingUtilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.digihori.pgp.controller.ui.ControllerSettings
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.input.GamepadSettings

/** Isolated visual check: no preferences, ROM library, real ROM, credentials or native input backend. */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "--assembler-profiles") {
        com.digihori.pgp.desktop.project.renderAssemblerProfilePreviews(File(args.getOrElse(1) { "build/assembler-profiles-preview" }))
        return
    }
    if (args.firstOrNull() == "--debugger-step4") {
        com.digihori.pgp.desktop.debug.renderDebuggerStep4Previews(File(args.getOrElse(1) { "build/debugger-step4-preview" }))
        return
    }
    if (args.firstOrNull() == "--debugger-step3") {
        com.digihori.pgp.desktop.debug.renderDebuggerStep3Previews(File(args.getOrElse(1) { "build/debugger-step3-preview" }))
        return
    }
    if (args.firstOrNull() == "--debugger-step2") {
        com.digihori.pgp.desktop.debug.renderDebuggerStep2Previews(File(args.getOrElse(1) { "build/debugger-step2-preview" }))
        return
    }
    if (args.firstOrNull() == "--debugger-step1") {
        com.digihori.pgp.desktop.debug.renderDebuggerStep1Previews(File(args.getOrElse(1) { "build/debugger-step1-preview" }))
        return
    }
    if (args.firstOrNull() == "--render") {
        renderPreviews(File(args.getOrElse(1) { "build/localization-preview" }), args.getOrNull(2))
        return
    }
    launchPreview()
}

private fun launchPreview() = application {
    for (language in StudioLanguage.entries) {
        val strings = remember(language) { StudioStrings(language) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Localization preview — ${language.name}",
            state = rememberWindowState(width = 600.dp, height = 780.dp),
        ) {
            MenuBar {
                Menu(strings.text("menu.file")) {
                    Item(strings.text("rom.create"), onClick = {})
                }
                Menu(strings.text("menu.help")) {
                    Item(strings.text("about.menu", "Pokecom GO Studio"), onClick = {})
                }
            }
            PreviewContent(strings)
        }
    }
}


@Composable
private fun PreviewContent(strings: StudioStrings, menu: Boolean = false, basic: Boolean = false) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (menu) {
                    Box {
                        DropdownMenu(expanded = true, onDismissRequest = {}) {
                            for (key in listOf("project.new", "project.open", "project.buildLoad", "assembly.open", "debug.saveCheckpoint")) {
                                DropdownMenuItem(text = { Text(strings.text(key)) }, onClick = {})
                            }
                        }
                    }
                }
                Text(strings.text("about.title", "Pokecom GO Studio"), style = MaterialTheme.typography.headlineSmall)
                Text(strings.text("about.description"))
                Text(strings.text("romSet.help"))
                Text(strings.text("settings.currentEditor", "/folder with spaces/日本語/very-long-folder-name/source.bas"))
                if (basic) {
                    Text(strings.text("settings.speed"), style = MaterialTheme.typography.titleLarge)
                    for (speed in DesktopExecutionSpeed.entries) Text(speed.uiLabel(strings))
                    Text(strings.text("settings.speedNote"))
                    HostKeyInputModeSelector(HostKeyInputMode.LOGICAL, {}, strings)
                    Text(strings.text("settings.editorExample"))
                    Text(strings.editorError("Editor command has an unclosed quote"), color = MaterialTheme.colorScheme.error)
                    Text(strings.text("main.status", RunnerState.RUNNING.uiLabel(strings), "1234567"))
                    Text(strings.text("main.stepped", "4"))
                } else ControllerSettings(
                    settings = GamepadSettings(), devices = emptyList(), selectedId = null,
                    pressed = emptySet(), allowedKeys = PocketKey.entries.toSet(),
                    onSelectDevice = {}, onSettingsChange = {}, strings = strings.controllerStrings(),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun renderPreviews(directory: File, group: String? = null) {
    check(directory.isDirectory || directory.mkdirs())
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            if (group?.substringBefore(':') in setOf("step8-ja", "step9-ja", "step10-ja", "language-ja") && language != StudioLanguage.JAPANESE) continue
            if (group?.substringBefore(':') in setOf("step8-en", "step9-en", "step10-en", "language-en") && language != StudioLanguage.ENGLISH) continue
            val strings = StudioStrings(language)
            for ((name, dimensions) in mapOf("normal" to (600 to 780), "narrow" to (420 to 620), "dialog" to (480 to 420), "menu" to (480 to 420), "basic" to (600 to 780), "basic-narrow" to (420 to 780), "rom" to (600 to 900), "rom-narrow" to (420 to 900), "rom-delete" to (480 to 420), "rom-replace" to (480 to 420), "project" to (600 to 1100), "project-narrow" to (420 to 1100), "project-create" to (480 to 650), "transfer" to (600 to 850), "transfer-narrow" to (420 to 850), "memory-range" to (480 to 550), "debugger" to (900 to 1100), "debugger-narrow" to (420 to 1100), "debug-tools" to (900 to 1000), "debug-tools-narrow" to (420 to 1000), "character" to (900 to 700), "character-narrow" to (420 to 800), "wav" to (800 to 420), "wav-narrow" to (420 to 550), "wave-s1" to (900 to 1000), "wave-s1-narrow" to (420 to 1300), "wave" to (900 to 1000), "wave-narrow" to (420 to 1300), "wav-unsaved" to (480 to 400), "wav-s2-basic-input" to (800 to 900), "wav-s2-basic-input-narrow" to (420 to 1050), "wav-s1-basic-input" to (800 to 900), "wav-s1-basic-input-narrow" to (420 to 1050), "wav-s1-input" to (800 to 900), "wav-s1-input-narrow" to (420 to 1050), "wav-auto" to (800 to 900), "wav-auto-narrow" to (420 to 900), "wav-project-dmp" to (420 to 900), "wav-input" to (800 to 900), "wav-input-narrow" to (420 to 1050), "mcp" to (760 to 1500), "mcp-narrow" to (420 to 2200), "mcp-stopped" to (420 to 1800), "controller" to (600 to 850), "controller-narrow" to (420 to 1000), "final-dialog" to (480 to 650), "final-dialog-bottom" to (480 to 650), "final-assembler" to (420 to 650), "language" to (600 to 480), "language-narrow" to (420 to 500))) {
                if (group == "wav-auto" && !name.startsWith("wav-auto")) continue
                if (group == "wav-choice" && !(name.startsWith("wav-s1-basic-input") || name.startsWith("wav-s2-basic-input") || name.startsWith("wav-input"))) continue
                if (group == "wav-finish" && !name.startsWith("wave")) continue
                if (group == "s2-basic" && !name.startsWith("wav-s2-basic-input")) continue
                if (group == "s1-basic" && !name.startsWith("wav-s1-basic-input")) continue
                if (group?.startsWith("step8") == true && !name.startsWith("wav") && !name.startsWith("character")) continue
                if (group?.startsWith("step9") == true && !name.startsWith("mcp") && !name.startsWith("controller")) continue
                if (group?.startsWith("step10") == true && !name.startsWith("final")) continue
                if (group?.startsWith("language") == true && !name.startsWith("language")) continue
                if (group?.contains(':') == true && name != group.substringAfter(':')) continue
                val scene = ImageComposeScene(dimensions.first, dimensions.second) {
                    if (name.startsWith("language")) PgpStudioTheme {
                        Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(20.dp)) {
                            Text(strings.text("settings.language"), style = MaterialTheme.typography.titleLarge)
                            LanguageSettingsPanel(StudioLanguagePreference.ENGLISH, {}, true, strings = strings)
                        } }
                    } else if (name.startsWith("final")) FinalPreview(strings, name)
                    else if (name.startsWith("mcp")) McpPreview(strings, name == "mcp-stopped")
                    else if (name.startsWith("controller")) ControllerPreview(strings)
                    else if (name == "wav-unsaved") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {}, title = { Text(strings.text("wav.replaceUnsaved")) },
                            text = { Text(strings.text("wav.unsaved")) },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("wav.replace")) } },
                            dismissButton = { TextButton(onClick = {}) { Text(strings.text("wav.keepCurrent")) } })
                    } else if (name.startsWith("character")) PgpStudioTheme {
                        Surface(Modifier.fillMaxSize()) { CharacterEditorPanel(strings, DesktopCharacterEditorModel(CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP)
                            .withByteText("1, 2, 3, 4, 128").applyByteText()) }
                    } else if (name.startsWith("wave")) WavePreview(strings, name.startsWith("wave-s1"))
                    else if (name.startsWith("wav")) PgpStudioTheme { Surface(Modifier.fillMaxSize()) { WavToolPanel(null, if (name.startsWith("wav-auto")) WavToolInput(File("/examples/input.wav"), "File", MachineId("pc-1251"), 1L) else if (name == "wav-project-dmp") WavToolInput(File("/examples/code.dmp"), "Project", MachineId("pc-1251"), 1L, fromProject = true) else if (name.startsWith("wav-s2-basic-input")) WavToolInput(File("/examples/sample.bas"), strings.text("wav.origin"), MachineId("pc-1470u"), 1L) else if (name.startsWith("wav-s1-basic-input")) WavToolInput(File("/examples/sample.bas"), strings.text("wav.origin"), MachineId("pc-1401"), 1L) else if (name.startsWith("wav-s1-input")) WavToolInput(File("/examples/code.dmp"), strings.text("wav.origin"), MachineId("pc-1350"), 1L) else if (name.startsWith("wav-input")) WavToolInput(File("/日本語/very-long-folder-name/program.bas"), strings.text("wav.origin"), MachineId("pc-1245"), 1L) else null, false, {}, {}, strings) } }
                    else if (name.startsWith("debug")) DebuggerPreview(strings, name.startsWith("debug-tools"))
                    else if (name == "memory-range") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {},
                            title = { Text(strings.text("memory.saveTitle")) },
                            text = { MemoryDumpRangeFields("C100", "C000", {}, {}, strings.text("memory.reversedRange"), strings) },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("common.save")) } },
                            dismissButton = { TextButton(onClick = {}) { Text(strings.text("common.cancel")) } })
                    } else if (name.startsWith("transfer")) TransferPreview(strings)
                    else if (name == "project-create") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {},
                            title = { Text(strings.text("project.createTitle")) },
                            text = { Column {
                                OutlinedTextField("New PGP Project", {}, label = { Text(strings.text("project.name")) })
                                Text(strings.text("project.starters"))
                                ProjectStarterOptions(setOf(DesktopProjectStarter.ASSEMBLY), {}, strings)
                                Text(strings.text("project.noOverwrite"))
                            } },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("project.createFolder")) } },
                            dismissButton = { TextButton(onClick = {}) { Text(strings.text("common.cancel")) } })
                    } else if (name.startsWith("project")) ProjectPreview(strings)
                    else if (name == "rom-replace") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {},
                            title = { Text(strings.text("romSet.replaceConfirm")) },
                            text = { Text("/folder with spaces/日本語/very-long-folder-name/output ROM.pgrom") },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("romSet.replace")) } },
                            dismissButton = { TextButton(onClick = {}) { Text(strings.text("romSet.cancel")) } })
                    } else if (name == "rom-delete") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {},
                            title = { Text(strings.text("rom.removeConfirm", DesktopRomRegistrationCatalog.label(MachineId("pc-1261"), strings))) },
                            text = { Text(strings.text("rom.removeHelp")) },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("rom.removeAction")) } },
                            dismissButton = { TextButton(onClick = {}) { Text(strings.text("common.cancel")) } })
                    } else if (name.startsWith("rom")) RomPreview(strings)
                    else if (name == "dialog") PgpStudioTheme {
                        AlertDialog(onDismissRequest = {},
                            title = { Text(strings.text("about.title", "Pokecom GO Studio")) },
                            text = { Text(strings.text("about.description") + "\n" + strings.text("about.version", "1.3.1")) },
                            confirmButton = { TextButton(onClick = {}) { Text(strings.text("common.close")) } })
                    } else PreviewContent(strings, menu = name == "menu", basic = name.startsWith("basic"))
                }
                try {
                    scene.render(0).close()
                    scene.render(100_000_000).use { image ->
                        image.encodeToData()?.use { data ->
                            File(directory, "${language.name.lowercase()}-$name.png").writeBytes(data.bytes)
                        } ?: error("PNG encoding failed")
                    }
                } finally { scene.close() }
            }
        }
    }
}


@Composable
private fun RomPreview(strings: StudioStrings) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text("rom.libraryTitle"), style = MaterialTheme.typography.titleLarge)
                Text(strings.text("rom.libraryHelp"))
                RomLibraryRow(MachineId("pc-1251"), true, "日本語/very-long-folder-name/original ROM.pgrom", {}, {}, strings)
                RomLibraryRow(MachineId("pc-1261"), false, null, {}, {}, strings)
                RomLibraryRow(MachineId("pc-1401"), true, "original.bin", {}, {}, strings)
                Text(strings.text("romSet.title"), style = MaterialTheme.typography.titleLarge)
                Text(strings.text("romSet.help"))
                RomFileField(strings.text("romSet.internal"), File("/folder with spaces/日本語/internal ROM.bin"), true,
                    strings.romSetInputIssue(RomSetInputIssue.SizeMismatch(8192, 3)), strings) {}
                RomFileField(strings.text("romSet.externalBanked", "128", "8"), null, true, strings = strings) {}
                Text(strings.text("romSet.bankHelp", "7"))
                RomFileField(strings.text("romSet.destination"), File("/folder with spaces/日本語/output ROM.pgrom"), true, strings = strings) {}
                Text(strings.text("romSet.created", "/folder with spaces/日本語/output ROM.pgrom"))
                Text(strings.text("romSet.creating"))
            }
        }
    }
}


@Composable
private fun ProjectPreview(strings: StudioStrings) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.text("assembly.title"), style = MaterialTheme.typography.titleLarge)
                Text("日本語の長いプロジェクト名")
                Text(strings.text("assembly.previewReady", "1024", "2"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text(strings.text("assembly.assemble")) }
                    Button(onClick = {}) { Text(strings.text("assembly.build")) }
                    Button(onClick = {}) { Text(strings.text("project.buildLoad")) }
                }
                Text(strings.text("assembly.help"))
                Text(strings.text("assembly.emulatorChanged"))
                Text(strings.text("load.help"))
                Text(strings.text("project.groupCount", strings.text("project.tracked"), "1"))
                Text("src/日本語/very-long-folder-name/source.asm")
                Text(strings.text("assembly.untrackedSource", "src/未登録/source.asm"))
                Text(strings.text("assembly.outputs"), style = MaterialTheme.typography.titleMedium)
                Text(strings.text("assembly.outputFile", "build/source.dmp", strings.text("assembly.fileExists")))
                Text(strings.text("common.diagnostics"), style = MaterialTheme.typography.titleMedium)
                Text(strings.text("project.error.assemblySource", "source-id", "42", strings.projectOperationError("SC61 assembly failed (exit code 2)")))
                AssemblyOutputField(strings.text("assembly.memorySummary", "C000", "C003", "4", "RAM") + "\n\nORG 0xC000\nSTART: LII 0x12\nRTN\n\nSTART EQU 49152\n", strings)
                Text(strings.text("project.updated", "2", "1", strings.text("project.skipped", "src/日本語/file.txt")))
            }
        }
    }
}


@Composable
private fun TransferPreview(strings: StudioStrings) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text("basic.loadTitle"), style = MaterialTheme.typography.titleLarge)
                Text(strings.text("basic.loadedDirect", "日本語 very-long-folder-name/program.bas", "256", "C000", "C0FF"))
                Text(strings.text("transfer.standaloneAdditional", "日本語 very-long-folder-name/program.bas"))
                Text(strings.text("basic.merged", "program.bas", "PC-1245", "42"))
                Text(strings.text("project.error.oldBasic", "PC-1245", "12", "34", strings.transferDetail("BASIC line must start with a line number")), color = MaterialTheme.colorScheme.error)
                Text(strings.text("memory.saveTitle"), style = MaterialTheme.typography.titleLarge)
                Text(strings.text("memory.saved", "256", "C000", "C0FF", "日本語 very-long-folder-name/memory.dmp"))
                Text(strings.text("quick.openTitle"), style = MaterialTheme.typography.titleLarge)
                Text(strings.text("quick.saved", "3", "日本語 very-long-folder-name/output.dmp"))
                Text(strings.text("quick.error", "42", strings.transferDetail("Undefined symbol: USER_SYMBOL")), color = MaterialTheme.colorScheme.error)
                Text(strings.text("quick.externalFailed", strings.projectOperationError("SC61 assembly failed (exit code 2)")) + "\nLINE:42 ERROR CODE:7\nLIDP 0xC000\n{source}")
                Text("10 LET A=1.25\n20 PRINT A\n\nC000: 00 12 37\n\nORG 0xC100\nLII 0x12\nRTN", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}


@Composable
private fun DebuggerPreview(strings: StudioStrings, tools: Boolean) {
    val runner = remember { debuggerTestRunner() }
    val keyboard = remember { DesktopKeyboardInput() }
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text("debug.window", "Pokecom GO Studio"), style = MaterialTheme.typography.titleLarge)
                if (tools) {
                    InstructionTracePanel(runner, 0L, keyboard, strings)
                    MemoryWatchPanel(runner, keyboard, strings)
                    MemoryMapPanel(MachineCatalog.find(Pc1245RomDefinition.MACHINE_ID)!!.memoryRegions, {}, strings)
                    Text(strings.text("debug.contextHelp"))
                } else {
                    CpuRegisterPanel(runner.cpuSnapshot(), setOf(CpuField.PC, CpuField.INTERNAL_RAM), strings)
                    MemoryViewPanel(runner, keyboard, 0xC000, {}, strings)
                    DisassemblyPanel(runner, runner.cpuSnapshot(), keyboard, {}, strings)
                }
            }
        }
    }
}


@Composable
private fun WavePreview(strings: StudioStrings, s1: Boolean = false) {
    val output = remember {
        DesktopWavOperation.encode("C000: 01 02 FF\n".encodeToByteArray(), true,
            MachineId(if (s1) "pc-1350" else "pc-1245"), "TEST", false)
    }
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.wavSummary(output.summary))
                WavWaveform(output, 0, {}, {}, strings)
                val diagnostic = com.digihori.pgp.core.wav.CodecDiagnostic(
                    com.digihori.pgp.core.wav.CodecDiagnosticSeverity.ERROR,
                    com.digihori.pgp.core.wav.CodecStage.TRANSFER_DECODE,
                    com.digihori.pgp.core.wav.CodecDiagnosticCode.CHECKSUM_MISMATCH,
                    if (s1) "S1 binary checksum mismatch" else "OLD filename checksum mismatch",
                    com.digihori.pgp.core.wav.CodecDataRange(com.digihori.pgp.core.wav.CodecDataKind.RAW_BYTES, 8, 2),
                    mapOf("expected" to "FF", "actual" to "12"))
                Text(diagnostic.uiDescription(strings))
                Text(strings.text("wav.saved", "/日本語/very-long-folder-name/{file}/test.wav"))
            }
        }
    }
}


@Composable
private fun McpPreview(strings: StudioStrings, stopped: Boolean) {
    val info = if (stopped) null else DesktopMcpServerInfo("http://127.0.0.1:8765/mcp", "synthetic-preview-token-not-a-credential", 8765,
        "synthetic-helper", "/example/日本語/very-long-folder-name/mcp-headers.json")
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                McpServerPanel("8765", {}, info,
                    if (stopped) strings.mcpStartError("MCP credential file permissions are not owner-only") else null,
                    if (stopped) null else DesktopMcpRequestSummary("2026-10-14T00:00:00Z", "tools/call", "pgp_get_debug_context", false),
                    {}, {}, {}, {}, {}, strings)
            }
        }
    }
}

@Composable
private fun ControllerPreview(strings: StudioStrings) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                ControllerSettings(GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.ENTER, GamepadButton.WEST to PocketKey.SHIFT)),
                    listOf(GamepadDevice("synthetic-device-1", "日本語 very-long-controller-name"), GamepadDevice("synthetic-device-2", "Second controller")),
                    "synthetic-device-1", setOf(GamepadButton.SOUTH), setOf(PocketKey.ENTER, PocketKey.NUM_4), {}, {},
                    availableButtons = GamepadButton.entries.toSet() - GamepadButton.NORTH,
                    status = strings.text("controller.inputUnavailable", "synthetic native error {detail}"), strings = strings.controllerStrings())
            }
        }
    }
}


@Composable
private fun FinalPreview(strings: StudioStrings, name: String) {
    PgpStudioTheme {
        Surface(Modifier.fillMaxSize()) {
            if (name.startsWith("final-dialog")) {
                val scroll = rememberScrollState(if (name.endsWith("bottom")) Int.MAX_VALUE else 0)
                AlertDialog(onDismissRequest = {}, title = { Text(strings.text("debug.contextTitle")) },
                    text = {
                        Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.text("debug.contextHelp"))
                            DebugContextFieldPair("C000", "C0FF", {}, {}, "debug.memoryStart", "debug.memoryEnd", strings)
                            Text(strings.text("debug.omitMemory"))
                            DebugContextFieldPair("16", "64", {}, {}, "debug.disassemblyCount", "debug.traceEntries", strings)
                            DebugContextFieldPair("32", "16", {}, {}, "debug.accessEntries", "debug.bankEntries", strings)
                            Text(strings.text("debug.integerLimits"), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    confirmButton = { TextButton(onClick = {}) { Text(strings.text("debug.preview")) } },
                    dismissButton = { TextButton(onClick = {}) { Text(strings.text("common.close")) } })
            } else {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.text("settings.assembler"), style = MaterialTheme.typography.titleLarge)
                    Text(strings.text("assembler.scopeHelp"))
                    AssemblerSelectionOptions(false, {}, true, strings)
                    Text(strings.text("assembler.commandHelp"))
                    Text(strings.text("assembler.testHelp"))
                    Text(strings.text("settings.editorHelp"))
                    Text(strings.text("settings.keyboardHelp"))
                    Text(strings.text("romSet.help"))
                    Text(strings.text("settings.editorExample"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {}) { Text(strings.text("assembly.assemble")) }
                        Button(onClick = {}) { Text(strings.text("assembly.build")) }
                        Button(onClick = {}) { Text(strings.text("project.buildLoad")) }
                    }
                }
            }
        }
    }
}
