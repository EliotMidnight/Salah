package com.example.data.remote

import android.content.Context
import com.example.data.model.Prayer
import com.example.data.model.UserLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The one published month this device has adopted, kept on disk.
 *
 * Only the matched city is stored, not the 190-city feed: it is about 2 KB of
 * preferences instead of 290 KB, and the app never needs the other 189 rows.
 * The trade is that moving to another city costs a re-fetch, which is why
 * [read] also checks how far the stored selection was made from where the
 * reader is now - a stale city's timetable must not survive a move, offline or
 * not.
 *
 * It stores the clock it was aligned to as well. The rows are shifted from the
 * feed's `fuseau` onto the reader's clock at fetch time; if the device's zone
 * then changes - a flight, or a manual change while stationary - the stored
 * shift is an hour out and must not be reused. A zone change is a miss, exactly
 * like a move.
 *
 * The format is plain text on purpose. It is written and read by this file
 * only, it has to survive a process death, and a corrupt cache must be a
 * *miss* - never a half-read table.
 */
object HabousStore {

    private const val KEY = "habous_official_month"
    private const val FORMAT = "v2"

    /** A month is only reused while the reader stays this close to where it was chosen. */
    private const val MAX_DISTANCE_KM = 30.0

    fun read(context: Context, location: UserLocation, zoneId: ZoneId): OfficialMonth? {
        val raw = prefs(context).getString(KEY, null) ?: return null
        return try {
            // Written with a trailing newline per row, so drop the empty tail
            // rather than parsing it as a day with no times. Header layout:
            // format, year, month, code, name, shift, zone, lat, lon.
            val lines = raw.split('\n').filter { it.isNotEmpty() }
            if (lines.size < 10 || lines[0] != FORMAT) return null
            if (lines[6] != zoneId.id) return null
            val storedLat = lines[7].toDouble()
            val storedLon = lines[8].toDouble()
            if (distanceKm(location.latitude, location.longitude, storedLat, storedLon) > MAX_DISTANCE_KM) {
                return null
            }
            val days = lines.drop(9).map { line ->
                val cells = line.split(',')
                if (cells.size != Prayer.entries.size) {
                    throw IllegalArgumentException("${cells.size} times in a day")
                }
                cells.map { LocalTime.parse(it) }
            }
            OfficialMonth(
                year = lines[1].toInt(),
                month = lines[2].toInt(),
                cityCode = lines[3].toInt(),
                cityName = lines[4],
                clockShiftMinutes = lines[5].toInt(),
                days = days
            )
        } catch (_: Exception) {
            null
        }
    }

    fun write(
        context: Context,
        month: OfficialMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ) {
        val body = buildString {
            append(FORMAT).append('\n')
            append(month.year).append('\n')
            append(month.month).append('\n')
            append(month.cityCode).append('\n')
            append(month.cityName).append('\n')
            append(month.clockShiftMinutes).append('\n')
            append(zoneId.id).append('\n')
            append(latitude).append('\n')
            append(longitude).append('\n')
            month.days.forEach { row ->
                append(row.joinToString(",") { it.toString() }).append('\n')
            }
        }
        prefs(context).edit().putString(KEY, body).apply()
    }

    /** The stored month, if it covers [date] for [location] in [zoneId]'s clock. */
    fun readFor(
        context: Context,
        date: LocalDate,
        location: UserLocation,
        zoneId: ZoneId
    ): OfficialMonth? = read(context, location, zoneId)
        ?.takeIf { it.year == date.year && it.month == date.monthValue }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)

    /** Great-circle distance, in kilometres. */
    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return earthKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
