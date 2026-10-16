package com.example.ui.home

/**
 * The type size the Today headline is drawn at.
 *
 * ### Why it is measured rather than fixed
 *
 * The headline is the prayer's name at 58sp on one line, and that line is the whole
 * design point of the page. The name is not the same length in every language: at the
 * ceiling English *Sunrise* measures about half the row, while French *Lever du
 * soleil* and German *Sonnenaufgang* measure more than the whole of it. With a fixed
 * size and `maxLines = 1`, the longer names were cut off mid-word, with no ellipsis,
 * on the one line the page is built around.
 *
 * ### The rules
 *
 * - [CEILING_SP] is a ceiling, never a target. A word that fits is drawn at it, and one
 *   that does not is stepped down until it does. The size comes from a measurement of
 *   the word that is about to be drawn, so it cannot drift from the text.
 * - [FLOOR_SP] is the floor. Below it the headline stops being the loudest thing on the
 *   page; at the floor the word is ellipsised rather than shrunk further.
 */
internal object HeadlineFit {

    /** The size the headline asks for, and the largest it is ever drawn at. */
    const val CEILING_SP = 58f

    /**
     * The line box at the ceiling, held while the type steps down.
     *
     * Deliberately not scaled with the size: the headline sits between a drawn sky and
     * a ruled date row, and a line box that shrank with the word would move both.
     */
    const val LINE_HEIGHT_SP = 66f

    /** The smallest size the headline is drawn at before the word is ellipsised. */
    const val FLOOR_SP = 28f

    /**
     * The size to draw a word of [measuredWidthPx] in a row [availableWidthPx] wide.
     *
     * Both are device pixels, measured with the same density and font scale, so their
     * ratio is the factor the type may be scaled by. A width that cannot be measured -
     * zero, or a first pass with no layout yet - is not "too wide": it is answered with
     * the ceiling, which is what a word that fits is drawn at anyway.
     */
    fun size(measuredWidthPx: Int, availableWidthPx: Int): Float {
        if (measuredWidthPx <= 0 || availableWidthPx <= 0 || measuredWidthPx <= availableWidthPx) {
            return CEILING_SP
        }
        return (CEILING_SP * availableWidthPx / measuredWidthPx).coerceIn(FLOOR_SP, CEILING_SP)
    }
}
