package com.example.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranCorpus
import com.example.ui.SalahUiState
import com.example.ui.quran.reader.onSurfacePage
import com.example.ui.quran.reader.rememberReaderPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Quran destination: read.
 *
 * **There is no library screen.** Opening Quran opens the text, at the last
 * place it was left, which is what the reader is for. The browse affordances -
 * surahs, saved verses, search, page/juz'/hizb - live in the index sheet, which
 * is one tap away from the reading surface and dismisses back onto it, rather
 * than being a screen you have to pass *through* to reach the book.
 *
 * ### The reader's place is owned *here*
 *
 * `ReaderPosition` is created at the destination and handed down, rather than being
 * created inside the reader, so exactly one writer can move it.
 */
@Composable
fun QuranScreen(
    state: SalahUiState,
    onOpen: (QuranRef) -> Unit,
    /**
     * Told when the pending request has been honoured, so it can be forgotten.
     *
     * Required, not defaulted: a caller that does not pass it is a caller that
     * leaves the request set, which is exactly the bug the effect below spent a
     * release with - a request re-delivered on every re-entry into the
     * destination. The compiler is the only reliable reminder.
     */
    onOpenConsumed: () -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    onImmersiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // The corpus is the one read in this app big enough to be felt: a SHA-256
    // gate, a parse and 6,236 verses, hundreds of milliseconds on a cold
    // process. The reader is where every reader meets it first, so the wait
    // happens here, off the main thread, and shows the reader's own loading
    // face rather than arriving as a frozen frame.
    //
    // The check is a field read once the corpus is in memory, so every later
    // visit composes the reader immediately; only the first open in a process
    // can see the loading face at all.
    var corpusReady by remember { mutableStateOf(QuranCorpus.isLoaded) }
    if (!corpusReady) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) { QuranCorpus.ayahs }
            corpusReady = true
        }
        QuranLoading(modifier = modifier)
        return
    }

    var indexOpen by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }

    // The one position, and the one debounced writer of the persisted position.
    //
    // Seeded from the ViewModel's `pendingOpen` on first composition - which is where
    // Continue Reading and a first run both arrive - and thereafter owned entirely
    // here, so that nothing outside the destination has to hold a copy of it.
    //
    // The seed is attributed by this reader's own pagination *before* the pager
    // composes: the pager reads `position.page` in this same composition, and the
    // effect that would re-answer a flowing-view page number for the print runs
    // only after - the delay would be visible as one wrong first frame. A
    // position restored from a save is re-answered by the reader's own effect.
    val position = rememberReaderPosition(
        initial = state.pendingOpen
            // A stored row from a build whose partition differed, or a corrupt
            // bookmark, names no verse this build shows: it opens nowhere, and
            // the reader starts where its own save put it.
            ?.takeIf { QuranBrowse.ayah(it.surah, it.ayah) != null }
            // The seed is attributed by this reader's own pagination *before*
            // the pager composes: the pager reads `position.page` in this same
            // composition, and the effect that would re-answer a flowing-view
            // page number for the print runs only after - the delay would be
            // visible as one wrong first frame. A position restored from a
            // save is re-answered by the reader's own effect.
            ?.onSurfacePage(state.quranReadingOptions.layout)
            ?: QuranRef.Start,
        // The position is resolved against the corpus before it is persisted, so a
        // stored row can never name a verse this build does not have.
        onPosition = { ref -> QuranBrowse.ayah(ref.surah, ref.ayah)?.let(onAyahViewed) }
    )

    // A request to be put somewhere, honoured once - and consumed.
    //
    // Keyed on the *request*, not on the position, so a request fires the reader
    // exactly once per arrival and the reader's own movements cannot re-trigger it -
    // the loop that the two-writer arrangement invited. `goTo` is the only way
    // anything outside a reading surface moves the reader, which is what makes
    // "where am I" have one answer.
    //
    // Consumption is the other half of "once". A request left set is an event kept
    // as state: the destination is recomposed on every re-entry (a tab switch
    // re-uses the destination through `saveState`/`restoreState`), this effect
    // re-runs with the same request, and the reader is moved back to it - and the
    // debounced position writer saves that place. The ViewModel is told to forget
    // the request the moment it has been handed over, so a re-entry finds an empty
    // mailbox and the reader's own place stands.
    LaunchedEffect(state.pendingOpen) {
        state.pendingOpen?.let { ref ->
            // Honoured against this reader's own pagination, so a verse opened
            // in the Madinah mushaf lands on the page the print shows it on -
            // not on the number the request's flowing-view attribution wrote.
            position.openRef(ref)
            onOpenConsumed()
        }
    }

    // The system back gesture closes the reader's own layers before it leaves
    // the destination. Without this, back from an open index sheet left the
    // Quran tab entirely, which is a long way to travel from one tap.
    BackHandler(enabled = indexOpen || optionsOpen) {
        indexOpen = false
        optionsOpen = false
    }

    QuranReader(
        state = state,
        position = position,
        onToggleBookmark = onToggleBookmark,
        onTogglePlayAyah = onTogglePlayAyah,
        onStopAudio = onStopAudio,
        onCycleSpeed = onCycleSpeed,
        onCycleRepeat = onCycleRepeat,
        options = state.quranReadingOptions,
        onOptionsChange = onOptionsChange,
        immersive = state.isQuranImmersive,
        onImmersiveChange = onImmersiveChange,
        onOpenIndex = { indexOpen = true },
        onOpenOptions = { optionsOpen = true },
        modifier = modifier
    )

    if (indexOpen) {
        QuranIndexSheet(
            currentSurah = position.surah,
            bookmarks = state.bookmarks,
            onSelectSurah = { surah ->
                onOpen(QuranBrowse.ref(surah, 1) ?: return@QuranIndexSheet)
                indexOpen = false
            },
            onSelectSurahAyah = { surah, ayah ->
                onOpen(QuranBrowse.ref(surah, ayah) ?: return@QuranIndexSheet)
                indexOpen = false
            },
            onDismiss = { indexOpen = false }
        )
    }

    if (optionsOpen) {
        ReadingOptionsSheet(
            options = state.quranReadingOptions,
            onOptionsChange = onOptionsChange,
            onDismiss = { optionsOpen = false }
        )
    }
}
