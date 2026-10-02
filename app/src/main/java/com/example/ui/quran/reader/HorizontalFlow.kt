package com.example.ui.quran.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.Surah
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.ui.theme.Space
import androidx.compose.ui.platform.testTag

/**
 * The continuous surah, read sideways.
 *
 * ### Why right-to-left
 *
 * Arabic is read right to left, so on this axis the first block sits at the **right** and
 * advancing moves leftward. The override is scoped to this pager rather than applied to the
 * reader, because an English or French interface is still left to right everywhere else -
 * only this axis is reading order for the Book.
 *
 * ### One block per screen
 *
 * The blocks are the same twelve-verse groups the vertical axis scrolls through, and
 * `flowIndexOf` already maps the reader's verse onto one, so a reader who switches axes
 * arrives at the same words rather than at the top of the surah.
 *
 * The surah heading sits above the pager rather than being a page of its own, so it stays
 * put while the reader moves, which is what a heading is for. A mushaf has it in the margin
 * for the same reason.
 */
@Composable
internal fun HorizontalFlow(
    surah: Surah,
    blocks: List<List<Ayah>>,
    initialBlock: Int,
    selected: QuranRef?,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    onSelectVerse: (Ayah) -> Unit,
    onSettled: (List<Ayah>) -> Unit,
    /**
     * Room for the reader's chrome, above the heading.
     *
     * The vertical axis gets this from its list's *content padding* rather than from the
     * heading, and passes `0.dp` for exactly that reason. This axis has no list, so
     * nothing else carries it and the heading would sit under the control pill.
     */
    topInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val pageCount = blocks.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = initialBlock.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    // Write the reader's position back as blocks settle, debounced — the same reason the
    // vertical list does: the observer fires on every scroll frame and a position saved on
    // a frame is a position saved mid-swipe.
    LaunchedEffect(pagerState, blocks) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .debounce(SCROLL_SETTLE_MILLIS)
            .filter { it in blocks.indices }
            .collect { onSettled(blocks[it]) }
    }

    Column(modifier = modifier) {
        SurahHeading(
            surah = surah,
            ink = ink,
            muted = muted,
            topInset = topInset,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(space.sm))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = space.lg)
                    .testTag("horizontal_flow_pager"),
                pageSpacing = space.md,
                verticalAlignment = Alignment.Top
            ) { index ->
                blocks.getOrNull(index)?.let { block ->
                    FlowingBlock(
                        ayahs = block,
                        selected = selected,
                        options = options,
                        ink = ink,
                        accent = accent,
                        onSelectVerse = onSelectVerse
                    )
                }
            }
        }
    }
}