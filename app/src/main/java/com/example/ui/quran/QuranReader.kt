package com.example.ui.quran

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.RowDivider
import com.example.ui.components.StatusDot
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** Whether verses are shown one per block, or as continuous text. */
private enum class ReadingLayout { PER_VERSE, CONTINUOUS }

/**
 * Reading a surah.
 *
 * Two layouts, because they are genuinely different tasks: per-verse for working
 * through a passage one ayah at a time, continuous for reading straight through.
 * The choice is a labelled segmented control in a sheet rather than an unlabelled
 * icon whose content description named the *target* state while the icon showed
 * the *current* one.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun QuranReader(
    state: SalahUiState,
    onBack: () -> Unit,
    onSelectSurah: (Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    val surah = state.selectedSurah
    val ayahs = state.currentSurahAyahs
    val scale = state.quranFontScale

    var layout by rememberSaveable { mutableStateOf(ReadingLayout.PER_VERSE) }
    var showTextSize by rememberSaveable { mutableStateOf(false) }
    var showSurahPicker by rememberSaveable { mutableStateOf(false) }
    var showTranslations by rememberSaveable { mutableStateOf(false) }
    var selectedAyah by rememberSaveable { mutableStateOf(0) }

    val listState = rememberLazyListState()
    val resumeIndex = remember(state.continueReading.surahNumber, surah.number) {
        if (state.continueReading.surahNumber == surah.number) {
            ayahs.indexOfFirst { it.ayahNumber == state.continueReading.ayahNumber }.coerceAtLeast(0)
        } else {
            0
        }
    }

    // Jump to the saved position when the surah opens, but only once per surah -
    // re-running this on every recomposition threw the reader back to the top.
    LaunchedEffect(surah.number) {
        if (resumeIndex > 0) listState.scrollToItem(resumeIndex + 1)
    }

    // Record progress, debounced. This previously wrote to the database on every
    // single scroll step.
    LaunchedEffect(listState, layout) {
        if (layout != ReadingLayout.PER_VERSE) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(400)
            .collect { index ->
                // +1 for the surah header item at the top of the list.
                ayahs.getOrNull(index - 1)?.let(onAyahViewed)
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ReaderTopBar(
                surahName = surah.englishName,
                surahArabic = surah.arabicName,
                layout = layout,
                onBack = onBack,
                onToggleLayout = {
                    layout = if (layout == ReadingLayout.PER_VERSE) {
                        ReadingLayout.CONTINUOUS
                    } else {
                        ReadingLayout.PER_VERSE
                    }
                },
                textSizeActive = showTextSize,
                onToggleTextSize = { showTextSize = !showTextSize },
                onOpenSurahPicker = { showSurahPicker = true }
            )
        },
        bottomBar = {
            if (state.isAudioPlaying) {
                AudioBar(
                    surahName = surah.englishName,
                    surahNumber = surah.number,
                    ayahNumber = state.currentAudioAyah,
                    reciter = state.reciter,
                    onStop = onStopAudio
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showTextSize) {
                TextSizeControl(
                    scale = scale,
                    onScaleChange = onFontScaleChange,
                    onDismiss = { showTextSize = false }
                )
                RowDivider()
            }

            if (ayahs.isEmpty()) {
                EmptyState(
                    title = strings.more.loading,
                    modifier = Modifier.fillMaxSize()
                )
                return@Column
            }

            if (layout == ReadingLayout.PER_VERSE) {
                PerVerseList(
                    state = state,
                    ayahs = ayahs,
                    surah = surah,
                    scale = scale,
                    listState = listState,
                    onToggleBookmark = onToggleBookmark,
                    onTogglePlayAyah = onTogglePlayAyah,
                    showTranslations = showTranslations,
                    onToggleTranslations = { showTranslations = !showTranslations },
                    onChangeSurah = onSelectSurah
                )
            } else {
                ContinuousLayout(
                    state = state,
                    ayahs = ayahs,
                    surah = surah,
                    scale = scale,
                    selectedAyah = selectedAyah,
                    onSelectAyah = { selectedAyah = it },
                    onToggleBookmark = onToggleBookmark,
                    onTogglePlayAyah = onTogglePlayAyah,
                    showTranslations = showTranslations,
                    onToggleTranslations = { showTranslations = !showTranslations },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showSurahPicker) {
        SurahPicker(
            current = surah.number,
            onSelect = {
                onSelectSurah(it)
                showSurahPicker = false
            },
            onDismiss = { showSurahPicker = false }
        )
    }
}

@Composable
private fun ReaderTopBar(
    surahName: String,
    surahArabic: String,
    layout: ReadingLayout,
    textSizeActive: Boolean,
    onBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onToggleTextSize: () -> Unit,
    onOpenSurahPicker: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = statusBarHeight(), start = space.xs, end = space.sm, bottom = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(MaterialTheme.layoutMetrics.minTouchTarget)
                    .testTag("reader_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.more.actionBack,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = onOpenSurahPicker)
                    .padding(horizontal = space.sm, vertical = space.xs)
                    .semantics { contentDescription = "${strings.more.selectSurah}: $surahName" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = surahName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = surahArabic,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = ArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            IconButton(
                onClick = onToggleLayout,
                modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = if (layout == ReadingLayout.PER_VERSE) {
                        strings.more.continuousLayout
                    } else {
                        strings.more.cardsLayout
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onToggleTextSize,
                modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = strings.more.textSize,
                    tint = if (textSizeActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun statusBarHeight(): androidx.compose.ui.unit.Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val px = androidx.compose.foundation.layout.WindowInsets.statusBars
        .getTop(density)
    return with(density) { px.toDp() }
}

@Composable
private fun TextSizeControl(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = space.lg, vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.more.textSize,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            // Commit once, on release, rather than writing to storage on every frame.
            onValueChangeFinished = onDismiss,
            valueRange = 0.8f..1.6f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.md)
                .semantics {
                    contentDescription = strings.more.textSize
                    stateDescription = "${(scale * 100).toInt()}%"
                }
        )
        Text(
            text = "${(scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Surah heading, then one block per verse. */
@Composable
private fun PerVerseList(
    state: SalahUiState,
    ayahs: List<Ayah>,
    surah: com.example.data.model.Surah,
    scale: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    onChangeSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val bookmarked = remember(state.bookmarks) {
        state.bookmarks.map { it.surahNumber to it.ayahNumber }.toSet()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "header") {
            SurahHeading(surah = surah)
            Spacer(Modifier.height(space.lg))
        }

        itemsIndexed(ayahs) { _, ayah ->
            VerseBlock(
                ayah = ayah,
                scale = scale,
                isBookmarked = (ayah.surahNumber to ayah.ayahNumber) in bookmarked,
                isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                onToggleBookmark = { onToggleBookmark(ayah) },
                onTogglePlay = { onTogglePlayAyah(ayah) }
            )
            Spacer(Modifier.height(space.lg))
        }

        item(key = "translations_toggle") {
            com.example.ui.components.ActionRow(
                title = if (showTranslations) strings.more.hideTranslation else strings.more.showTranslation,
                onClick = onToggleTranslations,
                showChevron = false
            )
            if (showTranslations) {
                ayahs.forEach { ayah ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = space.xs)
                    ) {
                        Text(
                            text = "${ayah.surahNumber}:${ayah.ayahNumber}  ${ayah.textEnglish}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(space.md)
                        )
                    }
                }
            }
            Spacer(Modifier.height(space.lg))
        }

        item(key = "nav") {
            SurahNavigation(
                current = surah.number,
                enabledPrevious = surah.number > 1,
                enabledNext = surah.number < 114,
                onChangeSurah = onChangeSurah
            )
        }
    }
}

/** Surah name, revelation place, verse count, and the opening basmalah. */
@Composable
private fun SurahHeading(surah: com.example.data.model.Surah) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = space.lg)
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = surah.arabicName,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = "${surah.englishName} · ${surah.englishTranslation}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(space.xs))
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
        // Al-Fatihah opens with the basmalah as its first verse, and At-Tawbah has
        // none at all, so it is shown for every surah except those two.
        if (surah.number != 1 && surah.number != 9) {
            Spacer(Modifier.height(space.lg))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = ArabicFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * One verse: reference, actions, Arabic, translation.
 *
 * The four actions are in a single row of icon buttons under the Arabic rather
 * than split between the header and the footer, so they read as one set. The
 * whole card is not clickable, because previously tapping it silently did nothing
 * visible - it only saved a scroll position.
 */
@Composable
private fun VerseBlock(
    ayah: Ayah,
    scale: Float,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    onToggleBookmark: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val reference = "${ayah.surahNumber}:${ayah.ayahNumber}"

    val arabic = remember(ayah.ayahNumber, scale) {
        "${ayah.textArabic} ۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_${ayah.ayahNumber}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = reference,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (isPlaying) {
                StatusDot(color = MaterialTheme.colorScheme.primary, description = strings.more.playVerse)
                Spacer(Modifier.width(space.sm))
            }
            VerseAction(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (isPlaying) strings.more.pauseVerse else strings.more.playVerse,
                onClick = onTogglePlay
            )
            VerseAction(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (isBookmarked) strings.more.removeBookmark else strings.more.bookmarkVerse,
                onClick = onToggleBookmark
            )
            VerseAction(
                icon = Icons.Default.ContentCopy,
                label = strings.more.copyVerse,
                onClick = {
                    clipboard.setText(AnnotatedString(arabic))
                    android.widget.Toast.makeText(context, strings.more.verseCopied, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
            VerseAction(
                icon = Icons.Default.Share,
                label = strings.more.shareVerse,
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, arabic)
                    }
                    context.startActivity(Intent.createChooser(intent, strings.more.shareVerse))
                }
            )
        }

        Spacer(Modifier.height(space.sm))

        Text(
            text = arabic,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (22 * scale).sp,
                lineHeight = (38 * scale).sp,
                textAlign = TextAlign.End
            ),
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(space.sm))

        RowDivider()

        Spacer(Modifier.height(space.sm))

        Text(
            text = ayah.textEnglish,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * scale).sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VerseAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The surah as one flowing block of Arabic with tappable verse markers.
 *
 * The accessibility story here is the reason this is not just a huge `Text`: the
 * verses are separate semantics nodes over the text, so a screen-reader user can
 * step through them, instead of hearing the entire surah read as a single string
 * with no way to act on any of it.
 */
@Composable
private fun ContinuousLayout(
    state: SalahUiState,
    ayahs: List<Ayah>,
    surah: com.example.data.model.Surah,
    scale: Float,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val bookmarked = remember(state.bookmarks) {
        state.bookmarks.map { it.surahNumber to it.ayahNumber }.toSet()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "heading") {
            SurahHeading(surah = surah)
            Spacer(Modifier.height(space.lg))
        }

        items(ayahs, key = { it.ayahNumber }) { ayah ->
            val isSelected = ayah.ayahNumber == selectedAyah
            val isBookmarked = (ayah.surahNumber to ayah.ayahNumber) in bookmarked

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { onSelectAyah(if (isSelected) 0 else ayah.ayahNumber) }
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .padding(vertical = space.sm)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${ayah.surahNumber}:${ayah.ayahNumber}"
                    }
            ) {
                Text(
                    text = "${ayah.textArabic} ۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (22 * scale).sp,
                        lineHeight = (44 * scale).sp
                    ),
                    fontFamily = ArabicFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSelected) {
                    Spacer(Modifier.height(space.sm))
                    RowDivider()
                    Spacer(Modifier.height(space.sm))
                    Text(
                        text = ayah.textEnglish,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(space.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VerseAction(
                            icon = Icons.Default.PlayArrow,
                            label = strings.more.playVerse,
                            onClick = { onTogglePlayAyah(ayah) }
                        )
                        VerseAction(
                            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            label = if (isBookmarked) strings.more.removeBookmark else strings.more.bookmarkVerse,
                            onClick = { onToggleBookmark(ayah) }
                        )
                    }
                }
            }
        }

        item(key = "translations") {
            Spacer(Modifier.height(space.md))
            com.example.ui.components.ActionRow(
                title = if (showTranslations) strings.more.hideTranslation else strings.more.showTranslation,
                onClick = onToggleTranslations,
                showChevron = false
            )
        }
    }
}

@Composable
private fun AudioBar(surahName: String, surahNumber: Int, ayahNumber: Int, reciter: String, onStop: () -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg, vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${strings.more.recitingLabel} $surahName · ${strings.more.verseReference.format(ayahNumber)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reciter,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            com.example.ui.components.ActionRow(
                title = strings.more.stopAudio,
                onClick = onStop,
                showChevron = false,
                modifier = Modifier.width(96.dp)
            )
        }
    }
}

/** Previous / next surah, disabled at the ends of the book. */
@Composable
private fun SurahNavigation(
    current: Int,
    enabledPrevious: Boolean,
    enabledNext: Boolean,
    onChangeSurah: (Int) -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space.sm)
    ) {
        com.example.ui.components.ActionRow(
            title = strings.more.previousSurahLabel,
            onClick = { onChangeSurah(current - 1) },
            showChevron = false,
            enabled = enabledPrevious,
            modifier = Modifier.weight(1f)
        )
        com.example.ui.components.ActionRow(
            title = strings.more.nextSurahLabel,
            onClick = { onChangeSurah(current + 1) },
            showChevron = false,
            enabled = enabledNext,
            modifier = Modifier.weight(1f)
        )
    }
}

/** A searchable, scrollable surah chooser. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahPicker(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filtered = remember(query) {
        if (query.isBlank()) {
            QuranDataSource.SURAHS
        } else {
            QuranDataSource.SURAHS.filter {
                it.englishName.contains(query, ignoreCase = true) ||
                    it.number.toString().startsWith(query.trim())
            }
        }
    }

    com.example.ui.components.OptionSheet(
        title = strings.more.selectSurah,
        subtitle = "${QuranDataSource.SURAHS.size} ${strings.more.surahsTab.lowercase()}",
        onDismiss = onDismiss
    ) {
        com.example.ui.components.SearchInput(
            value = query,
            onValueChange = { query = it },
            onClear = { query = "" },
            placeholder = strings.more.search
        )
        Spacer(Modifier.height(space.sm))

        filtered.forEach { surah ->
            com.example.ui.components.OptionRow(
                title = "${surah.number}. ${surah.englishName}",
                description = surah.englishTranslation,
                selected = surah.number == current,
                onClick = { onSelect(surah.number) }
            )
        }
    }
}
