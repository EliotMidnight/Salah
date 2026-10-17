package com.example.data.quran

import com.example.data.model.QuranRef

/**
 * The Madinah mushaf's printed lines: where every word of the verified corpus
 * sits on the 604 pages of the KFGQPC V2 (1421H) print.
 *
 * `mushaf_layout.txt` carries geometry and nothing else. One record per printed
 * line names the corpus word ordinals the print sets on that line; the words
 * themselves are read from the corpus at render time. So verse text has one
 * source, and an edited layout cannot quietly move a different verse onto a
 * line: the records must still tile the corpus exactly, and the asset's bytes
 * must still match [QuranCorpus.MUSHAF_LAYOUT_HASH].
 *
 * ### Where verse-end markers come from
 *
 * A verse-end marker is not one of the corpus's words, so the layout does not
 * place it; the reading derives it. The print closes a verse either by
 * attaching the marker to the entry that ends the verse, or - when the verse
 * ends exactly at a line break - by printing the marker alone at the start of
 * the next line. Both cases render from the geometry alone: a [Span] whose
 * last word is its verse's last word carries the marker after it, unless
 * [Span.markerWrapped] says the next text row prints it - and a TEXT
 * line with [Line.wrappedMarkerAyah] prints that marker before its first word.
 * Every marker in the corpus renders exactly once; the tests prove it over all
 * 6,236 verses, and the marking is made *here*, in the same walk that
 * establishes the wrap, so no renderer ever has to look ahead to row it does
 * not own and no second copy of the rule can drift.
 *
 * The one anomaly the digitization carries is documented in
 * `tools/madinah_layout.py`: page 589's line 13 holds ayah 84:21's marker as a
 * stray mid-line entry while the verse itself continues onto line 14. The asset
 * records that marker where the verse ends, which is both what the print's own
 * words say and where the Quranic Universal Library export of the same print
 * puts it; no edition sets a verse marker mid-verse.
 *
 * ### Why the basmalah rows name the opener's own words
 *
 * The corpus writes the basmalah as the first four words of every surah's first
 * ayah (all but At-Tawbah's; Al-Fatihah's own 1:1 *is* the basmalah). The print
 * renders those four words as their own large row between the surah-name row
 * and the verse text, so a BASMALAH row is a real word range - exactly the
 * opener's first four words - and those two surahs get no row of their own.
 *
 * ### Why tokenisation is not [QuranText.normalise]
 *
 * The print's word stream is the corpus's raw token stream: a token that holds
 * a letter is a word, and a token that holds none (a pause mark, a prostration
 * sign) is fused onto the word before it, because that is where the print
 * paints it. Search's fold exists to match variants against each other and
 * folds ى to ي - a *search* choice that would corrupt this alignment - so the
 * classification here is its own, matching the derivation script's.
 */
internal object MushafLayout {

    /** The print's pages: the corpus's page table counts the same 604. */
    const val PAGE_COUNT = QuranCorpus.PAGE_COUNT

    private const val ASSET = "mushaf_layout.txt"

    /**
     * One printed line's record: slot digits, a row kind, a payload and
     * trailing `w`/`c` flags. A SURAH_NAME payload is a surah number; a
     * BASMALAH or TEXT payload is a 1-based word-ordinal range.
     */
    private val recordRegex = Regex("(\\d+)([HBA])(\\d+)?(?:-(\\d+))?([wc]*)")

    /**
     * The codepoints the corpus counts as letters: hamza through ya, then the
     * alif wasla through the small voweled forms. Digits, diacritics, small
     * marks and the tatweel are deliberately absent: a token holding none of
     * these is a mark, and the fusion rule attaches it to the word before it -
     * where the print paints it.
     */
    private val LETTER = Regex("[\\u0621-\\u064A\\u0671-\\u06D3]")

    private val WHITESPACE = Regex("\\s+")

    /** One page of the print, in print order. */
    class Page internal constructor(
        /** 1..604. */
        val number: Int,
        val lines: List<Line>,
    )

    /** One printed line of a page. */
    class Line internal constructor(
        /** 1..n within its page, top to bottom as the print carries it. */
        val slot: Int,
        val kind: Kind,
        /**
         * The surah the row's name names, as the record carries it; 0 on every
         * other row, which carries verse words instead of a name.
         */
        val namedSurah: Int,
        /**
         * Whether the print centres this row. The surah-name and basmalah rows
         * always are, so the asset carries the flag only for TEXT rows.
         */
        val centered: Boolean,
        /**
         * The surah and ayah whose verse-end marker prints before this line's
         * own words, when the verse that ended on the previous text row broke
         * at a line break; both 0 when nothing wraps. Resolved at load from the
         * record order, so a record claiming a wrap the print could not have
         * carried fails the load, rather than printing a marker off the page.
         */
        val wrappedMarkerSurah: Int,
        val wrappedMarkerAyah: Int,
        /**
         * The verse words on this line, in print order, contiguous over the
         * corpus. Empty only for a SURAH_NAME row: a basmalah row points at
         * its surah's opener, and a text row at verse words.
         */
        val spans: List<Span>,
    ) {
        enum class Kind { SURAH_NAME, BASMALAH, TEXT }
    }

    /**
     * One verse-word run on a line: a maximal run of one verse's consecutive
     * words that this line owns. A line holding more than one verse is more
     * than one span, split exactly where the verse changes - which is also
     * where the print sets a marker between two chunks.
     */
    class Span internal constructor(
        val surah: Int,
        val ayah: Int,
        /** First and last of the verse's own words on this line, inclusive. */
        val firstWord: Int,
        val lastWord: Int,
        /**
         * Whether this row leaves the verse's end marker to the next text row:
         * the verse ends exactly at the break, and the print sets its marker
         * alone at the head of the row that carries the verse's continuation.
         * Resolved once here, in the same walk that establishes the wrap, so
         * the renderer never has to look ahead a row it does not own and no
         * second copy of the placement rule can disagree with this one.
         */
        val markerWrapped: Boolean = false,
    ) {
        /** Whether the verse's final word is this span's final word. */
        val endsAyah: Boolean
            get() = lastWord == wordCountOf(surah, ayah) - 1

        /** How many of the verse's words this span carries. */
        val wordCount: Int
            get() = lastWord - firstWord + 1
    }

    /** All 604 pages, in order. */
    val pages: List<Page> by lazy { build() }

    /**
     * Forces the one walk that fills every table this object answers from.
     *
     * [pages] is a single visit over the asset, and the same visit fills
     * [verseHeadPage], [surahHeadPage] and [pageOpener]; nothing else does.
     * An accessor that reads one of those tables owes correctness to the
     * walk having happened first - which a test that happens to touch
     * [page] on its way to [openingVerse] hides, and the first caller in a
     * running app (a page turn asking for the print's opener before any
     * Madinah page has composed) is not such a test. This is exactly why the
     * lazy is forced from every reader of a filled table rather than left to
     * whoever gets there first.
     */
    private fun built(): List<Page> = pages

    /** The print's [page], 1..604. */
    fun page(page: Int): Page {
        require(page in 1..PAGE_COUNT) { "the print has no page $page" }
        return pages[page - 1]
    }

    /**
     * The page a surah's ink starts on: where its basmalah row prints, or its
     * first words when it has none. The print and metadata.xml disagree about
     * which verse *starts* a page (25 documented boundary attributions), but on
     * this question they agree: a surah's first verse prints on the page whose
     * header block it sits under. The tests keep both tables honest.
     */
    fun pageOfSurah(surah: Int): Int {
        require(surah in 1..QuranCorpus.SURA_COUNT) { "not a surah: $surah" }
        built()
        return surahHeadPage[surah]
    }

    /**
     * The page the print sets [verse]'s opening word on.
     *
     * This is the print's own attribution, which is what the Madinah page
     * surface must navigate by: it is the page whose ink actually carries the
     * verse's start. Where the print breaks a page differently than the
     * corpus's own page table does, this answers a page the metadata row does
     * not - 56 verses, pinned in `MushafLayoutTest` so the seam stays a fact
     * that is read, not a difference somebody tripped over.
     */
    fun pageOfVerse(surah: Int, ayah: Int): Int {
        built()
        val verse = QuranCorpus.indexOf(surah, ayah)
        require(verse >= 0) { "not a verse: $surah:$ayah" }
        return verseHeadPage[verse]
    }

    /**
     * The verse as the print attributes it: its own numbers and the page its
     * opening word prints on.
     *
     * The Madinah surface's taps and turns build references here, so a
     * selection's page and the page on screen are one fact, not two tables
     * that are compared and disagree.
     */
    fun ref(surah: Int, ayah: Int): QuranRef {
        built()
        val verse = QuranCorpus.indexOf(surah, ayah)
        require(verse >= 0) { "not a verse: $surah:$ayah" }
        return QuranRef(
            surah = surah,
            ayah = ayah,
            page = verseHeadPage[verse],
        )
    }

    /**
     * The verse whose opening word prints highest on [page] - the verse a
     * page turn lands on in this layout.
     *
     * This is the print's answer to "where does page N start", which is not
     * always the metadata's (25 agreed disagreements, pinned in the tests);
     * the flowing layout keeps metadata's answer, so the two surfaces each
     * write a position whose page is the page actually being read.
     */
    fun openingVerse(page: Int): QuranRef {
        require(page in 1..PAGE_COUNT) { "the print has no page $page" }
        built()
        return checkNotNull(pageOpener[page]) { "the print gives page $page no opening verse" }
    }

    /** How many words of a verse the print sets, or 0 for a reference that is none. */
    fun wordCountOf(surah: Int, ayah: Int): Int {
        val verse = QuranCorpus.indexOf(surah, ayah)
        return if (verse < 0) 0 else wordCounts[verse]
    }

    /**
     * A verse's words as the print sets them: the corpus's letter tokens, in
     * order, with each following pause mark or prostration sign fused on. Word
     * `n` of the list is the layout's ordinal `n` - the same indexing the
     * span records and this list share.
     */
    fun wordsOf(surah: Int, ayah: Int): List<String> {
        val verse = QuranCorpus.indexOf(surah, ayah)
        require(verse >= 0) { "not a verse: $surah:$ayah" }
        return fusedWords[verse] ?: fuse(verse, "$surah:$ayah").also { fusedWords[verse] = it }
    }

    // --- The corpus's word stream -----------------------------------------

    private val wordCounts: IntArray by lazy {
        IntArray(QuranCorpus.ayahs.size) { index ->
            var count = 0
            for (token in QuranCorpus.ayahs[index].textArabic.trim().split(WHITESPACE)) {
                if (LETTER.containsMatchIn(token)) count++
            }
            count
        }
    }

    /**
     * `wordStart[i]` is the global ordinal of the word that begins verse list
     * position `i`, and the last entry is the stream's end. A layout record is
     * an index into this one stream, so a record and the corpus cannot
     * disagree about which verse an ordinal belongs to.
     */
    private val wordStart: IntArray by lazy {
        val counts = wordCounts
        val start = IntArray(counts.size + 1)
        var total = 0
        for (i in counts.indices) {
            start[i] = total
            total += counts[i]
        }
        start[counts.size] = total
        start
    }

    private val totalWords: Int
        get() = wordStart[wordCounts.size]

    private val fusedWords = arrayOfNulls<List<String>>(QuranCorpus.VERSE_COUNT)

    private fun fuse(verse: Int, name: String): List<String> {
        val words = ArrayList<String>()
        for (token in QuranCorpus.ayahs[verse].textArabic.trim().split(WHITESPACE)) {
            if (LETTER.containsMatchIn(token)) {
                words.add(token)
            } else if (words.isNotEmpty()) {
                // The pause marks and the prostration sign ride on the word
                // before them, where the print paints them.
                words[words.size - 1] = words[words.size - 1] + token
            } else {
                throw IllegalStateException("verse $name opens with a bare mark: $token")
            }
        }
        return words
    }

    // --- The record walk ---------------------------------------------------

    private val surahHeadPage = IntArray(QuranCorpus.SURA_COUNT + 1)

    /**
     * `verseHeadPage[verseIndex]` is the page the print sets that verse's
     * opening word on - this layout's own attribution of every verse to the
     * page a reader must be on to see its first printed word.
     */
    private val verseHeadPage = IntArray(QuranCorpus.VERSE_COUNT)

    /**
     * `pageOpener[page]` is the verse whose opening word prints highest on the
     * page - the print's own page-start attribution, which the test pins
     * against metadata.xml's 25 agreed disagreements.
     */
    private val pageOpener = arrayOfNulls<QuranRef>(PAGE_COUNT + 1)

    /**
     * One parsed record, before its spans and wrap verse are attached.
     */
    private class Raw(
        val page: Int,
        val slot: Int,
        val kind: Line.Kind,
        val centered: Boolean,
        val wraps: Boolean,
        /** The surah-name row's surah; 0 on word rows. */
        val surah: Int,
        /** The word-ordinal range, 0-based inclusive; -1 on a name row. */
        val from: Int,
        val to: Int,
    )

    private fun build(): List<Page> {
        val text = QuranCorpus.resource(ASSET, QuranCorpus.MUSHAF_LAYOUT_HASH)
            .toString(Charsets.UTF_8)

        val raws = ArrayList<Raw>(9100)
        var loadedPages = 0
        var namedSurah = 0
        for (line in text.lineSequence()) {
            if (line.isBlank() || line.startsWith("#")) continue
            val fields = line.trim().split(WHITESPACE)
            val page = fields.first().toInt()
            require(page == loadedPages + 1) { "the print's page $page follows $loadedPages" }
            loadedPages = page
            require(fields.size > 1) { "page $page carries no lines" }
            var slot = 0
            for (field in fields.drop(1)) {
                slot++
                val match = recordRegex.matchEntire(field)
                    ?: throw IllegalStateException(
                        "page $page slot $slot: record \"$field\" is not a layout record"
                    )
                val kind = when (match.groupValues[2]) {
                    "H" -> Line.Kind.SURAH_NAME
                    "B" -> Line.Kind.BASMALAH
                    else -> Line.Kind.TEXT
                }
                val payload = match.groupValues[3]
                val rangeTo = match.groupValues[4]
                val flags = match.groupValues[5]
                when (kind) {
                    Line.Kind.SURAH_NAME -> require(
                        payload.isNotEmpty() && rangeTo.isEmpty() && flags.isEmpty()
                    ) { "page $page slot $slot: a surah-name row carries no flag or range" }

                    Line.Kind.BASMALAH -> require(
                        payload.isNotEmpty() && rangeTo.isNotEmpty() && flags.isEmpty()
                    ) { "page $page slot $slot: a basmalah row carries its word range and no flag" }

                    Line.Kind.TEXT -> require(payload.isNotEmpty() && rangeTo.isNotEmpty()) {
                        "page $page slot $slot: a text row carries its word range"
                    }
                }
                val surah: Int
                if (kind == Line.Kind.SURAH_NAME) {
                    surah = payload.toInt()
                    require(surah == namedSurah + 1) {
                        "page $page: a surah-name row names $surah after $namedSurah"
                    }
                    namedSurah = surah
                } else {
                    surah = 0
                }
                raws.add(
                    Raw(
                        page = page,
                        slot = slot,
                        kind = kind,
                        centered = flags.contains('c') || kind != Line.Kind.TEXT,
                        wraps = flags.contains('w'),
                        surah = surah,
                        from = if (kind == Line.Kind.SURAH_NAME) -1 else payload.toInt() - 1,
                        to = if (kind == Line.Kind.SURAH_NAME) -1 else rangeTo.toInt() - 1,
                    )
                )
            }
        }
        require(loadedPages == PAGE_COUNT) {
            "the print loads $loadedPages pages, not the $PAGE_COUNT its reader opens"
        }
        require(namedSurah == QuranCorpus.SURA_COUNT) {
            "the print names $namedSurah surahs, not the ${QuranCorpus.SURA_COUNT} the corpus holds"
        }

        val pageLines = ArrayList<ArrayList<Line>>(PAGE_COUNT)
        var pageOfCurrent = 0
        var slotSoFar = 0

        // The records tile the corpus, in order, with nothing missing and
        // nothing repeated. This is the gate that stops an edited layout from
        // moving a verse onto a page it does not print on.
        var next = 0
        var verse = 0
        var verseStart = wordStart[verse]
        var verseEnd = wordStart[verse + 1]

        var pendingHeaderSurah = 0
        var previousEnd: Span? = null
        // The row the previous span was built into, so a wrap can mark the span
        // that left its marker to this row. The `Line` already holds the same
        // list, and replacing the element, not copying the list, is what keeps
        // them one structure.
        var previousRowSpans: ArrayList<Span>? = null
        val wrappedVerses = HashSet<Int>()

        for (raw in raws) {
            if (raw.page != pageOfCurrent) {
                require(raw.page == pageOfCurrent + 1) {
                    "the print's page ${raw.page} follows $pageOfCurrent"
                }
                pageOfCurrent = raw.page
                pageLines.add(ArrayList(15))
                slotSoFar = 0
            }
            require(raw.slot == slotSoFar + 1) {
                "page ${raw.page}: a record claims slot ${raw.slot} after $slotSoFar"
            }
            slotSoFar++

            if (raw.kind == Line.Kind.SURAH_NAME) {
                require(pendingHeaderSurah == 0) {
                    "page ${raw.page}: a second surah-name row before the first surah's words"
                }
                pendingHeaderSurah = raw.surah
                pageLines.last().add(
                    Line(
                        slot = raw.slot,
                        kind = raw.kind,
                        namedSurah = raw.surah,
                        centered = raw.centered,
                        wrappedMarkerSurah = 0,
                        wrappedMarkerAyah = 0,
                        spans = emptyList(),
                    )
                )
                continue
            }

            require(raw.from == next) {
                "page ${raw.page} slot ${raw.slot} starts at word ${raw.from + 1}, " +
                    "while the corpus's next unplaced word is ${next + 1}"
            }
            val spans = ArrayList<Span>(4)
            var ordinal = raw.from
            while (ordinal <= raw.to) {
                while (ordinal >= verseEnd) {
                    verse++
                    verseStart = verseEnd
                    verseEnd += wordCounts[verse]
                }
                val ayah = QuranCorpus.ayahs[verse]
                val spanEnd = minOf(raw.to, verseEnd - 1)
                spans.add(
                    Span(
                        surah = ayah.surahNumber,
                        ayah = ayah.ayahNumber,
                        firstWord = ordinal - verseStart,
                        lastWord = spanEnd - verseStart,
                    )
                )
                ordinal = spanEnd + 1
            }
            next = raw.to + 1

            if (pendingHeaderSurah != 0) {
                require(spans.first().surah == pendingHeaderSurah) {
                    "page ${raw.page} slot ${raw.slot}: the words after the surah-name row " +
                        "belong to ${spans.first().surah}, not to its ${pendingHeaderSurah}"
                }
                pendingHeaderSurah = 0
            }
            if (raw.kind == Line.Kind.BASMALAH) {
                val opener = spans.single()
                require(opener.firstWord == 0 && opener.ayah == 1 && opener.lastWord == 3) {
                    "page ${raw.page} slot ${raw.slot}: a basmalah row is not its surah's opener"
                }
            }

            var wrapsSurah = 0
            var wrapsAyah = 0
            if (raw.wraps) {
                // A row that takes a wrapped marker follows - possibly across a
                // page or a surah opening - the text row where its verse ended.
                val ended = requireNotNull(previousEnd) {
                    "page ${raw.page} slot ${raw.slot}: a wrapped marker with no text row before it"
                }
                require(ended.endsAyah) {
                    "page ${raw.page} slot ${raw.slot}: its marker wraps " +
                        "${ended.surah}:${ended.ayah}, which the previous text row does not end"
                }
                val leads = spans.first()
                require(QuranCorpus.indexOf(leads.surah, leads.ayah) ==
                    QuranCorpus.indexOf(ended.surah, ended.ayah) + 1) {
                    "page ${raw.page} slot ${raw.slot}: the wrapped marker's verse does not " +
                        "precede the row's own verse"
                }
                wrapsSurah = ended.surah
                wrapsAyah = ended.ayah
                // The span that ended the verse is now leaving its marker to
                // this row, which the renderer must know before it draws
                // either.
                val spansOf = requireNotNull(previousRowSpans)
                val last = spansOf.last()
                spansOf[spansOf.lastIndex] = Span(
                    surah = last.surah,
                    ayah = last.ayah,
                    firstWord = last.firstWord,
                    lastWord = last.lastWord,
                    markerWrapped = true,
                )
                require(wrappedVerses.add(QuranCorpus.reference(wrapsSurah, wrapsAyah))) {
                    "two text rows wrap the marker of $wrapsSurah:$wrapsAyah"
                }
            }

            for (span in spans) {
                // The opener's own row is where the surah's ink starts, which
                // is the print's answer for where a reader jumping to the
                // surah lands.
                if (span.firstWord == 0) {
                    if (span.ayah == 1 && surahHeadPage[span.surah] == 0) {
                        surahHeadPage[span.surah] = raw.page
                    }
                    if (verseHeadPage[QuranCorpus.indexOf(span.surah, span.ayah)] == 0) {
                        verseHeadPage[QuranCorpus.indexOf(span.surah, span.ayah)] = raw.page
                    }
                    if (pageOpener[raw.page] == null) {
                        pageOpener[raw.page] = QuranRef(
                            surah = span.surah,
                            ayah = span.ayah,
                            page = raw.page,
                        )
                    }
                }
            }
            pageLines.last().add(
                Line(
                    slot = raw.slot,
                    kind = raw.kind,
                    namedSurah = 0,
                    centered = raw.centered,
                    wrappedMarkerSurah = wrapsSurah,
                    wrappedMarkerAyah = wrapsAyah,
                    spans = spans,
                )
            )
            if (raw.kind == Line.Kind.TEXT) {
                previousEnd = spans.last()
                previousRowSpans = spans
            }
        }

        require(pendingHeaderSurah == 0) {
            "the print's last surah-name row has no words under it"
        }
        require(next == totalWords) {
            "the layout places $next of the corpus's $totalWords words"
        }
        for (surah in 1..QuranCorpus.SURA_COUNT) {
            require(surahHeadPage[surah] != 0) {
                "the print does not place surah $surah's opening words"
            }
        }
        for (verse in 0 until QuranCorpus.VERSE_COUNT) {
            require(verseHeadPage[verse] != 0) {
                val ayah = QuranCorpus.ayahs[verse]
                "the print does not place ${ayah.surahNumber}:${ayah.ayahNumber}'s opening word"
            }
        }
        for (page in 1..PAGE_COUNT) {
            require(pageOpener[page] != null) { "the print gives page $page no opening verse" }
        }

        return pageLines.mapIndexed { index, lines -> Page(number = index + 1, lines = lines) }
    }
}
