package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Reciter
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/**
 * The chain: after a verse, the next verse of its surah starts from the
 * hand-off the player prepared while the first was still sounding - not from
 * the caller tearing the player down and preparing a new one at the seam.
 *
 * ### What was wrong
 *
 * The chain used to live in a completion callback in the view model: every
 * verse completed, tore its player down, and a fresh `MediaPlayer` prepared the
 * next verse *from that moment* - the whole download delay re-lit at every
 * seam, which on a slow connection is a silence between verses that outlasted
 * the verse itself. And because the chain was the caller's arrangement, the
 * player could not manage the seam at all.
 *
 * ### What is asserted
 *
 * The two verses' URLs are registered as shadow sources (the way the network
 * would feed them), the created players are captured as they are made, and the
 * prepared/completion events are driven exactly as the platform drives them:
 * - after the first verse completes, the **player itself** has advanced the
 *   recitation to the next verse. Under the caller-owned chain, this exact
 *   sequence of call-and-event left the recitation stopped, so this assertion
 *   is the whole delta of the change made observable;
 * - a verse with nothing after it ends the chain quietly - no sound, no hold,
 *   a claim given back;
 * - a prefetch that cannot even be set up does not stop the verse *sounding*:
 *   the fault will be paid at the seam, and the sound of the present stands
 *   until then;
 * - at that seam, a fallback prepare that also fails ends the chain honestly:
 *   the failure state names it, the way an offline verse always did;
 * - `stop` tears down both the sounding and the queued players, so nothing of
 *   a previous chain leaks into the next play.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranAudioChainTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val player = QuranAudioPlayer(context)
    private val created = ArrayList<MediaPlayer>()

    @Before
    fun captureCreatedPlayers() {
        ShadowMediaPlayer.setCreateListener { mp, _ -> created.add(mp) }
    }

    @After
    fun stopEverything() {
        player.stop()
        ShadowMediaPlayer.setCreateListener(null)
    }

    private fun registerVerse(surah: Int, ayah: Int) {
        ShadowMediaPlayer.addMediaInfo(
            DataSource.toDataSource(Reciter.ALAFASY.verseUrl(surah, ayah)),
            ShadowMediaPlayer.MediaInfo()
        )
    }

    private fun shadowFor(surah: Int, ayah: Int): ShadowMediaPlayer =
        Shadows.shadowOf(
            created.first {
                Shadows.shadowOf(it).sourceUri.toString() ==
                    Reciter.ALAFASY.verseUrl(surah, ayah)
            }
        )

    @Test
    fun `the player advances the chain itself, without a caller at the seam`() {
        registerVerse(2, 255)
        registerVerse(2, 256)
        player.playAyah(2, 255, Reciter.ALAFASY) { surah, ayah ->
            if (ayah < 256) surah to (ayah + 1) else null
        }
        shadowFor(2, 255).invokePreparedListener()
        assertTrue(
            "the registered verse never reached the start of playing",
            player.playbackState.value.isPlaying
        )

        // The successor is prepared during the first verse, so the seam is a
        // hand-off and not a fresh download.
        shadowFor(2, 256).invokePreparedListener()

        // And the completion arrives, as the platform's end-of-stream does.
        shadowFor(2, 255).invokeCompletionListener()

        assertEquals(
            "the chain did not advance at the seam - the recitation stopped where it was",
            256,
            player.playbackState.value.ayahNumber
        )
        assertTrue(
            "the next verse is not sounding after the seam",
            player.playbackState.value.isPlaying
        )

        shadowFor(2, 256).invokeCompletionListener()
        assertFalse(
            "the chain did not end at the surah's last verse",
            player.playbackState.value.isPlaying
        )
        assertFalse(player.playbackState.value.isPaused)
    }

    @Test
    fun `a prefetch that cannot be set up does not stop the sounding verse`() {
        registerVerse(2, 255)
        ShadowMediaPlayer.addException(
            DataSource.toDataSource(Reciter.ALAFASY.verseUrl(2, 256)),
            IOException("offline")
        )
        player.playAyah(2, 255, Reciter.ALAFASY) { surah, ayah -> surah to (ayah + 1) }
        shadowFor(2, 255).invokePreparedListener()

        // The prefetch attempt already failed with the registered exception
        // before the first prepared callback got through... actually before
        // this test reaches here: assert the present still stands.
        assertTrue(
            "a failed prefetch stopped the verse that was correctly playing",
            player.playbackState.value.isPlaying
        )
        assertFalse(
            "a failed prefetch reported a failure for a verse still sounding",
            player.playbackState.value.failed
        )

        // At the seam, the chain falls back to the sequential prepare, which
        // meets the same refused connection and fails like any verse would.
        shadowFor(2, 255).invokeCompletionListener()
        assertTrue(
            "the degraded seam failed silently and the surah never moved on",
            player.playbackState.value.failed
        )
        assertFalse(player.playbackState.value.isPlaying)
    }

    @Test
    fun `stop tears down the sounding verse and the queued one`() {
        registerVerse(2, 255)
        registerVerse(2, 256)
        player.playAyah(2, 255, Reciter.ALAFASY) { surah, ayah -> surah to (ayah + 1) }
        shadowFor(2, 255).invokePreparedListener()
        shadowFor(2, 256).invokePreparedListener()

        player.stop()

        assertFalse(player.playbackState.value.isPlaying)
        assertFalse(player.playbackState.value.isPaused)
        assertFalse(player.playbackState.value.failed)
        assertEquals(
            "the sounding player was not released by stop",
            ShadowMediaPlayer.State.END,
            shadowFor(2, 255).state
        )
        assertEquals(
            "the queued player leaked: it was never released",
            ShadowMediaPlayer.State.END,
            shadowFor(2, 256).state
        )
    }
}
