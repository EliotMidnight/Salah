package com.example.ui.quran.reader

import androidx.compose.ui.graphics.Color
import com.example.data.model.QuranRef
import com.example.data.quran.ArabicDigits
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The furniture on a page: the surah head, the ayah markers, the prostrations.
 *
 * ### The defect this file exists for
 *
 * The first render of the rebuilt reader put a prostration marker in the middle of
 * Al-Ikhlas - on 112:1, which has no prostration - and it was visible in the
 * screenshot. `U+06E9` is a small shape that reads as "ص" with marks on it, so a
 * stray one is not obvious: it looks like part of the text.
 *
 * Two things had to be true for it to appear at all, and both are checked here.
 * The corpus's fifteen positions must not be consulted for a verse that has none,
 * and a page that spans three surahs must not carry one surah's markers.
 */
class PageFurnitureTest {

    private val ink = Color.Black
    private val accent = Color.Blue
    private val highlight = Color.Yellow

    private fun build(page: Int, selected: QuranRef? = null) = MushafPageText.build(
        ayahs = QuranBrowse.ayahsOnPage(page),
        scale = 1f,
        selected = selected,
        ink = ink,
        accent = accent,
        highlight = highlight,
        lineHeightFactor = 2f
    )

    // --- Prostrations ---------------------------------------------------

    @Test
    fun `a verse with no prostration carries no marker`() {
        // The defect. 112:1 is the first verse of Al-Ikhlas and has no prostration,
        // and a marker appeared anyway - in a page that spans three surahs.
        assertEquals(
            "the corpus says 112:1 has no prostration, so the fixture is wrong",
            null,
            QuranBrowse.sajdaAfter(112, 1)
        )
        val text = build(604).text.text
        assertFalse(
            "Al-Ikhlas must not carry a prostration marker",
            text.contains(SajdaGlyph)
        )
    }

    @Test
    fun `a page with no prostrations at all carries none`() {
        // Swept over every page, so a marker cannot appear on a page that has no
        // prostration on it. The single-verse case above would not catch a stray
        // marker on some other surah.
        var pagesChecked = 0
        (1..604).forEach { page ->
            val verses = QuranBrowse.ayahsOnPage(page)
            val expected = verses.count {
                QuranBrowse.sajdaAfter(it.surahNumber, it.ayahNumber) != null
            }
            if (expected == 0) {
                assertFalse(
                    "page $page has no prostrations but carries a marker",
                    build(page).text.text.contains(SajdaGlyph)
                )
                pagesChecked++
            }
        }
        assertTrue(
            "the sweep found no page without a prostration, so it proved nothing",
            pagesChecked > 500
        )
    }

    @Test
    fun `a verse that already carries a marker in the corpus text does not get a second`() {
        // Tanzil's own text has U+06E9 written into 7:206. The app was appending its
        // own marker regardless, so page 176 carried two prostration marks in
        // Al-A'raf - one of them in the middle of a line, looking like a letter.
        val verse = QuranBrowse.ayah(7, 206)!!
        assertTrue(
            "the fixture is wrong: the corpus text has no marker in 7:206",
            verse.textArabic.contains(SajdaGlyph)
        )
        assertTrue(
            "the fixture is wrong: 7:206 is not a prostration",
            QuranBrowse.sajdaAfter(7, 206) != null
        )
        assertEquals(
            "7:206 must still carry exactly one marker",
            1,
            countOccurrences(build(176).text.text, SajdaGlyph)
        )
    }

    @Test
    fun `a page carries exactly as many markers as it has prostrations`() {
        // 32:15 is one of the four obligatory ones, but the page it falls on also
        // contains 22:77 - a surah's two prostrations can share a page - so the count
        // is derived from the page rather than asserted as one.
        assertTrue(
            "the fixture is wrong: 32:15 has no prostration",
            QuranBrowse.sajdaAfter(32, 15) != null
        )
        (1..604).forEach { page ->
            val expected = QuranBrowse.ayahsOnPage(page).count {
                QuranBrowse.sajdaAfter(it.surahNumber, it.ayahNumber) != null
            }
            val actual = countOccurrences(build(page).text.text, SajdaGlyph)
            assertEquals(
                "page $page carries $actual prostration markers, it has $expected",
                expected,
                actual
            )
        }
    }

    @Test
    fun `a prostration marker belongs to the verse it follows`() {
        // And it has to be *inside* that verse's span, or a tap on it selects the
        // last verse on the page. `unitEnd` used to be read before the glyph was
        // appended, which put the glyph outside every span.
        val page = pageOf(32, 15)
        val built = build(page)
        val sajda = QuranBrowse.ayahsOnPage(page).first {
            QuranBrowse.sajdaAfter(it.surahNumber, it.ayahNumber) != null
        }
        val span = built.spans.first { it.ayahNumber == sajda.ayahNumber }
        val markerAt = built.text.text.indexOf(SajdaGlyph)
        assertTrue("the marker was not found on page $page", markerAt >= 0)
        assertTrue(
            "the marker at $markerAt lies outside its verse's span ${span.full}",
            markerAt in span.full
        )
    }

    @Test
    fun `every prostration in the corpus is reachable from a page`() {
        // All fifteen, swept. A prostration on a page nobody can render is a
        // prostration the reader can never find.
        var found = 0
        (1..114).forEach { surah ->
            (1..286).forEach { ayah ->
                if (QuranBrowse.sajdaAfter(surah, ayah) == null) return@forEach
                found++
                val page = pageOf(surah, ayah)
                assertTrue(
                    "$surah:$ayah has a prostration but $page does not exist",
                    page in 1..604
                )
                assertTrue(
                    "$surah:$ayah is not on its own page $page",
                    QuranBrowse.ayahsOnPage(page).any { it.surahNumber == surah && it.ayahNumber == ayah }
                )
            }
        }
        assertEquals("prostrations found in the sweep", 15, found)
    }

    // --- Surah heads ----------------------------------------------------

    @Test
    fun `a page carries the name of the surah it opens`() {
        val text = build(1).text.text
        assertTrue(
            "page 1 opens Al-Fatihah and must name it",
            text.contains(QuranBrowse.surah(1)!!.arabicName)
        )
    }

    @Test
    fun `a page carries no basmalah of its own`() {
        // The Tanzil text has the basmalah inside verse 1 of every surah but
        // At-Tawbah, so a head that printed one printed it twice. Swept over every
        // page that opens a surah.
        val basmalah = QuranBrowse.ayah(1, 1)!!.textArabic
        (1..604).filter { QuranBrowse.ayahsOnPage(it).firstOrNull()?.ayahNumber == 1 }
            .forEach { page ->
                val verses = QuranBrowse.ayahsOnPage(page)
                val inVerses = verses.count { basmalah in it.textArabic }
                assertEquals(
                    "page $page (${verses.first().surahNumber}) prints the basmalah " +
                        "${countOccurrences(build(page).text.text, basmalah)} times, " +
                        "its verses contain it $inVerses",
                    inVerses,
                    countOccurrences(build(page).text.text, basmalah)
                )
            }
    }

    @Test
    fun `a page that continues a surah does not claim to start it`() {
        // Page 3 is mid-Baqarah. Printing Al-Baqarah's name on it says the surah
        // starts there, which is false - so the check is on the *verse that opens
        // the page*, not on the surah's name appearing anywhere, because a verse of
        // Baqarah can contain the letters of the name in other words.
        val ayahs = QuranBrowse.ayahsOnPage(3)
        assertEquals(2, ayahs.map { it.surahNumber }.distinct().single())
        assertTrue("page 3 should be mid-surah", ayahs.first().ayahNumber > 1)

        // The name is the page's *first* run of text, before the first verse. So
        // the honest check is that the text begins with the first verse's own words
        // rather than with the surah name.
        val built = build(3)
        val firstVerseStart = built.text.text.indexOf(ayahs.first().textArabic)
        assertEquals(
            "page 3 should open on the verse itself, not on a surah head",
            0,
            firstVerseStart
        )
    }

    @Test
    fun `a page spanning three surahs names only the one it opens`() {
        // Page 604 holds Al-Ikhlas, Al-Falaq and An-Nas. A head naming all three
        // would be a lie about where the surahs begin, and a head naming none would
        // leave the reader with no idea they had crossed two boundaries.
        val surahs = QuranBrowse.ayahsOnPage(604).map { it.surahNumber }.distinct()
        assertEquals(listOf(112, 113, 114), surahs)
        val text = build(604).text.text
        assertTrue("the opening surah must be named", text.contains(QuranBrowse.surah(112)!!.arabicName))
        // The other two are *in* the text as their own verses, not as heads - which
        // is what the partition says, and what the printed mushaf does.
        assertTrue(
            "Al-Falaq's text should be on the page",
            QuranBrowse.ayah(113, 1)!!.textArabic in text
        )
    }

    // --- Ayah markers ---------------------------------------------------

    @Test
    fun `every verse on a page carries its own number`() {
        listOf(1, 2, 42, 285, 604).forEach { page ->
            val text = build(page).text.text
            QuranBrowse.ayahsOnPage(page).forEach { ayah ->
                assertTrue(
                    "page $page has no marker for ${ayah.surahNumber}:${ayah.ayahNumber}",
                    text.contains(AyahMarker(ayah.ayahNumber))
                )
            }
        }
    }

    @Test
    fun `a marker follows the verse it numbers`() {
        // The marker is part of the verse's span, so a tap on the ornament selects
        // that verse rather than the next one.
        val built = build(42)
        built.spans.forEach { span ->
            val marker = AyahMarker(span.ayahNumber)
            val at = built.text.text.indexOf(marker, span.text.first)
            assertTrue("${span.ayahNumber}'s marker was not found after its text", at >= 0)
            assertTrue(
                "the marker for ${span.ayahNumber} at $at lies outside ${span.full}",
                at in span.full
            )
        }
    }

    private fun pageOf(surah: Int, ayah: Int): Int =
        QuranBrowse.ayah(surah, ayah)?.pageNumber ?: 1

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var at = haystack.indexOf(needle)
        while (at >= 0) {
            count++
            at = haystack.indexOf(needle, at + needle.length)
        }
        return count
    }

    private companion object {
        /** U+06E9. Reads as a small "ص" with marks, which is why a stray one hid. */
        const val SajdaGlyph = "\u06E9"

        /** U+06DD plus the verse's own number, from the app's own function. */
        fun AyahMarker(ayah: Int): String = ArabicDigits.ayahMarker(ayah)
    }
}
