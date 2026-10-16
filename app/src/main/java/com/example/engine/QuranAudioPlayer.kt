package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
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

class QuranAudioPlayer(private val context: Context) {

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var prepareTimeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val appContext: Context get() = context.applicationContext

    fun playAyah(surahNumber: Int, ayahNumber: Int, reciter: Reciter, onAyahCompleted: (() -> Unit)? = null) {
        stop()

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
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
                    mp.start()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                    )
                }
                setOnCompletionListener {
                    prepareTimeoutJob?.cancel()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    onAyahCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    // Fallback to offline synthetic tone chimes
                    prepareTimeoutJob?.cancel()
                    playOfflineChime(surahNumber, ayahNumber, onAyahCompleted)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
            // Watchdog: if the stream cannot be prepared (offline / hanging
            // network), fall back to the offline chime instead of spinning.
            prepareTimeoutJob?.cancel()
            prepareTimeoutJob = scope.launch {
                delay(12_000)
                if (!prepared) {
                    try {
                        player.reset()
                        player.release()
                    } catch (_: Exception) {}
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                        playOfflineChime(surahNumber, ayahNumber, onAyahCompleted)
                    }
                }
            }
        } catch (e: Exception) {
            prepareTimeoutJob?.cancel()
            playOfflineChime(surahNumber, ayahNumber, onAyahCompleted)
        }
    }

    private fun playOfflineChime(surahNumber: Int, ayahNumber: Int, onAyahCompleted: (() -> Unit)?) {
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
        )

        synthJob = scope.launch(Dispatchers.Default) {
            try {
                // Generate soft, meditative contemplative acoustic harmonic wave
                val sampleRate = 44100
                val durationSec = 3.5
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)

                val baseFreq = 220.0 + (ayahNumber % 12) * 18.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Gentle envelope attack and slow decay
                    val envelope = kotlin.math.exp(-1.2 * t) * (1.0 - kotlin.math.exp(-20.0 * t))
                    val wave = (sin(2 * Math.PI * baseFreq * t) * 0.6 +
                            sin(2 * Math.PI * (baseFreq * 1.5) * t) * 0.3 +
                            sin(2 * Math.PI * (baseFreq * 2.0) * t) * 0.1) * envelope
                    samples[i] = (wave * 28000).toInt().coerceIn(-32767, 32767).toShort()
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
                while (isActive && System.currentTimeMillis() - startTime < 3500) {
                    val elapsed = (System.currentTimeMillis() - startTime).toInt()
                    delay(100)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
            }

            scope.launch(Dispatchers.Main) {
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                onAyahCompleted?.invoke()
            }
        }
    }


    fun pause() {
        mediaPlayer?.pause()
        synthJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
        }
    }

    fun playAudioPreview(name: String, onDone: (() -> Unit)? = null) {
        stop()
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
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }
}
