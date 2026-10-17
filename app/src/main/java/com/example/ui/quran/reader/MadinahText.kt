package com.example.ui.quran.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.quran.ArabicDigits
import com.example.data.quran.MushafLayout
import com.example.data.quran.QuranBrowse

/**
 * One printed row of the Madinah mushaf, built as text.
 *
 * [text] is the row whole, ornaments included; [spans] are the verse extents
 * inside it, in the same offsets. Every word comes from
 * [MushafLayout.wordsOf] - the print's own fused word stream - so the row is
 * exactly the ordinals the layout record names on it. Nothing measures here:
 * the drawing side measures, scales and justifies, which it can only do in
 * pixels it owns. That split is what keeps this question - what does a row
 * say, and which verse is each part of it - testable without a Compose
 * runtime at all.
 */
internal class MadinahRowText(
    val line: MushafLayout.Line,
    val text: AnnotatedString,
    val spans: List<VerseSpan>,
) {
    /**
     * The verse whose extent covers [offset], answered by the one rule the
     * flowing page resolves taps with: a tap between verses means the verse
     * the reader was reading, and a tap on an ornament means the verse it
     * closes.
     */
    fun verseAt(offset: Int): VerseSpan? = verseAtIn(offset, spans)
}

/**
 * The rows of the Madinah mushaf's page, built for drawing.
 *
 * ### What a row is, and where each of its parts comes from
 *
 * - **Words** are [MushafLayout.wordsOf]'s, sliced by the record's ordinals.
 *   The tokenizer is the loader's decision; this file would only be able to
 *   disagree with it, so it does not tokenize at all.
 * - **A verse-end marker** stands after the span that closes its verse -
 *   except where the span's [MushafLayout.Span.markerWrapped] says the print
 *   left its marker to the next row, which the loader resolved in the same
 *   walk that established the wrap. The marker is inside the verse's extent,
 *   so a tap on the ornament selects the verse it closes, as on the flowing
 *   page. It renders a size below the body, separated by one space, because
 *   an ornament welded to the last letter reads as a letter.
 * - **A wrapped marker** leads its row alone and is an extent of the verse
 *   whose words ended on the row above: its text range is empty, its full
 *   range is the ornament's. A tap on it means that verse, exactly as a tap
 *   on an embedded ornament does.
 * - **A surah name** is the metadata's Arabic name at the print's share of
 *   the row's type size, and an extent of the opener verse - a tap on the
 *   name reads as the verse it introduces. On the five pages whose head row
 *   prints alone (the words below it on the page that follows), it still
 *   answers that verse; the position keeps it and the page turn finds the
 *   ink.
 * - **The prostration sign** is not appended by anyone. The corpus's fused
 *   words carry ۩ where the print signs one - it is part of a word the
 *   layout already names.
 * - **A selection** is a background over the verse's own words - the exact
 *   terms the flowing page highlights on, so a selection looks like itself
 *   on both surfaces.
 *
 * The body's own style (family, size, colour, direction) is the drawing
 * side's to apply; a builder that also carried it would be a second copy of
 * the [QuranFonts.LocalQuranTypeface.current] question, and only the parts
 * that differ from the body - ornament, name, selection - are styled here.
 */
internal object MadinahText {

    /** The share of the row's size the print sets an ayah circle at. */
    private const val MARKER_FRACTION = 0.82f

    /** The share of the row's size the print sets a surah name at. */
    private const val HEAD_FRACTION = 0.72f

    /**
     * A whole page's rows, in print order, at [scale].
     *
     * [scale] is the flowing page's own unit - the reader's base Arabic size
     * times it - because the Madinah page's fit arithmetic lands on a size,
     * not a preference, and sharing the unit is what lets both surfaces use
     * the same [QuranTypeface] styling unchanged.
     */
    fun page(
        pageNumber: Int,
        scale: Float,
        accent: Color,
        selected: QuranRef?,
        highlight: Color,
    ): List<MadinahRowText> =
        MushafLayout.page(pageNumber).lines.map {
            row(
                line = it,
                scale = scale,
                accent = accent,
                selected = selected,
                highlight = highlight
            )
        }

    /**
     * One printed row at [scale].
     */
    fun row(
        line: MushafLayout.Line,
        scale: Float,
        accent: Color,
        selected: QuranRef?,
        highlight: Color,
    ): MadinahRowText {
        val bodySize = QuranReadingOptions.ARABIC_BASE_SP * scale
        val markerStyle = SpanStyle(
            color = accent,
            fontSize = (bodySize * MARKER_FRACTION).sp,
            fontWeight = FontWeight.Normal
        )
        val headStyle = SpanStyle(color = accent, fontSize = (bodySize * HEAD_FRACTION).sp)
        val spans = ArrayList<VerseSpan>(line.spans.size + 1)

        val text = buildAnnotatedString {
            when (line.kind) {
                MushafLayout.Line.Kind.SURAH_NAME -> {
                    val name = QuranBrowse.surah(line.namedSurah)?.arabicName.orEmpty()
                    if (name.isNotEmpty()) {
                        val start = length
                        withStyle(headStyle) { append(name) }
                        spans += VerseSpan(
                            ayahNumber = 1,
                            surahNumber = line.namedSurah,
                            text = IntRange.EMPTY,
                            full = start until length
                        )
                    }
                }

                else -> {
                    if (line.wrappedMarkerAyah != 0) {
                        // The ornament closes a verse whose words ended on the
                        // row above. It is its own extent - the words proper
                        // are not on this row - and the row's words continue
                        // the verse after it.
                        val start = length
                        withStyle(markerStyle) {
                            append(ArabicDigits.ayahMarker(line.wrappedMarkerAyah))
                        }
                        spans += VerseSpan(
                            ayahNumber = line.wrappedMarkerAyah,
                            surahNumber = line.wrappedMarkerSurah,
                            text = IntRange.EMPTY,
                            full = start until length
                        )
                        append(' ')
                    }

                    var first = true
                    for (span in line.spans) {
                        if (!first) append(' ')
                        first = false

                        val isSelected = selected != null &&
                            selected.surah == span.surah &&
                            selected.ayah == span.ayah
                        val textStart = length
                        if (isSelected) {
                            pushStyle(SpanStyle(background = highlight))
                        }
                        val words = MushafLayout.wordsOf(span.surah, span.ayah)
                        for (word in span.firstWord..span.lastWord) {
                            append(words[word])
                            if (word < span.lastWord) append(' ')
                        }
                        if (isSelected) pop()
                        val textEnd = length
                        var fullEnd = length

                        if (span.endsAyah && !span.markerWrapped) {
                            append(' ')
                            withStyle(markerStyle) {
                                append(ArabicDigits.ayahMarker(span.ayah))
                            }
                            fullEnd = length
                        }
                        spans += VerseSpan(
                            ayahNumber = span.ayah,
                            surahNumber = span.surah,
                            text = textStart until textEnd,
                            full = textStart until fullEnd
                        )
                    }
                }
            }
        }
        return MadinahRowText(line, text, spans)
    }
}
