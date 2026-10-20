package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.example.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * iOS-style type scale, mapped onto Material 3's slots.
 *
 * Levels (large → small): Large Title → Title → Headline → Body → Footnote → Caption.
 * System-first font stack: SF on Apple platforms, the system default everywhere else.
 * Semibold emphasis instead of heavy weights; near-zero body tracking; generous
 * line rhythm so hierarchy reads from type alone.
 */

private val Sans = FontFamily.Default

/** Arabic and Quranic text. Serif gives the script the stroke contrast it needs. */
val ArabicFamily = FontFamily.Serif

/**
 * Handwriting, for the one place on the Today page that wants to feel written
 * rather than typeset - the prayer's name, at headline size.
 */
val HandwritingFamily = FontFamily(
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold)
)

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    // -----------------------------------------------------------------------
    // Large Title - reserved for one number per screen: the countdown, the
    // Qibla bearing, the Today prayer name.
    // -----------------------------------------------------------------------
    displayLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // -----------------------------------------------------------------------
    // Title - screen titles and the largest on-screen headings.
    // -----------------------------------------------------------------------
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // -----------------------------------------------------------------------
    // Headline - list row titles, card titles, dialog titles.
    // -----------------------------------------------------------------------
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // -----------------------------------------------------------------------
    // Body - long-form translation text, Quran English, explanatory copy.
    // Near-zero tracking so paragraphs read as calm, even texture.
    // -----------------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // -----------------------------------------------------------------------
    // Label - buttons, tabs, field labels, section headers.
    // -----------------------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = trim
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = trim
    )
)

/**
 * Small all-caps label used for section headers. Sits between Footnote and
 * Caption in the scale: small, semibold, lightly tracked.
 */
val SectionLabelStyle: TextStyle
    get() = Typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
