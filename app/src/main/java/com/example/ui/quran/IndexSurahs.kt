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

/** The 114 surahs, filtered, as a lazy list. */
internal fun LazyListScope.surahIndex(
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
                // Which name leads is the reader's language, not the data's.
                //
                // The row shows both names either way — the English on one side, the
                // unconditionally. In an Arabic interface that made "Al-Fatihah" the
                // headline of الفاتحة and demoted the name a reader is actually looking
                // for to a quiet aside on the far edge.
                //
                // It is not only aesthetic: `searchSurahs` matches the Arabic name, so a
                // reader who searched الفاتحة was handed a row whose largest text was
                // not the thing they typed.
                text = if (strings.preferArabicName) surah.arabicName else surah.englishName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (strings.preferArabicName) ArabicFamily else null,
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
            // The other name, on the far side, quiet — so whichever leads, both are shown.
            text = if (strings.preferArabicName) surah.englishName else surah.arabicName,
            style = if (strings.preferArabicName) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontFamily = if (strings.preferArabicName) null else ArabicFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
