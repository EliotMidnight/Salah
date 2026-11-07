package com.example.data.remote

import androidx.test.core.app.ApplicationProvider
import com.example.data.PrayerTimesProvider
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.PrayerCalculationEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The ministry feed: its wrapper, its rows, and the gate that decides whether a
 * published row may replace the calculation.
 *
 * Robolectric because `org.json` is part of the Android framework - the same
 * implementation the app will use, rather than a test-only stand-in that could
 * disagree with it.
 */
@RunWith(RobolectricTestRunner::class)
class HabousFeedTest {

    /**
     * Two cities, two days, in the ministry's own shape: a `push({...})` wrapper
     * around a JSON object, `c` for code, `n` for the Arabic name, `h` for the
     * rows of six times.
     */
    private val payload = """
        (window.HORAIRES_MOIS=window.HORAIRES_MOIS||[]).push({"annee":2026,"mois":11,"fuseau":"GMT",
        "cols":["Fajr","Chorouq","Dhuhr","Asr","Maghrib","Ishae"],"villes":[
        {"c":1,"n":"الرباط","h":[["05:17","06:44","12:16","15:12","17:39","18:54"],["05:18","06:45","12:16","15:11","17:38","18:54"]]},
        {"c":2,"n":"الخميسات","h":[["05:20","06:47","12:19","15:15","17:42","18:57"],["05:21","06:48","12:19","15:14","17:41","18:57"]]}]});
    """.trimIndent()

    private fun reference(vararg times: String): Map<Prayer, LocalTime> {
        assertEquals(Prayer.entries.size, times.size)
        return Prayer.entries.mapIndexed { index, prayer -> prayer to LocalTime.parse(times[index]) }.toMap()
    }

    @Test
    fun `parses the ministry's push wrapper and its rows`() {
        val feed = HabousFeed.parse(payload)
        assertEquals(2026, feed.year)
        assertEquals(11, feed.month)
        assertEquals("GMT", feed.zoneLabel)
        assertEquals(listOf(1, 2), feed.cities.map { it.code })
        assertEquals("الرباط", feed.cities[0].name)
        assertEquals(LocalTime.of(5, 17), feed.cities[0].days[0][Prayer.FAJR.ordinal])
        assertEquals(LocalTime.of(18, 57), feed.cities[1].days[1][Prayer.ISHA.ordinal])
    }

    @Test
    fun `adopts the row that agrees with the calculated day`() {
        val selected = HabousFeed.select(
            feed = HabousFeed.parse(payload),
            date = LocalDate.of(2026, 11, 1),
            reference = reference("05:17", "06:44", "12:16", "15:12", "17:39", "18:54")
        )
        assertEquals("الرباط", selected?.cityName)
        assertEquals(0, selected?.clockShiftMinutes)
        assertEquals(
            LocalTime.of(18, 54),
            selected?.timesOn(LocalDate.of(2026, 11, 1))?.get(Prayer.ISHA)
        )
    }

    @Test
    fun `refuses a difference that is not a whole-hour clock shift`() {
        // A different city, or a calculation that disagrees for a non-clock
        // reason: the gap is minutes, not a time zone, so no published row may
        // be adopted. (A whole-hour difference is a clock difference and is
        // aligned instead - see the next test.)
        val selected = HabousFeed.select(
            feed = HabousFeed.parse(payload),
            date = LocalDate.of(2026, 11, 1),
            reference = reference("05:37", "07:04", "12:36", "15:32", "17:59", "19:14")
        )
        assertNull(selected)
    }

    @Test
    fun `aligns a whole-hour clock difference instead of rejecting it`() {
        // The feed states its own `fuseau`. If the ministry ever publishes in
        // UTC+1 while the reader's Moroccan clock is UTC+0, the rows are an hour
        // ahead and must be shifted, not discarded - and the shift must be a
        // whole hour, never a handful of minutes that would let the matcher
        // "explain away" a genuinely different city.
        val selected = HabousFeed.select(
            feed = HabousFeed.parse(payload),
            date = LocalDate.of(2026, 11, 1),
            reference = reference("04:17", "05:44", "11:16", "14:12", "16:39", "17:54")
        )
        assertEquals("الرباط", selected?.cityName)
        assertEquals(-60, selected?.clockShiftMinutes)
        assertEquals(
            // The row is published an hour ahead; the adopted day is on the
            // reader's clock, so it is the reference, not the raw row.
            LocalTime.of(4, 17),
            selected?.timesOn(LocalDate.of(2026, 11, 1))?.get(Prayer.FAJR)
        )
    }

    @Test
    fun `a month outside the feed selects nothing`() {
        assertNull(
            HabousFeed.select(
                feed = HabousFeed.parse(payload),
                date = LocalDate.of(2026, 12, 1),
                reference = reference("05:17", "06:44", "12:16", "15:12", "17:39", "18:54")
            )
        )
    }

    @Test
    fun `stores a month and refuses it after a move`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val month = OfficialMonth(
            year = 2026,
            month = 11,
            cityCode = 1,
            cityName = "الرباط",
            clockShiftMinutes = 0,
            days = listOf(List(Prayer.entries.size) { LocalTime.of(5, 17) })
        )
        HabousStore.write(
            context = context,
            month = month,
            latitude = UserLocation.RABAT.latitude,
            longitude = UserLocation.RABAT.longitude,
            zoneId = ZoneOffset.UTC
        )

        val stored = HabousStore.readFor(
            context, LocalDate.of(2026, 11, 1), UserLocation.RABAT, ZoneOffset.UTC
        )
        assertEquals("الرباط", stored?.cityName)
        assertNull(
            "another month is not covered",
            HabousStore.readFor(context, LocalDate.of(2026, 12, 1), UserLocation.RABAT, ZoneOffset.UTC)
        )

        // Marrakech is 200 km away: the row was chosen for Rabat and must not
        // survive the move, or a reader would get the wrong city's timetable
        // until the next successful fetch - possibly never, offline.
        assertNull(
            HabousStore.readFor(context, LocalDate.of(2026, 11, 1), UserLocation.MARRAKECH, ZoneOffset.UTC)
        )

        // The stored rows were shifted onto UTC at fetch time. A device that
        // later reports another zone needs the same times on another clock, and
        // reusing the stored shift would move every prayer by the difference.
        assertNull(
            HabousStore.readFor(
                context, LocalDate.of(2026, 11, 1), UserLocation.RABAT, ZoneId.of("Europe/London")
            )
        )
    }

    @Test
    fun `provider applies the reader's adjustments on top of the published row`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // The published row is the ministry's announcement, not this reader's
        // correction to it. Turning the feed on must not quietly zero the six
        // sliders in Settings.
        val published = listOf("05:17", "06:44", "12:16", "15:12", "17:39", "18:54")
            .map { LocalTime.parse(it) }
        HabousStore.write(
            context = context,
            month = OfficialMonth(
                year = 2026,
                month = 11,
                cityCode = 1,
                cityName = "الرباط",
                clockShiftMinutes = 0,
                days = listOf(published)
            ),
            latitude = UserLocation.RABAT.latitude,
            longitude = UserLocation.RABAT.longitude,
            zoneId = ZoneId.systemDefault()
        )

        val day = PrayerTimesProvider.forDate(
            context = context,
            date = LocalDate.of(2026, 11, 1),
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.STANDARD,
            adjustments = PrayerAdjustments(fajr = 10, isha = -5)
        )

        assertEquals(LocalTime.of(5, 27), day.prayers[Prayer.FAJR.ordinal].time)
        assertEquals(LocalTime.of(18, 49), day.prayers[Prayer.ISHA.ordinal].time)
        // Untouched prayers keep the published minute.
        assertEquals(LocalTime.of(12, 16), day.prayers[Prayer.DHUHR.ordinal].time)
    }

    @Test
    fun `provider leaves a Hanafi reader on the calculated Asr`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val published = listOf("05:17", "06:44", "12:16", "15:12", "17:39", "18:54")
            .map { LocalTime.parse(it) }
        HabousStore.write(
            context = context,
            month = OfficialMonth(2026, 11, 1, "الرباط", 0, listOf(published)),
            latitude = UserLocation.RABAT.latitude,
            longitude = UserLocation.RABAT.longitude,
            zoneId = ZoneId.systemDefault()
        )

        val date = LocalDate.of(2026, 11, 1)
        val day = PrayerTimesProvider.forDate(
            context = context,
            date = date,
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.HANAFI,
            adjustments = PrayerAdjustments()
        )
        val calculated = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.HANAFI,
            adjustments = PrayerAdjustments()
        )

        // The ministry publishes a standard-Asr table. Adopting it would make
        // the Hanafi setting a no-op for the one prayer it changes.
        assertEquals(calculated.prayers.map { it.time }, day.prayers.map { it.time })
    }

    @Test
    fun `a corrupt cache is a miss, not half a table`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("salah_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("habous_official_month", "v2\n2026\n11\nnot-a-number\nx\n0\nUTC\n1\n2\n")
            .commit()
        assertNull(HabousStore.read(context, UserLocation.RABAT, ZoneOffset.UTC))
    }
}
