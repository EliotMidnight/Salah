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

@Entity(tableName = "continue_reading")
data class ContinueReadingEntity(
    @PrimaryKey
    val id: Int = 1,
    val surahNumber: Int = 2,
    val ayahNumber: Int = 184,
    val surahName: String = "Al-Baqarah",
    val surahNameAr: String = "البقرة",
    val pageNumber: Int = 28,
    val snippetAr: String = "أَيَّامًا مَّعْدُودَاتٍ ۚ فَمَن كَانَ مِنكُم مَّرِيضًا أَوْ عَلَىٰ سَفَرٍ فَعِدَّةٌ مِّنْ أَيَّامٍ أُخَرَ",
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
