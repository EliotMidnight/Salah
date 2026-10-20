package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Three radii, and nothing else.
 *
 * - [Shapes.small]  chips, badges, text fields, small buttons
 * - [Shapes.medium] rows, cards, list items - the default for anything a user
 *                    can press
 * - [Shapes.large]  dialogs and large panels
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
