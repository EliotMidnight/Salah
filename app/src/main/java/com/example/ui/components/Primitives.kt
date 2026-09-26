package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DotShape
import com.example.ui.theme.Space
import com.example.ui.theme.QuranShape
import com.example.ui.theme.Tonal
import com.example.ui.theme.layoutMetrics

/**
 * A tappable row: icon, title, supporting text, optional value, optional chevron.
 *
 * This is the app's workhorse. It replaces four near-identical private composables
 * (two in Settings, one in Prayer, one in Quran) that had each grown their own
 * padding, radius and semantics. A row is always at least 48dp tall, and the
 * whole row - not just the text - is the target.
 *
 * The icon is decorative by default and dropped from the accessibility tree, since
 * it almost always restates the title. Pass [iconDescription] when it carries
 * meaning the title does not.
 */
@Composable
fun ActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    selectedLabel: String? = null,
    contentDescription: String? = null,
    testTag: String? = null
) {
    val space = Space.current
    val minTarget = MaterialTheme.layoutMetrics.minTouchTarget

    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minTarget)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClickLabel = selectedLabel,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(vertical = space.md, horizontal = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clearAndSetSemantics { }
                )
                Spacer(Modifier.width(space.md))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(space.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (value != null) {
                Spacer(Modifier.width(space.sm))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics { }
                )
            }
        }
    }
}

/** A hairline between rows. One weight, one colour, used everywhere. */
@Composable
fun RowDivider(
    modifier: Modifier = Modifier,
    inset: Dp = 0.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = inset),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * A labelled container for a related group of rows.
 *
 * Plain surface, no border, no shadow. On the tinted page a white surface with
 * 12dp corners is already enough separation; adding a hairline on top of that is
 * the "excessive borders" problem, not a solution to it.
 */
@Composable
fun SectionGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Space.current.lg, vertical = Space.current.xs),
            content = { content() }
        )
    }
}

/**
 * The one place a "nothing here yet" state is built.
 *
 * Previously each list hand-rolled its own: an icon at 34dp or 48dp, then a
 * sentence, then sometimes a second sentence. Icon, short title, one line of
 * guidance, and an optional action.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    action: (@Composable () -> Unit)? = null
) {
    val space = Space.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.xxxl, horizontal = space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null || painter != null) {
            val iconModifier = Modifier
                .size(32.dp)
                .clearAndSetSemantics { }
            val iconTint = MaterialTheme.colorScheme.outline
            val vector = icon
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = iconModifier
                )
            } else if (vector != null) {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = iconModifier
                )
            }
            Spacer(Modifier.height(space.md))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(Modifier.height(space.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(space.lg))
            action()
        }
    }
}

/** Severity for [StatusBanner]. Only three meanings exist in this app. */
enum class BannerTone { Neutral, Success, Warning, Danger }

/**
 * One inline message: information, confirmation, a warning, or a failure.
 *
 * Replaces four unrelated treatments - a red adhan-playing bar, a
 * `primaryContainer` "reset to real time" chip, an `errorContainer` magnetic
 * interference card, and a bare `primary`-coloured status line - with one row
 * that has the same shape every time.
 */
@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Neutral,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val space = Space.current
    val semantic = Tonal.colors

    val (container, content) = when (tone) {
        BannerTone.Neutral ->
            MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface
        BannerTone.Success ->
            semantic.successContainer to semantic.onSuccessContainer
        BannerTone.Warning ->
            semantic.warningContainer to semantic.onWarningContainer
        BannerTone.Danger ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = container,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = space.md, vertical = space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .size(18.dp)
                        .clearAndSetSemantics { }
                )
                Spacer(Modifier.width(space.sm))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f)
            )
            if (action != null) {
                Spacer(Modifier.width(space.sm))
                action()
            }
        }
    }
}

/**
 * A key/value line, for the read-only diagnostic panels.
 *
 * The old version coloured its value with `primary`, which put the accent on ~12
 * lines per sheet and weakened the hierarchy. Values now use the normal text
 * colour, and the *label* carries the muted tone.
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasise: Boolean = false
) {
    val space = Space.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(132.dp)
        )
        Spacer(Modifier.width(space.sm))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/** A small circular status dot. Replaces six copies of the same 7-8dp Box. */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    description: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(DotShape)
            .background(color)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier.clearAndSetSemantics { }
                }
            )
    )
}

/**
 * A selectable option inside a sheet.
 *
 * Selection is carried by a filled check plus the accent text colour, not by a
 * coloured background *and* a border *and* a check. A selected row that is only a
 * slightly different shade of the sheet is invisible - which is what the old
 * unselected state amounted to, at roughly 1.05:1 against its own container.
 */
@Composable
fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val space = Space.current

    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .padding(vertical = space.xs)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = space.sm, vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (description != null) {
                    Spacer(Modifier.height(space.xxs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(space.sm))
                trailing()
            }
            if (selected) {
                Spacer(Modifier.width(space.sm))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics { }
                )
            }
        }
    }
}

/**
 * A compact segmented control for 2-5 mutually exclusive options.
 *
 * Used where the old code put a row of `Surface` chips with hand-rolled weights
 * and borders (auto-silent duration, per-prayer alert modes). Options are real
 * buttons with a selected state, so the group is navigable by keyboard and
 * announced correctly.
 */
@Composable
fun SegmentedOptions(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(space.xxs)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                        .clickable(
                            role = Role.Tab,
                            onClick = { onSelect(index) }
                        )
                        .semantics {
                            stateDescription = if (selected) "Selected" else "Not selected"
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = space.xs, vertical = space.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * A label, a live value and a slider, with the value announced to screen readers.
 *
 * The old sliders labelled themselves with a decorative "A" on each end and no
 * semantics, so TalkBack read out a bare percentage. This carries a real
 * [contentDescription] and a [stateDescription] of the current value.
 */
@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val space = Space.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { stateDescription = valueText }
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
                .semantics { contentDescription = label }
        )
    }
}

/** Small pill for a count or a short status word. */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Neutral
) {
    val semantic = Tonal.colors
    val space = Space.current
    val (container, content) = when (tone) {
        BannerTone.Neutral ->
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        BannerTone.Success ->
            semantic.successContainer to semantic.onSuccessContainer
        BannerTone.Warning ->
            semantic.warningContainer to semantic.onWarningContainer
        BannerTone.Danger ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = container, shape = CircleShape, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = space.sm, vertical = space.xxs)
        )
    }
}

/** Convenience for the many `border` variations the old screens hand-rolled. */
@Composable
fun hairlineBorder(color: Color = MaterialTheme.colorScheme.outlineVariant) =
    BorderStroke(1.dp, color)

/**
 * A search input.
 *
 * Takes the full width of its container, takes focus when it appears, and closes
 * the keyboard on submit. The Quran library used to share a row with the screen
 * title - both children `weight(1f)` - so the title collapsed to half width and
 * ellipsized the moment you typed, and the field needed a second tap to focus.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (onClear != null && value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboard?.hide()
                onSubmit?.invoke()
            }
        ),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .focusRequester(focusRequester)
    )
}

/**
 * A floating row of pill tabs, drawn over the content beneath it.
 *
 * The Quran library uses this instead of a Material `TabRow`. The difference
 * matters: a `TabRow` is a full-width band with an underline, which claims
 * horizontal space and adds a second rule to the page. Pills carry their own
 * shape, so the row reads as controls sitting on the content rather than as
 * another structural element - and because each pill has a visible boundary,
 * the selected state does not need an indicator to be legible.
 *
 * Callers must reserve the row's height themselves (see `PillTabBarHeight`)
 * before their scrolling content, otherwise the first row can slide underneath.
 */
val PillTabBarHeight = 48.dp

@Composable
fun PillTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = space.lg, vertical = space.sm),
        horizontalArrangement = Arrangement.spacedBy(space.sm)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = QuranShape.pill,
                border = if (selected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PillTabBarHeight)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    .semantics { stateDescription = label }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = space.xs, vertical = space.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
