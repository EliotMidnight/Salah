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
 * ### Why it was four
 *
 * `ScreenScaffold` and `ScreenTopBar` asked `WindowInsets.statusBars` for the top
 * and nothing else, so on a device with a cutout that reaches the top edge their
 * titles sat under it. The Today screen, the Quran reader and the reader's page
 * insets each had their own copy of the correct `max(statusBars, cutout)`
 * computation, and a fifth - a `CameraHoleSpacer` - was another copy that nothing had
 * called since the frame was consolidated.
 */
object SafeArea {

    /**
     * The room to leave at the top of the drawing, in pixels.
     */
    fun topPx(statusBars: WindowInsets, cutout: WindowInsets, density: Density): Int =
        maxOf(statusBars.getTop(density), cutout.getTop(density))

    /**
     * The room to leave at **each** side of the drawing, in pixels.
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
