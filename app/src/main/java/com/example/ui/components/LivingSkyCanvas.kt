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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.model.UserLocation
import com.example.ui.theme.Motion
import com.example.engine.AstronomicalSky
import com.example.engine.MoonPhaseInfo
import com.example.engine.QiblaEngine
import com.example.engine.SkyColorPalette
import com.example.engine.SkyPeriod
import com.example.engine.SunPosition
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stable Star definition for astronomical sky rendering
 */
private data class AstroStar(
    val normX: Float,
    val normY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val phaseOffset: Float
)

/**
 * Constellation line segment connecting two normalized coordinates
 */
private data class ConstellationLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

@Composable
fun LivingSkyCanvas(
    skyPeriod: SkyPeriod,
    celestialProgress: Float,
    modifier: Modifier = Modifier,
    sunPosition: SunPosition? = null,
    location: UserLocation? = null,
    currentTime: LocalTime = LocalTime.now(),
    hijriDay: Int = 14
) {
    // 1. Resolve exact solar altitude and azimuth based on user location and time
    val effectiveSunPosition = remember(sunPosition, location, currentTime) {
        sunPosition ?: run {
            val loc = location ?: UserLocation.RABAT
            val ldt = LocalDateTime.of(
                java.time.LocalDate.now(),
                currentTime
            )
            QiblaEngine.calculateSunPosition(loc, ldt)
        }
    }

    val sunAltitude = effectiveSunPosition.altitude
    val sunAzimuth = effectiveSunPosition.azimuth
    val isSetting = sunAzimuth > 180f

    // 2. Compute dynamic continuous sky palette
    val rawPalette: SkyColorPalette = remember(sunAltitude, isSetting) {
        AstronomicalSky.calculateContinuousSkyColors(sunAltitude, isSetting)
    }

    // Smooth color transitions across sky changes
    val colorAnimSpec = tween<Color>(durationMillis = Motion.duration(1600), easing = FastOutSlowInEasing)
    val animatedZenith by animateColorAsState(targetValue = rawPalette.zenithColor, animationSpec = colorAnimSpec, label = "zenith")
    val animatedMidSky by animateColorAsState(targetValue = rawPalette.midSkyColor, animationSpec = colorAnimSpec, label = "midSky")
    val animatedHorizon by animateColorAsState(targetValue = rawPalette.horizonColor, animationSpec = colorAnimSpec, label = "horizon")
    val animatedHorizonHaze by animateColorAsState(targetValue = rawPalette.horizonHazeColor, animationSpec = colorAnimSpec, label = "haze")
    val animatedSunColor by animateColorAsState(targetValue = rawPalette.sunColor, animationSpec = colorAnimSpec, label = "sunColor")
    val animatedSunHalo by animateColorAsState(targetValue = rawPalette.sunHaloColor, animationSpec = colorAnimSpec, label = "sunHalo")
    val animatedCloudTint by animateColorAsState(targetValue = rawPalette.cloudTint, animationSpec = colorAnimSpec, label = "cloudTint")

    // Moon phase based on lunar Hijri day
    val moonPhase: MoonPhaseInfo = remember(hijriDay) {
        AstronomicalSky.getMoonPhaseInfo(hijriDay)
    }

    // 3. Infinite animations for living atmosphere — only animate what this sky period needs.
    // Night twinkle/meteor paused during full day; sun rays/corona paused when sun is below horizon.
    // When the user has reduced motion enabled, none of these loops start: the sky
    // renders the same scene statically (large, slow, full-screen movement is the
    // classic vestibular trigger, so this matters more here than almost anywhere).
    val needStars = rawPalette.starAlpha > 0.03f || rawPalette.isNight || sunAltitude < 6f
    val needSunFx = sunAltitude > -8f
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

    val cloudDrift by atmosphere.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reducedMotion) 1 else 65_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudDrift"
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

    // 4. Stable starfield coordinates & constellations (Ursa Major, Orion Belt)
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
            val normX = rand.nextFloat()
            val normY = rand.nextFloat() * 0.72f // Upper 72% of sky
            val radius = rand.nextFloat() * 1.8f + 0.6f
            val baseAlpha = rand.nextFloat() * 0.55f + 0.45f
            val color = starColors[rand.nextInt(starColors.size)]
            val speed = rand.nextFloat() * 1.6f + 0.7f
            val phase = rand.nextFloat() * (2 * PI).toFloat()
            AstroStar(normX, normY, radius, baseAlpha, color, speed, phase)
        }
    }

    // Ursa Major (The Big Dipper / الدب الأكبر) constellation segments
    val bigDipper = remember {
        listOf(
            ConstellationLine(0.18f, 0.14f, 0.24f, 0.16f), // Alkaid to Mizar
            ConstellationLine(0.24f, 0.16f, 0.29f, 0.20f), // Mizar to Alioth
            ConstellationLine(0.29f, 0.20f, 0.35f, 0.22f), // Alioth to Megrez
            ConstellationLine(0.35f, 0.22f, 0.36f, 0.28f), // Megrez to Phecda
            ConstellationLine(0.36f, 0.28f, 0.44f, 0.27f), // Phecda to Merak
            ConstellationLine(0.44f, 0.27f, 0.43f, 0.21f), // Merak to Dubhe
            ConstellationLine(0.43f, 0.21f, 0.35f, 0.22f)  // Dubhe to Megrez
        )
    }

    // Orion's Belt (حزام الجبار)
    val orionBelt = remember {
        listOf(
            ConstellationLine(0.72f, 0.18f, 0.76f, 0.20f), // Alnitak to Alnilam
            ConstellationLine(0.76f, 0.20f, 0.80f, 0.22f)  // Alnilam to Mintaka
        )
    }

    // Static silhouette geometry does not depend on animation values — build once per size.
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val staticSilhouette = remember(canvasW, canvasH, density) {
        if (canvasW <= 0f || canvasH <= 0f) null
        else with(density) { buildHorizonSilhouette(canvasW, canvasH) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasW = size.width.toFloat()
                    canvasH = size.height.toFloat()
                }
        ) {
            val width = size.width
            val height = size.height
            val arcZoneHeight = minOf(height * 0.50f, 380.dp.toPx())
            val horizonY = height * 0.88f

            // A. Primary Living Sky Vertical Gradient
            val skyBrush = Brush.verticalGradient(
                colors = listOf(animatedZenith, animatedMidSky, animatedHorizon),
                startY = 0f,
                endY = horizonY
            )
            drawRect(brush = skyBrush, size = size)

            // B. Horizon Atmospheric Diffusion / Belt of Venus Glow
            if (animatedHorizonHaze.alpha > 0.05f) {
                // Calculate sun azimuth position mapped to screen width
                val sunNormX = when {
                    sunAzimuth < 60f -> 0.15f
                    sunAzimuth > 300f -> 0.85f
                    else -> ((sunAzimuth - 60f) / 240f).coerceIn(0.1f, 0.9f)
                }
                val hazeCenter = Offset(width * sunNormX, horizonY * 0.92f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedHorizonHaze,
                            animatedHorizonHaze.copy(alpha = animatedHorizonHaze.alpha * 0.4f),
                            Color.Transparent
                        ),
                        center = hazeCenter,
                        radius = width * 0.75f
                    ),
                    size = size
                )
            }

            // C. Living Starfield & Constellations
            val starAlpha = rawPalette.starAlpha
            if (starAlpha > 0.03f) {
                // 1. Constellation guide paths
                val constellationAlpha = (starAlpha * 0.28f).coerceIn(0f, 0.35f)
                val constellationColor = Color.White.copy(alpha = constellationAlpha)

                for (line in bigDipper) {
                    drawLine(
                        color = constellationColor,
                        start = Offset(line.x1 * width, line.y1 * height),
                        end = Offset(line.x2 * width, line.y2 * height),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                for (line in orionBelt) {
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
                    val modAlpha = (star.baseAlpha * (0.35f + 0.65f * ((twinkle + 1f) / 2f)) * starAlpha).coerceIn(0f, 1f)

                    if (modAlpha > 0.02f) {
                        val center = Offset(star.normX * width, star.normY * height)
                        // Star center core
                        drawCircle(
                            color = star.color.copy(alpha = modAlpha),
                            radius = star.radius.dp.toPx(),
                            center = center
                        )
                        // Subtle star diffraction shimmer for bright stars
                        if (star.radius > 1.8f && modAlpha > 0.65f) {
                            drawCircle(
                                color = star.color.copy(alpha = modAlpha * 0.25f),
                                radius = (star.radius * 2.6f).dp.toPx(),
                                center = center
                            )
                        }
                    }
                }

                // 3. Periodic Shooting Star / Meteor Streak
                // Active during meteorCycle window [0.18 .. 0.24] (~840ms duration)
                if ((rawPalette.isNight || starAlpha > 0.45f) && meteorCycle in 0.18f..0.24f) {
                    val meteorT = (meteorCycle - 0.18f) / 0.06f // 0f to 1f
                    val startX = width * 0.72f
                    val startY = height * 0.08f
                    val streakLen = 140.dp.toPx()
                    val dx = -180.dp.toPx()
                    val dy = 95.dp.toPx()

                    val headX = startX + dx * meteorT
                    val headY = startY + dy * meteorT
                    val tailX = headX - (dx / 2.2f)
                    val tailY = headY - (dy / 2.2f)

                    val meteorAlpha = sin(meteorT * PI.toFloat()).coerceIn(0f, 1f) * starAlpha
                    if (meteorAlpha > 0.05f) {
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFFD54F).copy(alpha = meteorAlpha * 0.6f),
                                    Color.White.copy(alpha = meteorAlpha)
                                ),
                                start = Offset(tailX, tailY),
                                end = Offset(headX, headY)
                            ),
                            start = Offset(tailX, tailY),
                            end = Offset(headX, headY),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = meteorAlpha),
                            radius = 2.5.dp.toPx(),
                            center = Offset(headX, headY)
                        )
                    }
                }
            }

            // D. Living Sun (Google Weather-inspired Simple Geometric Vector Aesthetics)
            if (sunAltitude > -8.0f) {
                // Determine screen coordinates from solar altitude & azimuth
                val sunNormX = when {
                    sunAzimuth < 60f -> 0.12f
                    sunAzimuth > 300f -> 0.88f
                    else -> ((sunAzimuth - 60f) / 240f).coerceIn(0.12f, 0.88f)
                }
                val sunX = width * sunNormX

                // Map altitude (-8° to 90°) to vertical position
                val altClamped = sunAltitude.coerceIn(-8f, 85f)
                val altProgress = ((altClamped - (-8f)) / 93f).coerceIn(0f, 1f)
                val zenithY = arcZoneHeight * 0.22f
                val baseHorizonY = arcZoneHeight * 0.88f
                val sunY = baseHorizonY - (baseHorizonY - zenithY) * altProgress

                val sunCenter = Offset(sunX, sunY)

                drawGoogleWeatherSun(
                    sunCenter = sunCenter,
                    sunAltitude = sunAltitude,
                    sunColor = animatedSunColor,
                    sunHaloColor = animatedSunHalo,
                    coronaPulse = coronaPulse,
                    rotationDegrees = rayRotation
                )
            }

            // E. Living Moon (Google Weather-inspired Simple Geometric Vector Aesthetics with Hijri Phase)
            if (sunAltitude < 8.0f || rawPalette.isNight) {
                val moonX = width * 0.80f
                val moonY = arcZoneHeight * 0.32f
                val moonRadius = 24.dp.toPx()
                val moonCenter = Offset(moonX, moonY)

                val moonAlpha = when {
                    sunAltitude <= -12f -> 1.0f
                    sunAltitude <= 0f -> 0.85f
                    else -> (1.0f - (sunAltitude / 8f)).coerceIn(0f, 1f)
                }

                if (moonAlpha > 0.05f) {
                    drawGoogleWeatherMoon(
                        moonCenter = moonCenter,
                        moonRadius = moonRadius,
                        illumination = moonPhase.illumination,
                        isWaxing = moonPhase.isWaxing,
                        alpha = moonAlpha
                    )
                }
            }

            // F. Floating Atmospheric Cloud Wisps (Dynamic drift with sunlight tint)
            drawAtmosphericClouds(
                width = width,
                height = height,
                drift = cloudDrift,
                cloudTint = animatedCloudTint
            )

            // G. Horizon Silhouette with Desert Dunes & Architectural Minaret (static paths)
            staticSilhouette?.let { sil ->
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
}

/**
 * Renders the Sun with Google Weather-inspired simple geometric vector aesthetics:
 * - Bold, vibrant geometric solar disc (#FBBC04 sunshine gold / #FF7043 horizon coral)
 * - 8 iconic geometric capsule rays with rounded caps
 * - Concentric geometric vector orbital aura rings
 * - Clean geometric specular core and subtle vector duo-tone depth
 */
private fun DrawScope.drawGoogleWeatherSun(
    sunCenter: Offset,
    sunAltitude: Float,
    sunColor: Color,
    sunHaloColor: Color,
    coronaPulse: Float,
    rotationDegrees: Float
) {
    val baseRadius = 21.dp.toPx()
    val isNearHorizon = sunAltitude in -8f..6f

    // Google Weather signature solar palette (vibrant gold with warm amber depth)
    val coreColor = if (isNearHorizon) {
        Color(0xFFFF7043) // Warm horizon coral
    } else {
        Color(0xFFFBBC04) // Google's iconic vibrant sunshine gold
    }
    val rayColor = if (isNearHorizon) {
        Color(0xFFFFA726)
    } else {
        Color(0xFFF9AB00)
    }

    // 1. Concentric Geometric Vector Aura Rings (Material vector orbit, not fuzzy blur)
    val ring1Radius = baseRadius * 1.65f * coronaPulse
    val ring2Radius = baseRadius * 2.30f * coronaPulse

    drawCircle(
        color = coreColor.copy(alpha = 0.16f),
        radius = ring2Radius,
        center = sunCenter,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = coreColor.copy(alpha = 0.28f),
        radius = ring1Radius,
        center = sunCenter,
        style = Stroke(width = 1.5.dp.toPx())
    )
    // Soft subtle inner disk backing (crisp flat vector fill)
    drawCircle(
        color = coreColor.copy(alpha = 0.07f),
        radius = ring1Radius,
        center = sunCenter
    )

    // 2. Google Weather Iconic Geometric Capsule Rays
    // 8 equidistant radial rays with rounded caps
    val rayInnerOffset = baseRadius + 6.dp.toPx()
    val rayLength = 7.5.dp.toPx() * coronaPulse
    val rayStrokeWidth = 3.6.dp.toPx()

    rotate(degrees = rotationDegrees, pivot = sunCenter) {
        for (i in 0 until 8) {
            val angleRad = (i * 45.0) * (PI / 180.0)
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            val startP = Offset(
                sunCenter.x + rayInnerOffset * cosA,
                sunCenter.y + rayInnerOffset * sinA
            )
            val endP = Offset(
                sunCenter.x + (rayInnerOffset + rayLength) * cosA,
                sunCenter.y + (rayInnerOffset + rayLength) * sinA
            )

            drawLine(
                color = rayColor.copy(alpha = 0.92f),
                start = startP,
                end = endP,
                strokeWidth = rayStrokeWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Central Geometric Solar Disc
    if (isNearHorizon) {
        // At horizon: crisp geometric vector disc with warm lower horizon band
        drawCircle(
            color = coreColor,
            radius = baseRadius,
            center = sunCenter
        )
        // Warm lower vector crescent / shadow band
        drawArc(
            color = Color(0xFFD84315).copy(alpha = 0.65f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(sunCenter.x - baseRadius, sunCenter.y - baseRadius),
            size = Size(baseRadius * 2, baseRadius * 2)
        )
    } else {
        // Daytime: Solid brilliant vector sun disc
        drawCircle(
            color = coreColor,
            radius = baseRadius,
            center = sunCenter
        )
        // Subtle duo-tone geometric depth (Google vector style warm accent on lower perimeter)
        drawArc(
            color = Color(0xFFF29900).copy(alpha = 0.35f),
            startAngle = 15f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(sunCenter.x - baseRadius * 0.92f, sunCenter.y - baseRadius * 0.92f),
            size = Size(baseRadius * 1.84f, baseRadius * 1.84f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        // Clean geometric specular highlight dot (top-left)
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 3.2.dp.toPx(),
            center = Offset(sunCenter.x - baseRadius * 0.38f, sunCenter.y - baseRadius * 0.38f)
        )
    }
}

/**
 * Renders the Moon with Google Weather-inspired simple geometric vector aesthetics:
 * - Crisp porcelain white (#F8F9FA) geometric lunar body
 * - Boolean circle subtraction for razor-sharp, authentic crescent curvature
 * - 3 clean geometric circular crater spots (Google Weather signature maria)
 * - Concentric geometric vector aura rings and clean earthshine outline
 */
private fun DrawScope.drawGoogleWeatherMoon(
    moonCenter: Offset,
    moonRadius: Float,
    illumination: Float,
    isWaxing: Boolean,
    alpha: Float
) {
    if (alpha <= 0.02f) return

    val moonColor = Color(0xFFF8F9FA).copy(alpha = alpha) // Crisp Google porcelain white
    val craterColor = Color(0xFF90A4AE).copy(alpha = 0.32f * alpha) // Clean vector crater tint

    // 1. Concentric Geometric Vector Aura Rings
    drawCircle(
        color = Color.White.copy(alpha = 0.20f * alpha),
        radius = moonRadius * 1.48f,
        center = moonCenter,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.09f * alpha),
        radius = moonRadius * 1.88f,
        center = moonCenter,
        style = Stroke(width = 1.0.dp.toPx())
    )

    // 2. Geometric Earthshine (Unlit side silhouette with clean vector boundary)
    drawCircle(
        color = Color(0xFFB0BEC5).copy(alpha = 0.14f * alpha),
        radius = moonRadius,
        center = moonCenter
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f * alpha),
        radius = moonRadius,
        center = moonCenter,
        style = Stroke(width = 1.0.dp.toPx())
    )

    // 3. Illuminated Lunar Body (Geometric vector construction)
    if (illumination >= 0.90f) {
        // Full Moon (Badr): Pure crisp geometric disc
        drawCircle(
            color = moonColor,
            radius = moonRadius,
            center = moonCenter
        )

        // 3 iconic Google Weather geometric circular craters
        drawCircle(
            color = craterColor,
            radius = moonRadius * 0.22f,
            center = Offset(moonCenter.x - moonRadius * 0.26f, moonCenter.y - moonRadius * 0.18f)
        )
        drawCircle(
            color = craterColor,
            radius = moonRadius * 0.16f,
            center = Offset(moonCenter.x + moonRadius * 0.24f, moonCenter.y + moonRadius * 0.22f)
        )
        drawCircle(
            color = craterColor,
            radius = moonRadius * 0.12f,
            center = Offset(moonCenter.x - moonRadius * 0.06f, moonCenter.y + moonRadius * 0.32f)
        )
    } else {
        // Crescent to Gibbous (Hilal, Tarbi', Ahdab)
        // Construct clean vector path using boolean geometry
        val baseCirclePath = Path().apply {
            addOval(
                Rect(
                    moonCenter.x - moonRadius,
                    moonCenter.y - moonRadius,
                    moonCenter.x + moonRadius,
                    moonCenter.y + moonRadius
                )
            )
        }

        val illuminatedPath = Path()

        if (illumination <= 0.50f) {
            // Crescent: Base circle MINUS an offset subtractive circle
            val normIllum = (illumination / 0.50f).coerceIn(0.04f, 1.0f)
            val shadowOffset = moonRadius * (1.95f - normIllum * 1.05f)
            val shadowCenter = Offset(
                if (isWaxing) moonCenter.x - shadowOffset else moonCenter.x + shadowOffset,
                moonCenter.y
            )
            val shadowRadius = moonRadius * (1.02f + (1f - normIllum) * 0.25f)
            val shadowCirclePath = Path().apply {
                addOval(
                    Rect(
                        shadowCenter.x - shadowRadius,
                        shadowCenter.y - shadowRadius,
                        shadowCenter.x + shadowRadius,
                        shadowCenter.y + shadowRadius
                    )
                )
            }
            illuminatedPath.op(baseCirclePath, shadowCirclePath, PathOperation.Difference)
        } else {
            // Gibbous
            val normIllum = ((illumination - 0.50f) / 0.50f).coerceIn(0f, 1f)
            val shadowOffset = moonRadius * (1.0f - normIllum) * 1.25f
            val shadowCenter = Offset(
                if (isWaxing) moonCenter.x - shadowOffset - moonRadius else moonCenter.x + shadowOffset + moonRadius,
                moonCenter.y
            )
            val shadowRadius = moonRadius * 1.05f
            val shadowCirclePath = Path().apply {
                addOval(
                    Rect(
                        shadowCenter.x - shadowRadius,
                        shadowCenter.y - shadowRadius,
                        shadowCenter.x + shadowRadius,
                        shadowCenter.y + shadowRadius
                    )
                )
            }
            illuminatedPath.op(baseCirclePath, shadowCirclePath, PathOperation.Difference)
        }

        // Draw the illuminated vector shape
        drawPath(
            path = illuminatedPath,
            color = moonColor,
            style = Fill
        )

        // Clip craters inside illuminated surface so they look integrated
        clipPath(illuminatedPath) {
            drawCircle(
                color = craterColor,
                radius = moonRadius * 0.22f,
                center = Offset(moonCenter.x - moonRadius * 0.22f, moonCenter.y - moonRadius * 0.16f)
            )
            drawCircle(
                color = craterColor,
                radius = moonRadius * 0.15f,
                center = Offset(moonCenter.x + moonRadius * 0.20f, moonCenter.y + moonRadius * 0.20f)
            )
            drawCircle(
                color = craterColor,
                radius = moonRadius * 0.11f,
                center = Offset(moonCenter.x - moonRadius * 0.05f, moonCenter.y + moonRadius * 0.28f)
            )
        }

        // Crisp vector limb outline on the curved edge
        drawPath(
            path = illuminatedPath,
            color = Color.White.copy(alpha = 0.55f * alpha),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/**
 * Renders layered atmospheric cloud wisps with continuous horizontal drift.
 */
private fun DrawScope.drawAtmosphericClouds(
    width: Float,
    height: Float,
    drift: Float,
    cloudTint: Color
) {
    if (cloudTint.alpha < 0.04f) return

    val driftPx1 = (drift * width * 1.2f) % (width * 1.5f) - (width * 0.25f)
    val driftPx2 = ((drift * 0.7f + 0.4f) * width * 1.2f) % (width * 1.5f) - (width * 0.25f)

    // High Atmospheric Wisp (Layer 1)
    val cloudPath1 = Path().apply {
        val y1 = height * 0.24f
        moveTo(-width * 0.2f + driftPx1, y1)
        cubicTo(
            width * 0.15f + driftPx1, y1 - 18.dp.toPx(),
            width * 0.45f + driftPx1, y1 + 12.dp.toPx(),
            width * 0.75f + driftPx1, y1 - 10.dp.toPx()
        )
        cubicTo(
            width * 0.95f + driftPx1, y1 - 4.dp.toPx(),
            width * 1.15f + driftPx1, y1 + 14.dp.toPx(),
            width * 1.35f + driftPx1, y1
        )
        lineTo(width * 1.35f + driftPx1, y1 + 22.dp.toPx())
        cubicTo(
            width * 1.05f + driftPx1, y1 + 34.dp.toPx(),
            width * 0.65f + driftPx1, y1 + 16.dp.toPx(),
            width * 0.25f + driftPx1, y1 + 28.dp.toPx()
        )
        close()
    }
    drawPath(path = cloudPath1, color = cloudTint.copy(alpha = cloudTint.alpha * 0.35f), style = Fill)

    // Mid-Atmospheric Cirrus Vapor (Layer 2)
    val cloudPath2 = Path().apply {
        val y2 = height * 0.42f
        moveTo(-width * 0.2f + driftPx2, y2)
        cubicTo(
            width * 0.20f + driftPx2, y2 + 14.dp.toPx(),
            width * 0.50f + driftPx2, y2 - 20.dp.toPx(),
            width * 0.80f + driftPx2, y2 + 8.dp.toPx()
        )
        cubicTo(
            width * 1.00f + driftPx2, y2 + 16.dp.toPx(),
            width * 1.20f + driftPx2, y2 - 6.dp.toPx(),
            width * 1.40f + driftPx2, y2
        )
        lineTo(width * 1.40f + driftPx2, y2 + 25.dp.toPx())
        cubicTo(
            width * 0.90f + driftPx2, y2 + 35.dp.toPx(),
            width * 0.40f + driftPx2, y2 + 18.dp.toPx(),
            -width * 0.2f + driftPx2, y2 + 28.dp.toPx()
        )
        close()
    }
    drawPath(path = cloudPath2, color = cloudTint.copy(alpha = cloudTint.alpha * 0.45f), style = Fill)
}

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

