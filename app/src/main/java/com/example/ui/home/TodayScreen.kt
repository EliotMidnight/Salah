package com.example.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.SafeArea
import com.example.ui.localization.LocalLanguage
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.appLocale
import com.example.ui.localization.prayerName
import com.example.ui.localization.isArabicInterface
import com.example.ui.localization.DateNavStrings
import com.example.ui.theme.Motion
import com.example.ui.theme.HandwritingFamily
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Today: the prayer, its time, and how long until the next one.
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
    val timeFormatter = remember(state.timeFormat24h) {
        DateTimeFormatter.ofPattern(if (state.timeFormat24h) "HH:mm" else "h:mm a")
    }

    var showMonth by rememberSaveable { mutableStateOf(false) }
    // How far the reader has walked from the shared date with this page's own
    // arrows. Kept as an offset rather than a second selected date, so browsing a
    // day here cannot move the app's date: the Prayer screen owns that switch, and
    // the notifications and the "next prayer" countdown are about *today*, which
    // this page would otherwise be redefining.
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

    // For today, the live figures come from the state, which the ViewModel's
    // one-second ticker already recomputes. They used to be computed here instead,
    // inside `remember(day, isToday)` - and those keys do not change between
    // seconds, so the page froze: the countdown stopped, and the headline prayer
    // and the "next" row stayed on the answer that was true when the page was
    // first composed. The three values the ticker was maintaining for exactly this
    // purpose had no reader.
    //
    // A scrubbed date is the other case, and the state cannot help: there is no
    // tomorrow or yesterday for the ViewModel to hold. Those days are anchored on
    // Maghrib, which is deterministic and is what makes walking the arrows stable.
    val currentPrayer: Prayer
    val nextPrayer: Prayer?
    val nextTime: LocalTime?
    val countdown: String
    if (isToday) {
        val next = state.nextPrayer
        val previous = state.previousPrayer
        // Before the first tick lands there is nothing in the state yet. Computing
        // from the same day keeps the page renderable in that window rather than
        // blank, and the ticker replaces it a second later. Both fields are asked
        // for together because the ticker writes them together: a next prayer with
        // no previous prayer would mean a half-written state, not a real reading.
        val live = next != null && previous != null
        currentPrayer = if (live) previous!!.prayer
        else currentPrayerOf(day, LocalTime.now())
        nextPrayer = if (live) next.prayer else nextPrayerOf(day, LocalTime.now())
        nextTime = next?.time
        countdown = if (live) state.countdownString else ""
    } else {
        currentPrayer = currentPrayerOf(day, maghrib)
        nextPrayer = nextPrayerOf(day, maghrib)
        nextTime = nextPrayer?.let { times[it] }
        countdown = ""
    }

    // The reader's Hijri adjustment is applied here, and this is the only place it is.
    //
    // The ViewModel also computes a Hijri date - the only one that honours the
    // adjustment - and puts it in `state.hijriDate`. Nothing read it. So the control
    // in Settings ("Hijri adjustment ±2") round-tripped all the way through
    // preferences, the repository flow, a full prayer-time recalculation and the sun
    // position, changed the number in the settings row, and changed nothing on any
    // date the reader could see. A setting that provably does nothing is worse than
    // no setting, because the row reads as "changed".
    //
    // So the adjustment is applied where the date is rendered, and the state field is
    // gone rather than left as a second answer. `hijriAdjustment` is in the state, so
    // the adjustment cannot go stale the way the field did.
    val hijri = remember(date, state.hijriAdjustment) {
        HijriCalendarEngine.getHijriDate(date.plusDays(state.hijriAdjustment.toLong()))
    }

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
                    top = SafeArea.top() + space.sm,
                    start = SafeArea.sides(),
                    end = SafeArea.sides(),
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
                    // A 48dp tap target around a 24dp line of text: the location is how
                    // the reader changes where the times come from, and the label alone
                    // was a 32dp-tall target - the smallest control on the page in the
                    // one place that is a primary action. The text stays where it was;
                    // the target and the ripple grow around it.
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onLocationClick)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(vertical = space.xs, horizontal = space.xs)
                )
            }

            Spacer(Modifier.height(space.xl))

            // 2. The page itself. No tap target: the 24-hour clock is gone, and
            //    with it the gesture that was the only reason to make the whole
            //    page tappable.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.lg),
                contentAlignment = Alignment.Center
            ) {
                SimpleView(
                    currentPrayer = currentPrayer,
                    isArabicUi = isArabicUi,
                    nextPrayer = nextPrayer,
                    nextTime = nextTime,
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

    if (showMonth) {
        HijriMonthSheet(
            date = date,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments,
            timeFormatter = timeFormatter,
            hijriAdjustment = state.hijriAdjustment,
            onSelectDate = { dayOffset = (it.toEpochDay() - base.toEpochDay()).toInt() },
            onDismiss = { showMonth = false }
        )
    }
}

/**
 * The prayers in day order, which is the order the list below uses.
 */
private val PrayerListOrder = listOf(
    Prayer.FAJR,
    Prayer.SUNRISE,
    Prayer.DHUHR,
    Prayer.ASR,
    Prayer.MAGHRIB,
    Prayer.ISHA
)

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
        //
        // Measured, not fixed: see [HeadlineFit]. A fixed 58sp cut "Lever du
        // soleil" and "Sonnenaufgang" off mid-word on one line, with no ellipsis,
        // on the one line the page is built around.
        val localisedName = strings.prayerName(currentPrayer)
        val arabicLeads = isArabicUi
        val headlineText = if (arabicLeads) currentPrayer.arabicName else localisedName
        // Handwriting, not the display serif. This is the one line on the
        // page that should read as written by a person rather than set by a
        // typesetter, and at 58sp the difference is the whole character of
        // the page. The Arabic subtitle below keeps the serif, because
        // [HandwritingFamily] has no Arabic glyphs to fall back from within.
        val headlineStyle = TextStyle(
            fontFamily = if (arabicLeads) FontFamily.Serif else HandwritingFamily,
            fontSize = HeadlineFit.CEILING_SP.sp,
            lineHeight = HeadlineFit.LINE_HEIGHT_SP.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center
        )
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        // The box is here to read the row's width; centring is asked for
        // explicitly, because a box lays its child out from the top-left where
        // the column used to centre the name itself.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val rowWidthPx = with(density) { maxWidth.roundToPx() }
            val fontScale = density.fontScale
            val sizeSp = remember(headlineText, headlineStyle, rowWidthPx, fontScale) {
                HeadlineFit.size(
                    measuredWidthPx = measurer
                        .measure(AnnotatedString(headlineText), headlineStyle)
                        .size
                        .width,
                    availableWidthPx = rowWidthPx
                )
            }
            Text(
                text = headlineText,
                style = headlineStyle.copy(fontSize = sizeSp.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("today_headline")
            )
        }
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
                    text = strings.dateNav.nextPrayerPrefix.uppercase(),
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

            // The date row's own arrows, the same control the Prayer screen has. They
            // step this page's date and write nothing back: the app's date is
            // switched on the Prayer screen, and a second writer would be a second
            // answer to "which day is it".
            Spacer(Modifier.height(space.sm))

            // The day's own name comes from the platform, in the *app's* language and
            // not the device's - the two are separate settings, and an Arabic interface
            // on a German phone was reading a German weekday under an Arabic date.
            // Hoisted out of the body as well: this row redraws with the countdown, and
            // the pattern was rebuilt with it.
            val dateLocale = appLocale(LocalLanguage.current)
            val gregorianFormatter = remember(dateLocale) {
                DateTimeFormatter.ofPattern("EEEE d MMMM", dateLocale)
            }
            DaySelector(
                selectedDate = date,
                isToday = isToday,
                hijriLabel = "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH",
                gregorianLabel = date.format(gregorianFormatter),
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
                    state = prayerRowState(
                        isToday = isToday,
                        prayer = prayer,
                        currentPrayer = currentPrayer,
                        nextPrayer = nextPrayer
                    )
                )
            }
        }

        Spacer(Modifier.height(space.lg))

        // Two rows, in the same hand as the times above them: a label, a dotted
        // leader, a value. Not cards - a card around each would give them more
        // weight than the prayer times, and the whole point is that they are the
        // quiet way out of this page.
        LinkRow(
            label = LocalStrings.current.dateNav.fullHijriMonth,
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
 */
@Composable
private fun CountdownRule(accent: Color, countdown: String, modifier: Modifier = Modifier) {
    val space = Space.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.lg)
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

/** Which of the three marks a prayer's row carries. */
internal enum class PrayerTimeRowState { Past, Current, Future }

/**
 * The mark a prayer's row carries on the day being shown.
 *
 * ### Why a day that is not today gets no marks
 *
 * A mark is a claim about now, and now is only on today. A scrubbed day cannot ask
 * the state - it holds today - so its hero and its next line come from a fixed
 * anchor, the day's own Maghrib, picked for being deterministic and for keeping the
 * arrows stable to walk (see the derivation above). The marks were fed that same
 * anchored pair, so a page of tomorrow's times dimmed the prayer *after* the anchor:
 * Isha, the last of the day, drawn as already gone on a page whose own "now" was
 * Maghrib - the prayer before it. The anchor is how the hero is drawn; it is not
 * turned into marks.
 *
 * ### On today, the walk marks the wrong row
 *
 * The current prayer's row is accent. The rows marked [PrayerTimeRowState.Past] come
 * from [isAfter], whose forward walk steps before it compares and therefore includes
 * the `next` end of its range: `current` and `next` always bracket now, so the only
 * row it can reach is `next` - the row the NEXT line names - while the prayer whose
 * window has ended keeps full-strength ink. That disagrees with the only written
 * account of the state ("a past prayer is one that has already come round") and with
 * the times printed on the same rows. Fixing it needs a clock, and the clock has one
 * owner: the ticker that writes the state's previous and next prayers can name the
 * elapsed rows in the same pass, and [isAfter] can then go. This function takes no
 * clock of its own on purpose - a second reading of now on the page is exactly the
 * kind of duplicate decision this list cannot afford.
 */
internal fun prayerRowState(
    isToday: Boolean,
    prayer: Prayer,
    currentPrayer: Prayer,
    nextPrayer: Prayer?
): PrayerTimeRowState = when {
    !isToday -> PrayerTimeRowState.Future
    prayer == currentPrayer -> PrayerTimeRowState.Current
    nextPrayer != null && isAfter(prayer, currentPrayer, nextPrayer) -> PrayerTimeRowState.Past
    else -> PrayerTimeRowState.Future
}

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
