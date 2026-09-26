package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Guards the contrast claims the palette is built on.
 *
 * The redesign's central accessibility claim is "every text pairing clears WCAG
 * AA". A claim like that decays silently the next time someone nudges a hex
 * value, so it is asserted here rather than left as a comment.
 *
 * Replaces `AdaptiveSkyTextTest`, which tested a contrast resolver that existed
 * only because text was being drawn over a moving background. With the animated
 * sky off by default there is nothing left to resolve.
 */
class ColorContrastTest {

    @Test
    fun `light theme body text clears 4_5 to 1 on every surface it is used on`() {
        val colors = lightScheme()
        listOf(
            colors.background to colors.onBackground,
            colors.surface to colors.onSurface,
            colors.surfaceContainer to colors.onSurface,
            colors.surfaceContainerHigh to colors.onSurface,
            colors.surfaceContainerHighest to colors.onSurface,
            colors.surface to colors.onSurfaceVariant,
            colors.surfaceContainerHigh to colors.onSurfaceVariant,
            colors.surfaceContainerHighest to colors.onSurfaceVariant
        ).forEach { (background, foreground) ->
            val ratio = contrastRatio(foreground, background)
            assertTrue(
                "onSurface/onSurfaceVariant on $background was $ratio:1, expected >= 4.5:1",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun `dark theme body text clears 4_5 to 1 on every surface it is used on`() {
        val colors = darkScheme()
        listOf(
            colors.background to colors.onBackground,
            colors.surface to colors.onSurface,
            colors.surfaceContainer to colors.onSurface,
            colors.surfaceContainerHigh to colors.onSurface,
            colors.surfaceContainerHighest to colors.onSurface,
            colors.surface to colors.onSurfaceVariant,
            colors.surfaceContainerHigh to colors.onSurfaceVariant,
            colors.surfaceContainerHighest to colors.onSurfaceVariant
        ).forEach { (background, foreground) ->
            val ratio = contrastRatio(foreground, background)
            assertTrue(
                "onSurface/onSurfaceVariant on $background was $ratio:1, expected >= 4.5:1",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun `accent text clears 4_5 to 1 on the page and on its own container`() {
        listOf(lightScheme(), darkScheme()).forEach { colors ->
            listOf(
                colors.background to colors.primary,
                colors.surface to colors.primary,
                colors.surfaceContainerHigh to colors.primary,
                colors.primaryContainer to colors.onPrimaryContainer
            ).forEach { (background, foreground) ->
                val ratio = contrastRatio(foreground, background)
                assertTrue(
                    "accent $foreground on $background was $ratio:1, expected >= 4.5:1",
                    ratio >= 4.5f
                )
            }
        }
    }

    @Test
    fun `accent filled buttons are legible in both themes`() {
        listOf(lightScheme(), darkScheme()).forEach { colors ->
            val ratio = contrastRatio(colors.onPrimary, colors.primary)
            assertTrue("onPrimary on primary was $ratio:1, expected >= 4.5:1", ratio >= 4.5f)
        }
    }

    @Test
    fun `semantic success and danger text are legible on the surface they sit on`() {
        listOf(
            SuccessLight to OnSuccessLight,
            SuccessDark to OnSuccessDark,
            DangerLight to OnDangerLight,
            DangerDark to OnDangerDark
        ).forEach { (foreground, background) ->
            val ratio = contrastRatio(foreground, background)
            assertTrue("$foreground on $background was $ratio:1, expected >= 4.5:1", ratio >= 4.5f)
        }
    }

    @Test
    fun `outlines meet the 3 to 1 boundary requirement against the page`() {
        listOf(
            OutlineLight to PageLight,
            OutlineDark to PageDark
        ).forEach { (outline, page) ->
            val ratio = contrastRatio(outline, page)
            assertTrue(
                "outline $outline on page $page was $ratio:1, expected >= 3:1 for a UI boundary",
                ratio >= 3f
            )
        }
    }

    /**
     * The bug this palette was written to fix.
     *
     * `surfaceContainerLow` and `background` were both `#F8FAFC` in light mode,
     * so a card was exactly the colour of the page behind it and only a 1dp
     * border separated the two - which is why the old light theme looked like an
     * undifferentiated wall of text.
     */
    @Test
    fun `light theme cards are distinguishable from the page`() {
        val colors = lightScheme()
        assertTrue(
            "surfaceContainerLow must differ from background in light mode",
            colors.surfaceContainerLow != colors.background
        )
        assertTrue(
            "surface must differ from background in light mode",
            colors.surface != colors.background
        )
    }

    /** Same check, dark mode, for the same reason. */
    @Test
    fun `dark theme cards are distinguishable from the page`() {
        val colors = darkScheme()
        assertTrue(
            "surfaceContainerLow must differ from background in dark mode",
            colors.surfaceContainerLow != colors.background
        )
    }

    /**
     * The surface ladder must actually be a ladder - each step visibly distinct
     * from the one below it, or "elevation" carries no meaning.
     *
     * In light mode this starts at `surfaceContainerLow`, because `lowest` is the
     * page colour and `low` is deliberately the same white as `surface`: a card is
     * pure white sitting on a tinted page. The steps that express raisedness are
     * the ones above it.
     */
    @Test
    fun `surface containers form a monotonic ladder in both themes`() {
        listOf(lightScheme(), darkScheme()).forEach { colors ->
            val ladder = listOf(
                colors.surfaceContainerLow,
                colors.surfaceContainer,
                colors.surfaceContainerHigh,
                colors.surfaceContainerHighest
            )
            ladder.zipWithNext().forEach { (lower, higher) ->
                assertTrue(
                    "expected $higher to be more raised than $lower",
                    contrastRatio(higher, lower) > 1.02f
                )
            }
        }
    }

    /**
     * Light mode only.
     *
     * A card in light mode is pure white, identical to `surface`, because the page
     * behind it is tinted - that is the separation. Dark mode is the mirror image:
     * the page is already the darkest step, so a card has to be *lighter* than
     * `surface` to stand out, and the two are deliberately not equal.
     */
    @Test
    fun `light mode cards are white so the tinted page separates them`() {
        val colors = lightScheme()
        assertEquals(colors.surface, colors.surfaceContainerLow)
        assertTrue("the page must be tinted, not white", colors.background != colors.surface)
    }

    @Test
    fun `dark mode cards are lighter than the page so they stand out`() {
        val colors = darkScheme()
        assertTrue(
            "a dark-mode card must be lighter than the page",
            luminance(colors.surfaceContainerLow) > luminance(colors.background)
        )
    }

    @Test
    fun `lighten moves a colour towards white without changing hue order`() {
        val base = AccentLight
        val lightened = base.lighten(0.5f)
        assertTrue(lightened.red > base.red)
        assertTrue(lightened.green > base.green)
        assertTrue(lightened.blue > base.blue)
        assertEquals(1f, base.lighten(1f).red, 0.01f)
    }

    @Test
    fun `mix interpolates between two colours`() {
        val a = androidx.compose.ui.graphics.Color(0f, 0f, 0f)
        val b = androidx.compose.ui.graphics.Color(1f, 1f, 1f)
        assertEquals(0.5f, a.mix(b, 0.5f).red, 0.01f)
        assertEquals(a.red, a.mix(b, 0f).red, 0.01f)
        assertEquals(b.red, a.mix(b, 1f).red, 0.01f)
    }

    // Mirrors lightScheme()/darkScheme() in Theme.kt, which are private.
    private fun lightScheme(): ColorScheme = lightColorScheme(
        primary = AccentLight,
        onPrimary = OnAccentLight,
        primaryContainer = AccentContainerLight,
        onPrimaryContainer = OnAccentContainerLight,
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
        outlineVariant = OutlineVariantLight
    )

    private fun darkScheme(): ColorScheme = darkColorScheme(
        primary = AccentDark,
        onPrimary = OnAccentDark,
        primaryContainer = AccentContainerDark,
        onPrimaryContainer = OnAccentContainerDark,
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
        outlineVariant = OutlineVariantDark
    )

    private fun contrastRatio(
        first: androidx.compose.ui.graphics.Color,
        second: androidx.compose.ui.graphics.Color
    ): Float {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
    }

    private fun luminance(color: androidx.compose.ui.graphics.Color): Float {
        fun linear(channel: Float): Float =
            if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

        return 0.2126f * linear(color.red) +
            0.7152f * linear(color.green) +
            0.0722f * linear(color.blue)
    }
}
