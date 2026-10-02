package com.example.data.local

import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The first-run Continue Reading row says what the corpus says, and holds no Quranic text.
 *
 * ### The defect
 *
 * `ContinueReadingEntity`'s defaults are what a first run has: with no row in the table,
 * the app still has to answer "where was I?", and the honest answer for someone who has
 * never read here is Al-Fatihah 1:1. That is a legitimate reason for the row to exist with
 * nothing behind it.
 *
 * It is **not** a legitimate reason for the row to hold a hand-typed copy of Quranic text.
 * It had a `snippetAr` carrying the Arabic of 1:1, written on every verse viewed — and
 * **read by nothing**. The Continue Reading card says `surahName` and
 * `surahNumber:ayahNumber`, and nothing else on any screen touched it.
 *
 * Worse, the copy had **already drifted**. The corpus spells الرحمن with a tatweel before
 * the dagger alif — U+0640 then U+0670, `ٱلرَّحْمَـٰنِ` — and the hand-typed default had
 * `ٱلرَّحْمَٰنِ`. Visually indistinguishable. Not the same string. And not the text of the
 * Book, in the one field in the entire app holding Quranic text that the SHA-256 gate did
 * not cover, one `SELECT` away from a reader's screen.
 *
 * ### Why deleting beats fixing
 *
 * Fixing the copy would have made it correct today. Deleting it means there is **no second
 * copy of the Book anywhere in the app to drift** — a stronger guarantee than a test
 * asserting the copy is currently right, and the reason `the row holds no Quranic text`
 * below is a test at all rather than a one-off edit.
 *
 * ### Why it was a copy rather than a read
 *
 * Because it was a Room entity's field initialiser. Reading the corpus there would parse
 * 6,236 verses and verify three digests inside a UI state's defaults, on every
 * construction. `LocationStore` reads `SharedPreferences` in a field initialiser for the
 * same reason and keeps the database off that path.
 *
 * The surah *name* stays, because a reader needs to recognise where they were and unlike
 * verse text it is a label rather than the Book — pinned below so it cannot drift either.
 */
class ContinueReadingDefaultsTest {

    /** The row a first run has: the default-constructed entity. */
    private val defaults = ContinueReadingEntity()

    private val firstVerse = QuranBrowse.ayah(1, 1)!!

    private val firstSurah = QuranBrowse.surah(1)!!

    @Test
    fun `the row holds no Quranic text`() {
        // The durable version of the fix. A field whose value is a verse of the Book is a
        // second copy of the corpus, and a second copy drifts: this one had already lost a
        // tatweel. Rather than assert the particular column is gone, this asserts the
        // *property* - no field of this row may be Quranic text - so the column cannot
        // come back under another name.
        val arabic = Regex("[\\u0600-\\u06FF]")

        for (field in ContinueReadingEntity::class.java.declaredFields) {
            val value = defaults.let { entity ->
                runCatching {
                    field.isAccessible = true
                    field.get(entity)
                }.getOrNull()
            }
            if (value !is String) continue
            assertFalse(
                "`${field.name}` holds Quranic text (\"${value.take(40)}\"). Verse text " +
                    "comes from the bundled corpus and from nowhere else - a second copy " +
                    "is how it drifts, and this one already had. A label is fine; the Book " +
                    "is not.",
                arabic.containsMatchIn(value)
            )
        }
    }

    @Test
    fun `the default names the surah the corpus names`() {
        assertEquals(
            "the first-run Continue Reading card names surah " +
                "\"${defaults.surahName}\" where the corpus names it " +
                "\"${firstSurah.englishName}\"",
            firstSurah.englishName,
            defaults.surahName
        )
    }

    @Test
    fun `the default points at a verse the corpus agrees is there`() {
        assertTrue(
            "the first-run Continue Reading row points at " +
                "${defaults.surahNumber}:${defaults.ayahNumber}, which is not a verse",
            QuranBrowse.ref(defaults.surahNumber, defaults.ayahNumber) != null
        )
        assertEquals(
            "the first-run Continue Reading row is on page ${defaults.pageNumber}, but " +
                "1:1 is on page ${QuranCorpus.pageOf(1, 1)}",
            QuranCorpus.pageOf(1, 1),
            defaults.pageNumber
        )
    }

    @Test
    fun `the default opens the book rather than a random spot`() {
        // The reason the row has defaults at all, and worth pinning: someone who has never
        // read here should be offered the opening surah, not the middle of Al-Baqarah. A
        // "sensible placeholder" change would be invisible on a fresh install to everyone
        // except the person reading it.
        assertEquals(1, defaults.surahNumber)
        assertEquals(1, defaults.ayahNumber)
        assertEquals(1, defaults.pageNumber)
        assertEquals(
            "a first run has no stored row, so the default's id must be the single row's",
            1,
            defaults.id
        )
    }

    @Test
    fun `the name the row stores is the one a reader would recognise`() {
        // Not "the row holds no Arabic" - the surah's name *is* Arabic for most of the
        // book's readers, and stripping it would make the card unreadable for them. This
        // pins that the stored name is the corpus's name rather than a hand-typed one, by
        // checking the default against the corpus.
        assertEquals(
            "the stored name is not the corpus's name for surah 1",
            firstSurah.englishName.lowercase(Locale.ROOT),
            defaults.surahName.lowercase(Locale.ROOT)
        )
        assertEquals(
            "Al-Fatihah is 1:1's surah and its name must not be a translation of it",
            "Al-Fatihah",
            QuranBrowse.surah(1)?.englishName
        )
    }
}