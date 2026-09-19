package com.example.engine

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.Prayer
import com.example.data.model.PrayerTimesDay
import java.time.LocalTime

enum class SkyPeriod(
    val title: String,
    val arabicTitle: String,
    val subtitle: String,
    val isNight: Boolean,
    val topColor: Color,
    val middleColor: Color,
    val horizonColor: Color,
    val accentColor: Color,
    val contentOnSkyColor: Color
) {
    FAJR_DAWN(
        title = "Dawn",
        arabicTitle = "الفجر",
        subtitle = "First light breaks on the horizon",
        isNight = false,
        topColor = Color(0xFF141936),
        middleColor = Color(0xFF28285C),
        horizonColor = Color(0xFF6B436D),
        accentColor = Color(0xFFFFD180),
        contentOnSkyColor = Color(0xFFFFFFFF)
    ),
    SUNRISE(
        title = "Sunrise",
        arabicTitle = "الشروق",
        subtitle = "The sun illuminates the world",
        isNight = false,
        topColor = Color(0xFF2E4068),
        middleColor = Color(0xFFC07062),
        horizonColor = Color(0xFFFFAE68),
        accentColor = Color(0xFFFFE082),
        contentOnSkyColor = Color(0xFFFFFFFF)
    ),
    MORNING(
        title = "Morning",
        arabicTitle = "الضحى",
        subtitle = "Quiet expanse of the morning sky",
        isNight = false,
        topColor = Color(0xFF286299),
        middleColor = Color(0xFF5B98C6),
        horizonColor = Color(0xFFBCE3F5),
        accentColor = Color(0xFFFFE082),
        contentOnSkyColor = Color(0xFF0F263B)
    ),
    DHUHR_MIDDAY(
        title = "Midday",
        arabicTitle = "الظهر",
        subtitle = "The sun reaches its zenith",
        isNight = false,
        topColor = Color(0xFF1E5F9E),
        middleColor = Color(0xFF4288C4),
        horizonColor = Color(0xFFA6D5F2),
        accentColor = Color(0xFFFFF9C4),
        contentOnSkyColor = Color(0xFF0C243C)
    ),
    ASR_AFTERNOON(
        title = "Afternoon",
        arabicTitle = "العصر",
        subtitle = "Golden shadows lengthen across the day",
        isNight = false,
        topColor = Color(0xFF2D507B),
        middleColor = Color(0xFF686C88),
        horizonColor = Color(0xFFE4A470),
        accentColor = Color(0xFFFFCC80),
        contentOnSkyColor = Color(0xFFFFFFFF)
    ),
    MAGHRIB_SUNSET(
        title = "Sunset",
        arabicTitle = "المغرب",
        subtitle = "The day folds into peaceful dusk",
        isNight = true,
        topColor = Color(0xFF17183B),
        middleColor = Color(0xFF5A2A47),
        horizonColor = Color(0xFFD46243),
        accentColor = Color(0xFFFFAB91),
        contentOnSkyColor = Color(0xFFFFFFFF)
    ),
    ISHA_TWILIGHT(
        title = "Twilight",
        arabicTitle = "العشاء",
        subtitle = "Deep twilight gives way to the cosmos",
        isNight = true,
        topColor = Color(0xFF0B1021),
        middleColor = Color(0xFF161F3D),
        horizonColor = Color(0xFF2D325A),
        accentColor = Color(0xFF90CAF9),
        contentOnSkyColor = Color(0xFFFFFFFF)
    ),
    NIGHT(
        title = "Night",
        arabicTitle = "الليل",
        subtitle = "Silent vigil under the starfield",
        isNight = true,
        topColor = Color(0xFF060914),
        middleColor = Color(0xFF0D1527),
        horizonColor = Color(0xFF151C33),
        accentColor = Color(0xFFCE93D8),
        contentOnSkyColor = Color(0xFFFFFFFF)
    );

    fun asBrush(): Brush {
        return Brush.verticalGradient(
            colors = listOf(topColor, middleColor, horizonColor)
        )
    }
}

data class MoonPhaseInfo(
    val day: Int,
    val illumination: Float, // 0.0 to 1.0
    val isWaxing: Boolean,
    val nameEn: String,
    val nameAr: String
)

data class SkyColorPalette(
    val zenithColor: Color,
    val midSkyColor: Color,
    val horizonColor: Color,
    val horizonHazeColor: Color,
    val sunColor: Color,
    val sunHaloColor: Color,
    val cloudTint: Color,
    val starAlpha: Float,
    val isNight: Boolean
)

object AstronomicalSky {

    fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * f,
            green = c1.green + (c2.green - c1.green) * f,
            blue = c1.blue + (c2.blue - c1.blue) * f,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * f
        )
    }

    fun getMoonPhaseInfo(hijriDay: Int): MoonPhaseInfo {
        val day = hijriDay.coerceIn(1, 30)
        val isWaxing = day <= 15
        val illumination = if (isWaxing) {
            (day - 1).toFloat() / 14.0f
        } else {
            (30 - day).toFloat() / 15.0f
        }.coerceIn(0f, 1f)

        val (nameEn, nameAr) = when (day) {
            1, 2, 3 -> Pair("Hilal (Waxing Crescent)", "هلال جديد")
            4, 5, 6 -> Pair("Waxing Crescent", "هلال متزايد")
            7, 8, 9 -> Pair("First Quarter", "التربيع الأول")
            10, 11, 12, 13 -> Pair("Waxing Gibbous", "أحدب متزايد")
            14, 15, 16 -> Pair("Badr (Full Moon)", "بدر كامل")
            17, 18, 19, 20 -> Pair("Waning Gibbous", "أحدب متناقص")
            21, 22, 23 -> Pair("Third Quarter", "التربيع الثاني")
            24, 25, 26, 27, 28 -> Pair("Waning Crescent", "هلال متناقص")
            else -> Pair("Muhāq (Dark Moon)", "محاق")
        }

        return MoonPhaseInfo(day, illumination, isWaxing, nameEn, nameAr)
    }

    /**
     * Calculates smooth, continuous sky gradient and atmospheric colors
     * based on exact solar altitude and whether sun is rising or setting.
     */
    fun calculateContinuousSkyColors(altitude: Float, isSetting: Boolean = false): SkyColorPalette {
        return when {
            altitude <= -18.0f -> {
                // Deep Astronomical Night: Starry cosmic void
                SkyColorPalette(
                    zenithColor = Color(0xFF04060E),
                    midSkyColor = Color(0xFF080C1E),
                    horizonColor = Color(0xFF10152B),
                    horizonHazeColor = Color(0x1A1B2440),
                    sunColor = Color(0xFFFF9800),
                    sunHaloColor = Color(0x00FF9800),
                    cloudTint = Color(0x22181F38),
                    starAlpha = 1.0f,
                    isNight = true
                )
            }
            altitude <= -12.0f -> {
                // Astronomical Twilight: Deep sapphire with faint horizon shift
                val t = (altitude - (-18.0f)) / 6.0f
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF04060E), Color(0xFF0A0F26), t),
                    midSkyColor = lerpColor(Color(0xFF080C1E), Color(0xFF131A3D), t),
                    horizonColor = lerpColor(Color(0xFF10152B), Color(0xFF261D42), t),
                    horizonHazeColor = Color(0x262A2B52),
                    sunColor = Color(0xFFFF9800),
                    sunHaloColor = Color(0x0AEC407A),
                    cloudTint = Color(0x2E242845),
                    starAlpha = 1.0f - (t * 0.25f),
                    isNight = true
                )
            }
            altitude <= -6.0f -> {
                // Nautical Twilight / Fajr / Maghrib Dusk: Violet-indigo with rich horizon glow
                val t = (altitude - (-12.0f)) / 6.0f
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF0A0F26), Color(0xFF141A38), t),
                    midSkyColor = lerpColor(Color(0xFF131A3D), Color(0xFF38234D), t),
                    horizonColor = lerpColor(Color(0xFF261D42), Color(0xFF7A3356), t),
                    horizonHazeColor = lerpColor(Color(0x262A2B52), Color(0x55C25562), t),
                    sunColor = Color(0xFFFF7043),
                    sunHaloColor = Color(0x26FF5722),
                    cloudTint = lerpColor(Color(0x2E242845), Color(0x559C385C), t),
                    starAlpha = (1.0f - t) * 0.75f,
                    isNight = false
                )
            }
            altitude <= 0.0f -> {
                // Civil Twilight: Fiery crimson, coral, magenta, and Belt of Venus
                val t = (altitude - (-6.0f)) / 6.0f
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF141A38), Color(0xFF202F54), t),
                    midSkyColor = lerpColor(Color(0xFF38234D), Color(0xFF863B5D), t),
                    horizonColor = lerpColor(Color(0xFF7A3356), Color(0xFFE86A3E), t),
                    horizonHazeColor = lerpColor(Color(0x55C25562), Color(0x88FFA726), t),
                    sunColor = Color(0xFFFF8A65),
                    sunHaloColor = Color(0x44FF7043),
                    cloudTint = lerpColor(Color(0x559C385C), Color(0x77FF8A65), t),
                    starAlpha = (1.0f - t) * 0.25f,
                    isNight = false
                )
            }
            altitude <= 10.0f -> {
                // Golden Hour / Sunrise / Sunset: Luminous amber-gold and apricot
                val t = altitude / 10.0f
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF202F54), Color(0xFF2B527E), t),
                    midSkyColor = lerpColor(Color(0xFF863B5D), Color(0xFFCC7258), t),
                    horizonColor = lerpColor(Color(0xFFE86A3E), Color(0xFFFFB74D), t),
                    horizonHazeColor = lerpColor(Color(0x88FFA726), Color(0x66FFE082), t),
                    sunColor = lerpColor(Color(0xFFFF8A65), Color(0xFFFFD54F), t),
                    sunHaloColor = Color(0x55FFB300),
                    cloudTint = lerpColor(Color(0x77FF8A65), Color(0x88FFCC80), t),
                    starAlpha = 0.0f,
                    isNight = false
                )
            }
            altitude <= 35.0f -> {
                // Morning / Late Afternoon: Crisp azure, cerulean, and soft atmospheric cyan
                val t = (altitude - 10.0f) / 25.0f
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF2B527E), Color(0xFF236098), t),
                    midSkyColor = lerpColor(Color(0xFFCC7258), Color(0xFF5294C7), t),
                    horizonColor = lerpColor(Color(0xFFFFB74D), Color(0xFFBCE3F5), t),
                    horizonHazeColor = Color(0x44B3E5FC),
                    sunColor = Color(0xFFFFF9C4),
                    sunHaloColor = Color(0x40FFF59D),
                    cloudTint = Color(0x66FFFFFF),
                    starAlpha = 0.0f,
                    isNight = false
                )
            }
            else -> {
                // Midday Solar Zenith: Brilliant deep azure and radiant solar corona
                val t = ((altitude - 35.0f) / 55.0f).coerceIn(0f, 1f)
                SkyColorPalette(
                    zenithColor = lerpColor(Color(0xFF236098), Color(0xFF165494), t),
                    midSkyColor = lerpColor(Color(0xFF5294C7), Color(0xFF3B86C4), t),
                    horizonColor = lerpColor(Color(0xFFBCE3F5), Color(0xFFA1D3F0), t),
                    horizonHazeColor = Color(0x38E1F5FE),
                    sunColor = Color(0xFFFFFDE7),
                    sunHaloColor = Color(0x4DFFF9C4),
                    cloudTint = Color(0x80FFFFFF),
                    starAlpha = 0.0f,
                    isNight = false
                )
            }
        }
    }

    fun determineSkyPeriod(currentTime: LocalTime, prayerTimes: PrayerTimesDay?): SkyPeriod {
        if (prayerTimes == null) {
            val hour = currentTime.hour
            return when (hour) {
                in 5..6 -> SkyPeriod.FAJR_DAWN
                7 -> SkyPeriod.SUNRISE
                in 8..11 -> SkyPeriod.MORNING
                in 12..15 -> SkyPeriod.DHUHR_MIDDAY
                in 16..18 -> SkyPeriod.ASR_AFTERNOON
                19 -> SkyPeriod.MAGHRIB_SUNSET
                in 20..22 -> SkyPeriod.ISHA_TWILIGHT
                else -> SkyPeriod.NIGHT
            }
        }

        val fajr = prayerTimes.prayers.find { it.prayer == Prayer.FAJR }?.time ?: LocalTime.of(5, 0)
        val sunrise = prayerTimes.prayers.find { it.prayer == Prayer.SUNRISE }?.time ?: LocalTime.of(6, 30)
        val dhuhr = prayerTimes.prayers.find { it.prayer == Prayer.DHUHR }?.time ?: LocalTime.of(12, 30)
        val asr = prayerTimes.prayers.find { it.prayer == Prayer.ASR }?.time ?: LocalTime.of(16, 0)
        val maghrib = prayerTimes.prayers.find { it.prayer == Prayer.MAGHRIB }?.time ?: LocalTime.of(18, 45)
        val isha = prayerTimes.prayers.find { it.prayer == Prayer.ISHA }?.time ?: LocalTime.of(20, 15)

        return when {
            currentTime.isBefore(fajr) -> SkyPeriod.NIGHT
            currentTime.isBefore(sunrise) -> SkyPeriod.FAJR_DAWN
            currentTime.isBefore(sunrise.plusMinutes(45)) -> SkyPeriod.SUNRISE
            currentTime.isBefore(dhuhr) -> SkyPeriod.MORNING
            currentTime.isBefore(asr) -> SkyPeriod.DHUHR_MIDDAY
            currentTime.isBefore(maghrib) -> SkyPeriod.ASR_AFTERNOON
            currentTime.isBefore(maghrib.plusMinutes(45)) -> SkyPeriod.MAGHRIB_SUNSET
            currentTime.isBefore(isha.plusMinutes(60)) -> SkyPeriod.ISHA_TWILIGHT
            else -> SkyPeriod.NIGHT
        }
    }

    /**
     * Normalized progress (0.0 to 1.0) of the sun across daylight hours
     * or moon across nighttime hours.
     */
    fun getCelestialBodyProgress(currentTime: LocalTime, prayerTimes: PrayerTimesDay?): Float {
        val sunrise = prayerTimes?.prayers?.find { it.prayer == Prayer.SUNRISE }?.time ?: LocalTime.of(6, 30)
        val maghrib = prayerTimes?.prayers?.find { it.prayer == Prayer.MAGHRIB }?.time ?: LocalTime.of(18, 45)

        val currentSec = currentTime.toSecondOfDay()
        val sunriseSec = sunrise.toSecondOfDay()
        val maghribSec = maghrib.toSecondOfDay()

        return if (currentSec in sunriseSec..maghribSec) {
            val totalDay = (maghribSec - sunriseSec).coerceAtLeast(1)
            ((currentSec - sunriseSec).toFloat() / totalDay).coerceIn(0f, 1f)
        } else {
            // Nighttime progress
            val totalNight = (86400 - maghribSec + sunriseSec).coerceAtLeast(1)
            val elapsed = if (currentSec > maghribSec) currentSec - maghribSec else (86400 - maghribSec) + currentSec
            (elapsed.toFloat() / totalNight).coerceIn(0f, 1f)
        }
    }
}
