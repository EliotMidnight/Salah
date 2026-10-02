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
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.UiStrings
import com.example.ui.localization.prayerName
import com.example.MainActivity
import com.example.data.model.Prayer
import com.example.service.PrayerAlarmReceiver
import kotlin.math.roundToInt

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

    const val CHANNEL_ADHAN = "salah_adhan_channel"
    const val CHANNEL_PRE_PRAYER = "salah_pre_prayer_channel"
    const val CHANNEL_SILENT = "salah_silent_channel"

    /**
     * A bearing in whole degrees, as a reader reads it.
     */
    fun formatBearing(degrees: Float): String = "${degrees.roundToInt()}°"

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

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High priority channel with sound & vibration
            val adhanChannel = NotificationChannel(
                CHANNEL_ADHAN,
                strings(context).more.notifications.notifChannelAdhan,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = strings(context).more.notifications.notifChannelAdhanDescription
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
                strings(context).more.notifications.notifChannelPrePrayer,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = strings(context).more.notifications.notifChannelPrePrayerDescription
                enableVibration(true)
            }

            // Low priority channel for silent mode
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                strings(context).more.notifications.notifChannelSilent,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = strings(context).more.notifications.notifChannelSilentDescription
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
        prayerTime: String,
        alertMode: String = "Full Adhan",
        isGlobalSilent: Boolean = false,
        isVibrateOnly: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Shown, not carried: the reader's 12/24-hour choice, read now.
        val shown = formatPrayerTime(context, prayerTime)

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

        // The prayer's *localised* name, not `prayer.englishName`. The title below
        // still shows both scripts so a reader can recognise either, but the sentence
        // is prose and prose should be in their language.
        val spoken = strings(context).prayerName(prayer)
        val status = strings(context).more.notifications

        val statusText = when {
            isGlobalSilent -> status.notifGlobalSilent
            alertMode.equals("Silent", ignoreCase = true) ->
                status.notifSilentFor.format(spoken)
            isVibrateOnly || alertMode.equals("Vibrate Only", ignoreCase = true) ->
                status.notifVibrateAlert.format(spoken)
            alertMode.equals("Takbeer Only", ignoreCase = true) ->
                status.notifTakbeerAlert.format(spoken)
            alertMode.equals("Gentle Chime", ignoreCase = true) ->
                status.notifChimeAlert.format(spoken)
            else -> status.notifPrayerArrived.format(spoken, shown)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${prayer.englishName} · ${prayer.arabicName}")
            .setContentText(statusText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$statusText\n" +
                            status.notifEnterPrayer.format(shown)
                    )
            )
            .setPriority(if (isSilent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                status.notifSilenceAction,
                silencePendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                status.notifMarkPrayed,
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
        prayerTime: String,
        offsetMinutes: Int = 10,
        isGlobalSilent: Boolean = false
    ) {
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

        val channelId = if (isGlobalSilent) CHANNEL_SILENT else CHANNEL_PRE_PRAYER

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(
                strings(context).more.notifications.notifPrePrayerTitle
                    .format(strings(context).prayerName(prayer), offsetMinutes)
            )
            .setContentText(
                strings(context).more.notifications.notifPrePrayerText
                    .format(strings(context).prayerName(prayer), shown)
            )
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
