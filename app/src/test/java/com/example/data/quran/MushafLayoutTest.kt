package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Corpus-wide properties of the Madinah mushaf's layout, held over all 604
 * pages and all 6,236 verses.
 *
 * ### Why properties, not examples
 *
 * A single example passes while a swapped verse goes unnoticed, and a print
 * replica is exactly where a swap hurts the most: the reader trusts that the
 * ink on this page and the verse they read elsewhere are the same verse. So
 * every check walks the whole layout, and the numbers it pins - line counts,
 * centred rows, wrapped markers, the 25 boundary attributions - are the
 * derivation's locked facts, not samples of it.
 *
 * ### What the numbers are
 *
 * 77,881 letter words (the corpus tokenised the way the print paints it, each
 * pause mark riding on the word before it), 9,046 printed lines (114 surah-name
 * rows, 112 basmalah rows, 8,820 text rows), 15 lines on every page but the
 * print's two short openers, 573 markers that print at a following line's head,
 * 30 centred text rows.
 */
class MushafLayoutTest {

    /** The word ordinals the derivation froze: the corpus's every letter word. */
    private val wordCount = 77_881

    /**
     * The print's centred text rows, as `(page, slot)`. The surah-name and
     * basmalah rows always are centred, so this ledger is what the print
     * centres beyond them.
     */
    private val centred = setOf(
        1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 1 to 7, 1 to 8,
        2 to 3, 2 to 4, 2 to 5, 2 to 6, 2 to 7, 2 to 8,
        255 to 2, 528 to 9, 534 to 6, 545 to 6, 586 to 1, 593 to 2, 594 to 5,
        600 to 10, 602 to 5, 602 to 11, 602 to 15, 603 to 10, 603 to 15,
        604 to 4, 604 to 9, 604 to 14, 604 to 15
    )

    /**
     * The 25 pages where the print and metadata.xml disagree about which verse
     * starts the page: the page number, then metadata's start, then the print's
     * - found by asking which verse-opening word the print sets first on each
     * page. Most are pages carrying a surah opening, where metadata credits the
     * page to the surah its header names while the print's first opener is
     * still the previous surah's tail; the 121-123/145 row and the 55 block are
     * where the print's line breaks genuinely sit elsewhere.
     */
    private val boundaryAttributions = listOf(
        Triple(121, 5 to 77, 5 to 78),
        Triple(122, 5 to 83, 5 to 84),
        Triple(123, 5 to 90, 5 to 91),
        Triple(145, 6 to 132, 6 to 131),
        Triple(532, 55 to 17, 55 to 19),
        Triple(533, 55 to 41, 55 to 42),
        Triple(534, 55 to 68, 55 to 70),
        Triple(565, 68 to 16, 68 to 17),
        Triple(568, 69 to 35, 69 to 36),
        Triple(570, 70 to 40, 70 to 41),
        Triple(576, 74 to 18, 74 to 19),
        Triple(584, 79 to 16, 79 to 17),
        Triple(586, 81 to 1, 80 to 41),
        Triple(588, 83 to 7, 83 to 5),
        Triple(589, 83 to 35, 83 to 34),
        Triple(590, 85 to 1, 84 to 25),
        Triple(592, 87 to 16, 87 to 11),
        Triple(593, 89 to 1, 88 to 23),
        Triple(594, 89 to 24, 89 to 23),
        Triple(595, 91 to 1, 90 to 19),
        Triple(596, 92 to 15, 92 to 10),
        Triple(597, 95 to 1, 94 to 3),
        Triple(598, 97 to 1, 96 to 13),
        Triple(599, 98 to 8, 98 to 6),
        Triple(600, 100 to 10, 100 to 6)
    )

    /** Every row of the print, in print order, with the page it sits on. */
    private val rows: List<Pair<MushafLayout.Page, MushafLayout.Line>> by lazy {
        val flat = ArrayList<Pair<MushafLayout.Page, MushafLayout.Line>>(9046)
        for (page in MushafLayout.pages) {
            for (line in page.lines) flat.add(page to line)
        }
        flat
    }

    /** The text rows, in print order. */
    private val textRows: List<Pair<MushafLayout.Page, MushafLayout.Line>> =
        rows.filter { it.second.kind == MushafLayout.Line.Kind.TEXT }

    @Test
    fun `every verse's words sit on the print's rows in order, with nothing missing`() {
        val runs = HashMap<Int, MutableList<MushafLayout.Span>>(QuranCorpus.VERSE_COUNT)
        var total = 0
        for (page in MushafLayout.pages) {
            for (line in page.lines) {
                for (span in line.spans) {
                    total += span.wordCount
                    runs.getOrPut(QuranCorpus.reference(span.surah, span.ayah)) {
                        ArrayList()
                    }.add(span)
                }
            }
        }
        assertEquals("the print's words", wordCount, total)

        for (surah in 1..QuranCorpus.SURA_COUNT) {
            for (ayah in 1..QuranCorpus.surahVerseCount[surah]) {
                val placed = checkNotNull(runs[QuranCorpus.reference(surah, ayah)]) {
                    "the print places no word of $surah:$ayah"
                }
                var expected = 0
                for (run in placed) {
                    assertEquals(
                        "$surah:$ayah's words are not where the print kept them",
                        expected, run.firstWord
                    )
                    expected = run.lastWord + 1
                }
                assertEquals(
                    "$surah:$ayah's words are not all placed",
                    MushafLayout.wordCountOf(surah, ayah), expected
                )
            }
        }
    }

    @Test
    fun `the print's pages carry the print's line counts`() {
        assertEquals(604, MushafLayout.pages.size)
        for (page in 1..MushafLayout.PAGE_COUNT) {
            assertEquals(
                "page $page carries",
                if (page <= 2) 8 else 15,
                MushafLayout.page(page).lines.size
            )
        }
    }

    @Test
    fun `the print opens 114 surahs by name and 112 openers with a basmalah row`() {
        val named = ArrayList<Int>(114)
        val opened = ArrayList<Int>(112)
        var textRowTotal = 0
        for ((page, line) in rows) {
            when (line.kind) {
                MushafLayout.Line.Kind.SURAH_NAME -> {
                    assertEquals(
                        "page ${page.number} slot ${line.slot}: a surah-name row carries no words",
                        0, line.spans.size
                    )
                    named.add(line.namedSurah)
                }

                MushafLayout.Line.Kind.BASMALAH -> {
                    val opener = line.spans.single()
                    assertEquals(
                        "page ${page.number} slot ${line.slot}: a basmalah row opens its own surah",
                        1, opener.ayah
                    )
                    assertEquals(
                        "page ${page.number} slot ${line.slot}: a basmalah row is its own opener's first four words",
                        0, opener.firstWord
                    )
                    assertEquals(3, opener.lastWord)
                    opened.add(opener.surah)
                }

                MushafLayout.Line.Kind.TEXT -> textRowTotal++
            }
        }
        assertEquals("the surah-name rows, in print order", (1..114).toList(), named)
        assertEquals(
            "the surahs that open with their own basmalah row",
            (2..114).filter { it != 9 }, opened
        )
        assertEquals("the print's text rows", 8820, textRowTotal)
        assertEquals(
            "114 name rows + 112 basmalah rows + 8820 text rows",
            9046, rows.size
        )
    }

    @Test
    fun `every verse-end marker renders exactly once`() {
        val wrappedRows = textRows.filter { it.second.wrappedMarkerAyah != 0 }
        assertEquals("rows that print a marker at their own head", 573, wrappedRows.size)

        val wrappedVerses = HashSet<Int>(573)
        var renderings = 0
        var embedded = 0
        for (i in textRows.indices) {
            val line = textRows[i].second
            val next = textRows.getOrNull(i + 1)?.second
            if (line.wrappedMarkerAyah != 0) {
                val reference = QuranCorpus.reference(line.wrappedMarkerSurah, line.wrappedMarkerAyah)
                assertTrue(
                    "two rows wrap the marker of " +
                        "${line.wrappedMarkerSurah}:${line.wrappedMarkerAyah}",
                    wrappedVerses.add(reference)
                )
            }
            for (span in line.spans) {
                if (!span.endsAyah) continue
                renderings++
                val wrappedAfter = next != null &&
                    next.wrappedMarkerSurah == span.surah &&
                    next.wrappedMarkerAyah == span.ayah
                if (!wrappedAfter) embedded++
            }
        }
        assertEquals("markers over the corpus, one per verse", QuranCorpus.VERSE_COUNT, renderings)
        assertEquals(
            "markers the print attaches to the word that closes its verse",
            QuranCorpus.VERSE_COUNT - 573, embedded
        )

        // The one anomaly the digitization carries: page 589's line 13 sets
        // ayah 84:21's marker as a stray mid-line entry while the verse itself
        // continues onto line 14. The layout registers the marker where the
        // verse ends, on the row the marker's digits belong to, and no row
        // wraps it away from there.
        val (page, line) = textRows.first { row ->
            row.second.spans.any { it.surah == 84 && it.ayah == 21 && it.endsAyah }
        }
        assertEquals("where 84:21's marker renders", 589, page.number)
        assertEquals("where 84:21's marker renders", 14, line.slot)
        assertTrue(
            "no row wraps 84:21's marker",
            textRows.none {
                it.second.wrappedMarkerSurah == 84 && it.second.wrappedMarkerAyah == 21
            }
        )
    }

    @Test
    fun `the print centres exactly the rows the print centres`() {
        assertEquals(30, centred.size)
        for ((page, line) in rows) {
            when (line.kind) {
                MushafLayout.Line.Kind.TEXT -> {
                    val key = page.number to line.slot
                    if (line.centered) {
                        assertTrue(
                            "page $page slot ${line.slot} centred is not in the ledger",
                            key in centred
                        )
                    } else {
                        assertTrue(
                            "the ledger's centred row on page ${page.number} slot ${line.slot} loaded un-centred",
                            key !in centred
                        )
                    }
                }

                else -> assertTrue(
                    "page ${page.number} slot ${line.slot}: a ${line.kind} row is always centred",
                    line.centered
                )
            }
        }
    }

    @Test
    fun `the print starts every page where the print starts it, except 25 documented boundaries`() {
        var agreed = 0
        val differing = ArrayList<Triple<Int, Pair<Int, Int>, Pair<Int, Int>>>(25)
        for (page in 1..MushafLayout.PAGE_COUNT) {
            val printStart = MushafLayout.page(page).lines.asSequence()
                .flatMap { it.spans.asSequence() }
                .first { it.firstWord == 0 }
            val metadata = QuranCorpus.ayahsOnPage(page).first()
            val printRef = printStart.surah to printStart.ayah
            val metadataRef = metadata.surahNumber to metadata.ayahNumber
            if (printRef == metadataRef) {
                agreed++
            } else {
                differing.add(Triple(page, metadataRef, printRef))
            }
        }
        assertEquals("pages both tables start alike", 579, agreed)
        assertEquals("the boundary attributions", boundaryAttributions, differing)
    }

    @Test
    fun `a surah's ink starts on the page both tables agree it starts on`() {
        for (surah in 1..QuranCorpus.SURA_COUNT) {
            assertEquals(
                "surah $surah's start page, print vs the corpus's page table",
                QuranCorpus.surahStartPage(surah),
                MushafLayout.pageOfSurah(surah)
            )
        }
    }

    @Test
    fun `every verse supplies its print words, none of them blank`() {
        var total = 0
        for (index in 0 until QuranCorpus.VERSE_COUNT) {
            val ayah = QuranCorpus.ayahs[index]
            val words = MushafLayout.wordsOf(ayah.surahNumber, ayah.ayahNumber)
            assertEquals(
                "the print words of ${ayah.surahNumber}:${ayah.ayahNumber}",
                MushafLayout.wordCountOf(ayah.surahNumber, ayah.ayahNumber), words.size
            )
            assertTrue(
                "a blank print word in ${ayah.surahNumber}:${ayah.ayahNumber}",
                words.all { it.isNotBlank() }
            )
            total += words.size
        }
        assertEquals(wordCount, total)
    }

    @Test
    fun `Al-Fatihah's opener is the whole first page's own second row`() {
        val nameRow = MushafLayout.page(1).lines[0]
        assertEquals(MushafLayout.Line.Kind.SURAH_NAME, nameRow.kind)
        assertEquals(1, nameRow.namedSurah)
        assertTrue(nameRow.centered)

        val opener = MushafLayout.page(1).lines[1]
        assertEquals(MushafLayout.Line.Kind.TEXT, opener.kind)
        assertTrue(opener.centered)
        assertEquals(1, opener.spans.size)
        val span = opener.spans.single()
        assertEquals(1, span.surah)
        assertEquals(1, span.ayah)
        assertEquals(0, span.firstWord)
        // Al-Fatihah's 1:1 *is* the basmalah, so its row alone is four words.
        assertEquals(3, span.lastWord)
        assertTrue(span.endsAyah)
        assertEquals(4, MushafLayout.wordCountOf(1, 1))
        assertEquals(4, MushafLayout.wordsOf(1, 1).size)
    }
}
