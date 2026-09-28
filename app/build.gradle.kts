
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

// ---------------------------------------------------------------------------
// Host capability gate for Robolectric.
//
// Robolectric 4.15+ dropped the legacy interpreter; every Robolectric test now
// needs the `nativeruntime` shared library, which is published only for
// linux-x86_64 (plus macOS). On any other host - notably Linux aarch64, e.g.
// Termux on an ARM device - the runner aborts with
//   "The Robolectric native runtime is not supported on Linux (aarch64)"
// before a single assertion runs, which turns the whole build red for a
// platform reason rather than a code reason.
//
// So detect the host, and on unsupported hosts exclude exactly the classes that
// use RobolectricTestRunner (discovered from source, so new ones are picked up
// automatically) and say so loudly. The pure-JVM tests keep running everywhere.
// ---------------------------------------------------------------------------
val osName = System.getProperty("os.name").orEmpty()
val osArch = System.getProperty("os.arch").orEmpty()
val arm64 = osArch.equals("aarch64", true) || osArch.equals("arm64", true)
val robolectricSupported = !(osName.startsWith("Linux") && arm64)

val robolectricTestClasses: List<String> =
  file("src/test").walkTopDown()
    .filter { it.isFile && (it.name.endsWith(".kt") || it.name.endsWith(".java")) }
    .filter { it.readText().contains("RobolectricTestRunner") }
    .map { f ->
      val rel = f.relativeTo(file("src/test/java")).path
      rel.substringBeforeLast('.').replace(File.separatorChar, '.')
    }
    .sorted()
    .toList()

if (!robolectricSupported) {
  logger.lifecycle(
    "SALAH: Robolectric is not supported on $osName/$osArch - skipping " +
      "${robolectricTestClasses.size} Robolectric test class(es): ${robolectricTestClasses.joinToString()}. " +
      "Run them on an x86_64 or macOS host."
  )
}

tasks.withType<Test>().configureEach {
  if (!robolectricSupported) {
    robolectricTestClasses.forEach { cls -> filter { excludeTestsMatching(cls) } }
  }
  // Robolectric 4.15+ loads a native runtime per fork. With four workers on a
  // 4 GB machine the forks race each other extracting and binding it, and the
  // suite fails with "Unable to load Robolectric native runtime library" or an
  // UnsatisfiedLinkError on RenderNode - reproducibly when the whole suite runs,
  // and never when a single class runs alone. One fork at a time costs a little
  // wall clock and makes the result deterministic.
  maxParallelForks = 1
  forkEvery = 0
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.salah.quietpray"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // A release build is signed with the upload keystore, and only ever that.
  //
  // The previous version fell back to the *debug* keystore when the credentials
  // were absent and logged a warning. That is a release-blocking bug, not a
  // convenience: a debug-signed APK is rejected by the Play Store, the fallback
  // was silent enough to miss in a CI log, and "assembleRelease" still produced
  // a green build. The check below runs only when a release task is actually in
  // the graph, so `assembleDebug` and the compile tasks still work locally.
  //
  //   KEYSTORE_PATH  path to the upload .keystore / .jks
  //   STORE_PASSWORD keystore password
  //   KEY_ALIAS      key alias (defaults to "upload")
  //   KEY_PASSWORD   key password
  val uploadKeystore = System.getenv("KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.exists() }
  val storePasswordEnv = System.getenv("STORE_PASSWORD")
  val hasReleaseCreds = uploadKeystore != null && !storePasswordEnv.isNullOrEmpty()

  signingConfigs {
    create("release") {
      storeFile = uploadKeystore ?: file("${rootDir}/debug.keystore")
      storePassword = storePasswordEnv ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any {
      it.name == "assembleRelease" || it.name == "bundleRelease"
    }
    if (buildingRelease && !hasReleaseCreds) {
      throw GradleException(
        "Release signing credentials are missing. Set KEYSTORE_PATH, STORE_PASSWORD " +
          "(and KEY_ALIAS / KEY_PASSWORD if they differ from the defaults). A release " +
          "build is never signed with the debug keystore - the Play Store rejects it."
      )
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// SALAH is offline-first: prayer times, Qibla and the Quran corpus are computed
// and read on-device, and the app declares no INTERNET permission. There is no
// server, no analytics and no API key, so no secret-injection plugin is used and
// no `.env` file is read at build time.
//
// Release signing is the only thing that needs credentials, and those come from
// the environment (see `signingConfigs` above), never from a file in the repo.
dependencies {
  coreLibraryDesugaring(libs.android.desugar.jdk.libs)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.material)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
