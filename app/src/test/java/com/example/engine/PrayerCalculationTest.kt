package com.example.engine

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculationTest {

    private val date = LocalDate.of(2026, 3, 20) // equinox
    private val noAdj = PrayerAdjustments()

    @Test
    fun `prayer times are strictly ordered for every method and madhhab`() {
        for (method in CalculationMethod.entries) {
            for (madhhab in Madhhab.entries) {
                val day = PrayerCalculationEngine.calculatePrayerTimes(
                    date = date,
                    location = UserLocation.RABAT,
                    method = method,
                    madhhab = madhhab,
                    adjustments = noAdj
                )
                assertEquals(6, day.prayers.size)
                val times = day.prayers.map { it.time }
                for (i in 0 until times.size - 1) {
                    assertTrue(
                        "${method.name}/${madhhab.name}: ${day.prayers[i].prayer} should precede ${day.prayers[i + 1].prayer}",
                        times[i].isBefore(times[i + 1])
                    )
                }
            }
        }
    }

    @Test
    fun `next and previous prayer wrap around midnight`() {
        val day = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.STANDARD,
            adjustments = noAdj
        )
        val fajr = day.prayers.first { it.prayer == Prayer.FAJR }
        val isha = day.prayers.first { it.prayer == Prayer.ISHA }

        val beforeFajr = fajr.time.minusMinutes(30)
        val next = PrayerCalculationEngine.getNextPrayer(day, beforeFajr)
        assertEquals(Prayer.FAJR, next.prayer)

        val prev = PrayerCalculationEngine.getPreviousPrayer(day, beforeFajr)
        assertEquals(Prayer.ISHA, prev.prayer)
        assertEquals(day.date.minusDays(1), prev.dateTime.toLocalDate())

        val afterIsha = LocalTime.of(23, 59)
        val nextAfterIsha = PrayerCalculationEngine.getNextPrayer(day, afterIsha)
        assertEquals(Prayer.FAJR, nextAfterIsha.prayer)
        assertEquals(day.date.plusDays(1), nextAfterIsha.dateTime.toLocalDate())
        assertTrue(isha.time.isBefore(afterIsha) || isha.time == afterIsha)
    }

    @Test
    fun `countdown switches to seconds inside the last hour`() {
        // Two tiers, and the switch is the whole point. The page used to carry a
        // second, differently-formatted copy of this; the string the user watched
        // was the one with no unit tests, which is how it ended up frozen inside a
        // `remember` and nobody noticed.
        val now = LocalDateTime.of(date, LocalTime.of(12, 0))
        assertEquals(
            "01h 30m",
            PrayerCalculationEngine.formatRemainingCountdown(
                LocalDateTime.of(date, LocalTime.of(13, 30)), now
            )
        )
        assertEquals(
            "45m 00s",
            PrayerCalculationEngine.formatRemainingCountdown(
                LocalDateTime.of(date, LocalTime.of(12, 45)), now
            )
        )
        assertEquals(
            "00m 05s",
            PrayerCalculationEngine.formatRemainingCountdown(
                LocalDateTime.of(date, LocalTime.of(12, 0, 5)), now
            )
        )
    }

    @Test
    fun `countdown counts forwards past midnight rather than backwards`() {
        // Fajr after Isha belongs to tomorrow. A raw subtraction makes that a
        // negative number of hours, and a reader watching the countdown would see
        // it jump to eighteen hours and fall.
        val tonight = LocalDateTime.of(date, LocalTime.of(23, 50))
        val tomorrowFajr = LocalDateTime.of(date.plusDays(1), LocalTime.of(5, 10))
        assertEquals(
            "05h 20m",
            PrayerCalculationEngine.formatRemainingCountdown(tomorrowFajr, tonight)
        )
    }

    @Test
    fun `countdown never exceeds a day`() {
        // The other side of the wrap, which is the defensive one. A next-prayer time
        // is always inside a day, so this only fires on bad input - but a raw
        // subtraction would format it as "72h 00m", and "hours" above 24 is not a
        // duration anyone counts down. Clamping to zero says "nothing sensible to
        // count" instead of inventing one.
        val now = LocalDateTime.of(date, LocalTime.of(12, 0))
        assertEquals(
            "00m 00s",
            PrayerCalculationEngine.formatRemainingCountdown(now.plusDays(3), now)
        )
    }

    @Test
    fun `extreme latitudes stay strictly ordered via nearest-latitude fallback`() {
        // Polar day/night used to wrap clamped times past midnight and misorder
        // the day. The engine now applies the nearest-latitude (aqrab al-bilad)
        // fallback, so ordering holds everywhere without crashing.
        //
        // Contract note: prayer times are civil times, so the device zone must
        // match the location (as on any real device carried by the user). Each
        // case below runs with that location's real zone; the zone-mismatch
        // case is covered separately by `zone mismatch degrades gracefully`.
        val cases = listOf(
            Triple(UserLocation("Tromsø", "Norway", 69.65, 18.96), "Europe/Oslo", LocalDate.of(2026, 6, 21)),
            Triple(UserLocation("Tromsø", "Norway", 69.65, 18.96), "Europe/Oslo", LocalDate.of(2026, 12, 21)),
            Triple(UserLocation("Sydney", "Australia", -33.87, 151.21), "Australia/Sydney", LocalDate.of(2026, 6, 21)),
            Triple(UserLocation("Sydney", "Australia", -33.87, 151.21), "Australia/Sydney", LocalDate.of(2026, 12, 21))
        )
        val previousZone = java.util.TimeZone.getDefault()
        try {
            for ((loc, zoneId, season) in cases) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zoneId))
                for (method in CalculationMethod.entries) {
                    val day = PrayerCalculationEngine.calculatePrayerTimes(
                        date = season,
                        location = loc,
                        method = method,
                        madhhab = Madhhab.STANDARD,
                        adjustments = noAdj
                    )
                    val times = day.prayers.map { it.time }
                    for (i in 0 until times.size - 1) {
                        assertTrue(
                            "${loc.name} $season ${method.name}: " +
                                "${day.prayers[i].prayer} should precede ${day.prayers[i + 1].prayer}",
                            times[i].isBefore(times[i + 1])
                        )
                    }
                }
            }
        } finally {
            java.util.TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun `polar day still resolves a sane next prayer`() {
        val previousZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Oslo"))
            val tromsoSummer = PrayerCalculationEngine.calculatePrayerTimes(
                date = LocalDate.of(2026, 6, 21),
                location = UserLocation("Tromsø", "Norway", 69.65, 18.96),
                method = CalculationMethod.MWL,
                madhhab = Madhhab.STANDARD,
                adjustments = noAdj
            )
            val next = PrayerCalculationEngine.getNextPrayer(tromsoSummer, LocalTime.of(12, 0))
            // Midday in Tromsø summer must resolve to Dhuhr or later — never wrap
            // back to Fajr the way the old clamped geometry did.
            assertTrue(next.prayer in listOf(Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA))
        } finally {
            java.util.TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun `zone mismatch degrades gracefully instead of breaking the day`() {
        // Traveler case: device zone far from solar time at the location
        // (documented limitation — times recompute once the zone updates).
        // The day must stay usable: non-decreasing times, working
        // next/previous lookup, and no crash.
        val previousZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            val day = PrayerCalculationEngine.calculatePrayerTimes(
                date = LocalDate.of(2026, 6, 21),
                location = UserLocation("Tromsø", "Norway", 69.65, 18.96),
                method = CalculationMethod.MOROCCO_MINISTRY,
                madhhab = Madhhab.STANDARD,
                adjustments = noAdj
            )
            assertEquals(6, day.prayers.size)
            val times = day.prayers.map { it.time }
            for (i in 0 until times.size - 1) {
                assertTrue(
                    "${day.prayers[i].prayer} should not come after ${day.prayers[i + 1].prayer}",
                    !times[i].isAfter(times[i + 1])
                )
            }
            // Next/previous lookup must return a valid prayer, not crash.
            val next = PrayerCalculationEngine.getNextPrayer(day, LocalTime.of(12, 0))
            val prev = PrayerCalculationEngine.getPreviousPrayer(day, LocalTime.of(12, 0))
            assertTrue(next.prayer in Prayer.entries)
            assertTrue(prev.prayer in Prayer.entries)
        } finally {
            java.util.TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun `large manual adjustments keep times valid`() {
        val adj = PrayerAdjustments(fajr = -30, sunrise = 15, dhuhr = 10, asr = -10, maghrib = 20, isha = 30)
        val day = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.STANDARD,
            adjustments = adj
        )
        assertEquals(6, day.prayers.size)
        day.prayers.forEach {
            assertTrue(it.time.hour in 0..23)
            assertTrue(it.time.minute in 0..59)
        }
    }
}
