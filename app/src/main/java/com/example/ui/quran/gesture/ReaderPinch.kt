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
 * - [QuranPinchTarget.TEXT_SIZE] changes the type. The line rewraps, the saved
 *   preference matches what is on screen, and it stays crisp at every size.
 * - [QuranPinchTarget.VIEW_SCALE] magnifies the whole reading surface. The type is
 *   untouched and no preference is written, because nothing about the *reading*
 *   changed - only how much of it fits.
 *
 * ### Why the state is on [ReaderPosition]
 *
 * ### Why the detector is *not* keyed on the magnification
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
