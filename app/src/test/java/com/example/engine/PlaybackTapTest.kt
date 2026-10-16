package com.example.engine

import com.example.data.model.Reciter
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What a tap on a verse's play control means.
 *
 * ### Why this is a test and not a screenshot
 *
 * The defect was a dead end the screen cannot show: pausing a verse set "not
 * playing", so the next tap on that same verse took the same branch as a tap on
 * any other one and played it from the beginning - a fresh download, not a
 * continuation - while the player's `resume()` had no caller at all. A picture of
 * the control before and after the tap shows a play icon both times; only the
 * meaning of the tap separates them.
 *
 * ### The whole book
 *
 * The decision is positional - "is this the verse the player holds, and in which
 * of its two held states" - so it is walked over every verse of the corpus, each
 * time with one *other* verse to pin the answer for a tap that is not on the held
 * one. The first and last verses of the book are in the walk by construction,
 * which is where a test of chosen examples would need its own special cases.
 */
class PlaybackTapTest {

    @Test
    fun `a tap on the held verse ends a play or continues a pause, and any other verse plays`() {
        val everyVerse = (1..114).flatMap { QuranBrowse.ayahsInSurah(it) }
        assertEquals("the corpus is not the whole book", 6_236, everyVerse.size)

        for (index in everyVerse.indices) {
            val verse = everyVerse[index]
            val another = everyVerse[(index + 1) % everyVerse.size]

            val playing = AudioPlaybackState(
                isPlaying = true,
                surahNumber = verse.surahNumber,
                ayahNumber = verse.ayahNumber,
                reciter = Reciter.ALAFASY,
            )
            assertEquals(
                "a tap on ${verse.ref} while it is being recited does not end it",
                PlaybackTap.PAUSE,
                playbackTapFor(playing, verse.surahNumber, verse.ayahNumber)
            )
            assertEquals(
                "a tap on ${another.ref} while ${verse.ref} is being recited does not play it",
                PlaybackTap.PLAY,
                playbackTapFor(playing, another.surahNumber, another.ayahNumber)
            )

            val paused = playing.copy(isPlaying = false, isPaused = true)
            assertEquals(
                "a tap on ${verse.ref} after pausing it does not continue it",
                PlaybackTap.RESUME,
                playbackTapFor(paused, verse.surahNumber, verse.ayahNumber)
            )
            assertEquals(
                "a tap on ${another.ref} while ${verse.ref} is paused does not play it",
                PlaybackTap.PLAY,
                playbackTapFor(paused, another.surahNumber, another.ayahNumber)
            )

            // Out of silence, and the state's default verse is 1:1 - a default, not
            // a claim that the first verse of the book is being held.
            val idle = AudioPlaybackState()
            assertEquals(
                "a tap on ${verse.ref} out of silence does not play it",
                PlaybackTap.PLAY,
                playbackTapFor(idle, verse.surahNumber, verse.ayahNumber)
            )
        }
    }
}
