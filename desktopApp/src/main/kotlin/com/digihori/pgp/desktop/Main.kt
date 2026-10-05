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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
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
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.machine.PgpMemoryDumpError
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
import com.digihori.pgp.desktop.input.Pc1245KeyboardLayout
import com.digihori.pgp.desktop.input.Pc1251KeyboardLayout
import com.digihori.pgp.desktop.input.PocketKeyCap
import com.digihori.pgp.desktop.audio.DesktopAudioPlayer
import com.digihori.pgp.desktop.display.CharacterCellGeometry
import com.digihori.pgp.desktop.rom.DesktopRomLoadError
import com.digihori.pgp.desktop.rom.DesktopRomLoadResult
import com.digihori.pgp.desktop.rom.DesktopRomLoader
import com.digihori.pgp.desktop.rom.DesktopRomLocator
import com.digihori.pgp.desktop.rom.DesktopRomHistory
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
private fun App(keyboardInput: DesktopKeyboardInput, ownerWindow: Frame) {
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
    var operatingMode by remember { mutableStateOf(OperatingMode.RUN) }
    var requestedToneHz by remember { mutableStateOf(0) }
    var showOpenRomGuide by remember { mutableStateOf(false) }
    var showCreateRomSetGuide by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSaveMemoryDumpDialog by remember { mutableStateOf(false) }
    var dumpStartAddress by remember { mutableStateOf("C000") }
    var dumpEndAddress by remember { mutableStateOf("C0FF") }
    var dumpRangeError by remember { mutableStateOf<String?>(null) }
    var focusRestoreRequest by remember { mutableLongStateOf(0L) }
    val scrollState = rememberScrollState()
    val audioPlayer = remember { DesktopAudioPlayer() }
    val romHistory = remember { DesktopRomHistory() }
    val appFocusRequester = remember { FocusRequester() }

    fun selectAndRestoreFocus(select: () -> File?): File? = try {
        select()
    } finally {
        focusRestoreRequest++
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
    ): Boolean {
        val configuration = EmulatorConfiguration(pc1251FamilyMemoryMode = memoryMode)
        val result = runCatching {
            val bytes = file.readBytes()
            if (file.extension.equals("pgrom", ignoreCase = true)) {
                DesktopRomLoader.loadPackage(bytes, configuration)
            } else {
                DesktopRomLoader.loadLegacyImage(selectedMachineId, bytes, configuration)
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
                return true
            }
            is DesktopRomLoadResult.Failure -> {
                if (reportErrors) showError(result.error.message())
                return false
            }
        }
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

    DisposableEffect(audioPlayer) {
        onDispose(audioPlayer::close)
    }

    LaunchedEffect(Unit) {
        val previous = romHistory.lastSelection()
        if (previous != null && previous.file.isFile) {
            selectedMachineId = previous.machineId
            if (loadRom(previous.file, startAutomatically = true, reportErrors = false)) return@LaunchedEffect
        } else if (previous != null) {
            romHistory.clear()
        }
        DesktopRomLocator.findFirstAvailableRom()?.let { fallback ->
            selectedMachineId = fallback.machineId
            loadRom(fallback.file, startAutomatically = true)
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
            val latestDisplay = activeRunner.displaySnapshot()
            if (display?.revision != latestDisplay.revision || display?.enabled != latestDisplay.enabled) {
                display = latestDisplay
            }
            if (++cpuRefreshCounter >= CPU_REFRESH_FRAME_INTERVAL) {
                cpu = activeRunner.cpuSnapshot()
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
            }
        }
    }

    LaunchedEffect(focusRestoreRequest) {
        if (focusRestoreRequest > 0L) {
            delay(50)
            ownerWindow.toFront()
            ownerWindow.requestFocus()
            appFocusRequester.requestFocus()
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
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                Text(ProjectInfo.STUDIO_DISPLAY_NAME, style = MaterialTheme.typography.headlineMedium)
                Text("Machine: ${selectedMachineId.displayName()}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MachineCatalog.definitions.forEach { definition ->
                        Button(
                            enabled = selectedMachineId != definition.id,
                            onClick = { selectedMachineId = definition.id },
                        ) { Text(definition.displayName) }
                    }
                }
                if (MachineCatalog.require(runner?.machineId ?: selectedMachineId).supportsConfigurableRam) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = familyMemoryMode != Pc1251FamilyMemoryMode.EXPANDED,
                            onClick = {
                                val file = loadedRomFile
                                val resume = runnerState == RunnerState.RUNNING
                                familyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED
                                if (file == null) {
                                    message = "Expanded RAM selected. It will apply when a ROM is loaded."
                                } else if (loadRom(
                                        file,
                                        resume,
                                        memoryMode = Pc1251FamilyMemoryMode.EXPANDED,
                                    )) {
                                    message = "Restarted with expanded PC-1255-size RAM."
                                }
                            },
                        ) { Text("Expanded RAM") }
                        Button(
                            enabled = familyMemoryMode != Pc1251FamilyMemoryMode.HARDWARE,
                            onClick = {
                                val file = loadedRomFile
                                val resume = runnerState == RunnerState.RUNNING
                                familyMemoryMode = Pc1251FamilyMemoryMode.HARDWARE
                                if (file == null) {
                                    message = "Hardware RAM selected. It will apply when a ROM is loaded."
                                } else if (loadRom(
                                        file,
                                        resume,
                                        memoryMode = Pc1251FamilyMemoryMode.HARDWARE,
                                    )) {
                                    message = "Restarted with ${selectedMachineId.displayName()} hardware RAM."
                                }
                            },
                        ) { Text("Hardware RAM") }
                    }
                }
                Text("ROM: ${loadedRomName ?: "not loaded"}")
                Text("State: ${runnerState.name}")
                Text("Executed cycles: $executedCycles")
                Text("Requested tone: ${if (requestedToneHz == 0) "silent" else "$requestedToneHz Hz"}")
                Text(message)
                Pc1245LcdPanel(display)
                CpuRegisterPanel(cpu)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showOpenRomGuide = true }) { Text("Open ROM") }
                    Button(onClick = { showCreateRomSetGuide = true }) { Text("Create ROM Set") }
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            val activeRunner = runner ?: return@Button
                            val file = selectAndRestoreFocus {
                                selectBasicFile(ownerWindow, "Load BASIC source")
                            } ?: return@Button
                            val compiled = runCatching {
                                DesktopBasicLoader.compilePc1245Program(file.readBytes())
                            }.getOrElse {
                                message = "Could not read BASIC source: ${it.message ?: it::class.simpleName}"
                                return@Button
                            }
                            when (compiled) {
                                is DesktopBasicProgramCompileResult.Failure ->
                                    message = compiled.error.message(activeRunner.machineId.displayName())
                                is DesktopBasicProgramCompileResult.Success -> {
                                    when (val loaded = activeRunner.loadBasicProgram(compiled.bytes)) {
                                        is BasicProgramLoadResult.Failure -> {
                                            runnerState = activeRunner.state
                                            message = loaded.error.message()
                                        }
                                        is BasicProgramLoadResult.Success -> {
                                            runnerState = activeRunner.state
                                            display = activeRunner.displaySnapshot()
                                            cpu = activeRunner.cpuSnapshot()
                                            message = "Loaded ${file.name} directly (${loaded.size} bytes at " +
                                                "0x${loaded.startAddress.hex(4)}..0x${loaded.endAddress.hex(4)})."
                                        }
                                    }
                                }
                            }
                        },
                    ) { Text("Load BASIC") }
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            val activeRunner = runner ?: return@Button
                            val file = selectAndRestoreFocus {
                                selectMemoryDumpFile(ownerWindow)
                            } ?: return@Button
                            val parsed = runCatching { DesktopMemoryDumpLoader.parse(file.readBytes()) }
                                .getOrElse {
                                    message = "Could not read memory dump: ${it.message ?: it::class.simpleName}"
                                    return@Button
                                }
                            when (parsed) {
                                is DesktopMemoryDumpLoadResult.Failure -> message = parsed.error.message()
                                is DesktopMemoryDumpLoadResult.Success -> {
                                    when (val loaded = activeRunner.loadMemoryImage(parsed.image)) {
                                        is MemoryImageLoadResult.Failure -> message = loaded.error.message()
                                        is MemoryImageLoadResult.Success -> message =
                                            "Loaded ${file.name} (${loaded.byteCount} bytes in " +
                                                "${loaded.segmentCount} segments)."
                                    }
                                    runnerState = activeRunner.state
                                    display = activeRunner.displaySnapshot()
                                    cpu = activeRunner.cpuSnapshot()
                                }
                            }
                        },
                    ) { Text("Load Machine Code") }
                    Button(
                        enabled = runner != null,
                        onClick = {
                            dumpRangeError = null
                            showSaveMemoryDumpDialog = true
                        },
                    ) { Text("Save Machine Code") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            val activeRunner = runner ?: return@Button
                            when (val snapshot = activeRunner.basicProgramSnapshot()) {
                                is BasicProgramSnapshotResult.Failure -> message = snapshot.error.message()
                                is BasicProgramSnapshotResult.Success -> {
                                    when (val decoded = DesktopBasicLoader.detokenizePc1245Program(snapshot.copyBytes())) {
                                        is DesktopBasicProgramDecodeResult.Failure ->
                                            message = "BASIC program error at byte ${decoded.error.offset}: " +
                                                decoded.error.message
                                        is DesktopBasicProgramDecodeResult.Success -> {
                                            val file = selectAndRestoreFocus {
                                                selectBasicSaveFile(ownerWindow)
                                            } ?: return@Button
                                            runCatching { file.writeBytes(decoded.utf8Bytes) }
                                                .onSuccess { message = "Saved BASIC source to ${file.name}." }
                                                .onFailure {
                                                    message = "Could not save BASIC source: " +
                                                        (it.message ?: it::class.simpleName)
                                                }
                                        }
                                    }
                                }
                            }
                        },
                    ) { Text("Save BASIC") }
                    Button(
                        enabled = runner != null && runnerState != RunnerState.FAULTED,
                        onClick = {
                            val activeRunner = runner ?: return@Button
                            val file = selectAndRestoreFocus {
                                selectBasicFile(ownerWindow, "Type BASIC through ROM")
                            } ?: return@Button
                            val loadResult = runCatching {
                                DesktopBasicLoader.compileRomInput(file.readBytes(), activeRunner.machineId)
                            }.getOrElse {
                                message = "Could not read BASIC source: ${it.message ?: it::class.simpleName}"
                                return@Button
                            }
                            when (loadResult) {
                                is DesktopBasicLoadResult.Failure ->
                                    message = loadResult.error.message(activeRunner.machineId.displayName())
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
                                }
                            }
                        },
                    ) { Text("Type BASIC") }
                    Button(
                        enabled = runner != null && runnerState == RunnerState.PAUSED,
                        onClick = {
                            runner?.run()
                            runnerState = runner?.state ?: RunnerState.PAUSED
                            message = "Running at normal speed."
                        },
                    ) { Text("Run") }
                    Button(
                        enabled = runnerState == RunnerState.RUNNING,
                        onClick = {
                            runner?.pause()
                            audioPlayer.stop()
                            runnerState = runner?.state ?: RunnerState.PAUSED
                            cpu = runner?.cpuSnapshot()
                            message = "Paused."
                        },
                    ) { Text("Pause") }
                    Button(
                        enabled = runner != null,
                        onClick = {
                            runner?.reset()
                            audioPlayer.stop()
                            runnerState = runner?.state ?: RunnerState.PAUSED
                            executedCycles = 0L
                            display = runner?.displaySnapshot()
                            cpu = runner?.cpuSnapshot()
                            operatingMode = OperatingMode.RUN
                            requestedToneHz = 0
                            message = "Reset complete."
                        },
                    ) { Text("Reset") }
                    Button(
                        enabled = runner != null && runnerState == RunnerState.PAUSED,
                        onClick = {
                            val result = runner?.step() ?: return@Button
                            runnerState = runner?.state ?: RunnerState.PAUSED
                            executedCycles += result.cycles
                            display = runner?.displaySnapshot()
                            cpu = runner?.cpuSnapshot()
                            val audio = runner?.audioSnapshot()
                            if (audio != null) {
                                requestedToneHz = audio.frequencyHz
                            }
                            message = "Executed one instruction (${result.cycles} cycles)."
                        },
                    ) { Text("Step") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = runner != null && operatingMode != OperatingMode.RUN,
                        onClick = {
                            runner?.setOperatingMode(OperatingMode.RUN)
                            operatingMode = OperatingMode.RUN
                            display = runner?.displaySnapshot()
                        },
                    ) { Text("RUN mode") }
                    Button(
                        enabled = runner != null && operatingMode != OperatingMode.PROGRAM,
                        onClick = {
                            runner?.setOperatingMode(OperatingMode.PROGRAM)
                            operatingMode = OperatingMode.PROGRAM
                            display = runner?.displaySnapshot()
                        },
                    ) { Text("PRO mode") }
                    if (OperatingMode.RESERVE in MachineCatalog.require(
                            runner?.machineId ?: selectedMachineId,
                        ).supportedOperatingModes
                    ) {
                        Button(
                            enabled = runner != null && operatingMode != OperatingMode.RESERVE,
                            onClick = {
                                runner?.setOperatingMode(OperatingMode.RESERVE)
                                operatingMode = OperatingMode.RESERVE
                                display = runner?.displaySnapshot()
                            },
                        ) { Text("RSV mode") }
                    }
                }

                PocketSoftwareKeyboard(
                    runner,
                    MachineCatalog.require(runner?.machineId ?: selectedMachineId).keyboardLayout,
                )
            }
        }

        if (showOpenRomGuide) {
            AlertDialog(
                onDismissRequest = { showOpenRomGuide = false },
                title = { Text("Open ${selectedMachineId.displayName()} ROM") },
                text = {
                    Text(
                        "Machine: ${selectedMachineId.displayName()}\n\nChoose one of the following files:\n\n" +
                            "• PGP ROM package (.pgrom)\n" +
                            "  Created by PGP from separate physical ROM dumps.\n\n" +
                            "• Pokecom GO compatible image (.bin)\n" +
                            "  A 32 KiB or 64 KiB address-space image. It can be opened directly; " +
                            "conversion is not required.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOpenRomGuide = false
                        selectAndRestoreFocus { selectRomFile(ownerWindow, selectedMachineId) }
                            ?.let { loadRom(it, startAutomatically = false) }
                    }) { Text("Choose File") }
                },
                dismissButton = {
                    TextButton(onClick = { showOpenRomGuide = false }) { Text("Cancel") }
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
                            "2. External ROM dump — exactly 16 KiB (16384 bytes)\n" +
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
private fun CpuRegisterPanel(snapshot: CpuSnapshot?) {
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
    }
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
                        activeRunner.pressKey(cap.key)
                        try {
                            tryAwaitRelease()
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
    val pc1251 = layout == MachineKeyboardLayout.PC_1251
    val rows = if (pc1251) Pc1251KeyboardLayout.rows else Pc1245KeyboardLayout.rows
    val columnCount = if (pc1251) Pc1251KeyboardLayout.COLUMN_COUNT else Pc1245KeyboardLayout.COLUMN_COUNT
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
private fun Pc1245LcdPanel(snapshot: DisplaySnapshot?) {
    val panelAspectRatio = if (snapshot == null) {
        LCD_PANEL_ASPECT_RATIO
    } else {
        CharacterCellGeometry.visualColumnCount(snapshot.characterColumns, snapshot.characterWidth) / 11f
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .aspectRatio(panelAspectRatio),
        ) {
            drawRect(LCD_BACKGROUND)
            if (snapshot == null) return@Canvas

            val visualColumnCount = CharacterCellGeometry.visualColumnCount(
                snapshot.characterColumns,
                snapshot.characterWidth,
            )
            val cellWidth = size.width / visualColumnCount
            val cellHeight = size.height / snapshot.dotRows
            val insetX = cellWidth * LCD_DOT_INSET_RATIO
            val insetY = cellHeight * LCD_DOT_INSET_RATIO
            for (row in 0 until snapshot.dotRows) {
                for (column in 0 until snapshot.dotColumns) {
                    if (snapshot.isDotOn(column, row)) {
                        val visualColumn = CharacterCellGeometry.visualColumn(column, snapshot.characterWidth)
                        drawRect(
                            color = LCD_DOT,
                            topLeft = Offset(visualColumn * cellWidth + insetX, row * cellHeight + insetY),
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
    "Select ${machineId.displayName()} external ROM (16 KiB)",
    "external.bin",
)

private fun selectPackageDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save PGP ROM Package",
    suggestedFile = "pc-1245.pgrom",
    mode = FileDialog.SAVE,
)

private fun selectBasicFile(owner: Frame, title: String): File? = selectFile(owner, title, "*.bas")

private fun selectMemoryDumpFile(owner: Frame): File? = selectFile(owner, "Load PGP Memory Dump", "*.dmp")

private fun selectMemoryDumpDestination(owner: Frame): File? = selectFile(
    owner = owner,
    title = "Save PGP Memory Dump",
    suggestedFile = "memory.dmp",
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
private const val AUTOMATIC_ROM_BOOT_CYCLES: Long = 1_000_000L
private const val CPU_REFRESH_FRAME_INTERVAL: Int = 6
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
