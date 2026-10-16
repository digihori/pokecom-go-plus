package com.digihori.pgp.player.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.input.*
import com.digihori.pgp.player.*
import java.io.FileDescriptor
import java.io.PrintWriter

/** Debug-only adapter check. Original NOPW instructions, isolated preferences, no ROM Library access. */
class GamepadProbeActivity : Activity() {
    private var player = session(false)
    private var resumed = false
    private var focused = false
    private var modal = false
    private var downs = 0
    private var ups = 0
    private var accepted = 0
    private var keyEvents = 0
    private var lastKey = "none"
    private val owners = HostKeyState({ key ->
        downs++
        player.pressKey(key).also { if (it is InputResult.Accepted) accepted++ }
    }, { key -> ups++; player.releaseKey(key) })
    private lateinit var pad: AndroidGamepadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("pc1350", false)) player = session1350()
        // The production adapter is exercised unchanged. Its writes go only to the probe namespace.
        val isolated = object : ContextWrapper(this) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("gamepad-probe-$name", Context.MODE_PRIVATE)
        }
        pad = AndroidGamepadController(isolated,
            { button, key -> owners.press("gamepad:$button", key) },
            { button, _ -> owners.release("gamepad:$button") })
        pad.save(GamepadSettings(true, mapOf(
            GamepadButton.UP to PocketKey.UP, GamepadButton.DOWN to PocketKey.DOWN,
            GamepadButton.LEFT to PocketKey.LEFT, GamepadButton.RIGHT to PocketKey.RIGHT,
            GamepadButton.SOUTH to PocketKey.NUM_4, GamepadButton.WEST to PocketKey.NUM_4,
            GamepadButton.EAST to PocketKey.ENTER, GamepadButton.NORTH to if (player.machineId.value == "pc-1350") PocketKey.MODE else PocketKey.BASIC,
            GamepadButton.L1 to PocketKey.SHIFT, GamepadButton.R1 to if (player.machineId.value == "pc-1350") PocketKey.KANA else PocketKey.CAL,
        )))
        pad.start()
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        layout.addView(TextView(this).apply { text = "ROM-free gamepad adapter probe (isolated settings)" })
        fun action(label: String, block: () -> Unit) {
            layout.addView(Button(this).apply { text = label; setOnClickListener { block(); configure() } })
        }
        action("Modal") { modal = !modal }
        action("Reset") { pad.reset(); owners.releaseAll(); player.reset() }
        action("Disable") { pad.save(pad.settings.copy(enabled = !pad.settings.enabled)) }
        action("Remap") { pad.save(pad.settings.withBinding(GamepadButton.SOUTH,
            if (pad.settings.bindings[GamepadButton.SOUTH] == PocketKey.NUM_4) PocketKey.CAL else PocketKey.NUM_4)) }
        for ((label, is1245) in listOf("PC-1245" to true, "PC-1401" to false)) {
            action(label) { pad.reset(); owners.releaseAll(); player = session(is1245) }
        }
        action("PC-1350") { pad.reset(); owners.releaseAll(); player = session1350() }
        setContentView(layout)
        ViewCompat.requestApplyInsets(layout)
    }

    private fun configure() {
        if (::pad.isInitialized) pad.configure(player.machineId, resumed && focused, modal)
    }
    override fun onResume() { super.onResume(); resumed = true; configure() }
    override fun onPause() { resumed = false; configure(); owners.releaseAll(); super.onPause() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus); focused = hasFocus; configure()
    }
    override fun onDestroy() { pad.close(); owners.releaseAll(); super.onDestroy() }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyEvents++
        lastKey = "${event.action}:${event.keyCode}@${event.deviceId}"
        return (::pad.isInitialized && pad.onKey(event)) || super.dispatchKeyEvent(event)
    }
    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        (::pad.isInitialized && pad.onMotion(event)) || super.onGenericMotionEvent(event)

    override fun dump(prefix: String, fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        if (args?.contains("--gamepad-probe") == true) {
            writer.println("${prefix}machine=${player.machineId.value} resumed=$resumed focus=$focused modal=$modal enabled=${pad.settings.enabled}")
            writer.println("${prefix}devices=${pad.devices.map { it.id }} selected=${pad.selectedId} available=${pad.available.joinToString { it.name }}")
            writer.println("${prefix}pressed=${pad.pressed.joinToString { it.name }} held=${PocketKey.entries.filter(owners::isHeld).joinToString { it.name }} downs=$downs ups=$ups accepted=$accepted")
            writer.println("${prefix}keyEvents=$keyEvents lastKey=$lastKey")
            return
        }
        super.dump(prefix, fd, writer, args)
    }

    private fun session1350(): PlayerSession {
        val rom = (Pc1350ComponentRomImporter.importImages(ByteArray(0x2000) { 0x4d },
            ByteArray(0x8000) { 0x4d }) as RomImportResult.Success).romSet
        return (PlayerSessionFactory.create(rom) as PlayerSessionCreationResult.Success).session
    }

    private fun session(is1245: Boolean): PlayerSession {
        // Generated instructions only; never read a firmware image or the user's ROM Library.
        val imported = if (is1245) Pc1245ComponentRomImporter.importImages(
            ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d },
            ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d },
        ) else Pc1401ComponentRomImporter.importImages(ByteArray(0x2000) { 0x4d }, ByteArray(0x8000) { 0x4d })
        val rom = (imported as RomImportResult.Success).romSet
        return (PlayerSessionFactory.create(rom) as PlayerSessionCreationResult.Success).session
    }
}
