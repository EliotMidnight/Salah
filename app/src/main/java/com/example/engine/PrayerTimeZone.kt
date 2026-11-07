package com.example.engine

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Which legal clock a day's prayer times are written in.
 *
 * The answer used to be a country-name table inside the calculation engine
 * (`"Morocco"` → +1, `"Saudi"` → +3, `"Egypt"` → +3, plus dead `"UK"`/`"USA"`
 * branches that could never match `United Kingdom`/`United States`). Every row
 * of that table was a guess that the device's own time zone already knows
 * better, and two of them were wrong in a way that shifted prayer times by a
 * full hour: Morocco has not been on UTC+1 since 20 September 2026, and Egypt
 * is on UTC+2 every winter. The table is gone; the device zone decides.
 *
 * ### The one override, and why it exists
 *
 * Morocco's legal time became GMT (UTC+0) on 20 September 2026 (decree
 * 2.26.530). `Africa/Casablanca` in tzdata agrees, but tzdata on a phone is only
 * as new as the phone's last system update, and a device still carrying the
 * pre-decree rule reports **UTC+1** - which is exactly the one-hour error this
 * change set out to fix. The ministry's own website works around the same
 * problem the same way, by printing `GMT` unconditionally.
 *
 * So for the two Moroccan zone ids, and only from the decree onwards, the
 * offset is pinned to UTC. Before that date the zone's own rules are correct
 * and are left alone (they encode the older UTC+1 / Ramadan UTC+0 alternation).
 *
 * This is a *narrow* exception on purpose: a device on any other zone is
 * trusted as-is. A reader in Rabat whose phone is set to, say, `Europe/London`
 * sees London wall-clock times, because that is the clock in their status bar
 * and the one they will compare against. Times are reported in the clock the
 * reader is actually looking at.
 *
 * When the official ministry table is available it states its own `fuseau` and
 * supersedes this entirely - see `HabousTimetable`.
 */
object PrayerTimeZone {

    /** Zone ids that mean "the reader keeps Moroccan legal time". */
    private val MOROCCAN_ZONE_IDS = setOf("Africa/Casablanca", "Africa/El_Aaiun")

    /** First day of Morocco's permanent GMT, per decree 2.26.530. */
    val MOROCCO_GMT_SINCE: LocalDate = LocalDate.of(2026, 9, 20)

    /**
     * The zone to write [date]'s times in.
     *
     * @param deviceZone the device's own zone, passed in rather than read here
     *   so that a caller - and a test - can state it.
     */
    fun forDay(date: LocalDate, deviceZone: ZoneId): ZoneId =
        if (deviceZone.id in MOROCCAN_ZONE_IDS && !date.isBefore(MOROCCO_GMT_SINCE)) {
            ZoneOffset.UTC
        } else {
            deviceZone
        }

    /**
     * "Now" on the clock the day's times are written in.
     *
     * A countdown compares two wall-clock readings ("12:16" and "now"), so both
     * must come from the same zone. Reading "now" from the device while the day
     * was written in the pinned UTC clock made the app treat Dhuhr as already
     * passed an hour before it was - on exactly the phones with stale tzdata
     * that the pin exists for.
     */
    fun localNow(deviceZone: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        val clock = forDay(LocalDate.now(deviceZone), deviceZone)
        return LocalDateTime.now(clock)
    }
}
