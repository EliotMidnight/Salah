package com.example.ui

import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.FrenchStrings
import com.example.ui.localization.GermanStrings
import com.example.ui.localization.IndonesianStrings
import com.example.ui.localization.MalayStrings
import com.example.ui.localization.RussianStrings
import com.example.ui.localization.SpanishStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UrduStrings
import com.example.ui.localization.UiStrings
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's labels are actually translated.
 *
 * ### Why this test exists
 *
 * `ReaderStrings` is its own data class because the reader's labels had pushed
 * `UiStringsMore` past the JVM's 255-parameter constructor limit - a failure
 * that compiles cleanly and then throws `ClassFormatError` from the class
 * loader at runtime.
 *
 * Giving it its own class fixed that crash and introduced a quieter one. With
 * its own constructor every field got an English default, and because no
 * per-language `UiStrings` passed a `reader =`, the entire reader - the index,
 * immersive mode, both layouts, the axis picker, the pinch target, paper, font
 * and the per-verse toggle - rendered in English for all ten non-English
 * languages. Nothing failed. The build was green, the strings resolved, and a
 * reader who selected Urdu got an English Quran screen.
 *
 * Defaults are a trap here precisely because they compile, so this walks the
 * values instead of reading the source. It catches the class-wide regression,
 * and it catches a label added to `ReaderStrings` that nobody remembered to
 * translate - which is the failure that will actually recur.
 *
 * ### Why there is an allowlist
 *
 * "Differs from English" is not the same as "translated". Some labels are
 * proper nouns that are *supposed* to read the same in every language, and a
 * test that demands they differ would push a translator to invent a wrong
 * transliteration. Those are named below with the reason, so adding one is a
 * deliberate act rather than a way to silence a red build.
 */
class StringCoverageTest {

    private val nonEnglish: List<Pair<String, UiStrings>> = listOf(
        "Arabic" to ArabicStrings,
        "French" to FrenchStrings,
        "Indonesian" to IndonesianStrings,
        "Turkish" to TurkishStrings,
        "Urdu" to UrduStrings,
        "Malay" to MalayStrings,
        "Bengali" to BengaliStrings,
        "Russian" to RussianStrings,
        "German" to GermanStrings,
        "Spanish" to SpanishStrings
    )

    /**
     * Reader labels that are correctly spelled the same as English, by language.
     *
     * "Differs from English" is not the same as "translated". Two kinds of label
     * are supposed to match:
     *
     * - **Borrowed names.** "Mushaf" is the name of the book and is used
     *   untranslated wherever the language has not established its own form.
     *   Languages that *do* have one - Arabic `المصحف`, Urdu `مصحف`, Bengali
     *   `মুশফ`, Russian `Мусхаф` - are required to differ, and are not listed
     *   here. Demanding a difference from the others would push a translator to
     *   invent a transliteration nobody uses.
     * - **True cognates.** French, German, Spanish and Indonesian all spell the
     *   axis words the same way English does.
     *
     * Each entry is per-language on purpose: a field may be borrowed in one
     * language and translated in another, and a global allowlist could not tell
     * those apart.
     */
    private val identicalByDesign: Map<String, Set<String>> = mapOf(
        "mushaf" to setOf("French", "Indonesian", "Turkish", "Malay", "German", "Spanish"),
        "scrollVertical" to setOf("French", "Spanish"),
        "scrollHorizontal" to setOf("French", "Indonesian", "German", "Spanish")
    )

    @Test
    fun `every reader label is translated in every language`() {
        val failures = buildList {
            nonEnglish.forEach { (language, strings) ->
                readerFields().forEach { field ->
                    if (language in identicalByDesign[field].orEmpty()) return@forEach
                    val translated = read(strings.more.reader, field)
                    val english = read(EnglishStrings.more.reader, field)
                    if (translated == english) {
                        add("$language.reader.$field is still the English text (\"$english\")")
                    }
                }
            }
        }

        assertTrue(
            "Reader labels left in English:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun `every language overrides the reader rather than inheriting it`() {
        // The class-wide regression, stated directly. If someone drops a
        // `reader = ReaderStrings(...)` override again, this fails immediately
        // rather than only noticing field by field.
        val bare = com.example.ui.localization.ReaderStrings()

        val failures = nonEnglish.filterNot { (_, strings) ->
            val reader = strings.more.reader
            readerFields().count { read(reader, it) != read(bare, it) } > 20
        }.map { (language, _) -> "$language uses the English defaults for the whole reader" }

        assertTrue(
            "The reader fell back to English defaults:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun `no language ships a blank reader label`() {
        val failures = buildList {
            nonEnglish.forEach { (language, strings) ->
                readerFields().forEach { field ->
                    if (read(strings.more.reader, field).isBlank()) {
                        add("${language}.reader.$field")
                    }
                }
            }
        }

        assertTrue(
            "Blank labels shipped to users:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun `a language that has its own name for a borrowed term uses it`() {
        // The counterpart to [identicalByDesign]. Arabic, Urdu, Bengali and
        // Russian all have an established form for "Mushaf"; if one of them
        // reverted to the English spelling that would be a regression the
        // allowlist would otherwise hide.
        val mustDiffer = setOf("Arabic", "Urdu", "Bengali", "Russian")

        val failures = nonEnglish
            .filter { (language, _) -> language in mustDiffer }
            .filter { (language, strings) ->
                read(strings.more.reader, "mushaf") ==
                    read(EnglishStrings.more.reader, "mushaf")
            }
            .map { (language, _) -> "$language should use its own form of \"Mushaf\"" }

        assertTrue(
            "Borrowed term used where the language has its own:\n" +
                failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    private fun readerFields(): List<String> =
        EnglishStrings.more.reader::class.java.declaredFields
            // `$stable` is emitted by the Compose compiler, not written by hand.
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .map { it.name }

    private fun read(target: Any, field: String): String =
        runCatching {
            target::class.java.declaredFields
                .first { it.name == field }
                .apply { isAccessible = true }
                .get(target) as? String ?: ""
        }.getOrDefault("")
}
