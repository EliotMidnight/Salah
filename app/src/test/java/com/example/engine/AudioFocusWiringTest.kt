package com.example.engine

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Reciter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/**
 * The player claims audio focus before it makes sound, answers the system when
 * the claim is contested, and releases the claim whenever the sound is gone.
 *
 * ### What was wrong
 *
 * `playAyah` streamed without ever asking: a recitation sounded through a phone
 * call, an alarm preview, another app's audio. Nothing in the suite could catch
 * it, because there was nothing to look for - here, the shadow records the
 * claim.
 *
 * ### What is asserted
 *
 * - A play claims focus before the stream is opened (the request is recorded
 *   even on this test's failing stream, where the claim is simply never
 *   answered by sound).
 * - `stop` abandons what was claimed.
 * - A transient loss, answered on a player that holds nothing, neither pauses
 *   nor lies: a state with no player must not emerge saying `paused`.
 * - On a stream that *does* hold, a transient loss pauses in place and the
 *   following gain resumes it - the full loop the reader experiences when a
 *   call arrives and ends.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioFocusWiringTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Before
    fun grantFocusRequests() {
        org.robolectric.Shadows.shadowOf(audioManager)
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    @Test
    fun `a verse claims focus before it streams, and lets go when stopped`() {
        val player = QuranAudioPlayer(context)

        player.playAyah(2, 255, Reciter.ALAFASY)
        val claimed = org.robolectric.Shadows.shadowOf(audioManager)
            .lastAudioFocusRequest
        assertNotNull(
            "the player streamed without ever asking whose turn it was",
            claimed.listener
        )
        // The stream itself cannot come up under the shadow (no media registered),
        // and the state must say so - the claim is not a substitute for sound.
        assertTrue(player.playbackState.value.failed)

        player.stop()
        assertEquals(
            "the player kept a focus claim after the sound it justified was gone",
            claimed.listener,
            org.robolectric.Shadows.shadowOf(audioManager)
                .lastAbandonedAudioFocusListener
        )
    }

    @Test
    fun `a transient loss over a player holding nothing, pauses nothing`() {
        val player = QuranAudioPlayer(context)
        player.playAyah(2, 255, Reciter.ALAFASY)
        val listener = org.robolectric.Shadows.shadowOf(audioManager)
            .lastAudioFocusRequest
            .listener

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertFalse(
            "a player with no stream answered a loss with 'paused' - nothing was lost",
            player.playbackState.value.isPaused
        )
        assertFalse(player.playbackState.value.isPlaying)

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertFalse(
            "a gain with nothing held started something that does not exist",
            player.playbackState.value.isPlaying
        )
    }

    @Test
    fun `a transient loss pauses a held stream, and its gain resumes it exactly`() {
        DataSource.toDataSource(Reciter.ALAFASY.verseUrl(2, 255))
            .let { source ->
                ShadowMediaPlayer.addMediaInfo(source, ShadowMediaPlayer.MediaInfo())
            }
        val player = QuranAudioPlayer(context)
        player.playAyah(2, 255, Reciter.ALAFASY)
        // Flush the shadow's prepared callback on the main looper: prepareAsync
        // posts it there, the way the platform does.
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(
            "the registered stream did not reach the start of playing",
            player.playbackState.value.isPlaying
        )
        val listener = org.robolectric.Shadows.shadowOf(audioManager)
            .lastAudioFocusRequest
            .listener

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertTrue(
            "a call arrived and the recitation did not yield",
            player.playbackState.value.isPaused
        )

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertTrue(
            "the call ended and the recitation did not continue where it stopped",
            player.playbackState.value.isPlaying
        )
    }
}
