package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhhab
import com.example.data.model.Prayer
import com.example.data.model.PrayerAdjustments
import com.example.data.model.UserLocation
import com.example.engine.HijriCalendarEngine
import com.example.engine.PrayerCalculationEngine
import com.example.engine.QiblaEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SALAH", appName)
    }

    @Test
    fun `test prayer times calculated for Rabat Morocco`() {
        val rabat = UserLocation.RABAT
        val date = LocalDate.of(2026, 9, 14)
        val prayerTimes = PrayerCalculationEngine.calculatePrayerTimes(
            date = date,
            location = rabat,
            method = CalculationMethod.MOROCCO_MINISTRY,
            madhhab = Madhhab.STANDARD,
            adjustments = PrayerAdjustments()
        )

        assertNotNull(prayerTimes)
        assertEquals(6, prayerTimes.prayers.size)

        val fajr = prayerTimes.prayers.find { it.prayer == Prayer.FAJR }
        val maghrib = prayerTimes.prayers.find { it.prayer == Prayer.MAGHRIB }
        assertNotNull(fajr)
        assertNotNull(maghrib)

        // Fajr must precede Maghrib
        assertTrue(fajr!!.time.isBefore(maghrib!!.time))
    }

    @Test
    fun `test qibla bearing from Morocco points towards Makkah east-southeast`() {
        val bearing = QiblaEngine.calculateQiblaBearing(UserLocation.RABAT.latitude, UserLocation.RABAT.longitude)
        // From Rabat (34°N, 6.8°W) to Makkah (21.4°N, 39.8°E), bearing is approximately 95° - 105° E
        assertTrue("Qibla bearing should be around ~95-105 degrees from Rabat, got $bearing", bearing in 90f..115f)
    }

    @Test
    fun `test Hijri calendar engine computes Islamic date`() {
        val date = LocalDate.of(2026, 9, 14)
        val hijriDate = HijriCalendarEngine.getHijriDate(date)
        assertNotNull(hijriDate)
        assertTrue(hijriDate.day in 1..30)
        assertTrue(hijriDate.monthNumber in 1..12)
        assertTrue(hijriDate.year >= 1447)
    }
}
