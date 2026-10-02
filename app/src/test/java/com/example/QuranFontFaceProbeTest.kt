package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.QuranFontFace
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.reader.MushafPage
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.SalahTheme
import com.example.ui.theme.Space
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every bundled face, on the densest page in the book.
 *
 * ### What this is for
 *
 * A face is a typographic decision, and the decision is [QuranFontFace.lineHeightFactor] -
 * the only per-face adjustment, and the one that decides whether a page's lines have
 * room for their marks. Nothing in the logic suite can see it: a line box that is half
 * a point too short is a perfectly valid `TextStyle` and a perfectly valid `PageFit`
 * measurement, and the page still draws. It fails only as a picture.
 *
 * Which is why this is a baseline per face rather than an assertion, and why the page
 * is **page 2**. Al-Baqarah at page 2 is the densest thing in the mushaf; a short
 * page like Al-Ikhlas has room to spare in every face and so cannot distinguish a
 * correct leading from a wrong one.
 *
 * ### The render pass that deleted the baseline shift
 *
 * `QuranFontFace` used to carry a `baselineShiftSp` that was never applied. These same
 * five renders were used to test applying it, and it made every face worse - the text
 * rises inside its line, which is where Arabic's marks are - so the field was deleted
 * rather than wired up. This file is the record of why, in a form a reviewer can look
 * at.
 *
 * Every face in [QuranFontFace] is covered, and the count is asserted, so a face
 * added to the picker without a baseline here is a visible omission.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel4, sdk = [36])
class QuranFontFaceProbeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The densest page in the book. See the class doc for why this one. */
    private val densestPage = 2

    /** Records under the face's own key, so the file name cannot drift from the enum. */
    private fun render(face: QuranFontFace) {
        composeTestRule.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = "English") {
                    QuranFonts.Provide(face) {
                        Box(Modifier.fillMaxSize()) {
                            MushafPage(
                                pageNumber = densestPage,
                                requestedScale = 1f,
                                selected = null,
                                ink = Color.Black,
                                accent = Color(0xFF1B5E9B),
                                onSelectVerse = {},
                                // A fixed top reserve, so the only thing that differs
                                // between the five images is the face.
                                topInset = Space.current.xl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/screenshots/quran_font_${face.key}.png")
    }

    @Test
    fun `Amiri - the default face`() = render(QuranFontFace.AMIRI)

    @Test
    fun `Amiri Quran - the Quranic cut, least leading of the set`() =
        render(QuranFontFace.AMIRI_QURAN)

    @Test
    fun `Lateef - the densest face`() = render(QuranFontFace.LATEEF)

    @Test
    fun `Scheherazade New`() =
        render(QuranFontFace.SCHEHERAZADE_NEW)

    @Test
    fun `Harmattan - the deepest descender, most leading of the set`() =
        render(QuranFontFace.HARMATTAN)

    @Test
    fun `every face the reader offers has a rendered baseline`() {
        // So that a face added to the picker is a *visible* omission rather than a
        // silent one. The names have to match what `render` writes.
        val expected = QuranFontFace.entries.map { "quran_font_${it.key}" }.toSet()
        val recorded = java.io.File("src/test/screenshots")
            .listFiles { f -> f.name.startsWith("quran_font_") && f.name.endsWith(".png") }
            .orEmpty()
            .map { it.name.removeSuffix(".png") }
            .toSet()

        assertEquals(
            "the recorded font baselines and the faces the reader offers have drifted " +
                "apart. A face with no baseline renders into a picture nobody has " +
                "looked at.\n  missing: ${expected - recorded}\n  stale: " +
                "${recorded - expected}",
            expected,
            recorded
        )
    }
}
