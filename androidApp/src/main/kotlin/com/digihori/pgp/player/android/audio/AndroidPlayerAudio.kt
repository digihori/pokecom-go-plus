package com.digihori.pgp.player.android.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.digihori.pgp.core.api.AudioPcmSnapshot

/** Main-thread lifecycle/focus policy. Losing audio focus never changes CPU state. */
internal class AndroidPlayerAudio(
    private val context: Context,
    private val onStatus: (String?) -> Unit,
) : AutoCloseable {
    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(AudioManager::class.java)
    private var active = false
    private var enabled = true
    private var blocked = false
    private var transientLoss = false
    private var focusHeld = false
    private var generation = 0L
    private var closed = false
    private val output = AndroidPcmOutput { failedGeneration ->
        handler.post {
            if (!closed && active && failedGeneration == generation) {
                blocked = true
                generation = outputStop()
                abandonFocus()
                onStatus("音声を出力できません。サウンドをOFF→ONにするか、CPUを再開すると再試行します。")
            }
        }
    }
    private var focus: AudioFocusRequest? = null
    private var focusGeneration = 0L

    private fun createFocusRequest(): AudioFocusRequest {
        val token = ++focusGeneration
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setOnAudioFocusChangeListener({ change ->
                if (token == focusGeneration && !closed) onFocusChanged(change)
            }, handler).build()
    }

    private fun onFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (active && enabled && transientLoss && !blocked) {
                    focusHeld = true
                    transientLoss = false
                    generation = output.stop(retry = true)
                    onStatus(null)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                focusHeld = false
                transientLoss = true
                generation = output.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                blocked = true
                transientLoss = false
                generation = output.stop()
                abandonFocus()
                if (active) onStatus("他のアプリへ音声出力を譲りました。CPUの再開またはサウンドOFF→ONで再試行できます。")
            }
        }
    }
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY || !active) return
            blocked = true
            transientLoss = false
            generation = output.stop()
            abandonFocus()
            onStatus("音声出力先が切断されました。CPUの再開またはサウンドOFF→ONで再試行できます。")
        }
    }
    init {
        ContextCompat.registerReceiver(context, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    fun setEnabled(value: Boolean) {
        enabled = value
        stop()
    }
    fun start() {
        active = true
        blocked = false
        transientLoss = false
        generation = output.stop(retry = true)
        onStatus(null)
    }
    fun play(pcm: AudioPcmSnapshot, playbackRate: Int) {
        if (!active || !enabled || blocked || transientLoss) return
        if (!focusHeld) {
            if (pcm.sampleCount == 0) return
            val request = focus ?: createFocusRequest().also { focus = it }
            focusHeld = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!focusHeld) {
                blocked = true
                generation = output.stop()
                onStatus("音声フォーカスを取得できません。CPUの再開またはサウンドOFF→ONで再試行できます。")
                return
            }
        }
        generation = output.offer(pcm, playbackRate)
    }
    fun stop() {
        active = false
        transientLoss = false
        generation = output.stop(retry = true)
        abandonFocus()
        onStatus(null)
    }
    private fun outputStop(): Long = output.stop()
    private fun abandonFocus() {
        focusGeneration++
        val previous = focus
        focus = null
        if (previous != null) manager.abandonAudioFocusRequest(previous)
        focusHeld = false
    }
    override fun close() {
        closed = true
        stop()
        context.unregisterReceiver(noisy)
        output.close()
    }
}
