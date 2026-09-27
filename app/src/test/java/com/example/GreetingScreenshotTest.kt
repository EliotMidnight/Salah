package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CalculationMethod
import com.example.data.model.HijriDate
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.engine.SkyPeriod
import com.example.ui.SalahUiState
import com.example.ui.home.TodayScreen
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Renders Today to a PNG so the layout can be reviewed without a device.
 *
 * Covered in both light and dark, because the redesign specifically had to fix
 * the light theme: the old palette set `background` and `surfaceContainerLow` to
 * the same value, so every card was invisible in light mode and only showed up
 * in dark.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun sampleState(): SalahUiState {
        val date = LocalDate.of(2026, 9, 14)
        val prayers = listOf(
            PrayerTime(Prayer.FAJR, LocalTime.of(5, 12), LocalDateTime.of(date, LocalTime.of(5, 12)), isPassed = true),
            PrayerTime(Prayer.SUNRISE, LocalTime.of(6, 38), LocalDateTime.of(date, LocalTime.of(6, 38)), isPassed = true),
            PrayerTime(Prayer.DHUHR, LocalTime.of(13, 20), LocalDateTime.of(date, LocalTime.of(13, 20)), isPassed = true),
            PrayerTime(Prayer.ASR, LocalTime.of(16, 45), LocalDateTime.of(date, LocalTime.of(16, 45)), isPassed = true),
            PrayerTime(Prayer.MAGHRIB, LocalTime.of(19, 25), LocalDateTime.of(date, LocalTime.of(19, 25)), isNext = true),
            PrayerTime(Prayer.ISHA, LocalTime.of(20, 48), LocalDateTime.of(date, LocalTime.of(20, 48)))
        )
        return SalahUiState(
            location = UserLocation.RABAT,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.STANDARD,
            adjustments = PrayerAdjustments(),
            todayPrayerTimes = PrayerTimesDay(
                date = date,
                prayers = prayers,
                imsak = LocalTime.of(5, 2),
                midnight = LocalTime.of(0, 20),
                lastThirdOfNight = LocalTime.of(2, 0),
                calculationMethod = CalculationMethod.MOROCCO_MINISTRY,
                location = UserLocation.RABAT
            ),
            nextPrayer = prayers[4],
            previousPrayer = prayers[3],
            countdownString = "00:37:12",
            skyPeriod = SkyPeriod.MAGHRIB_SUNSET,
            celestialProgress = 0.85f,
            hijriDate = HijriDate(24, 3, "Rabi' al-Awwal", "ربيع الأول", 1448)
        )
    }

    private fun render(dark: Boolean, name: String) {
        composeTestRule.setContent {
            SalahTheme(darkTheme = dark) {
                ProvideAppLanguage(language = "English") {
                    TodayScreen(
                        state = sampleState(),
                        onLocationClick = {},
                        onContinueReadingClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun home_screen_light() = render(dark = false, name = "home_light")

    @Test
    fun home_screen_dark() = render(dark = true, name = "home_dark")
}
