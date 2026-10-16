package com.example.ui.home

import com.example.ui.localization.AppLanguage
import com.example.ui.localization.appLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The month sheet's weekday header lines up with the dates under it, in every language.
 *
 * ### The defect this file exists for
 *
 * The header was the literal `"S M T W T F S"` - the last English in the sheet, printed
 * under Arabic and Bengali headings alike. Fixed, it has a second job: the dates are
 * placed with `dayOfWeek.value % 7` leading blanks, so a header that reads Sunday-first
 * while the blanks count from Monday (or the other way round) puts every date under the
 * wrong label, silently - and a language whose labels repeat hides it.
 *
 * ### The trap this file exists to keep closed
 *
 * Reading the platform's *narrow* names alone does not fix the defect: Urdu has no narrow
 * weekday names, and the platform answers with the root locale's, which are the English
 * letters again. The first reading of this fix ruled Urdu in English. [weekdayLabels] is
 * therefore tested for all eleven languages the app ships rather than for the one that
 * prompted it.
 *
 * ### Why this is a test and not a screenshot
 *
 * A screenshot can show the seven labels and cannot show that the 14th is under the right
 * one - the columns are unlabelled numbers either way unless you count them.
 */
class HijriWeekdayLabelsTest {

    /** Every language the app offers, from the list the app itself ships. */
    private val languages = AppLanguage.entries.toList()

    /** The names the platform itself gives for a day, either form the grid may use. */
    private fun platformNames(day: DayOfWeek, locale: Locale): List<String> = listOf(
        day.getDisplayName(TextStyle.NARROW, locale),
        day.getDisplayName(TextStyle.SHORT, locale)
    )

    @Test
    fun `every language the app offers gets seven labels`() {
        for (language in languages) {
            val labels = weekdayLabels(appLocale(language.nativeName))

            assertEquals(
                "${language.englishName} produced ${labels.size} weekday labels",
                7,
                labels.size
            )
            assertTrue(
                "${language.englishName} produced a blank weekday label: $labels",
                labels.all { it.isNotBlank() }
            )
        }
    }

    @Test
    fun `the label above a column is a name the platform gives for that weekday`() {
        // The property, for all seven days in all eleven languages: the header cell the
        // grid's blank arithmetic points at carries the platform's own name for *that*
        // weekday and not a neighbour's. A header in the wrong order fails here for
        // every date in the month at once.
        for (language in languages) {
            val locale = appLocale(language.nativeName)
            val labels = weekdayLabels(locale)

            for (day in DayOfWeek.entries) {
                val column = weekdayColumn(day)
                assertTrue(
                    "${language.englishName}: ${day.name.lowercase()} lands in column " +
                        "$column, which reads '${labels[column]}' - not a name the " +
                        "platform gives for ${day.name.lowercase()}: " +
                        "${platformNames(day, locale)}",
                    labels[column] in platformNames(day, locale)
                )
            }
        }
    }

    @Test
    fun `the columns start at Sunday, which is where a month beginning on Sunday puts its first`() {
        // The one hard fact the two halves share: ISO numbers Monday 1 ... Sunday 7, so
        // `% 7` gives Sunday 0 - and a month starting on a Sunday draws no blank cells.
        assertEquals("Sunday is not the first column", 0, weekdayColumn(DayOfWeek.SUNDAY))
        assertEquals("Monday is not the second column", 1, weekdayColumn(DayOfWeek.MONDAY))
        assertEquals("Saturday is not the last column", 6, weekdayColumn(DayOfWeek.SATURDAY))
    }

    @Test
    fun `no language but English is ruled in the root locale's letters`() {
        // The defect as a property: the sheet was ruled in English in all eleven
        // languages, and the trap above re-creates it for one language while looking
        // fixed everywhere else. Any language whose labels are still the root's letters
        // is reading from a fallback rather than from its own locale data.
        val rootLetters = WEEKDAY_COLUMNS.map { it.getDisplayName(TextStyle.NARROW, Locale.ROOT) }

        for (language in languages) {
            val labels = weekdayLabels(appLocale(language.nativeName))
            if (language == AppLanguage.ENGLISH) {
                // English is the root's own language: these letters are its names, and
                // the single-letter header is what the page is designed around.
                assertEquals(
                    "English no longer reads its own weekday letters",
                    rootLetters,
                    labels
                )
            } else {
                assertNotEquals(
                    "${language.englishName} is still ruled by the root locale's weekday " +
                        "letters $rootLetters",
                    rootLetters,
                    labels
                )
            }
        }
    }

    @Test
    fun `the languages that do not write in Latin script do not read Latin letters`() {
        // Four of the app's languages do not use Latin script at all, and they are the
        // ones the literal failed most visibly.
        for (language in listOf(
            AppLanguage.ARABIC,
            AppLanguage.URDU,
            AppLanguage.BENGALI,
            AppLanguage.RUSSIAN
        )) {
            val labels = weekdayLabels(appLocale(language.nativeName))

            assertTrue(
                "${language.englishName} weekday labels are being read as Latin letters: " +
                    "$labels",
                labels.none { label ->
                    label.any { it.code in 'A'.code..'Z'.code || it.code in 'a'.code..'z'.code }
                }
            )
        }
    }

    @Test
    fun `a date is named in the app's language, not the device's`() {
        // The same `appLocale` the sheet's labels come from is what the date row on
        // Today and Prayer now pass to their formatters. A formatter built with
        // `Locale.getDefault()` reads the *device's* language, which is a separate
        // setting: an Arabic interface on a German phone was reading a German weekday
        // under an Arabic date.
        val date = LocalDate.of(2026, 9, 14)
        fun named(language: AppLanguage) =
            DateTimeFormatter.ofPattern("EEEE d MMMM", appLocale(language.nativeName))
                .format(date)

        val english = named(AppLanguage.ENGLISH)
        assertEquals(
            "the English date no longer reads as it did",
            "Monday 14 September",
            english
        )
        for (language in languages) {
            if (language == AppLanguage.ENGLISH) continue
            assertNotEquals(
                "${language.englishName} names the date in English: ${named(language)}",
                english,
                named(language)
            )
        }
    }

    @Test
    fun `a stored language name resolves to its own locale, not to the device's`() {
        // The app-language name is what the settings store, and every language offers
        // both a native name and an English name. A lookup that fell back to English
        // would rule the German grid with the device's locale instead of German.
        for (language in languages) {
            assertEquals(
                "${language.englishName}: '${language.nativeName}' did not resolve to its " +
                    "own language tag",
                language.code,
                appLocale(language.nativeName).language
            )
            assertEquals(
                "${language.englishName}: '${language.englishName}' did not resolve to " +
                    "its own language tag",
                language.code,
                appLocale(language.englishName).language
            )
        }
    }
}
