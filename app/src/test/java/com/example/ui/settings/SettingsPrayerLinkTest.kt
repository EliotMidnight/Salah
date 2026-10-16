package com.example.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.SalahUiState
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The prayer times have a way in from Settings.
 *
 * ### The defect this file exists for
 *
 * The screen that shows the day's times and the month table is not one of the four dock
 * tabs - the dock is Today, Quran, Qibla and Settings - and the README and the
 * destination's own documentation both say the times are *reached from Settings*. The
 * row was never drawn anywhere: the destination was registered, the route existed, and
 * nothing led to it, so a whole screen of the app was reachable only from code.
 *
 * ### Why a screen test
 *
 * The row and the route are one journey split across two files, and this is the half a
 * screen owns: the row is on the screen, and pressing it asks for the times. The other
 * half - that `MainActivity` answers the request with the route - is the same wiring
 * every other row on this screen already has.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsPrayerLinkTest {

    @get:Rule
    val compose = createComposeRule()

    private var requests = 0

    private fun mount() {
        compose.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    SettingsScreen(
                        state = SalahUiState(),
                        onLocationSelect = {},
                        onMethodSelect = {},
                        onMadhhabSelect = {},
                        onAdjustmentsChange = {},
                        onAdhanToggle = {},
                        onPrePrayerToggle = {},
                        onVibrateOnlyToggle = {},
                        onTimeFormatToggle = {},
                        onOpenPrayer = { requests++ }
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the prayer times row is on the settings screen and asks for the prayer times`() {
        mount()

        val label = EnglishStrings.more.prayerTimesRow
        assertTrue(
            "the \"$label\" label is blank, so this test cannot find the row it presses",
            label.isNotBlank()
        )

        compose.onNodeWithTag("setting_prayer_times")
            .performScrollTo()
            .assertIsDisplayed()

        compose.onNodeWithText(label)
            .performScrollTo()
            .performClick()
        compose.waitForIdle()

        assertEquals(
            "the \"$label\" row was pressed and no request to open the prayer times left " +
                "the screen",
            1,
            requests
        )
    }
}
