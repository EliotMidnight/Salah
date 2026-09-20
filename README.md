# SALAH

Offline-first Android companion for prayer times, Quran reading, and Qibla direction.
Built with Kotlin and Jetpack Compose (Material 3). No account, no tracking, works fully offline.

## Features

- **Today dashboard** — next prayer countdown, live astronomical sky indicator, Hijri date, prayer checklist, continue-reading shortcut.
- **Prayer times** — 8 calculation methods (Morocco Ministry/Habous default, MWL, ISNA, Egypt, Umm Al-Qura, Karachi, Dubai, France 12°), Standard/Hanafi Asr jurisprudence, per-prayer minute adjustments, monthly calendar, Imsak / Islamic midnight / last-third-of-night vigils.
- **Quran reader** — all 114 surahs with verified Uthmani Arabic and full Saheeh International English, bundled offline (6,236 verses). Cards and continuous-text modes, tap-to-inspect verses, verse-level search in Arabic or English, bookmarks, continue-reading with progress, copy/share, per-verse audio recitation (Mishary Alafasy via everyayah.com, streamed).
- **Qibla compass** — sensor-fused bearing with true/magnetic north, distance to the Kaaba, magnetic-interference diagnostics, device-level indicator, vibration on alignment.
- **Adhan & alerts** — full Adhan, Takbeer-only, chime, vibration or silent per prayer; pre-prayer reminders; global silent and auto masjid-silence mode during prayer windows.
- **Localization** — full UI in 11 languages (English, Arabic, French, Indonesian, Turkish, Urdu, Malay, Bengali, Russian, German, Spanish) with RTL support.
- **Offline-first & private** — prayer math runs on-device; location stays on the device; the only network use is verse audio streaming.

## Requirements

- Android Studio (recent) with JDK 17+
- Android SDK with API 36 (compile/target) and build-tools 36
- `minSdk 24` — runs on Android 7.0+

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
3. Copy the secrets template (placeholders are fine for a debug build):
   ```bash
   cp .env.example .env
   ```
   `google-services.json` is optional — the build warns and continues without it.
4. Build and install the debug APK:
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

Pure-JVM tests (including the 6,236-verse corpus integrity test) run anywhere.
Robolectric/screenshot tests require an x86_64 host — they cannot run on ARM devices
(`native runtime is not supported on Linux (aarch64)`).
