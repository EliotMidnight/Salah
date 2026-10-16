package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reader's takeover ends where the reader's destination does.
 *
 * ### The defect this file exists for
 *
 * `isQuranImmersive` was a persisted preference, and the only thing that ever
 * set it back to false was the reader toggling it off. So a reader who entered
 * immersive mode and then tapped Today left the dock hidden *on Today* and the
 * status bar hidden with it - and because the flag was written to preferences,
 * the first screen after every relaunch was missing its chrome too. Nothing was
 * wrong inside the reader: the fact was wrong everywhere the reader was not.
 *
 * ### Why the two halves are asserted apart
 *
 * The chrome reads [readerTakesTheScreen] and nothing else; the reset keeps the
 * stored flag from outliving the destination. They fail separately - a build can
 * derive the chrome correctly and still relive the mode on the next visit, or
 * clear the flag and still read it raw while the route is changing - so they are
 * separate tests rather than one walk through a screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImmersiveTakeoverTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the takeover is in force on the reader's destination and nowhere else`() {
        // A property over the whole enum, not one example with Today: the dock,
        // the status bar and the page padding all read this predicate, and a
        // destination added later must not inherit chrome that hides.
        for (destination in SalahDestination.entries) {
            assertEquals(
                "the takeover is not in force exactly when the route is " +
                    "'${SalahDestination.QURAN.route}', and it says otherwise for " +
                    "'${destination.route}'",
                destination == SalahDestination.QURAN,
                readerTakesTheScreen(destination.route, immersive = true)
            )
            assertFalse(
                "an unset flag took the screen on '${destination.route}'",
                readerTakesTheScreen(destination.route, immersive = false)
            )
        }
    }

    @Test
    fun `leaving the reader ends the takeover`() {
        val route = mutableStateOf(SalahDestination.QURAN.route)
        val flag = mutableStateOf(true)
        var clears = 0
        compose.setContent {
            ClearImmersiveWhenLeavingReader(route.value, flag.value) {
                clears++
                flag.value = false
            }
        }
        compose.waitForIdle()
        assertEquals("staying in the reader ended the takeover", 0, clears)

        // The tab switch. This is the case that used to leave the dock and the
        // status bar hidden on Today, and to persist that state.
        route.value = SalahDestination.TODAY.route
        compose.waitForIdle()
        assertEquals("leaving the reader left the takeover set", 1, clears)
        assertFalse("the flag outlived the destination", flag.value)
    }

    @Test
    fun `a takeover the reader already ended is not ended again`() {
        // The reset runs on every route change, and the reader's own toggle is
        // also a state write - a reset that fired on a false flag would toggle
        // the reader out of a mode they were about to enter, or out of one they
        // were already in while a transition composed.
        val route = mutableStateOf(SalahDestination.TODAY.route)
        val flag = mutableStateOf(false)
        var clears = 0
        compose.setContent {
            ClearImmersiveWhenLeavingReader(route.value, flag.value) { clears++ }
        }
        compose.waitForIdle()
        assertEquals("a flag that was never set was cleared", 0, clears)

        route.value = SalahDestination.QIBLA.route
        compose.waitForIdle()
        assertEquals("a flag that was never set was cleared on a route change", 0, clears)
    }
}
