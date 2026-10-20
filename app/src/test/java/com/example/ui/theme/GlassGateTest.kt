package com.example.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chrome material has exactly one gate. These tests pin its truth table so a
 * later refactor cannot quietly make glass the only path on an old device, over
 * the reader's Reduce Transparency choice, or in high contrast text mode.
 */
class GlassGateTest {

    @Test
    fun `glass requires a platform that can blur`() {
        assertFalse(glassEnabled(buildEnabled = true, sdkInt = 30, reduceTransparency = false, highContrast = false))
        assertTrue(glassEnabled(buildEnabled = true, sdkInt = 31, reduceTransparency = false, highContrast = false))
    }

    @Test
    fun `reduce transparency forces the opaque path`() {
        assertFalse(glassEnabled(buildEnabled = true, sdkInt = 36, reduceTransparency = true, highContrast = false))
    }

    @Test
    fun `high contrast text forces the opaque path`() {
        assertFalse(glassEnabled(buildEnabled = true, sdkInt = 36, reduceTransparency = false, highContrast = true))
    }

    @Test
    fun `the build flag can ship opaque chrome`() {
        assertFalse(glassEnabled(buildEnabled = false, sdkInt = 36, reduceTransparency = false, highContrast = false))
    }
}
