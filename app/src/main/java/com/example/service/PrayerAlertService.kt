package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.data.model.AdhanSound
import com.example.data.model.Prayer
import com.example.engine.AdhanAudioSynthesizer
import com.example.engine.PrayerNotificationManager
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.UiStringsMore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background service for managing active prayer alerts, audio Adhan playback,
 * vibration patterns, and silent-mode auto-suppression during Salah.
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
            } catch (_: Exception) {
            }
        }

        fun stopActiveAlert(context: Context) {
            val intent = Intent(context, PrayerAlertService::class.java).apply {
                action = ACTION_STOP_ALERT
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                AdhanAudioSynthesizer.stop()
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoSilentRestoreJob: Job? = null

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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    buildForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
            }
        } catch (_: Exception) {
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
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
        val isVibrateOnly = prefs.getBoolean("pref_vibrate_only", false)
        val adhanEnabled = prefs.getBoolean("pref_adhan_notif", true)
        val prePrayerEnabled = prefs.getBoolean("pref_pre_prayer", true)
        // Resolved here, once, so the service and the preview agree on which sound a
        // stored preference means. `fromStored` also accepts the old label, because
        // that is what every installed build has on disk.
        val adhanSound = AdhanSound.fromStored(prefs.getString("pref_adhan_sound", ""))
        val adhanVolume = prefs.getFloat("pref_adhan_volume", 0.85f)
        // [Prayer.defaultAlertMode], not a literal. This used to say "Full Adhan" for
        // every prayer while the Settings screen said Sunrise gets a silent reminder -
        // so a reader who had never opened the per-prayer sheet heard a full adhan at
        // sunrise, against what the app had told them.
        val alertMode = prefs.getString(
            "pref_alert_mode_${prayer.name}",
            prayer.defaultAlertMode
        ) ?: prayer.defaultAlertMode
        val autoSilentDuringPrayer = prefs.getBoolean("pref_auto_silent_during_prayer", false)
        val autoSilentDuration = prefs.getInt("pref_auto_silent_duration", 20)

        try { PrayerNotificationManager.initChannels(this) } catch (_: Exception) {}

        // Acquire WakeLock briefly to guarantee completion while screen is off
        acquireWakeLock()

        if (isPrePrayer) {
            if (prePrayerEnabled) {
                PrayerNotificationManager.showPrePrayerNotification(
                    context = this,
                    prayer = prayer,
                    prayerTime = prayerTime,
                    offsetMinutes = offsetMins,
                    isGlobalSilent = isGlobalSilent
                )
            }
            releaseWakeLock()
            stopSelf(startId)
            return
        }

        // Active prayer alert
        if (!adhanEnabled) {
            releaseWakeLock()
            stopSelf(startId)
            return
        }

        // Show rich notification
        PrayerNotificationManager.showAdhanNotification(
            context = this,
            prayer = prayer,
            prayerTime = prayerTime,
            alertMode = alertMode,
            isGlobalSilent = isGlobalSilent,
            isVibrateOnly = isVibrateOnly
        )

        // Handle auto-silent mode during prayer (Masjid mode)
        if (autoSilentDuringPrayer) {
            applyAutoSilentWindow(autoSilentDuration)
        }

        // Play audio alert if not silent and not vibrate-only
        val shouldPlaySound = !isGlobalSilent && !isVibrateOnly && !alertMode.equals("Silent", ignoreCase = true)

        if (shouldPlaySound) {
            AdhanAudioSynthesizer.playAlert(
                alertMode = alertMode,
                sound = adhanSound,
                volume = adhanVolume,
                onCompletion = {
                    releaseWakeLock()
                    stopSelf(startId)
                }
            )
        } else {
            releaseWakeLock()
            stopSelf(startId)
        }
    }

    private fun handleStopAlert() {
        AdhanAudioSynthesizer.stop()
        releaseWakeLock()
    }

    private fun applyAutoSilentWindow(durationMinutes: Int) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val originalRingerMode = audioManager.ringerMode
            // Set to vibrate mode during prayer if normal
            if (originalRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                } catch (_: Exception) {
                }
            }

            autoSilentRestoreJob?.cancel()
            autoSilentRestoreJob = serviceScope.launch {
                delay(durationMinutes * 60 * 1000L)
                try {
                    audioManager.ringerMode = originalRingerMode
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Salah:PrayerAlertWakeLock"
            )?.apply {
                acquire(45 * 1000L) // 45 seconds max safety timeout
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val openIntent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPi = android.app.PendingIntent.getActivity(
            this, 0, openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PrayerNotificationManager.CHANNEL_SILENT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            // The ongoing notification for the alert itself. English until this stage:
            // it is the one notification a reader cannot dismiss and so cannot avoid
            // reading, in every prayer, in whatever language they chose.
            .setContentTitle(strings.notifications.notifAdhanInProgress)
            .setContentText(strings.notifications.notifAdhanInProgressBody)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPi)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }
}
