package com.digihori.pgp.player.android

/** A save may resume only the same session, once; explicit pause remains paused. */
internal class SaveResumeState<T : Any> {
    private var source: T? = null
    var shouldResume: Boolean = false
        private set
    fun begin(session: T, running: Boolean) { source = session; shouldResume = running }
    fun takeFor(session: T?): Boolean {
        val resume = shouldResume && session != null && source === session
        source = null
        shouldResume = false
        return resume
    }
}
