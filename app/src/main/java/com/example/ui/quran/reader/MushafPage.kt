package com.example.ui.quran.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.Space

/**
 * One canonical mushaf page, drawn whole.
 *
 * ### The page is the unit, and it is shown whole
 *
 * A mushaf page is a fixed object: these lines, on this paper, in this order. It
 * is shown as a page - never scrolled as a window onto part of one, and never
 * clipped. "Where am I" has to be answerable by a page number, and it cannot be
 * while half the page is off the top of the screen.
 *
 * The previous version tried to achieve that by *estimating* a page's height from
 * character counts and a fudge factor, then scaling by the ratio. Because the
 * estimate was wrong, pages were shrunk that fitted and clipped pages that did
 * not. Worse, the scale it clamped at was 0.5 - below the reader's own slider
 * minimum of 0.7 - so the text on screen could be smaller than the preference
 * that supposedly set it, with nothing able to put it back.
 *
 * So the page is **measured**, by the same [TextMeasurer] the layout will use, at
 * the width the page actually has, and [PageFit] does arithmetic on two real
 * numbers. There is no longer a heuristic in the path.
 *
 * ### The two passes, and why the text is built twice
 *
 * Pass one measures the page's text *at the size the reader asked for*. [PageFit]
 * compares that height to the space available and returns the scale to draw at.
 * Pass two builds the text at that scale and lays it out normally.
 *
 * Scaling the already-laid-out text with a graphics layer instead would be one
 * pass and cheaper, and would be wrong twice over: the glyphs would be resampled
 * rather than re-shaped, so the text would be visibly softer than the same text
 * in the continuous reader; and the tap hit-testing would be working in the
 * unscaled coordinate space, so a reader's tap would select whatever verse
 * happened to be under the *unscaled* point. Rebuilding is a string concatenation
 * over one page's verses, and it is the reason a tap selects the verse under the
 * finger rather than a neighbouring one.
 *
 * ### When the page still does not fit
 *
 * A dense page at a large size on a short viewport cannot be shown at any size
 * the reader's slider can express. That page scrolls, and says so. It does not
 * shrink into illegibility and it does not lose its last lines: a printed page is
 * never missing a line, and a reader who cannot see the end of a page has no way
 * to know the end exists.
 */
@Composable
internal fun MushafPage(
    pageNumber: Int,
    requestedScale: Float,
    selected: QuranRef?,
    ink: Color,
    accent: Color,
    onSelectVerse: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOverflowChange: (Boolean) -> Unit = {},
    /**
     * Space reserved at the top of the surface for the reader's floating chrome.
     *
     * Passed in rather than read from an inset here, because the reader knows
     * whether its chrome is currently on screen and the page does not. A page that
     * reserved the room unconditionally would lose it in immersive mode, where
     * nothing is floating over it.
     */
    topInset: androidx.compose.ui.unit.Dp = 0.dp
) {
    val space = Space.current
    val density = LocalDensity.current
    // The page is laid out right to left because Quranic text is right to left *by
    // script*, not by the reader's interface language - an English or French reader
    // still needs the Arabic laid out RTL, and an Arabic reader reading the
    // translation needs that LTR, which the translation's own style handles.
    val layoutDirection = LayoutDirection.Rtl
    val measurer = rememberPageTextMeasurer()
    val strings = LocalStrings.current
    val typeface = QuranFonts.LocalQuranTypeface.current
    val highlight = MaterialTheme.colorScheme.primaryContainer

    val ayahs = remember(pageNumber) { QuranBrowse.ayahsOnPage(pageNumber) }
    if (ayahs.isEmpty()) return

    // The space the page has to be shown in.
    //
    // [PageInsets.top] is reserved because the reader's control pill and its
    // immersive button float *over* the reading surface rather than sitting above
    // it - they are chrome on a page, not a header above one. Reserving the room
    // here rather than trusting the layout to clear it is what stops the top of a
    // fitted page, including a surah name, from ending up behind the pill.
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val gutter = space.lg
    val contentWidth = remember(viewport, density, gutter) {
        (viewport.width - with(density) { gutter.roundToPx() } * 2).coerceAtLeast(0)
    }
    val availableHeight = remember(viewport.height, density, topInset) {
        (viewport.height - with(density) { topInset.roundToPx() }).coerceAtLeast(0)
    }

    // **Pass one: measure, then decide.**
    //
    // `PageFit.resolve` is handed a measuring function rather than a height,
    // because height is not proportional to size for this text - see [PageFit]. It
    // calls it at the reader's size first, which is the common case and costs one
    // measurement, and only measures again if the page genuinely does not fit.
    val fit = remember(
        ayahs,
        requestedScale,
        contentWidth,
        availableHeight,
        density,
        typeface,
        ink,
        accent,
        highlight,
        selected
    ) {
        if (contentWidth <= 0 || availableHeight <= 0) {
            PageFitResult.exact(requestedScale)
        } else {
            PageFit.resolve(
                measure = { candidate ->
                    val text = MushafPageText.build(
                        ayahs = ayahs,
                        scale = candidate,
                        selected = selected,
                        ink = ink,
                        accent = accent,
                        highlight = highlight,
                        lineHeightFactor = typeface.lineHeightFactor
                    )
                    val style = typeface.arabicStyle(
                        scale = candidate,
                        color = ink,
                        align = TextAlign.Start
                    )
                    measureHeight(
                        measurer = measurer,
                        text = text.text,
                        style = style,
                        widthPx = contentWidth,
                        density = density,
                        layoutDirection = layoutDirection
                    )
                },
                viewportHeightPx = availableHeight,
                requested = requestedScale
            )
        }
    }
    val fitScale = fit.scale

    // The reader is told, rather than left to work it out from a page that will not
    // fit. Through `rememberUpdatedState`, because the effect is keyed on the flag
    // and would otherwise call a lambda captured in an older composition.
    val reportOverflow by rememberUpdatedState(onOverflowChange)
    LaunchedEffect(fit.needsScroll) { reportOverflow(fit.needsScroll) }

    // **Pass two**: the text at the scale the fit chose.
    //
    // Keyed on the **inputs** to the build, not on the built page.
    // [MushafPageText] is not a value type, so keying on a built instance misses on
    // every recomposition - which rebuilt the page, reset the text layout, and with
    // it the tap target, on every frame. A reader tapping a verse on a page that
    // happened to recompose got nothing, because `layout` had just been nulled.
    val drawn = remember(
        ayahs,
        fitScale,
        selected,
        ink,
        accent,
        highlight,
        typeface.lineHeightFactor
    ) {
        MushafPageText.build(
            ayahs = ayahs,
            scale = fitScale,
            selected = selected,
            ink = ink,
            accent = accent,
            highlight = highlight,
            lineHeightFactor = typeface.lineHeightFactor
        )
    }
    val drawnStyle = remember(fitScale, typeface, ink) {
        typeface.arabicStyle(scale = fitScale, color = ink, align = TextAlign.Start)
    }

    // A `Box`, not a `BoxWithConstraints`: the size comes from `onSizeChanged` and
    // the constraints scope would never be read, which invites the next reader to
    // assume one is in play.
    Box(
        modifier = modifier
            .testTag("mushaf_page_$pageNumber")
            .onSizeChanged { viewport = it }
    ) {
        val scrollState = rememberScrollState()
        Box(
            modifier = if (fit.needsScroll) {
                // Vertical only, and only on a page that genuinely does not fit at
                // any size the slider can express.
                //
                // The axis matters: on the *horizontal* mushaf this inner scroll is
                // on the pager's vertical axis, so it competes with a vertical page
                // turn. It exists at all only because the page is taller than the
                // screen, and a page that is taller than the screen is exactly the
                // case where a reader most needs to get off it - so on that axis a
                // drag here is a scroll and a page turn has to come from the
                // accessibility action, not from a swipe that no longer moves.
                Modifier
                    .fillMaxSize()
                    .padding(top = topInset)
                    .verticalScroll(scrollState)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(top = topInset)
            },
            contentAlignment = Alignment.TopCenter
        ) {
            // Captured here rather than hoisted, because a `TextLayoutResult` is only
            // valid for the text it was measured from, and a hoisted one would be
            // resolving taps against a page that is no longer on screen.
            var layout by remember(drawn, drawnStyle) { mutableStateOf<TextLayoutResult?>(null) }

            SelectionContainer {
                BasicText(
                    text = drawn.text,
                    style = drawnStyle,
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gutter)
                        .testTag("mushaf_page_text")
                        .pointerInput(drawn, layout, gutter, density) {
                            detectTapGestures { tap ->
                                val result = layout ?: return@detectTapGestures
                                // **The padding must come off the tap.**
                                //
                                // `padding` is a layout modifier on the *same node* as
                                // the text, so the node's own origin is the outer edge
                                // of the gutter while `TextLayoutResult` coordinates
                                // start at the text's box. A tap is padding-inclusive
                                // and the layout is not, so passing the tap straight
                                // through resolves almost every tap to a verse about
                                // one word to the left of the one under the finger -
                                // and under RTL, to the left in *reading* order, so
                                // it is consistently the wrong verse.
                                val inText = tap.copy(
                                    x = tap.x - with(density) { gutter.roundToPx() }
                                )
                                val offset = runCatching {
                                    result.getOffsetForPosition(inText)
                                }.getOrNull() ?: return@detectTapGestures
                                drawn.verseAt(offset)?.let { verse ->
                                    onSelectVerse(verse.ayahNumber)
                                }
                            }
                        }
                        .semantics {
                            contentDescription = describePage(pageNumber, ayahs)
                            stateDescription = strings.more.pageWord + " " + pageNumber
                        }
                )
            }
        }
    }
}

/**
 * The height [text] needs at [widthPx].
 *
 * Unbounded height on purpose: the question is "how tall would this be", and
 * asking it under a height cap answers with the cap.
 */
private fun measureHeight(
    measurer: TextMeasurer,
    text: androidx.compose.ui.text.AnnotatedString,
    style: TextStyle,
    widthPx: Int,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: LayoutDirection
): Int = measurer.measure(
    text = text,
    style = style,
    overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    softWrap = true,
    maxLines = Int.MAX_VALUE,
    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
    density = density,
    layoutDirection = layoutDirection
).size.height

/**
 * What a screen reader announces for a whole page.
 *
 * The reference and the extent, because "page 42" alone tells a screen-reader
 * user nothing about the text. The surah *range* rather than one name, since a
 * page can cross a boundary and naming only the surah it opens in would be a lie
 * on the 40-odd pages that do.
 */
private fun describePage(pageNumber: Int, ayahs: List<Ayah>): String {
    val first = ayahs.first()
    val last = ayahs.last()
    val surahs = ayahs.map { it.surahNumber }.distinct()
    val where = if (surahs.size == 1) {
        first.surahNumber.toString()
    } else {
        "${first.surahNumber} to ${last.surahNumber}"
    }
    return "Page $pageNumber, juz' ${first.juzNumber}, surah $where, " +
        "${ayahs.size} verses, from ${first.surahNumber}:${first.ayahNumber} " +
        "to ${last.surahNumber}:${last.ayahNumber}"
}

/**
 * The measurer, and the cache size it is given.
 *
 * Named so it cannot shadow `androidx.compose.ui.text.rememberTextMeasurer`. The
 * framework's version defaults to `Density(1f)` and `LayoutDirection.Ltr`, and a
 * same-named private wrapper that shadows it means the next unqualified call in
 * this file silently measures with those defaults - which produces a plausible
 * number and a wrong fit.
 *
 * The cache is bounded on purpose. A reader turns pages continuously, and an
 * unbounded cache holds every page at every size it has ever been drawn at - which
 * for a book of 6,236 verses read at two text sizes is the whole corpus, laid out,
 * held for the life of the process. Sixteen is far more than a pager can have on
 * screen, and it means a page measured while scrolling back and forth is still
 * there when the reader returns to it.
 */
@Composable
private fun rememberPageTextMeasurer(): TextMeasurer =
    androidx.compose.ui.text.rememberTextMeasurer(
        cacheSize = MEASURE_CACHE_SIZE
    )

/** How many laid-out pages a reader can have measured at once. */
private const val MEASURE_CACHE_SIZE = 16
