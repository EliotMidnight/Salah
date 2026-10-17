package com.example.data.quran

import com.example.data.model.RevelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The derived surah registry, checked against the corpus it was derived from.
 *
 * These are not mirrors of the construction code - each fact is *recomputed*
 * here by its own path (a verse count from the partition walk, an opening page
 * from the first verse's own reference), so a construction bug is not the
 * oracle of the test that judges it.
 *
 * The names are exempted from one direction of the corpus check on purpose:
 * they are curated, and the corpus's Arabic spellings differ from the app's in
 * what each hamza is on. What must still hold - and is held here and at
 * construction - is that the two are *folded* equal, which forgives the hamza
 * and forgives nothing else.
 *
 * These are properties over all 114 rows, not examples, because a wrong fact
 * about one surah is as much a wrong book as a wrong fact about every surah.
 */
class SurahRegistryTest {

    private val surahs = QuranDataSource.SURAHS

    @Test
    fun `one hundred fourteen rows, numbered one to one-fourteen without a gap`() {
        assertEquals(114, surahs.size)
        assertEquals((1..114).toList(), surahs.map { it.number })
    }

    @Test
    fun `verse counts are the corpus's, and the whole book is accounted for`() {
        assertEquals(6236, surahs.sumOf { it.totalVerses })
        // Counted here by walking the verses the corpus *exposes*, past any
        // bound the metadata would set - so a count that disagrees with the
        // book is caught by the book, not by the number it disagrees with.
        surahs.forEach { surah ->
            val actual = (1..300).count { QuranBrowse.ayah(surah.number, it) != null }
            assertEquals(
                "surah ${surah.number} listed ${surah.totalVerses} verses, " +
                    "the corpus has $actual",
                actual,
                surah.totalVerses
            )
        }
    }

    @Test
    fun `revelation places are the corpus's`() {
        // Both places exist, and every surah carries exactly one.
        assertTrue(surahs.any { it.revelationType == RevelationType.MECCAN })
        assertTrue(surahs.any { it.revelationType == RevelationType.MEDINAN })
        surahs.forEach { surah ->
            val expected = if (QuranCorpus.surahIsMedinan[surah.number]) {
                RevelationType.MEDINAN
            } else {
                RevelationType.MECCAN
            }
            assertEquals(
                "surah ${surah.number}'s revelation place disagrees with the corpus",
                expected,
                surah.revelationType
            )
        }
    }

    @Test
    fun `every surah opens on the page its first verse sits on`() {
        surahs.forEach { surah ->
            assertEquals(
                "surah ${surah.number} opens on the wrong page",
                QuranBrowse.ref(surah.number, 1)!!.page,
                surah.startPage
            )
        }
    }

    @Test
    fun `opening pages climb through the mushaf and never leave it`() {
        // Two surahs may open on the same page - page 604 holds three verse-1s -
        // but no surah can open before the one before it or after the last page.
        surahs.zipWithNext().forEach { (a, b) ->
            assertTrue(
                "surah ${b.number} opens before surah ${a.number}",
                a.startPage <= b.startPage
            )
        }
        assertEquals(1, surahs.first().startPage)
        assertEquals(604, surahs.last().startPage)
        surahs.forEach { surah ->
            assertTrue(
                "surah ${surah.number} opens outside the mushaf, on ${surah.startPage}",
                surah.startPage in 1..604
            )
        }
    }

    @Test
    fun `no name is blank and names do not collide`() {
        surahs.forEach { surah ->
            assertFalse("surah ${surah.number} has a blank Arabic name", surah.arabicName.isBlank())
            assertFalse("surah ${surah.number} has a blank English name", surah.englishName.isBlank())
            assertFalse(
                "surah ${surah.number} has a blank English translation",
                surah.englishTranslation.isBlank()
            )
        }
        assertEquals(
            "two surahs share an English name",
            114,
            surahs.map { it.englishName }.distinct().size
        )
    }

    @Test
    fun `the arabic names only differ from the corpus's in what folding forgives`() {
        surahs.forEach { surah ->
            assertEquals(
                "surah ${surah.number}'s Arabic name is a different name",
                QuranText.normalise(QuranCorpus.surahArabicName(surah.number)),
                QuranText.normalise(surah.arabicName)
            )
        }
    }
}
