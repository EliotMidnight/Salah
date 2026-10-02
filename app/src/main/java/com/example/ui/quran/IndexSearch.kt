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

/** The search face: a query, four filters, and a list of results. */
internal fun LazyListScope.searchIndex(
    labels: IndexLabels,
    query: String,
    onQueryChange: (String) -> Unit,
    referenceKind: ReferenceKind?,
    onKindChange: (ReferenceKind?) -> Unit,
    results: VerseResults,
    referenceNumbers: List<Int>,
    surahHits: SurahResults,
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
    if (surahHits.hits.isEmpty() && results.hits.isEmpty()) {
        item(key = "search_none") {
            EmptyState(
                title = labels.noSearchResults,
                message = labels.noResultsMessage,
                icon = Icons.Default.Search
            )
        }
        return
    }

    if (surahHits.hits.isNotEmpty()) {
        item(key = "surah_hits_header") {
            Text(
                text = labels.surahsFound.format(surahHits.total) +
                    if (surahHits.isTruncated) {
                        " \u00b7 " + labels.searchShowingFirst.format(surahHits.hits.size)
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = space.lg,
                    vertical = space.xs
                )
            )
        }
        items(surahHits.hits, key = { "hit_surah_${it.number}" }) { surah ->
            SurahIndexRow(
                surah = surah,
                selected = currentSurah == surah.number,
                strings = labels,
                onClick = { onSelectSurah(surah.number) }
            )
        }
    }

    if (results.hits.isNotEmpty()) {
        item(key = "verse_hits_header") {
            Text(
                text = labels.versesFound.format(results.total) +
                    if (results.isTruncated) {
                        " \u00b7 " + labels.searchShowingFirst.format(results.hits.size)
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = space.lg,
                    vertical = space.xs
                )
            )
        }
        itemsIndexed(results.hits, key = { _, hit -> "hit_${hit.ref.surah}_${hit.ref.ayah}" }) { _, hit ->
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
                    text = emphasise(ayah.textArabic, hit.range),
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
                // Only the field that matched is emphasised, which is why the range
                // travels with `matchedIn` rather than being applied to both rows.
                text = emphasise(
                    ayah.textEnglish,
                    if (hit.matchedIn == QuranSearchHit.Field.ENGLISH) hit.range else null
                ),
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
