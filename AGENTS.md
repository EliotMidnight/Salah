# AGENTS.md

Single-module Android app (`:app`): Kotlin, Jetpack Compose (Material 3), Room. Prayer
times, Quran and Qibla are computed/read on-device; the only network use is verse audio.
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
  debug-keystore fallback to release.
- Don't raise Gradle heap or parallelism in `gradle.properties`; the values are bounded
  for this host and the comments explain the failures they prevent.

## Quran corpus (never weaken)

- `app/src/main/resources/quran/{uthmani.txt,en_sahihintl.txt,metadata.xml}` are
  SHA-256-gated at load (`QuranCorpus`) and by `QuranResourceIntegrityTest`. If you edit
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
- KDoc carries the contract and the reason for it; history belongs in commit messages.
  Rebuild-history KDoc was trimmed deliberately — don't reintroduce it.
- Commit messages: `type(scope): the finding as a sentence` plus an explanatory body
  (e.g. `fix(quran): the search results header said "50 verses" where 143 verses match`).
- Don't add network calls, analytics, servers or new SDKs; offline-first is a product
  guarantee, and INTERNET exists only for `MediaPlayer` verse audio.
