package org.tasks.backup

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.service.Upgrade_15_13
import com.todoroo.astrid.service.Upgrade_15_14_1
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.tasks.BuildConfig
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.ETAG
import org.tasks.makers.CaldavTaskMaker.OBJECT
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltAndroidTest
class TasksJsonImporterTest : InjectingTestCase() {
    @Inject lateinit var jsonExporter: TasksJsonExporter
    @Inject lateinit var jsonImporter: TasksJsonImporter
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var database: Database
    @Inject lateinit var vtodoCache: VtodoCache

    @Before
    @After
    fun clearVtodoCache() {
        runBlocking { vtodoCache.clear() }
    }

    private suspend fun export(): ByteArray {
        val os = ByteArrayOutputStream()
        jsonExporter.doTasksExport(os, taskDao.getAllTaskIds())
        return os.toByteArray()
    }

    private suspend fun import(backup: ByteArray): TasksJsonImporter.ImportResult =
        jsonImporter.importTasks(context, { ByteArrayInputStream(backup) })

    private fun ByteArray.withVersion(version: Int) = String(this, Charsets.UTF_8)
        .replace("\"version\":${BuildConfig.VERSION_CODE}", "\"version\":$version")
        .toByteArray(Charsets.UTF_8)

    private suspend fun setupAccount(
        uuid: String,
        accountType: Int = TYPE_TASKS,
        url: String? = "https://caldav.tasks.org/calendars/user1/",
        username: String? = "user@test.com",
    ) {
        caldavDao.insert(
            CaldavAccount(
                uuid = uuid,
                accountType = accountType,
                url = url,
                username = username,
            )
        )
    }

    private suspend fun setupCalendar(
        account: String,
        uuid: String,
        url: String? = "https://caldav.tasks.org/calendars/user1/tasks/",
        name: String = "Tasks",
        ctag: String? = null,
    ) {
        caldavDao.insert(
            CaldavCalendar(
                account = account,
                uuid = uuid,
                url = url,
                name = name,
                ctag = ctag,
            )
        )
    }

    private suspend fun setupTask(
        title: String,
        calendar: String,
        remoteId: String,
        obj: String = "$remoteId.ics",
        etag: String? = null,
    ): Task {
        val task = Task(title = title)
        taskDao.createNew(task)
        caldavDao.insert(
            newCaldavTask(
                with(TASK, task.id),
                with(CALENDAR, calendar),
                with(REMOTE_ID, remoteId),
                with(OBJECT, obj),
                with(ETAG, etag),
            )
        )
        return task
    }

    @Test
    fun backupAndRestore() = runBlocking {
        setupAccount("acc-1")
        setupCalendar("acc-1", "cal-1")
        setupTask("test task", "cal-1", "remote-1")

        val backup = export()
        database.clearAllTables()

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(1, result.importCount)
        assertEquals(0, result.skipCount)
        val tasks = taskDao.getAll()
        assertEquals(1, tasks.size)
        assertEquals("test task", tasks[0].title)
        assertEquals(1, caldavDao.getAccounts().size)
        assertEquals(1, caldavDao.getCalendars().size)
    }

    @Test
    fun readStatusFromVtodoInPre1515Backup() = runBlocking {
        setupAccount("acc-1")
        setupCalendar("acc-1", "cal-1")
        cacheVtodo(setupTask("test task", "cal-1", "remote-1"), "cal-1", "remote-1", "IN-PROCESS")

        val backup = export().withVersion(Upgrade_15_14_1.VERSION)
        database.clearAllTables()
        import(backup)

        assertEquals(Task.Status.IN_PROCESS, taskDao.getAll().single().status)
    }

    @Test
    fun ignoreVtodoStatusInCurrentBackup() = runBlocking {
        setupAccount("acc-1")
        setupCalendar("acc-1", "cal-1")
        cacheVtodo(setupTask("test task", "cal-1", "remote-1"), "cal-1", "remote-1", "IN-PROCESS")

        val backup = export()
        database.clearAllTables()
        import(backup)

        assertEquals(Task.Status.NEEDS_ACTION, taskDao.getAll().single().status)
    }

    @Test
    fun ignoreVtodoStatusForOpenTasksInPre1515Backup() = runBlocking {
        setupAccount("acc-1", accountType = TYPE_OPENTASKS)
        setupCalendar("acc-1", "cal-1")
        cacheVtodo(setupTask("test task", "cal-1", "remote-1"), "cal-1", "remote-1", "IN-PROCESS")

        val backup = export().withVersion(Upgrade_15_14_1.VERSION)
        database.clearAllTables()
        import(backup)

        assertEquals(Task.Status.NEEDS_ACTION, taskDao.getAll().single().status)
    }

    private suspend fun cacheVtodo(task: Task, calendar: String, remoteId: String, status: String) {
        val caldavTask = caldavDao.getTasks(task.id).single()
        vtodoCache.putVtodo(
            caldavDao.getCalendar(calendar)!!,
            caldavTask,
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:test",
                "BEGIN:VTODO",
                "UID:$remoteId",
                "DTSTAMP:20260101T000000Z",
                "SUMMARY:${task.title}",
                "STATUS:$status",
                "END:VTODO",
                "END:VCALENDAR",
                "",
            ).joinToString("\r\n"),
        )
    }

    @Test
    fun skipDuplicatesByUuid() = runBlocking {
        val task = Task(title = "test task")
        taskDao.createNew(task)

        val backup = export()

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(1, taskDao.getAll().size)
    }

    @Test
    fun deduplicateByCaldavUrl() = runBlocking {
        val accountUrl = "https://caldav.tasks.org/calendars/user1/"
        val calendarUrl = "${accountUrl}tasks/"
        setupAccount("acc-A", url = accountUrl, username = "user@test.com")
        setupCalendar("acc-A", "cal-A", url = calendarUrl)
        setupTask("test task", "cal-A", "remote-1")

        val backup = export()
        database.clearAllTables()

        setupAccount("acc-B", url = accountUrl, username = "user@test.com")
        setupCalendar("acc-B", "cal-B", url = calendarUrl)
        setupTask("test task", "cal-B", "remote-1")

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(1, taskDao.getAll().size)
        assertEquals(1, caldavDao.getAccounts().size)
        assertEquals(1, caldavDao.getCalendars().size)
    }

    @Test
    fun deduplicateCaldavAccount() = runBlocking {
        val ncUrl = "https://nextcloud.example.com/remote.php/dav/calendars/user/"
        val ncCalUrl = "${ncUrl}tasks/"
        setupAccount("acc-A", accountType = TYPE_CALDAV, url = ncUrl, username = "user")
        setupCalendar("acc-A", "cal-A", url = ncCalUrl)
        setupTask("nextcloud task", "cal-A", "nc-remote-1")

        val backup = export()
        database.clearAllTables()

        setupAccount("acc-B", accountType = TYPE_CALDAV, url = ncUrl, username = "user")
        setupCalendar("acc-B", "cal-B", url = ncCalUrl)
        setupTask("nextcloud task", "cal-B", "nc-remote-1")

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(1, taskDao.getAll().size)
        assertEquals(1, caldavDao.getAccounts().size)
        assertEquals(1, caldavDao.getCalendars().size)
    }

    @Test
    fun deduplicateCaldavUrlsStoredBeforeCanonicalization() = runBlocking {
        val ncUrl = "https://nextcloud.example.com/remote.php/dav/calendars/foo%40example.com/"
        val ncCalUrl = "${ncUrl}tasks/"
        setupAccount("acc-A", accountType = TYPE_CALDAV, url = ncUrl, username = "user")
        setupCalendar("acc-A", "cal-A", url = ncCalUrl)
        setupTask("nextcloud task", "cal-A", "nc-remote-1")

        val backup = export().withVersion(Upgrade_15_13.VERSION - 1)
        database.clearAllTables()

        val canonicalUrl = "https://nextcloud.example.com/remote.php/dav/calendars/foo@example.com/"
        setupAccount("acc-B", accountType = TYPE_CALDAV, url = canonicalUrl, username = "user")
        setupCalendar("acc-B", "cal-B", url = "${canonicalUrl}tasks/")
        setupTask("nextcloud task", "cal-B", "nc-remote-1")

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(1, taskDao.getAll().size)
        assertEquals(listOf(canonicalUrl), caldavDao.getAccounts().map { it.url })
        assertEquals(listOf("${canonicalUrl}tasks/"), caldavDao.getCalendars().map { it.url })
    }

    @Test
    fun deduplicateMicrosoftByUsername() = runBlocking {
        setupAccount("acc-A", accountType = TYPE_MICROSOFT, url = null, username = "user@outlook.com")
        setupCalendar("acc-A", "cal-A", url = "tasks-list-1")
        setupTask("outlook task", "cal-A", "ms-remote-1", obj = "ms-remote-1")

        val backup = export()
        database.clearAllTables()

        setupAccount("acc-B", accountType = TYPE_MICROSOFT, url = null, username = "user@outlook.com")
        setupCalendar("acc-B", "cal-B", url = "tasks-list-1")
        setupTask("outlook task", "cal-B", "ms-remote-1", obj = "ms-remote-1")

        val result = import(backup)

        assertEquals(1, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(1, taskDao.getAll().size)
        assertEquals(1, caldavDao.getAccounts().size)
    }

    @Test
    fun importNewTaskToRemappedCalendar() = runBlocking {
        setupAccount("acc-A")
        setupCalendar("acc-A", "cal-A")
        setupTask("task 1", "cal-A", "remote-1")
        setupTask("task 2", "cal-A", "remote-2")

        val backup = export()
        database.clearAllTables()

        // Device B only has task 1 synced
        setupAccount("acc-B")
        setupCalendar("acc-B", "cal-B")
        setupTask("task 1", "cal-B", "remote-1")

        val result = import(backup)

        assertEquals(2, result.taskCount)
        assertEquals(1, result.importCount)
        assertEquals(1, result.skipCount)
        assertEquals(2, taskDao.getAll().size)
        val caldavTasks = caldavDao.getTasks(taskDao.getAll().first { it.title == "task 2" }.id)
        assertEquals("cal-B", caldavTasks.first().calendar)
    }

    @Test
    fun doNotDeduplicateDifferentUsersOnSameServer() = runBlocking {
        setupAccount("acc-A", accountType = TYPE_CALDAV, url = "https://server.example.com/dav/user1/", username = "user1")

        val backup = export()
        database.clearAllTables()

        setupAccount("acc-B", accountType = TYPE_CALDAV, url = "https://server.example.com/dav/user2/", username = "user2")

        import(backup)

        assertEquals(2, caldavDao.getAccounts().size)
    }

    @Test
    fun backupAndRestorePreservesTags() = runBlocking {
        tagDataDao.insert(TagData(name = "important", remoteId = "tag-1"))

        val backup = export()
        database.clearAllTables()

        import(backup)

        assertEquals(1, tagDataDao.getAll().size)
        assertEquals("important", tagDataDao.getAll()[0].name)
    }

    @Test
    fun downloadTasksAgainAfterRestoringDamagedBackup() = runBlocking {
        setupAccount("acc-1", accountType = TYPE_CALDAV)
        setupCalendar("acc-1", "cal-1", ctag = "ctag")
        setupTask("test task", "cal-1", "remote-1", etag = "etag")

        val backup = export().withVersion(Upgrade_15_13.VERSION)
        database.clearAllTables()

        import(backup)

        assertEquals(Upgrade_15_14_1.CTAG, caldavDao.getCalendars().single().ctag)
        assertNull(caldavDao.getTasks(taskDao.getAll().single().id).single().etag)
    }

    @Test
    fun keepSyncStateAfterRestoringRepairedBackup() = runBlocking {
        setupAccount("acc-1", accountType = TYPE_CALDAV)
        setupCalendar("acc-1", "cal-1", ctag = "ctag")
        setupTask("test task", "cal-1", "remote-1", etag = "etag")

        val backup = export().withVersion(Upgrade_15_14_1.VERSION)
        database.clearAllTables()

        import(backup)

        assertEquals("ctag", caldavDao.getCalendars().single().ctag)
        assertEquals("etag", caldavDao.getTasks(taskDao.getAll().single().id).single().etag)
    }
}
