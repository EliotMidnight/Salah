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
import kotlin.math.sin

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 1,
    val reciterName: String = "Mishary Rashid Alafasy",
    val isOfflineMode: Boolean = true
)

class QuranAudioPlayer(private val context: Context) {

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun playAyah(surahNumber: Int, ayahNumber: Int, onAyahCompleted: (() -> Unit)? = null) {
        stop()

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            currentPositionMs = 0,
            durationMs = 8000
        )

        // Try online audio stream (EveryAyah public domain audio)
        val paddedSurah = String.format("%03d", surahNumber)
        val paddedAyah = String.format("%03d", ayahNumber)
        val url = "https://everyayah.com/data/Alafasy_128kbps/${paddedSurah}${paddedAyah}.mp3"

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(url))
                setOnPreparedListener { mp ->
                    mp.start()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        durationMs = mp.duration.coerceAtLeast(1000),
                        isOfflineMode = false
                    )
                    startProgressTracker()
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0)
                    onAyahCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    // Fallback to offline synthetic tone chimes
                    playOfflineChime(surahNumber, ayahNumber, onAyahCompleted)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            playOfflineChime(surahNumber, ayahNumber, onAyahCompleted)
        }
    }

    private fun playOfflineChime(surahNumber: Int, ayahNumber: Int, onAyahCompleted: (() -> Unit)?) {
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isOfflineMode = true,
            durationMs = 4000
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
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = elapsed)
                    delay(100)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
            }

            scope.launch(Dispatchers.Main) {
                _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0)
                onAyahCompleted?.invoke()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = current,
                    durationMs = total.coerceAtLeast(1)
                )
                delay(300)
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        synthJob?.cancel()
        progressJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            startProgressTracker()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
        }
    }

    fun playAudioPreview(name: String, onDone: (() -> Unit)? = null) {
        stop()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            reciterName = name,
            durationMs = 3000,
            isOfflineMode = true
        )
        synthJob = scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSec = 3.0
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                val baseFreq = when {
                    name.contains("Makkah") -> 329.63 // E4
                    name.contains("Madinah") -> 293.66 // D4
                    name.contains("Al-Aqsa") -> 261.63 // C4
                    name.contains("Moroccan") -> 392.00 // G4
                    name.contains("Basit") -> 220.00 // A3
                    name.contains("Husary") -> 246.94 // B3
                    name.contains("Ghamdi") -> 349.23 // F4
                    else -> 293.66
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
        progressJob?.cancel()
        synthJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0)
    }
}
