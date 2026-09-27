package com.example.ui.quran

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The type sizes the reader allows, and the one place they are defined.
 *
 * The options sheet's slider clamps to the same range, so a pinch can never
 * leave a size the slider cannot express or undo.
 */
internal val QuranScaleRange = 0.7f..1.6f

/** How far a finger must travel before the swipe counts. */
private val SwipeThreshold = 72.dp

/**
 * Pinch to change the size of the text.
 *
 * This scales the font rather than the layer, so the line rewraps and stays
 * crisp. Scaling the layer instead would be less code and it is wrong: the text
 * would grow past the margins, stop matching the saved preference, and come back
 * blurry until the next recomposition.
 *
 * `panZoomLock` is what keeps the two gestures from fighting. With it on, a
 * one-finger drag is decided to be a pan, the zoom is pinned to 1, and this
 * block does nothing at all - so the list underneath scrolls exactly as before.
 * A second finger turns it into a zoom and the pan is dropped.
 */
internal fun Modifier.pinchToResize(
    scale: Float,
    onScaleChange: (Float) -> Unit
): Modifier = composed {
    pointerInput(scale) {
        detectTransformGestures(panZoomLock = true) { _, _, zoom, _ ->
            if (zoom != 1f) {
                onScaleChange((scale * zoom).coerceIn(QuranScaleRange))
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
 */
internal fun Modifier.swipeToChangeSurah(
    current: Int,
    last: Int = 114,
    onChange: (Int) -> Unit
): Modifier = composed {
    // The threshold is authored in dp so it is the same physical distance on
    // every screen, then converted once to the pixels the drag reports in.
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
