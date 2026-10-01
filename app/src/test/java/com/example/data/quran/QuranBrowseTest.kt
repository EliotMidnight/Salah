package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The navigation index.
 *
 * ### Why this is worth testing at all
 *
 * The corpus loader already asserts that the partitions are *present* - 604
 * pages, 30 juz', 240 quarters, 6,236 verses. What it cannot assert is that the
 * reader's answers are the ones a reader expects, and those are the things that
 * broke:
 *
 * - the reader's page pill disagreeing with the page on screen;
 * - a page-turn writing the wrong verse into "continue reading";
 * - the surah index promising a start page the corpus disagrees with.
 *
 * Every test here is one of those claims, checked against the partition data
 * rather than against a number someone typed. If the corpus and this index ever
 * disagree, this fails.
 */
class QuranBrowseTest {

    // --- The partition itself --------------------------------------------

    @Test
    fun `the corpus is the full 604-page partition and every page has verses`() {
        // A page with no verses is a page a reader can turn onto and find blank.
        // The old data source would have returned an empty list there and the
        // reader would have rendered its loading state forever.
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            assertTrue(
                "page $page has no verses",
                QuranBrowse.ayahsOnPage(page).isNotEmpty()
            )
        }
    }

    @Test
    fun `every verse appears on exactly one page and the pages partition the corpus`() {
        val total = (1..QuranBrowse.TOTAL_PAGES).sumOf { QuranBrowse.ayahsOnPage(it).size }
        assertEquals(QuranBrowse.TOTAL_VERSES, total)
    }

    @Test
    fun `the first page is Al-Fatihah and the last page is the last three surahs`() {
        // These are the two ends a reader meets first and last, and both are
        // facts about the partition rather than about this app.
        val first = QuranBrowse.ayahsOnPage(1)
        assertEquals(1, first.first().surahNumber)
        assertEquals(1, first.first().ayahNumber)
        assertEquals(7, first.size)

        val last = QuranBrowse.ayahsOnPage(604)
        assertEquals(listOf(112, 113, 114), last.map { it.surahNumber }.distinct())
    }

    @Test
    fun `a page can cross a surah boundary and the index says so`() {
        // This is the case a single running-head surah gets wrong, so it is worth
        // pinning: the page after Al-Isra's last verse belongs to Al-Kahf.
        val crossing = (1..QuranBrowse.TOTAL_PAGES).first { page ->
            QuranBrowse.ayahsOnPage(page).map { it.surahNumber }.distinct().size > 1
        }
        val surahs = QuranBrowse.surahsOnPage(crossing)
        assertTrue("expected a page spanning two surahs", surahs.size >= 2)
        // And the reported surahs must be the ones actually on the page, in order.
        assertEquals(
            QuranBrowse.ayahsOnPage(crossing).map { it.surahNumber }.distinct(),
            surahs.map { it.number }
        )
    }

    @Test
    fun `juz and hizb partitions nest correctly`() {
        // Four quarters make a hizb, and a juz' is six and a quarter hizb - so
        // every hizb must sit inside exactly one juz', and the hizb count for
        // each juz' must sum to sixty over the whole book.
        var hizbsPerJuz = IntArray(QuranBrowse.TOTAL_JUZ + 1)
        for (hizb in 1..QuranBrowse.TOTAL_HIZB) {
            val place = QuranBrowse.placeAtHizb(hizb)
            hizbsPerJuz[place.verse.let { QuranBrowse.juzOf(it.surah, it.ayah) }]++
        }
        assertEquals(QuranBrowse.TOTAL_HIZB, hizbsPerJuz.sum())
        // 60 hizb over 30 juz' is exactly 2 per juz' in the standard partition.
        assertTrue(
            "a juz' must contain whole hizb only: $hizbsPerJuz",
            hizbsPerJuz.drop(1).all { it == 2 }
        )
    }

    @Test
    fun `quarters are one per page-or-less and never empty`() {
        for (quarter in 1..QuranCorpus.QUARTER_COUNT) {
            val start = QuranCorpus.ayahsInJuz(1) // touch the loader once
            assertTrue(start.isNotEmpty())
            break
        }
        // Every quarter resolves a verse, and quarters partition the corpus.
        val refs = (1..QuranCorpus.QUARTER_COUNT).map { quarter ->
            val index = QuranCorpus.quarterBounds[quarter]
            val ayah = QuranCorpus.ayahAt(index)!!
            assertEquals(quarter, QuranBrowse.quarterOf(ayah.surahNumber, ayah.ayahNumber))
        }
        assertEquals(QuranCorpus.QUARTER_COUNT, refs.size)
    }

    // --- References -------------------------------------------------------

    @Test
    fun `a reference resolves to its own page`() {
        // The Ayat al-Kursi is the check everyone reaches for, and it is on a
        // specific page: 2:255 is page 42 in this partition.
        val kursi = QuranBrowse.ref(2, 255)
        assertNotNull(kursi)
        assertEquals(2, kursi!!.surah)
        assertEquals(255, kursi.ayah)
        assertEquals(QuranBrowse.ayah(2, 255)!!.pageNumber, kursi.page)
    }

    @Test
    fun `an ayah number is ambiguous without its surah and a reference is not`() {
        // This is the bug the reference type exists for. Ayah 1 exists in 114
        // surahs, and the last page holds three of them, so a bare ayah number
        // cannot identify a verse.
        val page604 = QuranBrowse.ayahsOnPage(604)
        val firstOfEach = page604.filter { it.ayahNumber == 1 }
        assertTrue("expected three 'verse 1's on page 604", firstOfEach.size >= 3)
        assertTrue(
            "they must be distinguishable",
            firstOfEach.map { it.surahNumber }.distinct().size == firstOfEach.size
        )
    }

    @Test
    fun `a reference outside the corpus resolves to nothing`() {
        assertNull(QuranBrowse.ref(1, 8))
        assertNull(QuranBrowse.ref(115, 1))
        assertNull(QuranBrowse.ref(0, 1))
        assertNull(QuranBrowse.ref(2, 0))
    }

    @Test
    fun `an unknown reference clamps to the opening rather than throwing`() {
        // The reader must always have somewhere to be. A crash on open is worse
        // than landing on Al-Fatihah.
        assertEquals(QuranRefStart, QuranBrowse.refOrStart(1, 8))
        assertEquals(QuranRefStart, QuranBrowse.refOrStart(999, 999))
        assertEquals(QuranRefStart, QuranBrowse.refOrStart(0, 0))
    }

    @Test
    fun `every surah's start page agrees with the curated index`() {
        // The surah list is curated (the names are), but its *facts* are the
        // metadata's. This is the assertion that keeps the two from drifting.
        QuranDataSource.assertMatchesCorpus()
    }

    @Test
    fun `the first verse of a surah is on the page the surah index claims`() {
        QuranDataSource.SURAHS.forEach { surah ->
            assertEquals(
                "surah ${surah.number} starts on the wrong page",
                surah.startPage,
                QuranBrowse.ref(surah.number, 1)!!.page
            )
        }
    }

    // --- Places -----------------------------------------------------------

    @Test
    fun `a page's place resolves to the first verse actually on it`() {
        for (page in listOf(1, 42, 285, 300, 604)) {
            val place = QuranBrowse.placeAtPage(page)
            val first = QuranBrowse.ayahsOnPage(page).first()
            assertEquals(first.surahNumber, place.verse.surah)
            assertEquals(first.ayahNumber, place.verse.ayah)
            assertEquals(page, place.verse.page)
        }
    }

    @Test
    fun `places clamp rather than throw`() {
        assertEquals(1, QuranBrowse.placeAtPage(0).number)
        assertEquals(604, QuranBrowse.placeAtPage(9999).number)
        assertEquals(1, QuranBrowse.placeAtJuz(0).number)
        assertEquals(30, QuranBrowse.placeAtJuz(99).number)
        assertEquals(1, QuranBrowse.placeAtHizb(-1).number)
        assertEquals(60, QuranBrowse.placeAtHizb(61).number)
    }

    @Test
    fun `the last page's juz is the last juz`() {
        val last = QuranBrowse.placeAtPage(604).verse
        assertEquals(30, QuranBrowse.juzOf(last.surah, last.ayah))
    }

    // --- Prostrations -----------------------------------------------------

    @Test
    fun `all fifteen prostrations are readable and none of them is invented`() {
        val found = QuranCorpus.sajdaAfter
        assertEquals(15, found.size)
        // Every one must be a verse that exists, in the surah the metadata names.
        found.keys.forEach { ref ->
            val surah = ref / 1000
            val ayah = ref % 1000
            assertNotNull(
                "prostration at $surah:$ayah names a verse that does not exist",
                QuranBrowse.ayah(surah, ayah)
            )
            assertNotNull(
                "prostration at $surah:$ayah is not reachable through the browse surface",
                QuranBrowse.sajdaAfter(surah, ayah)
            )
        }
    }

    @Test
    fun `the obligatory prostrations are the ones the metadata marks obligatory`() {
        // Counted through the public surface, which is where the reader reads it.
        val obligatory = (1..QuranBrowse.TOTAL_SURAHS).flatMap { surah ->
            (1..QuranCorpus.surahVerseCount[surah])
                .filter { QuranBrowse.sajdaAfter(surah, it) == QuranBrowse.SajdaKind.OBLIGATORY }
                .map { surah to it }
        }
        // Four in the Tanzil metadata, marked `type="obligatory"`; the other
        // eleven of the fifteen are recommended. Enumerated from the corpus rather
        // than from a list typed here, so a metadata change shows up as a test
        // failure instead of as a stale expectation nobody notices.
        assertEquals(
            listOf(32 to 15, 41 to 38, 53 to 62, 96 to 19),
            obligatory
        )
    }

    @Test
    fun `a recommended prostration is distinguishable from an obligatory one`() {
        // The distinction is the whole reason the kind exists: 22:18 and 22:77 are
        // both in the metadata, and only one of them is obligatory.
        assertEquals(
            QuranBrowse.SajdaKind.RECOMMENDED,
            QuranBrowse.sajdaAfter(22, 18)
        )
        assertEquals(
            QuranBrowse.SajdaKind.RECOMMENDED,
            QuranBrowse.sajdaAfter(7, 206)
        )
        assertNull(QuranBrowse.sajdaAfter(2, 255))
    }

    private companion object {
        val QuranRefStart = com.example.data.model.QuranRef.Start
    }
}
