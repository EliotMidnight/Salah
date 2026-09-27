package com.example.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.data.model.Prayer
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Geometry for the 24-hour clock face, in the webapp's 0..100 viewBox space.
 *
 * The clock is a day, not a face: it runs clockwise from Maghrib at the top
 * through Isha, the night, Fajr, Sunrise, Dhuhr and Asr, and back to Maghrib.
 * That is why every angle here is measured from Maghrib rather than from
 * midnight - a clock anchored at 00:00 would put Dhuhr at the bottom and hide
 * the night behind the status bar.
 *
 * Angles are degrees, 0 = 12 o'clock, increasing clockwise. Compose's `drawArc`
 * measures from 3 o'clock, so [arcDegrees] does that one conversion.
 */
internal object ClockGeometry {

    const val CENTRE = 50f

    /** Hours elapsed since midnight, with minutes and seconds as fractions. */
    fun hoursOf(time: LocalTime): Float =
        time.hour + time.minute / 60f + time.second / 3600f

    /**
     * Position of [time] on the clock face, 0..360, where 0 is Maghrib.
     *
     * A null or missing time lands on 0 rather than throwing, so a day with an
     * incomplete timetable still renders every marker.
     */
    fun angleOf(time: LocalTime?, maghribHour: Float): Float {
        if (time == null) return 0f
        val offset = ((hoursOf(time) - maghribHour) % 24f + 24f) % 24f
        return offset / 24f * 360f
    }

    /** Angle of the current moment, for the sweeping hand. */
    fun angleOfNow(now: LocalTime, maghribHour: Float): Float = angleOf(now, maghribHour)

    /**
     * A point on the clock at [angleDegrees] and [radius] from the centre.
     *
     * The 90-degree shift is what puts 0 at the top: at 0 degrees a unit circle
     * point is at 3 o'clock, so the whole dial is rotated a quarter turn.
     */
    fun position(angleDegrees: Float, radius: Float): Offset {
        val radians = Math.toRadians((angleDegrees - 90f).toDouble())
        return Offset(
            x = CENTRE + radius * cos(radians).toFloat(),
            y = CENTRE + radius * sin(radians).toFloat()
        )
    }

    /**
     * A label anchor on an ellipse of [rx] by [ry], at [angleDegrees].
     *
     * Used for the text around the clock: the ring is a circle, but the labels
     * around it cannot be, or the ones at 3 and 9 o'clock fall off the screen.
     */
    fun labelPosition(angleDegrees: Float, rx: Float, ry: Float): Offset {
        val point = position(angleDegrees, 1f)
        return Offset(
            x = CENTRE + (point.x - CENTRE) * rx,
            y = CENTRE + (point.y - CENTRE) * ry
        )
    }

    /**
     * Convert a webapp angle (0 = top) into Compose's `drawArc` convention
     * (0 = 3 o'clock, positive sweep clockwise).
     */
    fun arcDegrees(angleDegrees: Float): Float = angleDegrees - 90f

    /**
     * Clockwise sweep from [startAngle] to [endAngle], both in webapp angles.
     *
     * Normalised into 0..360 because most of the windows wrap past midnight -
     * the last third of the night runs from about 22:00 round to Fajr - and a
     * raw subtraction would hand `drawArc` a negative sweep, which draws
     * backwards from the wrong end.
     */
    fun sweepFromTo(startAngle: Float, endAngle: Float): Float =
        ((endAngle - startAngle) % 360f + 360f) % 360f

    /** The box a circle of [radius] occupies, centred on the dial. */
    fun circleBox(radius: Float) = Offset(CENTRE - radius, CENTRE - radius) to Size(radius * 2, radius * 2)

    /**
     * A window on the clock face.
     *
     * Carries the hour bounds as well as the angles because "am I inside this
     * window" is a question about the clock, and back-solving the hours out of
     * the angles is how that question gets answered wrongly.
     */
    data class NightSegment(
        val startAngle: Float,
        val endAngle: Float,
        val startHour: Float,
        val endHour: Float
    ) {
        fun contains(nowHour: Float): Boolean {
            if (endHour < startHour) return nowHour >= startHour || nowHour < endHour
            return nowHour >= startHour && nowHour < endHour
        }
    }

    /** Last third of the night: the 2/3 mark of Maghrib -> Fajr through to Fajr. */
    fun lastThirdOfNight(maghrib: LocalTime?, fajr: LocalTime?): NightSegment {
        val night = nightSpan(maghrib, fajr) ?: return EMPTY
        // Wrapped into 0..24 on purpose. "Is it 3am?" has to be answerable by
        // comparing hours, and an unwrapped 25.9 is not an hour of the day.
        val startHour = (night.first + night.second * 2f / 3f) % 24f
        val endHour = (night.first + night.second) % 24f
        return NightSegment(clockAngleOf(startHour), clockAngleOf(endHour), startHour, endHour)
    }

    /** End of the first third of the night - the Hanbali Isha time. */
    fun firstThirdEnd(maghrib: LocalTime?, fajr: LocalTime?): NightSegment {
        val night = nightSpan(maghrib, fajr) ?: return EMPTY
        val startHour = night.first % 24f
        val endHour = (night.first + night.second / 3f) % 24f
        return NightSegment(clockAngleOf(startHour), clockAngleOf(endHour), startHour, endHour)
    }

    /**
     * Duha: the morning window between sunrise and Dhuhr.
     *
     * Opens 20 minutes after sunrise and closes 15 minutes before Dhuhr, that
     * gap being zawal - the sun's exact meridian crossing, a time of makruh for
     * prayer, and deliberately excluded.
     */
    fun duha(sunrise: LocalTime?, dhuhr: LocalTime?): NightSegment {
        if (sunrise == null || dhuhr == null) return EMPTY
        return span(
            startHour = hoursOf(sunrise) + 20f / 60f,
            endHour = hoursOf(dhuhr) - 15f / 60f
        )
    }

    /**
     * Qaylula: the mid-day rest after Dhuhr.
     *
     * Opens 30 minutes after Dhuhr and closes 60% of the way to Asr.
     */
    fun qaylula(dhuhr: LocalTime?, asr: LocalTime?): NightSegment {
        if (dhuhr == null || asr == null) return EMPTY
        // The 60% is measured from Dhuhr, not from the window's start: Qaylula
        // ends two-fifths of the way from Asr back to Dhuhr, and measuring from
        // the start would stretch it by half an hour.
        val dhuhrHour = hoursOf(dhuhr)
        return span(
            startHour = dhuhrHour + 0.5f,
            endHour = dhuhrHour + (hoursOf(asr) - dhuhrHour) * 0.6f
        )
    }

    /** Angle for an hour-of-day, anchored so that Maghrib lands at 0. */
    private fun clockAngleOf(hourOfDay: Float, maghribHour: Float = 0f): Float =
        ((hourOfDay - maghribHour) % 24f + 24f) % 24f / 24f * 360f

    /** Maghrib -> Fajr as (start hour, duration in hours), with the end pushed past 24. */
    private fun nightSpan(maghrib: LocalTime?, fajr: LocalTime?): Pair<Float, Float>? {
        if (maghrib == null || fajr == null) return null
        val start = hoursOf(maghrib)
        var end = hoursOf(fajr)
        if (end < start) end += 24f
        return start to (end - start)
    }

    private fun span(startHour: Float, endHour: Float): NightSegment {
        val duration = ((endHour - startHour) % 24f + 24f) % 24f
        return NightSegment(
            startAngle = clockAngleOf(startHour % 24f),
            endAngle = clockAngleOf((startHour + duration) % 24f),
            startHour = startHour % 24f,
            endHour = (startHour + duration) % 24f
        )
    }

    private val EMPTY = NightSegment(0f, 0f, 0f, 0f)
}

/**
 * The prayers the clock places markers for, in the order they appear going
 * clockwise from Maghrib.
 *
 * Sunrise sits in the ring but is drawn as a diamond rather than a dot: it ends
 * a period rather than beginning a prayer, and a filled dot would claim it is
 * one of the five.
 */
internal val ClockMarkerOrder = listOf(
    Prayer.MAGHRIB,
    Prayer.ISHA,
    Prayer.FAJR,
    Prayer.SUNRISE,
    Prayer.DHUHR,
    Prayer.ASR
)

/** The prayers listed in day order on the simple view. */
internal val PrayerListOrder = listOf(
    Prayer.FAJR,
    Prayer.SUNRISE,
    Prayer.DHUHR,
    Prayer.ASR,
    Prayer.MAGHRIB,
    Prayer.ISHA
)

/**
 * The lit shape of a moon at [illumination], as a path in the glyph's own space.
 *
 * The terminator is an ellipse seen edge on: a straight line at half, matching
 * the outer circle at none or all. Building it as a real path rather than
 * subtracting a circle is what makes a 40%-lit moon look like a moon - the
 * subtraction approach the app's sky uses can only produce crescents and
 * gibbous, and it fakes the quarter phases.
 */
internal fun moonPhasePath(
    centre: Offset,
    radius: Float,
    illumination: Float,
    waxing: Boolean
): androidx.compose.ui.graphics.Path {
    // Rounded to 10% so the phase steps like a calendar instead of creeping.
    val phase = Math.round(illumination * 10.0).toFloat() / 10f
    val path = androidx.compose.ui.graphics.Path()

    if (phase <= 0.03f) return path

    val terminatorRx = abs(cos(Math.toRadians((phase * 180f).toDouble()))).toFloat() * radius
    val top = centre.y - radius
    val bottom = centre.y + radius

    // Right side lit when waxing, left when waning.
    val litOnRight = waxing
    path.moveTo(centre.x, top)
    // Outer limb, down the lit side.
    path.arcTo(
        rect = androidx.compose.ui.geometry.Rect(
            centre.x - radius, top, centre.x + radius, bottom
        ),
        startAngleDegrees = if (litOnRight) -90f else -90f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false
    )
    // Terminator, back up. It bulges into the lit side for a crescent and away
    // from it for a gibbous, which is the whole visual difference.
    val terminatorSweep = when {
        phase < 0.5f && litOnRight -> 180f
        phase < 0.5f -> -180f
        litOnRight -> -180f
        else -> 180f
    }
    val terminatorRect = androidx.compose.ui.geometry.Rect(
        centre.x - terminatorRx, top, centre.x + terminatorRx, bottom
    )
    path.arcTo(
        rect = terminatorRect,
        startAngleDegrees = if (litOnRight) 90f else -90f,
        sweepAngleDegrees = terminatorSweep,
        forceMoveTo = false
    )
    path.close()
    return path
}

/** Whole hours between two instants, for the "N hours until" readouts. */
internal fun hoursBetween(nowHour: Float, targetHour: Float): Float {
    val delta = targetHour - nowHour
    return if (delta < 0f) delta + 24f else delta
}

/** Rounds an hour-of-day down to the whole hour, for the 24 tick marks. */
internal fun wholeHourIndex(hourOfDay: Float): Int = floor(hourOfDay).toInt().coerceIn(0, 23)
