package com.example.ui.quran.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach

/**
 * Where the reader is, and the one way it changes.
 *
 * ### The defect this exists to fix
 *
 * The previous reader had **three** writers of "where am I", all writing
 * different things to different state, and they overwrote each other:
 *
 * 1. `pageCursor` - the page the pager was on, written by the pager's own
 *    `snapshotFlow`, and separately re-seeded from the anchor in a `LaunchedEffect`.
 * 2. `state.activeReadingAyahNumber` - the anchor, written by `onAyahViewed` from
 *    the continuous layouts' scroll observer.
 * 3. `browsedPage` - a *third* number, written by `onVerseVisible` from
 *    `onGloballyPositioned` inside the flowing text, purely so the pill could show
 *    something sensible.
 *
 * Which produced the defects the previous commit messages describe: a page
 * indicator that disagreed with the page on screen, a pill whose number changed
 * meaning when the layout changed, and a "continue reading" that recorded the top
 * of the visible block rather than the verse under the reader's eye.
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
     *
     * The one entry point. Everything that navigates calls this, so there is one
     * place where a navigation clears the selection - and a reader who jumps to
     * another surah is not left with a verse from the old one "selected".
     */
    fun goTo(next: QuranRef) {
        ref = next
        selection = null
    }

    /**
     * Moves to [next] *keeping* the selection, where the selection is still valid.
     *
     * Only for a page turn within the same page's neighbours, and only when the
     * new page actually contains the selected verse - see [goTo].
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
}

/**
 * The reader's position, and the one debounced writer of the persisted position.
 *
 * ### Why the position is persisted through a debounce and a flow
 *
 * Turning a page is a discrete event, but scrolling is not: a continuous
 * `snapshotFlow` of the first visible item fires dozens of times a second, and
 * the previous reader wrote a database row for each one. It is also wrong to
 * record a position more precisely than a verse - nobody needs to know they were
 * 40% down verse 12 - and writing on every frame means the row being written is
 * one the reader has already scrolled past.
 *
 * So the position is emitted as a **flow**, debounced, and it is the same flow
 * the persistence collects. There is exactly one writer: the position moves,
 * [onPosition] fires when it settles, and everything downstream - the saved
 * position, Continue Reading, the Today shortcut - reads that one value.
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

/** The position a first run starts at: Al-Fatihah, page 1. */
internal fun initialPosition(): QuranRef = QuranRef.Start

/**
 * Drops a selection when the surface it was made on goes away.
 *
 * A layout or axis change re-seats the reader, and a *pending* selection from the
 * old surface is dropped - because the thing it referred to is no longer on
 * screen. The position is not moved: it is already correct, and moving it here
 * would be a second writer of the one thing there is only one of.
 *
 * The first composition is not a re-seat, so it does not clear anything; only a
 * *change* of [key] does.
 */
@Composable
internal fun ReSeatSelectionOnChange(position: ReaderPosition, key: Any?) {
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) {
            first = false
        } else {
            position.clearSelection()
        }
    }
}
