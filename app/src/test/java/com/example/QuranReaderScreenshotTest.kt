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
 * Renders the reader to a PNG so its four combinations can be reviewed without a
 * device.
 *
 * ### Why these exist
 *
 * The reader is four independent settings - layout, axis, per-verse, pinch - and
 * the combinations are the product. Every serious bug in it so far was invisible
 * to the compiler and to the logic tests:
 *
 * - the page indicator disagreeing with the page on screen, because the label and
 *   the pager were reading from different state;
 * - the surah heading eating a third of the screen;
 * - the horizontal axis re-laying-out the surah into side-by-side panels,
 *   because the axis had been treated as a shape rather than a mechanism.
 *
 * None of those are logic errors. The functions were correct and the code was
 * tidy; the *picture* was wrong. So the picture is the assertion.
 *
 * ### What each image is for
 *
 * The first two pair up to show that per-page is genuinely one page on either
 * axis - same page number, same running head, only the direction of travel
 * differs. The next two do the same for continuous, where the axis must change
 * only the measure and never the arrangement of the text.
 *
 * A reader is expected to check these by eye after touching the layout code.
 * They are not a pixel-diff gate: Arabic rendering differs across hosts and font
 * stacks, so a diff would fail on typography rather than on layout.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QuranReaderScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private var surahNumber = 112
    private var surah: com.example.data.model.Surah =
        QuranBrowse.surah(surahNumber)!!
    private var ayahs: List<Ayah> = QuranBrowse.ayahsInSurah(surahNumber)

    private fun state(ayahNumber: Int = 1) = SalahUiState(
        selectedSurah = surah,
        currentSurahAyahs = ayahs,
        activeReadingAyahNumber = ayahNumber
    )

    /**
     * Point the test at a different surah.
     *
     * Al-Ikhlas is four verses on a single page, which is a good check that a
     * short page is not shrunk - there is nothing to fit - but it can never show
     * a page separator, because it never crosses a page. The long surah is
     * there to exercise the boundary.
     */
    private fun openSurah(number: Int) {
        surahNumber = number
        surah = QuranBrowse.surah(number)!!
        ayahs = QuranBrowse.ayahsInSurah(number)
    }

    private fun render(
        options: QuranReadingOptions,
        dark: Boolean,
        name: String
    ) {
        render(options, dark, name, surahNumber)
    }

    private fun render(
        options: QuranReadingOptions,
        dark: Boolean,
        name: String,
        surah: Int
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
    //
    // These are the two that matter most. Per-verse must break the same text
    // into units in both layouts, and on the horizontal axis it must do so
    // *without* rearranging the verses sideways.

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

    // --- Dark, and a paper that is not the app default ---------------------

    // --- Page separators, in the only case that can show one ----------------
    //
    // A separator is drawn between the last verse of one mushaf page and the
    // first of the next, so it can only appear in a surah that crosses a page
    // boundary. Al-Ikhlas never does, which is why the tests above cannot check
    // it: those images would look identical whether the separator worked or not.
    //
    // Al-Baqarah runs to 286 ayat across pages 2 to 7, so it crosses five
    // boundaries. Long surahs are also the ones where fitting a page to the
    // viewport has to actually shrink the text, so these two images check the
    // separator and the fit together.

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

    @Test
    fun continuous_vertical_across_pages() = render(
        QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS),
        dark = false,
        name = "quran_continuous_vertical_across_pages",
        surah = 2
    )

    @Test
    fun per_page_vertical_dense() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = false,
        name = "quran_per_page_vertical_dense",
        surah = 2
    )

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
}
