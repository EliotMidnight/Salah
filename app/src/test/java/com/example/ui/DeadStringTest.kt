package com.example.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No string field is declared and read by nothing.
 *
 * ### The duplicate check that is deliberately not here
 *
 * There were once **six** strings for two layout options, three of them spelling
 * "Continuous", plus `fontNotBundled` ("Not included yet") when every face in the
 * picker had shipped for a while. Duplicated copy is the same defect as dead copy — one
 * fact in two places — and all of it went in the cleanup this file guards.
 *
 * A test for it was written and then **removed**, because it cannot be made sound.
 * Matching on the English text finds real duplicates and also two fields that are
 * correctly separate: `versesFound` ("12 verses found", in search results) and
 * `verseCount` ("Al-Kahf · 110 verses", on a surah) are the same English phrase for two
 * different facts, and a translator may well want them to diverge. Comparing every
 * language does not help — they are identical in all ten too.
 *
 * An unsound detector gets an allowlist to make it pass, and an allowlisted detector
 * checks nothing while still costing a build. So the duplication is recorded here and
 * in the commit, and the guard below sticks to what it can prove: a field with no
 * reader at all.
 *
 * ### What this is for
 *
 * `StringCoverageTest` asks whether a string is **translated**. It cannot ask whether a
 * string is **used**, and a translated string nobody reads passes it perfectly — in all
 * ten languages, on every build, forever.
 *
 * That is how **133 fields** accumulated in the string classes. They were not
 * mistranslations; they were whole features' worth of copy whose controls had been
 * removed, left behind still carrying a translation in every language. Several were
 * duplicate spellings of one concept, which is the more interesting case:
 *
 * - **six** strings for two layout options — `layoutPerVerse` and `cardsLayout` both
 *   said "Per verse", and `layoutContinuous`, `continuousModeLabel` and
 *   `continuousLayout` all said "Continuous". Three spellings of one word, in ten
 *   languages each.
 * - `previousSurahLabel` and `nextSurahLabel` were the accessibility labels of the 48dp
 *   tap gutters this rebuild deliberately deleted. They survived the deletion because
 *   the strings file has no idea a control was removed.
 * - `showControls` and `fontNotBundled` both lived in `ReaderStrings`, and
 *   `fontNotBundled` ("Not included yet") had been true for a while: every face in the
 *   picker now ships.
 *
 * ### Why a detector rather than a one-off cleanup
 *
 * Deleting them once fixes today's hundred and thirty-three. The ones that came back
 * before were deleted *deliberately* — a control was removed and its copy was not — and
 * that will happen again. A test that fails when a field has no reader is what makes the
 * cleanup hold, and it costs one pass over the source tree.
 *
 * ### How it reads "read by nothing"
 *
 * An identifier that appears nowhere but its own declaration and its ten per-language
 * assignments. Per-language assignments are the *storage* of a string, not a use of it,
 * so counting them would make every field look used.
 */
class DeadStringTest {

    private val localization =
        File("src/main/java/com/example/ui/localization/SalahLocalization.kt")

    private fun allSourceFiles(): List<File> =
        File("app/src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    /**
     * The lines of [file] that are neither a declaration, a per-language assignment nor
     * a comment — the part of the file where a *use* can live.
     */
    private fun codeLines(file: File): List<String> =
        file.readLines().filter { line ->
            val st = line.trim()
            !st.startsWith("*") && !st.startsWith("//") && !st.startsWith("/*") &&
                !Regex("^\\s*val \\w+\\s*:").containsMatchIn(line) &&
                !Regex("^\\s*\\w+\\s*=\\s*\"").containsMatchIn(line)
        }

    @Test
    fun `no string field is read by nothing`() {
        assertTrue(
            "${localization.path} does not exist",
            localization.isFile
        )
        val code = codeLines(localization).joinToString("\n")

        val elsewhere = HashMap<String, Int>()
        val here = localization.absoluteFile
        for (file in allSourceFiles()) {
            if (file.absoluteFile == here) continue
            for (word in Regex("[A-Za-z_][A-Za-z0-9_]*").findAll(file.readText())) {
                val name = word.value
                elsewhere[name] = (elsewhere[name] ?: 0) + 1
            }
        }

        val dead = Regex("^\\s*val (\\w+)\\s*:").findAll(localization.readText())
            .map { it.groupValues[1] }
            .filter { field ->
                !Regex("\\b" + Regex.escape(field) + "\\b").containsMatchIn(code) &&
                    (elsewhere[field] ?: 0) == 0
            }
            .toList()

        assertTrue(
            buildString {
                appendLine(
                    "${dead.size} string fields are declared, translated into ten " +
                        "languages, and read by nothing. Each is copy for a control " +
                        "that is not there, and a translation nobody sees is the " +
                        "easiest kind of code to keep forever:"
                )
                for (field in dead) {
                    appendLine("  - $field")
                }
                append(
                    "Delete them, or wire them up. tools/prune_dead_strings.py lists " +
                        "them and removes the declarations safely."
                )
            },
            dead.isEmpty()
        )
    }
}
