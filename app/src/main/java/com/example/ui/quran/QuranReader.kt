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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    val listState = rememberLazyListState()
    val ayahPager = rememberPagerState(
        initialPage = (state.activeReadingAyahNumber - 1).coerceAtLeast(0),
        pageCount = { ayahs.size.coerceAtLeast(1) }
    )
    val pagePager = rememberPagerState(
        initialPage = (pageCursor - 1).coerceIn(0, TotalPages - 1),
        pageCount = { TotalPages }
    )

    // Re-seat on the anchor whenever the surah or layout changes, so changing
    // layout lands on the verse you were reading rather than the top.
    LaunchedEffect(surah.number, options.layout, options.scroll) {
        val target = state.activeReadingAyahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        when (options.layout) {
            QuranReadingLayout.PER_AYAH -> {
                val index = ayahs.indexOfFirst { it.ayahNumber == target }.coerceAtLeast(0)
                if (options.scroll == QuranScrollDirection.VERTICAL) {
                    listState.scrollToItem(index)
                } else {
                    ayahPager.scrollToPage(index)
                }
            }

            else -> Unit
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
        if (options.layout == QuranReadingLayout.PER_AYAH &&
            options.scroll == QuranScrollDirection.HORIZONTAL
        ) {
            snapshotFlow { ayahPager.currentPage }
                .debounce(400)
                .collect { page -> ayahs.getOrNull(page)?.let(onAyahViewed) }
            return@LaunchedEffect
        }
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
                        onViewScaleChange = setViewScale
                    )
                    // The view scale magnifies the reading surface only. It is
                    // applied above the scroll container so it zooms the text
                    // without dragging the gesture surfaces along with it.
                    .graphicsLayer {
                        scaleX = viewScale
                        scaleY = viewScale
                    }

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

                    options.layout == QuranReadingLayout.PER_AYAH &&
                        options.scroll == QuranScrollDirection.VERTICAL -> PerAyahList(
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

                    options.layout == QuranReadingLayout.PER_AYAH -> PerAyahPager(
                        surah = surah,
                        ayahs = ayahs,
                        pagerState = ayahPager,
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        state = state,
                        onToggleBookmark = onToggleBookmark,
                        onTogglePlayAyah = onTogglePlayAyah,
                        modifier = gesture.fillMaxSize()
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

            // In immersive mode the controls are gone, and the first thing a
            // tap does is bring them back rather than select a verse. Layered
            // over the page on purpose: it consumes the tap so "reveal the UI"
            // and "select this ayah" can never fire from the same touch.
            if (immersive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            onClickLabel = strings.more.reader.showControls,
                            role = androidx.compose.ui.semantics.Role.Button,
                            onClick = { onImmersiveChange(false) }
                        )
                        .testTag("reader_restore_controls")
                )
            }

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
                onToggleImmersive = { onImmersiveChange(true) },
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
 * The whole chrome: one pill and three round buttons, in a single row.
 *
 * The pill is leading - top-left in LTR, top-right in RTL, without a single
 * conditional, because [Row] mirrors with the layout direction. It carries the
 * reader's location, so the one thing you always want is the thing that is
 * always there, and it doubles as the way into the index.
 *
 * The three buttons are trailing and equal in weight. Immersive comes before
 * options because it is the one a reader reaches for most: it is the control
 * that makes the rest of this row go away.
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
    onToggleImmersive: () -> Unit,
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
                    .padding(horizontal = Space.current.lg),
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
                    icon = Icons.Default.TextFields,
                    contentDescription = strings.more.reader.immersiveMode,
                    tint = muted,
                    onClick = onToggleImmersive,
                    testTag = "reader_immersive"
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
 * The location, as one thin pill, and the way into the index.
 *
 * Thin because it sits over text: a full-height bar here would be a header
 * again, which is the thing this screen exists not to be. The surah name leads
 * and the reference follows, because the name is what you recognise and the
 * number is what you check.
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
                    "${strings.more.selectSurah}: ${surah.englishName}"
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
                    surah.number.toString()
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

/** One verse per screen, turned sideways. Reading a passage beat by beat. */
@Composable
private fun PerAyahPager(
    surah: Surah,
    ayahs: List<Ayah>,
    pagerState: androidx.compose.foundation.pager.PagerState,
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

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = space.lg,
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) { page ->
        val ayah = ayahs.getOrNull(page)
        if (ayah == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spacer(Modifier.height(space.lg)) }
            return@HorizontalPager
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(space.lg))
            VerseReferenceChip("${surah.number}:${ayah.ayahNumber}")
            Spacer(Modifier.height(space.lg))
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
                fillMaxWidth = false
            )
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

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageShown(it) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = space.lg,
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) { page ->
        MushafPage(
            pageNumber = page + 1,
            options = options,
            ink = ink,
            accent = accent,
            muted = muted,
            selectedAyah = 0,
            onSelectAyah = {},
            modifier = Modifier.fillMaxSize()
        )
    }
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
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val ayahs = remember(pageNumber) { QuranDataSource.getAyahsForPage(pageNumber) }
    if (ayahs.isEmpty()) return

    val openingSurah = remember(pageNumber) { QuranDataSource.getSurahByNumber(ayahs.first().surahNumber) }

    Column(modifier = modifier) {
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
