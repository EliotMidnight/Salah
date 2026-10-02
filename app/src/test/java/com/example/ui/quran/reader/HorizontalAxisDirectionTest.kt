package com.example.ui.quran.reader

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.quran.QuranBrowse
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.QuranScrollDirection
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.QuranReader
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sideways axes advance the way Arabic is read.
 *
 * ### The defect
 *
 * Two things, and only one of them was visible in a screenshot.
 *
 * **The horizontal axis inherited the interface language.** A `HorizontalPager` takes its
 * direction from the composition, so an Arabic reader swiped right-to-left and an English,
 * French or Bengali reader swiped **left-to-right through an Arabic mushaf**. The text
 * inside each page was already forced RTL, so the reading ran right-to-left inside a
 * carousel that ran left-to-right — advancing a page moved the reader *backwards*.
 *
 * **The continuous horizontal axis laid a verse out as one 720dp line.** It was a
 * `horizontalScroll` around a column given a fixed wide measure, and the comment above it
 * called the overflow correct. It was the report: *the whole sentence on one line,
 * exceeding the screen*.
 *
 * ### Why the first needs a gesture test
 *
 * The pager renders identically whichever way it runs — a page of mushaf looks the same
 * left-to-right or right-to-left, because the text inside forces its own direction. A
 * screenshot cannot distinguish them, and a screenshot was checked and looked right while
 * the axis was backwards. So this asserts the gesture: in a right-to-left axis, dragging
 * **rightward** is what advances, and dragging leftward goes back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HorizontalAxisDirectionTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * @param perVerse which continuous variant; the per-page axis ignores it.
     */
    private fun reader(options: QuranReadingOptions, language: String = "English") {
        compose.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = language) {
                    QuranReader(
                        state = SalahUiState(),
                        position = ReaderPosition(QuranRef(2, 120, 42)),
                        options = options,
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
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
    }

    private fun screenWidthPx(): Float =
        compose.onNodeWithTag("quran_reader", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.right

    /** Which mushaf pages are currently composed. */
    private fun composedPages(): List<Int> {
        val found = ArrayList<Int>()
        for (n in 36..50) {
            if (compose.onAllNodes(hasTestTag("mushaf_page_$n")).fetchSemanticsNodes().isNotEmpty()) {
                found += n
            }
        }
        return found
    }

    /**
     * Swipe the pager node itself rather than the reader.
     *
     * A drag sent to the reader's own node is consumed by the tap-to-select gesture that
     * wraps the surface, so the pager never sees it - which looks exactly like a pager that
     * does not respond. Targeting the pager is also what a reader's finger reaches.
     */
    private fun swipePagerRight() {
        compose.onNodeWithTag("horizontal_flow_pager")
            .performTouchInput { swipeRight() }
        compose.waitForIdle()
    }

    private fun swipePagerLeft() {
        compose.onNodeWithTag("horizontal_flow_pager")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
    }

    private fun swipeReaderRight() {
        compose.onNodeWithTag("quran_reader", useUnmergedTree = true)
            .performTouchInput { swipeRight() }
        compose.waitForIdle()
    }

    private fun swipeReaderLeft() {
        compose.onNodeWithTag("quran_reader", useUnmergedTree = true)
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
    }

    /** The first block's node - the vertical list composes several. */
    private fun firstBlockWidth(): Float =
        compose.onAllNodesWithTag("flowing_block_text", useUnmergedTree = true)[0]
            .fetchSemanticsNode().boundsInRoot.width

    // --- The gesture is the whole claim ---------------------------------------

    /**
     * The per-page axis advances on a rightward drag, in an English interface.
     *
     * Which gesture is which was **measured**, not assumed: page 1 sits rightmost, so
     * reaching page 2 moves the content rightward, which is a rightward drag. Before the
     * fix this axis followed the interface language, so an English reader dragged
     * *leftward* to advance and was moving backwards through the Book.
     *
     * A screenshot cannot catch that. The pager renders identically whichever way it runs,
     * because the text inside forces its own direction - and a screenshot was checked and
     * looked right while the axis was backwards.
     */
    @Test
    fun `the per-page axis advances on a rightward drag, in an English interface`() {
        reader(QuranReadingOptions(scroll = QuranScrollDirection.HORIZONTAL))
        assertTrue("page 42 is not composed to begin with", composedPages().contains(42))

        swipeReaderRight()
        assertTrue(
            "a rightward drag did not advance the per-page axis; composed pages were " +
                "${composedPages()}. On a right-to-left axis the first page is rightmost, " +
                "so advancing moves the content rightward.",
            composedPages().contains(43)
        )

        swipeReaderLeft()
        assertTrue(
            "a leftward drag did not go back; composed pages were ${composedPages()}",
            composedPages().contains(42) || composedPages().contains(41)
        )
    }

    @Test
    fun `the continuous axis advances on a rightward drag too`() {
        reader(
            QuranReadingOptions(
                layout = QuranReadingLayout.CONTINUOUS,
                scroll = QuranScrollDirection.HORIZONTAL
            )
        )
        // The reader is at 2:120, which is block 10 of Al-Baqarah at twelve a block, so
        // block 11 opens on 2:132. A verse that can only be on screen after advancing.
        val afterBlock = QuranBrowse.ayah(2, 132)?.textArabic?.take(12).orEmpty()
        assertTrue("the fixture is wrong: 2:132 has no text", afterBlock.isNotBlank())

        assertTrue(
            "2:132 is already composed, so the reader did not start where it claims",
            !compose.onAllNodesWithText(afterBlock, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
        swipePagerRight()
        assertTrue(
            "a rightward drag did not advance the continuous axis onto the next block",
            compose.onAllNodesWithText(afterBlock, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    // --- The overflow ---------------------------------------------------------

    /**
     * A verse is not one line running off both sides of the screen.
     *
     * The old measure was a fixed 720dp on a screen far narrower than that, so a line of
     * Arabic ran past both edges and had to be dragged sideways to read - the report this
     * replaced.
     *
     * Asserted on the rendered node rather than on the absence of a constant, because a
     * missing constant proves nothing about the layout and a node's width does.
     */
    @Test
    fun `the continuous horizontal text fits the screen it is drawn on`() {
        reader(
            QuranReadingOptions(
                layout = QuranReadingLayout.CONTINUOUS,
                scroll = QuranScrollDirection.HORIZONTAL
            )
        )
        val width = firstBlockWidth()
        val screen = screenWidthPx()
        assertTrue(
            "the text is ${width}px wide on a ${screen}px screen, so a verse is " +
                "still laid out as one line running past both edges",
            width <= screen + 1f
        )
    }

    @Test
    fun `the vertical continuous axis is not a wide measure either`() {
        // The same layout on the other axis, as a control: if this failed too, the width
        // assertion above would be measuring the screen rather than the behaviour.
        reader(QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS))
        val width = firstBlockWidth()
        assertTrue(
            "the vertical block is ${width}px wide",
            width <= screenWidthPx() + 1f
        )
    }
}
