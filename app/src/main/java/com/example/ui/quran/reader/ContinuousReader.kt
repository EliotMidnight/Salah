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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
import com.example.ui.quran.VerseTranslationCard
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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
 * So both axes render the same ordered column of verses, and the mechanism is added
 * *around* the reading rather than in place of it.
 *
 * ### The horizontal axis was a panning surface, and that was the bug
 *
 * It used to be a `horizontalScroll` around a column given a **fixed 720dp measure**,
 * and the comment above it claimed the overflow was correct. It was not; it was the
 * report this replaced: *the whole sentence on one line, exceeding the screen*.
 *
 * A fixed width cannot be fixed by choosing a different fixed width. Any measure wider
 * than the screen puts text off the screen; any measure equal to the screen leaves
 * nothing to pan. So the content is now the **viewport's width** and the axis scrolls
 * *through the text* rather than *within a line* - swipe to move on, as the page axis
 * does. See [HorizontalFlow].
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
    controlsVisible: Boolean = true,
    /**
     * How many items to scroll past the target and then back, for the initial seat.
     *
     * A parameter rather than a constant so the test can drive the arithmetic without
     * a device-sized viewport, and named for what it is for rather than for how it is
     * done. Zero means "do not overshoot", which is what a caller with no viewport
     * yet wants.
     */
    resumeOvershoot: Int = DEFAULT_RESUME_OVERSHOOT
) {
    val space = Space.current
    val horizontal = options.scroll == QuranScrollDirection.HORIZONTAL

    // The flowing text's blocks, grouped once.
    //
    // It used to be written twice - once for the items and once *inside the item
    // lambda*, to find the previous block's first verse for the page boundary. A
    // `LazyColumn` composes an item on every scroll frame, so that was a fresh list
    // of every block in the surah allocated per block per frame: for Al-Baqarah, 24
    // lists of 286 verses each, every time the reader moved. Nothing about it looked
    // wrong and the profile was the only place it appeared.
    //
    // Declared up here rather than inside the `LazyListScope` body, because that body
    // is not a composable scope and so cannot `remember`.
    val blocks = remember(ayahs) { ayahs.chunked(FLOW_BLOCK_VERSES) }

    // The item index the reader's verse sits at, or 0 when it is not in this surah.
    //
    // `+ HEADING_ITEMS` because the list is `[heading, verse…]` - the same offset the
    // scroll observer below subtracts. The two must agree or a scroll writes back a
    // position one verse from where the reader is.
    val initialIndex = remember(ayahs, blocks, options.perVerse, position.ref) {
        val verseIndex = ayahs.indexOfFirst {
            it.surahNumber == position.ref.surah && it.ayahNumber == position.ref.ayah
        }
        if (verseIndex < 0) {
            0
        } else if (options.perVerse) {
            verseIndex + HEADING_ITEMS
        } else {
            flowIndexOf(verseIndex, blocks)
        }
    }

    // Open at the reader's verse, not at the top of the surah.
    //
    // The position is the reader's place and the whole module exists so that changing
    // layout does not move them - so a list that starts at index 0 threw that away on
    // every switch *into* continuous, and worse, made it stick: the scroll observer
    // below debounced and then wrote 2:1 to Continue Reading, so the wrong place was
    // not just shown, it was saved. Reading page 42 of Al-Baqarah and switching layout
    // sent the reader to the top of the surah and remembered they were there.
    //
    // `scrollToItem`, not `animateScrollToItem`, and not a negative offset: this is a
    // re-seat to where the reader already is, so it should be instant and it should
    // put the verse at the top of the viewport rather than guess an offset that lands
    // it in the middle.
    //
    // Gated on a measured layout, because scrolling before the list has one puts the
    // reader at the end of the content instead of at the item.
    //
    // **A negative offset**, and this is the part that was wrong twice. A lazy list can
    // only compose what is near its scroll position, so scrolling to item 21 means
    // composing items around 21 - which is a screenful of text *above* the reader's
    // verse, ending in the middle of a line, under the control pill. Every position
    // had a line of clipped Arabic across the top of it, because the composition
    // starts at the item's first pixel rather than at a line boundary.
    //
    // Scrolling further and putting the item back down by the overshoot is the fix, and
    // it is why the target is scaled rather than used raw: the viewport is about three
    // screens of blocks, so a third of it reads as "some context above", and the
    // overshoot below is what the reader lands on. Without it the top of the screen is
    // a half-rendered line of Arabic, which is the single most obviously wrong thing a
    // reading surface can show.
    LaunchedEffect(surah.number, initialIndex, listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > 0 }
        if (initialIndex <= 0) return@LaunchedEffect

        // Scroll *past* the target, then back - as two separate awaits across frames,
        // which is the only way a lazy list will compose the items above the target.
        //
        // The obvious `scrollToItem(target + n); scrollToItem(target)` in one go does
        // not work and looks like it does: both calls are scroll *requests* on the same
        // state, so the second replaces the first and only the final position is ever
        // composed. The screenshot then shows a line of Arabic cut in half across the
        // top, under the control pill, which is the exact thing this was for.
        //
        // So: request the overshoot, let a frame compose it, then request the target.
        // `withFrameNanos` between them is what makes the first one land.
        listState.scrollToItem(initialIndex + resumeOvershoot)
        withFrameNanos { }
        listState.scrollToItem(initialIndex)
    }

    // The verse at the top of the viewport is the reader's position.
    //
    // The list is `[heading, verse…]`, so the index the scroll state reports is one
    // more than the verse index. The previous version indexed straight into the
    // ayah list with it, which put the recorded position one verse ahead of the top
    // of the screen for the whole of the surah - and sent `ayahs[1]` whenever the
    // heading was showing at all.
    //
    // `drop(1)` and not `debounce`: the first emission of a fresh list is the item it
    // opened at, which is the position the reader already had, so there is nothing to
    // report. Debouncing it instead reported it half a second later, which is how a
    // layout switch wrote a position the reader never chose.
    LaunchedEffect(listState, surah.number) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
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
            // The chrome's room is the list's *content padding*, not the heading's.
            //
            // It was on the heading, which is correct for exactly one scroll position:
            // the top of the surah. Anywhere else - and after the resume above, that is
            // everywhere - a line of Arabic sat under the control pill, because nothing
            // between the list's first pixel and the text accounted for it. The pill is
            // chrome *over* the reading surface, so the surface has to reserve room for
            // it at every offset, and in a lazy list the only thing that does so at
            // every offset is the content padding.
            //
            // Read here rather than inside the scope, because `LazyListScope` is not a
            // composable scope and cannot call [PageInsets.top].
            contentPadding = PaddingValues(
                top = PageInsets.top(controlsVisible) + space.md,
                bottom = space.xxl
            )
        ) {
            item(key = HEADING_KEY) {
                SurahHeading(
                    surah = surah,
                    ink = ink,
                    muted = muted,
                    // No top inset: the list's content padding already carries it, and
                    // passing it twice put a full bar of dead space above the heading.
                    topInset = 0.dp,
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
                    blocks,
                    key = { index, _ -> "flow_$index" }
                ) { index, group ->
                    if (index > 0 && group.first().pageNumber !=
                        blocks[index - 1].first().pageNumber
                    ) {
                        PageRule(page = group.first().pageNumber)
                    }
                    FlowingBlock(
                        ayahs = group,
                        selected = position.selection,
                        options = options,
                        ink = ink,
                        accent = accent,
                        // The verse itself, not a number to go and look up. This
                        // used to search the whole surah for a verse it was already
                        // holding - a linear scan per tap, on a list of up to 286,
                        // to recover fields it had just been handed.
                        onSelectVerse = { ayah ->
                            position.toggleSelection(
                                QuranRef(
                                    ayah.surahNumber,
                                    ayah.ayahNumber,
                                    ayah.pageNumber
                                )
                            )
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
        HorizontalFlow(
            surah = surah,
            blocks = blocks,
            initialBlock = initialIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0)),
            selected = position.selection,
            options = options,
            ink = ink,
            accent = accent,
            muted = muted,
            onSelectVerse = { ayah ->
                position.toggleSelection(
                    QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
                )
            },
            onSettled = { block ->
                block.firstOrNull()?.let { ayah ->
                    onPositionSettled(
                        QuranRef(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
                    )
                }
            },
            topInset = PageInsets.top(controlsVisible) + space.md,
            modifier = modifier.fillMaxSize()
        )
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
        // One part per line, each its own paragraph.
        //
        // This used to be three parts joined with " · " into one string. A single
        // string is a single bidirectional paragraph, and under RTL that is not three
        // phrases in a row: the Latin and the digits each take their own run, and the
        // order the reader sees is the *visual* order. The line came out as
        // "4 · آية · The Sincerity" - reversed, with the count first and the meaning in
        // the middle.
        //
        // Splitting the paragraphs is the whole fix, and no direction override is
        // needed or wanted: `TextStyle.textDirection` already defaults to `Content`,
        // which resolves each paragraph from its own first strong character, so a
        // Latin line reads left-to-right inside an RTL interface and an Arabic line
        // reads right-to-left inside an LTR one. That is the correct behaviour for both
        // and it is what per-part `Text`s get for free.
        //
        // The `·` separators go with the join: there is nothing to separate any more,
        // and a line per part reads better than three fragments with rules between
        // them on a page that is otherwise only text.
        listOf(
            surah.englishTranslation,
            strings.more.verseCount.format(surah.totalVerses),
            if (surah.revelationType == RevelationType.MECCAN) {
                strings.meccan
            } else {
                strings.medinan
            }
        ).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
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

        // The translation, when the reader asked for it.
        //
        // It was missing here entirely, and invisibly: `showTranslation` is a reader
        // setting with a control in the options sheet, and turning it on changed the
        // flowing layout and the mushaf's selected-verse bar - but not a single
        // per-verse row. A reader who asked for translations, chose per-ayah, and got
        // Arabic only. Nothing errored; the option was just not connected here.
        if (options.showTranslation) {
            Spacer(Modifier.height(space.sm))
            VerseTranslationCard(
                ayah = ayah,
                translationScale = options.translationScale,
                ink = ink,
                // No card, and no reference chip: the row already opens with
                // "2:255", and a second one directly below it is a number repeated
                // for no gain.
                surface = Color.Transparent,
                showReference = false
            )
        }
    }
}

/**
 * Which block a verse is in, as an item index.
 *
 * Flowing text is `ayahs.chunked(FLOW_BLOCK_VERSES)`, so a verse's item is its
 * block's index - which is its verse index divided by the block size, not the verse
 * index itself. Getting that wrong scrolls to a block twelve times further on, which
 * on Al-Baqarah means landing anywhere at all in a 24-block surah.
 *
 * Counted rather than divided, because blocks are the last one short: 286 verses at
 * twelve a block is 23 full blocks and one of ten, so the last verse is in block 23
 * and not 23.83 rounded. Dividing gives 24, which is one past the end of the list -
 * and a `scrollToItem` past the end is how a reader ends up looking at a blank sheet
 * rather than at the text they asked for.
 */
internal fun flowIndexOf(verseIndex: Int, blocks: List<List<Ayah>>): Int {
    if (verseIndex < 0) return 0
    // Counted, not searched: `indexOfFirst { verseIndex in it.indices }` answers 0
    // for every verse, because block 0's indices contain 0 and `0 in 0..11` is true
    // for the first verse *and* the predicate never examines the later blocks that
    // would also be candidates. Every reader resumed at the top of the surah and the
    // arithmetic behind it said it had not.
    var block = 0
    var remaining = verseIndex
    while (block < blocks.size && remaining >= blocks[block].size) {
        remaining -= blocks[block].size
        block++
    }
    return block.coerceAtMost(blocks.lastIndex.coerceAtLeast(0))
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
 * How far past the reader's verse the initial seat scrolls, then back.
 *
 * One screenful of items, which is about three screens of blocks. Enough for the
 * reader to see where they have arrived and still be at their verse, and it is what
 * makes the list compose *whole* blocks above the target instead of starting
 * mid-line: a lazy list begins composing at the item's own first pixel, so a plain
 * `scrollToItem` puts the top of the screen inside a line of Arabic.
 */
internal const val DEFAULT_RESUME_OVERSHOOT = 3

// The horizontal axis was a `horizontalScroll` around a 720dp column, which laid a verse
// out as one line running past both edges of the screen. `wideMeasure` and
// `rtlScrollState` went with it: the surface is now the viewport's width and the axis
// paginates, so there is no wide measure to name and no scroll offset to start at.

