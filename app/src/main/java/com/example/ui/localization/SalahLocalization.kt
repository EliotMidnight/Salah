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
    val continueReading: String,
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
    val prayerTimesHeader: String,
    val vigilsAndNightPeriods: String,
    val imsakTitle: String,
    val midnightTitle: String,
    val lastThirdTitle: String,
    val todayBtn: String,

    // Qibla Screen
    val kaabaDistance: String,
    val trueNorth: String,
    val magneticNorth: String,

    // Quran Screen
    val meccan: String,
    val medinan: String,

    // Settings Screen
    val languageLabel: String,
    val appThemeLabel: String,
    val sectionPrayerCalc: String,
    val locationLabel: String,
    val methodLabel: String,
    val madhhabLabel: String,
    val adjustmentsLabel: String,
    val hijriCalibrationLabel: String,
    val adhanCallLabel: String,
    val prePrayerReminderLabel: String,
    val adhanSoundLabel: String,
    val adhanVolumeLabel: String,
    val sectionQuran: String,
    val riwayahLabel: String,
    val scriptStyleLabel: String,
    val reciterLabel: String,
    val translationLabel: String,
    val arabicTextSizeLabel: String,
    val autoMasjidModeLabel: String,
    val sectionSystemDiagnostics: String,
    val compassDiagnosticsLabel: String,
    val networkSyncLabel: String,
    val privacyPhilosophyTitle: String,

    // Dialog & Common Buttons
    val cancel: String,
    val confirm: String,
    val save: String,
    val done: String,
    val close: String,

    /**
     * Copy added by the redesign.
     */
    val more: UiStringsMore = UiStringsMore(),
    val dateNav: DateNavStrings = DateNavStrings()
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
    val openIndex: String = "Open the index",
    val immersiveMode: String = "Immersive mode",
    val exitImmersive: String = "Leave immersive mode",
    val saveThisLocation: String = "Save this verse",
    val layoutPerPage: String = "Per page",
    val layoutContinuousSurah: String = "Continuous surah",
    val scrollDirection: String = "Scroll direction",
    val previousPage: String = "Previous page",
    val nextPage: String = "Next page",
    val perVerseTitle: String = "Break out each verse",
    val perVerseDescription: String =
        "Study mode: every verse gets its own reference and its own actions, revealed when you select it.",
    val scrollVertical: String = "Vertical",
    val scrollHorizontal: String = "Horizontal",
    val backgroundColour: String = "Background",
    val backgroundDefault: String = "App default",

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
    val paperApricot: String = "Apricot",

    /** The sand paper. */
    val paperSand: String = "Sand",

    /** The sage paper. */
    val paperSage: String = "Sage",

    /** The mist paper. */
    val paperMist: String = "Mist",

    /** The indigo paper. */
    val paperIndigo: String = "Indigo",

    /** The lilac paper. */
    val paperLilac: String = "Lilac",
    val pinchBehaviour: String = "Pinch does",
    val pinchZoomView: String = "Zoom the view",
    val pinchTextSize: String = "Change text size",
    val arabicTextSize: String = "Arabic size",
    val translationSize: String = "Translation size",
    val quranFont: String = "Quran font",
    val showTranslationLabelShort: String = "Show translation",
    val indexSurahs: String = "Surahs",
    val indexSaved: String = "Saved",
    val indexSearch: String = "Search",
    val emptySavedTitle: String = "Nothing saved yet",
    val emptySavedMessage: String =
        "Save a verse while reading and it will be waiting here.",
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
    val pageAnnouncement: String =
        "Page %1\$d, juz' %2\$d, surah %3\$s, %4\$s, from %5\$s to %6\$s",

    /** A block of continuous text, announced: extent, verse count, page. */
    val blockAnnouncement: String = "%1\$s to %2\$s, %3\$s, page %4\$d",

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
    val mushaf: String = "Mushaf",

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
    val changeInReader: String = "Change this in the reader\u2019s Reading options."
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
     * A range of surah numbers as this language writes one, e.g. "2 to 3".
     *
     * A single number when the two ends are the same, because "surah 2 to 2" is a
     * range nobody meant to write - and a page that does not cross a boundary is 51
     * fewer pages where the question arises.
     */
    fun range(from: Int, to: Int): String =
        if (from == to) from.toString() else "$from$rangeTo$to"

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
    val nextMonth: String = "Next month"
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
    val notifSilentFor: String = "Silent Mode active for %1\$s",
    val notifVibrateAlert: String = "Vibrate alert \u00b7 %1\$s has entered",
    val notifTakbeerAlert: String = "Takbeer alert \u00b7 Time for %1\$s",
    val notifChimeAlert: String = "Gentle Chime alert \u00b7 Time for %1\$s",
    val notifPrayerArrived: String = "Time for %1\$s prayer has arrived (%2\$s)",
    val notifEnterPrayer: String =
        "Enter prayer and turn towards the Holy Kaaba (%1\$s).",
    val notifSilenceAction: String = "Silence",
    val notifMarkPrayed: String = "Mark prayed",
    val notifPrePrayerTitle: String = "%1\$s in %2\$d minutes",
    val notifPrePrayerText: String =
        "%1\$s begins at %2\$s \u00b7 Prepare for prayer",
    val notifAdhanInProgress: String = "Adhan in progress",
    val notifAdhanInProgressBody: String = "Prayer alert is playing",
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
    val computedOnDevice: String = "Computed on this device",
    /**
     * Re-derive today's times and re-arm the alarms.
     */
    val reschedulePrayers: String = "Re-arm prayer alarms",
    val minutesShort: String = "min",
    val daysShort: String = "days",
    val corpusSummary: String = "114 surahs · 30 juz · 6,236 verses",
    val noSearchResults: String = "No matches",
    val searchSurahsAndVerses: String = "Search surahs and verses",
    val versesFound: String = "%d verses",

    /**
     * The note that says the list is a page and not everything.
     */
    val searchShowingFirst: String = "showing the first %d",    val surahsFound: String = "%d surahs",
    val verseCount: String = "%d verses",
    val verseReference: String = "%d:%d",
    val selectSurah: String = "Select surah",
    val textSize: String = "Text size",
    val showTranslation: String = "Show translation",
    val recitingLabel: String = "Reciting",
    val stopAudio: String = "Stop",
    val playVerse: String = "Play",
    val pauseVerse: String = "Pause",
    val bookmarkVerse: String = "Save verse",
    val removeBookmark: String = "Remove saved verse",
    val copyVerse: String = "Copy",
    val shareVerse: String = "Share",
    val verseCopied: String = "Verse copied",
    val juzOf: String = "Juz %d",
    val hizbOf: String = "Hizb %d",
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
    val chooseHijriOffset: String = "Hijri date offset",
    val chooseAdhanSound: String = "Adhan sound",
    val chooseReciter: String = "Reciter",
    val chooseScript: String = "Script",
    val choosePrePrayerOffset: String = "Reminder before prayer",
    val prePrayerDisabled: String = "Off",
    val perPrayerModes: String = "Alert per prayer",
    val adhanVolume: String = "Volume",
    val testSound: String = "Test sound",
    val silentModeLabel: String = "Silence all alerts",
    val autoSilenceDurationLabel: String = "Auto-silence duration",
    val vibrateOnlyLabel: String = "Vibrate only",
    val storageLabel: String = "Storage",
    val audioSourceLabel: String = "Recitation audio",
    val audioStreamedNotCached: String = "Streamed, not stored on device",
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
    val hizbWord: String = "Hizb",
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
    val juzWord: String = "Juz’",
    val pageWord: String = "Page",
    val privacyNote: String = "Prayer times, the Qibla direction and the Quran are all calculated on this device. Nothing is uploaded.",
    val privacyOffline: String = "Completely offline",
    val privacyNoAds: String = "No advertising",
    val privacyNoAnalytics: String = "No analytics or data collection",
    val privacyNoAccount: String = "No account or sign-in",
    val privacyFree: String = "Free and open source",
    val allSurahsLabel: String = "All surahs",
    val searchHintTitle: String = "Search the Quran",
    val searchHintMessage: String = "Find a surah by name or meaning, or a verse by its Arabic or English text.",
    val selected: String = "Selected",
    val notSelected: String = "Not selected",
    val nowReading: String = "Now reading",
    val loadingQuranMessage: String =
        "Preparing the text. If this does not finish, go back and pick the surah again.",
    val noResultsMessage: String =
        "Nothing matched. Try a different spelling, or a shorter query.",
    val versesLabel: String = "Verses",
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
        "Salah needs permission to show notifications so it can call you at prayer " +
            "times. Prayer times stay on the Today screen either way, and you can " +
            "turn alerts on later in Settings.",
    val allowNotificationsAction: String = "Allow notifications",
    val notNow: String = "Not now",
    val selectLayoutTitle: String = "Reading layout",
    /**
     * Everything the immersive reader and its two sheets need.
     *
     * A nested class rather than thirty more fields on this one, because
     * [UiStringsMore] was already at the JVM's limit: a data class with 256
     * constructor parameters compiles cleanly and then fails at *runtime* with
     * `ClassFormatError: Too many arguments in method signature`, raised by the
     * class loader rather than the compiler - so it would only have surfaced in
     * the Robolectric screenshot tests, never in a build.
     */
    val reader: ReaderStrings = ReaderStrings(),
    /**
     * The notification texts. See [NotificationStrings] for why they are separate — the
     * short version is that `UiStringsMore` ran out of method signature.
     */
    val notifications: NotificationStrings = NotificationStrings(),
    val readingOptions: String = "Reading options",
    val selectVerse: String = "Select verse",
    val translationCreditLine: String = "English — Saheeh International",
)

val EnglishStrings = UiStrings(
    appName = "SALAH",
    navToday = "Today",
    navPrayer = "Prayer",
    navQuran = "Quran",
    navQibla = "Qibla",
    navSettings = "Settings",

    continueReading = "CONTINUE READING",
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
    prayerTimesHeader = "PRAYER TIMES",
    vigilsAndNightPeriods = "VIGILS & NIGHT PERIODS",
    imsakTitle = "Imsak (10 min before Fajr)",
    midnightTitle = "Islamic Midnight",
    lastThirdTitle = "Last Third of the Night (Tahajjud)",
    todayBtn = "Today",

    kaabaDistance = "Distance to Kaaba",
    trueNorth = "True North",
    magneticNorth = "Magnetic North",

    meccan = "Meccan",
    medinan = "Medinan",

    languageLabel = "Language",
    appThemeLabel = "App Theme",
    sectionPrayerCalc = "PRAYER TIMES & CALCULATION",
    locationLabel = "Location",
    methodLabel = "Calculation Method",
    madhhabLabel = "Asr Juristic Madhhab",
    adjustmentsLabel = "Manual Minute Adjustments",
    hijriCalibrationLabel = "Hijri Date Calibration",
    adhanCallLabel = "Adhan Call at Prayer Time",
    prePrayerReminderLabel = "Pre-Prayer Reminder Timing",
    adhanSoundLabel = "Adhan Recitation Voice",
    adhanVolumeLabel = "Adhan Volume & Audio Tester",
    sectionQuran = "THE NOBLE QURAN & RECITATION",
    riwayahLabel = "Riwāyāt (Recitation Tradition)",
    scriptStyleLabel = "Quran Script Style",
    reciterLabel = "Audio Reciter",
    translationLabel = "Translation & Exegesis",
    arabicTextSizeLabel = "Quran Arabic Text Size",
    autoMasjidModeLabel = "Auto Masjid Silence Mode",
    sectionSystemDiagnostics = "SYSTEM & DIAGNOSTICS",
    compassDiagnosticsLabel = "Compass Sensors & Diagnostics",
    networkSyncLabel = "Network Synchronization & Source",
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

    continueReading = "متابعة التلاوة",
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
    prayerTimesHeader = "مواقيت الصلاة",
    vigilsAndNightPeriods = "قيام الليل وأوقات السحر",
    imsakTitle = "الإمساك (١٠ دقائق قبل الفجر)",
    midnightTitle = "منتصف الليل الشرعي",
    lastThirdTitle = "الثلث الأخير من الليل (التهجد)",
    todayBtn = "اليوم",

    kaabaDistance = "المسافة إلى الكعبة",
    trueNorth = "الشمال الحقيقي",
    magneticNorth = "الشمال المغناطيسي",

    meccan = "مكية",
    medinan = "مدنية",

    languageLabel = "اللغة",
    appThemeLabel = "مظهر التطبيق",
    sectionPrayerCalc = "مواقيت الصلاة والحساب",
    locationLabel = "الموقع الجغرافي",
    methodLabel = "طريقة الحساب",
    madhhabLabel = "المذهب الفقهي (العصر)",
    adjustmentsLabel = "تعديل الدقائق يدوياً",
    hijriCalibrationLabel = "معايرة التاريخ الهجري",
    adhanCallLabel = "نداء الأذان عند دخول الوقت",
    prePrayerReminderLabel = "تذكير قبل الصلاة",
    adhanSoundLabel = "صوت المؤذن",
    adhanVolumeLabel = "مستوى الصوت وتجربة الأذان",
    sectionQuran = "القرآن الكريم والتلاوة",
    riwayahLabel = "الرواية القرآنية",
    scriptStyleLabel = "رسم المصحف الشريف",
    reciterLabel = "القارئ الصوتي",
    translationLabel = "الترجمة والتفسير",
    arabicTextSizeLabel = "حجم الخط القرآني",
    autoMasjidModeLabel = "الوضع الصامت التلقائي للصلاة",
    sectionSystemDiagnostics = "النظام والمستشعرات",
    compassDiagnosticsLabel = "مستشعرات البوصلة والتشخيص",
    networkSyncLabel = "المزامنة والمصدر",
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
reader = ReaderStrings(
        openIndex = "فتح الفهرس",
        immersiveMode = "وضع الغمر",
        exitImmersive = "الخروج من وضع الغمر",
        saveThisLocation = "حفظ هذه الآية",  // verify
        layoutPerPage = "صفحة بصفحة",  // verify
        layoutContinuousSurah = "سورة متصلة",  // verify
        scrollDirection = "اتجاه التمرير",
        previousPage = "الصفحة السابقة",
        nextPage = "الصفحة التالية",
        perVerseTitle = "إبراز كل آية",  // verify
        perVerseDescription = "وضع الدراسة: لكل آية مرجعها وإجراءاتها، تظهر عند تحديدها.",  // verify
        scrollVertical = "رأسي",
        scrollHorizontal = "أفقي",
        backgroundColour = "الخلفية",
        backgroundDefault = "افتراضي التطبيق",
        pinchBehaviour = "ماذا يفعل القرص",
        pinchZoomView = "تكبير العرض",
        pinchTextSize = "تغيير حجم النص",
        arabicTextSize = "حجم النص العربي",
        translationSize = "حجم الترجمة",
        quranFont = "خط المصحف",
        showTranslationLabelShort = "إظهار الترجمة",  // verify
        indexSurahs = "السور",  // verify
        indexSaved = "المحفوظات",
        indexSearch = "البحث",
        emptySavedTitle = "لا توجد محفوظات بعد",  // verify
        emptySavedMessage = "احفظ آية أثناء القراءة وستجدها هنا.",  // verify
        headingLabel = "العنوان",
        mushaf = "المصحف",  // verify
        changeInReader = "غيّر هذا من خيارات القراءة في المصحف.",
        pageAnnouncement = "صفحة %1\$d، جزء %2\$d، سورة %3\$s، %4\$s، من %5\$s إلى %6\$s",
        blockAnnouncement = "%1\$s إلى %2\$s، %3\$s، صفحة %4\$d",
        verseCountOne = "آية واحدة",
        verseCountMany = "%d آية",
        rangeTo = "إلى ",
        paperRose = "وردي",
        paperApricot = "برتقال",
        paperSand = "رملي",
        paperSage = "أخضر زمردي",
        paperMist = "ضبابي",
        paperIndigo = "نيلي",
        paperLilac = "أرجواني",
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
        playVerse = "تشغيل الآية",
        pauseVerse = "إيقاف مؤقت",
        bookmarkVerse = "أضف علامة",
        removeBookmark = "إزالة العلامة",
        copyVerse = "نسخ الآية",
        shareVerse = "مشاركة الآية",
        verseCopied = "تم نسخ الآية",
        textSize = "حجم النص",
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
        actionChange = "تغيير",
        adhanVolume = "مستوى الصوت", // verify
        alertSilentReminder = "تذكير صامت", // verify
        allSurahsLabel = "جميع السور", // verify
        ambientField = "المجال المحيط",
        appliedAdjustments = "التعديلات اليدوية", // verify
        audioSourceLabel = "صوت التلاوة", // verify
        audioStreamedNotCached = "يُبثّ ولا يُخزَّن على الجهاز", // verify
        autoSilenceDurationLabel = "مدة الكتم التلقائي", // verify
        calibrationMessage = "حرّك الجهاز على شكل الرقم ثمانية عدة مرات لتهدئة المستشعر.",
        chooseAdhanSound = "صوت الأذان", // verify
        chooseAdjustments = "تعديلات بالدقائق", // verify
        chooseHijriOffset = "إزاحة التاريخ الهجري", // verify
        chooseLanguage = "اللغة",
        chooseMadhhab = "حساب العصر", // verify
        chooseMethod = "طريقة الحساب", // verify
        choosePrePrayerOffset = "تذكير قبل الصلاة", // verify
        chooseReciter = "القارئ", // verify
        chooseScript = "الخط", // verify
        chooseTheme = "المظهر",
        computedOnDevice = "يُحسب على هذا الجهاز", // verify
        reschedulePrayers = "إعادة ضبط تنبيهات الصلاة",
        corpusSummary = "114 سورة · 30 جزءًا · 6236 آية", // verify
        daysShort = "يوم", // verify
        gpsCached = "تم حفظ موقع GPS",
        hizbOf = "الحزب %d", // verify
        hizbWord = "الحزب", // verify
        illumination = "الإضاءة",
        juzOf = "الجزء %d", // verify
        juzWord = "الجزء", // verify
        loadingQuranMessage = "جارٍ تحضير النص. إذا لم ينتهِ، عُد واختر السورة مجددًا.",
        madhhabLabelShort = "طريقة العصر", // verify
        methodology = "الطريقة", // verify
        minutesShort = "د", // verify
        nowReading = "يُقرأ الآن", // verify
        pageWord = "صفحة", // verify
        perPrayerModes = "تنبيه لكل صلاة", // verify
        prePrayerDisabled = "مغلق", // verify
        privacyNote = "تُحسب أوقات الصلاة واتجاه القبلة والقرآن على هذا الجهاز. لا يُرفع أي شيء.",
        privacyOffline = "يعمل دون اتصال بالكامل",
        privacyNoAds = "بدون إعلانات",
        privacyNoAnalytics = "بدون تحليلات أو جمع بيانات",
        privacyNoAccount = "بدون حساب أو تسجيل دخول",
        privacyFree = "مجاني ومفتوح المصدر",
        readingOptions = "خيارات القراءة", // verify
        recitingLabel = "يتلو", // verify
        resetAllConfirmMessage = "تعود إعدادات الحساب والتنبيه والعرض إلى قيمها الافتراضية. تبقى آياتك المحفوظة وسجل صلاتك.",
        resetAllConfirmTitle = "إعادة ضبط كل الإعدادات؟",
        resetAllLabel = "إعادة ضبط كل الإعدادات",
        searchHintMessage = "ابحث عن سورة بالاسم أو المعنى، أو عن آية بنصها العربي أو الإنجليزي.", // verify
        searchHintTitle = "البحث في القرآن", // verify
        searchSurahsAndVerses = "البحث في السور والآيات", // verify
        selectLayoutTitle = "تخطيط القراءة", // verify
        selectSurah = "اختر السورة", // verify
        selectVerse = "اختر الآية", // verify
        selectedCity = "المدينة المحددة",
        sensorAccuracy = "دقة المستشعر",
        showTranslation = "إظهار الترجمة", // verify
        silentModeLabel = "كتم كل التنبيهات", // verify
        stopAudio = "إيقاف", // verify
        storageLabel = "التخزين",
        sunAligned = "الشمس شبه متجهة نحو القبلة. استقبلها للتأكد.", // verify
        sunAltitude = "الشمس",
        sunAltitudeValue = "ارتفاع الشمس",
        sunAzimuth = "اتجاه الشمس",
        sunBelowHorizon = "الشمس تحت الأفق، لذا لا يمكن استخدامها كمرجع الآن.",
        sunToTheLeft = "القبلة نحو %d° يسار الشمس.", // verify
        sunToTheRight = "القبلة نحو %d° يمين الشمس.", // verify
        surahsFound = "%d سورة", // verify
        testSound = "تجربة الصوت", // verify
        themeDark = "داكن",
        themeLight = "فاتح",
        themeSystem = "مطابقة النظام",
        timeFormat24hLabel = "الوقت بنظام 24 ساعة", // verify
        translationCreditLine = "الإنجليزية — صحيح إنترناشيونال", // verify
        verseCount = "%d آية", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d آية", // verify
        searchShowingFirst = "عرض أول %d",
        versesLabel = "آيات", // verify
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
            notifSilentFor = "الوضع الصامت مُفعّل لـ %1\$s",
            notifVibrateAlert = "تنبيه بالاهتزاز · دخل وقت %1\$s",
            notifTakbeerAlert = "تنبيه التكبير · حان وقت %1\$s",
            notifChimeAlert = "تنبيه الجرس اللطيف · حان وقت %1\$s",
            notifPrayerArrived = "حان وقت صلاة %1\$s (%2\$s)",
            notifEnterPrayer = "ادخل الصلاة واستقبل الكعبة المشرفة (%1\$s).",
            notifSilenceAction = "كتم",
            notifMarkPrayed = "تمّت الصلاة",
            notifPrePrayerTitle = "%1\$s بعد %2\$d دقيقة",
            notifPrePrayerText = "يبدأ %1\$s في %2\$s · استعد للصلاة",
            notifAdhanInProgress = "جارٍ الأذان",
            notifAdhanInProgressBody = "يتم تشغيل تنبيه الصلاة",
        ),    ),
)

val FrenchStrings = UiStrings(

    appName = "SALAH",
    navToday = "Aujourd'hui",
    navPrayer = "Prière",
    navQuran = "Coran",
    navQibla = "Qibla",
    navSettings = "Paramètres",

    continueReading = "CONTINUER LA LECTURE",
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
    prayerTimesHeader = "HORAIRES DE PRIÈRE",
    vigilsAndNightPeriods = "VEILLES & PÉRIODES DE NUIT",
    imsakTitle = "Imsak (10 min avant Fajr)",
    midnightTitle = "Minuit islamique",
    lastThirdTitle = "Dernier tiers de la nuit (Tahajjud)",
    todayBtn = "Aujourd'hui",

    kaabaDistance = "Distance à la Kaaba",
    trueNorth = "Nord géographique",
    magneticNorth = "Nord magnétique",

    meccan = "Mecquoise",
    medinan = "Médinoise",

    languageLabel = "Langue",
    appThemeLabel = "Thème de l'application",
    sectionPrayerCalc = "HORAIRES & CALCULS",
    locationLabel = "Emplacement",
    methodLabel = "Méthode de calcul",
    madhhabLabel = "École juridique (Asr)",
    adjustmentsLabel = "Ajustements manuels (minutes)",
    hijriCalibrationLabel = "Calibrage calendrier hégirien",
    adhanCallLabel = "Appel à la prière à l'heure",
    prePrayerReminderLabel = "Rappel avant la prière",
    adhanSoundLabel = "Voix de l'Adhan",
    adhanVolumeLabel = "Volume et test audio de l'Adhan",
    sectionQuran = "LE SAINT CORAN & RÉCITATION",
    riwayahLabel = "Riwāyah (Tradition de récitation)",
    scriptStyleLabel = "Style de calligraphie coranique",
    reciterLabel = "Récitateur audio",
    translationLabel = "Traduction & Exégèse",
    arabicTextSizeLabel = "Taille du texte arabe",
    autoMasjidModeLabel = "Mode silencieux auto prière",
    sectionSystemDiagnostics = "SYSTÈME & CAPTEURS",
    compassDiagnosticsLabel = "Capteurs boussole & diagnostic",
    networkSyncLabel = "Synchronisation réseau & source",
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
reader = ReaderStrings(
        openIndex = "Ouvrir l’index",
        immersiveMode = "Mode immersif",
        exitImmersive = "Quitter le mode immersif",
        saveThisLocation = "Enregistrer ce verset",  // verify
        layoutPerPage = "Page par page",  // verify
        layoutContinuousSurah = "Sourate continue",  // verify
        scrollDirection = "Sens de défilement",
        previousPage = "Page précédente",
        nextPage = "Page suivante",
        perVerseTitle = "Isoler chaque verset",  // verify
        perVerseDescription = "Mode étude : chaque verset a sa référence et ses actions, révélées à la sélection.",  // verify
        scrollVertical = "Vertical",
        scrollHorizontal = "Horizontal",
        backgroundColour = "Arrière-plan",
        backgroundDefault = "Réglage de l’application",
        pinchBehaviour = "Le pincement",
        pinchZoomView = "Agrandir la vue",
        pinchTextSize = "Modifier la taille du texte",
        arabicTextSize = "Taille du texte arabe",
        translationSize = "Taille de la traduction",
        quranFont = "Police du Coran",
        showTranslationLabelShort = "Afficher la traduction",  // verify
        indexSurahs = "Sourates",  // verify
        indexSaved = "Enregistrés",
        indexSearch = "Rechercher",
        emptySavedTitle = "Rien d’enregistré pour l’instant",  // verify
        emptySavedMessage = "Enregistrez un verset en lisant et il vous attendra ici.",  // verify
        headingLabel = "Titre",
        mushaf = "Mushaf",  // verify
        changeInReader = "À modifier dans les options de lecture du Coran.",
        pageAnnouncement = "Page %1\$d, juz' %2\$d, sourate %3\$s, %4\$s, de %5\$s à %6\$s",
        blockAnnouncement = "%1\$s à %2\$s, %3\$s, page %4\$d",
        verseCountOne = "%d verset",
        verseCountMany = "%d versets",
        rangeTo = " à ",
        paperRose = "Rose",
        paperApricot = "Abricot",
        paperSand = "Sable",
        paperSage = "Olive",
        paperMist = "Brume",
        paperIndigo = "Indigo",
        paperLilac = "Lila",
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
        playVerse = "Lire le verset",
        pauseVerse = "Mettre en pause",
        bookmarkVerse = "Ajouter un signet",
        removeBookmark = "Retirer le signet",
        copyVerse = "Copier le verset",
        shareVerse = "Partager le verset",
        verseCopied = "Verset copié",
        textSize = "Taille du texte",
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
        actionChange = "Modifier",
        adhanVolume = "Volume", // verify
        alertSilentReminder = "Rappel silencieux", // verify
        allSurahsLabel = "Toutes les sourates", // verify
        ambientField = "Champ ambiant",
        appliedAdjustments = "Ajustements manuels", // verify
        audioSourceLabel = "Audio de récitation", // verify
        audioStreamedNotCached = "Diffusé, non stocké sur l'appareil", // verify
        autoSilenceDurationLabel = "Durée du silence automatique", // verify
        calibrationMessage = "Déplacez l'appareil en forme de huit plusieurs fois pour stabiliser le capteur.",
        chooseAdhanSound = "Son de l'adhan", // verify
        chooseAdjustments = "Ajustements en minutes", // verify
        chooseHijriOffset = "Décalage de la date hégirienne", // verify
        chooseLanguage = "Langue",
        chooseMadhhab = "Calcul de Asr", // verify
        chooseMethod = "Méthode de calcul", // verify
        choosePrePrayerOffset = "Rappel avant la prière", // verify
        chooseReciter = "Récitateur", // verify
        chooseScript = "Écriture", // verify
        chooseTheme = "Thème",
        computedOnDevice = "Calculé sur cet appareil", // verify
        reschedulePrayers = "Réarmer les alarmes de prière",
        corpusSummary = "114 sourates · 30 juz · 6 236 versets", // verify
        daysShort = "j", // verify
        gpsCached = "GPS mis en cache",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Illumination",
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        loadingQuranMessage = "Préparation du texte. Si cela ne se termine pas, revenez en arrière et choisissez à nouveau la sourate.",
        madhhabLabelShort = "Méthode de Asr", // verify
        methodology = "Méthode", // verify
        minutesShort = "min", // verify
        nowReading = "En cours de lecture", // verify
        pageWord = "Page", // verify
        perPrayerModes = "Alerte par prière", // verify
        prePrayerDisabled = "Désactivé", // verify
        privacyNote = "Les horaires de prière, la direction de la qibla et le Coran sont calculés sur cet appareil. Rien n'est envoyé.",
        privacyOffline = "Fonctionne entièrement hors ligne",
        privacyNoAds = "Pas de publicité",
        privacyNoAnalytics = "Pas d'analyse ni de collecte de données",
        privacyNoAccount = "Pas de compte ni de connexion",
        privacyFree = "Gratuit et open source",
        readingOptions = "Options de lecture", // verify
        recitingLabel = "En récitation", // verify
        resetAllConfirmMessage = "Les paramètres de calcul, d'alerte et d'affichage reviennent à leurs valeurs par défaut. Vos versets enregistrés et votre journal de prière sont conservés.",
        resetAllConfirmTitle = "Réinitialiser tous les paramètres ?",
        resetAllLabel = "Réinitialiser tous les paramètres",
        searchHintMessage = "Trouvez une sourate par nom ou par sens, ou un verset par son texte arabe ou anglais.", // verify
        searchHintTitle = "Rechercher dans le Coran", // verify
        searchSurahsAndVerses = "Rechercher des sourates et des versets", // verify
        selectLayoutTitle = "Disposition de lecture", // verify
        selectSurah = "Sélectionner la sourate", // verify
        selectVerse = "Sélectionner le verset", // verify
        selectedCity = "Ville sélectionnée",
        sensorAccuracy = "Précision du capteur",
        showTranslation = "Afficher la traduction", // verify
        silentModeLabel = "Silence pour toutes les alertes", // verify
        stopAudio = "Arrêter", // verify
        storageLabel = "Stockage",
        sunAligned = "Le soleil est presque dans la direction de la qibla. Tournez-vous vers lui pour confirmer.", // verify
        sunAltitude = "Soleil",
        sunAltitudeValue = "Hauteur du soleil",
        sunAzimuth = "Direction du soleil",
        sunBelowHorizon = "Le soleil est sous l'horizon, il ne peut donc pas servir de référence pour le moment.",
        sunToTheLeft = "La qibla est à environ %d° à gauche du soleil.", // verify
        sunToTheRight = "La qibla est à environ %d° à droite du soleil.", // verify
        surahsFound = "%d sourates", // verify
        testSound = "Tester le son", // verify
        themeDark = "Sombre",
        themeLight = "Clair",
        themeSystem = "Suivre le système",
        timeFormat24hLabel = "Heure sur 24 heures", // verify
        translationCreditLine = "Anglais — Saheeh International", // verify
        verseCount = "%d versets", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d versets", // verify
        searchShowingFirst = "affichage des %d premiers",
        versesLabel = "Versets", // verify
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
            notifSilentFor = "Mode silencieux actif pour %1\$s",
            notifVibrateAlert = "Alerte vibreur · %1\$s est entré",
            notifTakbeerAlert = "Alerte takbeer · C'est l'heure de %1\$s",
            notifChimeAlert = "Alerte cloche douce · C'est l'heure de %1\$s",
            notifPrayerArrived = "L'heure de la prière %1\$s est arrivée (%2\$s)",
            notifEnterPrayer = "Entrez en prière et tournez-vous vers la Kaaba (%1\$s).",
            notifSilenceAction = "Silence",
            notifMarkPrayed = "Marquer comme prié",
            notifPrePrayerTitle = "%1\$s dans %2\$d minutes",
            notifPrePrayerText = "%1\$s commence à %2\$s · Préparez-vous à prier",
            notifAdhanInProgress = "Adhan en cours",
            notifAdhanInProgressBody = "L'alerte de prière est en cours de lecture",
        ),    ),
)

val IndonesianStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hari Ini",
    navPrayer = "Jadwal",
    navQuran = "Al-Qur'an",
    navQibla = "Kiblat",
    navSettings = "Pengaturan",

    continueReading = "LANJUTKAN MEMBACA",
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
    prayerTimesHeader = "JADWAL SHOLAT",
    vigilsAndNightPeriods = "WAKTU MALAM & TAHAJUD",
    imsakTitle = "Imsak (10 mnt sebelum Subuh)",
    midnightTitle = "Tengah Malam Islam",
    lastThirdTitle = "Sepertiga Malam Terakhir (Tahajud)",
    todayBtn = "Hari Ini",

    kaabaDistance = "Jarak ke Ka'bah",
    trueNorth = "Utara Sejati",
    magneticNorth = "Utara Magnetik",

    meccan = "Makkiyyah",
    medinan = "Madaniyyah",

    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    sectionPrayerCalc = "JADWAL SHOLAT & PERHITUNGAN",
    locationLabel = "Lokasi",
    methodLabel = "Metode Perhitungan",
    madhhabLabel = "Mazhab Fikih (Ashar)",
    adjustmentsLabel = "Penyesuaian Menit Manual",
    hijriCalibrationLabel = "Kalibrasi Penanggalan Hijriah",
    adhanCallLabel = "Kumandangkan Adzan Tepat Waktu",
    prePrayerReminderLabel = "Pengingat Sebelum Sholat",
    adhanSoundLabel = "Suara Muadzin Adzan",
    adhanVolumeLabel = "Volume & Uji Suara Adzan",
    sectionQuran = "AL-QUR'AN & TILAWAH",
    riwayahLabel = "Riwayat Bacaan",
    scriptStyleLabel = "Gaya Rasm Al-Qur'an",
    reciterLabel = "Qari Audio",
    translationLabel = "Terjemahan & Tafsir",
    arabicTextSizeLabel = "Ukuran Huruf Arab",
    autoMasjidModeLabel = "Senyap Otomatis Saat Sholat",
    sectionSystemDiagnostics = "SISTEM & DIAGNOSTIK",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Sinkronisasi Jaringan & Sumber",
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
reader = ReaderStrings(
        openIndex = "Buka indeks",
        immersiveMode = "Mode imersif",
        exitImmersive = "Keluar dari mode imersif",
        saveThisLocation = "Simpan ayat ini",  // verify
        layoutPerPage = "Per halaman",  // verify
        layoutContinuousSurah = "Surah_continuous",  // verify
        scrollDirection = "Arah gulir",
        previousPage = "Halaman sebelumnya",
        nextPage = "Halaman berikutnya",
        perVerseTitle = "Pisahkan setiap ayat",  // verify
        perVerseDescription = "Mode belajar: setiap ayat punya referensi dan aksinya sendiri, muncul saat dipilih.",  // verify
        scrollVertical = "Vertikal",
        scrollHorizontal = "Horizontal",
        backgroundColour = "Latar belakang",
        backgroundDefault = "Bawaan aplikasi",
        pinchBehaviour = "Pinch melakukan",
        pinchZoomView = "Perbesar tampilan",
        pinchTextSize = "Ubah ukuran teks",
        arabicTextSize = "Ukuran teks Arab",
        translationSize = "Ukuran terjemahan",
        quranFont = "Font Quran",
        showTranslationLabelShort = "Tampilkan terjemahan",  // verify
        indexSurahs = "Surah",  // verify
        indexSaved = "Tersimpan",
        indexSearch = "Cari",
        emptySavedTitle = "Belum ada yang tersimpan",  // verify
        emptySavedMessage = "Simpan ayat saat membaca dan akan menunggu di sini.",  // verify
        headingLabel = "Judul",
        mushaf = "Mushaf",  // verify
        changeInReader = "Ubah ini di Opsi Bacaan Al-Qur’an.",
        pageAnnouncement = "Halaman %1\$d, juz' %2\$d, surah %3\$s, %4\$s, dari %5\$s ke %6\$s",
        blockAnnouncement = "%1\$s hingga %2\$s, %3\$s, halaman %4\$d",
        verseCountOne = "%d ayat",
        verseCountMany = "%d ayat",
        rangeTo = " sampai ",
        paperRose = "Mawar",
        paperApricot = "Aprikot",
        paperSand = "Pasir",
        paperSage = "Lumut",
        paperMist = "Kabut",
        paperIndigo = "Indigo",
        paperLilac = "Lila",
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
        playVerse = "Putar ayat",
        pauseVerse = "Jeda",
        bookmarkVerse = "Tandai",
        removeBookmark = "Hapus tanda",
        copyVerse = "Salin ayat",
        shareVerse = "Bagikan ayat",
        verseCopied = "Ayat disalin",
        textSize = "Ukuran teks",
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
        actionChange = "Ubah",
        adhanVolume = "Volume", // verify
        alertSilentReminder = "Pengingat senyap", // verify
        allSurahsLabel = "Semua surah", // verify
        ambientField = "Medan ambient",
        appliedAdjustments = "Penyesuaian manual", // verify
        audioSourceLabel = "Audio bacaan", // verify
        audioStreamedNotCached = "Distreamkan, tidak disimpan di perangkat", // verify
        autoSilenceDurationLabel = "Durasi senyap otomatis", // verify
        calibrationMessage = "Gerakkan perangkat menyerupai angka delapan beberapa kali agar sensor stabil.",
        chooseAdhanSound = "Suara adzan", // verify
        chooseAdjustments = "Penyesuaian menit", // verify
        chooseHijriOffset = "Offset tanggal Hijriah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Perhitungan Asr", // verify
        chooseMethod = "Metode perhitungan", // verify
        choosePrePrayerOffset = "Pengingat sebelum shalat", // verify
        chooseReciter = "Qori", // verify
        chooseScript = "Skrip", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Dihitung di perangkat ini", // verify
        reschedulePrayers = "Aktifkan kembali alarm salat",
        corpusSummary = "114 surah · 30 juz · 6.236 ayat", // verify
        daysShort = "hr", // verify
        gpsCached = "GPS di-cache",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Iluminasi",
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        loadingQuranMessage = "Menyiapkan teks. Jika tidak selesai, kembali dan pilih surah lagi.",
        madhhabLabelShort = "Metode Asr", // verify
        methodology = "Metode", // verify
        minutesShort = "mnt", // verify
        nowReading = "Sedang dibaca", // verify
        pageWord = "Halaman", // verify
        perPrayerModes = "Pengingat per shalat", // verify
        prePrayerDisabled = "Nonaktif", // verify
        privacyNote = "Waktu shalat, arah kiblat, dan Al-Qur'an semua dihitung di perangkat ini. Tidak ada yang diunggah.",
        privacyOffline = "Sepenuhnya offline",
        privacyNoAds = "Tanpa iklan",
        privacyNoAnalytics = "Tanpa analitik atau pengumpulan data",
        privacyNoAccount = "Tanpa akun atau masuk",
        privacyFree = "Gratis dan sumber terbuka",
        readingOptions = "Opsi bacaan", // verify
        recitingLabel = "Membaca", // verify
        resetAllConfirmMessage = "Pengaturan perhitungan, pengingat, dan tampilan kembali ke bawaan. Ayat tersimpan dan catatan shalat Anda tetap disimpan.",
        resetAllConfirmTitle = "Atur ulang semua pengaturan?",
        resetAllLabel = "Atur ulang semua pengaturan",
        searchHintMessage = "Cari surah berdasarkan nama atau makna, atau ayat berdasarkan teks Arab atau Inggris.", // verify
        searchHintTitle = "Cari di Al-Qur'an", // verify
        searchSurahsAndVerses = "Cari surah dan ayat", // verify
        selectLayoutTitle = "Tata letak bacaan", // verify
        selectSurah = "Pilih surah", // verify
        selectVerse = "Pilih ayat", // verify
        selectedCity = "Kota terpilih",
        sensorAccuracy = "Akurasi sensor",
        showTranslation = "Tampilkan terjemahan", // verify
        silentModeLabel = "Senyapkan semua pengingat", // verify
        stopAudio = "Hentikan", // verify
        storageLabel = "Penyimpanan",
        sunAligned = "Matahari hampir searah kiblat. Hadapi untuk memastikan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horison, sehingga tidak bisa dipakai sebagai acuan sekarang.",
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        surahsFound = "%d surah", // verify
        testSound = "Uji suara", // verify
        themeDark = "Gelap",
        themeLight = "Terang",
        themeSystem = "Ikuti sistem",
        timeFormat24hLabel = "Format 24 jam", // verify
        translationCreditLine = "Inggris — Saheeh International", // verify
        verseCount = "%d ayat", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayat", // verify
        searchShowingFirst = "menampilkan %d pertama",
        versesLabel = "Ayat", // verify
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
            notifSilentFor = "Mode Senyap aktif untuk %1\$s",
            notifVibrateAlert = "Peringatan getar · %1\$s telah masuk",
            notifTakbeerAlert = "Peringatan takbir · Waktunya %1\$s",
            notifChimeAlert = "Peringatan lonceng lembut · Waktunya %1\$s",
            notifPrayerArrived = "Waktu salat %1\$s telah tiba (%2\$s)",
            notifEnterPrayer = "Masuk salat dan menghadap Kaaba (%1\$s).",
            notifSilenceAction = "Diam",
            notifMarkPrayed = "Tandai sudah salat",
            notifPrePrayerTitle = "%1\$s dalam %2\$d menit",
            notifPrePrayerText = "%1\$s dimulai pukul %2\$s · Bersiap salat",
            notifAdhanInProgress = "Adzan sedang berlangsung",
            notifAdhanInProgressBody = "Peringatan salat sedang diputar",
        ),    ),
)

val TurkishStrings = UiStrings(

    appName = "SALAH",
    navToday = "Bugün",
    navPrayer = "Vakitler",
    navQuran = "Kur'an",
    navQibla = "Kıble",
    navSettings = "Ayarlar",

    continueReading = "OKUMAYA DEVAM ET",
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
    prayerTimesHeader = "NAMAZ VAKİTLERİ",
    vigilsAndNightPeriods = "TEHECCÜD & GECE VAKİTLERİ",
    imsakTitle = "İmsak (Fecr-i Sadık)",
    midnightTitle = "Şer'i Gece Yarısı",
    lastThirdTitle = "Gecenin Son Üçte Biri (Teheccüd)",
    todayBtn = "Bugün",

    kaabaDistance = "Kabe'ye Mesafe",
    trueNorth = "Gerçek Kuzey",
    magneticNorth = "Manyetik Kuzey",

    meccan = "Mekki",
    medinan = "Medeni",

    languageLabel = "Dil",
    appThemeLabel = "Uygulama Teması",
    sectionPrayerCalc = "NAMAZ VAKİTLERİ & HESAPLAMA",
    locationLabel = "Konum",
    methodLabel = "Hesaplama Yöntemi",
    madhhabLabel = "İkindi Mezhebi",
    adjustmentsLabel = "Manuel Dakika Düzeltmeleri",
    hijriCalibrationLabel = "Hicri Takvim Ayarı",
    adhanCallLabel = "Vaktinde Ezan Oku",
    prePrayerReminderLabel = "Vakit Öncesi Hatırlatıcı",
    adhanSoundLabel = "Müezzin Ezan Sesi",
    adhanVolumeLabel = "Ezan Sesi Düzeyi & Testi",
    sectionQuran = "KUR'AN-I KERİM & TİLAVET",
    riwayahLabel = "Kıraat Rivayeti",
    scriptStyleLabel = "Kur'an Hat / Yazı Stili",
    reciterLabel = "Sesli Kâri (Okuyucu)",
    translationLabel = "Meal & Tefsir",
    arabicTextSizeLabel = "Arapça Yazı Boyutu",
    autoMasjidModeLabel = "Namazda Otomatik Sessiz Mod",
    sectionSystemDiagnostics = "SİSTEM & SENSÖRLER",
    compassDiagnosticsLabel = "Pusula Sensörleri & Tanılama",
    networkSyncLabel = "Ağ Eşitlemesi & Kaynak",
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
reader = ReaderStrings(
        openIndex = "Dizinü aç",
        immersiveMode = "Odak modu",
        exitImmersive = "Odak modundan çık",
        saveThisLocation = "Bu ayeti kaydet",  // verify
        layoutPerPage = "Sayfa sayfa",  // verify
        layoutContinuousSurah = "Kesintisiz sûre",  // verify
        scrollDirection = "Kaydırma yönü",
        previousPage = "Önceki sayfa",
        nextPage = "Sonraki sayfa",
        perVerseTitle = "Her ayeti ayır",  // verify
        perVerseDescription = "Çalışma kipi: her ayetin kendi referansı ve işlemleri vardır, seçildiğinde görünür.",  // verify
        scrollVertical = "Dikey",
        scrollHorizontal = "Yatay",
        backgroundColour = "Arka plan",
        backgroundDefault = "Uygulama varsayılanı",
        pinchBehaviour = "Parmak hareketi",
        pinchZoomView = "Görünümü yakınlaştır",
        pinchTextSize = "Yazı boyutunu değiştir",
        arabicTextSize = "Arapça yazı boyutu",
        translationSize = "Çeviri boyutu",
        quranFont = "Kur’an yazı tipi",
        showTranslationLabelShort = "Çeviriyi göster",  // verify
        indexSurahs = "Sureler",  // verify
        indexSaved = "Kaydedilenler",
        indexSearch = "Ara",
        emptySavedTitle = "Henüz kaydedilen yok",  // verify
        emptySavedMessage = "Okurken bir ayeti kaydet, burada seni bekliyor olacak.",  // verify
        headingLabel = "Başlık",
        mushaf = "Mushaf",  // verify
        changeInReader = "Bunu Mushaf okuma seçeneklerinden değiştirebilirsin.",
        pageAnnouncement = "Sayfa %1\$d, cüz %2\$d, sure %3\$s, %4\$s, %5\$s'ten %6\$s'ya",
        blockAnnouncement = "%1\$s ile %2\$s arası, %3\$s, sayfa %4\$d",
        verseCountOne = "%d ayet",
        verseCountMany = "%d ayet",
        rangeTo = " - ",
        paperRose = "Gül",
        paperApricot = "Kayısı",
        paperSand = "Kum",
        paperSage = "Adaçayı",
        paperMist = "Sis",
        paperIndigo = "Çivit",
        paperLilac = "Leylak",
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
        playVerse = "Ayeti oynat",
        pauseVerse = "Duraklat",
        bookmarkVerse = "Yer imi ekle",
        removeBookmark = "Yer imini kaldır",
        copyVerse = "Ayeti kopyala",
        shareVerse = "Ayeti paylaş",
        verseCopied = "Ayet kopyalandı",
        textSize = "Yazı boyutu",
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
        actionChange = "Değiştir",
        adhanVolume = "Ses seviyesi", // verify
        alertSilentReminder = "Sessiz hatırlatma", // verify
        allSurahsLabel = "Tüm sureler", // verify
        ambientField = "Çevresel alan",
        appliedAdjustments = "Elle ayarlamalar", // verify
        audioSourceLabel = "Kıraat sesi", // verify
        audioStreamedNotCached = "Yayınlanıyor, cihazda saklanmıyor", // verify
        autoSilenceDurationLabel = "Otomatik sessizlik süresi", // verify
        calibrationMessage = "Sensörün dengelemesi için cihazı birkaç kez sekiz şeklinde hareket ettirin.",
        chooseAdhanSound = "Ezan sesi", // verify
        chooseAdjustments = "Dakika ayarlamaları", // verify
        chooseHijriOffset = "Hicri tarih farkı", // verify
        chooseLanguage = "Dil",
        chooseMadhhab = "Asr hesabı", // verify
        chooseMethod = "Hesap yöntemi", // verify
        choosePrePrayerOffset = "Namazdan önce hatırlatma", // verify
        chooseReciter = "Kari", // verify
        chooseScript = "Yazı", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Bu cihazda hesaplanır", // verify
        reschedulePrayers = "Namaz alarmlarını yeniden kur",
        corpusSummary = "114 sure · 30 cüz · 6.236 ayet", // verify
        daysShort = "g", // verify
        gpsCached = "GPS önbelleğe alındı",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Aydınlanma",
        juzOf = "Cüz %d", // verify
        juzWord = "Cüz", // verify
        loadingQuranMessage = "Metin hazırlanıyor. Bitmiyorsa geri dönün ve sureyi yeniden seçin.",
        madhhabLabelShort = "Asr yöntemi", // verify
        methodology = "Yöntem", // verify
        minutesShort = "dk", // verify
        nowReading = "Şu an okunuyor", // verify
        pageWord = "Sayfa", // verify
        perPrayerModes = "Namaz bazında uyarı", // verify
        prePrayerDisabled = "Kapalı", // verify
        privacyNote = "Namaz vakitleri, kıble yönü ve Kur'an bu cihazda hesaplanır. Hiçbir şey yüklenmez.",
        privacyOffline = "Tamamen çevrimdışı",
        privacyNoAds = "Reklam yok",
        privacyNoAnalytics = "Analiz veya veri toplama yok",
        privacyNoAccount = "Hesap veya giriş yok",
        privacyFree = "Ücretsiz ve açık kaynak",
        readingOptions = "Okuma seçenekleri", // verify
        recitingLabel = "Kıraat ediyor", // verify
        resetAllConfirmMessage = "Hesap, uyarı ve görünüm ayarları varsayılanlara döner. Kayıtlı ayetleriniz ve namaz kaydınız korunur.",
        resetAllConfirmTitle = "Tüm ayarlar sıfırlansın mı?",
        resetAllLabel = "Tüm ayarları sıfırla",
        searchHintMessage = "Sureyi adına veya anlamına göre, ayeti Arapça veya İngilizce metnine göre bulun.", // verify
        searchHintTitle = "Kur'an'da ara", // verify
        searchSurahsAndVerses = "Sure ve ayet ara", // verify
        selectLayoutTitle = "Okuma düzeni", // verify
        selectSurah = "Sure seç", // verify
        selectVerse = "Ayet seç", // verify
        selectedCity = "Seçilen şehir",
        sensorAccuracy = "Sensör doğruluğu",
        showTranslation = "Çeviriyi göster", // verify
        silentModeLabel = "Tüm uyarıları sessize al", // verify
        stopAudio = "Durdur", // verify
        storageLabel = "Depolama",
        sunAligned = "Güneş neredeyse kıble yönünde. Doğrulamak için ona dönün.", // verify
        sunAltitude = "Güneş",
        sunAltitudeValue = "Güneş yüksekliği",
        sunAzimuth = "Güneş yönü",
        sunBelowHorizon = "Güneş ufuk altında, bu yüzden şu an referans olarak kullanılamaz.",
        sunToTheLeft = "Kıble güneşin yaklaşık %d° solunda.", // verify
        sunToTheRight = "Kıble güneşin yaklaşık %d° sağında.", // verify
        surahsFound = "%d sure", // verify
        testSound = "Sesi test et", // verify
        themeDark = "Koyu",
        themeLight = "Açık",
        themeSystem = "Sisteme uy",
        timeFormat24hLabel = "24 saatlik format", // verify
        translationCreditLine = "İngilizce — Saheeh International", // verify
        verseCount = "%d ayet", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayet", // verify
        searchShowingFirst = "ilk %d gösteriliyor",
        versesLabel = "Ayetler", // verify
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
            notifSilentFor = "%1\$s için sessiz mod etkin",
            notifVibrateAlert = "Titreşim uyarısı · %1\$s girdi",
            notifTakbeerAlert = "Tekbir uyarısı · %1\$s vakti",
            notifChimeAlert = "Yumuşak zil uyarısı · %1\$s vakti",
            notifPrayerArrived = "%1\$s namaz vakti geldi (%2\$s)",
            notifEnterPrayer = "Namaza girip Kâ'be'ye yönelin (%1\$s).",
            notifSilenceAction = "Sessiz",
            notifMarkPrayed = "Namaz kılındı olarak işaretle",
            notifPrePrayerTitle = "%2\$d dakika sonra %1\$s",
            notifPrePrayerText = "%1\$s saat %2\$s'de başlıyor · Namaza hazırlanın",
            notifAdhanInProgress = "Ezan çalıyor",
            notifAdhanInProgressBody = "Namaz uyarısı çalıyor",
        ),    ),
)

val UrduStrings = UiStrings(

    appName = "صلاۃ",
    navToday = "آج",
    navPrayer = "نماز",
    navQuran = "قرآن",
    navQibla = "قبلہ",
    navSettings = "ترتیبات",

    continueReading = "تلاوت جاری رکھیں",
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
    prayerTimesHeader = "اوقاتِ نماز",
    vigilsAndNightPeriods = "تہجد اور رات کے اوقات",
    imsakTitle = "امساک (فجر سے ۱۰ منٹ قبل)",
    midnightTitle = "شرعی نصف شب",
    lastThirdTitle = "رات کا آخری تہائی حصہ (تہجد)",
    todayBtn = "آج",

    kaabaDistance = "کعبہ شریف کا فاصلہ",
    trueNorth = "حقیقی شمال",
    magneticNorth = "مقناطیسی شمال",

    meccan = "مکی",
    medinan = "مدنی",

    languageLabel = "زبان",
    appThemeLabel = "ایپ تھیم",
    sectionPrayerCalc = "اوقاتِ نماز اور حساب",
    locationLabel = "مقام",
    methodLabel = "طریقہ حساب",
    madhhabLabel = "فقہی مسلک (عصر)",
    adjustmentsLabel = "دستی منٹ ایڈجسٹمنٹ",
    hijriCalibrationLabel = "ہجری تاریخ کی ترتیب",
    adhanCallLabel = "وقت پر اذان کی پکار",
    prePrayerReminderLabel = "نماز سے قبل یاد دہانی",
    adhanSoundLabel = "مؤذن کی آواز",
    adhanVolumeLabel = "اذان کی آواز اور ٹیسٹ",
    sectionQuran = "قرآن مجید اور تلاوت",
    riwayahLabel = "قرآنی روایت",
    scriptStyleLabel = "قرآنی رسم الخط",
    reciterLabel = "آڈیو قاری",
    translationLabel = "ترجمہ و تفسیر",
    arabicTextSizeLabel = "عربی فونٹ سائز",
    autoMasjidModeLabel = "نماز کے دوران خودکار سائلنٹ",
    sectionSystemDiagnostics = "سسٹم اور سینسرز",
    compassDiagnosticsLabel = "قطب نما اور سینسر کی جانچ",
    networkSyncLabel = "نیٹ ورک سنک اور ماخذ",
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
reader = ReaderStrings(
        openIndex = "فہرست کھولیں",
        immersiveMode = "غمر شدہ موڈ",
        exitImmersive = "غمر شدہ موڈ چھوڑیں",
        saveThisLocation = "یہ آیت محفوظ کریں",  // verify
        layoutPerPage = "صفحہ بہ صفحہ",  // verify
        layoutContinuousSurah = "مسلسل سورہ",  // verify
        scrollDirection = "اسکرول کی سمت",
        previousPage = "پچھلا صفحہ",
        nextPage = "اگلا صفحہ",
        perVerseTitle = "ہر آیت الگ کریں",  // verify
        perVerseDescription = "مطالعے کا انداز: ہر آیت کا اپنا حوالہ اور اپنے اقدامات ہوتے ہیں، منتخب کرنے پر نظر آتے ہیں۔",  // verify
        scrollVertical = "عمودی",
        scrollHorizontal = "افقی",
        backgroundColour = "پس منظر",
        backgroundDefault = "ایپ کا ڈیفالٹ",
        pinchBehaviour = " pinch کیا کرتا ہے",
        pinchZoomView = "ویو زوم کریں",
        pinchTextSize = "متن کا حجم تبدیل کریں",
        arabicTextSize = "عربی متن کا حجم",
        translationSize = "ترجمے کا حجم",
        quranFont = "قرآنی فونٹ",
        showTranslationLabelShort = "ترجمہ دکھائیں",  // verify
        indexSurahs = "سورے",  // verify
        indexSaved = "محفوظات",
        indexSearch = "تلاش",
        emptySavedTitle = "ابھی کچھ محفوظ نہیں",  // verify
        emptySavedMessage = "پڑھتے ہوئے کوئی آیت محفوظ کریں، وہ یہاں انتظار کرے گی۔",  // verify
        headingLabel = "عنوان",
        mushaf = "مصحف",  // verify
        changeInReader = "یہ قرآن کی تلاوت کے اختیارات میں تبدیل کریں۔",
        pageAnnouncement = "صفحہ %1\$d، پارہ %2\$d، سورہ %3\$s، %4\$s، %5\$s سے %6\$s تک",
        blockAnnouncement = "%1\$s سے %2\$s تک، %3\$s، صفحہ %4\$d",
        verseCountOne = "ایک آیت",
        verseCountMany = "%d آیت",
        rangeTo = " سے ",
        paperRose = "گلابی",
        paperApricot = "نارنگی",
        paperSand = "سنترہ",
        paperSage = "ہرا",
        paperMist = "دھند",
        paperIndigo = "نیل",
        paperLilac = "بنفشی",
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
        playVerse = "آیت چلائیں",
        pauseVerse = "روکیں",
        bookmarkVerse = "نشان لگائیں",
        removeBookmark = "نشان ہٹائیں",
        copyVerse = "آیت کاپی کریں",
        shareVerse = "آیت شیئر کریں",
        verseCopied = "آیت کاپی ہو گئی",
        textSize = "متن کا حجم",
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
        actionChange = "تبدیل کریں",
        adhanVolume = "آواز کا درجہ", // verify
        alertSilentReminder = "خاموش یاددہانی", // verify
        allSurahsLabel = "تمام سورتیں", // verify
        ambientField = "ماحولی میدان",
        appliedAdjustments = "دستی ترتیبات", // verify
        audioSourceLabel = "تلاوت کی آواز", // verify
        audioStreamedNotCached = "سٹریم ہو رہا ہے، ڈیوائس پر محفوظ نہیں", // verify
        autoSilenceDurationLabel = "خاموشی کی مدت", // verify
        calibrationMessage = "سینسر کو مستحکم کرنے کے لیے ڈیوائس کو کئی بار ٹ شکل میں گھمائیں۔",
        chooseAdhanSound = "اذان کی آواز", // verify
        chooseAdjustments = "منٹ کی ترتیبات", // verify
        chooseHijriOffset = "ہجری تاریخ کا آفسیٹ", // verify
        chooseLanguage = "زبان",
        chooseMadhhab = "عصر کا حساب", // verify
        chooseMethod = "حساب کا طریقہ", // verify
        choosePrePrayerOffset = "نماز سے پہلے یاددہانی", // verify
        chooseReciter = "قاری", // verify
        chooseScript = "رسم الخط", // verify
        chooseTheme = "تھیم",
        computedOnDevice = "اسی ڈیوائس پر حساب ہوتا ہے", // verify
        reschedulePrayers = "نماز کے الرٹ ایڈام دوبارہ فعال کریں",
        corpusSummary = "114 سورتیں · 30 جزء · 6236 آیات", // verify
        daysShort = "دن", // verify
        gpsCached = "جی پی ایس کیش ہو گئی",
        hizbOf = "حزب %d", // verify
        hizbWord = "حزب", // verify
        illumination = "روشنی",
        juzOf = "جزء %d", // verify
        juzWord = "جزء", // verify
        loadingQuranMessage = "متن تیار ہو رہا ہے۔ اگر یہ مکمل نہ ہو تو واپس جائیں اور سورت دوبارہ منتخب کریں۔",
        madhhabLabelShort = "عصر کا طریقہ", // verify
        methodology = "طریقہ", // verify
        minutesShort = "منٹ", // verify
        nowReading = "ابھی پڑھا جا رہا ہے", // verify
        pageWord = "صفحہ", // verify
        perPrayerModes = "ہر نماز کے لیے الرٹ", // verify
        prePrayerDisabled = "بند", // verify
        privacyNote = "نماز کے اوقات، قبلہ کی سمت اور قرآن سب اسی ڈیوائس پر حساب ہوتے ہیں۔ کچھ اپلوڈ نہیں ہوتا۔",
        privacyOffline = "مکمل آف لائن",
        privacyNoAds = "کوئی اشتہار نہیں",
        privacyNoAnalytics = "کوئی تجزیہ یا ڈیٹا جمع کرنے نہیں",
        privacyNoAccount = "کوئی اکاؤنٹ یا سائن ان نہیں",
        privacyFree = "مفت اور اوپن سورس",
        readingOptions = "پڑھنے کے اختیارات", // verify
        recitingLabel = "تلاوت کر رہے ہیں", // verify
        resetAllConfirmMessage = "حساب، الرٹ اور ڈسپلے کی ترتیبات ڈیفالٹ پر آ جاتی ہیں۔ آپ کی محفوظ آیات اور نماز کا لاگ محفوظ رہتا ہے۔",
        resetAllConfirmTitle = "تمام ترتیبات دوبارہ ترتیب دیں؟",
        resetAllLabel = "تمام ترتیبات دوبارہ ترتیب دیں",
        searchHintMessage = "نام یا معنی کے لحاظ سے سورت تلاش کریں، یا عربی یا انگریزی متن سے آیت تلاش کریں۔", // verify
        searchHintTitle = "قرآن میں تلاش کریں", // verify
        searchSurahsAndVerses = "سورتیں اور آیات تلاش کریں", // verify
        selectLayoutTitle = "پڑھنے کی ترتیب", // verify
        selectSurah = "سورت منتخب کریں", // verify
        selectVerse = "آیت منتخب کریں", // verify
        selectedCity = "منتخب شہر",
        sensorAccuracy = "سینسر کی درستگی",
        showTranslation = "ترجمہ دکھائیں", // verify
        silentModeLabel = "تمام الرٹس خاموش کریں", // verify
        stopAudio = "روکیں", // verify
        storageLabel = "اسٹوریج",
        sunAligned = "سورج قبلہ کی سمت میں تقریباً ہے۔ تصدیق کے لیے اس کی طرف منہ کریں۔", // verify
        sunAltitude = "سورج",
        sunAltitudeValue = "سورج کی اونچائی",
        sunAzimuth = "سورج کی سمت",
        sunBelowHorizon = "سورج افق سے نیچے ہے، اس لیے اسے ابھی بطور حوالہ استعمال نہیں کیا جا سکتا۔",
        sunToTheLeft = "قبلہ سورج کے بائیں جانب تقریباً %d° ہے۔", // verify
        sunToTheRight = "قبلہ سورج کے دائیں جانب تقریباً %d° ہے۔", // verify
        surahsFound = "%d سورتیں", // verify
        testSound = "آواز آزمائیں", // verify
        themeDark = "ڈارک",
        themeLight = "لائٹ",
        themeSystem = "سسٹم کے مطابق",
        timeFormat24hLabel = "24 گھنٹے کا فارمیٹ", // verify
        translationCreditLine = "انگریزی — صحیح انٹرنیشنل", // verify
        verseCount = "%d آیات", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d آیات", // verify
        searchShowingFirst = "پہلے %d دکھایا جا رہا ہے",
        versesLabel = "آیات", // verify
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
            notifSilentFor = "%1\$s کے لیے خاموش حالت فعال",
            notifVibrateAlert = "کانپن اطلاع · %1\$s داخل ہو گیا",
            notifTakbeerAlert = "تکبیر اطلاع · %1\$s کا وقت",
            notifChimeAlert = "نرم گھنٹی اطلاع · %1\$s کا وقت",
            notifPrayerArrived = "%1\$s کے نماز کا وقت آ گیا (%2\$s)",
            notifEnterPrayer = "نماز میں جائیں اور کعبہ کی طرف منہ کریں (%1\$s)۔",
            notifSilenceAction = "خاموش",
            notifMarkPrayed = "نماز ادا شدہ نشان زد کریں",
            notifPrePrayerTitle = "%2\$d منٹ میں %1\$s",
            notifPrePrayerText = "%1\$s کی نماز %2\$s پر شروع ہوگی · نماز کی تیاری کریں",
            notifAdhanInProgress = "اذان جاری ہے",
            notifAdhanInProgressBody = "نماز کی اطلاع چل رہی ہے",
        ),    ),
)

val MalayStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hari Ini",
    navPrayer = "Solat",
    navQuran = "Al-Quran",
    navQibla = "Kiblat",
    navSettings = "Tetapan",

    continueReading = "TERUSKAN MEMBACA",
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
    prayerTimesHeader = "WAKTU SOLAT",
    vigilsAndNightPeriods = "WAKTU MALAM & TAHAJJUD",
    imsakTitle = "Imsak (10 minit sebelum Subuh)",
    midnightTitle = "Tengah Malam Islam",
    lastThirdTitle = "Sepertiga Malam Terakhir (Tahajjud)",
    todayBtn = "Hari Ini",

    kaabaDistance = "Jarak ke Kaabah",
    trueNorth = "Utara Benar",
    magneticNorth = "Utara Magnetik",

    meccan = "Makkiyyah",
    medinan = "Madaniyyah",

    languageLabel = "Bahasa",
    appThemeLabel = "Tema Aplikasi",
    sectionPrayerCalc = "WAKTU SOLAT & PENGIRAAN",
    locationLabel = "Lokasi",
    methodLabel = "Kaedah Pengiraan",
    madhhabLabel = "Mazhab Fiqh (Asar)",
    adjustmentsLabel = "Pelarasan Minit Manual",
    hijriCalibrationLabel = "Kalibrasi Tarikh Hijrah",
    adhanCallLabel = "Panggilan Azan Tepat Waktu",
    prePrayerReminderLabel = "Peringatan Sebelum Solat",
    adhanSoundLabel = "Suara Muazin Azan",
    adhanVolumeLabel = "Kelantangan & Ujian Audio Azan",
    sectionQuran = "AL-QURAN & TILAWAH",
    riwayahLabel = "Riwayat Bacaan",
    scriptStyleLabel = "Gaya Khat Rasm Al-Quran",
    reciterLabel = "Qari Audio",
    translationLabel = "Terjemahan & Tafsir",
    arabicTextSizeLabel = "Saiz Teks Arab",
    autoMasjidModeLabel = "Mod Senyap Automatik Semasa Solat",
    sectionSystemDiagnostics = "SISTEM & DIAGNOSTIK",
    compassDiagnosticsLabel = "Sensor Kompas & Diagnostik",
    networkSyncLabel = "Penyelarasan Rangkaian & Sumber",
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
reader = ReaderStrings(
        openIndex = "Buka indeks",
        immersiveMode = "Mod imersif",
        exitImmersive = "Keluar daripada mod imersif",
        saveThisLocation = "Simpan ayat ini",  // verify
        layoutPerPage = "Halaman demi halaman",  // verify
        layoutContinuousSurah = "Surah berterusan",  // verify
        scrollDirection = "Arah skrol",
        previousPage = "Halaman sebelumnya",
        nextPage = "Halaman seterusnya",
        perVerseTitle = "Pisahkan setiap ayat",  // verify
        perVerseDescription = "Modul belajar: setiap ayat mempunyai rujukan dan tindakannya sendiri, muncul apabila dipilih.",  // verify
        scrollVertical = "Menegak",
        scrollHorizontal = "Mendatar",
        backgroundColour = "Latar belakang",
        backgroundDefault = "Lalaian aplikasi",
        pinchBehaviour = "Cubit melakukan",
        pinchZoomView = "Besarkan paparan",
        pinchTextSize = "Ubah saiz teks",
        arabicTextSize = "Saiz teks Arab",
        translationSize = "Saiz terjemahan",
        quranFont = "Font Quran",
        showTranslationLabelShort = "Tunjukkan terjemahan",  // verify
        indexSurahs = "Surah",  // verify
        indexSaved = "Disimpan",
        indexSearch = "Cari",
        emptySavedTitle = "Belum ada yang disimpan",  // verify
        emptySavedMessage = "Simpan ayat semasa membaca dan ia akan menunggu di sini.",  // verify
        headingLabel = "Tajuk",
        mushaf = "Mushaf",  // verify
        changeInReader = "Ubah ini dalam pilihan bacaan Al-Quran.",
        pageAnnouncement = "Halaman %1\$d, juz' %2\$d, surah %3\$s, %4\$s, daripada %5\$s hingga %6\$s",
        blockAnnouncement = "%1\$s hingga %2\$s, %3\$s, halaman %4\$d",
        verseCountOne = "%d ayat",
        verseCountMany = "%d ayat",
        rangeTo = " sampai ",
        paperRose = "Merah",
        paperApricot = "Apricot",
        paperSand = "Pasir",
        paperSage = "Hijau",
        paperMist = "Kabus",
        paperIndigo = "Indigo",
        paperLilac = "Lila",
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
        playVerse = "Mainkan ayat",
        pauseVerse = "Jeda",
        bookmarkVerse = "Tanda",
        removeBookmark = "Buang tanda",
        copyVerse = "Salin ayat",
        shareVerse = "Kongsi ayat",
        verseCopied = "Ayat disalin",
        textSize = "Saiz teks",
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
        actionChange = "Tukar",
        adhanVolume = "Kelantangan", // verify
        alertSilentReminder = "Peringatan senyap", // verify
        allSurahsLabel = "Semua surah", // verify
        ambientField = "Medan sekitar",
        appliedAdjustments = "Pelarasan manual", // verify
        audioSourceLabel = "Audio bacaan", // verify
        audioStreamedNotCached = "Distreamkan, tidak disimpan pada peranti", // verify
        autoSilenceDurationLabel = "Tempoh senyap automatik", // verify
        calibrationMessage = "Gerakkan peranti membentuk angka lapan beberapa kali agar sensor stabil.",
        chooseAdhanSound = "Bunyi azan", // verify
        chooseAdjustments = "Pelarasan minit", // verify
        chooseHijriOffset = "Ofset tarih Hijrah", // verify
        chooseLanguage = "Bahasa",
        chooseMadhhab = "Pengiraan Asr", // verify
        chooseMethod = "Kaedah pengiraan", // verify
        choosePrePrayerOffset = "Peringatan sebelum solat", // verify
        chooseReciter = "Qori", // verify
        chooseScript = "Skrip", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Dikira pada peranti ini", // verify
        reschedulePrayers = "Hidupkan semula alarm solat",
        corpusSummary = "114 surah · 30 juz · 6,236 ayat", // verify
        daysShort = "hari", // verify
        gpsCached = "GPS dicache",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Pencahayaan",
        juzOf = "Juz %d", // verify
        juzWord = "Juz", // verify
        loadingQuranMessage = "Menyediakan teks. Jika tidak selesai, kembali dan pilih surah lagi.",
        madhhabLabelShort = "Kaedah Asr", // verify
        methodology = "Kaedah", // verify
        minutesShort = "min", // verify
        nowReading = "Sedang dibaca", // verify
        pageWord = "Muka surat", // verify
        perPrayerModes = "Peringatan setiap solat", // verify
        prePrayerDisabled = "Dimatikan", // verify
        privacyNote = "Waktu solat, arah kiblat, dan Al-Quran semua dikira pada peranti ini. Tiada apa yang dimuat naik.",
        privacyOffline = "Sepenuhnya luar talian",
        privacyNoAds = "Tiada iklan",
        privacyNoAnalytics = "Tiada analitik atau pengumpulan data",
        privacyNoAccount = "Tiada akaun atau log masuk",
        privacyFree = "Percuma dan sumber terbuka",
        readingOptions = "Pilihan bacaan", // verify
        recitingLabel = "Membaca", // verify
        resetAllConfirmMessage = "Tetapan pengiraan, peringatan, dan paparan kembali ke lalai. Ayat dan catatan solat anda disimpan.",
        resetAllConfirmTitle = "Set semula semua tetapan?",
        resetAllLabel = "Set semula semua tetapan",
        searchHintMessage = "Cari surah mengikut nama atau maksud, atau ayat mengikut teks Arab atau Inggeris.", // verify
        searchHintTitle = "Cari dalam Al-Quran", // verify
        searchSurahsAndVerses = "Cari surah dan ayat", // verify
        selectLayoutTitle = "Tata letak bacaan", // verify
        selectSurah = "Pilih surah", // verify
        selectVerse = "Pilih ayat", // verify
        selectedCity = "Bandar dipilih",
        sensorAccuracy = "Ketepatan sensor",
        showTranslation = "Tunjukkan terjemahan", // verify
        silentModeLabel = "Senyapkan semua peringatan", // verify
        stopAudio = "Berhenti", // verify
        storageLabel = "Storan",
        sunAligned = "Matahari hampir mengikut arah kiblat. Hadapkan diri untuk sahkan.", // verify
        sunAltitude = "Matahari",
        sunAltitudeValue = "Ketinggian matahari",
        sunAzimuth = "Arah matahari",
        sunBelowHorizon = "Matahari di bawah horizon, jadi ia tidak boleh dijadikan rujukan sekarang.",
        sunToTheLeft = "Kiblat kira-kira %d° di sebelah kiri matahari.", // verify
        sunToTheRight = "Kiblat kira-kira %d° di sebelah kanan matahari.", // verify
        surahsFound = "%d surah", // verify
        testSound = "Uji bunyi", // verify
        themeDark = "Gelap",
        themeLight = "Cerah",
        themeSystem = "Ikut sistem",
        timeFormat24hLabel = "Format 24 jam", // verify
        translationCreditLine = "Inggeris — Saheeh International", // verify
        verseCount = "%d ayat", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d ayat", // verify
        searchShowingFirst = "memaparkan %d yang pertama",
        versesLabel = "Ayat", // verify
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
            notifSilentFor = "Mod senyap diaktifkan untuk %1\$s",
            notifVibrateAlert = "Amaran getaran · %1\$s telah masuk",
            notifTakbeerAlert = "Amaran takbir · Waktunya %1\$s",
            notifChimeAlert = "Amaran loceng lembut · Waktunya %1\$s",
            notifPrayerArrived = "Waktu solat %1\$s telah tiba (%2\$s)",
            notifEnterPrayer = "Masuk solat dan menghadap Kaaba (%1\$s).",
            notifSilenceAction = "Senyap",
            notifMarkPrayed = "Tandakan sudah solat",
            notifPrePrayerTitle = "%1\$s dalam %2\$d minit",
            notifPrePrayerText = "%1\$s bermula pada %2\$s · Sediakan diri untuk solat",
            notifAdhanInProgress = "Azan sedang berjalan",
            notifAdhanInProgressBody = "Amaran solat sedang dimainkan",
        ),    ),
)

val BengaliStrings = UiStrings(

    appName = "সালাহ",
    navToday = "আজ",
    navPrayer = "নামাজ",
    navQuran = "কুরআন",
    navQibla = "কিবলা",
    navSettings = "সেটিংস",

    continueReading = "তেলাওয়াত চালিয়ে যান",
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
    prayerTimesHeader = "নামাজের ওয়াক্ত",
    vigilsAndNightPeriods = "তাহাজ্জুদ ও রাতের ওয়াক্ত",
    imsakTitle = "ইমসাক (ফজরের ১০ মিনিট আগে)",
    midnightTitle = "শরয়ী মধ্যরাত",
    lastThirdTitle = "রাতের শেষ তৃতীয়াংশ (তাহাজ্জুদ)",
    todayBtn = "আজ",

    kaabaDistance = "কাবা শরীফের দূরত্ব",
    trueNorth = "প্রকৃত উত্তর",
    magneticNorth = "চৌম্বকীয় উত্তর",

    meccan = "মাক্কী",
    medinan = "মাদানী",

    languageLabel = "ভাষা",
    appThemeLabel = "অ্যাপ থিম",
    sectionPrayerCalc = "নামাজের সময় ও হিসাব",
    locationLabel = "অবস্থান",
    methodLabel = "হিসাব পদ্ধতি",
    madhhabLabel = "আসর ফিকহ মাজহাব",
    adjustmentsLabel = "ম্যানুয়াল মিনিট সমন্বয়",
    hijriCalibrationLabel = "হিজরি তারিখ সমন্বয়",
    adhanCallLabel = "ওয়াক্তে আজানের আহ্বান",
    prePrayerReminderLabel = "নামাজের আগের রিমাইন্ডার",
    adhanSoundLabel = "মুয়াজ্জিনের কণ্ঠ",
    adhanVolumeLabel = "আজানের সাউন্ড ও অডিও টেস্ট",
    sectionQuran = "পবিত্র কুরআন ও তেলাওয়াত",
    riwayahLabel = "ক্বেরাত রেওয়ায়েত",
    scriptStyleLabel = "কুরআনের ক্যালিগ্রাফি স্টাইল",
    reciterLabel = "ক্বারী কণ্ঠ",
    translationLabel = "অনুবাদ ও তাফসীর",
    arabicTextSizeLabel = "আরবি হরফের আকার",
    autoMasjidModeLabel = "নামাজে স্বয়ংক্রিয় নীরব মোড",
    sectionSystemDiagnostics = "সিস্টেম ও সেন্সর",
    compassDiagnosticsLabel = "কম্পাস সেন্সর ও ডায়াগনস্টিক",
    networkSyncLabel = "নেটওয়ার্ক সিঙ্ক ও উৎস",
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
reader = ReaderStrings(
        openIndex = "সূচি খুলুন",
        immersiveMode = "নিমগ্ন মোড",
        exitImmersive = "নিমগ্ন মোড থেকে বের হয় নিন",
        saveThisLocation = "এই আয়াত সংরক্ষণ করুন",  // verify
        layoutPerPage = "পৃষ্ঠা ধরে পৃষ্ঠা",  // verify
        layoutContinuousSurah = "ধারাবাহিক সূরা",  // verify
        scrollDirection = "স্ক্রলের দিক",
        previousPage = "আগের পৃষ্ঠা",
        nextPage = "পরের পৃষ্ঠা",
        perVerseTitle = "প্রতিটি আয়াত আলাদা করুন",  // verify
        perVerseDescription = "অধ্যয়নের ধরন: প্রতিটি আয়াতের নিজস্ব রেফারেন্স ও কাজ থাকে, নির্বাচন করলে দৃশ্যমান হয়।",  // verify
        scrollVertical = "উল্লম্ব",
        scrollHorizontal = "অনুভূমিক",
        backgroundColour = "পটভূমি",
        backgroundDefault = "অ্যাপের ডিফল্ট",
        pinchBehaviour = "পিঞ্চ করলে",
        pinchZoomView = "ভিউ জুম হয়",
        pinchTextSize = "লেখার আকার বদলায়",
        arabicTextSize = "আরবি লেখার আকার",
        translationSize = "অনুবাদের আকার",
        quranFont = "কুরআনের ফন্ট",
        showTranslationLabelShort = "অনুবাদ দেখান",  // verify
        indexSurahs = "সূরা",  // verify
        indexSaved = "সংরক্ষিত",
        indexSearch = "খুঁজুন",
        emptySavedTitle = "এখনও কিছু সংরক্ষণ করা হয়নি",  // verify
        emptySavedMessage = "পড়ার সময় একটি আয়াত সংরক্ষণ করুন, তা এখানে অপেক্ষা করবে।",  // verify
        headingLabel = "শিরোনাম",
        mushaf = "মুশফ",  // verify
        changeInReader = "এটি মুশফ পাঠের বিকল্পগুলোতে বদলান।",
        pageAnnouncement = "পৃষ্ঠা %1\$d, জুজ %2\$d, সূরা %3\$s, %4\$s, %5\$s থেকে %6\$s পর্যন্ত",
        blockAnnouncement = "%1\$s থেকে %2\$s পর্যন্ত, %3\$s, পৃষ্ঠা %4\$d",
        verseCountOne = "%d আয়াত",
        verseCountMany = "%d আয়াত",
        rangeTo = " থেকে ",
        paperRose = "গোলাপি",
        paperApricot = "কমলা",
        paperSand = "বেলে",
        paperSage = "জলওয়ালা",
        paperMist = "কুয়াশা",
        paperIndigo = "নীল",
        paperLilac = "বেগুনি",
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
        playVerse = "আয়াত চালান",
        pauseVerse = "বিরতি",
        bookmarkVerse = "বুকমার্ক যোগ করুন",
        removeBookmark = "বুকমার্ক সরান",
        copyVerse = "আয়াত কপি করুন",
        shareVerse = "আয়াত শেয়ার করুন",
        verseCopied = "আয়াত কপি হয়েছে",
        textSize = "লেখার আকার",
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
        actionChange = "পরিবর্তন",
        adhanVolume = "ভলিউম", // verify
        alertSilentReminder = "নীরব স্মরণিকা", // verify
        allSurahsLabel = "সব সূরা", // verify
        ambientField = "পরিবেশী ক্ষেত্র",
        appliedAdjustments = "ম্যানুয়াল সমন্বয়", // verify
        audioSourceLabel = "তিলাওয়ার অডিও", // verify
        audioStreamedNotCached = "স্ট্রিম হচ্ছে, ডিভাইসে সংরক্ষিত নয়", // verify
        autoSilenceDurationLabel = "স্বয়ংক্রিয় নীরবতার সময়কাল", // verify
        calibrationMessage = "সেন্সরটি স্থিতিশীল হতে ডিভাইসটি কয়েকবার ৮ আকারে নাড়ুন।",
        chooseAdhanSound = "আজানের শব্দ", // verify
        chooseAdjustments = "মিনিট সমন্বয়", // verify
        chooseHijriOffset = "হিজরি তারিখ অফসেট", // verify
        chooseLanguage = "ভাষা",
        chooseMadhhab = "আসরের হিসাব", // verify
        chooseMethod = "গণনা পদ্ধতি", // verify
        choosePrePrayerOffset = "নামাজের আগে স্মরণিকা", // verify
        chooseReciter = "কারী", // verify
        chooseScript = "লিপি", // verify
        chooseTheme = "থিম",
        computedOnDevice = "এই ডিভাইসে গণনা করা হয়", // verify
        reschedulePrayers = "নামাজের অ্যালার্ম পুনরায় সক্রিয় করুন",
        corpusSummary = "১১৪ সূরা · ৩০ জুয় · ৬,২৩৬ আয়াত", // verify
        daysShort = "দিন", // verify
        gpsCached = "জিপিএস ক্যাশ করা হয়েছে",
        hizbOf = "হিয়ব %d", // verify
        hizbWord = "হিয়ব", // verify
        illumination = "আলোকসজ্জা",
        juzOf = "জুয় %d", // verify
        juzWord = "জুয়", // verify
        loadingQuranMessage = "পাঠ্য প্রস্তুত হচ্ছে। এটি শেষ না হলে পেছনে যান এবং সূরাটি আবার নির্বাচন করুন।",
        madhhabLabelShort = "আসর পদ্ধতি", // verify
        methodology = "পদ্ধতি", // verify
        minutesShort = "মিনিট", // verify
        nowReading = "এখন পড়া হচ্ছে", // verify
        pageWord = "পৃষ্ঠা", // verify
        perPrayerModes = "প্রতিটি নামাজের জন্য সতর্কতা", // verify
        prePrayerDisabled = "নিষ্ক্রিয়", // verify
        privacyNote = "নামাজের সময়, কিবলার দিক এবং কুরআন সবই এই ডিভাইসে গণনা করা হয়। কিছুই আপলোড হয় না।",
        privacyOffline = "সম্পূর্ণ অফলাইন",
        privacyNoAds = "কোনো বিজ্ঞাপন নেই",
        privacyNoAnalytics = "কোনো বিশ্লেষণ বা ডেটা সংগ্রহ নেই",
        privacyNoAccount = "কোনো অ্যাকাউন্ট বা সাইন ইন নেই",
        privacyFree = "বিনামূল্যে এবং ওপেন সোর্স",
        readingOptions = "পড়ার বিকল্প", // verify
        recitingLabel = "তিলাওয়াত করছে", // verify
        resetAllConfirmMessage = "গণনা, সতর্কতা ও প্রদর্শনের সেটিংস ডিফল্টে ফিরে যায়। আপনার সংরক্ষিত আয়াত ও নামাজের বিবরণ থেকে যায়।",
        resetAllConfirmTitle = "সমস্ত সেটিংস পুনরায় সেট করবেন?",
        resetAllLabel = "সমস্ত সেটিংস পুনরায় সেট করুন",
        searchHintMessage = "নাম বা অর্থ অনুযায়ী সূরা খুঁজুন, অথবা আরবি বা ইংরেজি পাঠ্য অনুযায়ী আয়াত খুঁজুন।", // verify
        searchHintTitle = "কুরআনে অনুসন্ধান", // verify
        searchSurahsAndVerses = "সূরা ও আয়াত অনুসন্ধান", // verify
        selectLayoutTitle = "পড়ার লেআউট", // verify
        selectSurah = "সূরা নির্বাচন করুন", // verify
        selectVerse = "আয়াত নির্বাচন করুন", // verify
        selectedCity = "নির্বাচিত শহর",
        sensorAccuracy = "সেন্সরের নির্ভুলতা",
        showTranslation = "অনুবাদ দেখান", // verify
        silentModeLabel = "সমস্ত সতর্কতা নীরব করুন", // verify
        stopAudio = "থামান", // verify
        storageLabel = "সঞ্চয়",
        sunAligned = "সূর্য প্রায় কিবলার দিকে। নিশ্চিত করতে এর দিকে মুখ করুন।", // verify
        sunAltitude = "সূর্য",
        sunAltitudeValue = "সূর্যের উচ্চতা",
        sunAzimuth = "সূর্যের দিক",
        sunBelowHorizon = "সূর্য ক্ষিতিজের নিচে, তাই এটিকে এখন রেফারেন্স হিসেবে ব্যবহার করা যাবে না।",
        sunToTheLeft = "কিবলা সূর্যের বাম পাশে প্রায় %d°।", // verify
        sunToTheRight = "কিবলা সূর্যের ডান পাশে প্রায় %d°।", // verify
        surahsFound = "%d সূরা", // verify
        testSound = "শব্দ পরীক্ষা", // verify
        themeDark = "ঢাকা",
        themeLight = "উজ্জ্বল",
        themeSystem = "সিস্টেম অনুযায়ী",
        timeFormat24hLabel = "২৪ ঘণ্টার ফরম্যাট", // verify
        translationCreditLine = "ইংরেজি — সহীহ ইন্টারন্যাশনাল", // verify
        verseCount = "%d আয়াত", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d আয়াত", // verify
        searchShowingFirst = "প্রথম %d দেখানো হচ্ছে",
        versesLabel = "আয়াত", // verify
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
            notifSilentFor = "%1\$s এর জন্য নীরব মোড চালু",
            notifVibrateAlert = "কম্পন বিজ্ঞপ্তি · %1\$s শুরু হয়েছে",
            notifTakbeerAlert = "তাকবীর বিজ্ঞপ্তি · %1\$s এর সময়",
            notifChimeAlert = "নরম ঘণ্টা বিজ্ঞপ্তি · %1\$s এর সময়",
            notifPrayerArrived = "%1\$s নামাজের সময় এসেছে (%2\$s)",
            notifEnterPrayer = "নামাজে প্রবেশ করুন এবং কাবার দিকে মুখ করুন (%1\$s)।",
            notifSilenceAction = "নীরব",
            notifMarkPrayed = "নামাজ আদায় হয়েছে চিহ্নিত করুন",
            notifPrePrayerTitle = "%2\$d মিনিটে %1\$s",
            notifPrePrayerText = "%1\$s শুরু হবে %2\$s · নামাজের প্রস্তুতি নিন",
            notifAdhanInProgress = "আজান চলছে",
            notifAdhanInProgressBody = "নামাজের বিজ্ঞপ্তি বাজছে",
        ),    ),
)

val RussianStrings = UiStrings(

    appName = "SALAH",
    navToday = "Сегодня",
    navPrayer = "Намаз",
    navQuran = "Коран",
    navQibla = "Кибла",
    navSettings = "Настройки",

    continueReading = "ПРОДОЛЖИТЬ ЧТЕНИЕ",
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
    prayerTimesHeader = "ВРЕМЯ НАМАЗА",
    vigilsAndNightPeriods = "НОЧНЫЕ ПЕРИОДЫ И ТАХАДЖУД",
    imsakTitle = "Имсак (за 10 мин до Фаджра)",
    midnightTitle = "Исламская полночь",
    lastThirdTitle = "Последняя треть ночи (Тахаджуд)",
    todayBtn = "Сегодня",

    kaabaDistance = "Расстояние до Каабы",
    trueNorth = "Истинный север",
    magneticNorth = "Магнитный север",

    meccan = "Мекканская",
    medinan = "Мединская",

    languageLabel = "Язык интерфейса",
    appThemeLabel = "Тема приложения",
    sectionPrayerCalc = "ВРЕМЯ НАМАЗА И РАСЧЕТ",
    locationLabel = "Местоположение",
    methodLabel = "Метод расчета",
    madhhabLabel = "Мазхаб (Аср)",
    adjustmentsLabel = "Ручная корректировка (минуты)",
    hijriCalibrationLabel = "Калибровка календаря Хиджры",
    adhanCallLabel = "Звук азана при наступлении времени",
    prePrayerReminderLabel = "Напоминание перед намазом",
    adhanSoundLabel = "Голос муэдзина",
    adhanVolumeLabel = "Громкость и проверка азана",
    sectionQuran = "СВЯЩЕННЫЙ КОРАН И ЧТЕНИЕ",
    riwayahLabel = "Риваят (Традиция чтения)",
    scriptStyleLabel = "Стиль шрифта Корана",
    reciterLabel = "Чтец Корана",
    translationLabel = "Перевод и тафсир",
    arabicTextSizeLabel = "Размер арабского шрифта",
    autoMasjidModeLabel = "Авто-беззвучный режим во время намаза",
    sectionSystemDiagnostics = "СИСТЕМА И ДАТЧИКИ",
    compassDiagnosticsLabel = "Датчики компаса и диагностика",
    networkSyncLabel = "Синхронизация с сетью и источник",
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
reader = ReaderStrings(
        openIndex = "Открыть указатель",
        immersiveMode = "Погружение",
        exitImmersive = "Выйти из погружения",
        saveThisLocation = "Сохранить этот аят",  // verify
        layoutPerPage = "По страницам",  // verify
        layoutContinuousSurah = "Сплошная сура",  // verify
        scrollDirection = "Направление прокрутки",
        previousPage = "Предыдущая страница",
        nextPage = "Следующая страница",
        perVerseTitle = "Выделить каждый аят",  // verify
        perVerseDescription = "Режим изучения: у каждого аята своя ссылка и действия, появляются при выборе.",  // verify
        scrollVertical = "Вертикально",
        scrollHorizontal = "Горизонтально",
        backgroundColour = "Фон",
        backgroundDefault = "Как в приложении",
        pinchBehaviour = "Щипок делает",
        pinchZoomView = "Увеличивает вид",
        pinchTextSize = "Меняет размер текста",
        arabicTextSize = "Размер арабского текста",
        translationSize = "Размер перевода",
        quranFont = "Шрифт Корана",
        showTranslationLabelShort = "Показать перевод",  // verify
        indexSurahs = "Суры",  // verify
        indexSaved = "Сохранённые",
        indexSearch = "Поиск",
        emptySavedTitle = "Пока ничего не сохранено",  // verify
        emptySavedMessage = "Сохраните аят при чтении — он будет ждать здесь.",  // verify
        headingLabel = "Заголовок",
        mushaf = "Мусхаф",  // verify
        changeInReader = "Измените это в параметрах чтения Корана.",
        pageAnnouncement = "Страница %1\$d, джуз' %2\$d, сура %3\$s, %4\$s, с %5\$s по %6\$s",
        blockAnnouncement = "с %1\$s по %2\$s, %3\$s, страница %4\$d",
        verseCountOne = "%d аят",
        verseCountMany = "%d аятов",
        rangeTo = " - ",
        paperRose = "Розовый",
        paperApricot = "Абрикосовый",
        paperSand = "Песочный",
        paperSage = "Шалфейный",
        paperMist = "Голубой",
        paperIndigo = "Индиго",
        paperLilac = "Сиреневый",
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
        playVerse = "Воспроизвести аят",
        pauseVerse = "Пауза",
        bookmarkVerse = "В закладки",
        removeBookmark = "Убрать из закладок",
        copyVerse = "Копировать аят",
        shareVerse = "Поделиться аятом",
        verseCopied = "Аят скопирован",
        textSize = "Размер текста",
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
        actionChange = "Изменить",
        adhanVolume = "Громкость", // verify
        alertSilentReminder = "Тихое напоминание", // verify
        allSurahsLabel = "Все суры", // verify
        ambientField = "Окружающее поле",
        appliedAdjustments = "Ручные поправки", // verify
        audioSourceLabel = "Аудио чтения", // verify
        audioStreamedNotCached = "Потоком, не сохраняется на устройстве", // verify
        autoSilenceDurationLabel = "Длительность авто-тишины", // verify
        calibrationMessage = "Двигайте устройство в виде восьмёрки несколько раз, чтобы датчик стабилизировался.",
        chooseAdhanSound = "Звук азана", // verify
        chooseAdjustments = "Поправки в минутах", // verify
        chooseHijriOffset = "Смещение хиджры", // verify
        chooseLanguage = "Язык",
        chooseMadhhab = "Расчёт Асра", // verify
        chooseMethod = "Метод расчёта", // verify
        choosePrePrayerOffset = "Напоминание до молитвы", // verify
        chooseReciter = "Чтец", // verify
        chooseScript = "Письмо", // verify
        chooseTheme = "Тема",
        computedOnDevice = "Вычисляется на этом устройстве", // verify
        reschedulePrayers = "Перевключить сигналы намаза",
        corpusSummary = "114 сур · 30 джузов · 6236 аятов", // verify
        daysShort = "дн", // verify
        gpsCached = "GPS закэширован",
        hizbOf = "Хизб %d", // verify
        hizbWord = "Хизб", // verify
        illumination = "Освещённость",
        juzOf = "Джуз %d", // verify
        juzWord = "Джуз", // verify
        loadingQuranMessage = "Подготовка текста. Если это не завершится, вернитесь назад и выберите суру снова.",
        madhhabLabelShort = "Метод Асра", // verify
        methodology = "Методика", // verify
        minutesShort = "мин", // verify
        nowReading = "Сейчас читается", // verify
        pageWord = "Страница", // verify
        perPrayerModes = "Напоминание для каждой молитвы", // verify
        prePrayerDisabled = "Выкл.", // verify
        privacyNote = "Время молитв, направление киблы и Коран вычисляются на этом устройстве. Ничего не отправляется.",
        privacyOffline = "Полностью офлайн",
        privacyNoAds = "Без рекламы",
        privacyNoAnalytics = "Без аналитики и сбора данных",
        privacyNoAccount = "Без аккаунта и входа",
        privacyFree = "Бесплатно и с открытым исходным кодом",
        readingOptions = "Настройки чтения", // verify
        recitingLabel = "Читает", // verify
        resetAllConfirmMessage = "Настройки расчёта, оповещений и отображения возвращаются к значениям по умолчанию. Сохранённые аяты и журнал молитв сохраняются.",
        resetAllConfirmTitle = "Сбросить все настройки?",
        resetAllLabel = "Сбросить все настройки",
        searchHintMessage = "Найти суру по названию или значению, или аят по арабскому или английскому тексту.", // verify
        searchHintTitle = "Поиск в Коране", // verify
        searchSurahsAndVerses = "Поиск сур и аятов", // verify
        selectLayoutTitle = "Макет чтения", // verify
        selectSurah = "Выбрать суру", // verify
        selectVerse = "Выбрать аят", // verify
        selectedCity = "Выбранный город",
        sensorAccuracy = "Точность датчика",
        showTranslation = "Показать перевод", // verify
        silentModeLabel = "Выключить все оповещения", // verify
        stopAudio = "Остановить", // verify
        storageLabel = "Хранилище",
        sunAligned = "Солнце почти в направлении киблы. Повернитесь к нему, чтобы подтвердить.", // verify
        sunAltitude = "Солнце",
        sunAltitudeValue = "Высота солнца",
        sunAzimuth = "Направление солнца",
        sunBelowHorizon = "Солнце под горизонтом, поэтому сейчас его нельзя использовать как ориентир.",
        sunToTheLeft = "Кибла примерно в %d° слева от солнца.", // verify
        sunToTheRight = "Кибла примерно в %d° справа от солнца.", // verify
        surahsFound = "%d сур", // verify
        testSound = "Проверить звук", // verify
        themeDark = "Тёмная",
        themeLight = "Светлая",
        themeSystem = "Как в системе",
        timeFormat24hLabel = "24-часовой формат", // verify
        translationCreditLine = "Английский — Saheeh International", // verify
        verseCount = "%d аятов", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d аятов", // verify
        searchShowingFirst = "показаны первые %d",
        versesLabel = "Аяты", // verify
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
            notifSilentFor = "Беззвучный режим включён для %1\$s",
            notifVibrateAlert = "Виброуведомление · %1\$s наступило",
            notifTakbeerAlert = "Уведомление с такбиром · Время %1\$s",
            notifChimeAlert = "Уведомление мягким звонком · Время %1\$s",
            notifPrayerArrived = "Время намаза %1\$s наступило (%2\$s)",
            notifEnterPrayer = "Встаньте на намаз и повернитесь к Каабе (%1\$s).",
            notifSilenceAction = "Без звука",
            notifMarkPrayed = "Отметить как прочитанный",
            notifPrePrayerTitle = "%1\$s через %2\$d мин",
            notifPrePrayerText = "%1\$s начнётся в %2\$s · Приготовьтесь к намазу",
            notifAdhanInProgress = "Азан играет",
            notifAdhanInProgressBody = "Звучит уведомление о намазе",
        ),    ),
)

val GermanStrings = UiStrings(

    appName = "SALAH",
    navToday = "Heute",
    navPrayer = "Gebet",
    navQuran = "Koran",
    navQibla = "Qibla",
    navSettings = "Einstellungen",

    continueReading = "LESEN FORTSETZEN",
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
    prayerTimesHeader = "GEBETSZEITEN",
    vigilsAndNightPeriods = "NACHTWACHEN & TAHAJJUD",
    imsakTitle = "Imsak (10 Min vor Fajr)",
    midnightTitle = "Islamische Mitternacht",
    lastThirdTitle = "Letztes Drittel der Nacht (Tahajjud)",
    todayBtn = "Heute",

    kaabaDistance = "Entfernung zur Kaaba",
    trueNorth = "Geographisch Nord",
    magneticNorth = "Magnetisch Nord",

    meccan = "Mekkanisch",
    medinan = "Medinensisch",

    languageLabel = "Sprache",
    appThemeLabel = "App-Design",
    sectionPrayerCalc = "GEBETSZEITEN & BERECHNUNG",
    locationLabel = "Standort",
    methodLabel = "Berechnungsmethode",
    madhhabLabel = "Rechtsschule (Asr)",
    adjustmentsLabel = "Manuelle Minutenanpassung",
    hijriCalibrationLabel = "Hidschri-Kalibrierung",
    adhanCallLabel = "Gebetsruf zur Gebetszeit",
    prePrayerReminderLabel = "Erinnerung vor dem Gebet",
    adhanSoundLabel = "Muezzin-Stimme",
    adhanVolumeLabel = "Lautstärke & Audiotest",
    sectionQuran = "DER HEILIGE KORAN & REZITATION",
    riwayahLabel = "Riwayah (Überlieferungstradition)",
    scriptStyleLabel = "Schriftstil des Korans",
    reciterLabel = "Rezitator",
    translationLabel = "Übersetzung & Exegese",
    arabicTextSizeLabel = "Arabische Schriftgröße",
    autoMasjidModeLabel = "Automatischer Stummmodus beim Gebet",
    sectionSystemDiagnostics = "SYSTEM & SENSOREN",
    compassDiagnosticsLabel = "Kompasssensoren & Diagnose",
    networkSyncLabel = "Netzwerksynchronisation & Quelle",
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
reader = ReaderStrings(
        openIndex = "Verzeichnis öffnen",
        immersiveMode = "Immersiver Modus",
        exitImmersive = "Immersiven Modus verlassen",
        saveThisLocation = "Diesen Vers speichern",  // verify
        layoutPerPage = "Seite für Seite",  // verify
        layoutContinuousSurah = "Fortlaufende Surea",  // verify
        scrollDirection = "Scrollrichtung",
        previousPage = "Vorherige Seite",
        nextPage = "Nächste Seite",
        perVerseTitle = "Jeden Vers einzeln",  // verify
        perVerseDescription = "Lernmodus: Jeder Vers hat seine eigene Referenz und eigene Aktionen, sichtbar bei Auswahl.",  // verify
        scrollVertical = "Vertikal",
        scrollHorizontal = "Horizontal",
        backgroundColour = "Hintergrund",
        backgroundDefault = "App-Standard",
        pinchBehaviour = "Auf Fingerbreite",
        pinchZoomView = "Ansicht vergrößern",
        pinchTextSize = "Schriftgröße ändern",
        arabicTextSize = "Arabische Schriftgröße",
        translationSize = "Übersetzungsgröße",
        quranFont = "Koran-Schriftart",
        showTranslationLabelShort = "Übersetzung anzeigen",  // verify
        indexSurahs = "Suren",  // verify
        indexSaved = "Gespeichert",
        indexSearch = "Suchen",
        emptySavedTitle = "Noch nichts gespeichert",  // verify
        emptySavedMessage = "Speichere beim Lesen einen Vers, er wartet hier auf dich.",  // verify
        headingLabel = "Überschrift",
        mushaf = "Mushaf",  // verify
        changeInReader = "Ändere das in den Lesoptionen des Mushaf.",
        pageAnnouncement = "Seite %1\$d, Dschuz %2\$d, Sure %3\$s, %4\$s, von %5\$s bis %6\$s",
        blockAnnouncement = "%1\$s bis %2\$s, %3\$s, Seite %4\$d",
        verseCountOne = "%d Vers",
        verseCountMany = "%d Verse",
        rangeTo = " bis ",
        paperRose = "Rose",
        paperApricot = "Aprikose",
        paperSand = "Sand",
        paperSage = "Salbei",
        paperMist = "Dunst",
        paperIndigo = "Indigo",
        paperLilac = "Flieder",
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
        playVerse = "Vers vortragen",
        pauseVerse = "Pausieren",
        bookmarkVerse = "Lesezeichen setzen",
        removeBookmark = "Lesezeichen entfernen",
        copyVerse = "Vers kopieren",
        shareVerse = "Vers teilen",
        verseCopied = "Vers kopiert",
        textSize = "Schriftgröße",
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
        actionChange = "Ändern",
        adhanVolume = "Lautstärke", // verify
        alertSilentReminder = "Stille Erinnerung", // verify
        allSurahsLabel = "Alle Suren", // verify
        ambientField = "Umgebungsfeld",
        appliedAdjustments = "Manuelle Anpassungen", // verify
        audioSourceLabel = "Rezitations-Audio", // verify
        audioStreamedNotCached = "Gestreamt, nicht auf dem Gerät gespeichert", // verify
        autoSilenceDurationLabel = "Dauer der automatischen Stille", // verify
        calibrationMessage = "Bewegen Sie das Gerät mehrmals in einer Acht, damit der Sensor sich stabilisiert.",
        chooseAdhanSound = "Adhan-Klang", // verify
        chooseAdjustments = "Minuten-Anpassungen", // verify
        chooseHijriOffset = "Hijri-Datum-Versatz", // verify
        chooseLanguage = "Sprache",
        chooseMadhhab = "Asr-Berechnung", // verify
        chooseMethod = "Berechnungsmethode", // verify
        choosePrePrayerOffset = "Erinnerung vor dem Gebet", // verify
        chooseReciter = "Rezitator", // verify
        chooseScript = "Schrift", // verify
        chooseTheme = "Design",
        computedOnDevice = "Wird auf diesem Gerät berechnet", // verify
        reschedulePrayers = "Gebetsalarme neu aktivieren",
        corpusSummary = "114 Suren · 30 Dschuz · 6.236 Verse", // verify
        daysShort = "T", // verify
        gpsCached = "GPS zwischengespeichert",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Beleuchtung",
        juzOf = "Dschuz %d", // verify
        juzWord = "Dschuz", // verify
        loadingQuranMessage = "Der Text wird vorbereitet. Wenn dies nicht abgeschlossen wird, gehen Sie zurück und wählen Sie die Sure erneut.",
        madhhabLabelShort = "Asr-Methode", // verify
        methodology = "Methode", // verify
        minutesShort = "Min", // verify
        nowReading = "Wird gerade gelesen", // verify
        pageWord = "Seite", // verify
        perPrayerModes = "Erinnerung pro Gebet", // verify
        prePrayerDisabled = "Aus", // verify
        privacyNote = "Gebetszeiten, Qibla-Richtung und der Koran werden auf diesem Gerät berechnet. Es wird nichts hochgeladen.",
        privacyOffline = "Vollständig offline",
        privacyNoAds = "Keine Werbung",
        privacyNoAnalytics = "Keine Analyse oder Datenerfassung",
        privacyNoAccount = "Kein Konto und keine Anmeldung",
        privacyFree = "Kostenlos und Open Source",
        readingOptions = "Leseoptionen", // verify
        recitingLabel = "Rezitiert", // verify
        resetAllConfirmMessage = "Berechnungs-, Warn- und Anzeigeeinstellungen werden auf die Standardwerte zurückgesetzt. Ihre gespeicherten Verse und Ihr Gebetsprotokoll bleiben erhalten.",
        resetAllConfirmTitle = "Alle Einstellungen zurücksetzen?",
        resetAllLabel = "Alle Einstellungen zurücksetzen",
        searchHintMessage = "Finden Sie eine Sure nach Name oder Bedeutung oder einen Vers nach arabischem oder englischem Text.", // verify
        searchHintTitle = "Im Koran suchen", // verify
        searchSurahsAndVerses = "Suren und Verse suchen", // verify
        selectLayoutTitle = "Lese-Layout", // verify
        selectSurah = "Sure auswählen", // verify
        selectVerse = "Vers auswählen", // verify
        selectedCity = "Ausgewählte Stadt",
        sensorAccuracy = "Sengenauigkeit",
        showTranslation = "Übersetzung einblenden", // verify
        silentModeLabel = "Alle Warnungen stummschalten", // verify
        stopAudio = "Stopp", // verify
        storageLabel = "Speicher",
        sunAligned = "Die Sonne steht fast in Qibla-Richtung. Drehen Sie sich ihr zu, um zu bestätigen.", // verify
        sunAltitude = "Sonne",
        sunAltitudeValue = "Sonnenhöhe",
        sunAzimuth = "Sonnenrichtung",
        sunBelowHorizon = "Die Sonne steht unter dem Horizont und kann deshalb gerade nicht als Referenz dienen.",
        sunToTheLeft = "Die Qibla liegt etwa %d° links von der Sonne.", // verify
        sunToTheRight = "Die Qibla liegt etwa %d° rechts von der Sonne.", // verify
        surahsFound = "%d Suren", // verify
        testSound = "Ton testen", // verify
        themeDark = "Dunkel",
        themeLight = "Hell",
        themeSystem = "System folgen",
        timeFormat24hLabel = "24-Stunden-Format", // verify
        translationCreditLine = "Englisch — Saheeh International", // verify
        verseCount = "%d Verse", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d Verse", // verify
        searchShowingFirst = "die ersten %d werden angezeigt",
        versesLabel = "Verse", // verify
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
            notifSilentFor = "Stiller Modus aktiv für %1\$s",
            notifVibrateAlert = "Vibrationshinweis · %1\$s ist eingetreten",
            notifTakbeerAlert = "Takbeer-Hinweis · Zeit für %1\$s",
            notifChimeAlert = "Sanfter Glockenhinweis · Zeit für %1\$s",
            notifPrayerArrived = "Die Gebetszeit für %1\$s ist eingetreten (%2\$s)",
            notifEnterPrayer = "Zieh das Gebet ein und wende dich der Kaaba zu (%1\$s).",
            notifSilenceAction = "Stumm",
            notifMarkPrayed = "Als gebetet markieren",
            notifPrePrayerTitle = "%1\$s in %2\$d Minuten",
            notifPrePrayerText = "%1\$s beginnt um %2\$s · Bereite dich auf das Gebet vor",
            notifAdhanInProgress = "Adhan läuft",
            notifAdhanInProgressBody = "Gebetshinweis wird abgespielt",
        ),    ),
)

val SpanishStrings = UiStrings(

    appName = "SALAH",
    navToday = "Hoy",
    navPrayer = "Oración",
    navQuran = "Corán",
    navQibla = "Alquibla",
    navSettings = "Ajustes",

    continueReading = "CONTINUAR LEYENDO",
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
    prayerTimesHeader = "HORARIOS DE ORACIÓN",
    vigilsAndNightPeriods = "VIGILIAS Y NOCHE",
    imsakTitle = "Imsak (10 min antes de Fajr)",
    midnightTitle = "Medianoche islámica",
    lastThirdTitle = "Último tercio de la noche (Taháyyud)",
    todayBtn = "Hoy",

    kaabaDistance = "Distancia a la Kaaba",
    trueNorth = "Norte verdadero",
    magneticNorth = "Norte magnético",

    meccan = "Mequí",
    medinan = "Mediní",

    languageLabel = "Idioma",
    appThemeLabel = "Tema de la aplicación",
    sectionPrayerCalc = "HORARIOS Y CÁLCULO",
    locationLabel = "Ubicación",
    methodLabel = "Método de cálculo",
    madhhabLabel = "Escuela jurídica (Asr)",
    adjustmentsLabel = "Ajustes manuales (minutos)",
    hijriCalibrationLabel = "Calibración fecha hiyri",
    adhanCallLabel = "Llamada a la oración puntual",
    prePrayerReminderLabel = "Recordatorio antes de la oración",
    adhanSoundLabel = "Voz del muecín",
    adhanVolumeLabel = "Volumen y prueba de audio",
    sectionQuran = "EL NOBLE CORÁN Y RECITACIÓN",
    riwayahLabel = "Riwāyah (Tradición de recitación)",
    scriptStyleLabel = "Estilo de caligrafía coránica",
    reciterLabel = "Recitador de audio",
    translationLabel = "Traducción y exégesis",
    arabicTextSizeLabel = "Tamaño del texto árabe",
    autoMasjidModeLabel = "Modo silencioso automático en oración",
    sectionSystemDiagnostics = "SISTEMA Y SENSORES",
    compassDiagnosticsLabel = "Sensores de brújula y diagnóstico",
    networkSyncLabel = "Sincronización de red y fuente",
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
reader = ReaderStrings(
        openIndex = "Abrir el índice",
        immersiveMode = "Modo inmersivo",
        exitImmersive = "Salir del modo inmersivo",
        saveThisLocation = "Guardar este versículo",  // verify
        layoutPerPage = "Página por página",  // verify
        layoutContinuousSurah = "Sura continua",  // verify
        scrollDirection = "Dirección de desplazamiento",
        previousPage = "Página anterior",
        nextPage = "Página siguiente",
        perVerseTitle = "Separar cada versículo",  // verify
        perVerseDescription = "Modo de estudio: cada versículo tiene su referencia y sus acciones, reveladas al seleccionarlo.",  // verify
        scrollVertical = "Vertical",
        scrollHorizontal = "Horizontal",
        backgroundColour = "Fondo",
        backgroundDefault = "Predeterminado de la app",
        pinchBehaviour = "El pellizco hace",
        pinchZoomView = "Ampliar la vista",
        pinchTextSize = "Cambiar el tamaño del texto",
        arabicTextSize = "Tamaño del texto árabe",
        translationSize = "Tamaño de la traducción",
        quranFont = "Fuente del Corán",
        showTranslationLabelShort = "Mostrar la traducción",  // verify
        indexSurahs = "Suras",  // verify
        indexSaved = "Guardados",
        indexSearch = "Buscar",
        emptySavedTitle = "Aún no has guardado nada",  // verify
        emptySavedMessage = "Guarda un versículo al leer y te estará esperando aquí.",  // verify
        headingLabel = "Título",
        mushaf = "Mushaf",  // verify
        changeInReader = "Cámbialo en las opciones de lectura del Mushaf.",
        pageAnnouncement = "Página %1\$d, juz' %2\$d, sura %3\$s, %4\$s, de %5\$s a %6\$s",
        blockAnnouncement = "%1\$s a %2\$s, %3\$s, página %4\$d",
        verseCountOne = "%d versículo",
        verseCountMany = "%d versículos",
        rangeTo = " a ",
        paperRose = "Rosa",
        paperApricot = "Albaricoque",
        paperSand = "Arena",
        paperSage = "Salvia",
        paperMist = "Niebla",
        paperIndigo = "Índigo",
        paperLilac = "Lila",
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
        playVerse = "Reproducir el versículo",
        pauseVerse = "Pausar",
        bookmarkVerse = "Añadir marcador",
        removeBookmark = "Quitar marcador",
        copyVerse = "Copiar el versículo",
        shareVerse = "Compartir el versículo",
        verseCopied = "Versículo copiado",
        textSize = "Tamaño del texto",
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
        actionChange = "Cambiar",
        adhanVolume = "Volumen", // verify
        alertSilentReminder = "Recordatorio silencioso", // verify
        allSurahsLabel = "Todas las suras", // verify
        ambientField = "Campo ambiental",
        appliedAdjustments = "Ajustes manuales", // verify
        audioSourceLabel = "Audio de la recitación", // verify
        audioStreamedNotCached = "En transmisión, no se guarda en el dispositivo", // verify
        autoSilenceDurationLabel = "Duración del silencio automático", // verify
        calibrationMessage = "Mueva el dispositivo en forma de ocho varias veces para que el sensor se estabilice.",
        chooseAdhanSound = "Sonido del adhan", // verify
        chooseAdjustments = "Ajustes en minutos", // verify
        chooseHijriOffset = "Desfase de la fecha hégira", // verify
        chooseLanguage = "Idioma",
        chooseMadhhab = "Cálculo del Asr", // verify
        chooseMethod = "Método de cálculo", // verify
        choosePrePrayerOffset = "Recordatorio antes de la oración", // verify
        chooseReciter = "Recitador", // verify
        chooseScript = "Escritura", // verify
        chooseTheme = "Tema",
        computedOnDevice = "Calculado en este dispositivo", // verify
        reschedulePrayers = "Reactivar las alarmas de oración",
        corpusSummary = "114 suras · 30 yuz · 6236 versículos", // verify
        daysShort = "d", // verify
        gpsCached = "GPS en caché",
        hizbOf = "Hizb %d", // verify
        hizbWord = "Hizb", // verify
        illumination = "Iluminación",
        juzOf = "Yuz %d", // verify
        juzWord = "Yuz", // verify
        loadingQuranMessage = "Preparando el texto. Si no termina, vuelve y elige la sura de nuevo.",
        madhhabLabelShort = "Método del Asr", // verify
        methodology = "Metodología", // verify
        minutesShort = "min", // verify
        nowReading = "Leyendo ahora", // verify
        pageWord = "Página", // verify
        perPrayerModes = "Alerta por oración", // verify
        prePrayerDisabled = "Desactivado", // verify
        privacyNote = "Los horarios de oración, la dirección de la qibla y el Corán se calculan en este dispositivo. No se sube nada.",
        privacyOffline = "Completamente sin conexión",
        privacyNoAds = "Sin publicidad",
        privacyNoAnalytics = "Sin análisis ni recopilación de datos",
        privacyNoAccount = "Sin cuenta ni inicio de sesión",
        privacyFree = "Gratuito y de código abierto",
        readingOptions = "Opciones de lectura", // verify
        recitingLabel = "Recitando", // verify
        resetAllConfirmMessage = "Los ajustes de cálculo, alerta y visualización vuelven a sus valores predeterminados. Tus versículos guardados y tu registro de oraciones se conservan.",
        resetAllConfirmTitle = "¿Restablecer todos los ajustes?",
        resetAllLabel = "Restablecer todos los ajustes",
        searchHintMessage = "Busca una sura por nombre o significado, o un versículo por su texto árabe o inglés.", // verify
        searchHintTitle = "Buscar en el Corán", // verify
        searchSurahsAndVerses = "Buscar suras y versículos", // verify
        selectLayoutTitle = "Diseño de lectura", // verify
        selectSurah = "Seleccionar sura", // verify
        selectVerse = "Seleccionar versículo", // verify
        selectedCity = "Ciudad seleccionada",
        sensorAccuracy = "Precisión del sensor",
        showTranslation = "Mostrar la traducción", // verify
        silentModeLabel = "Silenciar todas las alertas", // verify
        stopAudio = "Detener", // verify
        storageLabel = "Almacenamiento",
        sunAligned = "El sol está casi en la dirección de la qibla. Enfréntalo para confirmar.", // verify
        sunAltitude = "Sol",
        sunAltitudeValue = "Altitud del sol",
        sunAzimuth = "Dirección del sol",
        sunBelowHorizon = "El sol está bajo el horizonte, así que no puede usarse como referencia ahora.",
        sunToTheLeft = "La qibla está aproximadamente %d° a la izquierda del sol.", // verify
        sunToTheRight = "La qibla está aproximadamente %d° a la derecha del sol.", // verify
        surahsFound = "%d suras", // verify
        testSound = "Probar el sonido", // verify
        themeDark = "Oscuro",
        themeLight = "Claro",
        themeSystem = "Igual al sistema",
        timeFormat24hLabel = "Formato de 24 horas", // verify
        translationCreditLine = "Inglés — Saheeh International", // verify
        verseCount = "%d versículos", // verify
        verseReference = "%d:%d", // verify
        versesFound = "%d versículos", // verify
        searchShowingFirst = "se muestran los primeros %d",
        versesLabel = "Versículos", // verify
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
            notifSilentFor = "Modo silencioso activo para %1\$s",
            notifVibrateAlert = "Aviso por vibración · Ha entrado %1\$s",
            notifTakbeerAlert = "Aviso de takbeer · Es hora de %1\$s",
            notifChimeAlert = "Aviso de campana suave · Es hora de %1\$s",
            notifPrayerArrived = "Ha llegado la hora de la salá de %1\$s (%2\$s)",
            notifEnterPrayer = "Entra en la salá y orienta hacia la Kaaba (%1\$s).",
            notifSilenceAction = "Silenciar",
            notifMarkPrayed = "Marcar como rezada",
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
