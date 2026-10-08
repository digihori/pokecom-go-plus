package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WavCodecModelTest {
    @Test
    fun identifiesOldPayloadTypesByRawValue() {
        assertEquals(OldPayloadType.BASIC, OldPayloadType.fromRawValue(0x02))
        assertEquals(OldPayloadType.PASSWORD_PROTECTED_BASIC, OldPayloadType.fromRawValue(0x12))
        assertEquals(OldPayloadType.BINARY, OldPayloadType.fromRawValue(0x62))
        assertNull(OldPayloadType.fromRawValue(0xff))
    }

    @Test
    fun validatesAndQueriesCodecRanges() {
        val range = CodecDataRange(CodecDataKind.RAW_BYTES, offset = 4, length = 3)

        assertEquals(7, range.endExclusive)
        assertTrue(4 in range)
        assertTrue(6 in range)
        assertFalse(7 in range)
        assertFailsWith<IllegalArgumentException> {
            CodecDataRange(CodecDataKind.RAW_BYTES, offset = -1, length = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            CodecDataRange(CodecDataKind.RAW_BYTES, offset = Int.MAX_VALUE, length = 1)
        }
    }

    @Test
    fun byteAndSampleSequencesOwnTheirContent() {
        val logicalSource = byteArrayOf(0x01, 0x02)
        val rawSource = byteArrayOf(0x80.toByte(), 0x7f)
        val pcmSource = shortArrayOf(-100, 100)
        val logical = LogicalByteSequence(logicalSource)
        val raw = RawTransferByteSequence(rawSource)
        val pcm = PcmSampleSequence(pcmSource)

        logicalSource[0] = 0x7f
        rawSource[0] = 0
        pcmSource[0] = 0

        assertContentEquals(byteArrayOf(0x01, 0x02), logical.copyBytes())
        assertEquals(0x80, raw.unsignedByteAt(0))
        assertContentEquals(shortArrayOf(-100, 100), pcm.copySamples())

        logical.copyBytes()[0] = 0
        raw.copyBytes()[0] = 0
        pcm.copySamples()[0] = 0
        assertContentEquals(byteArrayOf(0x01, 0x02), logical.copyBytes())
        assertEquals(0x80, raw.unsignedByteAt(0))
        assertContentEquals(shortArrayOf(-100, 100), pcm.copySamples())
    }

    @Test
    fun validatesOldPayloadTypeByte() {
        val raw = RawTransferByteSequence(byteArrayOf(0x02, 0x00))
        val payload = OldTransferPayload(OldPayloadType.BASIC, raw)

        assertContentEquals(byteArrayOf(0x02, 0x00), payload.copyRawBytes())
        assertFailsWith<IllegalArgumentException> {
            OldTransferPayload(OldPayloadType.BINARY, raw)
        }
    }

    @Test
    fun enforcesDiagnosticResultInvariantsAndCopiesCollections() {
        val detailSource = mutableMapOf("expected" to "F5")
        val warning = CodecDiagnostic(
            severity = CodecDiagnosticSeverity.WARNING,
            stage = CodecStage.TRANSFER_DECODE,
            code = CodecDiagnosticCode.TRAILING_DATA,
            message = "Trailing data remains",
            range = CodecDataRange(CodecDataKind.RAW_BYTES, 12, 2),
            details = detailSource,
        )
        detailSource["expected"] = "changed"
        assertEquals("F5", warning.details["expected"])

        val diagnosticSource = mutableListOf(warning)
        val success = CodecResult.Success("decoded", diagnosticSource)
        diagnosticSource.clear()
        assertEquals(1, success.diagnostics.size)

        val error = CodecDiagnostic(
            severity = CodecDiagnosticSeverity.ERROR,
            stage = CodecStage.WAV_CONTAINER,
            code = CodecDiagnosticCode.INVALID_HEADER,
            message = "RIFF header is missing",
        )
        assertFailsWith<IllegalArgumentException> { CodecResult.Success("invalid", listOf(error)) }
        assertFailsWith<IllegalArgumentException> { CodecResult.Failure(listOf(warning)) }
        assertEquals(1, CodecResult.Failure(listOf(error)).diagnostics.size)
    }

    @Test
    fun validatesWavPcmFormat() {
        assertEquals(
            WavPcmFormat(sampleRate = 8000, channelCount = 1, bitsPerSample = 8),
            WavPcmFormat(sampleRate = 8000, channelCount = 1, bitsPerSample = 8),
        )
        assertFailsWith<IllegalArgumentException> {
            WavPcmFormat(sampleRate = 0, channelCount = 1, bitsPerSample = 8)
        }
    }
}
