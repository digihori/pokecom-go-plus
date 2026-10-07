package com.digihori.pgp.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
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
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadError
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpWriter
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
import com.digihori.pgp.desktop.project.DesktopProjectManifestUpdater
import com.digihori.pgp.desktop.project.DesktopProjectUpdateResult
import com.digihori.pgp.desktop.project.DesktopAssemblyDiagnostic
import com.digihori.pgp.desktop.project.DesktopAssemblySourceResult
import com.digihori.pgp.desktop.project.DesktopAssemblyWorkspaceCompiler
import com.digihori.pgp.desktop.project.DesktopAssemblyWorkspaceResult
import com.digihori.pgp.desktop.project.DesktopExternalEditor
import com.digihori.pgp.desktop.project.DesktopExternalEditorResult
import com.digihori.pgp.desktop.project.DesktopProjectBuildOutputWriter
import com.digihori.pgp.desktop.project.renderDisassembly
import com.digihori.pgp.desktop.project.renderListing
import com.digihori.pgp.desktop.project.renderMap
import com.digihori.pgp.desktop.project.renderMemorySummary
import com.digihori.pgp.desktop.input.Pc1245KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1251KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1350KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1360KeyboardLayout
import com.digihori.pgp.desktop.input.PocketKeyCap
import com.digihori.pgp.desktop.audio.DesktopAudioPlayer
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
import com.digihori.pgp.desktop.rom.DesktopRomPackageConverter
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import java.awt.FileDialog
import java.awt.Frame
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.time.Instant
import javax.swing.JFileChooser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

fun main() {
    configureSkikoRenderApi()
    application {
        val keyboardInput = remember { DesktopKeyboardInput() }
        Window(
            onCloseRequest = {
                keyboardInput.attach(null)
                exitApplication()
            },
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
            App(keyboardInput, window)
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
private fun FrameWindowScope.App(keyboardInput: DesktopKeyboardInput, ownerWindow: Frame) {
    var runner by remember { mutableStateOf<DesktopEmulatorRunner?>(null) }
    var selectedMachineId by remember { mutableStateOf(MachineCatalog.defaultDefinition.id) }
    var runnerState by remember { mutableStateOf(RunnerState.PAUSED) }
    var loadedRomName by remember { mutableStateOf<String?>(null) }
    var loadedRomFile by remember { mutableStateOf<File?>(null) }
    var familyMemoryMode by remember { mutableStateOf(Pc1251FamilyMemoryMode.EXPANDED) }
    var message by remember { mutableStateOf("Select a PC-1245 legacy 32/64 KiB ROM image.") }
    var executedCycles by remember { mutableLongStateOf(0L) }
    var display by remember { mutableStateOf<DisplaySnapshot?>(null) }
    var cpu by remember { mutableStateOf<CpuSnapshot?>(null) }
    var changedCpuFields by remember { mutableStateOf(emptySet<CpuField>()) }
    var debuggerStopReason by remember { mutableStateOf<DebuggerStopReason?>(null) }
    var showDebuggerWindow by remember { mutableStateOf(false) }
    var memoryViewStartAddress by remember { mutableStateOf(DEFAULT_MEMORY_VIEW_ADDRESS) }
    var operatingMode by remember { mutableStateOf(OperatingMode.RUN) }
    var requestedToneHz by remember { mutableStateOf(0) }
    var showCreateRomSetGuide by remember { mutableStateOf(false) }
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
    var focusRestoreRequest by remember { mutableLongStateOf(0L) }
    var projectWorkspace by remember { mutableStateOf<DesktopProjectWorkspace?>(null) }
    var projectChangeTracker by remember { mutableStateOf<DesktopProjectChangeTracker?>(null) }
    var projectTree by remember { mutableStateOf<DesktopProjectTreeSnapshot?>(null) }
    var projectBasicBaseline by remember { mutableStateOf<ByteArray?>(null) }
    var projectBasicModified by remember { mutableStateOf(false) }
    var changedProjectSources by remember { mutableStateOf(emptySet<String>()) }
    var changedAssemblySources by remember { mutableStateOf(emptySet<String>()) }
    var projectRuntimeStatus by remember { mutableStateOf(ProjectRuntimeStatus.NOT_BUILT) }
    var showProjectWindow by remember { mutableStateOf(false) }
    var showAssemblyWorkspace by remember { mutableStateOf(false) }
    var assemblyPreview by remember { mutableStateOf(emptyList<DesktopAssemblySourceResult>()) }
    var assemblyDiagnostics by remember { mutableStateOf(emptyList<DesktopAssemblyDiagnostic>()) }
    var assemblyStatus by remember { mutableStateOf("Not assembled") }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("New PGP Project") }
    var newProjectStarters by remember { mutableStateOf(setOf(DesktopProjectStarter.BASIC)) }
    var newProjectError by remember { mutableStateOf<String?>(null) }
    var requestProjectParentSelection by remember { mutableStateOf(false) }
    var machineMenuExpanded by remember { mutableStateOf(false) }
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
                val bankImage = if (MachineCatalog.require(selectedMachineId).family == MachineFamily.PC_1360) {
                    File(file.parentFile, "pc1360bank.bin").readBytes()
                } else null
                DesktopRomLoader.loadLegacyImage(selectedMachineId, bytes, configuration, bankImage)
            }
        }
            .getOrElse {
                if (reportErrors) showError("Could not read ROM: ${it.message ?: it::class.simpleName}")
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
                val newRunner = DesktopEmulatorRunner(result.session)
                selectedMachineId = result.session.machineId
                runner = newRunner
                keyboardInput.attach(newRunner)
                loadedRomName = file.name
                loadedRomFile = file.absoluteFile
                executedCycles = bootResult?.executedCycles ?: 0L
                display = newRunner.displaySnapshot()
                cpu = newRunner.cpuSnapshot()
                changedCpuFields = emptySet()
                debuggerStopReason = newRunner.stopReason
                operatingMode = OperatingMode.RUN
                requestedToneHz = 0
                romHistory.remember(file, result.session.machineId)
                if (startAutomatically) newRunner.run()
                runnerState = newRunner.state
                message = if (startAutomatically) {
                    "ROM loaded automatically and running."
                } else {
                    "ROM loaded. Press Run to start."
                }
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
                val bankImage = if (MachineCatalog.require(targetMachineId).family == MachineFamily.PC_1360) {
                    File(file.parentFile, "pc1360bank.bin").readBytes()
                } else null
                when (val converted = DesktopRomPackageConverter.convertLegacyImage(targetMachineId, source, bankImage)) {
                    is DesktopRomPackageConversionResult.Success -> converted.packageBytes
                    is DesktopRomPackageConversionResult.Failure ->
                        error(DesktopRomLoadError.InvalidRom(converted.error).message())
                }
            }
        }.getOrElse {
            if (reportErrors) showError("Could not import ROM: ${it.message ?: it::class.simpleName}")
            return false
        }
        return when (val installed = romLibrary.install(packageBytes, file.name)) {
            is DesktopRomLibraryResult.Failure -> {
                if (reportErrors) showError("Could not store ROM: ${installed.message}")
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
            showError("Could not remove the stored ${machineId.displayName()} ROM.")
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
            loadedRomName = null
            loadedRomFile = null
            runnerState = RunnerState.PAUSED
        }
        message = "Removed the stored ${machineId.displayName()} ROM."
    }

    fun createRomSet() {
        val internalFile = selectAndRestoreFocus { selectInternalRomFile(ownerWindow, selectedMachineId) } ?: return
        val externalFile = selectAndRestoreFocus { selectExternalRomFile(ownerWindow, selectedMachineId) } ?: return
        val conversion = runCatching {
            DesktopRomPackageConverter.createPackage(
                selectedMachineId,
                internalFile.readBytes(),
                externalFile.readBytes(),
            )
        }.getOrElse {
            showError("Could not read ROM files: ${it.message ?: it::class.simpleName}")
            return
        }
        when (conversion) {
            is DesktopRomPackageConversionResult.Failure -> {
                showError(DesktopRomLoadError.InvalidRom(conversion.error).message())
            }
            is DesktopRomPackageConversionResult.Success -> {
                val destination = selectAndRestoreFocus { selectPackageDestination(ownerWindow) } ?: return
                runCatching { destination.writeBytes(conversion.packageBytes) }
                    .onSuccess {
                        message = "Created ${destination.name}. Select it with Open ROM to verify the package."
                    }
                    .onFailure {
                        showError("Could not write package: ${it.message ?: it::class.simpleName}")
                    }
            }
        }
    }

    fun openProject(file: File, resetBasicTracking: Boolean = true) {
        when (val opened = DesktopProjectWorkspaceLoader.open(file)) {
            is DesktopProjectOpenResult.Failure -> showError(opened.errors.joinToString("\n") { it.message() })
            is DesktopProjectOpenResult.Success -> {
                projectWorkspace = opened.workspace
                projectChangeTracker = DesktopProjectChangeTracker(opened.workspace)
                projectTree = DesktopProjectTreeScanner.scan(opened.workspace)
                changedProjectSources = emptySet()
                changedAssemblySources = emptySet()
                assemblyPreview = emptyList()
                assemblyDiagnostics = emptyList()
                assemblyStatus = "Not assembled"
                projectRuntimeStatus = ProjectRuntimeStatus.NOT_BUILT
                if (resetBasicTracking) {
                    projectBasicBaseline = null
                    projectBasicModified = false
                }
                selectedMachineId = opened.workspace.definition.machineId
                showProjectWindow = true
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
                    "Opened project ${opened.workspace.definition.name} ($sourceSummary)."
                } else {
                    "Opened project ${opened.workspace.definition.name} ($sourceSummary). " +
                        "Register the ${projectMachineId.displayName()} ROM before Build & Load."
                }
            }
        }
    }

    fun updateProject() {
        val workspace = projectWorkspace ?: return
        val tree = DesktopProjectTreeScanner.scan(workspace)
        when (val result = DesktopProjectManifestUpdater.synchronize(workspace, tree)) {
            is DesktopProjectUpdateResult.Success -> {
                openProject(workspace.manifestFile, resetBasicTracking = false)
                val skipped = if (result.skippedPaths.isEmpty()) "" else {
                    " Skipped unsupported files: ${result.skippedPaths.joinToString()}."
                }
                message = "Updated project: ${result.addedCount} added, ${result.removedCount} removed.$skipped"
            }
            is DesktopProjectUpdateResult.CouldNotWrite -> showError(
                "Could not update project manifest: ${result.message}",
            )
        }
    }

    fun assembleWorkspace() {
        val workspace = projectWorkspace ?: return
        when (val result = DesktopAssemblyWorkspaceCompiler.compile(workspace)) {
            is DesktopAssemblyWorkspaceResult.Failure -> {
                assemblyDiagnostics = result.diagnostics
                assemblyStatus = "Assembly failed — last successful preview retained"
                message = "Assembly failed with ${result.diagnostics.size} diagnostic(s)."
            }
            is DesktopAssemblyWorkspaceResult.Success -> {
                assemblyPreview = result.sources
                assemblyDiagnostics = emptyList()
                changedAssemblySources = emptySet()
                val bytes = result.sources.sumOf { it.image.byteCount }
                assemblyStatus = "Assembled — $bytes bytes from ${result.sources.size} source(s)"
                message = assemblyStatus
            }
        }
    }

    fun openExternalSource(file: File, line: Int? = null) {
        when (val opened = DesktopExternalEditor.open(file, line)) {
            DesktopExternalEditorResult.Opened -> message = "Opened ${file.name}."
            is DesktopExternalEditorResult.Failed -> showError(
                "Could not open external editor: ${opened.message}",
            )
        }
    }

    fun buildProjectOutputs(): DesktopProjectBuildResult.Success? {
        val workspace = projectWorkspace ?: return null
        return when (val built = DesktopProjectBuilder.build(workspace)) {
            is DesktopProjectBuildResult.Failure -> {
                projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                when (val assembly = DesktopAssemblyWorkspaceCompiler.compile(workspace)) {
                    is DesktopAssemblyWorkspaceResult.Failure -> {
                        assemblyDiagnostics = assembly.diagnostics
                        assemblyStatus = "Build failed — last successful preview retained"
                    }
                    is DesktopAssemblyWorkspaceResult.Success -> Unit
                }
                val machineName = runner?.machineId?.displayName() ?: workspace.definition.machineId.displayName()
                showError(built.errors.joinToString("\n") { it.message(machineName) })
                null
            }
            is DesktopProjectBuildResult.Success -> {
                val output = runCatching { DesktopProjectBuildOutputWriter.write(workspace, built.artifact) }
                    .getOrElse {
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        showError("Could not write build output: ${it.message ?: it::class.simpleName}")
                        return null
                    }
                assemblyPreview = built.artifact.assemblySources
                assemblyDiagnostics = emptyList()
                changedAssemblySources = emptySet()
                assemblyStatus = "Built${output?.let { " — ${it.path}" }.orEmpty()}"
                message = assemblyStatus
                built
            }
        }
    }

    fun buildProject() {
        val workspace = projectWorkspace ?: return
        val activeRunner = runner ?: return
        if (activeRunner.machineId != workspace.definition.machineId) {
            projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
            showError(
                "Project targets ${workspace.definition.machineId.displayName()}, but the loaded " +
                    "ROM is ${activeRunner.machineId.displayName()}.",
            )
            return
        }
        buildProjectOutputs()?.let { built ->
                when (val applied = DesktopProjectArtifactLoader.load(activeRunner, built.artifact)) {
                    is DesktopProjectApplyResult.BasicFailure -> {
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        showError(applied.error.message())
                    }
                    is DesktopProjectApplyResult.MemoryFailure -> {
                        projectRuntimeStatus = ProjectRuntimeStatus.BUILD_FAILED
                        showError(applied.error.message())
                    }
                    is DesktopProjectApplyResult.Success -> {
                        projectRuntimeStatus = ProjectRuntimeStatus.UP_TO_DATE
                        changedProjectSources = emptySet()
                        projectChangeTracker = DesktopProjectChangeTracker(workspace)
                        val summary = "${applied.basicByteCount} BASIC bytes, " +
                            "${applied.memoryByteCount} machine-code bytes in " +
                            "${applied.memorySegmentCount} segments"
                        message = "Built ${workspace.definition.name}: $summary. " +
                            "Start it from the pocket-computer keyboard."
                        runnerState = activeRunner.state
                        display = activeRunner.displaySnapshot()
                        cpu = activeRunner.cpuSnapshot()
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

    fun openProjectFromDisk() {
        selectAndRestoreFocus(restoreMainWindowFocus = false) { selectProjectDirectory(ownerWindow) }
            ?.let { File(it, DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME) }
            ?.let { openProject(it) }
    }

    fun loadBasicFromDisk() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, "Load BASIC source") } ?: return
        val compiled = runCatching {
            DesktopBasicLoader.compileProgram(file.readBytes(), activeRunner.machineId)
        }.getOrElse {
            showError("Could not read BASIC source: ${it.message ?: it::class.simpleName}")
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
                    projectBasicBaseline = null
                    projectBasicModified = false
                    runnerState = activeRunner.state
                    display = activeRunner.displaySnapshot()
                    cpu = activeRunner.cpuSnapshot()
                    message = "Loaded ${file.name} directly (${loaded.size} bytes at " +
                        "0x${loaded.startAddress.hex(4)}..0x${loaded.endAddress.hex(4)})."
                    if (projectWorkspace != null) projectRuntimeStatus = ProjectRuntimeStatus.RUNTIME_MODIFIED
                }
            }
        }
    }

    fun loadMachineCodeFromDisk() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectMemoryDumpFile(ownerWindow) } ?: return
        val parsed = runCatching { DesktopMemoryDumpLoader.parse(file.readBytes()) }.getOrElse {
            showError("Could not read memory dump: ${it.message ?: it::class.simpleName}")
            return
        }
        when (parsed) {
            is DesktopMemoryDumpLoadResult.Failure -> showError(parsed.error.message())
            is DesktopMemoryDumpLoadResult.Success -> {
                when (val loaded = activeRunner.loadMemoryImage(parsed.image)) {
                    is MemoryImageLoadResult.Failure -> showError(loaded.error.message())
                    is MemoryImageLoadResult.Success -> {
                        message = "Loaded ${file.name} (${loaded.byteCount} bytes in " +
                            "${loaded.segmentCount} segments)."
                        if (projectWorkspace != null) projectRuntimeStatus = ProjectRuntimeStatus.RUNTIME_MODIFIED
                    }
                }
                runnerState = activeRunner.state
                display = activeRunner.displaySnapshot()
                cpu = activeRunner.cpuSnapshot()
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
                    "BASIC program error at byte ${decoded.error.offset}: ${decoded.error.message}",
                )
                is DesktopBasicProgramDecodeResult.GenericFailure -> showError(
                    "BASIC program error at byte ${decoded.offset}: ${decoded.message}",
                )
                is DesktopBasicProgramDecodeResult.Success -> {
                    val file = selectAndRestoreFocus { selectBasicSaveFile(ownerWindow) } ?: return
                    runCatching { file.writeBytes(decoded.utf8Bytes) }
                        .onSuccess { message = "Saved BASIC source to ${file.name}." }
                        .onFailure { showError("Could not save BASIC source: ${it.message ?: it::class.simpleName}") }
                }
            }
        }
    }

    fun typeBasicThroughRom() {
        val activeRunner = runner ?: return
        val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, "Type BASIC through ROM") } ?: return
        val loadResult = runCatching {
            DesktopBasicLoader.compileRomInput(file.readBytes(), activeRunner.machineId)
        }.getOrElse {
            showError("Could not read BASIC source: ${it.message ?: it::class.simpleName}")
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
                requestedToneHz = activeRunner.audioSnapshot().frequencyHz
                message = if (runResult.status is ExecutionStatus.Faulted) {
                    "BASIC loading stopped: ${runResult.status}"
                } else {
                    "Merged ${file.name} through the ${activeRunner.machineId.displayName()} ROM " +
                        "(${loadResult.keys.size} key taps)."
                }
                if (runResult.status !is ExecutionStatus.Faulted && projectWorkspace != null) {
                    projectRuntimeStatus = ProjectRuntimeStatus.RUNTIME_MODIFIED
                }
            }
        }
    }

    fun quickAssemble() {
        val sourceFile = selectAndRestoreFocus { selectAssemblyFile(ownerWindow) } ?: return
        val assembled = runCatching { Sc61860Assembler.assemble(sourceFile.readText()) }.getOrElse {
            showError("Could not read assembly source: ${it.message ?: it::class.simpleName}")
            return
        }
        when (assembled) {
            is Sc61860AssemblyResult.Failure -> showError(
                "Assembly error at line ${assembled.line}: ${assembled.message}",
            )
            is Sc61860AssemblyResult.Success -> {
                val destination = selectAndRestoreFocus { selectAssembledDumpDestination(ownerWindow) } ?: return
                runCatching { destination.writeBytes(DesktopMemoryDumpWriter.write(assembled.image)) }
                    .onSuccess { message = "Assembled ${assembled.image.byteCount} bytes into ${destination.name}." }
                    .onFailure { showError("Could not save assembled output: ${it.message ?: it::class.simpleName}") }
            }
        }
    }

    fun runEmulator() {
        runner?.run()
        runnerState = runner?.state ?: RunnerState.PAUSED
        changedCpuFields = emptySet()
        debuggerStopReason = runner?.stopReason
        message = "Running at normal speed."
    }

    fun pauseEmulator() {
        runner?.pause()
        audioPlayer.stop()
        runnerState = runner?.state ?: RunnerState.PAUSED
        cpu = runner?.cpuSnapshot()
        changedCpuFields = emptySet()
        debuggerStopReason = runner?.stopReason
        message = "Paused."
    }

    fun resetEmulator() {
        runner?.reset()
        audioPlayer.stop()
        runnerState = runner?.state ?: RunnerState.PAUSED
        executedCycles = 0L
        display = runner?.displaySnapshot()
        cpu = runner?.cpuSnapshot()
        changedCpuFields = emptySet()
        debuggerStopReason = runner?.stopReason
        operatingMode = OperatingMode.RUN
        requestedToneHz = 0
        projectBasicBaseline = null
        projectBasicModified = false
        message = "Reset complete."
    }

    fun stepEmulator() {
        val before = runner?.cpuSnapshot()
        val result = runner?.step() ?: return
        runnerState = runner?.state ?: RunnerState.PAUSED
        executedCycles += result.cycles
        display = runner?.displaySnapshot()
        val after = runner?.cpuSnapshot()
        changedCpuFields = CpuSnapshotDifference.changed(before, after)
        cpu = after
        debuggerStopReason = runner?.stopReason
        requestedToneHz = runner?.audioSnapshot()?.frequencyHz ?: 0
        message = "Executed one instruction (${result.cycles} cycles)."
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
                "Expanded RAM selected. It will apply when a ROM is loaded."
            } else {
                "Hardware RAM selected. It will apply when a ROM is loaded."
            }
        } else if (loadRom(file, resume, memoryMode = mode, machineIdOverride = runner?.machineId)) {
            message = if (mode == Pc1251FamilyMemoryMode.EXPANDED) {
                "Restarted with expanded PC-1255-size RAM."
            } else {
                "Restarted with ${selectedMachineId.displayName()} hardware RAM."
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
            delay(FRAME_DELAY_MILLISECONDS)
            val activeRunner = runner ?: continue
            val tick = activeRunner.tick()
            executedCycles += tick.runResult?.executedCycles ?: 0L
            runnerState = activeRunner.state
            debuggerStopReason = activeRunner.stopReason
            val latestDisplay = activeRunner.displaySnapshot()
            if (display?.revision != latestDisplay.revision || display?.enabled != latestDisplay.enabled) {
                display = latestDisplay
            }
            if (++cpuRefreshCounter >= CPU_REFRESH_FRAME_INTERVAL) {
                cpu = activeRunner.cpuSnapshot()
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
            if (activeRunner.state == RunnerState.RUNNING) {
                audioPlayer.write(activeRunner.drainAudioSamples()).onFailure {
                    message = "Audio output unavailable: ${it.message ?: it::class.simpleName}"
                }
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
                message = "Could not check project files: ${it.message ?: it::class.simpleName}"
                continue
            }
            projectWorkspace?.let { workspace ->
                projectTree = runCatching {
                    withContext(Dispatchers.IO) { DesktopProjectTreeScanner.scan(workspace) }
                }.getOrElse {
                    message = "Could not refresh project tree: ${it.message ?: it::class.simpleName}"
                    projectTree
                }
            }
            if (changes.isNotEmpty()) {
                changedProjectSources = changedProjectSources + changes.map { it.source.definition.id }
                changedAssemblySources = changedAssemblySources + changes
                    .filter { it.source.definition.type == ProjectSourceType.ASSEMBLY }
                    .map { it.source.definition.id }
                projectRuntimeStatus = ProjectRuntimeStatus.SOURCE_CHANGED
                message = "External changes detected: " + changes.joinToString { change ->
                    "${change.source.definition.path} (${change.kind.name.lowercase()})"
                }
            }
        }
    }

    LaunchedEffect(showCreateProjectDialog) {
        keyboardInput.setEnabled(!showCreateProjectDialog)
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
            newProjectError = "Could not choose project folder: ${error.message ?: error::class.simpleName}"
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
        Menu("File") {
            Item("Register ROM…", onClick = { showRomManager = true })
            Item("Manage ROMs…", onClick = { showRomManager = true })
            Item("Create ROM Set…", onClick = { showCreateRomSetGuide = true })
        }
        Menu("Project") {
            Item("New Project…", onClick = {
                newProjectError = null
                showCreateProjectDialog = true
            })
            Item("Open Project…", onClick = ::openProjectFromDisk)
            Item(
                "Show Project Files",
                enabled = projectWorkspace != null && !showProjectWindow,
                onClick = { showProjectWindow = true },
            )
            Item(
                "Open Assembly Workspace",
                onClick = { showAssemblyWorkspace = true },
            )
            Separator()
            Item(
                "Build & Load",
                enabled = projectWorkspace != null && runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::buildProject,
            )
            Item("Update Project", enabled = projectWorkspace != null, onClick = ::updateProject)
        }
        Menu("Program") {
            Item(
                "Load BASIC…",
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::loadBasicFromDisk,
            )
            Item(
                "Save BASIC…",
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::saveBasicToDisk,
            )
            Item(
                "Type BASIC through ROM…",
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::typeBasicThroughRom,
            )
            Separator()
            Item(
                "Load Machine Code…",
                enabled = runner != null && runnerState != RunnerState.FAULTED,
                onClick = ::loadMachineCodeFromDisk,
            )
            Item("Save Machine Code…", enabled = runner != null, onClick = {
                dumpRangeError = null
                showSaveMemoryDumpDialog = true
            })
            Item("Quick Assemble…", onClick = ::quickAssemble)
        }
        Menu("Emulator") {
            Item("Run", enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::runEmulator)
            Item("Pause", enabled = runnerState == RunnerState.RUNNING, onClick = ::pauseEmulator)
            Item("Step", enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::stepEmulator)
            Item("Reset", enabled = runner != null, onClick = ::resetEmulator)
            if (MachineCatalog.require(runner?.machineId ?: selectedMachineId).supportsConfigurableRam) {
                Separator()
                Item(
                    "Expanded RAM",
                    enabled = familyMemoryMode != Pc1251FamilyMemoryMode.EXPANDED,
                    onClick = { setMemoryMode(Pc1251FamilyMemoryMode.EXPANDED) },
                )
                Item(
                    "Hardware RAM",
                    enabled = familyMemoryMode != Pc1251FamilyMemoryMode.HARDWARE,
                    onClick = { setMemoryMode(Pc1251FamilyMemoryMode.HARDWARE) },
                )
            }
        }
        Menu("Debug") {
            Item("Open Debugger", enabled = !showDebuggerWindow, onClick = { showDebuggerWindow = true })
            Item("Save Debug Checkpoint…", enabled = runner != null && cpu != null, onClick = {
                checkpointRangeError = null
                showSaveCheckpointDialog = true
            })
        }
    }

    if (showDebuggerWindow) {
        Window(
            onCloseRequest = { showDebuggerWindow = false },
            title = "${ProjectInfo.STUDIO_DISPLAY_NAME} — Debugger",
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CpuRegisterPanel(cpu, changedCpuFields)
                        debuggerStopReason?.let { Text("Debugger: ${it.displayName()}") }
                        Button(
                            enabled = runner != null && cpu != null,
                            onClick = {
                                checkpointRangeError = null
                                showSaveCheckpointDialog = true
                            },
                        ) { Text("Save Debug Checkpoint") }
                        DisassemblyPanel(
                            runner = runner,
                            snapshot = cpu,
                            keyboardInput = keyboardInput,
                            onSaveRange = {
                                disassemblyStartAddress = (cpu?.programCounter ?: 0).hex(4)
                                disassemblyEndAddress = ((cpu?.programCounter ?: 0) + 0xff)
                                    .coerceAtMost(0xffff).hex(4)
                                disassemblyRangeError = null
                                showSaveDisassemblyDialog = true
                            },
                        )
                        InstructionTracePanel(runner, executedCycles)
                        MemoryMapPanel(
                            regions = MachineCatalog.require(runner?.machineId ?: selectedMachineId).memoryRegions,
                            onSelect = { memoryViewStartAddress = it.startAddress },
                        )
                        MemoryViewPanel(
                            runner = runner,
                            keyboardInput = keyboardInput,
                            startAddress = memoryViewStartAddress,
                            onStartAddressChange = { memoryViewStartAddress = it },
                        )
                        MemoryWatchPanel(runner, keyboardInput)
                    }
                }
            }
        }
    }

    if (showProjectWindow) {
        Window(
            onCloseRequest = { showProjectWindow = false },
            title = projectWorkspace?.let { "${ProjectInfo.STUDIO_DISPLAY_NAME} — ${it.definition.name}" }
                ?: "${ProjectInfo.STUDIO_DISPLAY_NAME} — Project Files",
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        projectWorkspace?.let { workspace ->
                            Text("Project: ${workspace.definition.name}")
                            Text("Machine: ${workspace.definition.machineId.displayName()}")
                            Text(
                                "Runtime: ${projectRuntimeStatus.displayName}" +
                                    if (changedProjectSources.isEmpty()) "" else
                                        " — changed: ${changedProjectSources.joinToString()}",
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = runner != null && runnerState != RunnerState.FAULTED,
                                    onClick = ::buildProject,
                                ) { Text("Build & Load") }
                                Button(
                                    onClick = { showAssemblyWorkspace = true },
                                ) { Text("Assembly Workspace") }
                                Button(onClick = ::updateProject) { Text("Update Project") }
                            }
                            projectTree?.let { ProjectTreePanel(it) }
                        } ?: Text("No project is open.")
                    }
                }
            }
        }
    }

    if (showAssemblyWorkspace) {
        Window(
            onCloseRequest = { showAssemblyWorkspace = false },
            title = projectWorkspace?.let { "${ProjectInfo.STUDIO_DISPLAY_NAME} — ${it.definition.name} — Assembly" }
                ?: "${ProjectInfo.STUDIO_DISPLAY_NAME} — Assembly Workspace",
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val workspace = projectWorkspace
                        if (workspace == null) {
                            Text("No project is open.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::openProjectFromDisk) { Text("Open Project…") }
                                Button(onClick = {
                                    newProjectError = null
                                    showCreateProjectDialog = true
                                }) { Text("New Project…") }
                            }
                        } else {
                            Text("Assembly Workspace", style = MaterialTheme.typography.titleLarge)
                            Text(assemblyStatus)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val hasAssemblySources = workspace.sources.any {
                                    it.definition.type == ProjectSourceType.ASSEMBLY
                                }
                                Button(enabled = hasAssemblySources, onClick = ::assembleWorkspace) {
                                    Text("Assemble")
                                }
                                Button(enabled = hasAssemblySources, onClick = { buildProjectOutputs() }) {
                                    Text("Build")
                                }
                                Button(
                                    enabled = hasAssemblySources && runner != null && runnerState != RunnerState.FAULTED,
                                    onClick = ::buildProject,
                                ) { Text("Build & Load") }
                            }
                            Text("Sources", style = MaterialTheme.typography.titleMedium)
                            val assemblySources = workspace.sources.filter {
                                it.definition.type == ProjectSourceType.ASSEMBLY
                            }
                            val untrackedAssemblyFiles = projectTree?.untracked.orEmpty().filter {
                                it.file.extension.equals("asm", ignoreCase = true)
                            }
                            if (assemblySources.isEmpty() && untrackedAssemblyFiles.isEmpty()) {
                                Text("No Assembly sources. Add an .asm file under src, then select Update Project.")
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
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Button(onClick = { openExternalSource(source.file) }) { Text("Open") }
                                }
                            }
                            untrackedAssemblyFiles.forEach { source ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("  ${source.relativePath} * (untracked)", fontFamily = FontFamily.Monospace)
                                    Button(onClick = { openExternalSource(source.file) }) { Text("Open") }
                                }
                            }
                            if (untrackedAssemblyFiles.isNotEmpty()) {
                                Button(onClick = ::updateProject) { Text("Add to Project") }
                            }
                            Text("Outputs", style = MaterialTheme.typography.titleMedium)
                            Text("  build/program.dmp\n  build/program.lst\n  build/program.map", fontFamily = FontFamily.Monospace)
                            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
                            if (assemblyDiagnostics.isEmpty()) Text("No diagnostics.")
                            assemblyDiagnostics.forEach { diagnostic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        buildString {
                                            append(diagnostic.source.definition.path).append(':')
                                                .append(diagnostic.line ?: "?").append(": ").append(diagnostic.message)
                                            diagnostic.sourceText?.let { append("\n  ").append(it) }
                                        },
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    TextButton(onClick = {
                                        openExternalSource(diagnostic.source.file, diagnostic.line)
                                    }) { Text("Open at line") }
                                }
                            }
                            assemblyPreview.forEach { result ->
                                Text(result.source.definition.path, style = MaterialTheme.typography.titleMedium)
                                AssemblyPreviewSection("Memory", result.renderMemorySummary())
                                AssemblyPreviewSection("Disassembly", result.renderDisassembly())
                                AssemblyPreviewSection("Listing", result.renderListing())
                            }
                            if (assemblyPreview.isNotEmpty()) {
                                AssemblyPreviewSection("Symbol Map", assemblyPreview.renderMap())
                            }
                        }
                    }
                }
            }
        }
    }

    MaterialTheme {
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
                    Text("Machine")
                    Box {
                        Button(onClick = { machineMenuExpanded = true }) {
                            val displayedMachine = runner?.machineId ?: selectedMachineId
                            Text(
                                displayedMachine.displayName() +
                                    if (displayedMachine in installedRomMachines) " ▾"
                                    else " — ROM not registered ▾",
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
                                            else "${definition.displayName} — ROM not registered",
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
                                text = { Text("Manage ROMs…") },
                                onClick = {
                                    machineMenuExpanded = false
                                    showRomManager = true
                                },
                            )
                        }
                    }
                    Text("ROM: ${if (runner?.machineId in installedRomMachines) "registered" else loadedRomName ?: "not loaded"}")
                    projectWorkspace?.let { Text("Project: ${it.definition.name}") }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            if (runnerState == RunnerState.RUNNING) pauseEmulator() else runEmulator()
                        },
                    ) { Text(if (runnerState == RunnerState.RUNNING) "Pause" else "Run") }
                    Button(enabled = runner != null, onClick = ::resetEmulator) { Text("Reset") }
                    Button(
                        enabled = runner != null && runnerState == RunnerState.PAUSED,
                        onClick = ::stepEmulator,
                    ) { Text("Step") }
                    val activeDefinition = MachineCatalog.require(runner?.machineId ?: selectedMachineId)
                    if (activeDefinition.family == MachineFamily.PC_1245 ||
                        activeDefinition.family == MachineFamily.PC_1251
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
                    if (activeDefinition.family == MachineFamily.PC_1251) {
                        Button(
                            enabled = runner != null && operatingMode != OperatingMode.RESERVE,
                            onClick = { setMode(OperatingMode.RESERVE) },
                        ) { Text("RSV") }
                    }
                }

                PocketLcdPanel(display)
                PocketSoftwareKeyboard(
                    runner,
                    MachineCatalog.require(runner?.machineId ?: selectedMachineId).keyboardLayout,
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(message)
                        Text(
                            buildString {
                                append("${runnerState.name}  •  $executedCycles cycles")
                                if (requestedToneHz != 0) append("  •  $requestedToneHz Hz")
                                projectWorkspace?.let {
                                    append("  •  ${projectRuntimeStatus.displayName}")
                                    if (changedProjectSources.isNotEmpty()) append("  •  Source changed")
                                }
                                debuggerStopReason?.let { append("  •  ${it.displayName()}") }
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

            }
        }

        if (showRomManager) {
            AlertDialog(
                onDismissRequest = { showRomManager = false },
                title = { Text("ROM Library") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Registered ROMs are copied into the application data folder and remain available when the original files are moved or deleted.")
                        MachineCatalog.definitions
                            .filter { definition ->
                                definition.family != MachineFamily.PC_1251 || definition.id.value == "pc-1251"
                            }
                            .forEach { definition ->
                            val installed = definition.id in installedRomMachines
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        if (definition.family == MachineFamily.PC_1251) {
                                            "PC-1250 / PC-1251 / PC-1255 family"
                                        } else {
                                            definition.displayName
                                        },
                                    )
                                    val metadata = romLibrary.metadata(definition.id)
                                    Text(
                                        if (installed) {
                                            "Registered and verified" +
                                                (metadata?.let { " — ${it.sourceName}" } ?: "")
                                        } else {
                                            "ROM not registered"
                                        },
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { chooseAndImportRom(definition.id) }) {
                                        Text(if (installed) "Replace…" else "Register…")
                                    }
                                    if (installed) {
                                        TextButton(onClick = { pendingRomRemoval = definition.id }) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRomManager = false }) { Text("Close") }
                },
            )
        }

        pendingRomRemoval?.let { machineId ->
            AlertDialog(
                onDismissRequest = { pendingRomRemoval = null },
                title = {
                    Text(
                        if (MachineCatalog.require(machineId).family == MachineFamily.PC_1251) {
                            "Remove PC-1250 / PC-1251 / PC-1255 family ROM?"
                        } else {
                            "Remove ${machineId.displayName()} ROM?"
                        },
                    )
                },
                text = { Text("The application-managed copy will be deleted. The original ROM file is not changed.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRomRemoval = null
                        removeStoredRom(machineId)
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRomRemoval = null }) { Text("Cancel") }
                },
            )
        }

        if (showCreateProjectDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjectDialog = false },
                title = { Text("Create Pokecom GO Studio Project") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Machine: ${selectedMachineId.displayName()}")
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = {
                                newProjectName = it
                                newProjectError = null
                            },
                            label = { Text("Project name") },
                            singleLine = true,
                        )
                        Text("Starter sources")
                        DesktopProjectStarter.entries.forEach { starter ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = starter in newProjectStarters,
                                    onCheckedChange = { checked ->
                                        newProjectStarters = if (checked) {
                                            newProjectStarters + starter
                                        } else {
                                            newProjectStarters - starter
                                        }
                                        newProjectError = null
                                    },
                                )
                                Text(starter.displayName)
                            }
                        }
                        Text("Existing files will not be overwritten.")
                        newProjectError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newProjectName.isBlank()) {
                            newProjectError = "Project name must not be blank."
                            return@TextButton
                        }
                        if (newProjectStarters.isEmpty()) {
                            newProjectError = "Select at least one starter source."
                            return@TextButton
                        }
                        newProjectError = null
                        showCreateProjectDialog = false
                        requestProjectParentSelection = true
                    }) { Text("Create in Folder…") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjectDialog = false }) { Text("Cancel") }
                },
            )
        }

        if (showCreateRomSetGuide) {
            AlertDialog(
                onDismissRequest = { showCreateRomSetGuide = false },
                title = { Text("Create ${selectedMachineId.displayName()} ROM Set") },
                text = {
                    Text(
                        "Machine: ${selectedMachineId.displayName()}\n\n" +
                            "You will choose three items in this order:\n\n" +
                            "1. Internal ROM dump — exactly 8 KiB (8192 bytes)\n" +
                            if (MachineCatalog.require(selectedMachineId).family == MachineFamily.PC_1360) {
                                "2. Bank ROM dump — exactly 128 KiB (8 × 16 KiB)\n"
                            } else {
                                "2. External ROM dump — use the size required by this machine\n"
                            } +
                            "3. Destination for the new .pgrom file\n\n" +
                            "File names do not matter. PGP validates each size and creates the manifest " +
                            "and SHA-256 values automatically.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showCreateRomSetGuide = false
                        createRomSet()
                    }) { Text("Continue") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateRomSetGuide = false }) { Text("Cancel") }
                },
            )
        }

        if (showSaveMemoryDumpDialog) {
            AlertDialog(
                onDismissRequest = { showSaveMemoryDumpDialog = false },
                title = { Text("Save PGP Memory Dump") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter an inclusive 16-bit hexadecimal address range.")
                        OutlinedTextField(
                            value = dumpStartAddress,
                            onValueChange = { dumpStartAddress = it },
                            label = { Text("Start address") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = dumpEndAddress,
                            onValueChange = { dumpEndAddress = it },
                            label = { Text("End address") },
                            singleLine = true,
                        )
                        dumpRangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = parseHexAddress(dumpStartAddress)
                        val end = parseHexAddress(dumpEndAddress)
                        when {
                            start == null -> dumpRangeError = "Start address must be 0000..FFFF."
                            end == null -> dumpRangeError = "End address must be 0000..FFFF."
                            end < start -> dumpRangeError = "End address must not be less than start address."
                            else -> {
                                val activeRunner = runner ?: return@TextButton
                                showSaveMemoryDumpDialog = false
                                val destination = selectAndRestoreFocus {
                                    selectMemoryDumpDestination(ownerWindow)
                                } ?: return@TextButton
                                val snapshot = activeRunner.memorySnapshot(start, end - start + 1)
                                runCatching { destination.writeBytes(DesktopMemoryDumpWriter.write(snapshot)) }
                                    .onSuccess {
                                        message = "Saved ${snapshot.size} bytes " +
                                            "(0x${start.hex(4)}..0x${end.hex(4)}) to ${destination.name}."
                                    }
                                    .onFailure {
                                        message = "Could not save memory dump: ${it.message ?: it::class.simpleName}"
                                    }
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveMemoryDumpDialog = false }) { Text("Cancel") }
                },
            )
        }

        if (showSaveDisassemblyDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDisassemblyDialog = false },
                title = { Text("Save Disassembly") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter an inclusive 16-bit hexadecimal address range.")
                        OutlinedTextField(
                            value = disassemblyStartAddress,
                            onValueChange = { disassemblyStartAddress = it },
                            label = { Text("Start address") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = disassemblyEndAddress,
                            onValueChange = { disassemblyEndAddress = it },
                            label = { Text("End address") },
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
                            start == null -> disassemblyRangeError = "Start address must be 0000..FFFF."
                            end == null -> disassemblyRangeError = "End address must be 0000..FFFF."
                            end < start -> disassemblyRangeError = "End address must not be less than start address."
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
                                    message = "Saved $instructionCount disassembly lines " +
                                        "(0x${start.hex(4)}..0x${end.hex(4)}) to ${destination.name}."
                                }.onFailure {
                                    message = "Could not save disassembly: ${it.message ?: it::class.simpleName}"
                                }
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDisassemblyDialog = false }) { Text("Cancel") }
                },
            )
        }

        if (showSaveCheckpointDialog) {
            AlertDialog(
                onDismissRequest = { showSaveCheckpointDialog = false },
                title = { Text("Save Debug Checkpoint") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select the inclusive memory range to include (maximum 4096 bytes).")
                        OutlinedTextField(
                            value = checkpointStartAddress,
                            onValueChange = { checkpointStartAddress = it },
                            label = { Text("Start address") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = checkpointEndAddress,
                            onValueChange = { checkpointEndAddress = it },
                            label = { Text("End address") },
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
                            start == null || end == null -> checkpointRangeError = "Addresses must be 0000..FFFF."
                            end < start -> checkpointRangeError = "End address must not be less than start address."
                            end - start + 1 > DesktopEmulatorRunner.MAX_MEMORY_WATCH_BYTES ->
                                checkpointRangeError = "A checkpoint range may contain at most 4096 bytes."
                            else -> {
                                val activeRunner = runner ?: return@TextButton
                                activeRunner.pause()
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
                                        stopReason = activeRunner.stopReason?.displayName(),
                                    )
                                }.getOrElse {
                                    checkpointRangeError = "Could not capture checkpoint: ${it.message ?: it::class.simpleName}"
                                    return@TextButton
                                }
                                showSaveCheckpointDialog = false
                                val destination = selectAndRestoreFocus {
                                    selectCheckpointDestination(ownerWindow)
                                } ?: return@TextButton
                                runCatching { destination.writeBytes(checkpointBytes) }
                                    .onSuccess { message = "Saved debug checkpoint to ${destination.name}." }
                                    .onFailure {
                                        message = "Could not save debug checkpoint: ${it.message ?: it::class.simpleName}"
                                    }
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveCheckpointDialog = false }) { Text("Cancel") }
                },
            )
        }

        errorDialogMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { errorDialogMessage = null },
                title = { Text("ROM Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { errorDialogMessage = null }) { Text("OK") }
                },
            )
        }
    }
}

@Composable
private fun CpuRegisterPanel(snapshot: CpuSnapshot?, changedFields: Set<CpuField>) {
    val firstLine = if (snapshot == null) {
        "PC=----  CUR=----  OP=--  DP=----  P=--  Q=--  R=--  D=--"
    } else {
        "PC=${snapshot.programCounter.hex(4)}  CUR=${snapshot.currentProgramCounter.hex(4)}  " +
            "OP=${snapshot.opcode.hex(2)}  DP=${snapshot.dataPointer.hex(4)}  " +
            "P=${snapshot.p.hex(2)}  Q=${snapshot.q.hex(2)}  R=${snapshot.r.hex(2)}  D=${snapshot.d.hex(2)}"
    }
    val secondLine = if (snapshot == null) {
        "ALU=----  C=-  Z=-  IA=--  IB=--  FO=--  CTRL=--  TEST=--"
    } else {
        "ALU=${snapshot.alu.hex(4)}  C=${snapshot.carry.bit()}  Z=${snapshot.zero.bit()}  " +
            "IA=${snapshot.ia.hex(2)}  IB=${snapshot.ib.hex(2)}  FO=${snapshot.fo.hex(2)}  " +
            "CTRL=${snapshot.control.hex(2)}  TEST=${snapshot.testPort.hex(2)}"
    }

    Column(horizontalAlignment = Alignment.Start) {
        Text("CPU", style = MaterialTheme.typography.labelLarge)
        Text(firstLine, fontFamily = FontFamily.Monospace)
        Text(secondLine, fontFamily = FontFamily.Monospace)
        if (changedFields.isNotEmpty()) {
            Text(
                "Changed: ${changedFields.joinToString { it.displayName }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DisassemblyPanel(
    runner: DesktopEmulatorRunner?,
    snapshot: CpuSnapshot?,
    keyboardInput: DesktopKeyboardInput,
    onSaveRange: () -> Unit,
) {
    var customStartAddress by remember(runner) { mutableStateOf<Int?>(null) }
    var addressText by remember(runner) { mutableStateOf("") }
    var addressError by remember(runner) { mutableStateOf(false) }
    var breakpoints by remember(runner) { mutableStateOf(runner?.breakpoints().orEmpty()) }
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
        Text("Disassembly", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = {
                    addressText = it
                    addressError = false
                },
                label = { Text("Address (hex)") },
                placeholder = { Text(startAddress?.hex(4).orEmpty()) },
                isError = addressError,
                singleLine = true,
                modifier = Modifier
                    .widthIn(max = 180.dp)
                    .onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            Button(onClick = {
                val address = parseHexAddress(addressText)
                if (address == null) addressError = true else customStartAddress = address
            }) { Text("Go") }
            Button(
                enabled = customStartAddress != null,
                onClick = {
                    customStartAddress = null
                    addressText = ""
                    addressError = false
                },
            ) { Text("Follow PC") }
            Button(
                enabled = runner != null,
                onClick = onSaveRange,
            ) { Text("Save range…") }
        }
        if (lines.isEmpty()) {
            Text("No CPU state.", fontFamily = FontFamily.Monospace)
        } else {
            lines.forEachIndexed { index, line ->
                val marker = if (index == 0) ">" else " "
                val address = line.instruction.address
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            runner?.toggleBreakpoint(address)
                            breakpoints = runner?.breakpoints().orEmpty()
                        },
                    ) { Text(if (address in breakpoints) "●" else "○") }
                    TextButton(
                        enabled = runner != null && runner.state != RunnerState.FAULTED,
                        onClick = { runner?.runToAddress(address) },
                    ) { Text("Run to") }
                    Text(
                        "$marker${address.hex(4)}  " +
                            "${line.byteText.padEnd(DISASSEMBLY_BYTE_COLUMN_WIDTH)}  ${line.instructionText}",
                        fontFamily = FontFamily.Monospace,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryViewPanel(
    runner: DesktopEmulatorRunner?,
    keyboardInput: DesktopKeyboardInput,
    startAddress: Int,
    onStartAddressChange: (Int) -> Unit,
) {
    var addressText by remember(runner) { mutableStateOf(startAddress.hex(4)) }
    var addressError by remember(runner) { mutableStateOf(false) }
    var refreshRevision by remember(runner) { mutableStateOf(0) }
    LaunchedEffect(startAddress) {
        addressText = startAddress.hex(4)
        addressError = false
    }
    val lines = remember(runner, startAddress, refreshRevision) {
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
        Text("Memory", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = addressText,
                onValueChange = {
                    addressText = it
                    addressError = false
                },
                label = { Text("Start address (hex)") },
                isError = addressError,
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
            }) { Text("Go") }
            Button(
                enabled = runner != null,
                onClick = {
                    onStartAddressChange((startAddress - MEMORY_VIEW_PAGE_SIZE) and 0xffff)
                },
            ) { Text("Previous") }
            Button(
                enabled = runner != null,
                onClick = {
                    onStartAddressChange((startAddress + MEMORY_VIEW_PAGE_SIZE) and 0xffff)
                },
            ) { Text("Next") }
            Button(
                enabled = runner != null,
                onClick = { refreshRevision++ },
            ) { Text("Refresh") }
        }
        if (lines.isEmpty()) {
            Text("No memory available.", fontFamily = FontFamily.Monospace)
        } else {
            lines.forEach { line ->
                Text(
                    "${line.address.hex(4)}: ${line.hexText}  |${line.asciiText}|",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun InstructionTracePanel(
    runner: DesktopEmulatorRunner?,
    executionRevision: Long,
) {
    var enabled by remember(runner) { mutableStateOf(runner?.isInstructionTraceEnabled() == true) }
    var localRevision by remember(runner) { mutableStateOf(0) }
    val entries = remember(runner, executionRevision, localRevision) {
        runner?.instructionTrace().orEmpty().takeLast(INSTRUCTION_TRACE_VISIBLE_LINES).asReversed()
    }

    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Instruction trace", style = MaterialTheme.typography.labelLarge)
            Button(
                enabled = runner != null,
                onClick = {
                    enabled = !enabled
                    runner?.setInstructionTraceEnabled(enabled)
                    localRevision++
                },
            ) { Text(if (enabled) "Disable" else "Enable") }
            Button(
                enabled = runner != null && entries.isNotEmpty(),
                onClick = {
                    runner?.clearInstructionTrace()
                    localRevision++
                },
            ) { Text("Clear") }
            Text("${runner?.instructionTrace()?.size ?: 0}/256 instructions")
        }
        when {
            !enabled && entries.isEmpty() -> Text("Tracing is disabled.", fontFamily = FontFamily.Monospace)
            entries.isEmpty() -> Text("No instructions recorded yet.", fontFamily = FontFamily.Monospace)
            else -> entries.forEach { entry ->
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

@Composable
private fun MemoryWatchPanel(
    runner: DesktopEmulatorRunner?,
    keyboardInput: DesktopKeyboardInput,
) {
    var startText by remember(runner) { mutableStateOf("C000") }
    var endText by remember(runner) { mutableStateOf("C0FF") }
    var error by remember(runner) { mutableStateOf<String?>(null) }
    var activeRange by remember(runner) { mutableStateOf(runner?.memoryWatch()) }
    var activeAccessRange by remember(runner) { mutableStateOf(runner?.memoryAccessWatchRange()) }

    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Memory change watch", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = startText,
                onValueChange = {
                    startText = it
                    error = null
                },
                label = { Text("Start (hex)") },
                singleLine = true,
                modifier = Modifier.widthIn(max = 150.dp)
                    .onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            OutlinedTextField(
                value = endText,
                onValueChange = {
                    endText = it
                    error = null
                },
                label = { Text("End (hex)") },
                singleLine = true,
                modifier = Modifier.widthIn(max = 150.dp)
                    .onFocusChanged { keyboardInput.setEnabled(!it.isFocused) },
            )
            Button(
                enabled = runner != null,
                onClick = {
                    val start = parseHexAddress(startText)
                    val end = parseHexAddress(endText)
                    when {
                        start == null || end == null -> error = "Addresses must be 0000..FFFF."
                        end < start -> error = "End address must not be less than start address."
                        end - start + 1 > DesktopEmulatorRunner.MAX_MEMORY_WATCH_BYTES ->
                            error = "A watched range may contain at most 4096 bytes."
                        else -> {
                            runner?.setMemoryWatch(start, end)
                            activeRange = start..end
                            error = null
                        }
                    }
                },
            ) { Text("Watch") }
            Button(
                enabled = runner != null && activeRange != null,
                onClick = {
                    runner?.clearMemoryWatch()
                    activeRange = null
                },
            ) { Text("Clear") }
        }
        activeRange?.let { Text("Watching 0x${it.first.hex(4)}..0x${it.last.hex(4)}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fun setAccessWatch(reads: Boolean, writes: Boolean) {
                val start = parseHexAddress(startText)
                val end = parseHexAddress(endText)
                when {
                    start == null || end == null -> error = "Addresses must be 0000..FFFF."
                    end < start -> error = "End address must not be less than start address."
                    else -> {
                        runner?.setMemoryAccessWatch(start, end, reads, writes)
                        activeAccessRange = start..end
                        error = null
                    }
                }
            }
            Button(enabled = runner != null, onClick = { setAccessWatch(true, false) }) { Text("Watch Read") }
            Button(enabled = runner != null, onClick = { setAccessWatch(false, true) }) { Text("Watch Write") }
            Button(enabled = runner != null, onClick = { setAccessWatch(true, true) }) { Text("Watch R/W") }
            Button(
                enabled = runner != null && activeAccessRange != null,
                onClick = {
                    runner?.clearMemoryAccessWatch()
                    activeAccessRange = null
                },
            ) { Text("Clear R/W") }
        }
        activeAccessRange?.let { Text("Watching CPU access at 0x${it.first.hex(4)}..0x${it.last.hex(4)}") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "Stops when a value changes; read access and same-value writes are not detected.",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MemoryMapPanel(
    regions: List<MachineMemoryRegion>,
    onSelect: (MachineMemoryRegion) -> Unit,
) {
    val regionsById = regions.associateBy(MachineMemoryRegion::id)
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Memory map", style = MaterialTheme.typography.labelLarge)
        regions.forEach { region ->
            TextButton(onClick = { onSelect(region) }) {
                val mirrorTarget = region.mirrorsRegionId?.let { id ->
                    regionsById[id]?.displayName?.let { " → $it" }
                }.orEmpty()
                Text(
                    "${region.startAddress.hex(4)}–${region.endAddressInclusive.hex(4)}  " +
                        "${region.displayName} [${region.kind.displayName}]$mirrorTarget",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

private val MachineMemoryRegionKind.displayName: String
    get() = when (this) {
        MachineMemoryRegionKind.ROM -> "ROM"
        MachineMemoryRegionKind.RAM -> "RAM"
        MachineMemoryRegionKind.DISPLAY -> "VRAM"
        MachineMemoryRegionKind.MIRROR -> "mirror"
    }

private val CpuField.displayName: String
    get() = when (this) {
        CpuField.CURRENT_PC -> "CUR"
        CpuField.OPCODE -> "OP"
        CpuField.CARRY -> "C"
        CpuField.ZERO -> "Z"
        CpuField.CONTROL -> "CTRL"
        CpuField.INTERNAL_RAM -> "Internal RAM"
        else -> name
    }

private fun DebuggerStopReason.displayName(): String = when (this) {
    DebuggerStopReason.Reset -> "Reset"
    DebuggerStopReason.UserPause -> "Paused by user"
    DebuggerStopReason.StepComplete -> "Step complete"
    is DebuggerStopReason.Breakpoint -> "Breakpoint at 0x${address.hex(4)}"
    is DebuggerStopReason.RunToAddress -> "Run to address reached at 0x${address.hex(4)}"
    is DebuggerStopReason.MemoryChanged -> {
        val preview = changes.take(4).joinToString { change ->
            "${change.address.hex(4)}:${change.before.hex(2)}→${change.after.hex(2)}"
        }
        val remaining = if (changes.size > 4) " (+${changes.size - 4} more)" else ""
        "Memory changed after 0x${instructionAddress.hex(4)}: $preview$remaining"
    }
    is DebuggerStopReason.MemoryAccessed -> {
        val preview = accesses.take(4).joinToString { access ->
            val kind = if (access.kind == MemoryAccessKind.READ) "R" else "W"
            "$kind:${access.address.hex(4)}=${access.value.hex(2)}"
        }
        val remaining = if (accesses.size > 4) " (+${accesses.size - 4} more)" else ""
        "Memory access by 0x${instructionAddress.hex(4)}: $preview$remaining"
    }
    is DebuggerStopReason.Fault -> "Fault: ${fault.message()}"
}

@Composable
private fun PocketKeyButton(
    cap: PocketKeyCap,
    runner: DesktopEmulatorRunner?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (runner == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.pointerInput(runner, cap.key) {
            detectTapGestures(
                onPress = {
                    val activeRunner = runner
                    if (activeRunner != null) {
                        val pressedAt = System.nanoTime()
                        activeRunner.pressKey(cap.key)
                        try {
                            tryAwaitRelease()
                            val elapsedMilliseconds = (System.nanoTime() - pressedAt) / 1_000_000L
                            val remainingHold = activeRunner.minimumSoftwareKeyHoldMilliseconds() - elapsedMilliseconds
                            if (remainingHold > 0L) delay(remainingHold)
                        } finally {
                            activeRunner.releaseKey(cap.key)
                        }
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = cap.shiftedLabel ?: cap.basicLabel.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
            )
            Text(
                text = cap.primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = if (cap.shiftedLabel != null) cap.basicLabel.orEmpty() else "",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PocketSoftwareKeyboard(runner: DesktopEmulatorRunner?, layout: MachineKeyboardLayout) {
    val (rows, columnCount) = when (layout) {
        MachineKeyboardLayout.PC_1245 -> Pc1245KeyboardLayout.rows to Pc1245KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1251 -> Pc1251KeyboardLayout.rows to Pc1251KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1350 -> Pc1350KeyboardLayout.rows to Pc1350KeyboardLayout.COLUMN_COUNT
        MachineKeyboardLayout.PC_1360 -> Pc1360KeyboardLayout.rows to Pc1360KeyboardLayout.COLUMN_COUNT
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1_100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                var nextColumn = 0
                row.forEach { cap ->
                    if (cap.column > nextColumn) {
                        Spacer(modifier = Modifier.weight((cap.column - nextColumn).toFloat()))
                    }
                    PocketKeyButton(
                        cap = cap,
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
private fun PocketLcdPanel(snapshot: DisplaySnapshot?) {
    val panelAspectRatio = if (snapshot == null) {
        LCD_PANEL_ASPECT_RATIO
    } else if (snapshot.characterRows == 1) {
        CharacterCellGeometry.visualColumnCount(
            snapshot.characterColumns,
            snapshot.characterWidth,
            snapshot.interCharacterColumnGap,
        ) / 11f
    } else {
        CharacterCellGeometry.visualColumnCount(
            snapshot.characterColumns,
            snapshot.characterWidth,
            snapshot.interCharacterColumnGap,
        ).toFloat() / CharacterCellGeometry.visualRowCount(
            snapshot.characterRows,
            snapshot.characterHeight,
            snapshot.interCharacterRowGap,
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1_000.dp)
                .aspectRatio(panelAspectRatio),
        ) {
            drawRect(LCD_BACKGROUND)
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
                            color = LCD_DOT,
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
            text = snapshot?.symbols?.joinToString(separator = "  ") { it.name }.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun selectRomFile(owner: Frame, machineId: MachineId): File? {
    return selectFile(owner, "Open ${machineId.displayName()} ROM", "${machineId.value}.pgrom")
}

private fun selectInternalRomFile(owner: Frame, machineId: MachineId): File? = selectFile(
    owner,
    "Select ${machineId.displayName()} internal ROM (8 KiB)",
    "internal.bin",
)

private fun selectExternalRomFile(owner: Frame, machineId: MachineId): File? = selectFile(
    owner,
    if (MachineCatalog.require(machineId).family == MachineFamily.PC_1360) {
        "Select ${machineId.displayName()} bank ROM (128 KiB)"
    } else {
        "Select ${machineId.displayName()} external ROM"
    },
    if (MachineCatalog.require(machineId).family == MachineFamily.PC_1360) "pc1360bank.bin" else "external.bin",
)

private fun selectPackageDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save PGP ROM Package",
    suggestedFile = "pc-1245.pgrom",
    mode = FileDialog.SAVE,
)

private fun selectBasicFile(owner: Frame, title: String): File? = selectFile(owner, title, "*.bas")

private fun selectProjectDirectory(owner: Frame): File? = selectDirectory(
    owner,
    "Choose Pokecom GO Studio Project Folder",
)

private fun selectProjectParentDirectory(owner: Frame): File? = selectDirectory(
    owner,
    "Choose Folder for New Pokecom GO Studio Project",
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

private fun selectMemoryDumpFile(owner: Frame): File? = selectFile(owner, "Load PGP Memory Dump", "*.dmp")

private fun selectAssemblyFile(owner: Frame): File? = selectFile(owner, "Assemble SC61860 source", "*.asm")

private fun selectMemoryDumpDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save PGP Memory Dump",
    suggestedFile = "memory.dmp",
    mode = FileDialog.SAVE,
)

private fun selectDisassemblyDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save SC61860 Disassembly",
    suggestedFile = "disassembly.asm",
    mode = FileDialog.SAVE,
)

private fun selectAssembledDumpDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save assembled PGP Memory Dump",
    suggestedFile = "program.dmp",
    mode = FileDialog.SAVE,
)

private fun selectCheckpointDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save Pokecom GO Studio Debug Checkpoint",
    suggestedFile = "checkpoint.pgpdebug.json",
    mode = FileDialog.SAVE,
)

private fun selectBasicSaveFile(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save BASIC source",
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
private fun ProjectTreePanel(tree: DesktopProjectTreeSnapshot) {
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Project files", style = MaterialTheme.typography.titleMedium)
        ProjectTreeGroup("Tracked", tree.tracked.map { it.definition.path })
        ProjectTreeGroup("Untracked", tree.untracked.map { it.relativePath })
    }
}

@Composable
private fun ProjectTreeGroup(name: String, paths: List<String>) {
    Text("$name (${paths.size})")
    paths.forEach { path -> Text("  $path", fontFamily = FontFamily.Monospace) }
}

@Composable
private fun AssemblyPreviewSection(title: String, content: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Text(
        content.ifBlank { "(empty)" },
        modifier = Modifier.fillMaxWidth().widthIn(max = 1100.dp),
        fontFamily = FontFamily.Monospace,
    )
}

private fun DesktopBasicLoadError.message(machineName: String): String = when (this) {
    DesktopBasicLoadError.InvalidUtf8 -> "BASIC source is not valid UTF-8."
    is DesktopBasicLoadError.Parse ->
        "BASIC text error at ${error.line}:${error.column}: ${error.message}"
    is DesktopBasicLoadError.UnsupportedRomInput -> {
        val detail = when (val value = error.unsupported) {
            is Pc1245RomInputUnsupported.Character -> "character '${value.value}'"
            is Pc1245RomInputUnsupported.SpecialSymbol -> "symbol ${value.value}"
            is Pc1245RomInputUnsupported.RawByte -> "raw byte 0x${value.value.hex(2)}"
        }
        "$machineName ROM input does not support $detail at ${error.line}:${error.column}."
    }
    is DesktopBasicLoadError.UnsupportedMachine ->
        "BASIC loading is not available for $machineName yet."
}

private fun CoreFault.message(): String = when (this) {
    is CoreFault.UnsupportedOpcode ->
        "Emulation stopped: unsupported opcode 0x${opcode.hex(2)} at PC=0x${address.hex(4)}."
}

private fun DesktopBasicProgramCompileError.message(machineName: String): String = when (this) {
    DesktopBasicProgramCompileError.InvalidUtf8 -> "BASIC source is not valid UTF-8."
    is DesktopBasicProgramCompileError.Parse ->
        "BASIC text error at ${error.line}:${error.column}: ${error.message}"
    is DesktopBasicProgramCompileError.Tokenize ->
        "$machineName OLD BASIC error at ${error.line}:${error.column}: ${error.message}"
    is DesktopBasicProgramCompileError.S1Tokenize ->
        "$machineName S1 BASIC error at ${error.line}:${error.column}: ${error.message}"
    is DesktopBasicProgramCompileError.S2Tokenize ->
        "$machineName S2 BASIC error at ${error.line}:${error.column}: ${error.message}"
    is DesktopBasicProgramCompileError.UnsupportedMachine ->
        "BASIC compilation is not available for $machineName yet."
}

private fun BasicProgramMemoryError.message(): String = when (this) {
    is BasicProgramMemoryError.InvalidPointer ->
        "Invalid BASIC memory pointers: start=0x${startAddress.hex(4)}, end=0x${endAddress.hex(4)}."
    is BasicProgramMemoryError.ProgramTooLarge ->
        "BASIC program is too large: $size bytes (capacity $capacity bytes)."
    is BasicProgramMemoryError.InvalidProgram ->
        "Invalid BASIC program at byte $offset: $reason"
    is BasicProgramMemoryError.UnsupportedMachine ->
        "BASIC program memory access is not implemented for ${machineId.value}."
}

private fun DesktopMemoryDumpLoadError.message(): String = when (this) {
    DesktopMemoryDumpLoadError.InvalidUtf8 -> "Memory dump is not valid UTF-8."
    is DesktopMemoryDumpLoadError.Parse -> when (val detail = error) {
        PgpMemoryDumpError.Empty -> "Memory dump contains no data."
        is PgpMemoryDumpError.Syntax ->
            "Memory dump error at ${detail.line}:${detail.column}: ${detail.message}"
        is PgpMemoryDumpError.AddressOverflow ->
            "Memory dump line ${detail.line} exceeds address 0xFFFF."
        is PgpMemoryDumpError.Overlap ->
            "Memory dump line ${detail.line} overlaps line ${detail.previousLine} at 0x${detail.address.hex(4)}."
    }
}

private fun DesktopProjectOpenError.message(): String = when (this) {
    is DesktopProjectOpenError.CouldNotReadManifest -> "Could not read project: $message"
    DesktopProjectOpenError.InvalidUtf8 -> "Project manifest is not valid UTF-8."
    is DesktopProjectOpenError.InvalidManifest -> errors.joinToString("; ") { it.message() }
}

private fun DesktopProjectCreateError.message(): String = when (this) {
    is DesktopProjectCreateError.DestinationExists -> "Project folder already exists: $path"
    is DesktopProjectCreateError.CouldNotCreate -> "Could not create project: $message"
}

private fun ProjectManifestError.message(): String = when (this) {
    is ProjectManifestError.InvalidJson -> "Invalid project JSON: $message"
    is ProjectManifestError.UnsupportedFormat -> "Unsupported project format: $actual"
    is ProjectManifestError.UnsupportedVersion -> "Unsupported project version: $actual"
    ProjectManifestError.BlankName -> "Project name must not be blank."
    is ProjectManifestError.UnsupportedMachine -> "Unsupported project machine: $actual"
    is ProjectManifestError.InvalidSourceId -> "Invalid source id at index $sourceIndex: $actual"
    is ProjectManifestError.DuplicateSourceId -> "Duplicate source id: $id"
    is ProjectManifestError.InvalidSourcePath -> "Invalid path for source '$sourceId': $actual"
    is ProjectManifestError.DuplicateSourcePath -> "Duplicate source path: $path"
    is ProjectManifestError.InvalidLoadAddress -> "Invalid load address for source '$sourceId': $actual"
    is ProjectManifestError.MissingLoadAddress -> "Raw binary source '$sourceId' needs loadAddress."
    is ProjectManifestError.UnexpectedLoadAddress -> "Source '$sourceId' must not specify loadAddress."
}

private fun ProjectSourceType.displayName(): String = when (this) {
    ProjectSourceType.BASIC -> "BASIC"
    ProjectSourceType.MEMORY_DUMP -> "memory dump"
    ProjectSourceType.RAW_BINARY -> "raw binary"
    ProjectSourceType.ASSEMBLY -> "assembly"
}

private fun DesktopProjectBuildError.message(machineName: String): String = when (this) {
    is DesktopProjectBuildError.MultipleBasicSources ->
        "A project currently supports one BASIC program; found: ${sourceIds.joinToString()}."
    is DesktopProjectBuildError.CouldNotReadSource -> "Could not read project source '$sourceId': $message"
    is DesktopProjectBuildError.BasicCompile -> "Source '$sourceId': ${error.message(machineName)}"
    is DesktopProjectBuildError.MemoryDumpParse -> "Source '$sourceId': ${error.message()}"
    is DesktopProjectBuildError.EmptyRawBinary -> "Raw binary source '$sourceId' is empty."
    is DesktopProjectBuildError.InvalidRawBinaryRange ->
        "Source '$sourceId' does not fit in memory at 0x${startAddress.hex(4)} ($size bytes)."
    is DesktopProjectBuildError.MemoryOverlap ->
        "Source '$sourceId' overlaps '$previousSourceId' at 0x${address.hex(4)}."
    is DesktopProjectBuildError.Assembly ->
        "Assembly source '$sourceId', line $line: $message"
}

private fun MemoryImageLoadError.message(): String = when (this) {
    is MemoryImageLoadError.ReadOnlyAddress ->
        "Memory dump line $sourceLine writes read-only address 0x${address.hex(4)}."
}

private fun DesktopRomLoadError.message(): String = when (this) {
    is DesktopRomLoadError.InvalidRom -> when (val reason = error) {
        is com.digihori.pgp.core.emulator.machine.pc1245.RomImportError.UnsupportedMachine ->
            "Unsupported ROM machine: ${reason.machineId.value}."
        is com.digihori.pgp.core.emulator.machine.pc1245.RomImportError.InvalidImageSize ->
            "Invalid ROM size: ${reason.actual} bytes (expected ${reason.expected.joinToString(" or ")})."
        is com.digihori.pgp.core.emulator.machine.pc1245.RomImportError.InvalidComponentSize ->
            "Invalid ${reason.componentId.value} ROM size: ${reason.actual} bytes " +
                "(expected ${reason.expected})."
    }
    is DesktopRomLoadError.InvalidPackage -> "Invalid .pgrom package: ${error.message()}"
    is DesktopRomLoadError.SessionCreation -> "Could not create emulator session: $error"
}

private fun com.digihori.pgp.desktop.rom.DesktopRomPackageError.message(): String = when (this) {
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.PackageTooLarge ->
        "package is too large ($actual bytes; maximum $maximum)"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.TooManyEntries ->
        "package has more than $maximum entries"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.EntryTooLarge ->
        "$path exceeds $maximum bytes"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.UnsafePath -> "unsafe path: $path"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.DuplicateEntry -> "duplicate entry: $path"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.InvalidZip -> reason
    com.digihori.pgp.desktop.rom.DesktopRomPackageError.MissingManifest -> "manifest.json is missing"
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.InvalidManifest -> reason
    is com.digihori.pgp.desktop.rom.DesktopRomPackageError.Validation ->
        errors.joinToString { it.toString() }
}

private const val FRAME_DELAY_MILLISECONDS: Long = 16L
private const val PROJECT_SCAN_DELAY_MILLISECONDS: Long = 500L
private const val AUTOMATIC_ROM_BOOT_CYCLES: Long = 1_000_000L
private const val CPU_REFRESH_FRAME_INTERVAL: Int = 6
private const val DISASSEMBLY_LINE_COUNT: Int = 10
private const val DISASSEMBLY_BYTE_COLUMN_WIDTH: Int = 11
private const val DEFAULT_MEMORY_VIEW_ADDRESS: Int = 0xc000
private const val MEMORY_VIEW_LINE_COUNT: Int = 8
private const val MEMORY_VIEW_PAGE_SIZE: Int =
    DesktopMemoryViewModel.BYTES_PER_LINE * MEMORY_VIEW_LINE_COUNT
private const val INSTRUCTION_TRACE_VISIBLE_LINES: Int = 32
private const val LCD_DOT_INSET_RATIO: Float = 0.14f
private const val LCD_PANEL_ASPECT_RATIO: Float = 95f / 11f
private val LCD_BACKGROUND: Color = Color(0xffc9d2b0)
private val LCD_DOT: Color = Color(0xff263126)

private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
    .toString(16)
    .uppercase()
    .padStart(width, '0')

private fun parseHexAddress(value: String): Int? {
    val normalized = value.trim().removePrefix("0x").removePrefix("0X")
    if (normalized.isEmpty() || normalized.length > 4) return null
    return normalized.toIntOrNull(16)?.takeIf { it in 0..0xffff }
}

private fun Boolean.bit(): Int = if (this) 1 else 0

private fun MachineId.displayName(): String = MachineCatalog.find(this)?.displayName ?: value.uppercase()

private enum class ProjectRuntimeStatus(val displayName: String) {
    NOT_BUILT("not built"),
    UP_TO_DATE("up to date"),
    SOURCE_CHANGED("source changed"),
    RUNTIME_MODIFIED("modified by quick load"),
    BUILD_FAILED("build failed"),
}
