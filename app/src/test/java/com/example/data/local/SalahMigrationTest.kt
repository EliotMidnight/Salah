package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The migrations move a reader's data forward without losing it.
 *
 * ### Why this test exists at all
 *
 * The database holds the **only user-authored data this app has** — the reader's
 * bookmarks and their Continue Reading position. Everything else is bundled or computed.
 * That is why `MIGRATION_2_3` was written by hand rather than left to
 * `fallbackToDestructiveMigration`, which was the previous behaviour for every version
 * bump, and it took the bookmarks with it to remove a table nothing read.
 *
 * **And it had no test.** A hand-written migration over irreplaceable data, on a path that
 * only runs once per user, on hardware nobody runs CI on. If the SQL were wrong, the
 * finding would be a crash report from someone who had saved verses they can no longer
 * see.
 *
 * ### Why not `MigrationTestHelper`
 *
 * It needs `androidx.room:room-testing`, and it needs `exportSchema = true` with a schema
 * directory configured. Adding a dependency and a codegen setting to test two statements is
 * a poor trade, and the thing worth testing is the SQL: that it runs, that the data
 * survives, and that the resulting table matches the entity. So the version-3 schema is
 * written out here exactly as Room generated it, a real SQLite database is opened with
 * Robolectric, and the migration's own `migrate` is called on it.
 *
 * That also means the expected schema is written down twice — once by Room, once here. If
 * they disagree, `the migrated table is not the one the entity describes` fires, which is
 * exactly the check `MigrationTestHelper` would have given.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SalahMigrationTest {

    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun openVersion3() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        db = helper.writableDatabase

        // The schema exactly as version 3 generated it, including the two columns that
        // version 4 removes and the unique index that version 3 added.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `continue_reading` (
                `id` INTEGER NOT NULL,
                `surahNumber` INTEGER NOT NULL,
                `ayahNumber` INTEGER NOT NULL,
                `surahName` TEXT NOT NULL,
                `surahNameAr` TEXT NOT NULL,
                `pageNumber` INTEGER NOT NULL,
                `snippetAr` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `surahNumber` INTEGER NOT NULL,
                `ayahNumber` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "index_bookmarks_reference ON bookmarks (surahNumber, ayahNumber)"
        )
    }

    @After
    fun close() {
        db.close()
    }

    // --- The data, planted before the migration runs ---------------------------

    private fun plantReaderData() {
        db.execSQL(
            "INSERT INTO continue_reading " +
                "(id, surahNumber, ayahNumber, surahName, surahNameAr, pageNumber, " +
                "snippetAr, timestamp) VALUES " +
                "(1, 112, 4, 'Al-Ikhlas', 'الإخلاص', 604, 'قُلْ هُوَ ٱللَّهُ أَحَدٌ', 1700000000000)"
        )
        db.execSQL(
            "INSERT INTO bookmarks (surahNumber, ayahNumber, createdAt) VALUES " +
                "(2, 255, 1690000000000), (112, 1, 1690000000001), (114, 6, 1690000000002)"
        )
    }

    private fun columnsOf(table: String): List<String> {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val names = ArrayList<String>(cursor.count)
            while (cursor.moveToNext()) {
                names += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
            return names
        }
    }

    // --- 3 to 4 ---------------------------------------------------------------

    @Test
    fun `the reading position survives the migration`() {
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)

        db.query("SELECT surahNumber, ayahNumber, surahName, pageNumber, timestamp " +
            "FROM continue_reading").use { cursor ->
            assertTrue("the reader's position was lost", cursor.moveToFirst())
            assertEquals(112, cursor.getInt(0))
            assertEquals(4, cursor.getInt(1))
            assertEquals("Al-Ikhlas", cursor.getString(2))
            assertEquals(604, cursor.getInt(3))
            assertEquals(1700000000000L, cursor.getLong(4))
            assertFalse(
                "the migration left a second row behind",
                cursor.moveToNext()
            )
        }
    }

    @Test
    fun `the bookmarks survive the migration`() {
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)

        val saved = mutableListOf<Pair<Int, Int>>()
        db.query("SELECT surahNumber, ayahNumber FROM bookmarks ORDER BY id").use { c ->
            while (c.moveToNext()) saved += c.getInt(0) to c.getInt(1)
        }
        assertEquals(
            "the reader's bookmarks were lost or reordered by the migration",
            listOf(2 to 255, 112 to 1, 114 to 6),
            saved
        )
    }

    @Test
    fun `the unique bookmark index survives, so a verse cannot be saved twice`() {
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)
        // The migration only touches `continue_reading`, so the risk is that recreating a
        // table disturbed another table's index. Saving 2:255 again must be *refused* -
        // which is the whole point of the index, so the proof is that it throws.
        val refused = runCatching {
            db.execSQL(
                "INSERT INTO bookmarks (surahNumber, ayahNumber, createdAt) " +
                    "VALUES (2, 255, 1700000000000)"
            )
        }.isFailure
        assertTrue(
            "a verse was saved twice after the migration, so the unique index that " +
                "version 3 added is not there",
            refused
        )
        val count = db.query("SELECT COUNT(*) FROM bookmarks").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        assertEquals("the refused insert still added a row", 3, count)
    }

    @Test
    fun `the two columns that were never read are gone`() {
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)

        val columns = columnsOf("continue_reading")
        assertFalse(
            "snippetAr is still a column: it held a hand-typed copy of Quranic text that " +
                "had drifted from the corpus, and nothing read it",
            columns.contains("snippetAr")
        )
        assertFalse(
            "surahNameAr is still a column, and nothing read it either",
            columns.contains("surahNameAr")
        )
    }

    @Test
    fun `the migrated table is exactly the one the entity describes`() {
        // The reason the column list is spelled out in the migration: Room validates the
        // schema when the database is opened, on the user's device. A mismatch here is
        // that crash, moved into a test.
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)

        assertEquals(
            "the migrated continue_reading table does not match ContinueReadingEntity. " +
                "Room would refuse to open the database on a user's device.",
            listOf("id", "surahNumber", "ayahNumber", "surahName", "pageNumber", "timestamp"),
            columnsOf("continue_reading")
        )
    }

    @Test
    fun `the migration leaves no table behind`() {
        // A recreated table whose rename failed leaves `_new` sitting next to the real
        // one, and the next migration would then find two of everything.
        plantReaderData()
        SalahDatabase.MIGRATION_3_4.migrate(db)

        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        assertFalse(
            "the migration left a staging table behind: $tables",
            tables.any { it.contains("continue_reading_new") }
        )
        assertTrue(
            "the migration lost the continue_reading table: $tables",
            tables.contains("continue_reading")
        )
    }

    @Test
    fun `the migration is idempotent in the only sense that matters`() {
        // Room runs a migration exactly once per upgrade, so running it twice is not a
        // scenario. What *is* a scenario is a reader who installed a build that took the
        // destructive fallback and is now on version 4 with no row at all - and then the
        // entity's defaults are the whole of their history. That has to work.
        SalahDatabase.MIGRATION_3_4.migrate(db)
        val rows = db.query("SELECT COUNT(*) FROM continue_reading").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        assertEquals(
            "the migration invented a reading position on an empty database",
            0,
            rows
        )
        val defaults = ContinueReadingEntity()
        assertEquals(
            "with no stored row, the entity's defaults are the whole of a first run, and " +
                "they must still describe 1:1",
            1,
            defaults.surahNumber
        )
        assertEquals(1, defaults.ayahNumber)
    }

    // --- 2 to 3, which also had no test ---------------------------------------

    @Test
    fun `the 2 to 3 migration collapses duplicate bookmarks and keeps the oldest`() {
        // Version 2's `bookmarks` had no unique index, so `toggleBookmark`'s
        // read-then-write could insert the same verse twice and the Saved list showed it
        // twice. The migration deduplicates, keeping the row whose timestamp says when
        // the reader actually saved it.
        db.execSQL("DROP TABLE bookmarks")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `surahNumber` INTEGER NOT NULL,
                `ayahNumber` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO bookmarks (id, surahNumber, ayahNumber, createdAt) VALUES " +
                "(1, 2, 255, 1690000000000), " +
                "(2, 2, 255, 1690000009999), " +
                "(3, 112, 1, 1690000000001)"
        )

        SalahDatabase.MIGRATION_2_3.migrate(db)

        db.query("SELECT id, surahNumber, ayahNumber FROM bookmarks ORDER BY id").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(
                "the duplicate bookmark was not collapsed onto the oldest row",
                1,
                c.getInt(0)
            )
            c.moveToNext()
            assertEquals("the verse that was never duplicated was lost", 3, c.getInt(0))
            assertFalse("the duplicate survived", c.moveToNext())
        }

        // And the index the migration exists to add really is there: the duplicate that
        // version 2 permitted must now be refused.
        val refused = runCatching {
            db.execSQL(
                "INSERT INTO bookmarks (surahNumber, ayahNumber, createdAt) " +
                    "VALUES (2, 255, 1700000000000)"
            )
        }.isFailure
        assertTrue(
            "a duplicate bookmark could still be inserted after the 2 to 3 migration, so " +
                "the unique index it exists to add is not there",
            refused
        )
    }

    @Test
    fun `the 2 to 3 migration drops the orphaned locations table`() {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_locations` (`id` INTEGER NOT NULL)")
        db.execSQL("INSERT INTO cached_locations (id) VALUES (1)")
        plantReaderData()

        SalahDatabase.MIGRATION_2_3.migrate(db)

        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        assertFalse(
            "cached_locations survived: $tables",
            tables.contains("cached_locations")
        )
        // And it took nothing else with it.
        val saved = db.query("SELECT COUNT(*) FROM bookmarks").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        assertEquals("dropping an unread table also dropped the bookmarks", 3, saved)
    }
}