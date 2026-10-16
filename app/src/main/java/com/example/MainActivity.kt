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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.SalahViewModel
import com.example.ui.home.TodayScreen
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

/**
 * The four destinations the bottom bar offers.
 *
 * A list rather than `entries`, because the bar is not every destination. Prayer
 * is one of the app's real screens - calculation method, madhhab, the month
 * table, and the day switcher the Today page follows - but it is reached from
 * Settings rather than from the dock, so five tabs became four and the phone's
 * dock stopped being the widest thing on screen.
 */
private val SALAH_TABS = listOf(
    SalahDestination.TODAY,
    SalahDestination.QURAN,
    SalahDestination.QIBLA,
    SalahDestination.SETTINGS
)

/**
 * Whether the reader is taking the whole screen, this moment.
 *
 * The flag alone is not the answer, and that was the bug: the reader's
 * immersive flag hid the dock and the status bar on Today, and - because it was
 * persisted - on the first screen after every relaunch. A takeover is a fact
 * about the destination on screen, so the route is half the answer, and the
 * dock's visibility and the reset below both read this one predicate so they
 * cannot disagree about when the takeover is in force.
 */
internal fun readerTakesTheScreen(route: String, immersive: Boolean): Boolean =
    immersive && route == SalahDestination.QURAN.route

/**
 * Ends the reader's takeover when the destination under it changes.
 *
 * The derivation above keeps the chrome honest on the frame the route changes;
 * this keeps the flag honest, so a later visit to the reader starts with the
 * chrome in place instead of reliving a mode the reader had left. The flag is
 * session state now - nothing writes it to preferences - so this is the only
 * thing that ends it besides the reader's own toggle.
 *
 * Scoped to the route rather than run from the destination's `onDispose`,
 * because a destination leaves the screen by more paths than it can call back
 * on: a tab switch and a back-stack pop are not the same callback.
 */
@Composable
internal fun ClearImmersiveWhenLeavingReader(
    route: String,
    immersive: Boolean,
    onClear: () -> Unit
) {
    LaunchedEffect(route, immersive) {
        if (immersive && !readerTakesTheScreen(route, immersive)) onClear()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: SalahViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            // The status bar's *icons* follow the theme, and only the theme.
            // Whether the bar is there at all depends on the destination on
            // screen, which this composable does not know - `SalahApp` derives
            // that from the route and owns the hide/show below.
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val strings = LocalStrings.current

    // Whether the reader is taking the whole screen, this moment.
    //
    // Derived from the route, not read from the flag alone. The flag says the
    // reader asked for the takeover; the route says they are still in the
    // reader. Reading the flag by itself is how the dock and the status bar
    // stayed hidden on Today, and the flag below is cleared the moment the
    // reader leaves so a return does not relive the mode.
    val immersive = readerTakesTheScreen(currentRoute, uiState.isQuranImmersive)
    ClearImmersiveWhenLeavingReader(
        route = currentRoute,
        immersive = uiState.isQuranImmersive,
        onClear = { viewModel.setQuranImmersive(false) }
    )

    // The status bar belongs to the window, but whether it is hidden is a fact
    // about the destination on screen, so it is set here where the route is
    // known rather than inside the reader - setting it from a screen that gets
    // navigated away from is exactly how it was left hidden on Today.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            if (immersive) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                // Swipe it back rather than never showing it again: a bar
                // the user cannot summon with a gesture is a bar they will
                // assume the app got stuck behind.
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // The ticker is a 1 Hz recomputation and the compass is a sensor stream;
    // both run only while the app is in front of the reader. The write side
    // (the ViewModel) is told, rather than the app collecting differently,
    // because a stopped ticker is also less work, not just a quieter one.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setForeground(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setForeground(false) }

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

    // The dock is hidden while the reader is taking the screen. Not just made
    // transparent: the reader's own controls are already off the top of the screen
    // at that point, so leaving a navigation bar under a full page of Arabic would
    // be the one piece of chrome the reader could not dismiss. The value is the
    // route-derived one above, so "hidden" cannot be true on a destination the
    // reader is not in.

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (!immersive) {
                SalahNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (immersive) 0.dp else innerPadding.calculateBottomPadding())
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
                    TodayScreen(
                        state = uiState,
                        onLocationClick = { navController.navigateTab(SalahDestination.SETTINGS) },
                        onContinueReadingClick = {
                            viewModel.jumpToContinueReading()
                            navController.navigateTab(SalahDestination.QURAN)
                        }
                    )
                }

                composable(SalahDestination.PRAYER.route) {
                    PrayerScreen(
                        state = uiState,
                        onMethodChange = viewModel::setCalculationMethod,
                        onMadhhabChange = viewModel::setMadhhab,
                        onSelectDate = viewModel::setSelectedDate
                    )
                }

                composable(SalahDestination.QURAN.route) {
                    QuranScreen(
                        state = uiState,
                        onOpen = viewModel::requestOpen,
                        onOpenConsumed = viewModel::onOpenConsumed,
                        onAyahViewed = viewModel::onAyahViewed,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onTogglePlayAyah = viewModel::togglePlayAyah,
                        onStopAudio = viewModel::stopAudio,
                        onOptionsChange = viewModel::setQuranReadingOptions,
                        onImmersiveChange = viewModel::setQuranImmersive
                    )
                }

                composable(SalahDestination.QIBLA.route) {
                    // The heading is the only thing on this screen, and the
                    // sensors are only polled while a screen that shows one is
                    // composed. Settings has its own registration, because its
                    // sensor diagnostics read the same accuracy report.
                    DisposableEffect(Unit) {
                        viewModel.startCompass()
                        onDispose { viewModel.stopCompass() }
                    }
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
                    DisposableEffect(Unit) {
                        viewModel.startCompass()
                        onDispose { viewModel.stopCompass() }
                    }
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
                        onThemeSelect = viewModel::setAppTheme,
                        onTimeFormatToggle = viewModel::setTimeFormat24h,
                        onAdhanSoundSelect = viewModel::setAdhanSound,
                        onHijriAdjustmentChange = viewModel::setHijriAdjustment,
                        onReciterSelect = viewModel::setReciter,
                        onPrePrayerOffsetChange = viewModel::setPrePrayerOffsetMinutes,
                        onAdhanVolumeChange = viewModel::setAdhanVolume,
                        onPrayerAlertModeChange = viewModel::setPrayerAlertMode,
                        onPlayAudioPreview = viewModel::playAudioPreview,
                        onStopAudioPreview = viewModel::stopAudioPreview,
                        onCustomLocationSave = viewModel::setCustomLocation,
                        onReschedulePrayers = viewModel::reschedulePrayers,
                        onResetAllSettings = viewModel::resetAllSettings,
                        onOpenPrayer = { navController.navigateTab(SalahDestination.PRAYER) },
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

/**
 * Explains why notifications are wanted before the system prompt appears.
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
            SALAH_TABS.forEach { destination ->
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
