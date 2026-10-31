package com.digihori.pgp.desktop.wav

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.*

class DesktopHighRateWavTest {
    /** Independent continuous 2/4 kHz recording, packed as signed 16-bit PCM WAV. */
    private fun recording(source: WavByteSequence, rate: Int, channels: Int): WavByteSequence {
        val parsed = assertIs<CodecResult.Success<WavPcmData>>(PcmWavReader.read(source)).value
        val original = assertIs<CodecResult.Success<NormalizedPcmData>>(PcmNormalizer.normalize(parsed)).value.samples.copySamples()
        val signalLength = (original.size.toLong() * rate / 8000).toInt()
        val samples = signalLength + rate / 50
        val dataSize = samples * channels * 2
        val wav = ByteArray(44 + dataSize)
        fun text(offset: Int, value: String) { value.encodeToByteArray().copyInto(wav, offset) }
        fun number(offset: Int, value: Int, size: Int) { repeat(size) { wav[offset + it] = (value ushr (8 * it)).toByte() } }
        text(0, "RIFF"); number(4, wav.size - 8, 4); text(8, "WAVEfmt ")
        number(16, 16, 4); number(20, 1, 2); number(22, channels, 2); number(24, rate, 4)
        number(28, rate * channels * 2, 4); number(32, channels * 2, 2); number(34, 16, 2)
        text(36, "data"); number(40, dataSize, 4)
        repeat(signalLength) { i ->
            val position = i * 8000.0 / rate
            val unit = minOf(position.toInt() / 16 * 16, original.size - 16)
            val frequency = if (original[unit] != original[unit + 1]) 4000 else 2000
            val value = (24000 * sin(2 * PI * frequency * (position - unit) / 8000 + 0.37)).toInt()
            repeat(channels) { channel -> number(44 + (i * channels + channel) * 2, value, 2) }
        }
        return WavByteSequence(wav)
    }

    @Test fun allDialectsAndBasicTypesDecodeHighRateRecordingsWithoutChangingOutputRate() {
        for (target in listOf("pc-1245", "pc-1350", "pc-1360")) for (protected in listOf(false, true)) {
            val machine = MachineId(target)
            val encoded = DesktopWavOperation.encode("10 PRINT \"TEST\"\n20 END\n".encodeToByteArray(), false, machine, "TEST", protected)
            val source = assertNotNull(encoded.wav, encoded.error)
            assertEquals(8000, assertIs<CodecResult.Success<WavPcmData>>(PcmWavReader.read(source)).value.format.sampleRate)
            val baseline = DesktopWavOperation.decode(source, machine)
            for (rate in listOf(44100, 48000)) {
                val decoded = DesktopWavOperation.decode(recording(source, rate, if (rate == 44100) 1 else 2), machine)
                assertNull(decoded.error, "$target/$protected/$rate: ${decoded.report()}")
                assertContentEquals(assertNotNull(baseline.program), assertNotNull(decoded.program, decoded.report()))
                assertContentEquals(assertNotNull(baseline.raw).copyBytes(), assertNotNull(decoded.raw).copyBytes())
                assertEquals(rate, assertNotNull(decoded.pcm).sampleRate)
            }
        }
    }

    @Test fun binaryAndChecksumFailuresKeepRawButBlockProgramExport() {
        for (target in listOf("pc-1245", "pc-1350", "pc-1360")) {
            val machine = MachineId(target)
            val encoded = DesktopWavOperation.encode("4000: 01 02 03 FF\n".encodeToByteArray(), true, machine, "CHECK", false)
            val source = assertNotNull(encoded.wav, encoded.error)
            val baseline = DesktopWavOperation.decode(source, machine)
            for (rate in listOf(44100, 48000)) {
                val decoded = DesktopWavOperation.decode(recording(source, rate, 1), machine)
                assertNull(decoded.error, decoded.report())
                assertContentEquals(baseline.program, decoded.program, "$target/$rate: ${decoded.report()}")
                val raw = assertNotNull(baseline.raw).copyBytes().also { it[9] = (it[9].toInt() xor 1).toByte() }
                val pcm = if (target == "pc-1245") assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(RawTransferByteSequence(raw))).value.samples
                    else assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(raw))).value.samples
                val damaged = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, pcm)).value.bytes
                val failure = DesktopWavOperation.decode(recording(damaged, rate, 1), machine)
                assertNull(failure.program)
                assertContentEquals(raw, assertNotNull(failure.raw).copyBytes())
                assertTrue(failure.diagnostics.any { it.code == CodecDiagnosticCode.CHECKSUM_MISMATCH && it.range?.offset == 9 }, failure.report())
                assertNotNull(failure.pcm)
            }
        }
    }
}
