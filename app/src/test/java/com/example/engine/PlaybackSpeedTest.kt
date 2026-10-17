package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The speed pill's one tap, one step.
 *
 * The offer is five speeds and the cycle is its whole control law: a tap from
 * the last speed wraps to the first, because a reader who pushed through 2× was
 * reaching for a change, not for the end of the list. Anything the preference
 * flow could produce but the offer never made - an older build's value, a bent
 * write - snaps to its nearest step and cycles from *there*, rather than
 * producing a second value that no pill label can show.
 */
class PlaybackSpeedTest {

    @Test
    fun `one tap is one step through the offer`() {
        assertEquals(1f, nextPlaybackSpeed(0.75f))
        assertEquals(1.25f, nextPlaybackSpeed(1f))
        assertEquals(1.5f, nextPlaybackSpeed(1.25f))
        assertEquals(2f, nextPlaybackSpeed(1.5f))
    }

    @Test
    fun `the end of the offer is a wrap, not a wall`() {
        assertEquals(0.75f, nextPlaybackSpeed(2f))
    }

    @Test
    fun `a value the offer never made snaps to the nearest step and cycles`() {
        // 1.9 is 0.1 from 2× and 0.4 from 1.5×: nearest is 2×, so the cycle
        // continues from the speed the reader *almost* had.
        assertEquals(0.75f, nextPlaybackSpeed(1.9f))
        // A speed below every step is nearest 0.75×.
        assertEquals(1f, nextPlaybackSpeed(0.1f))
    }
}
