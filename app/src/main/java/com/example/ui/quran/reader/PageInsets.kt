package com.example.ui.quran.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.example.ui.components.SafeArea
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/**
 * The space the reader's floating chrome occupies at the top of the reading
 * surface.
 *
 * ### Why this is a number the reader passes in rather than an inset each page reads
 */
object PageInsets {

    /**
     * The room a page's content must leave at the top, given whether the reader's
     * control row is showing.
     */
    @Composable
    fun top(controlsVisible: Boolean): Dp {
        val space = Space.current
        val minTouch = MaterialTheme.layoutMetrics.minTouchTarget
        val chrome = if (controlsVisible) minTouch + space.sm else minTouch
        return SafeArea.top() + chrome + space.xs
    }

    /**
     * The horizontal margin around the text, and where both reading surfaces get it.
     *
     * **It must stay one value applied to both sides.** Both `MushafPage` and
     * `FlowingBlock` reserve this with a single `padding(horizontal = ...)` and then
     * subtract the same number from a tap's x coordinate to move from node space
     * into text space - the padding is a layout modifier on the *same node* as the
     * text, so the node's origin is the outer edge of the gutter while the text
     * layout's coordinates start at the text. Symmetry is what makes that
     * subtraction correct. Padding each side by its own cutout inset would be more
     * precise and would silently break tap hit-testing on the side with the notch,
     * resolving taps to the wrong verse.
     *
     * A *vertical* value needs no such care, which is why this asymmetry exists at
     * all and why it is easy to break.
     */
    @Composable
    fun gutter(minimum: Dp): Dp = minimum + SafeArea.sides()
}
