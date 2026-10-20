package com.example.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.engine.AstronomicalSky
import com.example.ui.components.isDarkSurface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.PI

/** Solar altitude below which the sun is treated as set. */
private const val SUN_HORIZON_DEGREES = -1.5f

/**
 * The height of the sky strip, shared by the disc and its halo.
 *
 * One constant because both derive their geometry from it: the halo's centre is
 * the disc's centre by construction, not by two matching copies of the same
 * pixel arithmetic.
 */
internal val CelestialStripHeight = 148.dp

/** The disc radius for a strip [height] pixels tall. */
internal fun skyDiscRadius(height: Float): Float = height * 0.10f

/**
 * The one body in the sky right now, in its real place.
 *
 * The disc itself stays monochrome and static - colour comes from the type
 * scale, not from a painted sky. What it casts is [CelestialHalo]'s job, and
 * that wash is painted behind the page's content rather than inside this
 * widget, which is why it can reach the lines below the disc.
 */
@Composable
fun CelestialBody(
    sunAltitude: Float,
    sunAzimuth: Float,
    isSetting: Boolean,
    hijriDay: Int,
    modifier: Modifier = Modifier
) {
    val body = rememberSkyBody(sunAltitude, sunAzimuth, hijriDay)

    // The disc is ink, not a painted body. Taking it from the scheme keeps it
    // tied to the warm neutral ladder instead of freezing an old copy of the
    // light/dark text colours here.
    val ink = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val radius = skyDiscRadius(size.height)
        val centre = skyBodyCentre(size.width, size.height, body.position)

        if (body.isSun) {
            drawSunBody(centre = centre, color = ink, radius = radius)
        } else {
            drawMoonBody(centre = centre, color = ink, radius = radius * 0.92f,
                illumination = body.illumination, waxing = body.isWaxing)
        }
    }
}

/**
 * The ambient halo the current body casts, behind the page's content.
 *
 * This is the one deliberate wash in the design, and it is the sky's own light
 * rather than decoration: the page is lit by whatever is in it. A compact core
 * makes the body itself visibly warm/cool; a much wider, fainter wash carries
 * that light behind the lines. It is static - no animation - and the wash fades
 * to nothing before the day's list, so ink and the single accent keep their
 * contrast on top of it.
 *
 * Painted across the whole content column (Today's `SimpleView`), not inside
 * [CelestialBody], because a widget cannot draw outside its own bounds and the
 * halo has to reach behind the lines under the disc.
 */
@Composable
fun CelestialHalo(
    sunAltitude: Float,
    sunAzimuth: Float,
    hijriDay: Int,
    modifier: Modifier = Modifier
) {
    val body = rememberSkyBody(sunAltitude, sunAzimuth, hijriDay)
    val dark = MaterialTheme.colorScheme.background.isDarkSurface()
    // The broad wash is deliberately faint. The core is where the sun/moon
    // actually reads as gold/silver; keeping it compact stops a bright centre
    // from turning the whole page into a tinted panel.
    val (washPeak, corePeak) = haloPeaks(dark = dark, isSun = body.isSun)
    val aura = auraFor(body.isSun)

    Canvas(modifier = modifier) {
        val strip = CelestialStripHeight.toPx()
        val centre = skyBodyCentre(size.width, strip, body.position)
        val radius = max(size.width * 1.15f, 320.dp.toPx())

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to aura.copy(alpha = washPeak),
                    0.5f to aura.copy(alpha = washPeak * 0.45f),
                    1f to aura.copy(alpha = 0f)
                ),
                center = centre,
                radius = radius
            ),
            radius = radius,
            center = centre
        )

        val coreRadius = radius * 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to aura.copy(alpha = corePeak),
                    0.5f to aura.copy(alpha = corePeak * 0.35f),
                    1f to aura.copy(alpha = 0f)
                ),
                center = centre,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = centre
        )
    }
}

/** The sun's halo. A bright yellow-gold, not an amber that reads brown. */
private val SunAura = Color(0xFFFFD54F)

/** The moon's halo. Bright silver-blue, the cool counterpart of [SunAura]. */
private val MoonAura = Color(0xFFB9D7FF)

/**
 * The halo's two opacities, as data rather than four branches inside the draw.
 *
 * [wash] is the peak of the broad, faint light that can sit behind text, so
 * contrast tests use the same number the page paints. [core] is the compact
 * body glow; it is kept tight around the disc.
 */
internal data class HaloPeaks(val wash: Float, val core: Float)

internal fun haloPeaks(dark: Boolean, isSun: Boolean): HaloPeaks = when {
    dark && isSun -> HaloPeaks(wash = 0.14f, core = 0.90f)
    dark -> HaloPeaks(wash = 0.12f, core = 0.70f)
    isSun -> HaloPeaks(wash = 0.08f, core = 0.45f)
    else -> HaloPeaks(wash = 0.08f, core = 0.32f)
}

internal fun auraFor(isSun: Boolean): Color = if (isSun) SunAura else MoonAura

/**
 * What is in the sky right now, and where in the strip it sits.
 *
 * Resolved once by both the disc and the halo, so the two cannot disagree about
 * which body is up or where it is.
 */
internal data class SkyBody(
    val isSun: Boolean,
    val position: BodyPosition,
    val illumination: Float,
    val isWaxing: Boolean
)

/** The body in the sky right now, re-resolved when its inputs change. */
@Composable
internal fun rememberSkyBody(
    sunAltitude: Float,
    sunAzimuth: Float,
    hijriDay: Int
): SkyBody {
    val isSun = sunAltitude > SUN_HORIZON_DEGREES
    val moonPhase = remember(hijriDay) { AstronomicalSky.getMoonPhaseInfo(hijriDay) }
    return remember(isSun, sunAltitude, sunAzimuth, moonPhase) {
        SkyBody(
            isSun = isSun,
            position = if (isSun) {
                BodyPosition.of(sunAltitude, sunAzimuth)
            } else {
                moonPosition(sunAltitude, sunAzimuth, moonPhase.illumination, moonPhase.isWaxing)
            },
            illumination = moonPhase.illumination,
            isWaxing = moonPhase.isWaxing
        )
    }
}

/** Where on the strip a body sits: 0..1 in each axis. */
internal data class BodyPosition(val xFraction: Float, val yFraction: Float) {
    companion object {
        fun of(altitude: Float, azimuth: Float) = BodyPosition(
            xFraction = ((azimuth - 60f) / (300f - 60f))
                .coerceIn(0.14f, 0.86f),
            yFraction = ((altitude - -15f) / (70f - -15f))
                .coerceIn(0f, 1f)
        )
    }
}

/**
 * The body's centre inside a [width] x [stripHeight] pixel strip.
 *
 * Both the disc and its halo place themselves through this function, so the
 * two can never drift apart by a pixel.
 */
internal fun skyBodyCentre(width: Float, stripHeight: Float, position: BodyPosition): Offset {
    val radius = skyDiscRadius(stripHeight)
    val pad = radius * 3.3f
    val left = pad
    val right = (width - pad).coerceAtLeast(left + 1f)
    val top = pad
    val bottom = (stripHeight - pad).coerceAtLeast(top + 1f)
    return Offset(
        x = left + (right - left) * position.xFraction,
        y = bottom - (bottom - top) * position.yFraction
    )
}

/** Where the moon is, given where the sun is and how far through the cycle we are. */
private fun moonPosition(
    sunAltitude: Float,
    sunAzimuth: Float,
    illumination: Float,
    isWaxing: Boolean
): BodyPosition {
    val cycle = (if (isWaxing) illumination else 1f - illumination) * 2f * PI.toFloat()
    val altitude = sunAltitude * cos(cycle)
    val swing = ((cycle / PI.toFloat()) * 180f + 360f) % 360f
    return BodyPosition.of(altitude, sunAzimuth + swing)
}

private fun DrawScope.drawSunBody(centre: Offset, color: Color, radius: Float) {
    drawCircle(color = color, radius = radius, center = centre)
}

private fun DrawScope.drawMoonBody(
    centre: Offset,
    color: Color,
    radius: Float,
    illumination: Float,
    waxing: Boolean
) {
    val phase = Math.round(illumination * 10.0).toFloat() / 10f
    if (phase <= 0.03f) {
        // New moon: an outline.
        drawCircle(
            color = color,
            radius = radius,
            center = centre,
            style = Stroke(width = radius * 0.07f)
        )
        return
    }
    if (phase >= 0.97f) {
        drawCircle(color = color, radius = radius, center = centre)
        return
    }

    val terminatorRx = abs(cos(Math.toRadians((phase * 180f).toDouble()))).toFloat() * radius
    val litOnRight = waxing
    val top = centre.y - radius
    val bottom = centre.y + radius

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(centre.x, top)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(centre.x - radius, top, centre.x + radius, bottom),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
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
    drawPath(path = path, color = color)
}
