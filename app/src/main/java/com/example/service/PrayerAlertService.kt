package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.Prayer
import com.example.engine.PrayerNotificationManager
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.UiStringsMore

/**
 * Background service for managing active prayer alerts.
 *
 * No audio synthesis: the notification uses the system default sound, and nothing
 * is played beyond it.
 */
class PrayerAlertService : Service() {

    companion object {
        const val ACTION_PLAY_ALERT = "com.example.salah.service.PLAY_ALERT"
        const val ACTION_STOP_ALERT = "com.example.salah.service.STOP_ALERT"

        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_PRAYER_TIME = "EXTRA_PRAYER_TIME"
        const val EXTRA_IS_PRE_PRAYER = "EXTRA_IS_PRE_PRAYER"
        const val EXTRA_OFFSET_MINS = "EXTRA_OFFSET_MINS"
        const val FOREGROUND_NOTIFICATION_ID = 4100

        fun startAlert(
            context: Context,
            prayer: Prayer,
            prayerTime: String,
            isPrePrayer: Boolean = false,
            offsetMinutes: Int = 10
        ) {
            val intent = Intent(context, PrayerAlertService::class.java).apply {
                action = ACTION_PLAY_ALERT
                putExtra(EXTRA_PRAYER_NAME, prayer.name)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_IS_PRE_PRAYER, isPrePrayer)
                putExtra(EXTRA_OFFSET_MINS, offsetMinutes)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopActiveAlert(context: Context) {
            val intent = Intent(context, PrayerAlertService::class.java).apply {
                action = ACTION_STOP_ALERT
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * The reader's strings, read at the moment a notification is built.
     */
    private val strings: UiStringsMore
        get() = LocalizationManager
            .getStrings(
                getSharedPreferences("salah_prefs", MODE_PRIVATE)
                    .getString("pref_language", "English") ?: "English"
            )
            .more

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALERT -> {
                handleStopAlert()
                stopSelf(startId)
            }
            ACTION_PLAY_ALERT -> {
                handlePlayAlert(intent, startId)
            }
            else -> {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun handlePlayAlert(intent: Intent, startId: Int) {
        // The foreground notification needs its channel before startForeground.
        // On a cold alarm the ViewModel may never have run in this process.
        try {
            PrayerNotificationManager.initChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("PrayerAlertService", "Failed to init notification channels", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    buildForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerAlertService", "Failed to start foreground service", e)
            stopSelf(startId)
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
        if (prayerName == null) {
            stopSelf(startId)
            return
        }
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val isPrePrayer = intent.getBooleanExtra(EXTRA_IS_PRE_PRAYER, false)
        val offsetMins = intent.getIntExtra(EXTRA_OFFSET_MINS, 10)

        val prayer = try {
            Prayer.valueOf(prayerName)
        } catch (_: Exception) {
            stopSelf(startId)
            return
        }

        val prefs = getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
        val isGlobalSilent = prefs.getBoolean("pref_global_silent", false)
        val vibrateOnly = prefs.getBoolean("pref_vibrate_only", false)
        val prePrayerEnabled = prefs.getBoolean("pref_pre_prayer", true)
        val adhanEnabled = prefs.getBoolean("pref_adhan_notif", true)

        if (isPrePrayer) {
            if (prePrayerEnabled) {
                PrayerNotificationManager.showPrePrayerNotification(
                    context = this,
                    prayer = prayer,
                    prayerTime = prayerTime,
                    offsetMinutes = offsetMins
                )
            }
            stopSelf(startId)
            return
        }

        // Active prayer alert
        if (!adhanEnabled) {
            stopSelf(startId)
            return
        }

        // Show standard notification with system default sound - no custom adhan.
        PrayerNotificationManager.showPrayerNotification(
            context = this,
            prayer = prayer,
            prayerTime = prayerTime,
            isGlobalSilent = isGlobalSilent,
            vibrateOnly = vibrateOnly
        )

        stopSelf(startId)
    }

    private fun handleStopAlert() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val openIntent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPi = android.app.PendingIntent.getActivity(
            this, 0, openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        // The foreground notification is bookkeeping, not the alert. It uses the
        // silent channel so it cannot add a second sound before the real prayer
        // notification is posted a moment later.
        return NotificationCompat.Builder(
            this,
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT
        )
            .setSmallIcon(R.drawable.ic_stat_salah)
            .setContentTitle(strings.notifications.notifAdhanInProgress)
            .setContentText(strings.notifications.notifAdhanInProgressBody)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPi)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
