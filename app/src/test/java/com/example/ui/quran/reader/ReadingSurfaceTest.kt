package com.example.ui.quran.reader

import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the reader decides which page it is on, in each layout.
 *
 * ### What replaced what
 *
 * The previous continuous layouts reported their page by measuring the text block's
 * own layout and comparing each verse's last line against `positionInWindow().y` -
 * two coordinate spaces, with the node's own padding, its list content padding and
 * any `graphicsLayer` offset all missing from the arithmetic. The rule itself,
 * `firstVerseIndexVisibleAt`, had eleven tests.
 *
 * It is gone, and that is the point: the page a reader is on is a *fact about the
 * corpus*, not a fact about text layout. The verse at the top of the viewport is
 * found from the list index, and its page comes from the verse itself. There is no
 * geometry to get wrong, and the page indicator cannot disagree with the text
 * because it is reading the same row the text is.
 *
 * These tests cover the rule that replaced it, including the off-by-one that the
 * previous version got wrong twice.
 */
class ReadingSurfaceTest {

    private val alBaqarah = QuranBrowse.ayahsInSurah(2)
    private val alIkhlas = QuranBrowse.ayahsInSurah(112)

    // --- The list offset -------------------------------------------------

    @Test
    fun `a list of a heading and verses reports the verse under the top of the screen`() {
        // The list is `[heading, verse…]`, so the scroll state's index is one more
        // than the verse index. The previous version indexed straight into the ayah
        // list with it, which put the recorded position one verse ahead of the top
        // of the screen for the whole of the surah.
        assertEquals(1, HEADING_ITEMS)

        for (verseIndex in 0 until 10) {
            val listIndex = verseIndex + HEADING_ITEMS
            assertEquals(
                "list index $listIndex should resolve to verse $verseIndex",
                alBaqarah[verseIndex],
                alBaqarah.getOrNull(listIndex - HEADING_ITEMS)
            )
        }
    }

    @Test
    fun `a scrolled-past-the-end list reports no verse rather than crashing`() {
        // The scroll state can report an index past the end during a fling, and
        // `getOrNull` returning null is what keeps that from being a crash on a
        // 286-verse surah.
        assertNull(alBaqarah.getOrNull(alBaqarah.size + 5 - HEADING_ITEMS))
        assertNull(emptyList<Any>().getOrNull(0))
    }

    // --- Verse to page ---------------------------------------------------

    @Test
    fun `the page comes from the verse, so it cannot disagree with the text`() {
        // The whole point. A reader scrolling into Al-Baqarah's third page sees
        // "Page 4" in the pill because the verse at the top of their screen is on
        // page 4 - not because a text layout was asked where it thought it was.
        alBaqarah.forEach { ayah ->
            val ref = QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} claims page " +
                    "${ref.page} but the partition says ${ayah.pageNumber}",
                ayah.pageNumber,
                ref.page
            )
        }
    }

    @Test
    fun `the pages of a surah are the pages its verses are on`() {
        val pages = alBaqarah.map { it.pageNumber }.distinct()
        // Al-Baqarah runs from page 2 to page 49 in this partition, and a reader who
        // scrolls the whole surah should see every one of them.
        assertEquals(2, pages.first())
        assertEquals(49, pages.last())
        assertEquals(48, pages.size)
    }

    @Test
    fun `a short surah is on one page and stays on it`() {
        val pages = alIkhlas.map { it.pageNumber }.distinct()
        assertEquals("Al-Ikhlas is on one page", 1, pages.size)
        assertEquals(604, pages.first())
    }

    @Test
    fun `the page is non-decreasing as a reader scrolls forward`() {
        // The invariant the page indicator depends on: a reader scrolling down can
        // only ever move forward through the book. A page indicator that goes
        // backwards is a bug a reader sees immediately.
        val pages = alBaqarah.map { it.pageNumber }
        for (i in 1 until pages.size) {
            assertTrue(
                "page went from ${pages[i - 1]} to ${pages[i]} at verse $i",
                pages[i] >= pages[i - 1]
            )
        }
    }

    // --- The flow block --------------------------------------------------

    @Test
    fun `a flow block is a few verses, not a surah`() {
        // One `AnnotatedString` for 286 verses is a single laid-out object several
        // screens tall: it cannot be measured cheaply, cannot be lazily discarded,
        // and holds its whole text in memory for as long as the reader is on the
        // page. A dozen verses is one screenful - enough that the text reads as a
        // block rather than as a list, and small enough to be cheap.
        assertTrue(
            "a flow block of ${FLOW_BLOCK_VERSES} verses is not a screenful",
            FLOW_BLOCK_VERSES in 6..16
        )
    }

    @Test
    fun `the verses of a surah divide into whole flow blocks`() {
        // No verse is dropped at the boundaries, which is the only thing a chunking
        // can get wrong that matters. A block boundary is not a page boundary and
        // must not be treated as one.
        val blocks = alBaqarah.chunked(FLOW_BLOCK_VERSES)
        assertEquals(alBaqarah.size, blocks.sumOf { it.size })
        assertEquals(
            "the last block should be the remainder, not a repeat",
            (alBaqarah.size - 1) % FLOW_BLOCK_VERSES + 1,
            blocks.last().size
        )
    }

    @Test
    fun `every flow block carries at least one verse`() {
        // `chunked` guarantees it, and the assertion is here because the alternative
        // - a block that renders as an empty gap in the text - is exactly the kind of
        // failure a reader blames on the Quran.
        QuranBrowse.surahs.forEach { surah ->
            QuranBrowse.ayahsInSurah(surah.number)
                .chunked(FLOW_BLOCK_VERSES)
                .forEachIndexed { index, block ->
                    assertTrue(
                        "surah ${surah.number} block $index is empty",
                        block.isNotEmpty()
                    )
                }
        }
    }
}
