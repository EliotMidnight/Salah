package com.example.ui.quran

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.local.BookmarkEntity
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranSearch
import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The index's search results and saved list name a surah in the reader's language.
 *
 * ### The defect
 *
 * The surah list leads with the Arabic name in an Arabic interface - the row carries
 * two names precisely so a reader looking for الفاتحة finds الفاتحة
 * (`SurahIndexNameOrderTest`). The other two lists did not follow: a search result and
 * a saved verse named their surah in transliteration in every language, so a reader
 * who typed الفاتحة was handed "Al-Fatihah · 1:1".
 *
 * ### What is checked, and why by rendering
 *
 * The shipped rows, in an Arabic sheet, opened the way a reader opens them. The rule is
 * one call per row ([surahDisplayName]), but no unit test of that call would notice a
 * row not making it - the failure the reader actually saw. The text read is the row's
 * own `SemanticsProperties.Text`, so what is asserted is what reaches the reader.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class IndexSurahNamesTest {

    @get:Rule
    val compose = createComposeRule()

    private fun mountIndex(language: String) {
        compose.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = language) {
                    QuranIndexSheet(
                        currentSurah = 1,
                        bookmarks = listOf(BookmarkEntity(surahNumber = 2, ayahNumber = 255)),
                        onSelectSurah = {},
                        onSelectSurahAyah = { _, _ -> },
                        onDismiss = {}
                    )
                }
            }
        }
    }

    /** The text a row publishes to a reader, in composition order. */
    private fun rowTexts(tag: String): List<String> {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
    }

    @Test
    fun `an Arabic search result names its surah in Arabic`() {
        // A word of the English translation, so this is the search a reader uses to
        // find a verse they only know in English - and the row still has to tell an
        // Arabic reader which surah it landed in.
        val query = "mercy"
        val hit = QuranSearch.searchVerses(query).hits.first()

        mountIndex("Arabic")
        // The sheet opens on the surahs face; the search field only exists on the
        // search face, so the tab is tapped first. Found by its label rather than by a
        // test tag added to production code for a test's convenience.
        compose.onNodeWithText(ArabicStrings.more.reader.indexSearch).performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextInput(query)
        // The sheet debounces before scanning the corpus, so waiting for the row to
        // exist waits for the debounce, the search and the composition in one go.
        val tag = "result_${hit.ref.surah}_${hit.ref.ayah}"
        compose.waitUntilNodeExists(tag)

        val surah = QuranBrowse.surah(hit.ref.surah)!!
        val header = "${surah.arabicName} · ${hit.ref.surah}:${hit.ref.ayah}"
        assertTrue(
            "the row for ${hit.ref} shows ${rowTexts(tag)}, whose header is not " +
                "\"$header\"",
            rowTexts(tag).contains(header)
        )
    }

    @Test
    fun `an Arabic saved verse names its surah in Arabic`() {
        mountIndex("Arabic")
        compose.onNodeWithText(ArabicStrings.more.reader.indexSaved).performClick()
        compose.waitForIdle()

        val surah = QuranBrowse.surah(2)!!
        val header = "${surah.arabicName} · 2:255"
        assertTrue(
            "the saved row for 2:255 shows ${rowTexts("saved_2_255")}, whose header " +
                "is not \"$header\"",
            rowTexts("saved_2_255").contains(header)
        )
    }
}

/**
 * Wait for a tagged node to be composed.
 *
 * The search debounces before scanning the corpus. A fixed sleep would pass on a fast
 * machine and fail on a slow one; this waits for the thing the test is about.
 */
private fun ComposeTestRule.waitUntilNodeExists(tag: String) {
    waitUntil(5_000) { onAllNodesWithTagOrEmpty(tag).isNotEmpty() }
    waitForIdle()
}

private fun ComposeTestRule.onAllNodesWithTagOrEmpty(tag: String) =
    onAllNodes(hasTestTag(tag)).fetchSemanticsNodes()
