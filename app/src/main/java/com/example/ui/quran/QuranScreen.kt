package com.example.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.data.model.Ayah
import com.example.ui.SalahUiState

/**
 * The Quran destination: browse, then read.
 *
 * Both halves live in one destination because moving between them is a mode
 * change, not a journey - and because the reader needs a back affordance that
 * returns *here* rather than dropping the user out of the app. [BackHandler] is
 * what makes the system gesture behave; without it, back from a surah used to
 * leave the Quran entirely.
 */
@Composable
fun QuranScreen(
    state: SalahUiState,
    onSurahSelected: (Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onPageSelected: (Int) -> Unit = {},
    onJuzSelected: (Int) -> Unit = {},
    onHizbSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var reading by rememberSaveable { mutableStateOf(false) }

    fun openReader() { reading = true }

    if (reading) {
        BackHandler { reading = false }
        QuranReader(
            state = state,
            onBack = { reading = false },
            onSelectSurah = onSurahSelected,
            onAyahViewed = onAyahViewed,
            onToggleBookmark = onToggleBookmark,
            onTogglePlayAyah = onTogglePlayAyah,
            onStopAudio = onStopAudio,
            onFontScaleChange = onFontScaleChange,
            modifier = modifier
        )
    } else {
        QuranLibrary(
            state = state,
            onOpenReader = ::openReader,
            onSurahSelected = onSurahSelected,
            onPageSelected = onPageSelected,
            onJuzSelected = onJuzSelected,
            onHizbSelected = onHizbSelected,
            modifier = modifier
        )
    }
}
