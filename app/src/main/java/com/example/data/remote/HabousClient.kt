package com.example.data.remote

import android.content.Context
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.PrayerCalculationEngine
import com.example.engine.PrayerTimeZone
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches the ministry's current month, narrows it to this place, and stores it.
 *
 * Failure is ordinary, not exceptional: no connectivity, the ministry's server
 * being down, its certificate chain being incomplete (it omits the intermediate,
 * which is why `network_security_config.xml` carries it), a future month not yet
 * published, or a month that simply does not describe this place. Every one of
 * those leaves the calculation in charge.
 */
object HabousClient {

    private const val TIMEOUT_MS = 15_000

    /**
     * @return true if a month was fetched, adopted and stored.
     */
    suspend fun refresh(
        context: Context,
        location: UserLocation,
        method: CalculationMethod,
        madhhab: Madhhab
    ): Boolean = withContext(Dispatchers.IO) {
        // Only the Moroccan convention is published by the Moroccan ministry.
        // A reader who chose another method asked for another convention. The
        // same is true of a Hanafi shadow factor: the published table is
        // standard-Asr, and adopting it would override the reader's choice.
        if (!method.isMoroccoNationalTable) return@withContext false
        if (madhhab != Madhhab.STANDARD) return@withContext false

        // The month to ask for is the reader's legal date, not the device's: on
        // a phone carrying pre-decree tzdata the two can be a day apart.
        val today = PrayerTimeZone.localNow().toLocalDate()
        // Deliberately unadjusted: the ministry publishes a timetable, not this
        // reader's corrections to it. Matching against the corrected times would
        // let a +10 min Fajr adjustment masquerade as "no city is close", and
        // switching online would then drop the very adjustments that caused it.
        val reference = PrayerCalculationEngine.calculatePrayerTimes(
            date = today,
            location = location,
            method = method,
            madhhab = madhhab,
            adjustments = PrayerAdjustments()
        )

        // The published file is generated once per month, so a month already
        // adopted for this place and clock needs no second download. This also
        // keeps repeated location updates from turning into repeated 292 KB
        // fetches.
        if (HabousStore.readFor(context, today, location, reference.zoneId) != null) {
            return@withContext false
        }

        try {
            val payload = download(HabousFeed.url(today.year, today.monthValue))
            val feed = HabousFeed.parse(payload)
            val selected = HabousFeed.select(
                feed = feed,
                date = today,
                reference = reference.prayers.associate { it.prayer to it.time }
            ) ?: return@withContext false
            HabousStore.write(
                context = context,
                month = selected,
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = reference.zoneId
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Salah/1.0 (Android)")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP $code for $url")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
