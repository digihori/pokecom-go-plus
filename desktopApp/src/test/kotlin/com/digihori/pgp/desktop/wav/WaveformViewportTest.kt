package com.digihori.pgp.desktop.wav

import kotlin.test.*

class WaveformViewportTest {
    @Test fun panningUsesZoomScaleAndClampsToRecording() {
        assertEquals(150, WaveformViewport.pan(100, 200, 1000, 100.0, 400.0))
        assertEquals(0, WaveformViewport.pan(100, 200, 1000, -10000.0, 400.0))
        assertEquals(800, WaveformViewport.pan(100, 200, 1000, 10000.0, 400.0))
        assertEquals(0, WaveformViewport.pan(0, 1000, 1000, 100.0, 400.0))
    }

    @Test fun smallTrackpadDeltasAccumulateAtSampleZoom() {
        var position = 0.0
        repeat(100) { position = WaveformViewport.panPosition(position, 8, 1000, 1.0, 800.0) }
        assertEquals(1.0, position, 1e-9)
    }

    @Test fun zoomKeepsCenterAndClampsAtEdges() {
        assertEquals(350, WaveformViewport.zoomStart(300, 200, 100, 1000))
        assertEquals(0, WaveformViewport.zoomStart(0, 100, 1000, 1000))
        assertEquals(600, WaveformViewport.zoomStart(900, 100, 400, 1000))
    }

    @Test fun ticksStayReadableAtMillisecondAndSecondScales() {
        assertEquals(.002, WaveformViewport.tickStep(.01), 1e-12)
        assertEquals(2.0, WaveformViewport.tickStep(10.0), 1e-12)
        assertTrue(WaveformViewport.tickStep(1e-6) > 0)
    }
}
