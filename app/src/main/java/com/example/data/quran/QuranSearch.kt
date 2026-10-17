package com.example.data.quran

import com.example.data.model.Ayah
import com.example.data.model.QuranRef
import com.example.data.model.Surah

/**
 * One search result, with enough about the match to render it honestly.
 *
 * ### Why the match carries a highlight range
 *
 * A result list that shows the whole verse and gives no hint *why* it matched
 * makes the reader read 6,236 verses to check the search worked. The range is
 * the answer to "why is this here", and it is free: the caller already knows
 * the index the term was found at.
 */
data class QuranSearchHit(
    val ayah: Ayah,
    val surah: Surah,
    /** Where the reader should go. */
    val ref: QuranRef,
    /** Which text matched. Named `matchedIn` rather than `field` on purpose. */
    val matchedIn: Field,
    val range: IntRange?
) {
    enum class Field { ARABIC, ENGLISH, SURAH_NAME }

    /**
     * The text this hit matched inside - what a row should highlight.
     */
    val subject: String
        get() = when (matchedIn) {
            Field.ARABIC -> ayah.textArabic
            Field.ENGLISH -> ayah.textEnglish
            Field.SURAH_NAME -> surah.englishName
        }
}

/**
 * Verse search, and the surah-name search that has to be kept separate from it.
 *
 *
 * 1. **It was O(n) with regex compilation inside the loop.** Every keystroke
 *    normalised all 6,236 verses, allocating three compiled `Regex` objects per
 *    verse, on the main thread. See [QuranText] for what replaced it.
 * 2. **A surah-name match returned every verse of that surah.** The old filter
 *    had `surah.englishName.contains(query)` in the *verse* predicate, so
 *    searching "Maryam" matched the surah name - and then returned all 97 of
 *    her verses, none of which contained the word. The index sheet then showed
 *    "97 verses found" and a reader tapping the first one landed in the middle
 *    of an unrelated search. Surah names are now their own result tier, above
 *    the verses, which is also where every serious reader puts them.
 *
 *    The total comes from the same scan that fills the page, so saying it costs
 *    nothing. A separate counting pass would double the work over 6,236 verses on
 *    every debounced keystroke to arrive at the same number.
 */
object QuranSearch {

    /**
     * How many verses one keystroke returns.
     *
     */
    const val PAGE_SIZE = 50

    /**
     * Surahs whose name matches, best first.
     *
     * [skip] and [limit] are a window into one ranked order, not two searches:
     * every window slices the same [SurahResults.total] matches the reader was
     * shown the first of, so "more" can extend a list the reader is already
     * reading without its rows ever moving. The total does not answer a skip -
     * it is how many match at all - and it is the same number at every window,
     * which is what stops a header from renaming the list mid-read.
     */
    fun searchSurahs(query: String, limit: Int = PAGE_SIZE, skip: Int = 0): SurahResults {
        require(skip >= 0) { "skip is a number of rows to pass over, not $skip" }
        require(limit >= 0) { "limit is a number of rows, not $limit" }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SurahResults(emptyList(), 0)
        val folded = QuranText.normaliseQuery(trimmed)
        val asNumber = trimmed.toIntOrNull()

        val scored = QuranDataSource.SURAHS
            .mapNotNull { surah ->
                val score = surahNameScore(surah, trimmed, folded, asNumber)
                if (score == Int.MAX_VALUE) null else surah to score
            }
            .sortedWith(compareBy({ it.second }, { it.first.number }))

        return SurahResults(
            scored.drop(skip).take(limit).map { it.first },
            scored.size
        )
    }

    private fun surahNameScore(
        surah: Surah,
        trimmed: String,
        folded: String,
        asNumber: Int?
    ): Int {
        if (asNumber != null && surah.number == asNumber) return 0

        val english = surah.englishName.lowercase()
        val meaning = surah.englishTranslation.lowercase()
        val arabic = QuranText.normalise(surah.arabicName).lowercase()

        // An exact name is the reader's answer, not a near miss.
        if (english == trimmed.lowercase()) return 1
        if (english.startsWith(trimmed.lowercase())) return 2
        if (english.contains(trimmed.lowercase())) return 3
        if (meaning.contains(trimmed.lowercase())) return 4
        if (folded.isNotEmpty() && arabic.contains(folded)) return 5
        return Int.MAX_VALUE
    }

    /**
     * Verses matching [query], best first.
     *
     * [skip] and [limit] are a window into one ranked order: `searchVerses(q,
     * skip = 50)` holds the matches the reader was not shown the first time -
     * the same ranks, none re-decided, none repeating a row already rendered.
     * [VerseResults.total] is the count before the window is taken, so it says
     * how many verses match rather than how many this window fit, and does not
     * answer [skip] - the same total at every window is what keeps a header
     * honest while the reader asks for more.
     */
    fun searchVerses(query: String, limit: Int = PAGE_SIZE, skip: Int = 0): VerseResults {
        require(skip >= 0) { "skip is a number of rows to pass over, not $skip" }
        require(limit >= 0) { "limit is a number of rows, not $limit" }
        val terms = QuranText.terms(query)
        if (terms.isEmpty()) return VerseResults(emptyList(), 0)

        val arabic = QuranText.normalised
        val english = QuranText.lowerEnglish
        val ayahs = QuranCorpus.ayahs
        val matches = ArrayList<Match>(minOf(limit, ayahs.size) * 2)

        for (index in ayahs.indices) {
            val inArabic = allPresent(arabic[index], terms)
            val inEnglish = allPresent(english[index], terms)
            if (!inArabic && !inEnglish) continue

            val ayah = ayahs[index]
            val surah = QuranDataSource.getSurahByNumber(ayah.surahNumber) ?: continue

            // The rank is taken **here**, once per matching verse, rather than in
            // the comparator. It used to be in the comparator, which re-ran this
            // same rank - a scan of the verse already scanned - for every
            // comparison the sort made: O(n log n) scans to order 50 rows.
            //
            // English is the language a reader is most likely reading in, so an
            // English match is not behind an Arabic one by much. One point, not
            // a rank: it should break a tie, not decide the order.
            val field = if (inArabic) {
                QuranSearchHit.Field.ARABIC
            } else {
                QuranSearchHit.Field.ENGLISH
            }
            val rank = if (field == QuranSearchHit.Field.ARABIC) {
                QuranText.rank(arabic[index], terms)
            } else {
                QuranText.rank(english[index], terms) + 1
            }
            matches += Match(index = index, ayah = ayah, surah = surah, field = field, rank = rank)
        }

        // The total is the *count before the window is taken*, so a caller can say
        // how many there are rather than how many it was given. See [VerseResults].
        val total = matches.size
        val page = matches
            .sortedWith(compareBy({ it.rank }, { it.ayah.surahNumber }, { it.ayah.ayahNumber }))
            .drop(skip)
            .take(limit)
            // Only the returned rows get their highlight computed. A broad query
            // matches thousands of verses and shows fifty, and the ranges of the
            // other few thousand were computed only to be dropped with them.
            .map { match ->
                QuranSearchHit(
                    ayah = match.ayah,
                    surah = match.surah,
                    ref = QuranRef(
                        match.ayah.surahNumber,
                        match.ayah.ayahNumber,
                        match.ayah.pageNumber
                    ),
                    matchedIn = match.field,
                    range = highlightRange(match.index, match.field, terms)
                )
            }
        return VerseResults(page, total)
    }

    /** A matching verse, with the rank its row will be ordered by. */
    private class Match(
        val index: Int,
        val ayah: Ayah,
        val surah: Surah,
        val field: QuranSearchHit.Field,
        val rank: Int
    )

    /**
     * The span **in the verse as the reader sees it** that [terms] matched.
     */
    private fun highlightRange(
        index: Int,
        field: QuranSearchHit.Field,
        terms: List<String>
    ): IntRange? {
        return when (field) {
            QuranSearchHit.Field.ARABIC -> {
                val fold = QuranText.arabicFolds[index]
                for (term in terms) {
                    val range = fold.originalRangeOf(term)
                    if (range != null) return range
                }
                null
            }

            QuranSearchHit.Field.ENGLISH -> {
                val english = QuranText.lowerEnglish[index]
                for (term in terms) {
                    val at = english.indexOf(term)
                    if (at >= 0) return at until (at + term.length)
                }
                null
            }

            QuranSearchHit.Field.SURAH_NAME -> null
        }
    }

    private fun allPresent(haystack: String, terms: List<String>): Boolean {
        for (term in terms) if (!haystack.contains(term)) return false
        return true
    }
}

/**
 * One page of search results, and how many there really are.
 */
data class VerseResults(
    /**
     * One window of the matches, ranked best first - the first window opens at
     * the best, later windows at [com.example.data.quran.QuranSearch.searchVerses]'s skip.
     */
    val hits: List<QuranSearchHit>,
    /** How many verses match at all. */
    val total: Int
) {
    /**
     * True when [hits] is a strict subset — the case a caller must not describe as "all".
     *
     * "More remain" only for the first window, where a caller's skip is 0:
     * while paging, more remain exactly when `skip + hits.size < total`, and
     * the caller's own window is the thing to compare.
     */
    val isTruncated: Boolean get() = total > hits.size
}

/** One page of surah-name matches, and how many there really are. See [VerseResults]. */
data class SurahResults(
    /**
     * One window of the matches, ranked best first. See [VerseResults.hits].
     */
    val hits: List<Surah>,
    /** How many surahs' names match at all. */
    val total: Int
) {
    /** True when [hits] is a strict subset. See [VerseResults.isTruncated]. */
    val isTruncated: Boolean get() = total > hits.size
}
