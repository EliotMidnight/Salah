package com.example.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.AstronomicalSky
import com.example.ui.components.isDarkSurface
import com.example.ui.theme.Motion
import com.example.ui.theme.mix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Solar altitude below which the sun is treated as set. */
private const val SUN_HORIZON_DEGREES = -1.5f

/**
 * Azimuth arc mapped across the body's width, matching the living sky's.
 * Below 60° the sun is north of the frame, above 300° it has swung past it.
 */
private const val ARC_START_AZIMUTH = 60f
private const val ARC_END_AZIMUTH = 300f
private const val ARC_MIN_X = 0.14f
private const val ARC_MAX_X = 0.86f

/**
 * Altitudes mapped across the body's height.
 *
 * The lower bound is below the horizon so a body that has only just set sinks
 * toward the bottom edge rather than stopping dead at it. The upper bound is
 * well short of the zenith so a disc at its highest never has its glow clipped.
 */
private const val ALTITUDE_FLOOR = -15f
private const val ALTITUDE_CEILING = 70f

/**
 * The one body in the sky right now, in its real place, glowing its own colour.
 *
 * ## Position
 *
 * The sun's altitude and azimuth come from [com.example.engine.QiblaEngine] for
 * the user's location and time, so it rises on the side it actually rises on and
 * crosses at the actual solar noon. Scrub to another date and it shows that
 * day's arc.
 *
 * The moon's *position* is not computed by the app, and pretending otherwise
 * would be a lie in a prayer app. It is derived from the phase instead: a new
 * moon rides with the sun, a full moon is opposite it, and the two swap in
 * between. That is a real approximation - it ignores lunar declination, the
 * five degrees of orbital inclination, and the fifty minutes the moon gains each
 * day - so treat it as "roughly where it is", not as ephemeris. Its *phase* is
 * exact, because that comes from the lunar day.
 *
 * ## Colour
 *
 * The sun's colour is the same [AstronomicalSky.calculateContinuousSkyColors]
 * altitude ramp that tints the sky, so it reddens at sunset for the same reason
 * the horizon does. The glow is the body's own colour rather than one shared
 * accent: a gold bloom behind a white moon is two unrelated colours, and it made
 * every night look like a late afternoon.
 */
@Composable
fun CelestialBody(
    sunAltitude: Float,
    sunAzimuth: Float,
    isSetting: Boolean,
    hijriDay: Int,
    modifier: Modifier = Modifier
) {
    val showSun = sunAltitude > SUN_HORIZON_DEGREES
    val moonPhase = remember(hijriDay) { AstronomicalSky.getMoonPhaseInfo(hijriDay) }
    val isDarkPage = MaterialTheme.colorScheme.background.isDarkSurface()

    val palette = remember(sunAltitude, isSetting) {
        AstronomicalSky.calculateContinuousSkyColors(sunAltitude, isSetting)
    }
    val rawColor = if (isDarkPage) palette.sunColor else palette.sunColor.ink()
    // Smoothed, because the palette is piecewise: a tween keeps the disc from
    // stepping as the sun crosses one of the ramp's band boundaries.
    val bodyColor by animateColorAsState(
        targetValue = rawColor,
        animationSpec = tween(durationMillis = Motion.duration(700)),
        label = "bodyColor"
    )
    val moonColor = if (isDarkPage) MoonLitDark else MoonLitLight

    // Where the body is, resolved once so the draw pass is arithmetic-free.
    val position = remember(showSun, sunAltitude, sunAzimuth, moonPhase) {
        if (showSun) {
            BodyPosition.of(sunAltitude, sunAzimuth)
        } else {
            moonPosition(sunAltitude, sunAzimuth, moonPhase.illumination, moonPhase.isWaxing)
        }
    }

    val reduced = !Motion.allowContinuous
    val drift = rememberInfiniteTransition(label = "body")
    val phase by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduced) 1 else 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // A gentle arrival when the body swaps, so it does not pop.
    //
    // Deliberately keyed on `showSun` and nothing else. Keying it on the altitude
    // - which changes on every refresh - restarted the animation continuously and
    // left the body stuck at zero alpha, which looked exactly like a glitch.
    val arrive = remember { Animatable(1f) }
    LaunchedEffect(showSun) {
        if (reduced) {
            arrive.snapTo(1f)
        } else {
            arrive.snapTo(0.35f)
            arrive.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier = modifier) {
        // The disc is sized off the height, and the padding is three and a bit
        // radii so the widest glow always fits without being clipped.
        val radius = size.height * 0.11f
        val pad = radius * 3.3f
        val left = pad
        val right = (size.width - pad).coerceAtLeast(left + 1f)
        val top = pad
        val bottom = (size.height - pad).coerceAtLeast(top + 1f)

        val centre = Offset(
            x = left + (right - left) * position.xFraction,
            y = bottom - (bottom - top) * position.yFraction
        )
        val pulse = (0.5f + 0.5f * sin(phase * 2f * PI.toFloat())).coerceIn(0f, 1f)

        // The bloom: the wide, soft light the body sits in, in the body's own
        // colour. Drawn first and sized well past the widget, so it washes out
        // over the prayer name and the page around it rather than sitting in a
        // box - a Canvas does not clip, and that is the point: the light should
        // not look like it came from a panel.
        val swell = 1f + 0.06f * (pulse - 0.5f) * 2f
        // The bloom belongs to whichever body is up. Using the sun's colour at
        // night put a gold wash behind a silver moon, which is the exact
        // mismatch the shared accent was removed for.
        val glowColor = if (showSun) bodyColor else moonColor
        drawBloom(centre, size.height * 1.55f * swell, glowColor, 0.16f * arrive.value)
        drawBloom(centre, size.height * 0.95f * swell, glowColor, 0.13f * arrive.value)

        if (showSun) {
            drawSunBody(
                centre = centre,
                color = bodyColor,
                radius = radius,
                pulse = pulse,
                arrive = arrive.value
            )
        } else {
            drawMoonBody(
                centre = centre,
                color = moonColor,
                radius = radius * 0.92f,
                illumination = moonPhase.illumination,
                waxing = moonPhase.isWaxing,
                pulse = pulse,
                arrive = arrive.value
            )
        }
    }
}

/** Where on the widget a body sits: 0..1 in each axis. */
private data class BodyPosition(val xFraction: Float, val yFraction: Float) {
    companion object {
        fun of(altitude: Float, azimuth: Float) = BodyPosition(
            xFraction = ((azimuth - ARC_START_AZIMUTH) / (ARC_END_AZIMUTH - ARC_START_AZIMUTH))
                .coerceIn(ARC_MIN_X, ARC_MAX_X),
            yFraction = ((altitude - ALTITUDE_FLOOR) / (ALTITUDE_CEILING - ALTITUDE_FLOOR))
                .coerceIn(0f, 1f)
        )
    }
}

/**
 * Where the moon is, given where the sun is and how far through the cycle we are.
 *
 * The sun reflected through the moon's orbit: same altitude at new moon, opposite
 * at full, crossing in between, and a half-turn of azimuth for every half-turn
 * of phase. Crude, and cheaper and more honest than a fake ephemeris.
 */
private fun moonPosition(
    sunAltitude: Float,
    sunAzimuth: Float,
    illumination: Float,
    isWaxing: Boolean
): BodyPosition {
    // 0 = new (riding with the sun), 0.5 = full (opposite), 1 = new again.
    val cycle = (if (isWaxing) illumination else 1f - illumination) * 2f * PI.toFloat()
    val altitude = sunAltitude * cos(cycle)
    val swing = ((cycle / PI.toFloat()) * 180f + 360f) % 360f
    return BodyPosition.of(altitude, sunAzimuth + swing)
}

/**
 * What a glowing body has to do on a page that is already bright.
 *
 * The engine's colours are glow colours: brighter than the sky behind them,
 * which is the whole point on a dark page. On a near-white page the same numbers
 * vanish - a cream sun on cream paper measured 1.1:1 - so on a light page the
 * disc is pushed toward ink.
 *
 * Warm for the sun specifically: the ramp goes salmon -> gold below 10° and then
 * cream, so above that there is no hue left to preserve. Mixing toward a neutral
 * grey turned a midday sun into a grey ball. Warm ink keeps it a *sun* at every
 * altitude and keeps the reddening at sunset, which is the part that carries
 * information.
 */
private val SunInk = Color(0xFF7A4E00)
private val MoonInk = Color(0xFF2A3644)

private fun Color.ink(): Color = mix(SunInk, 0.5f)

/** The lit face: porcelain against a night page, steel against a daylit one. */
private val MoonLitDark = Color(0xFFF3F5F7)
private val MoonLitLight = Color(0xFF64748B)

/**
 * One soft disc of light.
 *
 * Three stops, because a two-stop gradient ends on a visible ring where the
 * alpha reaches zero, and a ring is the one thing that makes a glow look like a
 * shape instead of light.
 */
private fun DrawScope.drawBloom(centre: Offset, radius: Float, color: Color, strength: Float) {
    if (strength <= 0.001f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = strength),
                color.copy(alpha = strength * 0.45f),
                color.copy(alpha = strength * 0.14f),
                Color.Transparent
            ),
            center = centre,
            radius = radius
        ),
        radius = radius,
        center = centre
    )
}

/**
 * A halo in the body's own colour, brightest at the middle.
 *
 * Two stops rather than one, because a single-stop gradient ends on a visible
 * ring where the alpha hits zero; fading through a dimmer copy of the same hue
 * pushes that edge past the edge of the widget.
 */
private fun DrawScope.halo(centre: Offset, radius: Float, color: Color, strength: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = strength),
                color.copy(alpha = strength * 0.28f),
                Color.Transparent
            ),
            center = centre,
            radius = radius
        ),
        radius = radius,
        center = centre
    )
}

private fun DrawScope.drawSunBody(
    centre: Offset,
    color: Color,
    radius: Float,
    pulse: Float,
    arrive: Float
) {
    val alpha = arrive
    halo(centre, radius * (2.9f + 0.25f * pulse), color, 0.30f * alpha)
    halo(centre, radius * (1.75f + 0.1f * pulse), color, 0.34f * alpha)

    // A ring of short rays, breathing with the halo. Drawn between the glows and
    // the disc so the disc always reads as the solid centre.
    val rayInner = radius * 1.45f
    val rayOuter = rayInner + radius * 0.42f * (0.85f + 0.15f * pulse)
    val rayColour = color.copy(alpha = 0.55f * alpha)
    for (i in 0 until 12) {
        val radians = Math.toRadians((i * 30f - 90f).toDouble())
        val cosA = cos(radians).toFloat()
        val sinA = sin(radians).toFloat()
        drawLine(
            color = rayColour,
            start = Offset(centre.x + rayInner * cosA, centre.y + rayInner * sinA),
            end = Offset(centre.x + rayOuter * cosA, centre.y + rayOuter * sinA),
            strokeWidth = radius * 0.12f,
            cap = StrokeCap.Round
        )
    }

    drawCircle(color = color.copy(alpha = alpha), radius = radius, center = centre)
    // Specular, offset up-left so the disc is lit from somewhere.
    drawCircle(
        color = Color.White.copy(alpha = 0.35f * alpha),
        radius = radius * 0.55f,
        center = Offset(centre.x - radius * 0.22f, centre.y - radius * 0.22f)
    )
}

/**
 * The moon at its real phase.
 *
 * The lit shape is a real path with an elliptical terminator rather than a circle
 * with another circle subtracted: subtraction can only make crescents and
 * gibbous, so it fakes the quarter phases, which are the phases you actually
 * recognise.
 */
private fun DrawScope.drawMoonBody(
    centre: Offset,
    radius: Float,
    illumination: Float,
    waxing: Boolean,
    pulse: Float,
    arrive: Float,
    color: Color
) {
    val alpha = arrive
    val silver = color
    halo(centre, radius * (2.6f + 0.22f * pulse), silver, 0.26f * alpha)
    halo(centre, radius * (1.6f + 0.1f * pulse), silver, 0.24f * alpha)

    // Earthshine: the unlit disc, so a crescent is a shape rather than a sliver
    // floating in space.
    drawCircle(
        color = silver.mix(MoonInk, 0.35f).copy(alpha = 0.22f * alpha),
        radius = radius,
        center = centre
    )

    // Rounded to 10% so the phase steps like a calendar instead of creeping.
    val phase = Math.round(illumination * 10.0).toFloat() / 10f
    if (phase <= 0.03f) {
        // New moon: an outline, so "there is a moon and it is not lit" is legible.
        drawCircle(
            color = silver.copy(alpha = 0.34f * alpha),
            radius = radius,
            center = centre,
            style = Stroke(width = radius * 0.07f)
        )
        return
    }
    if (phase >= 0.97f) {
        drawCircle(color = silver.copy(alpha = alpha), radius = radius, center = centre)
        return
    }

    val terminatorRx = abs(cos(Math.toRadians((phase * 180f).toDouble()))).toFloat() * radius
    val litOnRight = waxing
    val top = centre.y - radius
    val bottom = centre.y + radius

    val path = Path().apply {
        moveTo(centre.x, top)
        // Outer limb, down whichever side is lit.
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(centre.x - radius, top, centre.x + radius, bottom),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        // Terminator back up. It bulges into the lit side for a crescent and away
        // from it for a gibbous, which is the whole visual difference between them.
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                centre.x - terminatorRx, top, centre.x + terminatorRx, bottom
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = when {
                phase < 0.5f && litOnRight -> 180f
                phase < 0.5f -> -180f
                litOnRight -> -180f
                else -> 180f
            },
            forceMoveTo = false
        )
        close()
    }
    drawPath(path = path, color = silver.copy(alpha = alpha))
    // A crater catches light from the same direction as the disc, so on an inked
    // moon it is a lighter mark and on a porcelain one a darker one.
    drawCircle(
        color = if (silver == MoonLitLight) Color.White.copy(alpha = 0.45f * alpha)
        else Color(0xFF9AA7B2).copy(alpha = 0.35f * alpha),
        radius = radius * 0.26f,
        center = Offset(centre.x - radius * 0.26f, centre.y - radius * 0.24f)
    )
}
