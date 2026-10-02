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
     */
    fun searchSurahs(query: String, limit: Int = PAGE_SIZE): SurahResults {
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

        return SurahResults(scored.take(limit).map { it.first }, scored.size)
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
     * Verses matching [query], best first, capped at [limit].
     */
    fun searchVerses(query: String, limit: Int = PAGE_SIZE): VerseResults {
        val terms = QuranText.terms(query)
        if (terms.isEmpty()) return VerseResults(emptyList(), 0)

        val arabic = QuranText.normalised
        val english = QuranText.lowerEnglish
        val ayahs = QuranCorpus.ayahs
        val results = ArrayList<QuranSearchHit>(limit * 2)

        for (index in ayahs.indices) {
            val inArabic = allPresent(arabic[index], terms)
            val inEnglish = allPresent(english[index], terms)
            if (!inArabic && !inEnglish) continue

            val ayah = ayahs[index]
            val surah = QuranDataSource.getSurahByNumber(ayah.surahNumber) ?: continue

            // A verse that matches in both scripts is a better answer than one
            // that matches in one, so the score is the sum rather than a choice.
            val score = when {
                inArabic && inEnglish -> 0
                inArabic -> QuranText.rank(arabic[index], terms)
                else -> QuranText.rank(english[index], terms) + 1
            }
            val field = if (inArabic) {
                QuranSearchHit.Field.ARABIC
            } else {
                QuranSearchHit.Field.ENGLISH
            }
            results += QuranSearchHit(
                ayah = ayah,
                surah = surah,
                ref = QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber),
                matchedIn = field,
                range = highlightRange(index, field, terms)
            )
        }

        // The total is the *count before the page is taken*, so a caller can say how
        // many there are rather than how many it was given. See [VerseResults].
        val total = results.size
        val page = results
            .sortedWith(
                compareBy(
                    { hitRank(it, terms) },
                    { it.ayah.surahNumber },
                    { it.ayah.ayahNumber }
                )
            )
            .take(limit)
        return VerseResults(page, total)
    }

    private fun hitRank(hit: QuranSearchHit, terms: List<String>): Int {
        val index = verseIndex(hit)
        val source = when (hit.matchedIn) {
            QuranSearchHit.Field.ARABIC -> QuranText.normalised[index]
            QuranSearchHit.Field.ENGLISH -> QuranText.lowerEnglish[index]
            QuranSearchHit.Field.SURAH_NAME -> hit.surah.englishName.lowercase()
        }
        val base = QuranText.rank(source, terms)
        // English is the language a reader is most likely reading in, so an
        // English match is not behind an Arabic one by much. One point, not a
        // rank: it should break a tie, not decide the order.
        return if (hit.matchedIn == QuranSearchHit.Field.ENGLISH) base + 1 else base
    }

    private fun verseIndex(hit: QuranSearchHit): Int = QuranCorpus.indexOf(
        hit.ayah.surahNumber,
        hit.ayah.ayahNumber
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
    /** The best [VerseResults.total] matches, capped at the requested limit. */
    val hits: List<QuranSearchHit>,
    /** How many verses match at all. */
    val total: Int
) {
    /** True when [hits] is a strict subset — the case a caller must not describe as "all". */
    val isTruncated: Boolean get() = total > hits.size
}

/** One page of surah-name matches, and how many there really are. See [VerseResults]. */
data class SurahResults(
    /** The best [SurahResults.total] matches, capped at the requested limit. */
    val hits: List<Surah>,
    /** How many surahs' names match at all. */
    val total: Int
) {
    /** True when [hits] is a strict subset. See [VerseResults.isTruncated]. */
    val isTruncated: Boolean get() = total > hits.size
}
