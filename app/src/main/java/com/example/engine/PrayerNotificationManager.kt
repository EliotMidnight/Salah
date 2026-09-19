package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.Prayer
import com.example.service.PrayerAlarmReceiver

object PrayerNotificationManager {

    const val CHANNEL_ADHAN = "salah_adhan_channel"
    const val CHANNEL_PRE_PRAYER = "salah_pre_prayer_channel"
    const val CHANNEL_SILENT = "salah_silent_channel"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High priority channel with sound & vibration
            val adhanChannel = NotificationChannel(
                CHANNEL_ADHAN,
                "Adhan & Prayer Call Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when prayer time arrives with sound or adhan tone"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 250, 400, 250, 600)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            // Default priority channel for pre-prayer heads-up
            val preChannel = NotificationChannel(
                CHANNEL_PRE_PRAYER,
                "Pre-Prayer Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle heads-up before upcoming prayer"
                enableVibration(true)
            }

            // Low priority channel for silent mode
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "Silent Prayer Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Discreet notifications when silent mode or mute is active"
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(adhanChannel)
            notificationManager.createNotificationChannel(preChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    /**
     * Shows a rich Adhan notification with customizable alert mode and action buttons.
     */
    fun showAdhanNotification(
        context: Context,
        prayer: Prayer,
        timeFormatted: String,
        alertMode: String = "Full Adhan",
        isGlobalSilent: Boolean = false,
        isVibrateOnly: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent: open app
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

        // Action: Silence Adhan
        val silenceIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_SILENCE_ACTIVE
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
        }
        val silencePendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal + 1000,
            silenceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Prayed
        val markPrayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_MARK_PRAYED
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
        }
        val markPrayedPendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal + 2000,
            markPrayedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isSilent = isGlobalSilent || alertMode.equals("Silent", ignoreCase = true)
        val channelId = if (isSilent) CHANNEL_SILENT else CHANNEL_ADHAN

        val statusText = when {
            isGlobalSilent -> "Silent Mode active · Adhan muted"
            alertMode.equals("Silent", ignoreCase = true) -> "Silent Mode active for ${prayer.englishName}"
            isVibrateOnly || alertMode.equals("Vibrate Only", ignoreCase = true) -> "Vibrate alert · ${prayer.englishName} has entered"
            alertMode.equals("Takbeer Only", ignoreCase = true) -> "Takbeer alert · Time for ${prayer.englishName}"
            alertMode.equals("Gentle Chime", ignoreCase = true) -> "Gentle Chime alert · Time for ${prayer.englishName}"
            else -> "Time for ${prayer.englishName} prayer has arrived ($timeFormatted)"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${prayer.englishName} · ${prayer.arabicName}")
            .setContentText(statusText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$statusText\nEnter prayer and turn towards the Holy Kaaba ($timeFormatted).")
            )
            .setPriority(if (isSilent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                "Silence",
                silencePendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Prayed",
                markPrayedPendingIntent
            )

        if (!isSilent && (isVibrateOnly || alertMode.equals("Vibrate Only", ignoreCase = true))) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500))
        }

        notificationManager.notify(prayer.ordinal + 100, builder.build())
    }

    /**
     * Shows a gentle pre-prayer heads-up notification.
     */
    fun showPrePrayerNotification(
        context: Context,
        prayer: Prayer,
        timeFormatted: String,
        offsetMinutes: Int = 10,
        isGlobalSilent: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isGlobalSilent) CHANNEL_SILENT else CHANNEL_PRE_PRAYER

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("${prayer.englishName} in $offsetMinutes minutes")
            .setContentText("${prayer.englishName} begins at $timeFormatted · Prepare for prayer")
            .setPriority(if (isGlobalSilent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(prayer.ordinal + 200, builder.build())
    }

    fun dismissNotification(context: Context, prayer: Prayer) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(prayer.ordinal + 100)
    }
}
