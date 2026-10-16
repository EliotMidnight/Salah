package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SalahViewModel
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The live ticker runs between ON_START and ON_STOP, and nowhere else.
 *
 * ### Why the assertion is about scheduling and not about state
 *
 * A tick recomputes the next prayer, the countdown and the sun's position, but
 * none of them has to *change* for a tick to have happened: `LocalDateTime.now()`
 * has second resolution, and the countdown reads in whole minutes once the
 * target is an hour away. Counting emissions would be a test of the wall clock,
 * which Robolectric does not virtualise for `java.time`, not of the ticker.
 *
 * What the ticker does control by itself is its next wake-up. The loop's `delay`
 * is a task on the main looper, and stopping the ticker cancels it, so the
 * looper's queue answers the only question here: while the app is foregrounded
 * there is a next tick; while it is not, there is none, and virtual seconds pass
 * without one appearing.
 *
 * ### The regression
 *
 * The ticker used to be started in the ViewModel's `init` and cancelled only in
 * `onCleared`, so a backgrounded app - or one left on a different tab - kept a
 * 1 Hz wake-up for its whole process life. The old behaviour fails this test at
 * the first background assertion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveTickerTest {

    @Test
    fun `the ticker stops when the app is not foregrounded and restarts on start`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SalahViewModel(application)
        val looper = shadowOf(Looper.getMainLooper())

        // Let the launch settle: the ViewModel emits while it reads its
        // preferences and opens its Room flows, and none of that is the ticker.
        looper.idleFor(Duration.ofSeconds(2))
        viewModel.setForeground(false)

        viewModel.setForeground(true)
        looper.idleFor(Duration.ofSeconds(2))
        assertTrue(
            "a foregrounded ticker should have its next tick on the looper",
            looper.nextScheduledTaskTime > Duration.ZERO
        )

        viewModel.setForeground(false)
        assertEquals(
            "stopping the ticker should cancel its pending wake-up",
            Duration.ZERO,
            looper.nextScheduledTaskTime
        )

        looper.idleFor(Duration.ofSeconds(10))
        assertEquals(
            "ten seconds in the background should not queue a tick",
            Duration.ZERO,
            looper.nextScheduledTaskTime
        )

        viewModel.setForeground(true)
        looper.idleFor(Duration.ofSeconds(2))
        assertTrue(
            "returning to the foreground should restart the ticker",
            looper.nextScheduledTaskTime > Duration.ZERO
        )
    }
}
