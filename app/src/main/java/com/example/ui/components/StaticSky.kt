package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.engine.SkyColorPalette
import com.example.ui.theme.lighten
import com.example.ui.theme.mix

/**
 * The default background for the Today screen: a still sky.
 *
 * The animated [LivingSkyCanvas] remains available behind a Settings toggle, but
 * it is no longer the default, and this is what replaced it.
 *
 * The point of the change is legibility. The living sky is a continuously moving,
 * high-contrast field, and text on top of it needed six layout-bound probes per
 * frame to work out whether to switch to dark or light type, plus a drop shadow
 * on every string, plus translucent surfaces and borders to keep rows readable.
 * A static gradient needs none of that: it is dark at the top in dark mode and
 * near-white in light mode, so the ordinary `onSurface` / `onSurfaceVariant`
 * colours already clear 4.5:1 against it and no per-element measurement exists
 * anywhere in the screen.
 *
 * The gradient is still derived from the same astronomical palette, so the screen
 * keeps its sense of time of day - it is the movement and the contrast churn that
 * were removed, not the colour.
 */
@Composable
fun StaticSkyBackground(
    palette: SkyColorPalette,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    // A dark sky keeps its own luminance; a light one is lifted almost to white so
    // it behaves like the page it sits behind rather than a coloured panel.
    val top: Color
    val bottom: Color
    if (isDark) {
        top = palette.zenithColor.mix(palette.midSkyColor, 0.35f)
        bottom = palette.midSkyColor.mix(palette.horizonColor, 0.25f)
    } else {
        top = palette.zenithColor.lighten(0.86f)
        bottom = palette.horizonColor.lighten(0.80f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to top,
                    0.55f to palette.midSkyColor.lighten(if (isDark) 0.0f else 0.84f),
                    1f to bottom,
                    startY = 0f,
                    endY = 1400f
                )
            )
    )
}
