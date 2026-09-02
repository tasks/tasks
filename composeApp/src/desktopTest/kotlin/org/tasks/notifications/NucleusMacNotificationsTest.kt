package org.tasks.notifications

import dev.nucleusframework.notification.CategoryOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun everyCategoryAsksToBeToldAboutTheCloseButton() {
        val categories = NucleusMacNotifications.categories(complete = "Done", snooze = "Snooze")

        assertEquals(
            setOf(
                NucleusMacNotifications.CATEGORY_ACTIONABLE,
                NucleusMacNotifications.CATEGORY_SNOOZE_ONLY,
            ),
            categories.map { it.identifier }.toSet(),
        )
        categories.forEach {
            assertTrue(
                "${it.identifier} would not report the close button",
                CategoryOption.CUSTOM_DISMISS_ACTION in it.options,
            )
        }
    }

    @Test
    fun anIdesBundledRuntimeIsNotOurAppBundle() {
        assertTrue(
            NucleusMacNotifications.someoneElsesBundle(
                "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java"
            )
        )
        assertTrue(
            NucleusMacNotifications.someoneElsesBundle(
                "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java"
            )
        )
    }

    @Test
    fun ourOwnLauncherIsAnAppBundle() {
        assertFalse(
            NucleusMacNotifications.someoneElsesBundle(
                "/Applications/Tasks.app/Contents/MacOS/Tasks"
            )
        )
        assertFalse(
            NucleusMacNotifications.someoneElsesBundle(
                "/Users/abaker/src/tasks/build/compose/binaries/main/app/Tasks.app/Contents/MacOS/Tasks"
            )
        )
    }

    @Test
    fun anUnbundledRuntimeIsLeftToTheBridgeToRuleOut() {
        assertFalse(
            NucleusMacNotifications.someoneElsesBundle(
                "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"
            )
        )
        assertFalse(NucleusMacNotifications.someoneElsesBundle(null))
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
