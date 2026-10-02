package com.example.ui.quran.reader

import com.example.data.quran.QuranCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tatweel rule is read off the corpus, not invented.
 *
 * ### Why this test is the load-bearing one
 *
 * [Kashida.canInsertAfter] decides where elongation may go. If it is wrong in the
 * permissive direction, the reader inserts tatweels where there is no joint and the
 * Arabic visibly breaks — a worse outcome than not justifying at all, and one that would
 * not throw or crash. If it is wrong in the restrictive direction, lines simply do not
 * justify and nobody notices.
 *
 * So neither failure is loud, which means the rule has to be checked against something
 * independent of it. There is something: **Tanzil's own text carries 6,848 tatweels**,
 * placed by typesetters following the traditional rule. Every one of them is a placement
 * the rule must accept.
 *
 * That is a real check and not a tautology. The rule is expressed as two joining classes
 * written out by hand; Tanzil's placements were made by people centuries apart. If my
 * classes are wrong, some of their 6,848 will fall outside them.
 */
class KashidaEligibilityTest {

    private val tatweel = Kashida.TATWEEL

    // --- Against Tanzil's own placements --------------------------------------

    @Test
    fun `every tatweel Tanzil placed for elongation is a position the rule accepts`() {
        var checked = 0
        val rejected = ArrayList<String>()

        for (ayah in QuranCorpus.ayahs) {
            val text = ayah.textArabic
            for (i in text.indices) {
                if (text[i] != tatweel) continue
                checked++
                // The rule asks where a tatweel may be inserted *after* an index, so an
                // existing one must be legal one character earlier: the joint is before it.
                val joint = i - 1
                if (joint < 0 || !Kashida.canInsertAfter(text, joint)) {
                    // The reference, not the surrounding glyphs: a window of text is an
                    // implementation detail of this loop and pinning it here would make
                    // the test fail when the window moves, for no reason.
                    rejected += "${ayah.surahNumber}:${ayah.ayahNumber}"
                }
            }
        }

        assertTrue(
            "the corpus carries no tatweels, so this proves nothing",
            checked > 5000
        )
        assertEquals(
            "the rule rejects ${rejected.size} of Tanzil's $checked tatweels; the only " +
                "refusal it should make is the orthographic one, and any other means the " +
                "joining classes are wrong:\n" + rejected.take(12).joinToString("\n"),
            // See `the one tatweel Tanzil places that is not elongation`: the divine
            // name in 43:58, where the tatweel is spelling rather than elongation.
            listOf("43:58"),
            rejected
        )
    }

    /**
     * The one tatweel in the whole book that is spelling rather than elongation.
     *
     * Tanzil writes the divine name as `ءَأَـٰلِهَتُنَا` — with a tatweel between the alef
     * and the superscript alef. That is part of how the word is *spelled* in Uthmani
     * script, not a justification stretch: the joint there is after an alef, and an alef
     * connects to nothing on its right, so there is no joint for a joining stroke to
     * occupy. Putting one there would produce a floating stroke.
     *
     * It is the only such tatweel in 6,848, which is what makes it an *enumerated*
     * exception rather than a licence: a second one appearing means a rule changed, not
     * that the tradition is inconsistent.
     *
     * The rule refusing it is the correct behaviour, and it is worth stating rather than
     * loosening the rule to accept it — a rule that permits elongation after an alef
     * would be wrong on the other 6,847.
     */
    @Test
    fun `the one tatweel Tanzil places that is not elongation`() {
        val ayah = QuranCorpus.ayahsInSurah(43).first { it.ayahNumber == 58 }
        val text = ayah.textArabic
        val at = text.indexOf(tatweel)
        assertTrue("the fixture is wrong: 43:58 carries no tatweel", at > 0)

        assertFalse(
            "the rule now allows elongation after an alef, which would put a joining " +
                "stroke where there is no joint",
            Kashida.canInsertAfter(text, at - 1)
        )
        // And the reason is the alef specifically, not the surrounding marks.
        assertTrue(
            "the fixture is wrong: this is not the divine name",
            text.contains("أَـٰلِهَ")
        )
    }

    @Test
    fun `the corpus carries enough tatweels for this to be a real corpus`() {
        // Not an assertion about the Quran. A guard on the guard: if a corpus swap ever
        // produced text with no tatweels at all, the test above would pass vacuously.
        var total = 0
        for (ayah in QuranCorpus.ayahs) {
            total += ayah.textArabic.count { it == tatweel }
        }
        assertEquals(
            "the bundled corpus no longer carries Tanzil's tatweels",
            6848,
            total
        )
    }

    // --- The rule itself ------------------------------------------------------

    @Test
    fun `a tatweel cannot follow a letter that does not join onward`() {
        // The letters that connect only backward. Putting a joining stroke after any of
        // them leaves a stroke attached to nothing.
        for (letter in listOf('ا', 'د', 'ذ', 'ر', 'ز', 'و', 'ة', 'ى', 'ؤ', 'ء')) {
            val word = "ب${letter}ب"
            val before = word.indexOf(letter)
            assertFalse(
                "a tatweel was allowed after '$letter', which does not join onward",
                Kashida.canInsertAfter(word, before)
            )
        }
    }

    @Test
    fun `a tatweel may follow a letter that joins onward`() {
        // The empirical set from Tanzil's own text: lam, ya, nun, meem, ha, ta, sad,
        // sin, ba, ha, ain, kaf, kha and za are all things he elongated after.
        for (letter in listOf('ل', 'ي', 'ن', 'م', 'ه', 'ت', 'ص', 'س', 'ب', 'ع', 'ك', 'خ', 'ظ')) {
            val word = "${letter}بت"
            assertTrue(
                "a tatweel was refused after '$letter', which joins onward",
                Kashida.canInsertAfter(word, 0)
            )
        }
    }

    @Test
    fun `a tatweel may sit before a letter that joins backward only`() {
        // Alef, dal, thal, waw and ra connect backward and not forward - and Tanzil's
        // text puts tatweels before them constantly (`مـَا` rather than `مَـا`, which
        // would be unjoinable). This is the case that makes the rule more than a filter on
        // the left-hand letter.
        for (letter in listOf('ا', 'ذ', 'و', 'ر', 'أ')) {
            val word = "م${letter}"
            assertTrue(
                "a tatweel was refused before '$letter', which joins backward",
                Kashida.canInsertAfter(word, 0)
            )
        }
    }

    @Test
    fun `the harakat around a joint do not hide it`() {
        // This is the trap the walking logic exists for. In `مَ` the fatha sits between
        // the meem and where the tatweel would go, and a naive `text[index]` test sees the
        // fatha - which is not a letter - and refuses. Tanzil's own text is full of these.
        val withFatha = "مَبت"
        assertTrue(
            "a tatweel was refused because a fatha stood at the joint",
            Kashida.canInsertAfter(withFatha, 0)
        )
        val withShadda = "بَّت"
        assertTrue(
            "a tatweel was refused because a shadda stood at the joint",
            Kashida.canInsertAfter(withShadda, 0)
        )
        // And a tatweel already there is itself a valid neighbour.
        val alreadyElongated = "مـبت"
        assertTrue(
            "a tatweel was refused next to a tatweel Tanzil already placed",
            Kashida.canInsertAfter(alreadyElongated, 1)
        )
    }

    @Test
    fun `a tatweel is never placed at a word boundary`() {
        // A space has no joint on either side, so this is implied by the letter classes -
        // but it is the case a reader would notice most, and it deserves to be stated.
        val twoWords = "بت تلك"
        // Index 1 is the joint between ta and the space; index 2 is between the space and
        // the next ta. Index 3 is *not* one of these - it is the joint between ta and lam
        // inside "tilk", which is a perfectly good joint, and an earlier version of this
        // test asserted against it and was wrong.
        assertFalse("a tatweel was allowed between a letter and a space", Kashida.canInsertAfter(twoWords, 1))
        assertFalse("a tatweel was allowed between a space and a letter", Kashida.canInsertAfter(twoWords, 2))
        assertTrue(
            "the joint inside the second word was refused, so this proves nothing",
            Kashida.canInsertAfter(twoWords, 3)
        )
    }

    @Test
    fun `the ends of a word offer no joint`() {
        val word = "بت"
        assertFalse("a tatweel was allowed at the very start", Kashida.canInsertAfter(word, -1))
        assertFalse("a tatweel was allowed at the very end", Kashida.canInsertAfter(word, 2))
    }

    // --- Spreading ------------------------------------------------------------

    @Test
    fun `elongation is spread along the line rather than piled at one end`() {
        // A mushaf line is elongated across its length. Left-packing every tatweel into
        // the first two words leaves the rest of the line visibly short and the result
        // obviously artificial, so `spread` has to distribute.
        val line = "بتثجحخسشصضطعغفقكلمنهويئبتثجحخسشصضطعغف"
        val spots = Kashida.spread(line, 0, line.length, 5)
        assertEquals("fewer joints were found than were asked for", 5, spots.size)
        assertEquals("the joints are not in order", spots.sorted(), spots)
        assertEquals("the same joint was chosen twice", spots.distinct().size, spots.size)

        // Spread means the chosen points span the range rather than clustering.
        val span = spots.last() - spots.first()
        assertTrue(
            "five elongations were all placed in the first quarter of a 40-character line: " +
                "$spots",
            span > (line.length / 3)
        )
    }

    @Test
    fun `a line with nowhere to elongate returns nothing rather than something wrong`() {
        // One letter, or a word with no dual-joining letter in it. The fallback is the
        // word gaps, and this must not crash or invent a position.
        assertTrue(
            "a single letter offered a joint",
            Kashida.spread("ا", 0, 1, 5).isEmpty()
        )
        assertTrue(
            "an unjoinable word offered a joint",
            Kashida.spread("ااا", 0, 3, 5).isEmpty()
        )
        assertTrue(
            "a request for zero elongations produced some",
            Kashida.spread("بتثج", 0, 4, 0).isEmpty()
        )
    }

    @Test
    fun `more elongations than joints gives every joint exactly once`() {
        val line = "بتثج"
        val spots = Kashida.spread(line, 0, line.length, 99)
        val joints = Kashida.eligibleIndices(line, 0, line.length)
        assertEquals(
            "every joint was not used exactly once",
            joints.size,
            spots.size
        )
        assertEquals(joints.distinct().size, spots.distinct().size)
    }
}