package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Colour schemes
//
// Dynamic colour is deliberately OFF. The app already offers System / Dark /
// Light as an explicit setting, and honouring the wallpaper palette on top of
// that produced the real problem this redesign fixes: an unpredictable accent
// that cannot be contrast-checked. One accent, always the same, is the point.
// ---------------------------------------------------------------------------

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    primaryContainer = AccentContainerLight,
    onPrimaryContainer = OnAccentContainerLight,
    secondary = AccentLight,
    onSecondary = OnAccentLight,
    secondaryContainer = AccentContainerLight,
    onSecondaryContainer = OnAccentContainerLight,
    tertiary = AccentLight,
    onTertiary = OnAccentLight,
    tertiaryContainer = AccentContainerLight,
    onTertiaryContainer = OnAccentContainerLight,
    background = PageLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = PageLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = DangerLight,
    onError = OnDangerLight,
    errorContainer = DangerContainerLight,
    onErrorContainer = OnDangerContainerLight,
    surfaceDim = SurfaceContainerHighestLight,
    surfaceBright = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentContainerDark,
    onPrimaryContainer = OnAccentContainerDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentContainerDark,
    onSecondaryContainer = OnAccentContainerDark,
    tertiary = AccentDark,
    onTertiary = OnAccentDark,
    tertiaryContainer = AccentContainerDark,
    onTertiaryContainer = OnAccentContainerDark,
    background = PageDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = DangerDark,
    onError = OnDangerDark,
    errorContainer = DangerContainerDark,
    onErrorContainer = OnDangerContainerDark,
    surfaceDim = SurfaceDark,
    surfaceBright = SurfaceContainerHighDark
)

/**
 * Semantic colours that Material 3 has no slot for.
 *
 * Kept as one small object rather than four loose composition locals so a
 * screen can only ever reach for a colour that has a defined meaning.
 */
@Immutable
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

private val LightSemantics = SemanticColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight
)

private val DarkSemantics = SemanticColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantics }

// ---------------------------------------------------------------------------
// Spacing - a 4dp grid. Screens compose these instead of inventing dp values.
// ---------------------------------------------------------------------------

@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val giant: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

object Space {
    val current: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

/** Reads the semantic palette without threading a parameter through every call. */
object Tonal {
    val colors: SemanticColors
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current
}

// ---------------------------------------------------------------------------
// Icon sizes - one set, so a "play" triangle is the same size on every screen.
//
// The app had drifted to a dozen ad-hoc icon sizes (14, 16, 18, 20, 22, 24,
// 26, 28, 32dp) chosen per call site, so the same affordance rendered at two
// sizes on adjacent screens. These are the sizes the product actually uses.
// ---------------------------------------------------------------------------

object IconSize {
    val xs = 14.dp
    val sm = 16.dp
    val md = 18.dp
    val lg = 20.dp
    val xl = 22.dp
    val xxl = 24.dp
    val xxxl = 28.dp
    val huge = 32.dp
}

// ---------------------------------------------------------------------------
// Layout - one content measure, reused by every screen so nothing drifts.
// ---------------------------------------------------------------------------

@Immutable
data class LayoutMetrics(
    /** Reading measure. Comfortable for a phone, still readable on a monitor. */
    val contentMaxWidth: Dp = 560.dp,
    /** Slightly wider for dense grids (the prayer month table). */
    val wideMaxWidth: Dp = 720.dp,
    /** Minimum touch target, per the Material accessibility guidance. */
    val minTouchTarget: Dp = 48.dp
)

val LocalLayoutMetrics = staticCompositionLocalOf { LayoutMetrics() }

/** Layout metrics, reachable the same way as colours and type. */
val MaterialTheme.layoutMetrics: LayoutMetrics
    @Composable @ReadOnlyComposable get() = LocalLayoutMetrics.current

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

@Composable
fun SalahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSemanticColors provides if (darkTheme) DarkSemantics else LightSemantics
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
