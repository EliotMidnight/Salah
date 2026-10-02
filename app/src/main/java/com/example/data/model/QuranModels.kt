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
     */
    val ref: QuranRef get() = QuranRef(surah = surahNumber, ayah = ayahNumber, page = pageNumber)
}
