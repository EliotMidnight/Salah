package com.example.ui.quran.reader

import com.example.data.model.QuranReadingOptions

/**
 * How a mushaf page is scaled to the space it has to be shown in.
 *
 * ### Why this is arithmetic and not a measurement
 *
 * The page's height at a given text size is a property of the *text*, and it is
 * measured once, off the main thread of the decision, by [MushafPageLayout]. What
 * is left for a page to be shown is a comparison of one number with another - "is
 * the height I measured bigger than the height I have" - and a division to answer
 * it. That is arithmetic, and putting it in a pure function is what makes it
 * testable at all.
 *
 * The previous implementation was not testable because it was a heuristic *and* a
 * decision in one. It counted characters, divided by a guessed characters-per-
 * line, and multiplied the result by a fudge factor, all inside a `remember` block
 * in a composable. Nobody could check whether the number it produced was right,
 * which is how a page could be silently shrunk to 50% - below the slider's own
 * minimum of 70% - with the stored preference still saying 100%.
 *
 * ### The two numbers that matter
 *
 * - [contentHeightPx]: what the page's text measures at the reader's chosen size.
 * - [viewportHeightPx]: what the page has to fit inside.
 *
 * Everything else is about being honest about the three outcomes:
 *
 * | condition | outcome |
 * | --- | --- |
 * | it fits | shown at the reader's size, untouched |
 * | it is too tall, and shrinking would fix it | shrunk to fit, never enlarged |
 * | it is too tall even at the minimum | shrunk to the minimum **and scrollable** |
 *
 * The third row is the one the old code had no answer for. It clamped at 0.5 and
 * called the page shown, so a dense page on a small screen lost its last lines
 * with no way to reach them.
 */
object PageFit {

    /**
     * The scale to draw a page at.
     *
     * [requested] is the reader's own text size, used as the ceiling. It is never
     * exceeded: a reader who has deliberately made the text smaller than the
     * default has answered a question, and growing it back would be the app
     * overriding them on most pages.
     *
     * [minimum] defaults to the bottom of the reader's own slider range, so every
     * scale this can return is one the reader can express and undo. That is the
     * invariant the old 0.5 floor broke.
     */
    fun scale(
        contentHeightPx: Int,
        viewportHeightPx: Int,
        requested: Float,
        minimum: Float = QuranReadingOptions.ArabicScaleRange.start
    ): Float {
        if (viewportHeightPx <= 0) return requested
        if (contentHeightPx <= viewportHeightPx) return requested
        // The exact scale that would make it fit, in both directions, so the
        // measurement's own error does not accumulate into a visible over- or
        // under-shoot.
        val exact = requested * (viewportHeightPx.toFloat() / contentHeightPx.toFloat())
        return exact.coerceIn(minimum, requested)
    }

    /**
     * Whether a page at [scale] still overflows and therefore has to scroll.
     *
     * A separate question from [scale] because the answer is not implied by it:
     * once the scale is clamped at [minimum], the page can still be too tall, and
     * that is the case the reader must be able to *see* and reach.
     */
    fun needsScroll(
        contentHeightPx: Int,
        viewportHeightPx: Int,
        requested: Float,
        scale: Float
    ): Boolean {
        if (viewportHeightPx <= 0) return false
        if (requested <= 0f) return false
        val scaled = contentHeightPx * (scale / requested)
        return scaled > viewportHeightPx
    }

    /**
     * Both answers at once, from the same two measurements.
     *
     * [PageFitResult.wasScaled] exists so the page can tell the reader *why* the
     * text is smaller than they set it - which is the difference between a page
     * that quietly shrank and one that says so.
     */
    fun resolve(
        contentHeightPx: Int,
        viewportHeightPx: Int,
        requested: Float,
        minimum: Float = QuranReadingOptions.ArabicScaleRange.start
    ): PageFitResult {
        val chosen = scale(contentHeightPx, viewportHeightPx, requested, minimum)
        return PageFitResult(
            scale = chosen,
            wasScaled = chosen < requested,
            needsScroll = needsScroll(contentHeightPx, viewportHeightPx, requested, chosen)
        )
    }

}

/** What [PageFit] decided about one page. */
data class PageFitResult(
    /** The scale to draw at. Always within [QuranReadingOptions.ArabicScaleRange]. */
    val scale: Float,
    /** True when the text is smaller than the reader asked for, to fit the page. */
    val wasScaled: Boolean,
    /**
     * True when the page still does not fit at the minimum size.
     *
     * The page must then be scrollable, and must say so. Clipping a page's last
     * lines with no way to reach them is the one failure a mushaf reader cannot
     * have: a printed page is never missing a line.
     */
    val needsScroll: Boolean
) {
    companion object {
        /** Shown at the reader's own size. */
        fun exact(scale: Float) = PageFitResult(scale, wasScaled = false, needsScroll = false)
    }
}
