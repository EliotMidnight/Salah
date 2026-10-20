package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SALAH colour system — warm paper and deep ink.
 *
 * 1. **A soft neutral ground.** Light mode is warm ivory paper with sand-toned
 *    container surfaces; dark mode is deep navy-black with layered blue-black
 *    surfaces. Text stays near-black / near-white so the page still reads as
 *    ink on paper rather than as a tinted card stack.
 * 2. **One saturated accent at a time.** The reader picks it; the default is a
 *    deep sapphire slate. Presets keep their chroma instead of being desaturated
 *    into the neutrals, and `accentFamily` contrast-checks every pairing.
 * 3. **Semantic colour only where meaning demands it** - completion (success),
 *    silence and failure (danger), magnetic interference (warning). These are
 *    functional, not decorative.
 * 4. **Text contrast is checked, not guessed.** Every `on*` pairing below clears
 *    WCAG AA (4.5:1 for body text, 3:1 for UI boundaries). The exact ratios are
 *    noted beside each token.
 * 5. **Opaque surfaces with visible depth.** Page and cards no longer collapse
 *    into one flat white or black; the ladder gives sheets and banners a real
 *    ground without becoming a gradient bloom.
 */

// ---------------------------------------------------------------------------
// Accent - one accent at a time, chosen by the reader. The default is a deep
// sapphire slate; every preset keeps enough chroma to register as colour rather
// than as a gray. Accent.kt owns the hex values, the derivation and the contrast
// clamp. These names expose the *default* preset, so the tests that measure the
// shipped palette name one fact rather than a hex value.
// ---------------------------------------------------------------------------

/** Accent on light surfaces - the default preset, contrast-checked by Accent.kt. */
val AccentLight: Color get() = accentFamily(Accent.SLATE, dark = false).primary

/** Text/icon colour for content sitting *on* [AccentLight]. */
val OnAccentLight: Color get() = accentFamily(Accent.SLATE, dark = false).onPrimary

/** Tinted accent fill, for selected rows. */
val AccentContainerLight: Color get() = accentFamily(Accent.SLATE, dark = false).primaryContainer
val OnAccentContainerLight: Color get() = accentFamily(Accent.SLATE, dark = false).onPrimaryContainer

/** Accent on dark surfaces - the default preset, contrast-checked by Accent.kt. */
val AccentDark: Color get() = accentFamily(Accent.SLATE, dark = true).primary

/** Text/icon colour for content sitting *on* [AccentDark]. */
val OnAccentDark: Color get() = accentFamily(Accent.SLATE, dark = true).onPrimary

/** Tinted accent fill for dark mode - same hue as the accent. */
val AccentContainerDark: Color get() = accentFamily(Accent.SLATE, dark = true).primaryContainer
val OnAccentContainerDark: Color get() = accentFamily(Accent.SLATE, dark = true).onPrimaryContainer

// ---------------------------------------------------------------------------
// Neutrals - warm ivory paper in light, deep navy-black in dark. The page and
// surface share a colour; container surfaces step up a short sand/blue ladder
// so sheets and banners have a visible ground without becoming cards everywhere.
// ---------------------------------------------------------------------------

// Light. Warm paper page and surface.
val PageLight = Color(0xFFFFFBF5)            // background, surfaceContainerLowest
val SurfaceLight = Color(0xFFFFFBF5)         // surface, surfaceContainerLow (cards)
val SurfaceContainerLight = Color(0xFFF7F0E5)
val SurfaceContainerHighLight = Color(0xFFF1E8DA)
val SurfaceContainerHighestLight = Color(0xFFEAE0D0)
// Hairlines and control boundaries. 4.9:1 on the page - holds the UI minimum.
val OutlineLight = Color(0xFF7A6C5C)
val OutlineVariantLight = Color(0xFFEDE3D5)  // lighter decorative hairline
val TextPrimaryLight = Color(0xFF1A1611)     // ~17.5:1 on the page - warm near-black ink
val TextSecondaryLight = Color(0xFF5C5348)   // ~7.3:1 on the page; clears 3:1 against sky bands
val TextTertiaryLight = Color(0xFF7C7062)    // ~4.7:1 on the page - tertiary warm gray

// Dark. Deep navy-black page and surface.
val PageDark = Color(0xFF080B11)             // background
val SurfaceDark = Color(0xFF080B11)          // surface
val SurfaceContainerLowestDark = Color(0xFF080B11)
val SurfaceContainerLowDark = Color(0xFF0E1622)
val SurfaceContainerDark = Color(0xFF131D2B)
val SurfaceContainerHighDark = Color(0xFF1A2534)
val SurfaceContainerHighestDark = Color(0xFF223040)
val OutlineDark = Color(0xFF837C6E)          // 4.8:1 on the page, 3.2:1 on the highest surface
val OutlineVariantDark = Color(0xFF1A2330)   // hairline dividers
val TextPrimaryDark = Color(0xFFF7F2E9)      // ~17.7:1 on the page - warm near-white ink
val TextSecondaryDark = Color(0xFFB8AFA2)    // ~9.1:1 on the page - secondary warm gray
val TextTertiaryDark = Color(0xFF8A8174)     // ~5.1:1 on the page - tertiary warm gray

// ---------------------------------------------------------------------------
// Semantic - used only where the meaning is real. Muted so they never compete
// with the accent, but still distinguishable by hue for colour-blind readers.
// ---------------------------------------------------------------------------

/** A prayer has been marked complete. */
val SuccessLight = Color(0xFF15803D)       // 5.2:1 on white
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
