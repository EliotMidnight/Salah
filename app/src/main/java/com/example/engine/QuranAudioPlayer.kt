package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
)

class QuranAudioPlayer(private val context: Context) {

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var prepareTimeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val appContext: Context get() = context.applicationContext

    fun playAyah(surahNumber: Int, ayahNumber: Int, onAyahCompleted: (() -> Unit)? = null) {
        stop()

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
        )

        // Try online audio stream (EveryAyah public domain audio)
        //
        // `Locale.ROOT` deliberately, and the reason is not tidiness: these digits go
        // into a URL, and in a locale whose digits are not ASCII - Arabic, Urdu,
        // Bengali, all of which this app ships in - `String.format("%03d", 2)` produces
        // "٠٠٢". everyayah.com names files in ASCII, so the request 404s and the reader
        // hears nothing, in exactly the languages where listening matters most. Lint
        // flags the implicit default here and was right to.
        val paddedSurah = String.format(Locale.ROOT, "%03d", surahNumber)
        val paddedAyah = String.format(Locale.ROOT, "%03d", ayahNumber)
        val url = "https://everyayah.com/data/Alafasy_128kbps/${paddedSurah}${paddedAyah}.mp3"

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
        )
        synthJob = scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSec = 3.0
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                // **Reciters, not adhan sounds.**
                //
                // This list began with Makkah, Madinah, Al-Aqsa and Moroccan - the
                // *adhan* vocabulary, copied from the synthesizer - and then carried
                // three reciter names after them. None of the first four can appear
                // here: the four callers of this function pass a reciter, a sound, an
                // alert mode, or a sound label, and a reciter is not a place. So four
                // of the seven branches could never fire, and the two vocabularies sat
                // in one function where either looked plausible.
                //
                // A reciter's preview is a placeholder tone, and its own registry is
                // [com.example.data.model.Reciter]; the mapping is keyed on the
                // reciter's key so a rename cannot silently fall through.
                val baseFreq = when {
                    name.contains("Basit") -> 220.00 // A3
                    name.contains("Husary") -> 246.94 // B3
                    name.contains("Ghamdi") -> 349.23 // F4
                    else -> 293.66 // D4
                }
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
