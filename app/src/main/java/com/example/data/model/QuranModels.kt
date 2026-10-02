package com.example.data.model

data class Surah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishTranslation: String,
    val totalVerses: Int,
    val revelationType: RevelationType,
    val startPage: Int
)

enum class RevelationType {
    MECCAN,
    MEDINAN
}

data class Ayah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val textEnglish: String = "",
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val hizbQuarter: Int = 1
) {
    /**
     * This verse as a [QuranRef].
     *
     * So that a verse is converted to a reference in exactly one way.
     *
     * The mushaf page used to hand its caller an **ayah number** and let the caller
     * rebuild the reference, filling in the surah from the page's *first* verse. On
     * the 51 pages that hold more than one surah that produced a reference to a
     * different verse in a different surah - and 523 verses could not be selected
     * by tap at all. A page is not inside one surah, so "which surah is this verse
     * in" is a question only the verse itself can answer.
     *
     * `pageNumber` comes from the same corpus row as the other numbers, so it is
     * the same answer every time rather than a number a caller supplied.
     */
    val ref: QuranRef get() = QuranRef(surah = surahNumber, ayah = ayahNumber, page = pageNumber)
}
