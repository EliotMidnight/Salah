package com.example.engine

/**
 * The speeds a recitation can be played at, in the order the reader cycles
 * through them.
 *
 * Five, and these five: slower than 0.75× turns recitation into a lecture, and
 * faster than 2× turns it into an exercise, and the points between are the ones
 * audio players have settled on. The list is the offer - a pill cycles it, a
 * stored value snaps into it, and nothing between the steps is reachable, which
 * is what keeps the banner's label honest about what will actually play.
 */
val PLAYBACK_SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * The next speed the reader is offered, one step round from [after].
 *
 * A stored value the enum does not know (an old build's, a corrupted write) is
 * snapped to its nearest step first, so a cycle resumes from a *real* speed
 * rather than from one that was never offered. Wraps at the end: the reader who
 * passes 2× has reached for slower, not for the wall.
 *
 * Pure, like the rest of the player's decisions: a pill tap asks this and
 * applies the answer.
 */
internal fun nextPlaybackSpeed(after: Float): Float {
    val current = PLAYBACK_SPEEDS.minByOrNull { kotlin.math.abs(it - after) } ?: 1f
    val index = PLAYBACK_SPEEDS.indexOf(current)
    return PLAYBACK_SPEEDS[(index + 1) % PLAYBACK_SPEEDS.size]
}
