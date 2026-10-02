package com.example.data.model

/**
 * The adhan sounds this app can make.
 *
 * ### Why this is an enum and not a list of strings
 *
 * The sound was stored as an English label and decoded by `String.contains`, in **four
 * places with four different vocabularies**. They disagreed, and one of the
 * disagreements was audible:
 *
 * - Settings offered five sounds. "Gentle Bell Chime" matched **nothing** in the
 *   synthesizer's five-way match, so it fell through to a 300 Hz adhan - so choosing
 *   the one *non-adhan* option got you a full adhan.
 * - The preview in Settings asked a fifth question, `isAdhanSound`, which listed
 *   Cairo (not offered) and did not list "Gentle Bell Chime" - so previewing it played
 *   a **chime** while the alarm for the same setting played an **adhan**. The reader
 *   heard two different things from one setting.
 * - The takbeer renderer had its own two-way match, so Makkah's adhan started at
 *   330 Hz and its takbeer at 320 Hz, and only Moroccan agreed with itself.
 *
 * Every one of those is the same bug: the identity of a sound was a *word*, and four
 * pieces of code each guessed what the word might say. Here the word is a `key`, the
 * tones are declared next to the name that owns them, and there is nothing to guess.
 *
 * ### The tones are placeholders, and that is stated
 *
 * These are synthesised approximations, not recordings of any adhan. `baseHz` and
 * [takbeerHz] choose a root pitch and a maqam-flavoured phrase; they do not reproduce
 * a muezzin's voice, and no setting in the app should imply otherwise. The
 * *difference* between sounds is what the enum is for, and it is real.
 */
enum class AdhanSound(
    /** The persisted value. Stable, and nothing to do with any language. */
    val key: String,
    /** The label shown in the picker. A place name, or a description. */
    val label: String,
    /**
     * The root pitch of the full adhan, in Hz. `null` for a sound that is not an
     * adhan at all, which is the honest answer for a bell chime - and is what makes
     * [isAdhan] false rather than a guess.
     */
    val baseHz: Double?,
    /**
     * The root pitch of the takbeer, in Hz.
     *
     * Separate from [baseHz] because the takbeer is a different phrase in a different
     * register, and because they were separately wrong: only one of the five sounds
     * had the same value in both, and only by accident.
     */
    val takbeerHz: Double?,
    /** Whether choosing this means "play an adhan" rather than "play a chime". */
    val isAdhan: Boolean
) {
    MAKKAH("makkah", "Makkah Al-Mukarramah", 330.0, 320.0, true),
    MADINAH("madinah", "Madinah An-Nabawi", 294.0, 320.0, true),
    AL_AQSA("al_aqsa", "Al-Aqsa", 262.0, 320.0, true),
    MOROCCAN("moroccan", "Moroccan", 392.0, 392.0, true),

    /**
     * A bell, not a call to prayer.
     *
     * The only option here that is not an adhan, and the only one that used to play
     * an adhan anyway.
     */
    GENTLE_CHIME("gentle_chime", "Gentle Bell Chime", null, 660.0, false);

    /** The root pitch for the full adhan, or [fallbackHz] for a sound without one. */
    fun baseFrequency(fallbackHz: Double = DEFAULT_FALLBACK_HZ): Double =
        baseHz ?: fallbackHz

    /** The root pitch for the takbeer, or [fallbackHz] for a sound without one. */
    fun takbeerFrequency(fallbackHz: Double = DEFAULT_TAKBEER_FALLBACK_HZ): Double =
        takbeerHz ?: fallbackHz

    /**
     * Resolves a stored preference to a sound.
     *
     * Accepts the [key] **and** the old [label], because the label is what every
     * installed build has on disk. A resolver that understood only keys would fall
     * back to the default for every reader who had already chosen a sound - a silent
     * loss of a preference the reader made on purpose.
     *
     * In the companion, and not as an instance method, because the value being
     * resolved is *not* a sound - that is what is being worked out.
     */
    companion object {
        /** Every sound the picker offers, in reading order. */
        val offered: List<AdhanSound> get() = entries.toList()

        fun fromStored(stored: String?): AdhanSound {
            if (stored.isNullOrBlank()) return MAKKAH
            return entries.firstOrNull { it.key == stored }
                ?: entries.firstOrNull { it.label.equals(stored, ignoreCase = true) }
                ?: MAKKAH
        }

        /**
         * What a sound with no declared pitch plays at.
         *
         * Only reached for an adhan with a null [baseHz], which is none of them - the
         * chime has no adhan pitch because it is not an adhan. It exists so a caller
         * that insists on a number gets a defined one rather than a crash.
         */
        const val DEFAULT_FALLBACK_HZ = 300.0

        /** The takbeer equivalent of [DEFAULT_FALLBACK_HZ]. */
        const val DEFAULT_TAKBEER_FALLBACK_HZ = 320.0
    }
}
