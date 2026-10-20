package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Guards the accent claim: every choice, in either mode, clears WCAG AA.
 *
 * The preset list is a corpus, not an example, so the test loops all of it.
 * The wallpaper path is a *function of an arbitrary seed*, so it is checked as
 * a property: 24 hues through the same derivation the app runs at runtime. The
 * contrast math here is deliberately an independent copy - the oracle must not
 * share the implementation it is checking.
 */
class AccentContrastTest {

    private val modes = listOf(false to "light", true to "dark")

    @Test
    fun `every preset clears the text minimum in both modes`() {
        for ((dark, mode) in modes) {
            Accent.entries.filter { it != Accent.WALLPAPER }.forEach { accent ->
                assertFamily(accentFamily(accent, dark), mode, accent.key)
            }
        }
    }

    @Test
    fun `the clamp holds for any wallpaper seed in both modes`() {
        for ((dark, mode) in modes) {
            for (hue in 0 until 360 step 15) {
                val seed = Color.hsv(
                    hue = hue.toFloat(),
                    saturation = 0.6f,
                    value = if (dark) 0.85f else 0.6f
                )
                assertFamily(familyFromSeed(seed, dark), mode, "hue $hue")
            }
        }
    }

    @Test
    fun `preset accents keep visible chroma in both modes`() {
        for ((dark, mode) in modes) {
            Accent.entries.filter { it != Accent.WALLPAPER }.forEach { accent ->
                val seed = accent.seed(dark)
                val high = max(seed.red, max(seed.green, seed.blue))
                val low = min(seed.red, min(seed.green, seed.blue))
                val chroma = if (high == 0f) 0f else (high - low) / high
                assertTrue(
                    "${accent.key} ($mode) was too gray to read as an accent: $chroma",
                    chroma >= 0.30f
                )
            }
        }
    }

    @Test
    fun `an unknown or absent key falls back to the default`() {
        assertEquals(Accent.DEFAULT, Accent.fromKey(null))
        assertEquals(Accent.DEFAULT, Accent.fromKey("not-a-key"))
    }

    private fun assertFamily(family: AccentFamily, mode: String, label: String) {
        val page = if (mode == "dark") PageDark else PageLight
        listOf(
            "accent text on the page" to (family.primary to page),
            "text on the accent" to (family.onPrimary to family.primary),
            "text on the accent container" to (family.onPrimaryContainer to family.primaryContainer)
        ).forEach { (what, pair) ->
            val ratio = contrastRatio(pair.first, pair.second)
            assertTrue(
                "$label ($mode): $what was $ratio:1, expected >= 4.5:1",
                ratio >= 4.5f
            )
        }
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val lighter = max(luminance(a), luminance(b))
        val darker = min(luminance(a), luminance(b))
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun luminance(color: Color): Float {
        fun linear(channel: Float): Float =
            if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

        return 0.2126f * linear(color.red) +
            0.7152f * linear(color.green) +
            0.0722f * linear(color.blue)
    }
}
