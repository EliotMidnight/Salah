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

enum class RevelationType(val labelEn: String, val labelAr: String) {
    MECCAN("Meccan", "مكية"),
    MEDINAN("Medinan", "مدنية")
}

data class Ayah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val textEnglish: String = "",
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val hizbQuarter: Int = 1
)

data class Bookmark(
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val ayahSnippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ContinueReading(
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val surahName: String = "Al-Fatihah",
    val surahNameAr: String = "الفاتحة",
    val pageNumber: Int = 1,
    val snippetAr: String = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
