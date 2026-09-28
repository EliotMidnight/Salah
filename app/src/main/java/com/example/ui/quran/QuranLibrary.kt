package com.example.ui.quran

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.Motion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.RowDivider
import com.example.ui.components.SearchInput
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusDot
import com.example.ui.components.statusBarInset
import com.example.ui.theme.DotShape
import com.example.ui.theme.IconSize
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.delay

/** Diameter of the count badge on the floating bookmark control. */
private val CountBadgeSize = 18.dp

/** Thickness of the reading-progress track. */
private val ProgressTrackHeight = 4.dp

/** Width of the day/numeral column, shared by the surah tile and the reference row. */
private val NumberColumnWidth = 40.dp

/** Room the collapsed floating controls reserve at the top of a list. */
private val CollapsedControlsReserve = 60.dp

/** Room the open search bar (field + filter chips) reserves. */
private val ExpandedControlsReserve = 108.dp

/** What the library is showing. The pills are gone; this is now all there is. */
private enum class LibraryView { SURAHS, SAVED }

/** Which numbering scheme the reference filters are browsing. */
private enum class ReferenceKind { PAGE, JUZ, HIZB }

/**
 * Browse.
 *
 * There is no top bar and no tab row. Two floating controls sit in the top-right
 * corner - search and bookmarks - and nothing else, because the page has three
 * states and a row of four pills was three more controls than that needs.
 *
 * The search control is a button until you want it, and then it is a field: the
 * same button expands in place rather than opening a screen, so the page never
 * navigates away from the list you were reading. The three quick filters -
 * page, juz', hizb - live *inside* the expanded field, which is where the old
 * Reference tab's only job was. Choosing one browses that numbering; choosing
 * none searches verses.
 *
 * The Continue Reading card is the only accent-filled block on the page and
 * carries the heaviest type in the screen, which is what gives the library a
 * focal point instead of 114 equal rows.
 */
@Composable
fun QuranLibrary(
    state: SalahUiState,
    onOpenReader: () -> Unit,
    onSurahSelected: (Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onJuzSelected: (Int) -> Unit,
    onHizbSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    var view by rememberSaveable { mutableStateOf(LibraryView.SURAHS) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Null means "search the verses". A kind means "browse that numbering".
    var referenceKind by rememberSaveable { mutableStateOf<ReferenceKind?>(null) }

    // Verse search runs over all 6,236 verses. Debounced, because the previous
    // version filtered on every keystroke.
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    LaunchedEffect(query, referenceKind) {
        if (query.isBlank() || referenceKind != null) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        results = QuranDataSource.searchAyahs(query).take(50)
    }

    val listInset = contentTopInset(searchOpen)

    val closeSearch = {
        searchOpen = false
        referenceKind = null
        query = ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            view == LibraryView.SAVED -> SavedList(
                contentTopInset = listInset,
                bookmarks = state.bookmarks,
                onSelect = {
                    onSurahSelected(it.surahNumber)
                    onOpenReader()
                }
            )

            searchOpen && referenceKind != null -> ReferenceList(
                kind = referenceKind!!,
                query = query,
                contentTopInset = listInset,
                onSelectPage = onPageSelected,
                onSelectJuz = onJuzSelected,
                onSelectHizb = onHizbSelected,
                onOpenReader = onOpenReader
            )

            searchOpen -> SearchResults(
                query = query,
                onQueryChange = { query = it },
                contentTopInset = listInset,
                results = results,
                selectedSurahNumber = state.selectedSurah.number,
                onSurahSelected = onSurahSelected,
                onOpenReader = onOpenReader
            )

            else -> SurahList(
                state = state,
                contentTopInset = listInset,
                onSelect = onSurahSelected,
                onOpenReader = onOpenReader
            )
        }

        // The two controls. Right-aligned, floating, and the only things in the
        // top corner - the page's own header used to be a Material band above a
        // row of pills, which is two layers of chrome around a list.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth()
                .padding(top = topInset())
                .padding(horizontal = space.lg),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn(tween(Motion.duration(180))) +
                    expandHorizontally(
                        expandFrom = Alignment.End,
                        animationSpec = tween(Motion.duration(240))
                    ),
                exit = fadeOut(tween(Motion.duration(120))) +
                    shrinkHorizontally(
                        shrinkTowards = Alignment.End,
                        animationSpec = tween(Motion.duration(200))
                    )
            ) {
                ExpandedSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    referenceKind = referenceKind,
                    onKindChange = { referenceKind = it },
                    onClose = closeSearch
                )
            }

            if (!searchOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingCircleButton(
                        icon = Icons.Default.BookmarkBorder,
                        contentDescription = strings.bookmarksTab,
                        onClick = { view = LibraryView.SAVED },
                        badge = state.bookmarks.size,
                        selected = view == LibraryView.SAVED,
                        testTag = "quran_bookmark"
                    )
                    Spacer(Modifier.width(space.sm))
                    FloatingCircleButton(
                        icon = Icons.Default.Search,
                        contentDescription = strings.more.search,
                        onClick = {
                            view = LibraryView.SURAHS
                            searchOpen = true
                        },
                        testTag = "quran_search"
                    )
                }
            }
        }
    }
}

/** Status bar plus any display cutout, so the floating controls clear both. */
@Composable
private fun topInset(): Dp = statusBarInset() + Space.current.sm

/**
 * A circular floating control, with an optional count badge.
 *
 * `surfaceContainerHigh` on a `background` page rather than a Material filled
 * button: the page has no bar to sit in, so these need to read as objects
 * floating over the list instead of as a band that happens to have gaps in it.
 */
@Composable
private fun FloatingCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    badge: Int = 0,
    selected: Boolean = false
) {
    val space = Space.current
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            shape = QuranShape.pill,
            modifier = Modifier
                .size(MaterialTheme.layoutMetrics.minTouchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(testTag)
                .semantics {
                    this.contentDescription = contentDescription
                    if (badge > 0) stateDescription = "$badge"
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .size(CountBadgeSize)
                    .clip(DotShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 9) "9+" else "$badge",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * The search control, open: a field with the three quick filters beneath it.
 *
 * The filters are the whole reason this replaces a tab. Page, juz' and hizb are
 * three ways of asking "where is this", and burying them behind a tab made the
 * common case - look up today's page - two taps and a mode switch away.
 */
@Composable
private fun ExpandedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    referenceKind: ReferenceKind?,
    onKindChange: (ReferenceKind?) -> Unit,
    onClose: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = QuranShape.pill,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                SearchInput(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = strings.more.search,
                    onClear = if (query.isNotEmpty()) ({ onQueryChange("") }) else null
                )
            }
            Spacer(Modifier.width(space.xs))
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(MaterialTheme.layoutMetrics.minTouchTarget)
                    .testTag("quran_search_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.more.closeReader,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(space.xs))

        // Page / Juz' / Hizb. Selecting one browses that numbering; the leading
        // "verses" chip clears back to searching the text.
        Row(horizontalArrangement = Arrangement.spacedBy(space.xs)) {
            ReferenceChip(
                label = strings.more.versesLabel,
                selected = referenceKind == null,
                onClick = { onKindChange(null) }
            )
            ReferenceChip(
                label = strings.pageTab,
                selected = referenceKind == ReferenceKind.PAGE,
                onClick = { onKindChange(ReferenceKind.PAGE) }
            )
            ReferenceChip(
                label = strings.more.juzWord,
                selected = referenceKind == ReferenceKind.JUZ,
                onClick = { onKindChange(ReferenceKind.JUZ) }
            )
            ReferenceChip(
                label = strings.more.hizbWord,
                selected = referenceKind == ReferenceKind.HIZB,
                onClick = { onKindChange(ReferenceKind.HIZB) }
            )
        }
    }
}

@Composable
private fun ReferenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = QuranShape.pill,
        modifier = Modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) strings.more.selected else strings.more.notSelected }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = space.md, vertical = space.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

/**
 * How much room the first list item keeps clear at the top.
 *
 * The floating controls are only 48dp tall when collapsed, but the open search
 * bar adds a row of filter chips, so the two states need different reserves.
 * A single reserve large enough for both pushed the Continue Reading card a
 * whole row further down the page; a single one sized for the buttons let the
 * expanded field sit on top of the first list item.
 */
@Composable
private fun contentTopInset(searchOpen: Boolean): Dp =
    topInset() + if (searchOpen) ExpandedControlsReserve else CollapsedControlsReserve

@Composable
private fun SurahList(
    state: SalahUiState,
    contentTopInset: Dp,
    onSelect: (Int) -> Unit,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val resume = state.continueReading

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = space.xxxl)
    ) {
        item(key = "reserve") { Spacer(Modifier.height(contentTopInset)) }

        if (resume.surahNumber > 0) {
            item(key = "continue") {
                ContinueReadingHero(
                    surahName = resume.surahName,
                    reference = "${resume.surahNumber}:${resume.ayahNumber}",
                    onClick = {
                        onSelect(resume.surahNumber)
                        onOpenReader()
                    },
                    modifier = Modifier.padding(horizontal = space.lg)
                )
                Spacer(Modifier.height(space.xl))
            }
        }

        // The "Popular surahs" block is gone. It listed Al-Fatihah, Yasin, Al-Kahf,
        // Al-Mulk, Ar-Rahman and An-Nas, and every one of them also appeared in
        // the full list immediately below with an identical row and no marker to
        // tell the two apart - so six of the first rows were duplicates. On a
        // fresh install Al-Fatihah appeared three times on one screen, because it
        // is also what the Continue Reading hero resolves to by default. Search
        // and the alphabetical list already cover the need.
        item(key = "all_header") {
            SectionHeader(strings.more.allSurahsLabel)
        }

        items(QuranDataSource.SURAHS, key = { it.number }) { surah ->
            SurahRow(
                surah = surah,
                selected = state.selectedSurah.number == surah.number,
                onClick = {
                    onSelect(surah.number)
                    onOpenReader()
                }
            )
            RowDivider()
        }
    }
}

/**
 * The one accent-filled block on the library page.
 *
 * It is the only place `primaryContainer` is used here, and the surah name inside
 * it is the heaviest type in the screen. Everything below it is medium or
 * semi-bold, so the eye lands on "pick up where you left off" first and the
 * 114-row list second.
 */
@Composable
private fun ContinueReadingHero(
    surahName: String,
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = QuranShape.card,
        modifier = modifier
            .fillMaxWidth()
            .clickableHero(shape = QuranShape.card, onClick = onClick)
            .testTag("quran_continue")
    ) {
        Column(modifier = Modifier.padding(space.lg)) {
            Text(
                text = strings.continueReading,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(space.xxs))
            Text(
                text = surahName,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(space.xxs))
            Text(
                text = reference,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(space.md))
            ReadingProgress(reference = reference)
        }
    }
}

/** A determinate bar for "verse n of the surah", parsed from the reference. */
@Composable
private fun ReadingProgress(reference: String, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current
    val surahNumber = reference.substringBefore(':').toIntOrNull() ?: 0
    val ayahNumber = reference.substringAfter(':').toIntOrNull() ?: 0
    val total = QuranDataSource.getSurahByNumber(surahNumber)?.totalVerses ?: 0
    val fraction = if (total <= 0) 0f else (ayahNumber.toFloat() / total).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(ProgressTrackHeight)
                .clearAndSetSemantics { }
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = "${strings.more.verseOf.format(ayahNumber, total)} · ${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * One surah.
 *
 * A 40dp accent tile carrying the number, the English name and its meaning, and
 * the Arabic name set large and quiet on the right - the same string that is the
 * loudest thing in the reader header is deliberately the quietest thing here.
 */
@Composable
private fun SurahRow(
    surah: Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clickableHero(onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("surah_${surah.number}")
            // "You are here" for the surah currently open in the reader. Without
            // it, 114 identical rows gave no way to tell which one you had opened.
            .semantics { stateDescription = if (selected) strings.more.nowReading else "" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            shape = QuranShape.tile,
            modifier = Modifier.size(Space.current.huge)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(space.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.englishName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${strings.more.verseCount.format(surah.totalVerses)} · " +
                    if (surah.revelationType == RevelationType.MECCAN) {
                        strings.meccan
                    } else {
                        strings.medinan
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = surah.englishTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(space.sm))

        Text(
            text = surah.arabicName,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

// ---------------------------------------------------------------------------
// Reference: page / juz / hizb
// ---------------------------------------------------------------------------

@Composable
private fun ReferenceList(
    kind: ReferenceKind,
    query: String,
    contentTopInset: Dp,
    onSelectPage: (Int) -> Unit,
    onSelectJuz: (Int) -> Unit,
    onSelectHizb: (Int) -> Unit,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val space = Space.current
    val count = when (kind) {
        ReferenceKind.PAGE -> 604
        ReferenceKind.JUZ -> 30
        ReferenceKind.HIZB -> 60
    }
    val filtered = remember(kind, query) {
        val q = query.trim()
        if (q.isEmpty()) (1..count).toList() else (1..count).filter { it.toString().contains(q) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // The field and the Page/Juz'/Hizb selector both live in the floating
        // bar now. Leaving a copy here is what put a second outlined field and a
        // second set of chips on the page.
        Spacer(Modifier.height(contentTopInset))

        SectionHeader(
            when (kind) {
                ReferenceKind.PAGE -> strings.more.searchPages
                ReferenceKind.JUZ -> strings.more.searchJuz
                ReferenceKind.HIZB -> strings.more.searchHizb
            }
        )

        LazyColumn(contentPadding = PaddingValues(bottom = space.xxxl)) {
            items(filtered, key = { it }) { number ->
                ReferenceRow(
                    kind = kind,
                    number = number,
                    onClick = {
                        when (kind) {
                            ReferenceKind.PAGE -> onSelectPage(number)
                            ReferenceKind.JUZ -> onSelectJuz(number)
                            ReferenceKind.HIZB -> onSelectHizb(number)
                        }
                        onOpenReader()
                    }
                )
                RowDivider()
            }
        }
    }
}

@Composable
private fun ReferenceRow(
    kind: ReferenceKind,
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val title: String
    val detail: String?
    when (kind) {
        ReferenceKind.PAGE -> {
            val surah = QuranDataSource.surahForPage(number)
            val ayah = QuranDataSource.firstAyahOnPage(number)
            // The numeral already heads the row, so the title is the kind, not
            // the kind plus the number again. This used to render
            // "1 | Page 1 | Al-Fatihah - 1:1" - the number three times.
            title = strings.more.pageWord
            detail = surah?.let {
                "${it.englishName} · ${strings.more.verseReference.format(it.number, ayah?.ayahNumber ?: 1)}"
            }
        }

        ReferenceKind.JUZ -> {
            val ayah = QuranDataSource.firstAyahForJuz(number)
            title = strings.more.juzWord
            detail = ayah?.let {
                "${QuranDataSource.getSurahByNumber(it.surahNumber)?.englishName.orEmpty()} · " +
                    strings.more.verseReference.format(it.surahNumber, it.ayahNumber)
            }
        }

        ReferenceKind.HIZB -> {
            val ayah = QuranDataSource.firstAyahForHizb(number)
            title = strings.more.hizbWord
            // No "1st half" / "2nd half" here. The row stands for a whole hizb,
            // and firstAyahForHizb always resolves to the hizb's opening ayah -
            // whose quarter index is 4(n-1)+1, i.e. always odd. Every row
            // therefore claimed "1st half" and the 2nd-half string was
            // unreachable. Saying just the juz is true and sufficient.
            detail = ayah?.let { strings.more.juzOf.format(it.juzNumber) }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.tile)
            .clickableHero(onClick)
            .padding(horizontal = space.lg, vertical = space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(NumberColumnWidth)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@Composable
private fun SearchResults(
    query: String,
    onQueryChange: (String) -> Unit,
    contentTopInset: Dp,
    results: List<Ayah>,
    selectedSurahNumber: Int,
    onSurahSelected: (Int) -> Unit,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val matching = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            QuranDataSource.SURAHS.filter { surah ->
                surah.englishName.contains(query, ignoreCase = true) ||
                    surah.englishTranslation.contains(query, ignoreCase = true) ||
                    surah.arabicName.contains(QuranDataSource.normalizeArabic(query)) ||
                    surah.number.toString() == query.trim()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // No field here. The floating bar is the field; this used to render a
        // second one underneath the old pill row, which is where the duplicate
        // outline and the stray "search a surah" copy came from.
        Spacer(Modifier.height(contentTopInset))

        if (query.isBlank()) {
            EmptyState(
                title = strings.more.searchHintTitle,
                message = strings.more.searchHintMessage,
                painter = painterResource(R.drawable.salah_quran_search_ayah)
            )
            return@Column
        }

        if (matching.isEmpty() && results.isEmpty()) {
            // The body used to be the raw query, which told the user nothing
            // about what to do next, and EmptyState's action slot was never used
            // anywhere in the module - so both no-results screens were dead ends.
            EmptyState(
                title = strings.more.noSearchResults,
                message = strings.more.noResultsMessage,
                icon = Icons.Default.Search,
                action = {
                    TextButton(onClick = { onQueryChange("") }) {
                        Text(strings.more.clearSearch)
                    }
                }
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = space.xxxl)) {
            if (matching.isNotEmpty()) {
                item(key = "surahs") {
                    SectionHeader(strings.more.surahsFound.format(matching.size))
                }
                items(matching, key = { "s${it.number}" }) { surah ->
                    SurahRow(
                        surah = surah,
                        selected = selectedSurahNumber == surah.number,
                        onClick = {
                            onSurahSelected(surah.number)
                            onOpenReader()
                        }
                    )
                    RowDivider()
                }
            }

            if (results.isNotEmpty()) {
                item(key = "verses") {
                    SectionHeader(strings.more.versesFound.format(results.size))
                }
                items(results, key = { "v${it.surahNumber}_${it.ayahNumber}" }) { ayah ->
                    VerseResultCard(
                        ayah = ayah,
                        onClick = {
                            onSurahSelected(ayah.surahNumber)
                            onOpenReader()
                        }
                    )
                    RowDivider()
                }
            }
        }
    }
}

@Composable
private fun VerseResultCard(ayah: Ayah, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current
    val surah = QuranDataSource.getSurahByNumber(ayah.surahNumber)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuranShape.tile)
            .clickableHero(onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .semantics { contentDescription = "${surah?.englishName.orEmpty()} ${ayah.ayahNumber}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${surah?.englishName.orEmpty()} · ${ayah.surahNumber}:${ayah.ayahNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(space.xs))
            Text(
                text = "${ayah.textArabic} ۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = ArabicFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(space.xs))
            Text(
                text = ayah.textEnglish,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Saved
// ---------------------------------------------------------------------------

@Composable
private fun SavedList(
    contentTopInset: Dp,
    bookmarks: List<BookmarkEntity>,
    onSelect: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    if (bookmarks.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            Spacer(Modifier.height(contentTopInset))
            EmptyState(
                title = strings.more.noBookmarksTitle,
                message = strings.more.noBookmarksMessage,
                icon = Icons.Default.BookmarkBorder
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = space.xxxl)
    ) {
        item(key = "reserve") { Spacer(Modifier.height(contentTopInset)) }
        items(bookmarks, key = { "${it.surahNumber}_${it.ayahNumber}" }) { bookmark ->
            val ayah = QuranDataSource.resolveAyah(bookmark.surahNumber, bookmark.ayahNumber)
            val surah = QuranDataSource.getSurahByNumber(bookmark.surahNumber)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(QuranShape.tile)
                    .clickableHero(onClick = { onSelect(bookmark) })
                    .padding(horizontal = space.lg, vertical = space.md),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${surah?.englishName.orEmpty()} · ${bookmark.surahNumber}:${bookmark.ayahNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (ayah != null) {
                        Spacer(Modifier.height(space.xs))
                        Text(
                            text = ayah.textArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = ArabicFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // The trailing bookmark glyph is gone. Every row in the *bookmark*
                // list carried one, where it is true by definition: 20dp of
                // nothing per row, and the only icon in the list, so it pulled
                // the eye to the least informative element on the row.
            }
            RowDivider()
        }
    }
}

/**
 * Tappable row for every card-shaped surface on this screen.
 *
 * The button role is real now. Without it, TalkBack announced 800+ rows as
 * plain text and gave no hint that they were actions.
 *
 * The ripple shape is [QuranShape.tile]. Callers whose surface is a different
 * shape (the pill chips, the hero card) pass the matching shape, so the ripple
 * is not a 12dp-rectangle drawn inside a pill or a 20dp card.
 */
private fun Modifier.clickableHero(
    onClick: () -> Unit,
    shape: Shape = QuranShape.tile
): Modifier = this
    .clip(shape)
    .clickable(role = Role.Button, onClick = onClick)
