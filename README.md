# SALAH

Offline-first Android companion for prayer times, Quran reading, and Qibla direction.
Built with Kotlin and Jetpack Compose (Material 3). No account, no tracking, works fully offline.

## Features

- **Today dashboard** — next prayer countdown, live astronomical sky indicator, Hijri date, prayer checklist, continue-reading shortcut.
- **Prayer times** — 8 calculation methods (Morocco Ministry/Habous default, MWL, ISNA, Egypt, Umm Al-Qura, Karachi, Dubai, France 12°), Standard/Hanafi Asr jurisprudence, per-prayer minute adjustments, monthly calendar, Imsak / Islamic midnight / last-third-of-night vigils.
- **Quran reader** — opens straight into the text at your last position (Al-Fatihah on a first run). Two layouts — the canonical 604-page mushaf, or continuous per-surah flow with optional per-verse blocks — each on either axis. Seven washed-out papers with light/dark treatment, independent Arabic and translation sizing, five bundled Quran faces, pinch as zoom or as text size, and immersive mode that clears the status bar and the dock. All 114 surahs with verified Uthmani Arabic and Saheeh International English, bundled offline (6,236 verses). Verse-level search in Arabic or English with the matched words emphasised, and page/juz'/hizb quick filters, bookmarks, copy/share, per-verse audio recitation (Mishary Alafasy via everyayah.com, streamed), whose banner names the verse being recited so a reader who has scrolled elsewhere can still find it. Page turns work by drag *and* by accessibility action, so a page is reachable without a finger; every page announces itself — reference, extent, surah range and page — in the reader's own language, and the text is kept out of a display cutout in landscape, where the outer column of a page is where a page *begins*.
- **Qibla compass** — sensor-fused bearing with true/magnetic north, distance to the Kaaba, magnetic-interference diagnostics, device-level indicator, vibration on alignment. Graduated dial: tick every 2°, numerals every 30°, heading and Qibla bearing side by side, and a banner that names the turn — "turn right 12°" — rather than only reporting that you are not there yet.
- **Adhan & alerts** — every word a notification shows is in the reader's own language, read from the same preference the interface uses: full Adhan, Takbeer-only, chime, vibration or silent per prayer; pre-prayer reminders; global silent and auto masjid-silence mode during prayer windows. Five adhan timbres, one decoder, and the Settings preview plays the sound the alarm will play. Prayer times are computed on this device and are never described as having been verified against anything.
- **Localization** — full UI in 11 languages (English, Arabic, French, Indonesian, Turkish, Urdu, Malay, Bengali, Russian, German, Spanish) with RTL support.
- **Offline-first & private** — prayer math runs on-device; location stays on the device in one store; the only network use is verse audio streaming. The app ships no HTTP client at all.

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
│   └── quran/                 # QuranCorpus + the browse/search/text facades over it
├── engine/                    # Prayer math, Hijri calendar, Qibla, sky, audio
├── service/                   # Alarm receiver/scheduler, alert service
└── ui/
    ├── quran/                 # Reader, index sheet, options sheet, verse cards
    │   ├── reader/            # Mushaf pager/page, continuous flow, page fitting
    │   └── gesture/           # Pinch maths and the modifier that applies it
    ├── qibla/                 # Dial, guidance banner, sun cross-check
    ├── home/                  # Today, clock geometry, Hijri month sheet
    └── localization/          # 11 languages, one data class per group
app/src/main/resources/quran/  # Uthmani text, metadata, EN translation (see SOURCES.md)
app/src/test/                  # JVM + Robolectric suites, screenshot baselines
```

### The Quran text is verified, not trusted

Every bundled resource — `uthmani.txt`, `en_sahihintl.txt`, `metadata.xml` — is checked
against a SHA-256 digest on load, and a mismatch throws rather than warning. That is the
one place in the app that refuses to run instead of guessing: a swapped or truncated
corpus would put words in a reader's mouth that are not in the Quran, and no amount of
correct pagination makes that acceptable.

`QuranResourceIntegrityTest` checks the *declared digests against the shipped files*, so
editing an asset and forgetting its constant is a red build rather than an empty reader on
every device that installs it. The hex is formatted with `Locale.ROOT`: a digest is ASCII
by definition, and the check should depend on the bytes rather than on a formatting
decision — the failure mode otherwise is the whole corpus, thrown from the first lazy
access.

Every other number in the app follows the device locale on purpose. The ayah marker does
not: it is a typographic unit of the mushaf rather than a number in a sentence, so it
carries Eastern Arabic-Indic digits in every language — see `ArabicDigits`.

## Data sources & attribution

- Quran Arabic (Uthmani 1.1) and partition metadata: **Tanzil Project** (CC BY 3.0) — https://tanzil.net — see `app/src/main/resources/quran/SOURCES.md` for hashes and notices.
- English translation: **Saheeh International**, shown in-app as “English — Saheeh International”.
- Verse audio: **everyayah.com** (Mishary Alafasy, 128 kbps), streamed on demand.

## Tests

```bash
./tools/build.sh test     # the JVM test suite
./tools/build.sh check    # assembleDebug + the test suite
./tools/build.sh record   # re-record the screenshot baselines (x86_64 only)
```

`tools/build.sh` exists because of two facts about this particular machine, both
local to it and neither in `gradle.properties` where they would break a build
anywhere else: the default `java` on PATH is a JRE with no `javac`, so the build is
pointed at an installed JDK 21; and with 4 cores and ~3.8 GB of RAM alongside a
desktop session, a parallel Compose build gets OOM-killed, so it runs without
parallelism and with a bounded worker count. `./gradlew` works anywhere it has a
JDK and memory for it.

404 tests. The pure-JVM suites (Quran corpus integrity, page fitting, gesture
maths, search, prayer maths, Qibla bearing and guidance, localisation coverage,
sky-text contrast) run on any host, including ARM64 Linux/Termux.

The Robolectric classes need an **x86_64 Linux or macOS** host: Robolectric 4.15+
requires its native runtime, which has no ARM64 Linux build. `app/build.gradle.kts`
detects the host and skips exactly the classes that use `RobolectricTestRunner` on
ARM64 with a loud log line, so the suite stays green for the right reason instead
of failing for a platform one.

### The notification was the last English surface

Sixteen strings in `PrayerNotificationManager` and the foreground service were
hard-coded English, in an app that ships ten languages: three notification **channel**
names and descriptions, **six** status sentences (one per alert mode), the expanded text,
the "Silence" and "Mark Prayed" actions, the pre-prayer title and body, and "Adhan in
progress". A notification is read at prayer time by a reader who chose this app *because*
it speaks their language, and the ongoing one cannot be dismissed.

The six status sentences also interpolated `prayer.englishName`, so even the prayer's own
name was English in the sentence while the title above it showed both scripts. They now
interpolate `UiStrings.prayerName`.

Android **caches a channel's name at creation** and ignores later changes, so on an
existing install those three channel names stay English whatever the app does. That is a
platform limit, not something the code can work around; the strings are there so a fresh
install is not the only one that reads correctly.

### `UiStringsMore` was too big to test

Adding those strings took `UiStringsMore` to **249 `String` fields**, and Robolectric's
instrumenter emits a constructor with one parameter per field — past the JVM's 64 KB
method limit. The suite failed with `ClassFormatError: Too many arguments in method
signature` **before a single assertion ran**, so no test touching the app's own strings
could execute. Narrowing `instrumentedPackages` does not help (a class-level `@Config`
overrides the properties file), and `@DoNotInstrument` cannot even be written here — it
lives in Robolectric's annotations artifact, which is a *test* dependency.

So the nineteen notification strings became `NotificationStrings`, their own class with
their own reason to exist: they are built in one place, shown in one place, and read by
someone looking at a notification. `ReaderStrings` is already a nested class for the same
reason. **The remaining ~230 fields are still the next thing to do to that file** — they
should split the same way, by surface: settings, the Hijri calendar, the sky, search.

Localisation is guarded on the *translated* axis and on the *used* axis, which are
different questions.

### 133 strings nobody read

`StringCoverageTest` asks whether a string is translated. It cannot ask whether a string
is **used** — and a translated string nobody reads passes it perfectly, in all ten
languages, on every build, forever. That is how **133 fields** accumulated:

- **six** strings for two layout options. `layoutPerVerse` and `cardsLayout` both said
  "Per verse"; `layoutContinuous`, `continuousModeLabel` and `continuousLayout` all said
  "Continuous". Three spellings of one word, translated ten times over.
- `previousSurahLabel` and `nextSurahLabel` were the accessibility labels of the 48dp tap
  gutters this rebuild deliberately deleted. They survived because the strings file has no
  idea a control was removed. (They were also *wrong* while they existed — the gutter
  turned a **page**, so its label claimed "previous surah".)
- `fontNotBundled` said "Not included yet" at a time when every face in the picker shipped
  and `fontRes` was a non-null `Int`.
- `surahsTab`, `pageTab`, `juzTab`, `hizbTab`, `bookmarksTab` — five tab names, beaten by
  `ReaderStrings.indexSurahs` / `indexSaved` / `indexSearch`.

`DeadStringTest` now fails when a field has no reader, so the hundred and thirty-three
cannot come back — and they do try, because most of them were left behind deliberately by
a control that was removed. `tools/prune_dead_strings.py` lists and removes them safely;
it refuses to write unless every top-level declaration survives and every class body keeps
all of its fields, because a two-line declaration that loses its value line produces a
file that still *looks* plausible.

A **duplicate-string** detector was written and removed. Matching on the English text
finds real duplicates and also two fields that are correctly separate: `versesFound`
("12 verses found", in search results) and `verseCount` ("Al-Kahf · 110 verses", on a
surah) are one English phrase for two different facts, identical in all ten languages. An
unsound detector gets an allowlist to make it pass, and an allowlisted detector checks
nothing while still costing a build.

Localisation is guarded rather than trusted. `StringCoverageTest` sweeps **every**
`String` field of both `ReaderStrings` and `UiStringsMore` by reflection — about 2,000
strings across nine languages — and fails on one left in English, on one shipped blank,
and on one language inheriting the whole class's defaults. It used to sweep only
`ReaderStrings`, which is the smallest of the classes and the one a Quran rebuild is
most likely to be working in; every localisation defect found in this rebuild was in
the classes it was not covering. `BorrowedWords` is the one list of words a language
may keep as English — French *page* and *Silence*, German *Sand* and *NO* for
north-east, the translator's name in a credit line — and both tests read it, because
two allowlists for one fact is the defect this whole rebuild has been about.

Screenshot baselines live in `app/src/test/screenshots/` — 32 of them, including one
per bundled Quran face. They are only meaningful if a recording is reproducible, so
every screen they cover is fed state rather than reading the wall clock: the Today
page reads its countdown and its current prayer from the state the one-second ticker
maintains, which is what a page should have been doing anyway. Two consecutive
`record` runs produce byte-identical baselines.

### The third thing about this machine

`check` runs its compile and its test phase as **two invocations with the Gradle
daemon stopped in between**, and points `java.io.tmpdir` at `/home`. Both are the same
bug wearing two hats. `/tmp` here is a 1.9 GB tmpfs, and Robolectric extracts a
~205 MB native runtime into it on every run and never cleans up; three runs later the
extraction started failing, which surfaced as `Unable to load Robolectric native
runtime library` in whichever test class happened to start first — on an otherwise
identical tree, moving between runs. Stopping the daemon is the same fix from the
other side: `assembleDebug` drives the Compose compiler inside the daemon and keeps
its 1280m, and the test phase then cannot get a worker large enough for the
screenshots' native bitmaps. A disk-space and a heap failure, presenting as a
missing shared library.

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
- Location is used only to compute prayer times/Qibla and stays on the device, in
  `SharedPreferences` and nowhere else. Finding it reports one of four states —
  acquiring, resolved fresh, resolved from the OS's last known fix, or one of three
  named failures — as *facts*; the sentence is composed from the strings at the point
  of display. It was a finished English sentence in state, produced by a service
  layer, which meant a reader in any other language saw English and no change to the
  UI could have reached it. The banner deliberately does not call a last-known fix a
  "GPS location": it may be hours old. It used to be written to a Room table as
  well, and the two consumers read *different* stores — the UI read the preferences
  and the alarm scheduler read the Room row first — so the screen could show one
  place while the adhan fired for another. `LocationStore` is now the only reader and
  the only writer. It is included in the standard OS app backup
  (`res/xml/data_extraction_rules.xml`); uninstall wipes it.
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
home page: one dominant prayer name, the countdown on a hairline rule, and the day's
times as a dotted-leader list.

The 24-hour clock view is **not** ported. It made the whole page a tap target, and
the tap was the only affordance the page had; removing the clock and the
whole-page tap together is what left the layout quiet. The day pager the webapp
walked with is the app's existing date switcher instead.

- `ui/home/TodayScreen.kt` — the page and the day pager.
- `ui/home/CelestialBody.kt` — the sun or moon above the prayer name, glowing in
  its own colour and *placed* by the sky it is in. The sun's altitude and
  azimuth come from `QiblaEngine` for the user's location and time, so it rises
  on the correct side and crosses at the actual solar noon; its colour is the same
  `AstronomicalSky` altitude ramp that tints the sky, so it reddens at sunset for
  the same reason the horizon does. That ramp is the *only* sky palette:
  `AstronomicalSky` used to carry an eight-case `SkyPeriod` enum with its own
  hand-picked gradients, chosen by hour of day, which nothing rendered. Two
  answers to "what colour is the sky", one of them consulted by no one.
  The moon's position is *derived from its phase* — new moon rides with the sun,
  full moon is opposite — because the app does not compute lunar ephemeris and a
  prayer app should not pretend to. Its phase is exact. On a light page the disc
  is mixed toward warm ink rather than shown at its glow value, because a cream
  sun on cream paper measures 1.1:1.
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

## Quran

### Search, and the offset that came with it

Search matches in a *folded* copy of the text — the verse with every harakat, dagger alif,
small high mark and tatweel deleted and four letters folded onto others — so that a search
for `الرحمن` finds `ٱلرَّحْمَٰنِ`. That is what makes it diacritic-insensitive, and it is the
right thing to match against.

It is **not** the right thing to take an offset from. `ٱلرَّحْمَٰنِ` is nine characters and
`الرحمن` is six, so a match at offset 20 of the folded text is not at offset 20 of the verse
a reader is looking at. The search result's highlight was computed that way and applied to
the original — so the emphasis landed on the wrong words, further off the further into the
verse it was. It was also **rendered by nothing**: `QuranSearchHit.range` was computed on
every keystroke, described in six lines of KDoc as the answer to "why is this here", and
read only by its own test.

Both are fixed at the root. `QuranText.fold` emits the folded text *and* an offset map in
the **same pass**, so a match's position is carried out of the fold rather than guessed at
afterwards — recovering it by searching the original for the folded text would put the
second occurrence of a repeated word in the wrong place. `normalised` and the offsets are
two views of that one pass, so they cannot disagree.

The English path needs no map, because lowercasing the bundled translation preserves
length — which is *proved over all 6,236 verses* by a test rather than assumed.

`QuranSearchRangeTest` (11) checks the property that makes this falsifiable: fold the span
the highlight covers and it must equal the term that matched, for every verse and every
term. **The old offsets were planted and the test watched fail**, reporting that in 1:1 a
search for الرحمن highlighted `لَّهِ`. `SearchHighlightRenderTest` (5) then checks the
half a range test cannot see — that anything *uses* it — by reading the styled spans back
out of the semantics tree, so "the emphasis covers exactly the matched term, in the field
that matched, and changes nothing but colour" is an equality rather than a look at a
picture. **The highlight was removed and it was watched fail too.**

### The count on the results header was a false statement

`searchVerses` takes the best fifty matches and the sheet counted that page with `size`
and printed it. A search for "mercy" — **143** verses — was reported as **"50 verses"**.

That is worse than showing too few results. Fifty rows with no admission reads as *all* of
them, and nothing on screen contradicts the reader, so the number was not a truncation but
a false claim about the book: someone looking for every verse containing "mercy" had been
told they had seen them. The surah-name heading had the same bug.

`QuranSearch`'s KDoc claimed it was already fixed — "It took the first 50 and said
nothing. Results are now paged honestly" — which is the second half of the problem: the
documentation described the fix while the code kept the bug.

Both searches now return the page *and* the true total (`VerseResults`, `SurahResults`),
and the header reads **"143 verses · showing the first 50"**. The total comes from the same
scan that fills the page, so saying it costs nothing — a separate counting pass would
double the work over 6,236 verses on every debounced keystroke to reach the same number.
There is deliberately **no `size`** on either type: an ambiguity that caused this cannot
be reintroduced as a convenience.

`QuranSearchCountTest` (6) pins it, including a count taken **straight off the bundled
translation** so the search cannot agree with itself. **The old behaviour was planted and
the test watched fail**: `expected:<143> but was:<50>`.

The emphasis is colour and nothing else, deliberately: a result row is scanned, so the
match must be findable at a glance, but changing weight or size would reflow the row on
every keystroke as results re-rank — and in a Quranic face a synthetic bold is either
absent or a different typeface.

The library has no top bar and no tab row, and there is no library screen
at all: opening Quran opens the text, at the last place it was left. The browse
affordances - surahs, saved verses, search, page/juz'/hizb - live in one sheet
that is a tap away from the reading surface and dismisses back onto it, rather
than being a screen you have to pass *through* to reach the book.

- `ui/quran/QuranReader.kt` - the reader's chrome, the surface switch, and the
  gesture layer. It used to be 2,261 lines holding every layout inline.
- `ui/quran/reader/MushafPager.kt`, `MushafPage.kt`, `MushafPageText.kt` - the
  604-page surface: pager, page, and the Arabic set on it.
- `ui/quran/reader/ContinuousReader.kt`, `FlowingBlock.kt` - continuous flow, in
  blocks of twelve verses so neither the measure nor memory cost is proportional to
  the surah.
- `ui/quran/reader/ReaderPosition.kt` - position, selection and magnification as
  one value.
- `ui/quran/reader/PageFit.kt` - chooses the type size a page is actually measured
  to fit.
- `ui/quran/reader/PageActionBar.kt`, `PageInsets.kt` - the per-page action row and
  the chrome the page must not draw under.
- `ui/quran/QuranIndexSheet.kt` - surahs / saved / search in one sheet.
- `ui/quran/ReadingOptionsSheet.kt` - layout, axis, paper, typeface, pinch.
- `ui/quran/VerseCards.kt` - the verse block, translation card and inspector.
- `ui/quran/gesture/PinchMath.kt`, `ReaderPinch.kt` - the pinch arithmetic,
  separated from the modifier so it can be tested without a device.
- `ui/theme/QuranFonts.kt`, `ui/theme/QuranPaper.kt` - typeface registry and the
  seven-colour mushaf paper, both contrast-checked in `QuranPaperContrastTest`.

### The one thing worth knowing about the reader

A verse is identified by `QuranRef(surah, ayah, page)`. The page is not redundant:
an ayah number alone is ambiguous, because page 604 holds three ayah-1s and a
bookmark that stored only `ayah = 1` could not say which one.

Every layout resolves that same reference into its own coordinates, so switching
layout keeps your place instead of throwing you to the top of the surah, and there
is no second "where am I" cursor anywhere.

**The Quran destination owns the position, not the reader.** `ReaderPosition` used to
be created inside `QuranReader`, with the ViewModel holding a copy of the same fact
as `selectedSurah`. Navigation arrived through the ViewModel and the reader never
asked — so **the index could not move the reader at all**: picking a surah changed
the name in the pill and left the page where it was, and "Continue reading" on the
Today page did the same. `ReaderPosition.goTo` was documented as "the one entry
point; everything that navigates calls this" and had **zero callers**. The
destination now creates the position, the index and the ViewModel both navigate
through `goTo`, and what the ViewModel holds is a *request* — `pendingOpen: QuranRef?`
— with one writer, honoured once. `selectedSurah`, `currentSurahAyahs` and
`readingAyahHint` are gone, and a page's verses are derived from `position.surah`.

A verse is also assembled into a reference in exactly one place. A mushaf page is not
inside one surah — 51 of the 604 hold more than one, and 523 verses sit after a
boundary — so a page that handed its caller an ayah number and let the caller fill in
the surah from the page's *first* verse selected the wrong verse in the wrong surah
on every one of those pages: no highlight, the action bar showing a different verse's
translation, and a bookmark of the wrong reference. `Ayah.ref` is the one conversion
and `MushafPageText`, the only thing that knows which span a tap fell in, assembles
the reference. Per-page mode follows the canonical
604-page partition from the bundled corpus, so a page boundary is where the
printed page actually breaks and a page can begin in one surah and end in the next.

**A page is data, not a layout.** The corpus says which verses are on which page;
the reader never decides that. This is why a surah head prints no basmalah of its
own - the bundled Tanzil text carries the basmalah *inside* verse 1 of 113 of the
114 surahs, so printing one as well doubles it. At-Tawbah is the exception that
proves it: no basmalah is printed there either, because there is none in the text.

**Page fit is measured, not estimated.** A page's height is *quadratic* in the type
size, because both the number of lines and the line height grow with it. Arithmetic
that assumes one factor reports a clipped page as fitting - in the dangerous
direction, since it fails by hiding text. `PageFit.resolve` bisects on real
measurements instead.

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
accident.

The immersive button is *not* in the control row. It is pinned to the top-trailing
corner and stays visible in both states, faint when active, because a button that
disappears when you need it is a trap - "out of the way" and "no way back" are
different things. Nothing at all overlays the reading surface while immersive, so
swiping and tapping the page work exactly as they do outside it.

### Per-ayah, both ways

Per-ayah is one vertical stack of verses on either axis. Turning it sideways used
to produce a carousel with a single verse filling the screen, which is a
slideshow rather than a page of reading.

### Turning a page

A page turns by dragging, and by a page-turn accessibility action on the page
itself. Both go through one `turn` function, so the gesture and the accessibility
route cannot drift.

There used to be a third route as well: 48dp tap strips in the margin either side
of the text. They are gone. They cost reading width, and they only ever worked on
the horizontal axis, so a screen-reader user on the vertical layout had no way to
reach the next page at all. A page is reachable by finger or by action; the
margin is for the text.

A page can also be taller than the screen, since the Arabic is sized by the reader
and not by the page. The paged surface scrolls vertically inside the horizontal
pager rather than clipping, so no line is ever lost off the bottom.

### Panning while zoomed

With pinch set to *Zoom the view*, the magnified surface can be dragged with one
finger. `panZoomLock` is released above 1x, because a magnified view that cannot
be moved is one you have cropped yourself out of. At 1x the lock stays on, so a
one-finger drag is still an ordinary scroll and the two gestures never fight.
The offset is clamped to the surplus the scale actually created and re-centres on
the way back to 1x, so dragging can never leave a strip of empty paper beside the
text. The magnification lives on `ReaderPosition` rather than in the gesture layer,
because the drag/pan switch has to agree with the scale it is switching on.

### Quran fonts

Five faces, and all five ship in the APK: **Amiri**, **Amiri Quran**, **Lateef**,
**Scheherazade New** and **Harmattan**, all under the SIL Open Font License, which
permits redistribution inside an Apache-2.0 application.

There used to be seven on offer and two bundled. The other five - KFGQ, MeQuran,
Digital Khatt, Naskh Nastaleeq and Noorani - cannot be redistributed (KFGQPC is
"all rights reserved", me_quran is "free for non commercial use", the rest are
trademarked), so each silently fell back to Amiri. A picker that offers five
choices which all draw the same face is worse than a smaller honest list, and it
cost a settings screen to discover.

Each face was checked for **U+06DD**, the ayah-end ornament, before being added.
That codepoint is a standalone ornament rather than a numeric placeholder, and a
mushaf whose ayah markers are tofu boxes is not a mushaf; Reem Kufi was rejected on
exactly that. `fontRes` is a non-null `Int`, so a face without a file cannot be added
by accident.

There is no `isBundled` flag, and there is not meant to be. It was `get() = true` and
its only reader was a test asserting it was true — an assertion that could not fail,
for a property that existed only to be asserted. What actually can be zero is
`fontRes`, and that is what the test checks now.

**Typeface names are not translated; paper names are.** The picker shows "Amiri" and
"Lateef" in every language, because those are the names of the designs and translating
them would make a face unrecognisable to anyone who has seen it. The seven paper
names — Rose, Apricot, Sand, Sage, Mist, Indigo, Lilac — are ordinary words, and they
used to be an English `when` in `ReadingOptionsSheet`, so a reader in any of the ten
languages saw "Apricot" under the row and a screen-reader user heard it in each
swatch's `contentDescription`. They are strings now, in all ten.

Each face carries its own line-height multiplier rather than sharing one, because
Nastaliq descenders need materially more room than a Naskh face and a Nastaliq
set with Naskh leading looks like a mistake. Harmattan is the extreme case here -
a deep descender that needs more room than any other face in the set. The picker
previews each face with real Quranic text, so the choice is made by looking rather
than by reading a name. Licences are in `app/src/main/assets/quran_fonts_OFL.txt`.

**There is no baseline shift on any face, and there was never meant to be.** Each face
carried a `baselineShiftSp` (-0.5sp on the two Amiri faces, -1sp on Harmattan) that
was copied faithfully into the typeface registry and then never applied. Applying it
was tried and measured: every one of the five faces was rendered on page 2, the
densest page in the book, with and without it — and it made **all five worse**, because
a negative shift raises the text inside its line and that is exactly where Arabic's
shadda, fatha and dagger alif are. The field was deleted rather than wired up.

`QuranFontFaceProbeTest` records a baseline for every face on that dense page, so a
future change to leading shows up in a diff instead of in a bug report, and a face
added to the picker without one is a visible omission.

## Notes on the port

The date lives in `SalahUiState.selectedDate` and is switched on the **Prayer**
tab. It is the app's only date switcher; Today follows it. Two switches meant two
answers to "which day am I looking at", and they drifted apart the moment you used
one and not the other.

The webapp's nine accent themes and its Google Fonts are **not** ported: the page
reads from the app's Material light/dark scheme so it sits with every other
screen, and the display serif is the system one. The webapp's own source is
checked out alongside this project at `../athan-pwa` for reference.

Today's live reading - the countdown, the headline prayer, the highlighted row -
comes from `SalahUiState`, maintained by a one-second ticker in the ViewModel. The
page used to compute its own inside a `remember(day, isToday)`, and those keys do
not change between seconds, so all three held whatever was true when the screen was
composed: leave the page open across Dhuhr and the headline still read Asr with
Dhuhr as "next". A side effect worth knowing about is that the page read
`LocalTime.now()`, so every recording of the Today screenshots baked in whatever
time of day it was run at. Two recordings twenty minutes apart produced two
different images. The baselines are now reproducible, and that is verified rather
than assumed: two consecutive `record` runs are byte-identical.

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

A third defect was this app's own. The dial named its directions with a private
`getCardinalDirection` returning `"N"`, `"NE"`, `"E"`, `"SE"`, `"S"`, `"SW"`, `"W"`,
`"NW"` — in two visible places, the caption under the heading readout and the text
beside the azimuth. Every one of those is an *English* abbreviation, so a reader in
any of the ten shipped languages saw one. And it was not only untranslated: German
abbreviates *Nordost* to "NO" and *Südost* to "SO", so a shared abbreviation would
have been wrong for a German reader who reads English. The sectors now live in
`ReaderStrings.cardinal` with the words beside them, in all ten languages.

That move also fixed a bug the old code hid. The sectors were closed ranges —
`22.5f..67.5f` and `67.5f..112.5f` both contain 67.5 — so every boundary belonged to
two sectors and the answer depended on which arm of the `when` ran first. It came
out right, by luck, and nothing tested it. They are half-open now, and
`QiblaDirectionTest` checks all eight boundaries from either side.

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

The second fix is less obvious. Folding the difference is only half of it: the
result also has to be something a sentence can be built from, because "not aligned"
is not guidance. `QiblaGuidance` is that result - a direction and a whole number of
degrees - and it is the *one* source for the banner's words, the dial's ring colour,
the check mark, the number of degrees and the haptic. That last part matters: the
window used to be decided in three places at once (`abs(relativeAngle) <= 4.0f` in
the ViewModel, a `> 0` sign test in the banner, and the window in the guidance
type), and at 4.5° they gave three different answers - a green ring, a check mark,
and "turn 5°" on the same screen.

The alignment window is 5°, and that is deliberately forgiving. A compass reading
jitters by a degree or two with nobody moving, and a phone held at chest height in
one hand is worse. A tighter window makes the aligned state flicker as the reading
crosses the threshold, and a reader who has to watch for a flicker rather than being
*told* they are aligned will correct past the target and correct back.

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
- Mushaf justification is typographic, not print-exact. Only the KFGQ/QCF-style
  page-glyph fonts can reproduce the Uthmani justification rule, because the glyph
  itself carries the kashida; with a general-purpose face the line is justified by
  the text engine instead. Every bundled face is OFL, and none of the page-glyph
  fonts are.

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
