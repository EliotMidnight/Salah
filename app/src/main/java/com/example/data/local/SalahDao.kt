package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SalahDao {
    // Prayer Logs
    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString LIMIT 1")
    fun getPrayerLog(dateString: String): Flow<PrayerLogEntity?>

    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getPrayerLogOnce(dateString: String): PrayerLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrayerLog(log: PrayerLogEntity)

    // Continue Reading
    @Query("SELECT * FROM continue_reading WHERE id = 1 LIMIT 1")
    fun getContinueReading(): Flow<ContinueReadingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContinueReading(continueReading: ContinueReadingEntity)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun deleteBookmark(surahNumber: Int, ayahNumber: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun isBookmarked(surahNumber: Int, ayahNumber: Int): Int

    // Cached Offline Location
    @Query("SELECT * FROM cached_locations WHERE id = 1 LIMIT 1")
    fun getCachedLocation(): Flow<CachedLocationEntity?>

    @Query("SELECT * FROM cached_locations WHERE id = 1 LIMIT 1")
    suspend fun getCachedLocationOnce(): CachedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedLocation(cachedLocation: CachedLocationEntity)
}
