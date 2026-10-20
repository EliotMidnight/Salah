package com.example.ui.system

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.LocationStatus
import com.example.ui.components.BannerTone
import com.example.ui.components.StatusBanner
import com.example.data.location.LocationFailure
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.layoutMetrics

/**
 * The one location banner Settings and Qibla both use.
 *
 * The message says what failed; the action is chosen from the *current* system
 * access, not from the stale failure reason. A reader who turned location
 * services back on and returned to the app should see "Try again", not be sent
 * to the same settings screen again.
 */
@Composable
fun LocationAccessBanner(
    status: LocationStatus?,
    systemAccess: SystemAccessState,
    actions: SystemAccessActions,
    modifier: Modifier = Modifier
) {
    // Settings passes null before the reader has tried GPS. If the permission
    // or the service was already off, say so now instead of waiting for a
    // failed attempt to make the problem visible. The current system state wins
    // over a stale "resolved" status too: revoking permission while the app is
    // backgrounded must not leave a reassuring "GPS: ..." line behind.
    val effective = locationBannerStatus(status, systemAccess) ?: return
    val strings = LocalStrings.current
    val failed = effective as? LocationStatus.Failed

    val message = when (effective) {
        is LocationStatus.Acquiring -> strings.more.locationAcquiring
        is LocationStatus.Resolved -> {
            val template = if (effective.isFresh) {
                strings.more.locationResolved
            } else {
                strings.more.locationResolvedCached
            }
            template.format(effective.name, effective.country)
        }

        is LocationStatus.Failed -> when (effective.reason) {
            LocationFailure.PERMISSION_NOT_GRANTED ->
                strings.more.locationErrorNoPermission
            LocationFailure.SERVICES_DISABLED ->
                strings.more.locationErrorServicesOff
            LocationFailure.NO_SIGNAL ->
                strings.more.locationErrorNoSignal
        }
    }

    val actionLabel: String?
    val action: (() -> Unit)?
    if (failed == null) {
        actionLabel = null
        action = null
    } else {
        when (failed.reason) {
            LocationFailure.SERVICES_DISABLED -> {
                if (systemAccess.locationServicesEnabled) {
                    actionLabel = strings.system.tryAgain
                    action = actions.retryLocation
                } else {
                    actionLabel = strings.system.turnOnLocation
                    action = actions.openLocationSettings
                }
            }

            LocationFailure.PERMISSION_NOT_GRANTED -> when (systemAccess.locationPermission) {
                PermissionAccess.GRANTED -> {
                    actionLabel = strings.system.tryAgain
                    action = actions.retryLocation
                }

                PermissionAccess.DENIED_PERMANENTLY -> {
                    actionLabel = strings.system.openSettings
                    action = actions.openAppSettings
                }

                else -> {
                    actionLabel = strings.system.allowLocation
                    action = actions.requestLocation
                }
            }

            LocationFailure.NO_SIGNAL -> {
                actionLabel = strings.system.tryAgain
                action = actions.retryLocation
            }
        }
    }

    StatusBanner(
        message = message,
        tone = if (failed != null) BannerTone.Warning else BannerTone.Neutral,
        icon = if (failed != null) Icons.Outlined.Warning else Icons.Outlined.Info,
        modifier = modifier,
        action = if (actionLabel != null && action != null) {
            {
                OutlinedButton(
                    onClick = action,
                    modifier = Modifier.heightIn(
                        min = MaterialTheme.layoutMetrics.minTouchTarget
                    )
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            null
        }
    )
}

/**
 * Which location status the banner should speak about.
 *
 * A permission or service problem is more current than the status left by the
 * last fetch, so it wins even when that status says "resolved". A past failure
 * still shows when the system is healthy again; its action becomes "Try again".
 */
fun locationBannerStatus(
    status: LocationStatus?,
    systemAccess: SystemAccessState
): LocationStatus? = when (systemAccess.locationIssue) {
    LocationIssue.PERMISSION_DENIED_CAN_ASK,
    LocationIssue.PERMISSION_DENIED_PERMANENTLY ->
        LocationStatus.Failed(LocationFailure.PERMISSION_NOT_GRANTED)

    LocationIssue.SERVICES_DISABLED ->
        LocationStatus.Failed(LocationFailure.SERVICES_DISABLED)

    null -> status
}

/**
 * Why prayer alerts will not appear, with the one system screen that fixes it.
 *
 * The three causes are deliberately distinct: a runtime permission is granted by
 * a prompt, the app-level switch lives in app notification settings, and a
 * channel switch lives in that channel's settings. Sending every case to the
 * same generic page makes the reader hunt.
 */
@Composable
fun NotificationAccessBanner(
    systemAccess: SystemAccessState,
    alertsEnabled: Boolean,
    actions: SystemAccessActions,
    modifier: Modifier = Modifier
) {
    if (!alertsEnabled) return
    val strings = LocalStrings.current
    val issue = systemAccess.notificationIssue ?: return

    val message = when (issue) {
        NotificationIssue.PERMISSION_DENIED -> strings.system.notificationsPermissionNeeded
        NotificationIssue.APP_BLOCKED -> strings.system.notificationsBlocked
        NotificationIssue.CHANNEL_BLOCKED -> strings.system.prayerChannelBlocked
    }

    val actionLabel: String
    val action: () -> Unit
    when (issue) {
        NotificationIssue.PERMISSION_DENIED -> {
            if (systemAccess.notificationPermission == PermissionAccess.DENIED_PERMANENTLY) {
                actionLabel = strings.system.openSettings
                action = actions.openNotificationSettings
            } else {
                actionLabel = strings.more.allowNotificationsAction
                action = actions.requestNotifications
            }
        }

        NotificationIssue.APP_BLOCKED -> {
            actionLabel = strings.system.openSettings
            action = actions.openNotificationSettings
        }

        NotificationIssue.CHANNEL_BLOCKED -> {
            actionLabel = strings.system.openSettings
            action = actions.openPrayerChannelSettings
        }
    }

    StatusBanner(
        message = message,
        tone = BannerTone.Warning,
        icon = Icons.Outlined.Warning,
        modifier = modifier,
        action = {
            OutlinedButton(
                onClick = action,
                modifier = Modifier.heightIn(
                    min = MaterialTheme.layoutMetrics.minTouchTarget
                )
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * Android 12+ can withhold exact alarms. The app still schedules an inexact
 * alarm, so this is a "late", not "missing", warning.
 */
@Composable
fun ExactAlarmAccessBanner(
    systemAccess: SystemAccessState,
    alertsEnabled: Boolean,
    actions: SystemAccessActions,
    modifier: Modifier = Modifier
) {
    if (!alertsEnabled || systemAccess.exactAlarms != ExactAlarmAccess.DENIED) return
    val strings = LocalStrings.current

    StatusBanner(
        message = strings.system.exactAlarmsDelayed,
        tone = BannerTone.Warning,
        icon = Icons.Outlined.Warning,
        modifier = modifier,
        action = {
            OutlinedButton(
                onClick = actions.requestExactAlarms,
                modifier = Modifier.heightIn(
                    min = MaterialTheme.layoutMetrics.minTouchTarget
                )
            ) {
                Text(
                    text = strings.system.allowExactAlarms,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
