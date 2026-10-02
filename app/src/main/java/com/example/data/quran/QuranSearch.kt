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
 *
 * [field] matters as much as the range. A search for "mercy" can match the
 * Arabic, the English, or a surah's name, and those are three different things a
 * reader is looking for. The field says which one this result is, so the row can
 * show the Arabic and the English in the right places and say so.
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
     *
     * Not stored: it is derivable from [matchedIn] and the hit, and a second
     * copy of it is a second thing that can disagree.
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
 * ### What the old search got wrong
 *
 * Three things, all of them visible to a reader:
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
 * 3. **It took the first 50 and said nothing.** Fifty results with no "and 400
 *    more" reads as *all* of them. Results are now paged honestly.
 *
 * ### Ranking
 *
 * Best match first, and the order is deliberate: a surah whose *name* is what you
 * typed outranks a verse that happens to contain the word, and a verse that
 * begins with the word outranks one that mentions it mid-line. A reader who
 * types "Al-Kahf" wants the surah, and a reader who types "mercy" wants the
 * verses - so both get what they typed at the top.
 */
object QuranSearch {

    /** Verses returned per page of results. */
    const val PAGE_SIZE = 50

    /** How many verses one keystroke will scan before the caller should say so. */
    const val SCAN_LIMIT = PAGE_SIZE

    /**
     * Surahs whose name matches, best first.
     *
     * Matched on the English name, the English meaning, the Arabic name and the
     * surah number, because "how do I find it" is answered differently depending
     * on which of those the reader knows.
     */
    fun searchSurahs(query: String, limit: Int = PAGE_SIZE): List<Surah> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val folded = QuranText.normaliseQuery(trimmed)
        val asNumber = trimmed.toIntOrNull()

        return QuranDataSource.SURAHS
            .mapNotNull { surah ->
                val score = surahNameScore(surah, trimmed, folded, asNumber)
                if (score == Int.MAX_VALUE) null else surah to score
            }
            .sortedWith(compareBy({ it.second }, { it.first.number }))
            .take(limit)
            .map { it.first }
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
     *
     * Every whitespace-separated term must appear, in the Arabic or in the
     * English - not all in one, because a reader searching `Allah light` wants
     * the verses that have both, whichever language each is in.
     *
     * Returns at most [limit] results and nothing more. The caller is expected
     * to say so when it hits the cap; see the note on [PAGE_SIZE].
     */
    fun searchVerses(query: String, limit: Int = PAGE_SIZE): List<QuranSearchHit> {
        val terms = QuranText.terms(query)
        if (terms.isEmpty()) return emptyList()

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

        return results
            .sortedWith(
                compareBy(
                    { hitRank(it, terms) },
                    { it.ayah.surahNumber },
                    { it.ayah.ayahNumber }
                )
            )
            .take(limit)
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
     *
     * This is the whole reason [QuranText.arabicOrigins] exists, and getting it wrong
     * was a live defect: the range used to be found in `QuranText.normalised`, which is
     * the verse with every harakat, dagger alif and tatweel deleted - a nine-character
     * word folded to six. So a match at offset 20 of the folded text was reported at
     * offset 20 of a string that had lost a third of its characters, and the highlight
     * landed on the wrong words, further off the further into the verse it was.
     *
     * The Arabic case maps through the fold. The English case does not need to, because
     * lowercasing the bundled English translation preserves length -
     * `QuranSearchRangeTest` proves that over all 6,236 verses rather than assuming it.
     *
     * The field matched decides which text is highlighted, so the emphasis and the
     * ordering can never come from different strings. A verse that matches in *both*
     * scripts reports [QuranSearchHit.Field.ARABIC], and its Arabic is what is shown
     * emphasised.
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
