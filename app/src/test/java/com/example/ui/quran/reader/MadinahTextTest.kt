package com.example.ui.quran.reader

import androidx.compose.ui.graphics.Color
import com.example.data.model.QuranRef
import com.example.data.quran.ArabicDigits
import com.example.data.quran.MushafLayout
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Madinah mushaf's rows, as text.
 *
 * The builder is a pure function of [MushafLayout]'s own records and the
 * corpus's word stream, so every question here is asked of the whole print:
 * all 604 pages, no samples. Its terms are the loader's: a span's ordinals
 * name the verse words, a row's kind names what it holds, and the
 * wrapped-marker decision was made once in the loader's walk. What the tests
 * pin is that this file neither drops, repeats, reorders nor decorates
 * wrongly anything that passed through it - and that the tap-resolving
 * extents agree with the ink.
 */
class MadinahTextTest {

    private val accent = Color(0xFF8B6F47)
    private val highlight = Color(0xFFFFFF00)

    private fun page(page: Int, selected: QuranRef? = null): List<MadinahRowText> =
        MushafLayout.page(page).lines.map {
            MadinahText.row(
                line = it,
                scale = 1f,
                accent = accent,
                selected = selected,
                highlight = highlight
            )
        }

    /**
     * The row's ink reconstructs from its own records: each span's words in
     * order, one gap between spans, the ornaments where and only where the
     * loader put them - a leading marker for a wrapped verse's end, a
     * trailing marker for a verse that ends on the row.
     */
    @Test
    fun `a row's ink is exactly its spans' words with the loader's ornaments`() {
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            page(pageNumber).forEach { row ->
                if (row.line.kind == MushafLayout.Line.Kind.SURAH_NAME) {
                    assertEquals(
                        "page $pageNumber name row",
                        QuranBrowse.surah(row.line.namedSurah)?.arabicName,
                        row.text.text
                    )
                    return@forEach
                }
                val rebuilt = StringBuilder()
                if (row.line.wrappedMarkerAyah != 0) {
                    rebuilt.append(ArabicDigits.ayahMarker(row.line.wrappedMarkerAyah))
                    rebuilt.append(' ')
                }
                for (span in row.line.spans) {
                    if (rebuilt.isNotEmpty() && !rebuilt.endsWith(' ')) rebuilt.append(' ')
                    val words = MushafLayout.wordsOf(span.surah, span.ayah)
                    rebuilt.append((span.firstWord..span.lastWord).joinToString(" ") { words[it] })
                    if (span.endsAyah && !span.markerWrapped) {
                        rebuilt.append(' ').append(ArabicDigits.ayahMarker(span.ayah))
                    }
                }
                assertEquals("page $pageNumber row ${row.line.slot}", rebuilt.toString(), row.text.text)
            }
        }
    }

    /**
     * Building all 604 pages succeeds, every verse contributes its words
     * exactly once across the rows of the print, and each verse's words are
     * covered contiguously - the reading surface can only show what this
     * walk leaves it.
     */
    @Test
    fun `the print holds every verse's every word once`() {
        val covered = HashMap<Pair<Int, Int>, Int>()
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            page(pageNumber).forEach { row ->
                if (row.line.kind == MushafLayout.Line.Kind.SURAH_NAME) return@forEach
                for (span in row.line.spans) {
                    val key = span.surah to span.ayah
                    val expected = covered[key] ?: 0
                    assertEquals(
                        "verse $key resumes mid-word on page $pageNumber",
                        expected, span.firstWord
                    )
                    covered[key] = span.lastWord + 1
                }
            }
        }
        assertEquals(QuranCorpus.VERSE_COUNT.toLong(), covered.size.toLong())
        for ((key, next) in covered) {
            assertEquals("verse $key ends short of its words", MushafLayout.wordCountOf(key.first, key.second), next)
        }
    }

    /**
     * A tap on any character of a verse's words, or on that verse's own
     * ornament, resolves to exactly that verse's extent - one tap rule over
     * two surfaces.
     */
    @Test
    fun `every inked offset of a verse's words and marker taps that verse`() {
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            page(pageNumber).forEach { row ->
                // The built extents line up with the records one row after
                // the next, after a wrapped ornament, if any, leads them.
                val content = if (row.line.wrappedMarkerAyah == 0) {
                    row.spans
                } else {
                    row.spans.drop(1)
                }
                row.line.spans.forEachIndexed { index, record ->
                    if (record.firstWord > record.lastWord) return@forEachIndexed
                    val built = content[index]
                    for (offset in built.full) {
                        assertSame(
                            "page $pageNumber row ${row.line.slot} offset $offset",
                            built, row.verseAt(offset)
                        )
                    }
                }
            }
        }
    }

    /**
     * A wrapped ornament leads a row as an extent of the verse it closes:
     * empty of words, headed by the marker, and the verse named is the one
     * the loader said wrapped.
     */
    @Test
    fun `a wrapped ornament leads its row and taps as the verse it closes`() {
        var seen = 0
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            page(pageNumber).forEach { row ->
                if (row.line.wrappedMarkerAyah == 0) return@forEach
                seen++
                val head = row.spans.first()
                assertEquals(row.line.wrappedMarkerAyah, head.ayahNumber)
                assertEquals(row.line.wrappedMarkerSurah, head.surahNumber)
                assertTrue(head.text.isEmpty())
                val headText = row.text.text.substring(head.full.first, head.full.last + 1)
                assertEquals(ArabicDigits.ayahMarker(row.line.wrappedMarkerAyah), headText)
                assertSame(head, row.verseAt(head.full.first))
            }
        }
        assertEquals("the loader's wrapped rows number 573", 573, seen)
    }

    /**
     * A name row is the metadata's Arabic name for the surah the record
     * names, and taps as that surah's opener.
     */
    @Test
    fun `a surah's name row says the name and taps as its opener`() {
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            page(pageNumber).forEach { row ->
                if (row.line.kind != MushafLayout.Line.Kind.SURAH_NAME) return@forEach
                assertEquals(1, row.spans.size)
                val span = row.spans.single()
                assertEquals(0, row.line.wrappedMarkerAyah)
                assertEquals(row.line.namedSurah, span.surahNumber)
                assertEquals(1, span.ayahNumber)
                assertEquals(
                    QuranBrowse.surah(row.line.namedSurah)?.arabicName,
                    row.text.text.substring(span.full.first, span.full.last + 1)
                )
                assertSame(span, row.verseAt(span.full.first))
            }
        }
    }

    /**
     * A selection is a background over the verse's own words and over
     * nothing else - the ornament beside the words stays unhighlighted, so
     * the look matches the flowing page's.
     */
    @Test
    fun `a selection highlights its verse's words and nothing beyond them`() {
        var pinned = false
        for (pageNumber in 1..MushafLayout.PAGE_COUNT) {
            val record = MushafLayout.page(pageNumber).lines
                .asSequence()
                .flatMap { it.spans.asSequence() }
                .firstOrNull { it.firstWord <= it.lastWord } ?: continue
            val selected = QuranRef(surah = record.surah, ayah = record.ayah, page = pageNumber)
            pinned = true
            page(pageNumber, selected).forEach { row ->
                row.spans.forEach { span ->
                    if (span.text.isEmpty()) return@forEach
                    val covering = row.text.spanStyles.filter {
                        it.item.background == highlight &&
                            it.start <= span.text.first && span.text.last in it.start until it.end
                    }
                    if (span.surahNumber == selected.surah && span.ayahNumber == selected.ayah) {
                        assertTrue("page $pageNumber selected verse has no background", covering.isNotEmpty())
                        assertTrue(
                            "page $pageNumber background spills past the verse's words",
                            covering.all { it.end == span.text.last + 1 }
                        )
                    } else {
                        assertTrue(
                            "page $pageNumber uninvolved verse " +
                                "${span.surahNumber}:${span.ayahNumber} highlighted",
                            covering.isEmpty()
                        )
                    }
                }
            }
        }
        assertTrue("no page offered a verse to select", pinned)
    }

    /**
     * The fixed facts of this print, pinned where they shape the renderer.
     */
    @Test
    fun `the fixed pages this renderer leans on`() {
        // Page 1 opens on Fatiha's own first verse: there is no basmalah row,
        // its centred text *is* the basmalah, and the ornament is on it.
        val pageOne = page(1)
        assertEquals(MushafLayout.Line.Kind.SURAH_NAME, pageOne[0].line.kind)
        assertTrue("page 1 has no basmalah row", pageOne.none { it.line.kind == MushafLayout.Line.Kind.BASMALAH })
        val fatihah = pageOne[1].line.spans.first()
        assertEquals(1, fatihah.surah)
        assertEquals(1, fatihah.ayah)
        assertEquals(3, fatihah.lastWord)

        // Page 2 opens the surah's own header block: the name row, below it
        // the basmalah row - whose span is surah 2's opener's first four
        // words - and the text row under that continues the verse from word
        // four. The basmalah is part of the verse in the corpus stream, which
        // is why the print's rows and the corpus's tokens agree here.
        val pageTwo = page(2)
        assertEquals(MushafLayout.Line.Kind.SURAH_NAME, pageTwo[0].line.kind)
        assertEquals(2, pageTwo[0].line.namedSurah)
        assertEquals(MushafLayout.Line.Kind.BASMALAH, pageTwo[1].line.kind)
        val basmalah = pageTwo[1].line.spans.single()
        assertEquals(2, basmalah.surah)
        assertEquals(1, basmalah.ayah)
        assertEquals(3, basmalah.lastWord)
        assertEquals(4, pageTwo[2].line.spans.first().firstWord)

        // Page 586 opens on Ar-Rum's own last verse, fully - the print's own
        // attribution, which the corpus page table would have answered 80:40
        // with.
        assertEquals(80, page(586)[0].line.spans.first().surah)
        assertEquals(41, page(586)[0].line.spans.first().ayah)

        // Page 56's rows four and five are consecutive wraps: row four leads
        // with row three's ornament and still leaves its own to row five - a
        // row can wrap a marker and leave one in the same breath.
        val fiftySix = page(56)
        assertEquals(fiftySix[2].line.spans.last().ayah, fiftySix[3].line.wrappedMarkerAyah)
        assertTrue(fiftySix[3].line.spans.last().markerWrapped)
        assertTrue(fiftySix[4].line.wrappedMarkerAyah != 0)
    }
}
