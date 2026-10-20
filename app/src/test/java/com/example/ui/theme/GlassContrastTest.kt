package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Guards the contrast claim behind the glass tint.
 *
 * The dock's label is `onSurface`, and the tint is the surface colour at
 * [Glass.tintAlpha]. A blurred backdrop can be any colour, so the two extremes
 * are the only cases worth proving: white text over a black backdrop in dark
 * mode, and black text over a white backdrop in light mode. If the tint is
 * lowered until either fails, this test fails with it.
 */
class GlassContrastTest {

    @Test
    fun `light glass text clears 4_5 to 1 over the worst backdrop`() {
        val glass = SurfaceLight.copy(alpha = Glass.tintAlpha).compositeOver(Color.Black)
        val ratio = contrast(TextPrimaryLight, glass)
        assertTrue("light glass contrast was $ratio:1", ratio >= 4.5)
    }

    @Test
    fun `dark glass text clears 4_5 to 1 over the worst backdrop`() {
        val glass = SurfaceDark.copy(alpha = Glass.tintAlpha).compositeOver(Color.White)
        val ratio = contrast(TextPrimaryDark, glass)
        assertTrue("dark glass contrast was $ratio:1", ratio >= 4.5)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)

    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
