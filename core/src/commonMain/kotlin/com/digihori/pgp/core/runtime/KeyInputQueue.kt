package com.digihori.pgp.core.runtime

import com.digihori.pgp.core.api.PocketKey

/** Schedules host-generated key taps against emulated cycles, not wall-clock time. */
public class KeyInputQueue(
    private val holdCycles: Long,
    private val gapCycles: Long,
) {
    private val pendingKeys: ArrayDeque<PocketKey> = ArrayDeque()
    private var activeKey: PocketKey? = null
    private var phase: Phase? = null
    private var remainingCycles: Long = 0

    init {
        require(holdCycles > 0) { "holdCycles must be positive" }
        require(gapCycles > 0) { "gapCycles must be positive" }
    }

    public val isIdle: Boolean
        get() = activeKey == null && pendingKeys.isEmpty() && phase == null

    public fun enqueue(keys: Iterable<PocketKey>) {
        pendingKeys.addAll(keys)
    }

    /** Presses the first pending key. Call immediately before emulated execution. */
    public fun start(): KeyTransition? {
        if (phase != null || pendingKeys.isEmpty()) return null
        return pressNext()
    }

    /** Limits one run call so a key transition can occur near its intended cycle boundary. */
    public fun limitCycles(requestedCycles: Long): Long {
        require(requestedCycles >= 0) { "requestedCycles must not be negative" }
        return if (phase == null) requestedCycles else minOf(requestedCycles, remainingCycles)
    }

    /**
     * Accounts for cycles actually executed. A CPU instruction may cross the requested
     * boundary, so excess cycles delay the transition rather than advancing the next phase.
     */
    public fun advance(executedCycles: Long): KeyTransition? {
        require(executedCycles >= 0) { "executedCycles must not be negative" }
        val currentPhase = phase ?: return null
        remainingCycles -= executedCycles
        if (remainingCycles > 0) return null

        return when (currentPhase) {
            Phase.HOLD -> {
                val key = checkNotNull(activeKey)
                activeKey = null
                if (pendingKeys.isEmpty()) {
                    phase = null
                    remainingCycles = 0
                } else {
                    phase = Phase.GAP
                    remainingCycles = gapCycles
                }
                KeyTransition.Release(key)
            }
            Phase.GAP -> pressNext()
        }
    }

    /** Clears queued input and releases an automatically held key, if any. */
    public fun cancel(): KeyTransition.Release? {
        pendingKeys.clear()
        val key = activeKey
        activeKey = null
        phase = null
        remainingCycles = 0
        return key?.let(KeyTransition::Release)
    }

    private fun pressNext(): KeyTransition.Press {
        val key = pendingKeys.removeFirst()
        activeKey = key
        phase = Phase.HOLD
        remainingCycles = holdCycles
        return KeyTransition.Press(key)
    }

    private enum class Phase {
        HOLD,
        GAP,
    }
}

public sealed interface KeyTransition {
    public val key: PocketKey

    public data class Press(override val key: PocketKey) : KeyTransition
    public data class Release(override val key: PocketKey) : KeyTransition
}
