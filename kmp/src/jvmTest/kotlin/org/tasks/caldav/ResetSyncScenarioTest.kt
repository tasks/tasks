package org.tasks.caldav

import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.service.Upgrade_15_14_1
import io.ktor.http.decodeURLPart
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.data.MergedGeofence
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.reminders.Random
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfSecond

class ResetSyncScenarioTest : DatabaseTest() {
    @get:Rule val folder = TemporaryFolder()

    private val dav = FakeCalDav()
    private val server = MockWebServer().apply { dispatcher = dav }
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val dirtyDao = db.dirtyDao()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val encryption = testEncryption()
    private val vtodoCache by lazy { VtodoCache(caldavDao, FileStorage(folder.root.absolutePath)) }
    private val notifier: Notifier = mock()
    private var defaultAlarms: List<Alarm> = emptyList()

    private val appPreferences: AppPreferences = mock {
        onBlocking { addTasksToTop() } doReturn false
        onBlocking { isDefaultDueTimeEnabled() } doReturn false
        onBlocking { defaultAlarms() } doAnswer { defaultAlarms }
        onBlocking { defaultDueTime() } doReturn 0
        onBlocking { isCurrentlyQuietHours() } doReturn false
        onBlocking { defaultPriority() } doReturn Task.Priority.NONE
        onBlocking { defaultRingMode() } doReturn 0
        onBlocking { defaultLocationReminder() } doReturn 0
    }

    private val locationService = object : LocationService {
        override val locationDao = db.locationDao()
        override val appPreferences = this@ResetSyncScenarioTest.appPreferences
        override suspend fun currentLocation(): MapPosition? = null
        override fun addGeofences(geofence: MergedGeofence) = Unit
        override fun removeGeofences(place: Place) = Unit
    }

    private val iCal by lazy {
        iCalendar(
            tagDataDao = db.tagDataDao(),
            preferences = appPreferences,
            locationDao = db.locationDao(),
            geocoder = mock(),
            locationService = locationService,
            tagDao = db.tagDao(),
            taskDao = taskDao,
            dirtyDao = dirtyDao,
            taskSaver = TaskSaver(
                taskDao = taskDao,
                refreshBroadcaster = mock(),
                notifier = notifier,
                locationService = locationService,
                timerPlugin = mock(),
                backgroundWork = mock(),
                caldavDao = caldavDao,
            ),
            caldavDao = caldavDao,
            alarmDao = db.alarmDao(),
            alarmService = AlarmService(
                alarmDao = db.alarmDao(),
                taskDao = taskDao,
                dirtyDao = dirtyDao,
                refreshBroadcaster = mock(),
                notifier = notifier,
                alarmCalculator = AlarmCalculator(Random()),
                preferences = appPreferences,
            ),
            vtodoCache = vtodoCache,
            notifier = notifier,
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

    private val taskMover by lazy {
        TaskMover(
            taskDao = taskDao,
            caldavDao = caldavDao,
            googleTaskDao = db.googleTaskDao(),
            dirtyDao = dirtyDao,
            appPreferences = appPreferences,
            refreshBroadcaster = mock(),
            taskDeleter = taskDeleter,
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

    @Before
    fun setUp() = runBlocking {
        server.start()
        caldavDao.insert(
            CaldavAccount(
                uuid = ACCOUNT,
                username = "user",
                password = encryption.encrypt("password"),
                url = server.url(HOME_SET).toString(),
            )
        )
        Unit
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun undamagedTasksAreLeftExactlyAsTheyWere() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        dav.put(LIST, "done.ics", vtodo("done", "SUMMARY:Done", "STATUS:COMPLETED", "COMPLETED:20260105T000000Z"))
        dav.put(LIST, "parent.ics", vtodo("parent", "SUMMARY:Parent"))
        dav.put(LIST, "child.ics", vtodo("child", "SUMMARY:Child", "RELATED-TO:parent"))
        dav.put(LIST, "alarm.ics", vtodo("alarm", "SUMMARY:Alarm", "DUE:20270101T090000Z", *ALARM))
        sync()
        createLocal(LIST, "written-by-tasks") {
            copy(title = "Written by Tasks", notes = "local notes", priority = Task.Priority.HIGH)
        }
        sync()
        assertEquals(taskId("parent"), taskDao.fetch(taskId("child"))!!.parent)
        assertEquals(listOf("Errands", "Garden"), snapshot("rich").tags)
        assertEquals(1, snapshot("alarm").alarms.size)
        assertNotNull(snapshot("rich").place)
        val before = snapshots()
        dav.requests.clear()
        clearInvocations(notifier)

        reset()
        sync()

        assertEquals(before, snapshots())
        assertTrue(dav.writes().isEmpty())
        for (uid in listOf("rich", "parent", "child", "alarm", "written-by-tasks")) {
            verify(notifier, never()).cancel(eq(taskId(uid)), any())
        }
        assertEquals(dav.ctag(LIST), calendar(LIST).ctag)
    }

    @Test
    fun everyTaskInEveryListIsDownloadedOnceWithoutDuplicatesOrDeletions() = runBlocking {
        repeat(75) { dav.put(LIST, "a$it.ics", vtodo("a$it", "SUMMARY:A $it")) }
        repeat(5) { dav.put(OTHER, "b$it.ics", vtodo("b$it", "SUMMARY:B $it")) }
        sync()
        val before = snapshots()
        assertEquals(80, before.size)
        dav.requests.clear()

        reset()
        sync()

        val requested = dav.multigetHrefs()
        assertEquals(80, requested.size)
        assertEquals(80, requested.toSet().size)
        assertEquals(before, snapshots())
        assertEquals(80, taskDao.getAll().size)
        assertTrue(dav.writes().isEmpty())
        assertEquals(dav.ctag(LIST), calendar(LIST).ctag)
        assertEquals(dav.ctag(OTHER), calendar(OTHER).ctag)
    }

    @Test
    fun syncingAgainAfterTheRepairDownloadsNothing() = runBlocking {
        repeat(3) { dav.put(LIST, "a$it.ics", vtodo("a$it", "SUMMARY:A $it")) }
        sync()
        reset()
        sync()
        dav.requests.clear()

        sync()

        assertTrue(dav.requests.none { it.method == "REPORT" })
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun editsMadeBeforeTheUpgradeAreKeptAndPushed() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        val before = snapshot("rich")
        edit("rich") { copy(title = "Edited") }

        reset()
        sync()

        val after = snapshot("rich")
        assertEquals("Edited", after.task.title)
        assertEquals(before.task.notes, after.task.notes)
        assertEquals(before.task.priority, after.task.priority)
        assertEquals(before.task.dueDate, after.task.dueDate)
        assertEquals(before.task.recurrence, after.task.recurrence)
        assertEquals(before.tags, after.tags)
        assertEquals(before.alarms, after.alarms)
        assertFalse(after.dirty!!)
        val put = dav.writes().single()
        assertEquals("PUT", put.method)
        assertTrue(put.body.contains("SUMMARY:Edited"))
        assertTrue(put.body.contains("X-CUSTOM-PROP:keep me"))
        assertTrue(put.body.contains("BEGIN:VALARM"))
        assertTrue(dav.ics(LIST, "rich.ics")!!.contains("SUMMARY:Edited"))
    }

    @Test
    fun localAndRemoteChangesToDifferentFieldsAreBothKept() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        edit("rich") { copy(title = "Edited locally") }
        dav.put(LIST, "rich.ics", RICH.replace("PRIORITY:1", "PRIORITY:9").replace("Line one", "Changed remotely"))

        reset()
        sync()

        val task = taskDao.fetch(taskId("rich"))!!
        assertEquals("Edited locally", task.title)
        assertEquals(Task.Priority.LOW, task.priority)
        assertTrue(task.notes!!.startsWith("Changed remotely"))
        val put = dav.writes().single().body
        assertTrue(put.contains("SUMMARY:Edited locally"))
        assertTrue(put.contains("PRIORITY:9"))
        assertTrue(put.contains("Changed remotely"))
    }

    @Test
    fun localChangeWinsWhenBothSidesChangedTheSameField() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        edit("rich") { copy(title = "Local title") }
        dav.put(LIST, "rich.ics", RICH.replace("SUMMARY:Rich", "SUMMARY:Remote title"))

        reset()
        sync()

        assertEquals("Local title", taskDao.fetch(taskId("rich"))!!.title)
        assertTrue(dav.ics(LIST, "rich.ics")!!.contains("SUMMARY:Local title"))
    }

    @Test
    fun tasksCreatedBeforeTheUpgradeArePushed() = runBlocking {
        dav.put(LIST, "existing.ics", vtodo("existing", "SUMMARY:Existing"))
        sync()
        createLocal(LIST, "new") { copy(title = "Created offline") }

        reset()
        sync()

        assertEquals(2, taskDao.getAll().size)
        assertEquals("Created offline", taskDao.fetch(taskId("new"))!!.title)
        assertTrue(dav.ics(LIST, "new.ics")!!.contains("SUMMARY:Created offline"))
        assertEquals(listOf("PUT /new.ics"), dav.writes().map { "${it.method} /${it.name}" })
    }

    @Test
    fun taskWhosePushWasNeverRecordedIsPushedAgain() = runBlocking {
        dav.put(LIST, "existing.ics", vtodo("existing", "SUMMARY:Existing"))
        sync()
        createLocal(LIST, "unrecorded") { copy(title = "Edited after the push") }
        dirtyDao.setDirtyState(caldavDao.getTask(taskId("unrecorded"))!!.id, dirtyVersion = 1, syncedVersion = 0)
        dav.put(LIST, "unrecorded.ics", vtodo("unrecorded", "SUMMARY:Pushed"))
        vtodoCache.putVtodo(calendar(LIST), caldavDao.getTask(taskId("unrecorded"))!!, dav.ics(LIST, "unrecorded.ics"))

        reset()
        sync()

        assertEquals("Edited after the push", taskDao.fetch(taskId("unrecorded"))!!.title)
        assertEquals(listOf("PUT /unrecorded.ics"), dav.writes().map { "${it.method} /${it.name}" })
        assertTrue(dav.ics(LIST, "unrecorded.ics")!!.contains("SUMMARY:Edited after the push"))
        assertFalse(snapshot("unrecorded").dirty!!)
    }

    @Test
    fun tasksDeletedBeforeTheUpgradeAreDeletedOnTheServer() = runBlocking {
        dav.put(LIST, "keep.ics", vtodo("keep", "SUMMARY:Keep"))
        dav.put(LIST, "gone.ics", vtodo("gone", "SUMMARY:Gone"))
        sync()
        taskDeleter.markDeleted(listOf(taskId("gone")))

        reset()
        sync()

        assertEquals(listOf("DELETE /gone.ics"), dav.writes().map { "${it.method} /${it.name}" })
        assertNull(dav.ics(LIST, "gone.ics"))
        assertEquals(listOf("Keep"), taskDao.getAll().filterNot { it.isDeleted }.map { it.title })
    }

    @Test
    fun tasksMovedBeforeTheUpgradeAreNotDuplicatedOrLost() = runBlocking {
        dav.put(LIST, "moved.ics", vtodo("moved", "SUMMARY:Moved", "DESCRIPTION:notes"))
        dav.put(OTHER, "other.ics", vtodo("other", "SUMMARY:Other"))
        sync()
        val id = taskId("moved")
        taskMover.move(listOf(id), CaldavFilter(calendar = calendar(OTHER), account = account()))
        dav.put(LIST, "moved.ics", vtodo("moved", "SUMMARY:Changed in the old list", "DESCRIPTION:other notes"))

        reset()
        sync()

        val live = taskDao.getAll().filterNot { it.isDeleted }
        assertEquals(listOf("Moved", "Other"), live.mapNotNull { it.title }.sorted())
        assertEquals(calendar(OTHER).uuid, caldavDao.getTask(id)!!.calendar)
        assertEquals("notes", taskDao.fetch(id)!!.notes)
        assertNull(dav.ics(LIST, "moved.ics"))
        assertEquals(1, dav.objects(OTHER).count { it.contains("SUMMARY:Moved") })
    }

    @Test
    fun tasksAddedOnTheServerAreImportedOnce() = runBlocking {
        dav.put(LIST, "existing.ics", vtodo("existing", "SUMMARY:Existing"))
        sync()
        dav.put(LIST, "added.ics", vtodo("added", "SUMMARY:Added"))

        reset()
        sync()

        assertEquals(listOf("Added", "Existing"), taskDao.getAll().mapNotNull { it.title }.sorted())
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun tasksDeletedOnTheServerAreRemovedLocally() = runBlocking {
        dav.put(LIST, "keep.ics", vtodo("keep", "SUMMARY:Keep"))
        dav.put(LIST, "gone.ics", vtodo("gone", "SUMMARY:Gone"))
        sync()
        dav.remove(LIST, "gone.ics")

        reset()
        sync()

        assertEquals(listOf("Keep"), taskDao.getAll().filterNot { it.isDeleted }.map { it.title })
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun aFailedDownloadPushesNothingDeletesNothingAndResumes() = runBlocking {
        repeat(75) { dav.put(LIST, "a$it.ics", vtodo("a$it", "SUMMARY:A $it")) }
        sync()
        edit("a70") { copy(title = "Edited") }
        reset()
        dav.failMultiget = 2

        synchronizer.sync(account(), hasPro = true)

        assertFalse(account().error.isNullOrBlank())
        assertTrue(dav.writes().isEmpty())
        assertEquals(75, taskDao.getAll().count { !it.isDeleted })
        assertEquals("Edited", taskDao.fetch(taskId("a70"))!!.title)
        assertTrue(snapshot("a70").dirty!!)
        assertEquals(Upgrade_15_14_1.CTAG, calendar(LIST).ctag)
        dav.failMultiget = null
        dav.requests.clear()

        sync()

        assertEquals(45, dav.multigetHrefs().size)
        assertEquals(listOf("PUT /a70.ics"), dav.writes().map { "${it.method} /${it.name}" })
        assertEquals(75, taskDao.getAll().count { !it.isDeleted })
        assertNotNull(calendar(LIST).ctag)
        assertTrue(dav.ics(LIST, "a70.ics")!!.contains("SUMMARY:Edited"))
    }

    @Test
    fun escapedFileNamesStillMatchTheirTasks() = runBlocking {
        dav.put(LIST, "foo@bar.ics", vtodo("foo@bar", "SUMMARY:At"))
        dav.put(LIST, "with space.ics", vtodo("with space", "SUMMARY:Space"))
        sync()
        val before = snapshots()

        reset()
        sync()

        assertEquals(before, snapshots())
        assertEquals(2, taskDao.getAll().size)
        assertTrue(dav.writes().isEmpty())
        assertEquals(setOf("foo@bar.ics", "with space.ics"), before.values.map { it.caldavTask.obj }.toSet())
    }

    @Test
    fun existingTasksDoNotGetDefaultReminders() = runBlocking {
        dav.put(LIST, "plain.ics", vtodo("plain", "SUMMARY:Plain", "DUE:20270101T090000Z"))
        sync()
        defaultAlarms = listOf(Alarm(type = Alarm.TYPE_REL_END))

        reset()
        sync()

        assertTrue(db.alarmDao().getAlarms(taskId("plain")).isEmpty())
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun tasksAddedOnTheServerDuringTheRepairGetDefaultReminders() = runBlocking {
        dav.put(LIST, "existing.ics", vtodo("existing", "SUMMARY:Existing", "DUE:20270101T090000Z"))
        sync()
        defaultAlarms = listOf(Alarm(type = Alarm.TYPE_REL_END))
        dav.put(LIST, "added.ics", vtodo("added", "SUMMARY:Added", "DUE:20270101T090000Z"))

        reset()
        sync()

        assertEquals(listOf(Alarm.TYPE_REL_END), db.alarmDao().getAlarms(taskId("added")).map { it.type })
        assertTrue(db.alarmDao().getAlarms(taskId("existing")).isEmpty())
    }

    @Test
    fun listsThatNeverFinishedTheirFirstSyncImportWithoutDefaultReminders() = runBlocking {
        dav.put(LIST, "a.ics", vtodo("a", "SUMMARY:A", "DUE:20270101T090000Z"))
        dav.put(OTHER, "b.ics", vtodo("b", "SUMMARY:B", "DUE:20270101T090000Z"))
        for ((list, ctag) in listOf(LIST to null, OTHER to "")) {
            caldavDao.insert(
                CaldavCalendar(
                    account = ACCOUNT,
                    uuid = list,
                    name = list,
                    url = server.url("$HOME_SET$list/").toString().canonicalUrl(),
                    ctag = ctag,
                )
            )
        }
        defaultAlarms = listOf(Alarm(type = Alarm.TYPE_REL_END))

        reset()
        assertNull(calendar(LIST).ctag)
        assertEquals("", calendar(OTHER).ctag)
        sync()

        assertTrue(db.alarmDao().getAlarms(taskId("a")).isEmpty())
        assertTrue(db.alarmDao().getAlarms(taskId("b")).isEmpty())
    }

    @Test
    fun unchangedTaskWithoutACachedCopyTakesTheServerCopy() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        val before = snapshot("rich")
        vtodoCache.delete(calendar(LIST), caldavDao.getTask(taskId("rich"))!!)

        reset()
        sync()

        assertEquals(before, snapshot("rich"))
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun taskWithoutAnETagInTheListingIsNeitherDownloadedNorDeleted() = runBlocking {
        dav.put(LIST, "a.ics", vtodo("a", "SUMMARY:A"))
        dav.put(LIST, "b.ics", vtodo("b", "SUMMARY:B"))
        sync()
        val before = snapshot("b")
        dav.withoutETag += "b.ics"
        dav.requests.clear()

        reset()
        sync()

        assertEquals(listOf("$HOME_SET$LIST/a.ics"), dav.multigetHrefs())
        assertEquals(before.copy(caldavTask = before.caldavTask.copy(etag = null)), snapshot("b"))
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun tasksDeletedOnBothSidesAreRemovedWithoutAnError() = runBlocking {
        dav.put(LIST, "keep.ics", vtodo("keep", "SUMMARY:Keep"))
        dav.put(LIST, "gone.ics", vtodo("gone", "SUMMARY:Gone"))
        sync()
        taskDeleter.markDeleted(listOf(taskId("gone")))
        dav.remove(LIST, "gone.ics")

        reset()
        sync()

        assertTrue(dav.writes().isEmpty())
        assertEquals(listOf("Keep"), taskDao.getAll().map { it.title })
    }

    @Test
    fun aFailedDeleteIsRetriedOnTheNextSync() = runBlocking {
        dav.put(LIST, "gone.ics", vtodo("gone", "SUMMARY:Gone"))
        sync()
        val id = taskId("gone")
        taskDeleter.markDeleted(listOf(id))
        reset()
        dav.deleteStatus = 500

        synchronizer.sync(account(), hasPro = true)

        assertNotNull(dav.ics(LIST, "gone.ics"))
        assertTrue(taskDao.fetch(id)!!.isDeleted)
        assertNotNull(caldavDao.getTask(id))
        dav.deleteStatus = 204

        sync()

        assertNull(dav.ics(LIST, "gone.ics"))
        assertNull(taskDao.fetch(id))
    }

    @Test
    fun aRejectedPushStopsTheSyncAndKeepsTheEdit() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        edit("rich") { copy(title = "Edited") }
        reset()
        dav.putStatus = 415

        synchronizer.sync(account(), hasPro = true)

        assertFalse(account().error.isNullOrBlank())
        assertEquals("Edited", taskDao.fetch(taskId("rich"))!!.title)
        assertTrue(snapshot("rich").dirty!!)
        assertTrue(dav.ics(LIST, "rich.ics")!!.contains("SUMMARY:Rich"))
        dav.putStatus = 201

        sync()

        assertFalse(snapshot("rich").dirty!!)
        assertTrue(dav.ics(LIST, "rich.ics")!!.contains("SUMMARY:Edited"))
    }

    @Test
    fun pushesToServersThatReturnNoETagAreNotUndone() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        edit("rich") { copy(title = "Edited") }
        dav.putETag = false
        reset()
        sync()
        val pushed = snapshot("rich")
        dav.requests.clear()

        sync()

        assertEquals(listOf("$HOME_SET$LIST/rich.ics"), dav.multigetHrefs())
        assertEquals("Edited", pushed.task.title)
        assertEquals(pushed.copy(caldavTask = pushed.caldavTask.copy(etag = dav.etag(LIST, "rich.ics"))), snapshot("rich"))
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun editedTaskWithoutACachedCopyKeepsTheEdit() = runBlocking {
        dav.put(LIST, "rich.ics", RICH)
        sync()
        edit("rich") { copy(title = "Edited") }
        vtodoCache.delete(calendar(LIST), caldavDao.getTask(taskId("rich"))!!)

        reset()
        sync()

        assertEquals("Edited", taskDao.fetch(taskId("rich"))!!.title)
        assertFalse(snapshot("rich").dirty!!)
        val put = dav.writes().single().body
        assertTrue(put.contains("SUMMARY:Edited"))
        assertTrue(put.contains("X-CUSTOM-PROP:keep me"))
    }

    @Test
    fun localRemindersAreKeptOnServersWithoutReminderSync() = runBlocking {
        caldavDao.update(account().copy(serverType = CaldavAccount.SERVER_SYNOLOGY_CALENDAR))
        dav.put(LIST, "plain.ics", vtodo("plain", "SUMMARY:Plain", "DUE:20270101T090000Z"))
        sync()
        db.alarmDao().insert(Alarm(task = taskId("plain"), type = Alarm.TYPE_REL_END))
        val before = snapshot("plain")
        assertEquals(1, before.alarms.size)

        reset()
        sync()

        assertEquals(before, snapshot("plain"))
        assertTrue(dav.writes().isEmpty())
    }

    @Test
    fun readOnlyListsAreRepairedWithoutPushing() = runBlocking {
        dav.readOnly += LIST
        dav.put(LIST, "rich.ics", RICH)
        sync()
        val before = snapshot("rich")
        assertTrue(before.task.readOnly)

        reset()
        sync()

        assertEquals(before, snapshot("rich"))
        assertTrue(dav.writes().isEmpty())
    }

    private data class Snapshot(
        val task: Task,
        val caldavTask: CaldavTask,
        val tags: List<String>,
        val alarms: List<Alarm>,
        val place: Place?,
        val dirty: Boolean?,
        val cached: String?,
    )

    private suspend fun account() = caldavDao.getAccountByUuid(ACCOUNT)!!

    private suspend fun calendar(list: String) =
        caldavDao.getCalendarByUrl(ACCOUNT, server.url("$HOME_SET$list/").toString().canonicalUrl())!!

    private suspend fun taskId(remoteId: String) =
        taskDao.getAll().single { caldavDao.getTask(it.id)?.remoteId == remoteId }.id

    private suspend fun snapshot(remoteId: String) = snapshot(taskId(remoteId))

    private suspend fun snapshot(taskId: Long): Snapshot {
        val caldavTask = caldavDao.getTask(taskId)!!
        val task = taskDao.fetch(taskId)!!
        return Snapshot(
            task = task.copy(
                creationDate = task.creationDate.startOfSecond(),
                modificationDate = task.modificationDate.startOfSecond(),
            ),
            caldavTask = caldavTask,
            tags = db.tagDataDao().getTagDataForTask(taskId).mapNotNull { it.name }.sorted(),
            alarms = db.alarmDao().getAlarms(taskId).map { it.copy(id = 0) }.sortedBy { it.toString() },
            place = db.locationDao().getPlaceForTask(taskId),
            dirty = dirtyDao.isDirty(caldavTask.id),
            cached = vtodoCache.getVtodo(caldavDao.getCalendarByUuid(caldavTask.calendar!!), caldavTask),
        )
    }

    private suspend fun snapshots(): Map<String, Snapshot> =
        taskDao.getAll().associate { caldavDao.getTask(it.id)!!.remoteId!! to snapshot(it.id) }

    private suspend fun createLocal(list: String, uid: String, change: Task.() -> Task) {
        val task = Task(modificationDate = currentTimeMillis()).change()
        taskDao.createNew(task)
        caldavDao.insert(CaldavTask(task = task.id, calendar = calendar(list).uuid, remoteId = uid))
        dirtyDao.setDirty(listOf(task.id))
    }

    private suspend fun edit(remoteId: String, change: Task.() -> Task) {
        val id = taskId(remoteId)
        taskDao.update(taskDao.fetch(id)!!.change())
        dirtyDao.setDirty(listOf(id))
    }

    private suspend fun reset() = Upgrade_15_14_1(caldavDao).run()

    private suspend fun sync() {
        synchronizer.sync(account(), hasPro = true)
        val error = account().error
        assertTrue(error, error.isNullOrBlank())
    }

    private class Request(val method: String, val path: String, val body: String) {
        val name get() = path.substringAfterLast('/').decodeURLPart()
    }

    private inner class FakeCalDav : Dispatcher() {
        private val lists = linkedMapOf<String, LinkedHashMap<String, Pair<String, String>>>()
        private val versions = mutableMapOf<String, Int>()
        private var etags = 0
        private var multigets = 0
        val requests: MutableList<Request> = java.util.Collections.synchronizedList(mutableListOf())
        val withoutETag = mutableSetOf<String>()
        val readOnly = mutableSetOf<String>()
        var putStatus = 201
        var deleteStatus = 204
        var putETag = true
        var failMultiget: Int? = null
            set(value) {
                field = value
                multigets = 0
            }

        @Synchronized
        fun put(list: String, name: String, ics: String): String {
            val etag = "e${++etags}"
            lists.getOrPut(list) { linkedMapOf() }[name] = etag to ics
            versions[list] = (versions[list] ?: 0) + 1
            return etag
        }

        @Synchronized
        fun remove(list: String, name: String) {
            lists[list]?.remove(name)
            versions[list] = (versions[list] ?: 0) + 1
        }

        @Synchronized
        fun ics(list: String, name: String) = lists[list]?.get(name)?.second

        @Synchronized
        fun etag(list: String, name: String) = lists[list]?.get(name)?.first

        @Synchronized
        fun objects(list: String) = lists[list]?.values?.map { it.second }.orEmpty()

        @Synchronized
        fun ctag(list: String) = "ctag-$list-${versions[list] ?: 0}"

        fun writes() = requests.filter { it.method == "PUT" || it.method == "DELETE" }

        fun multigetHrefs() = requests
            .filter { it.method == "REPORT" && it.body.contains("calendar-multiget") }
            .flatMap { HREF.findAll(it.body).map { match -> match.groupValues[1] }.toList() }

        @Synchronized
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path!!.substringBefore('?')
            val recorded = Request(request.method!!, path, request.body.readUtf8())
            requests += recorded
            val relative = path.removePrefix(HOME_SET)
            val list = relative.substringBefore('/')
            return when (recorded.method) {
                "PROPFIND" -> if (relative.isEmpty()) multiStatus(listing()) else MockResponse().setResponseCode(404)
                "REPORT" -> if (recorded.body.contains("calendar-multiget")) {
                    if (++multigets == failMultiget) {
                        MockResponse().setResponseCode(500)
                    } else {
                        multiStatus(multiget(list, HREF.findAll(recorded.body).map { it.groupValues[1] }.toList()))
                    }
                } else {
                    multiStatus(query(list))
                }
                "PUT" -> if (putStatus >= 300) {
                    MockResponse().setResponseCode(putStatus)
                } else {
                    val etag = put(list, recorded.name, recorded.body)
                    MockResponse().setResponseCode(putStatus).apply { if (putETag) setHeader("ETag", "\"$etag\"") }
                }
                "DELETE" -> if (deleteStatus >= 300) {
                    MockResponse().setResponseCode(deleteStatus)
                } else if (lists[list]?.containsKey(recorded.name) == true) {
                    remove(list, recorded.name)
                    MockResponse().setResponseCode(204)
                } else {
                    MockResponse().setResponseCode(404)
                }
                else -> MockResponse().setResponseCode(405)
            }
        }

        private fun href(list: String, name: String) =
            "$HOME_SET$list/${name.replace("@", "%40").replace(" ", "%20")}"

        private fun listing() = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/">
                <d:response>
                    <d:href>$HOME_SET</d:href>
                    <d:propstat>
                        <d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                ${lists.keys.joinToString("") { list -> """
                <d:response>
                    <d:href>$HOME_SET$list/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype><d:collection /><cal:calendar /></d:resourcetype>
                            <d:displayname>$list</d:displayname>
                            <cal:supported-calendar-component-set><cal:comp name="VTODO" /></cal:supported-calendar-component-set>
                            <cs:getctag>${ctag(list)}</cs:getctag>
                            ${if (list in readOnly) "<d:current-user-privilege-set><d:privilege><d:read /></d:privilege></d:current-user-privilege-set>" else ""}
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                """ }}
            </d:multistatus>
        """.trimIndent().trim()

        private fun query(list: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                ${lists[list].orEmpty().entries.joinToString("") { (name, value) -> """
                <d:response>
                    <d:href>${href(list, name)}</d:href>
                    <d:propstat>
                        <d:prop>${if (name in withoutETag) "" else "<d:getetag>\"${value.first}\"</d:getetag>"}</d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                """ }}
            </d:multistatus>
        """.trimIndent().trim()

        private fun multiget(list: String, hrefs: List<String>) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                ${hrefs.joinToString("") { href ->
                    val name = href.substringAfterLast('/').decodeURLPart()
                    val value = lists[list]?.get(name)
                    if (value == null) """
                <d:response>
                    <d:href>$href</d:href>
                    <d:status>HTTP/1.1 404 Not Found</d:status>
                </d:response>
                    """ else """
                <d:response>
                    <d:href>$href</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>"${value.first}"</d:getetag>
                            <cal:calendar-data>${value.second.xmlEscaped()}</cal:calendar-data>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                    """
                }}
            </d:multistatus>
        """.trimIndent().trim()
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val HOME_SET = "/dav/calendars/user/"
        private const val LIST = "list"
        private const val OTHER = "other"
        private val HREF = Regex("<(?:[A-Za-z0-9]+:)?href>([^<]+)</(?:[A-Za-z0-9]+:)?href>")

        private val ALARM = arrayOf(
            "BEGIN:VALARM",
            "ACTION:DISPLAY",
            "DESCRIPTION:Reminder",
            "TRIGGER;RELATED=END:-PT15M",
            "END:VALARM",
        )

        private fun vtodo(uid: String, vararg properties: String) = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Other//Client//EN",
            "BEGIN:VTODO",
            "DTSTAMP:20260101T000000Z",
            "UID:$uid",
            "CREATED:20260101T000000Z",
            "LAST-MODIFIED:20260102T000000Z",
            *properties,
            "END:VTODO",
            "END:VCALENDAR",
        ).joinToString("\r\n", postfix = "\r\n")

        private val RICH = vtodo(
            "rich",
            "SUMMARY:Rich",
            "DESCRIPTION:Line one\\nLine two",
            "PRIORITY:1",
            "DTSTART:20270110T090000Z",
            "DUE:20270115T090000Z",
            "RRULE:FREQ=WEEKLY;BYDAY=MO",
            "CATEGORIES:Garden,Errands",
            "GEO:52.52;13.4",
            "X-APPLE-SORT-ORDER:42",
            "X-CUSTOM-PROP:keep me",
            *ALARM,
        )

        private fun String.xmlEscaped() =
            replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;")

        init {
            CaldavClient.registerFactories()
        }
    }
}
