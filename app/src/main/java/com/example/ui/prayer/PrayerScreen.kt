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
import com.example.ui.home.DaySelector
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
    onSelectDate: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    // The selected day lives in the ViewModel, not in this screen. This is the
    // app's only date switcher, and the Today page follows whatever it picks -
    // two independent switches were two answers to "which day is this".
    //
    // Null means "today", so the app keeps following the current day instead of
    // freezing on whatever date it was built.
    val today = state.todayPrayerTimes?.date ?: LocalDate.now()
    val selectedDate = state.selectedDate ?: today
    var showMethodSheet by rememberSaveable { mutableStateOf(false) }
    var showTrustSheet by rememberSaveable { mutableStateOf(false) }

    val isToday = selectedDate == today
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

    // No header. The bottom navigation already says "Prayer", and the method it
    // used to carry in the subtitle is shown as a labelled row further down this
    // same screen, so nothing is lost.
    ScreenScaffold(
        title = null,
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
                onPrevious = { onSelectDate(selectedDate.minusDays(1)) },
                onNext = { onSelectDate(selectedDate.plusDays(1)) },
                onToday = { onSelectDate(null) }
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
                            // Once the UI language is Arabic, `label` is already the
                            // Arabic term, so this second line rendered the same
                            // word twice in the same row. Skip it when they agree.
                            if (arabic != label) {
                                Text(
                                    text = arabic,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = ArabicFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                today = today,
                location = state.location,
                method = state.method,
                madhhab = state.madhhab,
                adjustments = state.adjustments,
                onSelectDate = onSelectDate
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
                            // Localized name, not prayer.englishName. This is the
                            // "source / method" summary, so an Arabic or Urdu user
                            // was reading English prayer names inside an otherwise
                            // localized screen.
                            "${strings.prayerName(prayer)} ${signed(minutes)}"
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
 *
 * The whole month is computed in one `remember` rather than one per row inside the
 * loop. Thirty separate computations meant thirty separate cache entries, each
 * re-evaluated whenever any input changed, which is what made switching months and
 * changing the calculation method feel heavy on a slow phone.
 */
@Composable
private fun MonthTable(
    selectedDate: LocalDate,
    today: LocalDate,
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

    val fard = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    val timePattern = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // day of month -> prayer -> time.
    val monthTimes: Map<Int, Map<Prayer, PrayerTime>> = remember(
        selectedDate.year,
        month,
        location,
        method,
        madhhab,
        adjustments
    ) {
        (1..daysInMonth).associateWith { day ->
            PrayerCalculationEngine.calculatePrayerTimes(
                date = LocalDate.of(selectedDate.year, month, day),
                location = location,
                method = method,
                madhhab = madhhab,
                adjustments = adjustments
            ).prayers.associateBy { it.prayer }
        }
    }

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
                    // Deliberately the Latin abbreviation rather than
                    // strings.prayerName(prayer).take(3): truncating a
                    // non-Latin script to three characters produces an
                    // unreadable fragment, which is worse than a stable
                    // abbreviation in a five-column numeric table. These
                    // headings are cleared from the accessibility tree, and the
                    // rows carry the full name. Fixing this properly means
                    // adding a short-form to the prayer-name dictionary.
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
            val byPrayer = monthTimes[day].orEmpty()

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
                        text = byPrayer[prayer]?.time?.format(timePattern) ?: "--:--",
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
