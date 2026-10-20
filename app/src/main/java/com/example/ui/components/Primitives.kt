package com.example.ui.components

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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.DotShape
import com.example.ui.theme.IconSize
import com.example.ui.theme.Space
import com.example.ui.theme.Tonal
import com.example.ui.theme.layoutMetrics

/**
 * A tappable row: icon, title, supporting text, optional value, optional chevron.
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
    destructive: Boolean = false,
    isSelected: Boolean = false,
    selectedLabel: String? = null,
    contentDescription: String? = null,
    testTag: String? = null
) {
    val space = Space.current
    val minTarget = MaterialTheme.layoutMetrics.minTouchTarget
    // At accessibility sizes a title and a long value cannot share a line
    // without one of them ellipsizing. Stacking the value under the title gives
    // both the full row width; the normal side-by-side layout is unchanged.
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val valueColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                // space.md, not space.sm: inside a SectionGroup this row sat at
                // 24dp from the screen edge while ToggleRow sat at 28dp, so
                // titles and right-hand controls in the same card zig-zagged.
                .padding(vertical = space.md, horizontal = space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(IconSize.xl)
                        .clearAndSetSemantics { }
                )
                Spacer(Modifier.width(space.md))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        destructive -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
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
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (value != null && largeText) {
                    Spacer(Modifier.height(space.xxs))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = valueColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (value != null && !largeText) {
                Spacer(Modifier.width(space.sm))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    // onSurfaceVariant, not outline. `outline` measures 2.89:1
                    // against `surface` in dark mode, and this chevron is the
                    // only signal that the row opens something. M3 tints list
                    // chevrons with onSurfaceVariant for the same reason.
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(IconSize.lg)
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
 * In the monochrome theme there is no visible container: page and surface are the
 * same colour, so a card would be invisible. Grouping comes from the hairline
 * dividers between items and the spacing around the group.
 */
@Composable
fun SectionGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val space = Space.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = space.xs),
        content = { content() }
    )
}

/**
 * The one place a "nothing here yet" state is built.
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
                .size(IconSize.huge)
                .clearAndSetSemantics { }
            val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
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
 * Monochrome: no filled colour block. Tone is signalled by an icon and by the
 * semantic colour of that icon, not by tinting the whole container.
 */
@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Neutral,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    val space = Space.current
    val semantic = Tonal.colors

    val iconTint = when (tone) {
        BannerTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        BannerTone.Success -> semantic.success
        BannerTone.Warning -> semantic.warning
        BannerTone.Danger -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
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
                    tint = iconTint,
                    modifier = Modifier
                        .size(IconSize.md)
                        .clearAndSetSemantics { }
                )
                Spacer(Modifier.width(space.sm))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
    val strings = LocalStrings.current

    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) strings.more.selected else strings.more.notSelected }
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
                        .size(IconSize.lg)
                        .clearAndSetSemantics { }
                )
            }
        }
    }
}

/**
 * A compact segmented control for 2-5 mutually exclusive options.
 */
@Composable
fun SegmentedOptions(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
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
                            stateDescription = if (selected) strings.more.selected else strings.more.notSelected
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
                            maxLines = 2,
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(space.sm))
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

/**
 * A label, an optional subtitle, and a switch whose whole row is the target.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    role: Role = Role.Switch
) {
    val space = Space.current
    val strings = LocalStrings.current

    val toggle: () -> Unit = onClick ?: { onCheckedChange(!checked) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
            .clickable(onClick = toggle)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = space.md, vertical = space.sm)
            .semantics(mergeDescendants = true) {
                this.role = role
                stateDescription = if (role == Role.Switch) {
                    if (checked) strings.more.stateOn else strings.more.stateOff
                } else {
                    ""
                }
                onClick(label = if (role == Role.Switch) null else strings.more.actionChange) {
                    toggle()
                    true
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(space.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(space.md))
        // The switch is decorative here: the row carries the click and the
        // semantics, so exposing it as a second focus stop would double-announce.
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
    }
}

/**
 * A search input.
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
    val strings = LocalStrings.current

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(IconSize.lg))
        },
        trailingIcon = {
            if (onClear != null && value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        // The search field's only button, and it was an unlabelled
                        // one: "button" beside a query, with no way to know it is
                        // the query it clears.
                        contentDescription = strings.more.actionClear,
                        modifier = Modifier.size(IconSize.md)
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
 * Whether this colour is dark enough to need light ink on it.
 */
fun Color.isDarkSurface(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
