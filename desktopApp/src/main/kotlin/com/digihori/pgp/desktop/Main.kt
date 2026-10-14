package com.digihori.pgp.desktop

import com.digihori.pgp.desktop.debug.DebuggerRegistrationPanel
import com.digihori.pgp.desktop.debug.DebuggerRegistrationError
import com.digihori.pgp.desktop.debug.DebuggerRegistrationResult
import com.digihori.pgp.desktop.debug.display
import com.digihori.pgp.desktop.debug.DebuggerHistoryPanel
import com.digihori.pgp.desktop.debug.captureDebuggerHistory
import com.digihori.pgp.desktop.debug.DebuggerComparisonTracker
import com.digihori.pgp.desktop.debug.DebuggerComparisonFrame
import com.digihori.pgp.desktop.debug.DebuggerComparisonBasis
import com.digihori.pgp.desktop.debug.DebuggerCpuValues
import com.digihori.pgp.desktop.debug.DebuggerMemoryLine
import com.digihori.pgp.desktop.debug.DebuggerComparisonLegend
import com.digihori.pgp.desktop.debug.DebuggerControls
import com.digihori.pgp.desktop.debug.DebuggerLayout
import com.digihori.pgp.desktop.debug.DebugContextFieldPair
import com.digihori.pgp.desktop.localization.LanguageSettingsPanel
import com.digihori.pgp.desktop.mcp.McpServerPanel
import com.digihori.pgp.desktop.localization.mcpStartError
import com.digihori.pgp.desktop.wav.WavToolInput
import com.digihori.pgp.desktop.wav.WavToolPanel
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.player.Pc1401KeyboardLayout
import com.digihori.pgp.player.PlayerKeyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.application
import com.digihori.pgp.core.ProjectInfo
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.EmulatorConfiguration
import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.BasicProgramMemoryError
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.api.MemoryImageLoadError
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.api.MemoryAccessKind
import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.api.MachineMemoryRegion
import com.digihori.pgp.core.api.MachineMemoryRegionKind
import com.digihori.pgp.core.debug.Sc61860InstructionFormatter
import com.digihori.pgp.core.debug.Sc61860Disassembler
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.machine.PgpMemoryDumpError
import com.digihori.pgp.core.project.ProjectManifestError
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.basic.DesktopBasicLoadError
import com.digihori.pgp.desktop.basic.DesktopBasicLoadResult
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileError
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import com.digihori.pgp.desktop.basic.DesktopBasicProgramDecodeResult
import com.digihori.pgp.desktop.character.CharacterEditorPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.input.HostKeyInputMode
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadError
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpWriter
import com.digihori.pgp.desktop.mcp.AwtMcpApplicationDispatcher
import com.digihori.pgp.desktop.mcp.DesktopMcpRequestSummary
import com.digihori.pgp.desktop.mcp.DesktopMcpServer
import com.digihori.pgp.desktop.mcp.DesktopMcpServerInfo
import com.digihori.pgp.desktop.mcp.DesktopMcpStartResult
import com.digihori.pgp.desktop.mcp.PgpMcpToolAdapter
import com.digihori.pgp.desktop.project.DesktopProjectChangeTracker
import com.digihori.pgp.desktop.project.DesktopProjectApplyResult
import com.digihori.pgp.desktop.project.DesktopProjectArtifactLoader
import com.digihori.pgp.desktop.project.DesktopProjectBuildError
import com.digihori.pgp.desktop.project.DesktopProjectBuildResult
import com.digihori.pgp.desktop.project.DesktopProjectBuilder
import com.digihori.pgp.desktop.project.DesktopProjectCreateError
import com.digihori.pgp.desktop.project.DesktopProjectCreateResult
import com.digihori.pgp.desktop.project.DesktopProjectStarter
import com.digihori.pgp.desktop.project.DesktopProjectWorkspaceCreator
import com.digihori.pgp.desktop.project.DesktopProjectOpenError
import com.digihori.pgp.desktop.project.DesktopProjectOpenResult
import com.digihori.pgp.desktop.project.DesktopProjectWorkspace
import com.digihori.pgp.desktop.project.DesktopProjectWorkspaceLoader
import com.digihori.pgp.desktop.project.DesktopProjectTreeScanner
import com.digihori.pgp.desktop.project.DesktopProjectTreeSnapshot
import com.digihori.pgp.desktop.project.DesktopProjectEditor
import com.digihori.pgp.desktop.project.DesktopProjectManifestUpdater
import com.digihori.pgp.desktop.project.DesktopProjectUpdateResult
import com.digihori.pgp.desktop.project.DesktopAssemblyDiagnostic
import com.digihori.pgp.desktop.project.DesktopAssemblySourceResult
import com.digihori.pgp.desktop.project.renderMemoryLayout
import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.desktop.project.DesktopAssemblyWorkspaceCompiler
import com.digihori.pgp.desktop.project.DesktopAssemblyWorkspaceResult
import com.digihori.pgp.desktop.project.DesktopExternalEditor
import com.digihori.pgp.desktop.project.DesktopExternalEditorResult
import com.digihori.pgp.desktop.project.DesktopProjectBuildOutputWriter
import com.digihori.pgp.desktop.project.renderDisassembly
import com.digihori.pgp.desktop.project.renderListing
import com.digihori.pgp.desktop.project.renderMap
import com.digihori.pgp.desktop.project.renderMemorySummary
import com.digihori.pgp.desktop.input.DesktopGamepadController
import com.digihori.pgp.desktop.localization.uiText
import com.digihori.pgp.desktop.localization.debugContextRequestError
import com.digihori.pgp.desktop.localization.memoryRegionLabel
import com.digihori.pgp.desktop.localization.transferDetail
import com.digihori.pgp.desktop.localization.projectOperationError
import com.digihori.pgp.desktop.localization.romLoadError
import com.digihori.pgp.desktop.localization.uiLabel
import com.digihori.pgp.desktop.localization.editorError
import com.digihori.pgp.desktop.localization.StudioLanguage
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.localization.controllerStrings
import com.digihori.pgp.controller.ui.ControllerSettings
import com.digihori.pgp.input.GamepadKeys
import kotlinx.coroutines.NonCancellable
import com.digihori.pgp.desktop.input.Pc1245KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1261KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1251KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1350KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1360KeyboardLayout
import com.digihori.pgp.desktop.input.PocketKeyCap
import com.digihori.pgp.desktop.audio.DesktopAudioPlayer
import com.digihori.pgp.desktop.application.debug.DebugContextError
import com.digihori.pgp.desktop.application.debug.DebugContextJsonWriter
import com.digihori.pgp.desktop.application.debug.DebugContextRequest
import com.digihori.pgp.desktop.application.debug.DebugContextResult
import com.digihori.pgp.desktop.application.debug.DebugMemoryRangeRequest
import com.digihori.pgp.desktop.application.debug.DesktopDebugContextService
import com.digihori.pgp.desktop.display.CharacterCellGeometry
import com.digihori.pgp.desktop.debug.CpuField
import com.digihori.pgp.desktop.debug.CpuSnapshotDifference
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.debug.DesktopDisassemblyModel
import com.digihori.pgp.desktop.debug.DesktopMemoryViewModel
import com.digihori.pgp.desktop.debug.DesktopDebuggerCheckpointWriter
import com.digihori.pgp.desktop.rom.DesktopRomLoadError
import com.digihori.pgp.desktop.rom.DesktopRomLoadResult
import com.digihori.pgp.desktop.rom.DesktopRomLoader
import com.digihori.pgp.desktop.rom.DesktopRomLocator
import com.digihori.pgp.desktop.rom.DesktopRomHistory
import com.digihori.pgp.desktop.rom.DesktopRomLibrary
import com.digihori.pgp.desktop.rom.DesktopRomLibraryResult
import com.digihori.pgp.desktop.rom.DesktopRomPackageConversionResult
import com.digihori.pgp.desktop.rom.DesktopRomSetWindow
import com.digihori.pgp.desktop.rom.DesktopRomPackageConverter
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import com.digihori.pgp.desktop.window.DesktopUiPreferences
import com.digihori.pgp.desktop.window.StudioWindow
import com.digihori.pgp.desktop.theme.LocalStudioComponentColors
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.awt.FileDialog
import java.awt.Desktop
import java.awt.Frame
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.Cursor
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

fun main() {
    StudioStrings.configureForStartup(DesktopUiPreferences.languagePreference())
    StudioStrings.system // Resolve and freeze the UI language before opening windows.
    configureSkikoRenderApi()
    application {
        val keyboardInput = remember { DesktopKeyboardInput() }
        val quitApplication = {
            keyboardInput.attach(null)
            exitApplication()
        }
        StudioWindow(
            id = "main",
            onCloseRequest = quitApplication,
            onPreviewKeyEvent = keyboardInput::handle,
            title = ProjectInfo.STUDIO_DISPLAY_NAME,
        ) {
            DisposableEffect(window) {
                val listener = object : WindowAdapter() {
                    override fun windowLostFocus(event: WindowEvent?) {
                        keyboardInput.clearActiveInputs()
                    }
                }
                window.addWindowFocusListener(listener)
                onDispose { window.removeWindowFocusListener(listener) }
            }
            App(keyboardInput, window, quitApplication)
        }
    }
}

private fun configureSkikoRenderApi() {
    val requestedApi = System.getenv("PGP_RENDER_API")
        ?.trim()
        ?.uppercase()
        ?.takeIf { it in SUPPORTED_RENDER_APIS }
    if (requestedApi != null) {
        System.setProperty("skiko.renderApi", requestedApi)
        return
    }
    if (
        System.getProperty("skiko.renderApi").isNullOrBlank() &&
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    ) {
        // GPU hover corruption was observed when the x64 build ran under emulation in a Windows ARM VM.
        // Keep the conservative default until a native Windows x64 system and future Skiko versions are verified.
        System.setProperty("skiko.renderApi", "SOFTWARE")
    }
}

private val SUPPORTED_RENDER_APIS = setOf(
    "DIRECT3D",
    "OPENGL",
    "SOFTWARE",
    "SOFTWARE_FAST",
    "SOFTWARE_COMPAT",
)

@Composable
private fun FrameWindowScope.App(
    keyboardInput: DesktopKeyboardInput,
    ownerWindow: Frame,
    onQuit: () -> Unit,
) {
    var runner by remember { mutableStateOf<DesktopEmulatorRunner?>(null) }
    var selectedMachineId by remember { mutableStateOf(MachineCatalog.defaultDefinition.id) }
    var runnerState by remember { mutableStateOf(RunnerState.PAUSED) }
    var loadedRomName by remember { mutableStateOf<String?>(null) }
    var loadedRomFile by remember { mutableStateOf<File?>(null) }
    var familyMemoryMode by remember { mutableStateOf(Pc1251FamilyMemoryMode.EXPANDED) }
    var languagePreference by remember { mutableStateOf(StudioStrings.startupPreference) }
    var languageSettingsError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf(uiText("main.initial")) }
    var executedCycles by remember { mutableLongStateOf(0L) }
    var display by remember { mutableStateOf<DisplaySnapshot?>(null) }
    var cpu by remember { mutableStateOf<CpuSnapshot?>(null) }
    var changedCpuFields by remember { mutableStateOf(emptySet<CpuField>()) }
    var debuggerStopReason by remember { mutableStateOf<DebuggerStopReason?>(null) }
    var showDebuggerWindow by remember { mutableStateOf(false) }
    var memoryViewStartAddress by remember { mutableStateOf(DEFAULT_MEMORY_VIEW_ADDRESS) }
    val comparisonTracker = remember { DebuggerComparisonTracker(DEFAULT_MEMORY_VIEW_ADDRESS) }
    var debuggerComparison by remember { mutableStateOf(DebuggerComparisonFrame()) }
    fun resetDebuggerComparison() {
        debuggerComparison = comparisonTracker.reset(runner)
        changedCpuFields = emptySet()
    }
    fun selectDebuggerMemory(address: Int) {
        memoryViewStartAddress = address and 0xffff
        debuggerComparison = comparisonTracker.selectMemory(runner, memoryViewStartAddress)
    }
    var operatingMode by remember { mutableStateOf(OperatingMode.RUN) }
    var requestedToneHz by remember { mutableStateOf(0) }
    var showCreateRomSetWindow by remember { mutableStateOf(false) }
    var showRomManager by remember { mutableStateOf(false) }
    var pendingRomRemoval by remember { mutableStateOf<MachineId?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSaveMemoryDumpDialog by remember { mutableStateOf(false) }
    var dumpStartAddress by remember { mutableStateOf("C000") }
    var dumpEndAddress by remember { mutableStateOf("C0FF") }
    var dumpRangeError by remember { mutableStateOf<String?>(null) }
    var showSaveDisassemblyDialog by remember { mutableStateOf(false) }
    var disassemblyStartAddress by remember { mutableStateOf("0000") }
    var disassemblyEndAddress by remember { mutableStateOf("1FFF") }
    var disassemblyRangeError by remember { mutableStateOf<String?>(null) }
    var showSaveCheckpointDialog by remember { mutableStateOf(false) }
    var checkpointStartAddress by remember { mutableStateOf("C000") }
    var checkpointEndAddress by remember { mutableStateOf("C0FF") }
    var checkpointRangeError by remember { mutableStateOf<String?>(null) }
    var showDebugContextDialog by remember { mutableStateOf(false) }
    var debugContextMemoryStart by remember { mutableStateOf("C000") }
    var debugContextMemoryEnd by remember { mutableStateOf("C0FF") }
    var debugContextDisassemblyCount by remember { mutableStateOf("32") }
    var debugContextAccessHistoryLimit by remember { mutableStateOf("256") }
    var debugContextBankHistoryLimit by remember { mutableStateOf("256") }
    var debugContextTraceLimit by remember { mutableStateOf("256") }
    var debugContextError by remember { mutableStateOf<String?>(null) }
    var debugContextPreview by remember { mutableStateOf<ByteArray?>(null) }
    val debugContextService = remember(runner) {
        runner?.let { activeRunner ->
            DesktopDebugContextService(activeRunner, executedCyclesProvider = { executedCycles })
        }
    }
    var showMcpServerWindow by remember { mutableStateOf(false) }
    var mcpPort by remember { mutableStateOf(DesktopMcpServer.DEFAULT_PORT.toString()) }
    var mcpServerInfo by remember { mutableStateOf<DesktopMcpServerInfo?>(null) }
    var mcpServerError by remember { mutableStateOf<String?>(null) }
    var mcpLastRequest by remember { mutableStateOf<DesktopMcpRequestSummary?>(null) }
    val debugContextServiceReference = remember { AtomicReference<com.digihori.pgp.desktop.application.debug.DebugContextService?>() }
    SideEffect { debugContextServiceReference.set(debugContextService) }
    val mcpServer = remember {
        DesktopMcpServer(
            adapter = PgpMcpToolAdapter(
                serviceProvider = debugContextServiceReference::get,
                applicationDispatcher = AwtMcpApplicationDispatcher,
            ),
            onRequest = { request -> EventQueue.invokeLater { mcpLastRequest = request } },
        )
    }
    DisposableEffect(mcpServer) {
        onDispose { mcpServer.stop() }
    }
    var focusRestoreRequest by remember { mutableLongStateOf(0L) }
    var projectWorkspace by remember { mutableStateOf<DesktopProjectWorkspace?>(null) }
    var projectChangeTracker by remember { mutableStateOf<DesktopProjectChangeTracker?>(null) }
    var projectTree by remember { mutableStateOf<DesktopProjectTreeSnapshot?>(null) }
    var loadedProgramLabel by remember { mutableStateOf<String?>(null) }
    var loadedProjectManifest by remember { mutableStateOf<String?>(null) }
    var externalEditorCommand by remember {
        mutableStateOf(DesktopUiPreferences.externalEditorCommand() ?: System.getenv("PGP_EDITOR").orEmpty())
    }
    var executionSpeed by remember { mutableStateOf(DesktopUiPreferences.executionSpeed()) }
    SideEffect { runner?.setExecutionSpeed(executionSpeed) }
    var editorCommandDraft by remember { mutableStateOf(externalEditorCommand) }
    var editorSettingsError by remember { mutableStateOf<String?>(null) }
    var projectBasicBaseline by remember { mutableStateOf<ByteArray?>(null) }
    var projectBasicModified by remember { mutableStateOf(false) }
    var changedProjectSources by remember { mutableStateOf(emptySet<String>()) }
    var changedAssemblySources by remember { mutableStateOf(emptySet<String>()) }
    var projectRuntimeStatus by remember { mutableStateOf(ProjectRuntimeStatus.NOT_BUILT) }
    var showAssemblyWorkspace by remember { mutableStateOf(false) }
    var showWavTool by remember { mutableStateOf(false) }
    var showCharacterEditor by remember { mutableStateOf(false) }
    var closeWavToolRequested by remember { mutableStateOf(false) }
    var wavToolInput by remember { mutableStateOf<WavToolInput?>(null) }
    var assemblyPreview by remember { mutableStateOf(emptyList<DesktopAssemblySourceResult>()) }
    var assemblyDiagnostics by remember { mutableStateOf(emptyList<DesktopAssemblyDiagnostic>()) }
    var assemblyStatus by remember { mutableStateOf(uiText("assembly.notAssembled")) }
    var showRenameProjectDialog by remember { mutableStateOf(false) }
    var renamedProjectName by remember { mutableStateOf("") }
    var renameProjectError by remember { mutableStateOf<String?>(null) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("New PGP Project") }
    var newProjectStarters by remember { mutableStateOf(setOf(DesktopProjectStarter.BASIC)) }
    var newProjectError by remember { mutableStateOf<String?>(null) }
    var requestProjectParentSelection by remember { mutableStateOf(false) }
    var machineMenuExpanded by remember { mutableStateOf(false) }
    var showAboutWindow by remember { mutableStateOf(false) }
    var showSettingsWindow by remember { mutableStateOf(false) }
    val gamepad = remember { DesktopGamepadController() }
    var mainWindowFocused by remember { mutableStateOf(ownerWindow.isFocused) }
    DisposableEffect(ownerWindow, gamepad) {
        val listener = object : WindowAdapter() {
            override fun windowGainedFocus(event: WindowEvent?) { mainWindowFocused = true }
            override fun windowLostFocus(event: WindowEvent?) {
                mainWindowFocused = false
                gamepad.setContext(runner, false)
            }
        }
        ownerWindow.addWindowFocusListener(listener)
        onDispose {
            gamepad.setContext(null, false)
            ownerWindow.removeWindowFocusListener(listener)
        }
    }
    SideEffect {
        gamepad.setContext(runner, mainWindowFocused && keyboardInput.isEnabled && !showSettingsWindow && !showCreateRomSetWindow && !showCreateProjectDialog && !showRenameProjectDialog)
    }
    LaunchedEffect(gamepad) {
        try {
            while (isActive) {
                gamepad.setContext(runner, mainWindowFocused && keyboardInput.isEnabled && !showSettingsWindow && !showCreateRomSetWindow && !showCreateProjectDialog && !showRenameProjectDialog)
                try {
                    gamepad.poll()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    gamepad.unavailable(failure)
                    delay(2_000)
                } catch (failure: LinkageError) {
                    gamepad.unavailable(failure)
                    break
                }
                delay(8)
            }
        } finally {
            withContext(NonCancellable) { runCatching { gamepad.close() } }
        }
    }

    var showHelpWindow by remember { mutableStateOf(false) }
    var projectPaneWidth by remember {
        mutableStateOf(
            DesktopUiPreferences.projectPaneWidth(DEFAULT_PROJECT_PANE_WIDTH.value)
                .dp.coerceIn(MIN_PROJECT_PANE_WIDTH, MAX_PROJECT_PANE_WIDTH),
        )
    }
    var selectedProjectSourcePath by remember { mutableStateOf<String?>(null) }
    var hostKeyInputMode by remember { mutableStateOf(DesktopUiPreferences.hostKeyInputMode()) }
    SideEffect { keyboardInput.setInputMode(hostKeyInputMode) }
    val audioPlayer = remember { DesktopAudioPlayer() }
    val romHistory = remember { DesktopRomHistory() }
    val romLibrary = remember { DesktopRomLibrary() }
    var installedRomMachines by remember { mutableStateOf(romLibrary.installedMachineIds()) }
    val appFocusRequester = remember { FocusRequester() }

    fun selectAndRestoreFocus(
        restoreMainWindowFocus: Boolean = true,
        select: () -> File?,
    ): File? = try {
        select()
    } finally {
        if (restoreMainWindowFocus) focusRestoreRequest++
    }

    fun showError(value: String) {
        message = value
        errorDialogMessage = value
    }

    fun loadRom(
        file: File,
        startAutomatically: Boolean,
        reportErrors: Boolean = true,
        memoryMode: Pc1251FamilyMemoryMode = familyMemoryMode,
        machineIdOverride: MachineId? = null,
    ): Boolean {
        val configuration = EmulatorConfiguration(pc1251FamilyMemoryMode = memoryMode)
        val result = runCatching {
            val bytes = file.readBytes()
            if (file.extension.equals("pgrom", ignoreCase = true)) {
                if (machineIdOverride == null) {
                    DesktopRomLoader.loadPackage(bytes, configuration)
                } else {
                    DesktopRomLoader.loadPackageForMachine(bytes, machineIdOverride, configuration)
                }
            } else {
                val bankImage = if (MachineCatalog.require(selectedMachineId).memoryBanking is com.digihori.pgp.core.api.MachineMemoryBanking.Banked) {
                    File(file.parentFile, "${selectedMachineId.value.replace("-", "").removeSuffix("u")}bank.bin").readBytes()
                } else null
                DesktopRomLoader.loadLegacyImage(selectedMachineId, bytes, configuration, bankImage)
            }
        }
            .getOrElse {
                if (reportErrors) showError(uiText("rom.readFailed", it.message ?: it::class.simpleName))
                return false
        }
        when (result) {
            is DesktopRomLoadResult.Success -> {
                val bootResult = if (startAutomatically) {
                    result.session.runCycles(AUTOMATIC_ROM_BOOT_CYCLES)
                } else {
                    null
                }
                if (bootResult?.status is ExecutionStatus.Faulted) {
                    if (reportErrors) showError((bootResult.status as ExecutionStatus.Faulted).fault.message())
                    return false
                }
                runner?.pause()
                audioPlayer.stop()
                val newRunner = DesktopEmulatorRunner(result.session).apply { setExecutionSpeed(executionSpeed) }
                selectedMachineId = result.session.machineId
                runner = newRunner
                keyboardInput.attach(newRunner)
                loadedRomName = file.name
                loadedRomFile = file.absoluteFile
                executedCycles = bootResult?.executedCycles ?: 0L
                display = newRunner.displaySnapshot()
                cpu = newRunner.cpuSnapshot()
                resetDebuggerComparison()
                debuggerStopReason = newRunner.stopReason
                operatingMode = OperatingMode.RUN
                requestedToneHz = 0
                romHistory.remember(file, result.session.machineId)
                if (startAutomatically) newRunner.run()
                runnerState = newRunner.state
                message = if (startAutomatically) {
                    uiText("rom.loadedRunning")
                } else {
                    uiText("rom.loaded")
                }
                loadedProgramLabel = null
                loadedProjectManifest = null
                if (projectWorkspace != null) projectRuntimeStatus = ProjectRuntimeStatus.NOT_BUILT
                projectBasicBaseline = null
                projectBasicModified = false
                return true
            }
            is DesktopRomLoadResult.Failure -> {
                if (reportErrors) showError(result.error.message())
                return false
            }
        }
    }

    fun importRom(file: File, targetMachineId: MachineId, reportErrors: Boolean = true): Boolean {
        val packageBytes = runCatching {
            val source = file.readBytes()
            if (file.extension.equals("pgrom", ignoreCase = true)) {
                source
            } else {
                val bankImage = if (MachineCatalog.require(targetMachineId).memoryBanking is com.digihori.pgp.core.api.MachineMemoryBanking.Banked) {
                    File(file.parentFile, "${targetMachineId.value.replace("-", "").removeSuffix("u")}bank.bin").readBytes()
                } else null
                when (val converted = DesktopRomPackageConverter.convertLegacyImage(targetMachineId, source, bankImage)) {
                    is DesktopRomPackageConversionResult.Success -> converted.packageBytes
                    is DesktopRomPackageConversionResult.Failure ->
                        error(DesktopRomLoadError.InvalidRom(converted.error).message())
                }
            }
        }.getOrElse {
            if (reportErrors) showError(uiText("rom.importFailed", it.message ?: it::class.simpleName))
            return false
        }
        return when (val installed = romLibrary.install(packageBytes, file.name)) {
            is DesktopRomLibraryResult.Failure -> {
                if (reportErrors) showError(uiText("rom.storeFailed", installed.error?.let { StudioStrings.system.romLoadError(it) } ?: installed.message))
                false
            }
            is DesktopRomLibraryResult.Success -> {
                installedRomMachines = romLibrary.installedMachineIds()
                val targetMachine = if (
                    MachineCatalog.require(installed.machineId).family ==
                    MachineCatalog.require(targetMachineId).family
                ) targetMachineId else installed.machineId
                selectedMachineId = targetMachine
                loadRom(
                    installed.file,
                    startAutomatically = true,
                    reportErrors = reportErrors,
                    machineIdOverride = targetMachine,
                )
            }
        }
    }

    fun chooseAndImportRom(machineId: MachineId) {
        selectAndRestoreFocus { selectRomFile(ownerWindow, machineId) }
            ?.let { importRom(it, machineId) }
    }

    fun switchMachine(machineId: MachineId) {
        if (machineId !in installedRomMachines) return
        selectedMachineId = machineId
        loadRom(romLibrary.packageFile(machineId), startAutomatically = true, machineIdOverride = machineId)
    }

    fun removeStoredRom(machineId: MachineId) {
        if (!romLibrary.remove(machineId)) {
            showError(uiText("rom.removeFailed", machineId.displayName()))
            return
        }
        installedRomMachines = romLibrary.installedMachineIds()
        if (runner?.machineId?.let(romLibrary::packageFile) == romLibrary.packageFile(machineId)) {
            runner?.pause()
            audioPlayer.stop()
            keyboardInput.attach(null)
            runner = null
            display = null
            cpu = null
            resetDebuggerComparison()
            loadedRomName = null
            loadedRomFile = null
            runnerState = RunnerState.PAUSED
        }
        message = uiText("rom.removed", machineId.displayName())
    }

    fun openProject(file: File, resetBasicTracking: Boolean = true) {
        when (val opened = DesktopProjectWorkspaceLoader.open(file)) {
            is DesktopProjectOpenResult.Failure -> showError(opened.errors.joinToString("\n") { it.message() })
            is DesktopProjectOpenResult.Success -> {
                if (projectWorkspace?.manifestFile?.absolutePath != opened.workspace.manifestFile.absolutePath) {
                    selectedProjectSourcePath = null
                    projectBasicBaseline = null
                    projectBasicModified = false
                }
                projectWorkspace = opened.workspace
                projectChangeTracker = DesktopProjectChangeTracker(opened.workspace)
                projectTree = DesktopProjectTreeScanner.scan(opened.workspace)
                changedProjectSources = emptySet()
                changedAssemblySources = emptySet()
                assemblyPreview = emptyList()
                assemblyDiagnostics = emptyList()
                assemblyStatus = uiText("assembly.notAssembled")
                projectRuntimeStatus = ProjectRuntimeStatus.NOT_BUILT
                if (resetBasicTracking) {
                    projectBasicBaseline = null
                    projectBasicModified = false
                }
                selectedMachineId = opened.workspace.definition.machineId
                val sourceSummary = opened.workspace.sources
                    .groupingBy { it.definition.type }
                    .eachCount()
                    .entries
                    .joinToString { "${it.key.displayName()}: ${it.value}" }
                val projectMachineId = opened.workspace.definition.machineId
                if (projectMachineId in installedRomMachines && runner?.machineId != projectMachineId) {
                    switchMachine(projectMachineId)
                }
                message = if (projectMachineId in installedRomMachines) {
                    uiText("project.opened" , (opened.workspace.definition.name), (sourceSummary))
                } else {
                    uiText("project.openedNeedsRom" , (opened.workspace.definition.name), (sourceSummary), (projectMachineId.displayName()))
                }
            }
        }
    }

    fun closeProject() {
        projectWorkspace = null
        projectChangeTracker = null
        projectTree = null
        selectedProjectSourcePath = null
        projectBasicBaseline = null
        projectBasicModified = false
        changedProjectSources = emptySet()
        changedAssemblySources = emptySet()
        assemblyPreview = emptyList()
        assemblyDiagnostics = emptyList()
        assemblyStatus = uiText("assembly.notAssembled")
        projectRuntimeStatus = ProjectRuntimeStatus.NOT_BUILT
        showAssemblyWorkspace = false
        message = uiText("project.closed")
    }

    fun markStandalone(file: File, additional: Boolean = false) {
        loadedProgramLabel = uiText(if (additional) "transfer.standaloneAdditional" else "transfer.standalone", file.name)
        loadedProjectManifest = null
        projectBasicBaseline = null
        projectBasicModified = false
        if (projectWorkspace != null) projectRuntimeStatus = ProjectRuntimeStatus.NOT_LOADED
    }

    fun addProjectSource() {
        val workspace = projectWorkspace ?: return
        val file = selectAndRestoreFocus {
            selectFile(ownerWindow, uiText("project.addFile"), "*.*")
        } ?: return
        DesktopProjectEditor.addSource(workspace, file).fold(
            onSuccess = { updated ->
                projectWorkspace = updated
                projectChangeTracker = DesktopProjectChangeTracker(updated)
                projectTree = DesktopProjectTreeScanner.scan(updated)
                selectedProjectSourcePath = updated.sources.last().definition.path
                projectRuntimeStatus = ProjectRuntimeStatus.NOT_BUILT
                message = uiText("project.added" , (updated.sources.last().definition.path))
            },
            onFailure = { showError(uiText("project.addFailed" , StudioStrings.system.projectOperationError(it.message.orEmpty()))) },
        )
    }

    fun updateProject() {
        val workspace = projectWorkspace ?: return
        val tree = DesktopProjectTreeScanner.scan(workspace)
        when (val result = DesktopProjectManifestUpdater.synchronize(workspace, tree)) {
            is DesktopProjectUpdateResult.Success -> {
                openProject(workspace.manifestFile, resetBasicTracking = false)
                val skipped = if (result.skippedPaths.isEmpty()) "" else {
                    uiText("project.skipped", result.skippedPaths.joinToString())
                }
                message = uiText("project.updated", result.addedCount.toString(), result.removedCount.toString(), skipped)
            }
            is DesktopProjectUpdateResult.CouldNotWrite -> showError(
                uiText("project.updateFailed" , (result.message)),
            )
        }
    }

    fun projectMemoryLayout(workspace: DesktopProjectWorkspace): MachineMemoryLayout =
        runner?.takeIf { it.machineId == workspace.definition.machineId }?.memoryLayout
            ?: MachineMemoryLayout(workspace.definition.machineId)

    suspend fun assembleWorkspaceWork() {
        val workspace = projectWorkspace ?: return
        val result = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, projectMemoryLayout(workspace), DesktopUiPreferences.assemblerSettings())
        check(projectWorkspace === workspace) { uiText("assembly.projectChanged") }
        when (result) {
            is DesktopAssemblyWorkspaceResult.Failure -> {
                assemblyDiagnostics = result.diagnostics
                assemblyStatus = uiText("assembly.failedPreview")
                message = uiText("assembly.diagnosticsCount" , (result.diagnostics.size).toString())
            }
            is DesktopAssemblyWorkspaceResult.Success -> {
                assemblyPreview = result.sources
                assemblyDiagnostics = emptyList()
                changedAssemblySources = emptySet()
                val bytes = result.sources.sumOf { it.image.byteCount }
                assemblyStatus = uiText("assembly.previewReady" , (bytes).toString(), (result.sources.size).toString())
                message = assemblyStatus
            }
        }
    }

    fun openExternalSource(file: File, line: Int? = null) {
        when (val opened = DesktopExternalEditor.open(file, line)) {
            DesktopExternalEditorResult.Opened -> message = uiText("editor.opened", file.name)
            is DesktopExternalEditorResult.Failed -> showError(
                uiText("editor.openFailed", StudioStrings.system.editorError(opened.message)),
            )
        }
    }

    suspend fun buildProjectOutputsWork(): DesktopProjectBuildResult.Success? {
        val workspace = projectWorkspace ?: return null
        val layout = projectMemoryLayout(workspace)
        val assembly = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, DesktopUiPreferences.assemblerSettings())
        val built = withContext(Dispatchers.IO) { DesktopProjectBuilder.build(workspace, layout, compiledAssembly = assembly) }
        check(projectWorkspace === workspace) { uiText("assembly.buildProjectChanged") }
        return when (built) {
            is DesktopProjectBuildResult.Failure -> {
                projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                assemblyStatus = uiText("assembly.buildFailedNoLoad")
                when (assembly) {
                    is DesktopAssemblyWorkspaceResult.Failure -> {
                        assemblyDiagnostics = assembly.diagnostics
                        assemblyStatus = uiText("assembly.buildFailedPreview")
                    }
                    is DesktopAssemblyWorkspaceResult.Success -> Unit
                }
                val machineName = runner?.machineId?.displayName() ?: workspace.definition.machineId.displayName()
                showError(built.errors.joinToString("\n") { it.message(machineName) })
                null
            }
            is DesktopProjectBuildResult.Success -> {
                val output = runCatching { withContext(Dispatchers.IO) { DesktopProjectBuildOutputWriter.write(workspace, built.artifact) } }
                    .getOrElse {
                        if (it is CancellationException) throw it
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        assemblyStatus = uiText("assembly.saveFailedNoLoad")
                        showError(uiText("assembly.saveFailed" , (it.message ?: it::class.simpleName).toString()))
                        return null
                    }
                check(projectWorkspace === workspace) { uiText("assembly.changedWhileSaving") }
                assemblyPreview = built.artifact.assemblySources
                assemblyDiagnostics = emptyList()
                changedAssemblySources = emptySet()
                assemblyStatus = if (output == null) uiText("assembly.built") else uiText("assembly.builtPath", output.path)
                projectRuntimeStatus = ProjectRuntimeStatus.BUILT_NOT_LOADED
                message = assemblyStatus
                built
            }
        }
    }

    suspend fun buildProjectWork() {
        val workspace = projectWorkspace ?: return
        val activeRunner = runner ?: return
        if (activeRunner.machineId != workspace.definition.machineId) {
            projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
            assemblyStatus = uiText("assembly.machineMismatchStatus")
            showError(
                uiText("project.machineMismatch" , (workspace.definition.machineId.displayName()), (activeRunner.machineId.displayName())),
            )
            return
        }
        buildProjectOutputsWork()?.let { built ->
                check(runner === activeRunner && projectWorkspace === workspace) { uiText("assembly.changedBeforeLoad") }
                when (val applied = DesktopProjectArtifactLoader.load(activeRunner, built.artifact)) {
                    is DesktopProjectApplyResult.BasicFailure -> {
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        assemblyStatus = uiText("assembly.loadFailed" , (applied.error.message()))
                        showError(applied.error.message())
                    }
                    is DesktopProjectApplyResult.MemoryFailure -> {
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        assemblyStatus = uiText("assembly.loadFailed" , (applied.error.message()))
                        showError(applied.error.message())
                    }
                    is DesktopProjectApplyResult.Success -> {
                        loadedProgramLabel = uiText("project.programLabel" , (workspace.definition.name))
                        loadedProjectManifest = workspace.manifestFile.absolutePath
                        projectRuntimeStatus = ProjectRuntimeStatus.UP_TO_DATE
                        changedProjectSources = emptySet()
                        projectChangeTracker = DesktopProjectChangeTracker(workspace)
                        val summary = uiText("assembly.loadedSummary" , (applied.basicByteCount).toString(), (applied.memoryByteCount).toString(), (applied.memorySegmentCount).toString())
                        assemblyStatus = uiText("assembly.loaded" , (activeRunner.machineId.displayName()), (summary))
                        message = uiText("assembly.startHelp" , (assemblyStatus))
                        runnerState = activeRunner.state
                        display = activeRunner.displaySnapshot()
                        cpu = activeRunner.cpuSnapshot()
                        resetDebuggerComparison()
                        requestedToneHz = activeRunner.audioSnapshot().frequencyHz
                        projectBasicBaseline = if (built.artifact.copyBasicProgram() != null) {
                            (activeRunner.basicProgramSnapshot() as? BasicProgramSnapshotResult.Success)
                                ?.copyBytes()
                        } else {
                            null
                        }
                        projectBasicModified = false
                    }
                }
        }
    }

    val assemblyScope = androidx.compose.runtime.rememberCoroutineScope()
    var assemblyBusy by remember { mutableStateOf(false) }
    var assemblyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun startAssembly(action: suspend () -> Unit) {
        if (assemblyBusy) return
        assemblyBusy = true
        assemblyStatus = uiText("assembly.busy")
        message = assemblyStatus
        assemblyJob = assemblyScope.launch {
            try { action() }
            catch (cancelled: CancellationException) {
                assemblyStatus = uiText("assembly.cancelledNoLoad")
                message = assemblyStatus
                throw cancelled
            } catch (error: Exception) { showError(uiText("assembly.failureDetail", StudioStrings.system.projectOperationError(error.message ?: error::class.simpleName.orEmpty()))) }
            finally { assemblyBusy = false }
        }
    }
    fun assembleWorkspace() = startAssembly { assembleWorkspaceWork() }
    fun buildProjectOutputs() = startAssembly { buildProjectOutputsWork() }
    fun buildProject() = startAssembly { buildProjectWork() }

    fun openProjectFromDisk() {
        selectAndRestoreFocus(restoreMainWindowFocus = false) { selectProjectDirectory(ownerWindow) }
            ?.let { File(it, DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME) }
            ?.let { openProject(it) }
    }

    fun loadBasicFromDisk() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, uiText("basic.loadTitle")) } ?: return
        val compiled = runCatching {
            DesktopBasicLoader.compileProgram(file.readBytes(), activeRunner.machineId)
        }.getOrElse {
            showError(uiText("basic.readFailed", (it.message ?: it::class.simpleName).toString()))
            return
        }
        when (compiled) {
            is DesktopBasicProgramCompileResult.Failure ->
                showError(compiled.error.message(activeRunner.machineId.displayName()))
            is DesktopBasicProgramCompileResult.Success -> when (val loaded = activeRunner.loadBasicProgram(compiled.bytes)) {
                is BasicProgramLoadResult.Failure -> {
                    runnerState = activeRunner.state
                    showError(loaded.error.message())
                }
                is BasicProgramLoadResult.Success -> {
                    markStandalone(file)
                    runnerState = activeRunner.state
                    display = activeRunner.displaySnapshot()
                    cpu = activeRunner.cpuSnapshot()
                    resetDebuggerComparison()
                    message = uiText("basic.loadedDirect", (file.name).toString(), (loaded.size).toString(), (loaded.startAddress.hex(4)), (loaded.endAddress.hex(4)))
                }
            }
        }
    }

    fun loadMachineCodeFromDisk() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectMemoryDumpFile(ownerWindow) } ?: return
        val parsed = runCatching { DesktopMemoryDumpLoader.parse(file.readBytes()) }.getOrElse {
            showError(uiText("memory.readFailed", (it.message ?: it::class.simpleName).toString()))
            return
        }
        when (parsed) {
            is DesktopMemoryDumpLoadResult.Failure -> showError(parsed.error.message())
            is DesktopMemoryDumpLoadResult.Success -> {
                when (val loaded = activeRunner.loadMemoryImage(parsed.image)) {
                    is MemoryImageLoadResult.Failure -> showError(loaded.error.message())
                    is MemoryImageLoadResult.Success -> {
                        message = uiText("memory.loaded", (file.name).toString(), (loaded.byteCount).toString(), (loaded.segmentCount).toString())
                        markStandalone(file, additional = true)
                    }
                }
                runnerState = activeRunner.state
                display = activeRunner.displaySnapshot()
                cpu = activeRunner.cpuSnapshot()
                resetDebuggerComparison()
            }
        }
    }

    fun saveBasicToDisk() {
        val activeRunner = runner ?: return
        when (val snapshot = activeRunner.basicProgramSnapshot()) {
            is BasicProgramSnapshotResult.Failure -> showError(snapshot.error.message())
            is BasicProgramSnapshotResult.Success -> when (
                val decoded = DesktopBasicLoader.detokenizeProgram(snapshot.copyBytes(), activeRunner.machineId)
            ) {
                is DesktopBasicProgramDecodeResult.Failure -> showError(
                    uiText("basic.decodeFailure", (decoded.error.offset).toString(), StudioStrings.system.transferDetail(decoded.error.message)),
                )
                is DesktopBasicProgramDecodeResult.GenericFailure -> showError(
                    uiText("basic.genericDecodeFailure", (decoded.offset).toString(), StudioStrings.system.transferDetail(decoded.message)),
                )
                is DesktopBasicProgramDecodeResult.Success -> {
                    val file = selectAndRestoreFocus { selectBasicSaveFile(ownerWindow) } ?: return
                    runCatching { file.writeBytes(decoded.utf8Bytes) }
                        .onSuccess { message = uiText("basic.saved", (file.name).toString()) }
                        .onFailure { showError(uiText("basic.saveFailed", (it.message ?: it::class.simpleName).toString())) }
                }
            }
        }
    }

    fun typeBasicThroughRom() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, uiText("basic.typeTitle")) } ?: return
        val loadResult = runCatching {
            DesktopBasicLoader.compileRomInput(file.readBytes(), activeRunner.machineId)
        }.getOrElse {
            showError(uiText("basic.readFailed", (it.message ?: it::class.simpleName).toString()))
            return
        }
        when (loadResult) {
            is DesktopBasicLoadResult.Failure -> showError(loadResult.error.message(activeRunner.machineId.displayName()))
            is DesktopBasicLoadResult.Success -> {
                activeRunner.setOperatingMode(OperatingMode.PROGRAM)
                operatingMode = OperatingMode.PROGRAM
                val runResult = activeRunner.runKeySequenceImmediately(loadResult.keys)
                executedCycles += runResult.executedCycles
                runnerState = activeRunner.state
                display = activeRunner.displaySnapshot()
                cpu = activeRunner.cpuSnapshot()
                resetDebuggerComparison()
                requestedToneHz = activeRunner.audioSnapshot().frequencyHz
                message = if (runResult.status is ExecutionStatus.Faulted) {
                    uiText("basic.loadingStopped", (runResult.status as ExecutionStatus.Faulted).fault.message())
                } else {
                    uiText("basic.merged", (file.name).toString(), (activeRunner.machineId.displayName()), (loadResult.keys.size).toString())
                }
                if (runResult.status !is ExecutionStatus.Faulted) {
                    markStandalone(file, additional = true)
                }
            }
        }
    }

    suspend fun quickAssembleWork(sourceFile: File) {
        val settings = DesktopUiPreferences.assemblerSettings()
        if (settings.external) {
            val layout = runner?.memoryLayout ?: MachineMemoryLayout(selectedMachineId)
            when (val result = com.digihori.pgp.desktop.project.DesktopSc61Assembler.assemble(settings, sourceFile, layout)) {
                is com.digihori.pgp.desktop.project.DesktopSc61AssemblyResult.Failure -> showError(uiText("quick.externalFailed", StudioStrings.system.projectOperationError(result.message)) + "\n" + result.log)
                is com.digihori.pgp.desktop.project.DesktopSc61AssemblyResult.Success -> {
                    val destination = selectAndRestoreFocus { selectAssembledDumpDestination(ownerWindow) } ?: return
                    withContext(Dispatchers.IO) { destination.writeBytes(DesktopMemoryDumpWriter.write(result.image)) }
                    message = uiText("quick.externalSaved", (result.image.byteCount).toString(), (destination.name).toString(), (result.log))
                }
            }
            return
        }
        val assembled = runCatching { withContext(Dispatchers.IO) { Sc61860Assembler.assemble(sourceFile.readText()) } }.getOrElse {
            showError(uiText("quick.readFailed", (it.message ?: it::class.simpleName).toString()))
            return
        }
        when (assembled) {
            is Sc61860AssemblyResult.Failure -> showError(
                uiText("quick.error", (assembled.line).toString(), StudioStrings.system.transferDetail(assembled.message)),
            )
            is Sc61860AssemblyResult.Success -> {
                val destination = selectAndRestoreFocus { selectAssembledDumpDestination(ownerWindow) } ?: return
                runCatching { destination.writeBytes(DesktopMemoryDumpWriter.write(assembled.image)) }
                    .onSuccess { message = uiText("quick.saved", (assembled.image.byteCount).toString(), (destination.name).toString()) }
                    .onFailure { showError(uiText("quick.saveFailed", (it.message ?: it::class.simpleName).toString())) }
            }
        }
    }

    fun quickAssemble() {
        if (assemblyBusy) return
        val source = selectAndRestoreFocus { selectAssemblyFile(ownerWindow) } ?: return
        startAssembly { quickAssembleWork(source) }
    }

    fun runEmulator() {
        runner?.let { debuggerComparison = comparisonTracker.running(it) }
        runner?.run()
        runnerState = runner?.state ?: RunnerState.PAUSED
        changedCpuFields = emptySet()
        debuggerStopReason = runner?.stopReason
        message = uiText("main.running", executionSpeed.uiLabel())
    }

    fun pauseEmulator() {
        val wasRunning = runner?.state == RunnerState.RUNNING
        runner?.pause()
        audioPlayer.stop()
        runnerState = runner?.state ?: RunnerState.PAUSED
        cpu = runner?.cpuSnapshot()
        if (wasRunning) runner?.let { debuggerComparison = comparisonTracker.stopped(it) }
        changedCpuFields = CpuSnapshotDifference.changed(debuggerComparison.previousCpu, cpu)
        debuggerStopReason = runner?.stopReason
        message = uiText("main.paused")
    }

    fun resetEmulator() {
        loadedProgramLabel = null
        loadedProjectManifest = null
        if (projectWorkspace != null) projectRuntimeStatus = ProjectRuntimeStatus.NOT_LOADED
        runner?.reset()
        audioPlayer.stop()
        runnerState = runner?.state ?: RunnerState.PAUSED
        executedCycles = 0L
        display = runner?.displaySnapshot()
        cpu = runner?.cpuSnapshot()
        resetDebuggerComparison()
        debuggerStopReason = runner?.stopReason
        operatingMode = OperatingMode.RUN
        requestedToneHz = 0
        projectBasicBaseline = null
        projectBasicModified = false
        message = uiText("main.resetComplete")
    }

    fun stepEmulator() {
        val before = runner?.cpuSnapshot()
        runner?.let(comparisonTracker::preparingStep)
        val result = runner?.step() ?: return
        runnerState = runner?.state ?: RunnerState.PAUSED
        executedCycles += result.cycles
        display = runner?.displaySnapshot()
        val after = runner?.cpuSnapshot()
        changedCpuFields = CpuSnapshotDifference.changed(before, after)
        runner?.let { debuggerComparison = comparisonTracker.stepped(it) }
        cpu = after
        debuggerStopReason = runner?.stopReason
        requestedToneHz = runner?.audioSnapshot()?.frequencyHz ?: 0
        message = uiText("main.stepped", result.cycles.toString())
    }

    fun setMode(mode: OperatingMode) {
        runner?.setOperatingMode(mode)
        operatingMode = mode
        display = runner?.displaySnapshot()
    }

    fun setMemoryMode(mode: Pc1251FamilyMemoryMode) {
        val file = loadedRomFile
        val resume = runnerState == RunnerState.RUNNING
        familyMemoryMode = mode
        if (file == null) {
            message = if (mode == Pc1251FamilyMemoryMode.EXPANDED) {
                uiText("main.expandedSelected")
            } else {
                uiText("main.hardwareSelected")
            }
        } else if (loadRom(file, resume, memoryMode = mode, machineIdOverride = runner?.machineId)) {
            message = if (mode == Pc1251FamilyMemoryMode.EXPANDED) {
                uiText("main.expandedRestart")
            } else {
                uiText("main.hardwareRestart", selectedMachineId.displayName())
            }
        }
    }

    DisposableEffect(audioPlayer) {
        onDispose(audioPlayer::close)
    }

    LaunchedEffect(Unit) {
        val previous = romHistory.lastSelection()
        if (previous != null && romLibrary.isInstalled(previous.machineId)) {
            selectedMachineId = previous.machineId
            if (loadRom(
                    romLibrary.packageFile(previous.machineId),
                    startAutomatically = true,
                    reportErrors = false,
                    machineIdOverride = previous.machineId,
                )
            ) {
                return@LaunchedEffect
            }
        }
        if (previous != null && previous.file.isFile) {
            selectedMachineId = previous.machineId
            if (importRom(previous.file, previous.machineId, reportErrors = false)) return@LaunchedEffect
        }
        val firstInstalled = MachineCatalog.definitions.firstOrNull { it.id in installedRomMachines }?.id
        if (firstInstalled != null) {
            selectedMachineId = firstInstalled
            if (loadRom(
                    romLibrary.packageFile(firstInstalled),
                    startAutomatically = true,
                    reportErrors = false,
                    machineIdOverride = firstInstalled,
                )
            ) {
                return@LaunchedEffect
            }
        }
        DesktopRomLocator.findFirstAvailableRom()?.let { fallback ->
            selectedMachineId = fallback.machineId
            importRom(fallback.file, fallback.machineId)
        }
    }

    LaunchedEffect(runner) {
        var cpuRefreshCounter = 0
        while (isActive) {
            // Compose's dispatcher can immediately resume yield() within the same event turn.
            // A real suspension lets AWT input and rendering run between unlimited CPU batches.
            delay(if (runner?.unlimited == true && runner?.state == RunnerState.RUNNING) 1L
                else FRAME_DELAY_MILLISECONDS)
            val activeRunner = runner ?: continue
            val stateBeforeTick = activeRunner.state
            if (stateBeforeTick == RunnerState.RUNNING && debuggerComparison.cpuBasis != DebuggerComparisonBasis.RUNNING) {
                debuggerComparison = comparisonTracker.running(activeRunner)
                changedCpuFields = emptySet()
            }
            val tick = activeRunner.tick()
            executedCycles += tick.runResult?.executedCycles ?: 0L
            runnerState = activeRunner.state
            debuggerStopReason = activeRunner.stopReason
            operatingMode = activeRunner.currentOperatingMode()
            val latestDisplay = activeRunner.displaySnapshot()
            if (display?.revision != latestDisplay.revision || display?.enabled != latestDisplay.enabled) {
                display = latestDisplay
            }
            if (++cpuRefreshCounter >= CPU_REFRESH_FRAME_INTERVAL ||
                (stateBeforeTick == RunnerState.RUNNING && activeRunner.state != RunnerState.RUNNING)) {
                cpu = activeRunner.cpuSnapshot()
                if (stateBeforeTick == RunnerState.RUNNING && activeRunner.state != RunnerState.RUNNING) {
                    debuggerComparison = comparisonTracker.stopped(activeRunner)
                    changedCpuFields = CpuSnapshotDifference.changed(debuggerComparison.previousCpu, cpu)
                }
                projectBasicBaseline?.let { baseline ->
                    projectBasicModified = when (val snapshot = activeRunner.basicProgramSnapshot()) {
                        is BasicProgramSnapshotResult.Success -> !snapshot.copyBytes().contentEquals(baseline)
                        is BasicProgramSnapshotResult.Failure -> true
                    }
                }
                cpuRefreshCounter = 0
            }
            val audio = activeRunner.audioSnapshot()
            requestedToneHz = audio.frequencyHz
            if (activeRunner.unlimited) {
                audioPlayer.stop()
                activeRunner.drainAudioSamples()
            } else if (activeRunner.state == RunnerState.RUNNING) {
                audioPlayer.write(activeRunner.drainAudioSamples()).onFailure {
                    message = uiText("main.audioUnavailable", it.message ?: it::class.simpleName)
                }
            } else {
                audioPlayer.stop()
                activeRunner.drainAudioSamples()
            }
            val status = tick.runResult?.status
            if (status is ExecutionStatus.Faulted) {
                audioPlayer.stop()
                message = status.fault.message()
                debuggerStopReason = activeRunner.stopReason
            }
        }
    }

    LaunchedEffect(projectChangeTracker) {
        val tracker = projectChangeTracker ?: return@LaunchedEffect
        while (isActive) {
            delay(PROJECT_SCAN_DELAY_MILLISECONDS)
            val changes = runCatching { withContext(Dispatchers.IO) { tracker.scan() } }.getOrElse {
                if (it is CancellationException) throw it
                message = uiText("project.scanFailed" , (it.message ?: it::class.simpleName).toString())
                continue
            }
            projectWorkspace?.let { workspace ->
                projectTree = runCatching {
                    withContext(Dispatchers.IO) { DesktopProjectTreeScanner.scan(workspace) }
                }.getOrElse {
                    if (it is CancellationException) throw it
                    message = uiText("project.treeFailed" , (it.message ?: it::class.simpleName).toString())
                    projectTree
                }
            }
            if (changes.isNotEmpty()) {
                changedProjectSources = changedProjectSources + changes.map { it.source.definition.id }
                changedAssemblySources = changedAssemblySources + changes
                    .filter { it.source.definition.type == ProjectSourceType.ASSEMBLY }
                    .map { it.source.definition.id }
                if (loadedProjectManifest == projectWorkspace?.manifestFile?.absolutePath &&
                    loadedProjectManifest != null) projectRuntimeStatus = ProjectRuntimeStatus.SOURCE_CHANGED
                message = uiText("project.externalChanges", changes.joinToString { change ->
                    uiText("project.changedPath", change.source.definition.path, uiText("project.change.${change.kind.name}"))
                })
            }
        }
    }

    LaunchedEffect(showCreateProjectDialog, showRenameProjectDialog) {
        keyboardInput.setEnabled(!showCreateProjectDialog && !showRenameProjectDialog)
    }

    LaunchedEffect(focusRestoreRequest) {
        if (focusRestoreRequest > 0L) {
            delay(50)
            ownerWindow.toFront()
            ownerWindow.requestFocus()
            appFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(requestProjectParentSelection) {
        if (!requestProjectParentSelection) return@LaunchedEffect
        // Let the Compose modal disappear before showing the native macOS directory dialog.
        delay(50)
        val destination = runCatching {
            selectAndRestoreFocus(restoreMainWindowFocus = false) { selectProjectParentDirectory(ownerWindow) }
        }.getOrElse { error ->
            requestProjectParentSelection = false
            newProjectError = uiText("project.chooseFolderFailed" , (error.message ?: error::class.simpleName).toString())
            showCreateProjectDialog = true
            return@LaunchedEffect
        }
        if (destination == null) {
            requestProjectParentSelection = false
            showCreateProjectDialog = true
            return@LaunchedEffect
        }
        when (val created = DesktopProjectWorkspaceCreator.create(
            destination,
            newProjectName,
            selectedMachineId,
            newProjectStarters,
        )) {
            is DesktopProjectCreateResult.Failure -> {
                newProjectError = created.error.message()
                showCreateProjectDialog = true
            }
            is DesktopProjectCreateResult.Success -> openProject(created.manifestFile)
        }
        requestProjectParentSelection = false
    }

    MenuBar {
        Menu(ProjectInfo.STUDIO_DISPLAY_NAME) {
            Item(
                uiText("about.menu", ProjectInfo.STUDIO_DISPLAY_NAME),
                enabled = !showAboutWindow,
                onClick = { showAboutWindow = true },
            )
            Item(uiText("action.settings"), enabled = !showSettingsWindow, onClick = { showSettingsWindow = true })
            Separator()
            Item(uiText("action.quit", ProjectInfo.STUDIO_DISPLAY_NAME), onClick = onQuit)
        }
        Menu(uiText("menu.file")) {
            Item(uiText("rom.register"), onClick = { showRomManager = true })
            Item(uiText("rom.manage"), onClick = { showRomManager = true })
            Item(uiText("rom.create"), enabled = !showCreateRomSetWindow, onClick = { showCreateRomSetWindow = true })
        }
        Menu(uiText("menu.project")) {
            Item(uiText("project.new"), onClick = {
                newProjectError = null
                showCreateProjectDialog = true
            })
            Item(uiText("project.open"), onClick = ::openProjectFromDisk)
            Item(uiText("project.rename"), enabled = projectWorkspace != null, onClick = {
                renamedProjectName = projectWorkspace?.definition?.name.orEmpty()
                renameProjectError = null
                showRenameProjectDialog = true
            })
            Item(uiText("project.addSource"), enabled = projectWorkspace != null, onClick = ::addProjectSource)
            Item(uiText("project.close"), enabled = projectWorkspace != null, onClick = ::closeProject)
            Item(
                uiText("assembly.open"),
                onClick = { showAssemblyWorkspace = true },
            )
            Separator()
            Item(
                uiText("project.buildLoad"),
                enabled = projectWorkspace != null && runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::buildProject,
            )
            Item(uiText("project.update"), enabled = projectWorkspace != null, onClick = ::updateProject)
        }
        Menu(uiText("menu.program")) {
            Item(
                uiText("basic.load"),
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::loadBasicFromDisk,
            )
            Item(
                uiText("basic.save"),
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::saveBasicToDisk,
            )
            Item(
                uiText("basic.type"),
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::typeBasicThroughRom,
            )
            Separator()
            Item(
                uiText("memory.load"),
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::loadMachineCodeFromDisk,
            )
            Item(uiText("memory.save"), enabled = runner != null, onClick = {
                dumpRangeError = null
                showSaveMemoryDumpDialog = true
            })
            Item(uiText("assembly.quick"), onClick = ::quickAssemble)
        }
        Menu(uiText("menu.tools")) {
            Item(
                uiText("character.title"),
                enabled = !showCharacterEditor,
                onClick = { showCharacterEditor = true },
            )
            Item(
                uiText("wav.title"),
                enabled = !showWavTool,
                onClick = { showWavTool = true },
            )
        }
        Menu(uiText("menu.emulator")) {
            Item(uiText("emulator.run"), enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::runEmulator)
            Item(uiText("emulator.pause"), enabled = runnerState == RunnerState.RUNNING, onClick = ::pauseEmulator)
            Item(uiText("emulator.step"), enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::stepEmulator)
            Item(uiText("emulator.reset"), enabled = runner != null, onClick = ::resetEmulator)
            Separator()
            Item(
                uiText("history.previous"),
                enabled = runner?.commandHistorySize()?.let { it > 0 } == true && operatingMode == OperatingMode.RUN,
                onClick = {
                    message = if (runner?.recallPreviousCommand() == true) {
                        uiText("history.recalledPrevious")
                    } else {
                        uiText("history.empty")
                    }
                },
            )
            Item(
                uiText("history.next"),
                enabled = runner?.commandHistorySize()?.let { it > 0 } == true && operatingMode == OperatingMode.RUN,
                onClick = {
                    message = if (runner?.recallNextCommand() == true) {
                        uiText("history.recalledNext")
                    } else {
                        uiText("history.noNewer")
                    }
                },
            )
            if (MachineCatalog.require(runner?.machineId ?: selectedMachineId).supportsConfigurableRam) {
                Separator()
                Item(
                    uiText("memory.expanded"),
                    enabled = familyMemoryMode != Pc1251FamilyMemoryMode.EXPANDED,
                    onClick = { setMemoryMode(Pc1251FamilyMemoryMode.EXPANDED) },
                )
                Item(
                    uiText("memory.hardware"),
                    enabled = familyMemoryMode != Pc1251FamilyMemoryMode.HARDWARE,
                    onClick = { setMemoryMode(Pc1251FamilyMemoryMode.HARDWARE) },
                )
            }
        }
        Menu(uiText("menu.debug")) {
            Item(uiText("debug.open"), enabled = !showDebuggerWindow, onClick = { showDebuggerWindow = true })
            Item(uiText("debug.saveCheckpoint"), enabled = runner != null && cpu != null, onClick = {
                checkpointRangeError = null
                showSaveCheckpointDialog = true
            })
            Item(uiText("debug.exportContext"), enabled = runner != null && cpu != null, onClick = {
                val activeRunner = runner ?: return@Item
                val ram = MachineCatalog.require(activeRunner.machineId).memoryRegions
                    .firstOrNull { it.kind == MachineMemoryRegionKind.RAM }
                ram?.let {
                    debugContextMemoryStart = it.startAddress.hex(4)
                    debugContextMemoryEnd = (it.startAddress + 0xff).coerceAtMost(it.endAddressInclusive).hex(4)
                }
                debugContextError = null
                debugContextPreview = null
                showDebugContextDialog = true
            })
            Separator()
            Item(uiText("mcp.title"), enabled = !showMcpServerWindow, onClick = { showMcpServerWindow = true })
        }
        Menu(uiText("menu.help")) {
            Item(uiText("help.contents"), enabled = !showHelpWindow, onClick = { showHelpWindow = true })
            Separator()
            Item(uiText("help.gettingStarted"), onClick = {
                openDocumentationUrl(GETTING_STARTED_URL)?.let { message = it }
            })
            Item(uiText("help.keyboard"), onClick = {
                openDocumentationUrl(PROJECTS_HELP_URL)?.let { message = it }
            })
            Item(uiText("help.debugger"), onClick = {
                openDocumentationUrl(DEBUGGER_HELP_URL)?.let { message = it }
            })
            Separator()
            Item(uiText("help.github"), onClick = {
                openDocumentationUrl(REPOSITORY_URL)?.let { message = it }
            })
        }
    }

    if (showAboutWindow) {
        StudioWindow(
            id = "about",
            onCloseRequest = { showAboutWindow = false },
            title = uiText("about.title", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        SelectionContainer {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(ProjectInfo.STUDIO_DISPLAY_NAME, style = MaterialTheme.typography.headlineMedium)
                                Text(uiText("about.version", ProjectInfo.VERSION))
                                Text(uiText("about.description"))
                                Text("Copyright © 2026 Y Horiuchi", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(onClick = {
                            openDocumentationUrl(REPOSITORY_URL)?.let { message = it }
                        }) { Text(uiText("about.github")) }
                    }
                }
            }
        }
    }

    if (showCreateRomSetWindow) {
        DesktopRomSetWindow(
            initialMachineId = selectedMachineId,
            onClose = { showCreateRomSetWindow = false },
            onRegister = { file, machine -> importRom(file, machine) },
        )
    }

    if (showSettingsWindow) {
        StudioWindow(
            id = "settings",
            onCloseRequest = { showSettingsWindow = false },
            title = uiText("settings.window", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudioScrollableColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = 20.dp,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(uiText("settings.title"), style = MaterialTheme.typography.headlineSmall)
                        SettingsSection(uiText("settings.language")) {
                            LanguageSettingsPanel(
                                selected = languagePreference,
                                onSelect = { preference ->
                                    runCatching { DesktopUiPreferences.saveLanguagePreference(preference) }
                                        .onSuccess { languagePreference = preference; languageSettingsError = null }
                                        .onFailure { languageSettingsError = uiText("settings.languageSaveFailed", it.message ?: it::class.simpleName) }
                                },
                                pendingRestart = languagePreference != StudioStrings.startupPreference,
                                error = languageSettingsError,
                            )
                        }
                        SettingsSection(uiText("settings.appearance")) {
                            Text(uiText("settings.theme"))
                            Text("INSTRUMENT", fontFamily = FontFamily.Monospace)
                            Text(
                                uiText("settings.themeHelp"),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        SettingsSection(uiText("settings.speed")) {
                            Text(uiText("settings.speedHelp"))
                            DesktopExecutionSpeed.entries.forEach { option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = executionSpeed == option,
                                        modifier = Modifier.semantics { contentDescription = option.uiLabel() },
                                        onClick = {
                                            executionSpeed = option
                                            runner?.setExecutionSpeed(option)
                                            DesktopUiPreferences.saveExecutionSpeed(option)
                                        },
                                    )
                                    Text(option.uiLabel())
                                }
                            }
                            Text(uiText("settings.speedNote"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        SettingsSection(uiText("settings.editor")) {
                            Text(uiText("settings.editorHelp"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    DesktopUiPreferences.saveExternalEditorCommand("")
                                    externalEditorCommand = ""
                                    editorCommandDraft = ""
                                    editorSettingsError = null
                                }) { Text(uiText("settings.osEditor")) }
                            }
                            Text(if (externalEditorCommand.isBlank()) uiText("settings.currentOsEditor")
                                else uiText("settings.currentEditor", externalEditorCommand), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = editorCommandDraft,
                                onValueChange = { editorCommandDraft = it; editorSettingsError = null },
                                label = { Text(uiText("settings.editorCommand")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(uiText("settings.editorExample"),
                                style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                val command = editorCommandDraft.trim()
                                val failure = if (command.isBlank()) null else
                                    runCatching { DesktopExternalEditor.splitCommand(command) }.exceptionOrNull()
                                if (failure != null) editorSettingsError = StudioStrings.system.editorError(failure.message ?: failure::class.simpleName.orEmpty()) else {
                                    DesktopUiPreferences.saveExternalEditorCommand(command)
                                    externalEditorCommand = command
                                    editorSettingsError = null
                                }
                            }) { Text(uiText("settings.applyEditor")) }
                            editorSettingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                        SettingsSection(uiText("settings.assembler")) {
                            com.digihori.pgp.desktop.project.DesktopAssemblerSettingsPanel(
                                projectWorkspace?.let(::projectMemoryLayout) ?: MachineMemoryLayout(selectedMachineId),
                            )
                        }
                        SettingsSection(uiText("settings.keyboard")) {
                            Text(uiText("settings.keyboardHelp"))
                            Text(uiText("settings.defHelp"))
                            Text(uiText("settings.historyHelp"), fontFamily = FontFamily.Monospace)
                        }
                        SettingsSection(uiText("settings.controller")) {
                            ControllerSettings(
                                settings = gamepad.settings,
                                devices = gamepad.devices,
                                selectedId = gamepad.selectedId,
                                pressed = gamepad.pressed,
                                allowedKeys = GamepadKeys.forMachine(runner?.machineId ?: selectedMachineId),
                                availableButtons = gamepad.available,
                                status = gamepad.status,
                                strings = StudioStrings.system.controllerStrings(),
                                onSelectDevice = gamepad::select,
                                onSettingsChange = gamepad::save,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpWindow) {
        StudioWindow(
            id = "help",
            onCloseRequest = { showHelpWindow = false },
            title = uiText("help.window", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudioScrollableColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = 20.dp,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(uiText("menu.help"), style = MaterialTheme.typography.headlineSmall)
                        Text(uiText("help.description"))
                        HelpLink(uiText("help.gettingStarted"), GETTING_STARTED_URL, onMessage = { message = it })
                        HelpLink(uiText("help.keyboard"), PROJECTS_HELP_URL, onMessage = { message = it })
                        HelpLink(uiText("help.debugger"), DEBUGGER_HELP_URL, onMessage = { message = it })
                        HelpLink(uiText("help.repository"), REPOSITORY_URL, onMessage = { message = it })
                    }
                }
            }
        }
    }

    if (showCharacterEditor) {
        StudioWindow(
            id = "character-editor",
            onCloseRequest = { showCharacterEditor = false },
            title = uiText("character.window", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CharacterEditorPanel()
                }
            }
        }
    }

    if (showWavTool) {
        StudioWindow(
            id = "old-wav",
            onCloseRequest = { closeWavToolRequested = true },
            title = uiText("wav.window", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WavToolPanel(window, wavToolInput, closeWavToolRequested,
                        onCloseHandled = { closeWavToolRequested = false },
                        onClose = { showWavTool = false },
                    )
                }
            }
        }
    }

    if (showDebuggerWindow) {
        StudioWindow(
            id = "debugger",
            onCloseRequest = { showDebuggerWindow = false },
            title = uiText("debug.window", (ProjectInfo.STUDIO_DISPLAY_NAME)),
        ) {
            DisposableEffect(runner) {
                runner?.setDebugObservationEnabled(true)
                onDispose { runner?.setDebugObservationEnabled(false) }
            }
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    key(runner) {
                        var memoryNavigationRevision by remember { mutableStateOf(0) }
                        var navigationRevision by remember(runner) { mutableStateOf(0) }
                        var disassemblyNavigation by remember(runner) { mutableStateOf<Pair<Int, Int?>?>(null) }
                        var navigateMemory by remember(runner) { mutableStateOf(false) }
                        fun navigateInstruction(address: Int?) {
                            navigateMemory = false
                            navigationRevision++
                            disassemblyNavigation = navigationRevision to address
                        }
                        fun navigateToMemory(address: Int) {
                            selectDebuggerMemory(address)
                            memoryNavigationRevision++
                            navigateMemory = true
                            navigationRevision++
                        }
                        DebuggerLayout(
                            navigationRevision = navigationRevision,
                            navigateMemory = navigateMemory,
                            header = {
                                DebuggerControls(
                                    hasSession = runner != null,
                                    state = runnerState,
                                    snapshot = cpu,
                                    stopReason = debuggerStopReason,
                                    onRun = ::runEmulator,
                                    onPause = ::pauseEmulator,
                                    onStep = ::stepEmulator,
                                    onFollowPc = { navigateInstruction(null) },
                                    onStopInstruction = { navigateInstruction(it) },
                                    onStopMemory = ::navigateToMemory,
                                )
                            },
                            execution = {
                                DebuggerSection {
                                    SelectionContainer {
                                        CpuRegisterPanel(cpu, changedCpuFields,
                                            previousSnapshot = debuggerComparison.previousCpu,
                                            comparisonBasis = debuggerComparison.cpuBasis)
                                    }
                                }
                                DebuggerSection {
                                    SelectionContainer {
                                        DisassemblyPanel(
                                            runner = runner,
                                            snapshot = cpu,
                                            keyboardInput = keyboardInput,
                                            navigation = disassemblyNavigation,
                                            onSaveRange = {
                                                disassemblyStartAddress = (cpu?.programCounter ?: 0).hex(4)
                                                disassemblyEndAddress = ((cpu?.programCounter ?: 0) + 0xff)
                                                    .coerceAtMost(0xffff).hex(4)
                                                disassemblyRangeError = null
                                                showSaveDisassemblyDialog = true
                                            },
                                        )
                                    }
                                }
                                DebuggerSection { SelectionContainer { InstructionTracePanel(runner, executedCycles, keyboardInput) } }
                                DebuggerSection {
                                    Button(
                                        enabled = runner != null && cpu != null,
                                        onClick = {
                                            checkpointRangeError = null
                                            showSaveCheckpointDialog = true
                                        },
                                    ) { Text(uiText("debug.checkpointTitle")) }
                                    Button(
                                        enabled = runner != null && cpu != null,
                                        onClick = {
                                            val activeRunner = runner ?: return@Button
                                            val ram = MachineCatalog.require(activeRunner.machineId).memoryRegions
                                                .firstOrNull { it.kind == MachineMemoryRegionKind.RAM }
                                            ram?.let {
                                                debugContextMemoryStart = it.startAddress.hex(4)
                                                debugContextMemoryEnd = (it.startAddress + 0xff)
                                                    .coerceAtMost(it.endAddressInclusive).hex(4)
                                            }
                                            debugContextError = null
                                            debugContextPreview = null
                                            showDebugContextDialog = true
                                        },
                                    ) { Text(uiText("debug.contextTitle")) }
                                }
                            },
                            memory = {
                                DebuggerSection {
                                    SelectionContainer {
                                        MemoryViewPanel(
                                            runner = runner,
                                            keyboardInput = keyboardInput,
                                            startAddress = memoryViewStartAddress,
                                            onStartAddressChange = ::selectDebuggerMemory,
                                            snapshotRevision = cpu,
                                            comparison = debuggerComparison,
                                            navigationRevision = memoryNavigationRevision,
                                        )
                                    }
                                }
                                DebuggerSection {
                                    SelectionContainer {
                                        MemoryMapPanel(
                                            regions = MachineCatalog.require(runner?.machineId ?: selectedMachineId).memoryRegions,
                                            onSelect = { navigateToMemory(it.startAddress) },
                                        )
                                    }
                                }
                            },
                            history = {
                                var refreshRevision by rememberSaveable { mutableStateOf(0) }
                                // CPU refresh is paced independently of fast Runner ticks.
                                val historySnapshot = remember(runner, cpu, runnerState, refreshRevision) { captureDebuggerHistory(runner) }
                                DebuggerSection {
                                    DebuggerHistoryPanel(historySnapshot, keyboardInput,
                                        onInstruction = { navigateInstruction(it) },
                                        onMemory = ::navigateToMemory,
                                        onRefresh = { refreshRevision++ })
                                }
                            },
                            registrations = {
                                DebuggerSection { DebuggerRegistrationPanel(runner, keyboardInput, executionRevision = cpu) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showMcpServerWindow) {
        StudioWindow(
            id = "mcp-server",
            onCloseRequest = { showMcpServerWindow = false },
            title = uiText("mcp.window", ProjectInfo.STUDIO_DISPLAY_NAME),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudioScrollableColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = 16.dp,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        McpServerPanel(
                            port = mcpPort,
                            onPortChange = { mcpPort = it; mcpServerError = null },
                            info = mcpServerInfo,
                            error = mcpServerError,
                            lastRequest = mcpLastRequest,
                            onStart = {
                                val port = mcpPort.toIntOrNull()
                                if (port == null || port !in 1..65535) {
                                    mcpServerError = uiText("mcp.invalidPort")
                                } else {
                                    when (val result = mcpServer.start(port)) {
                                        is DesktopMcpStartResult.Success -> {
                                            mcpServerInfo = result.info
                                            mcpServerError = null
                                        }
                                        is DesktopMcpStartResult.Failure -> mcpServerError = StudioStrings.system.mcpStartError(result.message)
                                    }
                                }
                            },
                            onStop = { mcpServer.stop(); mcpServerInfo = null; mcpLastRequest = null },
                            onCopyUrl = { mcpServerInfo?.let { copyToClipboard(it.url); message = uiText("mcp.copiedUrl") } },
                            onCopyToken = { mcpServerInfo?.let { copyToClipboard(it.token); message = uiText("mcp.copiedToken") } },
                            onCopyConfiguration = { mcpServerInfo?.let { copyToClipboard(codexMcpConfiguration(it)); message = uiText("mcp.copiedConfig") } },
                        )
                    }
                }
            }
        }
    }

    if (showAssemblyWorkspace) {
        StudioWindow(
            id = "assembly-workspace",
            onCloseRequest = { showAssemblyWorkspace = false },
            title = projectWorkspace?.let { uiText("assembly.windowProject" , (ProjectInfo.STUDIO_DISPLAY_NAME), (it.definition.name)) }
                ?: uiText("assembly.window" , (ProjectInfo.STUDIO_DISPLAY_NAME)),
        ) {
            PgpStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SelectionContainer {
                        StudioScrollableColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = 16.dp,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                        val workspace = projectWorkspace
                        if (workspace == null) {
                            Text(uiText("project.empty"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::openProjectFromDisk) { Text(uiText("project.open")) }
                                Button(onClick = {
                                    newProjectError = null
                                    showCreateProjectDialog = true
                                }) { Text(uiText("project.new")) }
                            }
                        } else {
                            Text(uiText("assembly.title"), style = MaterialTheme.typography.titleLarge)
                            Text(assemblyStatus)
                            assemblyPreview.filter { it.consoleLog.isNotBlank() }.forEach {
                                AssemblyPreviewSection(uiText("assembly.logTitle" , (it.source.definition.path)), it.consoleLog)
                            }
                            val memoryLayout = projectMemoryLayout(workspace)
                            val memoryLayoutText = remember(memoryLayout.machineId, memoryLayout.pc1251MemoryMode) {
                                memoryLayout.renderMemoryLayout(StudioStrings.system)
                            }
                            var showMemoryLayout by remember { mutableStateOf(false) }
                            TextButton(onClick = { showMemoryLayout = !showMemoryLayout }) {
                                Text(if (showMemoryLayout) uiText("assembly.hideLayout") else uiText("assembly.showLayout"))
                            }
                            if (showMemoryLayout) AssemblyPreviewSection(uiText("assembly.layout"), memoryLayoutText)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val hasAssemblySources = workspace.sources.any {
                                    it.definition.type == ProjectSourceType.ASSEMBLY
                                }
                                Button(enabled = hasAssemblySources && !assemblyBusy, onClick = ::assembleWorkspace) {
                                    Text(uiText("assembly.assemble"))
                                }
                                Button(enabled = hasAssemblySources && !assemblyBusy, onClick = { buildProjectOutputs() }) {
                                    Text(uiText("assembly.build"))
                                }
                                Button(
                                    enabled = hasAssemblySources && !assemblyBusy && runner != null && runnerState != RunnerState.FAULTED,
                                    onClick = ::buildProject,
                                ) { Text(uiText("project.buildLoad")) }
                            }
                            if (assemblyBusy) OutlinedButton(onClick = { assemblyJob?.cancel() }) { Text(uiText("assembly.cancel")) }
                            Text(uiText("assembly.help"))
                            val projectIsLoaded = loadedProjectManifest == workspace.manifestFile.absolutePath
                            val emulatorLoadStatus = when {
                                !projectIsLoaded -> uiText("assembly.emulatorNotLoaded")
                                projectRuntimeStatus == ProjectRuntimeStatus.UP_TO_DATE ->
                                    uiText("assembly.emulatorLoaded" , (runner?.machineId?.displayName().orEmpty()))
                                projectRuntimeStatus == ProjectRuntimeStatus.SOURCE_CHANGED ->
                                    uiText("assembly.emulatorChanged")
                                projectRuntimeStatus == ProjectRuntimeStatus.BUILT_NOT_LOADED ->
                                    uiText("assembly.emulatorBuilt")
                                else -> uiText("assembly.emulatorPrevious")
                            }
                            Text(
                                emulatorLoadStatus,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (projectIsLoaded && projectRuntimeStatus == ProjectRuntimeStatus.UP_TO_DATE)
                                    Color(0xff28783c) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (projectIsLoaded) Text(uiText("load.help"))
                            Text(uiText("project.sources"), style = MaterialTheme.typography.titleMedium)
                            val assemblySources = workspace.sources.filter {
                                it.definition.type == ProjectSourceType.ASSEMBLY
                            }
                            val untrackedAssemblyFiles = projectTree?.untracked.orEmpty().filter {
                                it.file.extension.equals("asm", ignoreCase = true)
                            }
                            if (assemblySources.isEmpty() && untrackedAssemblyFiles.isEmpty()) {
                                Text(uiText("assembly.noSources"))
                            }
                            assemblySources.forEach { source ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "  ${source.definition.path}" +
                                            if (source.definition.id in changedAssemblySources) " *" else "",
                                        modifier = Modifier.weight(1f),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Button(onClick = { openExternalSource(source.file) }) { Text(uiText("common.open")) }
                                }
                            }
                            untrackedAssemblyFiles.forEach { source ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(uiText("assembly.untrackedSource" , (source.relativePath)), fontFamily = FontFamily.Monospace)
                                    Button(onClick = { openExternalSource(source.file) }) { Text(uiText("common.open")) }
                                }
                            }
                            if (untrackedAssemblyFiles.isNotEmpty()) {
                                Button(onClick = ::updateProject) { Text(uiText("assembly.addToProject")) }
                            }
                            Text(uiText("assembly.outputs"), style = MaterialTheme.typography.titleMedium)
                            Text(DesktopProjectBuildOutputWriter.outputFiles(workspace).joinToString("\n") {
                                uiText("assembly.outputFile", "build/${it.name}", uiText(if (it.isFile) "assembly.fileExists" else "assembly.fileMissing"))
                            }, fontFamily = FontFamily.Monospace)
                            Text(uiText("common.diagnostics"), style = MaterialTheme.typography.titleMedium)
                            if (assemblyDiagnostics.isEmpty()) Text(uiText("assembly.noDiagnostics"))
                            assemblyDiagnostics.forEach { diagnostic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        buildString {
                                            append(if (diagnostic.file == diagnostic.source.file) diagnostic.source.definition.path else diagnostic.file.path).append(':')
                                                .append(diagnostic.line ?: "?").append(": ").append(StudioStrings.system.projectOperationError(diagnostic.message))
                                            diagnostic.sourceText?.let { append("\n  ").append(it) }
                                        },
                                        modifier = Modifier.weight(1f),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    TextButton(onClick = {
                                        openExternalSource(diagnostic.file, diagnostic.line)
                                    }) { Text(if (diagnostic.line == null) uiText("assembly.openSource") else uiText("assembly.openLine")) }
                                }
                            }
                            val previewSources = assemblyPreview
                            val assemblyOutput by produceState(uiText("assembly.preparing"), previewSources, memoryLayout) {
                                value = withContext(Dispatchers.Default) { buildString {
                                    previewSources.forEach { result ->
                                        appendLine(result.source.definition.path)
                                        appendLine(uiText("assembly.memory"))
                                        appendLine(result.renderMemorySummary(memoryLayout, StudioStrings.system))
                                        appendLine(uiText("assembly.disassembly"))
                                        appendLine(result.renderDisassembly().trimEnd())
                                        appendLine(uiText("assembly.listing"))
                                        appendLine(result.renderListing().trimEnd())
                                        appendLine()
                                    }
                                    if (previewSources.isNotEmpty()) {
                                        appendLine(uiText("assembly.symbolMap"))
                                        append(previewSources.renderMap())
                                    }
                                } }
                            }
                            AssemblyOutputField(assemblyOutput)
                        }
                        }
                    }
                }
            }
        }
    }

    PgpStudioTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(appFocusRequester)
                .focusable(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(uiText("main.machine"))
                    Box {
                        Button(onClick = { machineMenuExpanded = true }) {
                            val displayedMachine = runner?.machineId ?: selectedMachineId
                            Text(
                                uiText(if (displayedMachine in installedRomMachines) "main.machineSelected" else "main.machineMissing", displayedMachine.displayName()),
                            )
                        }
                        DropdownMenu(
                            expanded = machineMenuExpanded,
                            onDismissRequest = { machineMenuExpanded = false },
                        ) {
                            MachineCatalog.definitions.forEach { definition ->
                                val installed = definition.id in installedRomMachines
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (installed) definition.displayName
                                            else uiText("main.machineUnregistered", definition.displayName),
                                        )
                                    },
                                    enabled = installed,
                                    onClick = {
                                        machineMenuExpanded = false
                                        switchMachine(definition.id)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(uiText("rom.manage")) },
                                onClick = {
                                    machineMenuExpanded = false
                                    showRomManager = true
                                },
                            )
                        }
                    }
                    Text(uiText("main.romStatus", if (runner?.machineId in installedRomMachines) uiText("main.registered") else loadedRomName ?: uiText("main.notLoaded")))
                    projectWorkspace?.let { Text(uiText("main.openProject", it.definition.name)) }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            if (runnerState == RunnerState.RUNNING) pauseEmulator() else runEmulator()
                        },
                    ) { Text(if (runnerState == RunnerState.RUNNING) uiText("emulator.pause") else uiText("emulator.run")) }
                    Button(enabled = runner != null, onClick = ::resetEmulator) { Text(uiText("emulator.reset")) }
                    Button(
                        enabled = runner != null && runnerState == RunnerState.PAUSED,
                        onClick = ::stepEmulator,
                    ) { Text(uiText("emulator.step")) }
                    val activeDefinition = MachineCatalog.require(runner?.machineId ?: selectedMachineId)
                    val showModeButtons = activeDefinition.id.value in setOf(
                        "pc-1245", "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261",
                    )
                    if (showModeButtons && OperatingMode.RUN in activeDefinition.supportedOperatingModes &&
                        OperatingMode.PROGRAM in activeDefinition.supportedOperatingModes
                    ) {
                        Button(
                            enabled = runner != null && operatingMode != OperatingMode.RUN,
                            onClick = { setMode(OperatingMode.RUN) },
                        ) { Text("RUN") }
                        Button(
                            enabled = runner != null && operatingMode != OperatingMode.PROGRAM,
                            onClick = { setMode(OperatingMode.PROGRAM) },
                        ) { Text("PRO") }
                    }
                    if (showModeButtons && OperatingMode.RESERVE in activeDefinition.supportedOperatingModes) {
                        Button(
                            enabled = runner != null && operatingMode != OperatingMode.RESERVE,
                            onClick = { setMode(OperatingMode.RESERVE) },
                        ) { Text("RSV") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(projectPaneWidth).fillMaxHeight(),
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        var controlsHeight by remember { mutableStateOf(
                            DesktopUiPreferences.projectControlsHeight(280f).dp
                        ) }
                        val maximumControlsHeight = maxHeight * 0.55f
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StudioScrollableColumn(modifier = Modifier.fillMaxWidth()
                                .height(controlsHeight.coerceIn(48.dp, maxOf(48.dp, maximumControlsHeight))),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(uiText("menu.project"), style = MaterialTheme.typography.titleMedium)
                                    projectWorkspace?.let { workspace ->
                                        Text(workspace.definition.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${workspace.definition.machineId.displayName()}  •  " +
                                                projectRuntimeStatus.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        if (changedProjectSources.isNotEmpty()) {
                                            Text(
                                                uiText("main.changedSources", changedProjectSources.joinToString()),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    } ?: Text(uiText("project.empty"))
                                }
                            }
                            projectWorkspace?.let { workspace ->
                                Button(
                                    enabled = runner != null && runnerState != RunnerState.FAULTED,
                                    onClick = ::buildProject,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(uiText("project.buildLoad")) }
                                Button(
                                    onClick = ::updateProject,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(uiText("main.updateProject")) }
                                Button(
                                    onClick = { showAssemblyWorkspace = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(uiText("assembly.title")) }
                            }
                            }
                            if (projectWorkspace != null) {
                                ProjectHorizontalSplitter(
                                    onDelta = { controlsHeight = (controlsHeight + it).coerceIn(48.dp, maxOf(48.dp, maximumControlsHeight)) },
                                    onFinished = { DesktopUiPreferences.saveProjectControlsHeight(controlsHeight.value) },
                                )
                            }
                            projectWorkspace?.let { workspace ->
                                projectTree?.let { tree ->
                                    ProjectTreePanel(
                                        tree = tree,
                                        selectedPath = selectedProjectSourcePath,
                                        onSelected = { selectedProjectSourcePath = it },
                                        onEdit = { openExternalSource(it) },
                                        onCreateWav = { file ->
                                            wavToolInput = WavToolInput(file, uiText("project.programLabel" , (workspace.definition.name)), workspace.definition.machineId, System.nanoTime())
                                            showWavTool = true
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                } ?: Text(uiText("main.projectFilesLoading"))
                            }
                        }
                    }
                    }

                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .width(PROJECT_PANE_RESIZE_HANDLE_WIDTH)
                            .fillMaxHeight()
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                            .pointerInput(density) {
                                detectDragGestures(
                                    onDragEnd = {
                                        DesktopUiPreferences.saveProjectPaneWidth(projectPaneWidth.value)
                                    },
                                    onDragCancel = {
                                        DesktopUiPreferences.saveProjectPaneWidth(projectPaneWidth.value)
                                    },
                                ) { change, dragAmount ->
                                        change.consume()
                                        val delta = with(density) { dragAmount.x.toDp() }
                                        projectPaneWidth = (projectPaneWidth + delta).coerceIn(
                                            MIN_PROJECT_PANE_WIDTH,
                                            MAX_PROJECT_PANE_WIDTH,
                                        )
                                    }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(1.dp).fillMaxHeight(),
                        ) {}
                    }

                    StudioScrollableColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PocketLcdPanel(display, runner?.machineId ?: selectedMachineId)
                        PocketSoftwareKeyboard(
                            runner,
                            MachineCatalog.require(runner?.machineId ?: selectedMachineId).keyboardLayout,
                        )
                        loadedProgramLabel?.let { label ->
                            Text(
                                if (loadedProjectManifest != null &&
                                    loadedProjectManifest != projectWorkspace?.manifestFile?.absolutePath)
                                    uiText("main.outsideProject", label) else label,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HostKeyInputModeSelector(
                            mode = hostKeyInputMode,
                            onModeSelected = { selected ->
                                keyboardInput.setInputMode(selected)
                                hostKeyInputMode = selected
                                DesktopUiPreferences.saveHostKeyInputMode(selected)
                            },
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(message)
                            Text(buildString {
                                append(uiText("main.status", runnerState.uiLabel(), executedCycles.toString()))
                                if (requestedToneHz != 0) append("  •  $requestedToneHz Hz")
                                projectWorkspace?.let {
                                    append("  •  ${projectRuntimeStatus.displayName}")
                                    if (changedProjectSources.isNotEmpty()) append(uiText("main.sourceChanged"))
                                }
                                debuggerStopReason?.let { append("  •  ${it.displayName()}") }
                            }, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

            }
        }

        if (showRomManager) {
            AlertDialog(
                onDismissRequest = { showRomManager = false },
                title = { Text(uiText("rom.libraryTitle")) },
                text = {
                    StudioScrollableColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(uiText("rom.libraryHelp"))
                        com.digihori.pgp.desktop.rom.DesktopRomRegistrationCatalog.definitions
                            .forEach { definition ->
                            val installed = definition.id in installedRomMachines
                            com.digihori.pgp.desktop.rom.RomLibraryRow(
                                machineId = definition.id,
                                installed = installed,
                                sourceName = romLibrary.metadata(definition.id)?.sourceName,
                                onRegister = { chooseAndImportRom(definition.id) },
                                onRemove = { pendingRomRemoval = definition.id },
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRomManager = false }) { Text(uiText("common.close")) }
                },
            )
        }

        pendingRomRemoval?.let { machineId ->
            AlertDialog(
                onDismissRequest = { pendingRomRemoval = null },
                title = {
                    Text(
                        uiText("rom.removeConfirm", com.digihori.pgp.desktop.rom.DesktopRomRegistrationCatalog.label(machineId, StudioStrings.system)),
                    )
                },
                text = { Text(uiText("rom.removeHelp")) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRomRemoval = null
                        removeStoredRom(machineId)
                    }) { Text(uiText("rom.removeAction")) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRomRemoval = null }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showRenameProjectDialog) {
            AlertDialog(
                onDismissRequest = { showRenameProjectDialog = false },
                title = { Text(uiText("project.renameTitle")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renamedProjectName,
                            onValueChange = { renamedProjectName = it; renameProjectError = null },
                            label = { Text(uiText("project.name")) },
                            singleLine = true,
                        )
                        Text(uiText("project.folderLabel", (DesktopProjectWorkspaceCreator.folderName(renamedProjectName))))
                        renameProjectError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val workspace = projectWorkspace ?: return@TextButton
                        DesktopProjectEditor.rename(workspace, renamedProjectName).fold(
                            onSuccess = { updated ->
                                projectWorkspace = updated
                                projectChangeTracker = DesktopProjectChangeTracker(updated)
                                projectTree = DesktopProjectTreeScanner.scan(updated)
                                if (loadedProjectManifest == workspace.manifestFile.absolutePath) {
                                    loadedProgramLabel = uiText("project.renamedProgramLabel" , (updated.definition.name))
                                    loadedProjectManifest = updated.manifestFile.absolutePath
                                }
                                showRenameProjectDialog = false
                                message = uiText("project.renamed" , (updated.definition.name))
                            },
                            onFailure = { renameProjectError = StudioStrings.system.projectOperationError(it.message.orEmpty()) },
                        )
                    }) { Text(uiText("project.renameAction")) }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameProjectDialog = false }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showCreateProjectDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjectDialog = false },
                title = { Text(uiText("project.createTitle")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(uiText("project.machineLabel" , (selectedMachineId.displayName())))
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = {
                                newProjectName = it
                                newProjectError = null
                            },
                            label = { Text(uiText("project.name")) },
                            singleLine = true,
                        )
                        Text(uiText("project.starters"))
                        com.digihori.pgp.desktop.project.ProjectStarterOptions(
                            selected = newProjectStarters,
                            onSelected = { newProjectStarters = it; newProjectError = null },
                        )
                        Text(uiText("project.noOverwrite"))
                        newProjectError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newProjectName.isBlank()) {
                            newProjectError = uiText("project.blankName")
                            return@TextButton
                        }
                        if (newProjectStarters.isEmpty()) {
                            newProjectError = uiText("project.noStarters")
                            return@TextButton
                        }
                        newProjectError = null
                        showCreateProjectDialog = false
                        requestProjectParentSelection = true
                    }) { Text(uiText("project.createFolder")) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjectDialog = false }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showSaveMemoryDumpDialog) {
            AlertDialog(
                onDismissRequest = { showSaveMemoryDumpDialog = false },
                title = { Text(uiText("memory.saveTitle")) },
                text = {
                    com.digihori.pgp.desktop.machine.MemoryDumpRangeFields(
                        start = dumpStartAddress,
                        end = dumpEndAddress,
                        onStart = { dumpStartAddress = it },
                        onEnd = { dumpEndAddress = it },
                        error = dumpRangeError,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = parseHexAddress(dumpStartAddress)
                        val end = parseHexAddress(dumpEndAddress)
                        when {
                            start == null -> dumpRangeError = uiText("memory.invalidStart")
                            end == null -> dumpRangeError = uiText("memory.invalidEnd")
                            end < start -> dumpRangeError = uiText("memory.reversedRange")
                            else -> {
                                val activeRunner = runner ?: return@TextButton
                                showSaveMemoryDumpDialog = false
                                val destination = selectAndRestoreFocus {
                                    selectMemoryDumpDestination(ownerWindow)
                                } ?: return@TextButton
                                val snapshot = activeRunner.memorySnapshot(start, end - start + 1)
                                runCatching { destination.writeBytes(DesktopMemoryDumpWriter.write(snapshot)) }
                                    .onSuccess {
                                        message = uiText("memory.saved", (snapshot.size).toString(), (start.hex(4)), (end.hex(4)), (destination.name).toString())
                                    }
                                    .onFailure {
                                        message = uiText("memory.saveFailed", (it.message ?: it::class.simpleName).toString())
                                    }
                            }
                        }
                    }) { Text(uiText("common.save")) }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveMemoryDumpDialog = false }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showSaveDisassemblyDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDisassemblyDialog = false },
                title = { Text(uiText("debug.saveDisassembly")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(uiText("memory.rangeHelp"))
                        OutlinedTextField(
                            value = disassemblyStartAddress,
                            onValueChange = { disassemblyStartAddress = it },
                            label = { Text(uiText("memory.start")) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = disassemblyEndAddress,
                            onValueChange = { disassemblyEndAddress = it },
                            label = { Text(uiText("memory.end")) },
                            singleLine = true,
                        )
                        disassemblyRangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = parseHexAddress(disassemblyStartAddress)
                        val end = parseHexAddress(disassemblyEndAddress)
                        when {
                            start == null -> disassemblyRangeError = uiText("memory.invalidStart")
                            end == null -> disassemblyRangeError = uiText("memory.invalidEnd")
                            end < start -> disassemblyRangeError = uiText("memory.reversedRange")
                            else -> {
                                val activeRunner = runner ?: return@TextButton
                                showSaveDisassemblyDialog = false
                                val destination = selectAndRestoreFocus {
                                    selectDisassemblyDestination(ownerWindow)
                                } ?: return@TextButton
                                runCatching {
                                    Sc61860Disassembler.renderAssembly(start, end, activeRunner::memoryByte)
                                }.mapCatching { source ->
                                    destination.writeText(source)
                                    source.lineSequence().count() - 1
                                }.onSuccess { instructionCount ->
                                    message = uiText("debug.disassemblySaved", (instructionCount).toString(), (start.hex(4)), (end.hex(4)), (destination.name).toString())
                                }.onFailure {
                                    message = uiText("debug.disassemblySaveFailed", (it.message ?: it::class.simpleName).toString())
                                }
                            }
                        }
                    }) { Text(uiText("common.save")) }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDisassemblyDialog = false }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showSaveCheckpointDialog) {
            AlertDialog(
                onDismissRequest = { showSaveCheckpointDialog = false },
                title = { Text(uiText("debug.checkpointTitle")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(uiText("debug.checkpointRange"))
                        OutlinedTextField(
                            value = checkpointStartAddress,
                            onValueChange = { checkpointStartAddress = it },
                            label = { Text(uiText("memory.start")) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = checkpointEndAddress,
                            onValueChange = { checkpointEndAddress = it },
                            label = { Text(uiText("memory.end")) },
                            singleLine = true,
                        )
                        checkpointRangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = parseHexAddress(checkpointStartAddress)
                        val end = parseHexAddress(checkpointEndAddress)
                        when {
                            start == null || end == null -> checkpointRangeError = uiText("debug.invalidAddresses")
                            end < start -> checkpointRangeError = uiText("memory.reversedRange")
                            end - start + 1 > DesktopEmulatorRunner.MAX_MEMORY_WATCH_BYTES ->
                                checkpointRangeError = uiText("debug.checkpointLimit")
                            else -> {
                                val activeRunner = runner ?: return@TextButton
                                val wasRunning = activeRunner.state == RunnerState.RUNNING
                                activeRunner.pause()
                                if (wasRunning) debuggerComparison = comparisonTracker.stopped(activeRunner)
                                audioPlayer.stop()
                                runnerState = activeRunner.state
                                debuggerStopReason = activeRunner.stopReason
                                val cpuSnapshot = activeRunner.cpuSnapshot()
                                cpu = cpuSnapshot
                                val checkpointBytes = runCatching {
                                    DesktopDebuggerCheckpointWriter.write(
                                        machineId = activeRunner.machineId.value,
                                        capturedAt = Instant.now().toString(),
                                        executedCycles = executedCycles,
                                        cpu = cpuSnapshot,
                                        memory = activeRunner.memorySnapshot(start, end - start + 1),
                                        trace = activeRunner.instructionTrace(),
                                        stopReason = activeRunner.stopReason?.checkpointDescription(),
                                    )
                                }.getOrElse {
                                    checkpointRangeError = uiText("debug.checkpointCaptureFailed", (it.message ?: it::class.simpleName).toString())
                                    return@TextButton
                                }
                                showSaveCheckpointDialog = false
                                val destination = selectAndRestoreFocus {
                                    selectCheckpointDestination(ownerWindow)
                                } ?: return@TextButton
                                runCatching { destination.writeBytes(checkpointBytes) }
                                    .onSuccess { message = uiText("debug.checkpointSaved", (destination.name).toString()) }
                                    .onFailure {
                                        message = uiText("debug.checkpointSaveFailed", (it.message ?: it::class.simpleName).toString())
                                    }
                            }
                        }
                    }) { Text(uiText("common.save")) }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveCheckpointDialog = false }) { Text(uiText("common.cancel")) }
                },
            )
        }

        if (showDebugContextDialog) {
            AlertDialog(
                onDismissRequest = { showDebugContextDialog = false },
                title = { Text(uiText("debug.contextTitle")) },
                text = {
                    Column(
                        modifier = Modifier.widthIn(max = 760.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(uiText("debug.contextHelp"))
                        DebugContextFieldPair(
                            first = debugContextMemoryStart, second = debugContextMemoryEnd,
                            onFirstChange = { debugContextMemoryStart = it; debugContextPreview = null },
                            onSecondChange = { debugContextMemoryEnd = it; debugContextPreview = null },
                            firstLabelKey = "debug.memoryStart", secondLabelKey = "debug.memoryEnd",
                        )
                        Text(uiText("debug.omitMemory"))
                        DebugContextFieldPair(
                            first = debugContextDisassemblyCount, second = debugContextTraceLimit,
                            onFirstChange = { debugContextDisassemblyCount = it; debugContextPreview = null },
                            onSecondChange = { debugContextTraceLimit = it; debugContextPreview = null },
                            firstLabelKey = "debug.disassemblyCount", secondLabelKey = "debug.traceEntries",
                        )
                        DebugContextFieldPair(
                            first = debugContextAccessHistoryLimit, second = debugContextBankHistoryLimit,
                            onFirstChange = { debugContextAccessHistoryLimit = it; debugContextPreview = null },
                            onSecondChange = { debugContextBankHistoryLimit = it; debugContextPreview = null },
                            firstLabelKey = "debug.accessEntries", secondLabelKey = "debug.bankEntries",
                        )
                        debugContextError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        debugContextPreview?.let { preview ->
                            val previewScrollState = rememberScrollState()
                            Text(uiText("debug.previewSize", (preview.size).toString()))
                            Surface(
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SelectionContainer {
                                        Text(
                                            text = preview.decodeToString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxSize().padding(12.dp)
                                                .verticalScroll(previewScrollState),
                                        )
                                    }
                                    VerticalScrollbar(
                                        adapter = rememberScrollbarAdapter(previewScrollState),
                                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (debugContextPreview == null) {
                        TextButton(onClick = {
                            val memoryStart = debugContextMemoryStart.trim()
                            val memoryEnd = debugContextMemoryEnd.trim()
                            val memoryRange = when {
                                memoryStart.isEmpty() && memoryEnd.isEmpty() -> null
                                memoryStart.isEmpty() || memoryEnd.isEmpty() -> {
                                    debugContextError = uiText("debug.bothAddresses")
                                    return@TextButton
                                }
                                else -> {
                                    val start = parseHexAddress(memoryStart)
                                    val end = parseHexAddress(memoryEnd)
                                    if (start == null || end == null || end < start) {
                                        debugContextError = uiText("debug.memoryOrder")
                                        return@TextButton
                                    }
                                    DebugMemoryRangeRequest(start, end - start + 1)
                                }
                            }
                            val disassemblyCount = debugContextDisassemblyCount.toIntOrNull()
                            val accessLimit = debugContextAccessHistoryLimit.toIntOrNull()
                            val bankLimit = debugContextBankHistoryLimit.toIntOrNull()
                            val traceLimit = debugContextTraceLimit.toIntOrNull()
                            if (disassemblyCount == null || accessLimit == null || bankLimit == null || traceLimit == null) {
                                debugContextError = uiText("debug.integerLimits")
                                return@TextButton
                            }
                            val activeCpu = cpu
                            val service = debugContextService
                            if (activeCpu == null || service == null) {
                                debugContextError = uiText("debug.noSession")
                                return@TextButton
                            }
                            when (val result = service.capture(
                                DebugContextRequest(
                                    memoryRanges = listOfNotNull(memoryRange),
                                    disassemblyStartAddress = activeCpu.programCounter,
                                    disassemblyInstructionCount = disassemblyCount,
                                    memoryAccessHistoryLimit = accessLimit,
                                    bankHistoryLimit = bankLimit,
                                    traceLimit = traceLimit,
                                ),
                            )) {
                                is DebugContextResult.Success -> {
                                    debugContextPreview = DebugContextJsonWriter.write(result.context)
                                    debugContextError = null
                                }
                                is DebugContextResult.Failure -> {
                                    debugContextError = result.error.displayMessage()
                                    debugContextPreview = null
                                }
                            }
                        }) { Text(uiText("debug.preview")) }
                    } else {
                        TextButton(onClick = {
                            val bytes = debugContextPreview ?: return@TextButton
                            val destination = selectAndRestoreFocus {
                                selectDebugContextDestination(ownerWindow)
                            } ?: return@TextButton
                            runCatching { destination.writeBytes(bytes) }
                                .onSuccess {
                                    showDebugContextDialog = false
                                    message = uiText("debug.contextSaved", (destination.name).toString())
                                }
                                .onFailure {
                                    debugContextError = uiText("debug.contextSaveFailed", (it.message ?: it::class.simpleName).toString())
                                }
                        }) { Text(uiText("debug.saveJson")) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDebugContextDialog = false }) { Text(uiText("common.close")) }
                },
            )
        }

        errorDialogMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { errorDialogMessage = null },
                title = { Text(uiText("common.errorTitle")) },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { errorDialogMessage = null }) { Text(uiText("common.ok")) }
                },
            )
        }
    }
}

@Composable
internal fun CpuRegisterPanel(
    snapshot: CpuSnapshot?, changedFields: Set<CpuField>, strings: StudioStrings = StudioStrings.system,
    previousSnapshot: CpuSnapshot? = null, comparisonBasis: DebuggerComparisonBasis = DebuggerComparisonBasis.NONE,
) {
    DebuggerCpuValues(snapshot, previousSnapshot, changedFields, comparisonBasis, strings)
}

@Composable
internal fun DisassemblyPanel(
    runner: DesktopEmulatorRunner?,
    snapshot: CpuSnapshot?,
    keyboardInput: DesktopKeyboardInput,
    onSaveRange: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
    navigation: Pair<Int, Int?>? = null,
) {
    var customStartAddress by rememberSaveable(runner) { mutableStateOf<Int?>(null) }
    var addressText by rememberSaveable(runner) { mutableStateOf("") }
    var addressError by rememberSaveable(runner) { mutableStateOf(false) }
    var breakpointRevision by remember(runner) { mutableStateOf(0) }
    var breakpointError by remember(runner) { mutableStateOf<DebuggerRegistrationError?>(null) }
    val breakpointEntries = remember(runner, snapshot, runner?.registrationRevision, breakpointRevision) { runner?.breakpointRegistrations().orEmpty() }
    val breakpoints = breakpointEntries.filter { it.enabled }.map { it.address }.toSet()
    var handledNavigation by rememberSaveable(runner) { mutableStateOf<Int?>(null) }
    LaunchedEffect(navigation) {
        navigation?.takeIf { it.first != handledNavigation }?.let {
            customStartAddress = it.second
            addressText = it.second?.hex(4).orEmpty()
            addressError = false
            handledNavigation = it.first
        }
    }
    val startAddress = customStartAddress ?: snapshot?.programCounter
    val lines = if (runner == null || startAddress == null) {
        emptyList()
    } else {
        runCatching {
            DesktopDisassemblyModel.build(startAddress, DISASSEMBLY_LINE_COUNT, runner::memoryByte)
        }.getOrDefault(emptyList())
    }
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(strings.text("debug.disassembly"), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = {
                    addressText = it
                    addressError = false
                },
                label = { Text(strings.text("debug.addressHex")) },
                placeholder = { Text(startAddress?.hex(4).orEmpty()) },
                isError = addressError,
                supportingText = { if (addressError) Text(strings.text("debug.addressError")) },
                singleLine = true,
                modifier = Modifier
                    .widthIn(max = 180.dp)
                    .onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            Button(onClick = {
                val address = parseHexAddress(addressText)
                if (address == null) addressError = true else customStartAddress = address
            }) { Text(strings.text("debug.go")) }
            Button(
                enabled = customStartAddress != null,
                onClick = {
                    customStartAddress = null
                    addressText = ""
                    addressError = false
                },
            ) { Text(strings.text("debug.followPc")) }
            Button(
                enabled = runner != null,
                onClick = onSaveRange,
            ) { Text(strings.text("debug.saveRange")) }
        }
        breakpointError?.let { Text(it.display(strings), color = MaterialTheme.colorScheme.error) }
        if (lines.isEmpty()) {
            Text(strings.text("debug.noCpu"), fontFamily = FontFamily.Monospace)
        } else {
            Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            lines.forEach { line ->
                val address = line.instruction.address
                val isCurrent = address == snapshot?.programCounter
                val marker = if (isCurrent) ">" else " "
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val registration = breakpointEntries.firstOrNull { it.address == address }
                    TextButton(
                        modifier = Modifier.semantics { contentDescription = strings.text(
                            if (address in breakpoints) "debug.breakpointRemove" else if (registration != null) "debug.registration.enableBreakpoint" else "debug.breakpointAdd", address.hex(4)) },
                        onClick = {
                            val result = if (registration?.enabled == true) runner?.removeBreakpoint(registration.id)
                                else runner?.saveBreakpoint(registration?.id, address, true)
                            breakpointError = (result as? DebuggerRegistrationResult.Failure)?.error
                            breakpointRevision++
                        },
                    ) { Text(if (address in breakpoints) "●" else if (registration != null) "○-" else "○") }
                    TextButton(
                        enabled = runner != null && runner.state != RunnerState.FAULTED,
                        onClick = { runner?.runToAddress(address) },
                        modifier = Modifier.semantics { contentDescription = strings.text("debug.runToAddress", address.hex(4)) },
                    ) { Text(strings.text("debug.runTo")) }
                    Text(
                        "$marker${address.hex(4)}  " +
                            "${line.byteText.padEnd(DISASSEMBLY_BYTE_COLUMN_WIDTH)}  ${line.instructionText}",
                        fontFamily = FontFamily.Monospace,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                }
            }
            }
        }
    }
}

@Composable
internal fun MemoryViewPanel(
    runner: DesktopEmulatorRunner?,
    keyboardInput: DesktopKeyboardInput,
    startAddress: Int,
    onStartAddressChange: (Int) -> Unit,
    strings: StudioStrings = StudioStrings.system,
    snapshotRevision: CpuSnapshot? = null,
    navigationRevision: Int = 0,
    comparison: DebuggerComparisonFrame = DebuggerComparisonFrame(),
) {
    var addressText by rememberSaveable(runner) { mutableStateOf(startAddress.hex(4)) }
    var addressError by rememberSaveable(runner) { mutableStateOf(false) }
    var refreshRevision by remember(runner) { mutableStateOf(0) }
    var handledStartAddress by rememberSaveable(runner) { mutableStateOf(startAddress) }
    var handledNavigation by rememberSaveable(runner) { mutableStateOf(navigationRevision) }
    LaunchedEffect(startAddress, navigationRevision) {
        if (startAddress != handledStartAddress || navigationRevision != handledNavigation) {
            addressText = startAddress.hex(4)
            addressError = false
            handledStartAddress = startAddress
            handledNavigation = navigationRevision
        }
    }
    val lines = remember(runner, startAddress, refreshRevision, snapshotRevision, navigationRevision) {
        if (runner == null) {
            emptyList()
        } else {
            runCatching {
                DesktopMemoryViewModel.build(startAddress, MEMORY_VIEW_LINE_COUNT, runner::memoryByte)
            }.getOrDefault(emptyList())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(strings.text("debug.memory"), style = MaterialTheme.typography.labelLarge)
        val comparisonMatches = comparison.memoryStart == startAddress && runner?.state != RunnerState.RUNNING
        val basis = if (runner?.state == RunnerState.RUNNING) DebuggerComparisonBasis.RUNNING
            else if (comparisonMatches) comparison.memoryBasis else DebuggerComparisonBasis.NONE
        DebuggerComparisonLegend(basis, strings)
        if (comparisonMatches && comparison.excludedMemoryBytes > 0) {
            Text(strings.text("debug.diff.mappingChanged", comparison.excludedMemoryBytes.toString()))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = {
                    addressText = it
                    addressError = false
                },
                label = { Text(strings.text("debug.startHex")) },
                isError = addressError,
                supportingText = { if (addressError) Text(strings.text("debug.addressError")) },
                singleLine = true,
                modifier = Modifier
                    .widthIn(max = 180.dp)
                    .onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            Button(onClick = {
                val address = parseHexAddress(addressText)
                if (address == null) {
                    addressError = true
                } else {
                    onStartAddressChange(address)
                    refreshRevision++
                }
            }) { Text(strings.text("debug.go")) }
            Button(
                enabled = runner != null,
                onClick = {
                    onStartAddressChange((startAddress - MEMORY_VIEW_PAGE_SIZE) and 0xffff)
                },
            ) { Text(strings.text("debug.previous")) }
            Button(
                enabled = runner != null,
                onClick = {
                    onStartAddressChange((startAddress + MEMORY_VIEW_PAGE_SIZE) and 0xffff)
                },
            ) { Text(strings.text("debug.next")) }
            Button(
                enabled = runner != null,
                onClick = { refreshRevision++ },
            ) { Text(strings.text("debug.refresh")) }
        }
        if (lines.isEmpty()) {
            Text(strings.text("debug.noMemory"), fontFamily = FontFamily.Monospace)
        } else {
            Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            lines.forEach { line ->
                DebuggerMemoryLine(line, if (comparisonMatches) comparison.memoryChanges else emptyMap(), strings)
            }
            }
        }
    }
}

@Composable
internal fun InstructionTracePanel(
    runner: DesktopEmulatorRunner?,
    executionRevision: Long,
    keyboardInput: DesktopKeyboardInput,
    strings: StudioStrings = StudioStrings.system,
) {
    var enabled by remember(runner) { mutableStateOf(runner?.isInstructionTraceEnabled() == true) }
    var localRevision by remember(runner) { mutableStateOf(0) }
    var capacityText by rememberSaveable(runner) {
        mutableStateOf((runner?.instructionTraceCapacity() ?: DesktopEmulatorRunner.DEFAULT_INSTRUCTION_TRACE_CAPACITY).toString())
    }
    var capacityError by rememberSaveable(runner) { mutableStateOf(false) }
    val entries = remember(runner, executionRevision, localRevision) {
        runner?.instructionTrace().orEmpty().takeLast(INSTRUCTION_TRACE_VISIBLE_LINES).asReversed()
    }

    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.text("debug.trace"), style = MaterialTheme.typography.labelLarge)
            Button(
                enabled = runner != null,
                onClick = {
                    enabled = !enabled
                    runner?.setInstructionTraceEnabled(enabled)
                    localRevision++
                },
            ) { Text(if (enabled) strings.text("debug.disable") else strings.text("debug.enable")) }
            Button(
                enabled = runner != null && entries.isNotEmpty(),
                onClick = {
                    runner?.clearInstructionTrace()
                    localRevision++
                },
            ) { Text(strings.text("common.clear")) }
            OutlinedTextField(
                value = capacityText,
                onValueChange = {
                    capacityText = it.filter(Char::isDigit)
                    capacityError = false
                },
                label = { Text(strings.text("debug.capacity")) },
                isError = capacityError,
                supportingText = { if (capacityError) Text(strings.text("debug.capacityError", DesktopEmulatorRunner.MIN_INSTRUCTION_TRACE_CAPACITY.toString(), DesktopEmulatorRunner.MAX_INSTRUCTION_TRACE_CAPACITY.toString())) },
                singleLine = true,
                modifier = Modifier.width(120.dp).onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            Button(
                enabled = runner != null,
                onClick = {
                    val capacity = capacityText.toIntOrNull()
                    if (capacity == null || capacity !in
                        DesktopEmulatorRunner.MIN_INSTRUCTION_TRACE_CAPACITY..
                            DesktopEmulatorRunner.MAX_INSTRUCTION_TRACE_CAPACITY
                    ) {
                        capacityError = true
                    } else {
                        runner?.setInstructionTraceCapacity(capacity)
                        localRevision++
                    }
                },
            ) { Text(strings.text("common.apply")) }
            Text(
                strings.text("debug.traceCount", (runner?.instructionTrace()?.size ?: 0).toString(), (runner?.instructionTraceCapacity() ?: DesktopEmulatorRunner.DEFAULT_INSTRUCTION_TRACE_CAPACITY).toString()),
            )
        }
        when {
            !enabled && entries.isEmpty() -> Text(strings.text("debug.traceDisabled"), fontFamily = FontFamily.Monospace)
            entries.isEmpty() -> Text(strings.text("debug.traceEmpty"), fontFamily = FontFamily.Monospace)
            else -> Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                val instruction = entry.instruction
                val bytes = instruction.bytes.joinToString(" ") { it.hex(2) }
                Text(
                    "#${entry.sequence.toString().padStart(6)}  " +
                        "${instruction.address.hex(4)}  ${bytes.padEnd(DISASSEMBLY_BYTE_COLUMN_WIDTH)}  " +
                        "${Sc61860InstructionFormatter.format(instruction).padEnd(14)}  " +
                        "DP=${entry.dataPointer.hex(4)} P=${entry.p.hex(2)} " +
                        "C=${entry.carry.bit()} Z=${entry.zero.bit()}" +
                        (entry.romLocation?.let { location ->
                            "  ROM=${location.componentId.value}+${location.offset.hex(4)}"
                        } ?: ""),
                    fontFamily = FontFamily.Monospace,
                )
            }
            }
        }
    }
}

@Composable
internal fun MemoryWatchPanel(
    runner: DesktopEmulatorRunner?, keyboardInput: DesktopKeyboardInput, strings: StudioStrings = StudioStrings.system,
) {
    DebuggerRegistrationPanel(runner, keyboardInput, strings = strings, initialWatch = true)
}

@Composable
internal fun MemoryMapPanel(
    regions: List<MachineMemoryRegion>,
    onSelect: (MachineMemoryRegion) -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    val regionsById = regions.associateBy(MachineMemoryRegion::id)
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(strings.text("debug.map"), style = MaterialTheme.typography.labelLarge)
        regions.forEach { region ->
            TextButton(onClick = { onSelect(region) }) {
                val mirrorTarget = region.mirrorsRegionId?.let { id ->
                    regionsById[id]?.let { " → ${strings.memoryRegionLabel(it)}" }
                }.orEmpty()
                Text(
                    "${region.startAddress.hex(4)}–${region.endAddressInclusive.hex(4)}  " +
                        "${strings.memoryRegionLabel(region)} [${strings.text(if (region.kind == MachineMemoryRegionKind.MIRROR) "debug.mirrorKind" else "debug.kind.${region.kind.name}")}]$mirrorTarget",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

internal fun DebuggerStopReason.checkpointDescription(): String =
    displayName(StudioStrings(StudioLanguage.ENGLISH))

internal fun DebuggerStopReason.displayName(strings: StudioStrings = StudioStrings.system): String = when (this) {
    DebuggerStopReason.Reset -> strings.text("emulator.reset")
    DebuggerStopReason.UserPause -> strings.text("main.userPause")
    DebuggerStopReason.StepComplete -> strings.text("main.stepComplete")
    is DebuggerStopReason.Breakpoint -> strings.text("debug.stop.breakpoint", address.hex(4))
    is DebuggerStopReason.RunToAddress -> strings.text("debug.stop.runToReached", address.hex(4))
    is DebuggerStopReason.MemoryChanged -> {
        val preview = changes.take(4).joinToString { change ->
            "${change.address.hex(4)}:${change.before.hex(2)}→${change.after.hex(2)}"
        }
        val remaining = if (changes.size > 4) strings.text("debug.stop.more", (changes.size - 4).toString()) else ""
        strings.text("debug.stop.memoryChanged", instructionAddress.hex(4), preview, remaining)
    }
    is DebuggerStopReason.MemoryAccessed -> {
        val preview = accesses.take(4).joinToString { access ->
            val kind = if (access.kind == MemoryAccessKind.READ) "R" else "W"
            "$kind:${access.address.hex(4)}=${access.value.hex(2)}"
        }
        val remaining = if (accesses.size > 4) strings.text("debug.stop.moreAccess", (accesses.size - 4).toString()) else ""
        strings.text("debug.stop.memoryAccessed", instructionAddress.hex(4), preview, remaining)
    }
    is DebuggerStopReason.Fault -> strings.text("main.fault", fault.message(strings))
}

internal fun DebugContextError.displayMessage(strings: StudioStrings = StudioStrings.system): String = when (this) {
    DebugContextError.SessionRunning -> strings.text("debug.contextPause")
    is DebugContextError.InvalidRequest -> strings.debugContextRequestError(message)
    is DebugContextError.RomBytesNotAllowed ->
        strings.text("debug.contextNoRom")
}

@Composable
private fun PocketKeyButton(
    cap: PocketKeyCap,
    runner: DesktopEmulatorRunner?,
    modifier: Modifier = Modifier,
    inputOwner: String = "screen:${cap.key.name}",
    fitLabels: Boolean = false,
) {
    var isPressed by remember(runner, cap.key) { mutableStateOf(false) }
    val componentColors = LocalStudioComponentColors.current
    val accentColor = when (cap.key) {
        PocketKey.MODE, PocketKey.CAL, PocketKey.BASIC -> Color(0xffd7ecd3)
        PocketKey.SHIFT -> Color(0xffffefb3)
        PocketKey.CLEAR -> Color(0xfff3d0ce)
        else -> null
    }
    val keyColor by animateColorAsState(
        targetValue = when {
            runner == null -> componentColors.softwareKeyDisabled
            isPressed -> componentColors.softwareKeyPressed
            else -> accentColor ?: componentColors.softwareKey
        },
        animationSpec = tween(durationMillis = SOFTWARE_KEY_ANIMATION_MILLISECONDS),
        label = "software key color",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isPressed -> componentColors.onSoftwareKeyPressed
            accentColor != null && runner != null -> Color(0xff202020)
            else -> componentColors.onSoftwareKey
        },
        animationSpec = tween(durationMillis = SOFTWARE_KEY_ANIMATION_MILLISECONDS),
        label = "software key content color",
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = SOFTWARE_KEY_ANIMATION_MILLISECONDS),
        label = "software key offset",
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed || runner == null) 0.dp else 2.dp,
        animationSpec = tween(durationMillis = SOFTWARE_KEY_ANIMATION_MILLISECONDS),
        label = "software key elevation",
    )
    Surface(
        color = keyColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        shadowElevation = shadowElevation,
        modifier = modifier
            .offset(y = pressOffset)
            .pointerInput(runner, cap.key) {
                detectTapGestures(
                    onPress = {
                        val activeRunner = runner
                        if (activeRunner != null) {
                            val pressedAt = System.nanoTime()
                            isPressed = true
                            activeRunner.pressHostKey(cap.key, inputOwner)
                            try {
                                tryAwaitRelease()
                                val elapsedMilliseconds = (System.nanoTime() - pressedAt) / 1_000_000L
                                val remainingHold = activeRunner.minimumSoftwareKeyHoldMilliseconds() - elapsedMilliseconds
                                if (remainingHold > 0L) delay(remainingHold)
                            } finally {
                                activeRunner.releaseHostKey(cap.key, inputOwner)
                                isPressed = false
                            }
                        }
                    },
                )
            },
    ) {
        val upperLabel = cap.shiftedLabel ?: cap.basicLabel.orEmpty()
        val lowerLabel = if (cap.shiftedLabel != null) cap.basicLabel.orEmpty() else ""
        if (fitLabels) {
            val measurer = rememberTextMeasurer()
            val primaryStyle = MaterialTheme.typography.titleMedium
            val secondaryStyle = MaterialTheme.typography.labelSmall
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val primaryColor = contentColor
            Canvas(Modifier.fillMaxSize().semantics {
                contentDescription = listOf(cap.primaryLabel, upperLabel, lowerLabel).filter(String::isNotEmpty).joinToString(", ")
            }) {
                fun caption(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, top: Float, height: Float) {
                    if (text.isEmpty()) return
                    val layout = measurer.measure(text, style = style, softWrap = false, maxLines = 1)
                    val scale = minOf(1f, size.width * 0.86f / layout.size.width.coerceAtLeast(1),
                        height / layout.size.height.coerceAtLeast(1))
                    withTransform({
                        translate((size.width - layout.size.width * scale) / 2f,
                            top + (height - layout.size.height * scale) / 2f)
                        scale(scale, scale, pivot = Offset.Zero)
                    }) { drawText(layout, color = color) }
                }
                caption(upperLabel, secondaryStyle, secondaryColor, 0f, size.height * 0.25f)
                caption(cap.primaryLabel, primaryStyle, primaryColor, size.height * 0.25f, size.height * 0.5f)
                caption(lowerLabel, secondaryStyle, primaryColor, size.height * 0.75f, size.height * 0.25f)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = upperLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                Text(text = cap.primaryLabel, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, maxLines = 1)
                Text(text = lowerLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PocketSoftwareKeyboard(runner: DesktopEmulatorRunner?, layout: MachineKeyboardLayout) {
    if (layout == MachineKeyboardLayout.PC_1401 || layout in setOf(MachineKeyboardLayout.PC_1450, MachineKeyboardLayout.PC_1460, MachineKeyboardLayout.PC_1470U)) {
        val keyboard = com.digihori.pgp.desktop.input.Pc14xxKeyboardLayout.forLayout(layout) ?: Pc1401KeyboardLayout.keyboard
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PocketKeyboardPanel(runner, checkNotNull(keyboard.functionPanel).copy(
                columnCount = keyboard.columnCount,
                rows = checkNotNull(keyboard.functionPanel).rows.map { row ->
                    row.map { it.copy(column = it.column + keyboard.columnCount - checkNotNull(keyboard.functionPanel).columnCount) }
                },
            ), fitLabels = layout != MachineKeyboardLayout.PC_1401)
            PocketKeyboardPanel(runner, keyboard, fitLabels = layout != MachineKeyboardLayout.PC_1401)
        }
        return
    }
    val (rows, columnCount) = when (layout) {
        MachineKeyboardLayout.PC_1245 -> Pc1245KeyboardLayout.rows to Pc1245KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1251 -> Pc1251KeyboardLayout.rows to Pc1251KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1261 -> Pc1261KeyboardLayout.rows to Pc1261KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1350 -> Pc1350KeyboardLayout.rows to Pc1350KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1360 -> Pc1360KeyboardLayout.rows to Pc1360KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1470U, MachineKeyboardLayout.PC_1460, MachineKeyboardLayout.PC_1450, MachineKeyboardLayout.PC_1401 -> error("PC-1401 panels are rendered separately")
    }
    PocketKeyboardRows(runner, rows, columnCount)
}

@Composable
private fun PocketKeyboardPanel(runner: DesktopEmulatorRunner?, panel: PlayerKeyboard, fitLabels: Boolean = false) {
    PocketKeyboardRows(runner, panel.rows.map { row -> row.map {
        com.digihori.pgp.desktop.input.PocketKeyCap(it.key, it.primaryLabel, it.shiftedLabel, it.basicLabel, it.column, it.columnSpan)
    } }, panel.columnCount, fitLabels)
}

@Composable
private fun PocketKeyboardRows(runner: DesktopEmulatorRunner?, rows: List<List<com.digihori.pgp.desktop.input.PocketKeyCap>>, columnCount: Int, fitLabels: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1_100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                var nextColumn = 0
                row.forEach { cap ->
                    if (cap.column > nextColumn) {
                        Spacer(modifier = Modifier.weight((cap.column - nextColumn).toFloat()))
                    }
                    PocketKeyButton(
                        cap = cap,
                        inputOwner = "screen:$rowIndex:${cap.column}:${cap.key.name}",
                        fitLabels = fitLabels,
                        runner = runner,
                        modifier = Modifier
                            .weight(cap.columnSpan.toFloat())
                            .height(62.dp)
                            .padding(horizontal = 2.dp),
                    )
                    nextColumn = cap.column + cap.columnSpan
                }
                if (nextColumn < columnCount) {
                    Spacer(modifier = Modifier.weight((columnCount - nextColumn).toFloat()))
                }
            }
        }
    }
}
@Composable
internal fun HostKeyInputModeSelector(
    mode: HostKeyInputMode,
    onModeSelected: (HostKeyInputMode) -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(strings.text("main.hostInput"), style = MaterialTheme.typography.labelMedium)
        HostKeyInputModeButton(strings.text("main.logical"), mode == HostKeyInputMode.LOGICAL) {
            onModeSelected(HostKeyInputMode.LOGICAL)
        }
        HostKeyInputModeButton(strings.text("main.physical"), mode == HostKeyInputMode.PHYSICAL) {
            onModeSelected(HostKeyInputMode.PHYSICAL)
        }
    }
}

@Composable
private fun HostKeyInputModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected }.background(
            if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            MaterialTheme.shapes.small,
        ),
    ) {
        Text(label)
    }
}

@Composable
private fun PocketLcdPanel(snapshot: DisplaySnapshot?, machineId: MachineId) {
    val componentColors = LocalStudioComponentColors.current
    val panelAspectRatio = snapshot?.let {
        com.digihori.pgp.desktop.display.DesktopLcdGeometry.panelAspectRatio(machineId, it)
    } ?: LCD_PANEL_ASPECT_RATIO
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1_000.dp)
                .semantics { contentDescription = uiText("main.lcdDescription", machineId.displayName()) }
                .aspectRatio(panelAspectRatio),
        ) {
            drawRect(componentColors.lcdBackground)
            if (snapshot == null) return@Canvas

            val visualColumnCount = CharacterCellGeometry.visualColumnCount(
                snapshot.characterColumns,
                snapshot.characterWidth,
                snapshot.interCharacterColumnGap,
            )
            val visualRowCount = if (snapshot.characterRows == 1) {
                snapshot.dotRows
            } else {
                CharacterCellGeometry.visualRowCount(
                    snapshot.characterRows,
                    snapshot.characterHeight,
                    snapshot.interCharacterRowGap,
                )
            }
            val cellWidth = size.width / visualColumnCount
            val cellHeight = size.height / visualRowCount
            val insetX = cellWidth * LCD_DOT_INSET_RATIO
            val insetY = cellHeight * LCD_DOT_INSET_RATIO
            for (row in 0 until snapshot.dotRows) {
                for (column in 0 until snapshot.dotColumns) {
                    if (snapshot.isDotOn(column, row)) {
                        val visualColumn = CharacterCellGeometry.visualColumn(
                            column,
                            snapshot.characterWidth,
                            snapshot.interCharacterColumnGap,
                        )
                        val visualRow = if (snapshot.characterRows == 1) {
                            row
                        } else {
                            CharacterCellGeometry.visualRow(
                                row,
                                snapshot.characterHeight,
                                snapshot.interCharacterRowGap,
                            )
                        }
                        drawRect(
                            color = componentColors.lcdDot,
                            topLeft = Offset(
                                visualColumn * cellWidth + insetX,
                                visualRow * cellHeight + insetY,
                            ),
                            size = Size(cellWidth - insetX * 2, cellHeight - insetY * 2),
                        )
                    }
                }
            }
        }
        Text(
            text = snapshot?.let { com.digihori.pgp.player.lcdSymbolText(it.symbols) { when (it) {
                com.digihori.pgp.core.api.DisplaySymbol.MEMORY -> "M"
                com.digihori.pgp.core.api.DisplaySymbol.PARENTHESES -> "( )"
                com.digihori.pgp.core.api.DisplaySymbol.KANA_SMALL -> "小"
                com.digihori.pgp.core.api.DisplaySymbol.BATTERY -> "BATT"
                else -> it.name
            } } }.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun selectRomFile(owner: Frame, machineId: MachineId): File? {
    return selectFile(owner, uiText("rom.selectFile", machineId.displayName()), "${machineId.value}.pgrom")
}

private fun selectBasicFile(owner: Frame, title: String): File? = selectFile(owner, title, "*.bas")

private fun selectProjectDirectory(owner: Frame): File? = selectDirectory(
    owner,
    uiText("project.chooseFolder"),
)

private fun selectProjectParentDirectory(owner: Frame): File? = selectDirectory(
    owner,
    uiText("project.chooseParent"),
)

private fun selectDirectory(owner: Frame, title: String): File? {
    if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
        return selectMacDirectory(owner, title)
    }
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }
    return if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

private fun selectMacDirectory(owner: Frame, title: String): File? {
    val property = "apple.awt.fileDialogForDirectories"
    val previous = System.getProperty(property)
    System.setProperty(property, "true")
    return try {
        val dialog = FileDialog(owner, title, FileDialog.LOAD)
        try {
            dialog.isVisible = true
            val directory = dialog.directory
            val fileName = dialog.file
            if (directory != null && fileName != null) File(directory, fileName) else null
        } finally {
            dialog.dispose()
        }
    } finally {
        if (previous == null) System.clearProperty(property) else System.setProperty(property, previous)
    }
}

private fun selectMemoryDumpFile(owner: Frame): File? = selectFile(owner, uiText("memory.loadTitle"), "*.dmp")

private fun selectAssemblyFile(owner: Frame): File? = selectFile(owner, uiText("quick.openTitle"), "*.asm")

private fun selectMemoryDumpDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("memory.saveTitle"),
    suggestedFile = "memory.dmp",
    mode = FileDialog.SAVE,
)

private fun selectDisassemblyDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("debug.selectDisassembly"),
    suggestedFile = "disassembly.asm",
    mode = FileDialog.SAVE,
)

private fun selectAssembledDumpDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("quick.saveTitle"),
    suggestedFile = "program.dmp",
    mode = FileDialog.SAVE,
)

private fun selectCheckpointDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("debug.selectCheckpoint"),
    suggestedFile = "checkpoint.pgpdebug.json",
    mode = FileDialog.SAVE,
)

private fun selectDebugContextDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("debug.selectContext"),
    suggestedFile = "debug-context.pgpdebug.json",
    mode = FileDialog.SAVE,
)

private fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

internal fun codexMcpConfiguration(info: DesktopMcpServerInfo): String = """
    [mcp_servers.pokecom_go_studio]
    url = "${info.url}"
    http_headers_helper = "${tomlEscape(info.headerHelperCommand)}"
    enabled_tools = ["pgp_get_capabilities", "pgp_get_debug_context"]
    default_tools_approval_mode = "auto"
""".trimIndent()

private fun tomlEscape(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

private fun selectBasicSaveFile(owner: Frame): File? = selectFile(
    owner = owner,
    title = uiText("basic.saveTitle"),
    suggestedFile = "program.bas",
    mode = FileDialog.SAVE,
)

private fun selectFile(
    owner: Frame,
    title: String,
    suggestedFile: String,
    mode: Int = FileDialog.LOAD,
): File? {
    val dialog = FileDialog(owner, title, mode).apply {
        file = suggestedFile
        isVisible = true
    }
    val directory = dialog.directory
    val fileName = dialog.file
    dialog.dispose()
    EventQueue.invokeLater {
        owner.toFront()
        owner.requestFocus()
        owner.requestFocusInWindow()
    }
    return if (directory != null && fileName != null) File(directory, fileName) else null
}

@Composable
private fun StudioScrollableColumn(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = SCROLLBAR_CONTENT_GUTTER)
                .padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content,
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun DebuggerSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Column(content = content) }
        }
    }
}

@Composable
private fun HelpLink(label: String, url: String, onMessage: (String) -> Unit) {
    Button(onClick = { openDocumentationUrl(url)?.let(onMessage) }) { Text(label) }
}

private fun openDocumentationUrl(url: String): String? = runCatching {
    check(Desktop.isDesktopSupported()) { uiText("help.unsupported") }
    val desktop = Desktop.getDesktop()
    check(desktop.isSupported(Desktop.Action.BROWSE)) { uiText("help.noBrowser") }
    desktop.browse(URI(url))
}.exceptionOrNull()?.let { uiText("help.failed" , it.message ?: it::class.simpleName) }

private data class ProjectTreeDisplayFile(val path: String, val file: File, val supportsDescription: Boolean)

@Composable
private fun ProjectHorizontalSplitter(onDelta: (androidx.compose.ui.unit.Dp) -> Unit, onFinished: () -> Unit) {
    val density = LocalDensity.current
    val currentDelta by androidx.compose.runtime.rememberUpdatedState(onDelta)
    val currentFinished by androidx.compose.runtime.rememberUpdatedState(onFinished)
    Box(modifier = Modifier.fillMaxWidth().height(PROJECT_TREE_SPLITTER_HEIGHT)
        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
        .pointerInput(density) {
            detectDragGestures(onDragEnd = { currentFinished() }, onDragCancel = { currentFinished() }) { change, amount ->
                change.consume()
                currentDelta(with(density) { amount.y.toDp() })
            }
        }, contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth().height(1.dp)) {}
    }
}

@Composable
private fun ProjectTreePanel(
    tree: DesktopProjectTreeSnapshot,
    selectedPath: String?,
    onSelected: (String) -> Unit,
    onEdit: (File) -> Unit,
    onCreateWav: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sourceInformationHeight by remember {
        mutableStateOf(
            DesktopUiPreferences.sourceInformationHeight(DEFAULT_SOURCE_INFORMATION_HEIGHT.value)
                .dp.coerceIn(MIN_SOURCE_INFORMATION_HEIGHT, MAX_SOURCE_INFORMATION_HEIGHT),
        )
    }
    val tracked = tree.tracked.map {
        ProjectTreeDisplayFile(
            path = it.definition.path,
            file = it.file,
            supportsDescription = it.definition.type != ProjectSourceType.RAW_BINARY,
        )
    }
    val untracked = tree.untracked.map {
        ProjectTreeDisplayFile(
            path = it.relativePath,
            file = it.file,
            supportsDescription = it.file.extension.lowercase() in setOf("bas", "asm", "dmp"),
        )
    }
    val selected = (tracked + untracked).firstOrNull { it.path == selectedPath }
    val description = tree.sourceDescriptions[selected?.path].orEmpty()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val maximumInformationHeight = maxOf(24.dp, maxHeight * 0.3f)
    val effectiveInformationHeight = sourceInformationHeight.coerceIn(24.dp, maximumInformationHeight)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(uiText("project.files"), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { selected?.let { onEdit(it.file) } },
                enabled = selected?.let { it.supportsDescription && it.file.isFile } == true,
            ) { Text(uiText("project.edit")) }
        }
        OutlinedButton(
            onClick = { selected?.let { onCreateWav(it.file) } },
            enabled = selected?.let { it.file.isFile && it.file.extension.lowercase() in setOf("bas", "dmp") } == true,
            modifier = Modifier.align(Alignment.End),
        ) { Text(uiText("project.wav")) }
        val verticalState = rememberScrollState()
        val horizontalState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(verticalState)
                    .horizontalScroll(horizontalState)
                    .padding(end = 12.dp, bottom = 12.dp),
            ) {
                ProjectTreeGroup(uiText("project.tracked"), tracked, selectedPath, onSelected)
                ProjectTreeGroup(uiText("project.untracked"), untracked, selectedPath, onSelected)
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(verticalState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontalState),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
        ProjectHorizontalSplitter(
            onDelta = { delta ->
                sourceInformationHeight = (effectiveInformationHeight - delta).coerceIn(
                    MIN_SOURCE_INFORMATION_HEIGHT, maximumInformationHeight,
                )
            },
            onFinished = { DesktopUiPreferences.saveSourceInformationHeight(sourceInformationHeight.value) },
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(effectiveInformationHeight),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(uiText("project.sourceInformation"), style = MaterialTheme.typography.titleSmall)
            val descriptionScrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Text(
                    when {
                        selected == null -> uiText("project.selectSource")
                        !selected.supportsDescription -> uiText("project.noInformationType")
                        description.isBlank() -> uiText("project.noDescription")
                        else -> description
                    },
                    modifier = Modifier.fillMaxWidth().verticalScroll(descriptionScrollState).padding(end = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(descriptionScrollState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }
}
}

@Composable
private fun ProjectTreeGroup(
    name: String,
    files: List<ProjectTreeDisplayFile>,
    selectedPath: String?,
    onSelected: (String) -> Unit,
) {
    Text(uiText("project.groupCount", name, files.size.toString()))
    files.forEach { source ->
        Text(
            "  ${source.path}",
            modifier = Modifier
                .background(
                    if (source.path == selectedPath) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent,
                )
                .semantics {
                    contentDescription = uiText("project.selectPath", source.path)
                    selected = source.path == selectedPath
                }
                .clickable { onSelected(source.path) }
                .padding(vertical = 2.dp, horizontal = 3.dp),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
internal fun AssemblyOutputField(content: String, strings: StudioStrings = StudioStrings.system) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(strings.text("assembly.result"), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { copyToClipboard(content) }) { Text(strings.text("common.copyAll")) }
    }
    val lines = remember(content) { content.ifBlank {
        strings.text("assembly.noResult")
    }.lines() }
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    val style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textWidth = remember(lines, style, density) {
        with(density) { measurer.measure(lines.maxByOrNull { it.length }.orEmpty(), style,
            softWrap = false).size.width.toDp() } + 2.dp
    }
    LaunchedEffect(content) {
        verticalState.scrollToItem(0)
        horizontalState.scrollTo(0)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(end = 12.dp, bottom = 12.dp)) {
                val lineWidth = maxOf(maxWidth, textWidth)
                Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalState)) {
                    LazyColumn(state = verticalState, modifier = Modifier.width(lineWidth).fillMaxHeight()) {
                        items(lines) { line ->
                            Text(line.ifEmpty { " " }, style = style, softWrap = false, maxLines = 1)
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(verticalState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(bottom = 12.dp),
            )
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontalState),
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun AssemblyPreviewSection(title: String, content: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Text(
        content.ifBlank { uiText("common.empty") },
        modifier = Modifier.fillMaxWidth().widthIn(max = 1100.dp),
        fontFamily = FontFamily.Monospace,
    )
}

internal fun DesktopBasicLoadError.message(machineName: String, strings: StudioStrings = StudioStrings.system): String = when (this) {
    DesktopBasicLoadError.InvalidUtf8 -> strings.text("project.error.basicUtf8")
    is DesktopBasicLoadError.Parse ->
        strings.text("project.error.basicParse", (error.line).toString(), (error.column).toString(), strings.transferDetail(error.message))
    is DesktopBasicLoadError.UnsupportedRomInput -> {
        val detail = when (val value = error.unsupported) {
            is Pc1245RomInputUnsupported.Character -> strings.text("basic.unsupportedCharacter", value.value.toString())
            is Pc1245RomInputUnsupported.SpecialSymbol -> strings.text("basic.unsupportedSymbol", value.value.toString())
            is Pc1245RomInputUnsupported.RawByte -> strings.text("basic.unsupportedByte", value.value.hex(2))
        }
        strings.text("basic.unsupportedInput", machineName, detail, error.line.toString(), error.column.toString())
    }
    is DesktopBasicLoadError.UnsupportedMachine ->
        strings.text("basic.unsupportedMachine", machineName)
}

private fun CoreFault.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is CoreFault.UnsupportedOpcode ->
        strings.text("main.unsupportedOpcode", opcode.hex(2), address.hex(4))
}

internal fun DesktopBasicProgramCompileError.message(machineName: String, strings: StudioStrings = StudioStrings.system): String = when (this) {
    DesktopBasicProgramCompileError.InvalidUtf8 -> strings.text("project.error.basicUtf8")
    is DesktopBasicProgramCompileError.Parse ->
        strings.text("project.error.basicParse", (error.line).toString(), (error.column).toString(), strings.transferDetail(error.message))
    is DesktopBasicProgramCompileError.Tokenize ->
        strings.text("project.error.oldBasic", (machineName), (error.line).toString(), (error.column).toString(), strings.transferDetail(error.message))
    is DesktopBasicProgramCompileError.S1Tokenize ->
        strings.text("project.error.s1Basic", (machineName), (error.line).toString(), (error.column).toString(), strings.transferDetail(error.message))
    is DesktopBasicProgramCompileError.S2Tokenize ->
        strings.text("project.error.s2Basic", (machineName), (error.line).toString(), (error.column).toString(), strings.transferDetail(error.message))
    is DesktopBasicProgramCompileError.UnsupportedMachine ->
        strings.text("project.error.basicUnsupported", (machineName))
}

internal fun BasicProgramMemoryError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is BasicProgramMemoryError.InvalidPointer ->
        strings.text("project.error.basicPointer", (startAddress.hex(4)), (endAddress.hex(4)))
    is BasicProgramMemoryError.ProgramTooLarge ->
        strings.text("project.error.basicCapacity", (size).toString(), (capacity).toString())
    is BasicProgramMemoryError.InvalidProgram ->
        strings.text("project.error.basicInvalid", (offset).toString(), strings.transferDetail(reason))
    is BasicProgramMemoryError.UnsupportedMachine ->
        strings.text("project.error.basicMemoryUnsupported", (machineId.value))
}

internal fun DesktopMemoryDumpLoadError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    DesktopMemoryDumpLoadError.InvalidUtf8 -> strings.text("project.error.dumpUtf8")
    is DesktopMemoryDumpLoadError.Parse -> when (val detail = error) {
        PgpMemoryDumpError.Empty -> strings.text("project.error.emptyDump")
        is PgpMemoryDumpError.Syntax ->
            strings.text("project.error.dumpSyntax", (detail.line).toString(), (detail.column).toString(), strings.transferDetail(detail.message))
        is PgpMemoryDumpError.AddressOverflow ->
            strings.text("project.error.dumpOverflow", (detail.line).toString())
        is PgpMemoryDumpError.Overlap ->
            strings.text("project.error.dumpOverlap", (detail.line).toString(), (detail.previousLine).toString(), (detail.address.hex(4)))
    }
}

internal fun DesktopProjectOpenError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is DesktopProjectOpenError.CouldNotReadManifest -> strings.text("project.error.read", (message))
    DesktopProjectOpenError.InvalidUtf8 -> strings.text("project.error.utf8")
    is DesktopProjectOpenError.InvalidManifest -> errors.joinToString("; ") { it.message(strings) }
}

internal fun DesktopProjectCreateError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is DesktopProjectCreateError.DestinationExists -> strings.text("project.error.exists", (path))
    is DesktopProjectCreateError.CouldNotCreate -> strings.text("project.error.create", (message))
}

internal fun ProjectManifestError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is ProjectManifestError.InvalidJson -> strings.text("project.error.json", (message))
    is ProjectManifestError.UnsupportedFormat -> strings.text("project.error.format", (actual))
    is ProjectManifestError.UnsupportedVersion -> strings.text("project.error.version", (actual).toString())
    ProjectManifestError.BlankName -> strings.text("project.blankName")
    is ProjectManifestError.UnsupportedMachine -> strings.text("project.error.machine", (actual))
    is ProjectManifestError.InvalidSourceId -> strings.text("project.error.sourceId", (sourceIndex).toString(), (actual))
    is ProjectManifestError.DuplicateSourceId -> strings.text("project.error.duplicateId", (id))
    is ProjectManifestError.InvalidSourcePath -> strings.text("project.error.sourcePath", (sourceId), (actual))
    is ProjectManifestError.DuplicateSourcePath -> strings.text("project.error.duplicatePath", (path))
    is ProjectManifestError.InvalidLoadAddress -> strings.text("project.error.loadAddress", (sourceId), (actual))
    is ProjectManifestError.MissingLoadAddress -> strings.text("project.error.missingAddress", (sourceId))
    is ProjectManifestError.UnexpectedLoadAddress -> strings.text("project.error.unexpectedAddress", (sourceId))
}

private fun ProjectSourceType.displayName(): String = when (this) {
    ProjectSourceType.BASIC -> "BASIC"
    ProjectSourceType.MEMORY_DUMP -> uiText("project.sourceType.MEMORY_DUMP")
    ProjectSourceType.RAW_BINARY -> uiText("project.sourceType.RAW_BINARY")
    ProjectSourceType.ASSEMBLY -> uiText("project.sourceType.ASSEMBLY")
}

internal fun DesktopProjectBuildError.message(machineName: String, strings: StudioStrings = StudioStrings.system): String = when (this) {
    is DesktopProjectBuildError.MultipleBasicSources ->
        strings.text("project.error.multipleBasic", (sourceIds.joinToString()))
    is DesktopProjectBuildError.CouldNotReadSource -> strings.text("project.error.readSource", (sourceId), (message))
    is DesktopProjectBuildError.BasicCompile -> strings.text("project.error.basicSource", (sourceId), (error.message(machineName, strings)))
    is DesktopProjectBuildError.MemoryDumpParse -> strings.text("project.error.sourceDetail", (sourceId), (error.message(strings)))
    is DesktopProjectBuildError.EmptyRawBinary -> strings.text("project.error.emptyBinary", (sourceId))
    is DesktopProjectBuildError.InvalidRawBinaryRange ->
        strings.text("project.error.binaryRange", (sourceId), (startAddress.hex(4)), (size).toString())
    is DesktopProjectBuildError.MemoryOverlap ->
        strings.text("project.error.overlap", (sourceId), (previousSourceId), (address.hex(4)))
    is DesktopProjectBuildError.Placement -> strings.text("project.error.sourceDetail", (sourceId), (error.message(strings)))
    is DesktopProjectBuildError.BasicPlacement -> error.message(strings)
    is DesktopProjectBuildError.Assembly ->
        strings.text("project.error.assemblySource", sourceId, line.toString(), strings.projectOperationError(message))
}

internal fun MemoryImageLoadError.message(strings: StudioStrings = StudioStrings.system): String = when (this) {
    is MemoryImageLoadError.ReadOnlyAddress ->
        strings.text("project.error.readOnly", (sourceLine).toString(), (address.hex(4)))
    is MemoryImageLoadError.MirrorOverlap ->
        strings.text("project.error.mirrorOverlap", (sourceLine).toString(), (address.hex(4)), (previousAddress.hex(4)), (physicalAddress.hex(4)))
    is MemoryImageLoadError.BasicOverlap ->
        strings.text("project.error.basicOverlap", (sourceLine).toString(), (address.hex(4)))
}

private fun DesktopRomLoadError.message(): String = StudioStrings.system.romLoadError(this)

private const val FRAME_DELAY_MILLISECONDS: Long = 16L
private const val PROJECT_SCAN_DELAY_MILLISECONDS: Long = 500L
private const val AUTOMATIC_ROM_BOOT_CYCLES: Long = 1_000_000L
private const val CPU_REFRESH_FRAME_INTERVAL: Int = 6
private const val DISASSEMBLY_LINE_COUNT: Int = 10
private const val DISASSEMBLY_BYTE_COLUMN_WIDTH: Int = 11
private const val DEFAULT_MEMORY_VIEW_ADDRESS: Int = 0xc000
private const val MEMORY_VIEW_LINE_COUNT: Int = 16
private const val MEMORY_VIEW_PAGE_SIZE: Int =
    DesktopMemoryViewModel.BYTES_PER_LINE * MEMORY_VIEW_LINE_COUNT
private const val INSTRUCTION_TRACE_VISIBLE_LINES: Int = 32
private const val LCD_DOT_INSET_RATIO: Float = 0.14f
private const val LCD_PANEL_ASPECT_RATIO: Float = 95f / 11f
private const val SOFTWARE_KEY_ANIMATION_MILLISECONDS: Int = 70
private val DEFAULT_PROJECT_PANE_WIDTH = 260.dp
private val MIN_PROJECT_PANE_WIDTH = 220.dp
private val MAX_PROJECT_PANE_WIDTH = 520.dp
private val PROJECT_PANE_RESIZE_HANDLE_WIDTH = 12.dp
private val DEFAULT_SOURCE_INFORMATION_HEIGHT = 160.dp
private val MIN_SOURCE_INFORMATION_HEIGHT = 24.dp
private val MAX_SOURCE_INFORMATION_HEIGHT = 420.dp
private val PROJECT_TREE_SPLITTER_HEIGHT = 10.dp
private val SCROLLBAR_CONTENT_GUTTER = 10.dp
private const val REPOSITORY_URL: String = "https://github.com/digihori/pokecom-go-plus"
private const val GETTING_STARTED_URL: String = "$REPOSITORY_URL/blob/main/docs/GETTING_STARTED.md"
private const val PROJECTS_HELP_URL: String = "$REPOSITORY_URL/blob/main/docs/PROJECTS.md#command-history"
private const val DEBUGGER_HELP_URL: String = "$REPOSITORY_URL/blob/main/docs/DEBUGGER.md"

private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
    .toString(16)
    .uppercase()
    .padStart(width, '0')

internal fun parseHexAddress(value: String): Int? {
    val normalized = value.trim().removePrefix("0x").removePrefix("0X")
    if (normalized.isEmpty() || normalized.length > 4) return null
    return normalized.toIntOrNull(16)?.takeIf { it in 0..0xffff }
}

private fun Boolean.bit(): Int = if (this) 1 else 0

private fun MachineId.displayName(): String = MachineCatalog.find(this)?.displayName ?: value.uppercase()

private enum class ProjectRuntimeStatus {
    NOT_BUILT, BUILT_NOT_LOADED, NOT_LOADED, UP_TO_DATE, SOURCE_CHANGED, BUILD_FAILED;

    val displayName: String get() = uiText("project.state.$name")
}
