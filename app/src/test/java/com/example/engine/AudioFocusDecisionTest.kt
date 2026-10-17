package com.example.engine

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The branch table of the audio focus listener, asked as a pure question.
 *
 * The listener itself runs inside [QuranAudioPlayer] on the system's thread -
 * untestable there without simulating a whole platform's worth of competing
 * sound. What is testable, and what is worth testing, is the *decision*: what a
 * focus change means for whatever the player holds.
 *
 * **What was wrong** until this table existed: nothing - the player never asked
 * for focus at all, so a recitation sounded straight through a phone call, an
 * alarm preview, or another app's audio, because no one was told whose turn
 * it was. The branches below are the contract of asking.
 */
class AudioFocusDecisionTest {

    @Test
    fun `a gain resumes only a stream the system paused itself`() {
        assertEquals(
            "our own loss-pause is ours to undo when the other sound leaves",
            FocusAction.RESUME_HELD,
            focusActionFor(AudioManager.AUDIOFOCUS_GAIN, pausedForFocusLoss = true)
        )
        assertEquals(
            "a gain with no loss of ours must not start what the user paused",
            FocusAction.NOTHING,
            focusActionFor(AudioManager.AUDIOFOCUS_GAIN, pausedForFocusLoss = false)
        )
    }

    @Test
    fun `a transient loss pauses and remembers, ducking included`() {
        assertEquals(
            FocusAction.PAUSE_UNTIL_REGAIN,
            focusActionFor(
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                pausedForFocusLoss = false
            )
        )
        assertEquals(
            FocusAction.PAUSE_UNTIL_REGAIN,
            focusActionFor(
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                pausedForFocusLoss = false
            )
        )
        assertEquals(
            FocusAction.PAUSE_UNTIL_REGAIN,
            focusActionFor(
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                pausedForFocusLoss = true
            )
        )
    }

    @Test
    fun `a permanent loss stops everything, from either state`() {
        assertEquals(
            FocusAction.STOP_ALL,
            focusActionFor(AudioManager.AUDIOFOCUS_LOSS, pausedForFocusLoss = false)
        )
        assertEquals(
            FocusAction.STOP_ALL,
            focusActionFor(AudioManager.AUDIOFOCUS_LOSS, pausedForFocusLoss = true)
        )
    }

    @Test
    fun `an unknown change is not an opportunity to move a stream`() {
        assertEquals(FocusAction.NOTHING, focusActionFor(-999, pausedForFocusLoss = false))
        assertEquals(FocusAction.NOTHING, focusActionFor(-999, pausedForFocusLoss = true))
    }
}
