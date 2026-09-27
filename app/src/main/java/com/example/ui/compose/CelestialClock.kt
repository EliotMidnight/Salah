package com.example.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.prayerName
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import com.example.data.model.Prayer
import com.example.ui.home.ClockGeometry
import com.example.ui.home.ClockMarkerOrder
import com.example.ui.theme.Motion
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The 24-hour clock: a day, read as a circle.
 *
 * Maghrib sits at 12 o'clock and the day runs clockwise through the night, so
 * the arc under the hand is "how far through today we are" and the unmarked
 * stretch is the night. Prayers land at their real times, which is the entire
 * point - and two of them, the first third and the last third of the night, get
 * their own arcs because those are the times a worshipper actually watches for.
 *
 * Colours are passed in rather than read from the theme, so this file has no
 * Material dependency and can be reasoned about as geometry.
 */
@Composable
fun CelestialClock(
    now: LocalTime,
    maghrib: LocalTime,
    prayerTimes: Map<Prayer, LocalTime>,
    currentPrayer: Prayer,
    accent: Color,
    accentBright: Color,
    accentDim: Color,
    marker: Color,
    qiblaBearing: Float,
    compassAzimuth: Float,
    showQibla: Boolean,
    modifier: Modifier = Modifier
) {
    // The hand sweeps up from the top on entry rather than snapping, so opening
    // the clock reads as the day filling in behind it.
    val sweepIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (Motion.allowContinuous) {
            sweepIn.snapTo(0f)
            sweepIn.animateTo(1f, tween(2800, easing = FastOutSlowInEasing))
        } else {
            sweepIn.snapTo(1f)
        }
    }

    val nowHour = ClockGeometry.hoursOf(now)
    val layout = remember(maghrib, prayerTimes) { ClockLayout.build(maghrib, prayerTimes) }
    val targetAngle = ClockGeometry.angleOfNow(now, layout.maghribHour)
    val handAngle = if (sweepIn.value < 1f) targetAngle * sweepIn.value else targetAngle

    Canvas(modifier = modifier) {
        val fit = minOf(size.width, size.height) / 100f
        translate(
            left = (size.width - 100f * fit) / 2f,
            top = (size.height - 100f * fit) / 2f
        ) {
            scale(fit, fit, pivot = Offset.Zero) {
                drawClock(
                    layout = layout.at(nowHour),
                    handAngle = handAngle,
                    accent = accent,
                    accentBright = accentBright,
                    accentDim = accentDim,
                    marker = marker,
                    currentPrayer = currentPrayer,
                    qiblaBearing = qiblaBearing,
                    compassAzimuth = compassAzimuth,
                    showQibla = showQibla
                )
            }
        }
    }
}

/**
 * Every angle the clock draws, resolved once per timetable.
 *
 * Kept separate from rendering so the geometry can be checked against a known
 * day in a test, and so a 60fps repaint is arithmetic-free.
 */
internal data class ClockLayout(
    val maghribHour: Float,
    val prayerAngles: Map<Prayer, Float>,
    val lastThird: ClockGeometry.NightSegment,
    val firstThird: ClockGeometry.NightSegment,
    val duha: ClockGeometry.NightSegment,
    val qaylula: ClockGeometry.NightSegment,
    val fridayDuaStart: Float
) {
    /** The same layout with each special window marked active or not for [nowHour]. */
    fun at(nowHour: Float) = ActiveClockLayout(
        layout = this,
        inLastThird = lastThird.contains(nowHour),
        inFirstThird = firstThird.contains(nowHour),
        inDuha = duha.contains(nowHour),
        inQaylula = qaylula.contains(nowHour)
    )

    companion object {
        fun build(maghrib: LocalTime, prayerTimes: Map<Prayer, LocalTime>): ClockLayout {
            val maghribHour = ClockGeometry.hoursOf(maghrib)
            return ClockLayout(
                maghribHour = maghribHour,
                prayerAngles = ClockMarkerOrder.associateWith {
                    ClockGeometry.angleOf(prayerTimes[it], maghribHour)
                },
                lastThird = ClockGeometry.lastThirdOfNight(
                    prayerTimes[Prayer.MAGHRIB], prayerTimes[Prayer.FAJR]
                ),
                firstThird = ClockGeometry.firstThirdEnd(
                    prayerTimes[Prayer.MAGHRIB], prayerTimes[Prayer.FAJR]
                ),
                duha = ClockGeometry.duha(prayerTimes[Prayer.SUNRISE], prayerTimes[Prayer.DHUHR]),
                qaylula = ClockGeometry.qaylula(prayerTimes[Prayer.DHUHR], prayerTimes[Prayer.ASR]),
                fridayDuaStart = ClockGeometry.angleOf(prayerTimes[Prayer.ASR], maghribHour)
            )
        }
    }
}

/** A [ClockLayout] plus whether each special window contains the current moment. */
internal data class ActiveClockLayout(
    val layout: ClockLayout,
    val inLastThird: Boolean,
    val inFirstThird: Boolean,
    val inDuha: Boolean,
    val inQaylula: Boolean
)

private fun DrawScope.drawClock(
    layout: ActiveClockLayout,
    handAngle: Float,
    accent: Color,
    accentBright: Color,
    accentDim: Color,
    marker: Color,
    currentPrayer: Prayer,
    qiblaBearing: Float,
    compassAzimuth: Float,
    showQibla: Boolean
) {
    val centre = Offset(ClockGeometry.CENTRE, ClockGeometry.CENTRE)

    // 1. Outer frame
    drawCircle(
        color = accent.copy(alpha = 0.15f),
        radius = 44f,
        center = centre,
        style = Stroke(width = 0.3f)
    )

    // 2. Qibla needle. It points at the qibla relative to where the phone is
    //    facing, and flares when the two line up.
    if (showQibla) {
        val delta = qiblaBearing - compassAzimuth
        val aligned = abs(((delta % 360f) + 540f) % 360f - 180f) < 5f
        rotate(degrees = delta, pivot = centre) {
            val colour = if (aligned) accentBright else marker
            drawPath(
                path = Path().apply {
                    // A 5-unit spike at the tip, not a wedge across the dial: at
                    // this size a wide triangle reads as a pie slice rather than
                    // as a direction.
                    moveTo(50f, 0f)
                    lineTo(47.5f, 5f)
                    lineTo(52.5f, 5f)
                    close()
                },
                color = colour.copy(alpha = if (aligned) 1f else 0.7f)
            )
            if (aligned) halo(Offset(50f, 3f), 5f, accentBright, 0.7f)
        }
    }

    // 3. Hour ticks, every sixth one heavier so the quarters read at a glance
    for (hour in 0 until 24) {
        val angle = hour / 24f * 360f
        val major = hour % 6 == 0
        drawLine(
            color = if (major) accent.copy(alpha = 0.4f) else marker.copy(alpha = 0.15f),
            start = ClockGeometry.position(angle, if (major) 40f else 41.5f),
            end = ClockGeometry.position(angle, 43f),
            strokeWidth = if (major) 0.6f else 0.3f
        )
    }

    // 4. The track the hand sweeps
    drawCircle(
        color = marker.copy(alpha = 0.10f),
        radius = 38f,
        center = centre,
        style = Stroke(width = 2f)
    )

    // 5. Special-time windows, all on the inner radius so the prayer markers on
    //    the track stay legible. Each brightens while it is the current window.
    drawWindow(layout.layout.lastThird, accent.copy(alpha = if (layout.inLastThird) 0.6f else 0.4f))
    drawWindow(layout.layout.duha, accent.copy(alpha = if (layout.inDuha) 0.6f else 0.3f))
    drawWindow(layout.layout.qaylula, accent.copy(alpha = if (layout.inQaylula) 0.6f else 0.3f))
    // Friday dua spans Asr to Maghrib. Dashed, because it is a weekly window
    // rather than a daily one, and that difference should be visible.
    drawWindow(
        segment = ClockGeometry.NightSegment(
            layout.layout.fridayDuaStart, 0f, 0f, 0f
        ),
        color = accentBright.copy(alpha = 0.32f),
        dashed = true
    )

    // 6. The day's progress
    if (handAngle > 1f) {
        drawWindow(
            segment = ClockGeometry.NightSegment(0f, handAngle, 0f, 0f),
            color = accentBright,
            radius = 38f,
            strokeWidth = 2f
        )
    }

    // 7. Prayer markers. Sunrise is a diamond: it ends a period rather than
    //    beginning a prayer, and a filled dot would claim it is one of the five.
    ClockMarkerOrder.forEach { prayer ->
        val angle = layout.layout.prayerAngles[prayer] ?: return@forEach
        val active = prayer == currentPrayer
        val point = ClockGeometry.position(angle, 38f)
        if (prayer == Prayer.SUNRISE) {
            drawDiamond(point, 2.5f, marker.copy(alpha = if (active) 1f else 0.8f), angle)
        } else {
            if (active) halo(point, 7f, accentBright, 0.6f)
            drawCircle(
                color = if (active) accentBright else accentDim,
                radius = if (active) 3.5f else 2f,
                center = point
            )
        }
    }

    // 8. The two night thirds, as diamonds on the track
    drawDiamond(
        ClockGeometry.position(layout.layout.firstThird.endAngle, 38f), 2f,
        marker.copy(alpha = if (layout.inFirstThird) 1f else 0.8f),
        layout.layout.firstThird.endAngle
    )
    drawDiamond(
        ClockGeometry.position(layout.layout.lastThird.startAngle, 38f), 2f,
        marker.copy(alpha = if (layout.inLastThird) 1f else 0.8f),
        layout.layout.lastThird.startAngle
    )

    // 9. The hand
    rotate(degrees = handAngle, pivot = centre) {
        val point = ClockGeometry.position(0f, 38f)
        halo(point, 5f, accentBright, 0.35f)
        drawCircle(color = Color.White, radius = 2f, center = point)
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 0.8f,
            center = Offset(point.x, point.y - 0.5f)
        )
    }
}

/** A window on the inner ring, drawn clockwise from its start to its end. */
private fun DrawScope.drawWindow(
    segment: ClockGeometry.NightSegment,
    color: Color,
    radius: Float = 33f,
    strokeWidth: Float = 1f,
    dashed: Boolean = false
) {
    val sweep = ClockGeometry.sweepFromTo(segment.startAngle, segment.endAngle)
    if (sweep <= 0.1f) return
    val topLeft = Offset(ClockGeometry.CENTRE - radius, ClockGeometry.CENTRE - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val start = ClockGeometry.arcDegrees(segment.startAngle)

    if (!dashed) {
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        return
    }
    // Dashed: short round-capped segments stepped around the arc.
    var drawn = 0f
    while (drawn < sweep) {
        drawArc(
            color = color,
            startAngle = start + drawn,
            sweepAngle = minOf(2f, sweep - drawn),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawn += 4f
    }
}

private fun DrawScope.halo(centre: Offset, radius: Float, color: Color, strength: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = strength), Color.Transparent),
            center = centre,
            radius = radius
        ),
        radius = radius,
        center = centre
    )
}

/** A diamond marker, rotated so its points face along the ring. */
private fun DrawScope.drawDiamond(centre: Offset, size: Float, color: Color, angle: Float) {
    rotate(degrees = angle, pivot = Offset(ClockGeometry.CENTRE, ClockGeometry.CENTRE)) {
        drawPath(
            path = Path().apply {
                moveTo(centre.x, centre.y - size)
                lineTo(centre.x + size * 0.8f, centre.y)
                lineTo(centre.x, centre.y + size)
                lineTo(centre.x - size * 0.8f, centre.y)
                close()
            },
            color = color
        )
    }
}

/**
 * The names and times printed around the clock's outside.
 *
 * Positioned children rather than canvas strokes, because they are text and
 * text belongs to the layout system - font scaling, right-to-left mirroring and
 * the accessibility tree all come free, and none of them survive being drawn as
 * a path.
 */
@Composable
internal fun ClockFaceLabels(
    layout: ClockLayout,
    prayerTimes: Map<Prayer, LocalTime>,
    currentPrayer: Prayer,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val strong = MaterialTheme.colorScheme.onSurface
    val weak = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = modifier) {
        // The same 0..100 space the canvas draws in, so one geometry serves both.
        val fit = minOf(maxWidth.value, maxHeight.value) / 100f
        val originX = maxWidth.value / 2f
        val originY = maxHeight.value / 2f

        @Composable
        fun label(angle: Float, name: String, time: String?, active: Boolean) {
            // Placed on an ellipse, not a circle. On a circle the Dhuhr and Asr
            // anchors land 8 units outside a square box, so their labels get
            // clipped by the screen edge; pulling the horizontal radius in keeps
            // every label whole while leaving the top and bottom ones outside
            // the ring, where they read best.
            val point = ClockGeometry.labelPosition(angle, rx = 44f, ry = 57f)
            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((originX - 8f + (point.x - ClockGeometry.CENTRE) * fit) * density).roundToInt(),
                            y = ((originY - 6f + (point.y - ClockGeometry.CENTRE) * fit) * density).roundToInt()
                        )
                    }
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) strong else weak,
                    maxLines = 1
                )
                if (time != null) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = weak.copy(alpha = if (active) 0.9f else 0.6f),
                        maxLines = 1
                    )
                }
            }
        }

        ClockMarkerOrder.forEach { prayer ->
            val angle = layout.prayerAngles[prayer]
            val time = prayerTimes[prayer]?.format(timeFormatter)
            if (angle != null) label(angle, strings.prayerName(prayer), time, prayer == currentPrayer)
        }
    }
}
