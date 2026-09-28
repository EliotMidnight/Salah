package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.qibla.QiblaScreen
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QiblaScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun state(facing: Boolean) = SalahUiState(
        location = UserLocation.RABAT,
        method = CalculationMethod.MOROCCO_MINISTRY,
        madhhab = Madhhab.STANDARD,
        adjustments = PrayerAdjustments(),
        compassAzimuth = 40f,
        qiblaBearing = 118f,
        relativeQiblaAngle = if (facing) 0f else 78f,
        isFacingQibla = facing,
        useTrueNorth = true,
        distanceToKaabaKm = 4812,
        isDeviceLevel = true
    )

    private fun render(facing: Boolean, dark: Boolean, name: String) {
        composeTestRule.setContent {
            SalahTheme(darkTheme = dark) {
                ProvideAppLanguage(language = "English") {
                    QiblaScreen(
                        state = state(facing),
                        onToggleTrueNorth = {},
                        onFetchLocation = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun qibla_searching_light() = render(facing = false, dark = false, name = "qibla_searching_light")

    @Test
    fun qibla_searching_dark() = render(facing = false, dark = true, name = "qibla_searching_dark")

    @Test
    fun qibla_aligned_light() = render(facing = true, dark = false, name = "qibla_aligned_light")

    @Test
    fun qibla_aligned_dark() = render(facing = true, dark = true, name = "qibla_aligned_dark")
}
