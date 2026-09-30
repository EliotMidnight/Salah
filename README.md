# SALAH

Offline-first Android companion for prayer times, Quran reading, and Qibla direction.
Built with Kotlin and Jetpack Compose (Material 3). No account, no tracking, works fully offline.

## Features

- **Today dashboard** — next prayer countdown, live astronomical sky indicator, Hijri date, prayer checklist, continue-reading shortcut.
- **Prayer times** — 8 calculation methods (Morocco Ministry/Habous default, MWL, ISNA, Egypt, Umm Al-Qura, Karachi, Dubai, France 12°), Standard/Hanafi Asr jurisprudence, per-prayer minute adjustments, monthly calendar, Imsak / Islamic midnight / last-third-of-night vigils.
- **Quran reader** — opens straight into the text at your last position (Al-Fatihah on a first run). Three layouts (per ayah, per page, continuous surah), vertical or horizontal, the real 604-page mushaf partition, seven washed-out papers with light/dark treatment, Arabic and translation size, five named Quran fonts (two bundled), pinch as zoom or as text size, and immersive mode that clears the status bar and the dock. All 114 surahs with verified Uthmani Arabic and full Saheeh International English, bundled offline (6,236 verses). Verse-level search in Arabic or English with page/juz'/hizb quick filters, bookmarks, copy/share, per-verse audio recitation (Mishary Alafasy via everyayah.com, streamed).
- **Qibla compass** — sensor-fused bearing with true/magnetic north, distance to the Kaaba, magnetic-interference diagnostics, device-level indicator, vibration on alignment. Graduated dial: tick every 2°, numerals every 30°, heading and Qibla bearing side by side.
- **Adhan & alerts** — full Adhan, Takbeer-only, chime, vibration or silent per prayer; pre-prayer reminders; global silent and auto masjid-silence mode during prayer windows.
- **Localization** — full UI in 11 languages (English, Arabic, French, Indonesian, Turkish, Urdu, Malay, Bengali, Russian, German, Spanish) with RTL support.
- **Offline-first & private** — prayer math runs on-device; location stays on the device; the only network use is verse audio streaming.

## Requirements

- Android Studio (recent) with JDK 17+ (JDK 21 also verified)
- Android SDK with API 36 (compile/target) and build-tools 36
- `minSdk 24` — runs on Android 7.0+
- No separate Gradle install: the Gradle wrapper is checked in (`./gradlew`, Gradle 9.3.1)

## Quick start

```bash
git clone https://github.com/EliotMidnight/Salah.git
cd Salah
```

1. Point Gradle at your SDK (`local.properties`, not committed):
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
2. Provide the debug keystore the build expects (`debug.keystore`, not committed):
   ```bash
   keytool -genkeypair -keystore debug.keystore -alias androiddebugkey \
     -keyalg RSA -keysize 2048 -validity 10950 \
     -storepass android -keypass android \
     -dname "CN=Android Debug, O=Android, C=US"
   ```
3. Build and install the debug APK. No `.env` or `google-services.json` is needed —
   the app has no server and reads no secrets at build time:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Building on-device (Termux, ARM64)

The SDK's x86_64 `aapt2`/`zipalign` cannot execute on ARM Android. The working setup is:

- Native Termux `aapt2`, selected via a device-local override in `~/.gradle/gradle.properties`
  (user-home properties apply to every build on the device and are never committed):
  ```properties
  android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
  ```
  Do NOT put this in the repo's `gradle.properties` — the path only exists on Termux
  and would break builds on normal machines (verified: AGP ignores this key in
  `local.properties`).
- A `zipalign` passthrough shim in `build-tools/36.0.0/` (same reason).
- Copy the project to internal storage (`~/Salah-build`) before building — the shared
  `/storage` mount lacks exec permissions. Build with reduced parallelism on low-RAM devices:
  ```bash
  gradle assembleDebug --no-parallel --max-workers=2
  ```

## Project structure

```
app/src/main/java/com/example/
├── MainActivity.kt            # Nav host: Today / Prayer / Quran / Qibla / Settings
├── data/
│   ├── model/                 # Prayer, Quran, location models
│   ├── local/                 # Room: prayer log, bookmarks, continue-reading
│   ├── location/              # GPS + cached location
│   └── quran/                 # QuranDataSource + verified corpus loader
├── engine/                    # Prayer math, Hijri calendar, Qibla, sky, audio
├── service/                   # Alarm receiver/scheduler, alert service
└── ui/                        # Compose screens, theme, 11-language strings
app/src/main/resources/quran/  # Uthmani text, metadata, EN translation (see SOURCES.md)
app/src/test/                  # JVM unit tests incl. QuranCorpusTest
```

## Data sources & attribution

- Quran Arabic (Uthmani 1.1) and partition metadata: **Tanzil Project** (CC BY 3.0) — https://tanzil.net — see `app/src/main/resources/quran/SOURCES.md` for hashes and notices.
- English translation: **Saheeh International**, shown in-app as “English — Saheeh International”.
- Verse audio: **everyayah.com** (Mishary Alafasy, 128 kbps), streamed on demand.

## Tests

```bash
./gradlew testDebugUnitTest
```

24 pure-JVM tests (Quran corpus integrity, prayer math, Qibla bearing, location
validation, sky-text contrast) run on any host, including ARM64 Linux/Termux.

Three Robolectric classes (`ExampleRobolectricTest`, `GreetingScreenshotTest`)
need an **x86_64 Linux or macOS** host: Robolectric 4.15+ requires its native
runtime, which has no ARM64 Linux build. The build detects this and skips exactly
those classes on ARM64 with a loud log line, so the suite stays green for the
right reason instead of failing for a platform one. Run them on CI or an x86_64
machine to exercise them:

```bash
./gradlew testDebugUnitTest   # pure-JVM suites on any host
./gradlew recordRoborazziDebug  # regenerate screenshot baselines (x86_64 only)
```

## Configuration & environment

| Variable / file | Required | Notes |
| --- | --- | --- |
| `local.properties` (`sdk.dir`) | Yes, local builds | Not committed. Points Gradle at your Android SDK. |
| `debug.keystore` | Yes, debug builds | Not committed. Generate with the `keytool` command above. |
| `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` (env) | Release builds | Release signing credentials. Never commit; provide via CI secrets. |

There is no `.env` file, no `google-services.json` and no secret-injection
Gradle plugin: the app has no server, no Firebase and no AI API, so it reads
nothing from disk at build time. `.env.example` documents the signing variables
for reference. No other secrets exist. Debug signing uses the well-known `android`/`androiddebugkey`
credentials, which are standard for debug builds and never used for release.

## Privacy

- No account, no analytics, no tracking, no ads.
- Prayer math, Hijri calendar, Qibla bearing, and the full Quran corpus run on-device.
- Location is used only to compute prayer times/Qibla and stays on the device
  (SharedPreferences + local Room cache). It is included in the standard OS app
  backup (`res/xml/data_extraction_rules.xml`); uninstall wipes it.
- The only network use is streaming verse audio from `https://everyayah.com`
  (Mishary Alafasy, 128 kbps) on demand. If the stream fails or times out
  (12 s watchdog), the app falls back to a gentle offline tone and stays usable.

## Production build & release

Debug APK (installable directly):

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Signed release bundle/APK (requires the env credentials above):

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD='***'
export KEY_PASSWORD='***'
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # AAB for Play publishing
```

If `KEYSTORE_PATH` is unset or the file is missing, the build logs a warning and
signs with the local `debug.keystore` so `assembleRelease` still succeeds for
verification. **Never publish a debug-signed build** — confirm the warning is
absent in your CI log.

Release notes:

- `minSdk 24` (Android 7.0+), `target/compile SDK 36`.
- R8 minification is off (`isMinifyEnabled = false`) — a deliberate choice to keep
  the Compose/Room release stable without a per-rule shrink profile.
- `versionCode`/`versionName` live in `app/build.gradle.kts` — bump both per release.
- Exact-alarm scheduling degrades gracefully: if the user denies
  `SCHEDULE_EXACT_ALARM`, alarms fall back to inexact delivery.
- Notifications require the runtime `POST_NOTIFICATIONS` grant (Android 13+);
  prayer times themselves always work without it.

## Today

The Today page is a port of the [athan-pwa](https://github.com/klsoen/athan-pwa)
home page: one dominant prayer name, the countdown on a hairline rule, the day's
times as a dotted-leader list, and a tap anywhere that opens the whole day as a
24-hour clock with Maghrib at the top.

- `ui/home/TodayScreen.kt` — the page, both views, and the day pager.
- `ui/home/ClockGeometry.kt` — the dial's angles, arcs and windows. Pure, and
  unit-tested in `ClockGeometryTest`, because a misplaced prayer still looks like
  a clock and a reversed arc still looks like an arc.
- `ui/home/CelestialBody.kt` — the sun or moon above the prayer name, glowing in
  its own colour and *placed* by the sky it is in. The sun's altitude and
  azimuth come from `QiblaEngine` for the user's location and time, so it rises
  on the correct side and crosses at the actual solar noon; its colour is the same
  `AstronomicalSky` altitude ramp that tints the sky, so it reddens at sunset for
  the same reason the horizon does. The moon's position is *derived from its
  phase* — new moon rides with the sun, full moon is opposite — because the app
  does not compute lunar ephemeris and a prayer app should not pretend to. Its
  phase is exact. On a light page the disc is mixed toward warm ink rather than
  shown at its glow value, because a cream sun on cream paper measures 1.1:1.
- `ui/home/HijriMonthSheet.kt` — the whole Hijri month, each cell carrying both
  the Hijri and Gregorian number. Month length is found by walking outward and
  asking the engine, not by arithmetic: a Hijri month is 29 or 30 days and
  guessing puts a day in two months or none.
- `ui/home/DaySelector.kt` — the date switcher, shared by the Prayer tab and
  Today.

The headline is the prayer's name **in the language the app is set to**, set in
Kalam — a handwriting face bundled under the SIL OFL
(`app/src/main/assets/kalam_OFL.txt`) — with the Arabic underneath as a subtitle.
Arabic leads only when the reader chose Arabic: a headline in a language you did
not pick is decoration, and it keeps the serif because Kalam has no Arabic
glyphs. The prayer *names* come from the same dictionary the rest of the app
uses, so the list below the headline speaks the same language.

The Hijri month sheet pages by whole Hijri months, walking outward and asking the
engine where each day lands rather than stepping 29 or 30 days - either of those
drifts, and lands in the wrong month within a year.

The Quran library has no top bar and no tab row, and there is no library screen
at all: opening Quran opens the text, at the last place it was left. The browse
affordances - surahs, saved verses, search, page/juz'/hizb - live in one sheet
that is a tap away from the reading surface and dismisses back onto it, rather
than being a screen you have to pass *through* to reach the book.

- `ui/quran/QuranReader.kt` - the reader, all four layouts, and the control pill.
- `ui/quran/QuranIndexSheet.kt` - surahs / saved / search in one sheet.
- `ui/quran/ReadingOptionsSheet.kt` - layout, axis, paper, typeface, pinch.
- `ui/quran/VerseCards.kt` - the verse block, translation card and inspector.
- `ui/theme/QuranFonts.kt`, `ui/theme/QuranPaper.kt` - typeface registry and the
  seven-colour mushaf paper, both contrast-checked in `QuranPaperContrastTest`.

### The one thing worth knowing about the reader

Every layout reads from the *same* anchor - a `(surah, ayah)` pair - and each
layout knows how to resolve that anchor into its own coordinates: an index into a
surah for the scrolling layouts, a page number for the mushaf layouts. That is
what makes switching layout keep your place instead of throwing you to the top
of the surah, and it is why there is no second "where am I" cursor anywhere.

Per-page mode follows the canonical 604-page partition from the bundled corpus,
so a page boundary is where the printed page actually breaks and a page can
begin in one surah and end in the next. That is also why page mode carries its
own cursor: no single verse stands for "page 300", so the cursor is the page and
it is written back into the anchor on every turn, which keeps Continue Reading
and bookmarks truthful.

Two invariants live in `QuranReadingOptions` rather than at the call sites, and
are unit-tested:

- **Continuous text cannot scroll sideways.** It has no page boundaries, so
  asking for it while horizontal pulls the axis back to vertical. The sheet does
  not hide the scroll control - it explains it, because a control that vanishes
  is worse than one that says why.
- **A pinch cannot leave a size the slider cannot express.** Both scales are
  clamped to the slider's range on the way in *and* on the way out, so a stored
  value from an older build cannot restore a size nothing can undo.

Pinch does two things that look identical and are not - it can change the type
(the line rewraps, the preference matches the screen) or magnify the view
(nothing about the reading changes, only how much of it fits). The reader asks
which, rather than picking one and leaving half the users frustrated. The view
scale is deliberately *not* persisted: reopening should show the reading at the
size the reader chose, not at whatever magnification was left behind.

### Immersive mode

Hides the status bar and the dock, and takes the control pill with them. It is
persisted, because the reason to want it is not something people flip on once by
accident. While the controls are hidden the first tap anywhere on the page brings
them back and does *not* select a verse - layered over the page on purpose, so
"reveal the UI" and "select this ayah" can never fire from the same touch.

### Quran fonts

Five faces are offered by name and two ship with the APK. `res/font` could only
take OFL faces: **Amiri** and **Lateef** are SIL Open Font License and freely
redistributable, and they carry the U+06DD ayah marker the corpus uses. The other
five slots - KFGQ, MeQuran, Digital Khatt v2, Naskh Nastaleeq, Noorani Quran -
resolve to the default face and are labelled *Not included yet*, because their
own licences do not allow redistribution (KFGQPC is "all rights reserved",
me_quran is "free for non commercial use", the rest are trademarked). Enabling
one you hold permission for is a two-step change; see
`app/src/main/assets/quran_fonts_OFL.txt`.

Each face carries its own line-height multiplier rather than sharing one, because
Nastaliq descenders need materially more room than a Naskh face and a Nastaliq
set with Naskh leading looks like a mistake. The picker previews each face with
real Quranic text, so the choice is made by looking rather than by reading a name.

- `ui/compose/CelestialClock.kt` — the dial and its labels.

The date lives in `SalahUiState.selectedDate` and is switched on the **Prayer**
tab. It is the app's only date switcher; Today follows it. Two switches meant two
answers to "which day am I looking at", and they drifted apart the moment you used
one and not the other.

The webapp's nine accent themes and its Google Fonts are **not** ported: the page
reads from the app's Material light/dark scheme so it sits with every other
screen, and the display serif is the system one. The webapp's own source is
checked out alongside this project at `../athan-pwa` for reference.

## Qibla

The Qibla dial follows [al_quran_v3](https://github.com/IsmailHosenIsmailJames/al_quran_v3)
- a graduated instrument, tick every 2° with 30° and 90° weighted, degree
numerals every 30°, and heading and Qibla bearing side by side so the eye can
compare them without the dial in between.

Only the design and the interaction were ported. The maths was not: this app's
`QiblaEngine` already computes the great-circle bearing, applies magnetic
declination for true north, reports the distance to the Kaaba and diagnoses
interference, none of which the reference has. Porting the reference's version
would have been a downgrade wearing a port's clothes.

Two defects in the reference were **not** carried across:

- **The needle was 180° out of phase.** Its tick lines used `(sin θ, +cos θ)`
  while its labels, cardinals and needle used `(sin θ, -cos θ)`. In a Y-down
  canvas those are diametrically opposite, so the ticks looked perfect (their
  style pattern has period 90°, which hides the mirror) while the needle pointed
  *directly away* from the Kaaba at the moment of alignment. Every endpoint in
  this app's dial uses the same `sin / -cos` parameterisation.
- **Alignment was computed as a linear difference**, so heading 359° and Qibla
  2° - two degrees apart in reality - compared as 357° apart and reported "not
  aligned", while the guidance banner beside the dial correctly said "turn 2°
  right". The two could contradict each other. `QiblaEngine.calculateRelativeAngle`
  folds the difference circularly, so they cannot.

The dial also unrolls its rotation: the target angle accumulates the shortest
signed delta from the previous frame rather than being set from the raw heading,
so crossing 359° → 0° continues the turn instead of spinning the card backwards
through 360°. That is the single detail that makes the compass feel like an
instrument rather than a spinning image.



- **Reduced motion**: when the system animation scale is 0 (*Remove animations* /
  developer setting), the living sky renders the same scene statically — the
  twinkling starfield, sun corona and ray rotation all stop
  looping, and every screen transition collapses to an instant change. This
  matters more here than in most apps, because large-area, slow, full-screen
  movement is exactly the pattern that triggers vestibular symptoms. See
  `SalahReduceMotion` in `ui/theme/Motion.kt`; every spec routes through
  `ExpressiveMotion.duration(...)`.
- **Reduced motion on the clock page**: the sun and moon's arrival and breathing
  glow, and the clock hand's entry sweep, all park into a single static frame at
  animation scale 0. The 24-hour dial is legible without any of it.
- **Touch targets** are 48 dp minimum; layouts use wrapping rows and bounded
  content widths so they survive large font scales and small screens.
- **RTL** is supported across all 11 languages.

## Known limitations

- Verse audio needs connectivity; everything else is offline.
- Robolectric and screenshot tests need an x86_64 host; they are skipped
  automatically on ARM64 (see Tests above).
- Time-zone offsets use a small built-in table for known countries
  (Morocco, Saudi Arabia, Egypt, Turkey, UK, US) and the device zone otherwise;
  travelers crossing zones should re-open the app so times recompute.
- Custom coordinates are validated to -90…+90 / -180…+180. Above the Arctic
  circle (or below the Antarctic circle) during polar day/night the sun never
  reaches some twilight angles, so the engine applies the standard
  nearest-latitude (`aqrab al-bilad`) fallback: times are computed at the
  closest latitude where all angles resolve, keeping the day ordered and the
  next-prayer countdown correct. Users there should still confirm with their
  local mosque timetable.
- Qibla accuracy depends on the device magnetometer; the app shows
  interference diagnostics when the field looks unreliable.

## Credits

**Icons by Salah Icons — CC BY 4.0**
<https://creativecommons.org/licenses/by/4.0/>

The per-prayer, Quran, mosque and sky glyphs in `app/src/main/res/drawable*/salah_*.xml`
come from the Salah Icons set. They are generated from the set's `themeable/` SVGs by
`tools/convert_salah_icons.py`, which maps the set's six semantic colour tokens
(`--ink`, `--tone`, `--paper`, `--soft-line`, `--mid-line`) onto this app's palette and
emits a light and a dark variant. Re-run it after changing the palette:

```bash
python3 tools/convert_salah_icons.py
```

## License

Copyright 2026 The SALAH Project Authors — Apache License 2.0. See [LICENSE](LICENSE).

The bundled Quran text and metadata come from the Tanzil Project (CC BY 3.0) and the
English translation from Saheeh International; attribution is preserved in
`app/src/main/resources/quran/SOURCES.md`. Verse audio is streamed from
everyayah.com at request time and is not redistributed in the APK.
