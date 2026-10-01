package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    ISHA("Isha", "العشاء", R.drawable.salah_times_isha)
}

/**
 * One prayer's time.
 *
 * A *time*, and nothing about what time it is now.
 *
 * `isNext`, `isCurrent` and `isPassed` used to be here, stamped by the engine when it
 * calculated the day. That put a clock reading inside a value describing a schedule,
 * and the reading went stale immediately: the day is only recalculated on a settings
 * or location change and at midnight, so "which prayer is next" froze at whatever it
 * was at the last recalculation while the Today page recomputed it every second and
 * moved on. Two tabs, two answers.
 *
 * "Which prayer is next" is a live reading. It lives in the ViewModel's one-second
 * ticker, as `nextPrayer` and `previousPrayer`, and screens ask it there. Note that
 * `isCompleted` *is* kept here, because it is not a reading of the clock - it is the
 * reader's own tick, stored per day, and it is the same fact on every screen by
 * construction because there is only one copy of it.
 */
data class PrayerTime(
    val prayer: Prayer,
    val time: LocalTime,
    val dateTime: LocalDateTime,
    val isCompleted: Boolean = false
)

data class PrayerTimesDay(
    val date: LocalDate,
    val prayers: List<PrayerTime>,
    val imsak: LocalTime,
    val midnight: LocalTime,
    val lastThirdOfNight: LocalTime,
    val calculationMethod: CalculationMethod,
    val location: UserLocation
)

enum class CalculationMethod(
    val title: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val isMoroccoNationalTable: Boolean = false
) {
    MOROCCO_MINISTRY(
        "Kingdom of Morocco (Habous)",
        "Official national table by Ministry of Islamic Affairs & Habous",
        19.0,
        17.0,
        isMoroccoNationalTable = true
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
)

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
) {
    fun formatDisplay(): String = "$day $monthNameEn $year AH"
    fun formatArabic(): String = "$day $monthNameAr $year هـ"
}
