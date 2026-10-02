package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.UserLocation
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.components.ActionRow
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusBanner
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
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.alertModeLabel
import com.example.ui.localization.prayerName
import com.example.ui.theme.IconSize
import com.example.ui.theme.Tonal
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import androidx.compose.material3.OutlinedTextField

/** The five alert levels a prayer can be set to, in cycle order. */
private val ALERT_MODES = listOf("Full Adhan", "Takbeer Only", "Gentle Chime", "Vibrate Only", "Silent")

/**
 * Sunrise's options.
 *
 * Sunrise is seeded with "Silent Reminder" (SalahViewModel), which is not one
 * of [ALERT_MODES] - and a full adhan at sunrise is not a sensible offer. These
 * are the levels that make sense for the dawn sign, and they include the seeded
 * value so the row can show the truth.
 */
private val SUNRISE_ALERT_MODES = listOf("Silent Reminder", "Vibrate Only", "Silent")
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
private val RIWAYAHS = listOf("Hafs 'an 'Asim", "Warsh 'an Nafi", "Qalun 'an Nafi", "Al-Duri 'an Abi 'Amir")
private val SCRIPTS = listOf("Uthmani (Madani)", "Maghrebi", "Indo-Pak")
private val THEMES = listOf("System Default", "Dark Mode (OLED)", "Clean Light")
private val HIJRI_OFFSETS = listOf(-2, -1, 0, 1, 2)
private val PRE_PRAYER_OFFSETS = listOf(5, 10, 15, 20, 30)

/** Height of the scrollable country-capital list inside the location sheet. */
private val CapitalsListMaxHeight = 360.dp

/**
 * The play/stop control on an audio option.
 *
 * It was copy-pasted into three sheets, and the copies had drifted: one played a
 * fixed icon, two toggled, and the icon was 20dp in two places and 16dp in the
 * third. One composable, one icon size, and a label that names the thing it
 * previews - six identical "Test sound" buttons in a row were indistinguishable
 * to a screen reader.
 */
@Composable
private fun PlayPreviewButton(
    label: String,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalStrings.current
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.lg)
        )
    }
}

/**
 * One line in the privacy card: a check and a fact.
 *
 * The card states what the app does not do, which is the whole of its privacy
 * stance - there is no server, account or SDK to describe. A check reads as an
 * assurance; the text carries the fact.
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
            tint = Tonal.colors.success,
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
    onTimeFormatToggle: (Boolean) -> Unit,
    onAdhanSoundSelect: (String) -> Unit = {},
    onHijriAdjustmentChange: (Int) -> Unit = {},
    onReciterSelect: (String) -> Unit = {},
    onPrePrayerOffsetChange: (Int) -> Unit = {},
    onAdhanVolumeChange: (Float) -> Unit = {},
    onPrayerAlertModeChange: (Prayer, String) -> Unit = { _, _ -> },
    onPlayAudioPreview: (String) -> Unit = {},
    onStopAudioPreview: () -> Unit = {},
    onCustomLocationSave: (String, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    onReschedulePrayers: () -> Unit = {},
    onResetAllSettings: () -> Unit = {},
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
                    value = strings.themeLabel(state.appTheme),
                    onClick = { sheet = Sheet.THEME }
                )
                RowDivider()
                ActionRow(
                    title = strings.more.timeFormat24hLabel,
                    value = if (state.timeFormat24h) "24h" else "12h",
                    onClick = { onTimeFormatToggle(!state.timeFormat24h) }
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
                    StatusBanner(
                        message = strings.more.loading,
                        action = {
                            OutlinedButton(
                                onClick = onFetchLocation,
                                modifier = Modifier.heightIn(
                                    min = MaterialTheme.layoutMetrics.minTouchTarget
                                )
                            ) {
                                Text(strings.more.useGps, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    )
                }
                state.locationStatusMessage?.let { message ->
                    StatusBanner(message = message)
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
                    testTag = "setting_pre_prayer",
                    // Opens a sheet, so it is a button, not a switch.
                    role = Role.Button
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
                RowDivider()
                ToggleRow(
                    title = strings.autoMasjidModeLabel,
                    checked = state.autoSilentDuringPrayer,
                    onCheckedChange = { onAutoSilentDuringPrayerToggle() },
                    testTag = "setting_auto_silent"
                )
                if (state.autoSilentDuringPrayer) {
                    Column(modifier = Modifier.padding(horizontal = space.md, vertical = space.sm)) {
                        Text(
                            text = strings.more.autoSilenceDurationLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(space.sm))
                        SegmentedOptions(
                            options = AUTO_SILENCE_OPTIONS.map { "$it ${strings.more.minutesShort}" },
                            selectedIndex = AUTO_SILENCE_OPTIONS.indexOf(state.autoSilentDurationMinutes)
                                .coerceAtLeast(0),
                            onSelect = { onAutoSilentDurationChange(AUTO_SILENCE_OPTIONS[it]) }
                        )
                    }
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
                // A fact, not a choice.
                //
                // This opened a twelve-item picker. The corpus bundles exactly one
                // translation - `en_sahihintl.txt`, verified at load by SHA-256 and
                // asserted to be complete - so eleven of the twelve changed nothing
                // at all, and the choice was never persisted either, so the tick
                // moved back to the first item on the next launch. A reader could
                // pick Français, be shown English, and be told nothing.
                //
                // The two things a reader can actually do - show the translation, and
                // how large it is - are real and live in the reader's own Reading
                // options, which is where they are now. This row stays because a
                // reader deserves to know *which* translation they are being shown,
                // and saying so is not decoration; what was decoration was offering
                // twelve answers to a question with one.
                //
                // The same treatment as the Arabic-size row below, and for the same
                // reason: a control that does not do anything is worse than no
                // control, because it is a promise.
                ActionRow(
                    title = strings.translationLabel,
                    value = QuranBrowse.TRANSLATION_EDITION,
                    subtitle = strings.more.reader.changeInReader,
                    showChevron = false,
                    enabled = false,
                    onClick = {}
                )
                RowDivider()
                // A summary, not a second slider.
                //
                // This used to be a live Arabic-size slider bound to
                // `quranFontScale`, which the reader no longer reads - so it
                // looked like it worked and changed nothing. The reading
                // appearance now has exactly one home, the reader's own Reading
                // options, and this row reports the current value and says where
                // to change it. Two controls writing the same preference is how
                // they drift apart.
                ActionRow(
                    title = strings.arabicTextSizeLabel,
                    value = "${(state.quranReadingOptions.arabicScale * 100).toInt()}%",
                    subtitle = strings.more.reader.changeInReader,
                    showChevron = false,
                    enabled = false,
                    onClick = {}
                )
            }

            // -- Diagnostics ----------------------------------------------
            SectionHeader(strings.more.sectionAbout)
            SectionGroup {
                // "Prayer schedule cache" and its "Last checked" subtitle are gone.
                //
                // The row opened a sheet whose detail read "Last checked: Online ·
                // Synced at 14:32 (Verified Ephemeris)", and the three sentences
                // that could appear there were written by three different code
                // paths, none of which contacted a server - the app has no HTTP
                // client at all. One of them claimed 365 days were verified for a
                // path that computes one. A prayer app telling a reader its times
                // have been verified is not a cosmetic bug.
                //
                // The information a reader actually wants here is *how* their
                // times are derived, and that is real, is already collected, and
                // is already on the Prayer screen. So the row becomes what it
                // always should have been: the calculation source, with a control
                // that does something.
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
                // Connectivity, reported as connectivity.
                //
                // It is a *status*, not an action: nothing is fetched, so there is
                // nothing to retry, and a button that re-reads the platform's own
                // answer and reports it back is a control that only exists to be
                // pressed. The reason it is worth showing at all is the audio
                // stream, and that is stated in the storage sheet beside it.
                ActionRow(
                    title = strings.networkSyncLabel,
                    subtitle = if (state.isOnline) strings.onlineStatus else strings.offlineStatus,
                    showChevron = false,
                    enabled = false,
                    onClick = {}
                )
                RowDivider()
                SectionGroup {
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

            RowDivider()

            // Every country's capital, so a location is a name away rather than
            // two numbers. Bounded and lazy: the sheet's own content is a
            // scrolling column, and 195 rows composed eagerly inside one is a
            // visible stutter on open.
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

            // Free entry. The callback existed and was wired from MainActivity
            // but nothing ever called it, so the city list plus GPS were the only
            // ways to set a location - and 16 preset cities is not enough for
            // anyone who does not live in one of them. The seven orphaned
            // strings and imports that were already in the file are what this
            // form was always meant to consume.
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
                    // Validate before committing. Bounds are checked here rather
                    // than in the ViewModel so the user is told what is wrong
                    // next to the field, instead of the app silently keeping a
                    // location it cannot calculate with.
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

        Sheet.PRAYER_MODES -> OptionSheet(
            title = strings.more.perPrayerModes,
            onDismiss = { sheet = null }
        ) {
            // Sunrise is offered here because it has a real, separately
            // configurable alert - it is not one of the five obligatory
            // prayers, but it does sound. Its seeded value is
            // "Silent Reminder", which is not in ALERT_MODES, so
            // `indexOf` returned -1, `coerceAtLeast(0)` turned that into 0,
            // and the row highlighted "Full Adhan" on first run for every
            // user - advertising a full adhan call at sunrise that the stored
            // setting never asked for. Sunrise therefore gets its own option
            // list, built around the value it is actually seeded with.
            listOf(
                Prayer.FAJR, Prayer.DHUHR, Prayer.ASR,
                Prayer.MAGHRIB, Prayer.ISHA, Prayer.SUNRISE
            ).forEach { prayer ->
                val current = state.prayerAlertModes[prayer] ?: ALERT_MODES.first()
                val options = if (prayer == Prayer.SUNRISE) SUNRISE_ALERT_MODES else ALERT_MODES
                Column(modifier = Modifier.padding(vertical = space.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.prayerName(prayer),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        PlayPreviewButton(
                            label = "${strings.more.testSound}: ${strings.prayerName(prayer)}",
                            isPlaying = state.audioPreviewPlaying == current,
                            onToggle = { onPlayAudioPreview(current) }
                        )
                    }
                    Spacer(Modifier.height(space.sm))
                    ChipRow(
                        // Labels are localized; the values passed back are still
                        // the stored preference keys.
                        labels = options.map { strings.more.alertModeLabel(it) },
                        selectedIndex = options.indexOf(current)
                            .takeIf { it >= 0 }
                            ?: options.indexOfFirst { strings.more.alertModeLabel(it) == strings.more.alertModeLabel(current) },
                        onSelect = { onPrayerAlertModeChange(prayer, options[it]) }
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
                        PlayPreviewButton(
                            label = "${strings.more.testSound}: $sound",
                            isPlaying = state.audioPreviewPlaying == sound,
                            onToggle = {
                                if (state.audioPreviewPlaying == sound) onStopAudioPreview()
                                else onPlayAudioPreview(sound)
                            }
                        )
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
                        PlayPreviewButton(
                            label = "${strings.more.testSound}: $reciter",
                            isPlaying = state.audioPreviewPlaying == reciter,
                            onToggle = {
                                if (state.audioPreviewPlaying == reciter) onStopAudioPreview()
                                else onPlayAudioPreview(reciter)
                            }
                        )
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
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(IconSize.sm))
                    Spacer(Modifier.width(space.xs))
                    Text(strings.more.testSound, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        onStopAudioPreview()
                        sheet = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                ) {
                    Text(strings.more.stopAudio, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Sheet.CALCULATION -> OptionSheet(
            title = strings.transparentCalculationSource,
            subtitle = strings.more.computedOnDevice,
            onDismiss = { sheet = null }
        ) {
            // Every row here is a real, current input to the engine, read from
            // state. That is the whole reason to show this sheet: a reader whose
            // Isha looks wrong can see which method, which madhhab, which
            // adjustment and which coordinates produced it, and change any of them
            // from the rows above. Nothing here is a claim about a server.
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
            // Re-derive today's times and re-arm the alarms.
            //
            // The only control in this sheet, and the only one with an effect a
            // reader can observe: alarms are lost when a device reboots, when a
            // battery-optimisation app is installed, and when a permission is
            // revoked, and the app cannot always hear about it. This puts them
            // back. It is not a "recompute 365-day schedule" - nothing is cached
            // and nothing is 365 days; the engine computes today and tomorrow in
            // under a millisecond and hands the result to AlarmManager.
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
            // Read-only. The "Clear audio cache" button is gone.
            //
            // QuranAudioPlayer streams from everyayah.com through MediaPlayer and
            // keeps nothing on disk; when the stream cannot be prepared it falls
            // back to a synthesised chime. There is no cache. The button deleted
            // nothing, reported a hardcoded "0 KB" as though it had measured
            // something, and wrote that into `lastChecked` - the field the
            // Ephemeris row and the "Last checked" detail both display - so
            // pressing it made the app claim its prayer times had been verified
            // when they had not. A control that reports a fabricated result is
            // worse than no control.
            DetailList(
                items = listOf(
                    strings.sectionQuran to strings.more.corpusSummary,
                    strings.more.audioSourceLabel to strings.more.audioStreamedNotCached
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
    VOLUME, CALCULATION, STORAGE, COMPASS
}

/**
 * A minute offset, always signed.
 *
 * "+0" and "0" are different sentences, and a reader scanning a list of five
 * offsets should not have to check which convention each one uses.
 */
private fun signed(minutes: Int): String = if (minutes >= 0) "+$minutes" else "$minutes"

/** Per-prayer minute offsets, -15..+15. */
@Composable
private fun AdjustmentSliders(
    adjustments: PrayerAdjustments,
    onChange: (PrayerAdjustments) -> Unit
) {
    val strings = LocalStrings.current
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
            label = strings.prayerName(binding.prayer),
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

/**
 * The six applied offsets, in reading order, with Sunrise among them.
 *
 * One list, because the same five-and-a-half facts were written out in **three**
 * places - this summary, the Prayer screen's "Applied adjustments" and the Settings
 * calculation sheet - and all three omitted Sunrise. So a reader who set a +3 minute
 * sunrise offset saw an *empty* summary, two diagnostic sheets that never mentioned
 * sunrise, and an engine that applied it. A summary that silently drops one of six
 * settings is worse than no summary: it says "nothing adjusted" and means something
 * else.
 *
 * Sunrise is here because the reader can set it. The `Prayer` enum carries it because
 * the engine applies it, and an offset the reader can change and no screen will
 * report has not really been exposed.
 */
internal fun appliedAdjustments(adjustments: PrayerAdjustments): List<Pair<Prayer, Int>> =
    listOf(
        Prayer.FAJR to adjustments.fajr,
        Prayer.SUNRISE to adjustments.sunrise,
        Prayer.DHUHR to adjustments.dhuhr,
        Prayer.ASR to adjustments.asr,
        Prayer.MAGHRIB to adjustments.maghrib,
        Prayer.ISHA to adjustments.isha
    )

/** The non-zero ones, in reading order. */
internal fun adjustedPrayers(adjustments: PrayerAdjustments): List<Pair<Prayer, Int>> =
    appliedAdjustments(adjustments).filter { it.second != 0 }

/** The summary the Adjustments row shows: one letter per adjusted prayer, or nothing. */
private fun adjustmentSummary(state: SalahUiState): String {
    val parts = adjustedPrayers(state.adjustments)
    return if (parts.isEmpty()) {
        ""
    } else {
        parts.joinToString(" ") { (prayer, minutes) ->
            "${prayer.ordinalLetter()}${minutes}"
        }
    }
}

/**
 * The one or two letters a prayer is abbreviated to in the summary.
 *
 * Hand-written rather than derived, because "Fajr" and "Sunrise" both begin with F
 * and a summary is only useful if the reader can tell them apart at a glance.
 */
private fun Prayer.ordinalLetter(): String = when (this) {
    Prayer.FAJR -> "F"
    Prayer.SUNRISE -> "S"
    Prayer.DHUHR -> "D"
    Prayer.ASR -> "A"
    Prayer.MAGHRIB -> "M"
    Prayer.ISHA -> "I"
}

private fun hijriSummary(offset: Int): String =
    if (offset == 0) "±0" else if (offset > 0) "+$offset" else "$offset"

/** Human label for a stored theme preference, which is persisted as a raw string. */
private fun UiStrings.themeLabel(theme: String): String = when (theme) {
    "Dark Mode (OLED)" -> more.themeDark
    "Clean Light" -> more.themeLight
    else -> more.themeSystem
}
