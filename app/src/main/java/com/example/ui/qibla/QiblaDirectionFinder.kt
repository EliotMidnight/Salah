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
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.MagneticFieldStatus
import com.example.engine.QiblaGuidance
import com.example.engine.formatBearing
import com.example.ui.SalahUiState
import com.example.ui.formatCoordinates
import com.example.ui.isolateLtr
import com.example.ui.components.ActionRow
import com.example.ui.components.DetailList
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionGroup
import com.example.ui.components.SegmentedOptions
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.IconSize
import com.example.ui.theme.Space
import java.util.Locale

import kotlin.math.cos
import kotlin.math.sin

/** The compass dial is the largest single instrument, so it gets its own measure. */
private val CompassDialMaxWidth = 420.dp

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
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
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

    // Monochrome: the dial ring is accent when facing, neutral otherwise.
    val dialRingColor by animateColorAsState(
        targetValue = if (isFacing) primaryColor else outlineVariantColor,
        label = "dialRingColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QiblaGuidanceBanner(
            state = state,
            onShowCalibrationTip = onShowCalibrationTip,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(space.md))

        // Heading and Qibla bearing, side by side.
        //
        // Two numbers is the whole instrument, and putting them in one row lets
        // the eye compare them without the dial in between. They are labelled
        // because "95°" above a compass answers "which way am I facing" and the
        // same digits below it answer "which way is the Kaaba" - identical
        // numerals, opposite questions.
        DialReadout(
            heading = state.compassAzimuth,
            bearing = state.qiblaBearing,
            isFacing = isFacing,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CompassDialMaxWidth)
        )

        Spacer(modifier = Modifier.height(space.md))

        val headingText = "${state.compassAzimuth.toInt()}° " +
            strings.more.reader.cardinal(state.compassAzimuth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CompassDialMaxWidth)
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
                        color = primaryColor.copy(alpha = 0.15f),
                        center = center,
                        radius = outerRadius + 18.dp.toPx()
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
                    // The dial is a fixed-size instrument: its etched labels stop
                    // growing at 135% so the cardinal letters cannot collide with
                    // the degree numerals. Every value they show is set at full
                    // Dynamic Type in the readouts above.
                    val dialFontScale = fontScale.coerceAtMost(1.35f)
                    val cardinalPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 14.dp.toPx() * dialFontScale
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    // The graduated scale.
                    //
                    // A tick every 2 degrees, so the dial reads as an instrument
                    // rather than four cardinal marks with a marker between
                    // them; 30 and 90 carry the weight so the eye still finds
                    // the quarters without reading anything.
                    //
                    // Every endpoint uses the same `sin / -cos` parameterisation
                    // as the cardinals and the needle below. Mixing `+cos` here
                    // with `-cos` there mirrors the whole scale through the
                    // centre while leaving its 90-degree symmetry perfectly
                    // intact - so the ticks look correct and the needle ends up
                    // pointing directly *away* from the Kaaba at the exact
                    // moment of alignment.
                    val tickOuter = outerRadius - 4.dp.toPx()
                    for (deg in 0 until 360 step 2) {
                        val rad = Math.toRadians(deg.toDouble())
                        val is30 = deg % 30 == 0
                        val is90 = deg % 90 == 0
                        val length = when {
                            is90 -> 16.dp.toPx()
                            is30 -> 10.dp.toPx()
                            else -> 4.dp.toPx()
                        }
                        val colour = when {
                            is90 -> primaryColor
                            is30 -> primaryColor.copy(alpha = 0.75f)
                            else -> onSurfaceVariant.copy(alpha = 0.45f)
                        }
                        drawLine(
                            color = colour,
                            start = Offset(
                                center.x + (tickOuter - length) * sin(rad).toFloat(),
                                center.y - (tickOuter - length) * cos(rad).toFloat()
                            ),
                            end = Offset(
                                center.x + tickOuter * sin(rad).toFloat(),
                                center.y - tickOuter * cos(rad).toFloat()
                            ),
                            strokeWidth = when {
                                is90 -> 2.dp.toPx()
                                is30 -> 1.5.dp.toPx()
                                else -> 1.dp.toPx()
                            },
                            cap = StrokeCap.Round
                        )
                    }

                    // The degree numerals, every 30 degrees except the four
                    // cardinal points: there the letter is the label, and
                    // printing "270" under "W" only made both harder to read.
                    // They rotate with the card rather than staying upright.
                    val degreePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val labelRadius = outerRadius - 27.dp.toPx()
                    for (deg in 0 until 360 step 30) {
                        val rad = Math.toRadians(deg.toDouble())
                        val is90 = deg % 90 == 0
                        degreePaint.textSize =
                            (if (is90) 11.dp else 9.dp).toPx() * dialFontScale
                        degreePaint.color = if (is90) {
                            onSurfaceColor.copy(alpha = 0.5f).toArgb()
                        } else {
                            onSurfaceVariant.copy(alpha = 0.4f).toArgb()
                        }
                        degreePaint.typeface = if (is90) {
                            android.graphics.Typeface.DEFAULT_BOLD
                        } else {
                            android.graphics.Typeface.DEFAULT
                        }
                        if (deg % 90 != 0) {
                            val x = center.x + labelRadius * sin(rad).toFloat()
                            val y = center.y - labelRadius * cos(rad).toFloat() +
                                (degreePaint.textSize / 3f)
                            drawContext.canvas.nativeCanvas.drawText("$deg", x, y, degreePaint)
                        }
                    }

                    // The dial is a physical instrument and must not mirror in
                    // RTL, but its labels are still words: use the localized
                    // abbreviations rather than hard-coded English letters.
                    listOf(
                        0f to strings.more.reader.cardinalNorth,
                        90f to strings.more.reader.cardinalEast,
                        180f to strings.more.reader.cardinalSouth,
                        270f to strings.more.reader.cardinalWest
                    ).forEach { (deg, label) ->
                        val rad = Math.toRadians(deg.toDouble())
                        val isNorth = deg == 0f
                        cardinalPaint.color =
                            if (isNorth) primaryColor.toArgb() else onSurfaceColor.toArgb()
                        val r = outerRadius - 44.dp.toPx()
                        val x = center.x + r * sin(rad).toFloat()
                        val y = center.y - r * cos(rad).toFloat() + (cardinalPaint.textSize / 3f)
                        drawContext.canvas.nativeCanvas.drawText(label, x, y, cardinalPaint)
                    }

                    val qiblaRad = Math.toRadians(state.qiblaBearing.toDouble())
                    val dirX = sin(qiblaRad).toFloat()
                    val dirY = (-cos(qiblaRad)).toFloat()
                    val perpX = -dirY
                    val perpY = dirX

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
                        color = if (isFacing) primaryColor else primaryColor,
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
                    color = primaryColor,
                    style = Fill
                )

                if (isFacing) {
                    drawCircle(color = primaryColor, radius = 15.dp.toPx(), center = center)
                    val check = Path().apply {
                        moveTo(center.x - 6.dp.toPx(), center.y)
                        lineTo(center.x - 1.5.dp.toPx(), center.y + 4.5.dp.toPx())
                        lineTo(center.x + 6.5.dp.toPx(), center.y - 4.5.dp.toPx())
                    }
                    drawPath(
                        path = check,
                        color = Color.White,
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
                    tint = onSurfaceVariant,
                    modifier = Modifier
                        .size(IconSize.xs)
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
                    text = isolateLtr(
                        "${String.format(Locale.US, "%,d", state.distanceToKaabaKm)} km"
                    ),
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
                    "${formatCoordinates(state.location.latitude, state.location.longitude, 4)} · " +
                    if (state.location.isGps) strings.more.gpsCached else strings.more.selectedCity,
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

/**
 * Heading and Qibla bearing, as two labelled figures in one row.
 */
@Composable
private fun DialReadout(
    heading: Float,
    bearing: Float,
    isFacing: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val space = Space.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReadoutFigure(
                label = strings.more.reader.headingLabel,
                value = "${heading.toInt()}°",
                caption = strings.more.reader.cardinal(heading),
                captionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            ReadoutFigure(
                label = strings.more.qiblaBearing,
                value = formatBearing(bearing),
                caption = strings.more.reader.cardinal(bearing),
                captionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** One labelled figure: a quiet label, the number, and a caption under it. */
@Composable
private fun ReadoutFigure(
    label: String,
    value: String,
    caption: String,
    valueColor: Color,
    captionColor: Color,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(space.xxs))
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = valueColor,
            maxLines = 1
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = captionColor,
            maxLines = 1
        )
    }
}

@Composable
private fun QiblaGuidanceBanner(
    state: SalahUiState,
    onShowCalibrationTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One guidance value, asked once. The banner's words, its colour and the
    // dial's ring and check mark are then the same answer, which is the whole
    // point: a banner that says "turn 5°" beside a dial that has already turned
    // green is worse than either one alone.
    val guidance = state.qiblaGuidance
    val isFacing = guidance.isAligned
    val space = Space.current
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current

    val (bannerColor, contentColor, guidanceText) = when (guidance.direction) {
        QiblaGuidance.Direction.ON_TARGET -> Triple(
            colorScheme.primary,
            Color.White,
            strings.more.alignedWithQibla
        )
        QiblaGuidance.Direction.RIGHT -> Triple(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
            strings.more.turnBy.format(
                "${guidance.degrees}°",
                strings.more.rightOfQibla
            )
        )
        QiblaGuidance.Direction.LEFT -> Triple(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
            strings.more.turnBy.format(
                "${guidance.degrees}°",
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
                    Modifier.clickable(role = Role.Button, onClick = onShowCalibrationTip)
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
                    .size(IconSize.sm)
                    .clearAndSetSemantics { }
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The old English `getCardinalDirection`, gone.
 */
