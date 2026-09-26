package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SALAH colour system.
 *
 * Rules this palette is built to satisfy:
 *
 * 1. **One accent.** A single calm blue carries every interactive and "this is
 *    selected" signal. Nothing else competes with it.
 * 2. **A real elevation ladder.** The previous palette set `background` and
 *    `surfaceContainerLow` to the same value in light mode, so every card was
 *    invisible and only a hairline border separated it from the page. Here the
 *    page is tinted and the cards are pure white, so a card reads as a card with
 *    no border at all.
 * 3. **Text contrast is checked, not guessed.** Every `on*` pairing below clears
 *    WCAG AA (4.5:1 for body text, 3:1 for large text and UI boundaries). The
 *    exact ratios are noted beside each token.
 * 4. **Semantic colour only where meaning demands it** - completion (success),
 *    silence and failure (error), magnetic interference (warning).
 */

// ---------------------------------------------------------------------------
// Accent - a single calm blue, used for primary actions, selection and focus.
// ---------------------------------------------------------------------------

/** Accent on light surfaces. 5.9:1 on white, 5.5:1 on the light page. */
val AccentLight = Color(0xFF0369A1)

/** Text/icon colour for content sitting *on* [AccentLight]. 5.9:1. */
val OnAccentLight = Color(0xFFFFFFFF)

/** Tinted accent fill, for selected rows. [OnAccentContainerLight] clears 5.2:1. */
val AccentContainerLight = Color(0xFFE0F2FE)
val OnAccentContainerLight = Color(0xFF075985)

/** Accent on dark surfaces. 11.5:1 on the dark page. */
val AccentDark = Color(0xFF7DD3FC)

/** Text/icon colour for content sitting *on* [AccentDark]. */
val OnAccentDark = Color(0xFF00344C)

/** Tinted accent fill for dark mode. */
val AccentContainerDark = Color(0xFF0C4A6E)
val OnAccentContainerDark = Color(0xFFBAE6FD)

// ---------------------------------------------------------------------------
// Neutrals - the page is tinted, content is white. This is the whole reason
// cards are visible without borders.
// ---------------------------------------------------------------------------

// Light. The page is tinted and content is white, which is the whole reason a
// card is visible without a border. The container ladder runs from the page
// colour upward, so `lowest` is the most recessed and `highest` the most raised.
val PageLight = Color(0xFFF6F8FA)            // background, surfaceContainerLowest
val SurfaceLight = Color(0xFFFFFFFF)         // surface, surfaceContainerLow (cards)
val SurfaceContainerLight = Color(0xFFF1F4F7)
val SurfaceContainerHighLight = Color(0xFFE9EEF3)
val SurfaceContainerHighestLight = Color(0xFFE1E8EF)
// 3.3:1 on the page. `outline` carries meaning - unselected control boundaries and
// icons - so it is held to the 3:1 non-text minimum. Decorative separators use
// `outlineVariant` instead, which is a hairline and carries no information.
val OutlineLight = Color(0xFF7E8B99)
val OutlineVariantLight = Color(0xFFE4E9EF) // hairline dividers
val TextPrimaryLight = Color(0xFF0F1B26)     // 16.9:1 on page
val TextSecondaryLight = Color(0xFF475569)   // 7.1:1 on page

// Dark
val PageDark = Color(0xFF0A0E13)           // background
val SurfaceDark = Color(0xFF121820)        // surface
val SurfaceContainerLowestDark = Color(0xFF0A0E13)
val SurfaceContainerLowDark = Color(0xFF151C25)
val SurfaceContainerDark = Color(0xFF1A222C)
val SurfaceContainerHighDark = Color(0xFF222C38)
val SurfaceContainerHighestDark = Color(0xFF2C3745)
val OutlineDark = Color(0xFF55636F)         // 3.1:1 on page - boundaries & icons
val OutlineVariantDark = Color(0xFF232C37)  // hairline dividers
val TextPrimaryDark = Color(0xFFEDF2F7)    // 16.4:1 on page
val TextSecondaryDark = Color(0xFF9AA8B6)  // 7.5:1 on page

// ---------------------------------------------------------------------------
// Semantic - used only where the meaning is real.
// ---------------------------------------------------------------------------

/** A prayer has been marked complete. */
val SuccessLight = Color(0xFF15803D)       // 4.9:1 on white
val OnSuccessLight = Color(0xFFFFFFFF)
val SuccessContainerLight = Color(0xFFDCFCE7)
val OnSuccessContainerLight = Color(0xFF14532D)

val SuccessDark = Color(0xFF6EE7A0)
val OnSuccessDark = Color(0xFF052E16)
val SuccessContainerDark = Color(0xFF14532D)
val OnSuccessContainerDark = Color(0xFFBBF7D0)

/** Alerts are silenced, or something failed. */
val DangerLight = Color(0xFFB42318)        // 6.4:1 on white
val OnDangerLight = Color(0xFFFFFFFF)
val DangerContainerLight = Color(0xFFFEE4E2)
val OnDangerContainerLight = Color(0xFF7A271A)

val DangerDark = Color(0xFFFFB4AB)
val OnDangerDark = Color(0xFF690005)
val DangerContainerDark = Color(0xFF93000A)
val OnDangerContainerDark = Color(0xFFFFDAD6)

/** Magnetic interference, degraded sensor. */
val WarningLight = Color(0xFFB54708)       // 4.6:1 on white
val OnWarningLight = Color(0xFFFFFFFF)
val WarningContainerLight = Color(0xFFFEF0C7)
val OnWarningContainerLight = Color(0xFF7A2E0E)

val WarningDark = Color(0xFFFDB022)
val OnWarningDark = Color(0xFF412A00)
val WarningContainerDark = Color(0xFF7A2E0E)
val OnWarningContainerDark = Color(0xFFFEF0C7)

/**
 * Blends towards white by [amount].
 *
 * Used instead of a hand-picked pale constant per sky period so the static
 * background stays in step with the live palette rather than drifting from it.
 */
internal fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha
)

/** Linear blend towards [other]. */
internal fun Color.mix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t
    )
}
