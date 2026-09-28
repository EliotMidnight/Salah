package com.example.ui.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MagneticFieldStatus
import com.example.ui.SalahUiState
import com.example.ui.components.ActionRow
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionGroup
import com.example.ui.components.SegmentedOptions
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.Space
import com.example.ui.theme.Tonal
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    val space = Space.current
    val successColors = Tonal.colors
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val tertiaryColor = colorScheme.tertiary
    val errorColor = colorScheme.error
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant
    val outlineVariantColor = colorScheme.outlineVariant

    var unwrappedHeading by remember { mutableFloatStateOf(state.compassAzimuth) }
    LaunchedEffect(state.compassAzimuth) {
        val step = ((state.compassAzimuth - unwrappedHeading + 540f) % 360f) - 180f
        unwrappedHeading += step
    }
    val animatedHeading by animateFloatAsState(
        targetValue = unwrappedHeading,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatedHeading"
    )

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
        QiblaGuidanceBanner(
            state = state,
            onShowCalibrationTip = onShowCalibrationTip,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(space.md))

        val headingText = "${state.compassAzimuth.toInt()}° " +
            getCardinalDirection(state.compassAzimuth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .aspectRatio(1f)
                .testTag("qibla_sensor_compass_dial")
                .semantics(mergeDescendants = true) {
                    contentDescription = strings.more.dialDescription.format(
                        headingText,
                        "${state.qiblaBearing.toInt()}°"
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.width / 2f) - 20f

                if (isFacing) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                alignedSuccess.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = outerRadius + 18.dp.toPx()
                        ),
                        radius = outerRadius + 18.dp.toPx(),
                        center = center
                    )
                }

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
                    style = Stroke(
                        width = if (isFacing) 4.dp.toPx() else 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                rotate(degrees = -animatedHeading, pivot = center) {
                    val cardinalPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 14.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W").forEach { (deg, label) ->
                        val rad = Math.toRadians(deg.toDouble())
                        val isNorth = deg == 0
                        val tickOuter = outerRadius - 14.dp.toPx()
                        val tickLen = if (isNorth) 14.dp.toPx() else 9.dp.toPx()
                        val startX = center.x + (tickOuter - tickLen) * sin(rad).toFloat()
                        val startY = center.y - (tickOuter - tickLen) * cos(rad).toFloat()
                        val endX = center.x + tickOuter * sin(rad).toFloat()
                        val endY = center.y - tickOuter * cos(rad).toFloat()
                        drawLine(
                            color = if (isNorth) errorColor else onSurfaceVariant,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isNorth) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        cardinalPaint.color =
                            if (isNorth) errorColor.toArgb() else onSurfaceColor.toArgb()
                        val r = outerRadius - 36.dp.toPx()
                        val x = center.x + r * sin(rad).toFloat()
                        val y = center.y - r * cos(rad).toFloat() + (cardinalPaint.textSize / 3f)
                        drawContext.canvas.nativeCanvas.drawText(label, x, y, cardinalPaint)
                    }

                    val qiblaRad = Math.toRadians(state.qiblaBearing.toDouble())
                    val dirX = sin(qiblaRad).toFloat()
                    val dirY = (-cos(qiblaRad)).toFloat()
                    val perpX = -dirY
                    val perpY = dirX

                    val kaabaDist = outerRadius - 16.dp.toPx()
                    val kaabaCenter = Offset(center.x + dirX * kaabaDist, center.y + dirY * kaabaDist)
                    val kaabaHalf = 7.dp.toPx()
                    drawRect(
                        color = if (isFacing) alignedSuccess else alignedGold,
                        topLeft = Offset(kaabaCenter.x - kaabaHalf, kaabaCenter.y - kaabaHalf),
                        size = Size(kaabaHalf * 2, kaabaHalf * 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val tipDist = outerRadius - 30.dp.toPx()
                    val halfWidth = 3.dp.toPx()
                    val tip = Offset(center.x + dirX * tipDist, center.y + dirY * tipDist)
                    val needle = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(center.x + perpX * halfWidth, center.y + perpY * halfWidth)
                        lineTo(center.x - perpX * halfWidth, center.y - perpY * halfWidth)
                        close()
                    }
                    drawPath(
                        path = needle,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isFacing) alignedSuccess else primaryColor,
                                if (isFacing) alignedSuccess else alignedGold
                            ),
                            start = center,
                            end = tip
                        ),
                        style = Fill
                    )
                }

                val arrowTipY = center.y - outerRadius - 10.dp.toPx()
                val arrow = Path().apply {
                    moveTo(center.x, arrowTipY)
                    lineTo(center.x - 6.dp.toPx(), arrowTipY + 10.dp.toPx())
                    lineTo(center.x + 6.dp.toPx(), arrowTipY + 10.dp.toPx())
                    close()
                }
                drawPath(
                    path = arrow,
                    color = if (isFacing) alignedSuccess else primaryColor,
                    style = Fill
                )

                if (isFacing) {
                    drawCircle(color = alignedSuccess, radius = 15.dp.toPx(), center = center)
                    val check = Path().apply {
                        moveTo(center.x - 6.dp.toPx(), center.y)
                        lineTo(center.x - 1.5.dp.toPx(), center.y + 4.5.dp.toPx())
                        lineTo(center.x + 6.5.dp.toPx(), center.y - 4.5.dp.toPx())
                    }
                    drawPath(
                        path = check,
                        color = successColors.onSuccess,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                } else {
                    drawCircle(color = onSurfaceVariant, radius = 2.5.dp.toPx(), center = center)
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isDeviceLevel,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.xs),
                modifier = Modifier.padding(horizontal = space.md, vertical = space.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = alignedGold,
                    modifier = Modifier
                        .size(14.dp)
                        .clearAndSetSemantics { }
                )
                Text(
                    text = strings.more.holdFlatHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(space.md))

        SectionGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.kaabaDistance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${String.format(Locale.US, "%,d", state.distanceToKaabaKm)} km",
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurfaceColor
                )
            }

            RowDivider()

            Column(modifier = Modifier.padding(vertical = space.md)) {
                Text(
                    text = strings.more.northReferenceLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = onSurfaceColor
                )
                Spacer(modifier = Modifier.height(space.sm))
                SegmentedOptions(
                    options = listOf(strings.trueNorth, strings.magneticNorth),
                    selectedIndex = if (state.useTrueNorth) 0 else 1,
                    onSelect = { index ->
                        if ((index == 0) != state.useTrueNorth) onToggleTrueNorth()
                    },
                    modifier = Modifier.testTag("north_mode_card")
                )
            }

            RowDivider()

            ActionRow(
                title = strings.more.locateMe,
                subtitle = "${state.location.name}, ${state.location.country} · " +
                    String.format(
                        Locale.US,
                        "%.4f°, %.4f° · %s",
                        state.location.latitude,
                        state.location.longitude,
                        if (state.location.isGps) strings.more.gpsCached else strings.more.selectedCity
                    ),
                icon = Icons.Default.MyLocation,
                value = if (state.isLocating) strings.more.loading else null,
                enabled = !state.isLocating,
                showChevron = false,
                onClick = onFetchLocation,
                testTag = "fetch_gps_location_button"
            )

            RowDivider()

            ActionRow(
                title = strings.more.solarReferenceTitle,
                icon = Icons.Default.WbSunny,
                showChevron = false,
                onClick = onShowSunVerification,
                testTag = "verify_with_sun_button"
            )

            RowDivider()

            ActionRow(
                title = strings.more.calibrationTitle,
                icon = Icons.Default.CompassCalibration,
                showChevron = false,
                onClick = onShowCalibrationTip,
                testTag = "sensor_calibration_button"
            )
        }
    }
}

@Composable
private fun QiblaGuidanceBanner(
    state: SalahUiState,
    onShowCalibrationTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeAngle = state.relativeQiblaAngle
    val isFacing = state.isFacingQibla
    val space = Space.current
    val successColors = Tonal.colors
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current

    val (bannerColor, contentColor, guidanceText) = when {
        isFacing -> Triple(
            successColors.success,
            successColors.onSuccess,
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

    val actionable = !isFacing && state.magneticStatus == MagneticFieldStatus.INTERFERENCE

    Surface(
        shape = MaterialTheme.shapes.small,
        color = bannerColor,
        contentColor = contentColor,
        modifier = modifier
            .testTag("qibla_guidance_banner")
            .then(
                if (actionable) {
                    Modifier.clickable(onClick = onShowCalibrationTip)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = space.md, vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isFacing) Icons.Default.CheckCircle else Icons.Default.NearMe,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .clearAndSetSemantics { }
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
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
