package com.example.ui.qibla

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.engine.MagneticFieldStatus
import com.example.ui.SalahUiState
import com.example.ui.components.BannerTone
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.OptionSheet
import com.example.ui.components.StatusBanner
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.Space
import kotlin.math.abs

/**
 * Qibla.
 *
 * The compass is the screen, so it gets the space and everything else is
 * supporting text. The previous version put a 22dp "verified" icon and a title
 * above the compass, then a bordered warning card, then the compass, then more
 * bordered cards - four competing frames around one instrument.
 */
@Composable
fun QiblaScreen(
    state: SalahUiState,
    onToggleTrueNorth: () -> Unit = {},
    onFetchLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current

    var showSunSheet by rememberSaveable { mutableStateOf(false) }
    var showCalibration by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        QiblaHeader(
            bearing = state.qiblaBearing,
            delta = state.qiblaDelta,
            isFacing = state.isFacingQibla,
            isTrueNorth = state.useTrueNorth
        )

        Spacer(Modifier.height(space.md))

        if (state.magneticStatus == MagneticFieldStatus.INTERFERENCE) {
            StatusBanner(
                message = "${strings.more.magneticInterference}: ${strings.more.magneticInterferenceMessage}",
                tone = BannerTone.Warning,
                modifier = Modifier.testTag("magnetic_warning")
            )
            Spacer(Modifier.height(space.md))
        }

        QiblaDirectionFinder(
            state = state,
            onToggleTrueNorth = onToggleTrueNorth,
            onFetchLocation = onFetchLocation,
            onShowCalibrationTip = { showCalibration = true },
            onShowSunVerification = { showSunSheet = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showSunSheet) {
        SunReferenceSheet(
            state = state,
            onDismiss = { showSunSheet = false }
        )
    }

    if (showCalibration) {
        ConfirmDialog(
            title = strings.more.calibrationTitle,
            message = "${strings.more.calibrationMessage}\n\n" +
                "${strings.more.ambientField}: ${state.magneticFieldMagnitude} µT (${state.magneticStatus.label}) · " +
                "${strings.more.sensorAccuracy}: ${state.compassAccuracy}",
            confirmLabel = strings.done,
            onConfirm = { showCalibration = false },
            onDismiss = { showCalibration = false }
        )
    }
}

/**
 * The bearing to the Kaaba, and whether you are facing it.
 *
 * The big number is the *destination* bearing, not a live compass heading: it
 * depends only on where you are, so it holds still while you turn. It used to be
 * rendered with no label at all, sitting directly above the dial, where it read
 * as a heading that had frozen - which is precisely the question it cannot
 * answer. The label now sits above it, and the true/magnetic north reference
 * reads as a caption instead of a second value.
 */
@Composable
private fun QiblaHeader(
    bearing: Float,
    delta: Float,
    isFacing: Boolean,
    isTrueNorth: Boolean,
    modifier: Modifier = Modifier
) {
    val space = Space.current
    val strings = LocalStrings.current
    val semantic = com.example.ui.theme.Tonal.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.more.qiblaBearing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(space.xxs))
        Text(
            text = String.format(java.util.Locale.getDefault(), "%.0f°", bearing),
            style = MaterialTheme.typography.displayMedium,
            color = if (isFacing) semantic.success else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(space.xs))
        Text(
            text = if (isFacing) {
                strings.more.isFacingQibla
            } else {
                // Localized direction words. This used to be built inline in
                // English, which is how an Arabic or Urdu user ended up reading
                // English guidance on an otherwise localized screen.
                strings.more.turnBy.format(
                    String.format(java.util.Locale.getDefault(), "%.0f°", abs(delta)),
                    if (delta >= 0) strings.more.rightOfQibla else strings.more.leftOfQibla
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isTrueNorth) strings.trueNorth else strings.magneticNorth,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Cross-check the bearing against the sun.
 *
 * A genuinely useful field tool, so it is kept - but the explanation is now one
 * sentence chosen from five cases, rather than a bulleted list of raw numbers the
 * user had to interpret themselves.
 */
@Composable
private fun SunReferenceSheet(state: SalahUiState, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val sun = state.sunPosition

    OptionSheet(
        title = strings.more.solarReferenceTitle,
        onDismiss = onDismiss
    ) {
        if (sun == null) {
            Text(
                text = strings.more.loading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@OptionSheet
        }

        val relative = (state.qiblaBearing - sun.azimuth + 360f) % 360f
        val explanation = when {
            !sun.isSunVisible -> strings.more.sunBelowHorizon
            abs(relative) < 15f -> strings.more.sunAligned
            relative in 15f..165f -> strings.more.sunToTheLeft.format(relative.toInt())
            relative in 195f..345f -> strings.more.sunToTheRight.format((360 - relative).toInt())
            else -> strings.more.sunOpposite
        }

        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(Space.current.lg))

        com.example.ui.components.DetailList(
            items = listOf(
                strings.more.sunAzimuth to String.format(java.util.Locale.getDefault(), "%.1f°", sun.azimuth),
                strings.more.sunAltitudeValue to String.format(java.util.Locale.getDefault(), "%.1f°", sun.altitude),
                strings.more.qiblaBearing to String.format(java.util.Locale.getDefault(), "%.1f°", state.qiblaBearing)
            )
        )
    }
}
