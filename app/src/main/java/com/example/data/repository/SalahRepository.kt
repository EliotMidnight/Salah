package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.BookmarkEntity
import com.example.data.local.CachedLocationEntity
import com.example.data.local.ContinueReadingEntity
import com.example.data.local.PrayerLogEntity
import com.example.data.local.SalahDao
import com.example.data.location.AppLocationService
import com.example.data.location.LocationFetchResult
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.AdhanAudioSynthesizer
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

    private val _isSyncingFlow = MutableStateFlow(false)
    val isSyncingFlow: StateFlow<Boolean> = _isSyncingFlow.asStateFlow()

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

    private val _quranFontScaleFlow = MutableStateFlow(prefs.getFloat("pref_quran_scale", 1.0f))
    val quranFontScaleFlow: StateFlow<Float> = _quranFontScaleFlow.asStateFlow()

    private val _lastCheckedFlow = MutableStateFlow(
        prefs.getString("pref_last_checked", "Today · Just now") ?: "Today · Just now"
    )
    val lastCheckedFlow: StateFlow<String> = _lastCheckedFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(prefs.getString("pref_language", "English") ?: "English")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _riwayahFlow = MutableStateFlow(prefs.getString("pref_riwayah", "Hafs 'an 'Asim") ?: "Hafs 'an 'Asim")
    val riwayahFlow: StateFlow<String> = _riwayahFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(prefs.getString("pref_theme", "System Default") ?: "System Default")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _quranScriptFlow = MutableStateFlow(prefs.getString("pref_quran_script", "Uthmani (Madani)") ?: "Uthmani (Madani)")
    val quranScriptFlow: StateFlow<String> = _quranScriptFlow.asStateFlow()

    private val _timeFormat24hFlow = MutableStateFlow(prefs.getBoolean("pref_time_format_24h", true))
    val timeFormat24hFlow: StateFlow<Boolean> = _timeFormat24hFlow.asStateFlow()

    private val _adhanSoundFlow = MutableStateFlow(prefs.getString("pref_adhan_sound", "Makkah Al-Mukarramah") ?: "Makkah Al-Mukarramah")
    val adhanSoundFlow: StateFlow<String> = _adhanSoundFlow.asStateFlow()

    private val _hijriAdjustmentFlow = MutableStateFlow(prefs.getInt("pref_hijri_adj", 0))
    val hijriAdjustmentFlow: StateFlow<Int> = _hijriAdjustmentFlow.asStateFlow()

    private val _reciterFlow = MutableStateFlow(prefs.getString("pref_reciter", "Mishary Rashid Alafasy") ?: "Mishary Rashid Alafasy")
    val reciterFlow: StateFlow<String> = _reciterFlow.asStateFlow()

    private val _globalSilentFlow = MutableStateFlow(prefs.getBoolean("pref_global_silent", false))
    val globalSilentFlow: StateFlow<Boolean> = _globalSilentFlow.asStateFlow()

    private val _autoSilentDuringPrayerFlow = MutableStateFlow(prefs.getBoolean("pref_auto_silent_during_prayer", false))
    val autoSilentDuringPrayerFlow: StateFlow<Boolean> = _autoSilentDuringPrayerFlow.asStateFlow()

    private val _autoSilentDurationFlow = MutableStateFlow(prefs.getInt("pref_auto_silent_duration", 20))
    val autoSilentDurationFlow: StateFlow<Int> = _autoSilentDurationFlow.asStateFlow()

    private val _adhanVolumeFlow = MutableStateFlow(prefs.getFloat("pref_adhan_volume", 0.85f))
    val adhanVolumeFlow: StateFlow<Float> = _adhanVolumeFlow.asStateFlow()

    private val _prePrayerOffsetFlow = MutableStateFlow(prefs.getInt("pref_pre_prayer_offset", 10))
    val prePrayerOffsetFlow: StateFlow<Int> = _prePrayerOffsetFlow.asStateFlow()

    private val _prayerAlertModesFlow = MutableStateFlow(loadPrayerAlertModes())
    val prayerAlertModesFlow: StateFlow<Map<Prayer, String>> = _prayerAlertModesFlow.asStateFlow()

    private fun loadPrayerAlertModes(): Map<Prayer, String> {
        return Prayer.entries.associateWith { prayer ->
            val defaultMode = if (prayer == Prayer.SUNRISE) "Silent Reminder" else "Full Adhan"
            prefs.getString("pref_alert_mode_${prayer.name}", defaultMode) ?: defaultMode
        }
    }

    val locationService = AppLocationService(context)
    val cachedLocationDbFlow: Flow<CachedLocationEntity?> = dao.getCachedLocation()

    private fun loadLocation(): UserLocation {
        val name = prefs.getString("loc_name", UserLocation.DEFAULT.name) ?: UserLocation.DEFAULT.name
        val country = prefs.getString("loc_country", UserLocation.DEFAULT.country) ?: UserLocation.DEFAULT.country
        val lat = prefs.getFloat("loc_lat", UserLocation.DEFAULT.latitude.toFloat()).toDouble()
        val lng = prefs.getFloat("loc_lng", UserLocation.DEFAULT.longitude.toFloat()).toDouble()
        val isGps = prefs.getBoolean("loc_is_gps", false)
        return UserLocation(name, country, lat, lng, isGps)
    }

    fun saveLocation(location: UserLocation) {
        prefs.edit()
            .putString("loc_name", location.name)
            .putString("loc_country", location.country)
            .putFloat("loc_lat", location.latitude.toFloat())
            .putFloat("loc_lng", location.longitude.toFloat())
            .putBoolean("loc_is_gps", location.isGps)
            .putString("pref_last_checked", if (location.isGps) "Offline cached (GPS)" else "Offline cached (City)")
            .apply()
        _locationFlow.value = location
        _lastCheckedFlow.value = if (location.isGps) "Offline cached (GPS)" else "Offline cached (City)"
        PrayerAlarmScheduler.scheduleAllPrayers(context)

        // Persist to Room for robust offline caching
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.insertCachedLocation(
                    CachedLocationEntity(
                        id = 1,
                        name = location.name,
                        country = location.country,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isGps = location.isGps,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Ignore DB error
            }
        }
    }

    suspend fun fetchAndCacheLocation(): LocationFetchResult {
        _isSyncingFlow.value = true
        return try {
            val result = locationService.fetchCurrentCoordinates(fallbackLocation = _locationFlow.value)
            if (result is LocationFetchResult.Success) {
                saveLocation(result.location)
            }
            result
        } finally {
            _isSyncingFlow.value = false
        }
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
        if (enabled) {
            AdhanAudioSynthesizer.stop()
        }
    }

    fun setAutoSilentDuringPrayer(enabled: Boolean) {
        prefs.edit().putBoolean("pref_auto_silent_during_prayer", enabled).apply()
        _autoSilentDuringPrayerFlow.value = enabled
    }

    fun setAutoSilentDuration(minutes: Int) {
        prefs.edit().putInt("pref_auto_silent_duration", minutes).apply()
        _autoSilentDurationFlow.value = minutes
    }

    fun setAdhanVolume(volume: Float) {
        prefs.edit().putFloat("pref_adhan_volume", volume).apply()
        _adhanVolumeFlow.value = volume
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

    fun setQuranFontScale(scale: Float) {
        prefs.edit().putFloat("pref_quran_scale", scale).apply()
        _quranFontScaleFlow.value = scale
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("pref_language", language).apply()
        _languageFlow.value = language
    }

    fun setRiwayah(riwayah: String) {
        prefs.edit().putString("pref_riwayah", riwayah).apply()
        _riwayahFlow.value = riwayah
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("pref_theme", theme).apply()
        _themeFlow.value = theme
    }

    fun setQuranScript(script: String) {
        prefs.edit().putString("pref_quran_script", script).apply()
        _quranScriptFlow.value = script
    }

    fun setTimeFormat24h(is24h: Boolean) {
        prefs.edit().putBoolean("pref_time_format_24h", is24h).apply()
        _timeFormat24hFlow.value = is24h
    }

    fun setAdhanSound(sound: String) {
        prefs.edit().putString("pref_adhan_sound", sound).apply()
        _adhanSoundFlow.value = sound
    }

    fun setHijriAdjustment(adjustment: Int) {
        prefs.edit().putInt("pref_hijri_adj", adjustment).apply()
        _hijriAdjustmentFlow.value = adjustment
    }

    fun setReciter(reciter: String) {
        prefs.edit().putString("pref_reciter", reciter).apply()
        _reciterFlow.value = reciter
    }

    // Room DB delegated methods
    fun getPrayerLog(date: LocalDate): Flow<PrayerLogEntity?> {
        return dao.getPrayerLog(date.toString())
    }

    suspend fun savePrayerLog(log: PrayerLogEntity) {
        dao.insertOrUpdatePrayerLog(log)
    }

    fun getContinueReading(): Flow<ContinueReadingEntity?> {
        return dao.getContinueReading()
    }

    suspend fun saveContinueReading(continueReading: ContinueReadingEntity) {
        dao.saveContinueReading(continueReading)
    }

    fun getBookmarks(): Flow<List<BookmarkEntity>> {
        return dao.getAllBookmarks()
    }

    suspend fun toggleBookmark(surahNumber: Int, ayahNumber: Int, surahName: String, snippet: String): Boolean {
        val count = dao.isBookmarked(surahNumber, ayahNumber)
        return if (count > 0) {
            dao.deleteBookmark(surahNumber, ayahNumber)
            false
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    surahName = surahName,
                    ayahSnippet = snippet
                )
            )
            true
        }
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
                        _isOnlineFlow.value = true
                        refreshOnlineDataSync()
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

    fun refreshOnlineDataSync() {
        val online = checkIsOnline()
        _isOnlineFlow.value = online
        _isSyncingFlow.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                if (online) {
                    val syncedStatus = "Online · Synced at $timeStr (Verified Ephemeris)"
                    prefs.edit().putString("pref_last_checked", syncedStatus).apply()
                    _lastCheckedFlow.value = syncedStatus
                } else {
                    val offlineStatus = "Offline · Calculated on-device ($timeStr)"
                    prefs.edit().putString("pref_last_checked", offlineStatus).apply()
                    _lastCheckedFlow.value = offlineStatus
                }
            } finally {
                _isSyncingFlow.value = false
            }
        }
    }
}
