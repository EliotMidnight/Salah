package com.example.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranRef
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranSearch
import com.example.data.quran.QuranSearchHit
import com.example.ui.components.EmptyState
import com.example.ui.components.OptionListSheet
import com.example.ui.components.SearchInput
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.Spacing
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
 * One sheet with three faces - surahs, saved, search - because they are three ways
 * of answering the same question, "where do I go next", and they used to be three
 * screens plus a tab row to move between them.
 *
 * It opens as a sheet over the reader rather than replacing it, so the text being
 * read is still behind it when it closes. That matters more here than on a normal
 * screen: arriving somewhere is the end of a journey in a list, and the middle of a
 * breath in a reader.
 *
 * ### Why every list in here is lazy
 *
 * They were not. The sheet's body was a `Column` with a `verticalScroll`, which
 * composes **every** child whether or not it is visible - and this sheet put 114
 * surahs, every saved verse, and all 604 page numbers through that path, on the
 * frame the sheet opened. Opening the index to jump to Al-Kaharah cost a
 * composition of the entire Quran's table of contents.
 *
 * [OptionListSheet] gives the lists a bounded viewport, so `items` composes what
 * scrolls into view. Nothing else about the sheet changed: it is still a sheet over
 * the reader, and the same three faces.
 *
 * ### Why the search field's state is held here and passed in
 *
 * It is `rememberSaveable`, and the composable is only in the tree while the sheet
 * is open, so a query survives a rotation but not a close. That is deliberate: a
 * reader who closes the index and comes back wants a clean sheet, and a reader who
 * rotates wants their query. Splitting the state in two to achieve that is not
 * worth an API change.
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

    // Verse search scans all 6,236 verses, so it is debounced rather than run on
    // every keystroke. The hits carry their own reference and their own highlight
    // range, which is why this is a list of hits and not a list of verses.
    var results by remember { mutableStateOf<List<QuranSearchHit>>(emptyList()) }
    LaunchedEffect(query, referenceKind) {
        if (query.isBlank() || referenceKind != null) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MILLIS)
        results = QuranSearch.searchVerses(query)
    }

    // Strings for the list builders.
    //
    // A `LazyListScope` extension cannot be `@Composable` - it is not called from a
    // composable context, it is the *content* of a `LazyColumn` - so it cannot read
    // `LocalStrings` itself. The labels are gathered here, once, and handed in.
    val labels = IndexLabels(
        allSurahs = strings.more.allSurahsLabel,
        meccan = strings.meccan,
        medinan = strings.medinan,
        verseCount = strings.more.verseCount,
        nowReading = strings.more.nowReading,
        searchSurahsAndVerses = strings.more.searchSurahsAndVerses,
        versesLabel = strings.more.versesLabel,
        pageWord = strings.more.pageWord,
        juzWord = strings.more.juzWord,
        hizbWord = strings.more.hizbWord,
        verseReference = strings.more.verseReference,
        juzOf = strings.more.juzOf,
        surahsFound = strings.more.surahsFound,
        versesFound = strings.more.versesFound,
        selected = strings.more.selected,
        notSelected = strings.more.notSelected,
        searchHintTitle = strings.more.searchHintTitle,
        searchHintMessage = strings.more.searchHintMessage,
        noSearchResults = strings.more.noSearchResults,
        noResultsMessage = strings.more.noResultsMessage,
        emptySavedTitle = strings.more.reader.emptySavedTitle,
        emptySavedMessage = strings.more.reader.emptySavedMessage
    )

    // Search results, and the numbers a numbering browse offers.
    //
    // Both are computed *here* rather than inside the list, because a
    // `LazyListScope` body is not composable and so cannot `remember`. It is also
    // the better place: these are *decisions* about what the reader asked for, not
    // rows to be drawn, and a decision belongs with the state it is derived from.
    val referenceNumbers = remember(referenceKind, query) {
        browseNumbers(referenceKind, query)
    }
    val surahHits = remember(query, referenceKind) {
        if (referenceKind != null) emptyList() else QuranSearch.searchSurahs(query)
    }

    OptionListSheet(
        title = strings.more.selectSurah,
        subtitle = strings.more.corpusSummary,
        onDismiss = onDismiss,
        header = {
            // The three faces. A pill row rather than a Material TabRow: the page has
            // no bar to sit in, and a full-width band with an underline claims
            // horizontal space this sheet does not have to spare.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg)
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
        }
    ) {
        when (view) {
            IndexView.SURAHS -> surahIndex(
                labels = labels,
                current = currentSurah,
                filter = surahFilter,
                onFilterChange = { surahFilter = it },
                onSelect = onSelectSurah
            )

            IndexView.SAVED -> savedIndex(
                labels = labels,
                bookmarks = bookmarks,
                onSelect = { bookmark ->
                    onSelectSurahAyah(bookmark.surahNumber, bookmark.ayahNumber)
                    onDismiss()
                }
            )

            IndexView.SEARCH -> searchIndex(
                labels = labels,
                query = query,
                onQueryChange = { query = it },
                referenceKind = referenceKind,
                onKindChange = { referenceKind = it },
                results = results,
                referenceNumbers = referenceNumbers,
                surahHits = surahHits,
                currentSurah = currentSurah,
                onSelectSurah = onSelectSurah,
                onSelectAyah = { hit ->
                    onSelectSurahAyah(hit.ref.surah, hit.ref.ayah)
                    onDismiss()
                },
                onSelectPlace = { ref ->
                    // A juz' or a hizb is not a surah. Each resolves to the
                    // reference it opens on, and the jump goes to that - so "go to
                    // juz' 30" lands on 78:1, which is what the reader meant, rather
                    // than at the top of whichever surah happened to be selected
                    // last.
                    onSelectSurahAyah(ref.surah, ref.ayah)
                    onDismiss()
                }
            )
        }
    }
}

/** How long a query settles before it is run. */
private const val SEARCH_DEBOUNCE_MILLIS = 220L

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

/** The 114 surahs, filtered, as a lazy list. */
private fun LazyListScope.surahIndex(
    labels: IndexLabels,
    current: Int,
    filter: SurahFilter,
    onFilterChange: (SurahFilter) -> Unit,
    onSelect: (Int) -> Unit
) {
    val space = Spacing()

    val surahs = when (filter) {
        SurahFilter.ALL -> QuranBrowse.surahs
        SurahFilter.MECCAN -> QuranBrowse.surahs.filter { it.revelationType == RevelationType.MECCAN }
        SurahFilter.MEDINAN -> QuranBrowse.surahs.filter { it.revelationType == RevelationType.MEDINAN }
    }

    item(key = "surah_filters") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg)
                .padding(bottom = space.xs),
            horizontalArrangement = Arrangement.spacedBy(space.xs)
        ) {
            FilterPill(labels.allSurahs, filter == SurahFilter.ALL, labels) {
                onFilterChange(SurahFilter.ALL)
            }
            FilterPill(labels.meccan, filter == SurahFilter.MECCAN, labels) {
                onFilterChange(SurahFilter.MECCAN)
            }
            FilterPill(labels.medinan, filter == SurahFilter.MEDINAN, labels) {
                onFilterChange(SurahFilter.MEDINAN)
            }
        }
    }

    items(surahs, key = { it.number }) { surah ->
        SurahIndexRow(
            surah = surah,
            selected = surah.number == current,
            strings = labels,
            onClick = { onSelect(surah.number) }
        )
    }
}

/**
 * One surah.
 *
 * A 40dp accent tile carrying the number, the English name and its meaning, and the
 * Arabic name set large and quiet on the far side - the same string that is the
 * loudest thing in the reader's opening is deliberately the quietest thing here.
 */
@Composable
internal fun SurahIndexRow(
    surah: Surah,
    selected: Boolean,
    strings: IndexLabels,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("surah_${surah.number}")
            // "You are here" for the surah open in the reader. Without it, 114
            // identical rows gave no way to tell which one had been opened.
            .semantics { stateDescription = if (selected) strings.nowReading else "" },
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
            modifier = Modifier.size(space.huge)
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
                text = "${strings.verseCount.format(surah.totalVerses)} · " +
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

/** The reader's saved verses, as a lazy list. */
private fun LazyListScope.savedIndex(
    labels: IndexLabels,
    bookmarks: List<BookmarkEntity>,
    onSelect: (BookmarkEntity) -> Unit
) {
    if (bookmarks.isEmpty()) {
        item(key = "saved_empty") {
            EmptyState(
                title = labels.emptySavedTitle,
                message = labels.emptySavedMessage,
                icon = Icons.Default.BookmarkBorder
            )
        }
        return
    }

    // The preview is rendered in Amiri whatever the reader's mushaf face is: it is
    // not inside the reader, and a preview that changed typeface with a setting
    // elsewhere would be pretending to be part of a reading it is not in.
    item(key = "saved_list") {
        QuranFonts.Provide(QuranFontFace.AMIRI) {
            Column(modifier = Modifier.fillMaxWidth()) {
                bookmarks.forEach { bookmark ->
                    SavedVerseRow(
                        bookmark = bookmark,
                        onSelect = { onSelect(bookmark) }
                    )
                }
            }
        }
    }
}

/** One saved verse, with enough of it to recognise. */
@Composable
private fun SavedVerseRow(bookmark: BookmarkEntity, onSelect: () -> Unit) {
    val space = Space.current
    val ayah = QuranBrowse.ayah(bookmark.surahNumber, bookmark.ayahNumber)
    val surah = QuranBrowse.surah(bookmark.surahNumber)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("saved_${bookmark.surahNumber}_${bookmark.ayahNumber}"),
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
    }
}

// ---------------------------------------------------------------------------
// Search, with the page / juz' / hizb quick filters
// ---------------------------------------------------------------------------

/** The search face: a query, four filters, and a list of results. */
private fun LazyListScope.searchIndex(
    labels: IndexLabels,
    query: String,
    onQueryChange: (String) -> Unit,
    referenceKind: ReferenceKind?,
    onKindChange: (ReferenceKind?) -> Unit,
    results: List<QuranSearchHit>,
    referenceNumbers: List<Int>,
    surahHits: List<Surah>,
    currentSurah: Int,
    onSelectSurah: (Int) -> Unit,
    onSelectAyah: (QuranSearchHit) -> Unit,
    onSelectPlace: (QuranRef) -> Unit
) {
    val space = Spacing()

    item(key = "search_field") {
        Column(modifier = Modifier.padding(horizontal = space.lg)) {
            SearchInput(
                value = query,
                onValueChange = onQueryChange,
                placeholder = labels.searchSurahsAndVerses,
                onClear = if (query.isNotEmpty()) ({ onQueryChange("") }) else null
            )
            Spacer(Modifier.height(space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                FilterPill(labels.versesLabel, referenceKind == null, labels) { onKindChange(null) }
                FilterPill(labels.pageWord, referenceKind == ReferenceKind.PAGE, labels) {
                    onKindChange(ReferenceKind.PAGE)
                }
                FilterPill(labels.juzWord, referenceKind == ReferenceKind.JUZ, labels) {
                    onKindChange(ReferenceKind.JUZ)
                }
                FilterPill(labels.hizbWord, referenceKind == ReferenceKind.HIZB, labels) {
                    onKindChange(ReferenceKind.HIZB)
                }
            }
            Spacer(Modifier.height(space.sm))
        }
    }

    // Browsing a numbering: a plain numbered list, because the number is the whole
    // content and any other column would be decoration.
    if (referenceKind != null) {
        itemsIndexed(
            referenceNumbers,
            key = { index, number -> "${referenceKind}_${number}_$index" }
        ) { _, number ->
            ReferenceIndexRow(
                kind = referenceKind,
                number = number,
                labels = labels,
                onClick = {
                    val ref = when (referenceKind) {
                        ReferenceKind.PAGE -> QuranBrowse.placeAtPage(number).verse
                        ReferenceKind.JUZ -> QuranBrowse.placeAtJuz(number).verse
                        else -> QuranBrowse.placeAtHizb(number).verse
                    }
                    onSelectPlace(ref)
                    onQueryChange("")
                    onKindChange(null)
                }
            )
        }
        return
    }

    if (query.isBlank()) {
        item(key = "search_hint") {
            EmptyState(
                title = labels.searchHintTitle,
                message = labels.searchHintMessage,
                painter = painterResource(R.drawable.salah_quran_search_ayah)
            )
        }
        return
    }

    // Surah names first, and as their own tier.
    //
    // The old implementation put the name test *inside the verse predicate*, so a
    // search for "Maryam" matched the surah's name and then returned all 97 of her
    // verses - none of which contain the word - under a heading that said "97 verses
    // found". A name match is a different thing from a text match and is reported
    // as one.
    if (surahHits.isEmpty() && results.isEmpty()) {
        item(key = "search_none") {
            EmptyState(
                title = labels.noSearchResults,
                message = labels.noResultsMessage,
                icon = Icons.Default.Search
            )
        }
        return
    }

    if (surahHits.isNotEmpty()) {
        item(key = "surah_hits_header") {
            Text(
                text = labels.surahsFound.format(surahHits.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = space.lg,
                    vertical = space.xs
                )
            )
        }
        items(surahHits, key = { "hit_surah_${it.number}" }) { surah ->
            SurahIndexRow(
                surah = surah,
                selected = currentSurah == surah.number,
                strings = labels,
                onClick = { onSelectSurah(surah.number) }
            )
        }
    }

    if (results.isNotEmpty()) {
        item(key = "verse_hits_header") {
            Text(
                text = labels.versesFound.format(results.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = space.lg,
                    vertical = space.xs
                )
            )
        }
        itemsIndexed(results, key = { _, hit -> "hit_${hit.ref.surah}_${hit.ref.ayah}" }) { _, hit ->
            VerseResultRow(hit = hit, onClick = { onSelectAyah(hit) })
        }
    }
}

/**
 * A verse that matched, with the match itself made visible.
 *
 * The highlight is the whole reason a result is trustworthy: a list that shows the
 * whole verse and no hint *why* it matched makes a reader read 6,236 verses to
 * check the search worked.
 */
@Composable
private fun VerseResultRow(hit: QuranSearchHit, onClick: () -> Unit) {
    val space = Space.current
    val ayah = hit.ayah
    val surah = hit.surah

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("result_${hit.ref.surah}_${hit.ref.ayah}")
            .semantics {
                contentDescription = "${surah.englishName}, ${hit.ref.surah}:${hit.ref.ayah}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${surah.englishName} · ${hit.ref.surah}:${hit.ref.ayah}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(space.xs))
            if (hit.matchedIn == QuranSearchHit.Field.ARABIC) {
                Text(
                    text = ayah.textArabic,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = ArabicFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(space.xs))
            }
            Text(
                text = ayah.textEnglish,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
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
    labels: IndexLabels,
    onClick: () -> Unit
) {
    val space = Space.current

    val detail: String? = when (kind) {
        ReferenceKind.PAGE -> QuranBrowse.surahsOnPage(number).joinToString(", ") { it.englishName }
            .takeIf { it.isNotBlank() }

        ReferenceKind.JUZ -> QuranBrowse.ayahsInJuz(number).firstOrNull()?.let { first ->
            "${QuranBrowse.surah(first.surahNumber)?.englishName.orEmpty()} · " +
                labels.verseReference.format(first.surahNumber, first.ayahNumber)
        }

        ReferenceKind.HIZB -> QuranBrowse.ayahsInHizb(number).firstOrNull()?.let { first ->
            labels.juzOf.format(first.juzNumber)
        }
    }

    val title = when (kind) {
        ReferenceKind.PAGE -> labels.pageWord
        ReferenceKind.JUZ -> labels.juzWord
        ReferenceKind.HIZB -> labels.hizbWord
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.md)
            .testTag("reference_${kind}_$number"),
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

/** A small selectable filter pill. */
@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    labels: IndexLabels,
    onClick: () -> Unit
) {
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
            .semantics {
                stateDescription = if (selected) labels.selected else labels.notSelected
            }
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

/**
 * The labels the index's lists need, gathered once.
 *
 * A `LazyListScope` extension cannot read `LocalStrings` - it is not called from a
 * composable context, it *is* the content of a `LazyColumn` - so the strings are
 * read in the sheet's composable and handed down. That is the only reason this
 * type exists, and it is here rather than threaded as eleven parameters because
 * eleven parameters is not a thing anyone can read.
 */
internal data class IndexLabels(
    val allSurahs: String,
    val meccan: String,
    val medinan: String,
    val verseCount: String,
    val nowReading: String,
    val searchSurahsAndVerses: String,
    val versesLabel: String,
    val pageWord: String,
    val juzWord: String,
    val hizbWord: String,
    val verseReference: String,
    val juzOf: String,
    val surahsFound: String,
    val versesFound: String,
    val selected: String,
    val notSelected: String,
    val searchHintTitle: String,
    val searchHintMessage: String,
    val noSearchResults: String,
    val noResultsMessage: String,
    val emptySavedTitle: String,
    val emptySavedMessage: String
)

/**
 * The numbers a page / juz' / hizb browse shows.
 *
 * A plain numbered list, because the number is the whole content and any other
 * column would be decoration - narrowed by whatever the reader has typed, so "285"
 * gets them to page 285 without scrolling past 280 of them.
 *
 * Recomputed in the composable rather than inside the list, because a
 * `LazyListScope` body is not composable and so cannot `remember`. It is also the
 * better place: this is a *decision* about what the reader asked for, not a row.
 */
private fun browseNumbers(kind: ReferenceKind?, query: String): List<Int> {
    if (kind == null) return emptyList()
    val count = when (kind) {
        ReferenceKind.PAGE -> QuranBrowse.TOTAL_PAGES
        ReferenceKind.JUZ -> QuranBrowse.TOTAL_JUZ
        ReferenceKind.HIZB -> QuranBrowse.TOTAL_HIZB
    }
    val trimmed = query.trim()
    return (1..count).filter { trimmed.isEmpty() || it.toString().contains(trimmed) }
}
