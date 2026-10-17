package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.example.data.model.Reciter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    /**
     * True while a stream is held paused, so the next tap continues it instead of
     * starting the verse over.
     *
     * Separate from [isPlaying], which cannot carry it: a paused verse and a
     * finished one are both "not playing", and only one of them has a place to go
     * back to.
     */
    val isPaused: Boolean = false,
    /**
     * True when the last stream could not be prepared - the connection never
     * answered, or the request was refused.
     *
     * A failure is not silence: the surfaces read this to say *why* there is no
     * sound. It used to be answered with a synthesized chime, which is a
     * different sound pretending to be the recitation.
     */
    val failed: Boolean = false,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    /**
     * The reciter actually being streamed, or null when what is playing is not
     * a recitation - a Settings preview, for instance.
     *
     * Null rather than a default reciter, because the recitation banner reads
     * this and no banner may ever name a stream reciting when the reader could
     * be hearing an adhan preview just as easily.
     */
    val reciter: Reciter? = null,
)

/** What a tap on a verse's play control means, from the player's own state. */
internal enum class PlaybackTap { PLAY, PAUSE, RESUME }

/**
 * The meaning of a tap on the verse [surahNumber]:[ayahNumber].
 *
 * Three answers, because two are not enough: the play control asked only "is the
 * verse I hold playing" - [AudioPlaybackState.isPlaying] - so the tap that should
 * continue a paused recitation saw "not playing" and started the verse over from
 * zero, re-downloading it, while [QuranAudioPlayer.resume] sat with no caller.
 * The state read here is the player's own, not the surfaces' copy of it.
 */
internal fun playbackTapFor(
    state: AudioPlaybackState,
    surahNumber: Int,
    ayahNumber: Int
): PlaybackTap {
    val isHeldVerse = state.surahNumber == surahNumber && state.ayahNumber == ayahNumber
    return when {
        isHeldVerse && state.isPlaying -> PlaybackTap.PAUSE
        isHeldVerse && state.isPaused -> PlaybackTap.RESUME
        else -> PlaybackTap.PLAY
    }
}

/**
 * The preview tone for a reciter, in Hz.
 *
 * A placeholder tone: a Settings preview cannot be a sample of a reciter's
 * voice, so each reciter gets its own steady pitch and the reader hears that
 * *this* reciter's mapping fired. Keyed by `when` over [Reciter] and not by
 * substring because the branches are compiler-checked - a reciter added to the
 * enum without a tone fails to compile, and a renamer cannot quietly lose one.
 * The old matcher was `name.contains(...)`, and a third of its branches were
 * for a reciter this app never offered.
 */
internal fun previewToneHz(reciter: Reciter): Double = when (reciter) {
    Reciter.ALAFASY -> 293.66 // D4
    Reciter.BASIT -> 220.00 // A3
    Reciter.HUSARY -> 246.94 // B3
    Reciter.SHURAIM -> 349.23 // F4
}

/** What a system audio-focus change means for the stream that holds sound. */
internal enum class FocusAction { PAUSE_UNTIL_REGAIN, RESUME_HELD, STOP_ALL, NOTHING }

/**
 * The meaning of a system audio-focus change, from the state the player keeps.
 *
 * Recitation is the kind of sound that claims the phone's audio, so the player
 * claims focus and answers the system when something else needs it. The four
 * answers:
 *
 * - `GAIN` with [pausedForFocusLoss] resumes *our own* loss-pause. Without the
 *   flag a gain is noise arriving late - without it the flag, the player would
 *   "resume" a stream the user paused themselves, which is how a patient phone
 *   call ends and the surah plays over the confusion.
 * - A transient loss pauses and remembers. A duckable loss is answered the same
 *   way rather than by lowering the volume: a voice reciting quietly is not a
 *   thing you can hear at 20% anyway; for a *voice*, ducking is a pause played
 *   badly.
 * - A permanent loss stops everything. The system asserted another app owns the
 *   audio until further notice.
 * - Anything unknown, and gains while nothing was lost to the system: nothing.
 *   An unrecognised change must not be an opportunity to move a stream.
 *
 * Pure so the branch table is testable without an `AudioManager` to argue with.
 */
internal fun focusActionFor(
    change: Int,
    pausedForFocusLoss: Boolean
): FocusAction = when (change) {
    AudioManager.AUDIOFOCUS_GAIN ->
        if (pausedForFocusLoss) FocusAction.RESUME_HELD else FocusAction.NOTHING
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusAction.PAUSE_UNTIL_REGAIN
    AudioManager.AUDIOFOCUS_LOSS -> FocusAction.STOP_ALL
    else -> FocusAction.NOTHING
}

class QuranAudioPlayer(private val context: Context) {

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var synthJob: Job? = null
    private var prepareTimeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val appContext: Context get() = context.applicationContext

    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * True while the player holds audio focus - the system's word that this sound
     * matched the phone's.
     *
     * Recitation is claimed with [AudioManager.AUDIOFOCUS_GAIN], not the transient
     * kinds, because a verse is not a notification-length sound: while it plays,
     * the phone's audio is the reader's.
     *
     * A refusal does not stop it. The refusals that arrive in practice are delays
     * - a call dialing, another app negotiating - and pausing a recitation the
     * reader *tapped* because a permission clerk said no would be the reader
     * serving the system instead of the other way round. The state records only
     * whether a claim is outstanding, which is what [abandonFocus] must know.
     */
    private var holdsFocus = false

    /**
     * True after a transient focus loss paused the stream, consumed by the
     * following gain.
     *
     * This side of the value is what keeps the focus listener from "resuming" a
     * stream the user paused on purpose: the flag records that *the system* asked
     * for the pause, and only its own contradiction - the gain when the other
     * sound left - may answer it with a start.
     */
    private var pausedForFocusLoss = false

    /**
     * The attributes focus is claimed with. Habit for media volume; nothing
     * stream-specific - the players carry their own playback attributes already.
     */
    private val focusAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /**
     * Answers the system on the shared listener. The *decision* lives in
     * [focusActionFor], pure and tested; this is only the wiring, with one
     * small bookkeeping duty: [pause] can pause nothing (a stream still
     * preparing has not made a sound), so after the call the flag is reread
     * from the state rather than trusted. A loss that paused nothing leaves
     * no value a later gain would resume.
     */
    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            when (focusActionFor(change, pausedForFocusLoss)) {
                FocusAction.PAUSE_UNTIL_REGAIN -> {
                    pause()
                    pausedForFocusLoss = _playbackState.value.isPaused
                }
                FocusAction.RESUME_HELD -> resume()

                FocusAction.STOP_ALL -> stop()

                FocusAction.NOTHING -> Unit
            }
        }

    /**
     * The focus claim itself, on API 26+, where the deprecated call became a
     * request and its listener lives inside it. `AudioFocusRequest` cannot be
     * *built* below 26 - not merely deprecated - so the field is null there and
     * [abandonFocus]/[requestFocus] fall back to `AudioManager`'s direct calls
     * with the same listener. Both paths keep [focusListener] as the one
     * listener; there is never a second answerer to drift apart.
     */
    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(focusAttributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
        } else {
            null
        }

    /**
     * Claim focus for the sound that is about to be made. Called before a
     * player prepares, so the claim is outstanding before the first sample,
     * and released again at every point where the sound stops existing:
     * [stop], [playbackFailed], and the last verse of a chain (there, by
     * the completion handler, since nothing remains to hold it).
     */
    private fun requestFocus() {
        holdsFocus =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = checkNotNull(focusRequest) {
                    "AudioFocusRequest is built on every release from O, and the " +
                        "version branch above proves the device is one of them"
                }
                audioManager.requestAudioFocus(request) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
    }

    /**
     * Give the claim back when the sound that justified it is gone. No-op without
     * an outstanding claim, because [stop] is called at the start of every new
     * play as well as by the reader, and releasing focus nobody holds is not
     * an error the platform minds - but it is also not an event that happened.
     */
    private fun abandonFocus() {
        if (!holdsFocus) return
        holdsFocus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(
                checkNotNull(focusRequest) {
                    "AudioFocusRequest is built on every release from O, and the " +
                        "version branch above proves the device is one of them"
                }
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    /**
     * One verse in flight, holding its player and the identity its callbacks
     * check against: every event a [MediaPlayer] posts must be answered by the
     * verse it belongs to *at the moment it arrives*, because the player behind
     * the field it is read from may have been replaced, handed off, or dropped
     * since.
     *
     * [prepared] is set by the platform's prepared callback and read by the
     * watchdog after it - the same data, written where the event lands.
     * [attached] is true once this player has been armed as another one's next
     * player, which is a one-way trip: the platform will start it at the other
     * player's end-of-stream, and the player must never be started by hand
     * afterwards.
     */
    private class VerseStream(
        val surahNumber: Int,
        val ayahNumber: Int,
        val player: MediaPlayer,
        var prepared: Boolean = false,
        var attached: Boolean = false,
    )

    /** The verse that is sounding, or about to. */
    private var active: VerseStream? = null

    /**
     * The verse prepared for the seam while [active] sounds.
     *
     * Null most of the lifetimes that matter: no chain, chain finished, prefetch
     * failed, or - after a hand-off - the queued verse *becoming* the active one.
     */
    private var queued: VerseStream? = null

    /**
     * Who answers "what plays after this verse", or null when the caller asked
     * for one verse and no chain.
     *
     * The player does not hold the corpus, so it cannot know that 255 is not
     * the last verse of Baqarah; the caller - the view model, via `QuranBrowse`
     * - answers, and this field is where the answer lives for the whole chain
     * the [playAyah] started. One play, one finder.
     */
    private var nextVerseFinder: ((Int, Int) -> Pair<Int, Int>?)? = null

    /** The reciter the chain was started with, for every verse it prepares. */
    private var chainedReciter: Reciter? = null

    private var prefetchWatchdogJob: Job? = null

    /**
     * The speed every stream is recited at.
     *
     * Written by [setSpeed], read at every prepare - so a chain keeps its speed
     * across the seams (each player is told before it starts) - and applied to a
     * sounding player at once, so the change is heard, not queued.
     */
    private var playbackSpeed = 1f

    /**
     * More plays the verse now sounding owes after this one ends, as the
     * [VerseRepeat] choice resolves it: 0 means the chain moves on, a positive
     * counts down through the choice's replays, and a negative is the loop,
     * which no completion can spend.
     *
     * Set at [playAyah] from that call's choice, and on [setRepeat] when the
     * reader changes the choice mid-verse - and a change mid-verse counts from
     * now, because the pill on a sounding recitation can only mean *this*
     * recitation.
     */
    private var playsRemaining = 0

    /**
     * Play one verse, and - when given a next-verse finder - the rest of its
     * surah after it, without a download stall at the seam.
     *
     * The chain used to live in the caller's completion callback: every verse
     * completion tore the player down (stop, release) and built a fresh one for
     * the next verse, so each seam carried the next verse's whole prepare delay
     * - a connection stall between verses the reader hears as dead air, or, on
     * a slow connection, as a silence long enough to be mistaken for the end.
     *
     * Now the *current* verse's successor is prepared in a second player while
     * the first still sounds, and armed onto it with
     * `MediaPlayer.setNextMediaPlayer`: the platform then starts the successor
     * itself at end-of-stream, as seamlessly as it can. Three honest fallbacks
     * keep the chain alive when the seam is lost:
     *
     * - a prefetch that cannot even be set up (offline, refused) is dropped and
     *   leaves the current verse sounding - a bad future must not stop a good
     *   present;
     * - a prefetch that loses the race (current completes before it is
     *   prepared) falls back to the old sequential start - the old cost, at
     *   the rare seam the network made it reach;
     * - a stalled prefetch fails like any other stream and the fallback
     *   re-run - which is the same download, from the same dead connection -
     *   fails into [playbackFailed] like the current verse would, and the
     *   reader is told.
     *
     * @param nextVerse the seam: given (surah, ayah) now sounding, the verse
     *   that follows, or null when this is the last.
     */
    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        reciter: Reciter,
        nextVerse: ((Int, Int) -> Pair<Int, Int>?)? = null,
        repeat: VerseRepeat = VerseRepeat.ONCE,
    ) {
        stop()
        pausedForFocusLoss = false
        requestFocus()
        chainedReciter = reciter
        nextVerseFinder = nextVerse
        playsRemaining = repeat.playsRemaining

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false,
            failed = false,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            reciter = reciter,
        )
        startStream(surahNumber, ayahNumber, reciter, asQueued = false)
    }

    /**
     * Prepare a verse's stream. `asQueued` decides the role, and the role
     * decides what failure and completion *mean*:
     *
     * - A queued stream that fails is dropped, non-fatally - the current verse
     *   keeps sounding, and the completion path falls back to a sequential
     *   start.
     * - The active stream that fails fails the whole chain: there is nothing
     *   sounding to fall back from.
     */
    private fun startStream(
        surahNumber: Int,
        ayahNumber: Int,
        reciter: Reciter,
        asQueued: Boolean,
    ) {
        val stream = VerseStream(surahNumber, ayahNumber, MediaPlayer())
        try {
            with(stream.player) {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // The directory is the reciter's own, from [Reciter]: the same
                // enum the Settings picker offers, so the selection and the stream
                // are the one fact. The banner can only name a reciter whose
                // stream is real, because it names the [Reciter] recorded here
                // and not the preference stored beside it.
                setDataSource(appContext, Uri.parse(reciter.verseUrl(surahNumber, ayahNumber)))
                setOnPreparedListener { mp ->
                    stream.prepared = true
                    cancelWatchdog(asQueued)
                    // The reader may have released this attempt while it prepared -
                    // by tapping pause, which cannot pause a stream that has not
                    // made a sound - and a callback already on its way must not
                    // start a player that is gone.
                    if (!asQueued && active?.player === mp) {
                        // The speed is set before the start: a player whose params
                        // are already right starts at the right speed, and the
                        // platforms' quirks with params set mid-flight never fire.
                        applySpeed(mp)
                        mp.start()
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = true,
                            isPaused = false,
                        )
                    }
                    attachQueued()
                    if (!asQueued && active?.player === mp) {
                        schedulePrefetchAfter(stream)
                    }
                }
                setOnCompletionListener { mp ->
                    // A hand-off leaves the finished player superseded; only the
                    // player whose verse the state names may answer completion.
                    if (active !== stream) return@setOnCompletionListener
                    verseCompleted(stream)
                }
                setOnErrorListener { mp, _, _ ->
                    if (active?.player === mp) {
                        playbackFailed()
                    } else if (queued?.player === mp) {
                        dropQueued("the prefetch was refused")
                    }
                    true
                }
                // The hand-off signal arrives on the *queued* player when the
                // platform starts it to bridge the seam. Promotion from here is
                // redundancy in disguise: the completed player's own completion
                // callback is expected to promote first, and this is the idempotent
                // second chance for the platforms whose event order differs.
                setOnInfoListener { mp, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT && queued?.player === mp) {
                        promoteQueued()
                    }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            if (asQueued) {
                dropQueued("the prefetch could not even be set up: offline or refused")
            } else {
                playbackFailed()
            }
            return
        }
        if (asQueued) {
            queued = stream
        } else {
            active = stream
            // A new active stream is a new verse on the banner. For playAyah the
            // state was already moved; this is for the sequential fallback, whose
            // verses are started from inside the chain and would otherwise leave
            // the recitation banner naming the verse that just ended.
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                reciter = reciter,
            )
        }
        armWatchdog(stream, asQueued)
    }

    /**
     * Arm the hand-off: tell the platform that [queued] starts when [active]
     * reaches its end.
     *
     * Guarded on the *current player actually sounding*, because an armed
     * hand-off is the platform's, not ours: attaching a next player onto a
     * paused or still-preparing stream would promise a seamless start on a
     * player whose continuation we ourselves control, and the seam would then
     * depend on the pause resolving rather than on the recitation finishing.
     * So the attach waits for the resumption (see [resume]), and stays lawful
     * under the platform rule that a next player must be prepared - but never
     * started by hand - before the player it is attached to completes.
     */
    private fun attachQueued() {
        val active = active ?: return
        val queued = queued ?: return
        if (queued.attached) return
        // A verse that owes plays keeps its own end for itself: an armed
        // hand-off is the platform's - it cannot be un-fired - so arming one
        // while replays remain would turn the next replay into the next verse
        // and spend the reader's count before it was recited.
        if (playsRemaining != 0) return
        if (!active.player.isPlaying) return
        try {
            // Deprecated since 34, and unreplaceable below 34: the modern
            // setNextMediaItem lives behind a media-session surface this app
            // does not have, and minSdk 24 forbids requiring it. Chaining plain
            // players is exactly what this method is for.
            @Suppress("DEPRECATION")
            active.player.setNextMediaPlayer(queued.player)
            queued.attached = true
        } catch (_: Exception) {
            // An unarmed seam is a lost race, answered at completion by the
            // sequential fallback - never by stopping the sounding verse.
        }
    }

    /**
     * The queued verse's turn: the platform started it (or, in the redundancy
     * path, it is started here), the finished player is released, and the
     * state moves to the verse the reader is now hearing.
     */
    private fun promoteQueued() {
        val queued = queued ?: return
        val finished = active
        active = queued
        this.queued = null
        releaseQuietly(finished?.player)
        if (!queued.player.isPlaying) {
            // Some platforms post the completion and the start-as-next out of
            // order; a prepared stream that was never 'released into' by the
            // hand-off has to be started by hand or it will sit silent forever,
            // with no completion to ever fire.
            try {
                queued.player.start()
            } catch (_: Exception) {
            }
        }
        prefetchWatchdogJob?.cancel()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false,
            failed = false,
            surahNumber = queued.surahNumber,
            ayahNumber = queued.ayahNumber,
        )
        schedulePrefetchAfter(queued)
    }

    /**
     * The verse that was sounding finished. The armed hand-off is answered by
     * starting its beneficiary; anything else keeps the chain alive the way it
     * has always kept it alive - a sequential start for the next verse, or
     * silence at the surah's end.
     *
     * Either way the finished player is done: a hand-off releases it (the
     * framework has moved on), a fallback releases it before preparing the
     * next, and the end of the chain releases everything.
     */
    private fun verseCompleted(stream: VerseStream) {
        // A verse that owes plays pays them first: back to its start, in place,
        // and the chain moves on only when the count is spent. The queued
        // successor is untouched - it stays prepared through the replays, and
        // the hand-off onto it is armed only once, at the transition that
        // made this play the verse's last (see below), because an armed
        // hand-off cannot be un-fired and would recite the next verse under
        // the count of this one.
        if (playsRemaining != 0) {
            if (playsRemaining > 0) playsRemaining -= 1
            restartStream(stream)
            if (playsRemaining == 0) {
                // The replay just started is the verse's last; if the successor
                // is already prepared, this is its one chance to be armed -
                // the prepared callback and the resume have both passed.
                attachQueued()
            }
            return
        }
        val queued = queued
        if (queued != null && queued.attached) {
            promoteQueued()
            return
        }
        // The prefetch never made it: drop it in flight and prepare the next
        // verse the old way.
        dropQueued("the prefetch lost the race")
        releaseQuietly(stream.player)
        this.active = null
        val next = findNextAfter(stream)
        if (next != null) {
            startStream(next.first, next.second, chainedReciter ?: return, asQueued = false)
        } else {
            endChain()
        }
    }

    /**
     * Ask the injected finder what follows, tolerant of being called at the
     * moment a play was never given a finder at all.
     */
    private fun findNextAfter(stream: VerseStream): Pair<Int, Int>? {
        val finder = nextVerseFinder ?: return null
        return finder(stream.surahNumber, stream.ayahNumber)
    }

    /**
     * Play the verse again, from its start, on the player that just finished.
     *
     * This is how a repeat sounds: no second player, no prepare, no re-download
     * - the completed player is rewound and start()ed, the platform's own
     * replay path, and the state stays on the verse it already named. A player
     * that refuses the rewind (released underneath the completion, say) has no
     * sound left to repeat, and fails the chain honestly rather than playing
     * nothing while the banner claims a recitation.
     */
    private fun restartStream(stream: VerseStream) {
        try {
            stream.player.seekTo(0)
            stream.player.start()
        } catch (_: Exception) {
            playbackFailed()
            return
        }
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false,
        )
    }

    /** Prepare the verse after the one now sounding, into the queued slot. */
    private fun schedulePrefetchAfter(stream: VerseStream) {
        val reciter = chainedReciter ?: return
        val next = findNextAfter(stream) ?: return
        startStream(next.first, next.second, reciter, asQueued = true)
    }

    /**
     * Change how fast the recitation plays.
     *
     * The stored value reaches the next prepare; a sounding player is told now,
     * because the pill's promise - the next verse *and this one* at the new
     * speed - would be a half-lie if the speed landed only after the seam.
     * Valid only on prepared-or-later states, so an attempt on a still-preparing
     * player is skipped: the prepared listener applies the stored speed to every
     * player the chain makes, and one apply per player is the whole contract.
     */
    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        applySpeed(active?.player)
    }

    /**
     * Tell one player the speed. Skipped quietly on the states the platform
     * refuses the call in - a player still preparing is told at its prepared
     * callback, and the skip is what keeps a rejected call from taking down a
     * recitation it was meant to describe.
     */
    private fun applySpeed(player: MediaPlayer?) {
        player ?: return
        try {
            val params = player.playbackParams
            params.setSpeed(playbackSpeed)
            player.playbackParams = params
        } catch (_: Exception) {
        }
    }

    /**
     * Change how many times the verse recites before the chain moves on.
     *
     * A change counts *from now*, because the pill on a sounding recitation
     * means this recitation: back to `once` spends nothing further (the verse
     * finishes at its current play and the chain moves on), and a loop now
     * loops this verse. One change is outside even this: if the seam was
     * already armed - the verse's last play in flight, its hand-off given to
     * the platform - the platform holds the promise and no setter can take it
     * back, so the very next verse is recited anyway and the new choice binds
     * from there.
     */
    fun setRepeat(repeat: VerseRepeat) {
        playsRemaining = repeat.playsRemaining
    }

    /**
     * Drop the queued verse without touching the sounding one.
     *
     * Whatever went wrong - refused, stalled, lost the race - the fault is the
     * *future's*: the verse now sounding is fine, and the seam will be answered
     * when it completes by the fallback path this drop leaves standing.
     */
    private fun dropQueued(why: String) {
        releaseQuietly(queued?.player)
        prefetchWatchdogJob?.cancel()
        queued = null
    }

    /**
     * The chain is over, and so is its equipment: the last verse released, the
     * queued slot empty, the watchdogs cold. The state is a finished verse -
     * sounding nothing, holding no pause, so the next tap on the verse starts
     * it over rather than 'resuming' a player that ended.
     */
    private fun endChain() {
        releaseQuietly(active?.player)
        active = null
        dropQueued("end of chain")
        prepareTimeoutJob?.cancel()
        prefetchWatchdogJob?.cancel()
        abandonFocus()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
        )
    }

    /**
     * Watchdogs. Two roles, one timer each: the active stream that never
     * prepares fails the chain; a queued stream that never prepares only loses
     * the seam - it is answered at completion by the sequential fallback.
     */
    private fun armWatchdog(stream: VerseStream, asQueued: Boolean) {
        val job = scope.launch {
            delay(12_000)
            if (stream.prepared) return@launch
            if (asQueued && queued === stream) {
                dropQueued("the prefetch stalled")
            } else if (!asQueued && active === stream) {
                playbackFailed()
            }
        }
        if (asQueued) {
            prefetchWatchdogJob?.cancel()
            prefetchWatchdogJob = job
        } else {
            prepareTimeoutJob?.cancel()
            prepareTimeoutJob = job
        }
    }

    private fun cancelWatchdog(asQueued: Boolean) {
        if (asQueued) prefetchWatchdogJob?.cancel() else prepareTimeoutJob?.cancel()
    }

    private fun releaseQuietly(player: MediaPlayer?) {
        try {
            player?.release()
        } catch (_: Exception) {
        }
    }

    /**
     * A stream that could not be prepared: offline, refused, or a connection that
     * never answered.
     *
     * The player used to answer this with a synthesized chime and then reap the
     * verse as recited - `onAyahCompleted` ran and the next verse chained in, so
     * an offline reader heard a tone march down the surah while the banner named
     * a reciter who was never heard. There is nothing to play, so nothing plays:
     * the state carries the failure for the reader to see, and the surah stays
     * where it is.
     */
    private fun playbackFailed() {
        prepareTimeoutJob?.cancel()
        prefetchWatchdogJob?.cancel()
        try {
            active?.player?.reset()
            active?.player?.release()
        } catch (_: Exception) {
        }
        releaseQuietly(queued?.player)
        active = null
        queued = null
        pausedForFocusLoss = false
        abandonFocus()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            failed = true,
        )
    }

    /**
     * Hold the sound, and only sound that can be held.
     *
     * A stream that has started is paused in place, and [AudioPlaybackState.isPaused]
     * says so, so [resume] can continue it where it stopped. Anything else is
     * *released*: a preview tone has no head to continue from, and a stream still
     * preparing has not made a sound yet - calling `pause()` on one throws, which
     * is what the old two-way tap asked of a reader on a slow connection. Nothing
     * is held, so the next tap on the verse starts it over rather than resuming a
     * stream that is gone.
     *
     * A focus claim is *not* released here. The pause exists so the next play can
     * continue this stream; the claim stands for the whole held stretch, and
     * re-claiming on resume would be a new favour asked for an old sound.
     */
    fun pause() {
        val player = active?.player
        val held = player != null && try {
            player.pause()
            true
        } catch (_: IllegalStateException) {
            false
        }
        if (held) {
            _playbackState.value = _playbackState.value.copy(isPlaying = false, isPaused = true)
        } else {
            stop()
        }
    }

    /**
     * Continue the stream [pause] held, exactly where it stopped.
     *
     * Every path that releases the player clears [AudioPlaybackState.isPaused] on
     * the way, so the flag being set is the guarantee that there is a stream to
     * start. Runs only when that guarantee holds, too: a resume with no stream
     * to start used to write `isPlaying = true` over a state whose player did
     * not exist, which is not a resume but a lie about one.
     *
     * Either the reader or the system's own ended loss can be the asker, so
     * *a* resume consumes the loose-ends flag either way.
     */
    fun resume() {
        pausedForFocusLoss = false
        val player = active?.player ?: return
        player.start()
        // A resumption is the moment a paused seam can be sealed: the queued
        // prefetch, if it prepared while the stream sat paused, was refused its
        // attach by the not-sounding guard, and this is its second chance
        // before the end-of-stream that it must be armed for.
        attachQueued()
        _playbackState.value = _playbackState.value.copy(isPlaying = true, isPaused = false)
    }

    fun playAudioPreview(name: String, onDone: (() -> Unit)? = null) {
        stop()
        pausedForFocusLoss = false
        requestFocus()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            // A preview is not a recitation, whatever reciter's tone it plays:
            // the recitation banner reads who is streaming, and a Settings
            // preview must not be able to borrow a reciter's name for an adhan.
            reciter = null,
        )
        synthJob = scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSec = 3.0
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                // **Reciters, not adhan sounds.** The caller of this branch always
                // passes a reciter's name - the adhan sounds and alert modes are
                // dispatched to the synthesizer before anything reaches here - so
                // the mapping is keyed on the [Reciter] the name resolves to, in
                // [previewToneHz], where a missing branch is a compile error.
                val baseFreq = previewToneHz(Reciter.from(name))
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = kotlin.math.exp(-0.8 * t) * (1.0 - kotlin.math.exp(-20.0 * t))
                    val wave = (sin(2 * Math.PI * baseFreq * t) * 0.55 +
                            sin(2 * Math.PI * (baseFreq * 1.5) * t) * 0.3 +
                            sin(2 * Math.PI * (baseFreq * 2.0) * t) * 0.15) * envelope
                    samples[i] = (wave * 26000).toInt().coerceIn(-32767, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()

                val startTime = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - startTime < 3000) {
                    delay(100)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {}

            scope.launch(Dispatchers.Main) {
                // The tone is over; its focus claim goes with it, the same
                // way a finished verse releases its own.
                abandonFocus()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                onDone?.invoke()
            }
        }
    }

    fun stop() {
        synthJob?.cancel()
        prepareTimeoutJob?.cancel()
        releaseQuietly(queued?.player)
        queued = null
        try {
            active?.player?.stop()
            active?.player?.release()
        } catch (_: Exception) {}
        active = null
        pausedForFocusLoss = false
        abandonFocus()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            failed = false,
        )
    }
}
