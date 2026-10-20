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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SalahMigrationFromVersion2Test {

    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun openVersion2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v2-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_logs` (
                `dateString` TEXT NOT NULL,
                `fajrDone` INTEGER NOT NULL,
                `dhuhrDone` INTEGER NOT NULL,
                `asrDone` INTEGER NOT NULL,
                `maghribDone` INTEGER NOT NULL,
                `ishaDone` INTEGER NOT NULL,
                PRIMARY KEY(`dateString`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `surahNumber` INTEGER NOT NULL,
                `ayahNumber` INTEGER NOT NULL,
                `surahName` TEXT NOT NULL,
                `ayahSnippet` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )
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
            CREATE TABLE IF NOT EXISTS `cached_locations` (
                `id` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }

    @After
    fun close() {
        db.close()
    }

    private fun plantData() {
        db.execSQL(
            "INSERT INTO prayer_logs (dateString, fajrDone, dhuhrDone, asrDone, maghribDone, ishaDone) " +
                "VALUES ('2026-04-20', 1, 0, 1, 0, 1)"
        )
        db.execSQL(
            "INSERT INTO bookmarks (surahNumber, ayahNumber, surahName, ayahSnippet, timestamp) " +
                "VALUES (2, 255, 'Al-Baqarah', 'planted', 1690000000000)"
        )
        db.execSQL(
            "INSERT INTO continue_reading " +
                "(id, surahNumber, ayahNumber, surahName, surahNameAr, pageNumber, snippetAr, timestamp) " +
                "VALUES (1, 112, 4, 'Al-Ikhlas', 'الإخلاص', 604, 'قُلْ', 1700000000000)"
        )
    }

    private fun tables(): List<String> {
        val names = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) names += cursor.getString(0)
        }
        return names
    }

    @Test
    fun `an install from version 2 reaches version 7 without losing prayer logs`() {
        plantData()

        SalahDatabase.MIGRATION_2_3.migrate(db)
        SalahDatabase.MIGRATION_3_4.migrate(db)
        SalahDatabase.MIGRATION_4_5.migrate(db)
        SalahDatabase.MIGRATION_5_6.migrate(db)
        SalahDatabase.MIGRATION_6_7.migrate(db)

        db.query("SELECT fajrDone, dhuhrDone, asrDone, maghribDone, ishaDone " +
            "FROM prayer_logs WHERE dateString = '2026-04-20'").use { cursor ->
            assertTrue("the prayer log was lost", cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(1, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals(1, cursor.getInt(4))
        }

        val tables = tables()
        assertTrue("prayer_logs was lost: $tables", tables.contains("prayer_logs"))
        assertFalse("bookmarks survived the migration: $tables", tables.contains("bookmarks"))
        assertFalse("continue_reading survived the migration: $tables", tables.contains("continue_reading"))
        assertFalse("cached_locations survived the migration: $tables", tables.contains("cached_locations"))
    }
}
