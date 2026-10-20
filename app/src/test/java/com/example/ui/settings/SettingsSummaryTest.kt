package com.example.ui.settings

import com.example.data.model.PrayerAdjustments
import com.example.ui.SalahUiState
import com.example.ui.localization.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The adjustment row is visible before its sheet opens, so its zero state is
 * user-facing copy. It used to be the English word "None" in every language.
 */
class SettingsSummaryTest {

    @Test
    fun `zero adjustments read as a language-neutral sign`() {
        assertEquals("±0", adjustmentSummary(SalahUiState(), EnglishStrings))
    }

    @Test
    fun `non-zero adjustments stay named and signed`() {
        val state = SalahUiState(
            adjustments = PrayerAdjustments(fajr = 5, isha = -3)
        )
        assertEquals("Fajr +5, Isha -3", adjustmentSummary(state, EnglishStrings))
    }
}
