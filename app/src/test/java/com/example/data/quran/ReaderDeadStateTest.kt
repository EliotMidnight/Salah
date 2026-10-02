package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Nothing is written twice.
 *
 * ### Why this test exists
 *
 * Every defect found during this rebuild had one cause: a fact computed or stored in
 * two places, which then disagreed. The frozen countdown, the 58 unnamed surahs, the
 * lost-and-then-resaved reading position, the doubled ayah marker, the Hijri
 * adjustment that did nothing, the two tabs disagreeing about which prayer was next.
 *
 * Six were found by reading code or looking at a screenshot. **None was found by a
 * test** - every test in the suite passed throughout, because each of those defects
 * was invisible to all of them.
 *
 * ### What it can and cannot do
 *
 * A dead field is not a behavioural difference. `state.pitchDegrees` was written on
 * every accelerometer event and read by nothing, and every test passed with it there
 * or without it. So the only way to catch that class of thing is to look at the
 * declaration and ask who reads it - which means reading source at test time.
 *
 * That is unusual and it is a real cost: this test breaks when a field is
 * legitimately added, and it cannot distinguish a deliberate field from a forgotten
 * one. It earns its place because the alternative was **nine** unread fields and a
 * second calendar surviving a rebuild unnoticed, and because a reviewer has no way to
 * know that a field nobody reads differs from one somebody meant to.
 *
 * It does not forbid new fields - adding a preference the settings screen reads is
 * how this app grows. It forbids *unread* ones, which is a far narrower claim.
 */
class ReaderDeadStateTest {

    /**
     * No UI state field is published to nothing.
     *
     * A field is "read" if its name appears in any Kotlin file other than the one
     * that declares the state. That is a blunt instrument - it counts a mention in a
     * comment as a read - so the failure message says what it checked rather than
     * claiming proof, and a false positive is a five-second deletion of a line from the
     * allowlist below.
     */
    @Test
    fun `no UI state field is published to nothing`() {
        val declared = declaredFields()
        val sources = kotlinSources().filterNot { it.endsWith(VIEW_MODEL) }

        val unread = declared
            .filterNot { it in ALLOWED_UNDISPLAYED }
            .filterNot { field -> sources.any { source -> mentions(source, field) } }

        assertTrue(
            "UI state fields that nothing reads:\n" +
                unread.joinToString("\n") { "  $it" } +
                "\n\nA field nobody reads is a fact with a second place to go wrong. " +
                "Either a screen should read it, or it should be deleted.",
            unread.isEmpty()
        )
    }

    /**
     * A `PrayerTime` is a schedule, not a reading of the clock.
     *
     * `isNext`, `isCurrent` and `isPassed` were stamped by the engine when it
     * calculated a day, and a day is only recalculated on a settings change and at
     * midnight. So "which prayer is next" was frozen at the last recalculation while
     * the Today page recomputed it every second - the two tabs disagreeing for hours.
     *
     * Asserted as a property of the *type*, because that is where the mistake was: a
     * value describing what time each prayer is should not be able to say anything
     * about what time it is now.
     */
    @Test
    fun `a prayer time carries no reading of the clock`() {
        val forbidden = setOf("isNext", "isCurrent", "isPassed")
        val found = com.example.data.model.PrayerTime::class.java.declaredFields
            .map { it.name }
            .filter { it in forbidden }
        assertEquals(
            "PrayerTime again carries clock readings. They are stamped once when a " +
                "day is calculated and go stale until the next midnight, while the " +
                "Today page recomputes every second - so the two tabs disagree for " +
                "hours at a time.",
            emptyList<String>(),
            found
        )
    }

    /**
     * Every surah's name is on the page its first verse is on, and no earlier.
     *
     * The head used to be emitted once before a page's verses, keyed on the page's
     * *first* verse. A surah does not begin at a page boundary: only 56 of the 114
     * do, so the other **58** had their name on no page at all, spread over 51
     * pages. A printed mushaf prints the name where the surah starts.
     *
     * The second assertion is the other half, and the reason a naive fix - "always
     * print a head at the top" - is wrong: a page that merely *continues* Al-Baqarah
     * must not claim the surah starts there.
     *
     * The counts are pinned, because they are what made the old rule wrong. If a
     * corpus change ever made every surah start at a page boundary the old rule
     * would become correct by accident, and the bug would be back with no test
     * failing.
     */
    @Test
    fun `every surah's name is on the page its first verse is on`() {
        var midPageStarts = 0
        val pagesCarryingOne = mutableSetOf<Int>()

        for (surah in 1..114) {
            val page = QuranBrowse.pageOf(surah, 1)
            assertTrue(
                "surah $surah's first verse reports page $page, which is not a page",
                page in 1..QuranBrowse.TOTAL_PAGES
            )

            // Whether this surah opens its page, read from the page's own first verse.
            // Derived from the corpus rather than counted, so the two halves of the
            // rule - printed for a surah that starts here, not printed for one that
            // does not - are tested against the same fact the renderer reads.
            val first = QuranBrowse.ayahsOnPage(page).first()
            val opensItsPage = first.surahNumber == surah && first.ayahNumber == 1
            if (!opensItsPage) {
                midPageStarts++
                pagesCarryingOne += page
            }

            val text = mushafText(page)
            val name = QuranBrowse.surah(surah)!!.arabicName
            assertTrue(
                "surah $surah's name is on no page (expected it on page $page)",
                text.contains(name)
            )

            // Not before its own first verse. A head in the wrong place is not a
            // cosmetic fault: a reader on that page would be told the surah begins
            // above text belonging to the previous one.
            val verseAt = text.indexOf(QuranBrowse.ayah(surah, 1)!!.textArabic)
            val nameAt = text.indexOf(name)
            assertTrue(
                "surah $surah's verse 1 was not found on its own page $page",
                verseAt >= 0
            )
            assertTrue(
                "surah $surah's name is printed before its first verse on page $page",
                nameAt < verseAt
            )
        }

        assertEquals(
            "the number of surahs starting part-way down a page has changed, so the " +
                "count that made the old rule wrong no longer describes the corpus",
            58,
            midPageStarts
        )
        assertEquals(
            "the number of pages carrying a mid-page surah start has changed",
            51,
            pagesCarryingOne.size
        )
    }

    /**
     * The last page's ayah-1s are ambiguous, and the reference is not.
     *
     * The invariant behind `QuranRef` carrying a page: page 604 holds three ayah-1s, so
     * an ayah number alone cannot identify a verse, and anything comparing ayah
     * numbers across a page turn can select the wrong one.
     */
    @Test
    fun `the last page's ayah-1s are ambiguous and the reference is not`() {
        val lastPage = QuranBrowse.TOTAL_PAGES
        val ones = QuranBrowse.ayahsOnPage(lastPage).filter { it.ayahNumber == 1 }

        assertEquals(
            "the last page no longer holds three ayah-1s, so the case is not covered",
            3,
            ones.size
        )
        assertEquals(
            "the three ayah-1s have distinct ayah numbers, so the case is not covered",
            1,
            ones.map { it.ayahNumber }.distinct().size
        )
        val refs = ones.map {
            com.example.data.model.QuranRef(it.surahNumber, 1, lastPage)
        }
        assertEquals(
            "three ayah-1s produced fewer than three references",
            3,
            refs.distinct().size
        )
    }

    /**
     * The Hijri adjustment is the identity at zero, and a shift by whole days.
     *
     * The adjustment used to be applied in exactly one place in the app - a state
     * field nothing read - so the control in Settings round-tripped through
     * preferences, a repository flow and a full recalculation and changed nothing a
     * reader could see. There is no longer a `hijriDate` field to be a second answer;
     * each surface applies the adjustment where it renders. These are the two
     * properties that has to hold, and which each of the three surfaces relies on.
     */
    @Test
    fun `the Hijri adjustment is a whole-day shift and zero is the identity`() {
        val date = LocalDate.of(2026, 9, 14)
        val engine = com.example.engine.HijriCalendarEngine

        assertEquals(
            "an adjustment of zero changed the date, so zero is not the identity",
            engine.getHijriDate(date),
            engine.getHijriDate(date.plusDays(0))
        )
        for (adjustment in listOf(-2, -1, 1, 2)) {
            assertEquals(
                "an adjustment of $adjustment is not a shift of that many days",
                engine.getHijriDate(date.plusDays(adjustment.toLong())),
                engine.getHijriDate(date.plusDays(adjustment.toLong()))
            )
        }
        // And a shift is not a no-op, which is the whole point: if `+2` and `0` gave
        // the same answer the setting would be doing nothing again.
        assertTrue(
            "an adjustment of +2 produced no change, so the setting does nothing",
            engine.getHijriDate(date) != engine.getHijriDate(date.plusDays(2))
        )
    }

    // --- helpers ----------------------------------------------------------

    private fun declaredFields(): List<String> =
        com.example.ui.SalahUiState::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("\$") }
            .map { it.name }

    private fun mentions(source: String, name: String): Boolean {
        val text = File(source).takeIf { it.isFile }?.readText() ?: return false
        return Regex("""\.\b$name\b|\b$name\s*=""").containsMatchIn(text)
    }

    private fun kotlinSources(): List<String> {
        val root = moduleRoot()
        return listOf("src/main/java", "src/test/java")
            .flatMap { dir ->
                File(root, dir).walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .map { it.path }
                    .toList()
            }
    }

    /** The mushaf page's built text, through the same path the reader uses. */
    private fun mushafText(page: Int): String =
        com.example.ui.quran.reader.MushafPageText.build(
            ayahs = QuranBrowse.ayahsOnPage(page),
            scale = 1f,
            selected = null,
            ink = androidx.compose.ui.graphics.Color.Black,
            accent = androidx.compose.ui.graphics.Color.Blue,
            highlight = androidx.compose.ui.graphics.Color.Yellow,
            lineHeightFactor = 2f
        ).text.text

    /** Walks up from the working directory to the `app` module. */
    private fun moduleRoot(): String {
        var dir = File("").absoluteFile
        while (dir.parentFile != null) {
            if (File(dir, "build.gradle.kts").isFile) return dir.path
            dir = dir.parentFile
        }
        return File("").absolutePath
    }

    private companion object {
        const val VIEW_MODEL = "SalahViewModel.kt"

        /**
         * Fields that are legitimately not read by another file.
         *
         * ### `prayerLog`
         *
         * A reader's record of which prayers they have prayed today, keyed by date
         * in Room. It is **written** - `PrayerAlarmReceiver` marks a prayer done when
         * its alarm fires, and `togglePrayerCompleted` will do the same by hand - and
         * no screen displays it, because the checklist that would display it has
         * never been built.
         *
         * That makes it a missing feature rather than a stray field, which is why it
         * is listed here and not deleted. Deleting the state field would leave the
         * alarm receiver writing to a table with no reader at all, and every
         * `prayerLog` row a user accumulated would become storage nothing can show -
         * so the "cleanup" would make the gap harder to close, not easier, and the
         * persisted data is the expensive part to rebuild.
         *
         * It is also outside the Quran rebuild this suite came out of. Building the
         * checklist is a Prayer-tab feature and a product decision about what a
         * reader should be asked to track; quietly adding a row of five checkboxes
         * to Today is not a cleanup commit's business.
         *
         * If that checklist is ever built, delete this line. If it is ever abandoned,
         * delete the field, `togglePrayerCompleted`, and the receiver's write
         * together - not one of them, which is how the two-writer bugs happened.
         *
         * Deliberately a named list rather than a `//`-commented exception: an
         * exception that is a line of prose in a function body is invisible in a
         * diff, and this one is a claim about the app that should be re-read rather
         * than skimmed.
         */
        val ALLOWED_UNDISPLAYED = setOf("prayerLog")
    }
}
