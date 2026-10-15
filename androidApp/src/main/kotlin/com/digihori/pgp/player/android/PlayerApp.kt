package com.digihori.pgp.player.android

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import kotlin.math.min
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.player.PlayerRomEntry
import com.digihori.pgp.player.PlayerMachineCatalog
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

// Shared with the ROM-free debug geometry probe.
internal val PlayerTitleBarHeight = 56.dp

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
    onControllerSettings: () -> Unit = {},
    onPlaybackSettings: (String) -> Unit = {},
    onLoadProgram: () -> Unit = {},
    onSaveProgram: () -> Unit = {},
    onMenuVisibilityChange: (Boolean) -> Unit = {},
    onManageRom: () -> Unit = {},
    onInformation: (String) -> Unit = {},
    showHome: Boolean = false,
    onPlay: (MachineId) -> Unit = {},
    homeRomEntries: List<PlayerRomEntry>? = null,
    onImportMachineRom: (MachineId) -> Unit = {},
    onHome: () -> Unit = {},
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout).union(WindowInsets.statusBars)),
                contentAlignment = Alignment.Center,
            ) {
                if (showHome) {
                    PlayerHomeScreen(screenState, homeRomEntries, onPlay, onImportMachineRom, romImportEnabled)
                } else when (screenState) {
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
                        onLoadProgram = onLoadProgram,
                        onSaveProgram = onSaveProgram,
                        onMenuVisibilityChange = onMenuVisibilityChange,
                        onHome = onHome,
                    )
                }
                if (showHome || screenState !is PlayerScreenState.Emulator) {
                    PlayerMenu(
                        runState = PlayerRunState.PAUSED,
                        presentationMode = PresentationMode.FULL_DEVICE,
                        operatingMode = OperatingMode.RUN,
                        supportedOperatingModes = emptySet(),
                        showPresentationMode = false,
                        onRun = {}, onPause = {}, onReset = {},
                        onTogglePresentationMode = {}, onOperatingModeChange = {},
                        onMenuVisibilityChange = onMenuVisibilityChange,
                        onManageRom = onManageRom,
                        onControllerSettings = onControllerSettings,
                        onPlaybackSettings = onPlaybackSettings,
                        onInformation = onInformation,
                        homeMenu = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHomeScreen(
    state: PlayerScreenState,
    entries: List<PlayerRomEntry>?,
    onPlay: (MachineId) -> Unit,
    onImportRom: (MachineId) -> Unit,
    importEnabled: Boolean,
) {
    Column(
        Modifier.fillMaxSize().padding(top = 80.dp, start = 24.dp, end = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("機種を選択")
        PlayerMachineCatalog.definitions.forEach { machine ->
            val entry = entries?.find { it.machineId == machine.id }
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(machine.displayName + if (state is PlayerScreenState.Emulator &&
                            state.machineId == machine.id) "（選択中）" else "",
                            style = MaterialTheme.typography.titleMedium)
                        Text(when {
                            entries == null -> "ROMを確認しています…"
                            entry?.installed == true -> "登録済み"
                            else -> "ROM未登録"
                        })
                    }
                    if (entry?.installed == true) {
                        Button(onClick = { onPlay(machine.id) }) { Text("プレイ") }
                    } else {
                        Button(onClick = { onImportRom(machine.id) }, enabled = entries != null && importEnabled) {
                            Text("ROMを登録")
                        }
                    }
                }
            }
        }
        if (state is PlayerScreenState.Failure) Text("選択中のROMを復元できませんでした。ROM管理で確認するか、別の機種を選択してください。")
        Text("Playerとホームを同じ機種で往復する場合はRAMを維持します。実行中なら自動再開し、明示停止は維持します。")
        Text("別の機種へ切り替える場合は、確認後に起動し直します。RAMとロード済みプログラムは引き継ぎません。")
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
    onLoadProgram: () -> Unit = {},
    onSaveProgram: () -> Unit = {},
    onMenuVisibilityChange: (Boolean) -> Unit = {},
    onHome: () -> Unit = {},
) {
    val skin = PlayerSkinCatalog.forMachine(machineId)
    val quickModes = machineId == MachineId("pc-1245") &&
        supportedOperatingModes == setOf(OperatingMode.RUN, OperatingMode.PROGRAM)
    Box(modifier = Modifier.fillMaxSize()) {
        if (displayFrame == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (skin != null) {
            SkinnedDevice(
                skin = skin,
                operatingMode = operatingMode,
                presentationMode = presentationMode,
                displayFrame = displayFrame,
                keyboard = keyboard,
                onKeyPress = onKeyPress,
                onKeyRelease = onKeyRelease,
                modifier = Modifier.fillMaxSize().padding(top = PlayerTitleBarHeight),
            )
        } else {
            if (keyboard == null) {
                LcdDisplay(displayFrame, modifier = Modifier.fillMaxSize().padding(top = PlayerTitleBarHeight))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(top = PlayerTitleBarHeight + 8.dp, start = 8.dp, end = 8.dp)
                    .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LcdDisplay(displayFrame, modifier = Modifier.fillMaxWidth().height(100.dp))
                    keyboard.functionPanel?.let { SoftwareKeyboard(it, onKeyPress, onKeyRelease) }
                    SoftwareKeyboard(keyboard, onKeyPress, onKeyRelease)
                }
            }
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
            onLoadProgram = onLoadProgram,
            onSaveProgram = onSaveProgram,
            onMenuVisibilityChange = onMenuVisibilityChange,
            onHome = onHome,
            quickModes = quickModes,
            showOperatingModes = MachineCatalog.require(machineId).family in setOf(
                MachineFamily.PC_1245, MachineFamily.PC_1251, MachineFamily.PC_1261),
            machineTitle = MachineCatalog.require(machineId).displayName,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PlayerRunStatus(
    runState: PlayerRunState,
    onRun: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .focusProperties { canFocus = false }
            .clickable(enabled = runState != PlayerRunState.FAULTED, role = Role.Button) {
                if (runState == PlayerRunState.RUNNING) onPause() else onRun()
            }.semantics {
                contentDescription = when (runState) {
                    PlayerRunState.RUNNING -> "CPU実行中、タップでPAUSE"
                    PlayerRunState.PAUSED -> "CPU停止中、タップでRUN"
                    PlayerRunState.FAULTED -> "CPUエラー停止、メニューからResetしてください"
                }
            },
        color = Color(0xFFE8E8E8),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = when (runState) {
                PlayerRunState.RUNNING -> "● RUNNING"
                PlayerRunState.PAUSED -> "Ⅱ PAUSED"
                PlayerRunState.FAULTED -> "! FAULTED"
            },
            color = if (runState == PlayerRunState.RUNNING) Color(0xFF205B35) else Color(0xFF263238),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
    onControllerSettings: () -> Unit = {},
    onPlaybackSettings: (String) -> Unit = {},
    onLoadProgram: () -> Unit = {},
    onSaveProgram: () -> Unit = {},
    onMenuVisibilityChange: (Boolean) -> Unit = {},
    onManageRom: () -> Unit = {},
    onInformation: (String) -> Unit = {},
    onHome: () -> Unit = {},
    homeMenu: Boolean = false,
    quickModes: Boolean = false,
    showOperatingModes: Boolean = false,
    machineTitle: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf<String?>(null) }
    fun close() { expanded = false; section = null; onMenuVisibilityChange(false) }
    BackHandler(enabled = expanded) { if (section != null) section = null else close() }
    val menuDescription = stringResource(R.string.player_menu_description)
    BoxWithConstraints(modifier = modifier) {
        if (homeMenu) {
            Surface(Modifier.fillMaxWidth().heightIn(min = 56.dp), tonalElevation = 3.dp) {
                Text(
                    "Pokecom GO Player",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp, end = 64.dp, top = 14.dp, bottom = 14.dp),
                )
            }
        }
        if (machineTitle != null) {
            Surface(Modifier.fillMaxWidth().height(PlayerTitleBarHeight), tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxSize().padding(start = 16.dp, end = if (quickModes) 128.dp else 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(machineTitle, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    PlayerRunStatus(runState, onRun, onPause)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (quickModes) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 64.dp),
                shape = CircleShape,
                color = Color(0xFF1B5E20),
            ) {
                TextButton(
                    onClick = {
                        onOperatingModeChange(if (operatingMode == OperatingMode.RUN)
                            OperatingMode.PROGRAM else OperatingMode.RUN)
                    },
                    enabled = !expanded,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp).focusProperties { canFocus = false }
                        .semantics { contentDescription = "動作モード ${operatingMode.shortLabel()}、タップで${if (operatingMode == OperatingMode.RUN) "PRO" else "RUN"}へ切替" },
                ) {
                    Text(operatingMode.shortLabel(), color = Color.White, maxLines = 1)
                }
            }
        }
        IconButton(
            onClick = { expanded = true; onMenuVisibilityChange(true) },
            modifier = Modifier.align(Alignment.TopEnd).focusProperties { canFocus = homeMenu }
                .padding(top = 4.dp, end = 8.dp)
                .size(48.dp).then(if (homeMenu) Modifier else Modifier.background(Color(0xFFE8E8E8), CircleShape))
                .semantics { contentDescription = menuDescription },
        ) {
            Text(stringResource(R.string.player_menu_symbol), fontSize = 28.sp, color = Color(0xFF263238))
        }
        if (expanded) {
            Box(Modifier.fillMaxSize().background(Color(0x33000000)).clickable { close() })
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp)
                    .width(260.dp).heightIn(max = (maxHeight - 56.dp).coerceAtLeast(48.dp))
                    .clickable {},
                shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                    if (section == null) {
                        if (homeMenu) {
                            MenuEntry("設定  ›") { section = "設定" }
                            MenuEntry("ヘルプ・情報  ›") { section = "ヘルプ・情報" }
                        } else {
                            MenuEntry("エミュレータ操作  ›") { section = "エミュレータ操作" }
                            MenuEntry("プログラム  ›") { section = "プログラム" }
                            if (showOperatingModes) MenuEntry("動作モード  ›") { section = "動作モード" }
                            if (showPresentationMode) MenuEntry("表示  ›") { section = "表示" }
                            MenuEntry("ホームへ戻る") { close(); onHome() }
                        }
                    } else {
                        MenuEntry("‹ 戻る") { section = null }
                        Text(section.orEmpty(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp))
                        when (section) {
                            "設定" -> {
                                MenuEntry("ROM管理") { close(); onManageRom() }
                                MenuEntry("CPU速度") { close(); onPlaybackSettings("speed") }
                                MenuEntry("サウンド") { close(); onPlaybackSettings("sound") }
                                MenuEntry("コントローラ設定") { close(); onControllerSettings() }
                            }
                            "エミュレータ操作" -> {
                                MenuEntry(stringResource(R.string.run_emulator), runState == PlayerRunState.PAUSED) { close(); onRun() }
                                MenuEntry(stringResource(R.string.pause_emulator), runState == PlayerRunState.RUNNING) { close(); onPause() }
                                MenuEntry(stringResource(R.string.reset_emulator)) { close(); onReset() }
                            }
                            "プログラム" -> {
                                MenuEntry("ロード") { close(); onLoadProgram() }
                                MenuEntry("セーブ") { close(); onSaveProgram() }
                            }
                            "ヘルプ・情報" -> listOf("Playerとは", "Studioについて", "プライバシーポリシー", "バージョン・ライセンス").forEach { label ->
                                MenuEntry(label) { close(); onInformation(label) }
                            }
                            "動作モード" -> if (showOperatingModes) OperatingMode.entries.filter { it in supportedOperatingModes }.forEach { mode ->
                                MenuEntry("${if (mode == operatingMode) "✓ " else ""}${mode.shortLabel()}", mode != operatingMode) { close(); onOperatingModeChange(mode) }
                            }
                            "表示" -> MenuEntry(stringResource(if (presentationMode == PresentationMode.CONTROLLER_DISPLAY)
                                R.string.show_full_device else R.string.enlarge_display)) { close(); onTogglePresentationMode() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEntry(label: String, enabled: Boolean = true, action: () -> Unit) {
    TextButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

private fun OperatingMode.shortLabel(): String = when (this) {
    OperatingMode.RUN -> "RUN"
    OperatingMode.CALCULATOR -> "CAL"
    OperatingMode.PROGRAM -> "PRO"
    OperatingMode.RESERVE -> "RSV"
    OperatingMode.AER -> "AER"
}

@Composable
private fun SkinnedDevice(
    skin: SkinDefinition,
    operatingMode: OperatingMode,
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
    val imageResource = when (skin.id) {
        PlayerSkinCatalog.PC1245_SKIN_ID -> R.drawable.skin_pc1245
        PlayerSkinCatalog.PC1401_SKIN_ID -> R.drawable.skin_pc1401
        else -> error("No Android image for skin ${skin.id}")
    }
    val skinImage = ImageBitmap.imageResource(imageResource)
    val skinDescription = if (skin.id == PlayerSkinCatalog.PC1245_SKIN_ID)
        stringResource(R.string.pc1245_skin_content_description) else "PC-1401"
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
            val panel = skin.lcdPanelRegion
            Box(Modifier.offset(placement.screenX(panel.x).dp, placement.screenY(panel.y).dp)
                .width((panel.width * placement.scale).dp).height((panel.height * placement.scale).dp)
                .background(Color(0xFFB7C8A7)))
            skin.statusRegions.forEach { region ->
                val bounds = region.bounds
                SkinLcdLabel(
                    text = region.symbol.lcdLabel(),
                    color = if (displayFrame.enabled && region.symbol in displayFrame.symbols)
                        Color(0xFF26352C) else Color(0x1F26352C),
                    alignEnd = region.symbol == DisplaySymbol.SHIFT && skin.id != PlayerSkinCatalog.PC1401_SKIN_ID,
                    bold = skin.id == PlayerSkinCatalog.PC1401_SKIN_ID,
                    modifier = Modifier.offset(placement.screenX(bounds.x).dp, placement.screenY(bounds.y).dp)
                        .width((bounds.width * placement.scale).dp).height((bounds.height * placement.scale).dp),
                )
            }
            skin.operatingModeRegion?.let { bounds ->
                SkinLcdLabel(
                    text = operatingMode.shortLabel(), color = Color(0xFF26352C),
                    modifier = Modifier.offset(placement.screenX(bounds.x).dp, placement.screenY(bounds.y).dp)
                        .width((bounds.width * placement.scale).dp).height((bounds.height * placement.scale).dp),
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
                (keyboard?.rows.orEmpty().flatten() +
                    keyboard?.functionPanel?.rows.orEmpty().flatten()).associateBy(PlayerKeyCap::key)
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

/** Fits measured glyphs in skin coordinates, independent of the host font-size setting. */
@Composable
private fun SkinLcdLabel(text: String, color: Color, modifier: Modifier, alignEnd: Boolean = false, bold: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val layout = remember(measurer, text, bold) {
        measurer.measure(text, style = TextStyle(fontSize = 20.sp, lineHeight = 24.sp,
                fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal),
            density = Density(1f, 1f), softWrap = false, maxLines = 1)
    }
    Canvas(modifier.semantics { contentDescription = text }) {
        val factor = min(size.width / layout.size.width.coerceAtLeast(1), size.height / layout.size.height.coerceAtLeast(1))
        val x = if (alignEnd) size.width - layout.size.width * factor else 0f
        val y = (size.height - layout.size.height * factor) / 2f
        withTransform({ translate(x, y); scale(factor, factor, pivot = Offset.Zero) }) {
            drawText(layout, color = color, topLeft = Offset.Zero)
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
                val dotWidth = cellWidth * 0.80f
                val dotHeight = cellHeight * 0.80f
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
                    text = com.digihori.pgp.player.lcdSymbolText(frame.symbols) { it.lcdLabel() },
                    color = Color(0xFFB7C8A7),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun DisplaySymbol.lcdLabel(): String = when (this) {
    DisplaySymbol.MEMORY -> "M"
    DisplaySymbol.PARENTHESES -> "( )"
    DisplaySymbol.ERROR -> "E"
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
