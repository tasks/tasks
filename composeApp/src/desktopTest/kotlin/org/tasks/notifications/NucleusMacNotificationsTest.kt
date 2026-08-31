package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NucleusMacNotificationsTest {
    @Test
    fun eachButtonShapeGetsItsOwnCategory() {
        assertEquals(
            NucleusMacNotifications.CATEGORY_ACTIONABLE,
            NucleusMacNotifications.categoryFor(
                listOf(
                    NotificationAction.OPEN,
                    NotificationAction.COMPLETE,
                    NotificationAction.SNOOZE,
                ),
            ),
        )

        assertEquals(
            NucleusMacNotifications.CATEGORY_SNOOZE_ONLY,
            NucleusMacNotifications.categoryFor(
                listOf(NotificationAction.OPEN, NotificationAction.SNOOZE),
            ),
        )
    }

    @Test
    fun theCategoryIdentifiersAreTheOnesAlreadyOnScreen() {
        assertEquals("org.tasks.reminder", NucleusMacNotifications.CATEGORY_ACTIONABLE)
        assertEquals("org.tasks.reminder.snooze", NucleusMacNotifications.CATEGORY_SNOOZE_ONLY)
    }

    @Test
    fun anUnrecognisedActionIsIgnoredRatherThanGuessedAt() {
        val listener = RecordingListener()

        NucleusMacNotifications.route(listener, "com.apple.UNNotificationSomethingNew", 7L)

        assertTrue(listener.actions.isEmpty())
        assertTrue(listener.dismissals.isEmpty())
    }

    @Test
    fun aResponseWithNoTaskIdentifierIsDropped() {
        val listener = RecordingListener()

        NucleusMacNotifications.route(listener, NotificationAction.COMPLETE.key, null)

        assertTrue(listener.actions.isEmpty())
        assertTrue(listener.dismissals.isEmpty())
    }

    @Test
    fun theUsersChoiceReachesTheListener() {
        val listener = RecordingListener()

        NucleusMacNotifications.route(listener, NotificationAction.COMPLETE.key, 7L)
        NucleusMacNotifications.route(
            listener,
            "com.apple.UNNotificationDismissActionIdentifier",
            8L,
        )

        assertEquals(listOf(7L to NotificationAction.COMPLETE), listener.actions)
        assertEquals(listOf(8L), listener.dismissals)
    }
}
