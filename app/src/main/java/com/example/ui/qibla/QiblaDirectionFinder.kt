package com.example.ui.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MagneticFieldStatus
import com.example.ui.SalahUiState
import com.example.ui.localization.LocalStrings
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    // Smooth spring rotation for heading
    val animatedHeading by animateFloatAsState(
        targetValue = state.compassAzimuth,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatedHeading"
    )

    // Smooth color change when locked on Kaaba
    val alignedGold = Color(0xFFFFB300)
    val alignedGreen = Color(0xFF00C853)
    val dialRingColor by animateColorAsState(
        targetValue = if (isFacing) alignedGreen else outlineVariantColor,
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
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = if (isFacing) alignedGreen else onSurfaceColor,
                    modifier = Modifier.testTag("qibla_bearing_text")
                )
                Text(
                    text = strings.kaabaDistance,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant
                )
            }

            // Divider Pill
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val cardinal = getCardinalDirection(state.compassAzimuth)
                Text(
                    text = "${state.compassAzimuth.toInt()}° $cardinal",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                    color = onSurfaceColor,
                    modifier = Modifier.testTag("current_heading_text")
                )
                Text(
                    text = if (state.useTrueNorth) strings.trueNorth else strings.magneticNorth,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive 2D Sensor Compass Dial (Fusion of Magnetometer & Accelerometer)
        Box(
            modifier = Modifier
                .size(290.dp)
                .testTag("qibla_sensor_compass_dial"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.width / 2f) - 20f

                // Outer Dial Background
                drawCircle(
                    color = surfaceColor,
                    radius = outerRadius,
                    center = center
                )

                // Outer Border Ring
                drawCircle(
                    color = dialRingColor,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = if (isFacing) 4.dp.toPx() else 1.8.dp.toPx())
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

                // Rotating dial markings (Magnetometer azimuth)
                rotate(degrees = -animatedHeading, pivot = center) {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    for (i in 0 until 72) {
                        val angle = i * 5.0
                        val angleRad = Math.toRadians(angle)
                        val isCardinal = i % 18 == 0
                        val isMajor = i % 6 == 0
                        val tickLen = when {
                            isCardinal -> 16.dp.toPx()
                            isMajor -> 10.dp.toPx()
                            else -> 5.dp.toPx()
                        }
                        val strokeWidth = if (isCardinal) 2.4.dp.toPx() else 1.dp.toPx()

                        val startX = (center.x + (outerRadius - tickLen) * sin(angleRad)).toFloat()
                        val startY = (center.y - (outerRadius - tickLen) * cos(angleRad)).toFloat()
                        val endX = (center.x + outerRadius * sin(angleRad)).toFloat()
                        val endY = (center.y - outerRadius * cos(angleRad)).toFloat()

                        drawLine(
                            color = when {
                                angle == 0.0 -> Color(0xFFEF4444) // North
                                isCardinal -> primaryColor
                                else -> dialRingColor
                            },
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )

                        // Cardinal letters
                        if (isCardinal) {
                            val label = when (i) {
                                0 -> "N"
                                18 -> "E"
                                36 -> "S"
                                else -> "W"
                            }
                            paint.color = if (i == 0) android.graphics.Color.RED else android.graphics.Color.GRAY
                            val textRadius = outerRadius - 26.dp.toPx()
                            val textX = (center.x + textRadius * sin(angleRad)).toFloat()
                            val textY = (center.y - textRadius * cos(angleRad)).toFloat() + (paint.textSize / 3)
                            drawContext.canvas.nativeCanvas.drawText(label, textX, textY, paint)
                        }
                    }

                    // Golden Qibla Pointer Needle towards Kaaba
                    val qiblaRad = Math.toRadians(state.qiblaBearing.toDouble())
                    val kaabaIndicatorRadius = outerRadius - 38.dp.toPx()
                    val kaabaX = (center.x + kaabaIndicatorRadius * sin(qiblaRad)).toFloat()
                    val kaabaY = (center.y - kaabaIndicatorRadius * cos(qiblaRad)).toFloat()

                    // Needle stem line
                    drawLine(
                        color = if (isFacing) alignedGreen else primaryColor,
                        start = center,
                        end = Offset(kaabaX, kaabaY),
                        strokeWidth = if (isFacing) 4.dp.toPx() else 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Kaaba Target Disc
                    drawCircle(
                        color = if (isFacing) alignedGreen else alignedGold,
                        radius = 13.dp.toPx(),
                        center = Offset(kaabaX, kaabaY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(kaabaX, kaabaY)
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
                    color = if (isFacing) alignedGreen else primaryColor,
                    style = Fill
                )

                // 2D Spirit Bubble Level in center (Accelerometer Sensor)
                val levelRingRadius = 24.dp.toPx()
                drawCircle(
                    color = if (state.isDeviceLevel) alignedGreen.copy(alpha = 0.25f) else outlineVariantColor.copy(alpha = 0.35f),
                    radius = levelRingRadius,
                    center = center
                )
                drawCircle(
                    color = if (state.isDeviceLevel) alignedGreen else outlineVariantColor,
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
                    color = if (state.isDeviceLevel) alignedGreen else alignedGold,
                    radius = 5.5.dp.toPx(),
                    center = bubbleCenter
                )
            }

            // Central Icon (Kaaba or Level indicator)
            if (isFacing) {
                Surface(
                    shape = CircleShape,
                    color = alignedGreen,
                    contentColor = Color.White,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Aligned",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Tilt Alert Reminder if device is not held flat
        if (!state.isDeviceLevel) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = alignedGold,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Hold device flat for optimal compass precision",
                    fontSize = 11.sp,
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
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Distance",
                        fontSize = 11.sp,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,d", state.distanceToKaabaKm)} km",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = "Great-Circle to Mecca",
                        fontSize = 10.sp,
                        color = onSurfaceVariant
                    )
                }
            }

            // North Mode Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleTrueNorth() }
                    .testTag("north_mode_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reference",
                            fontSize = 11.sp,
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = "Tap to switch",
                        fontSize = 10.sp,
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
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verify with Sun",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor
                    )
                }
            }

            // Sensor Calibration Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${state.magneticFieldMagnitude.toInt()} µT · Status",
                        fontSize = 11.sp,
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
    val alignedGreen = Color(0xFF00C853)

    val (bannerColor, contentColor, guidanceText) = when {
        isFacing -> Triple(
            alignedGreen,
            Color.White,
            "✦ ALIGNED WITH THE KAABA 🕋 ✦"
        )
        relativeAngle > 0 -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Turn ${abs(relativeAngle).toInt()}° Right  ➔"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            "⬅  Turn ${abs(relativeAngle).toInt()}° Left"
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.testTag("location_status_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "${state.location.name}, ${state.location.country}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.4f° N, %.4f° E · %s",
                                state.location.latitude,
                                state.location.longitude,
                                if (state.location.isGps) "GPS Cached" else "Selected City"
                            ),
                            fontSize = 10.5.sp,
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
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.testTag("fetch_gps_location_button")
                ) {
                    if (state.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
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
                                contentDescription = "Locate",
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Locate",
                                fontSize = 11.sp,
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
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "Coordinates cached offline. Calculations run 100% on-device.",
                    fontSize = 10.sp,
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
