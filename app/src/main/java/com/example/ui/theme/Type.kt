package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * A deliberately short type scale.
 *
 * The previous scale carried 15 Material slots and the screens then bypassed it
 * entirely with ~19 hardcoded `sp` literals (10, 11, 11.5, 12, 12.5, 13, 13.5,
 * 14, 14.5, 15, 16, 17, 18, 20, 22, 24, 28sp ...). Nothing lined up and several
 * of those sizes failed contrast at 4.5:1.
 *
 * Every text style in the app now comes from this table. Sizes are trimmed to
 * the ones the product actually needs, `bodySmall` is raised from 12sp to 13sp
 * because 12sp is below comfortable reading size for the secondary metadata this
 * app is full of, and line heights are opened up slightly for readability.
 */

private val Sans = FontFamily.Default

/** Arabic and Quranic text. Serif gives the script the stroke contrast it needs. */
val ArabicFamily = FontFamily.Serif

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    // Reserved for one number per screen: the countdown, the Qibla bearing.
    displayLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = trim
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = trim
    ),
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // Screen titles.
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),

    // The workhorse: list row titles, card titles, dialog titles.
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = trim
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = trim
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = trim
    ),

    // Body copy. Long-form translation text and Quran English sit here.
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = trim
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = trim
    ),
    // Raised from 12sp: this style carries most of the app's metadata, and 12sp
    // failed 4.5:1 against several of the old surface tints.
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = trim
    ),

    // Buttons, tabs, field labels, section headers.
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = trim
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        lineHeightStyle = trim
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        lineHeightStyle = trim
    )
)

/**
 * Small all-caps label used for section headers.
 *
 * Not a Material slot because it is a distinct thing: a quiet group label, not a
 * heading competing with the screen title.
 */
val SectionLabelStyle: TextStyle
    get() = Typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
