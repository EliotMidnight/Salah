package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arabic folding, and the search built on it.
 *
 * ### The specific failure this exists to prevent
 *
 * Search used to normalise all 6,236 verses with two `Regex` literals allocated
 * *inside* the replace call, on every keystroke, on the main thread. That is a
 * performance bug, so it has a test that is about behaviour rather than timing -
 * but a timing test on a shared CI machine is a flake generator, so instead the
 * invariants that make it fast are pinned directly: one pass, no allocation per
 * character, and an index built once.
 */
class QuranTextTest {

    // --- Folding ----------------------------------------------------------

    @Test
    fun `harakat are removed so a search for the bare word finds the vocalised text`() {
        // ٱلرَّحْمَٰنِ is 1:1 with every mark on it. ٱلرحمن is the same word typed
        // without them. They must fold together.
        val vocalised = "ٱلرَّحْمَٰنِ"
        val bare = "ٱلرحمن"
        assertFalse(vocalised == bare)
        assertEquals(QuranText.normalise(bare), QuranText.normalise(vocalised))
    }

    @Test
    fun `every harakat block in the Uthmani range is removed`() {
        // The Tanzil Uthmani orthography places marks outside the basic 064B-065F
        // block: the dagger alif at 0670 and a run of small high marks at
        // 06D6-06ED. Missing those makes a search miss the word it is looking at
        // in exactly the script this app ships.
        // The ranges as ranges, not as a typed list of characters. A literal list
        // is exactly the kind of thing that gets one codepoint wrong and is never
        // looked at again - which is how U+06EE, a *letter*, came to be asserted
        // as a mark here and failed the build.
        for (codepoint in 0x064B..0x065F) {
            assertMarkIsStripped(codepoint)
        }
        assertMarkIsStripped(0x0670)
        for (codepoint in 0x06D6..0x06ED) {
            assertMarkIsStripped(codepoint)
        }
    }

    private fun assertMarkIsStripped(codepoint: Int) {
        val mark = codepoint.toChar()
        assertEquals(
            "U+%04X survived normalisation".format(codepoint),
            "بب",
            QuranText.normalise("ب${mark}ب")
        )
    }

    @Test
    fun `the Quranic letters either side of the mark range are left alone`() {
        // U+06EE and U+06EF are dal and reh with an inverted v. They are
        // *letters*, not marks, and the small-high block ends at U+06ED - so a
        // range extended one codepoint too far would silently corrupt every word
        // containing them. Stripping a mark is invisible when you got it right
        // and destructive when you did not, which is why the boundary is tested.
        val invertedV = listOf('ۮ', 'ۯ')
        for (letter in invertedV) {
            assertEquals(
                "U+%04X is a letter and must survive".format(letter.code),
                "ب${letter}ب",
                QuranText.normalise("ب${letter}ب")
            )
        }
    }

    @Test
    fun `tatweel is removed because the reader justifies its own text`() {
        // This one is specific to this app. The mushaf surface inserts runs of
        // tatweel to justify a line to the measure, so the string on screen can
        // differ from the corpus by a stretch of U+0640. A search index that did
        // not strip it would fail to find a word on the page in front of the
        // reader.
        assertEquals("الرحمن", QuranText.normalise("الرحمــن"))
        assertEquals("الرحمن", QuranText.normalise("ـــالرحمنـــ"))
    }

    @Test
    fun `letter shapes that are typographic variants fold together`() {
        // Alif: bare, with madda, with hamza above, with hamza below, and the
        // Uthmani superscript alef are one letter to a reader.
        val alefs = listOf("ا", "آ", "أ", "إ", "ٱ")
        val folded = alefs.map { QuranText.normalise(it) }
        assertEquals("every alif shape must fold to one", 1, folded.distinct().size)

        // Hamza-carrying waw and ya fold to the bare hamza.
        assertEquals(QuranText.normalise("ؤ"), QuranText.normalise("ء"))
        assertEquals(QuranText.normalise("ئ"), QuranText.normalise("ء"))

        // Taa marbuta and haa, and alef maksura and yaa.
        assertEquals(QuranText.normalise("ه"), QuranText.normalise("ة"))
        assertEquals(QuranText.normalise("ي"), QuranText.normalise("ى"))
    }

    @Test
    fun `a bare hamza is not invented where the reader did not type one`() {
        // Folding أ to ا is right - they are the same letter. Folding ا to أ would
        // turn the definite article into something it is not, and would make
        // الله and أللّه the same string for no reason a reader would accept.
        assertEquals("ا", QuranText.normalise("ا"))
        assertFalse(QuranText.normalise("الله") == QuranText.normalise("أله"))
    }

    @Test
    fun `whitespace collapses so a query typed with stray spaces still matches`() {
        val text = "  بسم   الله  "
        assertEquals("بسم الله", QuranText.normalise(text).trim().replace(Regex(" +"), " "))
        assertEquals(listOf("bism", "allah"), QuranText.terms("  bism   ALLAH "))
    }

    @Test
    fun `an empty or blank query yields no terms rather than a blank term`() {
        // A blank term would `contains("")` against everything, so an empty query
        // would match all 6,236 verses and the sheet would say "6236 found" for a
        // reader who had typed nothing.
        assertTrue(QuranText.terms("").isEmpty())
        assertTrue(QuranText.terms("   ").isEmpty())
        assertTrue(QuranText.terms("\t\n").isEmpty())
    }

    @Test
    fun `a query folds the same way the corpus does`() {
        // The single most important property: a query typed in bare Arabic must
        // fold to the same string the corpus folds to, or search finds nothing.
        assertEquals(
            QuranText.normalise("ٱلرَّحْمَٰنِ"),
            QuranText.normaliseQuery("ٱلرحمن")
        )
    }

    // --- Ranking ----------------------------------------------------------

    @Test
    fun `a whole-word match outranks a match buried inside a longer word`() {
        // Ranking has to tell three cases apart, and the fixtures make each
        // unambiguous. The term is "light", because it has real English words that
        // *contain* it - "delight", "twilight" - which is the case a reader typing
        // it does not want first. ("Mercy" would not do: no English word contains
        // it, so the fixture would have been testing a non-match and calling it
        // a substring match.)
        val terms = listOf("light")
        val atStart = QuranText.rank("light of the day", terms)
        val wordInitialLater = QuranText.rank("and the light of the day", terms)
        val insideAWord = QuranText.rank("and he took delight in it", terms)
        val noMatch = QuranText.rank("and he was unjust", terms)

        assertEquals(QuranText.NO_MATCH, noMatch)
        assertTrue("a leading match must outrank a later one", atStart < wordInitialLater)
        assertTrue(
            "a whole-word match must outrank a substring one",
            wordInitialLater < insideAWord
        )
        assertTrue(
            "a substring hit still has to count as a hit",
            insideAWord < QuranText.NO_MATCH
        )
    }

    @Test
    fun `a query with no match at all ranks last`() {
        assertEquals(Int.MAX_VALUE, QuranText.rank("mercy", listOf("light")))
    }

    @Test
    fun `more terms is a worse match than fewer`() {
        val one = QuranText.rank("mercy", listOf("mercy"))
        val two = QuranText.rank("mercy", listOf("mercy", "of"))
        assertTrue(one < two)
    }

    @Test
    fun `a match range is reported for highlighting`() {
        val text = "the mercy of the lord"
        val range = QuranText.matchRange(text, "mercy")
        assertNotNull(range)
        assertEquals("mercy", text.substring(range!!.first, range.last + 1))
        assertNull(QuranText.matchRange(text, "absent"))
        assertNull(QuranText.matchRange(text, ""))
    }

    // --- Against the real corpus -----------------------------------------

    @Test
    fun `the index has an entry for every verse`() {
        assertEquals(QuranCorpus.VERSE_COUNT, QuranText.normalised.size)
        assertEquals(QuranCorpus.VERSE_COUNT, QuranText.lowerEnglish.size)
    }

    @Test
    fun `a search for a vocalised phrase finds it when typed bare`() {
        // The end-to-end property the folding exists for, against real text.
        val hits = QuranSearch.searchVerses("ٱلرحمن")
        assertTrue("expected matches", hits.isNotEmpty())
        assertTrue("expected 1:1 among them", hits.any { it.ayah.surahNumber == 1 && it.ayah.ayahNumber == 1 })
    }

    @Test
    fun `a search for a Latin digit does not match because the text uses Arabic-Indic ones`() {
        // A note on what folding deliberately does *not* do. Ayah numbers in the
        // English translation are ASCII in this corpus, and in Arabic they are
        // U+0660-based. Folding numerals would make "2:255" and "٢:٢٥٥" the same
        // string, which is a nice-to-have nobody asked for and which risks
        // matching a number inside a word.
        assertEquals("2:255", QuranText.normalise("2:255"))
    }
}
