package org.tasks.backup

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.CaldavTaskMaker.CALENDAR
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

    private suspend fun export(): ByteArray {
        val os = ByteArrayOutputStream()
        jsonExporter.doTasksExport(os, taskDao.getAllTaskIds())
        return os.toByteArray()
    }

    private suspend fun import(backup: ByteArray): TasksJsonImporter.ImportResult =
        jsonImporter.importTasks(context, { ByteArrayInputStream(backup) })

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
    ) {
        caldavDao.insert(
            CaldavCalendar(
                account = account,
                uuid = uuid,
                url = url,
                name = name,
            )
        )
    }

    private suspend fun setupTask(
        title: String,
        calendar: String,
        remoteId: String,
        obj: String = "$remoteId.ics",
    ): Task {
        val task = Task(title = title)
        taskDao.createNew(task)
        caldavDao.insert(
            newCaldavTask(
                with(TASK, task.id),
                with(CALENDAR, calendar),
                with(REMOTE_ID, remoteId),
                with(OBJECT, obj),
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
}
