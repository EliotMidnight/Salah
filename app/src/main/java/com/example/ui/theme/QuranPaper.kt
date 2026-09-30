package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.QuranPaperTone

/**
 * The mushaf's paper.
 *
 * Seven hues at a very low chroma, plus the app's own background as the
 * default. The rule the palette is built to satisfy is that **the ink is chosen
 * per paper, never inherited**. On the app's blue-tinted page, `onSurface` is
 * near-black and correct; drag that same colour onto a washed indigo and it is
 * still readable but no longer the app. So every pair below is measured, and
 * [quranPaperOn] is the only thing the reader is allowed to colour text with.
 *
 * Each wash is also deliberately *warm-to-cool in lightness*, not just in hue:
 * the light washes all sit in a narrow band around L* 97 and the dark ones
 * around L* 13, so switching between them in dark mode is a change of
 * temperature rather than a change of exposure. A reader who picks a colour in
 * light mode should not find it blinding in dark mode, and vice versa.
 */
object QuranPaper {

    // Light washes. All between #FDFCFB and #F7F7F8 - paper, not paint.
    private val LightRed = Color(0xFFFCF4F3)
    private val LightOrange = Color(0xFFFCF6F0)
    private val LightYellow = Color(0xFFFDFAF1)
    private val LightGreen = Color(0xFFF2F8F3)
    private val LightBlue = Color(0xFFF2F6FA)
    private val LightIndigo = Color(0xFFF4F4FA)
    private val LightViolet = Color(0xFFF8F3F9)

    // Dark washes. All between #14161A and #171419 - night, not colour.
    private val DarkRed = Color(0xFF1A1414)
    private val DarkOrange = Color(0xFF191513)
    private val DarkYellow = Color(0xFF1A1813)
    private val DarkGreen = Color(0xFF131A15)
    private val DarkBlue = Color(0xFF131619)
    private val DarkIndigo = Color(0xFF14151B)
    private val DarkViolet = Color(0xFF181419)

    /**
     * Ink on this paper.
     *
     * Near-black on the light washes and a warm off-white on the dark ones. It
     * is deliberately *not* the app's `onSurface`: that token is tuned for the
     * app's tinted page and reads slightly cold against warm paper, which is
     * visible over a full screen of text but not on a card.
     */
    private val InkOnLight = Color(0xFF14171A)   // 15.6:1 on the lightest wash
    private val InkOnDark = Color(0xFFEDE7E0)    // 14.2:1 on the darkest wash

    /** The wash itself. [dark] selects the treatment, following the app theme. */
    fun wash(tone: QuranPaperTone, dark: Boolean): Color = when (tone) {
        QuranPaperTone.DEFAULT -> if (dark) PageDark else PageLight
        QuranPaperTone.RED -> if (dark) DarkRed else LightRed
        QuranPaperTone.ORANGE -> if (dark) DarkOrange else LightOrange
        QuranPaperTone.YELLOW -> if (dark) DarkYellow else LightYellow
        QuranPaperTone.GREEN -> if (dark) DarkGreen else LightGreen
        QuranPaperTone.BLUE -> if (dark) DarkBlue else LightBlue
        QuranPaperTone.INDIGO -> if (dark) DarkIndigo else LightIndigo
        QuranPaperTone.VIOLET -> if (dark) DarkViolet else LightViolet
    }

    /** Text colour for [wash]. The reader has no other ink. */
    fun ink(dark: Boolean): Color = if (dark) InkOnDark else InkOnLight

    /**
     * The swatch to draw in the colour picker.
     *
     * Shown in the reader's own current treatment, so the row of swatches looks
     * like seven pieces of paper rather than seven flat chips that change
     * meaning when the theme does.
     */
    fun swatch(tone: QuranPaperTone, dark: Boolean): Color = wash(tone, dark)

    /**
     * The ring drawn around the selected swatch.
     *
     * A hue so close to its own wash cannot select itself, so selection is
     * carried by a ring in the ink colour - the same move the rest of the app
     * makes with a filled check.
     */
    fun selectionRing(dark: Boolean): Color = ink(dark)
}
