package org.tasks.notifications

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import dev.nucleusframework.notification.linux.NotificationAction as LinuxAction

class NucleusLinuxNotificationsTest {
    @Test
    fun aTaskWithNothingOnScreenIsReportedAsAlreadyDown() = runTest {
        val notifications = NucleusLinuxNotifications(
            supportsActions = true,
            supportsBodyMarkup = true,
            listener = RecordingListener(),
            elapsedRealtime = { 0L },
        )

        assertEquals(setOf(42L), notifications.dismiss(listOf(42L)))
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        assertEquals("Water the plants", NucleusLinuxNotifications.escapeMarkup("Water the plants"))
        assertEquals(
            "Ne pas oublier - 25 % é 🪴",
            NucleusLinuxNotifications.escapeMarkup("Ne pas oublier - 25 % é 🪴"),
        )
    }

    @Test
    fun escapesTheMarkupMetacharacters() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;", NucleusLinuxNotifications.escapeMarkup("&<>\"'"))
    }

    @Test
    fun escapesAmpersandsBeforeTheEntitiesThoseEscapesIntroduce() {
        assertEquals("&amp;lt;", NucleusLinuxNotifications.escapeMarkup("&lt;"))
        assertEquals(
            "Tom &amp; Jerry &lt;b&gt;",
            NucleusLinuxNotifications.escapeMarkup("Tom & Jerry <b>"),
        )
    }

    @Test
    fun sendsAnAbsentBodyAsEmptyText() {
        assertEquals("", NucleusLinuxNotifications.bodyFor(null, supportsBodyMarkup = false))
        assertEquals("", NucleusLinuxNotifications.bodyFor(null, supportsBodyMarkup = true))
    }

    @Test
    fun escapesTheBodyOnlyWhereTheServerParsesMarkup() {
        assertEquals(
            "Tom & Jerry <b>",
            NucleusLinuxNotifications.bodyFor("Tom & Jerry <b>", supportsBodyMarkup = false),
        )
        assertEquals(
            "Tom &amp; Jerry &lt;b&gt;",
            NucleusLinuxNotifications.bodyFor("Tom & Jerry <b>", supportsBodyMarkup = true),
        )
    }

    @Test
    fun readsTheDefaultActionAsOpen() {
        assertEquals(
            NotificationAction.OPEN,
            NucleusLinuxNotifications.actionFor(LinuxAction.DEFAULT_KEY),
        )
    }

    @Test
    fun readsOurOwnKeysBack() {
        assertEquals(NotificationAction.COMPLETE, NucleusLinuxNotifications.actionFor("complete"))
        assertEquals(NotificationAction.SNOOZE, NucleusLinuxNotifications.actionFor("snooze"))
    }

    @Test
    fun ignoresKeysWeNeverSent() {
        assertNull(NucleusLinuxNotifications.actionFor("dismiss"))
        assertNull(NucleusLinuxNotifications.actionFor(""))
    }

    @Test
    fun putsOpenOnTheWireAsTheDefaultAction() = with(NucleusLinuxNotifications) {
        assertEquals(LinuxAction.DEFAULT_KEY, NotificationAction.OPEN.wireKey)
        assertEquals("complete", NotificationAction.COMPLETE.wireKey)
        assertEquals("snooze", NotificationAction.SNOOZE.wireKey)
    }

    @Test
    fun everyActionSurvivesTheRoundTrip() = with(NucleusLinuxNotifications) {
        NotificationAction.entries.forEach { assertEquals(it, actionFor(it.wireKey)) }
    }
}
