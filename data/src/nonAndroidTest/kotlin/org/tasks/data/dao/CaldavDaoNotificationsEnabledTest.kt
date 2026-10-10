package org.tasks.data.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CaldavDaoNotificationsEnabledTest {
    private lateinit var db: Database
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(Database.CALLBACK)
            .build()
        caldavDao = db.caldavDao()
        taskDao = db.taskDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun returnsTasksInListsWithNotificationsDisabled() = runBlocking {
        val muted = createTask("shared list")
        val active = createTask("my list")
        addToList("muted", muted, notificationsEnabled = false)
        addToList("active", active, notificationsEnabled = true)

        assertEquals(listOf(muted), caldavDao.getMutedTaskIds(listOf(muted, active)))
    }

    @Test
    fun tasksWithoutAListAreNotMuted() = runBlocking {
        val task = createTask("no list")

        assertEquals(emptyList(), caldavDao.getMutedTaskIds(listOf(task)))
    }

    @Test
    fun deletedTasksAreNotMuted() = runBlocking {
        val task = createTask("deleted")
        addToList("muted", task, notificationsEnabled = false, deleted = 1)

        assertEquals(emptyList(), caldavDao.getMutedTaskIds(listOf(task)))
    }

    private suspend fun createTask(title: String): Long = taskDao.createNew(Task(title = title))

    private suspend fun addToList(
        uuid: String,
        task: Long,
        notificationsEnabled: Boolean,
        deleted: Long = 0,
    ) {
        caldavDao.insert(CaldavCalendar(uuid = uuid, notificationsEnabled = notificationsEnabled))
        caldavDao.insert(CaldavTask(task = task, calendar = uuid, deleted = deleted))
    }
}
