package com.example.engine

import androidx.compose.ui.graphics.Color

data class MoonPhaseInfo(
    val day: Int,
    val illumination: Float, // 0.0 to 1.0
    val isWaxing: Boolean,
    val nameEn: String,
    val nameAr: String
)

/**
 * The sky at one instant, derived entirely from the sun's altitude and whether
 * it is rising or setting.
 */
data class SkyColorPalette(
    val zenithColor: Color,
    val midSkyColor: Color,
    val horizonColor: Color,
    val horizonHazeColor: Color,
    val sunColor: Color,
    val starAlpha: Float,
    val isNight: Boolean
)

/**
 * The sky, from one number.
 */
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
                    starAlpha = 0.0f,
                    isNight = false
                )
            }
        }
    }

}
