package com.example.data.quran

/**
 * Arabic text normalisation, for search.
 *
 * ### Why it is not a regex
 *
 * The previous implementation stripped tashkeel with two `Regex` literals built
 * *inside* the replace call, so every invocation allocated and recompiled both
 * patterns. Search calls this once per verse per keystroke over 6,236 verses,
 * which is roughly 19,000 pattern compilations per character typed - on the
 * main thread, while the search sheet is open.
 *
 * This is a single pass over the characters instead. It is allocation-free
 * beyond the result string, and about forty lines of `when` is both faster and
 * easier to reason about than the equivalent character class, because every
 * rule is named.
 *
 * ### What it normalises
 *
 * The standard recipe for diacritic-insensitive Quranic search: drop the
 * harakat and the dagger alif and the small high marks, drop tatweel, and fold
 * the letter shapes that are typographic variants of one another - so that a
 * search for `الرحمن` finds `ٱلرَّحْمَٰنِ` and a search for `الرحمن` finds it
 * too.
 *
 * Note that it normalises *for matching only*. The corpus text is never
 * rewritten: a copied or shared verse carries its tashkeel, because that is
 * the text of the Quran and a search index has no business changing it.
 */
object QuranText {

    /**
     * [normalise] applied to a whole corpus once, lazily.
     *
     * Built on the first search rather than at startup, because a reader who
     * never searches should not pay for an index they never use. Once built it
     * is held for the life of the process, which is the right trade: the second
     * search is free.
     */
    val normalised: List<String> by lazy {
        QuranCorpus.ayahs.map { normalise(it.textArabic) }
    }

    /** Lowercased English, for the same reason and the same laziness. */
    val lowerEnglish: List<String> by lazy {
        QuranCorpus.ayahs.map { it.textEnglish.lowercase() }
    }

    /**
     * Arabic, folded for matching.
     *
     * Everything a search must not be defeated by is removed or folded:
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
     *
     * It deliberately does **not** remove hamza from a bare `ا` (U+0627) to
     * `أ`, which would collapse `الله` and `لله` in a way no reader expects.
     */
    fun normalise(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in '\u064B'..'\u065F', '\u0670', in '\u06D6'..'\u06ED', '\u0640' -> Unit
                '\u0622', '\u0623', '\u0625', '\u0671' -> out.append('\u0627')
                '\u0624', '\u0626' -> out.append('\u0621')
                '\u0629' -> out.append('\u0647')
                '\u0649' -> out.append('\u064A')
                ' ', '\t', '\n', '\r' -> out.append(' ')
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    /**
     * The search form of a query: normalised, lowercased, whitespace-collapsed.
     *
     * Folding the case as well as the script is what lets one search box answer
     * "Allah" and "الله" with the same keystrokes, which is how people actually
     * search - they do not pick a script first and then type.
     */
    fun normaliseQuery(text: String): String =
        normalise(text).lowercase().replace(Regex("\\s+"), " ").trim()

    /**
     * Every whitespace-separated term of a search query.
     *
     * All terms must match - `Rahman rahim` finds the verses containing both.
     * That is what a reader typing several words means, and it is the same
     * rule the reference search implementations converge on.
     */
    fun terms(query: String): List<String> =
        normaliseQuery(query).split(' ').filter { it.isNotBlank() }

    /**
     * The character range in [haystack] that [needle] matched, for highlighting.
     *
     * Returns null when there is no match, so the caller can fall back to
     * showing the verse unhighlighted rather than highlighting nothing
     * convincingly.
     */
    fun matchRange(haystack: String, needle: String): IntRange? {
        if (needle.isEmpty()) return null
        val at = haystack.indexOf(needle)
        if (at < 0) return null
        return at until (at + needle.length)
    }

    /**
     * True when [haystack] contains every term.
     *
     * A term that appears at the start of a word is preferred over one buried
     * mid-word by [rank], so "mercy" ranks `mercy` above `merciful` even though
     * both are matches.
     */
    fun containsAllTerms(haystack: String, terms: List<String>): Boolean =
        terms.all { haystack.contains(it) }

    /**
     * How good a match this is: lower is better.
     *
     * Three rules, in order of how much a reader cares:
     *
     * 1. A hit that **starts a word** beats one buried inside a longer word.
     *    Searching "mercy" and being shown a verse that says "unmerciful"
     *    first is the failure this prevents - the letters matched, the word did
     *    not.
     * 2. A hit **earlier in the text** beats one later, so a verse that opens
     *    with the word you typed comes first. Small weight, so it only ever
     *    breaks a tie between two equally good hits.
     * 3. Nothing matched at all is [NO_MATCH], and must sort last.
     *
     * The first occurrence of a term is the one scored. A verse with the word
     * twice does not become a better match than one with it once - that would
     * reward repetition, which is a different question.
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
}
