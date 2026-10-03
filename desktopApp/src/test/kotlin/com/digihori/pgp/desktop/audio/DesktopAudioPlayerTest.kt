package com.digihori.pgp.desktop.audio

import com.digihori.pgp.core.api.AudioSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopAudioPlayerTest {
    @Test
    fun startsReplacesAndStopsToneClipsOnRevisionChanges() {
        val factory = FakeClipFactory()
        val player = DesktopAudioPlayer(factory)

        player.update(AudioSnapshot(2_000, 1)).getOrThrow()
        player.update(AudioSnapshot(2_000, 1)).getOrThrow()
        player.update(AudioSnapshot(4_000, 2)).getOrThrow()
        player.update(AudioSnapshot(0, 3)).getOrThrow()

        assertEquals(listOf(2_000, 4_000), factory.frequencies)
        assertTrue(factory.clips.all { it.started })
        assertTrue(factory.clips.all { it.closed })
    }

    @Test
    fun stopAllowsTheSameSnapshotToRestart() {
        val factory = FakeClipFactory()
        val player = DesktopAudioPlayer(factory)
        val snapshot = AudioSnapshot(2_000, 1)

        player.update(snapshot).getOrThrow()
        player.stop()
        player.update(snapshot).getOrThrow()

        assertEquals(listOf(2_000, 2_000), factory.frequencies)
    }

    @Test
    fun returnsAudioDeviceFailureWithoutThrowingFromUpdate() {
        val player = DesktopAudioPlayer { error("no audio device") }

        val result = player.update(AudioSnapshot(2_000, 1))

        assertTrue(result.isFailure)
    }

    @Test
    fun generatesWholePeriodLittleEndianPcm() {
        val pcm = squareWavePcm(2_000)

        assertEquals(882, pcm.size)
        assertEquals(0x00, pcm[0].toInt() and 0xff)
        assertEquals(0x10, pcm[1].toInt() and 0xff)
        assertTrue(pcm.any { it.toInt() < 0 })
        assertFailsWith<IllegalArgumentException> { squareWavePcm(0) }
    }

    private class FakeClipFactory : ToneClipFactory {
        val frequencies = mutableListOf<Int>()
        val clips = mutableListOf<FakeClip>()

        override fun create(frequencyHz: Int): ToneClip {
            frequencies += frequencyHz
            return FakeClip().also(clips::add)
        }
    }

    private class FakeClip : ToneClip {
        var started = false
        var closed = false
        override fun startLooping() { started = true }
        override fun close() { closed = true }
    }
}
