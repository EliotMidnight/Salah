package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.PrayerLogEntity
import com.example.data.local.SalahDao
import com.example.data.location.AppLocationService
import com.example.data.location.LocationStore
import com.example.data.location.LocationFetchResult
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.service.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SalahRepository(
    private val dao: SalahDao,
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnlineFlow = MutableStateFlow(checkIsOnline())
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()


    private val _locationFlow = MutableStateFlow(loadLocation())
    val locationFlow: StateFlow<UserLocation> = _locationFlow.asStateFlow()

    private val _methodFlow = MutableStateFlow(loadMethod())
    val methodFlow: StateFlow<CalculationMethod> = _methodFlow.asStateFlow()

    private val _madhhabFlow = MutableStateFlow(loadMadhhab())
    val madhhabFlow: StateFlow<Madhhab> = _madhhabFlow.asStateFlow()

    private val _adjustmentsFlow = MutableStateFlow(loadAdjustments())
    val adjustmentsFlow: StateFlow<PrayerAdjustments> = _adjustmentsFlow.asStateFlow()

    private val _adhanNotificationFlow = MutableStateFlow(prefs.getBoolean("pref_adhan_notif", true))
    val adhanNotificationFlow: StateFlow<Boolean> = _adhanNotificationFlow.asStateFlow()

    private val _prePrayerAlertFlow = MutableStateFlow(prefs.getBoolean("pref_pre_prayer", true))
    val prePrayerAlertFlow: StateFlow<Boolean> = _prePrayerAlertFlow.asStateFlow()

    private val _vibrateOnlyFlow = MutableStateFlow(prefs.getBoolean("pref_vibrate_only", false))
    val vibrateOnlyFlow: StateFlow<Boolean> = _vibrateOnlyFlow.asStateFlow()

    private val _globalSilentFlow = MutableStateFlow(prefs.getBoolean("pref_global_silent", false))
    val globalSilentFlow: StateFlow<Boolean> = _globalSilentFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(prefs.getString("pref_language", "English") ?: "English")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(prefs.getString("pref_theme", "System Default") ?: "System Default")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    // Null means "no choice stored yet". The repository deliberately does not
    // name the default: that is `Accent.DEFAULT`, which lives with the theme.
    private val _accentFlow = MutableStateFlow(prefs.getString("pref_accent", null))
    val accentFlow: StateFlow<String?> = _accentFlow.asStateFlow()

    private val _reduceTransparencyFlow =
        MutableStateFlow(prefs.getBoolean("pref_reduce_transparency", false))
    val reduceTransparencyFlow: StateFlow<Boolean> = _reduceTransparencyFlow.asStateFlow()

    private val _timeFormat24hFlow = MutableStateFlow(prefs.getBoolean("pref_time_format_24h", true))
    val timeFormat24hFlow: StateFlow<Boolean> = _timeFormat24hFlow.asStateFlow()

    private val _hijriAdjustmentFlow = MutableStateFlow(prefs.getInt("pref_hijri_adj", 0))
    val hijriAdjustmentFlow: StateFlow<Int> = _hijriAdjustmentFlow.asStateFlow()

    private val _prePrayerOffsetFlow = MutableStateFlow(prefs.getInt("pref_pre_prayer_offset", 10))
    val prePrayerOffsetFlow: StateFlow<Int> = _prePrayerOffsetFlow.asStateFlow()

    private val _prayerAlertModesFlow = MutableStateFlow(loadPrayerAlertModes())
    val prayerAlertModesFlow: StateFlow<Map<Prayer, String>> = _prayerAlertModesFlow.asStateFlow()

    private fun loadPrayerAlertModes(): Map<Prayer, String> =
        Prayer.entries.associateWith { prayer ->
            // [Prayer.defaultAlertMode] - see there for why this is a property of
            // the prayer and not of the storage that records the choice.
            prefs.getString("pref_alert_mode_${prayer.name}", prayer.defaultAlertMode)
                ?: prayer.defaultAlertMode
        }

    val locationService = AppLocationService(context)

    /**
     * The stored location, read through [LocationStore].
     */
    private fun loadLocation(): UserLocation = LocationStore.read(context)

    fun saveLocation(location: UserLocation) {
        LocationStore.write(context, location).apply()
        _locationFlow.value = location
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    suspend fun fetchAndCacheLocation(): LocationFetchResult {
        val result = locationService.fetchCurrentCoordinates(fallbackLocation = _locationFlow.value)
        if (result is LocationFetchResult.Success) {
            saveLocation(result.location)
        }
        return result
    }

    private fun loadMethod(): CalculationMethod {
        val name = prefs.getString("calc_method", CalculationMethod.MOROCCO_MINISTRY.name)
        return try {
            CalculationMethod.valueOf(name ?: CalculationMethod.MOROCCO_MINISTRY.name)
        } catch (e: Exception) {
            CalculationMethod.MOROCCO_MINISTRY
        }
    }

    fun saveMethod(method: CalculationMethod) {
        prefs.edit().putString("calc_method", method.name).apply()
        _methodFlow.value = method
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    private fun loadMadhhab(): Madhhab {
        val name = prefs.getString("calc_madhhab", Madhhab.STANDARD.name)
        return try {
            Madhhab.valueOf(name ?: Madhhab.STANDARD.name)
        } catch (e: Exception) {
            Madhhab.STANDARD
        }
    }

    fun saveMadhhab(madhhab: Madhhab) {
        prefs.edit().putString("calc_madhhab", madhhab.name).apply()
        _madhhabFlow.value = madhhab
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    private fun loadAdjustments(): PrayerAdjustments {
        return PrayerAdjustments(
            fajr = prefs.getInt("adj_fajr", 0),
            sunrise = prefs.getInt("adj_sunrise", 0),
            dhuhr = prefs.getInt("adj_dhuhr", 0),
            asr = prefs.getInt("adj_asr", 0),
            maghrib = prefs.getInt("adj_maghrib", 0),
            isha = prefs.getInt("adj_isha", 0)
        )
    }

    fun saveAdjustments(adjustments: PrayerAdjustments) {
        prefs.edit()
            .putInt("adj_fajr", adjustments.fajr)
            .putInt("adj_sunrise", adjustments.sunrise)
            .putInt("adj_dhuhr", adjustments.dhuhr)
            .putInt("adj_asr", adjustments.asr)
            .putInt("adj_maghrib", adjustments.maghrib)
            .putInt("adj_isha", adjustments.isha)
            .apply()
        _adjustmentsFlow.value = adjustments
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    fun setAdhanNotification(enabled: Boolean) {
        prefs.edit().putBoolean("pref_adhan_notif", enabled).apply()
        _adhanNotificationFlow.value = enabled
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    fun setPrePrayerAlert(enabled: Boolean) {
        prefs.edit().putBoolean("pref_pre_prayer", enabled).apply()
        _prePrayerAlertFlow.value = enabled
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    fun setVibrateOnly(enabled: Boolean) {
        prefs.edit().putBoolean("pref_vibrate_only", enabled).apply()
        _vibrateOnlyFlow.value = enabled
    }

    fun setGlobalSilentMode(enabled: Boolean) {
        prefs.edit().putBoolean("pref_global_silent", enabled).apply()
        _globalSilentFlow.value = enabled
    }

    fun setPrePrayerOffset(offset: Int) {
        prefs.edit().putInt("pref_pre_prayer_offset", offset).apply()
        _prePrayerOffsetFlow.value = offset
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    fun setPrayerAlertMode(prayer: Prayer, mode: String) {
        prefs.edit().putString("pref_alert_mode_${prayer.name}", mode).apply()
        val updated = _prayerAlertModesFlow.value.toMutableMap()
        updated[prayer] = mode
        _prayerAlertModesFlow.value = updated
        PrayerAlarmScheduler.scheduleAllPrayers(context)
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("pref_language", language).apply()
        _languageFlow.value = language
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("pref_theme", theme).apply()
        _themeFlow.value = theme
    }

    fun setAccent(accentKey: String) {
        prefs.edit().putString("pref_accent", accentKey).apply()
        _accentFlow.value = accentKey
    }

    fun setReduceTransparency(enabled: Boolean) {
        prefs.edit().putBoolean("pref_reduce_transparency", enabled).apply()
        _reduceTransparencyFlow.value = enabled
    }

    fun setTimeFormat24h(is24h: Boolean) {
        prefs.edit().putBoolean("pref_time_format_24h", is24h).apply()
        _timeFormat24hFlow.value = is24h
    }

    fun setHijriAdjustment(adjustment: Int) {
        prefs.edit().putInt("pref_hijri_adj", adjustment).apply()
        _hijriAdjustmentFlow.value = adjustment
    }

    // Room DB delegated methods
    fun getPrayerLog(date: LocalDate): Flow<PrayerLogEntity?> {
        return dao.getPrayerLog(date.toString())
    }

    suspend fun savePrayerLog(log: PrayerLogEntity) {
        dao.insertOrUpdatePrayerLog(log)
    }

    init {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        // Reported, not acted on. There is nothing to fetch: prayer
                        // times are computed here.
                        _isOnlineFlow.value = true
                    }

                    override fun onLost(network: Network) {
                        _isOnlineFlow.value = false
                    }
                }
            )
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    fun checkIsOnline(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Whether the device currently has a connection.
     *
     * **It fetches nothing, and nothing here ever did.** This was called
     * `refreshOnlineDataSync` and its entire body was to write one of two
     * sentences into [lastCheckedFlow] - "Online · Synced at 14:32 (Verified
     * Ephemeris)" or "Offline · Calculated on-device (14:32)" - having contacted
     * no server. The app has no HTTP client; `grep` for one returns nothing.
     */
    fun refreshConnectivity() {
        _isOnlineFlow.value = checkIsOnline()
    }
}
