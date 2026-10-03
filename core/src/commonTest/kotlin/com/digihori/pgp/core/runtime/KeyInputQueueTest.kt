package com.digihori.pgp.core.runtime

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyInputQueueTest {
    @Test
    fun schedulesSequentialTapsUsingExecutedCycles() {
        val queue = KeyInputQueue(holdCycles = 10, gapCycles = 5)
        queue.enqueue(listOf(PocketKey.SHIFT, PocketKey.Q))

        assertEquals(KeyTransition.Press(PocketKey.SHIFT), queue.start())
        assertEquals(10, queue.limitCycles(100))
        assertNull(queue.advance(9))
        assertEquals(KeyTransition.Release(PocketKey.SHIFT), queue.advance(1))
        assertEquals(5, queue.limitCycles(100))
        assertEquals(KeyTransition.Press(PocketKey.Q), queue.advance(5))
        assertEquals(KeyTransition.Release(PocketKey.Q), queue.advance(10))
        assertTrue(queue.isIdle)
    }

    @Test
    fun instructionOvershootDoesNotConsumeTheFollowingGap() {
        val queue = KeyInputQueue(holdCycles = 10, gapCycles = 5)
        queue.enqueue(listOf(PocketKey.A, PocketKey.B))
        queue.start()

        assertEquals(KeyTransition.Release(PocketKey.A), queue.advance(12))
        assertEquals(5, queue.limitCycles(100))
    }

    @Test
    fun cancelReleasesOnlyAnActiveKeyAndClearsPendingInput() {
        val queue = KeyInputQueue(holdCycles = 10, gapCycles = 5)
        queue.enqueue(listOf(PocketKey.SHIFT, PocketKey.Q))
        queue.start()

        assertEquals(KeyTransition.Release(PocketKey.SHIFT), queue.cancel())
        assertTrue(queue.isIdle)
        assertNull(queue.start())
        assertNull(queue.cancel())
    }

    @Test
    fun queuedInputRemainsIdleUntilExplicitlyStarted() {
        val queue = KeyInputQueue(holdCycles = 10, gapCycles = 5)
        queue.enqueue(listOf(PocketKey.A))

        assertFalse(queue.isIdle)
        assertEquals(100, queue.limitCycles(100))
    }
}
