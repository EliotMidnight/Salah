# AGENTS.md

Single-module Android app (`:app`): Kotlin, Jetpack Compose (Material 3), Room. Prayer
times, Quran and Qibla are computed/read on-device. Two things use the network, both
optional: verse audio, and the Ministry of Habous monthly timetable (online-first,
with the on-device calculation as the fallback).
Entry: `MainActivity` (NavHost: Today/Prayer/Quran/Qibla/Settings) and
`ui/SalahViewModel.kt`. `README.md` is the design document — its per-area sections explain
*why* the constraints below exist. Where README prose and build files/tests disagree,
trust the executable source; several prose claims have drifted.

## Build & test — use the wrapper

- `tools/build.sh` is the supported entry point, not bare `./gradlew`. It pins JDK 21,
  caps Gradle workers at 2, keeps Robolectric's temp files off `/tmp` (a 1.9 GB tmpfs on
  this host), and runs compile and test as separate invocations with a daemon stop in
  between. Without it, host resource limits surface as
  "Unable to load Robolectric native runtime library", which reads as a code failure but
  is not.
- `./tools/build.sh` — assembleDebug + full JVM suite (default).
- `./tools/build.sh test` — JVM suite only.
  Single class: `./tools/build.sh test --tests 'com.example.data.quran.QuranSearchCountTest'`.
- `./tools/build.sh record` — re-record the 32 Roborazzi baselines in
  `app/src/test/screenshots/`.
- `./tools/build.sh lint` — Android lint.
- A first local build needs `local.properties` (`sdk.dir`) and a generated
  `debug.keystore`; both are gitignored, and the `keytool` command is in README
  ("Quick start").
- `.github/workflows/ci.yml` runs the JVM suite on an x86_64 runner. It runs Gradle
  directly, not `tools/build.sh` (that wrapper encodes this host's JDK path and
  `/tmp` redirect). Screenshot comparison (`verifyRoborazziDebug`) stays on the
  recording host.

## Test quirks

- Robolectric requires x86_64 Linux/macOS. On ARM64 the build auto-excludes Robolectric
  classes by a literal source match on `RobolectricTestRunner`; keep that string in new
  Robolectric tests or they will run on ARM64 and fail for a platform reason.
- Screenshot baselines must be deterministic: feed fixed state into the surface (see
  `GreetingScreenshotTest`) instead of letting it read the wall clock, so two consecutive
  `record` runs stay byte-identical.
- A regression test here is expected to be shown able to fail: revert the fix, watch the
  test fail, restore. New invariants should be properties over the whole corpus
  (all 6,236 verses / 604 pages), not single examples.

## Toolchain pins that differ from defaults

- `compileSdk { version = release(36) { minorApiLevel = 1 } }` needs SDK package
  `platforms;android-36.1` (not `platforms;android-36`). targetSdk 36, minSdk 24.
- Gradle 9.3.1 (wrapper), AGP 9.1.1, Kotlin 2.2.10; versions live in
  `gradle/libs.versions.toml`, not in `app/build.gradle.kts`.
- R8 is off on purpose; don't enable it casually.
- Release tasks throw unless `KEYSTORE_PATH`/`STORE_PASSWORD` are set. Never add a
  debug-keystore fallback to release. `.env.example` lists the variables, and `.env`
  (gitignored) holds them locally; Gradle reads the *process environment*, not the
  file, so source it first: `set -a; . ./.env; set +a`.
- Don't raise Gradle heap or parallelism in `gradle.properties`; the values are bounded
  for this host and the comments explain the failures they prevent.

## Quran corpus (never weaken)

- The on-device Quran reader and bundled corpus were intentionally removed in this
  refactor. No `QuranCorpus`, `QuranRef`, reader, audio player, bookmark, or corpus
  asset references should remain. If the reader is restored, reintroduce the
  SHA-256 gate and `QuranResourceIntegrityTest` unchanged.
- Historically, `app/src/main/resources/quran/{uthmani.txt,en_sahihintl.txt,metadata.xml}`
  were SHA-256-gated at load (`QuranCorpus`) and by `QuranResourceIntegrityTest`. If you edit
  an asset, update the matching constant in `QuranCorpus.kt` to the digest of the new
  bytes. The check is the one thing that throws instead of guessing — do not soften it.
- Verse identity is `QuranRef(surah, ayah, page)`; page is not redundant. Build refs only
  via `Ayah.ref`. 51 mushaf pages cross a surah boundary, so "surah from page.first()"
  silently selects the wrong verse (highlight, action bar, bookmark).
- Page/juz'/hizb/manzil/ruku' partitions all come from the corpus; the reader computes
  none of them.

## Localization

- No hard-coded user-facing English, and no new field left on its English default.
  Strings live in data classes in `ui/localization/SalahLocalization.kt`; every one of the
  11 languages has a top-level val (`EnglishStrings`, `ArabicStrings`, ...) constructing
  the full tree. `StringCoverageTest` catches an untranslated or blank field,
  `DeadStringTest` catches a field nothing reads. Remove dead fields with
  `tools/prune_dead_strings.py`.
- `BorrowedWords` (`app/src/test/java/com/example/ui/BorrowedWords.kt`) is the one
  deliberate allowlist, for proper nouns that may stay English; don't add to it to silence
  a failure.
- These data classes have exceeded the JVM's constructor-parameter limit before — the
  compile stays green and the class loader throws `ClassFormatError`. If a class grows
  near that size, split a surface-specific class out (as `ReaderStrings` and
  `NotificationStrings` were) rather than growing `UiStringsMore`.

## Room (the reader's only user data)

- `SalahDatabase` is version 6, `exportSchema = false`; migrations are hand-written and
  `SalahMigrationTest` runs them against a real old-schema SQLite. Any version bump needs
  a migration; the destructive fallback in `getDatabase` is for downgrades only and
  costs the reader's bookmarks and reading position.
- Never use `ALTER TABLE ... DROP COLUMN` (needs SQLite 3.35 / Android 12; minSdk 24).
  Recreate-and-copy as `MIGRATION_3_4` does.

## Reader specifics that are easy to miss

- Ayah markers use Eastern Arabic-Indic digits in every language on purpose
  (`ArabicDigits`); paper names translate, typeface names don't.
- The mushaf pager forces its own reading direction, independent of the UI language;
  justification is tatweel insertion (`Kashida`), not widened word gaps.
- The Quran destination owns `ReaderPosition`; navigation goes through `goTo` /
  `pendingOpen`. Don't reintroduce a second "where am I" cursor.

## Conventions

- "A fact decided in two places" is this repo's definition of a bug: prefer deleting the
  second copy over syncing it. A detector that can't be made sound gets deleted, not
  allowlisted.
- This file is living documentation: whenever an agent changes behavior, files, tests,
  or decisions, update the relevant section here in the same change. Do not leave
  AGENTS.md stale after modifying the codebase.
- KDoc carries the contract and the reason for it; history belongs in commit messages.
  Rebuild-history KDoc was trimmed deliberately — don't reintroduce it.
- Commit messages: `type(scope): the finding as a sentence` plus an explanatory body
  (e.g. `fix(quran): the search results header said "50 verses" where 143 verses match`).
- Don't add network calls, analytics, servers or new SDKs; offline-first is a product
  guarantee. `INTERNET` exists for exactly two things: `MediaPlayer` verse audio, and
  the single `GET` of the Ministry of Habous monthly timetable in `HabousClient`.
  Every feature must still work with the radio off.

## Prayer times (accuracy is the product)

- `PrayerTimesProvider.forDate(...)` is the single decision point for a day's times:
  it returns the adopted published row when one is stored for this place, and the
  calculation otherwise. The ViewModel's current day and `PrayerAlarmScheduler` both
  go through it, so the adhan cannot disagree with the list. The month grid and the
  Today pager's adjacent days call the engine directly on purpose - a grid that
  changed convention halfway down a month would be worse than one that is a minute
  off its neighbour.
- `PrayerTimeZone.forDay(date, deviceZone)` decides the legal clock. There is no
  country table: the device zone is trusted, with one exception - `Africa/Casablanca`
  and `Africa/El_Aaiun` are pinned to UTC from 2026-09-20 (decree 2.26.530), because a
  phone with stale tzdata still reports +1 and that was the one-hour Rabat bug. The
  resulting zone travels on `PrayerTimesDay.zoneId`; never call `systemDefault()` a
  second time downstream. "Now" for the countdown, the next/previous prayer, the
  prayer-log date and the Hijri sheet's today marker comes from
  `PrayerTimeZone.localNow()` for the same reason: mixing the pinned clock with the
  device's made the app treat each prayer as passed an hour early on a phone with stale
  tzdata. The sun's position is the exception - it is a physical fact, so it keeps the
  instant the device is at.
- The engine computes everything in UT and converts once, per prayer, at the end.
  The sun's position is evaluated *at* each prayer (`eventUt`), not frozen at 00:00 UT;
  times round to the nearest minute, not truncate.
- `CalculationMethod` carries the convention's own margins: Morocco is Sunrise −3,
  Dhuhr +5, Maghrib +4. Those are measured against the ministry table, not astronomy,
  and belong to the method rather than to a country or a city table. Do not
  reintroduce `MOROCCO_CITY_DELTAS` or any per-city delta map.
- Offline floor: the calculation reproduces the ministry's Rabat table to the minute on
  most days and within ±1 minute over a month (the ministry uses unpublished per-city
  reference points). The month property in `PrayerCalculationTest` pins that; exact
  agreement is not the contract offline, and the hour is the failure that matters.
- Online path: `HabousFeed` parses `habous.gov.ma/data/horaires-YYYY-MM.js` and
  selects by *time agreement* with the local calculation (`TOLERANCE_MINUTES = 3`,
  whole-hour clock shifts only) - never by city name or coordinates, which were tried
  and misassigned Rabat to Kénitra. `HabousStore` keeps only the matched city (~2 KB)
  and invalidates on a 30 km move, a change of device zone, or a corrupt read.
  `HabousClient` is gated on `method.isMoroccoNationalTable` and
  `madhhab == Madhhab.STANDARD` (the table is published with the standard shadow
  factor, so adopting it would override a Hanafi reader's choice); every failure
  is a fallback, not an error path.
- `res/xml/network_security_config.xml` scopes the bundled Sectigo intermediate
  (`res/raw/habous_intermediate.pem`) to `habous.gov.ma`, whose server omits the chain
  link. Cleartext stays off. Don't widen that anchor to the base config.
- Settings copy must not promise what the online path breaks: the privacy card says
  "Works without a connection" and the calculation sheet says "Method, place and
  adjustments", in all eleven languages. "Completely offline" / "Computed on this
  device" were true before the ministry fetch existed and are not true now.
