package com.example.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.ui.SalahUiState

/**
 * The Quran destination: read.
 *
 * **There is no library screen.** Opening Quran opens the text, at the last
 * place it was left, which is what the reader is for. The browse affordances -
 * surahs, saved verses, search, page/juz'/hizb - live in the index sheet, which
 * is one tap away from the reading surface and dismisses back onto it, rather
 * than being a screen you have to pass *through* to reach the book.
 *
 * That is the whole difference from the previous version, and it is why the
 * reader's back affordance is a dock tab rather than an arrow: leaving is a
 * change of destination, not an undo.
 *
 * The two sheets are owned here rather than by the reader so that dismissing
 * one does not disturb reader state - an earlier version kept the sheet flags
 * inside the reader, so opening the options sheet and rotating the device
 * dismissed it.
 */
@Composable
fun QuranScreen(
    state: SalahUiState,
    onSurahSelected: (Int) -> Unit,
    onSurahAyahSelected: (Int, Int) -> Unit,
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

    // The system back gesture closes the reader's own layers before it leaves
    // the destination. Without this, back from an open index sheet left the
    // Quran tab entirely, which is a long way to travel from one tap.
    BackHandler(enabled = indexOpen || optionsOpen) {
        indexOpen = false
        optionsOpen = false
    }

    QuranReader(
        state = state,
        onSelectSurahAyah = onSurahAyahSelected,
        onAyahViewed = onAyahViewed,
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
            currentSurah = state.selectedSurah.number,
            bookmarks = state.bookmarks,
            onSelectSurah = onSurahSelected,
            onSelectSurahAyah = { surah, ayah ->
                onSurahAyahSelected(surah, ayah)
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
