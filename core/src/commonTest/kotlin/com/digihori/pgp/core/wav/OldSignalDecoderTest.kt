package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OldSignalDecoderTest {
    @Test
    fun decodesEncoderOutputWithoutFixedLeadingSkip() {
        val raw = bytes(0x02, 0x00, 0xa5, 0xff)
        val signal = encode(raw)

        val decoded = decode(8000, signal.samples)

        assertContentEquals(raw, decoded.value.rawBytes.copyBytes())
        assertEquals(raw.size, decoded.value.rawByteMappings.size)
        assertEquals(raw.size * 2, decoded.value.syncPoints.size)
        assertTrue(decoded.value.syncPoints.first().sampleOffset >= OldSignalEncoder.LEADER_SAMPLES)
        assertEquals(SignalNibble.LOW, decoded.value.syncPoints[0].nibble)
        assertEquals(SignalNibble.HIGH, decoded.value.syncPoints[1].nibble)
    }

    @Test
    fun decodesDoubledSamplesAtSixteenKilohertz() {
        val raw = bytes(0x62, 0x10, 0x7f, 0x80)
        val source = encode(raw).samples.copySamples()
        val doubled = ShortArray(source.size * 2) { source[it / 2] }

        val decoded = decode(16000, PcmSampleSequence(doubled))

        assertContentEquals(raw, decoded.value.rawBytes.copyBytes())
        assertEquals(16000, decoded.value.sampleRate)
    }

    @Test
    fun toleratesLeadingSilenceAndReducedAmplitude() {
        val raw = bytes(0x02, 0x12, 0x62)
        val source = encode(raw).samples.copySamples()
        val samples = ShortArray(800 + source.size)
        source.forEachIndexed { index, sample -> samples[800 + index] = (sample / 2).toShort() }

        val decoded = decode(8000, PcmSampleSequence(samples))

        assertContentEquals(raw, decoded.value.rawBytes.copyBytes())
    }

    @Test
    fun returnsCompletedBytesAndWarnsAboutTruncatedFinalFrame() {
        val raw = bytes(0x02, 0xa5)
        val complete = encode(raw).samples.copySamples()
        val truncated = complete.copyOf(complete.size - 100)

        val decoded = decode(8000, PcmSampleSequence(truncated))

        assertContentEquals(bytes(0x02), decoded.value.rawBytes.copyBytes())
        assertEquals(CodecDiagnosticCode.SYNCHRONIZATION_LOST, decoded.diagnostics.single().code)
    }

    @Test
    fun rejectsUnsupportedRateAndSignalWithoutCompleteByte() {
        val unsupported = OldSignalDecoder.decode(2000, PcmSampleSequence(ShortArray(100)))
        assertEquals(
            CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            assertIs<CodecResult.Failure>(unsupported).diagnostics.single().code,
        )

        val silence = OldSignalDecoder.decode(8000, PcmSampleSequence(ShortArray(2000)))
        assertEquals(
            CodecDiagnosticCode.SYNCHRONIZATION_LOST,
            assertIs<CodecResult.Failure>(silence).diagnostics.single().code,
        )
    }

    @Test
    fun decodesFullWavPipelineToValidatedOldPayload() {
        val body = LogicalByteSequence(bytes(0xe0, 0x10, 0xd4, 0x00, 0xf0))
        val payload = assertIs<CodecResult.Success<OldPayloadEncoding>>(
            OldPayloadEncoder.encodeBasic("TEST", body),
        ).value.payload
        val signal = assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(payload)).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.writeOldSignal(signal)).value
        val parsed = assertIs<CodecResult.Success<WavPcmData>>(PcmWavReader.read(wav.bytes)).value
        val normalized = assertIs<CodecResult.Success<NormalizedPcmData>>(PcmNormalizer.normalize(parsed)).value
        val raw = assertIs<CodecResult.Success<OldSignalDecoding>>(OldSignalDecoder.decode(normalized)).value.rawBytes
        val decoded = assertIs<CodecResult.Success<OldPayloadDecoding>>(OldPayloadDecoder.decode(raw)).value

        assertEquals("TEST", decoded.filename)
        assertContentEquals(body.copyBytes(), decoded.body.copyBytes())
    }

    private fun encode(raw: ByteArray): OldSignalEncoding =
        assertIs<CodecResult.Success<OldSignalEncoding>>(
            OldSignalEncoder.encode(RawTransferByteSequence(raw)),
        ).value

    private fun decode(
        sampleRate: Int,
        samples: PcmSampleSequence,
    ): CodecResult.Success<OldSignalDecoding> =
        assertIs(OldSignalDecoder.decode(sampleRate, samples))

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
