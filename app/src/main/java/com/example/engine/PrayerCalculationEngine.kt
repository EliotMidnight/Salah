package com.example.engine

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerCalculationEngine {

    // Official Moroccan Ministry of Habous & Islamic Affairs baseline calibrations
    // For Moroccan cities, the national table calibrates exact astronomical horizons:
    private val MOROCCO_CITY_DELTAS = mapOf(
        "Rabat" to Triple(0, 0, 0),
        "Casablanca" to Triple(4, 3, 3), // +4 min Fajr, +3 min Maghrib
        "Marrakech" to Triple(4, 5, 4),
        "Fes" to Triple(-7, -8, -7),
        "Tangier" to Triple(-2, -4, -3),
        "Agadir" to Triple(8, 9, 8),
        "Meknes" to Triple(-5, -6, -5),
        "Oujda" to Triple(-19, -20, -19),
        "Tetouan" to Triple(-3, -5, -4)
    )

    fun calculatePrayerTimes(
        date: LocalDate,
        location: UserLocation,
        method: CalculationMethod,
        madhhab: Madhhab,
        adjustments: PrayerAdjustments
    ): PrayerTimesDay {
        val zoneOffsetHours = calculateTimeZoneOffset(location, date)

        // Astronomical Solar Coordinates
        val julianDay = calculateJulianDay(date)
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val declination = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))
        val ra = fixAngle(toDegrees(atan2(cos(toRadians(e)) * sin(toRadians(l)), cos(toRadians(l)))))
        var eqDiff = (q - ra)
        while (eqDiff < -180.0) eqDiff += 360.0
        while (eqDiff > 180.0) eqDiff -= 360.0
        val equationOfTime = eqDiff / 15.0

        // Solar Noon (Dhuhr base)
        val noon = 12.0 + zoneOffsetHours - (location.longitude / 15.0) - equationOfTime

        // Solar Angles
        val lat = location.latitude

        // Sunrise & Sunset Angle (-0.833° accounting for refraction & solar disk)
        val sunriseHourAngle = calculateHourAngle(-0.833, lat, declination)

        // Fajr angle
        val fajrAngle = -method.fajrAngle
        val fajrHourAngle = calculateHourAngle(fajrAngle, lat, declination)

        // Isha angle / offset
        val ishaHourAngle = if (method.ishaAngle > 0.0) {
            calculateHourAngle(-method.ishaAngle, lat, declination)
        } else {
            0.0
        }

        // Asr Angle
        val asrAltitude = toDegrees(
            atan(1.0 / (madhhab.shadowFactor + tan(toRadians(Math.abs(lat - declination)))))
        )
        val asrHourAngle = calculateHourAngle(asrAltitude, lat, declination)

        // Raw decimal hours
        var fajrRaw = noon - (fajrHourAngle / 15.0)
        var sunriseRaw = noon - (sunriseHourAngle / 15.0)
        var dhuhrRaw = noon
        var asrRaw = noon + (asrHourAngle / 15.0)
        var maghribRaw = noon + (sunriseHourAngle / 15.0)
        var ishaRaw = if (method.ishaAngle > 0.0) {
            noon + (ishaHourAngle / 15.0)
        } else {
            // Umm Al-Qura 90 min after Maghrib
            maghribRaw + 1.5
        }

        // Moroccan National Table flagship refinement
        if (method == CalculationMethod.MOROCCO_MINISTRY && location.country.contains("Morocco", ignoreCase = true)) {
            val delta = MOROCCO_CITY_DELTAS[location.name] ?: Triple(0, 0, 0)
            fajrRaw += delta.first / 60.0
            dhuhrRaw += delta.second / 60.0
            maghribRaw += delta.third / 60.0
            asrRaw += delta.second / 60.0
            ishaRaw += delta.third / 60.0
        }

        // Convert decimal hours to LocalTime with adjustments
        val fajrTime = decimalToTime(fajrRaw).plusMinutes(adjustments.fajr.toLong())
        val sunriseTime = decimalToTime(sunriseRaw).plusMinutes(adjustments.sunrise.toLong())
        val dhuhrTime = decimalToTime(dhuhrRaw).plusMinutes(adjustments.dhuhr.toLong())
        val asrTime = decimalToTime(asrRaw).plusMinutes(adjustments.asr.toLong())
        val maghribTime = decimalToTime(maghribRaw).plusMinutes(adjustments.maghrib.toLong())
        val ishaTime = decimalToTime(ishaRaw).plusMinutes(adjustments.isha.toLong())

        val now = LocalDateTime.now()
        val isToday = date == now.toLocalDate()
        val currentTime = now.toLocalTime()

        // Assemble Prayer Times
        val prayerList = listOf(
            PrayerTime(
                prayer = Prayer.FAJR,
                time = fajrTime,
                dateTime = LocalDateTime.of(date, fajrTime)
            ),
            PrayerTime(
                prayer = Prayer.SUNRISE,
                time = sunriseTime,
                dateTime = LocalDateTime.of(date, sunriseTime)
            ),
            PrayerTime(
                prayer = Prayer.DHUHR,
                time = dhuhrTime,
                dateTime = LocalDateTime.of(date, dhuhrTime)
            ),
            PrayerTime(
                prayer = Prayer.ASR,
                time = asrTime,
                dateTime = LocalDateTime.of(date, asrTime)
            ),
            PrayerTime(
                prayer = Prayer.MAGHRIB,
                time = maghribTime,
                dateTime = LocalDateTime.of(date, maghribTime)
            ),
            PrayerTime(
                prayer = Prayer.ISHA,
                time = ishaTime,
                dateTime = LocalDateTime.of(date, ishaTime)
            )
        )

        // Highlight next, current, passed prayers
        val decoratedList = if (isToday) {
            val nextPrayerEnum = getNextPrayerEnum(currentTime, prayerList)
            val currentPrayerEnum = getCurrentPrayerEnum(currentTime, prayerList)

            prayerList.map { pt ->
                val passed = currentTime.isAfter(pt.time)
                pt.copy(
                    isNext = pt.prayer == nextPrayerEnum,
                    isCurrent = pt.prayer == currentPrayerEnum,
                    isPassed = passed
                )
            }
        } else {
            prayerList
        }

        // Night divisions (Imsak = 10m before Fajr, Midnight = halfway between Maghrib and Fajr, Last Third)
        val imsakTime = fajrTime.minusMinutes(10)
        val nightDurationMinutes = Duration.between(maghribTime, fajrTime.plusHours(24)).toMinutes() % 1440
        val midnight = maghribTime.plusMinutes(nightDurationMinutes / 2)
        val lastThird = maghribTime.plusMinutes((nightDurationMinutes * 2) / 3)

        return PrayerTimesDay(
            date = date,
            prayers = decoratedList,
            imsak = imsakTime,
            midnight = midnight,
            lastThirdOfNight = lastThird,
            calculationMethod = method,
            location = location
        )
    }

    fun getNextPrayer(day: PrayerTimesDay, currentTime: LocalTime = LocalTime.now()): PrayerTime {
        val fardList = day.prayers.filter { it.prayer != Prayer.SUNRISE }
        for (pt in fardList) {
            if (currentTime.isBefore(pt.time)) {
                return pt
            }
        }
        // If all prayers today have passed, the next is tomorrow's Fajr
        val fajr = day.prayers.firstOrNull { it.prayer == Prayer.FAJR }
        if (fajr != null) return fajr.copy(dateTime = fajr.dateTime.plusDays(1))
        val first = day.prayers.firstOrNull() ?: return day.prayers.first()
        return first.copy(dateTime = first.dateTime.plusDays(1))
    }

    fun getPreviousPrayer(day: PrayerTimesDay, currentTime: LocalTime = LocalTime.now()): PrayerTime {
        val fardList = day.prayers.filter { it.prayer != Prayer.SUNRISE }
        for (pt in fardList.reversed()) {
            if (currentTime.isAfter(pt.time)) {
                return pt
            }
        }
        // If before Fajr, previous was yesterday's Isha
        val isha = day.prayers.firstOrNull { it.prayer == Prayer.ISHA }
        if (isha != null) return isha.copy(dateTime = isha.dateTime.minusDays(1))
        val last = day.prayers.lastOrNull() ?: return day.prayers.first()
        return last.copy(dateTime = last.dateTime.minusDays(1))
    }

    fun formatRemainingCountdown(targetTime: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val seconds = ChronoUnit.SECONDS.between(now, targetTime)
        if (seconds <= 0) return "00:00:00"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun getNextPrayerEnum(currentTime: LocalTime, list: List<PrayerTime>): Prayer {
        for (pt in list) {
            if (currentTime.isBefore(pt.time)) {
                return pt.prayer
            }
        }
        return Prayer.FAJR
    }

    private fun getCurrentPrayerEnum(currentTime: LocalTime, list: List<PrayerTime>): Prayer? {
        for (i in list.indices.reversed()) {
            if (currentTime.isAfter(list[i].time) || currentTime == list[i].time) {
                return list[i].prayer
            }
        }
        return Prayer.ISHA
    }

    private fun calculateHourAngle(alpha: Double, lat: Double, declination: Double): Double {
        val cosHA = (sin(toRadians(alpha)) - sin(toRadians(lat)) * sin(toRadians(declination))) /
                (cos(toRadians(lat)) * cos(toRadians(declination)))
        val clamped = cosHA.coerceIn(-1.0, 1.0)
        return toDegrees(acos(clamped))
    }

    private fun calculateJulianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        val day = date.dayOfMonth

        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5
    }

    private fun calculateTimeZoneOffset(location: UserLocation, date: LocalDate): Double {
        if (location.country.contains("Morocco", ignoreCase = true)) {
            return 1.0 // Morocco WEST (UTC+1)
        }
        if (location.country.contains("Saudi", ignoreCase = true)) {
            return 3.0 // AST (UTC+3)
        }
        if (location.country.contains("Egypt", ignoreCase = true) || location.country.contains("Turkey", ignoreCase = true)) {
            return 3.0
        }
        if (location.country.contains("UK", ignoreCase = true)) {
            return 1.0
        }
        if (location.country.contains("USA", ignoreCase = true)) {
            return -4.0
        }
        return try {
            val zoneId = ZoneId.systemDefault()
            val zonedDateTime = date.atStartOfDay(zoneId)
            zonedDateTime.offset.totalSeconds / 3600.0
        } catch (e: Exception) {
            Math.round(location.longitude / 15.0).toDouble()
        }
    }

    private fun decimalToTime(decimalHours: Double): LocalTime {
        var hours = decimalHours
        while (hours < 0.0) hours += 24.0
        while (hours >= 24.0) hours -= 24.0

        val h = floor(hours).toInt()
        val totalMinutes = floor((hours - h) * 60.0).toInt()
        val totalSeconds = floor(((hours - h) * 60.0 - totalMinutes) * 60.0).toInt()
        return LocalTime.of(
            h.coerceIn(0, 23),
            totalMinutes.coerceIn(0, 59),
            totalSeconds.coerceIn(0, 59)
        )
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0.0) a += 360.0
        return a
    }

    private fun toRadians(deg: Double): Double = Math.toRadians(deg)
    private fun toDegrees(rad: Double): Double = Math.toDegrees(rad)
    private fun round(v: Double): Long = Math.round(v)
}
