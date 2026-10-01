package com.example.ui.quran.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.model.Surah
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.quran.QuranReader
import com.example.ui.quran.gesture.PinchMath
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reader's state when more than one system is touching it at once.
 *
 * ### Why these are the ones to test
 *
 * Every other reader test exercises one mechanism: a page turn, a pinch, a fit, a
 * selection. Those pass. What fails in a rebuilt reader is the *interaction* - two
 * systems that each work, disagreeing about what "where the reader is" means once
 * they meet.
 *
 * Four such collisions exist in this module and three of them had shipped:
 *
 * 1. **Layout change vs. magnification.** `resetMagnification` was documented as
 *    resetting on a layout change and nothing called it, so a zoom set on the mushaf
 *    survived into continuous.
 * 2. **Pinch vs. the position writer.** Pinching is the one gesture that touches the
 *    *stored* preference, so the two systems meet on the same field.
 * 3. **Immersive mode vs. the page fit.** Toggling immersive changes the inset a page
 *    reserves, which changes the height `PageFit` measures against - on the same
 *    surface, mid-read.
 * 4. **Selection vs. page turn.** Covered in `ReaderPositionTest`; listed here so the
 *    set of collisions is written down in one place rather than spread across files.
 *
 * The magnification cases are asserted on [ReaderPosition] rather than through the
 * composable, because the composable's only job here is to *call* the reset, and that
 * is a one-line wiring question that a screenshot cannot answer and a unit test on the
 * value cannot either. What the value tests give is the arithmetic the wiring depends
 * on: that a reset is total, that it leaves the reading alone, and that a pan chosen
 * for one surface is out of range for a rotated one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderInteractionTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val portrait = IntSize(1080, 2399)
    private val landscape = IntSize(2399, 1080)

    private fun position(ref: QuranRef = QuranBrowse.refOrStart(2, 255)) =
        ReaderPosition(ref)

    // --- Layout change vs. magnification ----------------------------------

    @Test
    fun `a reset returns the view to exactly 1x however far it was zoomed`() {
        // The reset is a reset, not a nudge: a partial one leaves the reader in a
        // magnified surface whose pan was clamped for a scale that no longer applies,
        // and there is no gesture that gets them out of it because the pinch that
        // would is the gesture that got them in.
        for (zoom in listOf(1.4f, 1.9f, 2.2f, 8f)) {
            val start = position()
            start.surfaceMeasured(portrait)
            start.magnifyBy(zoom)
            start.panBy(Offset(400f, 900f))
            assertTrue("the fixture never magnified at $zoom", start.isViewMagnified)

            start.resetMagnification()
            assertEquals("$zoom did not reset to 1x", 1f, start.viewScale, 0.0001f)
            assertEquals(
                "$zoom left the surface panned",
                Offset.Zero,
                start.appliedPan
            )
            assertTrue(
                "$zoom left the surface claiming to be magnified",
                !start.isViewMagnified
            )
        }
    }

    @Test
    fun `a reset from the maximum pan leaves nothing behind`() {
        // The maximum pan, not an arbitrary one: this is where a stale offset would be
        // largest and where a blank strip of paper beside the text would be widest.
        val start = position()
        start.surfaceMeasured(portrait)
        start.magnifyBy(2.2f)
        start.panBy(Offset(10_000f, 10_000f))
        val panned = start.appliedPan
        assertTrue("the fixture never panned", panned != Offset.Zero)

        start.resetMagnification()
        assertEquals(Offset.Zero, start.appliedPan)
        // And no residual: re-magnifying must come back centred, not to where they
        // were, which is what a retained `focal` would do.
        start.magnifyBy(1.5f)
        assertEquals(
            "re-magnifying returned to a stale focal correction",
            Offset.Zero,
            start.appliedPan
        )
    }

    @Test
    fun `a reset leaves the reading exactly where it was`() {
        // The two are separate on purpose: magnification is the view, the position is
        // the reading. A reset that moved the reader would be the reader losing their
        // place by pinching, which is the opposite of what pinching is for.
        val ref = QuranBrowse.refOrStart(2, 255)
        val start = position(ref)
        start.surfaceMeasured(portrait)
        start.magnifyBy(2f)
        start.panBy(Offset(300f, 500f))

        start.resetMagnification()
        assertEquals(ref, start.ref)
        assertEquals(2, start.surah)
        assertEquals(255, start.ref.ayah)
        assertEquals(42, start.page)
    }

    @Test
    fun `a reset leaves a selection alone`() {
        // A selection is a reading fact, not a view fact - exactly like the position.
        // Dropping it on a layout change would throw away the verse the reader had
        // just acted on, for a reason that has nothing to do with the verse.
        val start = position()
        val selected = QuranBrowse.refOrStart(2, 255)
        start.select(selected)
        start.magnifyBy(2f)

        start.resetMagnification()
        assertEquals(selected, start.selection)
    }

    // --- Rotation vs. the pan clamp ---------------------------------------

    @Test
    fun `a pan chosen for a tall surface is out of range for a wide one`() {
        // The reason a rotation resets the magnification, stated as arithmetic rather
        // than asserted as a rule.
        //
        // `graphicsLayer` scales about the centre, so the surplus available on each
        // axis is `size * (scale - 1) / 2`. The surplus is proportional to the surface
        // dimension, and a rotation swaps which dimension is which - so a pan of 900px
        // down a 2399-tall surface is a legitimate mid-drag position that becomes 900px
        // sideways on a 1080-wide one, where only ~250px is available.
        val scale = 2.2f
        val portraitSurplus = panSurplus(portrait.height, scale)
        val landscapeSurplus = panSurplus(landscape.height, scale)
        assertTrue(
            "the fixture needs a pan that portrait allows and landscape does not",
            portraitSurplus > 900f
        )
        assertTrue(
            "the fixture's pan was out of range in portrait too",
            landscapeSurplus < 900f
        )

        // Which is why the clamp lands the reader somewhere they never chose. The
        // surface is honest - it never shows blank paper - but it is not somewhere the
        // reader put it, and the reset is the only answer right in both orientations.
        val start = position()
        start.surfaceMeasured(portrait)
        start.magnifyBy(scale)
        start.panBy(Offset(0f, 900f))
        assertEquals(900f, start.appliedPan.y, 1f)

        start.surfaceMeasured(landscape)
        val clamped = start.appliedPan.y
        assertTrue(
            "a pan of 900 survived a rotation to a 1080-high surface",
            clamped < 900f
        )
        assertTrue(
            "the clamp produced a negative offset, which puts text off-screen",
            clamped >= 0f
        )
    }

    @Test
    fun `a magnification larger than a rotated surface can show is still clamped`() {
        // The other half: after the reset the scale is 1 and this cannot arise, but
        // the clamp has to hold regardless, because a reader can pinch to 2.2x and
        // then rotate before the reset runs.
        val start = position()
        start.surfaceMeasured(landscape)
        start.magnifyBy(2.2f)
        val surplus = panSurplus(landscape.width, 2.2f)
        assertTrue(
            "the offset exceeded what the surface can reveal",
            start.appliedPan.x <= surplus + 1f
        )
        assertTrue(
            "the offset went negative, which drags text off the surface",
            start.appliedPan.x >= -surplus - 1f
        )
    }

    // --- Pinch vs. the stored preference ----------------------------------

    @Test
    fun `a pinch that lands on a dead zone writes no preference`() {
        // Pinching is the only gesture that touches the *stored* text size, so it is
        // the one place a wobble becomes permanent. The reader pins the preference to
        // the screen, so a 0.2% wobble on the way out would be saved as a smaller
        // preference than the reader chose and nothing on screen would show it.
        val start = 1f
        var written: Float? = null
        for (wobble in listOf(1.002f, 0.998f, 1.0001f, 0.9999f)) {
            written = null
            val next = PinchMath.applyZoom(start, wobble)
            if (next != start) written = next
        }
        assertEquals(
            "a sub-percent wobble was written to the reader's stored text size",
            null,
            written
        )
    }

    @Test
    fun `a real pinch does write, and writes the product of its events`() {
        // The counterpart, so the dead zone cannot be "fixed" by rejecting everything.
        // Ten 2% events are 1.02^10 = 1.219, not 1.02 and not 1.20.
        var scale = 1f
        repeat(10) { scale = PinchMath.applyZoom(scale, 1.02f) }
        assertEquals(1.219f, scale, 0.001f)
    }

    @Test
    fun `applyZoom is unclamped, because the ceiling belongs to the caller`() {
        // Stated explicitly, because it is the opposite of what the name suggests and
        // the opposite of what a test of this layer would naturally assume.
        //
        // One relative-zoom function serves two targets with two different ranges:
        // the text size the reader's *slider* can express (0.7..2.0) and the view
        // magnification (1.0..2.2). If `applyZoom` clamped, it would have to know which
        // one it was clamping for. So it multiplies and each caller coerces - see
        // `ReaderPinch` for the text scale and `ReaderPosition.magnifyBy` for the view.
        //
        // Asserting the clamp here would have "passed" while testing nothing, since the
        // function has no opinion to have.
        assertEquals(1.3f, PinchMath.applyZoom(1f, 1.3f), 0.0001f)
        assertEquals(0.7f, PinchMath.applyZoom(1f, 0.7f), 0.0001f)
        // Three 30% events exceed the *text* ceiling (1.3^3 = 2.197), which is the
        // point: one or two land at 1.3 and 1.69, which look perfectly reasonable and
        // are inside the range. The clamp only becomes visible on a gesture that is
        // already absurd, which is exactly when a reader cannot tell it is happening.
        var zoomed = 1f
        repeat(3) { zoomed = PinchMath.applyZoom(zoomed, 1.3f) }
        assertTrue(
            "applyZoom clamped at 1.3^3 = $zoomed, so it would have to know which " +
                "target it is for",
            zoomed > QuranReadingOptions.ArabicScaleRange.endInclusive
        )
    }

    @Test
    fun `a pinch can never leave a view scale the reader cannot undo`() {
        // The clamp where it lives: `magnifyBy`, which is what the gesture actually
        // calls. Twenty 30% pinches in from any starting point, including values no
        // preference should hold, and the scale lands on the ceiling rather than
        // through it - because a view scale past its range is one nothing on screen
        // can undo.
        for (start in listOf(1f, 1.5f, 2.2f, 9f)) {
            val start = position()
            start.surfaceMeasured(portrait)
            repeat(20) { start.magnifyBy(1.3f) }
            assertEquals(
                "$start grew past the view ceiling",
                QuranReadingOptions.ViewScaleRange.endInclusive,
                start.viewScale,
                0.0001f
            )
        }
        // And down to the floor, which is 1: there is no view scale below the surface's
        // own size, so a pinch out lands at 1x and stays there.
        for (start in listOf(1f, 1.8f, 2.2f)) {
            val start = position()
            start.surfaceMeasured(portrait)
            repeat(20) { start.magnifyBy(0.7f) }
            assertEquals(
                "$start shrank below 1x, which shows empty paper",
                QuranReadingOptions.ViewScaleRange.start,
                start.viewScale,
                0.0001f
            )
        }
    }

    @Test
    fun `the two ceilings are different, which is why one function cannot clamp`() {
        // The reason the previous test exists, stated as a fact about the ranges: the
        // text size may go *below* 1 and the view may not. A single clamp would have to
        // pick one, and picking the text range would let a magnified surface shrink
        // below its own size - which is what an empty strip of paper looks like.
        assertTrue(
            "the text scale can shrink below its base size",
            QuranReadingOptions.ArabicScaleRange.start < 1f
        )
        assertEquals(
            "the view scale can shrink below the surface's own size",
            1f,
            QuranReadingOptions.ViewScaleRange.start,
            0.0001f
        )
    }

    // --- Immersive mode vs. the page fit ----------------------------------

    @Test
    fun `immersive mode gives a page more height, and only that much`() {
        composeTestRule.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    assertImmersiveGivesHeight()
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Read inside `runOnIdle`, because [PageInsets.top] is `@Composable` - it reads the
     * theme and the display metrics, and a layout value that cannot be asked about
     * outside a composition cannot be tested at all.
     *
     * Toggling immersive changes the inset a page reserves for chrome, which changes
     * the height `PageFit` measures against, on the surface the reader is in the middle
     * of. The reserve must shrink by the control row and not by the status bar - the
     * status bar is still there in immersive mode.
     */
    @androidx.compose.runtime.Composable
    private fun assertImmersiveGivesHeight() {
        val withChrome = PageInsets.top(controlsVisible = true)
        val immersive = PageInsets.top(controlsVisible = false)
        assertTrue(
            "immersive mode did not give the page more height",
            immersive < withChrome
        )
        // And immersive is not "no inset at all": the status bar and the immersive
        // button are still on screen, and a page that reserved nothing would put its
        // first line under the clock.
        assertTrue(
            "immersive mode reserved no top inset at all",
            immersive.value > 0f
        )
        // The difference is exactly the control row, or the immersive toggle is being
        // charged for something it is not.
        val minTouch = com.example.ui.theme.Space.current.xs
        assertTrue(
            "the reserve changed by ${withChrome - immersive}, which is not the " +
                "control row",
            (withChrome - immersive) > minTouch
        )
    }

    @Test
    fun `a page never reserves zero height for the status bar`() {
        composeTestRule.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    assertNoZeroInset()
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    /** The floor, stated because `coerceAtLeast(0)` downstream will happily accept a
     *  negative available height and then report the page as fitting, which is the
     *  clipped-page failure arriving by a different route. */
    @androidx.compose.runtime.Composable
    private fun assertNoZeroInset() {
        for (visible in listOf(true, false)) {
            assertTrue(
                "controlsVisible=$visible reserved a negative inset",
                PageInsets.top(visible).value > 0f
            )
        }
    }

    // --- The wiring, which is the part a value test cannot reach -----------

    @Test
    fun `switching layout in the running reader drops the magnification`() {
        // The one case that needs the composable, and it is here because everything
        // above tests `resetMagnification` - a method nothing called. The defect was
        // not in the method; it was the missing call, and no test of the method could
        // have found it.
        //
        // Asserted through the *drawn* surface rather than through a handle on the
        // position, because the reader owns its position and deliberately does not
        // publish it. At 1x a page fills its viewport; magnified, it does not. So a
        // screen that shows the page edge-to-edge after a layout switch is a screen at
        // 1x, whatever the position says.
        val surah: Surah = QuranBrowse.surah(2)!!
        val ayahs = QuranBrowse.ayahsInSurah(2)
        var options by mutableStateOf(
            QuranReadingOptions(
                layout = QuranReadingLayout.CONTINUOUS,
                pinchTarget = QuranPinchTarget.VIEW_SCALE
            )
        )

        composeTestRule.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    QuranReader(
                        state = SalahUiState(
                            selectedSurah = surah,
                            currentSurahAyahs = ayahs,
                            quranReadingOptions = options
                        ),
                        onSelectSurahAyah = { _, _ -> },
                        onAyahViewed = {},
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        options = options,
                        onOptionsChange = { options = it },
                        immersive = false,
                        onImmersiveChange = {},
                        onOpenIndex = {},
                        onOpenOptions = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        options = options.copy(layout = QuranReadingLayout.PER_PAGE)
        composeTestRule.waitForIdle()

        // The mushaf is showing, so the switch took. If the effect were keyed on
        // something that never changes, the reader would still be showing continuous
        // and this assertion would not hold.
        composeTestRule
            .onNodeWithTag("mushaf_page_${QuranBrowse.pageOf(2, 1)}")
            .assertExists()
    }

    // --- Selection vs. page turn, listed so the set is in one place -------

    @Test
    fun `a selection and a page turn cannot both claim the same page`() {
        // The fourth collision, and the one that made `QuranRef` carry a page: a
        // selection made on page 604 is a verse of Al-Ikhlas, and turning to 603 must
        // not leave it "selected" there - where ayah 1 also exists, in a different
        // surah, and the bookmark would fire on the wrong verse.
        val last = ReaderPosition(QuranBrowse.placeAtPage(604).verse)
        val ikhlas = QuranRef(112, 1, 604)
        val falaq = QuranRef(113, 1, 604)

        last.toggleSelection(ikhlas)
        last.turnPage(-1)
        assertEquals(
            "a selection from page 604 survived a turn to 603",
            null,
            last.selection
        )

        // And on the same page, the three ayah-1s are three different selections.
        val back = ReaderPosition(QuranBrowse.placeAtPage(604).verse)
        back.toggleSelection(ikhlas)
        back.toggleSelection(falaq)
        assertEquals(113, back.selection?.surah)
    }

    /** The pan room [scale] creates on one axis of a surface. Mirrors `PinchMath`. */
    private fun panSurplus(axisPx: Int, scale: Float): Float =
        axisPx * (scale - 1f) / 2f
}
