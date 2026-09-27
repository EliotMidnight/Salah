package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.model.UserLocation
import com.example.engine.AstronomicalSky
import com.example.engine.MoonPhaseInfo
import com.example.engine.QiblaEngine
import com.example.engine.SunPosition
import com.example.ui.theme.Motion
import com.example.ui.theme.mix
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Stable Star definition for astronomical sky rendering */
private data class AstroStar(
    val normX: Float,
    val normY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val phaseOffset: Float
)

/** Constellation line segment connecting two normalized coordinates */
private data class ConstellationLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

/** Solar altitudes below this put the disc under the horizon, so it is not drawn. */
private const val SUN_SET_DROP_DEGREES = -8f

/** Azimuth arc, in degrees from true north, mapped across the full canvas width. */
private const val SUN_PATH_AZIMUTH_START = 60f
private const val SUN_PATH_AZIMUTH_END = 300f

/** Screen-x bounds for the sun, so the disc never clips against either edge. */
private const val SUN_PATH_MIN_X = 0.12f
private const val SUN_PATH_MAX_X = 0.88f

/** The sun's own colour pushed toward deep amber, for its rays and shaded limb. */
private val SunShade = Color(0xFF8A4B00)

/**
 * Horizontal screen position, 0..1, for a body at [azimuth].
 *
 * Azimuths outside the sun's arc are clamped rather than wrapped, because they
 * are still meaningful: below 60° the sun is north of the frame edge, above 300°
 * it has swung past it, and either way it has to render on the correct side so
 * the disc and the glow beneath it stay together.
 */
private fun azimuthToScreenX(azimuth: Float): Float = when {
    azimuth <= SUN_PATH_AZIMUTH_START -> SUN_PATH_MIN_X
    azimuth >= SUN_PATH_AZIMUTH_END -> SUN_PATH_MAX_X
    else -> ((azimuth - SUN_PATH_AZIMUTH_START) / (SUN_PATH_AZIMUTH_END - SUN_PATH_AZIMUTH_START))
        .coerceIn(SUN_PATH_MIN_X, SUN_PATH_MAX_X)
}

/**
 * Vertical screen position for the sun, from its [altitude] in degrees.
 *
 * Clamped to the band the disc is actually drawn in, so the sun sits on the
 * horizon line at sunset instead of sliding under the dune silhouette.
 */
private fun altitudeToScreenY(altitude: Float, arcZoneHeight: Float): Float {
    val progress = ((altitude.coerceIn(SUN_SET_DROP_DEGREES, 85f) - SUN_SET_DROP_DEGREES) / 93f)
        .coerceIn(0f, 1f)
    val zenithY = arcZoneHeight * 0.22f
    val baseHorizonY = arcZoneHeight * 0.88f
    return baseHorizonY - (baseHorizonY - zenithY) * progress
}

/**
 * The animated sky.
 *
 * Every celestial fact on this canvas is derived, never invented: the sun's
 * altitude and azimuth come from [QiblaEngine.calculateSunPosition] for the given
 * location and time, the gradient comes from that same altitude via
 * [AstronomicalSky.calculateContinuousSkyColors], and the moon's phase comes from
 * the lunar day. Nothing here is a decorative layer laid over the top - which is
 * why the gradient, the disc and the glow all move as one when the clock moves.
 *
 * What *is* decorative is confined to motion: twinkle, corona pulse, ray
 * rotation and meteors. All of it stops when the system animation scale is zero,
 * and each loop is parked while the sky does not need it, so a midday frame
 * never runs a star animation.
 *
 * Not the default background for the Today screen - see [StaticSkyBackground] for
 * why the still sky won - but available behind the "Animated sky" setting.
 */
@Composable
fun LivingSkyCanvas(
    modifier: Modifier = Modifier,
    sunPosition: SunPosition? = null,
    location: UserLocation? = null,
    currentTime: LocalTime = LocalTime.now(),
    hijriDay: Int = 14
) {
    // 1. Resolve exact solar altitude and azimuth for this location and time.
    val effectiveSunPosition = remember(sunPosition, location, currentTime) {
        sunPosition ?: QiblaEngine.calculateSunPosition(
            location ?: UserLocation.RABAT,
            LocalDateTime.of(LocalDate.now(), currentTime)
        )
    }

    val sunAltitude = effectiveSunPosition.altitude
    val isSetting = effectiveSunPosition.azimuth > 180f

    // 2. One palette drives the gradient, the glow and the disc alike.
    val rawPalette = remember(sunAltitude, isSetting) {
        AstronomicalSky.calculateContinuousSkyColors(sunAltitude, isSetting)
    }

    // Smooth colour transitions across sky changes.
    val colorAnimSpec = tween<Color>(durationMillis = Motion.duration(1600), easing = FastOutSlowInEasing)
    val animatedZenith by animateColorAsState(rawPalette.zenithColor, colorAnimSpec, label = "zenith")
    val animatedMidSky by animateColorAsState(rawPalette.midSkyColor, colorAnimSpec, label = "midSky")
    val animatedHorizon by animateColorAsState(rawPalette.horizonColor, colorAnimSpec, label = "horizon")
    val animatedHaze by animateColorAsState(rawPalette.horizonHazeColor, colorAnimSpec, label = "haze")
    val animatedSun by animateColorAsState(rawPalette.sunColor, colorAnimSpec, label = "sun")

    val moonPhase: MoonPhaseInfo = remember(hijriDay) {
        AstronomicalSky.getMoonPhaseInfo(hijriDay)
    }

    // 3. Infinite animations for living atmosphere - only animate what this sky
    // needs. Night twinkle/meteor paused during full day; sun rays/corona paused
    // when the sun is below the horizon.
    //
    // When the user has reduced motion, none of these loops start: the sky
    // renders the same scene statically (large, slow, full-screen movement is the
    // classic vestibular trigger, so this matters more here than almost anywhere).
    val needStars = rawPalette.starAlpha > 0.03f || rawPalette.isNight || sunAltitude < 6f
    val needSunFx = sunAltitude > SUN_SET_DROP_DEGREES
    val reducedMotion = !Motion.allowContinuous
    val atmosphere = rememberInfiniteTransition(label = "livingAtmosphere")

    val twinkleTime by atmosphere.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 1 else if (needStars) 3600 else 600_000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )

    val coronaPulse by atmosphere.animateFloat(
        initialValue = 0.94f,
        targetValue = if (needSunFx) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 1 else if (needSunFx) 4200 else 600_000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coronaPulse"
    )

    val rayRotation by atmosphere.animateFloat(
        initialValue = 0f,
        targetValue = if (needSunFx) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reducedMotion) 1 else 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRotation"
    )

    // Meteor window only advances while stars can actually show (saves most day frames).
    val meteorCycle by atmosphere.animateFloat(
        initialValue = 0f,
        targetValue = if (needStars) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 1 else if (needStars) 14_000 else 600_000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "meteorCycle"
    )

    // 4. Stable starfield coordinates & constellations (Ursa Major, Orion Belt).
    val stars = remember {
        val rand = Random(1337)
        val starColors = listOf(
            Color(0xFFFFFFFF), // Pure Vega white
            Color(0xFFE3F2FD), // Cool Sirius diamond
            Color(0xFFFFECB3), // Warm Capella gold
            Color(0xFFD1C4E9), // Mystical Pleiades lavender
            Color(0xFFB3E5FC)  // Cyan Arcturus
        )
        List(65) {
            AstroStar(
                normX = rand.nextFloat(),
                normY = rand.nextFloat() * 0.72f, // Upper 72% of sky
                radius = rand.nextFloat() * 1.8f + 0.6f,
                baseAlpha = rand.nextFloat() * 0.55f + 0.45f,
                color = starColors[rand.nextInt(starColors.size)],
                twinkleSpeed = rand.nextFloat() * 1.6f + 0.7f,
                phaseOffset = rand.nextFloat() * (2 * PI).toFloat()
            )
        }
    }

    val constellations = remember {
        listOf(
            // Ursa Major (The Big Dipper / الدب الأكبر)
            ConstellationLine(0.18f, 0.14f, 0.24f, 0.16f), // Alkaid to Mizar
            ConstellationLine(0.24f, 0.16f, 0.29f, 0.20f), // Mizar to Alioth
            ConstellationLine(0.29f, 0.20f, 0.35f, 0.22f), // Alioth to Megrez
            ConstellationLine(0.35f, 0.22f, 0.36f, 0.28f), // Megrez to Phecda
            ConstellationLine(0.36f, 0.28f, 0.44f, 0.27f), // Phecda to Merak
            ConstellationLine(0.44f, 0.27f, 0.43f, 0.21f), // Merak to Dubhe
            ConstellationLine(0.43f, 0.21f, 0.35f, 0.22f), // Dubhe to Megrez
            // Orion's Belt (حزام الجبار)
            ConstellationLine(0.72f, 0.18f, 0.76f, 0.20f), // Alnitak to Alnilam
            ConstellationLine(0.76f, 0.20f, 0.80f, 0.22f)  // Alnilam to Mintaka
        )
    }

    // Static silhouette geometry does not depend on animation values - build once per size.
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val silhouette = remember(canvasSize, density) {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) null
        else with(density) { buildHorizonSilhouette(canvasSize.width, canvasSize.height) }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
    ) {
        val width = size.width
        val height = size.height
        val arcZoneHeight = minOf(height * 0.50f, 380.dp.toPx())
        val horizonY = height * 0.88f

        // 1. The gradient: zenith -> mid sky -> horizon.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(animatedZenith, animatedMidSky, animatedHorizon),
                startY = 0f,
                endY = horizonY
            ),
            size = size
        )

        // 2. Belt of Venus glow, centred on the sun's own azimuth so it tracks the disc.
        if (animatedHaze.alpha > 0.05f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedHaze,
                        animatedHaze.copy(alpha = animatedHaze.alpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = width * azimuthToScreenX(effectiveSunPosition.azimuth),
                        y = horizonY * 0.92f
                    ),
                    radius = width * 0.75f
                ),
                size = size
            )
        }

        // 3. Stars, constellations and meteors - only once the sky is dark enough.
        drawStarfield(
            width = width,
            height = height,
            stars = stars,
            constellations = constellations,
            starAlpha = rawPalette.starAlpha,
            isNight = rawPalette.isNight,
            twinkleTime = twinkleTime,
            meteorCycle = meteorCycle
        )

        // 4. The sun, at its real altitude and azimuth for this time and place.
        if (needSunFx) {
            drawSun(
                center = Offset(
                    x = width * azimuthToScreenX(effectiveSunPosition.azimuth),
                    y = altitudeToScreenY(sunAltitude, arcZoneHeight)
                ),
                sunAltitude = sunAltitude,
                sunColor = animatedSun,
                coronaPulse = coronaPulse,
                rotationDegrees = rayRotation
            )
        }

        // 5. The moon, once the sun is low enough to share the frame.
        if (sunAltitude < 8f || rawPalette.isNight) {
            val moonAlpha = when {
                sunAltitude <= -12f -> 1.0f
                sunAltitude <= 0f -> 0.85f
                else -> (1.0f - (sunAltitude / 8f)).coerceIn(0f, 1f)
            }
            drawMoon(
                center = Offset(width * 0.80f, arcZoneHeight * 0.32f),
                moonRadius = 24.dp.toPx(),
                illumination = moonPhase.illumination,
                isWaxing = moonPhase.isWaxing,
                alpha = moonAlpha
            )
        }

        // 6. Desert dunes and a minaret on the horizon line.
        silhouette?.let { sil ->
            drawPath(path = sil.backDune, color = animatedHorizon.copy(alpha = 0.45f), style = Fill)
            drawPath(path = sil.minaret, color = animatedHorizon.copy(alpha = 0.65f), style = Fill)
            drawCircle(
                color = animatedHorizon.copy(alpha = 0.75f),
                radius = 1.4.dp.toPx(),
                center = sil.finial
            )
            drawPath(path = sil.frontDune, color = animatedHorizon.copy(alpha = 0.75f), style = Fill)
        }
    }
}

/**
 * Stars, the two guide constellations, and a periodic meteor.
 *
 * No-op until the palette actually lets stars show, which is most of a daylight
 * frame.
 */
private fun DrawScope.drawStarfield(
    width: Float,
    height: Float,
    stars: List<AstroStar>,
    constellations: List<ConstellationLine>,
    starAlpha: Float,
    isNight: Boolean,
    twinkleTime: Float,
    meteorCycle: Float
) {
    if (starAlpha <= 0.03f) return

    // 1. Constellation guide paths
    val constellationColor = Color.White.copy(alpha = (starAlpha * 0.28f).coerceIn(0f, 0.35f))
    for (line in constellations) {
        drawLine(
            color = constellationColor,
            start = Offset(line.x1 * width, line.y1 * height),
            end = Offset(line.x2 * width, line.y2 * height),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 2. Twinkling individual stars
    for (star in stars) {
        val twinkle = sin(twinkleTime * star.twinkleSpeed + star.phaseOffset)
        val alpha = (star.baseAlpha * (0.35f + 0.65f * ((twinkle + 1f) / 2f)) * starAlpha).coerceIn(0f, 1f)
        if (alpha <= 0.02f) continue

        val center = Offset(star.normX * width, star.normY * height)
        drawCircle(color = star.color.copy(alpha = alpha), radius = star.radius.dp.toPx(), center = center)
        // Subtle star diffraction shimmer for bright stars
        if (star.radius > 1.8f && alpha > 0.65f) {
            drawCircle(
                color = star.color.copy(alpha = alpha * 0.25f),
                radius = (star.radius * 2.6f).dp.toPx(),
                center = center
            )
        }
    }

    // 3. Periodic shooting star, active during the [0.18 .. 0.24] window of
    //    [meteorCycle] - about 840ms out of every 14s, and only at night.
    if (!isNight && starAlpha <= 0.45f) return
    if (meteorCycle < 0.18f || meteorCycle > 0.24f) return

    val t = (meteorCycle - 0.18f) / 0.06f
    val headX = width * 0.72f - 180.dp.toPx() * t
    val headY = height * 0.08f + 95.dp.toPx() * t
    val tailX = headX + 180.dp.toPx() / 2.2f
    val tailY = headY - 95.dp.toPx() / 2.2f

    val alpha = (sin(t * PI.toFloat()).coerceIn(0f, 1f) * starAlpha).coerceAtLeast(0f)
    if (alpha <= 0.05f) return

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFFFD54F).copy(alpha = alpha * 0.6f),
                Color.White.copy(alpha = alpha)
            ),
            start = Offset(tailX, tailY),
            end = Offset(headX, headY)
        ),
        start = Offset(tailX, tailY),
        end = Offset(headX, headY),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(color = Color.White.copy(alpha = alpha), radius = 2.5.dp.toPx(), center = Offset(headX, headY))
}

/**
 * Renders the Sun with simple geometric vector aesthetics:
 * - Vibrant geometric disc, coloured by [sunColor] from the same altitude ramp
 *   that tints the gradient, so the disc is never a different sun than the sky
 * - 8 iconic geometric capsule rays with rounded caps
 * - Concentric geometric orbital aura rings
 * - Clean geometric specular core and subtle duo-tone depth
 */
private fun DrawScope.drawSun(
    center: Offset,
    sunAltitude: Float,
    sunColor: Color,
    coronaPulse: Float,
    rotationDegrees: Float
) {
    val baseRadius = 21.dp.toPx()
    val isNearHorizon = sunAltitude <= 6f

    // Every other tone is the same hue pushed toward deep amber, so the whole
    // disc shifts together as the sun reddens instead of snapping between two
    // hand-picked palettes.
    val coreColor = sunColor
    val rayColor = sunColor.mix(SunShade, 0.18f)
    val limbColor = sunColor.mix(SunShade, 0.45f)

    // 1. Concentric aura rings (vector orbit, not fuzzy blur)
    val ring1Radius = baseRadius * 1.65f * coronaPulse
    val ring2Radius = baseRadius * 2.30f * coronaPulse
    drawCircle(
        color = coreColor.copy(alpha = 0.16f),
        radius = ring2Radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = coreColor.copy(alpha = 0.28f),
        radius = ring1Radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    // Soft subtle inner disk backing (crisp flat vector fill)
    drawCircle(color = coreColor.copy(alpha = 0.07f), radius = ring1Radius, center = center)

    // 2. Geometric capsule rays, 8 equidistant with rounded caps
    val rayInnerOffset = baseRadius + 6.dp.toPx()
    val rayLength = 7.5.dp.toPx() * coronaPulse
    val rayStrokeWidth = 3.6.dp.toPx()

    rotate(degrees = rotationDegrees, pivot = center) {
        for (i in 0 until 8) {
            val angleRad = (i * 45.0) * (PI / 180.0)
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            drawLine(
                color = rayColor.copy(alpha = 0.92f),
                start = Offset(
                    center.x + rayInnerOffset * cosA,
                    center.y + rayInnerOffset * sinA
                ),
                end = Offset(
                    center.x + (rayInnerOffset + rayLength) * cosA,
                    center.y + (rayInnerOffset + rayLength) * sinA
                ),
                strokeWidth = rayStrokeWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Central disc
    drawCircle(color = coreColor, radius = baseRadius, center = center)

    if (isNearHorizon) {
        // At the horizon: warm shaded lower band, as the disc sits into the haze
        drawArc(
            color = limbColor.copy(alpha = 0.65f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
            size = Size(baseRadius * 2, baseRadius * 2)
        )
    } else {
        // High sun: subtle rim on the lower perimeter, plus a specular highlight
        drawArc(
            color = limbColor.copy(alpha = 0.35f),
            startAngle = 15f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(center.x - baseRadius * 0.92f, center.y - baseRadius * 0.92f),
            size = Size(baseRadius * 1.84f, baseRadius * 1.84f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 3.2.dp.toPx(),
            center = Offset(center.x - baseRadius * 0.38f, center.y - baseRadius * 0.38f)
        )
    }
}

/**
 * Renders the Moon with simple geometric vector aesthetics:
 * - Crisp porcelain white geometric lunar body
 * - Boolean circle subtraction for razor-sharp, authentic crescent curvature
 * - 3 clean geometric circular crater spots
 * - Concentric aura rings and clean earthshine outline
 */
private fun DrawScope.drawMoon(
    center: Offset,
    moonRadius: Float,
    illumination: Float,
    isWaxing: Boolean,
    alpha: Float
) {
    if (alpha <= 0.02f) return

    val moonColor = Color(0xFFF8F9FA).copy(alpha = alpha) // Crisp porcelain white
    val craterColor = Color(0xFF90A4AE).copy(alpha = 0.32f * alpha) // Vector crater tint

    // 1. Concentric aura rings
    drawCircle(
        color = Color.White.copy(alpha = 0.20f * alpha),
        radius = moonRadius * 1.48f,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.09f * alpha),
        radius = moonRadius * 1.88f,
        center = center,
        style = Stroke(width = 1.0.dp.toPx())
    )

    // 2. Earthshine (unlit side silhouette with clean vector boundary)
    drawCircle(color = Color(0xFFB0BEC5).copy(alpha = 0.14f * alpha), radius = moonRadius, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.16f * alpha),
        radius = moonRadius,
        center = center,
        style = Stroke(width = 1.0.dp.toPx())
    )

    // 3. Illuminated lunar body
    if (illumination >= 0.90f) {
        // Full moon (Badr): pure geometric disc
        drawCircle(color = moonColor, radius = moonRadius, center = center)
        for (crater in MoonCraters) {
            drawCircle(color = craterColor, radius = moonRadius * crater.scale, center = center.offset(crater))
        }
    } else {
        // Crescent to gibbous (Hilal, Tarbi', Ahdab) built by boolean geometry
        val normIllum = if (illumination <= 0.50f) {
            (illumination / 0.50f).coerceIn(0.04f, 1.0f)
        } else {
            ((illumination - 0.50f) / 0.50f).coerceIn(0f, 1f)
        }

        // The shadow circle slides across the lit disc: far off-centre and larger
        // for a thin crescent, overlapping for a gibbous.
        val shadowOffset = if (illumination <= 0.50f) {
            moonRadius * (1.95f - normIllum * 1.05f)
        } else {
            moonRadius * (1.0f - normIllum) * 1.25f + moonRadius
        }
        val shadowCenterX = if (isWaxing) {
            center.x - shadowOffset
        } else {
            center.x + shadowOffset
        }
        val shadowRadius = if (illumination <= 0.50f) {
            moonRadius * (1.02f + (1f - normIllum) * 0.25f)
        } else {
            moonRadius * 1.05f
        }

        val shadow = Path().apply { addOval(Offset(shadowCenterX, center.y).toRect(shadowRadius)) }
        val lit = Path()
        lit.op(Path().apply { addOval(center.toRect(moonRadius)) }, shadow, PathOperation.Difference)

        drawPath(path = lit, color = moonColor, style = Fill)

        // Clip craters inside the illuminated surface so they read as integrated
        clipPath(lit) {
            for (crater in MoonCraters) {
                drawCircle(color = craterColor, radius = moonRadius * crater.scale, center = center.offset(crater))
            }
        }

        // Crisp limb outline on the curved edge
        drawPath(
            path = lit,
            color = Color.White.copy(alpha = 0.55f * alpha),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/** A crater, positioned as a fraction of the moon's radius. */
private data class MoonCrater(val dx: Float, val dy: Float, val scale: Float)

private val MoonCraters = listOf(
    MoonCrater(-0.24f, -0.17f, 0.20f),
    MoonCrater(0.22f, 0.21f, 0.15f),
    MoonCrater(-0.06f, 0.30f, 0.11f)
)

private fun Offset.offset(crater: MoonCrater) = Offset(x + crater.dx, y + crater.dy)

private fun Offset.toRect(radius: Float) = Rect(
    left = x - radius,
    top = y - radius,
    right = x + radius,
    bottom = y + radius
)

private data class HorizonSilhouette(
    val backDune: Path,
    val frontDune: Path,
    val minaret: Path,
    val finial: Offset
)

/**
 * Builds static desert dune + minaret silhouette paths once per canvas size.
 */
private fun Density.buildHorizonSilhouette(width: Float, height: Float): HorizonSilhouette {
    val horizonY = height * 0.88f

    val backDune = Path().apply {
        moveTo(0f, height)
        lineTo(0f, horizonY * 0.94f)
        cubicTo(
            width * 0.28f, horizonY * 0.89f,
            width * 0.65f, horizonY * 0.97f,
            width, horizonY * 0.91f
        )
        lineTo(width, height)
        close()
    }

    val minaretX = width * 0.76f
    val minaretBaseY = horizonY * 0.93f
    val minaretTopY = minaretBaseY - 32.dp.toPx()

    val minaretPath = Path().apply {
        moveTo(minaretX - 3.5.dp.toPx(), minaretBaseY)
        lineTo(minaretX - 2.5.dp.toPx(), minaretTopY + 8.dp.toPx())
        lineTo(minaretX - 5.dp.toPx(), minaretTopY + 8.dp.toPx())
        lineTo(minaretX - 5.dp.toPx(), minaretTopY + 6.dp.toPx())
        lineTo(minaretX - 2.dp.toPx(), minaretTopY + 6.dp.toPx())
        lineTo(minaretX, minaretTopY)
        lineTo(minaretX + 2.dp.toPx(), minaretTopY + 6.dp.toPx())
        lineTo(minaretX + 5.dp.toPx(), minaretTopY + 6.dp.toPx())
        lineTo(minaretX + 5.dp.toPx(), minaretTopY + 8.dp.toPx())
        lineTo(minaretX + 2.5.dp.toPx(), minaretTopY + 8.dp.toPx())
        lineTo(minaretX + 3.5.dp.toPx(), minaretBaseY)
        close()
    }

    val frontDune = Path().apply {
        moveTo(0f, height)
        lineTo(0f, horizonY * 0.97f)
        cubicTo(
            width * 0.35f, horizonY * 1.02f,
            width * 0.72f, horizonY * 0.94f,
            width, horizonY * 0.98f
        )
        lineTo(width, height)
        close()
    }

    return HorizonSilhouette(
        backDune = backDune,
        frontDune = frontDune,
        minaret = minaretPath,
        finial = Offset(minaretX, minaretTopY - 2.dp.toPx())
    )
}
