package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
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
 */
@Entity(tableName = "continue_reading")
data class ContinueReadingEntity(
    @PrimaryKey
    val id: Int = 1,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val surahName: String = "Al-Fatihah",
    val pageNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A verse the reader saved.
 *
 * **The unique index on `(surahNumber, ayahNumber)` is the point of this table.** It
 * had an `autoGenerate` primary key and no natural key, so `toggleBookmark`'s
 * read-then-write could insert the same verse twice - two taps landing close together
 * both read "not saved" and both inserted - and the Saved list showed the verse twice.
 * Two consumers disagreeing about a row is the usual way this goes wrong, but here both
 * agreed and the *store* could not hold the answer; the index is what makes a second
 * insert a no-op rather than a second row.
 *
 * A verse is `(surah, ayah)` and not the ayah alone, for the same reason `QuranRef`
 * carries a page: page 604 holds three ayah-1s.
 */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["surahNumber", "ayahNumber"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val ayahSnippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

