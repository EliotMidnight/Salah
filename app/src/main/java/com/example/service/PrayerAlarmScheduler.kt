package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.SalahDatabase
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.PrayerCalculationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Robust AlarmManager scheduler for prayer time reminders, customizable adhan alarms,
 * and midnight daily replenishment.
 */
object PrayerAlarmScheduler {

    /**
     * Schedules alarms for all prayers today and tomorrow, plus pre-prayer alerts and midnight refresh.
     */
    fun scheduleAllPrayers(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch
                val prefs = appContext.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)

                // Check notifications enabled
                val adhanEnabled = prefs.getBoolean("pref_adhan_notif", true)
                val prePrayerEnabled = prefs.getBoolean("pref_pre_prayer", true)
                val prePrayerOffset = prefs.getInt("pref_pre_prayer_offset", 10)
                // The *carried* form, and deliberately not the displayed one.
                //
                // This used to format for display at arm time, so a reader who
                // switched between 12 and 24 hour in Settings saw the app change
                // instantly and the notification keep the old format until something
                // else re-armed the alarms - a location change, a reboot, midnight.
                // "Fajr - begins at 5:12 PM" beside an app reading 17:12.
                //
                // So the alarm carries a wall clock and the notification decides how
                // to show it, from the preference as it is when the notification is
                // built. That also means switching the format does not have to
                // re-arm anything, which is why `setTimeFormat24h` does not.
                val timeFormatter = DateTimeFormatter.ofPattern(
                    PRAYER_TIME_FORMAT,
                    java.util.Locale.ROOT
                )

                // Resolve location (cached Room DB or SharedPrefs fallback)
                val db = SalahDatabase.getDatabase(appContext)
                val cachedLoc = db.salahDao().getCachedLocationOnce()
                val location = if (cachedLoc != null) {
                    UserLocation(
                        name = cachedLoc.name,
                        country = cachedLoc.country,
                        latitude = cachedLoc.latitude,
                        longitude = cachedLoc.longitude,
                        isGps = cachedLoc.isGps
                    )
                } else {
                    val lat = (if (prefs.contains("loc_lat")) prefs.getFloat("loc_lat", UserLocation.DEFAULT.latitude.toFloat())
                               else prefs.getFloat("pref_loc_lat", UserLocation.DEFAULT.latitude.toFloat())).toDouble()
                    val lon = (if (prefs.contains("loc_lng")) prefs.getFloat("loc_lng", UserLocation.DEFAULT.longitude.toFloat())
                               else prefs.getFloat("pref_loc_lon", UserLocation.DEFAULT.longitude.toFloat())).toDouble()
                    val name = if (prefs.contains("loc_name")) prefs.getString("loc_name", UserLocation.DEFAULT.name) ?: UserLocation.DEFAULT.name
                               else prefs.getString("pref_loc_name", UserLocation.DEFAULT.name) ?: UserLocation.DEFAULT.name
                    val country = if (prefs.contains("loc_country")) prefs.getString("loc_country", UserLocation.DEFAULT.country) ?: UserLocation.DEFAULT.country
                                  else prefs.getString("pref_loc_country", UserLocation.DEFAULT.country) ?: UserLocation.DEFAULT.country
                    UserLocation(name, country, lat, lon)
                }

                // Repository writes keys without the "pref_" prefix; accept both for reliability.
                fun prefOr(key: String, legacy: String, def: String): String =
                    if (prefs.contains(key)) prefs.getString(key, def) ?: def
                    else prefs.getString(legacy, def) ?: def
                fun prefOrInt(key: String, legacy: String, def: Int): Int =
                    if (prefs.contains(key)) prefs.getInt(key, def) else prefs.getInt(legacy, def)

                val methodName = prefOr("calc_method", "pref_calc_method", CalculationMethod.MOROCCO_MINISTRY.name)
                val method = try { CalculationMethod.valueOf(methodName) } catch (_: Exception) { CalculationMethod.MOROCCO_MINISTRY }

                val madhhabName = prefOr("calc_madhhab", "pref_madhhab", Madhhab.STANDARD.name)
                val madhhab = try { Madhhab.valueOf(madhhabName) } catch (_: Exception) { Madhhab.STANDARD }

                val adjustments = PrayerAdjustments(
                    fajr = prefOrInt("adj_fajr", "pref_adj_fajr", 0),
                    sunrise = prefOrInt("adj_sunrise", "pref_adj_sunrise", 0),
                    dhuhr = prefOrInt("adj_dhuhr", "pref_adj_dhuhr", 0),
                    asr = prefOrInt("adj_asr", "pref_adj_asr", 0),
                    maghrib = prefOrInt("adj_maghrib", "pref_adj_maghrib", 0),
                    isha = prefOrInt("adj_isha", "pref_adj_isha", 0)
                )

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val zoneId = ZoneId.systemDefault()
                val nowEpochMs = System.currentTimeMillis()

                // Process both Today and Tomorrow to ensure seamless boundary coverage
                listOf(today, tomorrow).forEach { date ->
                    val dayPrayerTimes = PrayerCalculationEngine.calculatePrayerTimes(
                        date = date,
                        location = location,
                        method = method,
                        madhhab = madhhab,
                        adjustments = adjustments
                    )

                    dayPrayerTimes.prayers.forEach { pt ->
                        val targetZoned = pt.dateTime.atZone(zoneId)
                        val targetEpochMs = targetZoned.toInstant().toEpochMilli()
                        val timeStr = pt.time.format(timeFormatter)

                        // 1. Primary Prayer Adhan Alarm
                        if (adhanEnabled && targetEpochMs > nowEpochMs) {
                            scheduleAlarmExact(
                                context = appContext,
                                alarmManager = alarmManager,
                                triggerEpochMs = targetEpochMs,
                                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM,
                                prayer = pt.prayer,
                                prayerTime = timeStr,
                                requestCode = getRequestCode(date, pt.prayer, isPrePrayer = false)
                            )
                        }

                        // 2. Pre-Prayer Reminder Alarm (e.g., 10 minutes prior)
                        if (prePrayerEnabled && pt.prayer != Prayer.SUNRISE) {
                            val prePrayerEpochMs = targetEpochMs - (prePrayerOffset * 60 * 1000L)
                            if (prePrayerEpochMs > nowEpochMs) {
                                scheduleAlarmExact(
                                    context = appContext,
                                    alarmManager = alarmManager,
                                    triggerEpochMs = prePrayerEpochMs,
                                    action = PrayerAlarmReceiver.ACTION_PRE_PRAYER_ALARM,
                                    prayer = pt.prayer,
                                    prayerTime = timeStr,
                                    offsetMinutes = prePrayerOffset,
                                    requestCode = getRequestCode(date, pt.prayer, isPrePrayer = true)
                                )
                            }
                        }
                    }
                }

                // 3. Schedule Daily Midnight Replenishment Alarm
                scheduleMidnightReplenishment(appContext, alarmManager)
            } catch (_: Exception) {
            }
        }
    }

    private fun scheduleAlarmExact(
        context: Context,
        alarmManager: AlarmManager,
        triggerEpochMs: Long,
        action: String,
        prayer: Prayer,
        prayerTime: String,
        offsetMinutes: Int = 10,
        requestCode: Int
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerTime)
            putExtra(PrayerAlarmReceiver.EXTRA_OFFSET_MINUTES, offsetMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            // Fallback for security exception
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    private fun scheduleMidnightReplenishment(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_MIDNIGHT_RESCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule next midnight at 00:02
        val nextMidnight = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 2))
        val midnightEpochMs = nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    midnightEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, midnightEpochMs, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancelAllAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            listOf(today, tomorrow).forEach { date ->
                Prayer.entries.forEach { prayer ->
                    cancelSingleAlarm(context, alarmManager, getRequestCode(date, prayer, false))
                    cancelSingleAlarm(context, alarmManager, getRequestCode(date, prayer, true))
                }
            }
            cancelSingleAlarm(context, alarmManager, 9999)
        } catch (_: Exception) {}
    }

    private fun cancelSingleAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getRequestCode(date: LocalDate, prayer: Prayer, isPrePrayer: Boolean): Int {
        val dayIndex = (date.dayOfYear % 100) * 10
        val prayerIndex = prayer.ordinal
        val prePrayerOffset = if (isPrePrayer) 5000 else 0
        return dayIndex + prayerIndex + prePrayerOffset
    }
}

/**
 * The wall-clock form a prayer time takes when it is carried in an alarm's `Intent`.
 *
 * 24-hour and locale-independent, so the string is a fact about the clock rather than
 * about the device's locale: an Arabic or Urdu device with 12-hour switched on would
 * otherwise put Eastern digits in here that [PrayerNotificationManager] then has to
 * parse back. [PrayerNotificationManager.formatPrayerTime] is the only thing that
 * turns this into something a reader reads.
 */
internal const val PRAYER_TIME_FORMAT = "HH:mm"
