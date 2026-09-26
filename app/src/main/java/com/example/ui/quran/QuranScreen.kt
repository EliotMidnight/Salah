package com.example.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.ScreenTopBar
import com.example.ui.components.SectionGroup
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.delay

/** The five ways into the text. */
private enum class QuranTab { SURAHS, PAGES, JUZ, HIZB, SAVED }

/**
 * Browse, then read.
 *
 * Two modes in one destination, which is what it has always been - the original
 * code just failed to make that obvious, because the mode switch was a single
 * unlabelled icon in a shadowed toolbar and pressing the system back button
 * exited the whole app rather than returning to the list. Both are fixed here:
 * there is an explicit back affordance, [BackHandler] returns to the library, and
 * the mode is visible in the top bar.
 */
@Composable
fun QuranScreen(
    state: SalahUiState,
    onSurahSelected: (Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onPageSelected: (Int) -> Unit = {},
    onJuzSelected: (Int) -> Unit = {},
    onHizbSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inReader by rememberSaveable { mutableStateOf(false) }

    if (inReader) {
        BackHandler { inReader = false }
        QuranReader(
            state = state,
            onBack = { inReader = false },
            onSelectSurah = {
                onSurahSelected(it)
                inReader = true
            },
            onAyahViewed = onAyahViewed,
            onToggleBookmark = onToggleBookmark,
            onTogglePlayAyah = onTogglePlayAyah,
            onStopAudio = onStopAudio,
            onFontScaleChange = onFontScaleChange,
            modifier = modifier
        )
    } else {
        QuranLibrary(
            state = state,
            onOpenReader = { inReader = true },
            onSurahSelected = {
                onSurahSelected(it)
                inReader = true
            },
            onPageSelected = {
                onPageSelected(it)
                inReader = true
            },
            onJuzSelected = {
                onJuzSelected(it)
                inReader = true
            },
            onHizbSelected = {
                onHizbSelected(it)
                inReader = true
            },
            modifier = modifier
        )
    }
}

// ---------------------------------------------------------------------------
// Library
// ---------------------------------------------------------------------------

@Composable
private fun QuranLibrary(
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

    var tab by rememberSaveable { mutableStateOf(QuranTab.SURAHS) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    // Search runs against the whole 6,236-verse corpus. Debounced, because the
    // previous version filtered on every keystroke and ran four regex passes per
    // verse on the main thread.
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    LaunchedEffect(query, tab) {
        if (query.isBlank() || tab != QuranTab.SURAHS) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        results = QuranDataSource.searchAyahs(query).take(50)
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenTopBar(
            title = strings.more.quranTitle,
            subtitle = strings.more.corpusSummary,
            actions = {
                IconButton(
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    },
                    modifier = Modifier
                        .size(MaterialTheme.layoutMetrics.minTouchTarget)
                        .testTag("quran_search_toggle")
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchOpen) {
                            strings.more.clearSearch
                        } else {
                            strings.more.search
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (searchOpen) {
            com.example.ui.components.SearchInput(
                value = query,
                onValueChange = { query = it },
                onClear = { query = "" },
                placeholder = strings.more.searchSurahsAndVerses,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg)
            )
            Spacer(Modifier.height(space.sm))
        }

        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("quran_tabs")
        ) {
            QuranTab.entries.forEach { entry ->
                val label = when (entry) {
                    QuranTab.SURAHS -> strings.more.surahsTab
                    QuranTab.PAGES -> strings.pageTab
                    QuranTab.JUZ -> strings.more.juzWord
                    QuranTab.HIZB -> strings.more.hizbWord
                    QuranTab.SAVED -> strings.bookmarksTab
                }
                val count = if (entry == QuranTab.SAVED && state.bookmarks.isNotEmpty()) {
                    " (${state.bookmarks.size})"
                } else {
                    ""
                }
                Tab(
                    selected = tab == entry,
                    onClick = { tab = entry },
                    modifier = Modifier.semantics { contentDescription = label + count }
                ) {
                    Text(
                        text = label + count,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = space.md)
                    )
                }
            }
        }

        val resume = state.continueReading
        if (resume.surahNumber > 0 && tab == QuranTab.SURAHS && query.isBlank()) {
            Spacer(Modifier.height(space.md))
            ContinueReadingStrip(
                surahName = resume.surahName,
                reference = "${resume.surahNumber}:${resume.ayahNumber}",
                onClick = {
                    onSurahSelected(resume.surahNumber)
                    onOpenReader()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when (tab) {
                QuranTab.SURAHS -> SurahList(
                    query = query,
                    results = results,
                    searching = query.isNotBlank(),
                    onSelect = onSurahSelected,
                    onOpenVerse = onOpenReader
                )

                QuranTab.PAGES -> ReferenceList(
                    query = query,
                    count = 604,
                    onSelect = onPageSelected,
                    label = { page -> "${strings.more.pageWord} $page" },
                    detail = { page ->
                        QuranDataSource.surahForPage(page)?.let { "${it.englishName} · ${strings.more.verseReference.format(it.number, QuranDataSource.firstAyahOnPage(page)?.ayahNumber ?: 1)}" }
                    }
                )

                QuranTab.JUZ -> ReferenceList(
                    query = query,
                    count = 30,
                    onSelect = onJuzSelected,
                    label = { juz -> strings.more.juzOf.format(juz) },
                    detail = { juz ->
                        QuranDataSource.firstAyahForJuz(juz)?.let { "${QuranDataSource.getSurahByNumber(it.surahNumber)?.englishName.orEmpty()} · ${strings.more.verseReference.format(it.surahNumber, it.ayahNumber)}" }
                    }
                )

                QuranTab.HIZB -> ReferenceList(
                    query = query,
                    count = 60,
                    onSelect = onHizbSelected,
                    label = { hizb -> strings.more.hizbOf.format(hizb) },
                    detail = { hizb ->
                        QuranDataSource.firstAyahForHizb(hizb)?.let {
                            val juz = it.juzNumber
                            val half = if ((it.hizbQuarter % 2) == 1) {
                                strings.more.hizbHalfFirst
                            } else {
                                strings.more.hizbHalfSecond
                            }
                            strings.more.hizbInJuz.format(juz, half)
                        }
                    }
                )

                QuranTab.SAVED -> BookmarkList(
                    bookmarks = state.bookmarks,
                    onSelect = { bookmark ->
                        onSurahSelected(bookmark.surahNumber)
                        onOpenReader()
                    }
                )
            }
        }
    }
}

/** Where the user left off. One row, no card-in-card. */
@Composable
private fun ContinueReadingStrip(
    surahName: String,
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .testTag("quran_continue")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = space.lg, vertical = space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.continueReading,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$surahName · $reference",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = strings.continueButton,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The surah list, or verse results when a search is active. */
@Composable
private fun SurahList(
    query: String,
    results: List<Ayah>,
    searching: Boolean,
    onSelect: (Int) -> Unit,
    onOpenVerse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val listState = rememberLazyListState()

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

    if (searching && matching.isEmpty() && results.isEmpty()) {
        EmptyState(
            title = strings.more.noSearchResults,
            message = query,
            icon = Icons.Default.Search,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.current.lg, vertical = Space.current.sm)
    ) {
        if (searching && matching.isNotEmpty()) {
            item(key = "surah_count") {
                Text(
                    text = strings.more.surahsFound.format(matching.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Space.current.xs)
                )
            }
            itemsIndexed(matching, key = { _, surah -> "s${surah.number}" }) { index, surah ->
                if (index > 0) RowDivider()
                SurahRow(surah = surah, onClick = { onSelect(surah.number) })
            }
        }

        if (searching && results.isNotEmpty()) {
            item(key = "ayah_count") {
                Text(
                    text = strings.more.versesFound.format(results.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.current.md, bottom = Space.current.xs)
                )
            }
            itemsIndexed(results, key = { _, ayah -> "v${ayah.surahNumber}_${ayah.ayahNumber}" }) { index, ayah ->
                if (index > 0) RowDivider()
                VerseResultRow(
                    ayah = ayah,
                    onClick = {
                        onSelect(ayah.surahNumber)
                        onOpenVerse()
                    }
                )
            }
        }

        if (!searching) {
            itemsIndexed(QuranDataSource.SURAHS, key = { _, surah -> surah.number }) { index, surah ->
                if (index > 0) RowDivider()
                SurahRow(surah = surah, onClick = { onSelect(surah.number) })
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(vertical = space.md)
            .testTag("surah_${surah.number}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = surah.number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp)
        )
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
                    if (surah.revelationType == com.example.data.model.RevelationType.MECCAN) {
                        strings.meccan
                    } else {
                        strings.medinan
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(space.sm))
        Text(
            text = surah.arabicName,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
private fun VerseResultRow(ayah: Ayah, onClick: () -> Unit) {
    val space = Space.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(vertical = space.md),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = strings_ref(ayah),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(64.dp)
                .clearAndSetSemantics { }
        )
        Spacer(Modifier.width(space.sm))
        Column(modifier = Modifier.weight(1f)) {
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

@Composable
private fun strings_ref(ayah: Ayah): String = "${ayah.surahNumber}:${ayah.ayahNumber}"

/**
 * Numbered reference lists (pages, juz, hizb).
 *
 * One implementation for all three. They were three near-identical LazyColumns
 * that had drifted apart, and the juz and hizb versions used exact `==` matching
 * while the page version used `contains`, so typing "1" behaved differently
 * depending on which tab you were on.
 */
@Composable
private fun ReferenceList(
    query: String,
    count: Int,
    onSelect: (Int) -> Unit,
    label: (Int) -> String,
    detail: (Int) -> String?,
    modifier: Modifier = Modifier
) {
    val filtered = remember(query, count) {
        val q = query.trim()
        if (q.isEmpty()) {
            (1..count).toList()
        } else {
            (1..count).filter { it.toString() == q || it.toString().contains(q) }
        }
    }

    if (filtered.isEmpty()) {
        EmptyState(
            title = LocalStrings.current.more.noSearchResults,
            message = query,
            icon = Icons.Default.Search,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.current.lg, vertical = Space.current.sm)
    ) {
        itemsIndexed(filtered, key = { _, number -> number }) { index, number ->
            if (index > 0) RowDivider()
            val space = Space.current
            val detailText = remember(number) { detail(number) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { onSelect(number) }
                    .padding(vertical = space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label(number),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (detailText != null) {
                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Saved verses, with a real empty state. */
@Composable
private fun BookmarkList(
    bookmarks: List<BookmarkEntity>,
    onSelect: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    if (bookmarks.isEmpty()) {
        EmptyState(
            title = strings.more.noBookmarksTitle,
            message = strings.more.noBookmarksMessage,
            icon = Icons.Default.BookmarkBorder,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.sm)
    ) {
        itemsIndexed(bookmarks, key = { _, bookmark -> "${bookmark.surahNumber}_${bookmark.ayahNumber}" }) { index, bookmark ->
            if (index > 0) RowDivider()
            val ayah = QuranDataSource.resolveAyah(bookmark.surahNumber, bookmark.ayahNumber)
            val surah = QuranDataSource.getSurahByNumber(bookmark.surahNumber)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { onSelect(bookmark) }
                    .padding(vertical = space.md),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${surah?.englishName.orEmpty()} · ${bookmark.surahNumber}:${bookmark.ayahNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (ayah != null) {
                        Spacer(Modifier.height(space.xs))
                        Text(
                            text = ayah.textArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = ArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .clearAndSetSemantics { }
                )
            }
        }
    }
}

@Composable
private fun RowDivider() {
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
