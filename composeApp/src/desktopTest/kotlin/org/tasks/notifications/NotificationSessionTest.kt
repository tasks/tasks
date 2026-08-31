package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.tasks.di.Platform
import java.io.File

class NotificationSessionTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun theBootIdIsWhereTheKernelPublishesIt() {
        assertEquals("/proc/sys/kernel/random/boot_id", BOOT_ID_PATH)
    }

    @Test
    fun theTokenIsTheBootIdOnLinux() {
        assertEquals(ONE_BOOT, notificationSessionToken(Platform.LINUX, bootId("$ONE_BOOT\n")))
    }

    @Test
    fun aRebootIsANewToken() {
        assertNotEquals(
            notificationSessionToken(Platform.LINUX, bootId(ONE_BOOT)),
            notificationSessionToken(Platform.LINUX, bootId(ANOTHER_BOOT)),
        )
    }

    @Test
    fun theSameBootIsTheSameToken() {
        val bootId = bootId(ONE_BOOT)

        assertEquals(
            notificationSessionToken(Platform.LINUX, bootId),
            notificationSessionToken(Platform.LINUX, bootId),
        )
    }

    @Test
    fun thereIsNoTokenWhenTheBootIdCannotBeRead() {
        assertNull(notificationSessionToken(Platform.LINUX, File(folder.root, "absent")))
    }

    @Test
    fun thereIsNoTokenWhenTheBootIdIsBlank() {
        assertNull(notificationSessionToken(Platform.LINUX, bootId("   \n")))
    }

    @Test
    fun whereNoServerAssignsIdsTheTokenNeverChanges() {
        assertEquals(
            NO_SERVER_ASSIGNED_IDS,
            notificationSessionToken(Platform.WINDOWS, File(folder.root, "absent")),
        )
        assertEquals(
            NO_SERVER_ASSIGNED_IDS,
            notificationSessionToken(Platform.MAC, File(folder.root, "absent")),
        )
    }

    private fun bootId(contents: String): File = folder.newFile().also { it.writeText(contents) }

    companion object {
        private const val ONE_BOOT = "4e0dca1e-6b3e-4a02-9f2f-b2a1a4c4b0d1"
        private const val ANOTHER_BOOT = "9a77c3f0-1d24-4c8b-8e55-0f6d2b7a9c33"
    }
}
