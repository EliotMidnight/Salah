package com.example.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Supported UI languages in SALAH
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false,
    val description: String
) {
    ENGLISH(
        code = "en",
        nativeName = "English",
        englishName = "English",
        isRtl = false,
        description = "Default international interface"
    ),
    ARABIC(
        code = "ar",
        nativeName = "العربية",
        englishName = "Arabic",
        isRtl = true,
        description = "واجهة عربية كاملة مع الأرقام والمصطلحات الشرعية"
    ),
    FRENCH(
        code = "fr",
        nativeName = "Français",
        englishName = "French",
        isRtl = false,
        description = "Interface complète en langue française"
    ),
    INDONESIAN(
        code = "id",
        nativeName = "Bahasa Indonesia",
        englishName = "Indonesian",
        isRtl = false,
        description = "Antarmuka lengkap Bahasa Indonesia (Waktu Sholat & Al-Qur'an)"
    ),
    TURKISH(
        code = "tr",
        nativeName = "Türkçe",
        englishName = "Turkish",
        isRtl = false,
        description = "Eksiksiz Türkçe arayüz (Namaz Vakitleri & Kur'an)"
    ),
    URDU(
        code = "ur",
        nativeName = "اردو",
        englishName = "Urdu",
        isRtl = true,
        description = "مکمل اردو انٹرفیس مع اوقاتِ نماز اور قرآنی اصطلاحات"
    ),
    MALAY(
        code = "ms",
        nativeName = "Bahasa Melayu",
        englishName = "Malay",
        isRtl = false,
        description = "Antara muka lengkap Bahasa Melayu (Waktu Solat & Al-Quran)"
    ),
    BENGALI(
        code = "bn",
        nativeName = "বাংলা",
        englishName = "Bengali",
        isRtl = false,
        description = "সম্পূর্ণ বাংলা ইন্টারফেস (নামাজের সময়সূচী ও কুরআন)"
    ),
    RUSSIAN(
        code = "ru",
        nativeName = "Русский",
        englishName = "Russian",
        isRtl = false,
        description = "Полный интерфейс на русском языке (Время намаза и Коран)"
    ),
    GERMAN(
        code = "de",
        nativeName = "Deutsch",
        englishName = "German",
        isRtl = false,
        description = "Vollständige deutsche Benutzeroberfläche (Gebetszeiten & Koran)"
    ),
    SPANISH(
        code = "es",
        nativeName = "Español",
        englishName = "Spanish",
        isRtl = false,
        description = "Interfaz completa en español (Horarios de oración y Corán)"
    );

    companion object {
        fun fromNameOrCode(nameOrCode: String): AppLanguage {
            return values().firstOrNull {
                it.nativeName.equals(nameOrCode, ignoreCase = true) ||
                it.englishName.equals(nameOrCode, ignoreCase = true) ||
                it.code.equals(nameOrCode, ignoreCase = true) ||
                it.name.equals(nameOrCode, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}

/**
 * Full UI String Dictionary Interface
 */
data class UiStrings(
    val appName: String,
    // Bottom navigation
    val navToday: String,
    val navPrayer: String,
    val navQuran: String,
    val navQibla: String,
    val navSettings: String,

    // Home / Today Screen
    val nextPrayerLabel: String,
    val adhanAt: String,
    val viewDetails: String,
    val currentPeriod: String,
    val skyPeriodSuffix: String,
    val hoursUnit: String,
    val minsUnit: String,
    val secsUnit: String,
    val todaysPrayers: String,
    val tapToMarkCompleted: String,
    val continueReading: String,
    val continueButton: String,
    val sourceLabel: String,
    val alertsActive: String,
    val silentModeOn: String,
    val masjidMode: String,
    val resetToRealtime: String,
    val onlineStatus: String,
    val offlineStatus: String,
    val syncingStatus: String,

    // Prayer names
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,

    // Prayer Screen
    val transparentCalculationSource: String,
    val todaysTimes: String,
    val prayerTimesHeader: String,
    val vigilsAndNightPeriods: String,
    val imsakTitle: String,
    val midnightTitle: String,
    val lastThirdTitle: String,
    val monthlyCalendarHeader: String,
    val todayBtn: String,

    // Qibla Screen
    val qiblaDirectionTitle: String,
    val sensoryCompassSubtitle: String,
    val magneticInterferenceDetected: String,
    val moveAwayMetal: String,
    val kaabaDistance: String,
    val trueNorth: String,
    val magneticNorth: String,
    val calibratedStatus: String,
    val facingQibla: String,
    val turnTowardsKaaba: String,

    // Quran Screen
    val surahTab: String,
    val pageTab: String,
    val juzTab: String,
    val hizbTab: String,
    val bookmarksTab: String,
    val searchSurahPlaceholder: String,
    val continueReadingBar: String,
    val versesCount: String,
    val meccan: String,
    val medinan: String,

    // Settings Screen
    val settingsTitle: String,
    val sectionGeneral: String,
    val languageLabel: String,
    val appThemeLabel: String,
    val timeFormatLabel: String,
    val sectionPrayerCalc: String,
    val locationLabel: String,
    val methodLabel: String,
    val madhhabLabel: String,
    val adjustmentsLabel: String,
    val hijriCalibrationLabel: String,
    val sectionAudioAlerts: String,
    val adhanCallLabel: String,
    val perPrayerModesLabel: String,
    val prePrayerReminderLabel: String,
    val adhanSoundLabel: String,
    val adhanVolumeLabel: String,
    val vibrationOnlyLabel: String,
    val sectionQuran: String,
    val riwayahLabel: String,
    val scriptStyleLabel: String,
    val reciterLabel: String,
    val translationLabel: String,
    val arabicTextSizeLabel: String,
    val sectionMasjidMode: String,
    val globalSilentLabel: String,
    val autoMasjidModeLabel: String,
    val autoMasjidDurationLabel: String,
    val sectionSystemDiagnostics: String,
    val compassDiagnosticsLabel: String,
    val networkSyncLabel: String,
    val resetDefaultsLabel: String,
    val privacyPhilosophyTitle: String,

    // Dialog & Common Buttons
    val cancel: String,
    val confirm: String,
    val save: String,
    val done: String,
    val close: String,

    /**
     * Copy added by the redesign.
     *
     * These are not fields on [UiStrings] on purpose. A Kotlin data class
     * generates a `copy` and a `componentN` per property, and the JVM caps a
     * method signature at 255 slots - so folding 160-odd strings into the same
     * class compiles cleanly and then dies at runtime with
     * `ClassFormatError: Too many arguments in method signature`. A second
     * data class keeps both well inside the limit.
     *
     * Read them as `strings.someLabel`. Every value here replaces an
     * English literal that used to sit directly in a layout file, which is
     * exactly why none of them could ever be translated. They are English
     * defaults; add a per-language override as each is translated.
     */
    val more: UiStringsMore = UiStringsMore(),
    val dateNav: DateNavStrings = DateNavStrings()
)

/** Copy the redesign added, kept in its own class to stay under the JVM method limit. */
/**
 * Strings for the clock page: the 24-hour dial, the windows a worshipper
 * watches for, and the day pager.
 *
 * Separate from [UiStringsMore] on purpose. That class is at 247 fields and the
 * JVM refuses a constructor with more than 255 parameters, so it cannot absorb
 * another screen's worth. A screen that needs its own copy should get its own
 * class rather than pushing the shared one over the edge.
 */
data class DateNavStrings(
    val nextPrayerPrefix: String = "Next",
    val fullHijriMonth: String = "Full Hijri month",
    val previousMonth: String = "Previous month",
    val nextMonth: String = "Next month"
)

data class UiStringsMore(
    val actionCancel: String = "Cancel",
    val actionSave: String = "Save",
    val actionClose: String = "Close",
    val actionBack: String = "Back",
    val actionReset: String = "Reset",
    val search: String = "Search",
    val clearSearch: String = "Clear search",
    val loading: String = "Loading",
    val somethingWentWrong: String = "Something went wrong",
    val tryAgain: String = "Try again",
    val todayTitle: String = "Today",
    val changeLocation: String = "Change location",
    val prayerMarkedDone: String = "Prayer marked as prayed",
    val prayerMarkedPending: String = "Prayer marked as not prayed",
    val silenceAdhan: String = "Silence",
    val adhanPlayingLabel: String = "Adhan playing",
    val sunAltitude: String = "Sun",
    val observatoryTitle: String = "Sky",
    val observatorySubtitle: String = "Astronomy for %s",
    val solarAltitude: String = "Sun altitude",
    val solarAzimuth: String = "Sun azimuth",
    val aboveHorizon: String = "Above horizon",
    val belowHorizon: String = "Below horizon",
    val trueNorthSuffix: String = "true north",
    val lunarPhase: String = "Moon phase",
    val illumination: String = "Illumination",
    val waxing: String = "Waxing",
    val waning: String = "Waning",
    val skyPeriodLabel: String = "Sky period",
    val timeScrubber: String = "Time of day",
    val previewingTime: String = "Previewing %s",
    val realTime: String = "Real time",
    val calculationSource: String = "%s · %s",
    val methodology: String = "Method",
    val madhhabLabelShort: String = "Asr method",
    val appliedAdjustments: String = "Manual adjustments",
    val computedOnDevice: String = "Computed on this device",
    val lastVerified: String = "Last checked",
    val previousDay: String = "Previous day",
    val nextDay: String = "Next day",
    val selectDate: String = "Select date",
    val minutesShort: String = "min",
    val daysShort: String = "days",
    val noAdjustment: String = "No adjustment",
    val quranTitle: String = "Quran",
    val corpusSummary: String = "114 surahs · 30 juz · 6,236 verses",
    val surahsTab: String = "Surahs",
    val noBookmarksTitle: String = "No saved verses",
    val noBookmarksMessage: String = "Tap the bookmark icon while reading to save a verse here.",
    val noSearchResults: String = "No matches",
    val searchSurahsAndVerses: String = "Search surahs and verses",
    val searchPages: String = "Search page number",
    val versesFound: String = "%d verses",
    val surahsFound: String = "%d surahs",
    val verseCount: String = "%d verses",
    val verseReference: String = "%d:%d",
    val selectSurah: String = "Select surah",
    val textSize: String = "Text size",
    val readingLayout: String = "Layout",
    val cardsLayout: String = "Per verse",
    val continuousLayout: String = "Continuous",
    val showTranslation: String = "Show translation",
    val hideTranslation: String = "Hide translation",
    val translationCredit: String = "Saheeh International",
    val readingSaved: String = "Reading position saved",
    val recitingLabel: String = "Reciting",
    val stopAudio: String = "Stop",
    val playVerse: String = "Play",
    val pauseVerse: String = "Pause",
    val bookmarkVerse: String = "Save verse",
    val removeBookmark: String = "Remove saved verse",
    val copyVerse: String = "Copy",
    val shareVerse: String = "Share",
    val verseCopied: String = "Verse copied",
    val selectVerseHint: String = "Select a verse to read it here.",
    val hizbHalfFirst: String = "1st half",
    val hizbHalfSecond: String = "2nd half",
    val juzOf: String = "Juz %d",
    val hizbOf: String = "Hizb %d",
    val hizbInJuz: String = "Juz %d, %s",
    val qiblaTitle: String = "Qibla",
    val qiblaSubtitle: String = "Direction to the Kaaba",
    val useCurrentLocation: String = "Use my location",
    val calibrateCompass: String = "Calibrate compass",
    val calibrationTitle: String = "Calibrate the compass",
    val calibrationMessage: String = "Move the device in a figure-eight pattern a few times to let the sensor settle.",
    val solarReferenceTitle: String = "Check with the sun",
    val sunAzimuth: String = "Sun direction",
    val sunAltitudeValue: String = "Sun height",
    val qiblaBearing: String = "Qibla direction",
    val sunBelowHorizon: String = "The sun is below the horizon, so it cannot be used as a reference right now.",
    val sunAligned: String = "The sun is almost in the Qibla direction. Face it to confirm.",
    val sunToTheLeft: String = "The Qibla is about %d° to the left of the sun.",
    val sunToTheRight: String = "The Qibla is about %d° to the right of the sun.",
    val sunOpposite: String = "The Qibla is in the opposite direction to the sun.",
    val magneticInterference: String = "Magnetic interference",
    val magneticInterferenceMessage: String = "Reading may be inaccurate. Move away from metal and electronics.",
    val sectionAppearance: String = "Appearance",
    val sectionLocationAndCalculation: String = "Location & calculation",
    val sectionAlerts: String = "Alerts",
    val sectionAbout: String = "About & diagnostics",
    val timeFormat24hLabel: String = "24-hour time",
    val chooseLanguage: String = "Language",
    val chooseTheme: String = "Theme",
    val themeSystem: String = "Match system",
    val themeDark: String = "Dark",
    val themeLight: String = "Light",
    val chooseLocation: String = "Location",
    val useGps: String = "Use GPS",
    val customLocation: String = "Enter manually",
    val searchCities: String = "Search cities",
    val allCountriesTitle: String = "All countries",
    val chooseMethod: String = "Calculation method",
    val chooseMadhhab: String = "Asr calculation",
    val chooseAdjustments: String = "Minute adjustments",
    val resetAdjustments: String = "Reset all adjustments",
    val chooseHijriOffset: String = "Hijri date offset",
    val chooseAdhanSound: String = "Adhan sound",
    val chooseReciter: String = "Reciter",
    val chooseRiwayah: String = "Riwayah",
    val chooseScript: String = "Script",
    val chooseTranslation: String = "Translation",
    val choosePrePrayerOffset: String = "Reminder before prayer",
    val prePrayerDisabled: String = "Off",
    val perPrayerModes: String = "Alert per prayer",
    val adhanVolume: String = "Volume",
    val testSound: String = "Test sound",
    val stopSound: String = "Stop sound",
    val silentModeLabel: String = "Silence all alerts",
    val autoSilenceLabel: String = "Auto-silence during prayer",
    val autoSilenceDurationLabel: String = "Auto-silence duration",
    val vibrateOnlyLabel: String = "Vibrate only",
    val adhanAtPrayerLabel: String = "Adhan at prayer time",
    val ephemerisCacheLabel: String = "Prayer schedule cache",
    val recomputeSchedule: String = "Recompute 365-day schedule",
    val copyTodaySchedule: String = "Copy today's times",
    val scheduleCopied: String = "Times copied",
    val storageLabel: String = "Storage",
    val audioSourceLabel: String = "Recitation audio",
    val audioStreamedNotCached: String = "Streamed, not stored on device",
    val audioCacheCleared: String = "Audio cache cleared",
    val sensorAccuracy: String = "Sensor accuracy",
    val ambientField: String = "Ambient field",
    val checkForUpdates: String = "Check now",
    val resetAllLabel: String = "Reset all settings",
    val resetAllConfirmTitle: String = "Reset all settings?",
    val resetAllConfirmMessage: String = "Calculation, alert and display settings return to their defaults. Your saved verses and prayer log are kept.",
    val settingsReset: String = "Settings reset",
    val nameField: String = "Place name",
    val latitudeField: String = "Latitude",
    val longitudeField: String = "Longitude",
    val invalidCoordinates: String = "Enter a latitude between -90 and 90, and a longitude between -180 and 180.",
    val nameRequired: String = "Enter a name for this place.",
    val locationSaved: String = "Location saved",
    val hizbWord: String = "Hizb",
    val previousSurahLabel: String = "Previous surah",
    val nextSurahLabel: String = "Next surah",
    val isFacingQibla: String = "You are facing the Qibla",
    val rightOfQibla: String = "to the right",
    val leftOfQibla: String = "to the left",
    /** %1$s is a formatted angle such as "12°", %2$s a direction phrase. */
    val turnBy: String = "Turn %1\$s %2\$s",
    val alignedWithQibla: String = "Aligned with the Qibla",
    val locateMe: String = "Locate",
    val gpsCached: String = "GPS cached",
    val selectedCity: String = "Selected city",
    val coordinatesCachedOffline: String =
        "Coordinates cached offline. Calculations run entirely on this device.",
    /**
     * Shown under the dial while the phone is tilted. The heading is only reliable
     * with the phone held flat, so this is an instruction, not a decoration.
     */
    val holdFlatHint: String = "Hold the phone flat for an accurate compass reading.",
    val northReferenceLabel: String = "North reference",
    /** %1$s is the live heading such as "95° E", %2$s the Qibla bearing "96°". */
    val dialDescription: String = "Compass dial, heading %1\$s. Qibla %2\$s.",
    val juzWord: String = "Juz’",
    val pageWord: String = "Page",
    val privacyNote: String = "Prayer times, the Qibla direction and the Quran are all calculated on this device. Nothing is uploaded.",
    val privacyPolicy: String = "Privacy policy",
    val referenceTab: String = "Reference",
    val allSurahsLabel: String = "All surahs",
    val verseOf: String = "Verse %1\$d of %2\$d",
    val searchHintTitle: String = "Search the Quran",
    val searchHintMessage: String = "Find a surah by name or meaning, or a verse by its Arabic or English text.",
    val surahLabel: String = "Surah",
    val selected: String = "Selected",
    val notSelected: String = "Not selected",
    val nowReading: String = "Now reading",
    val loadingQuranMessage: String =
        "Preparing the text. If this does not finish, go back and pick the surah again.",
    val noSurahMatchMessage: String =
        "No surah matches that. Try a number, or part of a name.",
    val noResultsMessage: String =
        "Nothing matched. Try a different spelling, or a shorter query.",
    val versesLabel: String = "Verses",
    val searchJuz: String = "Jump to juz number",
    val searchHizb: String = "Jump to hizb number",
    val alertAdhan: String = "Adhan",
    val alertTakbeer: String = "Takbeer",
    val alertChime: String = "Chime",
    val alertVibrate: String = "Vibrate",
    val alertSilent: String = "Silent",
    val alertSilentReminder: String = "Silent reminder",
    val stateOn: String = "On",
    val stateOff: String = "Off",
    val actionChange: String = "Change",
    val changeAlertMode: String = "Change alert",
    val allowNotificationsTitle: String = "Let Salah alert you",
    val allowNotificationsMessage: String =
        "Salah needs permission to show notifications so it can call you at prayer " +
            "times. Prayer times stay on the Today screen either way, and you can " +
            "turn alerts on later in Settings.",
    val allowNotificationsAction: String = "Allow notifications",
    val notNow: String = "Not now",
    val juzLabel: String = "Juz\u2019",
    val selectLayoutTitle: String = "Reading layout",
    val selectLayoutSubtitle: String = "Per verse suits study; continuous suits reading straight through.",
    val layoutPerVerse: String = "Per verse",
    val layoutContinuous: String = "Continuous",
    val verseActionsLabel: String = "Verse actions",
    val readingOptions: String = "Reading options",
    val closeReader: String = "Back to surahs",
    val referenceLabel: String = "Reference",
    val translationShownFor: String = "Showing %s",
    val selectVerse: String = "Select verse",
    val cardsModeLabel: String = "Cards",
    val continuousModeLabel: String = "Continuous",
    val readingSettingsLabel: String = "Reading settings",
    val backToSurahsLabel: String = "Back to surahs",
    val copyVerseLabel: String = "Copy verse",
    val shareVerseLabel: String = "Share verse",
    val verseCopiedToast: String = "Verse copied",
    val shareChooserTitle: String = "Share verse via",
    val translationSectionTitle: String = "Translation",
    val showTranslationLabel: String = "Show",
    val hideTranslationLabel: String = "Hide",
    val translationCreditLine: String = "English — Saheeh International",
    val tapVerseHint: String = "Tap a verse to inspect it",
    val searchVersesHint: String = "Search Arabic text or English translation...",
    val translationNotAvailable: String = "Translation not available"
)

val EnglishStrings = UiStrings(
    appName = "SALAH",
    navToday = "Today",
    navPrayer = "Prayer",
    navQuran = "Quran",
    navQibla = "Qibla",
    navSettings = "Settings",

    nextPrayerLabel = "NEXT PRAYER",
    adhanAt = "Adhan at",
    viewDetails = "View details",
    currentPeriod = "Current period",
    skyPeriodSuffix = "Sky ✦",
    hoursUnit = "HOURS",
    minsUnit = "MINS",
    secsUnit = "SECS",
    todaysPrayers = "Today's Prayers",
    tapToMarkCompleted = "Tap to mark completed",
    continueReading = "CONTINUE READING",
    continueButton = "Continue",
    sourceLabel = "Source",
    alertsActive = "Alerts Active",
    silentModeOn = "Silent Mode ON",
    masjidMode = "Masjid Mode",
    resetToRealtime = "Reset to Real-Time Sky",
    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",
    syncingStatus = "SYNCING",

    fajr = "Fajr",
    sunrise = "Sunrise",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Transparent Calculation Source",
    todaysTimes = "TODAY'S TIMES",
    prayerTimesHeader = "PRAYER TIMES",
    vigilsAndNightPeriods = "VIGILS & NIGHT PERIODS",
    imsakTitle = "Imsak (10 min before Fajr)",
    midnightTitle = "Islamic Midnight",
    lastThirdTitle = "Last Third of the Night (Tahajjud)",
    monthlyCalendarHeader = "MONTHLY CALENDAR",
    todayBtn = "Today",

    qiblaDirectionTitle = "Qibla Direction",
    sensoryCompassSubtitle = "Sensory compass pointing to the Kaaba",
    magneticInterferenceDetected = "Magnetic Interference Detected",
    moveAwayMetal = "Move away from metallic objects or tap for calibration.",
    kaabaDistance = "Distance to Kaaba",
    trueNorth = "True North",
    magneticNorth = "Magnetic North",
    calibratedStatus = "HIGH ACCURACY",
    facingQibla = "Facing the Kaaba",
    turnTowardsKaaba = "Turn towards the Kaaba",

    surahTab = "Surah",
    pageTab = "Page",
    juzTab = "Juz'",
    hizbTab = "Hizb",
    bookmarksTab = "Bookmarks",
    searchSurahPlaceholder = "Search Surah by name or number...",
    continueReadingBar = "Continue Reading",
    versesCount = "verses",
    meccan = "Meccan",
    medinan = "Medinan",

    settingsTitle = "Settings & Configuration",
    sectionGeneral = "GENERAL & LOCALIZATION",
    languageLabel = "Language",
    appThemeLabel = "App Theme",
    timeFormatLabel = "24-Hour Time Format",
    sectionPrayerCalc = "PRAYER TIMES & CALCULATION",
    locationLabel = "Location",
    methodLabel = "Calculation Method",
    madhhabLabel = "Asr Juristic Madhhab",
    adjustmentsLabel = "Manual Minute Adjustments",
    hijriCalibrationLabel = "Hijri Date Calibration",
    sectionAudioAlerts = "AUDIO ADHAN & NOTIFICATIONS",
    adhanCallLabel = "Adhan Call at Prayer Time",
    perPrayerModesLabel = "Per-Prayer Alert Modes",
    prePrayerReminderLabel = "Pre-Prayer Reminder Timing",
    adhanSoundLabel = "Adhan Recitation Voice",
    adhanVolumeLabel = "Adhan Volume & Audio Tester",
    vibrationOnlyLabel = "Vibration Only (Silent)",
    sectionQuran = "THE NOBLE QURAN & RECITATION",
    riwayahLabel = "Riwāyāt (Recitation Tradition)",
    scriptStyleLabel = "Quran Script Style",
    reciterLabel = "Audio Reciter",
    translationLabel = "Translation & Exegesis",
    arabicTextSizeLabel = "Quran Arabic Text Size",
    sectionMasjidMode = "MASJID MODE & DISTURBANCE PROTECTION",
    globalSilentLabel = "Global Silent Mode",
    autoMasjidModeLabel = "Auto Masjid Silence Mode",
    autoMasjidDurationLabel = "Silence Duration During Prayer",
    sectionSystemDiagnostics = "SYSTEM & DIAGNOSTICS",
    compassDiagnosticsLabel = "Compass Sensors & Diagnostics",
    networkSyncLabel = "Network Synchronization & Source",
    resetDefaultsLabel = "Reset Preferences to Factory Defaults",
    privacyPhilosophyTitle = "Privacy & Philosophy",

    cancel = "Cancel",
    confirm = "Confirm",
    save = "Save",
    done = "Done",
    close = "Close"
)

val ArabicStrings = UiStrings(
    appName = "صلاة",
    navToday = "اليوم",
    navPrayer = "الصلاة",
    navQuran = "القرآن",
    navQibla = "القبلة",
    navSettings = "الإعدادات",

    nextPrayerLabel = "الصلاة القادمة",
    adhanAt = "الأذان في",
    viewDetails = "عرض التفاصيل",
    currentPeriod = "الوقت الحالي",
    skyPeriodSuffix = "سماء ✦",
    hoursUnit = "ساعة",
    minsUnit = "دقيقة",
    secsUnit = "ثانية",
    todaysPrayers = "صلوات اليوم",
    tapToMarkCompleted = "اضغط للتعليم عند الأداء",
    continueReading = "متابعة التلاوة",
    continueButton = "متابعة",
    sourceLabel = "المصدر",
    alertsActive = "التنبيهات مفعّلة",
    silentModeOn = "الوضع الصامت مفعّل",
    masjidMode = "وضع المسجد",
    resetToRealtime = "العودة للوقت الفعلي",
    onlineStatus = "متصل",
    offlineStatus = "بدون اتصال",
    syncingStatus = "مزامنة",

    fajr = "الفجر",
    sunrise = "الشروق",
    dhuhr = "الظهر",
    asr = "العصر",
    maghrib = "المغرب",
    isha = "العشاء",

    transparentCalculationSource = "مصدر الحساب الشرعي الشفاف",
    todaysTimes = "أوقات اليوم",
    prayerTimesHeader = "مواقيت الصلاة",
    vigilsAndNightPeriods = "قيام الليل وأوقات السحر",
    imsakTitle = "الإمساك (١٠ دقائق قبل الفجر)",
    midnightTitle = "منتصف الليل الشرعي",
    lastThirdTitle = "الثلث الأخير من الليل (التهجد)",
    monthlyCalendarHeader = "التقويم الشهري",
    todayBtn = "اليوم",

    qiblaDirectionTitle = "اتجاه القبلة",
    sensoryCompassSubtitle = "بوصلة مستشعرات دقيقة نحو الكعبة المشرفة",
    magneticInterferenceDetected = "تم رصد تشويش مغناطيسي",
    moveAwayMetal = "ابتعد عن الأجسام المعدنية أو اضغط للمعايرة.",
    kaabaDistance = "المسافة إلى الكعبة",
    trueNorth = "الشمال الحقيقي",
    magneticNorth = "الشمال المغناطيسي",
    calibratedStatus = "دقة عالية",
    facingQibla = "أنت باتجاه القبلة تماماً",
    turnTowardsKaaba = "استدر نحو الكعبة المشرفة",

    surahTab = "السورة",
    pageTab = "الصفحة",
    juzTab = "الجزء",
    hizbTab = "الحزب",
    bookmarksTab = "الإشارات",
    searchSurahPlaceholder = "ابحث عن السورة بالاسم أو الرقم...",
    continueReadingBar = "متابعة القراءة",
    versesCount = "آيات",
    meccan = "مكية",
    medinan = "مدنية",

    settingsTitle = "الإعدادات والخيارات",
    sectionGeneral = "عام واللغة",
    languageLabel = "اللغة",
    appThemeLabel = "مظهر التطبيق",
    timeFormatLabel = "صيغة ٢٤ ساعة",
    sectionPrayerCalc = "مواقيت الصلاة والحساب",
    locationLabel = "الموقع الجغرافي",
    methodLabel = "طريقة الحساب",
    madhhabLabel = "المذهب الفقهي (العصر)",
    adjustmentsLabel = "تعديل الدقائق يدوياً",
    hijriCalibrationLabel = "معايرة التاريخ الهجري",
    sectionAudioAlerts = "صوت الأذان والإشعارات",
    adhanCallLabel = "نداء الأذان عند دخول الوقت",
    perPrayerModesLabel = "أوضاع التنبيه لكل صلاة",
    prePrayerReminderLabel = "تذكير قبل الصلاة",
    adhanSoundLabel = "صوت المؤذن",
    adhanVolumeLabel = "مستوى الصوت وتجربة الأذان",
    vibrationOnlyLabel = "اهتزاز فقط (صامت)",
    sectionQuran = "القرآن الكريم والتلاوة",
    riwayahLabel = "الرواية القرآنية",
    scriptStyleLabel = "رسم المصحف الشريف",
    reciterLabel = "القارئ الصوتي",
    translationLabel = "الترجمة والتفسير",
    arabicTextSizeLabel = "حجم الخط القرآني",
    sectionMasjidMode = "وضع المسجد وحماية الخشوع",
    globalSilentLabel = "الوضع الصامت العام",
    autoMasjidModeLabel = "الوضع الصامت التلقائي للصلاة",
    autoMasjidDurationLabel = "مدة الصمت أثناء الصلاة",
    sectionSystemDiagnostics = "النظام والمستشعرات",
    compassDiagnosticsLabel = "مستشعرات البوصلة والتشخيص",
    networkSyncLabel = "المزامنة والمصدر",
    resetDefaultsLabel = "إعادة ضبط المصنع",
    privacyPhilosophyTitle = "الخصوصية والمنهج",

    cancel = "إلغاء",
    confirm = "تأكيد",
    save = "حفظ",
    done = "تم",
    close = "إغلاق",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "الشهر الهجري كاملًا", // verify
            nextMonth = "الشهر التالي", // verify
            nextPrayerPrefix = "التالي", // verify
            previousMonth = "الشهر السابق", // verify
    ),
    more = UiStringsMore(
        actionSave = "حفظ",
        actionCancel = "إلغاء",
        actionClose = "إغلاق",
        actionBack = "رجوع",
        actionReset = "إعادة تعيين",
        search = "بحث",
        clearSearch = "مسح البحث",
        loading = "جارٍ التحميل",
        surahsTab = "السور",
        referenceTab = "الفهرس",
        playVerse = "تشغيل الآية",
        pauseVerse = "إيقاف مؤقت",
        bookmarkVerse = "أضف علامة",
        removeBookmark = "إزالة العلامة",
        copyVerse = "نسخ الآية",
        shareVerse = "مشاركة الآية",
        verseCopied = "تم نسخ الآية",
        textSize = "حجم النص",
        layoutPerVerse = "آية آية",
        layoutContinuous = "متصل",
        backToSurahsLabel = "العودة إلى السور",
        sectionAppearance = "المظهر",
        sectionLocationAndCalculation = "الموقع والحساب",
        sectionAlerts = "التنبيهات",
        sectionAbout = "حول التطبيق",
        chooseLocation = "اختر الموقع",
        useGps = "استخدام GPS",
        customLocation = "إدخال يدويًا",
        searchCities = "ابحث عن مدينة",
        allCountriesTitle = "كل الدول",
        nameField = "اسم المكان",
        latitudeField = "خط العرض",
        longitudeField = "خط الطول",
        nameRequired = "أدخل اسمًا لهذا المكان.",
        invalidCoordinates = "أدخل خط عرض بين -90 و90، وخط طول بين -180 و180.",
        qiblaBearing = "اتجاه القبلة",
        isFacingQibla = "أنت متجه نحو القبلة",
        rightOfQibla = "إلى اليمين",
        leftOfQibla = "إلى اليسار",
        turnBy = "استدر %1\$s %2\$s",
        alignedWithQibla = "مستقيم مع القبلة",
        locateMe = "تحديد موقعي",
        holdFlatHint = "أمسك الهاتف مستويًا للحصول على قراءة دقيقة للبوصلة.",
        northReferenceLabel = "المرجع الشمالي",
        dialDescription = "بوصلة، الاتجاه الحالي %1\$s. القبلة %2\$s.",
        solarReferenceTitle = "تحقق من الشمس",
        calibrationTitle = "معايرة البوصلة",
        magneticInterference = "تشويش مغناطيسي",
        magneticInterferenceMessage = "قد تكون القراءة غير دقيقة. ابتعد عن المعادن والأجهزة الإلكترونية.",
        alertAdhan = "أذان",
        alertTakbeer = "تكبير",
        alertChime = "نغمة",
        alertVibrate = "اهتزاز",
        alertSilent = "صامت",
        changeAlertMode = "تغيير التنبيه",
        selected = "محدد",
        notSelected = "غير محدد",
        stateOn = "مُفعّل",
        stateOff = "مُعطّل",
        noSearchResults = "لا توجد نتائج",
        noResultsMessage = "جرّب تهجئة أخرى أو كلمة أقصر.",
        allowNotificationsTitle = "اسمح لصلاة بتنبيهك",
        allowNotificationsMessage = "تحتاج صلاة إلى إذن الإشعارات لتناديك في أوقات الصلاة. تبقى مواقيت الصلاة معروضة في الشاشة الرئيسية على أي حال، ويمكنك تفعيل التنبيهات لاحقًا من الإعدادات.",
        allowNotificationsAction = "السماح بالإشعارات",
        notNow = "ليس الآن",
        aboveHorizon = "فوق الأفق",
        actionChange = "تغيير",
        adhanAtPrayerLabel = "الأذان عند وقت الصلاة", // verify
        adhanPlayingLabel = "الأذان يعمل", // verify
        adhanVolume = "مستوى الصوت", // verify
        alertSilentReminder = "تذكير صامت", // verify
        allSurahsLabel = "جميع السور", // verify
        ambientField = "المجال المحيط",
        appliedAdjustments = "التعديلات اليدوية", // verify
        audioCacheCleared = "تم مسح ذاكرة الصوت المؤقتة", // verify
        audioSourceLabel = "صوت التلاوة", // verify
        audioStreamedNotCached = "يُبثّ ولا يُخزَّن على الجهاز", // verify
        autoSilenceDurationLabel = "مدة الكتم التلقائي", // verify
        autoSilenceLabel = "كتم تلقائي أثناء الصلاة", // verify
        belowHorizon = "تحت الأفق",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "معايرة البوصلة",
        calibrationMessage = "حرّك الجهاز على شكل الرقم ثمانية عدة مرات لتهدئة المستشعر.",
        cardsLayout = "لكل آية", // verify
        cardsModeLabel = "بطاقات", // verify
        changeLocation = "تغيير الموقع",
        checkForUpdates = "تحقق الآن",
        chooseAdhanSound = "صوت الأذان", // verify
        chooseAdjustments = "تعديلات بالدقائق", // verify
        chooseHijriOffset = "إزاحة التاريخ الهجري", // verify
        chooseLanguage = "اللغة",
        chooseMadhhab = "حساب العصر", // verify
        chooseMethod = "طريقة الحساب", // verify
        choosePrePrayerOffset = "تذكير قبل الصلاة", // verify
        chooseReciter = "القارئ", // verify
        chooseRiwayah = "الرواية", // verify
        chooseScript = "الخط", // verify
        chooseTheme = "المظهر",
        chooseTranslation = "الترجمة", // verify
        closeReader = "العودة إلى السور", // verify
        computedOnDevice = "يُحسب على هذا الجهاز", // verify
        continuousLayout = "متصل", // verify
        continuousModeLabel = "متصل", // verify
        coordinatesCachedOffline = "تم حفظ الإحداثيات دون اتصال. تتم الحسابات بالكامل على هذا الجهاز.",
        copyTodaySchedule = "نسخ أوقات اليوم", // verify
        copyVerseLabel = "نسخ الآية", // verify
        corpusSummary = "114 سورة · 30 جزءًا · 6236 آية", // verify
        daysShort = "يوم", // verify
        ephemerisCacheLabel = "ذاكرة جدول الصلاة المؤقتة", // verify
        gpsCached = "تم حفظ موقع GPS",
        hideTranslation = "إخفاء الترجمة", // verify
        hideTranslationLabel = "إخفاء", // verify
        hizbHalfFirst = "النصف الأول", // verify
        hizbHalfSecond = "النصف الثاني", // verify
        hizbInJuz = "الجزء %d، %s", // verify
        hizbOf = "الحزب %d", // verify
        hizbWord = "الحزب", // verify
        illumination = "الإضاءة",
        juzLabel = "جزء", // verify
        juzOf = "الجزء %d", // verify
        juzWord = "الجزء", // verify
        lastVerified = "آخر فحص",
        loadingQuranMessage = "جارٍ تحضير النص. إذا لم ينتهِ، عُد واختر السورة مجددًا.",
        locationSaved = "تم حفظ الموقع",
        lunarPhase = "طور القمر", // verify
        madhhabLabelShort = "طريقة العصر", // verify
        methodology = "الطريقة", // verify
        minutesShort = "د", // verify
        nextDay = "اليوم التالي", // verify
        nextSurahLabel = "السورة التالية", // verify
        noAdjustment = "بدون تعديل", // verify
        noBookmarksMessage = "اضغط أيقونة الحفظ أثناء القراءة لحفظ آية هنا.", // verify
        noBookmarksTitle = "لا آيات محفوظة", // verify
        noSurahMatchMessage = "لا سورة تطابق ذلك. جرّب رقمًا أو جزءًا من ال�م.",
        nowReading = "يُقرأ الآن", // verify
        observatorySubtitle = "علم الفلك لـ %s",
        observatoryTitle = "السماء",
        pageWord = "صفحة", // verify
        perPrayerModes = "تنبيه لكل صلاة", // verify
        prayerMarkedDone = "تم تعليم الصلاة كصلاة مؤداة", // verify
        prayerMarkedPending = "تم تعليم الصلاة كغير مؤداة", // verify
        prePrayerDisabled = "مغلق", // verify
        previewingTime = "معاينة %s", // verify
        previousDay = "اليوم السابق", // verify
        previousSurahLabel = "السورة السابقة", // verify
        privacyNote = "تُحسب أوقات الصلاة واتجاه القبلة والقرآن على هذا الجهاز. لا يُرفع أي شيء.",
        privacyPolicy = "سياسة الخصوصية",
        qiblaSubtitle = "الاتجاه إلى الكعبة", // verify
        qiblaTitle = "القبلة", // verify
        quranTitle = "القرآن", // verify
        readingLayout = "التخطيط", // verify
        readingOptions = "خيارات القراءة", // verify
        readingSaved = "تم حفظ موضع القراءة", // verify
        readingSettingsLabel = "إعدادات القراءة", // verify
        realTime = "الوقت الفعلي", // verify
        recitingLabel = "يتلو", // verify
        recomputeSchedule = "إعادة حساب جدول 365 يومًا", // verify
        referenceLabel = "المرجع", // verify
        resetAdjustments = "إعادة ضبط كل التعديلات", // verify
        resetAllConfirmMessage = "تعود إعدادات الحساب والتنبيه والعرض إلى قيمها الافتراضية. تبقى آياتك المحفوظة وسجل صلاتك.",
        resetAllConfirmTitle = "إعادة ضبط كل الإعدادات؟",
        resetAllLabel = "إعادة ضبط كل الإعدادات",
        scheduleCopied = "تم نسخ الأوقات", // verify
        searchHintMessage = "ابحث عن سورة بالاسم أو المعنى، أو عن آية بنصها العربي أو الإنجليزي.", // verify
        searchHintTitle = "البحث في القرآن", // verify
        searchHizb = "الانتقال إلى رقم الحزب", // verify
        searchJuz = "الانتقال إلى رقم الجزء", // verify
        searchPages = "البحث برقم الصفحة", // verify
        searchSurahsAndVerses = "البحث في السور والآيات", // verify
        searchVersesHint = "ابحث في النص العربي أو الترجمة الإنجليزية...", // verify
        selectDate = "اختر التاريخ", // verify
        selectLayoutSubtitle = "لكل آية يناسب الدراسة؛ المتصل يناسب القراءة المتواصلة.", // verify
        selectLayoutTitle = "تخطيط القراءة", // verify
        selectSurah = "اختر السورة", // verify
        selectVerse = "اختر الآية", // verify
        selectVerseHint = "اختر آية لقراءتها هنا.", // verify
        selectedCity = "المدينة المحددة",
        sensorAccuracy = "دقة المستشعر",
        settingsReset = "تمت إعادة ضبط الإعدادات",
        shareChooserTitle = "مشاركة الآية عبر",
        shareVerseLabel = "مشاركة الآية", // verify
        showTranslation = "إظهار الترجمة", // verify
        showTranslationLabel = "إظهار", // verify
        silenceAdhan = "كتم", // verify
        silentModeLabel = "كتم كل التنبيهات", // verify
        skyPeriodLabel = "فترة السماء",
        solarAltitude = "ارتفاع الشمس",
        solarAzimuth = "اتجاه الشمس",
        somethingWentWrong = "حدث خطأ ما",
        stopAudio = "إيقاف", // verify
        stopSound = "إيقاف الصوت",
        storageLabel = "التخزين",
        sunAligned = "الشمس شبه متجهة نحو القبلة. استقبلها للتأكد.", // verify
        sunAltitude = "الشمس",
        sunAltitudeValue = "ارتفاع الشمس",
        sunAzimuth = "اتجاه الشمس",
        sunBelowHorizon = "الشمس تحت الأفق، لذا لا يمكن استخدامها كمرجع الآن.",
        sunOpposite = "القبلة في الاتجاه المعاكس للشمس.", // verify
        sunToTheLeft = "القبلة نحو %d° يسار الشمس.", // verify
        sunToTheRight = "القبلة نحو %d° يمين الشمس.", // verify
        surahLabel = "سورة", // verify
        surahsFound = "%d سورة", // verify
        tapVerseHint = "اضغط على آية لفحصها", // verify
        testSound = "تجربة الصوت", // verify
        themeDark = "داكن",
        themeLight = "فاتح",
        themeSystem = "مطابقة النظام",
        timeFormat24hLabel = "الوقت بنظام 24 ساعة", // verify
        timeScrubber = "وقت اليوم", // verify
        todayTitle = "اليوم", // verify
        translationCredit = "صحيح إنترناشيونال", // verify
        translationCreditLine = "الإنجليزية — صحيح إنترناشيونال", // verify
        translationNotAvailable = "الترجمة غير متاحة", // verify
        translationSectionTitle = "الترجمة", // verify
        translationShownFor = "يُعرض %s", // verify
        trueNorthSuffix = "الشمال الحقيقي", // verify
        tryAgain = "حاول مجددًا",
        useCurrentLocation = "استخدام موقعي",
        verseActionsLabel = "إجراءات الآية", // verify
        verseCopiedToast = "تم نسخ الآية", // verify
        verseCount = "%d آية", // verify
        verseOf = "الآية %1\$d من %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d آية", // verify
        versesLabel = "آيات", // verify
        vibrateOnlyLabel = "اهتزاز فقط", // verify
        waning = "متناقص", // verify
        waxing = "متزايد", // verify
    ),
)

val FrenchStrings = UiStrings(
    appName = "SALAH",
    navToday = "Aujourd'hui",
    navPrayer = "Prière",
    navQuran = "Coran",
    navQibla = "Qibla",
    navSettings = "Paramètres",

    nextPrayerLabel = "PROCHAINE PRIÈRE",
    adhanAt = "Adhan à",
    viewDetails = "Voir détails",
    currentPeriod = "Période actuelle",
    skyPeriodSuffix = "Ciel ✦",
    hoursUnit = "HEURES",
    minsUnit = "MIN",
    secsUnit = "SEC",
    todaysPrayers = "Prières d'aujourd'hui",
    tapToMarkCompleted = "Appuyez pour marquer accomplie",
    continueReading = "CONTINUER LA LECTURE",
    continueButton = "Continuer",
    sourceLabel = "Source",
    alertsActive = "Alertes actives",
    silentModeOn = "Mode silencieux ACTIF",
    masjidMode = "Mode Mosquée",
    resetToRealtime = "Retour au temps réel",
    onlineStatus = "EN LIGNE",
    offlineStatus = "HORS LIGNE",
    syncingStatus = "SYNCHRO",

    fajr = "Fajr",
    sunrise = "Lever du soleil",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Source de calcul transparente",
    todaysTimes = "HORAIRES DU JOUR",
    prayerTimesHeader = "HORAIRES DE PRIÈRE",
    vigilsAndNightPeriods = "VEILLES & PÉRIODES DE NUIT",
    imsakTitle = "Imsak (10 min avant Fajr)",
    midnightTitle = "Minuit islamique",
    lastThirdTitle = "Dernier tiers de la nuit (Tahajjud)",
    monthlyCalendarHeader = "CALENDRIER MENSUEL",
    todayBtn = "Aujourd'hui",

    qiblaDirectionTitle = "Direction de la Qibla",
    sensoryCompassSubtitle = "Boussole sensorielle orientée vers la Kaaba",
    magneticInterferenceDetected = "Interférence magnétique détectée",
    moveAwayMetal = "Éloignez-vous des métaux ou calibrez.",
    kaabaDistance = "Distance à la Kaaba",
    trueNorth = "Nord géographique",
    magneticNorth = "Nord magnétique",
    calibratedStatus = "HAUTE PRÉCISION",
    facingQibla = "Face à la Kaaba",
    turnTowardsKaaba = "Tournez-vous vers la Kaaba",

    surahTab = "Sourate",
    pageTab = "Page",
    juzTab = "Juz'",
    hizbTab = "Hizb",
    bookmarksTab = "Signets",
    searchSurahPlaceholder = "Rechercher par nom ou numéro...",
    continueReadingBar = "Reprendre la lecture",
    versesCount = "versets",
    meccan = "Mecquoise",
    medinan = "Médinoise",

    settingsTitle = "Paramètres & Configuration",
    sectionGeneral = "GÉNÉRAL & LOCALISATION",
    languageLabel = "Langue",
    appThemeLabel = "Thème de l'application",
    timeFormatLabel = "Format horaire 24h",
    sectionPrayerCalc = "HORAIRES & CALCULS",
    locationLabel = "Emplacement",
    methodLabel = "Méthode de calcul",
    madhhabLabel = "École juridique (Asr)",
    adjustmentsLabel = "Ajustements manuels (minutes)",
    hijriCalibrationLabel = "Calibrage calendrier hégirien",
    sectionAudioAlerts = "AUDIO ADHAN & NOTIFICATIONS",
    adhanCallLabel = "Appel à la prière à l'heure",
    perPrayerModesLabel = "Modes d'alerte par prière",
    prePrayerReminderLabel = "Rappel avant la prière",
    adhanSoundLabel = "Voix de l'Adhan",
    adhanVolumeLabel = "Volume et test audio de l'Adhan",
    vibrationOnlyLabel = "Vibreur seul (Silencieux)",
    sectionQuran = "LE SAINT CORAN & RÉCITATION",
    riwayahLabel = "Riwāyah (Tradition de récitation)",
    scriptStyleLabel = "Style de calligraphie coranique",
    reciterLabel = "Récitateur audio",
    translationLabel = "Traduction & Exégèse",
    arabicTextSizeLabel = "Taille du texte arabe",
    sectionMasjidMode = "MODE MOSQUÉE & TRANQUILLITÉ",
    globalSilentLabel = "Mode silencieux global",
    autoMasjidModeLabel = "Mode silencieux auto prière",
    autoMasjidDurationLabel = "Durée du silence pendant la prière",
    sectionSystemDiagnostics = "SYSTÈME & CAPTEURS",
    compassDiagnosticsLabel = "Capteurs boussole & diagnostic",
    networkSyncLabel = "Synchronisation réseau & source",
    resetDefaultsLabel = "Rétablir les paramètres par défaut",
    privacyPhilosophyTitle = "Confidentialité & Philosophie",

    cancel = "Annuler",
    confirm = "Confirmer",
    save = "Enregistrer",
    done = "Terminé",
    close = "Fermer",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Mois hégirien complet", // verify
            nextMonth = "Mois suivant", // verify
            nextPrayerPrefix = "Suivante", // verify
            previousMonth = "Mois précédent", // verify
    ),
    more = UiStringsMore(
        actionSave = "Enregistrer",
        actionCancel = "Annuler",
        actionClose = "Fermer",
        actionBack = "Retour",
        actionReset = "Réinitialiser",
        search = "Rechercher",
        clearSearch = "Effacer la recherche",
        loading = "Chargement",
        surahsTab = "Sourates",
        referenceTab = "Référence",
        playVerse = "Lire le verset",
        pauseVerse = "Mettre en pause",
        bookmarkVerse = "Ajouter un signet",
        removeBookmark = "Retirer le signet",
        copyVerse = "Copier le verset",
        shareVerse = "Partager le verset",
        verseCopied = "Verset copié",
        textSize = "Taille du texte",
        layoutPerVerse = "Verset par verset",
        layoutContinuous = "Continu",
        backToSurahsLabel = "Retour aux sourates",
        sectionAppearance = "Apparence",
        sectionLocationAndCalculation = "Localisation et calcul",
        sectionAlerts = "Alertes",
        sectionAbout = "À propos",
        chooseLocation = "Choisir un emplacement",
        useGps = "Utiliser le GPS",
        customLocation = "Saisir manuellement",
        searchCities = "Rechercher une ville",
        allCountriesTitle = "Tous les pays",
        nameField = "Nom du lieu",
        latitudeField = "Latitude",
        longitudeField = "Longitude",
        nameRequired = "Saisissez un nom pour ce lieu.",
        invalidCoordinates = "Saisissez une latitude entre -90 et 90, et une longitude entre -180 et 180.",
        qiblaBearing = "Direction de la Qibla",
        isFacingQibla = "Vous faites face à la Qibla",
        rightOfQibla = "vers la droite",
        leftOfQibla = "vers la gauche",
        turnBy = "Tournez de %1\$s %2\$s",
        alignedWithQibla = "Aligné sur la Qibla",
        locateMe = "Me localiser",
        holdFlatHint = "Tenez le téléphone à plat pour une lecture fiable de la boussole.",
        northReferenceLabel = "Référence nord",
        dialDescription = "Boussole, cap %1\$s. Qibla %2\$s.",
        solarReferenceTitle = "Vérifier avec le soleil",
        calibrationTitle = "Calibrer la boussole",
        magneticInterference = "Interférence magnétique",
        magneticInterferenceMessage = "La lecture peut être imprécise. Éloignez-vous des métaux et des appareils électroniques.",
        alertAdhan = "Adhan",
        alertTakbeer = "Takbeer",
        alertChime = "Carillon",
        alertVibrate = "Vibration",
        alertSilent = "Silencieux",
        changeAlertMode = "Changer l’alerte",
        selected = "Sélectionné",
        notSelected = "Non sélectionné",
        stateOn = "Activé",
        stateOff = "Désactivé",
        noSearchResults = "Aucun résultat",
        noResultsMessage = "Essayez une autre orthographe ou un terme plus court.",
        allowNotificationsTitle = "Autoriser les notifications de Salah",
        allowNotificationsMessage = "Salah a besoin de l’autorisation de notification pour vous appeler aux heures de prière. Les horaires restent affichés sur l’écran d’accueil, et vous pouvez activer les alertes plus tard dans les paramètres.",
        allowNotificationsAction = "Autoriser les notifications",
        notNow = "Pas maintenant",
        aboveHorizon = "Au-dessus de l'horizon",
        actionChange = "Modifier",
        adhanAtPrayerLabel = "Adhan à l'heure de la prière", // verify
        adhanPlayingLabel = "Adhan en cours", // verify
        adhanVolume = "Volume", // verify
        alertSilentReminder = "Rappel silencieux", // verify
        allSurahsLabel = "Toutes les sourates", // verify
        ambientField = "Champ ambiant",
        appliedAdjustments = "Ajustements manuels", // verify
        audioCacheCleared = "Cache audio effacé", // verify
        audioSourceLabel = "Audio de récitation", // verify
        audioStreamedNotCached = "Diffusé, non stocké sur l'appareil", // verify
        autoSilenceDurationLabel = "Durée du silence automatique", // verify
        autoSilenceLabel = "Silence automatique pendant la prière", // verify
        belowHorizon = "Sous l'horizon",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Calibrer la boussole",
        calibrationMessage = "Déplacez l'appareil en forme de huit plusieurs fois pour stabiliser le capteur.",
        cardsLayout = "Par verset", // verify
        cardsModeLabel = "Cartes", // verify
        changeLocation = "Changer de lieu",
        checkForUpdates = "Vérifier maintenant",
        chooseAdhanSound = "Son de l'adhan", // verify
        chooseAdjustments = "Ajustements en minutes", // verify
        chooseHijriOffset = "Décalage de la date hégirienne", // verify
        chooseLanguage = "Langue",
        chooseMadhhab = "Calcul de Asr", // verify
        chooseMethod = "Méthode de calcul", // verify
        choosePrePrayerOffset = "Rappel avant la prière", // verify
        chooseReciter = "Récitateur", // verify
        chooseRiwayah = "Riwayah", // verify
        chooseScript = "Écriture", // verify
        chooseTheme = "Thème",
        chooseTranslation = "Traduction", // verify
        closeReader = "Retour aux sourates", // verify
        computedOnDevice = "Calculé sur cet appareil", // verify
        continuousLayout = "Continu", // verify
        continuousModeLabel = "Continu", // verify
        coordinatesCachedOffline = "Coordonnées mises en cache hors ligne. Les calculs s'effectuent entièrement sur cet appareil.",
        copyTodaySchedule = "Copier les horaires du jour", // verify
        copyVerseLabel = "Copier le verset", // verify
        corpusSummary = "114 sourates · 30 juz · 6 236 versets", // verify
        daysShort = "j", // verify
        ephemerisCacheLabel = "Cache du calendrier de prière", // verify
        gpsCached = "GPS mis en cache",
        hideTranslation = "Masquer la traduction", // verify
        hideTranslationLabel = "Masquer", // verify
        hizbHalfFirst = "1re moitié", // verify
        hizbHalfSecond = "2e moitié", // verify
        hizbInJuz = "Juz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Illumination",
        juzLabel = "Juz", // verify
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        lastVerified = "Dernière vérification",
        loadingQuranMessage = "Préparation du texte. Si cela ne se termine pas, revenez en arrière et choisissez à nouveau la sourate.",
        locationSaved = "Lieu enregistré",
        lunarPhase = "Phase lunaire", // verify
        madhhabLabelShort = "Méthode de Asr", // verify
        methodology = "Méthode", // verify
        minutesShort = "min", // verify
        nextDay = "Jour suivant", // verify
        nextSurahLabel = "Sourate suivante", // verify
        noAdjustment = "Aucun ajustement", // verify
        noBookmarksMessage = "Touchez l'icône de marque-page pendant la lecture pour enregistrer un verset ici.", // verify
        noBookmarksTitle = "Aucun verset enregistré", // verify
        noSurahMatchMessage = "Aucune sourate ne correspond. Essayez un numéro ou une partie du nom.",
        nowReading = "En cours de lecture", // verify
        observatorySubtitle = "Astronomie pour %s",
        observatoryTitle = "Ciel",
        pageWord = "Page", // verify
        perPrayerModes = "Alerte par prière", // verify
        prayerMarkedDone = "Prière marquée comme accomplie", // verify
        prayerMarkedPending = "Prière marquée comme non accomplie", // verify
        prePrayerDisabled = "Désactivé", // verify
        previewingTime = "Aperçu de %s", // verify
        previousDay = "Jour précédent", // verify
        previousSurahLabel = "Sourate précédente", // verify
        privacyNote = "Les horaires de prière, la direction de la qibla et le Coran sont calculés sur cet appareil. Rien n'est envoyé.",
        privacyPolicy = "Politique de confidentialité",
        qiblaSubtitle = "Direction de la Kaaba", // verify
        qiblaTitle = "Qibla", // verify
        quranTitle = "Coran", // verify
        readingLayout = "Disposition", // verify
        readingOptions = "Options de lecture", // verify
        readingSaved = "Position de lecture enregistrée", // verify
        readingSettingsLabel = "Paramètres de lecture", // verify
        realTime = "Temps réel", // verify
        recitingLabel = "En récitation", // verify
        recomputeSchedule = "Recalculer le calendrier de 365 jours", // verify
        referenceLabel = "Référence", // verify
        resetAdjustments = "Réinitialiser tous les ajustements", // verify
        resetAllConfirmMessage = "Les paramètres de calcul, d'alerte et d'affichage reviennent à leurs valeurs par défaut. Vos versets enregistrés et votre journal de prière sont conservés.",
        resetAllConfirmTitle = "Réinitialiser tous les paramètres ?",
        resetAllLabel = "Réinitialiser tous les paramètres",
        scheduleCopied = "Horaires copiés", // verify
        searchHintMessage = "Trouvez une sourate par nom ou par sens, ou un verset par son texte arabe ou anglais.", // verify
        searchHintTitle = "Rechercher dans le Coran", // verify
        searchHizb = "Aller au numéro de hizb", // verify
        searchJuz = "Aller au numéro de juz", // verify
        searchPages = "Rechercher un numéro de page", // verify
        searchSurahsAndVerses = "Rechercher des sourates et des versets", // verify
        searchVersesHint = "Rechercher dans le texte arabe ou la traduction anglaise...", // verify
        selectDate = "Sélectionner la date", // verify
        selectLayoutSubtitle = "Par verset convient à l'étude ; continu convient à la lecture suivie.", // verify
        selectLayoutTitle = "Disposition de lecture", // verify
        selectSurah = "Sélectionner la sourate", // verify
        selectVerse = "Sélectionner le verset", // verify
        selectVerseHint = "Sélectionnez un verset pour le lire ici.", // verify
        selectedCity = "Ville sélectionnée",
        sensorAccuracy = "Précision du capteur",
        settingsReset = "Paramètres réinitialisés",
        shareChooserTitle = "Partager le verset via",
        shareVerseLabel = "Partager le verset", // verify
        showTranslation = "Afficher la traduction", // verify
        showTranslationLabel = "Afficher", // verify
        silenceAdhan = "Silence", // verify
        silentModeLabel = "Silence pour toutes les alertes", // verify
        skyPeriodLabel = "Période du ciel",
        solarAltitude = "Altitude du soleil",
        solarAzimuth = "Azimut du soleil",
        somethingWentWrong = "Une erreur s'est produite",
        stopAudio = "Arrêter", // verify
        stopSound = "Arrêter le son",
        storageLabel = "Stockage",
        sunAligned = "Le soleil est presque dans la direction de la qibla. Tournez-vous vers lui pour confirmer.", // verify
        sunAltitude = "Soleil",
        sunAltitudeValue = "Hauteur du soleil",
        sunAzimuth = "Direction du soleil",
        sunBelowHorizon = "Le soleil est sous l'horizon, il ne peut donc pas servir de référence pour le moment.",
        sunOpposite = "La qibla est dans la direction opposée au soleil.", // verify
        sunToTheLeft = "La qibla est à environ %d° à gauche du soleil.", // verify
        sunToTheRight = "La qibla est à environ %d° à droite du soleil.", // verify
        surahLabel = "Sourate", // verify
        surahsFound = "%d sourates", // verify
        tapVerseHint = "Touchez un verset pour l'examiner", // verify
        testSound = "Tester le son", // verify
        themeDark = "Sombre",
        themeLight = "Clair",
        themeSystem = "Suivre le système",
        timeFormat24hLabel = "Heure sur 24 heures", // verify
        timeScrubber = "Moment de la journée", // verify
        todayTitle = "Aujourd'hui", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Anglais — Saheeh International", // verify
        translationNotAvailable = "Traduction non disponible", // verify
        translationSectionTitle = "Traduction", // verify
        translationShownFor = "Affichage de %s", // verify
        trueNorthSuffix = "nord vrai", // verify
        tryAgain = "Réessayer",
        useCurrentLocation = "Utiliser ma position",
        verseActionsLabel = "Actions du verset", // verify
        verseCopiedToast = "Verset copié", // verify
        verseCount = "%d versets", // verify
        verseOf = "Verset %1\$d sur %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d versets", // verify
        versesLabel = "Versets", // verify
        vibrateOnlyLabel = "Vibreur uniquement", // verify
        waning = "Décroissant", // verify
        waxing = "Croissant", // verify
    ),
)

val IndonesianStrings = UiStrings(
    appName = "SALAH",
    navToday = "Hari Ini",
    navPrayer = "Jadwal",
    navQuran = "Al-Qur'an",
    navQibla = "Kiblat",
    navSettings = "Pengaturan",

    nextPrayerLabel = "WAKTU SHOLAT BERIKUTNYA",
    adhanAt = "Adzan pukul",
    viewDetails = "Lihat rincian",
    currentPeriod = "Waktu sekarang",
    skyPeriodSuffix = "Langit ✦",
    hoursUnit = "JAM",
    minsUnit = "MENIT",
    secsUnit = "DETIK",
    todaysPrayers = "Jadwal Sholat Hari Ini",
    tapToMarkCompleted = "Ketuk untuk tandai selesai",
    continueReading = "LANJUTKAN MEMBACA",
    continueButton = "Lanjut",
    sourceLabel = "Sumber",
    alertsActive = "Pengingat Aktif",
    silentModeOn = "Mode Senyap AKTIF",
    masjidMode = "Mode Masjid",
    resetToRealtime = "Kembali ke Waktu Nyata",
    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",
    syncingStatus = "SINKRONISASI",

    fajr = "Subuh",
    sunrise = "Terbit",
    dhuhr = "Dzuhur",
    asr = "Ashar",
    maghrib = "Maghrib",
    isha = "Isya",

    transparentCalculationSource = "Sumber Perhitungan Transparan",
    todaysTimes = "JADWAL SHOLAT HARI INI",
    prayerTimesHeader = "JADWAL SHOLAT",
    vigilsAndNightPeriods = "WAKTU MALAM & TAHAJUD",
    imsakTitle = "Imsak (10 mnt sebelum Subuh)",
    midnightTitle = "Tengah Malam Islam",
    lastThirdTitle = "Sepertiga Malam Terakhir (Tahajud)",
    monthlyCalendarHeader = "KALENDER BULANAN",
    todayBtn = "Hari Ini",

    qiblaDirectionTitle = "Arah Kiblat",
    sensoryCompassSubtitle = "Kompas sensor akurat mengarah ke Ka'bah",
    magneticInterferenceDetected = "Gangguan Magnetik Terdeteksi",
    moveAwayMetal = "Jauhkan dari benda logam atau ketuk kalibrasi.",
    kaabaDistance = "Jarak ke Ka'bah",
    trueNorth = "Utara Sejati",
    magneticNorth = "Utara Magnetik",
    calibratedStatus = "AKURASI TINGGI",
    facingQibla = "Tepat Menghadap Ka'bah",
    turnTowardsKaaba = "Arahkan ke Ka'bah",

    surahTab = "Surah",
    pageTab = "Halaman",
    juzTab = "Juz",
    hizbTab = "Hizb",
    bookmarksTab = "Penanda",
    searchSurahPlaceholder = "Cari Surah nama atau nomor...",
    continueReadingBar = "Lanjutkan Bacaan",
    versesCount = "ayat",
    meccan = "Makkiyyah",
    medinan = "Madaniyyah",

    settingsTitle = "Pengaturan & Konfigurasi",
    sectionGeneral = "UMUM & LOKALISASI",
    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    timeFormatLabel = "Format Waktu 24 Jam",
    sectionPrayerCalc = "JADWAL SHOLAT & PERHITUNGAN",
    locationLabel = "Lokasi",
    methodLabel = "Metode Perhitungan",
    madhhabLabel = "Mazhab Fikih (Ashar)",
    adjustmentsLabel = "Penyesuaian Menit Manual",
    hijriCalibrationLabel = "Kalibrasi Penanggalan Hijriah",
    sectionAudioAlerts = "ADZAN & PEMBERITAHUAN",
    adhanCallLabel = "Kumandangkan Adzan Tepat Waktu",
    perPrayerModesLabel = "Mode Pengingat Per Sholat",
    prePrayerReminderLabel = "Pengingat Sebelum Sholat",
    adhanSoundLabel = "Suara Muadzin Adzan",
    adhanVolumeLabel = "Volume & Uji Suara Adzan",
    vibrationOnlyLabel = "Getar Saja (Senyap)",
    sectionQuran = "AL-QUR'AN & TILAWAH",
    riwayahLabel = "Riwayat Bacaan",
    scriptStyleLabel = "Gaya Rasm Al-Qur'an",
    reciterLabel = "Qari Audio",
    translationLabel = "Terjemahan & Tafsir",
    arabicTextSizeLabel = "Ukuran Huruf Arab",
    sectionMasjidMode = "MODE MASJID & KEKHUSYUKAN",
    globalSilentLabel = "Mode Senyap Global",
    autoMasjidModeLabel = "Senyap Otomatis Saat Sholat",
    autoMasjidDurationLabel = "Durasi Senyap Saat Sholat",
    sectionSystemDiagnostics = "SISTEM & DIAGNOSTIK",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Sinkronisasi Jaringan & Sumber",
    resetDefaultsLabel = "Kembalikan ke Pengaturan Pabrik",
    privacyPhilosophyTitle = "Privasi & Filosofi",

    cancel = "Batal",
    confirm = "Konfirmasi",
    save = "Simpan",
    done = "Selesai",
    close = "Tutup",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Bulan Hijriah penuh", // verify
            nextMonth = "Bulan berikutnya", // verify
            nextPrayerPrefix = "Berikutnya", // verify
            previousMonth = "Bulan sebelumnya", // verify
    ),
    more = UiStringsMore(
        actionSave = "Simpan",
        actionCancel = "Batal",
        actionClose = "Tutup",
        actionBack = "Kembali",
        actionReset = "Atur ulang",
        search = "Cari",
        clearSearch = "Hapus pencarian",
        loading = "Memuat",
        surahsTab = "Surah",
        referenceTab = "Referensi",
        playVerse = "Putar ayat",
        pauseVerse = "Jeda",
        bookmarkVerse = "Tandai",
        removeBookmark = "Hapus tanda",
        copyVerse = "Salin ayat",
        shareVerse = "Bagikan ayat",
        verseCopied = "Ayat disalin",
        textSize = "Ukuran teks",
        layoutPerVerse = "Per ayat",
        layoutContinuous = "Bersambung",
        backToSurahsLabel = "Kembali ke surah",
        sectionAppearance = "Tampilan",
        sectionLocationAndCalculation = "Lokasi dan perhitungan",
        sectionAlerts = "Peringatan",
        sectionAbout = "Tentang",
        chooseLocation = "Pilih lokasi",
        useGps = "Gunakan GPS",
        customLocation = "Masukkan manual",
        searchCities = "Cari kota",
        allCountriesTitle = "Semua negara",
        nameField = "Nama tempat",
        latitudeField = "Lintang",
        longitudeField = "Bujur",
        nameRequired = "Masukkan nama untuk tempat ini.",
        invalidCoordinates = "Masukkan lintang antara -90 dan 90, serta bujur antara -180 dan 180.",
        qiblaBearing = "Arah kiblat",
        isFacingQibla = "Anda menghadap kiblat",
        rightOfQibla = "ke kanan",
        leftOfQibla = "ke kiri",
        turnBy = "Putar %1\$s %2\$s",
        alignedWithQibla = "Sejajar dengan kiblat",
        locateMe = "Lokasi saya",
        holdFlatHint = "Pegang ponsel secara datar agar bacaan kompas akurat.",
        northReferenceLabel = "Acuan utara",
        dialDescription = "Kompas, arah %1\$s. Kiblat %2\$s.",
        solarReferenceTitle = "Periksa dengan matahari",
        calibrationTitle = "Kalibrasi kompas",
        magneticInterference = "Gangguan magnetik",
        magneticInterferenceMessage = "Bacaan bisa tidak akurat. Jauhkan dari logam dan perangkat elektronik.",
        alertAdhan = "Adzan",
        alertTakbeer = "Takbir",
        alertChime = "Lonceng",
        alertVibrate = "Getaran",
        alertSilent = "Senyap",
        changeAlertMode = "Ubah peringatan",
        selected = "Dipilih",
        notSelected = "Belum dipilih",
        stateOn = "Aktif",
        stateOff = "Nonaktif",
        noSearchResults = "Tidak ada hasil",
        noResultsMessage = "Coba ejaan lain atau kata yang lebih pendek.",
        allowNotificationsTitle = "Izinkan notifikasi Salah",
        allowNotificationsMessage = "Salah memerlukan izin notifikasi untukanggil Anda pada waktu salat. Jadwal salat tetap terlihat di layar utama, dan Anda dapat menyalakan pengingat nanti di Pengaturan.",
        allowNotificationsAction = "Izinkan notifikasi",
        notNow = "Nanti saja",
        aboveHorizon = "Di atas horison",
        actionChange = "Ubah",
        adhanAtPrayerLabel = "Adzan pada waktu shalat", // verify
        adhanPlayingLabel = "Adzan sedang diputar", // verify
        adhanVolume = "Volume", // verify
        alertSilentReminder = "Pengingat senyap", // verify
        allSurahsLabel = "Semua surah", // verify
        ambientField = "Medan ambient",
        appliedAdjustments = "Penyesuaian manual", // verify
        audioCacheCleared = "Cache audio dibersihkan", // verify
        audioSourceLabel = "Audio bacaan", // verify
        audioStreamedNotCached = "Distreamkan, tidak disimpan di perangkat", // verify
        autoSilenceDurationLabel = "Durasi senyap otomatis", // verify
        autoSilenceLabel = "Senyap otomatis saat shalat", // verify
        belowHorizon = "Di bawah horison",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Kalibrasi kompas",
        calibrationMessage = "Gerakkan perangkat menyerupai angka delapan beberapa kali agar sensor stabil.",
        cardsLayout = "Per ayat", // verify
        cardsModeLabel = "Kartu", // verify
        changeLocation = "Ubah lokasi",
        checkForUpdates = "Periksa sekarang",
        chooseAdhanSound = "Suara adzan", // verify
        chooseAdjustments = "Penyesuaian menit", // verify
        chooseHijriOffset = "Offset tanggal Hijriah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Perhitungan Asr", // verify
        chooseMethod = "Metode perhitungan", // verify
        choosePrePrayerOffset = "Pengingat sebelum shalat", // verify
        chooseReciter = "Qori", // verify
        chooseRiwayah = "Riwayah", // verify
        chooseScript = "Skrip", // verify
        chooseTheme = "Tema",
        chooseTranslation = "Terjemahan", // verify
        closeReader = "Kembali ke daftar surah", // verify
        computedOnDevice = "Dihitung di perangkat ini", // verify
        continuousLayout = "Kontinu", // verify
        continuousModeLabel = "Kontinu", // verify
        coordinatesCachedOffline = "Koordinat di-cache offline. Perhitungan berjalan sepenuhnya di perangkat ini.",
        copyTodaySchedule = "Salin jadwal hari ini", // verify
        copyVerseLabel = "Salin ayat", // verify
        corpusSummary = "114 surah · 30 juz · 6.236 ayat", // verify
        daysShort = "hr", // verify
        ephemerisCacheLabel = "Cache jadwal shalat", // verify
        gpsCached = "GPS di-cache",
        hideTranslation = "Sembunyikan terjemahan", // verify
        hideTranslationLabel = "Sembunyikan", // verify
        hizbHalfFirst = "Paruh pertama", // verify
        hizbHalfSecond = "Paruh kedua", // verify
        hizbInJuz = "Juz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Iluminasi",
        juzLabel = "Juz", // verify
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        lastVerified = "Terakhir diperiksa",
        loadingQuranMessage = "Menyiapkan teks. Jika tidak selesai, kembali dan pilih surah lagi.",
        locationSaved = "Lokasi disimpan",
        lunarPhase = "Fase bulan", // verify
        madhhabLabelShort = "Metode Asr", // verify
        methodology = "Metode", // verify
        minutesShort = "mnt", // verify
        nextDay = "Hari berikutnya", // verify
        nextSurahLabel = "Surah berikutnya", // verify
        noAdjustment = "Tanpa penyesuaian", // verify
        noBookmarksMessage = "Ketuk ikon penanda saat membaca untuk menyimpan ayat di sini.", // verify
        noBookmarksTitle = "Tidak ada ayat tersimpan", // verify
        noSurahMatchMessage = "Tidak ada surah yang cocok. Coba nomor, atau sebagian nama.",
        nowReading = "Sedang dibaca", // verify
        observatorySubtitle = "Astronomi untuk %s",
        observatoryTitle = "Langit",
        pageWord = "Halaman", // verify
        perPrayerModes = "Pengingat per shalat", // verify
        prayerMarkedDone = "Shalat ditandai telah dikerjakan", // verify
        prayerMarkedPending = "Shalat ditandai belum dikerjakan", // verify
        prePrayerDisabled = "Nonaktif", // verify
        previewingTime = "Pratinjau %s", // verify
        previousDay = "Hari sebelumnya", // verify
        previousSurahLabel = "Surah sebelumnya", // verify
        privacyNote = "Waktu shalat, arah kiblat, dan Al-Qur'an semua dihitung di perangkat ini. Tidak ada yang diunggah.",
        privacyPolicy = "Kebijakan privasi",
        qiblaSubtitle = "Arah ke Ka'bah", // verify
        qiblaTitle = "Kiblat", // verify
        quranTitle = "Al-Qur'an", // verify
        readingLayout = "Tata letak", // verify
        readingOptions = "Opsi bacaan", // verify
        readingSaved = "Posisi bacaan disimpan", // verify
        readingSettingsLabel = "Pengaturan bacaan", // verify
        realTime = "Waktu nyata", // verify
        recitingLabel = "Membaca", // verify
        recomputeSchedule = "Hitung ulang jadwal 365 hari", // verify
        referenceLabel = "Referensi", // verify
        resetAdjustments = "Atur ulang semua penyesuaian", // verify
        resetAllConfirmMessage = "Pengaturan perhitungan, pengingat, dan tampilan kembali ke bawaan. Ayat tersimpan dan catatan shalat Anda tetap disimpan.",
        resetAllConfirmTitle = "Atur ulang semua pengaturan?",
        resetAllLabel = "Atur ulang semua pengaturan",
        scheduleCopied = "Jadwal disalin", // verify
        searchHintMessage = "Cari surah berdasarkan nama atau makna, atau ayat berdasarkan teks Arab atau Inggris.", // verify
        searchHintTitle = "Cari di Al-Qur'an", // verify
        searchHizb = "Lompat ke nomor hizb", // verify
        searchJuz = "Lompat ke nomor juz", // verify
        searchPages = "Cari nomor halaman", // verify
        searchSurahsAndVerses = "Cari surah dan ayat", // verify
        searchVersesHint = "Cari teks Arab atau terjemahan Inggris...", // verify
        selectDate = "Pilih tanggal", // verify
        selectLayoutSubtitle = "Per ayat cocok untuk belajar; kontinu cocok untuk membaca berkesinambungan.", // verify
        selectLayoutTitle = "Tata letak bacaan", // verify
        selectSurah = "Pilih surah", // verify
        selectVerse = "Pilih ayat", // verify
        selectVerseHint = "Pilih ayat untuk membacanya di sini.", // verify
        selectedCity = "Kota terpilih",
        sensorAccuracy = "Akurasi sensor",
        settingsReset = "Pengaturan diatur ulang",
        shareChooserTitle = "Bagikan ayat melalui",
        shareVerseLabel = "Bagikan ayat", // verify
        showTranslation = "Tampilkan terjemahan", // verify
        showTranslationLabel = "Tampilkan", // verify
        silenceAdhan = "Senyapkan", // verify
        silentModeLabel = "Senyapkan semua pengingat", // verify
        skyPeriodLabel = "Periode langit",
        solarAltitude = "Ketinggian matahari",
        solarAzimuth = "Azimut matahari",
        somethingWentWrong = "Terjadi kesalahan",
        stopAudio = "Hentikan", // verify
        stopSound = "Hentikan suara",
        storageLabel = "Penyimpanan",
        sunAligned = "Matahari hampir searah kiblat. Hadapi untuk memastikan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horison, sehingga tidak bisa dipakai sebagai acuan sekarang.",
        sunOpposite = "Kiblat berlawanan arah dengan matahari.", // verify
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        surahLabel = "Surah", // verify
        surahsFound = "%d surah", // verify
        tapVerseHint = "Ketuk ayat untuk memeriksanya.", // verify
        testSound = "Uji suara", // verify
        themeDark = "Gelap",
        themeLight = "Terang",
        themeSystem = "Ikuti sistem",
        timeFormat24hLabel = "Format 24 jam", // verify
        timeScrubber = "Waktu dalam sehari", // verify
        todayTitle = "Hari ini", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Inggris — Saheeh International", // verify
        translationNotAvailable = "Terjemahan tidak tersedia", // verify
        translationSectionTitle = "Terjemahan", // verify
        translationShownFor = "Menampilkan %s", // verify
        trueNorthSuffix = "utara sejati", // verify
        tryAgain = "Coba lagi",
        useCurrentLocation = "Gunakan lokasi saya",
        verseActionsLabel = "Tindakan ayat", // verify
        verseCopiedToast = "Ayat disalin", // verify
        verseCount = "%d ayat", // verify
        verseOf = "Ayat %1\$d dari %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayat", // verify
        versesLabel = "Ayat", // verify
        vibrateOnlyLabel = "Hanya getar", // verify
        waning = "Berkurang", // verify
        waxing = "Bertambah", // verify
    ),
)

val TurkishStrings = UiStrings(
    appName = "SALAH",
    navToday = "Bugün",
    navPrayer = "Vakitler",
    navQuran = "Kur'an",
    navQibla = "Kıble",
    navSettings = "Ayarlar",

    nextPrayerLabel = "SONRAKİ VAKİT",
    adhanAt = "Ezan vakti",
    viewDetails = "Ayrıntıları gör",
    currentPeriod = "Mevcut vakit",
    skyPeriodSuffix = "Gökyüzü ✦",
    hoursUnit = "SAAT",
    minsUnit = "DK",
    secsUnit = "SN",
    todaysPrayers = "Günün Namaz Vakitleri",
    tapToMarkCompleted = "Kılındı işaretlemek için dokunun",
    continueReading = "OKUMAYA DEVAM ET",
    continueButton = "Devam",
    sourceLabel = "Kaynak",
    alertsActive = "Bildirimler Açık",
    silentModeOn = "Sessiz Mod AÇIK",
    masjidMode = "Cami Modu",
    resetToRealtime = "Gerçek Zamana Dön",
    onlineStatus = "ÇEVRİMİÇİ",
    offlineStatus = "ÇEVRİMDIŞI",
    syncingStatus = "EŞİTLENİYOR",

    fajr = "İmsak",
    sunrise = "Güneş",
    dhuhr = "Öğle",
    asr = "İkindi",
    maghrib = "Akşam",
    isha = "Yatsı",

    transparentCalculationSource = "Şeffaf Hesaplama Kaynağı",
    todaysTimes = "BUGÜNÜN VAKİTLERİ",
    prayerTimesHeader = "NAMAZ VAKİTLERİ",
    vigilsAndNightPeriods = "TEHECCÜD & GECE VAKİTLERİ",
    imsakTitle = "İmsak (Fecr-i Sadık)",
    midnightTitle = "Şer'i Gece Yarısı",
    lastThirdTitle = "Gecenin Son Üçte Biri (Teheccüd)",
    monthlyCalendarHeader = "AYLIK İMSAKİYE",
    todayBtn = "Bugün",

    qiblaDirectionTitle = "Kıble Yönü",
    sensoryCompassSubtitle = "Kabe-i Muazzama'yı gösteren hassas pusula",
    magneticInterferenceDetected = "Manyetik Parazit Tespit Edildi",
    moveAwayMetal = "Metal nesnelerden uzaklaşın veya kalibre edin.",
    kaabaDistance = "Kabe'ye Mesafe",
    trueNorth = "Gerçek Kuzey",
    magneticNorth = "Manyetik Kuzey",
    calibratedStatus = "YÜKSEK DOĞRULUK",
    facingQibla = "Kıbleye Yöneldiniz",
    turnTowardsKaaba = "Kabe'ye Doğru Dönün",

    surahTab = "Sure",
    pageTab = "Sayfa",
    juzTab = "Cüz",
    hizbTab = "Hizb",
    bookmarksTab = "Yer İmleri",
    searchSurahPlaceholder = "Sure adı veya numarası ara...",
    continueReadingBar = "Kaldığın Yerden Oku",
    versesCount = "ayet",
    meccan = "Mekki",
    medinan = "Medeni",

    settingsTitle = "Ayarlar & Yapılandırma",
    sectionGeneral = "GENEL & YERELLEŞTİRME",
    languageLabel = "Dil",
    appThemeLabel = "Uygulama Teması",
    timeFormatLabel = "24 Saat Zaman Formatı",
    sectionPrayerCalc = "NAMAZ VAKİTLERİ & HESAPLAMA",
    locationLabel = "Konum",
    methodLabel = "Hesaplama Yöntemi",
    madhhabLabel = "İkindi Mezhebi",
    adjustmentsLabel = "Manuel Dakika Düzeltmeleri",
    hijriCalibrationLabel = "Hicri Takvim Ayarı",
    sectionAudioAlerts = "EZAN SESİ & BİLDİRİMLER",
    adhanCallLabel = "Vaktinde Ezan Oku",
    perPrayerModesLabel = "Vakitlere Özel Uyarı Modları",
    prePrayerReminderLabel = "Vakit Öncesi Hatırlatıcı",
    adhanSoundLabel = "Müezzin Ezan Sesi",
    adhanVolumeLabel = "Ezan Sesi Düzeyi & Testi",
    vibrationOnlyLabel = "Yalnızca Titreşim (Sessiz)",
    sectionQuran = "KUR'AN-I KERİM & TİLAVET",
    riwayahLabel = "Kıraat Rivayeti",
    scriptStyleLabel = "Kur'an Hat / Yazı Stili",
    reciterLabel = "Sesli Kâri (Okuyucu)",
    translationLabel = "Meal & Tefsir",
    arabicTextSizeLabel = "Arapça Yazı Boyutu",
    sectionMasjidMode = "CAMİ MODU & HUŞÛ KORUMASI",
    globalSilentLabel = "Genel Sessiz Mod",
    autoMasjidModeLabel = "Namazda Otomatik Sessiz Mod",
    autoMasjidDurationLabel = "Namaz Sırasında Sessizlik Süresi",
    sectionSystemDiagnostics = "SİSTEM & SENSÖRLER",
    compassDiagnosticsLabel = "Pusula Sensörleri & Tanılama",
    networkSyncLabel = "Ağ Eşitlemesi & Kaynak",
    resetDefaultsLabel = "Fabrika Ayarlarına Sıfırla",
    privacyPhilosophyTitle = "Gizlilik & Felsefe",

    cancel = "İptal",
    confirm = "Onayla",
    save = "Kaydet",
    done = "Tamam",
    close = "Kapat",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Hicri ayın tamamı", // verify
            nextMonth = "Sonraki ay", // verify
            nextPrayerPrefix = "Sonraki", // verify
            previousMonth = "Önceki ay", // verify
    ),
    more = UiStringsMore(
        actionSave = "Kaydet",
        actionCancel = "İptal",
        actionClose = "Kapat",
        actionBack = "Geri",
        actionReset = "Sıfırla",
        search = "Ara",
        clearSearch = "Aramayı temizle",
        loading = "Yükleniyor",
        surahsTab = "Sureler",
        referenceTab = "Dizin",
        playVerse = "Ayeti oynat",
        pauseVerse = "Duraklat",
        bookmarkVerse = "Yer imi ekle",
        removeBookmark = "Yer imini kaldır",
        copyVerse = "Ayeti kopyala",
        shareVerse = "Ayeti paylaş",
        verseCopied = "Ayet kopyalandı",
        textSize = "Yazı boyutu",
        layoutPerVerse = "Ayet ayet",
        layoutContinuous = "Sürekli",
        backToSurahsLabel = "Surelere dön",
        sectionAppearance = "Görünüm",
        sectionLocationAndCalculation = "Konum ve hesaplama",
        sectionAlerts = "Uyarılar",
        sectionAbout = "Hakkında",
        chooseLocation = "Konum seç",
        useGps = "GPS kullan",
        customLocation = "Elle giriş",
        searchCities = "Şehir ara",
        allCountriesTitle = "Tüm ülkeler",
        nameField = "Yer adı",
        latitudeField = "Enlem",
        longitudeField = "Boylam",
        nameRequired = "Bu yer için bir ad girin.",
        invalidCoordinates = "Enlem -90 ile 90, boylam -180 ile 180 arasında olmalıdır.",
        qiblaBearing = "Kıble yönü",
        isFacingQibla = "Kıbleye dönüksünüz",
        rightOfQibla = "sağa",
        leftOfQibla = "sola",
        turnBy = "%2\$s %1\$s dön",
        alignedWithQibla = "Kible ile hizalı",
        locateMe = "Konumumu bul",
        holdFlatHint = "Pusulanın doğru okunması için telefonu düz tutun.",
        northReferenceLabel = "Kuzey referansı",
        dialDescription = "Pusula, yön %1\$s. Kıble %2\$s.",
        solarReferenceTitle = "Güneşle doğrula",
        calibrationTitle = "Pusulayı kalibre et",
        magneticInterference = "Manyetik parazit",
        magneticInterferenceMessage = "Ölçüm hatalı olabilir. Metal ve elektronik cihazlardan uzaklaşın.",
        alertAdhan = "Ezan",
        alertTakbeer = "Tekbir",
        alertChime = "Çan sesi",
        alertVibrate = "Titreşim",
        alertSilent = "Sessiz",
        changeAlertMode = "Uyarıyı değiştir",
        selected = "Seçili",
        notSelected = "Seçili değil",
        stateOn = "Açık",
        stateOff = "Kapalı",
        noSearchResults = "Sonuç yok",
        noResultsMessage = "Farklı bir yazım veya daha kısa bir terim deneyin.",
        allowNotificationsTitle = "Salah sizi uyarsın",
        allowNotificationsMessage = "Salah, sizi namaz vakitlerinde uyarmak için bildirim iznine ihtiyaç duyar. Namaz vakitleri her hâlükârda ana ekranda görünür; uyarıları daha sonra Ayarlar’dan açabilirsiniz.",
        allowNotificationsAction = "Bildirimlere izin ver",
        notNow = "Şimdi değil",
        aboveHorizon = "Ufuk üstü",
        actionChange = "Değiştir",
        adhanAtPrayerLabel = "Namaz vaktinde ezan", // verify
        adhanPlayingLabel = "Ezan çalıyor", // verify
        adhanVolume = "Ses seviyesi", // verify
        alertSilentReminder = "Sessiz hatırlatma", // verify
        allSurahsLabel = "Tüm sureler", // verify
        ambientField = "Çevresel alan",
        appliedAdjustments = "Elle ayarlamalar", // verify
        audioCacheCleared = "Ses önbelleği temizlendi", // verify
        audioSourceLabel = "Kıraat sesi", // verify
        audioStreamedNotCached = "Yayınlanıyor, cihazda saklanmıyor", // verify
        autoSilenceDurationLabel = "Otomatik sessizlik süresi", // verify
        autoSilenceLabel = "Namazda otomatik sessizlik", // verify
        belowHorizon = "Ufuk altı",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Pusulayı kalibre et",
        calibrationMessage = "Sensörün dengelemesi için cihazı birkaç kez sekiz şeklinde hareket ettirin.",
        cardsLayout = "Ayete göre", // verify
        cardsModeLabel = "Kartlar", // verify
        changeLocation = "Konumu değiştir",
        checkForUpdates = "Şimdi kontrol et",
        chooseAdhanSound = "Ezan sesi", // verify
        chooseAdjustments = "Dakika ayarlamaları", // verify
        chooseHijriOffset = "Hicri tarih farkı", // verify
        chooseLanguage = "Dil",
        chooseMadhhab = "Asr hesabı", // verify
        chooseMethod = "Hesap yöntemi", // verify
        choosePrePrayerOffset = "Namazdan önce hatırlatma", // verify
        chooseReciter = "Kari", // verify
        chooseRiwayah = "Rivayet", // verify
        chooseScript = "Yazı", // verify
        chooseTheme = "Tema",
        chooseTranslation = "Çeviri", // verify
        closeReader = "Surelere dön", // verify
        computedOnDevice = "Bu cihazda hesaplanır", // verify
        continuousLayout = "Sürekli", // verify
        continuousModeLabel = "Sürekli", // verify
        coordinatesCachedOffline = "Koordinatlar çevrimdışı önbelleğe alındı. Hesaplamalar tamamen bu cihazda yapılır.",
        copyTodaySchedule = "Bugünün vakitlerini kopyala", // verify
        copyVerseLabel = "Ayeti kopyala", // verify
        corpusSummary = "114 sure · 30 cüz · 6.236 ayet", // verify
        daysShort = "g", // verify
        ephemerisCacheLabel = "Namaz vakitleri önbelleği", // verify
        gpsCached = "GPS önbelleğe alındı",
        hideTranslation = "Çeviriyi gizle", // verify
        hideTranslationLabel = "Gizle", // verify
        hizbHalfFirst = "İlk yarım", // verify
        hizbHalfSecond = "İkinci yarım", // verify
        hizbInJuz = "Cüz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Aydınlanma",
        juzLabel = "Cüz", // verify
        juzOf = "Cüz %d", // verify
        juzWord = "Cüz", // verify
        lastVerified = "Son kontrol",
        loadingQuranMessage = "Metin hazırlanıyor. Bitmiyorsa geri dönün ve sureyi yeniden seçin.",
        locationSaved = "Konum kaydedildi",
        lunarPhase = "Ay evresi", // verify
        madhhabLabelShort = "Asr yöntemi", // verify
        methodology = "Yöntem", // verify
        minutesShort = "dk", // verify
        nextDay = "Sonraki gün", // verify
        nextSurahLabel = "Sonraki sure", // verify
        noAdjustment = "Ayarlama yok", // verify
        noBookmarksMessage = "Buraya ayet kaydetmek için okuma sırasında yer imi simgesine dokunun.", // verify
        noBookmarksTitle = "Kayıtlı ayet yok", // verify
        noSurahMatchMessage = "Buna uyan sure yok. Bir numara veya adın bir kısmını deneyin.",
        nowReading = "Şu an okunuyor", // verify
        observatorySubtitle = "%s için astronomi",
        observatoryTitle = "Gökyüzü",
        pageWord = "Sayfa", // verify
        perPrayerModes = "Namaz bazında uyarı", // verify
        prayerMarkedDone = "Namaz kılındı olarak işaretlendi", // verify
        prayerMarkedPending = "Namaz kılınmadı olarak işaretlendi", // verify
        prePrayerDisabled = "Kapalı", // verify
        previewingTime = "%s önizleniyor", // verify
        previousDay = "Önceki gün", // verify
        previousSurahLabel = "Önceki sure", // verify
        privacyNote = "Namaz vakitleri, kıble yönü ve Kur'an bu cihazda hesaplanır. Hiçbir şey yüklenmez.",
        privacyPolicy = "Gizlilik politikası",
        qiblaSubtitle = "Kabe yönü", // verify
        qiblaTitle = "Kıble", // verify
        quranTitle = "Kur'an", // verify
        readingLayout = "Düzen", // verify
        readingOptions = "Okuma seçenekleri", // verify
        readingSaved = "Okuma konumu kaydedildi", // verify
        readingSettingsLabel = "Okuma ayarları", // verify
        realTime = "Gerçek zaman", // verify
        recitingLabel = "Kıraat ediyor", // verify
        recomputeSchedule = "365 günlük çizelgeyi yeniden hesapla", // verify
        referenceLabel = "Referans", // verify
        resetAdjustments = "Tüm ayarlamaları sıfırla", // verify
        resetAllConfirmMessage = "Hesap, uyarı ve görünüm ayarları varsayılanlara döner. Kayıtlı ayetleriniz ve namaz kaydınız korunur.",
        resetAllConfirmTitle = "Tüm ayarlar sıfırlansın mı?",
        resetAllLabel = "Tüm ayarları sıfırla",
        scheduleCopied = "Vakitler kopyalandı", // verify
        searchHintMessage = "Sureyi adına veya anlamına göre, ayeti Arapça veya İngilizce metnine göre bulun.", // verify
        searchHintTitle = "Kur'an'da ara", // verify
        searchHizb = "Hizb numarasına git", // verify
        searchJuz = "Cüz numarasına git", // verify
        searchPages = "Sayfa numarası ara", // verify
        searchSurahsAndVerses = "Sure ve ayet ara", // verify
        searchVersesHint = "Arapça metin veya İngilizce çeviri ara...", // verify
        selectDate = "Tarih seç", // verify
        selectLayoutSubtitle = "Ayete göre çalışmaya uygun; sürekli düzen okumaya uygun.", // verify
        selectLayoutTitle = "Okuma düzeni", // verify
        selectSurah = "Sure seç", // verify
        selectVerse = "Ayet seç", // verify
        selectVerseHint = "Burada okumak için bir ayet seçin.", // verify
        selectedCity = "Seçilen şehir",
        sensorAccuracy = "Sensör doğruluğu",
        settingsReset = "Ayarlar sıfırlandı",
        shareChooserTitle = "Ayeti şununla paylaş",
        shareVerseLabel = "Ayeti paylaş", // verify
        showTranslation = "Çeviriyi göster", // verify
        showTranslationLabel = "Göster", // verify
        silenceAdhan = "Sessize al", // verify
        silentModeLabel = "Tüm uyarıları sessize al", // verify
        skyPeriodLabel = "Gökyüzü dönemi",
        solarAltitude = "Güneş yüksekliği",
        solarAzimuth = "Güneş azimutu",
        somethingWentWrong = "Bir şeyler ters gitti",
        stopAudio = "Durdur", // verify
        stopSound = "Sesi durdur",
        storageLabel = "Depolama",
        sunAligned = "Güneş neredeyse kıble yönünde. Doğrulamak için ona dönün.", // verify
        sunAltitude = "Güneş",
        sunAltitudeValue = "Güneş yüksekliği",
        sunAzimuth = "Güneş yönü",
        sunBelowHorizon = "Güneş ufuk altında, bu yüzden şu an referans olarak kullanılamaz.",
        sunOpposite = "Kıble güneşin ters yönünde.", // verify
        sunToTheLeft = "Kıble güneşin yaklaşık %d° solunda.", // verify
        sunToTheRight = "Kıble güneşin yaklaşık %d° sağında.", // verify
        surahLabel = "Sure", // verify
        surahsFound = "%d sure", // verify
        tapVerseHint = "İncelemek için bir ayete dokunun.", // verify
        testSound = "Sesi test et", // verify
        themeDark = "Koyu",
        themeLight = "Açık",
        themeSystem = "Sisteme uy",
        timeFormat24hLabel = "24 saatlik format", // verify
        timeScrubber = "Günün saati", // verify
        todayTitle = "Bugün", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "İngilizce — Saheeh International", // verify
        translationNotAvailable = "Çeviri mevcut değil", // verify
        translationSectionTitle = "Çeviri", // verify
        translationShownFor = "%s gösteriliyor", // verify
        trueNorthSuffix = "gerçek kuzey", // verify
        tryAgain = "Tekrar dene",
        useCurrentLocation = "Konumumu kullan",
        verseActionsLabel = "Ayet işlemleri", // verify
        verseCopiedToast = "Ayet kopyalandı", // verify
        verseCount = "%d ayet", // verify
        verseOf = "Ayet %1\$d / %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayet", // verify
        versesLabel = "Ayetler", // verify
        vibrateOnlyLabel = "Yalnızca titreşim", // verify
        waning = "Küçülen", // verify
        waxing = "Büyüyen", // verify
    ),
)

val UrduStrings = UiStrings(
    appName = "صلاۃ",
    navToday = "آج",
    navPrayer = "نماز",
    navQuran = "قرآن",
    navQibla = "قبلہ",
    navSettings = "ترتیبات",

    nextPrayerLabel = "اگلی نماز",
    adhanAt = "اذان کا وقت",
    viewDetails = "تفصیلات دیکھیں",
    currentPeriod = "موجودہ وقت",
    skyPeriodSuffix = "آسمان ✦",
    hoursUnit = "گھنٹے",
    minsUnit = "منٹ",
    secsUnit = "سیکنڈ",
    todaysPrayers = "آج کی نمازیں",
    tapToMarkCompleted = "مکمل کرنے پر نشان لگائیں",
    continueReading = "تلاوت جاری رکھیں",
    continueButton = "جاری رکھیں",
    sourceLabel = "ماخذ",
    alertsActive = "انتباہات فعال ہیں",
    silentModeOn = "خاموش موڈ آن ہے",
    masjidMode = "مسجد موڈ",
    resetToRealtime = "اصل وقت پر واپس آئیں",
    onlineStatus = "آن لائن",
    offlineStatus = "آف لائن",
    syncingStatus = "ہم آہنگی جاری",

    fajr = "فجر",
    sunrise = "طلوع آفتاب",
    dhuhr = "ظہر",
    asr = "عصر",
    maghrib = "مغرب",
    isha = "عشاء",

    transparentCalculationSource = "شفاف حساب کا شرعی ماخذ",
    todaysTimes = "آج کے اوقات",
    prayerTimesHeader = "اوقاتِ نماز",
    vigilsAndNightPeriods = "تہجد اور رات کے اوقات",
    imsakTitle = "امساک (فجر سے ۱۰ منٹ قبل)",
    midnightTitle = "شرعی نصف شب",
    lastThirdTitle = "رات کا آخری تہائی حصہ (تہجد)",
    monthlyCalendarHeader = "ماہانہ تقویم",
    todayBtn = "آج",

    qiblaDirectionTitle = "سمتِ قبلہ",
    sensoryCompassSubtitle = "کعبہ شریف کی طرف رخ دکھانے والا قطب نما",
    magneticInterferenceDetected = "مقناطیسی خلل پایا گیا",
    moveAwayMetal = "دھاتی چیزوں سے دور ہوں یا کیلیبریٹ کریں۔",
    kaabaDistance = "کعبہ شریف کا فاصلہ",
    trueNorth = "حقیقی شمال",
    magneticNorth = "مقناطیسی شمال",
    calibratedStatus = "اعلیٰ درستگی",
    facingQibla = "آپ کا رخ قبلہ کی طرف ہے",
    turnTowardsKaaba = "کعبہ کی طرف رخ کریں",

    surahTab = "سورت",
    pageTab = "صفحہ",
    juzTab = "پارہ",
    hizbTab = "حزب",
    bookmarksTab = "نشانات",
    searchSurahPlaceholder = "سورت کا نام یا نمبر تلاش کریں...",
    continueReadingBar = "تلاوت جاری رکھیں",
    versesCount = "آیات",
    meccan = "مکی",
    medinan = "مدنی",

    settingsTitle = "ترتیبات اور ترجیحات",
    sectionGeneral = "عام اور زبان",
    languageLabel = "زبان",
    appThemeLabel = "ایپ تھیم",
    timeFormatLabel = "۲۴ گھنٹے وقت کی ترتیب",
    sectionPrayerCalc = "اوقاتِ نماز اور حساب",
    locationLabel = "مقام",
    methodLabel = "طریقہ حساب",
    madhhabLabel = "فقہی مسلک (عصر)",
    adjustmentsLabel = "دستی منٹ ایڈجسٹمنٹ",
    hijriCalibrationLabel = "ہجری تاریخ کی ترتیب",
    sectionAudioAlerts = "اذان آڈیو اور نوٹیفکیشن",
    adhanCallLabel = "وقت پر اذان کی پکار",
    perPrayerModesLabel = "ہر نماز کے لیے الرٹ موڈ",
    prePrayerReminderLabel = "نماز سے قبل یاد دہانی",
    adhanSoundLabel = "مؤذن کی آواز",
    adhanVolumeLabel = "اذان کی آواز اور ٹیسٹ",
    vibrationOnlyLabel = "صرف وائبریشن (خاموش)",
    sectionQuran = "قرآن مجید اور تلاوت",
    riwayahLabel = "قرآنی روایت",
    scriptStyleLabel = "قرآنی رسم الخط",
    reciterLabel = "آڈیو قاری",
    translationLabel = "ترجمہ و تفسیر",
    arabicTextSizeLabel = "عربی فونٹ سائز",
    sectionMasjidMode = "مسجد موڈ اور خشوع",
    globalSilentLabel = "عام سائلنٹ موڈ",
    autoMasjidModeLabel = "نماز کے دوران خودکار سائلنٹ",
    autoMasjidDurationLabel = "نماز میں سائلنٹ کا دورانیہ",
    sectionSystemDiagnostics = "سسٹم اور سینسرز",
    compassDiagnosticsLabel = "قطب نما اور سینسر کی جانچ",
    networkSyncLabel = "نیٹ ورک سنک اور ماخذ",
    resetDefaultsLabel = "فیکٹری سیٹنگز پر بحال کریں",
    privacyPhilosophyTitle = "رازداری اور اصول",

    cancel = "منسوخ",
    confirm = "تصدیق",
    save = "محفوظ کریں",
    done = "مکمل",
    close = "بند کریں",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "ہجری مہینہ مکمل", // verify
            nextMonth = "اگلا مہینہ", // verify
            nextPrayerPrefix = "اگلی", // verify
            previousMonth = "پچھلا مہینہ", // verify
    ),
    more = UiStringsMore(
        actionSave = "محفوظ کریں",
        actionCancel = "منسوخ کریں",
        actionClose = "بند کریں",
        actionBack = "واپس",
        actionReset = "reset کریں",
        search = "تلاش",
        clearSearch = "تلاش صافی کریں",
        loading = "لوڈ ہو رہا ہے",
        surahsTab = "سورتیں",
        referenceTab = "فہرست",
        playVerse = "آیت چلائیں",
        pauseVerse = "روکیں",
        bookmarkVerse = "نشان لگائیں",
        removeBookmark = "نشان ہٹائیں",
        copyVerse = "آیت کاپی کریں",
        shareVerse = "آیت شیئر کریں",
        verseCopied = "آیت کاپی ہو گئی",
        textSize = "متن کا حجم",
        layoutPerVerse = "ایک آیت",
        layoutContinuous = "مسلسل",
        backToSurahsLabel = "سورتوں واپس",
        sectionAppearance = "ظاہری شکل",
        sectionLocationAndCalculation = "مقام اور حساب",
        sectionAlerts = "اطلاعات",
        sectionAbout = "تعارف",
        chooseLocation = "مقام منتخب کریں",
        useGps = "GPS استعمال کریں",
        customLocation = "خود درج کریں",
        searchCities = "شہر تلاش کریں",
        allCountriesTitle = "تمام ممالک",
        nameField = "مقام کا نام",
        latitudeField = "عرض البلد",
        longitudeField = "طول البلد",
        nameRequired = "اس مقام کے لیے نام درج کریں۔",
        invalidCoordinates = "عرض البلد -90 اور 90 کے درمیان، طول البلد -180 اور 180 کے درمیان ہونا چاہیے۔",
        qiblaBearing = "قبلہ کا رخ",
        isFacingQibla = "آپ قبلہ کی طرف ہیں",
        rightOfQibla = "دائیں طرف",
        leftOfQibla = "بائیں طرف",
        turnBy = "%2\$s %1\$s گھومیں",
        alignedWithQibla = "قبلہ کے ساتھ ہم محاذ",
        locateMe = "مقام لیں",
        holdFlatHint = "قطب نما کی درست ریڈنگ کے لیے فون کو سیدھا رکھیں۔",
        northReferenceLabel = "شمالی حوالہ",
        dialDescription = "قطب نما، موجودہ رخ %1\$s۔ قبلہ %2\$s۔",
        solarReferenceTitle = "سورج سے تصدیق کریں",
        calibrationTitle = "قطب نما کیلیبریٹ کریں",
        magneticInterference = "مقناطیسی خلل",
        magneticInterferenceMessage = "ریڈنگ غلط ہو سکتی ہے۔ دھات اور برقی آلات سے دور ہوں۔",
        alertAdhan = "اذان",
        alertTakbeer = "تکبیر",
        alertChime = "گھنٹی",
        alertVibrate = "وائبریشن",
        alertSilent = "خاموش",
        changeAlertMode = "اطلاع تبدیل کریں",
        selected = "منتخب",
        notSelected = "غیر منتخب",
        stateOn = "فعال",
        stateOff = "غیر فعال",
        noSearchResults = "کوئی نتیجہ نہیں",
        noResultsMessage = "کوئی دوسری املا یا مختصر الفاظ آزمائیں۔",
        allowNotificationsTitle = "صلاة آپ کو مطلع کرے",
        allowNotificationsMessage = "صلاة کو نماز کے اوقات میں آپ کو مطلع کرنے کے لیے اطلاعات کی اجازت درکار ہے۔ نماز کے اوقات ہوم اسکرین پر کھلے رہتے ہیں، اور آپ بعد میں ترتیبات سے اطلاعات فعال کر سکتے ہیں۔",
        allowNotificationsAction = "اطلاعات کی اجازت دیں",
        notNow = "ابھی نہیں",
        aboveHorizon = "افق سے اوپر",
        actionChange = "تبدیل کریں",
        adhanAtPrayerLabel = "نماز کے وقت اذان", // verify
        adhanPlayingLabel = "اذان چل رہی ہے", // verify
        adhanVolume = "آواز کا درجہ", // verify
        alertSilentReminder = "خاموش یاددہانی", // verify
        allSurahsLabel = "تمام سورتیں", // verify
        ambientField = "ماحولی میدان",
        appliedAdjustments = "دستی ترتیبات", // verify
        audioCacheCleared = "آڈیو کیش صاف ہو گئی", // verify
        audioSourceLabel = "تلاوت کی آواز", // verify
        audioStreamedNotCached = "سٹریم ہو رہا ہے، ڈیوائس پر محفوظ نہیں", // verify
        autoSilenceDurationLabel = "خاموشی کی مدت", // verify
        autoSilenceLabel = "نماز کے دوران خودکار خاموشی", // verify
        belowHorizon = "افق سے نیچے",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "قطب نما کی کیلیبریشن کریں",
        calibrationMessage = "سینسر کو مستحکم کرنے کے لیے ڈیوائس کو کئی بار ٹ شکل میں گھمائیں۔",
        cardsLayout = "آیت کے لحاظ سے", // verify
        cardsModeLabel = "کارڈز", // verify
        changeLocation = "مقام تبدیل کریں",
        checkForUpdates = "ابھی چیک کریں",
        chooseAdhanSound = "اذان کی آواز", // verify
        chooseAdjustments = "منٹ کی ترتیبات", // verify
        chooseHijriOffset = "ہجری تاریخ کا آفسیٹ", // verify
        chooseLanguage = "زبان",
        chooseMadhhab = "عصر کا حساب", // verify
        chooseMethod = "حساب کا طریقہ", // verify
        choosePrePrayerOffset = "نماز سے پہلے یاددہانی", // verify
        chooseReciter = "قاری", // verify
        chooseRiwayah = "روایت", // verify
        chooseScript = "رسم الخط", // verify
        chooseTheme = "تھیم",
        chooseTranslation = "ترجمہ", // verify
        closeReader = "سورتوں پر واپس جائیں", // verify
        computedOnDevice = "اسی ڈیوائس پر حساب ہوتا ہے", // verify
        continuousLayout = "مسلسل", // verify
        continuousModeLabel = "مسلسل", // verify
        coordinatesCachedOffline = "کوآرڈینیٹس آف لائن کیش ہو گئے۔ حساب مکمل طور پر اسی ڈیوائس پر چلتا ہے۔",
        copyTodaySchedule = "آج کے اوقات کی نقل بنائیں", // verify
        copyVerseLabel = "آیت کی نقل بنائیں", // verify
        corpusSummary = "114 سورتیں · 30 جزء · 6236 آیات", // verify
        daysShort = "دن", // verify
        ephemerisCacheLabel = "نماز کے شیڈول کی کیش", // verify
        gpsCached = "جی پی ایس کیش ہو گئی",
        hideTranslation = "ترجمہ چھپائیں", // verify
        hideTranslationLabel = "چھپائیں", // verify
        hizbHalfFirst = "پہلا نصف", // verify
        hizbHalfSecond = "دوسرا نصف", // verify
        hizbInJuz = "جزء %d، %s", // verify
        hizbOf = "حزب %d", // verify
        hizbWord = "حزب", // verify
        illumination = "روشنی",
        juzLabel = "جزء", // verify
        juzOf = "جزء %d", // verify
        juzWord = "جزء", // verify
        lastVerified = "آخری جانچ",
        loadingQuranMessage = "متن تیار ہو رہا ہے۔ اگر یہ مکمل نہ ہو تو واپس جائیں اور سورت دوبارہ منتخب کریں۔",
        locationSaved = "مقام محفوظ ہو گیا",
        lunarPhase = "چاند کا دور", // verify
        madhhabLabelShort = "عصر کا طریقہ", // verify
        methodology = "طریقہ", // verify
        minutesShort = "منٹ", // verify
        nextDay = "اگلا دن", // verify
        nextSurahLabel = "اگلی سورت", // verify
        noAdjustment = "کوئی ترتیب نہیں", // verify
        noBookmarksMessage = "یہاں آیت محفوظ کرنے کے لیے پڑھتے وقت بک مارک کا آئیکن دبائیں۔", // verify
        noBookmarksTitle = "کوئی محفوظ آیات نہیں", // verify
        noSurahMatchMessage = "کوئی سورت نہیں ملتی۔ نمبر یا نام کا کوئی حصہ آزمائیں۔",
        nowReading = "ابھی پڑھا جا رہا ہے", // verify
        observatorySubtitle = "کے لیے فلکیات",
        observatoryTitle = "آسمان",
        pageWord = "صفحہ", // verify
        perPrayerModes = "ہر نماز کے لیے الرٹ", // verify
        prayerMarkedDone = "نماز ادا ہوئی کے طور پر نشان زد", // verify
        prayerMarkedPending = "نماز ادا نہیں ہوئی کے طور پر نشان زد", // verify
        prePrayerDisabled = "بند", // verify
        previewingTime = "%s کا پیش منظر", // verify
        previousDay = "پچھلا دن", // verify
        previousSurahLabel = "پچھلی سورت", // verify
        privacyNote = "نماز کے اوقات، قبلہ کی سمت اور قرآن سب اسی ڈیوائس پر حساب ہوتے ہیں۔ کچھ اپلوڈ نہیں ہوتا۔",
        privacyPolicy = "رازداری کی پالیسی",
        qiblaSubtitle = "کعبہ کی سمت", // verify
        qiblaTitle = "قبلہ", // verify
        quranTitle = "قرآن", // verify
        readingLayout = "ترتیب", // verify
        readingOptions = "پڑھنے کے اختیارات", // verify
        readingSaved = "پڑھنے کی جگہ محفوظ ہو گئی", // verify
        readingSettingsLabel = "پڑھنے کی ترتیبات", // verify
        realTime = "حقیقی وقت", // verify
        recitingLabel = "تلاوت کر رہے ہیں", // verify
        recomputeSchedule = "365 دن کا شیڈول دوبارہ حساب کریں", // verify
        referenceLabel = "حوالہ", // verify
        resetAdjustments = "تمام ترتیبات دوبارہ ترتیب دیں", // verify
        resetAllConfirmMessage = "حساب، الرٹ اور ڈسپلے کی ترتیبات ڈیفالٹ پر آ جاتی ہیں۔ آپ کی محفوظ آیات اور نماز کا لاگ محفوظ رہتا ہے۔",
        resetAllConfirmTitle = "تمام ترتیبات دوبارہ ترتیب دیں؟",
        resetAllLabel = "تمام ترتیبات دوبارہ ترتیب دیں",
        scheduleCopied = "اوقات کی نقل ہو گئی", // verify
        searchHintMessage = "نام یا معنی کے لحاظ سے سورت تلاش کریں، یا عربی یا انگریزی متن سے آیت تلاش کریں۔", // verify
        searchHintTitle = "قرآن میں تلاش کریں", // verify
        searchHizb = "حزب نمبر پر جائیں", // verify
        searchJuz = "جزء نمبر پر جائیں", // verify
        searchPages = "صفحہ نمبر تلاش کریں", // verify
        searchSurahsAndVerses = "سورتیں اور آیات تلاش کریں", // verify
        searchVersesHint = "عربی متن یا انگریزی ترجمہ تلاش کریں...", // verify
        selectDate = "تاریخ منتخب کریں", // verify
        selectLayoutSubtitle = "آیت کے لحاظ سے مطالعہ کے لیے مناسب؛ مسلسل پڑھنے کے لیے مناسب۔", // verify
        selectLayoutTitle = "پڑھنے کی ترتیب", // verify
        selectSurah = "سورت منتخب کریں", // verify
        selectVerse = "آیت منتخب کریں", // verify
        selectVerseHint = "یہاں پڑھنے کے لیے آیت منتخب کریں۔", // verify
        selectedCity = "منتخب شہر",
        sensorAccuracy = "سینسر کی درستگی",
        settingsReset = "ترتیبات دوبارہ ترتیب دی گئیں",
        shareChooserTitle = "آیت کا اشتراک کریں",
        shareVerseLabel = "آیت کا اشتراک کریں", // verify
        showTranslation = "ترجمہ دکھائیں", // verify
        showTranslationLabel = "دکھائیں", // verify
        silenceAdhan = "خاموش کریں", // verify
        silentModeLabel = "تمام الرٹس خاموش کریں", // verify
        skyPeriodLabel = "آسمان کی مدت",
        solarAltitude = "سورج کی اونچائی",
        solarAzimuth = "سورج کا ازیموت",
        somethingWentWrong = "کچھ غلط ہو گیا",
        stopAudio = "روکیں", // verify
        stopSound = "آواز بند کریں",
        storageLabel = "اسٹوریج",
        sunAligned = "سورج قبلہ کی سمت میں تقریباً ہے۔ تصدیق کے لیے اس کی طرف منہ کریں۔", // verify
        sunAltitude = "سورج",
        sunAltitudeValue = "سورج کی اونچائی",
        sunAzimuth = "سورج کی سمت",
        sunBelowHorizon = "سورج افق سے نیچے ہے، اس لیے اسے ابھی بطور حوالہ استعمال نہیں کیا جا سکتا۔",
        sunOpposite = "قبلہ سورج کے الٹ سمت میں ہے۔", // verify
        sunToTheLeft = "قبلہ سورج کے بائیں جانب تقریباً %d° ہے۔", // verify
        sunToTheRight = "قبلہ سورج کے دائیں جانب تقریباً %d° ہے۔", // verify
        surahLabel = "سورت", // verify
        surahsFound = "%d سورتیں", // verify
        tapVerseHint = "جانچنے کے لیے آیت پر ٹیپ کریں۔", // verify
        testSound = "آواز آزمائیں", // verify
        themeDark = "ڈارک",
        themeLight = "لائٹ",
        themeSystem = "سسٹم کے مطابق",
        timeFormat24hLabel = "24 گھنٹے کا فارمیٹ", // verify
        timeScrubber = "دن کا وقت", // verify
        todayTitle = "آج", // verify
        translationCredit = "صحیح انٹرنیشنل", // verify
        translationCreditLine = "انگریزی — صحیح انٹرنیشنل", // verify
        translationNotAvailable = "ترجمہ دستیاب نہیں", // verify
        translationSectionTitle = "ترجمہ", // verify
        translationShownFor = "%s دکھائی جا رہا ہے", // verify
        trueNorthSuffix = "حقیقی شمال", // verify
        tryAgain = "دوبارہ کوشش کریں",
        useCurrentLocation = "میرا مقام استعمال کریں",
        verseActionsLabel = "آیت کے اقدامات", // verify
        verseCopiedToast = "آیت کی نقل ہو گئی", // verify
        verseCount = "%d آیات", // verify
        verseOf = "آیت %1\$d از %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d آیات", // verify
        versesLabel = "آیات", // verify
        vibrateOnlyLabel = "صرف ہلاؤ", // verify
        waning = "گھٹتا ہوا", // verify
        waxing = "بڑھتا ہوا", // verify
    ),
)

val MalayStrings = UiStrings(
    appName = "SALAH",
    navToday = "Hari Ini",
    navPrayer = "Solat",
    navQuran = "Al-Quran",
    navQibla = "Kiblat",
    navSettings = "Tetapan",

    nextPrayerLabel = "SOLAT SETERUSNYA",
    adhanAt = "Azan pada",
    viewDetails = "Lihat butiran",
    currentPeriod = "Waktu sekarang",
    skyPeriodSuffix = "Langit ✦",
    hoursUnit = "JAM",
    minsUnit = "MINIT",
    secsUnit = "SAAT",
    todaysPrayers = "Waktu Solat Hari Ini",
    tapToMarkCompleted = "Ketik untuk tanda selesai",
    continueReading = "TERUSKAN MEMBACA",
    continueButton = "Teruskan",
    sourceLabel = "Sumber",
    alertsActive = "Pemberitahuan Aktif",
    silentModeOn = "Mod Senyap AKTIF",
    masjidMode = "Mod Masjid",
    resetToRealtime = "Kembali ke Waktu Nyata",
    onlineStatus = "DALAM TALIAN",
    offlineStatus = "LUAR TALIAN",
    syncingStatus = "MENYELARAS",

    fajr = "Subuh",
    sunrise = "Syuruk",
    dhuhr = "Zohor",
    asr = "Asar",
    maghrib = "Maghrib",
    isha = "Isyak",

    transparentCalculationSource = "Sumber Pengiraan Telus",
    todaysTimes = "WAKTU HARI INI",
    prayerTimesHeader = "WAKTU SOLAT",
    vigilsAndNightPeriods = "WAKTU MALAM & TAHAJJUD",
    imsakTitle = "Imsak (10 minit sebelum Subuh)",
    midnightTitle = "Tengah Malam Islam",
    lastThirdTitle = "Sepertiga Malam Terakhir (Tahajjud)",
    monthlyCalendarHeader = "TAKWIM BULANAN",
    todayBtn = "Hari Ini",

    qiblaDirectionTitle = "Arah Kiblat",
    sensoryCompassSubtitle = "Kompas sensor tepat menghadap Kaabah",
    magneticInterferenceDetected = "Gangguan Magnet Dikesan",
    moveAwayMetal = "Jauhi objek logam atau ketik kalibrasi.",
    kaabaDistance = "Jarak ke Kaabah",
    trueNorth = "Utara Benar",
    magneticNorth = "Utara Magnetik",
    calibratedStatus = "KETEPATAN TINGGI",
    facingQibla = "Menghadap Kaabah",
    turnTowardsKaaba = "Pusing ke Arah Kaabah",

    surahTab = "Surah",
    pageTab = "Halaman",
    juzTab = "Juzuk",
    hizbTab = "Hizib",
    bookmarksTab = "Penanda",
    searchSurahPlaceholder = "Cari Surah mengikut nama atau nombor...",
    continueReadingBar = "Teruskan Bacaan",
    versesCount = "ayat",
    meccan = "Makkiyyah",
    medinan = "Madaniyyah",

    settingsTitle = "Tetapan & Konfigurasi",
    sectionGeneral = "UMUM & PENSETAN BAHASA",
    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    timeFormatLabel = "Format Masa 24 Jam",
    sectionPrayerCalc = "WAKTU SOLAT & PENGIRAAN",
    locationLabel = "Lokasi",
    methodLabel = "Kaedah Pengiraan",
    madhhabLabel = "Mazhab Fiqh (Asar)",
    adjustmentsLabel = "Pelarasan Minit Manual",
    hijriCalibrationLabel = "Kalibrasi Tarikh Hijrah",
    sectionAudioAlerts = "AUDIO AZAN & PEMBERITAHUAN",
    adhanCallLabel = "Panggilan Azan Tepat Waktu",
    perPrayerModesLabel = "Mod Makluman Setiap Solat",
    prePrayerReminderLabel = "Peringatan Sebelum Solat",
    adhanSoundLabel = "Suara Muazin Azan",
    adhanVolumeLabel = "Kelantangan & Ujian Audio Azan",
    vibrationOnlyLabel = "Getaran Sahaja (Senyap)",
    sectionQuran = "AL-QURAN & TILAWAH",
    riwayahLabel = "Riwayat Bacaan",
    scriptStyleLabel = "Gaya Khat Rasm Al-Quran",
    reciterLabel = "Qari Audio",
    translationLabel = "Terjemahan & Tafsir",
    arabicTextSizeLabel = "Saiz Teks Arab",
    sectionMasjidMode = "MOD MASJID & KEKHUSYUKAN",
    globalSilentLabel = "Mod Senyap Menyeluruh",
    autoMasjidModeLabel = "Mod Senyap Automatik Semasa Solat",
    autoMasjidDurationLabel = "Tempoh Senyap Semasa Solat",
    sectionSystemDiagnostics = "SISTEM & DIAGNOSTIK",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Penyelarasan Rangkaian & Sumber",
    resetDefaultsLabel = "Tetapkan Semula ke Asal",
    privacyPhilosophyTitle = "Privasi & Falsafah",

    cancel = "Batal",
    confirm = "Sahkan",
    save = "Simpan",
    done = "Selesai",
    close = "Tutup",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Bulan Hijrah penuh", // verify
            nextMonth = "Bulan berikutnya", // verify
            nextPrayerPrefix = "Seterusnya", // verify
            previousMonth = "Bulan sebelumnya", // verify
    ),
    more = UiStringsMore(
        actionSave = "Simpan",
        actionCancel = "Batal",
        actionClose = "Tutup",
        actionBack = "Kembali",
        actionReset = "Set semula",
        search = "Cari",
        clearSearch = "Kosongkan carian",
        loading = "Memuatkan",
        surahsTab = "Surah",
        referenceTab = "Rujukan",
        playVerse = "Mainkan ayat",
        pauseVerse = "Jeda",
        bookmarkVerse = "Tanda",
        removeBookmark = "Buang tanda",
        copyVerse = "Salin ayat",
        shareVerse = "Kongsi ayat",
        verseCopied = "Ayat disalin",
        textSize = "Saiz teks",
        layoutPerVerse = "Per ayat",
        layoutContinuous = "Bersambung",
        backToSurahsLabel = "Kembali ke surah",
        sectionAppearance = "Paparan",
        sectionLocationAndCalculation = "Lokasi dan pengiraan",
        sectionAlerts = "Amaran",
        sectionAbout = "Perihal",
        chooseLocation = "Pilih lokasi",
        useGps = "Guna GPS",
        customLocation = "Masukkan secara manual",
        searchCities = "Cari bandar",
        allCountriesTitle = "Semua negara",
        nameField = "Nama tempat",
        latitudeField = "Lintang",
        longitudeField = "Bujur",
        nameRequired = "Masukkan nama untuk tempat ini.",
        invalidCoordinates = "Masukkan lintang antara -90 hingga 90, dan bujur antara -180 hingga 180.",
        qiblaBearing = "Arah kiblat",
        isFacingQibla = "Anda menghadap kiblat",
        rightOfQibla = "ke kanan",
        leftOfQibla = "ke kiri",
        turnBy = "Pusing %1\$s %2\$s",
        alignedWithQibla = "Selaras dengan kiblat",
        locateMe = "Lokasi saya",
        holdFlatHint = "Pegang telefon secara rata untuk bacaan kompas yang tepat.",
        northReferenceLabel = "Rujukan utara",
        dialDescription = "Kompas, arah %1\$s. Kiblat %2\$s.",
        solarReferenceTitle = "Semak dengan matahari",
        calibrationTitle = "Kalibrasi kompas",
        magneticInterference = "Gangguan magnet",
        magneticInterferenceMessage = "Bacaan mungkin tidak tepat. Jauhkan daripada logam dan peranti elektronik.",
        alertAdhan = "Azan",
        alertTakbeer = "Takbir",
        alertChime = "Loceng",
        alertVibrate = "Getaran",
        alertSilent = "Senyap",
        changeAlertMode = "Tukar amaran",
        selected = "Dipilih",
        notSelected = "Belum dipilih",
        stateOn = "Aktif",
        stateOff = "Tidak aktif",
        noSearchResults = "Tiada hasil",
        noResultsMessage = "Cuba ejaan lain atau istilah yang lebih pendek.",
        allowNotificationsTitle = "Benarkan notifikasi Salah",
        allowNotificationsMessage = "Salah memerlukan kebenaran notifikasi untuk memanggil anda pada waktu solat. Waktu solat kekal dipaparkan pada skrin utama, dan anda boleh mengaktifkan amaran kemudian dalam Tetapan.",
        allowNotificationsAction = "Benarkan notifikasi",
        notNow = "Bukan sekarang",
        aboveHorizon = "Di atas horizon",
        actionChange = "Tukar",
        adhanAtPrayerLabel = "Azan pada waktu solat", // verify
        adhanPlayingLabel = "Azan sedang dimainkan", // verify
        adhanVolume = "Kelantangan", // verify
        alertSilentReminder = "Peringatan senyap", // verify
        allSurahsLabel = "Semua surah", // verify
        ambientField = "Medan sekitar",
        appliedAdjustments = "Pelarasan manual", // verify
        audioCacheCleared = "Cache audio dibersihkan", // verify
        audioSourceLabel = "Audio bacaan", // verify
        audioStreamedNotCached = "Distreamkan, tidak disimpan pada peranti", // verify
        autoSilenceDurationLabel = "Tempoh senyap automatik", // verify
        autoSilenceLabel = "Senyap automatik semasa solat", // verify
        belowHorizon = "Di bawah horizon",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Tentukur kompas",
        calibrationMessage = "Gerakkan peranti membentuk angka lapan beberapa kali agar sensor stabil.",
        cardsLayout = "Per ayat", // verify
        cardsModeLabel = "Kad", // verify
        changeLocation = "Tukar lokasi",
        checkForUpdates = "Semak sekarang",
        chooseAdhanSound = "Bunyi azan", // verify
        chooseAdjustments = "Pelarasan minit", // verify
        chooseHijriOffset = "Ofset tarih Hijrah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Pengiraan Asr", // verify
        chooseMethod = "Kaedah pengiraan", // verify
        choosePrePrayerOffset = "Peringatan sebelum solat", // verify
        chooseReciter = "Qori", // verify
        chooseRiwayah = "Riwayah", // verify
        chooseScript = "Skrip", // verify
        chooseTheme = "Tema",
        chooseTranslation = "Terjemahan", // verify
        closeReader = "Kembali ke senarai surah", // verify
        computedOnDevice = "Dikira pada peranti ini", // verify
        continuousLayout = "Berterusan", // verify
        continuousModeLabel = "Berterusan", // verify
        coordinatesCachedOffline = "Koordinat dicache luar talian. Pengiraan berjalan sepenuhnya pada peranti ini.",
        copyTodaySchedule = "Salin jadual hari ini", // verify
        copyVerseLabel = "Salin ayat", // verify
        corpusSummary = "114 surah · 30 juz · 6,236 ayat", // verify
        daysShort = "hari", // verify
        ephemerisCacheLabel = "Cache jadual solat", // verify
        gpsCached = "GPS dicache",
        hideTranslation = "Sembunyikan terjemahan", // verify
        hideTranslationLabel = "Sembunyikan", // verify
        hizbHalfFirst = "Separuh pertama", // verify
        hizbHalfSecond = "Separuh kedua", // verify
        hizbInJuz = "Juz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Pencahayaan",
        juzLabel = "Juz", // verify
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        lastVerified = "Terakhir disemak",
        loadingQuranMessage = "Menyediakan teks. Jika tidak selesai, kembali dan pilih surah lagi.",
        locationSaved = "Lokasi disimpan",
        lunarPhase = "Fasa bulan", // verify
        madhhabLabelShort = "Kaedah Asr", // verify
        methodology = "Kaedah", // verify
        minutesShort = "min", // verify
        nextDay = "Hari berikutnya", // verify
        nextSurahLabel = "Surah seterusnya", // verify
        noAdjustment = "Tiada pelarasan", // verify
        noBookmarksMessage = "Ketuk ikon tanda semasa membaca untuk menyimpan ayat di sini.", // verify
        noBookmarksTitle = "Tiada ayat disimpan", // verify
        noSurahMatchMessage = "Tiada surah sepadan. Cuba nombor, atau sebahagian nama.",
        nowReading = "Sedang dibaca", // verify
        observatorySubtitle = "Astronomi untuk %s",
        observatoryTitle = "Langit",
        pageWord = "Muka surat", // verify
        perPrayerModes = "Peringatan setiap solat", // verify
        prayerMarkedDone = "Solat ditandakan telah dikerjakan", // verify
        prayerMarkedPending = "Solat ditandakan belum dikerjakan", // verify
        prePrayerDisabled = "Dimatikan", // verify
        previewingTime = "Pratonton %s", // verify
        previousDay = "Hari sebelumnya", // verify
        previousSurahLabel = "Surah sebelumnya", // verify
        privacyNote = "Waktu solat, arah kiblat, dan Al-Quran semua dikira pada peranti ini. Tiada apa yang dimuat naik.",
        privacyPolicy = "Dasar privasi",
        qiblaSubtitle = "Arah ke Kaabah", // verify
        qiblaTitle = "Kiblat", // verify
        quranTitle = "Al-Quran", // verify
        readingLayout = "Tata letak", // verify
        readingOptions = "Pilihan bacaan", // verify
        readingSaved = "Kedudukan bacaan disimpan", // verify
        readingSettingsLabel = "Tetapan bacaan", // verify
        realTime = "Masa sebenar", // verify
        recitingLabel = "Membaca", // verify
        recomputeSchedule = "Kira semula jadual 365 hari", // verify
        referenceLabel = "Rujukan", // verify
        resetAdjustments = "Set semula semua pelarasan", // verify
        resetAllConfirmMessage = "Tetapan pengiraan, peringatan, dan paparan kembali ke lalai. Ayat dan catatan solat anda disimpan.",
        resetAllConfirmTitle = "Set semula semua tetapan?",
        resetAllLabel = "Set semula semua tetapan",
        scheduleCopied = "Jadual disalin", // verify
        searchHintMessage = "Cari surah mengikut nama atau maksud, atau ayat mengikut teks Arab atau Inggeris.", // verify
        searchHintTitle = "Cari dalam Al-Quran", // verify
        searchHizb = "Lompat ke nombor hizb", // verify
        searchJuz = "Lompat ke nombor juz", // verify
        searchPages = "Cari nombor muka surat", // verify
        searchSurahsAndVerses = "Cari surah dan ayat", // verify
        searchVersesHint = "Cari teks Arab atau terjemahan Inggeris...", // verify
        selectDate = "Pilih tarikh", // verify
        selectLayoutSubtitle = "Per ayat sesuai untuk belajar; berterusan sesuai untuk membaca menerus.", // verify
        selectLayoutTitle = "Tata letak bacaan", // verify
        selectSurah = "Pilih surah", // verify
        selectVerse = "Pilih ayat", // verify
        selectVerseHint = "Pilih ayat untuk membacanya di sini.", // verify
        selectedCity = "Bandar dipilih",
        sensorAccuracy = "Ketepatan sensor",
        settingsReset = "Tetapan ditetapkan semula",
        shareChooserTitle = "Kongsi ayat melalui",
        shareVerseLabel = "Kongsi ayat", // verify
        showTranslation = "Tunjukkan terjemahan", // verify
        showTranslationLabel = "Tunjukkan", // verify
        silenceAdhan = "Senyapkan", // verify
        silentModeLabel = "Senyapkan semua peringatan", // verify
        skyPeriodLabel = "Tempoh langit",
        solarAltitude = "Ketinggian matahari",
        solarAzimuth = "Azimut matahari",
        somethingWentWrong = "Sesuatu tidak kena",
        stopAudio = "Berhenti", // verify
        stopSound = "Hentikan bunyi",
        storageLabel = "Storan",
        sunAligned = "Matahari hampir mengikut arah kiblat. Hadapkan diri untuk sahkan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horizon, jadi ia tidak boleh dijadikan rujukan sekarang.",
        sunOpposite = "Kiblat bertentangan arah dengan matahari.", // verify
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        surahLabel = "Surah", // verify
        surahsFound = "%d surah", // verify
        tapVerseHint = "Ketuk ayat untuk memeriksanya.", // verify
        testSound = "Uji bunyi", // verify
        themeDark = "Gelap",
        themeLight = "Cerah",
        themeSystem = "Ikut sistem",
        timeFormat24hLabel = "Format 24 jam", // verify
        timeScrubber = "Waktu dalam sehari", // verify
        todayTitle = "Hari ini", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Inggeris — Saheeh International", // verify
        translationNotAvailable = "Terjemahan tidak tersedia", // verify
        translationSectionTitle = "Terjemahan", // verify
        translationShownFor = "Memaparkan %s", // verify
        trueNorthSuffix = "utara sebenar", // verify
        tryAgain = "Cuba lagi",
        useCurrentLocation = "Gunakan lokasi saya",
        verseActionsLabel = "Tindakan ayat", // verify
        verseCopiedToast = "Ayat disalin", // verify
        verseCount = "%d ayat", // verify
        verseOf = "Ayat %1\$d daripada %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayat", // verify
        versesLabel = "Ayat", // verify
        vibrateOnlyLabel = "Getaran sahaja", // verify
        waning = "Berkurangan", // verify
        waxing = "Bertambah", // verify
    ),
)

val BengaliStrings = UiStrings(
    appName = "সালাহ",
    navToday = "আজ",
    navPrayer = "নামাজ",
    navQuran = "কুরআন",
    navQibla = "কিবলা",
    navSettings = "সেটিংস",

    nextPrayerLabel = "পরবর্তী নামাজ",
    adhanAt = "আজান হবে",
    viewDetails = "বিস্তারিত দেখুন",
    currentPeriod = "বর্তমান ওয়াক্ত",
    skyPeriodSuffix = "আকাশ ✦",
    hoursUnit = "ঘণ্টা",
    minsUnit = "মিনিট",
    secsUnit = "সেকেন্ড",
    todaysPrayers = "আজকের নামাজের সময়সূচি",
    tapToMarkCompleted = "আদায় চিহ্নিত করতে ট্যাপ করুন",
    continueReading = "তেলাওয়াত চালিয়ে যান",
    continueButton = "চালিয়ে যান",
    sourceLabel = "উৎস",
    alertsActive = "সতর্কবার্তা সক্রিয়",
    silentModeOn = "সাইলেন্ট মোড চালু",
    masjidMode = "মসজিদ মোড",
    resetToRealtime = "রিয়েল-টাইমে ফিরুন",
    onlineStatus = "অনলাইন",
    offlineStatus = "অফলাইন",
    syncingStatus = "সিঙ্ক হচ্ছে",

    fajr = "ফজর",
    sunrise = "সূর্যোদয়",
    dhuhr = "জোহর",
    asr = "আসর",
    maghrib = "মাগরিব",
    isha = "ইশা",

    transparentCalculationSource = "স্বচ্ছ হিসাব পদ্ধতি ও উৎস",
    todaysTimes = "আজকের সময়সূচি",
    prayerTimesHeader = "নামাজের ওয়াক্ত",
    vigilsAndNightPeriods = "তাহাজ্জুদ ও রাতের ওয়াক্ত",
    imsakTitle = "ইমসাক (ফজরের ১০ মিনিট আগে)",
    midnightTitle = "শরয়ী মধ্যরাত",
    lastThirdTitle = "রাতের শেষ তৃতীয়াংশ (তাহাজ্জুদ)",
    monthlyCalendarHeader = "মাসিক ক্যালেন্ডার",
    todayBtn = "আজ",

    qiblaDirectionTitle = "কিবলার দিক",
    sensoryCompassSubtitle = "কাবা শরীফের সঠিক দিকনির্দেশক কম্পাস",
    magneticInterferenceDetected = "চৌম্বকীয় বিভ্রান্তি শনাক্ত হয়েছে",
    moveAwayMetal = "ধাতব বস্তু থেকে দূরে সরে যান বা ক্যালিব্রেট করুন।",
    kaabaDistance = "কাবা শরীফের দূরত্ব",
    trueNorth = "প্রকৃত উত্তর",
    magneticNorth = "চৌম্বকীয় উত্তর",
    calibratedStatus = "উচ্চ নির্ভুলতা",
    facingQibla = "কাবার সম্মুখবর্তী",
    turnTowardsKaaba = "কাবার দিকে মুখ করুন",

    surahTab = "সূরা",
    pageTab = "পৃষ্ঠা",
    juzTab = "পারা",
    hizbTab = "হিযব",
    bookmarksTab = "বুকমার্ক",
    searchSurahPlaceholder = "সূরা নাম বা নম্বর দিয়ে খুঁজুন...",
    continueReadingBar = "পড়া চালিয়ে যান",
    versesCount = "আয়াত",
    meccan = "মাক্কী",
    medinan = "মাদানী",

    settingsTitle = "সেটিংস ও কনফিগারেশন",
    sectionGeneral = "সাধারণ ও ভাষা",
    languageLabel = "ভাষা",
    appThemeLabel = "অ্যাপ থিম",
    timeFormatLabel = "২৪ ঘণ্টার সময় ফরম্যাট",
    sectionPrayerCalc = "নামাজের সময় ও হিসাব",
    locationLabel = "অবস্থান",
    methodLabel = "হিসাব পদ্ধতি",
    madhhabLabel = "আসর ফিকহ মাজহাব",
    adjustmentsLabel = "ম্যানুয়াল মিনিট সমন্বয়",
    hijriCalibrationLabel = "হিজরি তারিখ সমন্বয়",
    sectionAudioAlerts = "আজান ও বিজ্ঞপ্তি",
    adhanCallLabel = "ওয়াক্তে আজানের আহ্বান",
    perPrayerModesLabel = "প্রতি ওয়াক্তের সতর্কবার্তা মোড",
    prePrayerReminderLabel = "নামাজের আগের রিমাইন্ডার",
    adhanSoundLabel = "মুয়াজ্জিনের কণ্ঠ",
    adhanVolumeLabel = "আজানের সাউন্ড ও অডিও টেস্ট",
    vibrationOnlyLabel = "শুধুমাত্র কম্পন (নীরব)",
    sectionQuran = "পবিত্র কুরআন ও তেলাওয়াত",
    riwayahLabel = "ক্বেরাত রেওয়ায়েত",
    scriptStyleLabel = "কুরআনের ক্যালিগ্রাফি স্টাইল",
    reciterLabel = "ক্বারী কণ্ঠ",
    translationLabel = "অনুবাদ ও তাফসীর",
    arabicTextSizeLabel = "আরবি হরফের আকার",
    sectionMasjidMode = "মসজিদ মোড ও একাগ্রতা",
    globalSilentLabel = "সাধারণ সাইলেন্ট মোড",
    autoMasjidModeLabel = "নামাজে স্বয়ংক্রিয় নীরব মোড",
    autoMasjidDurationLabel = "নামাজে নীরবতার স্থায়িত্ব",
    sectionSystemDiagnostics = "সিস্টেম ও সেন্সর",
    compassDiagnosticsLabel = "কম্পাস সেন্সর ও ডায়াগনস্টিক",
    networkSyncLabel = "নেটওয়ার্ক সিঙ্ক ও উৎস",
    resetDefaultsLabel = "ফ্যাক্টরি সেটিংসে রিসেট করুন",
    privacyPhilosophyTitle = "গোপনীয়তা ও নীতি",

    cancel = "বাতিল",
    confirm = "নিশ্চিত",
    save = "সংরক্ষণ",
    done = "সম্পন্ন",
    close = "বন্ধ করুন",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "পূর্ণ হিজরি মাস", // verify
            nextMonth = "পরের মাস", // verify
            nextPrayerPrefix = "পরবর্তী", // verify
            previousMonth = "আগের মাস", // verify
    ),
    more = UiStringsMore(
        actionSave = "সংরক্ষণ",
        actionCancel = "বাতিল",
        actionClose = "বন্ধ",
        actionBack = "পিছনে",
        actionReset = "রিসেট",
        search = "অনুসন্ধান",
        clearSearch = "অনুসন্ধান মুছুন",
        loading = "লোড হচ্ছে",
        surahsTab = "সূরা",
        referenceTab = "সূচি",
        playVerse = "আয়াত চালান",
        pauseVerse = "বিরতি",
        bookmarkVerse = "বুকমার্ক যোগ করুন",
        removeBookmark = "বুকমার্ক সরান",
        copyVerse = "আয়াত কপি করুন",
        shareVerse = "আয়াত শেয়ার করুন",
        verseCopied = "আয়াত কপি হয়েছে",
        textSize = "লেখার আকার",
        layoutPerVerse = "প্রতি আয়াত",
        layoutContinuous = "ধারাবাহিক",
        backToSurahsLabel = "সূরাগুলোতে ফিরুন",
        sectionAppearance = "চেহারা",
        sectionLocationAndCalculation = "অবস্থান ও হিসাব",
        sectionAlerts = "সতর্কতা",
        sectionAbout = "সম্পর্কে",
        chooseLocation = "অবস্থান বেছে নিন",
        useGps = "GPS ব্যবহার করুন",
        customLocation = "ম্যানুয়লি লিখুন",
        searchCities = "শহর খুঁজুন",
        allCountriesTitle = "সব দেশ",
        nameField = "স্থানের নাম",
        latitudeField = "অক্ষাংশ",
        longitudeField = "দ্রাঘিমাংশ",
        nameRequired = "এই স্থানের একটি নাম লিখুন।",
        invalidCoordinates = "অক্ষাংশ -90 থেকে 90 এবং দ্রাঘিমাংশ -180 থেকে 180 এর মধ্যে দিন।",
        qiblaBearing = "কিবলার দিক",
        isFacingQibla = "আপনি কিবলার দিকে তাকিয়ে আছেন",
        rightOfQibla = "ডান দিকে",
        leftOfQibla = "বাম দিকে",
        turnBy = "%2\$s %1\$s ঘোরান",
        alignedWithQibla = "কিবলার সঙ্গে সমান্তরাল",
        locateMe = "আমার অবস্থান",
        holdFlatHint = "কম্পাসের সঠিক রিডিংয়ের জন্য ফোনটি সমতলভাবে ধরুন।",
        northReferenceLabel = "উত্তরের প্রসঙ্গ",
        dialDescription = "কম্পাস, দিক %1\$s। কিবলা %2\$s।",
        solarReferenceTitle = "সূর্য দিয়ে যাচাই করুন",
        calibrationTitle = "কম্পাস ক্যালিব্রেট করুন",
        magneticInterference = "চৌম্বকীয় বিভ্রান্তি",
        magneticInterferenceMessage = "রিডিং ভুল হতে পারে। ধাতু ও ইলেকট্রনিক যন্ত্র থেকে দূরে যান।",
        alertAdhan = "আজান",
        alertTakbeer = "তাকবীর",
        alertChime = "ঘণ্টা",
        alertVibrate = "কম্পন",
        alertSilent = "নীরব",
        changeAlertMode = "সতর্কতা পরিবর্তন",
        selected = "নির্বাচিত",
        notSelected = "নির্বাচিত নয়",
        stateOn = "চালু",
        stateOff = "বন্ধ",
        noSearchResults = "কোনো ফলাফল নেই",
        noResultsMessage = "অন্য বানান বা ছোট শব্দ চেষ্টা করুন।",
        allowNotificationsTitle = "Salah-কে আপনাকে অবহিত করতে দিন",
        allowNotificationsMessage = "নামাজের সময়ে আপনাকে সম্বন্ধ করতে Salah-কে বিজ্ঞপ্তির অনুমতি দরকার। নামাজের সময়সূচি মূল পর্দায়ই থাকে, এবং আপনি পরে সেটিংস থেকে সতর্কতা চালু করতে পারেন।",
        allowNotificationsAction = "বিজ্ঞপ্তি অনুমতি দিন",
        notNow = "এখন নয়",
        aboveHorizon = "ক্ষিতিজের উপরে",
        actionChange = "পরিবর্তন",
        adhanAtPrayerLabel = "নামাজের সময় আজান", // verify
        adhanPlayingLabel = "আজান বাজছে", // verify
        adhanVolume = "ভলিউম", // verify
        alertSilentReminder = "নীরব স্মরণিকা", // verify
        allSurahsLabel = "সব সূরা", // verify
        ambientField = "পরিবেশী ক্ষেত্র",
        appliedAdjustments = "ম্যানুয়াল সমন্বয়", // verify
        audioCacheCleared = "অডিও ক্যাশ মুছে ফেলা হয়েছে", // verify
        audioSourceLabel = "তিলাওয়ার অডিও", // verify
        audioStreamedNotCached = "স্ট্রিম হচ্ছে, ডিভাইসে সংরক্ষিত নয়", // verify
        autoSilenceDurationLabel = "স্বয়ংক্রিয় নীরবতার সময়কাল", // verify
        autoSilenceLabel = "নামাজের সময় স্বয়ংক্রিয় নীরবতা", // verify
        belowHorizon = "ক্ষিতিজের নিচে",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "কম্পাস ক্যালিব্রেট করুন",
        calibrationMessage = "সেন্সরটি স্থিতিশীল হতে ডিভাইসটি কয়েকবার ৮ আকারে নাড়ুন।",
        cardsLayout = "প্রতি আয়াত", // verify
        cardsModeLabel = "কার্ড", // verify
        changeLocation = "অবস্থান পরিবর্তন",
        checkForUpdates = "এখন পরীক্ষা করুন",
        chooseAdhanSound = "আজানের শব্দ", // verify
        chooseAdjustments = "মিনিট সমন্বয়", // verify
        chooseHijriOffset = "হিজরি তারিখ অফসেট", // verify
        chooseLanguage = "ভাষা",
        chooseMadhhab = "আসরের হিসাব", // verify
        chooseMethod = "গণনা পদ্ধতি", // verify
        choosePrePrayerOffset = "নামাজের আগে স্মরণিকা", // verify
        chooseReciter = "কারী", // verify
        chooseRiwayah = "রিওয়ায়া", // verify
        chooseScript = "লিপি", // verify
        chooseTheme = "থিম",
        chooseTranslation = "অনুবাদ", // verify
        closeReader = "সূরার তালিকায় ফিরুন", // verify
        computedOnDevice = "এই ডিভাইসে গণনা করা হয়", // verify
        continuousLayout = "ধারাবাহিক", // verify
        continuousModeLabel = "ধারাবাহিক", // verify
        coordinatesCachedOffline = "স্থানাঙ্ক অফলাইনে ক্যাশ করা হয়েছে। গণনা সম্পূর্ণ এই ডিভাইসে চলে।",
        copyTodaySchedule = "আজকের সময়সূচি কপি করুন", // verify
        copyVerseLabel = "আয়াত কপি করুন", // verify
        corpusSummary = "১১৪ সূরা · ৩০ জুয় · ৬,২৩৬ আয়াত", // verify
        daysShort = "দিন", // verify
        ephemerisCacheLabel = "নামাজের সময়সূচি ক্যাশ", // verify
        gpsCached = "জিপিএস ক্যাশ করা হয়েছে",
        hideTranslation = "অনুবাদ লুকান", // verify
        hideTranslationLabel = "লুকান", // verify
        hizbHalfFirst = "প্রথম অর্ধেক", // verify
        hizbHalfSecond = "দ্বিতীয় অর্ধেক", // verify
        hizbInJuz = "জুয় %d, %s", // verify
        hizbOf = "হিয়ব %d", // verify
        hizbWord = "হিয়ব", // verify
        illumination = "আলোকসজ্জা",
        juzLabel = "জুয়", // verify
        juzOf = "জুয় %d", // verify
        juzWord = "জুয়", // verify
        lastVerified = "সর্বশেষ পরীক্ষিত",
        loadingQuranMessage = "পাঠ্য প্রস্তুত হচ্ছে। এটি শেষ না হলে পেছনে যান এবং সূরাটি আবার নির্বাচন করুন।",
        locationSaved = "অবস্থান সংরক্ষিত",
        lunarPhase = "চাঁদের অবস্থা", // verify
        madhhabLabelShort = "আসর পদ্ধতি", // verify
        methodology = "পদ্ধতি", // verify
        minutesShort = "মিনিট", // verify
        nextDay = "পরের দিন", // verify
        nextSurahLabel = "পরবর্তী সূরা", // verify
        noAdjustment = "কোনো সমন্বয় নেই", // verify
        noBookmarksMessage = "এখানে আয়াত সংরক্ষণ করতে পড়ার সময় বুকমার্ক আইকন আঘাত করুন।", // verify
        noBookmarksTitle = "কোনো সংরক্ষিত আয়াত নেই", // verify
        noSurahMatchMessage = "এর সাথে মিলে এমন কোনো সূরা নেই। একটি নম্বর বা নামের অংশ চেষ্টা করুন।",
        nowReading = "এখন পড়া হচ্ছে", // verify
        observatorySubtitle = "%s এর জন্য জ্যোতির্বিজ্ঞান",
        observatoryTitle = "আকাশ",
        pageWord = "পৃষ্ঠা", // verify
        perPrayerModes = "প্রতিটি নামাজের জন্য সতর্কতা", // verify
        prayerMarkedDone = "নামাজ আদায়কৃত হিসেবে চিহ্নিত", // verify
        prayerMarkedPending = "নামাজ অনাদায়কৃত হিসেবে চিহ্নিত", // verify
        prePrayerDisabled = "নিষ্ক্রিয়", // verify
        previewingTime = "%s প্রিভিউ করা হচ্ছে", // verify
        previousDay = "আগের দিন", // verify
        previousSurahLabel = "পূর্ববর্তী সূরা", // verify
        privacyNote = "নামাজের সময়, কিবলার দিক এবং কুরআন সবই এই ডিভাইসে গণনা করা হয়। কিছুই আপলোড হয় না।",
        privacyPolicy = "গোপনীয়তা নীতি",
        qiblaSubtitle = "কাবার দিক", // verify
        qiblaTitle = "কিবলা", // verify
        quranTitle = "কুরআন", // verify
        readingLayout = "লেআউট", // verify
        readingOptions = "পড়ার বিকল্প", // verify
        readingSaved = "পড়ার অবস্থান সংরক্ষিত", // verify
        readingSettingsLabel = "পড়ার সেটিংস", // verify
        realTime = "প্রকৃত সময়", // verify
        recitingLabel = "তিলাওয়াত করছে", // verify
        recomputeSchedule = "৩৬৫ দিনের সময়সূচি পুনরায় গণনা", // verify
        referenceLabel = "রেফারেন্স", // verify
        resetAdjustments = "সমস্ত সমন্বয় পুনরায় সেট করুন", // verify
        resetAllConfirmMessage = "গণনা, সতর্কতা ও প্রদর্শনের সেটিংস ডিফল্টে ফিরে যায়। আপনার সংরক্ষিত আয়াত ও নামাজের বিবরণ থেকে যায়।",
        resetAllConfirmTitle = "সমস্ত সেটিংস পুনরায় সেট করবেন?",
        resetAllLabel = "সমস্ত সেটিংস পুনরায় সেট করুন",
        scheduleCopied = "সময়সূচি কপি হয়েছে", // verify
        searchHintMessage = "নাম বা অর্থ অনুযায়ী সূরা খুঁজুন, অথবা আরবি বা ইংরেজি পাঠ্য অনুযায়ী আয়াত খুঁজুন।", // verify
        searchHintTitle = "কুরআনে অনুসন্ধান", // verify
        searchHizb = "হিয়ব নম্বরে যান", // verify
        searchJuz = "জুয় নম্বরে যান", // verify
        searchPages = "পৃষ্ঠা নম্বর অনুসন্ধান", // verify
        searchSurahsAndVerses = "সূরা ও আয়াত অনুসন্ধান", // verify
        searchVersesHint = "আরবি পাঠ্য বা ইংরেজি অনুবাদ অনুসন্ধান...", // verify
        selectDate = "তারিখ নির্বাচন করুন", // verify
        selectLayoutSubtitle = "আয়াতভিত্তিক অধ্যয়নের জন্য উপযুক্ত; ধারাবাহিক পড়ার জন্য উপযুক্ত।", // verify
        selectLayoutTitle = "পড়ার লেআউট", // verify
        selectSurah = "সূরা নির্বাচন করুন", // verify
        selectVerse = "আয়াত নির্বাচন করুন", // verify
        selectVerseHint = "এখানে পড়ার জন্য আয়াত নির্বাচন করুন।", // verify
        selectedCity = "নির্বাচিত শহর",
        sensorAccuracy = "সেন্সরের নির্ভুলতা",
        settingsReset = "সেটিংস পুনরায় সেট হয়েছে",
        shareChooserTitle = "আয়াত শেয়ার করুন",
        shareVerseLabel = "আয়াত শেয়ার করুন", // verify
        showTranslation = "অনুবাদ দেখান", // verify
        showTranslationLabel = "দেখান", // verify
        silenceAdhan = "নীরব করুন", // verify
        silentModeLabel = "সমস্ত সতর্কতা নীরব করুন", // verify
        skyPeriodLabel = "আকাশের সময়কাল",
        solarAltitude = "সূর্যের উচ্চতা",
        solarAzimuth = "সূর্যের দিকনির্দেশ",
        somethingWentWrong = "কিছু একটা ভুল হয়েছে",
        stopAudio = "থামান", // verify
        stopSound = "শব্দ থামান",
        storageLabel = "সঞ্চয়",
        sunAligned = "সূর্য প্রায় কিবলার দিকে। নিশ্চিত করতে এর দিকে মুখ করুন।", // verify
        sunAltitude = "সূর্য",
        sunAltitudeValue = "সূর্যের উচ্চতা",
        sunAzimuth = "সূর্যের দিক",
        sunBelowHorizon = "সূর্য ক্ষিতিজের নিচে, তাই এটিকে এখন রেফারেন্স হিসেবে ব্যবহার করা যাবে না।",
        sunOpposite = "কিবলা সূর্যের বিপরীত দিকে।", // verify
        sunToTheLeft = "কিবলা সূর্যের বাম পাশে প্রায় %d°।", // verify
        sunToTheRight = "কিবলা সূর্যের ডান পাশে প্রায় %d°।", // verify
        surahLabel = "সূরা", // verify
        surahsFound = "%d সূরা", // verify
        tapVerseHint = "পরীক্ষা করতে আয়াতে আঘাত করুন।", // verify
        testSound = "শব্দ পরীক্ষা", // verify
        themeDark = "ঢাকা",
        themeLight = "উজ্জ্বল",
        themeSystem = "সিস্টেম অনুযায়ী",
        timeFormat24hLabel = "২৪ ঘণ্টার ফরম্যাট", // verify
        timeScrubber = "দিনের সময়", // verify
        todayTitle = "আজ", // verify
        translationCredit = "সহীহ ইন্টারন্যাশনাল", // verify
        translationCreditLine = "ইংরেজি — সহীহ ইন্টারন্যাশনাল", // verify
        translationNotAvailable = "অনুবাদ পাওয়া যায় না", // verify
        translationSectionTitle = "অনুবাদ", // verify
        translationShownFor = "%s দেখানো হচ্ছে", // verify
        trueNorthSuffix = "প্রকৃত উত্তর", // verify
        tryAgain = "আবার চেষ্টা করুন",
        useCurrentLocation = "আমার অবস্থান ব্যবহার করুন",
        verseActionsLabel = "আয়াতের কাজ", // verify
        verseCopiedToast = "আয়াত কপি হয়েছে", // verify
        verseCount = "%d আয়াত", // verify
        verseOf = "%2\$d এর মধ্যে %1\$d নম্বর আয়াত", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d আয়াত", // verify
        versesLabel = "আয়াত", // verify
        vibrateOnlyLabel = "শুধু কম্পন", // verify
        waning = "ক্ষীণমান", // verify
        waxing = "বর্ধমান", // verify
    ),
)

val RussianStrings = UiStrings(
    appName = "SALAH",
    navToday = "Сегодня",
    navPrayer = "Намаз",
    navQuran = "Коран",
    navQibla = "Кибла",
    navSettings = "Настройки",

    nextPrayerLabel = "СЛЕДУЮЩИЙ НАМАЗ",
    adhanAt = "Азан в",
    viewDetails = "Подробнее",
    currentPeriod = "Текущее время",
    skyPeriodSuffix = "Небо ✦",
    hoursUnit = "ЧАС",
    minsUnit = "МИН",
    secsUnit = "СЕК",
    todaysPrayers = "Расписание на сегодня",
    tapToMarkCompleted = "Нажмите, чтобы отметить выполненным",
    continueReading = "ПРОДОЛЖИТЬ ЧТЕНИЕ",
    continueButton = "Продолжить",
    sourceLabel = "Источник",
    alertsActive = "Оповещения активны",
    silentModeOn = "Беззвучный режим ВКЛ",
    masjidMode = "Режим мечети",
    resetToRealtime = "Вернуться к текущему времени",
    onlineStatus = "ОНЛАЙН",
    offlineStatus = "ОФФЛАЙН",
    syncingStatus = "СИНХРОНИЗАЦИЯ",

    fajr = "Фаджр",
    sunrise = "Восход",
    dhuhr = "Зухр",
    asr = "Аср",
    maghrib = "Магриб",
    isha = "Иша",

    transparentCalculationSource = "Прозрачный метод расчета",
    todaysTimes = "ВРЕМЯ НА СЕГОДНЯ",
    prayerTimesHeader = "ВРЕМЯ НАМАЗА",
    vigilsAndNightPeriods = "НОЧНЫЕ ПЕРИОДЫ И ТАХАДЖУД",
    imsakTitle = "Имсак (за 10 мин до Фаджра)",
    midnightTitle = "Исламская полночь",
    lastThirdTitle = "Последняя треть ночи (Тахаджуд)",
    monthlyCalendarHeader = "МЕСЯЧНЫЙ КАЛЕНДАРЬ",
    todayBtn = "Сегодня",

    qiblaDirectionTitle = "Направление Киблы",
    sensoryCompassSubtitle = "Точный компас с указанием на Каабу",
    magneticInterferenceDetected = "Обнаружены магнитные помехи",
    moveAwayMetal = "Отойдите от металлических предметов.",
    kaabaDistance = "Расстояние до Каабы",
    trueNorth = "Истинный север",
    magneticNorth = "Магнитный север",
    calibratedStatus = "ВЫСОКАЯ ТОЧНОСТЬ",
    facingQibla = "Вы смотрите на Каабу",
    turnTowardsKaaba = "Повернитесь в сторону Каабы",

    surahTab = "Сура",
    pageTab = "Стр",
    juzTab = "Джуз",
    hizbTab = "Хизб",
    bookmarksTab = "Закладки",
    searchSurahPlaceholder = "Поиск суры по названию или номеру...",
    continueReadingBar = "Продолжить чтение",
    versesCount = "аятов",
    meccan = "Мекканская",
    medinan = "Мединская",

    settingsTitle = "Настройки и конфигурация",
    sectionGeneral = "ОБЩИЕ И ЯЗЫК",
    languageLabel = "Язык интерфейса",
    appThemeLabel = "Тема приложения",
    timeFormatLabel = "24-часовой формат",
    sectionPrayerCalc = "ВРЕМЯ НАМАЗА И РАСЧЕТ",
    locationLabel = "Местоположение",
    methodLabel = "Метод расчета",
    madhhabLabel = "Мазхаб (Аср)",
    adjustmentsLabel = "Ручная корректировка (минуты)",
    hijriCalibrationLabel = "Калибровка календаря Хиджры",
    sectionAudioAlerts = "АЗАН И УВЕДОМЛЕНИЯ",
    adhanCallLabel = "Звук азана при наступлении времени",
    perPrayerModesLabel = "Режимы оповещения для каждого намаза",
    prePrayerReminderLabel = "Напоминание перед намазом",
    adhanSoundLabel = "Голос муэдзина",
    adhanVolumeLabel = "Громкость и проверка азана",
    vibrationOnlyLabel = "Только вибрация (Без звука)",
    sectionQuran = "СВЯЩЕННЫЙ КОРАН И ЧТЕНИЕ",
    riwayahLabel = "Риваят (Традиция чтения)",
    scriptStyleLabel = "Стиль шрифта Корана",
    reciterLabel = "Чтец Корана",
    translationLabel = "Перевод и тафсир",
    arabicTextSizeLabel = "Размер арабского шрифта",
    sectionMasjidMode = "РЕЖИМ МЕЧЕТИ И СПОКОЙСТВИЕ",
    globalSilentLabel = "Общий беззвучный режим",
    autoMasjidModeLabel = "Авто-беззвучный режим во время намаза",
    autoMasjidDurationLabel = "Длительность беззвучного режима",
    sectionSystemDiagnostics = "СИСТЕМА И ДАТЧИКИ",
    compassDiagnosticsLabel = "Датчики компаса и диагностика",
    networkSyncLabel = "Синхронизация с сетью и источник",
    resetDefaultsLabel = "Сброс к заводским настройкам",
    privacyPhilosophyTitle = "Конфиденциальность и философия",

    cancel = "Отмена",
    confirm = "Подтвердить",
    save = "Сохранить",
    done = "Готово",
    close = "Закрыть",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Полный месяц хиджры", // verify
            nextMonth = "Следующий месяц", // verify
            nextPrayerPrefix = "След.", // verify
            previousMonth = "Предыдущий месяц", // verify
    ),
    more = UiStringsMore(
        actionSave = "Сохранить",
        actionCancel = "Отмена",
        actionClose = "Закрыть",
        actionBack = "Назад",
        actionReset = "Сбросить",
        search = "Поиск",
        clearSearch = "Очистить поиск",
        loading = "Загрузка",
        surahsTab = "Суры",
        referenceTab = "Указатель",
        playVerse = "Воспроизвести аят",
        pauseVerse = "Пауза",
        bookmarkVerse = "В закладки",
        removeBookmark = "Убрать из закладок",
        copyVerse = "Копировать аят",
        shareVerse = "Поделиться аятом",
        verseCopied = "Аят скопирован",
        textSize = "Размер текста",
        layoutPerVerse = "По аятам",
        layoutContinuous = "Сплошной",
        backToSurahsLabel = "Назад к сурам",
        sectionAppearance = "Оформление",
        sectionLocationAndCalculation = "Местоположение и расчёт",
        sectionAlerts = "Оповещения",
        sectionAbout = "О приложении",
        chooseLocation = "Выберите местоположение",
        useGps = "Использовать GPS",
        customLocation = "Ввести вручную",
        searchCities = "Найти город",
        allCountriesTitle = "Все страны",
        nameField = "Название места",
        latitudeField = "Широта",
        longitudeField = "Долгота",
        nameRequired = "Введите название этого места.",
        invalidCoordinates = "Введите широту от -90 до 90 и долготу от -180 до 180.",
        qiblaBearing = "Направление на Каабу",
        isFacingQibla = "Вы обращены к Каабе",
        rightOfQibla = "вправо",
        leftOfQibla = "влево",
        turnBy = "Поверните на %1\$s %2\$s",
        alignedWithQibla = "Направление на Каабу",
        locateMe = "Моё местоположение",
        holdFlatHint = "Держите телефон горизонтально, чтобы компас показывал точно.",
        northReferenceLabel = "Опорный север",
        dialDescription = "Компас, курс %1\$s. Кибла %2\$s.",
        solarReferenceTitle = "Проверить по солнцу",
        calibrationTitle = "Откалибровать компас",
        magneticInterference = "Магнитные помехи",
        magneticInterferenceMessage = "Показания могут быть неточными. Отойдите от металла и электроники.",
        alertAdhan = "Азан",
        alertTakbeer = "Такбир",
        alertChime = "Перезвон",
        alertVibrate = "Вибрация",
        alertSilent = "Без звука",
        changeAlertMode = "Изменить оповещение",
        selected = "Выбрано",
        notSelected = "Не выбрано",
        stateOn = "Вкл.",
        stateOff = "Выкл.",
        noSearchResults = "Ничего не найдено",
        noResultsMessage = "Попробуйте другое написание или более короткий запрос.",
        allowNotificationsTitle = "Разрешить уведомления Salah",
        allowNotificationsMessage = "Salah нужен доступ к уведомлениям, чтобы напомнить вам о времени намаза. Расписание всё равно отображается на главном экране, а оповещения можно включить позже в настройках.",
        allowNotificationsAction = "Разрешить уведомления",
        notNow = "Не сейчас",
        aboveHorizon = "Над горизонтом",
        actionChange = "Изменить",
        adhanAtPrayerLabel = "Азан в время молитвы", // verify
        adhanPlayingLabel = "Азан звучит", // verify
        adhanVolume = "Громкость", // verify
        alertSilentReminder = "Тихое напоминание", // verify
        allSurahsLabel = "Все суры", // verify
        ambientField = "Окружающее поле",
        appliedAdjustments = "Ручные поправки", // verify
        audioCacheCleared = "Кэш аудио очищен", // verify
        audioSourceLabel = "Аудио чтения", // verify
        audioStreamedNotCached = "Потоком, не сохраняется на устройстве", // verify
        autoSilenceDurationLabel = "Длительность авто-тишины", // verify
        autoSilenceLabel = "Авто-тишина во время молитвы", // verify
        belowHorizon = "Под горизонтом",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Откалибровать компас",
        calibrationMessage = "Двигайте устройство в виде восьмёрки несколько раз, чтобы датчик стабилизировался.",
        cardsLayout = "По аятам", // verify
        cardsModeLabel = "Карточки", // verify
        changeLocation = "Изменить местоположение",
        checkForUpdates = "Проверить сейчас",
        chooseAdhanSound = "Звук азана", // verify
        chooseAdjustments = "Поправки в минутах", // verify
        chooseHijriOffset = "Смещение хиджры", // verify
        chooseLanguage = "Язык",
        chooseMadhhab = "Расчёт Асра", // verify
        chooseMethod = "Метод расчёта", // verify
        choosePrePrayerOffset = "Напоминание до молитвы", // verify
        chooseReciter = "Чтец", // verify
        chooseRiwayah = "Ривая", // verify
        chooseScript = "Письмо", // verify
        chooseTheme = "Тема",
        chooseTranslation = "Перевод", // verify
        closeReader = "Назад к сурам", // verify
        computedOnDevice = "Вычисляется на этом устройстве", // verify
        continuousLayout = "Непрерывный", // verify
        continuousModeLabel = "Непрерывный", // verify
        coordinatesCachedOffline = "Координаты закэшированы офлайн. Расчёты выполняются полностью на этом устройстве.",
        copyTodaySchedule = "Скопировать время на сегодня", // verify
        copyVerseLabel = "Скопировать аят", // verify
        corpusSummary = "114 сур · 30 джузов · 6236 аятов", // verify
        daysShort = "дн", // verify
        ephemerisCacheLabel = "Кэш расписания молитвы", // verify
        gpsCached = "GPS закэширован",
        hideTranslation = "Скрыть перевод", // verify
        hideTranslationLabel = "Скрыть", // verify
        hizbHalfFirst = "Первая половина", // verify
        hizbHalfSecond = "Вторая половина", // verify
        hizbInJuz = "Джуз %d, %s", // verify
        hizbOf = "Хизб %d", // verify
        hizbWord = "Хизб", // verify
        illumination = "Освещённость",
        juzLabel = "Джуз", // verify
        juzOf = "Джуз %d", // verify
        juzWord = "Джуз", // verify
        lastVerified = "Последняя проверка",
        loadingQuranMessage = "Подготовка текста. Если это не завершится, вернитесь назад и выберите суру снова.",
        locationSaved = "Местоположение сохранено",
        lunarPhase = "Фаза луны", // verify
        madhhabLabelShort = "Метод Асра", // verify
        methodology = "Методика", // verify
        minutesShort = "мин", // verify
        nextDay = "Следующий день", // verify
        nextSurahLabel = "Следующая сура", // verify
        noAdjustment = "Без поправок", // verify
        noBookmarksMessage = "Нажмите значок закладки во время чтения, чтобы сохранить здесь аят.", // verify
        noBookmarksTitle = "Нет сохранённых аятов", // verify
        noSurahMatchMessage = "Нет подходящей суры. Попробуйте номер или часть названия.",
        nowReading = "Сейчас читается", // verify
        observatorySubtitle = "Астрономия для %s",
        observatoryTitle = "Небо",
        pageWord = "Страница", // verify
        perPrayerModes = "Напоминание для каждой молитвы", // verify
        prayerMarkedDone = "Молитва отмечена как совершённая", // verify
        prayerMarkedPending = "Молитва отмечена как несовершённая", // verify
        prePrayerDisabled = "Выкл.", // verify
        previewingTime = "Просмотр %s", // verify
        previousDay = "Предыдущий день", // verify
        previousSurahLabel = "Предыдущая сура", // verify
        privacyNote = "Время молитв, направление киблы и Коран вычисляются на этом устройстве. Ничего не отправляется.",
        privacyPolicy = "Политика конфиденциальности",
        qiblaSubtitle = "Направление на Каабу", // verify
        qiblaTitle = "Кибла", // verify
        quranTitle = "Коран", // verify
        readingLayout = "Макет", // verify
        readingOptions = "Настройки чтения", // verify
        readingSaved = "Позиция чтения сохранена", // verify
        readingSettingsLabel = "Настройки чтения", // verify
        realTime = "Реальное время", // verify
        recitingLabel = "Читает", // verify
        recomputeSchedule = "Пересчитать расписание на 365 дней", // verify
        referenceLabel = "Ссылка", // verify
        resetAdjustments = "Сбросить все поправки", // verify
        resetAllConfirmMessage = "Настройки расчёта, оповещений и отображения возвращаются к значениям по умолчанию. Сохранённые аяты и журнал молитв сохраняются.",
        resetAllConfirmTitle = "Сбросить все настройки?",
        resetAllLabel = "Сбросить все настройки",
        scheduleCopied = "Время скопировано", // verify
        searchHintMessage = "Найти суру по названию или значению, или аят по арабскому или английскому тексту.", // verify
        searchHintTitle = "Поиск в Коране", // verify
        searchHizb = "Перейти к номеру хибза", // verify
        searchJuz = "Перейти к номеру джуза", // verify
        searchPages = "Поиск по номеру страницы", // verify
        searchSurahsAndVerses = "Поиск сур и аятов", // verify
        searchVersesHint = "Поиск в арабском тексте или английском переводе...", // verify
        selectDate = "Выбрать дату", // verify
        selectLayoutSubtitle = "По аятам — для изучения; непрерывный — для чтения подряд.", // verify
        selectLayoutTitle = "Макет чтения", // verify
        selectSurah = "Выбрать суру", // verify
        selectVerse = "Выбрать аят", // verify
        selectVerseHint = "Выберите аят, чтобы прочитать его здесь.", // verify
        selectedCity = "Выбранный город",
        sensorAccuracy = "Точность датчика",
        settingsReset = "Настройки сброшены",
        shareChooserTitle = "Поделиться аятом через",
        shareVerseLabel = "Поделиться аятом", // verify
        showTranslation = "Показать перевод", // verify
        showTranslationLabel = "Показать", // verify
        silenceAdhan = "Выключить", // verify
        silentModeLabel = "Выключить все оповещения", // verify
        skyPeriodLabel = "Период неба",
        solarAltitude = "Высота солнца",
        solarAzimuth = "Азимут солнца",
        somethingWentWrong = "Что-то пошло не так",
        stopAudio = "Остановить", // verify
        stopSound = "Остановить звук",
        storageLabel = "Хранилище",
        sunAligned = "Солнце почти в направлении киблы. Повернитесь к нему, чтобы подтвердить.", // verify
        sunAltitude = "Солнце",
        sunAltitudeValue = "Высота солнца",
        sunAzimuth = "Направление солнца",
        sunBelowHorizon = "Солнце под горизонтом, поэтому сейчас его нельзя использовать как ориентир.",
        sunOpposite = "Кибла в направлении, противоположном солнцу.", // verify
        sunToTheLeft = "Кибла примерно в %d° слева от солнца.", // verify
        sunToTheRight = "Кибла примерно в %d° справа от солнца.", // verify
        surahLabel = "Сура", // verify
        surahsFound = "%d сур", // verify
        tapVerseHint = "Нажмите на аят, чтобы его рассмотреть.", // verify
        testSound = "Проверить звук", // verify
        themeDark = "Тёмная",
        themeLight = "Светлая",
        themeSystem = "Как в системе",
        timeFormat24hLabel = "24-часовой формат", // verify
        timeScrubber = "Время суток", // verify
        todayTitle = "Сегодня", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Английский — Saheeh International", // verify
        translationNotAvailable = "Перевод недоступен", // verify
        translationSectionTitle = "Перевод", // verify
        translationShownFor = "Показано %s", // verify
        trueNorthSuffix = "истинный север", // verify
        tryAgain = "Повторить",
        useCurrentLocation = "Использовать моё местоположение",
        verseActionsLabel = "Действия с аятом", // verify
        verseCopiedToast = "Аят скопирован", // verify
        verseCount = "%d аятов", // verify
        verseOf = "Аят %1\$d из %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d аятов", // verify
        versesLabel = "Аяты", // verify
        vibrateOnlyLabel = "Только вибрация", // verify
        waning = "Убывающая", // verify
        waxing = "Растущая", // verify
    ),
)

val GermanStrings = UiStrings(
    appName = "SALAH",
    navToday = "Heute",
    navPrayer = "Gebet",
    navQuran = "Koran",
    navQibla = "Qibla",
    navSettings = "Einstellungen",

    nextPrayerLabel = "NÄCHSTES GEBET",
    adhanAt = "Adhan um",
    viewDetails = "Details ansehen",
    currentPeriod = "Aktuelle Zeit",
    skyPeriodSuffix = "Himmel ✦",
    hoursUnit = "STD",
    minsUnit = "MIN",
    secsUnit = "SEK",
    todaysPrayers = "Heutige Gebetszeiten",
    tapToMarkCompleted = "Tippen zum Abhaken",
    continueReading = "LESEN FORTSETZEN",
    continueButton = "Fortsetzen",
    sourceLabel = "Quelle",
    alertsActive = "Erinnerungen aktiv",
    silentModeOn = "Stummmodus AN",
    masjidMode = "Moschee-Modus",
    resetToRealtime = "Zurück zur Echtzeit",
    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",
    syncingStatus = "SYNCHRONISIERUNG",

    fajr = "Fajr",
    sunrise = "Sonnenaufgang",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Transparente Berechnungsmethode",
    todaysTimes = "HEUTIGE ZEITEN",
    prayerTimesHeader = "GEBETSZEITEN",
    vigilsAndNightPeriods = "NACHTWACHEN & TAHAJJUD",
    imsakTitle = "Imsak (10 Min vor Fajr)",
    midnightTitle = "Islamische Mitternacht",
    lastThirdTitle = "Letztes Drittel der Nacht (Tahajjud)",
    monthlyCalendarHeader = "MONATSKALENDER",
    todayBtn = "Heute",

    qiblaDirectionTitle = "Qibla-Richtung",
    sensoryCompassSubtitle = "Präziser Kompass zur Kaaba",
    magneticInterferenceDetected = "Magnetische Störung erkannt",
    moveAwayMetal = "Von Metallgegenständen entfernen oder kalibrieren.",
    kaabaDistance = "Entfernung zur Kaaba",
    trueNorth = "Geographisch Nord",
    magneticNorth = "Magnetisch Nord",
    calibratedStatus = "HOHE GENAUIGKEIT",
    facingQibla = "Ausgerichtet zur Kaaba",
    turnTowardsKaaba = "Drehen Sie sich zur Kaaba",

    surahTab = "Sure",
    pageTab = "Seite",
    juzTab = "Juz'",
    hizbTab = "Hizb",
    bookmarksTab = "Lesezeichen",
    searchSurahPlaceholder = "Sure nach Name oder Nummer suchen...",
    continueReadingBar = "Weiterlesen",
    versesCount = "Verse",
    meccan = "Mekkanisch",
    medinan = "Medinensisch",

    settingsTitle = "Einstellungen & Konfiguration",
    sectionGeneral = "ALLGEMEIN & SPRACHE",
    languageLabel = "Sprache",
    appThemeLabel = "App-Design",
    timeFormatLabel = "24-Stunden-Zeitformat",
    sectionPrayerCalc = "GEBETSZEITEN & BERECHNUNG",
    locationLabel = "Standort",
    methodLabel = "Berechnungsmethode",
    madhhabLabel = "Rechtsschule (Asr)",
    adjustmentsLabel = "Manuelle Minutenanpassung",
    hijriCalibrationLabel = "Hidschri-Kalibrierung",
    sectionAudioAlerts = "ADHAN & BENACHRICHTIGUNGEN",
    adhanCallLabel = "Gebetsruf zur Gebetszeit",
    perPrayerModesLabel = "Benachrichtigungsmodi pro Gebet",
    prePrayerReminderLabel = "Erinnerung vor dem Gebet",
    adhanSoundLabel = "Muezzin-Stimme",
    adhanVolumeLabel = "Lautstärke & Audiotest",
    vibrationOnlyLabel = "Nur Vibration (Lautlos)",
    sectionQuran = "DER HEILIGE KORAN & REZITATION",
    riwayahLabel = "Riwayah (Überlieferungstradition)",
    scriptStyleLabel = "Schriftstil des Korans",
    reciterLabel = "Rezitator",
    translationLabel = "Übersetzung & Exegese",
    arabicTextSizeLabel = "Arabische Schriftgröße",
    sectionMasjidMode = "MOSCHEE-MODUS & RUHE",
    globalSilentLabel = "Globaler Stummmodus",
    autoMasjidModeLabel = "Automatischer Stummmodus beim Gebet",
    autoMasjidDurationLabel = "Dauer der Stummschaltung",
    sectionSystemDiagnostics = "SYSTEM & SENSOREN",
    compassDiagnosticsLabel = "Kompasssensoren & Diagnose",
    networkSyncLabel = "Netzwerksynchronisation & Quelle",
    resetDefaultsLabel = "Auf Werkseinstellungen zurücksetzen",
    privacyPhilosophyTitle = "Datenschutz & Philosophie",

    cancel = "Abbrechen",
    confirm = "Bestätigen",
    save = "Speichern",
    done = "Fertig",
    close = "Schließen",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Ganzer Hijri-Monat", // verify
            nextMonth = "Nächster Monat", // verify
            nextPrayerPrefix = "Nächst.", // verify
            previousMonth = "Vorheriger Monat", // verify
    ),
    more = UiStringsMore(
        actionSave = "Speichern",
        actionCancel = "Abbrechen",
        actionClose = "Schließen",
        actionBack = "Zurück",
        actionReset = "Zurücksetzen",
        search = "Suchen",
        clearSearch = "Suche löschen",
        loading = "Wird geladen",
        surahsTab = "Suren",
        referenceTab = "Verzeichnis",
        playVerse = "Vers vortragen",
        pauseVerse = "Pausieren",
        bookmarkVerse = "Lesezeichen setzen",
        removeBookmark = "Lesezeichen entfernen",
        copyVerse = "Vers kopieren",
        shareVerse = "Vers teilen",
        verseCopied = "Vers kopiert",
        textSize = "Schriftgröße",
        layoutPerVerse = "Vers für Vers",
        layoutContinuous = "Fortlaufend",
        backToSurahsLabel = "Zurück zu den Suren",
        sectionAppearance = "Darstellung",
        sectionLocationAndCalculation = "Standort & Berechnung",
        sectionAlerts = "Hinweise",
        sectionAbout = "Über",
        chooseLocation = "Standort wählen",
        useGps = "GPS verwenden",
        customLocation = "Manuell eingeben",
        searchCities = "Stadt suchen",
        allCountriesTitle = "Alle Länder",
        nameField = "Ortsname",
        latitudeField = "Breitengrad",
        longitudeField = "Längengrad",
        nameRequired = "Geben Sie einen Namen für diesen Ort ein.",
        invalidCoordinates = "Breitengrad zwischen -90 und 90, Längengrad zwischen -180 und 180 eingeben.",
        qiblaBearing = "Qibla-Richtung",
        isFacingQibla = "Sie blicken zur Qibla",
        rightOfQibla = "nach rechts",
        leftOfQibla = "nach links",
        turnBy = "Drehen Sie um %1\$s %2\$s",
        alignedWithQibla = "Auf die Qibla ausgerichtet",
        locateMe = "Mein Standort",
        holdFlatHint = "Halten Sie das Telefon flach, damit der Kompass genau liest.",
        northReferenceLabel = "Nordreferenz",
        dialDescription = "Kompass, Richtung %1\$s. Qibla %2\$s.",
        solarReferenceTitle = "Mit der Sonne prüfen",
        calibrationTitle = "Kompass kalibrieren",
        magneticInterference = "Magnetische Störung",
        magneticInterferenceMessage = "Die Messung kann ungenau sein. Entfernen Sie sich von Metall und Elektronik.",
        alertAdhan = "Adhan",
        alertTakbeer = "Takbeer",
        alertChime = "Glockenton",
        alertVibrate = "Vibration",
        alertSilent = "Stumm",
        changeAlertMode = "Hinweis ändern",
        selected = "Ausgewählt",
        notSelected = "Nicht ausgewählt",
        stateOn = "Ein",
        stateOff = "Aus",
        noSearchResults = "Keine Ergebnisse",
        noResultsMessage = "Andere Schreibweise oder einen kürzeren Begriff versuchen.",
        allowNotificationsTitle = "Salah darf Sie benachrichtigen",
        allowNotificationsMessage = "Salah benötigt die Benachrichtigungsberechtigung, um Sie zur Gebetszeit zu erinnern. Die Gebetszeiten bleiben ohnehin auf dem Startbildschirm sichtbar; Hinweise können Sie später in den Einstellungen aktivieren.",
        allowNotificationsAction = "Benachrichtigungen erlauben",
        notNow = "Jetzt nicht",
        aboveHorizon = "Über dem Horizont",
        actionChange = "Ändern",
        adhanAtPrayerLabel = "Adhan zur Gebetszeit", // verify
        adhanPlayingLabel = "Adhan läuft", // verify
        adhanVolume = "Lautstärke", // verify
        alertSilentReminder = "Stille Erinnerung", // verify
        allSurahsLabel = "Alle Suren", // verify
        ambientField = "Umgebungsfeld",
        appliedAdjustments = "Manuelle Anpassungen", // verify
        audioCacheCleared = "Audio-Cache geleert", // verify
        audioSourceLabel = "Rezitations-Audio", // verify
        audioStreamedNotCached = "Gestreamt, nicht auf dem Gerät gespeichert", // verify
        autoSilenceDurationLabel = "Dauer der automatischen Stille", // verify
        autoSilenceLabel = "Automatische Stille während des Gebets", // verify
        belowHorizon = "Unter dem Horizont",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Kompass kalibrieren",
        calibrationMessage = "Bewegen Sie das Gerät mehrmals in einer Acht, damit der Sensor sich stabilisiert.",
        cardsLayout = "Pro Vers", // verify
        cardsModeLabel = "Karten", // verify
        changeLocation = "Standort ändern",
        checkForUpdates = "Jetzt prüfen",
        chooseAdhanSound = "Adhan-Klang", // verify
        chooseAdjustments = "Minuten-Anpassungen", // verify
        chooseHijriOffset = "Hijri-Datum-Versatz", // verify
        chooseLanguage = "Sprache",
        chooseMadhhab = "Asr-Berechnung", // verify
        chooseMethod = "Berechnungsmethode", // verify
        choosePrePrayerOffset = "Erinnerung vor dem Gebet", // verify
        chooseReciter = "Rezitator", // verify
        chooseRiwayah = "Riwaya", // verify
        chooseScript = "Schrift", // verify
        chooseTheme = "Design",
        chooseTranslation = "Übersetzung", // verify
        closeReader = "Zurück zu den Suren", // verify
        computedOnDevice = "Wird auf diesem Gerät berechnet", // verify
        continuousLayout = "Kontinuierlich", // verify
        continuousModeLabel = "Kontinuierlich", // verify
        coordinatesCachedOffline = "Koordinaten offline zwischengespeichert. Die Berechnungen laufen vollständig auf diesem Gerät.",
        copyTodaySchedule = "Heutige Zeiten kopieren", // verify
        copyVerseLabel = "Vers kopieren", // verify
        corpusSummary = "114 Suren · 30 Dschuz · 6.236 Verse", // verify
        daysShort = "T", // verify
        ephemerisCacheLabel = "Gebetszeit-Cache", // verify
        gpsCached = "GPS zwischengespeichert",
        hideTranslation = "Übersetzung ausblenden", // verify
        hideTranslationLabel = "Ausblenden", // verify
        hizbHalfFirst = "1. Hälfte", // verify
        hizbHalfSecond = "2. Hälfte", // verify
        hizbInJuz = "Dschuz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Beleuchtung",
        juzLabel = "Dschuz", // verify
        juzOf = "Dschuz %d", // verify
        juzWord = "Dschuz", // verify
        lastVerified = "Zuletzt geprüft",
        loadingQuranMessage = "Der Text wird vorbereitet. Wenn dies nicht abgeschlossen wird, gehen Sie zurück und wählen Sie die Sure erneut.",
        locationSaved = "Standort gespeichert",
        lunarPhase = "Mondphase", // verify
        madhhabLabelShort = "Asr-Methode", // verify
        methodology = "Methode", // verify
        minutesShort = "Min", // verify
        nextDay = "Nächster Tag", // verify
        nextSurahLabel = "Nächste Sure", // verify
        noAdjustment = "Keine Anpassung", // verify
        noBookmarksMessage = "Tippen Sie beim Lesen auf das Lesezeichen-Symbol, um hier einen Vers zu speichern.", // verify
        noBookmarksTitle = "Keine gespeicherten Verse", // verify
        noSurahMatchMessage = "Keine Sure passt dazu. Versuchen Sie eine Nummer oder einen Teil des Namens.",
        nowReading = "Wird gerade gelesen", // verify
        observatorySubtitle = "Astronomie für %s",
        observatoryTitle = "Himmel",
        pageWord = "Seite", // verify
        perPrayerModes = "Erinnerung pro Gebet", // verify
        prayerMarkedDone = "Gebet als verrichtet markiert", // verify
        prayerMarkedPending = "Gebet als nicht verrichtet markiert", // verify
        prePrayerDisabled = "Aus", // verify
        previewingTime = "%s wird angezeigt", // verify
        previousDay = "Vorheriger Tag", // verify
        previousSurahLabel = "Vorherige Sure", // verify
        privacyNote = "Gebetszeiten, Qibla-Richtung und der Koran werden auf diesem Gerät berechnet. Es wird nichts hochgeladen.",
        privacyPolicy = "Datenschutzerklärung",
        qiblaSubtitle = "Richtung zur Kaaba", // verify
        qiblaTitle = "Qibla", // verify
        quranTitle = "Koran", // verify
        readingLayout = "Layout", // verify
        readingOptions = "Leseoptionen", // verify
        readingSaved = "Leseposition gespeichert", // verify
        readingSettingsLabel = "Lese-Einstellungen", // verify
        realTime = "Echtzeit", // verify
        recitingLabel = "Rezitiert", // verify
        recomputeSchedule = "365-Tages-Plan neu berechnen", // verify
        referenceLabel = "Referenz", // verify
        resetAdjustments = "Alle Anpassungen zurücksetzen", // verify
        resetAllConfirmMessage = "Berechnungs-, Warn- und Anzeigeeinstellungen werden auf die Standardwerte zurückgesetzt. Ihre gespeicherten Verse und Ihr Gebetsprotokoll bleiben erhalten.",
        resetAllConfirmTitle = "Alle Einstellungen zurücksetzen?",
        resetAllLabel = "Alle Einstellungen zurücksetzen",
        scheduleCopied = "Zeiten kopiert", // verify
        searchHintMessage = "Finden Sie eine Sure nach Name oder Bedeutung oder einen Vers nach arabischem oder englischem Text.", // verify
        searchHintTitle = "Im Koran suchen", // verify
        searchHizb = "Zur Hizb-Nummer springen", // verify
        searchJuz = "Zur Dschuz-Nummer springen", // verify
        searchPages = "Seitennummer suchen", // verify
        searchSurahsAndVerses = "Suren und Verse suchen", // verify
        searchVersesHint = "Arabischen Text oder englische Übersetzung suchen...", // verify
        selectDate = "Datum auswählen", // verify
        selectLayoutSubtitle = "Pro Vers eignet sich zum Studium, kontinuierlich zum fortlaufenden Lesen.", // verify
        selectLayoutTitle = "Lese-Layout", // verify
        selectSurah = "Sure auswählen", // verify
        selectVerse = "Vers auswählen", // verify
        selectVerseHint = "Wählen Sie einen Vers, um ihn hier zu lesen.", // verify
        selectedCity = "Ausgewählte Stadt",
        sensorAccuracy = "Sengenauigkeit",
        settingsReset = "Einstellungen zurückgesetzt",
        shareChooserTitle = "Vers teilen über",
        shareVerseLabel = "Vers teilen", // verify
        showTranslation = "Übersetzung einblenden", // verify
        showTranslationLabel = "Einblenden", // verify
        silenceAdhan = "Stumm schalten", // verify
        silentModeLabel = "Alle Warnungen stummschalten", // verify
        skyPeriodLabel = "Himmelszeitraum",
        solarAltitude = "Sonnenhöhe",
        solarAzimuth = "Sonnenazimut",
        somethingWentWrong = "Etwas ist schiefgelaufen",
        stopAudio = "Stopp", // verify
        stopSound = "Ton stoppen",
        storageLabel = "Speicher",
        sunAligned = "Die Sonne steht fast in Qibla-Richtung. Drehen Sie sich ihr zu, um zu bestätigen.", // verify
        sunAltitude = "Sonne",
        sunAltitudeValue = "Sonnenhöhe",
        sunAzimuth = "Sonnenrichtung",
        sunBelowHorizon = "Die Sonne steht unter dem Horizont und kann deshalb gerade nicht als Referenz dienen.",
        sunOpposite = "Die Qibla liegt in der entgegengesetzten Richtung zur Sonne.", // verify
        sunToTheLeft = "Die Qibla liegt etwa %d° links von der Sonne.", // verify
        sunToTheRight = "Die Qibla liegt etwa %d° rechts von der Sonne.", // verify
        surahLabel = "Sure", // verify
        surahsFound = "%d Suren", // verify
        tapVerseHint = "Tippen Sie auf einen Vers, um ihn anzusehen.", // verify
        testSound = "Ton testen", // verify
        themeDark = "Dunkel",
        themeLight = "Hell",
        themeSystem = "System folgen",
        timeFormat24hLabel = "24-Stunden-Format", // verify
        timeScrubber = "Tageszeit", // verify
        todayTitle = "Heute", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Englisch — Saheeh International", // verify
        translationNotAvailable = "Übersetzung nicht verfügbar", // verify
        translationSectionTitle = "Übersetzung", // verify
        translationShownFor = "%s wird angezeigt", // verify
        trueNorthSuffix = "rechter Norden", // verify
        tryAgain = "Erneut versuchen",
        useCurrentLocation = "Meinen Standort verwenden",
        verseActionsLabel = "Vers-Aktionen", // verify
        verseCopiedToast = "Vers kopiert", // verify
        verseCount = "%d Verse", // verify
        verseOf = "Vers %1\$d von %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d Verse", // verify
        versesLabel = "Verse", // verify
        vibrateOnlyLabel = "Nur vibrieren", // verify
        waning = "Abnehmend", // verify
        waxing = "Zunehmend", // verify
    ),
)

val SpanishStrings = UiStrings(
    appName = "SALAH",
    navToday = "Hoy",
    navPrayer = "Oración",
    navQuran = "Corán",
    navQibla = "Alquibla",
    navSettings = "Ajustes",

    nextPrayerLabel = "PRÓXIMA ORACIÓN",
    adhanAt = "Adhan a las",
    viewDetails = "Ver detalles",
    currentPeriod = "Período actual",
    skyPeriodSuffix = "Cielo ✦",
    hoursUnit = "HORAS",
    minsUnit = "MIN",
    secsUnit = "SEG",
    todaysPrayers = "Horarios de hoy",
    tapToMarkCompleted = "Toca para marcar cumplida",
    continueReading = "CONTINUAR LEYENDO",
    continueButton = "Continuar",
    sourceLabel = "Fuente",
    alertsActive = "Alertas activas",
    silentModeOn = "Modo silencioso ACTIVADO",
    masjidMode = "Modo Mezquita",
    resetToRealtime = "Restablecer a tiempo real",
    onlineStatus = "EN LÍNEA",
    offlineStatus = "SIN CONEXIÓN",
    syncingStatus = "SINCRONIZANDO",

    fajr = "Fajr",
    sunrise = "Amanecer",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Fuente de cálculo transparente",
    todaysTimes = "HORARIOS DE HOY",
    prayerTimesHeader = "HORARIOS DE ORACIÓN",
    vigilsAndNightPeriods = "VIGILIAS Y NOCHE",
    imsakTitle = "Imsak (10 min antes de Fajr)",
    midnightTitle = "Medianoche islámica",
    lastThirdTitle = "Último tercio de la noche (Taháyyud)",
    monthlyCalendarHeader = "CALENDARIO MENSUAL",
    todayBtn = "Hoy",

    qiblaDirectionTitle = "Dirección de la Alquibla",
    sensoryCompassSubtitle = "Brújula sensorial orientada hacia la Kaaba",
    magneticInterferenceDetected = "Interferencia magnética detectada",
    moveAwayMetal = "Aléjese de objetos metálicos o calibre.",
    kaabaDistance = "Distancia a la Kaaba",
    trueNorth = "Norte verdadero",
    magneticNorth = "Norte magnético",
    calibratedStatus = "ALTA PRECISIÓN",
    facingQibla = "Frente a la Kaaba",
    turnTowardsKaaba = "Gire hacia la Kaaba",

    surahTab = "Sura",
    pageTab = "Pág",
    juzTab = "Yuz'",
    hizbTab = "Hizb",
    bookmarksTab = "Marcadores",
    searchSurahPlaceholder = "Buscar sura por nombre o número...",
    continueReadingBar = "Continuar lectura",
    versesCount = "aleyas",
    meccan = "Mequí",
    medinan = "Mediní",

    settingsTitle = "Ajustes y Configuración",
    sectionGeneral = "GENERAL Y LOCALIZACIÓN",
    languageLabel = "Idioma",
    appThemeLabel = "Tema de la aplicación",
    timeFormatLabel = "Formato de hora 24h",
    sectionPrayerCalc = "HORARIOS Y CÁLCULO",
    locationLabel = "Ubicación",
    methodLabel = "Método de cálculo",
    madhhabLabel = "Escuela jurídica (Asr)",
    adjustmentsLabel = "Ajustes manuales (minutos)",
    hijriCalibrationLabel = "Calibración fecha hiyri",
    sectionAudioAlerts = "AUDIO ADHAN Y NOTIFICACIONES",
    adhanCallLabel = "Llamada a la oración puntual",
    perPrayerModesLabel = "Modos de alerta por oración",
    prePrayerReminderLabel = "Recordatorio antes de la oración",
    adhanSoundLabel = "Voz del muecín",
    adhanVolumeLabel = "Volumen y prueba de audio",
    vibrationOnlyLabel = "Solo vibración (Silencioso)",
    sectionQuran = "EL NOBLE CORÁN Y RECITACIÓN",
    riwayahLabel = "Riwāyah (Tradición de recitación)",
    scriptStyleLabel = "Estilo de caligrafía coránica",
    reciterLabel = "Recitador de audio",
    translationLabel = "Traducción y exégesis",
    arabicTextSizeLabel = "Tamaño del texto árabe",
    sectionMasjidMode = "MODO MEZQUITA Y SOSIEGO",
    globalSilentLabel = "Modo silencioso global",
    autoMasjidModeLabel = "Modo silencioso automático en oración",
    autoMasjidDurationLabel = "Duración del silencio en oración",
    sectionSystemDiagnostics = "SISTEMA Y SENSORES",
    compassDiagnosticsLabel = "Sensores de brújula y diagnóstico",
    networkSyncLabel = "Sincronización de red y fuente",
    resetDefaultsLabel = "Restablecer a valores de fábrica",
    privacyPhilosophyTitle = "Privacidad y Filosofía",

    cancel = "Cancelar",
    confirm = "Confirmar",
    save = "Guardar",
    done = "Listo",
    close = "Cerrar",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Mes hégira completo", // verify
            nextMonth = "Mes siguiente", // verify
            nextPrayerPrefix = "Próxima", // verify
            previousMonth = "Mes anterior", // verify
    ),
    more = UiStringsMore(
        actionSave = "Guardar",
        actionCancel = "Cancelar",
        actionClose = "Cerrar",
        actionBack = "Atrás",
        actionReset = "Restablecer",
        search = "Buscar",
        clearSearch = "Borrar búsqueda",
        loading = "Cargando",
        surahsTab = "Suras",
        referenceTab = "Referencia",
        playVerse = "Reproducir el versículo",
        pauseVerse = "Pausar",
        bookmarkVerse = "Añadir marcador",
        removeBookmark = "Quitar marcador",
        copyVerse = "Copiar el versículo",
        shareVerse = "Compartir el versículo",
        verseCopied = "Versículo copiado",
        textSize = "Tamaño del texto",
        layoutPerVerse = "Versículo a versículo",
        layoutContinuous = "Continuo",
        backToSurahsLabel = "Volver a las suras",
        sectionAppearance = "Apariencia",
        sectionLocationAndCalculation = "Ubicación y cálculo",
        sectionAlerts = "Alertas",
        sectionAbout = "Acerca de",
        chooseLocation = "Elegir ubicación",
        useGps = "Usar GPS",
        customLocation = "Introducir manualmente",
        searchCities = "Buscar ciudad",
        allCountriesTitle = "Todos los países",
        nameField = "Nombre del lugar",
        latitudeField = "Latitud",
        longitudeField = "Longitud",
        nameRequired = "Introduce un nombre para este lugar.",
        invalidCoordinates = "Introduce una latitud entre -90 y 90, y una longitud entre -180 y 180.",
        qiblaBearing = "Dirección de la qibla",
        isFacingQibla = "Está mirando a la qibla",
        rightOfQibla = "hacia la derecha",
        leftOfQibla = "hacia la izquierda",
        turnBy = "Gire %1\$s %2\$s",
        alignedWithQibla = "Alineado con la qibla",
        locateMe = "Mi ubicación",
        holdFlatHint = "Mantenga el teléfono plano para una lectura precisa de la brújula.",
        northReferenceLabel = "Referencia norte",
        dialDescription = "Brújula, rumbo %1\$s. Alquibla %2\$s.",
        solarReferenceTitle = "Comprobar con el sol",
        calibrationTitle = "Calibrar la brújula",
        magneticInterference = "Interferencia magnética",
        magneticInterferenceMessage = "La lectura puede ser imprecisa. Aléjese de metales y dispositivos electrónicos.",
        alertAdhan = "Adhan",
        alertTakbeer = "Takbeer",
        alertChime = "Campanita",
        alertVibrate = "Vibración",
        alertSilent = "Silencio",
        changeAlertMode = "Cambiar alerta",
        selected = "Seleccionado",
        notSelected = "No seleccionado",
        stateOn = "Activado",
        stateOff = "Desactivado",
        noSearchResults = "Sin resultados",
        noResultsMessage = "Prueba otra ortografía o un término más corto.",
        allowNotificationsTitle = "Permite que Salah te avise",
        allowNotificationsMessage = "Salah necesita permiso de notificaciones para avisarte a la hora de la oración. Los horarios siguen visibles en la pantalla de inicio, y puedes activar las alertas más tarde en Ajustes.",
        allowNotificationsAction = "Permitir notificaciones",
        notNow = "Ahora no",
        aboveHorizon = "Sobre el horizonte",
        actionChange = "Cambiar",
        adhanAtPrayerLabel = "Adhan a la hora de la oración", // verify
        adhanPlayingLabel = "Adhan sonando", // verify
        adhanVolume = "Volumen", // verify
        alertSilentReminder = "Recordatorio silencioso", // verify
        allSurahsLabel = "Todas las suras", // verify
        ambientField = "Campo ambiental",
        appliedAdjustments = "Ajustes manuales", // verify
        audioCacheCleared = "Caché de audio borrada", // verify
        audioSourceLabel = "Audio de la recitación", // verify
        audioStreamedNotCached = "En transmisión, no se guarda en el dispositivo", // verify
        autoSilenceDurationLabel = "Duración del silencio automático", // verify
        autoSilenceLabel = "Silencio automático durante la oración", // verify
        belowHorizon = "Bajo el horizonte",
        calculationSource = "%s · %s", // verify
        calibrateCompass = "Calibrar la brújula",
        calibrationMessage = "Mueva el dispositivo en forma de ocho varias veces para que el sensor se estabilice.",
        cardsLayout = "Por versículo", // verify
        cardsModeLabel = "Tarjetas", // verify
        changeLocation = "Cambiar ubicación",
        checkForUpdates = "Comprobar ahora",
        chooseAdhanSound = "Sonido del adhan", // verify
        chooseAdjustments = "Ajustes en minutos", // verify
        chooseHijriOffset = "Desfase de la fecha hégira", // verify
        chooseLanguage = "Idioma",
        chooseMadhhab = "Cálculo del Asr", // verify
        chooseMethod = "Método de cálculo", // verify
        choosePrePrayerOffset = "Recordatorio antes de la oración", // verify
        chooseReciter = "Recitador", // verify
        chooseRiwayah = "Riwaya", // verify
        chooseScript = "Escritura", // verify
        chooseTheme = "Tema",
        chooseTranslation = "Traducción", // verify
        closeReader = "Volver a las suras", // verify
        computedOnDevice = "Calculado en este dispositivo", // verify
        continuousLayout = "Continuo", // verify
        continuousModeLabel = "Continuo", // verify
        coordinatesCachedOffline = "Coordenadas en caché sin conexión. Los cálculos se realizan por completo en este dispositivo.",
        copyTodaySchedule = "Copiar los horarios de hoy", // verify
        copyVerseLabel = "Copiar el versículo", // verify
        corpusSummary = "114 suras · 30 yuz · 6236 versículos", // verify
        daysShort = "d", // verify
        ephemerisCacheLabel = "Caché del horario de oración", // verify
        gpsCached = "GPS en caché",
        hideTranslation = "Ocultar la traducción", // verify
        hideTranslationLabel = "Ocultar", // verify
        hizbHalfFirst = "Primera mitad", // verify
        hizbHalfSecond = "Segunda mitad", // verify
        hizbInJuz = "Yuz %d, %s", // verify
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Iluminación",
        juzLabel = "Yuz", // verify
        juzOf = "Yuz %d", // verify
        juzWord = "Yuz", // verify
        lastVerified = "Última comprobación",
        loadingQuranMessage = "Preparando el texto. Si no termina, vuelve y elige la sura de nuevo.",
        locationSaved = "Ubicación guardada",
        lunarPhase = "Fase lunar", // verify
        madhhabLabelShort = "Método del Asr", // verify
        methodology = "Metodología", // verify
        minutesShort = "min", // verify
        nextDay = "Día siguiente", // verify
        nextSurahLabel = "Siguiente sura", // verify
        noAdjustment = "Sin ajustes", // verify
        noBookmarksMessage = "Toca el icono de marcador mientras lees para guardar un versículo aquí.", // verify
        noBookmarksTitle = "No hay versículos guardados", // verify
        noSurahMatchMessage = "Ninguna sura coincide. Prueba un número o parte de un nombre.",
        nowReading = "Leyendo ahora", // verify
        observatorySubtitle = "Astronomía para %s",
        observatoryTitle = "Cielo",
        pageWord = "Página", // verify
        perPrayerModes = "Alerta por oración", // verify
        prayerMarkedDone = "Oración marcada como realizada", // verify
        prayerMarkedPending = "Oración marcada como no realizada", // verify
        prePrayerDisabled = "Desactivado", // verify
        previewingTime = "Vista previa de %s", // verify
        previousDay = "Día anterior", // verify
        previousSurahLabel = "Sur anterior", // verify
        privacyNote = "Los horarios de oración, la dirección de la qibla y el Corán se calculan en este dispositivo. No se sube nada.",
        privacyPolicy = "Política de privacidad",
        qiblaSubtitle = "Dirección a la Kaaba", // verify
        qiblaTitle = "Qibla", // verify
        quranTitle = "Corán", // verify
        readingLayout = "Diseño", // verify
        readingOptions = "Opciones de lectura", // verify
        readingSaved = "Posición de lectura guardada", // verify
        readingSettingsLabel = "Ajustes de lectura", // verify
        realTime = "Tiempo real", // verify
        recitingLabel = "Recitando", // verify
        recomputeSchedule = "Recalcular el horario de 365 días", // verify
        referenceLabel = "Referencia", // verify
        resetAdjustments = "Restablecer todos los ajustes", // verify
        resetAllConfirmMessage = "Los ajustes de cálculo, alerta y visualización vuelven a sus valores predeterminados. Tus versículos guardados y tu registro de oraciones se conservan.",
        resetAllConfirmTitle = "¿Restablecer todos los ajustes?",
        resetAllLabel = "Restablecer todos los ajustes",
        scheduleCopied = "Horarios copiados", // verify
        searchHintMessage = "Busca una sura por nombre o significado, o un versículo por su texto árabe o inglés.", // verify
        searchHintTitle = "Buscar en el Corán", // verify
        searchHizb = "Ir al número de hizb", // verify
        searchJuz = "Ir al número de yuz", // verify
        searchPages = "Buscar por número de página", // verify
        searchSurahsAndVerses = "Buscar suras y versículos", // verify
        searchVersesHint = "Buscar en el texto árabe o la traducción al inglés...", // verify
        selectDate = "Seleccionar fecha", // verify
        selectLayoutSubtitle = "Por versículo conviene para estudiar; continuo para leer seguido.", // verify
        selectLayoutTitle = "Diseño de lectura", // verify
        selectSurah = "Seleccionar sura", // verify
        selectVerse = "Seleccionar versículo", // verify
        selectVerseHint = "Selecciona un versículo para leerlo aquí.", // verify
        selectedCity = "Ciudad seleccionada",
        sensorAccuracy = "Precisión del sensor",
        settingsReset = "Ajustes restablecidos",
        shareChooserTitle = "Compartir versículo por",
        shareVerseLabel = "Compartir versículo", // verify
        showTranslation = "Mostrar la traducción", // verify
        showTranslationLabel = "Mostrar", // verify
        silenceAdhan = "Silenciar", // verify
        silentModeLabel = "Silenciar todas las alertas", // verify
        skyPeriodLabel = "Período del cielo",
        solarAltitude = "Altitud del sol",
        solarAzimuth = "Azimut del sol",
        somethingWentWrong = "Algo salió mal",
        stopAudio = "Detener", // verify
        stopSound = "Detener el sonido",
        storageLabel = "Almacenamiento",
        sunAligned = "El sol está casi en la dirección de la qibla. Enfréntalo para confirmar.", // verify
        sunAltitude = "Sol",
        sunAltitudeValue = "Altitud del sol",
        sunAzimuth = "Dirección del sol",
        sunBelowHorizon = "El sol está bajo el horizonte, así que no puede usarse como referencia ahora.",
        sunOpposite = "La qibla está en dirección opuesta al sol.", // verify
        sunToTheLeft = "La qibla está aproximadamente %d° a la izquierda del sol.", // verify
        sunToTheRight = "La qibla está aproximadamente %d° a la derecha del sol.", // verify
        surahLabel = "Sura", // verify
        surahsFound = "%d suras", // verify
        tapVerseHint = "Toca un versículo para examinarlo.", // verify
        testSound = "Probar el sonido", // verify
        themeDark = "Oscuro",
        themeLight = "Claro",
        themeSystem = "Igual al sistema",
        timeFormat24hLabel = "Formato de 24 horas", // verify
        timeScrubber = "Momento del día", // verify
        todayTitle = "Hoy", // verify
        translationCredit = "Saheeh International", // verify
        translationCreditLine = "Inglés — Saheeh International", // verify
        translationNotAvailable = "Traducción no disponible", // verify
        translationSectionTitle = "Traducción", // verify
        translationShownFor = "Mostrando %s", // verify
        trueNorthSuffix = "norte verdadero", // verify
        tryAgain = "Intentar de nuevo",
        useCurrentLocation = "Usar mi ubicación",
        verseActionsLabel = "Acciones del versículo", // verify
        verseCopiedToast = "Versículo copiado", // verify
        verseCount = "%d versículos", // verify
        verseOf = "Versículo %1\$d de %2\$d", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d versículos", // verify
        versesLabel = "Versículos", // verify
        vibrateOnlyLabel = "Solo vibrar", // verify
        waning = "Menguante", // verify
        waxing = "Creciente", // verify
    ),
)

object LocalizationManager {
    fun getStrings(languageNameOrCode: String): UiStrings {
        return when (AppLanguage.fromNameOrCode(languageNameOrCode)) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.ARABIC -> ArabicStrings
            AppLanguage.FRENCH -> FrenchStrings
            AppLanguage.INDONESIAN -> IndonesianStrings
            AppLanguage.TURKISH -> TurkishStrings
            AppLanguage.URDU -> UrduStrings
            AppLanguage.MALAY -> MalayStrings
            AppLanguage.BENGALI -> BengaliStrings
            AppLanguage.RUSSIAN -> RussianStrings
            AppLanguage.GERMAN -> GermanStrings
            AppLanguage.SPANISH -> SpanishStrings
        }
    }
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }

val UiStrings.navPrayers: String
    get() = navPrayer

val UiStrings.gotIt: String
    get() = done

val UiStrings.ayahLabel: String
    get() = when (this) {
        ArabicStrings -> "آية"
        UrduStrings -> "آیت"
        TurkishStrings -> "Ayet"
        IndonesianStrings -> "Ayat"
        MalayStrings -> "Ayat"
        BengaliStrings -> "আয়াত"
        RussianStrings -> "Аят"
        GermanStrings -> "Vers"
        SpanishStrings -> "Aleya"
        FrenchStrings -> "Verset"
        else -> "Ayah"
    }

/**
 * Quran reader strings (nour-style reading experience).
 * English defaults; other languages fall back to English until translated.
 */

/**
 * Is the interface in Arabic?
 *
 * The Today page's headline follows the interface language, and Arabic leads
 * only when the reader chose Arabic - so this needs to answer that question, not
 * "is this right-to-left", because Urdu is also RTL and has its own name for a
 * prayer that should lead in Urdu.
 */
fun isArabicInterface(language: String): Boolean =
    AppLanguage.entries.firstOrNull {
        it.code == language ||
            it.englishName.equals(language, ignoreCase = true) ||
            it.nativeName == language
    }?.code == "ar"

fun UiStrings.prayerName(prayer: com.example.data.model.Prayer): String {
    return when (prayer) {
        com.example.data.model.Prayer.FAJR -> fajr
        com.example.data.model.Prayer.SUNRISE -> sunrise
        com.example.data.model.Prayer.DHUHR -> dhuhr
        com.example.data.model.Prayer.ASR -> asr
        com.example.data.model.Prayer.MAGHRIB -> maghrib
        com.example.data.model.Prayer.ISHA -> isha
    }
}

/**
 * The display label for a stored prayer-alert mode.
 *
 * The five mode strings are **persisted preference keys**, not copy. They are
 * compared by name in AdhanAudioSynthesizer, PrayerNotificationManager,
 * PrayerAlertService and SalahViewModel, and they are already written into
 * existing users' SharedPreferences. They must stay English and stable.
 *
 * What was missing was a display layer: Home and Settings both rendered these
 * keys straight to the screen, so in Arabic, Urdu and the other ten languages
 * every prayer row showed an English word. This maps the key to a label without
 * touching storage, which is why it is safe to add.
 */
fun UiStringsMore.alertModeLabel(storedValue: String): String = when (storedValue) {
    "Full Adhan" -> alertAdhan
    "Takbeer Only" -> alertTakbeer
    "Gentle Chime" -> alertChime
    "Vibrate Only" -> alertVibrate
    "Silent Reminder" -> alertSilentReminder
    else -> alertSilent
}

/**
 * Applies the selected language.
 *
 * Also sets [LocalLayoutDirection]. The project previously tracked
 * `AppLanguage.isRtl` (true for Arabic and Urdu) but never used it, so Arabic
 * and Urdu interfaces rendered left-to-right with the labels merely swapped.
 * Providing the direction here means `start`/`end` padding, row order and the
 * auto-mirrored icons all follow the language, in one place.
 */
@Composable
fun ProvideAppLanguage(language: String, content: @Composable () -> Unit) {
    val strings = LocalizationManager.getStrings(language)
    val direction = if (AppLanguage.fromNameOrCode(language).isRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides direction
    ) {
        content()
    }
}
