package com.example.data.local

import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The first-run Continue Reading row says what the corpus says, and holds no copy of it.
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
 * **read by nothing**. The Continue Reading card said the surah's name and
 * `surahNumber:ayahNumber`, and no screen touched the rest.
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
 * asserting the copy is currently right, and the reason the property test below exists at
 * all rather than being a one-off edit.
 *
 * ### The name was the same mistake, one step quieter
 *
 * The row also carried `surahName`, the English transliteration written on every verse. It
 * *was* read — the card printed it — but stored text is one language, so an Arabic
 * interface still read "Al-Baqarah 2:255", and no later reader could change that. The
 * names table owns the name (`QuranDataSource.surahName`) and the card asks it in the
 * interface's own language at the point of drawing; the column is gone and the row is
 * numbers. The property test below is the durable half — a string cannot come back under
 * another name.
 *
 * ### Why it was a copy rather than a read
 *
 * Because it was a Room entity's field initialiser. Reading the corpus there would parse
 * 6,236 verses and verify three digests inside a UI state's defaults, on every
 * construction. `LocationStore` reads `SharedPreferences` in a field initialiser for the
 * same reason and keeps the database off that path. The card's own read is the names
 * table — 114 rows and no verses — which is why it can afford to ask per language instead
 * of storing an answer.
 */
class ContinueReadingDefaultsTest {

    /** The row a first run has: the default-constructed entity. */
    private val defaults = ContinueReadingEntity()

    @Test
    fun `the row carries a reference and a time, and nothing to read`() {
        // The durable version of the fix. A field whose value copies something the app's
        // own table or the corpus owns is a second copy, and a second copy drifts:
        // `snippetAr` had already lost a tatweel, and `surahName` could only ever name one
        // language. Rather than assert two columns are gone, this asserts the *property* -
        // every field is a number - so neither can come back under another name.
        //
        // The leading-`$` skip is the compiler's own field (`$stable`), not the row's.
        var checked = 0
        for (field in ContinueReadingEntity::class.java.declaredFields) {
            if (field.name.startsWith("$")) continue
            checked++
            val type = field.type
            assertTrue(
                "`${field.name}` is a ${type.simpleName}. The card's surah name and the " +
                    "verse's text come from the names table and the bundled corpus, in " +
                    "the reader's own language; a stored string is how the copy drifts. " +
                    "A reference number, a page and a timestamp are the facts this row " +
                    "may hold.",
                type == Int::class.javaPrimitiveType || type == Long::class.javaPrimitiveType
            )
        }
        assertTrue(
            "the entity's fields could not be read, so the property was never checked",
            checked > 0
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
}
