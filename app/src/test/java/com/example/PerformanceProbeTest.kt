package com.example

import com.example.data.model.CalculationMethod
import com.example.data.model.Prayer
import com.example.data.model.PrayerTime
import com.example.data.model.PrayerTimesDay
import com.example.data.model.UserLocation
import com.example.data.quran.QuranCorpus
import com.example.data.quran.QuranSearch
import com.example.data.quran.QuranText
import com.example.engine.PrayerCalculationEngine
import com.example.engine.QiblaEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Where the app's two hot paths - the corpus read and verse search - and the
 * one-second tick spend their time, **printed rather than asserted**.
 *
 * ### What this is for
 *
 * Both hot paths are lazy in the process, so their cost lands wherever they are
 * first touched. On the main thread that is a dropped frame; here it is a
 * number. The assertions are only the counts (the corpus is 6,236 verses, the
 * searches match something), so this can never fail for being slow - a probe
 * that failed on a busy machine would be a flaky test, not a measurement.
 *
 * ### Running it
 *
 * The corpus, the folds and the lowercased English are `lazy` singletons, so
 * the "first access" numbers are only first if **this class runs first** in a
 * fresh JVM:
 *
 * ```
 * ./tools/build.sh test --tests 'com.example.PerformanceProbeTest'
 * ```
 *
 * In a full-suite run the same lines still print, but earlier test classes have
 * already paid for the load. That is why the labels say what they measure
 * rather than claiming cold or warm.
 *
 * ### The tick
 *
 * `updateLiveTiming` recomputes the next/previous prayer, the countdown and the
 * sun position every second. It has no return value, so the probe drives the
 * same four calls a tick makes, to price one second of it.
 */
class PerformanceProbeTest {

    private inline fun <T> timed(label: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val millis = (System.nanoTime() - start) / 1_000_000
        println("[perf] $label=${millis}ms")
        return result
    }

    @Test
    fun `probe - corpus, folds and search`() {
        val ayahs = timed("corpus_first_access") { QuranCorpus.ayahs }
        assertEquals(6236, ayahs.size)

        val folds = timed("folds_first_access") { QuranText.arabicFolds }
        assertEquals(6236, folds.size)

        val english = timed("english_first_access") { QuranText.lowerEnglish }
        assertEquals(6236, english.size)

        // A narrow query and the worst case: "the" matches thousands of verses,
        // so both the ranking and the highlight work are at their largest.
        val narrow = timed("search_mercy_first") { QuranSearch.searchVerses("mercy") }
        val narrowAgain = timed("search_mercy_again") { QuranSearch.searchVerses("mercy") }
        assertEquals(narrow.total, narrowAgain.total)
        assertTrue("mercy should match verses", narrow.total > 0)

        val broad = timed("search_the_first") { QuranSearch.searchVerses("the") }
        val broadAgain = timed("search_the_again") { QuranSearch.searchVerses("the") }
        assertEquals(broad.total, broadAgain.total)
        assertTrue("the should be a broad match", broad.total > 1000)
    }

    @Test
    fun `probe - one second of the live tick`() {
        val date = LocalDate.of(2026, 9, 14)
        val day = PrayerTimesDay(
            date = date,
            prayers = listOf(
                PrayerTime(Prayer.FAJR, LocalTime.of(5, 12), LocalDateTime.of(date, LocalTime.of(5, 12))),
                PrayerTime(Prayer.SUNRISE, LocalTime.of(6, 38), LocalDateTime.of(date, LocalTime.of(6, 38))),
                PrayerTime(Prayer.DHUHR, LocalTime.of(13, 20), LocalDateTime.of(date, LocalTime.of(13, 20))),
                PrayerTime(Prayer.ASR, LocalTime.of(16, 45), LocalDateTime.of(date, LocalTime.of(16, 45))),
                PrayerTime(Prayer.MAGHRIB, LocalTime.of(19, 25), LocalDateTime.of(date, LocalTime.of(19, 25))),
                PrayerTime(Prayer.ISHA, LocalTime.of(20, 48), LocalDateTime.of(date, LocalTime.of(20, 48)))
            ),
            imsak = LocalTime.of(5, 2),
            midnight = LocalTime.of(0, 20),
            lastThirdOfNight = LocalTime.of(2, 0),
            calculationMethod = CalculationMethod.MOROCCO_MINISTRY,
            location = UserLocation.RABAT
        )
        // Exactly the calls `updateLiveTiming` makes, with the clock moving the
        // way it does on a device.
        val iterations = 500
        val start = System.nanoTime()
        var lastCountdown = ""
        repeat(iterations) { i ->
            val now = LocalDateTime.of(date, LocalTime.of(15, 30)).plusSeconds(i.toLong())
            val next = PrayerCalculationEngine.getNextPrayer(day, now.toLocalTime())
            PrayerCalculationEngine.getPreviousPrayer(day, now.toLocalTime())
            lastCountdown = PrayerCalculationEngine.formatRemainingCountdown(next.dateTime, now)
            QiblaEngine.calculateSunPosition(UserLocation.RABAT, now)
        }
        val microsPerTick = (System.nanoTime() - start) / iterations / 1_000
        println("[perf] tick_avg_us=$microsPerTick")
        assertTrue("a tick should produce a countdown", lastCountdown.isNotBlank())
    }
}
