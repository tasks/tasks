package org.tasks.caldav

import com.todoroo.astrid.service.Upgrade_15_13
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.analytics.Reporting
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.icalendar.VTodo
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
    private val iCal = mock<iCalendar>()
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
        iCal = iCal,
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
    fun `list missing from the listing is kept and recanonicalized when the server resolves it`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST",
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars("$HOME_SET$LIST/")
        server.enqueue(multiStatus(collection("$HOME_SET$LIST/")))

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        val stored = caldavDao.getCalendarsByAccount(account.uuid!!).single()
        assertEquals("calendar", stored.uuid)
        assertEquals("${account.url}$LIST/".canonicalUrl(), stored.url)
        assertNotNull(taskDao.fetch(task))
        assertNotNull(caldavDao.getTask(task))
    }

    @Test
    fun `server type is sniffed from a propfind that redirected to another host`() = runBlocking {
        val redirected = failFastServer()
        redirected.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", redirected.url(HOME_SET).toString())
            )
            redirected.enqueue(multiStatus(multistatus()).setHeader("x-sabre-version", "4.4.0"))

            synchronizer.sync(account, hasPro = true)

            assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
            assertEquals(SERVER_SABREDAV, caldavDao.getAccountByUuid(account.uuid!!)!!.serverType)
        } finally {
            redirected.shutdown()
        }
    }

    @Test
    fun `an unparseable task does not abort the rest of the calendar sync`() = runBlocking {
        CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = "stale",
        ).also { caldavDao.insert(it) }
        enqueueCalendars("$HOME_SET$LIST/")
        server.enqueue(multiStatus(etags("a.ics", "b.ics")))
        server.enqueue(multiStatus(calendarData("a.ics" to TWO_VTODOS, "b.ics" to ONE_VTODO)))

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        verify(iCal, times(1)).fromVtodo(
            any<CaldavAccount>(),
            any<CaldavCalendar>(),
            anyOrNull<CaldavTask>(),
            any<VTodo>(),
            anyOrNull<String>(),
            eq("b.ics"),
            eq("etag-b.ics"),
        )
        assertEquals(CTAG, caldavDao.getCalendarByUuid("calendar")!!.ctag)
    }

    @Test
    fun `list the server reports as gone is deleted`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars()
        server.enqueue(MockResponse().setResponseCode(404))

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(emptyList<String>(), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNull(taskDao.fetch(task))
    }

    @Test
    fun `list the server reports as gone inside a multistatus is deleted`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars()
        server.enqueue(multiStatus(notFound("$HOME_SET$LIST/")))

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(emptyList<String>(), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNull(taskDao.fetch(task))
    }

    @Test
    fun `list the server no longer shares with us is deleted`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars()
        server.enqueue(MockResponse().setResponseCode(403))

        synchronizer.sync(account, hasPro = true)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
        assertEquals(emptyList<String>(), caldavDao.getCalendarsByAccount(account.uuid!!).map { it.uuid })
        assertNull(taskDao.fetch(task))
    }

    @Test
    fun `list is kept when the server can't confirm it is gone`() = runBlocking {
        val calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = "${account.url}$LIST/".canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
        val task = insertSyncedTask(calendar)
        enqueueCalendars()
        server.enqueue(MockResponse().setResponseCode(500))

        synchronizer.sync(account, hasPro = true)

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

        private const val ONE_VTODO =
            "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//E//EN\nBEGIN:VTODO\nUID:b\nSUMMARY:B\nEND:VTODO\nEND:VCALENDAR"

        private const val TWO_VTODOS =
            "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//E//EN\nBEGIN:VTODO\nUID:a\nSUMMARY:A\nEND:VTODO\n" +
                    "BEGIN:VTODO\nUID:a\nRECURRENCE-ID:20260308T090000Z\nSUMMARY:A2\nEND:VTODO\nEND:VCALENDAR"

        private fun etags(vararg names: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                ${names.joinToString("") { """
                    <d:response>
                        <d:href>$HOME_SET$LIST/$it</d:href>
                        <d:propstat>
                            <d:prop><d:getetag>"etag-$it"</d:getetag></d:prop>
                            <d:status>HTTP/1.1 200 OK</d:status>
                        </d:propstat>
                    </d:response>
                """ }}
            </d:multistatus>
        """.trimIndent()

        private fun calendarData(vararg items: Pair<String, String>) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                ${items.joinToString("") { (name, data) -> """
                    <d:response>
                        <d:href>$HOME_SET$LIST/$name</d:href>
                        <d:propstat>
                            <d:prop>
                                <d:getetag>"etag-$name"</d:getetag>
                                <cal:calendar-data>${data.replace("\n", "&#13;&#10;")}</cal:calendar-data>
                            </d:prop>
                            <d:status>HTTP/1.1 200 OK</d:status>
                        </d:propstat>
                    </d:response>
                """ }}
            </d:multistatus>
        """.trimIndent()

        private fun notFound(href: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>$href</d:href>
                    <d:status>HTTP/1.1 404 Not Found</d:status>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private fun collection(href: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/">
                ${calendar(href)}
            </d:multistatus>
        """.trimIndent()

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
            CaldavClient.registerFactories()
        }
    }
}
