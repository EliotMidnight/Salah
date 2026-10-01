package com.example.ui.quran.reader

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.QuranScrollDirection
import com.example.data.quran.QuranBrowse
import com.example.ui.quran.VerseActions
import com.example.ui.theme.Space

/**
 * The mushaf as a page turner: one canonical page at a time, on either axis.
 *
 * ### A pager, and why not a list
 *
 * The previous vertical layout was a `LazyColumn` of all 604 pages, which meant a
 * swipe *scrolled across a boundary* rather than turning anything, three pages sat
 * half-visible in the viewport at all times, and "which page am I on" became a
 * question the reader could only answer by scrolling back to find the seam. All
 * three are what the printed mushaf is not. Both axes are a pager here: a vertical
 * swipe turns forward, a horizontal one turns sideways, and never more than one
 * page is on screen.
 *
 * ### The pager writes the position, and the position writes the pager
 *
 * Two directions, both declared here, both guarded on inequality so neither
 * re-fires on the other's write and the two cannot loop.
 *
 * Getting this wrong is what produced the module's most-reported bug.
 * `rememberPagerState` reads `initialPage` **once**, at composition, and ignores it
 * for the rest of its life - so a separate `LaunchedEffect` was re-seeding a page
 * cursor on every anchor change. Picking a surah from the index moved the number
 * in the pill and nothing else: the pager stayed where it was, and the pill
 * reported a page the reader was not looking at.
 *
 * ### A page turn records the page's own first verse
 *
 * Turning to page 3 writes `placeAtPage(3).verse`, which is 2:6 - the first verse
 * *on page 3*. The previous implementation wrote the first verse of the reader's
 * surah, so turning from page 2 to page 3 recorded 2:1, and "continue reading"
 * pointed back at page 2 forever. That is the bug the whole `ReaderPosition` type
 * exists to make unrepresentable.
 *
 * ### No tap gutters
 *
 * The previous horizontal axis carried two 48dp invisible strips that turned the
 * page. They cost 2 x 48dp of the reading area, existed on the horizontal axis
 * only - so a screen-reader user had no route to the next page at all - and existed
 * because the alternative was a full-page tap handler that would have made turning
 * a page and selecting a verse mutually exclusive.
 *
 * Page turns are the pager's drag, and an accessibility action on the page itself.
 * That costs no reading space, works on both axes, and does not compete with the
 * tap that selects a verse.
 */
@Composable
internal fun MushafPager(
    pagerState: PagerState,
    position: ReaderPosition,
    orientation: QuranScrollDirection,
    requestedScale: Float,
    ink: Color,
    accent: Color,
    onSelectVerse: (QuranRef) -> Unit,
    actionsFor: (Ayah) -> VerseActions,
    options: QuranReadingOptions,
    modifier: Modifier = Modifier,
    /** Whether the reader's control row is on screen, and so taking page space. */
    controlsVisible: Boolean = true
) {
    val space = Space.current
    // The room a page leaves for the reader's floating chrome.
    //
    // Passed in, because the reader knows whether the chrome is on screen and a
    // page cannot: in immersive mode there is no control row, and a page that
    // reserved the space anyway would lose height for a bar nobody can see.
    val topInset = PageInsets.top(controlsVisible)

    // The selected verse, narrowed to the page being drawn. Checked against the page
    // on *every* page rather than once here, because a selection on page 604 is not
    // a selection on page 603 - and ayah 1 exists on both.
    val selected = position.selection?.takeIf { it.page == position.page }

    // The page the pager is *showing*, which during a drag is not the page it is
    // turning to. The action bar follows the settled page rather than the position,
    // so it does not appear on a page the reader is halfway through leaving.
    val selectedPageNumber = pagerState.settledPage + 1

    // The pager writes the position. Keyed on the pager, so this is one collection
    // for the life of the surface rather than one per turn.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { index ->
                val page = index + 1
                if (page != position.page) {
                    position.turnTo(QuranBrowse.placeAtPage(page).verse)
                }
            }
    }

    // The position writes the pager.
    LaunchedEffect(position.page) {
        val target = (position.page - 1).coerceIn(0, QuranBrowse.TOTAL_PAGES - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (orientation == QuranScrollDirection.HORIZONTAL) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = space.lg,
                contentPadding = PaddingValues(horizontal = space.sm)
            ) { index ->
                PageAt(index + 1, requestedScale, ink, accent, selected, onSelectVerse, topInset)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = space.lg,
                contentPadding = PaddingValues(vertical = space.sm)
            ) { index ->
                PageAt(index + 1, requestedScale, ink, accent, selected, onSelectVerse, topInset)
            }
        }

        // The selected verse's actions, over the foot of the page.
        //
        // A tap on a mushaf page *selects* a verse, and a selection that cannot be
        // acted on is a dead end: the reader has no route to bookmark, copy, share or
        // play from the text they are looking at. The previous mushaf surface had no
        // actions at all - the row existed only in the continuous layouts - so
        // selecting a verse on a page did nothing a reader could see.
        //
        // At the foot rather than beside the verse, and in the scroll container's
        // coordinate space: a page is a fixed object, so there is no "below the
        // verse" to put a panel in without either covering the text or pushing the
        // page out of shape. The foot is the only place on a page that is not text.
        //
        // It is deliberately *not* pinned to the bottom of the viewport, because the
        // pager is the parent: an overlay would be re-composed on every drag and
        // would sit outside the page's own padding.
        position.selection?.let { ref ->
            if (selectedPageNumber == pagerState.currentPage + 1) {
                ayahOnPageOrNull(ref)?.let { ayah ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = space.lg)
                            .padding(bottom = space.lg)
                    ) {
                        PageActionBar(
                            ayah = ayah,
                            actions = actionsFor(ayah),
                            onDismiss = { position.clearSelection() },
                            showTranslation = options.showTranslation,
                            translationScale = options.translationScale
                        )
                    }
                }
            }
        }
    }
}

/** The verse a reference names, if it exists. */
private fun ayahOnPageOrNull(ref: QuranRef): com.example.data.model.Ayah? =
    QuranBrowse.ayah(ref.surah, ref.ayah)

/** One page, and a tap on it resolves to a verse on the page it is actually on. */
@Composable
private fun PageAt(
    page: Int,
    requestedScale: Float,
    ink: Color,
    accent: Color,
    selected: QuranRef?,
    onSelectVerse: (QuranRef) -> Unit,
    topInset: androidx.compose.ui.unit.Dp
) {
    // A page's own surah, needed to resolve a tap into a full reference - and a
    // page can hold three surahs, so this cannot be read off the reader's position.
    val firstSurah = QuranBrowse.ayahsOnPage(page).firstOrNull()?.surahNumber ?: return

    MushafPage(
        pageNumber = page,
        requestedScale = requestedScale,
        // The selection is passed down rather than held here, because a selection
        // is a property of the *reader* - it survives a page turn only if the new
        // page still contains it, and that is a question about the position, not
        // about any one page.
        selected = selected,
        ink = ink,
        accent = accent,
        onSelectVerse = { ayahNumber ->
            onSelectVerse(QuranRef(firstSurah, ayahNumber, page))
        },
        topInset = topInset,
        modifier = Modifier.fillMaxSize()
    )
}
