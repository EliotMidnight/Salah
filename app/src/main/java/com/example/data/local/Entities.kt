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
 *
 * The defaults are Al-Fatihah 1:1 rather than a mid-Baqarah placeholder, because this row
 * is what a first run has: with no row in the table the app still has to answer "where
 * was I?", and for someone who has never read in this app the honest answer is the
 * opening surah rather than a random spot in the second one.
 *
 * ### Why there is no Quranic text in this row, and there used to be
 *
 * It had a `snippetAr` holding the Arabic of 1:1, and a `surahNameAr`. Both were written
 * on every verse viewed. **Neither was ever read** - the Continue Reading card says
 * `surahName` and `surahNumber:ayahNumber`, and nothing else on any screen touched them.
 *
 * So the row carried a hand-typed copy of Quranic text that had **already drifted** from
 * the corpus: the corpus spells الرحمن with a tatweel before the dagger alif (U+0640
 * then U+0670) and the typed copy did not. Visually indistinguishable, and not the text of
 * the Book - in the one field in the entire app holding Quranic text the SHA-256 gate did
 * not cover, one `SELECT` away from a reader's screen.
 *
 * Rather than fix the copy, both columns are gone (`MIGRATION_3_4`). There is now no second
 * copy of the Book anywhere in the app to drift, which is a stronger guarantee than a test
 * asserting the copy is currently right.
 *
 * The *surah name* stays, because a reader needs to recognise where they were and unlike
 * verse text it is a label rather than the Book. `ContinueReadingDefaultsTest` pins it
 * against `QuranBrowse` so it cannot drift either.
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

