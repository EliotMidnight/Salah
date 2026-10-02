package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranReadingOptions

/**
 * Resolves a [QuranFontFace] into something Compose can set text in.
 *
 * Families are memoised per face because [FontFamily] construction walks the
 * resource loader, and this sits in the text style of every page of the mushaf.
 *
 * ### There is no baseline shift here, and that is a decision
 *
 * `QuranFontFace` used to carry a `baselineShiftSp` - `-0.5f` for the two Amiri
 * faces, `-1f` for Harmattan - copied faithfully into this class and then **never
 * applied**. Two things were done with that, and both are worth recording.
 *
 * **The dead value is gone**, because a value nobody reads is a second place for
 * the same fact to go wrong, and because its own doc claimed a remedy the rendering
 * already had.
 *
 * **The remedy it claimed to be is [QuranFontFace.lineHeightFactor]**, which is
 * per-face and does work: Harmattan is set at 2.35 and Lateef at 1.90, which is the
 * difference between a face with the deepest descender in the set and the densest.
 *
 * **Applying the shift would have been a regression, and this was checked rather
 * than assumed.** Every bundled face was rendered on the densest page in the book
 * (page 2, Al-Baqarah) with and without it, and with it the text moved *up* inside
 * each line - which is exactly where Arabic's marks are. A negative shift takes the
 * room away from the shadda, fatha and dagger alif that sit above the baseline, and
 * the pages came back with the lines closer together and the marks crowded towards
 * the descenders above. The premise that these faces "need a nudge" is true of faces
 * whose marks sit *low*; Arabic's crowded marks sit high.
 *
 * `PlatformTextStyle` is worth mentioning because it is the obvious thing to reach
 * for and it cannot do this: in this Compose version it carries only
 * `includeFontPadding`, a span style and a paragraph style. The API that would be
 * needed is `TextStyle.baselineShift`, and it is deliberately not set.
 *
 * `QuranFontFaceProbeTest` records a baseline for every face so that any future
 * change to leading is visible in a diff rather than in a bug report.
 */
@Immutable
data class QuranTypeface(
    val face: QuranFontFace,
    val family: FontFamily,
    val lineHeightFactor: Float
) {
    /**
     * The Arabic reading style at [scale].
     *
     * Every Arabic surface in the reader derives from here - the flowing page,
     * the per-ayah block, the verse inspector and the index preview - so the
     * three cannot drift apart on leading the way they used to.
     *
     * The line height is [QuranReadingOptions.ARABIC_BASE_SP] times the face's
     * own multiplier times [scale], computed in sp rather than scaled from an
     * already-laid-out style: a Nastaliq face and a Naskh face need different
     * multipliers at every size, not just at 100%.
     *
     * It is also the face's *only* typographic adjustment, and it is the one that
     * has to exist. Every bundled face was checked on the book's densest page at
     * 100% and none of them clips or collides; that is what the per-face multiplier
     * buys, and a baseline nudge would only take room away from the marks.
     */
    fun arabicStyle(
        scale: Float,
        color: Color,
        align: TextAlign = TextAlign.Start
    ): TextStyle {
        val size = QuranReadingOptions.ARABIC_BASE_SP * scale
        return TextStyle(
            fontFamily = family,
            fontSize = size.sp,
            lineHeight = (size * lineHeightFactor).sp,
            color = color,
            // Quranic text is RTL by script, not by interface language. An
            // English or French reader still needs the Arabic laid out RTL, and
            // an Arabic reader reading the English translation needs that LTR.
            textDirection = TextDirection.Rtl,
            textAlign = align
        )
    }
}

/**
 * The reader's typefaces, resolved once.
 *
 * Deliberately separate from [ArabicFamily]: that is the app-wide serif used
 * for surah names and inline Arabic, while this is the mushaf's own face,
 * which changes with the reader's preferences.
 */
object QuranFonts {

    private val cache = HashMap<QuranFontFace, QuranTypeface>()

    /** The typeface for [face], memoised. Safe to call from any composition. */
    @Composable
    fun of(face: QuranFontFace): QuranTypeface = remember(face) { typefaceFor(face) }

    /** The non-composable resolution, for tests and for one-shot style building. */
    fun typefaceFor(face: QuranFontFace): QuranTypeface =
        cache.getOrPut(face) {
            QuranTypeface(
                face = face,
                family = FontFamily(Font(face.fontRes)),
                lineHeightFactor = face.lineHeightFactor
            )
        }

    /**
     * The reader's current typeface, readable without a parameter.
     *
     * Provided once by the reader around everything it draws, and read by the
     * shared verse components so none of them has to be handed a face it would
     * only pass straight on. Outside a reader it resolves to the default
     * bundled face, which is what the index previews want anyway - they are not
     * inside the reader's typeface and should not pretend to be.
     */
    val LocalQuranTypeface: ProvidableCompositionLocal<QuranTypeface> =
        staticCompositionLocalOf { typefaceFor(QuranFontFace.AMIRI) }

    /** Wraps [content] in the reader's typeface. */
    @Composable
    fun Provide(face: QuranFontFace, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalQuranTypeface provides of(face), content = content)
    }
}
