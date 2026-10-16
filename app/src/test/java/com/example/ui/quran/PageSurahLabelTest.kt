package com.example.ui.quran

import com.example.data.quran.QuranBrowse
import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.FrenchStrings
import com.example.ui.localization.GermanStrings
import com.example.ui.localization.IndonesianStrings
import com.example.ui.localization.MalayStrings
import com.example.ui.localization.RussianStrings
import com.example.ui.localization.SpanishStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.UrduStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pill names the page it sits beside, not the reader's place on it.
 *
 * ### The defect this test exists for
 *
 * `QuranBrowse.surahsOnPage` exists because a page is not required to sit inside one
 * surah: 51 of the 604 hold more than one, and page 106 is the end of An-Nisa and the
 * start of Al-Ma'idah. The pill beside the page number named one surah - whichever the
 * reader's verse happened to be in - so the page's name changed as the reader scrolled
 * within it, and a reader checking their place against a printed mushaf was shown one
 * of the names the page carries.
 *
 * ### Why this is a sweep and not an example
 *
 * Whichever page an example picked, the next page could be wrong again. The rule is a
 * function of the page's surahs and the language's joining word, so it is checked on
 * every page in every language: one name where the page holds one surah, the range's
 * two ends where it holds more, and this language's own joining word in every case.
 */
class PageSurahLabelTest {

    private val languages: List<Pair<String, UiStrings>> = listOf(
        "English" to EnglishStrings,
        "Arabic" to ArabicStrings,
        "French" to FrenchStrings,
        "Indonesian" to IndonesianStrings,
        "Turkish" to TurkishStrings,
        "Urdu" to UrduStrings,
        "Malay" to MalayStrings,
        "Bengali" to BengaliStrings,
        "Russian" to RussianStrings,
        "German" to GermanStrings,
        "Spanish" to SpanishStrings
    )

    @Test
    fun `every page is named by the surahs it holds, ranged where it holds more than one`() {
        val crossing = (1..QuranBrowse.TOTAL_PAGES)
            .filter { QuranBrowse.surahsOnPage(it).size > 1 }
        assertEquals(
            "the pages that hold more than one surah changed, so the case this " +
                "sweep is about is not the one covered",
            51,
            crossing.size
        )

        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            val surahs = QuranBrowse.surahsOnPage(page)
            assertTrue("page $page holds no surah", surahs.isNotEmpty())

            for ((language, strings) in languages) {
                val reader = strings.more.reader
                val expected = if (surahs.size == 1) {
                    surahs.first().englishName
                } else {
                    surahs.first().englishName + reader.rangeTo + surahs.last().englishName
                }
                assertEquals(
                    "$language names page $page as " +
                        "\"${pageSurahLabel(reader, surahs)}\" instead of \"$expected\"",
                    expected,
                    pageSurahLabel(reader, surahs)
                )
            }
        }
    }
}
