package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The search window's contract, asked purely.
 *
 * "Show more" makes readers of queries that used to be capped: what it must
 * never make of the answer page is a re-ranking. Every window is a slice of
 * the *same* ranked order - a reader who asks for more receives the rows the
 * first withholded, in the order the first claimed was best, and no row shows
 * twice. These are properties of whole windows, not examples, because a
 * duplicate or a gap is a wrong page for someone however deep it hides.
 */
class QuranSearchPagingTest {

    // Queries with different shapes: a term English and Arabic verses share, a
    // letter that lands in most names, and one that barely matches.
    private val queries = listOf("mercy", "the", "a", "water")

    @Test
    fun `consecutive windows tile one ranked order, with no row twice and none skipped`() {
        queries.forEach { query ->
            val everything = QuranSearch.searchVerses(query, limit = Int.MAX_VALUE)
            if (everything.total <= QuranSearch.PAGE_SIZE) return@forEach

            val pages = generateSequence(0) { it + QuranSearch.PAGE_SIZE }
                .takeWhile { it < everything.total }
                .map { skip ->
                    QuranSearch.searchVerses(query, skip = skip).hits
                }
                .toList()

            assertEquals(
                "the windows of '$query' re-ranked the list instead of slicing it",
                everything.hits,
                pages.flatten()
            )
        }
    }

    @Test
    fun `the total says how many match, and answers no skip`() {
        queries.forEach { query ->
            val total = QuranSearch.searchVerses(query).total
            assertTrue("expected '$query' to match more than one page", total > QuranSearch.PAGE_SIZE)
            var skip = 0
            while (skip < total + QuranSearch.PAGE_SIZE) {
                assertEquals(
                    "'$query' reported a different total past skip $skip",
                    total,
                    QuranSearch.searchVerses(query, skip = skip).total
                )
                skip += QuranSearch.PAGE_SIZE
            }
        }
    }

    @Test
    fun `a window past the end of the matches is empty, not an error`() {
        queries.forEach { query ->
            val total = QuranSearch.searchVerses(query).total
            val past = QuranSearch.searchVerses(query, skip = total)
            assertTrue("expected an empty page past the end of '$query'", past.hits.isEmpty())
            assertEquals("an empty page still knows its query's total", total, past.total)
            // The caller's own more-remain rule - skip + rows seen < total -
            // closes on an empty past-the-end page by construction; the tiling
            // test above is what proves the windows never oversell it.
        }
    }

    @Test
    fun `the same window rules hold for the surah tier`() {
        val query = "a"
        val everything = QuranSearch.searchSurahs(query, limit = Int.MAX_VALUE)
        // "a" reaches most English names, so the tier carries rows the first
        // window could not show.
        assertTrue("expected the surah tier to be capped", everything.total > QuranSearch.PAGE_SIZE)

        val pages = generateSequence(0) { it + QuranSearch.PAGE_SIZE }
            .takeWhile { it < everything.total }
            .map { skip -> QuranSearch.searchSurahs(query, skip = skip).hits }
            .toList()
        assertEquals(
            "the surah tier's windows re-ranked the list instead of slicing it",
            everything.hits,
            pages.flatten()
        )
    }
}
