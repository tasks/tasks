package org.tasks.caldav

import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.injection.ProductionModule

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class ServerDetectionTest : CaldavTest() {

    @Test
    fun detectTasksServer() = runBlocking {
        setup(
            "DAV" to SABREDAV_COMPLIANCE,
            "x-sabre-version" to "4.1.3",
            accountType = TYPE_TASKS
        )

        sync()

        assertEquals(SERVER_TASKS, loadAccount().serverType)
    }

    @Test
    fun detectNextcloudServer() = runBlocking {
        setup("DAV" to NEXTCLOUD_COMPLIANCE)

        sync()

        assertEquals(SERVER_OWNCLOUD, loadAccount().serverType)
    }

    @Test
    fun detectSabredavServer() = runBlocking {
        setup(
            "DAV" to SABREDAV_COMPLIANCE,
            "x-sabre-version" to "4.1.3"
        )

        sync()

        assertEquals(SERVER_SABREDAV, loadAccount().serverType)
    }

    @Test
    fun detectOpenXchangeServer() = runBlocking {
        setup("server" to "Openexchange WebDAV")

        sync()

        assertEquals(SERVER_OPEN_XCHANGE, loadAccount().serverType)
    }

    @Test
    fun unknownServer() = runBlocking {
        setup()

        sync()

        assertEquals(SERVER_UNKNOWN, loadAccount().serverType)
    }

    private suspend fun loadAccount(): CaldavAccount =
        caldavDao.getAccounts().apply { assertEquals(1, size) }.first()

    private suspend fun setup(
        vararg headers: Pair<String, String>,
        accountType: Int = TYPE_CALDAV
    ) {
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = "username",
            password = encryption.encrypt("password"),
            url = server.url("/remote.php/dav/calendars/user1/").toString(),
            accountType = accountType,
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
        this.headers.putAll(headers)
        enqueue(NO_CALENDARS)
    }

    companion object {
        private const val NO_CALENDARS = """<?xml version="1.0"?><d:multistatus xmlns:d="DAV:"/>"""
        private const val SABREDAV_COMPLIANCE = "1, 3, extended-mkcol, access-control, calendarserver-principal-property-search, calendar-access, calendar-proxy, calendarserver-subscribed, calendar-auto-schedule, calendar-availability, resource-sharing, calendarserver-sharing"
        private const val NEXTCLOUD_COMPLIANCE = "1, 3, extended-mkcol, access-control, calendarserver-principal-property-search, calendar-access, calendar-proxy, calendar-auto-schedule, calendar-availability, nc-calendar-webcal-cache, calendarserver-subscribed, oc-resource-sharing, oc-calendar-publishing, calendarserver-sharing, nc-calendar-search, nc-enable-birthday-calendar"
    }
}