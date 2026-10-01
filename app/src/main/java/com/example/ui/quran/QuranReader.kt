package com.example.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.quran.QuranBrowse
import com.example.ui.components.statusBarInset
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusBanner
import com.example.ui.localization.LocalStrings
import com.example.ui.quran.reader.ContinuousReader
import com.example.ui.quran.reader.MushafPager
import com.example.ui.quran.reader.ReaderPosition
import com.example.ui.quran.reader.rememberReaderPosition
import com.example.ui.theme.IconSize
import com.example.ui.theme.Motion
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranPaper
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/**
 * Reading.
 *
 * The reader is the app's only full-screen surface, and it is built around one
 * idea: **the mushaf is the interface**. Everything that is not the page is a thin
 * pill that can be dismissed, because a reader that competes with its own text has
 * already lost.
 *
 * ### What is in here, and what is not
 *
 * This composable owns exactly three things: the paper, the chrome, and the choice
 * between the two reading surfaces. It does not own position - [ReaderPosition]
 * does - and it does not own how a page is fitted or how a verse is drawn. Each of
 * those was three or four intertwined copies of itself in the previous version, and
 * each is now one thing with a test.
 *
 * ### What is deliberately absent
 *
 * No app bar with a title, no bottom dock inside the reader, no settings row, no
 * floating action buttons over the text. The control pill at the top, one circle
 * button beside it, and the immersive button in the corner are the entire chrome.
 */
@Composable
fun QuranReader(
    state: SalahUiState,
    onSelectSurah: (Int) -> Unit,
    onSelectSurahAyah: (Int, Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    options: QuranReadingOptions,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    immersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit,
    onOpenIndex: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    // The paper. Everything else on this surface is read against it.
    val paper = QuranPaper.wash(options.paper, isDark)
    val ink = QuranPaper.ink(isDark)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val surah = state.selectedSurah
    val ayahs = state.currentSurahAyahs

    // **The position**, and the one debounced writer of the persisted position.
    //
    // The previous reader had three numbers that all meant "where am I" and three
    // writers, and they overwrote each other. See `ReaderPosition` for the specific
    // damage that produced.
    val position = rememberReaderPosition(
        initial = QuranBrowse.refOrStart(surah.number, state.activeReadingAyahNumber),
        onPosition = { ref ->
            onSelectSurahAyah(ref.surah, ref.ayah)
            QuranBrowse.ayah(ref.surah, ref.ayah)?.let(onAyahViewed)
        }
    )

    QuranFonts.Provide(options.font) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(paper)
                .testTag("quran_reader")
        ) {
            if (ayahs.isEmpty()) {
                // The first frame on launch, because `currentSurahAyahs` starts
                // empty - and also what a reader would stare at forever if the corpus
                // ever failed to produce verses. A bare "Loading" with no message and
                // no way out covered both.
                EmptyState(
                    title = strings.more.loading,
                    message = strings.more.loadingQuranMessage,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val actionsFor: (Ayah) -> VerseActions = { ayah ->
                    VerseActions(
                        ayah = ayah,
                        isBookmarked = state.isBookmarked(ayah),
                        isPlaying = state.isAudioPlaying &&
                            state.currentAudioAyah == ayah.ayahNumber,
                        onToggleBookmark = { onToggleBookmark(ayah) },
                        onTogglePlay = { onTogglePlayAyah(ayah) }
                    )
                }

                when (options.layout) {
                    QuranReadingLayout.PER_PAGE -> {
                        val pagerState = rememberPagerState(
                            initialPage = (position.page - 1)
                                .coerceIn(0, QuranBrowse.TOTAL_PAGES - 1),
                            pageCount = { QuranBrowse.TOTAL_PAGES }
                        )
                        MushafPager(
                            pagerState = pagerState,
                            position = position,
                            orientation = options.scroll,
                            requestedScale = options.arabicScale,
                            ink = ink,
                            accent = accent,
                            controlsVisible = !immersive,
                            onSelectVerse = { ref -> position.toggleSelection(ref) }
                        )
                    }

                    QuranReadingLayout.CONTINUOUS -> ContinuousReader(
                        surah = surah,
                        ayahs = ayahs,
                        listState = rememberLazyListState(),
                        position = position,
                        options = options,
                        ink = ink,
                        accent = accent,
                        muted = muted,
                        actionsFor = actionsFor,
                        onPositionSettled = { ref -> position.turnTo(ref) },
                        controlsVisible = !immersive
                    )
                }
            }

            // The controls row, floating *over* the reading surface. It is an
            // overlay rather than a header above it because the whole design is that
            // the text is the first thing; the page reserves the space it takes, and
            // in immersive mode it does not take any.
            ReaderControls(
                surah = surah,
                page = position.page,
                ink = ink,
                muted = muted,
                isBookmarked = state.isBookmarkedAt(surah.number, position.ref.ayah),
                visible = !immersive,
                onOpenIndex = onOpenIndex,
                onOpenOptions = onOpenOptions,
                onSaveCurrentLocation = {
                    QuranBrowse.ayah(surah.number, position.ref.ayah)
                        ?.let(onToggleBookmark)
                },
                // Resolved here, at the call site: this function's own body is not
                // inside the `Box`'s scope, so an `align` written in here would not
                // resolve.
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ImmersiveToggle(
                isImmersive = immersive,
                ink = ink,
                muted = muted,
                onToggle = { onImmersiveChange(!immersive) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarInset() + space.xs)
                    .padding(end = space.lg)
            )

            if (state.isAudioPlaying) {
                AudioStrip(
                    surahName = QuranBrowse.surah(surah.number)?.englishName.orEmpty(),
                    ayahNumber = state.currentAudioAyah,
                    reciter = state.reciter,
                    onStop = onStopAudio,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = space.sm)
                        .padding(horizontal = space.md)
                )
            }
        }
    }
}

/**
 * The whole chrome: one pill and two circle buttons, in a single row.
 *
 * The pill is leading - top-left in LTR, top-right in RTL, without a single
 * conditional, because [androidx.compose.foundation.layout.Row] mirrors with the
 * layout direction. It carries the reader's location, so the one thing a reader
 * always wants is the thing that is always there, and it doubles as the way into
 * the index.
 *
 * The immersive control is deliberately **not** in this row. It lives in its own
 * corner, visible in both states - see [ImmersiveToggle].
 */
@Composable
private fun ReaderControls(
    surah: com.example.data.model.Surah,
    page: Int,
    ink: Color,
    muted: Color,
    isBookmarked: Boolean,
    visible: Boolean,
    onOpenIndex: () -> Unit,
    onOpenOptions: () -> Unit,
    onSaveCurrentLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.duration(Motion.SHORT))),
        exit = fadeOut(tween(Motion.duration(Motion.SHORT))),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(
                Modifier.height(statusBarInset() + space.xs)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = space.lg)
                    // Room for the immersive button, which is in its own corner and
                    // drawn over this row in the non-immersive state too. Without
                    // this reserve the last circle button sat exactly underneath it
                    // and only the top one was reachable.
                    .padding(
                        end = MaterialTheme.layoutMetrics.minTouchTarget +
                            space.xs + space.lg
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.xs)
            ) {
                LocationPill(
                    surah = surah,
                    page = page,
                    ink = ink,
                    muted = muted,
                    onClick = onOpenIndex,
                    modifier = Modifier.weight(1f)
                )

                CircleControl(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = strings.more.reader.saveThisLocation,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else muted,
                    onClick = onSaveCurrentLocation,
                    testTag = "reader_save_location"
                )
                CircleControl(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = strings.more.readingOptions,
                    tint = muted,
                    onClick = onOpenOptions,
                    testTag = "reader_options"
                )
            }
        }
    }
}

/**
 * The enter/exit control for immersive mode.
 *
 * One control, both directions, and it never leaves. Faint in immersive mode rather
 * than hidden: the point of immersive mode is that the chrome gets out of the way,
 * but "out of the way" and "no way back" are different things, and a reader who
 * cannot find the exit reads the absence as a bug.
 */
@Composable
private fun ImmersiveToggle(
    isImmersive: Boolean,
    ink: Color,
    muted: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val label = if (isImmersive) {
        strings.more.reader.exitImmersive
    } else {
        strings.more.reader.immersiveMode
    }

    Surface(
        // Faint when immersive so the reader stops noticing it, but never so faint
        // that the target is unclear.
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (isImmersive) 0.55f else 0.94f
        ),
        shape = QuranShape.pill,
        modifier = modifier
            .size(MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .testTag("reader_immersive")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isImmersive) {
                    Icons.Default.FullscreenExit
                } else {
                    Icons.Default.Fullscreen
                },
                contentDescription = label,
                tint = if (isImmersive) ink else muted,
                modifier = Modifier.size(IconSize.lg)
            )
        }
    }
}

/**
 * The location, as one thin pill, and the way into the index.
 *
 * Thin because it sits over text: a full-height bar here would be a header again,
 * which is the thing this screen exists not to be. The surah name leads and the
 * page number follows, because the name is what a reader recognises and the number
 * is what they check.
 *
 * ### One number, and it is the page
 *
 * The previous pill showed the page in the mushaf and the *verse count* in the
 * continuous layouts, so the same number changed meaning with the layout. There is
 * one number now, and it is the page - which is the only figure a reader can lose
 * track of in either layout, and which [ReaderPosition] guarantees agrees with the
 * page under their eyes.
 */
@Composable
private fun LocationPill(
    surah: com.example.data.model.Surah,
    page: Int,
    ink: Color,
    muted: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val pageLabel = "${strings.more.pageWord} $page"

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shape = QuranShape.pill,
        modifier = modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(onClickLabel = strings.more.reader.openIndex, role = Role.Button, onClick = onClick)
            .testTag("reader_index")
            .semantics { contentDescription = pageLabel }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = space.lg, vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = surah.englishName,
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(space.sm))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(muted.copy(alpha = 0.35f))
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = pageLabel,
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                maxLines = 1
            )
        }
    }
}

/** One of the round controls. */
@Composable
private fun CircleControl(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shape = QuranShape.pill,
        modifier = Modifier
            .size(MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(IconSize.lg)
            )
        }
    }
}

/**
 * The recitation banner.
 *
 * Tinted onto whatever paper is behind it rather than using the app's own
 * container, because on a violet wash a `Surface` from the app scheme reads as a
 * grey box pasted onto coloured paper.
 */
@Composable
private fun AudioStrip(
    surahName: String,
    ayahNumber: Int,
    reciter: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val space = Space.current

    StatusBanner(
        message = "${strings.more.recitingLabel} $surahName · $reciter",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        action = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = QuranShape.pill,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(QuranShape.pill)
                    .clickable(role = Role.Button, onClick = onStop)
            ) {
                Text(
                    text = strings.more.stopAudio,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        horizontal = space.lg,
                        vertical = space.sm
                    )
                )
            }
        },
        modifier = modifier
    )
}

/** True when this colour is dark enough to need light ink on it. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Is this verse in the reader's bookmark list? */
private fun SalahUiState.isBookmarked(ayah: Ayah): Boolean =
    isBookmarkedAt(ayah.surahNumber, ayah.ayahNumber)

/** Is `ayah` of `surah` bookmarked? */
private fun SalahUiState.isBookmarkedAt(surah: Int, ayah: Int): Boolean =
    bookmarks.any { it.surahNumber == surah && it.ayahNumber == ayah }
