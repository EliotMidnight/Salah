package com.example.data.model

import java.util.Locale

/**
 * The recitations the Quran player can stream, one directory each on
 * everyayah.com.
 *
 * ### Why this is an enum and not a list of strings
 *
 * - Settings offered four reciters and the player streamed one: the stream's
 *   directory was literal text inside `QuranAudioPlayer.playAyah`, so the
 *   recitation banner named whichever reciter the reader had picked while every
 *   recitation heard was Alafasy's. A control that does not do anything is worse
 *   than no control, because it is a promise - here, a promise about what the
 *   reader is hearing.
 * - The Settings preview was keyed on `String.contains`, on a list that
 *   included a reciter this app never offered.
 *
 * [AdhanSound] made the same repair for the adhan sounds: the picker, the
 * player and the preview all read one registry, so there is nothing left to
 * keep in step.
 *
 * @property displayName The label Settings offers and the recitation banner
 *   repeats. Proper nouns are deliberately not translated, for the same reason
 *   typeface names are not.
 * @property directory The folder the files live in on everyayah.com. The
 *   spellings here are the site's, not ours - "Saood_ash-Shuraym" and not
 *   "Saud Ash-Shuraim" - because the directory name is part of the URL, not
 *   part of the name.
 */
enum class Reciter(
    val displayName: String,
    val directory: String
) {
    ALAFASY("Mishary Rashid Alafasy", "Alafasy_128kbps"),
    BASIT("Abdul Basit Abdus Samad", "Abdul_Basit_Murattal_192kbps"),
    HUSARY("Mahmoud Khalil Al-Husary", "Husary_128kbps"),
    SHURAIM("Saud Ash-Shuraim", "Saood_ash-Shuraym_128kbps");

    /**
     * The URL of one verse's file on everyayah.com.
     *
     * `Locale.ROOT` deliberately, and the reason is not tidiness: these digits
     * go into a URL, and in a locale whose digits are not ASCII - Arabic, Urdu,
     * Bengali, all of which this app ships in - `String.format("%03d", 2)`
     * produces "٠٠٢". everyayah.com names files in ASCII, so the request 404s
     * and the reader hears nothing, in exactly the languages where listening
     * matters most. Lint flags the implicit default here and was right to.
     */
    fun verseUrl(surahNumber: Int, ayahNumber: Int): String {
        val paddedSurah = String.format(Locale.ROOT, "%03d", surahNumber)
        val paddedAyah = String.format(Locale.ROOT, "%03d", ayahNumber)
        return "https://everyayah.com/data/$directory/${paddedSurah}${paddedAyah}.mp3"
    }

    companion object {
        /**
         * The reciter whose name a preference stores, or [ALAFASY] for anything
         * it cannot name.
         *
         * The fallback is what keeps the recitation banner honest: it can only
         * ever name a reciter whose stream exists, because it names the
         * resolution of this function and not the preference itself.
         */
        fun from(displayName: String?): Reciter =
            entries.firstOrNull { it.displayName == displayName?.trim() } ?: ALAFASY

        /** The recitations Settings offers. */
        val offered: List<Reciter> get() = entries.toList()
    }
}
