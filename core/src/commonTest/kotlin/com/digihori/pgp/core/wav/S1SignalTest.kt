package com.digihori.pgp.core.wav

import kotlin.test.*

class S1SignalTest {
    private val raw = ByteArray(256) { it.toByte() }
    private fun signal(data: ByteArray = raw): ShortArray = assertIs<CodecResult.Success<S1SignalEncoding>>(
        S1SignalEncoder.encode(RawTransferByteSequence(data))).value.samples.copySamples()
    private fun decode(rate: Int, pcm: ShortArray) = assertIs<CodecResult.Success<S1SignalDecoding>>(
        S1SignalDecoder.decode(rate, PcmSampleSequence(pcm)))

    @Test fun keepsFrameAndNibblePositions() {
        val pcm = signal()
        val decoded = decode(8000, pcm)
        assertContentEquals(raw, decoded.value.rawBytes.copyBytes())
        assertEquals(32768 + 256 * 256, pcm.size)
        assertEquals(256, decoded.value.rawByteMappings.size)
        decoded.value.rawByteMappings.forEachIndexed { i, mapping ->
            assertEquals(CodecDataRange(CodecDataKind.PCM_SAMPLES, 32768 + i * 256, 256), mapping.source)
            assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, i, 1), mapping.target)
            assertEquals(SignalSyncPoint(32768 + i * 256, i, SignalNibble.LOW), decoded.value.syncPoints[i * 2])
            assertEquals(SignalSyncPoint(32864 + i * 256, i, SignalNibble.HIGH), decoded.value.syncPoints[i * 2 + 1])
        }
    }

    @Test fun handlesSilenceAttenuationPolarityAndHigherRates() {
        val source = signal()
        for (factor in 1..4) {
            val prefix = 317
            val pcm = ShortArray(prefix + source.size * factor + 1000)
            source.indices.forEach { i -> repeat(factor) { pcm[prefix + i * factor + it] = (-source[i] / 3).toShort() } }
            val decoded = decode(8000 * factor, pcm)
            assertContentEquals(raw, decoded.value.rawBytes.copyBytes(), "rate factor $factor")
            assertTrue(decoded.diagnostics.isEmpty())
        }
    }

    @Test fun rejectsSilenceDcMissingLeaderAndUnsupportedRate() {
        for (pcm in listOf(ShortArray(5000), ShortArray(5000) { 16000 }, signal().copyOfRange(32768, 33024))) {
            assertIs<CodecResult.Failure>(S1SignalDecoder.decode(8000, PcmSampleSequence(pcm)))
        }
        assertEquals(CodecDiagnosticCode.UNSUPPORTED_FORMAT, assertIs<CodecResult.Failure>(
            S1SignalDecoder.decode(22050, PcmSampleSequence(signal()))).diagnostics.single().code)
    }

    @Test fun truncatedFramesNeverProducePartialBytes() {
        val source = signal(byteArrayOf(0x67, 0x12))
        for (units in listOf(1, 5, 7, 10, 15)) {
            val decoded = decode(8000, source.copyOf(32768 + 256 + units * 16))
            assertContentEquals(byteArrayOf(0x67), decoded.value.rawBytes.copyBytes())
            assertEquals(CodecDiagnosticCode.TRUNCATED_INPUT, decoded.diagnostics.single().code)
        }
    }

    @Test fun diagnosesCarrierDropoutAndRecoversOnlyAfterNewLeader() {
        val source = signal(byteArrayOf(0x67, 0x12, 0x34))
        source.fill(0, 32768 + 256 + 32, 32768 + 256 + 64)
        val decoded = decode(8000, source + signal(byteArrayOf(0x55)))
        assertContentEquals(byteArrayOf(0x67, 0x55), decoded.value.rawBytes.copyBytes())
        assertEquals(CodecDiagnosticCode.SYNCHRONIZATION_LOST, decoded.diagnostics.single().code)
    }

    @Test fun rejectsBrokenNibbleSyncAndStopBits() {
        for (unit in listOf(0, 5, 6, 11, 15)) {
            val source = signal(byteArrayOf(0x67, 0x12, 0x34))
            val start = 32768 + 256 + unit * 16
            // Replace framing zero with one, or stop one with zero.
            repeat(16) { i -> source[start + i] = if (unit == 0 || unit == 6) {
                if (i % 2 == 0) 32512 else -32768
            } else {
                if (i % 4 < 2) 32512 else -32768
            } }
            val decoded = decode(8000, source)
            assertContentEquals(byteArrayOf(0x67), decoded.value.rawBytes.copyBytes(), "unit $unit")
            assertEquals(CodecDiagnosticCode.SYNCHRONIZATION_LOST, decoded.diagnostics.single().code)
        }
    }

    @Test fun recoversAfterLongSilentGapWithNewLeader() {
        val source = signal(byteArrayOf(0x67)) + ShortArray(8000) + signal(byteArrayOf(0x55))
        val decoded = decode(8000, source)
        assertContentEquals(byteArrayOf(0x67, 0x55), decoded.value.rawBytes.copyBytes())
        assertEquals(CodecDiagnosticCode.SYNCHRONIZATION_LOST, decoded.diagnostics.single().code)
    }

    @Test fun usesExistingWavContainerAndNormalization() {
        val encoded = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, PcmSampleSequence(signal())))
        val parsed = assertIs<CodecResult.Success<WavPcmData>>(PcmWavReader.read(encoded.value.bytes))
        val normalized = assertIs<CodecResult.Success<NormalizedPcmData>>(PcmNormalizer.normalize(parsed.value))
        assertContentEquals(raw, assertIs<CodecResult.Success<S1SignalDecoding>>(
            S1SignalDecoder.decode(normalized.value)).value.rawBytes.copyBytes())
    }
}
