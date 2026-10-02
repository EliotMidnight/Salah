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
    version = 4,
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

        /**
         * **3 to 4.** Drops `continue_reading.snippetAr` and `continue_reading.surahNameAr`.
         *
         * Both were written on every verse viewed and read by nothing, and one of them
         * held a hand-typed copy of Quranic text that had already drifted from the corpus
         * - the copy lacked the tatweel the corpus writes before the dagger alif in
         * الرحمن. A second copy of the Book is worse than a missing one, so both go.
         *
         * ### Why the table is recreated rather than `ALTER TABLE ... DROP COLUMN`
         *
         * `DROP COLUMN` needs SQLite 3.35, which is Android 12. This app supports API 24,
         * so on every device below 12 the statement is a syntax error and the migration
         * throws - taking the reader's bookmarks and reading position with it, because the
         * only recovery Room has is `fallbackToDestructiveMigration`. The
         * create-copy-drop-rename dance works on every SQLite there is, which is why it is
         * written out rather than being neat.
         *
         * The column list is spelled rather than derived, which is what Room's own
         * generated migrations do for the same reason: it has to match the entity exactly
         * or the schema check fails on open, and a check that fires on a user's device
         * rather than in a test is the thing worth avoiding.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `continue_reading_new` (
                        `id` INTEGER NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `surahName` TEXT NOT NULL,
                        `pageNumber` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `continue_reading_new`
                        (`id`, `surahNumber`, `ayahNumber`, `surahName`, `pageNumber`,
                         `timestamp`)
                    SELECT `id`, `surahNumber`, `ayahNumber`, `surahName`, `pageNumber`,
                           `timestamp`
                    FROM `continue_reading`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `continue_reading`")
                db.execSQL("ALTER TABLE `continue_reading_new` RENAME TO `continue_reading`")
            }
        }

        fun getDatabase(context: Context): SalahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalahDatabase::class.java,
                    "salah_quiet.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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
