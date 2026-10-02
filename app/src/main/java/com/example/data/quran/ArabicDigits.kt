package com.example.data.quran

/**
 * Eastern Arabic-Indic numerals, U+0660..U+0669.
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
     */
    fun ayahMarker(ayahNumber: Int): String = "\u06DD" + of(ayahNumber)
}
