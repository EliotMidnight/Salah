package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.QiblaEngine
import com.example.engine.QiblaGuidance
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

    /**
     * Heading 40 with a Qibla of 118 is 78 degrees to the right, which is the
     * searching state; an aligned state puts the heading on the bearing.
     *
     * The guidance is built through the engine rather than hand-written, because
     * the states these screenshots are here to compare are precisely the ones
     * where the banner, the ring colour and the check mark can disagree - and a
     * hand-written fixture cannot drift into that disagreement the way a
     * hand-written `isFacingQibla = false` next to a raw angle could.
     */
    private fun state(facing: Boolean) = SalahUiState(
        location = UserLocation.RABAT,
        method = CalculationMethod.MOROCCO_MINISTRY,
        madhhab = Madhhab.STANDARD,
        adjustments = PrayerAdjustments(),
        compassAzimuth = if (facing) 118f else 40f,
        qiblaBearing = 118f,
        qiblaGuidance = QiblaGuidance.fromRelative(
            QiblaEngine.calculateRelativeAngle(
                currentHeading = if (facing) 118f else 40f,
                targetBearing = 118f
            )
        ),
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
