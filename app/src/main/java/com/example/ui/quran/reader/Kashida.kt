package com.example.ui.quran.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Justifying an Arabic line by elongating it, rather than by widening the gaps.
 *
 * ### Why this exists at all
 *
 * - **Widen the word gaps.** Works with any font and any text. But an Arabic word is a
 *   connected block, so stretching between them leaves visible holes, and a page of it
 *   looks like a paragraph of stretched Arabic rather than a mushaf.
 * - **Elongate the letters** — insert the tatweel, U+0640. This is what a printed mushaf
 *   does, and it is what makes justified Arabic look *right* rather than merely full.
 *
 * ### Why it needs a font check
 *
 * A tatweel is only a joining stroke if the shape engine connects it to its neighbours, so
 * a face without the glyph renders a gap or a box instead. Every bundled face was checked
 * for U+0640 by `KashidaGlyphTest`; all five have it.
 *
 * ### Where a tatweel may go
 *
 * **Not everywhere.** A tatweel is a joining stroke, so it can only sit where two letters
 * actually join — immediately after a letter that connects *forward* and immediately
 * before one that connects *backward*.
 *
 * `KashidaEligibilityTest` checks this rule against all 6,848 of Tanzil's own placements,
 * so it is verified by the data rather than asserted by me.
 */
/**
 * How a line is filled to its margin.
 */
enum class Justification {
    /**
     * Elongate the letters, widening the gaps only where there is nowhere legal to
     * elongate. This is what a printed mushaf does and what Arabic is set for.
     */
    ELONGATE,

    /**
     * Widen the gaps between words and touch nothing else.
     */
    WORD_GAPS
}

object Kashida {

    /** U+0640 ARABIC TATWEEL. A joining stroke with no sound of its own. */
    const val TATWEEL = 'ـ'

    /**
     * Letters that do **not** connect to the letter that follows.
     */
    private const val NO_FORWARD_JOIN = "اأإآٱدذرزوؤةىء"

    /**
     * Characters that cannot receive a joining stroke on their right.
     */
    private const val NO_BACKWARD_JOIN = "ء"

    /** How many measure-adjust passes to allow. Three reaches the fixed point in practice. */
    const val JUSTIFY_PASSES = 3

    /**
     * Slack below which a line is left alone.
     */
    private const val SLACK_EPSILON = 1.5f

    private fun isMark(ch: Char): Boolean {
        val c = ch.code
        return c in 0x064B..0x065F || c == 0x0670 || c in 0x06D6..0x06ED
    }

    /**
     * The base letter at or before [index], skipping harakat and any tatweel already there.
     */
    private fun baseBefore(text: String, index: Int): Char? {
        var j = index
        while (j >= 0 && (isMark(text[j]) || text[j] == TATWEEL)) j--
        return text.getOrNull(j)
    }

    /** The base letter after [index], skipping harakat and any tatweel already there. */
    private fun baseAfter(text: String, index: Int): Char? {
        var j = index + 1
        while (j < text.length && (isMark(text[j]) || text[j] == TATWEEL)) j++
        return text.getOrNull(j)
    }

    /**
     * Whether a tatweel may be inserted **after** [index] in [text].
     */
    fun canInsertAfter(text: String, index: Int): Boolean {
        if (index < 0 || index >= text.length - 1) return false
        val before = baseBefore(text, index) ?: return false
        val after = baseAfter(text, index) ?: return false
        if (before in NO_FORWARD_JOIN) return false
        if (after in NO_BACKWARD_JOIN) return false
        // A space is not a letter and neither side of a word boundary is a joint.
        if (before == ' ' || after == ' ') return false
        return true
    }

    /** Every legal insertion offset in `[from, to)`, ascending. */
    fun eligibleIndices(text: String, from: Int, to: Int): List<Int> {
        if (to - from < 2) return emptyList()
        val out = ArrayList<Int>(to - from)
        for (i in from until to - 1) {
            if (canInsertAfter(text, i)) out += i
        }
        return out
    }

    /**
     * Where to put [count] tatweels in `[from, until)`, spread as evenly as possible.
     */
    fun spread(text: String, from: Int, to: Int, count: Int): List<Int> {
        val eligible = eligibleIndices(text, from, to)
        if (count <= 0 || eligible.isEmpty()) return emptyList()
        val take = minOf(count, eligible.size)
        val out = ArrayList<Int>(take)
        // Taken out of a shrinking pool, so a point can never be chosen twice. Asking
        // "nearest to the ideal slot" independently for each k picks the same joint
        // repeatedly - every k near the first word wants the first joint - and the list
        // then holds one elongation three times and the line does not get any wider.
        val pool = eligible.toMutableList()
        for (k in 0 until take) {
            val ideal = from + ((to - from).toFloat() * (k + 0.5f) / take).toInt()
            var bestAt = 0
            var bestDistance = Int.MAX_VALUE
            for (i in pool.indices) {
                val distance = kotlin.math.abs(pool[i] - ideal)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestAt = i
                }
            }
            out += pool.removeAt(bestAt)
        }
        return out.sorted()
    }

    // --- The measure-adjust loop ---------------------------------------------

    /** A page of text after justification, with every offset it carried moved to match. */
    data class Justified(
        val text: AnnotatedString,
        val spans: List<VerseSpan>,
        /** How many tatweels were added. Zero means every line was already full. */
        val added: Int
    )

    /**
     * Fill [text] to [widthPx] by elongating it.
     */
    fun justify(
        text: AnnotatedString,
        style: TextStyle,
        widthPx: Int,
        spans: List<VerseSpan>,
        centredRanges: List<IntRange>,
        measurer: TextMeasurer,
        density: Density,
        layoutDirection: LayoutDirection
    ): Justified {
        if (widthPx <= 0 || text.isEmpty()) return Justified(text, spans, 0)

        // Measured before planning, because how many elongations a line needs is
        // `slack / tatweelWidth` and that is a count. Planning in pixels and converting
        // later is where this went wrong first time: a line with 800px of slack came out
        // asking for 800 tatweels, and the page rendered as a stack of horizontal strokes.
        val tatweelWidth = measureTatweel(style, measurer, density, layoutDirection)
        if (tatweelWidth <= 0f) return Justified(text, spans, 0)

        var current = text
        var currentSpans = spans
        var addedTotal = 0

        repeat(JUSTIFY_PASSES) {
            val layout = measurer.measure(
                text = current,
                style = style,
                overflow = TextOverflow.Clip,
                softWrap = true,
                maxLines = Int.MAX_VALUE,
                constraints = Constraints(maxWidth = widthPx),
                density = density,
                layoutDirection = layoutDirection
            )
            val joints = plan(current, layout, widthPx, centredRanges, tatweelWidth)
            if (joints.isEmpty()) return@repeat

            val result = applyInsertions(current, currentSpans, joints)
            current = result.first
            currentSpans = result.second
            addedTotal += joints.size
        }
        return Justified(current, currentSpans, addedTotal)
    }

    /**
     * The joints to elongate, as a set of offsets into [text].
     */
    private fun plan(
        text: AnnotatedString,
        layout: TextLayoutResult,
        widthPx: Int,
        centredRanges: List<IntRange>,
        tatweelWidth: Float
    ): Set<Int> {
        val lastLine = layout.lineCount - 1
        val out = LinkedHashSet<Int>()
        for (line in 0 until lastLine) {
            val start = layout.getLineStart(line)
            val end = layout.getLineEnd(line)
            if (end - start < 2) continue
            // A head is one centred paragraph; if this line sits wholly inside one, leave it.
            if (centredRanges.any { start in it && end - 1 in it }) continue

            // No `getLineWidth` in this Compose version. `lineRight - lineLeft` is the
            // line's own extent, which is its *natural* width as long as nothing else is
            // justifying the paragraph - and nothing is: every bit of justification in
            // this reader is applied here, by hand, as tatweels.
            val natural = layout.getLineRight(line) - layout.getLineLeft(line)
            val slack = widthPx - natural
            if (slack <= SLACK_EPSILON) continue

            val eligible = eligibleIndices(text.text, start, end)
            if (eligible.isEmpty()) continue

            val needed = (slack / tatweelWidth).toInt()
            if (needed < 1) continue
            val use = minOf(needed, eligible.size)
            out += spread(text.text, start, end, use)
        }
        return out
    }

    /** Width of one tatweel, measured rather than guessed. */
    private fun measureTatweel(
        style: TextStyle,
        measurer: TextMeasurer,
        density: Density,
        layoutDirection: LayoutDirection
    ): Float {
        val probe = buildString { repeat(16) { append(TATWEEL) } }
        val width = measurer.measure(
            text = probe,
            style = style,
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(),
            density = density,
            layoutDirection = layoutDirection
        ).size.width
        return width / 16f
    }

    /**
     * Insert tatweels and move every offset that referred to [text].
     */
    private fun applyInsertions(
        text: AnnotatedString,
        spans: List<VerseSpan>,
        joints: Set<Int>
    ): Pair<AnnotatedString, List<VerseSpan>> {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw.length + joints.size)
        // map[original] = where that character ended up.
        val map = IntArray(raw.length)
        for (i in raw.indices) {
            map[i] = builder.length
            builder.append(raw[i])
            if (i in joints) builder.append(TATWEEL)
        }
        // Style positions are relative to the builder's own length, so convert once.
        val styleRanges = text.spanStyles.map { range ->
            val newStart = map.getOrElse(range.start) { range.start }
            val newEnd = map.getOrElse(range.end - 1) { range.end - 1 } + 1
            newStart until newEnd to range.item
        }
        val paragraphRanges = text.paragraphStyles.map { range ->
            val newStart = map.getOrElse(range.start) { range.start }
            val newEnd = map.getOrElse(range.end - 1) { range.end - 1 } + 1
            newStart until newEnd to range.item
        }
        // Replay the styles over the new string. Two passes because a later paragraph
        // style would otherwise be clipped by an earlier span style.
        for ((range, item) in paragraphRanges) {
            builder.addStyle(item, range.first, range.last + 1)
        }
        for ((range, item) in styleRanges) {
            builder.addStyle(item, range.first, range.last + 1)
        }

        val movedSpans = spans.map { span ->
            val start = map.getOrElse(span.text.first) { span.text.first }
            val textEnd = map.getOrElse(span.text.last) { span.text.last } + 1
            val fullEnd = map.getOrElse(span.full.last) { span.full.last } + 1
            VerseSpan(
                ayahNumber = span.ayahNumber,
                surahNumber = span.surahNumber,
                text = start until textEnd,
                full = start until fullEnd
            )
        }
        return builder.toAnnotatedString() to movedSpans
    }
}