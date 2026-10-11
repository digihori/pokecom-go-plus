package com.digihori.pgp.core.wav

import kotlin.test.*

class OldSignalInspectionTest {
    @Test fun inspectionUsesDecoderTapAndPreservesSignedDifferenceBeyondPcmRange() {
        val samples = PcmSampleSequence(shortArrayOf(0, 0, 32767, -32768, 32767, -32768))
        val trace = OldSignalDecoder.inspect(8000, samples)
        assertEquals(2, trace.signalStart)
        assertEquals(2, trace.tapSamples)
        assertEquals(5120, trace.threshold)
        assertContentEquals(intArrayOf(0, 0, 32767, -32768, 0, 0), trace.copyDifferences())
        val edges = OldSignalDecoder.inspect(4000, samples)
        assertContentEquals(intArrayOf(0, 0, 32767, -65535, 65535, -65535), edges.copyDifferences())
        val copy = edges.copyDifferences()
        copy[3] = 0
        assertEquals(-65535, edges.copyDifferences()[3])
    }

    @Test fun silenceDoesNotInventThresholdCrossings() {
        val trace = OldSignalDecoder.inspect(8000, PcmSampleSequence(ShortArray(20)))
        assertEquals(20, trace.signalStart)
        assertTrue(trace.copyDifferences().all { it == 0 })
    }
}
