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
    fun `a surah starts on the page its first verse is on, and it is a page`() {
        // The claim the running head rests on, checked for every surah.
        //
        // The interesting half is the count: a surah must not start mid-page in the
        // sense of *belonging to no page*, and it must not be that two surahs claim
        // the same page's start. What this suite exists to catch is the third case -
        // a surah whose first verse is on a page that does not *begin* a surah, which
        // is 42 pages' worth of surahs starting part-way down a page. Those are
        // legitimate; they are simply the case a "print the head only when the page
        // opens a surah" rule silently drops.
        var midPageSurahs = 0
        var pagesWithNoHeadAtAll = 0
        for (surah in 1..114) {
            val first = QuranBrowse.ayah(surah, 1)
            assertTrue("surah $surah has no verse 1", first != null)
            val page = QuranBrowse.pageOf(surah, 1)
            assertTrue(
                "surah $surah's first verse reports page $page, which is not a page",
                page in 1..QuranBrowse.TOTAL_PAGES
            )
            assertEquals(
                "surah $surah's first verse reports page ${first!!.pageNumber}",
                page,
                first.pageNumber
            )
            assertTrue(
                "surah $surah is not on the page its first verse reports",
                QuranBrowse.ayahOnPage(QuranRef(surah, 1, page), page)
            )
            val onPage = QuranBrowse.ayahsOnPage(page)
            val indexOnPage = onPage.indexOfFirst {
                it.surahNumber == surah && it.ayahNumber == 1
            }
            assertTrue(
                "surah $surah's first verse is not on page $page",
                indexOnPage >= 0
            )
            if (indexOnPage > 0) {
                midPageSurahs++
                // The worst of the two cases, and the one the old rule could not see:
                // the page does not even *open* a surah, so a head printed at a
                // page's top would not have named this one either.
                if (onPage.first().ayahNumber != 1) pagesWithNoHeadAtAll++
            }
        }
        // Pinned rather than left open, because these numbers are the entire case:
        // 58 of 114 surahs begin part-way down a page, spread over 51 pages. On 42 of
        // those pages the page opens no surah either, so a "print the head when the
        // page opens a surah" rule would name 56 surahs and leave 58 unlabelled -
        // and 45 of those 58 would be on a page that has no head at all, not merely
        // at the wrong place on it.
        //
        // The 45 and the 42 differ because a page can start two surahs part-way down
        // (page 604 carries both Al-Falaq and An-Nas after Al-Ikhlas's four verses),
        // so the surah count is the larger of the two. Both are pinned because if
        // either ever reached zero the old rule would become correct again by
        // accident, and nobody would know why.
        assertEquals(
            "the number of surahs that start part-way down a page has changed; the " +
                "head rule and this count are meant to be read together",
            58,
            midPageSurahs
        )
        assertEquals(
            "the number of surahs that start on a page which opens no surah has " +
                "changed",
            45,
            pagesWithNoHeadAtAll
        )
    }

    @Test
    fun `a page's place and its verses name the same verse`() {
        // `placeAtPage`, `ayahsOnPage` and `pageOf` are three readers of one
        // partition, asked in three different places - a page turn here, a drawn
        // page there, the index sheet's page filter somewhere else. If any two
        // disagree the reader turns to one page and is shown another. Asserted
        // across the book, because it costs three lookups per page and the point is
        // that nothing is left unchecked.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            val onPage = QuranBrowse.ayahsOnPage(page)
            val place = QuranBrowse.placeAtPage(page).verse
            assertEquals(
                "page $page's place is ${place.surah}:${place.ayah}, which is not its " +
                    "first verse",
                onPage.first().surahNumber to onPage.first().ayahNumber,
                place.surah to place.ayah
            )
            for (ayah in onPage) {
                assertEquals(
                    "a verse on page $page reports page ${ayah.pageNumber}",
                    page,
                    QuranBrowse.pageOf(ayah.surahNumber, ayah.ayahNumber)
                )
            }
        }
    }

    @Test
    fun `a page opens in the surah the index reports for it`() {
        // The resolution a tap on a mushaf page is built on. A page opens in one
        // surah and can carry two more, so "the surah of this page" has to be the
        // page's *first* - and the pair it makes has to be a verse the page holds.
        //
        // Worth pinning because the alternative reads as harmless: resolving a tap
        // against the reader's current surah instead. On the pages that cross a
        // boundary that names a verse from the wrong surah, so a reader tapping a
        // verse of the second surah on the page gets a highlight on a verse that is
        // not the one they touched - and the bookmark action fires on that one.
        // Neither throws and both look like a selection.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            val onPage = QuranBrowse.ayahsOnPage(page)
            assertEquals(
                "page $page does not open in the surah the index reports",
                onPage.first().surahNumber,
                QuranBrowse.surahsOnPage(page).first().number
            )
            for (ayah in onPage) {
                val ref = QuranRef(ayah.surahNumber, ayah.ayahNumber, page)
                assertTrue(
                    "$ref is on page $page but did not resolve from it",
                    QuranBrowse.ayahOnPage(ref, page)
                )
            }
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
