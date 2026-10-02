package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SectionLabelStyle
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics

/**
 * The frame every screen sits in.
 *
 * Previously each of the five screens hand-rolled its own version of this and
 * they had drifted apart: Home centred at 680dp with no horizontal padding,
 * Settings centred at 680dp *with* 16dp padding, Prayer at 680dp, Qibla at
 * 680dp, and only two of the five respected the display cutout. Now there is one
 * implementation and one reading measure.
 *
 * @param onBack when non-null, shows a back affordance and the screen is treated
 *   as a pushed destination. Top-level tabs pass null.
 * @param title pass null to drop the top bar entirely. The status-bar inset is
 *   still applied, so content does not slide under the clock - only the title
 *   row goes. Used by the two screens whose heading merely repeated the bottom
 *   navigation label.
 *
 * The insets come from [SafeArea] rather than from `WindowInsets.statusBars`
 * directly, which is the whole point of that file: the status bar and the cutout
 * are one fact, and a caller that asks only for the status bar is not asking the
 * question. Both were true here - these two functions were the copy that got the
 * cutout wrong, so on a device whose camera hole reaches the top edge a title sat
 * underneath it. The horizontal reserve is the half nobody had: rotated, the cutout
 * is 30-40dp deep at the left or right edge against a 16dp gutter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val space = Space.current
    val sides = SafeArea.sides()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (title != null) {
            ScreenTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                actions = actions
            )
        } else {
            Spacer(Modifier.height(SafeArea.top()))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MaterialTheme.layoutMetrics.contentMaxWidth)
                    .padding(horizontal = space.lg + sides),
                content = { content(PaddingValues(bottom = space.xxxl)) }
            )
        }

        bottomBar()
    }
}

/**
 * The single top bar.
 *
 * A title, an optional subtitle, an optional back arrow and a trailing action
 * slot. Marked as a heading for screen readers so "where am I?" is answerable
 * from the accessibility tree alone.
 */
@Composable
fun ScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val space = Space.current
    val sides = SafeArea.sides()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = SafeArea.top())
            .padding(
                start = space.lg + sides,
                end = space.sm + sides,
                top = space.md,
                bottom = space.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(MaterialTheme.layoutMetrics.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(space.xs))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onBack != null) Spacer(Modifier.width(space.sm))
        actions()
    }
}

/**
 * A quiet group label.
 *
 * Not a heading - the screen title already owns that role. This exists to say
 * "the next few rows belong together" without competing for attention, which is
 * what the old all-caps accent-coloured headers were doing on every screen.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    Text(
        text = text.uppercase(),
        style = SectionLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = space.xl, bottom = space.sm)
    )
}

/**
 * Vertical rhythm for a screen that manages its own scrolling (lists, grids).
 *
 * Same measure and insets as [ScreenScaffold] so a lazy screen lines up pixel
 * for pixel with a scrolling one.
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = MaterialTheme.layoutMetrics.contentMaxWidth)
            .padding(horizontal = Space.current.lg + SafeArea.sides()),
        verticalArrangement = Arrangement.Top,
        content = { content() }
    )
}
