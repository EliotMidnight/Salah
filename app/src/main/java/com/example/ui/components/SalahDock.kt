package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.Glass
import com.example.ui.theme.Motion
import com.example.ui.theme.SalahReduceMotion
import com.example.ui.theme.Space
import com.example.ui.theme.pressScale

/**
 * One destination in the floating dock. The app owns the strings; the dock does
 * not read [com.example.ui.localization.LocalStrings], so screenshot tests can
 * feed fixed names and the component stays a pure layout.
 */
internal data class DockItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * The app's only glass surface.
 *
 * It floats over full-height content instead of taking a bottom-bar slot, which
 * is what gives the material something meaningful to blur. Selection is carried
 * by three channels - shape/fill, icon fill, and label weight - so it never
 * depends on colour alone. At large font scales the labels drop out and the
 * names move into semantics, keeping every icon a 64dp-tall target.
 */
@Composable
internal fun SalahDock(
    items: List<DockItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showLabels = LocalDensity.current.fontScale < Glass.labelsAtFontScaleBelow

    GlassSurface(
        shape = CircleShape,
        modifier = modifier
            .widthIn(max = Glass.dockMaxWidth)
            .fillMaxWidth()
            .height(Glass.dockHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
                .padding(horizontal = Glass.dockHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                DockDestination(
                    item = item,
                    selected = item.route == selectedRoute,
                    showLabel = showLabels,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DockDestination(
    item: DockItem,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = SalahReduceMotion.Local.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) pressScale else 1f,
        animationSpec = Motion.springQuick(),
        label = "dockPressScale"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.standard(),
        label = "dockIndicatorAlpha"
    )
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .height(Glass.dockHeight)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null
            )
            .then(
                if (showLabel) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = item.label }
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(Glass.indicatorWidth)
                .height(Glass.indicatorHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = indicatorAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Glass.iconSize)
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(Glass.indicatorGap))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The space a scrolling screen must leave at the bottom so its last item can
 * clear the floating dock. The dock height, bottom gap and breathing room are
 * tokens; the navigation-bar inset is read from the same window the scaffold
 * pads the dock with, so the two cannot drift apart.
 */
@Composable
internal fun dockContentPadding(): Dp {
    val navigationBar = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    return Glass.dockHeight + Glass.dockBottomGap + navigationBar + Space.current.lg
}
