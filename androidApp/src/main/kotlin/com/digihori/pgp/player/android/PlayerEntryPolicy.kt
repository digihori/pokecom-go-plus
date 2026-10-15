package com.digihori.pgp.player.android

import com.digihori.pgp.player.PlayerRunState

/** Only home navigation carries execution intent; backgrounding never requests a restart. */
internal class PlayerEntryPolicy {
    var shouldRun: Boolean = true
        private set

    fun newSession() { shouldRun = true }

    fun leavingPlayer(state: PlayerRunState, startPending: Boolean) {
        shouldRun = state == PlayerRunState.RUNNING ||
            (state == PlayerRunState.PAUSED && startPending)
    }
}
