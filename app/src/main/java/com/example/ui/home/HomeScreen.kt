package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Prayer
import com.example.engine.AstronomicalSky
import com.example.engine.QiblaEngine
import com.example.ui.SalahUiState
import com.example.ui.components.ContentColumn
import com.example.ui.components.LivingSkyCanvas
import com.example.ui.components.OptionSheet
import com.example.ui.components.SectionGroup
import com.example.ui.components.SectionHeader
import com.example.ui.components.StaticSkyBackground
import com.example.ui.components.StatusBanner
import com.example.ui.components.StatusDot
import com.example.ui.components.BannerTone
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.alertModeLabel
import com.example.ui.localization.prayerName
import com.example.ui.theme.ArabicFamily
import com.example.ui.theme.Space
import com.example.ui.theme.mix
import com.example.ui.theme.Tonal
import com.example.ui.theme.layoutMetrics
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Today.
 *
 * Reads top to bottom as: where you are, what the next prayer is and how long
 * until it, what today's prayers were, and one way back into what you were
 * reading. Nothing else competes for that order.
 *
 * The previous version wrapped all of this in eight translucent and bordered
 * surfaces over a moving sky, and carried six layout probes so its type could
 * change colour as the sun moved. The type is now fixed and the hierarchy comes
 * from size and space.
 */
@Composable
fun HomeScreen(
    state: SalahUiState,
    onTogglePrayer: (Prayer) -> Unit,
    onContinueReadingClick: () -> Unit,
    onOpenPrayerTimes: () -> Unit,
    onLocationClick: () -> Unit,
    onRefreshClick: () -> Unit = {},
    onToggleGlobalSilent: () -> Unit = {},
    onCyclePrayerAlertMode: (Prayer) -> Unit = {},
    onSilenceActiveAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val today = remember { LocalDate.now() }
    val timeFormatter = remember(state.timeFormat24h) {
        DateTimeFormatter.ofPattern(if (state.timeFormat24h) "HH:mm" else "h:mm a")
    }

    // The observatory is the one place the live sky is still explored. It is
    // reachable from the date line, not from a permanent chip in the toolbar.
    var showObservatory by rememberSaveable { mutableStateOf(false) }
    var simulatedMinutes by rememberSaveable { mutableStateOf<Float?>(null) }

    val activeTime: LocalTime = remember(simulatedMinutes) {
        simulatedMinutes?.let {
            LocalTime.ofSecondOfDay((it * 60).toInt().coerceIn(0, 86_399).toLong())
        } ?: LocalTime.now()
    }

    val sunPosition = remember(activeTime, state.location, state.sunPosition, simulatedMinutes) {
        if (simulatedMinutes != null) {
            QiblaEngine.calculateSunPosition(state.location, LocalDateTime.of(today, activeTime))
        } else {
            state.sunPosition
                ?: QiblaEngine.calculateSunPosition(state.location, LocalDateTime.of(today, LocalTime.now()))
        }
    }

    val skyPeriod = remember(activeTime, state.todayPrayerTimes, simulatedMinutes) {
        if (simulatedMinutes != null) {
            AstronomicalSky.determineSkyPeriod(activeTime, state.todayPrayerTimes)
        } else {
            state.skyPeriod
        }
    }

    val celestialProgress = remember(activeTime, state.todayPrayerTimes, simulatedMinutes) {
        if (simulatedMinutes != null) {
            AstronomicalSky.getCelestialBodyProgress(activeTime, state.todayPrayerTimes)
        } else {
            state.celestialProgress
        }
    }

    val palette = remember(sunPosition?.altitude, sunPosition?.azimuth) {
        AstronomicalSky.calculateContinuousSkyColors(
            sunPosition?.altitude ?: 0f,
            (sunPosition?.azimuth ?: 0f) > 180f
        )
    }

    // When the animated sky is on, the hero sits directly on it and needs a colour
    // chosen against it. When it is off - the default - the same code reads the
    // ordinary surface colours and no measurement is taken at all.
    val heroOnSky = state.livingSkyEnabled
    val heroText = if (heroOnSky) {
        onSkyColorFor(palette, sunPosition?.altitude ?: 0f, isDark)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.livingSkyEnabled) {
            LivingSkyCanvas(
                skyPeriod = skyPeriod,
                celestialProgress = celestialProgress,
                sunPosition = sunPosition,
                location = state.location,
                currentTime = activeTime,
                hijriDay = state.hijriDate?.day ?: 14,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("living_sky")
            )
        } else {
            StaticSkyBackground(
                palette = palette,
                page = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("static_sky")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeTopBar(
                locationName = state.location.name,
                onLocationClick = onLocationClick,
                isSyncing = state.isSyncing,
                isOnline = state.isOnline
            )

            ContentColumn {
                DateBlock(
                    hijriArabic = state.hijriDate?.formatArabic(),
                    gregorian = today.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
                    ),
                    skyLabel = skyPeriod.title,
                    onClick = { showObservatory = true },
                    contentColor = heroText
                )

                if (state.audioPreviewPlaying != null) {
                    Spacer(Modifier.height(space.md))
                    StatusBanner(
                        message = "${strings.more.adhanPlayingLabel} · ${state.audioPreviewPlaying}",
                        tone = BannerTone.Danger,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        action = {
                            TextButton(onClick = onSilenceActiveAlert) {
                                Text(strings.more.silenceAdhan, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(space.xxl))

                NextPrayerBlock(
                    state = state,
                    timeFormatter = timeFormatter,
                    onOpenPrayerTimes = onOpenPrayerTimes,
                    contentColor = heroText
                )

                Spacer(Modifier.height(space.xl))

                AlertStatusRow(
                    isSilent = state.isGlobalSilentMode,
                    autoSilenceMinutes = state.autoSilentDurationMinutes
                        .takeIf { state.autoSilentDuringPrayer },
                    onToggleSilent = onToggleGlobalSilent,
                    contentColor = heroText
                )

                SectionHeader(strings.todaysPrayers)

                val fard = listOf(
                    Prayer.FAJR to state.prayerLog.fajrDone,
                    Prayer.DHUHR to state.prayerLog.dhuhrDone,
                    Prayer.ASR to state.prayerLog.asrDone,
                    Prayer.MAGHRIB to state.prayerLog.maghribDone,
                    Prayer.ISHA to state.prayerLog.ishaDone
                )
                val times = state.todayPrayerTimes?.prayers?.associateBy { it.prayer } ?: emptyMap()

                SectionGroup {
                    fard.forEachIndexed { index, (prayer, isDone) ->
                        if (index > 0) {
                            Spacer(Modifier.height(space.xxs))
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(space.xxs))
                        }
                        PrayerRow(
                            prayer = prayer,
                            time = times[prayer]?.time?.format(timeFormatter) ?: "--:--",
                            isNext = times[prayer]?.isNext == true,
                            isDone = isDone,
                            alertMode = state.prayerAlertModes[prayer] ?: DEFAULT_ALERT_MODE,
                            alertsSilenced = state.isGlobalSilentMode,
                            onToggleDone = { onTogglePrayer(prayer) },
                            onCycleAlert = { onCyclePrayerAlertMode(prayer) }
                        )
                    }
                }

                SectionHeader(strings.continueReading)

                ContinueReadingRow(
                    surahName = state.continueReading.surahName,
                    reference = "${state.continueReading.surahNumber}:${state.continueReading.ayahNumber}",
                    snippet = state.continueReading.snippetAr,
                    onClick = onContinueReadingClick
                )

                Spacer(Modifier.height(space.xl))

                SourceFootnote(method = state.method.title, location = state.location.name)
            }

            Spacer(Modifier.height(space.xxxl))
        }
    }

    if (showObservatory) {
        ObservatorySheet(
            locationName = state.location.name,
            currentTime = activeTime,
            isRealTime = simulatedMinutes == null,
            sunAltitude = sunPosition?.altitude ?: 0f,
            sunAzimuth = sunPosition?.azimuth ?: 0f,
            skyPeriodTitle = skyPeriod.title,
            skyPeriodArabic = skyPeriod.arabicTitle,
            hijriDay = state.hijriDate?.day ?: 14,
            hijriMonthEn = state.hijriDate?.monthNameEn ?: "",
            simulatedMinutes = simulatedMinutes ?: (activeTime.toSecondOfDay() / 60f),
            onMinutesChanged = { simulatedMinutes = it },
            onResetToRealTime = { simulatedMinutes = null },
            onDismiss = { showObservatory = false }
        )
    }
}

private const val DEFAULT_ALERT_MODE = "Full Adhan"

/** Status bar height plus any display cutout, as a Dp. */
@Composable
private fun statusBarTop(): androidx.compose.ui.unit.Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val px = maxOf(
        WindowInsets.statusBars.getTop(density),
        WindowInsets.displayCutout.getTop(density)
    )
    return with(density) { px.toDp() }
}

/**
 * Location and connection state, and nothing else.
 *
 * Replaces a top bar that also carried a wordmark nobody needed, a settings gear
 * that was unreachable, and a three-state animated sync badge. Connection is now
 * a dot that only appears when there is something to say.
 */
@Composable
private fun HomeTopBar(
    locationName: String,
    onLocationClick: () -> Unit,
    isSyncing: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarTop() + space.sm, start = space.lg, end = space.lg, bottom = space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onLocationClick)
                .padding(horizontal = space.sm, vertical = space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = locationName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isSyncing) {
                Spacer(Modifier.width(space.sm))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(14.dp)
                        .clearAndSetSemantics { },
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!isOnline) {
                Spacer(Modifier.width(space.sm))
                StatusDot(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = strings.offlineStatus
                )
            }
        }
    }
}

/** Date, with the sky period as the tap target for the observatory. */
@Composable
private fun DateBlock(
    hijriArabic: String?,
    gregorian: String,
    skyLabel: String,
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space.xxs)
    ) {
        Text(
            text = gregorian,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
        if (hijriArabic != null) {
            Text(
                text = hijriArabic,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = ArabicFamily,
                color = contentColor
            )
        }
        Spacer(Modifier.height(space.xs))
        Text(
            text = skyLabel,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(onClick = onClick)
                .padding(vertical = space.sm, horizontal = space.xs)
        )
    }
}

/**
 * The one number that matters right now.
 *
 * The countdown is the only place in the app that uses [Typography.displayLarge].
 * Everything around it is deliberately smaller so this reads first.
 */
@Composable
private fun NextPrayerBlock(
    state: SalahUiState,
    timeFormatter: DateTimeFormatter,
    onOpenPrayerTimes: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val next = state.nextPrayer

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space.xs)
    ) {
        Text(
            text = strings.nextPrayerLabel,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )

        if (next == null) {
            Text(
                text = "--:--",
                style = MaterialTheme.typography.displayLarge,
                color = contentColor
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = next.prayer.arabicName,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = ArabicFamily,
                color = contentColor
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = strings.prayerName(next.prayer),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Countdown(value = state.countdownString, contentColor = contentColor)

        Spacer(Modifier.height(space.xs))

        Row(
            modifier = Modifier
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onOpenPrayerTimes)
                .padding(end = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = next.time.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = strings.viewDetails,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            Spacer(Modifier.width(space.xs))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .clearAndSetSemantics { }
            )
        }
    }
}

/**
 * Hours, minutes, seconds.
 *
 * Announced as a single phrase once a minute rather than as three numbers every
 * second, which is what animating this on a per-second basis did to a screen
 * reader before.
 */
@Composable
private fun Countdown(
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val parts = remember(value) { value.split(":") }
    val hours = parts.getOrElse(0) { "00" }
    val minutes = parts.getOrElse(1) { "00" }
    val seconds = parts.getOrElse(2) { "00" }

    var spoken by remember { mutableStateOf("") }
    LaunchedEffect(minutes, hours) {
        spoken = "$hours ${strings.hoursUnit} $minutes ${strings.minsUnit}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("countdown")
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
            },
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "$hours:$minutes:$seconds",
            style = MaterialTheme.typography.displayLarge,
            color = contentColor,
            maxLines = 1
        )
    }
}

/** Silent-mode state, and the way into it. */
@Composable
private fun AlertStatusRow(
    isSilent: Boolean,
    autoSilenceMinutes: Int?,
    onToggleSilent: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onToggleSilent)
                .padding(horizontal = space.xs, vertical = space.xs)
                .semantics {
                    stateDescription = if (isSilent) {
                        strings.silentModeOn
                    } else {
                        strings.alertsActive
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSilent) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(18.dp)
                    .clearAndSetSemantics { }
            )
            Spacer(Modifier.width(space.sm))
            Text(
                text = if (isSilent) strings.silentModeOn else strings.alertsActive,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }

        if (autoSilenceMinutes != null) {
            Text(
                text = "${strings.masjidMode} · ${autoSilenceMinutes}${strings.more.minutesShort}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One prayer: when it is, whether it is done, and how it will alert.
 *
 * The row is one flat list item with a hairline between rows rather than five
 * separate bordered cards, so the eye reads it as a schedule instead of a stack
 * of panels. The alert mode is text, not a coloured pill - colour was carrying
 * that meaning before and it was the loudest thing in the row.
 */
@Composable
private fun PrayerRow(
    prayer: Prayer,
    time: String,
    isNext: Boolean,
    isDone: Boolean,
    alertMode: String,
    alertsSilenced: Boolean,
    onToggleDone: () -> Unit,
    onCycleAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val semantic = Tonal.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .padding(vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(prayer.iconRes),
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
                        text = strings.prayerName(prayer),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isNext) {
                        Spacer(Modifier.width(space.sm))
                        Text(
                            text = strings.nextPrayerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = prayer.arabicName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = ArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }

            Text(
                text = strings.more.alertModeLabel(alertMode),
                style = MaterialTheme.typography.labelSmall,
                color = if (alertsSilenced) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    semantic.onSuccessContainer
                },
                modifier = Modifier
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = onCycleAlert)
                    .padding(horizontal = space.sm, vertical = space.sm)
                    // This is a button, not a label: tapping it cycles the alert
                    // mode. It had no role and no indication of what the tap
                    // would do, so TalkBack read it as static text.
                    .semantics {
                        role = Role.Button
                        contentDescription = strings.more.changeAlertMode
                        stateDescription = strings.more.alertModeLabel(alertMode)
                    }
            )

            Spacer(Modifier.width(space.sm))

            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End
            )

            Spacer(Modifier.width(space.xs))

            IconButton(
                onClick = onToggleDone,
                modifier = Modifier
                    .size(MaterialTheme.layoutMetrics.minTouchTarget)
                    .testTag("prayer_done_${prayer.name}")
            ) {
                Icon(
                    imageVector = if (isDone) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = if (isDone) {
                        strings.more.prayerMarkedDone
                    } else {
                        strings.more.prayerMarkedPending
                    },
                    tint = if (isDone) {
                        semantic.success
                    } else {
                        // onSurfaceVariant, not outline. `outline` is tuned for
                        // dividers and unselected boundaries and measured 2.89:1
                        // against `surface` in dark mode - under the 3:1 that
                        // WCAG 1.4.11 requires of a control boundary. This circle
                        // is the row's primary control, not decoration, so it now
                        // uses the role Material's own Checkbox uses for its
                        // unchecked border: 7.35:1 dark, 7.58:1 light.
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// The five prayer-alert mode strings are persisted preference keys, not copy,
// so they stay English; `alertModeLabel` maps a key to a localized label. See
// its docstring in SalahLocalization.kt before "fixing" them to be translated.

/** One row that returns the user to where they left off reading. */
@Composable
private fun ContinueReadingRow(
    surahName: String,
    reference: String,
    snippet: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("continue_reading")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .clickable(onClick = onClick)
                .padding(space.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surahName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reference,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (snippet.isNotBlank()) {
                    Spacer(Modifier.height(space.sm))
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = ArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(space.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = strings.continueButton,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Where the numbers came from. One line, muted, at the bottom. */
@Composable
private fun SourceFootnote(method: String, location: String, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Text(
        text = "${strings.sourceLabel}: $method · $location",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * The sky observatory.
 *
 * Still the app's astronomy panel, reached from the sky-period line. The scrubber
 * lets you move through the day and watch the sky respond - that is the feature
 * worth keeping, and it no longer requires the whole Home screen to be rebuilt
 * around a moving background.
 */
@Composable
private fun ObservatorySheet(
    locationName: String,
    currentTime: LocalTime,
    isRealTime: Boolean,
    sunAltitude: Float,
    sunAzimuth: Float,
    skyPeriodTitle: String,
    skyPeriodArabic: String,
    hijriDay: Int,
    hijriMonthEn: String,
    simulatedMinutes: Float,
    onMinutesChanged: (Float) -> Unit,
    onResetToRealTime: () -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current
    val moon = remember(hijriDay) { AstronomicalSky.getMoonPhaseInfo(hijriDay) }

    OptionSheet(
        title = strings.more.observatoryTitle,
        subtitle = strings.more.observatorySubtitle.format(locationName),
        onDismiss = onDismiss
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(space.lg)) {
                Row(horizontalArrangement = Arrangement.spacedBy(space.lg)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.more.solarAltitude,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.getDefault(), "%+.1f°", sunAltitude
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.more.solarAzimuth,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.getDefault(), "%.1f°", sunAzimuth
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(space.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(space.lg)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.more.lunarPhase,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = moon.nameEn.substringBefore(" ("),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.more.illumination,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(moon.illumination * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (hijriMonthEn.isNotBlank()) {
                    Spacer(Modifier.height(space.lg))
                    Text(
                        text = "${strings.more.skyPeriodLabel}: $skyPeriodTitle · $skyPeriodArabic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(space.xl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.more.timeScrubber,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = simulatedMinutes,
            onValueChange = onMinutesChanged,
            valueRange = 0f..1439f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("observatory_scrubber")
        )

        if (!isRealTime) {
            Spacer(Modifier.height(space.sm))
            TextButton(onClick = onResetToRealTime) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(space.xs))
                Text(strings.resetToRealtime)
            }
        }
    }
}

/** Picks black or white for text sitting directly on the animated sky. */
private fun onSkyColorFor(
    palette: com.example.engine.SkyColorPalette,
    sunAltitude: Float,
    isDark: Boolean
): Color {
    val mid = palette.midSkyColor.mix(palette.horizonColor, 0.3f)
    return if (mid.luminance() > 0.45f && sunAltitude > -6f) {
        Color(0xFF0A1018)
    } else if (isDark) {
        Color(0xFFF2F7FC)
    } else {
        Color(0xFF0A1018)
    }
}
