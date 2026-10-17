package com.example.ui.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.QuranScrollDirection
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    /**
     * Whether the destination is composed.
     *
     * The destination lives inside a [androidx.compose.runtime.saveable.SaveableStateHolder],
     * the same arrangement `NavHost` uses for a destination kept on the back stack
     * with `saveState` - so toggling this leaves the destination and comes back the
     * way a tab switch does, restoring everything `rememberSaveable` held.
     */
    private var mounted by mutableStateOf(true)

    private fun mount(language: String = "English") {
        compose.setContent {
            SalahTheme {
                ProvideAppLanguage(language = language) {
                    val holder = rememberSaveableStateHolder()
                    if (mounted) {
                        holder.SaveableStateProvider("quran") {
                            QuranScreen(
                                state = state.value,
                                onOpen = { requested = it },
                                onOpenConsumed = {
                                    state.value = state.value.copy(pendingOpen = null)
                                },
                                onAyahViewed = {},
                                onToggleBookmark = {},
                                onTogglePlayAyah = {},
                                onStopAudio = {},
                                onCycleSpeed = {},
                                onCycleRepeat = {},
                                onOptionsChange = {},
                                onImmersiveChange = {}
                            )
                        }
                    }
                }
            }
        }
        awaitReader()
    }

    /**
     * Leave the destination and come back.
     *
     * `LaunchedEffect`s run again from scratch on the way back, while
     * `rememberSaveable` state - the reader's own position among it - is
     * restored. That is the shape of the re-entry bug: a request still set in
     * the ViewModel was re-delivered by the fresh effect on every return.
     */
    private fun remount() {
        mounted = false
        compose.waitForIdle()
        mounted = true
        awaitReader()
    }

    private fun awaitReader() {
        // The destination loads the corpus before it shows the reader (off the
        // main thread, once per process), so the reader is what to wait for.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("reader_index").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** What `MainActivity` does in response to a request. */
    private fun deliver(ref: QuranRef?) {
        state.value = state.value.copy(pendingOpen = ref)
        compose.waitForIdle()
    }

    /**
     * What the control pill reads, as a reader sees it.
     *
     * Read from the text a reader sees, not from the accessibility label: the two are
     * built from the same values here, and a test that reached past the rendered text
     * would go on passing if the label drifted from it. Read from *the pill*, not by
     * searching all the text on screen: the flowing layout prints page rules among
     * the verses, and a global search finds those first.
     */
    private fun pillText(): String {
        val pill = compose.onNodeWithTag("reader_index")
            .assertIsDisplayed()
            .fetchSemanticsNode()
        return pill.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            ?: "the control pill's page label has no text"
    }

    /** The page the control pill is reporting. */
    private fun pillPage(): Int {
        val text = pillText()
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
        // The surah's name is on the pill, which is where the reader reads it: the
        // mushaf page itself is Arabic and says nothing in English. The page begins
        // inside Al-Isra, so the pill names the page's two surahs, the requested one
        // last.
        assertEquals(
            "the pill should name the page the reader was sent to",
            "Al-Isra to Al-Kahf Page 293",
            pillText()
        )
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

        // Each mushaf surface tags the page it draws with that page's number -
        // the corpus's on the flowing pager, the print's on the Madinah sheet -
        // so the node is asked for under the tag of whichever surface the
        // reader's own options have put on screen.
        val surfaceTag = if (state.value.quranReadingOptions.layout == QuranReadingLayout.MADINAH) {
            "madinah_page"
        } else {
            "mushaf_page"
        }
        compose.onNodeWithTag("${surfaceTag}_${ref.page}").assertIsDisplayed()
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
        // Al-Ikhlas, Al-Falaq and An-Nas; the pill names the page's range - both its
        // ends, the requested surah among them.
        assertEquals(
            "the last page's pill should name the page's surahs",
            "Al-Ikhlas to An-Nas Page 604",
            pillText()
        )
    }

    @Test
    fun `the pill names the surahs the page holds, not the one the reader is standing in`() {
        // The pill sits beside the page number, where one name reads as the page's own
        // name - and a page is not inside one surah. Page 106 ends An-Nisa and carries
        // the start of Al-Ma'idah, and the pill named whichever of the two the reader's
        // verse happened to be in, so the page's name changed as they scrolled.
        val ref = QuranBrowse.ref(4, 176)!!
        assertEquals(
            "fixture: 4:176 is no longer on page 106, so this is not the case that " +
                "was broken",
            106,
            ref.page
        )

        mount()
        // A page inside one surah is named once: a range against itself is not what a
        // reader wants to read.
        assertEquals(
            "the first page's pill should read its one surah",
            "Al-Fatihah Page 1",
            pillText()
        )

        deliver(ref)

        assertEquals(
            "page 106 holds An-Nisa and Al-Ma'idah",
            "An-Nisa to Al-Ma'idah Page 106",
            pillText()
        )
    }

    @Test
    fun `the Arabic interface names a surah in Arabic on the pill and in the recitation banner`() {
        // The two surfaces in the reader that print a surah's *name*: the pill beside
        // the page number, and the banner that says what is being recited. Both named
        // in transliteration in every language, so an Arabic reader reading an Arabic
        // mushaf was told where they were in Latin letters.
        mount(language = "Arabic")

        assertEquals(
            "the pill does not name its page's surah in Arabic",
            "الفاتحة صفحة 1",
            pillText()
        )

        // The banner reports the *audio*'s surah, which this test puts on a page the
        // reader is not on, so the two names cannot come from one another.
        state.value = state.value.copy(
            isAudioPlaying = true,
            currentAudioRef = QuranBrowse.ref(2, 255)!!,
            currentAudioReciter = "Alafasy"
        )
        compose.waitForIdle()
        compose.onNodeWithText("يتلو البقرة 2:255 · Alafasy")
            .assertExists("the recitation banner does not name its surah in Arabic")
            .assertIsDisplayed()
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

    @Test
    fun `an honoured request is emptied, so a return to the destination cannot drag the reader back`() {
        // The defect this guards: `pendingOpen` was set and never cleared, so the
        // destination's effect - keyed on the request - ran again on every re-entry
        // (a tab switch keeps the destination through `saveState`). A reader who
        // arrived from the index and read on was pulled back to the index's verse
        // on their next visit to the tab - and the debounced position writer saved
        // the stale place, so the wrong page outlived the session.
        val kahf = QuranBrowse.ref(18, 1)!!
        mount()
        deliver(kahf)
        assertEquals("the request did not reach the reader", 293, pillPage())
        assertNull(
            "the honoured request stayed in the mailbox, so every re-entry into " +
                "the destination re-delivers it",
            state.value.pendingOpen
        )

        // The reader moves on by their own hand: the page-turn action the page
        // exposes to accessibility, which drives the same `turn` a swipe and the
        // pager share.
        nextPage()
        val moved = pillPage()
        assertNotEquals("fixture: the page turn did not move the reader", 293, moved)

        // And switches to the flowing text. Which surface the reader is on decides
        // whether a replayed request is *visible*: the pager pushes its page into
        // the position on its first collection, so it shrugs the stale request off;
        // the list takes its seating from the position and drops its opening
        // emission, so a stale request drags the reader - and then the list follows
        // them there. That is the reader this guards.
        state.value = state.value.copy(
            quranReadingOptions = QuranReadingOptions(
                layout = QuranReadingLayout.CONTINUOUS,
                perVerse = true,
                scroll = QuranScrollDirection.VERTICAL
            )
        )
        compose.waitForIdle()
        assertEquals("the layout switch lost the reader's place", moved, pillPage())

        // Leave the destination and come back, the way a tab switch does.
        remount()

        assertEquals(
            "a request that had already been honoured was delivered again on re-entry " +
                "and dragged the reader back",
            moved,
            pillPage()
        )
    }

    /** Turn the page through the page's own accessibility action. */
    private fun nextPage() {
        val page = pillPage()
        // The page-turn actions ride on the page's node - the same node that
        // carries the page's state description - and the pager composes its
        // neighbours, so the right node is picked by what it says it is. The
        // flowing pager carries that contract on the page's text node; the
        // Madinah sheet carries it on the sheet's own root.
        val node = (
            compose.onAllNodesWithTag("mushaf_page_text", useUnmergedTree = true)
                .fetchSemanticsNodes() +
                compose.onAllNodesWithTag("madinah_page_$page", useUnmergedTree = true)
                    .fetchSemanticsNodes()
            ).first {
                it.config.getOrNull(SemanticsProperties.StateDescription) == "Page $page"
            }
        val turn = node.config[SemanticsActions.CustomActions]
            .first { it.label == "Next page" }
            .action
        compose.runOnIdle { turn() }
        compose.waitForIdle()
    }
}
