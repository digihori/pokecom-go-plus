package com.digihori.pgp.player.android

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.player.PlayerDisplayFrame
import com.digihori.pgp.player.PlayerFailureKind
import com.digihori.pgp.player.PlayerRunState
import com.digihori.pgp.player.PlayerScreenState
import com.digihori.pgp.player.PlayerKeyCap
import com.digihori.pgp.player.PlayerKeyboard
import com.digihori.pgp.player.PresentationMode
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import com.digihori.pgp.player.skin.SkinDefinition
import com.digihori.pgp.player.skin.SkinLayoutEngine
import com.digihori.pgp.player.skin.SkinLayoutRequest
import com.digihori.pgp.player.skin.SkinSize

@Composable
internal fun PlayerApp(
    screenState: PlayerScreenState,
    displayFrame: PlayerDisplayFrame?,
    keyboard: PlayerKeyboard?,
    onImportRom: () -> Unit,
    onKeyPress: (PocketKey) -> Unit,
    onKeyRelease: (PocketKey) -> Unit,
    onRun: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onTogglePresentationMode: () -> Unit,
    onOperatingModeChange: (OperatingMode) -> Unit,
    romImportEnabled: Boolean,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                contentAlignment = Alignment.Center,
            ) {
                when (screenState) {
                    PlayerScreenState.RomMissing -> RomMissingScreen(onImportRom, romImportEnabled)
                    PlayerScreenState.Loading -> LoadingScreen()
                    is PlayerScreenState.Failure -> FailureScreen(screenState.kind, onImportRom, romImportEnabled)
                    is PlayerScreenState.Emulator -> EmulatorScreen(
                        screenState.machineId,
                        screenState.runState,
                        screenState.presentationMode,
                        screenState.operatingMode,
                        screenState.supportedOperatingModes,
                        displayFrame,
                        keyboard,
                        onKeyPress,
                        onKeyRelease,
                        onRun,
                        onPause,
                        onReset,
                        onTogglePresentationMode,
                        onOperatingModeChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun RomMissingScreen(onImportRom: () -> Unit, romImportEnabled: Boolean) {
    PlayerMessageColumn {
        Text(
            text = stringResource(R.string.rom_missing_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.rom_missing_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onImportRom,
            enabled = romImportEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.import_rom))
        }
    }
}

@Composable
private fun LoadingScreen() {
    PlayerMessageColumn {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.loading_rom),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FailureScreen(
    kind: PlayerFailureKind,
    onImportRom: () -> Unit,
    romImportEnabled: Boolean,
) {
    PlayerMessageColumn {
        Text(
            text = stringResource(R.string.rom_error_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(kind.messageResource()),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onImportRom,
            enabled = romImportEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.choose_another_rom))
        }
    }
}

@Composable
private fun EmulatorScreen(
    machineId: MachineId,
    runState: PlayerRunState,
    presentationMode: PresentationMode,
    operatingMode: OperatingMode,
    supportedOperatingModes: Set<OperatingMode>,
    displayFrame: PlayerDisplayFrame?,
    keyboard: PlayerKeyboard?,
    onKeyPress: (PocketKey) -> Unit,
    onKeyRelease: (PocketKey) -> Unit,
    onRun: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onTogglePresentationMode: () -> Unit,
    onOperatingModeChange: (OperatingMode) -> Unit,
) {
    val skin = PlayerSkinCatalog.forMachine(machineId)
    Box(modifier = Modifier.fillMaxSize()) {
        if (displayFrame == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (skin != null) {
            SkinnedDevice(
                skin = skin,
                presentationMode = presentationMode,
                displayFrame = displayFrame,
                keyboard = keyboard,
                onKeyPress = onKeyPress,
                onKeyRelease = onKeyRelease,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LcdDisplay(displayFrame, modifier = Modifier.fillMaxSize())
        }
        PlayerMenu(
            runState = runState,
            presentationMode = presentationMode,
            operatingMode = operatingMode,
            supportedOperatingModes = supportedOperatingModes,
            showPresentationMode = skin != null,
            onRun = onRun,
            onPause = onPause,
            onReset = onReset,
            onTogglePresentationMode = onTogglePresentationMode,
            onOperatingModeChange = onOperatingModeChange,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun PlayerMenu(
    runState: PlayerRunState,
    presentationMode: PresentationMode,
    operatingMode: OperatingMode,
    supportedOperatingModes: Set<OperatingMode>,
    showPresentationMode: Boolean,
    onRun: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onTogglePresentationMode: () -> Unit,
    onOperatingModeChange: (OperatingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.player_menu_symbol))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.run_emulator)) },
                enabled = runState == PlayerRunState.PAUSED,
                onClick = { expanded = false; onRun() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pause_emulator)) },
                enabled = runState == PlayerRunState.RUNNING,
                onClick = { expanded = false; onPause() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reset_emulator)) },
                onClick = { expanded = false; onReset() },
            )
            if (showPresentationMode) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (presentationMode == PresentationMode.CONTROLLER_DISPLAY) {
                                    R.string.show_full_device
                                } else {
                                    R.string.enlarge_display
                                },
                            ),
                        )
                    },
                    onClick = { expanded = false; onTogglePresentationMode() },
                )
            }
            OperatingMode.entries.filter { it in supportedOperatingModes }.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                R.string.operating_mode_menu_item,
                                if (mode == operatingMode) "✓" else "",
                                mode.shortLabel(),
                            ),
                        )
                    },
                    enabled = mode != operatingMode,
                    onClick = { expanded = false; onOperatingModeChange(mode) },
                )
            }
        }
    }
}

private fun OperatingMode.shortLabel(): String = when (this) {
    OperatingMode.RUN -> "RUN"
    OperatingMode.PROGRAM -> "PRO"
    OperatingMode.RESERVE -> "RSV"
}

@Composable
private fun SkinnedDevice(
    skin: SkinDefinition,
    presentationMode: PresentationMode,
    displayFrame: PlayerDisplayFrame,
    keyboard: PlayerKeyboard?,
    onKeyPress: (PocketKey) -> Unit,
    onKeyRelease: (PocketKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = when (presentationMode) {
        PresentationMode.FULL_DEVICE -> skin.fullDeviceRegion
        PresentationMode.PLAYABLE -> skin.playableRegion
        PresentationMode.LANDSCAPE -> skin.landscapeRegion
        PresentationMode.CONTROLLER_DISPLAY -> skin.controllerDisplayRegion
    }
    val skinImage = ImageBitmap.imageResource(R.drawable.skin_pc1245)
    val skinDescription = stringResource(R.string.pc1245_skin_content_description)
    BoxWithConstraints(modifier = modifier) {
        val placement = SkinLayoutEngine.calculate(
            skin,
            SkinLayoutRequest(
                availableSize = SkinSize(maxWidth.value, maxHeight.value),
                mode = presentationMode,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = skinDescription },
        ) {
            Canvas(
                modifier = Modifier
                    .offset(placement.viewport.x.dp, placement.viewport.y.dp)
                    .width(placement.viewport.width.dp)
                    .height(placement.viewport.height.dp),
            ) {
                drawImage(
                    image = skinImage,
                    srcOffset = IntOffset(source.x.toInt(), source.y.toInt()),
                    srcSize = IntSize(source.width.toInt(), source.height.toInt()),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
            }
            val lcd = skin.lcdContentRegion
            LcdDisplay(
                frame = displayFrame,
                compact = true,
                modifier = Modifier
                    .offset(
                        x = placement.screenX(lcd.x).dp,
                        y = placement.screenY(lcd.y).dp,
                    )
                    .width((lcd.width * placement.scale).dp)
                    .height((lcd.height * placement.scale).dp),
            )
            val keyCaps = remember(keyboard) {
                keyboard?.rows?.flatten()?.associateBy(PlayerKeyCap::key).orEmpty()
            }
            skin.keyRegions.forEach { keyRegion ->
                val bounds = keyRegion.bounds
                if (
                    bounds.right > source.x && bounds.x < source.right &&
                    bounds.bottom > source.y && bounds.y < source.bottom
                ) {
                    val cap = keyCaps[keyRegion.key]
                    val accessibilityLabel = remember(cap, keyRegion.key) {
                        cap?.let {
                            listOfNotNull(it.primaryLabel, it.shiftedLabel, it.basicLabel).joinToString(", ")
                        } ?: keyRegion.key.name
                    }
                    var pressed by remember(keyRegion.key) { mutableStateOf(false) }
                    val pressOverlayAlpha by animateFloatAsState(
                        targetValue = if (pressed) 0.28f else 0f,
                        animationSpec = tween(durationMillis = if (pressed) 35 else 120),
                        label = "skin-key-${keyRegion.key.name}",
                    )
                    Box(
                        modifier = Modifier
                            .offset(
                                x = placement.screenX(bounds.x).dp,
                                y = placement.screenY(bounds.y).dp,
                            )
                            .width((bounds.width * placement.scale).dp)
                            .height((bounds.height * placement.scale).dp)
                            .background(Color.White.copy(alpha = pressOverlayAlpha))
                            .semantics {
                                contentDescription = accessibilityLabel
                                role = Role.Button
                                onClick {
                                    onKeyPress(keyRegion.key)
                                    onKeyRelease(keyRegion.key)
                                    true
                                }
                            }
                            .pointerInput(keyRegion.key) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressed = true
                                    onKeyPress(keyRegion.key)
                                    try {
                                        waitForUpOrCancellation()
                                    } finally {
                                        onKeyRelease(keyRegion.key)
                                        pressed = false
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftwareKeyboard(
    keyboard: PlayerKeyboard,
    onKeyPress: (PocketKey) -> Unit,
    onKeyRelease: (PocketKey) -> Unit,
) {
    val scrollState = rememberScrollState()
    val cellPitch = 48.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keyboard.rows.forEach { row ->
            Row(modifier = Modifier.width(cellPitch * keyboard.columnCount)) {
                var nextColumn = 0
                row.sortedBy(PlayerKeyCap::column).forEach { cap ->
                    if (cap.column > nextColumn) {
                        Spacer(Modifier.width(cellPitch * (cap.column - nextColumn)))
                    }
                    KeyCap(
                        cap = cap,
                        modifier = Modifier.width(cellPitch * cap.columnSpan - 4.dp),
                        onKeyPress = onKeyPress,
                        onKeyRelease = onKeyRelease,
                    )
                    Spacer(Modifier.width(4.dp))
                    nextColumn = cap.column + cap.columnSpan
                }
            }
        }
    }
}

@Composable
private fun KeyCap(
    cap: PlayerKeyCap,
    modifier: Modifier,
    onKeyPress: (PocketKey) -> Unit,
    onKeyRelease: (PocketKey) -> Unit,
) {
    val accessibilityLabel = remember(cap) {
        listOfNotNull(cap.primaryLabel, cap.shiftedLabel, cap.basicLabel).joinToString(", ")
    }
    Surface(
        color = Color(0xFFECEFF1),
        shape = RoundedCornerShape(5.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .height(48.dp)
            .semantics {
                contentDescription = accessibilityLabel
                role = Role.Button
                onClick {
                    onKeyPress(cap.key)
                    onKeyRelease(cap.key)
                    true
                }
            }
            .pointerInput(cap.key) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onKeyPress(cap.key)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        onKeyRelease(cap.key)
                    }
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        ) {
            cap.shiftedLabel?.let {
                Text(it, color = Color(0xFFC65D21), style = MaterialTheme.typography.labelSmall)
            }
            Text(cap.primaryLabel, color = Color(0xFF172126), style = MaterialTheme.typography.labelMedium)
            cap.basicLabel?.let {
                Text(it, color = Color(0xFF315A83), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LcdDisplay(
    frame: PlayerDisplayFrame,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val lcdDescription = stringResource(R.string.lcd_content_description)
    Surface(
        color = Color(0xFF263238),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(if (compact) 0.dp else 12.dp)) {
            Canvas(
                modifier = (if (compact) Modifier.fillMaxSize() else Modifier
                    .fillMaxWidth()
                    .aspectRatio(frame.logicalAspectRatio))
                    .background(Color(0xFFB7C8A7))
                    .semantics { contentDescription = lcdDescription },
            ) {
                val cellWidth = size.width / frame.logicalColumns
                val cellHeight = size.height / frame.logicalRows
                val dotWidth = cellWidth * 0.72f
                val dotHeight = cellHeight * 0.72f
                val offsetX = (cellWidth - dotWidth) / 2f
                val offsetY = (cellHeight - dotHeight) / 2f
                val active = Color(0xFF26352C)
                val inactive = Color(0x1F26352C)
                for (row in 0 until frame.dotRows) {
                    for (column in 0 until frame.dotColumns) {
                        drawRect(
                            color = if (frame.isDotOn(column, row)) active else inactive,
                            topLeft = Offset(
                                x = frame.logicalX(column) * cellWidth + offsetX,
                                y = frame.logicalY(row) * cellHeight + offsetY,
                            ),
                            size = Size(dotWidth, dotHeight),
                        )
                    }
                }
            }
            if (!compact && frame.symbols.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = frame.symbols.joinToString("  ") { it.lcdLabel() },
                    color = Color(0xFFB7C8A7),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun DisplaySymbol.lcdLabel(): String = when (this) {
    DisplaySymbol.RESERVE -> "RSV"
    else -> name
}

@Composable
private fun PlayerMessageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = { content() },
    )
}

private fun PlayerFailureKind.messageResource(): Int = when (this) {
    PlayerFailureKind.ROM_READ -> R.string.rom_error_read
    PlayerFailureKind.ROM_VALIDATION -> R.string.rom_error_validation
    PlayerFailureKind.SESSION_CREATION -> R.string.rom_error_session
    PlayerFailureKind.STORAGE -> R.string.rom_error_storage
}

@Preview(showBackground = true)
@Composable
private fun RomMissingPreview() {
    PlayerApp(PlayerScreenState.RomMissing, null, null, {}, {}, {}, {}, {}, {}, {}, {}, true)
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun RomMissingLandscapePreview() {
    PlayerApp(PlayerScreenState.RomMissing, null, null, {}, {}, {}, {}, {}, {}, {}, {}, false)
}

@Preview(showBackground = true)
@Composable
private fun EmulatorPlaceholderPreview() {
    PlayerApp(
        screenState = PlayerScreenState.Emulator(
            machineId = MachineId("pc-1245"),
            runState = PlayerRunState.PAUSED,
            presentationMode = PresentationMode.FULL_DEVICE,
            operatingMode = OperatingMode.RUN,
            supportedOperatingModes = setOf(OperatingMode.RUN, OperatingMode.PROGRAM),
        ),
        displayFrame = null,
        keyboard = null,
        onImportRom = {},
        onKeyPress = {},
        onKeyRelease = {},
        onRun = {},
        onPause = {},
        onReset = {},
        onTogglePresentationMode = {},
        onOperatingModeChange = {},
        romImportEnabled = true,
    )
}
