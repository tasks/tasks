package org.tasks.caldav

import com.todoroo.astrid.service.Upgrade_15_13
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.analytics.Reporting
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskDeleter

class CalendarUrlSyncTest : DatabaseTest() {
    private val server = failFastServer()
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val encryption = testEncryption()
    private val provider = testClientProvider(preferences, encryption)
    private val reporting = mock<Reporting>()
    private val synchronizer = CaldavSynchronizer(
        caldavDao = caldavDao,
        dirtyDao = db.dirtyDao(),
        refreshBroadcaster = mock(),
        taskDeleter = TaskDeleter(
            deletionDao = db.deletionDao(),
            taskDao = taskDao,
            caldavDao = caldavDao,
            refreshBroadcaster = mock(),
            vtodoCache = mock(),
            tasksPreferences = preferences,
            taskCleanup = object : TaskCleanup {},
        ),
        reporting = reporting,
        provider = provider,
        iCal = mock(),
        principalDao = db.principalDao(),
        vtodoCache = mock(),
        accountDataRepository = mock(),
        tagMetadataSync = mock { onBlocking { isPrimary(any()) } doReturn false },
    )
    private lateinit var account: CaldavAccount

    @Before
    fun setUp() = runBlocking {
        server.start()
        account = CaldavAccount(
            uuid = "account",
            username = "user",
            password = encryption.encrypt("password"),
            url = server.url(HOME_SET).toString(),
        ).let { it.copy(id = caldavDao.insert(it)) }
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueueCalendars(vararg hrefs: String) = server.enqueue(multiStatus(multistatus(*hrefs)))

    private suspend fun insertSyncedTask(calendar: CaldavCalendar): Long {
        val task = Task(title = "task")
        taskDao.createNew(task)
        caldavDao.insertOrUpdateAndMarkSynced(
            CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = "task", etag = "etag")
        )
        return task.id
    }

    @Test
    fun `list stored by okhttp survives the first sync after upgrading`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/",
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars("$HOME_SET$LIST/")

        Upgrade_15_13(caldavDao).canonicalizeUrls()
        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(listOf("calendar"), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNotNull(taskDao.fetch(task))
        assertNotNull(caldavDao.getTask(task))
    }

    @Test
    fun `list survives a sync when the server reports absolute hrefs`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars("${server.url(HOME_SET)}$LIST/")

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(listOf("calendar"), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNotNull(taskDao.fetch(task))
        assertNotNull(caldavDao.getTask(task))
    }

    @Test
    fun `list survives a sync when the server reports hrefs with a differently cased host`() = runBlocking {
        val account = account.copy(url = "http://localhost:${server.port}$HOME_SET").also { caldavDao.update(it) }
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars("http://LOCALHOST:${server.port}$HOME_SET$LIST/")

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(listOf("calendar"), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNotNull(taskDao.fetch(task))
        assertNotNull(caldavDao.getTask(task))
    }

    @Test
    fun `sync reports an unparseable url instead of crashing`() = runBlocking {
        val account = account.copy(url = "https://example.com:port/").also { caldavDao.update(it) }

        synchronizer.sync(account, hasPro = true)

        assertEquals("Invalid URL: https://example.com:port/", caldavDao.getAccountByUuid(account.uuid!!)!!.error)
        verify(reporting, never()).reportException(any(), any())
    }

    @Test
    fun `list created by makeCollection matches the href the server reports`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))
        provider.forAccount(account).use { client ->
            val created = client.makeCollection("Created", 0, null)

            enqueueCalendars(server.takeRequest().path!!)

            assertEquals(created, client.calendars().single().href.toString())
            assertEquals(created, created.canonicalUrl())
        }
    }

    @Test
    fun `home set is stored in the same form as the hrefs it yields`() = runBlocking {
        server.enqueue(multiStatus(homeSet(HOME_SET)))
        server.enqueue(multiStatus(homeSet(HOME_SET)))

        val homeSet = provider.forUrl(server.url("/").toString(), "user", "password").use { it.homeSet() }

        assertEquals("${server.url("/")}dav/calendars/user/foo@example.com/", homeSet)
    }

    companion object {
        private const val HOME_SET = "/dav/calendars/user/foo%40example.com/"
        private const val LIST = "1be1b8c8-9c2b-4b3f-9a9a-000000000000"
        private const val CTAG = "ctag-1"

        private fun multistatus(vararg hrefs: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/">
                <d:response>
                    <d:href>$HOME_SET</d:href>
                    <d:propstat>
                        <d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                ${hrefs.joinToString("") { calendar(it) }}
            </d:multistatus>
        """.trimIndent()

        private fun calendar(href: String) = """
            <d:response>
                <d:href>$href</d:href>
                <d:propstat>
                    <d:prop>
                        <d:resourcetype><d:collection /><cal:calendar /></d:resourcetype>
                        <d:displayname>List</d:displayname>
                        <cal:supported-calendar-component-set><cal:comp name="VTODO" /></cal:supported-calendar-component-set>
                        <cs:getctag>$CTAG</cs:getctag>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
        """

        private fun homeSet(href: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                <d:response>
                    <d:href>/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:current-user-principal><d:href>/</d:href></d:current-user-principal>
                            <cal:calendar-home-set><d:href>$href</d:href></cal:calendar-home-set>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        init {
            CaldavSynchronizer.registerFactories()
        }
    }
}
