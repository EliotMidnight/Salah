package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * The accent the reader has chosen.
 *
 * The design still has **one accent**; the reader picks which one. Every preset
 * is a rich, art-directed seed chosen per mode, and [accentFamily] derives the
 * full Material 3 accent family from it. That derivation is where the contrast
 * claim lives: a seed that fails the minimum is blended toward black or white
 * until it passes, so a new preset or a wallpaper palette cannot ship a pairing
 * that fails WCAG AA by accident.
 *
 * The stored form is [key] - a stable ASCII token, not the localized name the
 * settings sheet shows. Renaming a language must never change what is stored.
 */
enum class Accent(
    val key: String,
    internal val lightSeed: Color?,
    internal val darkSeed: Color?
) {
    /** The default: a deep sapphire slate. */
    SLATE("slate", Color(0xFF0D579E), Color(0xFF74ADE8)),

    PLUM("plum", Color(0xFF7B255B), Color(0xFFD083C2)),

    TEAL("teal", Color(0xFF056B6B), Color(0xFF52D0C7)),

    CLAY("clay", Color(0xFFA6411D), Color(0xFFE8986E)),

    /**
     * Follows the system's Material You palette.
     *
     * Only the wallpaper primary is read from the system; the surface palette
     * is not. On API levels without a system palette there is nothing to read,
     * so [seed] answers with [DEFAULT] and the app stays on Slate.
     */
    WALLPAPER("wallpaper", null, null);

    companion object {
        /** What a fresh install gets, and what a reset returns to. */
        val DEFAULT = SLATE

        /** The stored key, or [DEFAULT] when the key is unknown or absent. */
        fun fromKey(key: String?): Accent = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * The four accent slots a Material 3 scheme consumes.
 */
@Immutable
data class AccentFamily(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

/**
 * The floor every accent pairing is held to.
 *
 * Slightly above 4.5:1 so that float rounding cannot turn a passing seed into a
 * failing test by 0.01.
 */
internal const val MIN_ACCENT_CONTRAST = 4.6f

/** How far the light container is pulled toward the page. */
private const val LIGHT_CONTAINER_BLEND = 0.74f

/** How far the light on-container is pulled toward ink. */
private const val LIGHT_ON_CONTAINER_BLEND = 0.30f

/**
 * Resolves a choice to the accent family the scheme should use.
 *
 * [dynamicSeed] is the Material You primary, already read for the right mode;
 * it is null for every static preset and on devices without a system palette,
 * in which case [Accent.seed] answers with the art-directed colour.
 */
internal fun accentFamily(
    accent: Accent,
    dark: Boolean,
    dynamicSeed: Color? = null
): AccentFamily = familyFromSeed(dynamicSeed ?: accent.seed(dark), dark)

/**
 * The art-directed seed for [accent] in [dark], or Slate's when the choice has
 * none of its own (the wallpaper option, before the system palette is read).
 */
internal fun Accent.seed(dark: Boolean): Color =
    (if (dark) darkSeed else lightSeed)
        ?: (if (dark) Accent.DEFAULT.darkSeed else Accent.DEFAULT.lightSeed)!!

/**
 * Derives the full accent family from any seed, in either mode.
 *
 * Light mode gets a tinted container and a dark on-container; dark mode reuses
 * the primary as its container, which is the pattern the redesign already had.
 * The primary is clamped first, and because the page is pure white or pure
 * black, the on-colour inherits the same ratio without a second clamp.
 */
internal fun familyFromSeed(seed: Color, dark: Boolean): AccentFamily {
    val page = if (dark) PageDark else PageLight
    val primary = ensureContrast(seed, page, MIN_ACCENT_CONTRAST)
    return if (dark) {
        AccentFamily(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primary,
            onPrimaryContainer = Color.Black
        )
    } else {
        val container = primary.mix(PageLight, LIGHT_CONTAINER_BLEND)
        AccentFamily(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = ensureContrast(
                primary.mix(Color.Black, LIGHT_ON_CONTAINER_BLEND),
                container,
                MIN_ACCENT_CONTRAST
            )
        )
    }
}

/**
 * Blends [color] toward black or white until it clears [min] against
 * [background], preserving its hue. Returns [color] untouched when it already
 * passes.
 */
internal fun ensureContrast(color: Color, background: Color, min: Float): Color {
    if (contrastRatio(color, background) >= min) return color
    val target = if (background.luminance() > 0.5f) Color.Black else Color.White
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        if (contrastRatio(color.mix(target, middle), background) >= min) {
            high = middle
        } else {
            low = middle
        }
    }
    return color.mix(target, high)
}

/**
 * WCAG relative-luminance contrast ratio, 1:1 to 21:1.
 *
 * The tests keep their own copy of this formula on purpose: they are the
 * oracle, so they must not pass by sharing the implementation they check.
 */
internal fun contrastRatio(a: Color, b: Color): Float {
    val lighter = max(a.luminance(), b.luminance())
    val darker = min(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
