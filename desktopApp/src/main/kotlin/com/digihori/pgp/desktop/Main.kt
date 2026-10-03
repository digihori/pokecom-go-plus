package com.digihori.pgp.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.digihori.pgp.core.ProjectInfo
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.input.Pc1245KeyCap
import com.digihori.pgp.desktop.input.Pc1245KeyboardLayout
import com.digihori.pgp.desktop.audio.DesktopAudioPlayer
import com.digihori.pgp.desktop.rom.DesktopRomLoadError
import com.digihori.pgp.desktop.rom.DesktopRomLoadResult
import com.digihori.pgp.desktop.rom.DesktopRomLoader
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun main() = application {
    val keyboardInput = remember { DesktopKeyboardInput() }
    Window(
        onCloseRequest = {
            keyboardInput.attach(null)
            exitApplication()
        },
        onPreviewKeyEvent = keyboardInput::handle,
        title = ProjectInfo.DISPLAY_NAME,
    ) {
        App(keyboardInput)
    }
}

@Composable
private fun App(keyboardInput: DesktopKeyboardInput) {
    var runner by remember { mutableStateOf<DesktopEmulatorRunner?>(null) }
    var runnerState by remember { mutableStateOf(RunnerState.PAUSED) }
    var loadedRomName by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Select a PC-1245 legacy 64 KiB ROM image.") }
    var executedCycles by remember { mutableLongStateOf(0L) }
    var display by remember { mutableStateOf<DisplaySnapshot?>(null) }
    var cpu by remember { mutableStateOf<CpuSnapshot?>(null) }
    var operatingMode by remember { mutableStateOf(OperatingMode.RUN) }
    var requestedToneHz by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    val audioPlayer = remember { DesktopAudioPlayer() }

    DisposableEffect(audioPlayer) {
        onDispose(audioPlayer::close)
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
                audioPlayer.update(audio).onFailure {
                    message = "Audio output unavailable: ${it.message ?: it::class.simpleName}"
                }
            }
            val status = tick.runResult?.status
            if (status is ExecutionStatus.Faulted) {
                audioPlayer.stop()
                message = "Emulation stopped: ${status.fault}"
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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
                Text(ProjectInfo.DISPLAY_NAME, style = MaterialTheme.typography.headlineMedium)
                Text("Machine: PC-1245")
                Text("ROM: ${loadedRomName ?: "not loaded"}")
                Text("State: ${runnerState.name}")
                Text("Executed cycles: $executedCycles")
                Text("Requested tone: ${if (requestedToneHz == 0) "silent" else "$requestedToneHz Hz"}")
                Text(message)
                Pc1245LcdPanel(display)
                CpuRegisterPanel(cpu)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val selectedFile = selectRomFile()
                        val file = selectedFile ?: return@Button
                        val result = runCatching { DesktopRomLoader.loadPc1245LegacyImage(file.readBytes()) }
                            .getOrElse {
                                message = "Could not read ROM: ${it.message ?: it::class.simpleName}"
                                return@Button
                            }
                        when (result) {
                            is DesktopRomLoadResult.Success -> {
                                runner?.pause()
                                audioPlayer.stop()
                                runner = DesktopEmulatorRunner(result.session)
                                keyboardInput.attach(runner)
                                runnerState = RunnerState.PAUSED
                                loadedRomName = file.name
                                executedCycles = 0L
                                display = runner?.displaySnapshot()
                                cpu = runner?.cpuSnapshot()
                                operatingMode = OperatingMode.RUN
                                requestedToneHz = 0
                                message = "ROM loaded. Press Run to start."
                            }
                            is DesktopRomLoadResult.Failure -> message = result.error.message()
                        }
                    }) { Text("Select ROM") }
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
                }

                Pc1245SoftwareKeyboard(runner)
            }
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
    cap: Pc1245KeyCap,
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
private fun Pc1245SoftwareKeyboard(runner: DesktopEmulatorRunner?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1_100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Pc1245KeyboardLayout.rows.forEach { row ->
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
                if (nextColumn < Pc1245KeyboardLayout.COLUMN_COUNT) {
                    Spacer(modifier = Modifier.weight((Pc1245KeyboardLayout.COLUMN_COUNT - nextColumn).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun Pc1245LcdPanel(snapshot: DisplaySnapshot?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .aspectRatio(LCD_PANEL_ASPECT_RATIO),
        ) {
            drawRect(LCD_BACKGROUND)
            if (snapshot == null) return@Canvas

            val cellWidth = size.width / snapshot.dotColumns
            val cellHeight = size.height / snapshot.dotRows
            val insetX = cellWidth * LCD_DOT_INSET_RATIO
            val insetY = cellHeight * LCD_DOT_INSET_RATIO
            for (row in 0 until snapshot.dotRows) {
                for (column in 0 until snapshot.dotColumns) {
                    if (snapshot.isDotOn(column, row)) {
                        drawRect(
                            color = LCD_DOT,
                            topLeft = Offset(column * cellWidth + insetX, row * cellHeight + insetY),
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

private fun selectRomFile(): File? {
    val dialog = FileDialog(null as Frame?, "Select PC-1245 ROM", FileDialog.LOAD).apply {
        file = "pc1245mem.bin"
        isVisible = true
    }
    val directory = dialog.directory
    val fileName = dialog.file
    dialog.dispose()
    return if (directory != null && fileName != null) File(directory, fileName) else null
}

private fun DesktopRomLoadError.message(): String = when (this) {
    is DesktopRomLoadError.InvalidRom -> when (val reason = error) {
        is com.digihori.pgp.core.emulator.machine.pc1245.RomImportError.InvalidImageSize ->
            "Invalid ROM size: ${reason.actual} bytes (expected ${reason.expected})."
    }
    is DesktopRomLoadError.SessionCreation -> "Could not create emulator session: $error"
}

private const val FRAME_DELAY_MILLISECONDS: Long = 16L
private const val CPU_REFRESH_FRAME_INTERVAL: Int = 6
private const val LCD_DOT_INSET_RATIO: Float = 0.14f
private const val LCD_PANEL_ASPECT_RATIO: Float = 80f / 11f
private val LCD_BACKGROUND: Color = Color(0xffc9d2b0)
private val LCD_DOT: Color = Color(0xff263126)

private fun Int.hex(width: Int): String = (this and if (width == 2) 0xff else 0xffff)
    .toString(16)
    .uppercase()
    .padStart(width, '0')

private fun Boolean.bit(): Int = if (this) 1 else 0
