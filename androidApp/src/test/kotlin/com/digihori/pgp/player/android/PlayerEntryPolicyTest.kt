package com.digihori.pgp.player.android

import com.digihori.pgp.player.PlayerRunState
import kotlin.test.*

class PlayerEntryPolicyTest {
    @Test fun firstEntryAndNewSessionRunAutomatically() {
        val policy = PlayerEntryPolicy()
        assertTrue(policy.shouldRun)
        policy.leavingPlayer(PlayerRunState.PAUSED, false)
        assertFalse(policy.shouldRun)
        policy.newSession()
        assertTrue(policy.shouldRun)
    }

    @Test fun homeRoundTripPreservesRunOrPauseIntent() {
        val policy = PlayerEntryPolicy()
        policy.leavingPlayer(PlayerRunState.RUNNING, false)
        assertTrue(policy.shouldRun)
        policy.leavingPlayer(PlayerRunState.PAUSED, false)
        assertFalse(policy.shouldRun)
        policy.leavingPlayer(PlayerRunState.PAUSED, true)
        assertTrue(policy.shouldRun)
        policy.leavingPlayer(PlayerRunState.FAULTED, true)
        assertFalse(policy.shouldRun)
    }
}
