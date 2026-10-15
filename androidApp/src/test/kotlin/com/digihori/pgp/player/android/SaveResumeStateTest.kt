package com.digihori.pgp.player.android

import kotlin.test.*

class SaveResumeStateTest {
    @Test fun runningSaveResumesOnlySameSessionOnce() {
        val state = SaveResumeState<Any>()
        val session = Any()
        state.begin(session, true)
        assertTrue(state.shouldResume)
        assertTrue(state.takeFor(session))
        assertFalse(state.takeFor(session))
        state.begin(session, true)
        assertFalse(state.takeFor(Any()))
    }
    @Test fun explicitPauseAndMissingSessionNeverResume() {
        val state = SaveResumeState<Any>()
        val session = Any()
        state.begin(session, false)
        assertFalse(state.shouldResume)
        assertFalse(state.takeFor(session))
        state.begin(session, true)
        assertFalse(state.takeFor(null))
    }
}
