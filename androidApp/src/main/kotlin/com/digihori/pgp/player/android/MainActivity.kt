package com.digihori.pgp.player.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.player.PlayerDisplayFrame
import com.digihori.pgp.player.PlayerFailureKind
import com.digihori.pgp.player.PlayerKeyboard
import com.digihori.pgp.player.PlayerRunState
import com.digihori.pgp.player.PlayerSession
import com.digihori.pgp.player.PlayerSessionCreationResult
import com.digihori.pgp.player.PlayerSessionFactory
import com.digihori.pgp.player.PlayerScreenState
import com.digihori.pgp.player.PresentationMode
import com.digihori.pgp.player.android.rom.AndroidRomImporter
import com.digihori.pgp.player.android.rom.AndroidRomLibrary
import com.digihori.pgp.player.android.rom.AndroidRomPackage
import com.digihori.pgp.player.android.rom.RomImportOutcome
import com.digihori.pgp.player.android.rom.RomPackageReadResult
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var screenState: PlayerScreenState by mutableStateOf(PlayerScreenState.Loading)
    private var playerSession: PlayerSession? = null
    private var displayFrame: PlayerDisplayFrame? by mutableStateOf(null)
    private var keyboard: PlayerKeyboard? by mutableStateOf(null)
    private var presentationMode: PresentationMode = PresentationMode.FULL_DEVICE
    private val pressedKeys = mutableSetOf<PocketKey>()
    private var startRequested = false
    private var framePosted = false
    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }
    private val frameCallback = Choreographer.FrameCallback(::onEmulationFrame)
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val romPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        pauseEmulator()
        releaseAllKeys()
        playerSession = null
        displayFrame = null
        keyboard = null
        screenState = PlayerScreenState.Loading
        ioExecutor.execute {
            val outcome = AndroidRomImporter(
                contentResolver,
                AndroidRomLibrary(filesDir),
            ).import(uri)
            when (outcome) {
                is RomImportOutcome.Success -> publishSession(outcome.session)
                is RomImportOutcome.Failure -> publish(PlayerScreenState.Failure(outcome.kind))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        setContent {
            PlayerApp(
                screenState = screenState,
                displayFrame = displayFrame,
                keyboard = keyboard,
                onImportRom = { romPicker.launch(arrayOf("*/*")) },
                onKeyPress = ::pressKey,
                onKeyRelease = ::releaseKey,
                onRun = ::runEmulator,
                onPause = ::pauseEmulator,
                onReset = ::resetEmulator,
                onTogglePresentationMode = ::togglePresentationMode,
                onOperatingModeChange = ::setOperatingMode,
                romImportEnabled = screenState != PlayerScreenState.Loading,
            )
        }
        loadInstalledRom()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) configureSystemBars()
    }

    override fun onDestroy() {
        stopFrameLoop()
        releaseAllKeys()
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onStop() {
        pauseEmulator()
        releaseAllKeys()
        super.onStop()
    }

    private fun loadInstalledRom() {
        ioExecutor.execute {
            val bytes = try {
                AndroidRomLibrary(filesDir).loadActive()
            } catch (_: Exception) {
                publish(PlayerScreenState.Failure(PlayerFailureKind.STORAGE))
                return@execute
            }
            if (bytes == null) {
                publish(PlayerScreenState.RomMissing)
                return@execute
            }
            val parsed = AndroidRomPackage.read(bytes)
            if (parsed !is RomPackageReadResult.Success) {
                publish(PlayerScreenState.Failure(PlayerFailureKind.ROM_VALIDATION))
                return@execute
            }
            when (val created = PlayerSessionFactory.create(parsed.romSet)) {
                is PlayerSessionCreationResult.Success -> publishSession(created.session)
                is PlayerSessionCreationResult.Failure -> publish(
                    PlayerScreenState.Failure(PlayerFailureKind.SESSION_CREATION),
                )
            }
        }
    }

    private fun configureSystemBars() {
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
                playerSession = session
                keyboard = session.keyboard()
                presentationMode = PresentationMode.FULL_DEVICE
                refreshSession(session)
            }
        }
    }

    private fun publish(state: PlayerScreenState) {
        runOnUiThread {
            if (!isDestroyed) screenState = state
        }
    }

    private fun pressKey(key: PocketKey) {
        if (pressedKeys.add(key)) playerSession?.pressKey(key)
    }

    private fun releaseKey(key: PocketKey) {
        if (pressedKeys.remove(key)) playerSession?.releaseKey(key)
    }

    private fun releaseAllKeys() {
        val session = playerSession
        pressedKeys.forEach { session?.releaseKey(it) }
        pressedKeys.clear()
    }

    private fun runEmulator() {
        val session = playerSession ?: return
        if (session.runner.state != PlayerRunState.PAUSED || startRequested) return
        startRequested = true
        postFrame()
    }

    private fun pauseEmulator() {
        startRequested = false
        stopFrameLoop()
        playerSession?.let { session ->
            session.runner.pause()
            refreshSession(session)
        }
    }

    private fun resetEmulator() {
        startRequested = false
        stopFrameLoop()
        releaseAllKeys()
        playerSession?.let { session ->
            session.reset()
            refreshSession(session)
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
        val session = playerSession ?: return
        if (startRequested) {
            startRequested = false
            session.runner.run(frameTimeNanoseconds)
        }
        if (session.runner.state == PlayerRunState.RUNNING) {
            session.runner.tick(frameTimeNanoseconds)
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
        displayFrame = session.displayFrame()
        screenState = session.screenState(presentationMode)
    }

    private companion object {
        const val GESTURE_NAVIGATION_MODE = 2
    }
}
