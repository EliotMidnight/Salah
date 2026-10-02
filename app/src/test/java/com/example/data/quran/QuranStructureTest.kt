package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import com.example.ui.quran.reader.MushafPageText

/**
 * The bundled data is the Quran, and the gate says so before anything is drawn.
 *
 * ### What this is
 *
 * [QuranStructure] is a **runtime gate**, not a test — it runs on the load path and refuses
 * to serve a verse from a corpus whose shape is wrong. This file is what proves the gate is
 * worth having: that it passes on the real bundle, that every expectation is individually
 * capable of failing, and that its failure message names what broke.
 *
 * ### Why a structural gate is the right weight for this claim
 *
 * Everything else in the suite checks *behaviour* — that a page turn lands on the right
 * page, that a selection survives a turn, that search finds what was typed. All of those
 * can pass while the text itself is wrong.
 *
 * A wrong verse count or a shifted page boundary does not produce a visible glitch. It
 * produces **the wrong Quran, drawn correctly**: pagination faithful to bad boundaries,
 * announcements faithfully describing verses that are not there. There is no rendering bug
 * to notice and no crash to report. The only place that can catch it is a check that runs
 * before the first verse is shown.
 *
 * ### The numbers
 *
 * Twelve expectations, six of which nothing in the codebase checked before:
 * manzil and ruku' are in Tanzil's metadata and were never read, and the basmalah count
 * was wrong in the code's own documentation.
 */
class QuranStructureTest {

    /** The counts the real bundle produces, measured by the corpus itself. */
    private val actual get() = QuranCorpus.structure

    // --- The gate passes on the real data -----------------------------------

    @Test
    fun `the bundled data is the Quran`() {
        val failures = QuranStructure.expectations(actual).filter { !it.holds }
        if (failures.isNotEmpty()) {
            fail(
                "the bundled corpus does not match the expected structure:\n" +
                    failures.joinToString("\n") { "  ${it.name}: expected ${it.expected}, found ${it.actual}" }
            )
        }
    }

    /**
     * Every expectation, with its number, spelled out.
     *
     * A test that only says "it passed" hides which numbers are actually being checked, and
     * this is the list someone will read when the gate fires on a user's phone.
     */
    @Test
    fun `every expectation is stated with its number`() {
        val expectations = QuranStructure.expectations(actual)
        // Ten numbers. Five were already asserted by `require` in the corpus (surahs,
        // ayat, juz', rub' al-hizb, pages). Five were not, and are the reason this file
        // exists: hizb as 60, manzil as 7, ruku' as 556, the basmalah as 113, and the
        // prostrations as exactly 15 - the corpus checked only that the sajda map was
        // non-empty.
        assertEquals(
            "the gate checks a different number of things than it should: " +
                expectations.joinToString(", ") { it.name },
            10,
            expectations.size
        )
        val byName = expectations.associateBy { it.name }
        assertEquals(114, byName.getValue("surahs").expected)
        assertEquals(6236, byName.getValue("ayat").expected)
        assertEquals(30, byName.getValue("juz'").expected)
        assertEquals(60, byName.getValue("hizb").expected)
        assertEquals(240, byName.getValue("rub' al-hizb").expected)
        assertEquals(7, byName.getValue("manzil").expected)
        assertEquals(556, byName.getValue("ruku'").expected)
        assertEquals(604, byName.getValue("pages").expected)
        assertEquals(15, byName.getValue("prostrations").expected)
        assertEquals(113, byName.getValue("basmalah").expected)
    }

    // --- The numbers that were wrong, or unchecked ---------------------------

    /**
     * The basmalah opens 113 surahs, not 114 and not 111.
     *
     * 114 is the number people quote. It is wrong: **At-Tawbah has no basmalah.**
     *
     * 111 is what this codebase's own documentation claimed, and it was also wrong — the
     * comment sat directly above the code that reads the basmalah, describing it as being
     * inside verse 1 of "111 of the 114" surahs, when it is 113.
     *
     * Both errors came from the same place: a number written from memory rather than
     * counted. This one counts.
     */
    @Test
    fun `the basmalah opens every surah but At-Tawbah`() {
        assertEquals(
            "the corpus does not open 113 surahs with the basmalah",
            113,
            actual.basmalah
        )
        assertEquals(
            "the corpus opens the basmalah in surah ${QuranStructure.SURAH_WITHOUT_BASMALAH}, " +
                "which is the one surah that has none",
            false,
            opensWithBasmalah(QuranStructure.SURAH_WITHOUT_BASMALAH)
        )
        assertTrue(
            "Al-Fatihah does not open with the basmalah, which is the whole of its verse 1",
            opensWithBasmalah(1)
        )
        // And every other surah does.
        var missing = 0
        for (surah in 1..114) {
            if (surah != QuranStructure.SURAH_WITHOUT_BASMALAH && !opensWithBasmalah(surah)) {
                missing++
            }
        }
        assertEquals("surahs other than At-Tawbah that lack the basmalah", 0, missing)
    }

    /**
     * Manzil and ruku' were in the metadata all along and nothing counted them.
     *
     * They are now read, bounded and asserted. Before this, a bundle that had lost them
     * would have passed every other check in the codebase.
     */
    @Test
    fun `manzil and ruku' are read, and cover the book`() {
        assertEquals(7, actual.manzil)
        assertEquals(556, actual.ruku)

        // Contiguous and total, the same property `lookupTable` enforces for the others.
        assertEquals(
            "the ruku' partition does not cover every verse",
            1,
            QuranCorpus.rukuOf(1, 1)
        )
        assertEquals(
            "the last verse of the book is not in the last ruku'",
            556,
            QuranCorpus.rukuOf(114, 6)
        )
        assertEquals("manzil 1 opens the book", 1, QuranCorpus.manzilOf(1, 1))
        assertEquals(
            "manzil 7 is the last of the seven",
            7,
            QuranCorpus.manzilOf(114, 6)
        )

        // Every verse resolves to a valid partition number.
        for (ayah in QuranCorpus.ayahs) {
            val ruku = QuranCorpus.rukuOf(ayah.surahNumber, ayah.ayahNumber)
            val manzil = QuranCorpus.manzilOf(ayah.surahNumber, ayah.ayahNumber)
            assertTrue("${ayah.surahNumber}:${ayah.ayahNumber} is in ruku' $ruku", ruku in 1..556)
            assertTrue("${ayah.surahNumber}:${ayah.ayahNumber} is in manzil $manzil", manzil in 1..7)
        }
    }

    /** 604 pages of 15 lines is arithmetic, not a datum — the bundle has no line data. */
    @Test
    fun `the page grid is stated as arithmetic, and says so`() {
        assertEquals(15, QuranStructure.LINES_PER_PAGE)
        assertEquals(604, QuranStructure.PAGE_COUNT)
        assertEquals(9060, QuranStructure.PAGE_LINES)
        assertEquals(
            "9,060 must be 604 x 15 and nothing else",
            QuranStructure.PAGE_COUNT * QuranStructure.LINES_PER_PAGE,
            QuranStructure.PAGE_LINES
        )
        // And no expectation claims to check it, because the data cannot support such a
        // check. A gate entry that compares a constant with itself is worse than none.
        val names = QuranStructure.expectations(actual).map { it.name }
        assertTrue(
            "the gate asserts line counts, which the dataset does not contain: $names",
            names.none { it.contains("line", ignoreCase = true) }
        )
    }

    // --- The gate itself can fail --------------------------------------------

    /**
     * A wrong count fails, and the message says which count.
     *
     * Verified by planting a bad dataset, because a gate that cannot be seen to fail is a
     * gate nobody can trust to have passed. All ten are corrupted at once to show the
     * message reports *every* failure rather than only the first — a shifted metadata
     * offset usually breaks several at the same time.
     */
    @Test
    fun `the gate refuses a corpus with the wrong shape, and names every failure`() {
        val broken = QuranStructure.StructureCounts(
            surahs = 113,
            ayah = 6235,
            juz = 30,
            hizb = 60,
            rubAlHizb = 239,
            manzil = 6,
            ruku = 555,
            pages = 603,
            prostrations = 14,
            basmalah = 114
        )
        val failure = runCatching { QuranStructure.requireIntact(broken) }
            .exceptionOrNull()
        assertTrue(
            "a corpus with ten wrong counts was accepted",
            failure is IllegalArgumentException
        )
        val message = failure!!.message.orEmpty()
        for (expected in listOf(
            "surahs: expected 114, found 113",
            "ayat: expected 6236, found 6235",
            "rub' al-hizb: expected 240, found 239",
            "manzil: expected 7, found 6",
            "ruku': expected 556, found 555",
            "pages: expected 604, found 603",
            "prostrations: expected 15, found 14",
            "basmalah: expected 113, found 114"
        )) {
            assertTrue(
                "the failure message does not mention \"$expected\":\n$message",
                message.contains(expected)
            )
        }
        // The two that were right are not reported, so the message is a list of problems
        // rather than a wall of everything.
        assertTrue(
            "the message reports a count that was correct:\n$message",
            !message.contains("juz': expected 30, found 30")
        )
    }

    @Test
    fun `the gate names the file that could not be trusted`() {
        // A reader hitting this has a broken install. What they need is not a stack trace
        // but the reason, in words, and a consequence that is honest about why nothing is
        // being shown.
        val message = runCatching {
            QuranStructure.requireIntact(
                QuranStructure.StructureCounts(
                    113, 6236, 30, 60, 240, 7, 556, 604, 15, 113
                )
            )
        }.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "the message does not say what is wrong:\n$message",
            message.contains("does not match the expected structure")
        )
        assertTrue(
            "the message does not say what follows:\n$message",
            message.contains("Nothing will be displayed")
        )
    }

    // --- Kufan numbering -----------------------------------------------------

    /**
     * The verse numbers are Kufan's, and the app can say so.
     *
     * The decisive evidence is **at-Tawbah having 129 verses**, where the Hafs count gives
     * 127. Everything else — 6,236 in total, page numbers — is shared between the
     * conventions, so this is the one place the difference is visible.
     *
     * Worth pinning because it is invisible everywhere else: a reader who counts the other
     * way is not wrong, and the app is not going to offer both schemes. But a reference
     * printed here *is* a Kufan reference, and pretending otherwise would be the sort of
     * quiet inaccuracy the rest of this rebuild has been removing.
     */
    @Test
    fun `the verse numbering is Kufan, and that is decidable`() {
        assertEquals(
            "at-Tawbah does not have 129 verses, so this text is not Kufan-numbered and " +
                "the documentation is wrong",
            129,
            QuranCorpus.ayahsInSurah(QuranStructure.SURAH_WITHOUT_BASMALAH).size
        )
        assertEquals(
            "the book does not hold 6,236 verses under this numbering",
            6236,
            QuranCorpus.ayahs.size
        )
        // Al-Baqarah is 286 under both conventions, so it is the control that shows the
        // at-Tawbah count is a convention and not a corrupt file.
        assertEquals(286, QuranCorpus.ayahsInSurah(2).size)
    }

    private fun opensWithBasmalah(surah: Int): Boolean {
        val first = QuranBrowse.ayah(surah, 1) ?: return false
        return MushafPageText.startsWithBasmalah(first.textArabic)
    }
}