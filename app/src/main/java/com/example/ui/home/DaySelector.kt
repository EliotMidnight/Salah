package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.isolateLtr
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.Space
import com.example.ui.theme.layoutMetrics
import java.time.LocalDate

/**
 * Previous / next day, with the current day always one tap away.
 */
@Composable
fun DaySelector(
    selectedDate: LocalDate,
    isToday: Boolean,
    hijriLabel: String,
    gregorianLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    // The arrows were two unlabelled buttons: a screen reader walked the row as
    // "button, Friday 14 September, button", so the only date controls in the app
    // had no names. The month sheet's arrows carry theirs; these did not.
    val dateNav = LocalStrings.current.dateNav
    // The Hijri date is a Latin-script, digit-first run ("3 Rabi' al-Thani
    // 1448 AH"). In an RTL paragraph the bidi algorithm otherwise moved the day
    // to the end of the line. It is shown without a "today" suffix so the
    // picker reads the same on the current day as on any other.
    val hijriRun = isolateLtr(hijriLabel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.layoutMetrics.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("day_prev")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = dateNav.previousDay,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = !isToday, onClick = onToday)
                .padding(vertical = space.xs)
                .semantics { contentDescription = "$gregorianLabel, $hijriLabel" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The date is the reason this row exists, so it wraps rather than
            // ellipsizing at large Dynamic Type. At normal sizes both lines stay
            // single-line, which is why the arrows still frame one centred block.
            Text(
                text = gregorianLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = hijriRun,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onNext, modifier = Modifier.testTag("day_next")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = dateNav.nextDay,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
