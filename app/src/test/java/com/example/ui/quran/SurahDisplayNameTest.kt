package com.example.ui.quran

import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A surah is named in the interface's own language.
 *
 * ### The defect this test exists for
 *
 * The surah index leads with the Arabic name in an Arabic interface, and the rest of
 * the reader did not know that decision had been taken: the index's search results and
 * saved list, and the reader's pill and recitation banner, named surahs in
 * transliteration in all eleven languages. An Arabic reader who typed الفاتحة was
 * answered with "Al-Fatihah".
 *
 * ### Why this is a sweep and not an example
 *
 * The rule is a function of the surah and the interface, so it is checked on every
 * surah of the corpus in both directions. Both names are also required to differ, so
 * "prefer the Arabic name" cannot be satisfied by one string under two labels.
 *
 * ### The second door
 *
 * Today's Continue Reading card holds a surah *number* and must not compile the corpus
 * to name one, so the data layer answers the same question for a number
 * (`QuranDataSource.surahName`). The second test sweeps both directions through both
 * doors and requires the strings to agree, which is what keeps one policy from becoming
 * two.
 */
class SurahDisplayNameTest {

    @Test
    fun `every surah is named in the interface's own language`() {
        for (surah in QuranBrowse.surahs) {
            assertNotEquals(
                "surah ${surah.number} carries the same string for both its names, so " +
                    "which one an interface prefers cannot matter",
                surah.arabicName,
                surah.englishName
            )
            assertEquals(
                "surah ${surah.number} reads \"${surahDisplayName(surah, true)}\" in an " +
                    "Arabic interface instead of its Arabic name",
                surah.arabicName,
                surahDisplayName(surah, preferArabicName = true)
            )
            assertEquals(
                "surah ${surah.number} reads \"${surahDisplayName(surah, false)}\" in a " +
                    "non-Arabic interface instead of its transliteration",
                surah.englishName,
                surahDisplayName(surah, preferArabicName = false)
            )
        }
    }

    @Test
    fun `the names table answers a number with the same names`() {
        for (surah in QuranBrowse.surahs) {
            assertEquals(
                "surah ${surah.number} is named " +
                    "\"${QuranDataSource.surahName(surah.number, true)}\" through the " +
                    "names table, but \"${surahDisplayName(surah, true)}\" through the " +
                    "compiled rows",
                surahDisplayName(surah, preferArabicName = true),
                QuranDataSource.surahName(surah.number, preferArabicName = true)
            )
            assertEquals(
                "surah ${surah.number} is named " +
                    "\"${QuranDataSource.surahName(surah.number, false)}\" through the " +
                    "names table, but \"${surahDisplayName(surah, false)}\" through the " +
                    "compiled rows",
                surahDisplayName(surah, preferArabicName = false),
                QuranDataSource.surahName(surah.number, preferArabicName = false)
            )
        }
    }

    @Test
    fun `a number no surah has is not given a name`() {
        // The table is positional, so the ends are where an off-by-one would live; and
        // null is what lets a display site leave the name out rather than throw.
        assertNull(QuranDataSource.surahName(0, preferArabicName = false))
        assertNull(QuranDataSource.surahName(115, preferArabicName = true))
    }
}
