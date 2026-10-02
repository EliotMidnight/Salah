package com.example.ui.quran

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.quran.QuranSearch
import com.example.data.quran.QuranSearchHit
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The search result emphasises the match, and only the match.
 *
 * ### Why this is a rendered-surface test when the offsets already have one
 *
 * `QuranSearchRangeTest` proves the offsets are right: over all 6,236 verses, the span a
 * highlight covers folds back to the term that matched. That is the hard half, and it was
 * already true before this stage.
 *
 * This proves the other half — that anything *uses* it. The defect was never a wrong
 * offset. `QuranSearchHit.range` was computed on every keystroke, described in six lines
 * of KDoc as the answer to "why is this here", and read by nothing but its own test, so
 * the row showed the whole verse in both scripts with the match in neither. A range test
 * cannot catch that, because the range was always correct.
 *
 * ### Why not a screenshot
 *
 * A picture would show that *something* is emphasised but not *what*, and the property
 * that breaks first is that the emphasis drifts to cover the wrong span — or the whole
 * verse. `Text(AnnotatedString)` publishes its text through `SemanticsProperties.Text`
 * with the span styles intact, so the styled spans are observable without a pixel and the
 * assertion is an equality on a range.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SearchHighlightRenderTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Open the index sheet and search for [query].
     *
     * The query is the sheet's own internal state — it is not a parameter — so the test
     * types it in rather than the sheet taking one just for testing. The field is found by
     * its text-input action rather than by a test tag added to production code.
     */
    private fun search(query: String) {
        compose.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = "English") {
                    QuranIndexSheet(
                        currentSurah = 1,
                        bookmarks = emptyList(),
                        onSelectSurah = {},
                        onSelectSurahAyah = { _, _ -> },
                        onDismiss = {}
                    )
                }
            }
        }
        // The sheet opens on the surahs face; the search field only exists on the search
        // face, so the tab is tapped first. Found by its label rather than by a test tag
        // added to production code for a test's convenience.
        compose.onNodeWithText("Search").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextInput(query)
        // The sheet debounces before scanning 6,236 verses, so waiting for the row to
        // exist waits for the debounce, the search and the composition in one go — and
        // does not depend on how long the debounce happens to be.
    }

    private fun tagOf(hit: QuranSearchHit) = "result_${hit.ref.surah}_${hit.ref.ayah}"

    /**
     * The verse texts a result row publishes, with their span styles.
     *
     * A row publishes the Arabic and the English as separate `AnnotatedString`s, so a
     * caller says which one it means.
     */
    private fun rowTexts(tag: String): List<AnnotatedString> {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        // `Assert.fail` returns Unit, so it cannot be the else of an elvis on a List.
        return node.config.getOrNull(SemanticsProperties.Text)
            ?: throw AssertionError("the result row $tag publishes no text at all")
    }

    /** The single styled span on the row, or null when the whole row is plain. */
    private fun theOnlySpan(tag: String): Pair<AnnotatedString, IntRange>? {
        val spans = rowTexts(tag).flatMap { text ->
            text.spanStyles.map { text to (it.start until it.end) }
        }
        if (spans.isEmpty()) return null
        assertEquals(
            "the row $tag styles ${spans.map { it.second }}, so the match is either " +
                "not emphasised or something else is as well",
            1,
            spans.size
        )
        return spans.single()
    }

    @Test
    fun `an English search result emphasises the word that matched`() {
        val query = "mercy"
        val hit = QuranSearch.searchVerses(query).hits
            .first { it.matchedIn == QuranSearchHit.Field.ENGLISH }

        search(query)
        compose.waitUntilNodeExists(tagOf(hit))

        val span = theOnlySpan(tagOf(hit))
        assertNotNull(
            "the row for ${hit.ref} has no emphasised span, so a reader cannot see " +
                "why it matched — the range was computed and then thrown away",
            span
        )
        assertEquals(
            "the row for ${hit.ref} emphasises \"${span!!.second}\", which is not the " +
                "term that was searched for",
            query,
            hit.ayah.textEnglish
                .substring(span.second.first, span.second.last + 1)
                .lowercase()
        )
    }

    @Test
    fun `an Arabic search result emphasises the word that matched`() {
        val query = "الرحمن"
        val hit = QuranSearch.searchVerses(query).hits
            .first { it.matchedIn == QuranSearchHit.Field.ARABIC }

        search(query)
        compose.waitUntilNodeExists(tagOf(hit))

        val span = theOnlySpan(tagOf(hit))
        assertNotNull(
            "the Arabic row for ${hit.ref} has no emphasised span",
            span
        )
        assertEquals(
            "the row for ${hit.ref} emphasises \"${span!!.second}\" rather than the term " +
                "that was searched for",
            query,
            hit.ayah.textArabic.substring(span.second.first, span.second.last + 1)
                .let { com.example.data.quran.QuranText.normalise(it) }
        )
    }

    @Test
    fun `the emphasised span is the hit's own range, not a re-derived one`() {
        // If the row searched for something that looked like the term instead of using
        // the hit's range, the emphasis could disagree with the range the ordering was
        // decided by — and for a repeated word it would emphasise the wrong occurrence.
        val query = "mercy"
        val hit = QuranSearch.searchVerses(query).hits
            .first { it.matchedIn == QuranSearchHit.Field.ENGLISH }
        val range = hit.range
        assertNotNull("the hit carries no range at all", range)

        search(query)
        compose.waitUntilNodeExists(tagOf(hit))

        assertEquals(
            "the row for ${hit.ref} emphasises $range's neighbour rather than the " +
                "hit's own range",
            range,
            theOnlySpan(tagOf(hit))!!.second
        )
    }

    @Test
    fun `the emphasis sets a colour and nothing that could reflow the row`() {
        // A result row is scanned, so the match has to be findable at a glance — but
        // changing weight or size would move the row on every keystroke as results
        // re-rank, and in a Quranic face a synthetic bold is either absent or a
        // different typeface. Colour moves nothing.
        val query = "mercy"
        val hit = QuranSearch.searchVerses(query).hits
            .first { it.matchedIn == QuranSearchHit.Field.ENGLISH }

        search(query)
        compose.waitUntilNodeExists(tagOf(hit))

        val (text, span) = theOnlySpan(tagOf(hit))!!
        // `spanStyles` is a list of *ranges* carrying a style, so the style is `.item`.
        val style = text.spanStyles.single { (it.start until it.end) == span }.item
        assertTrue(
            "the emphasis sets no colour at all, so there is nothing to see",
            style.color != null
        )
        // `background` is a fully transparent colour on an unset SpanStyle rather than
        // null, so "no highlight behind the text" is `alpha == 0` — an opaque one would
        // be a box behind the match, which is the decoration this avoids.
        val backgroundIsInvisible = style.background == null || style.background.alpha == 0f
        assertTrue(
            "the emphasis changes more than colour, which would reflow the row: $style",
            style.fontWeight == null &&
                // `fontSize` is `Unspecified` rather than null on an unset SpanStyle.
                style.fontSize == TextUnit.Unspecified &&
                style.fontStyle == null &&
                backgroundIsInvisible &&
                style.textDecoration == null &&
                style.letterSpacing == TextUnit.Unspecified &&
                style.baselineShift == null
        )
    }

    @Test
    fun `a row whose match is in Arabic does not also emphasise the English`() {
        // The field that matched decides what is emphasised. Highlighting both would be
        // two claims about one hit, and the English span would be positioned by an Arabic
        // offset.
        val query = "الرحمن"
        val hit = QuranSearch.searchVerses(query).hits
            .first { it.matchedIn == QuranSearchHit.Field.ARABIC }

        search(query)
        compose.waitUntilNodeExists(tagOf(hit))

        val tag = tagOf(hit)
        val english = rowTexts(tag).firstOrNull { it.text == hit.ayah.textEnglish }
        assertNotNull(
            "the row for ${hit.ref} does not show the English verse at all, so this " +
                "cannot tell whether the English was emphasised",
            english
        )
        assertTrue(
            "the English verse on the row for ${hit.ref} is styled too: " +
                english!!.spanStyles,
            english.spanStyles.isEmpty()
        )
    }
}

/**
 * Waits for [tag] to appear, then for the composition to settle.
 *
 * The sheet runs the search behind a debounce, so the row appears a frame or two after
 * the input. A fixed sleep would pass on a fast machine and fail on a slow one; this
 * waits for the thing the test is about.
 */
private fun ComposeTestRule.waitUntilNodeExists(tag: String) {
    waitUntil(5_000) {
        onAllNodesWithTagOrEmpty(tag).isNotEmpty()
    }
    waitForIdle()
}

private fun ComposeTestRule.onAllNodesWithTagOrEmpty(tag: String) =
    onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes()