package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class Prayer(
    val englishName: String,
    val arabicName: String,
    val icon: ImageVector,
    val isFard: Boolean = true
) {
    FAJR("Fajr", "الفجر", Icons.Default.Brightness6),
    SUNRISE("Sunrise", "الشروق", Icons.Default.Brightness5, isFard = false),
    DHUHR("Dhuhr", "الظهر", Icons.Default.WbSunny),
    ASR("Asr", "العصر", Icons.Default.Brightness7),
    MAGHRIB("Maghrib", "المغرب", Icons.Default.Brightness5),
    ISHA("Isha", "العشاء", Icons.Default.NightsStay)
}

data class PrayerTime(
    val prayer: Prayer,
    val time: LocalTime,
    val dateTime: LocalDateTime,
    val isNext: Boolean = false,
    val isCurrent: Boolean = false,
    val isPassed: Boolean = false,
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
