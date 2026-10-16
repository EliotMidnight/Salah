package com.example.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Today headline is drawn at a size measured from the word, not at 58sp flat.
 *
 * ### Why this is not a screenshot
 *
 * The defect is a word too wide for its row, and Robolectric does not lay text out
 * with a device's metrics - a screenshot of the headline there would record whatever
 * the host happens to measure, which is not what a reader sees. The claim here is the
 * arithmetic that turns two widths into a type size; the pixels are checked on a
 * device, and [HeadlineFit.size] is the only place that arithmetic is decided.
 */
class HeadlineFitTest {

    private companion object {
        /** A 360dp phone, less the page's own gutter: the row the headline has. */
        const val NARROW_ROW_PX = 328 * 3
    }

    @Test
    fun `a word that fits is drawn at the ceiling`() {
        assertEquals(
            HeadlineFit.CEILING_SP,
            HeadlineFit.size(measuredWidthPx = 200, availableWidthPx = NARROW_ROW_PX),
            0.001f
        )
    }

    @Test
    fun `a word exactly as wide as its row is still drawn at the ceiling`() {
        assertEquals(
            HeadlineFit.CEILING_SP,
            HeadlineFit.size(measuredWidthPx = NARROW_ROW_PX, availableWidthPx = NARROW_ROW_PX),
            0.001f
        )
    }

    @Test
    fun `a word wider than its row is stepped down to fit it`() {
        // A row of 328dp and a word of 464dp is French "Lever du soleil" at the
        // ceiling: the size it needs is 58 x 328/464, about 41sp. A fixed 58sp put
        // the last two words of the name off the row.
        val size = HeadlineFit.size(measuredWidthPx = 464 * 3, availableWidthPx = NARROW_ROW_PX)

        assertTrue("expected a size below the ceiling, was $size", size < HeadlineFit.CEILING_SP)
        assertEquals(58f * 328f / 464f, size, 0.25f)
    }

    @Test
    fun `a word far too wide for its row stops at the floor rather than shrinking away`() {
        assertEquals(
            HeadlineFit.FLOOR_SP,
            HeadlineFit.size(measuredWidthPx = NARROW_ROW_PX * 6, availableWidthPx = NARROW_ROW_PX),
            0.001f
        )
    }

    @Test
    fun `no measurement yet is answered with the ceiling, not with a shrink`() {
        // The first composition has no layout and a zero width. Reading that as "the
        // word is too wide" would flash the headline at the floor on every open.
        assertEquals(
            HeadlineFit.CEILING_SP,
            HeadlineFit.size(measuredWidthPx = 0, availableWidthPx = 0),
            0.001f
        )
        assertEquals(
            HeadlineFit.CEILING_SP,
            HeadlineFit.size(measuredWidthPx = 0, availableWidthPx = NARROW_ROW_PX),
            0.001f
        )
    }

    @Test
    fun `every measured width between nothing and far too wide lands inside the range`() {
        // The property over the whole sweep, not the two examples above: whatever the
        // word measures, the size drawn is at least legible, never larger than the
        // design, and never larger than the row can hold.
        val row = NARROW_ROW_PX.toFloat()
        var measured = 1
        while (measured <= NARROW_ROW_PX * 8) {
            val size = HeadlineFit.size(measured, NARROW_ROW_PX)

            assertTrue("$measured px landed at $size, past the ceiling", size <= HeadlineFit.CEILING_SP)
            assertTrue("$measured px landed at $size, under the floor", size >= HeadlineFit.FLOOR_SP)
            if (measured <= NARROW_ROW_PX.toFloat()) {
                assertEquals(
                    "a word that fits was shrunk at $measured px",
                    HeadlineFit.CEILING_SP,
                    size,
                    0.001f
                )
            }
            // While the floor is not doing the work, the word fits: the size is high
            // enough that the widest the word was measured at cannot exceed the row.
            val widestAtSize = measured * (size / HeadlineFit.CEILING_SP)
            assertTrue(
                "at $measured px the drawn size $size would still overflow by " +
                    "${widestAtSize - row} px",
                size <= HeadlineFit.FLOOR_SP + 0.001f || widestAtSize <= row + 1f
            )
            measured += 37
        }
    }

    @Test
    fun `the line box is taller than the ceiling type, so the headline cannot clip itself`() {
        assertTrue(HeadlineFit.LINE_HEIGHT_SP > HeadlineFit.CEILING_SP)
        assertTrue(HeadlineFit.FLOOR_SP > 0f)
    }
}
