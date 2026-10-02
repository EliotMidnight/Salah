package com.example.ui.quran

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.FrenchStrings
import com.example.ui.localization.GermanStrings
import com.example.ui.localization.IndonesianStrings
import com.example.ui.localization.MalayStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.localization.RussianStrings
import com.example.ui.localization.SpanishStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.UrduStrings
import com.example.ui.quran.reader.ReaderPosition
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reader is announced in the reader's language.
 *
 * ### The defect this file exists for
 *
 * A mushaf page announced itself as a pre-rendered English sentence - "Page 42, juz'
 * 21, surah 2 to 2, 15 verses, from 2:255 to 2:255" - assembled in a `private fun` in
 * the reader, which had no access to the strings. The *page number* in the pill beside
 * it was already localized, on the same screen, about the same thing. So a
 * screen-reader user with the interface in Arabic heard an English sentence and then
 * an Arabic word, from one node.
 *
 * Ten languages ship. The continuous block had the same sentence, and a third copy in
 * the custom-action labels that announced only an ayah number - so two blocks could
 * offer identically-named actions.
 *
 * ### Why it needed a test at all
 *
 * The strings were *absent* rather than wrong, so no existing test failed and no
 * screenshot changed: `contentDescription` is invisible in a rendered image. Nothing in
 * the suite could see a reader's entire spoken experience of the book in the wrong
 * language, and nothing could have.
 *
 * So this asserts the announcement directly, in every language that ships, and checks
 * that the *words* of the announcement are that language's - not merely that the
 * string is non-empty, which is what a "did I remember to pass something?" test would
 * do.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ReaderAnnouncementTest {

    @get:Rule
    val compose = createComposeRule()

    private val languages: List<Pair<String, UiStrings>> = listOf(
        "English" to EnglishStrings,
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

    private fun announcementOnPage(
        language: String,
        page: Int
    ): String {
        val ref: QuranRef = QuranBrowse.placeAtPage(page).verse
        compose.setContent {
            SalahTheme {
                ProvideAppLanguage(language = language) {
                    QuranReader(
                        state = com.example.ui.SalahUiState(),
                        position = ReaderPosition(ref),
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        options = com.example.data.model.QuranReadingOptions(
                            layout = com.example.data.model.QuranReadingLayout.PER_PAGE,
                            font = QuranFontFace.AMIRI
                        ),
                        onOptionsChange = {},
                        immersive = false,
                        onImmersiveChange = {},
                        onOpenIndex = {},
                        onOpenOptions = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        // A pager composes the page and its neighbours, so the text node's tag is
        // shared by every page on screen. All of them are collected and the one for
        // [page] is picked out - which is also a fair thing to assert, because a
        // neighbour carrying the wrong page's announcement would be the same bug.
        val said = compose.onAllNodesWithTag("mushaf_page_text")
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                // ContentDescription is a List<String>; only Text carries
                // AnnotatedStrings.
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.joinToString(" ")
            }
        return said.firstOrNull { it.contains("Page $page") || it.contains("$page") }
            ?: error(
                "no composed page announced page $page. Announced: $said"
            )
    }

    @Test
    fun `the page is announced in English, in English`() {
        val said = announcementOnPage("English", 2)
        assertTrue(
            "the English announcement does not mention the page: \"$said\"",
            said.contains("Page 2")
        )
        assertTrue(
            "the announcement does not carry the verse count: \"$said\"",
            said.contains("verses") || said.contains("verse")
        )
    }

    /**
     * Words an announcement is allowed to share with English, by language.
     *
     * "Differs from English" is not the same as "translated". French writes *page*,
     * *sourate* and *verset*; the last contains "verse" as a substring; German and
     * Spanish keep a couple of cognates. A test that demands a difference would push a
     * translator to invent a wrong word, so the exceptions are named here - per
     * language, because a word may be borrowed in one language and translated in
     * another.
     *
     * This is the same reasoning, and the same allowlist shape, as
     * `StringCoverageTest`'s `identicalByDesign`.
     */
    private val borrowedWords: Map<String, Set<String>> = mapOf(
        "Page" to setOf("French", "German", "Spanish", "Indonesian", "Malay"),
        "page" to setOf("French", "German", "Spanish", "Indonesian", "Malay"),
        "verse" to setOf("French", "German"),
        "verses" to setOf("French", "German"),
        "surah" to setOf("Indonesian", "Malay"),
        // "juz'" with the apostrophe, because that is the word the template holds -
        // a French announcement of the *juz'* really is the English "juz'".
        "juz'" to setOf("French", "Indonesian", "Malay", "German", "Spanish")
    )

    /** Whether [language] may legitimately write [word] the English way. */
    private fun borrowed(language: String, word: String): Boolean =
        language in borrowedWords[word].orEmpty()

    @Test
    fun `the page is announced in the reader's language, in every language`() {
        // The core assertion. English words in a non-English announcement is the
        // defect, verbatim, so the test looks for the English words the old
        // hard-coded sentence was made of - minus the ones a language legitimately
        // shares.
        val englishWords = listOf("Page", "juz'", "surah", "verses", "verse", "from")

        for ((language, strings) in languages) {
            if (language == "English") continue
            val reader = strings.more.reader
            val said = reader.pageAnnouncement.format(
                2, 2, reader.range(2, 2), reader.verseCount(6),
                reader.reference(2, 1), reader.reference(2, 6)
            )
            for (word in englishWords) {
                if (borrowed(language, word)) continue
                assertFalse(
                    "the $language page announcement contains the English word " +
                        "\"$word\": \"$said\"",
                    said.contains(word)
                )
            }
            assertTrue("the $language page announcement is empty", said.isNotBlank())
        }
    }

    @Test
    fun `a block is announced in the reader's language too`() {
        for ((language, strings) in languages) {
            if (language == "English") continue
            val reader = strings.more.reader
            val said = reader.blockAnnouncement.format(
                reader.reference(2, 1),
                reader.reference(2, 12),
                reader.verseCount(12),
                2
            )
            for (word in listOf("verses", "verse", "page", "Page")) {
                if (borrowed(language, word)) continue
                assertFalse(
                    "the $language block announcement contains the English word " +
                        "\"$word\": \"$said\"",
                    said.contains(word)
                )
            }
            assertTrue("the $language block announcement is empty", said.isNotBlank())
        }
    }

    @Test
    fun `every announcement template is filled, in every language`() {
        // A template with a missing argument prints a bare "%1$s" and reads as noise
        // rather than as an error, and one with too many is silently ignored. Both are
        // invisible except to a reader, so they are checked here.
        for ((language, strings) in languages) {
            val reader = strings.more.reader
            val page = reader.pageAnnouncement.format(
                2, 2, reader.range(2, 3), reader.verseCount(15),
                reader.reference(2, 255), reader.reference(3, 1)
            )
            val block = reader.blockAnnouncement.format(
                "2:255", "3:1", reader.verseCount(15), 604
            )
            for ((what, said) in listOf("page" to page, "block" to block)) {
                assertFalse(
                    "the $language $what announcement has an unfilled placeholder: " +
                        "\"$said\"",
                    said.contains("%")
                )
                assertTrue("the $language $what announcement is blank", said.isNotBlank())
            }
        }
    }

    @Test
    fun `the counts read the same digits as the page number beside them`() {
        // One convention for one fact. A count rendered in Arabic-Indic digits inside
        // an announcement whose page number is Latin on screen is two conventions for
        // one reader, and a screen reader has no way to reconcile them.
        for ((language, strings) in languages) {
            val said = strings.more.reader.verseCount(15)
            assertFalse(
                "the $language verse count uses non-Latin digits: \"$said\"",
                said.any { it in '٠'..'٩' || it in '۰'..'۹' }
            )
        }
    }

    @Test
    fun `a single verse is not announced in the plural`() {
        // Not "the singular form contains a 1" - Arabic and Urdu write "one" as a
        // word and legitimately carry no digit at all. What must hold is that the
        // singular form is genuinely a *different* form, so a lone verse is never
        // announced as "1 verses".
        for ((language, strings) in languages) {
            val one = strings.more.reader.verseCount(1)
            val many = strings.more.reader.verseCount(7)
            assertNotEquals(
                "the $language singular and plural forms are identical, so a lone " +
                    "verse is announced as a plural",
                one,
                many
            )
        }
    }

    @Test
    fun `the page range across a surah boundary uses the localized joining word`() {
        // Page 604 holds three surahs, so its announcement has a range in it, and the
        // joining word is the piece that is English in every language if it is
        // hard-coded.
        val english = EnglishStrings.more.reader
        val arabic = ArabicStrings.more.reader

        val englishSaid = english.pageAnnouncement.format(
            604, 30, english.range(112, 114), english.verseCount(8),
            english.reference(112, 1), english.reference(114, 6)
        )
        val arabicSaid = arabic.pageAnnouncement.format(
            604, 30, arabic.range(112, 114), arabic.verseCount(8),
            arabic.reference(112, 1), arabic.reference(114, 6)
        )

        assertEquals(
            "the English range did not read as a range",
            "112 to 114",
            english.range(112, 114)
        )
        assertTrue(
            "the Arabic announcement did not carry the Arabic joining word: " +
                "\"$arabicSaid\"",
            arabicSaid.contains(arabic.range(112, 114))
        )
        assertFalse(
            "the Arabic announcement used the English joining word: \"$arabicSaid\"",
            arabicSaid.contains("112 to 114")
        )
        // And a page inside one surah is one number, not "2 to 2".
        assertEquals(
            "a range whose ends are the same was written as a range",
            "2",
            english.range(2, 2)
        )
    }
}
