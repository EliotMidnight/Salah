package com.example.ui.quran

import androidx.compose.foundation.background
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
import com.example.ui.components.PillTabBarHeight
import com.example.ui.components.PillTabRow
import com.example.ui.components.SearchInput
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.delay

/** The four things you can do from the Quran library. */
private enum class LibraryTab { SURAHS, REFERENCE, SEARCH, SAVED }

/** Which numbering scheme the Reference tab is listing. */
private enum class ReferenceKind { PAGE, JUZ, HIZB }

/**
 * Browse.
 *
 * Four pill tabs float over the content rather than sitting in a Material band,
 * and the first block of every list reserves their height so nothing can slide
 * underneath. The Continue Reading card is the only accent-filled block on the
 * page and carries the heaviest type in the screen, which is what gives the
 * library a focal point instead of 114 equal rows.
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

    var tab by rememberSaveable { mutableStateOf(LibraryTab.SURAHS) }
    var query by rememberSaveable { mutableStateOf("") }
    var referenceKind by rememberSaveable { mutableStateOf(ReferenceKind.PAGE) }

    // Verse search runs over all 6,236 verses. Debounced, because the previous
    // version filtered on every keystroke.
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        results = QuranDataSource.searchAyahs(query).take(50)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (tab) {
            LibraryTab.SURAHS -> SurahList(
                state = state,
                onSelect = onSurahSelected,
                onOpenReader = onOpenReader
            )

            LibraryTab.REFERENCE -> ReferenceList(
                kind = referenceKind,
                query = query,
                onQueryChange = { query = it },
                onKindChange = { referenceKind = it },
                onSelectPage = onPageSelected,
                onSelectJuz = onJuzSelected,
                onSelectHizb = onHizbSelected,
                onOpenReader = onOpenReader
            )

            LibraryTab.SEARCH -> SearchResults(
                query = query,
                onQueryChange = { query = it },
                results = results,
                selectedSurahNumber = state.selectedSurah.number,
                onSurahSelected = onSurahSelected,
                onOpenReader = onOpenReader
            )

            LibraryTab.SAVED -> SavedList(
                bookmarks = state.bookmarks,
                onSelect = {
                    onSurahSelected(it.surahNumber)
                    onOpenReader()
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // A scrim behind the floating pills. Without it the list scrolled
                // visibly through the 8dp gaps between pills and the 16dp side
                // margins: unselected pills are `surface` on a `background` page,
                // two different colours, and the docstring's promise that
                // "nothing can slide underneath" only held at rest.
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(Modifier.height(topInset()))
            PillTabRow(
                tabs = listOf(
                    strings.more.surahsTab,
                    strings.more.referenceTab,
                    strings.more.search,
                    if (state.bookmarks.isEmpty()) {
                        strings.bookmarksTab
                    } else {
                        "${strings.bookmarksTab} ${state.bookmarks.size}"
                    }
                ),
                selectedIndex = tab.ordinal,
                onSelect = { tab = LibraryTab.entries[it] },
                modifier = Modifier.testTag("quran_tabs")
            )
        }
    }
}

/** Status bar plus any display cutout, so the floating tabs clear both. */
@Composable
private fun topInset(): Dp {
    val density = LocalDensity.current
    val top = maxOf(
        WindowInsets.statusBars.getTop(density),
        WindowInsets.displayCutout.getTop(density)
    )
    return with(density) { top.toDp() } + Space.current.sm
}

/**
 * The height every list reserves so its first row clears the floating tabs.
 *
 * [PillTabBarHeight] is only the pill. The row also carries
 * `padding(vertical = space.sm)` above and below, so the real occupied height
 * is PillTabBarHeight + 2 * sm. Reserving the bare pill height silently ate
 * the whole gap, which left the Continue Reading card, the search field and the
 * first bookmark row flush against the pills while a section label further
 * down got its full 16dp.
 */
@Composable
private fun tabBarReserve(): Dp = PillTabBarHeight + Space.current.sm * 2 + Space.current.lg

// ---------------------------------------------------------------------------
// Surahs
// ---------------------------------------------------------------------------

@Composable
private fun SurahList(
    state: SalahUiState,
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
        item(key = "reserve") { Spacer(Modifier.height(tabBarReserve())) }

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
            SectionLabel(strings.more.allSurahsLabel)
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                .height(4.dp)
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

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val space = Space.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = space.lg, top = space.lg, bottom = space.sm)
    )
}

// ---------------------------------------------------------------------------
// Reference: page / juz / hizb
// ---------------------------------------------------------------------------

@Composable
private fun ReferenceList(
    kind: ReferenceKind,
    query: String,
    onQueryChange: (String) -> Unit,
    onKindChange: (ReferenceKind) -> Unit,
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
        Spacer(Modifier.height(tabBarReserve()))

        SearchInput(
            value = query,
            onValueChange = onQueryChange,
            onClear = { onQueryChange("") },
            // "Search" was the placeholder over a filter that accepts digits
            // only, so it promised more than it did. Say which kind of number
            // is wanted; searchPages already existed and was never used.
            placeholder = when (kind) {
                ReferenceKind.PAGE -> strings.more.searchPages
                ReferenceKind.JUZ -> strings.more.searchJuz
                ReferenceKind.HIZB -> strings.more.searchHizb
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg)
        )

        Spacer(Modifier.height(space.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg),
            horizontalArrangement = Arrangement.spacedBy(space.sm)
        ) {
            ReferenceKind.entries.forEach { entry ->
                val selected = entry == kind
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = QuranShape.pill,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                        .clickableHero(
                            onClick = { onKindChange(entry) },
                            shape = QuranShape.pill
                        )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = space.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (entry) {
                                ReferenceKind.PAGE -> strings.more.pageWord
                                ReferenceKind.JUZ -> strings.more.juzWord
                                ReferenceKind.HIZB -> strings.more.hizbWord
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(space.sm))

        if (filtered.isEmpty()) {
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
            modifier = Modifier.width(40.dp)
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
        Spacer(Modifier.height(tabBarReserve()))

        SearchInput(
            value = query,
            onValueChange = onQueryChange,
            onClear = { onQueryChange("") },
            placeholder = strings.more.searchSurahsAndVerses,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg)
        )

        Spacer(Modifier.height(space.sm))

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
                    SectionLabel(strings.more.surahsFound.format(matching.size))
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
                    SectionLabel(strings.more.versesFound.format(results.size))
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
    bookmarks: List<BookmarkEntity>,
    onSelect: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    if (bookmarks.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            Spacer(Modifier.height(tabBarReserve()))
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
        item(key = "reserve") { Spacer(Modifier.height(tabBarReserve())) }
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

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
