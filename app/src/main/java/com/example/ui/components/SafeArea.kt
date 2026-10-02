package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Where the display is obstructed.
 *
 * The app draws edge to edge, so the system bars and the display cutout are *behind*
 * the content and every screen has to keep its own text out of them. That makes "how
 * much room does the hardware take" a fact the whole app needs, and it used to be
 * recomputed in four places.
 *
 * ### Why it was four
 *
 * `ScreenScaffold` and `ScreenTopBar` asked `WindowInsets.statusBars` for the top
 * and nothing else, so on a device with a cutout that reaches the top edge their
 * titles sat under it. The Today screen, the Quran reader and the reader's page
 * insets each had their own copy of the correct `max(statusBars, cutout)`
 * computation, and a fifth - a `CameraHoleSpacer` - was another copy that nothing had
 * called since the frame was consolidated.
 *
 * So the two screens that were supposed to agree about where content starts had two
 * different answers, and the one that was right was the one whose *name* never
 * mentioned the cutout - which is a good part of why the wrong ones were written.
 * The name is as much of the fix as the body is: [top] says what it measures, so
 * there is nothing to misread it as.
 *
 * ### The sides, which is the half that was missing everywhere
 *
 * [sides] had no implementation at all. In portrait a cutout is at the top, so a
 * top-only answer is complete; rotated, it moves to the left or right edge, where it
 * is 30-40dp deep against a 16dp design gutter - so the outer column of every page of
 * Arabic was under the notch, and the outer column in right-to-left text is the one
 * a page *begins* with. A reader lost the start of the page, which is a wrong reading
 * rather than a crowded one.
 *
 * Deliberately **not** `WindowInsets.safeDrawing`. That includes the navigation bar,
 * and the app has its own bottom navigation already positioned against it - honouring
 * both is how a layout ends up reserving the same 48dp twice. This is the cutout and
 * the status bar, which is exactly what the hardware puts *over* a drawing and
 * nothing else.
 *
 * ### Why the arithmetic is not inside the `@Composable`s
 *
 * [topPx] and [sidesPx] are plain functions over values, and the composables are
 * one-liners that read the platform and delegate. That split is what makes this
 * testable at all: a Robolectric window reports no display cutout, so a test that
 * composed the real thing would see zero, pass, and prove nothing about a defect
 * that only appears on hardware with a hole in it.
 */
object SafeArea {

    /**
     * The room to leave at the top of the drawing, in pixels.
     *
     * The taller of the status bar and the top of the display cutout, because a
     * cutout can be deeper than the bar it sits inside - in which case the bar's own
     * height is not the constraint - and because a status bar can be hidden while
     * the cutout remains, which is the transient case this exists for.
     */
    fun topPx(statusBars: WindowInsets, cutout: WindowInsets, density: Density): Int =
        maxOf(statusBars.getTop(density), cutout.getTop(density))

    /**
     * The room to leave at **each** side of the drawing, in pixels.
     *
     * One number for both sides, not a start and an end, and that is a requirement
     * rather than a simplification. Every surface that reserves this does so with one
     * symmetric `padding(horizontal = ...)`, and the two reading surfaces subtract
     * that same value from a tap's x coordinate to move from node space into text
     * space. Symmetry is what makes that subtraction correct; padding each side by its
     * own cutout inset would be more precise and would silently break tap
     * hit-testing on the side that had the notch, resolving taps to the wrong verse.
     * See `PageInsets.gutter`.
     *
     * Both edges are asked for and the larger wins, so the answer does not depend on
     * [layoutDirection]. `WindowInsets.getLeft` resolves *against* the layout
     * direction, so reading only one of them would report a different physical edge
     * in RTL from the one it reports in LTR - and the mushaf lays its page out RTL
     * regardless of the interface language, so that is not a rare configuration.
     */
    fun sidesPx(
        cutout: WindowInsets,
        density: Density,
        layoutDirection: LayoutDirection
    ): Int = maxOf(
        cutout.getLeft(density, layoutDirection),
        cutout.getRight(density, layoutDirection)
    )

    @Composable
    fun top(): Dp = with(LocalDensity.current) {
        topPx(
            statusBars = WindowInsets.statusBars,
            cutout = WindowInsets.displayCutout,
            density = this
        ).toDp()
    }

    @Composable
    fun sides(): Dp {
        val density = LocalDensity.current
        return with(density) {
            sidesPx(
                cutout = WindowInsets.displayCutout,
                density = density,
                layoutDirection = LocalLayoutDirection.current
            ).toDp()
        }
    }
}
