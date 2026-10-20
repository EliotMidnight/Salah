package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrayerLogEntity::class
    ],
    version = 7,
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
         * ### Why the table is recreated rather than `ALTER TABLE ... DROP COLUMN`
         *
         * `DROP COLUMN` needs SQLite 3.35, which is Android 12. This app supports API 24,
         * so on every device below 12 the statement is a syntax error and the migration
         * throws - taking the reader's bookmarks and reading position with it, because the
         * only recovery Room has is `fallbackToDestructiveMigration`. The
         * create-copy-drop-rename dance works on every SQLite there is, which is why it is
         * written out rather than being neat.
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

        /**
         * **4 to 5.** Drops `bookmarks.surahName` and `bookmarks.ayahSnippet`.
         *
         * Both were written on every save and read by nothing: the Saved list resolves the
         * surah's name and the verse's text through the corpus from the row's reference. A
         * copy of the verse kept beside the reference could only drift from the corpus,
         * while nothing gated or checked it.
         *
         * The table is recreated rather than columns dropped for the reason spelled out on
         * [MIGRATION_3_4] - `DROP COLUMN` needs SQLite 3.35, which is Android 12. The
         * unique index dies with the table it indexes, so it is recreated by name here
         * too: it is declared on `BookmarkEntity`, and Room refuses to open a database
         * whose schema is missing it.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `bookmarks_new`
                        (`id`, `surahNumber`, `ayahNumber`, `timestamp`)
                    SELECT `id`, `surahNumber`, `ayahNumber`, `timestamp`
                    FROM `bookmarks`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `bookmarks`")
                db.execSQL("ALTER TABLE `bookmarks_new` RENAME TO `bookmarks`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_bookmarks_reference ON bookmarks (surahNumber, ayahNumber)"
                )
            }
        }

        /**
         * **5 to 6.** Drops `continue_reading.surahName`.
         *
         * It was the surah's English name, written on every verse the reader settled on
         * and printed back by the Continue Reading card - so the card named the surah in
         * English in every one of the eleven languages, and the name was a stored copy of
         * a fact the names table already owns. The card resolves the name from that table
         * in the interface's own language now; the row keeps the reference, the page and
         * the time.
         *
         * The table is recreated rather than the column dropped for the reason spelled
         * out on [MIGRATION_3_4] - `DROP COLUMN` needs SQLite 3.35, which is Android 12.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `continue_reading_new` (
                        `id` INTEGER NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `pageNumber` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `continue_reading_new`
                        (`id`, `surahNumber`, `ayahNumber`, `pageNumber`, `timestamp`)
                    SELECT `id`, `surahNumber`, `ayahNumber`, `pageNumber`, `timestamp`
                    FROM `continue_reading`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `continue_reading`")
                db.execSQL("ALTER TABLE `continue_reading_new` RENAME TO `continue_reading`")
            }
        }

        /**
         * **6 to 7.** Drops the Quran reader's `bookmarks` and `continue_reading`
         * tables now that the reader is gone. `prayer_logs` is untouched, so the
         * reader's removal costs no prayer-completion data.
         *
         * The tables are dropped rather than recreated: there is no entity left to
         * validate, and a `DROP TABLE` is the one statement that cannot drift from
         * the schema Room expects.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `bookmarks`")
                db.execSQL("DROP TABLE IF EXISTS `continue_reading`")
            }
        }

        fun getDatabase(context: Context): SalahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalahDatabase::class.java,
                    "salah_quiet.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    // Only reached when there is no path from the installed version -
                    // which for a shipped app means a downgrade or a version nobody
                    // wrote a migration for. It is still destructive, and that is now a
                    // real cost rather than a theoretical one, because the database
                    // holds the reader's prayer logs.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
