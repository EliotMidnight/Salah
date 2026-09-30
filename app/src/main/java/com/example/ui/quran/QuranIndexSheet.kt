package com.example.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.QuranFontFace
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.components.EmptyState
import com.example.ui.components.OptionSheet
import com.example.ui.components.SearchInput
import com.example.ui.components.SheetHeader
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.IconSize
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.delay

/** What the index is showing. */
private enum class IndexView { SURAHS, SAVED, SEARCH }

/** Which numbering the search field is browsing. Null means "the verses". */
private enum class ReferenceKind { PAGE, JUZ, HIZB }

/** Filters on the surah list that are not a text query. */
private enum class SurahFilter { ALL, MECCAN, MEDINAN }

/**
 * The index.
 *
 * One sheet with three faces - surahs, saved, search - because they are three
 * ways of answering the same question, "where do I go next", and they used to
 * be three screens plus a tab row to move between them.
 *
 * It opens as a sheet over the reader rather than replacing it, so the text you
 * were reading is still behind it when you close it again. That matters more
 * here than on a normal screen: arriving somewhere is the end of a journey in
 * a list, and the middle of a breath in a reader.
 *
 * The search field carries its own quick filters (page, juz', hizb) *inside* it,
 * which is what replaced the Reference tab's only job.
 */
@Composable
fun QuranIndexSheet(
    currentSurah: Int,
    bookmarks: List<BookmarkEntity>,
    onSelectSurah: (Int) -> Unit,
    onSelectSurahAyah: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    var view by rememberSaveable { mutableStateOf(IndexView.SURAHS) }
    var query by rememberSaveable { mutableStateOf("") }
    var referenceKind by rememberSaveable { mutableStateOf<ReferenceKind?>(null) }
    var surahFilter by rememberSaveable { mutableStateOf(SurahFilter.ALL) }

    // Verse search runs over all 6,236 verses, so it is debounced rather than
    // run on every keystroke.
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    LaunchedEffect(query, referenceKind) {
        if (query.isBlank() || referenceKind != null) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        results = QuranDataSource.searchAyahs(query).take(50)
    }

    OptionSheet(
        title = strings.more.selectSurah,
        subtitle = strings.more.corpusSummary,
        onDismiss = onDismiss
    ) {
        // The three faces. A pill row rather than a Material TabRow: the page
        // has no bar to sit in, and a full-width band with an underline claims
        // horizontal space this sheet does not have to spare.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = space.sm),
            horizontalArrangement = Arrangement.spacedBy(space.sm)
        ) {
            IndexTab(strings.more.reader.indexSurahs, view == IndexView.SURAHS) {
                view = IndexView.SURAHS
            }
            IndexTab(strings.more.reader.indexSaved, view == IndexView.SAVED) {
                view = IndexView.SAVED
            }
            IndexTab(strings.more.reader.indexSearch, view == IndexView.SEARCH) {
                view = IndexView.SEARCH
            }
        }

        when (view) {
            IndexView.SURAHS -> SurahIndex(
                current = currentSurah,
                filter = surahFilter,
                onFilterChange = { surahFilter = it },
                onSelect = onSelectSurah
            )

            IndexView.SAVED -> SavedIndex(
                bookmarks = bookmarks,
                onSelect = { bookmark ->
                    onSelectSurahAyah(bookmark.surahNumber, bookmark.ayahNumber)
                    onDismiss()
                }
            )

            IndexView.SEARCH -> SearchIndex(
                query = query,
                onQueryChange = { query = it },
                referenceKind = referenceKind,
                onKindChange = { referenceKind = it },
                results = results,
                currentSurah = currentSurah,
                onSelectSurah = onSelectSurah,
                onSelectAyah = { ayah ->
                    onSelectSurahAyah(ayah.surahNumber, ayah.ayahNumber)
                    onDismiss()
                },
                onSelectPage = { page ->
                    // A page number resolves to the verse it opens on, which is
                    // the anchor the reader needs - jumping to a page used to
                    // drop the reader at the top of that page's surah instead.
                    QuranDataSource.resolvePage(page)?.let { (surah, ayah) ->
                        onSelectSurahAyah(surah.number, ayah)
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
private fun RowScope.IndexTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = QuranShape.pill,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(role = Role.Tab, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = Space.current.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Surahs
// ---------------------------------------------------------------------------

@Composable
private fun SurahIndex(
    current: Int,
    filter: SurahFilter,
    onFilterChange: (SurahFilter) -> Unit,
    onSelect: (Int) -> Unit
) {
    val strings = LocalStrings.current

    val surahs = remember(filter) {
        when (filter) {
            SurahFilter.ALL -> QuranDataSource.SURAHS
            SurahFilter.MECCAN -> QuranDataSource.SURAHS.filter { it.revelationType == RevelationType.MECCAN }
            SurahFilter.MEDINAN -> QuranDataSource.SURAHS.filter { it.revelationType == RevelationType.MEDINAN }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Space.current.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.current.xs)
    ) {
        FilterPill(strings.more.allSurahsLabel, filter == SurahFilter.ALL) { onFilterChange(SurahFilter.ALL) }
        FilterPill(strings.meccan, filter == SurahFilter.MECCAN) { onFilterChange(SurahFilter.MECCAN) }
        FilterPill(strings.medinan, filter == SurahFilter.MEDINAN) { onFilterChange(SurahFilter.MEDINAN) }
    }

    surahs.forEach { surah ->
        SurahIndexRow(
            surah = surah,
            selected = surah.number == current,
            onClick = { onSelect(surah.number) }
        )
    }
}

/**
 * One surah.
 *
 * A 40dp accent tile carrying the number, the English name and its meaning, and
 * the Arabic name set large and quiet on the far side - the same string that is
 * the loudest thing in the reader's opening is deliberately the quietest thing
 * here.
 */
@Composable
internal fun SurahIndexRow(
    surah: Surah,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("surah_${surah.number}")
            // "You are here" for the surah open in the reader. Without it, 114
            // identical rows gave no way to tell which one you had opened.
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
// Saved
// ---------------------------------------------------------------------------

@Composable
private fun SavedIndex(
    bookmarks: List<BookmarkEntity>,
    onSelect: (BookmarkEntity) -> Unit
) {
    val strings = LocalStrings.current

    if (bookmarks.isEmpty()) {
        EmptyState(
            title = strings.more.reader.emptySavedTitle,
            message = strings.more.reader.emptySavedMessage,
            icon = Icons.Default.BookmarkBorder
        )
        return
    }

    QuranFonts.Provide(QuranFontFace.AMIRI) {
        bookmarks.forEach { bookmark ->
            val ayah = QuranDataSource.resolveAyah(bookmark.surahNumber, bookmark.ayahNumber)
            val surah = QuranDataSource.getSurahByNumber(bookmark.surahNumber)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(QuranShape.tile)
                    .clickable(role = Role.Button, onClick = { onSelect(bookmark) })
                    .padding(horizontal = Space.current.lg, vertical = Space.current.md),
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
                        Spacer(Modifier.height(Space.current.xs))
                        Text(
                            text = ayah.textArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = ArabicFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search, with the page / juz' / hizb quick filters
// ---------------------------------------------------------------------------

@Composable
private fun SearchIndex(
    query: String,
    onQueryChange: (String) -> Unit,
    referenceKind: ReferenceKind?,
    onKindChange: (ReferenceKind?) -> Unit,
    results: List<Ayah>,
    currentSurah: Int,
    onSelectSurah: (Int) -> Unit,
    onSelectAyah: (Ayah) -> Unit,
    onSelectPage: (Int) -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    SearchInput(
        value = query,
        onValueChange = onQueryChange,
        placeholder = strings.more.searchSurahsAndVerses,
        onClear = if (query.isNotEmpty()) ({ onQueryChange("") }) else null
    )

    Spacer(Modifier.height(space.sm))

    Row(horizontalArrangement = Arrangement.spacedBy(space.xs)) {
        FilterPill(strings.more.versesLabel, referenceKind == null) { onKindChange(null) }
        FilterPill(strings.more.pageWord, referenceKind == ReferenceKind.PAGE) {
            onKindChange(ReferenceKind.PAGE)
        }
        FilterPill(strings.more.juzWord, referenceKind == ReferenceKind.JUZ) {
            onKindChange(ReferenceKind.JUZ)
        }
        FilterPill(strings.more.hizbWord, referenceKind == ReferenceKind.HIZB) {
            onKindChange(ReferenceKind.HIZB)
        }
    }

    Spacer(Modifier.height(space.sm))

    // Browsing a numbering: a plain numbered list, because the number is the
    // whole content and any other column would be decoration.
    if (referenceKind != null) {
        val count = when (referenceKind) {
            ReferenceKind.PAGE -> 604
            ReferenceKind.JUZ -> 30
            ReferenceKind.HIZB -> 60
        }
        val trimmed = query.trim()
        val numbers = remember(referenceKind, trimmed) {
            (1..count).filter { trimmed.isEmpty() || it.toString().contains(trimmed) }
        }
        numbers.forEach { number ->
            ReferenceIndexRow(
                kind = referenceKind,
                number = number,
                onClick = {
                    when (referenceKind) {
                        ReferenceKind.PAGE -> onSelectPage(number)
                        ReferenceKind.JUZ -> QuranDataSource.firstAyahForJuz(number)
                            ?.let(onSelectAyah) ?: return@ReferenceIndexRow

                        ReferenceKind.HIZB -> QuranDataSource.firstAyahForHizb(number)
                            ?.let(onSelectAyah) ?: return@ReferenceIndexRow

                        else -> Unit
                    }
                    onQueryChange("")
                    onKindChange(null)
                }
            )
        }
        return
    }

    val matchingSurahs = remember(query) {
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

    if (query.isBlank()) {
        EmptyState(
            title = strings.more.searchHintTitle,
            message = strings.more.searchHintMessage,
            painter = painterResource(R.drawable.salah_quran_search_ayah)
        )
        return
    }

    if (matchingSurahs.isEmpty() && results.isEmpty()) {
        EmptyState(
            title = strings.more.noSearchResults,
            message = strings.more.noResultsMessage,
            icon = Icons.Default.Search
        )
        return
    }

    if (matchingSurahs.isNotEmpty()) {
        Text(
            text = strings.more.surahsFound.format(matchingSurahs.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = space.xs)
        )
        matchingSurahs.forEach { surah ->
            SurahIndexRow(
                surah = surah,
                selected = currentSurah == surah.number,
                onClick = { onSelectSurah(surah.number) }
            )
        }
    }

    if (results.isNotEmpty()) {
        Text(
            text = strings.more.versesFound.format(results.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = space.md, bottom = space.xs)
        )
        results.forEach { ayah ->
            VerseResultRow(ayah = ayah, onClick = { onSelectAyah(ayah) })
        }
    }
}

/** A verse that matched, with enough of it to recognise. */
@Composable
private fun VerseResultRow(ayah: Ayah, onClick: () -> Unit) {
    val space = Space.current
    val surah = QuranDataSource.getSurahByNumber(ayah.surahNumber)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
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
                text = ayah.textArabic,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = ArabicFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
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

/** One row of a page / juz' / hizb browse, naming where it lands. */
@Composable
private fun ReferenceIndexRow(
    kind: ReferenceKind,
    number: Int,
    onClick: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    val detail: String? = when (kind) {
        ReferenceKind.PAGE -> QuranDataSource.surahForPage(number)?.let {
            "${it.englishName} · ${strings.more.verseReference.format(it.number, QuranDataSource.firstAyahOnPage(number)?.ayahNumber ?: 1)}"
        }

        ReferenceKind.JUZ -> QuranDataSource.firstAyahForJuz(number)?.let {
            "${QuranDataSource.getSurahByNumber(it.surahNumber)?.englishName.orEmpty()} · " +
                strings.more.verseReference.format(it.surahNumber, it.ayahNumber)
        }

        ReferenceKind.HIZB -> QuranDataSource.firstAyahForHizb(number)?.let {
            strings.more.juzOf.format(it.juzNumber)
        }
    }

    val title = when (kind) {
        ReferenceKind.PAGE -> strings.more.pageWord
        ReferenceKind.JUZ -> strings.more.juzWord
        ReferenceKind.HIZB -> strings.more.hizbWord
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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

/** A small selectable filter pill. */@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = QuranShape.pill,
        modifier = Modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) strings.more.selected else strings.more.notSelected }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Space.current.md, vertical = Space.current.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
