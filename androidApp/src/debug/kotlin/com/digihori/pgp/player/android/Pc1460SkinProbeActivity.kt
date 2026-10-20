package com.digihori.pgp.player.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxModel
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.*
import com.digihori.pgp.player.skin.*
import java.io.FileDescriptor
import java.io.PrintWriter

/** Debug-only, original generated CPU instructions; never opens ROM Library or preferences. */
class Pc1460SkinProbeActivity : ComponentActivity() {
    private val pressed = mutableSetOf<PocketKey>()
    private val released = mutableSetOf<PocketKey>()
    private val centers = mutableMapOf<PocketKey, Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (intent.getBooleanExtra("systemBars", false)) show(WindowInsetsCompat.Type.systemBars())
            else hide(WindowInsetsCompat.Type.systemBars())
        }
        val session = structuralSession()
        val skin = checkNotNull(PlayerSkinCatalog.forMachine(MachineId("pc-1460")))
        setContent {
            var mode by remember { mutableStateOf(if (intent.getBooleanExtra("lcd", false))
                PresentationMode.CONTROLLER_DISPLAY else PresentationMode.FULL_DEVICE) }
            var running by remember { mutableStateOf(false) }
            val density = LocalDensity.current.density
            Box(Modifier.fillMaxSize()) {
                PlayerApp(
                    screenState = session.screenState(mode).copy(
                        runState = if (running) PlayerRunState.RUNNING else PlayerRunState.PAUSED),
                    displayFrame = session.displayFrame(), keyboard = session.keyboard(),
                    onImportRom = {}, romImportEnabled = false,
                    onKeyPress = { _, key -> pressed += key },
                    onKeyRelease = { released += PocketKey.valueOf(it) },
                    onRun = { running = true }, onPause = { running = false }, onReset = {},
                    onTogglePresentationMode = {
                        mode = if (mode == PresentationMode.FULL_DEVICE)
                            PresentationMode.CONTROLLER_DISPLAY else PresentationMode.FULL_DEVICE
                    },
                    onOperatingModeChange = {}, onHome = { finish() },
                )
                BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.displayCutout).union(WindowInsets.statusBars))
                    .padding(top = PlayerTitleBarHeight)) {
                    val placement = SkinLayoutEngine.calculate(skin, SkinLayoutRequest(
                        SkinSize(maxWidth.value, maxHeight.value), mode = mode))
                    Canvas(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                        val origin = coordinates.positionInWindow()
                        centers.clear()
                        skin.keyRegions.forEach { key ->
                            val b = key.bounds
                            centers[key.key] = Pair(
                                (origin.x + placement.screenX(b.x + b.width / 2) * density).toInt(),
                                (origin.y + placement.screenY(b.y + b.height / 2) * density).toInt())
                        }
                    }) {
                        if (intent.getBooleanExtra("regions", false)) {
                            (skin.keyRegions.map { it.bounds } + skin.lcdContentRegion +
                                skin.statusRegions.map { it.bounds }).forEach { b ->
                                drawRect(Color.Magenta,
                                    Offset(placement.screenX(b.x) * density, placement.screenY(b.y) * density),
                                    Size(b.width * placement.scale * density, b.height * placement.scale * density),
                                    style = Stroke(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dump(prefix: String, fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        super.dump(prefix, fd, writer, args)
        if (args?.contains("--skin-probe") == true) {
            writer.println("${prefix}skinProbe pressed=${pressed.size} released=${released.size}")
            writer.println("${prefix}pressed=${pressed.joinToString { it.name }}")
            writer.println("${prefix}released=${released.joinToString { it.name }}")
            centers.forEach { (key, point) -> writer.println("${prefix}keyCenter ${key.name} ${point.first} ${point.second}") }
        }
    }

    private fun structuralSession(): PlayerSession {
        val internal = ByteArray(0x2000) { 0x4d.toByte() }
        val program = mutableListOf<Int>()
        fun write(address: Int, value: Int) {
            program += listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52)
        }
        repeat(120) { column ->
            write(when (column) {
                in 0..29 -> 0x3000 + column
                in 30..44 -> 0x302d + column - 30
                in 45..59 -> 0x301e + column - 45
                in 60..74 -> 0x306c - (column - 60)
                in 75..89 -> 0x307b - (column - 75)
                else -> 0x305d - (column - 90) },
                if (column % 2 == 0) 0x55 else 0x2a)
        }
        write(0x303c, 31); write(0x303d, 0x7f); write(0x307c, 0x7f)
        val loop = program.size
        program += listOf(0x4d, 0x79, loop shr 8, loop and 255)
        program.forEachIndexed { index, value -> internal[index] = value.toByte() }
        val rom = (Pc14xxComponentRomImporter.importImages(Pc14xxModel.PC_1460, internal, ByteArray(0x10000))
            as RomImportResult.Success).romSet
        return (PlayerSessionFactory.create(rom) as PlayerSessionCreationResult.Success).session.apply {
            runner.run(0)
            repeat(20) { runner.tick((it + 1) * 20_000_000L) }
            runner.pause()
        }
    }
}
