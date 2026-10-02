package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.engine.SkyColorPalette
import com.example.ui.theme.mix

/**
 * How far each sky band is pulled toward the page colour.
 */
const val SKY_PAGE_BLEND = 0.72f

/** The three vertical bands of the static sky, already constrained for legibility. */
@Immutable
data class SkyBands(
    val top: Color,
    val middle: Color,
    val bottom: Color
)

/**
 * Derives the gradient bands from the live astronomical palette, pulled toward
 * the page so that text drawn over them stays readable.
 */
fun skyBands(palette: SkyColorPalette, page: Color): SkyBands {
    val raw = SkyBands(
        top = palette.zenithColor.mix(palette.midSkyColor, 0.35f),
        middle = palette.midSkyColor.mix(palette.horizonColor, 0.25f),
        bottom = palette.horizonColor
    )
    return SkyBands(
        top = raw.top.mix(page, SKY_PAGE_BLEND),
        middle = raw.middle.mix(page, SKY_PAGE_BLEND),
        bottom = raw.bottom.mix(page, SKY_PAGE_BLEND)
    )
}

/**
 * The background for the Today screen: a still sky.
 *
 * ## The one rule about text on this background
 *
 * | colour              | light | dark |
 * |---------------------|-------|------|
 * | `onSurface`         | 8.5:1 | 8.2:1 |
 * | `onSurfaceVariant`  | 3.7:1 | 3.8:1 |
 * | `primary`           | 2.9:1 | 5.5:1 |
 * | warning / success   | ~2.5:1| ~5.5:1|
 */
@Composable
fun StaticSkyBackground(
    palette: SkyColorPalette,
    page: Color,
    modifier: Modifier = Modifier
) {
    val bands = skyBands(palette, page)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to bands.top,
                    0.55f to bands.middle,
                    1f to bands.bottom,
                    startY = 0f,
                    endY = 1400f
                )
            )
    )
}
