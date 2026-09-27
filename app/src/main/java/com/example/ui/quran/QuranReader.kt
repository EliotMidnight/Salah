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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.OptionSheet
import com.example.ui.components.OptionRow
import com.example.ui.components.SegmentedOptions
import com.example.ui.components.StatusDot
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** How the text of a surah is presented. */
enum class QuranReadingLayout { PER_VERSE, CONTINUOUS }

/**
 * Reading.
 *
 * Two layouts, because they are different tasks. *Per verse* gives each ayah its
 * own block with its actions attached, which suits working through a passage.
 * *Continuous* renders the whole surah as a single flowing text surface with
 * tappable verse markers, which is what reading actually looks like and is the
 * reason to open a mushaf rather than a list.
 *
 * Continuous is the default. It is the better reading experience and the one the
 * layout was designed around; per verse is one tap away and remembers itself.
 */
@OptIn(FlowPreview::class)
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

    var layout by rememberSaveable { mutableStateOf(QuranReadingLayout.CONTINUOUS) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var showSurahPicker by rememberSaveable { mutableStateOf(false) }
    var showTranslations by rememberSaveable { mutableStateOf(false) }
    var selectedAyah by rememberSaveable { mutableStateOf(0) }

    val listState = rememberLazyListState()

    // Open on the right ayah, and open it only once.
    //
    // `state.activeReadingAyahNumber` is the position the ViewModel resolved. It
    // is what the Reference tab needs: selectPage/Juz/Hizb resolve to a surah
    // *and* an ayah, and the reader used to discard the ayah and open at the top
    // of the surah - so tapping page 300 landed you on page 1's worth of text.
    //
    // Keyed on the surah and the layout rather than remembered against
    // `activeReadingAyahNumber`, because that value also moves while the user
    // scrolls and a remember() keyed on it would fight the reader.
    //
    // This also clears the selection on a surah change. It used to survive, so
    // selecting verse 50 and then changing surah opened the verse inspector on
    // verse 50 of a surah the user had never tapped.
    LaunchedEffect(surah.number, layout) {
        val target = state.activeReadingAyahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        if (layout == QuranReadingLayout.PER_VERSE) {
            selectedAyah = 0
            val index = ayahs.indexOfFirst { it.ayahNumber == target }.coerceAtLeast(0)
            if (index > 0) listState.scrollToItem(index + 1)
        } else {
            selectedAyah = target
        }
    }

    // Record progress, debounced. This wrote to the database on every scroll step.
    LaunchedEffect(listState, layout) {
        if (layout != QuranReadingLayout.PER_VERSE) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(400)
            .collect { index -> ayahs.getOrNull(index - 1)?.let(onAyahViewed) }
    }

    // Continuous is the *default* layout, so this is the effect that actually
    // runs for most users. Without it the saved reading position never moved in
    // the default layout, which meant the library's Continue Reading hero - the
    // element meant to answer "where was I?" - stayed empty or permanently stale
    // unless the user had switched to per-verse first.
    LaunchedEffect(selectedAyah, layout) {
        if (layout != QuranReadingLayout.CONTINUOUS) return@LaunchedEffect
        ayahs.firstOrNull { it.ayahNumber == selectedAyah }?.let(onAyahViewed)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReaderBar(
            surah = surah,
            layout = layout,
            onBack = onBack,
            onToggleLayout = {
                layout = if (layout == QuranReadingLayout.PER_VERSE) {
                    QuranReadingLayout.CONTINUOUS
                } else {
                    QuranReadingLayout.PER_VERSE
                }
            },
            onOpenOptions = { showOptions = true },
            onOpenSurahPicker = { showSurahPicker = true }
        )

        if (ayahs.isEmpty()) {
            // This is the first frame on launch, because currentSurahAyahs starts
            // empty - and it is also what the user would stare at forever if the
            // corpus ever failed to produce verses. A bare "Loading" with no
            // message, no icon and no exit covered both cases and explained
            // neither. Say what is happening, and say what to do if it persists.
            EmptyState(
                title = strings.more.loading,
                message = strings.more.loadingQuranMessage,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        when (layout) {
            QuranReadingLayout.CONTINUOUS -> ContinuousReading(
                state = state,
                surah = surah,
                ayahs = ayahs,
                selectedAyah = selectedAyah,
                onSelectAyah = { selectedAyah = it },
                onToggleBookmark = onToggleBookmark,
                onTogglePlayAyah = onTogglePlayAyah,
                showTranslations = showTranslations,
                onToggleTranslations = { showTranslations = !showTranslations },
                onChangeSurah = onSelectSurah,
                modifier = Modifier
                    .weight(1f)
                    .pinchToResize(state.quranFontScale, onFontScaleChange)
                    .swipeToChangeSurah(surah.number, onChange = onSelectSurah)
            )

            QuranReadingLayout.PER_VERSE -> PerVerseReading(
                state = state,
                surah = surah,
                ayahs = ayahs,
                listState = listState,
                onToggleBookmark = onToggleBookmark,
                onTogglePlayAyah = onTogglePlayAyah,
                showTranslations = showTranslations,
                onToggleTranslations = { showTranslations = !showTranslations },
                onChangeSurah = onSelectSurah,
                modifier = Modifier
                    .weight(1f)
                    .pinchToResize(state.quranFontScale, onFontScaleChange)
                    .swipeToChangeSurah(surah.number, onChange = onSelectSurah)
            )
        }
    }

    if (state.isAudioPlaying) {
        AudioStrip(
            surahName = surah.englishName,
            surahNumber = surah.number,
            ayahNumber = state.currentAudioAyah,
            reciter = state.reciter,
            onStop = onStopAudio
        )
    }

    if (showOptions) {
        OptionSheet(
            title = strings.more.readingOptions,
            onDismiss = { showOptions = false }
        ) {
            Text(
                text = strings.more.selectLayoutTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(space.xs))
            Text(
                text = strings.more.selectLayoutSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(space.md))
            SegmentedOptions(
                options = listOf(strings.more.layoutPerVerse, strings.more.layoutContinuous),
                selectedIndex = if (layout == QuranReadingLayout.PER_VERSE) 0 else 1,
                onSelect = {
                    layout = if (it == 0) {
                        QuranReadingLayout.PER_VERSE
                    } else {
                        QuranReadingLayout.CONTINUOUS
                    }
                }
            )
            Spacer(Modifier.height(space.xl))
            TextSizeRow(
                scale = state.quranFontScale,
                onScaleChange = onFontScaleChange
            )
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

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun ReaderBar(
    surah: Surah,
    layout: QuranReadingLayout,
    onBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenSurahPicker: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset(), start = space.xs, end = space.sm, bottom = space.sm),
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
                    .clip(QuranShape.pill)
                    .clickable(onClick = onOpenSurahPicker)
                    .padding(horizontal = space.sm, vertical = space.xs)
                    .semantics {
                        contentDescription =
                            "${strings.more.selectSurah}: ${surah.englishName}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = surah.englishName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = surah.arabicName,
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
                    imageVector = if (layout == QuranReadingLayout.PER_VERSE) {
                        Icons.Default.ViewAgenda
                    } else {
                        Icons.Default.TextFields
                    },
                    contentDescription = if (layout == QuranReadingLayout.PER_VERSE) {
                        strings.more.layoutContinuous
                    } else {
                        strings.more.layoutPerVerse
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenOptions,
                modifier = Modifier
                    .size(MaterialTheme.layoutMetrics.minTouchTarget)
                    .testTag("reader_options")
            ) {
                Icon(
                    painter = painterResource(R.drawable.salah_quran_mushaf),
                    contentDescription = strings.more.readingOptions,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun topInset(): Dp {
    val density = LocalDensity.current
    val top = maxOf(
        WindowInsets.statusBars.getTop(density),
        WindowInsets.displayCutout.getTop(density)
    )
    return with(density) { top.toDp() }
}

// ---------------------------------------------------------------------------
// Surah heading
// ---------------------------------------------------------------------------

/**
 * The surah's identity block.
 *
 * The Arabic name is introduced by the word "Surah" and set in the accent colour
 * at display size - this is the one place in the app where the accent is used at
 * headline scale, which is what makes the page feel like an opening rather than a
 * list. The basmalah follows in plain text colour at reading size, separated by
 * more space than anything else on the page.
 */
@Composable
private fun SurahHeading(surah: Surah, modifier: Modifier = Modifier) {
    val space = Space.current
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.lg)
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "سُورَةُ ${surah.arabicName}",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = ArabicFamily,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = surah.englishName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = surah.englishTranslation,
            style = MaterialTheme.typography.bodySmall,
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Al-Fatihah opens with the basmalah as its first verse, so showing it
        // separately would print it twice. At-Tawbah has none at all.
        if (surah.number != 1 && surah.number != 9) {
            Spacer(Modifier.height(space.lg))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = ArabicFamily,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Continuous - the mushaf-style page
// ---------------------------------------------------------------------------

/**
 * The surah as one page of text.
 *
 * Every verse is annotated into a single [AnnotatedString], so the whole surah is
 * one laid-out block rather than a column of rows - which is what makes it read
 * as a page. Tapping resolves the tapped offset back to a verse through the text
 * layout, the selected verse is highlighted in place, and the ayah end-markers
 * are styled in the accent colour at a slightly smaller size than the verse text
 * so they recede instead of competing.
 *
 * Accessibility is handled as discrete nodes rather than one enormous string: a
 * screen reader would otherwise read the entire surah as a single utterance with
 * no way to act on any verse in it. Each verse is exposed with a "select verse"
 * custom action, and the same selection the sighted user gets by tapping.
 */
@Composable
private fun ContinuousReading(
    state: SalahUiState,
    surah: Surah,
    ayahs: List<Ayah>,
    selectedAyah: Int,
    onSelectAyah: (Int) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    onChangeSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val scale = state.quranFontScale
    val highlight = MaterialTheme.colorScheme.primaryContainer

    val accent = MaterialTheme.colorScheme.primary
    val page = remember(ayahs, selectedAyah, scale, highlight, accent) {
        buildContinuousPage(ayahs, selectedAyah, scale, highlight, accent)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Keyed on `selectedAyah` as well as `ayahs`. It used to be remembered
    // against `ayahs` alone, so the captured `selectedAyah` was frozen at its
    // first value: after one selection every action compared against a stale
    // number and the tap-to-deselect path could never fire.
    val accessibilityActions = remember(ayahs, selectedAyah) {
        ayahs.map { ayah ->
            CustomAccessibilityAction(
                label = "${strings.more.selectVerse} ${ayah.ayahNumber}"
            ) {
                onSelectAyah(
                    if (selectedAyah == ayah.ayahNumber) 0 else ayah.ayahNumber
                )
                true
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "heading") { SurahHeading(surah) }

        item(key = "page") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = QuranShape.card,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continuous_text_surface")
            ) {
                Text(
                    text = page.text,
                    onTextLayout = { layout = it },
                    style = TextStyle(
                        fontFamily = ArabicFamily,
                        fontSize = (24 * scale).sp,
                        lineHeight = (24 * scale * 1.9f).sp,
                        textDirection = TextDirection.Rtl,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(space.xl)
                        .pointerInput(ayahs, page, selectedAyah) {
                            detectTapGestures { tap ->
                                onSelectAyah(
                                    verseAt(layout, page.verseSpans, ayahs, tap, selectedAyah)
                                )
                            }
                        }
                        .semantics { customActions = accessibilityActions }
                )
            }
        }

        if (selectedAyah > 0) {
            item(key = "inspector") {
                ayahs.firstOrNull { it.ayahNumber == selectedAyah }?.let { ayah ->
                    Spacer(Modifier.height(space.md))
                    VerseInspector(
                        ayah = ayah,
                        isBookmarked = state.bookmarks.any {
                            it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
                        },
                        isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                        fontScale = state.quranFontScale,
                        onDismiss = { onSelectAyah(0) },
                        onToggleBookmark = { onToggleBookmark(ayah) },
                        onTogglePlay = { onTogglePlayAyah(ayah) },
                        modifier = Modifier.testTag("ayah_inspector")
                    )
                }
            }
        }

        item(key = "translations") {
            Spacer(Modifier.height(space.lg))
            TranslationToggle(
                count = ayahs.size,
                shown = showTranslations,
                onToggle = onToggleTranslations
            )
            if (showTranslations) {
                ayahs.forEach { ayah ->
                    TranslationRow(ayah = ayah)
                }
            }
        }

        item(key = "nav") {
            Spacer(Modifier.height(space.xl))
            SurahPager(
                current = surah.number,
                onChange = onChangeSurah
            )
        }
    }
}

/**
 * The rendered page plus the character span of every verse inside it.
 *
 * Hit testing used to rely on `pushStringAnnotation` + `getStringAnnotations`,
 * but that API no longer exists on `TextLayoutResult` in this Compose version -
 * it is absent from both `TextLayoutResult` and `MultiParagraph`. Tracking the
 * offsets while the string is built sidesteps the annotation API entirely and is
 * cheaper: one pass, no per-tap annotation lookup, and no dependency on an API
 * that has been moving between releases.
 */
private class ContinuousPage(
    val text: AnnotatedString,
    val verseSpans: List<IntRange>
)

/**
 * Builds the page.
 *
 * The ayah end marker (U+06DD plus Eastern-Arabic-Indic digits) gets its own span
 * in the accent colour at 90% of the body size. That small asymmetry is what
 * separates this from a wall of text.
 */
private fun buildContinuousPage(
    ayahs: List<Ayah>,
    selectedAyah: Int,
    scale: Float,
    highlight: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color
): ContinuousPage {
    val spans = ArrayList<IntRange>(ayahs.size)
    val text = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val start = length
            val selected = ayah.ayahNumber == selectedAyah

            if (selected) {
                pushStyle(
                    SpanStyle(
                        background = highlight,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            append(ayah.textArabic)
            append(" ")

            if (selected) pop()

            withStyle(
                SpanStyle(
                    color = accent,
                    fontWeight = FontWeight.Medium,
                    fontSize = (24 * scale * 0.9f).sp
                )
            ) {
                append("\u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)}")
            }
            append(" ")

            spans += start until length
        }
    }
    return ContinuousPage(text, spans)
}

/**
 * Maps a tap to the verse under it.
 *
 * A gesture arrives in pixels and the spans are character indices, so the tap
 * has to go through the laid-out text to become a character offset first.
 * Comparing the two directly would resolve almost every tap to the wrong verse.
 */
private fun verseAt(
    layout: TextLayoutResult?,
    spans: List<IntRange>,
    ayahs: List<Ayah>,
    tap: Offset,
    current: Int
): Int {
    if (layout == null) return current
    val total = spans.sumOf { it.count() }
    if (total == 0) return current
    val char = layout.getOffsetForPosition(tap).coerceIn(0, total)
    val index = spans.indexOfFirst { char in it }
    if (index < 0) return current
    return ayahs.getOrNull(index)?.ayahNumber ?: current
}

/** The card that opens under a tapped verse. */
@Composable
private fun VerseInspector(
    ayah: Ayah,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    fontScale: Float,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = QuranShape.card,
        // The one 2dp accent border in the app: selection is expressed as a
        // thicker accent edge rather than a fill change or a shadow.
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.more.actionClose,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(space.sm))

            Text(
                text = "${ayah.textArabic} ۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                // The same base size and line height the mushaf uses, so the
                // inspector tracks the Text Size slider. It was pinned at 19sp
                // while the page it quotes ran 17-38sp, which meant the setting
                // silently did nothing here and 19sp sat off the type scale.
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (24 * fontScale).sp,
                    lineHeight = (32 * fontScale).sp
                ),
                fontFamily = ArabicFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(space.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(space.md))

            Text(
                text = ayah.textEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(space.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Icon(
                        // Honours `isPlaying`. This was a dead parameter: the
                        // caller computed it and the body never read it, so a
                        // verse that was playing offered no pause and TalkBack
                        // announced "Play" on it.
                        imageVector = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) {
                            strings.more.pauseVerse
                        } else {
                            strings.more.playVerse
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                VerseTextAction(
                    icon = if (isBookmarked) {
                        Icons.Default.Bookmark
                    } else {
                        Icons.Default.BookmarkBorder
                    },
                    label = if (isBookmarked) {
                        strings.more.removeBookmark
                    } else {
                        strings.more.bookmarkVerse
                    },
                    tint = if (isBookmarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = onToggleBookmark
                )
                VerseTextAction(
                    icon = Icons.Default.ContentCopy,
                    label = strings.more.copyVerse,
                    onClick = { copyVerse(context, clipboard, ayah, strings.more.verseCopied) }
                )
                // Share belongs here too. It existed only on the per-verse block,
                // so reaching it meant switching reading layout - and the
                // inspector is what continuous mode shows, continuous being the
                // default.
                VerseTextAction(
                    icon = Icons.Default.Share,
                    label = strings.more.shareVerse,
                    onClick = { shareVerse(context, ayah, strings.more.shareVerse) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Per verse
// ---------------------------------------------------------------------------

@Composable
private fun PerVerseReading(
    state: SalahUiState,
    surah: Surah,
    ayahs: List<Ayah>,
    listState: LazyListState,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    onChangeSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val scale = state.quranFontScale

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = space.lg, vertical = space.md)
    ) {
        item(key = "heading") { SurahHeading(surah) }

        itemsIndexed(ayahs, key = { _, a -> a.ayahNumber }) { _, ayah ->
            VerseBlock(
                ayah = ayah,
                scale = scale,
                isBookmarked = state.bookmarks.any {
                    it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
                },
                isPlaying = state.isAudioPlaying && state.currentAudioAyah == ayah.ayahNumber,
                onToggleBookmark = { onToggleBookmark(ayah) },
                onTogglePlay = { onTogglePlayAyah(ayah) },
                modifier = Modifier.testTag("ayah_${ayah.ayahNumber}")
            )
            Spacer(Modifier.height(space.md))
        }

        item(key = "translations") {
            TranslationToggle(
                count = ayahs.size,
                shown = showTranslations,
                onToggle = onToggleTranslations
            )
            if (showTranslations) {
                ayahs.forEach { ayah -> TranslationRow(ayah = ayah) }
            }
        }

        item(key = "nav") {
            Spacer(Modifier.height(space.lg))
            SurahPager(current = surah.number, onChange = onChangeSurah)
        }
    }
}

/** One ayah as its own block: reference and actions above, text below. */
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuranShape.card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = QuranShape.pill,
                modifier = Modifier.heightIn(min = 28.dp)
            ) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = space.md, vertical = space.xxs)
                )
            }

            Spacer(Modifier.weight(1f))

            if (isPlaying) {
                StatusDot(
                    color = MaterialTheme.colorScheme.primary,
                    description = strings.more.playVerse
                )
                Spacer(Modifier.width(space.sm))
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) strings.more.pauseVerse else strings.more.playVerse,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            VerseTextAction(
                icon = if (isBookmarked) {
                    Icons.Default.Bookmark
                } else {
                    Icons.Default.BookmarkBorder
                },
                label = if (isBookmarked) {
                    strings.more.removeBookmark
                } else {
                    strings.more.bookmarkVerse
                },
                tint = if (isBookmarked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onToggleBookmark
            )
            VerseTextAction(
                icon = Icons.Default.ContentCopy,
                label = strings.more.copyVerse,
                onClick = { copyVerse(context, clipboard, ayah, strings.more.verseCopied) }
            )
            VerseTextAction(
                icon = Icons.Default.Share,
                label = strings.more.shareVerse,
                onClick = { shareVerse(context, ayah, strings.more.shareVerse) }
            )
        }

        Spacer(Modifier.height(space.md))

        Text(
            text = "${ayah.textArabic} ۝${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
            style = TextStyle(
                fontFamily = ArabicFamily,
                fontSize = (24 * scale).sp,
                lineHeight = (24 * scale * 1.8f).sp,
                textDirection = TextDirection.Rtl,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(space.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(space.md))

        Text(
            text = ayah.textEnglish,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun TranslationToggle(count: Int, shown: Boolean, onToggle: () -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(onClick = onToggle)
            .padding(horizontal = space.lg, vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = strings.more.translationSectionTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (shown) strings.more.hideTranslation else "${strings.more.showTranslation} ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TranslationRow(ayah: Ayah, modifier: Modifier = Modifier) {
    val space = Space.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = QuranShape.tile,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.xs)
    ) {
        Column(modifier = Modifier.padding(space.md)) {
            Text(
                text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(space.xs))
            Text(
                text = ayah.textEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SurahPager(current: Int, onChange: (Int) -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space.sm)
    ) {
        PagerButton(
            label = strings.more.previousSurahLabel,
            enabled = current > 1,
            onClick = { onChange(current - 1) },
            modifier = Modifier.weight(1f)
        )
        PagerButton(
            label = strings.more.nextSurahLabel,
            enabled = current < 114,
            onClick = { onChange(current + 1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PagerButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = QuranShape.pill,
        modifier = modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = space.md, horizontal = space.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VerseTextAction(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TextSizeRow(scale: Float, onScaleChange: (Float) -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.more.textSize,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(Space.current.md))
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = QuranScaleRange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
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

@Composable
private fun AudioStrip(
    surahName: String,
    surahNumber: Int,
    ayahNumber: Int,
    reciter: String,
    onStop: () -> Unit
) {
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
                    text = "${strings.more.recitingLabel} $surahName · " +
                        strings.more.verseReference.format(surahNumber, ayahNumber),
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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = QuranShape.pill,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(QuranShape.pill)
                    .clickable(onClick = onStop)
            ) {
                Text(
                    text = strings.more.stopAudio,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = space.lg, vertical = space.sm)
                )
            }
        }
    }
}

@Composable
private fun SurahPicker(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val space = Space.current
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }

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

    OptionSheet(
        title = strings.more.selectSurah,
        subtitle = strings.more.corpusSummary,
        onDismiss = onDismiss
    ) {
        com.example.ui.components.SearchInput(
            value = query,
            onValueChange = { query = it },
            onClear = { query = "" },
            placeholder = strings.more.search
        )
        Spacer(Modifier.height(space.sm))
        if (filtered.isEmpty()) {
            // A non-matching query used to render the search field over blank
            // space, with no explanation and no way back except retyping.
            EmptyState(
                title = strings.more.noSearchResults,
                message = strings.more.noSurahMatchMessage,
                icon = Icons.Default.Search,
                action = {
                    TextButton(onClick = { query = "" }) { Text(strings.more.clearSearch) }
                }
            )
        } else {
            // No inner scroll. OptionSheet's content is already a vertical
            // scroll, so wrapping the list in a second one gave two competing
            // drag consumers and pinned the list to a raw 420dp that dominated
            // the sheet in landscape.
            filtered.forEach { surah ->
                OptionRow(
                    title = "${surah.number}. ${surah.englishName}",
                    description = surah.englishTranslation,
                    selected = surah.number == current,
                    onClick = { onSelect(surah.number) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

/** Builds the text a copy or share puts on the clipboard. */
private fun versePayload(ayah: Ayah): String =
    "${ayah.textArabic} \u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)}\n\n" +
        "\"${ayah.textEnglish}\"\n[${ayah.surahNumber}:${ayah.ayahNumber}]"

private fun copyVerse(context: android.content.Context, clipboard: ClipboardManager, ayah: Ayah, label: String) {
    clipboard.setText(AnnotatedString(versePayload(ayah)))
    android.widget.Toast.makeText(context, label, android.widget.Toast.LENGTH_SHORT).show()
}

private fun shareVerse(context: android.content.Context, ayah: Ayah, chooserTitle: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, versePayload(ayah))
    }
    context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
}
