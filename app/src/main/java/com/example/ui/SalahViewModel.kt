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

data class SalahUiState(
    val location: UserLocation = UserLocation.DEFAULT,
    val method: CalculationMethod = CalculationMethod.MOROCCO_MINISTRY,
    val madhhab: Madhhab = Madhhab.STANDARD,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val todayPrayerTimes: PrayerTimesDay? = null,

    /**
     * The next prayer to come round, and the last one to have entered.
     *
     * The two halves of the live reading, and the page asks for both: the countdown
     * alone cannot draw the day, because which row is highlighted depends on where
     * between two prayers the reader is standing.
     */
    val nextPrayer: PrayerTime? = null,
    val previousPrayer: PrayerTime? = null,
    /**
     * The time left until [nextPrayer], as the Today page shows it.
     *
     * Maintained here by the one-second ticker rather than computed in the page:
     * the page used to compute its own inside a `remember(day, isToday)`, and those
     * keys do not change between seconds, so the number froze on the value that
     * happened to be true when the screen was composed. Same for the headline
     * prayer and the "next" row beside it.
     */
    val countdownString: String = "00h 00m",

    /**
     * The reader's Hijri adjustment, in days.
     *
     * The single source for "which Hijri date is it". Every surface applies this
     * itself where it renders a date, because there is no one Hijri date in the app -
     * only one Gregorian date and one adjustment, and a Hijri month that is a
     * function of both.
     */
    val hijriAdjustment: Int = 0,
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
    val adhanNotificationEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val vibrateOnly: Boolean = false,
    val isGlobalSilentMode: Boolean = false,
    val autoSilentDuringPrayer: Boolean = false,
    val autoSilentDurationMinutes: Int = 20,
    val lastChecked: String = "Today · Synced locally",
    val isOnline: Boolean = false,
    // Expanded user preferences
    val language: String = "English",
    val riwayah: String = "Hafs 'an 'Asim",
    val appTheme: String = "System Default",
    val quranScript: String = "Uthmani (Madani)",
    val timeFormat24h: Boolean = true,
    val adhanSound: String = "Makkah Al-Mukarramah",
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

    /**
     * Which way to turn to face the Kaaba, and by how much.
     *
     * This replaced two fields - a raw signed `relativeQiblaAngle` and an
     * `isFacingQibla` boolean - because two fields carrying one fact is how they
     * come to disagree. Everything that needs to know "am I aligned" reads
     * [isFacingQibla] below, which is derived from this, and everything that needs
     * to tell the reader which way to turn reads the direction and the magnitude.
     */
    val qiblaGuidance: QiblaGuidance = QiblaGuidance(QiblaGuidance.Direction.ON_TARGET, 0),

    /**
     * A sentence about how much the compass can be trusted, already chosen.
     *
     * Chosen here, in the ViewModel, because "which of the four accuracy states is
     * this" is a decision and not a value. The raw `SENSOR_STATUS_*` integer used to
     * live in this state beside it, and that is the exact shape this rebuild spent
     * four bugs removing: a fact with a string for one reader and an integer for
     * another, where the integer can be edited into disagreement and the string
     * cannot. One representation, and it is the one the screens read.
     */
    val compassAccuracy: String = "HIGH ACCURACY",
    val magneticFieldMagnitude: Float = 46.0f, // uT
    val magneticStatus: MagneticFieldStatus = MagneticFieldStatus.OPTIMAL,
    val useTrueNorth: Boolean = true,
    val sunPosition: SunPosition? = null,
    val distanceToKaabaKm: Int = 0,
    val isDeviceLevel: Boolean = true,
    val isLocating: Boolean = false,
    val locationStatusMessage: String? = null,
    // Quran reader state
    val selectedSurah: Surah = QuranBrowse.surahs.first(),
    val currentSurahAyahs: List<Ayah> = emptyList(),

    /**
     * Where to *place* the reader when it next opens in this surah.
     *
     * A hint, and read once - it seeds `ReaderPosition` and is then the reader's to
     * own. It is not the reader's position, which lives in `ReaderPosition` and is
     * written back through the debounced `onPosition`; keeping a second, continuously
     * updated anchor here is the shape of bug this rebuild exists to prevent.
     */
    val readingAyahHint: Int = 1,
    val isAudioPlaying: Boolean = false,
    val currentAudioAyah: Int = 1,
    /**
     * The reader's own preferences, as one value.
     *
     * Held here rather than in `rememberSaveable` so the reader's layout, paper
     * and typefaces survive process death the same way every other setting in
     * this app does, and so the reading surface never briefly renders with last
     * session's values before remembering the right ones.
     */
    val quranReadingOptions: QuranReadingOptions = QuranReadingOptions(),
    /**
     * Whether the reader has taken over the whole screen.
     *
     * Read by [MainActivity] to hide the dock, which lives outside the Quran
     * destination. It is a view state rather than a reading preference, which
     * is why it is not part of [quranReadingOptions].
     */
    val isQuranImmersive: Boolean = false
) {
    /**
     * True once the Kaaba is inside the alignment window.
     *
     * Derived rather than stored. It used to be a second field written in the same
     * `copy()` as the raw angle, which is how a boolean and the number it claims
     * to summarise come to disagree - and a reader who sees a green dial beside a
     * banner that still says "turn 5°" has been told two things at once.
     *
     * It stays a named property rather than becoming `qiblaGuidance.isAligned` at
     * every call site, because "am I facing it" and "which way do I turn" are two
     * different questions and the screen asks both.
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
     *
     * The continue-reading flow re-emits every time the reader records
     * progress, so without this the resume would fight the user - pulling them
     * back to where they started every time they scrolled a line.
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
                    if (!readingPositionRestored && cont.surahNumber > 0) {
                        readingPositionRestored = true
                        selectSurah(cont.surahNumber, cont.ayahNumber)
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
        viewModelScope.launch {
            audioPlayer.playbackState.collectLatest { ps ->
                _uiState.value = _uiState.value.copy(
                    isAudioPlaying = ps.isPlaying,
                    currentAudioAyah = ps.ayahNumber
                )
            }
        }

        // Initialize Quran default Surah.
        //
        // Al-Fatihah, and deliberately overwritten a moment later by the stored
        // reading position when there is one. It has to be selected eagerly
        // because `currentSurahAyahs` starts empty and the reader renders its
        // loading state until a surah is chosen; starting on nothing is what
        // made the first frame of the reader a blank page.
        selectSurah(1, 1)
        _uiState.value = _uiState.value.copy(isQuranImmersive = repository.quranImmersive)

        // Start 1-second live ticker
        startLiveTicker()

        // Register background prayer alarm scheduling
        PrayerAlarmScheduler.scheduleAllPrayers(application)

        // Register compass sensor
        startCompass()
    }

    fun startCompass() {
        // Whether this device has a compass is not published as state. It was, and
        // nothing read it - which meant a phone with no magnetometer showed a Qibla
        // screen with a dial that would never move, and no message saying why. The
        // honest fix is not another row: a device with no magnetometer has no Qibla to
        // offer, so it should not offer one. That is a change to the Qibla tab's
        // visibility rather than to this class, and it is worth doing deliberately -
        // hiding a tab is a bigger call than showing a warning inside it, and it is the
        // next person's decision rather than mine to make silently.
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
     *
     * This used to reach into the scroll axis to repair the combination, which is
     * how choosing continuous quietly turned horizontal reading off. Layout and
     * axis are independent answers now, so this writes one field.
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
     *
     * Not a third layout: it is a question about verse presentation, answerable
     * on top of the per-page mushaf and on top of a continuous surah alike.
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
     *
     * Persisted, because the reason to want it - reading with the dock and the
     * status bar out of the way - is not something people flip on once by
     * accident. Returns the new value so the caller can react without waiting
     * for the state to round-trip.
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
    //
    // Every way of going somewhere in the Quran ends in [selectPlace], which takes
    // the one reference type. The reader, the index sheet, Continue Reading and a
    // restored session all arrive as a `(surah, ayah, page)` and are therefore
    // incapable of disagreeing about where "here" is - which is the failure that
    // produced a page indicator that did not match the page.

    /**
     * Opens a surah at a verse.
     *
     * The ayah number is read once, here, when the reader asks to be *placed*. It is
     * not published as state, because the reader does not read it: `ReaderPosition`
     * owns the reader's place and holds it through rotation, and this is what seeds it
     * on open.
     *
     * It used to also write `activeReadingAyahNumber`, which was read back as the
     * reader's *initial* position - a write that fed a value consumed once, at first
     * composition. A second anchor for "where the reader is", in a rebuild whose whole
     * subject is that there must be one. `onAyahViewed` wrote it too, on the same code
     * path, so it had two writers and neither could affect anything.
     */
    fun selectSurah(surahNumber: Int, ayahNumber: Int = 1) {
        val surah = QuranBrowse.surah(surahNumber) ?: QuranBrowse.surahs.first()
        val ayahs = QuranBrowse.ayahsInSurah(surahNumber)
        _uiState.value = _uiState.value.copy(
            selectedSurah = surah,
            currentSurahAyahs = ayahs,
            readingAyahHint = ayahNumber.coerceIn(1, ayahs.size.coerceAtLeast(1))
        )
    }

    /** Moves to a reference, whatever asked. */
    fun selectPlace(ref: QuranRef) = selectSurah(ref.surah, ref.ayah)

    fun selectPage(pageNumber: Int) = selectPlace(QuranBrowse.placeAtPage(pageNumber).verse)

    fun selectJuz(juzNumber: Int) = selectPlace(QuranBrowse.placeAtJuz(juzNumber).verse)

    fun selectHizb(hizbNumber: Int) = selectPlace(QuranBrowse.placeAtHizb(hizbNumber).verse)

    /**
     * The reader has settled on a verse; remember it for next time.
     *
     * **Persistence only.** This used to also write the reader's in-session position,
     * which was a second anchor for a fact `ReaderPosition` already owned and which
     * only ever got read once - at first composition, long before this fired. Two
     * writers, neither of which could affect anything, in a rebuild whose subject is
     * that there must be one writer.
     *
     * The surah name is taken from [ayah], not from the selected surah: they are the
     * same on every path today, and if they ever are not - a stale index sheet, a
     * position restored from an older build - the name saved with a verse is the one
     * that belongs to it. A Continue Reading row whose name contradicts its own
     * reference is worse than one with no name.
     */
    fun onAyahViewed(ayah: Ayah) {
        viewModelScope.launch {
            val surah = QuranBrowse.surah(ayah.surahNumber)
            repository.saveContinueReading(
                ContinueReadingEntity(
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    surahName = surah?.englishName.orEmpty(),
                    surahNameAr = surah?.arabicName.orEmpty(),
                    pageNumber = ayah.pageNumber,
                    snippetAr = ayah.textArabic.take(60),
                    timestamp = System.currentTimeMillis()
                )
            )
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
     *
     * Held privately, because the sentence built from it is the only representation
     * any screen reads. It used to be a field in the UI state beside that sentence -
     * two answers to one question, and the one with the raw `SENSOR_STATUS_*` constant
     * in it is the one that invites being copied over on its own.
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
