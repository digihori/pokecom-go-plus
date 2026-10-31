package com.digihori.pgp.core.wav

import kotlin.test.*
import kotlin.math.PI
import kotlin.math.sin

class HighRateSignalTest {
    private val raw = RawTransferByteSequence(ByteArray(256) { it.toByte() })

    // Model a continuous tape waveform independently of the decoder's resampling algorithm.
    private fun recording(source: PcmSampleSequence, rate: Int, phase: Double = 0.0, durationScale: Double = 1.0): PcmSampleSequence {
        val original = source.copySamples()
        val length = (original.size.toDouble() * rate / 8000 * durationScale).toInt()
        return PcmSampleSequence(ShortArray(length + rate / 50) { i ->
            if (i >= length) return@ShortArray 0
            val position = i * 8000.0 / rate / durationScale
            val unit = minOf(position.toInt() / 16 * 16, original.size - 16)
            val frequency = if (original[unit] != original[unit + 1]) 4000 else 2000
            (24000 * sin(2 * PI * frequency * (position - unit) / 8000 + phase)).toInt().toShort()
        })
    }

    @Test fun decodesAllBytesAtBothRatesAndKeepsOriginalCoordinates() {
        for (rate in listOf(44100, 48000)) for (phase in listOf(0.0, 0.37)) {
            val oldPcm = assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(raw)).value.samples
            val s1Pcm = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(raw)).value.samples
            val oldInput = recording(oldPcm, rate, phase)
            val s1Input = recording(s1Pcm, rate, phase)
            val oldResult = OldSignalDecoder.decode(rate, oldInput)
            val old = assertIs<CodecResult.Success<OldSignalDecoding>>(oldResult, "$rate: ${oldResult.diagnostics.joinToString { it.message }}")
            val s1Result = S1SignalDecoder.decode(rate, s1Input)
            val s1 = assertIs<CodecResult.Success<S1SignalDecoding>>(s1Result, "$rate: ${s1Result.diagnostics.joinToString { it.message }}")
            assertContentEquals(raw.copyBytes(), old.value.rawBytes.copyBytes(), "OLD $rate $phase")
            assertContentEquals(raw.copyBytes(), s1.value.rawBytes.copyBytes(), "S1 $rate $phase")
            assertTrue(old.diagnostics.isEmpty(), "OLD $rate $phase: ${old.diagnostics.joinToString { it.message + it.range }}")
            assertTrue(s1.diagnostics.isEmpty(), "S1 $rate $phase: ${s1.diagnostics.joinToString { it.message + it.range }}")
            assertEquals(rate, old.value.sampleRate)
            assertEquals(rate, s1.value.sampleRate)
            assertEquals(oldInput.size, old.value.inputSampleCount)
            assertEquals(s1Input.size, s1.value.inputSampleCount)
            for ((mapping, size) in listOf(old.value.rawByteMappings to oldInput.size, s1.value.rawByteMappings to s1Input.size)) {
                assertEquals(256, mapping.size)
                assertTrue(mapping.all { it.source.kind == CodecDataKind.PCM_SAMPLES && it.source.endExclusive <= size })
                assertTrue(mapping.zipWithNext().all { (a, b) -> a.source.offset < b.source.offset })
            }
            assertTrue(kotlin.math.abs(s1.value.syncPoints.first().sampleOffset - 4.096 * rate) < rate / 1000)
        }
    }

    @Test fun squareRecordingsDoNotLoseLeaderToSamplingQuantization() {
        for (rate in listOf(44100, 48000)) {
            val oldSource = assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(raw)).value.samples.copySamples()
            val s1Source = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(raw)).value.samples.copySamples()
            fun square(source: ShortArray): PcmSampleSequence {
                val length = (source.size.toLong() * rate / 8000).toInt()
                return PcmSampleSequence(ShortArray(length + rate / 50) { i ->
                    if (i < length) source[(i.toLong() * 8000 / rate).toInt()] else 0
                })
            }
            val oldResult = OldSignalDecoder.decode(rate, square(oldSource))
            val s1Result = S1SignalDecoder.decode(rate, square(s1Source))
            val old = assertIs<CodecResult.Success<OldSignalDecoding>>(oldResult, oldResult.diagnostics.joinToString { it.message })
            val s1 = assertIs<CodecResult.Success<S1SignalDecoding>>(s1Result, s1Result.diagnostics.joinToString { it.message })
            assertContentEquals(raw.copyBytes(), old.value.rawBytes.copyBytes(), "OLD square $rate")
            assertContentEquals(raw.copyBytes(), s1.value.rawBytes.copyBytes(), "S1 square $rate")
            assertTrue(old.diagnostics.isEmpty(), old.diagnostics.joinToString { it.message })
            assertTrue(s1.diagnostics.isEmpty(), s1.diagnostics.joinToString { it.message })
        }
    }

    @Test fun tracksCompactHardwareFramesAndDcSettlingTail() {
        val encoded = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(raw)).value.samples.copySamples()
        val compact = buildList<Short> {
            addAll(encoded.take(S1SignalEncoder.LEADER_SAMPLES))
            repeat(raw.size) { index ->
                val units = if (index < 10) 16 else if (index == raw.size - 1) 15 else 13
                val offset = S1SignalEncoder.LEADER_SAMPLES + index * S1SignalEncoder.SAMPLES_PER_BYTE
                addAll(encoded.slice(offset until offset + units * 16))
            }
        }.toShortArray()
        for (rate in listOf(16000, 44100, 48000)) for (scale in listOf(1.0, 0.998)) {
            val clean = recording(PcmSampleSequence(compact), rate, 0.37, scale).copySamples()
            val signalEnd = clean.size - rate / 50
            val pcm = clean.copyOf(signalEnd + rate / 5)
            for (i in signalEnd until pcm.size) {
                pcm[i] = (18000 * kotlin.math.exp(-(i - signalEnd).toDouble() / (rate * 0.006))).toInt().toShort()
            }
            val result = S1SignalDecoder.decode(rate, PcmSampleSequence(pcm))
            val decoded = assertIs<CodecResult.Success<S1SignalDecoding>>(result, "$rate/$scale: ${result.diagnostics.joinToString { it.message }}")
            assertContentEquals(raw.copyBytes(), decoded.value.rawBytes.copyBytes(), "$rate/$scale")
            assertTrue(decoded.diagnostics.isEmpty(), decoded.diagnostics.joinToString { it.message })
        }
    }

    @Test fun wavReaderAndOldPayloadWorkAtBothRates() {
        val body = LogicalByteSequence(ByteArray(81) { it.toByte() })
        val raw = assertIs<CodecResult.Success<OldPayloadEncoding>>(OldPayloadEncoder.encodeBinary("TEST", 0x4000, body)).value.payload
        val signal = assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(RawTransferByteSequence(raw.copyRawBytes()))).value
        for (rate in listOf(44100, 48000)) {
            val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(rate, recording(signal.samples, rate))).value
            val decoded = assertIs<CodecResult.Success<OldWavDecoding>>(OldWavCodec.decode(wav.bytes))
            assertContentEquals(body.copyBytes(), decoded.value.payload.body.copyBytes())
        }
    }

    @Test fun failuresAndTruncationUseOriginalSampleCoordinates() {
        for (rate in listOf(44100, 48000)) {
            for (decode in listOf<(Int, PcmSampleSequence) -> CodecResult<*>>(OldSignalDecoder::decode, S1SignalDecoder::decode)) {
                val result = assertIs<CodecResult.Failure>(decode(rate, PcmSampleSequence(ShortArray(rate))))
                assertTrue(result.diagnostics.all { it.range?.endExclusive?.let { end -> end <= rate } ?: true })
            }
            val source = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(byteArrayOf(1, 2)))).value.samples
            val pcm = recording(source, rate).copySamples().dropLast(rate * 3 / 100).toShortArray()
            val result = assertIs<CodecResult.Success<S1SignalDecoding>>(S1SignalDecoder.decode(rate, PcmSampleSequence(pcm)))
            assertContentEquals(byteArrayOf(1), result.value.rawBytes.copyBytes())
            val diagnostic = result.diagnostics.single()
            assertEquals(CodecDiagnosticCode.TRUNCATED_INPUT, diagnostic.code)
            val range = assertNotNull(diagnostic.range)
            assertTrue(range.offset > rate * 4)
            assertTrue(range.endExclusive <= pcm.size)
        }
    }
}
