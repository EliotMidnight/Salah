package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.QuranScrollDirection
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.ui.SalahUiState
import com.example.data.quran.MushafLayout
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.QuranReader
import com.example.ui.quran.reader.ReaderPosition
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
 * Renders the Madinah mushaf's own page to a PNG so its shape can be reviewed
 * without a device.
 *
 * ### Why these exist
 *
 * This surface is a fixed object: a 3:4 sheet, the print's fifteen rows, the
 * print's own line breaks, its justification. The logic that puts ink there is
 * covered by corpus-wide tests ([MushafLayoutTest], [MadinahTextTest]); what
 * no logic test can say is whether the *picture* is the print's picture -
 * whether the rows are evenly led, whether a centred header reads as centred,
 * whether a justify pass pushed a row past its margin, whether the ornament
 * size recedes as a printed ornament does. Those are exactly the failures that
 * slipped past the compiler in the flowing reader, so here too the picture is
 * the assertion.
 *
 * The three pages chosen are where the shape is hardest to get right:
 *
 * - **page 1** - the print's own opening page: no basmalah row, eight rows
 *   where every other page has fifteen, all of them centred, so the pitch the
 *   sheet leads by is doubled and nothing in it may be stretched.
 * - **page 2** - the standard page: the surah's name row and basmalah row on
 *   one sheet, then full-justified text, in both scroll directions.
 * - **page 528** - dense, and the page whose wrapped ornament sits directly
 *   above a surah's name row, which is the arrangement most likely to make a
 *   justified row and a centred row argue about the margin.
 *
 * A reader is expected to check these by eye after touching the page code.
 * They are not a pixel-diff gate: Arabic rendering differs across hosts and
 * font stacks, so a diff would fail on typography rather than on layout.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class MadinahPageScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    /**
     * Render one of the print's pages, positioned the way a page turn writes
     * it: the page's own opener, attributed by this print. The reader's entry
     * re-attribution is idempotent for a reference that already carries the
     * print's pages, so what lands on screen is exactly the page the print
     * numbers.
     */
    private fun render(
        page: Int,
        name: String,
        dark: Boolean = false,
        language: String = "English",
        scroll: QuranScrollDirection = QuranScrollDirection.VERTICAL
    ) {
        composeTestRule.setContent {
            SalahTheme(darkTheme = dark) {
                ProvideAppLanguage(language = language) {
                    QuranReader(
                        state = SalahUiState().copy(language = language),
                        position = ReaderPosition(MushafLayout.openingVerse(page)),
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        onCycleSpeed = {},
                        onCycleRepeat = {},
                        options = QuranReadingOptions(
                            layout = QuranReadingLayout.MADINAH,
                            scroll = scroll
                        ),
                        onOptionsChange = {},
                        immersive = false,
                        onImmersiveChange = {},
                        onOpenIndex = {},
                        onOpenOptions = {}
                    )
                }
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/screenshots/quran_madinah_$name.png")
    }

    @Test
    fun madinah_vertical_first_page() = render(
        // The print's opening page: eight centred rows, no basmalah row, the
        // pitch of every other page doubled because the sheet still leads the
        // rows over its full height.
        page = 1,
        name = "vertical_first_page"
    )

    @Test
    fun madinah_vertical_baqarah_opening() = render(
        // The standard page: name row, basmalah row, then justified text.
        page = 2,
        name = "vertical_baqarah_opening"
    )

    @Test
    fun madinah_horizontal_baqarah_opening() = render(
        // The same page, on the horizontal axis: the axis turns the pager,
        // never the sheet, the pitch or the justification.
        page = 2,
        name = "horizontal_baqarah_opening",
        scroll = QuranScrollDirection.HORIZONTAL
    )

    @Test
    fun madinah_vertical_dense() = render(
        // Dense and the page whose wrapped ornament sits directly above a
        // surah's name row.
        page = 528,
        name = "vertical_dense"
    )

    @Test
    fun madinah_vertical_dark() = render(
        // The same sheet on the dark paper: ink and accents re-answer, the
        // sheet's proportions cannot.
        page = 2,
        name = "vertical_dark",
        dark = true
    )
}
