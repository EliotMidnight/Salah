package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.example.ui.home.TodayScreen
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.qibla.QiblaScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screens at 200% Dynamic Type, and the two compass-shaped screens in an
 * RTL interface.
 *
 * The dock has its own large-text baseline; these cover the content that sits
 * above it, where a fixed row or a one-line label is easy to clip. The RTL
 * cases are not mirror checks for their own sake: the Qibla dial is a physical
 * instrument and must stay unmirrored, while its labels still have to be in the
 * reader's language.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AccessibilityScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun render(
        name: String,
        fontScale: Float = 1f,
        language: String = "English",
        scrollToText: String? = null,
        content: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = fontScale
                )
            ) {
                SalahTheme(darkTheme = false) {
                    ProvideAppLanguage(language = language) {
                        content()
                    }
                }
            }
        }
        if (scrollToText != null) {
            composeTestRule.onNodeWithText(scrollToText).performScrollTo()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun today(language: String = "English") = todaySampleState().copy(
        language = language
    )

    @Composable
    private fun settings(language: String = "English") = SettingsScreen(
        state = todaySampleState().copy(language = language),
        onLocationSelect = {},
        onMethodSelect = {},
        onMadhhabSelect = {},
        onAdjustmentsChange = {},
        onAdhanToggle = {},
        onPrePrayerToggle = {},
        onVibrateOnlyToggle = {},
        onTimeFormatToggle = {}
    )

    @Test
    fun home_large_text_light() = render(
        name = "home_large_text_light",
        fontScale = 2f
    ) {
        TodayScreen(state = today(), onLocationClick = {})
    }

    @Test
    fun qibla_large_text_light() = render(
        name = "qibla_large_text_light",
        fontScale = 2f
    ) {
        QiblaScreen(state = qiblaSampleState(facing = false))
    }

    @Test
    fun settings_light() = render(name = "settings_light") {
        settings()
    }

    @Test
    fun settings_large_text_light() = render(
        name = "settings_large_text_light",
        fontScale = 2f
    ) {
        settings()
    }

    @Test
    fun settings_bottom_light() = render(
        name = "settings_bottom_light",
        scrollToText = EnglishStrings.more.resetAllLabel
    ) {
        settings()
    }

    @Test
    fun home_rtl_light() = render(
        name = "home_rtl_light",
        language = "العربية"
    ) {
        TodayScreen(state = today("العربية"), onLocationClick = {})
    }

    @Test
    fun qibla_rtl_light() = render(
        name = "qibla_rtl_light",
        language = "العربية"
    ) {
        QiblaScreen(state = qiblaSampleState(facing = false).copy(language = "العربية"))
    }

    @Test
    fun settings_rtl_light() = render(
        name = "settings_rtl_light",
        language = "العربية"
    ) {
        settings("العربية")
    }
}
