package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Prayer
import com.example.engine.AstronomicalSky
import com.example.engine.QiblaEngine
import com.example.ui.SalahUiState
import com.example.ui.components.LivingSkyCanvas
import com.example.ui.components.SalahTopBar
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.prayerName
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.expressiveCollapse
import com.example.ui.theme.expressiveExpand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: SalahUiState,
    onTogglePrayer: (Prayer) -> Unit,
    onContinueReadingClick: () -> Unit,
    onOpenPrayerDetails: () -> Unit,
    onLocationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit = {},
    onToggleGlobalSilent: () -> Unit = {},
    onCyclePrayerAlertMode: (Prayer) -> Unit = {},
    onSilenceActiveAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val strings = LocalStrings.current
    val today = LocalDate.now()
    val dayOfWeek = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))

    // Interactive astronomical sky exploration states
    var simulatedMinutes by remember { mutableStateOf<Float?>(null) }
    var showAstroSheet by remember { mutableStateOf(false) }

    val activeTime: LocalTime = remember(simulatedMinutes) {
        simulatedMinutes?.let { mins ->
            val totalSecs = (mins * 60).toInt().coerceIn(0, 86399)
            LocalTime.ofSecondOfDay(totalSecs.toLong())
        } ?: LocalTime.now()
    }

    val activeSunPosition = remember(activeTime, state.location, state.sunPosition, simulatedMinutes) {
        if (simulatedMinutes != null) {
            val ldt = LocalDateTime.of(today, activeTime)
            QiblaEngine.calculateSunPosition(state.location, ldt)
        } else {
            state.sunPosition ?: run {
                val ldt = LocalDateTime.of(today, LocalTime.now())
                QiblaEngine.calculateSunPosition(state.location, ldt)
            }
        }
    }

    val activeSkyPeriod = remember(activeTime, state.todayPrayerTimes, simulatedMinutes) {
        if (simulatedMinutes != null) {
            AstronomicalSky.determineSkyPeriod(activeTime, state.todayPrayerTimes)
        } else {
            state.skyPeriod
        }
    }

    val activeCelestialProgress = remember(activeTime, state.todayPrayerTimes, simulatedMinutes) {
        if (simulatedMinutes != null) {
            AstronomicalSky.getCelestialBodyProgress(activeTime, state.todayPrayerTimes)
        } else {
            state.celestialProgress
        }
    }

    val nextPt = state.nextPrayer
    val prevPt = state.previousPrayer
    val onSkyColor = activeSkyPeriod.contentOnSkyColor

    // Ambient contrast shadow to ensure crisp legibility over dynamic sky gradients
    val textShadowColor = remember(activeSkyPeriod.isNight) {
        if (activeSkyPeriod.isNight) Color.Black.copy(alpha = 0.75f)
        else Color.White.copy(alpha = 0.65f)
    }
    val textShadow = remember(textShadowColor) {
        Shadow(
            color = textShadowColor,
            offset = Offset(0f, 1.5f),
            blurRadius = 4f
        )
    }
    val prominentTextShadow = remember(textShadowColor) {
        Shadow(
            color = textShadowColor,
            offset = Offset(0f, 2f),
            blurRadius = 8f
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Living Sky Canvas (Full screen background transitioning continuously with time & location)
        LivingSkyCanvas(
            skyPeriod = activeSkyPeriod,
            celestialProgress = activeCelestialProgress,
            sunPosition = activeSunPosition,
            location = state.location,
            currentTime = activeTime,
            hijriDay = state.hijriDate?.day ?: 14,
            modifier = Modifier
                .fillMaxSize()
                .testTag("living_astronomical_sky_canvas")
        )

        // Cinematic legibility scrim: shields the camera-hole zone and the hero
        // countdown from bright sky wash, and grounds the bottom above the nav bar.
        // No click handling — touches pass straight through to content.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.22f to Color.Black.copy(alpha = 0.28f),
                        0.42f to Color.Black.copy(alpha = 0.12f),
                        0.62f to Color.Black.copy(alpha = 0.08f),
                        0.82f to Color.Black.copy(alpha = 0.22f),
                        1f to Color.Black.copy(alpha = 0.45f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Minimalist Top Bar on Sky without SALAH title or Settings icon on Today screen
                SalahTopBar(
                    location = state.location,
                    onLocationClick = onLocationClick,
                    onSettingsClick = onSettingsClick,
                    isTransparentOnSky = true,
                    contentColor = onSkyColor,
                    isOnline = state.isOnline,
                    isSyncing = state.isSyncing,
                    onRefreshClick = onRefreshClick,
                    showBrand = false,
                    showSettings = false
                )

            Spacer(modifier = Modifier.height(6.dp))

            // Minimalist Date Sanctuary (Harmonizing Hijri & Gregorian, uncrowded and serene with enhanced contrast)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("date_sanctuary_card"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Prominent Arabic Calligraphic Hijri Date
                state.hijriDate?.let {
                    Text(
                        text = it.formatArabic(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSkyColor,
                        style = TextStyle(shadow = textShadow),
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Gregorian Date & Transliterated Hijri Subtitle
                val formattedGregorian = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
                val hijriTransliterated = state.hijriDate?.let { "${it.day} ${it.monthNameEn} ${it.year} AH" }

                Text(
                    text = if (hijriTransliterated != null) "$formattedGregorian · $hijriTransliterated" else formattedGregorian,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = onSkyColor,
                    style = TextStyle(shadow = textShadow),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("hijri_date_label")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Living Astronomical Sky Badge (High visibility affordance on sky background)
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(onSkyColor.copy(alpha = 0.30f))
                        .border(
                            BorderStroke(1.dp, onSkyColor.copy(alpha = 0.45f)),
                            CircleShape
                        )
                        .clickable { showAstroSheet = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("astronomical_sky_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val altVal = activeSunPosition?.altitude ?: 0f
                    val altFormatted = String.format(Locale.US, "%+.1f°", altVal)
                    val moonInfo = AstronomicalSky.getMoonPhaseInfo(state.hijriDate?.day ?: 14)

                    Icon(
                        imageVector = if (altVal > -0.833f) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = onSkyColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (altVal > -0.833f) "Sun $altFormatted" else moonInfo.nameEn.substringBefore(" ("),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSkyColor
                    )
                    Text(
                        text = "·",
                        fontSize = 12.sp,
                        color = onSkyColor.copy(alpha = 0.70f)
                    )
                    Text(
                        text = if (simulatedMinutes != null) "Time Preview (${activeTime.format(DateTimeFormatter.ofPattern("HH:mm"))})" else "${activeSkyPeriod.title} Sky ✦",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSkyColor
                    )
                }
            }

            // Interactive simulation reset banner when user scrubbed time
            AnimatedVisibility(
                visible = simulatedMinutes != null,
                enter = expressiveExpand(),
                exit = expressiveCollapse()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { simulatedMinutes = null },
                        label = { Text(strings.resetToRealtime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset time",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)
                        ),
                        modifier = Modifier.testTag("reset_realtime_chip")
                    )
                }
            }

            // Live Adhan / Alert Playing Banner
            AnimatedVisibility(
                visible = state.audioPreviewPlaying != null,
                enter = expressiveExpand(),
                exit = expressiveCollapse()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Adhan Playing · ${state.audioPreviewPlaying}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    FilledTonalButton(
                        onClick = onSilenceActiveAlert,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("home_silence_adhan_button"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Silence", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Quick Alert / Silent Mode Status Strip
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val silentContainer by animateColorAsState(
                    targetValue = if (state.isGlobalSilentMode) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surface,
                    label = "silent_chip_container"
                )
                AssistChip(
                    onClick = onToggleGlobalSilent,
                    label = {
                        Text(
                            text = if (state.isGlobalSilentMode) strings.silentModeOn else strings.alertsActive,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (state.isGlobalSilentMode) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.isGlobalSilentMode) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (state.isGlobalSilentMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = silentContainer,
                        labelColor = if (state.isGlobalSilentMode) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (state.isGlobalSilentMode) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .semantics {
                            stateDescription = if (state.isGlobalSilentMode) "Silent mode on" else "Alerts active"
                        }
                        .testTag("home_silent_mode_chip")
                )

                if (state.autoSilentDuringPrayer) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onSettingsClick,
                        label = { Text("${strings.masjidMode} (${state.autoSilentDurationMinutes}m)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.testTag("home_masjid_mode_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dominant Next Prayer Hero Section (Deeply Immersive Architectural Countdown with enhanced contrast)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (nextPt != null) {
                    // Next prayer sub-label
                    Text(
                        text = strings.nextPrayerLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = onSkyColor,
                        style = TextStyle(shadow = textShadow)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Next prayer Arabic calligraphy + English Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = nextPt.prayer.arabicName,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = onSkyColor,
                            style = TextStyle(shadow = prominentTextShadow)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "·",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSkyColor.copy(alpha = 0.70f),
                            style = TextStyle(shadow = textShadow)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.prayerName(nextPt.prayer).uppercase(),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = onSkyColor,
                            style = TextStyle(shadow = prominentTextShadow)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Typographic Countdown Timer with Enhanced Visibility & Legibility
                    val countdownParts = state.countdownString.split(":")
                    val hoursStr = countdownParts.getOrElse(0) { "00" }
                    val minutesStr = countdownParts.getOrElse(1) { "00" }
                    val secondsStr = countdownParts.getOrElse(2) { "00" }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("countdown_timer")
                    ) {
                        CountdownSegment(value = hoursStr, label = strings.hoursUnit, onSkyColor = onSkyColor, shadow = textShadow)
                        CountdownDotsSeparator(onSkyColor = onSkyColor, shadowColor = textShadowColor)
                        CountdownSegment(value = minutesStr, label = strings.minsUnit, onSkyColor = onSkyColor, shadow = textShadow)
                        CountdownDotsSeparator(onSkyColor = onSkyColor, shadowColor = textShadowColor)
                        CountdownSegment(value = secondsStr, label = strings.secsUnit, onSkyColor = onSkyColor, shadow = textShadow)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next Prayer Time Pill & Details Affordance (Enhanced contrast capsule)
                    val timeFormatter = DateTimeFormatter.ofPattern(if (state.timeFormat24h) "HH:mm" else "h:mm a")
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onOpenPrayerDetails)
                            .testTag("next_prayer_badge"),
                        shape = MaterialTheme.shapes.medium,
                        color = onSkyColor.copy(alpha = 0.30f),
                        border = BorderStroke(1.dp, onSkyColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(onSkyColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.adhanAt} ${nextPt.time.format(timeFormatter)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSkyColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "·",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSkyColor.copy(alpha = 0.70f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.viewDetails,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSkyColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = onSkyColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Atmospheric Sky Period & Solar Context
                prevPt?.let {
                    Text(
                        text = "${strings.currentPeriod}: ${strings.prayerName(it.prayer)} · ${state.skyPeriod.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSkyColor,
                        style = TextStyle(shadow = textShadow),
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section 1: Daily Prayer Flow & Completion Checklist directly on background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.todaysPrayers,
                    style = MaterialTheme.typography.titleLarge.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    color = onSkyColor
                )
                Text(
                    text = strings.tapToMarkCompleted,
                    style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Medium,
                    color = onSkyColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val fardPrayers = listOf(
                Prayer.FAJR to state.prayerLog.fajrDone,
                Prayer.DHUHR to state.prayerLog.dhuhrDone,
                Prayer.ASR to state.prayerLog.asrDone,
                Prayer.MAGHRIB to state.prayerLog.maghribDone,
                Prayer.ISHA to state.prayerLog.ishaDone
            )

            val times = state.todayPrayerTimes?.prayers?.associateBy { it.prayer } ?: emptyMap()

            fardPrayers.forEach { (prayer, isDone) ->
                val pt = times[prayer]
                val isNext = pt?.isNext == true
                val timePattern = if (state.timeFormat24h) "HH:mm" else "h:mm a"
                val timeFormatted = pt?.time?.format(DateTimeFormatter.ofPattern(timePattern)) ?: "--:--"

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onTogglePrayer(prayer) }
                        .testTag("prayer_row_${prayer.name}"),
                    color = if (isNext) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    border = if (isNext) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = if (isNext) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = prayer.icon,
                                contentDescription = strings.prayerName(prayer),
                                tint = if (isNext) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.prayerName(prayer),
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                                    text = prayer.arabicName,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timeFormatted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Interactive Prayer Alert Mode Pill (Tap to cycle modes: Adhan -> Takbeer -> Chime -> Vibrate -> Silent)
                            val alertMode = state.prayerAlertModes[prayer] ?: "Full Adhan"
                            val isSilenced = state.isGlobalSilentMode || alertMode == "Silent" || alertMode == "Silent Reminder"
                            val alertContainer by animateColorAsState(
                                targetValue = if (isSilenced) MaterialTheme.colorScheme.surfaceContainerHighest
                                else MaterialTheme.colorScheme.primaryContainer,
                                label = "alert_pill_container"
                            )
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable { onCyclePrayerAlertMode(prayer) }
                                    .semantics { stateDescription = "Alert mode: $alertMode" }
                                    .testTag("prayer_alert_toggle_${prayer.name}"),
                                color = alertContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            state.isGlobalSilentMode || alertMode == "Silent" || alertMode == "Silent Reminder" -> Icons.AutoMirrored.Filled.VolumeOff
                                            alertMode == "Vibrate Only" -> Icons.Default.Vibration
                                            alertMode == "Gentle Chime" -> Icons.Default.NotificationsNone
                                            else -> Icons.AutoMirrored.Filled.VolumeUp
                                        },
                                        contentDescription = "Alert mode: $alertMode",
                                        tint = if (isSilenced) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (alertMode) {
                                            "Full Adhan" -> "Adhan"
                                            "Takbeer Only" -> "Takbeer"
                                            "Gentle Chime" -> "Chime"
                                            "Vibrate Only" -> "Vibrate"
                                            else -> "Silent"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSilenced) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            // Completion Toggle (minimum 48dp touch target standard)
                            IconButton(
                                onClick = { onTogglePrayer(prayer) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("prayer_done_toggle_${prayer.name}")
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isDone) "Marked done" else "Mark incomplete",
                                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: First-Class "Continue Reading" Card directly on background
            Text(
                text = strings.continueReading.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(shadow = textShadow),
                fontWeight = FontWeight.Bold,
                color = onSkyColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onContinueReadingClick)
                    .testTag("continue_reading_card"),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Quran reading",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = state.continueReading.surahName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${state.continueReading.surahNumber}:${state.continueReading.ayahNumber} · ${strings.pageTab} ${state.continueReading.pageNumber}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onContinueReadingClick,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("continue_reading_button")
                        ) {
                            Text(strings.continueButton, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Arabic snippet ﴿ ... ﴾
                    Text(
                        text = "﴿ ${state.continueReading.snippetAr} ﴾",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: App Trust Status Pill directly on background
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Trust status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strings.sourceLabel}: ${state.method.title} · ${state.location.name}",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Astronomical Observatory Bottom Sheet
        if (showAstroSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAstroSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("astronomical_observatory_sheet")
            ) {
                AstronomicalObservatoryContent(
                    location = state.location,
                    currentTime = activeTime,
                    isRealTime = (simulatedMinutes == null),
                    sunPosition = activeSunPosition,
                    skyPeriod = activeSkyPeriod,
                    hijriDay = state.hijriDate?.day ?: 14,
                    hijriMonthEn = state.hijriDate?.monthNameEn ?: "Rabi' al-Awwal",
                    simulatedMinutes = simulatedMinutes ?: (activeTime.toSecondOfDay() / 60f),
                    onMinutesChanged = { newMins ->
                        simulatedMinutes = newMins
                    },
                    onResetToRealTime = {
                        simulatedMinutes = null
                    },
                    onClose = { showAstroSheet = false }
                )
            }
        }
    }
}

@Composable
private fun AstronomicalObservatoryContent(
    location: com.example.data.model.UserLocation,
    currentTime: LocalTime,
    isRealTime: Boolean,
    sunPosition: com.example.engine.SunPosition?,
    skyPeriod: com.example.engine.SkyPeriod,
    hijriDay: Int,
    hijriMonthEn: String,
    simulatedMinutes: Float,
    onMinutesChanged: (Float) -> Unit,
    onResetToRealTime: () -> Unit,
    onClose: () -> Unit
) {
    val alt = sunPosition?.altitude ?: 0f
    val az = sunPosition?.azimuth ?: 0f
    val moonInfo = AstronomicalSky.getMoonPhaseInfo(hijriDay)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Astronomical Observatory",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Living sky telemetry for ${location.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Grid Card
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Solar Elevation & Azimuth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SOLAR ALTITUDE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%+.1f° (%s)", alt, if (alt > -0.833f) "Above Horizon" else "Below Horizon"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (alt > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SOLAR AZIMUTH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f° True North", az),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Lunar Phase & Hijri Calendar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "LUNAR PHASE (HIJRI)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${moonInfo.nameEn} · Day $hijriDay $hijriMonthEn",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ILLUMINATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${(moonInfo.illumination * 100).toInt()}% (${if (moonInfo.isWaxing) "Waxing" else "Waning"})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Current Sky Horizon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ATMOSPHERIC HORIZON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${skyPeriod.title} (${skyPeriod.arabicTitle})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 24-Hour Sky Time Scrubber
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
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "24-Hour Solar Scrubber",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = currentTime.format(timeFormatter),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = simulatedMinutes,
            onValueChange = onMinutesChanged,
            valueRange = 0f..1439f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("astronomical_time_slider")
        )

        // Quick Pick Horizons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onMinutesChanged(5f * 60 + 15) },
                label = { Text("Dawn (05:15)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(6f * 60 + 45) },
                label = { Text("Sunrise (06:45)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(12f * 60 + 30) },
                label = { Text("Zenith (12:30)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(16f * 60 + 15) },
                label = { Text("Golden Hour (16:15)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(18f * 60 + 50) },
                label = { Text("Sunset (18:50)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(20f * 60 + 30) },
                label = { Text("Twilight (20:30)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onMinutesChanged(1f * 60 + 30) },
                label = { Text("Starry Night (01:30)", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset to Real-time or Done Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onResetToRealTime,
                enabled = !isRealTime,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Real-Time", fontSize = 12.5.sp)
            }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Done", fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun CountdownSegment(
    value: String,
    label: String,
    onSkyColor: Color,
    shadow: Shadow? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(durationMillis = ExpressiveMotion.SHORT),
                    initialOffsetY = { it / 3 }
                ) + fadeIn()) togetherWith
                    (slideOutVertically(
                        animationSpec = tween(durationMillis = ExpressiveMotion.SHORT),
                        targetOffsetY = { -it / 3 }
                    ) + fadeOut())
            },
            label = "countdown_digit"
        ) { digit ->
            Text(
                text = digit,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    shadow = shadow
                ),
                color = onSkyColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(shadow = shadow),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = onSkyColor,
        )
    }
}

@Composable
private fun CountdownDotsSeparator(
    onSkyColor: Color,
    shadowColor: Color = Color.Transparent
) {
    Column(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(onSkyColor)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(onSkyColor)
        )
    }
}

