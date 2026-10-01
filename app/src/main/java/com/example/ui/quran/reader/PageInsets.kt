package com.example.ui.quran.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.example.ui.components.statusBarInset
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
 * and the old arrangement (a page reading `statusBarInset()` unconditionally)
 * reserved the room in both states.
 *
 * ### What is in it
 *
 * The status bar and any display cutout, the height of the control row, and a
 * little air. The immersive button is in its own corner and is covered by the same
 * reserve, because it is on screen in *both* states - which is deliberate: a button
 * that disappears when a reader needs it is a trap.
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
        return statusBarInset() + chrome + space.xs
    }
}

/** The inset a *continuously* scrolling surface uses, which has no page chrome. */
@Composable
internal fun continuousTopInset(): Dp =
    statusBarInset() + Space.current.sm
