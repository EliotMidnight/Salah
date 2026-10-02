package com.example.ui

/**
 * Strings that are legitimately the same in another language.
 *
 * ### Why this is one table and not two
 *
 * Two tests need to know the same thing. `StringCoverageTest` walks every
 * `ReaderStrings` field by reflection and fails when a non-English language's value
 * equals the English one; `ReaderAnnouncementTest` checks the paper names through the
 * `when` that reads them. For a *plain field value* those two comparisons are the
 * identical fact — `paperRose` in French is `"Rose"`, and `paperLabel(RED, french)`
 * is `"Rose"` — so an allowlist in each place would be one more fact decided in two
 * places, and adding a borrowed term would have to be remembered twice. The failure
 * mode is not a crash: it is a word silently reverted to English in one test's view
 * and still accepted by the other.
 *
 * **Keyed by the English text, not by the field name.** The tests compare *text*, and
 * text is what a translator reasons about; a field-name key would need a second
 * mapping to reach it.
 *
 * ### What belongs here
 *
 * A word a language has genuinely adopted, or has no other word for:
 *
 * - **True cognates.** French *page*, German *Seite*, Spanish *página*, Indonesian
 *   *halaman*.
 * - **Borrowed terms.** *Mushaf* is used as a loanword in most European languages and
 *   written identically; *Indigo* is likewise.
 * - **Loan translations.** German says *Sand*, Malay says *Apricot* — both are the
 *   correct German and Malay words for the thing, and they happen to be spelled like
 *   the English. Demanding a difference would push a translator to invent a wrong one.
 *
 * What does **not** belong here:
 *
 * - **Typeface names.** Amiri, Lateef and Harmattan are names of designs, not words,
 *   and translating them would make a face unrecognisable. They are English by
 *   design, in `faceLabel`, and are not `ReaderStrings` fields so no test can catch
 *   them here.
 * - **A whole sentence.** The reader's announcement templates are compared as
 *   formatted output, not as field values, and the words in them are `ReaderAnnouncementTest`'s
 *   business — see its own allowlist, which is about a different comparison.
 */
internal object BorrowedWords {

    /** English text → the languages that write it the same way. */
    private val byText: Map<String, Set<String>> = mapOf(
        // --- Reader chrome ---------------------------------------------------
        "Mushaf" to setOf("French", "Indonesian", "Turkish", "Malay", "German", "Spanish"),
        "Vertical" to setOf("French", "Spanish"),
        "Horizontal" to setOf("French", "Indonesian", "German", "Spanish"),

        // --- Paper names -----------------------------------------------------
        "Rose" to setOf("French", "Indonesian", "Malay", "German", "Spanish"),
        "Apricot" to setOf("Indonesian", "Malay"),
        "Sand" to setOf("German"),
        "Indigo" to setOf(
            "French", "Indonesian", "Turkish", "Urdu", "Malay", "Bengali",
            "Russian", "German", "Spanish"
        ),

        // --- Compass directions ----------------------------------------------
        //
        // Abbreviations, so the set is per-direction and per-language rather than
        // "the whole compass" - French agrees with English on north-east, German
        // abbreviates *Nordost* to "NO" and so agrees on nothing but north.
        "N" to setOf("French", "German", "Spanish"),
        "NE" to setOf("French", "Spanish"),
        "E" to setOf("French", "Spanish"),
        "SE" to setOf("French", "Spanish"),
        "S" to setOf("French", "Indonesian", "Malay", "German", "Spanish"),
        "SW" to setOf("German"),
        "W" to setOf("German"),
        "NW" to setOf("German"),

        // --- Religious and scholarly terms kept as loanwords -----------------
        //
        // French and the Indonesian/Malay pair say *juz* and *hizb*; German says *Hizb*
        // but has its own *Juz*; Turkish says *Cüz* and is not listed. Each language
        // that *does* have its own form is required to differ, and is not here.
        "Juz %d" to setOf("French", "Indonesian", "Malay"),
        "Juz %d, %s" to setOf("French", "Indonesian", "Malay"),
        "Hizb %d" to setOf(
            "French", "Indonesian", "Turkish", "Malay", "German", "Spanish"
        ),
        "Hizb" to setOf(
            "French", "Indonesian", "Turkish", "Malay", "German", "Spanish"
        ),

        // --- Proper nouns -----------------------------------------------------
        //
        // The translator of the bundled English translation. A credit line naming a
        // person or an edition is not a description, and translating it makes the
        // attribution wrong rather than merely foreign.
        "Saheeh International" to setOf(
            "French", "Indonesian", "Turkish", "Malay", "Russian", "German", "Spanish"
        ),
        // *Adhan* and *Takbeer* are the Arabic words for the call and the
        // invocation, used untranslated wherever the language has no settled
        // equivalent. Arabic and Urdu have their own and are not listed.
        "Adhan" to setOf("French", "German", "Spanish"),
        "Takbeer" to setOf("French", "German", "Spanish"),
        // The Kaaba's direction. Every language that spells it *qibla* keeps it;
        // Russian says *кибла*, Turkish *kıble*, Arabic *قبلة*.
        "Qibla" to setOf("French", "German", "Spanish"),
        // The body of Quranic reading. English borrowed it from Arabic and so did
        // Indonesian and Malay; German says *Layout*, Turkish * sûre*.
        "Surah" to setOf("Indonesian", "Malay"),
        // A school of transmission. A proper noun in practice.
        "Riwayah" to setOf("French", "Indonesian", "Malay"),

        // --- Cognates ---------------------------------------------------------
        //
        // Not borrowings, just words that happen to be spelled the same. Listed per
        // language because most of them are only the same in some: French *Silence*,
        // *Illumination*, *Volume*, *Latitude*, *Longitude* and *Page* are all
        // identical to English, while German has *Layout* where French has something
        // else and Indonesian has something else again.
        "Silence" to setOf("French"),
        "Illumination" to setOf("French"),
        "Volume" to setOf("French", "Indonesian"),
        "Latitude" to setOf("French"),
        "Longitude" to setOf("French"),
        "Page" to setOf("French", "German", "Spanish", "Indonesian", "Malay"),
        "Layout" to setOf("German"),
        // "min" is the standard abbreviation in French, Malay and Spanish; German uses
        // "Min." and the others write the word out.
        "min" to setOf("French", "Malay", "Spanish")
    )

    /**
     * A `java.util.Formatter` conversion specification.
     *
     * `%`, then an optional explicit index (`%1$s` only - plain `%s` has no `$`), then
     * flags, width and precision, then a conversion letter.
     *
     * The letter is why the substitution below has to happen before anything asks
     * whether the string contains letters: `%s` is two letters if you read it naively,
     * so `"%s · %s"` looks like it contains text.
     */
    private val CONVERSION = Regex("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]")

    /**
     * Whether [text] is nothing but separators and placeholders.
     *
     * `"%d:%d"` and `"%s · %s"` are not untranslated English; they are punctuation and
     * placeholders, and there is nothing in them for a translator to change. This is a
     * property of the string rather than an exception per language, so it is a rule -
     * which means the next value-only string someone adds needs no allowlist entry.
     *
     * A template that mixes placeholders with words (`"Juz %d"`) *does* have words, so
     * it does not qualify here and is listed by language above.
     */
    fun isFormatTemplate(text: String): Boolean =
        CONVERSION.replace(text, "").none { it.isLetter() }

    /**
     * Whether [language] may legitimately use [englishText] unchanged.
     *
     * English is never borrowed from itself. A string with no words in it is never
     * translatable, in any language.
     */
    fun isBorrowed(englishText: String, language: String): Boolean =
        language != "English" &&
            (isFormatTemplate(englishText) || language in byText[englishText].orEmpty())

    /** The languages that keep [englishText], for a failure message. */
    fun languagesFor(englishText: String): Set<String> = byText[englishText].orEmpty()
}