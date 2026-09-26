package com.example.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerTime
import com.example.engine.HijriCalendarEngine
import com.example.engine.PrayerCalculationEngine
import com.example.ui.SalahUiState
import com.example.ui.components.ActionRow
import com.example.ui.components.DetailRow
import com.example.ui.components.OptionSheet
import com.example.ui.components.RowDivider
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SectionGroup
import com.example.ui.components.SectionHeader
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.prayerName
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Prayer times for a chosen day, the night periods around them, and the month at
 * a glance.
 *
 * Order is deliberate: pick a day, read today's times, read the night periods,
 * then scan the month. The calculation method and madhhab are reachable but not
 * hoisted to the top - they are consulted occasionally, not on every visit, and
 * the previous layout led with a full-width "transparent calculation source"
 * banner that pushed the actual times below the fold.
 */
@Composable
fun PrayerScreen(
    state: SalahUiState,
    onMethodChange: (CalculationMethod) -> Unit,
    onMadhhabChange: (Madhhab) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = remember(selectedDateText) { LocalDate.parse(selectedDateText) }
    var showMethodSheet by rememberSaveable { mutableStateOf(false) }
    var showTrustSheet by rememberSaveable { mutableStateOf(false) }

    val isToday = selectedDate == LocalDate.now()
    val timePattern = if (state.timeFormat24h) "HH:mm" else "h:mm a"
    val timeFormatter = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern) }

    val dayTimes = remember(selectedDate, state.location, state.method, state.madhhab, state.adjustments) {
        if (isToday && state.todayPrayerTimes != null) {
            state.todayPrayerTimes
        } else {
            PrayerCalculationEngine.calculatePrayerTimes(
                date = selectedDate,
                location = state.location,
                method = state.method,
                madhhab = state.madhhab,
                adjustments = state.adjustments
            )
        }
    }
    val prayers = dayTimes?.prayers ?: emptyList()

    val hijri = remember(selectedDate) { HijriCalendarEngine.getHijriDate(selectedDate) }

    ScreenScaffold(
        title = strings.more.prayerTimesTitle,
        subtitle = "${strings.more.methodology}: ${state.method.title}",
        onBack = null,
        modifier = modifier
    ) { _ ->
        Column {
            DaySelector(
                selectedDate = selectedDate,
                isToday = isToday,
                hijriLabel = "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH",
                gregorianLabel = selectedDate.format(
                    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
                ),
                todayLabel = strings.todayBtn,
                onPrevious = { selectedDateText = selectedDate.minusDays(1).toString() },
                onNext = { selectedDateText = selectedDate.plusDays(1).toString() },
                onToday = { selectedDateText = LocalDate.now().toString() }
            )

            SectionHeader(if (isToday) strings.todaysTimes else strings.prayerTimesHeader)

            SectionGroup {
                prayers.forEachIndexed { index, prayerTime ->
                    if (index > 0) RowDivider()
                    PrayerTimeRow(
                        prayerTime = prayerTime,
                        isNext = isToday && prayerTime.isNext,
                        time = prayerTime.time.format(timeFormatter)
                    )
                }
            }

            SectionHeader(strings.vigilsAndNightPeriods)

            SectionGroup {
                listOf(
                    strings.imsakTitle to "الإمساك",
                    strings.midnightTitle to "منتصف الليل",
                    strings.lastThirdTitle to "الثلث الأخير"
                ).forEachIndexed { index, (label, arabic) ->
                    if (index > 0) RowDivider()
                    val time = when (index) {
                        0 -> dayTimes?.imsak
                        1 -> dayTimes?.midnight
                        else -> dayTimes?.lastThirdOfNight
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = space.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = arabic,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = ArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = time?.format(timeFormatter) ?: "--:--",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            SectionHeader(strings.sectionPrayerCalc)

            SectionGroup {
                ActionRow(
                    title = strings.methodLabel,
                    subtitle = state.method.description,
                    value = state.method.title,
                    onClick = { showMethodSheet = true },
                    testTag = "setting_method"
                )
                RowDivider()
                ActionRow(
                    title = strings.madhhabLabel,
                    value = state.madhhab.title,
                    onClick = { onMadhhabChange(if (state.madhhab == Madhhab.STANDARD) Madhhab.HANAFI else Madhhab.STANDARD) },
                    testTag = "setting_madhhab"
                )
            }

            SectionHeader(strings.sectionSystemDiagnostics)

            SectionGroup {
                ActionRow(
                    title = strings.transparentCalculationSource,
                    subtitle = "${state.location.name}, ${state.location.country}",
                    onClick = { showTrustSheet = true },
                    testTag = "trust_details"
                )
            }

            Spacer(Modifier.height(space.lg))
            MonthTable(
                selectedDate = selectedDate,
                location = state.location,
                method = state.method,
                madhhab = state.madhhab,
                adjustments = state.adjustments,
                onSelectDate = { selectedDateText = it.toString() }
            )
        }
    }

    if (showMethodSheet) {
        OptionSheet(
            title = strings.more.chooseMethod,
            onDismiss = { showMethodSheet = false }
        ) {
            CalculationMethod.entries.forEach { method ->
                androidx.compose.material3.HorizontalDivider()
                ActionRow(
                    title = method.title,
                    subtitle = method.description,
                    showChevron = false,
                    isSelected = method == state.method,
                    onClick = {
                        onMethodChange(method)
                        showMethodSheet = false
                    }
                )
            }
        }
    }

    if (showTrustSheet) {
        OptionSheet(
            title = strings.transparentCalculationSource,
            subtitle = "${state.method.title} · ${state.location.name}",
            onDismiss = { showTrustSheet = false }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(space.lg)) {
                    DetailRow(
                        strings.more.methodology,
                        if (state.method.isMoroccoNationalTable) {
                            "Kingdom of Morocco National Habous table"
                        } else {
                            "Fajr ${state.method.fajrAngle}° · Isha ${
                                if (state.method.ishaAngle > 0) "${state.method.ishaAngle}°" else "90 min"
                            }"
                        }
                    )
                    RowDivider()
                    DetailRow(
                        strings.more.madhhabLabelShort,
                        "${state.madhhab.title} (${state.madhhab.shadowFactor}x)"
                    )
                    RowDivider()
                    DetailRow(
                        strings.more.appliedAdjustments,
                        listOf(
                            Prayer.FAJR to state.adjustments.fajr,
                            Prayer.DHUHR to state.adjustments.dhuhr,
                            Prayer.ASR to state.adjustments.asr,
                            Prayer.MAGHRIB to state.adjustments.maghrib,
                            Prayer.ISHA to state.adjustments.isha
                        ).joinToString(", ") { (prayer, minutes) ->
                            "${prayer.englishName} ${signed(minutes)}"
                        }
                    )
                    RowDivider()
                    DetailRow(strings.offlineStatus, strings.more.computedOnDevice)
                    RowDivider()
                    DetailRow(strings.more.lastVerified, state.lastChecked)
                }
            }
        }
    }
}

private fun signed(minutes: Int): String = if (minutes >= 0) "+$minutes" else "$minutes"

/** Previous / next day, with the current day always one tap away. */
@Composable
private fun DaySelector(
    selectedDate: LocalDate,
    isToday: Boolean,
    hijriLabel: String,
    gregorianLabel: String,
    todayLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("day_prev")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = !isToday, onClick = onToday)
                .padding(vertical = space.xs)
                .semantics { contentDescription = "$gregorianLabel, $hijriLabel" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = gregorianLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isToday) "$hijriLabel · ${todayLabel.lowercase()}" else hijriLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onNext, modifier = Modifier.testTag("day_next")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One prayer time. The next prayer is marked by weight and a dot, not a badge. */
@Composable
private fun PrayerTimeRow(
    prayerTime: PrayerTime,
    isNext: Boolean,
    time: String,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .padding(vertical = space.md)
            .testTag("prayer_time_${prayerTime.prayer.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(prayerTime.prayer.iconRes),
            contentDescription = null,
            tint = if (isNext) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .padding(end = space.md)
                .size(26.dp)
                .clearAndSetSemantics { }
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.prayerName(prayerTime.prayer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isNext) {
                    Spacer(Modifier.width(space.sm))
                    com.example.ui.components.StatusDot(color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = prayerTime.prayer.arabicName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = ArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The month as a table.
 *
 * Uses the wider reading measure because a five-column time grid genuinely needs
 * the room, and drops the per-day card for a plain row: thirty bordered surfaces
 * with a coloured fill on the selected day was the loudest thing on a screen whose
 * job is to be scannable.
 */
@Composable
private fun MonthTable(
    selectedDate: LocalDate,
    location: com.example.data.model.UserLocation,
    method: CalculationMethod,
    madhhab: Madhhab,
    adjustments: com.example.data.model.PrayerAdjustments,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val month = selectedDate.month
    val daysInMonth = month.length(selectedDate.isLeapYear)
    val today = LocalDate.now()

    val fard = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(space.sm))

        // Column headings, so the five numbers per row are not unlabelled.
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(40.dp))
            fard.forEach { prayer ->
                Text(
                    text = prayer.englishName.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { }
                )
            }
        }
        Spacer(Modifier.height(space.xs))

        (1..daysInMonth).forEach { day ->
            val date = LocalDate.of(selectedDate.year, month, day)
            val isSelected = day == selectedDate.dayOfMonth
            val isToday = date == today

            val times = remember(date, location, method, madhhab, adjustments) {
                PrayerCalculationEngine.calculatePrayerTimes(
                    date = date,
                    location = location,
                    method = method,
                    madhhab = madhhab,
                    adjustments = adjustments
                )
            }
            val byPrayer = times.prayers.associateBy { it.prayer }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { onSelectDate(date) }
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .padding(vertical = space.xs, horizontal = space.xs)
                    .semantics { selected = isSelected },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected || isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.width(40.dp)
                )
                fard.forEach { prayer ->
                    Text(
                        text = byPrayer[prayer]?.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (day < daysInMonth) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        Spacer(Modifier.height(space.md))
        Text(
            text = strings.madhhabLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
