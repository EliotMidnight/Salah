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
}
