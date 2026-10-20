package com.example.ui

import java.util.Locale

/**
 * Wraps [text] in a left-to-right isolate (LRI…PDI).
 *
 * Numbers, coordinates, dates and units are read left-to-right even when the
 * surrounding paragraph is Arabic, Hebrew or another RTL language. Without the
 * isolate the bidi algorithm reorders their neutral punctuation: "34.02°, -6.84°"
 * became "6.84°- ,34.02°" in the Arabic settings screen.
 */
fun isolateLtr(text: String): String = "\u2066$text\u2069"

/**
 * A latitude/longitude pair as one left-to-right run.
 */
fun formatCoordinates(latitude: Double, longitude: Double, decimals: Int): String {
    val pattern = "%.${decimals}f°"
    return isolateLtr(
        String.format(Locale.US, pattern, latitude) +
            ", " +
            String.format(Locale.US, pattern, longitude)
    )
}
