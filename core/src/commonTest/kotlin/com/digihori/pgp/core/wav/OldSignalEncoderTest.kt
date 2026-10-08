package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OldSignalEncoderTest {
    @Test
    fun writesReferenceLeader() {
        val encoded = encode(raw(0x02))
        val samples = encoded.samples.copySamples()

        assertEquals(8000, encoded.sampleRate)
        assertContentEquals(
            oneSignalUnit(),
            samples.copyOfRange(0, OldSignalEncoder.SAMPLES_PER_SIGNAL_UNIT),
        )
        assertContentEquals(
            oneSignalUnit(),
            samples.copyOfRange(
                OldSignalEncoder.LEADER_SAMPLES - OldSignalEncoder.SAMPLES_PER_SIGNAL_UNIT,
                OldSignalEncoder.LEADER_SAMPLES,
            ),
        )
    }

    @Test
    fun framesRawByteLowNibbleFirstLikeReferenceImplementation() {
        val encoded = encode(raw(0xa5))
        val samples = encoded.samples.copySamples()
        val frame = samples.copyOfRange(
            OldSignalEncoder.LEADER_SAMPLES,
            OldSignalEncoder.LEADER_SAMPLES + OldSignalEncoder.SAMPLES_PER_BYTE,
        )
        val expectedUnits = listOf(
            0,
            1, 0, 1, 0,
            1, 1, 1, 1,
            0,
            0, 1, 0, 1,
            1, 1, 1, 1, 1,
        )

        expectedUnits.forEachIndexed { index, unit ->
            val actual = frame.copyOfRange(
                index * OldSignalEncoder.SAMPLES_PER_SIGNAL_UNIT,
                (index + 1) * OldSignalEncoder.SAMPLES_PER_SIGNAL_UNIT,
            )
            assertContentEquals(
                if (unit == 1) oneSignalUnit() else zeroSignalUnit(),
                actual,
                "signal unit $index",
            )
        }
    }

    @Test
    fun usesReferenceSampleCountFormulaAndMappings() {
        val encoded = encode(raw(0x02, 0x10, 0xff))

        assertEquals(
            OldSignalEncoder.LEADER_SAMPLES + 3 * OldSignalEncoder.SAMPLES_PER_BYTE,
            encoded.samples.size,
        )
        assertEquals(3, encoded.rawByteMappings.size)
        encoded.rawByteMappings.forEachIndexed { index, mapping ->
            assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, index, 1), mapping.source)
            assertEquals(
                CodecDataRange(
                    CodecDataKind.PCM_SAMPLES,
                    OldSignalEncoder.LEADER_SAMPLES + index * OldSignalEncoder.SAMPLES_PER_BYTE,
                    OldSignalEncoder.SAMPLES_PER_BYTE,
                ),
                mapping.target,
            )
        }
    }

    @Test
    fun matchesPcwavPcmBytesForKnownRawByte() {
        val samples = encode(raw(0x00)).samples.copySamples()
        val frame = samples.copyOfRange(OldSignalEncoder.LEADER_SAMPLES, samples.size)
        val unsignedPcm = frame.map(::toUnsignedPcmByte).toByteArray()
        val expected = buildList {
            addAll(zeroPcmUnit().asIterable())
            repeat(4) { addAll(zeroPcmUnit().asIterable()) }
            repeat(4) { addAll(onePcmUnit().asIterable()) }
            addAll(zeroPcmUnit().asIterable())
            repeat(4) { addAll(zeroPcmUnit().asIterable()) }
            repeat(5) { addAll(onePcmUnit().asIterable()) }
        }.toByteArray()

        assertEquals(304, unsignedPcm.size)
        assertContentEquals(expected, unsignedPcm)
    }

    private fun encode(raw: RawTransferByteSequence): OldSignalEncoding =
        assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(raw)).value

    private fun raw(vararg values: Int): RawTransferByteSequence =
        RawTransferByteSequence(values.map(Int::toByte).toByteArray())

    private fun oneSignalUnit(): ShortArray = ShortArray(16) { if (it % 2 == 0) 32512 else -32768 }

    private fun zeroSignalUnit(): ShortArray = ShortArray(16) { if (it % 4 < 2) 32512 else -32768 }

    private fun onePcmUnit(): ByteArray = ByteArray(16) { if (it % 2 == 0) 0xff.toByte() else 0x00 }

    private fun zeroPcmUnit(): ByteArray = ByteArray(16) { if (it % 4 < 2) 0xff.toByte() else 0x00 }

    private fun toUnsignedPcmByte(sample: Short): Byte = ((sample.toInt() shr 8) + 128).toByte()
}
