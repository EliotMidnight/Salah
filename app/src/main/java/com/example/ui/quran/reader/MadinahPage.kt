package com.example.ui.quran.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranRef
import com.example.data.quran.MushafLayout
import com.example.data.quran.QuranBrowse
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.QuranFonts

/**
 * The Madinah mushaf's own page: the print's rows, and the room they print
 * into.
 *
 * ### The room is the print's page, and so is the type
 *
 * A printed mushaf page is a fixed object: fifteen rows (two pages have
 * eight, and that is part of the object too), every word where the print
 * set it, and a full page's height behind all of it - the two short pages
 * do not stop their grid part-way down, they set deeper lines. This surface
 * does not fit the print into a viewport the way the flowing page fits its
 * own text - it makes the room the sheet: the viewport's padded space is
 * the page, the page's own row count divides it into bands, and the type
 * size is the largest that reproduces the print's proportions inside them:
 *
 * - the **row pitch** is the sheet's: one row band per [MushafLayout.Line],
 *   evenly, over the sheet's full height. The print leads its type by the
 *   page, not by the font's own leading, so the drawn leading is set to the
 *   band outright.
 * - the **type size** is the largest whose widest row fills the sheet's ink
 *   width, capped where a row's own leading would overflow its band. It is
 *   never the reader's Arabic-size slider, because the size of the type is a
 *   fact of the print's proportions, the way its margins are; the options
 *   sheet hides the slider on this surface. What a reader does change here
 *   is the typeface itself: the bundled faces are this reader's rendering
 *   choice, not part of the print.
 * - **centred rows** (surah names, the basmalah rows, page 1's openers - the
 *   loader's centred ledger) are set to the middle and never stretched;
 *   elongating a centred heading produces a heading with a stretched letter
 *   in it, which reads as a mistake.
 * - **text rows** are filled to the ink width the way the print fills them,
 *   by elongating letters ([Kashida]), one row at a time. Each row is
 *   finished on its own page, so its "last line" is the line, and
 *   [Kashida.justify] is asked to fill it rather than to leave it as an
 *   unfinished paragraph's tail. Word gaps stay where they are; the reader
 *   of this Book is owed the print's own idea of justification, not a
 *   table's.
 *
 * ### What a tap means
 *
 * The rows carry the verse extents; a tap resolves through the row's own
 * extents to a verse, and the reference it builds is this print's own
 * attribution - the page where the verse's opening word prints, which on
 * 25 pages is not the corpus page table's answer. The position a tap writes
 * is then the page the reader is looking at, and no second attribution
 * table enters the reader.
 */
@Composable
internal fun MadinahPage(
    pageNumber: Int,
    /** A verse on this page was selected, as this print attributes it. */
    selected: QuranRef?,
    ink: Color,
    accent: Color,
    onSelectVerse: (QuranRef) -> Unit,
    /** A page turn, for the accessibility action that has no gesture. */
    onTurn: (Int) -> Unit = {},
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val typeface = QuranFonts.LocalQuranTypeface.current
    // The same highlight the flowing page selects with: one selection, one
    // appearance, whichever surface it was made on.
    val highlight = MaterialTheme.colorScheme.primaryContainer
    val measurer = rememberPageTextMeasurer()

    val lines = remember(pageNumber) { MushafLayout.page(pageNumber).lines }
    if (lines.isEmpty()) return

    // The page's own verses, in print order: what the announcement says the
    // page holds, and what the "select verse" actions act on. They are the
    // print's spans, not the corpus page table's, because what is on this
    // page is this print's question. A basmalah row counts - its span is a
    // verse's own four words - and a marker-only head row counts as its
    // verse, which is every page's honest count.
    val pageVerses = remember(pageNumber) {
        lines.asSequence()
            .flatMap { it.spans.asSequence() }
            .map { it.surah to it.ayah }
            .distinct()
            .mapNotNull { QuranBrowse.ayah(it.first, it.second) }
            .toList()
    }

    // The space the sheet is shown in. [PageInsets.top] is reserved because
    // the reader's control pill and its immersive button float over the page
    // rather than sitting above it, as on the flowing page.
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // **The fit, like the flowing page's, in two passes.** The rows are
    // built and probed at the reader's base size, and the room's own
    // proportions answer with the scale. Nothing here asks the reader's
    // slider: the size a page's height divided by the print's rows wants
    // is the print's proportions, not a preference.
    val fit = remember(viewport, lines, typeface, ink, accent, density, layoutDirection, measurer) {
        if (viewport.width <= 0 || viewport.height <= 0) {
            null
        } else {
            // The room is the sheet, and every page of the print is a full
            // page of its own height: the print does not stop a short
            // page's grid part-way down, it sets the depth. The row count
            // divides the room, so the two eight-line pages read deep and
            // large the way their paper reads.
            val sheetWidth = viewport.width
            val pitch = viewport.height.toFloat() / lines.size
            val inkWidth = (sheetWidth * INK_WIDTH_RATIO).toInt()

            val probe = typeface.arabicStyle(scale = 1f, color = ink)
            var widest = 0
            var tallest = 0
            for (line in lines) {
                val rowText = MadinahText.row(
                    line = line,
                    scale = 1f,
                    accent = accent,
                    selected = null,
                    highlight = Color.Transparent
                )
                val layout = measurer.measure(
                    text = rowText.text,
                    style = probe,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(),
                    density = density,
                    layoutDirection = layoutDirection
                )
                widest = maxOf(widest, layout.size.width)
                tallest = maxOf(tallest, layout.size.height)
            }

            if (widest <= 0 || tallest <= 0) {
                null
            } else {
                // The ink width decides; a row band's own leading can only
                // pull the size further down. A band whose letters overflow
                // their pitch would print colliding rows, and a crowded page
                // is the one thing a print never is.
                MadinahFit(
                    scale = minOf(inkWidth.toFloat() / widest, pitch / tallest),
                    sheetWidth = sheetWidth,
                    inkWidth = inkWidth,
                    pitch = pitch
                )
            }
        }
    }

    Box(
        modifier
            .testTag("madinah_page_$pageNumber")
            .fillMaxSize()
            .padding(top = topInset)
            .onSizeChanged { viewport = it }
            .semantics {
                contentDescription = describePage(pageNumber, pageVerses, strings.more.reader)
                stateDescription = strings.more.pageWord + " " + pageNumber
                customActions = buildList {
                    if (pageNumber > 1) {
                        add(
                            CustomAccessibilityAction(strings.more.reader.previousPage) {
                                onTurn(-1)
                                true
                            }
                        )
                    }
                    if (pageNumber < QuranBrowse.TOTAL_PAGES) {
                        add(
                            CustomAccessibilityAction(strings.more.reader.nextPage) {
                                onTurn(1)
                                true
                            }
                        )
                    }
                    // The verses as numbered actions, labelled and acted on
                    // by the same verse: a reader who has been told what the
                    // page holds has a way to act on any of it.
                    pageVerses.forEach { ayah ->
                        add(
                            CustomAccessibilityAction(
                                "${strings.more.selectVerse} " +
                                    "${ayah.surahNumber}:${ayah.ayahNumber}"
                            ) {
                                onSelectVerse(MushafLayout.ref(ayah.surahNumber, ayah.ayahNumber))
                                true
                            }
                        )
                    }
                }
            }
    ) {
        if (fit == null) return@Box

        val sheetWidthDp = with(density) { fit.sheetWidth.toFloat().toDp() }
        val pitchDp = with(density) { fit.pitch.toDp() }
        val inkMargin = with(density) { ((fit.sheetWidth - fit.inkWidth) / 2f).toDp() }
        val sheetHeightDp = with(density) { (fit.pitch * lines.size).toDp() }

        val drawn = remember(pageNumber, fit.scale, accent, selected, highlight) {
            MadinahText.page(
                pageNumber = pageNumber,
                scale = fit.scale,
                accent = accent,
                selected = selected,
                highlight = highlight
            )
        }

        val justified = remember(drawn, fit, typeface, ink, density, layoutDirection, measurer) {
            drawn.map { row ->
                if (row.line.centered) {
                    row
                } else {
                    val result = Kashida.justify(
                        text = row.text,
                        style = typeface.arabicStyle(
                            scale = fit.scale,
                            color = ink,
                            align = TextAlign.Start
                        // The pitch, not the type's own leading: a band's
                        // height is the sheet's fact, and the justification
                        // must plan against the same leading the drawing
                        // shows.
                        ).copy(lineHeight = with(density) { fit.pitch.toSp() }),
                        widthPx = fit.inkWidth,
                        spans = row.spans,
                        centredRanges = emptyList(),
                        measurer = measurer,
                        density = density,
                        layoutDirection = layoutDirection,
                        fillLastLine = true
                    )
                    if (result.added == 0) row else MadinahRowText(row.line, result.text, result.spans)
                }
            }
        }

        val rowStyle = typeface.arabicStyle(scale = fit.scale, color = ink)
            .copy(
                // The pitch, not the type's own leading. A row band's height
                // is the sheet's fact; the type sits inside it.
                lineHeight = with(density) { fit.pitch.toSp() },
                textAlign = TextAlign.Start
            )
        val centredStyle = rowStyle.copy(textAlign = TextAlign.Center)

        // The sheet itself: the room, filled, the rows running head to foot
        // as one object. It is selectable as a page's text, never clipped,
        // and never scrolled: everything about it is sized, not cropped.
        SelectionContainer {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(sheetWidthDp)
                    .height(sheetHeightDp)
            ) {
                justified.forEach { rowText ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pitchDp),
                        contentAlignment = Alignment.Center
                    ) {
                        MadinahRow(
                            rowText = rowText,
                            style = if (rowText.line.centered) centredStyle else rowStyle,
                            inkMargin = inkMargin,
                            onSelectVerse = onSelectVerse
                        )
                    }
                }
            }
        }
    }
}

/**
 * One row of the print, drawn.
 *
 * The tap resolves through the row's own extents, which is where the verse
 * numbers live; the reference it hands on is the print's own attribution.
 */
@Composable
private fun MadinahRow(
    rowText: MadinahRowText,
    style: TextStyle,
    inkMargin: Dp,
    onSelectVerse: (QuranRef) -> Unit,
) {
    // Captured rather than hoisted: a TextLayoutResult is valid for the text
    // it was measured from, and a hoisted one would resolve taps against a
    // page that is no longer on screen.
    var layout by remember(rowText, style) { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = rowText.text,
        style = style,
        overflow = TextOverflow.Clip,
        softWrap = false,
        maxLines = 1,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inkMargin)
            .pointerInput(rowText, onSelectVerse) {
                detectTapGestures { position ->
                    val offset = layout?.let { result ->
                        runCatching { result.getOffsetForPosition(position) }.getOrNull()
                    } ?: return@detectTapGestures
                    rowText.verseAt(offset)?.let { verse ->
                        onSelectVerse(MushafLayout.ref(verse.surahNumber, verse.ayahNumber))
                    }
                }
            }
    )
}

/**
 * The share of the page's width the row's ink may run across. The print's
 * own margins sit between its edge rules and its text; a rounded share of
 * the page is what that is in this rendering, tunable against the print's
 * pages, and the same one value for every page so the margins cannot drift
 * page to page.
 */
private const val INK_WIDTH_RATIO = 0.92f

/** The sheet's proportions, resolved once per sheet and reused for both passes. */
private data class MadinahFit(
    /** The type scale, in the reader's own base-size unit. */
    val scale: Float,
    val sheetWidth: Int,
    val inkWidth: Int,
    /** One row band's height, between the sheet's top edge and its foot. */
    val pitch: Float,
)
