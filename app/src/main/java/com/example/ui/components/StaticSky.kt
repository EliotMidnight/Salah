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
 *
 * This number is the whole reason text on the sky is legible, so it is a named
 * constant with a test rather than a literal sprinkled through a brush.
 *
 * At 0.72 the darkest possible band still clears 4.5:1 against `onSurface` in
 * both themes (measured worst case: 8.5:1 light, 8.2:1 dark, across all seven sky
 * periods the astronomical engine can produce). Below about 0.6 the guarantee
 * breaks; the sky can be brighter than the page in dark mode, which is what made
 * midday text effectively invisible at 1.04:1.
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
 *
 * Pure and deterministic, so the contrast guarantee is unit-testable without
 * rendering anything.
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
 * It replaced an animated sky, which was a continuously moving high-contrast
 * field; text on top of it needed six layout-bound probes per frame to decide
 * between dark and light
 * type, plus a drop shadow on every string, plus translucent surfaces and borders
 * to keep rows readable.
 *
 * ## The one rule about text on this background
 *
 * Only `onSurface` may be drawn over it, in either theme.
 *
 * That is not a style preference, it is arithmetic. The sky is tinted, and a
 * tinted background cannot carry a mid-tone or accent foreground at 4.5:1 across
 * every sky period. Measured against the worst-case band of all seven palettes
 * the engine produces:
 *
 * | colour              | light | dark |
 * |---------------------|-------|------|
 * | `onSurface`         | 8.5:1 | 8.2:1 |
 * | `onSurfaceVariant`  | 3.7:1 | 3.8:1 |
 * | `primary`           | 2.9:1 | 5.5:1 |
 * | warning / success   | ~2.5:1| ~5.5:1|
 *
 * In light mode only `onSurface` clears the bar. So the accent is used where it
 * sits on a real `surface` - list rows, the navigation bar, sheets - and never
 * here. Hierarchy on the sky comes from size and weight instead, which is what
 * the design brief asks for anyway.
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
