package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrayerLogEntity::class,
        ContinueReadingEntity::class,
        BookmarkEntity::class,
        CachedLocationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SalahDatabase : RoomDatabase() {
    abstract fun salahDao(): SalahDao

    companion object {
        @Volatile
        private var INSTANCE: SalahDatabase? = null

        fun getDatabase(context: Context): SalahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalahDatabase::class.java,
                    "salah_quiet.db"
                )
                    // The only local state is the prayer checklist, so a schema
                    // change that drops it is preferable to a crash on upgrade.
                    // Pass `dropAllTables = false` when a migration is added so
                    // existing rows survive.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
