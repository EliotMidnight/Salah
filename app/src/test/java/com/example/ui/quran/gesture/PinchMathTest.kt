package com.example.ui.quran.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranReadingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Magnifying a page, and moving it afterwards.
 *
 * ### Why the old test file was not enough
 *
 * `ReaderPanTest` covered the clamp and the focal correction, and both of those
 * were correct. What it could not see was the two things that actually made the
 * gesture feel broken in the hand:
 *
 * 1. **Every event in a pinch multiplied by a stale scale.** The zoom was applied
 *    as `current * zoomChange` where `current` was captured when the gesture
 *    detector was built, so a ten-finger splay landed about where a two-finger one
 *    did. Re-keying the detector on the scale fixed the staleness and tore the
 *    gesture down mid-pinch, dropping the accumulated zoom. There was no test
 *    because the bug was in *when* the value was read, not in what the arithmetic
 *    did - so the arithmetic is now behind [PinchMath.applyZoom] and a sequence is
 *    testable.
 * 2. **Sub-percent wobble wrote the reader's stored preference.** A real pinch
 *    jitters by a fraction of a percent on every event, and each of those was a
 *    write to a preference the reader only meant to change deliberately.
 */
class PinchMathTest {

    private val screen = IntSize(width = 1080, height = 1920)

    // --- Zoom ------------------------------------------------------------

    @Test
    fun `a sequence of relative events compounds`() {
        // The property the stale-capture bug broke. Ten events of 1.05 from 1.0 is
        // 1.05^10, and a reader who splays their fingers should see that much.
        var scale = 1f
        repeat(10) { scale = PinchMath.applyZoom(scale, 1.05f) }
        assertEquals(1.05f.pow(10), scale, 0.001f)
    }

    @Test
    fun `a one-finger-sized splay and a ten-finger splay are told apart`() {
        // The exact failure: both multiply the same stale base, so they land in the
        // same place and a deliberate gesture is indistinguishable from a nudge.
        var fromTwo = 1f
        repeat(2) { fromTwo = PinchMath.applyZoom(fromTwo, 1.05f) }

        var fromTen = 1f
        repeat(10) { fromTen = PinchMath.applyZoom(fromTen, 1.05f) }

        assertTrue(
            "a ten-event splay ($fromTen) must exceed a two-event one ($fromTwo)",
            fromTen > fromTwo * 1.3f
        )
    }

    @Test
    fun `a pinch that wobbles below the dead zone does not move at all`() {
        // Every real pinch jitters. Without a dead zone each of those writes the
        // reader's stored text size, and a reader who pinched to look closer finds
        // their preference has drifted when they let go.
        val start = 1.4f
        var scale = start
        listOf(1.0009f, 0.9994f, 1.0002f, 0.9998f, 1.0001f).forEach {
            scale = PinchMath.applyZoom(scale, it)
        }
        assertEquals("a sub-percent wobble changed the scale", start, scale, 0f)
    }

    @Test
    fun `a real zoom clears the dead zone`() {
        var scale = 1f
        listOf(1.0009f, 1.02f, 0.9994f).forEach { scale = PinchMath.applyZoom(scale, it) }
        assertTrue("a 2% move is a real gesture and must be applied", scale > 1.01f)
    }

    @Test
    fun `a nonsensical zoom is ignored rather than applied`() {
        // `detectTransformGestures` can report a zero or negative change when a
        // pointer goes down or up mid-gesture, and multiplying by zero would
        // collapse the page to nothing.
        val start = 1.5f
        assertEquals(start, PinchMath.applyZoom(start, 0f), 0f)
        assertEquals(start, PinchMath.applyZoom(start, -1.2f), 0f)
        assertEquals(start, PinchMath.applyZoom(start, Float.NaN), 0f)
        assertTrue(
            "an infinite change must be refused",
            PinchMath.applyZoom(start, Float.POSITIVE_INFINITY).isFinite()
        )
    }

    @Test
    fun `zoom can go down as well as up`() {
        var scale = 2f
        scale = PinchMath.applyZoom(scale, 0.5f)
        assertEquals(1f, scale, 0.0001f)
    }

    // --- Surplus and the pan clamp ---------------------------------------

    @Test
    fun `at 1x the view is pinned to centre`() {
        // A paged view at 1x fills the screen, so any offset would show a strip of
        // empty paper beside it, with no way back except zooming out again.
        val clamped = PinchMath.clampPan(Offset(400f, -900f), scale = 1f, screen)
        assertEquals(0f, clamped.x, 0f)
        assertEquals(0f, clamped.y, 0f)
    }

    @Test
    fun `the surplus is the scaled half-size because a layer scales about the centre`() {
        // At 2x on a 1080px surface the content is 2160px wide, so there are 540px
        // of surplus on each axis to reach.
        val clamped = PinchMath.clampPan(Offset(10_000f, 10_000f), scale = 2f, screen)
        assertEquals(540f, clamped.x, 0.5f)
        assertEquals(960f, clamped.y, 0.5f)
    }

    @Test
    fun `a pan within bounds is left alone`() {
        val clamped = PinchMath.clampPan(Offset(-120f, 240f), scale = 2f, screen)
        assertEquals(-120f, clamped.x, 0f)
        assertEquals(240f, clamped.y, 0f)
    }

    @Test
    fun `the clamp is symmetric in both directions`() {
        val right = PinchMath.clampPan(Offset(9999f, 0f), 1.5f, screen)
        val left = PinchMath.clampPan(Offset(-9999f, 0f), 1.5f, screen)
        assertEquals(-left.x, right.x, 0.01f)
    }

    @Test
    fun `an unmeasured surface clamps to centre rather than to NaN`() {
        // Before the first layout pass the size is zero, and this is evaluated on
        // the very first frame. A negative coercion bound there would throw.
        val clamped = PinchMath.clampPan(Offset(50f, 50f), scale = 2f, IntSize.Zero)
        assertEquals(0f, clamped.x, 0f)
        assertEquals(0f, clamped.y, 0f)
    }

    @Test
    fun `the largest allowed scale still keeps the page reachable`() {
        // A magnified view that cannot be moved is a view the reader has cropped
        // themselves out of, and the only way back would be to zoom out again.
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val clamped = PinchMath.clampPan(
            Offset(Float.MAX_VALUE, Float.MAX_VALUE),
            maxScale,
            screen
        )
        assertTrue("the pan must stay finite", clamped.x.isFinite() && clamped.y.isFinite())
        assertTrue(
            "at max zoom there must still be room to move the page",
            clamped.x > 0f && clamped.y > 0f
        )
    }

    @Test
    fun `the surplus grows monotonically with the scale`() {
        var previous = 0f
        var scale = 1f
        while (scale <= QuranReadingOptions.ViewScaleRange.endInclusive) {
            val surplus = PinchMath.surplus(scale, screen)
            assertTrue(
                "the surplus shrank going from $previous to $scale",
                surplus.x >= previous
            )
            previous = surplus.x
            scale += 0.1f
        }
    }

    // --- Zooming about the fingers ---------------------------------------

    @Test
    fun `a pinch at the centre needs no correction`() {
        val centre = Offset(screen.width / 2f, screen.height / 2f)
        val correction = PinchMath.focalCorrection(centre, scale = 2f, screen)
        assertEquals(0f, correction.x, 0.001f)
        assertEquals(0f, correction.y, 0.001f)
    }

    @Test
    fun `a pinch below centre pulls the text up so the pinched point stays put`() {
        // Fingers in the lower half: magnifying about the centre pushes the lower
        // content further down and out of view, and the correction moves it back.
        val centroid = Offset(screen.width / 2f, screen.height * 0.75f)
        val scale = 2f
        val correction = PinchMath.focalCorrection(centroid, scale, screen)

        val expected = -(screen.height * 0.25f) * (scale - 1f)
        assertEquals(expected, correction.y, 0.5f)
        assertEquals(0f, correction.x, 0.001f)
        assertTrue("text pinched low must be corrected upward", correction.y < 0f)
    }

    @Test
    fun `the correction is symmetric about the centre`() {
        val above = PinchMath.focalCorrection(Offset(540f, 480f), 2f, screen)
        val below = PinchMath.focalCorrection(Offset(540f, 1440f), 2f, screen)
        assertEquals(-below.y, above.y, 0.5f)

        val left = PinchMath.focalCorrection(Offset(270f, 960f), 2f, screen)
        val right = PinchMath.focalCorrection(Offset(810f, 960f), 2f, screen)
        assertEquals(-right.x, left.x, 0.5f)
    }

    @Test
    fun `there is no correction at 1x or below`() {
        // Nothing has been displaced at 1x, so a correction would itself be a jump.
        val corner = Offset(0f, 0f)
        assertEquals(Offset.Zero, PinchMath.focalCorrection(corner, 1f, screen))
        assertEquals(Offset.Zero, PinchMath.focalCorrection(corner, 0.5f, screen))
    }

    @Test
    fun `an unmeasured surface has no correction rather than NaN`() {
        // Evaluated on the very first frame. A NaN here would poison the graphics
        // layer and make the text disappear.
        val correction = PinchMath.focalCorrection(Offset(100f, 100f), 2f, IntSize.Zero)
        assertTrue(correction.x.isFinite() && correction.y.isFinite())
        assertEquals(Offset.Zero, correction)
    }

    @Test
    fun `a corner pinch at max zoom never exceeds the visible surplus`() {
        // A pinch at the very corner at maximum zoom asks for a correction the size
        // of the whole surplus. Added to a pan at the opposite corner it would go
        // past what the surface can reveal - which is why the reader clamps the
        // two together rather than adding them separately.
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val corner = PinchMath.focalCorrection(Offset(0f, 0f), maxScale, screen)
        val surplus = PinchMath.clampPan(Offset(Float.MAX_VALUE, Float.MAX_VALUE), maxScale, screen)

        val combined = PinchMath.clampPan(
            corner + Offset(surplus.x, surplus.y),
            maxScale,
            screen
        )
        assertEquals(surplus.x, combined.x, 0.5f)
        assertEquals(surplus.y, combined.y, 0.5f)
        assertTrue(
            "the surface must never be dragged past its own content",
            combined.x <= surplus.x + 0.5f && combined.y <= surplus.y + 0.5f
        )
    }

    @Test
    fun `a bottom-edge pinch reaches the bottom of the page exactly`() {
        // A reader's escape from a page too tall for the viewport is to magnify and
        // pan, so the bottom has to actually be reachable: enough to reveal the last
        // line, not so much that the top vanishes.
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val surplus = PinchMath.clampPan(Offset(Float.MAX_VALUE, Float.MAX_VALUE), maxScale, screen)
        val atBottom = PinchMath.focalCorrection(Offset(540f, screen.height.toFloat()), maxScale, screen)
        val combined = PinchMath.clampPan(atBottom, maxScale, screen)

        assertEquals(-surplus.y, combined.y, 0.5f)
        assertTrue("a bottom-edge pinch must pull the surface up", combined.y < 0f)
        assertTrue(
            "and it must not drag past what the surface can reveal",
            combined.y >= -surplus.y - 0.5f
        )
    }

    /** Local power, so the test does not import kotlin.math for one call. */
    private infix fun Float.pow(exponent: Int): Float {
        var result = 1f
        repeat(exponent) { result *= this }
        return result
    }
}
