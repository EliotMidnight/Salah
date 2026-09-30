package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's own invariants.
 *
 * These are pure functions over the options value, and they are the rules the
 * UI depends on being true: that a pinch cannot leave a size the slider cannot
 * express, and that no combination of the three mode controls can produce a
 * reader that does not scroll.
 */
class QuranReadingOptionsTest {

    @Test
    fun `continuous layout forces vertical scrolling`() {
        val options = QuranReadingOptions(
            layout = QuranReadingLayout.PER_AYAH,
            scroll = QuranScrollDirection.HORIZONTAL
        )
        val continuous = options.withLayout(QuranReadingLayout.CONTINUOUS)

        assertEquals(QuranReadingLayout.CONTINUOUS, continuous.layout)
        assertEquals(
            "Continuous text cannot turn sideways, so the axis must come back",
            QuranScrollDirection.VERTICAL,
            continuous.scroll
        )
    }

    @Test
    fun `horizontal scroll with continuous layout falls back to per ayah`() {
        // Reaching this state is only possible from storage written by an older
        // build or hand-edited prefs, which is exactly why withScroll repairs it
        // rather than trusting the caller.
        val illegal = QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.VERTICAL
        )
        val horizontal = illegal.withScroll(QuranScrollDirection.HORIZONTAL)

        assertEquals(QuranScrollDirection.HORIZONTAL, horizontal.scroll)
        assertEquals(QuranReadingLayout.PER_AYAH, horizontal.layout)
    }

    @Test
    fun `per page keeps the axis it was given`() {
        listOf(QuranReadingLayout.PER_AYAH, QuranReadingLayout.PER_PAGE).forEach { layout ->
            listOf(
                QuranScrollDirection.VERTICAL,
                QuranScrollDirection.HORIZONTAL
            ).forEach { scroll ->
                val result = QuranReadingOptions()
                    .withLayout(layout)
                    .withScroll(scroll)
                assertEquals(layout, result.layout)
                assertEquals(scroll, result.scroll)
            }
        }
    }

    @Test
    fun `normalise clamps scales into the range the sliders express`() {
        val wild = QuranReadingOptions(
            arabicScale = 9f,
            translationScale = -4f
        ).let(QuranReadingOptions::normalise)

        assertTrue(wild.arabicScale <= QuranReadingOptions.ArabicScaleRange.endInclusive)
        assertTrue(wild.arabicScale >= QuranReadingOptions.ArabicScaleRange.start)
        assertTrue(wild.translationScale <= QuranReadingOptions.TranslationScaleRange.endInclusive)
        assertTrue(wild.translationScale >= QuranReadingOptions.TranslationScaleRange.start)
    }

    @Test
    fun `normalise repairs an illegal stored combination`() {
        val stored = QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL
        )
        val repaired = QuranReadingOptions.normalise(stored)

        assertEquals(QuranScrollDirection.VERTICAL, repaired.scroll)
    }

    @Test
    fun `every layout key round trips`() {
        QuranReadingLayout.entries.forEach { layout ->
            assertEquals(layout, QuranReadingLayout.fromKey(layout.key))
        }
        assertEquals(
            "an unknown key must not throw",
            QuranReadingLayout.PER_AYAH,
            QuranReadingLayout.fromKey("something-from-the-future")
        )
        assertEquals(QuranReadingLayout.PER_AYAH, QuranReadingLayout.fromKey(null))
    }

    @Test
    fun `every scroll pinch paper and font key round trips`() {
        QuranScrollDirection.entries.forEach { assertEquals(it, QuranScrollDirection.fromKey(it.key)) }
        QuranPinchTarget.entries.forEach { assertEquals(it, QuranPinchTarget.fromKey(it.key)) }
        QuranPaperTone.entries.forEach { assertEquals(it, QuranPaperTone.fromKey(it.key)) }
        QuranFontFace.entries.forEach { assertEquals(it, QuranFontFace.fromKey(it.key)) }
    }

    @Test
    fun `the wheel is exactly seven colours plus default`() {
        assertEquals(7, QuranPaperTone.wheel.size)
        assertEquals(7, QuranPaperTone.wheel.distinct().size)
        assertFalse(QuranPaperTone.wheel.contains(QuranPaperTone.DEFAULT))
        assertFalse(QuranPaperTone.DEFAULT.isCustom)
        assertTrue(QuranPaperTone.RED.isCustom)
    }

    @Test
    fun `isPaged is true whenever the page indicator has something to say`() {
        val paged = mapOf(
            QuranReadingLayout.PER_PAGE to QuranReadingOptions.ViewScaleRange,
            QuranReadingLayout.CONTINUOUS to QuranReadingOptions.ViewScaleRange
        )
        // Explicit pairs, because the rule is about the *pair* not either half.
        assertTrue(
            QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE, scroll = QuranScrollDirection.VERTICAL).isPaged
        )
        assertTrue(
            QuranReadingOptions(layout = QuranReadingLayout.PER_AYAH, scroll = QuranScrollDirection.HORIZONTAL).isPaged
        )
        assertFalse(
            QuranReadingOptions(layout = QuranReadingLayout.PER_AYAH, scroll = QuranScrollDirection.VERTICAL).isPaged
        )
        assertFalse(
            QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS, scroll = QuranScrollDirection.VERTICAL).isPaged
        )
        assertTrue(paged.isNotEmpty())
    }

    @Test
    fun `the bundled faces are a strict subset of the offered faces`() {
        // Offered but not bundled is the normal state: the picker shows all
        // seven and marks the ones without a file. What must never happen is
        // the reverse - a bundled face that is not selectable.
        assertTrue(QuranFontFace.bundled.isNotEmpty())
        assertTrue(QuranFontFace.bundled.all { it in QuranFontFace.entries })
        assertTrue(QuranFontFace.entries.size > QuranFontFace.bundled.size)
    }

    @Test
    fun `the default face is one that can actually be drawn`() {
        assertTrue(
            "a default that silently falls back is a silent bug",
            QuranReadingOptions().font.isBundled
        )
    }

    @Test
    fun `every face carries a line height that suits its script`() {
        QuranFontFace.entries.forEach { face ->
            assertTrue(
                "${face.key} needs more than a single line's leading",
                face.lineHeightFactor > 1f
            )
            assertTrue(
                "${face.key} would set lines on top of each other",
                face.lineHeightFactor <= 3f
            )
        }
        // Nastaliq descenders need materially more room than a Naskh face.
        assertTrue(
            QuranFontFace.NASKH_NASTALEEQ.lineHeightFactor >
                QuranFontFace.AMIRI.lineHeightFactor
        )
    }
}
