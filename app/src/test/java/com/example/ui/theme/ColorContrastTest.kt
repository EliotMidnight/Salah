package com.example.ui.theme

import androidx.compose.material3.ColorScheme
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
 * The palette has a warm ivory / deep navy page and a short container ladder.
 * The checks below still walk every surface a text role can land on, so a
 * richer neutral ground cannot quietly break body-text contrast.
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
    fun `accent text and buttons are legible in both themes`() {
        listOf(lightScheme(), darkScheme()).forEach { colors ->
            // Button / primary-action text on the accent.
            val ratio1 = contrastRatio(colors.onPrimary, colors.primary)
            assertTrue(
                "onPrimary on primary was $ratio1:1, expected >= 4.5:1",
                ratio1 >= 4.5f
            )
            // Selected-state text on the accent container.
            val ratio2 = contrastRatio(colors.onPrimaryContainer, colors.primaryContainer)
            assertTrue(
                "onPrimaryContainer on primaryContainer was $ratio2:1, expected >= 4.5:1",
                ratio2 >= 4.5f
            )
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

    @Test
    fun `lighten moves a colour towards white without changing hue order`() {
        val base = AccentLight
        val lightened = base.lighten(0.5f)
        assertTrue(lightened.red > base.red)
        assertTrue(lightened.green > base.green)
        assertTrue(lightened.blue > base.blue)
    }

    @Test
    fun `mix interpolates between two colours`() {
        val a = androidx.compose.ui.graphics.Color(0f, 0f, 0f)
        val b = androidx.compose.ui.graphics.Color(1f, 1f, 1f)
        assertEquals(0.5f, a.mix(b, 0.5f).red, 0.01f)
        assertEquals(a.red, a.mix(b, 0f).red, 0.01f)
        assertEquals(b.red, a.mix(b, 1f).red, 0.01f)
    }

    // The real builders, so this test cannot drift from the scheme it checks.
    private fun lightScheme(): ColorScheme =
        salahLightScheme(accentFamily(Accent.SLATE, dark = false))

    private fun darkScheme(): ColorScheme =
        salahDarkScheme(accentFamily(Accent.SLATE, dark = true))

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
