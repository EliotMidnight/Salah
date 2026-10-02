package com.example.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Which way to turn, and by how much.
 */
data class QiblaGuidance(
    /** Which way to turn, or that the target is already ahead. */
    val direction: Direction,
    /** How far, in whole degrees. Zero when aligned. */
    val degrees: Int
) {
    enum class Direction { ON_TARGET, RIGHT, LEFT }

    /** True once the target is inside the alignment window. */
    val isAligned: Boolean get() = direction == Direction.ON_TARGET

    /**
     * Guidance from a signed difference, in degrees.
     */
    companion object {
        /**
         * The alignment window, in degrees.
         */
        const val TOLERANCE_DEGREES = 5

        fun fromRelative(relativeDegrees: Float): QiblaGuidance {
            val magnitude = abs(relativeDegrees).roundToInt()
            return when {
                magnitude <= TOLERANCE_DEGREES ->
                    QiblaGuidance(Direction.ON_TARGET, 0)

                relativeDegrees > 0f ->
                    QiblaGuidance(Direction.RIGHT, magnitude)

                else ->
                    QiblaGuidance(Direction.LEFT, magnitude)
            }
        }
    }
}
