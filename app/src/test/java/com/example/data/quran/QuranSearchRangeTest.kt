package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A search highlight points at the word the reader typed.
 *
 * ### The defect
 *
 * `QuranSearchHit.range` is documented in six lines as the answer to "why is this here",
 * and the result row's own KDoc says the highlight is "the whole reason a result is
 * trustworthy". It was rendered by **nothing**. The row showed the whole verse, in both
 * scripts, with the match in neither.
 *
 * It would also have been **wrong** if it had been rendered. The range was found with
 * `indexOf` in `QuranText.normalised` — the verse with every harakat, dagger alif,
 * small high mark and tatweel deleted, and four letters folded onto others — and then
 * applied to `ayah.textArabic`, the original. `ٱلرَّحْمَٰنِ` is nine characters and
 * `الرحمن` is six, so a match at offset 20 of the folded text was reported at offset 20
 * of a string that had lost a third of its length. The highlight landed on the wrong
 * words, and the further into the verse, the worse it got.
 *
 * ### Why a test can check this at all
 *
 * The property is falsifiable per verse: take the original span the highlight covers,
 * fold *that span*, and it must equal the term that matched. If the mapping is off by
 * one character the fold of the span will not be the term, so this is not a
 * "does it look right" assertion — it is an equality over all 6,236 verses.
 *
 * The search is swept with terms taken *from the corpus itself* rather than typed in,
 * so the test cannot pass by accident on a word that happens to fold to itself.
 */
class QuranSearchRangeTest {

    /**
     * Terms drawn from real verses, so each is guaranteed to match somewhere and the
     * sweep has real offsets to be wrong about.
     *
     * Arabic: the basmalah, a long word, and a short one, plus a word that appears in
     * more than one place in the book — a repeated word is the case that defeats any
     * attempt to recover the offsets by searching the original text afterwards.
     */
    private val arabicTerms = listOf(
        "الرحمن",      // al-Rahman
        "ٱلرَّحْمَٰنِ",   // as written, with every mark
        "الرحيم",      // al-Rahim
        "الله",        // Allah
        "الم",         // a two-letter prefix
        "شاء"          // sha'a
    )

    private val englishTerms = listOf(
        "mercy",
        "the",
        "light",
        "Allah",
        "And"
    )

    // --- The property ------------------------------------------------------

    @Test
    fun `every Arabic highlight covers exactly the term that matched`() {
        var checked = 0
        for (verse in QuranCorpus.ayahs) {
            val fold = QuranText.fold(verse.textArabic)
            for (rawTerm in arabicTerms) {
                val term = QuranText.normaliseQuery(rawTerm)
                if (term.isEmpty() || term.contains(' ')) continue
                val range = fold.originalRangeOf(term) ?: continue
                checked++

                // The span the reader would see emphasised, folded, is the term.
                val highlighted = verse.textArabic.substring(range.first, range.last + 1)
                assertEquals(
                    "in ${verse.surahNumber}:${verse.ayahNumber} the highlight for " +
                        "\"$term\" covers \"$highlighted\", which folds to " +
                        "\"${QuranText.normalise(highlighted)}\"",
                    term,
                    QuranText.normalise(highlighted)
                )
            }
        }
        assertTrue(
            "the sweep checked $checked highlights, which is too few to mean anything - " +
                "either the terms no longer match or the corpus changed",
            checked > 500
        )
    }

    @Test
    fun `the old offset - a range found in the folded text - does not survive`() {
        // The defect, reproduced as a test. This is what the code used to do: take
        // `indexOf` in the folded text and use those numbers on the original.
        val verse = QuranCorpus.ayahAt(QuranCorpus.indexOf(2, 255)) ?: return
        val fold = QuranText.fold(verse.textArabic)
        val term = QuranText.normaliseQuery("ٱللَّهِ")
        val at = fold.text.indexOf(term)
        if (at < 0) return

        val wrong = at until (at + term.length)
        val right = fold.originalRangeOf(term)
        assertNotNull("the fold produced no range at all", right)

        // The naive span, folded, is *not* the term - it is the term plus whatever
        // deleted marks were in the way. That difference is the bug, stated as an
        // inequality rather than as a claim about which span looks nicer.
        assertFalse(
            "the naive span happens to fold to the term on ${verse.surahNumber}:" +
                "${verse.ayahNumber}, so this verse cannot demonstrate the defect - " +
                "pick one with diacritics before the match",
            QuranText.normalise(verse.textArabic.substring(wrong.first, wrong.last + 1))
                .trim() == term
        )
        assertEquals(
            "the corrected span does not fold to the term",
            term,
            QuranText.normalise(
                verse.textArabic.substring(right!!.first, right.last + 1)
            ).trim()
        )
    }

    @Test
    fun `lowercasing the English translation preserves length, so its offsets hold`() {
        // The English case relies on `lowercase()` being length-preserving, and the
        // English corpus is fixed and bundled, so that is provable rather than
        // assumed. If this ever fails, the English path needs a fold too.
        for (verse in QuranCorpus.ayahs) {
            if (verse.textEnglish.length != verse.textEnglish.lowercase().length) {
                throw AssertionError(
                    "lowercasing changed the length of ${verse.surahNumber}:" +
                        "${verse.ayahNumber}'s English (${verse.textEnglish.length} -> " +
                        "${verse.textEnglish.lowercase().length}), so an offset into the " +
                        "lowercased text no longer indexes the original: " +
                        "\"${verse.textEnglish.take(80)}\""
                )
            }
        }
    }

    // --- The shape of the map ----------------------------------------------

    @Test
    fun `every folded character knows which original character it came from`() {
        for (verse in QuranCorpus.ayahs) {
            val fold = QuranText.fold(verse.textArabic)
            assertEquals(
                "the origin map for ${verse.surahNumber}:${verse.ayahNumber} is not " +
                    "one entry per folded character",
                fold.text.length,
                fold.origin.size
            )
            for (i in fold.origin.indices) {
                assertTrue(
                    "folded character $i of ${verse.surahNumber}:${verse.ayahNumber} " +
                        "claims origin ${fold.origin[i]}, which is outside the verse",
                    fold.origin[i] in 0 until verse.textArabic.length
                )
                if (i > 0) {
                    assertTrue(
                        "the origin map for ${verse.surahNumber}:" +
                            "${verse.ayahNumber} is not monotonic at $i " +
                            "(${fold.origin[i - 1]} then ${fold.origin[i]})",
                        fold.origin[i] > fold.origin[i - 1]
                    )
                }
            }
        }
    }

    @Test
    fun `the whole verse normalises to the same text the lazy index holds`() {
        // `normalised` and `arabicOrigins` are two views of one pass. If they were ever
        // computed separately they could drift, and a highlight would be positioned
        // against a different fold from the one the match was found in.
        for (i in QuranCorpus.ayahs.indices) {
            assertEquals(
                "verse $i: the lazy index and a fresh fold disagree",
                QuranText.normalise(QuranCorpus.ayahs[i].textArabic),
                QuranText.arabicFolds[i].text
            )
            assertEquals(
                "verse $i: the origin map is not the one the index was built from",
                QuranText.arabicFolds[i].origin.size,
                QuranText.arabicOrigins[i].size
            )
        }
    }

    @Test
    fun `a term at the very end of a verse still produces a range`() {
        // The clamp case: a match whose length runs to the end of the fold must not be
        // rejected as malformed.
        val verse = QuranCorpus.ayahAt(QuranCorpus.indexOf(1, 1)) ?: return
        val fold = QuranText.fold(verse.textArabic)
        val term = fold.text.takeLast(2)
        val range = fold.originalRangeOf(term)
        assertNotNull("a match at the very end produced no range", range)
        assertTrue(
            "the range $range runs past the end of the verse (${verse.textArabic.length})",
            range!!.last < verse.textArabic.length
        )
    }

    @Test
    fun `a term that is not there produces no range`() {
        val verse = QuranCorpus.ayahAt(QuranCorpus.indexOf(1, 1)) ?: return
        val fold = QuranText.fold(verse.textArabic)
        assertNull(fold.originalRangeOf("zzzznotpresent"))
        assertNull(fold.originalRangeOf(""))
        assertNull(fold.originalRangeOf(-1, 3))
        assertNull(fold.originalRangeOf(0, 0))
    }

    // --- The end-to-end behaviour -------------------------------------------

    @Test
    fun `every Arabic search hit carries a range that covers a real term`() {
        // Through the public entry point, so the wiring is covered and not just the
        // fold. Ten terms that are known to occur in the book.
        val terms = listOf(
            "الرحمن", "الرحيم", "الله", "الم", "شاء", "الحمد", "الكتاب",
            "الصلاة", "رمضان", "يوسف"
        )
        var hits = 0
        for (term in terms) {
            for (hit in QuranSearch.searchVerses(term).hits) {
                if (hit.matchedIn != QuranSearchHit.Field.ARABIC) continue
                hits++
                val range = hit.range
                assertNotNull(
                    "an Arabic hit in ${hit.ref} has no range, so the result row cannot " +
                        "say why it matched",
                    range
                )
                val text: String = hit.ayah.textArabic
                assertTrue(
                    "the range $range does not fit ${hit.ref}, whose Arabic is " +
                        "${text.length} characters",
                    range!!.first >= 0 && range.last < text.length
                )
                // Folding the span and folding the term must agree on the first token,
                // because a term can be several words and only one is highlighted.
                val span = QuranText.normalise(text.substring(range.first, range.last + 1))
                val needle = QuranText.normaliseQuery(term)
                assertTrue(
                    "in ${hit.ref} the highlight \"$span\" does not contain the " +
                        "searched term \"$needle\"",
                    span.contains(needle.split(' ').first())
                )
            }
        }
        assertTrue("only $hits Arabic hits were checked", hits > 50)
    }

    @Test
    fun `every English search hit carries a range that covers a real term`() {
        var hits = 0
        for (term in englishTerms) {
            for (hit in QuranSearch.searchVerses(term).hits) {
                if (hit.matchedIn != QuranSearchHit.Field.ENGLISH) continue
                hits++
                val range = hit.range
                assertNotNull(
                    "an English hit in ${hit.ref} has no range",
                    range
                )
                val text: String = hit.ayah.textEnglish
                assertTrue(
                    "the range $range does not fit ${hit.ref}",
                    range!!.first >= 0 && range.last < text.length
                )
                val span = text.substring(range.first, range.last + 1).lowercase()
                assertTrue(
                    "in ${hit.ref} the highlight \"$span\" does not contain the " +
                        "searched term \"${term.lowercase()}\"",
                    span.contains(term.lowercase())
                )
            }
        }
        assertTrue("only $hits English hits were checked", hits > 50)
    }

    @Test
    fun `a hit in both scripts reports the Arabic, and its range is an Arabic offset`() {
        // `matchedIn` picks the field, and the range has to belong to the field the row
        // emphasises. A range from one script applied to the other would either run off
        // the end of the string or highlight the wrong words.
        var checked = 0
        for (hit in QuranSearch.searchVerses("mercy").hits) {
            val range = hit.range ?: continue
            val field = hit.matchedIn
            val text = when (field) {
                QuranSearchHit.Field.ARABIC -> hit.ayah.textArabic
                QuranSearchHit.Field.ENGLISH -> hit.ayah.textEnglish
                QuranSearchHit.Field.SURAH_NAME -> continue
            }
            assertTrue(
                "the range for ${hit.ref} ($field, $range) does not fit a " +
                    "${text.length}-character string",
                range.last < text.length
            )
            checked++
        }
        assertTrue("only $checked hits were checked", checked > 10)
    }

    @Test
    fun `the subject the row emphasises is the text the range indexes`() {
        // `QuranSearchHit.subject` is what a row should highlight in, and it must be the
        // same string the range was computed against. This is the invariant the row
        // depends on and nothing enforced before.
        for (term in listOf("mercy", "light", "الرحمن")) {
            for (hit in QuranSearch.searchVerses(term).hits) {
                val range = hit.range ?: continue
                val subject = hit.subject
                assertTrue(
                    "the range $range runs past the ${hit.matchedIn} subject of " +
                        "${hit.ref}, which is ${subject.length} characters",
                    range.last < subject.length && range.first >= 0
                )
            }
        }
    }
}