package com.example.ui.localization

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The system-access copy is new user-facing text, so every language must carry
 * all of it. A blank field renders as an empty button; a missing translation
 * would otherwise only be found by switching the app to that language.
 */
class SystemAccessStringsTest {

    private fun fields(strings: SystemAccessStrings): List<Pair<String, String>> = listOf(
        "openSettings" to strings.openSettings,
        "allowLocation" to strings.allowLocation,
        "turnOnLocation" to strings.turnOnLocation,
        "tryAgain" to strings.tryAgain,
        "allowExactAlarms" to strings.allowExactAlarms,
        "exactAlarmsDelayed" to strings.exactAlarmsDelayed,
        "notificationsPermissionNeeded" to strings.notificationsPermissionNeeded,
        "notificationsBlocked" to strings.notificationsBlocked,
        "prayerChannelBlocked" to strings.prayerChannelBlocked,
        "testNotification" to strings.testNotification
    )

    @Test
    fun `every language fills every system access string`() {
        AppLanguage.entries.forEach { language ->
            val strings = LocalizationManager.getStrings(language.code).system
            fields(strings).forEach { (name, value) ->
                assertTrue(
                    "${language.code}.$name is blank",
                    value.isNotBlank()
                )
            }
        }
    }

    @Test
    fun `non english languages do not silently fall back to english`() {
        val english = LocalizationManager.getStrings(AppLanguage.ENGLISH.code).system
        AppLanguage.entries
            .filter { it != AppLanguage.ENGLISH }
            .forEach { language ->
                assertNotEquals(
                    "${language.code} still uses the English system strings",
                    english,
                    LocalizationManager.getStrings(language.code).system
                )
            }
    }
}
