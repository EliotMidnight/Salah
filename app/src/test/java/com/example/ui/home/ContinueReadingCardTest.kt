package com.example.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.ContinueReadingEntity
import com.example.todaySampleState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Continue Reading card names the surah in the interface's language.
 *
 * ### The defect
 *
 * The card's surah name was stored: `onAyahViewed` wrote `surah.englishName` into
 * `continue_reading` on every verse and the card printed it back, so an Arabic interface
 * read "Al-Baqarah 2:255" - one language's copy of a fact the names table owns, frozen in
 * the row by whichever interface happened to write it. The row keeps the reference and
 * the time now, and the card asks `QuranDataSource.surahName` for the name in the
 * language `state.language` says the interface is in.
 *
 * ### Why an assertion and not a screenshot
 *
 * The English card read "Al-Baqarah 2:255" before the fix too - the defect only shows in
 * the other ten languages - so an English baseline round would have moved nothing, and
 * the string is the whole statement: the exact text beside "Continue reading", in both
 * languages the naming rule distinguishes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ContinueReadingCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mount(language: String, continueAt: ContinueReadingEntity) {
        composeTestRule.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = language) {
                    TodayScreen(
                        state = todaySampleState().copy(
                            language = language,
                            continueReading = continueAt
                        ),
                        onLocationClick = {},
                        onContinueReadingClick = {}
                    )
                }
            }
        }
    }

    /** 2:255, not the fixture's 1:1, so a card that ignored the state could not pass. */
    private val alBaqarah255 = ContinueReadingEntity(
        surahNumber = 2,
        ayahNumber = 255,
        pageNumber = 42
    )

    @Test
    fun `the Arabic card names the surah in Arabic`() {
        mount(language = "Arabic", continueAt = alBaqarah255)
        composeTestRule.onNodeWithText("البقرة 2:255").assertExists(
            "the Arabic card does not name 2:255 by its Arabic name"
        )
    }

    @Test
    fun `the English card names the surah in English`() {
        mount(language = "English", continueAt = alBaqarah255)
        composeTestRule.onNodeWithText("Al-Baqarah 2:255").assertExists(
            "the English card does not name 2:255 by its transliteration"
        )
    }
}
