package com.example.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.ArabicDigits
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusBanner
import com.example.ui.components.statusBarInset
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.Decelerate
import com.example.ui.theme.IconSize
import com.example.ui.theme.Motion
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranFonts.LocalQuranTypeface
import com.example.ui.theme.QuranPaper
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reading.
 *
 * The reader is the app's only full-screen surface and it is built around one
 * idea: **the mushaf is the interface**. Everything that is not the page is a
 * thin pill that can be dismissed, because a reader that competes with its own
 * text has already lost.
 *
 * ### The one thing worth knowing about this file
 *
 * Every layout reads from the *same* anchor - a `(surah, ayah)` pair held in
 * [SalahUiState] - and each mode knows how to resolve that anchor into its own
 * coordinates: an index into a surah for the scrolling layouts, a page number
 * for the mushaf layouts. That is what makes switching layout keep your place
 * instead of throwing you back to the top of the surah, and it is why there is
 * no second "where am I" cursor anywhere in the reader.
 *
 * ### What is deliberately absent
 *
 * No app bar with a title, no bottom dock inside the reader, no settings row, no
 * floating action buttons over the text. The control pill at the top and the
 * two round buttons beside it are the entire chrome, and all three leave in
 * immersive mode.
 */
@OptIn(FlowPreview::class)
@Composable
fun QuranReader(
    state: SalahUiState,
    onSelectSurah: (Int) -> Unit,
    onSelectSurahAyah: (Int, Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    options: QuranReadingOptions,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    immersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit,
    onOpenIndex: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    // The paper. Everything else on this surface is read against it.
    val paper = QuranPaper.wash(options.paper, isDark)
    val ink = QuranPaper.ink(isDark)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val surah = state.selectedSurah
    val ayahs = state.currentSurahAyahs

    // The anchor's page. Null until the corpus resolves it, which is why the
    // paged layouts fall back to page 1 rather than rendering nothing.
    val anchorPage = remember(surah.number, state.activeReadingAyahNumber) {
        QuranBrowse.ref(surah.number, state.activeReadingAyahNumber)?.page ?: 1
    }

    var selectedAyah by rememberSaveable { mutableStateOf(0) }

    // The page the reader is on.
    //
    // This is the single source of truth for "where am I", and the pager is
    // told to follow it rather than the other way round.
    //
    // It is deliberately *not* keyed on [anchorPage] the way it used to be.
    // Re-seeding it from the anchor on every anchor change meant the number in
    // the pill was whatever the anchor said, while the pager stayed wherever it
    // had been left - `rememberPagerState` reads `initialPage` once, at
    // composition, and ignores it from then on. So picking a surah from the
    // index moved the label and nothing else, and the pill reported a page the
    // reader was not looking at.
    //
    // Now the anchor moves *this*, and this moves the pager. External
    // navigation and the reader's own swipes both end up in one place.
    var pageCursor by rememberSaveable { mutableIntStateOf(anchorPage) }

    // Magnification is temporary and belongs to the *view*, not to the reading.
    // It is deliberately not persisted: reopening the reader should show the text
    // at the size the reader chose, not at whatever magnification they happened to
    // leave behind. Changing layout or axis resets it too, because a
    // magnification chosen for a page of continuous text makes no sense over one
    // verse - but per-verse does not reset it, since that only changes how the
    // same text is divided up. Leaving the Quran tab disposes this composition,
    // which resets it for the original reason.
    val (viewScale, setViewScale) = rememberViewScale("${options.layout}-${options.scroll}")
    // The magnified view can be dragged around. Only meaningful while
    // VIEW_SCALE is the pinch target and the scale is above 1 - `readerPinch`
    // decides when to forward a drag, and `rememberPan` clamps it to the
    // surplus the scale actually created.
    var readingSize by remember { mutableStateOf(IntSize.Zero) }
    // Where the pinch happened, so the magnified surface can stay under the
    // fingers instead of drifting away from them as the layer scales about its
    // centre. Reset whenever the scale returns to 1x, so a correction from a
    // finished pinch is never reapplied to the next.
    var pinchFocal by remember { mutableStateOf(Offset.Zero) }
    val focalOffset = focalCorrection(pinchFocal, viewScale, readingSize)
    val (pan, setPan) = rememberPan(viewScale, readingSize)
    // The reader's own pan plus the correction that keeps the pinch centred
    // under the fingers - clamped as one value, not two.
    //
    // Clamping the sum rather than each part matters: the focal correction is
    // not bounded by the same thing the reader's pan is. A pinch at the far
    // corner of a surface magnified to the maximum asks for a correction
    // exactly the size of the visible surplus, and adding it to an
    // already-clamped pan would drag the reading past the edge of its own
    // content, leaving a strip of blank paper with no way back. Clamped together,
    // the reader gets the best correction the geometry allows and the surface
    // never shows anything that is not text.
    val appliedPan = remember(pan, focalOffset, viewScale, readingSize) {
        clampPan(pan + focalOffset, viewScale, readingSize)
    }

    val listState = rememberLazyListState()
    val pagePager = rememberPagerState(
        initialPage = (pageCursor - 1).coerceIn(0, TotalPages - 1),
        pageCount = { TotalPages }
    )

    // How many non-verse items sit above the verses in the continuous layout.
    //
    // The continuous LazyColumns are `[heading, verse…]`, so the item index the
    // scroll state reports is *one more* than the verse index. The progress
    // writer used to index straight into the ayah list with it, which put the
    // recorded position one verse ahead of the top of the screen for the whole
    // of the surah, and sent `ayahs[1]` when the heading was showing at all.
    val continuousHeadingOffset = 1

    // Follow the anchor onto the pager.
    //
    // `rememberPagerState` only honours `initialPage` on its first composition,
    // so nothing else was ever going to move it. This is what makes the index
    // sheet work: picking a surah or a verse moves the anchor, this turns that
    // into a page change, and the reader lands on it.
    //
    // Guarded on inequality so that the write-back below - which also changes
    // the anchor - does not send the pager round again.
    LaunchedEffect(anchorPage) {
        val target = anchorPage.coerceIn(1, TotalPages)
        if (pageCursor != target) {
            pageCursor = target
            pagePager.scrollToPage(target - 1)
        }
    }

    // The pager is the other way into the same number. Kept up here rather than
    // at the call site so that a programmatic jump and a swipe cannot both be
    // fighting to set it.
    LaunchedEffect(pagePager) {
        snapshotFlow { pagePager.currentPage }.collect { page ->
            val shown = page + 1
            if (pageCursor != shown) pageCursor = shown
        }
    }

    // Re-seat on the anchor whenever the surah or the layout changes, so changing
    // layout lands on the verse you were reading rather than the top.
    LaunchedEffect(surah.number, options.layout, options.scroll) {
        val target = state.activeReadingAyahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        if (options.layout == QuranReadingLayout.CONTINUOUS) {
            val index = ayahs.indexOfFirst { it.ayahNumber == target }.coerceAtLeast(0)
            listState.scrollToItem(index + continuousHeadingOffset)
        }
        selectedAyah = if (options.layout == QuranReadingLayout.CONTINUOUS) target else 0
    }

    // Write the page cursor back into the anchor, so Continue Reading follows
    // the mushaf as it is actually turned and not only the surah view.
    LaunchedEffect(pageCursor) {
        if (options.layout != QuranReadingLayout.PER_PAGE) return@LaunchedEffect
        QuranBrowse.ayahsOnPage(pageCursor).firstOrNull()?.let { first ->
            onSelectSurahAyah(first.surahNumber, first.ayahNumber)
        }
    }

    // Progress, debounced. This used to write to the database on every scroll
    // step, which is both slow and unnecessary: nobody needs their position
    // recorded more precisely than a verse.
    //
    // Both continuous axes scroll a real list - the horizontal one only *also*
    // pans - so this observes it on either. Skipping the horizontal axis here
    // was half of the page pill drifting away from the text: nothing was writing
    // a position while reading sideways.
    LaunchedEffect(listState, options.layout, options.scroll) {
        if (options.layout == QuranReadingLayout.PER_PAGE) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(400)
            .collect { index ->
                ayahs.getOrNull(index - continuousHeadingOffset)?.let(onAyahViewed)
            }
    }

    // What the reader is actually looking at, reported by whichever surface is
    // on screen. This is what the page pill shows in the continuous layouts: not
    // the anchor, which only moves when something writes to it, but the top of
    // the visible text.
    var browsedPage by remember { mutableIntStateOf(anchorPage) }
    LaunchedEffect(pageCursor, options.layout) {
        if (options.layout == QuranReadingLayout.PER_PAGE) browsedPage = pageCursor
    }
    val onVerseVisible: (Ayah) -> Unit = { verse ->
        browsedPage = verse.pageNumber
    }

    // Continuous records progress from the selection itself, because there is
    // no list to observe - the whole surah is one text block.
    LaunchedEffect(selectedAyah, options.layout) {
        if (options.layout != QuranReadingLayout.CONTINUOUS) return@LaunchedEffect
        if (selectedAyah <= 0) return@LaunchedEffect
        ayahs.firstOrNull { it.ayahNumber == selectedAyah }?.let(onAyahViewed)
    }

    QuranFonts.Provide(options.font) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(paper)
                .testTag("quran_reader")
        ) {
            if (ayahs.isEmpty()) {
                // This is the first frame on launch, because currentSurahAyahs
                // starts empty - and it is also what the user would stare at
                // forever if the corpus ever failed to produce verses. A bare
                // "Loading" with no message and no way out covered both.
                EmptyState(
                    title = strings.more.loading,
                    message = strings.more.loadingQuranMessage,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val gesture = Modifier
                    .readerPinch(
                        target = options.pinchTarget,
                        arabicScale = options.arabicScale,
                        onArabicScaleChange = { onOptionsChange(options.copy(arabicScale = it)) },
                        viewScale = viewScale,
                        onViewScaleChange = setViewScale,
                        pan = pan,
                        onPanChange = setPan,
                        onFocal = { pinchFocal = it }
                    )
                    // The view scale magnifies the reading surface and can then
                    // be dragged around. It is applied above the scroll
                    // container so it zooms the text without dragging the
                    // gesture surfaces along with it. `clip = false` keeps a
                    // magnified line from being cut off at the page edge while
                    // it is being dragged into view.
                    //
                    // `appliedPan` is the reader's own pan plus the correction
                    // that keeps the pinch centred under the fingers, so the two
                    // cannot fight over the same offset.
                    .graphicsLayer {
                        scaleX = viewScale
                        scaleY = viewScale
                        translationX = appliedPan.x
                        translationY = appliedPan.y
                        clip = false
                    }
                    .onSizeChanged { readingSize = it }

                // A horizontal drag changes surah only where that axis is free.
                //
                // It is not free on the per-page mushaf, where a horizontal drag
                // is a page turn, and not on the horizontal continuous axis,
                // where it is a pan across the measure. Two drag consumers on
                // one axis means neither of them works, so this is attached only
                // in the one combination where the gesture is unclaimed.
                val surahSwipe = if (options.layout == QuranReadingLayout.CONTINUOUS &&
                    options.scroll == QuranScrollDirection.VERTICAL
                ) {
                    Modifier.swipeToChangeSurah(surah.number, onChange = onSelectSurah)
                } else {
                    Modifier
                }

                when {
                    // ---- Per-page mushaf: one page at a time, either axis ----
                    //
                    // The axis is read straight off the option rather than
                    // branched on here. An earlier version had two separate
                    // branches - one hardcoding HORIZONTAL, the other
                    // hardcoding VERTICAL - which is one more place for the axis
                    // and the layout to disagree about what the reader should
                    // show, and one more place to forget to update.
                    options.layout == QuranReadingLayout.PER_PAGE ->
                        MushafPager(
                            pagerState = pagePager,
                            orientation = options.scroll,
                            options = options,
                            ink = ink,
                            accent = accent,
                            muted = muted,
                            selectedAyah = selectedAyah,
                            state = state,
                            onSelectAyah = { selectedAyah = it },
                            onToggleBookmark = onToggleBookmark,
                            onTogglePlayAyah = onTogglePlayAyah,
                            modifier = gesture.fillMaxSize()
                        )

                    // ---- Continuous surah, verse by verse, either axis ----
                    options.layout == QuranReadingLayout.CONTINUOUS && options.perVerse ->
                        ContinuousPerVerse(
                            surah = surah,
                            ayahs = ayahs,
                            listState = listState,
                            selectedAyah = selectedAyah,
                            onSelectAyah = { selectedAyah = it },
                            options = options,
                            ink = ink,
                            accent = accent,
                            muted = muted,
                            state = state,
                            onToggleBookmark = onToggleBookmark,
                            onTogglePlayAyah = onTogglePlayAyah,
                            onVerseVisible = onVerseVisible,
                            modifier = gesture.then(surahSwipe).fillMaxSize()
                        )

                    // ---- Continuous surah: one unbroken flow ----
                    // One component for both axes. The axis is read inside it,
                    // and it changes only the measure and the pan container -
                    // never the arrangement of the text.
                    else -> ContinuousSurah(
                        surah = surah,
                        ayahs = ayahs,
                        listState = listState,
                        selectedAyah = selectedAyah,
                        onSelectAyah = { selectedAyah = it },
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        state = state,
                        onToggleBookmark = onToggleBookmark,
                        onTogglePlayAyah = onTogglePlayAyah,
                        onVerseVisible = onVerseVisible,
                        modifier = gesture
                            .then(surahSwipe)
                            .fillMaxSize()
                    )
                }
            }

            // In immersive mode the *controls row* goes, but the reading surface
            // is left completely alone. An earlier version put a full-screen
            // click target here to bring the controls back, and it ate every
            // gesture on the page: swiping turned nothing and tapping did not
            // turn the page, so immersive mode became a place you could get into
            // and not navigate out of.
            //
            // Nothing overlays the page now, and the exit is the immersive
            // button itself, which stays visible in both states.

            ReaderControls(
                surah = surah,
                browsedPage = browsedPage,
                layout = options.layout,
                options = options,
                ink = ink,
                muted = muted,
                isBookmarked = state.bookmarks.any {
                    it.surahNumber == surah.number &&
                        it.ayahNumber == state.activeReadingAyahNumber
                },
                visible = !immersive,
                onOpenIndex = onOpenIndex,
                onOpenOptions = onOpenOptions,
                onSaveCurrentLocation = {
                    QuranBrowse.ayah(surah.number, state.activeReadingAyahNumber)
                        ?.let(onToggleBookmark)
                },
                // The alignment has to be resolved here, at the call site: this
                // function's own body is not inside the Box's scope, so an
                // `align` modifier written in here does not resolve.
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // The one control that both enters and leaves immersive mode, and
            // the only chrome that survives inside it.
            //
            // It is in its own row pinned to the top-trailing corner rather than
            // in the controls row, because the controls row is hidden in
            // immersive mode - a button that disappears when you need it is a
            // trap. It stays on screen, half-transparent so it reads as
            // background rather than as a control the reader has to look at,
            // and it is a full 48dp touch target at all times, so there is
            // always exactly one tap between the reader and getting out.
            ImmersiveToggle(
                isImmersive = immersive,
                ink = ink,
                muted = muted,
                onToggle = { onImmersiveChange(!immersive) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarInset() + Space.current.xs)
                    .padding(end = Space.current.lg)
            )

            if (state.isAudioPlaying) {
                AudioStrip(
                    surahName = QuranBrowse.surah(surah.number)?.englishName.orEmpty(),
                    surahNumber = surah.number,
                    ayahNumber = state.currentAudioAyah,
                    reciter = state.reciter,
                    onStop = onStopAudio,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = space.sm)
                        .padding(horizontal = space.md)
                )
            }

            // The page tag. Only on the per-page mushaf - the one layout where a page is
            // a thing the reader can lose track of - and only while the controls
            // are up. Continuous reading has no page to name.
            if (!immersive && options.layout == QuranReadingLayout.PER_PAGE) {
                PageTag(
                    page = browsedPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = space.xxl)
                )
            }
        }
    }
}

/** The mushaf has 604 pages. The canonical partition lives in the corpus. */
private const val TotalPages = 604

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

/**
 * The whole chrome: one pill and two round buttons, in a single row.
 *
 * The pill is leading - top-left in LTR, top-right in RTL, without a single
 * conditional, because [Row] mirrors with the layout direction. It carries the
 * reader's location, so the one thing you always want is the thing that is
 * always there, and it doubles as the way into the index.
 *
 * The immersive control is deliberately **not** in this row. It lives in its own
 * corner, above, visible in both states - see [ImmersiveToggle] for why.
 */
@Composable
private fun ReaderControls(
    surah: Surah,
    browsedPage: Int,
    layout: QuranReadingLayout,
    options: QuranReadingOptions,
    ink: Color,
    muted: Color,
    isBookmarked: Boolean,
    visible: Boolean,
    onOpenIndex: () -> Unit,
    onOpenOptions: () -> Unit,
    onSaveCurrentLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.duration(Motion.SHORT))),
        exit = fadeOut(tween(Motion.duration(Motion.SHORT))),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(statusBarInset() + Space.current.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Space.current.lg)
                    // Room for the immersive control, which now lives in its own
                    // top-trailing corner and is drawn over this row in the
                    // non-immersive state too. Without this reserve the last
                    // circle button sat exactly underneath it, and only the
                    // top one was reachable.
                    .padding(
                        end = MaterialTheme.layoutMetrics.minTouchTarget +
                            Space.current.xs + Space.current.lg
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.current.xs)
            ) {
                IndexPill(
                    surah = surah,
                    // The page the reader is actually on, in every layout. This
                    // used to be the page cursor for the mushaf and the *anchor*
                    // for the continuous layouts, which meant the number changed
                    // meaning with the layout and lagged the text in both.
                    page = browsedPage,
                    showPage = true,
                    ink = ink,
                    muted = muted,
                    onClick = onOpenIndex,
                    modifier = Modifier.weight(1f)
                )

                CircleControl(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = strings.more.reader.saveThisLocation,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else muted,
                    onClick = onSaveCurrentLocation,
                    testTag = "reader_save_location"
                )
                CircleControl(
                    icon = Icons.Default.MenuBook,
                    contentDescription = strings.more.readingOptions,
                    tint = muted,
                    onClick = onOpenOptions,
                    testTag = "reader_options"
                )
            }
        }
    }
}

/**
 * The enter/exit control for immersive mode.
 *
 * One control, both directions, and it never leaves. It is deliberately drawn
 * faintly in immersive mode rather than hidden: the whole point of immersive
 * mode is that the chrome gets out of the way, but "out of the way" and "no way
 * back" are different things, and a reader who cannot find the exit reads the
 * absence as a bug.
 *
 * The label flips with the state so the button always answers "what will this
 * do" rather than "what is currently true".
 */
@Composable
private fun ImmersiveToggle(
    isImmersive: Boolean,
    ink: Color,
    muted: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Surface(
        // Faint when immersive so the reader stops noticing it, but never so
        // faint that the target is unclear.
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (isImmersive) 0.55f else 0.94f
        ),
        shape = QuranShape.pill,
        modifier = modifier
            .size(MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(
                onClickLabel = if (isImmersive) {
                    strings.more.reader.exitImmersive
                } else {
                    strings.more.reader.immersiveMode
                },
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onToggle
            )
            .testTag("reader_immersive")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isImmersive) {
                    Icons.Default.FullscreenExit
                } else {
                    Icons.Default.Fullscreen
                },
                contentDescription = if (isImmersive) {
                    strings.more.reader.exitImmersive
                } else {
                    strings.more.reader.immersiveMode
                },
                tint = if (isImmersive) ink else muted,
                modifier = Modifier.size(IconSize.lg)
            )
        }
    }
}

/**
 * The location, as one thin pill, and the way into the index.
 *
 * Thin because it sits over text: a full-height bar here would be a header
 * again, which is the thing this screen exists not to be. The surah name leads
 * and the count follows, because the name is what you recognise and the number
 * is what you check.
 *
 * The trailing number is the **verse count** in the surah-scoped layouts, not
 * the surah's position in the mushaf. The surah's order number was the one
 * thing on this pill that answered a question the reader had already answered
 * by choosing the surah, while the verse count is the fact that actually
 * describes what is on screen. Page mode keeps showing the page, which is the
 * only number that can be lost there.
 */
@Composable
private fun IndexPill(
    surah: Surah,
    page: Int,
    showPage: Boolean,
    ink: Color,
    muted: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shape = QuranShape.pill,
        modifier = modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(
                onClickLabel = strings.more.reader.openIndex,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
            .testTag("reader_index")
            .semantics {
                contentDescription = if (showPage) {
                    "${strings.more.pageWord} $page"
                } else {
                    "${strings.more.selectSurah}: ${surah.englishName}. " +
                        strings.more.verseCount.format(surah.totalVerses)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.current.lg, vertical = Space.current.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = surah.englishName,
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(Space.current.sm))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(muted.copy(alpha = 0.35f))
            )
            Spacer(Modifier.width(Space.current.sm))
            Text(
                text = if (showPage) {
                    "${strings.more.pageWord} $page"
                } else {
                    surah.totalVerses.toString()
                },
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                maxLines = 1
            )
        }
    }
}

/** One of the three round controls. */
@Composable
private fun CircleControl(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shape = QuranShape.pill,
        modifier = Modifier
            .size(MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(
                onClickLabel = contentDescription,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(IconSize.lg)
            )
        }
    }
}

/**
 * The current mushaf page, as a quiet tag at the foot of the screen.
 *
 * Subtle on purpose: it is an orientation cue, not a control. It sits at the
 * bottom because the top of the screen already carries the surah name, and
 * because a reader's eye returns to the bottom of the page between lines.
 */
@Composable
private fun PageTag(page: Int, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        shape = QuranShape.pill,
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(QuranShape.pill)
            .clearAndSetSemantics { }
    ) {
        Text(
            text = "${strings.more.pageWord} $page",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Space.current.md, vertical = Space.current.xs)
        )
    }
}

// ---------------------------------------------------------------------------
// Surah heading
// ---------------------------------------------------------------------------

/**
 * The surah's name, written straight onto the paper.
 *
 * This used to be a card: a centred column with the Arabic name at display
 * size, the English name, the meaning of the name, the verse count and
 * revelation place, then the basmalah - a block roughly a third of a phone
 * screen tall, before a single word of the surah. It was repeated in every
 * layout, so it was paid for four times over, and in the horizontal continuous
 * mode it sat in a column of its own, so panning across the surah meant panning
 * across the heading first.
 *
 * None of it was wrong as *content* and all of it was wrong as *chrome*. The
 * reader is a reading surface, not a chapter opening, and the surah is already
 * named in the pill and in the index. So the heading is now three lines on the
 * background - name, meaning, count - with the basmalah kept because it is
 * part of the text rather than a label, and the whole block given a modest
 * bottom margin so the first verse does not run into it.
 *
 * The heading semantics stay, because a screen reader still needs to be able to
 * jump to the surah name when navigating a page of Arabic.
 */
@Composable
private fun SurahHeading(surah: Surah, ink: Color, muted: Color, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current

    // The controls row floats over the top of the reading surface - it is an
    // overlay, not a bar the content is laid out beneath. Without this the
    // heading's first line sits behind the index pill and the immersive button.
    //
    // Reserved in both states rather than only when the controls are up, because
    // the immersive toggle stays visible in immersive mode too, and a heading
    // that shifted when the controls hid would move the text under the reader's
    // eye as they toggled it.
    val topInset = statusBarInset() + MaterialTheme.layoutMetrics.minTouchTarget +
        Space.current.lg

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topInset, bottom = space.lg)
            .surahHeading(),
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
        // Al-Fatihah opens with the basmalah as its first verse, so showing it
        // separately would print it twice. At-Tawbah has none at all.
        if (surah.number != 1 && surah.number != 9) {
            Spacer(Modifier.height(space.md))
            Text(
                text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064e\u0647\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0646\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = ArabicFamily,
                fontWeight = FontWeight.Normal,
                color = ink,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// The flowing mushaf text surface
// ---------------------------------------------------------------------------

/**
 * One page of Quranic text, laid out as a single block.
 *
 * Every verse is annotated into one [AnnotatedString], so a whole surah - or a
 * whole mushaf page - is one laid-out block rather than a column of rows. That
 * is what makes it read as a *page* and is the reason to open a mushaf rather
 * than a list.
 *
 * Tapping resolves the tapped offset back to a verse through the text layout,
 * the selected verse is highlighted in place, and the ayah end-markers are set
 * in the accent at a slightly smaller size than the body so they recede instead
 * of competing.
 *
 * Accessibility is handled as discrete nodes rather than one enormous string: a
 * screen reader would otherwise read the entire surah as a single utterance with
 * no way to act on any verse in it. Each verse is exposed with a "select verse"
 * custom action, and gets the same selection the sighted user gets by tapping.
 */
@Composable
private fun FlowingTextSurface(
    ayahs: List<Ayah>,
    selectedAyah: Int,
    arabicScale: Float,
    ink: Color,
    accent: Color,
    onSelectAyah: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onVerseVisible: ((Ayah) -> Unit)? = null
) {
    val highlight = MaterialTheme.colorScheme.primaryContainer
    val typeface = LocalQuranTypeface.current
    val selectVerseLabel = LocalStrings.current.more.selectVerse

    val page = remember(ayahs, selectedAyah, arabicScale, highlight, accent, typeface) {
        buildFlowingPage(ayahs, selectedAyah, arabicScale, highlight, accent, typeface.lineHeightFactor)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Keyed on `selectedAyah` as well as `ayahs`. Remembering against `ayahs`
    // alone froze the captured selection at its first value, so after one
    // selection every action compared against a stale number and tapping the
    // selected verse again could never deselect it.
    val accessibilityActions = remember(ayahs, selectedAyah, selectVerseLabel) {
        ayahs.map { ayah ->
            CustomAccessibilityAction(label = "$selectVerseLabel ${ayah.ayahNumber}") {
                onSelectAyah(if (selectedAyah == ayah.ayahNumber) 0 else ayah.ayahNumber)
                true
            }
        }
    }

    // Straight onto the paper. See the note on [PerVerseUnit] - the reading
    // surface already has a background wash, and a second surface on top of it
    // is a card by another name. The text is the thing that should carry the
    // eye, so nothing is drawn behind it.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("flowing_text_surface")
    ) {
        Text(
            text = page.text,
            onTextLayout = { layout = it },
            style = typeface.arabicStyle(scale = arabicScale, color = ink),
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.current.xl)
                // Which verse is at the top of the viewport, so the reader's page
                // indicator can follow the text.
                //
                // A flowing block is one list item, so there are no per-verse
                // items to observe and the position has to come from the text
                // layout. `positionInWindow().y` already accounts for scrolling -
                // the node's top edge moves up the window as the list is scrolled -
                // so the first verse still on screen is the first one whose last
                // line falls below that edge. No scroll arithmetic needed.
                .onGloballyPositioned { coords ->
                    val result = layout ?: return@onGloballyPositioned
                    val report = onVerseVisible ?: return@onGloballyPositioned
                    // Nullable: the layout can be measured before it has any
                    // line boxes to ask about, and there is no verse to report
                    // then. Keeping the last good value is better than clearing
                    // the indicator.
                    firstVerseVisibleAt(
                        result,
                        page.verseSpans,
                        ayahs,
                        coords.positionInWindow().y
                    )?.let(report)
                }
                .pointerInput(ayahs, page, selectedAyah) {
                    detectTapGestures { tap ->
                        onSelectAyah(
                            verseAt(layout, page.verseSpans, ayahs, tap, selectedAyah)
                        )
                    }
                }
                .semantics { customActions = accessibilityActions }
        )
    }
}

/** The rendered page plus the character span of every verse inside it. */
private class FlowingPage(
    val text: AnnotatedString,
    val verseSpans: List<IntRange>
)

/**
 * Builds the page.
 *
 * The ayah end marker (U+06DD plus Eastern-Arabic-Indic digits) gets its own
 * span in the accent colour at 90% of the body size. That small asymmetry is
 * what separates this from a wall of text.
 *
 * [lineHeightFactor] is passed rather than read from the style because the span
 * has to be sized in the same units as the text it sits in, and the face's
 * multiplier is what makes Nastaliq and Naskh legible at the same nominal size.
 */
private fun buildFlowingPage(
    ayahs: List<Ayah>,
    selectedAyah: Int,
    arabicScale: Float,
    highlight: Color,
    accent: Color,
    lineHeightFactor: Float
): FlowingPage {
    val spans = ArrayList<IntRange>(ayahs.size)
    val markerSize = (QuranReadingOptions.ARABIC_BASE_SP * arabicScale * 0.9f).sp
    val text = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val start = length
            val selected = ayah.ayahNumber == selectedAyah

            if (selected) {
                pushStyle(SpanStyle(background = highlight, fontWeight = FontWeight.SemiBold))
            }

            append(ayah.textArabic)
            append(" ")

            if (selected) pop()

            withStyle(
                SpanStyle(
                    color = accent,
                    fontWeight = FontWeight.Medium,
                    fontSize = markerSize
                )
            ) {
                append("۝${ArabicDigits.ayahMarker(ayah.ayahNumber)}")
            }
            append(" ")

            spans += start until length
        }
    }
    return FlowingPage(text, spans)
}

/**
 * The first verse still visible below [windowTopY].
 *
 * A flowing surah is one laid-out text block, so "which page am I on" has to be
 * answered from where the block sits rather than from a list index. Walking the
 * verses and asking the layout for each one's last line is the only way to know
 * whether a verse is still on screen or has been scrolled past.
 *
 * `positionInWindow().y` already accounts for scrolling - the node's top edge
 * moves up the window as the list is scrolled - so no scroll arithmetic is
 * needed here.
 *
 * Returns the first verse whose final line is still below the top edge, which
 * is the verse the reader's eye is on. When the whole block has been scrolled
 * past, the last verse is the honest answer rather than nothing: a blank page
 * indicator at the end of a surah reads as a broken one.
 */
private fun firstVerseVisibleAt(
    layout: TextLayoutResult,
    spans: List<IntRange>,
    ayahs: List<Ayah>,
    windowTopY: Float
): Ayah? {
    if (spans.isEmpty() || ayahs.isEmpty() || layout.lineCount == 0) return null

    // The bottom edge of each verse's last line, in the text's own coordinates.
    val bottoms = spans.map { span ->
        val end = (span.last + 1).coerceIn(1, layout.layoutInput.text.length)
        // `getLineBottom` wants a line index. Clamping to the last line keeps a
        // span ending exactly at the text length from asking for a line that
        // does not exist.
        val line = layout.getLineForOffset(end - 1).coerceIn(0, layout.lineCount - 1)
        layout.getLineBottom(line)
    }

    val index = firstVerseIndexVisibleAt(bottoms, windowTopY, ayahs.size)
    return ayahs.getOrNull(index)
}

/**
 * The index of the first verse whose last line is still below [windowTopY].
 *
 * Split out from [firstVerseVisibleAt] so the rule can be checked without
 * building a text layout, which is not constructible outside the framework.
 *
 * The comparison is `bottom >= windowTopY` and not `>`: a verse whose last line
 * sits exactly on the top edge is still visible, and reporting the one *after*
 * it would put the indicator a verse ahead at the moment each verse scrolls off.
 */
internal fun firstVerseIndexVisibleAt(
    verseLineBottoms: List<Float>,
    windowTopY: Float,
    verseCount: Int
): Int {
    if (verseLineBottoms.isEmpty() || verseCount <= 0) return -1
    val limit = verseLineBottoms.size.coerceAtMost(verseCount)
    for (index in 0 until limit) {
        if (verseLineBottoms[index] >= windowTopY) return index
    }
    // Everything has been scrolled past. Report the last verse rather than
    // nothing, so the indicator reads "the end of the surah" and not "broken".
    return limit - 1
}

/**
 * Maps a tap to the verse under it.
 *
 * A gesture arrives in pixels and the spans are character indices, so the tap
 * has to go through the laid-out text to become a character offset first.
 * Comparing the two directly would resolve almost every tap to the wrong verse.
 */
private fun verseAt(
    layout: TextLayoutResult?,
    spans: List<IntRange>,
    ayahs: List<Ayah>,
    tap: Offset,
    current: Int
): Int {
    if (layout == null) return current
    val total = spans.sumOf { it.count() }
    if (total == 0) return current
    val char = layout.getOffsetForPosition(tap).coerceIn(0, total)
    val index = spans.indexOfFirst { char in it }
    if (index < 0) return current
    return ayahs.getOrNull(index)?.ayahNumber ?: current
}

// ---------------------------------------------------------------------------
// The four reading layouts
// ---------------------------------------------------------------------------

/**
 * A horizontal scroll state that *starts* at the right edge.
 *
 * ### Why
 *
 * Arabic is right-to-left. In a wide measure the first word of the surah is at
 * the *right* of the column, and the text runs leftwards from there. A plain
 * `rememberScrollState()` starts at offset zero - the left edge - so opening a
 * surah on the horizontal axis landed the reader in the middle of a line, with
 * the beginning of the page off to the right and the end of the line off to the
 * left.
 *
 * That is not a cosmetic default. On a wide measure the first screen a reader
 * sees should be the first words of the text, the way the first screen of the
 * vertical axis is the first line of it.
 *
 * The scroll is otherwise untouched: the same gesture still moves the surface
 * the same way, so this only decides where the surface *starts*.
 */
@Composable
private fun rememberRtlScrollState(): ScrollState {
    val state = rememberScrollState()
    LaunchedEffect(state) {
        // `maxValue` is 0 until the content has been measured, so this waits for
        // a real width rather than jumping to the wrong place on the first frame
        // and leaving it there.
        snapshotFlow { state.maxValue }
            .filter { it > 0 }
            .first()
            .let { state.scrollTo(it) }
    }
    return state
}

/**
 * A hairline and a page number, between the text of one mushaf page and the next.
 *
 * ### Why
 *
 * Continuous layout has no page breaks, because it is one unbroken flow of a
 * surah. That is the point of it. But the mushaf *is* paginated, and a reader
 * who is looking for a particular page - the one a discussion referred to, the
 * one their tahfiz is open to - has no way to find it in an unbroken flow, and
 * no way to tell they have crossed into the next one.
 *
 * The page indicator at the top tells you where you are, but only after you have
 * moved, and it does not tell you *when* you crossed. A single hairline with the
 * page number does, without reintroducing a page break: nothing stops or snaps,
 * the text still runs through, and the number sits on the line.
 *
 * Deliberately the quietest thing on the screen. A rule at full contrast would
 * read as a card edge, and a boxed number would be exactly the chrome the
 * continuous layout exists to be free of. A hairline at low alpha and a small
 * label is enough to answer "am I still on the page I was on".
 *
 * Drawn *between* verses rather than replacing them, so no verse is lost to the
 * marker: the first verse of each page is preceded by the rule.
 */
@Composable
private fun PageSeparator(page: Int, modifier: Modifier = Modifier) {
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
 * The whole surah as a run of distinct verses, on either axis.
 *
 * Per-verse is not a third layout and not a modifier that only the mushaf
 * understands - it is a way of presenting the text, and it applies to either
 * layout on either axis. This is the continuous one.
 *
 * ### The axis is a mechanism, not a shape
 *
 * This is the part that was wrong before, and it is worth stating plainly
 * because the mistake is easy to make again.
 *
 * The earlier horizontal implementation put each verse in its own fixed-width
 * column and laid those columns out in a `Row`. So switching the axis did not
 * switch *how you moved through the text*, it changed *what the text looked
 * like*: verses were re-arranged side by side, and a surah that read top to
 * bottom now read left to right, one verse per screen-width, each one needing
 * its own pan and its own return. Reading order was destroyed, and the reader
 * had to hold the whole surah in their head as a strip of disconnected panels
 * rather than a piece of writing.
 *
 * That is the opposite of what the axis means. Turning the axis on should
 * change the **mechanism** - which way the surface travels under the finger -
 * and nothing else. The text stays laid out the way it is laid out: verses in
 * order, one after another, down the page.
 *
 * So both axes render the *same* ordered column of [PerVerseUnit]s. What differs
 * is only the direction the surface moves:
 *
 * - **Vertical** is the ordinary reading: one screen-wide column, scrolled down.
 * - **Horizontal** gives the surface a wide measure and lets it be panned
 *   sideways, the way a real mushaf page is wider than the phone. The verses
 *   are still stacked in reading order down that wide measure - the horizontal
 *   mechanism is added *around* the reading, never in place of it.
 *
 * Both are built from one `unit` lambda and one list, so a change to the verse
 * unit reaches both axes at once. Two components would have drifted, and the
 * horizontal one would have been the one that lost the reference chips.
 */
@Composable
private fun ContinuousPerVerse(
    surah: Surah,
    ayahs: List<Ayah>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    state: SalahUiState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onVerseVisible: ((Ayah) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val horizontal = options.scroll == QuranScrollDirection.HORIZONTAL

    // One unit, built once, used by both arrangements. This is the whole reason
    // the two axes cannot drift apart.
    val unit: @Composable (Ayah) -> Unit = { ayah ->
        PerVerseUnit(
            ayah = ayah,
            selected = selectedAyah == ayah.ayahNumber,
            options = options,
            ink = ink,
            muted = muted,
            actions = VerseActions(
                ayah = ayah,
                isBookmarked = state.isBookmarked(ayah),
                isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                onToggleBookmark = { onToggleBookmark(ayah) },
                onTogglePlay = { onTogglePlayAyah(ayah) }
            ),
            onSelect = {
                onSelectAyah(if (selectedAyah == ayah.ayahNumber) 0 else ayah.ayahNumber)
            }
        )
    }

    val heading: @Composable (Modifier) -> Unit = { mod ->
        SurahHeading(surah = surah, ink = ink, muted = muted, modifier = mod)
    }

    // Report the top of the visible text, so the page pill follows the reader
    // rather than whatever last wrote to the anchor. Keyed on the list alone, so
    // it runs identically on both axes - the horizontal axis scrolls the same
    // list, it just also gives it somewhere to pan to.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(250)
            .collect { index ->
                ayahs.getOrNull(index)?.let { onVerseVisible?.invoke(it) }
            }
    }

    val content: @Composable (Modifier) -> Unit = { measure ->
        LazyColumn(
            state = listState,
            modifier = measure,
            contentPadding = PaddingValues(vertical = space.md)
        ) {
            item(key = "heading") { heading(Modifier) }
            itemsIndexed(ayahs, key = { _, ayah -> ayah.ayahNumber }) { index, ayah ->
                // A rule before the first verse of each page, so the marker costs
                // no verse. Checking the *previous* verse's page is what makes
                // the boundary correct even where a surah ends mid-page and the
                // next one starts mid-page.
                val startsPage = index == 0 || ayahs[index - 1].pageNumber != ayah.pageNumber
                if (startsPage && index > 0) {
                    PageSeparator(page = ayah.pageNumber)
                }
                unit(ayah)
                Spacer(Modifier.height(space.md))
            }
        }
    }

    if (horizontal) {
        // The pan lives *outside* the reading. A reader on this axis pans left
        // and right across a wide measure, and still reads the verses top to
        // bottom - the two movements coexist rather than one replacing the
        // other, which is exactly what a printed mushaf page allows.
        val panState = rememberRtlScrollState()
        Box(
            modifier = modifier.horizontalScroll(panState),
            contentAlignment = Alignment.TopStart
        ) {
            content(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg)
            )
        }
        return
    }

    content(modifier.padding(horizontal = space.lg))
}

/**
 * The whole surah as one unbroken flow, on either axis.
 *
 * This is the flowing reading: every verse annotated into a single laid-out
 * block, so a surah reads as continuous prose rather than a stack of cards. It
 * is the same text and the same line breaking on both axes - only the direction
 * the surface travels differs.
 *
 * ### Why horizontal is a wider measure and not a re-laid-out page
 *
 * The horizontal axis exists so the surah can be read the way a printed mushaf
 * is read: across a page that is wider than the phone. So the measure widens to
 * the width of the surface and the surface pans sideways.
 *
 * What it must not do is re-arrange the text. An earlier attempt treated the
 * axis as a different *shape* - laying verses out side by side so the surah ran
 * left to right. That is not a wider page, it is a different document: reading
 * order is lost, and every verse becomes a panel you pan to and back from.
 * Direction of movement and arrangement of content are separate things, and only
 * the first one belongs to this setting.
 *
 * The actions and the inspector are shared with the vertical version rather than
 * reimplemented, because they are about *which verse* is selected, not about how
 * the text is laid out.
 */
@Composable
private fun ContinuousSurah(
    surah: Surah,
    ayahs: List<Ayah>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    state: SalahUiState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onVerseVisible: ((Ayah) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val horizontal = options.scroll == QuranScrollDirection.HORIZONTAL
    val panState = rememberRtlScrollState()

    // The surah split into its mushaf pages, in order, keeping every verse.
    //
    // Grouped rather than filtered so the total is unchanged: continuous means
    // the whole surah is here, and marking where the pages fall must not drop a
    // verse to do it. A surah that starts or ends mid-page keeps that partial
    // page, which is why the first and last groups can be short.
    val pageGroups = remember(ayahs) {
        ayahs.fold(mutableListOf<MutableList<Ayah>>()) { groups, ayah ->
            val current = groups.lastOrNull()
            if (current == null || current.last().pageNumber != ayah.pageNumber) {
                groups += mutableListOf(ayah)
            } else {
                current += ayah
            }
            groups
        }
    }

    val inspector: @Composable (Modifier) -> Unit = { measure ->
        ayahs.firstOrNull { it.ayahNumber == selectedAyah }?.let { ayah ->
            VerseInspector(
                ayah = ayah,
                actions = VerseActions(
                    ayah = ayah,
                    isBookmarked = state.isBookmarked(ayah),
                    isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                    onToggleBookmark = { onToggleBookmark(ayah) },
                    onTogglePlay = { onTogglePlayAyah(ayah) }
                ),
                arabicScale = options.arabicScale,
                ink = ink,
                muted = muted,
                onDismiss = { onSelectAyah(0) },
                modifier = measure.testTag("ayah_inspector")
            )
        }
    }

    val translations: @Composable (Modifier) -> Unit = { measure ->
        if (options.showTranslation) {
            Column(measure) {
                ayahs.forEach { ayah ->
                    VerseTranslationCard(
                        ayah = ayah,
                        translationScale = options.translationScale,
                        ink = ink,
                        surface = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = space.xs)
                    )
                }
            }
        }
    }

    // Vertical is a plain scroll. Horizontal is the same scroll given a wide
    // measure and a pan container around it - identical content, identical
    // order, identical line breaking, just more room across and a sideways way
    // in.
    val body: @Composable (Modifier) -> Unit = { measure ->
        LazyColumn(
            state = listState,
            modifier = measure,
            contentPadding = PaddingValues(vertical = space.md)
        ) {
            item(key = "heading") {
                SurahHeading(surah = surah, ink = ink, muted = muted)
            }
            // One text block per mushaf page, with a hairline between them.
            //
            // The blocks are laid out identically and with no card, gap or
            // padding between them, so the text still reads as one unbroken
            // flow - that is the whole promise of this layout. Only the page
            // rule interrupts it, and a reader who does not want the marker can
            // simply not look at it.
            //
            // Split per page rather than one block for the surah because a single
            // block cannot have anything drawn *inside* it, and the rule has to
            // sit between the last verse of one page and the first of the next.
            itemsIndexed(pageGroups, key = { index, _ -> "page_$index" }) { index, group ->
                if (index > 0) {
                    PageSeparator(page = group.first().pageNumber)
                }
                FlowingTextSurface(
                    ayahs = group,
                    selectedAyah = selectedAyah,
                    arabicScale = options.arabicScale,
                    ink = ink,
                    accent = accent,
                    onSelectAyah = onSelectAyah,
                    onVerseVisible = onVerseVisible
                )
            }
            if (selectedAyah > 0) {
                item(key = "inspector") {
                    Spacer(Modifier.height(space.md))
                    inspector(Modifier)
                }
            }
            item(key = "translations") { translations(Modifier) }
        }
    }

    if (horizontal) {
        Box(
            modifier = modifier.horizontalScroll(panState),
            contentAlignment = Alignment.TopStart
        ) {
            body(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg)
            )
        }
        return
    }

    body(modifier.padding(horizontal = space.lg))
}

/**
 * The horizontal measure is the width of the surface, not a fixed number.
 *
 * It used to be 640dp, chosen as "wider than a phone". That was wrong twice
 * over:
 *
 * - On a small phone it was wider than the screen, so a line of Arabic ran off
 *   the right edge and every line needed two pans to read.
 * - On a tablet or an unfolded foldable it was *narrower* than the window, so
 *   the "wide" measure was not wide at all and the pan did nothing.
 *
 * A dip constant cannot be right for both, because the thing it needs to match
 * is the screen it is being drawn on. So the measure is simply the width the
 * surface was given, and the pan container stays attached: with the measure
 * equal to the window there is no overflow, so it simply has nothing to move
 * and the axis becomes a no-op rather than a dead gesture.
 */

/**
 * One verse, broken out as its own unit: reference chip, Arabic, and - only once
 * it has been selected - the actions that belong to that verse and no other.
 *
 * This is the study mode. The whole surface is quiet until you touch a verse,
 * because a row of four buttons under every verse on a page is a wall of
 * controls and nothing about a page of reading; one row under the verse you are
 * actually on is the only set that is ever relevant.
 *
 * It is the same unit in both layouts. On the mushaf it sits within one page;
 * in continuous it sits within the surah. Nothing about the unit changes, because
 * the thing being decided - *this* verse - does not depend on how much text
 * surrounds it.
 *
 * Selection is exclusive and toggleable: tapping the selected verse again clears
 * it and takes the row away.
 */
@Composable
private fun PerVerseUnit(
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

    // No card, no fill, no border. The verse sits on the paper like every other
    // line of the reading.
    //
    // A surface behind each verse looked tidy in isolation and was wrong in
    // context: a stack of filled rectangles with gaps between them turns a page
    // of Arabic into a column of panels, and the panels are what the eye reads
    // first. The paper is already the background - the app draws a wash behind
    // the whole reading surface - so anything drawn on top of it is decoration
    // competing with the text.
    //
    // Selection is still visible, but by *inking* the verse rather than boxing
    // it: the background highlight sits behind the Arabic itself, so selecting a
    // verse highlights the words instead of drawing a frame around them.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = strings.more.selectVerse,
                onClick = onSelect
            )
            .semantics {
                stateDescription = if (selected) {
                    strings.more.selected
                } else {
                    strings.more.notSelected
                }
            }
            .testTag("ayah_${ayah.ayahNumber}")
            .padding(vertical = space.xs, horizontal = space.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                VerseReferenceChip("${ayah.surahNumber}:${ayah.ayahNumber}")
                Spacer(Modifier.weight(1f))
                PlayingDot(actions.isPlaying)
            }

            Spacer(Modifier.height(space.sm))

            VerseArabic(
                ayah = ayah,
                scale = options.arabicScale,
                ink = ink
            )

            // The actions live here rather than in the chip row because the
            // chip row is what the finger already passed over on the way to
            // selecting, and a target that appears under a tap is a target you
            // cannot aim at deliberately.
            if (selected) {
                Spacer(Modifier.height(space.md))
                if (options.showTranslation) {
                    VerseTranslationCard(
                        ayah = ayah,
                        translationScale = options.translationScale,
                        ink = ink,
                        surface = Color.Transparent,
                        showReference = false
                    )
                    Spacer(Modifier.height(space.md))
                }
                VerseActionRow(actions)
            }
    }
}

/** Whole-surah flowing text, scrolled vertically. */

/**
 * The mushaf: exactly one page at a time, on either axis.
 *
 * This is a page *turner*, not a document. The previous vertical version was a
 * `LazyColumn` of all 604 pages, which meant a swipe scrolled you across a
 * boundary rather than turning anything, three pages sat half-visible in the
 * viewport at any moment, and "which page am I on" was a question the reader
 * could only answer by scrolling back to find the seam. All three of those are
 * what the printed mushaf is not.
 *
 * So both axes are a pager. A vertical swipe turns forward and a horizontal one
 * turns sideways, and neither ever leaves more than one page on screen.
 *
 * The page is also independently scrollable, because the reader sizes the text
 * and a dense page at a generous size is taller than the phone. The inner scroll
 * is vertical only; the pager keeps the horizontal drag for turning, which is why
 * [orientation] is passed to the pager and the page always scrolls vertically.
 *
 * ### Edge tap zones
 *
 * Each page carries two invisible gutters that turn the page, at the ends of the
 * *pager's* axis - left and right when turning sideways, top and bottom when
 * turning down. They are 48dp, so neither is a target you have to aim at.
 *
 * Deliberately *not* a full-page tap handler. The page's own text claims taps to
 * select a verse, and a parent that consumed them would make turning the page and
 * selecting a verse mutually exclusive - you could not have both without the
 * reader guessing which you meant. Gutters live in the margin where there is no
 * text to select, so the two coexist.
 */
@Composable
private fun MushafPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    orientation: QuranScrollDirection,
    options: QuranReadingOptions,
    selectedAyah: Int,
    ink: Color,
    accent: Color,
    muted: Color,
    state: SalahUiState,
    onSelectAyah: (Int) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val turn: (Int) -> Unit = { delta ->
        val next = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
        scope.launch { pagerState.animateScrollToPage(next) }
    }

    val page: @Composable (Int) -> Unit = { index ->
        val current = index + 1

        // Tap gutters exist on the horizontal axis only.
        //
        // On the horizontal axis they earn their place: the spec asks for
        // invisible zones near the left and right edges so a page can be turned
        // by tapping instead of swiping, and the vertical space they cost is
        // only horizontal space, which the page was not using anyway.
        //
        // On the vertical axis there is no such request - the reader turns
        // pages by swiping - and a top/bottom gutter would cost 2 x 48dp of
        // *vertical* room from a page that, on this axis, no longer scrolls of
        // its own accord (see [MushafPage]). Trading the page's height for an
        // affordance nobody asked for, on the one axis where height is already
        // the scarce resource, is a bad trade. So the swipe is the whole
        // vertical affordance.
        if (orientation == QuranScrollDirection.HORIZONTAL) {
            Row(Modifier.fillMaxSize()) {
                TapZone(
                    onTap = { turn(-1) },
                    enabled = index > 0,
                    contentDescription = strings.more.reader.previousPage,
                    modifier = Modifier.width(TapGutterWidth)
                )
                MushafPage(
                    pageNumber = current,
                    orientation = orientation,
                    options = options,
                    ink = ink,
                    accent = accent,
                    muted = muted,
                    selectedAyah = if (options.perVerse) selectedAyah else 0,
                    onSelectAyah = onSelectAyah,
                    state = state,
                    onToggleBookmark = onToggleBookmark,
                    onTogglePlayAyah = onTogglePlayAyah,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                TapZone(
                    onTap = { turn(1) },
                    enabled = index < TotalPages - 1,
                    contentDescription = strings.more.reader.nextPage,
                    modifier = Modifier.width(TapGutterWidth)
                )
            }
        } else {
            MushafPage(
                pageNumber = current,
                orientation = orientation,
                options = options,
                ink = ink,
                accent = accent,
                muted = muted,
                selectedAyah = if (options.perVerse) selectedAyah else 0,
                onSelectAyah = onSelectAyah,
                state = state,
                onToggleBookmark = onToggleBookmark,
                onTogglePlayAyah = onTogglePlayAyah,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (orientation == QuranScrollDirection.HORIZONTAL) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            pageSpacing = space.lg,
            contentPadding = PaddingValues(vertical = space.md)
        ) { index -> page(index) }
    } else {
        VerticalPager(
            state = pagerState,
            modifier = modifier,
            pageSpacing = space.lg,
            contentPadding = PaddingValues(horizontal = space.md)
        ) { index -> page(index) }
    }
}

/** Width of the tap-to-turn gutters either side of a paged page. */
private val TapGutterWidth = 48.dp

/**
 * One tap-to-turn gutter.
 *
 * Invisible: it is a target, not a control. A reader who can see chevrons at
 * the edges of every page stops reading and starts swiping. It still carries an
 * accessible name and a role, because a screen-reader user turning pages by
 * swipe has no other route to the next one.
 */
@Composable
private fun TapZone(
    onTap: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onTap
            )
            .semantics { this.contentDescription = contentDescription }
    )
}

/**
 * The text scale at which a whole mushaf page fits the viewport.
 *
 * ### Why
 *
 * The page must be shown whole - see the note on [MushafPage] - so when it does
 * not fit, something has to give. Two things could: the page could scroll, or
 * the text could get smaller. Scrolling loses the reader's place, so the text
 * gives.
 *
 * The reader's own text-size setting is the **ceiling**, not the value. A page
 * that already fits is left at exactly the size they asked for. A page that does
 * not fit is reduced only as far as it has to be. Enlarging would be the other
 * way round and would be wrong: it would override a setting the reader made on
 * purpose, on most pages, to make the text smaller than the default.
 *
 * ### How it is measured
 *
 * The page's height is *estimated* from the corpus rather than measured by
 * laying it out and reading it back, because the alternatives are worse: laying
 * the page out at the requested size to discover it overflows would need a
 * second pass and would flash, and `AutoSizeText`-style subcomposition
 * remeasures on every scale change.
 *
 * The estimate is deliberately pessimistic - it assumes the worst case for
 * wrapping, so the page fits with a little room to spare rather than clipping
 * its last line by a fraction of a pixel. A little too small is invisible; a
 * little too large is a cut-off verse.
 */
@Composable
private fun rememberPageFitScale(
    ayahs: List<Ayah>,
    perVerse: Boolean,
    showTranslation: Boolean,
    requested: Float,
    availableWidth: Dp,
    availableHeight: Dp,
    reserved: Dp
): Float {
    val lineHeightSp = LocalQuranTypeface.current.lineHeightFactor *
        QuranReadingOptions.ARABIC_BASE_SP
    // Read here rather than inside the `remember` block, which is not a
    // composable scope: a density change (a display move, a font-scale change)
    // has to invalidate the fit, and reading it outside the block is what lets
    // `remember` key on it.
    val density = LocalDensity.current

    return remember(
        ayahs.size,
        perVerse,
        showTranslation,
        requested,
        availableWidth,
        availableHeight,
        reserved,
        density
    ) {
        if (availableHeight <= 0.dp || availableWidth <= 0.dp || ayahs.isEmpty()) {
            return@remember requested
        }

        val usable = (availableHeight - reserved).coerceAtLeast(1.dp)
        val widthPx = with(density) { availableWidth.toPx() }
        val charsPerLine = (widthPx / (lineHeightSp * 0.62f)).coerceAtLeast(8f)

        // Lines the Arabic occupies, wrapping pessimistically - one character per
        // line would be absurd, and one line per verse assumes no wrapping at
        // all, which is what makes the estimate safe.
        val arabicChars = ayahs.fold(0f) { acc, a -> acc + a.textArabic.length }
        val arabicLines = (arabicChars / charsPerLine).coerceAtLeast(ayahs.size.toFloat())
        val markerLines = ayahs.size.toFloat() * 0.15f
        val headingLines = 1.5f
        val translationLines = if (showTranslation) ayahs.size.toFloat() * 2.5f else 0f
        val verseGaps: Float = if (perVerse) ayahs.size * 0.35f else 0.35f
        val referenceChips = if (perVerse) ayahs.size.toFloat() else 0f

        val totalLines = arabicLines + markerLines + headingLines +
            translationLines + verseGaps + referenceChips
        val neededHeightDp = totalLines * lineHeightSp * 0.42f

        if (neededHeightDp <= usable.value) {
            requested
        } else {
            // Reduced to fit, never below the floor of the slider, and never
            // above what the reader asked for.
            (requested * usable.value / neededHeightDp)
                .coerceIn(0.5f, requested)
        }
    }
}

/**
 * One canonical mushaf page.
 *
 * The page *is* the unit, and it can begin in one surah and end in another, so
 * this resolves its own verses from the corpus rather than being handed the
 * current surah's. The running head names the surah the page opens in, which is
 * what the printed page does and what a reader checks when they turn back.
 *
 * ### Why the page fits rather than scrolls
 *
 * A mushaf page is a fixed object - these lines, in this order - so it is shown
 * whole. It used to scroll vertically on the horizontal axis and not on the
 * vertical one, which made the same page behave two different ways depending on a
 * setting whose only job is to change the direction of travel, and left a dense
 * page on the vertical axis simply cut off at the bottom with no way to reach
 * the missing lines.
 *
 * So it fits: see the note at the call site and [rememberPageFitScale]. The
 * reader's text size is the ceiling; the page shrinks to fit beneath it and is
 * never enlarged past it, which is what a printed page does when it does not fit
 * the paper.
 */
@Composable
private fun MushafPage(
    pageNumber: Int,
    orientation: QuranScrollDirection,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    state: SalahUiState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val ayahs = remember(pageNumber) { QuranBrowse.ayahsOnPage(pageNumber) }
    if (ayahs.isEmpty()) return

    val openingSurah = remember(pageNumber) { QuranBrowse.surah(ayahs.first().surahNumber) }

    // The running head clears the floating controls row, for the same reason the
    // surah heading does in the continuous layouts. It names the surah the page
    // opens in, which is the one thing a reader checks when turning back - so it
    // being half-hidden behind the pill is worse than not having it at all.
    val topInset = statusBarInset() + MaterialTheme.layoutMetrics.minTouchTarget +
        Space.current.lg

    // The page is shown whole, on every axis.
    //
    // A mushaf page is a fixed thing: these lines, on this paper, in this order.
    // A page that scrolls is not that - it is a window onto part of a page, and
    // the reader loses their place every time they look up, because "where am
    // I" becomes a scroll offset as well as a page number.
    //
    // The earlier arrangement scrolled on the horizontal axis and not on the
    // vertical one, so the same page behaved two different ways depending on a
    // setting whose entire job is to change the direction of travel. Worse, on
    // the vertical axis a dense page at a generous text size simply ran off the
    // bottom with its last lines cut, and the only way to reach them was to
    // pinch - a magnification gesture, not a "see the end of the page" gesture.
    //
    // So the page does not scroll on either axis. It *fits* instead: the text
    // scale is reduced until the whole page is inside the viewport. The
    // reader's own text-size setting is the upper bound, never exceeded, so a
    // generous size makes the page fit by getting smaller rather than by
    // scrolling - which is what a printed page does when it does not fit.
    BoxWithConstraints(modifier = modifier) {
        val fitScale = rememberPageFitScale(
            ayahs = ayahs,
            perVerse = options.perVerse,
            showTranslation = options.showTranslation,
            requested = options.arabicScale,
            availableWidth = maxWidth,
            availableHeight = maxHeight,
            // The running head and the reserved control inset are part of the
            // page's own height, so the space they take is subtracted before
            // fitting rather than being clipped by the fit.
            reserved = topInset + Space.current.sm
        )
        // The translation tracks the Arabic, so shrinking the page to fit shrinks
        // both and the proportion the reader chose is preserved. Clamped to the
        // slider's own range so a fit can never produce a value no slider can
        // express - which would be a preference the reader could not undo.
        val fittedTranslation = (options.translationScale *
            fitScale / options.arabicScale.coerceAtLeast(0.01f))
            .coerceIn(QuranReadingOptions.TranslationScaleRange)
        val fittedOptions = options.copy(
            arabicScale = fitScale,
            translationScale = fittedTranslation
        )

        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset, bottom = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = openingSurah?.englishName.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${strings.more.pageWord} $pageNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        if (options.perVerse) {
            // Per-verse on the mushaf. The page is still the page - the same
            // canonical boundary, the same running head - but its verses are
            // units you can select rather than one block of running text.
            ayahs.forEach { ayah ->
                PerVerseUnit(
                    ayah = ayah,
                    selected = selectedAyah == ayah.ayahNumber,
                    options = options,
                    ink = ink,
                    muted = muted,
                    actions = VerseActions(
                        ayah = ayah,
                        isBookmarked = state.isBookmarked(ayah),
                        isPlaying = state.isAudioPlaying &&
                            state.currentAudioAyah == ayah.ayahNumber,
                        onToggleBookmark = { onToggleBookmark(ayah) },
                        onTogglePlay = { onTogglePlayAyah(ayah) }
                    ),
                    onSelect = {
                        onSelectAyah(
                            if (selectedAyah == ayah.ayahNumber) 0 else ayah.ayahNumber
                        )
                    }
                )
                Spacer(Modifier.height(space.xs))
            }
            return@Column
        }

        FlowingTextSurface(
            ayahs = ayahs,
            selectedAyah = selectedAyah,
            arabicScale = options.arabicScale,
            ink = ink,
            accent = accent,
            onSelectAyah = onSelectAyah
        )

        if (options.showTranslation) {
            Spacer(Modifier.height(space.sm))
            ayahs.forEach { ayah ->
                VerseTranslationCard(
                    ayah = ayah,
                    translationScale = options.translationScale,
                    ink = ink,
                    surface = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = Space.current.xs)
                )
            }
        }
        }
    }
}

// ---------------------------------------------------------------------------
// Verse surfaces
// ---------------------------------------------------------------------------

/** The card that opens under a tapped verse in the flowing layout. */
@Composable
private fun VerseInspector(
    ayah: Ayah,
    actions: VerseActions,
    arabicScale: Float,
    ink: Color,
    muted: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = QuranShape.card,
        // The one 2dp accent border in the app: selection is expressed as a
        // thicker accent edge rather than a fill change or a shadow.
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.more.actionClose,
                    tint = muted,
                    modifier = Modifier
                        .size(IconSize.md)
                        .clip(QuranShape.pill)
                        .clickable(onClick = onDismiss)
                        .padding(IconSize.sm)
                )
            }

            Spacer(Modifier.height(space.sm))

            VerseArabic(
                ayah = ayah,
                scale = arabicScale,
                ink = ink,
                modifier = Modifier.padding(top = space.xs)
            )

            Spacer(Modifier.height(space.md))

            VerseTranslationCard(
                ayah = ayah,
                translationScale = 1f,
                ink = ink,
                surface = Color.Transparent,
                showReference = false
            )

            Spacer(Modifier.height(space.md))

            VerseActionRow(actions)
        }
    }
}

/**
 * The recitation banner.
 *
 * Tinted onto whatever paper is behind it rather than using the app's own
 * container, because on a violet wash an [androidx.compose.material3.Surface]
 * from the app scheme reads as a grey box pasted onto coloured paper.
 */
@Composable
private fun AudioStrip(
    surahName: String,
    surahNumber: Int,
    ayahNumber: Int,
    reciter: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    StatusBanner(
        message = "${strings.more.recitingLabel} $surahName · " +
            "${strings.more.verseReference.format(surahNumber, ayahNumber)} · $reciter",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        action = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = QuranShape.pill,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(QuranShape.pill)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onStop)
            ) {
                Text(
                    text = strings.more.stopAudio,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        horizontal = Space.current.lg,
                        vertical = Space.current.sm
                    )
                )
            }
        },
        modifier = modifier
    )
}

/** True when this colour is dark enough to need light ink on it. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Is [ayah] in the reader's bookmark list? */
private fun SalahUiState.isBookmarked(ayah: Ayah): Boolean = bookmarks.any {
    it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
}
