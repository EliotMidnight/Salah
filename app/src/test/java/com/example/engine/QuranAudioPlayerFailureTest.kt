package com.example.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Reciter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A recitation that cannot be streamed is a failure the reader is told about,
 * not a tone that pretends to be the recitation.
 *
 * ### What was wrong
 *
 * `playAyah`'s failure paths - the platform's error listener and the 12 s
 * watchdog - fell back to `playOfflineChime`: a synthesized tone, three and a
 * half seconds of it, which on ending called `onAyahCompleted`. That callback is
 * the autoplay chain, so a reader with no connection heard the tone march down
 * the surah one verse at a time, while the recitation banner named a reciter who
 * was never heard. The lie was in the state, too: `isPlaying` went true for the
 * chime, and nothing carried the failure at all.
 *
 * ### What is asserted
 *
 * The player is driven into the failure the way an offline phone reaches it: the
 * stream cannot be opened, so the attempt throws and the failure path runs. What
 * must hold afterwards is the state - not playing, failed - and the verse it
 * names is still the one asked for, because the failure is where the chain ends:
 * no autoplay, no substitute sound, and no verse but the one the reader tapped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranAudioPlayerFailureTest {

    @Test
    fun `a stream that cannot be prepared ends failed, plays no substitute, and does not march on`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = QuranAudioPlayer(context)

        player.playAyah(2, 255, Reciter.ALAFASY)

        val state = player.playbackState.value
        assertFalse(
            "a failed recitation still reported itself as playing - something played in its place",
            state.isPlaying
        )
        assertTrue(
            "the failure was not in the state, so no surface could tell the reader",
            state.failed
        )
        assertEquals(
            "the failure marched on: the player moved to another verse of its own",
            255,
            state.ayahNumber
        )
        assertEquals(2, state.surahNumber)
    }
}
