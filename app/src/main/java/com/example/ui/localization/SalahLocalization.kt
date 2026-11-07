package com.example.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

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
    val navQibla: String,
    val navSettings: String,

    // Home / Today Screen
    val onlineStatus: String,
    val offlineStatus: String,

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

    // Qibla Screen
    val kaabaDistance: String,
    val trueNorth: String,
    val magneticNorth: String,

    // Bottom navigation label kept for language-instance compatibility

    // Quran Screen (kept for binary compatibility with language instances;
    // no longer referenced by any screen)

    // Settings Screen
    val languageLabel: String,
    val appThemeLabel: String,
    val locationLabel: String,
    val methodLabel: String,
    val madhhabLabel: String,
    val adjustmentsLabel: String,
    val hijriCalibrationLabel: String,
    val adhanCallLabel: String,
    val prePrayerReminderLabel: String,
    val compassDiagnosticsLabel: String,
    val networkSyncLabel: String,
    val privacyPhilosophyTitle: String,

    // Dialog & Common Buttons
    val cancel: String,
    val done: String,
    val close: String,

    /**
     * Copy added by the redesign.
     */
    val more: UiStringsMore = UiStringsMore(),
    val dateNav: DateNavStrings = DateNavStrings(),

    /**
     * The system-access layer's actions and status messages. Kept out of
     * [UiStringsMore] so the permission wording can grow without changing the
     * constructor every screen already uses.
     */
    val system: SystemAccessStrings = SystemAccessStrings()
)

/** Copy the redesign added, kept in its own class to stay under the JVM method limit. */
/**
 * Strings for the clock page: the 24-hour dial, the windows a worshipper
 * watches for, and the day pager.
 */
/**
 * The immersive reader's strings.
 */
data class ReaderStrings(
    /** Opens the surah / saved / search index. */
    /**
     * "Madinah" in every language: the proper noun the print is named after,
     * like the fonts below - print names are not translated, paper names are.
     * Each language still owns its spelling, so the field is not a default.
     */

    // --- Paper names --------------------------------------------------------
    //
    // These seven are ordinary words and are translated in all ten languages.
    // They used to be an English `when` in `ReadingOptionsSheet.paperLabel` - "Rose",
    // "Apricot", "Sand", "Sage", "Mist", "Indigo", "Lilac" - which made the sheet's
    // one visible label and each swatch's *contentDescription* English in every
    // language. Same defect as the reader's announcement, reached from the other end:
    // a screen-reader user choosing a background heard "Apricot".
    //
    // A **typeface** name is deliberately not here. Amiri, Lateef and Harmattan are
    // names of the designs, not words, and translating them would make a face
    // unrecognisable to anyone who has seen it - which is why `faceLabel` stays
    // English and says why.

    /** The rose paper. */
    val paperRose: String = "Rose",

    /** The apricot paper. */

    /** The sand paper. */

    /** The sage paper. */

    /** The mist paper. */

    /** The indigo paper. */

    /** The lilac paper. */
    // --- Spoken descriptions ------------------------------------------------
    //
    // The mushaf page and the continuous block used to be announced as pre-rendered
    // English - "Page 42, juz' 21, surah 2 to 2, 15 verses, from 2:255 to 2:255" -
    // while the *page number* in the pill beside them was already localized, on the
    // same screen and about the same thing. A screen-reader user with the interface
    // in Arabic heard an English sentence and then an Arabic word, from one node.
    //
    // Format templates rather than finished sentences, for two reasons. A template
    // can carry each language's own word order, which a shared one cannot - Turkish
    // and Urdu do not put the number where English does. And the point is that the
    // *words* are translated, so the words have to live here rather than in a
    // `private fun` in the reader that cannot reach any of this.

    /**
     * A whole mushaf page, announced: page, juz', surah range, verse count, extent.
     */

    /** A block of continuous text, announced: extent, verse count, page. */

    /** The counted form for a single verse, e.g. "1 verse". */
    val verseCountOne: String = "%d verse",

    /** The counted form for more than one, e.g. "15 verses". */
    val verseCountMany: String = "%d verses",

    /**
     * The word joining a range's two ends, e.g. "2 to 3".
     *
     * A whole string for one preposition, which looks like over-engineering until
     * the day one language needs "bis", another needs "à", and a third writes the
     * range as an en-dash and wants an empty string.
     */
    val rangeTo: String = " to ",

    // --- The Qibla dial readouts --------------------------------------------
    val headingLabel: String = "Heading",

    // --- Compass directions ------------------------------------------------
    //
    // These were a private `when` in `QiblaDirectionFinder` returning the English
    // abbreviations "N", "NE", "E" ... and they were *visible*: the caption under the
    // heading readout, and the text beside the azimuth. So a reader in Arabic saw
    // "NE" under a numeral that meant something else to them, in an app that
    // translates everything else it says about the compass.
    //
    // German also disagrees: it writes *NO* for north-east where English writes *NE*,
    // because *Nordost* abbreviates to NO and not NE. So this is not a case of
    // borrowing, and a shared abbreviation would have been wrong in a language with a
    // Latin script.
    //
    // On [cardinal]: a member, not eight fields read at two call sites, because the
    // eight *boundaries* are the part that must not be duplicated - the old pair of
    // call sites each held their own idea of where north-east starts.
    val cardinalNorth: String = "N",
    val cardinalNorthEast: String = "NE",
    val cardinalEast: String = "E",
    val cardinalSouthEast: String = "SE",
    val cardinalSouth: String = "S",
    val cardinalSouthWest: String = "SW",
    val cardinalWest: String = "W",
    val cardinalNorthWest: String = "NW",
    /** Shown under the Settings summary row for a setting the reader owns. */
) {
    /**
     * "1 verse" or "15 verses", in this language.
     *
     * A member rather than a helper beside one call site, because two call sites
     * need it - the mushaf page and the continuous block - and the two must not be
     * able to disagree about what "1" reads as.
     *
     * **Two forms, not a plural rule, and that is a real limitation.** Arabic
     * distinguishes one / two / a few / many and Russian has three forms, so "3
     * verses" reads as `3 آية` where `3 آيات` is correct, and a Russian "2" gets the
     * many-form. Both are the common case and both stay comprehensible; the
     * alternative is a per-language plural-rule table for one number in one
     * sentence, which is a lot of machinery to get wrong in ten languages.
     *
     * `Locale.ROOT`, so a count reads in the same digits as the reference beside it
     * and the page number in the reader's pill. One convention for one fact.
     */
    fun verseCount(count: Int): String = if (count == 1) {
        String.format(java.util.Locale.ROOT, verseCountOne, count)
    } else {
        String.format(java.util.Locale.ROOT, verseCountMany, count)
    }

    /**
     * A range as this language writes one: its two ends, joined by [rangeTo].
     *
     * One end when the two are the same, because "An-Nisa to An-Nisa" is a range
     * nobody meant to write. 553 of the 604 pages want that single end; the other
     * 51 cross a surah boundary and want both.
     */
    fun range(from: String, to: String): String =
        if (from == to) from else "$from$rangeTo$to"

    /**
     * A range of surah numbers as the page announcement writes one, e.g. "2 to 3".
     */
    fun range(from: Int, to: Int): String = range(from.toString(), to.toString())

    /** A verse as `surah:ayah` for an announcement. Never locale-formatted. */
    fun reference(surah: Int, ayah: Int): String = "$surah:$ayah"

    /**
     * The compass direction [azimuthDegrees] points towards, in this language.
     */
    fun cardinal(azimuthDegrees: Float): String {
        val norm = ((azimuthDegrees % 360f) + 360f) % 360f
        return when {
            norm < 22.5f || norm >= 337.5f -> cardinalNorth
            norm < 67.5f -> cardinalNorthEast
            norm < 112.5f -> cardinalEast
            norm < 157.5f -> cardinalSouthEast
            norm < 202.5f -> cardinalSouth
            norm < 247.5f -> cardinalSouthWest
            norm < 292.5f -> cardinalWest
            else -> cardinalNorthWest
        }
    }
}

data class DateNavStrings(
    val nextPrayerPrefix: String = "Next",
    val fullHijriMonth: String = "Full Hijri month",
    val previousMonth: String = "Previous month",
    val nextMonth: String = "Next month",
    val nextDay: String = "Next day",
    val previousDay: String = "Previous day"
)

/**
 * Everything that is not reader-specific.
 *
 * **249 `String` fields, which is past a limit — see `RobolectricPackages.kt` in the
 * test sources.**
 *
 * **That is a mitigation, not a fix.** A data class with 249 constructor parameters is
 * not a list of strings; it is a list of strings nobody has reviewed as a group, and it
 * will cross a real limit on a lower one. The right shape is several nested classes —
 * `ReaderStrings` is already one, at 52 — each covering a surface: notifications, the
 * Hijri calendar, the sky. That is deliberate work over this whole file and every call
 * site, and it is not smuggled in beside a bug fix. It is the next thing to do here.
 */
/**
 * Everything a notification says.
 *
 * ### Why these are not fields on `UiStringsMore`
 *
 * `UiStringsMore` had reached 249 `String` fields, and adding these took its constructor
 * past the JVM's 64 KB method limit. Robolectric's instrumenter emits a constructor with
 * one parameter per field, so the test suite failed with
 *
 *     ClassFormatError: Too many arguments in method signature in class file
 *     com/example/ui/localization/UiStringsMore
 *
 * **before a single assertion ran** — which meant no test involving the app's own
 * strings could execute at all, and the failure named a bytecode limit rather than the
 * design problem behind it.
 */
data class NotificationStrings(
    val notifChannelAdhan: String = "Adhan & Prayer Call Alerts",
    val notifChannelAdhanDescription: String =
        "Notifies when prayer time arrives with sound or adhan tone",
    val notifChannelPrePrayer: String = "Pre-Prayer Reminders",
    val notifChannelPrePrayerDescription: String =
        "Gentle heads-up before upcoming prayer",
    val notifChannelSilent: String = "Silent Prayer Notifications",
    val notifChannelSilentDescription: String =
        "Discreet notifications when silent mode or mute is active",
    val notifGlobalSilent: String = "Silent Mode active \u00b7 Adhan muted",
    val notifVibrateAlert: String = "Vibrate alert \u00b7 %1\$s has entered",
    val notifPrayerArrived: String = "Time for %1\$s prayer has arrived (%2\$s)",
    val notifEnterPrayer: String =
        "Enter prayer and turn towards the Holy Kaaba (%1\$s).",
    val notifPrePrayerTitle: String = "%1\$s in %2\$d minutes",
    val notifPrePrayerText: String =
        "%1\$s begins at %2\$s \u00b7 Prepare for prayer",
    val notifAdhanInProgress: String = "Adhan in progress",
    val notifAdhanInProgressBody: String = "Prayer alert is playing",
    // New: standard prayer channel
)

/**
 * The system-access banners and actions.
 *
 * These are deliberately not phrased as permission names alone: each string
 * says what will not happen until the reader fixes it, because the fix lives in
 * a different settings screen and the reason has to survive the trip.
 */
data class SystemAccessStrings(
    val openSettings: String = "Open settings",
    val allowLocation: String = "Allow location",
    val turnOnLocation: String = "Turn on location",
    val tryAgain: String = "Try again",
    val allowExactAlarms: String = "Allow exact alarms",
    val exactAlarmsDelayed: String =
        "Alerts may arrive late while exact alarms are off.",
    val notificationsPermissionNeeded: String =
        "Allow notifications so prayer alerts can appear.",
    val notificationsBlocked: String =
        "Notifications are off. Prayer alerts won't appear.",
    val prayerChannelBlocked: String =
        "Prayer alerts are turned off in system settings.",
    val testNotification: String = "Send a test notification"
)

data class UiStringsMore(
    val actionCancel: String = "Cancel",
    val actionSave: String = "Save",
    val actionClose: String = "Close",
    val actionReset: String = "Reset",
    val search: String = "Search",
    val loading: String = "Loading",
    val sunAltitude: String = "Sun",
    val illumination: String = "Illumination",
    val waxing: String = "Waxing",
    val methodology: String = "Method",
    val madhhabLabelShort: String = "Asr method",
    val appliedAdjustments: String = "Manual adjustments",
    val computedOnDevice: String = "Method, place and adjustments",
    /**
     * Re-derive today's times and re-arm the alarms.
     */
    val reschedulePrayers: String = "Re-arm prayer alarms",
    val minutesShort: String = "min",
    val daysShort: String = "days",
    val prayerLogLabel: String = "Prayer log",
    val prayerLogStoredLocally: String = "Stored on this device",
    val noSearchResults: String = "No matches",

    /**
     * The note that says the list is a page and not everything.
     */

    /**
     * The search face's "there is more than this page" control, one per tier.
     */
    val verseCount: String = "%d verses",
    val textSize: String = "Text size",
    /**
     * What the recitation banner's speed pill does, for a reader listening to
     * it.
     *
     * The pill's visible label is the value itself - "1.5×" - which is honest
     * on screen and meaningless read aloud; this is the announced phrase.
     */

    /**
     * What the recitation banner's repeat pill shows: the count it recites the
     * verse before moving on, as the reader has cycled it.
     */

    /** The loop's pill: the count that is not a count. */
    /**
     * Told to the reader when a verse's recitation could not be streamed.
     *
     * The player used to answer a failed stream with a synthesized chime and
     * carry on with the next verse - the banner named a reciter while a tone
     * played, and the surah advanced. The honest answer to a stream that cannot
     * be prepared is to say what happened.
     */
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
    val magneticInterference: String = "Magnetic interference",
    val magneticInterferenceMessage: String = "Reading may be inaccurate. Move away from metal and electronics.",
    val sectionAppearance: String = "Appearance",
    val sectionLocationAndCalculation: String = "Location & calculation",
    val actionClear: String = "Clear",
    val sectionAlerts: String = "Alerts",
    val sectionAbout: String = "About & diagnostics",
    val timeFormat24hLabel: String = "24-hour time",
    val chooseLanguage: String = "Language",
    val chooseTheme: String = "Theme",
    val themeSystem: String = "Match system",
    val themeDark: String = "Dark",
    val themeLight: String = "Light",
    val accentLabel: String = "Accent",
    val accentSlate: String = "Slate",
    val accentPlum: String = "Plum",
    val accentTeal: String = "Teal",
    val accentClay: String = "Clay",
    val accentWallpaper: String = "Match wallpaper",
    val reduceTransparencyLabel: String = "Reduce transparency",
    val chooseLocation: String = "Location",
    val useGps: String = "Use GPS",
    val customLocation: String = "Enter manually",
    val searchCities: String = "Search cities",
    val allCountriesTitle: String = "All countries",
    val chooseMethod: String = "Calculation method",
    val chooseMadhhab: String = "Asr calculation",
    val chooseAdjustments: String = "Minute adjustments",
    val chooseHijriOffset: String = "Hijri date offset",
    val choosePrePrayerOffset: String = "Reminder before prayer",
    val prePrayerDisabled: String = "Off",
    val silentModeLabel: String = "Silence all alerts",
    val vibrateOnlyLabel: String = "Vibrate only",
    val storageLabel: String = "Storage",
    val sensorAccuracy: String = "Sensor accuracy",
    val ambientField: String = "Ambient field",
    val resetAllLabel: String = "Reset all settings",
    val resetAllConfirmTitle: String = "Reset all settings?",
    val resetAllConfirmMessage: String = "Calculation, alert and display settings return to their defaults. Your saved verses and prayer log are kept.",
    val nameField: String = "Place name",
    val latitudeField: String = "Latitude",
    val longitudeField: String = "Longitude",
    val invalidCoordinates: String = "Enter a latitude between -90 and 90, and a longitude between -180 and 180.",
    val nameRequired: String = "Enter a name for this place.",
    val isFacingQibla: String = "You are facing the Qibla",
    val rightOfQibla: String = "to the right",
    val leftOfQibla: String = "to the left",
    /** %1$s is a formatted angle such as "12°", %2$s a direction phrase. */
    val turnBy: String = "Turn %1\$s %2\$s",
    val alignedWithQibla: String = "Aligned with the Qibla",
    val locateMe: String = "Locate",
    val gpsCached: String = "GPS cached",

    // --- The location result -----------------------------------------------
    //
    // These three sentences used to be assembled in `SalahViewModel.fetchCurrentLocation`
    // - "Acquiring GPS coordinates...", "GPS Location: Rabat, Morocco", "Cached
    // Offline: Rabat, Morocco" - and carried in state as `locationStatusMessage: String`,
    // which is a sentence in one language sitting in a field the UI cannot translate.
    // The state now carries the *facts* and the banner composes these.
    //
    // The failure reasons moved the same way: `AppLocationService` returned a
    // `reason: String` that was one of three fixed English sentences, so a service layer
    // owned English and nothing the UI could do would have reached it.
    val locationAcquiring: String = "Finding your location\u2026",
    val locationResolved: String = "GPS location: %1\$s, %2\$s",
    val locationResolvedCached: String = "Last known location: %1\$s, %2\$s",
    val locationErrorNoPermission: String = "Location permission is not granted.",
    val locationErrorServicesOff:
        String = "Location services are switched off on this device.",
    val locationErrorNoSignal:
        String = "Could not get a GPS signal. Your saved location is still in use.",

    val selectedCity: String = "Selected city",
    /**
     * Shown under the dial while the phone is tilted. The heading is only reliable
     * with the phone held flat, so this is an instruction, not a decoration.
     */
    val holdFlatHint: String = "Hold the phone flat for an accurate compass reading.",
    val northReferenceLabel: String = "North reference",
    /** %1$s is the live heading such as "95° E", %2$s the Qibla bearing "96°". */
    val dialDescription: String = "Compass dial, heading %1\$s. Qibla %2\$s.",
    val privacyNote: String = "Prayer times are calculated on this device, or taken from the Moroccan ministry's published timetable when online. The Qibla direction and the Quran stay on this device. Nothing is uploaded.",
    val privacyOffline: String = "Works without a connection",
    val privacyNoAds: String = "No advertising",
    val privacyNoAnalytics: String = "No analytics or data collection",
    val privacyNoAccount: String = "No account or sign-in",
    val privacyFree: String = "Free and open source",
    val privacyPhilosophyTitle: String = "Privacy & Philosophy",
    val selected: String = "Selected",
    val notSelected: String = "Not selected",
    val alertAdhan: String = "Adhan",
    val alertTakbeer: String = "Takbeer",
    val alertChime: String = "Chime",
    val alertVibrate: String = "Vibrate",
    val alertSilent: String = "Silent",
    val alertSilentReminder: String = "Silent reminder",
    val stateOn: String = "On",
    val stateOff: String = "Off",
    val actionChange: String = "Change",
    val allowNotificationsTitle: String = "Let Salah alert you",
    val allowNotificationsMessage: String =
        "Salah needs permission to show notifications so it can alert you at prayer " +
            "times. Prayer times stay on the Today screen either way, and you can " +
            "turn alerts on later in Settings.",
    val allowNotificationsAction: String = "Allow notifications",
    val notNow: String = "Not now",
    /**
     * The Qibla compass strings (cardinal directions, heading label).
     */
    val reader: ReaderStrings = ReaderStrings(),
    /**
     * The notification texts.
     */
    val notifications: NotificationStrings = NotificationStrings(),
)

val EnglishStrings = UiStrings(
    appName = "SALAH",
    navToday = "Today",
    navQibla = "Qibla",
    navSettings = "Settings",

    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",

    fajr = "Fajr",
    sunrise = "Sunrise",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Transparent Calculation Source",
    todaysTimes = "TODAY'S TIMES",

    kaabaDistance = "Distance to Kaaba",
    trueNorth = "True North",
    magneticNorth = "Magnetic North",


    languageLabel = "Language",
    appThemeLabel = "App Theme",
    locationLabel = "Location",
    methodLabel = "Calculation Method",
    madhhabLabel = "Asr Juristic Madhhab",
    adjustmentsLabel = "Manual Minute Adjustments",
    hijriCalibrationLabel = "Hijri Date Calibration",
    adhanCallLabel = "Adhan Call at Prayer Time",
    prePrayerReminderLabel = "Pre-Prayer Reminder Timing",
    compassDiagnosticsLabel = "Compass Sensors & Diagnostics",
    networkSyncLabel = "Network Synchronization & Source",
    privacyPhilosophyTitle = "Privacy & Philosophy",

    cancel = "Cancel",
    done = "Done",
    close = "Close"
)

val ArabicStrings = UiStrings(

    appName = "صلاة",
    navToday = "اليوم",
    navQibla = "القبلة",
    navSettings = "الإعدادات",

    onlineStatus = "متصل",
    offlineStatus = "بدون اتصال",

    fajr = "الفجر",
    sunrise = "الشروق",
    dhuhr = "الظهر",
    asr = "العصر",
    maghrib = "المغرب",
    isha = "العشاء",

    transparentCalculationSource = "مصدر الحساب الشرعي الشفاف",
    todaysTimes = "أوقات اليوم",

    kaabaDistance = "المسافة إلى الكعبة",
    trueNorth = "الشمال الحقيقي",
    magneticNorth = "الشمال المغناطيسي",


    languageLabel = "اللغة",
    appThemeLabel = "مظهر التطبيق",
    locationLabel = "الموقع الجغرافي",
    methodLabel = "طريقة الحساب",
    madhhabLabel = "المذهب الفقهي (العصر)",
    adjustmentsLabel = "تعديل الدقائق يدوياً",
    hijriCalibrationLabel = "معايرة التاريخ الهجري",
    adhanCallLabel = "نداء الأذان عند دخول الوقت",
    prePrayerReminderLabel = "تذكير قبل الصلاة",
    compassDiagnosticsLabel = "مستشعرات البوصلة والتشخيص",
    networkSyncLabel = "المزامنة والمصدر",
    privacyPhilosophyTitle = "الخصوصية والمنهج",

    cancel = "إلغاء",
    done = "تم",
    close = "إغلاق",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "الشهر الهجري كاملًا", // verify
            nextMonth = "الشهر التالي", // verify
            nextPrayerPrefix = "التالي", // verify
            previousMonth = "الشهر السابق", // verify
            nextDay = "اليوم التالي", // verify
            previousDay = "اليوم السابق", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "فتح الإعدادات",
        allowLocation = "السماح بالموقع",
        turnOnLocation = "تشغيل خدمات الموقع",
        tryAgain = "المحاولة مرة أخرى",
        allowExactAlarms = "السماح بالتنبيهات الدقيقة",
        exactAlarmsDelayed = "قد تتأخر التنبيهات أثناء إيقاف التنبيهات الدقيقة.",
        notificationsPermissionNeeded = "اسمح بالإشعارات لتظهر تنبيهات الصلاة.",
        notificationsBlocked = "الإشعارات متوقفة. لن تظهر تنبيهات الصلاة.",
        prayerChannelBlocked = "تنبيهات الصلاة متوقفة في إعدادات النظام.",
        testNotification = "إرسال إشعار تجريبي",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "العنوان",
        verseCountOne = "آية واحدة",
        verseCountMany = "%d آية",
        rangeTo = " إلى ",
        paperRose = "وردي",
        cardinalNorth = "ش",
        cardinalNorthEast = "ش ق",
        cardinalEast = "ق",
        cardinalSouthEast = "ج ق",
        cardinalSouth = "ج",
        cardinalSouthWest = "ج غ",
        cardinalWest = "غ",
        cardinalNorthWest = "ش غ",
    ),
        actionSave = "حفظ",
        actionCancel = "إلغاء",
        actionClose = "إغلاق",
        actionReset = "إعادة تعيين",
        search = "بحث",
        loading = "جارٍ التحميل",
        textSize = "حجم النص",
        sectionAppearance = "المظهر",
        sectionLocationAndCalculation = "الموقع والحساب",
        actionClear = "مسح", // verify
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
        selected = "محدد",
        notSelected = "غير محدد",
        stateOn = "مُفعّل",
        stateOff = "مُعطّل",
        noSearchResults = "لا توجد نتائج",
        allowNotificationsTitle = "اسمح لصلاة بتنبيهك",
        allowNotificationsMessage = "تحتاج صلاة إلى إذن الإشعارات لتناديك في أوقات الصلاة. تبقى مواقيت الصلاة معروضة في الشاشة الرئيسية على أي حال، ويمكنك تفعيل التنبيهات لاحقًا من الإعدادات.",
        allowNotificationsAction = "السماح بالإشعارات",
        notNow = "ليس الآن",
        actionChange = "تغيير",
        alertSilentReminder = "تذكير صامت", // verify
        ambientField = "المجال المحيط",
        appliedAdjustments = "التعديلات اليدوية", // verify
        calibrationMessage = "حرّك الجهاز على شكل الرقم ثمانية عدة مرات لتهدئة المستشعر.",
        chooseAdjustments = "تعديلات بالدقائق", // verify
        chooseHijriOffset = "إزاحة التاريخ الهجري", // verify
        chooseLanguage = "اللغة",
        chooseMadhhab = "حساب العصر", // verify
        chooseMethod = "طريقة الحساب", // verify
        choosePrePrayerOffset = "تذكير قبل الصلاة", // verify
        chooseTheme = "المظهر",
        computedOnDevice = "الطريقة والموقع والتعديلات", // verify
        reschedulePrayers = "إعادة ضبط تنبيهات الصلاة",
        daysShort = "يوم", // verify
        gpsCached = "تم حفظ موقع GPS",
        illumination = "الإضاءة",
        madhhabLabelShort = "طريقة العصر", // verify
        methodology = "الطريقة", // verify
        minutesShort = "د", // verify
        prePrayerDisabled = "مغلق", // verify
        privacyNote = "تُحسب أوقات الصلاة على هذا الجهاز، أو تُؤخذ من الجدول الرسمي لوزارة الأوقاف المغربية عند الاتصال بالإنترنت. واتجاه القبلة والقرآن يبقيان على هذا الجهاز. لا يُرفع أي شيء.",
        privacyPhilosophyTitle = "الخصوصية والمنهج",
        prayerLogLabel = "سجل الصلاة",
        prayerLogStoredLocally = "محفوظ على هذا الجهاز",
        privacyOffline = "يعمل دون اتصال بالإنترنت",
        privacyNoAds = "بدون إعلانات",
        privacyNoAnalytics = "بدون تحليلات أو جمع بيانات",
        privacyNoAccount = "بدون حساب أو تسجيل دخول",
        privacyFree = "مجاني ومفتوح المصدر",
        resetAllConfirmMessage = "تعود إعدادات الحساب والتنبيه والعرض إلى قيمها الافتراضية. تبقى آياتك المحفوظة وسجل صلاتك.",
        resetAllConfirmTitle = "إعادة ضبط كل الإعدادات؟",
        resetAllLabel = "إعادة ضبط كل الإعدادات",
        selectedCity = "المدينة المحددة",
        sensorAccuracy = "دقة المستشعر",
        silentModeLabel = "كتم كل التنبيهات", // verify
        storageLabel = "التخزين",
        sunAligned = "الشمس شبه متجهة نحو القبلة. استقبلها للتأكد.", // verify
        sunAltitude = "الشمس",
        sunAltitudeValue = "ارتفاع الشمس",
        sunAzimuth = "اتجاه الشمس",
        sunBelowHorizon = "الشمس تحت الأفق، لذا لا يمكن استخدامها كمرجع الآن.",
        sunToTheLeft = "القبلة نحو %d° يسار الشمس.", // verify
        sunToTheRight = "القبلة نحو %d° يمين الشمس.", // verify
        themeDark = "داكن",
        themeLight = "فاتح",
        themeSystem = "مطابقة النظام",
        accentLabel = "لون التمييز",
        accentSlate = "أردوازي",
        accentPlum = "برقوقي",
        accentTeal = "أزرق مخضر",
        accentClay = "طيني",
        accentWallpaper = "مطابقة الخلفية",
        reduceTransparencyLabel = "تقليل الشفافية",
        timeFormat24hLabel = "الوقت بنظام 24 ساعة", // verify
        verseCount = "%d آية", // verify
        vibrateOnlyLabel = "اهتزاز فقط", // verify
        waxing = "متزايد", // verify
        locationAcquiring = "جارٍ تحديد موقعك…",
        locationResolved = "موقع GPS: %1\$s، %2\$s",
        locationResolvedCached = "آخر موقع معروف: %1\$s، %2\$s",
        locationErrorNoPermission = "لم يُمنح إذن الوصول إلى الموقع.",
        locationErrorServicesOff = "خدمات الموقع معطّلة على هذا الجهاز.",
        locationErrorNoSignal = "تعذّر الحصول على إشارة GPS. لا يزال موقعك المحفوظ قيد الاستخدام.",
        notifications = NotificationStrings(
            notifChannelAdhan = "تنبيهات الأذان ونداء الصلاة",
            notifChannelAdhanDescription = "يخطرك عند دخول وقت الصلاة بصوت أو نغمة الأذان",
            notifChannelPrePrayer = "تذكيرات ما قبل الصلاة",
            notifChannelPrePrayerDescription = "تنبيه لطيف قبل دخول وقت الصلاة",
            notifChannelSilent = "إشعارات الصلاة الصامتة",
            notifChannelSilentDescription = "إشعارات هادئة عند تفعيل الوضع الصامت",
            notifGlobalSilent = "الوضع الصامت مُفعّل · تم كتم الأذان",
            notifVibrateAlert = "تنبيه بالاهتزاز · دخل وقت %1\$s",
            notifPrayerArrived = "حان وقت صلاة %1\$s (%2\$s)",
            notifEnterPrayer = "ادخل الصلاة واستقبل الكعبة المشرفة (%1\$s).",
            notifPrePrayerTitle = "%1\$s بعد %2\$d دقيقة",
            notifPrePrayerText = "يبدأ %1\$s في %2\$s · استعد للصلاة",
            notifAdhanInProgress = "جارٍ الأذان",
            notifAdhanInProgressBody = "يتم تشغيل تنبيه الصلاة",
        ),    ),
)

val FrenchStrings = UiStrings(

    appName = "SALAH",
    navToday = "Aujourd'hui",
    navQibla = "Qibla",
    navSettings = "Paramètres",

    onlineStatus = "EN LIGNE",
    offlineStatus = "HORS LIGNE",

    fajr = "Fajr",
    sunrise = "Lever du soleil",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Source de calcul transparente",
    todaysTimes = "HORAIRES DU JOUR",

    kaabaDistance = "Distance à la Kaaba",
    trueNorth = "Nord géographique",
    magneticNorth = "Nord magnétique",


    languageLabel = "Langue",
    appThemeLabel = "Thème de l'application",
    locationLabel = "Emplacement",
    methodLabel = "Méthode de calcul",
    madhhabLabel = "École juridique (Asr)",
    adjustmentsLabel = "Ajustements manuels (minutes)",
    hijriCalibrationLabel = "Calibrage calendrier hégirien",
    adhanCallLabel = "Appel à la prière à l'heure",
    prePrayerReminderLabel = "Rappel avant la prière",
    compassDiagnosticsLabel = "Capteurs boussole & diagnostic",
    networkSyncLabel = "Synchronisation réseau & source",
    privacyPhilosophyTitle = "Confidentialité & Philosophie",

    cancel = "Annuler",
    done = "Terminé",
    close = "Fermer",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Mois hégirien complet", // verify
            nextMonth = "Mois suivant", // verify
            nextPrayerPrefix = "Suivante", // verify
            previousMonth = "Mois précédent", // verify
            nextDay = "Jour suivant", // verify
            previousDay = "Jour précédent", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Ouvrir les paramètres",
        allowLocation = "Autoriser la localisation",
        turnOnLocation = "Activer la localisation",
        tryAgain = "Réessayer",
        allowExactAlarms = "Autoriser les alarmes exactes",
        exactAlarmsDelayed = "Les alertes peuvent être retardées lorsque les alarmes exactes sont désactivées.",
        notificationsPermissionNeeded = "Autorisez les notifications pour que les alertes de prière apparaissent.",
        notificationsBlocked = "Les notifications sont désactivées. Les alertes de prière n’apparaîtront pas.",
        prayerChannelBlocked = "Les alertes de prière sont désactivées dans les paramètres système.",
        testNotification = "Envoyer une notification de test",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Titre",
        verseCountOne = "%d verset",
        verseCountMany = "%d versets",
        rangeTo = " à ",
        paperRose = "Rose",
        cardinalNorth = "N",
        cardinalNorthEast = "NE",
        cardinalEast = "E",
        cardinalSouthEast = "SE",
        cardinalSouth = "S",
        cardinalSouthWest = "SO",
        cardinalWest = "O",
        cardinalNorthWest = "NO",
    ),
        actionSave = "Enregistrer",
        actionCancel = "Annuler",
        actionClose = "Fermer",
        actionReset = "Réinitialiser",
        search = "Rechercher",
        loading = "Chargement",
        textSize = "Taille du texte",
        sectionAppearance = "Apparence",
        sectionLocationAndCalculation = "Localisation et calcul",
        actionClear = "Effacer", // verify
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
        selected = "Sélectionné",
        notSelected = "Non sélectionné",
        stateOn = "Activé",
        stateOff = "Désactivé",
        noSearchResults = "Aucun résultat",
        allowNotificationsTitle = "Autoriser les notifications de Salah",
        allowNotificationsMessage = "Salah a besoin de l’autorisation de notification pour vous appeler aux heures de prière. Les horaires restent affichés sur l’écran d’accueil, et vous pouvez activer les alertes plus tard dans les paramètres.",
        allowNotificationsAction = "Autoriser les notifications",
        notNow = "Pas maintenant",
        actionChange = "Modifier",
        alertSilentReminder = "Rappel silencieux", // verify
        ambientField = "Champ ambiant",
        appliedAdjustments = "Ajustements manuels", // verify
        calibrationMessage = "Déplacez l'appareil en forme de huit plusieurs fois pour stabiliser le capteur.",
        chooseAdjustments = "Ajustements en minutes", // verify
        chooseHijriOffset = "Décalage de la date hégirienne", // verify
        chooseLanguage = "Langue",
        chooseMadhhab = "Calcul de Asr", // verify
        chooseMethod = "Méthode de calcul", // verify
        choosePrePrayerOffset = "Rappel avant la prière", // verify
        chooseTheme = "Thème",
        computedOnDevice = "Méthode, lieu et ajustements", // verify
        reschedulePrayers = "Réarmer les alarmes de prière",
        daysShort = "j", // verify
        gpsCached = "GPS mis en cache",
        illumination = "Illumination",
        madhhabLabelShort = "Méthode de Asr", // verify
        methodology = "Méthode", // verify
        minutesShort = "min", // verify
        prePrayerDisabled = "Désactivé", // verify
        privacyNote = "Les horaires de prière sont calculés sur cet appareil, ou repris du tableau officiel du ministère marocain des Habous lorsque l'appareil est en ligne. La direction de la qibla et le Coran restent sur cet appareil. Rien n'est envoyé.",
        privacyPhilosophyTitle = "Confidentialité & Philosophie",
        prayerLogLabel = "Journal de prière",
        prayerLogStoredLocally = "Enregistré sur cet appareil",
        privacyOffline = "Fonctionne sans connexion",
        privacyNoAds = "Pas de publicité",
        privacyNoAnalytics = "Pas d'analyse ni de collecte de données",
        privacyNoAccount = "Pas de compte ni de connexion",
        privacyFree = "Gratuit et open source",
        resetAllConfirmMessage = "Les paramètres de calcul, d'alerte et d'affichage reviennent à leurs valeurs par défaut. Vos versets enregistrés et votre journal de prière sont conservés.",
        resetAllConfirmTitle = "Réinitialiser tous les paramètres ?",
        resetAllLabel = "Réinitialiser tous les paramètres",
        selectedCity = "Ville sélectionnée",
        sensorAccuracy = "Précision du capteur",
        silentModeLabel = "Silence pour toutes les alertes", // verify
        storageLabel = "Stockage",
        sunAligned = "Le soleil est presque dans la direction de la qibla. Tournez-vous vers lui pour confirmer.", // verify
        sunAltitude = "Soleil",
        sunAltitudeValue = "Hauteur du soleil",
        sunAzimuth = "Direction du soleil",
        sunBelowHorizon = "Le soleil est sous l'horizon, il ne peut donc pas servir de référence pour le moment.",
        sunToTheLeft = "La qibla est à environ %d° à gauche du soleil.", // verify
        sunToTheRight = "La qibla est à environ %d° à droite du soleil.", // verify
        themeDark = "Sombre",
        themeLight = "Clair",
        themeSystem = "Suivre le système",
        accentLabel = "Couleur d'accent",
        accentSlate = "Ardoise",
        accentPlum = "Prune",
        accentTeal = "Bleu canard",
        accentClay = "Argile",
        accentWallpaper = "Suivre le fond d'écran",
        reduceTransparencyLabel = "Réduire la transparence",
        timeFormat24hLabel = "Heure sur 24 heures", // verify
        verseCount = "%d versets", // verify
        vibrateOnlyLabel = "Vibreur uniquement", // verify
        waxing = "Croissant", // verify
        locationAcquiring = "Localisation en cours…",
        locationResolved = "Position GPS : %1\$s, %2\$s",
        locationResolvedCached = "Dernière position connue : %1\$s, %2\$s",
        locationErrorNoPermission = "L’autorisation de localisation n’est pas accordée.",
        locationErrorServicesOff = "Les services de localisation sont désactivés sur cet appareil.",
        locationErrorNoSignal = "Impossible d’obtenir un signal GPS. Votre position enregistrée reste utilisée.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Alertes d'adhan et d'appel à la prière",
            notifChannelAdhanDescription = "Vous avertit à l'heure de la prière, avec le son ou la tonalité de l'adhan",
            notifChannelPrePrayer = "Rappels avant la prière",
            notifChannelPrePrayerDescription = "Une alerte discrète avant la prière à venir",
            notifChannelSilent = "Notifications de prière silencieuses",
            notifChannelSilentDescription = "Notifications discrètes lorsque le mode silencieux est actif",
            notifGlobalSilent = "Mode silencieux actif · Adhan coupé",
            notifVibrateAlert = "Alerte vibreur · %1\$s est entré",
            notifPrayerArrived = "L'heure de la prière %1\$s est arrivée (%2\$s)",
            notifEnterPrayer = "Entrez en prière et tournez-vous vers la Kaaba (%1\$s).",
            notifPrePrayerTitle = "%1\$s dans %2\$d minutes",
            notifPrePrayerText = "%1\$s commence à %2\$s · Préparez-vous à prier",
            notifAdhanInProgress = "Adhan en cours",
            notifAdhanInProgressBody = "L'alerte de prière est en cours de lecture",
        ),    ),
)

val IndonesianStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hari Ini",
    navQibla = "Kiblat",
    navSettings = "Pengaturan",

    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",

    fajr = "Subuh",
    sunrise = "Terbit",
    dhuhr = "Dzuhur",
    asr = "Ashar",
    maghrib = "Maghrib",
    isha = "Isya",

    transparentCalculationSource = "Sumber Perhitungan Transparan",
    todaysTimes = "JADWAL SHOLAT HARI INI",

    kaabaDistance = "Jarak ke Ka'bah",
    trueNorth = "Utara Sejati",
    magneticNorth = "Utara Magnetik",


    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    locationLabel = "Lokasi",
    methodLabel = "Metode Perhitungan",
    madhhabLabel = "Mazhab Fikih (Ashar)",
    adjustmentsLabel = "Penyesuaian Menit Manual",
    hijriCalibrationLabel = "Kalibrasi Penanggalan Hijriah",
    adhanCallLabel = "Kumandangkan Adzan Tepat Waktu",
    prePrayerReminderLabel = "Pengingat Sebelum Sholat",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Sinkronisasi Jaringan & Sumber",
    privacyPhilosophyTitle = "Privasi & Filosofi",

    cancel = "Batal",
    done = "Selesai",
    close = "Tutup",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Bulan Hijriah penuh", // verify
            nextMonth = "Bulan berikutnya", // verify
            nextPrayerPrefix = "Berikutnya", // verify
            previousMonth = "Bulan sebelumnya", // verify
            nextDay = "Hari berikutnya", // verify
            previousDay = "Hari sebelumnya", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Buka setelan",
        allowLocation = "Izinkan lokasi",
        turnOnLocation = "Aktifkan lokasi",
        tryAgain = "Coba lagi",
        allowExactAlarms = "Izinkan alarm tepat",
        exactAlarmsDelayed = "Peringatan mungkin terlambat saat alarm tepat dimatikan.",
        notificationsPermissionNeeded = "Izinkan notifikasi agar pengingat salat muncul.",
        notificationsBlocked = "Notifikasi dimatikan. Pengingat salat tidak akan muncul.",
        prayerChannelBlocked = "Pengingat salat dimatikan di setelan sistem.",
        testNotification = "Kirim notifikasi uji",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Judul",
        verseCountOne = "%d ayat",
        verseCountMany = "%d ayat",
        rangeTo = " sampai ",
        paperRose = "Mawar",
        cardinalNorth = "U",
        cardinalNorthEast = "TL",
        cardinalEast = "T",
        cardinalSouthEast = "TG",
        cardinalSouth = "S",
        cardinalSouthWest = "BD",
        cardinalWest = "B",
        cardinalNorthWest = "BL",
    ),
        actionSave = "Simpan",
        actionCancel = "Batal",
        actionClose = "Tutup",
        actionReset = "Atur ulang",
        search = "Cari",
        loading = "Memuat",
        textSize = "Ukuran teks",
        sectionAppearance = "Tampilan",
        sectionLocationAndCalculation = "Lokasi dan perhitungan",
        actionClear = "Hapus", // verify
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
        selected = "Dipilih",
        notSelected = "Belum dipilih",
        stateOn = "Aktif",
        stateOff = "Nonaktif",
        noSearchResults = "Tidak ada hasil",
        allowNotificationsTitle = "Izinkan notifikasi Salah",
        allowNotificationsMessage = "Salah memerlukan izin notifikasi untukanggil Anda pada waktu salat. Jadwal salat tetap terlihat di layar utama, dan Anda dapat menyalakan pengingat nanti di Pengaturan.",
        allowNotificationsAction = "Izinkan notifikasi",
        notNow = "Nanti saja",
        actionChange = "Ubah",
        alertSilentReminder = "Pengingat senyap", // verify
        ambientField = "Medan ambient",
        appliedAdjustments = "Penyesuaian manual", // verify
        calibrationMessage = "Gerakkan perangkat menyerupai angka delapan beberapa kali agar sensor stabil.",
        chooseAdjustments = "Penyesuaian menit", // verify
        chooseHijriOffset = "Offset tanggal Hijriah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Perhitungan Asr", // verify
        chooseMethod = "Metode perhitungan", // verify
        choosePrePrayerOffset = "Pengingat sebelum shalat", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Metode, lokasi, dan penyesuaian", // verify
        reschedulePrayers = "Aktifkan kembali alarm salat",
        daysShort = "hr", // verify
        gpsCached = "GPS di-cache",
        illumination = "Iluminasi",
        madhhabLabelShort = "Metode Asr", // verify
        methodology = "Metode", // verify
        minutesShort = "mnt", // verify
        prePrayerDisabled = "Nonaktif", // verify
        privacyNote = "Waktu shalat dihitung di perangkat ini, atau diambil dari jadwal resmi Kementerian Habous Maroko saat daring. Arah kiblat dan Al-Qur'an tetap di perangkat ini. Tidak ada yang diunggah.",
        privacyPhilosophyTitle = "Privasi & Filosofi",
        prayerLogLabel = "Log salat",
        prayerLogStoredLocally = "Disimpan di perangkat ini",
        privacyOffline = "Berfungsi tanpa koneksi",
        privacyNoAds = "Tanpa iklan",
        privacyNoAnalytics = "Tanpa analitik atau pengumpulan data",
        privacyNoAccount = "Tanpa akun atau masuk",
        privacyFree = "Gratis dan sumber terbuka",
        resetAllConfirmMessage = "Pengaturan perhitungan, pengingat, dan tampilan kembali ke bawaan. Ayat tersimpan dan catatan shalat Anda tetap disimpan.",
        resetAllConfirmTitle = "Atur ulang semua pengaturan?",
        resetAllLabel = "Atur ulang semua pengaturan",
        selectedCity = "Kota terpilih",
        sensorAccuracy = "Akurasi sensor",
        silentModeLabel = "Senyapkan semua pengingat", // verify
        storageLabel = "Penyimpanan",
        sunAligned = "Matahari hampir searah kiblat. Hadapi untuk memastikan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horison, sehingga tidak bisa dipakai sebagai acuan sekarang.",
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        themeDark = "Gelap",
        themeLight = "Terang",
        themeSystem = "Ikuti sistem",
        accentLabel = "Warna aksen",
        accentSlate = "Batu tulis",
        accentPlum = "Prem",
        accentTeal = "Biru kehijauan",
        accentClay = "Tanah liat",
        accentWallpaper = "Ikuti wallpaper",
        reduceTransparencyLabel = "Kurangi transparansi",
        timeFormat24hLabel = "Format 24 jam", // verify
        verseCount = "%d ayat", // verify
        vibrateOnlyLabel = "Hanya getar", // verify
        waxing = "Bertambah", // verify
        locationAcquiring = "Mencari lokasi Anda…",
        locationResolved = "Lokasi GPS: %1\$s, %2\$s",
        locationResolvedCached = "Lokasi terakhir diketahui: %1\$s, %2\$s",
        locationErrorNoPermission = "Izin lokasi belum diberikan.",
        locationErrorServicesOff = "Layanan lokasi dimatikan di perangkat ini.",
        locationErrorNoSignal = "Sinyal GPS tidak diperoleh. Lokasi tersimpan Anda tetap digunakan.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Peringgatan Azan dan Panggilan Salat",
            notifChannelAdhanDescription = "Memberi tahu saat waktu salat tiba, dengan suara atau nada adzan",
            notifChannelPrePrayer = "Pengingat Salat",
            notifChannelPrePrayerDescription = "Pengingat singkat sebelum waktu salat berikutnya",
            notifChannelSilent = "Notifikasi Salat Senyap",
            notifChannelSilentDescription = "Notifikasi halus saat mode senyap aktif",
            notifGlobalSilent = "Mode Senyap aktif · Adzan dibisukan",
            notifVibrateAlert = "Peringatan getar · %1\$s telah masuk",
            notifPrayerArrived = "Waktu salat %1\$s telah tiba (%2\$s)",
            notifEnterPrayer = "Masuk salat dan menghadap Kaaba (%1\$s).",
            notifPrePrayerTitle = "%1\$s dalam %2\$d menit",
            notifPrePrayerText = "%1\$s dimulai pukul %2\$s · Bersiap salat",
            notifAdhanInProgress = "Adzan sedang berlangsung",
            notifAdhanInProgressBody = "Peringatan salat sedang diputar",
        ),    ),
)

val TurkishStrings = UiStrings(

    appName = "SALAH",
    navToday = "Bugün",
    navQibla = "Kıble",
    navSettings = "Ayarlar",

    onlineStatus = "ÇEVRİMİÇİ",
    offlineStatus = "ÇEVRİMDIŞI",

    fajr = "İmsak",
    sunrise = "Güneş",
    dhuhr = "Öğle",
    asr = "İkindi",
    maghrib = "Akşam",
    isha = "Yatsı",

    transparentCalculationSource = "Şeffaf Hesaplama Kaynağı",
    todaysTimes = "BUGÜNÜN VAKİTLERİ",

    kaabaDistance = "Kabe'ye Mesafe",
    trueNorth = "Gerçek Kuzey",
    magneticNorth = "Manyetik Kuzey",


    languageLabel = "Dil",
    appThemeLabel = "Uygulama Teması",
    locationLabel = "Konum",
    methodLabel = "Hesaplama Yöntemi",
    madhhabLabel = "İkindi Mezhebi",
    adjustmentsLabel = "Manuel Dakika Düzeltmeleri",
    hijriCalibrationLabel = "Hicri Takvim Ayarı",
    adhanCallLabel = "Vaktinde Ezan Oku",
    prePrayerReminderLabel = "Vakit Öncesi Hatırlatıcı",
    compassDiagnosticsLabel = "Pusula Sensörleri & Tanılama",
    networkSyncLabel = "Ağ Eşitlemesi & Kaynak",
    privacyPhilosophyTitle = "Gizlilik & Felsefe",

    cancel = "İptal",
    done = "Tamam",
    close = "Kapat",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Hicri ayın tamamı", // verify
            nextMonth = "Sonraki ay", // verify
            nextPrayerPrefix = "Sonraki", // verify
            previousMonth = "Önceki ay", // verify
            nextDay = "Sonraki gün", // verify
            previousDay = "Önceki gün", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Ayarları aç",
        allowLocation = "Konuma izin ver",
        turnOnLocation = "Konumu aç",
        tryAgain = "Tekrar dene",
        allowExactAlarms = "Kesin alarmlara izin ver",
        exactAlarmsDelayed = "Kesin alarmlar kapalıyken uyarılar gecikebilir.",
        notificationsPermissionNeeded = "Namaz uyarılarının görünmesi için bildirimlere izin verin.",
        notificationsBlocked = "Bildirimler kapalı. Namaz uyarıları görünmez.",
        prayerChannelBlocked = "Namaz uyarıları sistem ayarlarında kapatılmış.",
        testNotification = "Test bildirimi gönder",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Başlık",
        verseCountOne = "%d ayet",
        verseCountMany = "%d ayet",
        rangeTo = " - ",
        paperRose = "Gül",
        cardinalNorth = "K",
        cardinalNorthEast = "KD",
        cardinalEast = "D",
        cardinalSouthEast = "GD",
        cardinalSouth = "G",
        cardinalSouthWest = "GB",
        cardinalWest = "B",
        cardinalNorthWest = "KB",
    ),
        actionSave = "Kaydet",
        actionCancel = "İptal",
        actionClose = "Kapat",
        actionReset = "Sıfırla",
        search = "Ara",
        loading = "Yükleniyor",
        textSize = "Yazı boyutu",
        sectionAppearance = "Görünüm",
        sectionLocationAndCalculation = "Konum ve hesaplama",
        actionClear = "Temizle", // verify
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
        selected = "Seçili",
        notSelected = "Seçili değil",
        stateOn = "Açık",
        stateOff = "Kapalı",
        noSearchResults = "Sonuç yok",
        allowNotificationsTitle = "Salah sizi uyarsın",
        allowNotificationsMessage = "Salah, sizi namaz vakitlerinde uyarmak için bildirim iznine ihtiyaç duyar. Namaz vakitleri her hâlükârda ana ekranda görünür; uyarıları daha sonra Ayarlar’dan açabilirsiniz.",
        allowNotificationsAction = "Bildirimlere izin ver",
        notNow = "Şimdi değil",
        actionChange = "Değiştir",
        alertSilentReminder = "Sessiz hatırlatma", // verify
        ambientField = "Çevresel alan",
        appliedAdjustments = "Elle ayarlamalar", // verify
        calibrationMessage = "Sensörün dengelemesi için cihazı birkaç kez sekiz şeklinde hareket ettirin.",
        chooseAdjustments = "Dakika ayarlamaları", // verify
        chooseHijriOffset = "Hicri tarih farkı", // verify
        chooseLanguage = "Dil",
        chooseMadhhab = "Asr hesabı", // verify
        chooseMethod = "Hesap yöntemi", // verify
        choosePrePrayerOffset = "Namazdan önce hatırlatma", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Yöntem, konum ve düzeltmeler", // verify
        reschedulePrayers = "Namaz alarmlarını yeniden kur",
        daysShort = "g", // verify
        gpsCached = "GPS önbelleğe alındı",
        illumination = "Aydınlanma",
        madhhabLabelShort = "Asr yöntemi", // verify
        methodology = "Yöntem", // verify
        minutesShort = "dk", // verify
        prePrayerDisabled = "Kapalı", // verify
        privacyNote = "Namaz vakitleri bu cihazda hesaplanır veya cihaz çevrimiçiyken Fas Habous Bakanlığı'nın yayımladığı çizelgeden alınır. Kıble yönü ve Kur'an bu cihazda kalır. Hiçbir şey yüklenmez.",
        privacyPhilosophyTitle = "Gizlilik & Felsefe",
        prayerLogLabel = "Namaz günlüğü",
        prayerLogStoredLocally = "Bu cihazda saklandı",
        privacyOffline = "Bağlantı olmadan çalışır",
        privacyNoAds = "Reklam yok",
        privacyNoAnalytics = "Analiz veya veri toplama yok",
        privacyNoAccount = "Hesap veya giriş yok",
        privacyFree = "Ücretsiz ve açık kaynak",
        resetAllConfirmMessage = "Hesap, uyarı ve görünüm ayarları varsayılanlara döner. Kayıtlı ayetleriniz ve namaz kaydınız korunur.",
        resetAllConfirmTitle = "Tüm ayarlar sıfırlansın mı?",
        resetAllLabel = "Tüm ayarları sıfırla",
        selectedCity = "Seçilen şehir",
        sensorAccuracy = "Sensör doğruluğu",
        silentModeLabel = "Tüm uyarıları sessize al", // verify
        storageLabel = "Depolama",
        sunAligned = "Güneş neredeyse kıble yönünde. Doğrulamak için ona dönün.", // verify
        sunAltitude = "Güneş",
        sunAltitudeValue = "Güneş yüksekliği",
        sunAzimuth = "Güneş yönü",
        sunBelowHorizon = "Güneş ufuk altında, bu yüzden şu an referans olarak kullanılamaz.",
        sunToTheLeft = "Kıble güneşin yaklaşık %d° solunda.", // verify
        sunToTheRight = "Kıble güneşin yaklaşık %d° sağında.", // verify
        themeDark = "Koyu",
        themeLight = "Açık",
        themeSystem = "Sisteme uy",
        accentLabel = "Vurgu rengi",
        accentSlate = "Arduvaz",
        accentPlum = "Erik",
        accentTeal = "Deniz mavisi",
        accentClay = "Kil",
        accentWallpaper = "Duvar kağıdını eşleştir",
        reduceTransparencyLabel = "Saydamlığı azalt",
        timeFormat24hLabel = "24 saatlik format", // verify
        verseCount = "%d ayet", // verify
        vibrateOnlyLabel = "Yalnızca titreşim", // verify
        waxing = "Büyüyen", // verify
        locationAcquiring = "Konumunuz bulunuyor…",
        locationResolved = "GPS konumu: %1\$s, %2\$s",
        locationResolvedCached = "Bilinen son konum: %1\$s, %2\$s",
        locationErrorNoPermission = "Konum izni verilmemiş.",
        locationErrorServicesOff = "Bu cihazda konum servisleri kapalı.",
        locationErrorNoSignal = "GPS sinyali alınamadı. Kayıtlı konumunuz kullanılmaya devam ediyor.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Ezan ve Namaz Çağrı Uyarıları",
            notifChannelAdhanDescription = "Namaz vakti geldiğinde ses veya ezan tonuyla haber verir",
            notifChannelPrePrayer = "Namaz Öncesi Hatırlatıcılar",
            notifChannelPrePrayerDescription = "Yaklaşan namaz için kısa bir uyarı",
            notifChannelSilent = "Sessiz Namaz Bildirimleri",
            notifChannelSilentDescription = "Sessiz mod etkinken hafif bildirimler",
            notifGlobalSilent = "Sessiz mod etkin · Ezan kapatıldı",
            notifVibrateAlert = "Titreşim uyarısı · %1\$s girdi",
            notifPrayerArrived = "%1\$s namaz vakti geldi (%2\$s)",
            notifEnterPrayer = "Namaza girip Kâ'be'ye yönelin (%1\$s).",
            notifPrePrayerTitle = "%2\$d dakika sonra %1\$s",
            notifPrePrayerText = "%1\$s saat %2\$s'de başlıyor · Namaza hazırlanın",
            notifAdhanInProgress = "Ezan çalıyor",
            notifAdhanInProgressBody = "Namaz uyarısı çalıyor",
        ),    ),
)

val UrduStrings = UiStrings(

    appName = "صلاۃ",
    navToday = "آج",
    navQibla = "قبلہ",
    navSettings = "ترتیبات",

    onlineStatus = "آن لائن",
    offlineStatus = "آف لائن",

    fajr = "فجر",
    sunrise = "طلوع آفتاب",
    dhuhr = "ظہر",
    asr = "عصر",
    maghrib = "مغرب",
    isha = "عشاء",

    transparentCalculationSource = "شفاف حساب کا شرعی ماخذ",
    todaysTimes = "آج کے اوقات",

    kaabaDistance = "کعبہ شریف کا فاصلہ",
    trueNorth = "حقیقی شمال",
    magneticNorth = "مقناطیسی شمال",


    languageLabel = "زبان",
    appThemeLabel = "ایپ تھیم",
    locationLabel = "مقام",
    methodLabel = "طریقہ حساب",
    madhhabLabel = "فقہی مسلک (عصر)",
    adjustmentsLabel = "دستی منٹ ایڈجسٹمنٹ",
    hijriCalibrationLabel = "ہجری تاریخ کی ترتیب",
    adhanCallLabel = "وقت پر اذان کی پکار",
    prePrayerReminderLabel = "نماز سے قبل یاد دہانی",
    compassDiagnosticsLabel = "قطب نما اور سینسر کی جانچ",
    networkSyncLabel = "نیٹ ورک سنک اور ماخذ",
    privacyPhilosophyTitle = "رازداری اور اصول",

    cancel = "منسوخ",
    done = "مکمل",
    close = "بند کریں",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "ہجری مہینہ مکمل", // verify
            nextMonth = "اگلا مہینہ", // verify
            nextPrayerPrefix = "اگلی", // verify
            previousMonth = "پچھلا مہینہ", // verify
            nextDay = "اگلا دن", // verify
            previousDay = "پچھلا دن", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "ترتیبات کھولیں",
        allowLocation = "مقام کی اجازت دیں",
        turnOnLocation = "مقام کو آن کریں",
        tryAgain = "دوبارہ کوشش کریں",
        allowExactAlarms = "درست الارم کی اجازت دیں",
        exactAlarmsDelayed = "درست الارم بند ہونے پر اطلاعیں تاخیر سے آ سکتی ہیں۔",
        notificationsPermissionNeeded = "نماز کی اطلاعیں ظاہر ہونے کے لیے اطلاعات کی اجازت دیں۔",
        notificationsBlocked = "اطلاعات بند ہیں۔ نماز کی اطلاعیں ظاہر نہیں ہوں گی۔",
        prayerChannelBlocked = "نماز کی اطلاعیں سسٹم ترتیبات میں بند ہیں۔",
        testNotification = "آزمائشی اطلاع بھیجیں",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "عنوان",
        verseCountOne = "ایک آیت",
        verseCountMany = "%d آیت",
        rangeTo = " سے ",
        paperRose = "گلابی",
        cardinalNorth = "ش",
        cardinalNorthEast = "ش ق",
        cardinalEast = "ق",
        cardinalSouthEast = "ج ق",
        cardinalSouth = "ج",
        cardinalSouthWest = "ج غ",
        cardinalWest = "غ",
        cardinalNorthWest = "ش غ",
    ),
        actionSave = "محفوظ کریں",
        actionCancel = "منسوخ کریں",
        actionClose = "بند کریں",
        actionReset = "reset کریں",
        search = "تلاش",
        loading = "لوڈ ہو رہا ہے",
        textSize = "متن کا حجم",
        sectionAppearance = "ظاہری شکل",
        sectionLocationAndCalculation = "مقام اور حساب",
        actionClear = "مٹائیں", // verify
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
        selected = "منتخب",
        notSelected = "غیر منتخب",
        stateOn = "فعال",
        stateOff = "غیر فعال",
        noSearchResults = "کوئی نتیجہ نہیں",
        allowNotificationsTitle = "صلاة آپ کو مطلع کرے",
        allowNotificationsMessage = "صلاة کو نماز کے اوقات میں آپ کو مطلع کرنے کے لیے اطلاعات کی اجازت درکار ہے۔ نماز کے اوقات ہوم اسکرین پر کھلے رہتے ہیں، اور آپ بعد میں ترتیبات سے اطلاعات فعال کر سکتے ہیں۔",
        allowNotificationsAction = "اطلاعات کی اجازت دیں",
        notNow = "ابھی نہیں",
        actionChange = "تبدیل کریں",
        alertSilentReminder = "خاموش یاددہانی", // verify
        ambientField = "ماحولی میدان",
        appliedAdjustments = "دستی ترتیبات", // verify
        calibrationMessage = "سینسر کو مستحکم کرنے کے لیے ڈیوائس کو کئی بار ٹ شکل میں گھمائیں۔",
        chooseAdjustments = "منٹ کی ترتیبات", // verify
        chooseHijriOffset = "ہجری تاریخ کا آفسیٹ", // verify
        chooseLanguage = "زبان",
        chooseMadhhab = "عصر کا حساب", // verify
        chooseMethod = "حساب کا طریقہ", // verify
        choosePrePrayerOffset = "نماز سے پہلے یاددہانی", // verify
        chooseTheme = "تھیم",
        computedOnDevice = "طریقہ، مقام اور ترامیم", // verify
        reschedulePrayers = "نماز کے الرٹ ایڈام دوبارہ فعال کریں",
        daysShort = "دن", // verify
        gpsCached = "جی پی ایس کیش ہو گئی",
        illumination = "روشنی",
        madhhabLabelShort = "عصر کا طریقہ", // verify
        methodology = "طریقہ", // verify
        minutesShort = "منٹ", // verify
        prePrayerDisabled = "بند", // verify
        privacyNote = "نماز کے اوقات اسی ڈیوائس پر حساب ہوتے ہیں، یا آن لائن ہونے پر مراکش کی وزارتِ اوقاف کے شائع کردہ جدول سے لیے جاتے ہیں۔ قبلہ کی سمت اور قرآن اسی ڈیوائس پر رہتے ہیں۔ کچھ اپلوڈ نہیں ہوتا۔",
        privacyPhilosophyTitle = "رازداری اور اصول",
        prayerLogLabel = "نماز لاگ",
        prayerLogStoredLocally = "اس ڈیوائس پر محفوظ",
        privacyOffline = "انٹرنیٹ کے بغیر کام کرتا ہے",
        privacyNoAds = "کوئی اشتہار نہیں",
        privacyNoAnalytics = "کوئی تجزیہ یا ڈیٹا جمع کرنے نہیں",
        privacyNoAccount = "کوئی اکاؤنٹ یا سائن ان نہیں",
        privacyFree = "مفت اور اوپن سورس",
        resetAllConfirmMessage = "حساب، الرٹ اور ڈسپلے کی ترتیبات ڈیفالٹ پر آ جاتی ہیں۔ آپ کی محفوظ آیات اور نماز کا لاگ محفوظ رہتا ہے۔",
        resetAllConfirmTitle = "تمام ترتیبات دوبارہ ترتیب دیں؟",
        resetAllLabel = "تمام ترتیبات دوبارہ ترتیب دیں",
        selectedCity = "منتخب شہر",
        sensorAccuracy = "سینسر کی درستگی",
        silentModeLabel = "تمام الرٹس خاموش کریں", // verify
        storageLabel = "اسٹوریج",
        sunAligned = "سورج قبلہ کی سمت میں تقریباً ہے۔ تصدیق کے لیے اس کی طرف منہ کریں۔", // verify
        sunAltitude = "سورج",
        sunAltitudeValue = "سورج کی اونچائی",
        sunAzimuth = "سورج کی سمت",
        sunBelowHorizon = "سورج افق سے نیچے ہے، اس لیے اسے ابھی بطور حوالہ استعمال نہیں کیا جا سکتا۔",
        sunToTheLeft = "قبلہ سورج کے بائیں جانب تقریباً %d° ہے۔", // verify
        sunToTheRight = "قبلہ سورج کے دائیں جانب تقریباً %d° ہے۔", // verify
        themeDark = "ڈارک",
        themeLight = "لائٹ",
        themeSystem = "سسٹم کے مطابق",
        accentLabel = "نمایاں رنگ",
        accentSlate = "سلیٹ",
        accentPlum = "آلوچہ",
        accentTeal = "فیروزی",
        accentClay = "مٹیالا",
        accentWallpaper = "وال پیپر کے مطابق",
        reduceTransparencyLabel = "شفافیت کم کریں",
        timeFormat24hLabel = "24 گھنٹے کا فارمیٹ", // verify
        verseCount = "%d آیات", // verify
        vibrateOnlyLabel = "صرف ہلاؤ", // verify
        waxing = "بڑھتا ہوا", // verify
        locationAcquiring = "مقام جا رہا ہے…",
        locationResolved = "GPS مقام: %1\$s، %2\$s",
        locationResolvedCached = "آخری معلوم مقام: %1\$s، %2\$s",
        locationErrorNoPermission = "مقام کی اجازت نہیں دی گئی۔",
        locationErrorServicesOff = "اس آلے پر مقام کی سہولتیں بند ہیں۔",
        locationErrorNoSignal = "GPS سگنل نہیں مل سکا۔ آپ کا محفوظ مقام اب بھی استعمال ہو رہا ہے۔",
        notifications = NotificationStrings(
            notifChannelAdhan = "اذان اور نماز کی اطلاعیہات",
            notifChannelAdhanDescription = "نماز کے وقت آنے پر آواز یا اذان کے سُر میں خبر دیتا ہے",
            notifChannelPrePrayer = "نماز سے پہلے یاد دہانیاں",
            notifChannelPrePrayerDescription = "آنے والی نماز سے پہلے ہلکی اطلاع",
            notifChannelSilent = "خاموش نماز اطلاعیہات",
            notifChannelSilentDescription = "خاموش حالت فعال ہونے پر ہلکی اطلاعیہات",
            notifGlobalSilent = "خاموش حالت فعال · اذان خاموش",
            notifVibrateAlert = "کانپن اطلاع · %1\$s داخل ہو گیا",
            notifPrayerArrived = "%1\$s کے نماز کا وقت آ گیا (%2\$s)",
            notifEnterPrayer = "نماز میں جائیں اور کعبہ کی طرف منہ کریں (%1\$s)۔",
            notifPrePrayerTitle = "%2\$d منٹ میں %1\$s",
            notifPrePrayerText = "%1\$s کی نماز %2\$s پر شروع ہوگی · نماز کی تیاری کریں",
            notifAdhanInProgress = "اذان جاری ہے",
            notifAdhanInProgressBody = "نماز کی اطلاع چل رہی ہے",
        ),    ),
)

val MalayStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hari Ini",
    navQibla = "Kiblat",
    navSettings = "Tetapan",

    onlineStatus = "DALAM TALIAN",
    offlineStatus = "LUAR TALIAN",

    fajr = "Subuh",
    sunrise = "Syuruk",
    dhuhr = "Zohor",
    asr = "Asar",
    maghrib = "Maghrib",
    isha = "Isyak",

    transparentCalculationSource = "Sumber Pengiraan Telus",
    todaysTimes = "WAKTU HARI INI",

    kaabaDistance = "Jarak ke Kaabah",
    trueNorth = "Utara Benar",
    magneticNorth = "Utara Magnetik",


    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    locationLabel = "Lokasi",
    methodLabel = "Kaedah Pengiraan",
    madhhabLabel = "Mazhab Fiqh (Asar)",
    adjustmentsLabel = "Pelarasan Minit Manual",
    hijriCalibrationLabel = "Kalibrasi Tarikh Hijrah",
    adhanCallLabel = "Panggilan Azan Tepat Waktu",
    prePrayerReminderLabel = "Peringatan Sebelum Solat",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Penyelarasan Rangkaian & Sumber",
    privacyPhilosophyTitle = "Privasi & Falsafah",

    cancel = "Batal",
    done = "Selesai",
    close = "Tutup",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Bulan Hijrah penuh", // verify
            nextMonth = "Bulan berikutnya", // verify
            nextPrayerPrefix = "Seterusnya", // verify
            previousMonth = "Bulan sebelumnya", // verify
            nextDay = "Hari seterusnya", // verify
            previousDay = "Hari sebelumnya", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Buka tetapan",
        allowLocation = "Benarkan lokasi",
        turnOnLocation = "Hidupkan lokasi",
        tryAgain = "Cuba lagi",
        allowExactAlarms = "Benarkan penggera tepat",
        exactAlarmsDelayed = "Amaran mungkin lewat apabila penggera tepat dimatikan.",
        notificationsPermissionNeeded = "Benarkan notifikasi supaya amaran solat muncul.",
        notificationsBlocked = "Notifikasi dimatikan. Amaran solat tidak akan muncul.",
        prayerChannelBlocked = "Amaran solat dimatikan dalam tetapan sistem.",
        testNotification = "Hantar notifikasi ujian",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Tajuk",
        verseCountOne = "%d ayat",
        verseCountMany = "%d ayat",
        rangeTo = " sampai ",
        paperRose = "Merah",
        cardinalNorth = "U",
        cardinalNorthEast = "TL",
        cardinalEast = "T",
        cardinalSouthEast = "TG",
        cardinalSouth = "S",
        cardinalSouthWest = "BD",
        cardinalWest = "B",
        cardinalNorthWest = "BL",
    ),
        actionSave = "Simpan",
        actionCancel = "Batal",
        actionClose = "Tutup",
        actionReset = "Set semula",
        search = "Cari",
        loading = "Memuatkan",
        textSize = "Saiz teks",
        sectionAppearance = "Paparan",
        sectionLocationAndCalculation = "Lokasi dan pengiraan",
        actionClear = "Kosongkan", // verify
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
        selected = "Dipilih",
        notSelected = "Belum dipilih",
        stateOn = "Aktif",
        stateOff = "Tidak aktif",
        noSearchResults = "Tiada hasil",
        allowNotificationsTitle = "Benarkan notifikasi Salah",
        allowNotificationsMessage = "Salah memerlukan kebenaran notifikasi untuk memanggil anda pada waktu solat. Waktu solat kekal dipaparkan pada skrin utama, dan anda boleh mengaktifkan amaran kemudian dalam Tetapan.",
        allowNotificationsAction = "Benarkan notifikasi",
        notNow = "Bukan sekarang",
        actionChange = "Tukar",
        alertSilentReminder = "Peringatan senyap", // verify
        ambientField = "Medan sekitar",
        appliedAdjustments = "Pelarasan manual", // verify
        calibrationMessage = "Gerakkan peranti membentuk angka lapan beberapa kali agar sensor stabil.",
        chooseAdjustments = "Pelarasan minit", // verify
        chooseHijriOffset = "Ofset tarih Hijrah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Pengiraan Asr", // verify
        chooseMethod = "Kaedah pengiraan", // verify
        choosePrePrayerOffset = "Peringatan sebelum solat", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Kaedah, lokasi dan pelarasan", // verify
        reschedulePrayers = "Hidupkan semula alarm solat",
        daysShort = "hari", // verify
        gpsCached = "GPS dicache",
        illumination = "Pencahayaan",
        madhhabLabelShort = "Kaedah Asr", // verify
        methodology = "Kaedah", // verify
        minutesShort = "min", // verify
        prePrayerDisabled = "Dimatikan", // verify
        privacyNote = "Waktu solat dikira pada peranti ini, atau diambil daripada jadual rasmi Kementerian Habous Maghribi apabila dalam talian. Arah kiblat dan Al-Quran kekal pada peranti ini. Tiada apa yang dimuat naik.",
        privacyPhilosophyTitle = "Privasi & Falsafah",
        prayerLogLabel = "Log solat",
        prayerLogStoredLocally = "Disimpan pada peranti ini",
        privacyOffline = "Berfungsi tanpa sambungan",
        privacyNoAds = "Tiada iklan",
        privacyNoAnalytics = "Tiada analitik atau pengumpulan data",
        privacyNoAccount = "Tiada akaun atau log masuk",
        privacyFree = "Percuma dan sumber terbuka",
        resetAllConfirmMessage = "Tetapan pengiraan, peringatan, dan paparan kembali ke lalai. Ayat dan catatan solat anda disimpan.",
        resetAllConfirmTitle = "Set semula semua tetapan?",
        resetAllLabel = "Set semula semua tetapan",
        selectedCity = "Bandar dipilih",
        sensorAccuracy = "Ketepatan sensor",
        silentModeLabel = "Senyapkan semua peringatan", // verify
        storageLabel = "Storan",
        sunAligned = "Matahari hampir mengikut arah kiblat. Hadapkan diri untuk sahkan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horizon, jadi ia tidak boleh dijadikan rujukan sekarang.",
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        themeDark = "Gelap",
        themeLight = "Cerah",
        themeSystem = "Ikut sistem",
        accentLabel = "Warna aksen",
        accentSlate = "Batu tulis",
        accentPlum = "Plum",
        accentTeal = "Biru kehijauan",
        accentClay = "Tanah liat",
        accentWallpaper = "Ikut kertas dinding",
        reduceTransparencyLabel = "Kurangkan ketelusan",
        timeFormat24hLabel = "Format 24 jam", // verify
        verseCount = "%d ayat", // verify
        vibrateOnlyLabel = "Getaran sahaja", // verify
        waxing = "Bertambah", // verify
        locationAcquiring = "Mencari lokasi anda…",
        locationResolved = "Lokasi GPS: %1\$s, %2\$s",
        locationResolvedCached = "Lokasi terakhir yang diketahui: %1\$s, %2\$s",
        locationErrorNoPermission = "Kebenaran lokasi tidak diberikan.",
        locationErrorServicesOff = "Perkhidmatan lokasi dimatikan pada peranti ini.",
        locationErrorNoSignal = "Isyarat GPS tidak diperoleh. Lokasi tersimpan anda masih digunakan.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Amaran Azan dan Panggilan Solat",
            notifChannelAdhanDescription = "Memberitahu apabila waktu solat tiba, dengan bunyi atau nada azan",
            notifChannelPrePrayer = "Peringatan Pra-Solat",
            notifChannelPrePrayerDescription = "Amaran ringkas sebelum waktu solat akan tiba",
            notifChannelSilent = "Notifikasi Solat Senyap",
            notifChannelSilentDescription = "Notifikasi halus apabila mod senyap diaktifkan",
            notifGlobalSilent = "Mod Senyap diaktifkan · Azan didiamkan",
            notifVibrateAlert = "Amaran getaran · %1\$s telah masuk",
            notifPrayerArrived = "Waktu solat %1\$s telah tiba (%2\$s)",
            notifEnterPrayer = "Masuk solat dan menghadap Kaaba (%1\$s).",
            notifPrePrayerTitle = "%1\$s dalam %2\$d minit",
            notifPrePrayerText = "%1\$s bermula pada %2\$s · Sediakan diri untuk solat",
            notifAdhanInProgress = "Azan sedang berjalan",
            notifAdhanInProgressBody = "Amaran solat sedang dimainkan",
        ),    ),
)

val BengaliStrings = UiStrings(

    appName = "সালাহ",
    navToday = "আজ",
    navQibla = "কিবলা",
    navSettings = "সেটিংস",

    onlineStatus = "অনলাইন",
    offlineStatus = "অফলাইন",

    fajr = "ফজর",
    sunrise = "সূর্যোদয়",
    dhuhr = "জোহর",
    asr = "আসর",
    maghrib = "মাগরিব",
    isha = "ইশা",

    transparentCalculationSource = "স্বচ্ছ হিসাব পদ্ধতি ও উৎস",
    todaysTimes = "আজকের সময়সূচি",

    kaabaDistance = "কাবা শরীফের দূরত্ব",
    trueNorth = "প্রকৃত উত্তর",
    magneticNorth = "চৌম্বকীয় উত্তর",


    languageLabel = "ভাষা",
    appThemeLabel = "অ্যাপ থিম",
    locationLabel = "অবস্থান",
    methodLabel = "হিসাব পদ্ধতি",
    madhhabLabel = "আসর ফিকহ মাজহাব",
    adjustmentsLabel = "ম্যানুয়াল মিনিট সমন্বয়",
    hijriCalibrationLabel = "হিজরি তারিখ সমন্বয়",
    adhanCallLabel = "ওয়াক্তে আজানের আহ্বান",
    prePrayerReminderLabel = "নামাজের আগের রিমাইন্ডার",
    compassDiagnosticsLabel = "কম্পাস সেন্সর ও ডায়াগনস্টিক",
    networkSyncLabel = "নেটওয়ার্ক সিঙ্ক ও উৎস",
    privacyPhilosophyTitle = "গোপনীয়তা ও নীতি",

    cancel = "বাতিল",
    done = "সম্পন্ন",
    close = "বন্ধ করুন",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "পূর্ণ হিজরি মাস", // verify
            nextMonth = "পরের মাস", // verify
            nextPrayerPrefix = "পরবর্তী", // verify
            previousMonth = "আগের মাস", // verify
            nextDay = "পরের দিন", // verify
            previousDay = "আগের দিন", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "সেটিংস খুলুন",
        allowLocation = "অবস্থানের অনুমতি দিন",
        turnOnLocation = "অবস্থান চালু করুন",
        tryAgain = "আবার চেষ্টা করুন",
        allowExactAlarms = "সঠিক অ্যালার্মের অনুমতি দিন",
        exactAlarmsDelayed = "সঠিক অ্যালার্ম বন্ধ থাকলে সতর্কতা দেরিতে আসতে পারে।",
        notificationsPermissionNeeded = "নামাজের সতর্কতা দেখাতে বিজ্ঞপ্তির অনুমতি দিন।",
        notificationsBlocked = "বিজ্ঞপ্তি বন্ধ আছে। নামাজের সতর্কতা দেখাবে না।",
        prayerChannelBlocked = "নামাজের সতর্কতা সিস্টেম সেটিংসে বন্ধ আছে।",
        testNotification = "পরীক্ষামূলক বিজ্ঞপ্তি পাঠান",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "শিরোনাম",
        verseCountOne = "%d আয়াত",
        verseCountMany = "%d আয়াত",
        rangeTo = " থেকে ",
        paperRose = "গোলাপি",
        cardinalNorth = "উ",
        cardinalNorthEast = "উপূর্বে",
        cardinalEast = "পূর্বে",
        cardinalSouthEast = "দক্ষিণে",
        cardinalSouth = "দক্ষিণ",
        cardinalSouthWest = "দক্ষিণ-পশ্চিমে",
        cardinalWest = "পশ্চিমে",
        cardinalNorthWest = "উত্তর-পশ্চিমে",
    ),
        actionSave = "সংরক্ষণ",
        actionCancel = "বাতিল",
        actionClose = "বন্ধ",
        actionReset = "রিসেট",
        search = "অনুসন্ধান",
        loading = "লোড হচ্ছে",
        textSize = "লেখার আকার",
        sectionAppearance = "চেহারা",
        sectionLocationAndCalculation = "অবস্থান ও হিসাব",
        actionClear = "মুছুন", // verify
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
        selected = "নির্বাচিত",
        notSelected = "নির্বাচিত নয়",
        stateOn = "চালু",
        stateOff = "বন্ধ",
        noSearchResults = "কোনো ফলাফল নেই",
        allowNotificationsTitle = "Salah-কে আপনাকে অবহিত করতে দিন",
        allowNotificationsMessage = "নামাজের সময়ে আপনাকে সম্বন্ধ করতে Salah-কে বিজ্ঞপ্তির অনুমতি দরকার। নামাজের সময়সূচি মূল পর্দায়ই থাকে, এবং আপনি পরে সেটিংস থেকে সতর্কতা চালু করতে পারেন।",
        allowNotificationsAction = "বিজ্ঞপ্তি অনুমতি দিন",
        notNow = "এখন নয়",
        actionChange = "পরিবর্তন",
        alertSilentReminder = "নীরব স্মরণিকা", // verify
        ambientField = "পরিবেশী ক্ষেত্র",
        appliedAdjustments = "ম্যানুয়াল সমন্বয়", // verify
        calibrationMessage = "সেন্সরটি স্থিতিশীল হতে ডিভাইসটি কয়েকবার ৮ আকারে নাড়ুন।",
        chooseAdjustments = "মিনিট সমন্বয়", // verify
        chooseHijriOffset = "হিজরি তারিখ অফসেট", // verify
        chooseLanguage = "ভাষা",
        chooseMadhhab = "আসরের হিসাব", // verify
        chooseMethod = "গণনা পদ্ধতি", // verify
        choosePrePrayerOffset = "নামাজের আগে স্মরণিকা", // verify
        chooseTheme = "থিম",
        computedOnDevice = "পদ্ধতি, স্থান ও সমন্বয়", // verify
        reschedulePrayers = "নামাজের অ্যালার্ম পুনরায় সক্রিয় করুন",
        daysShort = "দিন", // verify
        gpsCached = "জিপিএস ক্যাশ করা হয়েছে",
        illumination = "আলোকসজ্জা",
        madhhabLabelShort = "আসর পদ্ধতি", // verify
        methodology = "পদ্ধতি", // verify
        minutesShort = "মিনিট", // verify
        prePrayerDisabled = "নিষ্ক্রিয়", // verify
        privacyNote = "নামাজের সময় এই ডিভাইসে গণনা করা হয়, অথবা অনলাইন থাকলে মরক্কোর হাবুস মন্ত্রণালয়ের প্রকাশিত সময়সূচি থেকে নেওয়া হয়। কিবলার দিক ও কুরআন এই ডিভাইসেই থাকে। কিছুই আপলোড হয় না।",
        privacyPhilosophyTitle = "গোপনীয়তা ও নীতি",
        prayerLogLabel = "নামাজ লগ",
        prayerLogStoredLocally = "এই ডিভাইসে সংরক্ষিত",
        privacyOffline = "ইন্টারনেট ছাড়াই চলে",
        privacyNoAds = "কোনো বিজ্ঞাপন নেই",
        privacyNoAnalytics = "কোনো বিশ্লেষণ বা ডেটা সংগ্রহ নেই",
        privacyNoAccount = "কোনো অ্যাকাউন্ট বা সাইন ইন নেই",
        privacyFree = "বিনামূল্যে এবং ওপেন সোর্স",
        resetAllConfirmMessage = "গণনা, সতর্কতা ও প্রদর্শনের সেটিংস ডিফল্টে ফিরে যায়। আপনার সংরক্ষিত আয়াত ও নামাজের বিবরণ থেকে যায়।",
        resetAllConfirmTitle = "সমস্ত সেটিংস পুনরায় সেট করবেন?",
        resetAllLabel = "সমস্ত সেটিংস পুনরায় সেট করুন",
        selectedCity = "নির্বাচিত শহর",
        sensorAccuracy = "সেন্সরের নির্ভুলতা",
        silentModeLabel = "সমস্ত সতর্কতা নীরব করুন", // verify
        storageLabel = "সঞ্চয়",
        sunAligned = "সূর্য প্রায় কিবলার দিকে। নিশ্চিত করতে এর দিকে মুখ করুন।", // verify
        sunAltitude = "সূর্য",
        sunAltitudeValue = "সূর্যের উচ্চতা",
        sunAzimuth = "সূর্যের দিক",
        sunBelowHorizon = "সূর্য ক্ষিতিজের নিচে, তাই এটিকে এখন রেফারেন্স হিসেবে ব্যবহার করা যাবে না।",
        sunToTheLeft = "কিবলা সূর্যের বাম পাশে প্রায় %d°।", // verify
        sunToTheRight = "কিবলা সূর্যের ডান পাশে প্রায় %d°।", // verify
        themeDark = "ঢাকা",
        themeLight = "উজ্জ্বল",
        themeSystem = "সিস্টেম অনুযায়ী",
        accentLabel = "অ্যাকসেন্ট রঙ",
        accentSlate = "স্লেট",
        accentPlum = "বরই",
        accentTeal = "সবুজাভ নীল",
        accentClay = "মাটির রঙ",
        accentWallpaper = "ওয়ালপেপার অনুসরণ করুন",
        reduceTransparencyLabel = "স্বচ্ছতা কমান",
        timeFormat24hLabel = "২৪ ঘণ্টার ফরম্যাট", // verify
        verseCount = "%d আয়াত", // verify
        vibrateOnlyLabel = "শুধু কম্পন", // verify
        waxing = "বর্ধমান", // verify
        locationAcquiring = "আপনার অবস্থান খোঁজা হচ্ছে…",
        locationResolved = "GPS অবস্থান: %1\$s, %2\$s",
        locationResolvedCached = "সর্বশেষ জানা অবস্থান: %1\$s, %2\$s",
        locationErrorNoPermission = "অবস্থানের অনুমতি দেওয়া হয়নি।",
        locationErrorServicesOff = "এই ডিভাইসে অবস্থান পরিষেবা বন্ধ আছে।",
        locationErrorNoSignal = "GPS সংকেত পাওয়া যায়নি। আপনার সংরক্ষিত অবস্থান ব্যবহৃত হচ্ছে।",
        notifications = NotificationStrings(
            notifChannelAdhan = "আজান ও নামাজের ডাকের সতর্কতা",
            notifChannelAdhanDescription = "নামাজের সময় এলে শব্দ বা আজানের সুরে জানায়",
            notifChannelPrePrayer = "নামাজের আগের মনে করানো",
            notifChannelPrePrayerDescription = "আসন্ন নামাজের আগে সংক্ষিপ্ত জানানো",
            notifChannelSilent = "নীরব নামাজের বিজ্ঞপ্তি",
            notifChannelSilentDescription = "নীরব মোড চালু থাকলে হালকা বিজ্ঞপ্তি",
            notifGlobalSilent = "নীরব মোড চালু · আজান বন্ধ",
            notifVibrateAlert = "কম্পন বিজ্ঞপ্তি · %1\$s শুরু হয়েছে",
            notifPrayerArrived = "%1\$s নামাজের সময় এসেছে (%2\$s)",
            notifEnterPrayer = "নামাজে প্রবেশ করুন এবং কাবার দিকে মুখ করুন (%1\$s)।",
            notifPrePrayerTitle = "%2\$d মিনিটে %1\$s",
            notifPrePrayerText = "%1\$s শুরু হবে %2\$s · নামাজের প্রস্তুতি নিন",
            notifAdhanInProgress = "আজান চলছে",
            notifAdhanInProgressBody = "নামাজের বিজ্ঞপ্তি বাজছে",
        ),    ),
)

val RussianStrings = UiStrings(

    appName = "SALAH",
    navToday = "Сегодня",
    navQibla = "Кибла",
    navSettings = "Настройки",

    onlineStatus = "ОНЛАЙН",
    offlineStatus = "ОФФЛАЙН",

    fajr = "Фаджр",
    sunrise = "Восход",
    dhuhr = "Зухр",
    asr = "Аср",
    maghrib = "Магриб",
    isha = "Иша",

    transparentCalculationSource = "Прозрачный метод расчета",
    todaysTimes = "ВРЕМЯ НА СЕГОДНЯ",

    kaabaDistance = "Расстояние до Каабы",
    trueNorth = "Истинный север",
    magneticNorth = "Магнитный север",


    languageLabel = "Язык интерфейса",
    appThemeLabel = "Тема приложения",
    locationLabel = "Местоположение",
    methodLabel = "Метод расчета",
    madhhabLabel = "Мазхаб (Аср)",
    adjustmentsLabel = "Ручная корректировка (минуты)",
    hijriCalibrationLabel = "Калибровка календаря Хиджры",
    adhanCallLabel = "Звук азана при наступлении времени",
    prePrayerReminderLabel = "Напоминание перед намазом",
    compassDiagnosticsLabel = "Датчики компаса и диагностика",
    networkSyncLabel = "Синхронизация с сетью и источник",
    privacyPhilosophyTitle = "Конфиденциальность и философия",

    cancel = "Отмена",
    done = "Готово",
    close = "Закрыть",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Полный месяц хиджры", // verify
            nextMonth = "Следующий месяц", // verify
            nextPrayerPrefix = "След.", // verify
            previousMonth = "Предыдущий месяц", // verify
            nextDay = "Следующий день", // verify
            previousDay = "Предыдущий день", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Открыть настройки",
        allowLocation = "Разрешить геопозицию",
        turnOnLocation = "Включить геопозицию",
        tryAgain = "Повторить",
        allowExactAlarms = "Разрешить точные будильники",
        exactAlarmsDelayed = "Оповещения могут приходить с опозданием, пока точные будильники отключены.",
        notificationsPermissionNeeded = "Разрешите уведомления, чтобы появлялись оповещения о намазе.",
        notificationsBlocked = "Уведомления отключены. Оповещения о намазе не появятся.",
        prayerChannelBlocked = "Оповещения о намазе отключены в настройках системы.",
        testNotification = "Отправить тестовое уведомление",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Заголовок",
        verseCountOne = "%d аят",
        verseCountMany = "%d аятов",
        rangeTo = " - ",
        paperRose = "Розовый",
        cardinalNorth = "С",
        cardinalNorthEast = "СВ",
        cardinalEast = "В",
        cardinalSouthEast = "ЮВ",
        cardinalSouth = "Ю",
        cardinalSouthWest = "ЮЗ",
        cardinalWest = "З",
        cardinalNorthWest = "СЗ",
    ),
        actionSave = "Сохранить",
        actionCancel = "Отмена",
        actionClose = "Закрыть",
        actionReset = "Сбросить",
        search = "Поиск",
        loading = "Загрузка",
        textSize = "Размер текста",
        sectionAppearance = "Оформление",
        sectionLocationAndCalculation = "Местоположение и расчёт",
        actionClear = "Очистить", // verify
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
        selected = "Выбрано",
        notSelected = "Не выбрано",
        stateOn = "Вкл.",
        stateOff = "Выкл.",
        noSearchResults = "Ничего не найдено",
        allowNotificationsTitle = "Разрешить уведомления Salah",
        allowNotificationsMessage = "Salah нужен доступ к уведомлениям, чтобы напомнить вам о времени намаза. Расписание всё равно отображается на главном экране, а оповещения можно включить позже в настройках.",
        allowNotificationsAction = "Разрешить уведомления",
        notNow = "Не сейчас",
        actionChange = "Изменить",
        alertSilentReminder = "Тихое напоминание", // verify
        ambientField = "Окружающее поле",
        appliedAdjustments = "Ручные поправки", // verify
        calibrationMessage = "Двигайте устройство в виде восьмёрки несколько раз, чтобы датчик стабилизировался.",
        chooseAdjustments = "Поправки в минутах", // verify
        chooseHijriOffset = "Смещение хиджры", // verify
        chooseLanguage = "Язык",
        chooseMadhhab = "Расчёт Асра", // verify
        chooseMethod = "Метод расчёта", // verify
        choosePrePrayerOffset = "Напоминание до молитвы", // verify
        chooseTheme = "Тема",
        computedOnDevice = "Метод, место и поправки", // verify
        reschedulePrayers = "Перевключить сигналы намаза",
        daysShort = "дн", // verify
        gpsCached = "GPS закэширован",
        illumination = "Освещённость",
        madhhabLabelShort = "Метод Асра", // verify
        methodology = "Методика", // verify
        minutesShort = "мин", // verify
        prePrayerDisabled = "Выкл.", // verify
        privacyNote = "Время молитв вычисляется на этом устройстве или берётся из официального расписания Министерства хабусов Марокко, когда устройство в сети. Направление киблы и Коран остаются на этом устройстве. Ничего не отправляется.",
        privacyPhilosophyTitle = "Конфиденциальность и философия",
        prayerLogLabel = "Журнал намазов",
        prayerLogStoredLocally = "Сохранено на этом устройстве",
        privacyOffline = "Работает без подключения",
        privacyNoAds = "Без рекламы",
        privacyNoAnalytics = "Без аналитики и сбора данных",
        privacyNoAccount = "Без аккаунта и входа",
        privacyFree = "Бесплатно и с открытым исходным кодом",
        resetAllConfirmMessage = "Настройки расчёта, оповещений и отображения возвращаются к значениям по умолчанию. Сохранённые аяты и журнал молитв сохраняются.",
        resetAllConfirmTitle = "Сбросить все настройки?",
        resetAllLabel = "Сбросить все настройки",
        selectedCity = "Выбранный город",
        sensorAccuracy = "Точность датчика",
        silentModeLabel = "Выключить все оповещения", // verify
        storageLabel = "Хранилище",
        sunAligned = "Солнце почти в направлении киблы. Повернитесь к нему, чтобы подтвердить.", // verify
        sunAltitude = "Солнце",
        sunAltitudeValue = "Высота солнца",
        sunAzimuth = "Направление солнца",
        sunBelowHorizon = "Солнце под горизонтом, поэтому сейчас его нельзя использовать как ориентир.",
        sunToTheLeft = "Кибла примерно в %d° слева от солнца.", // verify
        sunToTheRight = "Кибла примерно в %d° справа от солнца.", // verify
        themeDark = "Тёмная",
        themeLight = "Светлая",
        themeSystem = "Как в системе",
        accentLabel = "Цвет акцента",
        accentSlate = "Сланцево-синий",
        accentPlum = "Сливовый",
        accentTeal = "Бирюзовый",
        accentClay = "Глиняный",
        accentWallpaper = "Как на обоях",
        reduceTransparencyLabel = "Уменьшить прозрачность",
        timeFormat24hLabel = "24-часовой формат", // verify
        verseCount = "%d аятов", // verify
        vibrateOnlyLabel = "Только вибрация", // verify
        waxing = "Растущая", // verify
        locationAcquiring = "Определение местоположения…",
        locationResolved = "Местоположение по GPS: %1\$s, %2\$s",
        locationResolvedCached = "Последнее известное местоположение: %1\$s, %2\$s",
        locationErrorNoPermission = "Доступ к местоположению не предоставлен.",
        locationErrorServicesOff = "Службы определения местоположения отключены на этом устройстве.",
        locationErrorNoSignal = "Не удалось получить сигнал GPS. Сохранённое местоположение по-прежнему используется.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Оповещения азана и намаза",
            notifChannelAdhanDescription = "Сообщает о наступлении времени намаза — со звуком или тоном азана",
            notifChannelPrePrayer = "Напоминания перед намазом",
            notifChannelPrePrayerDescription = "Короткое уведомление перед приближающимся намазом",
            notifChannelSilent = "Тихие уведомления о намазе",
            notifChannelSilentDescription = "Ненавязчивые уведомления при включённом беззвучном режиме",
            notifGlobalSilent = "Беззвучный режим включён · Азан приглушён",
            notifVibrateAlert = "Виброуведомление · %1\$s наступило",
            notifPrayerArrived = "Время намаза %1\$s наступило (%2\$s)",
            notifEnterPrayer = "Встаньте на намаз и повернитесь к Каабе (%1\$s).",
            notifPrePrayerTitle = "%1\$s через %2\$d мин",
            notifPrePrayerText = "%1\$s начнётся в %2\$s · Приготовьтесь к намазу",
            notifAdhanInProgress = "Азан играет",
            notifAdhanInProgressBody = "Звучит уведомление о намазе",
        ),    ),
)

val GermanStrings = UiStrings(

    appName = "SALAH",
    navToday = "Heute",
    navQibla = "Qibla",
    navSettings = "Einstellungen",

    onlineStatus = "ONLINE",
    offlineStatus = "OFFLINE",

    fajr = "Fajr",
    sunrise = "Sonnenaufgang",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Transparente Berechnungsmethode",
    todaysTimes = "HEUTIGE ZEITEN",

    kaabaDistance = "Entfernung zur Kaaba",
    trueNorth = "Geographisch Nord",
    magneticNorth = "Magnetisch Nord",


    languageLabel = "Sprache",
    appThemeLabel = "App-Design",
    locationLabel = "Standort",
    methodLabel = "Berechnungsmethode",
    madhhabLabel = "Rechtsschule (Asr)",
    adjustmentsLabel = "Manuelle Minutenanpassung",
    hijriCalibrationLabel = "Hidschri-Kalibrierung",
    adhanCallLabel = "Gebetsruf zur Gebetszeit",
    prePrayerReminderLabel = "Erinnerung vor dem Gebet",
    compassDiagnosticsLabel = "Kompasssensoren & Diagnose",
    networkSyncLabel = "Netzwerksynchronisation & Quelle",
    privacyPhilosophyTitle = "Datenschutz & Philosophie",

    cancel = "Abbrechen",
    done = "Fertig",
    close = "Schließen",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Ganzer Hijri-Monat", // verify
            nextMonth = "Nächster Monat", // verify
            nextPrayerPrefix = "Nächst.", // verify
            previousMonth = "Vorheriger Monat", // verify
            nextDay = "Nächster Tag", // verify
            previousDay = "Vorheriger Tag", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Einstellungen öffnen",
        allowLocation = "Standort erlauben",
        turnOnLocation = "Standort einschalten",
        tryAgain = "Erneut versuchen",
        allowExactAlarms = "Exakte Alarme erlauben",
        exactAlarmsDelayed = "Erinnerungen können verspätet kommen, solange exakte Alarme aus sind.",
        notificationsPermissionNeeded = "Erlaube Benachrichtigungen, damit Gebetserinnerungen erscheinen.",
        notificationsBlocked = "Benachrichtigungen sind aus. Gebetserinnerungen erscheinen nicht.",
        prayerChannelBlocked = "Gebetserinnerungen sind in den Systemeinstellungen ausgeschaltet.",
        testNotification = "Testbenachrichtigung senden",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Überschrift",
        verseCountOne = "%d Vers",
        verseCountMany = "%d Verse",
        rangeTo = " bis ",
        paperRose = "Rose",
        cardinalNorth = "N",
        cardinalNorthEast = "NO",
        cardinalEast = "O",
        cardinalSouthEast = "SO",
        cardinalSouth = "S",
        cardinalSouthWest = "SW",
        cardinalWest = "W",
        cardinalNorthWest = "NW",
    ),
        actionSave = "Speichern",
        actionCancel = "Abbrechen",
        actionClose = "Schließen",
        actionReset = "Zurücksetzen",
        search = "Suchen",
        loading = "Wird geladen",
        textSize = "Schriftgröße",
        sectionAppearance = "Darstellung",
        sectionLocationAndCalculation = "Standort & Berechnung",
        actionClear = "Leeren", // verify
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
        selected = "Ausgewählt",
        notSelected = "Nicht ausgewählt",
        stateOn = "Ein",
        stateOff = "Aus",
        noSearchResults = "Keine Ergebnisse",
        allowNotificationsTitle = "Salah darf Sie benachrichtigen",
        allowNotificationsMessage = "Salah benötigt die Benachrichtigungsberechtigung, um Sie zur Gebetszeit zu erinnern. Die Gebetszeiten bleiben ohnehin auf dem Startbildschirm sichtbar; Hinweise können Sie später in den Einstellungen aktivieren.",
        allowNotificationsAction = "Benachrichtigungen erlauben",
        notNow = "Jetzt nicht",
        actionChange = "Ändern",
        alertSilentReminder = "Stille Erinnerung", // verify
        ambientField = "Umgebungsfeld",
        appliedAdjustments = "Manuelle Anpassungen", // verify
        calibrationMessage = "Bewegen Sie das Gerät mehrmals in einer Acht, damit der Sensor sich stabilisiert.",
        chooseAdjustments = "Minuten-Anpassungen", // verify
        chooseHijriOffset = "Hijri-Datum-Versatz", // verify
        chooseLanguage = "Sprache",
        chooseMadhhab = "Asr-Berechnung", // verify
        chooseMethod = "Berechnungsmethode", // verify
        choosePrePrayerOffset = "Erinnerung vor dem Gebet", // verify
        chooseTheme = "Design",
        computedOnDevice = "Methode, Ort und Korrekturen", // verify
        reschedulePrayers = "Gebetsalarme neu aktivieren",
        daysShort = "T", // verify
        gpsCached = "GPS zwischengespeichert",
        illumination = "Beleuchtung",
        madhhabLabelShort = "Asr-Methode", // verify
        methodology = "Methode", // verify
        minutesShort = "Min", // verify
        prePrayerDisabled = "Aus", // verify
        privacyNote = "Gebetszeiten werden auf diesem Gerät berechnet oder, wenn das Gerät online ist, aus dem veröffentlichten Zeitplan des marokkanischen Habus-Ministeriums übernommen. Qibla-Richtung und Koran bleiben auf diesem Gerät. Es wird nichts hochgeladen.",
        privacyPhilosophyTitle = "Datenschutz & Philosophie",
        prayerLogLabel = "Gebetsprotokoll",
        prayerLogStoredLocally = "Auf diesem Gerät gespeichert",
        privacyOffline = "Funktioniert ohne Verbindung",
        privacyNoAds = "Keine Werbung",
        privacyNoAnalytics = "Keine Analyse oder Datenerfassung",
        privacyNoAccount = "Kein Konto und keine Anmeldung",
        privacyFree = "Kostenlos und Open Source",
        resetAllConfirmMessage = "Berechnungs-, Warn- und Anzeigeeinstellungen werden auf die Standardwerte zurückgesetzt. Ihre gespeicherten Verse und Ihr Gebetsprotokoll bleiben erhalten.",
        resetAllConfirmTitle = "Alle Einstellungen zurücksetzen?",
        resetAllLabel = "Alle Einstellungen zurücksetzen",
        selectedCity = "Ausgewählte Stadt",
        sensorAccuracy = "Sengenauigkeit",
        silentModeLabel = "Alle Warnungen stummschalten", // verify
        storageLabel = "Speicher",
        sunAligned = "Die Sonne steht fast in Qibla-Richtung. Drehen Sie sich ihr zu, um zu bestätigen.", // verify
        sunAltitude = "Sonne",
        sunAltitudeValue = "Sonnenhöhe",
        sunAzimuth = "Sonnenrichtung",
        sunBelowHorizon = "Die Sonne steht unter dem Horizont und kann deshalb gerade nicht als Referenz dienen.",
        sunToTheLeft = "Die Qibla liegt etwa %d° links von der Sonne.", // verify
        sunToTheRight = "Die Qibla liegt etwa %d° rechts von der Sonne.", // verify
        themeDark = "Dunkel",
        themeLight = "Hell",
        themeSystem = "System folgen",
        accentLabel = "Akzentfarbe",
        accentSlate = "Schieferblau",
        accentPlum = "Pflaume",
        accentTeal = "Blaugrün",
        accentClay = "Ton",
        accentWallpaper = "Hintergrundbild folgen",
        reduceTransparencyLabel = "Transparenz reduzieren",
        timeFormat24hLabel = "24-Stunden-Format", // verify
        verseCount = "%d Verse", // verify
        vibrateOnlyLabel = "Nur vibrieren", // verify
        waxing = "Zunehmend", // verify
        locationAcquiring = "Standort wird ermittelt…",
        locationResolved = "GPS-Standort: %1\$s, %2\$s",
        locationResolvedCached = "Zuletzt bekannter Standort: %1\$s, %2\$s",
        locationErrorNoPermission = "Die Standortberechtigung wurde nicht erteilt.",
        locationErrorServicesOff = "Die Standortdienste sind auf diesem Gerät ausgeschaltet.",
        locationErrorNoSignal = "Kein GPS-Signal erhalten. Dein gespeicherter Standort wird weiterhin verwendet.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Adhan- und Gebetsaufrufe",
            notifChannelAdhanDescription = "Meldet den Gebetsbeginn mit Ton oder Adhan-Klang",
            notifChannelPrePrayer = "Erinnerungen vor dem Gebet",
            notifChannelPrePrayerDescription = "Ein sanfter Hinweis vor dem nächsten Gebet",
            notifChannelSilent = "Stille Gebetsbenachrichtigungen",
            notifChannelSilentDescription = "Unaufdringliche Hinweise im lautlosen Modus",
            notifGlobalSilent = "Stiller Modus aktiv · Adhan stummgeschaltet",
            notifVibrateAlert = "Vibrationshinweis · %1\$s ist eingetreten",
            notifPrayerArrived = "Die Gebetszeit für %1\$s ist eingetreten (%2\$s)",
            notifEnterPrayer = "Zieh das Gebet ein und wende dich der Kaaba zu (%1\$s).",
            notifPrePrayerTitle = "%1\$s in %2\$d Minuten",
            notifPrePrayerText = "%1\$s beginnt um %2\$s · Bereite dich auf das Gebet vor",
            notifAdhanInProgress = "Adhan läuft",
            notifAdhanInProgressBody = "Gebetshinweis wird abgespielt",
        ),    ),
)

val SpanishStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hoy",
    navQibla = "Alquibla",
    navSettings = "Ajustes",

    onlineStatus = "EN LÍNEA",
    offlineStatus = "SIN CONEXIÓN",

    fajr = "Fajr",
    sunrise = "Amanecer",
    dhuhr = "Dhuhr",
    asr = "Asr",
    maghrib = "Maghrib",
    isha = "Isha",

    transparentCalculationSource = "Fuente de cálculo transparente",
    todaysTimes = "HORARIOS DE HOY",

    kaabaDistance = "Distancia a la Kaaba",
    trueNorth = "Norte verdadero",
    magneticNorth = "Norte magnético",


    languageLabel = "Idioma",
    appThemeLabel = "Tema de la aplicación",
    locationLabel = "Ubicación",
    methodLabel = "Método de cálculo",
    madhhabLabel = "Escuela jurídica (Asr)",
    adjustmentsLabel = "Ajustes manuales (minutos)",
    hijriCalibrationLabel = "Calibración fecha hiyri",
    adhanCallLabel = "Llamada a la oración puntual",
    prePrayerReminderLabel = "Recordatorio antes de la oración",
    compassDiagnosticsLabel = "Sensores de brújula y diagnóstico",
    networkSyncLabel = "Sincronización de red y fuente",
    privacyPhilosophyTitle = "Privacidad y Filosofía",

    cancel = "Cancelar",
    done = "Listo",
    close = "Cerrar",

    // --- Translated critical subset -------------------------------------
    // The other ~170 fields of UiStringsMore stay English until translated.
    dateNav = DateNavStrings(
            fullHijriMonth = "Mes hégira completo", // verify
            nextMonth = "Mes siguiente", // verify
            nextPrayerPrefix = "Próxima", // verify
            previousMonth = "Mes anterior", // verify
            nextDay = "Día siguiente", // verify
            previousDay = "Día anterior", // verify
    ),
    system = SystemAccessStrings(
        openSettings = "Abrir ajustes",
        allowLocation = "Permitir ubicación",
        turnOnLocation = "Activar ubicación",
        tryAgain = "Reintentar",
        allowExactAlarms = "Permitir alarmas exactas",
        exactAlarmsDelayed = "Las alertas pueden llegar tarde mientras las alarmas exactas estén desactivadas.",
        notificationsPermissionNeeded = "Permite las notificaciones para que aparezcan los avisos de oración.",
        notificationsBlocked = "Las notificaciones están desactivadas. Los avisos de oración no aparecerán.",
        prayerChannelBlocked = "Los avisos de oración están desactivados en los ajustes del sistema.",
        testNotification = "Enviar una notificación de prueba",
    ),
    more = UiStringsMore(
reader = ReaderStrings(
        headingLabel = "Título",
        verseCountOne = "%d versículo",
        verseCountMany = "%d versículos",
        rangeTo = " a ",
        paperRose = "Rosa",
        cardinalNorth = "N",
        cardinalNorthEast = "NE",
        cardinalEast = "E",
        cardinalSouthEast = "SE",
        cardinalSouth = "S",
        cardinalSouthWest = "SO",
        cardinalWest = "O",
        cardinalNorthWest = "NO",
    ),
        actionSave = "Guardar",
        actionCancel = "Cancelar",
        actionClose = "Cerrar",
        actionReset = "Restablecer",
        search = "Buscar",
        loading = "Cargando",
        textSize = "Tamaño del texto",
        sectionAppearance = "Apariencia",
        sectionLocationAndCalculation = "Ubicación y cálculo",
        actionClear = "Borrar", // verify
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
        selected = "Seleccionado",
        notSelected = "No seleccionado",
        stateOn = "Activado",
        stateOff = "Desactivado",
        noSearchResults = "Sin resultados",
        allowNotificationsTitle = "Permite que Salah te avise",
        allowNotificationsMessage = "Salah necesita permiso de notificaciones para avisarte a la hora de la oración. Los horarios siguen visibles en la pantalla de inicio, y puedes activar las alertas más tarde en Ajustes.",
        allowNotificationsAction = "Permitir notificaciones",
        notNow = "Ahora no",
        actionChange = "Cambiar",
        alertSilentReminder = "Recordatorio silencioso", // verify
        ambientField = "Campo ambiental",
        appliedAdjustments = "Ajustes manuales", // verify
        calibrationMessage = "Mueva el dispositivo en forma de ocho varias veces para que el sensor se estabilice.",
        chooseAdjustments = "Ajustes en minutos", // verify
        chooseHijriOffset = "Desfase de la fecha hégira", // verify
        chooseLanguage = "Idioma",
        chooseMadhhab = "Cálculo del Asr", // verify
        chooseMethod = "Método de cálculo", // verify
        choosePrePrayerOffset = "Recordatorio antes de la oración", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Método, lugar y ajustes", // verify
        reschedulePrayers = "Reactivar las alarmas de oración",
        daysShort = "d", // verify
        gpsCached = "GPS en caché",
        illumination = "Iluminación",
        madhhabLabelShort = "Método del Asr", // verify
        methodology = "Metodología", // verify
        minutesShort = "min", // verify
        prePrayerDisabled = "Desactivado", // verify
        privacyNote = "Los horarios de oración se calculan en este dispositivo o, cuando está en línea, se toman del calendario publicado por el Ministerio de Habices de Marruecos. La dirección de la qibla y el Corán permanecen en este dispositivo. No se sube nada.",
        privacyPhilosophyTitle = "Privacidad y Filosofía",
        prayerLogLabel = "Registro de oración",
        prayerLogStoredLocally = "Guardado en este dispositivo",
        privacyOffline = "Funciona sin conexión",
        privacyNoAds = "Sin publicidad",
        privacyNoAnalytics = "Sin análisis ni recopilación de datos",
        privacyNoAccount = "Sin cuenta ni inicio de sesión",
        privacyFree = "Gratuito y de código abierto",
        resetAllConfirmMessage = "Los ajustes de cálculo, alerta y visualización vuelven a sus valores predeterminados. Tus versículos guardados y tu registro de oraciones se conservan.",
        resetAllConfirmTitle = "¿Restablecer todos los ajustes?",
        resetAllLabel = "Restablecer todos los ajustes",
        selectedCity = "Ciudad seleccionada",
        sensorAccuracy = "Precisión del sensor",
        silentModeLabel = "Silenciar todas las alertas", // verify
        storageLabel = "Almacenamiento",
        sunAligned = "El sol está casi en la dirección de la qibla. Enfréntalo para confirmar.", // verify
        sunAltitude = "Sol",
        sunAltitudeValue = "Altitud del sol",
        sunAzimuth = "Dirección del sol",
        sunBelowHorizon = "El sol está bajo el horizonte, así que no puede usarse como referencia ahora.",
        sunToTheLeft = "La qibla está aproximadamente %d° a la izquierda del sol.", // verify
        sunToTheRight = "La qibla está aproximadamente %d° a la derecha del sol.", // verify
        themeDark = "Oscuro",
        themeLight = "Claro",
        themeSystem = "Igual al sistema",
        accentLabel = "Color de acento",
        accentSlate = "Pizarra",
        accentPlum = "Ciruela",
        accentTeal = "Verde azulado",
        accentClay = "Arcilla",
        accentWallpaper = "Igual que el fondo",
        reduceTransparencyLabel = "Reducir transparencia",
        timeFormat24hLabel = "Formato de 24 horas", // verify
        verseCount = "%d versículos", // verify
        vibrateOnlyLabel = "Solo vibrar", // verify
        waxing = "Creciente", // verify
        locationAcquiring = "Buscando tu ubicación…",
        locationResolved = "Ubicación por GPS: %1\$s, %2\$s",
        locationResolvedCached = "Última ubicación conocida: %1\$s, %2\$s",
        locationErrorNoPermission = "No se ha concedido el permiso de ubicación.",
        locationErrorServicesOff = "Los servicios de ubicación están desactivados en este dispositivo.",
        locationErrorNoSignal = "No se ha podido obtener la señal GPS. Tu ubicación guardada sigue en uso.",
        notifications = NotificationStrings(
            notifChannelAdhan = "Alertas de adhan y llamada a la salá",
            notifChannelAdhanDescription = "Avisa al llegar la hora de la salá, con sonido o tono de adhan",
            notifChannelPrePrayer = "Recordatorios previos a la salá",
            notifChannelPrePrayerDescription = "Un aviso discreto antes de la próxima salá",
            notifChannelSilent = "Notificaciones de salá silenciosas",
            notifChannelSilentDescription = "Notificaciones discretas cuando está activo el modo silencioso",
            notifGlobalSilent = "Modo silencioso activo · Adhan silenciado",
            notifVibrateAlert = "Aviso por vibración · Ha entrado %1\$s",
            notifPrayerArrived = "Ha llegado la hora de la salá de %1\$s (%2\$s)",
            notifEnterPrayer = "Entra en la salá y orienta hacia la Kaaba (%1\$s).",
            notifPrePrayerTitle = "%1\$s en %2\$d minutos",
            notifPrePrayerText = "%1\$s comienza a las %2\$s · Prepárate para la salá",
            notifAdhanInProgress = "El adhan está sonando",
            notifAdhanInProgressBody = "El aviso de salá está sonando",
        ),    ),
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

/**
 * The interface language, as the reader chose it.
 *
 * [LocalStrings] cannot answer it. It has already been used to produce the strings and
 * does not know which bundle they came from, and a reader who has chosen Arabic is still
 * looking at the Quran's Arabic, an English translation, an English surah name and a
 * romanisation of it. Picking between those needs the language, not the strings.
 */
val LocalLanguage = staticCompositionLocalOf { "English" }

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
        LocalLayoutDirection provides direction,
        LocalLanguage provides language
    ) {
        content()
    }
}

/**
 * The platform locale of a stored app-language name.
 *
 * The reading surfaces ask the platform for month and weekday names, and the app's own
 * language is not the device's: a reader can choose Arabic on a German phone, and the
 * month sheet was then ruled in German beside Arabic headings. The locale comes from the
 * same [AppLanguage] the strings and the layout direction come from, so the language is
 * one decision rather than three.
 */
fun appLocale(language: String): Locale =
    Locale.forLanguageTag(AppLanguage.fromNameOrCode(language).code)
