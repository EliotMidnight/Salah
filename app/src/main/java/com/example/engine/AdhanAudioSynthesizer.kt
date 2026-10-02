package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.AdhanSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

/**
 * On-device real-time acoustic synthesizer for Adhan prayer calls, Takbeer, and gentle alert chimes.
 * Operates 100% offline with zero external network downloads, zero latency, and pure PCM wave generation.
 */
object AdhanAudioSynthesizer {

    private var synthJob: Job? = null
    private var activeTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Whether a phrase is sounding, for this class's own loops.
     *
     * Private now. It was public and nothing outside read it - a state a reader of the
     * class could have consulted and been told the wrong thing by, since it is only
     * about synthesis and says nothing about whether a *stream* is playing.
     */
    @Volatile
    private var isPlaying: Boolean = false

    /**
     * Plays the specified prayer alert mode (Full Adhan, Takbeer Only, Gentle Chime).
     */
    fun playAlert(
        alertMode: String,
        /**
         * Which adhan to play, as a resolved [AdhanSound] and not a string.
         *
         * A string, and the caller guessing at it, was how four places came to hold
         * four different vocabularies: one offered five sounds and matched three of
         * them, one matched four and offered two the picker never showed, one matched
         * reciter *names* because it was written for the Quran player and copied, and
         * one asked whether the sound "was an adhan" by listing Cairo. The result a
         * reader could hear: previewing "Gentle Bell Chime" played a chime, and the
         * alarm for the same setting played a full adhan.
         */
        sound: AdhanSound = AdhanSound.MAKKAH,
        volume: Float = 0.85f,
        onCompletion: (() -> Unit)? = null
    ) {
        stop()

        if (alertMode.equals("Silent", ignoreCase = true) || alertMode.equals("Vibrate Only", ignoreCase = true)) {
            onCompletion?.invoke()
            return
        }

        isPlaying = true

        synthJob = scope.launch {
            try {
                when {
                    alertMode.equals("Takbeer Only", ignoreCase = true) -> {
                        playTakbeerAcoustic(sound, volume)
                    }
                    alertMode.equals("Gentle Chime", ignoreCase = true) -> {
                        playGentleChimeAcoustic(volume)
                    }
                    // The reader chose a sound that is not an adhan, so a chime is
                    // what they get. It used to fall through to the full adhan here,
                    // because the sound's *name* matched no branch.
                    !sound.isAdhan -> playGentleChimeAcoustic(volume)
                    else -> playFullAdhanAcoustic(sound, volume)
                }
            } catch (_: Exception) {
            } finally {
                stopTrack()
                isPlaying = false
                onCompletion?.invoke()
            }
        }
    }

    /**
     * Full Adhan melodic synthesis following traditional Maqam intervals:
     * - Opening Takbeerat (Allahu Akbar, Allahu Akbar)
     * - Ashhadu an la ilaha illallah
     * - Hayya 'ala as-Salah
     * - Closing Takbeer & Tahlil
     */
    private suspend fun playFullAdhanAcoustic(sound: AdhanSound, volume: Float) {
        val sampleRate = 44100
        // One line, because the pitch belongs to the sound and not to this function.
        // It was a five-way `contains` match here and a two-way one in
        // [playTakbeerAcoustic], so Makkah's adhan began on E and its takbeer on
        // something else, and only Moroccan ever agreed with itself.
        val baseFreq = sound.baseFrequency()

        // Adhan Melodic Notes (Frequency multipliers from base root)
        // Authentic Maqam Bayati/Rast scale progression
        val melodyPhases = listOf(
            // Phase 1: Opening Takbeer (Allahu Akbar)
            MelodicPhrase(listOf(1.0 to 0.7, 1.25 to 0.9, 1.33 to 1.1, 1.0 to 1.2), pauseAfter = 400),
            // Phase 2: Second Takbeer (Allahu Akbar)
            MelodicPhrase(listOf(1.0 to 0.7, 1.25 to 0.9, 1.50 to 1.2, 1.33 to 0.8, 1.0 to 1.4), pauseAfter = 500),
            // Phase 3: Shahadah (Ashhadu an la ilaha illallah)
            MelodicPhrase(listOf(1.0 to 0.8, 1.12 to 0.7, 1.25 to 1.0, 1.33 to 1.2, 1.2 to 0.9, 1.0 to 1.5), pauseAfter = 600),
            // Phase 4: Hayya 'ala as-Salah
            MelodicPhrase(listOf(1.25 to 0.8, 1.50 to 1.1, 1.66 to 1.3, 1.50 to 1.0, 1.33 to 1.4), pauseAfter = 600),
            // Phase 5: Final Takbeer & Tahlil (Allahu Akbar, La ilaha illallah)
            MelodicPhrase(listOf(1.0 to 0.9, 1.25 to 1.1, 1.0 to 1.6), pauseAfter = 200)
        )

        for (phrase in melodyPhases) {
            if (!scope.isActive || !isPlaying) break
            synthesizeAndPlayPhrase(sampleRate, baseFreq, phrase.notes, volume)
            if (phrase.pauseAfter > 0) {
                delay(phrase.pauseAfter.toLong())
            }
        }
    }

    /**
     * Resonant Takbeer: "Allahu Akbar, Allahu Akbar"
     */
    private suspend fun playTakbeerAcoustic(sound: AdhanSound, volume: Float) {
        val sampleRate = 44100
        val baseFreq = sound.takbeerFrequency()

        val notes = listOf(
            1.0 to 0.8,
            1.25 to 1.0,
            1.33 to 1.1,
            1.0 to 1.3
        )
        // Repeat Takbeer twice
        synthesizeAndPlayPhrase(sampleRate, baseFreq, notes, volume)
        delay(450)
        if (scope.isActive && isPlaying) {
            synthesizeAndPlayPhrase(sampleRate, baseFreq, notes, volume)
        }
    }

    /**
     * Gentle meditation chime with rich multi-harmonic exponential decay.
     */
    private suspend fun playGentleChimeAcoustic(volume: Float) {
        val sampleRate = 44100
        val durationSec = 3.5
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val fundamental = 528.0 // Solfeggio pure harmonic frequency

        val volCoeff = volume.coerceIn(0.05f, 1.0f) * 28000.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Rich brass/bell bell envelope: sharp attack, long bell reverberation
            val envelope = exp(-1.1 * t) * (1.0 - exp(-35.0 * t))
            val wave = (
                sin(2 * Math.PI * fundamental * t) * 0.50 +
                sin(2 * Math.PI * (fundamental * 2.02) * t) * 0.25 +
                sin(2 * Math.PI * (fundamental * 3.01) * t) * 0.15 +
                sin(2 * Math.PI * (fundamental * 4.25) * t) * 0.10
            ) * envelope
            samples[i] = (wave * volCoeff).toInt().coerceIn(-32767, 32767).toShort()
        }

        playRawPcm(sampleRate, samples)
    }

    private suspend fun synthesizeAndPlayPhrase(
        sampleRate: Int,
        baseFreq: Double,
        notes: List<Pair<Double, Double>>, // Multiplier to durationSec
        volume: Float
    ) {
        val totalDurationSec = notes.sumOf { it.second }
        val numSamples = (sampleRate * totalDurationSec).toInt()
        val samples = ShortArray(numSamples)

        val volCoeff = volume.coerceIn(0.05f, 1.0f) * 27000.0
        var sampleOffset = 0

        for ((freqMult, noteDuration) in notes) {
            if (!scope.isActive || !isPlaying) return
            val noteSamples = (sampleRate * noteDuration).toInt()
            val noteFreq = baseFreq * freqMult

            for (i in 0 until noteSamples) {
                if (sampleOffset + i >= samples.size) break
                val t = i.toDouble() / sampleRate
                // Vocal format envelope: rounded attack, sustained center, soft release
                val envAttack = (1.0 - exp(-15.0 * t))
                val envDecay = exp(-1.2 * (t / noteDuration))
                val envelope = envAttack * envDecay

                // Human vocal acoustic harmonics (formants)
                val wave = (
                    sin(2 * Math.PI * noteFreq * t) * 0.55 +
                    sin(2 * Math.PI * (noteFreq * 2.0) * t) * 0.28 +
                    sin(2 * Math.PI * (noteFreq * 3.0) * t) * 0.12 +
                    sin(2 * Math.PI * (noteFreq * 4.0) * t) * 0.05
                ) * envelope

                samples[sampleOffset + i] = (wave * volCoeff).toInt().coerceIn(-32767, 32767).toShort()
            }
            sampleOffset += noteSamples
        }

        playRawPcm(sampleRate, samples)
    }

    private suspend fun playRawPcm(sampleRate: Int, samples: ShortArray) {
        if (!scope.isActive || !isPlaying) return
        try {
            stopTrack()

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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

            activeTrack = track
            track.write(samples, 0, samples.size)
            track.play()

            val durationMs = (samples.size.toDouble() / sampleRate * 1000).toLong()
            val start = System.currentTimeMillis()
            while (scope.isActive && isPlaying && System.currentTimeMillis() - start < durationMs) {
                delay(80)
            }
        } catch (_: Exception) {
        } finally {
            stopTrack()
        }
    }

    private fun stopTrack() {
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null
    }

    /**
     * Immediately stops any currently synthesizing or playing Adhan audio.
     */
    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        stopTrack()
    }

    private data class MelodicPhrase(
        val notes: List<Pair<Double, Double>>,
        val pauseAfter: Int = 400
    )
}
