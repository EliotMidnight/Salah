package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationMethod
import com.example.data.model.HijriDate
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.engine.HijriCalendarEngine
import com.example.engine.PrayerCalculationEngine
import com.example.engine.PrayerTimeZone
import com.example.ui.localization.LocalLanguage
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.appLocale
import com.example.ui.localization.prayerName
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The whole Hijri month, as a grid of its days.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HijriMonthSheet(
    date: LocalDate,
    location: UserLocation,
    method: CalculationMethod,
    madhhab: Madhhab,
    adjustments: PrayerAdjustments,
    timeFormatter: DateTimeFormatter,
    /**
     * The reader's Hijri adjustment, in days.
     */
    hijriAdjustment: Int = 0,
    onSelectDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current
    // The platform's month and weekday names are asked for in the *app's* language,
    // not the device's: a reader can choose Arabic on a German phone, and the grid
    // was then ruled in German beside Arabic headings.
    val locale = appLocale(LocalLanguage.current)

    // The month on screen, which is not necessarily the month the selected day
    // falls in once the reader starts paging. Null until the sheet opens, then
    // seeded from the selected day.
    // Not saved: a month anchor that survives process death but not rotation is
    // not worth a custom Saver for something the reader re-seeds on open anyway.
    var monthAnchor by remember { mutableStateOf<LocalDate?>(null) }
    val anchor = monthAnchor ?: date
    LaunchedEffect(date) { monthAnchor = date }

    val selected = remember(anchor, hijriAdjustment) { hijriOf(anchor, hijriAdjustment) }
    // Walk the Gregorian days that make up that Hijri month: from the first day
    // to the last, found by asking the engine where each day lands - with the same
    // adjustment, or the grid belongs to a different month than the heading.
    val days = remember(anchor, hijriAdjustment) { hijriMonthDays(anchor, hijriAdjustment) }

    val times = remember(anchor, location, method, madhhab, adjustments) {
        PrayerCalculationEngine.calculatePrayerTimes(
            date = anchor, location = location, method = method,
            madhhab = madhhab, adjustments = adjustments
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg)
                .padding(bottom = space.xxl)
        ) {
            // The month switcher. Arrows step by a whole Hijri month, which is
            // not 29 or 30 Gregorian days - stepping by a fixed day count drifts
            // by a day every few months and lands in the wrong month by the end
            // of a year. Stepping to the first day of the neighbouring month does
            // not.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { monthAnchor = shiftHijriMonth(anchor, -1, hijriAdjustment) },
                    modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.dateNav.previousMonth,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selected.monthNameEn} ${selected.year} AH",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { monthAnchor = shiftHijriMonth(anchor, 1, hijriAdjustment) },
                    modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.dateNav.nextMonth,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(space.md))

            // Weekday initials, so the seven columns are not unlabelled numbers.
            //
            // Labels, in the reader's own language, from [weekdayLabels] - the platform's
            // names rather than a literal, and Sunday first so they line up with the
            // grid's leading blanks ([weekdayColumn]).
            Row(modifier = Modifier.fillMaxWidth()) {
                val labels = remember(locale) { weekdayLabels(locale) }
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(space.xs))

            // One reading for the whole grid: 30 calls would be 30 chances to
            // straddle midnight between cells.
            val today = PrayerTimeZone.localNow().toLocalDate()
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth()
            ) {
                // A blank cell per leading weekday, so the 1st lands under its
                // own column rather than being pulled to the left.
                val leadingBlanks = weekdayColumn(days.first().dayOfWeek)
                items(leadingBlanks) { Box(Modifier.aspectRatio(1f)) }
                items(days) { day ->
                    HijriDayCell(
                        day = day,
                        isSelected = day == date,
                        isToday = day == today,
                        hijriAdjustment = hijriAdjustment,
                        onClick = { onSelectDate(day) }
                    )
                }
            }

            Spacer(Modifier.height(space.lg))

            Text(
                text = strings.todaysTimes,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(space.xs))
            PrayerTimesCompact(times = times, timeFormatter = timeFormatter)
        }
    }
}

/**
 * One day of the Hijri month.
 */
@Composable
private fun HijriDayCell(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hijriAdjustment: Int,
    onClick: () -> Unit
) {
    val space = Space.current
    val locale = appLocale(LocalLanguage.current)
    val gregorianDay = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val hijri = remember(day, hijriAdjustment) { hijriOf(day, hijriAdjustment) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    // **There is no dot on a day, and there should not be one.**
    //
    // There used to be: a 3dp marker drawn when four or more of the five fard prayers
    // "fell in the night", tested as `s % 86400 < 12 * 3600 || s > 11 * 3600`.
    // `toSecondOfDay()` is always in 0..86399, so `s % 86400` is `s`, and the condition
    // reduces to `s < 43200 || s > 39600` - true for **every second of every day**. The
    // count was therefore always 5, the threshold always met, and the dot drew on all
    // 29 or 30 days of the month. A marker that is on every cell marks nothing, and it
    // was drawing a claim about a religious observance the code never computed.
    //
    // What would be worth marking is a fact that *varies*: Jumu'ah, a public holiday, a
    // night-prayer threshold. Each is a product decision with a religious judgement in
    // it, and none of them is this condition - so the cell is left plain rather than
    // given an invented rule. A reader can see the times for any day by tapping it,
    // which is the real task and already works.

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .semantics {
                // "14 Muharram, 18 September". The Gregorian half was
                // `day.month.name.lowercase()`, which is the *enum constant* - so an
                // Arabic reader heard the English word "september" under a Hijri
                // date, in every language, on every cell of the month.
                contentDescription = "${hijri.day} ${hijri.monthNameEn}, " +
                    day.format(gregorianDay)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${hijri.day}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) onSurface else if (isToday) accent else onSurface,
                maxLines = 1
            )
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) muted else muted.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

/** The selected day's times, in two columns so the sheet is not a tall list. */
@Composable
private fun PrayerTimesCompact(times: PrayerTimesDay, timeFormatter: DateTimeFormatter) {
    val space = Space.current
    val strings = LocalStrings.current
    val byPrayer = times.prayers.associateBy { it.prayer }
    val rows = listOf(
        Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR,
        Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA
    )

    Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        rows.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { prayer ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.prayerName(prayer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.size(space.xs))
                        Text(
                            text = byPrayer[prayer]?.time?.format(timeFormatter) ?: "--:--",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Every Gregorian day belonging to the same Hijri month as [date].
 */
private fun hijriMonthDays(date: LocalDate, adjustment: Int): List<LocalDate> {
    val target = hijriOf(date, adjustment)
    val days = mutableListOf<LocalDate>()
    // Backwards from [date] inclusive. Seeding the list with [date] and then
    // looping from it as well would put the selected day in the grid twice.
    var cursor = date
    while (sameHijriMonth(cursor, target, adjustment)) {
        days.add(cursor)
        cursor = cursor.minusDays(1)
    }
    days.reverse()
    cursor = date.plusDays(1)
    while (sameHijriMonth(cursor, target, adjustment)) {
        days.add(cursor)
        cursor = cursor.plusDays(1)
    }
    return days
}

/**
 * The first day of the Hijri month [steps] away from [from].
 */
private fun shiftHijriMonth(from: LocalDate, steps: Int, adjustment: Int): LocalDate {
    val current = hijriOf(from, adjustment)
    val target = current.year * 12 + current.monthNumber + steps

    var probe = from
    // 400 days covers a year plus a margin; the loops are bounded so a broken
    // engine cannot spin here.
    var guard = 0
    while (hijriMonthIndex(probe, adjustment) < target && guard++ < 400) {
        probe = probe.plusDays(1)
    }
    while (hijriMonthIndex(probe, adjustment) > target && guard++ < 800) {
        probe = probe.minusDays(1)
    }
    // Now inside the target month; back up to its first day.
    while (hijriMonthIndex(probe.minusDays(1), adjustment) == target && guard++ < 800) {
        probe = probe.minusDays(1)
    }
    return probe
}

private fun hijriMonthIndex(date: LocalDate, adjustment: Int): Int {
    val h = hijriOf(date, adjustment)
    return h.year * 12 + h.monthNumber
}

private fun sameHijriMonth(candidate: LocalDate, month: HijriDate, adjustment: Int): Boolean {
    val h = hijriOf(candidate, adjustment)
    return h.monthNumber == month.monthNumber && h.year == month.year
}

/**
 * The Hijri date of a Gregorian day, with the reader's adjustment applied.
 */
private fun hijriOf(date: LocalDate, adjustment: Int): HijriDate =
    HijriCalendarEngine.getHijriDate(date.plusDays(adjustment.toLong()))

/**
 * The grid's columns, in the order the leading blanks count them.
 *
 * Sunday first, because [weekdayColumn] is `dayOfWeek.value % 7` over ISO's
 * Monday-first numbering, which lands Sunday in column one. The two are one decision
 * with two halves - a header in another order puts every date under the wrong initial,
 * and in a language whose initials are not all distinct that does not look wrong.
 */
internal val WEEKDAY_COLUMNS: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

/**
 * The column a weekday is drawn in, starting at nought.
 *
 * Shared with the header rather than written twice: the reader sees a date under an
 * initial and reads the two as one fact.
 */
internal fun weekdayColumn(day: DayOfWeek): Int = day.value % 7

/**
 * The seven weekday labels the month grid is ruled by, Sunday first, in [locale].
 *
 * Read from the platform rather than written as "S M T W T F S". Those seven letters
 * were the only English left in this sheet in all eleven languages, and the three T's
 * and two S's that make the English set ambiguous are exactly what a translated set
 * fixes.
 *
 * ### The fallback, and why it is not a detail
 *
 * The platform's *narrow* names are one letter in most languages, and for a language
 * that has none it answers with the **root locale's** - which are the English letters
 * again. Urdu's narrow names are exactly "S M T W T F S", so a header that read narrow
 * names and nothing else would have ruled the Urdu sheet in the same English letters the
 * literal did, in the one language the fix was for. A narrow set that is the root's while
 * the abbreviated set is the language's own is a *fallback*, not a translation, and the
 * abbreviated names ("اتوار") are what that language has. English is the root's own
 * language, so the same test would ask it for its abbreviated names as well; it is the
 * one locale where both sets are the root's, and there the narrow letters are the
 * design.
 */
internal fun weekdayLabels(locale: Locale): List<String> {
    val narrow = WEEKDAY_COLUMNS.map { it.getDisplayName(TextStyle.NARROW, locale) }
    val rootNarrow = WEEKDAY_COLUMNS.map { it.getDisplayName(TextStyle.NARROW, Locale.ROOT) }
    val abbreviated = WEEKDAY_COLUMNS.map { it.getDisplayName(TextStyle.SHORT, locale) }
    val rootAbbreviated = WEEKDAY_COLUMNS.map { it.getDisplayName(TextStyle.SHORT, Locale.ROOT) }

    return if (narrow != rootNarrow || abbreviated == rootAbbreviated) narrow else abbreviated
}
