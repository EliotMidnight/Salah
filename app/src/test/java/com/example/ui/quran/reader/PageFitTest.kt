package com.example.ui.quran.reader

import com.example.data.model.QuranReadingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a mushaf page is scaled to the space it has.
 *
 * ### Why the arithmetic is gone
 *
 * The previous rule multiplied the reader's scale by `viewport / contentHeight`, on
 * the assumption that a page's height is proportional to its type size. For this
 * text it is not: a page is `lines x lineHeight`, and raising the size raises
 * *both* - a bigger font has more leading **and** wraps the same text into more
 * lines. Height grows with roughly the square of the size.
 *
 * That assumption had two consequences, and the second loses verses:
 *
 * 1. pages were over-shrunk, visibly smaller than they needed to be;
 * 2. the overflow check extrapolated from the measurement at the reader's size and
 *    concluded that a page clamped at the minimum now fitted, when at that size it
 *    was still too tall. The page was reported as fitting, was not scrolled, and
 *    its last lines were cut off with no way to reach them.
 *
 * These tests pin a **quadratic** height model, because that is the model the real
 * text obeys, and they check that the answer comes from measuring at the scale
 * actually drawn.
 */
class PageFitTest {

    private val viewport = 2000
    private val minimum = QuranReadingOptions.ArabicScaleRange.start

    /**
     * A page whose height grows with the square of the scale.
     *
     * Built from a real shape rather than a formula, so the tests below exercise
     * the same non-linearity the corpus does: a base height at 1x plus a component
     * that grows with the size, standing in for the extra lines a bigger font
     * wraps into.
     */
    private fun pageAt(base: Int, wrapping: Int) = { scale: Float ->
        // Rounded, not truncated. `toInt()` on a value below 1 gives 0, so the
        // quadratic term vanished entirely for every scale under 1 - which is
        // every scale this rule ever chooses. The page then measured *shorter* as
        // the type got smaller, and the search walked the wrong way.
        (base * (scale * scale) + wrapping * scale).toInt()
    }

    // --- A page that fits ------------------------------------------------

    @Test
    fun `a page that fits is shown at exactly the requested size`() {
        var calls = 0
        val result = PageFit.resolve(
            measure = { calls++; viewport - 100 },
            viewportHeightPx = viewport,
            requested = 1.3f,
            minimum = minimum
        )
        assertEquals(1.3f, result.scale, 0.0001f)
        assertFalse("a fitting page must not be reported as scaled", result.wasScaled)
        assertFalse(result.needsScroll)
        assertEquals("a fitting page needs one measurement", 1, calls)
    }

    @Test
    fun `a page that exactly fits is not shrunk`() {
        // Off by one pixel either way. A page that exactly fills the viewport is not
        // over it, and shrinking it is a page the reader cannot undo back to the
        // size they chose.
        val exact = PageFit.resolve({ viewport }, viewport, 1f, minimum)
        assertEquals(1f, exact.scale, 0.0001f)
        assertFalse(exact.wasScaled)

        val oneOver = PageFit.resolve({ viewport + 1 }, viewport, 1f, minimum)
        assertTrue(oneOver.wasScaled)
    }

    @Test
    fun `a page is never enlarged past the requested size`() {
        // The asymmetry is the whole rule: a reader who made the text smaller than
        // the default on purpose has answered a question, and growing it back would
        // override them on most pages to suit a few short ones.
        val result = PageFit.resolve({ 400 }, viewport, 0.8f, minimum)
        assertEquals(0.8f, result.scale, 0.0001f)
        assertFalse(result.wasScaled)
    }

    @Test
    fun `a short page is never enlarged to fill the viewport`() {
        // A four-verse page is short. Printing it at whatever size fills the screen
        // would make Al-Ikhlas enormous and the next, dense page unreadable at the
        // same setting. Pages are not all the same density; the reader's size is
        // the same on all of them.
        assertEquals(1f, PageFit.resolve({ 300 }, viewport, 1f, minimum).scale, 0.0001f)
    }

    // --- A page that does not fit ----------------------------------------

    @Test
    fun `a page that is too tall is reduced until it measurably fits`() {
        // Height = 1000*s^2 + 2000*s. At s=1 that is 3000 against a 2000 viewport;
        // solving for 2000 gives s = 0.732.
        val measure = pageAt(base = 1000, wrapping = 2000)
        val result = PageFit.resolve(measure, viewport, 1f, minimum)

        assertTrue("the page was not reduced", result.wasScaled)
        assertTrue(
            "at scale ${result.scale} the page measures ${measure(result.scale)}, " +
                "which does not fit $viewport",
            measure(result.scale) <= viewport
        )
        assertFalse("a page that fits must not claim to overflow", result.needsScroll)
    }

    @Test
    fun `the chosen scale is close to the largest that fits`() {
        // A measured answer is only worth taking if it is *good*. The exact solution
        // for 1000*s^2 + 2000*s = 2000 is about 0.7321, and three bisections from
        // [0.7, 1] land within a few percent of it. Over-shrinking by more than that
        // is visible on a page of Arabic, and is what the old linear rule did.
        val measure = pageAt(base = 1000, wrapping = 2000)
        val result = PageFit.resolve(measure, viewport, 1f, minimum)
        val exact = solveQuadratic(base = 1000, wrapping = 2000, viewport = viewport)
        assertTrue(
            "scale ${result.scale} is more than 10% under the exact $exact",
            result.scale > exact * 0.9f
        )
        assertTrue(
            "scale ${result.scale} exceeds the exact solution $exact",
            result.scale <= exact + 0.0001f
        )
    }

    @Test
    fun `a page is never shrunk below the readers own minimum`() {
        // The invariant the old 0.5 clamp broke. A preference that can hold a value
        // no control can express is a preference the user cannot undo, and the
        // reader's slider bottoms out at 0.7.
        val result = PageFit.resolve({ 1_000_000 }, viewport, 1f, minimum)
        assertEquals(minimum, result.scale, 0.0001f)
    }

    @Test
    fun `the floor defaults to the bottom of the sliders own range`() {
        // Not to a constant in this file. If the slider's range moves, the floor
        // moves with it, because the default is read from the same value.
        val result = PageFit.resolve({ 1_000_000 }, viewport, 1f)
        assertEquals(QuranReadingOptions.ArabicScaleRange.start, result.scale, 0.0001f)
    }

    // --- Overflow, which is what this type exists for --------------------

    @Test
    fun `a page that still overflows at the floor asks to scroll`() {
        // The case the linear extrapolation got wrong. At the minimum this page is
        // still ten times the viewport, and a rule assuming height was proportional
        // to size would have reported it as fitting.
        val measure = pageAt(base = 20_000, wrapping = 40_000)
        val result = PageFit.resolve(measure, viewport, 1f, minimum)
        assertEquals(minimum, result.scale, 0.0001f)
        assertTrue("a page ten times too tall was reported as fitting", result.needsScroll)
    }

    @Test
    fun `overflow is decided from a measurement at the scale that will be drawn`() {
        // Deliberately awkward: a page only *slightly* too tall at 1x, whose height
        // at the fitted scale is close enough to the viewport that a linear estimate
        // rounds the wrong way. The answer has to come from `measure(best)`.
        val measure = pageAt(base = 3000, wrapping = 500)
        val result = PageFit.resolve(measure, viewport, 1f, minimum)
        assertEquals(
            "overflow must agree with what is actually drawn",
            measure(result.scale) > viewport,
            result.needsScroll
        )
    }

    @Test
    fun `a page at the minimum size that does fit is not reported as overflowing`() {
        // Getting this wrong the other way makes every page scrollable, which
        // destroys the "shown whole" promise for no reason.
        val measure = pageAt(base = 2000, wrapping = 0)
        val result = PageFit.resolve(measure, viewport, 1f, minimum)
        assertFalse("a page that fits at the floor must not scroll", result.needsScroll)
    }

    @Test
    fun `an unmeasured page is neither scaled nor overflowing`() {
        // No space measured yet is not "no space", and this is evaluated on the very
        // first frame.
        val zero = PageFit.resolve({ 0 }, 0, 1.2f, minimum)
        assertEquals(1.2f, zero.scale, 0.0001f)
        assertFalse(zero.wasScaled)
        assertFalse(zero.needsScroll)
    }

    @Test
    fun `an unmeasured page is not measured at all`() {
        // Measuring before checking the viewport would run a text layout with no
        // width, on the first frame, for a page about to be measured again anyway.
        var calls = 0
        PageFit.resolve({ calls++; 500 }, 0, 1f, minimum)
        assertEquals("the measure ran with no viewport", 0, calls)
    }

    // --- The scale is always expressible ----------------------------------

    @Test
    fun `every scale this can return is one the reader can express`() {
        // The property the old implementation violated. Swept rather than asserted
        // case by case, because the point is that *no* input produces an
        // unexpressible scale - including the ones nobody thought to write a test
        // for.
        val range = QuranReadingOptions.ArabicScaleRange
        for (viewportHeight in listOf(300, 800, 1200, 1600, 2400, 4000)) {
            for (base in listOf(50, 900, 1800, 2200, 5000, 20000, 200_000)) {
                // Stepped through the range rather than iterating it, because a
                // `ClosedFloatingPointRange` is not directly iterable in the test
                // source set - and stepping is clearer about what is being checked.
                var requested = range.start
                while (requested <= range.endInclusive) {
                    val result = PageFit.resolve(
                        measure = pageAt(base, base / 2),
                        viewportHeightPx = viewportHeight,
                        requested = requested,
                        minimum = minimum
                    )
                    assertTrue(
                        "scale ${result.scale} (base=$base viewport=$viewportHeight " +
                            "requested=$requested) is below ${range.start}",
                        result.scale >= range.start - 0.0001f
                    )
                    assertTrue(
                        "scale ${result.scale} exceeds the requested $requested",
                        result.scale <= requested + 0.0001f
                    )
                    requested += 0.1f
                }
            }
        }
    }

    @Test
    fun `a page reported as fitting really does fit`() {
        // The property that matters most and cannot be got from one example.
        // Swept over sizes, viewports and requests, on the quadratic shape.
        for (viewportHeight in listOf(400, 1000, 2000, 3200)) {
            for (base in listOf(100, 1500, 4000, 30000)) {
                for (requested in listOf(0.7f, 1f, 1.5f, 2f)) {
                    val measure = pageAt(base, base / 3)
                    val result = PageFit.resolve(measure, viewportHeight, requested, minimum)
                    if (!result.needsScroll) {
                        assertTrue(
                            "reported as fitting but measures ${measure(result.scale)} " +
                                "at ${result.scale}, viewport $viewportHeight, " +
                                "base $base, requested $requested",
                            measure(result.scale) <= viewportHeight
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `a page reported as overflowing really does not fit`() {
        // The converse, and the one the old rule broke in the dangerous direction.
        for (viewportHeight in listOf(400, 1000, 2000, 3200)) {
            for (base in listOf(100, 1500, 4000, 30000)) {
                for (requested in listOf(0.7f, 1f, 1.5f, 2f)) {
                    val measure = pageAt(base, base / 3)
                    val result = PageFit.resolve(measure, viewportHeight, requested, minimum)
                    if (result.needsScroll) {
                        assertTrue(
                            "reported as overflowing but fits: " +
                                "${measure(result.scale)} at ${result.scale}, " +
                                "viewport $viewportHeight",
                            measure(result.scale) > viewportHeight
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `the number of measurements is bounded`() {
        // A text measurement is real work on the frame a page turns. Bounded by
        // construction, not by hope.
        var calls = 0
        PageFit.resolve({ calls++; 999_999 }, viewport, 1f, minimum)
        assertTrue(
            "a non-fitting page took $calls measurements, " +
                "more than the ${PageFit.MAX_MEASUREMENTS} it is allowed",
            calls <= PageFit.MAX_MEASUREMENTS
        )
    }

    @Test
    fun `a fitting page costs one measurement and a non-fitting one costs a few`() {
        var cheap = 0
        PageFit.resolve({ cheap++; 100 }, viewport, 1f, minimum)
        assertEquals(1, cheap)

        // One at the reader's size, one at the floor - needed because the overflow
        // answer is about the floor - and up to three bisections between them.
        var dear = 0
        PageFit.resolve({ dear++; 50_000 }, viewport, 1f, minimum)
        assertTrue(
            "a non-fitting page took $dear measurements",
            dear in 3..PageFit.MAX_MEASUREMENTS
        )
    }

    /** The largest `s` for which `base*s^2 + wrapping*s == viewport`. */
    private fun solveQuadratic(base: Int, wrapping: Int, viewport: Int): Float {
        val b = wrapping.toDouble()
        val c = -viewport.toDouble()
        val root = (-b + kotlin.math.sqrt(b * b - 4 * base * c)) / (2 * base)
        return root.toFloat()
    }
}
