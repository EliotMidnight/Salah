package com.example.data.quran

/**
 * Eastern Arabic-Indic numerals, U+0660..U+0669.
 *
 * ### Why this is a function and not a `CharArray` inlined at four call sites
 *
 * The ayah end marker is drawn in the flowing page, the per-verse block, the
 * verse inspector and the copy/share payload. Four copies of
 * `charArrayOf('٠', ...)` means four places to get the digits wrong, and the one
 * that matters most - the payload the reader sends to someone else - is the one
 * that is easiest to forget to update.
 *
 * ### Why Arabic-Indic and not Latin
 *
 * Because the marker is a *marker*, not a number in a sentence. A mushaf's ayah
 * ornament carries its count in the script the rest of the page is set in; a
 * Latin `4` next to ۝ is a font fallback, and on a face without the glyph it is
 * a different digit shape sitting inside an Arabic circle. The digits are part
 * of the typographic unit, so they come from the same set.
 */
object ArabicDigits {

    private const val ZERO = '\u0660'

    /** The digits ٠١٢٣٤٥٦٧٨٩, U+0660..U+0669. */
    private val DIGITS = charArrayOf(
        '\u0660', '\u0661', '\u0662', '\u0663', '\u0664',
        '\u0665', '\u0666', '\u0667', '\u0668', '\u0669'
    )

    /** [number] in Eastern Arabic-Indic numerals. Negative numbers keep a sign. */
    fun of(number: Int): String {
        if (number < 0) return "-" + of(-number)
        if (number == 0) return ZERO.toString()
        val digits = CharArray(number.toString().length)
        var value = number
        for (i in digits.indices.reversed()) {
            digits[i] = DIGITS[value % 10]
            value /= 10
        }
        return String(digits)
    }

    /**
     * The ayah end marker: U+06DD followed by the count.
     *
     * U+06DD is `ARABIC END OF AYAH` - a standalone ornament, not a numeric
     * placeholder, and it is what the Quranic text's own orthography uses. Bundled
     * faces are required to carry it: an unmarked circle is not a mushaf.
     *
     * The marker is built here rather than trusted from the corpus, because the
     * Tanzil text stores verse boundaries as separate records and does not embed
     * the ornament. Building it from the verse model means the number is always
     * the verse's own number, which is the one thing a reader checking the page
     * against their mushaf will verify.
     */
    fun ayahMarker(ayahNumber: Int): String = "\u06DD" + of(ayahNumber)
}
