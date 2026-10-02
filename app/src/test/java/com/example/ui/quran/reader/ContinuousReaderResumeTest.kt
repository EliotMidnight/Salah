package com.example.ui.quran.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.QuranRef
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.data.quran.QuranBrowse
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.VerseActions
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Entering the continuous layout has to land on the reader's verse.
 *
 * ### Why this is a test and not a screenshot
 *
 * The bug was that the list opened at item 0 whatever the reader's position was,
 * and then the scroll observer - which debounces - wrote that position to storage
 * half a second later. So the wrong place was not merely *shown*, it was
 * *remembered*, and a screenshot cannot see the remembering. It can only see that
 * the wrong text is on screen, which is the smaller half of it.
 *
 * So this drives the real composable with a list state it owns, and asserts three
 * separate things: where the list opens, that the observer did not write a position
 * on the way, and that the two layouts agree on where "here" is.
 *
 * ### Why the list state is passed in
 *
 * `ContinuousReader` takes its `LazyListState` as a parameter - the reader passes
 * `rememberLazyListState()` - so the test can hold the same object the composable
 * scrolls and ask it where it went. Asserting on the composable's internals would
 * be a test that has to be rewritten every time they move; asserting on the state
 * the caller passed in is a test about the contract between them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContinuousReaderResumeTest {

    @get:Rule val composeTestRule = createComposeRule()

    /** What the observer wrote, if anything. */
    private var reported: QuranRef? = null

    private fun options(
        layout: QuranReadingLayout = QuranReadingLayout.CONTINUOUS,
        perVerse: Boolean = false,
        scroll: QuranScrollDirection = QuranScrollDirection.VERTICAL
    ) = QuranReadingOptions(
        layout = layout,
        perVerse = perVerse,
        scroll = scroll,
        pinchTarget = QuranPinchTarget.TEXT_SIZE
    )

    /**
     * Render the continuous reader at [ref] and report where the list opened.
     *
     * @return the first item index on screen once the surface has settled.
     */
    private fun openAt(
        ref: QuranRef,
        options: QuranReadingOptions
    ): Int {
        val surah = QuranBrowse.surah(ref.surah)!!
        val ayahs = QuranBrowse.ayahsInSurah(ref.surah)
        val position = ReaderPosition(ref)
        lateinit var listState: androidx.compose.foundation.lazy.LazyListState

        composeTestRule.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    listState = rememberLazyListState()
                    ContinuousReader(
                        surah = surah,
                        ayahs = ayahs,
                        listState = listState,
                        position = position,
                        options = options,
                        ink = androidx.compose.ui.graphics.Color.Black,
                        accent = androidx.compose.ui.graphics.Color.Blue,
                        muted = androidx.compose.ui.graphics.Color.Gray,
                        actionsFor = { VerseActions(ayah = it) },
                        onPositionSettled = { reported = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        return listState.firstVisibleItemIndex
    }

    @Test
    fun `flowing text opens on the block holding the reader's verse`() {
        // 2:200 is in block 16 of 24 (0-based), because blocks are twelve verses.
        // Opening at the *verse* index instead - 199 - would land twelve blocks
        // further on, which on a 286-verse surah is anywhere at all.
        val ref = QuranRef(2, 200, QuranBrowse.pageOf(2, 200))
        val index = openAt(ref, options())
        val expectedBlock = 199 / 12
        assertEquals(
            "flowing text opened at block $index, expected block $expectedBlock",
            expectedBlock,
            index
        )
    }

    @Test
    fun `per-verse text opens on the reader's verse`() {
        // One past the heading, so the heading is off-screen and the verse is at the
        // top. Getting the offset wrong by the heading count is a one-row error that
        // looks like nothing on screen.
        val ref = QuranRef(2, 200, QuranBrowse.pageOf(2, 200))
        val index = openAt(ref, options(perVerse = true))
        assertEquals(
            "per-verse text opened at item $index, expected 2:200's row",
            200,
            index
        )
    }

    @Test
    fun `opening the surface writes no position at all`() {
        // The half a screenshot cannot see.
        //
        // The observer debounces and then reports whatever is at the top of the
        // viewport. If the list opens *at* the position, the first emission is the
        // position itself and reporting it writes a row the reader did not choose -
        // which on a layout switch is how 2:1 became the saved position for a reader
        // who was at 2:200.
        val ref = QuranRef(2, 200, QuranBrowse.pageOf(2, 200))
        openAt(ref, options())
        assertEquals(
            "entering the surface reported ${reported?.toString()}, which is the " +
                "position it already had",
            null,
            reported
        )
    }

    @Test
    fun `the first verse of a surah opens on the heading`() {
        // Index 0, because there is nothing to scroll to: the heading and 2:1 are
        // already what the surface shows. Asserted so the "open at the reader's
        // verse" rule cannot be satisfied by always scrolling to some fixed offset.
        val ref = QuranRef(2, 1, 2)
        assertEquals(0, openAt(ref, options()))
    }

    @Test
    fun `a position outside this surah opens at the top rather than refusing`() {
        // The position and the surah can disagree - a stale ref, a corpus change, or
        // a surah that no longer contains the verse. There is nothing to scroll to,
        // and the reader must still get a page of text rather than an error.
        val ref = QuranRef(2, 200, 42)
        val surah = QuranBrowse.surah(3)!!
        val ayahs = QuranBrowse.ayahsInSurah(3)
        val position = ReaderPosition(ref)
        lateinit var listState: androidx.compose.foundation.lazy.LazyListState

        composeTestRule.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    listState = rememberLazyListState()
                    ContinuousReader(
                        surah = surah,
                        ayahs = ayahs,
                        listState = listState,
                        position = position,
                        options = options(),
                        ink = androidx.compose.ui.graphics.Color.Black,
                        accent = androidx.compose.ui.graphics.Color.Blue,
                        muted = androidx.compose.ui.graphics.Color.Gray,
                        actionsFor = { VerseActions(ayah = it) },
                        onPositionSettled = { reported = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(
            "a position from another surah left the list scrolled somewhere",
            0,
            listState.firstVisibleItemIndex
        )
    }

    @Test
    fun `the block a verse is in is the block its index says`() {
        // The arithmetic behind the resume, on its own so a wrong divisor is a
        // one-line failure rather than a scroll position three screens out.
        val ayahs = QuranBrowse.ayahsInSurah(2)
        val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
        assertEquals(24, blocks.size)
        for (verseIndex in listOf(0, 11, 12, 23, 24, 199, 285)) {
            assertEquals(
                "verse index $verseIndex is not in block ${verseIndex / 12}",
                verseIndex / 12,
                flowIndexOf(verseIndex, blocks)
            )
            assertTrue(
                "verse index $verseIndex is not in the block it resolved to",
                verseIndex in globalRangeOf(blocks).elementAt(flowIndexOf(verseIndex, blocks))
            )
        }
    }

    @Test
    fun `the block arithmetic is right for every verse of every surah`() {
        // The resume position *is* this function, so one sampled surah at seven indices
        // leaves the whole shape of the corpus untested: a surah shorter than a block, a
        // surah whose last block is short, a surah whose verse count is an exact multiple
        // of the block size. Those are the cases a divisor gets wrong, and each is a
        // different wrong answer - too far, one past the end, or off by one block.
        //
        // 6,236 assertions, which is nothing, against a resume that silently lands a
        // reader on a blank sheet.
        for (surah in 1..114) {
            val ayahs = QuranBrowse.ayahsInSurah(surah)
            val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
            val ranges = globalRangeOf(blocks)

            for (verseIndex in ayahs.indices) {
                val expected = verseIndex / FLOW_BLOCK_VERSES
                assertEquals(
                    "verse ${surah}:${ayahs[verseIndex].ayahNumber} (index " +
                        "$verseIndex of ${ayahs.size}) resolved to the wrong block",
                    expected,
                    flowIndexOf(verseIndex, blocks)
                )
                assertTrue(
                    "verse ${surah}:${ayahs[verseIndex].ayahNumber} resolved to a " +
                        "block that does not contain it",
                    verseIndex in ranges[flowIndexOf(verseIndex, blocks)]
                )
            }

            // And the last verse of every surah lands in the *last* block rather than
            // one past it, which is the case the counted arithmetic exists for.
            if (ayahs.isNotEmpty()) {
                assertEquals(
                    "the last verse of surah $surah did not land in the last block",
                    blocks.lastIndex,
                    flowIndexOf(ayahs.lastIndex, blocks)
                )
            }
        }
    }

    @Test
    fun `every surah shorter than one block is a single block`() {
        // Derived from the corpus rather than listed, because a hand-written list of
        // surah numbers is exactly the kind of fact that rots: the first version of this
        // test named Al-Alaq as one of the shortest surahs, and it has nineteen verses.
        val short = (1..114)
            .map { it to QuranBrowse.ayahsInSurah(it) }
            .filter { (_, ayahs) -> ayahs.size < FLOW_BLOCK_VERSES }

        assertTrue(
            "no surah in the book is shorter than a block, which cannot be right",
            short.isNotEmpty()
        )
        assertEquals(
            "Al-Kawthar is three verses and should be the shortest in the book",
            3,
            short.minOf { (_, ayahs) -> ayahs.size }
        )

        for ((number, ayahs) in short) {
            val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
            assertEquals(
                "surah $number has ${ayahs.size} verses, which is fewer than a " +
                    "block, so it must be one block and not ${blocks.size}",
                1,
                blocks.size
            )
            for (verseIndex in ayahs.indices) {
                assertEquals(
                    "verse $verseIndex of surah $number did not resolve to its only block",
                    0,
                    flowIndexOf(verseIndex, blocks)
                )
            }
        }
    }

    @Test
    fun `an out-of-range index resolves to the last block rather than past the end`() {
        // A `scrollToItem` past the end of a list is how a reader ends up looking at a
        // blank sheet instead of at the text they asked for, so this is clamped rather
        // than trusted.
        val blocks = QuranBrowse.ayahsInSurah(2).chunked(FLOW_BLOCK_VERSES)
        assertEquals(blocks.lastIndex, flowIndexOf(9_999, blocks))
        assertEquals(blocks.lastIndex, flowIndexOf(Int.MAX_VALUE, blocks))
        assertEquals(0, flowIndexOf(-1, blocks))
        assertEquals(0, flowIndexOf(Int.MIN_VALUE, blocks))

        val empty = emptyList<List<com.example.data.model.Ayah>>()
        assertEquals(
            "an empty flow resolved to something other than 0",
            0,
            flowIndexOf(0, empty)
        )
        assertEquals(0, flowIndexOf(7, empty))
    }

    /**
     * Each block's verse indices in the surah's own numbering.
     *
     * `blocks[b].indices` are indices *within* block `b`, which is the trap: asking
     * whether global verse 12 is in `0..11` is a question about the wrong coordinate
     * space, and the answer is false for every verse past the first block while
     * looking like a real failure of the resume.
     */
    private fun globalRangeOf(blocks: List<List<com.example.data.model.Ayah>>):
        List<IntRange> {
        var start = 0
        return blocks.map { block ->
            val range = start until (start + block.size)
            start += block.size
            range
        }
    }

    @Test
    fun `a reader arriving at a page and a reader arriving at a verse agree`() {
        // The claim the whole resume exists for, at the boundary where the two
        // layouts meet: a mushaf page turn writes the page's first verse, and
        // switching to continuous must land on exactly that verse.
        for (page in intArrayOf(2, 42, 106, 293, 604)) {
            val fromPage = QuranBrowse.placeAtPage(page).verse
            val ayahs = QuranBrowse.ayahsInSurah(fromPage.surah)
            val verseIndex = ayahs.indexOfFirst {
                it.surahNumber == fromPage.surah && it.ayahNumber == fromPage.ayah
            }
            assertTrue(
                "page $page's first verse is not in its own surah",
                verseIndex >= 0
            )
            val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
            val block = flowIndexOf(verseIndex, blocks)
            assertTrue(
                "page $page resumes onto block $block of ${blocks.size}",
                block in blocks.indices
            )
            // And the verse really is inside that block, or "the verse the page turned
            // to" and "the block it resumes onto" do not describe the same text - which
            // is the failure that shows as landing three screens away and looking
            // plausible.
            var start = 0
            blocks.forEachIndexed { index, size ->
                if (index == block) {
                    assertTrue(
                        "page $page's first verse is not inside block $block " +
                            "($start..${start + size.size})",
                        verseIndex in start until (start + size.size)
                    )
                }
                start += size.size
            }
        }
    }

    @Test
    fun `the last verse of a surah is in the last block`() {
        // The short-last-block case, and the one a plain division gets wrong. Al-Baqarah
        // is 286 verses: 23 full blocks of twelve and one of ten. Dividing gives 23.83,
        // which rounds to 24 - one past the end of a 24-item list, so the scroll lands
        // on nothing.
        val ayahs = QuranBrowse.ayahsInSurah(2)
        val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
        assertEquals(286, ayahs.size)
        assertEquals(24, blocks.size)
        assertEquals(10, blocks.last().size)
        assertEquals(
            "the last verse resolved past the end of the list",
            blocks.lastIndex,
            flowIndexOf(ayahs.lastIndex, blocks)
        )
        // And a verse index beyond the list clamps rather than scrolling off it.
        assertEquals(
            "an out-of-range verse index did not clamp to the last block",
            blocks.lastIndex,
            flowIndexOf(9999, blocks)
        )
    }
}
