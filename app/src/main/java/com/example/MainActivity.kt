package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserLocation
import com.example.ui.SalahUiState
import com.example.ui.SalahViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.prayer.PrayerScreen
import com.example.ui.qibla.QiblaScreen
import com.example.ui.quran.QuranScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SalahTheme
import com.example.ui.theme.expressiveEnterBack
import com.example.ui.theme.expressiveEnterForward
import com.example.ui.theme.expressiveExitBack
import com.example.ui.theme.expressiveExitForward
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ProvideAppLanguage

enum class SalahDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("today", "Today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    PRAYER("prayer", "Prayer", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    QURAN("quran", "Quran", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    QIBLA("qibla", "Qibla", Icons.Filled.Explore, Icons.Outlined.Explore),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide the status bar in full screen with transient swipe-down gesture behavior on Android 15
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())

        setContent {
            val viewModel: SalahViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val isDarkTheme = when (uiState.appTheme) {
                "Dark Mode (OLED)" -> true
                "Clean Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            SalahTheme(darkTheme = isDarkTheme) {
                ProvideAppLanguage(language = uiState.language) {
                    SalahApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SalahApp(viewModel: SalahViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SalahDestination.TODAY.route

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Request Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.fetchCurrentLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isTopLevelDestination = SalahDestination.values().any { it.route == currentRoute }
    val strings = LocalStrings.current

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    SalahDestination.values().forEach { dest ->
                        val isSelected = currentRoute == dest.route
                        val localizedLabel = when (dest) {
                            SalahDestination.TODAY -> strings.navToday
                            SalahDestination.PRAYER -> strings.navPrayer
                            SalahDestination.QURAN -> strings.navQuran
                            SalahDestination.QIBLA -> strings.navQibla
                            SalahDestination.SETTINGS -> strings.navSettings
                        }
                        NavigationBarItem(
                            selected = isSelected,
                            alwaysShowLabel = true,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(SalahDestination.TODAY.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = localizedLabel
                                )
                            },
                            label = {
                                Text(
                                    text = localizedLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${dest.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SalahDestination.TODAY.route,
            enterTransition = { expressiveEnterForward() },
            exitTransition = { expressiveExitForward() },
            popEnterTransition = { expressiveEnterBack() },
            popExitTransition = { expressiveExitBack() },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isTopLevelDestination) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(SalahDestination.TODAY.route) {
                HomeScreen(
                    state = uiState,
                    onTogglePrayer = { viewModel.togglePrayerCompleted(it) },
                    onContinueReadingClick = {
                        viewModel.jumpToContinueReading()
                        navController.navigate(SalahDestination.QURAN.route)
                    },
                    onOpenPrayerDetails = {
                        navController.navigate(SalahDestination.PRAYER.route)
                    },
                    onLocationClick = {
                        navController.navigate(SalahDestination.SETTINGS.route)
                    },
                    onSettingsClick = {
                        navController.navigate(SalahDestination.SETTINGS.route)
                    },
                    onRefreshClick = {
                        viewModel.refreshData()
                    },
                    onToggleGlobalSilent = {
                        viewModel.toggleGlobalSilentMode()
                    },
                    onCyclePrayerAlertMode = {
                        viewModel.cyclePrayerAlertMode(it)
                    },
                    onSilenceActiveAlert = {
                        viewModel.stopAudioPreview()
                    }
                )
            }

            composable(SalahDestination.PRAYER.route) {
                PrayerScreen(
                    state = uiState,
                    onMethodChange = { viewModel.setCalculationMethod(it) },
                    onMadhhabChange = { viewModel.setMadhhab(it) }
                )
            }

            composable(SalahDestination.QURAN.route) {
                QuranScreen(
                    state = uiState,
                    onSurahSelected = { viewModel.selectSurah(it) },
                    onAyahViewed = { viewModel.onAyahViewed(it) },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onTogglePlayAyah = { viewModel.togglePlayAyah(it) },
                    onStopAudio = { viewModel.stopAudio() },
                    onFontScaleChange = { viewModel.setQuranFontScale(it) },
                    onPageSelected = { viewModel.selectPage(it) },
                    onJuzSelected = { viewModel.selectJuz(it) },
                    onHizbSelected = { viewModel.selectHizb(it) }
                )
            }

            composable(SalahDestination.QIBLA.route) {
                QiblaScreen(
                    state = uiState,
                    onToggleTrueNorth = { viewModel.toggleTrueNorth() },
                    onFetchLocation = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            viewModel.fetchCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            composable(SalahDestination.SETTINGS.route) {
                SettingsScreen(
                    state = uiState,
                    onBack = null,
                    onLocationSelect = { viewModel.setLocation(it) },
                    onFetchLocation = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onMethodSelect = { viewModel.setCalculationMethod(it) },
                    onMadhhabSelect = { viewModel.setMadhhab(it) },
                    onAdjustmentsChange = { viewModel.setAdjustments(it) },
                    onAdhanToggle = { viewModel.setAdhanNotification(it) },
                    onPrePrayerToggle = { viewModel.setPrePrayerAlert(it) },
                    onVibrateOnlyToggle = { viewModel.setVibrateOnly(it) },
                    onGlobalSilentToggle = { viewModel.toggleGlobalSilentMode() },
                    onAutoSilentDuringPrayerToggle = { viewModel.toggleAutoSilentDuringPrayer() },
                    onAutoSilentDurationChange = { viewModel.setAutoSilentDuration(it) },
                    onLanguageSelect = { viewModel.setLanguage(it) },
                    onRiwayahSelect = { viewModel.setRiwayah(it) },
                    onThemeSelect = { viewModel.setAppTheme(it) },
                    onQuranScriptSelect = { viewModel.setQuranScript(it) },
                    onTimeFormatToggle = { viewModel.setTimeFormat24h(it) },
                    onAdhanSoundSelect = { viewModel.setAdhanSound(it) },
                    onHijriAdjustmentChange = { viewModel.setHijriAdjustment(it) },
                    onReciterSelect = { viewModel.setReciter(it) },
                    onFontScaleChange = { viewModel.setQuranFontScale(it) },
                    onRefreshClick = { viewModel.refreshData() },
                    onPrePrayerOffsetChange = { viewModel.setPrePrayerOffsetMinutes(it) },
                    onAdhanVolumeChange = { viewModel.setAdhanVolume(it) },
                    onPrayerAlertModeChange = { prayer, mode -> viewModel.setPrayerAlertMode(prayer, mode) },
                    onPlayAudioPreview = { viewModel.playAudioPreview(it) },
                    onStopAudioPreview = { viewModel.stopAudioPreview() },
                    onCustomLocationSave = { name, lat, lng, alt -> viewModel.setCustomLocation(name, lat, lng, alt) },
                    onTranslationSelect = { viewModel.setTranslationEdition(it) },
                    onRecomputeEphemerisCache = { viewModel.recomputeEphemerisCache() },
                    onClearAudioCache = { viewModel.clearAudioCache() },
                    onResetAllSettings = { viewModel.resetAllSettings() }
                )
            }
        }
    }
}
