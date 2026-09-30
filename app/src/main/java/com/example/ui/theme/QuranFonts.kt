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
 * The fallback is the reason this is a function rather than a lookup table. A
 * face whose file is not bundled resolves to the app's Arabic serif, so
 * choosing an unavailable font renders the Quran correctly - in the default
 * face - instead of silently drawing nothing or throwing on a missing resource.
 *
 * Families are memoised per face because [FontFamily] construction walks the
 * resource loader, and this sits in the text style of every page of the mushaf.
 */
@Immutable
data class QuranTypeface(
    val face: QuranFontFace,
    val family: FontFamily,
    val lineHeightFactor: Float,
    val baselineShiftSp: Float
) {
    /** True when the requested face is genuinely the one being rendered. */
    val isResolved: Boolean get() = face.isBundled

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
                family = face.fontRes?.let { FontFamily(Font(it)) } ?: ArabicFamily,
                lineHeightFactor = face.lineHeightFactor,
                baselineShiftSp = face.baselineShiftSp
            )
        }

    /** The face the reader should actually draw, given what is on disk. */
    fun effective(face: QuranFontFace): QuranFontFace =
        if (face.isBundled) face else QuranFontFace.AMIRI

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
