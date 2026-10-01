package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Ayah
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranPaperTone
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.QuranReader
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
 * Renders the reader to a PNG so its combinations can be reviewed without a device.
 *
 * ### Why these exist
 *
 * The reader is four independent settings - layout, axis, per-verse, pinch - and the
 * combinations are the product. Most of the serious bugs in it were invisible to the
 * compiler and to the logic tests:
 *
 * - the horizontal continuous measure being *infinite*, so each verse was set as one
 *   unwrapped line running off both edges - and the committed baseline for that mode
 *   had recorded the broken picture as correct;
 * - the surah heading printing a basmalah the verses already contained;
 * - a fitted page that was never actually drawn at its fitted size;
 * - a prostration marker that selected a different verse.
 *
 * None of those are logic errors. The functions were correct and the code was tidy;
 * the *picture* was wrong. So the picture is the assertion.
 *
 * ### What each image is for
 *
 * The per-page pair shows that page mode is genuinely one page on either axis - same
 * page number, same running head, only the direction of travel differs. The
 * continuous pair shows that the axis changes the *measure* and never the
 * arrangement of the text.
 *
 * A reader is expected to check these by eye after touching the layout code. They are
 * not a pixel-diff gate: Arabic rendering differs across hosts and font stacks, so a
 * diff would fail on typography rather than on layout.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QuranReaderScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private var surahNumber = 112
    private var surah: Surah = QuranBrowse.surah(surahNumber)!!
    private var ayahs: List<Ayah> = QuranBrowse.ayahsInSurah(surahNumber)

    private fun state(ayahNumber: Int = 1) = SalahUiState(
        selectedSurah = surah,
        currentSurahAyahs = ayahs,
        activeReadingAyahNumber = ayahNumber
    )

    /**
     * Point the test at a different surah.
     *
     * Al-Ikhlas is four verses on one page, which is a good check that a short page
     * is not shrunk - there is nothing to fit - but it can never show a page
     * separator, because it never crosses a page. The long surah is there to
     * exercise the boundary, and Al-Baqarah is the densest thing in the book.
     */
    private fun openSurah(number: Int) {
        surahNumber = number
        surah = QuranBrowse.surah(number)!!
        ayahs = QuranBrowse.ayahsInSurah(number)
    }

    private fun render(
        options: QuranReadingOptions,
        dark: Boolean,
        name: String,
        surah: Int = surahNumber
    ) {
        openSurah(surah)
        composeTestRule.setContent {
            SalahTheme(darkTheme = dark) {
                ProvideAppLanguage(language = "English") {
                    QuranReader(
                        state = state(),
                        onSelectSurah = {},
                        onSelectSurahAyah = { _, _ -> },
                        onAyahViewed = {},
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        options = options,
                        onOptionsChange = {},
                        immersive = false,
                        onImmersiveChange = {},
                        onOpenIndex = {},
                        onOpenOptions = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    // --- Per-page: one canonical page, either axis ------------------------

    @Test
    fun per_page_vertical() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = false,
        name = "quran_per_page_vertical"
    )

    @Test
    fun per_page_horizontal() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.PER_PAGE,
            scroll = QuranScrollDirection.HORIZONTAL
        ),
        dark = false,
        name = "quran_per_page_horizontal"
    )

    // --- Continuous: the axis must change only the measure ----------------

    @Test
    fun continuous_vertical() = render(
        QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS),
        dark = false,
        name = "quran_continuous_vertical"
    )

    @Test
    fun continuous_horizontal() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL
        ),
        dark = false,
        name = "quran_continuous_horizontal"
    )

    // --- Per-verse on each surface ----------------------------------------

    @Test
    fun per_page_vertical_per_verse() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.PER_PAGE,
            perVerse = true
        ),
        dark = false,
        name = "quran_per_page_vertical_per_verse"
    )

    @Test
    fun continuous_vertical_per_verse() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true
        ),
        dark = false,
        name = "quran_continuous_vertical_per_verse"
    )

    @Test
    fun continuous_horizontal_per_verse() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true,
            scroll = QuranScrollDirection.HORIZONTAL
        ),
        dark = false,
        name = "quran_continuous_horizontal_per_verse"
    )

    // --- Page boundaries, and the one case that can show one --------------

    @Test
    fun continuous_vertical_across_pages() = render(
        QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS),
        dark = false,
        name = "quran_continuous_vertical_across_pages",
        surah = 2
    )

    @Test
    fun continuous_vertical_per_verse_across_pages() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true
        ),
        dark = false,
        name = "quran_continuous_vertical_per_verse_across_pages",
        surah = 2
    )

    // --- A dense mushaf page, which is where the fit has to work ----------

    @Test
    fun per_page_vertical_dense() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = false,
        name = "quran_per_page_vertical_dense",
        surah = 2
    )

    // --- Dark, and a paper that is not the app default --------------------

    @Test
    fun per_page_vertical_dark() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = true,
        name = "quran_per_page_vertical_dark"
    )

    @Test
    fun continuous_vertical_per_verse_dark_paper() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true,
            paper = QuranPaperTone.GREEN,
            showTranslation = true,
            arabicScale = 1.3f,
            font = QuranFontFace.LATEEF,
            pinchTarget = QuranPinchTarget.VIEW_SCALE
        ),
        dark = true,
        name = "quran_continuous_vertical_per_verse_dark_paper"
    )

    // --- Immersive --------------------------------------------------------

    @Test
    fun per_page_vertical_immersive() {
        openSurah(112)
        composeTestRule.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = "English") {
                    QuranReader(
                        state = state(),
                        onSelectSurah = {},
                        onSelectSurahAyah = { _, _ -> },
                        onAyahViewed = {},
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        options = QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
                        onOptionsChange = {},
                        immersive = true,
                        onImmersiveChange = {},
                        onOpenIndex = {},
                        onOpenOptions = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/quran_per_page_vertical_immersive.png")
    }
}
