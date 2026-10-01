package com.example.ui.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranReadingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Panning a magnified view.
 *
 * The clamp is the whole of it, and it is arithmetic rather than layout, so it
 * is pulled out of the composable and checked here. Getting it wrong is
 * invisible in a screenshot and maddening in the hand: too loose and the reader
 * drags the page into empty space with no way to tell which bit is text, too
 * tight and a magnified page cannot be moved at all.
 */
class ReaderPanTest {

    private val screen = IntSize(width = 1080, height = 1920)

    @Test
    fun `at 1x the view is pinned to centre`() {
        // A paged view at 1x fills the screen, so any offset would show a strip
        // of empty paper beside it with no way back except zooming out again.
        val clamped = clampPan(Offset(400f, -900f), scale = 1f, containerSize = screen)
        assertEquals(0f, clamped.x, 0f)
        assertEquals(0f, clamped.y, 0f)
    }

    @Test
    fun `the surplus is the scaled half-size, because graphicsLayer scales about the centre`() {
        // At 2x on a 1080px-wide surface the content is 2160px wide, so there
        // are 540px of surplus on each side to reach.
        val clamped = clampPan(Offset(10_000f, 10_000f), scale = 2f, containerSize = screen)
        assertEquals(540f, clamped.x, 0.5f)
        assertEquals(960f, clamped.y, 0.5f)
    }

    @Test
    fun `a pan within bounds is left alone`() {
        val clamped = clampPan(Offset(-120f, 240f), scale = 2f, containerSize = screen)
        assertEquals(-120f, clamped.x, 0f)
        assertEquals(240f, clamped.y, 0f)
    }

    @Test
    fun `clamps are symmetric in both directions`() {
        val right = clampPan(Offset(9999f, 0f), scale = 1.5f, containerSize = screen)
        val left = clampPan(Offset(-9999f, 0f), scale = 1.5f, containerSize = screen)
        assertEquals(-left.x, right.x, 0.01f)
    }

    @Test
    fun `an unmeasured surface clamps to centre rather than to NaN`() {
        // Before the first layout pass the size is zero. A negative coercion
        // bound there would throw, and this is hit on the very first frame.
        val clamped = clampPan(Offset(50f, 50f), scale = 2f, containerSize = IntSize.Zero)
        assertEquals(0f, clamped.x, 0f)
        assertEquals(0f, clamped.y, 0f)
    }

    @Test
    fun `the largest allowed scale still keeps the page reachable`() {
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val clamped = clampPan(Offset(Float.MAX_VALUE, Float.MAX_VALUE), scale = maxScale, containerSize = screen)
        assertTrue("the pan must stay finite", clamped.x.isFinite() && clamped.y.isFinite())
        assertTrue(
            "at max zoom there must still be room to move the page",
            clamped.x > 0f && clamped.y > 0f
        )
    }

    // --- Zooming about the fingers, not about the middle of the page ------
    //
    // `graphicsLayer` scales about the centre of the surface. Pinching anywhere
    // else therefore moves the text as well as magnifying it, and the reader is
    // left looking at a different part of the page than the one under their
    // fingers. The correction below undoes that displacement.

    @Test
    fun `a pinch at the centre needs no correction`() {
        val centre = Offset(screen.width / 2f, screen.height / 2f)
        val correction = focalCorrection(centre, scale = 2f, containerSize = screen)
        assertEquals(0f, correction.x, 0.001f)
        assertEquals(0f, correction.y, 0.001f)
    }

    @Test
    fun `a pinch below centre pulls the text up so the pinched point stays put`() {
        // Fingers in the lower half. Magnifying about the centre pushes the
        // lower content further down, out of view; the correction moves it back
        // up by the amount the scale added.
        val centroid = Offset(screen.width / 2f, screen.height * 0.75f)
        val scale = 2f
        val correction = focalCorrection(centroid, scale, screen)

        // Surplus below centre after magnifying: (h/2) * (scale - 1).
        val expected = -(screen.height * 0.25f) * (scale - 1f)
        assertEquals(expected, correction.y, 0.5f)
        assertEquals(0f, correction.x, 0.001f)
        assertTrue(
            "text pinched low must be corrected upward, not further down",
            correction.y < 0f
        )
    }

    @Test
    fun `the correction is symmetric about the centre`() {
        val above = focalCorrection(Offset(540f, 480f), 2f, screen)
        val below = focalCorrection(Offset(540f, 1440f), 2f, screen)
        assertEquals(-below.y, above.y, 0.5f)

        val left = focalCorrection(Offset(270f, 960f), 2f, screen)
        val right = focalCorrection(Offset(810f, 960f), 2f, screen)
        assertEquals(-right.x, left.x, 0.5f)
    }

    @Test
    fun `there is no correction at 1x or below`() {
        // At 1x nothing has been displaced, so a correction would be a jump.
        val corner = Offset(0f, 0f)
        assertEquals(Offset.Zero, focalCorrection(corner, 1f, screen))
        assertEquals(Offset.Zero, focalCorrection(corner, 0.5f, screen))
    }

    @Test
    fun `an unmeasured surface has no correction rather than NaN`() {
        // Before the first layout pass the size is zero, and this is evaluated on
        // the very first frame. A NaN here would poison the graphics layer and
        // make the text disappear.
        val correction = focalCorrection(Offset(100f, 100f), 2f, IntSize.Zero)
        assertTrue(correction.x.isFinite() && correction.y.isFinite())
        assertEquals(Offset.Zero, correction)
    }

    @Test
    fun `the correction never exceeds the visible surplus`() {
        // A pinch at the very corner at maximum zoom asks for a correction the
        // size of the whole surplus. Combined with a pan to the opposite corner
        // it would exceed what the surface can reveal, which is why the reader
        // clamps the two together rather than adding them separately.
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val corner = focalCorrection(Offset(0f, 0f), maxScale, screen)
        val surplus = clampPan(Offset(Float.MAX_VALUE, Float.MAX_VALUE), maxScale, screen)

        val combined = clampPan(
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
    fun `a pinch at the bottom edge reaches the bottom of the page exactly`() {
        // The reader's escape hatch for a page too tall for the viewport is to
        // magnify and pan, so the bottom of the page has to actually be
        // reachable. Pinching with the fingers at the very bottom edge should
        // ask for exactly the surplus that reveals it - no more, so the top of
        // the page does not vanish, and no less, so the last line stays out of
        // reach.
        val maxScale = QuranReadingOptions.ViewScaleRange.endInclusive
        val surplus = clampPan(Offset(Float.MAX_VALUE, Float.MAX_VALUE), maxScale, screen)

        val atBottom = focalCorrection(Offset(540f, screen.height.toFloat()), maxScale, screen)
        val combined = clampPan(atBottom, maxScale, screen)

        assertEquals(-surplus.y, combined.y, 0.5f)
        assertEquals(
            "a bottom-edge pinch must pull the surface up, not down",
            true,
            combined.y < 0f
        )
        assertTrue(
            "and it must not drag past what the surface can reveal",
            combined.y >= -surplus.y - 0.5f
        )
    }
}
