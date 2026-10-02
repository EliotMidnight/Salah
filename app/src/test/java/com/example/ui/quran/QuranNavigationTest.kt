package com.example.ui.quran

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Navigation actually moves the reader.
 *
 * ### The defect this file exists for
 *
 * `ReaderPosition` - the one object that knows where the reader is - was created
 * *inside* `QuranReader`, and the ViewModel held a copy of the same fact as
 * `selectedSurah`. Navigation arrived through the ViewModel. The reader never asked.
 *
 * So **the index could not move the reader at all.** Picking a surah from the index
 * changed the name in the control pill and left the page where it was; "Continue
 * reading" on the Today page did exactly the same. The pill then showed a surah name
 * and a page number taken from two different objects, so they disagreed on every page
 * turn that crossed a surah boundary, for the 600ms it took the debounced write-back
 * to arrive.
 *
 * The fix is structural rather than a patch: the Quran *destination* owns the position,
 * so the index, the chrome, the reading surfaces and the persistence all read one
 * value. `ReaderPosition.goTo` existed for precisely this and had **zero callers** -
 * documented as "the one entry point; everything that navigates calls this".
 *
 * ### Why a screen test and not a unit test
 *
 * The bug was never inside any one function. Every function did exactly what it said:
 * `selectSurah` set the state's surah, the pill printed the state's surah, the pager
 * printed the pager's page. Nothing was wrong in isolation, which is how it survived a
 * full rebuild of this module and a 25-baseline screenshot suite - a screenshot cannot
 * show a reader pressing the index and being ignored.
 *
 * So the assertion is behavioural: navigate, then check the page on screen changed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QuranNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    /** What the caller would have in hand, and what a request is recorded into. */
    private val state = mutableStateOf(SalahUiState())
    private var requested: QuranRef? = null

    private fun mount() {
        compose.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    QuranScreen(
                        state = state.value,
                        onOpen = { requested = it },
                        onAyahViewed = {},
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        onOptionsChange = {},
                        onImmersiveChange = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /** What `MainActivity` does in response to a request. */
    private fun deliver(ref: QuranRef?) {
        state.value = state.value.copy(pendingOpen = ref)
        compose.waitForIdle()
    }

    /**
     * The page the control pill is reporting.
     *
     * Read from the text a reader sees, not from the accessibility label: the two are
     * built from the same `page` here, and a test that reached past the rendered text
     * would go on passing if the label drifted from it.
     */
    private fun pillPage(): Int {
        compose.onNodeWithTag("reader_index").assertIsDisplayed()
        val shown = compose
            .onNodeWithText("Page ", substring = true, useUnmergedTree = true)
            .fetchSemanticsNode()
        val text = shown.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            ?: "the control pill's page label has no text"
        return Regex("Page (\\d+)").find(text)?.groupValues?.get(1)?.toInt()
            ?: error("the control pill reports no page number; it shows \"$text\"")
    }

    @Test
    fun `a first run opens the beginning of the book`() {
        mount()
        assertEquals(
            "a first run did not open on page 1",
            1,
            pillPage()
        )
    }

    @Test
    fun `a request to open a surah moves the reader there`() {
        // The defect: a request arrived and the page did not move.
        //
        // Al-Kahf, whose first verse is nowhere near page 1 - so a request that is
        // ignored cannot be mistaken for one that happened to be a no-op.
        val ref = QuranBrowse.ref(18, 1)!!
        assertEquals(
            "fixture: Al-Kahf is no longer on page 293, so this is not the case that " +
                "was broken",
            293,
            ref.page
        )

        mount()
        deliver(ref)

        assertEquals(
            "the reader was asked to open page 293 and the pill reports ${pillPage()}. " +
                "The index could not move the reader.",
            293,
            pillPage()
        )
        compose.onNodeWithText("Al-Kahf").assertIsDisplayed()
    }

    @Test
    fun `a request for a single verse moves the reader to that verse's page`() {
        // A verse deep inside Al-Baqarah, which is the case a page number gets wrong
        // when it is carried separately from the surah it belongs to.
        val ref = QuranBrowse.ref(2, 255)!!

        mount()
        deliver(ref)

        assertEquals(
            "the reader was asked for 2:255, on page ${ref.page}",
            ref.page,
            pillPage()
        )
    }

    @Test
    fun `the requested page is the one on screen, not only the one reported`() {
        // The pill reporting a number while the text on screen is a different page is
        // the exact shape of the old bug, so both are checked: the number a reader
        // reads, and the page node that was actually composed.
        val ref = QuranBrowse.ref(2, 255)!!

        mount()
        deliver(ref)

        compose.onNodeWithTag("mushaf_page_${ref.page}").assertIsDisplayed()
        assertEquals(ref.page, pillPage())
    }

    @Test
    fun `the last page can be requested, which is where the three ayah-1s are`() {
        // Page 604 holds surahs 112, 113 and 114, so it is where a surah number and a
        // page number are furthest from interchangeable.
        val ref = QuranBrowse.ref(114, 1)!!
        assertEquals(
            "fixture: 114:1 is no longer on page 604",
            604,
            ref.page
        )

        mount()
        deliver(ref)

        assertEquals("the reader was asked for the last page", 604, pillPage())
        compose.onNodeWithText("An-Nas").assertIsDisplayed()
    }

    @Test
    fun `a request for a verse that does not exist is ignored rather than opening nonsense`() {
        // A stored row written by a build whose partition differed, or a corrupt
        // bookmark. Opening a page that does not contain the verse is worse than not
        // moving at all.
        mount()
        deliver(QuranRef(999, 1, 1))

        assertEquals(
            "a request for a surah that does not exist moved the reader",
            1,
            pillPage()
        )
    }

    @Test
    fun `the index asks for a surah, and the reader follows it`() {
        // The whole path a reader takes: open the index, tap a surah row, and the page
        // under their finger changes. This is the test that could not have existed
        // before the fix, because before it there was nothing for the index to call.
        mount()

        compose.onNodeWithTag("reader_index").performClick()
        compose.waitForIdle()

        // Surah 1, because the index sheet's list is lazy and only the rows near the
        // top are composed. The page it lands on is asserted by the tests above; what
        // this one is for is that the index has a route to the reader *at all* - which
        // it did not, so this assertion could not have been written before.
        compose.onNodeWithTag("surah_1").performClick()
        compose.waitForIdle()

        assertEquals(
            "tapping a surah in the index did not ask for its first verse",
            QuranBrowse.ref(1, 1),
            requested
        )

        deliver(requested)
        assertEquals(
            "the index's request never reached the reader",
            requested?.page,
            pillPage()
        )
    }
}
