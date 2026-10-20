package com.example.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.Accent
import com.example.ui.theme.PageDark
import com.example.ui.theme.PageLight
import com.example.ui.theme.accentFamily
import com.example.ui.theme.salahDarkScheme
import com.example.ui.theme.salahLightScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * The halo is painted behind the page, including the headline and the NEXT
 * line. Its wash is faint, but "faint" is not a contrast guarantee: the sun's
 * warm gold and the moon's cool blue can both move the background under AA.
 *
 * These checks use the same peak the page draws, so tuning the halo cannot
 * quietly invalidate the text palette. The compact core is excluded on purpose:
 * it sits around the disc, above every line of text.
 */
class HaloContrastTest {

    @Test
    fun `body and secondary text clear AA over the brightest wash`() {
        for (dark in listOf(false, true)) {
            for (isSun in listOf(false, true)) {
                val wash = haloPeaks(dark = dark, isSun = isSun).wash
                val background = composite(
                    overlay = auraFor(isSun),
                    under = if (dark) PageDark else PageLight,
                    alpha = wash
                )
                for (accent in Accent.entries) {
                    val family = accentFamily(accent, dark = dark)
                    val scheme = if (dark) salahDarkScheme(family) else salahLightScheme(family)
                    assertAtLeast(
                        4.5,
                        scheme.onSurface,
                        background,
                        "onSurface over ${name(dark, isSun)} wash ($accent)"
                    )
                    assertAtLeast(
                        4.5,
                        scheme.onSurfaceVariant,
                        background,
                        "onSurfaceVariant over ${name(dark, isSun)} wash ($accent)"
                    )
                    assertAtLeast(
                        4.5,
                        scheme.primary,
                        background,
                        "primary over ${name(dark, isSun)} wash ($accent)"
                    )
                }
            }
        }
    }

    private fun name(dark: Boolean, isSun: Boolean): String =
        "${if (isSun) "sun" else "moon"}/${if (dark) "dark" else "light"}"

    private fun assertAtLeast(minimum: Double, foreground: Color, background: Color, label: String) {
        val ratio = contrast(foreground, background)
        assertTrue(
            "$label measured %.2f:1, below %.1f:1".format(ratio, minimum),
            ratio >= minimum
        )
    }

    private fun composite(overlay: Color, under: Color, alpha: Float): Color = Color(
        red = overlay.red * alpha + under.red * (1f - alpha),
        green = overlay.green * alpha + under.green * (1f - alpha),
        blue = overlay.blue * alpha + under.blue * (1f - alpha),
        alpha = 1f
    )

    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance()
        val second = b.luminance()
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }
}
