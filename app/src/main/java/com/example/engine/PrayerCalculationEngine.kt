package com.example.engine

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerCalculationEngine {

    fun calculatePrayerTimes(
        date: LocalDate,
        location: UserLocation,
        method: CalculationMethod,
        madhhab: Madhhab,
        adjustments: PrayerAdjustments,
        zoneId: ZoneId = PrayerTimeZone.forDay(date, ZoneId.systemDefault())
    ): PrayerTimesDay {
        // Everything below is computed in universal time and converted to
        // [zoneId] once, at the end.
        //
        // Solar noon is a UTC instant; which civil hour it lands on is a
        // separate fact. This used to fold a guessed offset into `noon` at the
        // start, which made a wrong offset indistinguishable from a wrong
        // equinox - and made the sun's position be evaluated for a moment that
        // was wrong by however much the guess was wrong.
        val julianDay = calculateJulianDay(date)
        val utcStart = date.atStartOfDay(ZoneOffset.UTC).toInstant()

        val latitude = location.latitude
        val longitude = location.longitude

        // Solar noon: the hour angle is zero, so the only moving part is the
        // equation of time - evaluated *at* noon, which is why this iterates.
        var noonUt = 12.0 - longitude / 15.0
        repeat(3) {
            noonUt = 12.0 - longitude / 15.0 -
                    solarPosition(julianDay + noonUt / 24.0).equationOfTimeHours
        }
        val noonDeclination = solarPosition(julianDay + noonUt / 24.0).declination

        // Fajr angle
        val fajrAngle = -method.fajrAngle
        // Isha angle (null for Umm Al-Qura, which uses +90 min instead)
        val ishaAngle: Double? = if (method.ishaAngle > 0.0) -method.ishaAngle else null

        // Polar day/night: if any twilight angle is unreachable here, compute
        // the day at the nearest latitude where all angles resolve so the
        // returned times stay ordered and next-prayer logic stays correct.
        val requiredAngles = listOfNotNull(fajrAngle, -0.833, ishaAngle)
        val effectiveLat =
            if (requiredAngles.any { cosHourAngle(it, latitude, noonDeclination) !in -1.0..1.0 }) {
                nearestResolvableLatitude(latitude, noonDeclination, requiredAngles)
            } else {
                latitude
            }

        // Sunrise & sunset angle (-0.833° accounting for refraction & solar disk)
        val fajrUt = eventUt(julianDay, effectiveLat, noonUt, fajrAngle, morning = true)
        val sunriseUt = eventUt(julianDay, effectiveLat, noonUt, -0.833, morning = true)
        val maghribUt = eventUt(julianDay, effectiveLat, noonUt, -0.833, morning = false)

        // Asr is defined by shadow length, so its altitude depends on the
        // declination - which depends on when Asr is. Two passes settle it.
        var asrUt = noonUt
        var asrDeclination = noonDeclination
        repeat(2) {
            val asrAltitude = toDegrees(
                atan(1.0 / (madhhab.shadowFactor + tan(toRadians(Math.abs(effectiveLat - asrDeclination)))))
            )
            asrUt = eventUt(julianDay, effectiveLat, noonUt, asrAltitude, morning = false)
            asrDeclination = solarPosition(julianDay + asrUt / 24.0).declination
        }

        val ishaUt = if (ishaAngle != null) {
            eventUt(julianDay, effectiveLat, noonUt, ishaAngle, morning = false)
        } else {
            // Umm Al-Qura 90 min after Maghrib
            maghribUt + 1.5
        }

        // Round, and meanwhile let the convention's margins and the reader's
        // adjustments land on whole minutes.
        val fajrTime = localMinute(utcStart, zoneId, fajrUt, adjustments.fajr)
        val sunriseTime = localMinute(
            utcStart, zoneId, sunriseUt, method.sunriseOffsetMinutes + adjustments.sunrise
        )
        val dhuhrTime = localMinute(
            utcStart, zoneId, noonUt, method.dhuhrOffsetMinutes + adjustments.dhuhr
        )
        val asrTime = localMinute(utcStart, zoneId, asrUt, adjustments.asr)
        val maghribTime = localMinute(
            utcStart, zoneId, maghribUt, method.maghribOffsetMinutes + adjustments.maghrib
        )
        val ishaTime = localMinute(utcStart, zoneId, ishaUt, adjustments.isha)

        return assemble(
            date = date,
            times = mapOf(
                Prayer.FAJR to fajrTime,
                Prayer.SUNRISE to sunriseTime,
                Prayer.DHUHR to dhuhrTime,
                Prayer.ASR to asrTime,
                Prayer.MAGHRIB to maghribTime,
                Prayer.ISHA to ishaTime
            ),
            method = method,
            location = location,
            zoneId = zoneId
        )
    }

    /**
     * Assembles a day from six wall-clock times.
     *
     * The single place where a set of times becomes a [PrayerTimesDay]: the
     * ordering safety net, the prayers list, and the night divisions (Imsak,
     * midnight, last third). Calculated days and published days both come
     * through here, because those divisions are derived from Fajr and Maghrib
     * and deriving them twice would let the two paths disagree about midnight
     * on the very night the ministry's table is in use.
     */
    fun assemble(
        date: LocalDate,
        times: Map<Prayer, LocalTime>,
        method: CalculationMethod,
        location: UserLocation,
        zoneId: ZoneId
    ): PrayerTimesDay {
        // Safety net: when the device zone differs greatly from solar time at
        // the location (e.g. travel before the zone updates), or in extreme
        // polar fallback geometry, raw times can wrap past civil midnight and
        // come back unordered. Every consumer of this day (countdown,
        // checklist, calendar, alarms, next/previous lookup) requires a
        // strictly increasing day, so enforce monotonicity with 1-minute
        // steps. Never triggers for normal in-zone days.
        val ordered = enforceOrderedDay(Prayer.entries.map { times.getValue(it) })

        // Times, and nothing else.
        //
        // This used to also stamp `isNext`, `isCurrent` and `isPassed` onto each
        // prayer, chosen from `currentTime` at the moment it was called. That made a
        // *time* calculation carry a *clock* reading, which is the wrong shape for two
        // reasons that both showed up on screen:
        //
        // - The stamp was only refreshed when this function ran - on a settings or
        //   location change, and at midnight - so between those it went stale. The
        //   Prayer tab highlighted whichever prayer was next at the last
        //   recalculation, while the Today page recomputed every second and advanced.
        //   Two tabs, two answers, for hours at a time.
        // - `isToday` decided whether to stamp at all, so the same day carried the
        //   fields or not depending on which calendar the caller asked about.
        //
        // "Which prayer is next" is a live reading, so it lives where live readings
        // live: the ViewModel's one-second ticker, which publishes `nextPrayer` and
        // `previousPrayer`. Screens ask that. This function answers the only question
        // it can answer truthfully - what time is each prayer - and nothing about
        // what time it is now.
        val prayers = Prayer.entries.mapIndexed { index, prayer ->
            PrayerTime(
                prayer = prayer,
                time = ordered[index],
                dateTime = LocalDateTime.of(date, ordered[index])
            )
        }

        val fajr = ordered[Prayer.FAJR.ordinal]
        val maghrib = ordered[Prayer.MAGHRIB.ordinal]

        // Night divisions (Imsak = 10m before Fajr, Midnight = halfway between Maghrib and Fajr, Last Third)
        val nightDurationMinutes =
            Duration.between(maghrib, fajr.plusHours(24)).toMinutes() % 1440

        return PrayerTimesDay(
            date = date,
            prayers = prayers,
            imsak = fajr.minusMinutes(10),
            midnight = maghrib.plusMinutes(nightDurationMinutes / 2),
            lastThirdOfNight = maghrib.plusMinutes((nightDurationMinutes * 2) / 3),
            calculationMethod = method,
            location = location,
            zoneId = zoneId
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

    /**
     * The time left until [targetTime], in the form the Today page shows it.
     *
     * ### Two tiers, and why not `HH:MM:SS`
     *
     * A target on the far side of midnight counts forwards rather than backwards,
     * so the countdown never reads as a large negative.
     */
    fun formatRemainingCountdown(targetTime: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        var seconds = ChronoUnit.SECONDS.between(now, targetTime)
        if (seconds <= 0) seconds += 24 * 3600
        if (seconds >= 24 * 3600) seconds = 0

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02dh %02dm", hours, minutes)
        } else {
            String.format(Locale.US, "%02dm %02ds", minutes, secs)
        }
    }

    private fun calculateHourAngle(alpha: Double, lat: Double, declination: Double): Double {
        val clamped = cosHourAngle(alpha, lat, declination).coerceIn(-1.0, 1.0)
        return toDegrees(acos(clamped))
    }

    private fun cosHourAngle(alpha: Double, lat: Double, declination: Double): Double {
        return (sin(toRadians(alpha)) - sin(toRadians(lat)) * sin(toRadians(declination))) /
                (cos(toRadians(lat)) * cos(toRadians(declination)))
    }

    /**
     * Nearest-latitude fallback (aqrab al-bilad) for polar day/night: when the
     * sun never reaches a required twilight angle at [latitude], the clamped
     * geometry would wrap times past midnight and misorder the day. Instead,
     * step toward the equator until every required angle resolves, and compute
     * the day there. Longitude (solar noon) is unaffected.
     */
    private fun nearestResolvableLatitude(
        latitude: Double,
        declination: Double,
        angles: List<Double>
    ): Double {
        var candidate = latitude
        val step = if (latitude >= 0) -0.5 else 0.5
        repeat(120) {
            if (angles.all { cosHourAngle(it, candidate, declination).let { c -> c in -1.0..1.0 } }) {
                return candidate
            }
            candidate += step
        }
        return 45.0 * if (latitude >= 0) 1 else -1
    }

    /**
     * Enforces a strictly increasing same-day sequence with 1-minute steps.
     * Already-ordered days (the normal case) pass through untouched; only a
     * wrapped-around time is pushed forward to restore monotonicity. If the
     * day is degenerate past repair (push would cross midnight), the time is
     * pinned to the end of day as a best effort that keeps every consumer
     * crash-free and the list non-decreasing.
     */
    private fun enforceOrderedDay(times: List<LocalTime>): List<LocalTime> {
        if (times.size < 2) return times
        val out = times.toMutableList()
        for (i in 1 until out.size) {
            val minNext = out[i - 1].plusMinutes(1)
            if (minNext.isBefore(out[i - 1])) {
                // plusMinutes wrapped past midnight: pin to end of day.
                out[i] = LocalTime.of(23, 59, 59)
            } else if (out[i].isBefore(minNext)) {
                out[i] = minNext
            }
        }
        return out
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

    /**
     * The sun's declination and equation of time at the instant [julianDay]
     * (a Julian day, so `.5` of a day is a whole number of UTC hours).
     *
     * Low-precision solar theory, good to about 0.01° - roughly 30 seconds of
     * hour angle, well below the one-minute resolution a timetable prints. The
     * input is an instant rather than a date on purpose: the whole point of
     * this function is that it can be called *at* the prayer being computed.
     */
    private fun solarPosition(julianDay: Double): SolarPosition {
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val declination = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))
        val ra = fixAngle(toDegrees(atan2(cos(toRadians(e)) * sin(toRadians(l)), cos(toRadians(l)))))
        var eqDiff = q - ra
        while (eqDiff < -180.0) eqDiff += 360.0
        while (eqDiff > 180.0) eqDiff -= 360.0
        return SolarPosition(declination, eqDiff / 15.0)
    }

    /**
     * The UT hour at which the sun crosses [altitude] on the day starting at
     * [julianDay] 0h UT, given the day's converged solar noon [noonUt].
     *
     * Iterated, because the declination at dawn is not the declination at
     * midnight - the earth moves ~0.4° per day along its orbit, and the sun's
     * declination changes by up to 0.4° per day near the equinoxes. Evaluating
     * it once at 00:00 UT (what this used to do) is worth under a minute at
     * Rabat and about eleven minutes at Reykjavík, which is why the frozen
     * version looked fine in Morocco and nowhere else.
     */
    private fun eventUt(
        julianDay: Double,
        latitude: Double,
        noonUt: Double,
        altitude: Double,
        morning: Boolean
    ): Double {
        var ut = noonUt
        repeat(3) {
            val declination = solarPosition(julianDay + ut / 24.0).declination
            val hourAngle = calculateHourAngle(altitude, latitude, declination) / 15.0
            ut = if (morning) noonUt - hourAngle else noonUt + hourAngle
        }
        return ut
    }

    /**
     * [utHours] as a wall-clock time in [zoneId], rounded to the nearest minute.
     *
     * Rounding, not truncation. `decimalToTime` used to floor the seconds, so a
     * prayer 40 seconds past the minute was announced at the *previous* minute;
     * the ministry's table rounds. Prayer times are announced to the minute
     * everywhere, and a timetable that is systematically up to 59 seconds early
     * disagrees with the mosque's own board.
     *
     * The zone's offset is read at the resulting instant rather than at
     * midnight, so a day on which the clock changes still gets each prayer's
     * own offset.
     */
    private fun localMinute(
        utcStart: Instant,
        zoneId: ZoneId,
        utHours: Double,
        extraMinutes: Int
    ): LocalTime {
        val instant = utcStart.plusSeconds(floor(utHours * 3600.0).toLong())
        val offsetSeconds = zoneId.rules.getOffset(instant).totalSeconds
        val total = utHours * 60.0 + offsetSeconds / 60.0 + extraMinutes + 0.5
        val minutes = floor(total).toInt().mod(24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    private data class SolarPosition(
        val declination: Double,
        val equationOfTimeHours: Double
    )

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0.0) a += 360.0
        return a
    }

    private fun toRadians(deg: Double): Double = Math.toRadians(deg)
    private fun toDegrees(rad: Double): Double = Math.toDegrees(rad)
}
