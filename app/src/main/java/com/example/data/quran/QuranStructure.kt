package com.example.data.quran

/**
 * The shape of the Quran, checked against the bundled data before any of it is shown.
 *
 * ### Why this is a gate and not a test
 *
 * Every other check in the codebase lives in a test, and tests do not run on a user's
 * phone. This one runs on the load path: [QuranCorpus] refuses to serve a verse until the
 * dataset has been proved to be the dataset it claims to be.
 *
 * That is the right weight for this particular claim. A wrong verse count or a shifted
 * page boundary does not produce a visible glitch — it produces **the wrong Quran, drawn
 * correctly**. Pagination, verse selection and every announcement would be faithfully
 * reporting on text that is not the text. There is no rendering bug to notice and no
 * crash to report, so a gate that fails loudly is the only thing that catches it.
 *
 * ### Why the numbers are named rather than inlined
 *
 * They are the traditional structure of the book — 114 surahs, 30 juz', 240 rub' al-hizb,
 * 604 pages of a 15-line mushaf — and they are also *assertions about this bundle*. Both
 * meanings matter, and they are not the same claim: the first is what the Quran is, the
 * second is what Tanzil v1.0 ships. Naming them once, here, keeps a reader from having to
 * guess which is being checked.
 *
 * ### Two numbers that are commonly quoted differently
 *
 * - **556 ruku', not 558.** 558 is a frequently cited figure; Tanzil's own metadata
 *   enumerates **556** `<ruku>` elements. This bundle's answer is 556, and the gate checks
 *   556, because the gate is about *this data* matching *its own declared structure* — not
 *   about agreeing with a variant count.
 * - **113 basmalahs, not 114.** Every surah but At-Tawbah (9) opens with one. Al-Fatihah's
 *   is the whole of verse 1 rather than sitting inside it, which is why a printed mushaf
 *   shows it once either way.
 *
 * ### What this cannot check, and why that is not a gap
 *
 * **15 lines per page and 9,060 page lines.** The bundle has no line-level information at
 * all — Tanzil's metadata gives each page a *starting verse* and nothing more. There is no
 * `line` attribute anywhere in the file. So a line count is not a property of the data and
 * cannot be validated against it.
 *
 * It is a property of the *rendering*: the reader lays a page's text onto a 15-line grid,
 * the way a printed mushaf is set, and 604 x 15 is the arithmetic of the grid rather than
 * a datum to compare. Where a page cannot hold fifteen lines — Al-Fatihah's seven verses,
 * or the tail of Al-Baqarah's first five — the printed mushaf gives it fewer and the
 * reader does the same. That is checked at the rendering layer, not here, and pretending
 * otherwise would be a check that passes because it cannot fail.
 */
object QuranStructure {

    // --- Surahs and verses ---------------------------------------------------

    /** The book has 114 surahs. */
    const val SURA_COUNT = 114

    /**
     * The book has 6,236 verses.
     *
     * Under the **Kufan verse-numbering convention**, which is what Tanzil's `uthmani`
     * download uses and what this bundle carries. The convention is not cosmetic: it is
     * the reason at-Tawbah has **129** verses here, where the Hafs count gives 127. A
     * reader who counts differently is not wrong, and the app does not offer the other
     * scheme — but a verse *reference* printed by this app is a Kufan reference, and the
     * page numbers beside it are Kufan page numbers.
     */
    const val AYAH_COUNT = 6236

    // --- The divisions -------------------------------------------------------

    /** The book has 30 juz'. */
    const val JUZ_COUNT = 30

    /** Each juz' is two hizb, so the book has 60. Derived, not read from a tag. */
    const val HIZB_COUNT = 60

    /** Each hizb is four rub' al-hizb, so the book has 240. */
    const val RUB_AL_HIZB_COUNT = 240

    /** The book has 7 manzil — Tanzil's `<manzil>`, which the corpus does not otherwise read. */
    const val MANZIL_COUNT = 7

    /**
     * The book has **556** ruku'.
     *
     * Tanzil's own count, and what this bundle carries. 558 is also in circulation; the
     * difference is a division convention, not an error in either. This gate asserts what
     * the data says, because a gate that disagrees with its own bundle is worse than no
     * gate.
     */
    const val RUKU_COUNT = 556

    // --- The mushaf ----------------------------------------------------------

    /** The canonical Madani mushaf is 604 pages. */
    const val PAGE_COUNT = 604

    /**
     * A mushaf page is 15 lines.
     *
     * A *rendering* constant, not a dataset one — the bundle carries no line information.
     * Pages holding the start of a surah have fewer, as they do in print.
     */
    const val LINES_PER_PAGE = 15

    /** 604 pages of 15 lines. Arithmetic on the two above; see the note on line data. */
    const val PAGE_LINES = PAGE_COUNT * LINES_PER_PAGE

    /** Fifteen verses are followed by a prostration: 14 obligatory, 1 recommended. */
    const val SAJDA_COUNT = 15

    /**
     * The basmalah opens every surah but At-Tawbah.
     *
     * Not 114: At-Tawbah has none. In Al-Fatihah it is the whole of verse 1 rather than
     * sitting inside it.
     */
    const val BASMALAH_COUNT = 113

    /** The surah that has no basmalah. */
    const val SURAH_WITHOUT_BASMALAH = 9

    // --- The gate ------------------------------------------------------------

    /**
     * One expectation, as a name and a number, so a failure says which one broke.
     *
     * A bare `require(count == 604)` that fails tells a reader only that the corpus is
     * wrong; naming each one lets the message say *"pages: expected 604, found 603"* —
     * which is the difference between a bug report and a diagnosis.
     */
    data class Expectation(val name: String, val expected: Int, val actual: Int) {
        val holds: Boolean get() = expected == actual
    }

    /**
     * Every structural expectation, evaluated.
     *
     * Returns the failures rather than throwing, so a caller can report *all* of them at
     * once — a shifted metadata offset usually breaks several counts together, and one at
     * a time means five successive launches to find one problem.
     */
    fun expectations(counts: StructureCounts): List<Expectation> = listOf(
        Expectation("surahs", SURA_COUNT, counts.surahs),
        Expectation("ayat", AYAH_COUNT, counts.ayah),
        Expectation("juz'", JUZ_COUNT, counts.juz),
        Expectation("hizb", HIZB_COUNT, counts.hizb),
        Expectation("rub' al-hizb", RUB_AL_HIZB_COUNT, counts.rubAlHizb),
        Expectation("manzil", MANZIL_COUNT, counts.manzil),
        Expectation("ruku'", RUKU_COUNT, counts.ruku),
        Expectation("pages", PAGE_COUNT, counts.pages),
        Expectation("prostrations", SAJDA_COUNT, counts.prostrations),
        Expectation("basmalah", BASMALAH_COUNT, counts.basmalah)
    )

    /** What the bundle turned out to contain. */
    data class StructureCounts(
        val surahs: Int,
        val ayah: Int,
        val juz: Int,
        val hizb: Int,
        val rubAlHizb: Int,
        val manzil: Int,
        val ruku: Int,
        val pages: Int,
        val prostrations: Int,
        val basmalah: Int
    )

    /**
     * Throws unless the dataset is the Quran.
     *
     * The message is the whole point. A reader who hits this has a broken install, and
     * "the bundled Quran data does not match the expected structure" with the failing
     * counts is what turns an unexplained blank reader into a report someone can act on.
     */
    fun requireIntact(counts: StructureCounts) {
        val failures = expectations(counts).filter { !it.holds }
        require(failures.isEmpty()) {
            "The bundled Quran data does not match the expected structure:\n" +
                failures.joinToString("\n") { "  ${it.name}: expected ${it.expected}, found ${it.actual}" } +
                "\n\nNothing will be displayed, because text drawn from a corpus with the " +
                "wrong shape is the wrong Quran drawn correctly."
        }
    }
}