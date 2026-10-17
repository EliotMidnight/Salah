package com.example.ui.quran

import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
