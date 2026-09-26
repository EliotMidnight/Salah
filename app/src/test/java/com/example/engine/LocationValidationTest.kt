package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocationValidationTest {

    @Test
    fun `valid coordinates pass`() {
        assertNull(LocationValidation.validate("Rabat", 34.02, -6.84))
        assertNull(LocationValidation.validateRaw("  Mecca ", "21.42", "39.82"))
    }

    @Test
    fun `blank name rejected`() {
        assertNotNull(LocationValidation.validate("", 34.0, -6.0))
        assertNotNull(LocationValidation.validate("   ", 34.0, -6.0))
    }

    @Test
    fun `out-of-range latitude rejected`() {
        assertNotNull(LocationValidation.validate("North Pole+", 90.1, 0.0))
        assertNotNull(LocationValidation.validate("South", -90.5, 0.0))
        assertNull(LocationValidation.validate("Edge", 90.0, 180.0))
        assertNull(LocationValidation.validate("Edge", -90.0, -180.0))
    }

    @Test
    fun `out-of-range longitude rejected`() {
        assertNotNull(LocationValidation.validate("Far", 0.0, 180.5))
        assertNotNull(LocationValidation.validate("Far", 0.0, -181.0))
    }

    @Test
    fun `non-numeric raw input rejected`() {
        assertNotNull(LocationValidation.validateRaw("X", "abc", "10"))
        assertNotNull(LocationValidation.validateRaw("X", "10", ""))
        assertNotNull(LocationValidation.validateRaw("X", "NaN", "10"))
        assertNotNull(LocationValidation.validateRaw("X", "Infinity", "10"))
    }

    @Test
    fun `toValidLocation trims name`() {
        val valid = LocationValidation.toValidLocation("  Rabat  ", 34.0, -6.0)
        assertEquals("Rabat", valid.name)
    }
}
