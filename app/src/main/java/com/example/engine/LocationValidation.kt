package com.example.engine

/**
 * Validation for manually entered custom coordinates (Settings screen).
 *
 * Pure Kotlin with no Android dependencies so it is unit-testable on the JVM.
 */
object LocationValidation {

    data class ValidLocation(val name: String, val latitude: Double, val longitude: Double)

    /**
     * Returns `null` when the inputs are valid, otherwise a short
     * user-facing reason describing the first problem found.
     */
    fun validate(name: String, latitude: Double?, longitude: Double?): String? {
        if (name.isBlank()) return "Please enter a place name."
        if (latitude == null || !latitude.isFinite()) return "Latitude must be a decimal number."
        if (longitude == null || !longitude.isFinite()) return "Longitude must be a decimal number."
        if (latitude !in -90.0..90.0) return "Latitude must be between -90 and +90."
        if (longitude !in -180.0..180.0) return "Longitude must be between -180 and +180."
        return null
    }

    /** Parses and validates raw text-field input in one step. */
    fun validateRaw(name: String, latText: String, lngText: String): String? {
        return validate(name.trim(), latText.trim().toDoubleOrNull(), lngText.trim().toDoubleOrNull())
    }

    /** Builds the [ValidLocation]; call only after [validate] returns null. */
    fun toValidLocation(name: String, latitude: Double, longitude: Double): ValidLocation {
        return ValidLocation(name.trim(), latitude, longitude)
    }
}
