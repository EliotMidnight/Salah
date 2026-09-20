package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Semantic success roles (no M3 slot for these). Fixed tonal greens that stay
 * legible in both themes and alongside dynamic color.
 */
data class SuccessColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private val SuccessColorsLight = SuccessColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight
)

private val SuccessColorsDark = SuccessColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark
)

val LocalSuccessColors = staticCompositionLocalOf { SuccessColorsLight }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = SandAlabaster,
    surface = SurfaceDark,
    onSurface = SandAlabaster,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = CardBorderDark,
    outlineVariant = Color(0xFF1E293B),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = CelestialNavy,
    surface = SurfaceLight,
    onSurface = CelestialNavy,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight
)

@Composable
private fun ColorScheme.animated(): ColorScheme {
    val animSpec = tween<Color>(durationMillis = 350, easing = FastOutSlowInEasing)
    return this.copy(
        primary = animateColorAsState(primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(background, animSpec, label = "background").value,
        onBackground = animateColorAsState(onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        surfaceContainerLowest = animateColorAsState(surfaceContainerLowest, animSpec, label = "surfaceContainerLowest").value,
        surfaceContainerLow = animateColorAsState(surfaceContainerLow, animSpec, label = "surfaceContainerLow").value,
        surfaceContainer = animateColorAsState(surfaceContainer, animSpec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(surfaceContainerHigh, animSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(surfaceContainerHighest, animSpec, label = "surfaceContainerHighest").value,
        outline = animateColorAsState(outline, animSpec, label = "outline").value,
        outlineVariant = animateColorAsState(outlineVariant, animSpec, label = "outlineVariant").value,
        error = animateColorAsState(error, animSpec, label = "error").value,
        onError = animateColorAsState(onError, animSpec, label = "onError").value,
        errorContainer = animateColorAsState(errorContainer, animSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(onErrorContainer, animSpec, label = "onErrorContainer").value,
        surfaceDim = animateColorAsState(surfaceDim, animSpec, label = "surfaceDim").value,
        surfaceBright = animateColorAsState(surfaceBright, animSpec, label = "surfaceBright").value
    )
}

@Composable
fun SalahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val animatedScheme = targetScheme.animated()

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography,
        shapes = Shapes,
        content = {
            CompositionLocalProvider(
                LocalSuccessColors provides if (darkTheme) SuccessColorsDark else SuccessColorsLight
            ) {
                content()
            }
        }
    )
}
