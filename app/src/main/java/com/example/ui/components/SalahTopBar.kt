package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserLocation

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
    showSettings: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_spin"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity + Sync Status Pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBrand) {
                Text(
                    text = "SALAH",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = contentColor,
                    modifier = Modifier.testTag("app_brand_title")
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Dynamic Online/Offline Badge with tap-to-refresh
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        when {
                            isTransparentOnSky -> contentColor.copy(alpha = 0.16f)
                            isOnline -> Color(0xFF2E7D32).copy(alpha = 0.16f)
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
                    .clickable(enabled = onRefreshClick != null) { onRefreshClick?.invoke() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
                    .testTag("sync_status_badge"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Syncing",
                            tint = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(11.dp)
                                .rotate(rotation)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SYNCING",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary
                        )
                    } else if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isTransparentOnSky) Color(0xFF81C784) else Color(0xFF2E7D32))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ONLINE SYNC",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (isTransparentOnSky) contentColor else Color(0xFF1B5E20)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline indicator",
                            tint = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OFFLINE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (isTransparentOnSky) contentColor else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Actions: Location chip & Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isTransparentOnSky) contentColor.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isTransparentOnSky) contentColor.copy(alpha = 0.30f) else Color.Transparent
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onLocationClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("location_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Change Location",
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location.name,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
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
