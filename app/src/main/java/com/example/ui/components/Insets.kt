package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Camera-hole / status-bar safe spacer.
 *
 * Reserves the taller of the status-bar and display-cutout insets plus a small
 * extra gap, so titles and pills never slide under the camera hole — including
 * on devices where the status bar is hidden (transient) but the cutout remains.
 */
@Composable
fun CameraHoleSpacer(
    extra: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val topPx = maxOf(
        WindowInsets.statusBars.getTop(density),
        WindowInsets.displayCutout.getTop(density)
    )
    Spacer(
        modifier = modifier.height(
            with(density) { topPx.toDp() } + extra
        )
    )
}
