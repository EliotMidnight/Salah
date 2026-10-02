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
 * Justifying means filling a line to a fixed width without changing its content. In Latin
 * text the only thing that can move is the space between words, and a loose space is
 * unremarkable. Arabic has **two** levers and they do not look the same:
 *
 * - **Widen the word gaps.** Works with any font and any text. But an Arabic word is a
 *   connected block, so stretching between them leaves visible holes, and a page of it
 *   looks like a paragraph of stretched Arabic rather than a mushaf.
 * - **Elongate the letters** — insert the tatweel, U+0640. This is what a printed mushaf
 *   does, and it is what makes justified Arabic look *right* rather than merely full.
 *
 * This is the second one, with the first as the fallback for lines that have nowhere
 * legal to elongate.
 *
 * ### Why it needs a font check
 *
 * A tatweel is only a joining stroke if the shape engine connects it to its neighbours, so
 * a face without the glyph renders a gap or a box instead. Every bundled face was checked
 * for U+0640 by `KashidaGlyphTest`; all five have it.
 *
 * The deeper reason a mushaf looks right and this does not is **page-glyph fonts**. KFGQ
 * and QCF ship every glyph pre-elongated at fixed widths and the typesetter picks a
 * variant, so a line is filled by the precision of the drawing rather than by inserting
 * characters. The five faces bundled here are general-purpose Arabic fonts — Amiri,
 * Harmattan, Lateef, Scheherazade New — so they cannot do that, and this module is the
 * next best thing: real elongation in real text, with the gaps as a fallback.
 *
 * ### Where a tatweel may go
 *
 * **Not everywhere.** A tatweel is a joining stroke, so it can only sit where two letters
 * actually join — immediately after a letter that connects *forward* and immediately
 * before one that connects *backward*.
 *
 * That rule is not a guess; it is read off Tanzil's own text, which carries 6,848
 * tatweels. Every base letter preceding one in that text is dual-joining: `ل ي ن م ه ت ص
 * س ب ح ع ك خ ظ` and the like, and never `ا د ذ ر ز و ة ى` — the letters that do not
 * connect onward. Letters that connect *backward* only, like `ا ذ و ر`, do appear
 * *after* one, which is right: elongation happens on the joint, not on the far letter.
 *
 * `KashidaEligibilityTest` checks this rule against all 6,848 of Tanzil's own placements,
 * so it is verified by the data rather than asserted by me.
 *
 * ### Why this costs a measure-adjust-measure loop
 *
 * Elongation changes a line's width, which can move where the *next* line breaks, which
 * invalidates the plan. So the only honest way is to measure, plan, apply, and measure
 * again — bounded, because each pass adds width and the fixed point is reached quickly in
 * practice. [JUSTIFY_PASSES] caps it; the result is memoised per page and width, because a
 * reader flipping back and forth between two pages must not pay for this on every frame.
 */
/**
 * How a line is filled to its margin.
 *
 * Both options fill the line; they differ in what they move.
 */
enum class Justification {
    /**
     * Elongate the letters, widening the gaps only where there is nowhere legal to
     * elongate. This is what a printed mushaf does and what Arabic is set for.
     */
    ELONGATE,

    /**
     * Widen the gaps between words and touch nothing else.
     *
     * Correct for Latin, and the only option available in a general-purpose face with no
     * elongation rules. On Arabic it fills the line and leaves it looking stretched, so it
     * is the fallback rather than the default — but it is the honest behaviour for a line
     * whose words offer no joint at all, which is what [Kashida] falls back to per line.
     */
    WORD_GAPS
}

object Kashida {

    /** U+0640 ARABIC TATWEEL. A joining stroke with no sound of its own. */
    const val TATWEEL = 'ـ'

    /**
     * Letters that do **not** connect to the letter that follows.
     *
     * Alef and its variants, the four short letters whose tails point back, dal, thal, ra,
     * zain, waw, hamza-on-waw, teh marbuta, alef maksura, and bare hamza. A tatweel
     * cannot follow any of them, because there is no joint for it to occupy.
     */
    private const val NO_FORWARD_JOIN = "اأإآٱدذرزوؤةىء"

    /**
     * Characters that cannot receive a joining stroke on their right.
     *
     * In practice only the bare hamza, which joins nothing at all. Everything else in the
     * corpus — including `ا ذ و ر` — connects backward and so may sit after a tatweel.
     */
    private const val NO_BACKWARD_JOIN = "ء"

    /** How many measure-adjust passes to allow. Three reaches the fixed point in practice. */
    const val JUSTIFY_PASSES = 3

    /**
     * Slack below which a line is left alone.
     *
     * Not zero. A line can be within a pixel of full and adding a tatweel would *overshoot*
     * it, which looks worse than a one-pixel gap — and `getLineWidth` returns floats, so
     * "full enough" has to be a real threshold rather than `== 0`.
     */
    private const val SLACK_EPSILON = 1.5f

    private fun isMark(ch: Char): Boolean {
        val c = ch.code
        return c in 0x064B..0x065F || c == 0x0670 || c in 0x06D6..0x06ED
    }

    /**
     * The base letter at or before [index], skipping harakat and any tatweel already there.
     *
     * Starts **at** [index], not one before it: "insert after [index]" means the tatweel
     * lands between [index] and [index] + 1, so [index] is the letter on the near side of
     * the joint. Starting one earlier reads the far side, which asks the wrong question —
     * in `باب` it checked the joint before the alef and answered for the one after it,
     * and in `لبت` at index 0 it walked off the front of the word and found no letter at
     * all, refusing a tatweel on the most ordinary joint in the language.
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
     *
     * `index` is a raw character offset, so harakat between the joint and the letter do not
     * hide it — which is the whole reason this walks rather than testing `[index]` directly.
     * In `ٱلرَّحْمَـٰنِ` the tatweel in the corpus already sits after a fatha, and a naive
     * `text[index]` test would see the fatha and refuse.
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
     *
     * Spread rather than left-packed, because a mushaf line is elongated across its whole
     * length; piling every tatweel into the first two words leaves the rest of the line
     * visibly short and the elongation obviously artificial.
     *
     * Returns fewer than [count] when there are not enough joints, which is the signal to
     * fall back to widening the gaps for that line.
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
     *
     * [centredRanges] are character ranges that must be left alone — the surah head is a
     * centred heading, and elongating it would make a centred title with a stretched
     * letter in it, which is worse than not justifying at all.
     *
     * The last line of the layout is left alone for the ordinary typographic reason: a
     * paragraph's last line is short in every book ever set.
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
     *
     * Each joint gets **exactly one** tatweel, and a line gets as many as it needs up to
     * the number of joints it has. That is not a simplification: a line cannot take more
     * elongations than it has joints, so "how many" collapses to "how many joints do I
     * use" — and asking for more than the joints on the line is a request the data cannot
     * fill, which is the case that needs the word gaps instead.
     *
     * Returns an empty set when there is nothing to do, so the caller stops early.
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
     *
     * The remapping is the part that is easy to get wrong and expensive to get wrong: the
     * verse spans decide what a tap selects, and the span styles carry the selection
     * highlight and the accent colour on the ayah markers. Both are character ranges into
     * this string, so both have to move with the insertions or a tap selects the wrong
     * verse — which is a bug this rebuild has already fixed once, in the opposite
     * direction.
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