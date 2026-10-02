package com.example.data.quran

/**
 * The shape of the Quran, checked against the bundled data before any of it is shown.
 *
 * ### Why this is a gate and not a test
 *
 * ### Why the numbers are named rather than inlined
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
 */
object QuranStructure {

    // --- Surahs and verses ---------------------------------------------------

    /** The book has 114 surahs. */
    const val SURA_COUNT = 114

    /**
     * The book has 6,236 verses.
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