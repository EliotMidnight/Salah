package com.example.ui.home

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.example.todaySampleState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The prayer's name is centred above the page, not set at its column's edge.
 *
 * ### Why this is a test and not a screenshot
 *
 * It was only a screenshot once, and the screenshot was not enough. When the
 * name was wrapped in the box that measures the row for [HeadlineFit], it moved
 * to the left edge of the 360dp column - and the recording that introduced the
 * box re-recorded the baseline with it, so nothing failed. The diff was legible
 * (some 35,000 changed pixels in the headline band) but read as antialiasing.
 *
 * "The name is centred" is a statement about one node's x, so it is asserted as
 * one: the column is centred in the page, so the node's centre and the root's
 * centre are the same number. The state is the screenshot's own fixture, not a
 * second copy of it, so the two agree on what page they are testing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class TodayHeadlineCentringTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `the headline is centred above the page`() {
        composeTestRule.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = "English") {
                    TodayScreen(
                        state = todaySampleState(),
                        onLocationClick = {}
                    )
                }
            }
        }

        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val headline = composeTestRule
            .onNodeWithTag("today_headline")
            .getUnclippedBoundsInRoot()
        val rootCentre = (root.left + root.right) / 2
        val headlineCentre = (headline.left + headline.right) / 2

        assertEquals(
            "the headline is not centred in the page",
            rootCentre.value,
            headlineCentre.value,
            1f
        )
    }
}
