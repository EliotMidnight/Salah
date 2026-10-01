package com.example.ui.quran.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranReadingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Magnification, and the drag it takes away from scrolling.
 *
 * ### The defect
 *
 * The reader attached its pinch handler to the **whole reading surface** and
 * re-keyed the `pointerInput` on `isMagnified`, so that `panZoomLock` would flip
 * when the view crossed 1x. Two things went wrong, and they are related:
 *
 * - **Re-keying tears the gesture down.** The frame the scale crosses 1x destroys
 *   and rebuilds the detector, which discards the accumulated zoom *mid-pinch*.
 *   A reader pinching out from 1x to 2x lands part of the way there and the
 *   gesture appears to stop working.
 * - **The focal correction was fed the centroid unconditionally.** The detector
 *   reports a centroid for a one-finger drag too, so on a magnified view a scroll
 *   was added to the pan and the page slid away from the reader's own finger.
 *
 * Both are fixed by making the state a field on [ReaderPosition] and reading it
 * inside a stable detector, which is what is testable here.
 */
class ReaderMagnificationTest {

    private val surface = IntSize(width = 1080, height = 1920)

    private fun position() = ReaderPosition(com.example.data.model.QuranRef.Start)

    // --- Magnifying ------------------------------------------------------

    @Test
    fun `magnifying is bounded by the reader's own range`() {
        val start = position()
        // Far past the maximum, in both directions.
        repeat(40) { start.magnifyBy(1.2f) }
        assertEquals(
            QuranReadingOptions.ViewScaleRange.endInclusive,
            start.viewScale,
            0.0001f
        )
        repeat(80) { start.magnifyBy(0.8f) }
        assertEquals(1f, start.viewScale, 0.0001f)
    }

    @Test
    fun `a wobbling pinch does not magnify at all`() {
        val start = position()
        listOf(1.0009f, 0.9994f, 1.0002f, 0.9998f, 1.0001f).forEach { start.magnifyBy(it) }
        assertEquals("a sub-percent wobble magnified the view", 1f, start.viewScale, 0f)
    }

    @Test
    fun `a real pinch magnifies`() {
        val start = position()
        start.magnifyBy(1.1f)
        assertTrue("a 10% pinch did nothing", start.viewScale > 1.05f)
    }

    @Test
    fun `a sequence of relative events compounds`() {
        val start = position()
        repeat(6) { start.magnifyBy(1.05f) }
        assertEquals(1.05f.pow(6), start.viewScale, 0.001f)
    }

    // --- The magnification switch ---------------------------------------

    @Test
    fun `a view at 1x does not claim a one-finger drag`() {
        // This is the switch that decides whether a scroll is a scroll. If a
        // surface at 1x claimed drags, ordinary reading would be broken in every
        // layout at once.
        val start = position()
        assertFalse(start.isViewMagnified)
    }

    @Test
    fun `a wobble just past 1x does not count as magnified`() {
        // A pinch landing on 1.004 is a wobble, not a magnification. Treating it as
        // one steals the reader's scroll for a gesture they did not intend.
        val start = position()
        start.magnifyBy(1.003f)
        assertFalse("1.003 must not count as magnified", start.isViewMagnified)
    }

    @Test
    fun `a real magnification does claim a one-finger drag`() {
        // A magnified view that cannot be moved is a view the reader has cropped
        // themselves out of, and the only way back would be to zoom out again.
        val start = position()
        start.magnifyBy(1.3f)
        assertTrue(start.isViewMagnified)
    }

    // --- Panning ---------------------------------------------------------

    @Test
    fun `panning at 1x does nothing`() {
        val start = position()
        start.panBy(Offset(400f, 400f))
        assertEquals(Offset.Zero, start.appliedPan)
    }

    @Test
    fun `panning a magnified view moves it`() {
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(2f)
        start.panBy(Offset(100f, 50f))
        assertTrue("the view did not move", start.appliedPan.x > 0f)
    }

    @Test
    fun `a pan is clamped to what the magnification can reveal`() {
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(2f)
        start.panBy(Offset(99_999f, 99_999f))
        assertEquals(540f, start.appliedPan.x, 0.5f)
        assertEquals(960f, start.appliedPan.y, 0.5f)
    }

    @Test
    fun `the pan and the pinch correction are clamped together`() {
        // The two are added and then clamped as one. Clamped separately, a pinch at
        // the far corner at full magnification asks for a correction the size of the
        // whole surplus, and adding that to an already-clamped pan drags the reading
        // past its own content and leaves blank paper with no way back.
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(2f)
        start.panBy(Offset(500f, 900f))
        start.pinchBy(Offset(500f, 900f))
        assertTrue(
            "the combined offset exceeded the surplus",
            start.appliedPan.x <= 540.5f && start.appliedPan.y <= 960.5f
        )
    }

    @Test
    fun `a corner pinch alone never exceeds the surplus`() {
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(QuranReadingOptions.ViewScaleRange.endInclusive)
        start.pinchBy(Offset(99_999f, 99_999f))
        val max = 1080f * ((QuranReadingOptions.ViewScaleRange.endInclusive - 1f) / 2f)
        assertTrue("the correction exceeded the surplus", start.appliedPan.x <= max + 0.5f)
    }

    // --- Coming back -----------------------------------------------------

    @Test
    fun `returning to 1x re-centres the view`() {
        // At 1x the view fills the screen, so any leftover offset would leave a
        // strip of blank paper beside the text and the reader would have no way to
        // know why.
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(2f)
        start.panBy(Offset(300f, 300f))
        assertTrue("the view did not move first", start.appliedPan != Offset.Zero)

        start.magnifyBy(0.4f)
        assertEquals(1f, start.viewScale, 0.0001f)
        assertEquals(
            "a view back at 1x must be re-centred, not left shoved aside",
            Offset.Zero,
            start.appliedPan
        )
    }

    @Test
    fun `a layout change resets magnification`() {
        val start = position()
        start.surfaceMeasured(surface)
        start.magnifyBy(2f)
        start.panBy(Offset(200f, 200f))

        start.resetMagnification()
        assertEquals(1f, start.viewScale, 0.0001f)
        assertEquals(Offset.Zero, start.appliedPan)
    }

    @Test
    fun `resetting magnification does not move the reading`() {
        // Magnification belongs to the *view*, not to the reading. Losing the page
        // because a pinch was reset would be the two being confused.
        val start = ReaderPosition(com.example.data.model.QuranRef(2, 255, 42))
        start.magnifyBy(2f)
        start.resetMagnification()
        assertEquals(2, start.ref.surah)
        assertEquals(255, start.ref.ayah)
        assertEquals(42, start.ref.page)
    }

    @Test
    fun `a pinch on an unmeasured surface does not produce a NaN offset`() {
        // Evaluated on the first frame, before the first layout pass. A NaN here
        // would poison the graphics layer and make the text disappear.
        val start = position()
        start.magnifyBy(2f)
        start.pinchBy(Offset(50f, 50f))
        assertTrue(start.appliedPan.x.isFinite() && start.appliedPan.y.isFinite())
        assertEquals(Offset.Zero, start.appliedPan)
    }

    private infix fun Float.pow(exponent: Int): Float {
        var result = 1f
        repeat(exponent) { result *= this }
        return result
    }
}
