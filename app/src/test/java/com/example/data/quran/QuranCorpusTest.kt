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
        assertEquals(7, QuranDataSource.getAyahsForSurah(1).size)
        assertEquals(286, QuranDataSource.getAyahsForSurah(2).size)
        assertEquals(6, QuranDataSource.getAyahsForSurah(114).size)
    }

    @Test
    fun ayatAlKursiTranslationIsSahihInternational() {
        val kursi = QuranDataSource.resolveAyah(2, 255)
        assertNotNull(kursi)
        assertTrue(kursi!!.textEnglish.startsWith("Allah - there is no deity except Him"))
    }

    @Test
    fun resolveAyahRejectsOutOfRange() {
        assertNull(QuranDataSource.resolveAyah(1, 8))
        assertNull(QuranDataSource.resolveAyah(115, 1))
    }

    @Test
    fun searchFindsArabicAndEnglish() {
        assertTrue(QuranDataSource.searchAyahs("الرحمن").isNotEmpty())
        assertTrue(QuranDataSource.searchAyahs("Merciful").isNotEmpty())
        assertTrue(QuranDataSource.searchAyahs("Al-Fatihah").size >= 7)
    }

    @Test
    fun easternDigitsConvert() {
        assertEquals("١٢٣", QuranDataSource.toArabicDigits(123))
    }
}
