package com.example.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.engine.SkyColorPalette
import com.example.engine.SunPosition
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private val AdaptiveDarkText = Color(0xFF071522)
private val AdaptiveLightText = Color(0xFFF8FBFF)
private val HorizonShadow = Color(0xFF0A1020)

internal class SkyTextColorResolver(
    private val palette: SkyColorPalette,
    private val sunPosition: SunPosition?,
    private val width: Float,
    private val height: Float
) {
    fun colorForBounds(bounds: Rect): Color {
        if (width <= 0f || height <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
            return AdaptiveLightText
        }

        val padded = bounds.inflate(12f)
        val centerX = padded.center.x
        val centerY = padded.center.y
        val radiusX = (padded.width * 0.22f).coerceAtLeast(12f)
        val radiusY = (padded.height * 0.65f).coerceAtLeast(10f)
        var darkScore = Float.MAX_VALUE
        var lightScore = Float.MAX_VALUE

        for (xStep in -1..1) {
            for (yStep in -1..1) {
                val sampleX = (centerX + xStep * radiusX).coerceIn(0f, width)
                val sampleY = (centerY + yStep * radiusY).coerceIn(0f, height)
                val background = sampleBackground(sampleX, sampleY)
                darkScore = min(darkScore, contrastRatio(AdaptiveDarkText, background))
                lightScore = min(lightScore, contrastRatio(AdaptiveLightText, background))
            }
        }

        return if (darkScore >= lightScore) AdaptiveDarkText else AdaptiveLightText
    }

    private fun sampleBackground(x: Float, y: Float): Color {
        val normalizedX = (x / width).coerceIn(0f, 1f)
        val normalizedY = (y / height).coerceIn(0f, 1f)
        val gradientPosition = (normalizedY / 0.88f).coerceIn(0f, 1f)
        val gradientColor = if (gradientPosition <= 0.5f) {
            blend(palette.zenithColor, palette.midSkyColor, gradientPosition * 2f)
        } else {
            blend(palette.midSkyColor, palette.horizonColor, (gradientPosition - 0.5f) * 2f)
        }

        val horizonPosition = ((normalizedY - 0.70f) / 0.30f).coerceIn(0f, 1f)
        var result = blend(gradientColor, HorizonShadow, horizonPosition * 0.22f)
        val sunX = sunScreenX()
        val sunY = sunScreenY()
        val aspect = (width / height).coerceAtLeast(0.5f)
        val distanceX = (normalizedX - sunX) * aspect
        val distanceY = normalizedY - sunY
        val sunDistance = distanceX * distanceX + distanceY * distanceY
        val hazeWeight = exp(-sunDistance / 0.035f) * palette.horizonHazeColor.alpha
        result = composite(palette.horizonHazeColor.copy(alpha = hazeWeight), result)

        val cloudWave = (sin(normalizedX * 11f + normalizedY * 17f) + 1f) * 0.5f
        val cloudWeight = palette.cloudTint.alpha * cloudWave * 0.08f
        result = composite(palette.cloudTint.copy(alpha = cloudWeight), result)

        if (sunPosition != null && sunPosition.altitude > -8f) {
            val haloWeight = exp(-sunDistance / 0.012f) * 0.18f
            result = blend(result, palette.sunHaloColor, haloWeight)
        }

        return result
    }

    private fun sunScreenX(): Float {
        val azimuth = sunPosition?.azimuth ?: 180f
        return when {
            azimuth < 60f -> 0.12f
            azimuth > 300f -> 0.88f
            else -> ((azimuth - 60f) / 240f).coerceIn(0.12f, 0.88f)
        }
    }

    private fun sunScreenY(): Float {
        val altitude = (sunPosition?.altitude ?: 0f).coerceIn(-8f, 85f)
        val progress = ((altitude + 8f) / 93f).coerceIn(0f, 1f)
        val arcZone = 0.50f
        val baseHorizonY = arcZone * 0.88f
        val zenithY = arcZone * 0.22f
        return baseHorizonY - (baseHorizonY - zenithY) * progress
    }
}

@Composable
internal fun rememberAdaptiveSkyTextColor(target: Color): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 360),
        label = "adaptive_sky_text_color"
    )
    return animated
}

private fun blend(background: Color, foreground: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = background.red + (foreground.red - background.red) * fraction,
        green = background.green + (foreground.green - background.green) * fraction,
        blue = background.blue + (foreground.blue - background.blue) * fraction,
        alpha = background.alpha + (foreground.alpha - background.alpha) * fraction
    )
}

private fun composite(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha.coerceIn(0f, 1f)
    return Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f
    )
}

private fun relativeLuminance(color: Color): Float {
    fun linear(channel: Float): Float {
        return if (channel <= 0.03928f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    return 0.2126f * linear(color.red) +
        0.7152f * linear(color.green) +
        0.0722f * linear(color.blue)
}

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    val lighter = max(firstLuminance, secondLuminance)
    val darker = min(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
