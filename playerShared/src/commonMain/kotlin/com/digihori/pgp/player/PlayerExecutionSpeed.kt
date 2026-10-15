package com.digihori.pgp.player

import com.digihori.pgp.core.runtime.SpeedRatio

/** Stable persisted IDs; independent of Studio's internal enum and platform clocks. */
public enum class PlayerExecutionSpeed(
    public val id: String,
    public val numerator: Int,
    public val denominator: Int,
) {
    SLOWEST("0.8", 4, 5),
    SLOW("0.9", 9, 10),
    NORMAL("1.0", 1, 1),
    FAST("1.1", 11, 10),
    FASTEST("1.2", 6, 5);

    public val ratio: SpeedRatio get() = SpeedRatio(numerator.toLong(), denominator.toLong())
    public fun playbackRate(sampleRate: Int): Int = sampleRate * numerator / denominator

    public companion object {
        public fun fromId(id: String?): PlayerExecutionSpeed = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
