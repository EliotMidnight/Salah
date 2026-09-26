package com.example.ui.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.engine.SkyColorPalette
import com.example.engine.SunPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveSkyTextTest {
    @Test
    fun choosesDarkTextOverBrightSky() {
        val resolver = SkyTextColorResolver(
            palette = solidPalette(Color.White),
            sunPosition = SunPosition(azimuth = 180f, altitude = 45f, isSunVisible = true),
            width = 1080f,
            height = 1920f
        )

        assertEquals(Color(0xFF071522), resolver.colorForBounds(Rect(360f, 120f, 720f, 170f)))
    }

    @Test
    fun choosesLightTextOverDarkSky() {
        val resolver = SkyTextColorResolver(
            palette = solidPalette(Color.Black),
            sunPosition = SunPosition(azimuth = 180f, altitude = -45f, isSunVisible = false),
            width = 1080f,
            height = 1920f
        )

        assertEquals(Color(0xFFF8FBFF), resolver.colorForBounds(Rect(360f, 120f, 720f, 170f)))
    }

    private fun solidPalette(color: Color) = SkyColorPalette(
        zenithColor = color,
        midSkyColor = color,
        horizonColor = color,
        horizonHazeColor = color.copy(alpha = 0f),
        sunColor = color,
        sunHaloColor = color.copy(alpha = 0f),
        cloudTint = color.copy(alpha = 0f),
        starAlpha = 0f,
        isNight = false
    )
}
