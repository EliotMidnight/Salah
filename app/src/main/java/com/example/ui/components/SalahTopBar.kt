package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UserLocation
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalSuccessColors
import com.example.ui.theme.Shapes

@Composable
fun SalahTopBar(
    location: UserLocation,
    onLocationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTransparentOnSky: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isOnline: Boolean = false,
    isSyncing: Boolean = false,
    onRefreshClick: (() -> Unit)? = null,
    showBrand: Boolean = true,
    showSettings: Boolean = true,
    compact: Boolean = false
) {
    val successColors = LocalSuccessColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(
                horizontal = if (compact) 8.dp else 16.dp,
                vertical = if (compact) 4.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity + Sync Status Pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBrand) {
                Text(
                    text = "SALAH",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing,
                    color = contentColor,
                    modifier = Modifier.testTag("app_brand_title")
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Dynamic Online/Offline Badge with tap-to-refresh (48dp target)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isTransparentOnSky -> contentColor.copy(alpha = 0.16f)
                            isOnline -> successColors.successContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        }
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isTransparentOnSky) contentColor.copy(alpha = 0.30f) else Color.Transparent
                        ),
                        CircleShape
                    )
                    .clickable(
                        enabled = onRefreshClick != null,
                        role = Role.Button,
                        onClickLabel = "Refresh prayer data",
                        onClick = { onRefreshClick?.invoke() }
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("sync_status_badge"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = if (isSyncing) 0 else if (isOnline) 1 else 2,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT))
                    },
                    label = "sync_status_swap"
                ) { status ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (status) {
                            0 -> {
                                val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = ExpressiveMotion.duration(1000),
                                            easing = LinearEasing
                                        ),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "sync_spin"
                                )
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Syncing",
                                    tint = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .rotate(rotation)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SYNCING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary
                                )
                            }
                            1 -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isTransparentOnSky) contentColor else successColors.success)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ONLINE SYNC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransparentOnSky) contentColor else successColors.onSuccessContainer
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Offline indicator",
                                    tint = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions: Location chip & Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(Shapes.small)
                    .background(
                        if (isTransparentOnSky) contentColor.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isTransparentOnSky) contentColor.copy(alpha = 0.30f) else Color.Transparent
                        ),
                        Shapes.small
                    )
                    .clickable(role = Role.Button, onClickLabel = "Change location", onClick = onLocationClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("location_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Change Location",
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }

            if (showSettings) {
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
