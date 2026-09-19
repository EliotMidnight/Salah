package com.example.ui.prayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerTime
import com.example.engine.HijriCalendarEngine
import com.example.engine.PrayerCalculationEngine
import com.example.ui.SalahUiState
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.prayerName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    state: SalahUiState,
    onMethodChange: (CalculationMethod) -> Unit,
    onMadhhabChange: (Madhhab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showTrustLayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val isToday = selectedDate == LocalDate.now()

    // Calculate prayer times for selected date
    val dayPrayerTimes = remember(selectedDate, state.location, state.method, state.madhhab, state.adjustments) {
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

    val prayers = dayPrayerTimes?.prayers ?: emptyList()

    // Monthly calendar computation
    val currentMonth = selectedDate.month
    val daysInMonth = currentMonth.length(selectedDate.isLeapYear)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Day Switcher: < 2nd Rabi II 1448 > / Monday 15 Sep
                DaySwitcher(
                    selectedDate = selectedDate,
                    onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
                    onNextDay = { selectedDate = selectedDate.plusDays(1) },
                    onTodayClick = { selectedDate = LocalDate.now() },
                    todayLabel = strings.todayBtn
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Trust Layer Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showTrustLayerSheet = true }
                        .testTag("trust_layer_card"),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Trust verification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.transparentCalculationSource,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.method.title} · ${state.location.name}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Inspect source details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1 Header: Today's Times (or Selected Day's Times)
            Text(
                text = if (isToday) strings.todaysTimes else strings.prayerTimesHeader,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        val timePattern = if (state.timeFormat24h) "HH:mm" else "h:mm a"

        // Daily Prayers List with Soft Highlight (No 'NEXT' text)
        items(prayers) { pt ->
            PrayerRowCard(
                pt = pt,
                isNext = isToday && pt.isNext,
                timePattern = timePattern,
                localizedName = strings.prayerName(pt.prayer)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Night Periods (Imsak, Midnight, Last Third)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.vigilsAndNightPeriods,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    NightPeriodRow(
                        title = strings.imsakTitle,
                        arabic = "الإمساك",
                        time = dayPrayerTimes?.imsak?.format(DateTimeFormatter.ofPattern(timePattern)) ?: "--:--"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    NightPeriodRow(
                        title = strings.midnightTitle,
                        arabic = "منتصف الليل",
                        time = dayPrayerTimes?.midnight?.format(DateTimeFormatter.ofPattern(timePattern)) ?: "--:--"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    NightPeriodRow(
                        title = strings.lastThirdTitle,
                        arabic = "الثلث الأخير",
                        time = dayPrayerTimes?.lastThirdOfNight?.format(DateTimeFormatter.ofPattern(timePattern)) ?: "--:--"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2 Header: Monthly Calendar merged underneath
            Text(
                text = strings.monthlyCalendarHeader,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMonth.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap a day to view times",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Monthly Days List
        items((1..daysInMonth).toList()) { day ->
            val date = LocalDate.of(selectedDate.year, currentMonth, day)
            val isSelectedDay = day == selectedDate.dayOfMonth
            val isCurrentDay = day == LocalDate.now().dayOfMonth && currentMonth == LocalDate.now().month
            val times = remember(date, state.location, state.method, state.madhhab, state.adjustments) {
                PrayerCalculationEngine.calculatePrayerTimes(
                    date = date,
                    location = state.location,
                    method = state.method,
                    madhhab = state.madhhab,
                    adjustments = state.adjustments
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedDate = date },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelectedDay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface,
                border = if (isSelectedDay) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                else if (isCurrentDay) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = String.format("%02d", day),
                            fontSize = 13.sp,
                            fontWeight = if (isSelectedDay || isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrentDay) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    times.prayers.filter { it.prayer != Prayer.SUNRISE }.forEach { pt ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = pt.prayer.englishName.take(3),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pt.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                fontSize = 11.sp,
                                fontWeight = if (isSelectedDay) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
    }

    // Trust Layer Modal Bottom Sheet
    if (showTrustLayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrustLayerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Prayer Times Trust Layer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TrustItem(label = "Source", value = state.method.title)
                TrustItem(label = "Location", value = "${state.location.name}, ${state.location.country} (${String.format(Locale.US, "%.3f", state.location.latitude)}°, ${String.format(Locale.US, "%.3f", state.location.longitude)}°)")
                TrustItem(label = "Methodology", value = if (state.method.isMoroccoNationalTable) "Kingdom of Morocco National Habous Table calibration with astronomical solar fallback" else "Sun zenith angle equations: Fajr ${state.method.fajrAngle}°, Isha ${if (state.method.ishaAngle > 0) "${state.method.ishaAngle}°" else "90 min"}")
                TrustItem(label = "Madhhab (Asr Shadow)", value = "${state.madhhab.title} (Factor: ${state.madhhab.shadowFactor}x)")
                TrustItem(
                    label = "Applied Adjustments",
                    value = "Fajr ${offsetSign(state.adjustments.fajr)}, Dhuhr ${offsetSign(state.adjustments.dhuhr)}, Asr ${offsetSign(state.adjustments.asr)}, Maghrib ${offsetSign(state.adjustments.maghrib)}, Isha ${offsetSign(state.adjustments.isha)}"
                )
                TrustItem(label = "Offline Status", value = "100% computed on-device. No internet required.")
                TrustItem(label = "Last Sync / Verified", value = state.lastChecked)

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = { showTrustLayerSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DaySwitcher(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    todayLabel: String = "Today",
    modifier: Modifier = Modifier
) {
    val hijri = remember(selectedDate) { HijriCalendarEngine.getHijriDate(selectedDate) }
    val isToday = selectedDate == LocalDate.now()

    val ordinalDay = when {
        hijri.day in 11..13 -> "${hijri.day}th"
        hijri.day % 10 == 1 -> "${hijri.day}st"
        hijri.day % 10 == 2 -> "${hijri.day}nd"
        hijri.day % 10 == 3 -> "${hijri.day}rd"
        else -> "${hijri.day}th"
    }

    val hijriMonth = when (hijri.monthNumber) {
        1 -> "Muharram"
        2 -> "Safar"
        3 -> "Rabi I"
        4 -> "Rabi II"
        5 -> "Jumada I"
        6 -> "Jumada II"
        7 -> "Rajab"
        8 -> "Sha'ban"
        9 -> "Ramadan"
        10 -> "Shawwal"
        11 -> "Dhu al-Qi'dah"
        12 -> "Dhu al-Hijjah"
        else -> hijri.monthNameEn
    }

    val hijriText = "$ordinalDay $hijriMonth ${hijri.year}"
    val gregorianText = selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.testTag("day_switcher_prev")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isToday, onClick = onTodayClick)
            ) {
                Text(
                    text = hijriText,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = gregorianText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (!isToday) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = todayLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier.testTag("day_switcher_next")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    pt: PrayerTime,
    isNext: Boolean,
    timePattern: String = "HH:mm",
    localizedName: String = pt.prayer.englishName
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isNext) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pt.prayer.icon,
                        contentDescription = localizedName,
                        tint = if (isNext) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localizedName,
                            fontSize = 15.sp,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Soft highlight dot replacing "NEXT" text
                        if (isNext) {
                            Spacer(modifier = Modifier.width(7.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Text(
                        text = pt.prayer.arabicName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Text(
                text = pt.time.format(DateTimeFormatter.ofPattern(timePattern)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NightPeriodRow(title: String, arabic: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = arabic, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Serif)
        }
        Text(text = time, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TrustItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun offsetSign(minutes: Int): String {
    return if (minutes >= 0) "+$minutes" else "$minutes"
}
