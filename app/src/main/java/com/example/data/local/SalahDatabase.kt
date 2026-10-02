package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrayerLogEntity::class,
        ContinueReadingEntity::class,
        BookmarkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SalahDatabase : RoomDatabase() {
    abstract fun salahDao(): SalahDao

    companion object {
        @Volatile
        private var INSTANCE: SalahDatabase? = null

        /**
         * **2 to 3.** Drops the orphaned `cached_locations` table, and gives `bookmarks`
         * the unique index it should always have had.
         *
         * Written by hand rather than left to `fallbackToDestructiveMigration`, which is
         * what the previous version did for every version bump - and which would have
         * taken the reader's **bookmarks and their Continue Reading position** with it
         * to remove a table that nothing read. Those are the only user-authored data
         * this app has; they are not worth an empty table.
         *
         * The index is the other half, and it fixes a real defect rather than tidying:
         * `BookmarkEntity` had an `autoGenerate` primary key and no natural key, so the
         * read-then-write in `SalahRepository.toggleBookmark` could insert the same verse
         * twice when two taps landed close together, and the Saved list showed it
         * twice. A unique index makes the second insert a no-op, so the rows that exist
         * are the rows that are meant to. The `DELETE` first collapses any duplicates a
         * previous build already created, keeping the oldest - which is the one whose
         * timestamp says when the reader actually saved it.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Keep the earliest bookmark per verse; later ones are the duplicates.
                db.execSQL(
                    """
                    DELETE FROM bookmarks
                    WHERE id NOT IN (
                        SELECT MIN(id) FROM bookmarks GROUP BY surahNumber, ayahNumber
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_bookmarks_reference ON bookmarks (surahNumber, ayahNumber)"
                )
                // Nothing read this table after LocationStore became the one authority.
                db.execSQL("DROP TABLE IF EXISTS cached_locations")
            }
        }

        fun getDatabase(context: Context): SalahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalahDatabase::class.java,
                    "salah_quiet.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Only reached when there is no path from the installed version -
                    // which for a shipped app means a downgrade or a version nobody
                    // wrote a migration for. It is still destructive, and that is now a
                    // real cost rather than a theoretical one, because the database holds
                    // the reader's bookmarks and reading position. Anything that adds a
                    // version must add a migration here.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
