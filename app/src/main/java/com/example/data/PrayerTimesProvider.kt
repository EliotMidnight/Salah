package com.example.data

import android.content.Context
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.data.remote.HabousStore
import com.example.engine.PrayerCalculationEngine
import java.time.LocalDate

/**
 * The one answer to "what time is Fajr on this date, here".
 *
 * Three consumers used to compute their own answer from the engine and the
 * preferences - the screen, the alarm scheduler, and the calendar - and any
 * disagreement between them is a bug the reader experiences as an adhan at the
 * wrong minute. There is now one entry point, and it prefers the ministry's
 * published row when the device holds one that describes this place, falling
 * back to the calculation otherwise.
 *
 * The calendar deliberately does not come through here: it asks for a month of
 * days outside the published month as often as inside it, and a grid that
 * switched conventions halfway down a month would be worse than a grid that is
 * a minute off the row above it.
 */
object PrayerTimesProvider {

    fun forDate(
        context: Context,
        date: LocalDate,
        location: UserLocation,
        method: CalculationMethod,
        madhhab: Madhhab,
        adjustments: PrayerAdjustments
    ): PrayerTimesDay {
        val calculated = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = location,
            method = method,
            madhhab = madhhab,
            adjustments = adjustments
        )

        if (!method.isMoroccoNationalTable) return calculated

        // The ministry's table is published with the standard (Shafi/Maliki)
        // shadow factor. A Hanafi reader asked for a different Asr, which no
        // published row can supply, so their day is calculated instead of
        // silently replacing their choice with the ministry's.
        if (madhhab != Madhhab.STANDARD) return calculated

        val published = HabousStore
            .readFor(context, date, location, calculated.zoneId)
            ?.timesOn(date)
            ?: return calculated

        // The published row is the ministry's raw announcement: it knows nothing
        // about this reader's per-prayer adjustments, which are a personal
        // correction on top of any timetable. Apply them here, or switching
        // online would silently discard the reader's settings. The row was
        // matched against an *unadjusted* calculation (see `HabousClient`), so
        // the adjustments do not affect which city is adopted either.
        val adjusted = published.mapValues { (prayer, time) ->
            time.plusMinutes(adjustments.forPrayer(prayer).toLong())
        }

        return PrayerCalculationEngine.assemble(
            date = date,
            times = adjusted,
            method = method,
            location = location,
            zoneId = calculated.zoneId
        )
    }
}
