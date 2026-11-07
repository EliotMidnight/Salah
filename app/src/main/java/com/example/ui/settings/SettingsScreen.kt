package com.example.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.UserLocation
import com.example.ui.SalahUiState
import com.example.ui.formatCoordinates
import com.example.ui.components.ActionRow
import com.example.ui.components.EmptyState
import com.example.ui.components.ToggleRow
import com.example.ui.components.ChipRow
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.DetailList
import com.example.ui.components.LabeledSlider
import com.example.ui.components.OptionRow
import com.example.data.model.CapitalLocations
import com.example.ui.components.OptionSheet
import com.example.ui.components.RowDivider
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SearchInput
import com.example.ui.components.SectionGroup
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedOptions
import com.example.data.model.PrayerAdjustments
import com.example.ui.localization.AppLanguage
import com.example.ui.system.ExactAlarmAccess
import com.example.ui.system.ExactAlarmAccessBanner
import com.example.ui.system.LocationAccessBanner
import com.example.ui.system.NotificationAccessBanner
import com.example.ui.system.SystemAccessActions
import com.example.ui.system.SystemAccessState
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.prayerName
import com.example.ui.theme.Accent
import com.example.ui.theme.Glass
import com.example.ui.theme.IconSize
import com.example.ui.theme.Space
import com.example.ui.theme.accentFamily
import com.example.ui.theme.layoutMetrics
import androidx.compose.material3.OutlinedTextField

private val THEMES = listOf("System Default", "Dark Mode (OLED)", "Clean Light")

/**
 * The accent options, in sheet order.
 *
 * "Match wallpaper" is only offered where a system palette exists; a device
 * that never stored the choice simply never sees the row.
 */
private val ACCENTS: List<Accent> = Accent.entries.filter { accent ->
    accent != Accent.WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

private val HIJRI_OFFSETS = listOf(-2, -1, 0, 1, 2)
private val PRE_PRAYER_OFFSETS = listOf(5, 10, 15, 20, 30)
/** Height of the scrollable country-capital list inside the location sheet. */
private val CapitalsListMaxHeight = 360.dp

/**
 * One line in the privacy card: a check and a fact.
 */
@Composable
private fun PrivacyFact(text: String) {
    val space = Space.current
    Row(
        modifier = Modifier.padding(vertical = space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(IconSize.sm)
                .clearAndSetSemantics { }
        )
        Spacer(Modifier.width(space.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The colour chip beside an accent option.
 *
 * The row's title is the accessible label; the chip repeats the colour, so it
 * is decorative and cleared from semantics. "Match wallpaper" has no fixed
 * colour of its own - the chip shows the accent currently in force, which is
 * the best available preview of what choosing it does.
 */
@Composable
private fun AccentSwatch(accent: Accent, isDark: Boolean) {
    val color = if (accent == Accent.WALLPAPER) {
        MaterialTheme.colorScheme.primary
    } else {
        accentFamily(accent, dark = isDark).primary
    }
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clearAndSetSemantics { }
    )
}

/** The localized name of a stored accent key. */
private fun accentName(key: String, strings: UiStrings): String = when (Accent.fromKey(key)) {
    Accent.SLATE -> strings.more.accentSlate
    Accent.PLUM -> strings.more.accentPlum
    Accent.TEAL -> strings.more.accentTeal
    Accent.CLAY -> strings.more.accentClay
    Accent.WALLPAPER -> strings.more.accentWallpaper
}

/**
 * Settings.
 */
@Composable
fun SettingsScreen(
    state: SalahUiState,
    onLocationSelect: (UserLocation) -> Unit,
    onMethodSelect: (CalculationMethod) -> Unit,
    onMadhhabSelect: (Madhhab) -> Unit,
    onAdjustmentsChange: (PrayerAdjustments) -> Unit,
    onAdhanToggle: (Boolean) -> Unit,
    onPrePrayerToggle: (Boolean) -> Unit,
    onVibrateOnlyToggle: (Boolean) -> Unit,
    onGlobalSilentToggle: () -> Unit = {},
    onLanguageSelect: (String) -> Unit = {},
    onThemeSelect: (String) -> Unit = {},
    onAccentSelect: (String) -> Unit = {},
    onReduceTransparencyChange: (Boolean) -> Unit = {},
    onTimeFormatToggle: (Boolean) -> Unit,
    onHijriAdjustmentChange: (Int) -> Unit = {},
    onPrePrayerOffsetChange: (Int) -> Unit = {},
    onCustomLocationSave: (String, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    onReschedulePrayers: () -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    onFetchLocation: () -> Unit = {},
    systemAccess: SystemAccessState = SystemAccessState(),
    systemAccessActions: SystemAccessActions = SystemAccessActions(),
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    // The sheets render accent chips for the mode on screen. The page is pure
    // white or pure black, so its luminance answers the question exactly.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // One enum instead of eighteen booleans, so only one sheet can ever be open and
    // its open/close is a single state change.
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var cityQuery by rememberSaveable { mutableStateOf("") }

    // No header. The bottom navigation already says "Settings", so the top bar
    // repeated it and spent ~64dp saying so.
    ScreenScaffold(
        title = null,
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
                    value = when (state.appTheme) {
                        "System Default" -> strings.more.themeSystem
                        "Dark Mode (OLED)" -> strings.more.themeDark
                        else -> strings.more.themeLight
                    },
                    onClick = { sheet = Sheet.THEME }
                )
                RowDivider()
                ActionRow(
                    title = strings.more.accentLabel,
                    value = accentName(state.accent, strings),
                    onClick = { sheet = Sheet.ACCENT }
                )
                if (Glass.capable) {
                    RowDivider()
                    ToggleRow(
                        title = strings.more.reduceTransparencyLabel,
                        checked = state.reduceTransparency,
                        onCheckedChange = onReduceTransparencyChange,
                        testTag = "reduceTransparency"
                    )
                }
                RowDivider()
                ActionRow(
                    title = strings.more.timeFormat24hLabel,
                    value = if (state.timeFormat24h) "24" else "12",
                    onClick = { onTimeFormatToggle(!state.timeFormat24h) }
                )
            }

            // -- Location & calculation -----------------------------------
            SectionHeader(strings.more.sectionLocationAndCalculation)
            SectionGroup {
                ActionRow(
                    title = strings.locationLabel,
                    subtitle = state.location.name,
                    value = formatCoordinates(
                        state.location.latitude,
                        state.location.longitude,
                        2
                    ),
                    onClick = { sheet = Sheet.LOCATION }
                )
                LocationAccessBanner(
                    status = state.locationStatus,
                    systemAccess = systemAccess,
                    actions = systemAccessActions
                )
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
                    value = adjustmentSummary(state, strings),
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
                val alertsEnabled = state.adhanNotificationEnabled ||
                    state.prePrayerAlertEnabled
                if (alertsEnabled && systemAccess.notificationIssue != null) {
                    NotificationAccessBanner(
                        systemAccess = systemAccess,
                        alertsEnabled = alertsEnabled,
                        actions = systemAccessActions
                    )
                    Spacer(Modifier.height(space.sm))
                }
                if (alertsEnabled && systemAccess.exactAlarms == ExactAlarmAccess.DENIED) {
                    ExactAlarmAccessBanner(
                        systemAccess = systemAccess,
                        alertsEnabled = alertsEnabled,
                        actions = systemAccessActions
                    )
                    Spacer(Modifier.height(space.sm))
                }
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
                    testTag = "setting_pre_prayer",
                    role = Role.Button
                )
                RowDivider()
                ToggleRow(
                    title = strings.more.vibrateOnlyLabel,
                    checked = state.vibrateOnly,
                    onCheckedChange = onVibrateOnlyToggle,
                    testTag = "setting_vibrate_only"
                )
                RowDivider()
                ActionRow(
                    title = strings.system.testNotification,
                    onClick = systemAccessActions.sendTestNotification,
                    enabled = systemAccess.notificationsReady,
                    testTag = "setting_test_notification"
                )
            }

            // -- Diagnostics ----------------------------------------------
            SectionHeader(strings.more.sectionAbout)
            SectionGroup {
                ActionRow(
                    title = strings.transparentCalculationSource,
                    subtitle = strings.more.computedOnDevice,
                    onClick = { sheet = Sheet.CALCULATION }
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
                    showChevron = false,
                    enabled = false,
                    onClick = {}
                )
                RowDivider()
                Column(modifier = Modifier.padding(horizontal = space.md)) {
                    Text(
                        text = strings.privacyPhilosophyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(space.xs))
                    Text(
                        text = strings.more.privacyNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(space.md))
                    PrivacyFact(strings.more.privacyOffline)
                    PrivacyFact(strings.more.privacyNoAds)
                    PrivacyFact(strings.more.privacyNoAnalytics)
                    PrivacyFact(strings.more.privacyNoAccount)
                    PrivacyFact(strings.more.privacyFree)
                }
            }

            // Destructive and rare, so it sits outside the About group and
            // carries the error colour instead of reading as one more detail.
            Spacer(Modifier.height(space.lg))
            SectionGroup {
                ActionRow(
                    title = strings.more.resetAllLabel,
                    onClick = { confirmReset = true },
                    showChevron = false,
                    destructive = true
                )
            }
        }
    }

    // ---- Sheets ---------------------------------------------------------

    when (sheet) {
        Sheet.LANGUAGE -> OptionSheet(
            title = strings.more.chooseLanguage,
            onDismiss = { sheet = null }
        ) {
            AppLanguage.entries.forEach { language ->
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
                    title = when (theme) {
                        "System Default" -> strings.more.themeSystem
                        "Dark Mode (OLED)" -> strings.more.themeDark
                        else -> strings.more.themeLight
                    },
                    selected = theme == state.appTheme,
                    onClick = {
                        onThemeSelect(theme)
                        sheet = null
                    }
                )
            }
        }

        Sheet.ACCENT -> OptionSheet(
            title = strings.more.accentLabel,
            onDismiss = { sheet = null }
        ) {
            ACCENTS.forEach { accent ->
                OptionRow(
                    title = accentName(accent.key, strings),
                    selected = accent.key == state.accent,
                    trailing = { AccentSwatch(accent, isDark) },
                    onClick = {
                        onAccentSelect(accent.key)
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

            RowDivider()

            SectionHeader(strings.more.allCountriesTitle)
            val capitals = remember(cityQuery) { CapitalLocations.search(cityQuery) }
            if (capitals.isEmpty()) {
                EmptyState(title = strings.more.noSearchResults)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = CapitalsListMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(space.xs)
                ) {
                    items(capitals, key = { it.country }) { entry ->
                        val location = entry.toLocation()
                        OptionRow(
                            title = entry.capital,
                            description = entry.country,
                            selected = location.name == state.location.name &&
                                location.country == state.location.country,
                            onClick = {
                                onLocationSelect(location)
                                sheet = null
                            }
                        )
                    }
                }
            }

            RowDivider()

            var customName by rememberSaveable { mutableStateOf("") }
            var customLat by rememberSaveable { mutableStateOf("") }
            var customLng by rememberSaveable { mutableStateOf("") }
            var customError by rememberSaveable { mutableStateOf<String?>(null) }

            SectionHeader(strings.more.customLocation)
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it; customError = null },
                label = { Text(strings.more.nameField) },
                singleLine = true,
                isError = customError == strings.more.nameRequired,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                OutlinedTextField(
                    value = customLat,
                    onValueChange = { customLat = it; customError = null },
                    label = { Text(strings.more.latitudeField) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customLng,
                    onValueChange = { customLng = it; customError = null },
                    label = { Text(strings.more.longitudeField) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (customError != null) {
                Spacer(Modifier.height(space.xs))
                Text(
                    text = customError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(space.sm))
            OutlinedButton(
                onClick = {
                    val lat = customLat.trim().toDoubleOrNull()
                    val lng = customLng.trim().toDoubleOrNull()
                    val name = customName.trim()
                    when {
                        name.isEmpty() ->
                            customError = strings.more.nameRequired
                        lat == null || lng == null ||
                            lat !in -90.0..90.0 || lng !in -180.0..180.0 ->
                            customError = strings.more.invalidCoordinates
                        else -> {
                            onCustomLocationSave(name, lat, lng, 0.0)
                            customName = ""; customLat = ""; customLng = ""
                            customError = null
                            sheet = null
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Text(strings.more.actionSave, style = MaterialTheme.typography.labelLarge)
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
                    onClick = { onAdjustmentsChange(PrayerAdjustments()) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Text(strings.more.actionReset, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { sheet = null },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
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

        Sheet.CALCULATION -> OptionSheet(
            title = strings.transparentCalculationSource,
            subtitle = strings.more.computedOnDevice,
            onDismiss = { sheet = null }
        ) {
            DetailList(
                items = listOf(
                    strings.more.methodology to state.method.title,
                    strings.more.madhhabLabelShort to state.madhhab.title,
                    strings.locationLabel to state.location.name,
                    strings.more.appliedAdjustments to appliedAdjustments(state.adjustments)
                        .joinToString(", ") { (prayer, minutes) ->
                            "${strings.prayerName(prayer)} ${signed(minutes)}"
                        }
                )
            )
            Spacer(Modifier.height(space.md))
            OutlinedButton(
                onClick = {
                    onReschedulePrayers()
                    sheet = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Text(strings.more.reschedulePrayers, style = MaterialTheme.typography.labelLarge)
            }
        }

        Sheet.STORAGE -> OptionSheet(
            title = strings.more.storageLabel,
            onDismiss = { sheet = null }
        ) {
            DetailList(
                items = listOf(
                    strings.more.prayerLogLabel to strings.more.prayerLogStoredLocally
                )
            )
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
            onDismiss = { confirmReset = false },
            onConfirm = {
                onResetAllSettings()
                confirmReset = false
            }
        )
    }
}

internal fun adjustmentSummary(state: SalahUiState, strings: UiStrings): String {
    val adj = state.adjustments
    val nonZero = listOf(
        Prayer.FAJR to adj.fajr,
        Prayer.SUNRISE to adj.sunrise,
        Prayer.DHUHR to adj.dhuhr,
        Prayer.ASR to adj.asr,
        Prayer.MAGHRIB to adj.maghrib,
        Prayer.ISHA to adj.isha
    ).filter { it.second != 0 }
    // The zero state is a sign, not an English word: it reads the same in every
    // language and matches the Hijri calibration row's "±0".
    return if (nonZero.isEmpty()) {
        "±0"
    } else {
        nonZero.joinToString(", ") { (p, m) -> "${strings.prayerName(p)} ${signed(m)}" }
    }
}

/**
 * Per-prayer minute adjustments as a list of (prayer, minutes) pairs.
 */
fun appliedAdjustments(adjustments: PrayerAdjustments): List<Pair<Prayer, Int>> {
    return Prayer.entries.map { it to adjustments.forPrayer(it) }.filter { it.second != 0 }
}

/**
 * Per-prayer minute adjustment sliders, one row per prayer.
 */
@Composable
private fun AdjustmentSliders(
    adjustments: PrayerAdjustments,
    onChange: (PrayerAdjustments) -> Unit
) {
    val strings = LocalStrings.current
    val rows = listOf(
        Prayer.FAJR to adjustments.fajr,
        Prayer.SUNRISE to adjustments.sunrise,
        Prayer.DHUHR to adjustments.dhuhr,
        Prayer.ASR to adjustments.asr,
        Prayer.MAGHRIB to adjustments.maghrib,
        Prayer.ISHA to adjustments.isha
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (prayer, minutes) ->
            LabeledSlider(
                label = strings.prayerName(prayer),
                valueText = "${signed(minutes)} ${strings.more.minutesShort}",
                value = minutes.toFloat(),
                onValueChange = {
                    val updated = when (prayer) {
                        Prayer.FAJR -> adjustments.copy(fajr = it.toInt())
                        Prayer.SUNRISE -> adjustments.copy(sunrise = it.toInt())
                        Prayer.DHUHR -> adjustments.copy(dhuhr = it.toInt())
                        Prayer.ASR -> adjustments.copy(asr = it.toInt())
                        Prayer.MAGHRIB -> adjustments.copy(maghrib = it.toInt())
                        Prayer.ISHA -> adjustments.copy(isha = it.toInt())
                    }
                    onChange(updated)
                },
                valueRange = -30f..30f
            )
            if (index < rows.lastIndex) RowDivider()
        }
    }
}

private fun hijriSummary(offset: Int): String = when {
    offset == 0 -> "±0"
    offset > 0 -> "+$offset"
    else -> "$offset"
}

private fun signed(minutes: Int): String = if (minutes >= 0) "+$minutes" else "$minutes"

/** The sheet that is currently open, or null. */
private enum class Sheet {
    LANGUAGE, THEME, ACCENT, LOCATION, METHOD, MADHHAB, ADJUSTMENTS, HIJRI, PRE_PRAYER,
    CALCULATION, STORAGE, COMPASS
}
