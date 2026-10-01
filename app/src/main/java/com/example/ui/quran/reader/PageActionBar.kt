package com.example.ui.quran.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.ui.localization.LocalStrings
import com.example.ui.quran.VerseActionRow
import com.example.ui.quran.VerseActions
import com.example.ui.quran.VerseTranslationCard
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space

/**
 * What a selected verse offers.
 *
 * One bar, one surface, and the same [VerseActions] the continuous layouts use -
 * because the actions belong to a *verse*, not to a layout. The previous reader had
 * them modelled as six hand-assembled parameter lists per surface, and the result
 * was a per-verse block offering Play while the inspector on the very same verse
 * offered Pause.
 *
 * ### Why it sits at the foot of a page rather than under the verse
 *
 * A mushaf page is a fixed object: these lines, on this paper, in this order. Putting
 * a panel under the selected verse would mean either covering the text or reflowing
 * the page - and reflowing changes where every line falls, so the reader's eye
 * loses the place they were reading. The foot of the page is the only part of it
 * that is not text.
 *
 * ### Why the translation is here and not in the page
 *
 * Because it is an *action* on the selected verse, not a property of the page. A
 * page with translations on is a different reading mode - the reader asked for the
 * whole page translated - and this is one verse's translation, revealed by a tap.
 * Mixing the two is what made the old continuous layout append 286 verses of
 * English below a whole surah the moment the setting was turned on.
 */
@Composable
internal fun PageActionBar(
    ayah: Ayah,
    actions: VerseActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true,
    translationScale: Float = 1f
) {
    val space = Space.current
    val strings = LocalStrings.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = QuranShape.card,
        modifier = modifier
            .fillMaxWidth()
            .testTag("page_action_bar")
    ) {
        Column(modifier = Modifier.padding(space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("page_action_bar_close")
                ) {
                    Text(strings.more.actionClose)
                }
            }

            if (showTranslation) {
                Spacer(Modifier.height(space.xs))
                VerseTranslationCard(
                    ayah = ayah,
                    translationScale = translationScale,
                    ink = MaterialTheme.colorScheme.onSurface,
                    surface = Color.Transparent,
                    showReference = false
                )
            }

            Spacer(Modifier.height(space.sm))
            VerseActionRow(actions)
        }
    }
}

/**
 * The same bar, sized and shaped for the continuous layout.
 *
 * A thin alias rather than a parameter, because the two surfaces genuinely want
 * different things: in a scrolling list the bar is an *item* and moves with the
 * text, while on a page it is an overlay at the foot. Sharing the content and
 * splitting the placement is what stops the two from drifting.
 */
@Composable
internal fun SelectedVerseActions(
    ayah: Ayah,
    actions: VerseActions,
    options: QuranReadingOptions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PageActionBar(
        ayah = ayah,
        actions = actions,
        onDismiss = onDismiss,
        modifier = modifier,
        showTranslation = options.showTranslation,
        translationScale = options.translationScale
    )
}
