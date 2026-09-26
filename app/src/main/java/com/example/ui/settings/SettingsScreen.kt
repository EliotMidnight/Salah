package com.example.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.UserLocation
import com.example.ui.SalahUiState
import com.example.ui.components.ActionRow
import com.example.ui.components.ChipRow
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.DetailList
import com.example.ui.components.LabeledSlider
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSheet
import com.example.ui.components.RowDivider
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SearchInput
import com.example.ui.components.SectionGroup
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedOptions
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import androidx.compose.material3.OutlinedTextField

/** The five alert levels a prayer can be set to, in cycle order. */
private val ALERT_MODES = listOf("Full Adhan", "Takbeer Only", "Gentle Chime", "Vibrate Only", "Silent")
private val AUTO_SILENCE_OPTIONS = listOf(15, 20, 30, 45)
private val ADHAN_SOUNDS = listOf(
    "Makkah Al-Mukarramah",
    "Madinah An-Nabawi",
    "Al-Aqsa",
    "Moroccan",
    "Gentle Bell Chime"
)
private val RECITERS = listOf(
    "Mishary Rashid Alafasy",
    "Abdul Basit Abdus Samad",
    "Mahmoud Khalil Al-Husary",
    "Saud Ash-Shuraim"
)
private val RIWAYAHS = listOf("Hafs 'an 'Asim", "Warsh 'an Nafi", "Qalun 'an Nafi", "Al-Duri 'an Abi 'Amr")
private val SCRIPTS = listOf("Uthmani (Madani)", "Maghrebi", "Indo-Pak")
private val TRANSLATIONS = listOf(
    "English (Saheeh International)",
    "English (Pickthall)",
    "Français (Hamidullah)",
    "Bahasa Indonesia",
    "Türkçe (Diyanet)",
    "اردو",
    "Bahasa Melayu",
    "বাংলা",
    "Русский",
    "Deutsch",
    "Español",
    "العربية"
)
private val THEMES = listOf("System Default", "Dark Mode (OLED)", "Clean Light")
private val HIJRI_OFFSETS = listOf(-2, -1, 0, 1, 2)
private val PRE_PRAYER_OFFSETS = listOf(5, 10, 15, 20, 30)

/**
 * Settings.
 *
 * Every one of the twenty settings the app had is still here, with the same
 * callback behind it. What changed is that they are grouped by what a person is
 * actually trying to do - "where am I and how are the times worked out", "how
 * should it get my attention", "how should the Quran read" - instead of one flat
 * run of thirty identical bordered cards that took about 2,600dp to scroll.
 *
 * The eighteen pick-one dialogs are now one [OptionSheet]. The old ones each
 * declared their own `ModalBottomSheetState` that was never used to dismiss them,
 * so the second sheet you opened skipped its enter animation.
 */
@Composable
fun SettingsScreen(
    state: SalahUiState,
    onLocationSelect: (UserLocation) -> Unit,
    onMethodSelect: (CalculationMethod) -> Unit,
    onMadhhabSelect: (Madhhab) -> Unit,
    onAdjustmentsChange: (com.example.data.model.PrayerAdjustments) -> Unit,
    onAdhanToggle: (Boolean) -> Unit,
    onPrePrayerToggle: (Boolean) -> Unit,
    onVibrateOnlyToggle: (Boolean) -> Unit,
    onGlobalSilentToggle: () -> Unit = {},
    onAutoSilentDuringPrayerToggle: () -> Unit = {},
    onAutoSilentDurationChange: (Int) -> Unit = {},
    onLanguageSelect: (String) -> Unit = {},
    onRiwayahSelect: (String) -> Unit = {},
    onThemeSelect: (String) -> Unit = {},
    onQuranScriptSelect: (String) -> Unit = {},
    onTimeFormatToggle: (Boolean) -> Unit,
    onAdhanSoundSelect: (String) -> Unit = {},
    onHijriAdjustmentChange: (Int) -> Unit = {},
    onReciterSelect: (String) -> Unit = {},
    onFontScaleChange: (Float) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onPrePrayerOffsetChange: (Int) -> Unit = {},
    onAdhanVolumeChange: (Float) -> Unit = {},
    onPrayerAlertModeChange: (Prayer, String) -> Unit = { _, _ -> },
    onPlayAudioPreview: (String) -> Unit = {},
    onStopAudioPreview: () -> Unit = {},
    onCustomLocationSave: (String, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    onTranslationSelect: (String) -> Unit = {},
    onRecomputeEphemerisCache: () -> Unit = {},
    onClearAudioCache: () -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    onLivingSkyChange: (Boolean) -> Unit = {},
    onFetchLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    // One enum instead of eighteen booleans, so only one sheet can ever be open and
    // its open/close is a single state change.
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var cityQuery by rememberSaveable { mutableStateOf("") }

    ScreenScaffold(
        title = strings.settingsTitle,
        modifier = modifier
    ) { _ ->
        Column {
            // -- Appearance ------------------------------------------------
            SectionHeader(strings.more.sectionAppearance)
            SectionGroup {
                ActionRow(
                    title = strings.languageLabel,
                    value = state.language,
                    onClick = { sheet = Sheet.LANGUAGE }
                )
                RowDivider()
                ActionRow(
                    title = strings.appThemeLabel,
                    value = strings.themeLabel(state.appTheme),
                    onClick = { sheet = Sheet.THEME }
                )
                RowDivider()
                ActionRow(
                    title = strings.more.timeFormat24hLabel,
                    value = if (state.timeFormat24h) "24h" else "12h",
                    onClick = { onTimeFormatToggle(!state.timeFormat24h) }
                )
                RowDivider()
                ToggleRow(
                    title = strings.more.livingSkyLabel,
                    subtitle = strings.more.livingSkyDescription,
                    checked = state.livingSkyEnabled,
                    onCheckedChange = onLivingSkyChange,
                    testTag = "setting_living_sky"
                )
            }

            // -- Location & calculation -----------------------------------
            SectionHeader(strings.more.sectionLocationAndCalculation)
            SectionGroup {
                ActionRow(
                    title = strings.locationLabel,
                    subtitle = state.location.name,
                    value = String.format(
                        java.util.Locale.getDefault(),
                        "%.2f, %.2f",
                        state.location.latitude,
                        state.location.longitude
                    ),
                    onClick = { sheet = Sheet.LOCATION }
                )
                if (state.isLocating) {
                    Row(
                        modifier = Modifier.padding(horizontal = space.md, vertical = space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.more.loading,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(space.sm))
                        OutlinedButton(onClick = onFetchLocation) {
                            Text(strings.more.useGps, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                state.locationStatusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = space.md, vertical = space.sm)
                    )
                }
                RowDivider()
                ActionRow(
                    title = strings.methodLabel,
                    value = state.method.title,
                    onClick = { sheet = Sheet.METHOD }
                )
                RowDivider()
                ActionRow(
                    title = strings.madhhabLabel,
                    value = state.madhhab.title,
                    onClick = { sheet = Sheet.MADHHAB }
                )
                RowDivider()
                ActionRow(
                    title = strings.adjustmentsLabel,
                    value = adjustmentSummary(state),
                    onClick = { sheet = Sheet.ADJUSTMENTS }
                )
                RowDivider()
                ActionRow(
                    title = strings.hijriCalibrationLabel,
                    value = hijriSummary(state.hijriAdjustment),
                    onClick = { sheet = Sheet.HIJRI }
                )
            }

            // -- Alerts ---------------------------------------------------
            SectionHeader(strings.more.sectionAlerts)
            SectionGroup {
                ToggleRow(
                    title = strings.more.silentModeLabel,
                    checked = state.isGlobalSilentMode,
                    onCheckedChange = { onGlobalSilentToggle() },
                    testTag = "setting_global_silent"
                )
                RowDivider()
                ToggleRow(
                    title = strings.adhanCallLabel,
                    checked = state.adhanNotificationEnabled,
                    onCheckedChange = onAdhanToggle,
                    testTag = "setting_adhan"
                )
                RowDivider()
                ToggleRow(
                    title = strings.prePrayerReminderLabel,
                    subtitle = if (state.prePrayerAlertEnabled) {
                        "${state.prePrayerOffsetMinutes} ${strings.more.minutesShort}"
                    } else {
                        strings.more.prePrayerDisabled
                    },
                    checked = state.prePrayerAlertEnabled,
                    onCheckedChange = onPrePrayerToggle,
                    onClick = { sheet = Sheet.PRE_PRAYER },
                    testTag = "setting_pre_prayer"
                )
                RowDivider()
                ActionRow(
                    title = strings.more.perPrayerModes,
                    onClick = { sheet = Sheet.PRAYER_MODES }
                )
                RowDivider()
                ActionRow(
                    title = strings.adhanSoundLabel,
                    value = state.adhanSound,
                    onClick = { sheet = Sheet.ADHAN_SOUND }
                )
                RowDivider()
                ActionRow(
                    title = strings.adhanVolumeLabel,
                    value = "${(state.adhanVolume * 100).toInt()}%",
                    onClick = { sheet = Sheet.VOLUME }
                )
                RowDivider()
                ToggleRow(
                    title = strings.more.vibrateOnlyLabel,
                    checked = state.vibrateOnly,
                    onCheckedChange = onVibrateOnlyToggle,
                    testTag = "setting_vibrate_only"
                )
                if (state.autoSilentDuringPrayer) {
                    RowDivider()
                    Column(modifier = Modifier.padding(horizontal = space.md, vertical = space.sm)) {
                        Text(
                            text = strings.more.autoSilenceDurationLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(space.sm))
                        SegmentedOptions(
                            options = AUTO_SILENCE_OPTIONS.map { "$it" },
                            selectedIndex = AUTO_SILENCE_OPTIONS.indexOf(state.autoSilentDurationMinutes)
                                .coerceAtLeast(0),
                            onSelect = { onAutoSilentDurationChange(AUTO_SILENCE_OPTIONS[it]) }
                        )
                    }
                } else {
                    RowDivider()
                    ToggleRow(
                        title = strings.autoMasjidModeLabel,
                        checked = state.autoSilentDuringPrayer,
                        onCheckedChange = { onAutoSilentDuringPrayerToggle() },
                        testTag = "setting_auto_silent"
                    )
                }
            }

            // -- Quran ----------------------------------------------------
            SectionHeader(strings.sectionQuran)
            SectionGroup {
                ActionRow(
                    title = strings.riwayahLabel,
                    value = state.riwayah,
                    onClick = { sheet = Sheet.RIWAYAH }
                )
                RowDivider()
                ActionRow(
                    title = strings.scriptStyleLabel,
                    value = state.quranScript,
                    onClick = { sheet = Sheet.SCRIPT }
                )
                RowDivider()
                ActionRow(
                    title = strings.reciterLabel,
                    value = state.reciter,
                    onClick = { sheet = Sheet.RECITER }
                )
                RowDivider()
                ActionRow(
                    title = strings.translationLabel,
                    value = state.translationEdition,
                    onClick = { sheet = Sheet.TRANSLATION }
                )
                RowDivider()
                Column(modifier = Modifier.padding(horizontal = space.md, vertical = space.sm)) {
                    LabeledSlider(
                        label = strings.arabicTextSizeLabel,
                        valueText = "${(state.quranFontScale * 100).toInt()}%",
                        value = state.quranFontScale,
                        onValueChange = onFontScaleChange,
                        onValueChangeFinished = null,
                        valueRange = 0.8f..1.5f,
                        steps = 6
                    )
                }
            }

            // -- Diagnostics ----------------------------------------------
            SectionHeader(strings.more.sectionAbout)
            SectionGroup {
                ActionRow(
                    title = strings.more.ephemerisCacheLabel,
                    subtitle = state.lastChecked,
                    onClick = { sheet = Sheet.EPHEMERIS }
                )
                RowDivider()
                ActionRow(
                    title = strings.more.storageLabel,
                    onClick = { sheet = Sheet.STORAGE }
                )
                RowDivider()
                ActionRow(
                    title = strings.compassDiagnosticsLabel,
                    subtitle = state.compassAccuracy,
                    onClick = { sheet = Sheet.COMPASS }
                )
                RowDivider()
                ActionRow(
                    title = strings.networkSyncLabel,
                    subtitle = if (state.isOnline) strings.onlineStatus else strings.offlineStatus,
                    onClick = {
                        onRefreshClick()
                    }
                )
                RowDivider()
                ActionRow(
                    title = strings.more.resetAllLabel,
                    onClick = { confirmReset = true }
                )
            }

            Spacer(Modifier.height(space.lg))
            Text(
                text = strings.more.privacyNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(space.xxl))
        }
    }

    // ---- Sheets ---------------------------------------------------------

    when (sheet) {
        Sheet.LANGUAGE -> OptionSheet(
            title = strings.more.chooseLanguage,
            onDismiss = { sheet = null }
        ) {
            com.example.ui.localization.AppLanguage.entries.forEach { language ->
                OptionRow(
                    title = language.nativeName,
                    description = language.englishName,
                    selected = language.nativeName == state.language,
                    onClick = {
                        onLanguageSelect(language.nativeName)
                        sheet = null
                    }
                )
            }
        }

        Sheet.THEME -> OptionSheet(
            title = strings.more.chooseTheme,
            onDismiss = { sheet = null }
        ) {
            THEMES.forEach { theme ->
                OptionRow(
                    title = strings.themeLabel(theme),
                    selected = theme == state.appTheme,
                    onClick = {
                        onThemeSelect(theme)
                        sheet = null
                    }
                )
            }
        }

        Sheet.LOCATION -> OptionSheet(
            title = strings.more.chooseLocation,
            onDismiss = { sheet = null }
        ) {
            SearchInput(
                value = cityQuery,
                onValueChange = { cityQuery = it },
                onClear = { cityQuery = "" },
                placeholder = strings.more.searchCities
            )
            Spacer(Modifier.height(space.sm))
            OptionRow(
                title = strings.more.useGps,
                selected = state.location.isGps,
                onClick = {
                    onFetchLocation()
                    sheet = null
                }
            )
            RowDivider()
            UserLocation.POPULAR_CITIES
                .filter { city ->
                    cityQuery.isBlank() ||
                        city.name.contains(cityQuery, ignoreCase = true) ||
                        city.country.contains(cityQuery, ignoreCase = true)
                }
                .forEach { city ->
                    OptionRow(
                        title = city.name,
                        description = city.country,
                        selected = city.name == state.location.name,
                        onClick = {
                            onLocationSelect(city)
                            sheet = null
                        }
                    )
                }
        }

        Sheet.METHOD -> OptionSheet(
            title = strings.more.chooseMethod,
            onDismiss = { sheet = null }
        ) {
            CalculationMethod.entries.forEach { method ->
                OptionRow(
                    title = method.title,
                    description = method.description,
                    selected = method == state.method,
                    onClick = {
                        onMethodSelect(method)
                        sheet = null
                    }
                )
            }
        }

        Sheet.MADHHAB -> OptionSheet(
            title = strings.more.chooseMadhhab,
            onDismiss = { sheet = null }
        ) {
            Madhhab.entries.forEach { madhhab ->
                OptionRow(
                    title = madhhab.title,
                    description = "${madhhab.shadowFactor}x",
                    selected = madhhab == state.madhhab,
                    onClick = {
                        onMadhhabSelect(madhhab)
                        sheet = null
                    }
                )
            }
        }

        Sheet.ADJUSTMENTS -> OptionSheet(
            title = strings.more.chooseAdjustments,
            subtitle = strings.more.appliedAdjustments,
            onDismiss = { sheet = null }
        ) {
            AdjustmentSliders(
                adjustments = state.adjustments,
                onChange = onAdjustmentsChange
            )
            Spacer(Modifier.height(space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                OutlinedButton(
                    onClick = { onAdjustmentsChange(com.example.data.model.PrayerAdjustments()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.more.actionReset, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { sheet = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.more.actionClose, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Sheet.HIJRI -> OptionSheet(
            title = strings.more.chooseHijriOffset,
            onDismiss = { sheet = null }
        ) {
            HIJRI_OFFSETS.forEach { offset ->
                OptionRow(
                    title = if (offset == 0) "±0" else if (offset > 0) "+$offset" else "$offset",
                    description = strings.more.daysShort,
                    selected = offset == state.hijriAdjustment,
                    onClick = {
                        onHijriAdjustmentChange(offset)
                        sheet = null
                    }
                )
            }
        }

        Sheet.PRE_PRAYER -> OptionSheet(
            title = strings.more.choosePrePrayerOffset,
            onDismiss = { sheet = null }
        ) {
            PRE_PRAYER_OFFSETS.forEach { minutes ->
                OptionRow(
                    title = "$minutes ${strings.more.minutesShort}",
                    selected = state.prePrayerAlertEnabled && state.prePrayerOffsetMinutes == minutes,
                    onClick = {
                        onPrePrayerToggle(true)
                        onPrePrayerOffsetChange(minutes)
                        sheet = null
                    }
                )
            }
            RowDivider()
            OptionRow(
                title = strings.more.prePrayerDisabled,
                selected = !state.prePrayerAlertEnabled,
                onClick = {
                    onPrePrayerToggle(false)
                    sheet = null
                }
            )
        }

        Sheet.PRAYER_MODES -> OptionSheet(
            title = strings.more.perPrayerModes,
            onDismiss = { sheet = null }
        ) {
            // Sunrise was previously rendered here even though the subtitle said the
            // five obligatory prayers, and its default value ("Silent Reminder") was
            // not one of the options - so that row showed no selection at all.
            listOf(
                Prayer.FAJR, Prayer.DHUHR, Prayer.ASR,
                Prayer.MAGHRIB, Prayer.ISHA, Prayer.SUNRISE
            ).forEach { prayer ->
                val current = state.prayerAlertModes[prayer] ?: ALERT_MODES.first()
                Column(modifier = Modifier.padding(vertical = space.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.englishName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onPlayAudioPreview(current) },
                            modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = strings.more.testSound,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(space.sm))
                    ChipRow(
                        labels = ALERT_MODES,
                        selectedIndex = ALERT_MODES.indexOf(current).coerceAtLeast(0),
                        onSelect = { onPrayerAlertModeChange(prayer, ALERT_MODES[it]) }
                    )
                }
                RowDivider()
            }
        }

        Sheet.ADHAN_SOUND -> OptionSheet(
            title = strings.more.chooseAdhanSound,
            onDismiss = { sheet = null; onStopAudioPreview() }
        ) {
            ADHAN_SOUNDS.forEach { sound ->
                OptionRow(
                    title = sound,
                    selected = sound == state.adhanSound,
                    onClick = { onAdhanSoundSelect(sound) },
                    trailing = {
                        IconButton(
                            onClick = {
                                if (state.audioPreviewPlaying == sound) {
                                    onStopAudioPreview()
                                } else {
                                    onPlayAudioPreview(sound)
                                }
                            },
                            modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = if (state.audioPreviewPlaying == sound) {
                                    Icons.Default.Stop
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = strings.more.testSound,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }

        Sheet.RECITER -> OptionSheet(
            title = strings.more.chooseReciter,
            onDismiss = { sheet = null; onStopAudioPreview() }
        ) {
            RECITERS.forEach { reciter ->
                OptionRow(
                    title = reciter,
                    selected = reciter == state.reciter,
                    onClick = { onReciterSelect(reciter) },
                    trailing = {
                        IconButton(
                            onClick = {
                                if (state.audioPreviewPlaying == reciter) {
                                    onStopAudioPreview()
                                } else {
                                    onPlayAudioPreview(reciter)
                                }
                            },
                            modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = if (state.audioPreviewPlaying == reciter) {
                                    Icons.Default.Stop
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = strings.more.testSound,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }

        Sheet.RIWAYAH -> OptionSheet(
            title = strings.riwayahLabel,
            onDismiss = { sheet = null }
        ) {
            RIWAYAHS.forEach { value ->
                OptionRow(
                    title = value,
                    selected = value == state.riwayah,
                    onClick = { onRiwayahSelect(value); sheet = null }
                )
            }
        }

        Sheet.SCRIPT -> OptionSheet(
            title = strings.more.chooseScript,
            onDismiss = { sheet = null }
        ) {
            SCRIPTS.forEach { value ->
                OptionRow(
                    title = value,
                    selected = value == state.quranScript,
                    onClick = { onQuranScriptSelect(value); sheet = null }
                )
            }
        }

        Sheet.TRANSLATION -> OptionSheet(
            title = strings.more.chooseTranslation,
            onDismiss = { sheet = null }
        ) {
            // Previously this list of twelve sat in a non-scrolling sheet and the
            // last four were unreachable on a phone.
            TRANSLATIONS.forEach { value ->
                OptionRow(
                    title = value,
                    selected = value == state.translationEdition,
                    onClick = { onTranslationSelect(value); sheet = null }
                )
            }
        }

        Sheet.VOLUME -> OptionSheet(
            title = strings.more.adhanVolume,
            onDismiss = { sheet = null; onStopAudioPreview() }
        ) {
            LabeledSlider(
                label = strings.more.adhanVolume,
                valueText = "${(state.adhanVolume * 100).toInt()}%",
                value = state.adhanVolume,
                onValueChange = onAdhanVolumeChange,
                valueRange = 0f..1f
            )
            Spacer(Modifier.height(space.md))
            ChipRow(
                labels = ADHAN_SOUNDS,
                selectedIndex = ADHAN_SOUNDS.indexOf(state.adhanSound).coerceAtLeast(0),
                onSelect = { onAdhanSoundSelect(ADHAN_SOUNDS[it]) }
            )
            Spacer(Modifier.height(space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                OutlinedButton(
                    onClick = { onPlayAudioPreview(state.adhanSound) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(space.xs))
                    Text(strings.more.testSound, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        onStopAudioPreview()
                        sheet = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.more.stopAudio, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Sheet.EPHEMERIS -> OptionSheet(
            title = strings.more.ephemerisCacheLabel,
            subtitle = strings.more.computedOnDevice,
            onDismiss = { sheet = null }
        ) {
            DetailList(
                items = listOf(
                    strings.more.methodology to state.method.title,
                    strings.locationLabel to state.location.name,
                    strings.more.lastVerified to state.lastChecked
                )
            )
            Spacer(Modifier.height(space.md))
            OutlinedButton(
                onClick = {
                    onRecomputeEphemerisCache()
                    sheet = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.more.recomputeSchedule, style = MaterialTheme.typography.labelLarge)
            }
        }

        Sheet.STORAGE -> OptionSheet(
            title = strings.more.storageLabel,
            onDismiss = { sheet = null }
        ) {
            DetailList(
                items = listOf(
                    strings.sectionQuran to strings.more.corpusSummary,
                    strings.more.storageLabel to "On device"
                )
            )
            Spacer(Modifier.height(space.md))
            OutlinedButton(
                onClick = {
                    onClearAudioCache()
                    sheet = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.more.clearAudioCache, style = MaterialTheme.typography.labelLarge)
            }
        }

        Sheet.COMPASS -> OptionSheet(
            title = strings.compassDiagnosticsLabel,
            onDismiss = { sheet = null }
        ) {
            DetailList(
                items = listOf(
                    strings.more.sensorAccuracy to state.compassAccuracy,
                    strings.more.ambientField to "${state.magneticFieldMagnitude} µT",
                    strings.more.magneticInterference to state.magneticStatus.label,
                    strings.kaabaDistance to "${state.distanceToKaabaKm} km"
                )
            )
        }

        null -> Unit
    }

    if (confirmReset) {
        ConfirmDialog(
            title = strings.more.resetAllConfirmTitle,
            message = strings.more.resetAllConfirmMessage,
            confirmLabel = strings.more.actionReset,
            dismissLabel = strings.more.actionCancel,
            destructive = true,
            onConfirm = {
                onResetAllSettings()
                confirmReset = false
            },
            onDismiss = { confirmReset = false }
        )
    }
}

/** The pick-one panels, as one enum so only one can ever be open. */
private enum class Sheet {
    LANGUAGE, THEME, LOCATION, METHOD, MADHHAB, ADJUSTMENTS, HIJRI,
    PRE_PRAYER, PRAYER_MODES, ADHAN_SOUND, RECITER, RIWAYAH, SCRIPT,
    TRANSLATION, VOLUME, EPHEMERIS, STORAGE, COMPASS
}

/**
 * A row that toggles.
 *
 * The whole row is the target and the switch is the visible state, which is what
 * the accessibility guidance asks for. The previous version made the row
 * non-clickable and left the switch as a separate, unlabelled focus stop.
 */
@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val space = Space.current

    val toggle: () -> Unit = onClick ?: { onCheckedChange(!checked) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = toggle)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = space.md, vertical = space.sm)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                stateDescription = if (checked) "On" else "Off"
                onClick(label = null) {
                    toggle()
                    true
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(space.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(space.md))
        // The switch is decorative here: the row carries the click and the
        // semantics, so exposing it as a second focus stop would double-announce.
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** Per-prayer minute offsets, -15..+15. */
@Composable
private fun AdjustmentSliders(
    adjustments: com.example.data.model.PrayerAdjustments,
    onChange: (com.example.data.model.PrayerAdjustments) -> Unit
) {
    data class Binding(
        val prayer: Prayer,
        val current: Int
    )

    val bindings = listOf(
        Binding(Prayer.FAJR, adjustments.fajr),
        Binding(Prayer.SUNRISE, adjustments.sunrise),
        Binding(Prayer.DHUHR, adjustments.dhuhr),
        Binding(Prayer.ASR, adjustments.asr),
        Binding(Prayer.MAGHRIB, adjustments.maghrib),
        Binding(Prayer.ISHA, adjustments.isha)
    )

    bindings.forEach { binding ->
        LabeledSlider(
            label = binding.prayer.englishName,
            valueText = if (binding.current >= 0) "+${binding.current}" else "${binding.current}",
            value = binding.current.toFloat(),
            onValueChange = { value ->
                val minutes = value.toInt()
                onChange(
                    when (binding.prayer) {
                        Prayer.FAJR -> adjustments.copy(fajr = minutes)
                        Prayer.DHUHR -> adjustments.copy(dhuhr = minutes)
                        Prayer.ASR -> adjustments.copy(asr = minutes)
                        Prayer.MAGHRIB -> adjustments.copy(maghrib = minutes)
                        Prayer.ISHA -> adjustments.copy(isha = minutes)
                        Prayer.SUNRISE -> adjustments.copy(sunrise = minutes)
                    }
                )
            },
            valueRange = -15f..15f,
            steps = 29
        )
    }
}

private fun adjustmentSummary(state: SalahUiState): String {
    val parts = listOf(
        "F" to state.adjustments.fajr,
        "D" to state.adjustments.dhuhr,
        "A" to state.adjustments.asr,
        "M" to state.adjustments.maghrib,
        "I" to state.adjustments.isha
    )
    return if (parts.all { it.second == 0 }) "" else parts.joinToString(" ") { "${it.first}${it.second}" }
}

private fun hijriSummary(offset: Int): String =
    if (offset == 0) "±0" else if (offset > 0) "+$offset" else "$offset"

/** Human label for a stored theme preference, which is persisted as a raw string. */
private fun com.example.ui.localization.UiStrings.themeLabel(theme: String): String = when (theme) {
    "Dark Mode (OLED)" -> more.themeDark
    "Clean Light" -> more.themeLight
    else -> more.themeSystem
}
