package com.example.ui.home

import com.example.data.model.Prayer
import com.example.ui.home.PrayerTimeRowState.Current
import com.example.ui.home.PrayerTimeRowState.Future
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A day the reader has browsed to carries no "now".
 *
 * ### The defect this file exists for
 *
 * The row marks - accent and semibold for the current prayer, muted for the ones the
 * walk calls past - were fed the same two prayers whether or not the reader had
 * stepped to another day. A scrubbed day has no live pair to be fed: it is drawn from
 * a fixed anchor, the day's own Maghrib, and the marks followed that anchor, so
 * tomorrow's page dimmed the prayer after it - Isha, the last of the day, drawn as
 * already gone on a page whose own "now" was Maghrib, the prayer before it.
 *
 * ### Why this is a test and not a screenshot
 *
 * The screenshot suite renders today, where the marks are unchanged either way. The
 * decision is a function of the day and the two prayers it is given, so the sweep is
 * every prayer against every pair of them, on both kinds of day.
 *
 * Today's current row is pinned here too - exactly one per pair of live prayers, never
 * two. Which of the *other* rows counts as past is not: that is the subject of a
 * known disagreement, noted on [prayerRowState].
 */
class TodayRowStateTest {

    private val prayers = Prayer.entries.toList()

    @Test
    fun `a day that is not today is all future, for every pair of live prayers`() {
        var checked = 0
        for (current in prayers) {
            for (next in prayers) {
                for (prayer in prayers) {
                    assertEquals(
                        "on a browsed day, ${prayer.name.lowercase()} was marked " +
                            "${prayerRowState(false, prayer, current, next)} with " +
                            "${current.name.lowercase()} current and " +
                            "${next.name.lowercase()} next",
                        Future,
                        prayerRowState(
                            isToday = false,
                            prayer = prayer,
                            currentPrayer = current,
                            nextPrayer = next
                        )
                    )
                    checked++
                }
            }
        }
        assertEquals(
            "the sweep no longer covers every prayer against every pair of live prayers",
            prayers.size * prayers.size * prayers.size,
            checked
        )
    }

    @Test
    fun `the same sweep on today does find a current row, so the marks exist at all`() {
        // Without this, the test above would pass for a function that returned Future
        // unconditionally - which is one way to make the bug go away and lose the mark
        // in the process.
        var currents = 0
        for (current in prayers) {
            for (next in prayers) {
                for (prayer in prayers) {
                    if (prayerRowState(true, prayer, current, next) == Current) currents++
                }
            }
        }
        assertEquals(
            "today's list found $currents current rows across " +
                "${prayers.size * prayers.size * prayers.size} combinations; it should " +
                "be one per pair of live prayers",
            prayers.size * prayers.size,
            currents
        )
    }

    @Test
    fun `today never marks two rows current`() {
        for (current in prayers) {
            for (next in prayers) {
                val states = prayers.map { prayerRowState(true, it, current, next) }

                assertEquals(
                    "with ${current.name.lowercase()} current and ${next.name.lowercase()} " +
                        "next, the day has ${states.count { it == Current }} current rows: " +
                        "$states",
                    1,
                    states.count { it == Current }
                )
            }
        }
    }

    @Test
    fun `with no next prayer there is nothing past`() {
        // The end of the day: the ticker has not yet reported what follows. Nothing can
        // be "between the current and the next" when there is no next, and a row marked
        // past on a guess is worse than an unmarked one.
        for (current in prayers) {
            for (prayer in prayers) {
                val state = prayerRowState(true, prayer, current, nextPrayer = null)

                assertTrue(
                    "${prayer.name.lowercase()} was marked past with no next prayer",
                    state == Future || state == Current
                )
            }
        }
    }
}
