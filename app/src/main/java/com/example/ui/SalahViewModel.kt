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
import com.example.data.location.LocationFailure
import com.example.data.location.LocationFetchResult
import com.example.data.model.Ayah
import com.example.data.model.AdhanSound
import com.example.data.model.CalculationMethod
import com.example.data.model.defaultAlertModes
import com.example.data.model.HijriDate
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranPaperTone
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.data.model.QuranRef
import com.example.data.model.Surah
import com.example.data.model.UserLocation
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranDataSource
import com.example.data.repository.SalahRepository
import com.example.engine.HijriCalendarEngine
import com.example.engine.MagneticFieldStatus
import com.example.engine.PrayerCalculationEngine
import com.example.engine.PrayerNotificationManager
import com.example.engine.QiblaEngine
import com.example.engine.QiblaGuidance
import com.example.engine.QuranAudioPlayer
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
    val continueReading: ContinueReadingEntity = ContinueReadingEntity(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val adhanNotificationEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val vibrateOnly: Boolean = false,
    val isGlobalSilentMode: Boolean = false,
    val autoSilentDuringPrayer: Boolean = false,
    val autoSilentDurationMinutes: Int = 20,

    /**
     * Whether the device currently has a connection.
     */
    val isOnline: Boolean = false,
    // Expanded user preferences
    val language: String = "English",
    val riwayah: String = "Hafs 'an 'Asim",
    val appTheme: String = "System Default",
    val quranScript: String = "Uthmani (Madani)",
    val timeFormat24h: Boolean = true,
    val adhanSound: AdhanSound = AdhanSound.MAKKAH,
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
    // Quran reader state
    //
    // There is deliberately no `selectedSurah` and no `currentSurahAyahs` here.
    //
    // The reader's place is `ReaderPosition.ref`, and it is the Quran destination
    // that owns it - the same object the reading surfaces read, the index navigates
    // and the chrome reports. A copy of it in this state had two writers and no way
    // to reach the reader: `selectSurah` wrote it, and the reader never asked, so a
    // surah chosen from the index changed the pill's name and left the page where
    // it was. And the reader wrote it *back* through a 600ms debounce, so for half
    // a second the pill paired the new page number with the old surah's name.
    //
    // What the ViewModel has instead is a **request**: something outside the reader
    // - the index, a bookmark, a search hit, Continue Reading - asking to be put
    // somewhere. One field, one writer, and it is consumed by the destination rather
    // than mirrored.
    //
    // The reader's place in the book, once read, is not in this state at all.

    /**
     * A request to open the Quran at [QuranRef], or null.
     */
    val pendingOpen: QuranRef? = null,

    val isAudioPlaying: Boolean = false,

    /**
     * Which verse is playing, as a reference.
     *
     * A whole reference and not an ayah number, because an ayah number alone does
     * not say which verse: page 604 holds three ayah-1s, and a pager composes its
     * neighbours, so "ayah 5 is playing" was true of a verse on a page the reader
     * could also be looking at. `QuranAudioPlayer` already knew the surah - it is
     * the argument it was asked to play - and the field carrying it was never read.
     */
    val currentAudioRef: QuranRef = QuranRef.Start,
    /**
     * The reader's own preferences, as one value.
     */
    val quranReadingOptions: QuranReadingOptions = QuranReadingOptions(),
    /**
     * Whether the reader has taken over the whole screen.
     */
    val isQuranImmersive: Boolean = false
) {
    /**
     * True once the Kaaba is inside the alignment window.
     */
    val isFacingQibla: Boolean get() = qiblaGuidance.isAligned
}

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

    /** The magnetometer's own accuracy report; see [onAccuracyChanged]. */
    private var sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private var lastAzimuth: Float = 0f
    private var declCacheLat = Float.NaN
    private var declCacheLon = Float.NaN
    private var declCacheValue = 0f
    private var declCacheAtMs = 0L

    /**
     * Set once the stored reading position has been applied for this launch.
     */
    private var readingPositionRestored = false

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
                    // Resume, exactly once, the first time a stored position
                    // arrives.
                    //
                    // This has to be guarded rather than run on every emission,
                    // because this is also the flow the reader writes to as the
                    // user scrolls: an unguarded restore would yank the reader
                    // back to the opening ayah the instant they moved. The flag
                    // means the stored position is applied once per launch and
                    // never again.
                    //
                    // It is a *request* now, like every other way into the book, and
                    // the Quran destination resolves it to a reference from the
                    // corpus rather than from the stored row - so a row written by a
                    // build whose partition differed cannot seed the reader with a
                    // page that no longer holds the verse.
                    if (!readingPositionRestored && cont.surahNumber > 0) {
                        readingPositionRestored = true
                        requestOpen(QuranBrowse.ref(cont.surahNumber, cont.ayahNumber) ?: return@collectLatest)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.quranReadingOptions.collectLatest { options ->
                _uiState.value = _uiState.value.copy(quranReadingOptions = options)
            }
        }

        viewModelScope.launch {
            repository.getBookmarks().collectLatest { bms ->
                _uiState.value = _uiState.value.copy(bookmarks = bms)
            }
        }

        // Audio state observer
        //
        // The playing verse arrives as a whole reference, so the "is this verse
        // playing" test the surfaces run can be exact. The player already knew the
        // surah - it was the argument it was handed - and the state used to keep only
        // the ayah number, so the dot and the play/pause icon lit on ayah *N* of
        // whatever surah happened to be composed nearby.
        viewModelScope.launch {
            audioPlayer.playbackState.collectLatest { ps ->
                _uiState.value = _uiState.value.copy(
                    isAudioPlaying = ps.isPlaying,
                    currentAudioRef = QuranRef(
                        surah = ps.surahNumber,
                        ayah = ps.ayahNumber,
                        page = QuranBrowse.pageOf(ps.surahNumber, ps.ayahNumber)
                    )
                )
            }
        }

        // Nothing to initialise for the Quran, and that is the point.
        //
        // This used to eagerly select Al-Fatihah, because `currentSurahAyahs` started
        // empty and the reader rendered a loading state until a surah was chosen. Now
        // the reader's place is `ReaderPosition`, which the destination seeds with
        // `QuranRef.Start` - Al-Fatihah, page 1 - and the stored Continue Reading row
        // replaces as soon as it arrives. So a first run opens Al-Fatihah because that
        // is the start of the book, not because something had to be written down first.
        _uiState.value = _uiState.value.copy(isQuranImmersive = repository.quranImmersive)

        // Start 1-second live ticker
        startLiveTicker()

        // Register background prayer alarm scheduling
        PrayerAlarmScheduler.scheduleAllPrayers(application)

        // Register compass sensor
        startCompass()
    }

    /**
     * Whether this device has a compass at all.
     */
    val hasCompass: Boolean get() = rotationSensor != null || magneticSensor != null

    fun startCompass() {
        // A device with no compass registers nothing and simply never hears from
        // a sensor, which is the same as a device whose sensors are all asleep.
        // [hasCompass] is what tells those two apart, for the caller deciding
        // whether to show the Qibla tab.
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
        val sun = QiblaEngine.calculateSunPosition(state.location, now)

        _uiState.value = state.copy(
            nextPrayer = nextPt,
            previousPrayer = prevPt,
            countdownString = countdown,
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
        // The Hijri date is deliberately **not** computed here.
        //
        // It used to be, and it was the only Hijri date in the app that honoured
        // `hijriAdjustment` - and nothing read it. The adjustment therefore round-tripped
        // through preferences, the repository flow and a full recalculation, changed
        // the number in the Settings row, and changed nothing a reader could see.
        // Two representations of "what Hijri date is it", one of them invisible.
        //
        // So each surface applies the adjustment where it renders the date, from the
        // one `hijriAdjustment` in this state. See `TodayScreen`, `PrayerScreen` and
        // `HijriMonthSheet`.

        val qiblaBearing = QiblaEngine.calculateQiblaBearing(state.location.latitude, state.location.longitude)
        val distanceKaaba = QiblaEngine.calculateDistanceToKaabaKm(state.location.latitude, state.location.longitude)
        val sun = QiblaEngine.calculateSunPosition(state.location)

        // No second day is calculated here.
        //
        // There used to be a `calendarSelectedDate` / `calendarSelectedDayPrayers` pair,
        // and this function built a complete `PrayerTimesDay` for it on every location,
        // method, madhhab, adjustment and midnight change - and nothing read either
        // field. `selectCalendarDate`, which wrote them, was itself never called: the
        // calendar goes through `setSelectedDate`, so the pair sat frozen at
        // `LocalDate.now()` from construction and was recalculated all day to be
        // discarded.
        //
        // The Prayer tab already computes the day it is showing, from
        // `selectedDate`, on demand. So there is one day - the one the reader chose -
        // and no second calendar to disagree with it.

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

    // Quran reading options
    //
    // One setter rather than one per option, so every preference change is
    // normalised and persisted through the same path. The repairs
    // (continuous/horizontal, continuous/page) live in the model, which is the
    // only place that knows the rules.

    fun setQuranReadingOptions(options: QuranReadingOptions) {
        repository.saveQuranReadingOptions(options)
    }

    /**
     * Sets the layout, and nothing else.
     */
    fun setQuranLayout(layout: QuranReadingLayout) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(layout = layout))
    }

    /** Sets the axis, and nothing else. Same reasoning as [setQuranLayout]. */
    fun setQuranScroll(direction: QuranScrollDirection) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(scroll = direction))
    }

    /**
     * Breaks every verse out as its own selectable unit, in either layout.
     */
    fun setQuranPerVerse(perVerse: Boolean) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(perVerse = perVerse))
    }

    fun setQuranPinchTarget(target: QuranPinchTarget) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(pinchTarget = target))
    }

    fun setQuranPaper(tone: QuranPaperTone) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(paper = tone))
    }

    fun setQuranFont(face: QuranFontFace) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(font = face))
    }

    fun setQuranArabicScale(scale: Float) {
        setQuranReadingOptions(
            _uiState.value.quranReadingOptions.copy(
                arabicScale = scale.coerceIn(QuranReadingOptions.ArabicScaleRange)
            )
        )
    }

    fun setQuranTranslationScale(scale: Float) {
        setQuranReadingOptions(
            _uiState.value.quranReadingOptions.copy(
                translationScale = scale.coerceIn(QuranReadingOptions.TranslationScaleRange)
            )
        )
    }

    fun setQuranShowTranslation(show: Boolean) {
        setQuranReadingOptions(_uiState.value.quranReadingOptions.copy(showTranslation = show))
    }

    /**
     * Takes the reader over the whole screen, or gives the screen back.
     */
    fun setQuranImmersive(immersive: Boolean) {
        repository.quranImmersive = immersive
        _uiState.value = _uiState.value.copy(isQuranImmersive = immersive)
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

    fun setAdhanSound(sound: AdhanSound) {
        repository.setAdhanSound(sound)
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
        setAutoSilentDuringPrayerTo(!_uiState.value.autoSilentDuringPrayer)
    }

    /**
     * Auto-silence during prayer, as an explicit value.
     *
     * A setter as well as the toggle, so a reset can put it back without first reading
     * the current value and negating it. That would be a second derivation of "the
     * default", and it would restore the wrong thing if the toggle's own notion of the
     * current value were ever wrong.
     */
    fun setAutoSilentDuringPrayerTo(enabled: Boolean) {
        repository.setAutoSilentDuringPrayer(enabled)
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

    fun playAudioPreview(title: String) {
        if (_uiState.value.audioPreviewPlaying == title) {
            stopAudioPreview()
            return
        }
        _uiState.value = _uiState.value.copy(audioPreviewPlaying = title)

        // Which of the three things the reader might have pressed: an adhan sound, an
        // alert *mode*, or a reciter.
        //
        // This used to answer it by substring-matching the title against two hard-coded
        // word lists, one of which omitted "Gentle Bell Chime" - the only option in the
        // picker that is not an adhan. So previewing it fell through to the reciter
        // branch and played a **chime**, while the alarm for the same setting played a
        // **full adhan**, because the synthesizer's own matcher did not skip it either.
        // One setting, two sounds, and the one a reader could hear by pressing the
        // button was not the one they would get.
        val sound = AdhanSound.entries.firstOrNull { it.label == title }
        val mode = ALERT_MODES.firstOrNull { it == title }

        if (sound != null || mode != null) {
            AdhanAudioSynthesizer.playAlert(
                alertMode = mode ?: "Full Adhan",
                sound = sound ?: _uiState.value.adhanSound,
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

    /**
     * Re-derive the schedule and re-arm the alarms.
     *
     * **There is no cache.** `PrayerAlarmScheduler` computes today and tomorrow
     * from the engine every time it is asked and hands the result to
     * `AlarmManager`; nothing is stored, so nothing can go stale and there is
     * nothing to recompute. The engine runs in well under a millisecond for one
     * day. The button was reporting that it had verified a year of times.
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
     * reader cannot see coming and cannot undo: a reader who had silenced everything
     * and pressed "Reset all settings" was told they were back to defaults, and heard
     * a full adhan again on the next cold start.
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
        setPrePrayerOffsetMinutes(10)
        setAdhanVolume(0.85f)
        setVibrateOnly(false)
        defaultAlertModes.forEach { (prayer, mode) ->
            repository.setPrayerAlertMode(prayer, mode)
        }
        repository.setGlobalSilentMode(false)
        setAutoSilentDuringPrayerTo(false)
        setAutoSilentDuration(20)
        setAdhanSound(AdhanSound.MAKKAH)

        // Display and interface.
        setTimeFormat24h(true)
        setAppTheme("System Default")
        setLanguage("English")
        setQuranScript("Uthmani (Madani)")

        // The Quran reader's own preferences, so a reset really does hand back a
        // reader that looks and behaves the way it did on first run.
        setQuranReadingOptions(QuranReadingOptions())
        setQuranImmersive(false)

        // Reciter and riwayah name an audio stream and a reading tradition. There is
        // one of each in this app - the player streams from everyayah.com and the
        // corpus is one text - so they reset to what is actually offered rather than
        // being left as labels for nothing.
        setReciter("Mishary Rashid Alafasy")
        setRiwayah("Hafs 'an 'Asim")

        // Location and the reader's place are deliberately **not** reset.
        //
        // They are the reader's, not a preference: a reset that moved a reader to
        // Mecca, or to page 1 of wherever they had been reading, would be a surprise
        // with no undo. It is also why the confirmation names what is kept.

        recalculateAll()
    }

    // Quran actions
    //
    // Everything that wants the reader somewhere arrives as a [QuranRef] and becomes
    // `pendingOpen`. The Quran destination consumes it by handing it to
    // `ReaderPosition.goTo`, and the reader's place is then `ReaderPosition.ref` and
    // nothing else.

    /**
     * Asks for the reader to be put at [ref].
     */
    fun requestOpen(ref: QuranRef) {
        val resolved = QuranBrowse.ref(ref.surah, ref.ayah) ?: return
        _uiState.value = _uiState.value.copy(pendingOpen = resolved)
    }

    /**
     * Asks for a surah, which is a request for its first verse.
     *
     * A surah's ayah 1 is the only verse that means "this surah" - so it is resolved
     * through the same path as everything else rather than by a second convention.
     */
    fun requestOpenSurah(surahNumber: Int) {
        requestOpen(QuranBrowse.ref(surahNumber, 1) ?: return)
    }

    /**
     * The reader has settled on a verse; remember it for next time.
     *
     * **Persistence only.** This used to also write the reader's in-session position,
     * which was a second anchor for a fact `ReaderPosition` already owned and which
     * only ever got read once - at first composition, long before this fired. Two
     * writers, neither of which could affect anything, in a rebuild whose subject is
     * that there must be one writer.
     */
    fun onAyahViewed(ayah: Ayah) {
        viewModelScope.launch {
            val surah = QuranBrowse.surah(ayah.surahNumber)
            repository.saveContinueReading(
                ContinueReadingEntity(
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    surahName = surah?.englishName.orEmpty(),
                    pageNumber = ayah.pageNumber,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Show prayer times for [date], or for today when it is null.
     */
    fun setSelectedDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    /** Back to today, whatever day is currently selected. */
    fun clearSelectedDate() = setSelectedDate(null)

    /**
     * Asks for the reader's last remembered place.
     *
     * A request like any other, and the reason it is one is now visible: this used to
     * *be* the reader's place, in a state field the reader could not see. Pressing
     * "Continue reading" on the Today page set `selectedSurah`, so the pill named the
     * surah and the page stayed where it was.
     */
    fun jumpToContinueReading() {
        val cr = _uiState.value.continueReading
        requestOpen(QuranBrowse.ref(cr.surahNumber, cr.ayahNumber) ?: return)
    }

    fun toggleBookmark(ayah: Ayah) {
        viewModelScope.launch {
            // The name comes from the verse, not from whatever surah the reader
            // happens to have open. They are the same today; if they ever are not, a
            // bookmark whose name contradicts its own reference is worse than one
            // with no name.
            val surah = QuranBrowse.surah(ayah.surahNumber)
            repository.toggleBookmark(
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber,
                surahName = surah?.englishName.orEmpty(),
                snippet = ayah.textArabic
            )
        }
    }

    // Audio Playback
    fun togglePlayAyah(ayah: Ayah) {
        val ref = ayah.ref
        if (_uiState.value.isAudioPlaying && _uiState.value.currentAudioRef == ref) {
            audioPlayer.pause()
        } else {
            _uiState.value = _uiState.value.copy(currentAudioRef = ref)
            audioPlayer.playAyah(ayah.surahNumber, ayah.ayahNumber) {
                // Autoplay the next ayah of **this verse's** surah.
                //
                // It used to walk `_uiState.currentSurahAyahs` and stop at
                // `selectedSurah.totalVerses` - the surah the reader had *open*, which
                // is not necessarily the surah being recited. Playing 114:6 would try
                // to continue into a surah that was not being read.
                QuranBrowse.ayah(ayah.surahNumber, ayah.ayahNumber + 1)
                    ?.let { next -> togglePlayAyah(next) }
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
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
        //
        // This used to be its own `abs(relativeAngle) <= 4.0f`, and the banner had a
        // third answer again in the shape of the instruction it printed. Three
        // tolerances cannot agree at the edge: at 4.5 degrees the dial's ring turned
        // to the success colour, the check mark appeared in the middle of the dial,
        // and the banner still said "turn 5°". Now the boolean, the needle, the
        // number of degrees and the haptic all fall out of one value.
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
        //
        // `magneticSensorAccuracy` was the raw `SENSOR_STATUS_*` integer sitting beside
        // this string in the UI state, and it was the one the string was derived from.
        // Two representations of one fact, where one can be copied over without the
        // other - the shape that produced the four two-writer bugs this rebuild fixed.
        // Now there is only the sentence.
        //
        // Note also what this does *not* say. "FACING QIBLA" is not an accuracy
        // reading: it is the alignment state, which is what the banner and the dial's
        // ring already say, with the actual degree count. A row in Settings labelled
        // "Compass Sensors & Diagnostics" reporting alignment tells a reader nothing
        // about their compass, and the emoji made it read as a status light rather than
        // as a measurement. Aligned is now reported by the banner that exists for it.
        val accuracyLabel = when (sensorAccuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE (Calibrate)"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW ACCURACY"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM ACCURACY"
            else -> "HIGH ACCURACY"
        }

        // Two fields, both of which a screen reads and neither of which is derivable
        // here alone: the heading the compass currently reports, and the guidance
        // computed from it. The declination is not published - it is an input to the
        // heading above, and a second copy of it in the UI state is a number that can
        // disagree with the heading it produced.
        if (finalAzimuth != _uiState.value.compassAzimuth || accuracyLabel != _uiState.value.compassAccuracy) {
            _uiState.value = _uiState.value.copy(
                compassAzimuth = finalAzimuth,
                qiblaGuidance = guidance,
                compassAccuracy = accuracyLabel
            )
        } else {
            // The heading is smoothed, so it usually has not moved; the guidance may
            // still have, and it is the value the banner and the haptic read.
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
}

/**
 * How far the phone may be tilted before its compass reading is not trusted.
 *
 * A compass reading taken while the phone is tilted is wrong by roughly the tilt
 * angle, so this is a claim about the reading's accuracy rather than about the
 * device's posture. 18 degrees is past the point at which a phone is held one-handed
 * to face the Kaaba, which is the only posture this matters in.
 */
internal const val DEVICE_LEVEL_TOLERANCE_DEGREES = 18f

/**
 * The alert *modes* the Settings screen offers to preview.
 */
private val ALERT_MODES = listOf("Full Adhan", "Takbeer Only", "Gentle Chime")
