package com.example.ui.quran.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.QuranScrollDirection
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.ui.localization.LocalStrings
import com.example.ui.quran.VerseActions
import com.example.ui.quran.VerseArabic
import com.example.ui.quran.VerseReferenceChip
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The whole surah as one flow, on either axis.
 *
 * ### What this layout is
 *
 * A surah read straight through, with no page breaks - which is what reading
 * straight through feels like, and the reason it exists alongside the mushaf
 * rather than instead of it.
 *
 * ### The axis is a mechanism, never a shape
 *
 * Turning the axis changes **which way the surface travels under the finger** and
 * nothing else. The verses stay in the same order, down the page, the same way
 * round, breaking at the same places.
 *
 * That is worth stating because the earlier implementation got it wrong, and the
 * mistake is easy to repeat: it laid each verse out in its own fixed-width column
 * and put the columns in a `Row`. So turning the axis did not change how you
 * *moved* through the text, it changed what the text *looked like* - a surah that
 * read top to bottom became one that read left to right, one verse per screen,
 * each needing its own pan and its own return. Reading order was destroyed, and
 * the reader had to hold the whole surah in their head as a strip of disconnected
 * panels.
 *
 * So both axes render the same ordered column of verses. Horizontal gives the
 * surface a **wide measure** and a pan container around it: the mechanism is added
 * *around* the reading, never in place of it.
 *
 * ### Why the horizontal measure is a real width
 *
 * The earlier version used `Modifier.fillMaxWidth()` inside a `horizontalScroll`.
 * A `horizontalScroll` measures its child with **infinite** max width, and
 * `fillMaxWidth` resolves to `constraints.minWidth` - which is 0 - when the width
 * is unbounded. So the measure was not "the width of the surface", as the
 * surrounding comment claimed; it was *infinite*, and each verse was set as one
 * unwrapped line. The committed screenshot for this mode shows exactly that: one
 * line running off both edges, cut mid-word, described as correct.
 *
 * So the measure is an explicit [wideMeasure] and the child is measured with
 * `width(measure)`. Below that threshold the axis has nothing to pan across and
 * becomes a no-op, which is the right outcome - a gesture that silently does
 * nothing while appearing to would be worse.
 */
@OptIn(FlowPreview::class)
@Composable
internal fun ContinuousReader(
    surah: Surah,
    ayahs: List<Ayah>,
    listState: LazyListState,
    position: ReaderPosition,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    actionsFor: (Ayah) -> VerseActions,
    onPositionSettled: (QuranRef) -> Unit,
    modifier: Modifier = Modifier,
    /** Whether the reader's control row is on screen, and so taking list space. */
    controlsVisible: Boolean = true
) {
    val space = Space.current
    val horizontal = options.scroll == QuranScrollDirection.HORIZONTAL

    // The verse at the top of the viewport is the reader's position.
    //
    // The list is `[heading, verse…]`, so the index the scroll state reports is one
    // more than the verse index. The previous version indexed straight into the
    // ayah list with it, which put the recorded position one verse ahead of the top
    // of the screen for the whole of the surah - and sent `ayahs[1]` whenever the
    // heading was showing at all.
    LaunchedEffect(listState, surah.number) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(SCROLL_SETTLE_MILLIS)
            .collect { index ->
                ayahs.getOrNull(index - HEADING_ITEMS)?.let { ayah ->
                    onPositionSettled(
                        QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
                    )
                }
            }
    }

    val body: @Composable (Modifier) -> Unit = { measure ->
        LazyColumn(
            state = listState,
            modifier = measure,
            contentPadding = PaddingValues(vertical = space.md)
        ) {
            item(key = HEADING_KEY) {
                // The heading is the reader's first sight of the text, so it has to
                // clear the *whole* floating chrome, control row and all - not just
                // the status bar. In immersive mode the control row is gone, so it
                // is passed down rather than assumed: a heading that sat under the
                // index pill meant the reader could not read the surah's name at
                // exactly the moment they opened it.
                SurahHeading(
                    surah = surah,
                    ink = ink,
                    muted = muted,
                    topInset = PageInsets.top(controlsVisible),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (options.perVerse) {
                itemsIndexed(ayahs, key = { _, ayah -> ayah.ayahNumber }) { index, ayah ->
                    // A page rule before the first verse of each page, so the marker
                    // costs no verse. Comparing against the *previous* verse is what
                    // makes the boundary right where a surah ends mid-page and the
                    // next begins mid-page.
                    if (index > 0 && ayahs[index - 1].pageNumber != ayah.pageNumber) {
                        PageRule(page = ayah.pageNumber)
                    }
                    val ref = QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
                    VerseRow(
                        ayah = ayah,
                        selected = position.selection == ref,
                        options = options,
                        ink = ink,
                        muted = muted,
                        actions = actionsFor(ayah),
                        onSelect = { position.toggleSelection(ref) }
                    )
                }
            } else {
                // Flowing text: one block, and a tap resolves through its layout
                // exactly as a mushaf page's does.
                itemsIndexed(
                    ayahs.chunked(FLOW_BLOCK_VERSES),
                    key = { index, _ -> "flow_$index" }
                ) { index, group ->
                    if (index > 0 && group.first().pageNumber != ayahs
                            .chunked(FLOW_BLOCK_VERSES)[index - 1].first().pageNumber
                    ) {
                        PageRule(page = group.first().pageNumber)
                    }
                    FlowingBlock(
                        ayahs = group,
                        selected = position.selection,
                        options = options,
                        ink = ink,
                        accent = accent,
                        onSelectVerse = { ayah ->
                            ayahs.firstOrNull { it.ayahNumber == ayah.ayahNumber }?.let {
                                position.toggleSelection(
                                    QuranRef(
                                        it.surahNumber,
                                        it.ayahNumber,
                                        it.pageNumber
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // The selected verse's actions, at the end of the flow.
            //
            // They used to be an inspector card inserted *below the whole surah*, so
            // tapping a verse on page 1 of Al-Baqarah sent the reader scrolling
            // through 286 verses to find the card that had just opened. In a flow
            // with no page boundaries, the end is the only place that is not in the
            // middle of the text - and it is where a reader's eye is after a tap
            // near the bottom anyway.
            position.selection?.let { selected ->
                ayahs.firstOrNull { it.surahNumber == selected.surah && it.ayahNumber == selected.ayah }
                    ?.let { ayah ->
                        item(key = SELECTION_KEY) {
                            SelectedVerseActions(
                                ayah = ayah,
                                actions = actionsFor(ayah),
                                options = options,
                                onDismiss = { position.clearSelection() },
                                modifier = Modifier.padding(vertical = space.md)
                            )
                        }
                    }
            }
        }
    }

    if (horizontal) {
        // The scroll container has to be the **outer** node and the wide measure has
        // to be on its child.
        //
        // Written the other way round - `width(wideMeasure).horizontalScroll(...)` -
        // the width constraint lands on the scroll container itself, so the container
        // is 720dp wide, the content inside it is 720dp wide, there is nothing to
        // scroll, and the axis is a no-op that looks like it works. The measure
        // belongs to the *content*, and the pan to the viewport.
        Box(
            modifier = modifier
                .fillMaxSize()
                .horizontalScroll(rtlScrollState()),
            contentAlignment = Alignment.TopStart
        ) {
            body(
                Modifier
                    .width(wideMeasure)
                    .fillMaxHeight()
                    .padding(horizontal = space.lg)
            )
        }
        return
    }

    body(modifier.fillMaxSize())
}

/**
 * The surah's name, written onto the paper.
 *
 * Three lines, and nothing else. This used to be a card - a centred column with the
 * Arabic name at display size, the English name, the meaning, the verse count, the
 * revelation place, then the basmalah - roughly a third of a phone screen tall
 * before a single word of the surah, paid for once per layout.
 *
 * None of it was wrong as *content* and all of it was wrong as *chrome*: the reader
 * is a reading surface, not a chapter opening, and the surah is already named in
 * the location pill and in the index. The heading is kept because a screen reader
 * still needs a landmark to jump to when navigating a surah.
 *
 * ### No basmalah
 *
 * The bundled Tanzil text carries the basmalah **inside verse 1** of every surah
 * but At-Tawbah, so printing one here printed it twice - on all 113 of them, not
 * the two the old code excepted. The rule is in [MushafPageText] and is read from
 * the corpus.
 */
@Composable
internal fun SurahHeading(
    surah: Surah,
    ink: Color,
    muted: Color,
    topInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .padding(top = topInset, bottom = space.lg)
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = surah.arabicName,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = ArabicFamily,
            color = ink,
            textAlign = TextAlign.Center
        )
        Text(
            text = surah.englishName,
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${surah.englishTranslation} · " +
                "${strings.more.verseCount.format(surah.totalVerses)} · " +
                if (surah.revelationType == RevelationType.MECCAN) {
                    strings.meccan
                } else {
                    strings.medinan
                },
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * One verse, broken out as its own unit: reference, Arabic, and the actions that
 * belong to that verse alone.
 *
 * ### Why nothing is drawn behind it
 *
 * No card, no fill, no border. A surface behind each verse looked tidy in isolation
 * and was wrong in context: a stack of filled rectangles with gaps between them
 * turns a page of Arabic into a column of panels, and the panels are what the eye
 * reads first. The paper is already the background.
 *
 * Selection is visible, but by *inking* the verse - the highlight sits behind the
 * Arabic itself, so selecting highlights the words rather than drawing a frame
 * around the ayah marker, which a reader would read as a frame around a number.
 */
@Composable
internal fun VerseRow(
    ayah: Ayah,
    selected: Boolean,
    options: QuranReadingOptions,
    ink: Color,
    muted: Color,
    actions: VerseActions,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableVerse(
                selectLabel = strings.more.selectVerse,
                selectedLabel = strings.more.selected,
                unselectedLabel = strings.more.notSelected,
                selected = selected,
                onClick = onSelect
            )
            .testTag("ayah_${ayah.ayahNumber}")
            .padding(vertical = space.xs, horizontal = space.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerseReferenceChip("${ayah.surahNumber}:${ayah.ayahNumber}")
            Spacer(Modifier.weight(1f))
            com.example.ui.quran.PlayingDot(actions.isPlaying)
        }

        Spacer(Modifier.height(space.sm))

        VerseArabic(
            ayah = ayah,
            scale = options.arabicScale,
            ink = ink
        )
    }
}

/**
 * A hairline and a page number, between the text of one mushaf page and the next.
 *
 * The continuous layout has no page breaks, which is the point of it. But the
 * mushaf *is* paginated, and a reader looking for a particular page - the one a
 * discussion referred to, the one their tahfiz is open to - has no way to find it in
 * an unbroken flow, and no way to tell they have crossed into the next one.
 *
 * A rule with the number on it answers both without reintroducing a break: nothing
 * stops or snaps, the text runs through, and the number sits on the line. Drawn
 * *before* the first verse of the page rather than in place of it, so no verse is
 * lost to the marker.
 */
@Composable
internal fun PageRule(page: Int, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.sm)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
        )
        Text(
            text = "${strings.more.pageWord} $page",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
        )
    }
}

/**
 * Verse selection as a click, named and stateful for a screen reader.
 *
 * [Role.Button] and a `stateDescription` rather than a bare `clickable`: without
 * them a verse is announced as an unlabelled button with no indication of whether it
 * is the one selected, which leaves a screen-reader user with no way to tell
 * *which* of four identical-looking buttons they are on.
 *
 * The labels are the caller's to supply, because this file does not read strings -
 * a translated surface builds its own labels and passes them down.
 */
internal fun Modifier.selectableVerse(
    selectLabel: String,
    selectedLabel: String,
    unselectedLabel: String,
    selected: Boolean,
    onClick: () -> Unit
): Modifier = clickable(
    role = Role.Button,
    onClickLabel = selectLabel,
    onClick = onClick
).semantics {
    stateDescription = if (selected) selectedLabel else unselectedLabel
}

internal const val HEADING_KEY = "surah_heading"
internal const val SELECTION_KEY = "selected_verse"

/** How many non-verse items sit above the verses in the continuous list. */
internal const val HEADING_ITEMS = 1

/** How long a scroll must settle before it counts as a new position. */
internal const val SCROLL_SETTLE_MILLIS = 500L

/**
 * How many verses go into one flowing text block.
 *
 * A block, not the whole surah: one `AnnotatedString` for 286 verses is a single
 * laid-out object several screens tall, which cannot be measured cheaply, cannot be
 * lazily discarded, and holds its whole text in memory for as long as the reader is
 * on the page. A dozen verses is one screenful - enough that the text reads as a
 * block rather than as a list, and small enough to be cheap.
 */
internal const val FLOW_BLOCK_VERSES = 12

/**
 * The measure of a horizontally-panned continuous surah.
 *
 * A real width, not `fillMaxWidth` - see the note on [ContinuousReader] for what
 * an unbounded measure does to a line of Arabic.
 */
internal val wideMeasure = 720.dp

/**
 * A horizontal scroll that *starts at the right edge*.
 *
 * ### Why
 *
 * Arabic is right-to-left. In a wide measure the first word of the surah is at the
 * **right** of the column and the text runs leftwards from there. A plain
 * `rememberScrollState()` starts at offset zero - the left edge - so opening a
 * surah on this axis landed the reader in the middle of a line, with the beginning
 * of the page off to the right and the end of the line off to the left.
 *
 * That is not a cosmetic default. On a wide measure the first screen a reader
 * should see is the first words of the text, exactly as the first screen of the
 * vertical axis is the first line of it.
 *
 * The scroll is otherwise untouched, so this decides only where the surface
 * *starts* and not how it moves.
 *
 * The move waits for a real `maxValue` rather than scrolling on the first frame,
 * because `maxValue` is 0 until the content has been measured - scrolling to it
 * then would jump to the wrong place and stay there.
 */
@Composable
private fun rtlScrollState(): ScrollState {
    val state = rememberScrollState()
    LaunchedEffect(state) {
        snapshotFlow { state.maxValue }
            .filter { it > 0 }
            .first()
            .let { state.scrollTo(it) }
    }
    return state
}
