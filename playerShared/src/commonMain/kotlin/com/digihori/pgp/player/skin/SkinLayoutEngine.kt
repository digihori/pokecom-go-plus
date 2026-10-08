package com.digihori.pgp.player.skin

import com.digihori.pgp.player.PresentationMode
import kotlin.math.min

public data class SkinLayoutRequest(
    public val availableSize: SkinSize,
    public val safeInsets: SkinInsets = SkinInsets(),
    public val mode: PresentationMode,
) {
    init {
        require(safeInsets.left + safeInsets.right < availableSize.width) {
            "Horizontal safe insets leave no drawable area"
        }
        require(safeInsets.top + safeInsets.bottom < availableSize.height) {
            "Vertical safe insets leave no drawable area"
        }
    }
}

public data class SkinPlacement(
    public val mode: PresentationMode,
    public val sourceRegion: SkinRect,
    public val viewport: SkinRect,
    public val scale: Float,
) {
    public fun screenX(skinX: Float): Float = viewport.x + (skinX - sourceRegion.x) * scale
    public fun screenY(skinY: Float): Float = viewport.y + (skinY - sourceRegion.y) * scale
    public fun skinX(screenX: Float): Float = sourceRegion.x + (screenX - viewport.x) / scale
    public fun skinY(screenY: Float): Float = sourceRegion.y + (screenY - viewport.y) / scale
}

/** Pure layout calculation shared by Android and future iOS renderers. */
public object SkinLayoutEngine {
    public fun calculate(definition: SkinDefinition, request: SkinLayoutRequest): SkinPlacement {
        val source = when (request.mode) {
            PresentationMode.FULL_DEVICE -> definition.fullDeviceRegion
            PresentationMode.PLAYABLE -> definition.playableRegion
            PresentationMode.LANDSCAPE -> definition.landscapeRegion
            PresentationMode.CONTROLLER_DISPLAY -> definition.controllerDisplayRegion
        }
        val drawableWidth = request.availableSize.width - request.safeInsets.left - request.safeInsets.right
        val drawableHeight = request.availableSize.height - request.safeInsets.top - request.safeInsets.bottom
        val scale = min(drawableWidth / source.width, drawableHeight / source.height)
        val renderedWidth = source.width * scale
        val renderedHeight = source.height * scale
        val viewport = SkinRect(
            x = request.safeInsets.left + (drawableWidth - renderedWidth) / 2f,
            y = request.safeInsets.top + (drawableHeight - renderedHeight) / 2f,
            width = renderedWidth,
            height = renderedHeight,
        )
        return SkinPlacement(request.mode, source, viewport, scale)
    }
}
