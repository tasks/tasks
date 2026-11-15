package org.tasks.caldav

import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.service.Upgrade_15_14_1
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.caldav.LineSeparatorFixtures.CUT_OFF_NOTES
import org.tasks.caldav.LineSeparatorFixtures.DAMAGED
import org.tasks.caldav.LineSeparatorFixtures.NOTES
import org.tasks.caldav.LineSeparatorFixtures.VTODO
import org.tasks.caldav.LineSeparatorFixtures.multiget
import org.tasks.data.MergedGeofence
import org.tasks.data.TaskSaver
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.icalendar.isWellFormedICalendar
import org.tasks.icalendar.parseVTodos
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.reminders.Random
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskDeleter

class ResetSyncTest : DatabaseTest() {
    @get:Rule val folder = TemporaryFolder()

    private val server = failFastServer()
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val dirtyDao = db.dirtyDao()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val encryption = testEncryption()
    private val vtodoCache by lazy { VtodoCache(caldavDao, FileStorage(folder.root.absolutePath)) }

    private val appPreferences: AppPreferences = mock {
        onBlocking { addTasksToTop() } doReturn false
        onBlocking { isDefaultDueTimeEnabled() } doReturn false
        onBlocking { defaultAlarms() } doReturn emptyList()
        onBlocking { defaultDueTime() } doReturn 0
        onBlocking { isCurrentlyQuietHours() } doReturn false
        onBlocking { defaultPriority() } doReturn Task.Priority.NONE
        onBlocking { defaultRingMode() } doReturn 0
        onBlocking { defaultLocationReminder() } doReturn 0
    }

    private val locationService = object : LocationService {
        override val locationDao = db.locationDao()
        override val appPreferences = this@ResetSyncTest.appPreferences
        override suspend fun currentLocation(): MapPosition? = null
        override fun addGeofences(geofence: MergedGeofence) = Unit
        override fun removeGeofences(place: Place) = Unit
    }

    private val iCal by lazy {
        val taskSaver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = mock(),
            notifier = mock(),
            locationService = locationService,
            timerPlugin = mock(),
            backgroundWork = mock(),
            caldavDao = caldavDao,
        )
        iCalendar(
            tagDataDao = db.tagDataDao(),
            preferences = appPreferences,
            locationDao = db.locationDao(),
            geocoder = mock(),
            locationService = locationService,
            tagDao = db.tagDao(),
            taskDao = taskDao,
            dirtyDao = dirtyDao,
            taskSaver = taskSaver,
            caldavDao = caldavDao,
            alarmDao = db.alarmDao(),
            alarmService = AlarmService(
                alarmDao = db.alarmDao(),
                taskDao = taskDao,
                dirtyDao = dirtyDao,
                refreshBroadcaster = mock(),
                notifier = mock(),
                alarmCalculator = AlarmCalculator(Random()),
                preferences = appPreferences,
            ),
            vtodoCache = vtodoCache,
            notifier = mock(),
        )
    }

    private val taskDeleter by lazy {
        TaskDeleter(
            deletionDao = db.deletionDao(),
            taskDao = taskDao,
            caldavDao = caldavDao,
            refreshBroadcaster = mock(),
            vtodoCache = vtodoCache,
            tasksPreferences = preferences,
            taskCleanup = object : TaskCleanup {},
        )
    }

    private val synchronizer by lazy {
        CaldavSynchronizer(
            caldavDao = caldavDao,
            dirtyDao = dirtyDao,
            refreshBroadcaster = mock(),
            taskDeleter = taskDeleter,
            reporting = mock(),
            provider = testClientProvider(preferences, encryption),
            iCal = iCal,
            principalDao = db.principalDao(),
            vtodoCache = vtodoCache,
            accountDataRepository = mock(),
            tagMetadataSync = mock { onBlocking { isPrimary(any()) } doReturn false },
        )
    }

    private lateinit var account: CaldavAccount
    private lateinit var calendar: CaldavCalendar

    @Before
    fun setUp() = runBlocking {
        server.start()
        account = CaldavAccount(
            uuid = "account",
            username = "user",
            password = encryption.encrypt("password"),
            url = server.url(HOME_SET).toString(),
        ).let { it.copy(id = caldavDao.insert(it)) }
        calendar = CaldavCalendar(
            account = account.uuid,
            uuid = "calendar",
            url = server.url("$HOME_SET$LIST/").toString().canonicalUrl(),
            ctag = CTAG,
        ).also { caldavDao.insert(it) }
    }

    @After
    fun tearDown() = server.shutdown()

    private suspend fun damagedTask(cached: String = DAMAGED): Long {
        iCal.fromVtodo(account, calendar, null, parseVTodos(DAMAGED).single(), DAMAGED, OBJ, "etag-1")
        val caldavTask = caldavDao.getTasks(calendar.uuid!!, listOf(OBJ)).single()
        if (cached != DAMAGED) {
            vtodoCache.putVtodo(calendar, caldavTask, cached)
        }
        assertEquals(CUT_OFF_NOTES, taskDao.fetch(caldavTask.task)!!.notes)
        return caldavTask.task
    }

    private suspend fun edit(taskId: Long, change: Task.() -> Task) {
        taskDao.update(taskDao.fetch(taskId)!!.change())
        dirtyDao.setDirty(listOf(taskId))
    }

    private fun reset() = runBlocking { Upgrade_15_14_1(caldavDao).run() }

    private fun enqueueRedownload(vtodo: String = VTODO) {
        server.enqueue(multiStatus(calendars()))
        server.enqueue(multiStatus(etags(OBJ)))
        server.enqueue(multiStatus(multiget("$HOME_SET$LIST/$OBJ", vtodo)))
    }

    private fun sync() = runBlocking {
        synchronizer.sync(account, hasPro = true)
        val error = caldavDao.getAccountByUuid(account.uuid!!)!!.error
        assertFalse(error, !error.isNullOrBlank())
    }

    private fun requests() = List(server.requestCount) { server.takeRequest() }

    private fun List<okhttp3.mockwebserver.RecordedRequest>.puts() =
        filter { it.method == "PUT" }.map { it.body.readUtf8() }

    private fun unfoldedDescription(ics: String) =
        ics.replace("\r\n ", "").lines().single { it.startsWith("DESCRIPTION:") }.removePrefix("DESCRIPTION:")

    @Test
    fun uneditedTaskIsRepairedWithoutPushing() = runBlocking {
        val id = damagedTask()
        reset()
        enqueueRedownload()

        sync()

        val task = taskDao.fetch(id)!!
        assertEquals(NOTES, task.notes)
        assertEquals(Task.Priority.LOW, task.priority)
        assertTrue(requests().puts().isEmpty())
        assertEquals("etag-1", caldavDao.getTask(id)!!.etag)
        assertEquals(VTODO.replace("\r\n", "\n"), vtodoCache.getVtodo(calendar, caldavDao.getTask(id)!!)!!.replace("\r\n", "\n"))
    }

    @Test
    fun editedTaskKeepsTheEditAndPushesRepairedFields() = runBlocking {
        val id = damagedTask()
        edit(id) { copy(title = "Garden supplies") }
        reset()
        enqueueRedownload()
        server.enqueue(MockResponse().setResponseCode(204).setHeader("ETag", "\"etag-2\""))

        sync()

        val task = taskDao.fetch(id)!!
        assertEquals("Garden supplies", task.title)
        assertEquals(NOTES, task.notes)
        val put = requests().puts().single()
        assertTrue(put.isWellFormedICalendar())
        assertTrue(put.contains("SUMMARY:Garden supplies"))
        assertEquals(VTODO.let(::unfoldedDescription), unfoldedDescription(put))
        assertTrue(put.contains("X-APPLE-SORT-ORDER:123456"))
        assertFalse(dirtyDao.isDirty(caldavDao.getTask(id)!!.id)!!)
    }

    @Test
    fun taskThatWasNeverPushedSurvives() = runBlocking {
        damagedTask()
        val local = Task(title = "new")
        taskDao.createNew(local)
        caldavDao.insert(CaldavTask(task = local.id, calendar = calendar.uuid, remoteId = "new", obj = "new.ics"))
        dirtyDao.setDirty(listOf(local.id))
        reset()
        enqueueRedownload()
        server.enqueue(MockResponse().setResponseCode(201).setHeader("ETag", "\"etag-new\""))

        sync()

        assertNotNull(taskDao.fetch(local.id))
        assertFalse(taskDao.fetch(local.id)!!.isDeleted)
        assertTrue(requests().any { it.method == "PUT" && it.path!!.endsWith("/new.ics") })
    }

    @Test
    fun locallyDeletedTaskIsNotRestored() = runBlocking {
        val id = damagedTask()
        taskDeleter.markDeleted(listOf(id))
        reset()
        enqueueRedownload()
        server.enqueue(MockResponse().setResponseCode(204))

        sync()

        val requests = requests()
        assertTrue(requests.any { it.method == "DELETE" && it.path!!.endsWith("/$OBJ") })
        assertTrue(taskDao.fetch(id)?.isDeleted != false)
        assertTrue(caldavDao.getTasks(calendar.uuid!!, listOf(OBJ)).all { it.isDeleted() || it.task == id })
    }

    @Test
    fun editedNotesAreKept() = runBlocking {
        val id = damagedTask()
        edit(id) { copy(notes = "$CUT_OFF_NOTES, rake") }
        reset()
        enqueueRedownload()
        server.enqueue(MockResponse().setResponseCode(204).setHeader("ETag", "\"etag-2\""))

        sync()

        assertEquals("$CUT_OFF_NOTES, rake", taskDao.fetch(id)!!.notes)
        assertEquals("$CUT_OFF_NOTES\\, rake", unfoldedDescription(requests().puts().single()))
    }

    companion object {
        private const val HOME_SET = "/dav/calendars/user/foo/"
        private const val LIST = "list"
        private const val CTAG = "ctag-1"
        private const val OBJ = "task.ics"

        private fun etags(vararg names: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                ${names.joinToString("") { """
                    <d:response>
                        <d:href>$HOME_SET$LIST/$it</d:href>
                        <d:propstat>
                            <d:prop><d:getetag>"etag-1"</d:getetag></d:prop>
                            <d:status>HTTP/1.1 200 OK</d:status>
                        </d:propstat>
                    </d:response>
                """ }}
            </d:multistatus>
        """.trimIndent()

        private fun calendars() = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/">
                <d:response>
                    <d:href>$HOME_SET</d:href>
                    <d:propstat>
                        <d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                <d:response>
                    <d:href>$HOME_SET$LIST/</d:href>
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
            </d:multistatus>
        """.trimIndent()

        init {
            CaldavClient.registerFactories()
        }
    }
}
