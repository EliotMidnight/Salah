package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Colour schemes
//
// Dynamic colour is accent-only. The reader's choice (or the wallpaper's
// primary, on Android 12+) is a *seed*: `accentFamily` derives the four accent
// slots and clamps them until every pairing clears WCAG AA. Surfaces are never
// taken from the system palette - the page stays warm ivory / deep navy-black,
// so the neutral ladder and its contrast claims are untouched by what the
// wallpaper happens to be.
//
// The palette is warm paper and deep ink: an ivory page, sand-toned container
// surfaces, and exactly one saturated accent reserved for interaction and state.
// ---------------------------------------------------------------------------

internal fun salahLightScheme(accent: AccentFamily): ColorScheme = lightColorScheme(
    primary = accent.primary,
    onPrimary = accent.onPrimary,
    primaryContainer = accent.primaryContainer,
    onPrimaryContainer = accent.onPrimaryContainer,
    secondary = accent.primary,
    onSecondary = accent.onPrimary,
    secondaryContainer = accent.primaryContainer,
    onSecondaryContainer = accent.onPrimaryContainer,
    tertiary = accent.primary,
    onTertiary = accent.onPrimary,
    tertiaryContainer = accent.primaryContainer,
    onTertiaryContainer = accent.onPrimaryContainer,
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

internal fun salahDarkScheme(accent: AccentFamily): ColorScheme = darkColorScheme(
    primary = accent.primary,
    onPrimary = accent.onPrimary,
    primaryContainer = accent.primaryContainer,
    onPrimaryContainer = accent.onPrimaryContainer,
    secondary = accent.primary,
    onSecondary = accent.onPrimary,
    secondaryContainer = accent.primaryContainer,
    onSecondaryContainer = accent.onPrimaryContainer,
    tertiary = accent.primary,
    onTertiary = accent.onPrimary,
    tertiaryContainer = accent.primaryContainer,
    onTertiaryContainer = accent.onPrimaryContainer,
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
    accentKey: String = Accent.DEFAULT.key,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = Accent.fromKey(accentKey)
    val family = remember(accent, darkTheme) {
        // Read the wallpaper primary only for the mode on screen; the light
        // scheme's primary is a poor seed for a black page and vice versa.
        // Wallpaper changes are picked up on the next activity recreation,
        // which is also when the system refreshes its own palette.
        val dynamicSeed = if (
            accent == Accent.WALLPAPER &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            if (darkTheme) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary
        } else {
            null
        }
        accentFamily(accent, darkTheme, dynamicSeed)
    }
    CompositionLocalProvider(
        LocalSemanticColors provides if (darkTheme) DarkSemantics else LightSemantics
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) salahDarkScheme(family) else salahLightScheme(family),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
