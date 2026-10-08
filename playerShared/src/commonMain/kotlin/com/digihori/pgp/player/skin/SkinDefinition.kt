package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

public data class SkinSize(
    public val width: Float,
    public val height: Float,
) {
    init {
        require(width.isFinite() && width > 0f) { "Skin width must be finite and positive" }
        require(height.isFinite() && height > 0f) { "Skin height must be finite and positive" }
    }
}

public data class SkinInsets(
    public val left: Float = 0f,
    public val top: Float = 0f,
    public val right: Float = 0f,
    public val bottom: Float = 0f,
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it >= 0f }) {
            "Skin insets must be finite and non-negative"
        }
    }
}

public data class SkinRect(
    public val x: Float,
    public val y: Float,
    public val width: Float,
    public val height: Float,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Skin origin must be finite" }
        require(width.isFinite() && width > 0f) { "Skin region width must be finite and positive" }
        require(height.isFinite() && height > 0f) { "Skin region height must be finite and positive" }
    }

    public val right: Float get() = x + width
    public val bottom: Float get() = y + height
}

/** A tappable physical key in the skin's design coordinate system. */
public data class SkinKeyRegion(
    public val key: PocketKey,
    public val bounds: SkinRect,
)

/** Geometry only. Image loading and rendering remain platform responsibilities. */
public data class SkinDefinition(
    public val id: String,
    public val machineIds: Set<MachineId>,
    public val designSize: SkinSize,
    public val fullDeviceRegion: SkinRect,
    public val playableRegion: SkinRect = fullDeviceRegion,
    public val landscapeRegion: SkinRect = playableRegion,
    public val controllerDisplayRegion: SkinRect,
    public val lcdContentRegion: SkinRect,
    public val keyRegions: List<SkinKeyRegion> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Skin ID must not be blank" }
        require(machineIds.isNotEmpty()) { "A skin must support at least one machine" }
        listOf(
            fullDeviceRegion,
            playableRegion,
            landscapeRegion,
            controllerDisplayRegion,
            lcdContentRegion,
        ).forEach { region ->
            require(region.x >= 0f && region.y >= 0f) { "Skin regions must start inside the design canvas" }
            require(region.right <= designSize.width && region.bottom <= designSize.height) {
                "Skin regions must fit inside the design canvas"
            }
        }
        require(keyRegions.map(SkinKeyRegion::key).distinct().size == keyRegions.size) {
            "A skin must define at most one region for each key"
        }
        keyRegions.forEach { keyRegion ->
            require(keyRegion.bounds.x >= 0f && keyRegion.bounds.y >= 0f) {
                "Skin key regions must start inside the design canvas"
            }
            require(
                keyRegion.bounds.right <= designSize.width &&
                    keyRegion.bounds.bottom <= designSize.height,
            ) { "Skin key regions must fit inside the design canvas" }
        }
    }
}
