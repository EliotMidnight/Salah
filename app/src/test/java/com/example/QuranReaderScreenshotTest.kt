package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Ayah
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranPaperTone
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.QuranScrollDirection
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
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
    private var ayahNumber = 1

    private fun state(ayahNumber: Int = this.ayahNumber) = SalahUiState()

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

    /**
     * Point the reader at a mushaf page rather than at a surah.
     *
     * Needed for the page cases, because they cannot be reached by choosing a surah:
     * the surahs that start part-way down a page are not the surah the page is named
     * after. The position is the page's own first verse, which is what a page turn
     * writes, so this is the same thing a reader gets by turning there.
     */
    private fun render(
        options: QuranReadingOptions,
        dark: Boolean,
        name: String,
        surah: Int = surahNumber,
        page: Int? = null,
        language: String = "English"
    ) {
        if (page != null) {
            val ref = QuranBrowse.placeAtPage(page).verse
            openSurah(ref.surah)
            ayahNumber = ref.ayah
        } else {
            openSurah(surah)
        }
        composeTestRule.setContent {
            SalahTheme(darkTheme = dark) {
                ProvideAppLanguage(language = language) {
                    QuranReader(
                        state = state(ayahNumber).copy(language = language),
                        // The position the destination would own, pointed at the surah
                        // and verse under test. It used to be reached through three
                        // state fields, which is why a screenshot could pin a surah and
                        // a page without anything in the tree having a single "where am
                        // I" to disagree with.
                        position = ReaderPosition(
                            QuranRef(
                                surahNumber,
                                ayahNumber,
                                QuranBrowse.pageOf(surahNumber, ayahNumber)
                            )
                        ),
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

    // --- Landing in the middle of a surah ---------------------------------

    @Test
    fun continuous_vertical_resumes_midway() = render(
        QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS),
        dark = false,
        name = "quran_continuous_vertical_resumes_midway",
        // 2:200, the middle of Al-Baqarah. Entering the continuous layout used to
        // open at 2:1 whatever the position was - and then *wrote* 2:1 to Continue
        // Reading half a second later, so the wrong place was not merely shown, it
        // was saved. This baseline is the reader arriving where they were.
        page = 42
    )

    @Test
    fun continuous_vertical_per_verse_resumes_midway() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true
        ),
        dark = false,
        name = "quran_continuous_vertical_per_verse_resumes_midway",
        // Far enough in to be past the first screenful in both shapes, and on a
        // block boundary, so a wrong block index is visible rather than plausible.
        page = 42
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

    // --- A surah that starts part-way down the page ----------------------

    @Test
    fun per_page_vertical_surah_starts_mid_page() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = false,
        name = "quran_per_page_vertical_surah_starts_mid_page",
        // Page 106 ends An-Nisa with 4:176 and then carries 5:1, so Al-Ma'idah
        // begins part-way down it. There are 45 surahs in this position, spread
        // over 42 pages, and the rule that printed a surah's name only when the
        // page *opened* one left every one of them unnamed - on the page where they
        // start, which is the one page where the name is needed.
        page = 106
    )

    // --- RTL --------------------------------------------------------------

    @Test
    fun per_page_vertical_rtl() = render(
        QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE),
        dark = false,
        name = "quran_per_page_vertical_rtl",
        language = "ar"
    )

    @Test
    fun per_page_horizontal_rtl() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.PER_PAGE,
            scroll = QuranScrollDirection.HORIZONTAL
        ),
        dark = false,
        name = "quran_per_page_horizontal_rtl",
        language = "ar"
    )

    @Test
    fun continuous_vertical_rtl() = render(
        QuranReadingOptions(layout = QuranReadingLayout.CONTINUOUS),
        dark = false,
        name = "quran_continuous_vertical_rtl",
        language = "ar"
    )

    @Test
    fun continuous_horizontal_rtl() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL
        ),
        dark = false,
        name = "quran_continuous_horizontal_rtl",
        // The horizontal axis is the case worth having in RTL: a right-to-left surface
        // that starts at the *left* edge opens in the middle of a wide measure, so the
        // first words of the surah are off to the right and the reader lands in the
        // middle of a line.
        language = "ar"
    )

    @Test
    fun continuous_vertical_per_verse_rtl() = render(
        QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            perVerse = true,
            showTranslation = true
        ),
        dark = false,
        name = "quran_continuous_vertical_per_verse_rtl",
        // Per-verse plus translation, because that is the layout with the most chrome
        // per verse: a reference chip, the Arabic, and an English translation. Every
        // one of those has to mirror, and the translation has to stay LTR while the
        // page around it is RTL - which is the whole of the Arabic/English problem in
        // one surface.
        language = "ar"
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
                        position = ReaderPosition(
                            QuranRef(
                                surahNumber,
                                ayahNumber,
                                QuranBrowse.pageOf(surahNumber, ayahNumber)
                            )
                        ),
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
