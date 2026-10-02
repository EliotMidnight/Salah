package com.example.ui.quran.reader

import com.example.data.model.QuranReadingOptions

/**
 * How a mushaf page is scaled to the space it has.
 *
 * ### Why this cannot be arithmetic
 *
 * A page's height is `lines x lineHeight`. Raising the type size does two things at
 * once: the line height grows *with* the size, and so does the number of lines,
 * because a bigger font wraps the same text into more lines. Height therefore
 * scales roughly with the **square** of the size, not linearly.
 *
 * ### The rules
 *
 * - The reader's size is the **ceiling**. A page that fits is never enlarged:
 *   a reader who made the text smaller on purpose has answered a question, and
 *   growing it back would override them on most pages to suit a few short ones.
 * - The **floor** is the bottom of the reader's own slider range, so every scale
 *   this can return is one the reader can express and undo. The previous
 *   implementation clamped at 0.5 against a slider whose minimum was 0.7.
 * - If a page still does not fit at the floor, it **scrolls and says so**. It does
 *   not shrink into illegibility and it does not lose its last lines.
 */
object PageFit {

    /**
     * How many bisection steps a fit takes when the page does not fit.
     */
    const val MAX_ITERATIONS = 3

    /**
     * The most measurements a non-fitting page can cost.
     */
    const val MAX_MEASUREMENTS = 2 + MAX_ITERATIONS

    /**
     * The largest scale worth trying.
     */
    fun ceiling(requested: Float): Float = requested

    /**
     * The scale to draw a page at, from measurements taken *at* the candidates.
     *
     * - the requested scale, if the page fits at it;
     * - a scale strictly between the floor and the request, if the page was
     *   measured as fitting there;
     * - the floor, with [PageFitResult.needsScroll] set, if even the floor
     *   overflows.
     */
    fun resolve(
        measure: (Float) -> Int,
        viewportHeightPx: Int,
        requested: Float,
        minimum: Float = QuranReadingOptions.ArabicScaleRange.start
    ): PageFitResult {
        // No space measured yet is not "no space". Before the first layout pass
        // this is evaluated, and reporting a scale or an overflow here would either
        // flash the wrong size or make every page scrollable from frame one.
        if (viewportHeightPx <= 0) return PageFitResult.exact(requested)

        // The common case, and by far the most frequent: the page fits at the size
        // the reader chose. One measurement, and nothing is changed.
        val atRequested = measure(requested)
        if (atRequested <= viewportHeightPx) return PageFitResult.exact(requested)

        // Too tall. Bisect on the scale, measuring at each candidate.
        //
        // Bisection rather than a computed ratio precisely *because* the relation
        // is quadratic and a computed ratio lands in the wrong place. Three
        // measurements, and the answer is within a few percent of the largest scale
        // that fits - a difference no reader can see, and vastly better than
        // either clipping a page or shrinking it further than it needs to be.
        var low = minimum
        var high = requested
        var best = minimum
        var bestHeight = measure(minimum)

        repeat(MAX_ITERATIONS) {
            val mid = (low + high) / 2f
            val height = measure(mid)
            if (height <= viewportHeightPx) {
                best = mid
                bestHeight = height
                low = mid
            } else {
                high = mid
            }
        }

        return PageFitResult(
            scale = best,
            wasScaled = best < requested,
            // The overflow answer comes from a *measurement at the scale we will
            // draw*, not from extrapolating the one we took at the reader's size.
            // That extrapolation is what let a clipped page be reported as fitting.
            needsScroll = bestHeight > viewportHeightPx
        )
    }
}

/** What [PageFit] decided about one page. */
data class PageFitResult(
    /** The scale to draw at. Always within the reader's own slider range. */
    val scale: Float,
    /** True when the text is smaller than the reader asked for, to fit the page. */
    val wasScaled: Boolean,
    /**
     * True when the page still does not fit at the scale chosen.
     */
    val needsScroll: Boolean
) {
    companion object {
        /** Shown at the reader's own size. */
        fun exact(scale: Float) = PageFitResult(scale, wasScaled = false, needsScroll = false)
    }
}
