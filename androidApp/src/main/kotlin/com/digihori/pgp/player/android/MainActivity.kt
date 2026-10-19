package com.digihori.pgp.player.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Build
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.controller.ui.ControllerSettings
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.input.GamepadKeys
import com.digihori.pgp.input.HostKeyState
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.player.PlayerExecutionSpeed
import com.digihori.pgp.player.android.audio.AndroidPlayerAudio
import com.digihori.pgp.player.PlayerDisplayFrame
import com.digihori.pgp.player.PlayerKeyboard
import com.digihori.pgp.player.PlayerRunState
import com.digihori.pgp.player.PlayerSession
import com.digihori.pgp.player.PlayerScreenState
import com.digihori.pgp.player.PresentationMode
import java.util.concurrent.Executors

// Open only to let debug hosts reuse this flow with isolated storage/preferences.
open class MainActivity : ComponentActivity() {
    private var screenState: PlayerScreenState by mutableStateOf(PlayerScreenState.Loading)
    private var playerSession: PlayerSession? = null
    private var displayFrame: PlayerDisplayFrame? by mutableStateOf(null)
    private var keyboard: PlayerKeyboard? by mutableStateOf(null)
    private var presentationMode: PresentationMode = PresentationMode.FULL_DEVICE
    private val hostKeys = HostKeyState(
        press = { key -> playerSession?.pressKey(key) ?: InputResult.UnsupportedKey(key) },
        release = { key -> playerSession?.releaseKey(key) ?: InputResult.UnsupportedKey(key) },
    )
    private lateinit var programIo: AndroidProgramIo
    private var playerHasInputFocus = false
    private var previousWindowCallback: Window.Callback? = null
    private var playerWindowCallback: Window.Callback? = null
    private lateinit var physicalKeyboard: AndroidPhysicalKeyboard
    private lateinit var gamepad: AndroidGamepadController
    private val entryPolicy = PlayerEntryPolicy()
    private var homeLayout by mutableStateOf(PlayerHomeLayout.TILES)
    private val homePreferences by lazy { getSharedPreferences("player-home", MODE_PRIVATE) }
    private var showHome by mutableStateOf(true)
    private var showPlayerMenu by mutableStateOf(false)
    private var showControllerSettings by mutableStateOf(false)
    private var playbackSettings: String? by mutableStateOf(null)
    private var soundEnabled by mutableStateOf(true)
    private var executionSpeed by mutableStateOf(PlayerExecutionSpeed.NORMAL)
    private var audioStatus: String? by mutableStateOf(null)
    private lateinit var audio: AndroidPlayerAudio
    private val playbackPreferences by lazy { getSharedPreferences("player-playback", MODE_PRIVATE) }
    private var hostResumed by mutableStateOf(false)
    private var pendingInitialRun = false
    private var windowHasFocus by mutableStateOf(false)
    private var startRequested = false
    private var framePosted = false
    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }
    private val frameCallback = Choreographer.FrameCallback(::onEmulationFrame)
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private lateinit var romManager: AndroidRomManager
    private var informationSection: String? by mutableStateOf(null)
    private lateinit var playerVersion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeLayout = PlayerHomeLayout.fromId(runCatching { homePreferences.getString("layout", null) }.getOrNull())
        enableEdgeToEdge()
        configureSystemBars()
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        soundEnabled = runCatching { playbackPreferences.getBoolean("sound", true) }.getOrDefault(true)
        executionSpeed = PlayerExecutionSpeed.fromId(runCatching { playbackPreferences.getString("speed", null) }.getOrNull())
        audio = AndroidPlayerAudio(this) { status ->
            if (status != null && status != audioStatus) android.widget.Toast.makeText(this, status, android.widget.Toast.LENGTH_LONG).show()
            audioStatus = status
        }
        audio.setEnabled(soundEnabled)
        gamepad = AndroidGamepadController(this,
            press = { button, key -> hostKeys.press("gamepad:${button.name}", key) },
            release = { button, _ -> hostKeys.release("gamepad:${button.name}") },
        )
        gamepad.start()
        physicalKeyboard = AndroidPhysicalKeyboard(this,
            press = { owner, key -> hostKeys.press(owner, key) },
            release = { owner -> hostKeys.release(owner) },
        )
        physicalKeyboard.start()
        programIo = AndroidProgramIo(this, ioExecutor, { playerSession }, ::pauseEmulator,
            { playerSession?.let(::refreshSession) }, ::requestAutomaticRun, ::updateGamepadContext)
        romManager = AndroidRomManager(this, ioExecutor, { playerSession }, ::pauseEmulator,
            ::publishSession, ::replaceScreenState, ::updateGamepadContext)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        playerVersion = "${packageInfo.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)})"
        setContent {
            val playerInputFocus = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val playerOwnsFocus = !showHome && screenState is PlayerScreenState.Emulator &&
                !showPlayerMenu && !showControllerSettings && playbackSettings == null && informationSection == null &&
                programIo.dialog == null && romManager.state == null
            LaunchedEffect(playerOwnsFocus, hostResumed, windowHasFocus,
                (screenState as? PlayerScreenState.Emulator)?.runState) {
                if (playerOwnsFocus && hostResumed && windowHasFocus) {
                    focusManager.clearFocus(force = true)
                    playerInputFocus.requestFocus()
                }
            }
            BackHandler(enabled = !showHome) { returnHome() }
            BackHandler(enabled = playbackSettings != null) { playbackSettings = null; updateGamepadContext() }
            BackHandler(enabled = showControllerSettings) { closeControllerSettings() }
            Box(Modifier.fillMaxSize().focusRequester(playerInputFocus)
                .onFocusChanged { playerHasInputFocus = it.isFocused }
                .focusable(enabled = playerOwnsFocus)) {
                PlayerApp(
                    screenState = screenState,
                    showHome = showHome,
                    homeLayout = homeLayout,
                    onHomeLayoutChange = { layout ->
                        homeLayout = layout
                        homePreferences.edit().putString("layout", layout.id).apply()
                    },
                    onPlay = ::openPlayer,
                    onHome = ::returnHome,
                    displayFrame = displayFrame,
                    keyboard = keyboard,
                    onImportRom = { romManager.importRom(playerSession?.machineId ?: com.digihori.pgp.core.rom.MachineId("pc-1245"), fromHome = showHome) },
                    homeRomEntries = romManager.entries,
                    onImportMachineRom = { romManager.importRom(it, fromHome = true) },
                    onManageRom = romManager::open,
                    onInformation = { section ->
                        releaseAllKeys()
                        informationSection = section
                        updateGamepadContext()
                    },
                    onMenuVisibilityChange = { open ->
                        showPlayerMenu = open
                        if (open) releaseAllKeys()
                        updateGamepadContext()
                    },
                    onLoadProgram = programIo::openLoad,
                    onSaveProgram = programIo::openSave,
                    onKeyPress = ::pressKey,
                    onKeyRelease = ::releaseKey,
                    onRun = ::runEmulator,
                    onPause = ::pauseEmulator,
                    onReset = ::resetEmulator,
                    onTogglePresentationMode = ::togglePresentationMode,
                    onOperatingModeChange = ::setOperatingMode,
                    romImportEnabled = screenState != PlayerScreenState.Loading,
                    onPlaybackSettings = { section ->
                        playbackSettings = section
                        releaseAllKeys()
                        updateGamepadContext()
                    },
                    onControllerSettings = {
                        showControllerSettings = true
                        releaseAllKeys()
                        updateGamepadContext()
                    },
                )
                ProgramDialogs(programIo)
                RomManagementScreen(romManager, playerSession?.machineId)
                informationSection?.let { section ->
                    PlayerInformationScreen(section, playerVersion) {
                        informationSection = null
                        updateGamepadContext()
                    }
                }
                playbackSettings?.let { section ->
                    PlayerPlaybackSettings(section, soundEnabled, executionSpeed, audioStatus,
                        onSound = { enabled ->
                            soundEnabled = enabled
                            playbackPreferences.edit().putBoolean("sound", enabled).apply()
                            audio.setEnabled(enabled)
                        },
                        onSpeed = { speed ->
                            executionSpeed = speed
                            playbackPreferences.edit().putString("speed", speed.id).apply()
                            playerSession?.runner?.setSpeed(speed, System.nanoTime())
                            audio.stop()
                        },
                        onBack = { playbackSettings = null; updateGamepadContext() })
                }
                if (showControllerSettings) {
                    MaterialTheme {
                        Surface(Modifier.fillMaxSize()) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                .windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
                                androidx.compose.material3.TextButton(onClick = ::closeControllerSettings) {
                                    androidx.compose.material3.Text("戻る")
                                }
                                androidx.compose.material3.Text("対象キー：${(playerSession?.machineId
                                    ?: MachineCatalog.defaultDefinition.id).value.uppercase()}")
                                androidx.compose.material3.Text("割当は全機種で共通です。この機種にないキーの割当は保持されますが、入力されません。")
                                ControllerSettings(
                                    settings = gamepad.settings,
                                    devices = gamepad.devices,
                                    selectedId = gamepad.selectedId,
                                    pressed = gamepad.pressed,
                                    allowedKeys = GamepadKeys.forMachine(playerSession?.machineId
                                        ?: MachineCatalog.defaultDefinition.id),
                                    availableButtons = gamepad.available,
                                    status = gamepad.status,
                                    onSelectDevice = gamepad::select,
                                    onSettingsChange = gamepad::save,
                                )
                            }
                        }
                    }
                }
            }
        }
        installPlayerKeyDispatch()
        romManager.restore()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        releaseAllKeys()
        configureSystemBars()
        playerSession?.let(::refreshSession)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        windowHasFocus = hasFocus
        if (hasFocus) configureSystemBars() else releaseAllKeys()
        updateGamepadContext()
    }

    private fun handlePlayerKey(event: KeyEvent): Boolean {
        val actualFocus = window.decorView.hasWindowFocus()
        if (windowHasFocus != actualFocus) {
            windowHasFocus = actualFocus
            if (!actualFocus) releaseAllKeys()
            updateGamepadContext()
        }
        if (::gamepad.isInitialized && gamepad.onKey(event)) return true
        return ::physicalKeyboard.isInitialized && physicalKeyboard.onKey(event)
    }

    /** Dispatch before Compose/native focus handling; text fields keep their normal input while modal. */
    private fun installPlayerKeyDispatch() {
        val previous = window.callback ?: return
        val callback = object : Window.Callback by previous {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                handlePlayerKey(event) || previous.dispatchKeyEvent(event)
        }
        previousWindowCallback = previous
        playerWindowCallback = callback
        window.callback = callback
    }

    /** Debug-only on-demand state; no key values, ROM data, logging, or persistence. */
    override fun dump(prefix: String, fd: java.io.FileDescriptor?, writer: java.io.PrintWriter,
                      args: Array<out String>?) {
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
            args?.contains("--player-input") == true) {
            writer.println("${prefix}Player input: resumed=$hostResumed focus=$windowHasFocus " +
                "actualFocus=${window.decorView.hasWindowFocus()} home=$showHome menu=$showPlayerMenu " +
                "programModal=${programIo.dialog != null} romModal=${romManager.state != null} " +
                "controller=$showControllerSettings information=${informationSection != null} " +
                "run=${playerSession?.runner?.state} framePosted=$framePosted " +
                "callbackInstalled=${window.callback === playerWindowCallback} inputFocus=$playerHasInputFocus")
            writer.println("${prefix}machine=${playerSession?.machineId?.value} mode=${playerSession?.operatingMode} " +
                "lcdSymbols=${displayFrame?.symbols?.joinToString { it.name }} heldKeys=${PocketKey.entries.count(hostKeys::isHeld)}")
            writer.println("${prefix}playback speed=${playerSession?.runner?.speed?.id} savedSpeed=${executionSpeed.id} sound=$soundEnabled audioStatus=$audioStatus")
            writer.println("${prefix}${physicalKeyboard.diagnosticState()}")
            return
        }
        super.dump(prefix, fd, writer, args)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        if (::gamepad.isInitialized && gamepad.onMotion(event)) true else super.onGenericMotionEvent(event)

    private fun openPlayer(machineId: com.digihori.pgp.core.rom.MachineId) {
        if (playerSession?.machineId != machineId) {
            romManager.use(machineId, fromHome = true) { enterPlayer() }
        } else enterPlayer()
    }

    private fun enterPlayer() {
        if (playerSession == null) return
        pauseEmulator()
        showHome = false
        if (entryPolicy.shouldRun) requestAutomaticRun()
        updateGamepadContext()
    }

    private fun returnHome() {
        playerSession?.let { entryPolicy.leavingPlayer(it.runner.state, startRequested || pendingInitialRun) }
        pauseEmulator()
        showPlayerMenu = false
        showHome = true
        updateGamepadContext()
    }

    private fun closeControllerSettings() {
        showControllerSettings = false
        gamepad.reset()
        updateGamepadContext()
    }

    private fun updateGamepadContext() {
        if (::physicalKeyboard.isInitialized) physicalKeyboard.input.configure(
            hostResumed && windowHasFocus && !showHome && !showPlayerMenu &&
                !showControllerSettings && playbackSettings == null && informationSection == null &&
                (!::programIo.isInitialized || programIo.dialog == null) &&
                (!::romManager.isInitialized || romManager.state == null) &&
                playerSession?.runner?.state == PlayerRunState.RUNNING,
            keyboard?.let { it.rows.flatten().map { cap -> cap.key }.toSet() +
                it.functionPanel?.rows?.flatten().orEmpty().map { cap -> cap.key } }.orEmpty(),
            pc1350 = playerSession?.machineId?.value == "pc-1350",
            pc1360 = playerSession?.machineId?.value == "pc-1360",
            pc1360k = playerSession?.machineId?.value == "pc-1360k",
            minimumHoldCycles = playerSession?.machineId?.let { id ->
                MachineCatalog.require(id).automaticKeyHoldCycles
            } ?: 0L,
            minimumGapCycles = playerSession?.machineId?.let { id ->
                MachineCatalog.require(id).automaticKeyGapCycles
            } ?: 0L,
        )
        if (::gamepad.isInitialized) gamepad.configure(
            playerSession?.machineId,
            hostResumed && windowHasFocus && playerSession?.runner?.state == PlayerRunState.RUNNING,
            showHome || showPlayerMenu || showControllerSettings || playbackSettings != null || informationSection != null ||
                (::programIo.isInitialized && programIo.dialog != null) ||
                (::romManager.isInitialized && romManager.state != null),
        )
    }

    override fun onDestroy() {
        if (window.callback === playerWindowCallback) window.callback = previousWindowCallback
        playerWindowCallback = null
        previousWindowCallback = null
        stopFrameLoop()
        releaseAllKeys()
        if (::gamepad.isInitialized) gamepad.close()
        if (::physicalKeyboard.isInitialized) physicalKeyboard.close()
        if (::audio.isInitialized) audio.close()
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        hostResumed = true
        configureSystemBars()
        if (pendingInitialRun) runEmulator()
        updateGamepadContext()
    }

    override fun onPause() {
        hostResumed = false
        val waitingForAutomaticStart = pendingInitialRun
        pauseEmulator()
        pendingInitialRun = waitingForAutomaticStart
        releaseAllKeys()
        super.onPause()
    }

    private fun configureSystemBars() {
        // On legacy window-insets implementations, the view flag alone can leave a status-bar
        // inset on OEM devices. Keep the window fullscreen as well; API 30+ uses the controller.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
        if (usesGestureNavigation()) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    /** Android exposes no public navigation-mode API; use the platform config and fail safe. */
    @SuppressLint("DiscouragedApi")
    private fun usesGestureNavigation(): Boolean {
        val resourceId = resources.getIdentifier(
            "config_navBarInteractionMode",
            "integer",
            "android",
        )
        return resourceId != 0 && resources.getInteger(resourceId) == GESTURE_NAVIGATION_MODE
    }

    private fun publishSession(session: PlayerSession) {
        runOnUiThread {
            if (!isDestroyed) {
                stopFrameLoop()
                audio.stop()
                releaseAllKeys()
                entryPolicy.newSession()
                presentationMode = PresentationMode.FULL_DEVICE
                session.runner.setSpeed(executionSpeed, System.nanoTime())
                playerSession = session
                keyboard = session.keyboard()
                if (session.machineId.value == "pc-1245") session.setOperatingMode(OperatingMode.RUN)
                refreshSession(session)
                if (!showHome) requestAutomaticRun()
            }
        }
    }

    private fun requestAutomaticRun() {
        if (showHome) return
        pendingInitialRun = true
        if (hostResumed) runEmulator()
    }

    private fun replaceScreenState(state: PlayerScreenState) {
        pauseEmulator()
        pendingInitialRun = false
        playerSession = null
        showHome = true
        displayFrame = null
        keyboard = null
        screenState = state
        updateGamepadContext()
    }

    private fun pressKey(id: String, key: PocketKey) {
        hostKeys.press("screen:$id", key)
    }

    private fun releaseKey(id: String) {
        hostKeys.release("screen:$id")
    }

    private fun releaseAllKeys() {
        if (::physicalKeyboard.isInitialized) physicalKeyboard.input.reset()
        if (::gamepad.isInitialized) gamepad.reset()
        hostKeys.releaseAll()
    }

    private fun runEmulator() {
        val session = playerSession ?: return
        if (showHome || !hostResumed || session.runner.state != PlayerRunState.PAUSED || startRequested) return
        startRequested = true
        postFrame()
    }

    private fun pauseEmulator() {
        if (::audio.isInitialized) audio.stop()
        pendingInitialRun = false
        startRequested = false
        stopFrameLoop()
        releaseAllKeys()
        playerSession?.let { session ->
            session.runner.pause()
            refreshSession(session)
        }
    }

    private fun resetEmulator() {
        audio.stop()
        startRequested = false
        stopFrameLoop()
        releaseAllKeys()
        playerSession?.let { session ->
            session.reset()
            refreshSession(session)
            requestAutomaticRun()
        }
    }

    private fun setOperatingMode(mode: OperatingMode) {
        val session = playerSession ?: return
        releaseAllKeys()
        if (session.setOperatingMode(mode)) refreshSession(session)
    }

    private fun togglePresentationMode() {
        val session = playerSession ?: return
        releaseAllKeys()
        presentationMode = presentationMode.toggleControllerDisplay()
        refreshSession(session)
    }

    private fun onEmulationFrame(frameTimeNanoseconds: Long) {
        framePosted = false
        if (!hostResumed) return
        val session = playerSession ?: return
        if (startRequested) {
            pendingInitialRun = false
            startRequested = false
            session.runner.run(frameTimeNanoseconds)
            audio.start()
        }
        if (session.runner.state == PlayerRunState.RUNNING) {
            val tick = session.runner.tick(frameTimeNanoseconds)
            physicalKeyboard.input.advanceCycles(tick.runResult?.executedCycles ?: 0L)
            if (session.runner.state == PlayerRunState.RUNNING) {
                tick.audio?.let { audio.play(it, tick.speed.playbackRate(it.sampleRate)) }
            } else audio.stop()
        }
        refreshSession(session)
        if (session.runner.state == PlayerRunState.RUNNING) postFrame()
    }

    private fun postFrame() {
        if (framePosted) return
        framePosted = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun stopFrameLoop() {
        startRequested = false
        if (framePosted) choreographer.removeFrameCallback(frameCallback)
        framePosted = false
    }

    private fun refreshSession(session: PlayerSession) {
        updateGamepadContext()
        displayFrame = session.displayFrame()
        screenState = session.screenState(presentationMode)
    }

    private companion object {
        const val GESTURE_NAVIGATION_MODE = 2
    }
}
