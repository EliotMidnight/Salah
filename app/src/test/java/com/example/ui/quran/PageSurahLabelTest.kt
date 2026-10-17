package com.example.ui.quran

import com.example.data.model.Surah
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
import com.example.ui.localization.isArabicInterface
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
 * function of the page's surahs, the name the interface has for each of them, and the
 * language's joining word, so it is checked on every page in every language: one name
 * where the page holds one surah, the range's two ends where it holds more, the
 * interface's own name for each end - Arabic in Arabic, transliterated elsewhere - and
 * this language's own joining word in every case.
 *
 * The naming policy is written out here rather than read from [surahDisplayName], so a
 * flip in the policy is a failure and not a no-op.
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
    fun `every page is named by the surahs it holds, in the interface's own language`() {
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
                val preferArabic = isArabicInterface(language)
                fun interfaceName(surah: Surah): String =
                    if (preferArabic) surah.arabicName else surah.englishName

                val expected = if (surahs.size == 1) {
                    interfaceName(surahs.first())
                } else {
                    interfaceName(surahs.first()) + reader.rangeTo + interfaceName(surahs.last())
                }
                assertEquals(
                    "$language names page $page as " +
                        "\"${pageSurahLabel(reader, surahs, preferArabic)}\" instead of " +
                        "\"$expected\"",
                    expected,
                    pageSurahLabel(reader, surahs, preferArabic)
                )
            }
        }
    }

    /**
     * Every language's range mark is set off from the names on both sides of it.
     *
     * Arabic's was "إلى " - no space before it - and numbers hid it: "112إلى 114"
     * reads as a curiosity, but the pill puts names in the same two slots, and
     * "الإخلاصإلى الناس" reads as one broken word. The mark is a word like any other,
     * padded on both sides as the other ten languages' marks are.
     */
    @Test
    fun `every language's range mark is padded on both sides`() {
        for ((language, strings) in languages) {
            val mark = strings.more.reader.rangeTo
            assertTrue(
                "$language's range mark \"$mark\" has no space before it, so it joins " +
                    "onto whatever name precedes it",
                mark.first().isWhitespace()
            )
            assertTrue(
                "$language's range mark \"$mark\" has no space after it, so it joins " +
                    "onto whatever name follows it",
                mark.last().isWhitespace()
            )
        }
    }
}
