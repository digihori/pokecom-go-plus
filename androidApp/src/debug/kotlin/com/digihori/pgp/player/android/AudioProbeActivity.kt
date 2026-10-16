package com.digihori.pgp.player.android

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.media.AudioManager
import android.widget.*
import com.digihori.pgp.core.api.AudioPcmSnapshot
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.player.PlayerRunner
import com.digihori.pgp.player.PlayerSessionFactory
import com.digihori.pgp.player.PlayerSessionCreationResult
import com.digihori.pgp.player.PlayerExecutionSpeed
import com.digihori.pgp.player.android.audio.AndroidPlayerAudio
import kotlin.math.sin
import java.io.FileDescriptor
import java.io.PrintWriter

/** ROM-free debug PCM probe. No preferences, ROM Library, or normal navigation. */
class AudioProbeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audio: AndroidPlayerAudio
    private lateinit var label: TextView
    private var startedAt = 0L
    private var producing = false
    private var toneHz = 2000
    private var speed = PlayerExecutionSpeed.NORMAL
    private var frames = 0L
    private var packets = 0
    private var status: String? = null
    private var coreRunner: PlayerRunner? = null
    private val tick = object : Runnable {
        override fun run() {
            if (!producing) return
            val count = 220
            // 100 ms tone, 200 ms silence, 100 ms tone, repeat. Preserve silence in packets.
            val samples = ShortArray(count) { offset ->
                val frame = frames + offset
                if (frame % 8820 in 2205 until 6615) 0
                else (sin(frame * 2.0 * Math.PI * toneHz / 22050) * 6000).toInt().toShort()
            }
            val source = coreRunner
            val pcm = if (source != null) source.tick(System.nanoTime()).audio
                ?: AudioPcmSnapshot(22050, shortArrayOf(), 0)
            else AudioPcmSnapshot(22050, if (samples.all { it == 0.toShort() }) shortArrayOf() else samples, count)
            audio.play(pcm, speed.playbackRate(pcm.sampleRate))
            frames += pcm.frameCount
            packets++
            label.text = "${speed.id}x ${toneHz}Hz packets=$packets frames=$frames ${status.orEmpty()}"
            // Synthetic PCM uses an absolute deadline; Core consumes elapsed monotonic host time.
            val elapsed = frames * 1000 * speed.denominator / (22050 * speed.numerator)
            handler.postAtTime(this, if (coreRunner == null) startedAt + elapsed else android.os.SystemClock.uptimeMillis() + 10)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        label = TextView(this)
        audio = AndroidPlayerAudio(this) { status = it }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label) }
        for (value in PlayerExecutionSpeed.entries) {
            layout.addView(Button(this).apply {
                text = "Play ${value.id}x"
                setOnClickListener { start(value, 2000) }
            })
        }
        layout.addView(Button(this).apply { text = "Play 4kHz"; setOnClickListener { start(speed, 4000) } })
        layout.addView(Button(this).apply { text = "Stop"; setOnClickListener { stop() } })
        setContentView(layout)
        label.text = "PCM probe: stopped"
    }
    private fun start(value: PlayerExecutionSpeed, hz: Int) {
        stop()
        speed = value
        toneHz = hz
        frames = 0
        packets = 0
        startedAt = android.os.SystemClock.uptimeMillis()
        if (intent.getBooleanExtra("pc1350", false)) {
            val internal = ByteArray(0x2000) { 0x4d }
            // Original LIP/LIA/EXAM/OUTC oscillator command, not firmware-derived.
            byteArrayOf(0x12, 0x5f, 0x02, if (hz == 4000) 0x30 else 0x20, 0xdb.toByte(), 0xdf.toByte()).copyInto(internal)
            val rom = (Pc1350ComponentRomImporter.importImages(
                internal, ByteArray(0x8000) { 0x4d }) as RomImportResult.Success).romSet
            coreRunner = (PlayerSessionFactory.create(rom)
                as PlayerSessionCreationResult.Success).session.runner.apply {
                val now = System.nanoTime()
                setSpeed(value, now)
                run(now)
            }
        }
        producing = true
        audio.start()
        handler.post(tick)
    }
    private fun stop() {
        producing = false
        coreRunner?.pause()
        coreRunner = null
        handler.removeCallbacks(tick)
        audio.stop()
        label.text = "PCM probe: stopped"
    }
    override fun onPause() { stop(); super.onPause() }
    override fun onDestroy() { audio.close(); super.onDestroy() }
    override fun dump(prefix: String, fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        writer.println("${prefix}PCM probe machine=${coreRunner?.machineId?.value} producing=$producing speed=${speed.id} rate=${speed.playbackRate(22050)} packets=$packets frames=$frames status=$status")
    }
}
