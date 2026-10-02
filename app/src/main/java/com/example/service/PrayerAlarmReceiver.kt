package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SalahDatabase
import com.example.data.local.PrayerLogEntity
import com.example.data.model.Prayer
import com.example.engine.PrayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * BroadcastReceiver catching scheduled prayer alarms, system reboots, and notification actions.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.salah.ACTION_PRAYER_ALARM"
        const val ACTION_PRE_PRAYER_ALARM = "com.example.salah.ACTION_PRE_PRAYER_ALARM"
        const val ACTION_SILENCE_ACTIVE = "com.example.salah.ACTION_SILENCE_ACTIVE"
        const val ACTION_MARK_PRAYED = "com.example.salah.ACTION_MARK_PRAYED"
        const val ACTION_MIDNIGHT_RESCHEDULE = "com.example.salah.ACTION_MIDNIGHT_RESCHEDULE"

        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_PRAYER_TIME = "EXTRA_PRAYER_TIME"
        const val EXTRA_OFFSET_MINUTES = "EXTRA_OFFSET_MINUTES"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_MIDNIGHT_RESCHEDULE -> {
                // Reschedule all daily prayer alarms for current location
                PrayerAlarmScheduler.scheduleAllPrayers(context)
            }

            ACTION_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                val prayer = try { Prayer.valueOf(prayerName) } catch (_: Exception) { return }

                PrayerAlertService.startAlert(
                    context = context,
                    prayer = prayer,
                    prayerTime = prayerTime,
                    isPrePrayer = false
                )
            }

            ACTION_PRE_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                val offset = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 10)
                val prayer = try { Prayer.valueOf(prayerName) } catch (_: Exception) { return }

                PrayerAlertService.startAlert(
                    context = context,
                    prayer = prayer,
                    prayerTime = prayerTime,
                    isPrePrayer = true,
                    offsetMinutes = offset
                )
            }

            ACTION_SILENCE_ACTIVE -> {
                // Immediately silence adhan audio synthesis
                PrayerAlertService.stopActiveAlert(context)
            }

            ACTION_MARK_PRAYED -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
                val prayer = try { Prayer.valueOf(prayerName) } catch (_: Exception) { return }

                // Dismiss active notification
                PrayerNotificationManager.dismissNotification(context, prayer)
                // Stop any audio alert
                PrayerAlertService.stopActiveAlert(context)

                // Persist prayer completion in local Room DB
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = SalahDatabase.getDatabase(context)
                        val todayStr = LocalDate.now().toString()
                        val currentLog = db.salahDao().getPrayerLogOnce(todayStr) ?: PrayerLogEntity(dateString = todayStr)
                        val updated = when (prayer) {
                            Prayer.FAJR -> currentLog.copy(fajrDone = true)
                            Prayer.DHUHR -> currentLog.copy(dhuhrDone = true)
                            Prayer.ASR -> currentLog.copy(asrDone = true)
                            Prayer.MAGHRIB -> currentLog.copy(maghribDone = true)
                            Prayer.ISHA -> currentLog.copy(ishaDone = true)
                            else -> currentLog
                        }
                        db.salahDao().insertOrUpdatePrayerLog(updated)
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
