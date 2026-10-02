package com.example.ui.quran.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.data.model.Ayah
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranRef
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ReaderStrings
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.Space
import java.util.Locale

/**
 * A run of verses as one block of flowing text.
 *
 * ### Why a block and not a column of `Text`s
 *
 * A surah read straight through is prose. It has to wrap as a block, so that a
 * sentence runs across lines the way it would on a page, and so that the line
 * breaking is the text's own rather than one imposed per verse. A column of
 * per-verse `Text`s breaks every verse into its own paragraph, which turns running
 * prose into a list.
 *
 * The cost is that a string has no structure, so everything a reader can do with a
 * verse - select it, highlight it, find which verse a tap landed on - depends on
 * the spans recorded while it was built. [MushafPageText] does that, and its
 * `verseAt` resolves an offset to a verse.
 *
 * ### Accessibility: one action per verse, not one node per action
 *
 * A screen reader has to be able to act on *any* verse in the block, and the
 * previous implementation put every verse's "select" action in a single
 * `customActions` list. On a twelve-verse block that is twelve entries in one
 * node's action list; on a whole surah it was hundreds, and a screen reader's
 * custom-action menu is a flat list with no way to jump to a numbered one. Twelve
 * is already at the edge of usable, which is a large part of why
 * [FLOW_BLOCK_VERSES] is a dozen and not a surah.
 */
@Composable
internal fun FlowingBlock(
    ayahs: List<Ayah>,
    selected: QuranRef?,
    options: QuranReadingOptions,
    ink: Color,
    accent: Color,
    onSelectVerse: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val density = LocalDensity.current
    val strings = LocalStrings.current
    val typeface = QuranFonts.LocalQuranTypeface.current
    val highlight = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer

    val block = remember(
        ayahs,
        options.arabicScale,
        selected,
        ink,
        accent,
        highlight,
        typeface.lineHeightFactor
    ) {
        MushafPageText.build(
            ayahs = ayahs,
            scale = options.arabicScale,
            selected = selected,
            ink = ink,
            accent = accent,
            highlight = highlight,
            lineHeightFactor = typeface.lineHeightFactor,
            // The list already has a surah heading above it. Naming the surah again
            // here printed it twice, and the second copy landed *inline* at the start
            // of the first line, where it read as part of the text.
            showSurahHead = false
        )
    }
    val style = remember(options.arabicScale, typeface, ink) {
        typeface.arabicStyle(
            scale = options.arabicScale,
            color = ink,
            align = TextAlign.Start
        )
    }

    // Captured per block, and keyed on the block and its style: a
    // `TextLayoutResult` is only valid for the text it was measured from.
    var layout by remember(block, style) { mutableStateOf<TextLayoutResult?>(null) }

    val gutter: Dp = PageInsets.gutter(space.xl)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("flowing_block")
    ) {
        BasicText(
            text = block.text,
            style = style,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter)
                .testTag("flowing_block_text")
                .pointerInput(block, layout, gutter, density) {
                    detectTapGestures { tap ->
                        val result = layout ?: return@detectTapGestures
                        // The gutter has to come off: `padding` is a layout modifier
                        // on the same node, so the node's origin is the gutter's outer
                        // edge while the text layout's coordinates start at the text.
                        // Passing the tap through resolves it about one word off, and
                        // under RTL, consistently to the earlier verse.
                        val inText = tap.copy(
                            x = tap.x - with(density) { gutter.roundToPx() }
                        )
                        val offset = runCatching {
                            result.getOffsetForPosition(inText)
                        }.getOrNull() ?: return@detectTapGestures
                        val span = block.verseAt(offset) ?: return@detectTapGestures
                        // Matched on **both** numbers, not the ayah number alone.
                        //
                        // A block is twelve verses and is allowed to cross a surah
                        // boundary - 2:285 and 3:1 can share one - so an ayah number
                        // is not unique within the list. Today the longest such
                        // overlap is unreachable, because two surahs would have to
                        // contribute the same ayah number inside twelve verses, and
                        // the closest pair is 111 verses apart. So this is a trap
                        // rather than a live bug, and it is a trap in the direction
                        // that produced 523 unselectable verses elsewhere in this
                        // module: an identity that looks unique and is not.
                        ayahs.firstOrNull {
                            it.surahNumber == span.surahNumber &&
                                it.ayahNumber == span.ayahNumber
                        }?.let(onSelectVerse)
                    }
                }
                .semantics {
                    contentDescription = describeBlock(ayahs, strings.more.reader)
                    stateDescription = ayahs.firstOrNull()?.let { first ->
                        strings.more.pageWord + " " + first.pageNumber
                    }.orEmpty()
                    customActions = ayahs.map { ayah ->
                        CustomAccessibilityAction(
                            // The *same* identity the mushaf page announces, and for
                            // the same reason. This used to say only the ayah number
                            // while acting on the whole verse, so a screen-reader user
                            // choosing between two blocks heard "Select 3" and "Select
                            // 3" and could not tell which was which - and a block may
                            // cross a surah boundary, where the number alone does not
                            // name a verse.
                            "${strings.more.selectVerse} " +
                                "${ayah.surahNumber}:${ayah.ayahNumber}"
                        ) {
                            onSelectVerse(ayah)
                            true
                        }
                    }
                }
        )
    }
}

/**
 * What a screen reader announces for a block of flowing text.
 *
 * The extent and the page, because "12 verses" alone says nothing about *where* -
 * and a continuous block is not a page, so without the page number a reader
 * navigating by ear has nothing to check their place against.
 *
 * Built from [ReaderStrings] for the same reason the mushaf page's is: this was
 * another English sentence assembled in a file with no access to the strings, next
 * to a `stateDescription` that was already localized. The two halves of one node
 * speaking two languages is worse than either alone.
 */
private fun describeBlock(ayahs: List<Ayah>, reader: ReaderStrings): String {
    val first = ayahs.firstOrNull() ?: return ""
    val last = ayahs.last()
    return reader.blockAnnouncement.format(
        reader.reference(first.surahNumber, first.ayahNumber),
        reader.reference(last.surahNumber, last.ayahNumber),
        reader.verseCount(ayahs.size),
        first.pageNumber
    )
}
