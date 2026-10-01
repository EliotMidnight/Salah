package com.example.ui.quran.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
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
    onOverflowChange: (Boolean) -> Unit = {}
) {
    val space = Space.current
    val density = LocalDensity.current
    val layoutDirection = LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val strings = LocalStrings.current
    val typeface = QuranFonts.LocalQuranTypeface.current
    val highlight = MaterialTheme.colorScheme.primaryContainer

    val ayahs = remember(pageNumber) { QuranBrowse.ayahsOnPage(pageNumber) }
    if (ayahs.isEmpty()) return

    val page = MushafPageText.build(
        ayahs = ayahs,
        scale = requestedScale,
        selected = selected,
        ink = ink,
        accent = accent,
        highlight = highlight,
        lineHeightFactor = typeface.lineHeightFactor
    )
    val requestedStyle = remember(page, requestedScale, typeface, ink) {
        typeface.arabicStyle(
            scale = requestedScale,
            color = ink,
            align = TextAlign.Start
        )
    }

    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val gutter = space.lg
    val contentWidth = remember(viewport, density, gutter) {
        (viewport.width - with(density) { gutter.roundToPx() } * 2).coerceAtLeast(0)
    }

    // Pass one: measure, then decide. Both are keyed on the things that change the
    // answer - the page, the requested size, the width, the available height - so
    // a recomposition for an unrelated reason does not re-measure a page.
    val fit = remember(page, requestedStyle, contentWidth, viewport.height, density, layoutDirection) {
        if (contentWidth <= 0 || viewport.height <= 0) {
            PageFitResult.exact(requestedScale)
        } else {
            PageFit.resolve(
                contentHeightPx = measureHeight(
                    measurer = measurer,
                    text = page.text,
                    style = requestedStyle,
                    widthPx = contentWidth,
                    density = density,
                    layoutDirection = layoutDirection
                ),
                viewportHeightPx = viewport.height,
                requested = requestedScale
            )
        }
    }

    // The reader is told, rather than left to work it out from a page that will
    // not fit.
    LaunchedEffect(fit.needsScroll) { onOverflowChange(fit.needsScroll) }

    // Pass two: the text at the scale the fit chose.
    val drawn = remember(page, fit.scale, typeface, ink, highlight, selected) {
        if (fit.scale == requestedScale) {
            page
        } else {
            MushafPageText.build(
                ayahs = ayahs,
                scale = fit.scale,
                selected = selected,
                ink = ink,
                accent = accent,
                highlight = highlight,
                lineHeightFactor = typeface.lineHeightFactor
            )
        }
    }
    val drawnStyle = remember(drawn, fit.scale, typeface, ink) {
        typeface.arabicStyle(scale = fit.scale, color = ink, align = TextAlign.Start)
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("mushaf_page_$pageNumber")
            .onSizeChanged { viewport = it }
    ) {
        val scrollState = rememberScrollState()
        Box(
            modifier = if (fit.needsScroll) {
                Modifier.fillMaxSize().verticalScroll(scrollState)
            } else {
                Modifier.fillMaxSize()
            },
            contentAlignment = if (fit.needsScroll) Alignment.TopCenter else Alignment.Center
        ) {
            // Captured here rather than hoisted, because a `TextLayoutResult` is
            // only valid for the text it was measured from and a hoisted one would
            // be resolving taps against a page that is no longer on screen.
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
                        .pointerInput(drawn, selected) {
                            detectTapGestures { tap ->
                                // -1 for "not on a verse", so a tap that misses is
                                // distinguishable from a tap that hits. The old
                                // version returned the current selection when it had
                                // no layout, which made the first tap on a
                                // newly-turned page silently re-select what was
                                // already selected.
                                val result = layout ?: return@detectTapGestures
                                val offset = runCatching {
                                    result.getOffsetForPosition(tap)
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
 * The cache is bounded on purpose. A reader turns pages continuously, and an
 * unbounded measurer cache holds every page at every size it has ever been drawn
 * at - which for a book of 6,236 verses read at two text sizes is the whole
 * corpus, laid out, held for the life of the process. Sixteen entries is far more
 * than the pager can have on screen at once, and it means a page measured while
 * scrolling back and forth is still there when the reader comes back to it.
 */
@Composable
private fun rememberTextMeasurer(): TextMeasurer =
    androidx.compose.ui.text.rememberTextMeasurer(
        cacheSize = 16
    )
