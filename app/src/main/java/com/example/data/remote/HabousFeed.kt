package com.example.data.remote

import com.example.data.model.Prayer
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONObject

/** One city's month, as published. */
data class FeedCity(
    val code: Int,
    val name: String,
    /** One row per day of the month, six times each, in [Prayer] order. */
    val days: List<List<LocalTime>>
)

/** A published month, before it is narrowed to one place. */
data class FeedMonth(
    val year: Int,
    val month: Int,
    /** The clock the feed writes in, as the ministry labels it ("GMT"). */
    val zoneLabel: String,
    val cities: List<FeedCity>
)

/**
 * A published month narrowed to the timetable that belongs to one place, and
 * aligned to the reader's clock.
 *
 * @param clockShiftMinutes whole minutes to add to every published time to put
 *   it on the reader's clock. Zero for Morocco today; the field exists because
 *   the feed states its own `fuseau` and the app must not silently mix two
 *   clocks if the ministry ever publishes in another one.
 */
data class OfficialMonth(
    val year: Int,
    val month: Int,
    val cityCode: Int,
    val cityName: String,
    val clockShiftMinutes: Int,
    val days: List<List<LocalTime>>
) {
    fun timesOn(date: LocalDate): Map<Prayer, LocalTime>? {
        if (date.year != year || date.monthValue != month) return null
        val row = days.getOrNull(date.dayOfMonth - 1) ?: return null
        if (row.size != Prayer.entries.size) return null
        return Prayer.entries.mapIndexed { index, prayer ->
            prayer to row[index].plusMinutes(clockShiftMinutes.toLong())
        }.toMap()
    }
}

/**
 * The Ministry of Habous & Islamic Affairs' monthly timetable.
 *
 * The ministry publishes one file per month at
 * `habous.gov.ma/data/horaires-YYYY-MM.js`: 190 cities, one row of six times per
 * day, in the legal time of Morocco. The site's own script reads exactly this
 * file (`assets/js/prieres.js`), so it is the ministry's machine-readable
 * timetable rather than a scrape of its page.
 *
 * ### Why the app does not ask "which city am I in"
 *
 * Two cities 30 km apart announce times less than a minute apart, so the feed
 * carries no information that would let a GPS fix pick one of them, and the
 * ministry publishes no city coordinates to compare against. Guessing a city
 * from a name - or from a fitted latitude - was tried and misassigned Rabat to
 * Kénitra and Casablanca to Mohammedia.
 *
 * So the question is turned around. The app already computes the day from the
 * reader's own coordinates, to within a minute of the ministry's table. It then
 * asks: *is there a published row that agrees with that day?* If one is within
 * [TOLERANCE_MINUTES] on every prayer, that row is the ministry's announcement
 * for a place whose timetable is this place's timetable, and it is used as
 * published. If nothing agrees - a reader in Paris, or a row that is minutes
 * away but not on a whole-hour clock - nothing is used and the calculation
 * stands.
 *
 * The identity of the matched city is therefore not a claim the app makes; it
 * is not shown to the reader. What is guaranteed is the only thing that
 * matters: a row is adopted only when it is already within a few minutes of the
 * correct answer for where the reader is standing.
 */
object HabousFeed {

    /** Where the ministry keeps the monthly files. */
    const val BASE_URL = "https://www.habous.gov.ma/data/horaires-"

    /**
     * How far a published row may sit from the locally calculated day and still
     * be adopted.
     *
     * Measured, not chosen: across fourteen Moroccan cities, the ministry's own
     * row differs from the calculation by 0-3 minutes (Sunrise and Maghrib carry
     * the ministry's per-city horizon convention, which is why 3 and not 1).
     * Three keeps every real city inside the gate while still refusing a row
     * that would move a prayer by more than the calculation's own error.
     */
    const val TOLERANCE_MINUTES = 3

    /**
     * A whole-hour offset is the only shift allowed between the feed's clock
     * and the reader's: legal time differs by hours, never by five minutes.
     * Allowing smaller steps let the matcher "explain" a two-minute difference
     * with a minus-ten-minute clock shift and pick the wrong city.
     */
    private const val CLOCK_STEP_MINUTES = 60.0

    fun url(year: Int, month: Int): String =
        BASE_URL + String.format(Locale.ROOT, "%04d-%02d.js", year, month)

    /**
     * Parses the `.js` payload.
     *
     * The file is `(window.HORAIRES_MOIS=window.HORAIRES_MOIS||[]).push({...})`
     * and the object literal inside the call is plain JSON, so only the wrapper
     * has to be removed. Throws on anything unexpected: a caller that cannot
     * parse a month must fall back to calculating, not to half a table.
     */
    fun parse(payload: String): FeedMonth {
        val call = payload.indexOf(".push(")
        val json = if (call >= 0) {
            payload.substring(call + ".push(".length, payload.lastIndexOf(')'))
        } else {
            payload
        }
        val root = JSONObject(json)
        val array = root.getJSONArray("villes")
        val cities = ArrayList<FeedCity>(array.length())
        for (i in 0 until array.length()) {
            val city = array.getJSONObject(i)
            val rows = city.getJSONArray("h")
            val days = ArrayList<List<LocalTime>>(rows.length())
            for (d in 0 until rows.length()) {
                val row = rows.getJSONArray(d)
                if (row.length() != Prayer.entries.size) {
                    throw IllegalArgumentException("city ${city.getInt("c")} day ${d + 1}: ${row.length()} times")
                }
                days += (0 until Prayer.entries.size).map { LocalTime.parse(row.getString(it)) }
            }
            cities += FeedCity(city.getInt("c"), city.getString("n"), days)
        }
        return FeedMonth(
            year = root.getInt("annee"),
            month = root.getInt("mois"),
            zoneLabel = root.optString("fuseau", ""),
            cities = cities
        )
    }

    /**
     * The published month that belongs to [reference], or null if none is close
     * enough.
     *
     * @param reference the day the app would show from its own calculation -
     *   twelve months of it are not needed, one is enough to identify the row,
     *   and the whole city is then carried for the rest of the month.
     */
    fun select(feed: FeedMonth, date: LocalDate, reference: Map<Prayer, LocalTime>): OfficialMonth? {
        if (date.year != feed.year || date.monthValue != feed.month) return null
        val dayIndex = date.dayOfMonth - 1
        val wanted = Prayer.entries.map { reference.getValue(it) }

        var bestResidual = Int.MAX_VALUE
        var bestCity: FeedCity? = null
        var bestShift = 0

        for (city in feed.cities) {
            val row = city.days.getOrNull(dayIndex) ?: continue
            val deltas = Prayer.entries.mapIndexed { index, _ ->
                signedMinutes(row[index], wanted[index])
            }
            val shift = (median(deltas) / CLOCK_STEP_MINUTES).roundToInt() *
                    CLOCK_STEP_MINUTES.toInt()
            val residual = deltas.maxOf { abs(it - shift) }
            if (residual < bestResidual) {
                bestResidual = residual
                bestCity = city
                bestShift = shift
            }
        }

        val city = bestCity ?: return null
        if (bestResidual > TOLERANCE_MINUTES) return null
        return OfficialMonth(
            year = feed.year,
            month = feed.month,
            cityCode = city.code,
            cityName = city.name,
            clockShiftMinutes = -bestShift,
            days = city.days
        )
    }

    /** Minutes from [from] to [to], folded into a half-day so midnight cannot flip the sign. */
    private fun signedMinutes(to: LocalTime, from: LocalTime): Int {
        var delta = to.hour * 60 + to.minute - (from.hour * 60 + from.minute)
        if (delta > 720) delta -= 1440
        if (delta < -720) delta += 1440
        return delta
    }

    private fun median(values: List<Int>): Double {
        val sorted = values.sorted()
        return (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    }
}
