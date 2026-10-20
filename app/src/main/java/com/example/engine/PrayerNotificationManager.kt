package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Prayer
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.UiStrings
import com.example.ui.localization.prayerName

object PrayerNotificationManager {

    /**
     * The reader's own strings.
     */
    private fun strings(context: Context): UiStrings =
        LocalizationManager.getStrings(
            context.applicationContext
                .getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
                .getString("pref_language", "English") ?: "English"
        )

    /**
     * Four channels. Three carry the prayer-time alert in the reader's chosen
     * sound mode; Android channel settings decide sound and vibration *after* a
     * notification is posted, and changing one channel's sound later would
     * silently rewrite what the reader chose.
     *
     * The fourth is the pre-prayer reminder, deliberately low-importance and
     * silent. It is advance information, not the Adhan, so it must not borrow
     * the urgency of the prayer-time channel.
     */
    const val CHANNEL_PRAYER_SOUND = "salah_prayer_sound_v2"
    const val CHANNEL_PRAYER_VIBRATE = "salah_prayer_vibrate_v2"
    const val CHANNEL_PRAYER_SILENT = "salah_prayer_silent_v2"
    const val CHANNEL_PRE_PRAYER = "salah_pre_prayer_v2"

    /** The old single channel; deleted once so its settings cannot linger. */
    private const val CHANNEL_LEGACY = "salah_prayer_channel"

    private const val TEST_NOTIFICATION_ID = 4300

    /**
     * A carried prayer time, as the reader should see it.
     */
    fun formatPrayerTime(context: Context, prayerTime: String): String {
        val is24h = context
            .getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .getBoolean("pref_time_format_24h", true)
        val pattern = if (is24h) "HH:mm" else "h:mm a"
        return runCatching {
            java.time.LocalTime.parse(prayerTime).format(
                java.time.format.DateTimeFormatter.ofPattern(pattern)
            )
        }.getOrDefault(prayerTime)
    }

    /**
     * The channel the next alert should use. Silent wins when both choices are
     * on: "silent" is the stronger instruction.
     */
    fun channelIdFor(isGlobalSilent: Boolean, vibrateOnly: Boolean): String = when {
        isGlobalSilent -> CHANNEL_PRAYER_SILENT
        vibrateOnly -> CHANNEL_PRAYER_VIBRATE
        else -> CHANNEL_PRAYER_SOUND
    }

    /** Reads the two preferences that choose the next channel. */
    fun currentChannelId(context: Context): String {
        val prefs = context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
        return channelIdFor(
            isGlobalSilent = prefs.getBoolean("pref_global_silent", false),
            vibrateOnly = prefs.getBoolean("pref_vibrate_only", false)
        )
    }

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val strings = strings(context).more
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // If the reader had blocked the old single channel, carrying that block
        // into the three replacements is the only non-surprising migration: the
        // app must not start making noise because it renamed its channel.
        val legacyBlocked =
            notificationManager.getNotificationChannel(CHANNEL_LEGACY)?.importance ==
                NotificationManager.IMPORTANCE_NONE
        notificationManager.deleteNotificationChannel(CHANNEL_LEGACY)
        val alertImportance = if (legacyBlocked) {
            NotificationManager.IMPORTANCE_NONE
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
        val quietImportance = if (legacyBlocked) {
            NotificationManager.IMPORTANCE_NONE
        } else {
            NotificationManager.IMPORTANCE_LOW
        }

        // Standard system channel, default notification sound - no custom adhan.
        val soundChannel = NotificationChannel(
            CHANNEL_PRAYER_SOUND,
            strings.notifications.notifChannelAdhan,
            alertImportance
        ).apply {
            description = strings.notifications.notifChannelAdhanDescription
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
        }

        val vibrateChannel = NotificationChannel(
            CHANNEL_PRAYER_VIBRATE,
            strings.vibrateOnlyLabel,
            alertImportance
        ).apply {
            // The name is already "Vibrate only"; repeating it is honest, unlike
            // borrowing the silent channel's description here.
            description = strings.vibrateOnlyLabel
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
        }

        val silentChannel = NotificationChannel(
            CHANNEL_PRAYER_SILENT,
            strings.notifications.notifChannelSilent,
            quietImportance
        ).apply {
            description = strings.notifications.notifChannelSilentDescription
            setSound(null, null)
            enableVibration(false)
        }

        // A gentle reminder ten minutes before the Adhan should not make the
        // same sound or vibration as the Adhan itself. Low importance keeps it
        // in the shade without interrupting; the reader can still raise it in
        // Android's per-channel settings.
        val prePrayerChannel = NotificationChannel(
            CHANNEL_PRE_PRAYER,
            strings.notifications.notifChannelPrePrayer,
            quietImportance
        ).apply {
            description = strings.notifications.notifChannelPrePrayerDescription
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannels(
            listOf(soundChannel, vibrateChannel, silentChannel, prePrayerChannel)
        )
    }

    /**
     * Shows a prayer-time notification on the channel the reader chose.
     * No custom adhan sound, no action buttons beyond opening the app.
     */
    fun showPrayerNotification(
        context: Context,
        prayer: Prayer,
        prayerTime: String,
        isGlobalSilent: Boolean = false,
        vibrateOnly: Boolean = false
    ) {
        initChannels(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Shown, not carried: the reader's 12/24-hour choice, read now.
        val shown = formatPrayerTime(context, prayerTime)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_PRAYER", prayer.name)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ui = strings(context)
        val spoken = ui.prayerName(prayer)
        val status = ui.more.notifications

        val statusText = when {
            isGlobalSilent -> status.notifGlobalSilent
            vibrateOnly -> status.notifVibrateAlert.format(spoken)
            else -> status.notifPrayerArrived.format(spoken, shown)
        }

        val builder = NotificationCompat.Builder(
            context,
            channelIdFor(isGlobalSilent, vibrateOnly)
        )
            .setSmallIcon(R.drawable.ic_stat_salah)
            .setContentTitle(spoken)
            .setContentText(statusText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$statusText\n" + status.notifEnterPrayer.format(shown))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        notificationManager.notify(prayer.ordinal + 100, builder.build())
    }

    /**
     * Shows a gentle pre-prayer heads-up notification.
     */
    fun showPrePrayerNotification(
        context: Context,
        prayer: Prayer,
        prayerTime: String,
        offsetMinutes: Int = 10
    ) {
        initChannels(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val shown = formatPrayerTime(context, prayerTime)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ui = strings(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_PRE_PRAYER)
            .setSmallIcon(R.drawable.ic_stat_salah)
            .setContentTitle(
                ui.more.notifications.notifPrePrayerTitle
                    .format(ui.prayerName(prayer), offsetMinutes)
            )
            .setContentText(
                ui.more.notifications.notifPrePrayerText
                    .format(ui.prayerName(prayer), shown)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(prayer.ordinal + 200, builder.build())
    }

    /**
     * A notification the reader can use to check sound, vibration and the
     * channel before trusting the next prayer alert.
     */
    fun showTestNotification(context: Context) {
        initChannels(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ui = strings(context)
        val prefs = context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
        val isGlobalSilent = prefs.getBoolean("pref_global_silent", false)
        val vibrateOnly = prefs.getBoolean("pref_vibrate_only", false)
        val prayer = Prayer.FAJR
        val spoken = ui.prayerName(prayer)
        val shown = formatPrayerTime(context, "05:00")
        val text = ui.more.notifications.notifPrayerArrived.format(spoken, shown)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            channelIdFor(isGlobalSilent, vibrateOnly)
        )
            .setSmallIcon(R.drawable.ic_stat_salah)
            .setContentTitle(ui.system.testNotification)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text\n" + ui.more.notifications.notifEnterPrayer.format(shown))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        notificationManager.notify(TEST_NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context, prayer: Prayer) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(prayer.ordinal + 100)
    }
}
