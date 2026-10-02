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
 *
 * The reader's controls are chrome **over** the text, not a header above it - that
 * is the whole point of the design, and it is why there is no app bar. The
 * consequence is that a page has to reserve room for them itself, and how much
 * depends on whether the chrome is currently on screen: in immersive mode there is
 * no control row, so a page that reserved the full inset would be losing height for
 * a bar nobody can see.
 *
 * So the reader - which knows - passes the number down, and the page reserves what
 * it is told. Each page measuring its own insets cannot know about immersive mode,
 * and the old arrangement (a page reading the status-bar inset unconditionally)
 * reserved the room in both states.
 *
 * ### What is in it
 *
 * The status bar and any display cutout, the height of the control row, and a
 * little air. The immersive button is in its own corner and is covered by the same
 * reserve, because it is on screen in *both* states - which is deliberate: a button
 * that disappears when a reader needs it is a trap.
 *
 * ### And the sides, which both reading surfaces share
 *
 * [gutter] is the other half, and it exists here rather than in each surface
 * because two of them needed it and the reason is not obvious. A design gutter of
 * 16dp is enough in portrait, where a display cutout is at the top and
 * [SafeArea.top] has already covered it. Rotated, the cutout moves to the left or
 * right edge and is 30-40dp deep, so the outer column of every page of Arabic was
 * underneath it - and in right-to-left text the outer column is the *first* one,
 * so a reader lost the beginning of the page, which is a wrong reading rather than
 * a crowded one.
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
     * [minimum] is the design gutter; the display cutout is *added* to it rather
     * than replacing it, so a device with no cutout looks exactly as it did.
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
