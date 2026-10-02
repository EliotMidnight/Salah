package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IconSize
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable

/**
 * One bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onDismissRequest: () -> Unit = onDismiss,
    content: @Composable ColumnScope.() -> Unit
) {
    val space = Space.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = space.lg)
                .padding(bottom = space.xxl)
        ) {
            SheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
            Spacer(Modifier.height(space.xs))
            content()
            Spacer(Modifier.height(space.lg))
        }
    }
}

/**
 * A bottom sheet whose body is a lazy list.
 *
 * ### Why this exists
 *
 * [OptionSheet] puts its content in a `Column` with a `verticalScroll`, which
 * composes **every** child whether or not it is on screen. That is fine for a
 * settings sheet with a dozen rows and wrong for a list of anything a reader can
 * browse: the Quran index drew all 114 surahs, every saved verse, and all 604 page
 * numbers through that path, on the frame the sheet opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionListSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Content above the list, which does not scroll with it. */
    header: @Composable ColumnScope.() -> Unit = {},
    /**
     * The list. Scrolled, lazy, and bounded by the sheet.
     */
    list: LazyListScope.() -> Unit
) {
    val space = Space.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(horizontal = space.lg)) {
                SheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
            }
            header()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    // Bounded, so the list is a list and the sheet is a sheet. The
                    // cap leaves room for the header, the navigation bar and a row
                    // or two of the list itself, which is what a reader scrolling it
                    // expects to see.
                    .heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = space.xxl)
            ) {
                list()
            }
        }
    }
}

/** Title, optional explanation, and a close affordance. Used by [OptionSheet]. */
@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val space = Space.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = space.xs, bottom = space.md),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
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
        Spacer(Modifier.width(space.sm))
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(MaterialTheme.layoutMetrics.minTouchTarget)
                .testTag("sheet_close")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A confirmation dialog.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = message?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = dismissLabel?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * A short, scrollable list of read-only facts, used by the diagnostic panels.
 */
@Composable
fun DetailList(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, (label, value) ->
            if (index > 0) RowDivider()
            DetailRow(label = label, value = value)
        }
    }
}

/** Horizontal run of equally-weighted options, wrapping if they do not fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space.sm),
        verticalArrangement = Arrangement.spacedBy(space.sm)
    ) {
        labels.forEachIndexed { index, label ->
            OptionChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

/** A single selectable chip. */
@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier.heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget)
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .padding(horizontal = space.md, vertical = space.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Bottom padding that clears the gesture bar / navigation bar. */
@Composable
fun navigationBarSpacer(): Dp {
    val density = LocalDensity.current
    val insets = WindowInsets.navigationBars.getBottom(density)
    return Space.current.xl + with(density) { insets.toDp() }
}
