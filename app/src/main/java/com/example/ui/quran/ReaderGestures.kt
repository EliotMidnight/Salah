package com.example.ui.quran

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingOptions
import kotlin.math.abs

/**
 * How far a finger must travel before a swipe counts as a surah change.
 *
 * Authored in dp so it is the same physical distance on every screen, then
 * converted once to the pixels the drag reports in.
 */
private val SwipeThreshold = 72.dp

/**
 * Pinch to read.
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
 * Capturing the scale in the lambda instead - the obvious version, and the one
 * this file used to have - makes every event multiply by a stale value, so a
 * ten-finger-splay pinch lands at roughly the same place as a two-finger one.
 * Re-keying the `pointerInput` on the scale fixes the staleness but tears the
 * gesture detector down mid-pinch and drops the accumulated zoom. Reading the
 * live value through a state holder avoids both: the detector is created once
 * and the scale it applies against is always the current one.
 *
 * `panZoomLock` keeps the two gestures from fighting. With it on, a one-finger
 * drag is decided to be a pan, the zoom is pinned to 1 and this block does
 * nothing at all - so the list underneath scrolls exactly as it always did. A
 * second finger turns it into a zoom and the pan is dropped.
 */
internal fun Modifier.readerPinch(
    target: QuranPinchTarget,
    arabicScale: Float,
    onArabicScaleChange: (Float) -> Unit,
    viewScale: Float,
    onViewScaleChange: (Float) -> Unit
): Modifier = composed {
    val arabic = rememberUpdatedState(arabicScale)
    val view = rememberUpdatedState(viewScale)
    val onArabicChange = rememberUpdatedState(onArabicScaleChange)
    val onViewChange = rememberUpdatedState(onViewScaleChange)

    // Keyed on the *target* only. Re-keying on the scale would restart the
    // detector on every zoom event, which is the bug described above.
    pointerInput(target) {
        detectTransformGestures(panZoomLock = true) { _, _, zoom, _ ->
            if (zoom == 1f) return@detectTransformGestures
            when (target) {
                QuranPinchTarget.TEXT_SIZE -> onArabicChange.value(
                    (arabic.value * zoom).coerceIn(QuranReadingOptions.ArabicScaleRange)
                )

                QuranPinchTarget.VIEW_SCALE -> onViewChange.value(
                    (view.value * zoom).coerceIn(QuranReadingOptions.ViewScaleRange)
                )
            }
        }
    }
}

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
