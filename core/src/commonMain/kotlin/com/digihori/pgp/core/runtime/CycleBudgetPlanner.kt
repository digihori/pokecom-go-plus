package com.digihori.pgp.core.runtime

/**
 * Converts host-observed elapsed time into an emulator cycle budget.
 *
 * This class does not read a clock, sleep, create threads, or execute an emulator session. A host
 * runner owns those responsibilities and supplies elapsed monotonic time to [plan].
 */
public class CycleBudgetPlanner(
    public val cyclesPerSecond: Long,
    public val maximumCatchUpNanoseconds: Long = DEFAULT_MAXIMUM_CATCH_UP_NANOSECONDS,
) {
    private var remainder: Long = 0L
    private var activeSpeed: SpeedRatio = SpeedRatio.NORMAL

    init {
        require(cyclesPerSecond > 0) { "cyclesPerSecond must be greater than zero" }
        require(maximumCatchUpNanoseconds > 0) {
            "maximumCatchUpNanoseconds must be greater than zero"
        }
        require(cyclesPerSecond <= Long.MAX_VALUE / maximumCatchUpNanoseconds / MAX_SPEED_NUMERATOR) {
            "cyclesPerSecond and maximumCatchUpNanoseconds are too large"
        }
    }

    /**
     * Returns the cycles to execute for [elapsedNanoseconds]. Long gaps are capped so a resumed UI
     * does not spend an unbounded amount of time catching up. Fractional cycles carry into the next
     * call. Changing [speed] starts a new fractional interval.
     */
    public fun plan(
        elapsedNanoseconds: Long,
        speed: SpeedRatio = SpeedRatio.NORMAL,
    ): CycleBudget {
        require(elapsedNanoseconds >= 0) { "elapsedNanoseconds must not be negative" }
        if (speed != activeSpeed) {
            remainder = 0L
            activeSpeed = speed
        }

        val acceptedElapsed = minOf(elapsedNanoseconds, maximumCatchUpNanoseconds)
        val divisor = NANOS_PER_SECOND * speed.denominator
        val scaled = acceptedElapsed * cyclesPerSecond * speed.numerator + remainder
        val cycles = scaled / divisor
        remainder = scaled % divisor
        return CycleBudget(
            cycles = cycles,
            acceptedElapsedNanoseconds = acceptedElapsed,
            droppedElapsedNanoseconds = elapsedNanoseconds - acceptedElapsed,
        )
    }

    /** Clears fractional timing state after pause, reset, ROM replacement, or timeline restart. */
    public fun reset() {
        remainder = 0L
        activeSpeed = SpeedRatio.NORMAL
    }

    public companion object {
        public const val PC1245_CYCLES_PER_SECOND: Long = 288_000L
        public const val PC1251_CYCLES_PER_SECOND: Long = 192_000L
        public const val DEFAULT_MAXIMUM_CATCH_UP_NANOSECONDS: Long = 100_000_000L
        private const val NANOS_PER_SECOND: Long = 1_000_000_000L
        private const val MAX_SPEED_NUMERATOR: Long = 16L
    }
}

public data class CycleBudget(
    public val cycles: Long,
    public val acceptedElapsedNanoseconds: Long,
    public val droppedElapsedNanoseconds: Long,
)

public data class SpeedRatio(
    public val numerator: Long,
    public val denominator: Long,
) {
    init {
        require(numerator in 1..16) { "speed numerator must be between 1 and 16" }
        require(denominator in 1..16) { "speed denominator must be between 1 and 16" }
    }

    public companion object {
        public val NORMAL: SpeedRatio = SpeedRatio(1, 1)
        public val DOUBLE: SpeedRatio = SpeedRatio(2, 1)
        public val HALF: SpeedRatio = SpeedRatio(1, 2)
    }
}
