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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.data.model.QuranRef
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.engine.VerseRepeat
import com.example.ui.components.SafeArea
import com.example.ui.components.isDarkSurface
import com.example.ui.SalahUiState
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusBanner
import com.example.ui.components.BannerTone
import com.example.ui.localization.LocalLanguage
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ReaderStrings
import com.example.ui.localization.isArabicInterface
import com.example.ui.quran.gesture.readerPinch
import com.example.ui.quran.reader.ContinuousReader
import com.example.ui.quran.reader.MushafPager
import com.example.ui.quran.reader.ReaderPosition
import com.example.ui.theme.IconSize
import com.example.ui.theme.Motion
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranPaper
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/**
 * Reading.
 */
@Composable
fun QuranReader(
    state: SalahUiState,
    /**
     * The reader's place, owned by the Quran destination.
     *
     */
    position: ReaderPosition,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleRepeat: () -> Unit,
    options: QuranReadingOptions,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    immersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit,
    onOpenIndex: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val isDark = MaterialTheme.colorScheme.background.isDarkSurface()

    // The paper. Everything else on this surface is read against it.
    val paper = QuranPaper.wash(options.paper, isDark)
    val ink = QuranPaper.ink(isDark)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val surah = QuranBrowse.surah(position.surah) ?: QuranBrowse.surahs.first()
    // The surah's verses, derived from the position.
    val ayahs = remember(position.surah) { QuranBrowse.ayahsInSurah(position.surah) }

    // The surahs the *page* holds. The pill sits beside the page number and names
    // the page; 51 of the 604 hold more than one. See [pageSurahLabel].
    val pageSurahs = remember(position.page) { QuranBrowse.surahsOnPage(position.page) }

    // One set per bookmark list, not one scan per verse.
    //
    // It is asked per verse on the page ("is this one saved?") and again for the
    // control pill, and every ask re-scanned the whole list; the list changes
    // rarely and is rebuilt here only when it does.
    val bookmarked = remember(state.bookmarks) {
        state.bookmarks.mapTo(HashSet()) { it.surahNumber to it.ayahNumber }
    }

    // The position is one ref, and its page is the page the surface on screen
    // shows it on. When the reading surface changes - entering the reader, or
    // moving between the flowing layouts and the Madinah print's own pages -
    // the verse stays and its page attribution is re-answered by the surface
    // it now belongs to. A selection travels the same way, and survives where
    // the verse it names still starts on the page the new surface shows.
    LaunchedEffect(options.layout) {
        position.alignTo(options.layout)
    }

    // A layout change drops the magnification.
    //
    // `resetMagnification` was documented as doing this and nothing called it, so a
    // pinch-zoom set on the mushaf survived a switch to continuous - a magnification
    // chosen to make a dense page legible, then applied to one verse per screen, where
    // the text is already a line wide and the reader is looking at a fraction of a
    // surah through a keyhole.
    //
    // On a rotation too. Not because the magnification breaks - `PageFit` measures
    // the page's own text and the zoom is a `graphicsLayer` applied outside it, so
    // the fit is unaffected and nothing clips. But the pan is clamped to the surplus
    // the scale created in the *old* orientation, and the surplus is width x height,
    // so a pan chosen for a tall surface is out of range for a wide one - clamped to
    // whatever fits, which is a position the reader never chose and cannot explain.
    // Resetting is the only answer that is right in both orientations.
    //
    // Everything else survives - the position, the selection, the text size - because
    // those are properties of the *reading*, and neither a rotation nor a layout
    // change changes what the reader is reading.
    LaunchedEffect(options.layout, LocalConfiguration.current.orientation) {
        position.resetMagnification()
    }

    QuranFonts.Provide(options.font) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(paper)
                .testTag("quran_reader")
        ) {
            if (ayahs.isEmpty()) {
                // What a reader would stare at forever if the corpus ever failed to
                // produce verses for the surah the position names - which is the one
                // case where a bare "Loading" with no way out is the right thing to
                // show, and the only one.
                QuranLoading()
            } else {
                val actionsFor: (Ayah) -> VerseActions = { ayah ->
                    VerseActions(
                        ayah = ayah,
                        isBookmarked = (ayah.surahNumber to ayah.ayahNumber) in bookmarked,
                        // A whole reference, so "is this the playing verse" is
                        // exact. It was an ayah number alone, which is true of three
                        // different ayah-1s on the last page and of any ayah number
                        // on a page the pager happens to have composed.
                        isPlaying = state.isAudioPlaying &&
                            state.currentAudioRef == ayah.ref,
                        onToggleBookmark = { onToggleBookmark(ayah) },
                        onTogglePlay = { onTogglePlayAyah(ayah) }
                    )
                }

                // **The gesture layer**, and the only gesture layer.
                //
                // It is attached *outside* the surface, so it sees the same pointer
                // events the pager and the list do and can decide between them -
                // rather than being a sibling that competes with them.
                //
                // A magnification also moves the whole surface with a graphics
                // layer, applied above the scroll container so it zooms the text
                // without dragging the gesture surfaces along with it.
                // `clip = false` keeps a magnified line from being cut off at the
                // page edge while it is being dragged into view.
                //
                // `appliedPan` is the reader's own pan plus the correction that
                // keeps a pinch centred under the fingers, clamped as one value -
                // see `ReaderPosition` for why clamping the sum rather than the
                // parts is load-bearing.
                fun Modifier.magnifying(): Modifier = graphicsLayer {
                    scaleX = position.viewScale
                    scaleY = position.viewScale
                    translationX = position.appliedPan.x
                    translationY = position.appliedPan.y
                    clip = false
                }

                when (options.layout) {
                    // The pager shows both of the paginated surfaces - the
                    // flowing canon and the Madinah print - and defers to each
                    // page to draw its own ([MushafPager.PageAt]); only the
                    // flowing verse-per-screen reader differs from it. The
                    // pinch applies to both, and on the Madinah page its
                    // text-size end does nothing by design.
                    QuranReadingLayout.PER_PAGE, QuranReadingLayout.MADINAH -> {
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
                            onSelectVerse = { ref -> position.toggleSelection(ref) },
                            actionsFor = actionsFor,
                            options = options,
                            modifier = Modifier
                                .readerPinch(
                                    position = position,
                                    target = options.pinchTarget,
                                    arabicScale = options.arabicScale,
                                    onArabicScaleChange = {
                                        onOptionsChange(options.copy(arabicScale = it))
                                    }
                                )
                                .magnifying()
                                .onSizeChanged { position.surfaceMeasured(it) }
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
                        controlsVisible = !immersive,
                        modifier = Modifier
                            .readerPinch(
                                position = position,
                                target = options.pinchTarget,
                                arabicScale = options.arabicScale,
                                onArabicScaleChange = {
                                    onOptionsChange(options.copy(arabicScale = it))
                                }
                            )
                            .magnifying()
                            .onSizeChanged { position.surfaceMeasured(it) }
                    )
                }
            }

            // The controls row, floating *over* the reading surface. It is an
            // overlay rather than a header above it because the whole design is that
            // the text is the first thing; the page reserves the space it takes, and
            // in immersive mode it does not take any.
            ReaderControls(
                surah = surah,
                pageSurahs = pageSurahs,
                page = position.page,
                ink = ink,
                muted = muted,
                isBookmarked = (surah.number to position.ref.ayah) in bookmarked,
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
                    .padding(top = SafeArea.top() + space.xs)
                    .padding(end = space.lg + SafeArea.sides())
            )

            when {
                state.isAudioPlaying && state.currentAudioReciter != null ->
                    AudioStrip(
                        // From the *playing* reference, not from the reader's surah. The
                        // strip says what is being recited, and the reader may have
                        // navigated elsewhere while it plays - in which case the surah
                        // the text is in and the surah being recited are two different
                        // answers and the strip must be reporting the audio's.
                        ref = state.currentAudioRef,
                        // And from the *streaming* reciter, not the preference: the
                        // preference says who the reader chose; the player says who is
                        // being heard. They were the same value only while the stream
                        // ignored the choice.
                        reciter = state.currentAudioReciter.orEmpty(),
                        // The speed the preference flow carries: the same value the
                        // player was told, so the pill shows what is applied.
                        speed = state.versePlaybackSpeed,
                        onCycleSpeed = onCycleSpeed,
                        repeat = state.verseRepeat,
                        onCycleRepeat = onCycleRepeat,
                        onStop = onStopAudio,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = space.sm)
                            .padding(horizontal = space.md)
                    )

                state.audioFailed ->
                    AudioFailureStrip(
                        onDismiss = onStopAudio,
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
 */
@Composable
private fun ReaderControls(
    surah: Surah,
    pageSurahs: List<Surah>,
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
    // The cutout at the trailing edge, added to the design gutter. This row is the
    // reader's only chrome, so in landscape it was the row that went under the notch
    // on the side it happens to start from.
    val sides = SafeArea.sides()

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
                Modifier.height(SafeArea.top() + space.xs)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = space.lg + sides)
                    // Room for the immersive button, which is in its own corner and
                    // drawn over this row in the non-immersive state too. Without
                    // this reserve the last circle button sat exactly underneath it
                    // and only the top one was reachable.
                    .padding(
                        end = MaterialTheme.layoutMetrics.minTouchTarget +
                            space.xs + space.lg + sides
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.xs)
            ) {
                LocationPill(
                    pageSurahs = pageSurahs,
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
 * The surahs a page holds, as the pill names them: the one name, or the range's two ends.
 *
 * The pill sits beside the page number, where a single name reads as the page's own
 * name - and a page is not required to sit inside one surah. Page 106 ends An-Nisa
 * and carries the start of Al-Ma'idah; a pill that named the surah the reader's verse
 * happened to be in changed the page's name as the reader scrolled within it, and
 * answered a question the pill was not asking.
 *
 * The ends are joined with this language's own range mark, the same one the page
 * announcement ranges surah numbers with, so the reader looking at the page and the
 * reader hearing it are told the same thing. [ReaderStrings.range] owns the single
 * case: a range whose ends are the same is one end. Each end is the name this
 * interface has for its surah ([surahDisplayName]).
 *
 * An empty page - which the corpus does not have - names nothing rather than throwing
 * on a surface a reader is looking at.
 */
internal fun pageSurahLabel(
    reader: ReaderStrings,
    surahs: List<Surah>,
    preferArabicName: Boolean
): String =
    surahs.firstOrNull()
        ?.let {
            reader.range(
                surahDisplayName(it, preferArabicName),
                surahDisplayName(surahs.last(), preferArabicName)
            )
        }
        .orEmpty()

/**
 * The location, as one pill, and the way into the index.
 */
@Composable
private fun LocationPill(
    pageSurahs: List<Surah>,
    page: Int,
    ink: Color,
    muted: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val preferArabicName = isArabicInterface(LocalLanguage.current)
    val pageLabel = "${strings.more.pageWord} $page"
    val surahLabel = pageSurahLabel(strings.more.reader, pageSurahs, preferArabicName)

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
            // A wrapped range label used to raise this pill a full 8dp above the
            // circles beside it - the top row was ragged on exactly the pages
            // whose surah range did not fit one line. 4dp of inner padding keeps
            // two `titleSmall` lines within a rounding of the 48dp target the
            // circles hold: what remains above them is the text's own line-box
            // rounding - about a dp, where the row still reads as one bar.
            modifier = Modifier.padding(horizontal = space.lg, vertical = space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = surahLabel,
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                // Two lines, because a range does not fit on one: "An-Nisa to
                // Al-Ma'idah" is 21 characters, and the pill's share of the row asks
                // one to be dropped - the clipped "Al-Ma…" was the first attempt at
                // this. `PageInsets.top` reserves `minTouchTarget + space.sm` for
                // the row, so a wrapped label has the room, and the larger text
                // scales, where the row does outgrow the reserve, are what the
                // extra space is honest headroom for.
                maxLines = 2,
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
 */
@Composable
private fun AudioStrip(
    /**
     * The verse being recited, as a whole reference.
     */
    ref: QuranRef,
    reciter: String,
    /**
     * The speed the recitation is playing at, as the reader's preference names
     * it. The pill shows what is applied, not a menu: the value is the state.
     */
    speed: Float,
    onCycleSpeed: () -> Unit,
    /**
     * How many more times the verse recites before the chain moves on, as the
     * session's choice resolves it.
     */
    repeat: VerseRepeat,
    onCycleRepeat: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val space = Space.current
    val preferArabicName = isArabicInterface(LocalLanguage.current)

    StatusBanner(
        message = buildString {
            append(strings.more.recitingLabel)
            append(' ')
            append(
                QuranBrowse.surah(ref.surah)
                    ?.let { surahDisplayName(it, preferArabicName) }
                    .orEmpty()
            )
            append(' ')
            append(ref)
            append(" · ")
            append(reciter)
        },
        icon = Icons.AutoMirrored.Filled.MenuBook,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed and stop are both actions this strip owns, and one row
                // of pills is what the banner's slot is for: the three verbs a
                // recitation supports while it sounds.
                BannerPill(
                    label = if (repeat == VerseRepeat.LOOP) {
                        strings.more.repeatPillLoop
                    } else {
                        strings.more.repeatPillFormat.format(repeat.count)
                    },
                    onClick = onCycleRepeat
                )
                BannerPill(
                    label = "%.1f\u00d7".format(speed),
                    onClick = onCycleSpeed,
                    description = strings.more.playbackSpeedLabel
                )
                BannerPill(
                    label = strings.more.stopAudio,
                    onClick = onStop
                )
            }
        },
        modifier = modifier
    )
}

/**
 * The recitation the reader asked for could not be streamed.
 *
 * The player used to answer a failed stream with a synthesized chime and carry
 * on to the next verse, so an offline reader heard a tone march down the surah
 * while the banner named a reciter who was never heard. There is nothing to
 * play, so nothing plays - and this says why, instead of leaving silence and a
 * reader tapping play again.
 *
 * The message alone and a way out of it: the failure is a state the reader can
 * dismiss, not one that resolves on its own.
 */
@Composable
private fun AudioFailureStrip(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    StatusBanner(
        message = strings.more.recitationNeedsConnection,
        tone = BannerTone.Warning,
        action = {
            BannerPill(
                label = strings.more.actionClose,
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * The pill both audio banners offer: one tap, one word.
 *
 * The strip's "Stop" and the failure's "Close" are the same control, so they are
 * one piece of code rather than two that can drift apart.
 */
@Composable
private fun BannerPill(
    label: String,
    onClick: () -> Unit,
    /**
     * The whole meaning of the pill, for a reader listening to it.
     *
     * Needed when the visible label is a bare value - a speed pill reads
     * "1.5×", which tells a listener nothing about what will change. When it
     * is set the description is announced instead of the text, the way a
     * labelled control is announced.
     */
    description: String? = null
) {
    val space = Space.current

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = QuranShape.pill,
        modifier = Modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(QuranShape.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(
                horizontal = space.lg,
                vertical = space.sm
            )
        )
    }
}

/**
 * The reader's loading face.
 *
 * Shown while the corpus is loading - the Quran destination gates on it, because
 * the first access in a process is a parse, not a read - and in the one case
 * where the corpus has loaded but produced no verses for the requested surah,
 * where a bare "Loading" with no way out is still the right thing to show.
 */
@Composable
internal fun QuranLoading(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    EmptyState(
        title = strings.more.loading,
        message = strings.more.loadingQuranMessage,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        modifier = modifier.fillMaxSize()
    )
}
