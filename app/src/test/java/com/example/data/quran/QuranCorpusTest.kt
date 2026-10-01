package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM verification of the bundled offline corpus (Tanzil Uthmani Arabic
 * + Saheeh International English). No Robolectric needed.
 */
class QuranCorpusTest {

    @Test
    fun corpusHasAll6236VersesInOrder() {
        val ayahs = QuranCorpus.ayahs
        assertEquals(6236, ayahs.size)
        assertEquals(1, ayahs.first().surahNumber)
        assertEquals(1, ayahs.first().ayahNumber)
        assertEquals(114, ayahs.last().surahNumber)
        assertEquals(6, ayahs.last().ayahNumber)
    }

    @Test
    fun everyVerseHasArabicAndEnglish() {
        val missingArabic = QuranCorpus.ayahs.count { it.textArabic.isBlank() }
        val missingEnglish = QuranCorpus.ayahs.count { it.textEnglish.isBlank() }
        assertEquals(0, missingArabic)
        assertEquals(0, missingEnglish)
    }

    @Test
    fun surahVerseCountsMatchMetadata() {
        assertEquals(7, QuranBrowse.ayahsInSurah(1).size)
        assertEquals(286, QuranBrowse.ayahsInSurah(2).size)
        assertEquals(6, QuranBrowse.ayahsInSurah(114).size)
    }

    @Test
    fun ayatAlKursiTranslationIsSahihInternational() {
        val kursi = QuranBrowse.ayah(2, 255)
        assertNotNull(kursi)
        assertTrue(kursi!!.textEnglish.startsWith("Allah - there is no deity except Him"))
    }

    @Test
    fun resolveAyahRejectsOutOfRange() {
        assertNull(QuranBrowse.ayah(1, 8))
        assertNull(QuranBrowse.ayah(115, 1))
    }

    @Test
    fun searchFindsArabicAndEnglish() {
        assertTrue(QuranSearch.searchVerses("الرحمن").isNotEmpty())
        assertTrue(QuranSearch.searchVerses("Merciful").isNotEmpty())
        assertTrue(QuranSearch.searchSurahs("Al-Fatihah").isNotEmpty())
    }

    @Test
    fun easternDigitsConvert() {
        assertEquals("١٢٣", ArabicDigits.of(123))
        assertEquals("٠", ArabicDigits.of(0))
        assertEquals("٦٢٣٦", ArabicDigits.of(6236))
        assertEquals("-٧", ArabicDigits.of(-7))
    }

    @Test
    fun theAyahMarkerCarriesTheVerseNumber() {
        // U+06DD is a standalone ornament, not a numeric placeholder, so the
        // digits follow it rather than replacing it. A mushaf whose marker is a
        // plain circle, or whose count is wrong, is not a mushaf.
        assertEquals("۝٢٥٥", ArabicDigits.ayahMarker(255))
        assertTrue(ArabicDigits.ayahMarker(1).startsWith("۝"))
    }
}
