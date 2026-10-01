package com.example.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Which way to turn, and by how much.
 *
 * ### Why this is a value and not a boolean
 *
 * `isFacingQibla` answers "am I there yet", which is the only question a boolean can
 * answer and not the only one a reader has. Standing with the phone in one hand,
 * the useful thing is not "no" - it is "turn right, two degrees". A screen that can
 * only say "not yet" leaves the reader holding the compass against their own
 * judgement of which way their body is turning, which is exactly the arithmetic the
 * compass is supposed to do for them.
 *
 * So the guidance is a direction and a magnitude, and it is derived from the same
 * signed angle the dial is drawn from - so the banner and the needle cannot
 * disagree. That disagreement is a real failure of compass screens: two degrees
 * apart in the world is a linear difference of 358, and a screen that compares
 * linearly says "not aligned" beside a needle pointing straight at the target.
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
     *
     * [relativeDegrees] must already be folded circularly - see
     * [QiblaEngine.calculateRelativeAngle], which does - because a raw subtraction
     * of 359 from 2 is 357, and that would send a reader the long way round for a
     * two-degree turn.
     */
    companion object {
        /**
         * The alignment window, in degrees.
         *
         * Five is deliberately forgiving. A compass reading jitters by a degree or
         * two just from someone standing still, and a phone held at chest height in
         * one hand is worse than that. A tighter window makes the aligned state
         * flicker as the reading crosses the threshold, and a reader who has to
         * watch for a flicker rather than being *told* they are aligned will start
         * correcting past the target and correcting back.
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
