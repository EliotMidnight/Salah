package com.example.ui.quran.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.quran.ArabicDigits
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranStructure
import com.example.data.quran.QuranText

/**
 * One verse's extent inside a page's text.
 */
class VerseSpan internal constructor(
    /** 1-based ayah number. */
    val ayahNumber: Int,
    val surahNumber: Int,
    /** The verse's own characters, exclusive at the end. */
    val text: IntRange,
    /** The verse and its ayah marker. */
    val full: IntRange
) {
    /**
     * The verse as a reference, for selection and copy.
     *
     * A `MushafPageText` is always built from real verses, so this is a lookup and
     * not a question.
     */
    val ref: QuranRef
        get() = QuranBrowse.ayah(surahNumber, ayahNumber)?.ref
            ?: QuranRef(surah = surahNumber, ayah = ayahNumber, page = 0)
}

/**
 * A page's text: one [AnnotatedString], plus the spans that make it navigable.
 *
 * ### Why the page is one string and not a column of `Text`s
 *
 * A mushaf page is a page. It has to wrap as one block of Arabic, be selected as
 * one unit, and be *measured* as one unit to know whether it fits - and the fit
 * is the whole question the reader is asking of a page. A column of per-verse
 * composables cannot be measured as a block, so the reader falls back to
 * estimating, and estimating is how a page ends up silently shrunk.
 *
 * ### What is in the string, in order
 *
 * 1. Each verse, then its ayah marker, with the surah's name immediately before
 *    its own first verse - not only when the page happens to open a surah. A surah
 *    begins where its ayah 1 falls, which for 58 of the book's 114 surahs is
 *    part-way down a page - 51 pages carry such a start - and a head printed only
 *    at a page's top left those 58 surahs unnamed on the page where they start.
 *    Only 56 surahs begin at a page boundary, so the old rule named 56 of 114.
 * 2. The prostration marker, where the metadata says a prostration follows.
 *
 * ### No basmalah in the head
 *
 * **113, not 114 and not 111.** Both other numbers were in this file's own comments,
 * written from memory. At-Tawbah has no basmalah, which is the only reason it is not
 * 114; and 111 was never right either. `QuranStructure` counts it now, from the data,
 * and `QuranStructureTest` checks the count against every surah individually - so the
 * number in this comment is kept honest by a test rather than by whoever edits next.
 */
class MushafPageText private constructor(
    val text: AnnotatedString,
    val spans: List<VerseSpan>,
    /**
     * Character ranges that must not be justified - the surah heads.
     *
     * A head is a centred heading, and elongating a centred title produces a centred title
     * with a stretched letter in it, which looks like a mistake rather than a decision.
     * [Kashida] is told about these so it leaves them alone; it cannot work them out for
     * itself, because "is this line centred" and "is this a heading" are different
     * questions and only this one knows the answer.
     */
    val centredRanges: List<IntRange> = emptyList()
) {
    /**
     * The verse whose unit covers [offset].
     *
     * A tap arrives in pixels and the spans are in characters, so the caller
     * resolves the pixel to a character through the text layout first. Matching on
     * [VerseSpan.full] rather than [VerseSpan.text] is what makes a tap on the
     * ayah *ornament* select that verse - the ornament is part of the verse, and a
     * reader who taps the circle means the verse beside it.
     *
     * ### Offsets that fall between or outside the verses
     */
    fun verseAt(offset: Int): VerseSpan? {
        if (spans.isEmpty()) return null
        spans.firstOrNull { offset in it.full }?.let { return it }
        if (offset < spans.first().full.first) return spans.first()
        // Past the end of the last verse: the nearest span *before* the offset, not
        // unconditionally the last one. Those differ on a page whose text continues
        // after the last recorded span, and "the last verse" is then wrong.
        return spans.lastOrNull { it.full.first <= offset } ?: spans.last()
    }

    companion object {

        /**
         * Whether a surah's own first verse opens with the basmalah.
         */
        internal fun hasBasmalah(surah: Int): Boolean {
            val first = QuranBrowse.ayah(surah, 1)?.textArabic ?: return false
            return startsWithBasmalah(first)
        }

        /** The basmalah as the corpus spells it, folded. */
        private val basmalahFolded: String by lazy {
            QuranText.normalise(QuranBrowse.ayah(1, 1)?.textArabic.orEmpty())
        }

        /**
         * Whether [text] opens with the basmalah.
         *
         * A byte comparison also gets the *order* wrong in a way that is easy to
         * miss: inserting the shadda after the kasra produces U+0650 U+0651, while
         * the corpus has U+0651 U+0650. Visually identical, and not the same
         * string. Folding removes the question.
         */
        internal fun startsWithBasmalah(text: String): Boolean =
            basmalahFolded.isNotEmpty() && QuranText.normalise(text).startsWith(basmalahFolded)

        /**
         * The basmalah as this surah writes it, or empty when it has none.
         *
         */
        internal fun leadingBasmalahOf(surah: Int): String {
            val first = QuranBrowse.ayah(surah, 1)?.textArabic ?: return ""
            if (!startsWithBasmalah(first)) return ""

            // Returned *unfolded*, because this is text to print and folding
            // strips the tashkeel the basmalah is written with. The folded form is
            // only ever a comparison key.
            //
            // The **longest** matching prefix, not the first. Folding makes several
            // lengths equivalent at once - dropping the final kasra of
            // ٱلرَّحِيمِ changes nothing once it is stripped - so stopping at the
            // first match returns ٱلرَّحِيم and silently drops a character of the
            // first line of the book. Scanning to a bound and keeping the last
            // match is the whole fix, and the bound only exists because this is
            // called during composition.
            val limit = minOf(first.length, 64)
            var best = ""
            for (length in 1..limit) {
                if (QuranText.normalise(first.take(length)) == basmalahFolded) {
                    best = first.take(length)
                }
            }
            return best
        }

        /**
         * Builds the page.
         */
        fun build(
            ayahs: List<Ayah>,
            scale: Float,
            selected: QuranRef?,
            ink: Color,
            accent: Color,
            highlight: Color,
            lineHeightFactor: Float,
            /**
             * Whether to name the surah this text opens, as a *mushaf page* does.
             */
            showSurahHead: Boolean = true
        ): MushafPageText {
            val spans = ArrayList<VerseSpan>(ayahs.size)
            val centred = ArrayList<IntRange>(2)
            val bodySize = QuranReadingOptions.ARABIC_BASE_SP * scale
            // The marker is set smaller than the body so it recedes. At full size
            // an ayah circle is as loud as a word, and a page of them reads as a
            // page of numbers.
            val markerSize = (bodySize * 0.82f).sp

            // Named, because [appendSurahHead] has to pop it and push the identical
            // style back afterwards - an `AnnotatedString` permits exactly one
            // paragraph style at a time, and the head needs its own (centred).
            val bodyLineHeight = (bodySize * lineHeightFactor).sp
            val builder = AnnotatedString.Builder()
            builder.pushStyle(
                ParagraphStyle(
                    lineHeight = bodyLineHeight,
                    textDirection = TextDirection.Rtl
                )
            )

            for (ayah in ayahs) {
                // The head goes immediately before a surah's *first verse*, wherever
                // that verse falls on the page.
                //
                // Testing the page's own first verse - "does this page open a surah?"
                // - was the rule until a sweep over all 604 pages found it wrong on
                // 58 of the 114 surahs. A surah does not begin at a page boundary: on
                // 51 pages a surah's ayah 1 lands part-way down, so its name was never
                // printed anywhere - and since ayah 1 is on exactly one page, that
                // name was on no page at all. A printed mushaf prints it where the
                // surah starts.
                //
                // The test is `ayahNumber == 1` rather than "the page opens a surah",
                // so it is read from the data and holds for every page rather than
                // for the three a chosen example would cover.
                if (showSurahHead && ayah.ayahNumber == 1) {
                    val headStart = builder.length
                    builder.appendSurahHead(
                        surah = ayah.surahNumber,
                        scale = scale,
                        accent = accent,
                        lineHeight = bodyLineHeight
                    )
                    centred += headStart until builder.length
                }

                // Taken *after* the head, so the head is inside this verse's span and
                // a tap on the surah's name selects the verse it introduces rather
                // than resolving to the nearest verse *before* it - which on a
                // mid-page surah would be the last verse of the previous surah, on a
                // different page of the reader's mind entirely.
                val start = builder.length
                val isSelected = selected != null &&
                    selected.surah == ayah.surahNumber &&
                    selected.ayah == ayah.ayahNumber

                if (isSelected) {
                    builder.pushStyle(SpanStyle(background = highlight))
                }
                builder.append(ayah.textArabic)
                val textEnd = builder.length
                if (isSelected) builder.pop()

                // A space before the ornament. The Uthmani text carries no
                // trailing space, and without this the marker is welded to the
                // last letter.
                builder.append(' ')
                builder.pushStyle(
                    SpanStyle(color = accent, fontSize = markerSize, fontWeight = FontWeight.Normal)
                )
                builder.append(ArabicDigits.ayahMarker(ayah.ayahNumber))
                builder.pop()
                val unitEnd = builder.length

                // The prostration marker, inside the string so it cannot drift away
                // from the verse it belongs to when the text reflows.
                //
                // Appended *before* `unitEnd` is taken, so the glyph is part of the
                // verse's span. Reading `unitEnd` first put the glyph outside `full`,
                // and [verseAt] then had no span containing it and fell through to
                // "past the end" - which returns the *last* verse on the page. So a
                // reader who tapped ۩ on 32:15 selected An-Nas.
                //
                // **Only when the verse does not already carry one.** Tanzil's text
                // has U+06E9 written into 7:206 - the corpus's own text, not
                // something this app added - and appending a second one put two
                // prostration marks in the middle of Al-A'raf, visible in the
                // screenshot. A marker that is already in the text is the text.
                val sajda = QuranBrowse.sajdaAfter(ayah.surahNumber, ayah.ayahNumber)
                if (sajda != null && !ayah.textArabic.contains(SajdaMarker.OBLIGATORY)) {
                    builder.append(' ')
                    builder.pushStyle(SpanStyle(color = accent, fontSize = markerSize))
                    builder.append(sajdaMarkerFor(sajda))
                    builder.pop()
                }
                val unitEndWithSajda = builder.length

                // And a space between verses, so the last word of one and the
                // first of the next cannot read as one phrase.
                builder.append(' ')

                spans += VerseSpan(
                    ayahNumber = ayah.ayahNumber,
                    surahNumber = ayah.surahNumber,
                    text = start until textEnd,
                    full = start until unitEndWithSajda
                )
            }

            builder.pop()
            return MushafPageText(builder.toAnnotatedString(), spans, centred)
        }

        /**
         * The surah's Arabic name, immediately before its own first verse.
         */
        /**
         * The surah's Arabic name, on a line of its own, before its first verse.
         */
        private fun AnnotatedString.Builder.appendSurahHead(
            surah: Int,
            scale: Float,
            accent: Color,
            lineHeight: TextUnit
        ) {
            val name = QuranBrowse.surah(surah)?.arabicName ?: return

            // The body paragraph, popped for the head and restored after it.
            pop()
            pushStyle(
                ParagraphStyle(
                    textAlign = TextAlign.Center,
                    lineHeight = lineHeight,
                    textDirection = TextDirection.Rtl
                )
            )
            pushStyle(
                SpanStyle(
                    color = accent,
                    fontSize = (QuranReadingOptions.ARABIC_BASE_SP * scale * 0.72f).sp
                )
            )
            append(name)
            append('\n')
            pop()
            pop()
            pushStyle(
                ParagraphStyle(
                    lineHeight = lineHeight,
                    textDirection = TextDirection.Rtl
                )
            )
        }

        private fun sajdaMarkerFor(kind: QuranBrowse.SajdaKind): String =
            if (kind == QuranBrowse.SajdaKind.OBLIGATORY) SajdaMarker.OBLIGATORY
            else SajdaMarker.RECOMMENDED
    }

    /**
     * The prostration markers, U+06E9.
     */
    object SajdaMarker {
        /** Obligatory, as the Tanzil metadata types it. */
        const val OBLIGATORY = "۩"

        /** Recommended. */
        const val RECOMMENDED = "۩"
    }
}
