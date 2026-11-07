package com.example.engine

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
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

    // ---------------------------------------------------------------------
    // Morocco, against the ministry's own published table
    // ---------------------------------------------------------------------

    /**
     * The Ministry of Habous feed for Rabat, city code 1, November 2026
     * (`habous.gov.ma/data/horaires-2026-11.js`, `fuseau: GMT`), one line per
     * day: "day Fajr Sunrise Dhuhr Asr Maghrib Isha".
     *
     * This is transcribed data, not a computation: it is the table the mosque
     * prints, and the thing a reader in Rabat will compare the app against.
     */
    private val RABAT_NOVEMBER_2026 = listOf(
        "01 05:17 06:44 12:16 15:12 17:39 18:54",
        "02 05:18 06:45 12:16 15:11 17:38 18:54",
        "03 05:18 06:46 12:16 15:11 17:37 18:53",
        "04 05:19 06:47 12:16 15:10 17:36 18:52",
        "05 05:20 06:47 12:16 15:09 17:35 18:51",
        "06 05:21 06:48 12:16 15:09 17:34 18:51",
        "07 05:22 06:49 12:16 15:08 17:33 18:50",
        "08 05:22 06:50 12:16 15:07 17:33 18:49",
        "09 05:23 06:51 12:16 15:07 17:32 18:49",
        "10 05:24 06:52 12:16 15:06 17:31 18:48",
        "11 05:25 06:53 12:16 15:06 17:30 18:47",
        "12 05:26 06:54 12:17 15:05 17:30 18:47",
        "13 05:26 06:55 12:17 15:05 17:29 18:46",
        "14 05:27 06:56 12:17 15:04 17:28 18:46",
        "15 05:28 06:57 12:17 15:04 17:28 18:45",
        "16 05:29 06:58 12:17 15:03 17:27 18:45",
        "17 05:30 06:59 12:17 15:03 17:27 18:44",
        "18 05:30 07:00 12:18 15:02 17:26 18:44",
        "19 05:31 07:00 12:18 15:02 17:26 18:44",
        "20 05:32 07:01 12:18 15:02 17:25 18:43",
        "21 05:33 07:02 12:18 15:01 17:25 18:43",
        "22 05:34 07:03 12:19 15:01 17:24 18:43",
        "23 05:34 07:04 12:19 15:01 17:24 18:43",
        "24 05:35 07:05 12:19 15:01 17:24 18:42",
        "25 05:36 07:06 12:19 15:00 17:23 18:42",
        "26 05:37 07:07 12:20 15:00 17:23 18:42",
        "27 05:38 07:08 12:20 15:00 17:23 18:42",
        "28 05:38 07:09 12:20 15:00 17:23 18:42",
        "29 05:39 07:10 12:21 15:00 17:22 18:42",
        "30 05:40 07:11 12:21 15:00 17:22 18:42"
    )

    private fun rabat(date: LocalDate) = PrayerCalculationEngine.calculatePrayerTimes(
        date = date,
        location = UserLocation.RABAT,
        method = CalculationMethod.MOROCCO_MINISTRY,
        madhhab = Madhhab.STANDARD,
        adjustments = noAdj,
        zoneId = PrayerTimeZone.forDay(date, ZoneId.of("Africa/Casablanca"))
    )

    @Test
    fun `Rabat matches the ministry's published table`() {
        // The app used to answer 13:16 for Dhuhr on these days: the whole day ran
        // on a hard-coded UTC+1 for any location whose country string contained
        // "Morocco", which was true until decree 2.26.530 put the country on GMT
        // on 20 September 2026.
        val day = rabat(LocalDate.parse("2026-11-01"))
        assertEquals(
            listOf("05:17", "06:44", "12:16", "15:12", "17:39", "18:54"),
            day.prayers.map { it.time.toString() }
        )
        assertEquals(ZoneOffset.UTC, day.zoneId)
    }

    /**
     * The whole month, not a sample: every prayer on every day is within a
     * minute of the published table.
     *
     * One minute is the floor, and it is not a defect to be tuned away. The
     * ministry computes each city from a reference point it does not publish;
     * our coordinates for Rabat are the city centre, which is a few hundred
     * metres - and occasionally one rounding minute - away from theirs. What
     * must never come back is the hour: a wrong zone offset is 60 minutes, and
     * that is the failure this pins.
     */
    @Test
    fun `the whole of November is within a minute of the published table`() {
        for (line in RABAT_NOVEMBER_2026) {
            val cells = line.split(" ")
            val date = LocalDate.parse("2026-11-${cells[0]}")
            val official = cells.drop(1)
            val calculated = rabat(date).prayers.map { it.time }
            for (i in official.indices) {
                val expected = LocalTime.parse(official[i])
                val actual = calculated[i]
                val deltaMinutes = java.time.Duration.between(expected, actual).toMinutes()
                assertTrue(
                    "$date ${Prayer.entries[i]}: published ${official[i]}, calculated $actual",
                    kotlin.math.abs(deltaMinutes) <= 1
                )
            }
        }
    }

    @Test
    fun `morocco is on GMT from the 2026 decree, and on its own rules before it`() {
        val casablanca = ZoneId.of("Africa/Casablanca")
        // The decree: GMT from 20 September 2026 onwards.
        assertEquals(ZoneOffset.UTC, PrayerTimeZone.forDay(LocalDate.of(2026, 11, 1), casablanca))
        assertEquals(ZoneOffset.UTC, PrayerTimeZone.forDay(LocalDate.of(2026, 9, 20), casablanca))
        assertEquals(ZoneOffset.UTC, PrayerTimeZone.forDay(LocalDate.of(2026, 9, 20), ZoneId.of("Africa/El_Aaiun")))
        // The day before, the zone's own rules are right and are left alone.
        assertEquals(casablanca, PrayerTimeZone.forDay(LocalDate.of(2026, 9, 19), casablanca))
        // Any other zone is trusted as-is - the reader's status-bar clock.
        assertEquals(ZoneId.of("Europe/London"), PrayerTimeZone.forDay(LocalDate.of(2026, 11, 1), ZoneId.of("Europe/London")))
    }

    @Test
    fun `the ministry's margins are stated once and applied`() {
        val morocco = CalculationMethod.MOROCCO_MINISTRY
        assertEquals("sunrise", -3, morocco.sunriseOffsetMinutes)
        assertEquals("dhuhr", 5, morocco.dhuhrOffsetMinutes)
        assertEquals("maghrib", 4, morocco.maghribOffsetMinutes)
        // No other convention carries a margin: those numbers are the Moroccan
        // ministry's announcement practice, not something astronomical.
        for (method in CalculationMethod.entries - morocco) {
            assertEquals(method.name, 0, method.sunriseOffsetMinutes)
            assertEquals(method.name, 0, method.dhuhrOffsetMinutes)
            assertEquals(method.name, 0, method.maghribOffsetMinutes)
        }
    }

    /**
     * The sun is evaluated *at* each prayer, not once at midnight.
     *
     * Reykjavík in late March is where that matters: the declination moves
     * ~0.4°/day, and at 64°N a 0.4° error in the twilight geometry is several
     * minutes of clock time. A frozen-sun implementation answers
     * 04:12 / 07:12 / 13:34 / 16:42 / 19:56 / 22:42 for this day; an
     * independent service (Aladhan, MWL) answers
     * 04:11 / 07:11 / 13:34 / 16:41 / 19:58 / 22:46.
     */
    @Test
    fun `high latitude day uses the sun position at each prayer`() {
        val day = PrayerCalculationEngine.calculatePrayerTimes(
            date = LocalDate.of(2026, 3, 25),
            location = UserLocation("Reykjavik", "Iceland", 64.1466, -21.9426),
            method = CalculationMethod.MWL,
            madhhab = Madhhab.STANDARD,
            adjustments = noAdj,
            zoneId = ZoneId.of("Atlantic/Reykjavik")
        )
        assertEquals(
            listOf("04:11", "07:11", "13:34", "16:43", "19:58", "22:47"),
            day.prayers.map { it.time.toString() }
        )
    }
}
