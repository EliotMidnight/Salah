package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.os.PowerManager
import com.example.data.model.Prayer
import com.example.engine.AdhanAudioSynthesizer
import com.example.engine.PrayerNotificationManager
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
        const val EXTRA_TIME_FORMATTED = "EXTRA_TIME_FORMATTED"
        const val EXTRA_IS_PRE_PRAYER = "EXTRA_IS_PRE_PRAYER"
        const val EXTRA_OFFSET_MINS = "EXTRA_OFFSET_MINS"

        fun startAlert(
            context: Context,
            prayer: Prayer,
            timeFormatted: String,
            isPrePrayer: Boolean = false,
            offsetMinutes: Int = 10
        ) {
            val intent = Intent(context, PrayerAlertService::class.java).apply {
                action = ACTION_PLAY_ALERT
                putExtra(EXTRA_PRAYER_NAME, prayer.name)
                putExtra(EXTRA_TIME_FORMATTED, timeFormatted)
                putExtra(EXTRA_IS_PRE_PRAYER, isPrePrayer)
                putExtra(EXTRA_OFFSET_MINS, offsetMinutes)
            }
            try {
                context.startService(intent)
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
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val timeFormatted = intent.getStringExtra(EXTRA_TIME_FORMATTED) ?: ""
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
        val adhanSound = prefs.getString("pref_adhan_sound", "Makkah Al-Mukarramah") ?: "Makkah Al-Mukarramah"
        val adhanVolume = prefs.getFloat("pref_adhan_volume", 0.85f)
        val alertMode = prefs.getString("pref_alert_mode_${prayer.name}", "Full Adhan") ?: "Full Adhan"
        val autoSilentDuringPrayer = prefs.getBoolean("pref_auto_silent_during_prayer", false)
        val autoSilentDuration = prefs.getInt("pref_auto_silent_duration", 20)

        // Acquire WakeLock briefly to guarantee completion while screen is off
        acquireWakeLock()

        if (isPrePrayer) {
            if (prePrayerEnabled) {
                PrayerNotificationManager.showPrePrayerNotification(
                    context = this,
                    prayer = prayer,
                    timeFormatted = timeFormatted,
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
            timeFormatted = timeFormatted,
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
                soundStyle = adhanSound,
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

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }
}
