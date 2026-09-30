package com.example.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
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
        QuranDataSource.resolveAyah(surah.number, state.activeReadingAyahNumber)?.pageNumber ?: 1
    }

    var selectedAyah by rememberSaveable { mutableStateOf(0) }

    // The page cursor for the mushaf layouts. Kept as its own state because the
    // anchor is a verse and the mushaf is paginated by *page* - a page can begin
    // in one surah and end in the next, so there is no verse that stands for
    // "page 300". It is seeded from the anchor and written back to it on every
    // turn, which is what keeps Continue Reading and bookmarks truthful.
    var pageCursor by rememberSaveable(anchorPage) { mutableIntStateOf(anchorPage) }

    val (viewScale, setViewScale) = rememberViewScale("${options.layout}-${options.scroll}")
    // The magnified view can be dragged around. Only meaningful while
    // VIEW_SCALE is the pinch target and the scale is above 1 - `readerPinch`
    // decides when to forward a drag, and `rememberPan` clamps it to the
    // surplus the scale actually created.
    var readingSize by remember { mutableStateOf(IntSize.Zero) }
    val (pan, setPan) = rememberPan(viewScale, readingSize)

    val listState = rememberLazyListState()
    val pagePager = rememberPagerState(
        initialPage = (pageCursor - 1).coerceIn(0, TotalPages - 1),
        pageCount = { TotalPages }
    )

    // Re-seat on the anchor whenever the surah or layout changes, so changing
    // layout lands on the verse you were reading rather than the top.
    LaunchedEffect(surah.number, options.layout, options.scroll) {
        val target = state.activeReadingAyahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        if (options.layout == QuranReadingLayout.PER_AYAH) {
            val index = ayahs.indexOfFirst { it.ayahNumber == target }.coerceAtLeast(0)
            listState.scrollToItem(index)
        }
        selectedAyah = if (options.layout == QuranReadingLayout.CONTINUOUS) target else 0
    }

    // Write the page cursor back into the anchor, so Continue Reading follows
    // the mushaf as it is actually turned and not only the surah view.
    LaunchedEffect(pageCursor) {
        if (options.layout != QuranReadingLayout.PER_PAGE) return@LaunchedEffect
        QuranDataSource.firstAyahOnPage(pageCursor)?.let { first ->
            onSelectSurahAyah(first.surahNumber, first.ayahNumber)
        }
    }

    // Progress, debounced. This used to write to the database on every scroll
    // step, which is both slow and unnecessary: nobody needs their position
    // recorded more precisely than a verse.
    LaunchedEffect(listState, options.layout, options.scroll) {
        if (options.layout == QuranReadingLayout.PER_PAGE) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(400)
            .collect { index -> ayahs.getOrNull(index)?.let(onAyahViewed) }
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
                        onPanChange = setPan
                    )
                    // The view scale magnifies the reading surface and can then
                    // be dragged around. It is applied above the scroll
                    // container so it zooms the text without dragging the
                    // gesture surfaces along with it. `clip = false` keeps a
                    // magnified line from being cut off at the page edge while
                    // it is being dragged into view.
                    .graphicsLayer {
                        scaleX = viewScale
                        scaleY = viewScale
                        translationX = pan.x
                        translationY = pan.y
                        clip = false
                    }
                    .onSizeChanged { readingSize = it }

                val verticalSwipe = if (options.isPaged) {
                    Modifier
                } else {
                    Modifier.swipeToChangeSurah(surah.number, onChange = onSelectSurah)
                }

                when {
                    options.layout == QuranReadingLayout.CONTINUOUS -> ContinuousSurah(
                        surah = surah,
                        ayahs = ayahs,
                        selectedAyah = selectedAyah,
                        onSelectAyah = { selectedAyah = it },
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        state = state,
                        onToggleBookmark = onToggleBookmark,
                        onTogglePlayAyah = onTogglePlayAyah,
                        modifier = gesture.then(verticalSwipe).fillMaxSize()
                    )

                    // Per ayah is one column either way. A horizontal pager here
                    // put a single verse on the whole screen and turned the
                    // rest of the surah into a carousel, which is a slideshow
                    // and not a page of reading - so both axes now render the
                    // same stack of verses, filling the page.
                    options.layout == QuranReadingLayout.PER_AYAH -> PerAyahList(
                        surah = surah,
                        ayahs = ayahs,
                        listState = listState,
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        state = state,
                        onToggleBookmark = onToggleBookmark,
                        onTogglePlayAyah = onTogglePlayAyah,
                        modifier = gesture.then(verticalSwipe).fillMaxSize()
                    )

                    options.scroll == QuranScrollDirection.HORIZONTAL -> MushafPager(
                        pagerState = pagePager,
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        onPageShown = { pageCursor = it + 1 },
                        modifier = gesture.fillMaxSize()
                    )

                    else -> MushafList(
                        listState = listState,
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        onPageShown = { pageCursor = it + 1 },
                        modifier = gesture.then(verticalSwipe).fillMaxSize()
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
                anchorPage = anchorPage,
                pageCursor = pageCursor,
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
                    QuranDataSource.resolveAyah(surah.number, state.activeReadingAyahNumber)
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
                    surahName = QuranDataSource.getSurahByNumber(surah.number)?.englishName.orEmpty(),
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

            // The page tag. Only in the layouts where a page is a thing the
            // reader can lose track of, and only while the controls are up -
            // it exists to answer "which page am I on", and the moment the user
            // is reading rather than navigating, it is noise.
            if (!immersive && options.isPaged) {
                PageTag(
                    page = if (options.layout == QuranReadingLayout.PER_PAGE) {
                        pageCursor
                    } else {
                        anchorPage
                    },
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
    anchorPage: Int,
    pageCursor: Int,
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
                    page = if (layout == QuranReadingLayout.PER_PAGE) {
                        pageCursor
                    } else {
                        anchorPage
                    },
                    showPage = layout == QuranReadingLayout.PER_PAGE,
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
 * The surah's identity block.
 *
 * Kept from the previous reader, because it was already right: the Arabic name
 * in the accent at display size is the one place this screen uses the accent at
 * headline scale, which is what makes a surah feel like an opening rather than
 * a list entry, and the basmalah is separated from it by more space than
 * anything else on the page.
 */
@Composable
private fun SurahHeading(surah: Surah, ink: Color, muted: Color, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.lg)
            .surahHeading(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "سُورَةُ ${surah.arabicName}",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = surah.englishName,
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            textAlign = TextAlign.Center
        )
        Text(
            text = surah.englishTranslation,
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = "${strings.more.verseCount.format(surah.totalVerses)} · " +
                if (surah.revelationType == RevelationType.MECCAN) {
                    strings.meccan
                } else {
                    strings.medinan
                },
            style = MaterialTheme.typography.labelSmall,
            color = muted
        )
        // Al-Fatihah opens with the basmalah as its first verse, so showing it
        // separately would print it twice. At-Tawbah has none at all.
        if (surah.number != 1 && surah.number != 9) {
            Spacer(Modifier.height(space.lg))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall,
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

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        shape = QuranShape.card,
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
                append("۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}")
            }
            append(" ")

            spans += start until length
        }
    }
    return FlowingPage(text, spans)
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

/** Whole-surah flowing text, scrolled vertically. The default. */
@Composable
private fun ContinuousSurah(
    surah: Surah,
    ayahs: List<Ayah>,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    state: SalahUiState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "heading") {
            SurahHeading(surah = surah, ink = ink, muted = muted)
        }
        item(key = "page") {
            FlowingTextSurface(
                ayahs = ayahs,
                selectedAyah = selectedAyah,
                arabicScale = options.arabicScale,
                ink = ink,
                accent = accent,
                onSelectAyah = onSelectAyah
            )
        }
        if (selectedAyah > 0) {
            item(key = "inspector") {
                ayahs.firstOrNull { it.ayahNumber == selectedAyah }?.let { ayah ->
                    Spacer(Modifier.height(space.md))
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
                        modifier = Modifier.testTag("ayah_inspector")
                    )
                }
            }
        }
        if (options.showTranslation) {
            item(key = "translations") {
                Spacer(Modifier.height(space.md))
                ayahs.forEach { ayah ->
                    VerseTranslationCard(
                        ayah = ayah,
                        translationScale = options.translationScale,
                        ink = ink,
                        surface = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = Space.current.xs)
                    )
                }
            }
        }
    }
}

/** One block per verse, scrolled vertically. The working/study layout. */
@Composable
private fun PerAyahList(
    surah: Surah,
    ayahs: List<Ayah>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    state: SalahUiState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "heading") {
            SurahHeading(surah = surah, ink = ink, muted = muted)
        }
        itemsIndexed(
            ayahs,
            key = { _, a -> a.ayahNumber }
        ) { _, ayah ->
            VerseBlock(
                ayah = ayah,
                actions = VerseActions(
                    ayah = ayah,
                    isBookmarked = state.isBookmarked(ayah),
                    isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                    onToggleBookmark = { onToggleBookmark(ayah) },
                    onTogglePlay = { onTogglePlayAyah(ayah) }
                ),
                options = options,
                ink = ink,
                muted = muted,
                showTranslation = options.showTranslation,
                modifier = Modifier.testTag("ayah_${ayah.ayahNumber}")
            )
            Spacer(Modifier.height(space.md))
        }
    }
}

/** The real mushaf, turned sideways: one canonical page per swipe. */
@Composable
private fun MushafPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    onPageShown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageShown(it) }
    }

    val turn: (Int) -> Unit = { delta ->
        val next = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
        scope.launch { pagerState.animateScrollToPage(next) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = space.lg,
        contentPadding = PaddingValues(vertical = space.md)
    ) { page ->
        // `scrollable = true` because a page has to be able to be taller than
        // the screen. The Arabic is sized by the reader, not by the page, so a
        // generous text size on a dense page overflows - and with nothing to
        // scroll it, the overflow was clipped and the last lines of the page
        // were simply gone. The page scrolls now.
        //
        // The two axes are different, so the parent keeps the horizontal drag:
        // the pager turns the page, this column scrolls within it.
        Row(Modifier.fillMaxSize()) {
            // Tap-to-turn, as two gutters flanking the page.
            //
            // Deliberately *not* a full-width tap handler on the page: the text
            // already claims taps to select a verse, and a parent that consumed
            // them would either break verse selection or make turning the page
            // and selecting a verse mutually exclusive. These sit in the margin
            // where there is no text to select, so turning and reading coexist.
            //
            // A full 48dp each, so neither is a target you have to aim at.
            TapZone(
                onTap = { turn(-1) },
                enabled = page > 0,
                contentDescription = strings.more.previousSurahLabel,
                modifier = Modifier.width(TapGutterWidth)
            )

            MushafPage(
                pageNumber = page + 1,
                options = options,
                ink = ink,
                accent = accent,
                muted = muted,
                selectedAyah = 0,
                onSelectAyah = {},
                scrollable = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            TapZone(
                onTap = { turn(1) },
                enabled = page < TotalPages - 1,
                contentDescription = strings.more.nextSurahLabel,
                modifier = Modifier.width(TapGutterWidth)
            )
        }
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

/** The real mushaf, scrolled vertically: one canonical page per row. */
@Composable
private fun MushafList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    onPageShown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        items(count = TotalPages, key = { it + 1 }) { index ->
            MushafPage(
                pageNumber = index + 1,
                options = options,
                ink = ink,
                accent = accent,
                muted = muted,
                selectedAyah = 0,
                onSelectAyah = {},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(space.lg))
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(300)
            .collect { onPageShown(it) }
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
 * [scrollable] is true in the horizontal pager and false in the vertical list,
 * and the difference matters. In the list each page is already a row of a
 * scrolling parent, so an inner scroll would be a second vertical drag
 * competing with it. In the pager the page owns its own height, so without this
 * flag a page taller than the screen had nowhere to go but the clip.
 */
@Composable
private fun MushafPage(
    pageNumber: Int,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    muted: Color,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val space = Space.current
    val strings = LocalStrings.current

    val ayahs = remember(pageNumber) { QuranDataSource.getAyahsForPage(pageNumber) }
    if (ayahs.isEmpty()) return

    val openingSurah = remember(pageNumber) { QuranDataSource.getSurahByNumber(ayahs.first().surahNumber) }

    Column(
        modifier = modifier.then(
            if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = space.sm),
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

// ---------------------------------------------------------------------------
// Verse surfaces
// ---------------------------------------------------------------------------

/**
 * One ayah as its own block: reference and actions above, text below.
 *
 * [fillMaxWidth] is false in the horizontal layout, where the block is a card on
 * a page rather than a row in a column and should be bounded by the reading
 * measure rather than running to the bezel.
 */
@Composable
private fun VerseBlock(
    ayah: Ayah,
    actions: VerseActions,
    options: QuranReadingOptions,
    ink: Color,
    muted: Color,
    showTranslation: Boolean,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true
) {
    val space = Space.current

    Column(
        modifier = modifier
            .then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    // Bounded by the app's reading measure rather than running to
                    // the bezel, so a single verse on a turned page is still a
                    // comfortable line length.
                    Modifier.widthIn(max = MaterialTheme.layoutMetrics.contentMaxWidth)
                }
            )
            .clip(QuranShape.card)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerseReferenceChip("${ayah.surahNumber}:${ayah.ayahNumber}")
            Spacer(Modifier.weight(1f))
            PlayingDot(actions.isPlaying)
            if (actions.isPlaying) Spacer(Modifier.width(space.sm))
            VerseActionRow(actions)
        }

        Spacer(Modifier.height(space.md))

        VerseArabic(
            ayah = ayah,
            scale = options.arabicScale,
            ink = ink,
            modifier = if (fillMaxWidth) Modifier else Modifier.fillMaxWidth(0.9f)
        )

        if (showTranslation) {
            Spacer(Modifier.height(space.md))
            VerseTranslationCard(
                ayah = ayah,
                translationScale = options.translationScale,
                ink = ink,
                surface = Color.Transparent,
                showReference = false
            )
        }
    }
}

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
