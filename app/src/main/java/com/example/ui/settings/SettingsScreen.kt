package com.example.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.ui.SalahUiState
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.expressiveCollapse
import com.example.ui.theme.expressiveExpand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SalahUiState,
    onLocationSelect: (UserLocation) -> Unit,
    onFetchLocation: () -> Unit = {},
    onMethodSelect: (CalculationMethod) -> Unit,
    onMadhhabSelect: (Madhhab) -> Unit,
    onAdjustmentsChange: (PrayerAdjustments) -> Unit,
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
    onTimeFormatToggle: (Boolean) -> Unit = {},
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
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val strings = LocalStrings.current

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showRiwayahSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showMadhhabSheet by remember { mutableStateOf(false) }
    var showAdjustmentsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAdhanSoundSheet by remember { mutableStateOf(false) }
    var showScriptSheet by remember { mutableStateOf(false) }
    var showReciterSheet by remember { mutableStateOf(false) }
    var showHijriSheet by remember { mutableStateOf(false) }
    var showPrePrayerSheet by remember { mutableStateOf(false) }
    var showPrayerAlertMatrixSheet by remember { mutableStateOf(false) }
    var showAdhanTesterSheet by remember { mutableStateOf(false) }
    var showCacheManagerSheet by remember { mutableStateOf(false) }
    var showQuranStorageSheet by remember { mutableStateOf(false) }
    var showCompassDiagnosticsSheet by remember { mutableStateOf(false) }
    var showTranslationSheet by remember { mutableStateOf(false) }
    var showCustomLocationDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (onBack != null) {
                item {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            }

            // SECTION 1: GENERAL & LOCALIZATION
            item {
                SectionHeader(strings.sectionGeneral)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.Translate,
                            title = strings.languageLabel,
                            subtitle = state.language,
                            onClick = { showLanguageSheet = true },
                            tag = "setting_language"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Palette,
                            title = strings.appThemeLabel,
                            subtitle = state.appTheme,
                            onClick = { showThemeSheet = true },
                            tag = "setting_theme"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsToggleRow(
                            icon = Icons.Default.AccessTime,
                            title = strings.timeFormatLabel,
                            subtitle = if (state.timeFormat24h) "14:30 (24-Hour format)" else "2:30 PM (12-Hour format)",
                            checked = state.timeFormat24h,
                            onCheckedChange = onTimeFormatToggle,
                            tag = "setting_time_format"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 2: PRAYER TIMES & ASTRONOMY
            item {
                SectionHeader(strings.sectionPrayerCalc)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.LocationOn,
                            title = strings.locationLabel,
                            subtitle = "${state.location.name}, ${state.location.country} (${String.format("%.2f", state.location.latitude)}°, ${String.format("%.2f", state.location.longitude)}°)",
                            onClick = { showLocationSheet = true },
                            tag = "setting_location"
                        )

                        val locStatus = state.locationStatusMessage
                        if (locStatus != null || state.isLocating) {
                            Text(
                                text = locStatus ?: "Locating…",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isLocating) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp, bottom = 8.dp)
                                    .testTag("location_status")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Tune,
                            title = strings.methodLabel,
                            subtitle = state.method.title,
                            onClick = { showMethodSheet = true },
                            tag = "setting_method"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Tune,
                            title = strings.madhhabLabel,
                            subtitle = "${state.madhhab.title} (Factor ${state.madhhab.shadowFactor}x)",
                            onClick = { showMadhhabSheet = true },
                            tag = "setting_madhhab"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Edit,
                            title = strings.adjustmentsLabel,
                            subtitle = "F:${state.adjustments.fajr}m, D:${state.adjustments.dhuhr}m, A:${state.adjustments.asr}m, M:${state.adjustments.maghrib}m, I:${state.adjustments.isha}m",
                            onClick = { showAdjustmentsSheet = true },
                            tag = "setting_adjustments"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.CalendarMonth,
                            title = strings.hijriCalibrationLabel,
                            subtitle = if (state.hijriAdjustment == 0) "Standard (0 days offset)" else "${if (state.hijriAdjustment > 0) "+${state.hijriAdjustment}" else "${state.hijriAdjustment}"} days moon offset",
                            onClick = { showHijriSheet = true },
                            tag = "setting_hijri_cal"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 3: NOTIFICATIONS & ADHAN
            item {
                SectionHeader(strings.sectionAudioAlerts)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Master Silent Mode
                        SettingsToggleRow(
                            icon = if (state.isGlobalSilentMode) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            title = strings.globalSilentLabel,
                            subtitle = if (state.isGlobalSilentMode) "All adhans and chimes are currently silenced" else "Silence all prayer alarms and adhan calls",
                            checked = state.isGlobalSilentMode,
                            onCheckedChange = { onGlobalSilentToggle() },
                            tag = "setting_master_silent_toggle"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Auto-Silent at Prayer (Masjid Mode)
                        SettingsToggleRow(
                            icon = Icons.AutoMirrored.Filled.VolumeMute,
                            title = strings.autoMasjidModeLabel,
                            subtitle = "Automatically mutes device during prayer for ${state.autoSilentDurationMinutes} min",
                            checked = state.autoSilentDuringPrayer,
                            onCheckedChange = { onAutoSilentDuringPrayerToggle() },
                            tag = "setting_auto_silent_toggle"
                        )

                        AnimatedVisibility(
                            visible = state.autoSilentDuringPrayer,
                            enter = expressiveExpand(),
                            exit = expressiveCollapse()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.autoMasjidDurationLabel}:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listOf(15, 20, 30, 45).forEach { min ->
                                    val isSelected = state.autoSilentDurationMinutes == min
                                    Surface(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clickable { onAutoSilentDurationChange(min) }
                                            .semantics { selected = isSelected }
                                            .testTag("auto_silent_duration_${min}m"),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text(
                                            text = "${min}m",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = strings.adhanCallLabel,
                            subtitle = "Plays alert right when prayer enters",
                            checked = state.adhanNotificationEnabled,
                            onCheckedChange = onAdhanToggle,
                            tag = "setting_adhan_toggle"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Notifications,
                            title = strings.perPrayerModesLabel,
                            subtitle = "Fajr, Dhuhr, Asr, Maghrib, Isha customized sounds",
                            onClick = { showPrayerAlertMatrixSheet = true },
                            tag = "setting_prayer_alerts_matrix"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.AccessTime,
                            title = strings.prePrayerReminderLabel,
                            subtitle = if (state.prePrayerAlertEnabled) "${state.prePrayerOffsetMinutes} minutes before Salah" else "Disabled (Tap to configure)",
                            onClick = { showPrePrayerSheet = true },
                            tag = "setting_pre_prayer_sheet"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = strings.adhanSoundLabel,
                            subtitle = state.adhanSound,
                            onClick = { showAdhanSoundSheet = true },
                            tag = "setting_adhan_sound"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Speed,
                            title = strings.adhanVolumeLabel,
                            subtitle = "Volume: ${(state.adhanVolume * 100).toInt()}% · Tap to preview",
                            onClick = { showAdhanTesterSheet = true },
                            tag = "setting_adhan_tester"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsToggleRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = strings.vibrationOnlyLabel,
                            subtitle = "Silences audio playback and uses gentle haptics",
                            checked = state.vibrateOnly,
                            onCheckedChange = onVibrateOnlyToggle,
                            tag = "setting_vibrate_only"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 4: THE NOBLE QURAN
            item {
                SectionHeader(strings.sectionQuran)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.AutoStories,
                            title = strings.riwayahLabel,
                            subtitle = state.riwayah,
                            onClick = { showRiwayahSheet = true },
                            tag = "setting_riwayah"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.FontDownload,
                            title = strings.scriptStyleLabel,
                            subtitle = state.quranScript,
                            onClick = { showScriptSheet = true },
                            tag = "setting_script"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.RecordVoiceOver,
                            title = strings.reciterLabel,
                            subtitle = state.reciter,
                            onClick = { showReciterSheet = true },
                            tag = "setting_reciter"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Translate,
                            title = strings.translationLabel,
                            subtitle = state.translationEdition,
                            onClick = { showTranslationSheet = true },
                            tag = "setting_translation"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Text(
                            text = strings.arabicTextSizeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(state.quranFontScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                            }
                            Slider(
                                value = state.quranFontScale,
                                onValueChange = onFontScaleChange,
                                valueRange = 0.8f..1.5f,
                                steps = 7,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 5: OFFLINE ENGINE, STORAGE & SYSTEM TRUST
            item {
                SectionHeader(strings.sectionSystemDiagnostics)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.AccessTime,
                            title = "Offline Ephemeris Cache Manager",
                            subtitle = "365 days computed on-device · Tap to recompute & export",
                            onClick = { showCacheManagerSheet = true },
                            tag = "setting_cache_manager"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Storage,
                            title = "Quran Database & Offline Assets",
                            subtitle = "114 Surahs · 6,236 Ayahs in SQLite · Tap to manage cache",
                            onClick = { showQuranStorageSheet = true },
                            tag = "setting_quran_storage"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Explore,
                            title = strings.compassDiagnosticsLabel,
                            subtitle = "${state.compassAccuracy} · Kaaba: ${state.distanceToKaabaKm} km · Tap to calibrate",
                            onClick = { showCompassDiagnosticsSheet = true },
                            tag = "setting_compass_diag"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Refresh,
                            title = strings.networkSyncLabel,
                            subtitle = if (state.isOnline) "Online · Sync verified (${state.lastChecked})" else "Offline Mode · Fully functional on-device",
                            onClick = onRefreshClick,
                            tag = "setting_network_sync"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.RestartAlt,
                            title = strings.resetDefaultsLabel,
                            subtitle = "Restore default calculation, audio & display settings",
                            onClick = { showResetConfirmDialog = true },
                            tag = "setting_reset_defaults"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = strings.privacyPhilosophyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SALAH is your spiritual companion, designed with pure craft and intention.\n\n• Zero accounts, logins, or cloud tracking\n• Zero ads, zero commercial affiliates\n• Zero analytics SDKs or remote telemetry\n• 100% offline-first on-device calculations\n• All bookmarks and logs are stored privately in your local SQLite database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // 1. Language Sheet
    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = strings.languageLabel, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                AppLanguage.values().forEach { appLang ->
                    SelectionSheetItem(
                        title = "${appLang.nativeName} (${appLang.englishName})",
                        description = appLang.description,
                        isSelected = state.language.equals(appLang.nativeName, ignoreCase = true) ||
                                     state.language.equals(appLang.englishName, ignoreCase = true) ||
                                     state.language.equals(appLang.code, ignoreCase = true),
                        onClick = {
                            onLanguageSelect(appLang.nativeName)
                            showLanguageSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 2. Riwayah Sheet
    if (showRiwayahSheet) {
        ModalBottomSheet(onDismissRequest = { showRiwayahSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Select Riwāyah (Recitation Tradition)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("Hafs 'an 'Asim", "Standard worldwide prevalent recitation"),
                    Pair("Warsh 'an Nafi'", "Prevalent in North & West Africa (Morocco, Algeria)"),
                    Pair("Qalun 'an Nafi'", "Prevalent in Libya, Tunisia, and Mauritania"),
                    Pair("Al-Duri 'an Abi 'Amr", "Prevalent in Sudan, Chad, and East Africa")
                ).forEach { (rw, desc) ->
                    SelectionSheetItem(
                        title = rw,
                        description = desc,
                        isSelected = state.riwayah == rw,
                        onClick = {
                            onRiwayahSelect(rw)
                            showRiwayahSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 3. Theme Sheet
    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Select App Theme", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("System Default", "Follows device system settings automatically"),
                    Pair("Dark Mode (OLED)", "Deep cosmic dark theme optimized for night and battery savings"),
                    Pair("Clean Light", "Bright, high-contrast crisp day aesthetic")
                ).forEach { (th, desc) ->
                    SelectionSheetItem(
                        title = th,
                        description = desc,
                        isSelected = state.appTheme == th,
                        onClick = {
                            onThemeSelect(th)
                            showThemeSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 4. Location Sheet with Search & Custom Coordinates
    if (showLocationSheet) {
        var citySearchQuery by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Select City Location", style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                onFetchLocation()
                                showLocationSheet = false
                            },
                            modifier = Modifier.testTag("btn_detect_gps_location")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use GPS")
                        }
                        TextButton(onClick = {
                            showLocationSheet = false
                            showCustomLocationDialog = true
                        }) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = citySearchQuery,
                    onValueChange = { citySearchQuery = it },
                    placeholder = { Text("Search city or country...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredCities = UserLocation.POPULAR_CITIES.filter {
                    it.name.contains(citySearchQuery, ignoreCase = true) ||
                            it.country.contains(citySearchQuery, ignoreCase = true)
                }

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filteredCities) { city ->
                        SelectionSheetItem(
                            title = "${city.name}, ${city.country}",
                            description = "Lat: ${String.format("%.3f", city.latitude)}°, Lng: ${String.format("%.3f", city.longitude)}°",
                            isSelected = city.name == state.location.name,
                            onClick = {
                                onLocationSelect(city)
                                showLocationSheet = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 5. Method Sheet
    if (showMethodSheet) {
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Select Calculation Method", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(CalculationMethod.entries) { method ->
                        SelectionSheetItem(
                            title = method.title,
                            description = "Fajr: ${method.fajrAngle}° | Isha: ${method.ishaAngle}°",
                            isSelected = state.method == method,
                            onClick = {
                                onMethodSelect(method)
                                showMethodSheet = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 6. Madhhab Sheet
    if (showMadhhabSheet) {
        ModalBottomSheet(onDismissRequest = { showMadhhabSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Asr Juristic Method (Madhhab)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                Madhhab.entries.forEach { m ->
                    SelectionSheetItem(
                        title = m.title,
                        description = "Shadow multiplier factor: ${m.shadowFactor}x",
                        isSelected = state.madhhab == m,
                        onClick = {
                            onMadhhabSelect(m)
                            showMadhhabSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 7. Adjustments Sheet
    if (showAdjustmentsSheet) {
        ModalBottomSheet(onDismissRequest = { showAdjustmentsSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Manual Minute Adjustments", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onAdjustmentsChange(PrayerAdjustments()) }) {
                        Text("Reset All")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                AdjustmentSlider(label = "Fajr", value = state.adjustments.fajr) {
                    onAdjustmentsChange(state.adjustments.copy(fajr = it))
                }
                AdjustmentSlider(label = "Dhuhr", value = state.adjustments.dhuhr) {
                    onAdjustmentsChange(state.adjustments.copy(dhuhr = it))
                }
                AdjustmentSlider(label = "Asr", value = state.adjustments.asr) {
                    onAdjustmentsChange(state.adjustments.copy(asr = it))
                }
                AdjustmentSlider(label = "Maghrib", value = state.adjustments.maghrib) {
                    onAdjustmentsChange(state.adjustments.copy(maghrib = it))
                }
                AdjustmentSlider(label = "Isha", value = state.adjustments.isha) {
                    onAdjustmentsChange(state.adjustments.copy(isha = it))
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = { showAdjustmentsSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 8. Hijri Calibration Sheet
    if (showHijriSheet) {
        ModalBottomSheet(onDismissRequest = { showHijriSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Hijri Date Calibration", style = MaterialTheme.typography.titleLarge)
                Text(text = "Align with local moonsighting authority declaration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                listOf(-2, -1, 0, 1, 2).forEach { offset ->
                    SelectionSheetItem(
                        title = when (offset) {
                            0 -> "Exact Astronomical (0 days offset)"
                            1 -> "+1 Day ahead"
                            2 -> "+2 Days ahead"
                            -1 -> "-1 Day behind"
                            else -> "-2 Days behind"
                        },
                        description = if (offset == 0) "Standard calculated lunar calendar" else "Calibrated manual adjustment",
                        isSelected = state.hijriAdjustment == offset,
                        onClick = {
                            onHijriAdjustmentChange(offset)
                            showHijriSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 9. Adhan Sound Sheet with Live Preview
    if (showAdhanSoundSheet) {
        ModalBottomSheet(onDismissRequest = {
            onStopAudioPreview()
            showAdhanSoundSheet = false
        }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Adhan Recitation Sound", style = MaterialTheme.typography.titleLarge)
                Text(text = "Tap the play icon to audition each melody offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("Makkah Al-Mukarramah", "Majestic Haram Makkah style adhan"),
                    Pair("Madinah Al-Munawwarah", "Serene Prophet's Mosque adhan"),
                    Pair("Al-Aqsa Jerusalem", "Historic Jerusalem Al-Quds adhan style"),
                    Pair("Moroccan Traditional", "Maghrebi Andalusian melodic adhan tradition"),
                    Pair("Gentle Bell Chime", "Subtle acoustic chime for quiet environments")
                ).forEach { (sound, desc) ->
                    SelectionSheetItemWithPreview(
                        title = sound,
                        description = desc,
                        isSelected = state.adhanSound == sound,
                        isPlaying = state.audioPreviewPlaying == sound,
                        onPreviewClick = {
                            if (state.audioPreviewPlaying == sound) onStopAudioPreview()
                            else onPlayAudioPreview(sound)
                        },
                        onClick = {
                            onAdhanSoundSelect(sound)
                            showAdhanSoundSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 10. Script Style Sheet
    if (showScriptSheet) {
        ModalBottomSheet(onDismissRequest = { showScriptSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Quran Arabic Script Style", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("Uthmani (Madani)", "King Fahd Complex Madinah standard typography"),
                    Pair("Maghrebi (Moroccan)", "Traditional North African handwritten calligraphic style"),
                    Pair("Indo-Pak (Naskh)", "Subcontinent high-contrast diacritics script")
                ).forEach { (script, desc) ->
                    SelectionSheetItem(
                        title = script,
                        description = desc,
                        isSelected = state.quranScript == script,
                        onClick = {
                            onQuranScriptSelect(script)
                            showScriptSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 11. Reciter Sheet with Live Audition
    if (showReciterSheet) {
        ModalBottomSheet(onDismissRequest = {
            onStopAudioPreview()
            showReciterSheet = false
        }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Quran Audio Reciter", style = MaterialTheme.typography.titleLarge)
                Text(text = "Tap play icon to audition tone sample offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("Mishary Rashid Alafasy", "Clear, melodic modern recitation (Kuwait)"),
                    Pair("Abdul Basit Abdul Samad", "Legendary Mujawwad & Murattal master (Egypt)"),
                    Pair("Mahmoud Khalil Al-Husary", "Classic foundational Tartil tajweed (Egypt)"),
                    Pair("Saad Al-Ghamdi", "Warm, measured meditative pace (Saudi Arabia)")
                ).forEach { (rec, desc) ->
                    SelectionSheetItemWithPreview(
                        title = rec,
                        description = desc,
                        isSelected = state.reciter == rec,
                        isPlaying = state.audioPreviewPlaying == rec,
                        onPreviewClick = {
                            if (state.audioPreviewPlaying == rec) onStopAudioPreview()
                            else onPlayAudioPreview(rec)
                        },
                        onClick = {
                            onReciterSelect(rec)
                            showReciterSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 12. Pre-Prayer Timing Sheet
    if (showPrePrayerSheet) {
        ModalBottomSheet(onDismissRequest = { showPrePrayerSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Pre-Prayer Reminder Timing", style = MaterialTheme.typography.titleLarge)
                Text(text = "Set how early you want a gentle reminder before the Adhan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                listOf(
                    Pair(5, "5 minutes before (quick heads-up)"),
                    Pair(10, "10 minutes before (recommended for wudu)"),
                    Pair(15, "15 minutes before (sunnah prayers)"),
                    Pair(20, "20 minutes before (preparation for masjid)"),
                    Pair(30, "30 minutes before (extended alert)")
                ).forEach { (mins, desc) ->
                    SelectionSheetItem(
                        title = "$mins Minutes Before",
                        description = desc,
                        isSelected = state.prePrayerAlertEnabled && state.prePrayerOffsetMinutes == mins,
                        onClick = {
                            onPrePrayerToggle(true)
                            onPrePrayerOffsetChange(mins)
                            showPrePrayerSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onPrePrayerToggle(false)
                        showPrePrayerSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Pre-Prayer Reminder")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 13. Per-Prayer Alert Matrix Sheet
    if (showPrayerAlertMatrixSheet) {
        ModalBottomSheet(onDismissRequest = { showPrayerAlertMatrixSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Individual Prayer Notifications", style = MaterialTheme.typography.titleLarge)
                Text(text = "Customize sound or silent mode for each prayer individually", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                val alertOptions = listOf("Full Adhan", "Takbeer Only", "Gentle Chime", "Vibrate Only", "Silent")
                Prayer.entries.forEach { prayer ->
                    val currentMode = state.prayerAlertModes[prayer] ?: "Full Adhan"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prayer.englishName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = currentMode, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                if (currentMode != "Silent" && currentMode != "Vibrate Only") {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (state.audioPreviewPlaying == currentMode) onStopAudioPreview()
                                            else onPlayAudioPreview(currentMode)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.audioPreviewPlaying == currentMode) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Audition",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            alertOptions.forEach { opt ->
                                val isChosen = currentMode == opt
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clickable { onPrayerAlertModeChange(prayer, opt) }
                                        .semantics { selected = isChosen }
                                        .testTag("prayer_alert_option_${prayer.name}_${opt.replace(" ", "_")}"),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (opt == "Vibrate Only") "Vibrate" else if (opt == "Gentle Chime") "Chime" else if (opt == "Takbeer Only") "Takbeer" else opt,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 14. Adhan Volume & Tester Sheet
    if (showAdhanTesterSheet) {
        ModalBottomSheet(onDismissRequest = {
            onStopAudioPreview()
            showAdhanTesterSheet = false
        }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Adhan Volume & Audio Tester", style = MaterialTheme.typography.titleLarge)
                Text(text = "Adjust the alert loudness and preview the synthesizer tone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Adhan Audio Level", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "${(state.adhanVolume * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Slider(
                    value = state.adhanVolume,
                    onValueChange = onAdhanVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Preview Alert Sounds", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Full Adhan", "Takbeer Only", "Gentle Chime").forEach { mode ->
                        val isPlayingThis = state.audioPreviewPlaying == mode || (mode == "Full Adhan" && state.audioPreviewPlaying == state.adhanSound)
                        Button(
                            onClick = {
                                if (isPlayingThis) onStopAudioPreview()
                                else onPlayAudioPreview(mode)
                            },
                            modifier = Modifier.weight(1f).testTag("audition_mode_${mode.replace(" ", "_")}"),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = if (isPlayingThis) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                     else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(
                                imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (mode == "Full Adhan") "Adhan" else if (mode == "Takbeer Only") "Takbeer" else "Chime",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (state.audioPreviewPlaying != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStopAudioPreview,
                        modifier = Modifier.fillMaxWidth().testTag("stop_adhan_tester_audio"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Silence Active Audio")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 15. Cache & Ephemeris Manager Sheet
    if (showCacheManagerSheet) {
        ModalBottomSheet(onDismissRequest = { showCacheManagerSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Offline Ephemeris Cache Manager", style = MaterialTheme.typography.titleLarge)
                Text(text = "On-device solar algorithms calculate 365 days of prayer schedules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatusRow(label = "Storage Engine", value = "Room SQLite (Encrypted)", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Calculation Horizons", value = "Full 365 Days Cached", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Current Calculation Method", value = state.method.title, isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Last Cache Verification", value = state.lastChecked, isGood = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onRecomputeEphemerisCache()
                        Toast.makeText(context, "365-day solar ephemeris recomputed successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recompute & Verify 365-Day Schedule")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val schedule = buildString {
                            appendLine("✦ SALAH PRAYER SCHEDULE (${state.location.name}) ✦")
                            val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                            state.todayPrayerTimes?.prayers?.forEach { p ->
                                appendLine("${p.prayer.englishName}: ${p.time.format(timeFormatter)}")
                            }
                            state.hijriDate?.let { h ->
                                appendLine("Hijri: ${h.day} ${h.monthNameEn} ${h.year} AH")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(schedule))
                        Toast.makeText(context, "Today's schedule copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Today's Schedule to Clipboard")
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // 16. Quran Storage Manager Sheet
    if (showQuranStorageSheet) {
        ModalBottomSheet(onDismissRequest = { showQuranStorageSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Quran Database & Storage Assets", style = MaterialTheme.typography.titleLarge)
                Text(text = "Entire text of the Holy Quran is bundled and queryable 100% offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatusRow(label = "Surahs Bundled", value = "114 Surahs (Complete)", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Ayahs in SQLite", value = "6,236 Verses", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Active Script Style", value = state.quranScript, isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Bookmarks Saved", value = "${state.bookmarks.size} Bookmarks", isGood = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onClearAudioCache()
                        Toast.makeText(context, "Audio synthesis cache cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Audio Cache (Release Memory)")
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // 17. Compass Diagnostics Sheet
    if (showCompassDiagnosticsSheet) {
        ModalBottomSheet(onDismissRequest = { showCompassDiagnosticsSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Compass Sensors & Diagnostics", style = MaterialTheme.typography.titleLarge)
                Text(text = "Real-time magnetometer and Kaaba geometric coordinates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatusRow(label = "Sensor Accuracy", value = state.compassAccuracy, isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Current Device Heading", value = "${state.compassAzimuth.toInt()}°", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Qibla Bearing from Location", value = "${state.qiblaBearing.toInt()}° North", isGood = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StatusRow(label = "Direct Distance to Makkah", value = "${state.distanceToKaabaKm} km", isGood = true)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Tip: If the heading drifts, wave your device in a figure-8 motion in the air to calibrate magnetic sensors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 18. Translation Edition Sheet
    if (showTranslationSheet) {
        ModalBottomSheet(onDismissRequest = { showTranslationSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = "Quran Translation & Exegesis", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("English (Saheeh International)", "Clear, precise contemporary English"),
                    Pair("English (Yusuf Ali)", "Classic poetic translation with explanatory notes"),
                    Pair("Français (Muhammad Hamidullah)", "Traduction française de référence"),
                    Pair("Bahasa Indonesia (Kemenag)", "Terjemahan resmi Kementerian Agama RI"),
                    Pair("Türkçe (Diyanet İşleri)", "Diyanet İşleri Başkanlığı Kur'an-ı Kerim Meali"),
                    Pair("اردو (مولانا مودودی)", "تفہیم القرآن مع تشریحی حواشی"),
                    Pair("Bahasa Melayu (Basmeih)", "Tafsiran pimpinan ar-Rahman"),
                    Pair("বাংলা (মুহিউদ্দীন খান)", "সহজ ও নির্ভরযোগ্য বাংলা অনুবাদ"),
                    Pair("Русский (Эльмир Кулиев)", "Точный и авторитетный перевод смыслов"),
                    Pair("Deutsch (Frank Bubenheim)", "Präzise und anerkannte Übersetzung"),
                    Pair("Español (Julio Cortes)", "Traducción rigurosa y fiel al texto sagrado"),
                    Pair("Tafsir Al-Jalalayn (Arabic)", "وجيز وميسر لمعاني القرآن الكريم")
                ).forEach { (tr, desc) ->
                    SelectionSheetItem(
                        title = tr,
                        description = desc,
                        isSelected = state.translationEdition == tr,
                        onClick = {
                            onTranslationSelect(tr)
                            showTranslationSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 19. Custom Location Dialog
    if (showCustomLocationDialog) {
        var nameInput by remember { mutableStateOf("") }
        var latInput by remember { mutableStateOf("") }
        var lngInput by remember { mutableStateOf("") }
        var locationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomLocationDialog = false },
            title = { Text("Set Custom GPS Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Enter coordinates to calculate prayer times anywhere on Earth, completely offline.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; locationError = null },
                        label = { Text("City or Place Name") },
                        singleLine = true,
                        isError = locationError != null && nameInput.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it; locationError = null },
                        label = { Text("Latitude (-90 to +90)") },
                        singleLine = true,
                        isError = locationError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it; locationError = null },
                        label = { Text("Longitude (-180 to +180)") },
                        singleLine = true,
                        isError = locationError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (locationError != null) {
                        Text(
                            text = locationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val error = com.example.engine.LocationValidation.validateRaw(nameInput, latInput, lngInput)
                    if (error == null) {
                        val name = nameInput.trim()
                        val lat = latInput.trim().toDouble()
                        val lng = lngInput.trim().toDouble()
                        onCustomLocationSave(name, lat, lng, 0.0)
                        showCustomLocationDialog = false
                        Toast.makeText(context, "Location set to $name", Toast.LENGTH_SHORT).show()
                    } else {
                        locationError = error
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLocationDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 20. Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Factory Defaults?") },
            text = { Text("This will restore default calculation method (Ministry of Endowments Morocco), clear manual minute offsets, and reset all notification preferences.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetAllSettings()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "All preferences reset to defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun SelectionSheetItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
            .padding(vertical = 4.dp)
            .testTag("selection_${title.take(24).lowercase().replace(' ', '_')}"),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionSheetItemWithPreview(
    title: String,
    description: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPreviewClick: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Preview Tone",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(70.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = -15f..15f,
            steps = 29,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (value >= 0) "+$value m" else "$value m",
            modifier = Modifier.width(55.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isGood) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
