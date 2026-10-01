package com.example.ui.quran.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that decides how a mushaf page is scaled to the space it has.
 *
 * ### Why this is a test file rather than a comment
 *
 * The previous implementation was a heuristic inside a `remember` block in a
 * composable: it counted characters, divided by a guessed characters-per-line
 * derived from the line height, and scaled by `totalLines * lineHeight * 0.42f`.
 * Nobody could check whether the number it produced was right, which is exactly
 * how it went wrong. The three defects below are the ones that reached a reader,
 * and each is now a case here:
 *
 * 1. a page that fitted was shrunk anyway, and a page that did not fit was clipped;
 * 2. the scale was clamped at **0.5**, below the reader's own slider minimum of
 *    **0.7**, so the size on screen could be a size the reader could not express
 *    and nothing could undo;
 * 3. a page that still did not fit at the floor was reported as fitting, so its
 *    last lines were unreachable.
 *
 * The arithmetic is now separated from the measurement precisely so it can be
 * checked like this. `contentHeightPx` and `viewportHeightPx` are inputs; the
 * measurement that produces them lives in the composable and is exercised by the
 * screenshot tests.
 */
class PageFitTest {

    private val minimum = 0.7f

    // --- A page that fits -------------------------------------------------

    @Test
    fun `a page that fits is shown at exactly the requested size`() {
        val result = PageFit.resolve(
            contentHeightPx = 1800,
            viewportHeightPx = 2000,
            requested = 1.3f,
            minimum = minimum
        )
        assertEquals(1.3f, result.scale, 0.0001f)
        assertFalse("a fitting page must not be reported as scaled", result.wasScaled)
        assertFalse(result.needsScroll)
    }

    @Test
    fun `a page that exactly fits is not shrunk`() {
        // Off by one pixel in either direction. A page that exactly fills the
        // viewport is not over it, and shrinking it is a page the reader cannot
        // undo back to the size they chose.
        val exact = PageFit.resolve(2000, 2000, 1f, minimum)
        assertEquals(1f, exact.scale, 0.0001f)
        assertFalse(exact.wasScaled)

        val oneOver = PageFit.resolve(2001, 2000, 1f, minimum)
        assertTrue(oneOver.wasScaled)
    }

    @Test
    fun `a page is never enlarged past the requested size`() {
        // The asymmetry is deliberate and is the whole reason the rule is stated
        // this way round: a reader who has deliberately made the text *smaller*
        // than the default has answered a question, and growing it back would
        // override them on most pages to suit a few short ones.
        val result = PageFit.resolve(
            contentHeightPx = 400,
            viewportHeightPx = 2000,
            requested = 0.8f,
            minimum = minimum
        )
        assertEquals(0.8f, result.scale, 0.0001f)
        assertFalse(result.wasScaled)
    }

    @Test
    fun `a short page is never enlarged to fill the viewport`() {
        // A four-verse page is short. Printing it at the size that fills the
        // screen would make Al-Ikhlas - which is four lines long - into a page of
        // enormous type, and would make the *next* page, which is dense, unreadable
        // at the same setting. Pages are not all the same density; the reader's
        // size is the same on all of them.
        val result = PageFit.resolve(300, 2000, 1f, minimum)
        assertEquals(1f, result.scale, 0.0001f)
    }

    // --- A page that does not fit ----------------------------------------

    @Test
    fun `a page that is too tall is reduced by exactly the ratio`() {
        // Measured at 1.0 it is twice the space, so at 0.5 it fits. The floor
        // argument here is 0.4 so the ratio is the answer and the floor is not
        // tested in this case.
        val result = PageFit.resolve(
            contentHeightPx = 4000,
            viewportHeightPx = 2000,
            requested = 1f,
            minimum = 0.4f
        )
        assertEquals(0.5f, result.scale, 0.0001f)
        assertTrue(result.wasScaled)
        assertFalse("at 0.5 it fits, so it must not scroll", result.needsScroll)
    }

    @Test
    fun `a page is reduced by the overflow ratio whenever the floor allows it`() {
        // The ratio, not the absolute size: a page 5% too tall and a page 40% too
        // tall both come back to exactly the space available. Anything else means
        // some pages fit and others do not for reasons the reader cannot see.
        //
        // Every fixture here stays above the floor, so the ratio is the answer in
        // all of them - which is the point. The case where the floor binds is its
        // own test below, and conflating the two is what made the original rule
        // impossible to reason about.
        val viewport = 2000
        for (content in listOf(2100, 2200, 2600, 2800)) {
            val result = PageFit.resolve(content, viewport, 1f, 0.4f)
            assertTrue("content=$content was not reduced at all", result.wasScaled)
            assertTrue(
                "content=$content at scale ${result.scale} draws to " +
                    "${result.scale * content}, which does not fit $viewport",
                result.scale * content <= viewport + 1
            )
            assertFalse("content=$content reports overflow", result.needsScroll)
        }
    }

    @Test
    fun `a page too tall for the floor to save it is reduced to the floor and scrolls`() {
        // The case the ratio cannot cover, stated separately. Ten times the space
        // available needs a scale of 0.1, which the reader cannot express, so the
        // floor wins and the page is honestly reported as overflowing. The reader
        // scrolls; nothing is lost and nothing is illegible.
        val result = PageFit.resolve(20_000, 2000, 1f, 0.4f)
        assertEquals(0.4f, result.scale, 0.0001f)
        assertTrue(result.needsScroll)
    }

    // --- The floor -------------------------------------------------------

    @Test
    fun `a page never shrinks below the readers own minimum`() {
        // This is the invariant the old 0.5 clamp broke. A preference that can
        // hold a value no control can express is a preference the user cannot
        // undo, and the reader's slider bottoms out at 0.7.
        val result = PageFit.resolve(
            contentHeightPx = 100_000,
            viewportHeightPx = 2000,
            requested = 1f,
            minimum = minimum
        )
        assertEquals(minimum, result.scale, 0.0001f)
        assertTrue("it still does not fit, and must say so", result.needsScroll)
    }

    @Test
    fun `the floor defaults to the bottom of the sliders own range`() {
        // Not to a constant in this file. If the slider's range moves, the floor
        // moves with it, because the default is read from the same value.
        val result = PageFit.resolve(100_000, 2000, 1f)
        assertEquals(
            com.example.data.model.QuranReadingOptions.ArabicScaleRange.start,
            result.scale,
            0.0001f
        )
    }

    @Test
    fun `a page at the minimum size that does fit is not reported as overflowing`() {
        // The floor case is only an overflow when it genuinely still does not fit.
        // Getting this wrong in the other direction makes every page scrollable,
        // which destroys the "shown whole" promise for no reason.
        val result = PageFit.resolve(2857, 2000, 1f, minimum)
        assertEquals(minimum, result.scale, 0.0001f)
        assertFalse(
            "2857 * 0.7 = ${2857 * 0.7}, which is under 2000",
            result.needsScroll
        )
    }

    // --- Overflow --------------------------------------------------------

    @Test
    fun `a page that still overflows at the floor asks to scroll`() {
        val result = PageFit.resolve(10_000, 2000, 1f, minimum)
        assertTrue(result.needsScroll)
        assertTrue(result.wasScaled)
    }

    @Test
    fun `overflow is decided from the drawn size not the measured one`() {
        // The check has to be against what will be *drawn*. Measuring the
        // unshrunk page and reporting that as overflow would make a page
        // scrollable at every size, including the ones where it fits comfortably.
        val result = PageFit.resolve(4000, 2000, 1f, 0.4f)
        assertFalse(
            "4000 * 0.5 = 2000, which fits",
            result.needsScroll
        )
    }

    @Test
    fun `an unmeasured page is neither scaled nor overflowing`() {
        // Before the first layout pass there is no width and no height. Reporting
        // a scale in that state would either flash a wrong size or, worse, decide
        // the page is overflowing and make it scrollable from frame one.
        val zero = PageFit.resolve(0, 0, 1.2f, minimum)
        assertEquals(1.2f, zero.scale, 0.0001f)
        assertFalse(zero.wasScaled)
        assertFalse(zero.needsScroll)
    }

    @Test
    fun `a zero-height viewport leaves the page at the readers size`() {
        // Same reason: no space measured yet is not "no space".
        val result = PageFit.resolve(2000, 0, 1.2f, minimum)
        assertEquals(1.2f, result.scale, 0.0001f)
        assertFalse(result.needsScroll)
    }

    // --- The scale is always expressible ----------------------------------

    @Test
    fun `every scale this can return is one the reader can express`() {
        // The property the old implementation violated. Swept rather than
        // asserted case by case, because the point is that *no* input can produce
        // an unexpressible scale - including the ones nobody thought to write a
        // test for.
        val range = com.example.data.model.QuranReadingOptions.ArabicScaleRange
        val viewports = listOf(400, 800, 1200, 1600, 2400, 3200)
        val contents = listOf(100, 900, 1800, 2200, 5000, 20000, 200_000)
        val requesteds = listOf(0.7f, 0.85f, 1f, 1.3f, 1.7f, 2f)

        for (viewport in viewports) {
            for (content in contents) {
                for (requested in requesteds) {
                    val result = PageFit.resolve(content, viewport, requested)
                    assertTrue(
                        "scale ${result.scale} at requested=$requested content=$content " +
                            "viewport=$viewport is outside ${range.start}..${range.endInclusive}",
                        result.scale >= range.start - 0.0001f
                    )
                    assertTrue(
                        "scale ${result.scale} at requested=$requested exceeds the request",
                        result.scale <= requested + 0.0001f
                    )
                }
            }
        }
    }

    @Test
    fun `a scale is never larger than the reader asked for, whatever the page`() {
        for (viewport in listOf(200, 1000, 4000)) {
            for (content in listOf(1, 500, 3000, 90_000)) {
                for (requested in listOf(0.7f, 1f, 2f)) {
                    val scale = PageFit.scale(content, viewport, requested)
                    assertTrue(
                        "content=$content viewport=$viewport grew past requested=$requested",
                        scale <= requested + 0.0001f
                    )
                }
            }
        }
    }
}
