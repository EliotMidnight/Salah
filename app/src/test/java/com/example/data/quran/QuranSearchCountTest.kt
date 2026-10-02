package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A search says how many results there are, not how many it showed.
 *
 * ### The defect
 *
 * `QuranSearch` took the best fifty matches and returned them as a `List`. The sheet
 * counted that list with `size` and printed the number: **"50 verses"** — for a search
 * where **143** verses contain the word.
 *
 * That is worse than showing too few results. Fifty results with no admission reads as
 * *all* of them, and nothing on screen contradicts the reader, so the number was not a
 * truncation but a false statement about the book. Someone looking for every verse
 * containing "mercy" had been told they had seen them.
 *
 * `QuranSearch`'s KDoc claimed this was already fixed — "It took the first 50 and said
 * nothing. Results are now paged honestly" — which is the second half of the problem: the
 * documentation described the fix while the code kept the bug.
 *
 * ### Why a pair and not a list
 *
 * Because the two numbers answer different questions and only one of them is a list.
 * Returning a bare `List` invites `size` for both, which is exactly what happened.
 * `VerseResults` and `SurahResults` make `hits` and `total` separately nameable, and
 * there is deliberately **no `size` on either** — the ambiguity that caused this cannot
 * be reintroduced as a convenience.
 */
class QuranSearchCountTest {

    /**
     * The word the corpus comparison uses.
     *
     * A constant because it appears in both halves of that comparison, and two
     * spellings of one term is how an assertion stops testing what it claims to.
     */
    private val term = "mercy"

    /** One letter: the widest surah-name query there is, and so the one that fills a page. */
    private val shortTerm = "a"

    @Test
    fun `the total is the real number of matches, not the page`() {
        val page = QuranSearch.searchVerses(term)
        assertEquals(
            "the page is not full, so this cannot demonstrate the truncation",
            QuranSearch.PAGE_SIZE,
            page.hits.size
        )
        assertTrue(
            "the search reports ${page.total} matches but shows ${page.hits.size}, " +
                "and does not say so",
            page.isTruncated
        )
    }

    @Test
    fun `raising the limit raises the page but never the total`() {
        val wide = QuranSearch.searchVerses(term, limit = 500)
        val narrow = QuranSearch.searchVerses(term, limit = 10)

        assertEquals(
            "the total changed with the limit, so it is counting something other " +
                "than the matches",
            narrow.total,
            wide.total
        )
        assertEquals(10, narrow.hits.size)
        assertEquals(wide.total, wide.hits.size)
        assertFalse(
            "a search with room for every match still claims to be truncated",
            wide.isTruncated
        )
        assertTrue(narrow.isTruncated)
    }

    @Test
    fun `the total agrees with the bundled translation, counted independently`() {
        // Counted straight off the resource rather than through the search, so the
        // assertion cannot be satisfied by the search agreeing with itself.
        val resource = checkNotNull(
            javaClass.getResourceAsStream("/quran/en_sahihintl.txt")
        ) { "the bundled translation is not on the classpath" }
        val verses = resource.bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { it.split('|').getOrNull(2).orEmpty() }

        val expected = verses.count { it.contains(term, ignoreCase = true) }
        assertTrue(
            "only $expected of ${verses.size} verses contain the term, which is too " +
                "few to fill a page and so cannot demonstrate the truncation",
            expected > QuranSearch.PAGE_SIZE
        )

        val reported = QuranSearch.searchVerses(term).total
        assertEquals(
            "the search reported $reported matches but $expected verses of the " +
                "bundled translation contain the term",
            expected,
            reported
        )
    }

    @Test
    fun `a result that fits is never described as truncated`() {
        // "Showing the first 3 of 3" on every keystroke would train a reader to stop
        // reading the line.
        for (nothing in listOf("zzzznotaword", "", "   ")) {
            val none = QuranSearch.searchVerses(nothing)
            assertEquals(0, none.total)
            assertEquals(0, none.hits.size)
            assertFalse(
                "an empty search claims to be truncated",
                none.isTruncated
            )
        }

        val rare = QuranSearch.searchVerses("Abaddon")
        assertFalse(
            "a search whose every match fits is still marked truncated",
            rare.isTruncated
        )
    }

    @Test
    fun `a surah-name search counts the names, not the verses`() {
        val all = QuranSearch.searchSurahs(shortTerm, limit = 500)
        val five = QuranSearch.searchSurahs(shortTerm, limit = 5)

        assertEquals(
            "a surah-name search reported a total that changed with the limit",
            all.total,
            five.total
        )
        assertEquals(5, five.hits.size)
        assertTrue(
            "a one-letter surah-name search matched only ${all.total} surahs, so this " +
                "cannot demonstrate the truncation",
            all.total > QuranSearch.PAGE_SIZE
        )
        assertTrue(five.isTruncated)
        assertFalse(all.isTruncated)
    }

    @Test
    fun `a surah-name match still returns no verses`() {
        // The other half of the same heading. "Maryam" matched the surah's name and then
        // returned all 97 of her verses under a heading that said "97 verses found" —
        // none of which contain the word.
        assertEquals(
            listOf(19),
            QuranSearch.searchSurahs("Maryam").hits.map { it.number }
        )
        assertEquals(
            0,
            QuranSearch.searchVerses("Maryam").total
        )
    }
}