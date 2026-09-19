package com.example.engine

import com.example.data.model.UserLocation
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class SunPosition(
    val azimuth: Float, // 0 to 360 degrees clockwise from True North
    val altitude: Float, // degrees above horizon (-90 to +90)
    val isSunVisible: Boolean
)

object QiblaEngine {

    const val KAABA_LAT = 21.4224779
    const val KAABA_LNG = 39.8261818

    /**
     * Calculates the Great Circle bearing towards the Holy Kaaba in Mecca
     * from any latitude and longitude in degrees clockwise from True North (0..360).
     */
    fun calculateQiblaBearing(lat: Double, lng: Double): Float {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LNG - lng)

        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)

        var qibla = Math.toDegrees(atan2(y, x))
        qibla = (qibla + 360.0) % 360.0
        return qibla.toFloat()
    }

    /**
     * Calculates the approximate distance to Kaaba in kilometers.
     */
    fun calculateDistanceToKaabaKm(lat: Double, lng: Double): Int {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(KAABA_LAT - lat)
        val dLng = Math.toRadians(KAABA_LNG - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (r * c).toInt()
    }

    /**
     * Calculates the position of the Sun (Azimuth and Altitude) at the current time and location.
     * Used for the "Verify with Sun" feature.
     */
    fun calculateSunPosition(
        location: UserLocation,
        dateTime: LocalDateTime = LocalDateTime.now()
    ): SunPosition {
        val dayOfYear = dateTime.dayOfYear
        val hour = dateTime.hour + (dateTime.minute / 60.0) + (dateTime.second / 3600.0)
        val timeZoneOffset = try {
            val zId = ZoneId.systemDefault()
            zId.rules.getOffset(dateTime).totalSeconds / 3600.0
        } catch (e: Exception) {
            location.longitude / 15.0
        }

        // Fractional year in radians
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1 + (hour - 12) / 24.0)

        // Equation of time in minutes
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        // Solar declination angle in radians
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        // True solar time in minutes
        val timeOffset = eqTime + 4 * location.longitude - 60 * timeZoneOffset
        val tst = hour * 60 + timeOffset

        // Solar hour angle in degrees
        var ha = (tst / 4) - 180
        if (ha < -180) ha += 360
        val haRad = Math.toRadians(ha)

        val latRad = Math.toRadians(location.latitude)

        // Solar zenith angle
        val cosZenith = sin(latRad) * sin(decl) + cos(latRad) * cos(decl) * cos(haRad)
        val zenith = acos(cosZenith.coerceIn(-1.0, 1.0))
        val altitude = (90.0 - Math.toDegrees(zenith)).toFloat()

        // Solar azimuth angle (degrees clockwise from North)
        val cosAzimuth = (sin(decl) - sin(latRad) * cosZenith) / (cos(latRad) * sin(zenith))
        var azimuth = Math.toDegrees(acos(cosAzimuth.coerceIn(-1.0, 1.0)))
        if (ha > 0) {
            azimuth = 360.0 - azimuth
        }
        val finalAzimuth = ((azimuth + 360.0) % 360.0).toFloat()

        return SunPosition(
            azimuth = finalAzimuth,
            altitude = altitude,
            isSunVisible = altitude > -0.833f
        )
    }

    /**
     * Calculates relative angle from current device heading to target bearing.
     * Returns a signed angle in degrees [-180, +180]:
     * Negative means the target is to the LEFT.
     * Positive means the target is to the RIGHT.
     * 0 means directly centered ahead.
     */
    fun calculateRelativeAngle(currentHeading: Float, targetBearing: Float): Float {
        var diff = (targetBearing - currentHeading) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    /**
     * Evaluates magnetic field flux strength in microteslas (µT).
     * Typical ambient Earth magnetic field on surface is 25 to 65 µT.
     */
    fun evaluateMagneticField(magnitudeMicroTesla: Float): MagneticFieldStatus {
        return when {
            magnitudeMicroTesla <= 0f -> MagneticFieldStatus.UNAVAILABLE
            magnitudeMicroTesla < 22f -> MagneticFieldStatus.WEAK
            magnitudeMicroTesla in 22f..68f -> MagneticFieldStatus.OPTIMAL
            magnitudeMicroTesla in 68f..85f -> MagneticFieldStatus.ELEVATED
            else -> MagneticFieldStatus.INTERFERENCE
        }
    }
}

enum class MagneticFieldStatus(val label: String, val isGood: Boolean) {
    UNAVAILABLE("Sensor Unavailable", false),
    WEAK("Weak Field (<25µT)", false),
    OPTIMAL("Optimal (~45µT)", true),
    ELEVATED("Elevated Flux", true),
    INTERFERENCE("Metal Interference!", false)
}
