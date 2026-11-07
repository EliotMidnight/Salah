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
import com.example.data.PrayerTimesProvider
import com.example.data.local.PrayerLogEntity
import com.example.data.local.SalahDatabase
import com.example.data.location.LocationFailure
import com.example.data.location.LocationFetchResult
import com.example.data.remote.HabousClient
import com.example.data.repository.SalahRepository
import com.example.data.model.CalculationMethod
import com.example.data.model.defaultAlertModes
import com.example.data.model.HijriDate
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.engine.HijriCalendarEngine
import com.example.engine.MagneticFieldStatus
import com.example.engine.PrayerCalculationEngine
import com.example.engine.PrayerNotificationManager
import com.example.engine.PrayerTimeZone
import com.example.engine.QiblaEngine
import com.example.engine.QiblaGuidance
import com.example.engine.SunPosition
import com.example.service.PrayerAlarmScheduler
import com.example.service.PrayerAlertService
import com.example.ui.theme.Accent
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
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
import kotlin.math.abs

/**
 * How a location attempt ended.
 */
sealed interface LocationStatus {
    /** Started, not finished. */
    data object Acquiring : LocationStatus

    /**
     * Finished, with a place.
     */
    data class Resolved(
        val name: String,
        val country: String,
        val isFresh: Boolean
    ) : LocationStatus

    /** Finished, without a place. */
    data class Failed(val reason: LocationFailure) : LocationStatus
}

data class SalahUiState(
    val location: UserLocation = UserLocation.DEFAULT,
    val method: CalculationMethod = CalculationMethod.MOROCCO_MINISTRY,
    val madhhab: Madhhab = Madhhab.STANDARD,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val todayPrayerTimes: PrayerTimesDay? = null,

    /**
     * The next prayer to come round, and the last one to have entered.
     */
    val nextPrayer: PrayerTime? = null,
    val previousPrayer: PrayerTime? = null,
    /**
     * The time left until [nextPrayer], as the Today page shows it.
     */
    val countdownString: String = "00h 00m",

    /**
     * The reader's Hijri adjustment, in days.
     */
    val hijriAdjustment: Int = 0,
    /**
     * The day the Prayer times are being shown for, or null for today.
     */
    val selectedDate: LocalDate? = null,
    val prayerLog: PrayerLogEntity = PrayerLogEntity(LocalDate.now().toString()),
    val adhanNotificationEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val vibrateOnly: Boolean = false,
    val isGlobalSilentMode: Boolean = false,
    val prePrayerOffsetMinutes: Int = 10,

    /**
     * Whether the device currently has a connection.
     */
    val isOnline: Boolean = false,
    // Expanded user preferences
    val language: String = "English",
    val appTheme: String = "System Default",
    /** The stored accent key; the theme resolves it to a family. */
    val accent: String = Accent.DEFAULT.key,
    /** When true the chrome uses the opaque path even on a blur-capable device. */
    val reduceTransparency: Boolean = false,
    val timeFormat24h: Boolean = true,
    // Qibla state & magnetic sensor diagnostics
    val compassAzimuth: Float = 0f,
    val qiblaBearing: Float = 0f,

    /**
     * Which way to turn to face the Kaaba, and by how much.
     */
    val qiblaGuidance: QiblaGuidance = QiblaGuidance(QiblaGuidance.Direction.ON_TARGET, 0),

    /**
     * A sentence about how much the compass can be trusted, already chosen.
     */
    val compassAccuracy: String = "HIGH ACCURACY",
    val magneticFieldMagnitude: Float = 46.0f, // uT
    val magneticStatus: MagneticFieldStatus = MagneticFieldStatus.OPTIMAL,
    val useTrueNorth: Boolean = true,
    val sunPosition: SunPosition? = null,
    val distanceToKaabaKm: Int = 0,
    val isDeviceLevel: Boolean = true,
    val isLocating: Boolean = false,
    /**
     * How the location attempt is going, as facts.
     */
    val locationStatus: LocationStatus? = null,
) {
    /**
     * True once the Kaaba is inside the alignment window.
     */
    val isFacingQibla: Boolean get() = qiblaGuidance.isAligned
}

class SalahViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository: SalahRepository

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

    /** The magnetometer's own accuracy report; see [onAccuracyChanged]. */
    private var sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private var lastAzimuth: Float = 0f
    private var declCacheLat = Float.NaN
    private var declCacheLon = Float.NaN
    private var declCacheValue = 0f
    private var declCacheAtMs = 0L

    init {
        val database = SalahDatabase.getDatabase(application)
        repository = SalahRepository(database.salahDao(), application)
        PrayerNotificationManager.initChannels(application)

        // Observe repository state
        viewModelScope.launch {
            repository.locationFlow.collectLatest { loc ->
                _uiState.value = _uiState.value.copy(location = loc)
                recalculateAll()
                // The stored month belongs to the old place. Ask for the new
                // one now rather than waiting for a reconnect: being online and
                // getting calculated times after a move looks like a bug.
                if (_uiState.value.isOnline) refreshOfficialTimes()
            }
        }

        viewModelScope.launch {
            repository.methodFlow.collectLatest { meth ->
                _uiState.value = _uiState.value.copy(method = meth)
                recalculateAll()
                if (_uiState.value.isOnline) refreshOfficialTimes()
            }
        }

        viewModelScope.launch {
            repository.madhhabFlow.collectLatest { madh ->
                _uiState.value = _uiState.value.copy(madhhab = madh)
                recalculateAll()
                if (_uiState.value.isOnline) refreshOfficialTimes()
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
            repository.languageFlow.collectLatest { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }

        viewModelScope.launch {
            repository.themeFlow.collectLatest { t ->
                _uiState.value = _uiState.value.copy(appTheme = t)
            }
        }

        viewModelScope.launch {
            repository.accentFlow.collectLatest { key ->
                _uiState.value = _uiState.value.copy(accent = Accent.fromKey(key).key)
            }
        }

        viewModelScope.launch {
            repository.reduceTransparencyFlow.collectLatest { reduce ->
                _uiState.value = _uiState.value.copy(reduceTransparency = reduce)
            }
        }

        viewModelScope.launch {
            repository.timeFormat24hFlow.collectLatest { tf ->
                _uiState.value = _uiState.value.copy(timeFormat24h = tf)
            }
        }

        viewModelScope.launch {
            repository.hijriAdjustmentFlow.collectLatest { adj ->
                _uiState.value = _uiState.value.copy(hijriAdjustment = adj)
                recalculateAll()
            }
        }

        viewModelScope.launch {
            repository.isOnlineFlow.collectLatest { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
                if (online) refreshOfficialTimes()
            }
        }

        // Room observers.
        //
        // The prayer log is a row keyed by date, so this cannot be a one-off
        // collection: at midnight the date changes and the observer has to follow
        // it. Re-armed by [observePrayerLog] from the ticker below.
        observePrayerLog(PrayerTimeZone.localNow().toLocalDate())

        // Register background prayer alarm scheduling
        PrayerAlarmScheduler.scheduleAllPrayers(application)
    }

    /**
     * Whether this device has a compass at all.
     */
    val hasCompass: Boolean get() = rotationSensor != null || magneticSensor != null

    /**
     * How many composed surfaces are showing a heading.
     *
     * Registration is reference-counted rather than a flag because the
     * destination that is leaving and the one that is arriving overlap while a
     * navigation transition runs: without the count, the outgoing surface's
     * [stopCompass] would switch the sensors off under the incoming one.
     */
    private var compassWatchers = 0

    /**
     * Starts the compass stream for one composed surface.
     *
     * Called by the surfaces that display a heading - Qibla, and Settings'
     * sensor diagnostics - not at launch. The magnetometer at UI rate is a
     * continuous drain, and no other surface reads a heading; a device with no
     * compass registers nothing and simply never hears from a sensor, which is
     * the same as a device whose sensors are all asleep, and [hasCompass] is
     * what tells those two apart.
     */
    fun startCompass() {
        compassWatchers++
        if (compassWatchers > 1) return
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

    /**
     * Releases one composed surface's hold on the compass.
     */
    fun stopCompass() {
        if (compassWatchers == 0) return
        compassWatchers--
        if (compassWatchers == 0) {
            sensorManager?.unregisterListener(this)
        }
    }

    fun toggleTrueNorth() {
        val current = _uiState.value.useTrueNorth
        _uiState.value = _uiState.value.copy(useTrueNorth = !current)
        // Force recalculate heading with new north setting
        updateCompassHeading(lastAzimuth)
    }

    override fun onCleared() {
        super.onCleared()
        compassWatchers = 0
        sensorManager?.unregisterListener(this)
        tickerJob?.cancel()
    }

    /**
     * Ties the ticker to the app's foreground life.
     *
     * The tick is a 1 Hz recomputation of the next prayer, the countdown and the
     * sun's position. Nothing on screen needs keeping when the app is not in
     * front of the reader and the alarms are already scheduled, so it runs only
     * between ON_START and ON_STOP and catches up on the way back in: the loop
     * runs [updateLiveTiming] before its first delay.
     */
    fun setForeground(foreground: Boolean) {
        if (foreground) {
            startLiveTicker()
        } else {
            tickerJob?.cancel()
        }
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
        // "Today" and "now" come from the clock the day's times are written in.
        // On a phone whose tzdata predates Morocco's 2026 decree that clock is
        // pinned to UTC while the device displays +1, and mixing the two made
        // the app treat each prayer as passed an hour early.
        val now = PrayerTimeZone.localNow()
        val today = now.toLocalDate()

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
            // A day rollover can be a month rollover: ask for the new file
            // before the store is read, or an app left open across the 1st keeps
            // calculating. The client no-ops when a valid month is stored.
            if (_uiState.value.isOnline) refreshOfficialTimes()
            return
        }

        val nextPt = PrayerCalculationEngine.getNextPrayer(day, now.toLocalTime())
        val prevPt = PrayerCalculationEngine.getPreviousPrayer(day, now.toLocalTime())
        val countdown = PrayerCalculationEngine.formatRemainingCountdown(nextPt.dateTime, now)
        // The sun's position is a physical fact, not a legal-time one, so it
        // keeps the instant the device actually is at: `now` above may be the
        // pinned UTC clock rather than the one in the status bar.
        val sun = QiblaEngine.calculateSunPosition(state.location, LocalDateTime.now())

        _uiState.value = state.copy(
            nextPrayer = nextPt,
            previousPrayer = prevPt,
            countdownString = countdown,
            sunPosition = sun
        )
    }

    /**
     * Adopts the ministry's published month, if the device is online and the
     * published row describes this place. Recalculates only when something
     * actually changed, so a failed fetch costs nothing visible.
     */
    private fun refreshOfficialTimes() {
        val state = _uiState.value
        viewModelScope.launch {
            val adopted = HabousClient.refresh(
                context = getApplication(),
                location = state.location,
                method = state.method,
                madhhab = state.madhhab
            )
            if (adopted) recalculateAll()
        }
    }

    fun recalculateAll() {
        val state = _uiState.value
        // Same rule as the ticker: the day, "now" and the countdown all use the
        // clock the times are written in, never a second reading of the device's.
        val now = PrayerTimeZone.localNow()
        val today = now.toLocalDate()
        val calculatedToday = PrayerTimesProvider.forDate(
            context = getApplication(),
            date = today,
            location = state.location,
            method = state.method,
            madhhab = state.madhhab,
            adjustments = state.adjustments
        )

        val nextPt = PrayerCalculationEngine.getNextPrayer(calculatedToday, now.toLocalTime())
        val prevPt = PrayerCalculationEngine.getPreviousPrayer(calculatedToday, now.toLocalTime())
        val countdown = PrayerCalculationEngine.formatRemainingCountdown(nextPt.dateTime, now)
        val qiblaBearing = QiblaEngine.calculateQiblaBearing(state.location.latitude, state.location.longitude)
        val distanceKaaba = QiblaEngine.calculateDistanceToKaabaKm(state.location.latitude, state.location.longitude)
        val sun = QiblaEngine.calculateSunPosition(state.location)

        _uiState.value = state.copy(
            todayPrayerTimes = calculatedToday,
            nextPrayer = nextPt,
            previousPrayer = prevPt,
            countdownString = countdown,
            qiblaBearing = qiblaBearing,
            distanceToKaabaKm = distanceKaaba,
            sunPosition = sun
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

    /**
     * Find the reader's location and store it.
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLocating = true,
                locationStatus = LocationStatus.Acquiring
            )
            when (val result = repository.fetchAndCacheLocation()) {
                is LocationFetchResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        locationStatus = LocationStatus.Resolved(
                            name = result.location.name,
                            country = result.location.country,
                            isFresh = result.isFresh
                        )
                    )
                    recalculateAll()
                }

                is LocationFetchResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        locationStatus = LocationStatus.Failed(result.reason)
                    )
                }
            }
        }
    }

    /**
     * A permission denial is a location failure too. Before this existed the
     * Settings banner only appeared if a fetch had run, so refusing the prompt
     * left the screen looking as if nothing had happened.
     */
    fun reportLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            isLocating = false,
            locationStatus = LocationStatus.Failed(LocationFailure.PERMISSION_NOT_GRANTED)
        )
    }

    fun sendTestNotification() {
        PrayerNotificationManager.showTestNotification(getApplication())
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

    fun toggleGlobalSilentMode() {
        val newState = !_uiState.value.isGlobalSilentMode
        repository.setGlobalSilentMode(newState)
    }

    fun setLanguage(language: String) {
        repository.setLanguage(language)
    }

    fun setAppTheme(theme: String) {
        repository.setAppTheme(theme)
    }

    fun setAccent(accentKey: String) {
        repository.setAccent(Accent.fromKey(accentKey).key)
    }

    fun setReduceTransparency(enabled: Boolean) {
        repository.setReduceTransparency(enabled)
    }

    fun setTimeFormat24h(is24h: Boolean) {
        repository.setTimeFormat24h(is24h)
    }

    fun setHijriAdjustment(adjustment: Int) {
        repository.setHijriAdjustment(adjustment)
    }

    /**
     * Re-read connectivity, and re-derive today's times.
     */
    fun refreshConnectivity() {
        repository.refreshConnectivity()
        recalculateAll()
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

    /**
     * Re-derive the schedule and re-arm the alarms.
     */
    fun reschedulePrayers() {
        recalculateAll()
        PrayerAlarmScheduler.scheduleAllPrayers(getApplication())
    }

    /**
     * Every preference this app has, back to its default.
     *
     * **Through the repository, one setting at a time.** The last three lines used to
     * be a single `copy()` into the UI state, so the settings screen showed the
     * defaults and the *storage* kept the old values - which is the shape of bug a
     * reader cannot see coming and cannot undo.
     */
    fun resetAllSettings() {
        // Prayer calculation.
        setCalculationMethod(CalculationMethod.MOROCCO_MINISTRY)
        setMadhhab(Madhhab.STANDARD)
        setAdjustments(PrayerAdjustments())
        setHijriAdjustment(0)

        // Alerts. Each of these re-arms the alarms, so the order does not matter and
        // the last one wins the arming.
        setAdhanNotification(true)
        setPrePrayerAlert(true)
        setVibrateOnly(false)
        defaultAlertModes.forEach { (prayer, mode) ->
            repository.setPrayerAlertMode(prayer, mode)
        }
        repository.setGlobalSilentMode(false)

        // Display and interface.
        setTimeFormat24h(true)
        setAppTheme("System Default")
        setAccent(Accent.DEFAULT.key)
        setReduceTransparency(false)
        setLanguage("English")

        // Location and the reader's place are deliberately **not** reset.
        //
        // They are the reader's, not a preference: a reset that moved a reader to
        // Mecca would be a surprise with no undo. It is also why the confirmation
        // names what is kept.

        recalculateAll()
    }

    fun setSelectedDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    /** Back to today, whatever day is currently selected. */
    fun clearSelectedDate() = setSelectedDate(null)

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

            // Device tilt, as the one thing the reader acts on.
            //
            // The pitch and roll were also published individually, into two state fields
            // nothing read. The only consumer is the Qibla screen's "hold the phone
            // flat" hint, and what it asks is a yes or no - and the threshold is the
            // interesting part, not the degrees. Keeping the numbers would invite a
            // second threshold somewhere, and two thresholds on a jittery accelerometer
            // reading is a hint that flickers.
            //
            // The window is 18 degrees because a compass reading taken while the phone
            // is tilted is wrong by roughly the tilt, and 18 is past the point a reader
            // holds one-handed; see the low-pass filter above, which is what makes this
            // a stable answer rather than a noisy one.
            val ax = gravityValues[0].toDouble()
            val ay = gravityValues[1].toDouble()
            val az = gravityValues[2].toDouble()
            val pitch = Math.toDegrees(kotlin.math.atan2(-ax, kotlin.math.sqrt(ay * ay + az * az))).toFloat()
            val roll = Math.toDegrees(kotlin.math.atan2(ay, az)).toFloat()

            val level = abs(pitch) <= DEVICE_LEVEL_TOLERANCE_DEGREES &&
                abs(roll) <= DEVICE_LEVEL_TOLERANCE_DEGREES
            if (level != _uiState.value.isDeviceLevel) {
                // Only written on a change: this runs on every accelerometer event,
                // and an unconditional `copy()` on each one is a state emission per
                // event for a value nobody was waiting to change.
                _uiState.value = _uiState.value.copy(isDeviceLevel = level)
            }
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
        val relativeAngle = QiblaEngine.calculateRelativeAngle(finalAzimuth, qiblaBearing)

        // Alignment is asked of QiblaGuidance rather than re-decided here.
        val guidance = QiblaGuidance.fromRelative(relativeAngle)
        val isFacing = guidance.isAligned

        if (isFacing && !wasFacingQibla) {
            val now = System.currentTimeMillis()
            if (now - lastVibrateTimestamp > 1200L) {
                lastVibrateTimestamp = now
                vibrateQiblaLock()
            }
        }
        wasFacingQibla = isFacing

        // The accuracy sentence, chosen from the sensor's own report rather than from a
        // field in this state.
        val accuracyLabel = when (sensorAccuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE (Calibrate)"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW ACCURACY"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM ACCURACY"
            else -> "HIGH ACCURACY"
        }

        if (finalAzimuth != _uiState.value.compassAzimuth || accuracyLabel != _uiState.value.compassAccuracy) {
            _uiState.value = _uiState.value.copy(
                compassAzimuth = finalAzimuth,
                qiblaGuidance = guidance,
                compassAccuracy = accuracyLabel
            )
        } else {
            _uiState.value = _uiState.value.copy(qiblaGuidance = guidance)
        }
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

    /**
     * The magnetometer's own accuracy report.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD ||
            sensor?.type == Sensor.TYPE_ROTATION_VECTOR ||
            sensor?.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            sensorAccuracy = accuracy
        }
    }

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
}

/**
 * How far the phone may be tilted before its compass reading is not trusted.
 */
internal const val DEVICE_LEVEL_TOLERANCE_DEGREES = 18f
