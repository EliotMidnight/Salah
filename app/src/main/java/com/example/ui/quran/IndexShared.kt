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

/** A small selectable filter pill. */
@Composable
internal fun FilterPill(
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
    val searchShowingFirst: String,
    val preferArabicName: Boolean,
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
 */
internal fun browseNumbers(kind: ReferenceKind?, query: String): List<Int> {
    if (kind == null) return emptyList()
    val count = when (kind) {
        ReferenceKind.PAGE -> QuranBrowse.TOTAL_PAGES
        ReferenceKind.JUZ -> QuranBrowse.TOTAL_JUZ
        ReferenceKind.HIZB -> QuranBrowse.TOTAL_HIZB
    }
    val trimmed = query.trim()
    return (1..count).filter { trimmed.isEmpty() || it.toString().contains(trimmed) }
}

/**
 * [text] with [range] emphasised, or plain [text] when there is no range.
 *
 * **Colour, and nothing else.** A search row is read by scanning, so the match has to
 * be findable at a glance; but changing weight or size would reflow the row on every
 * keystroke as results re-rank, and in a Quranic face a synthetic bold is either absent
 * or a different typeface. Colour moves nothing.
 *
 * A range that does not fit [text] is ignored rather than thrown. [text] and [range]
 * are two fields of one object, so they should always agree; a future caller pairing
 * them wrongly should cost a row its highlight, not the whole results list.
 */
@Composable
internal fun emphasise(text: String, range: IntRange?): AnnotatedString =
    buildAnnotatedString {
        if (range == null || range.first < 0 || range.last >= text.length) {
            append(text)
            return@buildAnnotatedString
        }
        append(text.substring(0, range.first))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(text.substring(range.first, range.last + 1))
        }
        append(text.substring(range.last + 1))
    }
