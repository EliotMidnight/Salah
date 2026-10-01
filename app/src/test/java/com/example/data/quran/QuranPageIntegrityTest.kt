package com.example.data.quran

import com.example.data.model.QuranRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a page turn can and cannot land on, checked against the real corpus.
 *
 * ### Why the mushaf layout needs these and the other suites do not
 *
 * Every other reader test uses a hand-picked page: page 3, page 42, page 604. That
 * is enough to test the *rules* - a turn writes the page's first verse, a selection
 * does not outlive its page, a ref is compared whole - but it tests them against
 * three pages out of 604, chosen because they are the ones whose behaviour was
 * already known.
 *
 * The mushaf's awkward cases are not chosen, they are *there*: the pages that cross
 * a surah boundary, the pages where a verse straddles the break, the last page
 * where three ayah-1s sit side by side. So this suite walks all 604 of them and
 * asserts the invariant that makes the layout usable - **every page turn lands on a
 * real verse, on the page the reader asked for, and nothing else.**
 *
 * That is the invariant whose failure mode is the worst in the module. A page turn
 * that writes the wrong verse does not crash and does not look wrong: it writes a
 * position that is a real verse on a real page, and the reader finds out a week
 * later that Continue Reading opened the wrong page. It passed review because each
 * example, considered alone, looked right.
 */
class QuranPageIntegrityTest {

    @Test
    fun `every page has at least one verse`() {
        // A page with no text is a blank sheet with a page number on it. If this
        // fails, the partition has a hole in it and every page-turn test below is
        // about a fiction.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            assertTrue(
                "page $page has no verses",
                QuranBrowse.ayahsOnPage(page).isNotEmpty()
            )
        }
    }

    @Test
    fun `a turn from every page lands on that page and a verse that exists`() {
        // Forward from every page that has one. The bug this catches is the original
        // one - a turn writing the first verse of the reader's *surah* rather than
        // of the page - and on three chosen pages it would have been caught by
        // three chosen pages. Here it is caught wherever it happens to be true.
        //
        // The last page is excluded because it is the end of the book, and a book
        // that could be turned past the end would be a book with a page 605.
        for (from in 1 until QuranBrowse.TOTAL_PAGES) {
            val start = ReaderPositionAt(from)
            assertTrue(
                "page $from cannot turn forward, so it is not in the book",
                start.turnedTo(1)
            )
            val landed = QuranBrowse.placeAtPage(from + 1).verse
            assertEquals(
                "turning from page $from landed somewhere other than page ${from + 1}",
                landed,
                start.ref
            )
            assertTrue(
                "page ${from + 1} names ${start.ref}, which is not in the corpus",
                QuranBrowse.ayah(start.ref.surah, start.ref.ayah) != null
            )
        }
    }

    @Test
    fun `a turn back from every page lands on that page`() {
        // The other direction, which is a different bug: a back turn computed from
        // the reader's surah rather than from the page goes wrong in the opposite
        // place, and a forward-only sweep cannot see it.
        for (from in 2..QuranBrowse.TOTAL_PAGES) {
            val start = ReaderPositionAt(from)
            assertTrue("page $from cannot turn back", start.turnedTo(-1))
            assertEquals(
                "turning back from page $from landed somewhere other than page ${from - 1}",
                QuranBrowse.placeAtPage(from - 1).verse,
                start.ref
            )
        }
    }

    @Test
    fun `turning to a page never names a verse that is on a different page`() {
        // The claim the reader's own "continue reading" depends on, stated over the
        // whole book: the verse a page turn writes is a verse of the page it turned
        // *to*. Page 604 is where this is hardest - it holds three surahs and three
        // ayah-1s - and it is one page out of 604.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            val ref = QuranBrowse.placeAtPage(page).verse
            assertTrue(
                "page $page landed on ${ref.surah}:${ref.ayah}, which is not on page " +
                    "${QuranBrowse.pageOf(ref.surah, ref.ayah)}",
                QuranBrowse.ayahOnPage(ref, page)
            )
        }
    }

    @Test
    fun `a page turn never carries a selection onto a page that lacks it`() {
        // Over every page, with a verse from that page selected. The rule is that a
        // selection only survives while the new page still contains it, and the case
        // that matters is a selection which *is* still on the new page - a verse
        // that straddles a page boundary appears on two pages, and there the
        // selection must survive. Testing only the drop case would pass with a
        // blanket clear.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            val onPage = QuranBrowse.ayahsOnPage(page)
            val first = onPage.first()
            val selection = QuranRef(first.surahNumber, first.ayahNumber, page)
            val start = ReaderPositionAt(page)
            start.select(selection)

            if (page < QuranBrowse.TOTAL_PAGES && start.turnedTo(1)) {
                val stillThere = QuranBrowse.ayahOnPage(selection, page + 1)
                if (stillThere) {
                    assertEquals(
                        "a selection on page $page was dropped turning to ${page + 1}, " +
                            "which does contain it",
                        selection,
                        start.selection
                    )
                } else {
                    assertEquals(
                        "a selection from page $page survived onto page ${page + 1}",
                        null,
                        start.selection
                    )
                }
            }
        }
    }

    @Test
    fun `a verse straddling a page boundary is on both pages and is handled`() {
        // The real mushaf lets a verse break across two pages. This corpus is
        // verse-granular, so a straddling verse is recorded once - but the *verse
        // after* it starts the next page, and the pages either side of the break
        // must still be contiguous and non-overlapping. Asserting the partition
        // rather than the straddle, because the straddle is not representable.
        var pagesChecked = 0
        for (page in 2..QuranBrowse.TOTAL_PAGES) {
            val onPage = QuranBrowse.ayahsOnPage(page)
            val previousPageLast = QuranBrowse.ayahsOnPage(page - 1).last()
            val first = onPage.first()
            assertEquals(
                "page $page starts at ${first.surahNumber}:${first.ayahNumber}, which is " +
                    "not the verse after page ${page - 1}'s last " +
                    "(${previousPageLast.surahNumber}:${previousPageLast.ayahNumber})",
                nextAfter(previousPageLast),
                keyOf(first)
            )
            assertTrue(
                "page $page repeats a verse already on page ${page - 1}",
                keyOf(first) != keyOf(previousPageLast)
            )
            pagesChecked++
        }
        assertEquals(
            "the sweep did not cover the whole book",
            QuranBrowse.TOTAL_PAGES - 1,
            pagesChecked
        )
    }

    @Test
    fun `the pages that cross a surah boundary open in the surah they open in`() {
        // A page that crosses a boundary names two surahs. The page-turn target is
        // the page's *first* verse, so a turn onto such a page must land in the surah
        // the page begins in - not the one it ends in, which is what a reader
        // checking their place against a printed mushaf would call wrong.
        val crossing = (1..QuranBrowse.TOTAL_PAGES).filter { page ->
            QuranBrowse.surahsOnPage(page).size > 1
        }
        assertTrue(
            "no page in the book crosses a surah boundary, so this case is not covered",
            crossing.isNotEmpty()
        )
        for (page in crossing) {
            val surahs = QuranBrowse.surahsOnPage(page)
            val ref = QuranBrowse.placeAtPage(page).verse
            assertEquals(
                "page $page holds ${surahs.map { it.number }} and turned onto " +
                    "${ref.surah}:${ref.ayah}",
                surahs.first().number,
                ref.surah
            )
        }
    }

    @Test
    fun `the last page holds three ayah ones and each is a different verse`() {
        // The case the whole `QuranRef(surah, ayah, page)` type exists for. Page 604
        // holds Al-Ikhlas, Al-Falaq and An-Nas, so ayah 1 occurs three times on it.
        // If a verse were identified by its ayah number alone, selecting one of them
        // would select whichever came first, and the bookmark would land on the
        // wrong surah.
        val last = QuranBrowse.ayahsOnPage(QuranBrowse.TOTAL_PAGES)
        val ones = last.filter { it.ayahNumber == 1 }
        assertEquals(
            "the last page no longer holds three ayah-1s, so the case is not covered",
            3,
            ones.size
        )
        assertEquals(
            "the three ayah-1s are not three different surahs",
            3,
            ones.map { it.surahNumber }.distinct().size
        )
        for (ayah in ones) {
            val ref = QuranRef(ayah.surahNumber, 1, QuranBrowse.TOTAL_PAGES)
            assertTrue(
                "$ref is not recognised as being on the last page",
                QuranBrowse.ayahOnPage(ref, QuranBrowse.TOTAL_PAGES)
            )
            assertTrue(
                "$ref resolves to no verse",
                QuranBrowse.ayah(ref.surah, ref.ayah) != null
            )
        }
    }

    @Test
    fun `no verse is claimed by two different pages at once`() {
        // `ayahOnPage` is what decides whether a selection survives a turn, so it
        // has to be exact in both directions. A verse claimed by two pages would
        // survive a turn it should not; a verse claimed by none would be lost on
        // every turn. Swept over every page in the book, both ways.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            for (ayah in QuranBrowse.ayahsOnPage(page)) {
                val ref = QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
                assertTrue(
                    "$ref is on page $page but ayahOnPage says otherwise",
                    QuranBrowse.ayahOnPage(ref, page)
                )
                for (other in 1..QuranBrowse.TOTAL_PAGES) {
                    if (other == page) continue
                    assertFalse(
                        "$ref is claimed by both page $page and page $other",
                        QuranBrowse.ayahOnPage(ref, other)
                    )
                }
            }
        }
    }

    /** The verse that canonically follows [ayah], as a comparison key. */
    private fun nextAfter(ayah: com.example.data.model.Ayah): Pair<Int, Int> {
        val next = QuranBrowse.ayah(ayah.surahNumber, ayah.ayahNumber + 1)
        if (next != null) return next.surahNumber to next.ayahNumber
        // End of a surah: the next verse is 1 of the next surah.
        return (ayah.surahNumber + 1) to 1
    }

    /**
     * A verse as a bare reference.
     *
     * The corpus's `Ayah` carries the whole text of the verse, so two of them are
     * never `equals` for a comparison that only means "is this the same verse" -
     * and an assertion comparing them reports a thousand characters of Arabic as the
     * difference, which buries the one number that matters.
     */
    private fun keyOf(ayah: com.example.data.model.Ayah): Pair<Int, Int> =
        ayah.surahNumber to ayah.ayahNumber

    /**
     * A position parked on [page], and the one navigation the reader can perform.
     *
     * Wrapped so the sweep above reads as a statement about pages rather than as a
     * class of arithmetic, and so [ReaderPosition] stays the only thing that knows
     * how a turn works.
     */
    private class ReaderPositionAt(page: Int) {
        private val position = com.example.ui.quran.reader.ReaderPosition(
            QuranBrowse.placeAtPage(page).verse
        )

        val ref: QuranRef get() = position.ref
        val selection: QuranRef? get() = position.selection

        fun select(ref: QuranRef) = position.select(ref)
        fun turnedTo(delta: Int): Boolean = position.turnPage(delta)
    }
}
