package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.math.max
import kotlin.math.min

internal class Pc1245Buzzer {
    var frequencyHz: Int = 0
        private set
    var revision: Long = 0
        private set

    private var mode: Int = MODE_LOW
    private var elapsedCycles: Long = 0
    private var renderedCycles: Double = 0.0
    private var renderedMode: Int = MODE_LOW
    private var previousInput: Double = -1.0
    private var filteredOutput: Double = 0.0
    private val events = mutableListOf<ModeEvent>()

    fun writeControl(value: Int) {
        val requestedMode = (value ushr 4) and MODE_MASK
        if (requestedMode == mode) return
        mode = requestedMode
        frequencyHz = when (mode) {
            MODE_2_KHZ -> 2_000
            MODE_4_KHZ -> 4_000
            else -> 0
        }
        events += ModeEvent(elapsedCycles.toDouble(), mode)
        revision++
    }

    fun advanceCycles(cycles: Int) {
        require(cycles >= 0) { "Cycle count must not be negative" }
        elapsedCycles += cycles
    }

    fun drainPcm(): ShortArray {
        val sampleCount = ((elapsedCycles - renderedCycles) / CYCLES_PER_SAMPLE).toInt()
        if (sampleCount <= 0) return ShortArray(0)
        val output = ShortArray(sampleCount)
        var eventIndex = 0
        var activeMode = renderedMode
        var inputBefore = previousInput
        var filtered = filteredOutput

        repeat(sampleCount) { sampleIndex ->
            val start = renderedCycles + sampleIndex * CYCLES_PER_SAMPLE
            val end = start + CYCLES_PER_SAMPLE
            var cursor = start
            var weighted = 0.0
            while (eventIndex < events.size && events[eventIndex].cycle < end) {
                val event = events[eventIndex]
                val eventCycle = max(cursor, event.cycle)
                if (eventCycle > cursor) {
                    weighted += meanOutput(activeMode, cursor, eventCycle) * (eventCycle - cursor)
                }
                cursor = eventCycle
                activeMode = event.mode
                eventIndex++
            }
            if (end > cursor) weighted += meanOutput(activeMode, cursor, end) * (end - cursor)
            val input = weighted / CYCLES_PER_SAMPLE
            filtered = HIGH_PASS_FACTOR * (filtered + input - inputBefore)
            inputBefore = input
            output[sampleIndex] = (max(-1.0, min(1.0, filtered)) * AMPLITUDE).toInt().toShort()
        }

        renderedCycles += sampleCount * CYCLES_PER_SAMPLE
        renderedMode = activeMode
        previousInput = inputBefore
        filteredOutput = filtered
        if (eventIndex > 0) events.subList(0, eventIndex).clear()
        return if (output.any { it.toInt() != 0 }) output else ShortArray(0)
    }

    fun reset() {
        if (mode != MODE_LOW) revision++
        mode = MODE_LOW
        frequencyHz = 0
        elapsedCycles = 0
        renderedCycles = 0.0
        renderedMode = MODE_LOW
        previousInput = -1.0
        filteredOutput = 0.0
        events.clear()
    }

    private fun meanOutput(activeMode: Int, start: Double, end: Double): Double {
        if (activeMode == MODE_HIGH_1 || activeMode == MODE_HIGH_5) return 1.0
        val halfPeriod = when (activeMode) {
            MODE_2_KHZ -> CLOCK_HZ / 2_000.0 / 2.0
            MODE_4_KHZ -> CLOCK_HZ / 4_000.0 / 2.0
            else -> return -1.0
        }
        var sum = 0.0
        val step = (end - start) / OSCILLATOR_SUBSAMPLES
        repeat(OSCILLATOR_SUBSAMPLES) { index ->
            val cycle = start + (index + 0.5) * step
            sum += if ((cycle / halfPeriod).toLong() % 2L == 0L) 1.0 else -1.0
        }
        return sum / OSCILLATOR_SUBSAMPLES
    }

    private data class ModeEvent(val cycle: Double, val mode: Int)

    companion object {
        const val SAMPLE_RATE: Int = 22_050
        private const val CLOCK_HZ: Double = 192_000.0
        private const val CYCLES_PER_SAMPLE: Double = CLOCK_HZ / SAMPLE_RATE
        private const val AMPLITUDE: Double = 9_000.0
        private const val HIGH_PASS_FACTOR: Double = 0.985
        private const val OSCILLATOR_SUBSAMPLES: Int = 4
        private const val MODE_MASK: Int = 0x07
        private const val MODE_LOW: Int = 0
        private const val MODE_HIGH_1: Int = 1
        private const val MODE_2_KHZ: Int = 2
        private const val MODE_4_KHZ: Int = 3
        private const val MODE_HIGH_5: Int = 5
    }
}
