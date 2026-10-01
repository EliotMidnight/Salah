package com.example.ui.quran.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDirection
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The text of a page, and the spans that make one string navigable.
 *
 * ### What can go wrong in a single string
 *
 * A mushaf page is built as one `AnnotatedString` so that it wraps, selects and
 * *measures* as a block - which is what lets the reader know whether it fits. The
 * cost is that a string has no structure of its own, so everything a reader can do
 * with a verse - select it, highlight it, copy it, find which verse a tap landed
 * on - depends on the spans recorded while it was built. If a span is off by one,
 * a reader selects the wrong verse and copies the wrong words, and nothing about
 * the picture reveals it.
 *
 * These are the checks that a page's spans agree with the page's text.
 */
class MushafPageTest {

    private val ink = Color.Black
    private val accent = Color.Blue
    private val highlight = Color.Yellow

    /**
     * The basmalah exactly as the page prints it, read from the corpus.
     *
     * Not typed here, and the reason is specific: the Tanzil Uthmani text spells
     * the basmalah with a tatweel inside it - ٱلرَّحْمَـٰنِ carries U+0640 - so a
     * literal copied by hand is a different string and every count against it is
     * zero, which reads as a pass until the assertion is inverted.
     */
    private val BASMALAH_IN_USE = QuranBrowse.ayah(1, 1)!!.textArabic

    private fun build(
        page: Int,
        scale: Float = 1f,
        selected: com.example.data.model.QuranRef? = null
    ) = MushafPageText.build(
        ayahs = QuranBrowse.ayahsOnPage(page),
        scale = scale,
        selected = selected,
        ink = ink,
        accent = accent,
        highlight = highlight,
        lineHeightFactor = 2f
    )

    // --- The text ---------------------------------------------------------

    @Test
    fun `every verse of the page is in the page`() {
        listOf(1, 2, 42, 285, 604).forEach { page ->
            val ayahs = QuranBrowse.ayahsOnPage(page)
            val built = build(page)
            assertEquals(
                "page $page has the wrong number of spans",
                ayahs.size,
                built.spans.size
            )
        }
    }

    @Test
    fun `every span covers real characters of the text`() {
        // The check that catches an off-by-one: a span that runs past the end of
        // the string, or that is empty, or that runs backwards.
        listOf(1, 2, 42, 285, 604).forEach { page ->
            val built = build(page)
            val length = built.text.length
            built.spans.forEach { span ->
                assertTrue(
                    "page $page span ${span.ayahNumber} is empty",
                    !span.text.isEmpty()
                )
                assertTrue(
                    "page $page span ${span.ayahNumber} starts past the end: " +
                        "${span.text.first} of $length",
                    span.text.first >= 0
                )
                assertTrue(
                    "page $page span ${span.ayahNumber} ends past the text: " +
                        "${span.text.last} of $length",
                    span.text.last < length
                )
            }
        }
    }

    @Test
    fun `spans are in reading order and do not overlap`() {
        // Two spans claiming the same character means one verse's tap selects
        // another, and one verse's copy includes another's words.
        val built = build(285)
        var previousEnd = 0
        built.spans.forEach { span ->
            assertTrue(
                "span ${span.ayahNumber} starts at ${span.text.first}, " +
                    "before the previous span ended at $previousEnd",
                span.text.first >= previousEnd
            )
            previousEnd = span.text.last + 1
        }
    }

    @Test
    fun `a span covers exactly its own verse and nothing else`() {
        // Not "contains" - *exactly*. A span that runs on into the next verse
        // would highlight both when one is selected.
        val page = 42
        val ayahs = QuranBrowse.ayahsOnPage(page)
        val built = build(page)
        ayahs.forEachIndexed { index, ayah ->
            val span = built.spans[index]
            val covered = built.text.substring(span.text.first, span.text.last + 1)
            assertEquals(
                "page $page span for ${ayah.surahNumber}:${ayah.ayahNumber} " +
                    "covers the wrong characters",
                ayah.textArabic,
                covered
            )
        }
    }

    @Test
    fun `a span is never longer than the whole unit`() {
        // `full` includes the ayah marker and `text` does not. If the order were
        // ever reversed, the marker would be inside the highlighted words and a
        // selected verse would show its number as part of itself.
        val built = build(42)
        built.spans.forEach { span ->
            assertTrue(
                "the words of ${span.ayahNumber} are not inside its own unit",
                span.text.first >= span.full.first
            )
            assertTrue(
                "the unit of ${span.ayahNumber} does not contain its words",
                span.text.last <= span.full.last
            )
        }
    }

    @Test
    fun `every span carries the surah as well as the ayah`() {
        // Page 604 holds three surahs, and page 285 crosses two. A span with only
        // an ayah number is ambiguous there, and "ayah 1" on page 604 is three
        // different verses.
        val built = build(604)
        val surahs = built.spans.map { it.surahNumber }.distinct()
        assertTrue("page 604 should span several surahs", surahs.size >= 3)
        assertTrue(
            "the first verse of each surah should be present",
            built.spans.count { it.ayahNumber == 1 } >= 3
        )
    }

    // --- Resolving a tap --------------------------------------------------

    @Test
    fun `an offset inside a verse resolves to that verse`() {
        val ayahs = QuranBrowse.ayahsOnPage(42)
        val built = build(42)
        ayahs.forEachIndexed { index, ayah ->
            val span = built.spans[index]
            // The middle of the verse's own characters, which is unambiguously
            // inside it and unambiguously not inside any other.
            val middle = (span.text.first + span.text.last) / 2
            val found = built.verseAt(middle)
            assertNotNull("offset $middle resolved to nothing", found)
            assertEquals(
                "offset $middle in ${ayah.surahNumber}:${ayah.ayahNumber} " +
                    "resolved to ${found!!.surahNumber}:${found.ayahNumber}",
                ayah.surahNumber to ayah.ayahNumber,
                found.surahNumber to found.ayahNumber
            )
        }
    }

    @Test
    fun `an offset on a verse's marker resolves to that verse`() {
        // The marker is in `full` but not in `text`, so a tap on the ornament
        // resolves only because the lookup uses `full`. A reader tapping the ayah
        // circle is tapping the verse.
        val built = build(42)
        val span = built.spans.first()
        val markerOnly = span.full.last
        val found = built.verseAt(markerOnly)
        assertNotNull(found)
        assertEquals(span.ayahNumber, found!!.ayahNumber)
    }

    @Test
    fun `an offset in the surah head resolves to the first verse`() {
        // A tap on the surah name or the basmalah, which precede the first verse,
        // belongs to the first verse - not to nothing, and not to the second.
        val built = build(1)
        val first = built.spans.first()
        val found = built.verseAt(first.text.first - 1)
        assertNotNull("a tap on the surah head resolved to nothing", found)
        assertEquals(first.ayahNumber, found!!.ayahNumber)
    }

    @Test
    fun `an offset before the text resolves to the first verse rather than crashing`() {
        // A tap cannot normally produce a negative offset, but a stale layout can
        // - and the old version returned the *current selection* in that case,
        // which meant a tap during the first frame re-selected what was already
        // selected. Returning the first verse is a real answer.
        val built = build(1)
        assertEquals(built.spans.first().ayahNumber, built.verseAt(-50)?.ayahNumber)
    }

    @Test
    fun `an offset past the end resolves to the last verse`() {
        // A blank indicator at the end of a page reads as a broken one, and a tap
        // that lands in the margin after the last word is the reader's.
        val built = build(1)
        assertEquals(
            built.spans.last().ayahNumber,
            built.verseAt(built.text.length + 100)?.ayahNumber
        )
    }

    @Test
    fun `a page with no spans resolves nothing`() {
        val empty = MushafPageText.build(
            ayahs = emptyList(),
            scale = 1f,
            selected = null,
            ink = ink,
            accent = accent,
            highlight = highlight,
            lineHeightFactor = 2f
        )
        assertEquals(null, empty.verseAt(0))
        assertEquals(null, empty.verseAt(10))
    }

    // --- Direction and the head -------------------------------------------

    @Test
    fun `the page is set right to left regardless of the interface language`() {
        // Quranic text is RTL by script, not by the reader's language. An English
        // or French reader still needs the Arabic laid out RTL, and this is a
        // property of the page's paragraph style rather than of the app's locale -
        // which is why it lives here and not in a global direction provider.
        val built = build(2)
        val paragraph = built.text.paragraphStyles.firstOrNull()?.item
        assertNotNull("the page has no paragraph style", paragraph)
        assertEquals(
            "the page must be laid out right to left",
            TextDirection.Rtl,
            paragraph!!.textDirection
        )
    }

    @Test
    fun `a page that opens a surah carries its name`() {
        // Page 1 opens Al-Fatihah, so its name has to be on the page.
        val built = build(1)
        assertTrue(
            "page 1 does not name Al-Fatihah",
            built.text.text.contains(QuranBrowse.surah(1)!!.arabicName)
        )
    }

    @Test
    fun `a page that continues a surah does not claim to start it`() {
        // Page 3 continues Al-Baqarah. Printing Al-Baqarah's name on it says the
        // surah starts there, which is false and is the mistake a "print the head
        // on every page" version makes. The *running* head is chrome and lives in
        // the reader; this is the text of the page.
        val ayahs = QuranBrowse.ayahsOnPage(3)
        assertEquals(
            "page 3 was expected to be mid-surah",
            2,
            ayahs.map { it.surahNumber }.distinct().single()
        )
        assertTrue(
            "the first verse of page 3 is not verse 1",
            ayahs.first().ayahNumber > 1
        )
    }

    @Test
    fun `Al-Fatihah's basmalah is not printed twice`() {
        // In surah 1 the basmalah *is* verse 1, so printing it as a head as well
        // prints the first line of the book twice on the same page.
        //
        // Counted against the *corpus's own* basmalah rather than a literal typed
        // here, because the two differ by a tatweel: the Tanzil text spells
        // ٱلرَّحْمَـٰنِ with U+0640 in the middle, and a fixture copied from memory
        // without it counts zero and looks like a pass until the assertion is
        // inverted. The point of the test is the *count*, not the spelling.
        val firstVerse = QuranBrowse.ayah(1, 1)!!
        val built = build(1)
        assertEquals(
            "the basmalah appears more than once on the opening page",
            1,
            countOccurrences(built.text.text, firstVerse.textArabic)
        )
    }

    @Test
    fun `a surah whose first verse carries the basmalah does not get a second one`() {
        // The consequential one. The Tanzil text puts the basmalah *inside* verse 1
        // of 111 of the 114 surahs, so a page head that prints its own basmalah
        // prints it twice - once as furniture, once at the start of the first
        // verse. The previous reader did this on every surah except Al-Fatihah and
        // At-Tawbah, and avoided those two only by hard-coding them as exceptions -
        // a fact about two surahs someone remembered, standing in for a fact about
        // the data nobody had read.
        val ayahs = QuranBrowse.ayahsOnPage(2)
        val first = ayahs.first()
        assertEquals("page 2 should open Al-Baqarah", 2, first.surahNumber)
        assertTrue(
            "the fixture is wrong: 2:1 does not carry the basmalah",
            BASMALAH_IN_USE in first.textArabic
        )

        assertEquals(
            "the basmalah appears twice on the page that opens Al-Baqarah",
            1,
            countOccurrences(build(2).text.text, BASMALAH_IN_USE)
        )
    }

    @Test
    fun `no page prints a basmalah its verses do not already contain`() {
        // Swept over every page that opens a surah, because "prints the basmalah
        // twice" is a property of the head, not of one page - a fixture on page 2
        // alone would not catch it being reintroduced for some other surah.
        val openingPages = (1..604).filter { page ->
            QuranBrowse.ayahsOnPage(page).firstOrNull()?.ayahNumber == 1
        }
        assertTrue("expected many pages to open a surah", openingPages.size > 50)

        openingPages.forEach { page ->
            val ayahs = QuranBrowse.ayahsOnPage(page)
            val inVerses = ayahs.count { BASMALAH_IN_USE in it.textArabic }
            val inPage = countOccurrences(build(page).text.text, BASMALAH_IN_USE)
            assertEquals(
                "page $page (${ayahs.first().surahNumber}:${ayahs.first().ayahNumber}) " +
                    "prints the basmalah $inPage times, its verses contain it $inVerses",
                inVerses,
                inPage
            )
        }
    }

    @Test
    fun `At-Tawbah has no basmalah at all`() {
        // The one surah in the corpus whose first verse has none. Printing one
        // there would be inventing text, and it is the case the old two-surah
        // exception list was really covering.
        val page = (1..604).first { candidate ->
            QuranBrowse.ayahsOnPage(candidate).firstOrNull()?.surahNumber == 9
        }
        assertEquals(
            "At-Tawbah opened on page $page and must carry no basmalah",
            0,
            countOccurrences(build(page).text.text, BASMALAH_IN_USE)
        )
        assertFalse(
            "At-Tawbah must not be reported as having a basmalah",
            MushafPageText.hasBasmalah(9)
        )
        assertEquals("", MushafPageText.leadingBasmalahOf(9))
    }

    @Test
    fun `every surah but At-Tawbah carries the basmalah inside its first verse`() {
        // 113 of 114, and the one without is At-Tawbah - the surah the tradition
        // says has no basmalah at all. This was measured by *byte* comparison
        // first, which reported 111 and implicated Al-Tin and Al-Qadr; both open
        // their basmalah with a shadda on the ba, so a byte comparison against the
        // plain spelling misses them. Folding is what finds all 113.
        //
        // The count is asserted from the corpus rather than from a list so a
        // re-import that changed it would be visible here.
        val withBasmalah = (1..114).filter { MushafPageText.hasBasmalah(it) }
        assertEquals(
            "surahs whose first verse opens with the basmalah",
            113,
            withBasmalah.size
        )
        assertEquals(
            "the only surah without one is At-Tawbah",
            listOf(9),
            (1..114).filterNot { MushafPageText.hasBasmalah(it) }
        )
        // The two shadda spellings, which a byte comparison reports as absent.
        assertTrue("Al-Tin's basmalah was not recognised", MushafPageText.hasBasmalah(95))
        assertTrue("Al-Qadr's basmalah was not recognised", MushafPageText.hasBasmalah(97))
    }

    @Test
    fun `the extracted basmalah is the verses own text not a folded copy`() {
        // Printed text has to keep its tashkeel. Folding is for comparison only,
        // and a folded copy reaching a page would strip the marks from the first
        // line of the book.
        val canonical = QuranBrowse.ayah(1, 1)!!.textArabic
        assertEquals(
            "the extracted basmalah differs from the corpus's own text",
            canonical,
            MushafPageText.leadingBasmalahOf(1)
        )
        assertTrue(
            "the extracted basmalah should still carry its tashkeel",
            MushafPageText.leadingBasmalahOf(1).length >= 20
        )
        // And it is a prefix of the verse, for every surah that has one.
        for (surah in listOf(2, 3, 19, 95, 97, 114)) {
            val verse = QuranBrowse.ayah(surah, 1)!!.textArabic
            assertTrue(
                "surah $surah's basmalah is not a prefix of its own verse 1",
                verse.startsWith(MushafPageText.leadingBasmalahOf(surah))
            )
        }
    }

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

    // --- Selection -------------------------------------------------------

    @Test
    fun `selecting a verse does not change the spans`() {
        // Selection is a *style* on the text, not a change to it. If it moved the
        // spans, then selecting a verse and deselecting it would leave the page in
        // a slightly different state, and a reader who selected every verse in
        // turn would eventually get wrong taps.
        val plain = build(42)
        val selected = build(42, selected = com.example.data.model.QuranRef(2, 3, 42))
        assertEquals(
            plain.spans.map { it.text },
            selected.spans.map { it.text }
        )
        assertEquals(plain.text.text, selected.text.text)
    }

    @Test
    fun `selecting a verse that is not on the page changes nothing`() {
        val plain = build(42)
        val elsewhere = build(42, selected = com.example.data.model.QuranRef(114, 6, 604))
        assertEquals(plain.text.text, elsewhere.text.text)
    }
}
