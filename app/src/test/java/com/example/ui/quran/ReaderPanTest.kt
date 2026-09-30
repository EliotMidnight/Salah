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
}
