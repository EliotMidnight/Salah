package com.example.ui.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The reader's control row does not stand a control a line taller than its
 * neighbours, whatever the pill's label wraps to.
 *
 * ### The defect this file exists for
 *
 * The location pill invites a surah *range* - page 106 reads "An-Nisa to
 * Al-Ma'idah" - and the label wraps when the range is longer than the pill's
 * share of the row. The pill's 8dp inner padding then measured a wrapped label
 * at 56dp against the 48dp touch target the round controls beside it hold: the
 * pill rose a full 8dp above its neighbours and the top of the reading page was
 * two uneven controls high - on exactly the pages whose label stretched, and on
 * no other (the one-line pages keep the pill smaller than the target, so the
 * `heightIn` floor made them even already).
 *
 * The pill's inner padding is now 4dp, which brings two `titleSmall` lines
 * within the text's own line-box rounding of the 48dp target - about a dp, a
 * rounding no reader sees as raggedness and which the *label* may not be
 * flattened (to one line) to hide. So this test denies the pill anything more
 * than that rounding above its neighbour, rather than pixel equality: the
 * source of the remaining difference is the glyph raster's line rounding, not
 * the layout, and a tolerance narrower than it would be a test of the rasterizer.
 *
 * The NATIVE graphics mode is load-bearing: under the legacy one the label lays
 * out one line even here and the row measures even, so nothing can fail.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ControlRowHeightTest {

    @get:Rule
    val compose = createComposeRule()

    /** What the reader would hold, and what a request is delivered into. */
    private val state = mutableStateOf(SalahUiState())

    private fun mount() {
        compose.setContent {
            SalahTheme {
                ProvideAppLanguage(language = "English") {
                    QuranScreen(
                        state = state.value,
                        onOpen = {},
                        onOpenConsumed = {
                            state.value = state.value.copy(pendingOpen = null)
                        },
                        onAyahViewed = {},
                        onToggleBookmark = {},
                        onTogglePlayAyah = {},
                        onStopAudio = {},
                        onCycleSpeed = {},
                        onCycleRepeat = {},
                        onOptionsChange = {},
                        onImmersiveChange = {}
                    )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("reader_index").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    @Test
    fun `a wrapped range label does not stand the pill a line above the controls beside it`() {
        mount()
        // Page 106 ends An-Nisa with 4:176 and opens Al-Ma'idah at 5:1, so the
        // pill reads "An-Nisa to Al-Ma'idah Page 106" - a range that cannot fit
        // one line. Turn the reader there the way a request arrives.
        state.value = state.value.copy(pendingOpen = QuranBrowse.placeAtPage(106).verse)
        compose.waitForIdle()

        val pill = compose.onNodeWithTag("reader_index").fetchSemanticsNode()
        val save = compose.onNodeWithTag("reader_save_location").fetchSemanticsNode()

        // Fixture guard: if the label ever stops naming the range, this state is
        // no longer one where the pill has anything to wrap, and the heights the
        // reader never sees side by side would make the assertion pass for
        // nothing.
        val reported = pill.config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            .orEmpty()
        assertTrue(
            "fixture: the pill should carry the page's surah range, which is " +
                "what wraps; it reports \"$reported\"",
            " to " in reported
        )

        // The pill sits its wrapped content two titleSmall lines high against the
        // touch target. The only height over the target left to it is the glyph
        // raster's line-box rounding; two dp covers it with room for a rounding
        // bound or two, and what the old padding produced - a full 8dp line - is
        // far past it.
        val overshoot = pill.size.height - save.size.height
        val slack = compose.density.run { 2.dp.toPx() }.roundToInt()
        assertTrue(
            "the pill measured ${pill.size.height}px against the control beside " +
                "it at ${save.size.height}px - anything beyond ${slack}px means " +
                "the wrapped label stands the pill a line taller than the rest " +
                "of the row, and the top of the reading page comes out ragged " +
                "on exactly the pages whose range stretched",
            overshoot in 0..slack
        )
    }
}
