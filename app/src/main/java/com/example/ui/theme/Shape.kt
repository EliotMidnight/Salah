package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Three radii, and nothing else.
 *
 * The app previously mixed 10, 14, 16, 20, 24, 28, 32 and 36dp corners plus
 * `CircleShape` - often between sibling rows in the *same* list (the Quran
 * library drew surah rows at 20dp and page/juz/hizb rows at 28dp side by side).
 * A contained element now has exactly one shape, scaled only by size:
 *
 * - [Shapes.small]  chips, badges, text fields, small buttons
 * - [Shapes.medium] rows, cards, list items - the default for anything a user
 *                    can press
 * - [Shapes.large]  dialogs and large panels
 *
 * Sheets keep a larger top radius purely so they read as a separate layer.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

/** Bottom sheets: only the top two corners are rounded. */
val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/** Circular status dots, avatars and count badges. */
val DotShape = CircleShape

/**
 * The Quran reading page runs rounder than the rest of the app.
 *
 * The library and the reader are places a person sits with rather than scans, so
 * they get a softer silhouette than a settings list does: 20dp cards, 12dp tiles
 * and true pills for anything selectable. Still only three values, so it reads as
 * a deliberate variant of the same system rather than a second one.
 */
object QuranShape {
    /** Surah and verse cards, the continue-reading hero. */
    val card = RoundedCornerShape(20.dp)

    /** The number badge on a surah row, small chips. */
    val tile = RoundedCornerShape(12.dp)

    /** Every selectable control: tabs, option rows, the layout toggle. */
    val pill = CircleShape
}
