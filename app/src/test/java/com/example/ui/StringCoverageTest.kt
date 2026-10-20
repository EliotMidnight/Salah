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
import com.example.ui.localization.SystemAccessStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.UiStringsMore
import com.example.ui.localization.UrduStrings
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun `every surviving label is translated in every language`() {
        val failures = buildList {
            nonEnglish.forEach { (language, strings) ->
                stringFields(UiStrings::class.java).forEach { field ->
                    val translated = read(strings, field)
                    val english = read(EnglishStrings, field)
                    if (translated != english) return@forEach
                    if (BorrowedWords.isBorrowed(english, language)) return@forEach
                    add("$language.$field is still the English text (\"$english\")")
                }
                stringFields(UiStringsMore::class.java).forEach { field ->
                    val translated = read(strings.more, field)
                    val english = read(EnglishStrings.more, field)
                    if (translated != english) return@forEach
                    if (BorrowedWords.isBorrowed(english, language)) return@forEach
                    add("$language.more.$field is still the English text (\"$english\")")
                }
                stringFields(SystemAccessStrings::class.java).forEach { field ->
                    val translated = read(strings.system, field)
                    val english = read(EnglishStrings.system, field)
                    if (translated != english) return@forEach
                    if (BorrowedWords.isBorrowed(english, language)) return@forEach
                    add("$language.system.$field is still the English text (\"$english\")")
                }
            }
        }

        assertTrue(
            "Labels left in English:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun `no language ships a blank surviving label`() {
        val failures = buildList {
            nonEnglish.forEach { (language, strings) ->
                stringFields(UiStrings::class.java).forEach { field ->
                    if (read(strings, field).isBlank()) add("$language.$field")
                }
                stringFields(UiStringsMore::class.java).forEach { field ->
                    if (read(strings.more, field).isBlank()) add("$language.more.$field")
                }
                stringFields(SystemAccessStrings::class.java).forEach { field ->
                    if (read(strings.system, field).isBlank()) add("$language.system.$field")
                }
            }
        }

        assertTrue(
            "Blank labels shipped to users:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    private fun stringFields(type: Class<*>): List<String> =
        type.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .filter { it.type == String::class.java }
            .map { it.name }

    private fun read(target: Any, field: String): String =
        target::class.java.declaredFields
            .first { it.name == field }
            .apply { isAccessible = true }
            .get(target) as? String ?: ""
}
