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
 *
 * [text] and [full] are both needed and they are not the same range. Highlighting
 * a selected verse should cover the words, not the ornament that follows them -
 * a reader who selects 2:255 wants 2:255 to be visibly the selection, and a
 * frame around the ayah marker reads as a frame around a *number*. So the words
 * are [text], the words plus their marker are [full], and the two are used for
 * different jobs rather than one standing in for the other.
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
     * Resolved through [Ayah.ref] rather than assembling the three numbers here, so
     * that a span and the verse it was built from cannot name different pages - the
     * fallback of `?: 1` below did exactly that whenever a reference was asked for
     * a verse the corpus did not hold, quietly pointing the reader at page 1.
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
 * Not an omission, and it is the single most consequential thing found in the
 * corpus during the rebuild. The bundled Tanzil text carries the basmalah
 * **inside verse 1** of every surah that has one - **113 of the 114** - rather than as
 * a separate record. So a head that printed a basmalah of its own prints it
 * **twice**: once as furniture and once at the start of the first verse, immediately
 * below. The previous reader did exactly that on all 113 of them, and avoided it on
 * Al-Fatihah and At-Tawbah only because those two were hard-coded as exceptions - a
 * fact about two surahs remembered by someone, standing in for a fact about the data
 * that nobody had read.
 *
 * **113, not 114 and not 111.** Both other numbers were in this file's own comments,
 * written from memory. At-Tawbah has no basmalah, which is the only reason it is not
 * 114; and 111 was never right either. `QuranStructure` counts it now, from the data,
 * and `QuranStructureTest` checks the count against every surah individually - so the
 * number in this comment is kept honest by a test rather than by whoever edits next.
 *
 * ### Why the head is inside the string
 *
 * It is part of the page, so it has to be inside the thing that is measured; and
 * the page's height accounts for it, so a fit calculation can never leave a head
 * hanging off the bottom of a "fitted" page. A composable above the text would be
 * measured as nothing at all.
 */
class MushafPageText private constructor(
    val text: AnnotatedString,
    val spans: List<VerseSpan>
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
     *
     * The page's surah head and basmalah come before the first verse, and the
     * trailing space comes after the last, so an offset can legitimately be inside
     * the page and inside no verse at all. Both ends resolve to the nearest verse -
     * the head to the *first*, because a tap on "Al-Fatihah" is a tap at the top of
     * Al-Fatihah, and the tail to the last.
     *
     * The previous implementation returned the *currently selected* verse in these
     * positions. That made a tap during the first frame, before the text had been
     * laid out, silently re-select what was already selected, so the first tap on
     * a newly-turned page did nothing at all and a reader could not tell a tap that
     * missed from a tap that hit.
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
         *
         * Read from the corpus rather than from a list of surah numbers, and the
         * reason is the discovery that made this file's head logic wrong: the
         * Tanzil text carries the basmalah *inside* verse 1, not as a separate
         * record. 113 of the 114 surahs open with it; At-Tawbah has none, and
         * Al-Fatihah's is the whole of its verse 1. See [QuranStructure.BASMALAH_COUNT],
         * which counts it rather than asserting it from memory.
         *
         * So a page head must not print a basmalah of its own: the text below it
         * already has one. This function exists so the *absence* is stated in one
         * place and checked against the data, rather than being a special case for
         * two surahs someone remembered and the other 111 nobody did - a number that
         * was itself wrong, and is now counted.
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
         * Compared **folded**, and that is not a shortcut. Al-Tin and Al-Qadr open
         * with a shadda on the ba - ٱلسْمِ, shadda first - where every other surah
         * opens with a bare kasra - بِسْمِ, kasra first. Both are the same word in the
         * same script, and a byte comparison against one spelling reports those
         * two surahs as having no basmalah at all.
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
         * Not used to print anything on a page - see [appendSurahHead] - but the
         * reader's continuous surface needs it. A surah heading there separates
         * the basmalah from the first verse, because in a scrolling list the
         * reader needs to see where the surah begins before the words do, and a
         * basmalah welded to the front of 2:1 does not show that.
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
         *
         * [selected] is inked rather than boxed, because in a justified page a
         * reader needs to see *which words* they selected and a frame around the
         * ayah marker draws attention to the number instead.
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
             *
             * False for a flowing block in the continuous layout, and that is not
             * a detail. A page is a fixed object and needs its running head; a
             * twelve-verse block inside a scrolling list already sits under a
             * surah heading, so naming the surah again prints it twice - and in the
             * rendered result the second copy lands *inline*, at the start of the
             * first line, where it reads as part of the text rather than as a
             * heading.
             */
            showSurahHead: Boolean = true
        ): MushafPageText {
            val spans = ArrayList<VerseSpan>(ayahs.size)
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
                    builder.appendSurahHead(
                        surah = ayah.surahNumber,
                        scale = scale,
                        accent = accent,
                        lineHeight = bodyLineHeight
                    )
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
            return MushafPageText(builder.toAnnotatedString(), spans)
        }

        /**
         * The surah's Arabic name, immediately before its own first verse.
         *
         * Called from inside the verse loop rather than once before it, and that is
         * the whole point. A page that *continues* Al-Baqarah must not print
         * Al-Baqarah's name at the top - that claims the surah starts here - but a
         * page that Al-Baqarah *starts* part-way down must print it there, or the
         * surah begins on that page with nothing to say so. Both rules are the same
         * rule: the name goes immediately before ayah 1, wherever ayah 1 is.
         *
         * The *running* head that names every page is chrome and belongs to the
         * reader; this is the text of the page. See the note on [build] for why the
         * basmalah is deliberately not here.
         *
         * [hasBasmalah] states the basmalah rule and is checked against the corpus,
         * so the answer comes from the text rather than from a list.
         */
        /**
         * The surah's Arabic name, on a line of its own, before its first verse.
         *
         * Centred, as a printed mushaf centres it, and on its own line. Both of
         * those are corrections: the name used to be appended inline with a trailing
         * space, which under RTL puts it at the *right* of the first line with the
         * text running left from it - so "البقرة" sat immediately before the basmalah
         * on the same baseline, in Arabic, in the accent colour, and read as the
         * first word of the page rather than as a heading. It looked deliberate and
         * was the only thing on the page that was not where a mushaf puts it.
         *
         * Centred as its own *paragraph*, which is where
         * [ParagraphStyle.shouldNotOverlap] earns its keep: an `AnnotatedString`
         * allows one paragraph style at a time, so the body's own style has to be
         * popped around the head and pushed again after it. Trying to nest the two -
         * `pushStyle(ParagraphStyle(Center))` inside the body's paragraph style -
         * throws `IllegalArgumentException: ParagraphStyle should not overlap` from
         * the string builder, at every page that opens a surah.
         *
         * The line height is restated on the head's paragraph rather than inherited,
         * so the head's line is as tall as a line of text and [PageFit]'s
         * measurement is of what is drawn. It was measured correctly either way -
         * a short line in a tall-line-height paragraph is still a tall line - but
         * saying so is better than relying on it.
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
     *
     * One glyph for both kinds. A reader who knows whether a prostration is
     * obligatory or recommended is being told something, and the metadata
     * distinguishes fifteen of them - but U+06E9 is the only character Unicode has
     * for the place of sajdah, and inventing a second one would put a shape in the
     * text that no other Quran text uses. The distinction is kept in the data and
     * is available to a reader who asks for it; it is not smuggled into a glyph.
     *
     * It is worth naming because U+06E9 is small and reads as a "ص" with marks on
     * it, so a stray one is invisible in a screenshot of a page of Arabic - which
     * is exactly how one came to be printed in the middle of Al-Ikhlas.
     */
    object SajdaMarker {
        /** Obligatory, as the Tanzil metadata types it. */
        const val OBLIGATORY = "۩"

        /** Recommended. */
        const val RECOMMENDED = "۩"
    }
}
