package com.digihori.pgp.desktop.wav

import kotlin.math.*

internal object WaveformViewport {
    fun panPosition(start: Double, span: Int, count: Int, pixels: Double, width: Double): Double =
        (start + pixels * span / width.coerceAtLeast(1.0)).coerceIn(0.0, (count - span).coerceAtLeast(0).toDouble())

    fun pan(start: Int, span: Int, count: Int, pixels: Double, width: Double): Int =
        panPosition(start.toDouble(), span, count, pixels, width).roundToInt()

    fun zoomStart(start: Int, oldSpan: Int, newSpan: Int, count: Int): Int =
        (start.toLong() + (oldSpan - newSpan) / 2).coerceIn(0, (count - newSpan).coerceAtLeast(0).toLong()).toInt()

    fun tickStep(seconds: Double): Double {
        val rough = (seconds / 6).coerceAtLeast(1e-9)
        val power = 10.0.pow(floor(log10(rough)))
        val ratio = rough / power
        return power * when { ratio <= 1 -> 1; ratio <= 2 -> 2; ratio <= 5 -> 5; else -> 10 }
    }
}
