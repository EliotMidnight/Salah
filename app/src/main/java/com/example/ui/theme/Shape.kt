package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive (M3E) Shapes for SALAH - Android 16 specification.
 * Emphasizes organic, expressive curved geometry with distinctive, fluid curvature
 * for pills, chips, cards, modals, and interactive surfaces.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

val PillShape = CircleShape
val HeroSurfaceShape = RoundedCornerShape(32.dp)
val CardSurfaceShape = RoundedCornerShape(24.dp)
val ChipSurfaceShape = RoundedCornerShape(16.dp)
val SheetSurfaceShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/** Canonical section-card shape used across Settings, Prayer, Qibla, and lists. */
val SectionCardShape = Shapes.large

/** Canonical row/list-item shape used for prayer rows, day lists, and pills. */
val RowCardShape = Shapes.medium
