package com.example.ui.quran.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.QuranRef
import com.example.data.model.QuranReadingOptions
import com.example.data.quran.QuranBrowse
import com.example.ui.quran.gesture.PinchMath
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach

/**
 * Where the reader is, and the one way it changes.
 *
 * ### One value, and one direction
 *
 * There is a single [ref] here, and everything else is *derived* from it:
 *
 * | question | answered by |
 * | --- | --- |
 * | which page is on screen | `ref.page` |
 * | which surah is being read | `ref.surah` |
 * | which verse is selected | `selection`, separately - selection is not position |
 * | what to persist | `ref` |
 *
 * A page turn writes a ref. A scroll writes a ref. An index selection writes a
 * ref. There is no second number that can lag behind the first, because there is
 * no second number.
 *
 * ### Selection is not position
 *
 * A reader who taps a verse has *not* moved there. So [selection] is its own
 * field, it is not persisted, and turning a page clears it - because a selection
 * on the page behind is not a selection at all, and carrying it forward would
 * highlight a verse on a page that does not contain it.
 */
@Stable
class ReaderPosition internal constructor(
    /** Where the reader is. The single source of truth. */
    ref: QuranRef
) {
    /** The page on screen. Derived, and the only page number in the reader. */
    var ref: QuranRef by mutableStateOf(ref)
        private set

    /** The selected verse on this surface, or null. Never persisted. */
    var selection: QuranRef? by mutableStateOf(null)
        private set

    /** The page currently on screen. */
    val page: Int get() = ref.page

    /** The surah currently being read. */
    val surah: Int get() = ref.surah

    /**
     * Moves to [next].
     */
    fun goTo(next: QuranRef) {
        ref = next
        selection = null
    }

    /**
     * Moves to [next] *keeping* the selection, where the selection is still valid.
     */
    fun turnTo(next: QuranRef) {
        ref = next
        // A selection is only meaningful on the page that contains it. Page 604
        // holds three surahs, so ayah 1 selected there is not ayah 1 anywhere
        // else.
        selection = selection?.takeIf { QuranBrowse.ayahOnPage(it, next.page) }
    }

    /**
     * The page's own first verse, which is what a turn lands on.
     *
     * A page turn writes the *first* verse of the page, not the first verse of
     * the reader's surah. That was the previous bug: turning from page 2 to page 3
     * wrote 2:1, so "continue reading" put the reader back on page 2 forever.
     */
    fun turnPage(delta: Int): Boolean {
        val target = (ref.page + delta).coerceIn(1, QuranBrowse.TOTAL_PAGES)
        if (target == ref.page) return false
        turnTo(QuranBrowse.placeAtPage(target).verse)
        return true
    }

    /** Selects [verse], or clears the selection when it is already selected. */
    fun toggleSelection(verse: QuranRef?) {
        selection = if (verse == null || selection == verse) null else verse
    }

    /** Selects [verse] outright. Used by an accessibility action or a search hit. */
    fun select(verse: QuranRef) {
        selection = verse
    }

    fun clearSelection() {
        selection = null
    }

    // -----------------------------------------------------------------------
    // Magnification.
    //
    // It lives on the position because it is the other piece of *view* state that
    // has to be coordinated with it: a page turn resets it, a pinch changes it,
    // and both have to agree about whether a one-finger drag is a scroll or a
    // pan. Split across two objects they would drift, and the symptom of drift is
    // a reading surface that swallows a scroll.
    // -----------------------------------------------------------------------

    /** The current view magnification. Never persisted. */
    var viewScale: Float by mutableFloatStateOf(1f)
        private set

    private var pan: Offset by mutableStateOf(Offset.Zero)

    /**
     * Where the last pinch happened, so a magnified surface can stay under the
     * fingers instead of drifting away as the layer scales about its centre.
     */
    private var focal: Offset by mutableStateOf(Offset.Zero)

    /** The size of the reading surface, for the pan clamp. */
    var surfaceSize: IntSize by mutableStateOf(IntSize.Zero)
        private set

    /**
     * Whether a magnified view should claim a one-finger drag.
     */
    val isViewMagnified: Boolean
        get() = viewScale > MAGNIFIED_THRESHOLD

    /**
     * Magnifies the view, for a pinch set to *Zoom the view*.
     */
    fun magnifyBy(zoomChange: Float) {
        val next = PinchMath.applyZoom(viewScale, zoomChange)
            .coerceIn(QuranReadingOptions.ViewScaleRange)
        if (next == viewScale) return
        viewScale = next
        if (next <= MAGNIFIED_THRESHOLD) recentre()
    }

    /** Resets magnification, for a layout or surface change. */
    fun resetMagnification() = recentre()

    private fun recentre() {
        viewScale = 1f
        // At 1x the view fills the screen, so any leftover offset would leave a
        // strip of blank paper beside the text with no way back.
        pan = Offset.Zero
        focal = Offset.Zero
    }

    fun surfaceMeasured(size: IntSize) {
        surfaceSize = size
    }

    /**
     * The reader's own pan plus the pinch correction, clamped **as one value**.
     */
    val appliedPan: Offset
        get() = PinchMath.clampPan(pan + focal, viewScale, surfaceSize)

    /** Moves a magnified view. A no-op unless magnified, and a no-op at 1x. */
    fun panBy(delta: Offset) {
        if (!isViewMagnified || delta == Offset.Zero) return
        pan = PinchMath.clampPan(pan + delta, viewScale, surfaceSize)
    }

    /** Records a pinch's own movement, so the content stays under the fingers. */
    fun pinchBy(delta: Offset) {
        if (delta == Offset.Zero) return
        focal += delta
    }
}

/**
 * The reader's position, and the one debounced writer of the persisted position.
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberReaderPosition(
    initial: QuranRef,
    /** How long a position must settle before it counts. */
    settleMillis: Long = 600,
    onPosition: (QuranRef) -> Unit
): ReaderPosition {
    val position = rememberSaveable(
        saver = listSaver(
            save = { listOf(it.ref.surah, it.ref.ayah, it.ref.page) },
            restore = { saved -> ReaderPosition(QuranRef(saved[0], saved[1], saved[2])) }
        )
    ) { ReaderPosition(initial) }

    // Restored on a rotation or a process death, and re-emitted so the stored
    // position is refreshed with what the reader actually had open - which may be
    // different from what was last written, because the reader may have closed
    // the app mid-scroll.
    LaunchedEffect(Unit) { onPosition(position.ref) }

    LaunchedEffect(position, settleMillis) {
        // `collect` on the terminal rather than an `onEach` above it, so the whole
        // chain is one expression and there is no way to leave a `collect` off the
        // end of it - which compiles, and then does nothing at all.
        snapshotFlow { position.ref }
            .distinctUntilChanged()
            .filter { ref ->
                // Only a position that exists is worth writing. A stale page from
                // an older corpus would put a reader on a page that no longer has
                // their verse.
                QuranBrowse.ayah(ref.surah, ref.ayah) != null
            }
            .debounce(settleMillis)
            .collect { onPosition(it) }
    }

    return position
}


/**
 * The scale above which the view counts as magnified.
 */
internal const val MAGNIFIED_THRESHOLD = 1.01f
