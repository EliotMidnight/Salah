package com.example.ui.system

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Whether a runtime permission is available, and whether the app may ask again.
 *
 * "Permanently denied" is the app's own bookkeeping: the system only reports
 * `shouldShowRequestPermissionRationale == false` both before the first ask and
 * after "don't ask again", and those two states need different actions.
 */
enum class PermissionAccess {
    GRANTED,
    CAN_ASK,
    DENIED_PERMANENTLY,
    NOT_REQUIRED
}

/** Which system setting is stopping prayer alerts. */
enum class NotificationIssue {
    PERMISSION_DENIED,
    APP_BLOCKED,
    CHANNEL_BLOCKED
}

/** Which part of location access is missing. */
enum class LocationIssue {
    PERMISSION_DENIED_CAN_ASK,
    PERMISSION_DENIED_PERMANENTLY,
    SERVICES_DISABLED
}

/** Exact alarms are a separate, per-app grant on Android 12+. */
enum class ExactAlarmAccess {
    ALLOWED,
    DENIED,
    NOT_REQUIRED
}

/**
 * The three system facts the UI needs, read together so a screen cannot show
 * the permission as granted while the service or channel beside it is off.
 */
data class SystemAccessState(
    val locationPermission: PermissionAccess = PermissionAccess.GRANTED,
    val locationServicesEnabled: Boolean = true,
    val notificationPermission: PermissionAccess = PermissionAccess.GRANTED,
    val appNotificationsEnabled: Boolean = true,
    val prayerChannelEnabled: Boolean = true,
    val exactAlarms: ExactAlarmAccess = ExactAlarmAccess.ALLOWED
) {
    val notificationIssue: NotificationIssue?
        get() = when {
            notificationPermission == PermissionAccess.CAN_ASK ||
                notificationPermission == PermissionAccess.DENIED_PERMANENTLY ->
                NotificationIssue.PERMISSION_DENIED

            !appNotificationsEnabled -> NotificationIssue.APP_BLOCKED
            !prayerChannelEnabled -> NotificationIssue.CHANNEL_BLOCKED
            else -> null
        }

    val notificationsReady: Boolean get() = notificationIssue == null

    val locationIssue: LocationIssue?
        get() = when {
            locationPermission == PermissionAccess.CAN_ASK ->
                LocationIssue.PERMISSION_DENIED_CAN_ASK

            locationPermission == PermissionAccess.DENIED_PERMANENTLY ->
                LocationIssue.PERMISSION_DENIED_PERMANENTLY

            !locationServicesEnabled -> LocationIssue.SERVICES_DISABLED
            else -> null
        }
}

/**
 * The actions a banner can offer. Keeping them together means a screen can be
 * rendered with no system wiring (previews, screenshots) without a dozen
 * defaults on its own signature.
 */
data class SystemAccessActions(
    val requestLocation: () -> Unit = {},
    val openLocationSettings: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val retryLocation: () -> Unit = {},
    val requestNotifications: () -> Unit = {},
    val openNotificationSettings: () -> Unit = {},
    val openPrayerChannelSettings: () -> Unit = {},
    val requestExactAlarms: () -> Unit = {},
    val sendTestNotification: () -> Unit = {}
)

/**
 * Per-app permission bookkeeping. It is deliberately separate from
 * `salah_prefs`: resetting the reader's settings must not erase the fact that
 * Android has already shown its notification prompt, which is what
 * distinguishes "never asked" from "asked and declined".
 */
object SystemPermissionPrefs {
    private const val PREFS = "salah_permission_state"
    private const val KEY_PROMPTED_NOTIFICATIONS = "prompted_post_notifications"
    private const val KEY_ASKED_LOCATION = "asked_location"

    /**
     * Whether the *system* prompt has actually been launched. Only this can
     * distinguish "we never asked Android yet" from "the reader said no and
     * should be sent to settings".
     */
    fun wasNotificationPrompted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROMPTED_NOTIFICATIONS, false)

    fun markNotificationPrompted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_PROMPTED_NOTIFICATIONS, true) }
    }

    fun wasLocationAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED_LOCATION, false)

    fun markLocationAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ASKED_LOCATION, true) }
    }
}

/**
 * Reads the current state and refreshes it whenever the activity resumes.
 *
 * `channelId` is the channel the next alert would use, not a fixed one: the
 * reader's Silent mode / Vibrate-only choices change which channel matters, and
 * checking a channel the app is not going to post to would produce a warning
 * that does not describe the next notification.
 *
 * `revision` exists because a runtime-permission dialog does not reliably take
 * the activity through ON_PAUSE/ON_RESUME. The caller bumps it when a permission
 * result arrives, so the banner cannot keep describing a permission the reader
 * has just granted.
 */
@Composable
fun rememberSystemAccessState(
    channelId: String,
    revision: Int = 0
): SystemAccessState {
    val context = LocalContext.current
    var state by remember(channelId, revision) {
        mutableStateOf(readSystemAccess(context, channelId))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state = readSystemAccess(context, channelId)
    }
    return state
}

fun readSystemAccess(context: Context, channelId: String): SystemAccessState {
    val activity = context.findActivity()
    return SystemAccessState(
        locationPermission = readLocationPermission(context, activity),
        locationServicesEnabled = isLocationEnabled(context),
        notificationPermission = readNotificationPermission(context, activity),
        appNotificationsEnabled = NotificationManagerCompat.from(context)
            .areNotificationsEnabled(),
        prayerChannelEnabled = isNotificationChannelEnabled(context, channelId),
        exactAlarms = readExactAlarmAccess(context)
    )
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

fun isLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        LocationManagerCompat.isLocationEnabled(manager)
    } else {
        @Suppress("DEPRECATION")
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        ?: return false
    return manager.canScheduleExactAlarms()
}

private fun readLocationPermission(
    context: Context,
    activity: Activity?
): PermissionAccess {
    if (hasLocationPermission(context)) return PermissionAccess.GRANTED
    val rationale = activity != null && (
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        )
    return when {
        rationale -> PermissionAccess.CAN_ASK
        SystemPermissionPrefs.wasLocationAsked(context) -> PermissionAccess.DENIED_PERMANENTLY
        else -> PermissionAccess.CAN_ASK
    }
}

private fun readNotificationPermission(
    context: Context,
    activity: Activity?
): PermissionAccess {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return PermissionAccess.NOT_REQUIRED
    }
    if (hasNotificationPermission(context)) return PermissionAccess.GRANTED
    val rationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.POST_NOTIFICATIONS
    )
    return when {
        rationale -> PermissionAccess.CAN_ASK
        SystemPermissionPrefs.wasNotificationPrompted(context) ->
            PermissionAccess.DENIED_PERMANENTLY

        else -> PermissionAccess.CAN_ASK
    }
}

private fun isNotificationChannelEnabled(context: Context, channelId: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return true
    // A channel that has not been created yet is not blocked; initChannels is the
    // only creator, and it runs before the first notification.
    val channel = manager.getNotificationChannel(channelId) ?: return true
    return channel.importance != NotificationManager.IMPORTANCE_NONE
}

private fun readExactAlarmAccess(context: Context): ExactAlarmAccess {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return ExactAlarmAccess.NOT_REQUIRED
    return if (canScheduleExactAlarms(context)) {
        ExactAlarmAccess.ALLOWED
    } else {
        ExactAlarmAccess.DENIED
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
