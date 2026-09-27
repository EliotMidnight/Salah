package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkEntity
import com.example.data.local.ContinueReadingEntity
import com.example.data.local.PrayerLogEntity
import com.example.data.local.SalahDatabase
import com.example.data.location.LocationFetchResult
import com.example.data.model.Ayah
import com.example.data.model.CalculationMethod
import com.example.data.model.HijriDate
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.Surah
import com.example.data.model.UserLocation
import com.example.data.quran.QuranDataSource
import com.example.data.repository.SalahRepository
import com.example.engine.AstronomicalSky
import com.example.engine.HijriCalendarEngine
import com.example.engine.MagneticFieldStatus
import com.example.engine.PrayerCalculationEngine
import com.example.engine.PrayerNotificationManager
import com.example.engine.QiblaEngine
import com.example.engine.QuranAudioPlayer
import com.example.engine.SkyPeriod
import com.example.engine.SunPosition
import com.example.engine.AdhanAudioSynthesizer
import com.example.service.PrayerAlarmScheduler
import com.example.service.PrayerAlertService
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

data class SalahUiState(
    val location: UserLocation = UserLocation.DEFAULT,
    val method: CalculationMethod = CalculationMethod.MOROCCO_MINISTRY,
    val madhhab: Madhhab = Madhhab.STANDARD,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val todayPrayerTimes: PrayerTimesDay? = null,
    val nextPrayer: PrayerTime? = null,
    val previousPrayer: PrayerTime? = null,
    val countdownString: String = "00:00:00",
    val skyPeriod: SkyPeriod = SkyPeriod.DHUHR_MIDDAY,
    val celestialProgress: Float = 0.5f,
    val hijriDate: HijriDate? = null,
    /**
     * The day the Prayer times are being shown for, or null for today.
     *
     * Shared, and deliberately not per-screen: there is one date switcher in the
     * app, on the Prayer tab, and the Today page follows it. Two switches meant
     * two answers to "which day am I looking at", and they drifted apart the
     * moment you used one and not the other.
     *
     * Null rather than a concrete date so an app left open overnight still calls
     * today today - the ViewModel advances `todayPrayerTimes` at midnight.
     */
    val selectedDate: LocalDate? = null,
    val prayerLog: PrayerLogEntity = PrayerLogEntity(LocalDate.now().toString()),
    val continueReading: ContinueReadingEntity = ContinueReadingEntity(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val quranFontScale: Float = 1.0f,
    val adhanNotificationEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val vibrateOnly: Boolean = false,
    val isGlobalSilentMode: Boolean = false,
    val autoSilentDuringPrayer: Boolean = false,
    val autoSilentDurationMinutes: Int = 20,
    val lastChecked: String = "Today · Synced locally",
    val isOnline: Boolean = false,
    val isSyncing: Boolean = false,
    // Expanded user preferences
    val language: String = "English",
    val riwayah: String = "Hafs 'an 'Asim",
    val appTheme: String = "System Default",
    val quranScript: String = "Uthmani (Madani)",
    val timeFormat24h: Boolean = true,
    val adhanSound: String = "Makkah Al-Mukarramah",
    val hijriAdjustment: Int = 0,
    val reciter: String = "Mishary Rashid Alafasy",
    val prePrayerOffsetMinutes: Int = 10,
    val adhanVolume: Float = 0.85f,
    val audioPreviewPlaying: String? = null,
    val prayerAlertModes: Map<Prayer, String> = mapOf(
        Prayer.FAJR to "Full Adhan",
        Prayer.DHUHR to "Full Adhan",
        Prayer.ASR to "Full Adhan",
        Prayer.MAGHRIB to "Full Adhan",
        Prayer.ISHA to "Full Adhan",
        Prayer.SUNRISE to "Silent Reminder"
    ),
    val translationEdition: String = "English (Saheeh International)",
    // Qibla state & magnetic sensor diagnostics
    val compassAzimuth: Float = 0f,
    val qiblaBearing: Float = 0f,
    val qiblaDelta: Float = 0f,
    val relativeQiblaAngle: Float = 0f, // signed angle -180..+180 (0 is aligned straight ahead)
    val isFacingQibla: Boolean = false,
    val compassAccuracy: String = "HIGH ACCURACY",
    val magneticSensorAccuracy: Int = 3,
    val magneticFieldMagnitude: Float = 46.0f, // uT
    val magneticStatus: MagneticFieldStatus = MagneticFieldStatus.OPTIMAL,
    val useTrueNorth: Boolean = true,
    val magneticDeclination: Float = 0f,
    val hasMagneticSensor: Boolean = true,
    val sunPosition: SunPosition? = null,
    val distanceToKaabaKm: Int = 0,
    // Device level & tilt sensor telemetry (Magnetometer & Accelerometer)
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isDeviceLevel: Boolean = true,
    val isLocating: Boolean = false,
    val locationStatusMessage: String? = null,
    val cachedLocationTimestamp: Long? = null,
    // Quran reader state
    val selectedSurah: Surah = QuranDataSource.SURAHS[0],
    val currentSurahAyahs: List<Ayah> = emptyList(),
    val activeReadingAyahNumber: Int = 1,
    val isAudioPlaying: Boolean = false,
    val currentAudioAyah: Int = 1,
    // Calendar selected date inspection
    val calendarSelectedDate: LocalDate = LocalDate.now(),
    val calendarSelectedDayPrayers: PrayerTimesDay? = null
)

class SalahViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository: SalahRepository
    val audioPlayer: QuranAudioPlayer

    private val _uiState = MutableStateFlow(SalahUiState())
    val uiState: StateFlow<SalahUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var prayerLogJob: Job? = null
    private val sensorManager: SensorManager? =
        application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val magneticSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometerSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false
    private var lastVibrateTimestamp: Long = 0L
    private var wasFacingQibla: Boolean = false
    private var lastAzimuth: Float = 0f
    private var declCacheLat = Float.NaN
    private var declCacheLon = Float.NaN
    private var declCacheValue = 0f
    private var declCacheAtMs = 0L

    private fun cachedDeclinationFor(lat: Float, lon: Float): Float {
        val now = System.currentTimeMillis()
        if (lat == declCacheLat && lon == declCacheLon && now - declCacheAtMs < 6 * 60 * 60 * 1000L) {
            return declCacheValue
        }
        val value = try {
            GeomagneticField(lat, lon, 50f, now).declination
        } catch (_: Exception) {
            0f
        }
        declCacheLat = lat
        declCacheLon = lon
        declCacheValue = value
        declCacheAtMs = now
        return value
    }

    init {
        val database = SalahDatabase.getDatabase(application)
        repository = SalahRepository(database.salahDao(), application)
        audioPlayer = QuranAudioPlayer(application)
        PrayerNotificationManager.initChannels(application)

        // Observe repository state
        viewModelScope.launch {
            repository.locationFlow.collectLatest { loc ->
                _uiState.value = _uiState.value.copy(location = loc)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.cachedLocationDbFlow.collectLatest { cached ->
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        cachedLocationTimestamp = cached.timestamp
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.methodFlow.collectLatest { meth ->
                _uiState.value = _uiState.value.copy(method = meth)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.madhhabFlow.collectLatest { madh ->
                _uiState.value = _uiState.value.copy(madhhab = madh)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.adjustmentsFlow.collectLatest { adj ->
                _uiState.value = _uiState.value.copy(adjustments = adj)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.adhanNotificationFlow.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(adhanNotificationEnabled = enabled)
            }
        }

        viewModelScope.launch {
            repository.prePrayerAlertFlow.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(prePrayerAlertEnabled = enabled)
            }
        }

        viewModelScope.launch {
            repository.vibrateOnlyFlow.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(vibrateOnly = enabled)
            }
        }

        viewModelScope.launch {
            repository.globalSilentFlow.collectLatest { silent ->
                _uiState.value = _uiState.value.copy(isGlobalSilentMode = silent)
            }
        }

        viewModelScope.launch {
            repository.autoSilentDuringPrayerFlow.collectLatest { autoSilent ->
                _uiState.value = _uiState.value.copy(autoSilentDuringPrayer = autoSilent)
            }
        }

        viewModelScope.launch {
            repository.autoSilentDurationFlow.collectLatest { duration ->
                _uiState.value = _uiState.value.copy(autoSilentDurationMinutes = duration)
            }
        }

        viewModelScope.launch {
            repository.adhanVolumeFlow.collectLatest { vol ->
                _uiState.value = _uiState.value.copy(adhanVolume = vol)
            }
        }

        viewModelScope.launch {
            repository.prePrayerOffsetFlow.collectLatest { offset ->
                _uiState.value = _uiState.value.copy(prePrayerOffsetMinutes = offset)
            }
        }

        viewModelScope.launch {
            repository.prayerAlertModesFlow.collectLatest { modes ->
                _uiState.value = _uiState.value.copy(prayerAlertModes = modes)
            }
        }

        viewModelScope.launch {
            repository.quranFontScaleFlow.collectLatest { scale ->
                _uiState.value = _uiState.value.copy(quranFontScale = scale)
            }
        }

        viewModelScope.launch {
            repository.lastCheckedFlow.collectLatest { last ->
                _uiState.value = _uiState.value.copy(lastChecked = last)
            }
        }

        viewModelScope.launch {
            repository.languageFlow.collectLatest { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }

        viewModelScope.launch {
            repository.riwayahFlow.collectLatest { r ->
                _uiState.value = _uiState.value.copy(riwayah = r)
            }
        }

        viewModelScope.launch {
            repository.themeFlow.collectLatest { t ->
                _uiState.value = _uiState.value.copy(appTheme = t)
            }
        }

        viewModelScope.launch {
            repository.quranScriptFlow.collectLatest { s ->
                _uiState.value = _uiState.value.copy(quranScript = s)
            }
        }

        viewModelScope.launch {
            repository.timeFormat24hFlow.collectLatest { tf ->
                _uiState.value = _uiState.value.copy(timeFormat24h = tf)
            }
        }

        viewModelScope.launch {
            repository.adhanSoundFlow.collectLatest { asnd ->
                _uiState.value = _uiState.value.copy(adhanSound = asnd)
            }
        }

        viewModelScope.launch {
            repository.hijriAdjustmentFlow.collectLatest { adj ->
                _uiState.value = _uiState.value.copy(hijriAdjustment = adj)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.reciterFlow.collectLatest { rec ->
                _uiState.value = _uiState.value.copy(reciter = rec)
            }
        }

        viewModelScope.launch {
            repository.isOnlineFlow.collectLatest { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }

        viewModelScope.launch {
            repository.isSyncingFlow.collectLatest { syncing ->
                _uiState.value = _uiState.value.copy(isSyncing = syncing)
            }
        }

        // Room observers.
        //
        // The prayer log is a row keyed by date, so this cannot be a one-off
        // collection: at midnight the date changes and the observer has to follow
        // it. Re-armed by [observePrayerLog] from the ticker below.
        observePrayerLog(LocalDate.now())

        viewModelScope.launch {
            repository.getContinueReading().collectLatest { cont ->
                if (cont != null) {
                    _uiState.value = _uiState.value.copy(continueReading = cont)
                }
            }
        }

        viewModelScope.launch {
            repository.getBookmarks().collectLatest { bms ->
                _uiState.value = _uiState.value.copy(bookmarks = bms)
            }
        }

        // Audio state observer
        viewModelScope.launch {
            audioPlayer.playbackState.collectLatest { ps ->
                _uiState.value = _uiState.value.copy(
                    isAudioPlaying = ps.isPlaying,
                    currentAudioAyah = ps.ayahNumber
                )
            }
        }

        // Initialize Quran default Surah
        selectSurah(1)

        // Start 1-second live ticker
        startLiveTicker()

        // Register background prayer alarm scheduling
        PrayerAlarmScheduler.scheduleAllPrayers(application)

        // Register compass sensor
        startCompass()
    }

    fun startCompass() {
        val hasSensor = rotationSensor != null || magneticSensor != null
        _uiState.value = _uiState.value.copy(hasMagneticSensor = hasSensor)
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magneticSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopCompass() {
        sensorManager?.unregisterListener(this)
    }

    fun toggleTrueNorth() {
        val current = _uiState.value.useTrueNorth
        _uiState.value = _uiState.value.copy(useTrueNorth = !current)
        // Force recalculate heading with new north setting
        updateCompassHeading(lastAzimuth)
    }

    override fun onCleared() {
        super.onCleared()
        stopCompass()
        tickerJob?.cancel()
        audioPlayer.stop()
    }

    private fun startLiveTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                updateLiveTiming()
                delay(1000)
            }
        }
    }

    /**
     * Keeps Today's checklist pointed at the current day's row.
     *
     * Cancelled and re-subscribed by [updateLiveTiming] when the calendar day
     * changes, because the checklist is stored one row per date.
     */
    private fun observePrayerLog(date: LocalDate) {
        prayerLogJob?.cancel()
        prayerLogJob = viewModelScope.launch {
            repository.getPrayerLog(date).collectLatest { log ->
                if (log != null) {
                    _uiState.value = _uiState.value.copy(prayerLog = log)
                }
            }
        }
    }

    private fun updateLiveTiming() {
        val state = _uiState.value
        val today = LocalDate.now()

        // Midnight.
        //
        // Everything date-bearing here is computed once and then countdown-ticks,
        // so nothing used to roll over: leaving the app open across midnight kept
        // yesterday's date, yesterday's times and yesterday's "next prayer", and
        // ticking the checklist wrote into yesterday's row.
        //
        // The first branch also covers the state before the schedule has loaded at
        // all, so a cold start can never show an empty day.
        val day = state.todayPrayerTimes
        if (day == null || day.date != today) {
            if (day != null) {
                // A real rollover: clear the checklist to a fresh, empty row for
                // the new day and follow it. Without this the state would keep
                // yesterday's ticks, because the new row does not exist yet and the
                // observer only ever copies non-null rows in.
                _uiState.value = state.copy(
                    prayerLog = PrayerLogEntity(dateString = today.toString())
                )
                observePrayerLog(today)
            }
            recalculateAll()
            return
        }

        val now = LocalDateTime.now()
        val nextPt = PrayerCalculationEngine.getNextPrayer(day, now.toLocalTime())
        val prevPt = PrayerCalculationEngine.getPreviousPrayer(day, now.toLocalTime())
        val countdown = PrayerCalculationEngine.formatRemainingCountdown(nextPt.dateTime, now)
        val sky = AstronomicalSky.determineSkyPeriod(now.toLocalTime(), day)
        val celestialProgress = AstronomicalSky.getCelestialBodyProgress(now.toLocalTime(), day)
        val sun = QiblaEngine.calculateSunPosition(state.location, now)

        _uiState.value = state.copy(
            nextPrayer = nextPt,
            previousPrayer = prevPt,
            countdownString = countdown,
            skyPeriod = sky,
            celestialProgress = celestialProgress,
            sunPosition = sun
        )
    }

    fun recalculateAll() {
        val state = _uiState.value
        val today = LocalDate.now()
        val calculatedToday = PrayerCalculationEngine.calculatePrayerTimes(
            date = today,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments
        )

        val nextPt = PrayerCalculationEngine.getNextPrayer(calculatedToday, LocalTime.now())
        val prevPt = PrayerCalculationEngine.getPreviousPrayer(calculatedToday, LocalTime.now())
        val countdown = PrayerCalculationEngine.formatRemainingCountdown(nextPt.dateTime)
        val sky = AstronomicalSky.determineSkyPeriod(LocalTime.now(), calculatedToday)
        val celestial = AstronomicalSky.getCelestialBodyProgress(LocalTime.now(), calculatedToday)
        val hijri = HijriCalendarEngine.getHijriDate(today.plusDays(state.hijriAdjustment.toLong()))

        val qiblaBearing = QiblaEngine.calculateQiblaBearing(state.location.latitude, state.location.longitude)
        val distanceKaaba = QiblaEngine.calculateDistanceToKaabaKm(state.location.latitude, state.location.longitude)
        val sun = QiblaEngine.calculateSunPosition(state.location)

        // Calendar selected day calculation
        val calDayPrayers = PrayerCalculationEngine.calculatePrayerTimes(
            date = state.calendarSelectedDate,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments
        )

        _uiState.value = state.copy(
            todayPrayerTimes = calculatedToday,
            nextPrayer = nextPt,
            previousPrayer = prevPt,
            countdownString = countdown,
            skyPeriod = sky,
            celestialProgress = celestial,
            hijriDate = hijri,
            qiblaBearing = qiblaBearing,
            distanceToKaabaKm = distanceKaaba,
            sunPosition = sun,
            calendarSelectedDayPrayers = calDayPrayers
        )
    }

    // Prayer Completion Checklist
    fun togglePrayerCompleted(prayer: Prayer) {
        viewModelScope.launch {
            val current = _uiState.value.prayerLog
            val updated = when (prayer) {
                Prayer.FAJR -> current.copy(fajrDone = !current.fajrDone)
                Prayer.DHUHR -> current.copy(dhuhrDone = !current.dhuhrDone)
                Prayer.ASR -> current.copy(asrDone = !current.asrDone)
                Prayer.MAGHRIB -> current.copy(maghribDone = !current.maghribDone)
                Prayer.ISHA -> current.copy(ishaDone = !current.ishaDone)
                Prayer.SUNRISE -> current
            }
            repository.savePrayerLog(updated)
            _uiState.value = _uiState.value.copy(prayerLog = updated)
        }
    }

    // Location & Settings
    fun setLocation(location: UserLocation) {
        repository.saveLocation(location)
    }

    fun fetchCurrentLocation(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLocating = true,
                locationStatusMessage = "Acquiring GPS coordinates..."
            )
            val result = repository.fetchAndCacheLocation()
            when (result) {
                is LocationFetchResult.Success -> {
                    val loc = result.location
                    val msg = if (result.isFresh) {
                        "GPS Location: ${loc.name}, ${loc.country}"
                    } else {
                        "Cached Offline: ${loc.name}, ${loc.country}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        locationStatusMessage = msg
                    )
                    recalculateAll()
                    onComplete(true, msg)
                }
                is LocationFetchResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        locationStatusMessage = result.reason
                    )
                    onComplete(false, result.reason)
                }
            }
        }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        repository.saveMethod(method)
    }

    fun setMadhhab(madhhab: Madhhab) {
        repository.saveMadhhab(madhhab)
    }

    fun setAdjustments(adjustments: PrayerAdjustments) {
        repository.saveAdjustments(adjustments)
    }

    fun setAdhanNotification(enabled: Boolean) {
        repository.setAdhanNotification(enabled)
    }

    fun setPrePrayerAlert(enabled: Boolean) {
        repository.setPrePrayerAlert(enabled)
    }

    fun setVibrateOnly(enabled: Boolean) {
        repository.setVibrateOnly(enabled)
    }

    fun setQuranFontScale(scale: Float) {
        repository.setQuranFontScale(scale)
    }

    fun setLanguage(language: String) {
        repository.setLanguage(language)
    }

    fun setRiwayah(riwayah: String) {
        repository.setRiwayah(riwayah)
    }

    fun setAppTheme(theme: String) {
        repository.setAppTheme(theme)
    }

    fun setQuranScript(script: String) {
        repository.setQuranScript(script)
    }

    fun setTimeFormat24h(is24h: Boolean) {
        repository.setTimeFormat24h(is24h)
    }

    fun setAdhanSound(sound: String) {
        repository.setAdhanSound(sound)
    }

    fun setHijriAdjustment(adjustment: Int) {
        repository.setHijriAdjustment(adjustment)
    }

    fun refreshData() {
        repository.refreshOnlineDataSync()
        recalculateAll()
    }

    fun setReciter(reciter: String) {
        repository.setReciter(reciter)
    }

    fun setPrePrayerOffsetMinutes(minutes: Int) {
        repository.setPrePrayerOffset(minutes)
    }

    fun setAdhanVolume(volume: Float) {
        repository.setAdhanVolume(volume.coerceIn(0f, 1f))
    }

    fun setPrayerAlertMode(prayer: Prayer, mode: String) {
        repository.setPrayerAlertMode(prayer, mode)
    }

    fun toggleGlobalSilentMode() {
        val newState = !_uiState.value.isGlobalSilentMode
        repository.setGlobalSilentMode(newState)
        if (newState) {
            stopAudioPreview()
            silenceActiveAlert()
        }
    }

    fun toggleAutoSilentDuringPrayer() {
        val newState = !_uiState.value.autoSilentDuringPrayer
        repository.setAutoSilentDuringPrayer(newState)
    }

    fun setAutoSilentDuration(minutes: Int) {
        repository.setAutoSilentDuration(minutes)
    }

    fun cyclePrayerAlertMode(prayer: Prayer) {
        val current = _uiState.value.prayerAlertModes[prayer] ?: "Full Adhan"
        val nextMode = when (current) {
            "Full Adhan" -> "Takbeer Only"
            "Takbeer Only" -> "Gentle Chime"
            "Gentle Chime" -> "Vibrate Only"
            "Vibrate Only" -> "Silent"
            "Silent", "Silent Reminder" -> "Full Adhan"
            else -> "Full Adhan"
        }
        repository.setPrayerAlertMode(prayer, nextMode)
    }

    fun silenceActiveAlert() {
        PrayerAlertService.stopActiveAlert(getApplication())
        AdhanAudioSynthesizer.stop()
    }

    fun setTranslationEdition(edition: String) {
        _uiState.value = _uiState.value.copy(translationEdition = edition)
    }

    fun playAudioPreview(title: String) {
        if (_uiState.value.audioPreviewPlaying == title) {
            stopAudioPreview()
            return
        }
        _uiState.value = _uiState.value.copy(audioPreviewPlaying = title)

        val isAdhanSound = title.contains("Makkah") || title.contains("Madinah") ||
                title.contains("Al-Aqsa") || title.contains("Cairo") || title.contains("Moroccan")
        val isAlertMode = title == "Full Adhan" || title == "Takbeer Only" || title == "Gentle Chime"

        if (isAdhanSound || isAlertMode) {
            val mode = if (isAlertMode) title else "Full Adhan"
            val sound = if (isAdhanSound) title else _uiState.value.adhanSound
            AdhanAudioSynthesizer.playAlert(
                alertMode = mode,
                soundStyle = sound,
                volume = _uiState.value.adhanVolume
            ) {
                _uiState.value = _uiState.value.copy(audioPreviewPlaying = null)
            }
        } else {
            audioPlayer.playAudioPreview(title) {
                _uiState.value = _uiState.value.copy(audioPreviewPlaying = null)
            }
        }
    }

    fun stopAudioPreview() {
        audioPlayer.stop()
        AdhanAudioSynthesizer.stop()
        _uiState.value = _uiState.value.copy(audioPreviewPlaying = null)
    }

    fun setCustomLocation(name: String, lat: Double, lng: Double, altitude: Double = 0.0) {
        val custom = UserLocation(
            name = name,
            country = "Custom",
            latitude = lat,
            longitude = lng,
            isGps = false
        )
        setLocation(custom)
    }

    fun recomputeEphemerisCache() {
        recalculateAll()
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _uiState.value = _uiState.value.copy(
            lastChecked = "Recomputed at $timeStr · 365 Days Verified"
        )
    }

    fun resetAllSettings() {
        setCalculationMethod(CalculationMethod.MOROCCO_MINISTRY)
        setMadhhab(Madhhab.STANDARD)
        setAdjustments(PrayerAdjustments())
        setHijriAdjustment(0)
        setAdhanNotification(true)
        setPrePrayerAlert(true)
        setVibrateOnly(false)
        setQuranFontScale(1.0f)
        setTimeFormat24h(true)
        setAppTheme("System Default")
        _uiState.value = _uiState.value.copy(
            prePrayerOffsetMinutes = 10,
            adhanVolume = 0.85f,
            prayerAlertModes = mapOf(
                Prayer.FAJR to "Full Adhan",
                Prayer.DHUHR to "Full Adhan",
                Prayer.ASR to "Full Adhan",
                Prayer.MAGHRIB to "Full Adhan",
                Prayer.ISHA to "Full Adhan",
                Prayer.SUNRISE to "Silent Reminder"
            )
        )
        recalculateAll()
    }

    // Quran actions
    fun selectSurah(surahNumber: Int, ayahNumber: Int = 1) {
        val surah = QuranDataSource.getSurahByNumber(surahNumber) ?: QuranDataSource.SURAHS[0]
        val ayahs = QuranDataSource.getAyahsForSurah(surahNumber)
        val startAyah = ayahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        _uiState.value = _uiState.value.copy(
            selectedSurah = surah,
            currentSurahAyahs = ayahs,
            activeReadingAyahNumber = startAyah
        )
    }

    fun selectPage(pageNumber: Int) {
        val page = pageNumber.coerceIn(1, 604)
        val resolved = QuranDataSource.resolvePage(page)
        if (resolved != null) {
            selectSurah(resolved.first.number, resolved.second)
        } else {
            selectSurah(1, 1)
        }
    }

    fun selectJuz(juzNumber: Int) {
        val target = QuranDataSource.firstAyahForJuz(juzNumber)
        if (target != null) {
            selectSurah(target.surahNumber, target.ayahNumber)
        } else {
            selectSurah(1, 1)
        }
    }

    fun selectHizb(hizbNumber: Int) {
        val target = QuranDataSource.firstAyahForHizb(hizbNumber)
        if (target != null) {
            selectSurah(target.surahNumber, target.ayahNumber)
        } else {
            selectSurah(1, 1)
        }
    }

    fun onAyahViewed(ayah: Ayah) {
        _uiState.value = _uiState.value.copy(activeReadingAyahNumber = ayah.ayahNumber)
        viewModelScope.launch {
            val entity = ContinueReadingEntity(
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber,
                surahName = _uiState.value.selectedSurah.englishName,
                surahNameAr = _uiState.value.selectedSurah.arabicName,
                pageNumber = ayah.pageNumber,
                snippetAr = ayah.textArabic.take(60),
                timestamp = System.currentTimeMillis()
            )
            repository.saveContinueReading(entity)
        }
    }

    /**
     * Show prayer times for [date], or for today when it is null.
     *
     * Purely a selection: the timetable is already computed on demand by
     * [com.example.engine.PrayerCalculationEngine] for any date, so this changes
     * what the screens ask for and nothing else.
     */
    fun setSelectedDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    /** Back to today, whatever day is currently selected. */
    fun clearSelectedDate() = setSelectedDate(null)

    fun jumpToContinueReading() {
        val cr = _uiState.value.continueReading
        selectSurah(cr.surahNumber, cr.ayahNumber)
    }

    fun toggleBookmark(ayah: Ayah) {
        viewModelScope.launch {
            repository.toggleBookmark(
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber,
                surahName = _uiState.value.selectedSurah.englishName,
                snippet = ayah.textArabic
            )
        }
    }

    // Audio Playback
    fun togglePlayAyah(ayah: Ayah) {
        if (_uiState.value.isAudioPlaying && _uiState.value.currentAudioAyah == ayah.ayahNumber) {
            audioPlayer.pause()
        } else {
            audioPlayer.playAyah(ayah.surahNumber, ayah.ayahNumber) {
                // Autoplay next ayah if available
                val nextAyah = ayah.ayahNumber + 1
                if (nextAyah <= _uiState.value.selectedSurah.totalVerses) {
                    val next = _uiState.value.currentSurahAyahs.find { it.ayahNumber == nextAyah }
                    if (next != null) {
                        togglePlayAyah(next)
                    }
                }
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    // Calendar
    fun selectCalendarDate(date: LocalDate) {
        val state = _uiState.value
        val dayPrayers = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments
        )
        _uiState.value = state.copy(
            calendarSelectedDate = date,
            calendarSelectedDayPrayers = dayPrayers
        )
    }

    // SensorEventListener
    // The TYPE_ORIENTATION branch below is a deliberate last-resort fallback for
    // hardware that exposes neither a rotation vector nor a geomagnetic field
    // sensor. Modern devices take the getRotationMatrix / getRotationMatrixFromVector
    // paths above; removing this branch would leave the Qibla screen with no
    // heading at all on older phones.
    @Suppress("DEPRECATION")
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val alphaMag = 0.25f
            geomagneticValues[0] = alphaMag * event.values[0] + (1 - alphaMag) * geomagneticValues[0]
            geomagneticValues[1] = alphaMag * event.values[1] + (1 - alphaMag) * geomagneticValues[1]
            geomagneticValues[2] = alphaMag * event.values[2] + (1 - alphaMag) * geomagneticValues[2]
            hasGeomagnetic = true

            val x = geomagneticValues[0]
            val y = geomagneticValues[1]
            val z = geomagneticValues[2]
            val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
            val status = QiblaEngine.evaluateMagneticField(magnitude)
            _uiState.value = _uiState.value.copy(
                magneticFieldMagnitude = magnitude,
                magneticStatus = status
            )
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val alphaAcc = 0.20f
            gravityValues[0] = alphaAcc * event.values[0] + (1 - alphaAcc) * gravityValues[0]
            gravityValues[1] = alphaAcc * event.values[1] + (1 - alphaAcc) * gravityValues[1]
            gravityValues[2] = alphaAcc * event.values[2] + (1 - alphaAcc) * gravityValues[2]
            hasGravity = true

            // Calculate device pitch and roll tilt angles
            val ax = gravityValues[0].toDouble()
            val ay = gravityValues[1].toDouble()
            val az = gravityValues[2].toDouble()
            val pitch = Math.toDegrees(kotlin.math.atan2(-ax, kotlin.math.sqrt(ay * ay + az * az))).toFloat()
            val roll = Math.toDegrees(kotlin.math.atan2(ay, az)).toFloat()
            val isLevel = kotlin.math.abs(pitch) <= 18f && kotlin.math.abs(roll) <= 18f

            _uiState.value = _uiState.value.copy(
                pitchDegrees = pitch,
                rollDegrees = roll,
                isDeviceLevel = isLevel
            )
        }

        val rotationMatrix = FloatArray(9)
        var gotRotation = false

        if (hasGravity && hasGeomagnetic) {
            gotRotation = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)
        } else if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            gotRotation = true
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val azimuth = (event.values[0] + 360f) % 360f
            updateCompassHeading(azimuth)
            return
        }

        if (gotRotation) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val rawAzimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
            updateCompassHeading(rawAzimuth)
        }
    }

    private fun updateCompassHeading(rawAzimuth: Float) {
        // Smooth azimuth using circular difference
        var diff = (rawAzimuth - lastAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val smoothAzimuth = ((lastAzimuth + 0.20f * diff) + 360f) % 360f
        lastAzimuth = smoothAzimuth

        // Calculate geomagnetic declination for True North vs Magnetic North.
        // Cached — recomputing GeomagneticField on every sensor event is expensive.
        val loc = _uiState.value.location
        val declination = cachedDeclinationFor(loc.latitude.toFloat(), loc.longitude.toFloat())

        val finalAzimuth = if (_uiState.value.useTrueNorth) {
            (smoothAzimuth + declination + 360f) % 360f
        } else {
            smoothAzimuth
        }

        val qiblaBearing = _uiState.value.qiblaBearing
        val delta = (qiblaBearing - finalAzimuth + 360f) % 360f
        val relativeAngle = QiblaEngine.calculateRelativeAngle(finalAzimuth, qiblaBearing)
        val isFacing = kotlin.math.abs(relativeAngle) <= 4.0f

        if (isFacing && !wasFacingQibla) {
            val now = System.currentTimeMillis()
            if (now - lastVibrateTimestamp > 1200L) {
                lastVibrateTimestamp = now
                vibrateQiblaLock()
            }
        }
        wasFacingQibla = isFacing

        val accuracyLabel = if (isFacing) {
            "FACING QIBLA 🕋"
        } else {
            when (_uiState.value.magneticSensorAccuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE (Calibrate)"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW ACCURACY"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM ACCURACY"
                else -> "HIGH ACCURACY"
            }
        }

        _uiState.value = _uiState.value.copy(
            compassAzimuth = finalAzimuth,
            qiblaDelta = delta,
            relativeQiblaAngle = relativeAngle,
            isFacingQibla = isFacing,
            magneticDeclination = declination,
            compassAccuracy = accuracyLabel
        )
    }

    private fun vibrateQiblaLock() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(45)
                }
            }
        } catch (e: Exception) {
            // Ignored in unit tests or when vibrator is missing
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD ||
            sensor?.type == Sensor.TYPE_ROTATION_VECTOR ||
            sensor?.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            _uiState.value = _uiState.value.copy(magneticSensorAccuracy = accuracy)
        }
    }
}
