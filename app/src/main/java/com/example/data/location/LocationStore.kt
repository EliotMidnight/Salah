package com.example.data.location

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserLocation

/**
 * Where the reader's location is kept.
 *
 * ### Why this exists rather than being inline in two places
 *
 * The location was stored **twice** - in `SharedPreferences` and in a Room table - and
 * the two consumers each read a *different* authority:
 *
 * - `SalahRepository` read only the preferences, and is what the whole UI saw;
 * - `PrayerAlarmScheduler` read the Room row **first** and fell back to the
 *   preferences, and is what the alarms were computed from.
 *
 * So the screen could show one place and the adhan could fire for another, and the two
 * only agreed for as long as both writes always succeeded - the preferences were
 * written with `.apply()` and the Room insert ran in a bare `CoroutineScope` with its
 * exception swallowed, so a failure was silent and unreported. The scheduler also
 * carried a *third* set of key names (`pref_loc_lat` and friends) for an older build,
 * which is how a fourth spelling of the same fact got into the tree.
 *
 * ### Why the preferences, and not the Room table
 *
 * Three reasons, and the first is decisive:
 *
 * 1. `loadLocation()` is called in a field initialiser. A Room read is `suspend`, so
 *    making Room authoritative would force a loading state on every launch to read one
 *    row of latitude, on a screen that has a loading state for the Quran already.
 * 2. The database is opened with `fallbackToDestructiveMigration`, so a **schema
 *    change can wipe it**. A location that the adhan depends on should not be in a
 *    store that a Room version bump can destroy, and a book of bookmarks should not
 *    have to survive in the same one.
 * 3. `PrayerAlarmScheduler` is an `object` given a `Context`, with no repository and no
 *    coroutine. Reading four preferences is trivial there; opening a database and
 *    running a suspending query is not.
 *
 * ### What is still in Room, and why
 *
 * The `cached_locations` table is now written by nobody and read by nobody. It is
 * still *declared*, because removing an entity from `@Database` changes the expected
 * identity hash and therefore needs a version bump - and with the app's
 * `fallbackToDestructiveMigration(dropAllTables = true)` a bump that is not paired
 * with a hand-written migration **destroys the reader's bookmarks and their
 * Continue Reading position**. Losing those to empty a table nobody reads is a bad
 * trade, so the table stays until a real migration exists. Said here rather than
 * discovered later.
 */
object LocationStore {

    private const val KEY_NAME = "loc_name"
    private const val KEY_COUNTRY = "loc_country"
    private const val KEY_LAT = "loc_lat"
    private const val KEY_LNG = "loc_lng"
    private const val KEY_IS_GPS = "loc_is_gps"

    /**
     * Legacy key names, from a build that prefixed every preference.
     *
     * Read once, on the way out, and never written. The scheduler used to probe both
     * spellings on every call, so which one won depended on which a given install
     * happened to have - a fourth answer to "where is the reader".
     */
    private val LEGACY = mapOf(
        KEY_NAME to "pref_loc_name",
        KEY_COUNTRY to "pref_loc_country",
        KEY_LAT to "pref_loc_lat",
        KEY_LNG to "pref_loc_lon",
        KEY_IS_GPS to "pref_loc_is_gps"
    )

    /** The preferences file. One name, used by everything that already did. */
    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)

    /**
     * The stored location, or the default.
     *
     * The default is [UserLocation.DEFAULT], so a first run and a lost preference both
     * answer the same way rather than one throwing.
     */
    fun read(context: Context): UserLocation {
        val p = prefs(context)

        /**
         * The modern key if this install has it, the legacy one otherwise.
         *
         * Plain, not generic. A generic `pick` inferred its type parameter from the
         * *reader* lambda and then demanded a fallback of a different type, so the two
         * had to be written to fit each other - the same coupling this object exists
         * to remove, just moved somewhere smaller.
         */
        fun keyFor(key: String): String = when {
            p.contains(key) -> key
            p.contains(LEGACY.getValue(key)) -> LEGACY.getValue(key)
            else -> key
        }

        fun string(key: String, fallback: String): String =
            p.getString(keyFor(key), fallback) ?: fallback

        // Read as Float and widened, because that is what a preference holds. The
        // default goes through the same conversion, so a reader who has never chosen
        // a location gets the default rather than a Float round-trip of it.
        fun coordinate(key: String, fallback: Double): Double =
            p.getFloat(keyFor(key), fallback.toFloat()).toDouble()

        return UserLocation(
            name = string(KEY_NAME, UserLocation.DEFAULT.name),
            country = string(KEY_COUNTRY, UserLocation.DEFAULT.country),
            latitude = coordinate(KEY_LAT, UserLocation.DEFAULT.latitude),
            longitude = coordinate(KEY_LNG, UserLocation.DEFAULT.longitude),
            isGps = p.getBoolean(keyFor(KEY_IS_GPS), false)
        )
    }

    /**
     * Stores [location], and returns the values actually written.
     *
     * Returns what was written rather than what was passed, because a legacy key that
     * is still present would otherwise be read back and *win* - the two spellings
     * disagreeing after the caller believed it had set the value. Writing the modern
     * keys and reading them first fixes that, and `read` prefers them.
     *
     * The legacy keys are removed on write. They are only read on the way in, and a
     * reader who has moved on should not keep a stale second answer around to
     * resurrect later.
     */
    fun write(context: Context, location: UserLocation): SharedPreferences.Editor {
        val editor = prefs(context).edit()
            .putString(KEY_NAME, location.name)
            .putString(KEY_COUNTRY, location.country)
            .putFloat(KEY_LAT, location.latitude.toFloat())
            .putFloat(KEY_LNG, location.longitude.toFloat())
            .putBoolean(KEY_IS_GPS, location.isGps)
        LEGACY.values.forEach { editor.remove(it) }
        return editor
    }
}
