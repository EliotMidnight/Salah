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
import com.example.data.quran.VerseResults
import com.example.data.quran.SurahResults
import com.example.ui.components.EmptyState
import com.example.ui.components.OptionListSheet
import com.example.ui.components.SearchInput
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.isArabicInterface
import com.example.ui.localization.LocalLanguage
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.Spacing
import com.example.ui.theme.layoutMetrics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay

/** What the index is showing. */
private enum class IndexView { SURAHS, SAVED, SEARCH }

/** Which numbering the search field is browsing. Null means "the verses". */
internal enum class ReferenceKind { PAGE, JUZ, HIZB }

/** Filters on the surah list that are not a text query. */
internal enum class SurahFilter { ALL, MECCAN, MEDINAN }

/**
 * The index.
 *
 * One sheet with three faces - surahs, saved, search - because they are three ways
 * of answering the same question, "where do I go next".
 *
 * It opens as a sheet over the reader rather than replacing it, so the text being
 * read is still behind it when it closes.
 *
 *
 * [OptionListSheet] gives the lists a bounded viewport, so `items` composes what
 * scrolls into view. Nothing else about the sheet changed: it is still a sheet over
 * the reader, and the same three faces.
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
    var results by remember { mutableStateOf(VerseResults(emptyList(), 0)) }
    LaunchedEffect(query, referenceKind) {
        if (query.isBlank() || referenceKind != null) {
            results = VerseResults(emptyList(), 0)
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
        searchShowingFirst = strings.more.searchShowingFirst,
        // The row shows both names whichever language the interface is in, so which one
        // leads is a decision about the reader rather than about the data.
        preferArabicName = isArabicInterface(LocalLanguage.current),
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
        if (referenceKind != null) SurahResults(emptyList(), 0)
        else QuranSearch.searchSurahs(query)
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

