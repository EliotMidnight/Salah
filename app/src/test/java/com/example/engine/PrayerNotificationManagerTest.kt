package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Channel choice is a reader-visible contract: Silent must win over Vibrate
 * only, and neither may fall through to the sound channel. The IDs are also
 * kept distinct so a channel-level block or sound change cannot bleed from one
 * choice into another. The pre-prayer reminder is its own quiet channel; it
 * must not inherit the Adhan's urgency.
 */
class PrayerNotificationManagerTest {

    @Test
    fun `silent wins over vibrate only and sound`() {
        assertEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_SOUND,
            PrayerNotificationManager.channelIdFor(isGlobalSilent = false, vibrateOnly = false)
        )
        assertEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_VIBRATE,
            PrayerNotificationManager.channelIdFor(isGlobalSilent = false, vibrateOnly = true)
        )
        assertEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT,
            PrayerNotificationManager.channelIdFor(isGlobalSilent = true, vibrateOnly = false)
        )
        assertEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT,
            PrayerNotificationManager.channelIdFor(isGlobalSilent = true, vibrateOnly = true)
        )
    }

    @Test
    fun `channels are independent`() {
        assertNotEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_SOUND,
            PrayerNotificationManager.CHANNEL_PRAYER_VIBRATE
        )
        assertNotEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_SOUND,
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT
        )
        assertNotEquals(
            PrayerNotificationManager.CHANNEL_PRAYER_VIBRATE,
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT
        )
    }

    @Test
    fun `pre-prayer reminder has its own quiet channel`() {
        val ids = setOf(
            PrayerNotificationManager.CHANNEL_PRAYER_SOUND,
            PrayerNotificationManager.CHANNEL_PRAYER_VIBRATE,
            PrayerNotificationManager.CHANNEL_PRAYER_SILENT,
            PrayerNotificationManager.CHANNEL_PRE_PRAYER
        )
        assertEquals(4, ids.size)
    }
}
