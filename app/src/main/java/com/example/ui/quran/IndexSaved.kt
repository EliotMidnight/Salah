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

/** The reader's saved verses, as a lazy list. */
internal fun LazyListScope.savedIndex(
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
