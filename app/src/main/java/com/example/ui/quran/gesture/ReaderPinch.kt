package com.example.ui.quran.gesture

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingOptions
import com.example.ui.quran.reader.ReaderPosition

/**
 * Pinch to read, and drag to move around what was magnified.
 *
 * The two things a pinch can do look identical on screen and are not, so [target]
 * decides which one happens:
 *
 * - [QuranPinchTarget.TEXT_SIZE] changes the type. The line rewraps, the saved
 *   preference matches what is on screen, and it stays crisp at every size.
 * - [QuranPinchTarget.VIEW_SCALE] magnifies the whole reading surface. The type is
 *   untouched and no preference is written, because nothing about the *reading*
 *   changed - only how much of it fits.
 *
 * ### Why the state is on [ReaderPosition]
 *
 * The previous version threaded the scale, the pan, the focal correction, the
 * surface size and the "is magnified" boolean through five parameters and three
 * `remember` holders in the reader, with a `rememberViewScale` and a `rememberPan`
 * beside it. The switch that decides whether a one-finger drag is a scroll or a pan
 * therefore had to be *re-derived* by the gesture layer from a value the reader
 * owned, and the two could disagree.
 *
 * They did. A magnified view's drag was simultaneously a pan, a list scroll and a
 * page turn, and the reader found the page sliding away from their finger while it
 * also turned. [ReaderPosition] owns the scale, the pan, the focal correction and
 * the switch together, so there is nothing to disagree.
 *
 * ### Why the detector is *not* keyed on the magnification
 *
 * `panZoomLock` has to flip when the view crosses 1x, and the old code achieved
 * that by re-keying this whole `pointerInput` on `isMagnified` - which tears the
 * detector down on the frame the scale crosses 1 and **discards the accumulated
 * zoom mid-pinch**. A reader pinching from 1x to 2x landed part of the way and the
 * gesture appeared to stop working.
 *
 * So the switch is read *inside* a stable detector. It is read through a state
 * holder rather than a captured lambda for the same reason the zoom is: the scale
 * changes on every event of the gesture, and a value captured when the detector was
 * built is stale from the second event onwards.
 *
 * ### What happens to a one-finger drag
 *
 * At 1x: nothing. `panZoomLock` is on, the list or pager keeps its own scroll, and
 * this block never sees a drag.
 *
 * Above 1x: the lock is off, because a magnified view that cannot be moved is a
 * view the reader has cropped themselves out of, and the only way back would be to
 * zoom out again. The pan is forwarded to the position, which clamps it to the
 * surplus the scale actually created.
 *
 * ### The focal correction, and why it is only fed on a real zoom
 *
 * `graphicsLayer` scales about the centre, so a pinch whose centroid is not the
 * centre displaces the content *and* leaves the reader looking at a different part
 * of the page than the one they were reading. That is the reason a magnified mushaf
 * so often feels like it jumped, and cancelling the displacement against the
 * centroid is what stops it.
 *
 * But the centroid is reported whether or not there is a pinch, so forwarding it
 * unconditionally also feeds an ordinary one-finger drag into the pan - the two add
 * up, and the page slides away from the reader's own finger. It is forwarded only
 * on an event where the scale actually moved.
 */
internal fun Modifier.readerPinch(
    position: ReaderPosition,
    target: QuranPinchTarget,
    arabicScale: Float,
    onArabicScaleChange: (Float) -> Unit
): Modifier = composed {
    val arabic = rememberUpdatedState(arabicScale)
    val onArabicChange = rememberUpdatedState(onArabicScaleChange)
    val magnified = rememberUpdatedState(position.isViewMagnified)

    // Keyed on the target only. The magnification is read from a state holder
    // rather than being a key, so the detector survives the whole pinch.
    pointerInput(target) {
        detectTransformGestures(panZoomLock = !magnified.value) { _, panChange, zoom, _ ->
            if (zoom == 1f) return@detectTransformGestures

            when (target) {
                QuranPinchTarget.TEXT_SIZE -> {
                    // The dead zone is inside `applyZoom`, and it matters here: a
                    // real pinch jitters by a fraction of a percent on every event,
                    // and each of those would be a write to the reader's *stored*
                    // text size - so a reader who pinched to look closer found
                    // their preference had drifted when they let go.
                    val next = PinchMath.applyZoom(arabic.value, zoom)
                        .coerceIn(QuranReadingOptions.ArabicScaleRange)
                    if (next != arabic.value) onArabicChange.value(next)
                }

                QuranPinchTarget.VIEW_SCALE -> {
                    val before = position.viewScale
                    position.magnifyBy(zoom)
                    // Only when the scale actually moved on this event, so a
                    // one-finger drag is a drag and not also a pan.
                    if (position.viewScale != before && panChange != Offset.Zero) {
                        position.pinchBy(panChange)
                    }
                }
            }
        }
    }
}
