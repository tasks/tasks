package org.tasks.caldav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileStorageTest {
    private val fileStorage = FileStorage("/data/files")

    @Test
    fun `joins segments under the vtodo root`() {
        assertEquals(
            "/data/files/vtodo/account/calendar/object.ics",
            fileStorage.getFile("account", "calendar", "object.ics")?.toString(),
        )
    }

    @Test
    fun `no segments is the root itself`() {
        assertEquals(fileStorage.root, fileStorage.getFile())
    }

    @Test
    fun `rejects blank segments`() {
        assertNull(fileStorage.getFile("account", "", "object.ics"))
        assertNull(fileStorage.getFile("account", null, "object.ics"))
    }

    @Test
    fun `rejects absolute object names`() {
        assertNull(fileStorage.getFile("account", "calendar", "/etc/passwd"))
    }

    @Test
    fun `rejects object names that escape the root`() {
        assertNull(fileStorage.getFile("account", "calendar", "../../../evil.ics"))
    }

    @Test
    fun `allows traversal that stays under the root`() {
        assertEquals(
            "/data/files/vtodo/account/object.ics",
            fileStorage.getFile("account", "calendar", "../object.ics")?.toString(),
        )
    }
}
