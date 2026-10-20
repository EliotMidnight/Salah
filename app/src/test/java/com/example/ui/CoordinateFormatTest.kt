package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateFormatTest {

    @Test
    fun `coordinates are wrapped in a left-to-right isolate`() {
        assertEquals(
            "\u206634.02°, -6.84°\u2069",
            formatCoordinates(latitude = 34.0209, longitude = -6.8417, decimals = 2)
        )
    }

    @Test
    fun `number and unit are one left-to-right run`() {
        assertEquals("\u20664,812 km\u2069", isolateLtr("4,812 km"))
    }
}
