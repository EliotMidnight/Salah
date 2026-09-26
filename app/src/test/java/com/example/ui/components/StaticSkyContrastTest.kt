package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.engine.AstronomicalSky
import com.example.ui.theme.AccentLight
import com.example.ui.theme.PageDark
import com.example.ui.theme.PageLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextSecondaryLight
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Locks down the one rule about the static sky: only high-contrast text may sit
 * on it.
 *
 * The bug this exists to prevent is not hypothetical. The first version of the
 * sky was lightened in light mode but left at its raw palette value in dark mode,
 * and because the astronomical palette reaches a bright amber at golden hour and
 * a bright azure at midday, `onSurface` measured **1.04:1** against the midday
 * horizon band - white text on near-white. The screen looked fine in a dark
 * screenshot at night and was unreadable at lunchtime.
 *
 * These tests render nothing. [skyBands] is pure, so the guarantee is checked
 * directly against every sky period the engine can produce.
 */
class StaticSkyContrastTest {

    /** One altitude per branch of AstronomicalSky.calculateContinuousSkyColors. */
    private val altitudes = listOf(
        -30f to "deep night",
        -15f to "astronomical twilight",
        -9f to "nautical twilight",
        -3f to "civil twilight",
        5f to "golden hour",
        20f to "afternoon",
        60f to "midday"
    )

    private fun bands(altitude: Float, page: Color) =
        skyBands(AstronomicalSky.calculateContinuousSkyColors(altitude), page)

    @Test
    fun `primary text clears 4_5 to 1 on every band of the light sky`() {
        for ((altitude, name) in altitudes) {
            val b = bands(altitude, PageLight)
            for ((colour, band) in listOf(b.top to "top", b.middle to "middle", b.bottom to "bottom")) {
                val ratio = contrastRatio(TextPrimaryLight, colour)
                assertTrue(
                    "onSurface on the $name $band band was ${ratio.ratio()}, expected >= 4.5:1",
                    ratio >= 4.5f
                )
            }
        }
    }

    @Test
    fun `primary text clears 4_5 to 1 on every band of the dark sky`() {
        for ((altitude, name) in altitudes) {
            val b = bands(altitude, PageDark)
            for ((colour, band) in listOf(b.top to "top", b.middle to "middle", b.bottom to "bottom")) {
                val ratio = contrastRatio(TextPrimaryDark, colour)
                assertTrue(
                    "onSurface on the $name $band band was ${ratio.ratio()}, expected >= 4.5:1",
                    ratio >= 4.5f
                )
            }
        }
    }

    /**
     * The offline status dot is a meaningful graphic, so it is held to the 3:1
     * non-text minimum rather than 4.5:1.
     */
    @Test
    fun `secondary text meets the 3 to 1 graphic minimum on the sky`() {
        for ((altitude, name) in altitudes) {
            for (page in listOf(PageLight to "light", PageDark to "dark")) {
                val b = bands(altitude, page.first)
                val text = if (page.second == "light") TextSecondaryLight else TextSecondaryDark
                for (colour in listOf(b.top, b.middle, b.bottom)) {
                    val ratio = contrastRatio(text, colour)
                    assertTrue(
                        "onSurfaceVariant on the $name ${page.second} sky was ${ratio.ratio()}, " +
                            "expected >= 3:1 for a graphic",
                        ratio >= 3f
                    )
                }
            }
        }
    }

    /**
     * States the rule as an executable fact.
     *
     * The accent and the semantic colours genuinely cannot clear 4.5:1 on a tinted
     * sky - the accent needs the sky pulled 95% of the way to the page in light
     * mode, at which point there is no tint left. This test records that, so the
     * next person to reach for `primary` on the sky finds the reason rather than
     * rediscovering it at lunchtime.
     */
    @Test
    fun `accent and semantic colours are documented as unusable on the sky`() {
        val lightAccentWorst = altitudes.minOf { (altitude, _) ->
            val b = bands(altitude, PageLight)
            listOf(b.top, b.middle, b.bottom).minOf { contrastRatio(AccentLight, it) }
        }
        assertTrue(
            "expected the accent to be unusable as text on the light sky, measured $lightAccentWorst",
            lightAccentWorst < 4.5f
        )

        // Whatever the accent measures, the safe colour must clear the bar with
        // room to spare - this is the margin the blend constant is protecting.
        val onSurfaceWorst = altitudes.minOf { (altitude, _) ->
            val b = bands(altitude, PageLight)
            listOf(b.top, b.middle, b.bottom).minOf { contrastRatio(TextPrimaryLight, it) }
        }
        assertTrue(
            "expected onSurface to clear 4.5:1 with margin, measured $onSurfaceWorst",
            onSurfaceWorst >= 6f
        )
    }

    /** The sky must always be distinguishable from the page it sits on. */
    @Test
    fun `sky is visibly different from the page in both themes`() {
        for ((altitude, name) in altitudes) {
            val light = bands(altitude, PageLight)
            val dark = bands(altitude, PageDark)
            assertTrue(
                "$name: the light sky should not be the page colour",
                light.middle != PageLight
            )
            assertTrue(
                "$name: the dark sky should not be the page colour",
                dark.middle != PageDark
            )
        }
    }

    /** Sanity check on the helper itself. */
    @Test
    fun `contrast ratio is symmetric and bounded`() {
        val a = TextPrimaryLight
        val b = PageLight
        assertTrue(contrastRatio(a, b) > 1f)
        assertTrue(
            kotlin.math.abs(contrastRatio(a, b) - contrastRatio(b, a)) < 0.001f
        )
        assertTrue(contrastRatio(a, a) == 1f)
    }

    private fun Float.ratio() = "%.2f:1".format(this)

    private fun contrastRatio(first: Color, second: Color): Float {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
    }

    private fun luminance(color: Color): Float {
        fun linear(channel: Float): Float =
            if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * linear(color.red) +
            0.7152f * linear(color.green) +
            0.0722f * linear(color.blue)
    }
}
