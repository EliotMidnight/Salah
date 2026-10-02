package com.example.data.quran

/**
 * Arabic text normalisation, for search.
 */
object QuranText {

    /**
     * The corpus folded for Arabic matching, with the offsets back to the original.
     *
     * **[normalised] and [arabicOrigins] are two views of this one pass**, so they
     * cannot disagree about where a letter went. That is the whole reason this is a
     * `Folded` rather than two independent lists. `QuranSearch` reads it directly, so
     * a highlight and a ranking are always positioned against the same fold.
     */
    val arabicFolds: List<Folded> by lazy {
        QuranCorpus.ayahs.map { fold(it.textArabic) }
    }

    /** Folded Arabic, for matching. See [arabicFolds]. */
    val normalised: List<String> by lazy { arabicFolds.map { it.text } }

    /**
     * For each verse, where each folded character came from in the original.
     */
    val arabicOrigins: List<IntArray> by lazy { arabicFolds.map { it.origin } }

    /** Lowercased English, for the same reason and the same laziness. */
    val lowerEnglish: List<String> by lazy {
        QuranCorpus.ayahs.map { it.textEnglish.lowercase() }
    }

    /**
     * Folds [text] for matching and remembers where every character came from.
     *
     * **Why the origin map exists.** Folding *deletes*: the harakat, the dagger alif,
     * the small high marks and the tatweel all go, and four letters fold onto others.
     * `ٱلرَّحْمَٰنِ` (nine characters) becomes `الرحمن` (six). So a match found at offset
     * 20 of the folded text is at nothing like offset 20 of the text a reader is
     * looking at, and a highlight computed in folded space lands on the wrong words -
     * further off the further into the verse it is.
     */
    fun fold(text: String): Folded {
        val out = StringBuilder(text.length)
        val origin = IntArray(text.length)
        var written = 0
        for (at in text.indices) {
            val ch = text[at]
            val kept = when (ch) {
                in '\u064B'..'\u065F', '\u0670', in '\u06D6'..'\u06ED', '\u0640' -> null
                '\u0622', '\u0623', '\u0625', '\u0671' -> '\u0627'
                '\u0624', '\u0626' -> '\u0621'
                '\u0629' -> '\u0647'
                '\u0649' -> '\u064A'
                ' ', '\t', '\n', '\r' -> ' '
                else -> ch
            } ?: continue
            out.append(kept)
            origin[written++] = at
        }
        return Folded(out.toString(), origin.copyOf(written))
    }

    /**
     * Arabic, folded for matching.
     *
     * - U+064B..U+065F, U+0670, U+06D6..U+06ED - the harakat, the dagger alif
     *   and the small high marks the Uthmani script places above letters.
     * - U+0640 - tatweel, the elongation a *justified* rendering inserts. This
     *   matters more here than in most apps: the reader justifies its own text,
     *   so the text being searched and the text on screen can differ by runs of
     *   tatweel, and a search that did not strip it would miss its own page.
     * - U+0622 U+0623 U+0625 U+0671 - alif with madda, hamza above, hamza
     *   below and the bare alef of the Uthmani orthography, all one letter.
     * - U+0624 U+0626 - hamza-carrying waw and ya, folded to their hamza.
     * - U+0629 - taa marbuta, folded to haa: it is the same letter at the end
     *   of a word in a construct state, and a searcher does not know which.
     * - U+0649 - alef maksura, folded to yaa.
     */
    fun normalise(text: String): String = fold(text).text

    /**
     * The search form of a query: normalised, lowercased, whitespace-collapsed.
     */
    fun normaliseQuery(text: String): String =
        normalise(text).lowercase().replace(Regex("\\s+"), " ").trim()

    /**
     * Every whitespace-separated term of a search query.
     */
    fun terms(query: String): List<String> =
        normaliseQuery(query).split(' ').filter { it.isNotBlank() }

    /**
     * How good a match this is: lower is better.
     *
     * 1. A hit that **starts a word** beats one buried inside a longer word.
     *    Searching "mercy" and being shown a verse that says "unmerciful"
     *    first is the failure this prevents - the letters matched, the word did
     *    not.
     * 2. A hit **earlier in the text** beats one later, so a verse that opens
     *    with the word you typed comes first. Small weight, so it only ever
     *    breaks a tie between two equally good hits.
     * 3. Nothing matched at all is [NO_MATCH], and must sort last.
     */
    fun rank(haystack: String, terms: List<String>): Int {
        var score = 0
        for (term in terms) {
            val at = haystack.indexOf(term)
            if (at < 0) return NO_MATCH
            val startsWord = at == 0 || !haystack[at - 1].isLetter()
            // 100 for a substring hit against 10 for a whole-word one, so that a
            // position advantage (capped below at 9) can never promote a
            // substring hit over a whole-word one.
            score += if (startsWord) 10 else 100
            score += (at.coerceAtMost(9))
        }
        return score
    }

    /** The score a text gets when it does not contain the query at all. */
    const val NO_MATCH = Int.MAX_VALUE

    /**
     * Folded text, and where each folded character came from.
     */
    class Folded(val text: String, val origin: IntArray) {

        /**
         * The span **in the original text** that [foldedStart]..[foldedStart] +
         * [foldedLength] covers, or null when the fold is empty.
         *
         * A length longer than what remains is clamped rather than rejected: a term can
         * match at the very end of the folded text, and that is a real match, not a
         * malformed request.
         */
        fun originalRangeOf(foldedStart: Int, foldedLength: Int): IntRange? {
            if (foldedStart < 0 || foldedStart >= origin.size || foldedLength <= 0) return null
            val lastFolded = (foldedStart + foldedLength - 1).coerceAtMost(origin.size - 1)
            return origin[foldedStart] until (origin[lastFolded] + 1)
        }

        /**
         * The span in the original text that [term] matches, or null.
         */
        fun originalRangeOf(term: String): IntRange? {
            if (term.isEmpty()) return null
            val at = text.indexOf(term)
            if (at < 0) return null
            return originalRangeOf(at, term.length)
        }
    }
}