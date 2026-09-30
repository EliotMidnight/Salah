package com.example.ui.theme

import com.example.data.model.QuranPaperTone
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mushaf's paper, contrast-checked.
 *
 * These values cannot be eyeballed. A wash that reads comfortably against
 * near-black ink in light mode can sit at 3:1 against the warm off-white used
 * in dark mode, and the only way to know is to measure - which is why the app
 * already has [ColorContrastTest] for the scheme and this exists so the reader's
 * seven papers get the same treatment.
 */
class QuranPaperContrastTest {

    private val lightInk = QuranPaper.ink(dark = false)
    private val darkInk = QuranPaper.ink(dark = true)

    @Test
    fun `body text clears AA on every light wash`() {
        QuranPaperTone.entries.forEach { tone ->
            val ratio = contrast(lightInk, QuranPaper.wash(tone, dark = false))
            assertTrue(
                "${tone.key} light wash measures %.2f:1 against its ink".format(ratio),
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `body text clears AA on every dark wash`() {
        QuranPaperTone.entries.forEach { tone ->
            val ratio = contrast(darkInk, QuranPaper.wash(tone, dark = true))
            assertTrue(
                "${tone.key} dark wash measures %.2f:1 against its ink".format(ratio),
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `the light and dark treatments are genuinely different`() {
        // A bug that made both branches return the same colour would pass every
        // contrast check above while leaving the theme switch doing nothing.
        QuranPaperTone.entries.forEach { tone ->
            assertTrue(
                "${tone.key} has no separate dark treatment",
                QuranPaper.wash(tone, dark = true) != QuranPaper.wash(tone, dark = false)
            )
        }
    }

    @Test
    fun `each dark wash is darker than its light counterpart, and vice versa`() {
        QuranPaperTone.entries.filter { it != QuranPaperTone.DEFAULT }.forEach { tone ->
            val light = luminance(QuranPaper.wash(tone, dark = false))
            val dark = luminance(QuranPaper.wash(tone, dark = true))
            assertTrue("${tone.key} light wash should be lighter than its dark one", light > dark)
        }
    }

    @Test
    fun `the washes stay in a narrow band so no paper shouts`() {
        // A saturated background behind 24sp Arabic is a reading hazard. These
        // bounds are what "calm and washed out" means in numbers: paper, not
        // paint. Kept generous enough to allow a visible tint, tight enough that
        // no hue can dominate the page.
        QuranPaperTone.entries.forEach { tone ->
            listOf(true, false).forEach { dark ->
                val wash = QuranPaper.wash(tone, dark)
                val l = luminance(wash)
                if (dark) {
                    assertTrue("${tone.key} dark wash is too bright at %.3f".format(l), l < 0.10)
                } else {
                    assertTrue("${tone.key} light wash is too dark at %.3f".format(l), l > 0.85)
                }
            }
        }
    }

    @Test
    fun `the selection ring is visible against its own swatch`() {
        // A selected swatch has to look selected. The ring is drawn in the same
        // ink as the text, so this is really a check that ink and wash do not
        // collapse into each other.
        assertTrue(contrast(QuranPaper.selectionRing(false), QuranPaper.wash(QuranPaperTone.VIOLET, false)) > 3.0)
        assertTrue(contrast(QuranPaper.selectionRing(true), QuranPaper.wash(QuranPaperTone.VIOLET, true)) > 3.0)
    }

    private fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG relative luminance. */
    private fun luminance(color: androidx.compose.ui.graphics.Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
