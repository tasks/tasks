package org.tasks.data.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.tasks.data.db.Database
import org.tasks.data.entity.Task
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskDaoReminderDismissedTest {
    private lateinit var db: Database
    private lateinit var taskDao: TaskDao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(Database.CALLBACK)
            .build()
        taskDao = db.taskDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun dismissalTimeOnlyMovesForward() = runBlocking {
        val id = taskDao.createNew(Task())

        taskDao.setReminderDismissed(listOf(id), 2000L)
        taskDao.setReminderDismissed(listOf(id), 1000L)

        assertEquals(2000L, taskDao.fetch(id)!!.reminderDismissed)
    }

    @Test
    fun aStaleWriteDoesNotUndoADismissal() = runBlocking {
        val id = taskDao.createNew(Task())
        val readBeforeTheDismissal = taskDao.fetch(id)!!
        taskDao.setReminderDismissed(listOf(id), 2000L)

        taskDao.update(readBeforeTheDismissal.copy(title = "Water the plants"))

        assertEquals(2000L, taskDao.fetch(id)!!.reminderDismissed)
    }

    @Test
    fun aStaleWriteWithAnOriginalDoesNotUndoADismissal() = runBlocking {
        val id = taskDao.createNew(Task())
        val original = taskDao.fetch(id)!!
        taskDao.setReminderDismissed(listOf(id), 2000L)

        taskDao.update(original.copy(title = "Water the plants"), original)

        assertEquals(2000L, taskDao.fetch(id)!!.reminderDismissed)
    }

    @Test
    fun clearingTheDismissalIsHonored() = runBlocking {
        val id = taskDao.createNew(Task())
        taskDao.setReminderDismissed(listOf(id), 2000L)
        val original = taskDao.fetch(id)!!

        taskDao.update(original.copy(reminderDismissed = 0L), original)

        assertEquals(0L, taskDao.fetch(id)!!.reminderDismissed)
    }

    @Test
    fun anIncomingDismissalIsHonored() = runBlocking {
        val id = taskDao.createNew(Task())
        taskDao.setReminderDismissed(listOf(id), 1000L)
        val original = taskDao.fetch(id)!!

        taskDao.update(original.copy(reminderDismissed = 3000L), original)

        assertEquals(3000L, taskDao.fetch(id)!!.reminderDismissed)
    }
}
