package com.digihori.pgp.desktop.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Visual presets are kept separate from the UI so another identity can be added without rewriting screens. */
internal enum class StudioThemePreset {
    INSTRUMENT,
}

@Immutable
internal data class StudioComponentColors(
    val lcdBackground: Color,
    val lcdDot: Color,
    val characterDotOn: Color,
    val characterDotOff: Color,
    val characterGrid: Color,
    val softwareKey: Color,
    val softwareKeyPressed: Color,
    val softwareKeyDisabled: Color,
    val onSoftwareKey: Color,
    val onSoftwareKeyPressed: Color,
)

internal val LocalStudioComponentColors = staticCompositionLocalOf<StudioComponentColors> {
    error("Studio component colors are only available inside PgpStudioTheme")
}

private val InstrumentColorScheme = lightColorScheme(
    primary = Color(0xffa94718),
    onPrimary = Color(0xffffffff),
    primaryContainer = Color(0xffe8c3ad),
    onPrimaryContainer = Color(0xff351000),
    secondary = Color(0xff555d5f),
    onSecondary = Color(0xffffffff),
    secondaryContainer = Color(0xffd9dfe0),
    onSecondaryContainer = Color(0xff151a1b),
    tertiary = Color(0xff596044),
    onTertiary = Color(0xffffffff),
    background = Color(0xffe8e6df),
    onBackground = Color(0xff20211f),
    surface = Color(0xfff4f2ec),
    onSurface = Color(0xff20211f),
    surfaceVariant = Color(0xffd8d6cf),
    onSurfaceVariant = Color(0xff494a46),
    outline = Color(0xff777873),
    outlineVariant = Color(0xffc3c4bd),
    error = Color(0xffa9362a),
    onError = Color(0xffffffff),
)

private val InstrumentComponentColors = StudioComponentColors(
    lcdBackground = Color(0xffc5cdaa),
    lcdDot = Color(0xff273126),
    characterDotOn = Color(0xff292b29),
    characterDotOff = Color(0xffe4e2dc),
    characterGrid = Color(0xff747670),
    softwareKey = Color(0xffdedbd2),
    softwareKeyPressed = Color(0xffa94718),
    softwareKeyDisabled = Color(0xffd0cec7),
    onSoftwareKey = Color(0xff242522),
    onSoftwareKeyPressed = Color(0xffffffff),
)

private val InstrumentShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
internal fun PgpStudioTheme(
    preset: StudioThemePreset = StudioThemePreset.INSTRUMENT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (preset) {
        StudioThemePreset.INSTRUMENT -> InstrumentColorScheme
    }
    val componentColors = when (preset) {
        StudioThemePreset.INSTRUMENT -> InstrumentComponentColors
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalStudioComponentColors provides componentColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = InstrumentShapes,
            content = content,
        )
    }
}
