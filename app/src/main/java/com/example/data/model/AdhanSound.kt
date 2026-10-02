package com.example.data.model

/**
 * The adhan sounds this app can make.
 *
 * ### Why this is an enum and not a list of strings
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
         */
        const val DEFAULT_FALLBACK_HZ = 300.0

        /** The takbeer equivalent of [DEFAULT_FALLBACK_HZ]. */
        const val DEFAULT_TAKBEER_FALLBACK_HZ = 320.0
    }
}
