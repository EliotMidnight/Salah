package com.example.ui.home

import com.example.data.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Locks down the 24-hour clock's geometry.
 *
 * The clock is the one place in the app where a wrong number is silently
 * plausible: a misplaced prayer still looks like a clock, and a reversed arc
 * still looks like an arc. Both of the ways it was actually wrong while being
 * built are pinned here rather than left to a screenshot - a sweep that went
 * backwards instead of wrapping past midnight, and windows whose "am I inside
 * it" test used a different basis from the arc that drew them.
 */
class ClockGeometryTest {

    private fun at(h: Int, m: Int) = LocalTime.of(h, m)

    /** Maghrib at 12 o'clock, and the day runs clockwise from there. */
    @Test
    fun `maghrib is at the top`() {
        val maghribHour = ClockGeometry.hoursOf(at(19, 25))
        assertEquals(0f, ClockGeometry.angleOf(at(19, 25), maghribHour), 0.01f)
    }

    @Test
    fun `the day runs clockwise`() {
        val maghribHour = ClockGeometry.hoursOf(at(19, 25))
        // Isha is 83 minutes after Maghrib.
        assertEquals(83f / 60f / 24f * 360f, ClockGeometry.angleOf(at(20, 48), maghribHour), 0.5f)
    }

    @Test
    fun `a time before maghrib wraps past midnight instead of going negative`() {
        val maghribHour = ClockGeometry.hoursOf(at(19, 25))
        // Fajr is about six hours *before* Maghrib, but must land after it going
        // clockwise, not at a negative angle.
        val fajr = ClockGeometry.angleOf(at(5, 12), maghribHour)
        assertTrue("Fajr should sit in the second half of the dial, was $fajr", fajr in 120f..240f)
    }

    @Test
    fun `position puts zero degrees at the top`() {
        val top = ClockGeometry.position(0f, 50f)
        assertEquals(50f, top.x, 0.01f)
        assertEquals(0f, top.y, 0.01f)
    }

    @Test
    fun `position puts ninety degrees at the right`() {
        val right = ClockGeometry.position(90f, 50f)
        assertEquals(100f, right.x, 0.01f)
        assertEquals(50f, right.y, 0.01f)
    }

    /**
     * The bug this exists for: `endAngle - startAngle` is negative whenever the
     * window ends at or before its start on the dial, and a negative sweep makes
     * `drawArc` travel anticlockwise from the wrong end.
     */
    @Test
    fun `sweep wraps forwards when the window crosses the top`() {
        assertEquals(120f, ClockGeometry.sweepFromTo(startAngle = 300f, endAngle = 60f), 0.01f)
    }

    @Test
    fun `sweep is never negative`() {
        assertTrue(ClockGeometry.sweepFromTo(10f, 5f) > 0f)
        assertTrue(ClockGeometry.sweepFromTo(0f, 0f) >= 0f)
    }

    /** Labels sit on an ellipse, so the ones at 3 and 9 o'clock stay on screen. */
    @Test
    fun `label anchors are inside the box horizontally`() {
        val right = ClockGeometry.labelPosition(90f, rx = 44f, ry = 57f)
        assertTrue("right-hand label was at x=${right.x}", right.x <= 94f)
        val left = ClockGeometry.labelPosition(270f, rx = 44f, ry = 57f)
        assertTrue("left-hand label was at x=${left.x}", left.x >= 6f)
        // Vertically they stay outside the ring, which is where they read best.
        assertTrue(ClockGeometry.labelPosition(0f, 44f, 57f).y < 50f)
        assertTrue(ClockGeometry.labelPosition(180f, 44f, 57f).y > 50f)
    }

    /** 19:25 as an hour of the day, which is 19.4167, not 19. */
    private val maghribHour = 19f + 25f / 60f
    /** A 9h47m night from 19:25 to 05:12. */
    private val nightHours = 9f + 47f / 60f

    @Test
    fun `the last third of the night ends at fajr`() {
        val last = ClockGeometry.lastThirdOfNight(at(19, 25), at(5, 12))
        assertEquals("should end at Fajr", 5.2f, last.endHour, 0.02f)
        // Wrapped into 0..24, so this lands just after 01:00 rather than 25:56.
        assertEquals("should open two thirds into the night", (maghribHour + nightHours * 2f / 3f) % 24f, last.startHour, 0.05f)
    }

    @Test
    fun `the last third is the window the arc claims it is`() {
        val last = ClockGeometry.lastThirdOfNight(at(19, 25), at(5, 12))
        // Opens at 19:25 + 2/3 of 9:47, which is about 01:56.
        assertTrue("02:00 is inside the last third", last.contains(2f))
        assertTrue("04:30 is inside the last third", last.contains(4.5f))
        assertTrue("20:00 is the first third, not the last", !last.contains(20f))
        assertTrue("23:30 is the first third, not the last", !last.contains(23.5f))
        assertTrue("06:00 is past Fajr", !last.contains(6f))
    }

    @Test
    fun `the first third ends a third of the way through the night`() {
        val first = ClockGeometry.firstThirdEnd(at(19, 25), at(5, 12))
        assertEquals(maghribHour + nightHours / 3f, first.endHour, 0.05f)
    }

    @Test
    fun `duha stops before dhuhr because zawal is makruh`() {
        val duha = ClockGeometry.duha(at(6, 38), at(13, 20))
        // Opens 20 minutes after sunrise, closes 15 minutes before Dhuhr.
        assertEquals(6f + 58f / 60f, duha.startHour, 0.01f)
        assertEquals(13f + 5f / 60f, duha.endHour, 0.01f)
        assertTrue("07:30 is inside Duha", duha.contains(7f + 30f / 60f))
        assertTrue("06:50 is before sunrise+20min, so before Duha", !duha.contains(6f + 50f / 60f))
        assertTrue("13:10 is past zawal and must be outside Duha", !duha.contains(13f + 10f / 60f))
    }

    @Test
    fun `qaylula opens half an hour after dhuhr`() {
        val qaylula = ClockGeometry.qaylula(at(13, 20), at(16, 45))
        val dhuhrHour = 13f + 20f / 60f
        assertEquals("opens 30 min after Dhuhr", dhuhrHour + 0.5f, qaylula.startHour, 0.01f)
        // Closes 60% of the way from Dhuhr to Asr.
        assertEquals(dhuhrHour + (16f + 45f / 60f - dhuhrHour) * 0.6f, qaylula.endHour, 0.01f)
    }

    @Test
    fun `a missing prayer degrades instead of throwing`() {
        val empty = ClockGeometry.lastThirdOfNight(null, at(5, 12))
        assertEquals(0f, empty.startAngle, 0.01f)
        assertEquals(0f, ClockGeometry.sweepFromTo(empty.startAngle, empty.endAngle), 0.01f)
    }

    @Test
    fun `the clock lists all six times around the dial`() {
        assertEquals(
            listOf(Prayer.MAGHRIB, Prayer.ISHA, Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR),
            ClockMarkerOrder
        )
    }

    @Test
    fun `the list order starts at fajr and includes sunrise`() {
        assertEquals(Prayer.FAJR, PrayerListOrder.first())
        assertEquals(Prayer.SUNRISE, PrayerListOrder[1])
        assertEquals(Prayer.ISHA, PrayerListOrder.last())
    }
}
