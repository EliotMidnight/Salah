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

    private var mediaPlayer: MediaPlayer? = null
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

    fun playAyah(surahNumber: Int, ayahNumber: Int, reciter: Reciter, onAyahCompleted: (() -> Unit)? = null) {
        stop()
        pausedForFocusLoss = false
        requestFocus()

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false,
            failed = false,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            reciter = reciter,
        )

        // Try online audio stream (EveryAyah public domain audio).
        //
        // The directory is the reciter's own, from [Reciter]: the same enum the
        // Settings picker offers, so the selection and the stream are the one
        // fact. The banner can only name a reciter whose stream is real, because
        // it names the [Reciter] recorded here and not the preference stored
        // beside it.
        val url = reciter.verseUrl(surahNumber, ayahNumber)

        try {
            var prepared = false
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(appContext, Uri.parse(url))
                setOnPreparedListener { mp ->
                    prepared = true
                    prepareTimeoutJob?.cancel()
                    // The reader may have released this attempt while it prepared -
                    // by tapping pause, which cannot pause a stream that has not
                    // made a sound - and a callback already on its way must not
                    // start a player that is gone.
                    if (mediaPlayer === mp) {
                        mp.start()
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = true,
                            isPaused = false,
                        )
                    }
                }
                setOnCompletionListener {
                    prepareTimeoutJob?.cancel()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isPaused = false,
                    )
                    // The verse is finished, so the claim has no sound behind it
                    // until the next one prepares - the chain's next verse re-claims
                    // through playAyah, and a chain that ends here leaves nothing
                    // outstanding.
                    abandonFocus()
                    onAyahCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    playbackFailed()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
            // Watchdog: a stream that never arrives - a hanging connection rather
            // than an error the platform reported - fails like any other. The
            // identity guard keeps a superseded attempt from taking down the
            // stream that replaced it.
            prepareTimeoutJob?.cancel()
            prepareTimeoutJob = scope.launch {
                delay(12_000)
                if (!prepared && mediaPlayer === player) {
                    playbackFailed()
                }
            }
        } catch (_: Exception) {
            playbackFailed()
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
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
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
     */
    fun pause() {
        val player = mediaPlayer
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
        val player = mediaPlayer ?: return
        player.start()
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
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        pausedForFocusLoss = false
        abandonFocus()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            failed = false,
        )
    }
}
