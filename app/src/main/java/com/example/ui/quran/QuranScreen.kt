package com.example.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import com.example.ui.SalahUiState
import com.example.ui.quran.reader.rememberReaderPosition

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
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    onImmersiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var indexOpen by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }

    // The one position, and the one debounced writer of the persisted position.
    //
    // Seeded from the ViewModel's `pendingOpen` on first composition - which is where
    // Continue Reading and a first run both arrive - and thereafter owned entirely
    // here, so that nothing outside the destination has to hold a copy of it.
    val position = rememberReaderPosition(
        initial = state.pendingOpen ?: QuranRef.Start,
        // The position is resolved against the corpus before it is persisted, so a
        // stored row can never name a verse this build does not have.
        onPosition = { ref -> QuranBrowse.ayah(ref.surah, ref.ayah)?.let(onAyahViewed) }
    )

    // A request to be put somewhere, honoured once.
    //
    // Keyed on the *request*, not on the position, so a request fires the reader
    // exactly once and the reader's own movements cannot re-trigger it - the loop
    // that the two-writer arrangement invited. `goTo` is the only way anything
    // outside a reading surface moves the reader, which is what makes "where am I"
    // have one answer.
    LaunchedEffect(state.pendingOpen) {
        state.pendingOpen?.let { position.goTo(it) }
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
