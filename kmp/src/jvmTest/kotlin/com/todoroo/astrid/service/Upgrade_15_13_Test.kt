package com.todoroo.astrid.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar

class Upgrade_15_13_Test : DatabaseTest() {
    private val caldavDao = db.caldavDao()
    private val upgrade = Upgrade_15_13(caldavDao)

    private suspend fun account(type: Int, url: String): CaldavAccount {
        val account = CaldavAccount(uuid = "account-$type", accountType = type, url = url)
        return account.copy(id = caldavDao.insert(account))
    }

    private suspend fun calendar(account: CaldavAccount, url: String): CaldavCalendar =
        CaldavCalendar(account = account.uuid, uuid = "calendar-${account.uuid}", url = url)
            .also { caldavDao.insert(it) }

    private suspend fun accountUrl(account: CaldavAccount) =
        caldavDao.getAccountByUuid(account.uuid!!)!!.url

    private suspend fun calendarUrl(calendar: CaldavCalendar) =
        caldavDao.getCalendarByUuid(calendar.uuid!!)!!.url

    @Test
    fun canonicalizesCaldavAccountAndCalendarUrls() = runBlocking {
        val account = account(TYPE_CALDAV, "https://CalDAV.example.com/dav/calendars/user/foo%40example.com/")
        val calendar = calendar(account, "https://CalDAV.example.com/dav/calendars/user/foo%40example.com/abc/")

        upgrade.canonicalizeUrls()

        assertEquals("https://caldav.example.com/dav/calendars/user/foo@example.com/", accountUrl(account))
        assertEquals("https://caldav.example.com/dav/calendars/user/foo@example.com/abc/", calendarUrl(calendar))
    }

    @Test
    fun canonicalizesTasksOrgUrls() = runBlocking {
        val account = account(TYPE_TASKS, "https://caldav.tasks.org/calendars/user%40example.com/")
        val calendar = calendar(account, "https://caldav.tasks.org/calendars/user%40example.com/abc/")

        upgrade.canonicalizeUrls()

        assertEquals("https://caldav.tasks.org/calendars/user@example.com/", accountUrl(account))
        assertEquals("https://caldav.tasks.org/calendars/user@example.com/abc/", calendarUrl(calendar))
    }

    @Test
    fun leavesNonCaldavIdentifiersAlone() = runBlocking {
        val etebase = account(TYPE_ETEBASE, "https://API.etebase.com/")
        val etebaseCollection = calendar(etebase, "AbC%40deF")
        val openTasks = account(TYPE_OPENTASKS, "")
        val openTasksList = calendar(openTasks, "content://org.dmfs.tasks/tasklists/1")

        upgrade.canonicalizeUrls()

        assertEquals("https://API.etebase.com/", accountUrl(etebase))
        assertEquals("AbC%40deF", calendarUrl(etebaseCollection))
        assertEquals("content://org.dmfs.tasks/tasklists/1", calendarUrl(openTasksList))
    }

    @Test
    fun leavesUrlsWithoutASchemeAlone() = runBlocking {
        val account = account(TYPE_CALDAV, "")
        val calendar = calendar(account, "example.com/dav/abc/")

        upgrade.canonicalizeUrls()

        assertEquals("", accountUrl(account))
        assertEquals("example.com/dav/abc/", calendarUrl(calendar))
    }

    @Test
    fun doesNotCollapseTwoCalendarsOntoTheSameUrl() = runBlocking {
        val account = account(TYPE_CALDAV, "https://caldav.example.com/dav/")
        val encoded = CaldavCalendar(
            account = account.uuid,
            uuid = "encoded",
            url = "https://caldav.example.com/dav/foo%40example.com/",
        ).also { caldavDao.insert(it) }
        val decoded = CaldavCalendar(
            account = account.uuid,
            uuid = "decoded",
            url = "https://caldav.example.com/dav/foo@example.com/",
        ).also { caldavDao.insert(it) }

        upgrade.canonicalizeUrls()

        assertEquals("https://caldav.example.com/dav/foo%40example.com/", calendarUrl(encoded))
        assertEquals("https://caldav.example.com/dav/foo@example.com/", calendarUrl(decoded))
    }

    @Test
    fun doesNotCollapseTwoAccountsOntoTheSameUrl() = runBlocking {
        val encoded = CaldavAccount(
            uuid = "encoded",
            accountType = TYPE_CALDAV,
            url = "https://caldav.example.com/dav/foo%40example.com/",
        ).let { it.copy(id = caldavDao.insert(it)) }
        val decoded = CaldavAccount(
            uuid = "decoded",
            accountType = TYPE_CALDAV,
            url = "https://caldav.example.com/dav/foo@example.com/",
        ).let { it.copy(id = caldavDao.insert(it)) }

        upgrade.canonicalizeUrls()

        assertEquals("https://caldav.example.com/dav/foo%40example.com/", accountUrl(encoded))
        assertEquals("https://caldav.example.com/dav/foo@example.com/", accountUrl(decoded))
    }

    @Test
    fun alreadyCanonicalUrlsAreUntouched() = runBlocking {
        val account = account(TYPE_CALDAV, "https://caldav.example.com/dav/")
        val calendar = calendar(account, "https://caldav.example.com/dav/abc/")

        upgrade.canonicalizeUrls()

        assertEquals("https://caldav.example.com/dav/", accountUrl(account))
        assertEquals("https://caldav.example.com/dav/abc/", calendarUrl(calendar))
    }
}
