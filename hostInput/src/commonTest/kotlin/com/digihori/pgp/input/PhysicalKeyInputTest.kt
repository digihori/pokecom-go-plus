package com.digihori.pgp.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.*

class PhysicalKeyInputTest {
    @Test fun shortChordRetainsHoldAndReleaseGapOnExecutedCycles() {
        val events = mutableListOf<String>()
        val input = PhysicalKeyInput({ _, key -> events += key.name }, { events += "up" }) {
            when (it) { 1 -> PocketKey.SHIFT; 2 -> PocketKey.A; else -> null }
        }
        input.configure(true, setOf(PocketKey.SHIFT, PocketKey.A), 10, 20)
        input.handle(1, 1, true); input.handle(1, 2, true)
        input.handle(1, 1, false); input.handle(1, 2, false)
        input.handle(1, 2, true); input.handle(1, 2, false)
        input.advanceCycles(9)
        assertEquals(listOf("SHIFT", "A"), events)
        input.advanceCycles(1)
        assertEquals(listOf("SHIFT", "A", "up", "up"), events)
        input.advanceCycles(19); assertEquals(4, events.size)
        input.advanceCycles(1); assertEquals("A", events.last())
        input.advanceCycles(10); assertEquals("up", events.last())
    }
    @Test fun focusResetCancelsQueuedKeysAndRejectsRepeatsUntilFreshPress() {
        val events = mutableListOf<String>()
        val input = PhysicalKeyInput({ _, key -> events += key.name }, { events += "up" }) { PocketKey.A }
        input.configure(true, setOf(PocketKey.A), 10, 20)
        input.handle(1, 1, true); input.handle(1, 1, false); input.handle(1, 1, true)
        input.reset()
        input.advanceCycles(1000)
        assertEquals(listOf("A", "up"), events)
        input.handle(1, 1, true, 2); assertEquals(2, events.size)
        input.handle(1, 1, false); input.handle(1, 1, true)
        assertEquals("A", events.last())
        input.reset(1); assertEquals("up", events.last())
    }
}
