package com.example.ui.quran

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingOptions
import com.example.ui.quran.gesture.PinchMath
import kotlin.math.abs

/**
 * How far a finger must travel before a swipe counts as a surah change.
 *
 * Authored in dp so it is the same physical distance on every screen, then
 * converted once to the pixels the drag reports in.
 */
private val SwipeThreshold = 72.dp

/**
 * Pinch to read, and drag to move around what was magnified.
 *
 * The two things a pinch can do look identical on screen and are not, so
 * [target] decides which one happens:
 *
 * - [QuranPinchTarget.TEXT_SIZE] changes the type. The line rewraps, the saved
 *   preference matches what is on screen, and it stays crisp at every size.
 * - [QuranPinchTarget.VIEW_SCALE] magnifies the whole reading surface. The
 *   type is untouched and no preference is written, because nothing about the
 *   reading changed - only how much of it fits.
 *
 * ### Why the scale is read through `rememberUpdatedState`
 *
 * `detectTransformGestures` reports `zoomChange` **relative to the previous
 * event**, not relative to the start of the gesture, so the only correct way to
 * apply it is `current * zoomChange` with `current` read fresh each time.
 *
 * Capturing the scale in the lambda instead - the obvious version - makes every
 * event multiply by a stale value, so a ten-finger splay lands at roughly the
 * same place as a two-finger one. Re-keying the `pointerInput` on the scale
 * fixes the staleness but tears the gesture detector down mid-pinch and drops
 * the accumulated zoom. Reading the live value through a state holder avoids
 * both: the detector is created once and the scale it applies against is
 * always the current one.
 *
 * ### Why [isMagnified] unlocks panning
 *
 * Once the view is magnified, one finger has to *move the page* - a magnified
 * view that cannot be moved is a view you have cropped yourself out of, and the
 * only way back is to zoom out again. So `panZoomLock` is off exactly while
 * magnified, and the pan is forwarded to [onPanChange].
 *
 * At 1x it stays on, and the two gestures keep out of each other's way: a
 * one-finger drag is decided to be a scroll, the zoom is pinned to 1 and this
 * block does nothing at all, so the list underneath behaves exactly as it always
 * did. Two fingers turn it into a zoom and the pan is dropped. A second finger
 * still zooms at any magnification.
 *
 * ### Why the pan also cancels the offset the zoom introduced
 *
 * `graphicsLayer` scales about the centre by default, so a pinch that is not
 * centred on the fingers moves the text *and* leaves the reader looking at a
 * different part of the page than the one they were reading. That is the reason
 * a magnified mushaf so often feels like it jumped.
 *
 * So the pinch centroid is tracked and, while magnifying, part of the movement
 * is cancelled against it: the content stays under the fingers instead of
 * drifting away from them. What is left is the panning the reader asked for.
 * This applies only under [QuranPinchTarget.VIEW_SCALE] - resizing the type
 * reflows it anyway, so there is nothing to cancel.
 */
internal fun Modifier.readerPinch(
    target: QuranPinchTarget,
    arabicScale: Float,
    onArabicScaleChange: (Float) -> Unit,
    viewScale: Float,
    onViewScaleChange: (Float) -> Unit,
    pan: Offset = Offset.Zero,
    onPanChange: ((Offset) -> Unit)? = null,
    onFocal: ((Offset) -> Unit)? = null,
    isMagnified: Boolean = viewScale > 1.01f
): Modifier = composed {
    val arabic = rememberUpdatedState(arabicScale)
    val view = rememberUpdatedState(viewScale)
    val onArabicChange = rememberUpdatedState(onArabicScaleChange)
    val onViewChange = rememberUpdatedState(onViewScaleChange)
    val livePan = rememberUpdatedState(pan)
    val onPan = rememberUpdatedState(onPanChange)
    val onFocalChange = rememberUpdatedState(onFocal)

    // Keyed on the target and on whether we are magnified, because
    // `panZoomLock` has to change when the view crosses 1x. It is deliberately
    // *not* keyed on the scale value - see above.
    pointerInput(target, isMagnified) {
        detectTransformGestures(panZoomLock = !isMagnified) { centroid, panChange, zoom, _ ->
            // Zoom first, so that the branch below sees the updated scale for
            // this event rather than the one before it.
            if (zoom != 1f) {
                when (target) {
                    QuranPinchTarget.TEXT_SIZE -> {
                        // The dead zone is applied to the *change*, not to the
                        // result. A real pinch jitters by a fraction of a percent on
                        // every event, and each of those was a write to the reader's
                        // stored text size - so a reader who pinched to look closer
                        // found their preference had drifted when they let go.
                        val next = PinchMath.applyZoom(arabic.value, zoom)
                            .coerceIn(QuranReadingOptions.ArabicScaleRange)
                        if (next != arabic.value) onArabicChange.value(next)
                    }

                    QuranPinchTarget.VIEW_SCALE -> {
                        val next = PinchMath.applyZoom(view.value, zoom)
                            .coerceIn(QuranReadingOptions.ViewScaleRange)
                        val changed = next != view.value
                        if (changed) onViewChange.value(next)

                        // Hold the content under the fingers - but only when the
                        // scale actually moved.
                        //
                        // The centroid is reported whether or not there is a pinch,
                        // so forwarding it unconditionally feeds a *one-finger drag*
                        // into the pan as well. The two would add up, and the reader
                        // would find the page sliding away from their finger as they
                        // dragged it - which reads as the page being broken rather
                        // than as two gestures fighting.
                        if (changed && panChange != Offset.Zero) {
                            onFocalChange.value?.invoke(panChange)
                        }
                    }
                }
            }

            // Only a magnified view pans, and only when a pan handler is
            // actually attached. Without the second condition this would
            // silently swallow horizontal drags in every other mode.
            if (panChange != Offset.Zero &&
                target == QuranPinchTarget.VIEW_SCALE &&
                isMagnified &&
                onPan.value != null
            ) {
                onPan.value?.invoke(livePan.value + panChange)
            }
        }
    }
}

/**
 * The offset that keeps magnified content under the fingers.
 *
 * A `graphicsLayer` scale is applied about the centre, so a pinch whose centroid
 * is not the centre displaces the content by `(scale - 1) * (centroid - centre)`
 * on each axis. Cancelling that leaves the point between the fingers where it was
 * and the reader looking at what they were already reading.
 *
 * Split out from the gesture detector so the arithmetic is testable without a
 * composition and without a pointer.
 */
internal fun focalCorrection(
    centroid: Offset,
    scale: Float,
    containerSize: IntSize
): Offset = PinchMath.focalCorrection(centroid, scale, containerSize)

/**
 * Swipe left or right to change surah.
 *
 * Fires once the finger has travelled a real distance rather than on every
 * frame, so a slightly-off gesture does not skip a surah and a deliberate one is
 * not left waiting for a release that never comes.
 *
 * A drag that is mostly vertical never reaches the threshold, which leaves the
 * scroll behaviour alone: `draggable` reports the axis it is interested in, and
 * the list claims the gesture first when the finger is clearly heading up or
 * down the page.
 *
 * Only ever attached in the one layout where a horizontal drag is not already
 * spoken for. In the paged layouts that same gesture is a page turn, and two
 * drag consumers on one axis means neither of them works.
 */
internal fun Modifier.swipeToChangeSurah(
    current: Int,
    last: Int = 114,
    onChange: (Int) -> Unit
): Modifier = composed {
    val thresholdPx = with(LocalDensity.current) { SwipeThreshold.toPx() }
    // Remembered, or the accumulation would be thrown away on every
    // recomposition and a slow swipe could never reach the threshold.
    val travelled = remember { mutableFloatStateOf(0f) }
    val state = rememberDraggableState { delta ->
        travelled.floatValue += delta
        if (abs(travelled.floatValue) >= thresholdPx) {
            val step = if (travelled.floatValue < 0) 1 else -1
            onChange((current + step).coerceIn(1, last))
            travelled.floatValue = 0f
        }
    }
    draggable(
        state = state,
        orientation = Orientation.Horizontal,
        // A gesture that ends short leaves no residue for the next one.
        onDragStopped = { travelled.floatValue = 0f }
    )
}

/**
 * Remembers the view scale, and hands back a setter that resets it.
 *
 * The scale belongs to the *view*, not to the reading, so it is deliberately
 * not persisted: reopening the reader should show the reading at the size the
 * user chose, not at whatever magnification they happened to leave behind.
 * Changing layout resets it for the same reason - a magnification chosen for a
 * full page of continuous text makes no sense over one verse per screen.
 */
@Composable
internal fun rememberViewScale(
    resetKey: Any?
): Pair<Float, (Float) -> Unit> {
    val scale = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(resetKey) { scale.floatValue = 1f }
    // Read in the composable body so the caller recomposes when the scale moves,
    // which is what makes the magnified surface actually re-render.
    val value = scale.floatValue
    return value to { next: Float ->
        scale.floatValue = next.coerceIn(QuranReadingOptions.ViewScaleRange)
    }
}

/**
 * The panned offset of a magnified view, clamped to what the view can actually
 * be moved by.
 *
 * The clamp is the whole reason this is a state holder rather than a bare
 * `mutableStateOf`. At 1x the view fills the screen, so the offset must be
 * pinned to zero - otherwise returning to 1x leaves the page shoved to one side
 * with a strip of blank paper beside it, and the reader has no idea why. At
 * [scale] the surplus on each axis is `size * (scale - 1) / 2`, because
 * `graphicsLayer` scales about the centre; anything beyond that is off-paper.
 *
 * [size] is in pixels. Zero - before the first layout pass - clamps to zero,
 * which is the correct answer at that moment anyway.
 */
@Composable
internal fun rememberPan(
    scale: Float,
    containerSize: IntSize
): Pair<Offset, (Offset) -> Unit> {
    val pan = remember { mutableStateOf(Offset.Zero) }
    val value = pan.value
    return value to { next: Offset ->
        pan.value = clampPan(next, scale, containerSize)
    }
}

/**
 * Clamps a panned offset to the surplus the current scale actually created.
 *
 * Split out from [rememberPan] so the arithmetic is testable without a
 * composition. At 1x the view already fills the screen and the offset must be
 * pinned to zero; at [scale] the surplus on each axis is
 * `size * (scale - 1) / 2`, because `graphicsLayer` scales about the centre.
 *
 * A negative scale or a zero/unmeasured size yields a zero offset rather than
 * throwing or producing `NaN` from a negative coercion bound.
 */
internal fun clampPan(pan: Offset, scale: Float, containerSize: IntSize): Offset =
    PinchMath.clampPan(pan, scale, containerSize)