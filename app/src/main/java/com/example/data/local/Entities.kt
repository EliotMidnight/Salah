package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey
    val dateString: String, // format "yyyy-MM-dd"
    val fajrDone: Boolean = false,
    val dhuhrDone: Boolean = false,
    val asrDone: Boolean = false,
    val maghribDone: Boolean = false,
    val ishaDone: Boolean = false
)

/**
 * Where the reader was last left.
 *
 * The defaults are Al-Fatihah 1:1 rather than a mid-Baqarah placeholder,
 * because this row is what a first run has: with no row in the table the app
 * still has to answer "where was I?", and for someone who has never read in
 * this app the honest answer is the opening surah rather than a random spot
 * in the second one.
 */
@Entity(tableName = "continue_reading")
data class ContinueReadingEntity(
    @PrimaryKey
    val id: Int = 1,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val surahName: String = "Al-Fatihah",
    val surahNameAr: String = "الفاتحة",
    val pageNumber: Int = 1,
    val snippetAr: String = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val ayahSnippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_locations")
data class CachedLocationEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val isGps: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
