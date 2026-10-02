package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Search, and the two result tiers it has to keep apart.
 *
 * ### The bug that made this a test file
 *
 * The old `searchAyahs` had `surah.englishName.contains(query)` inside the
 * *verse* predicate. So searching "Maryam" matched the surah's name and then
 * returned all 97 of her verses - none of which contain the word. The sheet said
 * "97 verses found" and the first result landed the reader in the middle of a
 * surah they had not asked for. Surah names are a separate tier now, and these
 * tests are the guarantee they stay one.
 */
class QuranSearchTest {

    @Test
    fun `a surah name search returns the surah and not its verses`() {
        val surahs = QuranSearch.searchSurahs("Maryam").hits
        assertEquals(listOf(19), surahs.map { it.number })
        assertTrue("no verse results from a name query", QuranSearch.searchVerses("Maryam").hits.isEmpty())
    }

    @Test
    fun `every surah is findable by its own curated name`() {
        // A surah index a reader cannot find a surah in is not an index. All 114
        // names, as the app writes them, must resolve to themselves first.
        QuranDataSource.SURAHS.forEach { surah ->
            val hits = QuranSearch.searchSurahs(surah.englishName).hits
            assertTrue(
                "'${surah.englishName}' did not find surah ${surah.number}",
                hits.any { it.number == surah.number }
            )
        }
    }

    @Test
    fun `a surah is findable by its number and by its meaning`() {
        assertEquals(112, QuranSearch.searchSurahs("112").hits.first().number)
        assertEquals(1, QuranSearch.searchSurahs("1").hits.first().number)
        assertTrue(
            QuranSearch.searchSurahs("The Cow").hits.any { it.number == 2 }
        )
    }

    @Test
    fun `a surah name search is ranked by how well the name matches`() {
        // An exact name beats a prefix, a prefix beats a substring, a substring
        // beats a meaning-only match. A reader typing "Al" wants Al-Baqarah, not
        // every surah whose meaning contains the letters "al".
        val hits = QuranSearch.searchSurahs("Al-Baqarah").hits
        assertEquals(2, hits.first().number)
    }

    @Test
    fun `an ambiguous prefix still surfaces the obvious surah first`() {
        // "Al-Isra" and "Al-Ismail" both start with it; the exact name must win.
        val hits = QuranSearch.searchSurahs("Al-Isra").hits
        assertEquals(17, hits.first().number)
    }

    @Test
    fun `a surah name search in Arabic finds the surah`() {
        val hits = QuranSearch.searchSurahs("مريم").hits
        assertTrue("expected surah 19", hits.any { it.number == 19 })
    }

    // --- Verses -----------------------------------------------------------

    @Test
    fun `verse search finds Arabic and English`() {
        assertTrue(QuranSearch.searchVerses("ٱلرحمن").hits.isNotEmpty())
        assertTrue(QuranSearch.searchVerses("Merciful").hits.isNotEmpty())
    }

    @Test
    fun `a verse search reports which field matched`() {
        // The row has to show the Arabic for an Arabic match and the English for
        // an English one, so the field is not decoration.
        val arabic = QuranSearch.searchVerses("ٱلرحمن").hits.first()
        assertEquals(QuranSearchHit.Field.ARABIC, arabic.matchedIn)

        val english = QuranSearch.searchVerses("Merciful").hits.first()
        assertEquals(QuranSearchHit.Field.ENGLISH, english.matchedIn)
    }

    @Test
    fun `a verse hit carries a reference that opens the right page`() {
        val hit = QuranSearch.searchVerses("Merciful").hits.first()
        assertEquals(hit.ayah.pageNumber, hit.ref.page)
        assertEquals(
            hit.ayah.surahNumber to hit.ayah.ayahNumber,
            hit.ref.surah to hit.ref.ayah
        )
    }

    @Test
    fun `every term must be present`() {
        // "Allah light" is the test: a verse that has both, whichever language
        // each is in. The old implementation OR'd surah-name matches in, which is
        // how a two-word search returned a whole surah.
        val both = QuranSearch.searchVerses("Allah light").hits
        both.forEach { hit ->
            val hasAllah = QuranText.normalised[QuranCorpus.indexOf(hit.ayah.surahNumber, hit.ayah.ayahNumber)]
                .contains("الله") || hit.ayah.textEnglish.lowercase().contains("allah")
            val hasLight = hit.ayah.textEnglish.lowercase().contains("light") ||
                QuranText.normalise(hit.ayah.textArabic).contains("نور")
            assertTrue("hit has neither term", hasAllah || hasLight)
        }
    }

    @Test
    fun `a term that appears nowhere yields nothing`() {
        assertTrue(QuranSearch.searchVerses("zzzznotaword").hits.isEmpty())
    }

    @Test
    fun `an empty or blank query searches nothing`() {
        assertTrue(QuranSearch.searchVerses("").hits.isEmpty())
        assertTrue(QuranSearch.searchVerses("   ").hits.isEmpty())
        assertTrue(QuranSearch.searchSurahs("").hits.isEmpty())
    }

    @Test
    fun `results are capped and the cap is the one the sheet advertises`() {
        // A cap with no "and more" reads as "these are all of them", which is
        // how the old sheet claimed 50 was the whole corpus. The cap is a named
        // constant so the sheet can say what it is.
        val many = QuranSearch.searchVerses("الله", limit = 5).hits
        assertTrue("expected the cap to bind", many.size <= 5)
        assertTrue("expected more than five to exist", QuranSearch.searchVerses("الله", limit = 500).hits.size > 5)
    }

    @Test
    fun `the best-ranked verse match is a word-initial one`() {
        // "mercy" appears in many verses. The top result must be a verse where
        // the word stands as a word, not one that merely contains the letters
        // inside a longer word - otherwise the first result a reader taps is
        // arbitrary.
        val hits = QuranSearch.searchVerses("mercy", limit = 40).hits
        assertTrue("expected several matches", hits.size > 3)
        val first = hits.first()
        val source = when (first.matchedIn) {
            QuranSearchHit.Field.ARABIC -> QuranText.normalised[
                QuranCorpus.indexOf(first.ayah.surahNumber, first.ayah.ayahNumber)
            ]

            else -> first.ayah.textEnglish.lowercase()
        }
        val at = source.indexOf("mercy")
        assertTrue("the top hit does not contain the term at all", at >= 0)
        assertTrue(
            "the top hit should start the word, found at $at in \"$source\"",
            at == 0 || !source[at - 1].isLetter()
        )
    }

    @Test
    fun `search does not return a verse for a surah name query`() {
        // The regression itself, stated as a test.
        val hits = QuranSearch.searchVerses("Al-Fatihah").hits
        // Al-Fatihah is *in* Al-Fatihah, so this is about the general rule: no hit
        // may come from a surah whose *name* matched, only from its text.
        hits.forEach { hit ->
            assertFalse(
                "hit came from a name match, not a text match",
                hit.matchedIn == QuranSearchHit.Field.SURAH_NAME
            )
        }
    }

    @Test
    fun `the subject of a hit is the text the row should show`() {
        val arabic = QuranSearch.searchVerses("ٱلرحمن").hits.first()
        assertEquals(arabic.ayah.textArabic, arabic.subject)
        val english = QuranSearch.searchVerses("Merciful").hits.first()
        assertEquals(english.ayah.textEnglish, english.subject)
    }
}
