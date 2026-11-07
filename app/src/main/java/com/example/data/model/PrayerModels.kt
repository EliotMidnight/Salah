package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * @param iconRes per-prayer artwork from the Salah Icons set (CC BY 4.0). Each
 *   prayer gets its own glyph rather than a generic sun/moon, which is what
 *   makes the schedule scannable at a glance instead of requiring the name.
 */
enum class Prayer(
    val englishName: String,
    val arabicName: String,
    @DrawableRes val iconRes: Int,
    val isFard: Boolean = true
) {
    FAJR("Fajr", "الفجر", R.drawable.salah_times_fajr),
    SUNRISE("Sunrise", "الشروق", R.drawable.salah_times_sunrise, isFard = false),
    DHUHR("Dhuhr", "الظهر", R.drawable.salah_times_dhuhr),
    ASR("Asr", "العصر", R.drawable.salah_times_asr),
    MAGHRIB("Maghrib", "المغرب", R.drawable.salah_times_maghrib),
    ISHA("Isha", "العشاء", R.drawable.salah_times_isha);

    /**
     * What this prayer does on a fresh install, before the reader has ever chosen.
     *
     * A property of the *prayer*, so it is stated here rather than in the repository
     * that stores the choice - and the answer was written in three places with two
     * different values. The preference loader and the UI state's field initialiser
     * both gave Sunrise a **silent reminder**; `PrayerAlertService`, which is what
     * actually decides whether sound plays, defaulted every prayer to a **full
     * adhan**. So a reader who had never opened the per-prayer sheet was told sunrise
     * was silent, and heard a full call to prayer at sunrise.
     */
    val defaultAlertMode: String
        get() = if (this == SUNRISE) ALERT_MODE_REMINDER else ALERT_MODE_FULL
}

/** A full adhan: the call to prayer, with its takbir. */
const val ALERT_MODE_FULL = "Full Adhan"

/** A silent reminder: the notification, no call. */
const val ALERT_MODE_REMINDER = "Silent Reminder"

/** Every prayer's default, which is also what "reset all settings" restores. */
val defaultAlertModes: Map<Prayer, String>
    get() = Prayer.entries.associateWith { it.defaultAlertMode }

/**
 * One prayer's time.
 *
 * A *time*, and nothing about what time it is now.
 *
 * "Which prayer is next" is a live reading. It lives in the ViewModel's one-second
 * ticker, as `nextPrayer` and `previousPrayer`, and screens ask it there.
 */
data class PrayerTime(
    val prayer: Prayer,
    val time: LocalTime,
    val dateTime: LocalDateTime
)

/**
 * One day's schedule.
 *
 * @param zoneId the legal clock these [prayers] are written in. It travels with
 *   the day because a `LocalTime` alone is ambiguous: 05:17 is a different
 *   instant in Casablanca and in Cairo. The alarm scheduler converts these
 *   wall-clock times to instants, and it used to do so with
 *   `ZoneId.systemDefault()` - a second opinion about the same fact, which
 *   silently disagreed with the engine on exactly the days the two clocks
 *   differ (travel, and Morocco's 2026 change of legal time).
 */
data class PrayerTimesDay(
    val date: LocalDate,
    val prayers: List<PrayerTime>,
    val imsak: LocalTime,
    val midnight: LocalTime,
    val lastThirdOfNight: LocalTime,
    val calculationMethod: CalculationMethod,
    val location: UserLocation,
    val zoneId: ZoneId
)

/**
 * A calculation convention: the twilight angles, and the margins a particular
 * authority adds to the astronomical event afterwards.
 *
 * The margins are the non-obvious half. Morocco's ministry does not publish the
 * raw astronomical event; it publishes the moment the mosque announces, which
 * is a whole number of minutes later for Dhuhr and Maghrib and earlier for
 * Sunrise. Measured against the ministry's November 2026 table for Rabat, the
 * margins are Sunrise −3, Dhuhr +5, Maghrib +4; with them the calculated day
 * reproduces the published day to the minute. Without them it is up to five
 * minutes early at Dhuhr and four at Maghrib, all year.
 *
 * @param sunriseOffsetMinutes additive margin on the astronomical sunrise
 * @param dhuhrOffsetMinutes additive margin on solar noon
 * @param maghribOffsetMinutes additive margin on the astronomical sunset
 */
enum class CalculationMethod(
    val title: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val isMoroccoNationalTable: Boolean = false,
    val sunriseOffsetMinutes: Int = 0,
    val dhuhrOffsetMinutes: Int = 0,
    val maghribOffsetMinutes: Int = 0
) {
    MOROCCO_MINISTRY(
        "Kingdom of Morocco (Habous)",
        "Official national table by Ministry of Islamic Affairs & Habous",
        19.0,
        17.0,
        isMoroccoNationalTable = true,
        sunriseOffsetMinutes = -3,
        dhuhrOffsetMinutes = 5,
        maghribOffsetMinutes = 4
    ),
    MWL(
        "Muslim World League",
        "Standard European and global calculation (Fajr 18°, Isha 17°)",
        18.0,
        17.0
    ),
    ISNA(
        "Islamic Society of North America",
        "North American convention (Fajr 15°, Isha 15°)",
        15.0,
        15.0
    ),
    EGYPT(
        "Egyptian General Survey",
        "Egypt, Africa, Levant & parts of Arab world (Fajr 19.5°, Isha 17.5°)",
        19.5,
        17.5
    ),
    UMM_AL_QURA(
        "Umm Al-Qura, Makkah",
        "Saudi Arabia & Arabian Peninsula (Fajr 18.5°, Isha 90 min)",
        18.5,
        0.0 // 90 min after Maghrib
    ),
    KARACHI(
        "Univ. of Islamic Sciences, Karachi",
        "Pakistan, India, Bangladesh, Afghanistan (Fajr 18°, Isha 18°)",
        18.0,
        18.0
    ),
    DUBAI(
        "Dubai / UAE Awqaf",
        "United Arab Emirates (Fajr 18.2°, Isha 18.2°)",
        18.2,
        18.2
    ),
    FRANCE(
        "France (12° Method)",
        "Union des Organisations Islamiques de France (12° / 12°)",
        12.0,
        12.0
    )
}

enum class Madhhab(val title: String, val shadowFactor: Double) {
    STANDARD("Standard (Shafi, Maliki, Hanbali)", 1.0),
    HANAFI("Hanafi", 2.0)
}

data class PrayerAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    /**
     * This reader's adjustment for [prayer], in minutes.
     *
     * Named by prayer so that a consumer holding a [Prayer] - the published
     * timetable does - can apply the reader's intent without restating the
     * prayer-to-field mapping. That mapping exists once, here.
     */
    fun forPrayer(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

data class UserLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isGps: Boolean = false
) {
    companion object {
        val RABAT = UserLocation("Rabat", "Morocco", 34.020882, -6.841650)
        val CASABLANCA = UserLocation("Casablanca", "Morocco", 33.5731, -7.5898)
        val MARRAKECH = UserLocation("Marrakech", "Morocco", 31.6295, -7.9811)
        val FES = UserLocation("Fes", "Morocco", 34.0181, -5.0078)
        val TANGIER = UserLocation("Tangier", "Morocco", 35.7595, -5.8340)
        val AGADIR = UserLocation("Agadir", "Morocco", 30.4278, -9.5981)
        val MECCA = UserLocation("Mecca", "Saudi Arabia", 21.4225, 39.8262)
        val MEDINA = UserLocation("Medina", "Saudi Arabia", 24.5247, 39.5692)
        val CAIRO = UserLocation("Cairo", "Egypt", 30.0444, 31.2357)
        val ISTANBUL = UserLocation("Istanbul", "Turkey", 41.0082, 28.9784)
        val LONDON = UserLocation("London", "United Kingdom", 51.5074, -0.1278)
        val PARIS = UserLocation("Paris", "France", 48.8566, 2.3522)
        val NEW_YORK = UserLocation("New York", "United States", 40.7128, -74.0060)
        val JAKARTA = UserLocation("Jakarta", "Indonesia", -6.2088, 106.8456)
        val KUALA_LUMPUR = UserLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869)
        val SYDNEY = UserLocation("Sydney", "Australia", -33.8688, 151.2093)

        val DEFAULT = RABAT

        val POPULAR_CITIES = listOf(
            RABAT, CASABLANCA, MARRAKECH, FES, TANGIER, AGADIR,
            MECCA, MEDINA, CAIRO, ISTANBUL, LONDON, PARIS, NEW_YORK, JAKARTA, KUALA_LUMPUR, SYDNEY
        )
    }
}

data class HijriDate(
    val day: Int,
    val monthNumber: Int,
    val monthNameEn: String,
    val monthNameAr: String,
    val year: Int
)
