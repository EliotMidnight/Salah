package com.example.ui.quran

import com.example.data.model.Surah

/**
 * The name this interface has for [surah]: Arabic where the interface is Arabic, the
 * transliteration everywhere else.
 *
 * Both names are the corpus's data; which one a reader is shown is a decision about the
 * reader. The surah index took it per row, and the rest of the app did not know it had
 * been taken: the search results, the saved list, the pill beside the page and the
 * recitation banner named surahs in transliteration in every language, so an Arabic
 * reader typing الفاتحة was answered with "Al-Fatihah".
 *
 * Which languages are Arabic interfaces is `isArabicInterface`'s question - the
 * language, not the script, so an Urdu interface still reads the transliteration.
 */
internal fun surahDisplayName(surah: Surah, preferArabicName: Boolean): String =
    if (preferArabicName) surah.arabicName else surah.englishName
