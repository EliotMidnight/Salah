package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.engine.PrayerNotificationManager
import com.example.ui.SalahViewModel
import com.example.ui.components.DockItem
import com.example.ui.components.GlassChromeScaffold
import com.example.ui.components.SalahDock
import com.example.ui.home.TodayScreen
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.qibla.QiblaScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.system.NotificationIssue
import com.example.ui.system.PermissionAccess
import com.example.ui.system.SystemAccessActions
import com.example.ui.system.SystemPermissionPrefs
import com.example.ui.system.canScheduleExactAlarms
import com.example.ui.system.hasLocationPermission
import com.example.ui.system.hasNotificationPermission
import com.example.ui.system.isLocationEnabled
import com.example.ui.system.rememberSystemAccessState
import com.example.ui.theme.Glass
import com.example.ui.theme.LocalGlassEnabled
import com.example.ui.theme.LocalHighContrast
import com.example.ui.theme.Motion
import com.example.ui.theme.SalahReduceMotion
import com.example.ui.theme.SalahTheme
import com.example.ui.theme.Space
import com.example.ui.theme.glassEnabled
import com.example.ui.theme.screenEnterBack
import com.example.ui.theme.screenEnterForward
import com.example.ui.theme.screenExitBack
import com.example.ui.theme.screenExitForward

/**
 * The three top-level destinations.
 */
enum class SalahDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    QIBLA("qibla", Icons.Filled.Explore, Icons.Outlined.Explore),
    SETTINGS("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * The three destinations the bottom bar offers.
 *
 * Every destination is on the bar - Today, Qibla, Settings - so the dock is the
 * app's only navigation and nothing lives behind an extra tap.
 */
private val SALAH_TABS = listOf(
    SalahDestination.TODAY,
    SalahDestination.QIBLA,
    SalahDestination.SETTINGS
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: SalahViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // The app has three explicit theme choices plus "match system".
            // The accent is a separate choice: a muted preset, or the wallpaper
            // primary on Android 12+. Only the accent family follows that choice;
            // the monochrome surfaces and every contrast claim stay put.
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
                SalahTheme(darkTheme = darkTheme, accentKey = uiState.accent) {
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

    val prayerChannelId = PrayerNotificationManager.channelIdFor(
        isGlobalSilent = uiState.isGlobalSilentMode,
        vibrateOnly = uiState.vibrateOnly
    )
    var systemAccessRevision by remember { mutableStateOf(0) }
    val systemAccess = rememberSystemAccessState(prayerChannelId, systemAccessRevision)
    var pendingAlertEnable by rememberSaveable { mutableStateOf<AlertKind?>(null) }
    var pendingLocationFix by rememberSaveable { mutableStateOf(false) }
    var pendingExactAlarmReschedule by rememberSaveable { mutableStateOf(false) }

    // The one place the chrome material's runtime gate is decided. The build
    // flag, the platform capability, Reduce Transparency and Android's high
    // contrast text setting all resolve to a single boolean; no screen or
    // component re-decides it.
    val highContrast = rememberHighContrast()
    val glassOn = glassEnabled(
        buildEnabled = Glass.ENABLED,
        sdkInt = Build.VERSION.SDK_INT,
        reduceTransparency = uiState.reduceTransparency,
        highContrast = highContrast
    )
    val dockItems = remember(strings) {
        SALAH_TABS.map { destination ->
            DockItem(
                route = destination.route,
                label = when (destination) {
                    SalahDestination.TODAY -> strings.navToday
                    SalahDestination.QIBLA -> strings.navQibla
                    SalahDestination.SETTINGS -> strings.navSettings
                },
                selectedIcon = destination.selectedIcon,
                unselectedIcon = destination.unselectedIcon
            )
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
        SystemPermissionPrefs.markLocationAsked(context)
        if (permissions.values.any { it }) {
            viewModel.fetchCurrentLocation()
        } else {
            viewModel.reportLocationPermissionDenied()
        }
        systemAccessRevision++
    }

    fun applyPendingAlertEnable() {
        when (pendingAlertEnable) {
            AlertKind.ADHAN -> viewModel.setAdhanNotification(true)
            AlertKind.PRE_PRAYER -> viewModel.setPrePrayerAlert(true)
            null -> Unit
        }
        pendingAlertEnable = null
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Remember that the system prompt actually ran. This is what
        // distinguishes "never asked" from "asked and declined", so a later
        // tap on an alert switch can send the reader to system settings
        // instead of showing a prompt Android will ignore.
        SystemPermissionPrefs.markNotificationPrompted(context)
        if (granted) {
            applyPendingAlertEnable()
        } else {
            pendingAlertEnable = null
        }
        systemAccessRevision++
    }

    fun startAppDetails() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        }
    }

    fun openAppSettings() {
        // If the reader grants the permission while app settings is open, the
        // ON_RESUME below can finish the location attempt they started.
        pendingLocationFix = true
        startAppDetails()
    }

    fun openLocationSettings() {
        pendingLocationFix = true
        runCatching {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    fun openPrayerChannelSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startAppDetails()
            return
        }
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, prayerChannelId)
                }
            )
        }.onFailure { startAppDetails() }
    }

    fun openNotificationSettings() {
        if (systemAccess.notificationIssue == NotificationIssue.CHANNEL_BLOCKED) {
            openPrayerChannelSettings()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startAppDetails()
            return
        }
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }.onFailure { startAppDetails() }
    }

    fun requestExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        pendingExactAlarmReschedule = true
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri()
                )
            )
        }.onFailure {
            pendingExactAlarmReschedule = false
            startAppDetails()
        }
    }

    fun requestLocationFix() {
        when {
            hasLocationPermission(context) -> viewModel.fetchCurrentLocation()
            systemAccess.locationPermission == PermissionAccess.DENIED_PERMANENTLY -> {
                viewModel.reportLocationPermissionDenied()
                openAppSettings()
            }

            else -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    fun requestNotificationPermission() {
        when {
            hasNotificationPermission(context) -> applyPendingAlertEnable()
            systemAccess.notificationPermission == PermissionAccess.DENIED_PERMANENTLY ->
                openNotificationSettings()

            else -> {
                // hasNotificationPermission() is always true below Android 13,
                // so this branch is the Android 13+ prompt. Record the launch
                // before the dialog appears: a result may not come back if the
                // process is killed while it is open.
                SystemPermissionPrefs.markNotificationPrompted(context)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Permission is asked for only when a switch asks for it.
    //
    // A rationale sheet on the first frame is still a first-frame request in
    // disguise: it asks a reader to decide about prayer alerts before they have
    // seen the prayer page or chosen an alert. The rationale therefore starts
    // closed and is opened by [requestAlerts] at the moment an Adhan or
    // pre-prayer switch is turned on. If the reader never enables either, the
    // app never interrupts them about notifications.
    var showNotificationRationale by rememberSaveable { mutableStateOf(false) }

    fun requestAlerts(kind: AlertKind) {
        if (systemAccess.notificationsReady) {
            when (kind) {
                AlertKind.ADHAN -> viewModel.setAdhanNotification(true)
                AlertKind.PRE_PRAYER -> viewModel.setPrePrayerAlert(true)
            }
            pendingAlertEnable = null
            return
        }

        if (systemAccess.notificationIssue == NotificationIssue.PERMISSION_DENIED) {
            if (systemAccess.notificationPermission == PermissionAccess.CAN_ASK) {
                pendingAlertEnable = kind
                showNotificationRationale = true
            } else {
                pendingAlertEnable = null
                openNotificationSettings()
            }
            return
        }

        // The app-level switch or the channel is off. Honour the intent and let
        // the banner beside the switch say which system screen fixes it.
        when (kind) {
            AlertKind.ADHAN -> viewModel.setAdhanNotification(true)
            AlertKind.PRE_PRAYER -> viewModel.setPrePrayerAlert(true)
        }
        pendingAlertEnable = null
    }

    val systemAccessActions = SystemAccessActions(
        requestLocation = { requestLocationFix() },
        openLocationSettings = { openLocationSettings() },
        openAppSettings = { openAppSettings() },
        retryLocation = { viewModel.fetchCurrentLocation() },
        requestNotifications = { requestNotificationPermission() },
        openNotificationSettings = { openNotificationSettings() },
        openPrayerChannelSettings = { openPrayerChannelSettings() },
        requestExactAlarms = { requestExactAlarms() },
        sendTestNotification = {
            if (systemAccess.notificationsReady) viewModel.sendTestNotification()
        }
    )

    // Returning from a system settings screen is the only signal that the
    // reader may have changed something the app could not observe. Finish the
    // action they started instead of making them tap again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (pendingLocationFix &&
            hasLocationPermission(context) &&
            isLocationEnabled(context)
        ) {
            pendingLocationFix = false
            viewModel.fetchCurrentLocation()
        }
        if (pendingExactAlarmReschedule && canScheduleExactAlarms(context)) {
            pendingExactAlarmReschedule = false
            viewModel.reschedulePrayers()
        }
        if (pendingAlertEnable != null && hasNotificationPermission(context)) {
            applyPendingAlertEnable()
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

    CompositionLocalProvider(
        LocalGlassEnabled provides glassOn,
        LocalHighContrast provides highContrast
    ) {
        GlassChromeScaffold(
            dock = { dockModifier ->
                SalahDock(
                    items = dockItems,
                    selectedRoute = currentRoute,
                    onSelect = { route ->
                        val destination = SALAH_TABS.firstOrNull { it.route == route }
                        if (destination != null && destination.route != currentRoute) {
                            navController.navigateTab(destination)
                        }
                    },
                    modifier = dockModifier
                )
            }
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
                        onLocationClick = { navController.navigateTab(SalahDestination.SETTINGS) }
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
                        onFetchLocation = { requestLocationFix() },
                        systemAccess = systemAccess,
                        systemAccessActions = systemAccessActions
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
                        onFetchLocation = { requestLocationFix() },
                        onMethodSelect = viewModel::setCalculationMethod,
                        onMadhhabSelect = viewModel::setMadhhab,
                        onAdjustmentsChange = viewModel::setAdjustments,
                        onAdhanToggle = { enabled ->
                            if (enabled) {
                                requestAlerts(AlertKind.ADHAN)
                            } else {
                                pendingAlertEnable = null
                                viewModel.setAdhanNotification(false)
                            }
                        },
                        onPrePrayerToggle = { enabled ->
                            if (enabled) {
                                requestAlerts(AlertKind.PRE_PRAYER)
                            } else {
                                pendingAlertEnable = null
                                viewModel.setPrePrayerAlert(false)
                            }
                        },
                        onVibrateOnlyToggle = viewModel::setVibrateOnly,
                        onGlobalSilentToggle = viewModel::toggleGlobalSilentMode,
                        onLanguageSelect = viewModel::setLanguage,
                        onThemeSelect = viewModel::setAppTheme,
                        onAccentSelect = viewModel::setAccent,
                        onReduceTransparencyChange = viewModel::setReduceTransparency,
                        onTimeFormatToggle = viewModel::setTimeFormat24h,
                        onHijriAdjustmentChange = viewModel::setHijriAdjustment,
                        onCustomLocationSave = viewModel::setCustomLocation,
                        onReschedulePrayers = viewModel::reschedulePrayers,
                        onResetAllSettings = viewModel::resetAllSettings,
                        systemAccess = systemAccess,
                        systemAccessActions = systemAccessActions
                    )
                }
            }
        }
    }

    if (showNotificationRationale) {
        NotificationRationaleSheet(
            onAllow = {
                showNotificationRationale = false
                requestNotificationPermission()
            },
            onDismiss = {
                showNotificationRationale = false
                pendingAlertEnable = null
            }
        )
    }
}

/** Which alert switch asked for notification permission. */
private enum class AlertKind { ADHAN, PRE_PRAYER }

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

/** Tab switching that keeps one entry per tab and restores each tab's state. */
private fun NavHostController.navigateTab(destination: SalahDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Android's "high contrast text" setting.
 *
 * API 36 exposes it through [AccessibilityManager]. Older platforms only have
 * the secure setting the system toggle writes; the key is hidden from the SDK
 * but is marked `@Readable` and has not changed since it was introduced. The
 * value is re-read on resume, because that is the normal path back from the
 * system Settings screen and avoids registering two different listeners for the
 * two API paths.
 */
@Composable
private fun rememberHighContrast(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(readHighContrast(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = readHighContrast(context)
    }
    return enabled
}

private fun readHighContrast(context: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
            ?.isHighContrastTextEnabled == true
    } else {
        Settings.Secure.getInt(
            context.contentResolver,
            "high_text_contrast_enabled",
            0
        ) == 1
    }
}.getOrDefault(false)
