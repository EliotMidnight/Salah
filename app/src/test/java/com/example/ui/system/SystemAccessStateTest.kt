package com.example.ui.system

import com.example.data.location.LocationFailure
import com.example.ui.LocationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The banners pick one cause from the most specific fact available. These tests
 * pin that order so a later edit cannot send a permanently denied permission to
 * the channel settings, or hide a disabled location service behind a permission
 * it already has.
 */
class SystemAccessStateTest {

    @Test
    fun `notification permission is reported before the app or channel switch`() {
        val base = SystemAccessState()
        assertNull(base.notificationIssue)

        assertEquals(
            NotificationIssue.PERMISSION_DENIED,
            base.copy(notificationPermission = PermissionAccess.CAN_ASK).notificationIssue
        )
        assertEquals(
            NotificationIssue.PERMISSION_DENIED,
            base.copy(notificationPermission = PermissionAccess.DENIED_PERMANENTLY)
                .notificationIssue
        )
        assertEquals(
            NotificationIssue.APP_BLOCKED,
            base.copy(appNotificationsEnabled = false).notificationIssue
        )
        assertEquals(
            NotificationIssue.CHANNEL_BLOCKED,
            base.copy(prayerChannelEnabled = false).notificationIssue
        )
        assertEquals(
            NotificationIssue.APP_BLOCKED,
            base.copy(
                appNotificationsEnabled = false,
                prayerChannelEnabled = false
            ).notificationIssue
        )
    }

    @Test
    fun `the app switch is enough to say notifications are not ready`() {
        val ready = SystemAccessState()
        assertTrue(ready.notificationsReady)
        assertFalse(ready.copy(prayerChannelEnabled = false).notificationsReady)
        assertFalse(
            ready.copy(notificationPermission = PermissionAccess.DENIED_PERMANENTLY)
                .notificationsReady
        )
    }

    @Test
    fun `a current location problem wins over a stale resolved status`() {
        val resolved = LocationStatus.Resolved(
            name = "Marrakesh",
            country = "Morocco",
            isFresh = true
        )
        assertEquals(
            resolved,
            locationBannerStatus(
                status = resolved,
                systemAccess = SystemAccessState()
            )
        )
        assertEquals(
            LocationStatus.Failed(LocationFailure.PERMISSION_NOT_GRANTED),
            locationBannerStatus(
                status = resolved,
                systemAccess = SystemAccessState(
                    locationPermission = PermissionAccess.DENIED_PERMANENTLY
                )
            )
        )
        assertEquals(
            LocationStatus.Failed(LocationFailure.SERVICES_DISABLED),
            locationBannerStatus(
                status = null,
                systemAccess = SystemAccessState(locationServicesEnabled = false)
            )
        )
    }

    @Test
    fun `location permission is reported before the service switch`() {
        val base = SystemAccessState()
        assertNull(base.locationIssue)

        assertEquals(
            LocationIssue.PERMISSION_DENIED_CAN_ASK,
            base.copy(locationPermission = PermissionAccess.CAN_ASK).locationIssue
        )
        assertEquals(
            LocationIssue.PERMISSION_DENIED_PERMANENTLY,
            base.copy(locationPermission = PermissionAccess.DENIED_PERMANENTLY).locationIssue
        )
        assertEquals(
            LocationIssue.SERVICES_DISABLED,
            base.copy(locationServicesEnabled = false).locationIssue
        )
        assertEquals(
            LocationIssue.PERMISSION_DENIED_CAN_ASK,
            base.copy(
                locationPermission = PermissionAccess.CAN_ASK,
                locationServicesEnabled = false
            ).locationIssue
        )
    }
}
