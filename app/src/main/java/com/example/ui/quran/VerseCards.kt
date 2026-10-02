package com.example.ui.quran

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Ayah
import com.example.data.quran.ArabicDigits
import com.example.ui.components.StatusDot
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.IconSize
import com.example.ui.theme.QuranFonts.LocalQuranTypeface
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/** Height of the small reference chip that carries a "2:255" marker. */
internal val ChipHeight = 28.dp

/**
 * What a verse offers, and what state it is in.
 *
 * Modelled as one value rather than six booleans because every verse surface
 * renders the same row of actions, and they used to be six hand-assembled
 * parameter lists per surface - which is how the inspector came to offer Pause
 * while the per-verse block offered Play, on the very same verse.
 *
 * [ayah] rides along so the copy and share actions can be derived rather than
 * passed twice into every call site. It is null only on surfaces that show the
 * row without owning a verse.
 */
@Immutable
data class VerseActions(
    val ayah: Ayah? = null,
    val isBookmarked: Boolean = false,
    val isPlaying: Boolean = false,
    /** Off where audio is not offered - the index previews. */
    val showAudio: Boolean = true,
    val onToggleBookmark: () -> Unit = {},
    val onTogglePlay: () -> Unit = {}
)

/**
 * The verse's reference as a pill.
 *
 * One component for all three places it appears - the per-ayah block, the
 * inspector and the saved list - so "2:255" is always the same size, colour and
 * shape wherever the reader shows it.
 */
@Composable
internal fun VerseReferenceChip(
    reference: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Surface(
        color = if (filled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (filled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        shape = QuranShape.pill,
        modifier = modifier.heightIn(min = ChipHeight)
    ) {
        Text(
            text = reference,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = Space.current.md,
                vertical = Space.current.xxs
            )
        )
    }
}

/**
 * The translation card.
 *
 * Unchanged in design and unchanged in role: a quiet surface carrying the
 * reference above the translation, at the reader's translation scale so the
 * Arabic and its translation stay in proportion while both are adjusted.
 *
 * One composable rather than a per-surface variant, because it renders
 * identically under a verse block, under the inspector and in the index - and
 * three copies of a card that is meant to look like one thing is how they stop
 * looking like one thing.
 */
@Composable
fun VerseTranslationCard(
    ayah: Ayah,
    translationScale: Float,
    modifier: Modifier = Modifier,
    showReference: Boolean = true,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    surface: Color = MaterialTheme.colorScheme.surfaceContainer,
    muted: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val space = Space.current
    val base = MaterialTheme.typography.bodyMedium
    Surface(
        color = surface,
        contentColor = ink,
        shape = QuranShape.tile,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(space.md)) {
            if (showReference) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(space.xs))
            }
            // `textDirection = Content` on the translation itself.
            //
            // The translation is *English* wherever it appears, so its paragraph
            // direction must not be the interface's. Without this the Arabic
            // interface placed an English sentence in an RTL paragraph, and the
            // neutral characters at its edges - the trailing full stop, an opening
            // quote - are resolved by paragraph direction, so the period jumped to
            // the front of the line and the quotes swapped ends:
            // `,"Say, "He is Allah, [who is] One` instead of `Say, "He is
            // Allah..."`. The words stayed in order, which is why it reads as
            // almost-right rather than as broken.
            //
            // This is the one place in the reader that must force a direction
            // against the ambient one, and it is why the card takes it rather than
            // assuming it: a translation is LTR whatever the reader's interface is,
            // and the mushaf page's Arabic is RTL whatever it is.
            Text(
                text = ayah.textEnglish,
                style = base.copy(
                    fontSize = base.fontSize * translationScale,
                    lineHeight = base.lineHeight * translationScale,
                    textDirection = TextDirection.Content
                ),
                color = ink,
                textAlign = TextAlign.Start,
                modifier = Modifier.semantics { contentDescription = ayah.textEnglish }
            )
        }
    }
}

/**
 * The Arabic of one verse, in the reader's typeface and size.
 *
 * The single place the Quranic text style is built. The flowing page, the
 * per-ayah block, the inspector and the index preview all call this, which is
 * what keeps them from disagreeing about the leading of identical text - the
 * thing that went wrong three separate times before this component existed.
 *
 * The face comes from [LocalQuranTypeface], so it does not have to be threaded
 * through every call site, and a surface rendered outside a reader still gets a
 * correct Arabic style rather than a default one.
 */
@Composable
internal fun VerseArabic(
    ayah: Ayah,
    scale: Float,
    ink: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    showEndMarker: Boolean = true
) {
    Text(
        text = if (showEndMarker) {
            // `ayahMarker` already carries the U+06DD ornament. This used to write a
            // literal ۝ and *then* call it, so every per-verse block ended with two
            // ayah circles - the per-page mushaf does not, because
            // `MushafPageText` calls the same function once.
            //
            // It is only visible in RTL. In an LTR context the second circle is set
            // inside the first and the pair reads as one ornament drawn thick; under
            // RTL the pair resolves as two separate glyphs at the start of the line,
            // which is where the Arabic screenshot found it.
            "${ayah.textArabic} ${ArabicDigits.ayahMarker(ayah.ayahNumber)}"
        } else {
            ayah.textArabic
        },
        style = LocalQuranTypeface.current.arabicStyle(scale = scale, color = ink),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth()
    )
}

/** An icon button sized to the app's minimum touch target. */
@Composable
internal fun VerseActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(IconSize.lg)
        )
    }
}

/**
 * The row of per-verse actions, shared by the block and the inspector.
 *
 * Copy and share are derived from [VerseActions.ayah] so a verse copied from the
 * flowing page is byte-identical to one copied from its block.
 */
@Composable
internal fun VerseActionRow(
    actions: VerseActions,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ayah = actions.ayah

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (actions.showAudio) {
            VerseActionButton(
                icon = if (actions.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (actions.isPlaying) strings.more.pauseVerse else strings.more.playVerse,
                onClick = actions.onTogglePlay
            )
        }
        VerseActionButton(
            icon = if (actions.isBookmarked) {
                Icons.Default.Bookmark
            } else {
                Icons.Default.BookmarkBorder
            },
            label = if (actions.isBookmarked) {
                strings.more.removeBookmark
            } else {
                strings.more.bookmarkVerse
            },
            tint = if (actions.isBookmarked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            onClick = actions.onToggleBookmark
        )
        if (ayah != null) {
            VerseActionButton(
                icon = Icons.Default.ContentCopy,
                label = strings.more.copyVerse,
                onClick = { copyVerse(context, clipboard, ayah, strings.more.verseCopied) }
            )
            VerseActionButton(
                icon = Icons.Default.Share,
                label = strings.more.shareVerse,
                onClick = { shareVerse(context, ayah, strings.more.shareVerse) }
            )
        }
    }
}

/**
 * The text a copy or share puts on the clipboard.
 *
 * **One ayah marker.** This wrote a literal `۝` *and* `ArabicDigits.ayahMarker(...)`,
 * which is itself `۝` followed by the count - so the verse a reader copied or sent to
 * someone else carried two ornaments and one number: `۝ ۝٤`. Every other surface in
 * the app was fixed for exactly this and this one was missed, which is the argument
 * for having it written in one place at all.
 *
 * A copied verse is the one piece of this app a reader hands to another person, so a
 * doubled ornament is the most visible place the mistake could have landed.
 */
internal fun versePayload(ayah: Ayah): String =
    "${ayah.textArabic} ${ArabicDigits.ayahMarker(ayah.ayahNumber)}\n\n" +
        "\"${ayah.textEnglish}\"\n[${ayah.surahNumber}:${ayah.ayahNumber}]"

internal fun copyVerse(
    context: Context,
    clipboard: ClipboardManager,
    ayah: Ayah,
    label: String
) {
    clipboard.setText(AnnotatedString(versePayload(ayah)))
    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
}

internal fun shareVerse(context: Context, ayah: Ayah, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, versePayload(ayah))
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** The play state dot shown while a verse is reciting. */
@Composable
internal fun PlayingDot(isPlaying: Boolean) {
    if (!isPlaying) return
    StatusDot(
        color = MaterialTheme.colorScheme.primary,
        description = LocalStrings.current.more.playVerse
    )
}

/** Marks a surface that names the surah currently being read. */
internal fun Modifier.surahHeading() = semantics { heading() }
