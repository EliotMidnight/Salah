package com.example.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.example.data.quran.QuranBrowse
import com.example.data.model.QuranFontFace
import com.example.data.model.QuranPaperTone
import com.example.data.model.QuranPinchTarget
import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranReadingOptions
import com.example.data.model.QuranScrollDirection
import com.example.ui.components.LabeledSlider
import com.example.ui.components.OptionSheet
import com.example.ui.components.SegmentedOptions
import com.example.ui.components.ToggleRow
import com.example.ui.components.isDarkSurface
import com.example.ui.localization.LocalStrings
import com.example.ui.localization.ReaderStrings
import com.example.ui.theme.QuranFonts
import com.example.ui.theme.QuranPaper
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/** The translations this build ships. */
private data class Translation(
    val edition: String,
    val label: String
)

/**
 * The translations on offer.
 */
private val Translations = listOf(
    Translation("English (Saheeh International)", "English — Saheeh International")
)

/**
 * Reading options.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingOptionsSheet(
    options: QuranReadingOptions,
    onOptionsChange: (QuranReadingOptions) -> Unit,
    onDismiss: () -> Unit
) {
    val space = Space.current
    val strings = LocalStrings.current
    // future change to the threshold would fix two screens and not this one, which is
    // exactly the drift the shared function exists to prevent.
    val isDark = MaterialTheme.colorScheme.background.isDarkSurface()

    OptionSheet(
        title = strings.more.readingOptions,
        onDismiss = onDismiss
    ) {
        // --- Layout -----------------------------------------------------------
        // - "per ayah" - is now the per-page mushaf with verses broken out,
        // which is what it was drawing all along.
        GroupLabel(strings.more.selectLayoutTitle)
        Spacer(Modifier.height(space.xs))
        SegmentedOptions(
            options = listOf(
                strings.more.reader.layoutPerPage,
                strings.more.reader.layoutContinuousSurah
            ),
            selectedIndex = if (options.layout == QuranReadingLayout.PER_PAGE) 0 else 1,
            onSelect = { index ->
                onOptionsChange(
                    options.copy(
                        layout = if (index == 0) {
                            QuranReadingLayout.PER_PAGE
                        } else {
                            QuranReadingLayout.CONTINUOUS
                        }
                    )
                )
            }
        )

        // --- Verse presentation -----------------------------------------------
        // Applies to whichever layout is above. Not a mode of its own.
        Spacer(Modifier.height(space.xl))
        ToggleRow(
            title = strings.more.reader.perVerseTitle,
            subtitle = strings.more.reader.perVerseDescription,
            checked = options.perVerse,
            onCheckedChange = { onOptionsChange(options.copy(perVerse = it)) },
            testTag = "options_per_verse"
        )

        // --- Axis -------------------------------------------------------------
        // Always both. The old sheet disabled horizontal while continuous was
        // chosen and printed a line explaining why, on the grounds that text
        // without page breaks cannot scroll sideways. It can - it is one wide
        // column you pan across - so there is nothing left to explain and
        // nothing left to disable.
        Spacer(Modifier.height(space.xl))
        GroupLabel(strings.more.reader.scrollDirection)
        Spacer(Modifier.height(space.xs))
        SegmentedOptions(
            options = listOf(strings.more.reader.scrollVertical, strings.more.reader.scrollHorizontal),
            selectedIndex = if (options.scroll == QuranScrollDirection.VERTICAL) 0 else 1,
            onSelect = { index ->
                onOptionsChange(
                    options.copy(
                        scroll = if (index == 0) {
                            QuranScrollDirection.VERTICAL
                        } else {
                            QuranScrollDirection.HORIZONTAL
                        }
                    )
                )
            }
        )

        // --- Arabic text ------------------------------------------------------
        Spacer(Modifier.height(space.xl))
        GroupLabel(strings.more.reader.arabicTextSize)
        Spacer(Modifier.height(space.xs))
        LabeledSlider(
            label = strings.more.reader.arabicTextSize,
            valueText = "${(options.arabicScale * 100).toInt()}%",
            value = options.arabicScale,
            onValueChange = { onOptionsChange(options.copy(arabicScale = it)) },
            valueRange = QuranReadingOptions.ArabicScaleRange
        )

        // --- Typeface ---------------------------------------------------------
        Spacer(Modifier.height(space.xl))
        GroupLabel(strings.more.reader.quranFont)
        Spacer(Modifier.height(space.xs))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space.sm),
            verticalArrangement = Arrangement.spacedBy(space.sm)
        ) {
            QuranFontFace.entries.forEach { face ->
                FontFaceTile(
                    face = face,
                    selected = options.font == face,
                    onClick = { onOptionsChange(options.copy(font = face)) }
                )
            }
        }

        // --- Paper ------------------------------------------------------------
        Spacer(Modifier.height(space.xl))
        GroupLabel(strings.more.reader.backgroundColour)
        Spacer(Modifier.height(space.xs))
        PaperSwatches(
            selected = options.paper,
            isDark = isDark,
            onSelect = { onOptionsChange(options.copy(paper = it)) }
        )

        // --- Pinch ------------------------------------------------------------
        Spacer(Modifier.height(space.xl))
        GroupLabel(strings.more.reader.pinchBehaviour)
        Spacer(Modifier.height(space.xs))
        SegmentedOptions(
            options = listOf(strings.more.reader.pinchZoomView, strings.more.reader.pinchTextSize),
            selectedIndex = if (options.pinchTarget == QuranPinchTarget.VIEW_SCALE) 0 else 1,
            onSelect = { index ->
                onOptionsChange(
                    options.copy(
                        pinchTarget = if (index == 0) {
                            QuranPinchTarget.VIEW_SCALE
                        } else {
                            QuranPinchTarget.TEXT_SIZE
                        }
                    )
                )
            }
        )

        // --- Translation ------------------------------------------------------
        Spacer(Modifier.height(space.xl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleRow(
                title = strings.more.reader.showTranslationLabelShort,
                subtitle = Translations.first().label,
                checked = options.showTranslation,
                onCheckedChange = { onOptionsChange(options.copy(showTranslation = it)) },
                testTag = "options_show_translation"
            )
        }

        if (options.showTranslation) {
            Spacer(Modifier.height(space.sm))
            LabeledSlider(
                label = strings.more.reader.translationSize,
                valueText = "${(options.translationScale * 100).toInt()}%",
                value = options.translationScale,
                onValueChange = { onOptionsChange(options.copy(translationScale = it)) },
                valueRange = QuranReadingOptions.TranslationScaleRange
            )
            Spacer(Modifier.height(space.sm))
            LivePreview(
                arabicScale = options.arabicScale,
                translationScale = options.translationScale,
                showTranslation = options.showTranslation
            )
        }
    }
}

/** A quiet group label. Not a heading - the sheet title already owns that. */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

/**
 * One typeface, shown by its own glyphs.
 */
@Composable
private fun FontFaceTile(
    face: QuranFontFace,
    selected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val space = Space.current

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(QuranShape.tile)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, borderColour(selected)),
                QuranShape.tile
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                stateDescription = if (selected) strings.more.selected else strings.more.notSelected
            }
            .padding(vertical = space.sm, horizontal = space.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuranFonts.Provide(face) {
            Text(
                text = "بِسْمِ",
                style = QuranFonts.of(face)
                    .arabicStyle(
                        scale = 0.62f,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        align = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(space.xxs))
        Text(
            text = faceLabel(face),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun borderColour(selected: Boolean) =
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

/**
 * Human name for a face, written out rather than showing its storage key.
 */
internal fun faceLabel(face: QuranFontFace): String = when (face) {
    QuranFontFace.AMIRI -> "Amiri"
    QuranFontFace.AMIRI_QURAN -> "Amiri Quran"
    QuranFontFace.LATEEF -> "Lateef"
    QuranFontFace.SCHEHERAZADE_NEW -> "Scheherazade New"
    QuranFontFace.HARMATTAN -> "Harmattan"
}

/**
 * The paper swatches.
 */
@Composable
private fun PaperSwatches(
    selected: QuranPaperTone,
    isDark: Boolean,
    onSelect: (QuranPaperTone) -> Unit
) {
    val strings = LocalStrings.current
    val space = Space.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space.sm)
        ) {
            QuranPaperTone.entries.forEach { tone ->
                val isSelected = tone == selected
                Surface(
                    color = QuranPaper.swatch(tone, isDark),
                    shape = QuranShape.tile,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            QuranPaper.selectionRing(isDark)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(QuranShape.tile)
                        .clickable(role = Role.RadioButton, onClick = { onSelect(tone) })
                        .semantics {
                            contentDescription = paperLabel(tone, strings.more.reader)
                            stateDescription = if (isSelected) {
                                strings.more.selected
                            } else {
                                strings.more.notSelected
                            }
                        }
                ) {}
            }
        }
        Spacer(Modifier.height(space.xs))
        Text(
            text = paperLabel(selected, strings.more.reader),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The name of one paper, in the reader's language.
 *
 * A `when` over the tone rather than a name on the enum, because a name on the enum
 * would be English by construction and the tone enum knows nothing about the interface
 *
 * [QuranPaperTone.DEFAULT] is [ReaderStrings.backgroundDefault] rather than a colour,
 * because "app default" is a different kind of answer: it is the absence of a choice,
 * and calling it a colour would imply the app has a paper of its own.
 */
internal fun paperLabel(tone: QuranPaperTone, reader: ReaderStrings): String = when (tone) {
    QuranPaperTone.DEFAULT -> reader.backgroundDefault
    QuranPaperTone.RED -> reader.paperRose
    QuranPaperTone.ORANGE -> reader.paperApricot
    QuranPaperTone.YELLOW -> reader.paperSand
    QuranPaperTone.GREEN -> reader.paperSage
    QuranPaperTone.BLUE -> reader.paperMist
    QuranPaperTone.INDIGO -> reader.paperIndigo
    QuranPaperTone.VIOLET -> reader.paperLilac
}

/**
 * A live sample of the two sliders' effect.
 */
@Composable
private fun LivePreview(
    arabicScale: Float,
    translationScale: Float,
    showTranslation: Boolean
) {
    val space = Space.current
    val strings = LocalStrings.current

    val sample = remember { QuranBrowse.ayah(1, 1) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = QuranShape.card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(space.md)) {
            if (sample != null) {
                VerseArabic(
                    ayah = sample,
                    scale = arabicScale,
                    ink = MaterialTheme.colorScheme.onSurface
                )
            }
            if (showTranslation) {
                Spacer(Modifier.height(space.sm))
                Text(
                    text = strings.more.translationCreditLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
