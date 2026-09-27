package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.SalahViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.prayer.PrayerScreen
import com.example.ui.qibla.QiblaScreen
import com.example.ui.quran.QuranScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.Motion
import com.example.ui.theme.SalahReduceMotion
import com.example.ui.theme.SalahTheme
import com.example.ui.theme.Space
import com.example.ui.theme.screenEnterBack
import com.example.ui.theme.screenEnterForward
import com.example.ui.theme.screenExitBack
import com.example.ui.theme.screenExitForward

/**
 * The five top-level destinations.
 *
 * The label is carried in the enum only as documentation; the rendered label
 * always comes from the localisation dictionary, because hardcoding "Today" here
 * meant the bottom bar stayed English in Arabic, Urdu and the other nine
 * languages.
 */
enum class SalahDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    PRAYER("prayer", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    QURAN("quran", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    QIBLA("qibla", Icons.Filled.Explore, Icons.Outlined.Explore),
    SETTINGS("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: SalahViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            // The app has three explicit theme choices plus "match system".
            // Dynamic (wallpaper) colour is intentionally not offered: it cannot
            // be contrast-checked, and this design fixes its accent on purpose.
            val darkTheme = when (uiState.appTheme) {
                "Dark Mode (OLED)" -> true
                "Clean Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Status bar icon appearance was never set anywhere in the app.
            // enableEdgeToEdge() makes the bar transparent and the platform
            // default is light icons, so in the light theme white icons sat on
            // the #F6F8FA page and were effectively invisible - the clock and
            // battery could not be read at all. The dark theme happened to match
            // the default, which is why only light mode looked broken.
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
            }

            val reduceMotion = SalahReduceMotion.remember()
            // Mirror into Motion so its non-composable helpers honour the setting.
            SideEffect { Motion.reduced = reduceMotion }

            CompositionLocalProvider(SalahReduceMotion.Local provides reduceMotion) {
                SalahTheme(darkTheme = darkTheme) {
                    ProvideAppLanguage(language = uiState.language) {
                        SalahApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SalahApp(viewModel: SalahViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SalahDestination.TODAY.route
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = LocalStrings.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.fetchCurrentLocation()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Remember the answer either way. If the user declines we must not ask
        // again on the next cold start - only the system prompt is limited to
        // two attempts, and our own rationale must not become a nag.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED_NOTIFICATIONS, true)
            .putBoolean(KEY_NOTIFICATIONS_GRANTED, granted)
            .apply()
    }

    // The permission is no longer requested blind on the first frame.
    //
    // It used to fire from LaunchedEffect(Unit), so a first-time user met a
    // system dialog for notifications before they had seen the app, understood
    // that it is a prayer app, or been given any reason to say yes. Android's
    // own guidance and the Material permission pattern both ask for the
    // rationale first and in context.
    //
    // Nothing else in the app requested this permission, so simply deleting the
    // effect would have meant notifications were never asked for and the adhan
    // feature failed silently. The rationale below is what replaced it.
    val askNotifications = remember {
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED_NOTIFICATIONS, false)
    }
    var showNotificationRationale by rememberSaveable { mutableStateOf(askNotifications) }

    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Tapping the current tab again returns to that tab's root. Without this,
    // switching Today -> Settings -> Today leaves the Settings scroll position
    // and any open sheet state behind, so the tab felt stuck.
    BackHandler(enabled = currentRoute != SalahDestination.TODAY.route) {
        navController.navigate(SalahDestination.TODAY.route) {
            popUpTo(SalahDestination.TODAY.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = { SalahNavigationBar(navController = navController, currentRoute = currentRoute) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Each destination owns its own top bar, so the title always answers
            // "where am I?" on every screen instead of only on some of them.
            NavHost(
                navController = navController,
                startDestination = SalahDestination.TODAY.route,
                enterTransition = { screenEnterForward() },
                exitTransition = { screenExitForward() },
                popEnterTransition = { screenEnterBack() },
                popExitTransition = { screenExitBack() },
                modifier = Modifier.fillMaxSize()
            ) {
                composable(SalahDestination.TODAY.route) {
                    HomeScreen(
                        state = uiState,
                        onTogglePrayer = viewModel::togglePrayerCompleted,
                        onContinueReadingClick = {
                            viewModel.jumpToContinueReading()
                            navController.navigateTab(SalahDestination.QURAN)
                        },
                        onOpenPrayerTimes = { navController.navigateTab(SalahDestination.PRAYER) },
                        onLocationClick = { navController.navigateTab(SalahDestination.SETTINGS) },
                        onRefreshClick = viewModel::refreshData,
                        onToggleGlobalSilent = viewModel::toggleGlobalSilentMode,
                        onCyclePrayerAlertMode = viewModel::cyclePrayerAlertMode,
                        onSilenceActiveAlert = viewModel::stopAudioPreview
                    )
                }

                composable(SalahDestination.PRAYER.route) {
                    PrayerScreen(
                        state = uiState,
                        onMethodChange = viewModel::setCalculationMethod,
                        onMadhhabChange = viewModel::setMadhhab
                    )
                }

                composable(SalahDestination.QURAN.route) {
                    QuranScreen(
                        state = uiState,
                        onSurahSelected = viewModel::selectSurah,
                        onAyahViewed = viewModel::onAyahViewed,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onTogglePlayAyah = viewModel::togglePlayAyah,
                        onStopAudio = viewModel::stopAudio,
                        onFontScaleChange = viewModel::setQuranFontScale,
                        onPageSelected = viewModel::selectPage,
                        onJuzSelected = viewModel::selectJuz,
                        onHizbSelected = viewModel::selectHizb
                    )
                }

                composable(SalahDestination.QIBLA.route) {
                    QiblaScreen(
                        state = uiState,
                        onToggleTrueNorth = viewModel::toggleTrueNorth,
                        onFetchLocation = {
                            if (hasLocationPermission(context)) {
                                viewModel.fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                            }
                        }
                    )
                }

                composable(SalahDestination.SETTINGS.route) {
                    SettingsScreen(
                        state = uiState,
                        onLocationSelect = viewModel::setLocation,
                        onFetchLocation = { locationPermissionLauncher.launch(LOCATION_PERMISSIONS) },
                        onMethodSelect = viewModel::setCalculationMethod,
                        onMadhhabSelect = viewModel::setMadhhab,
                        onAdjustmentsChange = viewModel::setAdjustments,
                        onAdhanToggle = viewModel::setAdhanNotification,
                        onPrePrayerToggle = viewModel::setPrePrayerAlert,
                        onVibrateOnlyToggle = viewModel::setVibrateOnly,
                        onGlobalSilentToggle = viewModel::toggleGlobalSilentMode,
                        onAutoSilentDuringPrayerToggle = viewModel::toggleAutoSilentDuringPrayer,
                        onAutoSilentDurationChange = viewModel::setAutoSilentDuration,
                        onLanguageSelect = viewModel::setLanguage,
                        onRiwayahSelect = viewModel::setRiwayah,
                        onThemeSelect = viewModel::setAppTheme,
                        onQuranScriptSelect = viewModel::setQuranScript,
                        onTimeFormatToggle = viewModel::setTimeFormat24h,
                        onAdhanSoundSelect = viewModel::setAdhanSound,
                        onHijriAdjustmentChange = viewModel::setHijriAdjustment,
                        onReciterSelect = viewModel::setReciter,
                        onFontScaleChange = viewModel::setQuranFontScale,
                        onRefreshClick = viewModel::refreshData,
                        onPrePrayerOffsetChange = viewModel::setPrePrayerOffsetMinutes,
                        onAdhanVolumeChange = viewModel::setAdhanVolume,
                        onPrayerAlertModeChange = viewModel::setPrayerAlertMode,
                        onPlayAudioPreview = viewModel::playAudioPreview,
                        onStopAudioPreview = viewModel::stopAudioPreview,
                        onCustomLocationSave = viewModel::setCustomLocation,
                        onTranslationSelect = viewModel::setTranslationEdition,
                        onRecomputeEphemerisCache = viewModel::recomputeEphemerisCache,
                        onClearAudioCache = viewModel::clearAudioCache,
                        onResetAllSettings = viewModel::resetAllSettings,
                        onLivingSkyChange = viewModel::setLivingSkyEnabled
                    )
                }
            }
        }
    }

    if (showNotificationRationale) {
        NotificationRationaleSheet(
            onAllow = {
                showNotificationRationale = false
                requestNotifications()
            },
            onDismiss = {
                showNotificationRationale = false
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_ASKED_NOTIFICATIONS, true)
                    .apply()
            }
        )
    }
}

private const val PREFS = "salah_permission_state"
private const val KEY_ASKED_NOTIFICATIONS = "asked_post_notifications"
private const val KEY_NOTIFICATIONS_GRANTED = "post_notifications_granted"

/**
 * Explains why notifications are wanted before the system prompt appears.
 *
 * Three lines, one decision, and an honest Not now: the adhan is the app's
 * reason to exist, so silently never asking would have been worse, and asking
 * blind would have been colder. The prayer times themselves are always visible
 * on Today, so declining costs nothing, and the setting stays reachable in
 * Settings > Notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationRationaleSheet(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.lg)
                .padding(bottom = space.xxxl),
            verticalArrangement = Arrangement.spacedBy(space.md)
        ) {
            Text(
                text = strings.more.allowNotificationsTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = strings.more.allowNotificationsMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.more.allowNotificationsAction)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.more.notNow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Tab switching that keeps one entry per tab and restores each tab's state. */
private fun NavHostController.navigateTab(destination: SalahDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SalahNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    val strings = LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars
        ) {
            SalahDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                val label = when (destination) {
                    SalahDestination.TODAY -> strings.navToday
                    SalahDestination.PRAYER -> strings.navPrayer
                    SalahDestination.QURAN -> strings.navQuran
                    SalahDestination.QIBLA -> strings.navQibla
                    SalahDestination.SETTINGS -> strings.navSettings
                }

                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) navController.navigateTab(destination) },
                    // The label is the accessible name; a second announcement from
                    // the icon would just repeat it.
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null
                        )
                    },
                    label = {
                        AnimatedContent(
                            targetState = label,
                            transitionSpec = {
                                fadeIn(tween(Motion.duration(Motion.MICRO))) togetherWith
                                    fadeOut(tween(Motion.duration(Motion.MICRO)))
                            },
                            label = "nav_label"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.semantics { testTagsAsResourceId = true }
                )
            }
        }
    }
}
