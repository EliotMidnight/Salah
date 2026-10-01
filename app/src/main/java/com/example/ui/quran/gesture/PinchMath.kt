package com.example.ui.quran.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * The geometry of magnifying a reading surface, and moving it afterwards.
 *
 * ### Why this is arithmetic in a file of its own
 *
 * All of it is a few lines of arithmetic that the reader calls on every frame of a
 * pinch. Being in a composable meant none of it could be checked: a pan that is
 * too loose lets the reader drag the page into empty space, and a clamp that is
 * too tight makes a magnified page immovable. Neither is visible in a screenshot.
 *
 * ### What the scale is *not*
 *
 * Magnification is a property of the **view**, not of the reading, and it is
 * deliberately not persisted. Reopening the reader should show the text at the
 * size the reader chose, not at whatever magnification they happened to leave
 * behind. Changing layout resets it too: a magnification chosen for a page of
 * dense text makes no sense over one verse per screen.
 */
object PinchMath {

    /**
     * How far a pinch may wobble and still be treated as no pinch at all.
     *
     * A **deviation**, not a ratio: 0.004 means "within 0.4% of unchanged". Stated
     * that way because the distinction is load-bearing and the wrong reading is
     * silent - compare a ratio against 0.004 and every real zoom is rejected,
     * which looks like a dead gesture rather than like a bug.
     */
    const val DEAD_ZONE = 0.004f

    /**
     * Applies one event's relative [zoomChange] to [current].
     *
     * `detectTransformGestures` reports zoom **relative to the previous event**,
     * not to the start of the gesture, so the only correct application is
     * `current * zoomChange` with `current` read fresh each time. Capturing the
     * scale in the lambda instead - the obvious version - makes every event
     * multiply by a stale value, so a ten-finger splay lands about where a
     * two-finger one did.
     *
     * [DEAD_ZONE] rejects the sub-percent wobble that every real pinch produces,
     * which otherwise nudges the reader's stored preference on a gesture they
     * thought of as a scroll.
     */
    fun applyZoom(current: Float, zoomChange: Float): Float {
        if (!zoomChange.isFinite() || zoomChange <= 0f) return current
        // The dead zone is a deviation from 1, so it is compared as one.
        if (abs(zoomChange - 1f) < DEAD_ZONE) return current
        return current * zoomChange
    }

    /**
     * How far a magnified surface can be moved, per axis.
     *
     * A `graphicsLayer` scale is applied about the centre, so at [scale] the
     * content is `size * scale` and the surplus revealed on each axis is
     * `size * (scale - 1) / 2`. At 1x the view already fills the screen, so the
     * offset must be pinned to zero - otherwise returning to 1x leaves the page
     * shoved to one side beside a strip of blank paper, and the reader has no way
     * to know why.
     */
    fun surplus(scale: Float, containerSize: IntSize): Offset {
        if (scale <= 1f) return Offset.Zero
        val factor = (scale - 1f) / 2f
        return Offset(
            x = containerSize.width * factor,
            y = containerSize.height * factor
        )
    }

    /**
     * Clamps a pan to what the current scale can actually reveal.
     *
     * A negative scale or a zero/unmeasured size yields zero rather than throwing
     * or producing `NaN` from a negative coercion bound - and the zero case is
     * hit on the very first frame, before the first layout pass.
     */
    fun clampPan(pan: Offset, scale: Float, containerSize: IntSize): Offset {
        val max = surplus(scale, containerSize)
        return Offset(
            x = pan.x.coerceIn(-max.x, max.x),
            y = pan.y.coerceIn(-max.y, max.y)
        )
    }

    /**
     * The offset that keeps magnified content under the fingers.
     *
     * A `graphicsLayer` scale is applied about the centre, so a pinch whose
     * centroid is not the centre displaces the content by
     * `(scale - 1) * (centroid - centre)` on each axis. Cancelling that leaves the
     * point between the fingers where it was, and the reader looking at what they
     * were already reading - which is the reason a magnified mushaf so often feels
     * like it jumped.
     *
     * Nothing at 1x, because nothing has been displaced and a correction there
     * would itself be a jump.
     */
    fun focalCorrection(
        centroid: Offset,
        scale: Float,
        containerSize: IntSize
    ): Offset {
        if (scale <= 1f) return Offset.Zero
        if (containerSize.width == 0 || containerSize.height == 0) return Offset.Zero
        val centre = Offset(containerSize.width / 2f, containerSize.height / 2f)
        val delta = centroid - centre
        return Offset(
            x = -delta.x * (scale - 1f),
            y = -delta.y * (scale - 1f)
        )
    }
}
