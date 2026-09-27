package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.data.model.HijriDate
import com.example.engine.HijriCalendarEngine
import com.example.engine.PrayerCalculationEngine
import com.example.engine.QiblaEngine
import com.example.ui.SalahUiState
import com.example.ui.compose.CelestialClock
import com.example.ui.compose.ClockFaceLabels
import com.example.ui.compose.ClockLayout
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.prayerName
import com.example.ui.localization.isArabicInterface
import com.example.ui.localization.ClockStrings
import com.example.ui.theme.Motion
import com.example.ui.theme.layoutMetrics
import com.example.ui.theme.HandwritingFamily
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Today: the prayer, its time, and how long until the next one.
 *
 * Ported from the athan-pwa home page. The layout is the webapp's - one
 * dominant prayer name, the countdown on a hairline rule, the day's times as a
 * dotted-leader list, and a tap anywhere that opens the whole day as a
 * 24-hour clock. What is *not* ported is the palette: this page reads from the
 * app's Material theme so it sits with every other screen, and the webapp's
 * nine accent themes stay where they are.
 *
 * The date is not switched here. It is switched on the Prayer tab and read from
 * [SalahUiState.selectedDate], so there is one answer to "which day am I
 * looking at" rather than one per screen.
 */
@Composable
fun TodayScreen(
    state: SalahUiState,
    onLocationClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val space = Space.current
    val accent = MaterialTheme.colorScheme.primary
    val timeFormatter = remember(state.timeFormat24h) {
        DateTimeFormatter.ofPattern(if (state.timeFormat24h) "HH:mm" else "h:mm a")
    }

    var clockOpen by rememberSaveable { mutableStateOf(false) }
    var showMonth by rememberSaveable { mutableStateOf(false) }
    // How far the reader has walked from the shared date with this page's own
    // arrows. Kept as an offset rather than a second selected date, so the two
    // can never disagree: the arrows move the shared date, they do not shadow it.
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }

    // The Prayer tab owns the date. There is no switcher here: a second one
    // meant a second answer to "which day am I looking at", and the two drifted
    // apart the moment you used one and not the other.
    val today = state.todayPrayerTimes?.date ?: LocalDate.now()
    val base = state.selectedDate ?: today
    val date = base.plusDays(dayOffset.toLong())
    val isToday = date == today
    val isArabicUi = isArabicInterface(state.language)
    val continueReadingRef = "${state.continueReading.surahName} " +
        "${state.continueReading.surahNumber}:${state.continueReading.ayahNumber}"
    val day: PrayerTimesDay = remember(date, state.location, state.method, state.madhhab, state.adjustments) {
        state.todayPrayerTimes
            ?.takeIf { it.date == date }
            ?: PrayerCalculationEngine.calculatePrayerTimes(
                date = date,
                location = state.location,
                method = state.method,
                madhhab = state.madhhab,
                adjustments = state.adjustments
            )
    }
    val times = remember(day) { day.prayers.associate { it.prayer to it.time } }

    // Maghrib anchors the clock, so a day without one has no anchor. Falling
    // back to noon keeps the page renderable rather than blank on partial data.
    val maghrib = times[Prayer.MAGHRIB] ?: LocalTime.NOON
    val anchorTime = if (isToday) LocalTime.now() else maghrib
    val now = anchorTime
    val currentPrayer = remember(day, isToday) { currentPrayerOf(day, anchorTime) }
    val nextPrayer = remember(day, isToday) { nextPrayerOf(day, anchorTime) }
    val nextTime = nextPrayer?.let { times[it] }
    val countdown = remember(day, isToday) {
        if (isToday) countdownText(nextTime, LocalTime.now()) else ""
    }

    val hijri = remember(date) { HijriCalendarEngine.getHijriDate(date) }

    // Following the Prayer tab means dropping our own walk, or the page would
    // quietly stay three days ahead of the date the switcher says.
    LaunchedEffect(base) { dayOffset = 0 }

    // The body's identity and colour both come from the sun's real position for
    // the day being shown, so a scrubbed date shows that day's sky and not
    // today's.
    val sunPosition = remember(date, isToday, state.location, state.sunPosition) {
        if (!isToday) {
            QiblaEngine.calculateSunPosition(
                state.location,
                LocalDateTime.of(date, if (isToday) LocalTime.now() else LocalTime.NOON)
            )
        } else {
            state.sunPosition
                ?: QiblaEngine.calculateSunPosition(state.location, LocalDateTime.now())
        }
    }
    val sunAltitude = sunPosition?.altitude ?: 0f
    val sunAzimuth = sunPosition?.azimuth ?: 180f
    val isSetting = sunAzimuth > 180f

    Box(
        modifier = modifier
            .fillMaxSize()
            // The page paints its own background rather than inheriting one. It
            // is the only screen that is a whole composition rather than content
            // inside a Scaffold, and a screen that goes blank when it is composed
            // without one is not a screen.
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = statusBarTop() + space.sm,
                    bottom = space.xxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header: where you are.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.location.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onLocationClick)
                        .padding(vertical = space.xs, horizontal = space.xs)
                )
            }

            Spacer(Modifier.height(space.xl))

            // 2. The page itself. One tap target wraps both views so "tap for the
            //    full clock" is literally true anywhere on the screen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = strings.clock.tapForFullClock,
                        onClick = { clockOpen = !clockOpen }
                    )
                    .padding(horizontal = space.lg),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = clockOpen,
                    transitionSpec = {
                        (fadeIn(tween(Motion.duration(300))) + scaleIn(initialScale = 0.94f))
                            .togetherWith(
                                fadeOut(tween(Motion.duration(150))) + scaleOut(targetScale = 0.98f)
                            )
                    },
                    label = "todayView"
                ) { open ->
                    if (open) {
                        ClockView(
                            times = times,
                            now = now,
                            currentPrayer = currentPrayer,
                            isToday = isToday,
                            timeFormatter = timeFormatter,
                            state = state
                        )
                    } else {
                        SimpleView(
                            currentPrayer = currentPrayer,
                            isArabicUi = isArabicUi,
                            nextPrayer = nextPrayer,
                            nextTime = nextPrayer?.let { times[it] },
                            countdown = countdown,
                            isToday = isToday,
                            times = times,
                            date = date,
                            hijri = hijri,
                            dayOffset = dayOffset,
                            onShiftDate = { dayOffset = it },
                            onShowMonth = { showMonth = true },
                            onContinueReadingClick = onContinueReadingClick,
                            continueReadingRef = continueReadingRef,
                            sunAltitude = sunAltitude,
                            sunAzimuth = sunAzimuth,
                            isSetting = isSetting,
                            hijriDay = hijri.day,
                            timeFormatter = timeFormatter
                        )
                    }
                }
            }
        }
    }

    if (showMonth) {
        HijriMonthSheet(
            date = date,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments,
            timeFormatter = timeFormatter,
            onSelectDate = { dayOffset = (it.toEpochDay() - base.toEpochDay()).toInt() },
            onDismiss = { showMonth = false }
        )
    }
}

/**
 * The default view: which prayer it is, when it was, what is next, and the
 * whole day as a list.
 */
@Composable
private fun SimpleView(
    currentPrayer: Prayer,
    isArabicUi: Boolean,
    nextPrayer: Prayer?,
    nextTime: LocalTime?,
    countdown: String,
    isToday: Boolean,
    times: Map<Prayer, LocalTime>,
    date: LocalDate,
    hijri: HijriDate,
    dayOffset: Int,
    onShiftDate: (Int) -> Unit,
    onShowMonth: () -> Unit,
    onContinueReadingClick: () -> Unit,
    continueReadingRef: String,
    sunAltitude: Float,
    sunAzimuth: Float,
    isSetting: Boolean,
    hijriDay: Int,
    timeFormatter: DateTimeFormatter
) {
    val strings = LocalStrings.current
    val space = Space.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Whichever body is actually up, glowing in its own colour. Not a
        // per-prayer emblem: the sky above you at 3am is the moon whatever
        // prayer you are between.
        CelestialBody(
            sunAltitude = sunAltitude,
            sunAzimuth = sunAzimuth,
            isSetting = isSetting,
            hijriDay = hijriDay,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        )

        Spacer(Modifier.height(space.sm))

        // The prayer's name is the loudest thing on the page, in whatever
        // language the app is actually in. Arabic leads only when the reader
        // chose Arabic - a headline in a language you did not pick is decoration,
        // and the Arabic belongs underneath it as the subtitle, not above it as
        // the title.
        val localisedName = strings.prayerName(currentPrayer)
        val arabicLeads = isArabicUi
        Text(
            text = if (arabicLeads) currentPrayer.arabicName else localisedName,
            // Handwriting, not the display serif. This is the one line on the
            // page that should read as written by a person rather than set by a
            // typesetter, and at 58sp the difference is the whole character of
            // the page. The Arabic subtitle below keeps the serif, because
            // [HandwritingFamily] has no Arabic glyphs to fall back from within.
            fontFamily = if (arabicLeads) FontFamily.Serif else HandwritingFamily,
            fontSize = 58.sp,
            lineHeight = 66.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (!arabicLeads) {
            Text(
                text = currentPrayer.arabicName,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                lineHeight = 34.sp,
                color = muted,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(space.sm))

        // An affordance rather than an instruction. The words "tap for the full
        // clock" were doing two jobs badly: they sat between the prayer and its
        // time looking like a third fact, and they spelled out a gesture on a
        // whole-page target. A rule reads as "there is more under here", and the
        // container still carries the instruction for anyone who cannot see it.
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 20.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = muted.copy(alpha = 0.55f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Spacer(Modifier.height(space.lg))

        if (nextPrayer != null) {
            // One line, so it has to *be* one line - the same size throughout.
            // Matching only the line height was not enough: a 22sp name and an
            // 11sp label share a baseline but not a weight, and the label still
            // read as smaller type rather than as a quiet part of the same
            // thought. One size, differentiated by weight and colour instead.
            val size = 20.sp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.sm)
            ) {
                Text(
                    text = strings.clock.nextPrayerPrefix.uppercase(),
                    fontSize = size,
                    lineHeight = size * 1.2f,
                    fontWeight = FontWeight.Bold,
                    color = muted.copy(alpha = 0.7f)
                )
                Text(
                    text = strings.prayerName(nextPrayer),
                    fontSize = size,
                    lineHeight = size * 1.2f,
                    fontFamily = FontFamily.Serif,
                    color = onSurface
                )
                Text(
                    text = nextTime?.format(timeFormatter) ?: "--:--",
                    fontSize = size,
                    lineHeight = size * 1.2f,
                    color = muted
                )
            }

            Spacer(Modifier.height(space.md))

            if (isToday && countdown.isNotEmpty()) {
                CountdownRule(accent = accent, countdown = countdown)
            }

            // The app's one date switcher. It lives on the Prayer tab too, and
            // both write the same selected date - two switches would be two
            // answers to "which day am I looking at".
            Spacer(Modifier.height(space.sm))
            DaySelector(
                selectedDate = date,
                isToday = isToday,
                hijriLabel = "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH",
                gregorianLabel = date.format(
                    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
                ),
                todayLabel = LocalStrings.current.todayBtn,
                onPrevious = { onShiftDate(dayOffset - 1) },
                onNext = { onShiftDate(dayOffset + 1) },
                onToday = { onShiftDate(0) }
            )
        }

        Spacer(Modifier.height(space.lg))

        // The day as a list. A dotted leader is doing real work here: it is
        // what lets a column of names and a column of times be read as pairs
        // without a rule between every row.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space.sm)
        ) {
            PrayerListOrder.forEach { prayer ->
                PrayerTimeRow(
                    prayer = prayer,
                    time = times[prayer]?.format(timeFormatter) ?: "--:--",
                    state = when {
                        prayer == currentPrayer -> PrayerTimeRowState.Current
                        nextPrayer != null && isAfter(prayer, currentPrayer, nextPrayer) ->
                            PrayerTimeRowState.Past
                        else -> PrayerTimeRowState.Future
                    }
                )
            }
        }

        Spacer(Modifier.height(space.lg))

        // Two rows, in the same hand as the times above them: a label, a dotted
        // leader, a value. Not cards - a card around each would give them more
        // weight than the prayer times, and the whole point is that they are the
        // quiet way out of this page.
        LinkRow(
            label = LocalStrings.current.clock.fullHijriMonth,
            onClick = onShowMonth
        )
        LinkRow(
            label = LocalStrings.current.continueReading,
            value = continueReadingRef,
            onClick = onContinueReadingClick
        )
    }
}

/**
 * A label, a dotted leader and an optional value, tappable.
 *
 * Deliberately not a card. These are the page's exits - the month calendar and
 * where you left off reading - and a bordered surface around each would make
 * them louder than the prayer times they sit under.
 */
@Composable
private fun LinkRow(
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(vertical = space.sm, horizontal = space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LeaderDots(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.xs),
            colour = muted.copy(alpha = 0.35f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = muted.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = space.xxs)
        )
    }
}

/**
 * A hairline rule that fades out at both ends, with the countdown in the gap.
 *
 * The countdown is the one number that matters and the rule is the one thing
 * that separates it from the list below, so they are one component: drawn as
 * three siblings they only make sense together, and anyone who later inserts a
 * row between them breaks the composition silently.
 */
@Composable
private fun CountdownRule(accent: Color, countdown: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RuleSegment(accent, Modifier.weight(1f))
        Text(
            text = countdown,
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
        RuleSegment(accent, Modifier.weight(1f))
    }
}

@Composable
private fun RuleSegment(accent: Color, modifier: Modifier) {
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = 0.3f), Color.Transparent)
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}

private enum class PrayerTimeRowState { Past, Current, Future }

@Composable
private fun PrayerTimeRow(
    prayer: Prayer,
    time: String,
    state: PrayerTimeRowState
) {
    val space = Space.current
    val strings = LocalStrings.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    val nameColour = when (state) {
        PrayerTimeRowState.Current -> accent
        PrayerTimeRowState.Past -> muted
        PrayerTimeRowState.Future -> onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.prayerName(prayer),
            style = MaterialTheme.typography.bodyMedium,
            color = nameColour,
            fontWeight = if (state == PrayerTimeRowState.Current) FontWeight.SemiBold else FontWeight.Normal
        )
        LeaderDots(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.xs),
            colour = muted.copy(alpha = 0.35f)
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = nameColour
        )
    }
}

/**
 * A row of dots between a name and its time.
 *
 * Drawn rather than typed because a run of periods is a different length at
 * every text size and every language - and a leader that changes width shifts
 * the two columns it is supposed to be holding still.
 */
@Composable
private fun LeaderDots(colour: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(12.dp)) {
        val step = 6.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawCircle(color = colour, radius = 1.dp.toPx(), center = Offset(x, size.height / 2f))
            x += step
        }
    }
}

/**
 * The 24-hour clock, with the same centre the simple view shows.
 *
 * The window readout below the dial is the reason the special arcs exist at
 * all: an arc you are currently inside brightens, and this line says which one
 * in words, so the highlight is never the only signal.
 */
@Composable
private fun ClockView(
    times: Map<Prayer, LocalTime>,
    now: LocalTime,
    currentPrayer: Prayer,
    isToday: Boolean,
    timeFormatter: DateTimeFormatter,
    state: SalahUiState
) {
    val strings = LocalStrings.current
    val space = Space.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val maghrib = times[Prayer.MAGHRIB] ?: return

    val layout = remember(maghrib, times) { ClockLayout.build(maghrib, times) }
    val nowHour = ClockGeometry.hoursOf(now)
    val active = layout.at(nowHour)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // The dial is a square, but its labels sit on a 57-unit radius around a
        // 100-unit box, so they hang off the top, bottom and sides. The labels
        // are allowed to overflow the box - that is what puts them outside the
        // ring - so everything *after* the dial has to clear that overhang
        // itself, or it lands on the Sunrise and Fajr labels.
        val labelOverhang = maxWidth * 0.10f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CelestialClock(
                    now = now,
                    maghrib = maghrib,
                    prayerTimes = times,
                    currentPrayer = currentPrayer,
                    accent = accent,
                    accentBright = accent,
                    accentDim = accent.copy(alpha = 0.55f),
                    marker = muted,
                    qiblaBearing = state.qiblaBearing,
                    compassAzimuth = state.compassAzimuth,
                    showQibla = true,
                    modifier = Modifier.fillMaxSize()
                )
                ClockFaceLabels(
                    layout = layout,
                    prayerTimes = times,
                    currentPrayer = currentPrayer,
                    timeFormatter = timeFormatter,
                    modifier = Modifier.fillMaxSize()
                )

                // The dial's middle. The webapp puts the countdown here too; we
                // keep it to the prayer, because the countdown is already on the
                // simple view and repeating it invites two answers to "how long".
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentPrayer.arabicName,
                        fontFamily = FontFamily.Serif,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.prayerName(currentPrayer),
                        style = MaterialTheme.typography.titleSmall,
                        color = onSurface
                    )
                    if (isToday) {
                        Spacer(Modifier.height(space.xs))
                        Text(
                            text = strings.clock.tapToCloseClock,
                            style = MaterialTheme.typography.labelSmall,
                            color = muted.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(labelOverhang))

            // Which window, if any, we are inside right now.
            val readout = activeWindowReadout(layout, active, timeFormatter, isFriday(), strings.clock)
            if (readout != null) {
                Spacer(Modifier.height(space.sm))
                Text(
                    text = readout,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * The one line under the dial naming whichever window is current.
 *
 * This is the only place the highlighted arcs are explained in words. An arc
 * that brightens while you are inside it is a pleasant detail; a bright arc
 * with no label is a puzzle. The windows cannot overlap in practice, but the
 * order here is the order they are drawn in, so the text and the highlight can
 * never disagree about which is which.
 */
private fun activeWindowReadout(
    layout: ClockLayout,
    active: com.example.ui.compose.ActiveClockLayout,
    timeFormatter: DateTimeFormatter,
    isFriday: Boolean,
    strings: ClockStrings
): String? {
    return when {
        active.inDuha -> strings.duhaUntil.format(layout.duha.endHour.asLocalTime().format(timeFormatter))
        active.inQaylula -> strings.qaylulaUntil.format(layout.qaylula.endHour.asLocalTime().format(timeFormatter))
        active.inLastThird -> strings.lastThirdUntilFajr
        active.inFirstThird -> strings.firstThirdUntil.format(layout.firstThird.endHour.asLocalTime().format(timeFormatter))
        isFriday -> strings.jumuahDuaUntil
        else -> null
    }
}

/** An hour-of-day as a [LocalTime], for formatting a segment's closing bound. */
private fun Float.asLocalTime(): LocalTime {
    val totalMinutes = (this * 60f).toInt().coerceIn(0, 24 * 60 - 1)
    return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
}

private fun isFriday(): Boolean = LocalDate.now().dayOfWeek.value == 5

// ---------------------------------------------------------------------------
// Facts about a day, derived from the timetable the engine already produced.
//
// These are the webapp's store functions reduced to what Compose needs: the
// engine is the single source of truth for "which prayer is it", so nothing
// here re-derives a time that [PrayerCalculationEngine] has already decided.
// ---------------------------------------------------------------------------

/** The prayer whose window contains [at]: the last one to have entered. */
private fun currentPrayerOf(day: PrayerTimesDay, at: LocalTime): Prayer =
    PrayerCalculationEngine.getPreviousPrayer(day, at).prayer

private fun nextPrayerOf(day: PrayerTimesDay, at: LocalTime): Prayer =
    PrayerCalculationEngine.getNextPrayer(day, at).prayer

/**
 * The countdown, in the webapp's two-tier form: hours and minutes while there
 * are hours left, minutes and seconds once there are not.
 *
 * The switch is the point. An hour is too coarse to be worth watching when the
 * next prayer is four minutes out, and seconds are noise when it is six hours
 * away - so the last hour gets the seconds and the rest does not.
 */
private fun countdownText(target: LocalTime?, at: LocalTime): String {
    if (target == null) return ""
    val seconds = ChronoUnit.SECONDS.between(at, target).let {
        // A next-prayer time on the far side of midnight counts forwards, not
        // backwards, so the countdown never reads as a large negative.
        if (it < 0) it + 24 * 3600 else it
    }
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02dh %02dm", hours, minutes)
    } else {
        String.format(Locale.US, "%02dm %02ds", minutes, secs)
    }
}

/** A past prayer is one that has already come round since [current]. */
private fun isAfter(prayer: Prayer, current: Prayer, next: Prayer): Boolean {
    val order = PrayerListOrder
    val from = order.indexOf(current)
    val to = order.indexOf(next)
    val at = order.indexOf(prayer)
    if (from < 0 || to < 0 || at < 0) return false
    // Across midnight the list wraps, so "between current and next" is a
    // forward walk rather than a numeric range.
    var index = from
    while (index != to) {
        index = (index + 1) % order.size
        if (index == at) return true
    }
    return false
}

/** Status bar height plus any display cutout, as a Dp. */
@Composable
internal fun statusBarTop(): Dp {
    val density = LocalDensity.current
    val px = maxOf(
        WindowInsets.statusBars.getTop(density),
        WindowInsets.displayCutout.getTop(density)
    )
    return with(density) { px.toDp() }
}
