package com.example.ui.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MagneticFieldStatus
import com.example.ui.SalahUiState
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.Tonal
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Qibla Direction Finder Component.
 * Integrates real-time device magnetometer and accelerometer sensor telemetry
 * to guide users accurately toward the Holy Kaaba in Mecca.
 */
@Composable
fun QiblaDirectionFinder(
    state: SalahUiState,
    onToggleTrueNorth: () -> Unit,
    onFetchLocation: () -> Unit,
    onShowCalibrationTip: () -> Unit = {},
    onShowSunVerification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isFacing = state.isFacingQibla
    val strings = LocalStrings.current
    val successColors = Tonal.colors
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val tertiaryColor = colorScheme.tertiary
    val errorColor = colorScheme.error
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant
    val outlineVariantColor = colorScheme.outlineVariant

    // Smooth spring rotation for heading.
    //
    // A compass heading is a circle, not a line. `compassAzimuth` is a raw
    // sensor value in [0, 360), so crossing north reports 359.8 then 0.2.
    // Handing that straight to animateFloatAsState makes the spring interpolate
    // the long way round - 359.8 -> 180 -> 0.2 - and the whole bezel spins a
    // full turn instead of advancing half a degree.
    //
    // So the target is unwrapped first: each reading is converted to the angle
    // nearest the current one and accumulated without bound. The spring then
    // always takes the short path, and `rotate` is unaffected because rotation
    // is only ever interpreted modulo 360.
    var unwrappedHeading by remember { mutableFloatStateOf(state.compassAzimuth) }
    LaunchedEffect(state.compassAzimuth) {
        // Signed shortest angular distance from current to target, in (-180, 180].
        val step = ((state.compassAzimuth - unwrappedHeading + 540f) % 360f) - 180f
        unwrappedHeading += step
    }
    val animatedHeading by animateFloatAsState(
        targetValue = unwrappedHeading,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatedHeading"
    )

    // Smooth color change when locked on Kaaba (theme-aware success)
    val alignedSuccess = successColors.success
    val alignedGold = tertiaryColor
    val dialRingColor by animateColorAsState(
        targetValue = if (isFacing) alignedSuccess else outlineVariantColor,
        label = "dialRingColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Guidance Direction Banner
        QiblaGuidanceBanner(
            state = state,
            onShowCalibrationTip = onShowCalibrationTip,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large Bearing & Heading Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", state.qiblaBearing)}°",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFacing) alignedSuccess else onSurfaceColor,
                    modifier = Modifier.testTag("qibla_bearing_text")
                )
                Text(
                    text = strings.kaabaDistance,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant
                )
            }

            // Divider Pill
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val cardinal = getCardinalDirection(state.compassAzimuth)
                Text(
                    text = "${state.compassAzimuth.toInt()}° $cardinal",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light,
                    color = onSurfaceColor,
                    modifier = Modifier.testTag("current_heading_text")
                )
                Text(
                    text = if (state.useTrueNorth) strings.trueNorth else strings.magneticNorth,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive 2D Sensor Compass Dial (responsive: fills width, capped for tablets)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .aspectRatio(1f)
                .testTag("qibla_sensor_compass_dial"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.width / 2f) - 20f

                // Alignment halo: soft success glow behind the dial when locked on
                if (isFacing) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                alignedSuccess.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = outerRadius + 18.dp.toPx()
                        ),
                        radius = outerRadius + 18.dp.toPx(),
                        center = center
                    )
                }

                // Outer Dial Background
                drawCircle(
                    color = surfaceColor,
                    radius = outerRadius,
                    center = center
                )

                // Bezel: hairline outer ring plus the expressive state ring
                drawCircle(
                    color = outlineVariantColor,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = dialRingColor,
                    radius = outerRadius - 5.dp.toPx(),
                    center = center,
                    style = Stroke(width = if (isFacing) 5.dp.toPx() else 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Subtly shaded compass track
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            surfaceColor.copy(alpha = 0.5f),
                            dialRingColor.copy(alpha = if (isFacing) 0.15f else 0.06f)
                        ),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius - 4.dp.toPx(),
                    center = center
                )

                // Rotating bezel: ticks, degree numerals and cardinal letters
                rotate(degrees = -animatedHeading, pivot = center) {
                    val numeralPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT
                    }
                    val cardinalPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 17.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    for (i in 0 until 72) {
                        val angle = i * 5.0
                        val angleRad = Math.toRadians(angle)
                        val isCardinal = i % 18 == 0
                        val isMajor = i % 6 == 0
                        val tickOuter = outerRadius - 10.dp.toPx()
                        val tickLen = when {
                            isCardinal -> 14.dp.toPx()
                            isMajor -> 9.dp.toPx()
                            else -> 4.5.dp.toPx()
                        }
                        val strokeWidth = if (isCardinal) 2.4.dp.toPx() else 1.dp.toPx()

                        val startX = (center.x + (tickOuter - tickLen) * sin(angleRad)).toFloat()
                        val startY = (center.y - (tickOuter - tickLen) * cos(angleRad)).toFloat()
                        val endX = (center.x + tickOuter * sin(angleRad)).toFloat()
                        val endY = (center.y - tickOuter * cos(angleRad)).toFloat()

                        drawLine(
                            color = when {
                                angle == 0.0 -> errorColor // North
                                isCardinal -> primaryColor
                                else -> dialRingColor
                            },
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Degree numerals every 30° (cardinal slots carry letters instead)
                    numeralPaint.color = onSurfaceVariant.toArgb()
                    numeralPaint.alpha = 204
                    for (deg in 0 until 360 step 30) {
                        if (deg % 90 == 0) continue
                        val rad = Math.toRadians(deg.toDouble())
                        val r = outerRadius - 32.dp.toPx()
                        val x = (center.x + r * sin(rad)).toFloat()
                        val y = (center.y - r * cos(rad)).toFloat() + (numeralPaint.textSize / 3)
                        drawContext.canvas.nativeCanvas.drawText("$deg", x, y, numeralPaint)
                    }

                    // Cardinal letters
                    listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W").forEach { (deg, label) ->
                        val rad = Math.toRadians(deg.toDouble())
                        cardinalPaint.color =
                            if (deg == 0) errorColor.toArgb() else onSurfaceColor.toArgb()
                        val r = outerRadius - 32.dp.toPx()
                        val x = (center.x + r * sin(rad)).toFloat()
                        val y = (center.y - r * cos(rad)).toFloat() + (cardinalPaint.textSize / 3)
                        drawContext.canvas.nativeCanvas.drawText(label, x, y, cardinalPaint)
                    }

                    // Tapered Qibla needle towards the Kaaba
                    val qiblaRad = Math.toRadians(state.qiblaBearing.toDouble())
                    val dirX = sin(qiblaRad).toFloat()
                    val dirY = (-cos(qiblaRad)).toFloat()
                    val needleReach = outerRadius - 56.dp.toPx()
                    val tipX = center.x + dirX * needleReach
                    val tipY = center.y + dirY * needleReach
                    val baseDist = 30.dp.toPx()
                    val baseX = center.x - dirX * baseDist
                    val baseY = center.y - dirY * baseDist
                    val halfWidth = 6.5.dp.toPx()
                    val perpX = -dirY
                    val perpY = dirX
                    val needlePath = Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(baseX + perpX * halfWidth, baseY + perpY * halfWidth)
                        lineTo(baseX - perpX * halfWidth, baseY - perpY * halfWidth)
                        close()
                    }
                    drawPath(
                        path = needlePath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isFacing) alignedSuccess else primaryColor,
                                if (isFacing) alignedSuccess else alignedGold
                            ),
                            start = Offset(baseX, baseY),
                            end = Offset(tipX, tipY)
                        ),
                        style = Fill
                    )

                    // Kaaba target disc with halo ring
                    val discCenter = Offset(tipX, tipY)
                    drawCircle(
                        color = (if (isFacing) alignedSuccess else alignedGold).copy(alpha = 0.25f),
                        radius = 19.dp.toPx(),
                        center = discCenter
                    )
                    drawCircle(
                        color = if (isFacing) alignedSuccess else alignedGold,
                        radius = 13.dp.toPx(),
                        center = discCenter
                    )
                    drawCircle(
                        color = if (isFacing) successColors.onSuccess else colorScheme.onTertiary,
                        radius = 4.dp.toPx(),
                        center = discCenter
                    )
                }

                // Top Device Sighting Arrow (Static reference pointing forward)
                val arrowTipY = center.y - outerRadius - 10.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(center.x, arrowTipY)
                    lineTo(center.x - 7.dp.toPx(), arrowTipY + 12.dp.toPx())
                    lineTo(center.x + 7.dp.toPx(), arrowTipY + 12.dp.toPx())
                    close()
                }
                drawPath(
                    path = arrowPath,
                    color = if (isFacing) alignedSuccess else primaryColor,
                    style = Fill
                )

                // 2D Spirit Bubble Level in center (Accelerometer Sensor)
                val levelRingRadius = 24.dp.toPx()
                drawCircle(
                    color = if (state.isDeviceLevel) alignedSuccess.copy(alpha = 0.25f) else outlineVariantColor.copy(alpha = 0.35f),
                    radius = levelRingRadius,
                    center = center
                )
                drawCircle(
                    color = if (state.isDeviceLevel) alignedSuccess else outlineVariantColor,
                    radius = levelRingRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Accelerometer Bubble
                val maxBubbleDisplacement = levelRingRadius - 6.dp.toPx()
                val bubbleOffsetX = (state.rollDegrees / 45f).coerceIn(-1f, 1f) * maxBubbleDisplacement
                val bubbleOffsetY = (-state.pitchDegrees / 45f).coerceIn(-1f, 1f) * maxBubbleDisplacement
                val bubbleCenter = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)

                drawCircle(
                    color = if (state.isDeviceLevel) alignedSuccess else alignedGold,
                    radius = 5.5.dp.toPx(),
                    center = bubbleCenter
                )
            }

            // Central Icon (Kaaba indicator) with expressive spring pop
            val badgeScale by animateFloatAsState(
                targetValue = if (isFacing) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "badge_scale"
            )
            if (badgeScale > 0.02f) {
                Surface(
                    shape = CircleShape,
                    color = alignedSuccess,
                    contentColor = successColors.onSuccess,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = badgeScale
                            scaleY = badgeScale
                            alpha = badgeScale.coerceIn(0f, 1f)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Aligned",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Tilt Alert Reminder if device is not held flat
        AnimatedVisibility(
            visible = !state.isDeviceLevel,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = alignedGold,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Hold device flat for optimal compass precision",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Telemetry & Quick Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Kaaba Distance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,d", state.distanceToKaabaKm)} km",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = "Great-Circle to Mecca",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
            }

            // North Mode Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleTrueNorth() }
                    .testTag("north_mode_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reference",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (state.useTrueNorth) strings.trueNorth else strings.magneticNorth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = "Tap to switch",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Location Services & Offline Caching Card
        LocationStatusCard(
            state = state,
            onFetchLocation = onFetchLocation,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sensor Diagnostics and Sun Verification Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sun Verification Button
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onShowSunVerification() }
                    .testTag("verify_with_sun_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = alignedGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verify with Sun",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor
                    )
                }
            }

            // Sensor Calibration Button
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onShowCalibrationTip() }
                    .testTag("sensor_calibration_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${state.magneticFieldMagnitude.toInt()} µT · Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor
                    )
                }
            }
        }
    }
}

/**
 * Visual Guidance Banner telling the user exactly how many degrees to turn.
 */
@Composable
private fun QiblaGuidanceBanner(
    state: SalahUiState,
    onShowCalibrationTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeAngle = state.relativeQiblaAngle
    val isFacing = state.isFacingQibla
    val successColors = Tonal.colors
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current

    val (bannerColor, contentColor, guidanceText) = when {
        isFacing -> Triple(
            successColors.success,
            successColors.onSuccess,
            // Localized. All three of these were built inline in English, so
            // they stayed English in all twelve languages - which is how an
            // Arabic or Urdu user got English guidance above a localized dial.
            strings.more.alignedWithQibla
        )
        relativeAngle > 0 -> Triple(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
            strings.more.turnBy.format(
                String.format(Locale.getDefault(), "%.0f°", abs(relativeAngle)),
                strings.more.rightOfQibla
            )
        )
        else -> Triple(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
            strings.more.turnBy.format(
                String.format(Locale.getDefault(), "%.0f°", abs(relativeAngle)),
                strings.more.leftOfQibla
            )
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = bannerColor,
        contentColor = contentColor,
        modifier = modifier
            .testTag("qibla_guidance_banner")
            .clickable {
                if (!isFacing && state.magneticStatus == MagneticFieldStatus.INTERFERENCE) {
                    onShowCalibrationTip()
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isFacing) Icons.Default.CheckCircle else Icons.Default.NearMe,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Card displaying current coordinates, offline caching status, and GPS fetch action.
 */
@Composable
private fun LocationStatusCard(
    state: SalahUiState,
    onFetchLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successColors = Tonal.colors
    val strings = LocalStrings.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.testTag("location_status_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "${state.location.name}, ${state.location.country}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.4f° N, %.4f° E · %s",
                                state.location.latitude,
                                state.location.longitude,
                                if (state.location.isGps) strings.more.gpsCached else strings.more.selectedCity
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Locate Me Button
                Button(
                    onClick = onFetchLocation,
                    enabled = !state.isLocating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
                    modifier = Modifier.testTag("fetch_gps_location_button")
                ) {
                    if (state.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = strings.more.locateMe,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.more.locateMe,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Offline Caching Confirmation
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = successColors.success,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = strings.more.coordinatesCachedOffline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCardinalDirection(azimuth: Float): String {
    val norm = ((azimuth % 360f) + 360f) % 360f
    return when {
        norm in 22.5f..67.5f -> "NE"
        norm in 67.5f..112.5f -> "E"
        norm in 112.5f..157.5f -> "SE"
        norm in 157.5f..202.5f -> "S"
        norm in 202.5f..247.5f -> "SW"
        norm in 247.5f..292.5f -> "W"
        norm in 292.5f..337.5f -> "NW"
        else -> "N"
    }
}
