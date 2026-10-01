package com.example.ui.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which page the reader is actually on.
 *
 * ### Why this is worth testing
 *
 * A flowing surah is one laid-out text block, so there is no list index to ask
 * "where am I". The position has to come from the text layout, and when that is
 * wrong the symptom is not a crash - it is a page indicator that quietly
 * disagrees with the text underneath it. That kind of bug survives review,
 * ships, and gets believed to be a feature.
 *
 * [firstVerseIndexVisibleAt] is the rule the layout feeds, kept free of
 * `TextLayoutResult` so it can be checked directly. Building a real text layout
 * is not possible outside the framework, and a test that needs one would be a
 * test that gets skipped.
 */
class VerseVisibilityTest {

    /**
     * Six verses, one line each, 20px apart - so "the top of the window is here"
     * maps to an obvious answer.
     */
    private val bottoms = listOf(20f, 40f, 60f, 80f, 100f, 120f)
    private val verseCount = 6

    @Test
    fun `window at the top reports the first verse`() {
        assertEquals(0, firstVerseIndexVisibleAt(bottoms, windowTopY = 0f, verseCount = verseCount))
    }

    @Test
    fun `window part way down reports the verse whose line is below it`() {
        // Top at 60: verse 0's last line (20) has gone, verse 1's (40) has gone,
        // verse 2's (60) is exactly on the edge and still visible.
        assertEquals(2, firstVerseIndexVisibleAt(bottoms, windowTopY = 60f, verseCount = verseCount))
    }

    @Test
    fun `a verse whose last line sits exactly on the top edge is still counted`() {
        // `>=` rather than `>`. If this were `>`, the indicator would jump a verse
        // ahead at the precise moment each verse finished scrolling off.
        assertEquals(1, firstVerseIndexVisibleAt(bottoms, windowTopY = 40f, verseCount = verseCount))
        assertEquals(0, firstVerseIndexVisibleAt(bottoms, windowTopY = 20f, verseCount = verseCount))
    }

    @Test
    fun `the reported verse only ever moves forward as the window moves down`() {
        var previous = -1
        listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f, 105f, 120f).forEach { top ->
            val index = firstVerseIndexVisibleAt(bottoms, windowTopY = top, verseCount = verseCount)
            assertTrue(
                "position went backwards at windowTopY=$top (was $previous, now $index)",
                index >= previous
            )
            previous = index
        }
    }

    @Test
    fun `scrolled past the end it reports the last verse rather than nothing`() {
        // A blank indicator at the end of a surah reads as a broken one. The
        // last verse is the honest answer.
        assertEquals(
            verseCount - 1,
            firstVerseIndexVisibleAt(bottoms, windowTopY = 100_000f, verseCount = verseCount)
        )
    }

    @Test
    fun `an empty block has no verse to report`() {
        assertEquals(-1, firstVerseIndexVisibleAt(emptyList(), windowTopY = 0f, verseCount = 0))
        assertEquals(
            -1,
            firstVerseIndexVisibleAt(bottoms, windowTopY = 0f, verseCount = 0)
        )
    }

    @Test
    fun `a single verse is reported at any scroll position`() {
        assertEquals(0, firstVerseIndexVisibleAt(listOf(50f), windowTopY = 0f, verseCount = 1))
        assertEquals(0, firstVerseIndexVisibleAt(listOf(50f), windowTopY = 50f, verseCount = 1))
        assertEquals(0, firstVerseIndexVisibleAt(listOf(50f), windowTopY = 9_999f, verseCount = 1))
    }

    @Test
    fun `more line bottoms than verses does not read past the verse count`() {
        // Defensive: a layout and a verse list that disagree should report the
        // last *verse*, not index past the end of it.
        assertEquals(
            1,
            firstVerseIndexVisibleAt(listOf(10f, 20f, 30f, 40f), windowTopY = 99_999f, verseCount = 2)
        )
    }

    @Test
    fun `every position is covered by some verse`() {
        // The invariant the pill depends on: for any scroll position, the rule
        // yields a verse in range. A gap here is a blank indicator.
        (0..300).forEach { step ->
            val index = firstVerseIndexVisibleAt(bottoms, windowTopY = step.toFloat(), verseCount = verseCount)
            assertTrue("no verse reported at windowTopY=$step", index in 0 until verseCount)
        }
    }
}
